package model;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import model.Messages.GraphMessage;
import model.Messages.MGraphMessage;

/**
 * Represents a Set of mathamtical Edges to encapsulate its modifications
 * Can be tracked to react (MHyperGraph INternal) on Hyperedge Deletions, but sends
 * messages in global format that are advanced by the MHyperGraph
 * 
 * Internal Messages are neccessary to react on internal changes before they
 * get externally known (mainly deletions and index change)
 * 
 * @author Ronny Bergmann
 *
 */
public class MHyperEdgeSet extends Observable implements Observer {

	private HashSet<MHyperEdge> mHyperEdges;
	Lock HyperEdgeLock;
	/**
	 * Create a Hyperdge Set for Hypergraphs. 
	 */
	public MHyperEdgeSet()
	{
		mHyperEdges = new HashSet<MHyperEdge>();
		HyperEdgeLock = new ReentrantLock();
	}
	/**
	 * Add an hyperedge with index i
	 * If an edge exists with the same endnodes, nothing happens
	 *  
	 * If it is possible to add this hyperedge, a copy of the parameter is added
	 * @param e the new hyperedge
	 * 
	 * @return true if the hyperedge is added, else false
	 */
	public boolean add(MHyperEdge e)
	{
		if (add_(e))
		{
			setChanged();
			notifyObservers(new GraphMessage(GraphMessage.HYPEREDGE,e.index,GraphMessage.ADDITION,GraphMessage.HYPEREDGE));	
			return true;
		}
		return false;
	}
	/**
	 * Add an MEdge without notification but with the return of the success
	 * @param e
	 * @return
	 */
	private boolean add_(MHyperEdge e)
	{
		if (get(e.getEndNodes())!=null) //HyperEdge exists
			return false;
		HyperEdgeLock.lock();
		try 
		{
			mHyperEdges.add(e.clone());
		} 
		finally{HyperEdgeLock.unlock();}
		return true;
	}
	/**
	 * Get the Mathematical Hyperedge with index i
	 * @param i index of edge
	 * @return the edge if an edge with this index exists, else null
	 */
	public MHyperEdge get(int i)
	{
		Iterator<MHyperEdge> n = mHyperEdges.iterator();
		while (n.hasNext())
		{
			MHyperEdge e = n.next();
			if (e.index==i)
			{
				return e;
			}
		}
		return null;
	}
	/**
	 * Get an HyperEdge with the given NodeSubSet s
	 * 
	 * If no Hyperedge with this set of nodes exists, null is returned
	 * 
	 * @param s BitSet indicating nodes
	 * 
	 * @return the HyperEdge if exitent, else null
	 */
	public MHyperEdge get(BitSet s)
	{
		Iterator<MHyperEdge> ihe = mHyperEdges.iterator();
		while (ihe.hasNext())
		{
			MHyperEdge temp = ihe.next();
			BitSet check = temp.getEndNodes(); //Get a Clone o the BitSet
			check.and(s);
			if (check.cardinality()==s.cardinality())
				return temp;
		}
		return null;
	}
	/**
	 * Replace the an edge in the graph
	 * The index may not be changed, so the edge, that is replaced (if existent)
	 * is identfied by the index of the parameter edge given
	 * @param edge Replacement for the edge in the graph with same index
	 */
	public boolean replace(MHyperEdge he)
	{
		boolean changed;
		MHyperEdge old = get(he.index);
		HyperEdgeLock.lock();
		try
		{
			mHyperEdges.remove(old);
			changed = add_(he);
			if (changed) //New edge was adable
			{
					setChanged();
					notifyObservers(new GraphMessage(GraphMessage.HYPEREDGE,he.index,GraphMessage.UPDATE,GraphMessage.HYPEREDGE));	
			}
			else
				mHyperEdges.add(old); //Don't replace, add again
		}
		finally {HyperEdgeLock.unlock();}
		return changed;
	}
	/**
	 * Remove an edge from the graph.
	 * If it does not exist, nothing happens
	 * @param i edge defined by index
	 */
	public void remove(int i)
	{
		MHyperEdge toDel = get(i);
		HyperEdgeLock.lock();
		try
		{
			if (toDel!=null)
			{
				//Notify SubSets
				setChanged();
				notifyObservers(new MGraphMessage(MGraphMessage.EDGE,i,MGraphMessage.REMOVAL));
				mHyperEdges.remove(toDel);
				setChanged();
				notifyObservers(new GraphMessage(GraphMessage.EDGE,i,GraphMessage.REMOVAL,GraphMessage.EDGE));	
			}
		}
		finally {HyperEdgeLock.unlock();}
	}
	/**
	 * Add a Node to a HyperEdge
	 * If the hyperedge exists
	 * If the node is already in the hyperedge, no notification is pushed
	 * @param nodeindex which is unchecked (for Existence in the graph)!
	 * @param hyperedgeindex
	 */
	public void addNodeto(int nodeindex, int hyperedgeindex)
	{
		MHyperEdge he = get(hyperedgeindex);
		if (he==null)
			return;
		if (!he.containsNode(nodeindex)) //Change if it is not in the subgraph yet
		{ 
			he.addNode(nodeindex);
			setChanged();
			notifyObservers(new GraphMessage(GraphMessage.HYPEREDGE,hyperedgeindex,GraphMessage.UPDATE,GraphMessage.HYPEREDGE|GraphMessage.NODE));	
		}
	}
	/**
	 * Add a Node to a HyperEdge
	 * If the hyperedge exists
	 * If the node is already in the hyperedge, no notification is pushed
	 * @param nodeindex which is unchecked (for Existence in the graph)!
	 * @param hypergraphindex index of hyperedge where the node should be removed
	 */
	public void removeNode(int nodeindex, int hyperedgeindex)
	{
		MHyperEdge he = get(hyperedgeindex);
		if (he==null)
			return;
		if (he.containsNode(nodeindex))
		{
			he.removeNode(nodeindex);
			setChanged();
			notifyObservers(new GraphMessage(GraphMessage.HYPEREDGE,hyperedgeindex,GraphMessage.UPDATE,GraphMessage.HYPEREDGE|GraphMessage.NODE));	
		}
	}
	/**
	 * get a free edge index
	 * @return max_exge_index + 1
	 */
	public int getNextIndex()
	{
		HyperEdgeLock.lock();
		int index = 1;
		try {
			Iterator<MHyperEdge> n = mHyperEdges.iterator();
			while (n.hasNext()) {
				MHyperEdge temp = n.next();
				if (temp.index >= index) // index vergeben
				{
					index = temp.index + 1;
				}
			}
		} finally {
			HyperEdgeLock.unlock();
		}
		return index;
	}
	/**
	 * get a list of the edge names in a vector, where each edge name is stored at it's index
	 * every other component of the vector is null
	 * <br><br>
	 * an edge name is the mathematical given edge name 
	 * <br><br>
	 * @return a Vector of all edge names, 
	 */	
	public Vector<String> getNames() {
		Vector<String> ret = new Vector<String>();
		Iterator<MHyperEdge> n = mHyperEdges.iterator();
		while (n.hasNext()) {
			MHyperEdge actual = n.next();
			if ((actual.index + 1) > ret.size()) {
				ret.setSize(actual.index + 1);
			}
			ret.set(actual.index, actual.name);
		}
		return ret;
	}
	/**
	 * Returns the number of hyperedges a node belongs to
	 * 
	 * @param i index of the Node
	 *
	 * @return
	 */
	public int DegreeoOfNode(int i)
	{
		int count = 0;
		HyperEdgeLock.lock();
		try{
				Iterator<MHyperEdge> n = mHyperEdges.iterator();
				while (n.hasNext())
				{
					if (n.next().containsNode(i))
						count++;
				}
			} finally {HyperEdgeLock.unlock();}
		return count;
	}
	/**
	 * Get the indices of Edges containing a node
	 * @param end 
	 * 
	 * @return
	 */
	public Vector<Integer> indicesContainingNode(int end)
	{
		Vector<Integer> liste = new Vector<Integer>();
		HyperEdgeLock.lock();
		try{
				Iterator<MHyperEdge> n = mHyperEdges.iterator();
				while (n.hasNext())
				{
					MHyperEdge e = n.next();
					if (e.containsNode(end))
					{
						liste.add(e.index);
					}
					//return e.index;
				}
			} finally {HyperEdgeLock.unlock();}
		return liste;
	}
	/**
	 * Get the number of hyperedges in the mhypergraph
	 * @return number of hyperedges
	 */	
	public int cardinality()
	{
		return mHyperEdges.size();
	}
	/**
	 * Get a new Iterator for the edges. Attention: Because this stuff is threadsafe and is used in many threads the edges might change
	 */
	public Iterator<MHyperEdge> getIterator()
	{
		return mHyperEdges.iterator();
	}
	private void handleNodeUpdate(MGraphMessage mm)
	{
		int mod = mm.getModificationType();
		if ((mod!=MGraphMessage.INDEXCHANGED)&&(mod!=MGraphMessage.REMOVAL))
				return;
		HyperEdgeLock.lock();
		try
		{
			Iterator<MHyperEdge> e = mHyperEdges.iterator();
			HashSet<MHyperEdge> incident = new HashSet<MHyperEdge>();
			while (e.hasNext()) {
				MHyperEdge edge = e.next();
				if ((edge.containsNode(mm.getElementID()))&&(mod==MGraphMessage.REMOVAL))
						incident.add(edge);
				else if ((edge.containsNode(mm.getOldElementID()))&&(mod==MGraphMessage.INDEXCHANGED))
				{
					edge.removeNode(mm.getOldElementID());
					edge.addNode(mm.getElementID());
				}
			}	
			e = incident.iterator();
			while (e.hasNext())
			{
				//So remove them silent
				mHyperEdges.remove(e.next());
			}
		}
		finally {HyperEdgeLock.unlock();}
	}
	/**
	 * get a list of the subgraphs names in a vector, where each subgraph name is stored at it's index
	 * every other component of the vector is null
	 * @return a Vector of all subgraphs names, 
	 */	
	public void update(Observable o, Object arg) {
		if (!(arg instanceof MGraphMessage))
			return; //Only handle internal Messages
		MGraphMessage mm = (MGraphMessage)arg;
		switch (mm.getModifiedElement())
		{
			case MGraphMessage.NODE:
				handleNodeUpdate(mm);
				break;
		}
	}
}