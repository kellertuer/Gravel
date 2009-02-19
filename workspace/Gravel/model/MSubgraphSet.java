package model;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import model.Messages.GraphMessage;
import model.Messages.MGraphMessage;
/**
 * Class for handling the mathematical Stuff of Subgraphs
 * @author ronny
 *
 */
public class MSubgraphSet extends Observable implements Observer {

	HashSet<MSubgraph> mSubgraphs;

	public MSubgraphSet()
	{
		mSubgraphs = new HashSet<MSubgraph>();
	}
	/**
	 * Add a new subgraph. if the index is already in use, nothing happens
	 * @param s Mathematical Subgraph, which should be added, a clone of the parameter is added
	 */
	public void add(MSubgraph s)
	{
		if (get(s.getIndex())==null)
		{
			mSubgraphs.add(s.clone());
			setChanged();
			notifyObservers(new GraphMessage(GraphMessage.SUBGRAPH,s.getIndex(),GraphMessage.ADDITION,GraphMessage.SUBGRAPH));	
	
		}
	}
	/**
	 * Remove a subgraph from the graph. If it does not exist, nothing happens.
	 * @param index subgraph given by id, that should be removed
	 * @return true if a subgraph was removed
	 */
	public boolean remove(int index)
	{
		MSubgraph toDelete = get(index);
		if (toDelete!=null)
		{
			mSubgraphs.remove(toDelete);
			setChanged();
			notifyObservers(new GraphMessage(GraphMessage.SUBGRAPH,index,GraphMessage.REMOVAL,GraphMessage.ALL_ELEMENTS));	
			return true;
		}
		return false;
	}
	/**
	 * Get the Subgraph specified by the index. If ist does not exists, the Method returns null
	 * @param index Index of the Subgraph
	 * @return the subgraph with the index, if exists, else null
	 */
	public MSubgraph get(int index)
	{
		Iterator<MSubgraph> iter = mSubgraphs.iterator();
		while (iter.hasNext())
		{
			MSubgraph actual = iter.next();
			if (actual.getIndex()==index)
			{
				return actual;
			}
		}
		return null;
	}
	/**
	 * Get a free subgraph index
	 * @return max_subgraph_index + 1
	 */
	public int getNextIndex() {
		int index = 1;
		Iterator<MSubgraph> n = mSubgraphs.iterator();
		while (n.hasNext()) {
			MSubgraph temp = n.next();
			if (temp.getIndex() >= index) // index vergeben
			{
				index = temp.getIndex() + 1;
			}
		}
		return index;
	}
	/**
	 * Add a Node to a Subgraph
	 * If both node and subgraph exist
	 * If the node is already in the subgraph, no notification is pushed
	 * @param nodeindex which is unchecked!
	 * @param subgraphindex
	 */
	public void addNodetoSubgraph(int nodeindex, int subgraphindex)
	{
		MSubgraph s = get(subgraphindex);
		if (s==null)
			return;
		if (!s.containsNode(nodeindex)) //Change if it is not in the subgraph yet
		{ 
			s.addNode(nodeindex);
			setChanged();
			notifyObservers(new GraphMessage(GraphMessage.SUBGRAPH,subgraphindex,GraphMessage.UPDATE,GraphMessage.SUBGRAPH|GraphMessage.NODE));	
		}
	}
	/**
	 * Removes a node from a subgraph. If there was a change in the subgraph (both node an subgraph exist and the node was in the subgraph) the return value is true, else false
	 * @param nodeindex node that should be removed (is not checked for existence
	 * @param subgraphindex index of subgraph where the node should be removed
	 */
	public void removeNodefromSubgraph(int nodeindex, int subgraphindex)
	{
		MSubgraph s = get(subgraphindex);
		if (s==null)
			return;
		if (s.containsNode(nodeindex))
		{
			s.removeNode(nodeindex);
			setChanged();
			notifyObservers(new GraphMessage(GraphMessage.SUBGRAPH,subgraphindex,GraphMessage.UPDATE,GraphMessage.SUBGRAPH|GraphMessage.NODE));	
		}
	}
	/**
	 * Add an edge to a subgraph, if both edge and subgraph exist. If they don't nothing happens
	 * @param edgeindex edge index that should be added (is not checked for existence
	 * @param subgraphindex subgraph index where the edge should be added
	 */
	public void addEdgetoSubgraph(int edgeindex, int subgraphindex)
	{
		MSubgraph s = get(subgraphindex);
		if (s==null)
			return;
		if (!s.containsEdge(edgeindex))
		{
			s.addEdge(edgeindex);
			setChanged();
			notifyObservers(new GraphMessage(GraphMessage.SUBGRAPH,subgraphindex,GraphMessage.UPDATE,GraphMessage.SUBGRAPH|GraphMessage.EDGE));	
		}
	}
	/**
	 * Removes an edge from a subgraph, if both exist. If a change is done (edge is also contained in the subgraph). 
	 * If an edge is removed, so there was really a change, it returs true
	 * @param edgeindex Edge to be removed from (but is not checked for existence)
	 * @param subgraphindex subgraph with this index
	 *
	 * @return true if both edge and subgraph exist and the edge was in the subgraph, so it was removed
	 */
	public void removeEdgefromSubgraph(int edgeindex, int subgraphindex)
	{
		MSubgraph s = get(subgraphindex);
		if (s==null)
			return;
		if (s.containsEdge(edgeindex))
		{
			s.removeEdge(edgeindex);
			setChanged();
			notifyObservers(new GraphMessage(GraphMessage.SUBGRAPH,subgraphindex,GraphMessage.UPDATE,GraphMessage.SUBGRAPH|GraphMessage.EDGE));	
		}
	}
	/**
	 * get a new Subgraph Iterator.
	 * @return
	 */
	public Iterator<MSubgraph> getIterator()
	{
		return mSubgraphs.iterator();
	}
	/**
	 * Get the number of subgraphs in the mgraph
	 * @return the number of subgraphs in the mgraph
	 */
	public int cardinality() {
		return mSubgraphs.size();
	}

	public Vector<String> getNames() {
		Vector<String> ret = new Vector<String>();
		Iterator<MSubgraph> s = mSubgraphs.iterator();
		while (s.hasNext()) {
			MSubgraph actual = s.next();
			if ((actual.getIndex() + 1) > ret.size()) {
				ret.setSize(actual.getIndex() + 1);
			}
			ret.set(actual.getIndex(), get(actual.getIndex()).getName());
		}
		return ret;
	}
	public void update(Observable o, Object arg) {
		if (!(arg instanceof MGraphMessage))
			return; //Only handle internal Messages
		MGraphMessage mm = (MGraphMessage)arg;
		int mod = mm.getModificationType();
		if ((mod!=MGraphMessage.INDEXCHANGED)&&(mod!=MGraphMessage.REMOVAL))
			return;
		Iterator<MSubgraph> si = mSubgraphs.iterator();
		while (si.hasNext())
		{
			MSubgraph s = si.next();
			//If removed or index changed, remove (old) index from each set and set new one if changed
			if (mod==MGraphMessage.INDEXCHANGED)
			{
				if (mm.getModifiedElement()==MGraphMessage.NODE)
				{
					s.removeNode(mm.getOldElementID());
					s.addNode(mm.getElementID());
				}
				else if (mm.getModifiedElement()==GraphMessage.EDGE)
				{
					s.removeEdge(mm.getOldElementID());
					s.addEdge(mm.getElementID());
				}
			}
			else if (mod==MGraphMessage.REMOVAL)
			{
				if (mm.getModifiedElement()==MGraphMessage.NODE)
					s.removeNode(mm.getElementID());
				else if (mm.getModifiedElement()==GraphMessage.EDGE)
					s.removeEdge(mm.getElementID());
			}
		}//end while
	}
}
