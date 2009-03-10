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

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import view.Gui;

import dialogs.IntegerTextField;

import model.VEdge;
import model.VEdgeText;
import model.VHyperEdge;
/**
 * Container for the Parameters Fields of the Text Properties of an edge
 * @author ronny
 *
 */
public class CEdgeTextParameters extends Observable implements ActionListener, CaretListener
{
	public static final int CEDGETEXTPARAMETERS = 26666; 
	public static final int BSHOWTEXTVALUE=1, TEXTPOSITION=2, TEXTDISTANCE=3, BTEXTVISIBLE=4, TEXTSIZE=5;  
	private JCheckBox bShowText;
	private ButtonGroup TextShowChoice;
	private JRadioButton rShowValue, rShowText;
	private IntegerTextField iTextPosition, iTextDistance, iTextSize;
	private JLabel TextPosition, TextDistance, TextSize, TextChoice,TextInfo;
	private JCheckBox bChShowText, bChTextChoice, bChTextPosition, bChTextDistance, bChTextSize; ;
	
	private Vector<Integer> values;
	private Container TextContent;
	boolean global, checksEnabled;
	/**
	 * Initialize the Parameter GUI with the Values of the arrow of Edge e
	 * Choose null for General Preferences
	 * @param t The TextStyle to be modified
	 */
	public CEdgeTextParameters(VEdgeText t, boolean g, boolean checks)
	{
		global = (!checks&&global);
		checksEnabled = checks;
		TextContent = new Container();
		TextContent.setLayout(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(4,7,4,7);
		c.anchor = GridBagConstraints.WEST;
		c.gridy = 0;
		c.gridx = 0;
		if (checksEnabled)
		{
			bChShowText = new JCheckBox();
			TextContent.add(bChShowText,c);
			bChShowText.addActionListener(this);
			c.gridx++;
		}
		c.gridwidth=2;
		bShowText = new JCheckBox("Kantenbeschriftung anzeigen");
		bShowText.addActionListener(this);
		TextContent.add(bShowText,c);
		c.gridwidth=1;
		
		c.gridy++;
		c.gridx=0;
		if (checksEnabled)
		{
			bChTextPosition = new JCheckBox();
			TextContent.add(bChTextPosition,c);
			bChTextPosition.addActionListener(this);
			c.gridx++;
		}		
		iTextPosition = new IntegerTextField();
		iTextPosition.addCaretListener(this);
		iTextPosition.setPreferredSize(new Dimension(100, 20));
		TextPosition = new JLabel("<html><p>Position</html>");
		TextContent.add(TextPosition,c);
		c.gridx++;
		TextContent.add(iTextPosition,c);
		c.gridx = 0; if (checksEnabled) c.gridx++;
		c.gridy++; c.gridwidth=2;
		TextInfo = new JLabel("<html><p><font size=\"-2\">0 \u2264 p \u2264 50: 90 Grad gegen den Uhrzeigersinn, Start->Ende<br>51 \u2264 p \u2264 100 : 90 Grad im Uhrzeigersinn, Ende->Start</font></p></html>");
		TextContent.add(TextInfo,c);
		
		c.gridwidth=1;
		c.gridy++;
		c.gridx = 0;
		if (checksEnabled)
		{
			bChTextDistance = new JCheckBox();
			TextContent.add(bChTextDistance,c);
			bChTextDistance.addActionListener(this);
			c.gridx++;
		}				
		iTextDistance = new IntegerTextField();
		iTextDistance.addCaretListener(this);
		iTextDistance.setPreferredSize(new Dimension(100, 20));
		TextDistance = new JLabel("<html><p>Abstand<br><font size=\"-2\">von der Kante</font></p></html>");
		TextContent.add(TextDistance,c);		
		
		c.gridx++;
		TextContent.add(iTextDistance,c);
		
		c.gridy++;
		c.gridx = 0;
		if (checksEnabled)
		{
			bChTextSize = new JCheckBox();
			TextContent.add(bChTextSize,c);
			bChTextSize.addActionListener(this);
			c.gridx++;
		}		
		iTextSize = new IntegerTextField();
		iTextSize.addCaretListener(this);
		iTextSize.setPreferredSize(new Dimension(100, 20));
		TextSize = new JLabel("<html><p>Textgr"+main.CONST.html_oe+main.CONST.html_sz+"e<br><font size=\"-2\">in Pixeln</font></p></html>");
		TextContent.add(TextSize,c);
		c.gridx++;
		TextContent.add(iTextSize,c);
		
		c.gridy++;
		c.gridx = 0;
		if (checksEnabled)
		{
			bChTextChoice = new JCheckBox();
			TextContent.add(bChTextChoice,c);
			bChTextChoice.addActionListener(this);
			c.gridx++;
		}		
		TextShowChoice = new ButtonGroup();
		rShowValue = new JRadioButton("Gewicht");
		rShowValue.addActionListener(this);
		rShowText = new JRadioButton("Name");
		rShowText.addActionListener(this);
		TextShowChoice.add(rShowValue); TextShowChoice.add(rShowText);
		c.gridwidth=2;
		TextChoice = new JLabel("Anzeige von");
		TextContent.add(TextChoice,c);
		
		c.gridwidth=1;
		c.gridy++;
		TextContent.add(rShowValue,c);
		c.gridx++;
		TextContent.add(rShowText,c);
		TextContent.validate();
		values = new Vector<Integer>(5);
		values.setSize(6);
		values.set(0,CEdgeTextParameters.CEDGETEXTPARAMETERS);
		InitValues(t);
	}
	/**
	 * get the GUI-Content
	 * @return
	 */
	public Container getContent()
	{
		return TextContent;
	}
	/**
	 * Initialize the Fields to the edge e
	 * Choose null for general Preferences
	 * 
	 * @param e
	 */
	public void InitValues(VEdgeText e)
	{
		if (e!=null)
		{
			//	public static final int BSHOWTEXT=1, TEXTPOSITION=2, TEXTDISTANCE=3, BTEXTCHOICE=4, BTEXTVISIBLE=5;  

			if (e.isshowvalue())
				values.set(BSHOWTEXTVALUE, 1);
			else
				values.set(BSHOWTEXTVALUE,0);
			
			values.set(TEXTPOSITION, e.getPosition());
			values.set(TEXTDISTANCE, e.getDistance());
			values.set(TEXTSIZE, e.getSize());
			
			if (e.isVisible())
				values.set(BTEXTVISIBLE, 1);
			else
				values.set(BTEXTVISIBLE,0);	
		}
		else
		{
			GeneralPreferences gp = GeneralPreferences.getInstance();
			if (gp.getBoolValue("edge.text_showvalue"))
				values.set(BSHOWTEXTVALUE, 1);
			else
				values.set(BSHOWTEXTVALUE,0);
			
			values.set(TEXTPOSITION, gp.getIntValue("edge.text_position"));
			values.set(TEXTDISTANCE, gp.getIntValue("edge.text_distance"));
			values.set(TEXTSIZE, gp.getIntValue("edge.text_size"));
			
			if (gp.getBoolValue("edge.text_visible"))
				values.set(BTEXTVISIBLE, 1);
			else
				values.set(BTEXTVISIBLE,0);
		}
		updateUI(true,true);
	}
	/**
	 * Update the UI to the Values Vector
	 *
	 */
	public void updateUI(boolean setVisible, boolean setShowValue)
	{
		if (checksEnabled)
		{ //if checks are enabled...set the activity and select the checkbox if a value is given
			iTextPosition.setEnabled(values.get(TEXTPOSITION)!=-1);
			bChTextPosition.setSelected(values.get(TEXTPOSITION)!=-1);
		}
		if (values.get(TEXTPOSITION)!=-1) 
			iTextPosition.setValue(values.get(TEXTPOSITION));
		else if (checksEnabled)
		{
			TextPosition.setForeground(Color.GRAY);
			TextInfo.setForeground(Color.GRAY);
		}
		if (checksEnabled)
		{ //if checks are enabled...set the activity and select the checkbox if a value is given
			iTextDistance.setEnabled(values.get(TEXTDISTANCE)!=-1);
			bChTextDistance.setSelected(values.get(TEXTDISTANCE)!=-1);
		}
		if (values.get(TEXTDISTANCE)!=-1) 
			iTextDistance.setValue(values.get(TEXTDISTANCE));
		else if (checksEnabled)
			TextDistance.setForeground(Color.GRAY);

		if (checksEnabled)
		{ //if checks are enabled...set the activity and select the checkbox if a value is given
			iTextSize.setEnabled(values.get(TEXTSIZE)!=-1);
			bChTextSize.setSelected(values.get(TEXTSIZE)!=-1);
		}
		if (values.get(TEXTSIZE)!=-1) 
			iTextSize.setValue(values.get(TEXTSIZE));
		else if (checksEnabled)
			TextSize.setForeground(Color.GRAY);
		
		if (checksEnabled)
		{
			bChShowText.setSelected(setVisible);
			bShowText.setEnabled(setVisible);
			if (setVisible)
				bShowText.setSelected(values.get(BTEXTVISIBLE)==1);
			
			bChTextChoice.setSelected(setShowValue);
			rShowValue.setEnabled(setShowValue);
			rShowValue.setEnabled(setShowValue);
			if (setShowValue);
			{
				rShowValue.setSelected(values.get(BSHOWTEXTVALUE)==1);
				rShowText.setSelected(values.get(BSHOWTEXTVALUE)==0);
			}
		}
		else
		{
			rShowValue.setSelected(values.get(BSHOWTEXTVALUE)==1);
			rShowText.setSelected(values.get(BSHOWTEXTVALUE)==0);
			bShowText.setSelected(values.get(BTEXTVISIBLE)==1);
		}
		if (!(global||checksEnabled))
		{
			bShowText.setSelected(!(values.get(BTEXTVISIBLE)==1));
			bShowText.doClick();
		}
		TextContent.validate();
		setChanged();
		notifyObservers(values);
	}
	
	/**
	 * Verify the Input Fields of the Container.
	 * Returns an empty String if no error occured, else
	 * @return an error message
	 */
	public String VerifyInput(String edgename)
	{
		//The Cases where a wrong value is important
		//(1) global or
		//(2) !global && bShowText is Selected or 
		//(3) ChecksEnabel && ownCheckEnabled
		boolean pre = global||((!global)&&bShowText.isSelected());
		String t="";
		if (iTextPosition.getValue()==-1)
			t="Die Position des Kantentextes wurde nicht angegeben.";
		if ((iTextPosition.getValue()<0)||(iTextPosition.getValue()>100))
			t="Die Position des Textes ist ung"+main.CONST.html_ue+"ltig. Der Wert muss zwischen (inklusive) 0 und 100 liegen.";				
		if (!t.equals(""))
		{	
			if (pre)
				return t;
			else if (checksEnabled)
				if (bChTextPosition.isSelected())
					return t;
		}
		if (iTextDistance.getValue()==-1)
		{
			if (pre)
				return "Der Abstand des Textes von der Kante wurde nicht angegeben.";
			else if (checksEnabled)
				if (bChTextPosition.isSelected())
					return "Der Abstand des Textes von der Kante wurde nicht angegeben.";			
		}
		if (iTextSize.getValue()==-1)
		{
			if (pre)
				return "Die Textgr"+main.CONST.html_oe+main.CONST.html_sz+"e des Textes nicht angegeben.";
			else if (checksEnabled)
				if (bChTextPosition.isSelected())
					return "Die Textgr"+main.CONST.html_oe+main.CONST.html_sz+"e des Textes nicht angegeben.";
		}
		if (rShowText.isSelected()&&(edgename.equals("")))
		{
			boolean ask=pre;
			if (checksEnabled)
				if (bChTextChoice.isSelected())
					ask = true;
			if (ask)
			{
				int n = JOptionPane.showConfirmDialog(Gui.getInstance().getParentWindow(), "<html>Die Textanzeige des Kantennamens ist aktiviert, dieser ist jedoch leer.<br><br>Mit \"OK\" wird die Anzeige eines leeren Namens eingestellt.</html>","Warnung : leere Textanzeige",JOptionPane.OK_CANCEL_OPTION);
				if (n==JOptionPane.CANCEL_OPTION)
				{
					return "Bitte einen Textnamen angeben, oder die Anzeige auf den Kantenwert "+main.CONST.html_ae+"ndern.";
				}
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
		if (!VerifyInput("a").equals(""))//do not check empty Text again
			return null;
		//Text bauen

		if (checksEnabled)
		{
			VEdgeText t = e.getTextProperties();
			if (bChShowText.isSelected())
				t.setVisible(bShowText.isSelected());
			if (bChTextChoice.isSelected())
				t.setshowvalue(rShowValue.isSelected());
			if (bChTextPosition.isSelected())
				t.setPosition(iTextPosition.getValue());
			if (bChTextDistance.isSelected())
				t.setDistance(iTextDistance.getValue());
			if (bChTextSize.isSelected())
				t.setSize(iTextSize.getValue());
			e.setTextProperties(t); //notwenidg ?
		}
		else
		{
			VEdgeText newtext = new VEdgeText(iTextDistance.getValue(),iTextPosition.getValue(), iTextSize.getValue(), bShowText.isSelected(), rShowValue.isSelected());
			e.setTextProperties(newtext);
		}
		return e;
	}
	/**
	 * modifies the given Edge to the Fields in this Container, if the Inputs are all valid
	 * @param e the Edge that should be modified 
	 */
	public VHyperEdge modifyHyperEdge(VHyperEdge e)
	{
		if (!VerifyInput("a").equals(""))//do not check empty Text again
			return null;
		//Text bauen

		if (checksEnabled)
		{
			VEdgeText t = e.getTextProperties();
			if (bChShowText.isSelected())
				t.setVisible(bShowText.isSelected());
			if (bChTextChoice.isSelected())
				t.setshowvalue(rShowValue.isSelected());
			if (bChTextPosition.isSelected())
				t.setPosition(iTextPosition.getValue());
			if (bChTextDistance.isSelected())
				t.setDistance(iTextDistance.getValue());
			if (bChTextSize.isSelected())
				t.setSize(iTextSize.getValue());
			e.setTextProperties(t); //notwenidg ?
		}
		else
		{
			VEdgeText newtext = new VEdgeText(iTextDistance.getValue(),iTextPosition.getValue(), iTextSize.getValue(), bShowText.isSelected(), rShowValue.isSelected());
			e.setTextProperties(newtext);
		}
		return e;
	}
	
	public void caretUpdate(CaretEvent event) 
	{
		if (event.getSource()==iTextSize)
		{	
			if (iTextSize.getValue()>0)
			{
				values.set(TEXTSIZE,iTextSize.getValue());
				setChanged();
				notifyObservers(values);
			}
		}
		else if (event.getSource()==iTextDistance)
		{	
			if (iTextDistance.getValue()>0)
			{
				values.set(TEXTDISTANCE,iTextDistance.getValue());
				setChanged();
				notifyObservers(values);
			}
		}
		else if (event.getSource()==iTextPosition)
		{	
			if (iTextPosition.getValue()>0)
			{
				values.set(TEXTPOSITION,iTextPosition.getValue());
				setChanged();
				notifyObservers(values);
			}
		} 
	}
	public void actionPerformed(ActionEvent event) {
		if (event.getSource()==bShowText)
		{
			if (!global)
			{
				iTextPosition.setEnabled(bShowText.isSelected());
				iTextDistance.setEnabled(bShowText.isSelected());
				iTextSize.setEnabled(bShowText.isSelected());
				rShowValue.setEnabled(bShowText.isSelected());
				rShowText.setEnabled(bShowText.isSelected());
				Color c;
				if (bShowText.isSelected())
					c = Color.BLACK;
				else
					c = Color.GRAY;
				TextPosition.setForeground(c);
				TextDistance.setForeground(c);
				TextSize.setForeground(c);
				TextChoice.setForeground(c);
				TextInfo.setForeground(c);
			}
			if (bShowText.isSelected())
				values.set(BTEXTVISIBLE,1);
			else
				values.set(BTEXTVISIBLE,0);
			setChanged();
			notifyObservers(values);
		}
		if ((event.getSource()==rShowText)||(event.getSource()==rShowValue))
		{
			if (rShowText.isSelected())
				values.set(BSHOWTEXTVALUE, 0);
			else
				values.set(BSHOWTEXTVALUE, 1);
			setChanged();
			notifyObservers(values);
		}
		if (checksEnabled) //Activation & Deactivation
		{
			if (event.getSource()==bChShowText)
			{
				bShowText.setEnabled(bChShowText.isSelected());
			}

			else if (event.getSource()==bChTextPosition)
			{
				iTextPosition.setEnabled(bChTextPosition.isSelected());
				if (bChTextPosition.isSelected())
				{
					TextPosition.setForeground(Color.BLACK);
					TextInfo.setForeground(Color.BLACK);					
				}
				else
				{
					TextPosition.setForeground(Color.GRAY);
					TextInfo.setForeground(Color.GRAY);
				}
			}
			else if (event.getSource()==bChTextDistance)
			{
				iTextDistance.setEnabled(bChTextDistance.isSelected());
				if (bChTextDistance.isSelected())
					TextDistance.setForeground(Color.BLACK);
				else
					TextDistance.setForeground(Color.GRAY);
			}
			else if (event.getSource()==bChTextSize)
			{
				iTextSize.setEnabled(bChTextSize.isSelected());
				if (bChTextSize.isSelected())
					TextSize.setForeground(Color.BLACK);
				else
					TextSize.setForeground(Color.GRAY);
			}
			else if (event.getSource()==bChTextChoice)
			{
				rShowValue.setEnabled(bChTextChoice.isSelected());
				rShowText.setEnabled(bChTextChoice.isSelected());
				if (bChTextChoice.isSelected())
					TextChoice.setForeground(Color.BLACK);
				else
					TextChoice.setForeground(Color.GRAY);
			}
		}
	}
}