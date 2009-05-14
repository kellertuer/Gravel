package control.nurbs;

import io.GeneralPreferences;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.util.Iterator;
import java.util.Vector;


import model.MEdge;
import model.NURBSShapeFactory;
import model.VEdge;
import model.VGraph;
import model.VGraphInterface;
import model.VHyperEdge;
import model.NURBSShape;
import model.VHyperGraph;
import model.VItem;
import model.VNode;
import model.VOrthogonalEdge;
import model.VQuadCurveEdge;
import model.VSegmentedEdge;
import model.Messages.GraphConstraints;
import model.Messages.GraphMessage;
import model.Messages.NURBSCreationMessage;
import view.VCommonGraphic;
import view.VGraphic;
import view.VHyperGraphic;
/**
 * Class for handling Drags for Creation of a Circle
 * 
 * - Drag on Background begins new Circle (discarding old circle)
 *  - Drag Begin Point is the origin. Distance from Origin to Mouse is the Circle.
 *  - If Drag ends, the last MousePosition is the CircleRadius that remains
 *  
 * @author Ronny Bergmann
 *
 */
public class CircleCreationHandler implements ShapeCreationMouseHandler {
	VGraph vg = null;
	VHyperGraph vhg = null;
	VCommonGraphic vgc;
	GeneralPreferences gp;
	Point MouseOffSet;
	boolean firstdrag = true;
	Point2D CircleOrigin = null;
	Point DragOrigin=null; //Both Points CircleOrigin and DragOrigin keep the same point, the reset is at different stages and the tye differs
	int size = 0, hyperedgeindex;
	NURBSShape lastcircle=null;

	private void reInit()
	{
		CircleOrigin = null;
		size = 0;
	}
	/**
	 * Initialize the Controller to a given VHYperGraphic and a specified VHyperEdge,
	 * whose shape should be modified
	 * @param g
	 * @param vheI
	 */
	public CircleCreationHandler(VHyperGraphic g, int vheI)
	{
		vgc = g;
		vhg = g.getGraph();
		gp = GeneralPreferences.getInstance();
		MouseOffSet = new Point(0,0);
		hyperedgeindex = vheI;
	}
	
	public Rectangle getSelectionRectangle()
	{ //No Selections possible here
		return null;
	}

	public void resetShape()
	{
		lastcircle=null;
	}
	public NURBSCreationMessage getShapeParameters()
	{
		if ((CircleOrigin==null) || (size<=0))
			return new NURBSCreationMessage();
		//TODO: CreationCircleHandler : Degree
		return new NURBSCreationMessage(2, new Point2D.Double(CircleOrigin.getX(),CircleOrigin.getY()), size);
	}
	public void setShapeParameters(NURBSCreationMessage nm)
	{
		if (dragged())
			return;
		if ((!nm.isValid()) || (nm.getType()!=NURBSCreationMessage.CIRCLE)) //nonsiutable
		{
			reInit();
			return;
		}
		Point2D p = (Point2D) nm.getPoints().firstElement().clone();
		int rad = nm.getValues().firstElement();
		if ((p==null) || (rad<=0) )
		{
			reInit();
			return;
		}
		CircleOrigin = p;
		size = rad;
		buildCircle();
		vhg.modifyHyperEdges.get(hyperedgeindex).setShape(lastcircle);
		if (vhg!=null) //Hypergraph
			vhg.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,hyperedgeindex,GraphConstraints.UPDATE|GraphConstraints.HYPEREDGESHAPE|GraphConstraints.CREATION,GraphConstraints.HYPEREDGE));
	}
	public NURBSShape getShape()
	{
		return lastcircle;
	}
	public boolean dragged()
	{
		return (DragOrigin!=null)&&(!firstdrag);
	}

	private void buildCircle()
	{
		if ((CircleOrigin!=null)&&(size > 0))
		{
			lastcircle = NURBSShapeFactory.CreateShape(getShapeParameters());
		}
		else
			lastcircle = new NURBSShape();
	}
	private void internalReset()
	{
		//Only if a Block was started: End it...
		if ((DragOrigin!=null)&&(!firstdrag)) //We had an Drag an a Circle was created, draw it one final time
		{
			if (vg!=null)
				vg.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,GraphConstraints.BLOCK_END));
			else if (vhg!=null)
			{
				vhg.modifyHyperEdges.get(hyperedgeindex).setShape(lastcircle);
				vhg.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,GraphConstraints.BLOCK_END));			
			}
		}
		DragOrigin=null;
	}
	//One every Click a potental Drag is initialized but firstdrag = true signals, that no Drag-Movement happened yet
	public void mousePressed(MouseEvent e) {
		firstdrag=true;
		boolean alt = ((InputEvent.ALT_DOWN_MASK & e.getModifiersEx()) == InputEvent.ALT_DOWN_MASK); // alt ?
		boolean shift = ((InputEvent.SHIFT_DOWN_MASK & e.getModifiersEx()) == InputEvent.SHIFT_DOWN_MASK); //shift ?
		if (alt||shift)
			return;
		MouseOffSet = e.getPoint(); //Aktuelle Position merken für eventuelle Bewegungen while pressed
		DragOrigin = new Point(Math.round(e.getPoint().x/((float)vgc.getZoom()/100)),Math.round(e.getPoint().y/((float)vgc.getZoom()/100))); //Rausrechnen des zooms
		CircleOrigin = new Point2D.Double((double)e.getPoint().x/(vgc.getZoom()/100d),(double)e.getPoint().y/(vgc.getZoom()/100d));
		size=0;
	}

	public void mouseDragged(MouseEvent e) {
		
		if (((InputEvent.ALT_DOWN_MASK & e.getModifiersEx()) == InputEvent.ALT_DOWN_MASK)||((InputEvent.SHIFT_DOWN_MASK & e.getModifiersEx()) == InputEvent.SHIFT_DOWN_MASK))
		{	
			internalReset();
			return;
		}
		
		//Handling selection Rectangle
		if (DragOrigin!=null)
		{
			//Update Values
			MouseOffSet = e.getPoint(); //Aktuelle Position merken für eventuelle Bewegungen while pressed
			Point pointInGraph = new Point(Math.round(e.getPoint().x/((float)vgc.getZoom()/100)),Math.round(e.getPoint().y/((float)vgc.getZoom()/100))); //Rausrechnen des zooms
			size = Math.round((float)CircleOrigin.distance(pointInGraph));
			buildCircle();
			VGraphInterface notify=null;
			if (vg!=null) //Normal Graph
				notify = vg;
			else if (vhg!=null) //Hypergraph
				notify=vhg;

			if (firstdrag) //If wirst drag - start Block
				notify.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,hyperedgeindex,GraphConstraints.BLOCK_START|GraphConstraints.UPDATE|GraphConstraints.HYPEREDGESHAPE|GraphConstraints.CREATION,GraphConstraints.HYPEREDGE));
			else		//continnue Block
				notify.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,hyperedgeindex,GraphConstraints.UPDATE|GraphConstraints.HYPEREDGESHAPE|GraphConstraints.CREATION,GraphConstraints.HYPEREDGE));
		}
		MouseOffSet = e.getPoint();
		firstdrag = false;
	}

	public void mouseReleased(MouseEvent e) {
		//nur falls schon gedragged wurde nochmals draggen
		if (!firstdrag)
		{
			if (!((e.getPoint().x==-1)||(e.getPoint().y==-1))) //kein Reset von außerhalb wegen modusumschaltung
				mouseDragged(e); //Das gleiche wie als wenn man bewegt, nur ist danach kein Knoten mehr bewegter Knoten		
		}
		internalReset();
	}

	public void mouseMoved(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mouseClicked(MouseEvent e) {}
	public Point getMouseOffSet() {
		return MouseOffSet;
	}
	//Ignore Grid
	public void setGrid(int x, int y) {}
	public void setGridOrientated(boolean b) {}
}
