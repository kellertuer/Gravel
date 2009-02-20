package control;

import io.GeneralPreferences;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

import model.MEdge;
import model.MNode;
import model.VEdge;
import model.VGraph;
import model.VItem;
import model.VLoopEdge;
import model.VNode;
import model.VStraightLineEdge;
import model.Messages.GraphMessage;
/**
 * OneClick Handling of Mouse Drag actions
 * 
 * starting on a node an edge is created to the mouse
 * shoft+drag on a selected node creates edges from each selected node to the mouse
 * 
 * @author Ronny Bergmann
 */
public class OCMDragMouseHandler extends DragMouseHandler
{

	private VGraph vg;
	private Point MouseOffSet;
	private GeneralPreferences gp;
	private VNode StartNode,DragNode;
	
	private boolean multiple, firstdrag;
	/**
	 * Initializes the Drag Handler to observe a specific VGraph
	 * 
	 * @param g the vgraph
	 */
	public OCMDragMouseHandler(VGraph g)
	{
		super(g);
		vg = g;
		gp = GeneralPreferences.getInstance();
		DragNode = null;
		MouseOffSet = new Point(0,0);
		multiple = false;
		firstdrag = false;
	}
	/**
	 * return the mouseoffset. which is the startposition of any drag-action
	 * 
	 */
	public Point getMouseOffSet()
	{
		return MouseOffSet;
	}
	/**
	 * Return the status of a drag action.
	 * 
	 * @return true, if there is a drag currently running
	 */
	public boolean dragged()
	{
		return ((DragNode!=null));
	}
	/**
	 * Handles a movement of the mouse while a mouse button is pressed
	 * if its the first movement, the edge is initialized and drawn and so on
	 * 
	 * @param e the Event that has to be handled
	 */
	public void mouseDragged(MouseEvent e) {
		super.mouseDragged(e);
		if ( ((InputEvent.ALT_DOWN_MASK & e.getModifiersEx()) == InputEvent.ALT_DOWN_MASK) )
			return; //bei Alt nichts draggen
		
		Point p = new Point(Math.round(e.getPoint().x/((float)gp.getIntValue("vgraphic.zoom")/100)),Math.round(e.getPoint().y/((float)gp.getIntValue("vgraphic.zoom")/100))); //rausrechnen
		if (DragNode!=null)
		{
			DragNode.setPosition(p);
			if (firstdrag)
			{
				firstdrag=false;
				if (!multiple)
				{
					int i = vg.getMathGraph().modifyEdges.getNextIndex();
					vg.pushNotify(new GraphMessage(GraphMessage.EDGE,i,GraphMessage.BLOCK_START|GraphMessage.ADDITION,GraphMessage.EDGE|GraphMessage.NODE));
					vg.modifyEdges.add(new VStraightLineEdge(i,gp.getIntValue("edge.width")), new MEdge(i,StartNode.getIndex(),DragNode.getIndex(),gp.getIntValue("edge.value"),"\u22C6"),StartNode.getPosition(), DragNode.getPosition());
				}
				else
				{
					vg.pushNotify(new GraphMessage(GraphMessage.EDGE,GraphMessage.BLOCK_START|GraphMessage.ADDITION));
					vg.addEdgesfromSelectedNodes(DragNode);
				}
			}
			else
				vg.pushNotify(new GraphMessage(GraphMessage.EDGE,GraphMessage.UPDATE));
		}
	}

	public void mouseMoved(MouseEvent arg0) {}

	public void mouseClicked(MouseEvent e) {}

	public void mouseEntered(MouseEvent e) {}

	public void mouseExited(MouseEvent e) {}
	/**
	 * If the Mouse button is pressed the drag is initiated but firstdrag stays true. so if the mouse button is released without
	 * moevement, nothing happens
	 */
	public void mousePressed(MouseEvent e) {
		super.mousePressed(e);
		if ( ((InputEvent.ALT_DOWN_MASK & e.getModifiersEx()) == InputEvent.ALT_DOWN_MASK) )
			return; //bei Alt nichts draggen
		
		MouseOffSet = e.getPoint(); //Aktuelle Position merken f�r eventuelle Bewegungen while pressed
		Point p = new Point(Math.round(e.getPoint().x/((float)gp.getIntValue("vgraphic.zoom")/100)),Math.round(e.getPoint().y/((float)gp.getIntValue("vgraphic.zoom")/100))); //rausrechnen
		if ((e.getModifiers()&MouseEvent.BUTTON1_MASK)==MouseEvent.BUTTON1_MASK)
		{
			multiple = ((InputEvent.SHIFT_DOWN_MASK & e.getModifiersEx()) == InputEvent.SHIFT_DOWN_MASK);
			if (!multiple)
			{
				StartNode = vg.modifyNodes.getFirstinRangeOf(p); //kein Shift == moving Node merken, sonst werden alle selected Bewegt
				if (StartNode==null)
					return;
				DragNode = new VNode(0,p.x,p.y,3,0,0,0,false);
				DragNode.addColor(Color.white);
				vg.modifyNodes.add(DragNode, new MNode(DragNode.getIndex(),"-"));
			}
			else
			{
				if (!vg.modifyNodes.hasSelection()) //Es müssen welche selektiert sein
					return;
				if (vg.modifyNodes.getFirstinRangeOf(p)==null) //Es muss auf einem Knoten sein, der...
					return;
				if (!((vg.modifyNodes.getFirstinRangeOf(p).getSelectedStatus() & VItem.SELECTED) == VItem.SELECTED)) //selektiert ist
					return;
				//Temp Node index #0 erstellen in weiß
				DragNode = new VNode(0,p.x,p.y,1,0,0,0,false);
				DragNode.addColor(Color.white);
				vg.modifyNodes.add(DragNode, new MNode(DragNode.getIndex(),"-"));
			}
			firstdrag=true;
		}		
	}
	/**
	 * after handling the last dragging action, end the actual drag action
	 */
	public void mouseReleased(MouseEvent e) {
		super.mouseReleased(e);
		Point p = new Point(Math.round(e.getPoint().x/((float)gp.getIntValue("vgraphic.zoom")/100)),Math.round(e.getPoint().y/((float)gp.getIntValue("vgraphic.zoom")/100))); //rausrechnen
		if (((e.getPoint().x==-1)&&(e.getPoint().y==-1))||(firstdrag==true)) //never dragged
		{	
			if (DragNode!=null)
				vg.modifyNodes.remove(DragNode.getIndex());
			StartNode = null;
			DragNode = null;
			return;
		}
		mouseDragged(e); //Das gleiche wie als wenn man bewegt, nur ist danach kein Knoten mehr bewegter Knoten
		if ((DragNode!=null))	
		{	
			if (vg.modifyNodes.get(DragNode.getIndex())!=null)
				vg.modifyNodes.remove(DragNode.getIndex());
			
			VNode EndNode = vg.modifyNodes.getFirstinRangeOf(p);
			if (EndNode!=null)
			{
				if (!multiple)
				{ //Only One Edge created
					int i = vg.getMathGraph().modifyEdges.getNextIndex();
					MEdge m = new MEdge(i,StartNode.getIndex(),EndNode.getIndex(),gp.getIntValue("edge.value"),gp.getEdgeName(i,StartNode.getIndex(),EndNode.getIndex()));
					if ((StartNode.getIndex()==EndNode.getIndex())&&(vg.getMathGraph().isLoopAllowed()))
					{
						VLoopEdge t = new VLoopEdge(i,gp.getIntValue("edge.width"),gp.getIntValue("edge.looplength"),gp.getIntValue("edge.loopdirection"),(double)gp.getIntValue("edge.loopproportion")/100.0d,gp.getBoolValue("edge.loopclockwise"));
						if (vg.modifyEdges.getIndexWithSimilarEdgePath(t, m.StartIndex,m.EndIndex)==0) //No Similar Edge existsmedge.StartIndex,medge.EndIndex))
							{
								vg.modifyEdges.add(t, m,StartNode.getPosition(), EndNode.getPosition());							
								vg.pushNotify(new GraphMessage(GraphMessage.EDGE,i,GraphMessage.BLOCK_END|GraphMessage.ADDITION,GraphMessage.EDGE|GraphMessage.NODE));
							}
						else
						{
							System.err.println("OCMDrag.MouseReleased: Loop - Similar Edge exists, none added.");
							vg.pushNotify(new GraphMessage(GraphMessage.EDGE,i,GraphMessage.BLOCK_ABORT|GraphMessage.BLOCK_END,GraphMessage.EDGE|GraphMessage.NODE));							
						}
					}
					else if (StartNode.getIndex()!=EndNode.getIndex())
					{	
						VEdge s = new VStraightLineEdge(i,gp.getIntValue("edge.width"));
						if (vg.modifyEdges.getIndexWithSimilarEdgePath(s, m.StartIndex,m.EndIndex)==0) //No Similar Edge existsmedge.StartIndex,medge.EndIndex))
						{
							vg.modifyEdges.add(s, m, StartNode.getPosition(), EndNode.getPosition());
							vg.pushNotify(new GraphMessage(GraphMessage.EDGE,i,GraphMessage.BLOCK_END|GraphMessage.ADDITION,GraphMessage.EDGE|GraphMessage.NODE));
						}
						else
						{
							System.err.println("OCMDrag.MouseReleased: Straight - Similar Edge exists, none added.");
							vg.pushNotify(new GraphMessage(GraphMessage.EDGE,i,GraphMessage.BLOCK_ABORT|GraphMessage.BLOCK_END,GraphMessage.EDGE|GraphMessage.NODE));							
						}
					}
				}
				else if (DragNode!=null)
				{	
						vg.addEdgesfromSelectedNodes(EndNode);
						vg.pushNotify(new GraphMessage(GraphMessage.EDGE,GraphMessage.BLOCK_END|GraphMessage.ADDITION));
				}
			}
			else if ((DragNode!=null)&&(!firstdrag)) //We had a drag
			{
				vg.pushNotify(new GraphMessage(GraphMessage.ALL_ELEMENTS,GraphMessage.BLOCK_ABORT|GraphMessage.BLOCK_END));				
			}
			//End Drag Indication
			DragNode = null;
			StartNode = null;
		}
	}
}
