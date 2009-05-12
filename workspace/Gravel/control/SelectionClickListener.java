package control;


import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import view.VCommonGraphic;
import view.VGraphic;
import view.VHyperGraphic;

import model.*;
import model.Messages.*;

/**
 * Handle Selection-Click-Stuff
 * If an Item is Clicked once 
 * @author ronny
 *
 */
public class SelectionClickListener implements MouseListener {

	VCommonGraphic vgc;
	VGraph vg=null;
	VHyperGraph vhg=null;
	
	public SelectionClickListener(VGraphic g)
	{
		vgc= g;
		vg = g.getGraph();
	}

	public SelectionClickListener(VHyperGraphic g)
	{
		vgc= g;
		vhg = g.getGraph();
	}

	public void mouseClicked(MouseEvent e) 
	{
		//Point in the Graph, without the Zoom in the Display
		Point pointInGraph = new Point(Math.round(e.getPoint().x/((float)vgc.getZoom()/100)),Math.round(e.getPoint().y/((float)vgc.getZoom()/100)));
		VNode r=null;
		VEdge s=null;
		VHyperEdge t=null;
		VGraphInterface notify;
		if (vg!=null) //Normal Graph
		{
			r = vg.modifyNodes.getFirstinRangeOf(pointInGraph);
			s = vg.getEdgeinRangeOf(pointInGraph,2.0*((float)vgc.getZoom()/100));
			notify = vg;
		}
		else if (vhg!=null) //Hypergraph
		{
			r = vhg.modifyNodes.getFirstinRangeOf(pointInGraph);
			t = vhg.getEdgeinRangeOf(pointInGraph, 2.0*((float)vgc.getZoom()/100));
			notify=vhg;
		}
		else
			return; //No graph is !=null
		//Get the item in an VItem
		VItem selectedItem = null;
		if (r!=null)
			selectedItem = r;
		else if (s!=null)
			selectedItem = s;
		else if (t!=null)
			selectedItem = t;

		if (e.getModifiers() == MouseEvent.BUTTON1_MASK+MouseEvent.SHIFT_MASK) // if not Shift Click
		{
			if (selectedItem==null)
				return;
			if ((selectedItem.getSelectedStatus()&VItem.SELECTED)!=VItem.SELECTED)
				selectedItem.setSelectedStatus(VItem.SELECTED);
			else
				selectedItem.deselect();
			notify.pushNotify(new GraphMessage(GraphConstraints.SELECTION,GraphConstraints.UPDATE));
		}
		else if (e.getModifiers() == MouseEvent.BUTTON1_MASK) //Single Click
		{			
			if (selectedItem==null) //Left click on background -> Deselect
			{
				notify.deselect();		
				return;
			}
			if ((selectedItem.getSelectedStatus()&VItem.SELECTED)!=VItem.SELECTED) //Element not selected
			{
				notify.deselect();
				selectedItem.setSelectedStatus(VItem.SELECTED);
			}	
			else //was selected
				notify.deselect();
			notify.pushNotify(new GraphMessage(GraphConstraints.SELECTION,GraphConstraints.UPDATE));						
		}
	}

	public void mouseEntered(MouseEvent e) {}

	public void mouseExited(MouseEvent e) {}

	public void mousePressed(MouseEvent e) {}

	public void mouseReleased(MouseEvent e) {}
}
