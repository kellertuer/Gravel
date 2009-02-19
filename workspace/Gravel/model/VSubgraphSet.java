package model;

import java.awt.Color;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.TreeSet;

//import java.util.concurrent.locks.Lock;
//import java.util.concurrent.locks.ReentrantLock;

import model.Messages.GraphColorMessage;
import model.Messages.GraphMessage;

/**
 * Class for handling a Set of Subgraphs. And their specific Modificational Methods
 * 
 * Each Set of Elements, that may be contained in a Subgraph should subscribe for 
 * Color-Update-Messages. ColorUpdateMessage should be handled Graph-Internal
 * 
 * The Graph containing this Set should also subscribe to send specific messages to 
 * other Entities observing the Graph
 * 
 * @author Ronny Bergmann
 *
 */
public class VSubgraphSet extends Observable implements Observer {

	private TreeSet<VSubgraph> vSubgraphs;
//	private Lock SubgraphLock; TODO: Think about the need of this lock
	private MGraph mG;
	
	/**
	 * Create a new Set depending on the MGraph beneath
	 * @param g
	 */
	public VSubgraphSet(MGraph g)
	{
		vSubgraphs = new TreeSet<VSubgraph>(new VSubgraph.SubgraphIndexComparator());
//		SubgraphLock = new ReentrantLock();
		mG = g;
	}

	/**
	 * Add a new Subgraph to the VGraph
	 * The Index of the MSubgraph is ignored (if differs from VSubgraph-Index)
	 * 
	 * The MSubgraph may be used to introduce the new subhraph with already given elements
	 * So if a node in this VGraph exists, that is marked as belonging to the
	 * MSubgraph it gets by adding the new Color added, too
	 * 
	 * If a Subgraph with same index as VSubgraph exists or one of the arguments in null
	 * nothing happens
	 * @paran subgraph new VSubgraph to be added here
	 * @param msubgraph new MSubgraph to be added in MGraph underneath and used for initialization of Subgraph
	 */
	public void add(VSubgraph subgraph, MSubgraph msubgraph)
	{
		if ((subgraph==null)||(msubgraph==null)) //Oneof them Null
				return;
		if (get(subgraph.getIndex())!=null) //Subgraph exists?
			return;
		mG.addSubgraph(msubgraph); //Add the Subgraph mathematically - so the math graph is correct now
		for (int i=0; i<mG.getNextEdgeIndex(); i++)
		{
			if ((mG.getEdge(i)!=null)&&(msubgraph.containsEdge(i)))
			{
					//Notify Edge about Color Update
						setChanged();
						notifyObservers(new GraphColorMessage(GraphColorMessage.EDGE,i,GraphColorMessage.ADDITION,subgraph.getColor()));
			}
			
		}
		for (int i=0; i<mG.getNextNodeIndex(); i++)
		{
			if ((mG.getNode(i)!=null)&&(msubgraph.containsNode(i)))
			{
					//Notify Node about Color Update
						setChanged();
						notifyObservers(new GraphColorMessage(GraphColorMessage.NODE,i,GraphColorMessage.ADDITION,subgraph.getColor()));
			}
			
		}
		vSubgraphs.add(subgraph.clone());
		setChanged();
		notifyObservers(new GraphMessage(GraphMessage.SUBGRAPH,subgraph.getIndex(),GraphMessage.ADDITION,GraphMessage.ALL_ELEMENTS));	
	}
	/**
	 * get the set with index i
	 * @param i
	 * 		index of the set
	 * @return
	 * 		null, if no set with index i exists, else the set
	 */
	public VSubgraph get(int i) {
		Iterator<VSubgraph> s = vSubgraphs.iterator();
		while (s.hasNext()) {
			VSubgraph actual = s.next();
			if (i == actual.getIndex()) {
				return actual;
			}
		}
		return null;
	}

	/**
	 * remove a set from the VGraph and remove the Sets color from each node or edge contained in the set
	 * <br><br>
	 * if no set exists with given index SetIndex nothing happens
	 * @param subgraphindex
	 * 					Index of the set to be deleted
	 */
	public void remove(int subgraphindex) {
		VSubgraph toDel = get(subgraphindex);
		if (toDel==null)
			return;
		Iterator<MNode> iterNode = mG.getNodeIterator();
		while (iterNode.hasNext()) {
			MNode actual = iterNode.next();
			if (mG.getSubgraph(subgraphindex).containsNode(actual.index))
				removeNodefromSubgraph_(actual.index, subgraphindex);
		}
		Iterator<MEdge> iterEdge = mG.getEdgeIterator();
		while (iterEdge.hasNext()) {
			MEdge actual = iterEdge.next();
			if (mG.getSubgraph(subgraphindex).containsEdge(actual.index))
				removeEdgefromSubgraph_(actual.index, subgraphindex);
		}
		toDel = get(subgraphindex);
		vSubgraphs.remove(toDel);
		mG.removeSubgraph(toDel.getIndex());
		setChanged();
		notifyObservers(new GraphMessage(GraphMessage.SUBGRAPH,subgraphindex,GraphMessage.REMOVAL,GraphMessage.ALL_ELEMENTS));	
	}

	/**
	 * Set the Color of a Subgraph to a new color. Returns true, if the color was changed, else false.
	 * The color is not changed, if another Subgraph already got that color
	 * @param index Index of the Subgraph, whose color should be changed
	 * @param newcolor it should be changed to
	 */
	public void setColor(int index, Color newcolor)
	{
		VSubgraph actual=get(index);
		if (actual==null)
			return;
		if (actual.getClass().equals(newcolor))
			return;
		Color oldcolor = actual.getColor();
		//Notify Nodes
		Iterator<MNode> mni = mG.getNodeIterator();
		while (mni.hasNext())
		{
			MNode n = mni.next();
			if (mG.getSubgraph(index).containsNode(n.index))
			{
				setChanged();
				this.notifyObservers(new GraphColorMessage(GraphColorMessage.NODE,n.index,oldcolor,newcolor));
			}
		}
		Iterator<MEdge> mei = mG.getEdgeIterator();
		while (mei.hasNext())
		{
			MEdge e = mei.next();
			if (mG.getSubgraph(index).containsEdge(e.index))
			{
				setChanged();
				this.notifyObservers(new GraphColorMessage(GraphColorMessage.EDGE,e.index,oldcolor,newcolor));
			}
		}
		actual.setColor(newcolor);
		return;
	}
	/**
	 * add an Edge to a set
	 * @param edgeindex
	 * 			edgeindex
	 * @param subgraphindex
	 * 			setindex
	 * 
	 * @see MGraph.addEdgetoSubgraph(edgeindex,setindex)
	 */
	public void addEdgetoSubgraph(int edgeindex, int subgraphindex) {
		VSubgraph actual = get(subgraphindex);
		if ((mG.getEdge(edgeindex) != null)
				&& (actual!=null)
				&& (!mG.getSubgraph(subgraphindex).containsEdge(edgeindex))) {
			// Mathematisch hinzufuegen
			mG.addEdgetoSubgraph(edgeindex, subgraphindex);
			// Und der Kantenmenge Bescheid sagen
			setChanged();
			notifyObservers(new GraphColorMessage(GraphColorMessage.EDGE,edgeindex,GraphColorMessage.ADDITION,actual.getColor()));
		}
		//global notify
		setChanged();
		notifyObservers(new GraphMessage(GraphMessage.SUBGRAPH,subgraphindex,GraphMessage.UPDATE,GraphMessage.SUBGRAPH|GraphMessage.EDGE));	
	}
	/**
	 * remove an edge from a set
	 * @param edgeindex
	 * 				edge index of the edge to be removed 
	 * @param subgraphindex
	 * 				set index of the set
	 */
	public void removeEdgefromSubgraph(int edgeindex, int subgraphindex) {
		removeEdgefromSubgraph_(edgeindex,subgraphindex);
		//Notify Graph
		setChanged();
		notifyObservers(new GraphMessage(GraphMessage.SUBGRAPH,subgraphindex,GraphMessage.UPDATE,GraphMessage.SUBGRAPH|GraphMessage.EDGE));	
	}
	/**
	 * remove an edge from a set without informing the external observers outside the graph 
	 * ATTENTION : Internal Use only, if you use this methd make sure to notify Observers yourself !
	 * @param edgeindex
	 * @param SetIndex
	 */
	private void removeEdgefromSubgraph_(int edgeindex, int SetIndex)
	{	
		VSubgraph actual = get(SetIndex);
		if (actual==null) //Not existent
			return;
		if (mG.getSubgraph(SetIndex).containsEdge(edgeindex)) 
		{
			mG.removeEdgefromSubgraph(edgeindex, SetIndex);
			//Nodify Edge-Set internal about Change			
			setChanged();
			notifyObservers(new GraphColorMessage(GraphColorMessage.EDGE,edgeindex,GraphColorMessage.REMOVAL,actual.getColor()));
		}
	}
	/**
	 * Add a node to the Set
	 * if the node or the set does not exist, nothing happens
	 * @param nodeindex
	 * 					the node to be added
	 * @param SetIndex
	 * 					the set to be expanded
	 *
	 * @see MGraph.addNodetoSet(nodeindex,setindex)
	 */
	public void addNodetoSubgraph(int nodeindex, int SetIndex) {
		VSubgraph actual = get(SetIndex);
		if ((mG.getNode(nodeindex) != null)
				&& (actual!=null)
				&& (!mG.getSubgraph(SetIndex).containsNode(nodeindex))) {
			// Mathematisch hinzufuegen
			mG.addNodetoSubgraph(nodeindex, SetIndex);
			// Und der Knotenmenge Bescheid sagen
			setChanged();
			notifyObservers(new GraphColorMessage(GraphColorMessage.NODE,nodeindex,GraphColorMessage.ADDITION,actual.getColor()));
		}
		//global notify
		setChanged();
		notifyObservers(new GraphMessage(GraphMessage.SUBGRAPH,SetIndex,GraphMessage.UPDATE,GraphMessage.SUBGRAPH|GraphMessage.NODE));	
	}
	/**
	 * remove a node from a set
	 * if the node or the set does not exist or the node is not in the set, nothing happens
	 * @param nodeindex
	 * 				the node index to be removed from the
	 * @param SetIndex
	 * 				set with this index
	 */
	public void removeNodefromSubgraph(int nodeindex, int SetIndex) {
		removeNodefromSubgraph_(nodeindex, SetIndex);		
		setChanged();
		notifyObservers(new GraphMessage(GraphMessage.SUBGRAPH,SetIndex,GraphMessage.UPDATE,GraphMessage.SUBGRAPH|GraphMessage.NODE));	
	}
	/**
	 * remove Node from Sub set without informing the observsers
	 * ATTENTION : Internal Use only, if you use this method make sure to notify Observers !
	 * @param nodeindex
	 * @param SetIndex
	 */
	private void removeNodefromSubgraph_(int nodeindex, int SetIndex)
	{
		VSubgraph actual = get(SetIndex);
		if (actual==null) //Not existent
			return;
		if (mG.getSubgraph(SetIndex).containsNode(nodeindex))
		{
			mG.removeNodefromSubgraph(nodeindex, SetIndex);
			//Nodify Node-Set internal about Change			
			setChanged();
			notifyObservers(new GraphColorMessage(GraphColorMessage.NODE,nodeindex,GraphColorMessage.REMOVAL,actual.getColor()));
		}
	}

	/**
	 * get a new Iterator for the VSubgraphs
	 * @return
	 * 		an Iterator typed to VSubgraphs
	 */	
	public Iterator<VSubgraph> getIterator() {
		return vSubgraphs.iterator();
	}

	public void update(Observable o, Object arg) {
		//Handle node Deletions
		//Handle Edge Deletions in VGraph
	}
}
