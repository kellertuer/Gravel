package control;

import io.GeneralPreferences;

import java.awt.Point;
import java.awt.event.MouseEvent;

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
	/**
	 * no mode special actions in the case the mouse button is pressed
	 */
	public void mousePressed(MouseEvent e) {}

	/**
	 * if you click on the background in this mode, a node is created
	 * 
	 */
	public void mouseClicked(MouseEvent e) 
	{
		super.mouseClicked(e);
		Point p = new Point(Math.round(e.getPoint().x/((float)gp.getIntValue("vgraphic.zoom")/100)),Math.round(e.getPoint().y/((float)gp.getIntValue("vgraphic.zoom")/100))); //rausrechnen
		if (e.getModifiers()==MouseEvent.BUTTON1_MASK) // Button 1/Links
		{
			VNode r = vg.getNodeinRange(p);
			if (r==null) 
			{	//Kein Knoten in der Nähe, also einen erstellen
				int i= vg.getNextNodeIndex();
				vg.addNode(new VNode(i,p.x,p.y, gp.getIntValue("node.size"), gp.getIntValue("node.name_distance"), gp.getIntValue("node.name_rotation"), gp.getIntValue("node.name_size"), gp.getBoolValue("node.name_visible")),gp.getNodeName(i));
				vg.deselect();
			}	
		}
		if (e.getModifiers() == MouseEvent.BUTTON1_MASK+MouseEvent.SHIFT_MASK) // mit SHIFTlinks angeklickt, Selektion erweitern
		{} //superclass handles them 
		else if ((e.getModifiers() == MouseEvent.BUTTON3_MASK) || (e.getModifiers() == MouseEvent.BUTTON1_MASK+MouseEvent.CTRL_MASK)) // mit rechts oder strg links
		{} //moved this part to super()
	}
	
	public void mouseEntered(MouseEvent e) {}

	public void mouseExited(MouseEvent e) {}

	public void mouseReleased(MouseEvent e) {}
}
