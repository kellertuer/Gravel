package io;
import java.awt.Color;
import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.swing.JFileChooser;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import model.*;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import view.Gui;
import dialogs.JFileDialogs;

public class GraphMLReader {

	private static HashMap<String,String> keyTypes = new HashMap<String,String>();
	private static VGraphInterface loadedVGraph=null;
	private static MGraphInterface loadedMGraph=null;
	private static String errorMsg="";
	
	public static void main(String[] args) {	 
		JFileChooser fc = new JFileChooser("Öffnen einer Gravel-Datei");
		//Letzten Ordner verwenden
		if (!GeneralPreferences.getInstance().getStringValue("graph.lastfile").equals("$NONE"))
			fc.setCurrentDirectory(new File(GeneralPreferences.getInstance().getStringValue("graph.lastfile")).getParentFile());
		fc.addChoosableFileFilter(new JFileDialogs.SimpleFilter("XML","GraphML"));
		int returnVal = fc.showOpenDialog(Gui.getInstance().getParentWindow());
		if (returnVal != JFileChooser.APPROVE_OPTION)
		{
			System.err.println("Aborted");
			return;
		}
		File f = fc.getSelectedFile();
		Document doc=null;
		try {
			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			db.getSchema();
			//parse using builder to get DOM representation of the XML file
			doc = db.parse(f);
			DOMConfiguration config = doc.getDomConfig();
			config.setParameter("schema-type", "http://www.w3.org/2001/XMLSchema");
			config.setParameter("validate", Boolean.TRUE);
		}catch(ParserConfigurationException pce) {
			pce.printStackTrace();
		}catch(SAXException se) {
			se.printStackTrace();
		}catch(IOException ioe) {
			ioe.printStackTrace();
		}
		if (doc==null)
		{
			errorMsg = "Could not retrieve Document-XML-Tree";
			return;
		}
		if (!doc.getDocumentElement().getNodeName().equals("graphml"))
			errorMsg = "No GraphML-XML-File";
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
		n = root.getFirstChild();
		while ((n!=null)&&(!n.getNodeName().equals("graph"))) //Find the graph
			n = n.getNextSibling();
		if (n==null)
		{
			errorMsg = "No graph found in the GraphML-Document";
			return;
		}
		//Check for Attributes of the graph
		readAttributesAndInitGraph(n);
		if (ErrorOccured())
			return;
		//Parse all Node-Elements, that are direct children of n
		parseNodes(n);
		//Check for Edges (or Hyperedges if it's a hypergraph
		
		//Set Subgraphs into Graph		
		
	}
	/**
	 * Check for an Error
	 * @return
	 */
	public static boolean ErrorOccured()
	{
		return !errorMsg.equals("");
	}
	/**
	 * Return the last ErrorMsg, this value is empty (but never null) iff no Error occured
	 * @return
	 */
	public static String getErrorMsg()
	{
		return errorMsg;
	}
	/**
	 * Get the Data from the Graph-<Data>-elements needed for initialization
	 * @param GraphNode
	 */
	private static void readAttributesAndInitGraph(Node GraphNode)
	{
		boolean directed=GeneralPreferences.getInstance().getBoolValue("graph.directed");
		boolean allowloops=GeneralPreferences.getInstance().getBoolValue("graph.allowloops");
		boolean allowmultiple=GeneralPreferences.getInstance().getBoolValue("graph.allowmultiple");
		NamedNodeMap GraphAttrs = GraphNode.getAttributes();
		for (int j=0; j<GraphAttrs.getLength(); j++)
		{
			Attr attr = (Attr)GraphAttrs.item(j);
			if (attr.getNodeName().equals("edgedefault"))
			{
				System.err.println("Found Data for Direction: "+attr.getValue());
				directed = attr.getValue().equals("directed");
			}
        }
		NodeList GraphChildElements = GraphNode.getChildNodes();
		HashMap<String,String> keyValues = new HashMap<String,String>();
		for (int i=0; i<GraphChildElements.getLength(); i++)
		{ //Search all Data-Elements, take their key-ID and Content as Values
			Node actual = GraphChildElements.item(i);
			if (actual.getNodeName().equals("data")&&(actual.getNodeType()==Node.ELEMENT_NODE))
			{
				NamedNodeMap attrs = actual.getAttributes();
				for (int j=0; j<attrs.getLength(); j++)
				{
					Attr attr = (Attr)attrs.item(j);
					if (attr.getNodeName().equals("key"))
						keyValues.put(attr.getValue(),actual.getTextContent());
		        }
			}
		}
		if (keyValues.containsKey("graphloops"))
			allowloops = keyValues.get("graphloops").equals("true");
		if (keyValues.containsKey("graphmultipleedges"))
			allowmultiple = keyValues.get("graphmultipleedges").equals("true");
		if (!keyValues.containsKey("graphtype")) //Unknown Graphtype
		{
			System.err.println("Unknown Graphtype in File");
			return;
		}
		String type = keyValues.get("graphtype");
		if (type.contains("visual"))
		{
			if (type.contains("hyper")) //Visual Hypergraph
				loadedVGraph = new VHyperGraph();
			else
				loadedVGraph = new VGraph(directed,allowloops,allowmultiple);
		}
		else if (type.contains("math"))
		{
			if (type.contains("hyper")) //Math Hypergraph
				loadedMGraph = new MHyperGraph();
			else
				loadedMGraph = new MGraph(directed,allowloops,allowmultiple);
		}
	}
	//
	// Complete Node/Edge/HyperEdge-Stuff
	//
	/**
	 * Call for each direct Child of the Graph that is a Node the ParseNode-Function
	 */
	private static void parseNodes(Node GraphNode)
	{
		NodeList GraphChildElements = GraphNode.getChildNodes();
		for (int i=0; i<GraphChildElements.getLength(); i++)
		{ //Search all Data-Elements, take their key-ID and Content as Values
			Node actual = GraphChildElements.item(i);
			if (actual.getNodeName().equals("node")&&(actual.getNodeType()==Node.ELEMENT_NODE))
			{
				parseNode(actual);
				if (ErrorOccured())
					return;
			}
		}
	}
	/**
	 * Parse one single (V/M)Node and (if everything is right) put it into the specific (V/M ./H)Graph
	 * @param graphnodeNode parental Node which is a Node
	 */
	private static void parseNode(Node graphnodeNode)
	{
		if ((graphnodeNode.getNodeType()!=Node.ELEMENT_NODE)||(!graphnodeNode.getNodeName().equals("node")))
		{
			errorMsg = "No Node found when parsing Node.";
			return; 
		}
		int index = -1;
		NamedNodeMap attrs = graphnodeNode.getAttributes();
		VNode resultNode = null; String name="";
		for (int j=0; j<attrs.getLength(); j++) //Find the index
		{
			Attr attr = (Attr)attrs.item(j);
			if (attr.getNodeName().equals("id"))
			try {index = Integer.parseInt(attr.getValue());} catch(Exception e){index=-1;}
		}
		if (index==-1)
		{	errorMsg = "Error When Parsing Node ID";return;}
		resultNode = new VNode(index,0,0,0,0,0,0,false);
		NodeList nodeChildElements = graphnodeNode.getChildNodes();
		for (int i=0; i<nodeChildElements.getLength(); i++)
		{ //Search all Data-Elements, take their key-ID and Content as Values
			Node actual = nodeChildElements.item(i);
			if (actual.getNodeName().equals("data")&&(actual.getNodeType()==Node.ELEMENT_NODE))
			{ //For each Data element
				NamedNodeMap Nodeattrs = actual.getAttributes();
				String dataType="";
				for (int j=0; j<Nodeattrs.getLength(); j++)
				{
					Attr attr = (Attr)Nodeattrs.item(j);
					if (attr.getNodeName().equals("key"))
						dataType = attr.getNodeValue();
		        }
				if (dataType.equals("nodename")) //We have an Index and 
					name = actual.getTextContent();
				else if (dataType.equals("nodetext"))
				{ //Search for <nodetext> child
					VNode TextInfo = parseNodeText(getFirstChildWithElementName("nodetext",actual));
					resultNode.cloneTextDetailsFrom(TextInfo); //Nothig can go wrong, if that is missing we have std values that were returned
				}
				else if (dataType.equals("nodeform"))
				{
					Node info = getFirstChildWithElementName("form",actual);
					VNode FormInfo = resultNode.clone();
					if ((info==null)&&(loadedVGraph!=null)) //We need that for a VGraph
					{
						errorMsg = "No Form existent for the node #"+index;
						return;
					}
					else if (loadedVGraph!=null)
					{	FormInfo = parseNodeForm(info);
						if (!errorMsg.equals("")) //Something went wrong
							return;
						resultNode.setPosition(FormInfo.getPosition());
						resultNode.setSize(FormInfo.getSize());
					}
				}
				else
					System.err.println("Warning: Unhandled Data-Field in Node #"+index+" for key "+dataType);
			}
		} //End for - handle all Data Fields
		if (loadedVGraph!=null) //VGraph
		{
			if (loadedVGraph.getType()==VGraphInterface.GRAPH)
				((VGraph)loadedVGraph).modifyNodes.add(resultNode,new MNode(index,name));
			else if (loadedVGraph.getType()==VGraphInterface.HYPERGRAPH)
				((VHyperGraph)loadedVGraph).modifyNodes.add(resultNode,new MNode(index,name));
			else
				errorMsg = "Unknown visual GraphType when parsing Nodes";
		}
		else if (loadedMGraph!=null) //VGraph
		{
			if (loadedMGraph.getType()==MGraphInterface.GRAPH)
				((MGraph)loadedMGraph).modifyNodes.add(new MNode(index,name));
			else if (loadedMGraph.getType()==MGraphInterface.HYPERGRAPH)
				((MHyperGraph)loadedVGraph).modifyNodes.add(new MNode(index,name));
			else
				errorMsg = "Unknown visual GraphType when parsing Nodes";
		}
	}
	//
	//All Subgraphs
	//
	
	
	//
	//Std Values at the beginning of the document
	//
	
	/**
	 * Parse one single node that is a <key>-Element
	 * @param node
	 */
	static private void parseKeyIntoPreferences(Node node)
	{
		int type = node.getNodeType();
		if ((type!=Node.ELEMENT_NODE)||(!node.getNodeName().equals("key")))
			return; 
		NamedNodeMap attrs = node.getAttributes();
		int len = attrs.getLength();
		String keyName="", keyType="",keyID="";
		for (int i=0; i<len; i++)
		{
			Attr attr = (Attr)attrs.item(i);
			if (attr.getNodeName().equals("attr.name"))
				keyName = attr.getNodeValue();
			if (attr.getNodeName().equals("attr.type"))
				keyType = attr.getNodeValue();
			if (attr.getNodeName().equals("id"))
				keyID = attr.getNodeValue();
        }
//		System.err.print("\nParsing key "+keyName);
		if (keyName.equals("")||(keyType.equals(""))||(keyID.equals("")))
			return;
		//To decide when reaching data element
		keyTypes.put(keyID,keyType);
		Node defaultVal = getFirstChildWithElementName("default",node);
		if (defaultVal==null) //No Default for this key
		{
	//		System.err.print(" (no default) ");
			return;
		}
		parseDefaultIntoPref(keyName,keyType,defaultVal);
	}
	/**
	 * Parse one single default value from inside a key-Element
	 * @param pre Text for the General Preferences
	 * @param keyType Type of the default value from the attr.type-field
	 * @param defaultValue
	 */
	static private void parseDefaultIntoPref(String pre, String keyType, Node defaultValue)
	{
		if ((!defaultValue.getNodeName().equals("default"))||(pre.equals("graph.type")))
			return;
		String s = defaultValue.getTextContent();

		if (keyType.equals("string"))
			GeneralPreferences.getInstance().setStringValue(pre,s);
		else if (keyType.equals("integer"))
		{
			try{GeneralPreferences.getInstance().setIntValue(pre,Integer.parseInt(s));}
			catch (Exception e){}
		}
		else if (keyType.equals("boolean"))
			GeneralPreferences.getInstance().setBoolValue(pre,Boolean.parseBoolean(s));
		else if (keyType.equals("float"))
		{
			try{GeneralPreferences.getInstance().setFloatValue(pre,Float.parseFloat(s));}
			catch (Exception e){}
		}
		else if (keyType.equals("edge.text.type")) //So this works due to pre for hperedge and edge text stuff
		{
			Node n = defaultValue.getFirstChild();
			while ((n!=null)&&(!(n.getNodeName().equals("edgetext")||(n.getNodeName().equals("hyperedgetext"))))) //Find the graph
				n = n.getNextSibling();
			if (n==null)
			{
				System.err.println("no Edgetext found");
				return;
			}
			VEdgeText t = parseEdgeText(n);
			GeneralPreferences.getInstance().setIntValue(pre+"_distance", t.getDistance());
			GeneralPreferences.getInstance().setIntValue(pre+"_position",t.getPosition());
			GeneralPreferences.getInstance().setBoolValue(pre+"_showvalue",t.isshowvalue());
			GeneralPreferences.getInstance().setIntValue(pre+"_size",t.getSize());
			GeneralPreferences.getInstance().setBoolValue(pre+"_visible",t.isVisible());
		}
		else if (keyType.equals("edge.line.type")) //So this works due to pre for hperedge and edge text stuff
		{
			Node n = defaultValue.getFirstChild();
			while ((n!=null)&&(!(n.getNodeName().equals("edgeline")||(n.getNodeName().equals("hyperedgeline"))))) //Find the graph
				n = n.getNextSibling();
			if (n==null)
			{
				System.err.println("no Edgeline found");
				return;
			}
			VEdgeLinestyle l = parseEdgeLinestyle(n);
			GeneralPreferences.getInstance().setIntValue(pre+"_distance", l.getDistance());
			GeneralPreferences.getInstance().setIntValue(pre+"_length",l.getLength());
			GeneralPreferences.getInstance().setIntValue(pre+"_type",l.getType());
		}
		else if (keyType.equals("node.text.type")) //Node Text Std Values
		{
			Node n = getFirstChildWithElementName("nodetext",defaultValue);
			if (n==null)
			{
				System.err.println("no Nodetext found");
				return;
			}
			VNode node = parseNodeText(n);
			GeneralPreferences.getInstance().setIntValue(pre+"_distance", node.getNameDistance());
			GeneralPreferences.getInstance().setIntValue(pre+"_rotation",node.getNameRotation());
			GeneralPreferences.getInstance().setIntValue(pre+"_size",node.getNameSize());
			GeneralPreferences.getInstance().setBoolValue(pre+"_visible",node.isNameVisible());
		}
		else if (keyType.equals("edge.loop.type")) //So this works due to pre for hperedge and edge text stuff
		{
			Node n = getFirstChildWithElementName("loopedge",defaultValue);
			if (n==null)
			{
				System.err.println("no Edgeloop for default found");
				return;
			}
			VLoopEdge edge = parseLoopEdgeDetails(n);
			GeneralPreferences.getInstance().setIntValue(pre+"_direction", edge.getDirection());
			GeneralPreferences.getInstance().setBoolValue(pre+"_clockwise",edge.isClockwise());
			GeneralPreferences.getInstance().setIntValue(pre+"_length",edge.getLength());
			GeneralPreferences.getInstance().setIntValue(pre+"_proportion",Math.round(100f*(float)edge.getProportion()));
		}
		else if (keyType.equals("graph.subgraph.type")) //Subgraph-Std-Values
		{
			//Search subgraph
			Node n = getFirstChildWithElementName("subgraph",defaultValue);
			if (n==null)
			{
				System.err.println("no Subgraph inside default found");
				return;
			}
			MSubgraph msub = parseMSubgraph(n);			
			GeneralPreferences.getInstance().setStringValue(pre+".name", msub.getName());
		}
		else if (keyType.equals("node.form.type")) //Node Form (Up to now in the key a  circle-size)
		{
			Node n = getFirstChildWithElementName("form",defaultValue);
			if (n==null)
			{
				System.err.println("no node form found");
				return;
			}
			VNode node = parseNodeForm(n);
			GeneralPreferences.getInstance().setIntValue(pre+".size", node.getSize());
		}
		else if (keyType.equals("edge.arrow.type"))
		{
			Node n = getFirstChildWithElementName("arrow",defaultValue);
			if (n==null)
			{
				System.err.println("no edge arrow information found");
				return;
			}
			VEdgeArrow a = parseEdgeArrow(n);
			GeneralPreferences.getInstance().setIntValue(pre+"_alpha", Math.round(a.getAngle()));
			GeneralPreferences.getInstance().setFloatValue(pre+"_part", a.getPart());
			GeneralPreferences.getInstance().setFloatValue(pre+"_alpha", a.getPos());
			GeneralPreferences.getInstance().setIntValue(pre+"_size", Math.round(a.getSize()));
		}
		else
			System.err.print("Still TODO Type:"+keyType);
		
	}
	//
	// Single Elements of Node/Edge/HyperEdge/Graph
	//
	
	/**
	 * Parse a Node of the edge type with the attributes distance, position, show, size and visible
	 * @param edgeTextNode a Node of type edgetext or hyperedgetext
	 * @return
	 */
	static private VEdgeText parseEdgeText(Node edgeTextNode)
	{
		VEdgeText t = new VEdgeText(); //With Std Values;
		if ((!edgeTextNode.getNodeName().equals("edgetext"))&&(!edgeTextNode.getNodeName().equals("hyperedgetext")))
		{
			errorMsg = "Error when Parsing (hyper)edge Text Details: No Details found";
			return t;
		}
        NamedNodeMap attrs = edgeTextNode.getAttributes();
        for (int i=0; i<attrs.getLength(); i++) //Look at all atributes
        {
            Attr attr = (Attr)attrs.item(i);
            //Attributes distance="10" position="77.0" size="12" show="value" visible="true"
            if (attr.getNodeName().equals("distance"))
            	try {t.setDistance(Integer.parseInt(attr.getNodeValue()));} catch(Exception e){}
            else if (attr.getNodeName().equals("position")) //TODO Change model
              	try {t.setPosition(Math.round(Float.parseFloat(attr.getNodeValue())));} catch(Exception e){}
            else if (attr.getName().equals("size"))
            	try {t.setSize(Integer.parseInt(attr.getNodeValue()));} catch(Exception e){}
            else if (attr.getName().equals("show"))
              	t.setshowvalue(attr.getValue().equals("value"));
            else if (attr.getName().equals("visible"))
               	t.setVisible(Boolean.parseBoolean(attr.getNodeValue()));
		}
        return t;
	}
	/**
	 * Parse an XML-node with type for edgelinestyle (or hyperedgelinestyle) containing the values length, distance and type
	 * @param edgeLineNode
	 * @return
	 */
	static private VEdgeLinestyle parseEdgeLinestyle(Node edgeLineNode)
	{
		VEdgeLinestyle l = new VEdgeLinestyle(); //With Std Values;
		if ((!edgeLineNode.getNodeName().equals("edgeline"))&&(!edgeLineNode.getNodeName().equals("hyperedgeline")))
		{
			errorMsg = "Error when Parsing (hyper)edge Line Details: No Details found";
			return l;
		}
        NamedNodeMap attrs = edgeLineNode.getAttributes();
        int len = attrs.getLength();
        for (int i=0; i<len; i++) //Look at all atributes
        {
            Attr attr = (Attr)attrs.item(i);
            //Attributes length="5" distance="8" type="solid"
            if (attr.getNodeName().equals("length"))
            	try {l.setLength(Integer.parseInt(attr.getNodeValue()));} catch(Exception e){}
            else if (attr.getNodeName().equals("distance"))
              	try {l.setDistance(Integer.parseInt(attr.getNodeValue()));} catch(Exception e){}
            else if (attr.getName().equals("type"))
            {
            	if	(attr.getNodeValue().equals("solid"))
            		l.setType(VEdgeLinestyle.SOLID);
               	if	(attr.getNodeValue().equals("dashed"))
               		l.setType(VEdgeLinestyle.DASHED);
               	if	(attr.getNodeValue().equals("dotted"))
                 	l.setType(VEdgeLinestyle.DOTTED);
                if	(attr.getNodeValue().equals("dotdashed"))
                	l.setType(VEdgeLinestyle.DOTDASHED);
            }
		}
        return l;
	}
	/**
	 * Parse an Arrow of an Edge <arrow>-Element with attributes
	 * - size
	 * - part
	 * - position (along the edge)
	 * - headalpha (angle in the tip of the arrow)
	 * @param n
	 * @return
	 */
	static private VEdgeArrow parseEdgeArrow(Node edgearrowNode)
	{
		VEdgeArrow a = new VEdgeArrow();

		if (!edgearrowNode.getNodeName().equals("arrow"))
		{
			errorMsg = "Error when Parsing EdgeArrow-Details: No Details found";
			return a;
		}
        NamedNodeMap attrs = edgearrowNode.getAttributes();
        int len = attrs.getLength();
        for (int i=0; i<len; i++) //Look at all atributes
        {
            Attr attr = (Attr)attrs.item(i);
            //	<arrow size="14" part=".8" position=".77" headalpha="20.0"/>
            if (attr.getNodeName().equals("size"))
            	try {a.setSize(Float.parseFloat(attr.getNodeValue()));} catch(Exception e){}
            else if (attr.getNodeName().equals("part"))
              	try {a.setPart(Float.parseFloat(attr.getNodeValue()));} catch(Exception e){}
            else if (attr.getNodeName().equals("pos"))
               	try {a.setPos(Float.parseFloat(attr.getNodeValue()));} catch(Exception e){}
            else if (attr.getNodeName().equals("headalpha"))
               	try {a.setAngle(Float.parseFloat(attr.getNodeValue()));} catch(Exception e){}
 		}
        return a;
	}
	/**
	 * Parse the data of an Loopedge-Detail-Key/Data field
	 * @param loopedge
	 * @return
	 */
	static private VLoopEdge parseLoopEdgeDetails(Node loopedge)
	{
		VLoopEdge ve = new VLoopEdge(0,0,
				GeneralPreferences.getInstance().getIntValue("edge.loop_length"),
				GeneralPreferences.getInstance().getIntValue("edge.loop_direction"),
				(double)GeneralPreferences.getInstance().getIntValue("edge.loop_proportion")/100d,
				GeneralPreferences.getInstance().getBoolValue("edge.loop_clockwise"));
		if (!loopedge.getNodeName().equals("loopedge"))
		{
			errorMsg = "Error when parsing Details of a LoopEdge: No Details Found";
			return ve;
		}
	    NamedNodeMap attrs = loopedge.getAttributes();
	    int len = attrs.getLength();
	    for (int i=0; i<len; i++) //Look at all atributes
	    {
	        Attr attr = (Attr)attrs.item(i);
			//Attributes length="20" proportion="1" direction="270.0" clockwise="false"
	        if (attr.getNodeName().equals("length"))
	        	try {ve.setLength(Integer.parseInt(attr.getNodeValue()));} catch(Exception e){}
	        else if (attr.getNodeName().equals("proportion"))
	          	try {ve.setProportion((double)Integer.parseInt(attr.getNodeValue())/100d);} catch(Exception e){}
	        else if (attr.getNodeName().equals("direction"))
	           	try {ve.setDirection(Integer.parseInt(attr.getNodeValue()));} catch(Exception e){}
	        else if (attr.getNodeName().equals("clockwise"))
	        		ve.setClockwise(Boolean.parseBoolean(attr.getNodeValue()));
		}
	    return ve;
	}
	/**
	 * Parse a XML-Node with information about a Nodes Text and return that (within a VNode)
	 */
	static private VNode parseNodeText(Node nodeTextNode)
	{
		VNode n = new VNode(0,0,0,0,
				GeneralPreferences.getInstance().getIntValue("node.name_distance"),
				GeneralPreferences.getInstance().getIntValue("node.name_rotation"),
				GeneralPreferences.getInstance().getIntValue("node.name_size"),
				GeneralPreferences.getInstance().getBoolValue("node.name_visible"));

		if ((n==null)||(!nodeTextNode.getNodeName().equals("nodetext")))
		{ //Return Std Values that already exist
			return n;
		}
        NamedNodeMap attrs = nodeTextNode.getAttributes();
        int len = attrs.getLength();
        for (int i=0; i<len; i++) //Look at all atributes
        {
            Attr attr = (Attr)attrs.item(i);
            //Attributes distance="10" rotation="180.0" size="12" visible="true"
            if (attr.getNodeName().equals("rotation"))
            	try {n.setNameRotation(Math.round(Float.parseFloat(attr.getNodeValue())));} catch(Exception e){}
            else if (attr.getNodeName().equals("distance"))
              	try {n.setNameDistance(Integer.parseInt(attr.getNodeValue()));} catch(Exception e){}
            else if (attr.getNodeName().equals("size"))
               	try {n.setNameSize(Integer.parseInt(attr.getNodeValue()));} catch(Exception e){}
            else if (attr.getNodeName().equals("visible"))
               	n.setNameVisible(Boolean.parseBoolean(attr.getNodeValue()));
 		}
        return n;
	}
	/**
	 * Extract the Form, which is up to now just the attributes
	 * x y and size (because it's always a cirlce)
	 * 
	 * @param nodeFormNode
	 * @return
	 */
	private static VNode parseNodeForm(Node nodeFormNode)
	{
		VNode n = new VNode(0,0,0,0,
				GeneralPreferences.getInstance().getIntValue("node.name_distance"),
				GeneralPreferences.getInstance().getIntValue("node.name_rotation"),
				GeneralPreferences.getInstance().getIntValue("node.name_size"),
				GeneralPreferences.getInstance().getBoolValue("node.name_visible"));
	
		if (!nodeFormNode.getNodeName().equals("form"))
		{
			errorMsg = "error when Parsing Node Form: No Node Form found";
			return n;
		}
		NamedNodeMap attrs = nodeFormNode.getAttributes();
	    int len = attrs.getLength();
	    int x=-1,y=-1;
	    for (int i=0; i<len; i++) //Look at all atributes
	    {
	        Attr attr = (Attr)attrs.item(i);
	        //Attributes x,y,size (ignore form)
	        if (attr.getNodeName().equals("x"))
	        	try {x = Integer.parseInt(attr.getNodeValue());} catch(Exception e){}
	        else if (attr.getNodeName().equals("y"))
	          	try {y = Integer.parseInt(attr.getNodeValue());} catch(Exception e){}
	        else if (attr.getNodeName().equals("size"))
	           	try {n.setSize(Integer.parseInt(attr.getNodeValue()));} catch(Exception e){}
		}
	    if ((x>=0)&&(y>=0))
	    	n.setPosition(new Point(x,y));
	    else
	    	errorMsg = "Error when parsing NodeForm, Point ("+x+","+y+") out of Range";
	    return n;
	}
	/**
	 * Find index and all nodes/edges that belong to a subgraph
	 * and the name
	 * @param subgraphNode
	 * @return
	 */
	static MSubgraph parseMSubgraph(Node subgraphNode)
	{
		int index=-1;
	   NamedNodeMap attrs = subgraphNode.getAttributes();
       int len = attrs.getLength();
       for (int i=0; i<len; i++) //Look at all atributes
       {
           Attr attr = (Attr)attrs.item(i);
           if (attr.getNodeName().equals("id"))
        	   try {index = Integer.parseInt(attr.getNodeValue());} catch(Exception e){}
       }
       //Check SubSetStuff for nodeid, edgeids and the name
       MSubgraph s = new MSubgraph(index,"");
       //Run through all childnodes
       Node n = subgraphNode.getFirstChild();

		while (n!=null)
		{
			if (n.getNodeType()==Node.ELEMENT_NODE)
			{
				if (n.getNodeName().equals("name"))
					s.setName(n.getTextContent());
				else if (n.getNodeName().equals("nodeid"))
					try {s.addNode(Integer.parseInt(n.getTextContent()));}catch(Exception e){}
				else if (n.getNodeName().equals("edgeid"))
					try {s.addEdge(Integer.parseInt(n.getTextContent()));}catch(Exception e){}
			}
			n = n.getNextSibling();
		}
		return s;
	}
	/**
	 * Find index and id of a subgraph
	 * @param subgraphNode
	 * @return
	 */
	static VSubgraph parseVSubgraph(Node subgraphNode)
	{
		int index=-1;
	   
		String indexString = getFirstAttributeValue("id",subgraphNode);
		try {index = Integer.parseInt(indexString);} catch(Exception e){}
		//Check SubSetStuff for nodeid, edgeids and the name
		VSubgraph s = new VSubgraph(index,new Color(0,0,0));
		if (index==-1)
		{
			errorMsg = "Error when Parsing Subgraph, no ID found";
			return s;
		}
		//Run through all childnodes
		Node n = subgraphNode.getFirstChild();
		Color c = new Color(0,0,0);
		while (n!=null)
		{
			if ((n.getNodeType()==Node.ELEMENT_NODE)||(n.getNodeName().equals("color")))
			{
				NamedNodeMap attrs = subgraphNode.getAttributes();
			    for (int i=0; i<attrs.getLength(); i++) //Look at all atributes
			    {
			    	Attr attr = (Attr)attrs.item(i);
			        if (attr.getNodeName().equals("r"))
			           try {c = new Color(Integer.parseInt(attr.getNodeValue()),c.getGreen(),c.getBlue());} catch(Exception e){}
			        if (attr.getNodeName().equals("g"))
			           try {c = new Color(c.getRed(),Integer.parseInt(attr.getNodeValue()),c.getBlue());} catch(Exception e){}
			        if (attr.getNodeName().equals("r"))
			           try {c = new Color(c.getRed(),c.getGreen(),Integer.parseInt(attr.getNodeValue()));} catch(Exception e){}
			    }
			}
			n = n.getNextSibling();
		}
		if ((c.getRed()==0)&&(c.getGreen()==0)&&(c.getBlue()==0))
		{
		   	   errorMsg = "Error when Parsing Subgraph #"+index+": No Color found";
		   	   return s;
		}
		s = new VSubgraph(index,c);
		return s;		
	}
	//
	//Helper Functions
	//
	/**
	 * Search in the Child of parent for an Element-Node with the Name elementName
	 * Returns the first occurence, if that exists, else null
	 * @param elementName Search String in the Childs
	 * @param parent of this parent Node
	 * @return
	 */
	private static Node getFirstChildWithElementName(String elementName, Node parent)
	{
		NodeList children = parent.getChildNodes(); //If there is da default value...
		for (int i=0; i<children.getLength(); i++)
		{
			if ((children.item(i).getNodeName().equals(elementName))&&(children.item(i).getNodeType()==Node.ELEMENT_NODE))
				return children.item(i);
		}
		return null;
	}
	/**
	 * Return the Value of the first occurence of an attribute with specific name
	 * @param attributename name of the attribute
	 * @param parent of this node
	 * @return the value of the first attribute with that name, if existent, else null
	 */
	private static String getFirstAttributeValue(String attributename, Node parent)
	{
		   NamedNodeMap attrs = parent.getAttributes();
	       for (int i=0; i<attrs.getLength(); i++) //Look at all atributes
	       {
	    	   Attr attr = (Attr)attrs.item(i);
	           if (attr.getNodeName().equals(attributename))
	        	   return attr.getNodeValue();
	       }
	       return null; //not found
	}
}
