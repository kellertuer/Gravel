package history;

import model.*;
import java.util.Observable;
import java.util.Observer;

import view.VGraphic;

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
	VGraph trackedGraph;
	VGraphic vg;
	GraphMessage Blockstart;
	boolean active = true;
	int blockdepth;
	public GraphHistoryManager(VGraphic pvg)
	{
		trackedGraph = pvg.getVGraph();
		vg = pvg;
		trackedGraph.addObserver(this);
		System.err.println("Added HistoryManager to the VGraph");
		Blockstart=null;
		blockdepth=0;
	}
	private String getStatus(GraphMessage m)
	{
		if (m.getElementID() > 0)
		{
			String type = "unbekannt";
			if (m.getAction()==GraphMessage.NODE) //Unique because of ID:
				type="Knoten";
			else if (m.getAction()==GraphMessage.EDGE) //Unique because of ID:
				type="Kante";
			else if (m.getAction()==GraphMessage.SUBSET)
				type="Untergraph";
			String action = "";
			if (m.getChangeStatus() == GraphMessage.ADDED)
				action = "hinzugefügt";
			if (m.getChangeStatus() == GraphMessage.UPDATED)
				action = "verändert";
			if (m.getChangeStatus() == GraphMessage.REMOVED)
				action = "entfernt";
			return "Einzelaktion: "+type+" (ID #"+m.getElementID()+") "+action+".";
		}
		if ((m.getChangeStatus()&GraphMessage.START_BLOCK)==GraphMessage.START_BLOCK)
		{		
			String type="";
			if ((m.getAction()&GraphMessage.NODE)==GraphMessage.NODE) //nodes
				type="Knoten";
			else if ((m.getAction()&GraphMessage.EDGE)==GraphMessage.EDGE) //edges
				type="Kanten";
			else if ((m.getAction()&GraphMessage.SUBSET)==GraphMessage.SUBSET) //edges
				type="Untergraphen";			
			return "Blockaktion auf "+type;
		}
		return "--unknown ("+m.toString()+") ---";
	}
	public void update(Observable o, Object arg) 
	{
		GraphMessage m = (GraphMessage) arg;
		if (m==null)
			return;
		if (m.getAction()!=GraphMessage.SELECTION)
		{
			if ((m.getChangeStatus()&GraphMessage.END_BLOCK)==GraphMessage.END_BLOCK) //Block endet with this Message
			{
				if (blockdepth > 0)
					blockdepth--;
				if (blockdepth==0)
				{
					active = true;
					if (Blockstart!=null)
						System.err.println("Block was: "+getStatus(Blockstart));
					Blockstart=null;
				}
				return;
			}
			if ((m.getChangeStatus()&GraphMessage.START_BLOCK)==GraphMessage.START_BLOCK)
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
			System.err.println(getStatus(m));			
		}
	}
	
}
