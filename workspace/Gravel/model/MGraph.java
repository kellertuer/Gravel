package model;

import java.util.BitSet;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Observable;

import model.Messages.GraphMessage;
/**
 * MGraph
 * 
 * The pure mathematical graph
 * 
 * every node is represented by an index and its name
 * 
 * every edge is represened by its index, a start and an endnode, a value and its name
 * 
 * every subgraph contains its index, a name and the information which nodes and egdes are in this subgraph
 * 
 * in addition the mgraph contains information whether loops (startnode==endnode) or multiple edges between 2 nodes are allowed
 * This class is called from multiple threads, so its implemented threadsafe 
 *   
 * @author Ronny Bergmann
 *
 */

public class MGraph extends Observable
{
	public static final int EDGESTARTINDEX = 0;
	public static final int EDGEENDINDEX = 1;
	public static final int EDGEVALUE = 2;
	
	private HashSet<MNode> mNodes;
	private HashSet<MEdge> mEdges;
	private HashSet<MSubSet> mSubSets;
	private boolean directed, allowloops, allowmultiple = false;
	Lock EdgeLock, NodeLock;
	/**
	 * Create a new Graph where
	 * @param d indicates whether edges are directed (true) or not (false
	 * @param l indicates whether loops are allowed or not
	 * @param m indicates whether multiple edges between two nodes are allowed
	 */
	public MGraph(boolean d, boolean l, boolean m)
	{
		mNodes = new HashSet<MNode>();
		mEdges = new HashSet<MEdge>();
		mSubSets = new HashSet<MSubSet>();
		EdgeLock = new ReentrantLock();
		NodeLock = new ReentrantLock();
		directed = d;
		allowloops =l;
		allowmultiple = m;
	}
	/**
	 * clone this graph and 
	 * @return the copy
	 */
	public MGraph clone()
	{
		MGraph clone = new MGraph(directed,allowloops, allowmultiple);
		//Untergraphen
		Iterator<MSubSet> n1 = mSubSets.iterator();
		while (n1.hasNext())
		{
			MSubSet actualSet = n1.next();
			clone.addSubSet(actualSet.clone()); //Jedes Set kopieren
		}
		//Knoten
		Iterator<MNode> n2 = mNodes.iterator();
		while (n2.hasNext())
		{
			MNode actualNode = n2.next();
			MNode Nodeclone = new MNode(actualNode.index, actualNode.name);
			clone.addNode(Nodeclone);
			//In alle Sets einfuegen
			n1 = mSubSets.iterator();
			while (n1.hasNext())
			{
				MSubSet actualSet = n1.next();
				if (this.SubSetcontainsNode(actualNode.index, actualSet.getIndex()))
					clone.addNodetoSubSet(actualNode.index,actualSet.getIndex()); //Jedes Set kopieren
			}
		}
		//Analog Kanten
		Iterator<MEdge> n3 = mEdges.iterator();
		while (n3.hasNext())
		{
			MEdge actualEdge = n3.next();
			MEdge cEdge = new MEdge(actualEdge.index, actualEdge.StartIndex, actualEdge.EndIndex, actualEdge.Value, actualEdge.name);
			clone.addEdge(cEdge);
			//In alle Sets einfuegen
			n1 = mSubSets.iterator();
			while (n1.hasNext())
			{
				MSubSet actualSet = n1.next();
				if (this.SubSetcontainsEdge(actualEdge.index, actualSet.getIndex()))
					clone.addEdgetoSubSet(actualEdge.index,actualSet.getIndex()); //Jedes Set kopieren
			}
		}
		//und zurückgeben
		return clone;
	}
	 /** informs all subscribers about a change. This Method is used to push a notify from outside
	 * mit dem Oject o als Parameter
	 */
	public void pushNotify(Object o) {
		setChanged();
		if (o == null)
			notifyObservers();
		else
			notifyObservers(o);
	}	
	/**
	 * Indicator whether the graph is directed or not
	 * @return
	 */
	public boolean isDirected()
	{
		return directed;
	}
	/**
	 * Set the graph to directed or not. if the graph is set to non-directed, all 
	 * @param d
	 */
	public BitSet setDirected(boolean d)
	{
		BitSet removed = new BitSet();
		if (d==directed)
			return removed; //nicht geändert
		//Auf gerihctet umstellen ist kein Problem. 
		if ((!d)&&(!allowmultiple)) //if multiple edges are allowed we don't need to delete them
									//auf ungerichtet umstellen, existieren Kanten i->j und j->i so lösche eine
		{
			NodeLock.lock(); //Knoten finden
			try
			{
				Iterator<MNode> n = mNodes.iterator();				
				while (n.hasNext())
				{
					MNode t = n.next();
					Iterator<MNode> n2 = mNodes.iterator();
					while (n2.hasNext())
					{
						MNode t2 = n2.next();
						if (t.index <= t2.index)
						{
							Vector<Integer> ttot2 = getEdgeIndices(t.index,t2.index);
							Vector<Integer> t2tot = getEdgeIndices(t2.index,t.index);
							//In the nonmultiple case each Vector has exactely one or no edge in it
							if ((!ttot2.isEmpty())&&(!t2tot.isEmpty()))
							{
								int e1 = ttot2.firstElement();
								int e2 = t2tot.firstElement();
								MEdge m = getEdge(e2);
								m.Value = getEdge(e2).Value+getEdge(e1).Value;
								removeEdge(e1);
								removed.set(e1);
							}
						}
					}
				}
			}
			finally {NodeLock.unlock();}
		}
		directed = d;
		setChanged();
		notifyObservers(
				new GraphMessage(GraphMessage.EDGE|GraphMessage.DIRECTION, //Type
								GraphMessage.UPDATE) //Status 
			);

		return removed;
	}
	/**
	 * Indicates whether loops are allowed or not
	 * @return true if loops are allowed, else false
	 */
	public boolean isLoopAllowed() {
		return allowloops;
	}
	/**
	 * Set the Indicator for loops to a new value. If they are disabled, all loops are removed and 
	 * @param a the new value for the indicator
	 * @return a bitset of all removed edges (if switched loops of)
	 */
	public BitSet setLoopsAllowed(boolean a) 
	{
		BitSet removed = new BitSet();
		if ((allowloops)&&(!a)) //disbabling
		{
			EdgeLock.lock();
			try
			{
				HashSet<MEdge> deledges = new HashSet<MEdge>();
				Iterator<MEdge> n2 = mEdges.iterator();
				while (n2.hasNext())
				{
					MEdge e = n2.next();
					if (e.EndIndex==e.StartIndex)
					{
						removed.set(e.index);
						deledges.add(e);
					}
					else
						removed.clear(e.index);
				}
				Iterator<MEdge> n3 = deledges.iterator();
				while (n3.hasNext()) // Diese loeschen
				{
					mEdges.remove(n3.next());
				}
			} finally {EdgeLock.unlock();}
		}	
		this.allowloops = a;
		setChanged();
		notifyObservers(new GraphMessage(GraphMessage.LOOPS,GraphMessage.UPDATE,GraphMessage.EDGE));	
		return removed;
	}
	/**
	 * Indicates whether multiple edges between two nodes are allowed
	 * @return
	 */
	public boolean isMultipleAllowed() {
		return allowmultiple;
	}
	/**
	 * Set the possibility of multiple edges to the new value
	 * If multiple edges are disabled, the multiple edges are removed and the edge values between two nodes are added
	 * @param a
	 * @return a BitSet where alle indices of deleted edges are set true
	 */
	public BitSet setMultipleAllowed(boolean a) 
	{
		BitSet removed = new BitSet();
		if ((allowmultiple)&&(!a)) //Changed from allowed to not allowed, so remove all multiple
		{	
			NodeLock.lock(); //Knoten finden
			try
			{
				Iterator<MNode> n = mNodes.iterator();				
				while (n.hasNext())
				{
					MNode t = n.next();
					Iterator<MNode> n2 = mNodes.iterator();
					while (n2.hasNext())
					{
						MNode t2 = n2.next();
						//if the graph is directed
						if (((!directed)&&(t2.index<=t.index))||(directed)) //in the nondirected case only half the cases
						{
							if (existsEdge(t.index,t2.index)>1) //we have to delete
							{
								Vector<Integer> multipleedges = getEdgeIndices(t.index,t2.index);
								int value = getEdge(multipleedges.firstElement()).Value;
								//Add up the values and remove the edges from the second to the last
								Iterator<Integer> iter = multipleedges.iterator();
								iter.next();
								while(iter.hasNext())
								{
									int nextindex = iter.next();
									value += getEdge(nextindex).Value;
									removeEdge(nextindex);
									removed.set(nextindex);
								}
								getEdge(multipleedges.firstElement()).Value = value;
							}
						}					
					}
				}
			}
			finally {NodeLock.unlock();}
		}
		this.allowmultiple = a;
		setChanged();
		notifyObservers(new GraphMessage(GraphMessage.MULTIPLE,GraphMessage.UPDATE,GraphMessage.EDGE));	
		return removed;
	}

	
	/*
	 * Knotenfunktionen
	 */
	/**
	 * Adds a new node to the graph with
	 * @param m as the new MNode
	 */
	public void addNode(MNode m)
	{
		if (getNode(m.index)!=null)
			return;
		NodeLock.lock();
		try 
		{
			mNodes.add(m);
			setChanged();
			notifyObservers(new GraphMessage(GraphMessage.NODE,m.index,GraphMessage.ADDITION,GraphMessage.NODE));	
		} 
		finally {NodeLock.unlock();}
	}
	/**
	 * Replace (if existent) the node in the graph with the index of the parameter node by the parameter
	 * 
	 * @param node new node for its index
	 */
	public void replaceNode(MNode node)
	{
		NodeLock.lock();
		try 
		{
			Iterator<MNode> n = mNodes.iterator();
			while (n.hasNext())
			{
				MNode t = n.next();
				if (t.index==node.index)
				{
					mNodes.remove(t);
					mNodes.add(node);
					setChanged();
					notifyObservers(new GraphMessage(GraphMessage.NODE,node.index,GraphMessage.UPDATE,GraphMessage.NODE));	
					break;
				}
			}
		} 
		finally
		{NodeLock.unlock();}		
	}
	/**
	 * Change the index of a node. This method is neccessary, because all other functions rely on the fact, 
	 * that the nodeindex is the reference for everything
	 * 
	 * @param oldi old index of the node
	 * @param newi new index of the node
	 */
	public void changeNodeIndex(int oldi, int newi)
	{
		if (oldi==newi)
			return;
		MNode oldn = null, newn=null;
		NodeLock.lock(); //Knoten finden
		try
		{
			Iterator<MNode> n = mNodes.iterator();
			while (n.hasNext())
			{
				MNode t = n.next();
				if (t.index==oldi)
					oldn = t;
				else if (t.index==newi)
					newn = t;
			}
		} finally {NodeLock.unlock();}
		if ((oldn==null)||(newn!=null))
			return; //can't change
		//Change adjacent edges
		EdgeLock.lock();
		try //Find adjacent adjes and update index
		{		Iterator<MEdge> ei = mEdges.iterator();
				while (ei.hasNext())
				{
					MEdge e = ei.next();
					if (e.EndIndex==oldi)
						e.EndIndex=newi;
					if (e.StartIndex==oldi)
						e.StartIndex=newi;
				}
		}
		finally {EdgeLock.unlock();}
		//Update Subsets
		Iterator<MSubSet> iter = mSubSets.iterator();
		while (iter.hasNext())
		{
			MSubSet actual = iter.next();
			if (actual.containsNode(oldi))
			{
				actual.removeNode(oldi);
				actual.addNode(newi);
			}
		}
		//And Change the oldnode aswell
		oldn.index=newi;
		setChanged();
		notifyObservers(new GraphMessage(GraphMessage.NODE, GraphMessage.REPLACEMENT, GraphMessage.ALL_ELEMENTS));	
	}
	/**
	 * remove a node from the graph. thereby the adjacent edges are removed too. The indices of the deleted edges
	 * are set in the return value
	 * 
	 * @param i index of the node, that should be removed
	 * @return a bitset where all edge indices are set true, that are adjacent and were deleted too
	 */
	public BitSet removeNode(int i)
	{
		MNode toDel = null;
		BitSet ergebnis = new BitSet();
		NodeLock.lock(); //Knoten finden
		try
		{
			Iterator<MNode> n = mNodes.iterator();
			while (n.hasNext())
			{
				MNode t = n.next();
				if (t.index==i)
				{
					toDel = t;
					break;
				}
			}
		} finally {NodeLock.unlock();}
		EdgeLock.lock();
		try
		{ //Adjazente Kanten finden und
			if (toDel!=null)
			{
				HashSet<MEdge> deledges = new HashSet<MEdge>();
				Iterator<MEdge> n2 = mEdges.iterator();
				while (n2.hasNext())
				{
					MEdge e = n2.next();
					if ((e.EndIndex==i)||(e.StartIndex==i))
					{
						ergebnis.set(e.index);
						deledges.add(e);
					}
					else
						ergebnis.clear(e.index);
				}
				Iterator<MEdge> n3 = deledges.iterator();
				while (n3.hasNext()) // Diese loeschen
				{
					mEdges.remove(n3.next());
				}
				NodeLock.lock();
				try
				{
					mNodes.remove(toDel); //und den Knoten loeschen
				}finally {NodeLock.unlock();}
			}
		}
		finally {EdgeLock.unlock();}
		setChanged();
		notifyObservers(new GraphMessage(GraphMessage.NODE,i,GraphMessage.REMOVAL,GraphMessage.ALL_ELEMENTS));	
		return ergebnis;
	}
	/**
	 * @return max node index +1
	 */
	public int getNextNodeIndex()
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
		} finally {
			NodeLock.unlock();
		}
		return index;
	}
	/**
	 * reurns the node name of the 
	 * @param i node with index i
	 * @return the node name as string
	 */
	public MNode getNode(int i)
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
	 * get a list of the node names in a vector, where each node name is stored at it's index
	 * every other component of the vector is null
	 * 
	 * TODO: Move to Mathematical Graph
	 * @return a Vector of all node names, 
	 */
	public Vector<String> getNodeNames() {
		Vector<String> ret = new Vector<String>();
		Iterator<MNode> n = mNodes.iterator();
		while (n.hasNext()) {
			MNode actual = n.next();
			if ((actual.index + 1) > ret.size()) {
				ret.setSize(actual.index + 1);
			}
			if (actual.index!=0) //kein temp-knoten
				ret.set(actual.index, getNode(actual.index).name);
		}
		return ret;
	}
	/**
	 * Set the node name of a node. If the node does not exist, nothing happens
	 * @param i the node with index i
	 * @param name to the new name
	 */
	public void setNodeName(int i, String name)
	{
		NodeLock.lock();
		int index=0;
		try
		{
			Iterator<MNode> n = mNodes.iterator();
			while (n.hasNext())
			{
				MNode t = n.next();
				if (t.index==i)
				{
					t.name=name;
					index = i;
				}
			}
		} finally {NodeLock.unlock();}
		if (index!=0) //found the node, notify observers
		{
			setChanged();
			notifyObservers(new GraphMessage(GraphMessage.NODE,index,GraphMessage.UPDATE,GraphMessage.NODE));	
		}
	}
	/**
	 * Check the existence of the node with index i
	 * @param index node index
	 * @return true, if a node with the index exists else false
	 */
	public boolean existsNode(int index)
	{
		NodeLock.lock();
		try{
				Iterator<MNode> n = mNodes.iterator();
				while (n.hasNext())
				{
					MNode v = n.next();
					if ( ( v.index==index) )
						return true;
				}
			} finally {NodeLock.unlock();}
		return false;
	}
	/**
	 * Returns the number of nodes contained in the graph
	 * @return 
	 */
	public int NodeCount()
	{
		return mNodes.size();
	}	
	//
	//Knoteniteration
	//
	/**
	 * Returns an Iterator to iterate the nodes
	 * @return a new iterator for the nodes
	 */
	public Iterator<MNode> getNodeIterator()
	{
			return mNodes.iterator();
	}
	/*
	 * Kantenfunktionen
	 */
	/**
	 * Add an edge with index i between s and e width value v
	 * If an edge exists between s and e, a new edge is only added if multiple edges are allowed
	 * If start and end are equal, the edge is only added if loops are allowed
	 *  
	 * If itis possible to add this edge, a copy of the parameter is added
	 * 
	 * @param e the new edge
	 * 
	 * @return true if the edge is added, else false
	 */
	public boolean addEdge(MEdge e)
	{
		if ((e.StartIndex==e.EndIndex)&&(!allowloops)) //adding tries a loop but loops are not allowed
			return false;
		if ((existsEdge(e.StartIndex, e.EndIndex)>0)&&(!allowmultiple)) //adding tries a second edge between s and e and multiple edges are not allowed
			return false;
		if (getEdge(e.index)!=null) //index already in use
			return false;
		EdgeLock.lock();
		try 
		{
			mEdges.add(new MEdge(e.index, e.StartIndex, e.EndIndex, e.Value, e.name));
			setChanged();
			notifyObservers(new GraphMessage(GraphMessage.EDGE,e.index,GraphMessage.ADDITION,GraphMessage.EDGE));	
		} 
		finally
		{
			EdgeLock.unlock();
		}
		return true;
	}
	/**
	 * Replace the an edge in the graph
	 * The index may not be changed, so the edge, that is replaced (if existent)
	 * is identfied by the index of the parameter edge given
	 * 
	 * @param edge Replacement for the edge in the graph with same index
	 */
	public void replaceEdge(MEdge edge)
	{
		EdgeLock.lock();
		try {
			Iterator<MEdge> ei = mEdges.iterator();
			while (ei.hasNext()) {
				MEdge temp = ei.next();
				if (temp.index == edge.index) // index vergeben
				{
					mEdges.remove(temp);
					mEdges.add(edge);
					setChanged();
					notifyObservers(new GraphMessage(GraphMessage.EDGE,edge.index,GraphMessage.UPDATE,GraphMessage.EDGE));	
					break;
				}
			}
		}
		finally {EdgeLock.unlock();}
	}
	/**
	 * Remove an edge from the graph.
	 * If it does not exist, nothing happens
	 * @param i edge defined by index
	 */
	public void removeEdge(int i)
	{
		MEdge toDel = null;
		Iterator<MEdge> n = mEdges.iterator();
		while (n.hasNext())
		{
			MEdge e = n.next();
			if (e.index==i)
			{
				toDel = e;
				break;
			}
		}
		if (toDel!=null)
		{
			mEdges.remove(toDel);
			setChanged();
			notifyObservers(new GraphMessage(GraphMessage.EDGE,i,GraphMessage.REMOVAL,GraphMessage.EDGE));	
		}
	}
	/**
	 * Get the Mathematical Edge with index i
	 * @param i index of edge
	 * @return the edge if an edge with this index exists, else null
	 */
	public MEdge getEdge(int i)
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
	 * get a free edge index
	 * @return max_exge_index + 1
	 */
	public int getNextEdgeIndex()
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
	public Vector<String> getEdgeNames() {
		Vector<String> ret = new Vector<String>();
		Iterator<MEdge> n = mEdges.iterator();
		while (n.hasNext()) {
			MEdge actual = n.next();
			if ((actual.index + 1) > ret.size()) {
				ret.setSize(actual.index + 1);
			}
			if ((actual.StartIndex==0)||(actual.EndIndex==0))
			{
				//temporäre Kante
			}
			else
			{
				ret.set(actual.index, actual.name);
			}
		}
		return ret;
	}
	/**
	 * Returns the number of edges between two given nodes. For the non-multiple case 0 means no edge 1 means an edge exists
	 *
	 * @param start start node index
	 * @param ende end node index
	 * @return
	 */
	public int existsEdge(int start, int ende)
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
	public Vector<Integer> getEdgeIndices(int start, int ende)
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
	public int EdgeCount()
	{
		return mEdges.size();
	}
	/**
	 * Set the name of an edge. If the edge does not exist, nothing is changed.
	 * If the name is changed a notify is pushed
	 * @param i edge given by index
	 * @param name new name of the edge
	 */
	public void setEdgeName(int i, String name)
	{
		boolean change=false;
		EdgeLock.lock();
		try
		{
			Iterator<MEdge> e = mEdges.iterator();
			while (e.hasNext())
			{
				MEdge t = e.next();
				if (t.index==i)
				{
					if (!name.equals(t.name)) //the name must be changed
					{
						t.name=name;
						change = true;
					}
				}
			}
		} finally {EdgeLock.unlock();}
		if (change)
		{
			setChanged();
			notifyObservers(new GraphMessage(GraphMessage.EDGE,i,GraphMessage.UPDATE,GraphMessage.EDGE));	
		}
	}
	/**
	 * Get the Name of an edge. If the edge does not exist, null is returned
	 * @param i edge given by index
	 * @return the name of the edge if it exists, else null
	 */
	public String getEdgeName(int i)
	{
		EdgeLock.lock();
		try
		{
			Iterator<MEdge> e = mEdges.iterator();
			while (e.hasNext())
			{
				MEdge t = e.next();
				if (t.index==i)
				{
					return t.name;
				}
			}
		} finally {EdgeLock.unlock();}
		return null;
	}

	//
	//Kanteniteration
	//
	/**
	 * Get a new Iterator for the edges. Attention: Because this stuff is threadsafe and is used in many threads the edges might change
	 */
	public Iterator<MEdge> getEdgeIterator()
	{
		return mEdges.iterator();
	}
	/*
	 * Untergraphenmethoden
	 */
	/**
	 * Add a new subset. if the index is already in use, nothing happens
	 * @param s Mathematical Subset, which should be added, a clone of the parameter is added
	 */
	public void addSubSet(MSubSet s)
	{
		if (getSubSet(s.getIndex())==null)
		{
			mSubSets.add(s.clone());
			setChanged();
			notifyObservers(new GraphMessage(GraphMessage.SUBSET,s.getIndex(),GraphMessage.ADDITION,GraphMessage.SUBSET));	

		}
		//TODO if Subsetindex already in use -> Exception ?
	}
	/**
	 * Remove a subset from the graph. If it does not exist, nothing happens.
	 * @param index subset given by id, that should be removed
	 * @return true if a subset was removed
	 */
	public boolean removeSubSet(int index)
	{
		MSubSet toDelete = null;
		Iterator<MSubSet> iter = mSubSets.iterator();
		while (iter.hasNext())
		{
			MSubSet actual = iter.next();
			if (actual.getIndex()==index)
			{
				toDelete = actual;
			}
		}
		if (toDelete!=null)
		{
			mSubSets.remove(toDelete);
			setChanged();
			notifyObservers(new GraphMessage(GraphMessage.SUBSET,index,GraphMessage.REMOVAL,GraphMessage.ALL_ELEMENTS));	
			notifyObservers("S"+index);
			return true;
		}
		return false;
	}
	/**
	 * Get the Subset specified by the index. If ist does not exists, the Method returns null
	 * @param index Inddex of the Subset
	 * @return the subset with the index, if exists, else null
	 */
	public MSubSet getSubSet(int index)
	{
		Iterator<MSubSet> iter = mSubSets.iterator();
		while (iter.hasNext())
		{
			MSubSet actual = iter.next();
			if (actual.getIndex()==index)
			{
				return actual;
			}
		}
		return null;
	}
	/**
	 * Get a free subset index
	 * @return max_subset_index + 1
	 */
	public int getNextSetIndex() {
		int index = 1;
		Iterator<MSubSet> n = mSubSets.iterator();
		while (n.hasNext()) {
			MSubSet temp = n.next();
			if (temp.getIndex() >= index) // index vergeben
			{
				index = temp.getIndex() + 1;
			}
		}
		return index;
	}
	/**
	 * Get the name of a subset. returns null if the subset does not exist
	 * @param index subset index, where the name is wanted
	 * @return the name if the subset exists, else null
	 */
	public String getSubSetName(int index)
	{
		Iterator<MSubSet> iter = mSubSets.iterator();
		while (iter.hasNext())
		{
			MSubSet actual = iter.next();
			if (actual.getIndex()==index)
			{
				return actual.getName();
			}
		}
		return null;
	}
	/**
	 * Set the name of the subset.
	 * If subset exists and the name is changed, there is a notify
	 * @param index index of the subset where the name should be set to
	 * @param newname a new name
	 */
	public void setSubSetName(int index, String newname)
	{
		boolean change = false;
		Iterator<MSubSet> iter = mSubSets.iterator();
		while (iter.hasNext())
		{
			MSubSet actual = iter.next();
			if (actual.getIndex()==index)
			{
				change = !(actual.getName().equals(newname));
				actual.setName(newname);
			}
		}
		if (change)
		{
			setChanged();
			notifyObservers(new GraphMessage(GraphMessage.SUBSET,index,GraphMessage.UPDATE,GraphMessage.SUBSET));	
		}
	}
	/**
	 * Add a Node to a Subset
	 * If both node and subset exist
	 * If the node is already in the subset, no notify is done
	 * @param nodeindex
	 * @param SetIndex
	 */
	public void addNodetoSubSet(int nodeindex, int SetIndex)
	{
		if (!existsNode(nodeindex))
			return;
		boolean change = false;
		Iterator<MSubSet> iter = mSubSets.iterator();
		while (iter.hasNext())
		{
			MSubSet actual = iter.next();
			if (actual.getIndex()==SetIndex)
			{
				change = !actual.containsNode(nodeindex); //Change if it is not in the subset yet
				actual.addNode(nodeindex);
			}
		}
		if (change)
		{
			setChanged();
			notifyObservers(new GraphMessage(GraphMessage.SUBSET,SetIndex,GraphMessage.UPDATE,GraphMessage.SUBSET|GraphMessage.NODE));	
		}
	}
	/**
	 * Removes a node from a subset. If there was a change in the subset (both node an subset exist and the node was in the subset) the return value is true, else false
	 * @param nodeindex node that should be removed
	 * @param SetIndex index of subset where the node should be removed
	 */
	public boolean removeNodefromSet(int nodeindex, int SetIndex)
	{
		boolean change = false;
		if (!existsNode(nodeindex))
			return false;
		Iterator<MSubSet> iter = mSubSets.iterator();
		while (iter.hasNext())
		{
			MSubSet actual = iter.next();
			if (actual.getIndex()==SetIndex)
			{
				change = actual.containsNode(nodeindex);
				actual.removeNode(nodeindex);
			}
		}
		if (change)
		{
			setChanged();
			notifyObservers(new GraphMessage(GraphMessage.SUBSET,SetIndex,GraphMessage.UPDATE,GraphMessage.SUBSET|GraphMessage.NODE));	
		}
		return change;
	}
	/**
	 * If both node and subset exist, there is a check whether the node is contained in the set
	 * @param nodeindex 
	 * @param SetIndex
	 * @return true if node and subset exist and the node is in the subset
	 */
	public boolean SubSetcontainsNode(int nodeindex,int SetIndex)
	{
		if (!existsNode(nodeindex))
			return false;
		Iterator<MSubSet> iter = mSubSets.iterator();
		while (iter.hasNext())
		{
			MSubSet actual = iter.next();
			if (actual.getIndex()==SetIndex)
			{
				return actual.containsNode(nodeindex);
			}
		}
		return false;
	}
	/**
	 * Add an edge to a subset, if both edge and subset exist. If they don't nothing happens
	 * @param edgeindex edge index that should be added
	 * @param SetIndex subset index where the edge should be added
	 */
	public void addEdgetoSubSet(int edgeindex, int SetIndex)
	{
		if (getEdge(edgeindex)==null)
			return;
		Iterator<MSubSet> iter = mSubSets.iterator();
		while (iter.hasNext())
		{
			MSubSet actual = iter.next();
			if (actual.getIndex()==SetIndex)
			{
				actual.addEdge(edgeindex);
			}
		}
		setChanged();
		notifyObservers(new GraphMessage(GraphMessage.SUBSET,SetIndex,GraphMessage.UPDATE,GraphMessage.SUBSET|GraphMessage.EDGE));	
	}
	/**
	 * Removes an edge from a subset, if both exist. If a change is done (edge is also contained in the subset). 
	 * If an edge is removed, so there was really a change, it returs true
	 *
	 * @param edgeindex Edge to be removed from
	 * @param SetIndex subset with this index
	 *
	 * @return true if both edge and subset exist and the edge was in the subset, so it was removed
	 */
	public boolean removeEdgefromSet(int edgeindex, int SetIndex)
	{
		boolean change = false;
		if (getEdge(edgeindex)==null)
			return false;
		Iterator<MSubSet> iter = mSubSets.iterator();
		while (iter.hasNext())
		{
			MSubSet actual = iter.next();
			if (actual.getIndex()==SetIndex)
			{
				change = actual.containsEdge(edgeindex);
				actual.removeEdge(edgeindex);
			}
		}
		if (change)
		{
			setChanged();
			notifyObservers(new GraphMessage(GraphMessage.SUBSET,SetIndex,GraphMessage.UPDATE,GraphMessage.SUBSET|GraphMessage.EDGE));	
		}
		return change;
	}
	/**
	 * Indicator whether an edge belongs to a subset or not. If the edge does not exist, false is returned
	 * @param edgeindex index of the edge that should be proved to be in a 
	 * @param SetIndex subset with this index
	 * @return true if edge and subset exist and the edge is in the subset
	 */
	public boolean SubSetcontainsEdge(int edgeindex,int SetIndex)
	{
		if (getEdge(edgeindex)==null)
			return false;
		Iterator<MSubSet> iter = mSubSets.iterator();
		while (iter.hasNext())
		{
			MSubSet actual = iter.next();
			if (actual.getIndex()==SetIndex)
			{
				return actual.containsEdge(edgeindex);
			}
		}
		return false;
	}
	/**
	 * get a new Subset Iterator.
	 * @return
	 */
	public Iterator<MSubSet> getSubSetIterator()
	{
		return mSubSets.iterator();
	}
	/**
	 * Get the number of subsets in the mgraph
	 * @return the number of subsets in the mgraph
	 */
	public int SubSetCount() {
		return mSubSets.size();
	}
	/**
	 * get a list of the subset names in a vector, where each subset name is stored at it's index
	 * every other component of the vector is null
	 * @return a Vector of all subset names, 
	 */	
	public Vector<String> getSetNames() {
		Vector<String> ret = new Vector<String>();
		Iterator<MSubSet> s = mSubSets.iterator();
		while (s.hasNext()) {
			MSubSet actual = s.next();
			if ((actual.getIndex() + 1) > ret.size()) {
				ret.setSize(actual.getIndex() + 1);
			}
			ret.set(actual.getIndex(), getSubSetName(actual.getIndex()));
		}
		return ret;
	}
}

