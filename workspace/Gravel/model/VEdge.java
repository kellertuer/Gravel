package model;

import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.Vector;

/**
 * This class represents the visual information of an edge.
 * It's abstract to be sure that the 4 types implement functions that rely on the type
 * 
 * @author Ronny Bergmann
 *
 */
public abstract class VEdge extends VItem {
	
	public static final int STRAIGHTLINE = 1;
	public static final int QUADCURVE = 2;
	public static final int SEGMENTED = 3;
	public static final int ORTHOGONAL = 4;
	public static final int LOOP = 5;
	
	protected int width;
	
	private VEdgeText text;
	private VEdgeLinestyle linestyle;
	private VEdgeArrow arrow;

	/**
	 * Constructor that initializes the Arrow-Part of the Edge with the GeneralPreferences Standard
	 * 
	 * @param i index of the edge - must be the same as the index in the mathematical corresponding edge
	 * @param w line width of the edge when drawn
	 */
	public VEdge(int i,int w)
	{
		super(i);
		width=w;
		text = new VEdgeText();
		linestyle = new VEdgeLinestyle();
		arrow = new VEdgeArrow();
	}
	/**
	 * Create an Edge with specific Arrow, Text and LineStyle Elements
	 * @param i index of the new Edge
	 * @param w Width of the new edge
	 * @param arr Arrow of Edge
	 * @param t Text-Specifications of the edge
	 * @param l Linestyle values of the Edge
	 */
	public VEdge(int i,int w, VEdgeArrow arr, VEdgeText t, VEdgeLinestyle l)
	{
		super(i);
		width=w;
		arrow = arr;
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
	public abstract boolean PathEquals(VEdge v);
	/**
	 * get the Edge Type. Return the Type of the actual Edge 
	 * @return the edge type constant
	 */
	public abstract int getEdgeType();
	/**
	 * Returns the Path of an edge ignoring the line style, so only for computational cases
	 * @param Start Coordinates of the Start Node
	 * @param End Coordinates of the End Node
	 * @param zoom Zoomfactor in your computational environment
	 * @return a path equal to a solid line style path
	 */public abstract GeneralPath getPath(Point Start, Point End,float zoom);
	/**
	 * Translate an edge. The Start and End-Node are translated already so this method must move all internal
	 * points of an edge
	 * @param x movement in x-direction
	 * @param y movement in y-direction
	 */
	public abstract void translate(int x,int y);
	/**
	 * get the maximum (bottom right) point of the rectangle border of the internal edge points
	 * if there are none - return a Point with Coordinates (0,0)
	 * @return
	 */
	public abstract Point getMax();
	/**
	 * get the minimum (top left) point of the rectangle border of the internal edge points
	 * if there are none - return a Point with Coordinates (Int.Max, Int.Max)
	 * @return
	 */	
	public abstract Point getMin();
	/**
	 * Clone the edge, create a new Edge with the same values and return it
	 */
	public abstract VEdge clone();
		/**
	 * returns the path that is suitable for drawing includes the line style
	 * 
	 * @param Start Start Node Coordinates
	 * @param End End Node Coordinates
	 * @param zoom Zoomfactor of your drwaig environment
	 * @return
	 */
	public GeneralPath getDrawPath(Point Start, Point End, float zoom)
	{
		return linestyle.modifyPath(getPath(Start,End,zoom), width, zoom);
	}
	/**
	 * Apply the Properties of this Edge to another (possibly clone) the given parameter edge is modified
	 * @param target The Edge the Properties should be Applied to
	 * @return the modified target
	 */
	protected VEdge copyCommonProperties(VEdge target)
	{
		target.setTextProperties(this.getTextProperties().clone());
		
		target.setLinestyle(this.getLinestyle().clone());
		
		target.setSelectedStatus(this.getSelectedStatus());
		
		target.setArrow(this.getArrow().clone());
		return target;
	}
	/**returns a Shape for the Arrow on a directed Edge at the position in %
	 * The Path of the shape contains at first and last position the arrowhead
	 * 
	 * @param Start Position of the startnode
	 * @param End Position of the endnode
	 * @param startsize radius(!) of the startnode
	 * @param endsize radius(!) of the endnode
	 * @param zoom Zoomfactor for the size
	 * @return
	 */
	public Shape getArrowShape(Point Start, Point End, int startsize, int endsize, float zoom) 
	{
		double[] coords = new double[2];
    	double x = 0.0, y = 0.0, lastx=0.0, lasty = 0.0;
		//Länge der Kante
		double length=getLength(Start,End);
		double p1distance = (length-(double)startsize-(double)endsize - getArrow().getSize())*getArrow().getPos() + (double)startsize+1.0d; //Arrowhead
		double p2distance = p1distance + getArrow().getSize(); 
		if (p2distance > length) //Rundungsfehler
			p2distance=length;
		Point2D.Double p1=new Point2D.Double(0,0),p2 = new Point2D.Double(0,0);
		boolean p1done = false, p2done=false;
		//Berechnung von Pfeilspitze auf der Kante und ende auf der Kante (ohne part/part=1.0) ohne zoom
		GeneralPath edgepath = getPath(Start, End, 1.0f);
		PathIterator edgepathiter = edgepath.getPathIterator(null, 0.001);
		length = 0.0d; double seglength=0.0d;
		while( !edgepathiter.isDone() ) 
		{
		   int type = edgepathiter.currentSegment(coords);
		   x = coords[0]; y = coords[1];
		   if (type==PathIterator.SEG_MOVETO)
		   {} //we need from the first moveto the coords as lastxlasty
		   else if (type==PathIterator.SEG_LINETO)
		   {
			   seglength = (new Point2D.Double(lastx,lasty)).distance(x,y);
			   length += seglength;
		   }
		   if ((!p1done)&&(length>p1distance)) //the newest point is farther away than p1 should be
		   {
			   p1done = true;
			   p1distance = p1distance +seglength - length; //restlength of p1 on this seg
			   //From last only a part to x thats left from distance
			   p1 = new Point2D.Double(lastx + (x-lastx)*p1distance/seglength,lasty + (y-lasty)*p1distance/seglength);
		   }
		   if ((!p2done)&&((length>p2distance)||(length==p2distance))) //the newest point is farther away than p2 should be
		   {
			   p2done = true;
			   p2distance = p2distance +seglength - length; //restlength of p1 on this seg
			   //From last only a part to x thats left from distance
			   p2 = new Point2D.Double(lastx + (x-lastx)*p2distance/seglength,lasty + (y-lasty)*p2distance/seglength);
		   }
		   lastx = x; lasty = y;
		   edgepathiter.next();
		 } //So we got the length. now we need to now the distance to both arrow points on the edge
		//Much calculated but we got start end endpoint of the arrow, so let's build the arrow itself
		GeneralPath path = new GeneralPath();
		float beta = (float) (Math.PI*(90-getArrow().getAngle())/720);
		//float adjCos = (float)(arrSize*Math.cos((arrAlpha/360)*Math.PI*2));
		float adjSin = (float)((new Double(Math.sqrt(2))).floatValue()*getArrow().getSize()*zoom*Math.sin((getArrow().getAngle()/720)*Math.PI*2));
		float adjCosbeta = (float)((new Double(Math.sqrt(2))).floatValue()*getArrow().getSize()*zoom*Math.cos(beta));
		//float adjSinbeta = (float)(arrSize*Math.sin(beta));
		float adjSize = getArrow().getSize()*zoom;///zoom;
		float ey,ex;
		//Die Richtung ist von p1 kommend
		ex = new Double(p2.x - p1.x).floatValue();
		ey = new Double(p2.y - p1.y).floatValue();
		
		//und normieren
		float abs_e = (float)Math.sqrt(ex*ex + ey*ey);
		ex = (float)(ex/abs_e);
		ey = (float)(ey/abs_e);
		//ex und ey ist die Richtung des Pfeils normiert
		//nun den zoom einrechnen
		float ahx = (float)p2.x*zoom;
		float ahy = (float)p2.y*zoom;
		//Pfeil bauen
		path.moveTo(ahx,ahy);
		//path.lineTo(ahx + (ey - ex)*adjSize, ahy - (ex + ey)*adjSize);
		path.lineTo(ahx + ey*adjSin - ex*adjCosbeta, ahy - ex*adjSin - ey*adjCosbeta);
		path.lineTo(ahx-ex*adjSize*getArrow().getPart(), ahy-ey*adjSize*getArrow().getPart());
		path.lineTo(ahx - ey*adjSin - ex*adjCosbeta, ahy + ex*adjSin - ey*adjCosbeta);
		//2 Kanten zu einem Punkt zwischen dem MiitelPunkt hinten und der Spitze 
		path.lineTo(ahx,ahy);
		path.closePath();
		return path;
	}
	/**
	 * Calculates the length of the edge by iterating along
	 * @param Start Position of the Startnode
	 * @param End Position of the Endnode
	 * @return
	 */
	private double getLength(Point Start,Point End)
	{
		double ret = 0.0d;
		double[] coords = new double[2];
    	double x = 0.0, y = 0.0, lastx=0.0, lasty = 0.0;
		GeneralPath edgepath = getPath(Start, End, 1.0f);
		PathIterator edgepathiter = edgepath.getPathIterator(null, 0.001); 
		while( !edgepathiter.isDone() ) 
		{
		   int type = edgepathiter.currentSegment(coords);
		    x = coords[0]; y = coords[1];
		    if (type==PathIterator.SEG_MOVETO)
		    {} //we need from the first moveto the coords as lastxlasty
		    else if (type==PathIterator.SEG_LINETO)	
		    {
		    	ret += (new Point2D.Double(lastx,lasty)).distance(x,y);
		    }
		   lastx = x; lasty = y;
		   edgepathiter.next();
		 }
		return ret;
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
		double[] coords = new double[2];
    	double x = 0.0, y = 0.0, lastx=0.0, lasty = 0.0;
		//Länge der Kante
		double length=getLength(Start,End);
		double pointdistance = part*length;
		GeneralPath edgepath = getPath(Start, End, 1.0f);
		PathIterator edgepathiter = edgepath.getPathIterator(null, 0.001);
		length = 0.0d; double seglength=0.0d;
		while( !edgepathiter.isDone() ) 
		{
		   int type = edgepathiter.currentSegment(coords);
		   x = coords[0]; y = coords[1];
		   if (type==PathIterator.SEG_MOVETO)
		   {} //we need from the first moveto the coords as lastxlasty
		   else if (type==PathIterator.SEG_LINETO)
		   {
			   seglength = (new Point2D.Double(lastx,lasty)).distance(x,y);
			   length += seglength;
		   }
		   if (length>pointdistance) //the newest point is farther away than the point should be
		   {
			   pointdistance = pointdistance +seglength - length; //restlength of p1 on this seg
			   //From last only a part to x thats left from distance
			   return new Point((new Long(Math.round(lastx + (x-lastx)*pointdistance/seglength))).intValue(),(new Long(Math.round(lasty + (y-lasty)*pointdistance/seglength))).intValue());
		   }
		   lastx = x; lasty = y;
		   edgepathiter.next();
		 } //So we got the length. now we need to now the distance to both arrow points on the edge
		//Much calculated but we got start end endpoint of the arrow, so let's build the arrow itself
		return new Point(0,0);
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
	public Point2D.Double getDirectionatPointonEdge(Point Start,Point End,double part)
	{
		double[] coords = new double[2];
    	double x = 0.0, y = 0.0, lastx=0.0, lasty = 0.0;
		//Länge der Kante
		double length=getLength(Start,End);
		double pointdistance = part*length;
		Point2D.Double lastpoint = new Point2D.Double(0.0,0.0);
		GeneralPath edgepath = getPath(Start, End, 1.0f);
		PathIterator edgepathiter = edgepath.getPathIterator(null, 0.001);
		length = 0.0d; double seglength=0.0d;
		while( !edgepathiter.isDone() ) 
		{
		   int type = edgepathiter.currentSegment(coords);
		   x = coords[0]; y = coords[1];
		   if (type==PathIterator.SEG_MOVETO)
		   {} //we need from the first moveto the coords as lastxlasty
		   else if (type==PathIterator.SEG_LINETO)
		   {
			   lastpoint = new Point2D.Double(lastx,lasty);
			   seglength = lastpoint.distance(x,y);
			   length += seglength;
		   }
		   if (length>pointdistance) //the newest point is farther away than the point should be
		   {
			   if ((lastx==0)&&(lasty==0))
			   {
				   return new Point2D.Double(0.0,0.0); //no direction given!
			   }
			   pointdistance = pointdistance +seglength - length; //restlength of p1 on this seg
			   //From last only a part to x thats left from distance
			   Point2D.Double thispoint = new Point2D.Double((lastx + (x-lastx)*pointdistance/seglength),(lasty + (y-lasty)*pointdistance/seglength));
			   //The direction is now the direction from the last to this point
			   return new Point2D.Double(thispoint.x - lastpoint.x,thispoint.y - lastpoint.y);
		   }
		   lastx = x; lasty = y;
		   edgepathiter.next();
		 } //So we got the length. now we need to now the distance to both arrow points on the edge
		//Much calculated but we got start end endpoint of the arrow, so let's build the arrow itself
		return new Point2D.Double(0.0,0.0);
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
	public Point getTextCenter(Point startNodePos, Point endNodePos)
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
		Point p = getPointonEdge(startNodePos,endNodePos, part);
		Point2D.Double dir = getDirectionatPointonEdge(startNodePos,endNodePos, part);
		double l = dir.distance(0.0d,0.0d);
		//and norm dir
		dir.x = dir.x/l; dir.y = dir.y/l;
		//And now from the point on the edge the distance
		Point m = new Point(0,0); //middle of the text
		if (top) //Countter Clockwise rotation of dir
		{
			m.x = p.x + (new Long(Math.round(((double)getTextProperties().getDistance())*dir.y)).intValue());
			m.y = p.y - (new Long(Math.round(((double)getTextProperties().getDistance())*dir.x)).intValue());
		}
		else //invert both direction elements
		{
			m.x = p.x - (new Long(Math.round(((double)getTextProperties().getDistance())*dir.y)).intValue());
			m.y = p.y + (new Long(Math.round(((double)getTextProperties().getDistance())*dir.x)).intValue());
		}
		return m;
	}
	public VEdgeLinestyle getLinestyle() {
		return linestyle;
	}
	public void setLinestyle(VEdgeLinestyle plinestyle) {
		linestyle = plinestyle;
	}
	/**
	 * Returns an empty Vector of Points. Each Edge with Controllpoints must implement this method and return its CPs
	 * @return the Edge Controllpoints, if they exist, else an empty Vector
	 */
	public Vector<Point> getControlPoints()
	{
		return new Vector<Point>(); //return empty vector, here are no CPs
	}
	/**
	 * Set the Controllpoints to the given Values. Does nothing, if not implemented
	 * @param points
	 */
	public void setControlPoints(Vector<Point> points){}
	/**
	 * @param parrow the arrow to set
	 */
	public void setArrow(VEdgeArrow parrow) {
		arrow = parrow;
	}
	/**
	 * @return the arrow
	 */
	public VEdgeArrow getArrow() {
		return arrow;
	}
	public int getType()
	{
		return VItem.EDGE;
	}
}