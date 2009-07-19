package model;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.Vector;

import javax.vecmath.Point3d;

import model.Messages.NURBSCreationMessage;

/**
 * The NURBSShapeFactory creates specific NURBS-Curves based on the input
 * 
 * These NURBS-Curves are always unclamped and closed (and by that degree-1 times continuous also
 * at the start- and endpoint)
 * 
 * @author Ronny Bergmann
 * @since 0.4
 *
 */
public class NURBSShapeFactory {
	
	/**
	 * Create the Shape based on Specification of Type and needed Parameters
	 * 
	 * All Values given in Capital-Letters are predefined values at which the Parameter must be placed inside the Parameter-Vector
	 * and for each Parameter the Type is given in brackets
	 * 
	 * For the Types
	 * - „Circle“ with Parameters CIRCLE_RAdius (int), CircleOrigin (Point)
	 * - „Global Interpolation“ with given POINTS (Vector<Point2D>) and a DEGREE (int)
	 * - „Convex Hull“ the Nodepositions given through POINTS(Vector<Point2D>) their sizes given by SIZES (Vector<Integer>) and the DEGREE (int)
	 *
	 * @param type
	 * @param Parameters
	 * @return
	 */
	public static NURBSShape CreateShape(NURBSCreationMessage nm)
	{
		switch(nm.getType())
		{
			case NURBSCreationMessage.INTERPOLATION:
				if (nm.getDegree()>0) //Normal
					return CreateInterpolation(nm.getPoints(), nm.getDegree());
				else if ((nm.getCurve().getDecorationTypes()&NURBSShape.FRAGMENT)==NURBSShape.FRAGMENT)
					return CreateSubcurveInterpolation((NURBSShapeFragment)nm.getCurve(),nm.getPoints());
				else
					return new NURBSShape();
			case NURBSCreationMessage.CIRCLE:
				return CreateCircle(nm.getPoints().firstElement(), nm.getValues().firstElement());
			case NURBSCreationMessage.CONVEX_HULL:
				return CreateConvexHullPolygon(nm.getPoints(), nm.getValues(), nm.getDegree(), nm.getMargin());
			default:
				return new NURBSShape(); //Empty Shape
		}
	}
	
	/**
	 * Check whether Selection is Big enough
	 * @return
	 */
	public static boolean SubcurveSubstitutable(NURBSShapeFragment f)
	{
		NURBSShape origCurve = f.stripDecorations().clone();
		// refine Endpoints if not yet done
		double u1 = f.getStart(), u2 = f.getEnd();
		refineCircular(origCurve,f.getStart()); refineCircular(origCurve,u2);

		//Claculate part that is kept
		NURBSShapeFragment keptFragment = new NURBSShapeFragment(origCurve.clone(),u2,u1); //From this part we need the IP, so calulate that too for their determination
		keptFragment.prepareFragment(); //So we have enough CP in the kept Part

		int k1 = keptFragment.findSpan(u1);
		int k2 = keptFragment.findSpan(u2);
		//Check iff there is too less stuff to cut away
		if (u1<u2) //too less stuff to cut away
		{
			if ((k2-k1)<origCurve.degree+1)
			{
//				System.err.println("Wah 1");
				return false;
			}
		}
		else
		{
			if (Math.abs(k1-keptFragment.maxKnotIndex+2*keptFragment.degree-k2)<origCurve.degree+1)
			{
//				System.err.println("Wah 2");
				return false;
			}
		}
		return true;
	}

	/**
	 * Create a NURBSSHape Circle based on the Information, where its Origin is, its Radius and the Distance to the nodes
	 * 
	 * TODO: Create an Higher Order Circle
	 * 
	 * @param origin Point of Circle Origin
	 * @param radius Radius of the Circle
	 * @param dist Distance to the nodes
	 * @return
	 */
	private static NURBSShape CreateCircle(Point2D origin, int radius)
	{
		Point p = new Point(Math.round((float)origin.getX()),Math.round((float)origin.getY()));
		//See C. Bangert and H. Prautzsch for details
		int m=3; //
		double alpha = 90d/(double)m;
		Vector<Double> knots = new Vector<Double>();
//		knots.add(0d); knots.add(0d);
		knots.add(0d);
		knots.add(1d); knots.add(1d); knots.add(1d);
		knots.add(2d); knots.add(2d); knots.add(2d);
		knots.add(3d); knots.add(3d); knots.add(3d);
		knots.add(4d); knots.add(4d); knots.add(4d);
		knots.add(5d);
		//4 weitere zum Schließen
		knots.add(5d); knots.add(5d);
		knots.add(6d); knots.add(6d);
		Vector<Point2D> controlPoints = new Vector<Point2D>();
		Vector<Double> weights = new Vector<Double>();
		for (int l=1; l<=m; l++)
		{
			double beta = 2*(alpha)-90;
			double cosa = Math.cos(alpha/180d*Math.PI);
			double cosaq = cosa*cosa;
			double c = 1d/cosa;
			double d = (cosaq+2d)/(3d*cosaq);
			double omega = (2*cosaq*cosaq-cosaq+2d)/(3d*cosaq);
			//3l-1
			double rad = (beta+ ((double)(4*l-1))*alpha)/180d*Math.PI;
			controlPoints.add(new Point2D.Double(c*Math.cos(rad), c*Math.sin(rad) ));
			weights.add(1d);
			//3l
			rad = (beta+ ((double)4*l)*alpha)/180d*Math.PI;
			controlPoints.add(new Point2D.Double(d*Math.cos(rad), d*Math.sin(rad) ));
			weights.add(omega);
			//3l+1
			rad = (beta+ ((double)(4*l+1))*alpha)/180d*Math.PI;
			controlPoints.add(new Point2D.Double(c*Math.cos(rad), c*Math.sin(rad) ));
			weights.add(1d);
		}
		
		for (int i=0; i<4; i++)
		{
			controlPoints.add((Point2D)controlPoints.get(i).clone());
			weights.add(weights.get(i).doubleValue());			
		}
		NURBSShape c = new NURBSShape(knots,controlPoints,weights);
		c.scale((double)radius);
		c.translate(p.getX(),p.getY());
		System.err.println(c.degree+" with "+(controlPoints.size())+" CP and "+knots.size()+" Knots");
//		c.updateCircular(true);
		return c;
	}
	/**
  	  * Create and return a Smooth NURBS-Curve of degree degree through the given Interpolation Points,
  	  * that is degree-1 continuous at every Point
  	  * 
	  * @param IP The Interpolation-Points
	  * @param degree the degree
	  * @return
	 */
	private static NURBSShape CreateInterpolation(Vector<Point2D> q, int degree)
	{
		//Based on Algorithm 9.1 from the NURBS-Book
		//close IP to a closed curve
		Vector<Point2D> IP = new Vector<Point2D>();
		int IPCount = q.size();
		if (IPCount < 2*degree) //we have less then 2*degree IP -> no interpolatin possible due to overlappings needed
			return new NURBSShape();			
		for (int i=q.size()-degree; i<q.size(); i++)
			IP.add((Point2D) q.get(i).clone());
		for (int i=0; i<q.size(); i++)
			IP.add((Point2D) q.get(i).clone());
		for (int i=0; i<=degree; i++)
			IP.add((Point2D) q.get(i).clone());
		int maxIPIndex = IP.size()-1; //highest IP Index
		int maxKnotIndex = maxIPIndex+degree+1;//highest KnotIndex in the resulting NURBS-Curve
		//Determine Points to evaluate for IP with cetripetal Aproach
		double d = 0d;
		for (int i=1; i<=maxIPIndex; i++)
			d += Math.sqrt(IP.get(i).distance(IP.get(i-1)));
		Vector<Double> lgspoints = new Vector<Double>();
		lgspoints.setSize(IP.size());
		lgspoints.set(0,0d);
		lgspoints.set(maxIPIndex, 1d);
		for (int i=1; i<maxIPIndex; i++)
			lgspoints.set(i, lgspoints.get(i-1).doubleValue() + Math.sqrt(IP.get(i).distance(IP.get(i-1)))/d);
		//At the lgspoints we evaluate the Curve, get an LGS, that is totally positive and banded
		Vector<Double> Knots = calculateKnotVector(degree, maxKnotIndex, lgspoints);
		NURBSShape c = solveLGS(Knots, lgspoints, IP);
		c = unclamp(cutoverlaps(c,lgspoints.get(degree), lgspoints.get(degree+IPCount)));
		for (int i=0; i<degree; i++)
		{
			Point2D a = c.controlPoints.get(i);
			double aw = c.cpWeight.get(i);
			Point2D b = c.controlPoints.get(c.maxCPIndex-degree+1+i);
			double bw = c.cpWeight.get(c.maxCPIndex-degree+1+i);
			
			Point2D middle = new Point2D.Double((a.getX()+b.getX())/2d,	(a.getY()+b.getY())/2d);
			c.cpWeight.set(i, (aw+bw)/2d); //Update Circular to middle value
			c.cpWeight.set(c.maxCPIndex-degree+1+i, (aw+bw)/2d);
			c.controlPoints.set(i,(Point2D)middle.clone());
			c.controlPoints.set(c.maxCPIndex-degree+1+i,middle);
		}
		c.refreshInternalValues();
		return c;
	}

	/**
	 * Create an replacement for the subcurve specified by the Interpolationpoints.
	 * The parts before and following the subcurve-part are taken to get a smooth transition from the
	 * nonselected part (which stays the same) to the replacement.
	 * 
	 * @param fragment
	 * @param q
	 * @return
	 */
	private static NURBSShape CreateSubcurveInterpolation(NURBSShapeFragment fragment, Vector<Point2D> q)
	{
		if ((fragment.isEmpty())||(q.size()==0))
			return new NURBSShape();
		else if (fragment.getSubCurve().isEmpty())
			return fragment.stripDecorations().clone();
		if (!SubcurveSubstitutable(fragment))
			return new NURBSShape();
		//So we have a real existing subcurve
		double u1 = fragment.getStart(), u2 = fragment.getEnd();
		NURBSShape origCurve = fragment.stripDecorations().clone();
		// refine Endpoints if not yet done
		refineCircular(origCurve,u1); refineCircular(origCurve,u2);

		//Claculate part that is kept
		NURBSShapeFragment keptFragment = new NURBSShapeFragment(origCurve.clone(),u2,u1); //From this part we need the IP, so calulate that too for their determination
		keptFragment.prepareFragment(); //So we have enough CP in the kept Part

		int k1 = keptFragment.findSpan(u1);
		int k2 = keptFragment.findSpan(u2);
		double offset = keptFragment.Knots.get(keptFragment.maxKnotIndex-keptFragment.degree)-keptFragment.Knots.get(keptFragment.degree);
		Vector<Double> lgspoints = new Vector<Double>(); Vector<Point2D> IP = new Vector<Point2D>();
		//
		// Prepare first degree+1 IP and lgspoints
		//
		for (int i=origCurve.degree; i>=0; i--)
		{
			double pos;
			if ((k1-i)<0)
				pos = keptFragment.Knots.get(k1-i+keptFragment.maxKnotIndex-keptFragment.degree);
			else
				pos = keptFragment.Knots.get(k1-i);
			
			if (i<origCurve.degree)
			{
				double pos2 = (lgspoints.lastElement()+pos)/2;
				lgspoints.add(pos2);
				pos2 = refineIntoKnotRange(origCurve,pos2);
				IP.add((Point2D) origCurve.CurveAt(pos2).clone());
			//	drawCP(g2,new Point(Math.round((float)IP.lastElement().getX()),Math.round((float)IP.lastElement().getY())),Color.BLUE);
			}
			lgspoints.add(pos);
			pos = refineIntoKnotRange(origCurve,pos);
			IP.add((Point2D) origCurve.CurveAt(pos).clone());
		//	drawCP(g2,new Point(Math.round((float)IP.lastElement().getX()),Math.round((float)IP.lastElement().getY())),Color.BLUE);
		}
		//
		//Calculate New part to replace subCurve
		// distance for nice fitting of lgspoints
		//
		double d = Math.sqrt(IP.lastElement().distance(q.firstElement()));
		for (int i=1; i<q.size(); i++)
			d += Math.sqrt(q.get(i).distance(q.get(i-1)));
		d += Math.sqrt(origCurve.CurveAt(u2).distance(q.lastElement()));
		double oldseglength;
		if (u1<u2)
			oldseglength = u2-u1;
		else
			oldseglength = (keptFragment.Knots.get(keptFragment.maxKnotIndex-keptFragment.degree)-u1)+(u2-keptFragment.Knots.get(keptFragment.degree));
		d /= oldseglength; //Scale, TODO: for better fitting approximate old and new cordlength
		for (int i=0; i<q.size(); i++)
		{
			if (i==0)
				lgspoints.add(lgspoints.lastElement().doubleValue() + Math.sqrt(q.firstElement().distance(IP.lastElement()))/d);
			else
				lgspoints.add(lgspoints.lastElement().doubleValue() + Math.sqrt(q.get(i).distance(q.get(i-1)))/d);
			IP.add((Point2D)q.get(i).clone());
		}
		//
		// Prepare last degree+1 IP and lgsPoints
		// Replace old length by d
		//
		for (int i=0; i<=origCurve.degree; i++)
		{
			double pos;
			if ((k2+i)>keptFragment.maxKnotIndex)
				pos = keptFragment.Knots.get(k2+i-keptFragment.maxKnotIndex+keptFragment.degree);
			else
				pos = keptFragment.Knots.get(k2+i);
			if (u2<u1)
				pos += offset;
			if (i>0)
			{
				double pos2 = (lgspoints.lastElement()+pos)/2;
				lgspoints.add(pos2);
				pos2 = refineIntoKnotRange(origCurve,pos2);
				IP.add((Point2D) origCurve.CurveAt(pos2).clone());
	//			drawCP(g2,new Point(Math.round((float)IP.lastElement().getX()),Math.round((float)IP.lastElement().getY())),Color.BLUE);
			}
			lgspoints.add(pos);
			pos = refineIntoKnotRange(origCurve,pos);
			IP.add((Point2D) origCurve.CurveAt(pos).clone());
		//	drawCP(g2,new Point(Math.round((float)IP.lastElement().getX()),Math.round((float)IP.lastElement().getY())),Color.BLUE);
		}
		int maxIPIndex = IP.size()-1; //highest IP Index
		int maxKnotIndex = maxIPIndex+origCurve.degree+1;//highest KnotIndex in the resulting NURBS-Curve
		//Determine Points to evaluate for IP with cetripetal Aproach
		//At the lgspoints we evaluate the Curve, get an LGS, that is totally positive and banded
		Vector<Double> Knots = calculateKnotVector(origCurve.degree, maxKnotIndex, lgspoints);
		NURBSShape c = solveLGS(Knots, lgspoints, IP); //replacementcurve
		NURBSShape old = (new NURBSShapeFragment(origCurve,u2,u1)).getSubCurve();  //Unchanged Old part
		if (u2<u1)
		{
			return combine(old,(new NURBSShapeFragment(c,u1,u2+offset)).getSubCurve(), true);
		}
		else
		{
			c = combine((new NURBSShapeFragment(c,u1,u2)).getSubCurve(),old,false);
		}
		return c;
	}

	/**
	 * Combine 2 clamped (nonclosed) NURBSCurves, where the last Knot of s1 is the first Knot of s2
	 * 
	 * 
	 */
	private static NURBSShape combine(NURBSShape s1, NURBSShape s2, boolean OldOverCirc)
	{
		//
		// Copy Both Curves into 1 to unclamp the common part
		// Therefore the order is reversed (s2 - s1) Copy unaffected parts
		// And first is shifted by the offset of second
		Vector<Double> newKnots = new Vector<Double>();
		Vector<Point3d> newP = new Vector<Point3d>();
		s1.refreshInternalValues(); s2.refreshInternalValues();
		double s2offset = s2.Knots.get(s2.maxKnotIndex-s2.degree)-s2.Knots.get(s2.degree);
		double s1offset = s1.Knots.get(s1.maxKnotIndex-s1.degree)-s1.Knots.get(s1.degree);
		double shift = s1offset+s2offset;
		for (int i=0; i<=s2.maxKnotIndex; i++)
			newKnots.add(s2.Knots.get(i).doubleValue());
		for (int i=0; i<=s2.maxCPIndex; i++)
			newP.add((Point3d)s2.controlPointsHom.get(i).clone());
		int s1BeginKnots = newKnots.size(), s1BeginCP = newP.size();
		for (int i=s1.degree+1; i<=s1.maxKnotIndex; i++) //0...s1.degree are equal to the end of s2
			newKnots.add(s1.Knots.get(i).doubleValue()+shift);
		for (int i=0; i<=s1.maxCPIndex; i++)
			newP.add((Point3d)s1.controlPointsHom.get(i).clone());
		
		NURBSShape temp = new NURBSShape(newKnots,newP);
		temp = unclamp(temp); //So now they are unclamped, we can copy back to get s1 - s2
		temp.updateCircular(OldOverCirc); //Update Front if over cirv else back
		newKnots = new Vector<Double>();
		newP = new Vector<Point3d>();
		for (int i=0; i<=s1.degree; i++)
			newKnots.add(s1.Knots.get(i)); //Old Clamped Part
		for (int i=s1BeginKnots; i<temp.maxKnotIndex-s1.degree; i++) //copy s1 back to beginning
			newKnots.add(temp.Knots.get(i).doubleValue()-shift); //And shift back
		for (int i=s2.degree; i<s1BeginKnots; i++)
			newKnots.add(temp.Knots.get(i).doubleValue());
		
		for (int i=s1BeginCP; i<temp.maxCPIndex; i++)
			newP.add((Point3d)temp.controlPointsHom.get(i).clone());
		for (int i=s2.degree-1; i<s1BeginCP; i++)
			newP.add((Point3d)temp.controlPointsHom.get(i).clone());
		temp = new NURBSShape(newKnots,newP);
		temp = unclamp(temp); //Unclamp the part that stays at endvalues of the combinational curve
		temp.updateCircular(!OldOverCirc); //Update Front if normal, back if over circStart/End
		return temp;
	}
	/**
	 * Refine the given Curve by adding value u if u isn't yet inside and update Circular after that if necessary
	 * @param c NURBSShape to be refined
	 * @param u with this value
	 */
	private static void refineCircular(NURBSShape c, double u)
	{
		Vector<Double> ref = new Vector<Double>();
		ref.add(u);
		boolean updateCirc = ((u<=c.Knots.get(2*c.degree))||(u>=c.Knots.get(c.maxKnotIndex-2*c.degree)));
		if (!c.Knots.contains(u))
		{
			c.RefineKnots(ref);
			if (updateCirc)
				c.updateCircular(u>=c.Knots.get(c.maxKnotIndex-2*c.degree));
		}

	}

	/**
	 * Calculate Knot-Vector based on lgspoints, IP and Degree
	 */
	private static Vector<Double> calculateKnotVector(int degree, int maxKnotIndex,  Vector<Double> lgspoints)
	{
		Vector<Double> Knots = new Vector<Double>();
		Knots.setSize(maxKnotIndex+1); //Because there are 0,...,macKnotIndex Knots
		int maxIPIndex = maxKnotIndex-degree-1;
		for (int i=0; i<=degree; i++)
		{
			Knots.set(i,lgspoints.firstElement().doubleValue()); //First degree+1 Entries are zero
			Knots.set(maxKnotIndex-i,lgspoints.lastElement().doubleValue()); //Last p+1 Entries are 1
		}
		for (int j=1; j<=maxIPIndex-degree; j++) //middle values
		{
			double value = 0d;
			for (int i=j; i<=j+degree-1; i++)
			{
				value += lgspoints.get(i);
			}
			value /= degree;
			Knots.set(j+degree, value);
		}
		return Knots;
	}
	/**
	 * Cut the additionally added parts from the curve to get an 
	 * unclamped closed NURBSCurve
	 * 
	 * TODO: Try to set [u1,u2] to [0,1] For simplicity reasons?
	 * @param u1
	 * @param u2
	 */
	private static NURBSShape cutoverlaps(NURBSShape c, double u1, double u2)
	{
		int Start = c.findSpan(u1);
		int End = c.findSpan(u2);
		if (u2==c.Knots.get(c.maxKnotIndex-c.degree)) //Last possible Value the Curve is evaluated
			End++;
		if ((Start==-1)||(End==-1)||(u1>=u2)) //Ohne u out of range or invalid interval
			return new NURBSShape(); //Return amepty Shape
		
		//Raise both endvalues to multiplicity d to get an clamped curve
		int multStart = 0;
		while (c.Knots.get(Start+multStart).doubleValue()==u1)
			multStart++;
		int multEnd = 0;
		while (c.Knots.get(End-multEnd).doubleValue()==u2)
			multEnd++;
		Vector<Double> Refinement = new Vector<Double>();
		for (int i=0; i<=c.degree-multStart; i++)
			Refinement.add(u1);
		for (int i=0; i<=c.degree-multEnd; i++)
			Refinement.add(u2);
		//Nun wird der Start- und der Endpunkt
		NURBSShape subcurve = c.clone();
		subcurve.RefineKnots(Refinement); //Now it interpolates subcurve(u1) and subcurve(u2)
		Vector<Point2D> newCP = new Vector<Point2D>();
		Vector<Double> newWeight= new Vector<Double>();
		for (int i=Start+1; i<(End+c.degree+1-multStart+1); i++)
		{
			newCP.add((Point2D)subcurve.controlPoints.get(i).clone());
			newWeight.add(subcurve.cpWeight.get(i).doubleValue());
		}
		//Copy needed Knots
		int index = 0;
		Vector<Double> newKnots = new Vector<Double>();
		while (subcurve.Knots.get(index)<u1)
			index++;
		while (subcurve.Knots.get(index)<=u2)
		{
			newKnots.add(subcurve.Knots.get(index).doubleValue());
			index++;
		}
		return new NURBSShape(newKnots,newCP,newWeight);
	}

	/**
	 * For given Knot-Vector, lgspoints and InterpolationPoints this Method returns the NURBS-Shape
	 * The Knots define The range and distribution of the ControlPoints in the resulting NURBSShape
	 * The lgspoints (that lie within the range of the knots-values) define on  which values the 
	 * InterpolationPoints are reached within the curve
	 * @param Knots
	 * @param lgspoints
	 * @param IP
	 * @return
	 */
	private static NURBSShape solveLGS(Vector<Double> Knots, Vector<Double> lgspoints, Vector<Point2D> IP) 
	{	
		int maxIPIndex = IP.size()-1; //highest IP Index
		int maxKnotIndex = Knots.size()-1; //highest KnotIndex in the resulting NURBS-Curve
		int degree = maxKnotIndex-maxIPIndex-1;
		Vector<Point2D> ControlPoints = new Vector<Point2D>(); //The Resulting ConrolPointVecotr
		ControlPoints.setSize(maxIPIndex+1);
		Vector<Double> weights = new Vector<Double>();
		weights.setSize(maxIPIndex+1);
		for (int i=0; i<=maxIPIndex; i++)
		{
			weights.set(i,1.0d);
			ControlPoints.set(i,new Point(0,0)); //Zero Initialization
		}
		
		NURBSShape temp = new NURBSShape(Knots, ControlPoints,weights); //Temporary Shape for the BasisFunctions
		//Compute the basis-Function-Values and set them as row of a Matrix
		double[][] LGS = new double[maxIPIndex+1][maxIPIndex+1]; //Already zero initialized;
		for (int i=0; i<=maxIPIndex; i++) //For each Row
		{
			int firstnonzero = temp.findSpan(lgspoints.get(i));
			Vector<Double> nonZeroBasisValues = temp.BasisBSpline(lgspoints.get(i));
			for (int j=0; j<=degree; j++) //Set all nonzero Row values
			{
					LGS[i][j+firstnonzero-degree] = nonZeroBasisValues.get(j);
			}
		}
		//Now Solve the LGS for X to get P_i X-Coordinate and then for Y 
		//Because this matrix is totally positive and semibandwith less than p - gauss without pivoting is possible
		for (int i=0; i<=maxIPIndex; i++) //Calculate LGS = LR
		{
			for (int j=i; j<=maxIPIndex; j++)
			{	
				for (int k=1; k<=i-1; k++)
				{
	               LGS[i][j] -= LGS[i][k] * LGS[k][j];
				}
			}
			for (int j=i+1; j<=maxIPIndex; j++)
			{
				for (int k=1; k<=i-1; k++)
				{
				   LGS[j][i] -= LGS[j][k] * LGS[k][i];
				}
	            LGS[j][i] /= LGS[i][i];
			}
		}
		//Forward Computation of P
		for (int i=0; i<=maxIPIndex; i++)
		{
			double thisX = IP.get(i).getX(), thisY = IP.get(i).getY();
			for (int k=0; k<i; k++)
			{
				thisX -= LGS[i][k]*ControlPoints.get(k).getX();
				thisY -= LGS[i][k]*ControlPoints.get(k).getY();
			}
			ControlPoints.set(i, new Point2D.Double(thisX,thisY));
		}
	//	And Backward with R
		for (int i=maxIPIndex; i>=0; i--)
		{
			double thisPointX=ControlPoints.get(i).getX(), thisPointY=ControlPoints.get(i).getY();
			for (int k=i+1; k<=maxIPIndex; k++)
				{
					thisPointX -= LGS[i][k]*ControlPoints.get(k).getX();
					thisPointY -= LGS[i][k]*ControlPoints.get(k).getY();
				}
			thisPointX /= LGS[i][i];
			thisPointY /= LGS[i][i];
			ControlPoints.set(i,new Point2D.Double(thisPointX,thisPointY));
		}
		//Create a linear Shapepart around Start/End
		return new NURBSShape(Knots, ControlPoints,weights);
	}
	
	/**
	 * Unclamp the Curve at both sides
	 * @param c
	 * @return
	 */
	private static NURBSShape unclamp(NURBSShape c)
	{
		if ((c.getType()&NURBSShape.UNCLAMPED)==NURBSShape.UNCLAMPED)
			return c; //already unclamped
		//Algorithm 12.1
		Vector<Point3d> Pw = c.controlPointsHom;
		Vector<Double> U = c.Knots;
		int p = c.degree, n = c.maxCPIndex;
		for (int i=0; i<=p-2; i++)
		{
			U.set(p-i-1, U.get(p-i) - (U.get(n-i+1)-U.get(n-i)));
			int k=p-1;
			for (int j=i; j>=0; j--)
			{
				double alpha = (U.get(p)-U.get(k))/(U.get(p+j+1)-U.get(k));
				double x = (Pw.get(j).x - alpha*Pw.get(j+1).x)/(1.0-alpha);
				double y = (Pw.get(j).y - alpha*Pw.get(j+1).y)/(1.0-alpha);
				double w = (Pw.get(j).z - alpha*Pw.get(j+1).z)/(1.0-alpha);
				Pw.set(j, new Point3d(x,y,w));
				k--;
			}
		}
		U.set(0, U.get(1) - (U.get(n-p+2)-U.get(n-p+1)));
		for (int i=0; i<=p-2; i++)
		{
			U.set(n+i+2, U.get(n+i+1) + (U.get(p+i+1)-U.get(p+i)));
			for (int j=i; j>=0; j--)
			{
				double alpha = (U.get(n+1)-U.get(n-j))/(U.get(n-j+i+2)-U.get(n-j));
				double x = (Pw.get(n-j).x - (1d-alpha)*Pw.get(n-j-1).x)/alpha;
				double y = (Pw.get(n-j).y - (1d-alpha)*Pw.get(n-j-1).y)/alpha;
				double w = (Pw.get(n-j).z - (1d-alpha)*Pw.get(n-j-1).z)/alpha;
				Pw.set(n-j, new Point3d(x,y,w));
			}
		}
		U.set(n+p+1, U.get(n+p) + (U.get(2*p)-U.get(2*p-1)));
		c = new NURBSShape(U, Pw);
		return c;
	}
	
	/**
	 * 	 * Based on Grahams Scan this Method creates a convex hull and calculates Interpolation-Points that are at least
	 * The size of a node plus the minimal Distance away from the node 
	 * @param nodes Positions of the Nodes inside the convex hull
	 * @param sizes Node-Sizes, so that the Shape is at most sizes(i)+distance away from the node-position
	 * @param degree degree of the resulting NURBSShape
	 * @param distance Distance mentioned above in the sizes
	 * 
	 * @return
	 */
	private static NURBSShape CreateConvexHullPolygon(Vector<Point2D> nodes, Vector<Integer> sizes, int degree, double distance)
	{
		if (nodes.size()<2) //mindestens 3 Knoten nötig
			return new NURBSShape();
		Vector<Point2D> ConvexHull = GrahamsScan(nodes);
		Vector<Point2D> IPoints = new Vector<Point2D>();
		for (int i=0; i<ConvexHull.size(); i++)
		//Compute 3 Points for each Point of the convex hull to expand by minDist + individual node size
		{
			double prevX, prevY;
			if (i==0)
			{
				prevX = ConvexHull.lastElement().getX(); prevY = ConvexHull.lastElement().getY();
			}
			else
			{
				prevX = ConvexHull.get(i-1).getX(); prevY = ConvexHull.get(i-1).getY();				
			}
			double postX,postY;
			if (i==(ConvexHull.size()-1))
			{
				postX = ConvexHull.firstElement().getX(); postY = ConvexHull.firstElement().getY();
			}
			else
			{
				postX = ConvexHull.get(i+1).getX(); postY = ConvexHull.get(i+1).getY();				
			}
			double thisX = ConvexHull.get(i).getX();
			double thisY = ConvexHull.get(i).getY();
			
			int pos = nodes.indexOf(ConvexHull.get(i));
			Point2D direction1 = new Point2D.Double(prevX-thisX, prevY-thisY);
			double dir1l = direction1.distance(0d,0d);
			Point2D direction2 = new Point2D.Double(postX-thisX, postY-thisY);
			double dir2l = direction2.distance(0d,0d);
			Point2D direction3 = new Point2D.Double(direction1.getX()+direction2.getX(), direction1.getY()+direction2.getY());
			double dir3l = direction3.distance(0d,0d);

			//get inner degree at this by computing dir1-dir2 and get the degree from that direction
			double sk = (direction1.getX()*direction2.getX() + direction1.getY()*direction2.getY()) / (dir1l*dir2l);
			double x = Math.acos(sk);			
			Point2D p1,p2,p3; //Add these three points depending on arc
			double scaleby = (distance+Math.ceil((double)(sizes.get(pos))/2d)+2);
			if (x < Math.PI/4d) //less than 45°
			{
				//rotate around thisX to form a kind of circle
				p1 = new Point2D.Double(thisX - direction3.getY()/dir3l*scaleby, thisY + direction3.getX()/dir3l*scaleby);
				p2 = new Point2D.Double(thisX - direction3.getX()/dir3l*scaleby, thisY - direction3.getY()/dir3l*scaleby);						
				p3 = new Point2D.Double(thisX + direction3.getY()/dir3l*scaleby, thisY - direction3.getX()/dir3l*scaleby);
			}
			else if (x < Math.PI/(1.5d)) //less than 125°
			{		
				p1 = new Point2D.Double(thisX - direction2.getX()/dir2l*scaleby, thisY - direction2.getY()/dir2l*scaleby);
				p2 = new Point2D.Double(thisX - direction3.getX()/dir3l*scaleby, thisY - direction3.getY()/dir3l*scaleby);
				//Update p2 to get far away from thisX
				double d2 = p2.distance(thisX,thisY);
				p2 = new Point2D.Double(thisX + (p2.getX()-thisX)/d2*scaleby,thisY + (p2.getY()-thisY)/d2*scaleby);
				p3 = new Point2D.Double(thisX - direction1.getX()/dir1l*scaleby, thisY - direction1.getY()/dir1l*scaleby);
				double d3 = p3.distance(thisX,thisY);
				p3 = new Point2D.Double(thisX + (p3.getX()-thisX)/d3*scaleby,thisY + (p3.getY()-thisY)/d3*scaleby);
			}
			else
			{	//rotate dir 3 by 90° ccw
				direction3 = new Point2D.Double(direction1.getX()-direction2.getX(), direction1.getY()-direction2.getY());
				direction3 = new Point2D.Double(direction3.getY(),-direction3.getX());
				dir3l = direction3.distance(0,0);
				p2 = new Point2D.Double(thisX - direction3.getX()/dir3l*scaleby, thisY - direction3.getY()/dir3l*scaleby);
				p1 = new Point2D.Double(p2.getX()+(direction1.getX()/5),p2.getY()+(direction1.getY()/5));//parallel to direction1
				p3 = new Point2D.Double(p2.getX()+(direction2.getX()/5),p2.getY()+(direction2.getY()/5));//parallel to direction2
			}	
			IPoints.add(p1);
			IPoints.add(p2);
			IPoints.add(p3);
		}
		if (IPoints.size()<=(2*degree))
			return new NURBSShape();
		return CreateInterpolation(IPoints, degree);
	}
	
	/**
	 * Calculate the Convex Hull of a set of Points 
	 * by using Grahams Scan
	 * 
	 * @param nodes Points from which the resulting Convex Hull is calculated
	 * 
	 * @return Points of the nodes that from the convex hull in clockwise order
	 */
	private static Vector<Point2D> GrahamsScan(Vector<Point2D> nodes)
	{
		//Find Pivot Point with lowest Y Coordinate. If there are two, take the one with lowest X
		Point2D pivot = new Point2D.Double(Double.MAX_VALUE,Double.MAX_VALUE);
		Iterator<Point2D> iter = nodes.iterator();
		while (iter.hasNext())
		{
			Point2D pnext = iter.next();
			if (pnext.getY()<pivot.getY()) //new smallest
				pivot = (Point2D) pnext.clone();
			else if ((pnext.getY()==pivot.getY())&&(pnext.getX()>pivot.getX())) //Seach top right if both top
				pivot = (Point2D) pnext.clone();					
		}
		//Sort the Points nextP-P by angle with the X-Axis, which is between 0 and 180
		//Not so fast implemented but that is not so important, i think
		Vector<Point2D> sortedByAngle = new Vector<Point2D>();
		
		iter = nodes.iterator();
		while (iter.hasNext())
		{
			Point2D pnext = iter.next();
			Point2D direction = new Point2D.Double(pnext.getX()-pivot.getX(), pnext.getY()-pivot.getY());
			double deg = getDegreefromDirection(direction);
			//Sort em in
			Iterator<Point2D> sortiter = sortedByAngle.iterator();
			double cmpdeg = Double.MIN_VALUE;
			int index=0;
			while ((sortiter.hasNext())&&(cmpdeg<=deg))
			{
				Point2D pcomp = sortiter.next();
				Point2D cmpdir = new Point2D.Double(pcomp.getX()-pivot.getX(), pcomp.getY()-pivot.getY());
				cmpdeg = getDegreefromDirection(cmpdir);
				if (cmpdeg==deg) //Equal Degree, add before cmp by distance
				{
					if (cmpdir.distance(0d,0d)<=direction.distance(0d,0d))
						cmpdeg = deg + 1; //break
					else
						cmpdeg = deg - 1; //continue;
				}
				if (cmpdeg<=deg)
				index++;
			}
			sortedByAngle.add(index,pnext);
		}
		Vector<Point2D> result = new Vector<Point2D>();
		result.setSize(2);
		//Add the first two Elements - the first is pivot per def
		result.set(0, sortedByAngle.get(0));
		result.set(1, sortedByAngle.get(1));
		
		for (int i=2; i < sortedByAngle.size(); i++)
		{
			while ((result.size()>=2)
					&&(CrossProduct(result.get(result.size()-2), result.lastElement(), sortedByAngle.get(i))<0))
				result.remove(result.size()-1);
			result.add(sortedByAngle.get(i));
		}		 
		return result;
	}
	
	/**
	 * Calculating the cross product of three points
	 * @param p1
	 * @param p2
	 * @param p3
	 * @return
	 */
	private static double CrossProduct(Point2D p1, Point2D p2, Point2D p3)
	{
       return (p2.getX() - p1.getX())*(p3.getY() - p1.getY()) - (p3.getX() - p1.getX())*(p2.getY() - p1.getY());
	}
	
	/**
	 * Calculate the Degree of a given direction
	 * @param dir
	 * @return
	 */
	private static double getDegreefromDirection(Point2D dir)
	{
		//Compute Degree with inverted Y Axis due to Computergraphics
		double x = dir.getX(), y=-dir.getY();
		double length = dir.distance(0d,0d);
		if (x==0d) //90 or 270 Degree
		{
			if (y<0d) //Up
				return 90d;
			else if (y>0d) //Down
				return 270d;
			else
				return 0d;
		}
		if (y==0d) //0 or 180 Degree
		{
			if (x<0d) //Left
				return 180d;
			else //right
				return 0d;
		}
		//Now both are nonzero, 
		if (x>0d)
			if (y<0d) //  1. Quadrant
				return Math.asin(Math.abs(y)/length)*180.d/Math.PI; //In Degree
			else //y>0  , 4. Quadrant
				return Math.acos(Math.abs(y)/length)*180.d/Math.PI + 270d; //In Degree
		else //x<0 left side
			if (y<0d) //2. Quadrant
				return 180.0d - Math.asin(Math.abs(y)/length)*180.d/Math.PI; //In Degree
			else //y>0, 3. Quadrant
				return 270.0d - Math.acos(Math.abs(y)/length)*180.d/Math.PI; //In Degree
	}
	/**
	 * calculate the Value x \in [c.Knots.get(Degree),c.Knots.get(c.maxKnotIndex-c.degree]
	 * that represents pos
	 * 
	 * @param c
	 * @param pos
	 * @return
	 */
	private static double refineIntoKnotRange(NURBSShape c, double pos)
	{
		double offset = c.Knots.get(c.maxKnotIndex-c.degree)-c.Knots.get(c.degree);
		while (pos<c.Knots.get(c.degree))
			pos += offset;
		while (pos>c.Knots.get(c.maxKnotIndex-c.degree))
			pos -= offset;
		return pos;
	}
}