package history;

import java.util.Iterator;

import model.*;
import model.Messages.GraphConstraints;

public abstract class CommonGraphAction {

	protected Object ActionObject;
	protected int Objecttype;
	protected int Action;
	protected MNode mn;
	protected MSubgraph ms;
	protected VGraphInterface env;

	public CommonGraphAction()
	{}
	/**
	 * Create New Action inducted by a Node
	 * @param o the node
	 * @param action the action
	 * @param environment Graph containing the Subgraphs the node belongs to and (at least) andjacent edges and their second nodes
	 * @throws GraphActionException
	 */
	public CommonGraphAction(VNode o, int action, VGraphInterface environment) throws GraphActionException
	{
		if ((o==null)||(environment==null))
			throw new GraphActionException("Could not Create Action: Node and environment must not be null.");
		if ((environment.getType()==VGraphInterface.GRAPH)&&(((VGraph)environment).modifyNodes.get(o.getIndex())==null))
			throw new GraphActionException("Could not Create Action: Environment must contains at least the node itself.");			
		if ((environment.getType()==VGraphInterface.HYPERGRAPH)&&(((VHyperGraph)environment).modifyNodes.get(o.getIndex())==null))
			throw new GraphActionException("Could not Create Action: Environment must contains at least the node itself.");			
		if (environment.getType()==VGraphInterface.GRAPH)
			mn = ((VGraph)environment).getMathGraph().modifyNodes.get(o.getIndex()).clone();
		else if (environment.getType()==VGraphInterface.HYPERGRAPH)
			mn = ((VHyperGraph)environment).getMathGraph().modifyNodes.get(o.getIndex()).clone();
		else //Unknown Type
			throw new GraphActionException("Could not Create Action: Environmental Graphtype unknown.");					
		ActionObject = o;
		Action=action;
		env = environment;
		Objecttype=GraphConstraints.NODE;
	}
	/**
	 * Create a New Action induced by a Subgraph
	 * @param o VSubgraph manipulated
	 * @param action what was done?
	 * @param c Color.
	 * @throws GraphActionException
	 */
	public CommonGraphAction(VSubgraph o, int action, MSubgraph m) throws GraphActionException
	{
		if ((o==null)||(m==null))
			throw new GraphActionException("Could not Create Action: Subgraph must not be null.");
		ActionObject = o.clone();
		ms = m.clone();
		Action=action;
		Objecttype=GraphConstraints.SUBGRAPH;
	}

	/**
	 * Exchange the Memberships of a node or edge to Subgraphs between two graphs
	 * The subgraphs of the first graph are iterated (its assumed they have the same subgraphs)
	 * and for every subgraph the membership of the item is exchanged.
	 * 
	 * @param ItemType NODE or EDGE
	 * @param itemindex index of the node or edge
	 * @param first first graph, where the subgraphs are iterated
	 * @param second second graph
	 * @throws GraphActionException
	 */
	protected void exchangeSubgraphMembership(int ItemType, int itemindex, VGraphInterface first, VGraphInterface second) throws GraphActionException
	{
		if (first.getType()!=second.getType())
			return;
		VSubgraphSet firstSet, secondSet;
		MSubgraphSet firstMSet, secondMSet;
		if (first.getType()==VGraphInterface.GRAPH)
		{
			firstSet = ((VGraph)first).modifySubgraphs; firstMSet = ((VGraph)first).getMathGraph().modifySubgraphs;
			secondSet = ((VGraph)second).modifySubgraphs; secondMSet = ((VGraph)second).getMathGraph().modifySubgraphs;
			if (ItemType == GraphConstraints.HYPEREDGE)
				return;
		}
		else if (first.getType()==VGraphInterface.HYPERGRAPH)
		{
			firstSet = ((VHyperGraph)first).modifySubgraphs; firstMSet = ((VHyperGraph)first).getMathGraph().modifySubgraphs;
			secondSet = ((VHyperGraph)second).modifySubgraphs; secondMSet = ((VHyperGraph)second).getMathGraph().modifySubgraphs;			
			if (ItemType == GraphConstraints.EDGE)
				return;
		}
		else
			throw new GraphActionException("Unknown Graphtype for Subgraph Exchange");
		Iterator<VSubgraph> si = firstSet.getIterator();
		while (si.hasNext())
		{
			VSubgraph s = si.next();
			if (ItemType==GraphConstraints.NODE)
			{
				boolean wasfirst = firstMSet.get(s.getIndex()).containsNode(itemindex);
				boolean wassecond = secondMSet.get(s.getIndex()).containsNode(itemindex);
				if (wasfirst)
					secondSet.addNodetoSubgraph(itemindex, s.getIndex());
				else
					secondSet.removeNodefromSubgraph(itemindex, s.getIndex());
				if (wassecond)
					firstSet.addNodetoSubgraph(itemindex, s.getIndex());
				else
					firstSet.removeNodefromSubgraph(itemindex, s.getIndex());
			}
			else if ((ItemType==GraphConstraints.EDGE)||(ItemType==GraphConstraints.HYPEREDGE))
			{
				boolean wasfirst = firstMSet.get(s.getIndex()).containsEdge(itemindex);
				boolean wassecond = secondMSet.get(s.getIndex()).containsEdge(itemindex);
				if (wasfirst)
					secondSet.addEdgetoSubgraph(itemindex, s.getIndex());
				else
					secondSet.removeEdgefromSubgraph(itemindex, s.getIndex());
				if (wassecond)
					firstSet.addEdgetoSubgraph(itemindex, s.getIndex());
				else
					firstSet.removeEdgefromSubgraph(itemindex, s.getIndex());
			}
			else
				throw new GraphActionException("Unknown ItemType "+ItemType);
		}
	}

	/**
	 * Exchange  Selection between first and second Graph
	 * The first graph might be smaller (e.g. might be environment of a node), so that one is iterated
	 * 
	 * This method only handles node-selections, for further exchanges use subclasses
	 * 
	 * @param first
	 * @param second
	 * @throws GraphActionException 
	 */
	protected void exchangeSelection(VGraphInterface first, VGraphInterface second)
	{
		VNodeSet firstNodes, secondNodes;
		if (first.getType()==VGraphInterface.GRAPH)
		{
			firstNodes = ((VGraph)first).modifyNodes;
			secondNodes = ((VGraph)second).modifyNodes;
		}
		else if (first.getType()==VGraphInterface.HYPERGRAPH)
		{
			firstNodes = ((VHyperGraph)first).modifyNodes;
			secondNodes = ((VHyperGraph)second).modifyNodes;
		}
		else
			return;
		
		Iterator<VNode> ni = firstNodes.getIterator();
		while (ni.hasNext())
		{
			VNode n = ni.next();
			int sel = n.getSelectedStatus();
			VNode n2 = secondNodes.get(n.getIndex());
			if (n2!=null) //if its not null
			{ 
				n.setSelectedStatus(n2.getSelectedStatus());
				n2.setSelectedStatus(sel);
			}
		}
	}

	/**
	 * Recreate the Subgraphs the edge belonged to, depending on the actual environment
	 * 
	 * @param e Edge 
	 * @param g Graph the edge should be recreated in and the colors should be restored in
	 */
	protected void recreateItemColor(VItem i, VGraphInterface g) throws GraphActionException
	{
		if ((i.getType()==VItem.HYPEREDGE)&&(g.getType()!=VGraphInterface.HYPERGRAPH))
			throw new GraphActionException("Can't recreate Hyperedge color in a non-Hypergraph");
		if ((i.getType()==VItem.EDGE)&&(g.getType()!=VGraphInterface.GRAPH))
			throw new GraphActionException("Can't recreate edge color in a non-simple graph");

		Iterator<VSubgraph> si;
		MSubgraphSet envsubs;
		VSubgraphSet vsubs;
		if (g.getType()==VGraphInterface.GRAPH)
		{
			si = ((VGraph)g).modifySubgraphs.getIterator();
			vsubs = ((VGraph)g).modifySubgraphs;
			envsubs = ((VGraph)env).getMathGraph().modifySubgraphs;
		}
		else if (g.getType()==VGraphInterface.HYPERGRAPH)
		{
			si = ((VHyperGraph)g).modifySubgraphs.getIterator();
			vsubs = ((VHyperGraph)g).modifySubgraphs;
			envsubs = ((VHyperGraph)env).getMathGraph().modifySubgraphs;
		}
		else
			throw new GraphActionException("No valid Graphtype given for Color recreation");
		
		while (si.hasNext())
		{
			VSubgraph actual = si.next();
			if ((i.getType()==VItem.EDGE)||(i.getType()==VItem.HYPEREDGE))
			{
				if (envsubs.get(actual.getIndex()).containsEdge(i.getIndex()))
				{
					if (vsubs.get(actual.getIndex())==null)
						throw new GraphActionException("Can't replace edge, replacements belongs to Subgraphs, that don't exists in given parameter graph");
					vsubs.addEdgetoSubgraph(i.getIndex(), actual.getIndex());
//					System.err.println("Adding e "+i.getIndex()+" to Subgraph "+actual.getIndex());
				}
			}
			else if (i.getType()==VItem.NODE)
			{
				if (envsubs.get(actual.getIndex()).containsNode(i.getIndex()))
				{
					if (vsubs.get(actual.getIndex())==null)
						throw new GraphActionException("Can't replace edge, replacements belongs to Subgraphs, that don't exists in given parameter graph");
					vsubs.addNodetoSubgraph(i.getIndex(), actual.getIndex());
//					System.err.println("Adding n "+i.getIndex()+" to Subgraph "+actual.getIndex());
				}				
			}
			else
				throw new GraphActionException("Unknown ItemType for Color recreation.");
		}
	}
	
	
	/**
	 * Return Type of Action.
	 * @return
	 */
	public int getActionType() {
		return Action;
	}

	/**
	 * Replace this object in or with a Graph/Hypergraph (Depending on type of corresponding action).
	 * 
	 * The replaced element is stored in the action part, so that another replace restores the first situation.
	 * 
	 */
	protected abstract VGraphInterface doReplace(VGraphInterface graph) throws GraphActionException;

	/**
	 * Perform a Create - after that the message is directly without manipulation its own undo
	 * @param graph
	 * @return
	 * @throws GraphActionException
	 */
	protected abstract VGraphInterface doCreate(VGraphInterface graph) throws GraphActionException;
	
	/**
	 * Perform a Delete
	 * The Action itself is not manipulated, because it is its own undo
	 * @param graph
	 * @return
	 * @throws GraphActionException
	 */
	protected abstract VGraphInterface doDelete(VGraphInterface graph) throws GraphActionException;

	/**
	 * Apply this Action to a Graph. The graph given as argument is manipulated directly, though returned, too.
	 * The action itself is transofrmed to be its own undo-Message
	 * @param graph the graph to be manipulated
	 * @return the manipulated graph
	 */
	public VGraphInterface redoAction(VGraphInterface graph) throws GraphActionException
	{
		if (ActionObject==null)
			throw new GraphActionException("No Object available for the Action");
		VGraphInterface ret;
		switch(Action&GraphConstraints.ACTIONMASK) //Direct actions 
		{
			case GraphConstraints.UPDATE: 
				ret = doReplace(graph);
				break;
			case GraphConstraints.ADDITION:
				ret= doCreate(graph);
				break;
			case GraphConstraints.REMOVAL:
				ret = doDelete(graph);
				break;
			default: throw new GraphActionException("No Action given.");
		}
		if ((Objecttype==GraphConstraints.GRAPH)||(Objecttype==GraphConstraints.HYPERGRAPH)||(Objecttype==GraphConstraints.SELECTION)) //Move Selection from old graph to new one
			exchangeSelection((VGraphInterface)ActionObject,graph);
		else if (Objecttype!=GraphConstraints.SUBGRAPH) //Subgraph has no env, so if no subgraph, update selection
			exchangeSelection(env,graph);
		return ret;
	}

	/**
	 * Apply the action to the Clone of a Graph. The given Parameter Graph is not manipulated but cloned
	 * The Clone then gets manipulated and is returned
	 * @param g A Graph
	 * @return The Manipulated Clone
	 */
	public VGraphInterface redoActionOnClone(VGraphInterface g) throws GraphActionException
	{
		if (g.getType()==VGraphInterface.GRAPH)
			return redoAction(((VGraph)g).clone());
		else if (g.getType()==VGraphInterface.HYPERGRAPH)
			return redoAction(((VHyperGraph)g).clone());
		else
			throw new GraphActionException("Unknown Graph Type");
	}

	/**
	 * Undo The Action on a given Graph:
	 * - Created Elements are Deleted
	 * - Deleted Elements are Created again
	 * - Replaced Elements are Rereplaced again (because replace is its own undo)
	 * @param graph Graph the undo is performed in
	 * @return the same graph as the parameter, only the action is undone.
	 * @throws GraphActionException
	 */
	public VGraphInterface UnDoAction(VGraphInterface graph) throws GraphActionException
	{
		if (ActionObject==null)
			throw new GraphActionException("No Object available for the Action");
		VGraphInterface ret;
		switch(Action&GraphConstraints.ACTIONMASK) //Direct actions 
		{
			case GraphConstraints.UPDATE:  //Undo a replace is repace itself
				ret = doReplace(graph);
				break;
			case GraphConstraints.ADDITION: //Undo a Create is a delete
				ret = doDelete(graph);
				break;
			case GraphConstraints.REMOVAL: //Undo Delete is Create
				ret = doCreate(graph);
				break;
			default:
				throw new GraphActionException("No Action given");
		}
		if ((Objecttype==GraphConstraints.GRAPH)||(Objecttype==GraphConstraints.HYPERGRAPH)||(Objecttype==GraphConstraints.SELECTION)) //Move Selection from old graph to new one
			exchangeSelection((VGraphInterface)ActionObject,ret);			
		else if (Objecttype!=GraphConstraints.SUBGRAPH) //Subgraph has no env, so if no subgraph, update selection
			exchangeSelection(env,ret);
		return ret;
	}

	/**
	 * Undo The Action on the copy of the given graph
	 * does the same as UndoAction, but on a clone, so the parameter Graph given is unchanged
	 * @param g
	 * @return
	 * @throws GraphActionException
	 */
	public VGraphInterface UnDoActionOnClone(VGraphInterface g) throws GraphActionException
	{
		if (g.getType()==VGraphInterface.GRAPH)
			return UnDoAction(((VGraph)g).clone());
		else if (g.getType()==VGraphInterface.HYPERGRAPH)
			return UnDoAction(((VHyperGraph)g).clone());
		else
			throw new GraphActionException("Unknown Graph Type");
	}
}