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
import model.VHyperEdgeShape;
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
 * Class for handling Drags for Modification of the given HyperEdge in the VHyperGraph
 * 
 * The Modification Modes are
 * 
 *  - Rotation: The beginning Position of a Drag is used as rotation Center and the angle
 *    is computed by Difference to the Zero-Degree-Line, that is from the Center to the left
 *    
 *  - Movement of the whole Shape
 *  	Where the Movement vector of the Drag is applied as movement to the Shape
 *  
 *  - Scaling
 *    (a) The distance from the Drag Start Point divided by 10 is used as scaling factor and the
 *        Drag Start Point is the Origin of Scaling. Each Direction, X and Y are scaled
 *    (b) The Distance from Drag Start Point divided by 10 is used
 *        for scaling for X and Y direction seperately
 *   
 *  - Extending - hm the whle Shape is extended by the amount of the Dragged Distance
 *    (perhaps thats better han Scaling (a))
 *    (perhaps apply that also to Slacing b)
 *  
 * @author Ronny Bergmann
 *
 */
public class ShapeModificationDragListener implements DragShapeMouseHandler {
	VHyperGraph vhg = null;
	VCommonGraphic vgc;
	GeneralPreferences gp;
	Point MouseOffSet;
	Point2D.Double DragOrigin;
	boolean firstdrag = true;
	VHyperEdgeShape lastShape=null;

	public ShapeModificationDragListener(VHyperGraphic g)
	{
		vgc = g;
		vhg = g.getGraph();
		gp = GeneralPreferences.getInstance();
		MouseOffSet = new Point(0,0);
	}
	
	public Rectangle getSelectionRectangle()
	{ //No Selections possible here
		return null;
	}

	public void resetShape()
	{
		lastShape=null;
	}
	public Vector<Object> getShapeParameters()
	{
		return new Vector<Object>(); //Because we have no parameters for the Factory
	}
	public void setShapeParameters(Vector<Object> p)
	{} //see above
	public VHyperEdgeShape getShape()
	{
		return lastShape;
	}
	public boolean dragged()
	{
		return (DragOrigin!=null)&&(!firstdrag);
	}
	private void internalReset()
	{
		//Only if a Block was started: End it...
		if (dragged())//We had an Drag an a Circle was created, draw it one final time
		{
			DragOrigin = null;
			vhg.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,GraphConstraints.BLOCK_END));			
		}
	}
	//One every Click a potental Drag is initialized but firstdrag = true signals, that no Drag-Movement happened yet
	public void mousePressed(MouseEvent e) {
		firstdrag=true;
		boolean alt = ((InputEvent.ALT_DOWN_MASK & e.getModifiersEx()) == InputEvent.ALT_DOWN_MASK); // alt ?
		boolean shift = ((InputEvent.SHIFT_DOWN_MASK & e.getModifiersEx()) == InputEvent.SHIFT_DOWN_MASK); //shift ?
		if (alt||shift)
			return;
		MouseOffSet = e.getPoint(); //Aktuelle Position merken für eventuelle Bewegungen while pressed
		DragOrigin = new Point2D.Double((double)e.getPoint().x/((double)vgc.getZoom()/100d),(double)e.getPoint().y/((double)vgc.getZoom()/100d));
		//Rausrechnen des zooms
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
			//TODO MODIFY Shape depending on Modus and DragOrigin / MouseOffSet;
			System.err.println("Drag...");
			if (firstdrag) //If wirst drag - start Block
				vhg.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,GraphConstraints.BLOCK_START|GraphConstraints.UPDATE|GraphConstraints.HYPEREDGESHAPE,GraphConstraints.HYPEREDGE));
			else		//continnue Block
				vhg.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,GraphConstraints.UPDATE|GraphConstraints.HYPEREDGESHAPE,GraphConstraints.HYPEREDGE));
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
