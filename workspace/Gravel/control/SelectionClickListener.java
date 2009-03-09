package control;

import io.GeneralPreferences;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Observable;
import java.util.Observer;

import view.VCommonGraphic;
import view.VGraphic;
import view.VHyperGraphic;

import model.VEdge;
import model.VGraph;
import model.VGraphInterface;
import model.VHyperEdge;
import model.VHyperGraph;
import model.VItem;
import model.VNode;
import model.Messages.GraphConstraints;
import model.Messages.GraphMessage;

/**
 * Handle Selection-Click-Stuff
 * If an Item is Clicked once 
 * @author ronny
 *
 */
public class SelectionClickListener implements MouseListener, Observer {

	//Save Any Item that is clicked
	private VNode selectedNode=null; 
	private VEdge selectedEdge=null; 
	private VHyperEdge selectedHyperEdge=null;
	
	private Point PopupCoordinates;
	private GeneralPreferences gp;
	VCommonGraphic vgc;
	VGraph vg=null;
	VHyperGraph vhg=null;
	
	public SelectionClickListener(VGraphic g)
	{
		vgc= g;
		gp = GeneralPreferences.getInstance();
		vg = g.getGraph();
		vg.addObserver(this); //Sub
	}

	public SelectionClickListener(VHyperGraphic g)
	{
		vgc= g;
		gp = GeneralPreferences.getInstance();
		vhg = g.getGraph();
		vhg.addObserver(this); //Sub
	}

	public void mouseClicked(MouseEvent e) {
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
		if (e.getModifiers() == MouseEvent.BUTTON1_MASK+MouseEvent.SHIFT_MASK) // if not Shift Click
		{
			if (r != null) //Clicked on a node toggleSelecition status
			{
				if ((r.getSelectedStatus()&VItem.SELECTED)!=VItem.SELECTED)
					r.setSelectedStatus(VItem.SELECTED);
				else
					r.deselect();
				notify.pushNotify(new GraphMessage(GraphConstraints.SELECTION,GraphConstraints.UPDATE));
			}
			else if (s!=null)
			{
				if ((s.getSelectedStatus()&VItem.SELECTED)!=VItem.SELECTED)
					s.setSelectedStatus(VItem.SELECTED);
				else
					s.deselect();
				notify.pushNotify(new GraphMessage(GraphConstraints.SELECTION,GraphConstraints.UPDATE));
			}
			else if (t!=null)
			{
				if ((t.getSelectedStatus()&VItem.SELECTED)!=VItem.SELECTED)
					t.setSelectedStatus(VItem.SELECTED);
				else
					t.deselect();
				notify.pushNotify(new GraphMessage(GraphConstraints.SELECTION,GraphConstraints.UPDATE));			
			}
		}
		else if (e.getModifiers() == MouseEvent.BUTTON1_MASK) //Single Click
		{
			//Deselect all, and select only any item in range
			notify.deselect();
			if (r!=null)
				r.setSelectedStatus(VItem.SELECTED);
			else if (s!=null)
				s.setSelectedStatus(VItem.SELECTED);
			else if (t!=null)
				t.setSelectedStatus(VItem.SELECTED);
			
			if  ((r!=null) || (s!=null) || (t!=null))
				notify.pushNotify(new GraphMessage(GraphConstraints.SELECTION,GraphConstraints.UPDATE));						
		}
	}

	public void mouseEntered(MouseEvent e) {}

	public void mouseExited(MouseEvent e) {}

	public void mousePressed(MouseEvent e) {}

	public void mouseReleased(MouseEvent e) {}

	public void update(Observable o, Object arg) {}
}
