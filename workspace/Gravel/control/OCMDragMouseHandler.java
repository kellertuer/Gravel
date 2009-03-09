package control;

import io.GeneralPreferences;

import java.awt.Point;
import java.awt.event.MouseEvent;

import view.VCommonGraphic;
import view.VGraphic;
import view.VHyperGraphic;

import model.VGraph;
import model.VHyperGraph;
import model.VNode;
/**
 * OneClick Handling of Mouse Drag actions
 * 
 * starting on a node an edge is created to the mouse
 * shoft+drag on a selected node creates edges from each selected node to the mouse
 * 
 * @author Ronny Bergmann
 */
public class OCMDragMouseHandler extends DragMouseHandler
{

	private VGraph vg;
	private VHyperGraph vhg;
	private VCommonGraphic vgc;
	private OCMNodeDragListener GraphNodeDragActions;
	private Point MouseOffSet;
	private GeneralPreferences gp;
	private VNode StartNode,DragNode;
	
	private boolean multiple, firstdrag;
	/**
	 * Initializes the Drag Handler to observe a specific VGraph
	 * 
	 * @param g the vgraph
	 */
	public OCMDragMouseHandler(VGraphic g)
	{
		super(g);
		vgc = g;
		vg = g.getGraph();
		MouseOffSet = new Point(0,0);
		GraphNodeDragActions = new OCMNodeDragListener(g);
	}

	/**
	 * Initializes the Drag Handler to observe a specific VHyperGraph
	 * 
	 * @param g the VHyperGraphics-Umgebung
	 */
	public OCMDragMouseHandler(VHyperGraphic g)
	{
		super(g);
		vgc = g;
		vhg = g.getGraph();
		MouseOffSet = new Point(0,0);
	}

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
			return (GraphNodeDragActions.dragged());
		else
			return false;
	}
	public void mouseDragged(MouseEvent e) {
		super.mouseDragged(e);
		if (vg!=null)
			GraphNodeDragActions.mouseDragged(e);
	}

	public void mousePressed(MouseEvent e) {
		super.mousePressed(e);
		if (vg!=null)
			GraphNodeDragActions.mousePressed(e);

		MouseOffSet = e.getPoint();
	}

	public void mouseReleased(MouseEvent e) {
		super.mouseReleased(e);
		if (vg!=null)
			GraphNodeDragActions.mouseReleased(e);
		MouseOffSet = new Point(0,0);
	}

	public void mouseMoved(MouseEvent arg0) {}
	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
}
