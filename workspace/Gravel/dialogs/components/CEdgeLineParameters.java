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
import java.util.Observable;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import dialogs.IntegerTextField;

import model.VEdge;
import model.VEdgeLinestyle;
import model.VHyperEdge;
/**
 * Container for the Parameters Fields of the Text Properties of an edge
 * @author ronny
 *
 */
public class CEdgeLineParameters extends Observable implements ActionListener, CaretListener
{
	public static final int CEDGELINEPARAMETERS = 269866; 
	public static final int LINETYPE=1, LINEDISTANCE=2,LINELENGTH=3;
	
	private Vector<Integer> values;
	private Container LineContent;
	private String[] LineTypenames; 
	private JComboBox cLineTypes;
	private IntegerTextField iLineDistance, iLineLength;
	private JLabel LineTypes, LineDistance, LineLength;
	boolean global, checksEnabled;

	private JCheckBox bChLineDistance, bChLineLength, bChLineType;
	
	/**
	 * Initialize the Parameter GUI with the Values of the arrow of Edge e
	 * Choose null for General Preferences
	 * @param e
	 */
	public CEdgeLineParameters(VEdgeLinestyle e, boolean g, boolean checks)
	{
		global = (g)&&(!checks); //global is only possible if there are no checkBoxes
		checksEnabled = checks;
		LineContent = new Container();
		LineContent.setLayout(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(4,7,4,7);
		c.anchor = GridBagConstraints.WEST;
		c.gridy = 0;
		c.gridx = 0;
		
		c.gridwidth=1;
		if (checksEnabled)
		{
			bChLineType = new JCheckBox();
			LineContent.add(bChLineType,c);
			bChLineType.addActionListener(this);
			c.gridx++;
		}
		LineTypes = new JLabel("<html>Linienstil</html>");
		LineContent.add(LineTypes,c);
		c.gridx++;
		String[] typetemp = {	"\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500", //Solid
							" \u2500  \u2500  \u2500  \u2500  \u2500 ", //Dashed
							"\u25cf    \u25cf    \u25cf    \u25cf    \u25cf", //Dotted
							"\u25cf   \u2500   \u25cf   \u2500   \u25cf"}; //Dotdashed
		LineTypenames = typetemp;
		cLineTypes = new JComboBox(LineTypenames);
		cLineTypes.addActionListener(this);
		LineContent.add(cLineTypes,c);
		c.gridy++; c.gridx = 0;
		if (checksEnabled)
		{
			bChLineDistance = new JCheckBox();
			LineContent.add(bChLineDistance,c);
			bChLineDistance.addActionListener(this);
			c.gridx++;
		}
		LineDistance = new JLabel("<html>Abstand<br><font size=-2>zwischen Punkten/Strichen</font></html>");
		LineContent.add(LineDistance,c);
		iLineDistance = new IntegerTextField();
		iLineDistance.addCaretListener(this);
		iLineDistance.setPreferredSize(new Dimension(100, 20));
		c.gridx++;
		LineContent.add(iLineDistance,c);
		c.gridy++; c.gridx = 0;
		if (checksEnabled)
		{
			bChLineLength = new JCheckBox();
			LineContent.add(bChLineLength,c);
			bChLineLength.addActionListener(this);
			c.gridx++;
		}
		LineLength = new JLabel("<html>L"+main.CONST.utf8_ae+"nge<br><font size=-2>eines Strichs</font></html>");
		LineContent.add(LineLength,c);
		iLineLength = new IntegerTextField();
		iLineLength.addCaretListener(this);
		iLineLength.setPreferredSize(new Dimension(100, 20));
		c.gridx++;
		LineContent.add(iLineLength,c);
		
		values = new Vector<Integer>(5);
		values.setSize(6);
		values.set(0,CEDGELINEPARAMETERS);
		InitValues(e);
	}
	/**
	 * get the GUI-Content
	 * @return
	 */
	public Container getContent()
	{
		return LineContent;
	}
	/**
	 * Initialize the Fields to the edge e
	 * Choose null for general Preferences
	 * 
	 * @param e
	 */
	public void InitValues(VEdgeLinestyle e)
	{
		if (e!=null)
		{
			values.set(LINETYPE, e.getType()-1);
			values.set(LINELENGTH, e.getLength());
			values.set(LINEDISTANCE, e.getDistance());
		}
		else
		{
			GeneralPreferences gp = GeneralPreferences.getInstance();
			values.set(LINETYPE, gp.getIntValue("edge.line_type")-1); //sync values.LINETYPE to the UI (so add +1 so save value)
			values.set(LINELENGTH, gp.getIntValue("edge.line_length"));
			values.set(LINEDISTANCE, gp.getIntValue("edge.line_distance"));
		}
		updateUI(true);
	}
	/**
	 * Update the UI to the Values Vector, if checks are enabled, setLineType specifies the activity of the LineTypeCheckBox
	 *
	 */
	public void updateUI(boolean setLineType)
	{	
		if (checksEnabled)
		{ //if checks are enabled...set the activity and select the checkbox if a value is given
			iLineDistance.setEnabled(values.get(LINEDISTANCE)!=-1);
			bChLineDistance.setSelected(values.get(LINEDISTANCE)!=-1);
		}
		if (values.get(LINEDISTANCE)!=-1) 
			iLineDistance.setValue(values.get(LINEDISTANCE));
		else if (checksEnabled)
			LineDistance.setForeground(Color.GRAY);

		if (checksEnabled)
		{ //if checks are enabled...set the activity and select the checkbox if a value is given
			iLineLength.setEnabled(values.get(LINELENGTH)!=-1);
			bChLineLength.setSelected(values.get(LINELENGTH)!=-1);
		}
		if (values.get(LINELENGTH)!=-1) 
			iLineLength.setValue(values.get(LINELENGTH));
		else if (checksEnabled)
			LineLength.setForeground(Color.GRAY);

		if (checksEnabled)
		{ 
			cLineTypes.setEnabled(setLineType);
			bChLineType.setSelected(setLineType);
			if (!setLineType)
				LineTypes.setForeground(Color.GRAY);
			else
				cLineTypes.setSelectedIndex(values.get(LINETYPE));	
		}
		else
			cLineTypes.setSelectedIndex(values.get(LINETYPE));
		
		setChanged();
		notifyObservers(values);
	}
	
	/**
	 * Verify the Input Fields of the Container.
	 * Returns an empty String if no error occured, else
	 * @return an error message
	 */
	public String VerifyInput()
	{
		//Testen der Linienbedingungen, Felder die nicht ausgefüllt und gleichzeitig nicht benötigt werden, werden auch nicht gesetzt
		int type = cLineTypes.getSelectedIndex()+1;
		if (global||checksEnabled) //no deactivation depending on line style => both must be filled
		{
			if (iLineDistance.getValue()==-1)
			{
				if (checksEnabled)
				{
					if (bChLineDistance.isSelected())
						return "Der Abstand im Linienstil ist nicht angegeben.";
				}
				else
					return "Der Abstand im Linienstil ist nicht angegeben.";
			}
			if (iLineLength.getValue()==-1)
				if (checksEnabled)
				{
					if (bChLineLength.isSelected())
						return "Die Strichl"+main.CONST.html_ae+"nge im Linienstil ist nicht angegeben";
				}
				else
					return "Die Strichl"+main.CONST.html_ae+"nge im Linienstil ist nicht angegeben";
		}
		else if (!global)//ony the values for the chosen type must be checked, if not global the checkboxes are off, so no verifycation for them here
		{
			if (!(type==VEdgeLinestyle.SOLID))
			{
				if (iLineDistance.getValue()==-1)
					return "Der Abstand im Linienstil ist nicht angegeben, wird f"+main.CONST.html_ue+"r den gew"+main.CONST.html_ae+"hlten Stil aber ben"+main.CONST.html_oe+"tigt.";				
				if ((!(type==VEdgeLinestyle.DOTTED))&&(iLineLength.getValue()==-1))
					return "Die Strichl"+main.CONST.html_ae+"nge im Linienstil ist nicht angegeben, wird f"+main.CONST.html_ue+"r den gew"+main.CONST.html_ae+"hlten Stil aber ben"+main.CONST.html_oe+"tigt.";
			}
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
		//Text bauen
		if (checksEnabled)
		{
			VEdgeLinestyle actualline = e.getLinestyle(); //get old style
			if (bChLineDistance.isSelected())
				actualline.setDistance(iLineDistance.getValue());
			if (bChLineLength.isSelected())
				actualline.setLength(iLineLength.getValue());
			if (bChLineType.isSelected())
				actualline.setType(cLineTypes.getSelectedIndex()+1);
			e.setLinestyle(actualline);
		}
		else
		{
			VEdgeLinestyle newline = e.getLinestyle(); //Set every value that is possible in the nonglobal case
			if (global||(iLineLength.getValue()!=-1))
				newline.setLength(iLineLength.getValue());
			if (global||iLineDistance.getValue()!=-1)
				newline.setDistance(iLineDistance.getValue());
			newline.setType(cLineTypes.getSelectedIndex()+1);
			e.setLinestyle(newline);
		}
		return e;
	}

	/**
	 * modifies the given Edge to the Fields in this Container, if the Inputs are all valid
	 * @param e the Edge that should be modified 
	 */
	public VHyperEdge modifyHyperEdge(VHyperEdge e)
	{
		if (!VerifyInput().equals(""))
			return null;
		//Text bauen
		if (checksEnabled)
		{
			VEdgeLinestyle actualline = e.getLinestyle(); //get old style
			if (bChLineDistance.isSelected())
				actualline.setDistance(iLineDistance.getValue());
			if (bChLineLength.isSelected())
				actualline.setLength(iLineLength.getValue());
			if (bChLineType.isSelected())
				actualline.setType(cLineTypes.getSelectedIndex()+1);
			e.setLinestyle(actualline);
		}
		else
		{
			VEdgeLinestyle newline;
			if ((e==null)||(e.getLinestyle()==null))
				newline = new VEdgeLinestyle();
			else
				newline = e.getLinestyle();	//Set every value that is possible in the nonglobal case
			if (global||(iLineLength.getValue()!=-1))
				newline.setLength(iLineLength.getValue());
			if (global||iLineDistance.getValue()!=-1)
				newline.setDistance(iLineDistance.getValue());
			newline.setType(cLineTypes.getSelectedIndex()+1);
			e.setLinestyle(newline);
		}
		return e;
	}

	public void caretUpdate(CaretEvent event) 
	{
		if (event.getSource()==iLineLength)
		{	
			if (iLineLength.getValue()>0)
			{
				values.set(LINELENGTH,iLineLength.getValue());
				setChanged();
				notifyObservers(values);
			}
		}
		else if (event.getSource()==iLineDistance)
		{	
			if (iLineDistance.getValue()>0)
			{
				values.set(LINEDISTANCE,iLineDistance.getValue());
				setChanged();
				notifyObservers(values);
			}
		}
	}
	public void actionPerformed(ActionEvent event) 
	{
		if (event.getSource()==cLineTypes)
		{		
			values.set(LINETYPE, cLineTypes.getSelectedIndex());
			setChanged();
			notifyObservers(values);
			if ((!global)&&(!checksEnabled))
			{
				//The Distance is activated as soon as we're not solid
				iLineDistance.setEnabled((!((cLineTypes.getSelectedIndex()+1)==VEdgeLinestyle.SOLID)));
				if (!((cLineTypes.getSelectedIndex()+1)==VEdgeLinestyle.SOLID))
					LineDistance.setForeground(Color.BLACK);
				else
					LineDistance.setForeground(Color.GRAY);
				boolean line =  ((cLineTypes.getSelectedIndex()+1)==VEdgeLinestyle.DOTDASHED)||((cLineTypes.getSelectedIndex()+1)==VEdgeLinestyle.DASHED);
				iLineLength.setEnabled(line);
				if (line)
					LineLength.setForeground(Color.BLACK);
				else
					LineLength.setForeground(Color.GRAY);
			}
		} 
		else if (event.getSource()==bChLineType)
		{
			cLineTypes.setEnabled(bChLineType.isSelected());
			if (bChLineType.isSelected())
				LineTypes.setForeground(Color.BLACK);
			else
				LineTypes.setForeground(Color.GRAY);
		}
		else if (event.getSource()==bChLineDistance)
		{
			iLineDistance.setEnabled(bChLineDistance.isSelected());
			if (bChLineDistance.isSelected())
				LineDistance.setForeground(Color.BLACK);
			else
				LineDistance.setForeground(Color.GRAY);
		}
		else if (event.getSource()==bChLineLength)
		{
			iLineLength.setEnabled(bChLineLength.isSelected());
			if (bChLineLength.isSelected())
				LineLength.setForeground(Color.BLACK);
			else
				LineLength.setForeground(Color.GRAY);
		}
	}
}