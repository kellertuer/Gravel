package model;

import java.awt.Point;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.Vector;

import javax.vecmath.Point3d;

/**
 * This Class represents the Shape of an individual VHyperEdge
 * Its shape is based on NURBS and a minimal distance it should have from each node
 * This Minimum Distance is used to evaluate the validity of the Shape
 * @see VHyperEdge
 * 
 * Other Methods are
 * - Min and Max of the Bounding Box of the ControlPolygon
 * - TODO Addition & Removal of Controlpoints
 * - TODO 
 * - TODO Constructor with Interpolation-Points
 * - TODO Movement of an IP (possible ?)
 * - TODO Get&Set single CPs
 * - TODO (needed?) get&set knots &weights
 * - TODO In/Decrease Degree of the polynonials 
 * 
 * @author Ronny Bergmann
 *
 */
public class VHyperEdgeShape {

	private Vector<Double> t; //knots
	private Vector<Double> w; //weights
	private Vector<Point2D> b; //ControlPoints
	private Vector<Point3d> homogeneousb; //b in homogeneous coordinates multiplied by weight

	private int minDist;
	private int m,n,d; //n is the number of CP, d the Order of the polynom-splines, m=n+d+1

	/**
	 * Create an empty shape so nothing ever happens but its at least not null
	 */
	public VHyperEdgeShape()
	{
		t = new Vector<Double>();
		w = new Vector<Double>();
		b = new Vector<Point2D>();
		n=0; m=0; d=0;
		homogeneousb = new Vector<Point3d>();
	}
	/**
	 * Init an HyperEdgeShape with
	 * @param knots nots of the NURBS
	 * @param cpoints Controlpoints of the NURBS
	 * @param weights weights of the CP 
	 * @param dist minimal distance the curve should have from each node (whose are not saved here)
	 */
	public VHyperEdgeShape(Vector<Double> knots, Vector<Point2D> cpoints, Vector<Double> weights, int dist)//, int degree)
	{
		minDist = dist;
		setCurveTo(knots,cpoints,weights);
	}
	/**
	 * Set the Curve to another NURBS
	 * @param knots
	 * @param cpoints
	 * @param weights
	 */
	public void setCurveTo(Vector<Double> knots, Vector<Point2D> cpoints, Vector<Double> weights)
	{
		t = knots;
		b = cpoints;
		w = weights;
		n = cpoints.size()-1;
		m = t.size()-1;
		d = t.size()-b.size()-1;
		InitHomogeneous();
	}
	/**
	 * Initialization of the internal homogeneous Vector
	 * Should be called everytime either the b or w vector are completly exchanged
	 */
	private void InitHomogeneous()
	{
		homogeneousb = new Vector<Point3d>();
		Iterator<Point2D> ib =  b.iterator();
		while (ib.hasNext()) //Modify to be 3D Coordinates (homogeneous 2D)
		{
			Point2D p = ib.next();
			double weight = w.get(b.indexOf(p));
			Point3d newp = new Point3d(p.getX(),p.getY(),weight);
			newp.set(newp.x*weight, newp.y*weight, weight);
			homogeneousb.add(newp);
		}		
	}
	
	/**
	 * Get the Curve as a piecewise approximated linear Java Path
	 * @param stepsize Size in the Intervall two points on the path differ
	 * TODO: Vary that value to an maximum distance thwo points on the path should have (as Bezier-Paths do)
	 * @return
	 */
	public GeneralPath getCurve(double stepsize) //Adapt to a length on the curve?
	{
		//Intervallborders
		double first = t.firstElement();
		double last = t.lastElement();
		double actual = first;
		GeneralPath path = new GeneralPath();
		Point2D.Double f = NURBSCurveAt(first);
		path.moveTo((new Double(f.x)).floatValue(), (new Double(f.y)).floatValue());
		actual+=stepsize;
		while (actual<=last)
		{
			f = NURBSCurveAt(actual);
			path.lineTo((new Double(f.x)).floatValue(), (new Double(f.y)).floatValue());
			actual+=stepsize;
		}
		path.closePath();
		
		return path;
	}
	/**
	 * Find the interval u \in [t.get(j),t.get(j+1)] and return the index j
	 * 
	 * because the first and last d+1 values of t are assumed equal, the 
	 * @param u
	 * @return
	 */
	private int findSpan(double u)
	{
		if (u==t.lastElement())
			return t.indexOf(t.lastElement()); //first value of t equal to t.get(m)==t.lastElement - which is m-d		
		//Binary Search for the intervall
		int low = d; //because the first d+1 are equal too
		int high = m-d; //see above
		int mid = (low+high)/2;
		while ((u<t.get(mid)) || (u>=t.get(mid+1)))
		{ 
			if (u < t.get(mid))
					high = mid;
			else
				low = mid;
			mid = (low+high)/2;
		} //get the first t AFTER u in Variable j
		return mid;
	}
	/**
	 * Private Method to evaluate the Curve at given point u \in [t_0,t_m]
	 * @param u
	 * @return
	 */
	private Point2D.Double NURBSCurveAt(double u)
	{		
		int j = findSpan(u);
		Point3d erg = NURBSRek(u,j,d); //Result in homogeneous Values
		if (erg.z==0) //
			return new Point2D.Double(erg.x,erg.y);
		else
			return new Point2D.Double(erg.x/erg.z,erg.y/erg.z);
	}
	/**
	 * Private Method to evaluate the Curve at given point u \in [t_0,t_m]
	 * Returns the Derivate Direction at given Position u
	 * @param u
	 * @return
	 */
	private Point2D.Double NURBSDerivateCurveAt(double u)
	{		
		//Explanation see Formular by the NURBS Book p. 59
		int j = findSpan(u);
		Point3d derivate = NURBSRek(u,j,d-1); //Result in homogeneous Values
		derivate.scale(d/(t.get(j+d)-t.get(j)));
		Point3d derivatehelp = NURBSRek(u,j+1,d-1); //Result in homogeneous Values
		derivatehelp.scale(d/(t.get(j+d+1)-t.get(j+1)));
		derivate.sub(derivatehelp);
		//That is the homogeneous first derivate.
		//By formular (4.8) and the Leibnitz-rule we obtain
		Point3d orig = NURBSRek(u,j,d); //Result in homogeneous Values
		double derivw = derivate.z;
		double origw = orig.z;
		orig.scale(derivw);
		derivate.sub(orig); //A^1(u) - w^1(u)C^0(u)
		if (origw==0) //
			return new Point2D.Double(derivate.x,derivate.y);
		else
			return new Point2D.Double(derivate.x/origw,derivate.y/origw);
	}
	/**
	 * Calulation of Alpha, refer to deBoer-Algorithm
	 * @param u
	 * @param i
	 * @param j
	 * @return
	 */
	private double alpha(double u,int i, int j)
	{
		if ((u==t.get(i)&&(t.get(i+d-j+1)==t.get(i))))
			return 0;
		return (u-t.get(i))/(t.get(i+d-j+1)-t.get(i));
	}
	/**
	 * Calculate the Value recursive
	 * This might be a little bit too much computational Overhead but 
	 * works for experimental stuff. An Optimizational TODO is to calculate each
	 * recursive Point and alpha only once.
	 * 
	 * This method works for 2d homogeneous or 3d Stuff.
	 *
	 * @param u Point u \in [0,1], which result we want
	 * @param i Number of the Basis function of specific
	 * @param j Degree j
	 * @return a 3d-Value of the Point in the Curve.
	 */
	private Point3d NURBSRek(double u, int i, int j)
	{
		if (j==0)
		{
			return homogeneousb.get(i); 
		}
		Point3d bimjm = NURBSRek(u,i-1,j-1); //b_i-1^j-1
		double alpha = alpha(u,i,j);
		Point3d bijm = NURBSRek(u,i,j-1);
		double x = (1-alpha)*bimjm.x + alpha*bijm.x;
		double y = (1-alpha)*bimjm.y + alpha*bijm.y;
		double z = (1-alpha)*bimjm.z + alpha*bijm.z;
		return new Point3d(x,y,z);
	}
	/**
	 * Compares this Curve to another (minDist does not matter)
	 * if alle values of t,b,w are equal it returns true, else false
	 * 
	 * TODO: Check whether there's a way to check for equality if these values are different
	 * @param s another Shape
	 * @return true if Shape s is equal to this else false
	 */
	public boolean CurveEquals(VHyperEdgeShape s)
	{
		if ((s.b.size()!=b.size())||(t.size()!=s.t.size()))
			return false;
		Iterator<Point2D> bi = b.iterator();
		while (bi.hasNext())
		{
			Point2D p = bi.next();
			if (s.b.get(b.indexOf(p)).distance(p)!=0.0d)
				return false;
		}
		Iterator<Double> ti = t.iterator();
		while (ti.hasNext())
		{
			Double v = ti.next();
			if (s.t.get(t.indexOf(v)).compareTo(v)==0) //Equal
				return false;
		}
		Iterator<Double> wi = w.iterator();
		while (wi.hasNext())
		{
			Double v = wi.next();
			if (s.w.get(w.indexOf(v)).compareTo(v)==0) //Equal
				return false;
		}
		return true;
	}

	public Point2D getNearestCP(Point m) {
		double mindist = Double.MAX_VALUE;
		Point2D result = null;
		Iterator<Point2D> bi = b.iterator();
		while (bi.hasNext())
		{
			Point2D p = bi.next();
			double dist = p.distance(m);
			if (dist < mindist)
			{
				mindist = dist;
				result = p;
			}
		}
		return result;
	}
	public void translate(int x, int y)
	{
		Iterator<Point2D> bi = b.iterator();
		while (bi.hasNext())
		{
			Point2D p = bi.next();
			p.setLocation(p.getX()+x,p.getY()+y);
		}
		//recalculate Homogeneous
		InitHomogeneous();
	}
	/**
	 * Scale all Controlpoints by factor s, of you want to resize a shape
	 * make shure to translate its middle to 0,0 before and back afterwards
	 * @param s
	 */
	public void scale(double s)
	{
		Iterator<Point2D> bi = b.iterator();
		while (bi.hasNext())
		{
			Point2D p = bi.next();
			p.setLocation(p.getX()*s,p.getY()*s);
		}
		//recalculate Homogeneous
		InitHomogeneous();
		
	}
	/**
	 * Get Maximum (bottom right edge) of the CP bunding box
	 */
	public Point2D getMax()
	{
		double x = Double.MIN_VALUE;
		double y = Double.MIN_VALUE;
		Iterator<Point2D> bi = b.iterator();
		while (bi.hasNext())
		{
			Point2D p = bi.next();
			if (p.getX() > x)
				x = p.getX();
			if (p.getY() > y)
				y = p.getY();
		}
		return new Point2D.Double(x,y);
	}
	/**
	 * Get Minimum (top left edge) of the CP bunding box
	 */
	public Point2D getMin()
	{
		double x = Double.MAX_VALUE;
		double y = Double.MAX_VALUE;
		Iterator<Point2D> bi = b.iterator();
		while (bi.hasNext())
		{
			Point2D p = bi.next();
			if (p.getX() < x)
				x = p.getX();
			if (p.getY() < y)
				y = p.getY();
		}
		return new Point2D.Double(x,y);
	}

	public boolean isPointOnCurve(Point2D x, double variance)
	{
		return false;
	}
}
