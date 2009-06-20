package io;

import model.*;

import java.awt.Point;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.BitSet;
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
		s.write("pusblished under CC-BY -->"+nl+nl);
				
		s.write("<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\""+nl+  
				"     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""+nl+
				"xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns "+nl+
            "                    http://gravel.darkmoonwolf.de/xmlns/gravelml.xsd\">"+nl);
		s.write("\t <!-- Extensions of the Elements following the Key-Data-Paradigm of GraphML -->"+nl);
		s.write("\t\t<!-- Type of Graph represented here (no default value) - Values are math|math hyper|visual|visual hyper -->"+nl);
		s.write("\t<key id=\"graphtype\" for=\"graph\" attr.name=\"type\" attr.type=\"string\" />"+nl+nl);
		if (vg!=null)
		{
			s.write("\t\t<!-- Values for allowance of loops and multiple edges -->"+nl);
			s.write("\t<key id=\"graphloops\" for=\"graph\" attr.name=\"graph.allowloops\" attr.type=\"boolean\">"+nl+
					"\t\t<default>"+gp.getBoolValue("graph.allowloops")+"</default>"+nl+"\t</key>"+nl);
			s.write("\t<key id=\"graphmultipleedges\" for=\"graph\" attr.name=\"graph.allowmultiple\" attr.type=\"boolean\">"+nl+
					"\t\t<default>"+gp.getBoolValue("graph.allowmultiple")+"</default>"+nl+"\t</key>"+nl+nl);			
			s.write("\t\t<!-- Values for edges -->"+nl);
			s.write("\t<key id=\"edgevalue\" for=\"edge\" attr.name=\"edge.value\" attr.type=\"int\">"+nl+
					"\t\t<default>"+gp.getIntValue("edge.value")+"</default>"+nl+"\t</key>"+nl);
			s.write("\t<key id=\"edgename\" for=\"edge\" attr.name=\"edge.name\" attr.type=\"string\">"+nl+
					"\t\t<default>"+gp.getStringValue("edge.name")+"</default>"+nl+"\t</key>"+nl);
		}
		else if (vhg!=null)
		{
			s.write("\t\t<!-- Values for hyperedges -->"+nl);
			s.write("\t<key id=\"hyperedgevalue\" for=\"hyperedge\" attr.name=\"hyperedge.value\" attr.type=\"int\">"+nl
					+"\t\t<default>"+gp.getIntValue("hyperedge.value")+"</default>"+nl+"\t</key>"+nl);
			s.write("\t<key id=\"hyperedgename\" for=\"hyperedge\" attr.name=\"hyperedge.name\" attr.type=\"string\">"+nl+
					"\t\t<default>"+gp.getStringValue("hyperedge.name")+"</default>"+nl+"\t</key>"+nl);
		}
		s.write("\t\t<!-- Values for nodes -->"+nl);
		s.write("\t<key id=\"nodename\" for=\"node\" attr.name=\"node.name\" attr.type=\"string\">"+nl+
				"\t\t<default>"+gp.getStringValue("node.name")+"</default>"+nl+"\t</key>"+nl);

		VSubgraphSet vsubset;
		if (vg!=null)
		{
			vsubset = vg.modifySubgraphs;
		}
		else if (vhg!=null)
		{
			vsubset = vhg.modifySubgraphs;
		}
		else
			throw new IOException("No suitable input graph found.");
		Iterator<VSubgraph> ster = vsubset.getIterator();
		if (ster.hasNext()) //At leastone existent -> Comment
			s.write("\t\t<!-- One Value for each individual subgraph so that the data keys are unique -->"+nl);
		while (ster.hasNext()) //ONe Key for each subgraph
		{
			VSubgraph actual = ster.next();
			s.write("\t<key id=\"subgraph"+actual.getIndex()+"\" for=\"graph\" attr.name=\"subgraph"+actual.getIndex()+"\" attr.complexType=\"graph.subgraph.type\"/>"+nl);
		}
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
			s.write("\t<key id=\"edgearrow\" for=\"edge\" attr.name=\"edge.arrow\" attr.complexType=\"edge.arrow.type\">"+nl+
					"\t\t<default>"+nl+"\t\t\t<arrow"+
					" size=\""+gp.getIntValue("edge.arrow_size")+"\""+
					" part=\""+gp.getFloatValue("edge.arrow_part")+"\""+
					" position=\""+gp.getFloatValue("edge.arrow_pos")+"\""+
					" headalpha=\""+gp.getIntValue("edge.arrow_alpha")+"\"/>"+nl+
					"\t\t</default>"+nl+"\t</key>"+nl);
			s.write("\t<key id=\"edgepoints\" for=\"edge\" attr.name=\"edge.points\" attr.complexType=\"edge.points.type\"/>"+nl);
			s.write("\t<key id=\"edgewidth\" for=\"edge\" attr.name=\"edge.width\" attr.type=\"int\">"+nl+
					"\t\t<default>"+gp.getIntValue("edge.width")+"</default>"+nl+"\t</key>"+nl);
			s.write("\t<key id=\"edgetype\" for=\"edge\" attr.name=\"edge.type\" attr.type=\"string\"> <!-- Kantentyp (Orthogonal|QuadCurve|Segmented|StraightLine|)-->"+nl);
			s.write("\t\t<default>StraightLine</default>"+nl+"\t</key>"+nl); //StraightLine ist immer Std !
			s.write("\t<key id=\"edgeorthogonal\" for=\"edge\" attr.name=\"orthogonaledge_verticalfirst\" attr.type=\"boolean\"> <!--Nur fuer Orthogonal pflicht-->"+nl);
			s.write("\t\t<default>true</default>"+nl+"\t</key>"+nl);
			
			s.write("\t<key id=\"loopedge\" for=\"edge\" attr.name=\"edge.loop\" attr.complexType=\"edge.loop.type\">"+nl+
					"\t\t<default>"+nl+
					"\t\t\t<loopedge length=\""+gp.getIntValue("edge.loop_length")+"\""+
					" proportion=\""+gp.getIntValue("edge.loop_proportion")+"\""+
					" direction=\""+gp.getIntValue("edge.loop_direction")+"\""+
					" clockwise=\""+gp.getBoolValue("edge.loop_clockwise")+"\""+
					"/>"+nl+"\t\t</default>"+nl+"\t</key>"+nl);
			s.write("\t<key id=\"edgetext\" for=\"edge\" attr.name=\"edge.text\" attr.complexType=\"edge.text.type\">"+nl+
					"\t\t<default>"+nl+
					"\t\t\t<edgetext distance=\""+gp.getIntValue("edge.text_distance")+"\""+
					" position=\""+gp.getIntValue("edge.text_position")+"\""+
					" size=\""+gp.getIntValue("edge.text_size")+"\""+
					" show=");
			if (gp.getBoolValue("edge.text_showvalue"))
				s.write("\"value\"");
			else
				s.write("\"name\"");
			s.write(" visible=\""+gp.getBoolValue("edge.text_visible")+"\"/>"+nl);
			s.write("\t\t</default>"+nl+"\t</key>"+nl);
			
			s.write("\t<key id=\"edgeline\" for=\"edge\" attr.name=\"edge.line\" attr.complexType=\"edge.line.type\">"+nl+
					"\t\t<default>"+nl+
					"\t\t\t<edgeline length=\""+gp.getIntValue("edge.line_length")+"\""+
					" distance=\""+gp.getIntValue("edge.line_distance")+"\""+
					" type=\"");
			switch(gp.getIntValue("edge.line_type"))
			{
				case VEdgeLinestyle.DOTTED:
					s.write("dotted");
					break;
				case VEdgeLinestyle.DASHED:
					s.write("dashed");
					break;
				case VEdgeLinestyle.DOTDASHED:
					s.write("dotdashed");
					break;
				default:
					s.write("solid");
			}
			s.write("\"/>"+nl+
					"\t\t</default>"+nl+"\t</key>"+nl);
		}
		else
		{
			s.write("\t\t<!-- Hyperedge Details -->"+nl);
			s.write("\t<key id=\"hyperedgewidth\" for=\"hyperedge\" attr.name=\"hyperedge.width\" attr.type=\"int\">"+nl+
					"\t\t<default>"+gp.getIntValue("hyperedge.width")+"</default>"+nl+"\t</key>"+nl);
			s.write("\t<key id=\"hyperedgemargin\" for=\"hyperedge\" attr.name=\"hyperedge.margin\" attr.type=\"int\">"+nl+
					"\t\t<default>"+gp.getIntValue("hyperedge.margin")+"</default>"+nl+"\t</key>"+nl);
			s.write("\t<key id=\"hyperedgetext\" for=\"hyperedge\" attr.name=\"hyperedge.text\" attr.complexType=\"edge.text.type\">"+nl+
					"\t\t<default>"+nl+
					"\t\t\t<hyperedgetext distance=\""+gp.getIntValue("edge.text_distance")+"\""+
					" position=\""+gp.getIntValue("edge.text_position")+"\""+
					" size=\""+gp.getIntValue("edge.text_size")+"\""+
					" show=");
			if (gp.getBoolValue("edge.text_showvalue"))
				s.write("\"value\"");
			else
				s.write("\"name\"");
			s.write(" visible=\""+gp.getBoolValue("edge.text_visible")+"\"/>"+nl);
			s.write("\t\t</default>"+nl+"\t</key>"+nl);
			s.write("\t<key id=\"hyperedgeline\" for=\"hyperedge\" attr.name=\"hyperedge.line\" attr.complexType=\"edge.line.type\">"+nl+
					"\t\t<default>"+nl+
					"\t\t\t<hyperedgeline length=\""+gp.getIntValue("edge.line_length")+"\""+
					" distance=\""+gp.getIntValue("edge.line_distance")+"\""+
					" type=\"");
			switch(gp.getIntValue("edge.line_type"))
			{
				case VEdgeLinestyle.DOTTED:
					s.write("dotted");
					break;
				case VEdgeLinestyle.DASHED:
					s.write("dashed");
					break;
				case VEdgeLinestyle.DOTDASHED:
					s.write("dotdashed");
					break;
				default:
					s.write("solid");
			}
			s.write("\"/>"+nl+
				"\t\t</default>"+nl+"\t</key>"+nl);	
			s.write("\t<key id=\"hyperedgeshape\" for=\"hyperedge\" attr.name=\"hyperedge.shape\" attr.complexType=\"hyperedge.shape.type\"/>"+nl+nl);
		}
		s.write("\t\t<!-- Node Values -->"+nl);
		s.write("\t<key id=\"nodeform\" for=\"node\" attr.name=\"node\" attr.complexType=\"node.form.type\">"+nl+
				"\t\t<default>"+nl+
				"\t\t\t<form type=\"Circle\" x=\"0\" y=\"0\" size=\""+gp.getIntValue("node.size")+"\"/>"+nl+
				"\t\t</default>"+nl+"\t</key>"+nl);
		s.write("\t<key id=\"nodetext\" for=\"node\" attr.name=\"node.text\" attr.complexType=\"node.text.type\">"+nl+
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
	    		s.write("\t\t\t<data key=\"nodename\">"+mnodes.get(actual.getIndex()).name+"</data>"+nl);
	    	//Position
	    	s.write("\t\t\t<data key=\"nodeform\"><form type=\"Circle\" x=\""+actual.getPosition().x+"\""+
	    			" y=\""+actual.getPosition().y+"\"");
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
			if (vg==null)
				throw new IOException("No Graph Given");
	       Iterator<VEdge> edgeiter = vg.modifyEdges.getIterator();
	       while (edgeiter.hasNext())
	       {
	    	   VEdge actual = edgeiter.next();
	    	   MEdge me = vg.getMathGraph().modifyEdges.get(actual.getIndex());
	    	   int start = me.StartIndex;
	    	   int ende = me.EndIndex;
	    	   int value = me.Value;
	    	   s.write(nl+"\t\t<edge id=\""+actual.getIndex()+"\" source=\""+start+"\" target=\""+ende+"\">"+nl);
	    	   if (!gp.getEdgeName(me.index,start,ende).equals(me.name)) //If name is not std
	    		   s.write("\t\t\t<data key=\"edgename\">"+me.name+"</data>"+nl);
	    	   if (value!=gp.getIntValue("edge.value")) 	    	   //if the value is not std
	    		   s.write("\t\t\t<data key=\"edgevalue\">"+value+"</data>"+nl);
	    	   if (actual.getWidth()!=gp.getIntValue("edge.width")) //if width is not std
	    		   s.write("\t\t\t<data key=\"edgewidth\">"+actual.getWidth()+"</data>"+nl);
	    	   
	    	   if (actual.getEdgeType()==VEdge.ORTHOGONAL)
	    	   {
	    		   s.write("\t\t\t<data key=\"edgetype\">Orthogonal</data>"+nl);
	    		   if (((VOrthogonalEdge)actual).getVerticalFirst()!=gp.getBoolValue("edge.orth_verticalfirst")) //non standard Orth Edge
	    			   s.write("\t\t\t<data key=\"edgeorthogonal\">"+((VOrthogonalEdge)actual).getVerticalFirst()+"</data>"+nl);
	    	   }
	    	   else if (actual.getEdgeType()==VEdge.QUADCURVE)
	    	   {
	    		   s.write("\t\t\t<data key=\"edgetype\">QuadCurve</data>"+nl);
	    		   Point p = ((VQuadCurveEdge)actual).getControlPoints().firstElement();
	    		   s.write("\t\t\t<data key=\"edgepoints\">"+nl+
	    				   "\t\t\t\t<point id=\"0\" x=\""+p.x+"\" y=\""+p.y+"\"/>"+nl+
	    				   "\t\t\t</data>"+nl);		   
	    	   }
	    	   else if (actual.getEdgeType()==VEdge.SEGMENTED)
	    	   {
	    		   s.write("\t\t\t<data key=\"edgetype\">Segmented</data>"+nl);
	    		   Vector<Point> points = ((VSegmentedEdge)actual).getControlPoints();
	    		   s.write("\t\t\t<data key=\"edgepoints\">"+nl);
	    			for (int i=0; i<points.size(); i++)
	    			{
	    				if (points.get(i)!=null)
	    				{
	    				   Point p = points.get(i);
	    				   s.write("\t\t\t\t<point id=\""+i+"\" x=\""+p.x+"\" y=\""+p.y+"\"/>"+nl);
	    				}
	    			}		   
				   s.write("\t\t\t</data>"+nl);		   
	    	   }
	    	   else if (actual.getEdgeType()==VEdge.LOOP)
	    	   {
	    		   VLoopEdge vle = (VLoopEdge) actual;
	    		   s.write("\t\t\t<data key=\"edgetype\">Loop</data>"+nl);
				   boolean noStdLen = vle.getLength()!=gp.getIntValue("edge.loop_length"),
				   		   noStdProp = vle.getProportion()!=((double)gp.getIntValue("edge.loop_proportion") /100.0d),
				   		   noStdDir = vle.getDirection()!=gp.getIntValue("edge.loop_direction"),
				   		   noStdCW = vle.isClockwise()!=gp.getBoolValue("edge.loop_clockwise");
				   if (noStdLen||noStdProp||noStdDir||noStdCW) //Do we need an loopedge-Element?
				   {					   
					   s.write("\t\t\t<data key=\"loopedge\">"+nl+
							   "\t\t\t\t<loopedge");
					   if (noStdLen)
						   s.write(" length\""+vle.getLength()+"\"");
					   if (noStdProp)
						   s.write(" proportion=\""+((float)vle.getProportion())+"\"");
					   if (noStdDir)
						   s.write(" direction=\""+vle.getDirection()+"\"");
					   if (noStdCW)
						   s.write(" clockwise=\""+vle.isClockwise()+"\"");
					   s.write("/>"+nl+"\t\t\t</data>"+nl);
   				   }
	    	   }
	    	   //The Straightline does not need a type, because Straightline is Std.
	    	   
	    	   writeArrow(s,actual.getArrow());
	    	   writeText(s,actual.getTextProperties(),false);
	    	   writeLinestyle(s,actual.getLinestyle(),false);
	    	   s.write("\t\t</edge>"+nl);
 		}
	}
	/**
	 * Write an Arrow of an edge to the file
	 * @param s OutputStream
	 * @param arrow Arrow of the edge
	 * @throws IOException
	 */
	private void writeArrow(OutputStreamWriter s, VEdgeArrow arrow) throws IOException
	{
		if (arrow==null)
			throw new IOException(" No Arrow for Output");
 	   boolean noStdASize = arrow.getSize()!=((float)gp.getIntValue("edge.arrow_size")),
		   noStdAPart = arrow.getPart()!=gp.getFloatValue("edge.arrow_part"),
		   noStdAAngle = arrow.getAngle()!=((float)gp.getIntValue("edge.arrow_alpha")),
		   noStdAPos = arrow.getPos()!=gp.getFloatValue("edge.arrow_pos");
 	   if (noStdASize||noStdAPart||noStdAAngle||noStdAPos)
 	   { //<arrow size="14" part=".8" position=".77" headalpha="20.0"/>
 		   s.write("\t\t\t<data key=\"edgearrow\">"+nl+
 		   "\t\t\t\t<arrow");
 		   if (noStdASize)
 			   s.write(" size=\""+arrow.getSize()+"\"");
 		   if (noStdAPart)
 			   s.write(" part=\""+arrow.getPart()+"\"");
 		   if (noStdAPos)
 			   s.write(" position=\""+arrow.getPos()+"\"");
 		   if (noStdAAngle)
 			   s.write(" headalpha=\""+arrow.getAngle()+"\"");
 		   s.write("/>"+nl+"\t\t\t</data>"+nl);
 	   }
	}
	/**
	 * Write an (hyper)edge text to file
	 * @param s
	 * @param t
	 * @param hyper true if it is for an hperedge else false
	 * @throws IOException
	 */
	private void writeText(OutputStreamWriter s, VEdgeText t, boolean hyper) throws IOException
	{
		boolean noStdDis = t.getDistance()!=gp.getIntValue("edge.text_distance"),
		noStdPos = t.getPosition()!=gp.getIntValue("edge.text_position"),
		noStdSize = t.getSize()!=gp.getIntValue("edge.text_size"),
		noStdVis = t.isVisible()!=gp.getBoolValue("edge.text_visible"),
		noStdShow = t.isshowvalue()!=gp.getBoolValue("edge.text_showvalue");

		if (noStdDis||noStdPos||noStdSize||noStdVis||noStdShow) //We need an Textelement
		{
			s.write("\t\t\t<data key=\"");
			if (hyper)
				s.write("hyper");
			s.write("edgetext\">"+nl+
					"\t\t\t\t<");
			if (hyper)
				s.write("hyper");
			s.write("edgetext");
			if (noStdDis)
				s.write(" distance=\""+t.getDistance()+"\"");
			if (noStdPos)
				s.write(" position=\""+t.getPosition()+"\"");
			if (noStdSize)
				s.write(" size=\""+t.getSize()+"\"");
			if (noStdVis)
				s.write(" visible=\""+t.isVisible()+"\"");
			if (noStdDis)
			{
				if (t.isshowvalue())
					s.write(" show=\"value\"");
				else
					s.write(" show=\"name\"");
			}
			s.write("/>"+nl+"\t\t\t</data>"+nl);
		}
	}
	/**
	 * Write an (hyper)edge line style to file
	 * @param s
	 * @param l
	 * @param hyper true if it is for an hyperedge else false
	 * @throws IOException
	 */
	private void writeLinestyle(OutputStreamWriter s, VEdgeLinestyle l, boolean hyper) throws IOException
	{
		boolean noStdDist = l.getDistance()!=gp.getIntValue("edge.line_distance"),
		noStdLen = l.getLength()!=gp.getIntValue("edge.line_length"),
		noStdType = l.getType()!=gp.getIntValue("edge.line_type");
		
		if (noStdDist||noStdLen||noStdType)
		{
			s.write("\t\t\t<data key=\"");
			if (hyper)
				s.write("hyper");
			s.write("edgeline\">"+nl+
			"\t\t\t\t<");
			if (hyper)
				s.write("hyper");
			s.write("edgeline");
			if (noStdDist)
				s.write(" distance=\""+l.getDistance()+"\"");
			if (noStdLen)
				s.write(" length=\""+l.getLength()+"\"");
			if (noStdType)
			{
				s.write(" type=\"");
				switch(l.getType())
				{
					case VEdgeLinestyle.DOTTED:
						s.write("dotted");
						break;
					case VEdgeLinestyle.DASHED:
						s.write("dashed");
						break;
					case VEdgeLinestyle.DOTDASHED:
						s.write("dotdashed");
						break;
					default:
						s.write("solid");
				}
				s.write("\"");
			}
			//each value is only written if it different from the std value
		    s.write("/>"+nl+"\t\t\t</data>"+nl);
		}
	}
	/**
	 * Write all HyperEdges of vhg into File
	 * @param s
	 * @throws IOException
	 */
	private void writeVisualHyperEdges(OutputStreamWriter s) throws IOException
	{
		if (vhg==null)
			throw new IOException("No hyergraph given");
		Iterator<VHyperEdge> edgeiter = vhg.modifyHyperEdges.getIterator();
		while (edgeiter.hasNext())
		{
			VHyperEdge actual = edgeiter.next();
			MHyperEdge mhe = vhg.getMathGraph().modifyHyperEdges.get(actual.getIndex());
			s.write(nl+"\t\t<hyperedge id=\""+actual.getIndex()+"\">"+nl);
			BitSet endnodes = mhe.getEndNodes();
			for (int i=0; i<endnodes.size(); i++)
			{
				if (endnodes.get(i))
				{
					s.write("\t\t\t<endpoint node=\""+i+"\"/>"+nl);
				}
			}
    	   
    	   if (!gp.getHyperedgeName(mhe.index).equals(mhe.name)) //If name is not std
    		   s.write("\t\t\t<data key=\"hyperedgename\">"+mhe.name+"</data>"+nl);
    	   if (mhe.Value!=gp.getIntValue("hyperedge.value")) 	    	   //if the value is not std
    		   s.write("\t\t\t<data key=\"hyperedgevalue\">"+mhe.Value+"</data>"+nl);
    	   if (actual.getWidth()!=gp.getIntValue("hyperedge.width")) //if width is not std
    		   s.write("\t\t\t<data key=\"hyperedgewidth\">"+actual.getWidth()+"</data>"+nl);
    	   if (actual.getMinimumMargin()!=gp.getIntValue("hyperedge.margin")) //if width is not std
    		   s.write("\t\t\t<data key=\"hyperedgemargin\">"+actual.getMinimumMargin()+"</data>"+nl);

    	   writeText(s,actual.getTextProperties(),true);
    	   writeLinestyle(s,actual.getLinestyle(),true);
    	   writeShape(s,actual.getShape());
    	   s.write("\t\t</hyperedge>"+nl);
		}
	}
	/**
	 * Write one HyperEdgeShape into File
	 * @param s
	 * @param shape
	 * @throws IOException
	 */
	private void writeShape(OutputStreamWriter s, NURBSShape shape) throws IOException
	{
		String str = (new NURBSShapeGraphML(shape)).toGraphML("hyperedgeshape","hyperedgeshape",nl,"\t\t\t");
		if (!str.equals(""))
			s.write(str);
	}
	/**
	 * Write visual Subgraphs to File
	 * @param s
	 * @throws IOException
	 */
	private void writeVisualSubgraphs(OutputStreamWriter s) throws IOException
	{
		VSubgraphSet vsubset;
		MSubgraphSet msubset;
		if (vg!=null)
		{
			vsubset = vg.modifySubgraphs;
			msubset = vg.getMathGraph().modifySubgraphs;
		}
		else if (vhg!=null)
		{
			vsubset = vhg.modifySubgraphs;
			msubset = vhg.getMathGraph().modifySubgraphs;
		}
		else
			throw new IOException("No suitable input graph found.");
		Iterator<VSubgraph> ster = vsubset.getIterator();
		while (ster.hasNext())
		{
			VSubgraph actual = ster.next();
			s.write(nl+"\t\t<data key=\"subgraph"+actual.getIndex()+"\">"+nl+
					"\t\t\t<subgraph id=\""+actual.getIndex()+"\">"+nl);
			//if the name is not a standard name
			if (msubset.get(actual.getIndex()).getName().equals(gp.getSubgraphName(actual.getIndex())))
    		   s.write("\t\t\t\t<name>"+msubset.get(actual.getIndex()).getName()+"</name>"+nl);
			s.write("\t\t\t\t<color r=\""+actual.getColor().getRed()+"\""+
					" g=\""+actual.getColor().getGreen()+"\""+
					" b=\""+actual.getColor().getBlue()+"\"/>"+nl);

		   Iterator<VNode> nodeiter;
		   if (vg!=null)
			   nodeiter = vg.modifyNodes.getIterator();
		   else if (vhg!=null)
			   nodeiter = vhg.modifyNodes.getIterator();
		   else throw new IOException("no graph existent");
		   while (nodeiter.hasNext())
		   {
			   VNode n = nodeiter.next();
			   if (msubset.get(actual.getIndex()).containsNode(n.getIndex()))
				   s.write("\t\t\t\t<nodeid>"+n.getIndex()+"</nodeid>"+nl);
		   }
		   if (vg!=null)
		   {
			   Iterator<VEdge> edgeiter = vg.modifyEdges.getIterator();
			   while (edgeiter.hasNext())
			   {
				   VEdge e = edgeiter.next();
				   if (msubset.get(actual.getIndex()).containsEdge(e.getIndex()))
					   s.write("\t\t\t\t<edgeid>"+e.getIndex()+"</edgeid>"+nl);
			   }
		   }
		   else if (vhg!=null)
		   {
			   Iterator<VHyperEdge> edgeiter = vhg.modifyHyperEdges.getIterator();
			   while (edgeiter.hasNext())
			   {
				   VHyperEdge e = edgeiter.next();
				   if (msubset.get(actual.getIndex()).containsEdge(e.getIndex()))
					   s.write("\t\t\t\t<hyperedgeid>"+e.getIndex()+"</hyperedgeid>"+nl);
			   }
		   }
		   s.write("\t\t\t</subgraph>"+nl+"\t\t</data>"+nl);
		} //End of running through all Subgraphs
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
	        if (vg!=null)
	        {
	        	if (vg.getMathGraph().isDirected())
	        		out.write(nl+"\t<graph id=\"G\" edgedefault=\"directed\">"+nl);     
	        	else
	        		out.write(nl+"\t<graph id=\"G\" edgedefault=\"undirected\">"+nl);
	        }
	        else if (vhg!=null)
	        {
        		out.write(nl+"\t<graph id=\"HG\" edgedefault=\"undirected\">"+nl);     
	        }
	       //set type to visual
	       out.write("\t\t<data key=\"graphtype\">visual");
	       if(vg!=null)
	    	   out.write(" graph");
	       else if (vhg!=null)
	    	   out.write(" hypergraph");
	       out.write("</data>"+nl);
	       if (vg!=null)
	       {
	    	   out.write("\t\t<data key=\"graphloops\">"+vg.getMathGraph().isLoopAllowed()+"</data>"+nl);
	    	   out.write("\t\t<data key=\"graphmultipleedges\">"+vg.getMathGraph().isMultipleAllowed()+"</data>"+nl);
	       }
	       //Nodes
	       writeVisualNodes(out);
	       if (vg!=null)
	    	   writeVisualEdges(out);
	       else if (vhg!=null)
	    	   writeVisualHyperEdges(out);
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
		MNodeSet mnodes;
		if (vg!=null)
			mnodes = vg.getMathGraph().modifyNodes;
		else if (vhg!=null)
			mnodes = vhg.getMathGraph().modifyNodes;
		else
			throw new IOException("No Input given");
	    //Nodes
	    Iterator<MNode> nodeiter = mnodes.getIterator();
	    while (nodeiter.hasNext())
	    {
	    	MNode actual = nodeiter.next();
	    	s.write(nl+"\t\t<node id=\""+actual.index+"\">"+nl);
	    	//if the name is not a standard name
	    	if (!mnodes.get(actual.index).name.equals(gp.getNodeName(actual.index)))
	    		s.write("\t\t\t<data key=\"nodename\">"+mnodes.get(actual.index).name+"</data>"+nl);
	    	s.write("\t\t</node>"+nl);
	    }
	}
	/**
	 * Write Edges, but only their mathematical values
	 * @param s
	 * @throws IOException
	 */
	private void writeMathEdges(OutputStreamWriter s) throws IOException
	{
		if (vg==null)
			throw new IOException("No Graph found");
	   //Edges
	   Iterator<MEdge> edgeiter = vg.getMathGraph().modifyEdges.getIterator();
	   s.write(nl);
	   while (edgeiter.hasNext())
	   {
		   MEdge actual = edgeiter.next();
		   int start = actual.StartIndex;
		   int ende = actual.EndIndex;
		   int value = actual.Value;
    	   s.write(nl+"\t\t<edge id=\""+actual.index+"\" source=\""+start+"\" target=\""+ende+"\">"+nl);
    	   if (!gp.getEdgeName(actual.index,start,ende).equals(actual.name)) //If name is not std
    		   s.write("\t\t\t<data key=\"edgename\">"+actual.name+"</data>"+nl);
    	   if (value!=gp.getIntValue("edge.value")) 	    	   //if the value is not std
    		   s.write("\t\t\t<data key=\"edgevalue\">"+value+"</data>"+nl);
    	   s.write("\t\t</edge>"+nl);
 		}
	}
	/**
	 * Write the mathematical part of an hyperedge into
	 * @param s an OutputStream
	 * @throws IOException
	 */
	private void writeMathHyperEdges(OutputStreamWriter s) throws IOException
	{
		if (vhg==null)
			throw new IOException("No hyergraph given");
		Iterator<MHyperEdge> edgeiter = vhg.getMathGraph().modifyHyperEdges.getIterator();
		while (edgeiter.hasNext())
		{
			MHyperEdge actual = edgeiter.next();
			s.write(nl+"\t\t<hyperedge id=\""+actual.index+"\">"+nl);
			BitSet endnodes = actual.getEndNodes();
			for (int i=0; i<endnodes.size(); i++)
			{
				if (endnodes.get(i))
				{
					s.write("\t\t\t<endpoint node=\""+i+"\"/>"+nl);
				}
			}
    	   
    	   if (!gp.getHyperedgeName(actual.index).equals(actual.name)) //If name is not std
    		   s.write("\t\t\t<data key=\"hyperedgename\">"+actual.name+"</data>"+nl);
    	   if (actual.Value!=gp.getIntValue("hyperedge.value")) 	    	   //if the value is not std
    		   s.write("\t\t\t<data key=\"hyperedgevalue\">"+actual.Value+"</data>"+nl);
    	   s.write("\t\t</hyperedge>"+nl);
		}
	}
	/**
	 * Write mathematical Subgraphs to File
	 * @param s
	 * @throws IOException
	 */
	private void writeMathSubgraphs(OutputStreamWriter s) throws IOException
	{
		MSubgraphSet msubset;
		if (vg!=null)
			msubset = vg.getMathGraph().modifySubgraphs;
		else if (vhg!=null)
			msubset = vhg.getMathGraph().modifySubgraphs;
		else
			throw new IOException("No suitable input graph found.");
		Iterator<MSubgraph> ster = msubset.getIterator();
		while (ster.hasNext())
		{
			MSubgraph actual = ster.next();
			s.write(nl+"\t\t<data key=\"subgraph"+actual.getIndex()+"\">"+nl+
					"\t\t\t<subgraph id=\""+actual.getIndex()+"\">"+nl);
			//if the name is not a standard name
			if (msubset.get(actual.getIndex()).getName().equals(gp.getSubgraphName(actual.getIndex())))
    		   s.write("\t\t\t\t<name>"+msubset.get(actual.getIndex()).getName()+"</name>"+nl);
		   Iterator<MNode> nodeiter;
		   if (vg!=null)
			   nodeiter = vg.getMathGraph().modifyNodes.getIterator();
		   else if (vhg!=null)
			   nodeiter = vhg.getMathGraph().modifyNodes.getIterator();
		   else throw new IOException("no graph existent");
		   while (nodeiter.hasNext())
		   {
			   MNode n = nodeiter.next();
			   if (msubset.get(actual.getIndex()).containsNode(n.index))
				   s.write("\t\t\t\t<nodeid>"+n.index+"</nodeid>"+nl);
		   }
		   if (vg!=null)
		   {
			   Iterator<MEdge> edgeiter = vg.getMathGraph().modifyEdges.getIterator();
			   while (edgeiter.hasNext())
			   {
				   MEdge e = edgeiter.next();
				   if (msubset.get(actual.getIndex()).containsEdge(e.index))
					   s.write("\t\t\t\t<edgeid>"+e.index+"</edgeid>"+nl);
			   }
		   }
		   else if (vhg!=null)
		   {
			   Iterator<MHyperEdge> edgeiter = vhg.getMathGraph().modifyHyperEdges.getIterator();
			   while (edgeiter.hasNext())
			   {
				   MHyperEdge e = edgeiter.next();
				   if (msubset.get(actual.getIndex()).containsEdge(e.index))
					   s.write("\t\t\t\t<hyperedgeid>"+e.index+"</hyperedgeid>"+nl);
			   }
		   }
		   s.write("\t\t\t</subgraph>"+nl+"\t\t</data>"+nl);
		} //End of running through all Subgraphs
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
				return "DEBUG : Error on Writing File : "+e.getMessage();
			}
		if ((vg==null)&&(vhg==null))
			return "No graph or hypergraph given to write to file "+f.getAbsolutePath();
		try {        
	        OutputStream fout= new FileOutputStream(f);
	        OutputStream bout= new BufferedOutputStream(fout);
	        OutputStreamWriter out = new OutputStreamWriter(bout, "UTF8");
	        writeHeader(out);
	        if (vg!=null)
	        {
	        	if (vg.getMathGraph().isDirected())
	        		out.write(nl+"\t<graph id=\"G\" edgedefault=\"directed\">"+nl);     
	        	else
	        		out.write(nl+"\t<graph id=\"G\" edgedefault=\"undirected\">"+nl);
	        }
	        else if (vhg!=null)
	        {
	    		out.write(nl+"\t<graph id=\"HG\" edgedefault=\"undirected\">"+nl);     
	        }
	       //set type to math
	       out.write("\t\t<data key=\"graphtype\">math");
	       if(vg!=null)
	    	   out.write(" graph");
	       else if (vhg!=null)
	    	   out.write(" hypergraph");
	       out.write("</data>"+nl);
	       if (vg!=null)
	       {
	    	   out.write("\t\t<data key=\"graphloops\">"+vg.getMathGraph().isLoopAllowed()+"</data>"+nl);
	    	   out.write("\t\t<data key=\"graphmultipleedges\">"+vg.getMathGraph().isMultipleAllowed()+"</data>"+nl);
	       }
	       //Nodes
	       //Nodes
	       writeMathNodes(out);
	       if (vg!=null)
		       writeMathEdges(out);
	       else if (vhg!=null)
		       writeMathHyperEdges(out);
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
