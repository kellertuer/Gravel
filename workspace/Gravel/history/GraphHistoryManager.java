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
				type="Untergraphen";
			else
				type=" #"+(m.getAction()&127);
			return "Blockaktion auf "+type +"("+action+")";			
		}
		return "--unknown ("+m.toString()+") ---";
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
//					System.err.println("Block ended #"+(blockdepth)+".");	
					blockdepth--;
				}
				//If We left Block #1 or never were in a Bock set active and give info about block
				if (blockdepth==0)
				{
					active = true;
					if (((m.getChangeStatus() & GraphMessage.BLOCK_ABORT) != GraphMessage.BLOCK_ABORT) && (Blockstart!=null))
						System.err.println(getStatus(Blockstart)+" (Blockaktion)");
					else if(Blockstart!=null)
						System.err.println("Aborted Block");
					Blockstart=null;
					
				}
				return;
			}
			if ((m.getChangeStatus()&GraphMessage.BLOCK_START)==GraphMessage.BLOCK_START)
			{
					blockdepth++;
//					System.err.println("Block startet #"+(blockdepth)+".");	
				if (blockdepth==1)
				{
					active = false;
					Blockstart = m;
				}
				return;
			}			
			if (!active)
				return;
			if (m.getElementID() !=0) //Temporary Node not mentioning
				System.err.println(getStatus(m));			
		}
	}
	
}
