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

import model.MEdge;
import model.VEdge;
import model.VGraph;
import model.VGraphInterface;
import model.VHyperEdge;
import model.VHyperGraph;
import model.VItem;
import model.VNode;
import model.VOrthogonalEdge;
import model.VQuadCurveEdge;
import model.VSegmentedEdge;
import model.Messages.GraphConstraints;
import model.Messages.GraphMessage;
import view.VCommonGraphic;
import view.VGraphic;
import view.VHyperGraphic;
/**
 * Class for handling any Drags that affect Selection
 * 
 * - Drag on Background begins new Selection (discarding old selection) 
 * - Shift+Drag on Background adds rect to Selection
 * - Alt+Drag on Background removes rect from Selection
 *  
 * @author ronny
 *
 */
public class SelectionDragListener
		implements
			MouseListener,
			MouseMotionListener {
	VGraph vg = null;
	VHyperGraph vhg = null;
	VCommonGraphic vgc;
	GeneralPreferences gp;
	Point MouseOffSet;
	boolean altwaspressed = false,shiftwaspressed = false, firstdrag = true;
	Point selstart = null;
	Rectangle selrect = null;

	public SelectionDragListener(VGraphic g)
	{
		vgc = g;
		vg = g.getGraph();
		gp = GeneralPreferences.getInstance();
		MouseOffSet = new Point(0,0);
	}
	
	public SelectionDragListener(VHyperGraphic g)
	{
		vgc = g;
		vhg = g.getGraph();
		gp = GeneralPreferences.getInstance();
		MouseOffSet = new Point(0,0);
	}
	
	/**
	 * Update the selection of nodes and edges in the rectangle the user just dragges 
	 * on the background to the new status. This status can be specified, so that the rectanlge might be used for different modi
	 *
	 * Everithing in the rectangle gets status activated, everything else deactivated
	 *
	 * @param the status the selection is set to resp. the non selected part is removed from
	 */
	private void updateSelection(int status)
	{
		float zoom = ((float)vgc.getZoom()/100);
		Iterator<VNode> nodeiter = null;
		if (vg!=null)
			nodeiter = vg.modifyNodes.getIterator();
		else if (vhg!=null)
			nodeiter = vhg.modifyNodes.getIterator();
		else
			return;
		while (nodeiter.hasNext())
		{
			VNode act = nodeiter.next();
			//rectangle of the node w/ zoom
			Rectangle noderect = new Rectangle(Math.round((float)act.getPosition().x*zoom-(float)act.getSize()*zoom/2.0f),Math.round((float)act.getPosition().y*zoom-(float)act.getSize()*zoom/2.0f),Math.round((float)act.getSize()*zoom),Math.round((float)act.getSize()*zoom));
			if (selrect.intersects(noderect)) //If Node is Inside selrect
				act.setSelectedStatus(act.getSelectedStatus() | status);
			else
				act.setSelectedStatus(act.getSelectedStatus() & (~status));
		}
		if (vg!=null)
		{
			Iterator<VEdge> edgeiter = vg.modifyEdges.getIterator();
			while (edgeiter.hasNext())
			{
				VEdge e = edgeiter.next();
				MEdge me = vg.getMathGraph().modifyEdges.get(e.getIndex());
				int start = me.StartIndex;
				int ende = me.EndIndex;
				Point sp = (Point) vg.modifyNodes.get(start).getPosition().clone();
				sp.x = Math.round((float)sp.x*zoom);sp.y = Math.round((float)sp.y*zoom);
				Point ep = (Point) vg.modifyNodes.get(ende).getPosition().clone();
				ep.x = Math.round((float)ep.x*zoom);ep.y = Math.round((float)ep.y*zoom);
				boolean intersects = false;
				//Switch Type and check for intersection (special checks for some types)
				switch (e.getType())
				{
					case VEdge.STRAIGHTLINE:{
						intersects = selrect.intersectsLine(new Line2D.Double(sp,ep));
						break;
					}
					case VEdge.SEGMENTED : {
						Vector<Point> p = ((VSegmentedEdge)e).getControlPoints();
						Point last = sp;
						for (int i=0; i<p.size(); i++)
						{
							intersects |= selrect.intersectsLine(new Line2D.Double(last,p.get(i)));
							last = p.get(i);
						}
						intersects |= selrect.intersectsLine(new Line2D.Double(last,ep));
						break;
					}
					case VEdge.ORTHOGONAL : {
						if (((VOrthogonalEdge)e).getVerticalFirst())
							intersects = ((selrect.intersectsLine(new Line2D.Double(sp, new Point(sp.x,ep.y))))||(selrect.intersectsLine(new Line2D.Double(new Point(sp.x,ep.y),ep))));
						else
							intersects = ((selrect.intersectsLine(new Line2D.Double(sp, new Point(ep.x,sp.y))))||(selrect.intersectsLine(new Line2D.Double(new Point(ep.x,sp.y),ep))));
					break;
					}
					case VEdge.QUADCURVE: {
						Point bz = ((VQuadCurveEdge)e).getControlPoints().get(0);
						QuadCurve2D.Double q = new QuadCurve2D.Double(sp.x,sp.y,bz.x,bz.y,ep.x,ep.y);
						intersects = (q.intersects(selrect));
					}
					default: { //e.g. Loops or any other new kind, where we don't know any optimization to compute the untersection 
						GeneralPath p = e.getPath(vg.modifyNodes.get(start).getPosition(),vg.modifyNodes.get(ende).getPosition(), zoom);
						intersects = p.intersects(selrect);
					}
				} //end switch
				if (intersects) //edge lies somewhere in the rect
					e.setSelectedStatus(e.getSelectedStatus() | status);
				else
					e.setSelectedStatus(e.getSelectedStatus() & (~status));
			} //End while edges
		} //end vg!=null	
		else if (vhg!=null)
		{
			Iterator<VHyperEdge> edgeiter = vhg.modifyHyperEdges.getIterator();
			while (edgeiter.hasNext())
			{
				VHyperEdge actualhe = edgeiter.next();
//TODO Intersection of Shape & rect				if (actualhe.getShape().intersects(selrect)))
//					actualhe.setSelectedStatus(e.getSelectedStatus() | status);
//				else
//					actualhe.setSelectedStatus(e.getSelectedStatus() & (~status));
			}
		}
			
	}

	public Rectangle getSelectionRectangle()
	{
		return selrect;
	}

	public boolean dragged()
	{
		return (selrect!=null)&&(!firstdrag);
	}
	private void reset()
	{
		//Only if a Block was started: End it...
		if ((selrect!=null)&&(!firstdrag)) //We had an rectangle, Houston, We had a rectangle
		{
			if (vg!=null)
				vg.pushNotify(new GraphMessage(GraphConstraints.SELECTION,GraphConstraints.BLOCK_END));
			else if (vhg!=null)
				vhg.pushNotify(new GraphMessage(GraphConstraints.SELECTION,GraphConstraints.BLOCK_END));			
		}
		altwaspressed=false;
		shiftwaspressed=false;
		selstart=null;
		selrect = null;
		firstdrag = true;
	}
	//One every Click a potental Drag is initialized but firstdrag = true signals, that no Drag-Movement happened yet
	public void mousePressed(MouseEvent e) {
		firstdrag=true;
		
		MouseOffSet = e.getPoint(); //Aktuelle Position merken für eventuelle Bewegungen while pressed
		Point pointInGraph = new Point(Math.round(e.getPoint().x/((float)vgc.getZoom()/100)),Math.round(e.getPoint().y/((float)vgc.getZoom()/100))); //Rausrechnen des zooms

		VNode nodeInRange=null;
		VEdge edgeInRange=null;
		VHyperEdge hyperedgeInRange=null;
		//Any ControlPoint in Active Drag
		boolean ControlPointsActive = false;
		if (vg!=null) //Normal Graph
		{
			nodeInRange = vg.modifyNodes.getFirstinRangeOf(pointInGraph);
			edgeInRange = vg.getEdgeinRangeOf(pointInGraph,2.0*((float)vgc.getZoom()/100));
			ControlPointsActive = (vg.modifyEdges.firstCPinRageOf(pointInGraph, (new Integer(gp.getIntValue("vgraphic.cpsize"))).doubleValue())!=null)&&(gp.getBoolValue("vgraphic.cpshow"));
		}
		else if (vhg!=null) //Hypergraph
		{
			nodeInRange = vhg.modifyNodes.getFirstinRangeOf(pointInGraph);
			hyperedgeInRange = vhg.getEdgeinRangeOf(pointInGraph, 2.0*((float)vgc.getZoom()/100));
		}
		else
			return; //No graph is !=null
		boolean alt = ((InputEvent.ALT_DOWN_MASK & e.getModifiersEx()) == InputEvent.ALT_DOWN_MASK); // alt ?
		boolean shift = ((InputEvent.SHIFT_DOWN_MASK & e.getModifiersEx()) == InputEvent.SHIFT_DOWN_MASK); //shift ?
		
		if ((!alt)&&(!shift)) //Neither Alt nor Shift on Click...Check for Background
		{
			if ((nodeInRange==null)&&(hyperedgeInRange==null)&&(edgeInRange==null)&&(!ControlPointsActive))
				selstart = MouseOffSet;
		}
		else if ((alt)&&(!shift)) //Just Alt on Click...Check for Background
		{ //Alt and not shift
			altwaspressed=true;
			if ((nodeInRange==null)&&(hyperedgeInRange==null)&&(edgeInRange==null)&&(!ControlPointsActive))
				selstart = MouseOffSet;
		}
		else if ((!alt)&&(shift)) //Just Shift on Click...Check for Background
		{
			shiftwaspressed=true;
			if ((nodeInRange==null)&&(hyperedgeInRange==null)&&(edgeInRange==null)&&(!ControlPointsActive))
				selstart = MouseOffSet;
		}
		//Only Case that remains is Alt&&Shift...don't handle that here
	}

	public void mouseReleased(MouseEvent e) {
		//nur falls schon gedragged wurde nochmals draggen
		if (!firstdrag)
		{
			if (!((e.getPoint().x==-1)||(e.getPoint().y==-1))) //kein Reset von außerhalb wegen modusumschaltung
				mouseDragged(e); //Das gleiche wie als wenn man bewegt, nur ist danach kein Knoten mehr bewegter Knoten		
		}
		Iterator<VNode> nodeiter = null;
		if (vg!=null)
			nodeiter = 	vg.modifyNodes.getIterator();
		else if (vhg!=null)
			nodeiter = vhg.modifyNodes.getIterator();
		while (nodeiter.hasNext())
		{
			VNode act = nodeiter.next();
			if ((act.getSelectedStatus() & VItem.SOFT_SELECTED)==VItem.SOFT_SELECTED)
				act.setSelectedStatus(VItem.SELECTED);
			if ((act.getSelectedStatus() & VItem.SOFT_DESELECTED)==VItem.SOFT_DESELECTED)
				act.setSelectedStatus(VItem.DESELECTED);
		}
		if (vg!=null)
		{
			Iterator<VEdge> edgeiter = vg.modifyEdges.getIterator();
			while (edgeiter.hasNext())
			{
				VEdge act = edgeiter.next();
				if ((act.getSelectedStatus() & VItem.SOFT_SELECTED)==VItem.SOFT_SELECTED)
					act.setSelectedStatus(VItem.SELECTED);
				if ((act.getSelectedStatus() & VItem.SOFT_DESELECTED)==VItem.SOFT_DESELECTED)
					act.setSelectedStatus(VItem.DESELECTED);
			} //End while edges
		}
		else if (vhg!=null)
		{
			Iterator<VHyperEdge> edgeiter = vhg.modifyHyperEdges.getIterator();
			while (edgeiter.hasNext())
			{
				VHyperEdge act = edgeiter.next();
				if ((act.getSelectedStatus() & VItem.SOFT_SELECTED)==VItem.SOFT_SELECTED)
					act.setSelectedStatus(VItem.SELECTED);
				if ((act.getSelectedStatus() & VItem.SOFT_DESELECTED)==VItem.SOFT_DESELECTED)
					act.setSelectedStatus(VItem.DESELECTED);
			} //End while hyperedges
		}		
		reset();
	}

	public void mouseDragged(MouseEvent e) {
		Point p = new Point(e.getPoint().x-MouseOffSet.x, e.getPoint().y-MouseOffSet.y);
		int x = Math.round(p.x/((float)vgc.getZoom()/100)); //Zoom rausrechnen
		int y = Math.round(p.y/((float)vgc.getZoom()/100));
		
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
		
		//Handling selection Rectangle
		if (selstart!=null)
		{
			//Update Rectangle
			int rx,ry,rw,rh;
			rx = Math.min(selstart.x,e.getPoint().x);				
			ry = Math.min(selstart.y,e.getPoint().y);
			rw = Math.abs(selstart.x-e.getPoint().x);
			rh = Math.abs(selstart.y-e.getPoint().y);
			selrect = new Rectangle(rx,ry,rw,rh);
			if ((InputEvent.SHIFT_DOWN_MASK & e.getModifiersEx()) == InputEvent.SHIFT_DOWN_MASK)
			 //Handle Shift selection
				updateSelection(VItem.SOFT_SELECTED);	
			else if ((InputEvent.ALT_DOWN_MASK & e.getModifiersEx()) == InputEvent.ALT_DOWN_MASK)
			 //Handle ALT Selection
				updateSelection(VItem.SOFT_DESELECTED);				
			else
			 //weder shift noch alt -> Selektiertes auf SELECTED
				updateSelection(VItem.SELECTED);
			VGraphInterface notify=null;
			if (vg!=null) //Normal Graph
				notify = vg;
			else if (vhg!=null) //Hypergraph
				notify=vhg;

			if (firstdrag) //If wirst drag - start Block
				notify.pushNotify(new GraphMessage(GraphConstraints.SELECTION,GraphConstraints.BLOCK_START|GraphConstraints.UPDATE,GraphConstraints.SELECTION));
			else		//continnue Block
				notify.pushNotify(new GraphMessage(GraphConstraints.SELECTION,GraphConstraints.UPDATE,GraphConstraints.SELECTION));
		}
		MouseOffSet = e.getPoint();
		firstdrag = false;
	}

	public void mouseMoved(MouseEvent e) {}

	public void mouseEntered(MouseEvent e) {}

	public void mouseExited(MouseEvent e) {}

	public void mouseClicked(MouseEvent e) {}

}
