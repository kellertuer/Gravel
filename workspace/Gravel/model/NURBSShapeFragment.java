package model;

import java.awt.geom.Point2D;
import java.util.Vector;

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
 * C* [u1,u2+(b-a)]
 * 
 * If the subcurve is nonexistent (e.g. u1=u2) also the internal curve is set to empty, not only the subcurve
 * @author ronny
 *
 */
public class NURBSShapeFragment extends NURBSShape {

	private NURBSShape subcurve;

	public NURBSShapeFragment(NURBSShape c, double u1, double u2)
	{
		super(c.Knots, c.controlPoints,c.cpWeight); //Init the curve
		subcurve = ClampedSubCurve(u1,u2);
		if (subcurve.isEmpty()) //something went wrong in subcurve creation, set this all to empty
			clear();
	}
	/**
	 * Scale all Controlpoints by factor s, if you want to resize a shape
	 * make sure to translate its middle to 0,0 before and back afterwards
	 * @param s
	 */
	public void scale(double s)
	{
		this.scale(s,s);
	}
	/**
	 * Scale all Controlpoints by factor sx and sy in the directions X and Y, if you want to resize a shape
	 * make sure to translate its middle to 0,0 before and back afterwards
	 * @param s
	 */
	public void scale(double sx, double sy)
	{
		super.scale(sx,sy); //TODO scale only [u1,u2]
	}
	/**
	 * Translate Curve - due to Translation Invariance, only the ControlPoints need to be moved
	 * @param x
	 * @param y
	 */
	public void translate(double x, double y)
	{
		super.translate(x, y); //TODO: only ranslate [u1,u2]
	}
	/**
	 * Rotate the Curve - due to Rotation Invariance, only the ControlPoints need to be moved
	 * Center of Rotation is the Origin (0,0)
	 * 
	 * @param degree Amount of rotation - The Rotation is anticlockwise (for positive degree)
	 */
	public void rotate(double degree)
	{
		super.rotate(degree); //TODO: rotate only Curve between u1 and u2
	}	
	/**
	 * get the calculated Subcurve
	 */
	public NURBSShape getSubCurve()
	{
		return subcurve;
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
		boolean closed=true;
		closed &= (getType()==UNCLAMPED); //not closed if not unclamped
		for (int i=0; i<degree; i++)
			closed &= ((controlPoints.get(i).getX()==controlPoints.get(maxCPIndex-degree+1+i).getX())
						&& (controlPoints.get(i).getY()==controlPoints.get(maxCPIndex-degree+1+i).getY()));
		
		if ((!closed)||(u2>u1)) //for nonclosed cases or if we don't run over start/end - use simpleClapmedSubCurve
			return this.simpleClampedSubCurve(u1,u2);
		//So u1 > u2 and we have to take a subcurve that includes start/end
		int Start = findSpan(u1);
		int End = findSpan(u2);
		if (u2==Knots.get(maxKnotIndex-degree)) //Last possible Value the Curve is evaluated
			End++;
		if (u1==Knots.get(maxKnotIndex-degree)) //Last possible Value the Curve is evaluated
			Start++;
		if ((Start==-1)||(End==-1)||(u1==u2)) //Ohne u out of range or invalid interval
			return new NURBSShape(); //Return amepty Shape
		//Raise both endvalues to multiplicity d to get an clamped curve
		double offset = Knots.get(maxKnotIndex-degree)-Knots.get(degree);
		int multStart = 0;
		while (Knots.get(Start+multStart).doubleValue()==u1)
			multStart++;
		int multEnd = 0;
		NURBSShape sub = clone();
		while (Knots.get(End-multEnd).doubleValue()==u2)
			multEnd++;
		Vector<Double> Refinement = new Vector<Double>();
		for (int i=0; i<degree-multStart; i++)
			Refinement.add(u1);
		sub.RefineKnots(Refinement); //Now it interpolates subcurve(u1)
		if (u1>=sub.Knots.get(sub.maxKnotIndex-2*sub.degree)) //Update Circular Part in the subcurve iff u1 too near to the end
		{
			for (int i=0; i<sub.degree; i++)
				sub.Knots.set(i, sub.Knots.get(sub.maxKnotIndex-2*degree+i).doubleValue()-offset);
			for (int i=0; i<sub.degree; i++)
			{
				sub.controlPoints.set(i, (Point2D) sub.controlPoints.get(sub.maxCPIndex-sub.degree+1+i).clone());
				sub.cpWeight.set(i, sub.cpWeight.get(sub.maxCPIndex-sub.degree+1+i).doubleValue());
			}
			sub.InitHomogeneous();
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
		//Add CP from u1 to end of curve
		for (int i=subStart-degree; i<=sub.maxCPIndex-degree; i++)
		{
			newCP.add((Point2D)sub.controlPoints.get(i).clone());
			newWeight.add(sub.cpWeight.get(i).doubleValue());
		}
		//...and from start of curve to u2
		for (int i=0; i<=subEnd-degree+multEnd; i++)
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
		if (u2==Knots.get(maxKnotIndex-degree)) //Last possible Value the Curve is evaluated
			End++;
		if (u1==Knots.get(maxKnotIndex-degree)) //Last possible Value the Curve is evaluated
			Start++; //Happens only if closed
		if ((Start==-1)||(End==-1)||(u1==u2)) //Ohne u out of range or invalid interval
			return new NURBSShape(); //Return amepty Shape
		//Raise both endvalues to multiplicity d to get an clamped curve
		int multStart = 0;
		while (Knots.get(Start+multStart).doubleValue()==u1)
			multStart++;
		int multEnd = 0;
		while (Knots.get(End-multEnd).doubleValue()==u2)
			multEnd++;
		Vector<Double> Refinement = new Vector<Double>();
		for (int i=0; i<degree-multStart; i++)
			Refinement.add(u1);
		for (int i=0; i<degree-multEnd; i++)
			Refinement.add(u2);
		NURBSShape subcurve = clone();
		subcurve.RefineKnots(Refinement); //Now it interpolates subcurve(u1) and subcurve(u2)
		Vector<Point2D> newCP = new Vector<Point2D>();
		Vector<Double> newWeight= new Vector<Double>();
		int subStart = subcurve.findSpan(u1);
		int subEnd = subcurve.findSpan(u2);
		for (int i=subStart-degree; i<=subEnd-degree+multEnd; i++) //Copy needed CP
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

}
