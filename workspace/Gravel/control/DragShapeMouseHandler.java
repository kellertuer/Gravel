package control;

import java.util.Vector;

import model.VHyperEdgeShape;

/**
 * mouse drag handling for all mouse modes implementing shape modfication 
 * especially built for NURBS Modifications
 *
 * Each implementing Class should provide one wad to create or modify shapes
 *
 * @author Ronny Bergmann
 *
 */
public interface DragShapeMouseHandler extends DragMouseHandler 
{
	public void resetShape();
	
	public Vector<Object> getShapeParameters();

	public VHyperEdgeShape getShape();
	
}
