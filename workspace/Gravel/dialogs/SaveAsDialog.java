package dialogs;

import io.GeneralPreferences;

import java.awt.Container;
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
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;
/**
 * A Dialog for Save as, where the properties of the GraphML Format are determined
 * - visual or mathematical file format
 * @author ronny
 *
 */
public class SaveAsDialog extends JDialog implements ActionListener
{
	private JCheckBox IsVisual;
	private JButton bOk, bCancel;
	private boolean accepted = false;
	private static final long serialVersionUID = 1L;
	/**
	 * Init the SaveAsDialog on top of a parent window
	 * @param parent
	 */
	public SaveAsDialog(Frame parent)
	{
		super(parent,"Einstellungen für GravelML");
		Container content = getContentPane();
		GridBagConstraints c = new GridBagConstraints();
		Container ContentPane = this.getContentPane();
		ContentPane.removeAll();
		ContentPane.setLayout(new GridBagLayout());
		c.insets = new Insets(7,7,7,7);
		c.anchor = GridBagConstraints.CENTER;
		
		IsVisual = new JCheckBox("visuelle Informationen speichern");
		IsVisual.setSelected((GeneralPreferences.getInstance().getStringValue("graph.fileformat").equals("visual")));

		c.gridy=0;c.gridx=0;c.gridwidth=2;
		ContentPane.add(IsVisual,c);
		c.insets = new Insets(7,7,7,7);
		c.gridwidth = 1;
		
		c.gridy++;
		c.gridx = 0;
		c.anchor = GridBagConstraints.WEST;
		c.gridy++;
		c.gridx = 0;
		bCancel = new JButton("Abbrechen");
		bCancel.addActionListener(this);
		content.add(bCancel,c);
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


		c.gridx = 1;
		bOk = new JButton("Speichern");
		bOk.addActionListener(this);
		content.add(bOk,c);
		
		this.getRootPane().setDefaultButton(bOk);
		
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
	 * Indicates whether a mathematical or a visual file should be created
	 * @return true if the file should be visual
	 */
	public boolean IsVisual()
	{
		return IsVisual.isSelected();
	}
	/**
	 * Indicates whether the user accepted or canceled the file save as dialog
	 * @return true if accepted, else false
	 */
	public boolean IsAccepted()
	{
		return accepted;
	}
	public void actionPerformed(ActionEvent e) 
	{
		if (e.getSource()==bOk)
		{
			accepted = true;
			dispose();
		}
		else if (e.getSource()==bCancel)
		{
			accepted = false;
			dispose();
		}
	}
	
}
