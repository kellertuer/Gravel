package model;


import java.awt.Point;
import java.awt.geom.Point2D.Double;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import model.Messages.GraphColorMessage;
import model.Messages.GraphMessage;

/**
 * This Class represents the Visual Edges as a Set including its Modificational Methods
 * depending on visual edges.
 * 
 * Depending Sets (e.g. a SubGraph-Set) should subscribe to this Class to handle Changes
 * (e.g. edge deletions)
 * 
 * The Graph containing this Set should also subscribe to send specific messages to 
 * other Entities observing the Graph
 * 
 * @author Ronny Bergmann
 * @since 0.4
 */
public class VEdgeModification extends Observable implements Observer {

	private Lock EdgeLock;
	private MGraph mG;
	private TreeSet<VEdge> vEdges;
	
	public VEdgeModification(MGraph g)
	{
		vEdges = new TreeSet<VEdge>(new VEdge.EdgeIndexComparator());
		EdgeLock = new ReentrantLock();
		mG = g;
	}
	/**
	 * Set all Edges to not selected
	 */
	public void deselect()
	{
		Iterator<VEdge> e = vEdges.iterator();
		while (e.hasNext()) {
			e.next().deselect();
		}
	}
	/**
	 * Sets the Indicator for Loops in the graph to the parameter value
	 *
	 * @param graph TODO
	 * @param b new Acceptance of Loops
	 */
	public BitSet setLoopsAllowed(VGraph graph, boolean b)
	{
		BitSet removed = new BitSet();
		if ((mG.isLoopAllowed())&&(!b)) //disbabling
		{
			EdgeLock.lock();
			try
			{
				HashSet<VEdge> deledges = new HashSet<VEdge>();
				Iterator<VEdge> n2 = vEdges.iterator();
				while (n2.hasNext())
				{
					VEdge e = n2.next();
					MEdge me = mG.getEdge(e.getIndex());
					if (me.StartIndex==me.EndIndex)
					{
						removed.set(e.getIndex());
						deledges.add(e);
					}
					else
						removed.clear(e.getIndex());
				}
				Iterator<VEdge> n3 = deledges.iterator();
				while (n3.hasNext()) // Diese loeschen
				{
					removeEdge(n3.next().getIndex());
				}
			} finally {EdgeLock.unlock();}
		}	
		if (b!=mG.isLoopAllowed())
		{
			if (mG.setLoopsAllowed(b).cardinality() > 0)
			{
				System.err.println("DEBUG : Beim Umwandeln ds Graphen in schleifenlos stimmt was nicht, ");
			}
			setChanged();
			//Loops done, update Edges
			graph.notifyObservers(new GraphMessage(GraphMessage.LOOPS,GraphMessage.UPDATE,GraphMessage.EDGE));	
		}
		return removed;
	}
	/**
	 * add a new edge with given visual information in a VEdge from a source to a target and a value 
	 * @param edge 
	 * 				the new VEdge
	 * @param medge 
	 * 				mathematical elements of the new edge, if its index differs, this index is ignored
	 * @param startPoint TODO
	 * @param endPoint TODO
	 */
	public void addEdge(VEdge edge, MEdge medge, Point start, Point end) 
	{
		if ((edge==null)||(medge==null))
				return;
		if (medge.index!=edge.getIndex())
			medge.index = edge.getIndex();
		if (similarPathEdgeIndex(edge, medge.StartIndex,medge.EndIndex) > 0)
		{
			System.err.println("DEBUG : Similar Edge Exists, doing nothing");
			return;
		}
		if (mG.addEdge(medge)) //succesfull added in MathGraph
		{
			EdgeLock.lock();
			try 
			{
				// In einem ungerichteten Graphen existiert eine Kante von e zu s und die ist StraightLine und die neue Kante ist dies auch	
				if ((medge.StartIndex!=medge.EndIndex)&&(mG.isDirected())&&(mG.existsEdge(medge.EndIndex, medge.StartIndex)==1)&&(getEdge(mG.getEdgeIndices(medge.EndIndex, medge.StartIndex).firstElement()).getType()==VEdge.STRAIGHTLINE)&&(edge.getType()==VEdge.STRAIGHTLINE))
				{ //Dann würde diese Kante direkt auf der anderen liegen
					Point dir = new Point(end.x-start.x,end.y-start.y);
					double length = dir.distanceSq(new Point(0,0));
					Double orthogonal_norm = new Double ((double)dir.y/length,-(double)dir.x/length);
					Point bz1 = new Point(Math.round((float)start.x + (float)dir.x/2 + (float)orthogonal_norm.x*(float)length/4),Math.round((float)start.y + (float)dir.y/2 + (float)orthogonal_norm.y*(float)length/4));
					Point bz2 = new Point(Math.round((float)start.x + (float)dir.x/2 - (float)orthogonal_norm.x*(float)length/4),Math.round((float)start.y + (float)dir.y/2 - (float)orthogonal_norm.y*(float)length/4));
					VEdgeArrow arr = edge.getArrow().clone();
					//Update the new Edge
					edge = new VQuadCurveEdge(edge.getIndex(),edge.width,bz1);
					edge.setArrow(arr);
					//Update the old edge
					VEdge temp = getEdge(mG.getEdgeIndices(medge.EndIndex, medge.StartIndex).firstElement());
					arr = temp.getArrow().clone();
					VEdge tempcolorEdge = temp.clone();
					vEdges.remove(temp);
					temp = new VQuadCurveEdge(temp.getIndex(),temp.width,bz2);
					temp.setArrow(arr);
					tempcolorEdge.copyColorStatus(temp);
					vEdges.add(temp); //add modified edge in counter directtion
				}
				vEdges.add(edge); //add edge
				mG.setEdgeName(edge.getIndex(), medge.name);
			} 
			finally {EdgeLock.unlock();}
			setChanged();
			notifyObservers(new GraphMessage(GraphMessage.EDGE,edge.getIndex(),GraphMessage.ADDITION,GraphMessage.EDGE));	
		}
	}
	/**
	 * get the edge with a given index, if existens
	 * @param i
	 * 			index of the searched edge
	 * 
	 * @return the edge, if existens, else null 
	 */
	public VEdge getEdge(int i) {
		Iterator<VEdge> n = vEdges.iterator();
		while (n.hasNext()) {
			VEdge temp = n.next();
			if (temp.getIndex() == i) {
				return temp;
			}
		}
		return null;
	}
	/**
	 * Replace the edge with index given by an copy of edge with the parameter,
	 * if a node with same index as parameter exists, else do nothing
	 * 
	 * Its SubSetStuff is not changed
	 * @param e VEdge to be replaced with its Edge with similar index in the graph
	 * @param me Medge containing the mathematical stuff of the VEdge - index of his edge is ignored,
	 * 			if it differs from first parameter index
	 */
	public void replaceEdge(VEdge e, MEdge me)
	{
		if (me.index!=e.getIndex())
			me.index = e.getIndex();
		
		mG.replaceEdge(me);
		e = e.clone(); //Lose color!
		EdgeLock.lock(); //Find the edge to be replaced
		try
		{
			Iterator<VEdge> ei = vEdges.iterator();				
			while (ei.hasNext())
			{
				VEdge t = ei.next();
				if (t.getIndex()==e.getIndex())
				{
					vEdges.remove(t);
					//Clone Color Status of e from t
					e.copyColorStatus(t);
					vEdges.add(e);
					setChanged();
					notifyObservers(new GraphMessage(GraphMessage.EDGE,e.getIndex(), GraphMessage.REPLACEMENT,GraphMessage.EDGE));	
					break;
				}
			}
		}
		finally {EdgeLock.unlock();}
	}
	/**
	 * remove the edge with index i
	 * @param i
	 * 				index of the edge to be removed
	 * @return
	 * 			true, if an edge was removed, false, if not
	 */
	public boolean removeEdge(int i)
	{
			if (removeEdge_(i))
			{
				setChanged();
				notifyObservers(new GraphMessage(GraphMessage.EDGE,i,GraphMessage.REMOVAL,GraphMessage.EDGE|GraphMessage.SUBSET|GraphMessage.SELECTION));	
				return true;
			}
			return false;
	}
	/**
	 * removes an Edge from the Graph without notifying the Observers
	 * ATTENTION : Internal Use only, if you Use this method make sure you notify the Observers yourself!
	 * @param i Index of the Edge to be removed
	 * @return
	 */
	boolean removeEdge_(int i) {
		if (getEdge(i) == null)
			return false;
		EdgeLock.lock();
		try
		{
 			mG.removeEdge(i);
			vEdges.remove(getEdge(i));
		} 
		finally {EdgeLock.unlock();}
		return true;
	}
	/**
	 * sets the the name of a node with the index, if this node exists, else it does nothing
	 * @param i
	 * @param newname
	 * @see MGraph.setNodeName()
	 */
	public void setEdgeName(int i, String newname) {
		mG.setEdgeName(i, newname);
		setChanged();
		notifyObservers(new GraphMessage(GraphMessage.EDGE,i,GraphMessage.UPDATE,GraphMessage.EDGE));	
	}
	/**
	 * Checks whether an similar edge exists between s and e
	 * If multiple edges are allowed, an edge is similar, if it has same type and same path
	 * If multiples are not allowed, an edge is similar, if it has same start and end
	 * 
	 * An Edge is not similar to itself because than every edge that is already in the graph an checked against it would return true
	 * @param edge edge to be checked to similarity existence
	 * @param s start node index
	 * @param e end node index
	 * 
	 * @return the index of the similar edge, if it exists, else 0
	 */
	public int similarPathEdgeIndex(VEdge edge, int s, int e)
	{
		if (edge==null)
			return 0;
		//Check whether an edge is the same as this if multiples are allowed
		if (mG.isMultipleAllowed())
		{
			Vector<Integer> indices = mG.getEdgeIndices(s,e);
			Iterator<Integer> iiter = indices.iterator();
			while (iiter.hasNext())
			{
				VEdge act = getEdge(iiter.next());
				MEdge me = mG.getEdge(act.getIndex());
				if ((me.StartIndex==e)&&(!mG.isDirected())&&(act.getType()==VEdge.ORTHOGONAL)&&(edge.getType()==VEdge.ORTHOGONAL)) 
				//ungerichtet, beide orthogonal und entgegengesetz gespeichert
				{
					if (((VOrthogonalEdge)act).getVerticalFirst()!=((VOrthogonalEdge)edge).getVerticalFirst())
						return act.getIndex();
				}
				else if ((edge.PathEquals(act))&&(edge.getIndex()!=act.getIndex())) //same path but different indexx
				{
					return act.getIndex();
				}
	
			}
		}
		else if (mG.getEdgeIndices(s,e).size()>0)
		{
			return mG.getEdgeIndices(s,e).firstElement();
		}
		return 0;
	}
	/**
	 * get a new edge iterator
	 * @return
	 * 			a Iterator of type VEdge
	 */
	public Iterator<VEdge> getEdgeIterator() {
		return vEdges.iterator();
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
	public Vector getControlPointinRange(Point m, double variation) {
		Iterator<VEdge> n = vEdges.iterator();
		while (n.hasNext()) {
			VEdge temp = n.next(); // naechste Kante
			switch (temp.getType()) {
				case VEdge.LOOP :
				case VEdge.QUADCURVE : // Wenns eine Bezierkante ist
				{
					Point p = temp.getControlPoints().firstElement();
					if (p.distance(m) <= variation) {
						Vector c = new Vector();
						c.add(temp);
						c.add(new Integer(0));
						return c;
					}
					break;
				}
				case VEdge.SEGMENTED : {
					Vector<Point> p = temp.getControlPoints();
					for (int i = 0; i < p.size(); i++) {
						if (p.get(i).distance(m) <= variation) {
							Vector c = new Vector();
							c.add(temp); // Kante anfügen
							c.add(new Integer(i)); // Punkt angeben
							return c;
						}
					}
					break;
				}
				default : {
					break;
				} // Sonst - Straightline
			}
		}
		return null; // keinen gefunden
	}
	/**
	 * Check, whether at least one node is selected
	 * @return true, if there is at least one selected node, else false
	 */
	public boolean selectedEdgeExists() {
		Iterator<VEdge> e = vEdges.iterator();
		while (e.hasNext()) {
			if ((e.next().getSelectedStatus()&VItem.SELECTED)==VItem.SELECTED)
				return true;
		}
		return false;
	}
	//
	//private Stuff for handling changes from other sets
	//
	/**
	 * Remove Adjacent Edges of a Node without sending a Message, because this is evoked by
	 * Node Removement, so there is no need to inform anybody
	 * (SubSet handles this delete itself
	 * 
	 * Up to now a little bit sad, because the mG is already updated at this point
	 */
	private void updateRemoval()
	{
		Iterator<VEdge> e = vEdges.iterator();
		HashSet<VEdge> adjacent = new HashSet<VEdge>();
		while (e.hasNext()) {
			VEdge edge = e.next();
			if (mG.getEdge(edge.getIndex())==null) //This VEdge-Set is outdated
					adjacent.add(edge);
		}
		e = adjacent.iterator();
		while (e.hasNext())
		{
			//So remove them silent
			removeEdge_(e.next().getIndex());
		}
	}
	private void Colorchange(GraphColorMessage m)
	{
		if (m.getModifiedElement()!=GraphColorMessage.EDGE)
			return; //Does not affect us
		VEdge e = getEdge(m.getElementID());
		switch(m.getModificationType()) {
			case GraphColorMessage.REMOVAL:
				e.removeColor(m.getColor());
				break;
			case GraphColorMessage.UPDATE:
				e.removeColor(m.getOldColor()); //After this its equal to addition
			case GraphColorMessage.ADDITION:
				e.addColor(m.getColor());
				break;
		}
	}
	//Handle Updates of Nodes
	public void update(VNodeModification o, GraphMessage m) {
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
			if ((m.getModifiedElementTypes()==GraphMessage.NODE)||(m.getModification()==GraphMessage.REMOVAL)) //Node removement
				updateRemoval();
			//SubSetChanges don't affect edges, because ColorStuff is handles seperately
		}
	}
}
