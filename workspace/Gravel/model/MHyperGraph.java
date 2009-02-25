package model;

import java.util.Iterator;
import java.util.Observer;
import java.util.Observable;

import model.Messages.GraphMessage;
/**
 * MHyperGraph
 * 
 * The pure mathematical hypergraph
 * 
 * every node is represented by an MNode
 * every hyperedge is represened by its index, a start and an endnode, a value and its name
 * 
 * every subgraph contains its index, a name and the information which nodes and hyperegdes are in this subgraph
 *   
 * @author Ronny Bergmann
 * @since 0.4
 */

public class MHyperGraph extends Observable implements Observer, MGraphInterface
{	
	public MNodeSet modifyNodes;
	public MHyperEdgeSet modifyHyperEdges;
	public MSubgraphSet modifySubgraphs;
	/**
	 * Create a new Graph where
	 * @param d indicates whether edges are directed (true) or not (false
	 * @param l indicates whether loops are allowed or not
	 * @param m indicates whether multiple edges between two nodes are allowed
	 */
	public MHyperGraph()
	{
		modifyNodes = new MNodeSet();
		modifyHyperEdges = new MHyperEdgeSet();
		modifySubgraphs = new MSubgraphSet();
		modifyNodes.addObserver(this); //to advance global change Messages
		modifyNodes.addObserver(modifySubgraphs); //to react on Node Deletions
		modifyNodes.addObserver(modifyHyperEdges); //to react on Node Deletions
		
		modifyHyperEdges.addObserver(modifySubgraphs); //to react on Edge Deletions
		modifyHyperEdges.addObserver(this); //to advance global change Messages
		
		modifySubgraphs.addObserver(this); //to advance change Messages
		addObserver(modifyHyperEdges); //for changes of the Booleans
	}
	/**
	 * clone this graph and 
	 * @return the copy
	 */
	public MHyperGraph clone()
	{
		MHyperGraph clone = new MHyperGraph();
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
		Iterator<MHyperEdge> n3 = modifyHyperEdges.getIterator();
		while (n3.hasNext())
		{
			MHyperEdge actualEdge = n3.next();
			MHyperEdge cEdge = actualEdge.clone();
			clone.modifyHyperEdges.add(cEdge);
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
	public int getType()
	{
		return MGraphInterface.HYPERGRAPH;
	}
	public void update(Observable o, Object arg) {
		if (arg instanceof GraphMessage) //Send graphmessages to external listeners
			pushNotify((GraphMessage)arg);
	}
}