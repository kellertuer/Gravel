package algorithm.forms;

import java.awt.Point;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.JDialog;

import view.Gui;

import model.MGraph;
import model.VGraph;

public abstract class AlgorithmParameterForm extends JDialog implements ActionListener{

	public AlgorithmParameterForm(MGraph g)
	{
		
	}
	public AlgorithmParameterForm(VGraph g)
	{
		
	}	
	protected void alignCenter()
	{
		Point p = new Point(0,0);
		p.y += Math.round(Gui.getInstance().getParentWindow().getHeight()/2);
		p.x += Math.round(Gui.getInstance().getParentWindow().getWidth()/2);
		p.y -= Math.round(getHeight()/2);
		p.x -= Math.round(getWidth()/2);
		setLocation(p.x,p.y);
	}
	/**
	 * starts the dialog and returns 
	 * @return
	 */
	public abstract HashMap showDialog();
}
