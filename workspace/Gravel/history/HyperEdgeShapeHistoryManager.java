package history;

import model.*;
import model.Messages.GraphConstraints;
import model.Messages.GraphMessage;
import model.Messages.NURBSCreationMessage;

import java.awt.geom.Point2D;
import java.util.Observable;
import java.util.Vector;

import view.VCommonGraphic;
import view.VHyperShapeGraphic;

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
public class HyperEdgeShapeHistoryManager extends CommonGraphHistoryManager
{	
	private VHyperShapeGraphic ParameterVectorReference;
	private int VHyperEdgeIndex;
	/**
	 * Create a new HyperGraphShapeHistoryManager for the given VHyperGraph
	 * To track changes of the first Modus of shape creation 
	 * The second modus is tracked by the method already implemented in the
	 * CommonGraphHistoryManager from which they are passed on to this class
	 * 
	 * @param vhg the HyperGraph, that should be extended with a History
	 * @param vhsg VHyperShapeGraphic which keeps the last Parametervector for creation of shape
	 */
	public HyperEdgeShapeHistoryManager(VHyperShapeGraphic vhsg, int vheIndex)
	{
		super(vhsg.getGraph());
		trackedGraph = vhsg.getGraph();
		lastGraph = vhsg.getGraph().clone();
		ParameterVectorReference = vhsg;
		CommonInitialization();
		VHyperEdgeIndex=vheIndex;
	}
	
	public void Undo()
	{
		if (super.CanUndo())
		{
			super.Undo();
			this.setObservation(false);
			if (UndoStack.size()==0)
			{
				//Either the Shape Editing was started with a shape or not - in both cases reset to initial stuff
				ParameterVectorReference.setShapeParameters(null);
				ParameterVectorReference.repaint();
				this.setObservation(true);
				return;
			}
			CommonGraphAction status = UndoStack.getLast(); //Last pushed element is the action before the just undone action, so the actual status
			CommonGraphAction previousStatus = RedoStack.getLast();
			if (status instanceof HyperEdgeShapeAction) //We should be in modus 1
			{
				NURBSCreationMessage nm = (NURBSCreationMessage) ((HyperEdgeShapeAction)status).ActionObject;
				ParameterVectorReference.setShapeParameters(nm.clone());
			}
			else if ((!(status instanceof HyperEdgeShapeAction)) && (previousStatus instanceof HyperEdgeShapeAction))
			{ //Last action was modus 1 status is modus 2
				System.err.println("Change to modus 2, because we're in 1,");
				//Send invalid message
				ParameterVectorReference.setShapeParameters(new NURBSCreationMessage());			
			}
			this.setObservation(true);
		}
		
	}

	public void Redo()
	{
		if (super.CanRedo())
		{
			super.Redo();
			CommonGraphAction act = UndoStack.getLast();
			if (act instanceof HyperEdgeShapeAction)
			{
				this.setObservation(false);
				NURBSCreationMessage nm = (NURBSCreationMessage) ((HyperEdgeShapeAction)act).ActionObject;
				ParameterVectorReference.setShapeParameters(nm.clone());
				this.setObservation(true);
			}
			else //TODO: react on changes of normal Graph and change to second modus if the actual status before was creational
			{
				if (UndoStack.size()<2) //there is no change to be fond of
					return;
				this.setObservation(false);
				//I'm coming from the status that is now undo(undo())
				CommonGraphAction status = UndoStack.getLast(); //Last pushed element is the action before the just undone action, so the actual status
				CommonGraphAction previousStatus = UndoStack.get(UndoStack.size()-2);
				if (status instanceof HyperEdgeShapeAction) //We should be in modus 1
				{
					//Because that should be valid the change here happens automatically
					NURBSCreationMessage nm = (NURBSCreationMessage) ((HyperEdgeShapeAction)status).ActionObject;
					ParameterVectorReference.setShapeParameters(nm.clone());
				}
				else if ((!(status instanceof HyperEdgeShapeAction)) && (previousStatus instanceof HyperEdgeShapeAction))
				{ //Last action was modus 1 status is modus 2
					System.err.println("Change to modus 2, because we're in 1, (invalidate");				
					ParameterVectorReference.setShapeParameters(new NURBSCreationMessage());
				}
				this.setObservation(true);
			}
		}
	}
	/**
	 * Create an Action based on the message, that came from the Graph,
	 * return that Action and update LastGraph
	 * @param m the created Action
	 * @return
	 */
	private CommonGraphAction handleSingleAction(GraphMessage m, NURBSCreationMessage nm)
	{
		int noBlockMod = m.getModification()&(GraphConstraints.ACTIONMASK|GraphConstraints.PARTINFORMATIONMASK);
		if (noBlockMod==(GraphConstraints.UPDATE|GraphConstraints.HYPEREDGESHAPE|GraphConstraints.CREATION))
		{ //Only case handled here, HyperEdgeShapeCreation
			try {
			return new HyperEdgeShapeAction(
				nm, //These Parameters
				((m.getModification()&GraphConstraints.LOCAL) == GraphConstraints.LOCAL), //Local Action?
				((VHyperGraph)lastGraph).modifyHyperEdges.get(m.getElementID()),
				m.getModification(), //All known action stuff
				(VHyperGraph)lastGraph);
			}
			catch (GraphActionException e)
			{
				System.err.println("DEBUG: HyperEdgeshape (#"+m.getElementID()+") Action ("+m.getModification()+") Failed:"+e.getMessage());
				return null;
			}
		} //else handle as normal
		return super.handleSingleAction(m);
	}
	/**
	 * Add Tracked Action to the Undo-Stuff
	 * Create an Action based on tracked Message
	 * @param m
	 */
	private void addAction(GraphMessage m, NURBSCreationMessage nm)	{	
		if ((m.getModification()&GraphConstraints.BLOCK_ABORT)==GraphConstraints.BLOCK_ABORT)
			return; //Don't handle Block-Abort-Stuff
		CommonGraphAction act = null;
		if (m.getElementID() > 0) //Message for single stuff thats the only case of our interest
		{
			act = handleSingleAction(m,nm);	
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
		else //multiple modifications, up to know just a replace */
			super.addAction(m);
	}


	public void update(Observable o, Object arg)
	{
		GraphMessage temp = null;
		if (Blockstart!=null)
			temp = Blockstart.clone();
		//Handle normal stuff
		super.update(o, arg);
		
		GraphMessage m = (GraphMessage)arg;
		if (m==null)
			return;
		if ((m.getModification()&GraphConstraints.HISTORY)>0) //Ignore them, they'Re from us
			return;
		//Complete Replacement of Graphor Hypergraph Handling (Happens when loading a new graph
		GraphMessage actualAction;
		if ((blockdepth==0)&&(active) //super.update ended a block or we are active either way
		   && 
		   ( ((m.getModification()&GraphConstraints.BLOCK_END)==GraphConstraints.BLOCK_END))
		   || ((m.getModification()&GraphConstraints.BLOCK_ABORT)==GraphConstraints.BLOCK_ABORT))
		{ //Then a block ended
			actualAction = temp;
		}
		else
			actualAction = m;
		if (((m.getModification() & GraphConstraints.BLOCK_ABORT)==GraphConstraints.BLOCK_ABORT) || (temp==null) || (!active))
			return;
		int noBlockMod = actualAction.getModification() & (GraphConstraints.ACTIONMASK|GraphConstraints.PARTINFORMATIONMASK);
		if ((actualAction.getModifiedElementTypes()==GraphConstraints.HYPEREDGE)
			&& (noBlockMod == (GraphConstraints.UPDATE|GraphConstraints.HYPEREDGESHAPE|GraphConstraints.CREATION))
			&& (actualAction.getElementID() != 0)
			&& (actualAction.getAffectedElementTypes() ==GraphConstraints.HYPEREDGE))
		{// The type of action we want to track here - than it was tracked wrong before
			UndoStack.removeLast(); //Undo the undo-push from superclass and handle seperately
			addAction(actualAction, ParameterVectorReference.getShapeParameters()); //Do our action upon that
			//Update Menu, 
			trackedGraph.pushNotify(new GraphMessage(GraphConstraints.HYPERGRAPH_ALL_ELEMENTS, GraphConstraints.HISTORY));
		}
	}
}
