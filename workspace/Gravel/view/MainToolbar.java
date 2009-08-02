package view;


import java.awt.BorderLayout;

import javax.swing.JToolBar;

import view.pieces.*;

//import model.GeneralPreferences;
/**
 * Toolbar, that belongs to the mainwindows
 * Up to now it only contains a Zoom-Element
 * 
 * @author Ronny Bergmann
 * @since 0.2
 */
public class MainToolbar extends JToolBar
{	
	private static final long serialVersionUID = 1L;
	private VCommonGraphic vG;
	private ZoomComponent internalZoomC;
	private ItemInformationComponent internalInfoC;
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
        internalZoomC = new ZoomComponent();
        if (vG.getType()==VCommonGraphic.VGRAPHIC)
        	internalInfoC = new ItemInformationComponent(((VGraphic)vG).getGraph());
        else if (vG.getType()==VCommonGraphic.VHYPERGRAPHIC)
        	internalInfoC = new ItemInformationComponent(((VHyperGraphic)vG).getGraph());
        vG.addPiece("Zoom", internalZoomC);
		this.setLayout(new BorderLayout());
	    this.add(internalZoomC.getContent(),BorderLayout.CENTER);
	    this.add(internalInfoC.getContent(),BorderLayout.EAST);
	}
	public void changeVGraph(VCommonGraphic newvg)
	{
		vG.removePiece("Zoom");
		vG = newvg;
		vG.addPiece("Zoom",internalZoomC);
        if (vG.getType()==VCommonGraphic.VGRAPHIC)
        	internalInfoC = new ItemInformationComponent(((VGraphic)vG).getGraph());
        else if (vG.getType()==VCommonGraphic.VHYPERGRAPHIC)
        	internalInfoC = new ItemInformationComponent(((VHyperGraphic)vG).getGraph());
		validate();
	}
	public void validate()
	{
		internalZoomC.setZoom(internalZoomC.getZoom());
	}
}
