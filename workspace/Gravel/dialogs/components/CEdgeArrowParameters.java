package dialogs.components;

import io.GeneralPreferences;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;
import java.util.Observable;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import dialogs.IntegerTextField;

import model.VEdge;
import model.VEdgeArrow;
/**
 * Container for the Parameters Fields of the Arrow Properties of an edge
 * @author ronny
 *
 */
public class CEdgeArrowParameters extends Observable implements CaretListener, ChangeListener, ActionListener
{
	public static final int CEDGEARROWPARAMETERS = 27888; 
	public static final int ARROW_SIZE=1, ARROW_ANGLE=2, ARROW_PART=3,ARROW_POS=4;  
	private IntegerTextField iArrowSize;
	private JSlider sArrowAngle, sArrowPart,sArrowPos;
	private Vector<Integer> values;
	private JCheckBox bChArrowSize, bChArrowAngle, bChArrowPart, bChArrowPos;
	private JLabel ArrowSize, ArrowAngle, ArrowPart, ArrowPos;
	boolean checksEnabled;
	private Container cont;
	/**
	 * Initialize the Parameter GUI with the Values of the arrow of Edge e
	 * Choose null for General Preferences
	 * @param e
	 */
	public CEdgeArrowParameters(VEdge e, boolean checks)
	{
		cont = new Container();
		cont.setLayout(new GridBagLayout());
		checksEnabled = checks;
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(0,7,0,7);
		c.anchor = GridBagConstraints.WEST;
		c.gridy = 0;
		c.gridx = 0;
		if (checksEnabled)
		{
			bChArrowSize = new JCheckBox();
			cont.add(bChArrowSize,c);
			bChArrowSize.addActionListener(this);
			c.gridx++;
		}
		ArrowSize = new JLabel("<html>Pfeilgr"+main.CONST.html_oe+""+main.CONST.html_sz+"e</html>");
		cont.add(ArrowSize,c);
		c.gridx++; 
		iArrowSize = new IntegerTextField();
		iArrowSize.setPreferredSize(new Dimension(100,20));
		iArrowSize.addCaretListener(this);
		cont.add(iArrowSize,c);
		c.gridy++;
		c.gridx = 0;
		if (checksEnabled)
		{
			bChArrowAngle = new JCheckBox();
			cont.add(bChArrowAngle,c);
			bChArrowAngle.addActionListener(this);
			c.gridx++;
		}
		ArrowAngle =new JLabel("<html><p>Winkel<br><font size=\"-2\">in der Pfeilspitze</font></p></html>"); 
		cont.add(ArrowAngle,c);
		c.gridx++; 
		sArrowAngle = new JSlider();
		sArrowAngle.setMinimum(0);
		sArrowAngle.setMaximum(90);
		sArrowAngle.setMajorTickSpacing(15); //ArrowAngle.setMinorTickSpacing(10);
		sArrowAngle.setPaintLabels(true); sArrowAngle.setPaintTicks(true);
		sArrowAngle.addChangeListener(this);
		cont.add(sArrowAngle,c);
		c.gridy++;
		c.gridx = 0;
		if (checksEnabled)
		{
			bChArrowPart = new JCheckBox();
			cont.add(bChArrowPart,c);
			bChArrowPart.addActionListener(this);
			c.gridx++;
		}
		ArrowPart = new JLabel("<html><p>Pfeilform<br><font size=\"-2\">F"+main.CONST.html_ue+"llweite des Pfeils</font></p></html>");
		cont.add(ArrowPart,c);
		c.gridx++; 
		sArrowPart = new JSlider();
		sArrowPart.setMinimum(0);
		sArrowPart.setMaximum(100);
		sArrowPart.setMajorTickSpacing(25);		
		sArrowPart.setPaintLabels(true); sArrowPart.setPaintTicks(true);
		sArrowPart.addChangeListener(this);
		cont.add(sArrowPart,c);
		c.gridy++;
		c.gridx = 0;
		if (checksEnabled)
		{
			bChArrowPos = new JCheckBox();
			cont.add(bChArrowPos,c);
			bChArrowPos.addActionListener(this);
			c.gridx++;
		}
		ArrowPos = new JLabel("<html><p>Pfeilposition<br><font size=\"-2\">entlang der Kante</p></html>");
		cont.add(ArrowPos,c);
		c.gridx++; 
		sArrowPos = new JSlider();
		sArrowPos.setMinimum(0);
		sArrowPos.setMaximum(100);
		Hashtable<Integer, JLabel> t = new Hashtable<Integer, JLabel>();
		t.put(new Integer(0),new JLabel("Start")); t.put(new Integer(25),new JLabel("")); t.put(new Integer(50),new JLabel("Mitte")); t.put(new Integer(75),new JLabel("")); t.put(new Integer(100),new JLabel("Ende"));	
		sArrowPos.setLabelTable(t);
		sArrowPos.setPaintLabels(true); sArrowPos.setPaintTicks(true);
		sArrowPos.addChangeListener(this);
		cont.add(sArrowPos,c);
		c.gridy++; c.gridx=0; c.gridwidth=2; c.anchor=GridBagConstraints.CENTER;
		values = new Vector<Integer>(5);
		values.setSize(5);
		values.set(0,CEDGEARROWPARAMETERS);
		InitValues(e);
	}
	/**
	 * get the GUI-Content
	 * @return
	 */
	public Container getContent()
	{
		return cont;
	}
	/**
	 * Initialize the Fields to the edge e
	 * Choose null for general Preferences
	 * 
	 * @param e
	 */
	public void InitValues(VEdge e)
	{
		if (e!=null)
		{
			VEdgeArrow arr = e.getArrow();
			values.set(ARROW_SIZE, Math.round(arr.getSize()));
			values.set(ARROW_ANGLE, Math.round(arr.getAngle()));
			values.set(ARROW_PART, Math.round(arr.getPart()*100.0f));
			values.set(ARROW_POS, Math.round(arr.getPos()*100.0f));
		}
		else
		{
			GeneralPreferences gp = GeneralPreferences.getInstance();
			values.set(ARROW_SIZE, gp.getIntValue("edge.arrsize"));
			values.set(ARROW_ANGLE, gp.getIntValue("edge.arralpha"));
			values.set(ARROW_PART, gp.getIntValue("edge.arrpart"));
			values.set(ARROW_POS, gp.getIntValue("edge.arrpos"));
		}
		updateUI();
	}
	/**
	 * Update the UI to the Values Vector
	 *
	 */
	public void updateUI()
	{
		if (checksEnabled) //if checks enabled set the activity
		{
			bChArrowAngle.setSelected(values.get(ARROW_ANGLE)!=-1);
			sArrowAngle.setEnabled(values.get(ARROW_ANGLE)!=-1);
		}
		if (values.get(ARROW_ANGLE)!=-1) //valid value
			sArrowAngle.setValue(values.get(ARROW_ANGLE));
		else if (checksEnabled) //no valid value and checks are enabled, disable text
			ArrowAngle.setForeground(Color.GRAY);

		if (checksEnabled) //if checks enabled set the activity
		{	//for invalid value (-1) the values Vector contains a -100
			bChArrowPart.setSelected(values.get(ARROW_PART)!=-100);
			sArrowPart.setEnabled(values.get(ARROW_PART)!=-100);
		}
		if (values.get(ARROW_PART)!=-100) //valid value
			sArrowPart.setValue(values.get(ARROW_PART));
		else if (checksEnabled) //no valid value and checks are enabled, disable text
			ArrowPart.setForeground(Color.GRAY);
		
		if (checksEnabled) //if checks enabled set the activity
		{
			bChArrowSize.setSelected(values.get(ARROW_SIZE)!=-1);
			iArrowSize.setEnabled(values.get(ARROW_SIZE)!=-1);
		}
		if (values.get(ARROW_SIZE)!=-1) //valid value
			iArrowSize.setValue(values.get(ARROW_SIZE));
		else if (checksEnabled) //no valid value and checks are enabled, disable text
			ArrowSize.setForeground(Color.GRAY);

		if (checksEnabled) //if checks enabled set the activity
		{	//for invalid value (-1) the values Vector contains a -100
			bChArrowPos.setSelected(values.get(ARROW_POS)!=-100);
			sArrowPos.setEnabled(values.get(ARROW_POS)!=-100);
		}
		if (values.get(ARROW_POS)!=-100) //valid value
			sArrowPos.setValue(values.get(ARROW_POS));
		else if (checksEnabled) //no valid value and checks are enabled, disable text
			ArrowPos.setForeground(Color.GRAY);

		cont.validate();
	}
	
	/**
	 * Verify the Input Fields of the Container.
	 * Returns an empty String if no error occured, else
	 * @return an error message
	 */
	public String VerifyInput()
	{
		if (iArrowSize.getValue()==-1)
		{
			if (checksEnabled)
			{
				if (bChArrowSize.isSelected())
					return "Keine Pfeilspitzengr"+main.CONST.html_oe+""+main.CONST.html_sz+"e angegeben";
			}
			else
				return "Keine Pfeilspitzengr"+main.CONST.html_oe+""+main.CONST.html_sz+"e angegeben";				
		}
		return "";
	}
	/**
	 * modifies the given Edge to the Fields in this Container, if the Inputs are all valid
	 * @param e the Edge that should be modified 
	 */
	public VEdge modifyEdge(VEdge e)
	{
		if (!VerifyInput().equals(""))
			return null;
		if (checksEnabled)
		{
			if (bChArrowAngle.isSelected())
				e.getArrow().setAngle(sArrowAngle.getValue());
			if (bChArrowPart.isSelected())
				e.getArrow().setPart((float)sArrowPart.getValue()/100);
			if (bChArrowSize.isSelected())
				e.getArrow().setSize(iArrowSize.getValue());
			if (bChArrowPos.isSelected())
				e.getArrow().setPos((float)sArrowPos.getValue()/100);			
		}
		else
		{
			e.getArrow().setAngle(sArrowAngle.getValue());
			e.getArrow().setPart((float)sArrowPart.getValue()/100);
			e.getArrow().setSize(iArrowSize.getValue());
			e.getArrow().setPos((float)sArrowPos.getValue()/100);
		}
		return e;
	}
	public void stateChanged(ChangeEvent e) 
	{
		if (e.getSource()==sArrowPart)
		{
			if (sArrowPart.getValue()==0)
				sArrowPart.setValue(1);
			values.set(ARROW_PART,sArrowPart.getValue());
			setChanged();
			notifyObservers(values);
		}
		else if (e.getSource()==sArrowPos)
		{
			values.set(ARROW_POS,sArrowPos.getValue());
			setChanged();
			notifyObservers(values);
		}
		else if (e.getSource()==sArrowAngle)
		{
			values.set(ARROW_ANGLE,sArrowAngle.getValue());
			setChanged();
			notifyObservers(values);
		}
	}

	public void caretUpdate(CaretEvent event) 
	{
		if (event.getSource()==iArrowSize)
		{
			if (iArrowSize.getValue()>0&&(iArrowSize.getValue()!=values.get(ARROW_SIZE)))
			{ //Wert geändert
				values.set(ARROW_SIZE,iArrowSize.getValue());
				setChanged();
				notifyObservers(values);
			}
		}
	}
	public void actionPerformed(ActionEvent event)
	{
		if (event.getSource()==bChArrowAngle)
		{
			sArrowAngle.setEnabled(bChArrowAngle.isSelected());
			if (bChArrowAngle.isSelected())
				ArrowAngle.setForeground(Color.BLACK);
			else
				ArrowAngle.setForeground(Color.GRAY);
		}
		else if (event.getSource()==bChArrowPart)
		{
			sArrowPart.setEnabled(bChArrowPart.isSelected());
			if (bChArrowPart.isSelected())
				ArrowPart.setForeground(Color.BLACK);
			else
				ArrowPart.setForeground(Color.GRAY);
		}
		if (event.getSource()==bChArrowPos)
		{
			sArrowPos.setEnabled(bChArrowPos.isSelected());
			if (bChArrowPos.isSelected())
				ArrowPos.setForeground(Color.BLACK);
			else
				ArrowPos.setForeground(Color.GRAY);
		}
		if (event.getSource()==bChArrowSize)
		{
			iArrowSize.setEnabled(bChArrowSize.isSelected());
			if (bChArrowSize.isSelected())
				ArrowSize.setForeground(Color.BLACK);
			else
				ArrowSize.setForeground(Color.GRAY);
		}
	}
}
