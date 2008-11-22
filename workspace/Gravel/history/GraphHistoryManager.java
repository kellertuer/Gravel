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
	public GraphHistoryManager(VGraph g)
	{
		trackedGraph = g;
		trackedGraph.addObserver(this);
		System.err.println("Added HistoryManager to the VGraph");
		lastMsg="";
	}
	public void update(Observable o, Object arg) {
		if (o.equals(trackedGraph))
		{
			if (!((String) arg).startsWith("M"))
			{
				if (!(lastMsg.equals((String) arg)))
				{
					String output = "Action tracked: "+arg;
					String msg = (String) arg;
					int value = 0;
					if (msg.length() > 1)
					{
						int j=1;
						while (Character.isDigit(msg.charAt(j)))
						{
							value = value*10+Character.getNumericValue(msg.charAt(j));
							if ( (++j) >= msg.length())
								break;
						}
						switch (msg.charAt(0))
						{
							case 'N': output+=" (A Node"; break;
							case 'E': output+=" (An Edge"; break;
							case 'S': output+=" (A Subset"; break;
						}
						output +=" with index #"+value+")";
					}
					lastMsg = msg;
					if (value > 0)
						System.err.println(output);

				}		
			}
		}
	}
}
