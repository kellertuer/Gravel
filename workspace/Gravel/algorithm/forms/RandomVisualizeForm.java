package algorithm.forms;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

import model.MGraph;

import dialogs.IntegerTextField;


public class RandomVisualizeForm extends AlgorithmParameterForm 
{
	private static final long serialVersionUID = 1L;
	private HashMap<String,Object> Parameters;
	private JButton bOk, bCancel;
	private IntegerTextField iMaxX,iMaxY;
	private JCheckBox bRandomizeEdges, bRandomizeNodes;
	private MGraph mGCopy;
	public RandomVisualizeForm(MGraph guigraph)
	{
		super(guigraph);
		Parameters = new HashMap<String, Object>();
		mGCopy = guigraph;
	}
	@SuppressWarnings("unchecked")	
	public HashMap showDialog() {
		this.setTitle("Ein erster Testalgorithmus - Parameterangaben");
		Container content = getContentPane();
		content.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(7,7,7,7);
		c.anchor = GridBagConstraints.WEST;
		c.gridy = 0;
		c.gridx = 0;
		c.gridwidth = 2;
		content.add(new JLabel("<html>Verwendung des Graphen aus der Oberfl"+main.CONST.html_ae+"che\n"),c);
		
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
		content.add(new JLabel("Maximaler X-Wert"),c);
		iMaxX = new IntegerTextField();
		iMaxX.setPreferredSize(new Dimension(200, 20));
		c.gridx = 1;
		content.add(iMaxX,c);

		c.gridy++;
		c.gridx = 0;
		content.add(new JLabel("Maximaler Y-Wert"),c);
		iMaxY = new IntegerTextField();
		iMaxY.setPreferredSize(new Dimension(200, 20));
		c.gridx = 1;
		content.add(iMaxY,c);
		
		
		c.gridy++;
		c.gridx = 0;
		c.gridwidth=2;
		bRandomizeEdges = new JCheckBox("<html>Kantenbreite zufällig wählen</html>");
		content.add(bRandomizeEdges,c);
		c.gridy++;
		bRandomizeNodes = new JCheckBox("<html>Knotengröße zufällig wählen</html>");
		content.add(bRandomizeNodes,c);
		c.gridy++;
		content.add(new JLabel("<html>Zufällig bezeichnet einen Wert &plusmn;5 um den Standard-Wert,<br>jedoch nie kleiner 1.</html>"),c);
		c.gridy++;
		c.gridx = 0;
		c.gridwidth=1;
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
			Parameters.put("MGraph",mGCopy);
			Parameters.put("MaxX", new Integer(iMaxX.getValue()));
			Parameters.put("MaxY", new Integer(iMaxY.getValue()));
			Parameters.put("RandomizeEdges", new Boolean(bRandomizeEdges.isSelected()));
			Parameters.put("RandomizeNodes", new Boolean(bRandomizeNodes.isSelected()));			
			dispose();
		}
		else if (e.getSource()==bCancel)
		{
			Parameters = null;
			dispose();
		}
	}

}
