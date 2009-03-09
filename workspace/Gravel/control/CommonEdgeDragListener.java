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
 * This Class handles Drag Actions on Edges that are common to all modes (of graph editing)
 * this includes all 
 * ALt- & Alt-Shift-Drags that modify the TextPosition
 * 
 * @author ronny
 *
 */
public class CommonEdgeDragListener
		implements
			MouseListener,
			MouseMotionListener {

	VEdge movingEdge;
	VGraph vg;
	VCommonGraphic vgc;
	GeneralPreferences gp;
	Point MouseOffSet;
	boolean multiplemoving=false,altwaspressed = false,shiftwaspressed = false, firstdrag;

	public CommonEdgeDragListener(VGraphic g)
	{
		vgc = g;
		vg = g.getGraph();
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
			if (movingEdge!=null) //Single Node Handled
				vg.pushNotify(new GraphMessage(GraphConstraints.NODE,GraphConstraints.BLOCK_END));
			else if (multiplemoving) //Multiple (really!) Handled
				vg.pushNotify(new GraphMessage(GraphConstraints.EDGE|GraphConstraints.NODE,GraphConstraints.BLOCK_END));
		}
		movingEdge=null;
		altwaspressed=false;
		shiftwaspressed=false;
		multiplemoving = false;
		firstdrag = true;
	}
	public boolean dragged()
	{
		return (movingEdge!=null)||(multiplemoving);
	}
	/**
	 * Help method for moving selected nodes - if they touch the border of the area (e.g. some values are below 0 after a movement) the whole graph is moved the opposite direction
	 * 
	 * @param x
	 * @param y
	 */
	private void moveSelEdges(int x,int y)
	{
		Iterator<VEdge> edgeiter = vg.modifyEdges.getIterator();
		while (edgeiter.hasNext()) // drawNodes
		{
			VEdge temp = edgeiter.next();
			if (((temp.getSelectedStatus()&VItem.SELECTED)==VItem.SELECTED)&&(temp.getTextProperties().isVisible()))
			{
				int pos = (temp.getTextProperties().getPosition()+x)%100;
				if (pos < 0)
					pos +=100;
				int distance = (temp.getTextProperties().getDistance()+y);
				if (distance<0)
					distance = 0;
				temp.getTextProperties().setPosition(pos);
				temp.getTextProperties().setDistance(distance);
			}
		}
	}
	
	public void mousePressed(MouseEvent e) {
		firstdrag=true;
		
		MouseOffSet = e.getPoint(); //Aktuelle Position merken für eventuelle Bewegungen while pressed
		Point pointInGraph = new Point(Math.round(e.getPoint().x/((float)vgc.getZoom()/100)),Math.round(e.getPoint().y/((float)vgc.getZoom()/100))); //Rausrechnen des zooms

		VEdge edgeInRange = vg.getEdgeinRangeOf(pointInGraph,2.0*((float)vgc.getZoom()/100));
		
		boolean alt = ((InputEvent.ALT_DOWN_MASK & e.getModifiersEx()) == InputEvent.ALT_DOWN_MASK); // alt ?
		boolean shift = ((InputEvent.SHIFT_DOWN_MASK & e.getModifiersEx()) == InputEvent.SHIFT_DOWN_MASK); //shift ?
		
		if ((alt)&&(!shift))
		{ //Alt and not shift
			movingEdge = edgeInRange; //kein Shift == moving Node merken, sonst werden alle selected Bewegt
			altwaspressed = true;
		}
		else if ((alt)&&(shift))
		{
			//Shift and Alt Moving multiple Item-Texts
			shiftwaspressed=true;
			altwaspressed = true;
			//Node in Range must be selected
			if ((edgeInRange!=null)&&((edgeInRange.getSelectedStatus()&VItem.SELECTED)==VItem.SELECTED))
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

		if ((InputEvent.ALT_DOWN_MASK & e.getModifiersEx()) == InputEvent.ALT_DOWN_MASK)
		//alt&drag auf nem Knoten oder einer Kante begonnen
		{
			if ((movingEdge!=null)&&(movingEdge.getTextProperties().isVisible()))
			{ //Single Edge
				int pos = (movingEdge.getTextProperties().getPosition()+horizontalMovInGraph)%100;
				if (pos < 0)
					pos +=100;
				int distance = (movingEdge.getTextProperties().getDistance()+verticalMovInGraph);
				if (distance<0)
					distance = 0;
				movingEdge.getTextProperties().setPosition(pos);
				movingEdge.getTextProperties().setDistance(distance);
				if (firstdrag) //Begin drag with a Block-Notification
					vg.pushNotify(new GraphMessage(GraphConstraints.EDGE,GraphConstraints.BLOCK_START|GraphConstraints.UPDATE));
				else		
					vg.pushNotify(new GraphMessage(GraphConstraints.EDGE,GraphConstraints.UPDATE));
			}
			else if (((InputEvent.SHIFT_DOWN_MASK & e.getModifiersEx()) == InputEvent.SHIFT_DOWN_MASK)&&(multiplemoving))
			{
				moveSelEdges(horizontalMovInGraph,verticalMovInGraph);
				if (firstdrag) //Begin drag with a Block Start Notification
					vg.pushNotify(new GraphMessage(GraphConstraints.SELECTION|GraphConstraints.NODE|GraphConstraints.EDGE,GraphConstraints.BLOCK_START|GraphConstraints.UPDATE));
				else		
					vg.pushNotify(new GraphMessage(GraphConstraints.SELECTION|GraphConstraints.NODE|GraphConstraints.EDGE,GraphConstraints.UPDATE));				
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
