package model;

import java.awt.Color;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.TreeSet;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
public class VSubSetModification extends Observable implements Observer {

	private TreeSet<VSubSet> vSubSets;
	private Lock SubSetLock;
	private MGraph mG;
	
	/**
	 * Create a new Set depending on the MGraph beneath
	 * @param g
	 */
	public VSubSetModification(MGraph g)
	{
		vSubSets = new TreeSet<VSubSet>(new VSubSet.SubSetIndexComparator());
		SubSetLock = new ReentrantLock();
		mG = g;
	}

	/**
	 * Add a new SubSet to the VGraph
	 * The Index of the msubset is ignored (if differs from VSubSet-Index)
	 * 
	 * The Mathematical SubSet may be used to introduce the new subset with already given stuff
	 * So if a node in this VGraph exists, that is marked as belonging to the
	 * MSubSet it gets by adding the new Color added, too
	 * 
	 * If a SubSet with same index as VSUbSet exists or one of the arguments in null
	 * nothing happens
	 * @paran subset new VSubSet to be added here
	 * @param msubset new MSubSet to be added in MGraph underneath and used for initialization of SubSet
	 */
	public void addSubSet(VSubSet subset, MSubSet msubset)
	{
		if ((subset==null)||(msubset==null)) //Oneof them Null
				return;
		if (getSubSet(subset.getIndex())!=null) //SubSet exists?
			return;
		//Create an empty temporary SubSet for Math
		MSubSet temp = new MSubSet(subset.getIndex(),msubset.getName());		
		mG.addSubSet(temp);
		for (int i=0; i<mG.getNextEdgeIndex(); i++)
		{
			if ((mG.getEdge(i)!=null)&&(msubset.containsEdge(i)))
			{
					//Notify Edge about Color Update
						setChanged();
						notifyObservers(new GraphColorMessage(GraphColorMessage.EDGE,i,GraphColorMessage.ADDITION,subset.getColor()));
			}
			
		}
		for (int i=0; i<mG.getNextNodeIndex(); i++)
		{
			if ((mG.getNode(i)!=null)&&(msubset.containsNode(i)))
			{
					//Notify Node about Color Update
						setChanged();
						notifyObservers(new GraphColorMessage(GraphColorMessage.NODE,i,GraphColorMessage.ADDITION,subset.getColor()));
			}
			
		}
		vSubSets.add(subset.clone());
		setChanged();
		notifyObservers(new GraphMessage(GraphMessage.SUBSET,subset.getIndex(),GraphMessage.ADDITION,GraphMessage.ALL_ELEMENTS));	
	}
	/**
	 * get the set with index i
	 * @param i
	 * 		index of the set
	 * @return
	 * 		null, if no set with index i exists, else the set
	 */
	public VSubSet getSubSet(int i) {
		Iterator<VSubSet> s = vSubSets.iterator();
		while (s.hasNext()) {
			VSubSet actual = s.next();
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
	 * @param SetIndex
	 * 					Index of the set to be deleted
	 */
	public void removeSubSet(int SetIndex) {
		VSubSet toDel = null;
		if (getSubSet(SetIndex)==null)
			return;
		Iterator<MNode> iterNode = mG.getNodeIterator();
		while (iterNode.hasNext()) {
			MNode actual = iterNode.next();
			if (mG.getSubSet(SetIndex).containsNode(actual.index))
				removeNodefromSubSet_(actual.index, SetIndex);
		}
		Iterator<MEdge> iterEdge = mG.getEdgeIterator();
		while (iterEdge.hasNext()) {
			MEdge actual = iterEdge.next();
			if (mG.getSubSet(SetIndex).containsEdge(actual.index))
				removeEdgefromSubSet_(actual.index, SetIndex);
		}
		Iterator<VSubSet> iterSet = vSubSets.iterator();
		while (iterSet.hasNext()) {
			VSubSet actual = iterSet.next();
			if (actual.getIndex() == SetIndex)
				toDel = actual;
		}
		vSubSets.remove(toDel);
		mG.removeSubSet(toDel.getIndex());
		setChanged();
		notifyObservers(new GraphMessage(GraphMessage.SUBSET,SetIndex,GraphMessage.REMOVAL,GraphMessage.ALL_ELEMENTS));	
	}

	/**
	 * Set the Color of a Subset to a new color. Returns true, if the color was changed, else false.
	 * The color is not changed, if another Subset already got that color
	 * @param index Index of the SubSet, whose color should be changed
	 * @param newcolor it should be changed to
	 */
	public void setSubSetColor(int index, Color newcolor)
	{
		VSubSet actual=getSubSet(index);
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
			if (mG.getSubSet(index).containsNode(n.index))
			{
				setChanged();
				this.notifyObservers(new GraphColorMessage(GraphColorMessage.NODE,n.index,oldcolor,newcolor));
			}
		}
		Iterator<MEdge> mei = mG.getEdgeIterator();
		while (mei.hasNext())
		{
			MEdge e = mei.next();
			if (mG.getSubSet(index).containsEdge(e.index))
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
	 * @param SetIndex
	 * 			setindex
	 * 
	 * @see MGraph.addEdgetoSet(edgeindex,setindex)
	 */
	public void addEdgetoSubSet(int edgeindex, int SetIndex) {
		VSubSet actual = getSubSet(SetIndex);
		if ((mG.getEdge(edgeindex) != null)
				&& (actual!=null)
				&& (!mG.getSubSet(SetIndex).containsEdge(edgeindex))) {
			// Mathematisch hinzufuegen
			mG.addEdgetoSubSet(edgeindex, SetIndex);
			// Und der Kantenmenge Bescheid sagen
			setChanged();
			notifyObservers(new GraphColorMessage(GraphColorMessage.EDGE,edgeindex,GraphColorMessage.ADDITION,actual.getColor()));
		}
		//global notify
		setChanged();
		notifyObservers(new GraphMessage(GraphMessage.SUBSET,SetIndex,GraphMessage.UPDATE,GraphMessage.SUBSET|GraphMessage.EDGE));	
	}
	/**
	 * remove an edge from a set
	 * @param edgeindex
	 * 				edge index of the edge to be removed 
	 * @param SetIndex
	 * 				set index of the set
	 */
	public void removeEdgefromSubSet(int edgeindex, int SetIndex) {
		removeEdgefromSubSet_(edgeindex,SetIndex);
		//Notify Graph
		setChanged();
		notifyObservers(new GraphMessage(GraphMessage.SUBSET,SetIndex,GraphMessage.UPDATE,GraphMessage.SUBSET|GraphMessage.EDGE));	
	}
	/**
	 * remove an edge from a set without informing the external observers outside the graph 
	 * ATTENTION : Internal Use only, if you use this methd make sure to notify Observers yourself !
	 * @param edgeindex
	 * @param SetIndex
	 */
	private void removeEdgefromSubSet_(int edgeindex, int SetIndex)
	{	
		VSubSet actual = getSubSet(SetIndex);
		if (actual==null) //Not existent
			return;
		if (mG.getSubSet(SetIndex).containsEdge(edgeindex)) 
		{
			mG.removeEdgefromSet(edgeindex, SetIndex);
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
	public void addNodetoSubSet(int nodeindex, int SetIndex) {
		VSubSet actual = getSubSet(SetIndex);
		if ((mG.getNode(nodeindex) != null)
				&& (actual!=null)
				&& (!mG.getSubSet(SetIndex).containsNode(nodeindex))) {
			// Mathematisch hinzufuegen
			mG.addNodetoSubSet(nodeindex, SetIndex);
			// Und der Knotenmenge Bescheid sagen
			setChanged();
			notifyObservers(new GraphColorMessage(GraphColorMessage.NODE,nodeindex,GraphColorMessage.ADDITION,actual.getColor()));
		}
		//global notify
		setChanged();
		notifyObservers(new GraphMessage(GraphMessage.SUBSET,SetIndex,GraphMessage.UPDATE,GraphMessage.SUBSET|GraphMessage.NODE));	
	}
	/**
	 * remove a node from a set
	 * if the node or the set does not exist or the node is not in the set, nothing happens
	 * @param nodeindex
	 * 				the node index to be removed from the
	 * @param SetIndex
	 * 				set with this index
	 */
	public void removeNodefromSubSet(int nodeindex, int SetIndex) {
		removeNodefromSubSet_(nodeindex, SetIndex);		
		setChanged();
		notifyObservers(new GraphMessage(GraphMessage.SUBSET,SetIndex,GraphMessage.UPDATE,GraphMessage.SUBSET|GraphMessage.NODE));	
	}
	/**
	 * remove Node from Sub set without informing the observsers
	 * ATTENTION : Internal Use only, if you use this method make sure to notify Observers !
	 * @param nodeindex
	 * @param SetIndex
	 */
	private void removeNodefromSubSet_(int nodeindex, int SetIndex)
	{
		VSubSet actual = getSubSet(SetIndex);
		if (actual==null) //Not existent
			return;
		if (mG.getSubSet(SetIndex).containsNode(nodeindex))
		{
			mG.removeNodefromSet(nodeindex, SetIndex);
			//Nodify Node-Set internal about Change			
			setChanged();
			notifyObservers(new GraphColorMessage(GraphColorMessage.NODE,nodeindex,GraphColorMessage.REMOVAL,actual.getColor()));
		}
	}

	/**
	 * get a new Iterator for the subsets
	 * @return
	 * 		an Iterator typed to VSubSet
	 */	
	public Iterator<VSubSet> getSubSetIterator() {
		return vSubSets.iterator();
	}

	public void update(Observable o, Object arg) {}
}
