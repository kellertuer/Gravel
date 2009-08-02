package control;


import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import view.VGraphic;
import view.VHyperGraphic;

import model.VGraph;
import model.VHyperGraph;
/**
 * Standard Mouse Drag Hanlder extends the DragMouseHandler to the standard actions
 *
 * 
 * @author Ronny Bergmann
 *
 */
public class StandardDragMouseHandler implements DragMouseHandler
{

	private VGraph vg;
	private VHyperGraph vhg;
	private Point MouseOffSet;
	private StandardNodeDragListener sNodeDragActions;
	private StandardEdgeDragListener sEdgeDragActions;
	SelectionDragListener cSelectionDragActions;
	CommonNodeDragListener cNodeDragActions;
	CommonEdgeDragListener cEdgeDragActions;

	/**
	 * Initializes the Handler to a given VGraph
	 * 
	 * @param g the VGraph
	 */
	public StandardDragMouseHandler(VGraphic g)
	{
		vg = g.getGraph();
		MouseOffSet = new Point(0,0);
		sNodeDragActions = new StandardNodeDragListener(g);
		sEdgeDragActions = new StandardEdgeDragListener(g);
		cSelectionDragActions= new SelectionDragListener(g);
		cNodeDragActions = new CommonNodeDragListener(g);
		cEdgeDragActions = new CommonEdgeDragListener(g);

	}
	/**
	 * Initializes the Handler to a given VGraph
	 * 
	 * @param g the VGraph
	 */
	public StandardDragMouseHandler(VHyperGraphic g)
	{
		vhg = g.getGraph();
		MouseOffSet = new Point(0,0);
		sNodeDragActions = new StandardNodeDragListener(g);
		cSelectionDragActions= new SelectionDragListener(g);
		cNodeDragActions = new CommonNodeDragListener(g);
		cEdgeDragActions = new CommonEdgeDragListener(g);
	}
	public void removeGraphObservers()
	{}
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
			return (sNodeDragActions.dragged()||sEdgeDragActions.dragged()||cEdgeDragActions.dragged()||cNodeDragActions.dragged()||cSelectionDragActions.dragged());
		else if (vhg!=null)
			return ((sNodeDragActions.dragged())||cNodeDragActions.dragged()||cEdgeDragActions.dragged()||cSelectionDragActions.dragged());
		else
			return false;
	}
	/** set whether the nodes are set to a gridpoint after dragging or not. 
	 * 
	 */
	public void setGridOrientated(boolean b)
	{
		sNodeDragActions.setGridOrientated(b);
	}	
	/** update Gridinfo coordinate distances
	 * 
	 */
	public void setGrid(int x, int y)
	{
		sNodeDragActions.setGrid(x, y);
	}
	/**
	 * Handle a drag event, update the positions of moved nodes and theis adjacent edges
	 */
	public void mouseDragged(MouseEvent e) {
		cSelectionDragActions.mouseDragged(e);
		cNodeDragActions.mouseDragged(e);
		sNodeDragActions.mouseDragged(e);
		cEdgeDragActions.mouseDragged(e);
		if (vg!=null)
			sEdgeDragActions.mouseDragged(e);
		MouseOffSet = e.getPoint();
	}
	public void mousePressed(MouseEvent e) {
		cSelectionDragActions.mousePressed(e);
		cNodeDragActions.mousePressed(e);
		sNodeDragActions.mousePressed(e);		
		cEdgeDragActions.mousePressed(e);
		if (vg!=null)
			sEdgeDragActions.mousePressed(e);			
	}
	public void mouseReleased(MouseEvent e) {
		cSelectionDragActions.mouseReleased(e);
		cNodeDragActions.mouseReleased(e);
		sNodeDragActions.mouseReleased(e);
		cEdgeDragActions.mouseReleased(e);
		if (vg!=null)
			sEdgeDragActions.mouseReleased(e);
		MouseOffSet = new Point(0,0);
	}
	public Rectangle getSelectionRectangle() {
		return cSelectionDragActions.getSelectionRectangle();
	}
	public void mouseMoved(MouseEvent arg0) {}
	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}

}
