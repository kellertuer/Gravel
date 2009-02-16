package model;

import io.GeneralPreferences;

import java.awt.Color;
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
public abstract class VEdge extends VItem {
	
	public static class EdgeIndexComparator implements Comparator<VEdge>
	{
		public int compare(VEdge a, VEdge b) {
			if (a.getIndex() < b.getIndex())
				return -1;
			if (a.getIndex()==b.getIndex())
				return 0;
			else
				return 1;
		}
		
	}
	
	
	public static final int STRAIGHTLINE = 1;
	public static final int QUADCURVE = 2;
	public static final int SEGMENTED = 3;
	public static final int ORTHOGONAL = 4;
	public static final int LOOP = 5;
	
	protected int width;
	private Color colour; //Farbe des Knotens
	private int setCount; //Anzahl Mengen in denen der Knoten beteiligt ist, f�r Color 
	
	private VEdgeText Textvalues;
	private VEdgeLinestyle Linestyle;
	
	protected float arrSize = GeneralPreferences.getInstance().getIntValue("edge.arrsize");          // Size of the arrow segments
	protected float arrPart =  (float)GeneralPreferences.getInstance().getIntValue("edge.arrpart")/100;  		//Size of arrow height
	protected float arrAlpha =  GeneralPreferences.getInstance().getIntValue("edge.arralpha")/2; //Winkel der Pfeilspitze 0 =< alpa =< 45
	protected float arrPos =  (float)GeneralPreferences.getInstance().getIntValue("edge.arrpos")/100; //Position des Pfeils auf der Kante: 1 == spitze am Endknoten 0==Pfeilende am Startknoten je bündig
	
	
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
		setCount = 0;
		colour = Color.black;
		Textvalues = new VEdgeText();
		Linestyle = new VEdgeLinestyle();
	}
	/**
	 * Constructor for an edge with information about the arrow
	 * 
	 * @param i index of the edge - must be the same as the index in the mathematical corresponding edge
	 * @param w line width of the edge when drawn
	 * @param size size of the arrow (measured in px of the line he uses)
	 * @param part part of the arrow thats filled (so ist <1)
	 * @param alpha Arrow in the arrowhead 0<alpha<90
	 * @param pos position on the edge (0 means at the start 1 means at the end)
	 */
	public VEdge(int i,int w, float size, float part, float alpha, float pos)
	{
		super(i);
		width=w;
		setCount = 0;
		colour = Color.black;
		arrSize = size;
		arrPart = part;
		arrAlpha = alpha/2;
		arrPos = pos;
		Textvalues = new VEdgeText();
		Linestyle = new VEdgeLinestyle();
	}
	/**
	 * Initialize Edge with Text but without arrow
	 * @param i index
	 * @param w width
	 * @param td text distance
	 * @param tp text position
	 * @param ts text size
	 * @param tv text visibility
	 * @param tw text property (true=value, false=name) 
	 */
	public VEdge(int i, int w, VEdgeText t)
	{
		this(i,w);
		Textvalues = t;
	}
	public VEdge(int i, int w, VEdgeText t, VEdgeLinestyle l)
	{
		this(i,w);
		Textvalues = t;
		Linestyle = l;
	}
	public VEdge(int i, int w, VEdgeLinestyle l)
	{
		this(i,w);
		Linestyle = l;
	}

	public VEdge(int i,int w, float size, float part, float alpha, float pos, VEdgeText t)
	{
		this(i,w,size,part,alpha,pos);
		Textvalues = t;
	}
	public VEdge(int i,int w, float size, float part, float alpha, float pos, VEdgeText t, VEdgeLinestyle l)
	{
		this(i,w,size,part,alpha,pos);
		Textvalues = t;
		Linestyle = l;
	}
	public VEdge(int i,int w, float size, float part, float alpha, float pos, VEdgeLinestyle l)
	{
		this(i,w,size,part,alpha,pos);
		Linestyle = l;
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
	public abstract int getType();
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
		return Linestyle.modifyPath(getPath(Start,End,zoom), width, zoom);
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
		
		target.setArrowAngle(getArrowAngle());
		target.setArrowPart(getArrowPart());
		target.setArrowPos(getArrowPos());
		target.setArrowSize(getArrowSize());
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
		double p1distance = (length-(double)startsize-(double)endsize - arrSize)*arrPos + (double)startsize+1.0d; //Arrowhead
		double p2distance = p1distance + arrSize; 
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
		float beta = (float) (Math.PI*(90-arrAlpha)/360);
		//float adjCos = (float)(arrSize*Math.cos((arrAlpha/360)*Math.PI*2));
		float adjSin = (float)((new Double(Math.sqrt(2))).floatValue()*arrSize*zoom*Math.sin((arrAlpha/360)*Math.PI*2));
		float adjCosbeta = (float)((new Double(Math.sqrt(2))).floatValue()*arrSize*zoom*Math.cos(beta));
		//float adjSinbeta = (float)(arrSize*Math.sin(beta));
		float adjSize = arrSize*zoom;///zoom;
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
		path.lineTo(ahx-ex*adjSize*arrPart, ahy-ey*adjSize*arrPart);
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
	 * get actual Color of the Edge that is generated by the subsets the Edge is in
	 * @return the actual color
	 */
	public Color getColor()
	{
		return colour;
	}
	/**
	 * if an edge is added to a subset the subsets color is added here.
	 * 
	 * @param newc color of the subset the edge was added to
	 */
	public void addColor(Color newc)
	{
		int b=colour.getBlue()*setCount + newc.getBlue();
		int a=colour.getAlpha()*setCount + newc.getAlpha();
		int g=colour.getGreen()*setCount + newc.getGreen();
		int r=colour.getRed()*setCount + newc.getRed();
		setCount++;
		colour = new Color((r/setCount),(g/setCount),(b/setCount),(a/setCount));
	}
	/**
	 * if an edge is removed from a subset the color mus be removed too
	 * 
	 * @param newc color of the subset the edge was removed from
	 */
	public void removeColor(Color newc)
	{
		if (setCount > 1)
		{
			int b=colour.getBlue()*setCount - newc.getBlue();
			int a=colour.getAlpha()*setCount - newc.getAlpha();
			int g=colour.getGreen()*setCount - newc.getGreen();
			int r=colour.getRed()*setCount - newc.getRed();
			//Durch Rundungsfehler können dabei negative werte entstehen, diese also verhindern
			if (b<0) b=0;
			if (a<0) a=0;
			if (r<0) r=0;
			if (g<0) g=0;
			setCount--;
			colour = new Color((r/setCount),(g/setCount),(b/setCount),(a/setCount));
		}
		else
		{
			colour = Color.black;
			setCount = 0;
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
	 * Get the actual Size of the Arrow
	 * @return
	 */
	public float getArrowSize()
	{
		return arrSize;
	}
	/**
	 * Set the Siize of the Arrow
	 * @param i
	 */
	public void setArrowSize(float i)
	{
		arrSize = i;
	}
	/**
	 * get the Arrowpart. That is the part from the arrowhead along the Edge thats filled with color. 0 means none, 1 means the whole arrow to arrowsize is filled
	 * @return
	 */
	public float getArrowPart()
	{
		return arrPart;
	}
	/**
	 * Set the Arrowpart. That is the part from the arrowhead along the Edge thats filled with color. 0 means none, 1 means the whole arrow to arrowsize is filled
	 * @param i the new arrowpart - mus be between 0 and 1
	 */
	public void setArrowPart(float i)
	{
		arrPart = i;
	}
	/**
	 * get the Angle in the arrowhead
	 * @return
	 */
	public float getArrowAngle()
	{
		return arrAlpha*2.0f;
	}
	/**
	 * set the angle in the arrowhead to
	 * @param i Degree
	 */
	public void setArrowAngle(float i)
	{
		//Speicherung intern halbiert 
		arrAlpha = i/2.0f;
	}
	/**
	 * Position of the arrow on the edge. 0 means at the start 1 means at the end.
	 * @return position
	 */
	public float getArrowPos() {
		return arrPos;
	}
	/**
	 * Set the position of the Arrow. 0 Start 1 End or in between
	 * @param i
	 */
	public void setArrowPos(float i) {
		arrPos = i;
	}
	/**
	 * return the class for the textproperties
	 * @return
	 */
	public VEdgeText getTextProperties()
	{
		return this.Textvalues;
	}
	public void setTextProperties(VEdgeText newtext)
	{
		Textvalues = newtext;
	}
	public VEdgeLinestyle getLinestyle() {
		return Linestyle;
	}
	public void setLinestyle(VEdgeLinestyle linestyle) {
		Linestyle = linestyle;
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
}
