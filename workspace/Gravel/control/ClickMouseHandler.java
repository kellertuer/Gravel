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

import dialogs.JEdgeDialog;
import dialogs.JNodeDialog;
import dialogs.JSubSetDialog;

import model.GraphMessage;
import model.MGraph;
import model.VEdge;
import model.VGraph;
import model.VItem;
import model.VNode;
import model.VSubSet;
/**
 * Super class for the mouse handler for mouseclicks
 * this class is abstract, and is implemented by every mouse mode that is available in gravel
 * 
 * This abstract superclass is also an observer. it is an oberserv the VGraph
 * 
 * @author ronny
 *
 */
public abstract class ClickMouseHandler implements MouseListener, ActionListener, Observer
{
	// MenüPunkte
	private Vector<JMenuItem> vSubSetNMenus; // Ein JItem für jedes Set
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
	VGraph vg;
	/**
	 * Initializes the Mouse Click Handler to a specifig VGraph, where the mouse actions are involved
	 * 
	 * this Handler subscribes itself as an Observer of the VGraph to update the Subsetlists
	 * 
	 * @param g
	 */
	public ClickMouseHandler(VGraph g)
	{
		vg = g;
		gp = GeneralPreferences.getInstance();
		g.addObserver(this);
		initPopups();
		updateSubSetList();
	}
	/**
	 * Init all Popup Menus, for the background, the edges and the nodes. 
	 * This involves the sets ob subsets a node/edge belongs and doesn't belong to
	 *
	 */
	private void initPopups() {
		BgPopup = new JPopupMenu();
		addNode = new JMenuItem("Neuer Knoten...");
		addNode.addActionListener(this);
		BgPopup.add(addNode);
		addSet = new JMenuItem("Neuer Untergraph...");
		addSet.addActionListener(this);
		vSubSetNMenus = new Vector<JMenuItem>();
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
		NDelSelection.setEnabled(vg.selectedNodeExists()||vg.selectedEdgeExists());
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
		EDelSelection.setEnabled(vg.selectedNodeExists()||vg.selectedEdgeExists());
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
		Iterator<JMenuItem> iter = vSubSetNMenus.iterator(); // durchlaufe alle SetMenüeinträge
		while (iter.hasNext()) {
			JMenuItem t = iter.next();
			if (t!=null) //Falls ein Eintrag leer ist, behandle ihn nicht.
			{
				//Sets fangen mit 1 an die Menüeinträge mit 0
				if (!(vg.SubSetcontainsNode(i,vSubSetNMenus.indexOf(t)))) // Knoten nicht enthalten -> hinzufügenMenü
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
		Iterator<JMenuItem> iter = vSubSetNMenus.iterator(); // durchlaufe alle SetMenueeintraege
		while (iter.hasNext()) {
			JMenuItem t = iter.next();
			if (t!=null) //Falls ein Index nixht vergeben ist
			{
				if (!(vg.SubSetcontainsEdge(i, vSubSetNMenus.indexOf(t)))) // Knoten nicht enthalten -> hinzufuegenMenue
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
		Point p = new Point(Math.round(e.getPoint().x/((float)gp.getIntValue("vgraphic.zoom")/100)),Math.round(e.getPoint().y/((float)gp.getIntValue("vgraphic.zoom")/100))); //rausrechnen
		if ((e.getClickCount()==2)&&(e.getModifiers() == MouseEvent.BUTTON1_MASK))
		{ //Double Click on Node
			VNode dcn = vg.getNodeinRange(p);
			if (dcn!=null) //Doubleclick really on Node
			{
				new JNodeDialog(dcn,vg);
			}
		}
		if ((e.getModifiers() == MouseEvent.BUTTON3_MASK) || (e.getModifiers() == MouseEvent.BUTTON1_MASK+MouseEvent.CTRL_MASK)) // mit rechts oder strg links
		{
			VNode r = vg.getNodeinRange(p);
			VEdge s = vg.getEdgeinRange(p,2.0);
			if (r != null) {
				updateNodeSetList(r.getIndex());
				Nname.setText(vg.getNodeName(r.getIndex()) + " - (#" + r.getIndex() + ")");
				NaddEdgesTo.setEnabled(vg.selectedNodeExists());
				NaddEdgesFrom.setEnabled(vg.selectedNodeExists());
				NDelSelection.setEnabled(vg.selectedEdgeExists()||vg.selectedNodeExists());
				selectedNode = r;
				NodePopup.show(e.getComponent(), e.getX(), e.getY());
			} 
			else if (s!=null) //Kante rechts angeklickt
			{
				selectedEdge = s;
				updateEdgeSetList(s.getIndex());
				Ename.setText("Value : "+vg.getEdgeProperties(s.getIndex()).elementAt(MGraph.EDGEVALUE)+" - Index : "+s.getIndex()+"");
				EDelSelection.setEnabled(vg.selectedEdgeExists()||vg.selectedNodeExists());
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
			VNode r = vg.getNodeinRange(p);
			if (r != null) //Clicked on a node -> Select it
			{
				if ((r.getSelectedStatus()&VItem.SELECTED)!=VItem.SELECTED)
					r.setSelectedStatus(VItem.SELECTED);
				else
					r.deselect();
				vg.pushNotify(new GraphMessage(GraphMessage.SELECTION,GraphMessage.UPDATE));
			} else {
				VEdge s = vg.getEdgeinRange(p,2.0);
				if (s != null) 
				{
					if ((s.getSelectedStatus()&VItem.SELECTED)!=VItem.SELECTED)
						s.setSelectedStatus(VItem.SELECTED);
					else
						s.deselect();
					vg.pushNotify(new GraphMessage(GraphMessage.SELECTION,GraphMessage.UPDATE));
				}
			}
		}
	}
	/**
	 * If the number of subsets in the VGraph has changed, the SubSetMenus must be updated, to keep all
	 * known Subsets in here. so this method is evoked from the update method
	 *
	 */
	public void updateSubSetList()
	{
		vSubSetNMenus.removeAllElements(); //Untergraphen-Menues aktualisieren
		Iterator<VSubSet> subsetiter = vg.getSubSetIterator();
		while (subsetiter.hasNext())
		{
			VSubSet actual = subsetiter.next();
			JMenuItem t = new JMenuItem(vg.getSubSetName(actual.getIndex()));
			t.addActionListener(this);
			if ((actual.getIndex()+1)>vSubSetNMenus.size())
			{
				vSubSetNMenus.setSize(actual.getIndex()+1);
			}
			vSubSetNMenus.set(actual.getIndex(),t);
		}
	}	
	/**
	 * update is inherited from the Observer Interface and is called if the Graph changes.
	 * Then the subsetlist ist updated
	 */
	public void update(Observable o, Object arg) {
		if (((GraphMessage)arg)!=null)
		{ //If we get an GraphMessage and a SUBSET is AFFECTED
			if ((((GraphMessage)arg).getAffectedTypes()&GraphMessage.SUBSET)==GraphMessage.SUBSET)
				updateSubSetList();
		}
	}
	/**
	 * inherited from the Action LIstener to handle all clicks on the popup menus and call the specific functions
	 * 
	 */
	public void actionPerformed(ActionEvent e) {
		//Hintergrund: Knoten hinzuf�gen an Mausposition mit #i als Namen
		if (e.getSource() == addNode) {
			int newindex = vg.getNextNodeIndex();
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
				Iterator<VSubSet> subsetiter = vg.getSubSetIterator();
				while (subsetiter.hasNext())
				{
					if (subsetiter.next().getColor().equals(c)) //Farbe vergeben!
							colorgone = true;
				}
			} while(colorgone);
			new JSubSetDialog(vg.getNextSubSetIndex(),gp.getSubSetName(vg.getNextSubSetIndex()),c,vg);
		}
		//KnotenMen� :  Hinzuf�gen zu und Entfernen von einem Set
		//KantenMen� :  Hinzuf�gen zu und Entfernen von einem Set
		if (vSubSetNMenus.contains(e.getSource())) // dann wurde ein Mengenmen�punkt angew�hlt
		{
			JMenuItem t = (JMenuItem) (e.getSource()); // wenn der enthalten ist, l��t er sich auch casten
			//VSubSet chosen = vG.getvSubSets.get(vSubSetNMenus.indexOf(t));
			//In diesem Fall wars ein Knotenmen� (selectedNode != null)
			if (selectedNode!=null)
			{
				if (vg.SubSetcontainsNode(selectedNode.getIndex(),vSubSetNMenus.indexOf(t))) // gew�hlt und enth�lt den Knoten => entfernen
				{
					vg.removeNodefromSubSet(selectedNode.getIndex(),vSubSetNMenus.indexOf(t));
				} else {
					vg.addNodetoSubSet(selectedNode.getIndex(),vSubSetNMenus.indexOf(t));
				}
			}
			//In diesem Fall wars eine Kante
			if (selectedEdge!=null)
			{
				if (vg.SubSetcontainsEdge(selectedEdge.getIndex(),vSubSetNMenus.indexOf(t))) // gew�hlt und enth�lt den Knoten => entfernen
				{
					vg.removeEdgefromSubSet(selectedEdge.getIndex(),vSubSetNMenus.indexOf(t));
				} else {
					vg.addEdgetoSubSet(selectedEdge.getIndex(),vSubSetNMenus.indexOf(t));
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
			vg.removeNode(selectedNode.getIndex());
		}
		//KantenMen� : L�schen
		if (e.getSource()==Edelete)
		{
			vg.removeEdge(selectedEdge.getIndex());
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
