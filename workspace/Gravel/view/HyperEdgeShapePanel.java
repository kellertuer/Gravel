package view;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.KeyStroke;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import dialogs.IntegerTextField;


import main.CONST;
import model.*;
import model.Messages.*;

/**
 * This Class represents all GUI-Elements, e.g. Parameters / Buttons,
 * for manipulating the Shape of a single hyperedge (the drawing is in the
 * initially passed VHyperShapeGraphic
 * 
 * this includes its CreationParameters, e.g. a Circle 
 * TODO Modification Parameters and Modi
 * 
 * Therefore the interaction is split into 2 seperate steps:
 * 1) Create a Basic Shape, e.g. a circle, polygon or Interpolation Points
 * 2) Modify the Shape by
 *  -- dragging specific Shape-Areas,
 *  -- In- and Decreasing both knots and degree
 *  	(perhaps select a pasrt of the shape and in/decrease knots there?)
 *  -- Rotation, Scaling Shifting of the whole Shape (or selections? Is that possible?)
 *  
 *  TODO Use a second mode with possibility to be set to expert mode, where you can drag ControlPoints
 *		(and perhaps change their weight by double click?)  
 * 
 * Based on a single Hyperedgeindex and the corresponding VHyperGraph the
 * GUI is initialized
 * @author Ronny Bergmann
 * @since 0.4
 *
 */
public class HyperEdgeShapePanel implements CaretListener, ActionListener, Observer {

	private Container cont;
	private JComboBox cBasicShape;
	private JLabel BasicShape;
	//Circle Fields
	private JLabel CircleOriginX, CircleOriginY, CircleRadius;
	private IntegerTextField iCOrigX, iCOrigY, iCRad;
	//Interpolation Fields
	private JLabel Degree, IPInfo;
	private IntegerTextField iDegree;
	private ButtonGroup bAddIP;
	private JRadioButton rAddEnd, rAddBetween;

	//FreeModFields
	private JLabel knots, CheckResult;
	private JButton bIncKnots, bDecKnots, bCheckShape;
	
	private JButton bModeChange, bOk, bCancel;
	private JButton bRotation,bTranslation, bScaling, bScalingDir;
	private int HEdgeRefIndex; //Reference to the HyperEdge in the Graph that is edited here
	private VHyperGraph HGraphRef; //Reference to the edited Graph, should be a copy of the Graph from the main GUI because the user might cancel this dialog 
	private VHyperShapeGraphic HShapeGraphicRef;
	
	private Container CircleFields, InterpolationFields, DegreeFields, FreeModFields;
	/**
	 * Create the Dialog for an hyperedge with index i
	 * and the corresponding VHyperGraph
	 * If the Hypergraph does not contain an edge with the specified index,
	 * nothing happens and all functions might return null
	 * 
	 * @param index index of the hyperedge whose shape should be modified
	 * @param vhg corresponding hypergraph
	 */
	public HyperEdgeShapePanel(int index, VHyperShapeGraphic vhg)
	{
		if (vhg.getGraph().modifyHyperEdges.get(index)==null)
			return;
		HEdgeRefIndex = index;
		HGraphRef = vhg.getGraph();
		HGraphRef.addObserver(this);
		HShapeGraphicRef = vhg;
		cont = new Container();
		cont.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(0,7,0,7);
		c.anchor = GridBagConstraints.CENTER;
		c.gridy = 0;
		c.gridx = 0;
		c.gridwidth=1;
		c.gridheight=1;
		//INput fields besides the Display
		String[] BasicShapes = {"Kreis", "Interpolation", "konvexe Hülle"};
		cBasicShape = new JComboBox(BasicShapes);
		cBasicShape.setSelectedIndex(0);
		cBasicShape.setPreferredSize(new Dimension(100, 30));
		cBasicShape.addActionListener(this);
		//If we don't have a Shape...set it to visible because it's toggled at the end of this method to init visibility
		cBasicShape.setVisible(!HGraphRef.modifyHyperEdges.get(index).getShape().isEmpty());
		BasicShape = new JLabel("<html><p>Grundform</p></html>");
		cont.add(BasicShape,c);
		c.gridx++;
		cont.add(cBasicShape,c);
		
		c.gridy++;
		c.gridx=0;
		c.gridwidth=2;
		c.gridheight=2;
		c.insets = new Insets(30,7,0,7);
		
		//
		// Lay all Small Containers with Options at this position nonvisible
		//

		buildCirclePanel();
		cont.add(CircleFields,c);
		CircleFields.setVisible(false);

		c.gridheight=1;
		buildDegreePanel();
		cont.add(DegreeFields,c);
		DegreeFields.setVisible(false);
		c.gridy++;
		buildInterpolationPanel();
		cont.add(InterpolationFields,c);
		InterpolationFields.setVisible(false);

		//
		//Container for the fields of the second mode
		//
		c.gridy--;
		c.gridx=0;
		c.gridwidth=2;
		buildFreeModPanel();
		cont.add(FreeModFields,c);
		FreeModFields.setVisible(false);
		
		c.gridy+=2;
		c.gridx=0;
		c.gridwidth=2;
		
		c.anchor = GridBagConstraints.SOUTH;
		bModeChange = new JButton("Modifikation"); //Name suchen!
		bModeChange.setEnabled(!HGraphRef.modifyHyperEdges.get(HEdgeRefIndex).getShape().isEmpty());
		bModeChange.addActionListener(this);
		cont.add(bModeChange,c);

		c.gridy++;
		c.gridx=0;

		c.gridy++;
		c.gridx=0;
		c.insets.top+=5;
		bCheckShape = new JButton("<html>Umriss pr&uuml;fen</html>");
		bCheckShape.addActionListener(this);
		cont.add(bCheckShape,c);
		c.gridy++;
		c.insets.top=0;
		c.insets.bottom=7;
		CheckResult = new JLabel("<html>&nbsp;</html>");
		cont.add(CheckResult,c);
		c.gridy++;		
		c.insets.top=5;
		c.insets.bottom=0;		
		c.gridwidth=1;
		c.anchor = GridBagConstraints.SOUTHWEST;
		bCancel = new JButton("Abbrechen");
		bCancel.addActionListener(this);
		cont.add(bCancel,c);
		//Handling Escape as Cancel
		//Add ESC-Handling
		InputMap iMap = Gui.getInstance().getParentWindow().getRootPane().getInputMap(	 JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape");

		ActionMap aMap = Gui.getInstance().getParentWindow().getRootPane().getActionMap();
		aMap.put("escape", new AbstractAction()
			{
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e)
				{
					bCancel.doClick();
				}
		 	});
		c.gridx++;
		c.anchor = GridBagConstraints.SOUTHEAST;
		bOk = new JButton("Anwenden");
		bOk.addActionListener(this);
		bOk.setEnabled(!HGraphRef.modifyHyperEdges.get(HEdgeRefIndex).getShape().isEmpty());
		cont.add(bOk,c);
		Gui.getInstance().getParentWindow().getRootPane().setDefaultButton(bOk);
		
		cont.validate();
		actionPerformed(new ActionEvent(bModeChange,0,"Refresh"));
	}
	
	public VHyperEdge getActualEdge()
	{
		return HGraphRef.modifyHyperEdges.get(HEdgeRefIndex);
	}
	
	private void buildCirclePanel()
	{
		CircleFields = new Container();
		CircleFields.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(5,7,5,7);
		c.anchor = GridBagConstraints.WEST;
		c.gridy = 0;
		c.gridx = 0;
		c.gridwidth=1;
		c.gridheight=1;
		iCOrigX = new IntegerTextField();
		iCOrigX.addCaretListener(this);;
		iCOrigX.setPreferredSize(new Dimension(100, 20));
		CircleOriginX = new JLabel("<html><p>Mittelpunkt X</p></html>");
		CircleFields.add(CircleOriginX,c);
		c.gridx++;
		CircleFields.add(iCOrigX,c);

		c.gridy++;
		c.gridx=0;
		iCOrigY = new IntegerTextField();
		iCOrigY.addCaretListener(this);;
		iCOrigY.setPreferredSize(new Dimension(100, 20));
		CircleOriginY = new JLabel("<html><p>Mittelpunkt Y</p></html>");
		CircleFields.add(CircleOriginY,c);
		c.gridx++;
		CircleFields.add(iCOrigY,c);

		c.gridy++;
		c.gridx=0;
		iCRad = new IntegerTextField();
		iCRad.addCaretListener(this);;
		iCRad.setPreferredSize(new Dimension(80, 20));
		CircleRadius = new JLabel("<html><p>Radius</p></html>");
		CircleFields.add(CircleRadius,c);
		c.gridx++;
		CircleFields.add(iCRad,c);
	}

	private void buildDegreePanel()
	{
		DegreeFields = new Container();
		DegreeFields.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(5,7,5,7);
		c.anchor = GridBagConstraints.WEST;
		c.gridy = 0;
		c.gridx = 0;
		c.gridwidth=1;
		c.gridheight=1;
		iDegree = new IntegerTextField();
		iDegree.addCaretListener(this);;
		iDegree.setPreferredSize(new Dimension(80, 20));
		iDegree.setValue(3);
		Degree = new JLabel("<html><p>Polynomgrad</p></html>");
		DegreeFields.add(Degree,c);
		c.gridx++;
		DegreeFields.add(iDegree,c);
		c.gridy++;
		c.gridx=0;
		c.gridwidth=2;
		IPInfo = new JLabel("<html><p>&nbsp;</p></html>");
		DegreeFields.add(IPInfo,c);

	}
	private void buildInterpolationPanel()
	{
		InterpolationFields = new Container();
		InterpolationFields.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(5,7,5,7);
		c.anchor = GridBagConstraints.WEST;
		c.gridy = 0;
		c.gridx = 0;
		c.gridheight=1;
		c.gridwidth=2;		
		InterpolationFields.add(new JLabel("<html><p>Neuen Interpolationspunkt einfügen:</p></html>"),c);		
		c.gridwidth=1;
		c.gridy++;
		bAddIP = new ButtonGroup();
		rAddEnd = new JRadioButton("Am Ende");
		rAddEnd.addActionListener(this);
		rAddBetween = new JRadioButton("dazwischen");
		rAddBetween.addActionListener(this);
		rAddEnd.setSelected(true);
		bAddIP.add(rAddEnd); bAddIP.add(rAddBetween);
		InterpolationFields.add(rAddEnd,c);
		c.gridx++;
		InterpolationFields.add(rAddBetween,c);
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

		c.gridx++;
		c.gridwidth=1;
		bIncKnots = new JButton(new ImageIcon(IconDir+"plus16.png"));
		bIncKnots.setSize(new Dimension(17,17));
		bIncKnots.addActionListener(this);
		FreeModFields.add(bIncKnots,c);

		c.gridx++;
		bDecKnots = new JButton(new ImageIcon(IconDir+"minus16.png"));
		bDecKnots.setSize(new Dimension(17,17));
		bDecKnots.setEnabled(false);
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

		c.gridx++;
		c.gridwidth=1;
		bScalingDir = new JButton(new ImageIcon(IconDir+"scaledir32.png"));	
		bScalingDir.setSize(new Dimension(17,17));
		bScalingDir.addActionListener(this);
		FreeModFields.add(bScalingDir,c);

	}
	/**
	 * get the GUI-Content
	 * @return
	 */
	public Container getContent()
	{
		return cont;
	}

	private void CalculateConvexHullShape()
	{
		Iterator<VNode> nodeiter = HGraphRef.modifyNodes.getIterator();
		MHyperEdge me = HGraphRef.getMathGraph().modifyHyperEdges.get(HEdgeRefIndex);
		Vector<Point2D> nodepos = new Vector<Point2D>();
		Vector<Integer> nodesizes = new Vector<Integer>();
		while (nodeiter.hasNext())
		{
			VNode n = nodeiter.next();
			if (me.containsNode(n.getIndex()))
			{
				nodepos.add(new Point2D.Double(n.getPosition().x,n.getPosition().y));
				nodesizes.add(n.getSize());
			}
		}
		NURBSCreationMessage nm = new NURBSCreationMessage(
				iDegree.getValue(), 
				HGraphRef.modifyHyperEdges.get(HEdgeRefIndex).getMinimumMargin(),
				nodepos,
				nodesizes);
		NURBSShape s = NURBSShapeFactory.CreateShape(nm);
		if (s.isEmpty())
		{
			IPInfo.setText("<html><p>Polynomgrad "+iDegree.getValue()+" ist zu hoch.</p></html>");
			return;
		}
		else
			IPInfo.setText("<html><p>&nbsp;</p></html>");
		HGraphRef.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,HEdgeRefIndex,GraphConstraints.BLOCK_START|GraphConstraints.UPDATE|GraphConstraints.HYPEREDGESHAPE|GraphConstraints.CREATION,GraphConstraints.HYPEREDGE));
		HShapeGraphicRef.setShapeParameters(nm);
		HGraphRef.modifyHyperEdges.get(HEdgeRefIndex).setShape(s);
		HGraphRef.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,GraphConstraints.BLOCK_END));			
	}
	
	public void caretUpdate(CaretEvent e)
	{
		if ((e.getSource()==iCOrigX)||(e.getSource()==iCOrigY)||(e.getSource()==iCRad))
		{
			
			if ((iCOrigX.getValue()!=-1)&&(iCOrigY.getValue()!=-1)&&(iCRad.getValue()!=-1))
			{ //All valid
				NURBSCreationMessage old = HShapeGraphicRef.getShapeParameters();
				if ((old==null)||(!old.isValid())||(old.getType()!=NURBSCreationMessage.CIRCLE))
					return; //invalid
				if  (  (iCOrigX.getValue()==Math.round((float)old.getPoints().firstElement().getX()))
					&& (iCOrigY.getValue()==Math.round((float)old.getPoints().firstElement().getY()))
					&& (iCRad.getValue()==old.getValues().firstElement().intValue())	)
					return; //No Value changed
				NURBSCreationMessage nm = new NURBSCreationMessage(
						2, //TODO: Enable Circles with Degree
						new Point2D.Double((new Integer(iCOrigX.getValue())).doubleValue(), (new Integer(iCOrigY.getValue())).doubleValue()),
						iCRad.getValue()
				);
				HGraphRef.modifyHyperEdges.get(HEdgeRefIndex).setShape(NURBSShapeFactory.CreateShape(nm));
				HShapeGraphicRef.setShapeParameters(nm);
				HGraphRef.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE, HEdgeRefIndex, GraphConstraints.UPDATE|GraphConstraints.HYPEREDGESHAPE|GraphConstraints.CREATION, GraphConstraints.HYPEREDGE)); //HyperEdgeShape Updated
			}
		}
		if ((e.getSource()==iDegree)&&(iDegree.getValue()>0))
		{
	       	String Shape = (String)cBasicShape.getSelectedItem();
       		NURBSCreationMessage nm = HShapeGraphicRef.getShapeParameters();
	       	if (Shape.equals("Interpolation"))
        	{    		
	       		nm.setDegree(iDegree.getValue());
	       		HShapeGraphicRef.setShapeParameters(nm);
				HGraphRef.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE, HEdgeRefIndex, GraphConstraints.UPDATE|GraphConstraints.HYPEREDGESHAPE|GraphConstraints.CREATION, GraphConstraints.HYPEREDGE)); //HyperEdgeShape Updated
        	}
        	else if (Shape.equals("konvexe Hülle"))
        	{
        		CalculateConvexHullShape();
	       		nm.setDegree(iDegree.getValue());
	       		HShapeGraphicRef.setShapeParameters(nm);
				HGraphRef.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE, HEdgeRefIndex, GraphConstraints.UPDATE|GraphConstraints.HYPEREDGESHAPE|GraphConstraints.CREATION, GraphConstraints.HYPEREDGE)); //HyperEdgeShape Updated
        	}
        	else //no Degree Stuff
        		return;
	       	updateInfo(nm);
		}
	}
	private void deselectButtons()
	{
		bRotation.setSelected(false);
		bTranslation.setSelected(false);
		bScaling.setSelected(false);
		bScalingDir.setSelected(false);
	}
	private void performSecondModus(ActionEvent e)
	{
        if (e.getSource()==bIncKnots)
        {
        	HGraphRef.modifyHyperEdges.get(HEdgeRefIndex).getShape().refineMiddleKnots();
        	HShapeGraphicRef.setMouseHandling(HShapeGraphicRef.getMouseHandling()); //ReInit Drag/Click
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
        		deselectButtons();
        		bRotation.setSelected(true);
        		HShapeGraphicRef.setMouseHandling(VCommonGraphic.SHAPE_ROTATE_MOUSEHANDLING);	        		
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
        		deselectButtons();
        		bTranslation.setSelected(true);
        		HShapeGraphicRef.setMouseHandling(VCommonGraphic.SHAPE_TRANSLATE_MOUSEHANDLING);	        		
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
        		deselectButtons();
        		bScaling.setSelected(true);
        		HShapeGraphicRef.setMouseHandling(VCommonGraphic.SHAPE_SCALE_MOUSEHANDLING);	        		
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
        		deselectButtons();
        		bScalingDir.setSelected(true);
        		HShapeGraphicRef.setMouseHandling(VCommonGraphic.SHAPE_SCALEDIR_MOUSEHANDLING);	        		
        	}
        }
	}
	
	private void setCreationFields(boolean visible)
	{
 		CircleFields.setVisible(visible);
		InterpolationFields.setVisible(visible);
    	DegreeFields.setVisible(visible);
		cBasicShape.setVisible(visible);
		BasicShape.setVisible(visible);
	}
	private void setFreeModificationFields(boolean visible)
	{
 		FreeModFields.setVisible(visible);
 		if (visible) //init always with CP-Movement
 		{
       		HShapeGraphicRef.setMouseHandling(VCommonGraphic.CURVEPOINT_MOVEMENT_MOUSEHANDLING);
       		//Deactivate all Buttons
       		deselectButtons();
 		}
	}
	public void actionPerformed(ActionEvent e) {
	        if ((e.getSource()==bCancel)||(e.getSource()==bOk))
	        {
	    		InputMap iMap = Gui.getInstance().getParentWindow().getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
	    		iMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));

	    		ActionMap aMap = Gui.getInstance().getParentWindow().getRootPane().getActionMap();
	    		aMap.remove("escape");
	    		Gui.getInstance().getParentWindow().getRootPane().setDefaultButton(null); //Remove again
	    		Gui.getInstance().rebuildmaingrid(e.getSource()==bOk);
	        }
	        else if (e.getSource()==cBasicShape)//ComboBox
	        {
	        	CircleFields.setVisible(false);
	        	InterpolationFields.setVisible(false);
	        	DegreeFields.setVisible(false);
	        	String Shape = (String)cBasicShape.getSelectedItem();
	        	if (Shape.equals("Kreis"))
	        	{
	        		HShapeGraphicRef.setMouseHandling(VCommonGraphic.CIRCLE_MOUSEHANDLING);
	        		CircleFields.setVisible(true);
	        	}
	        	else if (Shape.equals("Interpolation"))
	        	{
	        		HShapeGraphicRef.setMouseHandling(VCommonGraphic.INTERPOLATION_MOUSEHANDLING);
	        		InterpolationFields.setVisible(true);
		        	DegreeFields.setVisible(true);
	        	}
	        	else if (Shape.equals("konvexe Hülle"))
	        	{
	        		HShapeGraphicRef.setMouseHandling(VCommonGraphic.NO_MOUSEHANDLING);
	        		CalculateConvexHullShape();
	        		DegreeFields.setVisible(true);
	        	}
	        	else
	        	{
	        		HShapeGraphicRef.setMouseHandling(VCommonGraphic.NO_MOUSEHANDLING);
	        	}
	        }
	        if (e.getSource()==bModeChange) //Modus Change Button
	        {
	        	if (cBasicShape.isVisible()) //We are in mode 1
	        	{
	        		bModeChange.setText("neue Grundfrom");
	        		setFreeModificationFields(true);
	        		setCreationFields(false);
	        	}
	        	else
	        	{
	        		bModeChange.setText("Modifikation");
	        		setFreeModificationFields(false);
	        		cBasicShape.setVisible(true);
	        		BasicShape.setVisible(true);
	        		actionPerformed(new ActionEvent(cBasicShape,0,"Refresh"));
	        	}
	        }
	        if ((e.getSource()==rAddBetween)||(e.getSource()==rAddEnd))
	        {
	       		NURBSCreationMessage nm = HShapeGraphicRef.getShapeParameters();
				if (rAddBetween.isSelected())
					nm.setStatus(NURBSCreationMessage.ADD_BETWEEN);
				else
					nm.setStatus(NURBSCreationMessage.ADD_END);
				HShapeGraphicRef.setShapeParameters(nm);
	        }	
	        performSecondModus(e);
	        if (e.getSource()==bCheckShape)
	        {
	        	NURBSShapeValidator validator = new NURBSShapeValidator(HGraphRef, HEdgeRefIndex, null); //Check actual Shape of the Edge
	    		if (validator.isShapeValid())
	    		{
	    			CheckResult.setOpaque(true);
	    			CheckResult.setBackground(Color.GREEN.darker());
	    			CheckResult.setText("<html>Der Umriss ist korrekt.</html>");
	    		}
	    		else
	    		{
	    			CheckResult.setOpaque(true);
	    			CheckResult.setBackground(Color.RED.darker());
		    		String msg = "<html>Umriss nicht korrekt.<br>";		    		
		    		Vector<Integer> WrongNodes = validator.getInvalidNodeIndices();
		    		for (int j=0; j<WrongNodes.size(); j++)
		    				msg+="#"+WrongNodes.get(j);
		    		msg+="</html>";
		    		CheckResult.setText(msg);
	    		}
	        }
	        getContent().validate();
	        getContent().repaint();
	}

	//Update Fields to fit the values of a message
	private void updateDegreeFields(NURBSCreationMessage nm)
	{
		if ((nm==null)||(!nm.isValid()))
		{
			iDegree.removeCaretListener(this);
			iDegree.setValue(3); //TODO: Degree Std value in Panel Update
			iDegree.addCaretListener(this);
			return;
		}
		int deg = nm.getDegree();
		if (iDegree.getValue()==deg)
			return;
		iDegree.removeCaretListener(this);
		iDegree.setValue(deg);
		iDegree.addCaretListener(this);
	}
	private void updateCircleFields(NURBSCreationMessage nm)
	{
		if ((nm==null)||(!nm.isValid()))
		{
			//Reset to empty stuff
			iCOrigX.removeCaretListener(this);
			iCOrigX.setText("");
			iCOrigX.addCaretListener(this);
			iCOrigY.removeCaretListener(this);
			iCOrigY.setText("");
			iCOrigY.addCaretListener(this);
			iCRad.removeCaretListener(this);
			iCRad.setText("");
			iCRad.addCaretListener(this);					
			return;
		}
		if (nm.getType()!=NURBSCreationMessage.CIRCLE)
			return; //unsuitable->ignore
		Point2D p = nm.getPoints().firstElement();
		if (p==null)
			return;
		Point porig = new Point(Math.round((float)p.getX()),Math.round((float)p.getY()));

		int size = nm.getValues().firstElement();
		if (porig.x!=iCOrigX.getValue())
		{//Update without evoking this caretUpdate
			iCOrigX.removeCaretListener(this);
			iCOrigX.setValue(porig.x);
			iCOrigX.addCaretListener(this);
		}
		if (porig.y!=iCOrigY.getValue())
		{
			iCOrigY.removeCaretListener(this);
			iCOrigY.setValue(porig.y);
			iCOrigY.addCaretListener(this);
		}
		if (size!=iCRad.getValue())
		{
			iCRad.removeCaretListener(this);
			iCRad.setValue(size);
			iCRad.addCaretListener(this);					
		}
	}
	private void updateIPFields(NURBSCreationMessage nm)
	{
		if ((nm==null)||(!nm.isValid()))
		{ //Back to std.
			rAddEnd.removeActionListener(this);
			rAddEnd.setSelected(true);
			rAddEnd.addActionListener(this);
			return;
		}
		if (nm.getType()!=NURBSCreationMessage.INTERPOLATION)
			return; //unsuitable->ignore
		if ((nm.getStatus()==NURBSCreationMessage.ADD_END)&&(rAddBetween.isSelected()))
		{ //Update
			rAddEnd.removeActionListener(this);
			rAddEnd.setSelected(true);
			rAddEnd.addActionListener(this);
		}
		if ((nm.getStatus()==NURBSCreationMessage.ADD_BETWEEN)&&(rAddEnd.isSelected()))
		{ //Update
			rAddBetween.removeActionListener(this);
			rAddBetween.setSelected(true);
			rAddBetween.addActionListener(this);
		}
		updateInfo(nm);
	}
	private void updateInfo(NURBSCreationMessage nm)
	{
		if ((nm==null)||(!nm.isValid()))
			return;
		int deg= nm.getDegree();
		Vector<Point2D> p= nm.getPoints();
		if (p==null)
			return;
		if (p.size()<=deg)
			IPInfo.setText("<html><p>Grad "+deg+" ben"+CONST.html_oe+"tigt "+(2*deg+1-p.size())+" weitere Punkt(e).</p></html>");
		else
			IPInfo.setText("");
	}

	private void updatePanel(NURBSCreationMessage nm)
	{
		if (nm==null) //Init to second Modus
		{
			if (cBasicShape.isVisible())
				actionPerformed(new ActionEvent(bModeChange,0,"Change Modus"));
			deselectButtons();
			return;
		}
		if (!nm.isValid()) //update all to std
		{
			updateDegreeFields(nm); updateIPFields(nm); updateCircleFields(nm);
			return;
		}
		//Init the correct modus in first...
		if (!cBasicShape.isVisible()) //We're in second modus
			actionPerformed(new ActionEvent(bModeChange,0,"Change Modus"));			
		switch(nm.getType())
		{
			default:
			case NURBSCreationMessage.INTERPOLATION: //Init to that
				cBasicShape.setSelectedIndex(1);
				updateDegreeFields(nm);
				updateIPFields(nm);
				break;
			case NURBSCreationMessage.CIRCLE: //Init to that
				cBasicShape.setSelectedIndex(0);
				updateCircleFields(nm);
			break;
			case NURBSCreationMessage.CONVEX_HULL: //Init to that
				cBasicShape.setSelectedIndex(2);
				updateDegreeFields(nm);
			break;
		}
		getContent().repaint();
	}
	
	public void update(Observable o, Object arg) {
		if (arg instanceof GraphMessage) //All Other GraphUpdates are handled in VGRaphCommons
		{
			GraphMessage m = (GraphMessage) arg;
			if ( ((m.getModifiedElementTypes()&(GraphConstraints.HYPEREDGESHAPE))==GraphConstraints.HYPEREDGESHAPE)
					&&(m.getModification()==GraphConstraints.HISTORY))
			{ //Shape changed by History - Check For correct Modus and right buttons
				updatePanel(HShapeGraphicRef.getShapeParameters());
				return;
			}
			if ((m.getModifiedElementTypes()==GraphConstraints.HYPEREDGE)
					&&(m.getModification()==(GraphConstraints.UPDATE|GraphConstraints.HYPEREDGESHAPE|GraphConstraints.CREATION))) 
			{
				if  (cBasicShape.isVisible()) //We're in mode one and got an HyperEdgeShape Creation Update
				{
					NURBSCreationMessage nm = HShapeGraphicRef.getShapeParameters();
					if (DegreeFields.isVisible())
						updateDegreeFields(nm);
					if (CircleFields.isVisible())//with circles
						updateCircleFields(nm);
					else if (InterpolationFields.isVisible())
						updateIPFields(nm);
				}
			}
			//Button Activity
			boolean shape = false;
			if ((HGraphRef.modifyHyperEdges.get(HEdgeRefIndex).getShape()!=null)&&(!HGraphRef.modifyHyperEdges.get(HEdgeRefIndex).getShape().isEmpty()))
			{ //We have a given nonempty shape
				IPInfo.setText("<html><p>&nbsp;</p></html>");
				shape = true;
			}
			else
			{
				shape = false;
				CheckResult.setText("<html>&nbsp;</html>");
			}
			bCheckShape.setEnabled(shape);
			bModeChange.setEnabled(shape);
			bOk.setEnabled(shape);
			if (!shape) //Empty Shape
			{
				if (cBasicShape.isVisible()) //-> clear values
				{
					NURBSCreationMessage nm = HShapeGraphicRef.getShapeParameters();
					if (CircleFields.isVisible())//with circles
						updateCircleFields(nm);
					else if (InterpolationFields.isVisible())
						updateIPFields(nm);
				}
				else //Second Modus -> change to first
				{
					actionPerformed(new ActionEvent(bModeChange,0,"Change Modus"));
				}
			}
		}	//End Handling Graph Messages
	}
}
