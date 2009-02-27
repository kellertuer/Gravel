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
 * 
 * TODO More save methods for the NURBS
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
	 * Private Method to evaluate the Curve at given point u \in [t_0,t_m]
	 * @param u
	 * @return
	 */
	private Point2D.Double NURBSCurveAt(double u)
	{
		//r is now the interval of the vektors t, where u resides
		int i=1,j=0;
		while ((i<t.size()))
		{ 
			if (((u >=t.get(i-1))&&((u < t.get(i)))) //u in interval between t_i-1 and t_i
					|| ((u==t.get(i))&&(t.get(i-1)<t.get(i))&&(t.get(i)==t.lastElement()))) //u equals the first element that is equal to the last 
				{
					j=i;
					break;
				}
			i++;
		} //get the first t AFTER u in Variable j
		
		Point3d erg = NURBSRek(u,j-1,d); //Result in homogeneous Values
		if (erg.z==0) //
			return new Point2D.Double(erg.x,erg.y);
		else
			return new Point2D.Double(erg.x/erg.z,erg.y/erg.z);
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
	 * 
	 * @param b3 Point Vector of 
	 * @param u
	 * @param i
	 * @param j
	 * @return
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
	}

}
