package model;

import java.util.BitSet;
/**
 * This Class models the mathematical stuff of an Hyperedge
 * 
 * An Hyperedge e consists of
 *  - A Set of endnodes
 *  - an index
 *  - a value (what ever that is needed to)
 *  - a name
 *  
 * @author Ronny Bergmann
 * @since 0.4
 */
public class MHyperEdge {
	public int index;
	private BitSet EndNodes;
	public int Value;
	public String name;

	public MHyperEdge(int pindex, int pvalue, String pname)
	{
		index = pindex;
		Value = pvalue;
		name = pname;
		EndNodes = new BitSet();
		
	}
	/**
	 * Return a Copy of this MHyperEdge
	 */
	public MHyperEdge clone()
	{
		MHyperEdge c = new MHyperEdge(index, Value, new String(name.toCharArray()));
		for (int i=0; i < EndNodes.size(); i++)
			if (EndNodes.get(i))
				c.addNode(i);
		return c;
	}
	/**
	 * Add the Node with Index i to the MSubgraph
	 * @param i node index 
	 */
	public void addNode(int i)
	{
		EndNodes.set(i);
	}
	/**
	 * Remove node i from the MSubgraph
	 * @param i node index
	 */
	public void removeNode(int i)
	{
		EndNodes.clear(i);
	}
	/**
	 * Indicates whether Node with index i belongs to this MSubgraph
	 * @param i index of a node
	 * @return true, if the node belongs to the MSubgraph else false
	 */
	public boolean containsNode(int i)
	{
		return EndNodes.get(i);
	}
	/**
	 * Return a BitSet containing
	 *  - 1 if Hyperedge contains node with index i
	 *  - 0 else
	 * Changes to the returned Value don't affect the HyperEdge itself 
	 *   
	 * @return a BitSet representing the joined nodes
	 */
	public BitSet getEndNodes()
	{
		return (BitSet) EndNodes.clone();
	}
	/**
	 * Check, whether a Node is Contained, that is incident to the Hyperedge
	 * @param index nodeindex
	 * @return
	 */
	public boolean isincidentTo(int index)
	{
		return EndNodes.get(index);
	}
	/**
	 * Check, whether this edge is adjacent to the Hyperedge given as parameter
	 * 
	 * Two hyperedges are adjacent if and only if they have at least one common incident node
	 * @param he another Hyperedge
	 * @return true, if they are adjacent, else false
	 */
	public boolean isadjacentTo(MHyperEdge he)
	{
		BitSet check = (BitSet) EndNodes.clone();
		check.and(he.getEndNodes());
		return (check.cardinality() > 0);			
	}
	/**
	 * Get the number of Nodes this Hyperedge joins
	 * 
	 * @return
	 */
	public int cardinality()
	{
		return EndNodes.cardinality();
	}
}
