package io;

import java.awt.Point;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;

public class DiplExports {
	private final static String NL = "\r\n";
	private final static double LINESPPT = 4.0d;

	
	public static String drawOnePath(PathIterator path, int linewidth, double distancefromline, Point offset, Point max, boolean CP)
	{
		String s ="";
	   	double[] coords = new double[2];
	   	double x = 0.0, y = 0.0, lastx=0.0, lasty = 0.0;
		int testcount=0; //don't let paths grow tooo long
		boolean moved=false;
		Point2D.Double dir, orth_n_h;
		double dlength=0.0d,movx=0.0d,movy=0.0d;
	    while( !path.isDone() ) 
		{
		   int type = path.currentSegment(coords);
		    x = coords[0]; y = coords[1];
		    if ((x!=lastx)||(y!=lasty))
		    {
		    	if (type==PathIterator.SEG_MOVETO)
				{
					moved = true;
				}
		    	else if (type==PathIterator.SEG_LINETO)	
		    	{
		    		if (moved)
		    		{
		    			moved = false;
		    			if (CP)
		    				s += "\\draw[line width=0.9pt, draw=maincolormiod]";
		    			else
		    				s += "\\draw[line width=2pt, draw=black] ";
		    		}
		    		dir = new Point2D.Double(x - lastx,y-lasty);
			    	dlength = LINESPPT*Math.sqrt(dir.x*dir.x + dir.y*dir.y);
					orth_n_h = new Point.Double (dir.y/dlength, (-1.0d)*dir.x/dlength);
					movx = orth_n_h.x*distancefromline;
			    	movy = orth_n_h.y*distancefromline;
		    		s +=("("+(lastx+movx-offset.x)+","+(max.y-lasty-movy)+") -- ");
			    	s +=("("+(x+movx-offset.x)+","+(max.y-y-movy)+") -- ");
		    	}
		   }
		   else
		   {
			   if (distancefromline==0.0d)
			   {
				if (!CP)   //Circle with diameter linewidth
				   s += drawFullCircle(x-offset.x,max.y-y,linewidth);
				else
				{
					s += drawFullCircle(x-offset.x,max.y-y,1.5);
				}
				moved=true;
			   }
		   }
		   lastx = x; lasty = y;
		   path.next();
		 }
		return s;
	}
	public static String drawFullCircle(double x, double y, double s)
	{	
		return NL+"\\fill[fill=white,draw=black] ("+x+","+y+") circle ("+s+")";
/*		String str="";
		str += NL+"\\thicklines";
		for (int i=s-1; i>=0; i--)
		{
			str += NL+"\\put("+(x)+","+(y)+"){\\color{white}\\circle*{"+i+"}}";
		}
		str += "\\thinlines"+
		NL+"\\put("+x+","+y+"){\\color{black}\\circle{3}}";
		return str;
*/	}
	public static String drawQuad(double x, double y, double s)
	{
		String str = "";
		str += NL+"\\filldraw[fill=white] ("+(x-s)+","+(y-s)+") -- ("+(x+s)+","+(y-s)+") -- ("+(x+s)+","+(y+s)+") -- ("+(x-s)+","+(y+s)+") -- ("+(x-s)+","+(y-s)+");";
		return str;
	}
	public static String drawCircle(double x, double y, int s)
	{	
		return NL+"\\put("+x+","+y+"){\\circle{"+s+"}}";
	}

}
