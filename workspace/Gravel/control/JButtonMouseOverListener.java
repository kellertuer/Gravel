package control;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JButton;

public class JButtonMouseOverListener implements MouseListener
{
	private String origText,mouseText;
	private JButton origin;
	private DisplayRunningController c=null;
	public JButtonMouseOverListener(JButton affectedButton, String MouseOverText, DisplayRunningController externalMode)
	{
		origin = affectedButton;
		origText = origin.getText();
		mouseText = MouseOverText;
		origin.addMouseListener(this);
		c = externalMode;
	}
	
	public void mouseEntered(MouseEvent e)
	{
		origin.setText(mouseText);
		if (c!=null)
			c.setAnimationPaused(true);
	}

	public void mouseExited(MouseEvent e)
	{
		if (origin.getText()==mouseText) //We really entered
			origin.setText(origText);
		if (c!=null)
			c.setAnimationPaused(false);
	}

	public void mouseClicked(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}
}
