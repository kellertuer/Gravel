package model;

import java.awt.Point;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.Vector;
/**
 * Loop edges, where start and endnode are equal
 * They are ellipsoids with one length and the proportion of the second length (length*proportion)
 * and a direction to wich the first length runs from the node.
 *  
 * @author Ronny Bergmann
 *
 */
public class VLoopEdge extends VEdge {
	
	private int length, direction;
	private double proportion;
	private boolean clockwise;
	private Point tempstart = new Point(0,0);
	/**
	 * Create new LoopEdge with
	 * @param i edge index
	 * @param w edge width	
	 * @param l length of long axis of the eclipse
	 * @param d direction of the long axis
	 * @param p part (times length is the smaller axis)
	 * @param c clockwise indicator
	 */
	public VLoopEdge(int i, int w, int l, int d, double p, boolean c) {
		super(i, w);
		length = l;
		direction = d;
		proportion = p;
		clockwise = c;
	}

	public VEdge clone() 
	{
		VEdge cloneedge = new VLoopEdge(getIndex(),width,length, direction, proportion, clockwise);
		return copyCommonProperties(cloneedge);
	}

	public Point getMax() {
		return new Point((new Double(getPath(tempstart,tempstart,1).getBounds().getMaxX())).intValue(),(new Double(getPath(tempstart,tempstart,1).getBounds().getMaxY())).intValue());
	}

	public Point getMin() {
		return new Point((new Double(getPath(tempstart,tempstart,1).getBounds().getMinX())).intValue(),(new Double(getPath(tempstart,tempstart,1).getBounds().getMinY())).intValue());
	}

	public GeneralPath getPath(Point Start, Point End, float zoom) {

		if ((Start.x!=End.x)||(Start.y!=End.y))
				return new GeneralPath();

		double rad = ((new Double(direction)).floatValue()-90f)*Math.PI/180f;
		double mainaxis = (double)length/2d;
		double minoraxis = mainaxis*proportion;
		
		//ellipse-middlepoint 
		tempstart = (Point) Start.clone();
		Point2D.Double ellm = new Point2D.Double((float)Start.x - length/2d*Math.sin(rad), (float)Start.y - length/2d*Math.cos(rad)); 
		GeneralPath p = new GeneralPath();
		p.moveTo((float)Start.x*zoom,(float)Start.y*zoom);
		//Generate the ellipse
		int precision = 540;
		for (int i=0; i<precision; i++)
		{
			double ellrad=0.0d;
			if (clockwise)
				ellrad = (double)i/((double) precision)*2d*Math.PI;
			else //for counter clockwise invert the actual degree
				ellrad = ((double)(precision-i-1))/((double) precision)*2d*Math.PI;
			//begin at the lower end of the ellipse
			//so a rotation moves this point into the startpoint
			double preX = minoraxis*Math.sin(ellrad);
			double preY = mainaxis*Math.cos(ellrad);
			//now rotate it to the direction (clockwise) and move it to the point
			double x = ellm.x - preX*Math.cos(rad) + preY*Math.sin(rad);
			double y = ellm.y + preX*Math.sin(rad) + preY*Math.cos(rad);
			
			p.lineTo((new Double(x)).floatValue()*zoom,(new Double(y)).floatValue()*zoom);
		}
		p.lineTo((float)End.x*zoom,(float)End.y*zoom);
		return p;
	}

	public int getType() {
		return VEdge.LOOP;
	}
	public void translate(int x, int y) {}
	
	public int getDirection() {
		return direction;
	}

	public void setDirection(int direction) {
		this.direction = direction;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public double getProportion() {
		return proportion;
	}

	public void setProportion(double proportion) {
		this.proportion = proportion;
	}
	public void setClockwise(boolean b)
	{
		clockwise = b;
	}
	public boolean isClockwise()
	{
		return clockwise;
	}
	public Vector<Point> getControlPoints()
	{
		double rad = ((new Double(direction)).floatValue()-90f)*Math.PI/180f;
		int ellmx = new Double((float)tempstart.x - length*Math.sin(rad)).intValue();
		int ellmy = new Double((float)tempstart.y - length*Math.cos(rad)).intValue();
		Vector<Point> p = new Vector<Point>();
		p.add(new Point(ellmx,ellmy));
		return p;
	}
	public void setControlPoints(Vector<Point> p)
	{
		if (p.size()==0)
			return;
		Point newcp = p.firstElement();
		//new length of the main axis
		length = (new Double(tempstart.distance(newcp.x,newcp.y))).intValue();
		//and compute the new angle
		double x = (-(double)newcp.x+(double)tempstart.x)/(double)length;
		double y = (-(double)newcp.y+(double)tempstart.y)/(double)length;

		if (y>=0) //Oberhalb
		{
			if (x<=0)
			 direction = 90 - (new Double(Math.acos(y)/Math.PI*180d)).intValue();
			else //x < 0
			 direction = 90 + (new Double(Math.acos(y)/Math.PI*180d)).intValue();				
		}
		else //y < 0
		{
			if (x<=0)
				direction = 270 + (new Double(Math.acos(Math.abs(y))/Math.PI*180d)).intValue();
			else
				direction = 270 - (new Double(Math.acos(Math.abs(y))/Math.PI*180d)).intValue();
		}
	}
	public boolean PathEquals(VEdge v)
	{
		if (v.getType()!=VEdge.LOOP)
			return false; //not the same type
		VLoopEdge tempv = (VLoopEdge)v;
		//if direction, length and proportion equal, they might share the same path		
		return ((length==tempv.getLength())&&(direction==tempv.getDirection())&&(proportion==tempv.getProportion()));
	}

}