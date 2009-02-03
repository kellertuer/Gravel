package model;

import java.awt.Color;

/**
 * A Visual SubSet contains the index for identification and the color 
 * @author Ronny Bergmannn
 */
public class VSubSet {
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
