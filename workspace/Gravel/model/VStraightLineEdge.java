package model;

import java.awt.Point;
import java.awt.geom.GeneralPath;
/**
 * The Straight Line Edge is the standard Edge Type, it only implements abstract methods of VEdge
 * @author ronny
 *
 */
public class VStraightLineEdge extends VEdge
{
	/**
	 * Initialize the VEdge with an index and a linewidth
	 * @param i
	 * @param w
	 */
	public VStraightLineEdge(int i, int w) {
		super(i, w);
	}
	public GeneralPath getPath(Point p1, Point End,float zoom) {
		GeneralPath p = new GeneralPath();
		p.moveTo(p1.x*zoom,p1.y*zoom);
		p.lineTo(End.x*zoom,End.y*zoom);
		return p;
		}
	public int getEdgeType() {
		return VEdge.STRAIGHTLINE;
	}
	public void translate(int x, int y)
	{}
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
		VEdge cloneedge = new VStraightLineEdge(this.getIndex(),this.width);
		return copyCommonProperties(cloneedge);
	}
	public boolean PathEquals(VEdge v)
	{
		//two straight line edges share the same path (if start and end are equal) so the test is only the type
		return (v.getEdgeType()==VEdge.STRAIGHTLINE);
	}
}
