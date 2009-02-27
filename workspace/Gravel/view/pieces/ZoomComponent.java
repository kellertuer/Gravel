package view.pieces;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Observable;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import dialogs.IntegerTextField;

public class ZoomComponent extends Observable implements ChangeListener, CaretListener
{
	
	private JPanel content;
	private JSlider zoomslider;
	private JLabel zoomfield;
	private IntegerTextField value;
	//	private int value;
	public ZoomComponent()
	{
		value = new IntegerTextField();
		value.setValue(100);
		content = new JPanel();
		content.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(0,0,0,0);
		c.anchor = GridBagConstraints.CENTER;
		c.gridy = 0;
		c.gridx = 0;
		c.gridwidth=5;
		zoomslider = new JSlider(JSlider.HORIZONTAL,15,800,value.getValue());
		zoomslider.setSize(250, 30);
		zoomslider.addChangeListener(this);
		zoomslider.setMajorTickSpacing(100);
		zoomslider.setPaintTicks(true);
		content.add(zoomslider,c);
		c.anchor = GridBagConstraints.EAST;
		zoomfield = new JLabel("<html>Maßstab:&nbsp;</html>");
		zoomfield.revalidate();
		c.gridwidth=1;
		c.gridy++;c.gridx = 1;
		content.add(zoomfield,c);
		c.gridx++;		
		content.add(value,c);
		c.gridx++;
		JLabel proz = new JLabel("%");
		content.add(proz,c);
		JLabel spacer = new JLabel();
		spacer.setPreferredSize(new Dimension(42,0));
		c.gridx = 0;
		content.add(spacer,c);
		content.revalidate();
		value.addCaretListener(this);
	}
	/**
	 * someone moved the Slider so the state changed, notify others
	 */
	public void stateChanged(ChangeEvent arg0)
	{
				value.setValue((int)zoomslider.getValue());
				//zoomfield.setText("<html>Ma"+main.CONST.html_sz+"stab: "+value+"%</html>");
				setChanged();
				notifyObservers("Zoom");
	}
	public JPanel getContent()
	{
		return content;
	}
	public int getZoom()
	{
		return value.getValue();
	}
	/**
	  * Set the Zoom (activates stateChanged)
	  * 
	  * @param v
	  */
	public void setZoom(final int v)
	{
		zoomslider.removeChangeListener(this);
		value.removeCaretListener(this);
		zoomslider.setValue(v);
		value.setValue(v);
		value.addCaretListener(this);
		zoomslider.addChangeListener(this);
		setChanged();
		notifyObservers("Zoom");		
	}
	/**
	 * someone entered text
	 */
	public void caretUpdate(CaretEvent arg0) 
	{
		if (zoomslider.getValue()!=value.getValue())
		{
			zoomslider.removeChangeListener(this);
			zoomslider.setValue(value.getValue());
			setChanged();
			notifyObservers("Zoom");			
			zoomslider.addChangeListener(this);
		}
	}
}
