package control.nurbs;

import io.GeneralPreferences;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.util.Iterator;
import java.util.Vector;


import model.MEdge;
import model.NURBSShapeFactory;
import model.VEdge;
import model.VGraph;
import model.VGraphInterface;
import model.VHyperEdge;
import model.VHyperEdgeShape;
import model.VHyperGraph;
import model.VItem;
import model.VNode;
import model.VOrthogonalEdge;
import model.VQuadCurveEdge;
import model.VSegmentedEdge;
import model.Messages.GraphConstraints;
import model.Messages.GraphMessage;
import view.VCommonGraphic;
import view.VGraphic;
import view.VHyperGraphic;
/**
 * Class for handling Clicks for Creation of a NURBS by Interpolation Points
 * 
 * - A Click creates a new Interpolation-Point that is added at the End of the Interpolation Point Vector
 * 
 * - A Drag creates a temporary shape with the mouseposition added as an Interpolation Point at the End of the vector
 *   The Point lies on the Curve (by interpolation) so Point Inversion is used to get it's position and while draging the Point is moved  
 *
 * @author Ronny Bergmann
 *
 */
public class InterpolationCreationHandler implements ShapeMouseHandler {
	VHyperGraph vhg = null;
	VCommonGraphic vgc;
	GeneralPreferences gp;
	Point MouseOffSet;
	boolean firstdrag = true;
	Point2D DragOrigin = null;
	Vector<Point2D> InterpolationPoints;
	int degree, actualInsertionIndex=-1;
	VHyperEdgeShape lastshape=null;

	public InterpolationCreationHandler(VHyperGraphic g)
	{
		vgc = g;
		vhg = g.getGraph();
		gp = GeneralPreferences.getInstance();
		MouseOffSet = new Point(0,0);
		degree = 5; //TODO: Std Value?
		InterpolationPoints = new Vector<Point2D>();
	}
	
	public Rectangle getSelectionRectangle()
	{ //No Selections possible here
		return null;
	}

	public void resetShape()
	{
		lastshape=null;
	}
	public Vector<Object> getShapeParameters()
	{
		Vector<Object> param = new Vector<Object>();
		param.setSize(NURBSShapeFactory.MAX_INDEX);
		param.set(NURBSShapeFactory.DEGREE, degree);
		param.set(NURBSShapeFactory.DISTANCE_TO_NODE,20); //TODO-Std Value
		param.set(NURBSShapeFactory.IP_POINTS, InterpolationPoints);
		return param;
	}
	private void UpdateShape()
	{
		if (InterpolationPoints.size()<=degree)
		{
			lastshape = new VHyperEdgeShape();
			return;
		}
		lastshape = NURBSShapeFactory.CreateShape("global interpolation", getShapeParameters());
	}

	public void setShapeParameters(Vector<Object> p)
	{
		if (dragged())
			return;
		VHyperEdgeShape d = NURBSShapeFactory.CreateShape("global interpolation", p);
		if (!d.isEmpty())
		{
			lastshape = d;
			vhg.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,GraphConstraints.UPDATE|GraphConstraints.HYPEREDGESHAPE,GraphConstraints.SELECTION));
		}
	}
	public VHyperEdgeShape getShape()
	{
		return lastshape;
	}
	public boolean dragged()
	{
		return (DragOrigin!=null)&&(!firstdrag);
	}
	private void internalReset()
	{
		//Only if a Block was started: End it...
		if ((DragOrigin!=null)&&(!firstdrag)) //We had an Drag an a Circle was created, draw it one final time
		{
			DragOrigin=null;
			vhg.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,GraphConstraints.BLOCK_END));			
		}
		actualInsertionIndex = -1;
	}
	private boolean containsPoint(Point2D p)
	{
		Iterator<Point2D> iter = InterpolationPoints.iterator();
		while (iter.hasNext())
		{
			Point2D actualPoint = iter.next();
			if (actualPoint.distance(p)<=0.00002d)
				return true;
		}
		return false;
	}
	//One every Click a potental Drag is initialized but firstdrag = true signals, that no Drag-Movement happened yet
	public void mousePressed(MouseEvent e) {
		firstdrag=true;
		boolean alt = ((InputEvent.ALT_DOWN_MASK & e.getModifiersEx()) == InputEvent.ALT_DOWN_MASK); // alt ?
		boolean shift = ((InputEvent.SHIFT_DOWN_MASK & e.getModifiersEx()) == InputEvent.SHIFT_DOWN_MASK); //shift ?
		if (alt||shift)
			return;
		MouseOffSet = e.getPoint(); //Aktuelle Position merken für eventuelle Bewegungen while pressed
		DragOrigin = new Point2D.Double((double)e.getPoint().x/((double)vgc.getZoom()/100d),(double)e.getPoint().y/((double)vgc.getZoom()/100)); //Rausrechnen des zooms
	}

	public void mouseDragged(MouseEvent e) {
		
		if (((InputEvent.ALT_DOWN_MASK & e.getModifiersEx()) == InputEvent.ALT_DOWN_MASK)||((InputEvent.SHIFT_DOWN_MASK & e.getModifiersEx()) == InputEvent.SHIFT_DOWN_MASK))
		{	
			internalReset();
			return;
		}
		
		//Handling selection Rectangle
		if (DragOrigin!=null)
		{
			//Update Rectangle
			MouseOffSet = e.getPoint(); //Aktuelle Position merken für eventuelle Bewegungen while pressed
			Point2D exactPointInGraph  = new Point2D.Double((double)e.getPoint().x/((double)vgc.getZoom()/100d),(double)e.getPoint().y/((double)vgc.getZoom()/100)); //Rausrechnen des zooms;
			if (firstdrag) //Add StartDragpoint
			{
				if (containsPoint(DragOrigin))
				{
					internalReset();
					return;
				}
				if(InterpolationPoints.size()==0) //No Point yet->add this point twice - start & end
				{
					InterpolationPoints.add((Point2D.Double) DragOrigin.clone()); 
					InterpolationPoints.add(DragOrigin);
					actualInsertionIndex = 1;
				}
				else //Else add as prelast element
				{
					int size = InterpolationPoints.size();
					InterpolationPoints.add(size-1,DragOrigin);
					actualInsertionIndex = size-1;
				}
				UpdateShape();
				vhg.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,GraphConstraints.BLOCK_START|GraphConstraints.UPDATE|GraphConstraints.HYPEREDGESHAPE,GraphConstraints.HYPEREDGE));
			}
			else
			{
				if ((!lastshape.isEmpty()))
				{
					if (!containsPoint(exactPointInGraph))
					{
						InterpolationPoints.set(actualInsertionIndex,exactPointInGraph);
						UpdateShape();
					}
				}
				vhg.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,GraphConstraints.UPDATE|GraphConstraints.HYPEREDGESHAPE,GraphConstraints.HYPEREDGE));
			}
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
	public void mouseClicked(MouseEvent e)
	{
		Point2D.Double newPoint = new Point2D.Double((double)e.getPoint().x/((double)vgc.getZoom()/100d),(double)e.getPoint().y/((double)vgc.getZoom()/100)); //Rausrechnen des zooms
		if (containsPoint(newPoint))
			return;
		vhg.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,GraphConstraints.BLOCK_START|GraphConstraints.UPDATE|GraphConstraints.HYPEREDGESHAPE,GraphConstraints.HYPEREDGE));
		if(InterpolationPoints.size()==0) //No Point yet->add this point twice - start & end
		{
			InterpolationPoints.add((Point2D.Double) newPoint.clone()); 
			InterpolationPoints.add(newPoint); 
		}
		else //Else add as prelast element
		{
			int size = InterpolationPoints.size();
			InterpolationPoints.add(size-1,newPoint);
		}
		UpdateShape();
		vhg.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,GraphConstraints.BLOCK_END));			
	}	
	public Point getMouseOffSet() {
		return MouseOffSet;
	}
	//Ignore Grid
	public void setGrid(int x, int y) {}
	public void setGridOrientated(boolean b) {}
}
