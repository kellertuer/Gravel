package control.nurbs;

import java.awt.geom.Point2D;

import model.NURBSShape;
import control.DragMouseHandler;

/**
 * mouse drag handling for all mouse modes implementing shape modification
 * these are the scond modi of HyperEdgeShape-Stuff where the basic shape is modified
 *
 * Each implementing Class should provide at least one way to Modify shapes
 *
 * @author Ronny Bergmann
 *
 */
public interface ShapeModificationMouseHandler extends DragMouseHandler 
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
	 * Set the Shape to a specific shape - this means, that any drags
	 * are endet and the shape was set externally to the parameter
	 *
	 * @param s New Shape for the GUI
	 */
	public void setShape(NURBSShape s);
	
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
