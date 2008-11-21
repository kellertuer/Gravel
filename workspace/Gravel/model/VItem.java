package model;
/**
 * Abstract superclass for both nodes and edges wdich contains the similar stuff for both
 * @author ronny
 *
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
	/**
	 * Standard Constructor that sets the new Item to Deselected
	 *
	 */
	public VItem()
	{
		status = DESELECTED;
	}
	/**
	 * Set the status to a given Value, use || to set it to multiple ones
	 * @param s
	 */
	public void setSelectedStatus(int s)
	{
		status = s;
	}
	/**
	 * reset to nonselected Standard
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
	 */
	public void select()
	{
		status=SELECTED;
	}
}
