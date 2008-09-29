package algorithm.forms;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

import model.VGraph;

public class MagnetismAndSpringsForm extends AlgorithmParameterForm 
{
	private static final long serialVersionUID = 1L;
	private HashMap<String,Object> Parameters;
	private JButton bOk, bCancel;
	private JCheckBox bUseEdgeValues;
	private VGraph vGCopy;
	public MagnetismAndSpringsForm(VGraph guigraph)
	{
		super(guigraph);
		Parameters = new HashMap<String, Object>();
		vGCopy = new VGraph(true,false,false);
		vGCopy.replace(guigraph);
	}
	public HashMap showDialog() {
		this.setTitle("Federn und Magnetfelder - Parameterangaben");
		Container content = getContentPane();
		content.removeAll();
		content.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(7,7,7,7);
		c.anchor = GridBagConstraints.WEST;
		c.gridy = 0;
		c.gridx = 0;
		c.gridwidth = 2;
		content.add(new JLabel("<html>Verwendung des Graphen aus der Oberfl"+main.CONST.html_ae+"che"),c);
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 2;
		bUseEdgeValues = new JCheckBox("Kantengewichte verwenden");
		content.add(bUseEdgeValues,c);
		c.gridwidth = 1;			
		c.gridy++;
		c.gridx = 0;
		bOk = new JButton("Ok");
		bOk.addActionListener(this);
		content.add(bOk,c);
		bCancel = new JButton("Abbrechen");
		bCancel.addActionListener(this);
		c.gridx = 1;
		content.add(bCancel,c);
		setResizable(false);
		this.getRootPane().setDefaultButton(bOk);
		this.setModal(true);
		pack();
		super.alignCenter();
		this.setVisible(true);
		return Parameters;
	}

	public void actionPerformed(ActionEvent e) 
	{
		if (e.getSource()==bOk)
		{
			Parameters.put("VGraph",vGCopy);
			Parameters.put("EdgeSizeFactor", new Double(100.0d));
			Parameters.put("EdgeValueUsed", new Boolean(bUseEdgeValues.isSelected()));
			dispose();
		}
		else if (e.getSource()==bCancel)
		{
			Parameters = null;
			dispose();
		}
	}

}
