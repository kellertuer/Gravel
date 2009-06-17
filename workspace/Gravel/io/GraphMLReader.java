package io;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import javax.swing.JFileChooser;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import model.VEdgeLinestyle;
import model.VEdgeText;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import view.Gui;
import dialogs.JFileDialogs;

public class GraphMLReader {

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
			System.err.println("Something went wrong");
			return;
		}
		if (!doc.getDocumentElement().getNodeName().equals("graphml"))
			System.err.println("No GraphML File");
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
		while ((n!=null)&&(n.getNodeName().equals("graph"))) //Find the graph
			n = n.getNextSibling();
		if (n==null)
		{
			System.err.println("No Graph inside graphml Found");
			return;
		}
		NodeList GraphElements = n.getChildNodes();
		//Check for Attributes of the graph
	//	VCommonGraph g = 
		//Check for Key-Values of the graph including Subgraph-Stuff (Store them externally until edges and nodes arive
		
		//Check for nodes
		
		//Check for Edges (or Hyperedges if it's a hypergraph
		
		//Set Subgraphs into Graph

		print(n);
		
		
	}
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
		String keyName="", keyType="";
		for (int i=0; i<len; i++)
		{
			Attr attr = (Attr)attrs.item(i);
			if (attr.getNodeName().equals("attr.name"))
				keyName = attr.getNodeValue();
			if (attr.getNodeName().equals("attr.type"))
				keyType = attr.getNodeValue();
        }
		System.err.print("\nParsing key "+keyName);
		if (keyName.equals("")||(keyType.equals("")))
			return;
		NodeList children = node.getChildNodes(); //If there is da default value...
		Node defaultVal = null;
		for (int i=0; i<children.getLength(); i++)
		{
			if (children.item(i).getNodeName().equals("default"))
				defaultVal = children.item(i);
		}
		if (defaultVal==null) //No Default for this key
			return;
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
		Node n = defaultValue.getFirstChild();

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
		else
			System.err.print("Still TODO Type:"+keyType);
		
	}
	/**
	 * Parse a Node of the edge type with the attributes distance, position, show, size and visible
	 * @param edgetext a Node of type edgetext or hyperedgetext
	 * @return
	 */
	static private VEdgeText parseEdgeText(Node edgetext)
	{
		VEdgeText t = new VEdgeText(); //With Std Values;
		if ((!edgetext.getNodeName().equals("edgetext"))&&(!edgetext.getNodeName().equals("hyperedgetext")))
			return t;
        NamedNodeMap attrs = edgetext.getAttributes();
        int len = attrs.getLength();
        for (int i=0; i<len; i++) //Look at all atributes
        {
            Attr attr = (Attr)attrs.item(i);
            //Attributes distance="10" position="77.0" size="12" show="value" visible="true"
            if (attr.getNodeName().equals("distance"))
            	try {t.setDistance(Integer.parseInt(attr.getNodeValue()));} catch(Exception e){}
            else if (attr.getNodeName().equals("position")) //TODO Change model
              	try {t.setPosition(Math.round((float)Double.parseDouble(attr.getNodeValue())));} catch(Exception e){}
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
	 * @param edgetext
	 * @return
	 */
	static private VEdgeLinestyle parseEdgeLinestyle(Node edgetext)
	{
		VEdgeLinestyle l = new VEdgeLinestyle(); //With Std Values;
		if ((!edgetext.getNodeName().equals("edgeline"))&&(!edgetext.getNodeName().equals("hyperedgeline")))
			return l;
        NamedNodeMap attrs = edgetext.getAttributes();
        int len = attrs.getLength();
        for (int i=0; i<len; i++) //Look at all atributes
        {
            Attr attr = (Attr)attrs.item(i);
            //Attributes length="5" distance="8" type="solid"
            if (attr.getNodeName().equals("length"))
            	try {l.setLength(Integer.parseInt(attr.getNodeValue()));} catch(Exception e){}
            else if (attr.getNodeName().equals("distance")) //TODO Change model
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
	
	static void print(Node node)
	  {
	    int type = node.getNodeType();
	    switch (type) {
	      case Node.ELEMENT_NODE:
	        System.err.print("<" + node.getNodeName());
	        NamedNodeMap attrs = node.getAttributes();
	        int len = attrs.getLength();
	        for (int i=0; i<len; i++) {
	            Attr attr = (Attr)attrs.item(i);
	            System.err.print(" " + attr.getNodeName() + "=\"" +
	                      escapeXML(attr.getNodeValue()) + "\"");
	        }
	        System.err.print('>');
	        NodeList children = node.getChildNodes();
	        len = children.getLength();
	        for (int i=0; i<len; i++)
	          print(children.item(i));
	        System.err.print("</" + node.getNodeName() + ">");
	        break;
	      case Node.ENTITY_REFERENCE_NODE:
	        System.err.print("&" + node.getNodeName() + ";");
	        break;
	      case Node.CDATA_SECTION_NODE:
	        System.err.print("<![CDATA[" + node.getNodeValue() + "]]>");
	        break;
	      case Node.TEXT_NODE:
	        System.err.print(escapeXML(node.getNodeValue()));
	        break;
	      case Node.PROCESSING_INSTRUCTION_NODE:
	        System.err.print("<?" + node.getNodeName());
	        String data = node.getNodeValue();
	        if (data!=null && data.length()>0)
	           System.err.print(" " + data);
	        System.err.println("?>");
	        break;
	    }
	  }

	  static String escapeXML(String s) {
	    StringBuffer str = new StringBuffer();
	    int len = (s != null) ? s.length() : 0;
	    for (int i=0; i<len; i++) {
	       char ch = s.charAt(i);
	       switch (ch) {
	       case '<': str.append("&lt;"); break;
	       case '>': str.append("&gt;"); break;
	       case '&': str.append("&amp;"); break;
	       case '"': str.append("&quot;"); break;
	       case '\'': str.append("&apos;"); break;
	       default: str.append(ch);
	     }
	    }
	    return str.toString();
	  }
}
