package control;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import view.VGraphic;
import view.VHyperGraphic;

import model.VGraph;
import model.VHyperGraph;

/**
 * General mouse drag handling for all mouse modes implemented in Gravel
 * this class is abstract, so this can't be an mouse mode itself.
 * Handles the general mouse movement actions such as shift-drag on background
 * @author Ronny Bergmann
 *
 */
public abstract class DragMouseHandler implements MouseListener, MouseMotionListener 
{
	VGraph vg;
	VHyperGraph vhg;
	SelectionDragListener SelectionDragActions;
	CommonNodeDragListener NodeDragActions;
	CommonEdgeDragListener EdgeDragActions;
	/**
	 * Initialize the mouse drag handler bound to a specifig VGRaph on with the drags work
	 * 
	 * @param g
	 */
	public DragMouseHandler(VGraphic g)
	{
		vg = g.getGraph();
		SelectionDragActions= new SelectionDragListener(g);
		NodeDragActions = new CommonNodeDragListener(g);
		EdgeDragActions = new CommonEdgeDragListener(g);
	}
	
	public DragMouseHandler(VHyperGraphic g)
	{
		vhg = g.getGraph();
		SelectionDragActions= new SelectionDragListener(g);
		NodeDragActions = new CommonNodeDragListener(g);
	}

	/**
	 * get actual MouseOffSet, abstract, so that every mouse mode has to implement this.
	 * mouseoffset is the startpoint of a drag
	 * @return
	 */
	public abstract Point getMouseOffSet();
	/**
	 * Indicated whetther someoe is just dragging or not
	 * @return
	 */
	public boolean dragged()
	{
		if (vg!=null)
			return SelectionDragActions.dragged()||NodeDragActions.dragged()||EdgeDragActions.dragged();
		else if (vhg!=null)
			return SelectionDragActions.dragged()||NodeDragActions.dragged();
		else
			return false;
	}
	/** set whether the nodes are set to a gridpoint after dragging or not. Handler must not implement this
	 * 
	 */
	public void setGridOrientated(boolean b){}
	/** update Gridinfo
	 * 
	 */
	public void setGrid(int x, int y){}
	/**
	 * For Displaying the Selection Rectangle
	 * 
	 * @return the rectangle if it exists, else null
	 */
	public Rectangle getSelectionRectangle()
	{
		return SelectionDragActions.getSelectionRectangle();
	}

	public void mousePressed(MouseEvent e)
	{
		SelectionDragActions.mousePressed(e);
		NodeDragActions.mousePressed(e);
		if (vg!=null)
			EdgeDragActions.mousePressed(e);
	}
	
	public void mouseDragged(MouseEvent e) 
	{
		SelectionDragActions.mouseDragged(e);
		NodeDragActions.mouseDragged(e);
		if (vg!=null)
			EdgeDragActions.mouseDragged(e);
	}

	public void mouseReleased(MouseEvent e) {
		SelectionDragActions.mouseReleased(e);
		NodeDragActions.mouseReleased(e);
		if (vg!=null)
			EdgeDragActions.mouseReleased(e);
	}
}
