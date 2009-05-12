package model;

import java.awt.Point;
import java.awt.geom.GeneralPath;
import java.util.Vector;
/**
 * The Quadcurve is a Bezier-Curve with one Controlpoint that specifies the shape of the egde
 * @author ronny
 *
 */
public class VQuadCurveEdge extends VEdge
{
	private Point bezierpoint;
	/**
	 * Initialize the QuadCurve with its
	 * @param i index and
	 * @param w linewidth and the
	 * @param p bezierpoint
	 */
	public VQuadCurveEdge(int i, int w, Point p) {
		super(i, w);
		bezierpoint = p;
	}
	
	public Vector<Point> getControlPoints()
	{
		Vector<Point> p = new Vector<Point>();
		p.add(bezierpoint);
		return p;
	}
	public void setControlPoints(Vector<Point> p)
	{
		if (p.size()>0)
			bezierpoint = p.firstElement();
	}
	
	public GeneralPath getPath(Point p1, Point p2, float zoom) 
	{
		GeneralPath p = new GeneralPath();
		p.moveTo(p1.x*zoom,p1.y*zoom);
		p.quadTo(bezierpoint.x*zoom,bezierpoint.y*zoom,p2.x*zoom, p2.y*zoom);
		return p;
	}
	public int getEdgeType() {
		return VEdge.QUADCURVE;
	}
	public void translate(int x, int y)
	{
		bezierpoint.translate(x,y);
		if (bezierpoint.x < 0)
			bezierpoint.x = 0;
		if (bezierpoint.y < 0)
			bezierpoint.y = 0;
	}
	public Point getMax()
	{	return bezierpoint;
	}
	public Point getMin()
	{	return bezierpoint;
	}
	public VEdge clone()
	{
		VEdge cloneedge = new VQuadCurveEdge(getIndex(),width,(Point)bezierpoint.clone());
		return copyCommonProperties(cloneedge);
	}
	public boolean PathEquals(VEdge v)
	{
		if (v.getEdgeType()!=VEdge.QUADCURVE)
			return false; //not the same type
		Point vcp = v.getControlPoints().firstElement();
		//both are quadcurves so they share same path if the bezierpoint is equal
		return ((bezierpoint.x==vcp.x)&&(bezierpoint.y==vcp.y));
	}
}