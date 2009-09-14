package control.nurbs;

import io.GeneralPreferences;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.Observable;
import java.util.Vector;


import model.VHyperEdge;
import model.NURBSShape;
import model.VHyperGraph;
import model.Messages.GraphConstraints;
import model.Messages.GraphMessage;
/**
 * Class for handling Drags for Free Modification
 * 
 * - Drag in Range of the Shape begins a Drag of the Projection point to the Mouse-Position
 *  
 * @author Ronny Bergmann
 *
 */
public class FreeModificationHandler implements ShapeModificationMouseHandler {
	VHyperGraph vhg;
	VHyperEdge HyperEdgeRef;
	float zoom;
	GeneralPreferences gp;
	Point MouseOffSet = new Point(0,0);;
	boolean firstdrag = true;
	double DragStartProjection = Double.NaN;
	NURBSShape temporaryShape=null;
	
	public FreeModificationHandler(VHyperGraph g, int hyperedgeindex)
	{
		vhg = g;
		gp = GeneralPreferences.getInstance();
		gp.addObserver(this);
		zoom = gp.getFloatValue("zoom");
		if (vhg.modifyHyperEdges.get(hyperedgeindex)==null)
			return; //Nothing can be done here.
		HyperEdgeRef = vhg.modifyHyperEdges.get(hyperedgeindex);
		temporaryShape = HyperEdgeRef.getShape().clone(); //Clone with eventual Decorations (if that decoration clones)
	}
	public void removeGraphObservers()
	{
		gp.deleteObserver(this);
	}

	public Rectangle getSelectionRectangle()
	{ //No Selections possible here
		return null;
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

	public boolean dragged()
	{
		return ((!Double.isNaN(DragStartProjection))&&(!firstdrag));
	}
	private void internalReset()
	{
		//Only if a Block was started: End it...
		if ((!Double.isNaN(DragStartProjection))&&(!firstdrag)) //We had an Drag an a Circle was created, draw it one final time
		{
			DragStartProjection=Double.NaN;
			//Set shape
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
			//Update temporary Shape
			MouseOffSet = e.getPoint(); //Aktuelle Position merken für eventuelle Bewegungen while pressed
			Point2D exactPointInGraph = new Point2D.Double((double)e.getPoint().x/((double)zoom),(double)e.getPoint().y/((double)zoom)); //Rausrechnen des zooms
			temporaryShape.movePoint(DragStartProjection, exactPointInGraph);

			if (firstdrag) //If wirst drag - start Block
				vhg.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,HyperEdgeRef.getIndex(),GraphConstraints.BLOCK_START|GraphConstraints.UPDATE,GraphConstraints.HYPEREDGE));
			else		//continnue Block
				vhg.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,HyperEdgeRef.getIndex(),GraphConstraints.UPDATE,GraphConstraints.HYPEREDGE));
		}
		MouseOffSet = e.getPoint();
		firstdrag = false;
	}

	public void mouseReleased(MouseEvent e) {
		//nur falls schon gedragged wurde nochmals draggen
		if (!firstdrag)
		{
			if (!((e.getPoint().x==-1)||(e.getPoint().y==-1))) //kein Reset von außerhalb wegen modusumschaltung
				mouseDragged(e); //Das gleiche wie als wenn man bewegt, nur ist danach kein Knoten mehr bewegter Knoten		
		}
		internalReset();
	}

	public void mouseMoved(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mouseClicked(MouseEvent e) {}
	public Point getMouseOffSet() {
		return MouseOffSet;
	}

	//Ignore Grid
	public void setGrid(int x, int y) {}
	public void setGridOrientated(boolean b) {}

	//There are no Parameters for the Factor to be returned in this mode
	public Vector<Object> getShapeParameters() { return null; }
	public void setShapeParameters(Vector<Object> p) {}

	public void update(Observable o, Object arg) {
		if ((o==gp)&&(arg=="zoom"))
			zoom = gp.getFloatValue("zoom");
	}
}
