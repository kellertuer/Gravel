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

import model.MGraph;
import model.VEdge;
import model.VGraph;
import model.VNode;
import model.VSegmentedEdge;
import model.VOrthogonalEdge;
import model.VQuadCurveEdge;

/**
 * General mouse drag handling for all mouse modes implemented in Gravel
 * this class is abstract, so this can't be an mouse mode itself.
 * Handles the general mouse movement actions such as shift-drag on background
 * @author ronny
 *
 */
public abstract class DragMouseHandler implements MouseListener, MouseMotionListener 
{
	VNode movingNode;
	VEdge movingEdge;
	VGraph vg;
	GeneralPreferences gp;
	Point MouseOffSet;
	boolean multiplemoving=false,multipleisNode,altwaspressed = false,shiftwaspressed = false, firstdrag;
	Point selstart = null;
	Rectangle selrect = null;
	/**
	 * Initialize the mouse drag handler bound to a specifig VGRaph on with the drags work
	 * 
	 * @param g
	 */
	public DragMouseHandler(VGraph g)
	{
		vg = g;
		gp = GeneralPreferences.getInstance();
		MouseOffSet = new Point(0,0);
		firstdrag = true;
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
	public abstract boolean dragged();
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
		return selrect;
	}
	/**
	 * Update the selection of nodes and edges in the rectangle the user just dragges on the background
	 *
	 */
	private void updateSelection()
	{
		float zoom = ((float)gp.getIntValue("vgraphic.zoom")/100);
		Iterator<VNode> nodeiter = vg.getNodeIterator();
		while (nodeiter.hasNext())
		{
			VNode act = nodeiter.next();
			//rectangle of the node w/ zoom
			Rectangle noderect = new Rectangle(Math.round((float)act.getPosition().x*zoom-(float)act.getSize()*zoom/2.0f),Math.round((float)act.getPosition().y*zoom-(float)act.getSize()*zoom/2.0f),Math.round((float)act.getSize()*zoom),Math.round((float)act.getSize()*zoom));
			if (selrect.intersects(noderect)) 
				act.select();
			else
				act.deselect();
		}
		Iterator<VEdge> edgeiter = vg.getEdgeIterator();
		while (edgeiter.hasNext())
		{
			VEdge e = edgeiter.next();
			e.deselect();
			int start = vg.getEdgeProperties(e.index).elementAt(MGraph.EDGESTARTINDEX);
			int ende = vg.getEdgeProperties(e.index).elementAt(MGraph.EDGEENDINDEX);
			Point sp = (Point) vg.getNode(start).getPosition().clone();
			sp.x = Math.round((float)sp.x*zoom);sp.y = Math.round((float)sp.y*zoom);
			Point ep = (Point) vg.getNode(ende).getPosition().clone();
			ep.x = Math.round((float)ep.x*zoom);ep.y = Math.round((float)ep.y*zoom);
			switch (e.getType())
			{
				case VEdge.STRAIGHTLINE:{
					if (selrect.intersectsLine(new Line2D.Double(sp,ep)))
						e.select();
					break;
				}
				case VEdge.SEGMENTED : {
					Vector<Point> p = ((VSegmentedEdge)e).getControlPoints();
					Point last = sp;
					for (int i=0; i<p.size(); i++)
					{
						if (selrect.intersectsLine(new Line2D.Double(last,p.get(i))))
							e.select();
						last = p.get(i);
					}
					if (selrect.intersectsLine(new Line2D.Double(last,ep)))
						e.select();
					break;
				}
				case VEdge.ORTHOGONAL : {
					if (((VOrthogonalEdge)e).getVerticalFirst())
					{
						if (selrect.intersectsLine(new Line2D.Double(sp, new Point(sp.x,ep.y))))
							e.select();
						if (selrect.intersectsLine(new Line2D.Double(new Point(sp.x,ep.y),ep)))
							e.select();
					}
					else
					{	
						if (selrect.intersectsLine(new Line2D.Double(sp, new Point(ep.x,sp.y))))
							e.select();
						if (selrect.intersectsLine(new Line2D.Double(new Point(ep.x,sp.y),ep)))
							e.select();
					}
					break;
				}
				case VEdge.QUADCURVE: {
					Point bz = ((VQuadCurveEdge)e).getControlPoints().get(0);
					QuadCurve2D.Double q = new QuadCurve2D.Double(sp.x,sp.y,bz.x,bz.y,ep.x,ep.y);
					if (q.intersects(selrect))
						e.select();
				}
				default: { //e.g. Loops or any other new kind, where we don't know any optimization to compute the untersection 
					GeneralPath p = e.getPath(vg.getNode(start).getPosition(),vg.getNode(ende).getPosition(), zoom);
					if (p.intersects(selrect))
						e.select();
				}
			} //end switch
		} //End while edges
	}
	/**
	 * Update the selection of nodes and edges by adding all edges and nodes in the rectangle to the previous selection
	 *
	 */
	private void updateShiftSelection()
	{
		float zoom = ((float)gp.getIntValue("vgraphic.zoom")/100);
		Iterator<VNode> nodeiter = vg.getNodeIterator();
		while (nodeiter.hasNext())
		{
			VNode act = nodeiter.next();
			//rectangle of the node w/ zoom
			Rectangle noderect = new Rectangle(Math.round((float)act.getPosition().x*zoom-(float)act.getSize()*zoom/2.0f),Math.round((float)act.getPosition().y*zoom-(float)act.getSize()*zoom/2.0f),Math.round((float)act.getSize()*zoom),Math.round((float)act.getSize()*zoom));
			if (selrect.intersects(noderect)) 
				act.select();
			else
				act.deselect();
		}
		Iterator<VEdge> edgeiter = vg.getEdgeIterator();
		while (edgeiter.hasNext())
		{
			VEdge e = edgeiter.next();
			e.deselect();
			int start = vg.getEdgeProperties(e.index).elementAt(MGraph.EDGESTARTINDEX);
			int ende = vg.getEdgeProperties(e.index).elementAt(MGraph.EDGEENDINDEX);
			Point sp = (Point) vg.getNode(start).getPosition().clone();
			sp.x = Math.round((float)sp.x*zoom);sp.y = Math.round((float)sp.y*zoom);
			Point ep = (Point) vg.getNode(ende).getPosition().clone();
			ep.x = Math.round((float)ep.x*zoom);ep.y = Math.round((float)ep.y*zoom);
			switch (e.getType())
			{
				case VEdge.STRAIGHTLINE:{
					if (selrect.intersectsLine(new Line2D.Double(sp,ep)))
						e.select();
					break;
				}
				case VEdge.SEGMENTED : {
					Vector<Point> p = ((VSegmentedEdge)e).getControlPoints();
					Point last = sp;
					for (int i=0; i<p.size(); i++)
					{
						if (selrect.intersectsLine(new Line2D.Double(last,p.get(i))))
							e.select();
						last = p.get(i);
					}
					if (selrect.intersectsLine(new Line2D.Double(last,ep)))
						e.select();
					break;
				}
				case VEdge.ORTHOGONAL : {
					if (((VOrthogonalEdge)e).getVerticalFirst())
					{
						if (selrect.intersectsLine(new Line2D.Double(sp, new Point(sp.x,ep.y))))
							e.select();
						if (selrect.intersectsLine(new Line2D.Double(new Point(sp.x,ep.y),ep)))
							e.select();
					}
					else
					{	
						if (selrect.intersectsLine(new Line2D.Double(sp, new Point(ep.x,sp.y))))
							e.select();
						if (selrect.intersectsLine(new Line2D.Double(new Point(ep.x,sp.y),ep)))
							e.select();
					}
					break;
				}
				case VEdge.QUADCURVE: {
					Point bz = ((VQuadCurveEdge)e).getControlPoints().get(0);
					QuadCurve2D.Double q = new QuadCurve2D.Double(sp.x,sp.y,bz.x,bz.y,ep.x,ep.y);
					if (q.intersects(selrect))
						e.select();
				}
				default: { //e.g. Loops or any other new kind, where we don't know any optimization to compute the untersection 
					GeneralPath p = e.getPath(vg.getNode(start).getPosition(),vg.getNode(ende).getPosition(), zoom);
					if (p.intersects(selrect))
						e.select();
				}
			} //end switch
		} //End while edges
	}
	/**
	 * inherited from the mouse drag handler
	 * mouse pressed handles the general wwork that needs to be done on an initialization of a drag - should be called by any subclass 
	 */
	public void mousePressed(MouseEvent e)
	{
		firstdrag=true;
		MouseOffSet = e.getPoint(); //Aktuelle Position merken für eventuelle Bewegungen while pressed
		Point p = new Point(Math.round(e.getPoint().x/((float)gp.getIntValue("vgraphic.zoom")/100)),Math.round(e.getPoint().y/((float)gp.getIntValue("vgraphic.zoom")/100))); //Rausrechnen des zooms
		VNode inrangeN = vg.getNodeinRange(p);
		
		VEdge inrangeE;
		if (vg.isDirected())
			inrangeE = vg.getEdgeinRange(p,2.0d);
		else
			inrangeE=null;
		
		if (!((InputEvent.ALT_DOWN_MASK & e.getModifiersEx()) == InputEvent.ALT_DOWN_MASK)) //no alt ?
		{// insgesamt auf dem Hintergrund ohne shift und ohne alt
			if (!((InputEvent.SHIFT_DOWN_MASK & e.getModifiersEx()) == InputEvent.SHIFT_DOWN_MASK))
				if ((inrangeN==null)&&(vg.getEdgeinRange(p,2.0d)==null)&&(vg.getControlPointinRange(p,gp.getIntValue("vgraphic.cpsize"))==null))
					selstart = MouseOffSet;	
		}
		else if (!((InputEvent.SHIFT_DOWN_MASK & e.getModifiersEx()) == InputEvent.SHIFT_DOWN_MASK))
		{ //Alt and not shift
			movingNode = inrangeN; //kein Shift == moving Node merken, sonst werden alle selected Bewegt
			movingEdge = inrangeE;
			altwaspressed = true;
			if ((inrangeN==null)&&(vg.getEdgeinRange(p,2.0d)==null)&&(vg.getControlPointinRange(p,gp.getIntValue("vgraphic.cpsize"))==null))
				selstart = MouseOffSet;	
		}
		else
		{//Shift and Alt
			shiftwaspressed=true;
			if ((inrangeN!=null)&&(inrangeN.isSelected()))
			{
				multiplemoving = true;
				multipleisNode = true;
			}
			else
			if ((inrangeE!=null)&&(inrangeE.isSelected()))
			{
				multiplemoving = true;
				multipleisNode = false;
			}
			altwaspressed = true;
			//No Edge nor Node in Range
		}
	}
	/**
	 * inherited from MouseDRagHanlder
	 * handles the general work of while dragging the mouse such as mooving the selected elements
	 */
	public void mouseDragged(MouseEvent e) 
	{
		firstdrag = false;
		Point p = new Point(e.getPoint().x-MouseOffSet.x, e.getPoint().y-MouseOffSet.y);
		int x = Math.round(p.x/((float)gp.getIntValue("vgraphic.zoom")/100));
		int y = Math.round(p.y/((float)gp.getIntValue("vgraphic.zoom")/100));
		
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
				vg.pushNotify("N");
			}
			else if (movingEdge!=null)
			{//Single Edge moving
				float arrpos = movingEdge.getArrowPos()+(float)x/100;
				if (arrpos>1.0f)
					arrpos = 1.0f;
				else if (arrpos<0.0f)
					arrpos = 0.0f;
				movingEdge.setArrowPos(arrpos);
				vg.pushNotify("E");
			}
			else if (((InputEvent.SHIFT_DOWN_MASK & e.getModifiersEx()) == InputEvent.SHIFT_DOWN_MASK)&&(multiplemoving))
			{ //Moving multiple elements
				if (multipleisNode)
					moveSelNodes(x,y);
				else
					moveSelEdges(x,y);
			}
		} //End handling ALT
		//Handling selection Rectangle
		else if ((selstart!=null)&&((InputEvent.SHIFT_DOWN_MASK & e.getModifiersEx()) != InputEvent.SHIFT_DOWN_MASK)) //weder shift noch alt
		{
			int rx,ry,rw,rh;
			rx = Math.min(selstart.x,e.getPoint().x);				
			ry = Math.min(selstart.y,e.getPoint().y);
			rw = Math.abs(selstart.x-e.getPoint().x);
			rh = Math.abs(selstart.y-e.getPoint().y);
			selrect = new Rectangle(rx,ry,rw,rh);
			updateSelection();
			vg.pushNotify("M");
		}
		MouseOffSet = e.getPoint();
	}
	/**
	 * Help method for moving selected nodes - if they touch the border of the area (e.g. some values are below 0 after a movement) the whole graph is moved the opposite direction
	 * 
	 * @param x
	 * @param y
	 */
	private void moveSelNodes(int x,int y)
	{
		Iterator<VNode> nodeiter = vg.getNodeIterator();
		while (nodeiter.hasNext()) // drawNodes
		{
			VNode temp = nodeiter.next();
			if ((temp.isSelected())&&(temp.isNameVisible()))
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
		vg.pushNotify("N");
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
		Iterator<VEdge> edgeiter = vg.getEdgeIterator();
		while (edgeiter.hasNext()) // drawNodes
		{
			VEdge temp = edgeiter.next();
			if ((temp.isSelected()))
			{
				float arrpos = temp.getArrowPos()+(float)x/100;
				if (arrpos>1.0f)
					arrpos = 1.0f;
				else if (arrpos<0.0f)
					arrpos = 0.0f;
				temp.setArrowPos(arrpos);
			}
		}
		vg.pushNotify("E");
	}
	/**
	 * And any motion of nodes if movemnt was activ
	 * the moving point is -1/-1 if the modus was changed, than the movement ends itself
	 */
	public void mouseReleased(MouseEvent e) {
		//nur falls schon gedragged wurde nochmals draggen
		if (!firstdrag)
		{
			if (!((e.getPoint().x==-1)||(e.getPoint().y==-1))) //kein Reset von außerhalb wegen modusumschaltung
			mouseDragged(e); //Das gleiche wie als wenn man bewegt, nur ist danach kein Knoten mehr bewegter Knoten		
		
		}
		reset();
	}
	/**
	 * reset all values at the end of a movement/drag
	 *
	 */
	private void reset()
	{
		movingNode=null;
		movingEdge=null;
		altwaspressed=false;
		shiftwaspressed=false;
		multiplemoving = false;
		selstart=null;
		selrect = null;
		firstdrag = true;
	}
}
