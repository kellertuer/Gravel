package control;

import io.GeneralPreferences;

import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.Vector;

import view.VCommonGraphic;
import view.VGraphic;
import view.VHyperGraphic;

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
	private Point MouseOffSet;
	private StandardNodeDragListener NodeDragActions;
	private StandardEdgeDragListener EdgeDragActions;
	/**
	 * Initializes the Handler to a given VGraph
	 * 
	 * @param g the VGraph
	 */
	public StandardDragMouseHandler(VGraphic g)
	{
		super(g);
		vg = g.getGraph();
		MouseOffSet = new Point(0,0);
		NodeDragActions = new StandardNodeDragListener(g);
		EdgeDragActions = new StandardEdgeDragListener(g);
	}
	/**
	 * Initializes the Handler to a given VGraph
	 * 
	 * @param g the VGraph
	 */
	public StandardDragMouseHandler(VHyperGraphic g)
	{
		super(g);
		vhg = g.getGraph();
		MouseOffSet = new Point(0,0);
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
		if (vg!=null)
			return ((NodeDragActions.dragged())||(EdgeDragActions.dragged())||(super.dragged()));
		else if (vhg!=null)
			return ((NodeDragActions.dragged())||(super.dragged()));
		else
			return false;
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
		if (vg!=null)
			EdgeDragActions.mouseClicked(e);
		MouseOffSet = e.getPoint();
	}
	public void mousePressed(MouseEvent e) {
		super.mousePressed(e);
		NodeDragActions.mousePressed(e);
		if (vg!=null)
			EdgeDragActions.mousePressed(e);
	}
	public void mouseReleased(MouseEvent e) {
		super.mouseReleased(e);
		NodeDragActions.mouseReleased(e);
		if (vg!=null)
			EdgeDragActions.mouseReleased(e);
		MouseOffSet = new Point(0,0);
	}
	public void mouseMoved(MouseEvent arg0) {}
	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
}
