package model;

import java.util.BitSet;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Observer;
import java.util.Vector;
import java.util.Observable;

import model.Messages.GraphConstraints;
import model.Messages.GraphMessage;
import model.Messages.MGraphMessage;
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

public class MGraph extends Observable implements Observer, MGraphInterface
{	
	public MNodeSet modifyNodes;
	public MEdgeSet modifyEdges;
	public MSubgraphSet modifySubgraphs;
	boolean directed;
	boolean allowloops;
	boolean allowmultiple;
	/**
	 * Create a new Graph where
	 * @param d indicates whether edges are directed (true) or not (false
	 * @param l indicates whether loops are allowed or not
	 * @param m indicates whether multiple edges between two nodes are allowed
	 */
	public MGraph(boolean d, boolean l, boolean m)
	{
		modifyNodes = new MNodeSet();
		modifyEdges = new MEdgeSet(d,l,m);
		modifySubgraphs = new MSubgraphSet();
		modifyNodes.addObserver(this); //to advance global change Messages
		modifyNodes.addObserver(modifySubgraphs); //to react on Node Deletions
		modifyNodes.addObserver(modifyEdges); //to react on Node Deletions
		
		modifyEdges.addObserver(modifySubgraphs); //to react on Edge Deletions
		modifyEdges.addObserver(this); //to advance global change Messages
		
		modifySubgraphs.addObserver(this); //to advance change Messages
		addObserver(modifyEdges); //for changes of the Booleans
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
		Iterator<MSubgraph> n1 = modifySubgraphs.getIterator();
		while (n1.hasNext())
			clone.modifySubgraphs.add(n1.next().clone()); //Jedes Set kopieren
		//Knoten
		Iterator<MNode> n2 = modifyNodes.getIterator();
		while (n2.hasNext())
		{
			MNode actualNode = n2.next();
			MNode Nodeclone = new MNode(actualNode.index, actualNode.name);
			clone.modifyNodes.add(Nodeclone);
			//In alle Sets einfuegen
			n1 = modifySubgraphs.getIterator();
			while (n1.hasNext())
			{
				MSubgraph actualSet = n1.next();
				if (actualSet.containsNode(actualNode.index))
					clone.modifySubgraphs.addNodetoSubgraph(actualNode.index, actualSet.getIndex()); //Jedes Set kopieren
			}
		}
		//Analog Kanten
		Iterator<MEdge> n3 = modifyEdges.getIterator();
		while (n3.hasNext())
		{
			MEdge actualEdge = n3.next();
			MEdge cEdge = new MEdge(actualEdge.index, actualEdge.StartIndex, actualEdge.EndIndex, actualEdge.Value, actualEdge.name);
			clone.modifyEdges.add(cEdge);
			//In alle Sets einfuegen
			n1 = modifySubgraphs.getIterator();
			while (n1.hasNext())
			{
				MSubgraph actualSet = n1.next();
				if (actualSet.containsEdge(actualEdge.index))
					clone.modifySubgraphs.addEdgetoSubgraph(actualEdge.index, actualSet.getIndex()); //Jedes Set kopieren
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
		int endstatus=0;
		if (d==directed)
			return removed; //nicht geändert
		//Auf gerihctet umstellen ist kein Problem. 
		if ((!d)&&(!allowmultiple)) //if multiple edges are allowed we don't need to delete them
									//auf ungerichtet umstellen, existieren Kanten i->j und j->i so lösche eine
		{
			setChanged();
			notifyObservers(new GraphMessage(GraphConstraints.EDGE|GraphConstraints.DIRECTION,GraphConstraints.UPDATE|GraphConstraints.BLOCK_START,GraphConstraints.EDGE));
			endstatus=GraphConstraints.BLOCK_END;
			Iterator<MNode> n = modifyNodes.getIterator();				
			while (n.hasNext())
			{
				MNode t = n.next();
				Iterator<MNode> n2 = modifyNodes.getIterator();
				while (n2.hasNext())
				{
					MNode t2 = n2.next();
					if (t.index <= t2.index)
					{
						Vector<Integer> ttot2 = modifyEdges.indicesBetween(t.index, t2.index);
						Vector<Integer> t2tot = modifyEdges.indicesBetween(t2.index, t.index);
						//In the nonmultiple case each Vector has exactely one or no edge in it
						if ((!ttot2.isEmpty())&&(!t2tot.isEmpty()))
						{
							int e1 = ttot2.firstElement();
							int e2 = t2tot.firstElement();
							MEdge m = modifyEdges.get(e2);
							m.Value = modifyEdges.get(e2).Value+modifyEdges.get(e1).Value;
							modifyEdges.remove(e1);
							removed.set(e1);
						}
					}
				}
			}
		}
		directed = d;
		setChanged();
		notifyObservers( new MGraphMessage(GraphConstraints.DIRECTION,d));
		setChanged();
		notifyObservers(new GraphMessage(GraphConstraints.EDGE|GraphConstraints.DIRECTION,GraphConstraints.UPDATE|endstatus,GraphConstraints.EDGE));
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
		int endstatus=0; //Block end or not ?
		if ((allowloops)&&(!a)) //disbabling
		{
			setChanged();
			notifyObservers(new GraphMessage(GraphConstraints.LOOPS,GraphConstraints.UPDATE|GraphConstraints.BLOCK_START,GraphConstraints.EDGE));	
			endstatus=GraphConstraints.BLOCK_END;
			HashSet<MEdge> deledges = new HashSet<MEdge>();
			Iterator<MEdge> n2 = modifyEdges.getIterator();
			while (n2.hasNext())
			{
					MEdge e = n2.next();
					removed.set(e.index, e.EndIndex==e.StartIndex); //Set if Loop, clear else
					if (removed.get(e.index)) //was is just set?
						deledges.add(e);
			}
			Iterator<MEdge> n3 = deledges.iterator();
			while (n3.hasNext()) // Diese loeschen
					modifyEdges.remove(n3.next().index);
		}	
		this.allowloops = a;
		setChanged();
		notifyObservers( new MGraphMessage(GraphConstraints.LOOPS,a));
		setChanged();
		notifyObservers(new GraphMessage(GraphConstraints.LOOPS,GraphConstraints.UPDATE|endstatus,GraphConstraints.EDGE));	
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
		int endstatus=0; //No End_BlockMessage
		if ((allowmultiple)&&(!a)) //Changed from allowed to not allowed, so remove all multiple
		{	
			setChanged();
			notifyObservers(new GraphMessage(GraphConstraints.MULTIPLE,GraphConstraints.UPDATE|GraphConstraints.BLOCK_START,GraphConstraints.EDGE));	
			endstatus=GraphConstraints.BLOCK_END;
			Iterator<MNode> n = modifyNodes.getIterator();				
			while (n.hasNext())
			{
				MNode t = n.next();
				Iterator<MNode> n2 = modifyNodes.getIterator();
				while (n2.hasNext())
				{
					MNode t2 = n2.next();
					//if the graph is directed
					if (((!directed)&&(t2.index<=t.index))||(directed)) //in the nondirected case only half the cases
					{
						if (modifyEdges.cardinalityBetween(t.index, t2.index)>1) //we have to delete
						{
							Vector<Integer> multipleedges = modifyEdges.indicesBetween(t.index, t2.index);
							int value = modifyEdges.get(multipleedges.firstElement()).Value;
							//Add up the values and remove the edges from the second to the last
							Iterator<Integer> iter = multipleedges.iterator();
							iter.next();
							while(iter.hasNext())
							{
									int nextindex = iter.next();
									value += modifyEdges.get(nextindex).Value;
									modifyEdges.remove(nextindex);
									removed.set(nextindex);
							}
							modifyEdges.get(multipleedges.firstElement()).Value = value;
						}
					}					
				}
			}
		}
		this.allowmultiple = a;
		setChanged();
		notifyObservers( new MGraphMessage(GraphConstraints.MULTIPLE,a));
		setChanged();
		notifyObservers(new GraphMessage(GraphConstraints.MULTIPLE,GraphConstraints.UPDATE|endstatus,GraphConstraints.EDGE));	
		return removed;
	}
	public int getType()
	{
		return MGraphInterface.GRAPH;
	}
	public void update(Observable o, Object arg) {
		if (arg instanceof GraphMessage) //Send graphmessages to external listeners
			pushNotify((GraphMessage)arg);
	}
}