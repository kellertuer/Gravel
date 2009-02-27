package model;

import java.awt.Color;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.TreeSet;

//import java.util.concurrent.locks.Lock;
//import java.util.concurrent.locks.ReentrantLock;

import model.Messages.GraphColorMessage;
import model.Messages.GraphConstraints;
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
 * Visual Node Set is based on an mathematical structure, e.g. MGraph or MHyperGraph
 *
 * @author Ronny Bergmann
 *
 */
public class VSubgraphSet extends Observable implements Observer {

	private TreeSet<VSubgraph> vSubgraphs;
//	private Lock SubgraphLock; TODO: Think about the need of this lock
	private MGraphInterface mG;
	private int allgraphElements, edgeElement;
	MSubgraphSet msubgraphs;
	MNodeSet mnodes;
	/**
	 * Create a new Set depending on the MGraph beneath
	 * @param g
	 */
	public VSubgraphSet(MGraphInterface g)
	{
		vSubgraphs = new TreeSet<VSubgraph>(new VSubgraph.SubgraphIndexComparator());
//		SubgraphLock = new ReentrantLock();
		mG = g;
		if (mG.getType()==MGraphInterface.GRAPH)
		{
			allgraphElements = GraphConstraints.GRAPH_ALL_ELEMENTS;
			edgeElement = GraphConstraints.EDGE;
			msubgraphs = ((MGraph)mG).modifySubgraphs;			
			mnodes = ((MGraph)mG).modifyNodes;
		}
		else
		{
			edgeElement = GraphConstraints.HYPEREDGE;
			allgraphElements = GraphConstraints.HYPERGRAPH_ALL_ELEMENTS;
			msubgraphs = ((MHyperGraph)mG).modifySubgraphs;
			mnodes = ((MHyperGraph)mG).modifyNodes;
		}
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
		msubgraphs.add(msubgraph);
		//Update Edges / HyperEdges 
		if (mG.getType()==MGraphInterface.GRAPH)
		{
			for (int i=0; i<((MGraph)mG).modifyEdges.getNextIndex(); i++)
			{
				if ((((MGraph)mG).modifyEdges.get(i)!=null)&&(msubgraph.containsEdge(i)))
				{
							setChanged();
							notifyObservers(new GraphColorMessage(GraphConstraints.EDGE,i,GraphConstraints.ADDITION,subgraph.getColor()));
				}
			}
		}
		else
		{
			for (int i=0; i<((MHyperGraph)mG).modifyHyperEdges.getNextIndex(); i++)
			{
				if ((((MHyperGraph)mG).modifyHyperEdges.get(i)!=null)&&(msubgraph.containsEdge(i)))
				{
							setChanged();
							notifyObservers(new GraphColorMessage(GraphConstraints.HYPEREDGE,i,GraphConstraints.ADDITION,subgraph.getColor()));
				}
			}
		}
		//Nodes
		for (int i=0; i<mnodes.getNextIndex(); i++)
		{
			if ((mnodes.get(i)!=null)&&(msubgraph.containsEdge(i)))
			{
						setChanged();
						notifyObservers(new GraphColorMessage(GraphConstraints.NODE,i,GraphConstraints.ADDITION,subgraph.getColor()));
			}
		}
		vSubgraphs.add(subgraph.clone());
		setChanged();
		notifyObservers(new GraphMessage(GraphConstraints.SUBGRAPH,subgraph.getIndex(),GraphConstraints.ADDITION,allgraphElements));	
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
		Iterator<MNode> iterNode;
		iterNode = mnodes.getIterator();
		while (iterNode.hasNext()) {
			MNode actual = iterNode.next();
			if (msubgraphs.get(subgraphindex).containsNode(actual.index))
				removeNodefromSubgraph_(actual.index, subgraphindex);
		}
		if (mG.getType()==MGraphInterface.GRAPH)
		{
			Iterator<MEdge> iterEdge = ((MGraph)mG).modifyEdges.getIterator();
			while (iterEdge.hasNext()) {
				MEdge actual = iterEdge.next();
				if (msubgraphs.get(subgraphindex).containsEdge(actual.index))
					removeEdgefromSubgraph_(actual.index, subgraphindex);
			}
		}
		else
		{
			Iterator<MHyperEdge> iterEdge = ((MHyperGraph)mG).modifyHyperEdges.getIterator();
			while (iterEdge.hasNext()) {
				MHyperEdge actual = iterEdge.next();
				if (msubgraphs.get(subgraphindex).containsEdge(actual.index))
					removeEdgefromSubgraph_(actual.index, subgraphindex);
			}
		}
		toDel = get(subgraphindex);
		vSubgraphs.remove(toDel);
		msubgraphs.remove(toDel.getIndex());
		setChanged();
		notifyObservers(new GraphMessage(GraphConstraints.SUBGRAPH,subgraphindex,GraphConstraints.REMOVAL,allgraphElements));	
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
		Iterator<MNode> mni = mnodes.getIterator();
		while (mni.hasNext())
		{
			MNode n = mni.next();
			if (msubgraphs.get(index).containsNode(n.index))
			{
				setChanged();
				this.notifyObservers(new GraphColorMessage(GraphConstraints.NODE,n.index,oldcolor,newcolor));
			}
		}
		if (mG.getType()==MGraphInterface.GRAPH)
		{
			Iterator<MEdge> mei = ((MGraph)mG).modifyEdges.getIterator();
			while (mei.hasNext())
			{
				MEdge e = mei.next();
				if (msubgraphs.get(index).containsEdge(e.index))
				{
					setChanged();
					this.notifyObservers(new GraphColorMessage(GraphConstraints.EDGE,e.index,oldcolor,newcolor));
				}
			}
		}
		else
		{
			Iterator<MHyperEdge> mei = ((MHyperGraph)mG).modifyHyperEdges.getIterator();
			while (mei.hasNext())
			{
				MHyperEdge e = mei.next();
				if (msubgraphs.get(index).containsEdge(e.index))
				{
					setChanged();
					this.notifyObservers(new GraphColorMessage(GraphConstraints.HYPEREDGE,e.index,oldcolor,newcolor));
				}
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
		if ( (mG.getType()==MGraphInterface.GRAPH) && ((((MGraph)mG).modifyEdges.get(edgeindex))==null))
			return;
		if ( (mG.getType()==MGraphInterface.HYPERGRAPH) && ((((MHyperGraph)mG).modifyHyperEdges.get(edgeindex))==null))
			return;
			
		if ((actual!=null) && (!msubgraphs.get(subgraphindex).containsEdge(edgeindex))) {
			// Mathematisch hinzufuegen
			msubgraphs.addEdgetoSubgraph(edgeindex, subgraphindex);
			// Und der Kantenmenge Bescheid sagen
			setChanged();
			notifyObservers(new GraphColorMessage(edgeElement,edgeindex,GraphConstraints.ADDITION,actual.getColor()));
			//global notify
			setChanged();
			notifyObservers(new GraphMessage(GraphConstraints.SUBGRAPH,subgraphindex,GraphConstraints.UPDATE,GraphConstraints.SUBGRAPH|GraphConstraints.EDGE));	
		}
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
		notifyObservers(new GraphMessage(GraphConstraints.SUBGRAPH,subgraphindex,GraphConstraints.UPDATE,GraphConstraints.SUBGRAPH|edgeElement));	
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
		if (msubgraphs.get(SetIndex).containsEdge(edgeindex)) 
		{
			msubgraphs.removeEdgefromSubgraph(edgeindex, SetIndex);
			//Notify Edge-Set internal about Change			
			setChanged();
			notifyObservers(new GraphColorMessage(edgeElement,edgeindex,GraphConstraints.REMOVAL,actual.getColor()));
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
		if ((mnodes.get(nodeindex) != null)
				&& (actual!=null)
				&& (!msubgraphs.get(SetIndex).containsNode(nodeindex))) {
			// Mathematisch hinzufuegen
			msubgraphs.addNodetoSubgraph(nodeindex, SetIndex);
			// Und der Knotenmenge Bescheid sagen
			setChanged();
			notifyObservers(new GraphColorMessage(GraphConstraints.NODE,nodeindex,GraphConstraints.ADDITION,actual.getColor()));
		}
		//global notify
		setChanged();
		notifyObservers(new GraphMessage(GraphConstraints.SUBGRAPH,SetIndex,GraphConstraints.UPDATE,GraphConstraints.SUBGRAPH|GraphConstraints.NODE));	
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
		notifyObservers(new GraphMessage(GraphConstraints.SUBGRAPH,SetIndex,GraphConstraints.UPDATE,GraphConstraints.SUBGRAPH|GraphConstraints.NODE));	
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
		if (msubgraphs.get(SetIndex).containsNode(nodeindex))
		{
			msubgraphs.removeNodefromSubgraph(nodeindex, SetIndex);
			//Nodify Node-Set internal about Change			
			setChanged();
			notifyObservers(new GraphColorMessage(GraphConstraints.NODE,nodeindex,GraphConstraints.REMOVAL,actual.getColor()));
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
