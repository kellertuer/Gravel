package history;

import java.util.Iterator;

import model.*;
import model.Messages.GraphConstraints;
import model.Messages.GraphMessage;
/**
 * HyperGraphAction represents one single action that can be performed to manipulate a VHyperGraph.
 * Besides a standard action, that replaces a graph with a new one (because 
 * there were many changes), there are specific Actions to reduce memory and computation usage
 * 
 * The given Values are Actions that happened to the Graph:
 * - Replace, where the change is given in a new graph, node, hyperedge or subgraph that is replaced
 * - create, where an Object is created in the graph (a node, hyperedge or subgraph)
 * - delete, where an Object is deleted
 * 
 *  The Action specified is the action DONE to the graph.
 *  
 *  If no specific action fits, it is recommended to use REPLACE with a complete graph, this suits e.g.
 *  for the manipulation of a node, where the ID changes.
 *  
 * @author Ronny Bergmann
 * @since 0.4
 */
public class HyperGraphAction extends CommonGraphAction {
	
	private MHyperEdge mhe;
	private VHyperGraph envHG;
	/**
	 * Create a New Action with whole HyperGraph or an Selection Change
	 * Only replacements are possible, because Graphs can't be created or deleted while editing a graph
	 * Selection is also simplified to updates, because the deselect-function does the same as the exChangeUpdate here
	 * (in their computational comparison)
	 *  
	 * @param o VHyperGraph
	 * @param action Action that happened
	 * @param boolean Itemchange Indicator for the change happened: True if an Node/Hyperedge/Subgraph was changed, false if only selection was changed
	 * @throws GraphActionException E.g. a Graph can not be Created or Deleted within a Graph
	 */
	public HyperGraphAction(VHyperGraph o, int action, boolean Itemchange) throws GraphActionException
	{
		if (o==null)
			throw new GraphActionException("Could not Create Action: HyperGraph must not be null.");
		ActionObject=o.clone();
		Action=action;
		if ((action&(GraphConstraints.ADDITION|GraphConstraints.REMOVAL))>0) //Create or delete is active
		{
			ActionObject=null;
			Action=0;
			throw new GraphActionException("Creating or Deletion Graph/Selection is not possible as an Trackable Action.");
		}
		if (Itemchange)
			Objecttype=GraphConstraints.HYPERGRAPH;
		else
			Objecttype=GraphConstraints.SELECTION;
	}
	/**
	 * Create an Action for Manipulation of an HyperEdge
	 * @param o The Visual Information of the Edge
	 * @param action Action Happening to it
	 * @param environment VHyperGraph containins at least the Endnodes of the HyperEdge and the Subgraphs the affected Hyperedge belongs to
	 * 
	 * @throws GraphActionException
	 */
	public HyperGraphAction(VHyperEdge o, int action, VHyperGraph environment) throws GraphActionException
	{
		if ((o==null)||(environment==null))
			throw new GraphActionException("Could not Create Action: Edge and Environment must not be null.");
		if (environment.modifyHyperEdges.get(o.getIndex())==null)
			throw new GraphActionException("Could not Create Action: Environment must contain hyper edge");
		ActionObject=o.clone();
		Action=action;
		mhe = environment.getMathGraph().modifyHyperEdges.get(o.getIndex());
		env = environment;
		envHG = environment;
		Objecttype=GraphConstraints.HYPEREDGE;
	}
	/**
	 * Create New Action inducted by a Node
	 * @param o the node
	 * @param action the action
	 * @param environment Graph containing the Subgraphs the node belongs to and (at least) andjacent edges and their second nodes
	 * @throws GraphActionException
	 */
	public HyperGraphAction(VNode o, int action, VHyperGraph environment) throws GraphActionException
	{
		super(o,action,environment);
		envHG = environment.clone();
	}
	/**
	 * Create a New Action induced by a Subgraph
	 * @param o VSubgraph manipulated
	 * @param action what was done?
	 * @param c Color.
	 * @throws GraphActionException
	 */
	public HyperGraphAction(VSubgraph o, int action, MSubgraph m) throws GraphActionException
	{
		super(o,action,m);
	}
	
	protected void exchangeSelection(VGraphInterface first, VGraphInterface second)
	{
		if ((first.getType()!=VGraphInterface.HYPERGRAPH)||(first.getType()!=second.getType()))
				return;
		//Exchange Selection on the nodes
		super.exchangeSelection(first, second);

		Iterator<VHyperEdge> ei = ((VHyperGraph)first).modifyHyperEdges.getIterator();
		while (ei.hasNext())
		{
			VHyperEdge e = ei.next();
			int sel = e.getSelectedStatus();
			VHyperEdge e2 = ((VHyperGraph)second).modifyHyperEdges.get(e.getIndex());
			if (e2!=null)
			{
				e.setSelectedStatus(e2.getSelectedStatus());
				e2.setSelectedStatus(sel);
			}
		}
	}

	protected VGraphInterface doReplace(VGraphInterface graph) throws GraphActionException
	{
		if (graph.getType()!=VGraphInterface.HYPERGRAPH)
			throw new GraphActionException("Can't doDelete(): Wrong Type of Graph");
		VHyperGraph g = (VHyperGraph)graph;
		VHyperGraph ret;
		int id=-1;
		switch(Objecttype)
		{
			case GraphConstraints.SELECTION:
				ret = g;
			break;
			case GraphConstraints.HYPERGRAPH: //Replace whole graph and save the actual parameter als old object
				ret = new VHyperGraph();
				ret.replace((VHyperGraph)ActionObject);
				((VHyperGraph)ActionObject).replace(g);
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
				id = n.getIndex();
				envHG.modifyNodes.replace((VNode)ActionObject, mn);
				exchangeSubgraphMembership(GraphConstraints.NODE,n.getIndex(),env,graph);
				ret = g; //return graph
			break;
			case GraphConstraints.HYPEREDGE: //Replace Edge in graph and keep the replaced one as old one 
				VHyperEdge e = ((VHyperEdge)ActionObject);
				if (g.modifyHyperEdges.get(e.getIndex())==null) //edge does not exists
					throw new GraphActionException("Can't replace hyper edge, none there.");
				ActionObject = g.modifyHyperEdges.get(e.getIndex()).clone(); //save old edge
				//Save Color of old node
				Iterator<VSubgraph> esi = g.modifySubgraphs.getIterator();
				while (esi.hasNext())
				{
					VSubgraph s = esi.next();
					if (g.getMathGraph().modifySubgraphs.get(s.getIndex()).containsEdge(e.getIndex()))
						((VHyperEdge)ActionObject).addColor(s.getColor());
				}
				MHyperEdge tempmhe = mhe.clone();
				MHyperEdge mhe = g.getMathGraph().modifyHyperEdges.get(e.getIndex());
				g.modifyHyperEdges.replace(e, tempmhe);
				id = e.getIndex();
				envHG.modifyHyperEdges.replace((VHyperEdge)ActionObject, mhe);
				exchangeSubgraphMembership(GraphConstraints.HYPEREDGE,e.getIndex(),env,graph);
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
				g.pushNotify(new GraphMessage(GraphConstraints.SUBGRAPH,newSubgraph.getIndex(),GraphConstraints.UPDATE|GraphConstraints.BLOCK_START|GraphConstraints.HISTORY,GraphConstraints.HYPERGRAPH_ALL_ELEMENTS));
				//Reintroduce all Nodes/Edges
				Iterator<VNode> ni = g.modifyNodes.getIterator();
				while (ni.hasNext())
				{
					VNode n2 = ni.next();
					if (tempms.containsNode(n2.getIndex()))
						g.modifySubgraphs.addNodetoSubgraph(n2.getIndex(), newSubgraph.getIndex());
				}
				Iterator<VHyperEdge> ei = g.modifyHyperEdges.getIterator();
				while (ei.hasNext())
				{
					VHyperEdge e2 = ei.next();
					if (tempms.containsEdge(e2.getIndex()))
						g.modifySubgraphs.addEdgetoSubgraph(e2.getIndex(), newSubgraph.getIndex());
				}
				graph.pushNotify(new GraphMessage(GraphConstraints.SUBGRAPH,newSubgraph.getIndex(),GraphConstraints.BLOCK_END,GraphConstraints.HYPERGRAPH_ALL_ELEMENTS));
				ret = g;
				break;
			default: throw new GraphActionException("HyperGraphAction::doReplace(); Unknown ActionObject:"+ActionObject);
		}
		if (Objecttype!=GraphConstraints.SUBGRAPH) //We had a Block on Subgraphs so don't push again then...
			g.pushNotify(new GraphMessage(Objecttype, id, GraphConstraints.UPDATE|GraphConstraints.HISTORY, GraphConstraints.ELEMENT_MASK));
		return ret;
	}

	protected VGraphInterface doCreate(VGraphInterface graph) throws GraphActionException
	{
		if (graph.getType()!=VGraphInterface.HYPERGRAPH)
			throw new GraphActionException("HyperGraphAction: Can't doDelete(): Wrong Type of Graph");
		VHyperGraph g = (VHyperGraph)graph;
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
				Iterator<VSubgraph> si = envHG.modifySubgraphs.getIterator();
				while (si.hasNext())
				{
					VSubgraph s = si.next();
					if (envHG.getMathGraph().modifySubgraphs.get(s.getIndex()).containsNode(n.getIndex()))
					{
						g.modifySubgraphs.addNodetoSubgraph(n.getIndex(), s.getIndex());
						recreateItemColor(n,g);
					}
				}
				//Recreate adjacent edges (if they were deleted) and their subgraphs
				Iterator<VHyperEdge> ei = envHG.modifyHyperEdges.getIterator();
				while (ei.hasNext())
				{
					VHyperEdge e = ei.next();
					MHyperEdge me = envHG.getMathGraph().modifyHyperEdges.get(e.getIndex());
					if (me.containsNode(n.getIndex()))
					{ //Add all Adjacent Edges again and recreate theis color
						g.modifyHyperEdges.add(e, me);
						recreateItemColor(e,g); //recreate its color from g
					}
				}
				break;
			case GraphConstraints.HYPEREDGE:
				VHyperEdge e = (VHyperEdge)ActionObject;
				if ((g.modifyHyperEdges.get(e.getIndex())!=null)) //edge exists or one of its Nodes does not
					throw new GraphActionException("Can't create edge, it already exists");
				for (int i=0; i<mhe.getEndNodes().size(); i++) //All possible node indices
				{
					if (mhe.containsNode(i)&&(g.modifyNodes.get(i)==null)) //Hyperedge contains this node but it does not exist
						throw new GraphActionException("Can't create hyper edge, one of its End nodes does not exist");						
				}
				g.modifyHyperEdges.add(e, mhe);
				id = e.getIndex();
				recreateItemColor(e,g);
				g.modifyHyperEdges.get(e.getIndex()).setSelectedStatus(envHG.modifyHyperEdges.get(e.getIndex()).getSelectedStatus());
				break;
			
			case GraphConstraints.SUBGRAPH:
				VSubgraph vs = (VSubgraph)ActionObject;
				if ((g.modifySubgraphs.get(vs.getIndex())!=null))
					throw new GraphActionException("Can't create subgraph, it already exists or one of its Nodes does not.");
				id = vs.getIndex();
				g.modifySubgraphs.add(vs, ms); //Adds old NOdes and Edges again, too
				break;
		}// End switch
		g.pushNotify(new GraphMessage(Objecttype, id, GraphConstraints.ADDITION|GraphConstraints.HISTORY, GraphConstraints.ELEMENT_MASK));
		return graph;
	}

	protected VGraphInterface doDelete(VGraphInterface graph) throws GraphActionException
	{
		if (graph.getType()!=VGraphInterface.HYPERGRAPH)
			throw new GraphActionException("Can't doDelete(): Wrong Type of Graph");
		VHyperGraph g = (VHyperGraph)graph;
		int id=-1;
		switch(Objecttype)
		{
			case GraphConstraints.NODE:
				VNode n = (VNode)ActionObject;
				if (g.modifyNodes.get(n.getIndex())==null) //node does not exists
					throw new GraphActionException("Can't delete node, none there.");
				g.modifyNodes.remove(n.getIndex());
				id = n.getIndex();
				break;
			case GraphConstraints.HYPEREDGE:
				VHyperEdge e = (VHyperEdge)ActionObject;
				if (g.modifyHyperEdges.get(e.getIndex())==null) //edge does not exists
					throw new GraphActionException("Can't delete hyperedge, none there.");
				g.modifyHyperEdges.remove(e.getIndex());
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