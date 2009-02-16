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
 * 
 * @author Ronny Bergmann
 * @since 0.2
 */
public class GravelMLWriter {

	private final static String nl = "\r\n";
	VGraph vg;
	GeneralPreferences gp;
	
	public GravelMLWriter(VGraph a_graph)
	{
		vg = a_graph;
		gp = GeneralPreferences.getInstance();
	}
	
	/**
	 * Write the Header containing comments, standard values and so on. For Math and Visual Graphs
	 */
	private void writeHeader(OutputStreamWriter s) throws IOException
	{
		s.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+nl+"");
		s.write("<!-- 	===GravelML - Format==="+nl);
		s.write("\tGraph Export in an GraphML like format"+nl+"\t");
		s.write("the variation to GraphML is an extra tag <subset> in a graph to save also the subsetgraphs"+nl+"\t");
		s.write(""+nl+"\tAnd some special Keys to store the data. These are GraphML valid and are checked on load of a graph"+nl+"\t");		
		s.write("Any other program may be able to read this file by ignoring the graph.subset tag or removing the part before."+nl+"\t");
		s.write(""+nl+"\t developed by"+nl+"\t\tRonny Bergmann"+nl+"\t adapted from GraphML and the GraphML.dtd (1.0rc) from http://graphml.graphdrawing.org -->"+nl+"");

		s.write(nl+"\t<!-- change the following Doctype to graphml.dtd if you have no internet running. Then the file must be placed in the same directory as this file-->"+nl+"");

		s.write("<!DOCTYPE graphml SYSTEM \"http://gravel.darkmoonwolf.de/gravelml.dtd\">"+nl+""+nl);
		
		s.write("<graphml>"+nl);
		s.write("<!-- Gravel key Defintition : graph.type is 'math' or 'visual' -->"+nl);
		s.write("\t<key id=\"gt\" for=\"graph\" attr.name=\"type\" attr.type=\"string\">"+nl);
		s.write("\t\t<default>math</default>"+nl+"\t</key>"+nl+""+nl);
		s.write("\t<key id=\"gt\" for=\"graph\" attr.name=\"allowloops\" attr.type=\"boolean\">"+nl);
		s.write("\t\t<default>"+vg.getMathGraph().isLoopAllowed()+"</default>"+nl+"\t</key>"+nl+""+nl);
		s.write("\t<key id=\"gt\" for=\"graph\" attr.name=\"allowmultiple\" attr.type=\"boolean\">"+nl);
		s.write("\t\t<default>"+vg.getMathGraph().isMultipleAllowed()+"</default>"+nl+"\t</key>"+nl+""+nl);
		
		s.write("<!-- Gravel key Definitions for any graph -->"+nl);
		s.write("\t<key id=\"ev\" for=\"edge\" attr.name=\"value\" attr.type=\"integer\"> <!-- Value of an edge-->"+nl+"");
		s.write("\t\t<default>"+gp.getIntValue("edge.value")+"</default>"+nl+"\t</key>"+nl); //Fill with GeneralPreferences Std
		s.write("\t<key id=\"en\" for=\"edge\" attr.name=\"name\" attr.type=\"string\" /><!-- name of an Edge -->"+nl);
		s.write("\t\t<default>"+gp.getStringValue("edge.name")+"</default>"+nl);
		s.write("\t<key id=\"nn\" for=\"node\" attr.name=\"name\" attr.type=\"string\"><!-- name of a Node -->"+nl);
		s.write("\t\t<default>"+gp.getStringValue("node.name")+"</default>"+nl+"\t</key>"+nl);
		s.write("\t<key id=\"sn\" for=\"subset\" attr.name=\"name\" attr.type=\"string\"><!-- std name of a subset-->"+nl);
		s.write("\t\t<default>"+gp.getStringValue("subset.name")+"</default>"+nl+"\t</key>"+nl);
	}
	/**
	 *  Write the additional values needed for visual graphs
	 * @param s
	 * @throws IOException
	 */
	private void writeVisualHeaderAddon(OutputStreamWriter s) throws IOException
	{
		s.write(nl+nl+"<!-- Graphel key Definitions for a Graph with visual information -->"+nl);
		s.write("\t<key id=\"nx\" for=\"node\" attr.name=\"x\" attr.type=\"integer\" /> <!--X-Position des Knotens -->"+nl);
		s.write("\t<key id=\"ny\" for=\"node\" attr.name=\"y\" attr.type=\"integer\" /> <!--Y-Position des Knotens -->"+nl);
		s.write("\t<key id=\"ns\" for=\"node\" attr.name=\"size\" attr.type=\"integer\">    <!-- Größe des Knotens -->"+nl);
		s.write("\t<default>"+gp.getIntValue("node.size")+"</default>"+nl+"\t</key>"+nl);
		s.write("\t	<key id=\"nd\" for=\"node\" attr.name=\"name_distance\" attr.type=\"integer\">    <!-- Abstand des Namens vom Knotenpunkt -->"+nl);
		s.write("\t\t<default>"+gp.getIntValue("node.name_distance")+"</default>"+nl+"\t</key>"+nl);
		s.write("\t<key id=\"nr\" for=\"node\" attr.name=\"name_rotation\" attr.type=\"integer\">    <!-- Drehung des Namens -->"+nl);
		s.write("\t\t<default>"+gp.getIntValue("node.name_rotation")+"</default>"+nl+"\t</key>"+nl);
		s.write("\t<key id=\"nns\" for=\"node\" attr.name=\"name_size\" attr.type=\"integer\">    <!-- Textgröße des Namens -->"+nl);
		s.write("\t<default>"+gp.getIntValue("node.name_size")+"</default>"+nl+"\t</key>"+nl);
		s.write("\t<key id=\"nnv\" for=\"node\" attr.name=\"name_visible\" attr.type=\"boolean\">    <!-- Name anzeigen -->"+nl);
		s.write("\t<default>"+gp.getBoolValue("node.name_visible")+"</default>"+nl+"\t</key>"+nl+nl);
		s.write("\t<key id=\"ew\" for=\"edge\" attr.name=\"width\" attr.type=\"integer\"> <!-- Kantenbreite -->"+nl);
		s.write("\t<default>"+gp.getIntValue("edge.width")+"</default>"+nl+"\t</key>"+nl);
		s.write("\t<key id=\"es\" for=\"edge\" attr.name=\"arrsize\" attr.type=\"float\"> <!--Pfeilgröße -->"+nl);
		s.write("\t\t<default>"+gp.getIntValue("edge.arrsize")+".0</default>"+nl+"\t</key>"+nl);
		s.write("\t<key id=\"ep\" for=\"edge\" attr.name=\"arrpart\" attr.type=\"float\"> <!-- Anteil des Pfeils -->"+nl);
		s.write("\t\t<default>"+((float)gp.getIntValue("edge.arrpart")/100)+"</default>"+nl+"\t</key>"+nl);
		s.write("\t<key id=\"eapos\" for=\"edge\" attr.name=\"arrpos\" attr.type=\"float\"> <!-- Anteil des Pfeils -->"+nl);
		s.write("\t\t<default>"+((float)gp.getIntValue("edge.arrpos")/100)+"</default>"+nl+"\t</key>"+nl);
		s.write("\t<key id=\"ea\" for=\"edge\" attr.name=\"arralpha\" attr.type=\"float\"> <!-- Winkel in der Pfeilspitze-->"+nl);
		s.write("\t\t<default>"+gp.getIntValue("edge.arralpha")+".0</default>"+nl+"\t</key>"+nl);
		s.write("\t<key id=\"et\" for=\"edge\" attr.name=\"edgetype\" attr.type=\"string\"> <!-- Kantentyp (Orthogonal|QuadCurve|Segmented|StraightLine|)-->"+nl);
		s.write("\t\t<default>StraightLine</default>"+nl+"\t</key>"); //StraightLine ist immer Std !
		s.write("\t<key id=\"ex\" for=\"edge\" attr.name=\"x\" attr.type=\"integer\" /> <!--weitere Kontrollpunkt (Segmented|QuadCurveEdge) -->"+nl);
		s.write("\t<key id=\"ey\" for=\"edge\" attr.name=\"y\" attr.type=\"integer\" />");
		s.write("\t<key id=\"eo\" for=\"edge\" attr.name=\"orth_verticalfirst\" attr.type=\"boolean\"> <!--Nur fuer Orthogonal pflicht-->"+nl);
		s.write("\t\t<default>true</default>"+nl+"\t</key>"+nl);
		//Loop Stuff
		s.write("\t<key id=\"elol\" for=\"edge\" attr.name=\"looplength\" attr.type=\"integer\"> <!-- Laenge der Hauptellipsenachse -->"+nl);
		s.write("\t\t<default>"+gp.getIntValue("edge.looplength")+"</default>"+nl+"\t</key>"+nl);
		s.write("\t<key id=\"elop\" for=\"edge\" attr.name=\"loopproportion\" attr.type=\"float\"> <!-- Anteil der Nebenachse an der Hauptachse -->"+nl);
		s.write("\t\t<default>"+((float)gp.getIntValue("edge.loopproportion"))+"</default>"+nl+"\t</key>"+nl);
		s.write("\t<key id=\"elod\" for=\"edge\" attr.name=\"loopdirection\" attr.type=\"integer\"> <!-- Rihtung der Schleife vom Knoten aus -->"+nl);
		s.write("\t\t<default>"+gp.getIntValue("edge.loopdirection")+"</default>"+nl+"\t</key>"+nl);
		s.write("\t<key id=\"eloc\" for=\"edge\" attr.name=\"loopclockwise\" attr.type=\"boolean\"> <!-- true, falls im Uhrzeigersinn, sonst false (Auswirkung auf die Pfeilrichtung) -->"+nl);
		s.write("\t\t<default>"+gp.getBoolValue("edge.loopclockwise")+"</default>"+nl+"\t</key>"+nl);
				
		//:Kantenbeschriftungsdaten
		s.write("\t<key id=\"etv\" for=\"edge\" attr.name=\"text_visible\" attr.type=\"boolean\">    <!-- Sichtbarkeit der Kantenbeschriftung -->"+nl);
		s.write("\t<default>"+gp.getBoolValue("edge.text_visible")+"</default>"+nl+"\t</key>"+nl);
		s.write("\t<key id=\"etw\" for=\"edge\" attr.name=\"text_showvalue\" attr.type=\"boolean\">    <!-- Anzeige von Kantenwert oder Kantenname -->"+nl);
		s.write("\t<default>"+gp.getBoolValue("edge.text_showvalue")+"</default>"+nl+"\t</key>"+nl);
		s.write("\t<key id=\"etd\" for=\"edge\" attr.name=\"text_distance\" attr.type=\"integer\">    <!-- Abstand von der Kantenlinie zum Textnittelpunkt -->"+nl);
		s.write("\t<default>"+gp.getIntValue("edge.text_distance")+"</default>"+nl+"\t</key>"+nl);
		s.write("\t<key id=\"etp\" for=\"edge\" attr.name=\"text_position\" attr.type=\"integer\">    <!-- Position auf der Kante -->"+nl);
		s.write("\t<default>"+gp.getIntValue("edge.text_position")+"</default>"+nl+"\t</key>"+nl);
		s.write("\t<key id=\"ets\" for=\"edge\" attr.name=\"text_size\" attr.type=\"integer\">    <!-- Textgroesse -->"+nl);
		s.write("\t<default>"+gp.getIntValue("edge.text_size")+"</default>"+nl+"\t</key>"+nl);
		//Kantenlinienart
		s.write("\t<key id=\"eld\" for=\"edge\" attr.name=\"line_distance\" attr.type=\"integer\">    <!-- Abstand dash-dash/dash-dot/dot-dot -->"+nl);
		s.write("\t<default>"+gp.getIntValue("edge.line_distance")+"</default>"+nl+"\t</key>"+nl);
		s.write("\t<key id=\"ell\" for=\"edge\" attr.name=\"line_length\" attr.type=\"integer\">    <!-- Laenge eines Strichs -->"+nl);
		s.write("\t<default>"+gp.getIntValue("edge.line_length")+"</default>"+nl+"\t</key>"+nl);
		s.write("\t<key id=\"elt\" for=\"edge\" attr.name=\"line_type\" attr.type=\"integer\">    <!-- Standard Linientyp. Empfohlen wird 1 (solid) -->"+nl);
		s.write("\t<default>"+gp.getIntValue("edge.line_type")+"</default>"+nl+"\t</key>"+nl);
		
		s.write("\t<key id=\"sr\" for=\"subset\" attr.name=\"color.r\" attr.type=\"integer\" />    <!-- SubSetColor - Red -->"+nl);
		s.write("\t<key id=\"sg\" for=\"subset\" attr.name=\"color.g\" attr.type=\"integer\" />    <!-- SubSetColor - Green -->"+nl);
		s.write("\t<key id=\"sb\" for=\"subset\" attr.name=\"color.b\" attr.type=\"integer\" />    <!-- SubSetColor - blue -->"+nl);
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
		s.write(nl+"</graphml>"+nl);
	}
	/**
	 * Write each node to a visual file
	 * @param s
	 * @throws IOException
	 */
	private void writeVisualNodes(OutputStreamWriter s) throws IOException
	{
	    //Nodes
	    Iterator<VNode> nodeiter = vg.getNodeIterator();
	    while (nodeiter.hasNext())
	    {
	    	VNode actual = nodeiter.next();
	    	s.write(nl+"\t\t<node id=\"node"+actual.getIndex()+"\">"+nl);
	    	//if the name is not a standard name
	    	if (!vg.getMathGraph().getNodeName(actual.getIndex()).equals(gp.getStringValue("node.name")+actual.getIndex()))
	    		s.write("\t\t\t<data key=\"nn\">"+vg.getMathGraph().getNodeName(actual.getIndex())+"</data>"+nl);
	    	//Position
	    	s.write("\t\t\t<data key=\"nx\">"+actual.getPosition().x+"</data>"+nl);
	    	s.write("\t\t\t<data key=\"ny\">"+actual.getPosition().y+"</data>"+nl);
	    	//Size if it is not std
	    	if (actual.getSize()!=gp.getIntValue("node.size"))
	    		s.write("\t\t\t<data key=\"ns\">"+actual.getSize()+"</data>"+nl);
	    	if (actual.getNameDistance()!=gp.getIntValue("node.name_distance"))
	    		s.write("\t\t\t<data key=\"nd\">"+actual.getNameDistance()+"</data>"+nl);
	    	if (actual.getNameRotation()!=gp.getIntValue("node.name_rotation"))
	    		s.write("\t\t\t<data key=\"nr\">"+actual.getNameRotation()+"</data>"+nl);
	    	if (actual.getNameSize()!=gp.getIntValue("node.name_size"))
	    		s.write("\t\t\t<data key=\"nns\">"+actual.getNameSize()+"</data>"+nl);
	    	if (actual.isNameVisible()!=gp.getBoolValue("node.name_visible"))
	    		s.write("\t\t\t<data key=\"nnv\">"+actual.isNameVisible()+"</data>"+nl);
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
	       Iterator<VEdge> edgeiter = vg.getEdgeIterator();
	       while (edgeiter.hasNext())
	       {
	    	   VEdge actual = edgeiter.next();
	    	   MEdge me = vg.getMathGraph().getEdge(actual.getIndex());
	    	   int start = me.StartIndex;
	    	   int ende = me.EndIndex;
	    	   int value = me.Value;
	    	   s.write(nl+"\t\t<edge id=\"edge"+actual.getIndex()+"\" source=\"node"+start+"\" target=\"node"+ende+"\">"+nl);
	    	   if (value!=gp.getIntValue("edge.value")) 	    	   //if the value is not std
	    		   s.write("\t\t\t<data key=\"ev\">"+value+"</data>"+nl);
	    	   if (actual.getWidth()!=gp.getIntValue("edge.width")) //if width is not std
	    		   s.write("\t\t\t<data key=\"ew\">"+actual.getWidth()+"</data>"+nl);
	    	   s.write("\t\t\t<data key=\"en\">"+vg.getMathGraph().getEdgeName(actual.getIndex())+"</data>"+nl);
	    	   if (actual.getArrow().getSize()!=((float)gp.getIntValue("edge.arrsize"))) //if arrpart is not std
	    		   s.write("\t\t\t<data key=\"es\">"+actual.getArrow().getSize()+"</data>"+nl);
	    	   if (actual.getArrow().getPart()!=((float)gp.getIntValue("edge.arrpart")/100)) //if arrpart is not std
	    		   s.write("\t\t\t<data key=\"ep\">"+actual.getArrow().getPart()+"</data>"+nl);
	    	   if (actual.getArrow().getAngle()!=((float)gp.getIntValue("edge.arralpha"))) //if arrangle is not std
	    		   s.write("\t\t\t<data key=\"ea\">"+actual.getArrow().getAngle()+"</data>"+nl);
	    	   if (actual.getArrow().getPos()!=((float)gp.getIntValue("edge.arrpos")/100)) //if arrangle is not std
	    		   s.write("\t\t\t<data key=\"eapos\">"+actual.getArrow().getPos()+"</data>"+nl);
	    	   
	    	   if (actual.getType()==VEdge.ORTHOGONAL)
	    	   {
	    		   s.write("\t\t\t<data key=\"et\">Orthogonal</data>"+nl);
	    		   if (((VOrthogonalEdge)actual).getVerticalFirst()!=gp.getBoolValue("edge.orth_verticalfirst")) //non standard Orth Edge
	    			   s.write("\t\t\t<data key=\"eo\">"+((VOrthogonalEdge)actual).getVerticalFirst()+"</data>"+nl);
	    	   }
	    	   else if (actual.getType()==VEdge.QUADCURVE)
	    	   {
	    		   s.write("\t\t\t<data key=\"et\">QuadCurve</data>"+nl);
	    		   Point p = ((VQuadCurveEdge)actual).getControlPoints().firstElement();
	    		   s.write("\t\t\t<data key=\"ex\">"+p.x+"</data>"+nl);
	    		   s.write("\t\t\t<data key=\"ey\">"+p.y+"</data>"+nl);		   
	    	   }
	    	   else if (actual.getType()==VEdge.SEGMENTED)
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
	    	   else if (actual.getType()==VEdge.LOOP)
	    	   {
	    		   VLoopEdge vle = (VLoopEdge) actual;
	    		   s.write("\t\t\t<data key=\"et\">Loop</data>"+nl);
				   if (vle.getLength()!=gp.getIntValue("edge.looplength"))
					   s.write("\t\t\t<data key=\"elol\">"+vle.getLength()+"</data>"+nl);
   				  if (vle.getProportion()!=((double) gp.getIntValue("edge.loopproportion") /100.0d))
   					  s.write("\t\t\t<data key=\"elop\">"+(new Double(vle.getProportion())).floatValue()+"</data>"+nl);
   				  if (vle.getDirection()!=gp.getIntValue("edge.loopdirection"))
   					  s.write("\t\t\t<data key=\"elod\">"+vle.getDirection()+"</data>"+nl);
   				  if (vle.isClockwise()!=gp.getBoolValue("edge.loopclockwise"))
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
	 * Write visual SubSets to File
	 * @param s
	 * @throws IOException
	 */
	private void writeVisualSubSets(OutputStreamWriter s) throws IOException
	{
	       //SubSets
	       Iterator<VSubSet> subsetiter = vg.getSubSetIterator();
	       if (vg.getMathGraph().SubSetCount()!=0)
	    	   s.write("<!-- == remove these lines to get a valid GraphML-FILE	-->"+nl);
	       while (subsetiter.hasNext())
	       {
	    	   VSubSet actual = subsetiter.next();
	    	   s.write(nl+"\t\t<subset id=\"subset"+actual.getIndex()+"\">"+nl);
	    	   //if the name is not a standard name
	    	   if (!vg.getMathGraph().getSubSetName(actual.getIndex()).equals(gp.getStringValue("subset.name")+actual.getIndex()))
	    		   s.write("\t\t\t<data key=\"sn\">"+vg.getMathGraph().getSubSetName(actual.getIndex())+"</data>"+nl);
	    	   
    		   s.write("\t\t\t<data key=\"sr\">"+actual.getColor().getRed()+"</data>"+nl);
    		   s.write("\t\t\t<data key=\"sg\">"+actual.getColor().getGreen()+"</data>"+nl);
    		   s.write("\t\t\t<data key=\"sb\">"+actual.getColor().getBlue()+"</data>"+nl+nl);

    		   Iterator<VNode> nodeiter = vg.getNodeIterator();
    		   while (nodeiter.hasNext())
    		   {
    			   VNode n = nodeiter.next();
    			   if (vg.getMathGraph().SubSetcontainsNode(n.getIndex(),actual.getIndex()))
    				   s.write("\t\t\t<snode node=\"node"+n.getIndex()+"\" />"+nl);
    		   }

    		   Iterator<VEdge> edgeiter = vg.getEdgeIterator();
    		   while (edgeiter.hasNext())
    		   {
    			   VEdge e = edgeiter.next();
    			   if (vg.getMathGraph().SubSetcontainsEdge(e.getIndex(),actual.getIndex()))
    				   s.write("\t\t\t<sedge edge=\"edge"+e.getIndex()+"\" />"+nl);
    		   }
    		   s.write("\t\t</subset>");
 		}
	       if (vg.getMathGraph().SubSetCount()!=0)
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
	       writeVisualSubSets(out);
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
	    Iterator<MNode> nodeiter = vg.getMathGraph().getNodeIterator();
	    while (nodeiter.hasNext())
	    {
	    	MNode actual = nodeiter.next();
	    	s.write(nl+"\t\t<node id=\"node"+actual.index+"\"");
	    	//if the name is not a standard name
	    	if (!vg.getMathGraph().getNodeName(actual.index).equals(gp.getStringValue("node.name")+actual.index))
	    		s.write(">"+nl+"\t\t\t<data key=\"nn\">"+vg.getMathGraph().getNodeName(actual.index)+"</data>"+nl+"\t\t</node>");
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
	       Iterator<MEdge> edgeiter = vg.getMathGraph().getEdgeIterator();
	       s.write(nl);
	       while (edgeiter.hasNext())
	       {
	    	   MEdge actual = edgeiter.next();
	    	   Vector<Integer> values = vg.getMathGraph().getEdgeProperties(actual.index);
	    	   int start = values.elementAt(MGraph.EDGESTARTINDEX);
	    	   int ende = values.elementAt(MGraph.EDGEENDINDEX);
	    	   int value = values.elementAt(MGraph.EDGEVALUE);
	    	   
	    	   s.write(nl+"\t\t<edge id=\"edge"+actual.index+"\" source=\"node"+start+"\" target=\"node"+ende+"\"");
	    	   if (value!=gp.getIntValue("edge.value")) 	    	   //if the value is not std
	    		   s.write(">"+nl+"\t\t\t<data key=\"ev\">"+value+"</data>"+nl+"\t\t</edge>");
	    	   else //std value, no data keys here
	    		   s.write(" />"); 		   
 		}

	}
	/**
	 * Write mathematical Subsets to File
	 * @param s
	 * @throws IOException
	 */
	private void writeMathSubSets(OutputStreamWriter s) throws IOException
	{
	       //SubSets
	       Iterator<MSubSet> subsetiter = vg.getMathGraph().getSubSetIterator();
	       s.write(nl);
	       if (vg.getMathGraph().SubSetCount()!=0)
	    	   s.write(nl+"<!-- == remove these lines to get a valid GraphML-FILE	-->"+nl);
	       while (subsetiter.hasNext())
	       {
	    	   MSubSet actual = subsetiter.next();
	    	   s.write("\t\t<subset id=\"subset"+actual.getIndex()+"\">"+nl);
	    	   //if the name is not a standard name
	    	   if (!actual.getName().equals(gp.getStringValue("subset.name")+actual.getIndex()))
	    		   s.write("\t\t\t<data key=\"sn\">"+actual.getName()+"</data>");
	    	   
       		   Iterator<VNode> nodeiter = vg.getNodeIterator();
    		   while (nodeiter.hasNext())
    		   {
    			   VNode n = nodeiter.next();
    			   if (vg.getMathGraph().SubSetcontainsNode(n.getIndex(),actual.getIndex()))
    				   s.write("\t\t\t<snode node=\"node"+n.getIndex()+"\" />"+nl);
    		   }

    		   Iterator<VEdge> edgeiter = vg.getEdgeIterator();
    		   while (edgeiter.hasNext())
    		   {
    			   VEdge e = edgeiter.next();
    			   if (vg.getMathGraph().SubSetcontainsEdge(e.getIndex(),actual.getIndex()))
    				   s.write("\t\t\t<sedge edge=\"edge"+e.getIndex()+"\" />"+nl);
    		   }
    		   s.write("\t\t</subset>"+nl);
 		}
	       if (vg.getMathGraph().SubSetCount()!=0)
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
	       writeMathSubSets(out);
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
