package model;
/**
 * This Class represents alls possible Updates and changes,
 * that may occur in a Graph and represents them
 * 
 * An Update Message is read Only so only the constructor can set internal values.
 * <br>Updates may contain
 * <br>
 * <br>- Nodes
 * <br>- Edges 
 * <br>- Subsets
 * <br> - Selections
 * <br> - Changes in allowance of Loops, Multiple Edges
 * <br> - Change whether the Graph is directed or not
 * <br> - A Graph translation or replacement
 * <br><br>- Begin and End of a Block of Changes (e.g. to group them somewhere in the stats)
 * <br> Therefore Type and Affected must be 0, ID must be -1
 * They may occur together in one message. If only one Item was changed, it's ID is given

 * @author Ronny Bergmann
 * @
 */
public class GraphMessage {
	
	//Type of change (and affection)
	public static final int NODE = 1;
	public static final int EDGE = 2;
	public static final int SUBSET = 4;
	public static final int ALL_ELEMENTS=7;
	public static final int SELECTION = 8;
	public static final int DIRECTION = 16;
	public static final int LOOPS = 32;
	public static final int MULTIPLE = 64;
	public static final int ALL = 0xffff;

	//Status - Actions done
	public static final int UPDATE = 1;
	public static final int ADDITION = 2;
	public static final int REMOVAL = 4;
	public static final int TRANSLATION = 8;
	public static final int REPLACEMENT = 16;
	//Special Stati for Block Updates
	public static final int BLOCK_START = 512;
	public static final int BLOCK_END = 1024;
	public static final int BLOCK_ABORT = 2048;
	
	private int id, status, type, affected;
	private String message="";
	
	/**
	 * Create a Message with all 4 Values Type, ID, Status and ID
	 * No Values are checked up to now (though if type is not unique there shouldn't be an ID) 
	 *
	 * @param pType Type of Element(s) changed
	 * @param pID ID of an unique item that was modified (-1 if there where more)
	 * @param pStatus what was the update?
	 * @param pAffected which Elements are affected by this?
	 */
	public GraphMessage(int pType, int pID, int pStatus, int pAffected)
	{
		type = pType;
		id = pID;
		status = pStatus;
		affected = pAffected;
	}
	/**
	 * Create a Message with all 3 Values Status, Type, Affected
	 * No Values are checked up to now
	 *
	 * @param pType Type of Element(s) changed
	 * @param pStatus what was the update?
	 * @param pAffected which Elements are affected by this?
	 */
	public GraphMessage(int pType, int pStatus, int pAffected)
	{
		this(pType,-1,pStatus,pAffected);
	}
	/**
	 * Create a Message with all 2 Values Status, Type, because only Type-Elements are affected
	 * No Values are checked up to now
	 *
	 * @param pType Type of Element(s) changed
	 * @param pStatus what was the update?
	 */
		public GraphMessage(int pType, int pStatus)
	{
		this(pType,-1,pStatus,pType);
	}
	/**
	 * Get the type of Change occured to the graph, it may be one of the Changes
	 * UPDATED, ADDED, REMOVED
	 * @return status
	 */
	public int getChangeStatus()
	{
		return status;
	}
	/**
	 * Get the elements that where actually changed,
	 * may not bet unique, because one action may have changed multiple types
	 */
	public int getAction()
	{
		return type;
	}
	/**
	 * Get the ID of the changed item, if there was a change to only one item changed in the graph
	 * @return a valid id if one item was changed, else -1
	 */
	public int getElementID()
	{
		return id;
	}
	/**
	 * Get the types of affected Types that need to be updated in other listening classes (e.g. drawing)
	 * @return
	 */
	public int getAffectedTypes()
	{
		return affected;
	}
	
	/**
	 * Specify a special Message String (for example at the Beginning of a Block
	 * @param s
	 */
	public void setMessage(String s)
	{
		message = s;
	}
	/**
	 * get the Specified Block
	 * @return
	 */
	public String getMessage()
	{
		return message;
	}
	public String toString()
	{
		return "Action:"+type+" (#"+id+") Status:"+status+" Affected:"+affected+" Msg:"+message;
	}
}
