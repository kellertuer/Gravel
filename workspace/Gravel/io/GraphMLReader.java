package io;
import java.awt.Color;
import java.awt.Point;
import java.io.File;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import model.*;

import org.w3c.dom.*;
import org.xml.sax.SAXException;
/**
 * Implementation of a reader being able to read and validate a GraphML-Document extended by special fields
 * (keys) for Gravel
 *
 * A DOM-Parser and a XMLSchemaValidation are used
 * @author Ronny Bergmann
 *@since 0.4
 */
public class GraphMLReader {
	private HashMap<String, String> keyTypes = new HashMap<String, String>();
	private VGraphInterface loadedVGraph = null;
	private MGraphInterface loadedMGraph = null;
	private String errorMsg = "", WarningMsg="";
	private GeneralPreferences gp = GeneralPreferences.getInstance();

	/**
	 * Init the Reader to a File f, this also starts the parsing of the file If
	 * an error Occurs, ErrorOccured() returns true and the message is available
	 * in getErrorMsg();
	 *
	 * @param f
	 */
	public GraphMLReader(File f, boolean validate)
	{	 
		Document doc=null;
        Validator validator;
       try
       {
			SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
			Schema schema = factory.newSchema();
			validator = schema.newValidator();
			DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
			domFactory.setNamespaceAware(true); // never forget this
			DocumentBuilder builder = domFactory.newDocumentBuilder();
			doc = builder.parse(f);
			if (validate)
				validator.validate(new DOMSource(doc));
		}
		catch (java.io.IOException ioe)
		{errorMsg = "Error when opening File:\n"+ioe.getMessage();}
		catch (SAXException e)
		{WarningMsg = "Error when Parsing File:\n"+ e.getLocalizedMessage();return;}
		catch (ParserConfigurationException e)
		{WarningMsg = "Error when setting up Parser:" + e.getMessage(); return;}
		if (doc==null)
		{errorMsg = "Could not retrieve Document-XML-Tree";return;}
		if (!doc.getDocumentElement().getNodeName().equals("graphml"))
		{errorMsg = "No GraphML-XML-File"; return;}
		
		Node root = doc.getDocumentElement();
		Node n = root.getFirstChild();
		//Search for Std-Values in the Graph
		while (n!=null) //All direct Child Elements of graphml
	    {
			if (n.getNodeName().equals("key"))
			{
				parseKeyIntoPreferences(n);
			}
			n = n.getNextSibling();
	    }
		n = getFirstChildWithElementName("graph",root);
		if (n==null)
		{errorMsg = "No graph found in the GraphML-Document";return;}
		//Check for Attributes of the graph
		readAttributesAndInitGraph(n);
		if (ErrorOccured()) return;
		//Parse all Node-Elements, that are direct children of n
		parseNodes(n);
		if (ErrorOccured())	return;
		//Check for Edges (or Hyperedges if it's a hypergraph
		if (loadedVGraph!=null)
		{
			if (loadedVGraph.getType()==VGraphInterface.GRAPH)
				parseEdges(n); //Parse all edges that are direct children of graph-Element
			else if (loadedVGraph.getType()==VGraphInterface.HYPERGRAPH)
				parseHyperedges(n); //analogogous parse all Hyperedges
			if (ErrorOccured())
				return;
		}
		else if (loadedMGraph!=null)
		{
			if (loadedMGraph.getType()==MGraphInterface.GRAPH)
				parseEdges(n); //Parse all edges that are direct children of graph-Element
			else if (loadedMGraph.getType()==VGraphInterface.HYPERGRAPH)
				parseHyperedges(n); //analogogous parse all Hyperedges
			if (ErrorOccured())	return;
		}
		else 
		{errorMsg = "Unknown graph type";return;}

	parseSubgraphs(n);
	//Set Subgraphs into Graph		
	}
	/**
	 * Return the Result, if the loaded Graph is a VGraph and no error occured
	 * @return a VGraph if it was successfully loaded, else null
	 */
	public VGraphInterface getVGraph() {
		if (!ErrorOccured())
			return loadedVGraph;
		return null;
	}
	/**
	 * Return the Result of loading a File, iff it was a mathematical Graph and noerror occured
	 * @return	- the mathematical Graph if no error occured, 
	 * 			- else null
	 */
	public MGraphInterface getMGraph() {
		if (!ErrorOccured())
			return loadedMGraph;
		return null;
	}
	/**
	 * Check for an Error
	 * 
	 * @return true if an error occured while parsing, else false
	 */
	public boolean ErrorOccured() {
		return !errorMsg.equals("");
	}
	/**
	 * Return the last ErrorMsg, this value is empty (but never null) iff no
	 * Error occured
	 * 
	 * @return
	 */
	public String getErrorMsg()	{
		return errorMsg;
	}
	/**
	 * Check for a Warning occured while validating
	 * 
	 * @return true if an error occured while parsing, else false
	 */
	public boolean WarningOccured() {
		return !WarningMsg.equals("");
	}
	/**
	 * Return the last ErrorMsg, this value is empty (but never null) iff no
	 * Error occured
	 * 
	 * @return
	 */
	public String getWarningMsg()
	{
		return WarningMsg;
	}
	/**
	 * Get the Data from the Graph-<Data>-elements needed for initialization
	 *
	 * @param GraphNode
	 */
	private void readAttributesAndInitGraph(Node GraphNode)
	{
		boolean directed = gp.getBoolValue("graph.directed");
		boolean allowloops = gp.getBoolValue("graph.allowloops");
		boolean allowmultiple = gp.getBoolValue("graph.allowmultiple");
		HashMap<String, String> graphAttrib = getAttributeHashMap(GraphNode);
		if (!graphAttrib.containsKey("edgedefault"))
		{errorMsg = "no edgedefault-value found, though its mandatory."; return;}
		directed = graphAttrib.get("edgedefault").equals("directed");
		NodeList GraphChildElements = GraphNode.getChildNodes();
		HashMap<String, String> keyValues = new HashMap<String, String>();
		for (int i = 0; i < GraphChildElements.getLength(); i++) {
			// Search all Data-Elements, take their key-ID and Content as Values
			Node actual = GraphChildElements.item(i);
			if (actual.getNodeName().equals("data")
					&& (actual.getNodeType() == Node.ELEMENT_NODE)) {
				HashMap<String, String> dataAttrib = getAttributeHashMap(actual);
				if (!dataAttrib.containsKey("key"))
				{errorMsg = "No key-Reference for Data Element found";return;}
				keyValues.put(dataAttrib.get("key"), actual.getTextContent());
			}
		}
		if (keyValues.containsKey("graphloops"))
			allowloops = keyValues.get("graphloops").equals("true");
		if (keyValues.containsKey("graphmultipleedges"))
			allowmultiple = keyValues.get("graphmultipleedges").equals("true");
		if (!keyValues.containsKey("graphtype")) // Unknown Graphtype
		{errorMsg = "Unknown Graphtype in File";return;}
		String type = keyValues.get("graphtype");
		if (type.contains("visual")) {
			if (type.contains("hyper")) // Visual Hypergraph
				loadedVGraph = new VHyperGraph();
			else
				loadedVGraph = new VGraph(directed, allowloops, allowmultiple);
		} else if (type.contains("math")) {
			if (type.contains("hyper")) // Math Hypergraph
				loadedMGraph = new MHyperGraph();
			else
				loadedMGraph = new MGraph(directed, allowloops, allowmultiple);
		}
	}
	//
	// Complete Node/Edge/HyperEdge-Stuff
	//
	/**
	 * Call for each direct Child of the Graph that is a Node the
	 * ParseNode-Function
	 */
	private void parseNodes(Node GraphNode) {
		NodeList GraphChildElements = GraphNode.getChildNodes();
		for (int i = 0; i < GraphChildElements.getLength(); i++)
		{// Search all Data-Elements, take their key-ID and Content as Values
			Node actual = GraphChildElements.item(i);
			if (actual.getNodeName().equals("node")	&& (actual.getNodeType() == Node.ELEMENT_NODE))
			{
				parseNode(actual);
				if (ErrorOccured()) return;
			}
		}
	}
	/**
	 * Parse one single (V/M)Node and (if everything is right) put it into the
	 * specific (V/M ./H)Graph
	 *
	 * @param graphnodeNode
	 *            parental Node which is a Node
	 */
	private void parseNode(Node graphnodeNode) {
		if ((graphnodeNode.getNodeType() != Node.ELEMENT_NODE) || (!graphnodeNode.getNodeName().equals("node")))
		{errorMsg = "No Node found when parsing Node.";return;}
		int index;
		HashMap<String, String> nodeAttributes = getAttributeHashMap(graphnodeNode);
		if (!nodeAttributes.containsKey("id"))
		{
			errorMsg = "Error When Parsing Node ID";
			return;
		}
		try {
			index = Integer.parseInt(nodeAttributes.get("id"));
		} catch (Exception e) {
			errorMsg = "Error When Parsing Node ID";
			return;
		}
		String name = gp.getNodeName(index);
		//Init with Std Values
		VNode resultNode = new VNode(index, 0, 0, gp.getIntValue("node.size"), gp.getIntValue("node.name_distance"),gp.getIntValue("node.name_rotation"), gp.getIntValue("node.name_size"), gp.getBoolValue("node.name_visible"));
		NodeList nodeChildElements = graphnodeNode.getChildNodes();
		for (int i = 0; i < nodeChildElements.getLength(); i++)
		{
			// Search all Data-Elements, take their key-ID and Content as Values
			Node actual = nodeChildElements.item(i);
			if (actual.getNodeName().equals("data")
					&& (actual.getNodeType() == Node.ELEMENT_NODE)) {
				// For each Data element
				HashMap<String, String> dataAttrib = getAttributeHashMap(actual);
				if (!dataAttrib.containsKey("key")) {
					errorMsg = "No key-Reference for Data Element found";
					return;
				}
				String dataType = dataAttrib.get("key");
				if (dataType.equals("nodename")) // We have an Index and
					name = actual.getTextContent();
				else if (dataType.equals("nodetext")) { // Search for <nodetext> child
					VNode TextInfo = parseNodeText(getFirstChildWithElementName(
							"nodetext", actual));
					resultNode.cloneTextDetailsFrom(TextInfo);
				} else if (dataType.equals("nodeform")) {
					Node info = getFirstChildWithElementName("form", actual);
					VNode FormInfo = resultNode.clone();
					if ((info == null) && (loadedVGraph != null))
					{ errorMsg = "Incorrect form information for the node #"+ index;return;}
					else if (loadedVGraph != null) {
						FormInfo = parseNodeForm(info);
						if (!errorMsg.equals("")) // Something went wrong
							return;
						resultNode.setPosition(FormInfo.getPosition());
						resultNode.setSize(FormInfo.getSize());
					}
				} else
					main.DEBUG.println(main.DEBUG.HIGH,"Warning: Unhandled Data in Node #"+ index + " for key " + dataType);
			}
		} // End for - handle all Data Fields
		if (loadedVGraph != null) // VGraph
		{
			if ((resultNode.getPosition().x == 0)
					|| (resultNode.getPosition().y == 0)
					|| (resultNode.getSize() == 0)) {
				errorMsg = "No Form given for node " + resultNode.getIndex();
				return;
			}
			if (loadedVGraph.getType() == VGraphInterface.GRAPH)
				((VGraph) loadedVGraph).modifyNodes.add(resultNode, new MNode(
						index, name));
			else if (loadedVGraph.getType() == VGraphInterface.HYPERGRAPH)
				((VHyperGraph) loadedVGraph).modifyNodes.add(resultNode,
						new MNode(index, name));
			else
				errorMsg = "Unknown visual GraphType when parsing Nodes";
		} else if (loadedMGraph != null) // MGraph - add MNode
		{
			if (loadedMGraph.getType() == MGraphInterface.GRAPH)
				((MGraph) loadedMGraph).modifyNodes.add(new MNode(index, name));
			else if (loadedMGraph.getType() == MGraphInterface.HYPERGRAPH)
				((MHyperGraph) loadedMGraph).modifyNodes.add(new MNode(index,
						name));
			else
				errorMsg = "Unknown visual GraphType when parsing Nodes";
		}
	}

	private void parseEdges(Node GraphNode) {
		NodeList GraphChildElements = GraphNode.getChildNodes();
		for (int i = 0; i < GraphChildElements.getLength(); i++)
		{
			Node actual = GraphChildElements.item(i);
			if (actual.getNodeName().equals("edge")
					&& (actual.getNodeType() == Node.ELEMENT_NODE)) {
				parseEdge(actual);
				if (ErrorOccured())
					return;
			}
		}
	}
	private void parseEdge(Node edgeNode) {
		if ((edgeNode.getNodeType() != Node.ELEMENT_NODE)
				|| (!edgeNode.getNodeName().equals("edge"))) {
			errorMsg = "No Eode found when trying to parse an Edge.";
			return;
		}
		// Value temporary fields
		int index = -1, start = -1, end = -1, width = gp
				.getIntValue("edge.width"), value = gp
				.getIntValue("edge.value");
		String name;
		VEdgeText text = new VEdgeText();
		VEdgeLinestyle linestyle = new VEdgeLinestyle();
		VEdgeArrow arrow = new VEdgeArrow();
		Vector<Point> Points = new Vector<Point>();
		boolean orth_verticalfirst = gp.getBoolValue("edge.orth_verticalfirst");
		int type = VEdge.STRAIGHTLINE;
		VLoopEdge StdLoopEdgeValues = parseLoopEdgeDetails(null);
		// Loop Values, std
		HashMap<String, String> edgeAttrib = getAttributeHashMap(edgeNode);
		if ((!edgeAttrib.containsKey("id"))
				|| (!edgeAttrib.containsKey("source"))
				|| (!edgeAttrib.containsKey("target"))) {
			errorMsg = "Edge incomplete, either ID, Start or Endnode are missing";
			return;
		}
		try {
			index = Integer.parseInt(edgeAttrib.get("id"));
			start = Integer.parseInt(edgeAttrib.get("source"));
			end = Integer.parseInt(edgeAttrib.get("target"));
		} catch (Exception e) {
			errorMsg = "Edge incomplete, either ID, Start or Endnode could not be parsed.";
			return;
		}
		name = gp.getEdgeName(index, start, end);
		NodeList nodeChildElements = edgeNode.getChildNodes();
		for (int i = 0; i < nodeChildElements.getLength(); i++)
		{
			Node actual = nodeChildElements.item(i);
			if (actual.getNodeName().equals("data")
					&& (actual.getNodeType() == Node.ELEMENT_NODE))
			{
				HashMap<String, String> dataAttrib = getAttributeHashMap(actual);
				if (!dataAttrib.containsKey("key")) {
					errorMsg = "No key-Reference for Data Element found";
					return;
				}
				String dataType = dataAttrib.get("key");
				if (dataType.equals("edgename")) // We have the name
					name = actual.getTextContent();
				else if (dataType.equals("edgevalue")) // Simple int
				{
					try {
						value = Integer.parseInt(actual.getTextContent());
					} catch (Exception e) {
						errorMsg = "Could not Parse value for edge #" + index
								+ "";
						return;
					}
				} else if (dataType.equals("edgewidth")) // Simple int
				{
					try {
						width = Integer.parseInt(actual.getTextContent());
					} catch (Exception e) {
						errorMsg = "Could not Parse width for edge #" + index
								+ "";
						return;
					}
				} else if (dataType.equals("edgeorthogonal")) // Simple int
				{
					try {
						orth_verticalfirst = Boolean.parseBoolean(actual
								.getTextContent());
					} catch (Exception e) {
						errorMsg = "Could not Parse orthogonal information for edge #"
								+ index + "";
						return;
					}
				} else if (dataType.equals("edgetype")) // Simple string
				{
					String sType = actual.getTextContent();
					if (sType.equals("Orthogonal"))
						type = VEdge.ORTHOGONAL;
					else if (sType.equals("QuadCurve"))
						type = VEdge.QUADCURVE;
					else if (sType.equals("Segmented"))
						type = VEdge.SEGMENTED;
					else if (sType.equals("Loop"))
						type = VEdge.LOOP;
					// If it is none of that, don't throw an error, because (a)
					// Strightline always works (b) edgetype is a String
				} else if (dataType.equals("edgetext")) {
					// get Child with edge text
					Node edgetext = getFirstChildWithElementName("edgetext",
							actual);
					if (edgetext == null) {
						errorMsg = "No edgetext-Specification inside edgetext data field found for edge #"
								+ index;
						return;
					}
					text = parseEdgeText(edgetext);
				} else if (dataType.equals("edgeline")) {
					// get Child with edge text
					Node edgeline = getFirstChildWithElementName("edgeline",
							actual);
					if (edgeline == null) {
						errorMsg = "No edgeline-Specification inside edgeline data field found for edge #"
								+ index;
						return;
					}
					linestyle = parseEdgeLinestyle(edgeline);
				} else if (dataType.equals("edgearrow")) {
					// get Child with edge text
					Node edgearrow = getFirstChildWithElementName("arrow",
							actual);
					if (edgearrow == null) {
						errorMsg = "No arrow-Specification inside edgearrow data field found for edge #"
								+ index;
						return;
					}
					arrow = parseEdgeArrow(edgearrow);
				} else if (dataType.equals("edgepoints")) {
					Points = parseEdgePoints(actual);
					if (ErrorOccured())
						return;
				} else
					main.DEBUG.println(main.DEBUG.HIGH,"Warning: Unhandled Data-Field in Edge #"
									+ index + " with key " + dataType);
			}
		} // End for - handle all Data Fields
		// Build Edge
		if ((loadedVGraph != null)
				&& (loadedVGraph.getType() == VGraphInterface.GRAPH)) {
			MEdge me = new MEdge(index, start, end, value, name);
			VEdge resultEdge = null;
			switch (type) {
				case VEdge.LOOP :
					resultEdge = new VLoopEdge(index, width, StdLoopEdgeValues
							.getLength(), StdLoopEdgeValues.getDirection(),
							StdLoopEdgeValues.getProportion(),
							StdLoopEdgeValues.isClockwise());
					break;
				case VEdge.ORTHOGONAL :
					resultEdge = new VOrthogonalEdge(index, width,
							orth_verticalfirst);
					break;
				case VEdge.QUADCURVE :
					if (Points.size() == 0) {
						errorMsg = "No Bezier control poin exists for quadcurve";
					}
					resultEdge = new VQuadCurveEdge(index, width, Points
							.firstElement());
					break;
				case VEdge.SEGMENTED :
					if (Points.size() == 0) {
						errorMsg = "No suitable controlpoints for the segmented edge";
					}
					resultEdge = new VSegmentedEdge(index, width, Points);
					break;
				case VEdge.STRAIGHTLINE :
				default :
					resultEdge = new VStraightLineEdge(index, width);
			}
			if (resultEdge == null) {
				errorMsg = "No suitable edge could be created";
				return;
			}
			resultEdge.setArrow(arrow);
			resultEdge.setTextProperties(text);
			resultEdge.setLinestyle(linestyle);
			VGraph castedGraph = ((VGraph) loadedVGraph);
			if (castedGraph.modifyNodes.get(start) == null)
			{
				errorMsg = "Incident node (#"+start+") does not exist for edge #"+index;
				return;
			}
			else if (castedGraph.modifyNodes.get(end) == null)
			{
				errorMsg = "Incident node (#"+start+") does not exist for edge #"+index;
				return;
			}
			Point s = castedGraph.modifyNodes.get(start).getPosition();
			Point e = castedGraph.modifyNodes.get(end).getPosition();
			((VGraph) loadedVGraph).modifyEdges.add(resultEdge, me, s, e);
		} else if ((loadedMGraph != null)
				&& (loadedMGraph.getType() == MGraphInterface.GRAPH)) {
			MEdge me = new MEdge(index, start, end, value, name);
			((MGraph) loadedMGraph).modifyEdges.add(me);
		} else {
			errorMsg = "No suitable Graph for adding an Edge #"+index+"found";
			return;
		}
	}
	private void parseHyperedges(Node GraphNode) {
		NodeList GraphChildElements = GraphNode.getChildNodes();
		for (int i = 0; i < GraphChildElements.getLength(); i++)
		{
			Node actual = GraphChildElements.item(i);
			if (actual.getNodeName().equals("hyperedge")
					&& (actual.getNodeType() == Node.ELEMENT_NODE)) {
				parseHyperedge(actual);
				if (ErrorOccured())
					return;
			}
		}
	}
	private void parseHyperedge(Node hyperedgeNode) {
		if ((hyperedgeNode.getNodeType() != Node.ELEMENT_NODE)
				|| (!hyperedgeNode.getNodeName().equals("hyperedge"))) {
			errorMsg = "No hyperedge found when trying to parse an hyperedge.";
			return;
		}
		// Value temporary fields
		int index = -1, width = gp.getIntValue("hyperedge.width"), value = gp
				.getIntValue("hyperedge.value"), margin = gp
				.getIntValue("hyperedge.margin");
		BitSet endnodes = new BitSet();
		String name;
		VEdgeText text = new VEdgeText();
		VEdgeLinestyle linestyle = new VEdgeLinestyle();
		NURBSShape shape = new NURBSShape();
		HashMap<String, String> hyperedgeAttrib = getAttributeHashMap(hyperedgeNode);
		if (!hyperedgeAttrib.containsKey("id")) {
			errorMsg = "Edge incomplete, either ID, Start or Endnode are missing";
			return;
		}
		try {
			index = Integer.parseInt(hyperedgeAttrib.get("id"));
		} catch (Exception e) {
			errorMsg = "Edge incomplete, either ID, Start or Endnode could not be parsed.";
			return;
		}
		name = gp.getHyperedgeName(index);
		NodeList nodeChildElements = hyperedgeNode.getChildNodes();
		for (int i = 0; i < nodeChildElements.getLength(); i++)
		{
			Node actual = nodeChildElements.item(i);
			if (actual.getNodeType() == Node.ELEMENT_NODE) {
				if (actual.getNodeName().equals("endpoint")) {
					HashMap<String, String> endnodeAttrib = getAttributeHashMap(actual);
					if (!endnodeAttrib.containsKey("node")) {
						errorMsg = "No key-Reference for Data Element found";
						return;
					}
					try {
						endnodes.set(Integer
								.parseInt(endnodeAttrib.get("node")));
					} catch (Exception e) {
						errorMsg = "Invalid Node Index Reference in Hyperedge endnote of hyperedge #"
								+ index;
					}
				} else if (actual.getNodeName().equals("data")) { // For each
																	// Data
																	// element
					HashMap<String, String> dataAttrib = getAttributeHashMap(actual);
					if (!dataAttrib.containsKey("key")) {
						errorMsg = "No key-Reference for Data Element found";
						return;
					}
					String dataType = dataAttrib.get("key");
					if (dataType.equals("hyperedgename")) // We have the name
						name = actual.getTextContent();
					else if (dataType.equals("hyperedgevalue")) // Simple int
					{
						try {
							value = Integer.parseInt(actual.getTextContent());
						} catch (Exception e) {
							errorMsg = "Could not parse value for hyperedge #"
									+ index + "";
							return;
						}
					} else if (dataType.equals("hyperedgewidth")) // Simple int
					{
						try {
							width = Integer.parseInt(actual.getTextContent());
						} catch (Exception e) {
							errorMsg = "Could not parse width for hyperedge #"
									+ index + "";
							return;
						}
					} else if (dataType.equals("hyperedgemargin")) // Simple int
					{
						try {
							margin = Integer.parseInt(actual.getTextContent());
						} catch (Exception e) {
							errorMsg = "Could not parse margin for hyperedge #"
									+ index + "";
							return;
						}
					} else if (dataType.equals("hyperedgetext")) {
						// get Child with edge text
						Node edgetext = getFirstChildWithElementName(
								"hyperedgetext", actual);
						if (edgetext == null) {
							errorMsg = "No hyperedgetext-Specification inside hyperedgetext data field found for hyperedge #"
									+ index;
							return;
						}
						text = parseEdgeText(edgetext);
					} else if (dataType.equals("hyperedgeline")) {
						// get Child with edge text
						Node edgeline = getFirstChildWithElementName(
								"hyperedgeline", actual);
						if (edgeline == null) {
							errorMsg = "No hyperedgeline-Specification inside hyperedgeline data field found for hyperedge #"
									+ index;
							return;
						}
						linestyle = parseEdgeLinestyle(edgeline);
					} else if (dataType.equals("hyperedgeshape")) {
						// get Child with edge text
						Node hyperedgeshapeNode = getFirstChildWithElementName(
								"hyperedgeshape", actual);
						if (hyperedgeshapeNode == null) {
							errorMsg = "No shape-Specification inside hyperedgeshape data field found for hyperedge #"
									+ index;
							return;
						}
						NURBSShapeGraphML ShapeParser = new NURBSShapeGraphML(
								shape);
						String error = ShapeParser
								.InitFromGraphML(hyperedgeshapeNode);
						if (!error.equals("")) {
							errorMsg = error + " (hyperedge #" + index + ")";
							return;
						}
						shape = ShapeParser.stripDecorations();
					} else
						main.DEBUG.println(main.DEBUG.HIGH,"Warning: Unhandled Data-Field in Edge #"
										+ index + " with key " + dataType);
				}
			} // End if NodeType==ELEMENT
		} // End for - handle all Data Fields
		// Build hyperedge
		if (endnodes.length() == 0) {
			errorMsg = "Empty hyperedges are not allowd, but the hyperedge #"
					+ index + " is empty";
			return;
		}
		if ((loadedVGraph != null)
				&& (loadedVGraph.getType() == VGraphInterface.HYPERGRAPH)) {
			MHyperEdge mhe = new MHyperEdge(index, value, name);
			for (int i = 0; i <= endnodes.length(); i++) {
				if (endnodes.get(i)) {
					if (((VHyperGraph) loadedVGraph).getMathGraph().modifyNodes
							.get(i) == null) // Node should be set but is
												// nonexistent
					{
						errorMsg = "The endnode with index " + i
								+ " does not exist for hyperedge"+index;
						return;
					}
					mhe.addNode(i);
				}
			}
			VHyperEdge resultEdge = new VHyperEdge(index, width, margin);
			resultEdge.setTextProperties(text);
			resultEdge.setLinestyle(linestyle);
			resultEdge.setShape(shape);
			((VHyperGraph) loadedVGraph).modifyHyperEdges.add(resultEdge, mhe);
		} else if ((loadedMGraph != null)
				&& (loadedMGraph.getType() == MGraphInterface.GRAPH)) {
			MHyperEdge mhe = new MHyperEdge(index, value, name);
			for (int i = 0; i <= endnodes.length(); i++) {
				if (endnodes.get(i)) {
					if (((VHyperGraph) loadedVGraph).getMathGraph().modifyNodes
							.get(i) == null) // Node should be set but is
												// nonexistent
					{
						errorMsg = "The endnode with index "+i+" does not exist for hyperedge"+index;
						return;
					}
					mhe.addNode(i);
				}
			}
			((MHyperGraph) loadedMGraph).modifyHyperEdges.add(mhe);
		} else {
			errorMsg = "No suitable Graph for adding an Edge found";
			return;
		}
	}
	//
	// All Subgraphs
	//
	private void parseSubgraphs(Node GraphNode) {

		NodeList GraphChildElements = GraphNode.getChildNodes();
		for (int i = 0; i < GraphChildElements.getLength(); i++)
		{
			Node actual = GraphChildElements.item(i);
			if (actual.getNodeName().equals("data")
					&& (actual.getNodeType() == Node.ELEMENT_NODE)) {
				HashMap<String, String> dataAttrib = getAttributeHashMap(actual);
				if (!dataAttrib.containsKey("key")) {
					errorMsg = "key-Attribute for data-Element missing";
					return;
				}
				if (dataAttrib.get("key").startsWith("subgraph")) {
					// Search for subgraph subentry
					Node sub = getFirstChildWithElementName("subgraph", actual);
					if (sub == null) {
						errorMsg = "No subgraph-Element found, though the data-key-reference indicated one";
						return;
					}
					MSubgraph msub = parseMSubgraph(sub);
					VSubgraph vsub = parseVSubgraph(sub);
					if (ErrorOccured())
						return;
					if (loadedVGraph != null) // VGraph
					{
						if (loadedVGraph.getType() == VGraphInterface.GRAPH)
							((VGraph) loadedVGraph).modifySubgraphs.add(vsub,
									msub);
						else if (loadedVGraph.getType() == VGraphInterface.HYPERGRAPH)
							((VHyperGraph) loadedVGraph).modifySubgraphs.add(
									vsub, msub);
						else
							errorMsg = "Unknown visual GraphType when parsing Nodes";
					} else if (loadedMGraph != null) // MGraph
					{
						if (loadedMGraph.getType() == MGraphInterface.GRAPH)
							((MGraph) loadedMGraph).modifySubgraphs.add(msub);
						else if (loadedMGraph.getType() == MGraphInterface.HYPERGRAPH)
							((MHyperGraph) loadedVGraph).modifySubgraphs
									.add(msub);
						else
							errorMsg = "Unknown visual GraphType when parsing Nodes";
					}
				}
			}
		}
	}
	//
	// Std Values at the beginning of the document
	//
	/**
	 * Parse one single node that is a <key>-Element
	 *
	 * @param node
	 */
	private void parseKeyIntoPreferences(Node node) {
		int type = node.getNodeType();
		if ((type != Node.ELEMENT_NODE) || (!node.getNodeName().equals("key")))
			return;
		NamedNodeMap attrs = node.getAttributes();
		int len = attrs.getLength();
		String keyName = "", keyType = "", keyID = "";
		for (int i = 0; i < len; i++) {
			Attr attr = (Attr) attrs.item(i);
			if (attr.getNodeName().equals("attr.name"))
				keyName = attr.getNodeValue();
			if (attr.getNodeName().equals("attr.type")
					|| attr.getNodeName().equals("attr.complexType"))
				keyType = attr.getNodeValue();
			if (attr.getNodeName().equals("id"))
				keyID = attr.getNodeValue();
		}
		if (keyName.equals("") || (keyType.equals("")) || (keyID.equals(""))) // We
																				// need
																				// them
																				// always
																				// all
																				// 3
		{
			return;
		}
		// To decide when reaching data element
		keyTypes.put(keyID, keyType);
		Node defaultVal = getFirstChildWithElementName("default", node);
		if (defaultVal == null) // No Default for this key
		{
			return;
		}
		parseDefaultIntoPref(keyName, keyType, defaultVal);
	}
	/**
	 * Parse one single default value from inside a key-Element
	 * 
	 * @param pre
	 *            Text for the General Preferences
	 * @param keyType
	 *            Type of the default value from the attr.type-field
	 * @param defaultValue
	 */
	private void parseDefaultIntoPref(String pre, String keyType,
			Node defaultValue) {
		if ((!defaultValue.getNodeName().equals("default"))
				|| (pre.equals("graph.type")))
			return; // We need an default-NOde and we don't want to have an
					// graphtype-default
		String s = defaultValue.getTextContent();

		if (keyType.equals("string"))
			gp.setStringValue(pre, s);
		else if (keyType.equals("int")) {
			try {
				gp.setIntValue(pre, Integer.parseInt(s));
			} catch (Exception e) {
			}
		} else if (keyType.equals("boolean"))
			gp.setBoolValue(pre, Boolean.parseBoolean(s));
		else if (keyType.equals("float")) {
			try {
				gp.setFloatValue(pre, Float.parseFloat(s));
			} catch (Exception e) {
			}
		} else if (keyType.equals("edge.text.type")) // So this works due to pre
														// for hperedge and edge
														// text stuff
		{
			Node n = defaultValue.getFirstChild();
			while ((n != null)
					&& (!(n.getNodeName().equals("edgetext") || (n
							.getNodeName().equals("hyperedgetext")))))
				// Find the graph
				n = n.getNextSibling();
			if (n == null) {
				errorMsg = "expected edgetext not found in default element";
				return;
			}
			VEdgeText t = parseEdgeText(n);
			gp.setIntValue(pre + "_distance", t.getDistance());
			gp.setFloatValue(pre + "_position", t.getPosition());
			gp.setBoolValue(pre + "_showvalue", t.isshowvalue());
			gp.setIntValue(pre + "_size", t.getSize());
			gp.setBoolValue(pre + "_visible", t.isVisible());
		} else if (keyType.equals("edge.line.type")) // So this works due to pre
														// for hperedge and edge
														// text stuff
		{
			Node n = defaultValue.getFirstChild();
			while ((n != null)
					&& (!(n.getNodeName().equals("edgeline") || (n
							.getNodeName().equals("hyperedgeline")))))
				// Find the graph
				n = n.getNextSibling();
			if (n == null) {
				errorMsg = "expected Edgeline not found in default element";
				return;
			}
			VEdgeLinestyle l = parseEdgeLinestyle(n);
			gp.setIntValue(pre + "_distance", l.getDistance());
			gp.setIntValue(pre + "_length", l.getLength());
			gp.setIntValue(pre + "_type", l.getType());
		} else if (keyType.equals("node.text.type")) // Node Text Std Values
		{
			Node n = getFirstChildWithElementName("nodetext", defaultValue);
			if (n == null) {
				errorMsg = "no nodetext element found for default node text specification.";
				return;
			}
			VNode node = parseNodeText(n);
			pre = "node.name"; //Change pre because i messed up consistency between GP and GraphML
			gp.setIntValue(pre + "_distance", node.getNameDistance());
			gp.setIntValue(pre + "_rotation", node.getNameRotation());
			gp.setIntValue(pre + "_size", node.getNameSize());
			gp.setBoolValue(pre + "_visible", node.isNameVisible());
		} else if (keyType.equals("edge.loop.type")) // So this works due to pre
														// for hperedge and edge
														// text stuff
		{
			Node n = getFirstChildWithElementName("loopedge", defaultValue);
			if (n == null) {
				errorMsg = "no edgeloop element found for edge loop default specification.";
				return;
			}
			VLoopEdge edge = parseLoopEdgeDetails(n);
			gp.setIntValue(pre + "_direction", edge.getDirection());
			gp.setBoolValue(pre + "_clockwise", edge.isClockwise());
			gp.setIntValue(pre + "_length", edge.getLength());
			gp.setIntValue(pre + "_proportion", Math.round(100f * (float) edge
					.getProportion()));
		} else if (keyType.equals("graph.subgraph.type")) // Subgraph-Std-Values
		{
			// Search subgraph
			Node n = getFirstChildWithElementName("subgraph", defaultValue);
			if (n == null) {
				errorMsg = "expected subgraph element not found for subgraph default value specification";
				return;
			}
			MSubgraph msub = parseMSubgraph(n);
			gp.setStringValue(pre + ".name", msub.getName());
		} else if (keyType.equals("node.form.type")) // Node Form (Up to now in
														// the key a
														// circle-size)
		{
			Node n = getFirstChildWithElementName("form", defaultValue);
			if (n == null) {
				errorMsg = "expected form-element not found for node form default specification";
				return;
			}
			VNode node = parseNodeForm(n);
			gp.setIntValue(pre + ".size", node.getSize());
		} else if (keyType.equals("edge.arrow.type")) {
			Node n = getFirstChildWithElementName("arrow", defaultValue);
			if (n == null) {
				errorMsg = "expected arrow-element not found for edge arrow default specification.";
				return;
			}
			VEdgeArrow a = parseEdgeArrow(n);
			gp.setIntValue(pre + "_alpha", Math.round(a.getAngle()));
			gp.setFloatValue(pre + "_part", a.getPart());
			gp.setFloatValue(pre + "_pos", a.getPos());
			gp.setIntValue(pre + "_size", Math.round(a.getSize()));
		} else
			main.DEBUG.println(main.DEBUG.HIGH,"Unhandled preferences-key in File with keyname:" + keyType);

	}
	//
	// Single Elements inside of Node/Edge/HyperEdge/Graph
	//
	/**
	 * Parse a Node of the edge type with the attributes distance, position,
	 * show, size and visible
	 *
	 * @param edgeTextNode
	 *            a Node of type edgetext or hyperedgetext
	 * @return
	 */
	private VEdgeText parseEdgeText(Node edgeTextNode) {
		VEdgeText t = new VEdgeText(); // With Std Values;
		if ((!edgeTextNode.getNodeName().equals("edgetext"))
				&& (!edgeTextNode.getNodeName().equals("hyperedgetext"))) {
			errorMsg = "Error when Parsing (hyper)edge Text Details: No Details found";
			return t;
		}
		NamedNodeMap attrs = edgeTextNode.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++) // Look at all atributes
		{
			Attr attr = (Attr) attrs.item(i);
			// Attributes distance="10" position="77.0" size="12" show="value"
			// visible="true"
			if (attr.getNodeName().equals("distance"))
				try {
					t.setDistance(Integer.parseInt(attr.getNodeValue()));
				} catch (Exception e) {
				}
			else if (attr.getNodeName().equals("position"))
				try {
					t.setPosition(Float.parseFloat(attr
							.getNodeValue()));
				} catch (Exception e) {
				}
			else if (attr.getName().equals("size"))
				try {
					t.setSize(Integer.parseInt(attr.getNodeValue()));
				} catch (Exception e) {
				}
			else if (attr.getName().equals("show"))
				t.setshowvalue(attr.getValue().equals("value"));
			else if (attr.getName().equals("visible"))
				t.setVisible(Boolean.parseBoolean(attr.getNodeValue()));
		}
		return t;
	}
	/**
	 * Parse an XML-node with type for edgelinestyle (or hyperedgelinestyle)
	 * containing the values length, distance and type
	 * 
	 * @param edgeLineNode
	 * @return
	 */
	private VEdgeLinestyle parseEdgeLinestyle(Node edgeLineNode) {
		VEdgeLinestyle l = new VEdgeLinestyle(); // With Std Values;
		if ((!edgeLineNode.getNodeName().equals("edgeline"))
				&& (!edgeLineNode.getNodeName().equals("hyperedgeline"))) {
			errorMsg = "Error when Parsing (hyper)edge Line Details: No Details found";
			return l;
		}
		NamedNodeMap attrs = edgeLineNode.getAttributes();
		int len = attrs.getLength();
		for (int i = 0; i < len; i++) // Look at all atributes
		{
			Attr attr = (Attr) attrs.item(i);
			// Attributes length="5" distance="8" type="solid"
			if (attr.getNodeName().equals("length"))
				try {
					l.setLength(Integer.parseInt(attr.getNodeValue()));
				} catch (Exception e) {
				}
			else if (attr.getNodeName().equals("distance"))
				try {
					l.setDistance(Integer.parseInt(attr.getNodeValue()));
				} catch (Exception e) {
				}
			else if (attr.getName().equals("type")) {
				if (attr.getNodeValue().equals("solid"))
					l.setType(VEdgeLinestyle.SOLID);
				if (attr.getNodeValue().equals("dashed"))
					l.setType(VEdgeLinestyle.DASHED);
				if (attr.getNodeValue().equals("dotted"))
					l.setType(VEdgeLinestyle.DOTTED);
				if (attr.getNodeValue().equals("dotdashed"))
					l.setType(VEdgeLinestyle.DOTDASHED);
			}
		}
		return l;
	}
	/**
	 * Parse an Arrow of an Edge <arrow>-Element with attributes - size - part -
	 * position (along the edge) - headalpha (angle in the tip of the arrow)
	 * 
	 * @param n
	 * @return
	 */
	private VEdgeArrow parseEdgeArrow(Node edgearrowNode) {
		VEdgeArrow a = new VEdgeArrow();

		if (!edgearrowNode.getNodeName().equals("arrow")) {
			errorMsg = "Error when Parsing EdgeArrow-Details: No Details found";
			return a;
		}
		NamedNodeMap attrs = edgearrowNode.getAttributes();
		int len = attrs.getLength();
		for (int i = 0; i < len; i++) // Look at all atributes
		{
			Attr attr = (Attr) attrs.item(i);
			// <arrow size="14" part=".8" position=".77" headalpha="20.0"/>
			if (attr.getNodeName().equals("size"))
				try {
					a.setSize(Float.parseFloat(attr.getNodeValue()));
				} catch (Exception e) {
				}
			else if (attr.getNodeName().equals("part"))
				try {
					a.setPart(Float.parseFloat(attr.getNodeValue()));
				} catch (Exception e) {
				}
			else if (attr.getNodeName().equals("position"))
				try {
					a.setPos(Float.parseFloat(attr.getNodeValue()));
				} catch (Exception e) {
				}
			else if (attr.getNodeName().equals("headalpha"))
				try {
					a.setAngle(Float.parseFloat(attr.getNodeValue()));
				} catch (Exception e) {
				}
		}
		return a;
	}
	/**
	 * Parse the data of an Loopedge-Detail-Key/Data field
	 *
	 * @param loopedgeNode
	 * @return
	 */
	private VLoopEdge parseLoopEdgeDetails(Node loopedgeNode) {
		VLoopEdge ve = new VLoopEdge(0, 0, gp.getIntValue("edge.loop_length"),
				gp.getIntValue("edge.loop_direction"), (double) gp
						.getIntValue("edge.loop_proportion") / 100d, gp
						.getBoolValue("edge.loop_clockwise"));
		if (loopedgeNode == null) // Not given return std
			return ve;
		if (!loopedgeNode.getNodeName().equals("loopedge")) {
			errorMsg = "Error when parsing Details of a LoopEdge: Wrong Type of Node";
			return ve;
		}
		NamedNodeMap attrs = loopedgeNode.getAttributes();
		int len = attrs.getLength();
		for (int i = 0; i < len; i++) // Look at all atributes
		{
			Attr attr = (Attr) attrs.item(i);
			// Attributes length="20" proportion="1" direction="270.0"
			// clockwise="false"
			if (attr.getNodeName().equals("length"))
				try {
					ve.setLength(Integer.parseInt(attr.getNodeValue()));
				} catch (Exception e) {
				}
			else if (attr.getNodeName().equals("proportion"))
				try {
					ve.setProportion((double) Integer.parseInt(attr
							.getNodeValue()) / 100d);
				} catch (Exception e) {
				}
			else if (attr.getNodeName().equals("direction"))
				try {
					ve.setDirection(Integer.parseInt(attr.getNodeValue()));
				} catch (Exception e) {
				}
			else if (attr.getNodeName().equals("clockwise"))
				ve.setClockwise(Boolean.parseBoolean(attr.getNodeValue()));
		}
		return ve;
	}
	/**
	 * Parse the data field edge.points and return a Vector of these points if
	 * they are all correct else an empty Vector is returned and the errorMsg is
	 * set
	 * 
	 * @param edgepointsNode
	 *            Data node with the point-Elements as childs
	 * @return
	 */
	private Vector<Point> parseEdgePoints(Node edgepointsNode) {
		Vector<Point> result = new Vector<Point>();
		NodeList nodeChildElements = edgepointsNode.getChildNodes();
		for (int i = 0; i < nodeChildElements.getLength(); i++)
		{
			Node actual = nodeChildElements.item(i);
			if (actual.getNodeName().equals("point")
					&& (actual.getNodeType() == Node.ELEMENT_NODE)) { // For
																		// each
																		// Data
																		// element
				HashMap<String, String> dataAttrib = getAttributeHashMap(actual);
				if ((!dataAttrib.containsKey("id"))
						|| (!dataAttrib.containsKey("y"))
						|| (!dataAttrib.containsKey("x"))) {
					errorMsg = "Edge Point Entry incomplete";
					return new Vector<Point>();
				}
				try {
					int id = Integer.parseInt(dataAttrib.get("id"));
					int x = Integer.parseInt(dataAttrib.get("x"));
					int y = Integer.parseInt(dataAttrib.get("y"));
					if (result.size() <= id)
						result.setSize(id + 1);
					result.set(id, new Point(x, y));
				} catch (Exception e) {
					errorMsg = "Edge Point Parsing failed";
					return new Vector<Point>();
				}
			}
		} // End for - handle all Data Fields
		return result;
	}
	/**
	 * Parse a XML-Node with information about a Nodes Text and return that
	 * (within a VNode)
	 */
	private VNode parseNodeText(Node nodeTextNode) {
		VNode n = new VNode(0, 0, 0, 0, gp.getIntValue("node.name_distance"),
				gp.getIntValue("node.name_rotation"), gp
						.getIntValue("node.name_size"), gp
						.getBoolValue("node.name_visible"));

		if ((n == null) || (!nodeTextNode.getNodeName().equals("nodetext"))) { // Return
																				// Std
																				// Values
																				// that
																				// already
																				// exist
			return n;
		}
		NamedNodeMap attrs = nodeTextNode.getAttributes();
		int len = attrs.getLength();
		for (int i = 0; i < len; i++) // Look at all atributes
		{
			Attr attr = (Attr) attrs.item(i);
			// Attributes distance="10" rotation="180.0" size="12"
			// visible="true" all optional
			if (attr.getNodeName().equals("rotation"))
				try {
					n.setNameRotation(Math.round(Float.parseFloat(attr
							.getNodeValue())));
				} catch (Exception e) {
				}
			else if (attr.getNodeName().equals("distance"))
				try {
					n.setNameDistance(Integer.parseInt(attr.getNodeValue()));
				} catch (Exception e) {
				}
			else if (attr.getNodeName().equals("size"))
				try {
					n.setNameSize(Integer.parseInt(attr.getNodeValue()));
				} catch (Exception e) {
				}
			else if (attr.getNodeName().equals("visible"))
				n.setNameVisible(Boolean.parseBoolean(attr.getNodeValue()));
		}
		return n;
	}
	/**
	 * Extract the Form, which is up to now just the attributes x y and size
	 * (because it's always a cirlce)
	 * 
	 * @param nodeFormNode
	 * @return
	 */
	private VNode parseNodeForm(Node nodeFormNode) {
		VNode n = new VNode(0, 0, 0, 0, gp.getIntValue("node.name_distance"),
				gp.getIntValue("node.name_rotation"), gp
						.getIntValue("node.name_size"), gp
						.getBoolValue("node.name_visible"));
		if (!nodeFormNode.getNodeName().equals("form")) {
			errorMsg = "error when Parsing Node Form: No Node Form found";
			return n;
		}
		NamedNodeMap attrs = nodeFormNode.getAttributes();
		int len = attrs.getLength();
		int x = -1, y = -1;
		for (int i = 0; i < len; i++) // Look at all atributes
		{
			Attr attr = (Attr) attrs.item(i);
			// Attributes x,y,size (ignore form)
			if (attr.getNodeName().equals("x"))
				try {
					x = Integer.parseInt(attr.getNodeValue());
				} catch (Exception e) {
				}
			else if (attr.getNodeName().equals("y"))
				try {
					y = Integer.parseInt(attr.getNodeValue());
				} catch (Exception e) {
				}
			else if (attr.getNodeName().equals("size"))
				try {
					n.setSize(Integer.parseInt(attr.getNodeValue()));
				} catch (Exception e) {
					n.setSize(0);
				}
		}
		if ((x >= 0) && (y >= 0))
			n.setPosition(new Point(x, y));
		else
			errorMsg = "Error when parsing NodeForm, Point (" + x + "," + y
					+ ") out of Range";
		if (n.getSize() == 0) // Still 0
			n.setSize(gp.getIntValue("node.size"));
		return n;
	}
	/**
	 * Find index and all nodes/edges that belong to a subgraph and the name
	 * 
	 * @param subgraphNode
	 * @return
	 */
	private MSubgraph parseMSubgraph(Node subgraphNode) {
		int index = -1;
		NamedNodeMap attrs = subgraphNode.getAttributes();
		int len = attrs.getLength();
		for (int i = 0; i < len; i++) // Look at all atributes
		{
			Attr attr = (Attr) attrs.item(i);
			if (attr.getNodeName().equals("id"))
				try {
					index = Integer.parseInt(attr.getNodeValue());
				} catch (Exception e) {
				}
		}
		// Check SubSetStuff for nodeid, edgeids and the name
		MSubgraph s = new MSubgraph(index, "");
		// Run through all childnodes
		Node n = subgraphNode.getFirstChild();

		while (n != null) {
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				if (n.getNodeName().equals("name"))
					s.setName(n.getTextContent());
				else if (n.getNodeName().equals("nodeid"))
					try {
						s.addNode(Integer.parseInt(n.getTextContent()));
					} catch (Exception e) {
					}
				else if (n.getNodeName().equals("edgeid"))
					try {
						s.addEdge(Integer.parseInt(n.getTextContent()));
					} catch (Exception e) {
					}
				else if (n.getNodeName().equals("hyperedgeid"))
					try {
						s.addEdge(Integer.parseInt(n.getTextContent()));
					} catch (Exception e) {
					}
			}
			n = n.getNextSibling();
		}
		return s;
	}
	/**
	 * Find index and id of a subgraph
	 * 
	 * @param subgraphNode
	 * @return
	 */
	private VSubgraph parseVSubgraph(Node subgraphNode) {
		int index = -1;

		String indexString = getFirstAttributeValue("id", subgraphNode);
		try {
			index = Integer.parseInt(indexString);
		} catch (Exception e) {
		}
		// Check SubSetStuff for nodeid, edgeids and the name
		VSubgraph s = new VSubgraph(index, new Color(0, 0, 0));
		if (index == -1) {
			errorMsg = "Error when Parsing Subgraph, no ID found";
			return s;
		}
		// Run through all childnodes
		Node n = subgraphNode.getFirstChild();
		Color c = new Color(0, 0, 0);
		while (n != null) {
			if ((n.getNodeType() == Node.ELEMENT_NODE)
					&& (n.getNodeName().equals("color"))) {
				HashMap<String, String> colAttributes = getAttributeHashMap(n);
				if ((!colAttributes.containsKey("r"))
						|| (!colAttributes.containsKey("g"))
						|| (!colAttributes.containsKey("b"))) {
					errorMsg = "Error when Parsing Subgraph #" + index
							+ ": Color Attributes Missing";
					return s;
				}
				try {
					c = new Color(Integer.parseInt(colAttributes.get("r")),
							Integer.parseInt(colAttributes.get("g")), Integer
									.parseInt(colAttributes.get("b")));
				} catch (Exception e) {
					errorMsg = "Error when parsing Color of Subgraph #"
							+ s.getIndex() + ": One Value of RGB is no Integer";
					return s;
				}
				break;
			}
			n = n.getNextSibling();
		}
		if ((c.getRed() == 0) && (c.getGreen() == 0) && (c.getBlue() == 0)) {
			errorMsg = "Error when Parsing Subgraph #" + index
					+ ": No Color found";
			return s;
		}
		s = new VSubgraph(index, c);
		return s;
	}
	//
	// Help Functions
	//
	/**
	 * Search in the Child of parent for an Element-Node with the Name
	 * elementName Returns the first occurence, if that exists, else null
	 * 
	 * @param elementName
	 *            Search String in the Childs
	 * @param parent
	 *            of this parent Node
	 * @return
	 */
	private Node getFirstChildWithElementName(String elementName, Node parent){
		NodeList children = parent.getChildNodes(); // If there is da default
													// value...
		for (int i = 0; i < children.getLength(); i++) {
			if ((children.item(i).getNodeName().equals(elementName))
					&& (children.item(i).getNodeType() == Node.ELEMENT_NODE))
				return children.item(i);
		}
		return null;
	}
	/**
	 * Return all Attributes of an Element as HashMap key=value
	 * 
	 * @param parent
	 *            Node to get Attributes from
	 * @return the HashMap of Attributes, which might be empty, if there are no
	 *         attributes
	 */
	private HashMap<String, String> getAttributeHashMap(Node parent) {
		HashMap<String, String> attributes = new HashMap<String, String>();
		NamedNodeMap Nodeattrs = parent.getAttributes();
		for (int j = 0; j < Nodeattrs.getLength(); j++) {
			Attr attr = (Attr) Nodeattrs.item(j);
			attributes.put(attr.getNodeName(), attr.getNodeValue());
		}
		return attributes;
	}
	/**
	 * Return the Value of the first occurence of an attribute with specific
	 * name
	 * 
	 * @param attributename
	 *            name of the attribute
	 * @param parent
	 *            of this node
	 * @return the value of the first attribute with that name, if existent,
	 *         else null
	 */
	private String getFirstAttributeValue(String attributename, Node parent) {
		NamedNodeMap attrs = parent.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++) // Look at all atributes
		{
			Attr attr = (Attr) attrs.item(i);
			if (attr.getNodeName().equals(attributename))
				return attr.getNodeValue();
		}
		return null; // not found
	}
}