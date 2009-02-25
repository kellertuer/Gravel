package model.Messages;

import java.awt.Color;
/**
 * GraphColorMessage keeps changes of a Color.
 * It belongs to the internal GraphMessages to keep other sets
 * updated
 * 
 * No external Element should react on these
 * They are filtered and kept inside VGraph
 * 
 * @author Ronny Bergmann
 * @since 0.4
 */
public class GraphColorMessage implements GraphConstraints {
	
	private int modElement=0;
	private int modType=0;
	int id=0;
	private Color color=null;
	private Color oldcolor=null;
	
	/**
	 * Create a new GraphColorMessage for Addition or Removal of a color
	 * @param modified Element modified
	 * @param id ID of the modified element
	 * @param modification type of modification (Update not possible, because no old color given)
	 * @param c Color that has been added or removed
	 */
	public GraphColorMessage(int modified, int id, int modification, Color c)
	{
		modElement=modified;
		modType = modification;
		this.id=id;
		color=c;
	}
	/**
	 * Create a new GraphColorMessage for Updated
	 * @param modified Type of the Updated Element
	 * @param id ID of the Updated Element
	 * @param cold Old Color that was replaced
	 * @param c New Color that was set
	 */
	public GraphColorMessage(int modified, int id, Color cold, Color c)
	{
		modElement=modified;
		modType=UPDATE;
		this.id=id;
		oldcolor=cold;
		color=c;
	}
	/**
	 * Return the Type of the modified Element
	 * @return
	 */
	public int getModifiedElement()
	{
		return modElement;
	}
	/**
	 * Return the Type of Modification
	 * @return
	 */
	public int getModificationType()
	{
		return modType;
	}
	/**
	 * Get ID of modified Element
	 * @return
	 */
	public int getElementID()
	{
		return id;
	}
	/**
	 * get Color that was added/removed or is New
	 * @return
	 */
	public Color getColor()
	{
		return color;
	}
	/**
	 * get Old Color
	 * Is NULL if a Color was added or removed.
	 * @return
	 */
	public Color getOldColor()
	{
		return oldcolor;
	}
}
