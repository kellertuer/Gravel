package control;

import java.awt.event.MouseEvent;

import view.VCommonGraphic;
import view.VGraphic;
import view.VHyperGraphic;

import model.*;
import model.Messages.*;
/**
 * Standard Cick Mouse Handler
 * 
 * Handles every special click for the standard mode
 * 
 * @author Ronny Bergmann
 *
 */
public class StandardClickMouseHandler extends ClickMouseHandler {
	
	private VGraph vg = null;
	private VHyperGraph vhg = null;
	private VCommonGraphic vgc;
	/**
	 * Initialize the given Standard Mode with an graph it is bound to 
	 * @param g
	 */
	public StandardClickMouseHandler(VGraphic g)
	{
		super(g);
		vgc = g;
		vg = g.getGraph();
	}

	public StandardClickMouseHandler(VHyperGraphic g)
	{
		super(g);
		vgc = g;
		vhg = g.getGraph();
	}

	public void mousePressed(MouseEvent e) {}

	public void mouseClicked(MouseEvent e) {}

	public void mouseEntered(MouseEvent e) {}

	public void mouseExited(MouseEvent e) {}

	public void mouseReleased(MouseEvent e) {}
}
