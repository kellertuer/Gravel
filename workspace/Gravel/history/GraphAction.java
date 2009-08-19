package history;

import java.util.Iterator;

import model.*;
import model.Messages.GraphConstraints;
import model.Messages.GraphMessage;
/**
 * GraphAction represents one single action that can be performed to manipulate a VGraph.
 * Besides a standard action, that replaces a graph with a new one (because 
 * there were many changes), there are specific Actions to reduce memory and computation usage
 * 
 * The given Values are Actions that happened to the Graph:
 * - Replace, where the change is given in a new graph, node, edge or subgraph that is replaced
 * - create, where an Object is created in the graph (a node, edge or subgraph)
 * - delete, where an Object is deleted
 * 
 *  The Action specified is the action DONE to the graph.
 *  
 *  If no specific action fits, it is recommended to use REPLACE with a complete graph, this suits e.g.
 *  for the manipulation of a node, where the ID changes.
 *  
 * @author Ronny Bergmann
 * @since 0.3
 */
public class GraphAction extends CommonGraphAction {
	
	private MEdge me;
	private VGraph envG;
	/**
	 * Create a New Action with whole Graph or an Selection Change
	 * Only updates are possible, because Graphs can't be created or deleted while editing a graph
	 * Selection is also simplified to updates, because the deselect-function does the same as the exChangeUpdate here
	 * (in their computational comparison)
	 *  
	 * @param o VGraph
	 * @param action Action that happened
	 * @param boolean Itemchange Indicator for the change happened: True if an Node/Edge/Subgraph was changed, false if only selection was changed
	 * @throws GraphActionException E.g. a Graph can not be Created or Deleted within a Graph
	 */
	public GraphAction(VGraph o, int action, boolean Itemchange) throws GraphActionException
	{
		if (o==null)
			throw new GraphActionException("Could not Create Action: Graph must not be null.");
		ActionObject=o.clone();
		Action=action;
		if ((action&(GraphConstraints.ADDITION|GraphConstraints.REMOVAL))>0) //Create or delete is active
		{
			ActionObject=null;
			Action=0;
			throw new GraphActionException("Creating or Deletion Graph/Selection is not possible as an Trackable Action.");
		}
		if (Itemchange)
			Objecttype=GraphConstraints.GRAPH;
		else
			Objecttype=GraphConstraints.SELECTION;
	}
	/**
	 * Create an Action for Manipulation of an Edge
	 * @param o The Visual Information of the Edge
	 * @param action Action Happening to it
	 * @param environment VGraph containins at least the Start- and Endnode and the Subgraphs the Edge belongs to
	 * @throws GraphActionException
	 */
	public GraphAction(VEdge o, int action, VGraph environment) throws GraphActionException
	{
		if ((o==null)||(environment==null))
			throw new GraphActionException("Could not Create Action: Edge and Environment must not be null.");
		if (environment.modifyEdges.get(o.getIndex())==null)
			throw new GraphActionException("Could not Create Action: Environment must contain edge");
		ActionObject=o;
		Action=action;
		me = environment.getMathGraph().modifyEdges.get(o.getIndex());
		env = environment;
		envG = environment;
		Objecttype=GraphConstraints.EDGE;
	}

	/**
	 * Create New Action inducted by a Node
	 * @param o the node
	 * @param action the action
	 * @param environment Graph containing the Subgraphs the node belongs to and (at least) andjacent edges and their second nodes
	 * @throws GraphActionException
	 */
	public GraphAction(VNode o, int action, VGraph environment) throws GraphActionException
	{
		super(o,action,environment);
		envG = environment;
	}

	/**
	 * Create a New Action induced by a Subgraph
	 * @param o VSubgraph manipulated
	 * @param action what was done?
	 * @param c Color.
	 * @throws GraphActionException
	 */
	public GraphAction(VSubgraph o, int action, MSubgraph m) throws GraphActionException
	{
		super(o,action,m);
	}
	
	protected void exchangeSelection(VGraphInterface first, VGraphInterface second)
	{
		if ((first.getType()!=VGraphInterface.GRAPH)||(first.getType()!=second.getType()))
				return;
		super.exchangeSelection(first, second);
		Iterator<VEdge> ei = ((VGraph)first).modifyEdges.getIterator();
		while (ei.hasNext())
		{
			VEdge e = ei.next();
			int sel = e.getSelectedStatus();
			VEdge e2 = ((VGraph)second).modifyEdges.get(e.getIndex());
			if (e2!=null)
			{
				e.setSelectedStatus(e2.getSelectedStatus());
				e2.setSelectedStatus(sel);
			}
		}
	}

	protected VGraphInterface doReplace(VGraphInterface graph) throws GraphActionException
	{
		if (graph.getType()!=VGraphInterface.GRAPH)
			throw new GraphActionException("Can't doDelete(): Wrong Type of Graph");
		VGraph g = (VGraph)graph;
		VGraph ret;
		int id=-1;
		switch(Objecttype)
		{
			case GraphConstraints.SELECTION:
				ret = g;
			break;
			case GraphConstraints.GRAPH: //Replace whole graph and save the actual parameter als old object
				ret = new VGraph(((VGraph)ActionObject).getMathGraph().isDirected(), ((VGraph)ActionObject).getMathGraph().isLoopAllowed(), ((VGraph)ActionObject).getMathGraph().isMultipleAllowed());
				ret.replace((VGraph)ActionObject);
				((VGraph)ActionObject).replace(g);
				g.replace(ret);
			break;
			case GraphConstraints.NODE: //Replace Node and keep the given one in the graph as old one
				VNode n = (VNode)ActionObject;
				if (g.modifyNodes.get(n.getIndex())==null) //node does not exists
					throw new GraphActionException("Can't replace node, none there.");
				ActionObject = g.modifyNodes.get(n.getIndex()).clone(); //save old node
				//Save Color of old node
				Iterator<VSubgraph> si = g.modifySubgraphs.getIterator();
				while (si.hasNext())
				{
					VSubgraph s = si.next();
					if (g.getMathGraph().modifySubgraphs.get(s.getIndex()).containsNode(n.getIndex()))
						((VNode)ActionObject).addColor(s.getColor());
				}
				MNode tempmn = mn;
				mn = new MNode(n.getIndex(), g.getMathGraph().modifyNodes.get(n.getIndex()).name);
				g.modifyNodes.replace(n, tempmn);
				envG.modifyNodes.replace((VNode)ActionObject, mn);
				id = n.getIndex();
				exchangeSubgraphMembership(GraphConstraints.NODE,n.getIndex(),env,graph);
				ret = g; //return graph
			break;
			case GraphConstraints.EDGE: //Replace Edge in graph and keep the replaced one as old one 
				VEdge e = (VEdge)ActionObject;
				if (g.modifyEdges.get(e.getIndex())==null) //edge does not exists
					throw new GraphActionException("Can't replace edge, none there.");
				ActionObject = g.modifyEdges.get(e.getIndex()).clone(); //save old edge
				//Save Color of old edge
				Iterator<VSubgraph> esi = g.modifySubgraphs.getIterator();
				while (esi.hasNext())
				{
					VSubgraph s = esi.next();
					if (g.getMathGraph().modifySubgraphs.get(s.getIndex()).containsEdge(e.getIndex()))
						((VEdge)ActionObject).addColor(s.getColor());
				}
				MEdge tempme = new MEdge(me.index, me.StartIndex, me.EndIndex, me.Value, me.name);
				MEdge me = g.getMathGraph().modifyEdges.get(e.getIndex());
				g.modifyEdges.replace(e, tempme);
				envG.modifyEdges.replace((VEdge)ActionObject, me);
				id = e.getIndex();
				exchangeSubgraphMembership(GraphConstraints.EDGE,e.getIndex(),env,graph);
				ret = g;
			break;
			case GraphConstraints.SUBGRAPH:
				VSubgraph newSubgraph = (VSubgraph)ActionObject;
				if (g.modifySubgraphs.get(newSubgraph.getIndex())==null) //edge does not exists
					throw new GraphActionException("Can't replace subgraph, none there.");
				ActionObject = g.modifySubgraphs.get(newSubgraph.getIndex()); //Save old one in action
				MSubgraph tempms = ms.clone();				
				ms = g.getMathGraph().modifySubgraphs.get(newSubgraph.getIndex()).clone();
				g.modifySubgraphs.remove(newSubgraph.getIndex()); //Remove old Subgraph.
				g.modifySubgraphs.add(newSubgraph, tempms);
				id = newSubgraph.getIndex();
				g.pushNotify(new GraphMessage(GraphConstraints.SUBGRAPH,newSubgraph.getIndex(),GraphConstraints.UPDATE|GraphConstraints.BLOCK_START,GraphConstraints.GRAPH_ALL_ELEMENTS));
				//Reintroduce all Nodes/Edges
				Iterator<VNode> ni = g.modifyNodes.getIterator();
				while (ni.hasNext())
				{
					VNode n2 = ni.next();
					if (tempms.containsNode(n2.getIndex()))
						g.modifySubgraphs.addNodetoSubgraph(n2.getIndex(), newSubgraph.getIndex());
				}
				Iterator<VEdge> ei = g.modifyEdges.getIterator();
				while (ei.hasNext())
				{
					VEdge e2 = ei.next();
					if (tempms.containsEdge(e2.getIndex()))
						g.modifySubgraphs.addEdgetoSubgraph(e2.getIndex(), newSubgraph.getIndex());
				}
				graph.pushNotify(new GraphMessage(GraphConstraints.SUBGRAPH,newSubgraph.getIndex(),GraphConstraints.BLOCK_END,GraphConstraints.GRAPH_ALL_ELEMENTS));
				ret = g;
				break;
			default: throw new GraphActionException("GraphAction::doReplace(); Unknown ActionObject");
		}
		if (Objecttype!=GraphConstraints.SUBGRAPH)
			g.pushNotify(new GraphMessage(Objecttype, id, GraphConstraints.UPDATE|GraphConstraints.HISTORY, GraphConstraints.ELEMENT_MASK));
		return ret;
	}

	protected VGraphInterface doCreate(VGraphInterface graph) throws GraphActionException
	{
		if (graph.getType()!=VGraphInterface.GRAPH)
			throw new GraphActionException("Can't doDelete(): Wrong Type of Graph");
		VGraph g = (VGraph)graph;
		int id=-1;
		switch(Objecttype)
		{
			case GraphConstraints.NODE:
				VNode n = (VNode)ActionObject;	
				if (g.modifyNodes.get(n.getIndex())!=null) //node exists
					throw new GraphActionException("Can't create node, already exists.");
				g.modifyNodes.add(n, mn.clone());
				id = n.getIndex();
				//Recreate all Subgraphs
				Iterator<VSubgraph> si = envG.modifySubgraphs.getIterator();
				while (si.hasNext())
				{
					VSubgraph s = si.next();
					if (envG.getMathGraph().modifySubgraphs.get(s.getIndex()).containsNode(n.getIndex()))
					{
						g.modifySubgraphs.addNodetoSubgraph(n.getIndex(), s.getIndex());
						recreateItemColor(n,g);
					}
				}
				//Recreate adjacent edges and their subgraohs
				Iterator<VEdge> ei = envG.modifyEdges.getIterator();
				while (ei.hasNext())
				{
					VEdge e = ei.next();
					MEdge me = envG.getMathGraph().modifyEdges.get(e.getIndex());
					if ((me.StartIndex==n.getIndex())||(me.EndIndex==n.getIndex()))
					{ //Add all Adjacent Edges again and recreate theis color
						g.modifyEdges.add(e, me, envG.modifyNodes.get(me.StartIndex).getPosition(), envG.modifyNodes.get(me.EndIndex).getPosition());
						recreateItemColor(e,g);
					}
				}
				break;
			case GraphConstraints.EDGE:
				VEdge e = (VEdge)ActionObject;
				if ((g.modifyEdges.get(e.getIndex())!=null)||(g.modifyNodes.get(me.StartIndex)==null)||(g.modifyNodes.get(me.EndIndex)==null)) //edge exists or one of its Nodes does not
					throw new GraphActionException("Can't create edge, it already exists or one of its Nodes does not.");
				g.modifyEdges.add(e, me, envG.modifyNodes.get(me.StartIndex).getPosition(),envG.modifyNodes.get(me.EndIndex).getPosition());
				id = e.getIndex();
				recreateItemColor(e,g);
				g.modifyEdges.get(e.getIndex()).setSelectedStatus(envG.modifyEdges.get(e.getIndex()).getSelectedStatus());
				break;
				
			case GraphConstraints.SUBGRAPH:
				VSubgraph vs = (VSubgraph)ActionObject;
				if ((g.modifySubgraphs.get(vs.getIndex())!=null))
					throw new GraphActionException("Can't create subgraph, it already exists or one of its Nodes does not.");
				
				g.modifySubgraphs.add(vs, ms); //Adds old NOdes and Edges again, too
				id = vs.getIndex();
				break;
		}// End switch
		g.pushNotify(new GraphMessage(Objecttype, id, GraphConstraints.ADDITION/GraphConstraints.HISTORY, GraphConstraints.ELEMENT_MASK));
		return graph;
	}

	protected VGraphInterface doDelete(VGraphInterface graph) throws GraphActionException
	{
		if (graph.getType()!=VGraphInterface.GRAPH)
			throw new GraphActionException("Can't doDelete(): Wrong Type of Graph");
		VGraph g = (VGraph)graph;
		int id = -1;
		switch(Objecttype)
		{
			case GraphConstraints.NODE:
				VNode n = (VNode)ActionObject;
				if (g.modifyNodes.get(n.getIndex())==null) //node does not exists
					throw new GraphActionException("Can't delete node, none there.");
				g.modifyNodes.remove(n.getIndex());
				id = n.getIndex();
				break;
			case GraphConstraints.EDGE:
				VEdge e = (VEdge)ActionObject;
				if (g.modifyEdges.get(e.getIndex())==null) //edge does not exists
					throw new GraphActionException("Can't delete edge, none there.");
				g.modifyEdges.remove(e.getIndex());
				id = e.getIndex();
				break;
			case GraphConstraints.SUBGRAPH:
				VSubgraph vs = (VSubgraph)ActionObject;
				if (g.modifySubgraphs.get(vs.getIndex())==null) //subgraph does not exists
					throw new GraphActionException("Can't delete subgraph, none there.");
				g.modifySubgraphs.remove(vs.getIndex());
				id = vs.getIndex();
				break;
			}
		//Delete does not need to update selection
		g.pushNotify(new GraphMessage(Objecttype, id, GraphConstraints.REMOVAL|GraphConstraints.HISTORY, GraphConstraints.ELEMENT_MASK));
		return g;
	}	
}
