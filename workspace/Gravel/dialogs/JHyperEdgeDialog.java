package dialogs;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.util.BitSet;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import dialogs.components.*;

import model.*;
import model.Messages.GraphConstraints;
import model.Messages.GraphMessage;

import view.Gui;
/**
 * This Dialog is used to change the properties of an hyperedge and to create new hyperedges.
 * 
 * It includes Initialization of all field with the values of a given hyperedge,
 * the text of the validity of the parameter fields due to any VHypergraph
 * and
 * the change of the hyperedge in the hypergraph it belongs to
 *
 * @author Ronny Bergmann
 * @since 0.4
 */
public class JHyperEdgeDialog extends JDialog implements ActionListener, ItemListener
{
	private static final long serialVersionUID = 1L;

	private MHyperEdge oldmhyperedge;
	private VHyperEdge oldvhyperedge;
	boolean isNewHyperedge;
	private Vector<String> nodelist; //Zum rueckwaerts nachschauen des Indexes
	private JCheckBox[] NodeChecks;
	private JScrollPane iNode;
	private Vector<String> subgraphlist;
	private JCheckBox[] SubgraphChecks;
	private JScrollPane iSubgraph;
	private VHyperGraph graphref;
	private IntegerTextField iEdgeIndex, iWidth, iValue, iMargin;
	private JTextField Colorfield, EdgeName;
	private JButton bOK, bCancel;
		
	private CEdgeLineParameters cLine;
	private CEdgeTextParameters cText;
	private Container MainContent, TextContent;

	private JTabbedPane tabs;
	
	/**
	 * Initialize the dialog with given values for creation of an edge 
	 * Reference Graph is the actual Graph edited in the GUI and startvalues are the 
	 * General standard Values
	 */
	public JHyperEdgeDialog()
	{
		this(new BitSet());
	}
	public JHyperEdgeDialog(BitSet initNodes)
	{
		if (Gui.getInstance().getVGraph().getType()!=VGraphInterface.HYPERGRAPH)
			return;
		graphref = (VHyperGraph)Gui.getInstance().getVGraph();
		int index = graphref.getMathGraph().modifyHyperEdges.getNextIndex();
		oldmhyperedge = new MHyperEdge(index,1,"E "+index); //Value 1 TODO GeneralPreferences-Std-Values
		oldvhyperedge = new VHyperEdge(index,1, 12); //Width 1 TODO GP STd Values also for Distance
		for (int i=0; i<=initNodes.length(); i++)
		{
			if (initNodes.get(i))
				oldmhyperedge.addNode(i);
		}
		CreateDialog(null);
	}
	/**
	 * Initialization of the Dialog with the properties of
	 * @param e an edge in the 
	 * @param vg corresponding VGraph
	 */
	public JHyperEdgeDialog(VHyperEdge e)
	{
		if (Gui.getInstance().getVGraph().getType()!=VGraphInterface.HYPERGRAPH)
			return;
		graphref = (VHyperGraph)Gui.getInstance().getVGraph();
		oldmhyperedge = graphref.getMathGraph().modifyHyperEdges.get(e.getIndex()).clone();
		oldvhyperedge = e.clone();
		e.copyColorStatus(oldvhyperedge);
		CreateDialog(e);
	}
	/**
	 * initialization of all parameter fields to the given values
	 * @param e
	 */
	private void CreateDialog(VHyperEdge e)
	{
		graphref = (VHyperGraph)Gui.getInstance().getVGraph();
		isNewHyperedge = (e==null);
		if (isNewHyperedge)
		{
			this.setTitle("Neue Hyperkante erstellen");
			cText = new CEdgeTextParameters(null,false,false); //nonglobal, no checks
			cLine = new CEdgeLineParameters(null,false,false); //nonglobal no checks
		}
		else
		{
			this.setTitle("Eigenschaften der Kante '"+oldmhyperedge.name+"' (#"+e.getIndex()+")");	
			cText = new CEdgeTextParameters(oldvhyperedge.getTextProperties(),false,false); //nonglobal, no checks
			cLine = new CEdgeLineParameters(oldvhyperedge.getLinestyle(),false,false); //nonglobal no checks
		}
		
		tabs = new JTabbedPane();
		GridBagConstraints c = new GridBagConstraints();
		
		//Build Main Tab
		buildMainContent();
		
		//Build View Tab
		buildViewTab();
		
		tabs.addTab("Allgemein", MainContent);
		tabs.addTab("Ansicht", TextContent);
				
		Container ContentPane = this.getContentPane();
		ContentPane.setLayout(new GridBagLayout());
		c.gridy=0;c.gridx=0;c.gridwidth=2;
		ContentPane.add(tabs,c);
		c.gridy++;c.gridx = 0;c.gridwidth=1;
		
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(3,3,3,3);
		bCancel = new JButton("Abbrechen");
		bCancel.addActionListener(this);
		ContentPane.add(bCancel,c);
		
		//Add ESC-Handling
		InputMap iMap = getRootPane().getInputMap(	 JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape");

		ActionMap aMap = getRootPane().getActionMap();
		aMap.put("escape", new AbstractAction()
			{
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e)
				{
					dispose();
				}
		 	});
		
		c.gridx = 1;
		c.anchor = GridBagConstraints.EAST;
		if (e==null)
			bOK = new JButton("Hyperkante erstellen");
		else
			bOK = new JButton("<html>"+main.CONST.html_Ae+"nderungen speichern</html>");
		bOK.addActionListener(this);
		ContentPane.add(bOK,c);
		
		this.getRootPane().setDefaultButton(bOK);
		setResizable(false);
		this.setModal(true);
		pack();
		Point p = new Point(0,0);
		p.y += Math.round(Gui.getInstance().getParentWindow().getHeight()/2);
		p.x += Math.round(Gui.getInstance().getParentWindow().getWidth()/2);
		p.y -= Math.round(getHeight()/2);
		p.x -= Math.round(getWidth()/2);

		setLocation(p.x,p.y);
		
		this.setVisible(true);
	}
	/**
	 * Build the main Tab containing the mathematical values and the standard visual properties
	 *
	 */
	private void buildMainContent()
	{
		MainContent = new Container();
		MainContent.setLayout(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(7,7,7,7);
		c.anchor = GridBagConstraints.WEST;
		c.gridy = 0;
		c.gridx = 0;
		MainContent.add(new JLabel("Index"),c);
		c.gridx = 1;
		iEdgeIndex = new IntegerTextField();
		iEdgeIndex.setPreferredSize(new Dimension(100, 20));

		MainContent.add(iEdgeIndex,c);

		c.gridy++;
		c.gridx = 0;
		MainContent.add(new JLabel("<html>Name</html>"),c);
		c.gridx = 1;
		EdgeName = new JTextField();
		EdgeName.setPreferredSize(new Dimension(100, 20));
		MainContent.add(EdgeName,c);

		c.gridy++;
		c.gridx = 0;
		MainContent.add(new JLabel("<html>Gewicht</html>"),c);
		c.gridx = 1;
		iValue = new IntegerTextField();
		iValue.setPreferredSize(new Dimension(100, 20));
		MainContent.add(iValue,c);

		c.gridy++;
		c.gridx = 0;
		MainContent.add(new JLabel("<html>Breite</html>"),c);
		c.gridx = 1;
		iWidth = new IntegerTextField();
		iWidth.setPreferredSize(new Dimension(100, 20));
		MainContent.add(iWidth,c);

		c.gridy++;
		c.gridx = 0;
		MainContent.add(new JLabel("<html><font color=#666666>Farbe<br><font size=\"-2\">ergibt sich aus den Untergraphen</font></font></html>"),c);
		c.gridx = 1;
		Colorfield = new JTextField(); 
		Colorfield.setPreferredSize(new Dimension(100,20));
		Colorfield.setEditable(false);
		MainContent.add(Colorfield,c);
		c.gridy++;
		c.gridx = 0;
		buildSubgraphList();
		buildNodeList();
		c.gridy++;
		c.gridx=0;
		c.insets = new Insets(0,7,0,7);
		c.anchor = GridBagConstraints.CENTER;
		MainContent.add(new JLabel("Knoten"),c);
		c.gridx=1;
		MainContent.add(new JLabel("Untergraphen"),c);
		c.gridy++;
		c.gridx=0;
		c.anchor = GridBagConstraints.WEST;
		MainContent.add(iNode,c);
		c.gridx=1;
		MainContent.add(iSubgraph,c);		
		c.gridy++;
		c.gridx = 0;
		c.insets = new Insets(7,7,7,7);
		c.gridwidth = 1;
		
		if (isNewHyperedge)
		{
			Colorfield.setBackground(Color.BLACK);
		}
		else
			Colorfield.setBackground(oldvhyperedge.getColor());
		
		//Werte einfuegen
		iEdgeIndex.setValue(oldmhyperedge.index);
		iWidth.setValue(oldvhyperedge.getWidth());
		iValue.setValue(oldmhyperedge.Value);
		EdgeName.setText(oldmhyperedge.name);
	}
	/**
	 * Built the view Tab containing the visual stuff
	 */
	private void buildViewTab()
	{
		TextContent = new Container();
		TextContent.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(7,7,7,7);
		c.anchor = GridBagConstraints.WEST;
		c.gridx=0; c.gridy=0; c.gridwidth=1;
		TextContent.add(new JLabel("<html>Innenabstand<br><font  size=\"-2\">Des Umrisses zu den Knoten der Kante</font></html>"),c);
		c.gridx = 1;
		iMargin = new IntegerTextField();
		iMargin.setPreferredSize(new Dimension(100, 20));
		TextContent.add(iMargin,c);

		c.gridx=0;
		c.gridwidth=2;
		c.anchor = GridBagConstraints.CENTER;
		c.gridy++; TextContent.add(cText.getContent(),c);
		c.gridy++; TextContent.add(cLine.getContent(),c);
		
		//Werte einfügen
		iMargin.setValue(12); //TODO Std.
	}
	/**
	 * Help function to create the list of node names from the corresponding VGraph 
	 * to create the Nodelists for start and endnode
	 */
	private void buildNodeList()
	{
		Container CiNodes = new Container();
		CiNodes.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(0,0,0,0);
		c.anchor = GridBagConstraints.NORTHWEST;
		c.gridy = 0;
		c.gridx = 0;
		nodelist = graphref.getMathGraph().modifyNodes.getNames();
		int temp = 0;
		for (int i=0; i<nodelist.size(); i++)
		{
			if (nodelist.elementAt(i)!=null) //Ein Knoten mit dem Index existiert
				temp ++; //Anzahl Knoten zaehlen
		}
		NodeChecks = new JCheckBox[temp];
		temp = 0;
		for (int i=0; i<nodelist.size(); i++)
		{
			if (nodelist.elementAt(i)!=null) //Ein Knoten mit dem Index existiert
			{
				NodeChecks[temp] = new JCheckBox(nodelist.get(i));
				NodeChecks[temp].setSelected(oldmhyperedge.containsNode(i));					
				CiNodes.add(NodeChecks[temp],c);
				NodeChecks[temp].addItemListener(this);
				c.gridy++;
				temp++; //Anzahl Knoten zaehlen
			}
		}
		iNode = new JScrollPane(CiNodes);
		iNode.setAlignmentX(JScrollPane.LEFT_ALIGNMENT);
		iNode.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		iNode.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		iNode.setPreferredSize(new Dimension(200,100));
	}
	/**
	 * Build the list of subgraphs
	 */
	private void buildSubgraphList()
	{
		Container CiSubgraphs = new Container();
		CiSubgraphs.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(0,0,0,0);
		c.anchor = GridBagConstraints.NORTHWEST;
		c.gridy = 0;
		c.gridx = 0;
		subgraphlist = graphref.getMathGraph().modifySubgraphs.getNames();
		int temp = 0;
		for (int i=0; i<subgraphlist.size(); i++)
		{
			if (subgraphlist.elementAt(i)!=null) //Ein Knoten mit dem Index existiert
			temp ++; //Anzahl Knoten zaehlen
		}
		SubgraphChecks = new JCheckBox[temp];
		temp = 0;
		for (int i=0; i<subgraphlist.size(); i++)
		{
			if (subgraphlist.elementAt(i)!=null) //Ein Knoten mit dem Index existiert
			{
				SubgraphChecks[temp] = new JCheckBox(graphref.getMathGraph().modifySubgraphs.get(i).getName());
				SubgraphChecks[temp].setSelected(graphref.getMathGraph().modifySubgraphs.get(i).containsEdge(oldvhyperedge.getIndex()));
				CiSubgraphs.add(SubgraphChecks[temp],c);
				SubgraphChecks[temp].addItemListener(this);
				c.gridy++;
				temp++; //Anzahl Knoten zaehlen
			}
		}
		iSubgraph = new JScrollPane(CiSubgraphs);
		iSubgraph.setAlignmentX(JScrollPane.LEFT_ALIGNMENT);
		iSubgraph.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		iSubgraph.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		iSubgraph.setPreferredSize(new Dimension(200,100));
	}
	/**
	 * Check the parameter fields to be valid and change the edge / add the edge to the graph
	 */
	private void Check()
	{
		String message="";
		if (isNewHyperedge)
			message = "<html><p>Erstellen der Kante nicht m"+main.CONST.html_oe+"glich.";
		else
			message = "<html><p>"+main.CONST.html_Ae+"ndern der Kante nicht m"+main.CONST.html_oe+"glich.";

//		Test, ob die notwendigen Felder ausgefuellt sind, wobei die Knotendropdowns nicht geprüft werden muessen
		if ((iEdgeIndex.getValue()==-1)||(iWidth.getValue()==-1)||(iValue.getValue()==-1)||(iMargin.getValue()==-1))
		{
			message+="<br><br>Einige Felder nicht ausgef"+main.CONST.html_ue+"llt.</p></hmtl>";
			JOptionPane.showMessageDialog(this,message, "Fehler", JOptionPane.ERROR_MESSAGE);
			return;
		}
		//Pruefen der Textfelder, falls aktiviert
		String t = cText.VerifyInput(EdgeName.getText());
		if (!t.equals(""))
		{
			message+="<br><br>"+t+"</p></html>";
			JOptionPane.showMessageDialog(this,message, "Fehler", JOptionPane.ERROR_MESSAGE);				
			return;
		}
		//Testen der Linienbedingungen, Felder die nicht ausgefüllt und gleichzeitig nicht benötigt werden, werden auch nicht gesetzt
		t = cLine.VerifyInput();
		if (!t.equals(""))
		{
			message+="<br><br>"+t+"</p></html>";			
			JOptionPane.showMessageDialog(this,message, "Fehler", JOptionPane.ERROR_MESSAGE);				
			return;
		}
		MHyperEdge checkEdge = new MHyperEdge(iEdgeIndex.getValue(),iValue.getValue(),EdgeName.getText());
		int temp=0;
		for (int i=0; i<nodelist.size(); i++)
		{
			if (nodelist.elementAt(i)!=null) //Ein Untergraph mit dem Index existiert
			{
				if (NodeChecks[temp].isSelected())
					checkEdge.addNode(i);
				temp++; //Position in checks-vector
			}
		}
		if (checkEdge.cardinality()==0)
		{
			JOptionPane.showMessageDialog(this,message+="<br><br>Es ist kein Knoten in der Hyperkante, leere Kanten sind nicht erlaubt.</p></html>", "Fehler", JOptionPane.ERROR_MESSAGE);				
			return;			
		}
		//If we have an edge with same nodeset, existsEdge is !=null
		MHyperEdge existsEdge = graphref.getMathGraph().modifyHyperEdges.get(checkEdge.getEndNodes());
		if (isNewHyperedge) //neuer Kante, index testen
		{
			//Index bereits vergeben ?
			if (graphref.modifyHyperEdges.get(iEdgeIndex.getValue())!=null) //So einen gibt es schon
			{
				JOptionPane.showMessageDialog(this, "<html><p>Erstellen der Hyperkante nicht m"+main.CONST.html_oe+"glich.<br><br>Der Index ist bereits vergeben.</p></hmtl>", "Fehler", JOptionPane.ERROR_MESSAGE);
				return;
			}
			if (existsEdge!=null) //Would result in Duplicate Edge
			{
				JOptionPane.showMessageDialog(this, "<html><p>Erstellen der Hyperkante nicht m"+main.CONST.html_oe+"glich.<br><br>Eine Kante mit der gleichen Menge Knoten existiert und Mehrfachkanten sind nicht erlaubt.</p></hmtl>", "Fehler", JOptionPane.ERROR_MESSAGE);
				return;
			}
		}
		else //Kantenaenderungsdialog
		{
//			Auswertung der neuen Daten, Pruefung auf Korrektheit
			//Falls sich der Kantenindex geaendert hat darf dieser nicht vergeben sein
			if ((graphref.modifyHyperEdges.get(iEdgeIndex.getValue())!=null)&&(iEdgeIndex.getValue()!=oldmhyperedge.index)) //So einen gibt es schon
			{
				JOptionPane.showMessageDialog(this, "<html><p>"+main.CONST.html_Ae+"nderung der Hyperkante nicht m"+main.CONST.html_oe+"glich.<br><br>Der Index ist bereits vergeben.</p></html>", "Fehler", JOptionPane.ERROR_MESSAGE);
				return;
			}
			if ((existsEdge!=null)&&(existsEdge.index!=oldmhyperedge.index)) //Another edge with the new set of nodex exists but it wasn't the old one before
			{
				JOptionPane.showMessageDialog(this, "<html><p>"+main.CONST.html_Ae+"nderung der Hyperkante nicht m"+main.CONST.html_oe+"glich.<br><br>Eine Hyperkante mit der gleichen Knotenmenge existiert bereits.</p></html>", "Fehler", JOptionPane.ERROR_MESSAGE);
				return;
				
			}
			if (oldmhyperedge.index==iEdgeIndex.getValue()) //Index not changed -> Just an EdgeReplace
				graphref.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,oldmhyperedge.index,GraphConstraints.UPDATE|GraphConstraints.BLOCK_START,GraphConstraints.HYPEREDGE));
			else
				graphref.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,GraphConstraints.UPDATE|GraphConstraints.BLOCK_START,GraphConstraints.HYPEREDGE));
			graphref.modifyHyperEdges.remove(oldmhyperedge.index);
		}
		VHyperEdge addEdge = new VHyperEdge (iEdgeIndex.getValue(), iWidth.getValue(), iMargin.getValue(), oldvhyperedge.getShape().clone(), new VEdgeText(), new VEdgeLinestyle());

		MHyperEdge mathEdge = new MHyperEdge(addEdge.getIndex(),iValue.getValue(),EdgeName.getText());
		int arrayindex=0;
		for (int i=0; i<nodelist.size(); i++)
		{
			if (nodelist.elementAt(i)!=null) //Ein Untergraph mit dem Index existiert
			{
				if (NodeChecks[arrayindex].isSelected())
					mathEdge.addNode(i);
				arrayindex++; //Position at vector
			}
		}
		addEdge.setWidth(iWidth.getValue());
		if (graphref.modifyHyperEdges.getIndexWithSimilarShape(addEdge) > 0)
		{
			JOptionPane.showMessageDialog(this, "<html><p>"+main.CONST.html_Ae+"nderung der Kante nicht m"+main.CONST.html_oe+"glich.<br><br>Eine Kante mit gleichem (nichtleeren) Umriss existiert.</p></html>", "Fehler", JOptionPane.ERROR_MESSAGE);
			return;	
		}
		else if (graphref.getMathGraph().modifyHyperEdges.get(mathEdge.getEndNodes())!=null)
		{
			JOptionPane.showMessageDialog(this, "<html><p>"+main.CONST.html_Ae+"nderung der Kante nicht m"+main.CONST.html_oe+"glich.<br><br>Eine Kante mit der gleichen Knotenmenge existiert.</p></html>", "Fehler", JOptionPane.ERROR_MESSAGE);
			return;	
		}
		graphref.modifyHyperEdges.add(addEdge,mathEdge);		//Gruppen einbauen
		temp=0;
		for (int i=0; i<subgraphlist.size(); i++)
		{
			if (subgraphlist.elementAt(i)!=null) //Ein Untergraph mit dem Index existiert
			{
				if (SubgraphChecks[temp].isSelected())
					graphref.modifySubgraphs.addEdgetoSubgraph(iEdgeIndex.getValue(), i);
				temp++; //Position at vector
			}
		}
		//Text bauen
		VHyperEdge e = graphref.modifyHyperEdges.get(iEdgeIndex.getValue());
		e = cText.modifyHyperEdge(e);
		e = cLine.modifyHyperEdge(e);
		if (!isNewHyperedge)//Change edge, end block
		{
			if (oldmhyperedge.index==iEdgeIndex.getValue()) //Index not changed -> Just an EdgeReplace
				graphref.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,oldmhyperedge.index,GraphConstraints.BLOCK_END,GraphConstraints.HYPEREDGE));
			else
				graphref.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,GraphConstraints.BLOCK_END,GraphConstraints.HYPEREDGE));
		}
		this.dispose();
	}
	public void actionPerformed(ActionEvent event)
	{
		if (event.getSource()==bCancel)
		{
			this.dispose();
		}
		else if (event.getSource()==bOK)
		{
			Check();
		}
	}
	
	public void itemStateChanged(ItemEvent event) 
	{		
		for (int i=0; i<SubgraphChecks.length; i++)
		{
			if (event.getSource()==SubgraphChecks[i])
			{
				//Ein Zustand hat sich geändert, neue Farbe berechnen
				Color colour = Color.BLACK;
				int colourcount = 0;
				int temp = 0; //zum mitzaehlen
				VHyperEdge colorsrc = new VHyperEdge(0,0,0);
				for (int j=0; j<subgraphlist.size();j++)
				{
					if (subgraphlist.elementAt(j)!=null)
					{
						if (SubgraphChecks[temp].isSelected())
						{
							Color newc = graphref.modifySubgraphs.get(j).getColor();
							int b=colour.getBlue()*colourcount + newc.getBlue();
							int a=colour.getAlpha()*colourcount + newc.getAlpha();
							int g=colour.getGreen()*colourcount + newc.getGreen();
							int r=colour.getRed()*colourcount + newc.getRed();
							colourcount++;
							colorsrc.addColor(newc);
							colour = new Color((r/colourcount),(g/colourcount),(b/colourcount),(a/colourcount));
						}
							temp++;
					}
				}
				Colorfield.setBackground(colour);
				Colorfield.repaint();
				return;
			}
		}
	}
}