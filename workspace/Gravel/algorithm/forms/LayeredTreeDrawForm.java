package algorithm.forms;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JLabel;

import view.pieces.GridComponent;

import model.MGraph;


public class LayeredTreeDrawForm extends AlgorithmParameterForm 
{
	private static final long serialVersionUID = 1L;
	private HashMap<String,Object> Parameters;
	private JButton bOk, bCancel;
	private MGraph mGCopy;
	GridComponent grid;
	public LayeredTreeDrawForm(MGraph guigraph)
	{
		super(guigraph);
		Parameters = new HashMap<String, Object>();
		mGCopy = guigraph;
	}
	
	public HashMap showDialog() {
		this.setTitle("Binärbaum-Divide-and.Conquer - Parameter");
		Container content = getContentPane();
		content.removeAll();
		content.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(7,7,7,7);
		c.anchor = GridBagConstraints.WEST;
		c.gridy = 0;
		c.gridx = 0;
		c.gridwidth = 2;
		content.add(new JLabel("<html>Verwendung des Graphen aus der Oberfläche</html>"),c);
		grid = new GridComponent();
		grid.setEnabled(true);
		grid.setEnableVisble(true);
		c.gridy++;
		content.add(grid.getContent(),c);
		grid.setEnabled(true);
		grid.setEnableVisble(false);
		c.gridwidth=1;
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
			Parameters.put("MGraph",mGCopy);
			Parameters.put("GridX", new Integer(grid.getGridX()));
			Parameters.put("GridY", new Integer(grid.getGridY()));
			dispose();
		}
		else if (e.getSource()==bCancel)
		{
			Parameters = null;
			dispose();
		}
	}

}
