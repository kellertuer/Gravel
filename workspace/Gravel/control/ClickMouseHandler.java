package control;

import io.GeneralPreferences;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import view.VGraphic;

import dialogs.JEdgeDialog;
import dialogs.JNodeDialog;
import dialogs.JSubgraphDialog;

import model.MEdge;
import model.VEdge;
import model.VGraph;
import model.VItem;
import model.VNode;
import model.VSubgraph;
import model.Messages.GraphConstraints;
import model.Messages.GraphMessage;
/**
 * Super class for the mouse handler for mouseclicks
 * this class is abstract, and is implemented by every mouse mode that is available in gravel
 * 
 * This abstract superclass is also an observer. it is an oberserv the VGraph
 * 
 * This Observer may only subscribe to Observables that send GraphMessages
 * @author ronny
 *
 */
public abstract class ClickMouseHandler implements MouseListener, ActionListener, Observer
{
	// MenüPunkte
	private Vector<JMenuItem> vSubgraphNMenus; // Ein JItem für jedes Set
	private JPopupMenu BgPopup; // HintergrundPopupMenu
	private JMenuItem addNode, addSet; // HintergrundMenüpunkte
	private JPopupMenu NodePopup; // KnotenPopupMenu
	private JMenuItem Nproperties, Ndelete, NaddEdgesTo, NaddEdgesFrom, Nname, NDelSelection;
	private JMenu NaddtoSet, NremfromSet; // Knotenuntermenüs
	private JPopupMenu EdgePopup;
	private JMenuItem Eproperties, Edelete, Ename, EDelSelection;
	private JMenu EaddtoSet, EremfromSet; // Kantenuntermenüs
	
	private VNode selectedNode=null; //Menüzugriff
	private VEdge selectedEdge=null; //als Orientierung
	
	private GeneralPreferences gp;
	private Point CreationPoint;
	VGraphic vgc;
	VGraph vg;
	/**
	 * Initializes the Mouse Click Handler to a specifig VGraph, where the mouse actions are involved
	 * 
	 * this Handler subscribes itself as an Observer of the VGraph to update the Subgraphlist
	 * 
	 * @param g
	 */
	public ClickMouseHandler(VGraphic g)
	{
		vgc= g;
		gp = GeneralPreferences.getInstance();
		vg = vgc.getVGraph();
		vg.addObserver(this); //Sub
		initPopups();
		updateSubgraphList();
	}
	/**
	 * Init all Popup Menus, for the background, the edges and the nodes. 
	 * This involves the subgraphsets, a node/edge belongs and doesn't belong to
	 *
	 */
	private void initPopups() {
		BgPopup = new JPopupMenu();
		addNode = new JMenuItem("Neuer Knoten...");
		addNode.addActionListener(this);
		BgPopup.add(addNode);
		addSet = new JMenuItem("Neuer Untergraph...");
		addSet.addActionListener(this);
		vSubgraphNMenus = new Vector<JMenuItem>();
		BgPopup.add(addSet);

		/* Build right mouseclick Node Menu (empty)*/
		Nproperties = new JMenuItem("Eigenschaften...");
		Nproperties.addActionListener(this);
		Nname = new JMenuItem("-none-");
		Nname.setEnabled(false);
		Ndelete = new JMenuItem("Löschen");
		Ndelete.addActionListener(this);
		NaddEdgesTo = new JMenuItem("Kanten zu selektierten Knoten");
		NaddEdgesTo.addActionListener(this);
		NaddEdgesFrom = new JMenuItem("Kanten von selektierten Knoten");
		NaddEdgesFrom.addActionListener(this);
		NaddtoSet = new JMenu("Knoten hinzufügen zu");
		NaddtoSet.setEnabled(false);
		NaddtoSet.addActionListener(this);
		NremfromSet = new JMenu("Knoten entfernen aus");
		NremfromSet.setEnabled(false);
		NremfromSet.addActionListener(this);
		NDelSelection = new JMenuItem("Auswahl löschen");
		NDelSelection.setEnabled(vg.modifyNodes.hasSelection()||vg.modifyEdges.hasSelection());
		NDelSelection.addActionListener(this);	
		NodePopup = new JPopupMenu();
		NodePopup.add(Nname);
		NodePopup.addSeparator();
		NodePopup.add(NaddEdgesTo);
		NodePopup.add(NaddEdgesFrom);
		NodePopup.add(Nproperties);
		NodePopup.add(NaddtoSet);
		NodePopup.add(NremfromSet);
		NodePopup.addSeparator();
		NodePopup.add(Ndelete);
		NodePopup.add(NDelSelection);
		
		/*Build empty Right Mouseclick Edge Menu */
		Ename = new JMenuItem("-name-");
		Ename.setEnabled(false);
		Eproperties = new JMenuItem("Eigenschaften...");
		Eproperties.addActionListener(this);
		Edelete = new JMenuItem("Löschen");
		Edelete.addActionListener(this);
		EaddtoSet = new JMenu("Kante hinzufügen zu");
		EaddtoSet.setEnabled(false);
		EaddtoSet.addActionListener(this);
		EremfromSet = new JMenu("Kante entfernen aus");
		EremfromSet.setEnabled(false);
		EremfromSet.addActionListener(this);
		EDelSelection = new JMenuItem("Auswahl löschen");
		EDelSelection.setEnabled(vg.modifyNodes.hasSelection()||vg.modifyEdges.hasSelection());
		EDelSelection.addActionListener(this);	
		EdgePopup = new JPopupMenu();
		EdgePopup.add(Ename);
		EdgePopup.addSeparator();
		EdgePopup.add(Eproperties);
		EdgePopup.add(EaddtoSet);
		EdgePopup.add(EremfromSet);
		EdgePopup.addSeparator();
		EdgePopup.add(Edelete);
		EdgePopup.add(EDelSelection);
		
	}
	/**
	 * Update the list a node belongs to and a node can be removerd from
	 * 
	 * @paramn i the index of the node, for whom the lists should be updated
	 */
	private void updateNodeSetList(int i) 
	{
		//Beide Menüs leeren und disablen
		NaddtoSet.removeAll();
		NaddtoSet.setEnabled(false);
		NremfromSet.removeAll();
		NremfromSet.setEnabled(false);
		Iterator<JMenuItem> iter = vSubgraphNMenus.iterator(); // get all Subgraphs
		while (iter.hasNext()) {
			JMenuItem t = iter.next();
			if (t!=null) //Falls ein Eintrag leer ist, behandle ihn nicht.
			{
				//Sets fangen mit 1 an die Menüeinträge mit 0
				if (!(vg.getMathGraph().modifySubgraphs.get(vSubgraphNMenus.indexOf(t)).containsNode(i))) // Knoten nicht enthalten -> hinzufügenMenü
				{
					NaddtoSet.setEnabled(true); //es gibt einen also gibts auch das Menü
					NaddtoSet.add(t);
				}
				else  // Knoten enthalten -> entfernen anbieten
				{
					NremfromSet.add(t);
					NremfromSet.setEnabled(true);
				}
			}
		}
	}
	/**
	 * Update the lists a node belongs to and it doesn't belong to
	 * 
	 * @param i index of the edge
	 */
	private void updateEdgeSetList(int i) 
	{
		//Beide Menues leeren und disablen
		EaddtoSet.removeAll();
		EaddtoSet.setEnabled(false);
		EremfromSet.removeAll();
		EremfromSet.setEnabled(false);
		Iterator<JMenuItem> iter = vSubgraphNMenus.iterator(); // durchlaufe alle SetMenueeintraege
		while (iter.hasNext()) {
			JMenuItem t = iter.next();
			if (t!=null) //Falls ein Index nixht vergeben ist
			{
				if (!(vg.getMathGraph().modifySubgraphs.get(vSubgraphNMenus.indexOf(t)).containsEdge(i))) // Knoten nicht enthalten -> hinzufuegenMenue
				{
					EaddtoSet.setEnabled(true); //es gibt einen also gibts auch das Menue
					EaddtoSet.add(t);
				}
				else  // Knoten enthalten -> entfernen anbieten
				{
					EremfromSet.add(t);
					EremfromSet.setEnabled(true);
				}
			}
		}
	}
	/**
	 * inherited from the MouseClickHandler - handling mouse clicks
	 * 
	 * shift+left : select additional the edge or node bwloe. if it is selected, deselect it
	 * right : open popup menu on the edge, node or backround
	 * all other clicks are specific for any subclass and are implemented there
	 *  
	 */
	public void mouseClicked(MouseEvent e)
	{
		Point p = new Point(Math.round(e.getPoint().x/((float)vgc.getZoom()/100)),Math.round(e.getPoint().y/((float)vgc.getZoom()/100))); //rausrechnen
		if ((e.getClickCount()==2)&&(e.getModifiers() == MouseEvent.BUTTON1_MASK))
		{ //Double Click on Node
			VNode dcn = vg.modifyNodes.getFirstinRangeOf(p);
			if (dcn!=null) //Doubleclick really on Node
			{
				new JNodeDialog(dcn,vg);
			}
		}
		if ((e.getModifiers() == MouseEvent.BUTTON3_MASK) || (e.getModifiers() == MouseEvent.BUTTON1_MASK+MouseEvent.CTRL_MASK)) // mit rechts oder strg links
		{
			VNode r = vg.modifyNodes.getFirstinRangeOf(p);
			VEdge s = vg.getEdgeinRangeOf(p,2.0*((float)vgc.getZoom()/100));
			if (r != null) {
				updateNodeSetList(r.getIndex());
				Nname.setText(vg.getMathGraph().modifyNodes.get(r.getIndex()).name + " - (#" + r.getIndex() + ")");
				NaddEdgesTo.setEnabled(vg.modifyNodes.hasSelection());
				NaddEdgesFrom.setEnabled(vg.modifyNodes.hasSelection());
				NDelSelection.setEnabled(vg.modifyEdges.hasSelection()||vg.modifyNodes.hasSelection());
				selectedNode = r;
				NodePopup.show(e.getComponent(), e.getX(), e.getY());
			} 
			else if (s!=null) //Kante rechts angeklickt
			{
				selectedEdge = s;
				updateEdgeSetList(s.getIndex());
				MEdge me = vg.getMathGraph().modifyEdges.get(s.getIndex());
				Ename.setText("Value : "+me.Value+" - Index : "+s.getIndex()+"");
				EDelSelection.setEnabled(vg.modifyEdges.hasSelection()||vg.modifyNodes.hasSelection());
				EdgePopup.show(e.getComponent(), e.getX(),e.getY());	
			}
			else //Weder Knoten noch Kante angeklickt -> Background popup
			{
				BgPopup.show(e.getComponent(), e.getX(), e.getY());
				//Position des neuen Knotens
				CreationPoint = p;
			}
		}
		if (e.getModifiers() == MouseEvent.BUTTON1_MASK+MouseEvent.SHIFT_MASK) // mit SHIFTlinks angeklickt, Auswahl erweitern
		{
			VNode r = vg.modifyNodes.getFirstinRangeOf(p);
			if (r != null) //Clicked on a node -> Select it
			{
				if ((r.getSelectedStatus()&VItem.SELECTED)!=VItem.SELECTED)
					r.setSelectedStatus(VItem.SELECTED);
				else
					r.deselect();
				vg.pushNotify(new GraphMessage(GraphConstraints.SELECTION,GraphConstraints.UPDATE));
			} else {
				VEdge s = vg.getEdgeinRangeOf(p,2.0*((float)vgc.getZoom()/100));
				if (s != null) 
				{
					if ((s.getSelectedStatus()&VItem.SELECTED)!=VItem.SELECTED)
						s.setSelectedStatus(VItem.SELECTED);
					else
						s.deselect();
					vg.pushNotify(new GraphMessage(GraphConstraints.SELECTION,GraphConstraints.UPDATE));
				}
			}
		}
	}
	/**
	 * If the number of VSubgraphs in the VGraph has changed, the SubgraphMenus must be updated, to keep all
	 * known Subgraphs in here. so this method is evoked from the update method
	 *
	 */
	public void updateSubgraphList()
	{
		vSubgraphNMenus.removeAllElements(); //Untergraphen-Menues aktualisieren
		Iterator<VSubgraph> siter = vg.modifySubgraphs.getIterator();
		while (siter.hasNext())
		{
			VSubgraph actual = siter.next();
			JMenuItem t = new JMenuItem(vg.getMathGraph().modifySubgraphs.get(actual.getIndex()).getName());
			t.addActionListener(this);
			if ((actual.getIndex()+1)>vSubgraphNMenus.size())
			{
				vSubgraphNMenus.setSize(actual.getIndex()+1);
			}
			vSubgraphNMenus.set(actual.getIndex(),t);
		}
	}	
	/**
	 * update is inherited from the Observer Interface and is called if the Graph changes.
	 * Then the subgraphslist ist updated
	 */
	public void update(Observable o, Object arg)
	{
		GraphMessage m = (GraphMessage)arg;
			if (m!=null)
			{ //If we get an GraphMessage and a SUBGRAPH is AFFECTED
				if ((m.getAffectedElementTypes()&GraphConstraints.SUBGRAPH)==GraphConstraints.SUBGRAPH)
					updateSubgraphList();
			}
	}
	/**
	 * inherited from the Action LIstener to handle all clicks on the popup menus and call the specific functions
	 * 
	 */
	public void actionPerformed(ActionEvent e) {
		//Hintergrund: Knoten hinzuf�gen an Mausposition mit #i als Namen
		if (e.getSource() == addNode) {
			int newindex = vg.getMathGraph().modifyNodes.getNextIndex();
			new JNodeDialog(newindex,gp.getNodeName(newindex),CreationPoint.x,CreationPoint.y,gp.getIntValue("node.size"),vg);
			CreationPoint = new Point(0,0);
		} 
		//Hintergrund: Neues Set
		if (e.getSource() == addSet) 
		{
			boolean colorgone = false;
			Color c = new Color(0,0,0);
			do
			{
				c = Color.getHSBColor((float)Math.random(), (float)Math.random(), (float)Math.random());
				colorgone = false;
				Iterator<VSubgraph> siter = vg.modifySubgraphs.getIterator();
				while (siter.hasNext())
				{
					if (siter.next().getColor().equals(c)) //Farbe vergeben!
							colorgone = true;
				}
			} while(colorgone);
			new JSubgraphDialog(vg.getMathGraph().modifySubgraphs.getNextIndex(),gp.getSubgraphName(vg.getMathGraph().modifySubgraphs.getNextIndex()),c,vg);
		}
		//KnotenMen� :  Hinzuf�gen zu und Entfernen von einem Set
		//KantenMen� :  Hinzuf�gen zu und Entfernen von einem Set
		if (vSubgraphNMenus.contains(e.getSource())) // dann wurde ein Mengenmen�punkt angew�hlt
		{
			JMenuItem t = (JMenuItem) (e.getSource()); // wenn der enthalten ist, l��t er sich auch casten
			if (selectedNode!=null)
			{
				if (vg.getMathGraph().modifySubgraphs.get(vSubgraphNMenus.indexOf(t)).containsNode(selectedNode.getIndex())) // gew�hlt und enth�lt den Knoten => entfernen
				{
					vg.modifySubgraphs.removeNodefromSubgraph(selectedNode.getIndex(), vSubgraphNMenus.indexOf(t));
				} else {
					vg.modifySubgraphs.addNodetoSubgraph(selectedNode.getIndex(), vSubgraphNMenus.indexOf(t));
				}
			}
			//In diesem Fall wars eine Kante
			if (selectedEdge!=null)
			{
				if (vg.getMathGraph().modifySubgraphs.get(vSubgraphNMenus.indexOf(t)).containsEdge(selectedEdge.getIndex())) // gew�hlt und enth�lt den Knoten => entfernen
				{
					vg.modifySubgraphs.removeEdgefromSubgraph(selectedEdge.getIndex(), vSubgraphNMenus.indexOf(t));
				} else {
					vg.modifySubgraphs.addEdgetoSubgraph(selectedEdge.getIndex(), vSubgraphNMenus.indexOf(t));
				}
			}
		} 
		//KnotenMen� : Kanten hinzuf�gen zu allen Selektierten Knoten
		if (e.getSource() == NaddEdgesTo) { // Add Edges to alle selected Nodes
			vg.addEdgestoSelectedNodes(selectedNode);
		} 
		if (e.getSource() == NaddEdgesFrom) { // Add Edges to alle selected Nodes
			vg.addEdgesfromSelectedNodes(selectedNode);
		} 
		//KNotenMen� : KnotenEigenschaftenDialog
		if (e.getSource() == Nproperties) 
		{
			new JNodeDialog(selectedNode,vg);
		} 
		//KnotenMen� : Knoten entfernen
		if (e.getSource() == Ndelete) {
			vg.modifyNodes.remove(selectedNode.getIndex());
		}
		//KantenMen� : L�schen
		if (e.getSource()==Edelete)
		{
			vg.modifyEdges.remove(selectedEdge.getIndex());
		}
		//KantenMen� : Eigenschaften
		if (e.getSource()==Eproperties)
		{
			new JEdgeDialog(selectedEdge,vg);
		}
		//Knoten oder Kantenmenü Auswahl löschen
		if ((e.getSource()==EDelSelection)||(e.getSource()==NDelSelection))
		{
			vg.removeSelection();
		}
		//Setze beide eventuell Men�bezugselemente auf null
		selectedEdge = null;
		selectedNode = null;
	}

}
