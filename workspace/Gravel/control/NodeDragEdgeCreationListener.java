package control;

import io.GeneralPreferences;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import view.VCommonGraphic;
import view.VGraphic;

import model.MEdge;
import model.MNode;
import model.VEdge;
import model.VGraph;
import model.VItem;
import model.VLoopEdge;
import model.VNode;
import model.VStraightLineEdge;
import model.Messages.GraphConstraints;
import model.Messages.GraphMessage;
/**
 * OneClick Handling of Mouse Node Drag actions
 * 
 * starting on a node an edge is created to the mouse
 * shoft+drag on a selected node creates edges from each selected node to the mouse
 * 
 * @author Ronny Bergmann
 */
public class NodeDragEdgeCreationListener implements MouseListener, MouseMotionListener 
{

	private VGraph vg;
	private VCommonGraphic vgc;
	private GeneralPreferences gp;
	private VNode StartNode,DragNode;
	
	private boolean multiple, firstdrag;
	/**
	 * Initializes the Drag Handler to observe a specific VGraph
	 * 
	 * @param g the vgraph
	 */
	public NodeDragEdgeCreationListener(VGraphic g)
	{
		vgc = g;
		vg = g.getGraph();
		gp = GeneralPreferences.getInstance();
		DragNode = null;
		multiple = false;
		firstdrag = false;
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

	public void mouseDragged(MouseEvent e) {
		if ( ((InputEvent.ALT_DOWN_MASK & e.getModifiersEx()) == InputEvent.ALT_DOWN_MASK) )
			return; //bei Alt nichts draggen
		
		Point posInGraph = new Point(Math.round(e.getPoint().x/((float)vgc.getZoom()/100)),Math.round(e.getPoint().y/((float)vgc.getZoom()/100))); //rausrechnen

		if (DragNode!=null)
		{
			DragNode.setPosition(posInGraph);
			if (firstdrag)
			{
				firstdrag=false;
				if (!multiple)
				{
					int i = vg.getMathGraph().modifyEdges.getNextIndex();
					vg.pushNotify(new GraphMessage(GraphConstraints.EDGE,i,GraphConstraints.BLOCK_START|GraphConstraints.ADDITION,GraphConstraints.EDGE|GraphConstraints.NODE));
					vg.modifyEdges.add(new VStraightLineEdge(i,gp.getIntValue("edge.width")), new MEdge(i,StartNode.getIndex(),DragNode.getIndex(),gp.getIntValue("edge.value"),"\u22C6"),StartNode.getPosition(), DragNode.getPosition());
				}
				else
				{
					vg.pushNotify(new GraphMessage(GraphConstraints.EDGE,GraphConstraints.BLOCK_START|GraphConstraints.ADDITION));
					vg.addEdgesfromSelectedNodes(DragNode);
				}
			}
			else
				vg.pushNotify(new GraphMessage(GraphConstraints.EDGE,GraphConstraints.UPDATE));
		}
	}

	public void mousePressed(MouseEvent e) {
		if ( ((InputEvent.ALT_DOWN_MASK & e.getModifiersEx()) == InputEvent.ALT_DOWN_MASK) )
			return; //bei Alt nichts draggen

		Point p = new Point(Math.round(e.getPoint().x/((float)vgc.getZoom()/100)),Math.round(e.getPoint().y/((float)vgc.getZoom()/100))); //rausrechnen
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

	public void mouseReleased(MouseEvent e) {
		Point p = new Point(Math.round(e.getPoint().x/((float)vgc.getZoom()/100)),Math.round(e.getPoint().y/((float)vgc.getZoom()/100))); //rausrechnen
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
						VLoopEdge t = new VLoopEdge(i,gp.getIntValue("edge.width"),gp.getIntValue("edge.loop_length"),gp.getIntValue("edge.loop_direction"),(double)gp.getIntValue("edge.loop_proportion")/100.0d,gp.getBoolValue("edge.loop_clockwise"));
						if (vg.modifyEdges.getIndexWithSimilarEdgePath(t, m.StartIndex,m.EndIndex)==0) //No Similar Edge existsmedge.StartIndex,medge.EndIndex))
							{
								vg.modifyEdges.add(t, m,StartNode.getPosition(), EndNode.getPosition());							
								vg.pushNotify(new GraphMessage(GraphConstraints.EDGE,i,GraphConstraints.BLOCK_END|GraphConstraints.ADDITION,GraphConstraints.EDGE|GraphConstraints.NODE));
							}
						else
						{
							main.DEBUG.println(main.DEBUG.LOW, "OCMDrag.MouseReleased: Loop - Similar Edge exists, none added.");
							vg.pushNotify(new GraphMessage(GraphConstraints.EDGE,i,GraphConstraints.BLOCK_ABORT|GraphConstraints.BLOCK_END,GraphConstraints.EDGE|GraphConstraints.NODE));							
						}
					}
					else if (StartNode.getIndex()!=EndNode.getIndex())
					{	
						VEdge s = new VStraightLineEdge(i,gp.getIntValue("edge.width"));
						if (vg.modifyEdges.getIndexWithSimilarEdgePath(s, m.StartIndex,m.EndIndex)==0) //No Similar Edge existsmedge.StartIndex,medge.EndIndex))
						{
							vg.modifyEdges.add(s, m, StartNode.getPosition(), EndNode.getPosition());
							vg.pushNotify(new GraphMessage(GraphConstraints.EDGE,i,GraphConstraints.BLOCK_END|GraphConstraints.ADDITION,GraphConstraints.EDGE|GraphConstraints.NODE));
						}
						else
						{
							main.DEBUG.println(main.DEBUG.LOW,"OCMDrag.MouseReleased: Straight - Similar Edge exists, none added.");
							vg.pushNotify(new GraphMessage(GraphConstraints.EDGE,i,GraphConstraints.BLOCK_ABORT|GraphConstraints.BLOCK_END,GraphConstraints.EDGE|GraphConstraints.NODE));							
						}
					}
				}
				else if (DragNode!=null)
				{	
						vg.addEdgesfromSelectedNodes(EndNode);
						vg.pushNotify(new GraphMessage(GraphConstraints.EDGE,GraphConstraints.BLOCK_END|GraphConstraints.ADDITION));
				}
			}
			else if ((DragNode!=null)&&(!firstdrag)) //We had a drag
			{
				vg.pushNotify(new GraphMessage(GraphConstraints.GRAPH_ALL_ELEMENTS,GraphConstraints.BLOCK_ABORT|GraphConstraints.BLOCK_END));				
			}
			//End Drag Indication
			DragNode = null;
			StartNode = null;
		}
	}
	public void mouseMoved(MouseEvent arg0) {}
	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
}
