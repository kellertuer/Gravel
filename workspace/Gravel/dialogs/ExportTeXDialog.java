package dialogs;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.ButtonGroup;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.KeyStroke;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

/**
 * Class for the Dialog, that displays properties of the TeX-Export
 * 
 * @author Ronny Bergmann
 */
public class ExportTeXDialog extends JDialog implements ActionListener, CaretListener
{
	private IntegerTextField iSizeX,iSizeY;
	private FloatTextField iSizeU;
	private JButton bOk, bCancel;
	private JRadioButton selX,selY,selU;
	private ButtonGroup DocumentChoice, TypeChoice;
	private JRadioButton rWholeDocument, rOnlyFigure, rPlainTeX, rTikZ;
	private boolean accepted = false;
	private int origx,origy;
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Initialize the Dialog to a parent Frame with a given picture size of the TeX-Picture (in mm)
	 * 
	 * @param parent frame deactivated by this modal fra,e
	 * @param ox horizontal and
	 * @param oy vertical preset values for the input fields
	 */
	public ExportTeXDialog(Frame parent, int ox, int oy)
	{
		super(parent,"Einstellungen für TeX-Export");
		origx = ox; origy = oy;
		Container content = getContentPane();
		GridBagConstraints c = new GridBagConstraints();
		Container ContentPane = this.getContentPane();
		ContentPane.removeAll();
		ContentPane.setLayout(new GridBagLayout());
		c.insets = new Insets(7,7,7,7);
		c.anchor = GridBagConstraints.WEST;
		c.gridy=0;
		c.gridx = 0;
		c.gridwidth=3;
		content.add(new JLabel("<html>Bitte eine Gr"+main.CONST.html_oe+main.CONST.html_sz+"e angeben. Aus einem Wert ergeben sich die anderen beiden.<br><b>Originalgr"+main.CONST.html_oe+main.CONST.html_sz+"e (Graph ohne Rand): </b> "+ox+"px "+main.CONST.html_times+" "+oy+"px</html>"),c);
		c.gridy++;
		c.gridwidth=1;
		selX = new JRadioButton("Breite (mm) :");
		selX.addActionListener(this);
		content.add(selX,c);
		c.gridx = 1;
		c.gridwidth=2;
		iSizeX = new IntegerTextField();
		iSizeX.setPreferredSize(new Dimension(200, 20));
		iSizeX.addCaretListener(this);
		content.add(iSizeX,c);
		c.gridwidth=1;
		c.gridy++;
		c.gridx = 0;
		selY = new JRadioButton("<html>H"+main.CONST.html_oe+"he (mm):</html>");
		selY.addActionListener(this);
		content.add(selY,c);
		c.gridx = 1;
		c.gridwidth=2;
		iSizeY = new IntegerTextField();
		iSizeY.setPreferredSize(new Dimension(200, 20));
		iSizeY.addCaretListener(this);
		content.add(iSizeY,c);
		c.gridy++;
		c.gridx = 0;
		selU = new JRadioButton("<html>Aufl"+main.CONST.html_oe+"sung (px pro mm):</html>");
		selU.addActionListener(this);
		content.add(selU,c);
		c.gridx = 1;
		c.gridwidth=2;
		iSizeU = new FloatTextField();
		iSizeU.setPreferredSize(new Dimension(200, 20));
		iSizeU.addCaretListener(this);
		content.add(iSizeU,c);
		
		c.gridy++;
		c.gridwidth=2;
		c.gridx=0;
		content.add(new JLabel("<html>Art des Exports</html>"),c);
		c.gridwidth=1;
		c.gridy++;
		c.gridx=0;
		DocumentChoice = new ButtonGroup();
		rWholeDocument = new JRadioButton("Gesamtes TeX-Dokument");
		rOnlyFigure = new JRadioButton("<html>nur die <i>figure</i>-Umgebung");
		DocumentChoice.add(rWholeDocument);
		DocumentChoice.add(rOnlyFigure);
		rOnlyFigure.setSelected(true);
		content.add(rWholeDocument,c);
		c.gridx++;
		content.add(rOnlyFigure,c);
		c.gridy++;
		c.gridx = 0;
		TypeChoice = new ButtonGroup();
		rPlainTeX = new JRadioButton("TeX-Picture (epic,eepic)");
		rTikZ = new JRadioButton("TikZ-Export");
		TypeChoice.add(rPlainTeX);
		TypeChoice.add(rTikZ);
		rPlainTeX.setSelected(true);
		content.add(rPlainTeX,c);
		c.gridx++;
		content.add(rTikZ,c);		
		c.gridy++;
		c.gridx = 0;
		c.gridwidth=3;
		content.add(new JLabel("<html><b>Achtung</b> : Im normalen TeX-Picture-Export werden Farben ignoriert.<br>Schriftgr"+main.CONST.html_oe+main.CONST.html_sz+"en k"+main.CONST.html_oe+"nnen leicht varriieren, da TeX einen anderen Font verwendet.<br>Im TikZ-Export werden Schriftgrößen ignoriert.</html>"),c);
		c.gridy++;
		c.gridx = 1;
		ButtonGroup b = new ButtonGroup();
		b.add(selX); b.add(selY); b.add(selU);
		c.gridwidth=1;
		c.anchor = GridBagConstraints.EAST;
		bCancel = new JButton("Abbrechen");
		bCancel.addActionListener(this);
		content.add(bCancel,c);	
		//Add ESC-Handling
		InputMap iMap = getRootPane().getInputMap(	 JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape");

		ActionMap aMap = getRootPane().getActionMap();
		aMap.put("escape", new AbstractAction()
			{
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e)
				{	
					accepted = false;
					dispose();
				}
		 	});
		c.gridx = 2;
		bOk = new JButton("Speichern");
		bOk.addActionListener(this);
		content.add(bOk,c);
	
		this.getRootPane().setDefaultButton(bOk);
		selX.doClick();
		iSizeX.setValue(70);
		setResizable(false);
		this.setModal(true);
		pack();
		Point p = new Point(0,0);
		p.y += Math.round(parent.getHeight()/2);
		p.x += Math.round(parent.getWidth()/2);
		p.y -= Math.round(getHeight()/2);
		p.x -= Math.round(getWidth()/2);
		setLocation(p.x,p.y);
		this.setVisible(true);
	}
	/**
	 * @return the horizontal pictrue size in mm
	 */
	public int getSizeX()
	{
		return iSizeX.getValue();
	}
	/**
	 * @return the vertical pictrue size in mm
	 */
	public int getSizeY()
	{
		return iSizeY.getValue();
	}
	/**
	 * Indicated whether the user acceted his data in the end
	 * 
	 * @return true, if the user pressed OK, else false
	 */
	public boolean IsAccepted()
	{
		return accepted;
	}
	/**
	 * Get the Value, whether the user wants the whole document exported or not
	 * 
	 * @return true, if the whole document should be created
	 */
	public boolean IsWholeDocument()
	{
		return rWholeDocument.isSelected();
	}
	/**
	 * Indicates, whether only the figure should be created in the tex-File
	 * 
	 * @return true, if only the figure must be created in the tex-File
	 */
	public boolean IsOnlyFigure()
	{
		return rOnlyFigure.isSelected();
	}
	public boolean IsPlainTeX()
	{
		return rPlainTeX.isSelected();
	}
	public boolean IsTikZ()
	{
		return rTikZ.isSelected();
	}
	/**
	 * Handle actions of the (radio-)buttons
	 */
	public void actionPerformed(ActionEvent e) 
	{
		if ((e.getSource()==selX)||(e.getSource()==selY)||(e.getSource()==selU))
		{
			iSizeX.setEnabled(selX.isSelected());
			iSizeY.setEnabled(selY.isSelected());
			iSizeU.setEnabled(selU.isSelected());
		}
		else
		if (e.getSource()==bOk)
		{
			if ((iSizeX.getValue()==-1)||(iSizeY.getValue()==-1)||(iSizeU.getValue()==-1))
			{
				JOptionPane.showMessageDialog(this, "<html><p>Bitte eine positive Zahl angeben.</p></hmtl>", "Fehler", JOptionPane.ERROR_MESSAGE);
			}
			else
			{
				accepted = true;
				dispose();
			}
		}
		else if (e.getSource()==bCancel)
		{
			accepted = false;
			dispose();
		}
	}
	/**
	 * handle caret updates in the integer text fields
	 */
	public void caretUpdate(CaretEvent e) {
		 if ((e.getSource()==iSizeX)||(e.getSource()==iSizeY)||(e.getSource()==iSizeU))
			{
				if ((selX.isSelected())&&(iSizeX.getValue()!=-1))
				{
					iSizeY.removeCaretListener(this); //So it doesn't ivoke itself
					iSizeY.setValue(Math.round((float)iSizeX.getValue()*(float)origy/(float)origx));
					iSizeY.addCaretListener(this); //So it doesn't ivoke itself
					iSizeU.removeCaretListener(this); //So it doesn't ivoke itself
					iSizeU.setValue((float)iSizeX.getValue()/(float)origx);
					iSizeU.addCaretListener(this); //So it doesn't ivoke itself
				}
				if ((selY.isSelected())&&(iSizeY.getValue()!=-1))
				{
					iSizeX.removeCaretListener(this); //So it doesn't evoke itself
					iSizeX.setValue(Math.round((float)iSizeY.getValue()*(float)origx/(float)origy));
					iSizeX.addCaretListener(this);
					iSizeU.removeCaretListener(this); //So it doesn't ivoke itself
					iSizeU.setValue((float)iSizeX.getValue()/(float)origx);
					iSizeU.addCaretListener(this); //So it doesn't ivoke itself
				}
				if ((selU.isSelected())&&(iSizeU.getValue()!=-1d))
				{
					iSizeX.removeCaretListener(this); //So it doesn't evoke itself
					iSizeX.setValue(Math.round(iSizeU.getValue()*(float)origx));
					iSizeX.addCaretListener(this);
					iSizeY.removeCaretListener(this); //So it doesn't ivoke itself
					iSizeY.setValue(Math.round((float)iSizeX.getValue()*(float)origy/(float)origx));
					iSizeY.addCaretListener(this); //So it doesn't ivoke itself
				}
			}
	}	
}