package model;

import java.awt.geom.Point2D;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Vector;

import view.VCommonGraphic;

/**
 * This Class is used for the projection and point inversion on NURBS Curves
 * Initialized with a NURBS Curve and a point the Class provides both the projection Point and
 * the projection parameter
 * 
 * This class is based on the Algorithm by Chen et al. unsing a circular clipping method
 *
 * It extends NURBSShape due to the resulting objective square distance function which is itself
 * a Bezier-Curve - so it follow the decorator-Pattern
 * 
 * @author Ronny Bergmann
 */
public class NURBSShapeProjection extends NURBSShape
{
	Point2D p;
	//Values of the just handled qadrated bezier curve
	Vector<Double> qcWeights, qcControlPoints;
	int qcDegree;
	double umin, umax;
	NURBSShape origCurve; //Reference to initial Curve we project on - Decorator-pattern
	double resultu;
	/**
	 * Start the NURBSShapeProjection with a given Curve the Point is projected onto
	 * 
	 * @param c Curve which must be stripped off deco before if you want to project on whole curve
	 * @param p Point that should be projected
	 */
	public NURBSShapeProjection(NURBSShape Curve, Point2D p)
	{
		this(Curve,p,null,0f);
	}
	/**
	 * Version with debug, Graphics2D and zoom
	 * @param Curve
	 * @param p
	 * @param debug
	 * @param z
	 */
	public NURBSShapeProjection(NURBSShape Curve, Point2D p,VCommonGraphic debug, float z)
	{
		origCurve = Curve;
		NURBSShape clone = Curve.clone(); //We clone with decorations - if anybody wants to project really only on curve - strip before init
		//If it is unclamped - clamp it!
		if ((clone.getType()&NURBSShape.UNCLAMPED)==NURBSShape.UNCLAMPED)
			clone = clamp(clone);
		//Set internal Curve to this curve clone
		setCurveTo(clone.Knots, clone.controlPoints, clone.cpWeight);
		resultu=Knots.firstElement(); //u=a
		this.p = p;
		Queue<NURBSShape> Parts = new LinkedList<NURBSShape>();
		if (!isInBezierForm())
		{
			Vector<NURBSShape> partsf = DecomposeCurve(this);
			for (int i=0; i<partsf.size(); i++)
			{
				Parts.offer(partsf.get(i));
			}
		}
		else
			Parts.offer(clone()); //We are nondecorative so clone is okay
		double alpha = Math.min(p.distanceSq(controlPoints.firstElement()), p.distanceSq(controlPoints.lastElement()));
		if (p.distanceSq(controlPoints.firstElement()) > p.distanceSq(controlPoints.lastElement()))
			resultu=Knots.lastElement();
		
		Vector<Double> candidates = new Vector<Double>(); //because we may have more than one candidate Span 
		while (!Parts.isEmpty()) 
			//Wander through all parts, if a part is splitted, its new parts are added at the end of
			//The Quere, so that they hopefully are out of range of the circle and get discarded then 
			//
		{
			NURBSShape actualPart = Parts.poll();	
			CalculateSquareDistanceFunction(actualPart.clone(),p); //they are nondecorative
			double partAlpha = Math.min(p.distanceSq(actualPart.controlPoints.firstElement()), p.distanceSq(actualPart.controlPoints.lastElement()));
			if (partAlpha<alpha)
				alpha = partAlpha;

			//Check whether all are outside compute min
			double min = Double.MAX_VALUE;
			for (int i=0; i<qcControlPoints.size(); i++)
			{
				if (qcControlPoints.get(i)<min)
					min = qcControlPoints.get(i);
			}
			if (min>alpha) //definetly outside -> do nothing
			{}
			else if (min==partAlpha) //might be projected onto one of the end-elements, add them to the candidates-> do nothing
			{
				candidates.add(actualPart.Knots.firstElement()); //Because its Bezier parts, first 
				candidates.add(actualPart.Knots.lastElement()); //and last Element of Knot vector do
			}
			else //may be inside polygon
			{
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
					//take a look at where the minimum in the qcCP was and take the startvalue inside actual [umin,umax]
					//(+1 and +2 ensures INSIDE umax-umin) and the first Method can't return -1 because we took min from the qcCP
					double startvalue = umin + (double)(qcControlPoints.indexOf(min)+1)/((double)qcControlPoints.size()+2)*(umax-umin);
					
					double candidate_u = NewtonIteration(actualPart.clone(), startvalue, p);
					candidates.add(candidate_u);
					double newdistsq = CurveAt(candidate_u).distanceSq(p);
					if (alpha > newdistsq)
						alpha = newdistsq;
				}
			} //End INside
		} //End while
		double min = Double.MAX_VALUE;
		for (int i=0; i<candidates.size(); i++)
		{
			Point2D pcmp = CurveAt(candidates.get(i));
			if (pcmp.distance(p)<min)
			{
				resultu = candidates.get(i);
				min = pcmp.distance(p);
			}
		}
	}
	
	/**
	 * Strip a NURBSShape off every Decorator
	 * This class strips itself from the NURBSShape it belongs to
	 * @return
	 */
	public NURBSShape stripDecorations()
	{
		return origCurve.stripDecorations();
	}
	/**
	 * Return all Decorations this class has
	 * That is all decorations the superclass has + validator
	 * @return
	 */
	public int getDecorationTypes()
	{
		return origCurve.getDecorationTypes()|NURBSShape.PROJECTION;
	}
	
	public double getResultParameter()
	{
		return resultu;
	}
	
	public Point2D getResultPoint()
	{
		return CurveAt(resultu);
	}
	
	private double NewtonIteration(NURBSShape c, double startvalue, Point2D p)
	{
		//End criteria
		double epsilon1 = 0.00002d;
		double epsilon2 = 0.0003d;
		//So now we 
		boolean running = true;
		Vector<Point2D> derivs = c.DerivateCurveValuesAt(2,startvalue);
		Point2D.Double Value = (Point2D.Double) derivs.get(0); //c.CurveAt(startvalue);
		Point2D.Double firstDeriv = (Point2D.Double) derivs.get(1); //c.DerivateCurveValuesAt(1,startvalue).get(1);
		Point2D.Double secondDeriv = (Point2D.Double) derivs.get(2); //c.DerivateCurveValuesAt(2,startvalue).get(2);
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
			derivs = c.DerivateCurveValuesAt(2,unext);
			Value = (Point2D.Double) derivs.get(0); //c.CurveAt(startvalue);
			firstDeriv = (Point2D.Double) derivs.get(1); //c.DerivateCurveValuesAt(1,startvalue).get(1);
			secondDeriv = (Point2D.Double) derivs.get(2); //c.DerivateCurveValuesAt(2,startvalue).get(2);
			diff = new Point2D.Double(Value.x-p.getX(), Value.y-p.getY());
			double coincidence = diff.distance(0d,0d);
			double movement = Math.abs(nominator/denominator);
			double movementu = Math.abs(unext-u)*Math.sqrt(firstDeriv.x*firstDeriv.x + firstDeriv.y*firstDeriv.y);
			if (iterations>100) //it sould converge fast so here we should change sth
			{
	        	main.DEBUG.println(main.DEBUG.MIDDLE,"NURBSShapeProjection::NewtonIterationInit() - Warning: Newton-Iteration took too long.");
				return (u+unext)/2; //Try to prevent circles
			}
			else
				u=unext;            
			if ((coincidence<=epsilon1)||(movement<=epsilon2)||(movementu<=epsilon1))
				running=false;
		  }
		  return u;
	}
	/**
	 * Returns true if and only if the NURBS Curve in this Class is in Bezier Form, that is, 
	 * there are only knots at the start and end of the Interval
	 * @return
	 */
	private boolean isInBezierForm()
	{
		double a = Knots.firstElement();		
		double b = Knots.lastElement();
		int counta=0, countb=0;
		for (int i=0; i<=maxKnotIndex; i++)
		{
			if (Knots.get(i).doubleValue()==a)
				counta++;
			else if (Knots.get(i).doubleValue()==b)
				countb++;
			else
				return false;
		}
		return ((degree==(counta-1))&&(degree==(countb-1)));
	}
	
	/**
	 * Calculate Square distance Function proposed by Chen et al.
	 * the Curve must be in Bezier-Form to work properly
	 * 
	 * @param c
	 * @param p
	 */
	private void CalculateSquareDistanceFunction(NURBSShape c, Point2D p)
	{
		c.translate(-p.getX(),-p.getY());
		//Further donted as FV
		double FirstValue = c.Knots.firstElement(); //In a pure Bezier-Curve on [0,1] this i 0, 
		//Futher denoted as LV
		double LastValue = c.Knots.lastElement(); //this is 1
		
		int Degree = c.maxCPIndex; //Old Degree of the given Bezier Curve
		
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
			long wholenum = binomial(2*Degree,Degree);
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
				double[] alphaP = alphaVec(Degree, c.Knots, tP,i);
				double[] alphaQ = alphaVec(Degree, c.Knots, tQ,i);
				for (int j1=0; j1<c.controlPoints.size(); j1++)
				{
					for (int j2=0; j2<c.controlPoints.size(); j2++)
					{
						foronepx += c.controlPoints.get(j1).getX()*alphaP[j1]*c.controlPoints.get(j2).getX()*alphaQ[j2];
						foronepy += c.controlPoints.get(j1).getY()*alphaP[j1]*c.controlPoints.get(j2).getY()*alphaQ[j2];
						foronepw += c.cpWeight.get(j1).doubleValue()*alphaP[j1]*c.cpWeight.get(j2).doubleValue()*alphaQ[j2];
					}
				}
				dix += foronepx*mult/wholenum;
				diy += foronepy*mult/wholenum;
				diw += foronepw*mult/wholenum;
				//Change the t^P and t^Q, so that t^P its first LV set to FV and t^Q its last FV set to LV
				int changeindex = 0;
				while ((changeindex<tP.size()-1)&&(tP.get(changeindex).doubleValue()==FirstValue))
					changeindex++;
				tP.set(changeindex-1, LastValue);
				changeindex = 0;
				//Search for a change
				while ((changeindex<tQ.size()-1)&&(tQ.get(changeindex).doubleValue()==FirstValue))
					changeindex++;
				tQ.set(changeindex, FirstValue);
			} //End Cases
			//Moved inside the summation for smaller results inside the loop
//			dix /= binomial(2*Degree,Degree);
//			diy /= binomial(2*Degree,Degree);
//			diw /= binomial(2*Degree,Degree);
			qcControlPoints.add(dix + diy);
			qcWeights.add(diw);
		}	//End i
		//Update Degree
		qcDegree = 2*Degree;
		umin = c.Knots.firstElement().doubleValue();
		umax = c.Knots.lastElement().doubleValue();
	}
	/**
	 * Computes the discrete B-Spline Coefficients of the projection-curve coefficient i from tau to t
	 * and a given index i
	 * @param k the Degree
	 * @param tau
	 * @param t
	 * @param i index of the Coefficient these discrete stuff is needed for
	**/
	private double[] alphaVec(int k, Vector<Double> tau, Vector<Double> t, int i)
	{
		double[] temp = new double[2*k+1]; //0...2k
		for (int j=0; j<=2*k; j++) //Init for degree 0
		{
			if ((tau.get(j)<=t.get(i))&&(t.get(i)<tau.get(j+1)))
				temp[j] = 1;
			else
				temp[j] = 0;	
		}
		for (int deg=1; deg<=k; deg++) //Compute up to degree k
		{
			for (int j=0; j<=2*k-deg; j++) //For each higher degree one coefficient in the end less
			{
				temp[j] = w(j,deg,tau,t.get(i+deg))*temp[j] + (1-w(j+1,deg,tau,t.get(i+deg)))*temp[j+1];
			}
		}
		double[] result = new double[k+1];
		for (int j=0; j<=k; j++)
			result[j] = temp[j];
		return result;
	}
	/**
	 * @param j element of the discrete alphas
	 * @param k the Degree
	 * @param tau
	 * @param t
	 * @param i index of the 
	 * @return alpha_j,i,tau,t(i)
	 * @deprecated use the nonrecursive alphaVec-Funktion
	 */
	private double alpha(int j, int k, Vector<Double> tau, Vector<Double> t, int i)
	{
		if (k==0)
		{ //Formular after 1.2 with i=j, t=tau x=t.get(i) with shiftet k by 1
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
	public static Vector<NURBSShape> DecomposeCurve(NURBSShape c)
	{			
		int m = c.maxCPIndex+1;
		int a = c.degree;
		int b = c.degree+1;
		Vector<NURBSShape> BezierSegments = new Vector<NURBSShape>();
		Vector<Point2dHom> bezierCP = new Vector<Point2dHom>();
		Vector<Point2dHom> nextbezierCP = new Vector<Point2dHom>();
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
						Point2dHom p1 = (Point2dHom) bezierCP.get(k).clone();
						Point2dHom p2 = (Point2dHom) bezierCP.get(k-1).clone();
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
				nextbezierCP = new Vector<Point2dHom>();
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
	
	public static NURBSShape clamp(NURBSShape c)
	{
		double u1 = c.Knots.get(c.degree);
		double u2 = c.Knots.get(c.maxKnotIndex-c.degree);
		int multStart=0, multEnd=0;
		//Raise both endvalues to multiplicity d to get an clamped curve
		for (int i=0; i<=c.maxKnotIndex; i++)
		{
		 if (c.Knots.get(i).doubleValue()==u1)
			multStart++;
		 if (c.Knots.get(i).doubleValue()==u2)
			multEnd++;
		}
		Vector<Double> Refinement = new Vector<Double>();
		NURBSShape subcurve = c.clone();
		for (int i=0; i<c.degree-multStart; i++)
			Refinement.add(u1);
		for (int i=0; i<c.degree-multEnd; i++)
			Refinement.add(u2);
		subcurve.RefineKnots(Refinement);
		//Do not care about circular stuff, it's clamped after that anyway
		Vector<Point2D> newCP = new Vector<Point2D>();
		Vector<Double> newWeight= new Vector<Double>();
		int subStart = subcurve.findSpan(u1);
		int subEnd = subcurve.findSpan(u2)-subcurve.degree+multEnd;
		for (int i=subStart-c.degree; i<=subEnd; i++) //Copy needed CP
		{
			newCP.add((Point2D)subcurve.controlPoints.get(i).clone());
			newWeight.add(subcurve.cpWeight.get(i).doubleValue());
		}
		//Copy needed Knots
		Vector<Double> newKnots = new Vector<Double>();
		newKnots.add(u1);
		int index = 0;
		while (subcurve.Knots.get(index)<u1)
			index++;
		while (subcurve.Knots.get(index)<=u2)
		{
			newKnots.add(subcurve.Knots.get(index).doubleValue());
			index++;
		}
		newKnots.add(u2);
		NURBSShape retval = new NURBSShape(newKnots,newCP,newWeight);
		return retval;
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
