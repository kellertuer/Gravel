package model;

import org.w3c.dom.Node;

/**
 * Class for handling any stuff that from GraphML and into GraphML
 * If the subcurve is nonexistent (e.g. u1=u2) also the internal curve is set to empty, not only the subcurve
 * @author Ronny Bergmann
 * @since 0.4
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
		s += indentation+"\t</"+elementName+">"+nl;
		s += indentation+"</data>"+nl;
		return s;
	}
}
