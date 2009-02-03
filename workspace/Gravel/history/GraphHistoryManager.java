package history;

import model.*;

import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;

/**
 * This class tacks all actions happening to a graph and saves the last few ones.
 * 
 * It also enables the undo and redo functions
 * 
 * @author Ronny Bergmann
 *
 */
public class GraphHistoryManager implements Observer
{
	//Lastgraph is for the creation of actions, for delete especially
	VGraph trackedGraph, lastGraph;
	GraphMessage Blockstart;
	boolean active = true;
	int blockdepth, stacksize;
	LinkedList<GraphAction> UndoStack, RedoStack;
	
	public GraphHistoryManager(VGraph vg)
	{
		trackedGraph = vg;
		lastGraph = trackedGraph.clone();
		trackedGraph.addObserver(this);
		Blockstart=null;
		blockdepth=0;
		UndoStack = new LinkedList<GraphAction>();
		RedoStack = new LinkedList<GraphAction>();
		stacksize=10;
	}
	public void ResetToNewGraph(VGraph vg)
	{
		trackedGraph = vg;
		lastGraph = trackedGraph.clone();
		trackedGraph.addObserver(this);
		Blockstart=null;
		blockdepth=0;
		UndoStack.clear();
		RedoStack.clear();
		stacksize=10;		
	}
	private String getStatus(GraphMessage m)
	{
		int id = m.getElementID();
		String action = "";
		if ((m.getChangeStatus() & GraphMessage.ADDED) == GraphMessage.ADDED)
			action = "hinzugefügt";
		else if ((m.getChangeStatus() & GraphMessage.UPDATED) == GraphMessage.UPDATED)
			action = "verändert";
		else if ((m.getChangeStatus() & GraphMessage.REMOVED) == GraphMessage.REMOVED)
			action = "entfernt";
		else 
			action = "Action #"+m.getChangeStatus();

		if (id > 0)
		{
			String type = "unbekannt";
			if (m.getAction()==GraphMessage.NODE) //Unique because of ID:
			{
				type="Knoten";
				if (trackedGraph.getNode(id)==null)
						type += " (failed)";
			}
			else if (m.getAction()==GraphMessage.EDGE) //Unique because of ID:
			{
				type="Kante";
				if (trackedGraph.getEdge(id)==null)
						type += " (failed)";
			}
			else if (m.getAction()==GraphMessage.SUBSET)
			{
				type="Untergraph";
				if (trackedGraph.getSubSet(id)==null)
					type += " (failed)";
			}
			return "Einzelaktion: "+type+" (ID #"+m.getElementID()+") "+action+".";
		}
		else if (((m.getAction() & (GraphMessage.NODE|GraphMessage.EDGE)) > 0) &&((m.getAction() & GraphMessage.SELECTION)==GraphMessage.SELECTION))
		{
			return "selektierte Knoten/Kanten "+action+".";
		}
		if ((m.getChangeStatus()&GraphMessage.BLOCK_START)==GraphMessage.BLOCK_START)
		{		
			String type="";
			if ((m.getAction()&GraphMessage.NODE)==GraphMessage.NODE)
				type="Knoten";
			else if ((m.getAction()&GraphMessage.EDGE)==GraphMessage.EDGE)
				type="Kanten";
			else if ((m.getAction()&GraphMessage.SUBSET)==GraphMessage.SUBSET) 
				type="Untergraphen";
			else if ((m.getAction()&GraphMessage.SELECTION)==GraphMessage.SELECTION) 
				type="Selektion";
			else
				type=" #"+(m.getAction()&127);
			return "Blockaktion auf "+type +"("+action+")";			
		}
		return "--unknown ("+m.toString()+") ---";
	}
	/**
	 * Create an Action based on the message, return that and update LastGraph for that
	 * @param m
	 * @return
	 */
	private GraphAction handleSingleAction(GraphMessage m)
	{
		int action = 0;
		VGraph tempG; //which Graph provides information for the action?
		switch(m.getChangeStatus()&0x1F) //Action must be spcific, no multiple, so switch not if and masks
		{								//But also ignore Block-Start&End Stuff
			case GraphMessage.ADDED: //Node added, Create action and modify lastgraph
				action = GraphAction.CREATE;
				tempG = trackedGraph; //get Information from actual Graph;
				break;
			case GraphMessage.REMOVED:
				action = GraphAction.DELETE;
				tempG = lastGraph; //Information of the deleted Node must be taken from last status
				break;
			case GraphMessage.REPLACED:
			case GraphMessage.UPDATED:
			case GraphMessage.TRANSLATED:
				action = GraphAction.REPLACE;
				tempG = lastGraph;
				break;
			default: //not suitable action
				return null;
		}		
		GraphAction act = null;
		if (m.getAction()==GraphMessage.NODE) //Action on unique node
		{
			try { 
				act = new GraphAction(tempG.getNode(m.getElementID()),action,tempG.getNodeName(m.getElementID())); 
				act.doAction(lastGraph);			
			}
			catch (GraphActionException e)
			{
				act=null; System.err.println("DEBUG: Node (#"+m.getElementID()+") Action ("+action+") Failed:"+e.getMessage());
			}
		} //End Handling Node
		else if (m.getAction()==GraphMessage.EDGE) //Change on Unique Edge
		{
			int s = tempG.getEdgeProperties(m.getElementID()).get(MGraph.EDGESTARTINDEX);
			int e = tempG.getEdgeProperties(m.getElementID()).get(MGraph.EDGEENDINDEX);
			int v = tempG.getEdgeProperties(m.getElementID()).get(MGraph.EDGEVALUE);
			try { 
				act = new GraphAction(tempG.getEdge(m.getElementID()),action,s,e,v,tempG.getEdgeName(m.getElementID()));
				act.doAction(lastGraph);
			}
			catch (GraphActionException e2)
			{
				act=null; System.err.println("DEBUG: Edge.added.GraphAction Creation Failed:"+e2.getMessage());
			}
		}
		else if (m.getAction()==GraphMessage.SUBSET)
		{
			try { 
				act = new GraphAction(tempG.getSubSet(m.getElementID()),action, tempG.getMathGraph().getSubSet(m.getElementID()),tempG.getSubSetName(m.getElementID()));
				act.doAction(lastGraph);
			}
			catch (GraphActionException e2)
			{
				act=null; System.err.println("DEBUG: Edge.added.GraphAction Creation Failed:"+e2.getMessage());
			}	
		}
		return act;
	}
	/**
	 * Add Tracked Action to the Undo-Stuff
	 * Create an Action based on tracked Message
	 * @param m
	 */
	private void addAction(GraphMessage m)
	{	
		if ((m.getChangeStatus()&GraphMessage.BLOCK_ABORT)==GraphMessage.BLOCK_ABORT)
			return; //Don't handle Block-Abort-Stuff
		GraphAction act = null;
		if (m.getElementID() > 0) //Message for single stuff
		{
			act = handleSingleAction(m);
		}
		else //multiple modifications, up to know just a replace
		{
			//Last status in action - Do i have to clone that?
			try {	act = new GraphAction(lastGraph, GraphAction.REPLACE); }
			catch (GraphActionException e)
			{
				act=null; System.err.println("DEBUG: Edge.added.GraphAction Creation Failed:"+e.getMessage());				
			}
			lastGraph = trackedGraph.clone(); //Actual status as last status.clone
		}
		if (act!=null)
		{ //Try to add to Stack, if its full discard oldest action
			//Because its a new action, delete Redo, if its filled
			if (!RedoStack.isEmpty())
				RedoStack.clear();
			if (UndoStack.size()>=stacksize)
				UndoStack.remove();
			UndoStack.add(act);
		}
		else
			System.err.println("Status:"+getStatus(m)+", I was not able to create Action.");
	}
	
	public boolean CanUndo()
	{
		return !UndoStack.isEmpty();
	}
	public void Undo()
	{
		if (!CanUndo())
			return;
		trackedGraph.deleteObserver(this); //Deaktivate Tracking
		System.err.print("Stacksize:"+stacksize+" UndoStack:"+UndoStack.size()+" RedoStack:"+RedoStack.size()+"- Undoing Action.");
		GraphAction LastAction = UndoStack.removeLast();
		try{
			trackedGraph.replace(LastAction.UnDoAction(trackedGraph));
			lastGraph = trackedGraph.clone();
		}
		catch (GraphActionException e)
		{
			System.err.println("Argh"+e.getMessage());
		}
		if (RedoStack.size()>=stacksize)
			RedoStack.remove();
		RedoStack.add(LastAction);
		System.err.println("After that:"+stacksize+" UndoStack:"+UndoStack.size()+" RedoStack:"+RedoStack.size()+".");
		trackedGraph.addObserver(this); //Activate Tracking again
	}
	
	public boolean CanRedo()
	{
		return !RedoStack.isEmpty();
	}
	public void Redo()
	{
		if (!CanRedo())
			return;
		trackedGraph.deleteObserver(this); //Deaktivate Tracking
		System.err.print("Stacksize:"+stacksize+" UndoStack:"+UndoStack.size()+" RedoStack:"+RedoStack.size()+"- Redoing Action.");
		GraphAction LastAction = RedoStack.removeLast();
		try{
		LastAction.doAction(trackedGraph);
		LastAction.doAction(lastGraph);
		}
		catch (GraphActionException e)
		{
			System.err.println("Argh"+e.getMessage());
		}
		if (UndoStack.size()>=stacksize)
			UndoStack.remove();
		UndoStack.add(LastAction);
		System.err.println("After that:"+stacksize+" UndoStack:"+UndoStack.size()+" RedoStack:"+RedoStack.size()+".");
		trackedGraph.addObserver(this); //Activate Tracking again
	}
	public void update(Observable o, Object arg) 
	{
		GraphMessage m = (GraphMessage) arg;
		if (m==null)
			return;
		if (m.getAction()!=GraphMessage.SELECTION) //nicht nur selektion
		{
			if ((m.getChangeStatus()&GraphMessage.BLOCK_END)==GraphMessage.BLOCK_END) //Block endet with this Message
			{
				if (blockdepth > 0)
				{
					blockdepth--;
				}
				//If We left Block #1 or never were in a Bock set active and give info about block
				if (blockdepth==0)
				{
					active = true;
					if (((m.getChangeStatus() & GraphMessage.BLOCK_ABORT) != GraphMessage.BLOCK_ABORT) && (Blockstart!=null))
					{
						/*
						 * Handle Block Action here 
						 */
						addAction(Blockstart);
					}
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
			if (!active)
				return;
			if (m.getElementID() != 0) //Temporary Node not mentioning
			{
				//System.err.println(getStatus(m));			
				addAction(m);
			}
		}
	}
	
}
