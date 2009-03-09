package control;

import io.GeneralPreferences;

import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Iterator;

import model.*;
import model.Messages.GraphConstraints;
import model.Messages.GraphMessage;
import view.*;
/**
 * This Class handles Drag Actions on Nodes that are common to alle modes
 * this includes all 
 * ALt- & Alt-Shift-Drags that modify the TextPosition
 * 
 * @author ronny
 *
 */
public class CommonNodeDragListener
		implements
			MouseListener,
			MouseMotionListener {

	VNode movingNode;
	VNodeSet nodes;
	VGraph vg=null;
	VHyperGraph vhg=null;
	VCommonGraphic vgc;
	GeneralPreferences gp;
	Point MouseOffSet;
	boolean multiplemoving=false,altwaspressed = false,shiftwaspressed = false, firstdrag;

	public CommonNodeDragListener(VGraphic g)
	{
		vgc = g;
		vg = g.getGraph();
		gp = GeneralPreferences.getInstance();
		MouseOffSet = new Point(0,0);
	}

	public CommonNodeDragListener(VHyperGraphic g)
	{
		vgc = g;
		vhg = g.getGraph();
		gp = GeneralPreferences.getInstance();
		MouseOffSet = new Point(0,0);
	}
	/**
	 * reset all values at the end of a movement/drag
	 *
	 */
	private void reset()
	{
		
		if (!firstdrag)//We had a Drag: End Block
		{
			VGraphInterface notify=null;
			if (vg!=null) //Normal Graph
				notify = vg;
			else if (vhg!=null) //Hypergraph
				notify=vhg;
			else
				return;
			if (movingNode!=null) //Single Node Handled
				notify.pushNotify(new GraphMessage(GraphConstraints.NODE,GraphConstraints.BLOCK_END));
			else if (multiplemoving) //Multiple (really!) Handled
				notify.pushNotify(new GraphMessage(GraphConstraints.EDGE|GraphConstraints.NODE,GraphConstraints.BLOCK_END));
		}
		movingNode=null;
		altwaspressed=false;
		shiftwaspressed=false;
		multiplemoving = false;
		firstdrag = true;
	}
	public boolean dragged()
	{
		return (movingNode!=null)||(multiplemoving);
	}
	/**
	 * Help method for moving selected nodes - if they touch the border of the area (e.g. some values are below 0 after a movement) the whole graph is moved the opposite direction
	 * 
	 * @param x
	 * @param y
	 */
	private void moveSelNodes(int x,int y)
	{
		Iterator<VNode> nodeiter=null;
		if (vg!=null)
			nodeiter = vg.modifyNodes.getIterator();
		else if (vhg!=null)
			nodeiter = vhg.modifyNodes.getIterator();
		else
			return;
		while (nodeiter.hasNext()) // drawNodes
		{
			VNode temp = nodeiter.next();
			if (((temp.getSelectedStatus()&VItem.SELECTED)==VItem.SELECTED)&&(temp.isNameVisible()))
			{
				int rotation = (temp.getNameRotation()+x*2)%360;
				if (rotation < 0)
					rotation +=360;
				int distance = (temp.getNameDistance()+y);
				if (distance<0)
					distance = 0;
				temp.setNameDistance(distance); //Y Richtung setzt Distance
				temp.setNameRotation(rotation); //X Setzt Rotation
			}
		}
	}
	
	public void mousePressed(MouseEvent e) {
		firstdrag=true;
		
		MouseOffSet = e.getPoint(); //Aktuelle Position merken für eventuelle Bewegungen while pressed
		Point pointInGraph = new Point(Math.round(e.getPoint().x/((float)vgc.getZoom()/100)),Math.round(e.getPoint().y/((float)vgc.getZoom()/100))); //Rausrechnen des zooms

		VNode nodeInRange=null;
		if (vg!=null) //Normal Graph
		{
			nodeInRange = vg.modifyNodes.getFirstinRangeOf(pointInGraph);
		}
		else if (vhg!=null) //Hypergraph
		{
			nodeInRange = vhg.modifyNodes.getFirstinRangeOf(pointInGraph);
		}
		else
			return; //No graph is !=null
		
		boolean alt = ((InputEvent.ALT_DOWN_MASK & e.getModifiersEx()) == InputEvent.ALT_DOWN_MASK); // alt ?
		boolean shift = ((InputEvent.SHIFT_DOWN_MASK & e.getModifiersEx()) == InputEvent.SHIFT_DOWN_MASK); //shift ?
		
		if ((alt)&&(!shift))
		{ //Alt and not shift
			movingNode = nodeInRange; //kein Shift == moving Node merken, sonst werden alle selected Bewegt
			altwaspressed = true;
		}
		else if ((alt)&&(shift))
		{
			//Shift and Alt Moving multiple Item-Texts
			shiftwaspressed=true;
			altwaspressed = true;
			//Node in Range must be selected
			if ((nodeInRange!=null)&&((nodeInRange.getSelectedStatus()&VItem.SELECTED)==VItem.SELECTED))
				multiplemoving = true;
		}
	}
	public void mouseDragged(MouseEvent e) {
		Point movement = new Point(e.getPoint().x-MouseOffSet.x, e.getPoint().y-MouseOffSet.y);
		int horizontalMovInGraph = Math.round(movement.x/((float)vgc.getZoom()/100)); //Zoom rausrechnen
		int verticalMovInGraph = Math.round(movement.y/((float)vgc.getZoom()/100));

		if ((altwaspressed)&&!(((InputEvent.ALT_DOWN_MASK & e.getModifiersEx()) == InputEvent.ALT_DOWN_MASK)))
		{ //Drag begun with alt and was released
			reset();
			return;
		}
		if ((shiftwaspressed)&&!(((InputEvent.SHIFT_DOWN_MASK & e.getModifiersEx()) == InputEvent.SHIFT_DOWN_MASK)))
		{ //Drag begun with shift and released
			reset();
			return;
		}
		VGraphInterface notify=null;
		if (vg!=null) //Normal Graph
			notify = vg;
		else if (vhg!=null) //Hypergraph
			notify=vhg;

		if ((InputEvent.ALT_DOWN_MASK & e.getModifiersEx()) == InputEvent.ALT_DOWN_MASK)
		//alt&drag auf nem Knoten oder einer Kante begonnen
		{
			if ((movingNode!=null)&&(movingNode.isNameVisible()))
			{ //SingleNode
				int rotation = (movingNode.getNameRotation()+horizontalMovInGraph*4)%360;
				if (rotation < 0)
					rotation +=360;
				int distance = (movingNode.getNameDistance()+verticalMovInGraph);
				if (distance<0)
					distance = 0;
				movingNode.setNameDistance(distance); //Y Richtung setzt Distance
				movingNode.setNameRotation(rotation); //X Setzt Rotation
				if (firstdrag) //Begin drag with a Block-Notification
					notify.pushNotify(new GraphMessage(GraphConstraints.NODE,GraphConstraints.BLOCK_START|GraphConstraints.UPDATE));
				else		
					notify.pushNotify(new GraphMessage(GraphConstraints.NODE,GraphConstraints.UPDATE));
			}
			else if (((InputEvent.SHIFT_DOWN_MASK & e.getModifiersEx()) == InputEvent.SHIFT_DOWN_MASK)&&(multiplemoving))
			{
				moveSelNodes(horizontalMovInGraph,verticalMovInGraph);
				if (firstdrag) //Begin drag with a Block Start Notification
					notify.pushNotify(new GraphMessage(GraphConstraints.SELECTION|GraphConstraints.NODE|GraphConstraints.EDGE,GraphConstraints.BLOCK_START|GraphConstraints.UPDATE));
				else		
					notify.pushNotify(new GraphMessage(GraphConstraints.SELECTION|GraphConstraints.NODE|GraphConstraints.EDGE,GraphConstraints.UPDATE));				
			}
		} //End handling ALT
		MouseOffSet = e.getPoint();
		firstdrag = false;
	}

	public void mouseReleased(MouseEvent e) {
		if (!firstdrag)
		{
			if (!((e.getPoint().x==-1)||(e.getPoint().y==-1))) //kein Reset von außerhalb wegen modusumschaltung
			mouseDragged(e); //Das gleiche wie als wenn man bewegt, nur ist danach kein Knoten mehr bewegter Knoten		
		}
		reset();
	}
	public void mouseMoved(MouseEvent e) {}
	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
}
