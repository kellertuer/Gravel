package control.nurbs;

import io.GeneralPreferences;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.Vector;

import model.*;
import model.Messages.*;
import view.VCommonGraphic;
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
	int degree, PointAdditionStatus, hyperedgeindex, DragOriginIndex=-1;
	NURBSShape lastshape=null, MessageCurve=new NURBSShape();

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
		MessageCurve=vhg.modifyHyperEdges.get(HyperEdgeIndex).getShape();
		if (MessageCurve.isEmpty()||((MessageCurve.getDecorationTypes()&NURBSShape.FRAGMENT)!=NURBSShape.FRAGMENT))
			MessageCurve=new NURBSShape(); //Jst keep it if its for an Subcurve Replacement
	}
	public void removeGraphObservers()
	{} //Nothing to remove
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
		if ( (nm==null) || (nm.getType()!=NURBSCreationMessage.PERIODIC_INTERPOLATION) || (!nm.isValid()))
		{ //Unsuitable, reset to initial Values
			reInit();
			return;
		}
		if (dragged())
			return;	
		//really something changed
			boolean notify = ((InterpolationPoints!=nm.getPoints())||(degree!=nm.getDegree())||(!MessageCurve.CurveEquals(nm.getCurve())));	
		NURBSCreationMessage local = nm.clone();
		InterpolationPoints = local.getPoints();
		PointAdditionStatus = local.getStatus();
		MessageCurve = nm.getCurve();
		degree = nm.getDegree();			
		if (notify)
		{
			vhg.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,hyperedgeindex,GraphConstraints.BLOCK_START|GraphConstraints.UPDATE|GraphConstraints.HYPEREDGESHAPE|GraphConstraints.CREATION,GraphConstraints.HYPEREDGE));
			updateShape();
			vhg.modifyHyperEdges.get(hyperedgeindex).setShape(lastshape);
			vhg.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,GraphConstraints.BLOCK_END));
		}
	}

	public NURBSCreationMessage getShapeParameters()
	{
		if (MessageCurve.isEmpty()||((MessageCurve.getDecorationTypes()&NURBSShape.FRAGMENT)!=NURBSShape.FRAGMENT)) //Normal
			return new NURBSCreationMessage(degree, PointAdditionStatus, InterpolationPoints);
		else //Subcurve
			return new NURBSCreationMessage((NURBSShapeFragment)MessageCurve, PointAdditionStatus, InterpolationPoints);			
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
		DragOriginIndex=-1;
		resetShape();
	}
	/**
	 * Returns the index of the Point that is within selection width of the given Point p
	 * @param p
	 * @return
	 */
	private int containsPoint(Point2D p)
	{
		Iterator<Point2D> iter = InterpolationPoints.iterator();
		while (iter.hasNext())
		{
			Point2D actualPoint = iter.next();
			if (actualPoint.distance(p)<=(new Integer(gp.getIntValue("vgraphic.selwidth"))).doubleValue())
				return InterpolationPoints.indexOf(actualPoint);
		}
		return -1;
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
		DragOriginIndex = containsPoint(DragOrigin); //Save whether we are moving a Point or not
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
		if (points.size()==0)
			return 0;
		if (points.lastElement()==null) //if the last element is null, the point can be inserted there
			return points.size()-1; 
		Point2D lastP;
		if (MessageCurve.isEmpty())
			lastP = points.lastElement(); //.distance(newPoint);
		else 
			lastP = MessageCurve.CurveAt(((NURBSShapeFragment)MessageCurve).getStart()); //.distance(newPoint);
		double min = Double.MAX_VALUE;
		int returnindex = -1;
		for (int i=0; i<points.size(); i++)
		{
			if (points.get(i)==null)
				return points.size();
			double a = lastP.distance(newPoint);
			double b =  points.get(i).distance(newPoint);
			double c = lastP.distance(points.get(i));
			//In the minimal case a+b = c so the quotient is one. if we substract 1, we search for the smallest value
			double value = (a+b)/c-1;
			if (value<min)
			{
				min = value;
				returnindex = i;
			}
			lastP = (Point2D)points.get(i).clone();
		}
		if (!MessageCurve.isEmpty())
		{
			double a = lastP.distance(newPoint);
			double b =  MessageCurve.CurveAt(((NURBSShapeFragment)MessageCurve).getEnd()).distance(newPoint);
			double c = lastP.distance(MessageCurve.CurveAt(((NURBSShapeFragment)MessageCurve).getEnd()));
			double value = (a+b)/c-1;
			if (value<min)
				returnindex = points.size();
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
				if (DragOriginIndex==-1) //New Point, add
				{
					if (PointAdditionStatus==NURBSCreationMessage.ADD_BETWEEN)
					{ //In Between
						DragOriginIndex = this.getSecondOfNearestPair(InterpolationPoints, DragOrigin);
						InterpolationPoints.add(DragOriginIndex,DragOrigin);
					}
					else //At the End, then DragOrigin Stays -1
						InterpolationPoints.add(DragOrigin);
				}
				//else Moving existentPoint, reinitialize lastshape
				updateShape();
				vhg.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,hyperedgeindex,GraphConstraints.BLOCK_START|GraphConstraints.UPDATE|GraphConstraints.HYPEREDGESHAPE|GraphConstraints.CREATION,GraphConstraints.HYPEREDGE));	
			}
			else
			{
				int MousePosInShape = containsPoint(exactPointInGraph);
				if ((DragOriginIndex!=-1) &&((MousePosInShape==DragOriginIndex)||(MousePosInShape==-1)))
				{ //Added in Between or Movement - move correct one
					InterpolationPoints.set(DragOriginIndex,exactPointInGraph);
					updateShape();
				}
				else if (MousePosInShape==-1)
				{ //Added in the end...update last CP
					InterpolationPoints.set(InterpolationPoints.size()-1,exactPointInGraph);
					updateShape();
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
		Point2D.Double newPoint = new Point2D.Double((double)e.getPoint().x/((double)vgc.getZoom()/100d),(double)e.getPoint().y/((double)vgc.getZoom()/100));
		if ( ((e.getModifiers()&MouseEvent.BUTTON1_MASK)>0) && ((e.getModifiers()&MouseEvent.CTRL_MASK)==0)) //Left click
		{
			//New Point without Zoom
			if (containsPoint(newPoint)!=-1) //Do not add twice
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
		//Only react, if Right Click or Strg+Left Click
		else if ( ((e.getModifiers()&MouseEvent.BUTTON3_MASK) > 0) || ( (e.getModifiers()& (MouseEvent.BUTTON1_MASK|MouseEvent.CTRL_MASK))>0) ) //Right or crtl+Click
		{
			int index = containsPoint(newPoint);
			if (index==-1)
				return;
			vhg.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,hyperedgeindex,GraphConstraints.BLOCK_START|GraphConstraints.UPDATE|GraphConstraints.HYPEREDGESHAPE|GraphConstraints.CREATION,GraphConstraints.HYPEREDGE));

			InterpolationPoints.remove(index);
			
			updateShape();
			vhg.modifyHyperEdges.get(hyperedgeindex).setShape(lastshape);
			vhg.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,GraphConstraints.BLOCK_END));
		}
	}	
	public Point getMouseOffSet() {
		return MouseOffSet;
	}
	//Ignore Grid
	public void setGrid(int x, int y) {}
	public void setGridOrientated(boolean b) {}
}
