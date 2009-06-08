package model;

import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.Vector;

import javax.print.attribute.SupportedValuesAttribute;

/**
 * Class for handling any stuff that is ment to only affect a part of a nurbsshape
 * 
 * For a given NURBSShape this Class uses the decorator pattern to
 * extend it by subcurve-claculation and modification, that is
 * 
 * A specification of u1,u2 for a NURBSCurve C: [a,b] onto the plane with
 * u1,u2 \in [a,b] and
 * u1<u2 is the Curve is not unclamped&closed (else the values are swapted)
 *
 * Then the subcurve C*: [u1,u2] is set internally as the part to be modified
 * any modification is calculated to affect only this interval and then applied to C
 * 
 * If C is unclamped and closed and u1>u2
 * the Intervall C* is living on is a bit complicated:
 * It is [u1,b) and [a,u2] concatenated by shifting [a,u2] by b-a
 * C* on [u1,u2+(b-a)]
 * 
 * If the subcurve is nonexistent (e.g. u1=u2) also the internal curve is set to empty, not only the subcurve
 * @author ronny
 *
 */
public class NURBSShapeFragment extends NURBSShape {

	private NURBSShape subcurve;
	private double u1,u2;
	private NURBSShape origCurve;
	private enum affinType {TRANSLATION, ROTATION, SCALING, SCALING_DIR};
	/**
	 * Initialize the subcurve of c to an intervall of the parameter [start,end]
	 * (Iff start>end and c is closed and unclamped the subcurve gets [start,b)[a+offset,end+offset] where offset=b-a
	 * 
	 * The values of start & end are always kept, even if the subcurve is nonexistent (so getSubcurve is empty)
	 * @param c
	 * @param start
	 * @param end
	 */
	public NURBSShapeFragment(NURBSShape c, double start, double end)
	{
		super(c.Knots, c.controlPoints,c.cpWeight); //Init the curve
		u1=start;
		u2=end;
		origCurve = c;
		refreshDecoration();
	}
	
	@Override
	public boolean CurveEquals(NURBSShape s)
	{
		boolean sup = super.CurveEquals(s);
		if ((!sup)||((s.getDecorationTypes()&NURBSShape.FRAGMENT)==NURBSShape.FRAGMENT))
			return false;
		NURBSShapeFragment s2 = (NURBSShapeFragment)s; //Works because the else case is above
		return ((s2.getStart()==u1)&&(s2.getEnd()==u2));
	}
	
	@Override
	public NURBSShapeFragment clone()
	{
		return new NURBSShapeFragment(origCurve.clone(), u1,u2);
	}
	/**
	 * Strip a NURBSShape off every Decorator
	 * This class strips itself from the NURBSShape it belongs to
	 * @return
	 */
	@Override
	public NURBSShape stripDecorations()
	{
		return origCurve.stripDecorations();
	}
	/**
	 * Return all Decorations this class has
	 * That is all decorations the superclass has + validator
	 * @return
	 */
	@Override
	public int getDecorationTypes()
	{
		return origCurve.getDecorationTypes()|NURBSShape.FRAGMENT;
	}

	private void clearSubcurve()
	{
		if (subcurve!=null)
			subcurve.clear();
	}	
	protected int prepareFragment()
	{
		return prepareFragment(u1,u2);
	}
	/**
	 * Prepares the Fragment for manipulation of only that Part and returns the number of CP to change
	 * these CP begin with findSpan(u1)+1
	 * @return
	 */
	protected int prepareFragment(double t1, double t2)
	{
		Vector<Double> Refinement = new Vector<Double>();
		int k1 = findSpan(t1);
		int k2 = findSpan(t2);
		int returnval;
		if (t1 < t2)
		{
			int r = k2-degree-k1-1; //Number of CP we have (without refinement) inside subcurve
			int minNum = r-degree;
			if (minNum<=0)
			{
				int count = -minNum + 2;
				Refinement.clear();
				for (int i=1; i<count; i++)
				{ //TODO: Nonlinear refinement that produces more points at start/end then in the middle
					double val = t1+ (t2-t1)*(double)i/(double)count;
					if (!Knots.contains(val))
						Refinement.add(val);
					else 
						Refinement.add(val+ (t2-t1)/(double)count*Math.random());
				}
				RefineKnots(Refinement);
				return prepareFragment(t1,t2);
			}
			returnval = r;
		}
		else //if we are in the area of circular movement and we have to think of moving over start/end the formular is a little bit different and mor difficult
		{
			int r = maxCPIndex-degree-k1-1; //Last few values, that are possible, might be -1;
			r += k2-degree;
			int minNum = r-degree;
			if (minNum<=0)
			{
				int count = -minNum + 2;
				Refinement.clear();
				double offset = Knots.get(maxKnotIndex-degree)-Knots.get(degree);
				double newt2 = t2+offset; 
				int i=1; 
				while ((i<count)&&( (t1+ (newt2-t1)*(double)i/(double)count)<=Knots.get(maxKnotIndex-degree)))
				{ //Refine at the end
					double val = t1+ (newt2-t1)*(double)i/(double)count;
					if (!Knots.contains(val))
						Refinement.add(val);
					else 
						Refinement.add(val+ (t2-t1)/(double)count*Math.random());					
					i++;
				}
				if (Refinement.size()>0) //We have something to refine at the end
				{
					RefineKnots(Refinement);
					updateCircular(true);
					Refinement.clear();
				}
				while (i<count) //Rest
				{
					double val = t1+ (newt2-t1)*(double)i/(double)count-offset;
					if (!Knots.contains(val))
						Refinement.add(val);
					else 
						Refinement.add(val+ (t2-t1)/(double)count*Math.random());					
					i++;
				}
				if (Refinement.size()>0)
				{
					RefineKnots(Refinement);
					updateCircular(false);
				}
				return prepareFragment(t1,t2); //Compute new R
			}
			returnval = r;
		}
		return returnval;
	}
	
	@Override
	public void scale(double s)
	{
		this.scale(s,s);
	}
	
	@Override
	public void scale(double sx, double sy)
	{
		if (subcurve.isEmpty())
			super.scale(sx,sy);
		else
			affinTrans(affinType.SCALING,sx,sy);
	}
	
	@Override
	public void translate(double x, double y)
	{
		if (subcurve.isEmpty())
			super.translate(x,y);
		else
			affinTrans(affinType.TRANSLATION,x,y);
	}
	
	@Override
	public void rotate(double deg)
	{
		if (subcurve.isEmpty())
			super.rotate(deg);
		else
			affinTrans(affinType.ROTATION, deg, 0d);
	}	
	
	private void affinTrans(affinType typeOfModification, double val1, double val2)
	{
		if (subcurve.isEmpty())
			return;
		int numCP = prepareFragment();
		int k1 = findSpan(u1);
		if (Knots.contains(u1))
			k1++;
		double rad = val1*Math.PI/180d;
		for (int i=k1+1; i<k1+1+numCP; i++)
		{
			int j;
			if (i>maxCPIndex)
				j = i-maxCPIndex; //Circular thinking
			else
				j = i;
			//So j is our actual index
			Point2D p = (Point2D)controlPoints.get(j).clone();
			Point2D newp = (Point2D) p.clone(); //for safety purposes - identity-function
			switch (typeOfModification)
			{
				case ROTATION:
					double x = p.getX()*Math.cos(rad) + p.getY()*Math.sin(rad);
					double y = -p.getX()*Math.sin(rad) + p.getY()*Math.cos(rad);
					newp = new Point2D.Double(x,y);
					break;
				case TRANSLATION:
					newp = new Point2D.Double(p.getX()+val1, p.getY()+val2);
					break;
				case SCALING:
					newp = new Point2D.Double(p.getX()*val1,p.getY()*val2);
					break;
			}
			controlPoints.set(j, newp);
			//Circular:
			if (j<degree) //first degree ones
				controlPoints.set(maxCPIndex-degree+j+1, (Point2D) newp.clone());
			else if (j > maxCPIndex-degree) // the higher ones of not yet translates at the beginning
				controlPoints.set(j-1-maxCPIndex+degree, (Point2D) newp.clone());
		}
		refreshInternalValues();
		subcurve = ClampedSubCurve(u1,u2);
		refreshDecoration();
	}
	
	public void refreshDecoration()
	{
		//Copy this to origCurve
		origCurve.controlPoints = controlPoints;
		origCurve.controlPointsHom = controlPointsHom;
		origCurve.cpWeight = cpWeight;
		origCurve.degree = degree;
		origCurve.Knots = Knots;
		origCurve.maxCPIndex = maxCPIndex;
		origCurve.maxKnotIndex = maxKnotIndex;
		if ((!Double.isNaN(u1))&&(!Double.isNaN(u2)))
			subcurve = ClampedSubCurve(u1,u2);
		else
			subcurve = new NURBSShape();
		if (subcurve.isEmpty()) //something went wrong in subcurve creation, set this all to empty
			clearSubcurve();
	}
	/**
	 * Return the clamped Subcurve between the parameters u1 and u2
	 * This is realized by knot insertion at u1 and u2 until the multiplicity in these
	 * points equals Degree+1 and cutting off all parts ouside of [u1,u2] of the Knotvector
	 * @param u1
	 * @param u2
	 * @return
	 */
	private NURBSShape ClampedSubCurve(double u1, double u2)
	{
		int Start = findSpan(u1);
		int End = findSpan(u2);
		if ((Start==-1)||(End==-1)||(u1==u2)) //Ohne u out of range or invalid interval
			return new NURBSShape(); //Return amepty Shape
		boolean closed=true;
		closed &= (getType()==UNCLAMPED); //not closed if not unclamped
		for (int i=0; i<degree; i++)
		{
			boolean actPosEqual = ((controlPoints.get(i).getX()==controlPoints.get(maxCPIndex-degree+1+i).getX())
			&& (controlPoints.get(i).getY()==controlPoints.get(maxCPIndex-degree+1+i).getY()));
			closed &= actPosEqual;
		}
		if ((!closed)||(u2>u1)) //for nonclosed cases or if we don't run over start/end - use simpleClapmedSubCurve
			return this.simpleClampedSubCurve(u1,u2);
		//So u1 > u2 and we have to take a subcurve that includes start/end
		if (u2==Knots.get(maxKnotIndex-degree)) //Last possible Value the Curve is evaluated
			End++;
		if (u1==Knots.get(maxKnotIndex-degree)) //Last possible Value the Curve is evaluated
			Start++;
		//Raise both endvalues to multiplicity d to get an clamped curve
		double offset = Knots.get(maxKnotIndex-degree)-Knots.get(degree);
		int multStart = 0;
		while (Knots.get(Start+multStart).doubleValue()==u1)
			multStart++;
		int multEnd = 0;
		NURBSShape sub = origCurve.clone();
		while (Knots.get(End-multEnd).doubleValue()==u2)
			multEnd++;
		Vector<Double> Refinement = new Vector<Double>();
		for (int i=0; i<degree-multStart; i++)
			Refinement.add(u1);
		sub.RefineKnots(Refinement); //Now it interpolates subcurve(u1)
		if (u1>=sub.Knots.get(sub.maxKnotIndex-2*sub.degree)) //Update Circular Part in the subcurve iff u1 too near to the end
		{
			sub.updateCircular(true);
			sub.refreshInternalValues();
		}
		Refinement.clear();
		for (int i=0; i<degree-multEnd; i++)
			Refinement.add(u2);
		sub.RefineKnots(Refinement); //Now it interpolates subcurve(u1) and subcurve(u2)		
		//Handle cases if Start or end are too near to the Curve-Start/End
		//Startpoint u1 is too near at end, so that it is affected by the startCP
		//Nun wird der Start- und der Endpunkt
		Vector<Point2D> newCP = new Vector<Point2D>();
		Vector<Double> newWeight= new Vector<Double>();
		int subStart = sub.findSpan(u1);
		int subEnd = sub.findSpan(u2);
		if (sub.Knots.get(sub.maxKnotIndex-sub.degree)==u2)
			subEnd++;
		//Add CP from u1 to end of curve
		for (int i=subStart-degree; i<=sub.maxCPIndex-degree; i++)
		{
			newCP.add((Point2D)sub.controlPoints.get(i).clone());
			newWeight.add(sub.cpWeight.get(i).doubleValue());
		}
		//...and from start of curve to u2
		for (int i=0; i<=subEnd-degree; i++)
		{
			newCP.add((Point2D)sub.controlPoints.get(i).clone());
			newWeight.add(sub.cpWeight.get(i).doubleValue());				
		}	
		//Copy needed Knots
		Vector<Double> newKnots = new Vector<Double>();
		newKnots.add(u1);
		int index=0;
		while (sub.Knots.get(index)<u1)
			index++;
		//KNots from u^to end of curve
		while (index<sub.maxKnotIndex-degree)
		{
			newKnots.add(sub.Knots.get(index).doubleValue());
			index++;
		}
		//knots from start of curve to u2
		index=degree; 
		while (sub.Knots.get(index)<=u2)
		{
			newKnots.add(sub.Knots.get(index).doubleValue()+offset);
			index++;
		}
		newKnots.add(u2+offset);
		NURBSShape c = new NURBSShape(newKnots,newCP,newWeight);
		return c;
	}
	/**
	 * Return the clamped Subcurve between the parameters u1 and u2
	 * This is realized by knot insertion at u1 and u2 until the multiplicity in these
	 * points equals Degree+1 and cutting off all parts ouside of [u1,u2] of the Knotvector
	 * if (u2<u1) the values are exchanged
	 * @param u1
	 * @param u2
	 * @return
	 */
	private NURBSShape simpleClampedSubCurve(double u1, double u2)
	{
		if (u1>u2)
		{
			double t=u1; u1=u2; u2=t;
		}
		int Start = findSpan(u1);
		int End = findSpan(u2);
		if ((Start==-1)||(End==-1)||(u1==u2)) //Ohne u out of range or invalid interval
			return new NURBSShape(); //Return amepty Shape
		int multStart=0, multEnd=0;
		if (Knots.get(maxKnotIndex-degree)==u2) //Last possible Value the Curve is evaluated
			End++; //Becaue the intervall per se is open but we need the next interval
		//Raise both endvalues to multiplicity d to get an clamped curve
		while (Knots.get(Start+multStart).doubleValue()==u1)
			multStart++;
		while (Knots.get(End-multEnd).doubleValue()==u2)
			multEnd++;
		Vector<Double> Refinement = new Vector<Double>();
		NURBSShape subcurve = origCurve.clone();
		for (int i=0; i<degree-multStart; i++)
			Refinement.add(u1);
		for (int i=0; i<degree-multEnd; i++)
			Refinement.add(u2);
		subcurve.RefineKnots(Refinement);
		Vector<Point2D> newCP = new Vector<Point2D>();
		Vector<Double> newWeight= new Vector<Double>();
		int subStart = subcurve.findSpan(u1);
		int subEnd = subcurve.findSpan(u2);
		if (subcurve.Knots.get(subcurve.maxKnotIndex-subcurve.degree)==u2)
			subEnd++;
		for (int i=subStart-degree; i<=subEnd-degree; i++) //Copy needed CP
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
		NURBSShape c = new NURBSShape(newKnots,newCP,newWeight);
		return c;
	}

	//
	// Get & Set
	//
	/**
	 * get the calculated Subcurve
	 */
	public NURBSShape getSubCurve()
	{
		return subcurve;
	}
	/**
	 * Update startvalue of the subcurve
	 * updates internal subcurve and might set the whole curve to empty iff start is not in [a,b]
	 * @param start new startvalue of subcurve
	 */
	public void setStart(double start)
	{
		u1=start;
		//Update subcurve
		subcurve = ClampedSubCurve(u1,u2);
		if (subcurve.isEmpty()) //something went wrong in subcurve creation, set this all to empty
			clearSubcurve();
	}
	/**
	 * Get the value u1\in[a,b] where the subcurve starts 
	 * @return
	 */
	public double getStart()
	{
		return u1;
	}
	/**
	 * Update endvalue of the subcurve
	 * updates internal subcurve and might set the whole curve to empty iff start is not in [a,b]
	 * @param end new endvalue of the subcurve
	 */
	public void setEnd(double end)
	{
		u2=end;
		//Update subcurve
		subcurve = ClampedSubCurve(u1,u2);
		if (subcurve.isEmpty()) //something went wrong in subcurve creation, set this all to empty
			clearSubcurve();		
	}
	/**
	 * Get the value u1\in[a,b] where the subcurve ends
	 * @return
	 */
	public double getEnd()
	{
		return u2;
	}
}
