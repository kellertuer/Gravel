package history;

import model.*;
import model.Messages.GraphConstraints;
import model.Messages.GraphMessage;

import io.GeneralPreferences;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;

/**
 * This class tacks all actions happening to a graph and saves the last few ones.
 * Depending on GeneralPreferences, the 
 * - Number of Actions to be undo- or redoable may vary
 * - Tracking of Selection-Changes may be active or inactive
 * 
 * It also provides Undo(), Redo() and the methods to check their possibility
 * 
 * This Observer may only subscribe to Observables that sends GraphMessages.
 * @author Ronny Bergmann
 * @since 0.3
 */
public class CommonGraphHistoryManager implements Observer
{
	//Lastgraph is for the creation of actions, for delete especially
	protected VGraphInterface trackedGraph, lastGraph;
	protected GraphMessage Blockstart;
	boolean active, trackSelection;
	protected int blockdepth, stacksize, SavedUndoStackSize, graphType;
	protected LinkedList<CommonGraphAction> UndoStack, RedoStack;
	
	/**
	 * Create a new GraphHistoryManager for the given VGraph
	 * @param vg the Graph, that should be extended with a History
	 */
	public CommonGraphHistoryManager(VGraph vg)
	{
		trackedGraph = vg;
		lastGraph = vg.clone();
		CommonInitialization();
	}
	/**
	 * Create a new GraphHistoryManager for the given VHyperGraph
	 * @param vhg the HyperGraph, that should be extended with a History
	 */
	public CommonGraphHistoryManager(VHyperGraph vhg)
	{
		trackedGraph = vhg;
		lastGraph = vhg.clone();
		CommonInitialization();
	}

	protected void CommonInitialization()
	{
		if (trackedGraph.getType()==VGraphInterface.GRAPH)
		{
			((VGraph)trackedGraph).deleteObserver(this); //For not adding double
			((VGraph)trackedGraph).addObserver(this); 			
		}
		else if (trackedGraph.getType()==VGraphInterface.HYPERGRAPH)
		{
			((VHyperGraph)trackedGraph).deleteObserver(this); //For not adding double
			((VHyperGraph)trackedGraph).addObserver(this); 						
		}
		active=true;
		Blockstart=null;
		blockdepth=0;
		UndoStack = new LinkedList<CommonGraphAction>();
		RedoStack = new LinkedList<CommonGraphAction>();
		stacksize=GeneralPreferences.getInstance().getIntValue("history.Stacksize");
		trackSelection=GeneralPreferences.getInstance().getBoolValue("history.trackSelection");
		SavedUndoStackSize = 0;
	}
	/**
	 * Create an Action based on the message, that came from the Graph,
	 * return that Action and update LastGraph
	 * @param m the created Action
	 * @return
	 */
	protected CommonGraphAction handleSingleAction(GraphMessage m)
	{
		int action = 0;
		//Check, which Graph contains the Information for the information, we need to create an Action
		VGraphInterface InformationalGraph; //which Graph provides information for the action?
		switch(m.getModification()&(GraphConstraints.ACTIONMASK)) //Action must be status updates only (neither part nor block info
		{	case GraphConstraints.ADDITION: //Node added, Create action and modify lastgraph
				action = GraphConstraints.ADDITION;
				InformationalGraph = trackedGraph; //get Information from actual Graph;
				break;
			case GraphConstraints.REMOVAL:
				action = GraphConstraints.REMOVAL;
				InformationalGraph = lastGraph; //Information of the deleted Node must be taken from last status
				break;
			case GraphConstraints.INDEXCHANGED:
			case GraphConstraints.UPDATE:
			case GraphConstraints.TRANSLATION:
				action = GraphConstraints.UPDATE;
				InformationalGraph = lastGraph;
				break;
			default: //not suitable action
				return null;
		}		
		CommonGraphAction act = null;
		if (m.getModifiedElementTypes()==GraphConstraints.NODE) //Action on unique node
		{
			try { 
				if (InformationalGraph.getType()==VGraphInterface.GRAPH)	//TODO: Optimize temp.clone();
					act = new GraphAction(((VGraph)InformationalGraph).modifyNodes.get(m.getElementID()).clone(),action,((VGraph)InformationalGraph).clone()); 
				else if (InformationalGraph.getType()==VGraphInterface.HYPERGRAPH)	//TODO: Optimize temp.clone();
					act = new HyperGraphAction(((VHyperGraph)InformationalGraph).modifyNodes.get(m.getElementID()),action,((VHyperGraph)InformationalGraph).clone()); 
				else
					act=null;
			}
			catch (GraphActionException e)
			{
				act=null; System.err.println("DEBUG: Node (#"+m.getElementID()+") Action ("+action+") Failed:"+e.getMessage());
			}
		} //End Handling Node
		else if (m.getModifiedElementTypes()==GraphConstraints.EDGE) //Change on Unique Edge
		{
			try { 
				if (InformationalGraph.getType()!=VGraphInterface.GRAPH)
					act = null;
				//TODO: Optimize Environment Graph
				act = new GraphAction(((VGraph)InformationalGraph).modifyEdges.get(m.getElementID()).clone(),action,((VGraph)InformationalGraph).clone());
			}
			catch (GraphActionException e2)
			{
				act=null; System.err.println("DEBUG: ¢added.GraphAction Creation Failed:"+e2.getMessage());
			}
		}
		else if (m.getModifiedElementTypes()==GraphConstraints.HYPEREDGE) //Change on Unique HyperEdge
		{
			try { 
				if (InformationalGraph.getType()!=VGraphInterface.HYPERGRAPH)
					act = null;
				//TODO: Optimize Environment Graph
				act = new HyperGraphAction(((VHyperGraph)InformationalGraph).modifyHyperEdges.get(m.getElementID()).clone(),action,((VHyperGraph)InformationalGraph).clone());
			}
			catch (GraphActionException e2)
			{
				act=null; System.err.println("DEBUG: ¢added.GraphAction Creation Failed:"+e2.getMessage());
			}
		}
		else if (m.getModifiedElementTypes()==GraphConstraints.SUBGRAPH)
		{
			try { 
				if (InformationalGraph.getType()==VGraphInterface.GRAPH)	//TODO: Optimize temp.clone();
					act = new GraphAction(((VGraph)InformationalGraph).modifySubgraphs.get(m.getElementID()),action,((VGraph)InformationalGraph).getMathGraph().modifySubgraphs.get(m.getElementID())); 
				else if (InformationalGraph.getType()==VGraphInterface.HYPERGRAPH)	//TODO: Optimize temp.clone();
					act = new HyperGraphAction(((VHyperGraph)InformationalGraph).modifySubgraphs.get(m.getElementID()),action,((VHyperGraph)InformationalGraph).getMathGraph().modifySubgraphs.get(m.getElementID())); 
				else
					return null;
			}
			catch (GraphActionException e2)
			{
				act=null; System.err.println("DEBUG: Edge.added.GraphAction Creation Failed:"+e2.getMessage());
			}	
		}
		return act;
	}
	/**
	 * Updates the last Graph to the Selection of the tracked Graph
	 *
	 */
	private void updateLastSelection()
	{
		Iterator<VNode> ni;
		VNodeSet lastNodeSet;
		if (trackedGraph.getType()==VGraphInterface.GRAPH)
		{
			Iterator<VEdge> ei = ((VGraph)trackedGraph).modifyEdges.getIterator();
			while (ei.hasNext())
			{
				VEdge e = ei.next();
				VEdge e2 = ((VGraph)lastGraph).modifyEdges.get(e.getIndex());
				if (e2!=null)
					e2.setSelectedStatus(e.getSelectedStatus());
			}
			ni = ((VGraph)trackedGraph).modifyNodes.getIterator();
			lastNodeSet = ((VGraph)lastGraph).modifyNodes;
		}
		else if (trackedGraph.getType()==VGraphInterface.HYPERGRAPH)
		{
			Iterator<VHyperEdge> ei = ((VHyperGraph)trackedGraph).modifyHyperEdges.getIterator();
			while (ei.hasNext())
			{
				VHyperEdge e = ei.next();
				VHyperEdge e2 = ((VHyperGraph)lastGraph).modifyHyperEdges.get(e.getIndex());
				if (e2!=null)
					e2.setSelectedStatus(e.getSelectedStatus());
			}
			ni = ((VHyperGraph)trackedGraph).modifyNodes.getIterator();
			lastNodeSet = ((VHyperGraph)lastGraph).modifyNodes;
		}
		else
			return;
		while (ni.hasNext())
		{
			VNode n = ni.next();
			VNode n2 = lastNodeSet.get(n.getIndex());
			if (n2!=null)
				n2.setSelectedStatus(n.getSelectedStatus());
		}
	}
	/**
	 * Add Tracked Action to the Undo-Stuff
	 * Create an Action based on tracked Message
	 * @param m
	 */
	protected void addAction(GraphMessage m)	{	
		if ((m.getModification()&GraphConstraints.BLOCK_ABORT)==GraphConstraints.BLOCK_ABORT)
			return; //Don't handle Block-Abort-Stuff
		CommonGraphAction act = null;
		if (m.getElementID() > 0) //Message for single stuff thats not just selection
			act = handleSingleAction(m);
		else //multiple modifications, up to know just a replace */
		{
			//Last status in action - and yes there was an Change in the items
			try {						
				//New Action, that tracks the graph or only the selection
				if (lastGraph.getType()==VGraphInterface.GRAPH)
					act = new GraphAction((VGraph)lastGraph, GraphConstraints.UPDATE, m.getModifiedElementTypes()!=GraphConstraints.SELECTION); 
				else if (lastGraph.getType()==VGraphInterface.HYPERGRAPH)
					act = new HyperGraphAction((VHyperGraph)lastGraph, GraphConstraints.UPDATE, m.getModifiedElementTypes()!=GraphConstraints.SELECTION); 
				else
					throw new GraphActionException("Unknown Graph Type");
			}
			catch (GraphActionException e)
			{
				act=null; System.err.println("DEBUG: Edge.added.GraphAction Creation Failed:"+e.getMessage());				
			}
		}
	
		if (act==null)
			return;
		clonetrackedGraph();
		if (!RedoStack.isEmpty())
			RedoStack.clear();
		if (UndoStack.size()>=stacksize)
		{	//Now it can't get Unchanged by Undo
			SavedUndoStackSize--;
			UndoStack.remove();
		}
		UndoStack.add(act);
	}
	protected void clonetrackedGraph()
	{
		if (lastGraph.getType()==VGraphInterface.GRAPH)
			lastGraph = ((VGraph)trackedGraph).clone(); //Actual status as last status.clone
		else if (lastGraph.getType()==VGraphInterface.HYPERGRAPH)
			lastGraph = ((VHyperGraph)trackedGraph).clone(); //Actual status as last status.clone
	}
	/**
	 * (De)Activate Observation and with that the acvitiy of the History-Manager itself
	 * @param observing true if it should be set to active, else false
	 */
	public void setObservation(boolean observing)
	{
		if (lastGraph.getType()==VGraphInterface.GRAPH)
		{
			if (observing)
				((VGraph)trackedGraph).addObserver(this);
			else
				((VGraph)trackedGraph).deleteObserver(this);
		}
		else if (lastGraph.getType()==VGraphInterface.HYPERGRAPH)
		{
			if (observing)
				((VHyperGraph)trackedGraph).addObserver(this);
			else
				((VHyperGraph)trackedGraph).deleteObserver(this);
		}
	}
	/**
	 * Indicates, whether an undo is possible or not
	 * 
	 * @return true, if an undo is possible on the tracked Graph, else false
	 */
	public boolean CanUndo()
	{
		return !UndoStack.isEmpty();
	}
	/**
	 * Undo the Last Action, that happened to the Graph.
	 * This is only performed if CanUndo() is true, if it isn't nothiing happens
	 */
	public void Undo()
	{
		if (!CanUndo())
			return;
		setObservation(false); //Deaktivate Tracking - because the action pushes its undo as history action
		CommonGraphAction LastAction = UndoStack.removeLast();
		try{
			LastAction.UnDoAction(trackedGraph);
			clonetrackedGraph();
		}
		catch (GraphActionException e)
		{
			System.err.println("DEBUG: An Error Occured while Undoing an Action "+e.getMessage());
		}
		if (RedoStack.size()>=stacksize)
			RedoStack.remove();
		RedoStack.add(LastAction);
		setObservation(true); //Activate Tracking again
	}
	/**
	 * Indicate whether an Redo is possible
	 * 
	 * @return true, if an Redo is possible, else false
	 */
	public boolean CanRedo()
	{
		return !RedoStack.isEmpty();
	}
	/**
	 * Redo the Last Action, that was undone.
	 * This is only performed if CanRedo() is true, if it isn't nothing happens 
	 */	
	public void Redo()
	{
		if (!CanRedo())
			return;
		setObservation(false);//Deaktivate Tracking - because the action pushes a notification
		CommonGraphAction LastAction = RedoStack.removeLast();
		try{
			LastAction.redoAction(trackedGraph);
			clonetrackedGraph();
		}
		catch (GraphActionException e)
		{
			System.err.println("An error occured when Redoing an Action: "+e.getMessage());
		}
		if (UndoStack.size()>=stacksize)
		{	
			SavedUndoStackSize--;
			UndoStack.remove();
		}
		UndoStack.add(LastAction);
		setObservation(true); //Activate Tracking again
	}
	/**
	 * Is the Tracked Graph Unchanged since last 
	 * @return
	 */
	public boolean IsGraphUnchanged()
	{
		return (SavedUndoStackSize==UndoStack.size());
	}
	/**
	 * Set actual Graph as saved, this status is remembered, 
	 * if the undoManager comes back to it, the graph is indicated as saved again
	 *
	 */
	public void setGraphSaved()
	{
		setObservation(false);
		SavedUndoStackSize=UndoStack.size();
		//Notify everyone despite us.
		if (trackedGraph.getType()==VGraphInterface.GRAPH)
			trackedGraph.pushNotify(new GraphMessage(GraphConstraints.GRAPH_ALL_ELEMENTS, GraphConstraints.HISTORY));
		else if (trackedGraph.getType()==VGraphInterface.HYPERGRAPH)
			trackedGraph.pushNotify(new GraphMessage(GraphConstraints.HYPERGRAPH_ALL_ELEMENTS, GraphConstraints.HISTORY));			
		setObservation(true);
	}
	/**
	 * Indicates whether Selection Changes on the Graph are put on the Undo Stack or not
	 * @return true, if they are put on Stack, else false
	 */
	public boolean isSelectionTracked()
	{
		return trackSelection;
	}
	/**
	 * get the maximum Size of Undo- and RedoStack
	 * If one of them exceeds this size, the oldest action is discarded
	 * 
	 * This value can't be set directliy. It is set by changing the GeneralPreferences.
	 * The new value gets set when a new Graph is loaded (or an empty one created) 
	 *
	 * @return the size of the stacks
	 */
	public int getStackSize()
	{
		return stacksize;
	}

	public void update(Observable o, Object arg)
	{
		GraphMessage m = (GraphMessage)arg;
		if (m==null)
			return;
		if ((m.getModification()&GraphConstraints.HISTORY)>0) //Ignore them, they'Re from us 
			return;
		//Complete Replacement of Graphor Hypergraph Handling (Happens when loading a new graph
		if ( ((m.getModifiedElementTypes()==GraphConstraints.GRAPH_ALL_ELEMENTS)&&(m.getAffectedElementTypes()==GraphConstraints.GRAPH_ALL_ELEMENTS)&&(m.getModification()==GraphConstraints.REPLACEMENT))
				|| ((m.getModifiedElementTypes()==GraphConstraints.HYPERGRAPH_ALL_ELEMENTS)&&(m.getAffectedElementTypes()==GraphConstraints.HYPERGRAPH_ALL_ELEMENTS)&&(m.getModification()==GraphConstraints.REPLACEMENT)))
		{
				clonetrackedGraph();
				//Reinit all stuff because graph has changed
				CommonInitialization();
				return;
		}
		if ((m.getModifiedElementTypes()==GraphConstraints.SELECTION)&&(!trackSelection))
		{ //Reine Selection Veränderung, aber wir verfolgen sowas nicht -> Update LastGraph
			if (active) //not every update but on ends of blocks
				updateLastSelection();
		}
		else //Sonst erst auf BlockEnde prüfen
		{
			if ((m.getModification()&GraphConstraints.BLOCK_END)==GraphConstraints.BLOCK_END) //Block endet with this Message
			{
				if (blockdepth > 0)
					blockdepth--;
				//If We left Block #1 or never were in a Bock set active and give info about block
				if (blockdepth==0)
				{ //Block_END & not Aborted, so we have a Message to handle
					active = true;
					if (((m.getModification() & GraphConstraints.BLOCK_ABORT) != GraphConstraints.BLOCK_ABORT) && (Blockstart!=null))
					{
						addAction(Blockstart);
						if (trackedGraph.getType()==VGraphInterface.GRAPH)
							trackedGraph.pushNotify(new GraphMessage(GraphConstraints.GRAPH_ALL_ELEMENTS, GraphConstraints.HISTORY));
						else if (trackedGraph.getType()==VGraphInterface.HYPERGRAPH)
							trackedGraph.pushNotify(new GraphMessage(GraphConstraints.HYPERGRAPH_ALL_ELEMENTS, GraphConstraints.HISTORY));
					}
					else if(Blockstart!=null)
						Blockstart=null;					
				}
				return;
			}
			else if ((m.getModification()&GraphConstraints.BLOCK_START)==GraphConstraints.BLOCK_START)
			{
					blockdepth++;
				if (blockdepth==1)
				{
					active = false;
					Blockstart = m;
				}
				return;
			}
			if ((blockdepth>0)&&(Blockstart!=null)) //we are in a block
			{		//Node Translation or Edge Controlpoint Movement
				if (((Blockstart.getModifiedElementTypes()==GraphConstraints.NODE)&&((Blockstart.getModification()&GraphConstraints.TRANSLATION) > 0))
						|| ((Blockstart.getModifiedElementTypes()==GraphConstraints.EDGE)&&((Blockstart.getModification()&GraphConstraints.UPDATE) > 0)))
				{
					if ((m.getModifiedElementTypes()==(GraphConstraints.NODE|GraphConstraints.EDGE)) //while that the whole graph
					&&(m.getModification()==GraphConstraints.TRANSLATION)) //is translated
					{ //Don't do Node/Edge-Update anymore but graph replacement
//						System.err.println("Switching from Node replacement to graph replacement.");
						Blockstart = m;
					}
				}	
			}
			if (!active)
				return;
			//Single Action or Selection change
			if (m.getElementID() != 0)
				addAction(m);
			if (trackedGraph.getType()==VGraphInterface.GRAPH)
				trackedGraph.pushNotify(new GraphMessage(GraphConstraints.GRAPH_ALL_ELEMENTS, GraphConstraints.HISTORY));
			else if (trackedGraph.getType()==VGraphInterface.HYPERGRAPH)
				trackedGraph.pushNotify(new GraphMessage(GraphConstraints.HYPERGRAPH_ALL_ELEMENTS, GraphConstraints.HISTORY));
		}
	}
}
