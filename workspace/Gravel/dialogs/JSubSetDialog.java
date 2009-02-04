package dialogs;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.util.BitSet;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JOptionPane;

import view.Gui;

import model.GraphMessage;
import model.MGraph;
import model.VEdge;
import model.VGraph;
import model.VNode;
import model.VSubSet;

/**
 *  JSubSetDialog
 *  Dialog for creation an Variation of SubSets
 *  
 * 	@author Ronny Bergmann 
 */
public class JSubSetDialog extends JDialog implements ActionListener, ItemListener
{
	private static final long serialVersionUID = 426L;
	//Alte Werte beim editieren
	private int oldindex;
	private Color oldcolor;
	private BitSet oldedges, oldnodes;
	private String oldname;
	//Knoten und Kantenlisten zum netten hinzufügen und entfernen
	private Vector<String> nodelist, edgelist; //Zum rueckwaerts nachschauen des Indexes
	private JCheckBox[] nodechecks, edgechecks; //Array der Knotennamen und ob diese enthalten sind (CheckBoxes)
	private JScrollPane iNodes, iEdges;
	//Der Graph
	private VGraph graphref;
	//Die Einfgabefelder
	private IntegerTextField iSubSetIndex;
	private TextField iSubSetName, Colorfield;
	//Beim Editieren zum testen der neuen Werte (im Vergleich zu den alten), Indikator fürs Editieren (==null falls erstellender Dialog)
	private VSubSet chSubSet;
	//Die Buttons
	private JButton bOK, bCancel, bChangeColor;
	
	/**
	 * Init the Dialog with Values for creation of a new SubSet
	 * 
	 * @param index its new index Index
	 * @param name	its new Name
	 * @param color	its color and the
	 * @param vg	corresponding VGraph
	 */
	public JSubSetDialog(int index, String name, Color color, VGraph vg)
	{
		chSubSet = null;
		oldindex = index;
		oldname = name;
		oldcolor = color;
		CreateDialog(null, vg);
	}
	/**
	 * Init the Dialog for Variation of a Subset
	 * 
	 * @param s SubSet in the
	 * @param vg corresponding VGraph
	 */
	public JSubSetDialog(VSubSet s,VGraph vg)
	{
		CreateDialog(s,vg);
	}
	/**
	 * Create and init the Dialog for a 
	 * @param s given SubSet
	 * @param vG in a corresponding VGraph
	 */
	private void CreateDialog(VSubSet s, VGraph vG)
	{
		graphref = vG;
		oldedges = new BitSet();
		oldnodes = new BitSet();
		if ((s!=null)&&(!vG.getSubSet(s.getIndex()).equals(s))) //In diesem Graphen ist s gar nicht drin
			s = null;
		if (s==null)
		{
			this.setTitle("Neuen Untergraphen erstellen");
			chSubSet = null;
		}
		else
		{
			chSubSet = s;
			oldname = graphref.getSubSetName(s.getIndex());
			oldindex = s.getIndex();
			oldcolor = s.getColor();
			//Knoten finden
			Iterator<VNode> nodeiter = graphref.getNodeIterator();
			while (nodeiter.hasNext())
			{
				VNode n = nodeiter.next();
				oldnodes.set(n.index,graphref.SubSetcontainsNode(n.index, s.getIndex()));
			}
			//Kanten finden
			Iterator <VEdge> edgeiter = graphref.getEdgeIterator();
			while (edgeiter.hasNext())
			{
				VEdge e = edgeiter.next();
				oldedges.set(e.index,graphref.SubSetcontainsEdge(e.index, s.getIndex()));
			}
			this.setTitle("Eigenschaften des Untergraphen '"+graphref.getSubSetName(s.getIndex())+"'");	
		}
		
		Container content = getContentPane();
		content.setLayout(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(7,7,7,7);
		c.anchor = GridBagConstraints.WEST;
		c.gridy = 0;
		c.gridx = 0;
		content.add(new JLabel("Index"),c);
		c.gridx = 1;
		iSubSetIndex = new IntegerTextField();
		iSubSetIndex.setPreferredSize(new Dimension(200, 20));

		content.add(iSubSetIndex,c);
		
		c.gridy++;
		c.gridx = 0;
		content.add(new JLabel("Name"),c);
		c.gridx = 1;
		iSubSetName = new TextField();
		iSubSetName.setPreferredSize(new Dimension(200, 20));
		
		content.add(iSubSetName,c);
		
		c.gridy++;
		c.gridx = 0;
		content.add(new JLabel("Farbe"),c);
		c.gridx = 1;
		Colorfield = new TextField(); 
		Colorfield.setPreferredSize(new Dimension(200,20));
		Colorfield.setEditable(false);
		content.add(Colorfield,c);
		c.gridy++;
		c.gridx = 1;
		c.insets = new Insets(0,7,7,7);
		bChangeColor = new JButton("<html>Farbe "+main.CONST.html_ae+"ndern</html>");
		bChangeColor.addActionListener(this);
		content.add(bChangeColor,c);
		
		//Knoten und Kantenlisten
		buildNodeList();
		buildEdgeList();
		c.gridy++;
		c.gridx=0;
		c.insets = new Insets(0,7,0,7);
		c.anchor = GridBagConstraints.CENTER;
		content.add(new JLabel("Knoten"),c);
		c.gridx=1;
		content.add(new JLabel("Kanten"),c);
		c.gridy++;
		c.gridx=0;
		c.anchor = GridBagConstraints.WEST;
		content.add(iNodes,c);
		c.gridx=1;
		content.add(iEdges,c);
		
		c.gridy++;
		c.gridx = 0;
		c.insets = new Insets(3,3,3,3);
		bCancel = new JButton("Abbrechen");
		bCancel.addActionListener(this);
		content.add(bCancel,c);

		c.gridx = 1;
		c.anchor = GridBagConstraints.EAST;
		if (s==null)
			bOK = new JButton("Untergraphen erstellen");
		else
			bOK = new JButton("<html>"+main.CONST.html_Ae+"nderungen speichern</html>");
		bOK.setMnemonic(KeyEvent.VK_ENTER);
		bOK.addActionListener(this);
		content.add(bOK,c);


		
		Colorfield.setBackground(oldcolor);
		//Werte einfuegen
		iSubSetIndex.setValue(oldindex);
		iSubSetName.setText(oldname);	
		
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
	 * Create the list of nodes
	 *
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
		nodelist = graphref.getNodeNames();
		int temp = 0;
		for (int i=0; i<nodelist.size(); i++)
		{
			if (nodelist.elementAt(i)!=null) //Ein Knoten mit dem Index existiert
			temp ++; //Anzahl Knoten zaehlen
		}
		nodechecks = new JCheckBox[temp];
		temp = 0;
		for (int i=0; i<nodelist.size(); i++)
		{
			if (nodelist.elementAt(i)!=null) //Ein Knoten mit dem Index existiert
			{
				nodechecks[temp] = new JCheckBox(graphref.getNodeName(i)+"   (#"+i+")");
				nodechecks[temp].setSelected(oldnodes.get(i));
				CiNodes.add(nodechecks[temp],c);
				c.gridy++;
				temp++; //Anzahl Knoten zaehlen
			}
		}
		iNodes = new JScrollPane(CiNodes);
		iNodes.setAlignmentX(JScrollPane.LEFT_ALIGNMENT);
		iNodes.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		iNodes.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		iNodes.setPreferredSize(new Dimension(200,100));
	}
	/**
	 * Create the list of edges
	 *
	 */
	private void buildEdgeList()
	{
		Container CiEdges = new Container();
		CiEdges.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(0,0,0,0);
		c.anchor = GridBagConstraints.NORTHWEST;
		c.gridy = 0;
		c.gridx = 0;
		edgelist = graphref.getEdgeNames();
		int temp = 0;
		for (int i=0; i<edgelist.size(); i++)
		{
			if (edgelist.elementAt(i)!=null) //Ein Knoten mit dem Index existiert
			temp ++; //Anzahl Knoten zaehlen
		}
		edgechecks = new JCheckBox[temp];
		temp = 0;
		for (int i=0; i<edgelist.size(); i++)
		{
			if (edgelist.elementAt(i)!=null) //Ein Knoten mit dem Index existiert
			{
				Vector<Integer> werte = graphref.getEdgeProperties(i);
				String actualname = "#"+werte.elementAt(MGraph.EDGESTARTINDEX)+" -";
				if (graphref.isDirected()) 
					actualname+=">";
				actualname +=" #"+werte.elementAt(MGraph.EDGEENDINDEX);
				edgechecks[temp] = new JCheckBox(actualname);
				edgechecks[temp].setSelected(oldedges.get(i));
				CiEdges.add(edgechecks[temp],c);
				c.gridy++;
				temp++; //Anzahl Knoten zaehlen
			}
		}
		iEdges = new JScrollPane(CiEdges);
		iEdges.setAlignmentX(JScrollPane.LEFT_ALIGNMENT);
		iEdges.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		iEdges.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		iEdges.setPreferredSize(new Dimension(200,100));
	}	
	public void actionPerformed(ActionEvent event)
	{
		if (event.getSource()==bChangeColor)
		{
			JColorChooser t = new JColorChooser();
			t.setPreviewPanel(new JLabel());
			Colorfield.setBackground(JColorChooser.showDialog(t, "Farbe "+main.CONST.utf8_ae+"ndern",	Colorfield.getBackground()));
			Colorfield.requestFocus();
			Colorfield.repaint();
			this.repaint();
		}
		if (event.getSource()==bCancel)
		{
			this.dispose();
		}
		if (event.getSource()==bOK)
		{
			//Test, ob die notwendigen Felder ausgefuellt sind, das umfasst einen INdex und einen Namen
			if ((iSubSetIndex.getValue()==-1)||(iSubSetName.equals("")))
			{
				String message = new String();
				if (chSubSet ==null)
					message = "<html><p>Erstellen des Untergraphen nicht m"+main.CONST.html_oe+"glich.";
				else
					message = "<html><p>"+main.CONST.html_Ae+"ndern des Untergraphen nicht m"+main.CONST.html_oe+"glich.";
				message+="<br><br>Einige Felder nicht ausgef"+main.CONST.html_ue+"llt.</p></hmtl>";
				JOptionPane.showMessageDialog(this,message, "Fehler", JOptionPane.ERROR_MESSAGE);
				return;
			}
			// Farbe bereits vergeben ?
			boolean colorgone = false;
			Iterator<VSubSet> subsetiter = graphref.getSubSetIterator();
			while (subsetiter.hasNext())
			{
				if (subsetiter.next().getColor().equals(Colorfield.getBackground())) //Farbe vergeben!
						colorgone = true;
			}
			int SetIndex = iSubSetIndex.getValue();
			GraphMessage startblock = new GraphMessage(GraphMessage.SUBSET, SetIndex, GraphMessage.ADDITION|GraphMessage.BLOCK_START, GraphMessage.NODE|GraphMessage.EDGE);
			//TESTS
			//1. Falls der Graph neu ist
			if (chSubSet==null) //neuer Untergraph, index testen
			{
				//Index bereits vergeben ?
				if (graphref.getSubSet(SetIndex)!=null) //So einen gibt es schon
				{
					JOptionPane.showMessageDialog(this, "<html><p>Erstellen des Untergraphen Nicht m"+main.CONST.html_oe+"glich.<br><br>Der Index #"+SetIndex+" ist bereits vergeben.</p></hmtl>", "Fehler", JOptionPane.ERROR_MESSAGE);
					return;
				}
				//Farbe bereits vergeben ?
				if (colorgone) //Farbe vergeben!
				{
					JOptionPane.showMessageDialog(this, "<html><p>Erstellen des Untergraphen Nicht m"+main.CONST.html_oe+"glich.<br><br>Die Farbe ist bereits vergeben.</p></hmtl>", "Fehler", JOptionPane.ERROR_MESSAGE);
					return;
				}
				startblock.setMessage("Untergraph #"+SetIndex+"erstellt");
				graphref.pushNotify(startblock);
			}
			else //2. Untergraphenaenderungsdialog
			{
				//Auswertung der neuen Daten, Pruefung auf Korrektheit
				//Falls sich der Kantenindex geaendert hat darf dieser nicht vergeben sein
				if ((graphref.getSubSet(SetIndex)!=null)&&(SetIndex!=oldindex)) //So einen gibt es schon
				{
					JOptionPane.showMessageDialog(this, "<html><p>"+main.CONST.html_Ae+"nderung des Untergraphen nicht m"+main.CONST.html_oe+"glich.<br><br>Der Index ist bereits vergeben.</p></hmtl>", "Fehler", JOptionPane.ERROR_MESSAGE);
					return;
				}
				//Falls sich die Farbe geaendert hat, darf auch diese nicht vergeben sein
				if ((!(Colorfield.getBackground().equals(oldcolor)))&&(colorgone))
				{
					JOptionPane.showMessageDialog(this, "<html><p>"+main.CONST.html_Ae+"nderung des Untergraphen nicht m"+main.CONST.html_oe+"glich.<br><br>Die Farbe ist bereits vergeben.</p></hmtl>", "Fehler", JOptionPane.ERROR_MESSAGE);
					return;
				}
				//Sonst läßt sich das alles ändern, also entfernen
				startblock.setMessage("Untergraph #"+SetIndex+" verändert");
				graphref.pushNotify(startblock);
				graphref.removeSubSet(oldindex);
			}
			//Und (im zweiten Fall neu, sonst allgemein) einfuegen
			//Sonst geht alles seiner Wege und wir fuegen den Untergraphen ein
			graphref.addSubSet(SetIndex, iSubSetName.getText(),Colorfield.getBackground());
			//Einfuegen der Knoten und Kanten in den Untergraphen
			//Kanten
			int temp = 0;
			for (int i=0; i<edgelist.size(); i++)
			{
				if (edgelist.elementAt(i)!=null) //Eine Kante mit diesem Index existiert und sie ist selektiert
				{
					if (edgechecks[temp].isSelected())
						graphref.addEdgetoSubSet(i, SetIndex);
					temp ++; //Anzahl Kanten zaehlen
				}	

			}
			//Knoten
			temp = 0;
			for (int i=0; i<nodelist.size(); i++)
			{
				if (nodelist.elementAt(i)!=null) //Eine Knoten mit diesem Index existiert und sie ist selektiert
				{
					if (nodechecks[temp].isSelected())
						graphref.addNodetoSubSet(i, SetIndex);
					temp ++; //Anzahl Knoten zaehlen
				}	
				
			}
			graphref.pushNotify(new GraphMessage(GraphMessage.SUBSET, SetIndex,GraphMessage.BLOCK_END, GraphMessage.NODE|GraphMessage.EDGE));
			this.dispose();
		}
	}
	
	public void itemStateChanged(ItemEvent event) 
	{
		
	}
}
