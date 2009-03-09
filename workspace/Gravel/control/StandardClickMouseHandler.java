package control;


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
public class StandardClickMouseHandler extends ClickMouseHandler {
	
//	private VGraph vg = null;
//	private VHyperGraph vhg = null;
//	private VCommonGraphic vgc;
	/**
	 * Initialize the given Standard Mode with an graph it is bound to 
	 * @param g
	 */
	public StandardClickMouseHandler(VGraphic g)
	{
		super(g);
//		vgc = g;
//		vg = g.getGraph();
	}

	public StandardClickMouseHandler(VHyperGraphic g)
	{
		super(g);
//		vgc = g;
//		vhg = g.getGraph();
	}
}
