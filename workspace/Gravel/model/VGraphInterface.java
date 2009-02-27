package model;

import java.awt.Graphics;
import java.awt.Point;

import model.Messages.GraphConstraints;

public interface VGraphInterface {

	
	public final int GRAPH = GraphConstraints.VISUAL|GraphConstraints.GRAPH;
	public final int HYPERGRAPH = GraphConstraints.VISUAL|GraphConstraints.HYPERGRAPH;

	/**
	 * deselect all Nodes and Edges
	 */
	public abstract void deselect();

	/**
	 * deletes all selected Nodes and (Hyper)Edges. That means, that also all incident Edges of selected Nodes are deleted
	 */
	public abstract void removeSelection();

	/**
	 * Translates the Graph by the given Offset in x and y direction
	 * <br><br>
	 * @param x Translation on the X-axis
	 * @param y Translation on the Y-axis
	 */
	public abstract void translate(int x, int y);

	/**
	 * returns the maximum point that is used by the VGraph.
	 * <br>On nodes the size of the node is included
	 * <br>On Edges the control point is included 
	 * 
	 * @return Maximum as a point
	 */
	public abstract Point getMaxPoint(Graphics g);

	/**
	 * returns the minimum point that is used by the VGraph.
	 * <br>On nodes the size of the node and the size of the text is included
	 * <br>On Edges the control point is included
	 * <br>The Graphics are needed to compute the fontsize
	 * <br>Zoom is not encalculated 
	 * <br>
	 * @param the Graphic in which the Graph lies. 
	 * @return Point MinPoint
	 */
	public abstract Point getMinPoint(Graphics g);

	/**
	 * informs all subscribers about a change. This Method is used to push a notify from outside
	 * mit dem Oject o als Parameter
	 */
	public abstract void pushNotify(Object o);

	public abstract int getType();

}