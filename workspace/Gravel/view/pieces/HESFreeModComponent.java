package view.pieces;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JRadioButton;

import model.NURBSShape;
import model.NURBSShapeFragment;

import view.VCommonGraphic;
import view.VHyperShapeGraphic;

/**
 * Small class containing all Buttons and Actionhandling for the second Modus of HyperEdgeShape-Dialog
 * @author ronny
 *
 */
public class HESFreeModComponent implements ActionListener {

	
	private JButton bIncKnots, bDecKnots;
	private ButtonGroup bModificationModus;
	private JRadioButton rModGlobal, rModLocal;
	private JButton bRotation,bTranslation, bScaling, bScalingDir;
	private JButton bLocalSetStart, bLocalSetEnd, bLocalInvert;
	private Container FreeModFields;

	private VHyperShapeGraphic HShapeGraphicRef;
	private int HEdgeRefIndex;
	
	public HESFreeModComponent(int index, VHyperShapeGraphic vhg)
	{
		if (vhg.getGraph().modifyHyperEdges.get(index)==null)
			return;
		HShapeGraphicRef = vhg;
		HEdgeRefIndex = index;
		buildFreeModPanel();
	}

	public Container getContent()
	{
		return FreeModFields;
	}
	
	public void setVisible(boolean visible)
	{
 		FreeModFields.setVisible(visible);
 		if (visible) //init always with CP-Movement TODO Button for that
 		{
       		HShapeGraphicRef.setMouseHandling(VCommonGraphic.CURVEPOINT_MOVEMENT_MOUSEHANDLING);
       		//Deactivate all Buttons
       		setButtonsDeselectedAndEnable(true);
 		}
	}
	
	private void setButtonsDeselectedAndEnable(boolean activity)
	{
		bRotation.setSelected(false);
		bTranslation.setSelected(false);
		bScaling.setSelected(false);
		bScalingDir.setSelected(false);
		bIncKnots.setSelected(false);
		bDecKnots.setSelected(false);
		bIncKnots.setEnabled(activity);
		bDecKnots.setEnabled(activity);
		bRotation.setEnabled(activity);
		bTranslation.setEnabled(activity);
		bScaling.setEnabled(activity);
		bScalingDir.setEnabled(activity);
	}
	
	private void setLocalVisibility(boolean visible)
	{
		bLocalSetStart.setVisible(visible);
		bLocalSetEnd.setVisible(visible);
		bLocalInvert.setVisible(visible);
	}
	private void deselectLocalButtons()
	{
		bLocalSetStart.setSelected(false);
		bLocalSetEnd.setSelected(false);
		bLocalInvert.setSelected(false);		
	}
	private void buildFreeModPanel()
	{
		String IconDir = System.getProperty("user.dir")+"/data/img/icon/";
		FreeModFields = new Container();
		FreeModFields.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(5,0,5,0);
		c.anchor = GridBagConstraints.CENTER;
		c.gridy = 0;
		c.gridx = 0;
		c.gridwidth=1;
		c.gridheight=1;

		c.gridwidth=2;
		bModificationModus = new ButtonGroup();
		rModGlobal = new JRadioButton("global");
		rModGlobal.addActionListener(this);
		bModificationModus.add(rModGlobal);
		FreeModFields.add(rModGlobal,c);
		c.gridx+=2;
		rModLocal = new JRadioButton("lokal");
		rModLocal.addActionListener(this);
		bModificationModus.add(rModLocal);
		FreeModFields.add(rModLocal,c);

		c.gridy++;
		c.gridx=0;
		c.gridwidth=1;
		bIncKnots = new JButton(new ImageIcon(IconDir+"plus16.png"));
		bIncKnots.setSize(new Dimension(17,17));
		bIncKnots.addActionListener(this);
		FreeModFields.add(bIncKnots,c);

		c.gridx++;
		bDecKnots = new JButton(new ImageIcon(IconDir+"minus16.png"));
		bDecKnots.setSize(new Dimension(17,17));
//		bDecKnots.setEnabled(false);
		FreeModFields.add(bDecKnots,c);
		
		c.gridy++;
		c.gridx=0;
		c.gridwidth=1;
		bRotation = new JButton(new ImageIcon(IconDir+"rotate32.png"));	
		bRotation.setSize(new Dimension(32,32));
		bRotation.addActionListener(this);
		FreeModFields.add(bRotation,c);

		c.gridx++;
		c.gridwidth=1;
		
		bTranslation = new JButton(new ImageIcon(IconDir+"translate32.png"));	
		bTranslation.setSize(new Dimension(32,32));
		bTranslation.addActionListener(this);
		FreeModFields.add(bTranslation,c);

		c.gridx++;
		c.gridwidth=1;
		bScaling = new JButton(new ImageIcon(IconDir+"scale32.png"));	
		bScaling.setSize(new Dimension(32,32));
		bScaling.addActionListener(this);
		FreeModFields.add(bScaling,c);

		c.gridy++;
		c.gridx=0;
		c.gridwidth=1;
		bScalingDir = new JButton(new ImageIcon(IconDir+"scaledir32.png"));	
		bScalingDir.setSize(new Dimension(17,17));
		bScalingDir.addActionListener(this);
		FreeModFields.add(bScalingDir,c);

		c.gridy++;
		c.gridx=0;
		c.gridwidth=1;
		
		bLocalSetStart = new JButton("S");
		bLocalSetStart.addActionListener(this);
		FreeModFields.add(bLocalSetStart,c);

		c.gridx++;
		bLocalInvert = new JButton("I");
		bLocalInvert.addActionListener(this);
		FreeModFields.add(bLocalInvert,c);

		c.gridx++;
		bLocalSetEnd = new JButton("E");
		bLocalSetEnd.addActionListener(this);
		FreeModFields.add(bLocalSetEnd,c);
		
		setLocalVisibility(rModLocal.isSelected());
	}

	private void handleLocalAction(ActionEvent e)
	{
		if (e.getSource()==bLocalSetStart)
		{
			boolean wasSelected = bLocalSetStart.isSelected();
			deselectLocalButtons();
			if (wasSelected) //back to default / no DETAIL
				HShapeGraphicRef.setMouseHandling(VCommonGraphic.SUBCURVE_MOUSEHANDLING|VCommonGraphic.NO_DETAIL);
			else
			{
				bLocalSetStart.setSelected(true);
				HShapeGraphicRef.setMouseHandling(VCommonGraphic.SUBCURVE_MOUSEHANDLING|VCommonGraphic.SET_START);
			}
		}
		else if (e.getSource()==bLocalSetEnd)
		{
			boolean wasSelected = bLocalSetEnd.isSelected();
			deselectLocalButtons();
			if (wasSelected) //Back to default
				HShapeGraphicRef.setMouseHandling(VCommonGraphic.SUBCURVE_MOUSEHANDLING|VCommonGraphic.NO_DETAIL);
			else
			{
				bLocalSetEnd.setSelected(true);
				HShapeGraphicRef.setMouseHandling(VCommonGraphic.SUBCURVE_MOUSEHANDLING|VCommonGraphic.SET_END);
			}
		}
		else if (e.getSource()==bLocalInvert)
			HShapeGraphicRef.setMouseHandling(VCommonGraphic.SUBCURVE_MOUSEHANDLING|VCommonGraphic.TOGGLE);
	}
	public void actionPerformed(ActionEvent e) {
        if ((e.getSource()==rModGlobal)||(e.getSource()==rModLocal))
        {
        	if (rModGlobal.isSelected())
        	{
            	setButtonsDeselectedAndEnable(true);
        		HShapeGraphicRef.setMouseHandling(VCommonGraphic.CURVEPOINT_MOVEMENT_MOUSEHANDLING);	        		
        	}
        	else
        	{
        		boolean selectionExists;
        		NURBSShape shape = HShapeGraphicRef.getGraph().modifyHyperEdges.get(HEdgeRefIndex).getShape();
        		if ((shape.getDecorationTypes()&NURBSShape.FRAGMENT)==NURBSShape.FRAGMENT)
        		{
        			selectionExists = !((NURBSShapeFragment) shape).getSubCurve().isEmpty();
        		}
        		else
        			selectionExists = false;
        		setButtonsDeselectedAndEnable(selectionExists);
        		if (!selectionExists)
        			HShapeGraphicRef.setMouseHandling(VCommonGraphic.SUBCURVE_MOUSEHANDLING);
        	}
    		setLocalVisibility(rModLocal.isSelected());
        }
		if (e.getSource()==bIncKnots)
        {
			System.err.println("//TODO Inc/Dec Knots");
        }
		if (e.getSource()==bDecKnots)
        {
			System.err.println("//TODO Inc/Dec Knots");			
        }
        else if (e.getSource()==bRotation)
        {
        	if (bRotation.isSelected())
        	{
        		bRotation.setSelected(false);
        		HShapeGraphicRef.setMouseHandling(VCommonGraphic.CURVEPOINT_MOVEMENT_MOUSEHANDLING);	        		
        	}
        	else
        	{
        		setButtonsDeselectedAndEnable(true);
        		bRotation.setSelected(true);
        		HShapeGraphicRef.setMouseHandling(VCommonGraphic.SHAPE_MOUSEHANDLING|VCommonGraphic.ROTATE);	        		
        	}
        }
        else if (e.getSource()==bTranslation)
        {
        	if (bTranslation.isSelected())
        	{
        		bTranslation.setSelected(false);
        		HShapeGraphicRef.setMouseHandling(VCommonGraphic.CURVEPOINT_MOVEMENT_MOUSEHANDLING);	        		
        	}
        	else
        	{
        		setButtonsDeselectedAndEnable(true);
        		bTranslation.setSelected(true);
        		HShapeGraphicRef.setMouseHandling(VCommonGraphic.SHAPE_MOUSEHANDLING|VCommonGraphic.TRANSLATE);	        		
        	}
        }
        else if (e.getSource()==bScaling)
        {
        	if (bScaling.isSelected())
        	{
        		bScaling.setSelected(false);
        		HShapeGraphicRef.setMouseHandling(VCommonGraphic.CURVEPOINT_MOVEMENT_MOUSEHANDLING);	        		
        	}
        	else
        	{
        		setButtonsDeselectedAndEnable(true);
        		bScaling.setSelected(true);
        		HShapeGraphicRef.setMouseHandling(VCommonGraphic.SHAPE_MOUSEHANDLING|VCommonGraphic.SCALE);	        		
        	}
        }
        else if (e.getSource()==bScalingDir)
        {
        	if (bScalingDir.isSelected())
        	{
        		bScalingDir.setSelected(false);
        		HShapeGraphicRef.setMouseHandling(VCommonGraphic.CURVEPOINT_MOVEMENT_MOUSEHANDLING);	        		
        	}
        	else
        	{
        		setButtonsDeselectedAndEnable(true);
        		bScalingDir.setSelected(true);
        		HShapeGraphicRef.setMouseHandling(VCommonGraphic.SHAPE_MOUSEHANDLING|VCommonGraphic.SCALE_DIR);	        		
        	}
        }
        else
        	handleLocalAction(e);
	}
}
