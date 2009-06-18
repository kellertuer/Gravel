package io;

import model.*;

import java.awt.Point;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.Vector;

/**
 * This Class provides a simple Writer for Graphs to a GravelML-XML-File
 * based on and extending GraphML, though backward compatible to the std-GraphML-Stuff
 * 
 * @author Ronny Bergmann
 * @since 0.2
 */
public class GraphMLWriter {

	private final static String nl = "\r\n";
	VGraph vg=null;
	VHyperGraph vhg=null;
	GeneralPreferences gp = GeneralPreferences.getInstance();
	
	public GraphMLWriter(VGraph a_graph)
	{
		vg = a_graph;
	}

	public GraphMLWriter(VHyperGraph a_hypergraph)
	{
		vhg = a_hypergraph;
	}

	/**
	 * Write the Header containing comments, standard values and so on. For Math and Visual Graphs
	 */
	private void writeHeader(OutputStreamWriter s) throws IOException
	{
		s.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+nl+"");
		s.write("<!-- 	===GraphML-Gravel-Format==="+nl+"\t");
		s.write("\tThe GraphML is a XML-format from "+nl+"\t");
		s.write("graphml.graphdrawing.org "+nl+"\t");
		s.write("pusblished under CC-BY,"+nl+"\t");
				
		s.write("<graphml xmlns=\"http://gravel.darkmoonwolf.de/xmlns\""+nl+  
            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance"+nl+
            "xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns "+ 
            "                     http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd\">"+nl);
		s.write("\t <!-- Extensions of the Elements following the Key-Data-Paradigm of GraphML -->"+nl);
		s.write("\t\t<!-- Type of Graph represented here (no default value) - Values are math|math hyper|visual|visual hyper -->"+nl);
		s.write("\t<key id=\"graphtype\" for=\"graph\" attr.name=\"type\" attr.type=\"string\" />"+nl+nl);
		if (vg!=null)
		{
			s.write("\t\t<!-- Values for allowance of loops and multiple edges -->"+nl);
			s.write("\t<key id=\"graphloops\" for=\"graph\" attr.name=\"allowloops\" attr.type=\"boolean\">"+nl+
					"\t\t<default>"+gp.getBoolValue("graph.allowloops")+"</default>"+nl+"\t</key>"+nl);
			s.write("<key id=\"graphmultipleedges\" for=\"graph\" attr.name=\"allowmultiple\" attr.type=\"boolean\">"+nl+
					"\t\t<default>"+gp.getBoolValue("graph.allowmultiple")+"</default>"+nl+"\t</key>"+nl+nl);			
			s.write("\t\t<!-- Values for edges -->"+nl);
			s.write("<key id=\"edgevalue\" for=\"edge\" attr.name=\"edge.value\" attr.type=\"integer\">"+nl+
					"\t\t<default>"+gp.getIntValue("edge.value")+"</default>"+nl+"\t</key>"+nl);
			s.write("<key id=\"edgename\" for=\"edge\" attr.name=\"edge.name\" attr.type=\"string\">"+nl+
					"\t\t<default>"+gp.getStringValue("edge.name")+"</default>"+nl+"\t</key>"+nl);
		}
		else if (vhg!=null)
		{
			s.write("\t\t<!-- Values for hyperedges -->"+nl);
			s.write("\t<key id=\"hyperedgevalue\" for=\"hyperedge\" attr.name=\"hyperedge.value\" attr.type=\"integer\">"+nl
					+"\t\t<default>"+gp.getIntValue("hyperedge.value")+"</default>"+nl+"\t</key>");
			s.write("<key id=\"hyperedgename\" for=\"hyperedge\" attr.name=\"hyperedge.name\" attr.type=\"string\">"+nl+
					"\t\t<default>"+gp.getStringValue("hyperedge.name")+"</default>"+nl+"\t</key>"+nl);
		}
		s.write("\t\t<!-- Values for nodes -->"+nl);
		s.write("<key id=\"nodename\" for=\"node\" attr.name=\"node.name\" attr.type=\"string\">"+nl+
				"\t\t<default>"+gp.getStringValue("node.name")+"</default>"+nl+"\t</key>"+nl);
		s.write("\t\t<!-- One Value for all subgraph stuff -->"+nl);
		s.write("<key id=\"subgraph\" for=\"graph\" attr.name=\"subgraph\" atttr.type=\"graph.subgraph.type\">"+nl+
				"\t\t<default>"+nl+
				"\t\t\t<subgraph><name>"+gp.getStringValue("subgraph.name")+"</name></subgraph>"+nl+
				"\t\t</default>"+nl+"\t</key>"+nl);
	}
	/**
	 *  Write the additional values needed for visual graphs
	 * @param s
	 * @throws IOException
	 */
	private void writeVisualHeaderAddon(OutputStreamWriter s) throws IOException
	{
		s.write(nl+nl+"<!-- Graphel key Definitions for a (hyper)graph with visual information -->"+nl+nl);

		if (vg!=null) //Edge Stuff
		{
			s.write("\t\t<!-- Edge Details -->"+nl);
			s.write("\t<key id=\"edgearrow\" for=\"edge\" attr.name=\"edge.arrow\" attr.type=\"edge.arrow.type\">"+nl+
					"\t\t<default>"+nl+"\t\t\t<arrow"+
					" size="+gp.getIntValue("edge.arrow_size")+
					" part="+gp.getFloatValue("edge.arrow_part")+
					" position="+gp.getFloatValue("edge.arrow_pos")+
					" headalpha="+gp.getIntValue("edge.arrow_alpha")+"/>"+nl+
					"\t\t</default>"+nl+"</key"+nl);
			s.write("<key id=\"edgepoints\" for=\"edge\" attr.name=\"edge.points\" attr.type=\"edge.points.type\"/>"+nl);
			s.write("<key id=\"edgewidth\" for=\"edge\" attr.name=\"edge.width\" attr.type=\"integer\">"+nl+
					"\t\t<default>"+gp.getIntValue("edge.width")+"</default>"+nl+"\t</key>"+nl);
			s.write("\t<key id=\"edgetype\" for=\"edge\" attr.name=\"edge.type\" attr.type=\"string\"> <!-- Kantentyp (Orthogonal|QuadCurve|Segmented|StraightLine|)-->"+nl);
			s.write("\t\t<default>StraightLine</default>"+nl+"\t</key>"); //StraightLine ist immer Std !
			s.write("\t<key id=\"e_orthogonal\" for=\"edge\" attr.name=\"orthogonaledge_verticalfirst\" attr.type=\"boolean\"> <!--Nur fuer Orthogonal pflicht-->"+nl);
			s.write("\t\t<default>true</default>"+nl+"\t</key>"+nl);
			
			s.write("\t<key id=\"loopedge\" for=\"edge\" attr.name=\"edge.loop\" attr.type=\"edge.loop.type\">"+nl+
					"\t\t<default>"+nl+
					"\t\t\t<loopedge length="+gp.getIntValue("edge.loop_length")+
					" proportion="+gp.getIntValue("edge.loop_proportion")+
					" direction="+gp.getIntValue("edge.loop_direction")+
					" clockwise="+gp.getBoolValue("edge.loop_clockwise")+
					"/>"+nl+"\t\t</default>"+nl+"\t</key>"+nl);
			s.write("\t<key id=\"edgetext\" for=\"edge\" attr.name=\"edge.text\" attr.type=\"edge.text.type\">"+nl+
					"\t\t<default>"+nl+
					"\t\t\t<edgetext distance=\""+gp.getIntValue("edge.text_distance")+"\""+
					" position=\""+gp.getIntValue("edge.text_position")+"\""+
					" size=\""+gp.getIntValue("edge.text_size")+"\""+
					" show=");
			if (gp.getBoolValue("edge.text_showvalue"))
				s.write("\"value\"");
			else
				s.write("\"name\"");
			s.write(" visible=\""+gp.getBoolValue("edge.text_visible")+"/>"+nl);
			s.write("\t\t</default>"+nl+"\t</key>"+nl);
			
			s.write("\t<kley id=\"edgeline\" for\"edge\" attr.name=\"edge.line\" attr.type=\"edge.line.type\">"+nl+
					"\t\t<default>"+nl+
					"\t\t\t<edgeline length=\""+gp.getIntValue("edge.line_length")+"\""+
					" distance=\""+gp.getIntValue("edge.line_distance")+"\""+
					" type=\""+gp.getIntValue("edge.line_type")+"\"/>"+nl+
					"\t\t</default>"+nl+"\t</key>"+nl);
		}
		else
		{
			s.write("\t\t<!-- Hyperedge Details -->");
			s.write("\t<key id=\"hyperedgewidth\" for=\"edge\" attr.name=\"hyperedge.width\" attr.type=\"integer\">"+nl+
					"\t\t<default>"+gp.getIntValue("hyperedge.width")+"</default>"+nl+"\t</key>");
			s.write("\t<key id=\"hyperedgemargin\" for=\"hyperedge\" attr.name=\"hyperedge.margin\" attr.type=\"integer\">"+nl+
					"\t\t<default>"+gp.getIntValue("hyperedge.margin")+"</default>"+nl+"\t</key>");
			s.write("\t<key id=\"hyperedgetext\" for=\"hyperedge\" attr.name=\"hyperedge.text\" attr.type=\"edge.text.type\">"+nl+
					"\t\t<default>"+nl+
					"\t\t\t<hyperedgetext distance=\""+gp.getIntValue("edge.text_distance")+"\""+
					" position=\""+gp.getIntValue("edge.text_position")+"\""+
					" size=\""+gp.getIntValue("edge.text_size")+"\""+
					" show=");
			if (gp.getBoolValue("edge.text_showvalue"))
				s.write("\"value\"");
			else
				s.write("\"name\"");
			s.write(" visible="+gp.getBoolValue("edge.text_visible")+"/>"+nl);
			s.write("\t\t</default>"+nl+"\t</key>");
			s.write("\t<kley id=\"hyperedgeline\" for\"hyperedge\" attr.name=\"edge.line\" attr.type=\"edge.line.type\">"+nl+
					"\t\t<default>"+nl+
					"\t\t\t<edgeline length=\""+gp.getIntValue("edge.line_length")+"\""+
					" distance=\""+gp.getIntValue("edge.line_distance")+"\""+
					" type=\""+gp.getIntValue("edge.line_type")+"\"/>"+nl+
					"\t\t</default>"+nl+"\t</key>");
	
			s.write("<key id=\"hyperedgeshape\" for=\"hyperedge\" attr.name=\"hyperedge.shape\" attr.type=\"hyperedge.shape.type\"/>"+nl+nl);
		}
		s.write("\t\t<!-- Node Values -->"+nl);
		s.write("\t<key id=\"nodeform\" for=\"node\" attr.name=\"node\" attr.type=\"node.form.type\">+nl+" +
				"\t\t<default>"+nl+
				"\t\t\t<form type=\"Circle\" x=\"0\" y=\"0\" size=\""+gp.getIntValue("node.size")+"\"/>"+nl+
				"\t\t</default>"+nl+"\t</key>"+nl);
		s.write("\t<key id=\"nodetext\" for=\"node\" attr.name=\"node.text\" attr.type=\"node.text.type\">"+nl+
				"\t\t<default>"+nl+
				"\t\t\t<nodetext distance=\""+gp.getIntValue("node.name_distance")+"\""+
				" rotation=\""+gp.getIntValue("node.name_rotation")+"\" "+
				" size=\""+gp.getIntValue("node.name_size")+"\""+
				" visible=\""+gp.getBoolValue("node.name_visible")+"\"/>"+nl+
				"\t\t</default>"+nl+"\t</key>");
		//Subgraph stuff is not needed here because the one subgraph element does it all
	}
	/**
	 * Write additional Stuff needed for visual graphs in the footer
	 * @param s
	 * @throws IOException
	 */
	private void writeVisualFooterAddon(OutputStreamWriter s) throws IOException{}
	/**
	 * Write the general footer
	 * @param s
	 * @throws IOException
	 */
	private void writeFooter(OutputStreamWriter s) throws IOException
	{
		s.write(nl+"</graphml>");
	}
	/**
	 * Write each node to a visual file
	 * @param s
	 * @throws IOException
	 */
	private void writeVisualNodes(OutputStreamWriter s) throws IOException
	{
		MNodeSet mnodes; VNodeSet vnodes;
		if (vg!=null)
		{
			mnodes = vg.getMathGraph().modifyNodes;
			vnodes = vg.modifyNodes;
		}
		else if (vhg!=null)
		{
			mnodes = vhg.getMathGraph().modifyNodes;
			vnodes = vhg.modifyNodes;			
		}
		else
			throw new IOException("No Input given");
	    //Nodes
	    Iterator<VNode> nodeiter = vnodes.getIterator();
	    while (nodeiter.hasNext())
	    {
	    	VNode actual = nodeiter.next();
	    	s.write(nl+"\t\t<node id=\""+actual.getIndex()+"\">"+nl);
	    	//if the name is not a standard name
	    	if (!mnodes.get(actual.getIndex()).name.equals(gp.getNodeName(actual.getIndex())))
	    		s.write("\t\t\t<data key=\"nodename\">"+vg.getMathGraph().modifyNodes.get(actual.getIndex()).name+"</data>"+nl);
	    	//Position
	    	s.write("\t\t\t<data key=\"nodeform\"><form type=\"Circle\" x=\""+actual.getPosition().x+"\""+
	    			" y=\""+actual.getPosition().y+"");
	    	if (actual.getSize()!=gp.getIntValue("node.size"))
	    		s.write(" size=\""+actual.getSize()+"\"");
	    	s.write("/></data>"+nl);

	    	boolean noStdDist = (actual.getNameDistance()!=gp.getIntValue("node.name_distance")),
	    			noStdRot = (actual.getNameRotation()!=gp.getIntValue("node.name_rotation")),
	    			noStdSize = (actual.getNameSize()!=gp.getIntValue("node.name_size")),
	    			noStdVis =  (actual.isNameVisible()!=gp.getBoolValue("node.name_visible"));
	    	if (noStdDist||noStdRot||noStdSize) //Do we need a nodename element?
	    	{
	    		s.write("\t\t\t<data key=\"nodetext\"><nodetext");
	    		if (noStdDist)
	    			s.write(" distance=\""+actual.getNameDistance()+"\"");
	    		if (noStdRot)
	    			s.write(" rotation=\""+actual.getNameRotation()+"\"");
	    		if (noStdSize)
	    			s.write(" size=\""+actual.getNameSize()+"\"");
	    		if (noStdVis)
	      			s.write(" visible=\""+actual.isNameVisible()+"\"");
	    		s.write("/></data>"+nl); 
	    	}
	    	s.write("\t\t</node>"+nl);
	    }

	}
	/**
	 * Write the visual edges
	 * @param s
	 * @throws IOException
	 */
	private void writeVisualEdges(OutputStreamWriter s) throws IOException
	{
	       //Nodes
	       Iterator<VEdge> edgeiter = vg.modifyEdges.getIterator();
	       while (edgeiter.hasNext())
	       {
	    	   VEdge actual = edgeiter.next();
	    	   MEdge me = vg.getMathGraph().modifyEdges.get(actual.getIndex());
	    	   int start = me.StartIndex;
	    	   int ende = me.EndIndex;
	    	   int value = me.Value;
	    	   s.write(nl+"\t\t<edge id=\"edge"+actual.getIndex()+"\" source=\"node"+start+"\" target=\"node"+ende+"\">"+nl);
	    	   if (value!=gp.getIntValue("edge.value")) 	    	   //if the value is not std
	    		   s.write("\t\t\t<data key=\"ev\">"+value+"</data>"+nl);
	    	   if (actual.getWidth()!=gp.getIntValue("edge.width")) //if width is not std
	    		   s.write("\t\t\t<data key=\"ew\">"+actual.getWidth()+"</data>"+nl);
	    	   s.write("\t\t\t<data key=\"en\">"+vg.getMathGraph().modifyEdges.get(actual.getIndex()).name+"</data>"+nl);
	    	   if (actual.getArrow().getSize()!=((float)gp.getIntValue("edge.arrow_size"))) //if arrow_part is not std
	    		   s.write("\t\t\t<data key=\"es\">"+actual.getArrow().getSize()+"</data>"+nl);
	    	   if (actual.getArrow().getPart()!=gp.getFloatValue("edge.arrow_part")) //if arrow_part is not std
	    		   s.write("\t\t\t<data key=\"ep\">"+actual.getArrow().getPart()+"</data>"+nl);
	    	   if (actual.getArrow().getAngle()!=((float)gp.getIntValue("edge.arrow_alpha"))) //if arrow_angle is not std
	    		   s.write("\t\t\t<data key=\"ea\">"+actual.getArrow().getAngle()+"</data>"+nl);
	    	   if (actual.getArrow().getPos()!=gp.getFloatValue("edge.arrow_pos")) //if arrow_pos is not std
	    		   s.write("\t\t\t<data key=\"eapos\">"+actual.getArrow().getPos()+"</data>"+nl);
	    	   
	    	   if (actual.getEdgeType()==VEdge.ORTHOGONAL)
	    	   {
	    		   s.write("\t\t\t<data key=\"et\">Orthogonal</data>"+nl);
	    		   if (((VOrthogonalEdge)actual).getVerticalFirst()!=gp.getBoolValue("edge.orth_verticalfirst")) //non standard Orth Edge
	    			   s.write("\t\t\t<data key=\"eo\">"+((VOrthogonalEdge)actual).getVerticalFirst()+"</data>"+nl);
	    	   }
	    	   else if (actual.getEdgeType()==VEdge.QUADCURVE)
	    	   {
	    		   s.write("\t\t\t<data key=\"et\">QuadCurve</data>"+nl);
	    		   Point p = ((VQuadCurveEdge)actual).getControlPoints().firstElement();
	    		   s.write("\t\t\t<data key=\"ex\">"+p.x+"</data>"+nl);
	    		   s.write("\t\t\t<data key=\"ey\">"+p.y+"</data>"+nl);		   
	    	   }
	    	   else if (actual.getEdgeType()==VEdge.SEGMENTED)
	    	   {
	    		   s.write("\t\t\t<data key=\"et\">Segmented</data>"+nl);
	    		   Vector<Point> points = ((VSegmentedEdge)actual).getControlPoints();
	    			for (int i=0; i<points.size(); i++)
	    			{
	    				if (points.get(i)!=null)
	    				{
	    				   Point p = points.get(i);
    					   s.write("\t\t\t<data key=\"ex\">"+p.x+"</data>"+nl);
    		    		   s.write("\t\t\t<data key=\"ey\">"+p.y+"</data>"+nl);	}
	    			}		   
	    	   }
	    	   else if (actual.getEdgeType()==VEdge.LOOP)
	    	   {
	    		   VLoopEdge vle = (VLoopEdge) actual;
	    		   s.write("\t\t\t<data key=\"et\">Loop</data>"+nl);
				   if (vle.getLength()!=gp.getIntValue("edge.loop_length"))
					   s.write("\t\t\t<data key=\"elol\">"+vle.getLength()+"</data>"+nl);
   				  if (vle.getProportion()!=((double) gp.getIntValue("edge.loop_proportion") /100.0d))
   					  s.write("\t\t\t<data key=\"elop\">"+(new Double(vle.getProportion())).floatValue()+"</data>"+nl);
   				  if (vle.getDirection()!=gp.getIntValue("edge.loop_direction"))
   					  s.write("\t\t\t<data key=\"elod\">"+vle.getDirection()+"</data>"+nl);
   				  if (vle.isClockwise()!=gp.getBoolValue("edge.loop_clockwise"))
   					  s.write("\t\t\t<data key=\"eloc\">"+vle.isClockwise()+"</data>"+nl);	 
	    	   }
	    	   //Textoutput
	    	   VEdgeText t = actual.getTextProperties();
	    	   //each value is only written if it different from the std value
	    	   if (t.getDistance()!=gp.getIntValue("edge.text_distance"))
	    		   s.write("\t\t\t<data key=\"etd\">"+t.getDistance()+"</data>"+nl);
	    	   if (t.getPosition()!=gp.getIntValue("edge.text_position"))
	    		   s.write("\t\t\t<data key=\"etp\">"+t.getPosition()+"</data>"+nl);
	    	   if (t.getSize()!=gp.getIntValue("edge.text_size"))
	    		   s.write("\t\t\t<data key=\"ets\">"+t.getSize()+"</data>"+nl);
	    	   if (t.isVisible()!=gp.getBoolValue("edge.text_visible"))
	    		   s.write("\t\t\t<data key=\"etv\">"+t.isVisible()+"</data>"+nl);
	    	   if (t.isshowvalue()!=gp.getBoolValue("edge.text_showvalue"))
	    		   s.write("\t\t\t<data key=\"etw\">"+t.isshowvalue()+"</data>"+nl);

	    	   //Linestyleoutput
	    	   VEdgeLinestyle l = actual.getLinestyle();
	    	   //each value is only written if it different from the std value
	    	   if (l.getDistance()!=gp.getIntValue("edge.line_distance"))
	    		   s.write("\t\t\t<data key=\"eld\">"+l.getDistance()+"</data>"+nl);
	    	   if (l.getLength()!=gp.getIntValue("edge.line_length"))
	    		   s.write("\t\t\t<data key=\"ell\">"+l.getLength()+"</data>"+nl);
	    	   if (l.getType()!=gp.getIntValue("edge.line_type"))
	    		   s.write("\t\t\t<data key=\"elt\">"+l.getType()+"</data>"+nl);

	    	   s.write("\t\t</edge>"+nl);
 		}
	}
	/**
	 * Write visual Subgraphs to File
	 * @param s
	 * @throws IOException
	 */
	private void writeVisualSubgraphs(OutputStreamWriter s) throws IOException
	{
	       //Subgraphs
	       Iterator<VSubgraph> ster = vg.modifySubgraphs.getIterator();
	       if (vg.getMathGraph().modifySubgraphs.cardinality()!=0)
	    	   s.write("<!-- == remove these lines to get a valid GraphML-FILE	-->"+nl);
	       while (ster.hasNext())
	       {
	    	   VSubgraph actual = ster.next();
	    	   s.write(nl+"\t\t<subset id=\"subset"+actual.getIndex()+"\">"+nl);
	    	   //if the name is not a standard name
	    	   if (!vg.getMathGraph().modifySubgraphs.get(actual.getIndex()).getName().equals(gp.getSubgraphName(actual.getIndex())))
	    		   s.write("\t\t\t<data key=\"sn\">"+vg.getMathGraph().modifySubgraphs.get(actual.getIndex()).getName()+"</data>"+nl);
	    	   
    		   s.write("\t\t\t<data key=\"sr\">"+actual.getColor().getRed()+"</data>"+nl);
    		   s.write("\t\t\t<data key=\"sg\">"+actual.getColor().getGreen()+"</data>"+nl);
    		   s.write("\t\t\t<data key=\"sb\">"+actual.getColor().getBlue()+"</data>"+nl+nl);

    		   Iterator<VNode> nodeiter = vg.modifyNodes.getIterator();
    		   while (nodeiter.hasNext())
    		   {
    			   VNode n = nodeiter.next();
    			   if (vg.getMathGraph().modifySubgraphs.get(actual.getIndex()).containsNode(n.getIndex()))
    				   s.write("\t\t\t<snode node=\"node"+n.getIndex()+"\" />"+nl);
    		   }

    		   Iterator<VEdge> edgeiter = vg.modifyEdges.getIterator();
    		   while (edgeiter.hasNext())
    		   {
    			   VEdge e = edgeiter.next();
    			   if (vg.getMathGraph().modifySubgraphs.get(actual.getIndex()).containsEdge(e.getIndex()))
    				   s.write("\t\t\t<sedge edge=\"edge"+e.getIndex()+"\" />"+nl);
    		   }
    		   s.write("\t\t</subset>");
 		}
	       if (vg.getMathGraph().modifySubgraphs.cardinality()!=0)
    	       s.write("<!-- == END remove these lines to get a valid GraphML-FILE	-->"+nl);
	}
	/**
	 * Complete Funktion to write an visual Graph to a File
	 * @param f
	 * @return
	 */
	public String saveVisualToFile(File f)
	{
		if (!f.exists())
			try{
				f.createNewFile();
			}
			catch (Exception e)
			{
				System.err.println("DEBUG : Error on Writing File : "+e.getMessage());
			}
		
		try {        
	        OutputStream fout= new FileOutputStream(f);
	        OutputStream bout= new BufferedOutputStream(fout);
	        OutputStreamWriter out = new OutputStreamWriter(bout, "UTF8");
	        writeHeader(out);
	        writeVisualHeaderAddon(out);
	       if (vg.getMathGraph().isDirected())
	    	   out.write(nl+"\t<graph id=\"G\" edgedefault=\"directed\">"+nl);     
	       else
	    	   out.write(nl+"\t<graph id=\"G\" edgedefault=\"undirected\">"+nl);     	    	   
	       //set type to visual
	       out.write("\t\t<data key=\"gt\">visual</data>"+nl);
	       out.write("\t\t<data key=\"gl\">"+vg.getMathGraph().isLoopAllowed()+"</data>"+nl);
	       //Nodes
	       writeVisualNodes(out);
	       writeVisualEdges(out);
	       writeVisualSubgraphs(out);
	       out.write("\t</graph>");
	       writeVisualFooterAddon(out);
	       writeFooter(out);
	       out.flush();  // Don't forget to flush!
	       out.close();
		}
		catch (Exception e)
		{
			return "Fehler beim schreiben: "+e;
		}
		return "";
	}

	
	/**
	 * Write Nodes, but only their mathematical values
	 * @param s
	 * @throws IOException
	 */
	private void writeMathNodes(OutputStreamWriter s) throws IOException
	{
	    //Nodes
	    Iterator<MNode> nodeiter = vg.getMathGraph().modifyNodes.getIterator();
	    while (nodeiter.hasNext())
	    {
	    	MNode actual = nodeiter.next();
	    	s.write(nl+"\t\t<node id=\"node"+actual.index+"\"");
	    	//if the name is not a standard name
	    	if (!(vg.getMathGraph().modifyNodes.get(actual.index).name).equals(gp.getStringValue("node.name")+actual.index))
	    		s.write(">"+nl+"\t\t\t<data key=\"nn\">"+vg.getMathGraph().modifyNodes.get(actual.index).name+"</data>"+nl+"\t\t</node>");
	    	else //no data one tag beginning and end
	    	s.write(" />");
 		}
	}
	/**
	 * Write Edges, but only their mathematical values
	 * @param s
	 * @throws IOException
	 */
	private void writeMathEdges(OutputStreamWriter s) throws IOException
	{
	       //Edges
	       Iterator<MEdge> edgeiter = vg.getMathGraph().modifyEdges.getIterator();
	       s.write(nl);
	       while (edgeiter.hasNext())
	       {
	    	   MEdge actual = edgeiter.next();
	    	   int start = actual.StartIndex;
	    	   int ende = actual.EndIndex;
	    	   int value = actual.Value;
	    	   
	    	   s.write(nl+"\t\t<edge id=\"edge"+actual.index+"\" source=\"node"+start+"\" target=\"node"+ende+"\"");
	    	   if (value!=gp.getIntValue("edge.value")) 	    	   //if the value is not std
	    		   s.write(">"+nl+"\t\t\t<data key=\"ev\">"+value+"</data>"+nl+"\t\t</edge>");
	    	   else //std value, no data keys here
	    		   s.write(" />"); 		   
 		}

	}
	/**
	 * Write mathematical Subgraphs to File
	 * @param s
	 * @throws IOException
	 */
	private void writeMathSubgraphs(OutputStreamWriter s) throws IOException
	{
	       Iterator<MSubgraph> siter = vg.getMathGraph().modifySubgraphs.getIterator();
	       s.write(nl);
	       if (vg.getMathGraph().modifySubgraphs.cardinality()!=0)
	    	   s.write(nl+"<!-- == remove these lines to get a valid GraphML-FILE	-->"+nl);
	       while (siter.hasNext())
	       {
	    	   MSubgraph actual = siter.next();
	    	   s.write("\t\t<subset id=\"subset"+actual.getIndex()+"\">"+nl);
	    	   //if the name is not a standard name
	    	   if (!actual.getName().equals(gp.getSubgraphName(actual.getIndex())))
	    		   s.write("\t\t\t<data key=\"sn\">"+actual.getName()+"</data>");
	    	   
       		   Iterator<VNode> nodeiter = vg.modifyNodes.getIterator();
    		   while (nodeiter.hasNext())
    		   {
    			   VNode n = nodeiter.next();
    			   if (vg.getMathGraph().modifySubgraphs.get(actual.getIndex()).containsNode(n.getIndex()))
    				   s.write("\t\t\t<snode node=\"node"+n.getIndex()+"\" />"+nl);
    		   }

    		   Iterator<VEdge> edgeiter = vg.modifyEdges.getIterator();
    		   while (edgeiter.hasNext())
    		   {
    			   VEdge e = edgeiter.next();
    			   if (actual.containsEdge(e.getIndex()))
    				   s.write("\t\t\t<sedge edge=\"edge"+e.getIndex()+"\" />"+nl);
    		   }
    		   s.write("\t\t</subset>"+nl);
 		}
	       if (vg.getMathGraph().modifySubgraphs.cardinality()!=0)
    	       s.write("<!-- == END remove these lines to get a valid GraphML-FILE	-->"+nl);
	}
	/**
	 * Write complete math graph to file f
	 * @param f
	 * @return
	 */
	public String saveMathToFile(File f)
	{
		if (!f.exists())
			try{
				f.createNewFile();
			}
			catch (Exception e)
			{
				System.err.println("DEBUG : Error on Creating File : "+e.getMessage());
			}
		
		try {        
	        OutputStream fout= new FileOutputStream(f);
	        OutputStream bout= new BufferedOutputStream(fout);
	        OutputStreamWriter out = new OutputStreamWriter(bout, "UTF8");
	        writeHeader(out);
	       if (vg.getMathGraph().isDirected())
	    	   out.write(nl+"\t<graph id=\"G\" edgedefault=\"directed\">"+nl);     
	       else
	    	   out.write(nl+"\t<graph id=\"G\" edgedefault=\"undirected\">"+nl);     	    	   
	       //set type to math
	       out.write("\t\t<data key=\"gt\">math</data>"+nl);
	       out.write("\t\t<data key=\"gl\">"+vg.getMathGraph().isLoopAllowed()+"</data>"+nl);
	       //Nodes
	       writeMathNodes(out);
	       writeMathEdges(out);
	       writeMathSubgraphs(out);
	       out.write(nl+"\t</graph>");
	       writeFooter(out);
	       out.flush();  // Don't forget to flush!
	       out.close();
		}
		catch (Exception e)
		{
			return "Fehler beim schreiben: "+e;
		}
		return "";
	}
}
