package dialogs.components;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import view.VHyperGraphic;

import model.VHyperGraph;

/**
 * This Class represents all GUI-Elements for manipulating the 
 * Shape of a single hyperedge,
 * including its Creation, modification and check for validity
 * 
 * Based on a single Hyperedgeindex and the corresponding VHyperGraph the
 * GUI is initialized
 * @author ronny
 *
 */
public class CHyperEdgeShapeParameters {

	private VHyperGraphic Editfield;
	private Container cont;
	/**
	 * Create the Dialog for an hyperedge with index i
	 * and the corresponding VHyperGraph
	 * If the Hypergraph does not contain an edge with the specified index,
	 * nothing happens and all functions might return null
	 * 
	 * @param index index of the hyperedge whose shape should be modified
	 * @param vhg corresponding hypergraph
	 */
	public CHyperEdgeShapeParameters(int index, VHyperGraph vhg)
	{
		if (vhg.modifyHyperEdges.get(index)==null)
			return;
		
		cont = new Container();
		cont.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(0,7,0,7);
		c.anchor = GridBagConstraints.WEST;
		c.gridy = 0;
		c.gridx = 0;
		c.gridwidth=2;
		c.gridheight=2;
		Dimension d = new Dimension(300,300);
		Editfield = new VHyperGraphic(d,vhg);
		Editfield.setMouseHandling(VHyperGraphic.NO_MOUSEHANDLING);
        //Das Ganze als Scrollpane
        JScrollPane scrollPane = new JScrollPane(Editfield);
        scrollPane.setViewportView(Editfield);
        Editfield.setViewPort(scrollPane.getViewport());
        scrollPane.setMinimumSize(d);
        scrollPane.setPreferredSize(d);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        Editfield.setSize(d);
        Editfield.validate();
		cont.add(scrollPane,c);
		c.gridy = 0;
		c.gridx = 2;
		c.gridwidth=1;
		c.gridheight=1;
		cont.add(new JLabel("Kram"),c);
		cont.validate();
	}
	/**
	 * get the GUI-Content
	 * @return
	 */
	public Container getContent()
	{
		return cont;
	}
}
