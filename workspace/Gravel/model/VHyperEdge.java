package model;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Vector;

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
	protected int width;
	private VEdgeText text;
	private VEdgeLinestyle linestyle;
	private VHyperEdgeShape shape;
	/**
	 * Constructor that initializes the Arrow-Part of the Edge with the GeneralPreferences Standard
	 * 
	 * @param i index of the edge - must be the same as the index in the mathematical corresponding edge
	 * @param w line width of the edge when drawn
	 */
	public VHyperEdge(int i,int w)
	{
		super(i);
		width=w;
		text = new VEdgeText();
		linestyle = new VEdgeLinestyle();
		shape = new VHyperEdgeShape();
	}
	/**
	 * Create an Edge with specific Arrow, Text and LineStyle Elements
	 * @param i index of the new Edge
	 * @param w Width of the new edge
	 * @param arr Arrow of Edge
	 * @param t Text-Specifications of the edge
	 * @param l Linestyle values of the Edge
	 */
	public VHyperEdge(int i,int w, VHyperEdgeShape s, VEdgeText t, VEdgeLinestyle l)
	{
		super(i);
		width=w;
		shape = s;
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
		return new VHyperEdge(getIndex(), width, shape, text.clone(), linestyle.clone());
	}
	/**
	 * getDirectionatPointonEdge
	 * returns the Direction the edge has at the point that is at the given part from the Start Point
	 * So the direction is on the edge at edge.lengt * part
	 * 
	 * @param Start Coordinates of the Startpoint
	 * @param End Coordinates of the Endpoint of the Edge
	 * @param part distance in % of the endge length from the start to the point required on the edge
	 * @return the Direction as a vector
	 */
	public Point2D.Double getDirectionatPointonEdge()
	{
		return null;
	}
	public boolean containsPoint(Point p, double variance)
	{
		if (shape==null)
			return false;
		else
		{
			return shape.isPointOnCurve(new Point2D.Double(p.x,p.y), variance);
			//TODO: Use COnvex Hull instead of Bounding Box? Or even a better thing?
			
		}
	}
	/**
	 * Validate the Shape of the 
	 * @param nodepoints
	 * @return
	 */
	public boolean validateShape(Vector<Point> nodepoints)
	{
		if (shape==null)
			return false;
		else
		{
			Iterator<Point >ni = nodepoints.iterator();
			while (ni.hasNext())
			{
//				if (!containsPoint(ni.next()))
					return false;
			}
			return true; //All inside
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
	public void setShape(VHyperEdgeShape s) {
		shape = s;
	}
	/**
	 * Get the actual Shape
	 * @return the arrow
	 */
	public VHyperEdgeShape getShape() {
		return shape;
	}
}