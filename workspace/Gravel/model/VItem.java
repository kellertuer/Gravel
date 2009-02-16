package model;
/**
 * Abstract superclass for both nodes and edges wdich contains the similar stuff for both
 * 
 * @author Ronny Bergmann
 * @since Gravel 0.2.3
 */
public abstract class VItem {
	
	//Definetly not selected
	public static int DESELECTED = 0;
	//Just ATM not selected e.g. while you drag a mouse or sth like that
	public static int SOFT_DESELECTED = 1;
	//Definetly selected
	public static int SELECTED = 2;
	//Just ATM selected e.g. while you drag a mouse or sth like that
	public static int SOFT_SELECTED = 4;
	
	private int status;
	
	private int index;  //kind of Nodekey

	/**
	 * Standard Constructor that initializes the VItem with a specific index and
	 * sets the new Item to Deselected 
	 * 
	 * @param i index of the VItem
	 */
	public VItem(int i)
	{
		index = i;
		status = DESELECTED;
	}
	/**
	 * Set Index of the item to a new value
	 * 
	 * ATTENTION: This Method should be called only if really needed
	 * The Index is the main element for references (e.g. to adjacent nodes of an edge)
	 * Therefore only use this Method if you're sure you don't mess the Graph
	 * 
	 * @param index the index to set
	 */
	protected void setIndex(int index) {
		this.index = index;
	}
	/**
	 * Get Index of this Item
	 * @return the index
	 */
	public int getIndex() {
		return index;
	};
	
	
	/**
	 * Set the status to a given Value, use || to set it to multiple ones
	 * @param s
	 */
	public void setSelectedStatus(int s)
	{
		status = s;
	}
	/**
	 * reset to the nonselected Standard
	 *
	 */
	public void deselect()
	{
		status = DESELECTED;
	}
	/**
	 * get the actual Status of the Item
	 * @return the status
	 */
	public int getSelectedStatus()
	{
		return status;
	}
	/**
	 * Get the Selected Status of the Item
	 * Deprecated since the VItem Stuff
	 * 
	 * @deprecated
	 * @return true if the Item is Selected, else false
	 */
	public boolean isSelected()
	{
		return ((status&SELECTED)==SELECTED);
	}
	/**
	 * Setselected - deprecated since the VItem Stuff
	 *
	 * @deprecated
	 */
	public void select()
	{
		status=SELECTED;
	}
}
