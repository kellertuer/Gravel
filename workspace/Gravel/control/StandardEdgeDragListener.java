package control;

import io.GeneralPreferences;

import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Vector;

import view.VCommonGraphic;
import view.VGraphic;

import model.MEdge;
import model.VEdge;
import model.VGraph;
import model.VQuadCurveEdge;
import model.Messages.GraphConstraints;
import model.Messages.GraphMessage;
/**
 * Standard Mouse Drag Actions for Edges
 * Only Capable of VGraphs
 *
 * 
 * @author Ronny Bergmann
 *
 */
public class StandardEdgeDragListener implements MouseListener, MouseMotionListener
{
	private VGraph vg;
	private VCommonGraphic vgc;
	private Point MouseOffSet;
	private GeneralPreferences gp;
	private VEdge movingControlPointEdge=null;
	private VEdge movingEdge=null;
	private int movingControlPointIndex;
	private boolean firstdrag;
	/**
	 * Initializes the Handler to a given VGraph
	 * 
	 * @param g the VGraph
	 */
	public StandardEdgeDragListener(VGraphic g)
	{
		vgc = g;
		vg = g.getGraph();
		gp = GeneralPreferences.getInstance();
		MouseOffSet = new Point(0,0);
		firstdrag = true; //Not dragged up to now
	}
	/**
	 * Indicates, whether a drag is running or not
	 * 
	 * @return true, if a node or edge control point is moved (drag action is running)
	 */
	public boolean dragged()
	{
		return ((movingEdge!=null)||(movingControlPointEdge!=null));
	}
	/**
	 * Handle a drag event, update the positions of moved nodes and theis adjacent edges
	 */
	public void mouseDragged(MouseEvent e) {
		if ((InputEvent.ALT_DOWN_MASK & e.getModifiersEx()) == InputEvent.ALT_DOWN_MASK)
			return; //No ALT Handling here
		//Get Movement since begin of Drag
		Point p = new Point(e.getPoint().x-MouseOffSet.x, e.getPoint().y-MouseOffSet.y);
		//Actual Movement in the graph (without Zoom)
		int Gtransx = Math.round(p.x/((float)vgc.getZoom()/100));
		int Gtransy = Math.round(p.y/((float)vgc.getZoom()/100));
		//Position im Graphen (ohne Zoom)
		Point posInGraph = new Point(Math.round(e.getPoint().x/((float)vgc.getZoom()/100)),Math.round(e.getPoint().y/((float)vgc.getZoom()/100)));
		if (movingControlPointEdge!=null)
		{
			Point newpos;
			Vector<Point> points = movingControlPointEdge.getControlPoints();
			newpos = points.get(movingControlPointIndex);
			newpos.translate(Gtransx,Gtransy); //Bewegung im Graphen aber mit Rungungsfehlern, also nur zurbetrachtung der Gesamtgraphbewegung
			if (newpos.x < 0)
			{
				vg.translate(Math.abs(newpos.x), 0);
				posInGraph.x=0;
			}
			if (newpos.y < 0)
			{
				vg.translate(0,Math.abs(newpos.y));
				posInGraph.y = 0;
			}
			points.set(movingControlPointIndex, posInGraph);
			movingControlPointEdge.setControlPoints(points);
			if (firstdrag) //On First Drag Movement start a Block for CP-Movement else just update it
				vg.pushNotify(new GraphMessage(GraphConstraints.EDGE,movingControlPointEdge.getIndex(),GraphConstraints.UPDATE|GraphConstraints.BLOCK_START,GraphConstraints.EDGE)); //Kanten aktualisiert
			else
				vg.pushNotify(new GraphMessage(GraphConstraints.EDGE,movingControlPointEdge.getIndex(),GraphConstraints.UPDATE,GraphConstraints.EDGE)); //Kanten aktualisiert
		}
		//Neither Shift nor ALT
		if ( ((InputEvent.ALT_DOWN_MASK & e.getModifiersEx()) != InputEvent.ALT_DOWN_MASK) &&
			((InputEvent.SHIFT_DOWN_MASK & e.getModifiersEx()) != InputEvent.SHIFT_DOWN_MASK))
		{
			if (movingEdge!=null)
			{
				if ((movingEdge.getEdgeType()==VEdge.STRAIGHTLINE)&&(firstdrag))
				{
					vg.pushNotify(new GraphMessage(GraphConstraints.EDGE,movingEdge.getIndex(),GraphConstraints.BLOCK_START|GraphConstraints.UPDATE,GraphConstraints.EDGE));
					VEdge temp = movingEdge.clone();
					movingEdge = new VQuadCurveEdge(movingEdge.getIndex(),movingEdge.getWidth(),posInGraph);
					movingEdge.setLinestyle(temp.getLinestyle().clone());
					movingEdge.setTextProperties(temp.getTextProperties().clone());
					movingEdge.setArrow(temp.getArrow().clone());
					MEdge me = vg.getMathGraph().modifyEdges.get(movingEdge.getIndex()).clone();
					//Exchange - places a copy in the graph so we also have to get that bacl
					vg.modifyEdges.replace(movingEdge,me);
					movingEdge = vg.modifyEdges.get(movingEdge.getIndex());
				}
				else if ((!firstdrag)&&(movingEdge.getEdgeType()==VEdge.QUADCURVE))
				{
					Vector<Point> pp = new Vector<Point>();
					pp.add(posInGraph);
					((VQuadCurveEdge)movingEdge).setControlPoints(pp);
					vg.pushNotify(new GraphMessage(GraphConstraints.EDGE,movingEdge.getIndex(),GraphConstraints.UPDATE, GraphConstraints.EDGE|GraphConstraints.SELECTION));
				}
			}
		}
		MouseOffSet = e.getPoint();
		if (firstdrag)
			firstdrag = false; //First time really draged, so it's not firstdrag anymore
	}

	@SuppressWarnings("unchecked")
	public void mousePressed(MouseEvent e) {
		firstdrag = true;
		MouseOffSet = e.getPoint(); //Aktuelle Position merken für eventuelle Bewegungen while pressed Zoom included
		Point p = new Point(Math.round(e.getPoint().x/((float)vgc.getZoom()/100)),Math.round(e.getPoint().y/((float)vgc.getZoom()/100))); //Rausrechnen des zooms
		boolean alt = ((InputEvent.ALT_DOWN_MASK & e.getModifiersEx()) == InputEvent.ALT_DOWN_MASK); // alt ?
		boolean shift = ((InputEvent.SHIFT_DOWN_MASK & e.getModifiersEx()) == InputEvent.SHIFT_DOWN_MASK); //shift ?

		if (alt||shift)
			return;

		if (gp.getBoolValue("vgraphic.cpshow")) 
		{
			Vector c = vg.modifyEdges.firstCPinRageOf(p, (new Integer(gp.getIntValue("vgraphic.cpsize"))).doubleValue());
			if (c!=null)
			{
				movingControlPointEdge = (VEdge) c.get(0);
				movingControlPointIndex = ((Integer)c.get(1)).intValue();
			}
		}
		if (movingControlPointEdge==null)
		{
			movingEdge = vg.getEdgeinRangeOf(p,2.0*((float)vgc.getZoom()/100));
			if ( (movingEdge!=null) &&
					((movingEdge.getEdgeType()!=VEdge.STRAIGHTLINE)||(vg.modifyNodes.getFirstinRangeOf(p)!=null)))
				movingEdge=null;
		}
	}

	public void mouseReleased(MouseEvent e) {
		if (!firstdrag) //If really dragged and not just clicked
		{
			if (!((e.getPoint().x==-1)||(e.getPoint().y==-1)))//kein Reset von außerhalb wegen modusumschaltung
				mouseDragged(e); //Das gleiche wie als wenn man bewegt, nur ist danach kein Knoten mehr bewegter Knoten
			vg.pushNotify(new GraphMessage(GraphConstraints.EDGE,GraphConstraints.BLOCK_END)); //Kanten aktualisiert
		}
		movingControlPointEdge=null;
		movingControlPointIndex = -1;
		movingEdge=null;
		firstdrag = true;
	}

	public void mouseMoved(MouseEvent arg0) {}
	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
}
