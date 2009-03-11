package view;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Vector;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import model.*;
import model.Messages.GraphConstraints;
import model.Messages.GraphMessage;

import java.util.Observable;
import java.util.Observer;

import dialogs.*;
/**
 * Sidebar Containing a Tree with the elements of the actual graph
 * 
 * This Sidebar may only subscribe itself (as Observer) to Classes that ONLY
 * provide GraphMessage-Updates
 * @author Ronny Bergmann
 *
 */
public class GraphTree extends JTree implements TreeSelectionListener, 
												MouseListener, 
												ActionListener,
												Observer
{

	private static final long serialVersionUID = 1L;
	private final int USENODES = 1;
	private final int USEEDGES = 2; //Might also be Hyperedges - it's the same entry-list
	private final int USESUBGRAPHS = 4;
	private DefaultMutableTreeNode root, Kanten, Knoten,Mengen;
	private Vector<String> Knotennamen, Kantennamen, Mengennamen;
	private DefaultTreeModel Daten;
	private VGraph vG;
	private VHyperGraph vhG;
	//Menuesachen
	int selectedPosition; //selecktierte Position im 
	int ParentType; //Vater
	JMenuItem Text,Properties, Delete, SCrop;
	JPopupMenu Menu;
	//Für updates
	/**
	 * Init sidebar to contain a specific VGraph. The Changes of the VGRaph are tracked
	 * @param Graph
	 */
	public GraphTree(VGraphInterface Graph)
	{
		super();
		root = new DefaultMutableTreeNode("r00t");
		//Graph
		if (Graph.getType()==VGraphInterface.GRAPH)
		{
			vG = (VGraph)Graph;
			vG.addObserver(this);
			Kanten = new DefaultMutableTreeNode("Kanten");
			root.add(Kanten);
			vhG=null; //to be secure
		}
		else if (Graph.getType()==VGraphInterface.HYPERGRAPH)
		{
			vhG = (VHyperGraph)Graph;
			vhG.addObserver(this);
			Kanten = new DefaultMutableTreeNode("Hyperkanten");
			root.add(Kanten);
			vG=null; 
		}
		Knoten = new DefaultMutableTreeNode("Knoten");
		
		root.add(Knoten);
		//Mengen
		Mengen = new DefaultMutableTreeNode("Untergraphen");
		//Tests
		root.add(Mengen);
		Daten = new DefaultTreeModel(root);
		//Menuesachen
		Text = new JMenuItem("-Data-");
		Text.setEnabled(false);
		Properties = new JMenuItem("Eigenschaften...");
		Properties.addActionListener(this);
		Delete = new JMenuItem("L"+main.CONST.utf8_oe+"schen");
		Delete.addActionListener(this);
		Menu = new JPopupMenu();
		Menu.add(Text);
		Menu.add(Properties);
		Menu.add(Delete);
		this.setModel(Daten);
		this.setVisible(true);
		setRootVisible(false);
		addTreeSelectionListener(this);
		addMouseListener(this);
		
		updateNodes();
		updateEdges();
		updateSubgraphs();
	}
	/**
	 * Force an Update of the NodeList
	 */
	public void updateNodes()
	{
		Vector<String> nodenames = new Vector<String>();
		if (vG!=null)
			nodenames = vG.getMathGraph().modifyNodes.getNames();
		else if (vhG!=null)
			nodenames = vhG.getMathGraph().modifyNodes.getNames();
		Knoten.removeAllChildren();
		for (int i=0; i<nodenames.size(); i++)
		{
			if (nodenames.elementAt(i)!=null) //Ein Knoten mit dem Index existiert
			{
				DefaultMutableTreeNode t = new DefaultMutableTreeNode(nodenames.elementAt(i)+" (#"+i+")");
				Knoten.add(t);
			}
		}
		//Zur Ruecktransformation zum Index bei Auswahl
		Knotennamen = nodenames;
		this.updateUI();
		this.revalidate();
		this.validate();
	}
	/**
	 * Force an Update of the Edgelist
	 */
	public void updateEdges()
	{
		Vector<String> edgenames = new Vector<String>();
		if (vG!=null)
			edgenames = vG.getMathGraph().modifyEdges.getNames();
		else if (vhG!=null)
			edgenames = vhG.getMathGraph().modifyHyperEdges.getNames();

		Kanten.removeAllChildren();
		for (int i=0; i<edgenames.size(); i++)
		{
			if (edgenames.elementAt(i)!=null) //Eine kante mit dem Index existiert
			{
				DefaultMutableTreeNode t = new DefaultMutableTreeNode(edgenames.elementAt(i));
				Kanten.add(t);
			}
		}
		//Zur Ruecktransformation zum Index bei Auswahl
		Kantennamen = edgenames;
		this.updateUI();
		this.revalidate();
		this.validate();
	}
	/**
	 * Force an Update of the Subgraph-List
	 */
	public void updateSubgraphs()
	{
		Vector<String> subgraphnames = new Vector<String>();
		if (vG!=null)
			subgraphnames = vG.getMathGraph().modifySubgraphs.getNames();
		else if (vhG!=null)
			subgraphnames = vhG.getMathGraph().modifySubgraphs.getNames();
		Mengen.removeAllChildren();
		for (int i=0; i<subgraphnames.size(); i++)
		{
			if (subgraphnames.elementAt(i)!=null) //Ein set mit dem Index existiert
			{
				DefaultMutableTreeNode t = new DefaultMutableTreeNode(subgraphnames.elementAt(i));
				Mengen.add(t);
			}
		}
		//Zur Ruecktransformation zum Index bei Auswahl
		Mengennamen = subgraphnames;
		this.updateUI();
		this.revalidate();
		this.validate();
	}
  
	//
	// TreeListener-Actions
	//
	
	public void valueChanged(TreeSelectionEvent e){}
	
	public void mouseClicked(MouseEvent e) {}
	
	public void mouseEntered(MouseEvent e) {}
	
	public void mouseExited(MouseEvent e) {}
	
	public void mousePressed(MouseEvent e)
	{
	    if (e.isPopupTrigger()) 
	    	popuphandling(e);
	}
	
	public void mouseReleased(MouseEvent e) 
	{
	    if (e.isPopupTrigger()) 
	    	popuphandling(e);
	}

	private void popuphandling(MouseEvent e)
    {
		TreePath selPath = getPathForLocation(e.getX(), e.getY()); 
		DefaultMutableTreeNode selectedNode = null;
		try { selectedNode = (DefaultMutableTreeNode)selPath.getLastPathComponent(); }
		catch (Exception E){return;} 
		setSelectionPath(selPath);
		selectedPosition = selectedNode.getParent().getIndex(selectedNode); //der wievielte Child Knoten man ist
		if (selectedNode.getParent().toString().equals("Knoten"))
		{
			ParentType = USENODES;
			int index = StringPos2Index(USENODES,selectedPosition);
			if (vG!=null)
				Text.setText(vG.getMathGraph().modifyNodes.get(index).name);
			else if (vhG!=null)
				Text.setText(vhG.getMathGraph().modifyNodes.get(index).name);
		}
		else if (selectedNode.getParent().toString().equals("Kanten"))
		{
			ParentType = USEEDGES;
			int index = StringPos2Index(USEEDGES,selectedPosition);
			if (vG!=null)
			{
				MEdge me = vG.getMathGraph().modifyEdges.get(index);
				String t = me.StartIndex+" -";
				if (vG.getMathGraph().isDirected()) t+="> "; 
				t+=""+me.EndIndex;
				Text.setText(t);
			}
		}
		else if (selectedNode.getParent().toString().equals("Hyperkanten"))
		{
			ParentType = USEEDGES;
			int index = StringPos2Index(USEEDGES,selectedPosition);
			if (vhG!=null)
				Text.setText(vhG.getMathGraph().modifyHyperEdges.get(index).name);
		}
		else if (selectedNode.getParent().toString().equals("Untergraphen"))
		{
			//System.err.println("")
			ParentType = USESUBGRAPHS;
			int index = StringPos2Index(USESUBGRAPHS,selectedPosition);
			if (vG!=null)
				Text.setText(vG.getMathGraph().modifySubgraphs.get(index).getName());
			else if (vhG!=null)
				Text.setText(vhG.getMathGraph().modifySubgraphs.get(index).getName());			
		}
		else //sonst kein menu anzeigen
		{
			return;
		}
		Menu.show(e.getComponent(), e.getX(), e.getY());
	//	JOptionPane.showMessageDialog(this,message, "Fehler", JOptionPane.INFORMATION_MESSAGE);	
    }
	
	public void actionPerformed(ActionEvent e) 
	{
		if (e.getSource()==Properties)
		{
			switch (ParentType) //Wo war das zuletzt
			{
				case USENODES : {
					if (vG!=null)	
						new JNodeDialog(vG.modifyNodes.get(StringPos2Index(ParentType,selectedPosition)),vG); 
					else if (vhG!=null)
						new JNodeDialog(vhG.modifyNodes.get(StringPos2Index(ParentType,selectedPosition)),vhG); 
					break;}
				case USEEDGES : {
					if (vG!=null)
						new JEdgeDialog(vG.modifyEdges.get(StringPos2Index(ParentType,selectedPosition)),vG); 
					else if (vhG!=null)
						new JHyperEdgeDialog(vhG.modifyHyperEdges.get(StringPos2Index(ParentType,selectedPosition)));
					break;
					}
				case USESUBGRAPHS :
				{
					if (vG!=null)	
						new JSubgraphDialog(vG.modifySubgraphs.get(StringPos2Index(ParentType,selectedPosition)),vG);
					else if (vhG!=null)
						new JSubgraphDialog(vhG.modifySubgraphs.get(StringPos2Index(ParentType,selectedPosition)),vhG);
					break;
				}
				default : {return;}
			}
		}
		else if (e.getSource()==Delete)
		{
			String msg = "<html>Wollen Sie wirklich ";
			switch (ParentType) //Wo war das zuletzt
			{
				case USENODES : {msg +="den Knoten <br> "+Knotennamen.elementAt(selectedPosition+1)+"<br>l"+main.CONST.html_oe+"schen ?"; break;}
				case USEEDGES : {
							msg +="Die ";
							if (vG!=null)
								msg+="Kante ";
							else if (vhG!=null)
								msg+="Hyperkante ";
							msg +="<br>"+Kantennamen.elementAt(selectedPosition+1)+"<br>l"+main.CONST.html_oe+"schen ?"; 
						break;}
				case USESUBGRAPHS : {msg +="den Untergraphen "+Mengennamen.elementAt(selectedPosition+1)+"<br>l"+main.CONST.html_oe+"schen ?<br>(Knoten und Kanten bleiben bestehen)"; break;}
				default : {return;}
			}
			msg +="</html>";
			int answer  = JOptionPane.showConfirmDialog(this,msg,"L"+main.CONST.html_oe+"schen best"+main.CONST.html_ae+"tigen", JOptionPane.YES_NO_OPTION); 
			   if ( answer == JOptionPane.YES_OPTION ) //wirklich löschen !
			   {
				   switch (ParentType) //Wo war das zuletzt
					{
						case USENODES : {
							if (vG!=null)
								vG.modifyNodes.remove(StringPos2Index(ParentType,selectedPosition));
							else if (vhG!=null)
								vhG.modifyNodes.remove(StringPos2Index(ParentType,selectedPosition));							
							updateNodes();
							updateEdges();
						break;}
						case USEEDGES : {
							if (vG!=null)
								vG.modifyEdges.remove(StringPos2Index(ParentType,selectedPosition));
							else
								vhG.modifyHyperEdges.remove(StringPos2Index(ParentType,selectedPosition)); 
							updateEdges();
							break;}
						case USESUBGRAPHS : {
							if (vG!=null)
								vG.modifySubgraphs.remove(StringPos2Index(ParentType,selectedPosition)); 
							else if (vhG!=null)	
								vhG.modifySubgraphs.remove(StringPos2Index(ParentType,selectedPosition));
							updateSubgraphs(); 									
								break;}
						default : {return;}
					}
			   }
		}	
	}
	/**
	 * Return the Index of an Graph-Element based on the Index it is set in the List
	 * @param type
	 * @param pos
	 * @return
	 */
	private int StringPos2Index(int type, int pos)
	{
		Vector<String> s;
		switch (type)
		{
			case USENODES : {s = Knotennamen; break;}
			case USEEDGES : {s = Kantennamen; break;}
			case USESUBGRAPHS : {s = Mengennamen; break;}
			default : {return 0;}
		}
		int index=0;
		//Suche den #index Nr pos, der nicht 0 ist , 
		for (int i=0; i<=pos; i++)
		{
			if (i>0)
				index++;
			while(s.elementAt(index)==null)
			{
				index++;
			}
		}
		return index;
	}

	public void update(Observable o, Object arg)
	{
		GraphMessage m = (GraphMessage)arg;
		if (m==null)
			return;
		if ((o.equals(vG))||(o.equals(vhG))) //Der Graph wurde aktualisiert und auch echt ein String gegeben, der Auftr�ge enth�lt
		{
			if ((m.getAffectedElementTypes()&GraphConstraints.NODE)==GraphConstraints.NODE) //Ein Knoten ist beteiligt
			{
				updateNodes();
				updateEdges();
			}
			if ((m.getAffectedElementTypes()&GraphConstraints.EDGE)==GraphConstraints.EDGE) //Kanten beteiligt
				updateEdges();
			if ((m.getAffectedElementTypes()&GraphConstraints.HYPEREDGE)==GraphConstraints.HYPEREDGE) //Kanten beteiligt
				updateEdges();
			if ((m.getAffectedElementTypes()&GraphConstraints.SUBGRAPH)==GraphConstraints.SUBGRAPH) //Sets beteiligt
				updateSubgraphs();
			if ((m.getAffectedElementTypes()&GraphConstraints.SELECTION)==GraphConstraints.SELECTION)
			{
				//updateSelection();
			}
		}	

	}

}