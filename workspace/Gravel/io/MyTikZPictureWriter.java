package io;

import model.*;

import java.awt.Point;
import java.awt.Shape;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.Vector;
import java.awt.Color;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;


import view.VCommonGraphic;
import view.VGraphic;
import view.VHyperGraphic;
/**
 * each node is written to the tex file and translated therefore
 * x - offset to get rid of the free space left of the graph
 * maxy - y to invert y (no offset substractiion needed, because 0 is max and max.y - offset.y is the maximum to be reached
 * @author ronny
 *
 */
public class MyTikZPictureWriter implements TeXWriter {

	private final static String NL = "\r\n";
	VCommonGraphic vgc;
	VGraphInterface vg;
	GeneralPreferences gp;
	Point max,offset;
	double pixelpercm, figurewidth; //Pixel auf einen cm und breite der ErgebnisGrafik in cm
	private boolean wholedoc,directed, linesinPT;

	VNodeSet nodes;
	Vector<String> nodenames, edgenames, subgraphnames;
	VEdgeSet edges;
	MEdgeSet medges;
	MHyperEdgeSet mhyperedges;
	VSubgraphSet subgraphs;
	MSubgraphSet msubgraphs;
	VHyperEdgeSet hyperedges;

	/**
	 * Starts the LaTeX-Export with some parameters
	 * @param a_picture a given VGraph in an VGraphic-Environment
	 * @param w width of the picture in LaTeX in mm
	 * @param type eiter "doc" for al whole LaTeX-Document or "fig" for just the figure
	 */
	public MyTikZPictureWriter(VCommonGraphic a_picture, int w, String type, boolean scalelines)
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
			msubgraphs = ((VGraphic)vgc).getGraph().getMathGraph().modifySubgraphs;
			subgraphnames = ((VGraphic)vgc).getGraph().getMathGraph().modifySubgraphs.getNames();
			vg=((VGraphic)vgc).getGraph();
			directed = ((VGraphic)vgc).getGraph().getMathGraph().isDirected();
		}
		else if (vgc.getType()==VCommonGraphic.VHYPERGRAPHIC)
		{
				nodes = ((VHyperGraphic)vgc).getGraph().modifyNodes;
				nodenames = ((VHyperGraphic)vgc).getGraph().getMathGraph().modifyNodes.getNames();
				hyperedges = ((VHyperGraphic)vgc).getGraph().modifyHyperEdges;
				mhyperedges = ((VHyperGraphic)vgc).getGraph().getMathGraph().modifyHyperEdges;
				edgenames = ((VHyperGraphic)vgc).getGraph().getMathGraph().modifyHyperEdges.getNames();
				subgraphs = ((VHyperGraphic)vgc).getGraph().modifySubgraphs;
				msubgraphs = ((VHyperGraphic)vgc).getGraph().getMathGraph().modifySubgraphs;
				subgraphnames = ((VHyperGraphic)vgc).getGraph().getMathGraph().modifySubgraphs.getNames();
				vg=((VHyperGraphic)vgc).getGraph();
		}
		gp = GeneralPreferences.getInstance();
		figurewidth = w/10d;
		if (type.equalsIgnoreCase("doc"))
			wholedoc = true;
		else
			wholedoc = false;		
		pixelpercm = (double)(vg.getMaxPoint(vgc.getGraphics()).x-vg.getMinPoint(vgc.getGraphics()).x)/figurewidth;
		offset = vg.getMinPoint(vgc.getGraphics());
		max = vg.getMaxPoint(vgc.getGraphics());
		linesinPT = scalelines;
//		System.err.println(pixelpercm);
	}
	/**
	 * Starts the LaTeX-Export with some parameters
	 * @param a_picture a given VGraph in an VGraphic-Environment
	 * @param w width of the picture in LaTeX in mm
	 * @param type eiter "doc" for al whole LaTeX-Document or "fig" for just the figure
	 */
	public MyTikZPictureWriter(VCommonGraphic a_picture, int w, String type)
	{
		this(a_picture,w,type,false);
	}
	
	//General
	
	private void writeHeader(OutputStreamWriter s, String filename) throws IOException
	{
		if (wholedoc)
		{
			s.write("%   Exported Graph by Gravel"+NL+"% "+NL+"%   to use in your own LaTeX-Document copy the figure into your file"+NL);
			s.write("%"+NL+"%   This Document is minimized in its usage of packages"+NL);
			s.write(NL+"\\documentclass[a4paper, DIV=calc]{scrartcl}"+NL);
			s.write("\\usepackage{tikz,xcolor}"+NL);
			s.write("\\usepackage[utf8]{inputenc}"+NL);
			s.write("\\usepackage[T1]{fontenc}"+NL);
			produceColors(s);
			s.write(NL+"\\providecolor{black}{rgb}{0,0,0}"+NL);
			s.write(NL+"\\begin{document}");
			s.write(NL+"\t%"+NL+"\t%Copy this Part into your Document"+NL+"\t%");
			//Compute the size and offset
		}
		else
		{
			s.write("% Export of a Graph by Gravel"+NL+"%"+NL+"% this document only contains the figure-Environment"+NL+"%"+NL+"% Usage : \\include{"+filename+"} to include this in your document"+NL+"% (if you haven't changed this filename and your main .tex-File is in the same directory"+NL);
			produceColors(s);
			s.write(NL+"\\providecolor{black}{rgb}{0,0,0}");
		}
		s.write(NL+"\t\\begin{figure}"+NL+"\t\\centering");				
		s.write(NL+"\t\\begin{tikzpicture}"+NL); //+kein Offset ist testweise
	}

	private void writeFooter(OutputStreamWriter s, String name) throws IOException
	{
		String name_escaped = replace(name,"_","\\_");
		name_escaped = replace(name_escaped,"&","\\&");
		s.write(NL+"\t\\end{tikzpicture}"+NL);
		s.write(NL+"\t\t\\caption{"+name_escaped+"}"+NL);
		s.write("%Caption of the Figure - Enter your Explanation here - "+NL);
		s.write("\t\\end{figure}");
		s.write(NL+"\t%"+NL+"\t%End of the Graph"+NL+"\t%");
		if (wholedoc)
		{
			s.write(NL+"\\end{document}"+NL);
		}
	}
	
	private void produceColors(OutputStreamWriter s) throws IOException
	{
		s.write(NL+"% The Following Colors each represent a subgraph and are mixed on the specific Nodes and (hyper)edges they are needed"+NL);
		Iterator<VSubgraph> subiter = subgraphs.getIterator();
		while (subiter.hasNext())
		{
			VSubgraph actsub = subiter.next();
			s.write(NL+"\\definecolor{subgraph"+actsub.getIndex()+"}{rgb}{"
					+(((double)actsub.getColor().getRed())/255d)+","
					+(((double)actsub.getColor().getGreen())/255d)+","
					+(((double)actsub.getColor().getBlue())/255d)+"}");
		}
	}
	private String produceColor(VItem item)
	{
		if ( (item.getColor().getRed()==0) && (item.getColor().getGreen()==0) && (item.getColor().getBlue()==0) )
		{
			return "black";
		}
		// Following After schema {rgb:red,4;green,2;yellow,1} 
		String colormix = "rgb:";
		Iterator<MSubgraph> msubiter = msubgraphs.getIterator();
		boolean first=true;
		while (msubiter.hasNext())
		{
			MSubgraph actsub = msubiter.next();
			
			if ( ((item.getType()==VItem.NODE)&&(actsub.containsNode(item.getIndex())))
					|| ((item.getType()==VItem.HYPEREDGE)&&(actsub.containsEdge(item.getIndex())))
					|| ((item.getType()==VItem.EDGE)&&(actsub.containsEdge(item.getIndex())))   )
			{
				if (first)
				{
					colormix += "subgraph"+actsub.getIndex()+",1";
					first=false;
				}
				else
					colormix += ";subgraph"+actsub.getIndex()+",1";
			}
		}
		return "{"+colormix+"}";
	}
	private String produceLineSpec(VEdgeLinestyle l, int linewidth)
	{
		// dash pattern=on 2pt off 3pt on 4pt off 4pt
		String linespec="";
		double realDist = ((double)l.getDistance())/pixelpercm;
		double realLength = ((double)l.getLength())/pixelpercm;
		double realLinewidth= ((double)linewidth)/pixelpercm;
		switch (l.getType())
		{
			case VEdgeLinestyle.DASHED:
				//Subtract realLinewidth for linecap=round 
				linespec +=" dash pattern=on "+(realLength-realLinewidth)+"cm off "+(realDist+realLinewidth)+"cm";
				break;
			case VEdgeLinestyle.DOTDASHED:
				//The very small on pattern approximates a dot because we have linecap round
				linespec +=" dash pattern=on "+(realLength-realLinewidth)+"cm off "+(realDist+realLinewidth/2d)
						 +"cm on 0cm off "+(realDist+realLinewidth/2d)+"cm";			
				break;
			case VEdgeLinestyle.DOTTED:
				linespec +=" dash pattern=on 0cm off "+(realDist-realLinewidth/2d)+"cm";
				break;
			case VEdgeLinestyle.SOLID:
			default:
				linespec += "solid";
				break;
		}
		linespec += ",line cap=round";
		return linespec;
	}
	
	private void writeNodes(OutputStreamWriter s) throws IOException
	{
	    //Nodes
	    Iterator<VNode> nodeiter = nodes.getIterator();
	    while (nodeiter.hasNext())
	    {
	    	VNode actual = nodeiter.next();
	    	Point2D drawpoint = new Point2D.Double(((double)(actual.getPosition().x-offset.x))/pixelpercm,((double)(max.y - actual.getPosition().y))/pixelpercm);
	    	s.write(drawCircle(drawpoint.getX(),drawpoint.getY(),((double)actual.getSize())/(2*pixelpercm),produceColor(actual)));
	    	if (actual.isNameVisible()) //draw name
			{					
	    		//mittelpunkt des Textes
				double x = drawpoint.getX() + (((double)actual.getNameDistance())/pixelpercm) * Math.cos(Math.toRadians((double)actual.getNameRotation()));
				//Invert y
				double y = drawpoint.getY() + (((double)actual.getNameDistance())/pixelpercm) * Math.sin(Math.toRadians((double)actual.getNameRotation()));
				//TODO Size?
	    		s.write(NL+"\\pgftext[x="+x+"cm,y="+y+"cm]{"+formname(nodenames.get(actual.getIndex()))+"}");
			}
		}
	}
	private String formname(String ur)
	{
		ur = replace(ur,"#","\\#");
		return "$"+ur+"$";
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
	    		GeneralPath p = actual.getPath(nodes.get(start).getPosition(),nodes.get(ende).getPosition(),1.0f); //without zoom nor line style!
	    		PathIterator path = p.getPathIterator(null, 5d/pixelpercm); 
	    		s.write(NL+drawOnePath(path, actual.getWidth(), produceColor(actual), produceLineSpec(actual.getLinestyle(),actual.getWidth())));
	    		//edge text
		    	if (actual.getTextProperties().isVisible()) //draw name
				{	
		    		VEdgeText t = actual.getTextProperties();
		    		Point m = actual.getTextCenter(nodes.get(start).getPosition(),nodes.get(ende).getPosition());
		    		//get the text wich should be displayd
				    String text = "";
				    if (t.isshowvalue())
						text = ""+me.Value;
				    else
				    	text = edgenames.get(actual.getIndex());
		    		//mittelpunkt des Textes
					double x = (m.getX()-offset.getX())/pixelpercm;
					//Invert y
					double y = (max.getY() - m.getY())/pixelpercm;
					//TODO Size?
		    		s.write("\\pgftext[x="+x+"cm,y="+y+"cm]{"+formname(text)+"}");
				}

			   //Nun die Liniendicke aufbauen
	    	  //Nun noch den Pfeil
			  s.write(drawArrow(actual,start,ende));
	       }//End while edges.hasNext()
	}
	private void writeHyperEdges(OutputStreamWriter s) throws IOException
	{
	       //Nodes
	    	Iterator<VHyperEdge> hyperedgeiter = hyperedges.getIterator();
	    	while (hyperedgeiter.hasNext())
	    	{
	    	   VHyperEdge actual = hyperedgeiter.next();
	    	   MHyperEdge me = mhyperedges.get(actual.getIndex());
			   //Mittlere Linie der Kante...immer Zeichnen
	    	   double res = Math.max(0.25d,75d/pixelpercm);
	    		GeneralPath p = actual.getShape().getCurve(res);
	     		PathIterator path = p.getPathIterator(null, 0.5d/pixelpercm); 
	    		// 0.005/sizeppt =  = the flatness; reduce if result is not accurate enough!
	    		s.write(drawOnePath(path, actual.getWidth(),produceColor(actual),produceLineSpec(actual.getLinestyle(),actual.getWidth())));
	    		if (actual.getTextProperties().isVisible())
	    		{
	    			//edge text
	    			VEdgeText t = actual.getTextProperties();
	    			Point m = actual.getTextCenter();
	    			//get the text wich should be displayd
	    			String text = "";
	    			if (t.isshowvalue())
	    				text = ""+me.Value;
	    			else
	    				text = edgenames.get(actual.getIndex());
	    			//mittelpunkt des Textes
	    			double x = (m.getX()-offset.getX())/pixelpercm;
	    			//Invert y
	    			double y = (max.getY() - m.getY())/pixelpercm;
	    			//TODO Size?
	    			s.write("\\pgftext[x="+x+"cm,y="+y+"cm]{"+formname(text)+"}");
	    		}
	       }//End while hyperedges.hasNext()
	}

	public String drawOnePath(PathIterator path, int linewidth, String ColorSpec, String LineSpec)
	{
		String s ="";
	   	double[] coords = new double[2];
	   	double x = 0.0, y = 0.0, lastx=0.0, lasty = 0.0;
		boolean start=true;
	    while( !path.isDone() ) 
		{
		   int type = path.currentSegment(coords);
		    x = coords[0]; y = coords[1];
		    if ((x!=lastx)||(y!=lasty))
		    {
		    	if (type==PathIterator.SEG_MOVETO)
		    	{
		    		if (!start)
		    			s +=";";
		    		s +=NL+"\\path["+LineSpec+",draw="+ColorSpec+", line width=";
		    		if (linesinPT)
		    			s += linewidth+"pt";
		    		else
		    			s += (((double)linewidth)/pixelpercm)+"cm";
		    		
		    		s+= "] ("+(x-offset.x)/pixelpercm+","+(max.y-y)/pixelpercm+")";
		    	}
		    	else if (type==PathIterator.SEG_LINETO)	
		    	{
	    			s += " -- ";
					s += " ("+(x-offset.x)/pixelpercm+","+(max.y-y)/pixelpercm+")";
		    	}
		    	else
		    		System.err.println("Unknown Type:"+type);
		   }
		   lastx = x; lasty = y;
		   if (start)
			   start=false;
		   path.next();
		 }
	    s +=";";
		return s;
	}
	/**
	 * 
	 * @param actual
	 * @param start
	 * @param ende
	 * @return
	 */
	private String drawArrow(VEdge actual, int start, int ende)
	{
		String s = "";
		if (directed)
		{
		  	Shape arrow = actual.getArrowShape(nodes.get(start).getPosition(),nodes.get(ende).getPosition(),Math.round(nodes.get(start).getSize()/2),Math.round(nodes.get(ende).getSize()/2),1.0f);
		  	PathIterator path = arrow.getPathIterator(null, 0.001);
		  	s += drawOnePath(path, actual.getWidth(), produceColor(actual),"solid, fill="+produceColor(actual));
		}
		return s;
	}
	private void writeSubgraphs(OutputStreamWriter s) throws IOException
	{
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
	       if (edges!=null)
	    	   writeEdges(out);
	       else if (hyperedges!=null)
	    	   writeHyperEdges(out);
	       writeNodes(out);
	       writeSubgraphs(out);
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
	private String drawCircle(double x, double y, double s, String color)
	{	
		return NL+"\\fill[fill="+color+"] ("+x+","+y+") circle ("+s+");";
	}
	public String drawCircle(double x, double y, double s, String fillcolor,String drawcolor)
	{	
		double x1 = (x-offset.getX())/pixelpercm;
		double y1 = (max.getY()-y)/pixelpercm;
		if (fillcolor.equals(""))
			return NL+"\\fill[draw="+drawcolor+"] ("+x1+","+y1+") circle ("+s+");";
		else if (drawcolor.equals(""))
			return NL+"\\fill[fill="+fillcolor+"] ("+x1+","+y1+") circle ("+s+");";			
		
		return NL+"\\fill[fill="+fillcolor+",draw="+drawcolor+"] ("+x1+","+y1+") circle ("+s+");";			
		
	}
	/* (non-Javadoc)
	 * @see io.TeXWriter#isWholeDoc()
	 */
	public boolean isWholeDoc() {
		return wholedoc;
	}

	/* (non-Javadoc)
	 * @see io.TeXWriter#setWholedoc(boolean)
	 */
	public void setWholedoc(boolean wholedoc) {
		this.wholedoc = wholedoc;
	}
}
