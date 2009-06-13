package control.nurbs;

import java.awt.geom.Point2D;
import java.util.Observer;

import model.NURBSShape;
import control.DragMouseHandler;

/**
 * mouse drag handling for all mouse modes implementing shape modification
 * these are the scond modi of HyperEdgeShape-Stuff where the basic shape is modified
 *
 * Each implementing Class should provide at least one way to Modify shapes
 * Each implementing Class is also an Observer, because it should react on changes in GeneralPreferences
 * mainly the zoomfactor, but they also might want to watch other changes
 *
 *
 * @author Ronny Bergmann
 *
 */
public interface ShapeModificationMouseHandler extends DragMouseHandler, Observer
{
	
	/**
	 * Reset Shape to last situation in the HyperEdge given as reference
	 */
	public void resetShape();
	
	/**
	 * Get Shape for drawing, if not null, else null is returned
	 * @return
	 */
	public NURBSShape getShape();
	
	/**
	 * get the startpoint (without zoom) in the graph, if a drag is active
	 * else null is returned
	 * @return
	 */
	public Point2D getDragStartPoint();
	/**
	 * get the actual mouseposition (without zoom) in the graph, if a drag is active
	 * else null is returned
	 * @return
	 */
	public Point2D getDragPoint();
	
}
