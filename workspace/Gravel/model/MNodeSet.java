package model;

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
public class MNodeSet extends Observable implements Observer {

	private HashSet<MNode> mNodes;
	Lock NodeLock;
	
	public MNodeSet()
	{
		mNodes = new HashSet<MNode>();
		NodeLock = new ReentrantLock();
	}
	/**
	 * Adds a new node to the graph with
	 * @param m as the new MNode
	 */
	public void add(MNode m)
	{
		if (get(m.index)!=null)
			return;
		NodeLock.lock();
		try 
		{
			mNodes.add(m);
			//No internal message
			setChanged();
			notifyObservers(new GraphMessage(GraphMessage.NODE,m.index,GraphMessage.ADDITION));	
		} 
		finally {NodeLock.unlock();}
	}
	/**
	 * Get a Specific MNode from the MNodeSet
	 * 
	 * @param i node with index i
	 * @return the MNode
	 */
	public MNode get(int i)
	{
		NodeLock.lock();
		try
		{
			Iterator<MNode> n = mNodes.iterator();
			while (n.hasNext())
			{
				MNode t = n.next();
				if (t.index==i)
					return t;
			}
		} finally {NodeLock.unlock();}
		return null;
	}
	/**
	 * remove a node from the graph. thereby the adjacent edges are removed too. The indices of the deleted edges
	 * are set in the return value
	 * @param i index of the node, that should be removed
	 * 
	 * @return a bitset where all edge indices are set true, that are adjacent and were deleted too
	 */
	public void remove(int i)
	{
		MNode toDel = get(i);
		if (toDel==null)
			return; //Nothing to delete
		mNodes.remove(toDel);
		//Notify all Edges and Subsets about removal - MGraph INternal
		setChanged();
		notifyObservers(new MGraphMessage(MGraphMessage.NODE,i,MGraphMessage.REMOVAL));
		//global notify
		setChanged();
		notifyObservers(new GraphMessage(GraphMessage.NODE,i,GraphMessage.REMOVAL,GraphMessage.ALL_ELEMENTS));	
		return;
	}
	/**
	 * Replace (if existent) the node in the graph with the index of the parameter node by the parameter
	 * @param node new node for its index
	 */
	public void replace(MNode node)
	{
		MNode oldnode = get(node.index);
		if (oldnode==null)
			return;
		NodeLock.lock();
		try 
		{
			mNodes.remove(oldnode);
			mNodes.add(node);
			//Only External because the internal integrity is not affected
			setChanged();
			notifyObservers(new GraphMessage(GraphMessage.NODE,node.index,GraphMessage.UPDATE,GraphMessage.NODE));	
		} 
		finally
		{NodeLock.unlock();}		
	}
	/**
	 * Change the index of a node. This method is neccessary, because all other functions rely on the fact, 
	 * that the nodeindex is the reference for everything
	 * @param oldi old index of the node
	 * @param newi new index of the node
	 */
	public void changeIndex(int oldi, int newi)
	{
		if (oldi==newi)
			return;
		MNode oldn = get(oldi);
		MNode newn = get(newi);
		if ((oldn==null)||(newn!=null))
			return; //can't change
		//Notify adjacent edges and Subsets
		setChanged();
		notifyObservers(new MGraphMessage(MGraphMessage.NODE, newi, oldi, MGraphMessage.INDEXCHANGED));
		//And Change the oldnode aswell
		oldn.index=newi;
		replace(newn);
		setChanged();
		notifyObservers(new GraphMessage(GraphMessage.NODE, GraphMessage.INDEXCHANGED, GraphMessage.ALL_ELEMENTS));	
	}
	/**
	 * @return max node index +1
	 */
	public int getNextIndex()
	{
		int index = 1;
		NodeLock.lock();
		try {
			Iterator<MNode> n = mNodes.iterator();
			while (n.hasNext()) {
				MNode temp = n.next();
				if (temp.index >= index) // index vergeben
				{
					index = temp.index + 1;
				}
			}
		} finally {NodeLock.unlock();}
		return index;
	}
	/**
	 * get a list of the node names in a vector, where each node name is stored at it's index
	 * every other component of the vector is null
	 * 
	 * @return a Vector of all node names, 
	 */
	public Vector<String> getNames() {
		Vector<String> ret = new Vector<String>();
		Iterator<MNode> n = mNodes.iterator();
		while (n.hasNext()) {
			MNode actual = n.next();
			if ((actual.index + 1) > ret.size()) {
				ret.setSize(actual.index + 1);
			}
			if (actual.index!=0) //kein temp-knoten
				ret.set(actual.index, get(actual.index).name);
		}
		return ret;
	}
	/**
	 * Returns the number of nodes contained in the graph
	 * @return 
	 */
	public int cardinality()
	{
		return mNodes.size();
	}
	/**
	 * Returns an Iterator to iterate the nodes
	 * @return a new iterator for the nodes
	 */
	public Iterator<MNode> getIterator()
	{
			return mNodes.iterator();
	}
	
	public void update(Observable o, Object arg) {
		// NOthign to react on yet
	}

}