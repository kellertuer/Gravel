package model;

import java.awt.Color;
import java.util.Comparator;

/**
 * A Visual Subgraph contains the index for identification and the color 
 * @author Ronny Bergmannn
 */
public class VSubgraph {
	
	public static class SubgraphIndexComparator implements Comparator<VSubgraph>
	{
		public int compare(VSubgraph a, VSubgraph b) {
			if (a.getIndex() < b.getIndex())
				return -1;
			if (a.getIndex()==b.getIndex())
				return 0;
			else
				return 1;
		}
		
	}

	//Farbe der Untermenge
	private Color colour;
	//INdex
	int index;
	/**
	 * Init the Subgraph
	 * 
	 * @param i index
	 * @param c color
	 * @param v corresponding VGRaph that is needed for color changes
	 */
	public VSubgraph(int i, Color c)
	{
		index = i;
		colour = c;
	}
	/**
	 * Set the Color to a new value
	 * protected because only a graph should to this (!)
	 * @param n
	 */
	protected void setColor(Color n)
	{
		colour = n;
	}
	/**
	 * Clone the actual VSubgraph, that is return a copy
	 */
	public VSubgraph clone()
	{
		return new VSubgraph(index,colour);
	}
	/**
	 * Get actual Color of the VSubgraph
	 * @return the color
	 */
	public Color getColor()
	{
		return colour;
	}
	/**
	 * get the index of the VSubgraph
	 * @return index
	 */
	public int getIndex()
	{
		return index;
	}
}
