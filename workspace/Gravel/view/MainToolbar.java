package view;


import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JToolBar;

import view.pieces.ZoomComponent;

//import model.GeneralPreferences;
/**
 * Die Toolbar zum Haupfenster inklusive eigenem Action Handling
 * 
 * @author Ronny Bergmann
 */
public class MainToolbar extends JToolBar implements ActionListener 
{	
	private static final long serialVersionUID = 1L;
	private VGraphic vG;

	public MainToolbar (VGraphic g)
	{
		super();
		vG = g;
		buildToolBar();
	}
	private  void buildToolBar()
	{
        setOpaque(true);
        this.setFloatable(false);
		this.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(0,0,0,0);
		c.anchor = GridBagConstraints.EAST;
		c.gridy = 0;
		c.gridx = 3;		
        ZoomComponent zoom = new ZoomComponent();
		vG.addPiece("Zoom", zoom);
	    this.add(zoom.getContent(),c);   
	}
	public void actionPerformed(ActionEvent e) 
	{	
	}
}
