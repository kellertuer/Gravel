package io;

import java.awt.Color;
import java.awt.Point;
import java.util.Stack;
import java.util.TreeMap;
import java.util.Vector;

import model.*;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * Eine Klasse, welche die Gravel-Variation von GraphML einliest, wobei der Typ bekannt sein muss
 * 
 * Der Typ ist entweder math oder visual
 * 
 * @author Ronny Bergmann
 * @since 0.2
 */
public class GravelMLContentHandler implements ContentHandler
{
	public static final int PARSE_NODES = 1;
	public static final int PARSE_EDGES = 2;
	public static final int PARSE_SUBGRAPHS = 3;
	public static final int PARSE_DONE = 4;
	
	private boolean isVisual; //VisualGraph ?
	private boolean isValid; //Still Valid XML ?
	private String position=""; //complete Nameprefix as String;
	private Stack<String> path; //Nameprefix in a Stack
	private GeneralPreferences gp;
	private TreeMap<String,Integer> NodeIdtoIndex, EdgeIdtoIndex; //For finding Nodes and Edges in the building Graph
	private int Status;
	//temporary variables for extracting values
	private VGraph vG= null;
	private MGraph mG= null;
	
	private int id=-1;
	private String CDATA="", idString="";
	private String data_key="", gpKey="", type="", def=""; //For any key
	//Node-Values in data
	private int nx=0,ny=0,ns=0,nd=0,nr=0,nns=0;
	private boolean nnv;
	private String nn,en;
	
	//Edge Values in Attributes
	int start=0,ende=0;
	
	//Edge Values in cdata
	int ev=0,ew=0,elol,elod; double elop;
	float es=0.0f,ep=0.0f,ea=0.0f, eapos=-1.0f; //Position is if not given 1.0f
	String et="";
	int ex=-1,ey=-1;
	boolean eo=false,eloc=true;
	VEdgeText etxt=new VEdgeText();
	VEdgeLinestyle eline= new VEdgeLinestyle();
	Vector<Point> edgepath;
	public GravelMLContentHandler(String gType)
	{
		isVisual = (gType.equals("visual"));
		isValid = true;
		gp = GeneralPreferences.getInstance();
		path = new Stack<String>();
		Status = PARSE_NODES;
		NodeIdtoIndex = new TreeMap<String,Integer>();
		EdgeIdtoIndex = new TreeMap<String,Integer>();
		edgepath = new Vector<Point>();
	}
		
	
	//Subgraph Values 
	String sn="";
	int sr=0,sg=0,sb=0; //Color Values
	
	//General Parser Functions
	
	public void characters(char[] text, int start, int length) throws SAXException 
	{
		String val = "";
		for (int i=0; i<length; i++)
			val += text[start+i];
		if (position.endsWith("key.default"))
			def = val;
		else
			CDATA += val;
	}
	public void endDocument() throws SAXException 
	{
		if (!isValid)
			return;
		if (Status==PARSE_NODES) //Just parsed all nodes an std keys
		{
			if (Float.isNaN(gp.getFloatValue("edge.arrow_pos"))) //not found
				gp.setFloatValue("edge.arrow_pos",1.0f); //Set again, because this value was new
		}
		if (Status==PARSE_EDGES)
		{
			gp.removeStringValue("edge.edgetype");
		}
	}
	public void endElement(String namespaceURI, String localName,String qualifiedName) throws SAXException 
	{
		String verlassen = path.pop();
		if (verlassen!=localName) //Ein Element nicht abgeschlosen
				throw new SAXException("ERROR! Tag nicht geschlossen");
		if (position.equals(verlassen))
			position = "";
		else //sonst den Punkt auch entfernen
			position = position.substring(0, position.length()-verlassen.length()-1);	
		if (localName.equals("data"))
		{
			if ((position.equals("graphml.graph.subset"))||(position.equals("graphml.graph"))||(position.equals("graphml.graph.node"))||(position.equals("graphml.graph.edge")))
				EndElementData();
		}
		else if (Status==PARSE_NODES)
		{
			EndElementNode(localName);
		}
		else if (Status==PARSE_EDGES)
		{
			EndElementEdge(localName);
		}
		else if (Status==PARSE_SUBGRAPHS)
		{
			EndElementSubgraph(localName);
		}
	}
	public void endPrefixMapping(String arg0) throws SAXException {}
	public void ignorableWhitespace(char[] arg0, int arg1, int arg2) throws SAXException {}
	public void processingInstruction(String arg0, String arg1) throws SAXException {}
	public void setDocumentLocator(Locator arg0) {}
	public void skippedEntity(String arg0) throws SAXException {}
	public void startDocument() throws SAXException 
	{
		if (Status==PARSE_EDGES)
		{
			if (isVisual)
			{	if ((vG==null)||(vG.getMathGraph().modifyNodes.cardinality()==0))
						{isValid=false; main.DEBUG.println(main.DEBUG.MIDDLE,"No VGraph or no Nodes existent. Can't Parse Edges"); return;}
			} 
			else if (mG==null)
			{isValid=false; main.DEBUG.println(main.DEBUG.MIDDLE,"No MGraph existent. Can't Parse Edges"); return;}
		}		
		else if (Status==PARSE_SUBGRAPHS)
		{
			if (isVisual)
			{	if ((vG==null))
						{isValid=false; main.DEBUG.println(main.DEBUG.MIDDLE,"No VGraph existent. Can't Parse Subgraphs/SubSets"); return;}
			} 
			else if (mG==null)
				{isValid=false; main.DEBUG.println(main.DEBUG.MIDDLE,"No MGraph existent. Can't Parse Subgraphs/SubSets"); return;}
		}
		
	}
	public void startElement(String namespaceURI, String localName,String qualifiedName, Attributes atts)
	{
		path.push(localName);
		if (position.equals(""))
			position = localName;
		else
			position += "."+localName;
		if (localName.equals("hyperegde"))
		{
			main.DEBUG.println(main.DEBUG.LOW,"Old file loader can't handle hypergraphs.");
		}
		else if (Status==PARSE_NODES)
		{
			startElementNode(atts);
		}
		else if (Status==PARSE_EDGES)
		{
			startElementEdge(atts);
		}
		else if (Status==PARSE_SUBGRAPHS)
		{
			startElementSubgraph(atts);
		}

	}
	public void startPrefixMapping(String arg0, String arg1) throws SAXException {}

	//jeden Durchganz
	private void EndElementData()
	{
		if (Status==PARSE_NODES)
		{
			//Knotendatenfelder
			if (data_key.equals("nn")) //Node Name
				nn = CDATA;
			else if (data_key.equals("nx")) //Node Position X
				try {nx = Integer.parseInt(CDATA);} catch(Exception e) {nx=0;}
			else if (data_key.equals("ny")) //Node Position Y
				try {ny = Integer.parseInt(CDATA);} catch(Exception e) {ny=0;}
			else if (data_key.equals("ns")) //Node Size
				try {ns = Integer.parseInt(CDATA);} catch(Exception e) {ns=-1;}
			else if (data_key.equals("nd")) //Node Name Distance 
				try {nd = Integer.parseInt(CDATA);} catch(Exception e) {nd=-1;}
			else if (data_key.equals("nr")) //Node Name Orientation 
				try {nr = Integer.parseInt(CDATA);} catch(Exception e) {nr=-1;}
			else if (data_key.equals("nns")) //None Name Size
				try {nns = Integer.parseInt(CDATA);} catch(Exception e) {nns=-1;}
			else if (data_key.equals("nnv")) //Node Name Visible
				try {nnv = Boolean.parseBoolean(CDATA);} catch(Exception e) {nnv=false;}
		} else
		if (Status==PARSE_EDGES)
		{
			//Kanten datenfelder	
			if (data_key.equals("ev")) //Edge Value
				try {ev = Integer.parseInt(CDATA);} catch(Exception e) {ev=0;}
			if (data_key.equals("ew")) //Edge Width
				try {ew = Integer.parseInt(CDATA);} catch(Exception e) {ew=0;}
			if (data_key.equals("en")) //Edge Name
				en = CDATA; //Edge Name is a String save it as normal 				
			if (data_key.equals("etd")) //Edge Text Distance
				try {etxt.setDistance(Integer.parseInt(CDATA));} catch(Exception e) {etxt.setDistance(gp.getIntValue("edge.text_distance"));}
			if (data_key.equals("etp")) //Edge Text Position
				try {etxt.setPosition((float)Integer.parseInt(CDATA)/100f);} catch(Exception e) {etxt.setPosition(gp.getFloatValue("edge.text_position"));}
			if (data_key.equals("ets")) //Edge Text Size
				try {etxt.setSize(Integer.parseInt(CDATA));} catch(Exception e) {etxt.setSize(gp.getIntValue("edge.text_size"));}
			if (data_key.equals("etv")) //Edge Text visible
				try {etxt.setVisible(Boolean.parseBoolean(CDATA));} catch(Exception e) {etxt.setVisible(gp.getBoolValue("edge.text_visible"));}
			if (data_key.equals("etw")) //Edge Text Show Value or text
				try {etxt.setshowvalue(Boolean.parseBoolean(CDATA));} catch(Exception e) {etxt.setshowvalue(gp.getBoolValue("edge.text_showvalue"));}
			if (data_key.equals("eld")) //Edge Line Distance
				try {eline.setDistance(Integer.parseInt(CDATA));} catch(Exception e) {eline.setDistance(gp.getIntValue("edge.line_distance"));}
			if (data_key.equals("ell")) //Edge Line Length
				try {eline.setLength(Integer.parseInt(CDATA));} catch(Exception e) {eline.setLength(gp.getIntValue("edge.line_distance"));}
			if (data_key.equals("elt")) //Edge Line Type
				try {eline.setType(Integer.parseInt(CDATA));} catch(Exception e) {eline.setType(gp.getIntValue("edge.line_type"));}
			
			if (data_key.equals("elol")) //Edge Loop Length
				try {elol = Integer.parseInt(CDATA);} catch(Exception e) {elol = gp.getIntValue("edge.loop_length");}
			if (data_key.equals("elod")) //Edge Loop Direction
				try {elod = Integer.parseInt(CDATA);} catch(Exception e) {elod = gp.getIntValue("edge.loop_direction");}
			if (data_key.equals("elop")) //Edge Loop Proportion
				try {elop = Double.parseDouble(CDATA);} catch(Exception e) {elop = ((double)gp.getIntValue("edge.loop_proportion"))/100.0d;}
			if (data_key.equals("eloc")) //Edge Loop Clockwise
				try {eloc = Boolean.parseBoolean(CDATA);} catch(Exception e) {eloc = gp.getBoolValue("edge.loop_clockwise");}
				
				
				
			if (data_key.equals("es")) //Edge Arrow Size
				try {es = Float.parseFloat(CDATA);} catch(Exception e) {es=0.0f;}
			if (data_key.equals("ep")) //Edge Arrow Part
				try {ep = Float.parseFloat(CDATA);} catch(Exception e) {ep=0.0f;}
			if (data_key.equals("eapos")) //Edge Arrow Part
				try {eapos = Float.parseFloat(CDATA);} catch(Exception e) {eapos=-1.0f;}
			if (data_key.equals("ea")) //Edge Arrow Alpha
				try {ea = Float.parseFloat(CDATA);} catch(Exception e) {ea=0.0f;}
			if (data_key.equals("et")) //Edge Type
				et = CDATA;
			if (data_key.equals("ex")) //Edge ControlPoint X
				try {ex = Integer.parseInt(CDATA);} catch(Exception e) {ex=-1;}
			if (data_key.equals("ey")) //Edge ControlPoint X
				try {ey = Integer.parseInt(CDATA);} catch(Exception e) {ey=-1;}
			else if (data_key.equals("eo")) //Node Name Visible
				try {eo = Boolean.parseBoolean(CDATA);} catch(Exception e) {eo=gp.getBoolValue("edge.oorth_verticalfirst");}
				if ((ex!=-1)&&(ey!=-1))
				{	//On ex and ey the order is important !!
					edgepath.add(new Point(ex,ey)); ex=-1; ey=-1;
				}
		}
		else if (Status==PARSE_SUBGRAPHS)
		{
			//Subgraph-Felder
			if (data_key.equals("sn")) //SubgraphName
			{
				if (isVisual)
					vG.getMathGraph().modifySubgraphs.get(id).setName(CDATA);
				else
					mG.modifySubgraphs.get(id).setName(CDATA);
			}
			if (!isVisual)
				return;
			if (data_key.equals("sr")) //Edge ControlPoint X
				try {sr = Integer.parseInt(CDATA);} catch(Exception e) {sr=0;}
			if (sr>255) sr%=256; 
			if (data_key.equals("sg")) //Edge ControlPoint X
				try {sg = Integer.parseInt(CDATA);} catch(Exception e) {sg=0;}
			if (sg>255) sg%=256;
			if (data_key.equals("sb")) //Edge ControlPoint X
				try {sb = Integer.parseInt(CDATA);} catch(Exception e) {sb=0;}
			if (sb>255) sb%=256;
			if ((sr!=0)||(sg!=0)||(sb!=0))
			{
				vG.modifySubgraphs.setColor(id, new Color(sr,sg,sb));
			}
		}
		data_key="";
		CDATA="";
	}
	

	//==PARSER DURCHGANG für KNOTEN

	private void startElementNode(Attributes atts)
	{
		if (position.equals("graphml.key")) //Normal Key (perhaps) with default value
		{
			gpKey = atts.getValue("for")+"."+atts.getValue("attr.name");
			type = atts.getValue("attr.type");
			def = "";
		}
		else if (position.equals("graphml.graph")) //Beginning Graph
		{
			if (atts.getValue("edgedefault").equals("undirected")) //ungerichtet als Std setzen
					gp.setBoolValue("graph.directed", false);
				else if (atts.getValue("edgedefault").equals("directed")) //gerichtet als Std setzen
					gp.setBoolValue("graph.directed", true);
				//sonst lassen wie s is
				if (isVisual)
					vG = new VGraph(gp.getBoolValue("graph.directed"),gp.getBoolValue("graph.allowloops"),gp.getBoolValue("graph.allowmultiple"));
				else
					mG = new MGraph(gp.getBoolValue("graph.directed"),gp.getBoolValue("graph.allowloops"),gp.getBoolValue("graph.allowmultiple"));
		}
		else if (position.equals("graphml.graph.node"))
		{
			//Auf leer zurück.
			nn=""; nx=0;ny=0; ns=0; id=0; nd=-1;nr=-1;nns=-1; nnv = gp.getBoolValue("node.name_visible");
			//Std-Fall id = node$NUMBER
			try {
				id = Integer.parseInt(atts.getValue("id").substring(4));
			}
			catch (Exception e)
			{
				if (isVisual)
					id = vG.getMathGraph().modifyNodes.getNextIndex();
				else
					id = mG.modifyNodes.getNextIndex();
				main.DEBUG.println(main.DEBUG.MIDDLE,"Malformed ID - generating own ("+id+")");
			}				
			idString = atts.getValue("id");
		}
		else if (position.equals("graphml.graph.node.data"))
		{
			data_key = atts.getValue("key");
		}
	}
	private void EndElementNode(String localName)
	{
		//		==KEY ENDED==
		if ((localName.equals("key"))&&(!gpKey.equals("graph.type"))) // Key und es ist nicht der Type
		{	
			if (!def.equals("")) //ein Default existiert...setze diesen in gp ausnahmen subgraphname nodename
			{
				if (type.equals("string"))
					gp.setStringValue(gpKey,def);
				else if (type.equals("integer"))
					gp.setIntValue(gpKey,(new Integer(def)).intValue());
				else if (type.equals("boolean"))
					gp.setBoolValue(gpKey,(new Boolean(def)).booleanValue());
				else if (type.equals("float"))
				{
					float fval = (new Float(def)).floatValue();
					if (fval<=1.0f)
						fval *= 100.0f;
					gp.setIntValue(gpKey,Math.round(fval));
				}
				def="";
				gpKey="";
			}
		}	
		//==NODE Ended
		else if ((localName.equals("node"))&&(position.equals("graphml.graph"))) //Top Level Node
		{
			if ((idString.equals(""))||(id==0))
			{
				main.DEBUG.println(main.DEBUG.MIDDLE,"Node id invalid");
				isValid=false;
				return;
			}
			if (nn.equals("")) //kein Name angegeben, Standard nehmen
				nn = gp.getNodeName(id);
			if (isVisual) //Mit Grafik muss noch
			{
				if (ns<=0) //none found or parse error on Node Size
					ns = gp.getIntValue("node.size"); //StandardKnotengröße
				if (!((nx!=0)&&(ny!=0))) //Parse error or one of them not found
				{
					isValid=false; //keine position => Fehler
					return;
				}
				if (vG.modifyNodes.get(id)!=null)
				{
					isValid=false;
					return;
				}
				if (nd==-1) //parse error or none found => std value
					nd = gp.getIntValue("node.name_distance");
				if (nr==-1) //parse error or none found => std value
					nr = gp.getIntValue("node.name_rotation");
				if (nns==-1) //parse error or none found => std value
					nns = gp.getIntValue("node.name_size");
				vG.modifyNodes.add(new VNode(id,nx,ny,ns,nd,nr,nns,nnv), new MNode(id,nn));
				this.NodeIdtoIndex.put(idString, id);
			}
			else //im Mathgraph reicht der Name schon
			{
				if (mG.modifyNodes.get(id)!=null)
				{
					isValid=false;
					return;
				}
				mG.modifyNodes.add(new MNode(id,nn));
				this.NodeIdtoIndex.put(idString, id);					
			}
		}
			
	}

	//==PARSER DURCHGANG für KANTEN
	
	private void startElementEdge(Attributes atts)
	{
		if (position.equals("graphml.graph.edge")) //Top-Edge
		{
			//Nodes are all done so get edgestart and edgeendindex
			start = NodeIdtoIndex.get(atts.getValue("source"));
			ende = NodeIdtoIndex.get(atts.getValue("target"));
			//Reset all Values
			ev=0;ew=0;es=0.0f;ep=0.0f;eapos=-1.0f;ea=0.0f;et="";id=0;ex=-1;ey=-1;eo=false;en="";
			elol = 0; elop = 0.0d; elod = 0; eloc = gp.getBoolValue("edge.loop_clockwise");
			etxt = new VEdgeText(); //Std Values
			eline = new VEdgeLinestyle(); //Back to Std
			edgepath=new Vector<Point>();
			try {
				id = Integer.parseInt(atts.getValue("id").substring(4));
			}
			catch (Exception e)
			{
				if (isVisual)
					id = vG.getMathGraph().modifyEdges.getNextIndex();
				else
					id = mG.modifyEdges.getNextIndex();
			}				
			idString = atts.getValue("id");
			edgepath = new Vector<Point>();
		}
		else if (position.equals("graphml.graph.edge.data"))
		{
			data_key = atts.getValue("key");
		}
	}
	private void EndElementEdge(String localName)
	{
		//		==EDGE ENDED==
		if ((localName.equals("edge"))&&(position.equals("graphml.graph"))) //Top Level Edge
		{
			//Check Values
			if ((start<=0)||(ende<=0))
			{
				isValid=false;
				return;
			}
			if (ev==0)
				ev = gp.getIntValue("edge.value");
			if (!isVisual) //MathGraph
			{
				MEdge newedge = new MEdge(id,start,ende,ev,en);
				isValid = mG.modifyEdges.add(newedge);
				if (!isValid)
					return;
			}
			if (ew==0)   ew = gp.getIntValue("edge.width");
			if ((!et.equals("StraightLine"))&&(!et.equals("Orthogonal"))&&(!et.equals("QuadCurve"))&&(!et.equals("Segmented"))&&(!et.equals("Loop")))
					et = gp.getStringValue("edge.edgetype");
			if (start==ende)
			{
				if (!vG.getMathGraph().isLoopAllowed())
				{
					isValid=false;
					return;
				}
				else if (!et.equals("Loop"))
				{
					isValid=false;
					return;
				}
			}
			else if (et.equals("Loop")) //start!=ende
			{
				isValid=false;
				return;				
			}
			VEdge toAdd;
			if (et.equals("StraightLine"))
				toAdd= new VStraightLineEdge(id,ew);
			else if (et.equals("Orthogonal"))
				toAdd = new VOrthogonalEdge(id,ew,eo);
			else if (et.equals("QuadCurve"))
			{
				if (!edgepath.isEmpty())
					toAdd = new VQuadCurveEdge(id,ew,edgepath.firstElement());
				else
				{isValid=false; return;}
			}
			else if (et.equals("Segmented"))
			{
				if (!edgepath.isEmpty())
					toAdd = new VSegmentedEdge(id,ew,edgepath);
				else
				{isValid=false; return;	}	
			}
			else if (et.equals("Loop"))
			{
				if (elol==0)
					elol = gp.getIntValue("edge.loop_length");
				if (elod==0)
					elod = gp.getIntValue("edge.loop_direction");
				if (elop==0.0d)
					elop = ((double)gp.getIntValue("edge.loop_proportion"))/100.0d;
				toAdd = new VLoopEdge(id,ew,elol,elod,elop,eloc);
			}
			else
			{isValid=false; return;}	
			//Arrow INfos
			if (es==0.0f) es = (new Integer(gp.getIntValue("edge.arrow_size"))).floatValue();
			if ((ep==0.0f)||(ep>1.0f)) ep = gp.getFloatValue("edge.arrow_part");
			if (ea==0.0f) ea = (new Integer(gp.getIntValue("edge.arrow_alpha"))).floatValue();
			if ((eapos<0.0f)||(eapos>1.0f)) eapos = gp.getFloatValue("edge.arrow_pos");
			toAdd.setArrow(new VEdgeArrow(es,ep,ea,eapos));
			//huh just text left, but that one is initiated with default values and on error theyre alle set to default so text must be okay
			toAdd.setTextProperties(etxt);
			//and edge line style, also no checks, initiated with default, so every value missing is std
			toAdd.setLinestyle(eline);
			//Everythings okay, add the VEdge (puh, that was work!)
			//For multiple edges - similar exist ? (non multiple : edge between start end end ?)
			//Check for the visual Part
			if (vG.modifyEdges.getIndexWithSimilarEdgePath(toAdd, start, ende) > 0) //old : vG.getEdgeIndices(start, ende)!=-1) 
			{ isValid= false; return; }
			vG.modifyEdges.add(
					toAdd,
					new MEdge(toAdd.getIndex(),start,ende,ev,en),
					vG.modifyNodes.get(start).getPosition(),
					vG.modifyNodes.get(ende).getPosition());
			this.EdgeIdtoIndex.put(idString,id);
		}
	}
	
	
	private void startElementSubgraph(Attributes atts)
	{
		if (position.equals("graphml.graph.subset")) //Top-Subgraph
		{
			//Reset Values
			sn="";sr=0;sg=0;sb=0; //Color Values
			try {
				id = Integer.parseInt(atts.getValue("id").substring(6));
				}
			catch (Exception e)
			{
				if (isVisual)
					id = vG.getMathGraph().modifySubgraphs.getNextIndex();
				else
					id = mG.modifySubgraphs.getNextIndex();
			}				
			idString = atts.getValue("id");
			//Sonderfall, hier mal zuerst erstellen, da kein andres Subgraph schwarz ist...dies hier schwarz machen, bis eine Farbe eintrudelt
			VSubgraph vs = new VSubgraph(id,Color.BLACK);
			MSubgraph ms = new MSubgraph(id,gp.getSubgraphName(id));
			if (isVisual)
				vG.modifySubgraphs.add(vs, ms);
			else
				mG.modifySubgraphs.add(ms);
		}
		else if (position.equals("graphml.graph.subset.snode"))
		{
			String nodeid = atts.getValue("node");
			int nodeindex = -1;
			if (NodeIdtoIndex.get(nodeid)==null)
				{isValid=false; return;}
			nodeindex = NodeIdtoIndex.get(nodeid);
			
			if (isVisual)
			{
				if (vG.modifyNodes.get(nodeindex)!=null)
					vG.modifySubgraphs.addNodetoSubgraph(nodeindex, id);
				else
					{isValid=false; return;}
			}
			else
			{
				if (mG.modifyNodes.get(nodeindex)!=null)
					mG.modifySubgraphs.addNodetoSubgraph(nodeindex, id);
				else
					{isValid=false; return;}
			}
		}
		else if (position.equals("graphml.graph.subset.sedge"))
		{
			String edgeid = atts.getValue("edge");
			int edgeindex = -1;
			if (EdgeIdtoIndex.get(edgeid)==null)
			{isValid=false; return;}
			edgeindex = EdgeIdtoIndex.get(edgeid);
			
			if (isVisual)
			{
				if (vG.modifyEdges.get(edgeindex)!=null)
					vG.modifySubgraphs.addEdgetoSubgraph(edgeindex, id);
				else
					{isValid=false; return;}
			}
			else
			{
				if (mG.modifyEdges.get(edgeindex).Value!=-1)
					vG.modifySubgraphs.addEdgetoSubgraph(edgeindex, id);
				else
					{isValid=false; return;}
			}
		}
		else if (position.equals("graphml.graph.subset.data"))
		{
			data_key = atts.getValue("key");
		}
	}
	private void EndElementSubgraph(String localName)
	{
		if ((localName.equals("subset"))&&(position.equals("graphml.graph"))) //Top Level Node
		{
			if (isVisual)
			{
				if (vG.modifySubgraphs.get(id).getColor()==Color.BLACK)
				{
					vG.modifySubgraphs.remove(id);
					isValid=false;
					return;
				}
			}
		}
		//In an MGraph is nothing to check.
	}

	public boolean isValid()
	{
		return isValid;
	}
	public void setStatus(int i)
	{
		Status = i;
	}
	public int getStatus()
	{
		return Status;
	}
	public boolean isVisual()
	{
		return isVisual;
	}
	public VGraph getVGraph()
	{
		return vG;	
	}
	public MGraph getMGraph()
	{
		return mG;
	}
}