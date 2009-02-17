package model;

import java.awt.Color;
import java.util.Comparator;

/**
 * A Visual SubSet contains the index for identification and the color 
 * @author Ronny Bergmannn
 */
public class VSubSet {
	
	public static class SubSetIndexComparator implements Comparator<VSubSet>
	{
		public int compare(VSubSet a, VSubSet b) {
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
	 * Init the Subset
	 * 
	 * @param i index
	 * @param c color
	 * @param v corresponding VGRaph that is needed for color changes
	 */
	public VSubSet(int i, Color c)
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
	 * Clone the actual VSubset, that is return a copy
	 */
	public VSubSet clone()
	{
		return new VSubSet(index,colour);
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
