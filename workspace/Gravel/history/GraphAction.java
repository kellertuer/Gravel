package history;

import java.util.Iterator;

import model.*;
import model.Messages.GraphMessage;
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
 *  
 * @author Ronny Bergmann
 * @since 0.3
 */
public class GraphAction {

	//Acion that happened to the Graph
	public static final int UPDATE = 1;
	public static final int ADDITION = 2;
	public static final int REMOVAL = 4;
	
	//Encoding of the internal object used
	private static final int NODE = 1;
	private static final int EDGE = 2;
	private static final int SUBSET = 4;
	private static final int SELECTION = 8;
	private static final int GRAPH = 128;
	
	private Object ActionObject;
	private int Objecttype;
	private int Action;
	private MEdge me;
	private MNode mn;
	private MSubSet ms;
	//Environement
	private VGraph env;
	/**
	 * Create a New Action with whole Graph or an Selection Change
	 * Only updates are possible, because Graphs can't be created or deleted while editing a graph
	 * Selection is also simplified to updates, because the deselect-function does the same as the exChangeUpdate here
	 * (in their computational comparison)
	 *  
	 * @param o VGraph
	 * @param action Action that happened
	 * @param boolean Itemchange Indicator for the change happened: True if an Node/Edge/SubSet was changed, false if only selection was changed
	 * @throws GraphActionException E.g. a Graph can not be Created or Deleted within a Graph
	 */
	public GraphAction(VGraph o, int action, boolean Itemchange) throws GraphActionException
	{
		if (o==null)
			throw new GraphActionException("Could not Create Action: Graph must not be null.");
		ActionObject=o.clone();
		Action=action;
		if ((action&(ADDITION|REMOVAL))>0) //Create or delete is active
		{
			ActionObject=null;
			Action=0;
			throw new GraphActionException("Creating or Deletion Graph/Selection is not possible as an Trackable Action.");
		}
		if (Itemchange)
			Objecttype=GRAPH;
		else
			Objecttype=SELECTION;
	}
	/**
	 * Create New Action inducted by a Node
	 * @param o the node
	 * @param action the action
	 * @param environment Graph containing the Subsets the node belongs to and (at least) andjacent edges and their second nodes
	 * @throws GraphActionException
	 */
	public GraphAction(VNode o, int action, VGraph environment) throws GraphActionException
	{
		if ((o==null)||(environment==null))
			throw new GraphActionException("Could not Create Action: Node and environment must not be null.");
		if (environment.modifyNodes.getNode(o.getIndex())==null)
			throw new GraphActionException("Could not Create Action: Environment must contains at least the node itself.");			
		ActionObject = o;
		Action=action;
		mn = environment.getMathGraph().getNode(o.getIndex());
		env = environment;
		Objecttype=NODE;
	}
	/**
	 * Create a New Action induced by a Subset
	 * @param o VSubSet manipulated
	 * @param action what was done?
	 * @param c Color.
	 * @throws GraphActionException
	 */
	public GraphAction(VSubSet o, int action, MSubSet m) throws GraphActionException
	{
		if ((o==null)||(m==null))
			throw new GraphActionException("Could not Create Action: SubSet must not be null.");
		ActionObject = o.clone();
		ms = m.clone();
		Action=action;
		Objecttype=SUBSET;
	}
	/**
	 * Create an Action for Manipulation of an Edge
	 * @param o The Visual Information of the Edge
	 * @param action Action Happening to it
	 * @param environment VGraph containins at least the Start- and Endnode and the Subsets the Edge belongs to
	 * @throws GraphActionException
	 */
	public GraphAction(VEdge o, int action, VGraph environment) throws GraphActionException
	{
		if ((o==null)||(environment==null))
			throw new GraphActionException("Could not Create Action: Edge and Environment must not be null.");
		if (environment.modifyEdges.getEdge(o.getIndex())==null)
			throw new GraphActionException("Could not Create Action: Environment must contain edge");
		ActionObject=o.clone();
		Action=action;
		me = environment.getMathGraph().getEdge(o.getIndex());
		env = environment;
		Objecttype=EDGE;
	}

	/**
	 * Exchange the Memberships of a node or edge to Subsets between two graphs
	 * The subsets of the first graph are iterated (its assumed they have the same subsets)
	 * and for every subset the membership of the item is exchanged.
	 * 
	 * @param ItemType NODE or EDGE
	 * @param itemindex index of the node or edge
	 * @param first first graph, where the subsets are iterated
	 * @param second second graph
	 * @throws GraphActionException
	 */
	private void exchangeSubSetMembership(int ItemType, int itemindex, VGraph first, VGraph second) throws GraphActionException
	{
		Iterator<VSubSet> si = first.modifySubSets.getSubSetIterator();
		while (si.hasNext())
		{
			VSubSet s = si.next();
			if (ItemType==NODE)
			{
				boolean wasfirst = first.getMathGraph().getSubSet(s.getIndex()).containsNode(itemindex);
				boolean wassecond = second.getMathGraph().getSubSet(s.getIndex()).containsNode(itemindex);
				if (wasfirst)
					second.modifySubSets.addNodetoSubSet(itemindex, s.getIndex());
				else
					second.modifySubSets.removeNodefromSubSet(itemindex, s.getIndex());
				if (wassecond)
					first.modifySubSets.addNodetoSubSet(itemindex, s.getIndex());
				else
					first.modifySubSets.removeNodefromSubSet(itemindex, s.getIndex());
			}
			else if (ItemType==EDGE)
			{
				boolean wasfirst = first.getMathGraph().getSubSet(s.getIndex()).containsEdge(itemindex);
				boolean wassecond = second.getMathGraph().getSubSet(s.getIndex()).containsEdge(itemindex);
				if (wasfirst)
					second.modifySubSets.addEdgetoSubSet(itemindex, s.getIndex());
				else
					second.modifySubSets.removeEdgefromSubSet(itemindex, s.getIndex());
				if (wassecond)
					first.modifySubSets.addEdgetoSubSet(itemindex, s.getIndex());
				else
					first.modifySubSets.removeEdgefromSubSet(itemindex, s.getIndex());
			}
			else
				throw new GraphActionException("Unknown ItemType "+ItemType);
		}
	}
	/**
	 * Recreate the Subsets the edge belonged to, depending on the actual environment
	 * 
	 * @param e Edge 
	 * @param g Graph the edge should be recreated in and the colors should be restored in
	 */
	private void recreateEdgeColor(VEdge e, VGraph g) throws GraphActionException
	{
		Iterator<VSubSet> si =  env.modifySubSets.getSubSetIterator();
		while (si.hasNext())
		{
			VSubSet s = si.next();
			if (env.getMathGraph().getSubSet(s.getIndex()).containsEdge(e.getIndex()))
			{
				if (g.modifySubSets.getSubSet(s.getIndex())==null)
					throw new GraphActionException("Can't replace edge, replacements belongs to Subsets, that don't exists in given parameter graph");
				g.modifySubSets.addEdgetoSubSet(e.getIndex(), s.getIndex());
			}
		}
	}
	/**
	 * Exchange  Selection between first and second Graph
	 * The first graph might be smaller (e.g. might be environment of a node), so that one is iterated
	 * 
	 * @param first
	 * @param second
	 * @throws GraphActionException 
	 */
	private void exChangeSelection(VGraph first, VGraph second) throws GraphActionException
	{
		Iterator<VNode> ni = first.modifyNodes.getNodeIterator();
		while (ni.hasNext())
		{
			VNode n = ni.next();
			int sel = n.getSelectedStatus();
			VNode n2 = second.modifyNodes.getNode(n.getIndex());
			if (n2!=null) //if its not null
			{ 
				n.setSelectedStatus(n2.getSelectedStatus());
				n2.setSelectedStatus(sel);
			}
		}
		Iterator<VEdge> ei = first.modifyEdges.getEdgeIterator();
		while (ei.hasNext())
		{
			VEdge e = ei.next();
			int sel = e.getSelectedStatus();
			VEdge e2 = second.modifyEdges.getEdge(e.getIndex());
			if (e2!=null)
			{
				e.setSelectedStatus(e2.getSelectedStatus());
				e2.setSelectedStatus(sel);
			}
		}
	}
	/**
	 * Replace this object in or with a Graph.
	 * 
	 * The replaced element is stored in the action part, so that another replace restores the first situation.
	 * 
	 */
	private VGraph doReplace(VGraph graph) throws GraphActionException
	{
		VGraph ret;
		switch(Objecttype)
		{
			case SELECTION:
				ret = graph;
			break;
			case GRAPH: //Replace whole graph and save the actual parameter als old object
				ret = new VGraph(((VGraph)ActionObject).getMathGraph().isDirected(), ((VGraph)ActionObject).getMathGraph().isLoopAllowed(), ((VGraph)ActionObject).getMathGraph().isMultipleAllowed());
				ret.replace((VGraph)ActionObject);
				((VGraph)ActionObject).replace(graph);
				graph.replace(ret);
			break;
			case NODE: //Replace Node and keep the given one in the graph as old one
				VNode n = (VNode)ActionObject;
				if (graph.modifyNodes.getNode(n.getIndex())==null) //node does not exists
					throw new GraphActionException("Can't replace node, none there.");
				ActionObject = graph.modifyNodes.getNode(n.getIndex()).clone(); //save old node
				//Save Color of old node
				Iterator<VSubSet> si = graph.modifySubSets.getSubSetIterator();
				while (si.hasNext())
				{
					VSubSet s = si.next();
					if (graph.getMathGraph().getSubSet(s.getIndex()).containsNode(n.getIndex()))
						((VNode)ActionObject).addColor(s.getColor());
				}
				MNode tempmn = mn;
				mn = new MNode(n.getIndex(), graph.getMathGraph().getNode(n.getIndex()).name);
				graph.modifyNodes.replaceNode(n, tempmn);
				env.modifyNodes.replaceNode((VNode)ActionObject, mn);
				exchangeSubSetMembership(NODE,n.getIndex(),env,graph);
				ret = graph; //return graph
			break;
			case EDGE: //Replace Edge in graph and keep the replaced one as old one 
				VEdge e = (VEdge)ActionObject;
				if (graph.modifyEdges.getEdge(e.getIndex())==null) //edge does not exists
					throw new GraphActionException("Can't replace edge, none there.");
				ActionObject = graph.modifyEdges.getEdge(e.getIndex()).clone(); //save old edge
				//Save Color of old node
				Iterator<VSubSet> esi = graph.modifySubSets.getSubSetIterator();
				while (esi.hasNext())
				{
					VSubSet s = esi.next();
					if (graph.getMathGraph().getSubSet(s.getIndex()).containsEdge(e.getIndex()))
						((VEdge)ActionObject).addColor(s.getColor());
				}
				MEdge tempme = new MEdge(me.index, me.StartIndex, me.EndIndex, me.Value, me.name);
				MEdge me = graph.getMathGraph().getEdge(e.getIndex());
				graph.modifyEdges.replaceEdge(e, tempme);
				env.modifyEdges.replaceEdge((VEdge)ActionObject, me);
				exchangeSubSetMembership(EDGE,e.getIndex(),env,graph);
				ret = graph;
			break;
			case SUBSET:
				VSubSet newSubSet = (VSubSet)ActionObject;
				if (graph.modifySubSets.getSubSet(newSubSet.getIndex())==null) //edge does not exists
					throw new GraphActionException("Can't replace subset, none there.");
				ActionObject = graph.modifySubSets.getSubSet(newSubSet.getIndex()); //Save old one in action
				MSubSet tempms = ms.clone();				
				ms = graph.getMathGraph().getSubSet(newSubSet.getIndex()).clone();
				graph.modifySubSets.removeSubSet(newSubSet.getIndex()); //Remove old SubSet.
				graph.modifySubSets.addSubSet(newSubSet, tempms);
				graph.pushNotify(new GraphMessage(GraphMessage.SUBSET,newSubSet.getIndex(),GraphMessage.UPDATE|GraphMessage.BLOCK_START,GraphMessage.ALL_ELEMENTS));
				//Reintroduce all Nodes/Edges
				Iterator<VNode> ni = graph.modifyNodes.getNodeIterator();
				while (ni.hasNext())
				{
					VNode n2 = ni.next();
					if (tempms.containsNode(n2.getIndex()))
						graph.modifySubSets.addNodetoSubSet(n2.getIndex(), newSubSet.getIndex());
				}
				Iterator<VEdge> ei = graph.modifyEdges.getEdgeIterator();
				while (ei.hasNext())
				{
					VEdge e2 = ei.next();
					if (tempms.containsEdge(e2.getIndex()))
						graph.modifySubSets.addEdgetoSubSet(e2.getIndex(), newSubSet.getIndex());
				}
				graph.pushNotify(new GraphMessage(GraphMessage.SUBSET,newSubSet.getIndex(),GraphMessage.BLOCK_END,GraphMessage.ALL_ELEMENTS));
				ret = graph;
				break;
			default: throw new GraphActionException("GraphAction::doReplace(); Unknown ActionObject");
		}
		return ret;
	}
	/**
	 * Perform a Create - after that the message is directly without manipulation its own undo
	 * @param graph
	 * @return
	 * @throws GraphActionException
	 */
	private VGraph doCreate(VGraph graph) throws GraphActionException
	{
		switch(Objecttype)
		{
			case NODE:
				VNode n = (VNode)ActionObject;	
				if (graph.modifyNodes.getNode(n.getIndex())!=null) //node exists
					throw new GraphActionException("Can't create node, already exists.");
				graph.modifyNodes.addNode(n, mn);
				//Recreate all Subsets
				Iterator<VSubSet> si = env.modifySubSets.getSubSetIterator();
				while (si.hasNext())
				{
					VSubSet s = si.next();
					if (env.getMathGraph().getSubSet(s.getIndex()).containsNode(n.getIndex()))
						graph.modifySubSets.addNodetoSubSet(n.getIndex(), s.getIndex());
				}
				//Recreate adjacent edges and their subsets
				Iterator<VEdge> ei = env.modifyEdges.getEdgeIterator();
				while (ei.hasNext())
				{
					VEdge e = ei.next();
					MEdge me = env.getMathGraph().getEdge(e.getIndex());
					if ((me.StartIndex==n.getIndex())||(me.EndIndex==n.getIndex()))
					{ //Add all Adjacent Edges again and recreate theis color
						graph.modifyEdges.addEdge(e, me, env.modifyNodes.getNode(me.StartIndex).getPosition(), env.modifyNodes.getNode(me.EndIndex).getPosition());
						recreateEdgeColor(e,graph);
					}
				}
				break;
			case EDGE:
				VEdge e = (VEdge)ActionObject;
				if ((graph.modifyEdges.getEdge(e.getIndex())!=null)||(graph.modifyNodes.getNode(me.StartIndex)==null)||(graph.modifyNodes.getNode(me.EndIndex)==null)) //edge exists or one of its Nodes does not
					throw new GraphActionException("Can't create edge, it already exists or one of its Nodes does not.");
				graph.modifyEdges.addEdge(e, me, env.modifyNodes.getNode(me.StartIndex).getPosition(),env.modifyNodes.getNode(me.EndIndex).getPosition());
				recreateEdgeColor(e,graph);
				graph.modifyEdges.getEdge(e.getIndex()).setSelectedStatus(env.modifyEdges.getEdge(e.getIndex()).getSelectedStatus());
				break;
			case SUBSET:
				VSubSet vs = (VSubSet)ActionObject;
				if ((graph.modifySubSets.getSubSet(vs.getIndex())!=null)) //subset exists
					throw new GraphActionException("Can't create subset, it already exists or one of its Nodes does not.");
				graph.modifySubSets.addSubSet(vs, ms); //Adds old NOdes and Edges again, too
				break;
		}// End switch
		return graph;
	}
	/**
	 * Perform a Delete
	 * The Action itself is not manipulated, because it is its own undo
	 * @param graph
	 * @return
	 * @throws GraphActionException
	 */
	private VGraph doDelete(VGraph graph) throws GraphActionException
	{
		switch(Objecttype)
		{
			case NODE:
				VNode n = (VNode)ActionObject;
				if (graph.modifyNodes.getNode(n.getIndex())==null) //node does not exists
					throw new GraphActionException("Can't delete node, none there.");
				graph.modifyNodes.removeNode(n.getIndex());
				break;
			case EDGE:
				VEdge e = (VEdge)ActionObject;
				if (graph.modifyEdges.getEdge(e.getIndex())==null) //edge does not exists
					throw new GraphActionException("Can't delete edge, none there.");
				graph.modifyEdges.removeEdge(e.getIndex());
				break;
			case SUBSET:
				VSubSet vs = (VSubSet)ActionObject;
				if (graph.modifySubSets.getSubSet(vs.getIndex())==null) //subset does not exists
					throw new GraphActionException("Can't delete subset, none there.");
				graph.modifySubSets.removeSubSet(vs.getIndex());
				break;
			}
		//Delete does not need to update selection
		return graph;
	}	
	/**
	 * Apply this Action to a Graph. The graph given as argument is manipulated directly, though returned, too.
	 * The action itself is transofrmed to be its own undo-Message
	 * @param graph the graph to be manipulated
	 * @return the manipulated graph
	 */
	public VGraph redoAction(VGraph graph) throws GraphActionException
	{
		if (ActionObject==null)
			throw new GraphActionException("No Object available for the Action");
		VGraph ret;
		switch(Action) 
		{
			case UPDATE: 
				ret = doReplace(graph);
				break;
			case ADDITION:
				ret= doCreate(graph);
				break;
			case REMOVAL:
				ret = doDelete(graph);
				break;
			default: throw new GraphActionException("No Action given.");
		}
		if ((Objecttype==GRAPH)||(Objecttype==SELECTION)) //Move Selection from old graph to new one
			exChangeSelection((VGraph)ActionObject,graph);			
		else if (Objecttype!=SUBSET) //SubSet has no env, so if no subset, update selection
			exChangeSelection(env,graph);
		return ret;
	}
	/**
	 * Apply the action to the Clone of a Graph. The given Parameter Graph is not manipulated but cloned
	 * The Clone then gets manipulated and is returned
	 * @param g A Graph
	 * @return The Manipulated Clone
	 */
	public VGraph redoActionOnClone(VGraph g) throws GraphActionException
	{
		return redoAction(g.clone());
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
		if (ActionObject==null)
			throw new GraphActionException("No Object available for the Action");
		VGraph ret;
		switch(Action) 
		{
			case UPDATE:  //Undo a replace is repace itself
				ret = doReplace(graph);
				break;
			case ADDITION: //Undo a Create is a delete
				ret = doDelete(graph);
				break;
			case REMOVAL: //Undo Delete is Create
				ret = doCreate(graph);
				break;
			default:
				throw new GraphActionException("No Action given");
		}
		if ((Objecttype==GRAPH)||(Objecttype==SELECTION)) //Move Selection from old graph to new one
			exChangeSelection((VGraph)ActionObject,ret);			
		else if (Objecttype!=SUBSET) //SubSet has no env, so if no subset, update selection
			exChangeSelection(env,ret);
		return ret;
	}
	/**
	 * Undo The Action on the copy of the given graph
	 * does the same as UndoAction, but on a clone, so the parameter Graph given is unchanged
	 * @param g
	 * @return
	 * @throws GraphActionException
	 */
	public VGraph UnDoActionOnClone(VGraph g) throws GraphActionException
	{
		return UnDoAction(g.clone());
	}
	
	/**
	 * Return Type of Action.
	 * @return
	 */
	public int getActionType()
	{
		return Action;
	}
}
