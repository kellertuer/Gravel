package model;


import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import model.Messages.GraphColorMessage;
import model.Messages.GraphConstraints;
import model.Messages.GraphMessage;

/**
 * This Class represents the Visual HyperEdgesEdges as a Set including its Modificational Methods
 * depending on visual hyperedges.
 * 
 * Depending Sets (e.g. a SubGraph-Set) should subscribe to this Class to handle Changes
 * (e.g. hyperedge deletions)
 * 
 * The Graph containing this Set should also subscribe to send specific messages to 
 * other Entities observing the Graph, when this Set changes
 * 
 * @author Ronny Bergmann
 * @since 0.4
 */
public class VHyperEdgeSet extends Observable implements Observer {

	private Lock HyperEdgeLock;
	private MHyperGraph mG;
	private TreeSet<VHyperEdge> vHyperEdges;
	
	public VHyperEdgeSet(MHyperGraph g)
	{
		vHyperEdges = new TreeSet<VHyperEdge>(new VItem.IndexComparator());
		HyperEdgeLock = new ReentrantLock();
		mG = g;
		mG.addObserver(this); //mG is VGraph-internal so node deletions are signaled through this message
	}
	/**
	 * Set all Hyperedges to not selected
	 */
	public void deselect()
	{
		Iterator<VHyperEdge> e = vHyperEdges.iterator();
		while (e.hasNext()) {
			e.next().deselect();
		}
	}
	/**
	 * add a new hyperedge with given visual information in a VHyperEdge to the VHyperEdgeSet
	 *
	 * @param edge 
	 * 				the new VHyperEdge
	 * @param medge 
	 * 				mathematical elements of the new hyperedge, if its index differs, this index is ignored
	 */
	public void add(VHyperEdge edge, MHyperEdge medge) 
	{
		if ((edge==null)||(medge==null))
				return;
		if (medge.index!=edge.getIndex())
			medge.index = edge.getIndex();
//		if (getIndexWithSimilarShape(edge) > 0)
//		{
//			return;
//		}
		if (mG.modifyHyperEdges.add(medge)) //succesfull added in MathGraph
		{
			HyperEdgeLock.lock();
			try {
				vHyperEdges.add(edge); //add edge			
			}
			finally {HyperEdgeLock.unlock();}
			setChanged();
			notifyObservers(new GraphMessage(GraphConstraints.HYPEREDGE,edge.getIndex(),GraphConstraints.ADDITION,GraphConstraints.HYPEREDGE));	
		}
	}
	/**
	 * get the hyper edge with a given index, if exists
	 * @param i
	 * 			index of the searched edge
	 * 
	 * @return the VHyperEdge, if exists, else null 
	 */
	public VHyperEdge get(int i) {
		Iterator<VHyperEdge> n = vHyperEdges.iterator();
		while (n.hasNext()) {
			VHyperEdge temp = n.next();
			if (temp.getIndex() == i) {
				return temp;
			}
		}
		return null;
	}
	/**
	 * Replace the hyper edge with index given by an copy of edge with the parameter,
	 * if a hyper edge with same index as parameter exists, else do nothing
	 * 
	 * Its Subgraph-Properties are not changed
	 * @param e VHyperEdge to be replaced with its Edge with similar index in the graph
	 * @param me MHyperEdge containing the mathematical stuff of the hyper edge 
	 * - its index is ignored, if it differs from first parameter index
	 */
	public void replace(VHyperEdge e, MHyperEdge me)
	{
		if (me.index!=e.getIndex())
			me.index = e.getIndex();
		
		mG.modifyHyperEdges.replace(me);
		e = e.clone(); //Lose color!
		HyperEdgeLock.lock(); //Find the edge to be replaced
		try
		{
			Iterator<VHyperEdge> ei = vHyperEdges.iterator();				
			while (ei.hasNext())
			{
				VHyperEdge t = ei.next();
				if (t.getIndex()==e.getIndex())
				{
					vHyperEdges.remove(t);
					//Clone Color Status of e from t
					t.copyColorStatus(e);
					vHyperEdges.add(e);
					setChanged();
					notifyObservers(new GraphMessage(GraphConstraints.HYPEREDGE,e.getIndex(), GraphConstraints.REPLACEMENT|GraphConstraints.HYPEREDGESHAPE,GraphConstraints.HYPEREDGE));	
					break;
				}
			}
		}
		finally {HyperEdgeLock.unlock();}
	}
	/**
	 * remove the hyper edge with index i from this set if one exists
	 * @param i
	 * 				index of the edge to be removed
	 * @return
	 * 			true, if an edge was removed, false, if not
	 */
	public boolean remove(int i)
	{
			if (remove_(i))
			{
				setChanged();
				notifyObservers(new GraphMessage(GraphConstraints.HYPEREDGE,i,GraphConstraints.REMOVAL,GraphConstraints.HYPEREDGE|GraphConstraints.SUBGRAPH|GraphConstraints.SELECTION));	
				return true;
			}
			return false;
	}
	/**
	 * removes an hyper edge from the Set without notifying the Observers
	 * ATTENTION : Internal Use only, if you Use this method make sure you notify the Observers yourself!
	 * An internal Notify is sent - but only other parts of VHyperGraph are meant to react on that
	 * @param i Index of the Edge to be removed
	 * @return
	 */
	boolean remove_(int i) {
		VHyperEdge toDel = get(i);
		if (toDel == null)
			return false;
		HyperEdgeLock.lock();
		try
		{
 			mG.modifyHyperEdges.remove(i);
			vHyperEdges.remove(toDel);
		} 
		finally {HyperEdgeLock.unlock();}
		return true;
	}
	/**
	 * Checks whether an similar hyper edge exists, that is an hyper edge with same shape (so that would be in visual the same
	 * 
	 * An Edge is not similar to itself because than every edge that is already in the graph an checked against it would return true
	 * @param edge hyper edge to be checked to similarity existence
	 * 
	 * @return the index of the similar hyper edge, if it exists, else 0
	 */
	public int getIndexWithSimilarShape(VHyperEdge edge)
	{
		if (edge==null)
			return 0;
		Iterator<VHyperEdge> hei = vHyperEdges.iterator();
		while (hei.hasNext())
		{
			VHyperEdge act = hei.next();
			//Actual Edge is not the same object as parameter and has same shape
			if ((!act.equals(edge))&&(act.getShape().CurveEquals(edge.getShape())))
			{
				if ((!act.getShape().isEmpty())||(!edge.getShape().isEmpty())) //One of them is nonempty
				return act.getIndex();
			}
		}
		return 0;
	}
	/**
	 * get a new hyper edge iterator
	 * @return
	 * 			a Iterator of type VEdge
	 */
	public Iterator<VHyperEdge> getIterator() {
		return vHyperEdges.iterator();
	}
	/**
	 * get an Control point near the point m
	 * a Control point is any point in a QuadCurveEdge or SegmentedEdge despite source and target
	 * @param m
	 * 			a Point 
	 * @param variation
	 * 			the distance from the point m
	 * 
	 * @return an Vector containing an edge and the number of its CP in Range if exists, else null
	 */
	@SuppressWarnings("unchecked")
	public Vector firstCPinRageOf(Point m, double variation) {
		Iterator<VHyperEdge> n = vHyperEdges.iterator();
		while (n.hasNext()) {
			VHyperEdge temp = n.next(); // naechste Kante
			Point2D ncp = temp.getShape().getNearestCP(m);
			if (ncp.distance(m) <= variation)
			{
				Vector c = new Vector();
				c.add(temp);
				c.add(new Integer(0));
				return c;
			}
		}
		return null; // keinen gefunden
	}
	/**
	 * Check, whether at least one hyper edge is selected
	 * @return true, if there is at least one selected node, else false
	 */
	public boolean hasSelection() {
		Iterator<VHyperEdge> e = vHyperEdges.iterator();
		HyperEdgeLock.lock();
		boolean result = false;
		try
		{
			while (e.hasNext())
				if ((e.next().getSelectedStatus()&VItem.SELECTED)==VItem.SELECTED)
					result=true;
		}
		finally{HyperEdgeLock.unlock();}	
		return result;
	}
	/**
	 * Get the one and simple selected Edge iff such an edge exists, in every other case null is returned
	 * @return
	 */
	public VHyperEdge getSingleSelectedEdge()
	{
		Iterator<VHyperEdge> e = vHyperEdges.iterator();
		HyperEdgeLock.lock();
		VHyperEdge result = null;
		try
		{
			while (e.hasNext())
			{
				VHyperEdge ve = e.next();
				if ((ve.getSelectedStatus()&VItem.SELECTED)==VItem.SELECTED)
				{
					if (result!=null) //This is the second selected Edge
						return new VHyperEdge(-1,-1,-1);
					else
						result = ve;
				}
			}
		}
		finally{HyperEdgeLock.unlock();}	
		return result;
	}
	//
	//private Stuff for handling changes from other sets
	//
	/**
	 * Remove Adjacent Edges of a Node without sending a Message, because this is evoked by
	 * Node Removement, so there is no need to inform anybody
	 * (VSubgraph handles this delete itself)
	 * 
	 * Up to now a little bit sad, because the mG is already updated at this point
	 */
	private void updateRemoval()
	{
		HyperEdgeLock.lock();
		try
		{
			Iterator<VHyperEdge> e = vHyperEdges.iterator();
			HashSet<VHyperEdge> empty = new HashSet<VHyperEdge>();
			while (e.hasNext()) {
				VHyperEdge edge = e.next();
				if (mG.modifyHyperEdges.get(edge.getIndex())==null)
					empty.add(edge);
			}
			e = empty.iterator();
			while (e.hasNext())
			{
				//So remove them silent
				remove_(e.next().getIndex());
			}
		}
		finally {HyperEdgeLock.unlock();}
	}
	/**
	 * React on Color Change in an VSubGraph
	 * @param m the message containing information about change
	 */
	private void Colorchange(GraphColorMessage m)
	{
		if (m.getModifiedElement()!=GraphConstraints.HYPEREDGE)
			return; //Does not affect us
		VHyperEdge e = get(m.getElementID());
		switch(m.getModificationType()) {
			case GraphConstraints.REMOVAL:
				e.removeColor(m.getColor());
				break;
			case GraphConstraints.UPDATE:
				e.removeColor(m.getOldColor()); //After this its equal to addition
			case GraphConstraints.ADDITION:
				e.addColor(m.getColor());
				break;
		}
	}
	public void update(Observable o, Object arg)
	{
		//TODO: find a better way than instanceof
		if (arg instanceof GraphColorMessage)
		{
			GraphColorMessage m = (GraphColorMessage)arg;
			if (m==null)
				return;
			else
				Colorchange(m);
		}
		else if (arg instanceof GraphMessage)
		{
			GraphMessage m = (GraphMessage)arg;
			if (m==null) //No News for us
				return;
			//On Node removal - remove adjacent edges - every other case of node changes does not affect edges
			if ((m.getModifiedElementTypes()==GraphConstraints.NODE)||(m.getModification()==GraphConstraints.REMOVAL)) //Node removement
				updateRemoval();
			//SubGraphChanges don't affect edges, because ColorStuff is handles seperately
		}
	}
}