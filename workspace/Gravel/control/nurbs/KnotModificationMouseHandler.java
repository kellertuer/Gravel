package control.nurbs;

import io.GeneralPreferences;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.Observable;

import view.VCommonGraphic;
import view.VHyperGraphic;

import model.NURBSShape;
import model.VHyperEdge;
import model.VHyperGraph;

/**
 * Knot Modification Mouse Handling uses Mouse actions to change Knots of a given NURBSShape
 * These Modifications are
 * - Addition of new Knots if someone clicked near the curve (within selwith+linewidth)
 * - Removal of knots (if someone clicks within mentioned range of a knot)
 *
 * This handler gets initialized with one of these Modi.
 * Though removal might change the Curve, that is not checked, because there is the capability of undo
 * 
 * @author Ronny Bergmann
 *
 */
public class KnotModificationMouseHandler implements ShapeModificationMouseHandler {

	public final static int ADDITION = 1;
	public final static int REMOVAL = 2;
	
	VHyperGraph vhg;
	double zoom;
	VHyperEdge HyperEdgeRef;
	GeneralPreferences gp;
	Point MouseOffSet = new Point(0,0);;
	boolean firstdrag = true;
	double DragStartProjection = Double.NaN;
	NURBSShape temporaryShape=null;

	/**
	 * Init the KnotModification to a given Modus. Use the public constraints of this class for a modus
	 * Despite that (as always) the view is needed and an given hyperedge of the model
	 * 
	 * @param type
	 */
	public KnotModificationMouseHandler(int type, VHyperGraph g, int hyperedgeindex)
	{
		vhg = g;
		gp = GeneralPreferences.getInstance();
		gp.addObserver(this);
		if (vhg.modifyHyperEdges.get(hyperedgeindex)==null)
			return; //Nothing can be done here.
		HyperEdgeRef = vhg.modifyHyperEdges.get(hyperedgeindex);
		temporaryShape = HyperEdgeRef.getShape().clone(); //Clone with eventual Decorations (if that decoration clones)
	}
	
	public Point2D getDragStartPoint()
	{
		if (!dragged())
			return null;
		return temporaryShape.CurveAt(DragStartProjection);
	}

	public Point2D getDragPoint()
	{
		if (!dragged())
			return null;
		return new Point2D.Double(MouseOffSet.getX()/((double)zoom),MouseOffSet.getY()/((double)zoom));		
	}

	public NURBSShape getShape()
	{
		return temporaryShape;
	}
	public void resetShape()
	{
		temporaryShape = HyperEdgeRef.getShape().clone(); //Clone with eventual Decorations (if that decoration clones)
	}

	public boolean dragged() {
		// TODO Auto-generated method stub
		return false;
	}

	public Point getMouseOffSet() {
		// TODO Auto-generated method stub
		return null;
	}

	public Rectangle getSelectionRectangle() {
		// No selection possible here
		return null;
	}

	public void setGrid(int x, int y) {
		// No Grid reactions here
	}

	public void setGridOrientated(boolean b) {
		// No Grid reactions here
	}

	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public void mouseDragged(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public void mouseMoved(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public void update(Observable o, Object arg) {
		if ((o==gp)&&(arg=="zoom"))
			zoom = gp.getFloatValue("zoom");		
	}
}
