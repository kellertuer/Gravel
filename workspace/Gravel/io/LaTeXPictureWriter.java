package io;

import model.*;

import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Iterator;

import view.VGraphic;
/**
 * each node is written to the tex file and translated therefore
 * x - offset to get rid of the free space left of the graph
 * maxy - y to invert y (no offset substractiion needed, because 0 is max and max.y - offset.y is the maximum to be reached
 * @author ronny
 *
 */
public class LaTeXPictureWriter implements TeXWriter {

	private final static String NL = "\r\n";
	private final static double LINESPPT = 4.0d;
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
	public LaTeXPictureWriter(VGraphic a_picture, int w, String type)
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
			s.write("%Exported Graph by Gravel"+NL+"% "+NL+"%to use in your own LaTeX-Document copy the figure into your file"+NL);
			s.write("%"+NL+"%This Document is minimized in its usage of packages"+NL);
			s.write(NL+"\\documentclass{scrartcl}"+NL);
			s.write("\\usepackage{epic}"+NL);
			s.write("\\usepackage{eepic}"+NL);
			//s.write("\\setpapersize{A4}");
			s.write("\\begin{document}");
			s.write(NL+"\t%"+NL+"\t%Copy this Part into your Document"+NL+"\t%");
			//Compute the size and offset
		}
		else
		{
			s.write("% Export of a Graph by Gravel"+NL+"%"+NL+"% this document only contains the figure-Environment"+NL+"%"+NL+"% Usage : \\include{"+filename+"} to include this in your document"+NL+"% (if you haven't changed this filename and your main .tex-File is in the same directory"+NL);
		}
		s.write("% The \\caption{}-Element of the Figure is right at the end of the file");
		offset = vg.getMinPoint(vgc.getGraphics());
		max = vg.getMaxPoint(vgc.getGraphics());
		int x = max.x - offset.x; //Breite
		int y = max.y - offset.y; //Hoehe
		s.write(NL+"\t\\begin{figure}"+NL+"\t\\centering");				
		s.write(NL+"\t%If you change this size of the graphic the textsize won't change, so the letters appear bigger or smaller than with the exportet size."+NL+"\t%For changing size, a new export is reccomended.");
		s.write(NL+"\\setlength{\\unitlength}{"+sizeppt+"mm}");
		s.write(NL+"\t\\begin{picture}("+x+","+(y+1)+")(0,0)"+NL); //+kein Offset ist testweise
	}

	private void writeFooter(OutputStreamWriter s, String name) throws IOException
	{
		String name_escaped = replace(name,"_","\\_");
		name_escaped = replace(name_escaped,"&","\\&");
		s.write(NL+"\t\\end{picture}"+NL);
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
	
	private void writeNodes(OutputStreamWriter s) throws IOException
	{
	    //Nodes
	    Iterator<VNode> nodeiter = vg.modifyNodes.getNodeIterator();
	    while (nodeiter.hasNext())
	    {
	    	VNode actual = nodeiter.next();
	    	Point drawpoint = new Point(actual.getPosition().x-offset.x,max.y - actual.getPosition().y);
	    	s.write(drawCircle(drawpoint,actual.getSize()));
	    	if (actual.isNameVisible()) //draw name
			{	
				//mittelpunkt des Textes
				int x = drawpoint.x + Math.round((float)actual.getNameDistance()*(float)Math.cos(Math.toRadians((double)actual.getNameRotation())));
				//Invert y
				int y = drawpoint.y + Math.round((float)actual.getNameDistance()*(float)Math.sin(Math.toRadians((double)actual.getNameRotation())));
				double tsize = Math.round((double)actual.getNameSize()*sizeppt*((double)1000))/1000;
				s.write(NL+"\t\t\\put("+x+","+y+"){\\makebox(0,0){\\fontsize{"+tsize+"mm}{10pt}\\selectfont "+formname(vg.getMathGraph().getNode(actual.getIndex()).name)+"}}");
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
	    	Iterator<VEdge> edgeiter = vg.modifyEdges.getEdgeIterator();
	    	while (edgeiter.hasNext())
	    	{
	    	   VEdge actual = edgeiter.next();
	    	   MEdge me = vg.getMathGraph().getEdge(actual.getIndex());
//	    	   Vector<Integer> values = vg.getEdgeProperties(actual.getIndex());
	    	   int start = me.StartIndex;
	    	   int ende = me.EndIndex;
	    	   //not needed int value = values.elementAt(MGraph.EDGEVALUE);
			   //Mittlere Linie der Kante...immer Zeichnen
			   s.write(NL+drawOneEdgeLine(actual,start,ende,0.0d));
			   if (actual.getWidth()>1)
			   s.write("\\thicklines");
			   for (int i=1; i<(actual.getWidth()-1)*(new Double(Math.round(LINESPPT/2))).intValue(); i++)
			   {
				   //One Side
				   s.write(drawOneEdgeLine(actual,start,ende,(new Double(i)).doubleValue()));
				   //Other Side
				   s.write(drawOneEdgeLine(actual,start,ende,(new Double(-i)).doubleValue()));				   
			   }
			   //edge text
		    	if (actual.getTextProperties().isVisible()) //draw name
				{	
		    		VEdgeText t = actual.getTextProperties();
					//mittelpunkt des Textes herausfinden
		    		int pos; boolean top; double part;
					if (t.getPosition() > 50)
					{ //below edge
						pos = t.getPosition() - 50;
						top = false;
						part = 1-((double)pos)*2.0d/100.0d; //from the end - so 1- at the part
					}
					else
					{
						pos = t.getPosition();
						top = true;
						part = ((double)pos)*2.0d/100.0d;
					}
					Point p = actual.getPointonEdge(vg.modifyNodes.getNode(start).getPosition(),vg.modifyNodes.getNode(ende).getPosition(), part);
					Point2D.Double dir = actual.getDirectionatPointonEdge(vg.modifyNodes.getNode(start).getPosition(),vg.modifyNodes.getNode(ende).getPosition(), part);
					double l = dir.distance(0.0d,0.0d);
					//and norm dir
					dir.x = dir.x/l; dir.y = dir.y/l;
					//And now from the point on the edge the distance
					Point m = new Point(0,0); //middle of the text
					if (top) //Countter Clockwise rotation of dir
					{
						m.x = p.x + (new Long(Math.round(((double)t.getDistance())*dir.y)).intValue());
						m.y = p.y - (new Long(Math.round(((double)t.getDistance())*dir.x)).intValue());				
					}
					else //invert both direction elements
					{
						m.x = p.x - (new Long(Math.round(((double)t.getDistance())*dir.y)).intValue());
						m.y = p.y + (new Long(Math.round(((double)t.getDistance())*dir.x)).intValue());				
					}
					//get the text wich should be displayd
				    String text = "";
				    if (t.isshowvalue())
						text = ""+me.Value;
				    else
				    	text = vg.getMathGraph().getEdge(actual.getIndex()).name;
				    double tsize = Math.round((double)t.getSize()*sizeppt*((double)1000))/1000;
					s.write(NL+"\t\t\\put("+(m.x-offset.x)+","+(max.y-m.y)+"){\\makebox(0,0){\\fontsize{"+tsize+"mm}{10pt}\\selectfont "+formname(text)+"}}");
				}

			   //Nun die Liniendicke aufbauen
	    	  //Nun noch den Pfeil
			  s.write(drawArrow(actual,start,ende));
	       }//End while edges.hasNext()
	}
	/** DRaw an Edge from Start to ende with the offset movx, movy
	 * 
	 * @param actual
	 * @param start
	 * @param ende
	 * @param movx
	 * @param movy
	 * @return The Picture Enviroment String for the drawing
	 */
	private String drawOneEdgeLine(VEdge actual,int start, int ende, double distancefromline)
	{
		String s ="";
    	double[] coords = new double[2];
    	double x = 0.0, y = 0.0, lastx=0.0, lasty = 0.0;
		GeneralPath p = actual.getDrawPath(vg.modifyNodes.getNode(start).getPosition(),vg.modifyNodes.getNode(ende).getPosition(),1.0f); //no zoom on check! But with line style
		PathIterator path = p.getPathIterator(null, 0.005d/sizeppt); 
		// 0.005/sizeppt =  = the flatness; reduce if result is not accurate enough!
		Point2D.Double dir, orth_n_h;
		double dlength=0.0d,movx=0.0d,movy=0.0d;
		int testcount=0; //don't let paths grow tooo long
		boolean moved=false;
		while( !path.isDone() ) 
		{
		   int type = path.currentSegment(coords);
		    x = coords[0]; y = coords[1];
		    if ((x!=lastx)||(y!=lasty))
		    {
		    	if (++testcount > 150)
		    	{
		    		moved = true;
		    		testcount=0;
		    	}
		    	if (type==PathIterator.SEG_MOVETO)
				{
					moved = true;
					
				}
		    	else if (type==PathIterator.SEG_LINETO)	
		    	{
		    		if (moved)
		    		{
		    			moved = false;
		    			s +=NL+"\\path";
		    		}
		    		dir = new Point2D.Double(x - lastx,y-lasty);
			    	dlength = LINESPPT*Math.sqrt(dir.x*dir.x + dir.y*dir.y);
					orth_n_h = new Point.Double (dir.y/dlength, (-1.0d)*dir.x/dlength);
					movx = orth_n_h.x*distancefromline;
			    	movy = orth_n_h.y*distancefromline;
		    		s +=("("+(lastx+movx-offset.x)+","+(max.y-lasty-movy)+")");
			    	s +=("("+(x+movx-offset.x)+","+(max.y-y-movy)+")  ");
		    	}
		   }
		   else
		   {
			   if (distancefromline==0.0d)
			   {
				   //Circle with diameter linewidth
				   s += drawCircle(x-offset.x,max.y-y,actual.getWidth());
			   }
		   }
		   lastx = x; lasty = y;
		   path.next();
		 }
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
		double[] coords = new double[2];
		double x = 0.0, y = 0.0;
		Point2D.Double arrowhead = new Point2D.Double(),line1start = new Point2D.Double(),line1 = new Point2D.Double(),line2start = new Point2D.Double(),line2 = new Point2D.Double();
		String s = "";
		if (actual.getWidth()>1) //Dann wurde die kante mit Thicklines verbreitert -> wiederzurück zu thin
			s += NL+"\\thinlines";
		if (vg.getMathGraph().isDirected())
		{
		  	Shape arrow = actual.getArrowShape(vg.modifyNodes.getNode(start).getPosition(),vg.modifyNodes.getNode(ende).getPosition(),Math.round(vg.modifyNodes.getNode(start).getSize()/2),Math.round(vg.modifyNodes.getNode(ende).getSize()/2),1.0f);
		  	PathIterator path = arrow.getPathIterator(null, 0.001);
		  	int i=0;
		    while( !path.isDone() ) 
		      {
		      int type = path.currentSegment(coords);
		    	x = coords[0]; y = coords[1];
		    	if (type==PathIterator.SEG_MOVETO)
		    	{
		    		s += NL+"\\path("+(x-offset.x)+","+(max.y-y)+")";
		    		arrowhead = new Point2D.Double(x-offset.x,max.y-y);
		    	}
		    	else if (type==PathIterator.SEG_LINETO)	
		    	{
		    		s += "("+(x-offset.x)+","+(max.y-y)+")";
		    		if (i==0)
		    			line1start = new Point2D.Double(x-offset.x,max.y-y);
		    		else if (i==1)
		    		{
		    			line2start = new Point2D.Double(x-offset.x,max.y-y);
		    			line1 = new Point2D.Double(line2start.x-line1start.x,line2start.y-line1start.y);
		    		}
		    		else if (i==2)
		    		{
		    			line2 = new Point2D.Double((double)(x-offset.x)-line2start.x,(double)(max.y-y) - line2start.y);		    			
		    		}
		    		i++;	
		    	}
		    	path.next();
		    }
		    //How many lines on one of the two parts ?
		    int maxcount = (new Double (Math.round(actual.getArrow().getSize()*LINESPPT/sizeppt))).intValue();
		    //Shorten the direction vectors to a 1/maxcount th of the length
		    line1.x = line1.x/maxcount; line1.y = line1.y/maxcount;
		    line2.x = line2.x/maxcount; line2.y = line2.y/maxcount;
		    s += "\\path("+arrowhead.x+","+arrowhead.y+")("+line2start.x+","+line2start.y+")"; //Middle line in Thick
		    for (int j=1; j<maxcount; j++)
		    {
		    	s += "\\path("+arrowhead.x+","+arrowhead.y+")("+(line1start.x+line1.x*(double)j)+","+(line1start.y+line1.y*(double)j)+")"; //Middle line in Thick
		    	s += "\\path("+arrowhead.x+","+arrowhead.y+")("+(line2start.x+line2.x*(double)j)+","+(line2start.y+line2.y*(double)j)+")"; //Middle line in Thick
		    }
		}
		return s;
	}
	private void writeSubSets(OutputStreamWriter s) throws IOException
	{
	       //SubSets - Umrandungen hier einbauen
	}
	/* (non-Javadoc)
	 * @see io.TeXWriter#saveToFile(java.io.File)
	 */
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
	       writeEdges(out);
	       writeNodes(out);
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
	private String drawCircle(Point m, int s)
	{	
		return NL+"\\put("+m.x+","+m.y+"){\\circle*{"+s+"}}";
	}
	private String drawCircle(double x, double y, int s)
	{	
		return NL+"\\put("+x+","+y+"){\\circle*{"+s+"}}";
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
