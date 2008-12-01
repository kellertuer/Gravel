package history;

import model.*;
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
	VGraph trackedGraph;
	String lastMsg;
	boolean active = true;
	public GraphHistoryManager(VGraph g)
	{
		trackedGraph = g;
		trackedGraph.addObserver(this);
		System.err.println("Added HistoryManager to the VGraph");
		lastMsg="";
	}
	public void update(Observable o, Object arg) 
	{
		GraphMessage m = (GraphMessage) arg;
		if ((m!=null)&&(m.getElements()!=GraphMessage.SELECTION))
		{
			if (m.getChangeStatus()==GraphMessage.START_BLOCK)
			{
				System.err.println("Started Block - deactivating");
				active = false;
				lastMsg = m.getMessage();
			}			
			if (m.getChangeStatus()==GraphMessage.START_BLOCK)
			{
				System.err.println("Ended Block - activating");
				active = false;
				System.err.println("Block was: "+lastMsg);
			}
			if (!active)
				return;
			if (m.getElementID() > 0)
			{
				String type = "unbekannt";
				if (m.getElements()==GraphMessage.NODE) //Unique because of ID:
					type="Knoten";
				else if (m.getElements()==GraphMessage.EDGE) //Unique because of ID:
					type="Kante";
				else if (m.getElements()==GraphMessage.SUBSET)
					type="Untergraph";
				String action = "";
				if (m.getChangeStatus() == GraphMessage.ADDED)
					action = "hinzugefügt";
				if (m.getChangeStatus() == GraphMessage.UPDATED)
					action = "verändert";
				if (m.getChangeStatus() == GraphMessage.REMOVED)
					action = "entfernt";
				System.err.println("Einzelaktion: "+type+" (ID #"+m.getElementID()+") "+action+".");
			}
		}
	}
}
