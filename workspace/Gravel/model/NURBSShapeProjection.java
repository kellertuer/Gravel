package model;

import java.awt.geom.Point2D;
import java.util.Vector;

/**
 * This Class is used for the projection and point inversion on NURBS Curves
 * Initialized with a NURBS Curve and a point the Class provides both the projection Point and
 * the projection parameter
 * 
 * This class is based on the Algorithm by Chen et al. unsing a circular clipping method
 *
 * It extends VHyperEdgeShape due to the resulting objective square distance function which is itself
 * a Bezier-Curve
 * @author Ronny Bergmann
 * 
 *
 */
public class NURBSShapeProjection
{
	VHyperEdgeShape curve;
	VHyperEdgeShape[] bezierparts;
	Point2D p;
	Vector<Double> weights, ControlPoints;
	public NURBSShapeProjection(VHyperEdgeShape c, Point2D p)
	{
		if (!isInBezierForm(c))
			System.err.println("TODO: Split");
		else
		initObjSquDist(c.clone(),p);
		double alpha = Math.min(p.distanceSq(c.controlPoints.firstElement()), p.distanceSq(c.controlPoints.lastElement()));
		System.err.println("Initial alpha: "+alpha+" Radius"+Math.sqrt(alpha));
		//Check whether all are outside compute min
		double min = Double.MAX_VALUE;
		for (int i=0; i<ControlPoints.size(); i++)
		{
			System.err.print(ControlPoints.get(i)+"  ");
			if (ControlPoints.get(i)<min)
				min = ControlPoints.get(i);
		}
		System.err.println("   \n");
		if (min>alpha)
		{
			System.err.println("Exlclude this part");
		}
		else if (min==alpha)
		{
			System.err.println("Minimum should be one of the endpoints.");
		}
		else
		{
			
			System.err.println("Inside, checking for Prop2");
			int k=0; //None found
			boolean prop2=true;
			double lastP = ControlPoints.firstElement();
			while (((k+2)<ControlPoints.size())&&(lastP > ControlPoints.get(k+1)))
				k++;
			System.err.println("First Min "+k+" if there is no max before end, prop 2 given");
				//First Minimum found in ControlPoints, so all the values after k must be increasing
			for (int i=k; i<ControlPoints.size()-1; i++)
			{
				if (ControlPoints.get(i)>=ControlPoints.get(i+1)) //nonincreasing
				{
					System.err.println("Prop2 not given");
					prop2=false;
				}
			}
			if (prop2)
				System.err.println("Griven");
		}
	}
	/**
	 * Returns true if and only if the given NURBS-Curve is a rational Bezier curve, that is, 
	 * there are only knots at the start and end of the Interval
	 * @param c
	 * @return
	 */
	private boolean isInBezierForm(VHyperEdgeShape c)
	{
		int deg = c.degree;
		double a = c.Knots.firstElement();		
		double b = c.Knots.lastElement();
		int counta=0, countb=0;
		for (int i=0; i<=c.maxKnotIndex; i++)
		{
			if (c.Knots.get(i).doubleValue()==a)
				counta++;
			else if (c.Knots.get(i).doubleValue()==b)
				countb++;
			else
				return false;
		}
		return ((deg==(counta-1))&&(deg==(countb-1)));
	}
	/**
	 * 
	 */
	private void initObjSquDist(VHyperEdgeShape c, Point2D p)
	{
		c.translate(-p.getX(),-p.getY());
		int n = c.maxCPIndex;
		//Compute new weights
		weights = new Vector<Double>();
		for (int k=0; k<=(2*n); k++) //For each k compute
		{
			double thisw = 0.0d;
			for (int i=0; (i<=k)&&(i<=n); i++)
			{
				int j = k-i; //So that the sum i+j=k - that way we get every sum of i+j that is k
				if (j<=n)
					thisw += alpha(i,j,n)*c.cpWeight.get(i)*c.cpWeight.get(j);
			}
			weights.add(thisw);
		}
		//With the new weights we also can compute the new CP
		ControlPoints = new Vector<Double>();
		for (int k=0; k<=(2*n); k++) //For each k compute
		{
			double thisp = 0.0d;
			for (int i=0; (i<=k)&&(i<=n); i++)
			{
//				System.err.print("\n k="+k+" ");
				int j = k-i; //So that the sum i+j=k - that way we get every sum of i+j that is k
				if (j<=n)
				{ //Compute Pi^T*Pj^
					double scp = c.controlPoints.get(i).getX()*c.controlPoints.get(j).getX() + c.controlPoints.get(i).getY()*c.controlPoints.get(j).getY();
//					System.err.print("i="+i+" j="+j+"  alpha_ij="+alpha(i,j,n)+" scp="+scp);
//					System.err.print(" Adding "+(alpha(i,j,n)*c.cpWeight.get(i)*c.cpWeight.get(j)*scp)+" ... ");
					thisp += alpha(i,j,n)*c.cpWeight.get(i)*c.cpWeight.get(j)*scp;
				}
			}
			ControlPoints.add(thisp/weights.get(k));
		}
	}
	private double alpha(int i, int j, int n)
	{
		long a = binomial(n,i);
		long b = binomial(n,j);
		long c = binomial(2*n,i+j);
		double alpha = (double)(a*b)/(double)c;
		return alpha;
	}
	// return integer nearest to x
	long nint(double x)
	{
		if (x < 0.0) return (long) Math.ceil(x - 0.5);
	      return (long) Math.floor(x + 0.5);
	}
	// return log n!
	double logFactorial(int n) {
		double ans = 0.0;
		for (int i = 1; i <= n; i++)
			ans += Math.log(i);
		return ans;
	}
	// return the binomial coefficient n choose k.
	long binomial(int n, int k)
	{
		return nint(Math.exp(logFactorial(n) - logFactorial(k) - logFactorial(n-k)));
	}
}
