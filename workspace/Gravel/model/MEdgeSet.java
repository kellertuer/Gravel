package model;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import model.Messages.GraphConstraints;
import model.Messages.GraphMessage;
import model.Messages.MGraphMessage;

/**
 * Represents a Set of mathamtical Nodes to encapsulate its modifications
 * Can be tracked to react (MGraph INternal) on Node Deletions, but sends
 * messages in global format that are advanced by the MGraph
 * 
 * Internal Messages are neccessary to react on internal changes before they
 * get externally known (mainly deletions and index change)
 * 
 * @author Ronny Bergmann
 *
 */
public class MEdgeSet extends Observable implements Observer {

	private HashSet<MEdge> mEdges;
	Lock EdgeLock;
	//Saved graph values
	private boolean allowloops, allowmultiple,directed;
	/**
	 * Edge Set for Graphs. 
	 * The EdgeSet needs for addition and replacements of edges the 
	 * Characteristics of the graph, and needs to know their changes
	 * 
	 * @param d true if and olny if Graph is directed
	 * @param l true if and only if Graph allows loops
	 * @param m true if and only if Graph allows multiple edges beween two nodes
	 */
	public MEdgeSet(boolean d, boolean l, boolean m)
	{
		allowloops = l;
		allowmultiple = m;
		directed = d;
		mEdges = new HashSet<MEdge>();
		EdgeLock = new ReentrantLock();
	}
	/**
	 * Add an edge with index i between s and e width value v
	 * If an edge exists between s and e, a new edge is only added if multiple edges are allowed
	 * If start and end are equal, the edge is only added if loops are allowed
	 *  
	 * If itis possible to add this edge, a copy of the parameter is added
	 * @param e the new edge
	 * 
	 * @return true if the edge is added, else false
	 */
	public boolean add(MEdge e)
	{
		if (add_(e))
		{
			setChanged();
			notifyObservers(new GraphMessage(GraphConstraints.EDGE,e.index,GraphConstraints.ADDITION,GraphConstraints.EDGE));	
			return true;
		}
		return false;
	}
	/**
	 * Add an MEdge without notification but with the return of the success
	 * @param e
	 * @return
	 */
	private boolean add_(MEdge e)
	{
		if ((e.StartIndex==e.EndIndex)&&(!allowloops)) //adding tries a loop but loops are not allowed
			return false;
		if ((cardinalityBetween(e.StartIndex, e.EndIndex)>0)&&(!allowmultiple)) //adding tries a second edge between s and e and multiple edges are not allowed
			return false;
		if (get(e.index)!=null) //index already in use
			return false;
		EdgeLock.lock();
		try 
		{
			mEdges.add(new MEdge(e.index, e.StartIndex, e.EndIndex, e.Value, e.name));
		} 
		finally{EdgeLock.unlock();}
		return true;
	}
	/**
	 * Get the Mathematical Edge with index i
	 * @param i index of edge
	 * @return the edge if an edge with this index exists, else null
	 */
	public MEdge get(int i)
	{
		Iterator<MEdge> n = mEdges.iterator();
		while (n.hasNext())
		{
			MEdge e = n.next();
			if (e.index==i)
			{
				return e;
			}
		}
		return null;
	}
	/**
	 * Replace the an edge in the graph
	 * The index may not be changed, so the edge, that is replaced (if existent)
	 * is identfied by the index of the parameter edge given
	 * @param edge Replacement for the edge in the graph with same index
	 */
	public boolean replace(MEdge edge)
	{
		boolean changed;
		MEdge old = get(edge.index);
		EdgeLock.lock();
		try
		{
			mEdges.remove(old);
			changed = add_(edge);
			if (changed) //New edge was adable
			{
					setChanged();
					notifyObservers(new GraphMessage(GraphConstraints.EDGE,edge.index,GraphConstraints.UPDATE,GraphConstraints.EDGE));	
			}
			else
				mEdges.add(old); //Don't replace
		}
		finally {EdgeLock.unlock();}
		return changed;
	}
	/**
	 * Remove an edge from the graph.
	 * If it does not exist, nothing happens
	 * @param i edge defined by index
	 */
	public void remove(int i)
	{
		MEdge toDel = get(i);
		EdgeLock.lock();
		try
		{
			if (toDel!=null)
			{
				//Notify SubSets
				setChanged();
				notifyObservers(new MGraphMessage(GraphConstraints.EDGE,i,GraphConstraints.REMOVAL));
				mEdges.remove(toDel);
				setChanged();
				notifyObservers(new GraphMessage(GraphConstraints.EDGE,i,GraphConstraints.REMOVAL,GraphConstraints.EDGE));	
			}
		}
		finally {EdgeLock.unlock();}
	}
	/**
	 * get a free edge index
	 * @return max_exge_index + 1
	 */
	public int getNextIndex()
	{
		EdgeLock.lock();
		int index = 1;
		try {
			Iterator<MEdge> n = mEdges.iterator();
			while (n.hasNext()) {
				MEdge temp = n.next();
				if (temp.index >= index) // index vergeben
				{
					index = temp.index + 1;
				}
			}
		} finally {
			EdgeLock.unlock();
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
		Iterator<MEdge> n = mEdges.iterator();
		while (n.hasNext()) {
			MEdge actual = n.next();
			if ((actual.index + 1) > ret.size()) {
				ret.setSize(actual.index + 1);
			}
			if (!((actual.StartIndex==0)||(actual.EndIndex==0)))//keine temporäre Kante
			{
				ret.set(actual.index, actual.name);
			}
		}
		return ret;
	}
	/**
	 * Returns the number of edges between two given nodes. For the non-multiple case 0 means no edge 1 means an edge exists
	 * @param start start node index
	 * @param ende end node index
	 *
	 * @return
	 */
	public int cardinalityBetween(int start, int ende)
	{
		int count = 0;
		EdgeLock.lock();
		try{
				Iterator<MEdge> n = mEdges.iterator();
				while (n.hasNext())
				{
					MEdge e = n.next();
					if ( ( e.StartIndex==start ) && (e.EndIndex==ende) )
						count ++; //count this edge because it is from start to end
					else if ( ( !directed ) && ( e.StartIndex==ende ) && ( e.EndIndex==start ) )
						count ++; //count this edge because in the nondirected case this is also from start to end
				}
			} finally {EdgeLock.unlock();}
		return count;
	}
	/**
	 * Get the indices of Edges between these two nodes
	 * @param start start node definnied by index
	 * @param ende end node
	 * @return
	 */
	public Vector<Integer> indicesBetween(int start, int ende)
	{
		Vector<Integer> liste = new Vector<Integer>();
		EdgeLock.lock();
		try{
				Iterator<MEdge> n = mEdges.iterator();
				while (n.hasNext())
				{
					MEdge e = n.next();
					if ( ( e.StartIndex==start ) && (e.EndIndex==ende) )
					{
						liste.add(e.index);
					}
					//return e.index;
					else if ( ( !directed ) && ( e.StartIndex==ende ) && ( e.EndIndex==start ) )
					{
						liste.add(e.index);
					}
					//return e.index;
				}
			} finally {EdgeLock.unlock();}
		return liste;
	}
	/**
	 * Get the number of edges in the mgraph
	 * @return number of edges
	 */	
	public int cardinality()
	{
		return mEdges.size();
	}
	/**
	 * Get a new Iterator for the edges. Attention: Because this stuff is threadsafe and is used in many threads the edges might change
	 */
	public Iterator<MEdge> getIterator()
	{
		return mEdges.iterator();
	}
	private void handleNodeUpdate(MGraphMessage mm)
	{
		int mod = mm.getModificationType();
		if ((mod!=GraphConstraints.INDEXCHANGED)&&(mod!=GraphConstraints.REMOVAL))
				return;
		EdgeLock.lock();
		try
		{
			Iterator<MEdge> e = mEdges.iterator();
			HashSet<MEdge> adjacent = new HashSet<MEdge>();
			while (e.hasNext()) {
				MEdge edge = e.next();
				if ((edge.EndIndex==mm.getElementID())&&(mod==GraphConstraints.REMOVAL))
						adjacent.add(edge);
				else if ((edge.StartIndex==mm.getElementID())&&(mod==GraphConstraints.REMOVAL))
					adjacent.add(edge);
				else if ((edge.EndIndex==mm.getOldElementID())&&(mod==GraphConstraints.INDEXCHANGED))
					edge.EndIndex = mm.getElementID();
				else if ((edge.StartIndex==mm.getOldElementID())&&(mod==GraphConstraints.INDEXCHANGED))
					edge.StartIndex = mm.getElementID();
			}	
			e = adjacent.iterator();
			while (e.hasNext())
			{
				//So remove them silent
				mEdges.remove(e.next());
			}
		}
		finally {EdgeLock.unlock();}
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
			case GraphConstraints.LOOPS:
				allowloops = mm.getBoolean();
				break;
			case GraphConstraints.DIRECTION:
				directed = mm.getBoolean();
				break;
			case GraphConstraints.MULTIPLE:
				allowmultiple = mm.getBoolean();
				break;
			case GraphConstraints.NODE:
				handleNodeUpdate(mm);
				break;
		}
	}

}