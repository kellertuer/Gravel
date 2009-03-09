package control;

import io.GeneralPreferences;

import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.Vector;

import view.VCommonGraphic;
import view.VGraphic;

import model.VEdge;
import model.VGraph;
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
	private VCommonGraphic vgc;
	private Point MouseOffSet;
	private GeneralPreferences gp;
	private StandardNodeDragListener NodeDragActions;
	private VEdge movingControlPointEdge;
	private int movingControlPointIndex;
	private boolean firstdrag;
	/**
	 * Initializes the Handler to a given VGraph
	 * 
	 * @param g the VGraph
	 */
	public StandardDragMouseHandler(VGraphic g)
	{
		super(g);
		vgc = g;
		vg = g.getGraph();
		gp = GeneralPreferences.getInstance();
		MouseOffSet = new Point(0,0);
		firstdrag = true; //Not dragged up to now
		NodeDragActions = new StandardNodeDragListener(g);
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
		return ((NodeDragActions.dragged())||(movingControlPointEdge!=null)||(super.dragged()));
	}
	/** set whether the nodes are set to a gridpoint after dragging or not. 
	 * 
	 */
	public void setGridOrientated(boolean b)
	{
		NodeDragActions.setGridOrientated(b);
	}	
	/** update Gridinfo coordinate distances
	 * 
	 */
	public void setGrid(int x, int y)
	{
		NodeDragActions.setGrid(x, y);
	}
	/**
	 * Handle a drag event, update the positions of moved nodes and theis adjacent edges
	 */
	public void mouseDragged(MouseEvent e) {
		super.mouseDragged(e);
		NodeDragActions.mouseDragged(e);
		if ((InputEvent.ALT_DOWN_MASK & e.getModifiersEx()) == InputEvent.ALT_DOWN_MASK)
			return; //No ALT Handling here
		//Get Movement since begin of Drag
		Point p = new Point(e.getPoint().x-MouseOffSet.x, e.getPoint().y-MouseOffSet.y);
		//Actual Movement in the graph (without Zoom)
		int Gtransx = Math.round(p.x/((float)vgc.getZoom()/100));
		int Gtransy = Math.round(p.y/((float)vgc.getZoom()/100));
		//Position im Graphen (ohne Zoom)
		Point GPos = new Point(Math.round(e.getPoint().x/((float)vgc.getZoom()/100)),Math.round(e.getPoint().y/((float)vgc.getZoom()/100)));
		if (movingControlPointEdge!=null)
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

	@SuppressWarnings("unchecked")
	public void mousePressed(MouseEvent e) {
		super.mousePressed(e);
		NodeDragActions.mousePressed(e);
		firstdrag = true;
		MouseOffSet = e.getPoint(); //Aktuelle Position merken für eventuelle Bewegungen while pressed Zoom included
		Point p = new Point(Math.round(e.getPoint().x/((float)vgc.getZoom()/100)),Math.round(e.getPoint().y/((float)vgc.getZoom()/100))); //Rausrechnen des zooms
		if (!((InputEvent.SHIFT_DOWN_MASK & e.getModifiersEx()) == InputEvent.SHIFT_DOWN_MASK))
		{
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
	}

	public void mouseReleased(MouseEvent e) {
		super.mouseReleased(e);
		NodeDragActions.mouseReleased(e);
		if (!firstdrag) //If really dragged and not just clicked
		{
			if (!((e.getPoint().x==-1)||(e.getPoint().y==-1)))//kein Reset von außerhalb wegen modusumschaltung
				mouseDragged(e); //Das gleiche wie als wenn man bewegt, nur ist danach kein Knoten mehr bewegter Knoten
		}
		if (!((InputEvent.SHIFT_DOWN_MASK & e.getModifiersEx()) == InputEvent.SHIFT_DOWN_MASK))
		{	movingControlPointEdge=null;
			movingControlPointIndex = -1;
		}
		firstdrag = true;
	}

	public void mouseMoved(MouseEvent arg0) {}
	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
}
