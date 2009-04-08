package model;

import java.awt.geom.Point2D;
import java.util.LinkedList;
import java.util.Queue;
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
	NURBSShape curve;
	Point2D p;
	//Values of the just handled qadrated bezier curve
	Vector<Double> qcWeights, qcControlPoints;
	int qcDegree;
	double umin, umax;
	
	double resultu;
	
	public NURBSShapeProjection(NURBSShape c, Point2D p)
	{
		curve = c.clone();
		this.p = p;
		Queue<NURBSShape> Parts = new LinkedList<NURBSShape>();
		if (!isInBezierForm(c))
		{
			Vector<NURBSShape> partsf = DecomposeCurve(c);
			for (int i=0; i<partsf.size(); i++)
				Parts.offer(partsf.get(i));
		}
		else
			Parts.offer(c.clone());
		double alpha = Math.min(p.distanceSq(c.controlPoints.firstElement()), p.distanceSq(c.controlPoints.lastElement()));
		if (p.distanceSq(c.controlPoints.firstElement()) > p.distanceSq(c.controlPoints.lastElement()))
			resultu=1.0d;
		
		Vector<Double> candidates = new Vector<Double>(); //because we may have more than one candidate Span 
		while (!Parts.isEmpty()) 
			//Wander through all parts, if a part is splitted, its new parts are added at the end of
			//The Quere, so that they hopefully are out of range of the circle and get discarded then 
			//
		{
			NURBSShape actualPart = Parts.poll();	
			CalculateSquareDistanceFunction(actualPart.clone(),p);
			double partAlpha = Math.min(p.distanceSq(actualPart.controlPoints.firstElement()), p.distanceSq(actualPart.controlPoints.lastElement()));
			if (partAlpha<alpha)
				alpha = partAlpha;
//			System.err.print(alpha+" on ["+actualPart.Knots.firstElement()+","+actualPart.Knots.lastElement()+"].");

			//Check whether all are outside compute min
			double min = Double.MAX_VALUE;
			for (int i=0; i<qcControlPoints.size(); i++)
			{
				if (qcControlPoints.get(i)<min)
					min = qcControlPoints.get(i);
			}
			if (min>alpha)
			{	//System.err.println(min+" out)");// - outside");
			}
			else if (min==partAlpha)
			{
				//System.err.println("Endknot candidate");
			}
			else
			{
		//		System.err.print(" - inside");
				int k=0; //None found
				boolean prop2=true;
				double lastP = qcControlPoints.firstElement();
					while (((k+2)<qcControlPoints.size())&&(lastP > qcControlPoints.get(k+1)))
					{
						lastP = qcControlPoints.get(k+1);
						k++;
					}
				for (int i=k; i<qcControlPoints.size()-1; i++)
				{
					if (qcControlPoints.get(i)>=qcControlPoints.get(i+1)) //nonincreasing
					{
						prop2=false;
						break;
					}
				}
				if (!prop2) //Split in the middle
				{
					double refinement = (actualPart.Knots.firstElement() + actualPart.Knots.lastElement())/2d;
//					if ((actualPart.Knots.lastElement()-actualPart.Knots.firstElement())<0.002d)
//						System.err.println((actualPart.Knots.firstElement()-actualPart.Knots.lastElement())+" addin "+refinement+" on ["+actualPart.Knots.firstElement()+" "+actualPart.Knots.lastElement()+"]");
					Vector<Double> ref = new Vector<Double>();
					ref.add(refinement);
					actualPart.RefineKnots(ref);
					Vector<NURBSShape> newParts = DecomposeCurve(actualPart);
					for (int i=0; i<newParts.size(); i++)
						Parts.offer(newParts.get(i));
				}
				else //Newton Iteration on this part
				{
					umin = actualPart.Knots.firstElement();
					umax = actualPart.Knots.lastElement();
					double candidate_u = NewtonIteration(actualPart.clone(), (umin+umax)/2d, p);
					candidates.add(candidate_u);
//					System.err.println("On ["+umin+","+umax+"] the Candidate u="+candidate_u);
					double newdistsq = curve.NURBSCurveAt(candidate_u).distanceSq(p);
					if (alpha > newdistsq)
						alpha = newdistsq;
				}
			} //End INside
		} //End while
		double min = Double.MAX_VALUE;
		for (int i=0; i<candidates.size(); i++)
		{
			Point2D pcmp = curve.NURBSCurveAt(candidates.get(i));
			if (pcmp.distance(p)<min)
			{
				resultu = candidates.get(i);
				min = pcmp.distance(p);
			}
		}
	}
	public double getResultParameter()
	{
		return resultu;
	}
	public Point2D getResultPoint()
	{
		return curve.NURBSCurveAt(resultu);
	}
	private double NewtonIteration(NURBSShape c, double startvalue, Point2D p)
	{
		//System.err.println("Start: "+startvalue);
		//End criteria
		double epsilon1 = 0.00002d;
		double epsilon2 = 0.0003d;
		//So now we 
		boolean running = true;
		Point2D.Double Value = (Point2D.Double) c.NURBSCurveAt(startvalue);
		Point2D.Double firstDeriv = (Point2D.Double) c.DerivateCurveAt(1,startvalue);
		Point2D.Double secondDeriv = (Point2D.Double) c.DerivateCurveAt(2,startvalue);
		Point2D.Double diff = new Point2D.Double(Value.x-p.getX(), Value.y-p.getY());
		double u=startvalue;
		int iterations=0;
		while (running)
		{
			iterations++;
			double nominator = firstDeriv.x*diff.x + firstDeriv.y*diff.y;
			double denominator = secondDeriv.x*diff.x + secondDeriv.y*diff.y + firstDeriv.distanceSq(0d,0d);
			double unext = u - nominator/denominator;
			if (unext > c.Knots.lastElement()) //Out of Range
				unext = c.Knots.lastElement();
			  if (unext < c.Knots.firstElement()) //Out of Range
				  unext = c.Knots.firstElement();
			  //System.err.print(" u="+unext);
			  Value = (Point2D.Double) c.NURBSCurveAt(unext);
			  firstDeriv = (Point2D.Double) c.DerivateCurveAt(1,unext);
			  secondDeriv = (Point2D.Double) c.DerivateCurveAt(2,unext);
			  diff = new Point2D.Double(Value.x-p.getX(), Value.y-p.getY());
			  double coincidence = diff.distance(0d,0d);
			  double movement = Math.abs(nominator/denominator);
			  double movementu = Math.abs(unext-u)*Math.sqrt(firstDeriv.x*firstDeriv.x + firstDeriv.y*firstDeriv.y);
			  if (iterations>50) //it sould converge fast so here we should change sth
			  {
				  u = (u+unext)/2;
				  System.err.println("#"+iterations+"Criteria: "+coincidence+" and "+movement+" and "+movementu);
				  iterations=0;
			  }
			  else
				   u=unext;            
			  if ((coincidence<=epsilon1)||(movement<=epsilon2)||(movementu<=epsilon1))
				  running=false;
		  }
		  if (iterations>15)
			  System.err.print(" #"+iterations);
		  return u;
	}
	/**
	 * Returns true if and only if the given NURBS-Curve is a rational Bezier curve, that is, 
	 * there are only knots at the start and end of the Interval
	 * @param c
	 * @return
	 */
	private boolean isInBezierForm(NURBSShape c)
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
	private void CalculateSquareDistanceFunction(NURBSShape c, Point2D p)
	{
		if (!isInBezierForm(c))
			return;
		c.translate(-p.getX(),-p.getY());
		//Further donted as FV
		double FirstValue = c.Knots.firstElement(); //In a pure Bezier-Curve on [0,1] this i 0, 
		//Futher denoted as LV
		double LastValue = c.Knots.lastElement(); //this is 1
		
		int Degree = c.maxCPIndex; //Old Degree of the given Bezier Curve
//		System.err.println("Degree: "+Degree+"   and of the product: "+(2*Degree)+" so the tP and tQ have "+(3*Degree+2)+" elements");
		
		Vector<Double> tP,tQ;
		qcControlPoints = new Vector<Double>();
		qcWeights = new Vector<Double>();
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
						double alpha1 = alpha(j1,Degree,c.Knots,tP,i);
						double alpha2 = alpha(j2,Degree,c.Knots,tQ,i);
//						System.err.println("("+i+") "+alpha1+"*"+alpha2+" "+c.controlPoints.get(j1)+" "+c.controlPoints.get(j2));
						foronepx += c.controlPoints.get(j1).getX()*alpha1*c.controlPoints.get(j2).getX()*alpha2;
						foronepy += c.controlPoints.get(j1).getY()*alpha1*c.controlPoints.get(j2).getY()*alpha2;
						foronepw += c.cpWeight.get(j1).doubleValue()*alpha1*c.cpWeight.get(j2).doubleValue()*alpha2;
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
			qcControlPoints.add(dix + diy);
			qcWeights.add(diw);
		}	//End i
		//Update Degree
		qcDegree = 2*Degree;
		umin = c.Knots.firstElement().doubleValue();
		umax = c.Knots.lastElement().doubleValue();
	}
	/**
	 * 
	 * @param j
	 * @param k the Degree
	 * @param tau
	 * @param t
	 * @param i index of the 
	 * @return
	 */
	private double alpha(int j, int k, Vector<Double> tau, Vector<Double> t, int i)
	{
		if (k==0)
		{ //Formular after 1.2 with i=j, t=tau x=t.get(i)
			if ((tau.get(j)<=t.get(i))&&(t.get(i)<tau.get(j+1)))
				return 1;
			else
				return 0;
		}	
		else
			return w(j,k,tau,t.get(i+k))*alpha(j,k-1,tau,t,i) + (1-w(j+1,k,tau,t.get(i+k)))*alpha(j+1,k-1,tau,t,i);
	}
	/**
	 * w from Formular (1.2) of K. Mørten
	 * TODO: Optimize for Bezier Curves
	 * @param i
	 * @param k the Degree
	 * @param t
	 * @param x
	 * @return
	 */
	private double w(int i, int k, Vector<Double> t, double x)
	{
		if (t.get(i)<t.get(i+k)) //i is the first value and k+i the last, they are different so not zero
		{
			return (x-t.get(i))/(t.get(i+k)-t.get(i));			
		}
		else
		{
			return 0.0d;
		}
	}

	/**
	 * Split a given NURBS-Curve into its rational Bezier-Segments
	 * @param c
	 * @return
	 */
	public Vector<NURBSShape> DecomposeCurve(NURBSShape c)
	{
		int m = c.maxCPIndex+1;
		int a = c.degree;
		int b = c.degree+1;
		Vector<NURBSShape> BezierSegments = new Vector<NURBSShape>();
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
			BezierSegments.add(new NURBSShape(bezierKnots, bezierCP));
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
		BezierSegments.add(new NURBSShape(bezierKnots, bezierCP));
		return BezierSegments;
	}
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
