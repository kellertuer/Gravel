package control;

import io.GeneralPreferences;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.Observable;

import view.VCommonGraphic;
import view.VGraphic;
import view.VHyperGraphic;

import model.MNode;
import model.VGraph;
import model.VHyperGraph;
import model.VNode;
/**
 * Additionally to the ClickMouseHandler
 * 
 * his Class handles left Click on Background as Node Creation in V(Hyper)Graphs
 * 
 * @author Ronny Bergmann
 */
public class OCMClickMouseHandler implements ClickMouseHandler {
	
	private VGraph vg=null;
	private VHyperGraph vhg = null;
	private VCommonGraphic vgc;
	private GeneralPreferences gp;
	
	CommonNodeClickListener NodeMouseActions;
	SelectionClickListener SelectionMouseActions;
	ContextMenuClickListener PopupClickActions;

		
	public OCMClickMouseHandler(VGraphic g)
	{
		NodeMouseActions = new CommonNodeClickListener(g);
		SelectionMouseActions = new SelectionClickListener(g);;
		PopupClickActions = new ContextMenuClickListener(g);
		vgc = g;
		vg = g.getGraph();
		gp = GeneralPreferences.getInstance();
	}
	
	public OCMClickMouseHandler(VHyperGraphic g)
	{
		NodeMouseActions = new CommonNodeClickListener(g);
		SelectionMouseActions = new SelectionClickListener(g);;
		PopupClickActions = new ContextMenuClickListener(g);
		vgc = g;
		vhg = g.getGraph();
		gp = GeneralPreferences.getInstance();
	}
	/*
	 * Mouse Listener fuer Tastenaktionen
	 */
	public void mousePressed(MouseEvent e) {}
	public void mouseClicked(MouseEvent e) 
	{
		NodeMouseActions.mouseClicked(e);
		SelectionMouseActions.mouseClicked(e);
		PopupClickActions.mouseClicked(e);
		Point pointInGraph = new Point(Math.round(e.getPoint().x/((float)vgc.getZoom()/100)),Math.round(e.getPoint().y/((float)vgc.getZoom()/100)));
		VNode r=null;
		if (vg!=null) //Normal Graph
			r = vg.modifyNodes.getFirstinRangeOf(pointInGraph);
		else if (vhg!=null) //Hypergraph
			r = vhg.modifyNodes.getFirstinRangeOf(pointInGraph);
		else
			return;

		Point p = new Point(Math.round(e.getPoint().x/((float)vgc.getZoom()/100)),Math.round(e.getPoint().y/((float)vgc.getZoom()/100))); //rausrechnen
		if (e.getModifiers()==MouseEvent.BUTTON1_MASK) // Button 1/Links
		{
			if (r==null) 
			{	//Kein Knoten in der Nähe, also einen erstellen
				if (vg!=null)
				{
					int i = vg.getMathGraph().modifyNodes.getNextIndex();
					vg.modifyNodes.add(new VNode(i,p.x,p.y, gp.getIntValue("node.size"), gp.getIntValue("node.name_distance"), gp.getIntValue("node.name_rotation"), gp.getIntValue("node.name_size"), gp.getBoolValue("node.name_visible")), new MNode(i,gp.getNodeName(i)));
				}
				else if (vhg!=null)
				{
					int i = vhg.getMathGraph().modifyNodes.getNextIndex();
					vhg.modifyNodes.add(new VNode(i,p.x,p.y, gp.getIntValue("node.size"), gp.getIntValue("node.name_distance"), gp.getIntValue("node.name_rotation"), gp.getIntValue("node.name_size"), gp.getBoolValue("node.name_visible")), new MNode(i,gp.getNodeName(i)));
				}
				else
					return;
			}	
		}
	}	
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}
	
	public void update(Observable o, Object arg)
	{
		PopupClickActions.update(o, arg);
	}	
}