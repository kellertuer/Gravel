package io;



import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Stack;
import java.util.TreeMap;

import model.MGraph;
import model.VGraph;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
/**
 * Liest einen Graphen im GraphML-Gravel-Format ein. Benötigt dafür 2 Durchläufe:
 *  - Einen, um den Typ festzustellen und die Keys auf Korrektheit zu prüfen
 *  - Einen, um den Graphen einzulesen
 * 
 * @author Ronny Bergmann
 * @since 0.2
 */
public class GravelMLReader 
{
	/**
	 *  Diese Klasse sucht in graph den Key id="gt" der angibt, was für ein Graphentyp vorliegt.
	 *  außerdem füllt er eine Liste von Keys mit ihren ids, um zu prüfen, ob alle da sind. Inklusive ihrer Typen. 
	 */
	private class typeExtractor implements ContentHandler
	{
		private String graphtype = null, position="";;
		private Stack<String> path; //graphtype as the result and path for the actual path
		private String data_key = ""; //For Data Key searching for gt
		private String id="", fortype="", type=""; //For any key
		private TreeMap<String,String> keys;
		
		public typeExtractor()
		{
			super();
			keys = new TreeMap<String,String>();
			path = new Stack<String>();
		}
			
		public void characters(char[] text, int start, int length) throws SAXException 
		{
			String val = "";
			for (int i=0; i<length; i++)
				val += text[start+i];
			if (data_key.equals("gt"))
				graphtype = val;
			//if (!data_key.equals(""))
					//System.err.println("Found "+val+" at "+data_key);
		}
		public void endDocument() throws SAXException {}
		public void endElement(String namespaceURI, String localName,String qualifiedName) throws SAXException 
		{
			String verlassen = path.pop();
			if (verlassen!=localName) //Ein Element nicht abgeschlosen
					throw new SAXException("ERROR! Tag nicht geschlossen");
			if (position.equals(verlassen))
				position = "";
			else //sonst den Punkt auch entfernen
				position = position.substring(0, position.length()-verlassen.length()-1);	
			if (localName.equals("key")) // Key gefunden
			{	
				//System.err.println("Leaving Key #ID '"+id+"' for='"+fortype+"'with type='"+type+"'. Saving to Map.");
				keys.put((fortype+"."+id),type);
				id = ""; //Reset id
				fortype=""; //Reset for 
				type = ""; //Reset Type
			}
			else if (localName.equals("data"))
			{
				data_key = "";
			}
			
		}
		public void endPrefixMapping(String arg0) throws SAXException {}
		public void ignorableWhitespace(char[] arg0, int arg1, int arg2) throws SAXException {}
		public void processingInstruction(String arg0, String arg1) throws SAXException {}
		public void setDocumentLocator(Locator arg0) {}
		public void skippedEntity(String arg0) throws SAXException {}
		public void startDocument() throws SAXException {}
		public void startElement(String namespaceURI, String localName,String qualifiedName, Attributes atts)
		{
			path.push(localName);
			if (position.equals(""))
				position = localName;
			else
				position += "."+localName;
			if (localName.equals("key")) //Attribute herausholen
			{
				id = atts.getValue("id"); //Reset id
				fortype= atts.getValue("for");
				type = atts.getValue("attr.type");
			}
			else if (localName.equals("data"))
			{
				data_key = atts.getValue("key");
			}		
		}
		public void startPrefixMapping(String arg0, String arg1) throws SAXException {}

		public String getGraphType()
		{
			return graphtype;
		}
		public TreeMap<String,String> getKeys()
		{
			return keys;
		}
	}

	private boolean error = true; //Ist true falls (a) kein File gecheckt wurde (Init State) oder beim Check ein Fehler auftrat
	File f = null;
	String GraphType ="";
	GravelMLContentHandler ggMLCH;
	/**
	 * 
	 * @return eine Fehlermeldung als String, falls ein vorliegt, sonst "", dann sind alle Keys da und der typ stimmt
	 */
	public String checkGraph(File pFile)
	{
		f = pFile;
		if (!f.exists())
		{
			error = true;
			return "Die Datei '"+f.getName()+"' existiert nicht";
		}
		typeExtractor checker = new typeExtractor();
		XMLReader parser;
		try {
			parser = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
		}
		catch (SAXException e) 
		{
			try {
				//System.err.println("bla");
				//Std System-Parser holen
				parser = XMLReaderFactory.createXMLReader();
			}
			catch (SAXException e2) 
			{
				error = true;
				return "Kein XML-Parser verfügbar!";
			}
		}
		try {
			InputStream in = new FileInputStream(f);
			InputSource input = new InputSource(in);
			input.setSystemId(f.getAbsolutePath());
			parser.setContentHandler(checker);
			parser.parse(input);
		}
		catch (Exception e2)
		{
			error = true;
			e2.printStackTrace();
			return "Beim parsen ist ein Fehler aufgetreten: <br>"+e2.getMessage();
		}
		//Typ holen, und Keys prüfen
		GraphType = checker.getGraphType();
		if (GraphType.equals("visual"))
		{
			error = !checkAllKeys(checker.getKeys());
			if (error)
			{
				return "Es fehlen Datenschlüssel";		
			}
			return "";
		}
		else if (GraphType.equals("math"))
		{
			error = !checkMathKeys(checker.getKeys());
			if (error)
			{
				return "Es fehlt mindestens einer der 3 Data Keys";
			}
			return "";
		}
		else //keiner der beiden Typen liegt vor
		{
			error = true;
			return "Unbekannter Graphentyp";
		}
	}
	/**
	 * Prüft, ob im Check alle wichtigen GraphML Keys gelesen worden sind, die ein visual Graph benötigt.
	 * 
	 * @param k
	 * @return
	 */
	private boolean checkAllKeys(TreeMap<String,String> k)
	{
		//Edge Name and
		if (!checkMathKeys(k))
			return false;
		
		//Edge Text Properties is not neccessarily needed in a File
		if (k.get("edge.etv")==null)
			k.put("edge.etv","boolean"); //non existent -> add
		else if (!k.get("edge.etv").equals("boolean")) //existent but not boolean -> wrong
			return false;
		if (k.get("edge.etw")==null)
			k.put("edge.etw","boolean"); //non existent -> add
		else if (!k.get("edge.etw").equals("boolean")) //existent but not boolean -> wrong
			return false;
		
		if (k.get("edge.etd")==null)
			k.put("edge.etd","integer"); //non existent -> add
		else if (!k.get("edge.etd").equals("integer")) //existent but not integer -> wrong
			return false;
		if (k.get("edge.etp")==null)
			k.put("edge.etp","integer"); //non existent -> add
		else if (!k.get("edge.etp").equals("integer")) //existent but not integer -> wrong
			return false;
		if (k.get("edge.ets")==null)
			k.put("edge.ets","integer"); //non existent -> add
		else if (!k.get("edge.ets").equals("integer")) //existent but not integer -> wrong
			return false;
		
		//Edge Line Style, can be left out in a file - use standard then
		if (k.get("edge.eld")==null)
			k.put("edge.eld","integer"); //non existent -> add
		if (k.get("edge.ell")==null)
			k.put("edge.ell","integer"); //non existent -> add
		if (k.get("edge.elt")==null)
			k.put("edge.elt","integer"); //non existent -> add
		
		//Edge Line Style, can be left out in a file - use standard then
		if (k.get("graph.gl")!=null) //loop file possible
		{
			if (k.get("edge.elod")==null)
				return false;
			if (k.get("edge.elop")==null)
				return false;
			if (k.get("edge.elol")==null)
				return false;
			if (k.get("edge.eloc")==null)
				return false;
		}

		
		if ((k.get("node.nx")==null)||(!(k.get("node.nx").equals("integer"))))
			return false;
		if ((k.get("node.ny")==null)||(!(k.get("node.ny").equals("integer"))))
			return false;
		if ((k.get("node.ns")==null)||(!(k.get("node.ns").equals("integer"))))
			return false;
		if ((k.get("node.nd")==null)||(!(k.get("node.nd").equals("integer"))))
			return false;
		if ((k.get("node.nr")==null)||(!(k.get("node.nr").equals("integer"))))
			return false;
		if ((k.get("node.nns")==null)||(!(k.get("node.nns").equals("integer"))))
			return false;
		if ((k.get("node.nnv")==null)||(!(k.get("node.nnv").equals("boolean"))))
			return false;
		
		if ((k.get("edge.ew")==null)||(!(k.get("edge.ew").equals("integer"))))
			return false;
		if ((k.get("edge.es")==null)||(!(k.get("edge.es").equals("float"))))
			return false;
		if ((k.get("edge.ep")==null)||(!(k.get("edge.ep").equals("float"))))
			return false;
		if ((k.get("edge.ea")==null)||(!(k.get("edge.ea").equals("float"))))
			return false;
		if ((k.get("edge.eapos")==null)||(!(k.get("edge.eapos").equals("float"))))
		{
			k.put("edge.eapos","float"); //to fit older versions
		}
		if ((k.get("edge.et")==null)||(!(k.get("edge.et").equals("string"))))
			return false;
		if ((k.get("edge.ex")==null)||(!(k.get("edge.ex").equals("integer"))))
			return false;
		if ((k.get("edge.ey")==null)||(!(k.get("edge.ey").equals("integer"))))
			return false;
		if ((k.get("edge.eo")==null)||(!(k.get("edge.eo").equals("boolean"))))
			return false;
		
		if ((k.get("subset.sr")==null)||(!(k.get("subset.sr").equals("integer"))))
			return false;
		if ((k.get("subset.sg")==null)||(!(k.get("subset.sg").equals("integer"))))
			return false;
		if ((k.get("subset.sb")==null)||(!(k.get("subset.sb").equals("integer"))))
			return false;
		
		
		return true;
	}
	private boolean checkMathKeys(TreeMap<String,String> k)
	{
		if ((k.get("edge.ev")==null)||(!(k.get("edge.ev").equals("integer"))))
			return false;
		if ((k.get("node.nn")==null)||(!(k.get("node.nn").equals("string"))))
			return false;
		if ((k.get("subset.sn")==null)||(!(k.get("subset.sn").equals("string"))))
			return false;
		//edge name is not necassarily needed but if it exists it must be string
		if (k.get("edge.name")==null)
			k.put("edge.name","string"); //non existent -> add
		else if (!k.get("edge.name").equals("string")) //existent but not boolean -> wrong
			return false;
		
		return true;
	}

	/**
	 * 
	 * @return einen Status, der leer ist, wenn alles geklappt hat
	 */
	public String readGraph()
	{
		if ((f==null)||(error)) //Kein File oder es liegt ein Fehler vor
			return "Keine oder eine fehlerhafte Datei angegeben";
		
		ggMLCH = new GravelMLContentHandler(GraphType);
		
		XMLReader parser;
		try {
			parser = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
		}
		catch (SAXException e) 
		{
			try {
				//System.err.println("bla");
				//Std System-Parser holen
				parser = XMLReaderFactory.createXMLReader();
			}
			catch (SAXException e2) 
			{
				error = true;
				return "es liegt kein Parser vor.";
			}
		}
		try {
			//Parse Nodes
			ggMLCH.setStatus(GravelMLContentHandler.PARSE_NODES);
			InputStream in = new FileInputStream(f);
			InputSource input = new InputSource(in);
			input.setSystemId(f.getAbsolutePath());
			parser.setContentHandler(ggMLCH);
			parser.parse(input);

			if (!ggMLCH.isValid())
				return "Bei den Knoten ist ein Fehler aufgetreten";
			else
				ggMLCH.setStatus(GravelMLContentHandler.PARSE_EDGES);

			//Parse Edges
			in = new FileInputStream(f);
			input = new InputSource(in);
			input.setSystemId(f.getAbsolutePath());
			parser.setContentHandler(ggMLCH);
			parser.parse(input);

			if (!ggMLCH.isValid())
				return "Bei den Kanten ist ein Fehler aufgetreten";
			else
				ggMLCH.setStatus(GravelMLContentHandler.PARSE_SUBSETS);

			//Parse Subsets
			in = new FileInputStream(f);
			input = new InputSource(in);
			input.setSystemId(f.getAbsolutePath());
			parser.setContentHandler(ggMLCH);
			parser.parse(input);

			if (!ggMLCH.isValid())
				return "Bei den Untergraphen ist ein Fehler aufgetreten";
			else
				ggMLCH.setStatus(GravelMLContentHandler.PARSE_DONE);
			
			return"";
		}
		catch (Exception e2)
		{
			error = true;
			e2.printStackTrace();
			return "Beim Parsen der Datei "+f.getName()+" ist ein Fehler aufgetreten "+e2.getLocalizedMessage();
		}
	}

	public String getGraphType()
	{
		return GraphType;
	}
	public VGraph getVGraph()
	{
		if (ggMLCH!=null)
		{
			if ((ggMLCH.isValid())&&(ggMLCH.getStatus()==GravelMLContentHandler.PARSE_DONE)&&(ggMLCH.isVisual()))
				return ggMLCH.getVGraph();
			else 
				return null;				
		}
		return null;
	}
	public MGraph getMGraph()
	{
		if (ggMLCH!=null)
		{
			if ((ggMLCH.isValid())&&(ggMLCH.getStatus()==GravelMLContentHandler.PARSE_DONE)&&(!ggMLCH.isVisual()))
				return ggMLCH.getMGraph();
			else 
				return null;				
		}
		return null;
	}
}

