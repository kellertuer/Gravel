package history;

import model.*;
/**
 * GraphAction represents one single action that can be performed to manipulate a VGraph.
 * Besides a standard action, that replaces a graph with a new one (because 
 * there were many changes), there are specific Actions to reduce memory and computation usage
 * 
 * The given Values are Actions that happened to the Graph:
 * - Replace, where the change is given in a new graph, node, edge or subset that is replaced
 * - create, where an Object is created in the graph (a node, edge or subset)
 * - delete, where an Object is deleted
 * 
 *  The Action specified is the action DONE to the graph.
 *  
 *  If no specific action fits, it is recommended to use REPLACE with a complete graph, this suits e.g.
 *  for the manipulation of a node, where the ID changes.
 * @author Ronny Bergmann
 */
public class GraphAction {

	//Acion that happened to the Graph
	public static final int REPLACE = 1;
	public static final int CREATE = 2;
	public static final int DELETE = 4;
	
	//Encoding of the internal object used
	private static final int GRAPH = 0;
	private static final int NODE = 1;
	private static final int EDGE = 2;
	private static final int SUBSET = 4;
	
	private Object ActionObject;
	private int Objecttype;
	private int Action,StartNode=0, EndNode=0, Value=0;
	private String name;
	/**
	 * Create a New Action with whole Graph
	 *  
	 * @param o VGraph
	 * @param action Action that happened
	 * @throws GraphActionException E.g. a Graph can not be Created or Deleted within a Graph
	 */
	public GraphAction(VGraph o, int action) throws GraphActionException
	{
		ActionObject=o;
		Action=action;
		if ((action&(CREATE|DELETE))>0) //Create or delete is active
		{
			ActionObject=null;
			Action=0;
			throw new GraphActionException("Creating Graph or Deleting it is not possible as an Trackable Action.");
		}
		Objecttype=GRAPH;
	}
	/**
	 * Create New Action inducted by a Node
	 * @param o the node
	 * @param action the action
	 * @param s node name
	 * @throws GraphActionException
	 */
	public GraphAction(VNode o, int action, String s) throws GraphActionException
	{
		ActionObject = o;
		Action=action;
		name=s;
		Objecttype=NODE;
	}
	/**
	 * Create a New Action induced by a Subset
	 * @param o VSubSet manipulated
	 * @param action what was done?
	 * @param s name of Subset
	 * @param c Color.
	 * @throws GraphActionException
	 */
	public GraphAction(VSubSet o, int action, String s) throws GraphActionException
	{
		ActionObject = o;
		Action=action;
		name=s;
		Objecttype=SUBSET;
	}
	/**
	 * Create an Action for Manipulation of an Edge
	 * @param o The Visual Information of the Edge
	 * @param action Action Happening to it
	 * @param s StartNode Index
	 * @param e Endnnode-Index
	 * @param v Value of the edge
	 * @param name name of the edge
	 * @throws GraphActionException
	 */
	public GraphAction(VEdge o, int action, int s, int e, int v, String name) throws GraphActionException
	{
		ActionObject=o;
		Action=action;
		this.name=name;
		StartNode=s;
		EndNode=e;
		Value=v;
		Objecttype=EDGE;
	}
	/**
	 * Replace this object in or with a Graph, and save the replaced object in this one, 
	 * so twice do or undo is the same graph again
	 * 
	 */
	private VGraph doReplace(VGraph graph) throws GraphActionException
	{
		VGraph ret;
		switch(Objecttype)
		{
			case GRAPH: 
				ret=(VGraph)ActionObject;
				ActionObject=graph;
			break;
			case NODE:
				VNode n = (VNode)ActionObject;
				if (graph.getNode(n.index)==null) //node does not exists
					throw new GraphActionException("Can't replace node, none there.");
				ActionObject = graph.getNode(n.index);
				name = graph.getNodeName(n.index);
				graph.updateNode(n.index, name, n);
				ret = graph;
			break;
			case EDGE:
				VEdge e = (VEdge)ActionObject;
				if (graph.getEdge(e.index)==null) //edge does not exists
					throw new GraphActionException("Can't replace edge, none there.");
				ActionObject = graph.getEdge(e.index);
				StartNode = graph.getEdgeProperties(e.index).get(MGraph.EDGESTARTINDEX);
				EndNode = graph.getEdgeProperties(e.index).get(MGraph.EDGEENDINDEX);
				Value = graph.getEdgeProperties(e.index).get(MGraph.EDGEVALUE);
				name = graph.getEdgeName(e.index);
				
				graph.pushNotify(new GraphMessage(GraphMessage.EDGE,e.index,GraphMessage.UPDATED|GraphMessage.BLOCK_START,GraphMessage.EDGE));
				graph.removeEdge(e.index);
				graph.addEdge(e, StartNode, EndNode, Value, name);
				graph.pushNotify(new GraphMessage(GraphMessage.EDGE,e.index,GraphMessage.BLOCK_END,GraphMessage.EDGE));
				ret = graph;
			break;
			case SUBSET:
				VSubSet vs = (VSubSet)ActionObject;
				if (graph.getSubSet(vs.getIndex())==null) //edge does not exists
					throw new GraphActionException("Can't replace subset, none there.");
				ActionObject = graph.getSubSetName(vs.getIndex());
				name = graph.getSubSetName(vs.getIndex());
				graph.pushNotify(new GraphMessage(GraphMessage.SUBSET,vs.getIndex(),GraphMessage.UPDATED|GraphMessage.BLOCK_START,GraphMessage.ALL_ELEMENTS));
				graph.removeSubSet(vs.getIndex());
				graph.addSubSet(vs.getIndex(), name, vs.getColor());
				graph.pushNotify(new GraphMessage(GraphMessage.SUBSET,vs.getIndex(),GraphMessage.BLOCK_END,GraphMessage.ALL_ELEMENTS));
				ret = graph;
				break;
			default: throw new GraphActionException("GraphAction::doReplace(); Unknown ActionObject");
		}
		return ret;
	}

	private VGraph doCreate(VGraph graph) throws GraphActionException
	{
		switch(Objecttype)
		{
			case NODE:
				VNode n = (VNode)ActionObject;	
				if (graph.getNode(n.index)!=null) //node exists
					throw new GraphActionException("Can't create node, already exists.");
				graph.addNode(n,name);
				break;
			case EDGE:
				VEdge e = (VEdge)ActionObject;
				if ((graph.getEdge(e.index)!=null)||(graph.getNode(StartNode)==null)||(graph.getNode(EndNode)==null)) //edge exists or one of its Nodes does not
					throw new GraphActionException("Can't create edge, it already exists or one of its Nodes does not.");
				graph.addEdge(e, StartNode, EndNode, Value, name);
				break;
			case SUBSET:
				VSubSet vs = (VSubSet)ActionObject;
				if ((graph.getSubSet(vs.getIndex())!=null)) //subset exists or one of its Nodes does not
					throw new GraphActionException("Can't create subset, it already exists or one of its Nodes does not.");
				graph.addSubSet(vs.getIndex(), name, vs.getColor());
				break;
		}
		return graph;
	}
	private VGraph doDelete(VGraph graph) throws GraphActionException
	{
		switch(Objecttype)
		{
			case NODE:
				VNode n = (VNode)ActionObject;
				if (graph.getNode(n.index)==null) //node does not exists
					throw new GraphActionException("Can't dekete node, none there.");
				graph.removeNode(n.index);
				break;
			case EDGE:
				VEdge e = (VEdge)ActionObject;
				if (graph.getEdge(e.index)==null) //edge does not exists
					throw new GraphActionException("Can't dekete edge, none there.");
				graph.removeEdge(e.index);
				break;
			case SUBSET:
				VSubSet vs = (VSubSet)ActionObject;
				if (graph.getSubSet(vs.getIndex())!=null) //subset does not exists
					throw new GraphActionException("Can't delete subset, none there.");
				graph.removeSubSet(vs.getIndex());
				break;
			}
		return graph;
	}

	/**
	 * Apply this Action to a Graph. The graph given as argument is manipulated directly, though returned, too.
	 * @param graph the graph to be manipulated
	 * @return the manipulated graph
	 */
	public VGraph doAction(VGraph graph) throws GraphActionException
	{
		if (Action==0)
			throw new GraphActionException("No Action given");
		if (ActionObject==null)
			throw new GraphActionException("No Object available for the Action");
		switch(Action) 
		{
			case REPLACE: 
				return doReplace(graph);
			case CREATE:
				return doCreate(graph);
			case DELETE:
				return doDelete(graph);
		}
		throw new GraphActionException("No Action given.");
	}
	/**
	 * Apply the action to the Clone of a Graph. The given Parameter Graph is not manipulated but cloned
	 * The Clone then gets manipulated and is returned
	 * @param g A Graph
	 * @return The Manipulated Clone
	 */
	public VGraph doActionOnClone(VGraph g) throws GraphActionException
	{
		return doAction(g.clone());
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
	public VGraph UnDoAction(VGraph graph) throws GraphActionException
	{
		if (Action==0)
			throw new GraphActionException("No Action given");
		if (ActionObject==null)
			throw new GraphActionException("No Object available for the Action");
		switch(Action) 
		{
			case REPLACE: 
				return doReplace(graph);
			case CREATE:
				return doCreate(graph);
			case DELETE:
				return doDelete(graph);
		}
		throw new GraphActionException("No Action given.");

	}
	
	public VGraph UnDoActionOnClone(VGraph g) throws GraphActionException
	{
		return UnDoAction(g.clone());
	}
}
