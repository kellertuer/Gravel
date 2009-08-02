package control;


import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import view.VGraphic;
import view.VHyperGraphic;

import model.VGraph;
import model.VHyperGraph;
/**
 * OneClick Handling of Mouse Drag actions
 * 
 * starting on a node an edge is created to the mouse
 * shoft+drag on a selected node creates edges from each selected node to the mouse
 * 
 * @author Ronny Bergmann
 */
public class OCMDragMouseHandler implements DragMouseHandler
{

	private VGraph vg;
	private VHyperGraph vhg;
	private NodeDragEdgeCreationListener GraphNodeDragActions;
	private Point MouseOffSet;
	private SelectionDragListener cSelectionDragActions;
	private CommonNodeDragListener cNodeDragActions;
	private CommonEdgeDragListener cEdgeDragActions;
	/**
	 * Initializes the Drag Handler to observe a specific VGraph
	 * 
	 * @param g the vgraph
	 */
	public OCMDragMouseHandler(VGraphic g)
	{
		vg = g.getGraph();
		MouseOffSet = new Point(0,0);
		GraphNodeDragActions = new NodeDragEdgeCreationListener(g);
		cSelectionDragActions= new SelectionDragListener(g);
		cNodeDragActions = new CommonNodeDragListener(g);
		cEdgeDragActions = new CommonEdgeDragListener(g);
	}

	/**
	 * Initializes the Drag Handler to observe a specific VHyperGraph
	 * 
	 * @param g the VHyperGraphics-Umgebung
	 */
	public OCMDragMouseHandler(VHyperGraphic g)
	{
		vhg = g.getGraph();
		MouseOffSet = new Point(0,0);
		cSelectionDragActions= new SelectionDragListener(g);
		cNodeDragActions = new CommonNodeDragListener(g);
		cEdgeDragActions = new CommonEdgeDragListener(g);
	}

	public void removeGraphObservers() {}

	/**
	 * return the mouseoffset. which is the startposition of any drag-action
	 * 
	 */
	public Point getMouseOffSet()
	{
		return MouseOffSet;
	}
	/**
	 * Return the status of a drag action.
	 * 
	 * @return true, if there is a drag currently running
	 */
	public boolean dragged()
	{
		if (vg!=null)
			return (GraphNodeDragActions.dragged()||cEdgeDragActions.dragged()||cNodeDragActions.dragged()||cSelectionDragActions.dragged());
		else if (vhg!=null)
			return (cNodeDragActions.dragged()||cSelectionDragActions.dragged()||cEdgeDragActions.dragged());
		else
			return false;
	}
	public void mouseDragged(MouseEvent e) {
		cSelectionDragActions.mouseDragged(e);
		cNodeDragActions.mouseDragged(e);
		cEdgeDragActions.mouseDragged(e);
		if (vg!=null)
			GraphNodeDragActions.mouseDragged(e);
	}

	public void mousePressed(MouseEvent e) {
		cSelectionDragActions.mousePressed(e);
		cNodeDragActions.mousePressed(e);
		cEdgeDragActions.mousePressed(e);
		if (vg!=null)
			GraphNodeDragActions.mousePressed(e);
		MouseOffSet = e.getPoint();
	}

	public void mouseReleased(MouseEvent e) {
		cSelectionDragActions.mouseReleased(e);
		cNodeDragActions.mouseReleased(e);
		cEdgeDragActions.mouseReleased(e);
		if (vg!=null)
			GraphNodeDragActions.mouseReleased(e);
		MouseOffSet = new Point(0,0);
	}

	public void mouseMoved(MouseEvent arg0) {}
	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}

	public Rectangle getSelectionRectangle() {
		return cSelectionDragActions.getSelectionRectangle();
	}

	public void setGrid(int x, int y) {}

	public void setGridOrientated(boolean b) {}

}
