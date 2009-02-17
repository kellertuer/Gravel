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
import java.awt.Color;


import view.VGraphic;
/**
 * each node is written to the tex file and translated therefore
 * x - offset to get rid of the free space left of the graph
 * maxy - y to invert y (no offset substractiion needed, because 0 is max and max.y - offset.y is the maximum to be reached
 * @author ronny
 *
 */
public class MyTikZPictureWriter implements TeXWriter {

	private final static String NL = "\r\n";
	VGraphic vgc;
	VGraph vg;
	GeneralPreferences gp;
	Point max,offset;
	private int width;
	double sizeppt; //Groeße die ein punkt/pixel ind er Grafik dann in mm aufm Papier hat
	private boolean wholedoc;
	/**
	 * Starts the LaTeX-Export with some parameters
	 * @param a_picture a given VGraph in an VGraphic-Environment
	 * @param w width of the picture in LaTeX in mm
	 * @param type eiter "doc" for al whole LaTeX-Document or "fig" for just the figure
	 */
	public MyTikZPictureWriter(VGraphic a_picture, int w, String type)
	{
		vgc = a_picture;
		vg = vgc.getVGraph();
		gp = GeneralPreferences.getInstance();
		width = w;
		if (type.equalsIgnoreCase("doc"))
			wholedoc = true;
		else
			wholedoc = false;		
	}
	
	//General
	
	private void writeHeader(OutputStreamWriter s, String filename) throws IOException
	{
		sizeppt = (double)width/(double)(vg.getMaxPoint(vgc.getGraphics()).x-vg.getMinPoint(vgc.getGraphics()).x);
		if (wholedoc)
		{
			s.write("%Exported Graph by Gravel"+NL+"% "+NL+"%to use in your own LaTeX-Document copy the tikzfigure into your file"+NL);
			s.write("%"+NL+"%This Document is minimized in its usage of packages"+NL);
			s.write(NL+"\\documentclass{scrartcl}"+NL);
			s.write("\\usepackage{tikz}"+NL);
			//s.write("\\setpapersize{A4}");
			s.write("\\begin{document}");
			s.write(NL+"\t%"+NL+"\t%Copy this Part into your Document"+NL+"\t%");
			//Compute the size and offset
		}
		else
		{
			s.write("% Export of a Graph by Gravel"+NL+"%"+NL+"% this document only contains the tikzfigure-Environment"+NL+"%"+NL+"% Usage : \\include{"+filename+"} to include this in your document"+NL+"% (if you haven't changed this filename and your main .tex-File is in the same directory"+NL);
		}
		s.write("% The \\caption{}-Element of the Figure is right at the end of the file");
		offset = vg.getMinPoint(vgc.getGraphics());
		max = vg.getMaxPoint(vgc.getGraphics());
		int x = max.x - offset.x; //Breite
		int y = max.y - offset.y; //Hoehe
		s.write(NL+"\t\\begin{figure}"+NL+"\t\\centering");				
		s.write(NL+"\t%If you change this size of the graphic the textsize won't change, so the letters appear bigger or smaller than with the exportet size."+NL+"\t%For changing size, a new export is reccomended.");
		s.write(NL+"\\setlength{\\unitlength}{"+sizeppt+"mm}");
		s.write(NL+"\t\\begin{tikzpicture}("+x+","+(y+1)+")(0,0)"+NL); //+kein Offset ist testweise
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
	
	//Visual
	/**
	 * Set a Color for each subgraph
	 * @return 
	 */
	private void writeColors(OutputStreamWriter s) throws IOException
	{
		Iterator<VSubSet> setiter = vg.getSubSetIterator();
		while (setiter.hasNext())
		{
			VSubSet actual = setiter.next();
			Color c = actual.getColor();
			
			s.write("\\definecolor{SubSet"+actual.getIndex()+"}{rgb}{"+
					((double)c.getRed())/255.0+","+
					((double)c.getGreen())/255.0+","+
					((double)c.getBlue())/255.0+"}");
		}
	}
	private String getNodeColorString(int nodeindex)
	{
		VNode v = vg.getNode(nodeindex);
		String c="";
		if (v==null)
			return c;
		//Count Subsets
		Iterator<VSubSet> setiter = vg.getSubSetIterator();
		int count=0;
		while (setiter.hasNext())
		{
			if (vg.getMathGraph().SubSetcontainsNode(nodeindex, setiter.next().getIndex()))
					count++;
		}
		int part = Math.round(100.0f/(float)count);
		setiter = vg.getSubSetIterator();
		while (setiter.hasNext())
		{ //Add the Color of the subset with the part it takes in the color
			c += "SubSet"+setiter.next().getIndex()+"!"+part;
		}
		return c;
	}
	private void writeNodes(OutputStreamWriter s) throws IOException
	{
	    //Nodes
	    Iterator<VNode> nodeiter = vg.getNodeIterator();
	    while (nodeiter.hasNext())
	    {
	    	VNode actual = nodeiter.next();
			s.write(NL+"\t\t\\node[circle,fill="+getNodeColorString(actual.getIndex())+"]"+
					"(ID"+actual.getIndex()+") at ("+actual.getPosition().x+","+(max.y - actual.getPosition().y)+")");
	    	if (actual.isNameVisible()) //draw name
			{	
				s.write("{$"+formname(vg.getMathGraph().getNode(actual.getIndex()).name)+"$};");
				//TODO Text
				//{\\makebox(0,0){\\fontsize{"+tsize+"mm}{10pt}\\selectfont "+formname(vg.getNodeName(actual.index))+"}}");
			}
	    	else
	    		s.write("{}");
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
	    	Iterator<VEdge> edgeiter = vg.getEdgeIterator();
	    	while (edgeiter.hasNext())
	    	{
	    	   VEdge actual = edgeiter.next();
	    	   MEdge me = vg.getMathGraph().getEdge(actual.getIndex());
	    	   
	    	   int start = me.StartIndex;
	    	   int ende = me.EndIndex;
	    	   s.write(NL+"\t\t\\draw[line with="+actual.getWidth());
    		   if (vg.getMathGraph().isDirected())
    			   s.write(",->");
    		   else
    			   s.write(",--");
    		   switch (actual.getLinestyle().getType())
    		   {
    			   case VEdgeLinestyle.DASHED:
    			   {
    				   s.write(",dash pattern=on "+actual.getLinestyle().getLength()+"pt off "+actual.getLinestyle().getDistance()+"pt");
	    			   break;
    			   }
	    		   case VEdgeLinestyle.DOTTED:
	    		   {
	    			   s.write(",dash pattern=on 1pt off "+actual.getLinestyle().getDistance()+"pt");
	    		   		break;
	    		   }
	    		   case VEdgeLinestyle.DOTDASHED:
	    		   {
	    			   s.write(",dash pattern=on "+actual.getLinestyle().getLength()+"pt off "+actual.getLinestyle().getDistance()+"pt on 1pt off"+actual.getLinestyle().getDistance()+"pt");
	    			   break;
	    		   } 
	    		   case VEdgeLinestyle.SOLID: 
	    		   default:
	    		   {
	    			 s.write(",solid");
	    			 break;
	    			}
	    		} //end of switch
    		   	s.write("]  (ID"+start+")");
    		   	if (actual.getType()==VEdge.ORTHOGONAL)
    		   	{
    		   		if (((VOrthogonalEdge)actual).getVerticalFirst())
    		   			s.write(" |- ");
    		   		else
    		   			s.write(" -| ");
    		   	}
    		   	else if (actual.getType()==VEdge.QUADCURVE)
    		   	{
    		   		int px = ((VQuadCurveEdge)actual).getControlPoints().firstElement().x;
    		   		int py = ((VQuadCurveEdge)actual).getControlPoints().firstElement().y;
    		   		s.write(".. controls ("+px+","+(max.y-py)+") ..");
    		   	}
    		   	else if (actual.getType()==VEdge.SEGMENTED)
    		   	{
    		   		Iterator <Point> p = ((VSegmentedEdge)actual).getControlPoints().iterator();
    		   		while (p.hasNext())
    		   		{
    		   			Point acp = p.next();
    		   			s.write(" ("+acp.x+","+(max.y-acp.y)+") ");
    		   		}
    		   	}
    		   	s.write(" (ID"+ende+");");	
	    }//End iter over all edges
	    		
	}
	
	private void writeSubSets(OutputStreamWriter s) throws IOException
	{
	       //SubSets - Umrandungen hier einbauen
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
		vgc.setZoomEnabled(false);
	       OutputStream fout= new FileOutputStream(f);
	       OutputStream bout= new BufferedOutputStream(fout);
	       OutputStreamWriter out = new OutputStreamWriter(bout, "UTF8");
	       writeHeader(out,f.getName());
	       writeColors(out);
	       writeNodes(out);
	       writeEdges(out);
	       writeSubSets(out);
	       writeFooter(out,"Gravel Graphen-Export '"+f.getName()+"'");
	       out.flush();  // Don't forget to flush!
	       out.close();
	       vgc.setZoomEnabled(true);
		}
		catch (Exception e)
		{
			return "Fehler beim schreiben: "+e;
		}
		return "";
	}
	public boolean isWholeDoc() {
		return wholedoc;
	}
	public void setWholedoc(boolean wholedoc) {
		this.wholedoc = wholedoc;
	}
}
