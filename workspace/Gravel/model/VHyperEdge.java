package model;

import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.Comparator;
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
//	private VHyperEdgeForm form;

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
		//form = new VHyperEdgeform();
	}
	/**
	 * Create an Edge with specific Arrow, Text and LineStyle Elements
	 * @param i index of the new Edge
	 * @param w Width of the new edge
	 * @param arr Arrow of Edge
	 * @param t Text-Specifications of the edge
	 * @param l Linestyle values of the Edge
	 */
	public VHyperEdge(int i,int w, /* VHyperEdgeForm f, */ VEdgeText t, VEdgeLinestyle l)
	{
		super(i);
		width=w;
//		form = f;
		text = t;
		linestyle = l;
	}
	// Abstract Methoden, die von jedem Typ überschrieben werden müssen
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
		return false;
	}
	/**
	 * get the maximum (bottom right) point of the rectangle border of the internal edge points
	 * if there are none - return a Point with Coordinates (0,0)
	 * @return
	 */
	public Point getMax()
	{
		return null;
	}
	/**
	 * get the minimum (top left) point of the rectangle border of the internal edge points
	 * if there are none - return a Point with Coordinates (Int.Max, Int.Max)
	 * @return
	 */	
	public Point getMin()
	{
		return null;
	}
	/**
	 * Clone the edge, create a new Edge with the same values and return it
	 */
	public VHyperEdge clone()
	{
		return new VHyperEdge(getIndex(), width, /*form,*/ text.clone(), linestyle.clone());
	}
	/**
	 * Calculates the length of the edge by iterating along
	 * @param Start Position of the Startnode
	 * @param End Position of the Endnode
	 * @return
	 */
	private double getLength(Point Start,Point End)
	{
		return 0.0d;
	}
	/**
	 * getPointonEdge
	 * returns the point that is at the given part from the Start Point
	 * So the point is on the edge at edge.lengt * part
	 * 
	 * @param Start Coordinates of the Startpoint
	 * @param End Coordinates of the Endpoint of the Edge
	 * @param part distance in % of the endge length from the start to the point required on the edge
	 * @return the point at the adge at given part
	 */
	public Point getPointonEdge(Point Start,Point End,double part)
	{
		return null;
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
	public boolean containsPoint(Point p)
	{
		return false;
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
	 * @param parrow the arrow to set
	 */
//	public void setForm(VHyperEdgeForm f) {
//		form = f;
//	}
	/**
	 * @return the arrow
	 */
//	public VHyperEdgeForm getForm() {
//		return form;
//	}
}