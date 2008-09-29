package model;

import io.GeneralPreferences;

import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;

/**
 * Class to represent the different Styles an Edge might have.
 * And the functions do manipulate a given edge-path to the style
 * 
 * Standard Style is Solid
 * 
 * Actual styles are : solid, dashed, dotted and dotdashed
 * for the dashed styles a dash-length (edge line length) mut be given
 * for dashed or dotted styles a distance between 2 Elements must be specified
 * 
 * @author Ronny Bergmann
 *
 */
public class VEdgeLinestyle {
	/*
	 * Preset Values for the Style
	 */
	public static final int SOLID = 1;
	public static final int DASHED = 2;
	public static final int DOTTED = 3;
	public static final int DOTDASHED = 4;
	
	//edge line length (of a dashed part)
	private int length;
	//Distance dash-dash / dot-dot / dash-dot / dot-dash
	private int distance;
	//type of line, must be one of the above defined final ints
	private int type;
	/**
	 * Create new line style with standard values
	 *
	 */
	public VEdgeLinestyle()
	{
		setType(GeneralPreferences.getInstance().getIntValue("edge.line_type"));
		length = GeneralPreferences.getInstance().getIntValue("edge.line_length");
		distance = GeneralPreferences.getInstance().getIntValue("edge.line_distance");
	}
	/**
	 * Create a new Linestyle with
	 * @param t Type
	 * @param l length of a dash
	 * @param d distance between dashes and/or dots
	 */
	public VEdgeLinestyle(int t, int l, int d)
	{
		setType(t);
		length = l;
		distance = d;
	}
	/**
	 * 
	 */
	public GeneralPath modifyPath(GeneralPath p, int edgewidth, double zoomfactor)
	{
		if (type==SOLID)
			return p;
		//Sonst entlanggehen und zerstückeln
		GeneralPath modified = new GeneralPath();
		//dash, dot, gap und gap2 sind indikatoren, was gerade behandelt wird.
		//Dash : ein Strich wird durchlaufen
		//Dot : Ein Punkt wird durchlaufen
		//gap: Eine Lücke vor einem punkt
		//gap2 : Eine Lücke vor einem neuen strich 
		boolean dash = false, dot = false, gap = false, gap2=false;
		double zoomlength = (new Double(length)).doubleValue()*zoomfactor;
		double zoomdistance = (new Double(distance)).doubleValue()*zoomfactor;
		double zoomwidth = 0.0d; //(new Double(edgewidth)).doubleValue()*zoomfactor/(2.0d);
		
		double sumdistance = 0.0d;
    	
		if ((type==DASHED)||(type==DOTDASHED))
		{
			dash = true;
			sumdistance += zoomlength; //erster Punkt bei der länge des strichs
		}
		if (type==DOTTED)
		{
			gap = true;
			sumdistance += zoomdistance; //erster Punkt bei der Länge des Abstands
		}
		double[] coords = new double[2];
    	double x = 0.0, y = 0.0, lastx=0.0, lasty = 0.0;
    	//Länge der Kante, length ist also die Gesamtlänge deiner Elipse
    	Point2D.Float p1=new Point2D.Float(0.0f,0.0f);
    	PathIterator pathiter = p.getPathIterator(null, 0.001); //0.001 ist die Genauigkeit
    	double pathlength = 0.0d; double seglength=0.0d;
    	//entlanglaufen
    	while( !pathiter.isDone() ) 
    	{
    		int pathtype = pathiter.currentSegment(coords);
    		x = coords[0]; y = coords[1];
    		if (pathtype==PathIterator.SEG_MOVETO)
    		{
    			modified.moveTo((new Double(x)).floatValue(),(new Double(y)).floatValue());
    		} //we need from the first moveto the coords as lastxlasty (mindestens der allererste Befehl istn MoveTo, wir machen da nix)
    		else if (pathtype==PathIterator.SEG_LINETO) //Wegstück
    		{
    			seglength = (new Point2D.Double(lastx,lasty)).distance(x,y);
    			pathlength += seglength;
    		}
    		if (pathlength>sumdistance) //auf dem aktuellen Wegstück liegt ein punkt des neuen Stylesch den Anteil des Wegstücks bedenken
    		{
    			while (pathlength > sumdistance) //alle weiteren Punkte auf diesem Wegstück
    			{
    				double restToPoint = sumdistance + seglength - pathlength; //restlength of on this seg
    				//System.err.println("("+dash+","+dot+","+gap+","+gap2+") "+restToPoint);
        				//double restFromPointToEnd = pathlength - restToPoint;
    				//From last only a part to x thats left from distance also Punktberechnung
    				p1 = new Point2D.Float((new Double(lastx + (x-lastx)*restToPoint/seglength)).floatValue(),(new Double(lasty + (y-lasty)*restToPoint/seglength)).floatValue());
    				if (dash) //doing a dash
    				{
    					dash = false;
    					modified.lineTo(p1.x, p1.y);
    					if (type==DASHED)
    						gap2 = true; //weiter nach der naechsten Luecke mit Strichen
    					if (type==DOTDASHED)
    						gap = true; //nach der naechsten Luecke weiter mit nem Punkt
    					//in beiden Faellen ist die Laenge distance
    					sumdistance+=zoomdistance;
    				}
    				else if (dot) //doing a dot
    				{
    					dot = false;
    					modified.lineTo(p1.x,p1.y);
    					if (type==DOTTED)
    						gap=true; 
    					if (type==DOTDASHED)
    						gap2=true; //nach der Luecke nen Strich
    					sumdistance += zoomdistance; //Auch hier wieder der Abstand der nu dazu solll draufaddiert
    				}
    				else if (gap)
    				{
    					gap = false; //Luecke vor dem Punkt fertig
    					modified.moveTo(p1.x,p1.y);
    					dot = true; //als nächstes am dot arbeiten
    					sumdistance += zoomwidth; //Dot = breit wie hoch
    				}
    				else if (gap2)
    				{
    					gap2=false;
    					modified.moveTo(p1.x,p1.y);
    					dash = true; //nach gap2 folgt ein strich
    					sumdistance += zoomlength; //mit seiner laenge
    				}
    			} //Ende des while im Segment, also liegt der nächste Punkt um nächsten Segment
    		} //Ende des Segmentes
    		//else //noch nicht am ende des Weges
    		
    			if (dash||dot) //weiter gehts, wobei das mit dot eher nicht vorkommt da der Länge 0 hat ;)
    				modified.lineTo((new Double(x)).floatValue(), (new Double(y)).floatValue());
    		
    		lastx = x; lasty = y;
    		pathiter.next(); //Next, so es den noch gibt :)
    		}
    		//Nach dem while steht also in p1 der Punkt, du kannst auch schon direkt nach der Berechnung abbrechen...
			return modified;
	}
	public VEdgeLinestyle clone()
	{
	  VEdgeLinestyle c = new VEdgeLinestyle();
	  c.setDistance(distance);
	  c.setLength(length);
	  c.setType(type);
	  return c;
	}
	/**
	 * 
	 * @return the actual line type. this is one of the above specified final values
	 */	
	public int getType() {
		return type;
	}
	/**
	 * Sets the type to one of the given (actually 4) line types
	 * if the new style is not known, the line style is not changed
	 *
	 * @param t new Style
	 */
	public void setType(int t) 
	{
		if ((t==DASHED)||(t==DOTTED)||(t==DOTDASHED)||(t==SOLID))
		{
			type = t;
		}
	}
	/**
	 * Distance between two line types, e.g.
	 * two dashs
	 * two dots
	 * a dash and a dot
	 * 
	 * 
	 * @return actual distance
	 */
	public int getDistance() {
		return distance;
	}
	/**
	 * Set the distance between two elements of the line (in px) to a new value.
	 * this distance is used betweeen
	 * 
	 * two dashs
	 * two dots
	 * a dash an da dot
	 * 
	 * depending on che chosen line type
	 * 
	 * @param distance
	 */
	public void setDistance(int distance) {
		this.distance = distance;
	}
	/**
	 * Length of a dashed element of the actual line
	 * 
	 * @return length in px
	 */
	public int getLength() {
		return length;
	}
	/**
	 * Sets the length in px  of a dashed part of the Line.
	 * Only used if a dashed style is chosen
	 * 
	 * @param length new dashlength
	 */
	public void setLength(int length) {
		this.length = length;
	}
}
