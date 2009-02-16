package history;

import model.*;

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
 * @author Ronny Bergmann
 * @since 0.3
 */
public class GraphHistoryManager implements Observer
{
	//Lastgraph is for the creation of actions, for delete especially
	VGraph trackedGraph, lastGraph;
	GraphMessage Blockstart;
	boolean active = true, trackSelection;
	int blockdepth, stacksize, SavedUndoStackSize;
	LinkedList<GraphAction> UndoStack, RedoStack;
	
	/**
	 * Create a new GraphHistoryManager for the given VGraph
	 * @param vg the Graph, that should be extended with a History
	 */
	public GraphHistoryManager(VGraph vg)
	{
		trackedGraph = vg;
		lastGraph = trackedGraph.clone();
		trackedGraph.addObserver(this);
		Blockstart=null;
		blockdepth=0;
		UndoStack = new LinkedList<GraphAction>();
		RedoStack = new LinkedList<GraphAction>();
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
	private GraphAction handleSingleAction(GraphMessage m)
	{
		int action = 0;
		VGraph tempG; //which Graph provides information for the action?
		switch(m.getChangeStatus()&(~GraphMessage.BLOCK_ALL)) //Action must be spcific, therefore a switch but by ignoring Blocks (thats already handled)
		{	case GraphMessage.ADDITION: //Node added, Create action and modify lastgraph
				action = GraphAction.ADDITION;
				tempG = trackedGraph; //get Information from actual Graph;
				break;
			case GraphMessage.REMOVAL:
				action = GraphAction.REMOVAL;
				tempG = lastGraph; //Information of the deleted Node must be taken from last status
				break;
			case GraphMessage.UPDATE:
			case GraphMessage.TRANSLATION:
				action = GraphAction.UPDATE;
				tempG = lastGraph;
				break;
			default: //not suitable action
				return null;
		}		
		GraphAction act = null;
		if (m.getAction()==GraphMessage.NODE) //Action on unique node
		{
			try { 
				//TODO: Optimize temp.clone();
				act = new GraphAction(tempG.getNode(m.getElementID()),action,tempG.clone()); 
			}
			catch (GraphActionException e)
			{
				act=null; System.err.println("DEBUG: Node (#"+m.getElementID()+") Action ("+action+") Failed:"+e.getMessage());
			}
		} //End Handling Node
		else if (m.getAction()==GraphMessage.EDGE) //Change on Unique Edge
		{
			try { 
				//TODO: Optimize Environment Graph
				act = new GraphAction(tempG.getEdge(m.getElementID()),action,tempG.clone());
			}
			catch (GraphActionException e2)
			{
				act=null; System.err.println("DEBUG: ¢added.GraphAction Creation Failed:"+e2.getMessage());
			}
		}
		else if (m.getAction()==GraphMessage.SUBSET)
		{
			try { 
				//TODO: Optimize Environment.
				act = new GraphAction(tempG.getSubSet(m.getElementID()),action, tempG.getMathGraph().getSubSet(m.getElementID()),tempG.getSubSetName(m.getElementID()));
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
		Iterator<VNode> ni = trackedGraph.getNodeIterator();
		while (ni.hasNext())
		{
			VNode n = ni.next();
			VNode n2 = lastGraph.getNode(n.index);
			if (n2!=null)
				n2.setSelectedStatus(n.getSelectedStatus());
		}
		Iterator<VEdge> ei = trackedGraph.getEdgeIterator();
		while (ei.hasNext())
		{
			VEdge e = ei.next();
			VEdge e2 = lastGraph.getEdge(e.index);
			if (e2!=null)
				e2.setSelectedStatus(e.getSelectedStatus());
		}
	}
	/**
	 * Add Tracked Action to the Undo-Stuff
	 * Create an Action based on tracked Message
	 * @param m
	 */
	private void addAction(GraphMessage m)	{	
		if ((m.getChangeStatus()&GraphMessage.BLOCK_ABORT)==GraphMessage.BLOCK_ABORT)
			return; //Don't handle Block-Abort-Stuff
		GraphAction act = null;
		if (m.getElementID() > 0) //Message for single stuff thats not just selection
			act = handleSingleAction(m);
		else //multiple modifications, up to know just a replace */
		{
			//Last status in action - and yes there was an Change in the items
			try {						
				//New Action, that tracks the graph, or only the selection
				act = new GraphAction(lastGraph, GraphAction.UPDATE, m.getAction()!=GraphMessage.SELECTION); }
			catch (GraphActionException e)
			{
				act=null; System.err.println("DEBUG: Edge.added.GraphAction Creation Failed:"+e.getMessage());				
			}
		}
		lastGraph = trackedGraph.clone(); //Actual status as last status.clone
		if (act!=null)
		{ //Try to add to Stack, if its full discard oldest action
			//Because its a new action, delete Redo, if its filled
			if (!RedoStack.isEmpty())
				RedoStack.clear();
			if (UndoStack.size()>=stacksize)
			{	//Now it can't get Unchanged by Undo
				SavedUndoStackSize--;
				UndoStack.remove();
			}
			UndoStack.add(act);
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
		trackedGraph.deleteObserver(this); //Deaktivate Tracking
		GraphAction LastAction = UndoStack.removeLast();
		try{
			LastAction.UnDoAction(trackedGraph);
			lastGraph = trackedGraph.clone();
		}
		catch (GraphActionException e)
		{
			System.err.println("DEBUG: An Error Occured while Undoing an Action "+e.getMessage());
		}
		if (RedoStack.size()>=stacksize)
			RedoStack.remove();
		RedoStack.add(LastAction);
		trackedGraph.pushNotify(new GraphMessage(GraphMessage.ALL_ELEMENTS,GraphMessage.UPDATE));
		trackedGraph.addObserver(this); //Activate Tracking again
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
		trackedGraph.deleteObserver(this); //Deaktivate Tracking
		GraphAction LastAction = RedoStack.removeLast();
		try{
			LastAction.redoAction(trackedGraph);
			lastGraph = trackedGraph.clone();
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
		trackedGraph.pushNotify(new GraphMessage(GraphMessage.ALL_ELEMENTS,GraphMessage.UPDATE));
		trackedGraph.addObserver(this); //Activate Tracking again
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
		trackedGraph.deleteObserver(this);
		SavedUndoStackSize=UndoStack.size();
		//Notify everyone despite us.
		trackedGraph.pushNotify(new GraphMessage(GraphMessage.ALL_ELEMENTS, GraphMessage.UPDATE));
		trackedGraph.addObserver(this);		
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
		GraphMessage m = (GraphMessage) arg;
		if (m==null)
			return;
		//Complete Replacement of Graph Handling
		if ((m.getAction()==GraphMessage.ALL_ELEMENTS)&&(m.getAffectedTypes()==GraphMessage.ALL_ELEMENTS)&&(m.getChangeStatus()==GraphMessage.REPLACEMENT))
		{
				lastGraph = trackedGraph.clone();
				Blockstart=null;
				blockdepth=0;
				UndoStack = new LinkedList<GraphAction>();
				RedoStack = new LinkedList<GraphAction>();
				active=true;
				SavedUndoStackSize=0;
				//Reload Standards
				stacksize=GeneralPreferences.getInstance().getIntValue("history.Stacksize");
				trackSelection=GeneralPreferences.getInstance().getBoolValue("history.trackSelection");
				return;
		}
		if ((m.getAction()==GraphMessage.SELECTION)&&(!trackSelection))
		{ //Reine Selection Veränderung, aber wir verfolgen sowas nicht -> Update LastGraph
			if (active) //not every update but on ends of blocks
				updateLastSelection();
		}
		else //Sonst erst auf BlockEnde prüfen
		{
			if ((m.getChangeStatus()&GraphMessage.BLOCK_END)==GraphMessage.BLOCK_END) //Block endet with this Message
			{
				if (blockdepth > 0)
					blockdepth--;
				//If We left Block #1 or never were in a Bock set active and give info about block
				if (blockdepth==0)
				{ //Block_END & not Aborted, so we have a Message to handle
					active = true;
					if (((m.getChangeStatus() & GraphMessage.BLOCK_ABORT) != GraphMessage.BLOCK_ABORT) && (Blockstart!=null))
						addAction(Blockstart);
					else if(Blockstart!=null)
						Blockstart=null;					
				}
				return;
			}
			else if ((m.getChangeStatus()&GraphMessage.BLOCK_START)==GraphMessage.BLOCK_START)
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
				if (((Blockstart.getAction()==GraphMessage.NODE)&&((Blockstart.getChangeStatus()&GraphMessage.TRANSLATION) > 0))
						|| ((Blockstart.getAction()==GraphMessage.EDGE)&&((Blockstart.getChangeStatus()&GraphMessage.UPDATE) > 0)))
				{
					if ((m.getAction()==(GraphMessage.NODE|GraphMessage.EDGE)) //while that the whole graph
					&&(m.getChangeStatus()==GraphMessage.TRANSLATION)) //is translated
					{ //Don't do Node/Edge-Update anymore but graph replacement
						System.err.println("Switching from Node replacement to graph replacement.");
						Blockstart = m;
					}
				}	
			}
			if (!active)
				return;
			//Single Action or Selection change
			if (m.getElementID() != 0)
				addAction(m);
		}
	}
}
