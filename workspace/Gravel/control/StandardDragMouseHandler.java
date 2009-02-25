package control;

import io.GeneralPreferences;

import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.Vector;

import model.MEdge;
import model.VEdge;
import model.VGraph;
import model.VItem;
import model.VNode;
import model.Messages.GraphConstraints;
import model.Messages.GraphMessage;
/**
 * Standard Mouse Drag Hanlder extends the DragMouseHandler to the standard actions
 *
 * 
 * @author Ronny Bergmann
 *
 */
public class StandardDragMouseHandler extends DragMouseHandler
{

	private VGraph vg;
	private Point MouseOffSet;
	private GeneralPreferences gp;
	private VNode movingNode;
	private VEdge movingControlPointEdge;
	private int movingControlPointIndex, gridx,gridy;
	private boolean gridorientated, firstdrag;
	/**
	 * Initializes the Handler to a given VGraph
	 * 
	 * @param g the VGraph
	 */
	public StandardDragMouseHandler(VGraph g)
	{
		super(g);
		vg = g;
		gp = GeneralPreferences.getInstance();
		MouseOffSet = new Point(0,0);
		firstdrag = true; //Not dragged up to now
	}
	/**
	 * Returns the actual Startpoint if a drag is running, if not it returns 0,0
	 * 
	 * @return Startpoint of the drag action
	 */
	public Point getMouseOffSet()
	{
		return MouseOffSet;
	}
	/**
	 * Indicates, whether a drag is running or not
	 * 
	 * @return true, if a node or edge control point is moved (drag action is running)
	 */
	public boolean dragged()
	{
		return ((movingNode!=null)||(movingControlPointEdge!=null)||(selrect!=null));
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
	/**
	 * Handle a drag event, update the positions of moved nodes and theis adjacent edges
	 */
	public void mouseDragged(MouseEvent e) {
		super.mouseDragged(e);
		if ((InputEvent.ALT_DOWN_MASK & e.getModifiersEx()) == InputEvent.ALT_DOWN_MASK)
			return; //No ALT Handling here
		//Get Movement since begin of Drag
		Point p = new Point(e.getPoint().x-MouseOffSet.x, e.getPoint().y-MouseOffSet.y);
		//Actual Movement in the graph (without Zoom)
		int Gtransx = Math.round(p.x/((float)gp.getIntValue("vgraphic.zoom")/100));
		int Gtransy = Math.round(p.y/((float)gp.getIntValue("vgraphic.zoom")/100));
		//Position im Graphen (ohne Zoom)
		Point GPos = new Point(Math.round(e.getPoint().x/((float)gp.getIntValue("vgraphic.zoom")/100)),Math.round(e.getPoint().y/((float)gp.getIntValue("vgraphic.zoom")/100)));
		//Das ist die Bewegung p
		if ((((InputEvent.SHIFT_DOWN_MASK & e.getModifiersEx()) == InputEvent.SHIFT_DOWN_MASK))&&(movingNode!=null)) //shift n drag == Selection bewegen
		{ //Move all Selected Nodes
			Iterator<VNode> nodeiter = vg.modifyNodes.getIterator();
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
					translateAdjacentEdges(temp.getIndex(), Gtransx,Gtransy);
				}
			}
			if (firstdrag) //First Drag: Start a Block for the Movement else just update
				vg.pushNotify(new GraphMessage(GraphConstraints.SELECTION|GraphConstraints.NODE|GraphConstraints.EDGE,GraphConstraints.UPDATE|GraphConstraints.BLOCK_START));
			else
				vg.pushNotify(new GraphMessage(GraphConstraints.SELECTION|GraphConstraints.NODE|GraphConstraints.EDGE,GraphConstraints.UPDATE));
		} //End Shift
		else if (movingNode!=null) //ohne Shift ohne alt - gibt es einen Knoten r auf dem Mausposition, der nicht selektiert ist ?
		{
			Point newpos = movingNode.getPosition();
			newpos.translate(Gtransx,Gtransy); //Bewegung im Graphen aber mit Rungungsfehlern, also nur zurbetrachtung der Gesamtgraphbewegung
			if (newpos.x < 0)
			{
				vg.translate(Math.abs(newpos.x), 0);
				GPos.x=0;
			}
			if (newpos.y < 0)
			{
				vg.translate(0,Math.abs(newpos.y));
				GPos.y = 0;
			}
			translateAdjacentEdges(movingNode.getIndex(),Gtransx,Gtransy);	
			movingNode.setPosition(GPos);
			if (firstdrag) //Start Block for Node Movement else just update graphic
				vg.pushNotify(new GraphMessage(GraphConstraints.NODE,movingNode.getIndex(),GraphConstraints.TRANSLATION|GraphConstraints.BLOCK_START,GraphConstraints.NODE|GraphConstraints.EDGE));
			else
				vg.pushNotify(new GraphMessage(GraphConstraints.NODE,movingNode.getIndex(),GraphConstraints.TRANSLATION,GraphConstraints.NODE|GraphConstraints.EDGE)); //Kanten aktualisiert
		}
		else if (movingControlPointEdge!=null)
		{
			Point newpos;
			Vector<Point> points = movingControlPointEdge.getControlPoints();
			newpos = points.get(movingControlPointIndex);
			newpos.translate(Gtransx,Gtransy); //Bewegung im Graphen aber mit Rungungsfehlern, also nur zurbetrachtung der Gesamtgraphbewegung
			if (newpos.x < 0)
			{
				vg.translate(Math.abs(newpos.x), 0);
				GPos.x=0;
			}
			if (newpos.y < 0)
			{
				vg.translate(0,Math.abs(newpos.y));
				GPos.y = 0;
			}
			points.set(movingControlPointIndex, GPos);
			movingControlPointEdge.setControlPoints(points);
			if (firstdrag) //On First Drag Movement start a Block for CP-Movement else just update it
				vg.pushNotify(new GraphMessage(GraphConstraints.EDGE,movingControlPointEdge.getIndex(),GraphConstraints.UPDATE|GraphConstraints.BLOCK_START,GraphConstraints.EDGE)); //Kanten aktualisiert
			else
				vg.pushNotify(new GraphMessage(GraphConstraints.EDGE,movingControlPointEdge.getIndex(),GraphConstraints.UPDATE,GraphConstraints.EDGE)); //Kanten aktualisiert
		}
		MouseOffSet = e.getPoint();
		if (firstdrag)
			firstdrag = false; //First time really draged, so it's not firstdrag anymore
	}

	public void mouseMoved(MouseEvent arg0) {}

	public void mouseClicked(MouseEvent e) {}

	public void mouseEntered(MouseEvent e) {}

	public void mouseExited(MouseEvent e) {}
	/**
	 * If the mouse button is pressed initialize a drag (only if left mouse button is pressed)
	 */
	@SuppressWarnings("unchecked")
	public void mousePressed(MouseEvent e) {
		super.mousePressed(e);
		firstdrag = true;
		MouseOffSet = e.getPoint(); //Aktuelle Position merken für eventuelle Bewegungen while pressed Zoom included
		Point p = new Point(Math.round(e.getPoint().x/((float)gp.getIntValue("vgraphic.zoom")/100)),Math.round(e.getPoint().y/((float)gp.getIntValue("vgraphic.zoom")/100))); //Rausrechnen des zooms
		if (!((InputEvent.SHIFT_DOWN_MASK & e.getModifiersEx()) == InputEvent.SHIFT_DOWN_MASK))
		{
			movingNode = vg.modifyNodes.getFirstinRangeOf(p); //kein Shift == moving Node merken, sonst werden alle selected Bewegt
			if (gp.getBoolValue("vgraphic.cpshow")) 
			{
				Vector c = vg.modifyEdges.firstCPinRageOf(p, (new Integer(gp.getIntValue("vgraphic.cpsize"))).doubleValue());
				if (c!=null)
				{
					movingControlPointEdge = (VEdge) c.get(0);
					movingControlPointIndex = ((Integer)c.get(1)).intValue();
				}
			}
		}
		else
		{//Shift - only handle stuff that beginns on a selected Node
			movingNode = vg.modifyNodes.getFirstinRangeOf(p);
			if ((movingNode!=null)&&((movingNode.getSelectedStatus() & VItem.SELECTED) != VItem.SELECTED))
			{
				movingNode=null; //do not start anything
				firstdrag = true;
			}
			VEdge selE = vg.getEdgeinRangeOf(p, 2.0);
			if ((selE!=null)&&((selE.getSelectedStatus() & VItem.SELECTED) == VItem.SELECTED))
			{ //Selected Edge, we move the selection so set the node to one of the edge adjacent nodes
				movingNode = vg.modifyNodes.get(vg.getMathGraph().modifyEdges.get(selE.getIndex()).StartIndex); 
			}
		}
	}
	/**
	 * ends the drag action, because the mouse button is released
	 */
	public void mouseReleased(MouseEvent e) {
		super.mouseReleased(e);
		if (!firstdrag) //If really dragged and not just clicked
		{
			if (!((e.getPoint().x==-1)||(e.getPoint().y==-1)))//kein Reset von außerhalb wegen modusumschaltung
				mouseDragged(e); //Das gleiche wie als wenn man bewegt, nur ist danach kein Knoten mehr bewegter Knoten
		}
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
				vg.pushNotify(new GraphMessage(GraphConstraints.NODE,movingNode.getIndex(),GraphConstraints.UPDATE|GraphConstraints.BLOCK_END,GraphConstraints.NODE|GraphConstraints.EDGE));
			else if (movingControlPointEdge!=null) //End Edge-CP Movement-Block
				vg.pushNotify(new GraphMessage(GraphConstraints.EDGE,movingControlPointEdge.getIndex(),GraphConstraints.UPDATE|GraphConstraints.BLOCK_END,GraphConstraints.EDGE)); //Kanten aktualisiert
			movingNode=null;
			movingControlPointEdge=null;
			movingControlPointIndex = -1;
		}
		else if (!firstdrag) //With Shift and a real drag
			vg.pushNotify(new GraphMessage(GraphConstraints.NODE|GraphConstraints.EDGE|GraphConstraints.SELECTION,GraphConstraints.BLOCK_END));

		firstdrag = true;
	}
}
