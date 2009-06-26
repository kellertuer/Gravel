package control;

import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import view.VCommonGraphic;
import view.VGraphic;
import view.VHyperGraphic;

import model.VEdge;
import model.VGraph;
import model.VHyperEdge;
import model.VHyperGraph;
import model.Messages.GraphConstraints;
import model.Messages.GraphMessage;

/**
 * Handle Click Actions on Edges, that are Common to all Modes and for both types of graphs
 * 
 * @author Ronny Bergmann
 * @since 0.4
 *
 */
public class CommonEdgeClickListener implements MouseListener {

	VCommonGraphic vgc;
	VGraph vg=null;
	VHyperGraph vhg=null;
	
	public CommonEdgeClickListener(VGraphic g)
	{
		vgc= g;
		vg = g.getGraph();
	}

	public CommonEdgeClickListener(VHyperGraphic g)
	{
		vgc= g;
		vhg = g.getGraph();
	}

	public void mouseClicked(MouseEvent e) {
		Point pointInGraph = new Point(Math.round(e.getPoint().x/((float)vgc.getZoom()/100)),Math.round(e.getPoint().y/((float)vgc.getZoom()/100))); //Rausrechnen des zooms
		VEdge edgeInRange=null; VHyperEdge hyperedgeInRange=null;
		if (vg!=null)
		{	
			if (vg.modifyNodes.getFirstinRangeOf(pointInGraph)!=null)
				return;
			edgeInRange = vg.getEdgeinRangeOf(pointInGraph,2.0*((float)vgc.getZoom()/100));
		}
		else if (vhg!=null)
		{
			if (vhg.modifyNodes.getFirstinRangeOf(pointInGraph)!=null)
				return;
			hyperedgeInRange = vhg.getEdgeinRangeOf(pointInGraph,2.0*((float)vgc.getZoom()/100));
		}
		
		boolean alt = ((InputEvent.ALT_DOWN_MASK & e.getModifiersEx()) == InputEvent.ALT_DOWN_MASK); // alt ?
		if (alt&&(edgeInRange!=null)) //if alt+click -> toggle visibility
		{
			edgeInRange.getTextProperties().setVisible(edgeInRange.getTextProperties().isVisible()^true);
			vg.pushNotify(new GraphMessage(GraphConstraints.EDGE, edgeInRange.getIndex(),GraphConstraints.UPDATE, GraphConstraints.EDGE));
		}
		else if (alt&&(hyperedgeInRange!=null))
		{
			hyperedgeInRange.getTextProperties().setVisible(hyperedgeInRange.getTextProperties().isVisible()^true);
			vhg.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE, hyperedgeInRange.getIndex(),GraphConstraints.UPDATE, GraphConstraints.HYPEREDGE));			
		}
	}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}
}
