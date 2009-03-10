package control;

import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Iterator;

import view.*;
import model.*;
import model.Messages.GraphConstraints;
import model.Messages.GraphMessage;

/**
 * Handle Drag Actions that affect Nodes.
 * 
 * - Drag begun on node -> Move Node
 * - Shift+Drag begun on selected Node 
 *    -> Move selected Nodes 
 *    in a graph additionally Controlpoints of adjacent Edges by half of the movement
 * @author ronny
 *
 */
public class StandardNodeDragListener  implements MouseListener, MouseMotionListener {

	private VGraph vg=null;
	private VHyperGraph vhg=null;
	private VCommonGraphic vgc;
	private Point MouseOffSet;
	private VNode movingNode;
	private int gridx,gridy;
	private boolean gridorientated, shiftwaspressed,firstdrag=true, multiplemoving=false;

	public StandardNodeDragListener(VGraphic g)
	{
		vgc = g;
		vg = g.getGraph();
		MouseOffSet = new Point(0,0);
	}

	public StandardNodeDragListener(VHyperGraphic g)
	{
		vgc = g;
		vhg = g.getGraph();
		MouseOffSet = new Point(0,0);
	}
	
	public boolean dragged()
	{
		return ((movingNode!=null)||(multiplemoving));
	}
	/** set whether the nodes are set to a gridpoint after dragging or not. 
	 * 
	 */
	public void setGridOrientated(boolean b)
	{
		gridorientated = b;
	}	
	/** update Gridinfo coordinate distances
	 * 
	 */
	public void setGrid(int x, int y)
	{
		gridx = x; gridy = y;
	}
	/**
	 * move edges that ar adjacent to a node, moves the edge control points
	 * - half the movement, if the adjacent node is selected
	 * @param nodeindex
	 * @param x
	 * @param y
	 */
	private void translateAdjacentEdges(int nodeindex, int x, int y)
	{
		if (vg==null)
			return;
		x = Math.round(x/2);
		y = Math.round(y/2);
		Iterator<VEdge> edgeiter = vg.modifyEdges.getIterator();
		while (edgeiter.hasNext())
		{
			VEdge e = edgeiter.next();
			MEdge me = vg.getMathGraph().modifyEdges.get(e.getIndex());
			int start = me.StartIndex;
			int ende = me.EndIndex;
			if ((start==nodeindex)||(ende==nodeindex))
					e.translate(x, y);
		}
	}

	public void mousePressed(MouseEvent e)
	{	
		firstdrag=true;
		
		MouseOffSet = e.getPoint(); //Aktuelle Position merken für eventuelle Bewegungen while pressed
		Point pointInGraph = new Point(Math.round(e.getPoint().x/((float)vgc.getZoom()/100)),Math.round(e.getPoint().y/((float)vgc.getZoom()/100))); //Rausrechnen des zooms
		if ((InputEvent.ALT_DOWN_MASK & e.getModifiersEx()) == InputEvent.ALT_DOWN_MASK)
			return; //Don't handle ALT-Stuff here
		VNode nodeInRange=null;

		if (vg!=null) //Normal Graph
			nodeInRange = vg.modifyNodes.getFirstinRangeOf(pointInGraph);
		else if (vhg!=null) //Hypergraph
			nodeInRange = vhg.modifyNodes.getFirstinRangeOf(pointInGraph);
		else
			return; //No graph is !=null

		if (nodeInRange==null) //Only handle nodes
			return;
		shiftwaspressed = ((InputEvent.SHIFT_DOWN_MASK & e.getModifiersEx()) == InputEvent.SHIFT_DOWN_MASK); //shift ?
	
		if (!shiftwaspressed)
			movingNode = nodeInRange; //kein Shift == moving Node merken, sonst werden alle selected Bewegt
		else 
		{
			if ((nodeInRange!=null)&&((nodeInRange.getSelectedStatus()&VItem.SELECTED)==VItem.SELECTED))
				multiplemoving = true;
		}
	}
	
	public void mouseDragged(MouseEvent e)
	{
		if ((shiftwaspressed)&&!(((InputEvent.SHIFT_DOWN_MASK & e.getModifiersEx()) == InputEvent.SHIFT_DOWN_MASK)))
		{ //Drag begun with shift and released
			firstdrag=true;
			movingNode=null;
			multiplemoving=false;
			return;
		}
		VGraphInterface notify=null;
		if (vg!=null) //Normal Graph
			notify = vg;
		else if (vhg!=null) //Hypergraph
			notify=vhg;
		//Get Movement since begin of Drag
		Point mouseMovement = new Point(e.getPoint().x-MouseOffSet.x, e.getPoint().y-MouseOffSet.y);
		//Actual Movement in the graph (without Zoom)
		int Gtransx = Math.round(mouseMovement.x/((float)vgc.getZoom()/100));
		int Gtransy = Math.round(mouseMovement.y/((float)vgc.getZoom()/100));
		//Position im Graphen (ohne Zoom)
		Point posInGraph = new Point(Math.round(e.getPoint().x/((float)vgc.getZoom()/100)),Math.round(e.getPoint().y/((float)vgc.getZoom()/100)));
		//Das ist die Bewegung p
		if ((((InputEvent.SHIFT_DOWN_MASK & e.getModifiersEx()) == InputEvent.SHIFT_DOWN_MASK))&&(multiplemoving)) //shift n drag == Selection bewegen
		{
			//Move Selected Nodes
			Iterator<VNode> nodeiter=null;
			if (vg!=null)
				nodeiter = vg.modifyNodes.getIterator();
			else if (vhg!=null)
				nodeiter = vhg.modifyNodes.getIterator();
			else
				return;
			while (nodeiter.hasNext()) // drawNodes
			{
				VNode temp = nodeiter.next();
				if ((temp.getSelectedStatus() & VItem.SELECTED) == VItem.SELECTED)
				{
					Point newpoint = temp.getPosition(); //bisherige Knotenposition
					newpoint.translate(Gtransx,Gtransy); //Bewegung im Graphen aber mit Rungungsfehlern, also nur zurbetrachtung der Gesamtgraphbewegung
					if (newpoint.x < 0)
					{
						vg.translate(Math.abs(newpoint.x), 0); //Um die Differenz verschieben (Zoomfactor aufheben)
						newpoint.x=0;
					}
					if (newpoint.y < 0)
					{
						vg.translate(0,Math.abs(newpoint.y));
						newpoint.y = 0;
					}
					temp.setPosition(newpoint); //Translate selected node
					//move Adjacent Edges
					if (vg!=null)
						translateAdjacentEdges(temp.getIndex(), Gtransx,Gtransy);
				}
			}
			if (firstdrag) //First Drag: Start a Block for the Movement else just update
				notify.pushNotify(new GraphMessage(GraphConstraints.SELECTION|GraphConstraints.NODE|GraphConstraints.EDGE,GraphConstraints.UPDATE|GraphConstraints.BLOCK_START));
			else
				notify.pushNotify(new GraphMessage(GraphConstraints.SELECTION|GraphConstraints.NODE|GraphConstraints.EDGE,GraphConstraints.UPDATE));
		} //End Shift
		else if (movingNode!=null) //ohne Shift ohne alt - gibt es einen Knoten r auf dem Mausposition, der nicht selektiert ist ?
		{
			Point newpos = movingNode.getPosition();
			newpos.translate(Gtransx,Gtransy); //Bewegung im Graphen aber mit Rungungsfehlern, also nur zurbetrachtung der Gesamtgraphbewegung
			//For Graph Translation get the non-null into an graphinterface
			VGraphInterface vgtr;
			if (vg!=null)
				vgtr = vg;
			else if (vhg!=null)
				vgtr = vhg;
			else
				return;
			if (newpos.x < 0)
			{
				vgtr.translate(Math.abs(newpos.x), 0);
				posInGraph.x=0;
			}
			if (newpos.y < 0)
			{
				vgtr.translate(0,Math.abs(newpos.y));
				posInGraph.y = 0;
			}
			if (vg!=null)
				translateAdjacentEdges(movingNode.getIndex(),Gtransx,Gtransy);	
			
			movingNode.setPosition(posInGraph);
			if (firstdrag) //Start Block for Node Movement else just update graphic
				notify.pushNotify(new GraphMessage(GraphConstraints.NODE,movingNode.getIndex(),GraphConstraints.TRANSLATION|GraphConstraints.BLOCK_START,GraphConstraints.NODE|GraphConstraints.EDGE));
			else
				notify.pushNotify(new GraphMessage(GraphConstraints.NODE,movingNode.getIndex(),GraphConstraints.TRANSLATION,GraphConstraints.NODE|GraphConstraints.EDGE)); //Kanten aktualisiert
		}
		MouseOffSet = e.getPoint();
		if (firstdrag)
			firstdrag = false; //First time really dragged, so it's not firstdrag anymore
	}

	public void mouseReleased(MouseEvent e)
	{
		if (!firstdrag) //If really dragged and not just clicked
		{
			if (!((e.getPoint().x==-1)||(e.getPoint().y==-1)))//kein Reset von außerhalb wegen modusumschaltung
				mouseDragged(e); //Das gleiche wie als wenn man bewegt, nur ist danach kein Knoten mehr bewegter Knoten
		}
		VGraphInterface notify=null;
		if (vg!=null) //Normal Graph
			notify = vg;
		else if (vhg!=null) //Hypergraph
			notify=vhg;
		if (!((InputEvent.SHIFT_DOWN_MASK & e.getModifiersEx()) == InputEvent.SHIFT_DOWN_MASK))
		{ //No Shift - Snap the moving node (if existent) to grid
			if ((movingNode!=null)&&gridorientated)
			{ //nur ein Knoten wurde bewegt, snap to grid	
				int xdistanceupper = movingNode.getPosition().x%gridx;
				int xdistancelower = gridx-xdistanceupper;
				int ydistanceupper = movingNode.getPosition().y%gridy;
				int ydistancelower = gridy-ydistanceupper;
				Point newpos = new Point();
				if (xdistanceupper>xdistancelower) //näher am oberen der beiden werte
					newpos.x = (new Double(Math.ceil((double)movingNode.getPosition().x/(double)gridx)).intValue())*gridx;
				else 
					newpos.x =  (new Double(Math.floor((double)movingNode.getPosition().x/(double)gridx)).intValue())*gridx;
				if (ydistanceupper>ydistancelower) //näher am oberen der beiden werte
					newpos.y = (new Double(Math.ceil((double)movingNode.getPosition().y/(double)gridy)).intValue())*gridy;
				else 
					newpos.y =  (new Double(Math.floor((double)movingNode.getPosition().y/(double)gridy)).intValue())*gridy;
				movingNode.setPosition(newpos);	
			}
			if (movingNode!=null) //End Nove Movement-Block
				notify.pushNotify(new GraphMessage(GraphConstraints.NODE,movingNode.getIndex(),GraphConstraints.UPDATE|GraphConstraints.BLOCK_END,GraphConstraints.NODE|GraphConstraints.EDGE));
			movingNode=null;
		}
		else if (!firstdrag) //With Shift and a real drag
		{
			multiplemoving=false;
			notify.pushNotify(new GraphMessage(GraphConstraints.NODE|GraphConstraints.EDGE|GraphConstraints.SELECTION,GraphConstraints.BLOCK_END));
		}
		firstdrag = true;
	}
	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mouseMoved(MouseEvent e) {}
}
