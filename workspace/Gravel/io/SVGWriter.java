package io;
import model.*;

import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.Vector;
import java.awt.Color;

import view.VCommonGraphic;
import view.VGraphic;
import view.VHyperGraphic;

	/**
	 * each node is written to the svg file and translated therefore
	 * x - offset to get rid of the free space left of the graph
	 * y - offset to get rid of the free space above the graph
	 * 
	 * @author Ronny Bergmann
	 *
	 */
public class SVGWriter
{
		private final static String NL = "\r\n";
		VCommonGraphic vgc;
		VGraphInterface vg;
		GeneralPreferences gp;
		Point max,offset;
		boolean directed=false;
		//All needed Sets and Name Vectors
		VNodeSet nodes;
		Vector<String> nodenames, edgenames, subgraphnames;
		VEdgeSet edges;
		MEdgeSet medges;
		MHyperEdgeSet mhyperedges;
		VSubgraphSet subgraphs;
		VHyperEdgeSet hyperedges;
		
		private int width;
		/**
		 * Starts the LaTeX-Export with some parameters
		 * @param a_picture a given VGraph in an VGraphic-Environment
		 * @param w width of the picture in LaTeX in mm
		 * @param type either "doc" for al whole LaTeX-Document or "fig" for just the figure
		 */
		public SVGWriter(VCommonGraphic a_picture, int w)
		{
			vgc = a_picture;
			if (vgc.getType()==VCommonGraphic.VGRAPHIC)
			{
				nodes = ((VGraphic)vgc).getGraph().modifyNodes;
				nodenames = ((VGraphic)vgc).getGraph().getMathGraph().modifyNodes.getNames();
				edges = ((VGraphic)vgc).getGraph().modifyEdges;
				medges = ((VGraphic)vgc).getGraph().getMathGraph().modifyEdges;
				edgenames = ((VGraphic)vgc).getGraph().getMathGraph().modifyEdges.getNames();
				subgraphs = ((VGraphic)vgc).getGraph().modifySubgraphs;
				subgraphnames = ((VGraphic)vgc).getGraph().getMathGraph().modifySubgraphs.getNames();
				vg=((VGraphic)vgc).getGraph();
				directed = ((VGraphic)vgc).getGraph().getMathGraph().isDirected();
			}
			else if (vgc.getType()==VCommonGraphic.VHYPERGRAPHIC)
			{
					nodes = ((VHyperGraphic)vgc).getGraph().modifyNodes;
					hyperedges = ((VHyperGraphic)vgc).getGraph().modifyHyperEdges;
					mhyperedges = ((VHyperGraphic)vgc).getGraph().getMathGraph().modifyHyperEdges;
					edgenames = ((VHyperGraphic)vgc).getGraph().getMathGraph().modifyHyperEdges.getNames();
					subgraphs = ((VHyperGraphic)vgc).getGraph().modifySubgraphs;
					subgraphnames = ((VHyperGraphic)vgc).getGraph().getMathGraph().modifySubgraphs.getNames();
					vg=((VHyperGraphic)vgc).getGraph();
			}
			gp = GeneralPreferences.getInstance();
			width = w;
		}		
		//General
		
		private void writeHeader(OutputStreamWriter s, String filename) throws IOException
		{
			offset = vg.getMinPoint(vgc.getGraphics());
			max = vg.getMaxPoint(vgc.getGraphics());
			int x = max.x-offset.x; //Breite
			int y = max.y-offset.y; //Hoehe
			int height = Math.round((float)width*(float)y/(float)x);
			String name_escaped = replace(filename,"_","\\_");
			name_escaped = replace(name_escaped,"&","\\&");
			
			s.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"+NL);
			s.write("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 20010904//EN\" ");
			s.write("  \"http://www.w3.org/TR/2001/REC-SVG-20010904/DTD/svg10.dtd\">"+NL);
			s.write("<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" width=\""+width+"\" height=\""+height+"\" viewBox=\""+offset.x+" "+offset.y+" "+x+" "+y+"\">"+NL);
			s.write("<style type=\"text/css\">"+NL+"<![CDATA["+NL+"\ttext {");
			s.write("font-family:serif;");
			s.write("text-anchor:middle;}"+NL+"]]>"+NL+"</style>");
						
			s.write("<title>Gravel-Export als SVG - "+name_escaped+"</title>"+NL);			
		}

		private void writeFooter(OutputStreamWriter s, String name) throws IOException
		{
			s.write(NL+"</svg>");
		}
		
		//Visual
		/**
		 * Get a Color for the node
		 * @return 
		 */
		private String getItemColorString(VItem Item)
		{
			String c="";
			if (Item==null)
				return c;
			Color col = Item.getColor();
			c +="rgb("+col.getRed()+","+col.getGreen()+","+col.getBlue()+")";
			return c;
		}

		private void writeNodes(OutputStreamWriter s) throws IOException
		{
		    //Nodes
		    Iterator<VNode> nodeiter = nodes.getIterator();
		    while (nodeiter.hasNext())
		    {
		    	VNode actual = nodeiter.next();
				s.write(NL+"\t\t<circle cx=\""+actual.getPosition().x+"\" cy=\""+actual.getPosition().y+"\" r=\""+(actual.getSize()/2)+"px\" ");
				s.write("style=\"fill:"+getItemColorString(actual)+"\"/>"+NL);
		    	if (actual.isNameVisible()) //draw name
				{	
					//mittelpunkt des Textes
					int x = actual.getPosition().x + Math.round((float)actual.getNameDistance()*(float)Math.cos(Math.toRadians((double)actual.getNameRotation())));
					int y = actual.getPosition().y - Math.round((float)actual.getNameDistance()*(float)Math.sin(Math.toRadians((double)actual.getNameRotation())));
					s.write("\t\t\t<text x=\""+x+"\" y=\""+y+"\" style=\"font-size:"+actual.getNameSize()+"px; baseline-shift:-"+(actual.getNameSize()/2)+";\">"+formname(nodenames.get(actual.getIndex()))+"</text>");
				}
			}
		}
		
		private String formname(String ur)
		{
			return ur;
			//ur = replace(ur,"#","\\#");
			//return "$"+ur+"$";
		}
		/**
		 * Einfache SubStringersetzung
		 * @param in Eingabe
		 * @param remove entfernen und 
		 * @param replace ersetzen durch diesen
		 * @return
		 */
		public static String replace(String in,String remove, String replace) 
		{
			if (in==null || remove==null || remove.length()==0) return in;
			StringBuffer sb = new StringBuffer();
			int oldIndex = 0;
			int newIndex = 0;
			int remLength = remove.length();
			while ( (newIndex = in.indexOf(remove,oldIndex)) > -1) 
			{
					//copy from last to new appearance
					sb.append(in.substring(oldIndex,newIndex));
					sb.append(replace);
					//set old index to end of last apperance.
					oldIndex = newIndex + remLength;
			}
			int inLength = in.length();
			//add part after last appearance of string to remove
			if(oldIndex<inLength) sb.append(in.substring(oldIndex,inLength));
			return sb.toString();
		}

		private void writeEdges(OutputStreamWriter s) throws IOException
		{
		       //Nodes
	    	Iterator<VEdge> edgeiter = edges.getIterator();
	    	while (edgeiter.hasNext())
	    	{
	    	   VEdge actual = edgeiter.next();
	    	   MEdge me = medges.get(actual.getIndex());
	    	   int start = me.StartIndex;
	    	   int ende = me.EndIndex;
	    	   Point b = nodes.get(start).getPosition();
	    	   Point e = nodes.get(ende).getPosition();
	    	   b.x = b.x; b.y = b.y;
	    	   e.x = e.x; e.y = e.y;
	    	   //not needed int value = values.elementAt(MGraph.EDGEVALUE);
			   //Mittlere Linie der Kante...immer Zeichnen
	    	   if (actual.getEdgeType()==VEdge.STRAIGHTLINE)
	    	  	   s.write("<path d=\"M "+b.x+","+b.y+" L "+e.x+","+e.y+"\" ");
	    	   if (actual.getEdgeType()==VEdge.ORTHOGONAL)
	    	   {
	    		   if (((VOrthogonalEdge)actual).getVerticalFirst())
	    			    s.write("<path d=\"M "+b.x+","+b.y+" L "+b.x+","+e.y+" L "+e.x+","+e.y+"\" ");
	    		   else	
	    			    s.write("<path d=\"M "+b.x+","+b.y+" L "+e.x+","+b.y+" L "+e.x+","+e.y+"\" ");
	    	   }
	    	   if (actual.getEdgeType()==VEdge.QUADCURVE)
	    	   {
	    		   Point bez = ((VQuadCurveEdge)actual).getControlPoints().firstElement();
	    		   s.write("<path d=\"M "+b.x+","+b.y+" Q "+bez.x+","+bez.y+" "+e.x+","+e.y+"\"");
	    	   }
	    	   if (actual.getEdgeType()==VEdge.SEGMENTED)
	    	   {
	    		   Iterator<Point> pit = ((VSegmentedEdge)actual).getControlPoints().iterator();
	    		   s.write("<path d=\"M "+b.x+","+b.y+"");
	    		   while (pit.hasNext())
	    		   {
	    			 Point p = pit.next();
	    			 s.write(" L "+(p.x)+","+(p.y)+"");
	    		   }
	    		   s.write(" \"");
	    	   }	    	  			   
	    	   if (actual.getEdgeType()==VEdge.LOOP) // SVG Export
	    	   {
	    		   VLoopEdge l = (VLoopEdge)actual;
	    		   Point m = l.getControlPoints().firstElement();
	    		   Point n = nodes.get(me.EndIndex).getPosition();
	    		   //Mitte zwischen Kontrollpunkt und Start/Endknoten der hier der selbe ist
	    		   m.x = (n.x+m.x)/2;
	    		   m.y = (n.y+m.y)/2;
	    		   int mainaxis = Math.round(((float)l.getLength())/2f);
	    		   int minoraxis = Math.round((((float)l.getLength())/2f)*((float)l.getProportion()));
	    		   s.write("<ellipse cx=\""+m.x+"\" cy=\""+m.y+"\" rx=\""+mainaxis+"\" ry=\""+minoraxis+"\" transform=\"rotate(-"+l.getDirection()+","+m.x+","+m.y+")\""+NL);
	    	   }
	    	  VEdgeLinestyle style = actual.getLinestyle();
	    	 s.write(" style=\"stroke-dashoffset:0;");
	    	  if (style.getType()==VEdgeLinestyle.DASHED)
	    		s.write(" stroke-dasharray:"+style.getLength()+","+style.getDistance()+";");  
		      if (style.getType()==VEdgeLinestyle.DOTTED)
		    		s.write(" stroke-dasharray:0,"+style.getDistance()+";");  		    	  
		      if (style.getType()==VEdgeLinestyle.DOTDASHED)
		    		s.write(" stroke-dasharray:"+style.getLength()+","+style.getDistance()+",0,"+style.getDistance()+";");  
		      
			  s.write(" stroke-linecap:round; stroke-linejoin:round; stroke:"+getItemColorString(actual)+"; stroke-width:"+actual.getWidth()+"px; fill:none;\"/>"+NL);
		      
			  if (actual.getTextProperties().isVisible()) //draw name
				{	
		    		VEdgeText t = actual.getTextProperties();
					Point m = actual.getTextCenter(nodes.get(me.StartIndex).getPosition(), nodes.get(me.EndIndex).getPosition());
		    		//get the text wich should be displayd
				    String text = "";
				    if (t.isshowvalue())
						text = ""+me.Value;
				    else
				    	text = me.name;
				    s.write(NL+"\t<text x=\""+(m.x)+"\" y=\""+(m.y)+"\" style=\"font-size:"+actual.getTextProperties().getSize()+"pt; baseline-shift:-"+(actual.getTextProperties().getSize()/2)+";\">"+formname(text)+"</text>"+NL);
				}
			  s.write(drawArrow(actual,start,ende));
	       }//End while edges.hasNext()
		}
		/**
		 * Write down an Arrow for an edge as an filed polygon
		 * @param edge
		 * @param nodestart
		 * @param nodeende
		 * @return
		 */
		private String drawArrow(VEdge edge, int start, int ende)
		{
			double[] coords = new double[2];
			int x = 0, y = 0;
			//Point2D.Double arrowhead = new Point2D.Double(),line1start = new Point2D.Double(),line1 = new Point2D.Double(),line2start = new Point2D.Double(),line2 = new Point2D.Double();
			String s = "";
			if (directed)
			{
			  	Shape arrow = edge.getArrowShape(nodes.get(start).getPosition(),nodes.get(ende).getPosition(),Math.round(nodes.get(start).getSize()/2),Math.round(nodes.get(ende).getSize()/2),1.0f);
			  	PathIterator path = arrow.getPathIterator(null, 0.001);
//			  	int i=0;
			  	s += "<path d=\"";
			    while( !path.isDone() ) 
			      {
			      int type = path.currentSegment(coords);
			    	x = Math.round((float)coords[0]); y = Math.round((float)coords[1]);
			    	if (type==PathIterator.SEG_MOVETO)
			    	{
			    		s += NL+"M "+(x)+","+(y)+" ";
			    	}
			    	else if (type==PathIterator.SEG_LINETO)	
			    	{
			    		s += "L "+x+","+y+" ";
			    	}
			    	path.next();
			    }
			  s +="z\" style=\"fill:"+getItemColorString(edge)+"\"/>";
			}
			return s;
		}
		private void writeSubgraphs(OutputStreamWriter s) throws IOException
		{
			//nothing to do up to now
		}
		private void writeHyperEdges(OutputStreamWriter s) throws IOException
		{
		       //Nodes
	    	Iterator<VHyperEdge> hyperedgeiter = hyperedges.getIterator();
	    	while (hyperedgeiter.hasNext())
	    	{
	    	   VHyperEdge actual = hyperedgeiter.next();
	    	   MHyperEdge me = mhyperedges.get(actual.getIndex());
	    	   s.write("<path "+drawOnePath(actual.getShape().getCurve(0.5d).getPathIterator(null,0.005d)));
	    	   
	    	   VEdgeLinestyle style = actual.getLinestyle();
	    	 s.write(" style=\"stroke-dashoffset:0;");
	    	  if (style.getType()==VEdgeLinestyle.DASHED)
	    		s.write(" stroke-dasharray:"+style.getLength()+","+style.getDistance()+";");  
		      if (style.getType()==VEdgeLinestyle.DOTTED)
		    		s.write(" stroke-dasharray:0,"+style.getDistance()+";");  		    	  
		      if (style.getType()==VEdgeLinestyle.DOTDASHED)
		    		s.write(" stroke-dasharray:"+style.getLength()+","+style.getDistance()+",0,"+style.getDistance()+";");  
		      
			  s.write(" stroke-linecap:round; stroke-linejoin:round; stroke:"+getItemColorString(actual)+"; stroke-width:"+actual.getWidth()+"px; fill:none;\"/>"+NL);
		      
			  if (actual.getTextProperties().isVisible()) //draw name
				{	
		    		VEdgeText t = actual.getTextProperties();
					Point m = actual.getTextCenter();
					//get the text wich should be displayd
				    String text = "";
				    if (t.isshowvalue())
						text = ""+me.Value;
				    else
				    	text = me.name;
				    s.write(NL+"\t<text x=\""+(m.x)+"\" y=\""+(m.y)+"\" style=\"font-size:"+actual.getTextProperties().getSize()+"pt; baseline-shift:-"+(actual.getTextProperties().getSize()/2)+";\">"+formname(text)+"</text>"+NL);
				}
		   }//End while hyperedges.hasNext()
		}
		private String drawOnePath(PathIterator path)
		{
			String s ="d=\"";
		   	double[] coords = new double[2];
		   	double x = 0.0, y = 0.0;
		    while( !path.isDone() ) 
			{
			   int type = path.currentSegment(coords);
			    x = coords[0]; y = coords[1];
		    	if (type==PathIterator.SEG_MOVETO)
					s +="M"+x+" "+y+" ";
		    	else if (type==PathIterator.SEG_LINETO)	
		    		s +="L"+x+" "+y+" ";
		    	path.next();
			}
			return s+" Z\""; //return as closed path
		}
		public String saveToFile(File f)
		{
			if (!f.exists())
				try{
					f.createNewFile();
				}
				catch (Exception e)
				{
					return "Creation_Error : "+e.getMessage();
				}
			
			try {        
		       OutputStream fout= new FileOutputStream(f);
		       OutputStream bout= new BufferedOutputStream(fout);
		       OutputStreamWriter out = new OutputStreamWriter(bout, "UTF8");
		       writeHeader(out,f.getName());
		       if (edges!=null) //Graph
		    	   writeEdges(out);
		       else if (hyperedges!=null) //Hypergraph
		    	   writeHyperEdges(out);
		       writeSubgraphs(out);
		       writeNodes(out);
		       writeFooter(out,"Gravel Graphen-Export '"+f.getName()+"'");
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