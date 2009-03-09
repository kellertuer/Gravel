package control;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import view.VCommonGraphic;
import view.VGraphic;
import view.VHyperGraphic;

import model.VGraph;
import model.VHyperGraph;
import model.VNode;
import dialogs.JNodeDialog;

/**
 * Handle Click Actions on Nodes, that are Common to all Modes
 * 
 * @author Ronny Bergmann
 * @since 0.4
 *
 */
public class CommonNodeClickListener implements MouseListener {

	VCommonGraphic vgc;
	VGraph vg=null;
	VHyperGraph vhg=null;
	
	public CommonNodeClickListener(VGraphic g)
	{
		vgc= g;
		vg = g.getGraph();
	}

	public CommonNodeClickListener(VHyperGraphic g)
	{
		vgc= g;
		vhg = g.getGraph();
	}

	public void mouseClicked(MouseEvent e) {
		Point p = new Point(Math.round(e.getPoint().x/((float)vgc.getZoom()/100)),Math.round(e.getPoint().y/((float)vgc.getZoom()/100))); //rausrechnen
		if ((e.getClickCount()==2)&&(e.getModifiers() == MouseEvent.BUTTON1_MASK))
		{ //Double Click on Node
			VNode r=null;
			if (vg!=null)
				r = vg.modifyNodes.getFirstinRangeOf(p);
			else if (vhg!=null)
				r = vhg.modifyNodes.getFirstinRangeOf(p);
			if (r!=null) //Doubleclick really on Node
			{
				if (vg!=null)
					new JNodeDialog(r,vg);
//				else
//TODO					new JNodeDialog(r,vhg);
			}
		}
	}

	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub

	}

}
