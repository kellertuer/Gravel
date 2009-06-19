package model;

import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.Vector;

import javax.print.attribute.SupportedValuesAttribute;

import org.w3c.dom.Node;

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
public class NURBSShapeGraphML extends NURBSShape {
	private NURBSShape origCurve;
	public NURBSShapeGraphML(NURBSShape c)
	{
		super(c.Knots, c.controlPoints,c.cpWeight); //Init the curve
		origCurve = c;
	}
	public NURBSShapeGraphML(Node shapenode)
	{
		
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
		return origCurve.getDecorationTypes()|NURBSShape.GRAPHML;
	}

	public String toGraphML(String keyID, String elementName, String nl, String indentation)
	{
		if (isEmpty())
			return "";
		String s;
		s = indentation+"<data key=\""+keyID+"\">"+nl+
			 indentation+"\t<"+elementName+" degree=\""+degree+"\">"+nl;
		for (int i=0; i<=maxKnotIndex; i++)
			s += indentation+"\t\t<knot id=\""+i+"\" u=\""+Knots.get(i).doubleValue()+"\"/>"+nl;
		for (int i=0; i<=maxCPIndex; i++)
			s += indentation+"\t\t<controlpoint id=\""+i+"\""+
			" x=\""+controlPoints.get(i).getX()+"\""+
			" y=\""+controlPoints.get(i).getY()+"\""+
			" w=\""+cpWeight.get(i).doubleValue()+"\"/>"+nl;
		s += indentation+"</data>";
		return s;
	}
}
