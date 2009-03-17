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
 * Toolbar, that belongs to the mainwindows
 * Up to now it only contains a Zoom-Element
 * 
 * @author Ronny Bergmann
 * @since 0.2
 */
public class MainToolbar extends JToolBar implements ActionListener 
{	
	private static final long serialVersionUID = 1L;
	private VCommonGraphic vG;
	private ZoomComponent internalZoomC;
	/**
	 * Init Toolbar, that is  added as an Control-Element to a VGraphic
	 * @param g VGraphic it controls
	 */
	public MainToolbar (VCommonGraphic g)
	{
		super();
		vG = g;
		buildToolBar();
	}
	/**
	 * Build Toolbar
	 */
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
        internalZoomC = new ZoomComponent();
		vG.addPiece("Zoom", internalZoomC);
	    this.add(internalZoomC.getContent(),c);   
	}
	public void changeVGraph(VCommonGraphic newvg)
	{
		vG.removePiece("Zoom");
		vG = newvg;
		vG.addPiece("Zoom",internalZoomC);
		validate();
	}
	public void validate()
	{
		internalZoomC.setZoom(internalZoomC.getZoom());
	}
	public void actionPerformed(ActionEvent e) 
	{	
	}
}
