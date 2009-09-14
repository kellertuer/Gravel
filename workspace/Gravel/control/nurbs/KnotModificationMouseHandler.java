package control.nurbs;

import io.GeneralPreferences;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.Observable;
import java.util.Vector;

import model.NURBSShape;
import model.NURBSShapeProjection;
import model.VHyperEdge;
import model.VHyperGraph;
import model.Messages.GraphConstraints;
import model.Messages.GraphMessage;
import view.VCommonGraphic;

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
	
	VHyperGraph vhg;
	double zoom;
	VHyperEdge HyperEdgeRef;
	GeneralPreferences gp;
	Point MouseOffSet = new Point(0,0);;
	boolean firstdrag = true;
	double DragStartProjection = Double.NaN;
	NURBSShape temporaryShape=null;
	boolean add=false, remove = false;
	/**
	 * Init the KnotModification to a given Modus. Use the public constraints of this class for a modus
	 * Despite that (as always) the view is needed and an given hyperedge of the model
	 * 
	 * @param modState specify the type of Knot Modification (ADD or REMOVE from VComonGraphic)
	 * @param g VHyperGraph containing the Hyperedge and its Shape
	 * @param hyperedgeindex the hyper edge that is modified
	 */
	public KnotModificationMouseHandler(int modState, VHyperGraph g, int hyperedgeindex)
	{
		vhg = g;
		gp = GeneralPreferences.getInstance();
		gp.addObserver(this);
		zoom = gp.getFloatValue("zoom");
		if (vhg.modifyHyperEdges.get(hyperedgeindex)==null)
			return; //Nothing can be done here.
		HyperEdgeRef = vhg.modifyHyperEdges.get(hyperedgeindex);
		temporaryShape = HyperEdgeRef.getShape().clone(); //Clone with eventual Decorations (if that decoration clones)
		setModificationState(modState);
	}
	public void removeGraphObservers()
	{
		gp.deleteObserver(this);
	}
	/**
	 * Change the state of modification
	 * Which is
	 * - Addition of Knots or
	 * - Removal of Knots
	 * If both are set to true with i, addition is set.
	 * @param i new State
	 */
	public void setModificationState(int i) {
		add = ((i&VCommonGraphic.ADD) > 0);
		remove = ((i&VCommonGraphic.REMOVE) > 0);
		if (add&&remove)
			remove = false;
		internalReset();
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
		HyperEdgeRef = vhg.modifyHyperEdges.get(HyperEdgeRef.getIndex());
		temporaryShape = HyperEdgeRef.getShape().clone(); //Clone with eventual Decorations (if that decoration clones)
	}

	public boolean dragged() {
		//We have no drag so untl there is something handling drags...return jus false
		return false;
	}

	private void internalReset()
	{
		//Only if a Block was started: End it...
		if ((!Double.isNaN(DragStartProjection))&&(!firstdrag)) //We had an Drag, but Reset ends that - though the shape is set a last time
		{
			DragStartProjection=Double.NaN;
			HyperEdgeRef.setShape(temporaryShape);
			vhg.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,GraphConstraints.BLOCK_END));
		}
		DragStartProjection=Double.NaN;
		resetShape();
	}
	//One every Click a potental Drag is initialized but firstdrag = true signals, that no Drag-Movement happened yet
	public void mousePressed(MouseEvent e) {
		firstdrag=true;
		boolean alt = ((InputEvent.ALT_DOWN_MASK & e.getModifiersEx()) == InputEvent.ALT_DOWN_MASK); // alt ?
		boolean shift = ((InputEvent.SHIFT_DOWN_MASK & e.getModifiersEx()) == InputEvent.SHIFT_DOWN_MASK); //shift ?
		if (alt||shift)
			return;
		MouseOffSet = e.getPoint(); //Aktuelle Position merken für eventuelle Bewegungen while pressed
		Point pointInGraph = new Point(Math.round(e.getPoint().x/((float)zoom)),Math.round(e.getPoint().y/((float)zoom))); //Rausrechnen des zooms
		if (temporaryShape.isPointOnCurve(pointInGraph, 2.0d)) //Are we near the Curve?
		{
			DragStartProjection = temporaryShape.ProjectionPointParameter(pointInGraph);
		}
	}

	public void mouseDragged(MouseEvent e)
	{
		if (((InputEvent.ALT_DOWN_MASK & e.getModifiersEx()) == InputEvent.ALT_DOWN_MASK)||((InputEvent.SHIFT_DOWN_MASK & e.getModifiersEx()) == InputEvent.SHIFT_DOWN_MASK))
		{
			internalReset();
			return;
		}
		//Handling selection Rectangle
		if (!Double.isNaN(DragStartProjection)) //We've initialized a Drag
		{
		}
		MouseOffSet = e.getPoint();
//		firstdrag = false; //We never really start a drag so it stays just true
	}

	public void mouseReleased(MouseEvent e) {
		//nur falls schon gedragged wurde nochmals draggen
		if (!firstdrag)
		{
			if (!((e.getPoint().x==-1)||(e.getPoint().y==-1))) //kein Reset von außerhalb wegen modusumschaltung
				mouseDragged(e);
		}
		internalReset();
	}

	public void mouseMoved(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mouseClicked(MouseEvent e) {
		MouseOffSet = e.getPoint(); //Aktuelle Position in der Grafik
		Point pointInGraph = new Point(Math.round(e.getPoint().x/((float)zoom)),Math.round(e.getPoint().y/((float)zoom))); //Rausrechnen des zooms
		int dist = HyperEdgeRef.getWidth()+gp.getIntValue("vgraphic.selwidth");
		NURBSShapeProjection proj = new NURBSShapeProjection(temporaryShape, pointInGraph);
		if (proj.getResultPoint().distance(pointInGraph)<=dist) //Are we near the Curve?
		{
			if (add||remove) //We're active
				vhg.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,HyperEdgeRef.getIndex(),GraphConstraints.BLOCK_START|GraphConstraints.UPDATE|GraphConstraints.HYPEREDGESHAPE,GraphConstraints.HYPEREDGE));

			if (add)
				temporaryShape.addKnot(proj.getResultParameter());
			else if (remove)
				temporaryShape.removeKnotNear(proj.getResultPoint(), dist);
			if (add||remove) //We're active
			{
				HyperEdgeRef.setShape(temporaryShape);
				resetShape();
				vhg.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,GraphConstraints.BLOCK_END));
//				vhg.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,HyperEdgeRef.getIndex(),GraphConstraints.UPDATE|GraphConstraints.HYPEREDGESHAPE,GraphConstraints.HYPEREDGE));
			}
			internalReset();
		}
	}
	public Point getMouseOffSet() {
		return MouseOffSet;
	}

	//Ignore Grid & Selection
	public void setGrid(int x, int y) {}
	public void setGridOrientated(boolean b) {}
	public Rectangle getSelectionRectangle() {
		return null;
	}

	//There are no Parameters for the Factor to be returned in this mode
	public Vector<Object> getShapeParameters() { return null; }
	public void setShapeParameters(Vector<Object> p) {}

	public void update(Observable o, Object arg) {
		if ((o==gp)&&(arg=="zoom"))
			zoom = gp.getFloatValue("zoom");		
	}
}
