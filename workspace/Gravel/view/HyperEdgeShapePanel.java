package view;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.KeyStroke;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import control.nurbs.ShapeModificationHandler;

import dialogs.IntegerTextField;


import model.MHyperEdge;
import model.NURBSShapeFactory;
import model.VHyperEdge;
import model.VHyperEdgeShape;
import model.VHyperGraph;
import model.VNode;
import model.Messages.GraphConstraints;
import model.Messages.GraphMessage;

/**
 * This Class represents all GUI-Elements, e.g. Parameters / Buttons,
 * for manipulating the Shape of a single hyperedge (the drawing is in the
 * initially passed VHyperShapeGraphic
 * 
 * this includes its CreationParameters, e.g. a Circle 
 * TODO Modification Parameters and Modi
 * TODO Validation of the Shape (distance to each Node of the VHyperEdge, and whether they are all inside the Shape)
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
	private IntegerTextField iDistance;
	private JComboBox cBasicShape;
	private JLabel Distance, BasicShape;
	//Circle Fields
	private JLabel CircleOriginX, CircleOriginY, CircleRadius;
	private IntegerTextField iCOrigX, iCOrigY, iCRad;
	//Interpolation Fields
	private JLabel Degree, IPInfo;
	private IntegerTextField iDegree;
	//FreeModFields
	private JLabel knots, degree;
	private JButton bIncKnots, bDecKnots, bIncDegree, bDecDegree;
	
	private JButton bModeChange, bOk, bCancel;
	private JButton bRotation,bTranslation, bScaling;
	private int HEdgeRefIndex; //Reference to the HyperEdge in the Graph that is edited here
	private VHyperGraph HGraphRef; //Reference to the edited Graph, should be a copy of the Graph from the main GUI because the user might cancel this dialog 
	private VHyperShapeGraphic HShapeGraphicRef;
	
	private Container CircleFields, InterpolationFields, FreeModFields;
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
		c.anchor = GridBagConstraints.WEST;
		c.gridy = 0;
		c.gridx = 0;
		c.gridwidth=1;
		c.gridheight=1;
		//INput fields besides the Display
		iDistance = new IntegerTextField();
		iDistance.addCaretListener(this);;
		iDistance.setPreferredSize(new Dimension(100, 20));
		iDistance.setValue(15); //TODO-Std Value
		//TODO Change to a Slider with actual Value in the middle and the possibility to vary that by -+10?
		Distance = new JLabel("<html><p>Mindestabstand<br><font size=\"-2\">Knoten \u2194 Umriss</font></p></html>");
		cont.add(Distance,c);
		c.gridx++;
		cont.add(iDistance,c);

		c.gridy++;
		c.gridx=0;
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
		c.insets = new Insets(30,7,0,7);
		
		//
		// Lay all Small Containers with Options at this position nonvisible
		//

		buildCirclePanel();
		cont.add(CircleFields,c);
		CircleFields.setVisible(false);
		
		buildInterpolationPanel();
		cont.add(InterpolationFields,c);
		InterpolationFields.setVisible(false);

		//
		//Container for the fields of the second mode
		//
		buildFreeModPanel();
		cont.add(FreeModFields,c);
		FreeModFields.setVisible(false);
		
		c.gridy++;
		c.gridx=0;
		c.gridwidth=2;
		
		c.anchor = GridBagConstraints.SOUTH;
		bModeChange = new JButton("freie Modifikation (?)"); //Name suchen!
		bModeChange.setEnabled(!HGraphRef.modifyHyperEdges.get(HEdgeRefIndex).getShape().isEmpty());
		bModeChange.addActionListener(this);
		cont.add(bModeChange,c);

		c.gridy++;
		c.gridx=0;
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

	private void buildInterpolationPanel()
	{
		InterpolationFields = new Container();
		InterpolationFields.setLayout(new GridBagLayout());
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
		iDegree.setValue(4);
		Degree = new JLabel("<html><p>Polynomgrad</p></html>");
		InterpolationFields.add(Degree,c);
		c.gridx++;
		InterpolationFields.add(iDegree,c);
		c.gridy++;
		c.gridx=0;
		c.gridwidth=3;
		c.gridheight=2;
		IPInfo = new JLabel("<html><p>&nbsp;</p></html>");

		InterpolationFields.add(IPInfo,c);
	}
	
	private void buildFreeModPanel()
	{
		FreeModFields = new Container();
		FreeModFields.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(5,3,5,3);
		c.anchor = GridBagConstraints.WEST;
		c.gridy = 0;
		c.gridx = 0;
		c.gridwidth=2;
		c.gridheight=1;

		knots = new JLabel("Knotenvektor");
		FreeModFields.add(knots,c);

		c.gridx++;
		c.gridwidth=1;
		bIncKnots = new JButton("+");
		bIncKnots.addActionListener(this);
		FreeModFields.add(bIncKnots,c);

		c.gridx++;
		bDecKnots = new JButton("-");
		bDecKnots.setSize(new Dimension(17,17));
		bDecKnots.setEnabled(false);
		FreeModFields.add(bDecKnots,c);
		
		c.gridy++;
		c.gridx=0;
		c.gridwidth=2;
		degree = new JLabel("Polynomgrad: "+iDegree.getValue());
		FreeModFields.add(degree,c);
		
		c.gridx++;
		c.gridwidth=1;
		bIncDegree = new JButton("+");
		bIncDegree.setSize(new Dimension(17,17));
		bIncDegree.setEnabled(false);
		FreeModFields.add(bIncDegree,c);
		
		c.gridx++;
		bDecDegree = new JButton("-");
		bDecDegree.setSize(new Dimension(17,17));
		bDecDegree.setEnabled(false);
		FreeModFields.add(bDecDegree,c);

		c.gridy++;
		c.gridx=0;
		c.gridwidth=1;
		bRotation = new JButton("R");
		bRotation.setSize(new Dimension(17,17));
		bRotation.addActionListener(this);
		FreeModFields.add(bRotation,c);

		c.gridx++;
		c.gridwidth=1;
		bTranslation = new JButton("T");
		bTranslation.setSize(new Dimension(17,17));
		bTranslation.addActionListener(this);
		FreeModFields.add(bTranslation,c);

		c.gridx++;
		c.gridwidth=1;
		bScaling = new JButton("S");
		bScaling.setSize(new Dimension(17,17));
		bScaling.addActionListener(this);
		FreeModFields.add(bScaling,c);

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
		Vector<Object> params = new Vector<Object>();
		params.setSize(NURBSShapeFactory.MAX_INDEX);
		params.set(NURBSShapeFactory.DEGREE,iDegree.getValue());
		params.set(NURBSShapeFactory.POINTS, nodepos);
		params.set(NURBSShapeFactory.SIZES, nodesizes);
		params.set(NURBSShapeFactory.DISTANCE_TO_NODE, iDistance.getValue());
		VHyperEdgeShape s = NURBSShapeFactory.CreateShape("convex hull", params);
		if (s.isEmpty())
		{
			IPInfo.setText("<html><p>Polynomgrad "+iDegree.getValue()+" ist zu hoch.</p></html>");
			return;
		}
		else
			IPInfo.setText("<html><p>&nbsp;</p></html>");
		HGraphRef.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,GraphConstraints.BLOCK_START|GraphConstraints.UPDATE|GraphConstraints.HYPEREDGESHAPE,GraphConstraints.HYPEREDGE));
		HShapeGraphicRef.setShapeParameters(params);
		HGraphRef.modifyHyperEdges.get(HEdgeRefIndex).setShape(s);
		HGraphRef.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,GraphConstraints.BLOCK_END));			
	}
	
	public void caretUpdate(CaretEvent e)
	{
		
		if ((e.getSource()==iCOrigX)||(e.getSource()==iCOrigY)||(e.getSource()==iCRad))
		{
			
			if ((iCOrigX.getValue()!=-1)&&(iCOrigY.getValue()!=-1)&&(iCRad.getValue()!=-1)&&(iDistance.getValue()!=-1))
			{
				Vector<Object> param = new Vector<Object>();
				param.setSize(NURBSShapeFactory.MAX_INDEX);
				param.add(NURBSShapeFactory.CIRCLE_ORIGIN, new Point(iCOrigX.getValue(), iCOrigY.getValue()));
				param.add(NURBSShapeFactory.CIRCLE_RADIUS, iCRad.getValue());
				param.add(NURBSShapeFactory.DISTANCE_TO_NODE,iDistance.getValue()); 			
				HGraphRef.modifyHyperEdges.get(HEdgeRefIndex).setShape(NURBSShapeFactory.CreateShape("Circle",param));
				HGraphRef.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE, GraphConstraints.UPDATE|GraphConstraints.HYPEREDGESHAPE)); //HyperEdgeShape Updated
			}
		}
		if ((e.getSource()==iDegree)&&(iDegree.getValue()>0))
		{
	       	String Shape = (String)cBasicShape.getSelectedItem();
	       	if (Shape.equals("Interpolation"))
        	{
	       		Vector<Object> param = HShapeGraphicRef.getShapeParameters();
	       		param.set(NURBSShapeFactory.DEGREE, iDegree.getValue());
	       		HShapeGraphicRef.setShapeParameters(param);
        	}
        	else if (Shape.equals("konvexe Hülle"))
        	{
        		CalculateConvexHullShape();
        	}
	       	updateInfo();
		}
	}
	private void deselectButtons()
	{
		bRotation.setSelected(false);
		bTranslation.setSelected(false);
		bScaling.setSelected(false);
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
	        	}
	        	else if (Shape.equals("konvexe Hülle"))
	        	{
	        		HShapeGraphicRef.setMouseHandling(VCommonGraphic.NO_MOUSEHANDLING);
	        		CalculateConvexHullShape();
	        		InterpolationFields.setVisible(true);
	        	}
	        	else
	        	{
	        		HShapeGraphicRef.setMouseHandling(VCommonGraphic.NO_MOUSEHANDLING);
	        		CircleFields.setVisible(false);
	        		InterpolationFields.setVisible(false);
	        	}
	        }
	        if (e.getSource()==bModeChange)
	        {
	        	if (cBasicShape.isVisible()) //We are in mode 1
	        	{
	        		bModeChange.setText("neue Grundform");
	        		CircleFields.setVisible(false);
	        		InterpolationFields.setVisible(false);
	        		cBasicShape.setVisible(false);
	        		BasicShape.setVisible(false);
	        		FreeModFields.setVisible(true);
	        		HShapeGraphicRef.setMouseHandling(VCommonGraphic.CURVEPOINT_MOVEMENT_MOUSEHANDLING);
	        	}
	        	else
	        	{
	        		bModeChange.setText("Freie Bearbeitung");
	        		FreeModFields.setVisible(false);
	        		cBasicShape.setVisible(true);
	        		BasicShape.setVisible(true);
	        		actionPerformed(new ActionEvent(cBasicShape,0,"Refresh"));
	        	}
	        }
	        if (e.getSource()==bIncKnots)
	        {
	        	HGraphRef.modifyHyperEdges.get(HEdgeRefIndex).getShape().refineMiddleKnots();
	        	HShapeGraphicRef.setMouseHandling(HShapeGraphicRef.getMouseHandling()); //ReInit Drag/Click
	        }
	        if (e.getSource()==bRotation)
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
	        if (e.getSource()==bTranslation)
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
	        if (e.getSource()==bScaling)
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
	        getContent().repaint();
	}
	private void updateCircleFields()
	{
		Vector<Object> params = HShapeGraphicRef.getShapeParameters();
		Point porig = (Point) params.get(NURBSShapeFactory.CIRCLE_ORIGIN);
		int size = Integer.parseInt(params.get(NURBSShapeFactory.CIRCLE_RADIUS).toString());
		if (porig==null) //There is no Shape-Stuff anymore but we have to update the Button for Mode again
		{
			return;
		}				
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
	
	private void updateIPFields()
	{
		Vector<Object> params = HShapeGraphicRef.getShapeParameters();
		int deg = Integer.parseInt(params.get(NURBSShapeFactory.DEGREE).toString());
		if (iDegree.getValue()==deg)
			return;
		iDegree.removeCaretListener(this);
		iDegree.setValue(deg);
		iDegree.addCaretListener(this);
		updateInfo();
	}
	private void updateInfo()
	{
		Vector<Object> params = HShapeGraphicRef.getShapeParameters();
		int deg=0;
		try
		{ deg = Integer.parseInt(params.get(NURBSShapeFactory.DEGREE).toString());}
		catch (Exception e)
		{return;}
		Vector<Point2D> p=null;
		try
		{
			p = (Vector<Point2D>) params.get(NURBSShapeFactory.POINTS);
		}
		catch (Exception e)
		{
			return;
		}
		if (p==null)
			return;
		
		if (p.size()<=deg)
			IPInfo.setText("<html><p>Zu wenig Punkte f"+main.CONST.html_ue+"r Polynomgrad "+deg+".</p></html>");
		else
			IPInfo.setText("");
	}
	public void update(Observable o, Object arg) {
		if (arg instanceof GraphMessage) //All Other GraphUpdates are handled in VGRaphCommons
		{
			GraphMessage m = (GraphMessage) arg;
			if ((m.getModifiedElementTypes()==GraphConstraints.HYPEREDGE)
				&&(m.getModification()==(GraphConstraints.UPDATE|GraphConstraints.HYPEREDGESHAPE)))
			{
				if  (cBasicShape.isVisible()) //We're in mode one with circles
				{
					if (CircleFields.isVisible())
						updateCircleFields();
					else if (InterpolationFields.isVisible())
						updateIPFields();
				}
			}
			if (!HGraphRef.modifyHyperEdges.get(HEdgeRefIndex).getShape().isEmpty())
				IPInfo.setText("<html><p>&nbsp;</p></html>");
			bModeChange.setEnabled(!HGraphRef.modifyHyperEdges.get(HEdgeRefIndex).getShape().isEmpty());
			bOk.setEnabled(!HGraphRef.modifyHyperEdges.get(HEdgeRefIndex).getShape().isEmpty());
		}	
	}
}
