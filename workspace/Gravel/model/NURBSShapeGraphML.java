package model;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Vector;

import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
	private void setDecoTo(NURBSShape c)
	{
		super.setCurveTo(c.Knots, c.controlPoints,c.cpWeight); //Init the curve
		origCurve = c;
	}
	/**
	 * Init this Curve from a XML-DOM-Node
	 * 
	 * @param nurbsshapeNode the XML-DOM-Node
	 * @return an error Message if one occurs
	 */
	public String InitFromGraphML(Node nurbsshapeNode)
	{
		if ((nurbsshapeNode.getNodeType()!=Node.ELEMENT_NODE)||(!nurbsshapeNode.getNodeName().equals("hyperedgeshape")))
		{ setDecoTo(new NURBSShape()); return "No hyperedge found when trying to parse an hyperedge.";}
		int pDegree;
		Vector<Double> pKnots=new Vector<Double>(), pWeight=new Vector<Double>();
		Vector<Point2D> pControlPoints= new Vector<Point2D>();
		HashMap<String,String> shapeAttrib = getAttributeHashMap(nurbsshapeNode);
		if (!shapeAttrib.containsKey("degree"))
		{ setDecoTo(new NURBSShape()); return "Edge incomplete, either ID, Start or Endnode are missing"; }
		try	{ pDegree = Integer.parseInt(shapeAttrib.get("degree"));}
		catch(Exception e) { setDecoTo(new NURBSShape()); return "can't parse degree for hyperedgeshape."; }
		NodeList nodeChildElements = nurbsshapeNode.getChildNodes();
		for (int i=0; i<nodeChildElements.getLength(); i++)
		{ //Search all Data-Elements, take their key-ID and Content as Values
			Node actual = nodeChildElements.item(i);
			if (actual.getNodeType()==Node.ELEMENT_NODE)
			{
				if (actual.getNodeName().equals("knot"))
				{
					HashMap<String,String> knotAttributes= getAttributeHashMap(actual);
					if ((!knotAttributes.containsKey("id"))||(!knotAttributes.containsKey("u")))
					{ setDecoTo(new NURBSShape()); return "No id or no knot value for hyperedge shape found";}
					double u; int id;
					try	{ u=Double.parseDouble(knotAttributes.get("u")); id = Integer.parseInt(knotAttributes.get("id"));}
					catch(Exception e)
					{ setDecoTo(new NURBSShape()); return "error when parsing knot or knot id in hyperedge nurbs shape";}
					if (pKnots.size()<=id)
						pKnots.setSize(id+1);
					pKnots.set(id,u);
				}
				else if (actual.getNodeName().equals("controlpoint"))
				{
					HashMap<String,String> knotAttributes= getAttributeHashMap(actual);
					if ((!knotAttributes.containsKey("id"))||(!knotAttributes.containsKey("x"))
							||(!knotAttributes.containsKey("y"))||(!knotAttributes.containsKey("w")))
					{ setDecoTo(new NURBSShape()); return "incomplete control point information for hyperedge shape found";}
					double x,y,w; int id;
					try	{
						x = Double.parseDouble(knotAttributes.get("x")); 
						y = Double.parseDouble(knotAttributes.get("y")); 
						w = Double.parseDouble(knotAttributes.get("w")); 
						id = Integer.parseInt(knotAttributes.get("id"));}
					catch(Exception e)
					{ setDecoTo(new NURBSShape()); return "error when parsing control point information for hyperedge shape";}
					if (pControlPoints.size()<=id)
						pControlPoints.setSize(id+1);
					pControlPoints.set(id,new Point2D.Double(x,y));
					if (pWeight.size()<=id)
						pWeight.setSize(id+1);
					pWeight.set(id,w);
				}
			}
		} //End for all Children
		//Check for correct degree
		int degreeCheck = pKnots.size()-pControlPoints.size()-1;
		if (pDegree!=degreeCheck)
		{ setDecoTo(new NURBSShape()); return "Error when parsing hyper edge shape: Degree does not fit the knot/controlpoint dimensions";}
		if (pControlPoints.size()!=pWeight.size())
		{ setDecoTo(new NURBSShape()); return "Error when parsing hyper edge shape: Controlpoint and weight dimension differ frm each other.";}
		setDecoTo(new NURBSShape(pKnots,pControlPoints,pWeight));
		return "";
	}
	/**
	 * Return all Attributes of an Element as HashMap key=value
	 * @param parent Node to get Attributes from
	 * @return the HashMap of Attributes, which might be empty, if there are no attributes
	 */
	private static HashMap<String,String> getAttributeHashMap(Node parent)
	{
		HashMap<String,String> attributes = new HashMap<String,String>();
		NamedNodeMap Nodeattrs = parent.getAttributes();
		for (int j=0; j<Nodeattrs.getLength(); j++)
		{
			Attr attr = (Attr)Nodeattrs.item(j);
			attributes.put(attr.getNodeName(),attr.getNodeValue());
        }
		return attributes;
	}
}
