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

import model.GraphMessage;
import model.MEdge;
import model.VGraph;

import java.util.Observable;
import java.util.Observer;

import dialogs.*;
/**
 * Sidebar Containing a Tree with the elements of the actual graph
 * 
 * @author ronny
 *
 */
public class GraphTree extends JTree implements TreeSelectionListener, 
												MouseListener, 
												ActionListener,
												Observer
{

	private static final long serialVersionUID = 1L;
	private final int USENODES = 1;
	private final int USEEDGES = 2;
	private final int USESETS = 3;
	private DefaultMutableTreeNode root, Kanten, Knoten,Mengen;
	private Vector<String> Knotennamen, Kantennamen, Mengennamen;
	private DefaultTreeModel Daten;
	
	//Menuesachen
	int selectedPosition; //selecktierte Position im 
	int ParentType; //Vater
	JMenuItem Text,Properties, Delete, SCrop;
	JPopupMenu Menu;
	//Für updates
	private VGraph vG;
	/**
	 * Init sidebar to contain a specific VGraph. The Changes of the VGRaph are tracked
	 * @param Graph
	 */
	public GraphTree(VGraph Graph)
	{
		super();
		vG = Graph;
		vG.addObserver(this); //Beim Graphen als Observer anmelden
		root = new DefaultMutableTreeNode("r00t");
		//Graph
		Kanten = new DefaultMutableTreeNode("Kanten");
		root.add(Kanten);
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
		updateSets();
	}
	/**
	 * Force an Update of the NodeList
	 */
	public void updateNodes()
	{
		Vector<String> nodenames = vG.getNodeNames();
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
		Vector<String> edges = vG.getEdgeNames();
		Kanten.removeAllChildren();
		for (int i=0; i<edges.size(); i++)
		{
			if (edges.elementAt(i)!=null) //Eine kante mit dem Index existiert
			{
				DefaultMutableTreeNode t = new DefaultMutableTreeNode(edges.elementAt(i));
				Kanten.add(t);
			}
		}
		//Zur Ruecktransformation zum Index bei Auswahl
		Kantennamen = edges;
		this.updateUI();
		this.revalidate();
		this.validate();
	}
	/**
	 * Force an Update of the SubSet-List
	 */
	public void updateSets()
	{
		Vector<String> Sets = vG.getSetNames();
		Mengen.removeAllChildren();
		for (int i=0; i<Sets.size(); i++)
		{
			if (Sets.elementAt(i)!=null) //Ein set mit dem Index existiert
			{
				DefaultMutableTreeNode t = new DefaultMutableTreeNode(Sets.elementAt(i));
				Mengen.add(t);
			}
		}
		//Zur Ruecktransformation zum Index bei Auswahl
		Mengennamen = Sets;
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
			Text.setText(vG.getMathGraph().getNodeName(StringPos2Index(USENODES,selectedPosition)));
		}
		else if (selectedNode.getParent().toString().equals("Kanten"))
		{
			ParentType = USEEDGES;
			MEdge me = vG.getMathGraph().getEdge(StringPos2Index(USEEDGES,selectedPosition));
			String t = me.StartIndex+" -";
			if (vG.getMathGraph().isDirected()) t+="> "; 
			t+=""+me.EndIndex;
			Text.setText(t);
		}
		else if (selectedNode.getParent().toString().equals("Untergraphen"))
		{
			//System.err.println("")
			ParentType = USESETS;
			Text.setText(vG.getSetNames().elementAt(StringPos2Index(USESETS,selectedPosition)));
		}
		else if (true) //sonst kein menu anzeigen
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
				case USENODES : {new JNodeDialog(vG.getNode(StringPos2Index(ParentType,selectedPosition)),vG); break;}
				case USEEDGES : {new JEdgeDialog(vG.getEdge(StringPos2Index(ParentType,selectedPosition)),vG); break;}
				case USESETS : {new JSubSetDialog(vG.getSubSet(StringPos2Index(ParentType,selectedPosition)),vG); break;}
				default : {return;}
			}
		}
		else if (e.getSource()==Delete)
		{
			String msg = "<html>Wollen Sie wirklich ";
			switch (ParentType) //Wo war das zuletzt
			{
				case USENODES : {msg +="den Knoten <br> "+Knotennamen.elementAt(selectedPosition+1)+"<br>l"+main.CONST.html_oe+"schen ?"; break;}
				case USEEDGES : {msg +="die Kante <br>"+Kantennamen.elementAt(selectedPosition+1)+"<br>l"+main.CONST.html_oe+"schen ?"; break;}
				case USESETS : {msg +="den Untergraphen "+Mengennamen.elementAt(selectedPosition+1)+"<br>l"+main.CONST.html_oe+"schen ?<br>(Knoten und Kanten bleiben bestehen)"; break;}
				default : {return;}
			}
			msg +="</html>";
			int answer  = JOptionPane.showConfirmDialog(this,msg,"L"+main.CONST.html_oe+"schen best"+main.CONST.html_ae+"tigen", JOptionPane.YES_NO_OPTION); 
			   if ( answer == JOptionPane.YES_OPTION ) //wirklich löschen !
			   {
				   switch (ParentType) //Wo war das zuletzt
					{
						case USENODES : {vG.removeNode(StringPos2Index(ParentType,selectedPosition)); updateNodes(); updateEdges(); break;}
						case USEEDGES : {vG.removeEdge(StringPos2Index(ParentType,selectedPosition)); updateEdges(); break;}
						case USESETS : {vG.removeSubSet(StringPos2Index(ParentType,selectedPosition)); updateSets(); break;}
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
			case USESETS : {s = Mengennamen; break;}
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
		if (o.equals(vG)&&(((GraphMessage)arg)!=null)) //Der Graph wurde aktualisiert und auch echt ein String gegeben, der Auftr�ge enth�lt
		{
			GraphMessage m = (GraphMessage)arg;
			if ((m.getAffectedTypes()&GraphMessage.NODE)==GraphMessage.NODE) //Ein Knoten ist beteiligt
			{
				updateNodes();
				updateEdges();
			}
			if ((m.getAffectedTypes()&GraphMessage.EDGE)==GraphMessage.EDGE) //Kanten beteiligt
			{
				updateEdges();
			}
			if ((m.getAffectedTypes()&GraphMessage.SUBSET)==GraphMessage.SUBSET) //Sets beteiligt
			{
				updateSets();
			}
			if ((m.getAffectedTypes()&GraphMessage.SELECTION)==GraphMessage.SELECTION)
			{
				//updateSelection();
			}
		}	
	}
}