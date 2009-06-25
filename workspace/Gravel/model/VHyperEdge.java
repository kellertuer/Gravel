package model;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.Comparator;

import view.VCommonGraphic;

/**
 * This class represents the visual information of an edge.
 * It's abstract to be sure that the 4 types implement functions that rely on the type
 * 
 * @author Ronny Bergmann
 *
 */
public class VHyperEdge extends VItem {
	
	public static class EdgeIndexComparator implements Comparator<VHyperEdge>
	{
		public int compare(VHyperEdge a, VHyperEdge b) {
			if (a.getIndex() < b.getIndex())
				return -1;
			if (a.getIndex()==b.getIndex())
				return 0;
			else
				return 1;
		}
		
	}		
	private int width, minMargin;
	private VEdgeText text;
	private VEdgeLinestyle linestyle;
	private NURBSShape shape;
	/**
	 * Constructor that initializes the Arrow-Part of the Edge with the GeneralPreferences Standard
	 * 
	 * @param i index of the edge - must be the same as the index in the mathematical corresponding edge
	 * @param w line width of the edge when drawn
	 * @param d minimal distance between any node of the VHyperEdge and its shape
	 */
	public VHyperEdge(int i,int w, int d)
	{
		super(i);
		minMargin = d;
		width=w;
		text = new VEdgeText();
		linestyle = new VEdgeLinestyle();
		shape = new NURBSShape();
	}
	/**
	 * Create an Edge with specific Arrow, Text and LineStyle Elements
	 * @param i index of the new Edge
	 * @param w Width of the new edge
	 * @param arr Arrow of Edge
	 * @param t Text-Specifications of the edge
	 * @param l Linestyle values of the Edge
	 */
	public VHyperEdge(int i,int w, int d, NURBSShape s, VEdgeText t, VEdgeLinestyle l)
	{
		super(i);
		width=w;
		shape = s;
		minMargin = d;
		text = t;
		linestyle = l;
	}
	/**
	 * Translate an HyperEdge (mainly by moving its Shape) - Might invalidate the HyperEgde
	 * @param x
	 * @param y
	 */
	public void translate(int x,int y)
	{
		shape.translate(x,y);
	}
	/**
	 * Returns true if the actual edge and the edge v are equal.
	 * That means, that every controlpoint is equal, so that they might share the same path (if they share start and endnode)
	 * Therefore the type must also be the same
	 * 
	 * @param v the edge checked against this edge
	 * 
	 * @return true, if the type and all controlpoints are equal, else false
	 */
	public boolean PathEquals(VHyperEdge v)
	{
		if ((v.getShape()==null)||(shape==null))
			return false;
		
		return false;
	}
	/**
	 * get the maximum (bottom right) point of the rectangle border of the internal edge points
	 * if there are none - return a Point with Coordinates (0,0)
	 * @return
	 */
	public Point getMax()
	{
		if (shape==null) //No Shape set yet, so no Bounding Box itself, or
						//in other words the nodes itself to themselves
			return new Point(0,0);
		else
		{
			Point2D p = shape.getMax();
			return new Point (Math.round((float)p.getX()),Math.round((float)p.getY()));
		}
	}
	/**
	 * get the minimum (top left) point of the rectangle border of the internal edge points
	 * if there are none - return a Point with Coordinates (Int.Max, Int.Max)
	 * @return
	 */	
	public Point getMin()
	{
		if (shape==null) //No Shape set yet, so no Bounding Box itself, or
			//in other words the nodes itself to themselves
			return new Point(Integer.MAX_VALUE,Integer.MAX_VALUE);
		else
		{
			Point2D p = shape.getMin();
			return new Point (Math.round((float)p.getX()),Math.round((float)p.getY()));
		}
	}
	/**
	 * Clone the edge, create a new Edge with the same values and return it
	 */
	public VHyperEdge clone()
	{
		//Clone the shape with decorators iff there are some (don't strip)
		return new VHyperEdge(getIndex(), width, minMargin, shape.clone(), text.clone(), linestyle.clone());
	}
	public boolean containsPoint(Point p, double variance)
	{
		if (shape==null)
			return false;
		else
		{
			return shape.isPointOnCurve(new Point2D.Double(p.x,p.y), variance);			
		}
	}
	/**
	 * Get the Width of the edge
	 * @return width of the edge
	 */
	public int getWidth()
	{
		return width;
	}
	/**
	 * Set the width of the Edge
	 * @param i new width
	 */
	public void setWidth(int i)
	{
		width = i;
	}
	
	public int getMinimumMargin()
	{
		return minMargin;
	}
	public void setMinimumMargin(int m)
	{
		minMargin = m;
	}
	/**
	 * return the class for the textproperties
	 * @return
	 */
	public VEdgeText getTextProperties()
	{
		return this.text;
	}
	public void setTextProperties(VEdgeText newtext)
	{
		text = newtext;
	}
	public VEdgeLinestyle getLinestyle() {
		return linestyle;
	}
	public void setLinestyle(VEdgeLinestyle plinestyle) {
		linestyle = plinestyle;
	}
	/**
	 * Set the Shape, You should notify the vGraph about this change
	 * @param s the new Shape
	 */
	public void setShape(NURBSShape s) {
		shape = s;
	}
	/**
	 * Get the actual Shape
	 * @return the arrow
	 */
	public NURBSShape getShape() {
		return shape;
	}
	/**
	 * return center Point of Text Display, so that anybody can either use that or calculate any edge of the 
	 * Text-Panel to display it correctly
	 * 
	 * @return
	 */
	public Point getTextCenter()
	{
		float pos; boolean top; double part;
		if (getTextProperties().getPosition() > .5f)
		{ //below edge
			pos = getTextProperties().getPosition() - .5f;
			top = false;
			part = 1-((double)pos)*2.0d; //from the end - so 1- at the part
		}
		else
		{
			pos = getTextProperties().getPosition();
			top = true;
			part = ((double)pos)*2.0d;
		}
		Point2D p = shape.CurveRelativeAt(part);
		Point2D dir = shape.DerivateCurveRelativeAt(1,part);
		double l = dir.distance(0.0d,0.0d);
		//and norm dir
		dir = new Point2D.Double(dir.getX()/l, dir.getY()/l);
		//And now from the point on the edge the distance
		Point m = new Point(0,0); //middle of the text
		if (top) //Countter Clockwise rotation of dir
		{
			m.x = (new Long(Math.round(p.getX() + ((double)getTextProperties().getDistance())*dir.getY())).intValue());
			m.y = (new Long(Math.round(p.getY() - ((double)getTextProperties().getDistance())*dir.getX())).intValue());
		}
		else //invert both direction elements
		{
			m.x = (new Long(Math.round(p.getX() - ((double)getTextProperties().getDistance())*dir.getY())).intValue());
			m.y = (new Long(Math.round(p.getY() + ((double)getTextProperties().getDistance())*dir.getX())).intValue());
		}
		return m;
	}
	public int getType()
	{
		return VItem.HYPEREDGE;
	}
}