package control;

import io.GeneralPreferences;

import java.awt.Point;
import java.awt.event.MouseEvent;

import model.MNode;
import model.VGraph;
import model.VNode;
/**
 * Handling left click on background to create nodes - all other click actions are in the superclass
 * 
 * 
 * @author Ronny Bergmann
 */
public class OCMClickMouseHandler extends ClickMouseHandler {
	
	private VGraph vg;
	private GeneralPreferences gp;
		
	public OCMClickMouseHandler(VGraph g)
	{
		super(g);
		vg = g;
		gp = GeneralPreferences.getInstance();
	}
	
	/*
	 * Mouse Listener fuer Tastenaktionen
	 */
	public void mousePressed(MouseEvent e) {}
	public void mouseClicked(MouseEvent e) 
	{
		super.mouseClicked(e);
		Point p = new Point(Math.round(e.getPoint().x/((float)gp.getIntValue("vgraphic.zoom")/100)),Math.round(e.getPoint().y/((float)gp.getIntValue("vgraphic.zoom")/100))); //rausrechnen
		if (e.getModifiers()==MouseEvent.BUTTON1_MASK) // Button 1/Links
		{
			VNode r = vg.modifyNodes.getNodeinRange(p);
			if (r==null) 
			{	//Kein Knoten in der Nähe, also einen erstellen
				int i= vg.getMathGraph().getNextNodeIndex();
				//TODO: Semantisch nochmal überlegen, ob die Auswahl entfertn werden soll, so ein neuer Knoten erstellt wird
				if ((vg.modifyEdges.selectedEdgeExists()||vg.modifyNodes.selectedNodeExists()))
					vg.deselect();
				vg.modifyNodes.addNode(new VNode(i,p.x,p.y, gp.getIntValue("node.size"), gp.getIntValue("node.name_distance"), gp.getIntValue("node.name_rotation"), gp.getIntValue("node.name_size"), gp.getBoolValue("node.name_visible")), new MNode(i,gp.getNodeName(i)));
			}	
		}
	}	
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}
}