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

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JRadioButton;

import model.VEdge;
import model.VLoopEdge;
import dialogs.IntegerTextField;
/**
 * This Container_Extension keeps all EdgeLoop-Parameters and the possibility for activation of them
 * @author ronny
 *
 */
public class CLoopParameters extends Container implements ActionListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JLabel Length, Direction, Proportion,Loop, Info;
	private IntegerTextField iLength, iDirection, iProportion;
	private ButtonGroup gLoop;
	private JRadioButton rClockwise, rCounterClockwise;

	private JCheckBox bLength,bDirection, bProportion, bLoop; 
	
	private VEdge chEdge;
	boolean directed, checksEnabled;
	
	/**
	 * Initialize the VLoopEdge Parameter Fields
	 * Every Field can be extended by a CheckBox in Front of it to disable the change of the Value
	 * 
	 * 
	 * 
	 * @param e an VLoopEdge with Parameters for Initialization, choose null for Global Std Values
	 * @param d true for a directed graph, else false
	 * @param checks true, if Checkboxes shall be shown, else false
	 */
	public CLoopParameters(VEdge e, boolean d, boolean checks)
	{
		super();
		checksEnabled = checks;
		directed = d;
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(7,7,7,7);

		
		c.gridx=0; c.gridy=0; c.gridwidth=1; c.anchor = GridBagConstraints.WEST;
		if(checksEnabled)
		{
			bLength = new JCheckBox();
			bLength.addActionListener(this);
			add(bLength,c);
			c.gridx++;
		}
		Length = new JLabel("<html><p>L"+main.CONST.html_ae+"nge<br><font size=\"-2\">der Hauptachse</font></p></html>");
		add(Length,c);
		iLength = new IntegerTextField();	iLength.setPreferredSize(new Dimension(100, 20));
		c.gridx++; add(iLength,c);
		
		c.gridx=0; c.gridy++;; c.gridwidth=1;
		if (checksEnabled)
		{
			bProportion = new JCheckBox();
			bProportion.addActionListener(this);
			add(bProportion,c);
			c.gridx++;
		}
		Proportion = new JLabel("<html><p>Verh"+main.CONST.html_ae+"lnis<br><font size=\"-2\">Haupt- zu Nebenachse(in %)</font></p></html>");
		add(Proportion,c);
		iProportion = new IntegerTextField(); iProportion.setPreferredSize(new Dimension(100, 20));
		c.gridx++; add(iProportion,c);
	
		c.gridx=0; c.gridy++; c.gridwidth=1;
		if (checksEnabled)
		{
			bDirection = new JCheckBox();
			bDirection.addActionListener(this);
			add(bDirection,c);
			c.gridx++;
		}
		Direction = new JLabel("<html><p>Richtung<br><font size=\"-2\">der Hauptachse in Grad</font></p></html>");
		add(Direction,c);
		iDirection = new IntegerTextField(); iDirection.setPreferredSize(new Dimension(100, 20));
		c.gridx++; add(iDirection,c);
		c.gridwidth = 2;
		c.gridx = 0;
		if (checksEnabled) c.gridx++;
		Info = new JLabel("<html><font size=-1>(0=rechts, 90=oben, 180=links, 270=unten)</font></html>");
		c.gridy++; add(Info,c);

		c.gridy++; c.gridx=0;
		if (directed)
		{
			gLoop = new ButtonGroup();
			if (checksEnabled)
			{
				c.gridwidth=1;
				bLoop = new JCheckBox();
				bLoop.addActionListener(this);
				add(bLoop,c);
				c.gridx++;
			}			
			c.gridwidth = 1; //c.gridy++; 
			Loop = new JLabel("<html>Umlaufrichtung der Kante</html>");
			add(Loop,c);
			c.gridy++; c.gridx =0; 
			if (checksEnabled) c.gridx++;
			c.gridwidth = 1;
			rClockwise = new JRadioButton("im Uhrzeigersinn");
			gLoop.add(rClockwise);
			add(rClockwise,c);
			c.gridx++;
			rCounterClockwise = new JRadioButton("gegen den Uhrzeigersinn");
			gLoop.add(rCounterClockwise);
			add(rCounterClockwise,c);
		}
		InitValues(e,d);
	}
	/**
	 * Initialze the Field Values again
	 * Sets also the Checkboxes and Textfield activity if the Checkboxes are activated
	 * choose -1 as Values for inactive fields
	 * @param e VLoopEdge with Values for the fields
	 * @param setClockwise true, if the Clockwise field should be set, to deactivate the field choose false
	 */
	public void InitValues(VEdge e, boolean setClockwise)
	{
		chEdge = e;
		if ((chEdge!=null)&&(chEdge.getType()==VEdge.LOOP))
		{
			VLoopEdge vle = (VLoopEdge)chEdge;
			
			if (checksEnabled)
			{
				bLength.setSelected((vle.getLength()!=-1));
				iLength.setEnabled((vle.getLength()!=-1));
			}
			if (vle.getLength()!=-1)
				iLength.setValue(vle.getLength());
			else if (checksEnabled)
				Length.setForeground(Color.GRAY);
			
			
			if (checksEnabled)
			{
				bProportion.setSelected((vle.getProportion()!=-1.0d));
				iProportion.setEnabled((vle.getProportion()!=-1.0d));
			}
			if (vle.getProportion()!=-1.0d)
				iProportion.setValue((new Double(vle.getProportion()*100.0d)).intValue());
			else if (checksEnabled)
				Proportion.setForeground(Color.GRAY);
			
			if (checksEnabled)
			{
				bDirection.setSelected((vle.getDirection()!=-1));
				iDirection.setEnabled((vle.getDirection()!=-1));
			}
			if (vle.getDirection()!=-1)
				iDirection.setValue(vle.getDirection());
			else if (checksEnabled)
				Direction.setForeground(Color.GRAY);

			if ((bLoop!=null)&&checksEnabled)
			{	
				bLoop.setSelected(setClockwise);
				rClockwise.setEnabled(setClockwise);
				rClockwise.setEnabled(setClockwise);
				if (!setClockwise)
					Loop.setForeground(Color.GRAY);
				if (setClockwise)
				{
					rClockwise.setSelected(vle.isClockwise());
					rCounterClockwise.setSelected(!vle.isClockwise());
				}				
			}
		}
		else
		{
			GeneralPreferences gp = GeneralPreferences.getInstance();
			iLength.setValue(gp.getIntValue("edge.looplength"));
			iProportion.setValue(gp.getIntValue("edge.loopproportion"));
			iDirection.setValue(gp.getIntValue("edge.loopdirection"));
			if (directed)
			{
				rClockwise.setSelected(gp.getBoolValue("edge.loopclockwise"));
				rCounterClockwise.setSelected(!gp.getBoolValue("edge.loopclockwise"));
			}
		}
	}
	/**
	 * Verify the Fields, 
	 * @return an error Message, if an error (empty Field, that is activated) occurs, else an empty String
	 */
	public String VerifyInput()
	{
		if (iLength.getValue()==-1) //nicht ausgefüllt
		{
			if (!checksEnabled) //Checks aus, also Fehler
				return "Schleifenl"+main.CONST.html_ae+"nge nicht angegeben";
			else if (bLength.isSelected())
				return "Schleifenl"+main.CONST.html_ae+"nge nicht angegeben";
		}
		if (iProportion.getValue()==-1) //nicht ausgefüllt
		{
			if (!checksEnabled) //Checks aus, also Fehler
				return "Schleifenverh"+main.CONST.html_ae+"ltnis nicht angegeben";
			else if (bProportion.isSelected())
				return "Schleifenverh"+main.CONST.html_ae+"ltnis nicht angegeben";
		}
		if (iDirection.getValue()==-1) //nicht ausgefüllt
		{
			if (!checksEnabled) //Checks aus, also Fehler
				return "Schleifenrichtung nicht angegeben";
			else if (bDirection.isSelected())
				return "Schleifenrichtung nicht angegeben";
		}
		return "";
	}
	/**
	 * Modify an Edgeand set it's Values to the specified input. Requires an Verified Input (VerifyInput() must return "")
	 * Each Edge, that is no VLoopEdge is not changed
	 * 
	 * @param e an VEdge to be modified
	 * @return the modified Edge, if Textfields are Valid and the VEdge is a VloopEdge, else the nonmdified e
	 */
	public VEdge modifyEdge(VEdge e)
	{
		if (!VerifyInput().equals(""))
			return e; //Exception ?
		if (e.getType()!=VEdge.LOOP)
			return e; //keine Schleife
		VLoopEdge vle = (VLoopEdge) e;
		if (checksEnabled) //nur angekreuzte Werte ändern
		{
			if (bLength.isSelected())
				vle.setLength(iLength.getValue());
			if (bProportion.isSelected())
				vle.setProportion(((double)iProportion.getValue())/100.0d);
			if (bDirection.isSelected())
				vle.setDirection(iDirection.getValue());
			if (directed&&bLoop.isSelected())
				vle.setClockwise(rClockwise.isSelected());
		}
		else //No checks - change all values
		{
				vle.setLength(iLength.getValue());
				vle.setProportion(((double)iProportion.getValue())/100.0d);
				vle.setDirection(iDirection.getValue());
				vle.setClockwise(rClockwise.isSelected());
		}
		return vle;
	}
	
	/**
	 * Create an VLoopEdge with the Parameters from the Inputfields, if they are valid. If they are not valid, this method returns null 
	 * @param i Index of the New Edge
	 * @param w Width of the New Edge
	 * @return null, if the Input Fields are not valid else a new VLoopEdge with index i, width w and the Parameters rom this Container
	 */
	public VLoopEdge createEdge(int i, int w)
	{
		if (!VerifyInput().equals(""))
			return null; //Exception ?
		if (checksEnabled) //No Creation possibe if any field is not active
		{
			if ((!bProportion.isSelected())||(!bLength.isSelected())||(!bDirection.isSelected()))
				return null;
		}
		double proportion = (new Integer(iProportion.getValue())).doubleValue()/100d;
		boolean cw = false;
		if (directed)
			cw = rClockwise.isSelected();
		return new VLoopEdge(i,w,iLength.getValue(),iDirection.getValue(), proportion,cw);
	}
	
	public void actionPerformed(ActionEvent e) 
	{
		if (e.getSource()==bLength)
		{
			iLength.setEnabled(bLength.isSelected());
			if (bLength.isSelected())
				Length.setForeground(Color.BLACK);
			else
				Length.setForeground(Color.GRAY);
		}
		if (e.getSource()==bProportion)
		{
			iProportion.setEnabled(bProportion.isSelected());
			if (bProportion.isSelected())
				Proportion.setForeground(Color.BLACK);
			else
				Proportion.setForeground(Color.GRAY);
		}
		if (e.getSource()==bDirection)
		{
			iDirection.setEnabled(bDirection.isSelected());
			if (bDirection.isSelected())
			{
				Direction.setForeground(Color.BLACK);
				Info.setForeground(Color.BLACK);
			}
			else
			{
				Direction.setForeground(Color.GRAY);
				Info.setForeground(Color.GRAY);
			}
		}
		if (e.getSource()==bLoop)
		{
			rClockwise.setEnabled(bLoop.isSelected());
			rCounterClockwise.setEnabled(bLoop.isSelected());
			if (bLoop.isSelected())
				Loop.setForeground(Color.BLACK);
			else
				Loop.setForeground(Color.GRAY);
		}

	}
}
