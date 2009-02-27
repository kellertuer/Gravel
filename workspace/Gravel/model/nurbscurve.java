package model;

import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.Vector;

import javax.vecmath.Point3d;

public class nurbscurve {

	private Vector<Double> t; //knots
	private Vector<Double> w; //weights
	private Vector<Point2D> b; //ControlPoints
	
	private int n,d,m; //n is the number of CP, d the Order of the polynom-splines, m=n+d+1
	public nurbscurve(Vector<Double> knots, Vector<Point2D> cpoints, Vector<Double> weights, int degree)
	{
		t = knots;
		b = cpoints;
		w = weights;
		d = degree;
		n = cpoints.size()-1;
		m = n+d+1;		
	}
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
			System.err.println(actual+" - ("+f.x+","+f.y+")");
			actual+=stepsize;
		}
		path.closePath();
		
		return path;
	}
	
	private Point2D.Double NURBSCurveAt(double u)
	{
		Vector<Point3d> b3 = new Vector<Point3d>();
		Iterator<Point2D> ib =  b.iterator();
		while (ib.hasNext()) //Modify to be 3D Coordinates (homogeneous 2D)
		{
			Point2D p = ib.next();
			double weight = w.get(b.indexOf(p));
			Point3d newp = new Point3d(p.getX(),p.getY(),weight);
			newp.set(newp.x*weight, newp.y*weight, weight);
			b3.add(newp);
		}
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
		} //get the first t AFTER u
//		System.err.println("NurbsCurve at "+u+" with r="+(j-1));
		Point3d erg = NURBSRek(b3,u,j-1,d);
		if (erg.z==0)
			return new Point2D.Double(erg.x,erg.y);
		else
			return new Point2D.Double(erg.x/erg.z,erg.y/erg.z);
	}
	private double alpha(double u,int i, int j)
	{
		return (u-t.get(i))/(t.get(i+d-j+1)-t.get(i));
	}
	private Point3d NURBSRek(Vector<Point3d> b3, double u, int i, int j)
	{
		if (j==0)
		{
			return b3.get(i); 
		}
		Point3d bimjm = NURBSRek(b3,u,i-1,j-1); //b_i-1^j-1
		double alpha = alpha(u,i,j);
		Point3d bijm = NURBSRek(b3,u,i,j-1);
		double x = (1-alpha)*bimjm.x + alpha*bijm.x;
		double y = (1-alpha)*bimjm.y + alpha*bijm.y;
		double z = (1-alpha)*bimjm.z + alpha*bijm.z;
		return new Point3d(x,y,z);
	}

}
