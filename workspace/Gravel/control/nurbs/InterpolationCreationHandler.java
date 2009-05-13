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
import model.NURBSShape;
import model.VHyperGraph;
import model.VItem;
import model.VNode;
import model.VOrthogonalEdge;
import model.VQuadCurveEdge;
import model.VSegmentedEdge;
import model.Messages.GraphConstraints;
import model.Messages.GraphMessage;
import model.Messages.NURBSCreationMessage;
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
public class InterpolationCreationHandler implements ShapeCreationMouseHandler {
	VHyperGraph vhg = null;
	VCommonGraphic vgc;
	GeneralPreferences gp;
	Point MouseOffSet;
	boolean firstdrag = true;
	Point2D DragOrigin = null;
	Vector<Point2D> InterpolationPoints;
	int degree, PointAdditionStatus, hyperedgeindex;
	NURBSShape lastshape=null;

	public InterpolationCreationHandler(VHyperGraphic g, int HyperEdgeIndex)
	{
		vgc = g;
		vhg = g.getGraph();
		gp = GeneralPreferences.getInstance();
		MouseOffSet = new Point(0,0);
		degree = 3; //TODO: Define Std-Value for nURBSShape-Degree
		InterpolationPoints = new Vector<Point2D>();
		PointAdditionStatus = NURBSCreationMessage.ADD_END;
		hyperedgeindex = HyperEdgeIndex;
	}
	
	public void reInit()
	{
		degree = 3; //TODO: Define Std-Value for nURBSShape-Degree
		InterpolationPoints = new Vector<Point2D>();
		updateShape();
	}
	public Rectangle getSelectionRectangle()
	{ //No Selections possible here
		return null;
	}

	public NURBSShape getShape()
	{
		return lastshape;
	}

	public void resetShape()
	{
		lastshape=null;
	}
	private void updateShape()
	{
		lastshape = NURBSShapeFactory.CreateShape(getShapeParameters());
	}
	public void setShapeParameters(NURBSCreationMessage nm)
	{
		//if shape type differs, ignore the parameters, because they might be unsuitable
		if ((nm.getType()!=NURBSCreationMessage.INTERPOLATION)||(nm.getPoints()==null))
		{
			reInit();
			return;
		}
		if (dragged())
			return;	
		InterpolationPoints = nm.getPoints();
		PointAdditionStatus = nm.getStatus();
		degree = nm.getDegree();
		vhg.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,hyperedgeindex,GraphConstraints.BLOCK_START|GraphConstraints.UPDATE|GraphConstraints.HYPEREDGESHAPE|GraphConstraints.CREATION,GraphConstraints.HYPEREDGE));
		updateShape();
		vhg.modifyHyperEdges.get(hyperedgeindex).setShape(lastshape);
		vhg.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,hyperedgeindex,GraphConstraints.BLOCK_END));			
	}
	public NURBSCreationMessage getShapeParameters()
	{
		return new NURBSCreationMessage(degree, PointAdditionStatus, InterpolationPoints);
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
			vhg.modifyHyperEdges.get(hyperedgeindex).setShape(lastshape);
			vhg.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,GraphConstraints.BLOCK_END));			
		}
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

	/**
	 * Search for the two consecutive Points in the first vector, whose distances are in sum minimal and return the second of these
	 * This result is identical with the addition point the second Parameter should be added by thinking of adding it in between the vector
	 * 
	 * If there exists any Element that is null in the Vector, Vector.size() is returned
	 *
	 * @param points Set of Point to search in 
	 * @param newPoint new point for insertion
	 * @return
	 */
	private int getSecondOfNearestPair(Vector<Point2D> points, Point2D newPoint)
	{
		if (points.lastElement()==null) //if the last element is null, the point can be inserted there
			return points.size()-1; 
		double lastdistance = points.lastElement().distance(newPoint);
		double min = Double.MAX_VALUE;
		int returnindex = -1;
		for (int i=0; i<points.size(); i++)
		{
			if (points.get(i)==null)
				return points.size();
			double actualdistance = points.get(i).distance(newPoint);
			if (lastdistance+actualdistance<min)
			{
				min = lastdistance+actualdistance;
				returnindex = i;
			}
			lastdistance = actualdistance;
		}
		return returnindex;
	}
	//
	// Mouse Handling
	//
	
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
				InterpolationPoints.add(DragOrigin);
				updateShape();
				vhg.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,hyperedgeindex,GraphConstraints.BLOCK_START|GraphConstraints.UPDATE|GraphConstraints.HYPEREDGESHAPE|GraphConstraints.CREATION,GraphConstraints.HYPEREDGE));
			}
			else
			{
				if ((!lastshape.isEmpty()))
				{
					if (!containsPoint(exactPointInGraph))
					{
						InterpolationPoints.set(InterpolationPoints.size()-1,exactPointInGraph);
						updateShape();
					}
				}
				vhg.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,hyperedgeindex,GraphConstraints.UPDATE|GraphConstraints.HYPEREDGESHAPE|GraphConstraints.CREATION,GraphConstraints.HYPEREDGE));
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
		//New Point without Zoom
		Point2D.Double newPoint = new Point2D.Double((double)e.getPoint().x/((double)vgc.getZoom()/100d),(double)e.getPoint().y/((double)vgc.getZoom()/100));
		if (containsPoint(newPoint)) //Do not add twice
			return;
		vhg.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,hyperedgeindex,GraphConstraints.BLOCK_START|GraphConstraints.UPDATE|GraphConstraints.HYPEREDGESHAPE|GraphConstraints.CREATION,GraphConstraints.HYPEREDGE));
		
		if (PointAdditionStatus==NURBSCreationMessage.ADD_END)
			InterpolationPoints.add((Point2D.Double) newPoint.clone()); 
		else
			InterpolationPoints.add(getSecondOfNearestPair(InterpolationPoints,newPoint), (Point2D) newPoint.clone());
		
		updateShape();
		vhg.modifyHyperEdges.get(hyperedgeindex).setShape(lastshape);
		vhg.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,GraphConstraints.BLOCK_END));			
	}	
	public Point getMouseOffSet() {
		return MouseOffSet;
	}
	//Ignore Grid
	public void setGrid(int x, int y) {}
	public void setGridOrientated(boolean b) {}
}
