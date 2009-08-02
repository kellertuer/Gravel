package control;

import java.awt.event.MouseListener;
import java.util.Observer;

/**
 * Super class for the mouse handler for mouseclicks
 * this class is abstract, and is implemented by every mouse mode that is available in gravel
 * 
 * This abstract superclass is also an observer. it is an oberserv the VGraph
 * 
 * This Observer may only subscribe to Observables that send GraphMessages
 * @author ronny
 *
 */
public interface  ClickMouseHandler extends MouseListener, Observer
{
	/**
	 * Remove all Graph Listeners from the Graph
	 */
	public void removeGraphObservers();
}
