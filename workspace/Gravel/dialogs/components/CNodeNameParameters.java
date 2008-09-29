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

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import model.VNode;

import dialogs.IntegerTextField;
/**
 * A Container for the Inputfields of Node Text Properties
 * and its Verification. Contains also the handling of Actions on these fields 
 * and the modification possibilities of nodes
 * 
 * @author Ronny Bergmann
 *
 */
public class CNodeNameParameters extends Container implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JLabel NodeNameSize, NodeNameDistance, NodeNameRotation, Info;
	private IntegerTextField iNodeNameSize, iNodeNameDistance, iNodeNameRotation;
	private JCheckBox bShowNodeName;
	//Aktivierungsbuttons
	private JCheckBox bChNodeNameSize, bChNodeNameDistance, bChNodeNameRotation, bChShowNodeName;

	boolean checksEnabled;
	VNode chNode;
	/**
	 * Create the NodeNameParameter GUI with Values from v and Checkboxes, if checks is true
	 * @param v the preset Values for the field, choose null for Global Std
	 * @param checks true for activation of JCheckboxes in front of each field, else false
	 */
	public CNodeNameParameters(VNode v, boolean checks)
	{
		super();
		checksEnabled = checks;

		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(7,7,7,7);
		c.anchor = GridBagConstraints.WEST;
		c.gridx=0; c.gridy=0; c.gridwidth=0;

		c.gridy++;
		c.gridx = 0;
		if (checksEnabled)
		{
			bChShowNodeName = new JCheckBox();
			add(bChShowNodeName, c);
			bChShowNodeName.addActionListener(this);
			c.gridx++;
		}
		c.gridwidth=2;
		bShowNodeName = new JCheckBox("Knotenname anzeigen");
		bShowNodeName.addActionListener(this);
		add(bShowNodeName,c);		
		c.gridwidth=1;
		
		c.gridy++;
		c.gridx = 0;
		if (checksEnabled)
		{
			bChNodeNameRotation = new JCheckBox();
			add(bChNodeNameRotation, c);
			bChNodeNameRotation.addActionListener(this);
			c.gridx++;
		}
		NodeNameRotation = new JLabel("<html><p>Textausrichtung</p></html>");
		add(NodeNameRotation,c);
		c.gridx++;
		iNodeNameRotation = new IntegerTextField();
		iNodeNameRotation.setPreferredSize(new Dimension(200, 20));
		add(iNodeNameRotation,c);		

		Info = new JLabel("<html><font size=-1>(0=rechts, 90=oben, 180=links, 270=unten)</font></html>");
		c.gridy++; c.gridwidth = 2; c.gridx=0; 
		if (checksEnabled) c.gridx++;
		add(Info,c);
		
		
		c.gridy++;
		c.gridx = 0;
		if (checksEnabled)
		{
			bChNodeNameDistance = new JCheckBox();
			add(bChNodeNameDistance, c);
			bChNodeNameDistance.addActionListener(this);
			c.gridx++;
		}
		NodeNameDistance = new JLabel("<html><p>Textabstand</p></html>");
		add(NodeNameDistance,c);
		c.gridx++;
		iNodeNameDistance = new IntegerTextField();
		iNodeNameDistance.setPreferredSize(new Dimension(200, 20));
		add(iNodeNameDistance,c);		

		c.gridy++;
		c.gridx = 0;
		if (checksEnabled)
		{
			bChNodeNameSize = new JCheckBox();
			add(bChNodeNameSize, c);
			bChNodeNameSize.addActionListener(this);
			c.gridx++;
		}
		NodeNameSize = new JLabel("<html><p>Textgr"+main.CONST.html_oe+main.CONST.html_sz+"e</p></html>");
		add(NodeNameSize,c);
		c.gridx++;
		iNodeNameSize = new IntegerTextField();
		iNodeNameSize.setPreferredSize(new Dimension(200, 20));
		add(iNodeNameSize,c);	
		InitValues(v,true);
	}
	/**
	 * Initialize the values of the Inputfields. Choose -1 for Integers in the VNode to disable a Value,
	 * if the Checkboxes are enabled.
	 * @param v VNode containing the Values for Initializeation
	 * @param setVisibility set true, if the NodeNameVisibility should be activated, else false. Has no effect, if checks are disabled
	 */
	public void InitValues(VNode v, boolean setVisibility)
	{
		chNode = v;
		if (chNode==null)
		{
			GeneralPreferences gp = GeneralPreferences.getInstance();
			iNodeNameRotation.setValue(gp.getIntValue("node.name_rotation"));
			iNodeNameDistance.setValue(gp.getIntValue("node.name_distance"));
			iNodeNameSize.setValue(gp.getIntValue("node.name_size"));
			bShowNodeName.setSelected(!gp.getBoolValue("node.name_visible"));
			bShowNodeName.doClick();
		}
		else
		{
			if (checksEnabled)
			{ //if checks are enabled...set the activity and select the checkbox if a value is given
				iNodeNameRotation.setEnabled((chNode.getNameRotation()!=-1));
				bChNodeNameRotation.setSelected((chNode.getNameRotation()!=-1));
			}
			if (chNode.getNameRotation()!=-1) 
				iNodeNameRotation.setValue(chNode.getNameRotation());
			else if (checksEnabled)
				NodeNameRotation.setForeground(Color.GRAY);
			
			if (checksEnabled)
			{
				iNodeNameDistance.setEnabled((chNode.getNameDistance()!=-1));
				bChNodeNameDistance.setSelected((chNode.getNameDistance()!=-1));
			}
			if (chNode.getNameDistance()!=-1)
				iNodeNameDistance.setValue(chNode.getNameDistance());
			else if (checksEnabled)
				NodeNameDistance.setForeground(Color.GRAY);
			
			if (checksEnabled)
			{
				iNodeNameSize.setEnabled((chNode.getNameSize()!=-1));
				bChNodeNameSize.setSelected((chNode.getNameSize()!=-1));
			}
			if (chNode.getNameSize()!=-1)
				iNodeNameSize.setValue(chNode.getNameSize());
			else if (checksEnabled)
				NodeNameSize.setForeground(Color.GRAY);
			
			if (checksEnabled)
			{
				bShowNodeName.setEnabled(setVisibility);
				bChShowNodeName.setSelected(setVisibility);
			}
			if (setVisibility)
			{
				bShowNodeName.setSelected(!chNode.isNameVisible());
				bShowNodeName.doClick();
			}
			else
			{ //disable
				
			}
		}
	}
	/**
	 * Verify the Input fields
	 * if Checks are disabled, each Integer Textfield must contain a valid Value (>0)
	 * if Checks are enabled, each activated Integer Textfield must be valid
	 * @return An Errormessage if an error occured, else an empty String
	 */
	public String VerifyInput()
	{
		if (iNodeNameSize.getValue()==-1)
		{ //field not filled
			if (!checksEnabled)
				return "Die neue Textgr"+main.CONST.html_oe+main.CONST.html_sz+"e der Knoten wurde nicht angegeben.";
			else if (bChNodeNameSize.isSelected()) //Checks enabled and the Value should be changed -> error
				return "Die neue Textgr"+main.CONST.html_oe+main.CONST.html_sz+"e der Knoten wurde nicht angegeben.";
		}
		if (iNodeNameDistance.getValue()==-1)
		{
			if (!checksEnabled)
				return "Der neue Textabstand der Knoten wurde nicht angegeben.";
			else if (bChNodeNameDistance.isSelected())
			return "Der neue Textabstand der Knoten wurde nicht angegeben.";
			
		}
		if (iNodeNameRotation.getValue()==-1)
		{
			if (!checksEnabled)
				return "Die neue Textausrichtung der Knoten wurde nicht angegeben.";
			else if (bChNodeNameRotation.isSelected())
				return "Die neue Textausrichtung der Knoten wurde nicht angegeben.";
		}
		return "";
	}
	/**
	 * Modify the Node specified by Parameter n, if the Fields are valid. If Checks are active only the selected Fields are changed
	 * @param n the Node that should be modified
	 * @return the modified node if fields are valid, else null
	 */
	public VNode modifyNode(VNode n)
	{
		if (!VerifyInput().equals(""))
			return null;
		if (checksEnabled)
		{
			if (bChNodeNameDistance.isSelected())
				n.setNameDistance(iNodeNameDistance.getValue());
			if (bChNodeNameSize.isSelected())
				n.setNameSize(iNodeNameSize.getValue());
			if (bChNodeNameRotation.isSelected())
				n.setNameRotation(iNodeNameRotation.getValue());
			if (bChShowNodeName.isSelected())
				n.setNameVisible(bShowNodeName.isSelected());			
		}
		else
		{
				n.setNameDistance(iNodeNameDistance.getValue());
				n.setNameSize(iNodeNameSize.getValue());
				n.setNameRotation(iNodeNameRotation.getValue()%360);
				n.setNameVisible(bShowNodeName.isSelected());
		}
		return n;
	}
	
	public void actionPerformed(ActionEvent event) {
 		if (event.getSource()==bShowNodeName)
		{
 			if (!checksEnabled)
 			{
 				iNodeNameRotation.setEnabled(bShowNodeName.isSelected());
 				iNodeNameDistance.setEnabled(bShowNodeName.isSelected());
 				iNodeNameSize.setEnabled(bShowNodeName.isSelected());
 				if (bShowNodeName.isSelected())
 				{
 					NodeNameDistance.setForeground(Color.BLACK);
					NodeNameRotation.setForeground(Color.BLACK);
 					Info.setForeground(Color.BLACK);
 					NodeNameSize.setForeground(Color.BLACK);
 				}
 				else
 				{
 					NodeNameDistance.setForeground(Color.GRAY);
					NodeNameRotation.setForeground(Color.GRAY);
 					Info.setForeground(Color.GRAY);
 					NodeNameSize.setForeground(Color.GRAY); 					
 				}
 			}
		}

		else if (event.getSource()==bChShowNodeName)
		{
			bShowNodeName.setEnabled(bChShowNodeName.isSelected());
			if (bChShowNodeName.isSelected())
				bShowNodeName.setForeground(Color.BLACK);
			else
				bShowNodeName.setForeground(Color.GRAY);
		}
		else if (event.getSource()==bChNodeNameRotation)
		{
			iNodeNameRotation.setEnabled(bChNodeNameRotation.isSelected());
			if (bChNodeNameRotation.isSelected())
			{
				NodeNameRotation.setForeground(Color.BLACK);
				Info.setForeground(Color.BLACK);
			}
			else
			{
				NodeNameRotation.setForeground(Color.GRAY);
				Info.setForeground(Color.GRAY);
			}
		}
		else if (event.getSource()==bChNodeNameDistance)
		{
			iNodeNameDistance.setEnabled(bChNodeNameDistance.isSelected());
			if (bChNodeNameDistance.isSelected())
				NodeNameDistance.setForeground(Color.BLACK);
			else
				NodeNameDistance.setForeground(Color.GRAY);
		}
		else if (event.getSource()==bChNodeNameSize)
		{
			iNodeNameSize.setEnabled(bChNodeNameSize.isSelected());
			if (bChNodeNameSize.isSelected())
				NodeNameSize.setForeground(Color.BLACK);
			else
				NodeNameSize.setForeground(Color.GRAY);
		}		
	}
	

}
