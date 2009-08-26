package control;

import io.GeneralPreferences;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.BitSet;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import dialogs.JEdgeDialog;
import dialogs.JHyperEdgeDialog;
import dialogs.JNodeDialog;
import dialogs.JSubgraphDialog;

import model.MEdge;
import model.MHyperEdge;
import model.MSubgraphSet;
import model.VEdge;
import model.VGraph;
import model.VHyperEdge;
import model.VHyperGraph;
import model.VItem;
import model.VNode;
import model.VSubgraph;
import model.VSubgraphSet;
import model.Messages.GraphConstraints;
import model.Messages.GraphMessage;
import view.Gui;
import view.VCommonGraphic;
import view.VGraphic;
import view.VHyperGraphic;
/**
 * Listener for handling Context-Menus
 * 
 * On Background, on Nodes, on Edges or on HyperEdges
 *
 * @author Ronny Bergmann
 * @since 0.4
 */
public class ContextMenuClickListener
		implements
			MouseListener,
			ActionListener,
			Observer {

	private Vector<JMenuItem> vSubgraphNMenus; // Ein JItem für jedes Set
	private JPopupMenu BgPopup; // HintergrundPopupMenu
	private JMenuItem addNode, addSet, BgCreateHyperEdgefromSel; // HintergrundMenüpunkte
	private JPopupMenu NodePopup; // KnotenPopupMenu
	private JMenuItem Nproperties, Ndelete, NaddEdgesTo, NaddEdgesFrom, Nname, NDelSelection,NCreateHyperEdgefromSel;
	private JMenu NaddtoSet, NremfromSet; // Knotenuntermenüs
	private JPopupMenu EdgePopup;
	private JMenuItem Eproperties, Edelete, Ename, EDelSelection;
	private JMenu EaddtoSet, EremfromSet; // Kantenuntermenüs
	private JMenuItem HEShape;
	//Save Any Item that is clicked
	private VNode selectedNode=null; 
	private VEdge selectedEdge=null; 
	private VHyperEdge selectedHyperEdge=null;
	
	private Point PopupCoordinates;
	private GeneralPreferences gp;
	VCommonGraphic vgc;
	VGraph vg=null;
	VHyperGraph vhg=null;

	/**
	 * Init this Selection Listener
	 * @param g
	 */
	public ContextMenuClickListener(VGraphic g)
	{
		vgc= g;
		gp = GeneralPreferences.getInstance();
		vg = g.getGraph();
		vg.addObserver(this); //Sub
		initPopups();
		updateSubgraphList();
	}
	public ContextMenuClickListener(VHyperGraphic g)
	{
		vgc= g;
		gp = GeneralPreferences.getInstance();
		vhg = g.getGraph();
		vhg.addObserver(this); //Sub
		initPopups();
		updateSubgraphList();
	}
	public void removeObservers()
	{
		if (vg!=null)
			vg.deleteObserver(this);
		else if (vhg!=null)
			vhg.deleteObserver(this);
	}
	private void initPopups() {
		initBackgroundPopup();
		initNodePopup();
		initEdgePopup();
	}
	private void initBackgroundPopup()
	{
		BgPopup = new JPopupMenu();
		addNode = new JMenuItem("Neuer Knoten...");
		addNode.addActionListener(this);
		BgPopup.add(addNode);
		addSet = new JMenuItem("Neuer Untergraph...");
		addSet.addActionListener(this);
		vSubgraphNMenus = new Vector<JMenuItem>();
		BgPopup.add(addSet);
		if (vhg!=null)
		{
			BgCreateHyperEdgefromSel = new JMenuItem("Neue Hyperkante...");
			BgCreateHyperEdgefromSel.addActionListener(this);
			BgPopup.add(BgCreateHyperEdgefromSel);
		}
	}
	private void initNodePopup()
	{
		/* Build right mouseclick Node Menu (empty)*/
		Nproperties = new JMenuItem("Eigenschaften...");
		Nproperties.addActionListener(this);
		Nname = new JMenuItem("-none-");
		Nname.setEnabled(false);
		Ndelete = new JMenuItem("Löschen");
		Ndelete.addActionListener(this);
		if (vg!=null) //Normal Graph
		{
			NaddEdgesTo = new JMenuItem("Kanten zu ausgewählten Knoten");
			NaddEdgesTo.addActionListener(this);
			NaddEdgesFrom = new JMenuItem("Kanten von ausgewählten Knoten");
			NaddEdgesFrom.addActionListener(this);
		}
		else if (vhg!=null)
		{
			NCreateHyperEdgefromSel = new JMenuItem("Neue Hyperkante...");
			NCreateHyperEdgefromSel.setEnabled(vhg.modifyNodes.hasSelection());
			NCreateHyperEdgefromSel.addActionListener(this);
		}
		NaddtoSet = new JMenu("Knoten hinzufügen zu");
		NaddtoSet.setEnabled(false);
		NaddtoSet.addActionListener(this);
		NremfromSet = new JMenu("Knoten entfernen aus");
		NremfromSet.setEnabled(false);
		NremfromSet.addActionListener(this);
		NDelSelection = new JMenuItem("Auswahl löschen");
		if (vg!=null)
			NDelSelection.setEnabled(vg.hasSelection());
		else if (vhg!=null)
			NDelSelection.setEnabled(vhg.hasSelection());
		NDelSelection.addActionListener(this);	
		NodePopup = new JPopupMenu();
		NodePopup.add(Nname);
		NodePopup.addSeparator();
		if (vg!=null)
		{
			NodePopup.add(NaddEdgesTo);
			NodePopup.add(NaddEdgesFrom);
		}
		else if (vhg!=null)
			NodePopup.add(NCreateHyperEdgefromSel);
		
		NodePopup.add(Nproperties);
		NodePopup.add(NaddtoSet);
		NodePopup.add(NremfromSet);
		NodePopup.addSeparator();
		NodePopup.add(Ndelete);
		NodePopup.add(NDelSelection);
	}
	private void initEdgePopup()
	{
		/*Build empty Right Mouseclick Edge Menu */
		Ename = new JMenuItem("-name-");
		Ename.setEnabled(false);
		Eproperties = new JMenuItem("Eigenschaften...");
		Eproperties.addActionListener(this);
		if (vhg!=null)
		{
			HEShape = new JMenuItem("Umriss bearbeiten...");
			HEShape.addActionListener(this);
		}
		Edelete = new JMenuItem("Löschen");
		Edelete.addActionListener(this);
		EaddtoSet = new JMenu("Kante hinzufügen zu");
		EaddtoSet.setEnabled(false);
		EaddtoSet.addActionListener(this);
		EremfromSet = new JMenu("Kante entfernen aus");
		EremfromSet.setEnabled(false);
		EremfromSet.addActionListener(this);
		EDelSelection = new JMenuItem("Auswahl löschen");
		if (vg!=null)
			EDelSelection.setEnabled(vg.hasSelection());
		else if (vhg!=null)
			EDelSelection.setEnabled(vhg.hasSelection());
		EDelSelection.addActionListener(this);	

		EdgePopup = new JPopupMenu();
		EdgePopup.add(Ename);
		EdgePopup.addSeparator();
		EdgePopup.add(Eproperties);
		if (vhg!=null)
			EdgePopup.add(HEShape);
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
		MSubgraphSet subgraphs=null;
		if (vg!=null)
			subgraphs = vg.getMathGraph().modifySubgraphs;
		else if (vhg!=null)
			subgraphs = vhg.getMathGraph().modifySubgraphs;
		else
			return; //Just for security
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
				if (!(subgraphs.get(vSubgraphNMenus.indexOf(t)).containsNode(i))) // Knoten nicht enthalten -> hinzufügenMenü
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
	 * @param i index of the (hyper)edge
	 */
	private void updateEdgeSetList(int i) 
	{
		MSubgraphSet subgraphs=null;
		if (vg!=null)
			subgraphs = vg.getMathGraph().modifySubgraphs;
		else if (vhg!=null)
			subgraphs = vhg.getMathGraph().modifySubgraphs;
		else
			return; //Just for security
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
				if (!(subgraphs.get(vSubgraphNMenus.indexOf(t)).containsEdge(i))) // Knoten nicht enthalten -> hinzufuegenMenue
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
	 * If the VSubgraphSet was changed in  in the V(Hyper)Graph has changed, the SubgraphMenus must be updated, to keep all
	 * known Subgraphs in here. so this method is evoked from the update method
	 *
	 */
	public void updateSubgraphList()
	{
		vSubgraphNMenus.removeAllElements(); //Untergraphen-Menues aktualisieren
		Iterator<VSubgraph> siter;
		MSubgraphSet subgraphs;
		if (vg!=null)
		{
			siter = vg.modifySubgraphs.getIterator();
			subgraphs = vg.getMathGraph().modifySubgraphs;
		}
		else if (vhg!=null)
		{
			subgraphs = vhg.getMathGraph().modifySubgraphs;
			siter = vhg.modifySubgraphs.getIterator();
		}
		else
			return;
		while (siter.hasNext())
		{
			VSubgraph actual = siter.next();
			JMenuItem t = new JMenuItem(subgraphs.get(actual.getIndex()).getName());
			t.addActionListener(this);
			if ((actual.getIndex()+1)>vSubgraphNMenus.size())
			{
				vSubgraphNMenus.setSize(actual.getIndex()+1);
			}
			vSubgraphNMenus.set(actual.getIndex(),t);
		}
	}	

	public void mouseClicked(MouseEvent e) {
		//Only react, if Right Click or Strg+Left Click
		if (!((e.getModifiers() == MouseEvent.BUTTON3_MASK) || (e.getModifiers() == MouseEvent.BUTTON1_MASK+MouseEvent.CTRL_MASK)))
			return;

		//Point in the Graph, without the Zoom in the Display
		PopupCoordinates = e.getPoint();
		Point pointInGraph = new Point(Math.round(e.getPoint().x/((float)vgc.getZoom()/100)),Math.round(e.getPoint().y/((float)vgc.getZoom()/100)));
		VNode r=null;
		VEdge s=null;
		VHyperEdge t=null;
		if (vg!=null) //Normal Graph
		{
			r = vg.modifyNodes.getFirstinRangeOf(pointInGraph);
			s = vg.getEdgeinRangeOf(pointInGraph,2.0*((float)vgc.getZoom()/100));
		}
		else if (vhg!=null) //Hypergraph
		{
			r = vhg.modifyNodes.getFirstinRangeOf(pointInGraph);
			t = vhg.getEdgeinRangeOf(pointInGraph, 2.0*((float)vgc.getZoom()/100));
		}
		else
			return; //No graph is !=null
		if (r != null) //Clicked on Node
		{
			selectedNode = r;
			updateNodeSetList(r.getIndex());
			if (vg!=null)
			{
				Nname.setText(vg.getMathGraph().modifyNodes.get(r.getIndex()).name + " - (#" + r.getIndex() + ")");
				NaddEdgesTo.setEnabled(vg.modifyNodes.hasSelection());
				NaddEdgesFrom.setEnabled(vg.modifyNodes.hasSelection());
				NDelSelection.setEnabled(vg.modifyEdges.hasSelection()||vg.modifyNodes.hasSelection());
			}
			else if (vhg!=null)
			{
				Nname.setText(vhg.getMathGraph().modifyNodes.get(r.getIndex()).name + " - (#" + r.getIndex() + ")");
				NDelSelection.setEnabled(vhg.modifyHyperEdges.hasSelection()||vhg.modifyNodes.hasSelection());
				NCreateHyperEdgefromSel.setEnabled(vhg.modifyNodes.hasSelection());
			}
			NodePopup.show(e.getComponent(), e.getX(), e.getY());
		} 
		else if (s!=null) //Clicked on an Edge
		{
			selectedEdge = s;
			updateEdgeSetList(s.getIndex());
			MEdge me = vg.getMathGraph().modifyEdges.get(s.getIndex());
			Ename.setText(me.name+" (#"+s.getIndex()+", v:"+me.Value+")");
			EDelSelection.setEnabled(vg.modifyEdges.hasSelection()||vg.modifyNodes.hasSelection());
			EdgePopup.show(e.getComponent(), e.getX(),e.getY());	
		}
		else if (t!=null) //Clicked on an HyperEdge
		{
			selectedHyperEdge = t;
			updateEdgeSetList(t.getIndex());
			MHyperEdge mhe = vhg.getMathGraph().modifyHyperEdges.get(t.getIndex());
			Ename.setText(mhe.name+" (#"+t.getIndex()+" v:"+mhe.Value+" Kard.:"+mhe.cardinality()+")");
			EDelSelection.setEnabled(vhg.modifyHyperEdges.hasSelection()||vhg.modifyNodes.hasSelection());
			EdgePopup.show(e.getComponent(), e.getX(),e.getY());	
		}
		else //Weder Knoten noch Kante angeklickt -> Background popup
		{
				BgPopup.show(e.getComponent(), e.getX(), e.getY());
		}
	}

	public void mouseEntered(MouseEvent e) {}

	public void mouseExited(MouseEvent e) {}

	public void mousePressed(MouseEvent e) {}

	public void mouseReleased(MouseEvent e) {}

	private void handleBackgroundItems(ActionEvent e)
	{
		//Background-Context: get a Free index and add the Node
		if (e.getSource() == addNode) {
			if (vg!=null)
			{
				int newindex = vg.getMathGraph().modifyNodes.getNextIndex();
				new JNodeDialog(newindex,gp.getNodeName(newindex),PopupCoordinates.x,PopupCoordinates.y,gp.getIntValue("node.size"),vg);
			}
			else if (vhg!=null)
			{
					int newindex = vhg.getMathGraph().modifyNodes.getNextIndex();
					new JNodeDialog(newindex,gp.getNodeName(newindex),PopupCoordinates.x,PopupCoordinates.y,gp.getIntValue("node.size"),vhg);
			}
			PopupCoordinates = new Point(0,0);
		} 
		//Hintergrund: Neues Set
		if (e.getSource() == addSet) 
		{
			VSubgraphSet subgraphs = null;
			if (vg!=null)
				subgraphs = vg.modifySubgraphs;
			else if (vhg!=null)
				subgraphs = vhg.modifySubgraphs;
			boolean colorgone = false;
			Color c = new Color(0,0,0);
			do
			{
				c = Color.getHSBColor((float)Math.random(), (float)Math.random(), (float)Math.random());
				colorgone = false;
				Iterator<VSubgraph> siter = subgraphs.getIterator();
				while (siter.hasNext())
				{
					if (siter.next().getColor().equals(c)) //Farbe vergeben!
							colorgone = true;
				}
			} while(colorgone);
			if (vg!=null)
			{
				BitSet selNodes = new BitSet();
				Iterator<VNode> nodeiter = vg.modifyNodes.getIterator();
				while (nodeiter.hasNext())
				{
					VNode actual = nodeiter.next();
					if ((actual.getSelectedStatus()&VItem.SELECTED)==VItem.SELECTED)
						selNodes.set(actual.getIndex());
				}
				BitSet selEdges = new BitSet();
				Iterator<VEdge> edgeiter = vg.modifyEdges.getIterator();
				while (edgeiter.hasNext())
				{
					VEdge actual = edgeiter.next();
					if ((actual.getSelectedStatus()&VItem.SELECTED)==VItem.SELECTED)
						selEdges.set(actual.getIndex());
				}
				new JSubgraphDialog(vg.getMathGraph().modifySubgraphs.getNextIndex(),gp.getSubgraphName(vg.getMathGraph().modifySubgraphs.getNextIndex()),c,vg,selNodes,selEdges);
			}
			else if (vhg!=null)
			{
				BitSet selNodes = new BitSet();
				Iterator<VNode> nodeiter = vhg.modifyNodes.getIterator();
				while (nodeiter.hasNext())
				{
					VNode actual = nodeiter.next();
					if ((actual.getSelectedStatus()&VItem.SELECTED)==VItem.SELECTED)
						selNodes.set(actual.getIndex());
				}
				BitSet selEdges = new BitSet();
				Iterator<VHyperEdge> edgeiter = vhg.modifyHyperEdges.getIterator();
				while (edgeiter.hasNext())
				{
					VHyperEdge actual = edgeiter.next();
					if ((actual.getSelectedStatus()&VItem.SELECTED)==VItem.SELECTED)
						selEdges.set(actual.getIndex());
				}
				new JSubgraphDialog(vhg.getMathGraph().modifySubgraphs.getNextIndex(),gp.getSubgraphName(vhg.getMathGraph().modifySubgraphs.getNextIndex()),c,vhg,selNodes,selEdges);
			}
		}
	}

	private void handleSubgraphItems(ActionEvent e)
	{
		//NodeMenu or (Hyper)EdgeMenu : Add/Remove from Subgraph
		if (vSubgraphNMenus.contains(e.getSource())) // dann wurde ein Mengenmen�punkt angew�hlt
		{
			JMenuItem t = (JMenuItem) (e.getSource()); // This SubgraphMenuItem triggert the action
			VSubgraphSet subgraphs = null;
			MSubgraphSet msubgraphs = null;
			if (vg!=null) 
			{
				subgraphs = vg.modifySubgraphs;
				msubgraphs = vg.getMathGraph().modifySubgraphs;
			}
			else if (vhg!=null)
			{
				subgraphs = vhg.modifySubgraphs;
				msubgraphs = vhg.getMathGraph().modifySubgraphs;
			}
			if (selectedNode!=null)
			{
				if (msubgraphs.get(vSubgraphNMenus.indexOf(t)).containsNode(selectedNode.getIndex())) // Node is in Subgraph, so the item is a removal one
					subgraphs.removeNodefromSubgraph(selectedNode.getIndex(), vSubgraphNMenus.indexOf(t));
				else
					subgraphs.addNodetoSubgraph(selectedNode.getIndex(), vSubgraphNMenus.indexOf(t));
			}
			else if (selectedEdge!=null)
			{
				if (msubgraphs.get(vSubgraphNMenus.indexOf(t)).containsEdge(selectedEdge.getIndex())) // Egde is in Subgraph, so the item is a removal one
					subgraphs.removeEdgefromSubgraph(selectedEdge.getIndex(), vSubgraphNMenus.indexOf(t));
				else
					subgraphs.addEdgetoSubgraph(selectedEdge.getIndex(), vSubgraphNMenus.indexOf(t));
			}
			if (selectedHyperEdge!=null)
			{
				if (msubgraphs.get(vSubgraphNMenus.indexOf(t)).containsEdge(selectedHyperEdge.getIndex())) // hyperEdge is in Subgraph, so the item is a removal one
						subgraphs.removeEdgefromSubgraph(selectedHyperEdge.getIndex(), vSubgraphNMenus.indexOf(t));
					else
						subgraphs.addEdgetoSubgraph(selectedHyperEdge.getIndex(), vSubgraphNMenus.indexOf(t));
			}
		} 
	}

	private void handleNodeItems(ActionEvent e)
	{
		if (e.getSource() == NaddEdgesTo) { // Add Edges to alle selected Nodes, only happens in Graphs
			vg.addEdgestoSelectedNodes(selectedNode);
		} 
		if (e.getSource() == NaddEdgesFrom) { // Add Edges to alle selected Nodes only happens in Graphs
			vg.addEdgesfromSelectedNodes(selectedNode);
		} 
		//KNotenMen� : KnotenEigenschaftenDialog
		if (e.getSource() == Nproperties)
		{
			if (vg!=null)
				new JNodeDialog(selectedNode,vg);
			else
				new JNodeDialog(selectedNode,vhg);
			Gui.getInstance().refresh();
		} 
		//KnotenMenü : Knoten entfernen
		if (e.getSource() == Ndelete) {
			if (vg!=null)
				vg.modifyNodes.remove(selectedNode.getIndex());
			else if (vhg!=null)
				vhg.modifyNodes.remove(selectedNode.getIndex());
		}
	}
	
	private void handleEdgeItems(ActionEvent e)
	{
		//KantenMen� : L�schen
		if (e.getSource()==Edelete)
		{	
			vg.modifyEdges.remove(selectedEdge.getIndex());
		}
		//KantenMen� : Eigenschaften
		else if (e.getSource()==Eproperties)
		{
			if (vg!=null)
				new JEdgeDialog(selectedEdge,vg);
			else if (vhg!=null)
				new JHyperEdgeDialog(selectedHyperEdge);
		}
		else if (e.getSource()==HEShape)
		{
			Gui.getInstance().InitShapeModification(selectedHyperEdge.getIndex());
		}
	}
	public void actionPerformed(ActionEvent e) {
		handleBackgroundItems(e);
		handleSubgraphItems(e);
		handleNodeItems(e);
		handleEdgeItems(e);
		if ((e.getSource()==BgCreateHyperEdgefromSel)||(e.getSource()==NCreateHyperEdgefromSel))
		{
			if (vhg!=null)
			{
				BitSet selNodes = new BitSet();
				Iterator<VNode> nodeiter = vhg.modifyNodes.getIterator();
				while (nodeiter.hasNext())
				{
					VNode actual = nodeiter.next();
					if ((actual.getSelectedStatus()&VItem.SELECTED)==VItem.SELECTED)
						selNodes.set(actual.getIndex());
				}
				new JHyperEdgeDialog(selNodes);
			}
		}
		if ((e.getSource()==EDelSelection)||(e.getSource()==NDelSelection))
		{
			if (vg!=null)
				vg.removeSelection();
			else if (vhg!=null)
				vhg.removeSelection();
		}
		//Setze beide eventuell Men�bezugselemente auf null
		selectedEdge = null;
		selectedNode = null;
		selectedHyperEdge=null;
	}
	
	//(nonjavadoc) - @see Observer 
	public void update(Observable o, Object arg)
	{
		GraphMessage m = (GraphMessage)arg;
			if (m!=null)
			{ //If we get an GraphMessage and a SUBGRAPH is AFFECTED
				if ((m.getAffectedElementTypes()&GraphConstraints.SUBGRAPH)==GraphConstraints.SUBGRAPH)
					updateSubgraphList();
			}
	}
}