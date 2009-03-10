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

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import view.Gui;

import model.*;
import model.Messages.GraphConstraints;
import model.Messages.GraphMessage;

/**
 *  JSubGraphDialog
 *  Dialog for creation an Variation of Subgraphs
 *  
 * 	@author Ronny Bergmann 
 */
public class JSubgraphDialog extends JDialog implements ActionListener, ItemListener
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
	private VGraphInterface graphref;
	//Die Einfgabefelder
	private IntegerTextField iSubgraphIndex;
	private TextField iSubgraphName, Colorfield;
	//Beim Editieren zum testen der neuen Werte (im Vergleich zu den alten), Indikator fürs Editieren (==null falls erstellender Dialog)
	private VSubgraph chSubgraph;
	//Die Buttons
	private JButton bOK, bCancel, bChangeColor;
	
	/**
	 * Init the Dialog with Values for creation of a new VSubgraph
	 * 
	 * @param index its new index Index
	 * @param name	its new Name
	 * @param color	its color and the
	 * @param vg	corresponding VGraph
	 */
	public JSubgraphDialog(int index, String name, Color color, VGraphInterface vg)
	{
		chSubgraph = null;
		oldindex = index;
		oldname = name;
		oldcolor = color;
		CreateDialog(null, vg);
	}
	/**
	 * Init the Dialog for Variation of a VSubgraph
	 * 
	 * @param s Subgraph in the
	 * @param vg corresponding VGraph
	 */
	public JSubgraphDialog(VSubgraph s,VGraphInterface vg)
	{
		CreateDialog(s,vg);
	}
	/**
	 * Create and init the Dialog for a 
	 * @param originalSubgraph given Subgraph
	 * @param vG in a corresponding VGraph
	 */
	private void CreateDialog(VSubgraph originalSubgraph, VGraphInterface vG)
	{
		graphref = vG;
		VSubgraphSet subgraphs;
		MSubgraphSet msubgraphs;
		VNodeSet nodes;
		VEdgeSet edges=null;
		VHyperEdgeSet hyperedges=null;
		
		if (graphref.getType()==VGraphInterface.GRAPH)
		{
			nodes = ((VGraph)graphref).modifyNodes;
			edges = ((VGraph)graphref).modifyEdges;
			subgraphs = ((VGraph)graphref).modifySubgraphs;
			msubgraphs = ((VGraph)graphref).getMathGraph().modifySubgraphs;
		}
		else if (graphref.getType()==VGraphInterface.HYPERGRAPH)
		{
			nodes = ((VHyperGraph)graphref).modifyNodes;
			hyperedges = ((VHyperGraph)graphref).modifyHyperEdges;
			subgraphs = ((VHyperGraph)graphref).modifySubgraphs;
			msubgraphs = ((VHyperGraph)graphref).getMathGraph().modifySubgraphs;
		}
		else
			return;
		oldedges = new BitSet();
		oldnodes = new BitSet();
		if ((originalSubgraph!=null)&&(!subgraphs.get(originalSubgraph.getIndex()).equals(originalSubgraph))) //In diesem Graphen ist s gar nicht drin
			originalSubgraph = null;
		if (originalSubgraph==null)
		{
			this.setTitle("Neuen Untergraphen erstellen");
			chSubgraph = null;
		}
		else
		{
			chSubgraph = originalSubgraph;
			oldname = msubgraphs.get(originalSubgraph.getIndex()).getName();
			oldindex = originalSubgraph.getIndex();
			oldcolor = originalSubgraph.getColor();
			//Knoten finden
			Iterator<VNode> nodeiter = nodes.getIterator();
			while (nodeiter.hasNext())
			{
				VNode n = nodeiter.next();
				oldnodes.set(n.getIndex(),msubgraphs.get(originalSubgraph.getIndex()).containsNode(n.getIndex()));
			}
			//Kanten finden
			if (edges!=null)
			{
				Iterator <VEdge> edgeiter = edges.getIterator();
				while (edgeiter.hasNext())
				{
					VEdge e = edgeiter.next();
					oldedges.set(e.getIndex(),msubgraphs.get(originalSubgraph.getIndex()).containsEdge(e.getIndex()));
				}
			}
			else if (hyperedges!=null)
			{
				Iterator <VHyperEdge> edgeiter = hyperedges.getIterator();
				while (edgeiter.hasNext())
				{
					VHyperEdge e = edgeiter.next();
					oldedges.set(e.getIndex(),msubgraphs.get(originalSubgraph.getIndex()).containsEdge(e.getIndex()));
				}
			}
			this.setTitle("Eigenschaften des Untergraphen '"+msubgraphs.get(originalSubgraph.getIndex()).getName()+"'");	
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
		iSubgraphIndex = new IntegerTextField();
		iSubgraphIndex.setPreferredSize(new Dimension(200, 20));

		content.add(iSubgraphIndex,c);
		
		c.gridy++;
		c.gridx = 0;
		content.add(new JLabel("Name"),c);
		c.gridx = 1;
		iSubgraphName = new TextField();
		iSubgraphName.setPreferredSize(new Dimension(200, 20));
		
		content.add(iSubgraphName,c);
		
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
		if (graphref.getType()==VGraphInterface.GRAPH)
			content.add(new JLabel("Kanten"),c);
		else if (graphref.getType()==VGraphInterface.HYPERGRAPH)
			content.add(new JLabel("Hyperkanten"),c);
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
		if (originalSubgraph==null)
			bOK = new JButton("Untergraphen erstellen");
		else
			bOK = new JButton("<html>"+main.CONST.html_Ae+"nderungen speichern</html>");
		bOK.addActionListener(this);
		content.add(bOK,c);


		
		Colorfield.setBackground(oldcolor);
		//Werte einfuegen
		iSubgraphIndex.setValue(oldindex);
		iSubgraphName.setText(oldname);	
		
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
		if (graphref.getType()==VGraphInterface.GRAPH)
			nodelist = ((VGraph)graphref).getMathGraph().modifyNodes.getNames();
		else if (graphref.getType()==VGraphInterface.HYPERGRAPH)
			nodelist = ((VHyperGraph)graphref).getMathGraph().modifyNodes.getNames();
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
				nodechecks[temp] = new JCheckBox(nodelist.get(i)+"   (#"+i+")");
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
		if (graphref.getType()==VGraphInterface.GRAPH)
			edgelist = ((VGraph)graphref).getMathGraph().modifyEdges.getNames();
		else if (graphref.getType()==VGraphInterface.HYPERGRAPH)
			edgelist = ((VHyperGraph)graphref).getMathGraph().modifyHyperEdges.getNames();
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
				String guiname="";
				if (graphref.getType()==VGraphInterface.GRAPH)
				{
					MEdge me = ((VGraph)graphref).getMathGraph().modifyEdges.get(i);
					guiname = "#"+me.StartIndex+" -";
					if (((VGraph)graphref).getMathGraph().isDirected()) 
						guiname+=">";
					guiname +=" #"+me.EndIndex;
				}
				else if (graphref.getType()==VGraphInterface.HYPERGRAPH)
				{
					guiname = edgelist.get(i)+" (#"+i+")";
				}
				edgechecks[temp] = new JCheckBox(guiname);
				edgechecks[temp].setSelected(oldedges.get(i));
				CiEdges.add(edgechecks[temp],c);
				c.gridy++;
				temp++; //Anzahl Kanten zaehlen
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
			Color c = JColorChooser.showDialog(t, "Farbe "+main.CONST.utf8_ae+"ndern",	Colorfield.getBackground());
			if (c==null)
				return;
			Colorfield.setBackground(c);
			Colorfield.validate();
			Colorfield.repaint();
		}
		if (event.getSource()==bCancel)
		{
			this.dispose();
		}
		if (event.getSource()==bOK)
		{
			VSubgraphSet subgraphs;
			int edgemsg, allelements;
			if (graphref.getType()==VGraphInterface.GRAPH)
			{
				edgemsg = GraphConstraints.EDGE;
				allelements = GraphConstraints.GRAPH_ALL_ELEMENTS;
				subgraphs = ((VGraph)graphref).modifySubgraphs;
			}
			else if (graphref.getType()==VGraphInterface.HYPERGRAPH)
			{
				edgemsg = GraphConstraints.HYPEREDGE;
				allelements = GraphConstraints.HYPERGRAPH_ALL_ELEMENTS;
				subgraphs = ((VHyperGraph)graphref).modifySubgraphs;
			}
			else
				return;
			//Test, ob die notwendigen Felder ausgefuellt sind, das umfasst einen INdex und einen Namen
			if ((iSubgraphIndex.getValue()==-1)||(iSubgraphName.equals("")))
			{
				String message = new String();
				if (chSubgraph ==null)
					message = "<html><p>Erstellen des Untergraphen nicht m"+main.CONST.html_oe+"glich.";
				else
					message = "<html><p>"+main.CONST.html_Ae+"ndern des Untergraphen nicht m"+main.CONST.html_oe+"glich.";
				message+="<br><br>Einige Felder nicht ausgef"+main.CONST.html_ue+"llt.</p></hmtl>";
				JOptionPane.showMessageDialog(this,message, "Fehler", JOptionPane.ERROR_MESSAGE);
				return;
			}
			// Farbe bereits vergeben ?
			boolean colorgone = false;
			Iterator<VSubgraph> siter = subgraphs.getIterator();
			while (siter.hasNext())
			{
				if (siter.next().getColor().equals(Colorfield.getBackground())) //Farbe vergeben!
						colorgone = true;
			}
			int SetIndex = iSubgraphIndex.getValue();
			GraphMessage startblock;
			if (chSubgraph==null)
				startblock = new GraphMessage(GraphConstraints.SUBGRAPH, SetIndex, GraphConstraints.ADDITION|GraphConstraints.BLOCK_START, allelements);
			else
			{
				if (SetIndex!=oldindex) //Index modify
					startblock = new GraphMessage(GraphConstraints.SUBGRAPH, GraphConstraints.UPDATE|GraphConstraints.BLOCK_START,allelements);
				else
					startblock = new GraphMessage(GraphConstraints.SUBGRAPH, SetIndex, GraphConstraints.UPDATE|GraphConstraints.BLOCK_START, allelements);
			}	
			//TESTS
			//1. Falls der Graph neu ist
			if (chSubgraph==null) //neuer Untergraph, index testen
			{
				//Index bereits vergeben ?
				if (subgraphs.get(SetIndex)!=null) //So einen gibt es schon
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
				//Falls sich der UGindex geaendert hat darf dieser nicht vergeben sein
				if ((subgraphs.get(SetIndex)!=null)&&(SetIndex!=oldindex)) //So einen gibt es schon
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
				subgraphs.remove(oldindex);
			}
			//Und (im zweiten Fall neu, sonst allgemein) einfuegen
			//Sonst geht alles seiner Wege und wir fuegen den Untergraphen ein
			VSubgraph vs = new VSubgraph(SetIndex,Colorfield.getBackground());
			MSubgraph ms = new MSubgraph(SetIndex,iSubgraphName.getText());
			subgraphs.add(vs, ms);
			//Einfuegen der Knoten und Kanten in den Untergraphen
			//Kanten
			int temp = 0;
			for (int i=0; i<edgelist.size(); i++) //Works due to generality of the VSubGraphSet for both edges and hyperedges
			{
				if (edgelist.elementAt(i)!=null) //Eine Kante mit diesem Index existiert und sie ist selektiert
				{
					if (edgechecks[temp].isSelected())
						subgraphs.addEdgetoSubgraph(i, SetIndex);
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
						subgraphs.addNodetoSubgraph(i, SetIndex);
					temp ++; //Anzahl Knoten zaehlen
				}	
				
			}
			graphref.pushNotify(new GraphMessage(GraphConstraints.SUBGRAPH,GraphConstraints.BLOCK_END, GraphConstraints.NODE|edgemsg));
			this.dispose();
		}
	}
	
	public void itemStateChanged(ItemEvent event) 
	{
		
	}
}
