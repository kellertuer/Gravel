package control;

import io.GeneralPreferences;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.QuadCurve2D;
import java.util.Iterator;
import java.util.Vector;

import view.VCommonGraphic;
import view.VGraphic;

import model.MEdge;
import model.VEdge;
import model.VGraph;
import model.VItem;
import model.VNode;
import model.VSegmentedEdge;
import model.VOrthogonalEdge;
import model.VQuadCurveEdge;
import model.Messages.GraphConstraints;
import model.Messages.GraphMessage;

/**
 * General mouse drag handling for all mouse modes implemented in Gravel
 * this class is abstract, so this can't be an mouse mode itself.
 * Handles the general mouse movement actions such as shift-drag on background
 * @author Ronny Bergmann
 *
 */
public abstract class DragMouseHandler implements MouseListener, MouseMotionListener 
{
	VNode movingNode;
	VEdge movingEdge;
	VGraph vg;
	VCommonGraphic vgc;
	GeneralPreferences gp;
	Point MouseOffSet;
	boolean multiplemoving=false,multipleisNode,altwaspressed = false,shiftwaspressed = false, firstdrag;
	SelectionDragListener SelectionDragActions;
	/**
	 * Initialize the mouse drag handler bound to a specifig VGRaph on with the drags work
	 * 
	 * @param g
	 */
	public DragMouseHandler(VGraphic g)
	{
		vgc = g;
		vg = g.getGraph();
		gp = GeneralPreferences.getInstance();
		MouseOffSet = new Point(0,0);
		firstdrag = true;
		SelectionDragActions= new SelectionDragListener(g);
	}
	/**
	 * get actual MouseOffSet, abstract, so that every mouse mode has to implement this.
	 * mouseoffset is the startpoint of a drag
	 * @return
	 */
	public abstract Point getMouseOffSet();
	/**
	 * Indicated whetther someoe is just dragging or not
	 * @return
	 */
	public boolean dragged()
	{
		return SelectionDragActions.dragged();
	}
	/** set whether the nodes are set to a gridpoint after dragging or not. Handler must not implement this
	 * 
	 */
	public void setGridOrientated(boolean b){}
	/** update Gridinfo
	 * 
	 */
	public void setGrid(int x, int y){}
	/**
	 * For Displaying the Selection Rectangle
	 * 
	 * @return the rectangle if it exists, else null
	 */
	public Rectangle getSelectionRectangle()
	{
		return SelectionDragActions.getSelectionRectangle();
	}
	/**
	 * reset all values at the end of a movement/drag
	 *
	 */
	private void reset()
	{
		//Only if a Block was started: End it...
		if (movingNode!=null) //Single Node Handled
			vg.pushNotify(new GraphMessage(GraphConstraints.NODE,GraphConstraints.BLOCK_END));
		else if (movingEdge!=null) //Single Edge Handled
			vg.pushNotify(new GraphMessage(GraphConstraints.EDGE,GraphConstraints.BLOCK_END));
		else if (multiplemoving&&(!firstdrag)) //Multiple (really!) Handled
			vg.pushNotify(new GraphMessage(GraphConstraints.EDGE|GraphConstraints.NODE,GraphConstraints.BLOCK_END));
		movingNode=null;
		movingEdge=null;
		altwaspressed=false;
		shiftwaspressed=false;
		multiplemoving = false;
		firstdrag = true;
	}
	/**
	 * moving selected Edges. The start and end point might not be moved, but every controlpoint is moved
	 * - half of the movement if one of the incident nodes is moved (selected)
	 * - whole movement if both nodes are moved (selected)
	 * @param x
	 * @param y
	 */
	private void moveSelEdges(int x,int y)
	{
		Iterator<VEdge> edgeiter = vg.modifyEdges.getIterator();
		while (edgeiter.hasNext()) // drawNodes
		{
			VEdge temp = edgeiter.next();
			if ((temp.getSelectedStatus()&VItem.SELECTED)==VItem.SELECTED)
			{
				float arrpos = temp.getArrow().getPos()+(float)x/100;
				if (arrpos>1.0f)
					arrpos = 1.0f;
				else if (arrpos<0.0f)
					arrpos = 0.0f;
				temp.getArrow().setPos(arrpos);
			}
		}
	}
	/**
	 * Help method for moving selected nodes - if they touch the border of the area (e.g. some values are below 0 after a movement) the whole graph is moved the opposite direction
	 * 
	 * @param x
	 * @param y
	 */
	private void moveSelNodes(int x,int y)
	{
		Iterator<VNode> nodeiter = vg.modifyNodes.getIterator();
		while (nodeiter.hasNext()) // drawNodes
		{
			VNode temp = nodeiter.next();
			if (((temp.getSelectedStatus()&VItem.SELECTED)==VItem.SELECTED)&&(temp.isNameVisible()))
			{
				int rotation = (temp.getNameRotation()+x*4)%360;
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
	/**
	 * inherited from the mouse drag handler
	 * mouse pressed handles the general wwork that needs to be done on an initialization of a drag - should be called by any subclass 
	 */
	public void mousePressed(MouseEvent e)
	{
		SelectionDragActions.mousePressed(e);
		firstdrag=true;
		
		MouseOffSet = e.getPoint(); //Aktuelle Position merken für eventuelle Bewegungen while pressed
		Point p = new Point(Math.round(e.getPoint().x/((float)vgc.getZoom()/100)),Math.round(e.getPoint().y/((float)vgc.getZoom()/100))); //Rausrechnen des zooms
		VNode inrangeN = vg.modifyNodes.getFirstinRangeOf(p);
		
		VEdge inrangeE;
		if (vg.getMathGraph().isDirected())
			inrangeE = vg.getEdgeinRangeOf(p,2.0*((float)vgc.getZoom()/100));
		else
			inrangeE=null;
		
		boolean alt = ((InputEvent.ALT_DOWN_MASK & e.getModifiersEx()) == InputEvent.ALT_DOWN_MASK); // alt ?
		boolean shift = ((InputEvent.SHIFT_DOWN_MASK & e.getModifiersEx()) == InputEvent.SHIFT_DOWN_MASK); //shift ?
		
		if ((alt)&&(!shift))
		{ //Alt and not shift
			movingNode = inrangeN; //kein Shift == moving Node merken, sonst werden alle selected Bewegt
			movingEdge = inrangeE;
			altwaspressed = true;
		}
		else
		{
			//Shift and Alt Moving multiple Item-Texts
			shiftwaspressed=true;
			altwaspressed = true;
			if ((inrangeN!=null)&&((inrangeN.getSelectedStatus()&VItem.SELECTED)==VItem.SELECTED))
			{
				multiplemoving = true;
				multipleisNode = true;
			}
			else
			if ((inrangeE!=null)&&((inrangeE.getSelectedStatus()&VItem.SELECTED)==VItem.SELECTED))
			{
				multiplemoving = true;
				multipleisNode = false;
			}
			//No Edge nor Node in Range
		}
	}
	public void mouseDragged(MouseEvent e) 
	{
		SelectionDragActions.mouseDragged(e);

		Point p = new Point(e.getPoint().x-MouseOffSet.x, e.getPoint().y-MouseOffSet.y);
		int x = Math.round(p.x/((float)vgc.getZoom()/100)); //Zoom rausrechnen
		int y = Math.round(p.y/((float)vgc.getZoom()/100));
		
		if ((altwaspressed)&&!(((InputEvent.ALT_DOWN_MASK & e.getModifiersEx()) == InputEvent.ALT_DOWN_MASK)))
		{ //Mit alt den Drag begonnen und dann alt losgelassen
			reset();
			return;
		}
		if ((shiftwaspressed)&&!(((InputEvent.SHIFT_DOWN_MASK & e.getModifiersEx()) == InputEvent.SHIFT_DOWN_MASK)))
		{ //Mit shift den Drag begonnen und dann alt losgelassen
			reset();
			return;
		}
		if (altwaspressed&&(((InputEvent.ALT_DOWN_MASK & e.getModifiersEx()) == InputEvent.ALT_DOWN_MASK))) 
		//alt&drag auf nem Knoten oder einer Kante begonnen
		{
			if ((movingNode!=null)&&(movingNode.isNameVisible()))
			{ //SingleNode
				int rotation = (movingNode.getNameRotation()+x*4)%360;
				if (rotation < 0)
					rotation +=360;
				int distance = (movingNode.getNameDistance()+y);
				if (distance<0)
					distance = 0;
				movingNode.setNameDistance(distance); //Y Richtung setzt Distance
				movingNode.setNameRotation(rotation); //X Setzt Rotation
				if (firstdrag) //Begin drag with a Block-Notification
					vg.pushNotify(new GraphMessage(GraphConstraints.NODE,GraphConstraints.BLOCK_START|GraphConstraints.UPDATE));
				else		
					vg.pushNotify(new GraphMessage(GraphConstraints.NODE,GraphConstraints.UPDATE));
			}
			else if (movingEdge!=null)
			{//Single Edge moving
				float arrpos = movingEdge.getArrow().getPos()+(float)x/100;
				if (arrpos>1.0f)
					arrpos = 1.0f;
				else if (arrpos<0.0f)
					arrpos = 0.0f;
				movingEdge.getArrow().setPos(arrpos);
				if (firstdrag) //Begin Drag with a Block Start-Notification
					vg.pushNotify(new GraphMessage(GraphConstraints.EDGE,GraphConstraints.BLOCK_START|GraphConstraints.UPDATE));
				else		
					vg.pushNotify(new GraphMessage(GraphConstraints.EDGE,GraphConstraints.UPDATE));
			}
			else if (((InputEvent.SHIFT_DOWN_MASK & e.getModifiersEx()) == InputEvent.SHIFT_DOWN_MASK)&&(multiplemoving))
			{ //Shift + Alt + Multiple Moving multiple elements
				if (multipleisNode)
					moveSelNodes(x,y);
				else
					moveSelEdges(x,y);
				if (firstdrag) //Begin drag with a Block Start Notification
					vg.pushNotify(new GraphMessage(GraphConstraints.SELECTION|GraphConstraints.NODE|GraphConstraints.EDGE,GraphConstraints.BLOCK_START|GraphConstraints.UPDATE));
				else		
					vg.pushNotify(new GraphMessage(GraphConstraints.SELECTION|GraphConstraints.NODE|GraphConstraints.EDGE,GraphConstraints.UPDATE));				
			}
		} //End handling ALT
		MouseOffSet = e.getPoint();
		firstdrag = false;
	}
	/**
	 * And any motion of nodes if movemnt was activ
	 * the moving point is -1/-1 if the modus was changed, than the movement ends itself
	 */
	public void mouseReleased(MouseEvent e) {
		SelectionDragActions.mouseReleased(e);
		//nur falls schon gedragged wurde nochmals draggen
		if (!firstdrag)
		{
			if (!((e.getPoint().x==-1)||(e.getPoint().y==-1))) //kein Reset von außerhalb wegen modusumschaltung
			mouseDragged(e); //Das gleiche wie als wenn man bewegt, nur ist danach kein Knoten mehr bewegter Knoten		
		}
		reset();
	}
}
