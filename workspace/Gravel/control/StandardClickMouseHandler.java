package control;


import java.awt.event.MouseEvent;
import java.util.Observable;

import view.*;

// import model.*;
/**
 * Standard Cick Mouse Handler
 * 
 * Handles every special click for the standard mode
 * Actually there is no Special Click ATM, but if there is...it would be here
 * Either Graph or Hypergraph possible.
 * 
 * @author Ronny Bergmann
 *
 */
public class StandardClickMouseHandler implements ClickMouseHandler {
	
	CommonNodeClickListener NodeMouseActions;
	CommonEdgeClickListener EdgeMouseActions;
	SelectionClickListener SelectionMouseActions;
	ContextMenuClickListener PopupClickActions;

	/**
	 * Initialize the given Standard Mode with an graph it is bound to 
	 * @param g
	 */
	public StandardClickMouseHandler(VGraphic g)
	{
		NodeMouseActions = new CommonNodeClickListener(g);
		EdgeMouseActions = new CommonEdgeClickListener(g);
		SelectionMouseActions = new SelectionClickListener(g);;
		PopupClickActions = new ContextMenuClickListener(g);
	}

	public StandardClickMouseHandler(VHyperGraphic g)
	{
		NodeMouseActions = new CommonNodeClickListener(g);
		EdgeMouseActions = new CommonEdgeClickListener(g);
		SelectionMouseActions = new SelectionClickListener(g);;
		PopupClickActions = new ContextMenuClickListener(g);
	}
	public void removeGraphObservers()
	{
		if (PopupClickActions!=null)
			PopupClickActions.removeObservers();
	}

	public void mouseClicked(MouseEvent e)
	{
		NodeMouseActions.mouseClicked(e);
		EdgeMouseActions.mouseClicked(e);
		SelectionMouseActions.mouseClicked(e);
		PopupClickActions.mouseClicked(e);
	}

	public void update(Observable o, Object arg)
	{
		PopupClickActions.update(o, arg);
	}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e)
	{
		PopupClickActions.mousePressed(e);
	}
	public void mouseReleased(MouseEvent e)
	{
		PopupClickActions.mouseReleased(e);
	}
}
