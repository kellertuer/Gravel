package model.Messages;

/**
 * MGraphMessage keeps changes of the elements in an MGraph.
 * 
 * It belongs to the internal GraphMessages to keep other sets
 * updated, because Edge/Node Deletions must trigger MSubgraph-Updates for example
 * 
 * No external Element should react on these
 * They are filtered and kept inside MGraph
 * 
 * @author Ronny Bergmann
 * @since 0.4
 */
public class MGraphMessage {

	private int modElement=0;
	private int modType=0;
	int id=0,old=0;	
	/**
	 * Create a new GraphColorMessage for Addition or Removal of a color
	 * @param modified Element modified
	 * @param id ID of the modified element
	 * @param modification type of modification (Update not possible, because no old color given)
	 */
	public MGraphMessage(int modified, int id, int modification)
	{
		modElement=modified;
		modType = modification;
		this.id=id;
		old = id;
	}
	/**
	 * Create a new GraphColorMessage for Addition or Removal of a color
	 * @param modified Element modified
	 * @param id ID of the modified element
	 * @param oldid ID the element had before the indexchange
	 * @param modification type of modification (Update not possible, because no old color given)
	 */
	public MGraphMessage(int modified, int id, int oldid, int modification)
	{
		modElement=modified;
		modType = modification;
		this.id=id;
		old = oldid;
	}
	/**
	 * Create a Change in the Boolean Values of MGraph
	 * @param modifiedboolean
	 * @param newb
	 */
	public MGraphMessage(int modifiedboolean, boolean newb)
	{
		modElement=modifiedboolean;
		id=0;
		if (newb)
			id++;
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
	 * Get old ID of modified Element
	 * @return
	 */
	public int getOldElementID()
	{
		return old;
	}
	public boolean getBoolean()
	{
		return (id > 0);
	}
}