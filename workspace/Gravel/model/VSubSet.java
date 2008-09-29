package model;

import java.awt.Color;
import java.util.Iterator;

/**
 * A Visual SubSet contains the index for identification and the color 
 * @author Ronny Bergmannn
 */
public class VSubSet {
	//Farbe der Untermenge
	private Color colour;
	//INdex
	int index;
	//Referenz zum vGraphen
	VGraph vGraph;
	/**
	 * Init the Subset
	 * 
	 * @param i index
	 * @param c color
	 * @param v corresponding VGRaph that is needed for color changes
	 */
	VSubSet(int i, Color c,VGraph v)
	{
		index = i;
		colour = c;
		vGraph =v;
	}
	/**
	 * Update the color of this subset
	 * removes the color from all nodes and edges it belongs to and adds the new
	 * @param newcolour
	 */
	public void setColor(Color newcolour)
	{
		colour = newcolour;
		Iterator<VNode> nodeiter = vGraph.getNodeIterator();
		while (nodeiter.hasNext())
		{
			VNode n = nodeiter.next();
			if (vGraph.SubSetcontainsNode(n.index,index))
			{
				n.removeColor(colour); n.addColor(newcolour);
			}
		}
		Iterator<VEdge> edgeiter = vGraph.getEdgeIterator();
		while (edgeiter.hasNext())
		{
			VEdge n = edgeiter.next();
			if (vGraph.SubSetcontainsEdge(n.index,index))
			{
				n.removeColor(colour); n.addColor(newcolour);
			}
		}
		colour = newcolour;
	}
	/**
	 * Get actual Color of the Subset
	 * @return the color
	 */
	public Color getColor()
	{
		return colour;
	}
	/**
	 * get the index of the subset
	 * @return index
	 */
	public int getIndex()
	{
		return index;
	}
}
