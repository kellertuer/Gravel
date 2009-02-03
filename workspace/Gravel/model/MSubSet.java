package model;
import java.util.BitSet;
/**
 * A Subset consits of two BitSets, an index and a name.
 * One Bitset represents the Edges the other one the nodes that belong to the subset
 * @author ronny
 *
 */
public class MSubSet 
{
	//Referenzen zu den Knoten und Kantenmengen des Urgraphen 
	//BitSet, das angibt welche Knoten und Kanten in dieser Menge liegen
	private BitSet setnodes, setedges;
	//Name des Sets
	private String SetName;
	private int index;
	/**
	 * Create a Subset with an
	 * @param i index
	 * @param n and a name
	 */
	public MSubSet(int i,String n)
	{
		SetName = n;
		index = i;
		setnodes = new BitSet();
		setedges = new BitSet();
	}
	
	public MSubSet clone()
	{
		MSubSet c = new MSubSet(index, SetName);
		for (int i=0; i < setnodes.size(); i++)
			if (setnodes.get(i))
				c.addNode(i);
		for (int i=0; i < setedges.size(); i++)
			if (setedges.get(i))
				c.addEdge(i);
		return c;
	}
	/**
	 * Indicates whether Node with index i belongs to this subset
	 * @param i index of a node
	 * @return true, if the node belongs to the subset else false
	 */
	public boolean containsNode(int i)
	{
		return setnodes.get(i);
	}
	/**
	 * Indicates whether edge with index i belongs to this subset
	 * @param i index of a edge
	 * @return true, if the egde belongs to the subset else false
	 */
	public boolean containsEdge(int i)
	{
		return setedges.get(i);
	}
	/**
	 * Add the Node with Index i to the subset
	 * @param i node index 
	 */
	public void addNode(int i)
	{
		setnodes.set(i);
	}
	/**
	 * Remove node i from the subset
	 * @param i node index
	 */
	public void removeNode(int i)
	{
		setnodes.clear(i);
	}
	/**
	 * Add an edge to this subset
	 * @param i edge index
	 */
	public void addEdge(int i)
	{
		setedges.set(i);
	}
	/**
	 * Remove an edge from the subset
	 * @param i edge index
	 */
	public void removeEdge(int i)
	{
		setedges.clear(i);
	}
	/**
	 * Get the Name of the Subset
	 * @return the name
	 */
	public String getName()
	{
		return SetName;
	}
	/**
	 * Set the Name of the Subset to
	 * @param s the specified string
	 */
	public void setName(String s)
	{
		SetName = s;
	}
	/**
	 * Get the Index of the Subset
	 * @return the index of the subset
	 */
	public int getIndex()
	{
		return index;
	}
}
