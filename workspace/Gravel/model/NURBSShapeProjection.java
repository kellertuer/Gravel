package model;

import java.awt.geom.Point2D;
import java.util.Vector;

import javax.vecmath.Point3d;

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
		{
			System.err.println("TODO: Split");
			return;
		}
		else
			initObjSquDist(c.clone(),p);
		double alpha = Math.min(p.distanceSq(c.controlPoints.firstElement()), p.distanceSq(c.controlPoints.lastElement()));
		System.err.println("Initial alpha: "+alpha+" Radius"+Math.sqrt(alpha));
		//Check whether all are outside compute min
		double min = Double.MAX_VALUE;
		for (int i=0; i<ControlPoints.size(); i++)
		{
			System.err.print("("+i+") "+ControlPoints.get(i)+"  with weight "+weights.get(i)+"\n");			
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
					break;
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
		if (!isInBezierForm(c))
			return;
		c.translate(-p.getX(),-p.getY());
		//Further donted as FV
		double FirstValue = c.Knots.firstElement(); //In a pure Bezier-Curve on [0,1] this i 0, 
		//Futher denoted as LV
		double LastValue = c.Knots.lastElement(); //this is 1
		
		int Degree = c.maxCPIndex; //Old Degree of the given Bezier Curve
		System.err.println("Degree: "+Degree+"   and of the product: "+(2*Degree)+" so the tP and tQ have "+(3*Degree+2)+" elements");
		
		Vector<Double> tP,tQ;
		ControlPoints = new Vector<Double>();
		weights = new Vector<Double>();
		for (int i=0; i<=(2*Degree); i++)
		{	//Compute new weights \hat w_i of the product with  i \in {0,...,2n}
			//i also determines the number of ones in the Intervall for tP and tQ we may choose			
			//Init tP and  tQ
			tP = new Vector<Double>();
			tQ = new Vector<Double>();
			//Init tP with maximum number of FV, that is the min (n+1+i, 2n+1), because there are never more than 2n+1 FV
			//Analog tQ as the max(n+1,i+1) because it starts with least possible zeros and ends with the max (2n+1)
			for (int j=0; j<(3*Degree+2); j++) //This is the length of P and Q
			{
				if (j<Math.min(Degree+1+i,2*Degree+1)) //All befor this max to FV all other to LV
					tP.add(FirstValue);
				else
					tP.add(LastValue);
				if (j<Math.max(Degree+1,i+1)) //All brfore this to FV all behind to LV
					tQ.add(FirstValue);
				else
					tQ.add(LastValue);
			}
			double dix = 0.0d, diy = 0.0d; //actualCoefficient
			double diw = 0.0d;
			//Now for each i there exist Degree+1 - |i-n| cases (Symmetric to n, because 0,...,2n is always odd)
			int numberOfCases = Degree+1 - Math.abs(i-Degree);
			for (int cases=0; cases < numberOfCases; cases++)
			{
				//This one case has combinatorial multiplicity:
				int realcases = cases; //Shift becase after i=Degree the first cases are missing
				if (i>Degree)
					realcases += (i-Degree); //After i=Degree first the case, that all p are FV is not possible anymore. this is for the binomial corrected here
				long mult = binomial(2*Degree-i, Degree-realcases)*binomial(i,realcases);
				//For each of the cases of t^P/t^Q compute the projection parameters alpha,
				//that project t^P onto the old t-Vector, and t^Q onto the old t
				double foronepx = 0.0d, foronepy = 0.0d; //actualCoefficient
				double foronepw = 0.0d;
				for (int j1=0; j1<c.controlPoints.size(); j1++)
				{
					for (int j2=0; j2<c.controlPoints.size(); j2++)
					{
						double alpha1 = alpha(j1,Degree+1,c.Knots,tP,i);
						double alpha2 = alpha(j2,Degree+1,c.Knots,tQ,i);
						System.err.println("("+i+") "+alpha1+"*"+alpha2+" "+c.controlPoints.get(j1)+" "+c.controlPoints.get(j2));
						foronepx += c.controlPoints.get(j1).getX()*alpha1*c.controlPoints.get(j2).getX()*alpha2;
						foronepy += c.controlPoints.get(j1).getY()*alpha1*c.controlPoints.get(j2).getY()*alpha2;
						foronepw += c.cpWeight.get(j1).doubleValue()*alpha1*c.cpWeight.get(j2).doubleValue()*alpha2;
						System.err.println(foronepx+"  "+foronepy+"  "+foronepw);
					}
				}
				dix += foronepx*mult;
				diy += foronepy*mult;
				diw += foronepw*mult;
				//Change the t^P and t^Q, so that t^P its first LV set to FV and t^Q its last FV set to LV
				int changeindex = 0;
				while (tP.get(changeindex).doubleValue()==FirstValue)
					changeindex++;
				tP.set(changeindex-1, LastValue);
				changeindex = 0;
				while (tQ.get(changeindex).doubleValue()==FirstValue)
					changeindex++;
				tQ.set(changeindex, FirstValue);
			} //End Cases
			dix /= binomial(2*Degree,Degree);
			diy /= binomial(2*Degree,Degree);
			diw /= binomial(2*Degree,Degree);
			ControlPoints.add(dix + diy);
			weights.add(diw);
		}	//End i
	}
	private double alpha(int j, int k, Vector<Double> tau, Vector<Double> t, int i)
	{
		if (k==1)
		{ //Formular after 1.2 with i=j, t=tau x=t.get(i)
			if ((tau.get(j)<=t.get(i))&&(t.get(i)<tau.get(j+1)))
				return 1;
			else
				return 0;
		}	
		else if ((i+k-1)>=t.size())
		{
			System.err.println("TODO: Check! "+j+" "+k+" "+i);
			return 0;
		}
		else
			return w(j,k,tau,t.get(i+k-1))*alpha(j,k-1,tau,t,i) + (1-w(j+1,k,tau,t.get(i+k-1)))*alpha(j+1,k-1,tau,t,i);
	}
	/**
	 * w from Formular (1.2) of K. Mørten
	 * TODO: Optimize for Bezier Curves
	 * @param i
	 * @param k
	 * @param t
	 * @param x
	 * @return
	 */
	private double w(int i, int k, Vector<Double> t, double x)
	{
		if (t.get(i)<t.get(i+k-1)) //i is the first value and k+i the last, they are different so not zero
		{
			System.err.println("w_"+i+","+k+" is "+(x-t.get(i))/(t.get(i+k-1)-t.get(i)));
			return (x-t.get(i))/(t.get(i+k-1)-t.get(i));			
		}
		else
		{
		//	System.err.println("w_"+i+","+k+" is zero");
			return 0.0d;
		}
	}

	/**
	 * Split a given NURBS-Curve into its rational Bezier-Segments
	 * @param c
	 * @return
	 */
	public Vector<VHyperEdgeShape> DecomposeCurve(VHyperEdgeShape c)
	{
		int m = c.maxCPIndex+1;
		int a = c.degree;
		int b = c.degree+1;
		Vector<VHyperEdgeShape> BezierSegments = new Vector<VHyperEdgeShape>();
		Vector<Point3d> bezierCP = new Vector<Point3d>();
		Vector<Point3d> nextbezierCP = new Vector<Point3d>();
		nextbezierCP.setSize(c.degree+1);
		for (int i=0; i<=c.degree; i++)
			bezierCP.add(c.controlPointsHom.get(i));
		while (b<m)
		{
			int i=b;
			while ((b < m)&&(c.Knots.get(b+1).doubleValue()==c.Knots.get(b).doubleValue()))
				b++;
			int multiplicity = b-i+1; //Multiplicity of actual Knot
			if (multiplicity < c.degree) //Refine it until it is p
			{
				double numer = c.Knots.get(b).doubleValue() - c.Knots.get(a).doubleValue(); //Numerator of the alphas 
				Vector<Double> alpha = new Vector<Double>();
				alpha.setSize(c.degree-multiplicity);
				for (int j=c.degree; j>multiplicity; j--)
					alpha.set(j-multiplicity-1, numer/(c.Knots.get(a+j).doubleValue()-c.Knots.get(a).doubleValue()));
				int insertionMultiplicity = c.degree-multiplicity;
				for (int j=1; j<=insertionMultiplicity; j++) //Insert Knot as often as needed
				{
					int save = insertionMultiplicity - j;
					int s = multiplicity+j; //These many new Points
					for (int k=c.degree; k>=s; k--) //
					{
						Point3d p1 = (Point3d) bezierCP.get(k).clone();
						Point3d p2 = (Point3d) bezierCP.get(k-1).clone();
						p1.scale(alpha.get(k-s)); p2.scale(1-alpha.get(k-s));						
						p1.add(p2);
						bezierCP.set(k,p1);
					}
					if (b<m) //then the last cp is also a CP of the next bezier segment
						nextbezierCP.set(save, bezierCP.get(c.degree));
				}
			} //End of refinement
			//Save actual
			Vector<Double> bezierKnots = new Vector<Double>();
			for (int k=0; k<=c.degree; k++)
				bezierKnots.add(c.Knots.get(a));
			for (int k=0; k<=c.degree; k++)
				bezierKnots.add(c.Knots.get(b));
			BezierSegments.add(new VHyperEdgeShape(bezierKnots, bezierCP, c.minDist));
			if (b<m) //init next
			{
				bezierCP = nextbezierCP;
				nextbezierCP = new Vector<Point3d>();
				nextbezierCP.setSize(c.degree+1);
				for (int k=(c.degree-multiplicity); k<=c.degree; k++)
					bezierCP.set(k, c.controlPointsHom.get(b-c.degree+k));
				a = b;
				b++;
			}
		}
		//save last
		Vector<Double> bezierKnots = new Vector<Double>();
		for (int k=0; k<=c.degree; k++)
			bezierKnots.add(c.Knots.get(a));
		for (int k=0; k<=c.degree; k++)
			bezierKnots.add(c.Knots.get(b));
		BezierSegments.add(new VHyperEdgeShape(bezierKnots, bezierCP, c.minDist));
		return BezierSegments;
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
