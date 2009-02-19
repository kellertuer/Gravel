package model;
import java.util.BitSet;
/**
 * A Subgraph consits of two BitSets, an index and a name.
 * One Bitset represents the Edges the other one the nodes that belong to the Subgraph
 * @author Ronny Bergmann
 *
 */
public class MSubgraph 
{
	//Referenzen zu den Knoten und Kantenmengen des Urgraphen 
	//BitSet, das angibt welche Knoten und Kanten in dieser Menge liegen
	private BitSet setnodes, setedges;
	//Name des Sets
	private String SetName;
	private int index;
	/**
	 * Create a Subgraph with an
	 * @param i index
	 * @param n and a name
	 */
	public MSubgraph(int i,String n)
	{
		SetName = n;
		index = i;
		setnodes = new BitSet();
		setnodes.clear();
		setedges = new BitSet();
		setedges.clear();
	}
	
	public MSubgraph clone()
	{
		MSubgraph c = new MSubgraph(index, SetName);
		for (int i=0; i < setnodes.size(); i++)
			if (setnodes.get(i))
				c.addNode(i);
		for (int i=0; i < setedges.size(); i++)
			if (setedges.get(i))
				c.addEdge(i);
		return c;
	}
	/**
	 * Add the Node with Index i to the MSubgraph
	 * @param i node index 
	 */
	public void addNode(int i)
	{
		setnodes.set(i);
	}

	/**
	 * Remove node i from the MSubgraph
	 * @param i node index
	 */
	public void removeNode(int i)
	{
		setnodes.clear(i);
	}

	/**
	 * Indicates whether Node with index i belongs to this MSubgraph
	 * @param i index of a node
	 * @return true, if the node belongs to the MSubgraph else false
	 */
	public boolean containsNode(int i)
	{
		return setnodes.get(i);
	}
	/**
	 * Add an edge to this MSubgraph
	 * @param i edge index
	 */
	public void addEdge(int i)
	{
		setedges.set(i);
	}

	/**
	 * Remove an edge from the MSubgraph
	 * @param i edge index
	 */
	public void removeEdge(int i)
	{
		setedges.clear(i);
	}

	/**
	 * Indicates whether edge with index i belongs to this MSubgraph
	 * @param i index of a edge
	 * @return true, if the egde belongs to the MSubgraph else false
	 */
	public boolean containsEdge(int i)
	{
		return setedges.get(i);
	}
	/**
	 * Get the Name of the MSubgraph
	 * @return the name
	 */
	public String getName()
	{
		return SetName;
	}
	/**
	 * Set the Name of the MSubgraph to
	 * @param s the specified string
	 */
	public void setName(String s)
	{
		SetName = s;
	}
	/**
	 * Get the Index of the MSubgraph
	 * @return the index of the MSubgraph
	 */
	public int getIndex()
	{
		return index;
	}
}
