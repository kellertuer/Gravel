package model;

import java.awt.Point;
import java.awt.geom.GeneralPath;
/**
 * An Orthogonal Edge consists of two lines, one is horizontal and one is vertical.
 * 
 * @author ronny
 *
 */
public class VOrthogonalEdge extends VEdge {

	private boolean verticalfirst;
	/**
	 * Initialize the Orthogobal Edge
	 * @param i index
	 * @param w linewidth
	 * @param v true for beginning with the vertical line else false
	 */
	public VOrthogonalEdge(int i, int w, boolean v)
	{
		super(i,w);
		verticalfirst = v;
	}

	public GeneralPath getPath(Point Start, Point End,float zoom) {
		GeneralPath p = new GeneralPath();
		p.moveTo(Start.x*zoom,Start.y*zoom);
		if (verticalfirst)
		{ //Also zuerst y anpassen
			p.lineTo(Start.x*zoom,End.y*zoom);
			p.lineTo(End.x*zoom,End.y*zoom);
		}
		else
		{	//Sonst erst Horizontal also zuerst x 
			p.lineTo(End.x*zoom,Start.y*zoom);
			p.lineTo(End.x*zoom,End.y*zoom);
		}
		return p;
	}
	/**
	 * Indicates whether the first line from start node is the vertical or the horizontal line
	 * @return true if vertical else false
	 */
	public boolean getVerticalFirst()
	{
		return verticalfirst;
	}
	/**
	 * Set the Indicator
	 * @param v to true for vertical line at start, else false
	 */
	public void setVerticalFirst(boolean v)
	{
		verticalfirst = v;
	}
	@Override
	public int getType() {
		return VEdge.ORTHOGONAL;
	}
	public void translate(int x,int y){}
	public Point getMax()
	{	//Kein Kontrollpunkt also ein Minimum zur�ckgeben
		return new Point(0,0);
	}
	public Point getMin()
	{	//Kein Kontrollpunkt also ein Maximum zur�ckgeben
		return new Point(Integer.MAX_VALUE,Integer.MAX_VALUE);
	}
	public VEdge clone()
	{
		VEdge cloneedge = new VOrthogonalEdge(getIndex(),width,verticalfirst);
		return copyCommonProperties(cloneedge);
	}
	public boolean PathEquals(VEdge v)
	{
		if (v.getType()!=VEdge.ORTHOGONAL)
			return false; //not the same type
		//both are orthogonal edges so thea are equal if both have the same verticalfirst
		return (verticalfirst==(((VOrthogonalEdge)v).getVerticalFirst()));
	}
}
