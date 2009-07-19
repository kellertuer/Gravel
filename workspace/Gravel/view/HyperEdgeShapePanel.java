package view;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
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
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.KeyStroke;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import view.pieces.HESFreeModComponent;

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
public class HyperEdgeShapePanel implements ActionListener, Observer, CaretListener, ChangeListener {

	private Container cont;
	private JComboBox cBasicShape;
	private DefaultComboBoxModel cBasicShapeEntries;
	private JLabel BasicShape;
	//Circle Fields
	private JLabel CircleOriginX, CircleOriginY, CircleRadius;
	private IntegerTextField iCOrigX, iCOrigY, iCRad;
	//Interpolation Fields
	private JLabel Degree, IPInfo;
	private JSlider iDegree;
	private ButtonGroup bAddIP;
	private JRadioButton rAddEnd, rAddBetween;

	private JButton bModeChange, bOk, bCancel,	bCheckShape;
	private int HEdgeRefIndex; //Reference to the HyperEdge in the Graph that is edited here
	private VHyperGraph HGraphRef; //Reference to the edited Graph, should be a copy of the Graph from the main GUI because the user might cancel this dialog 
	private VHyperShapeGraphic HShapeGraphicRef;
	
	private JPanel CircleFields, InterpolationFields, DegreeFields;

	private HESFreeModComponent FreeModPanel;
	
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
		cont.setMinimumSize(new Dimension(200,80));
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(0,2,0,2);
		c.anchor = GridBagConstraints.NORTH;
		c.gridy = 0;
		c.gridx = 0;
		c.gridwidth=1;
		c.gridheight=1;
		//INput fields besides the Display
		String[] BasicShapes = {"Kreis", "Interpolation", "konvexe Hülle"};
		cBasicShapeEntries = new DefaultComboBoxModel(BasicShapes);
		cBasicShape = new JComboBox(cBasicShapeEntries);
		cBasicShape.setSelectedIndex(0);
		cBasicShape.setPreferredSize(new Dimension(130, 30));
		cBasicShape.addActionListener(this);
		BasicShape = new JLabel("<html><p>Grundform</p></html>");
		cont.add(BasicShape,c);
		c.gridx++;
		cont.add(cBasicShape,c);
		c.insets = new Insets(0,7,0,7);		
		c.gridy++;
		c.gridx=0;
		c.gridwidth=2;
		c.gridheight=2;
		c.insets = new Insets(15,7,0,7);
		
		//
		// Lay all Small Containers with Options at this position nonvisible
		//
		GridBagConstraints c2 = new GridBagConstraints();		
		c2.anchor = GridBagConstraints.CENTER;
		c2.gridx=0;
		c2.insets = (Insets) c.insets.clone();
		buildDegreePanel();
		JPanel firstmode = new JPanel();
		firstmode.setLayout(new GridBagLayout());
		c.gridy++;
		firstmode.add(DegreeFields,c2);
		DegreeFields.setVisible(true);
		c.gridy++;
		buildInterpolationPanel();
		firstmode.add(InterpolationFields,c2);
		setIPVisibility(true);
		buildCirclePanel();
		firstmode.add(CircleFields,c2);
		CircleFields.setVisible(true);
		c.fill = GridBagConstraints.NONE;
		firstmode.validate();

		//
		//Container for the fields of the second mode
		//
		c.gridx=0;
		c.gridwidth=2;
		c.gridheight=3;
		c.fill = GridBagConstraints.VERTICAL;
		FreeModPanel = new HESFreeModComponent(HEdgeRefIndex, HShapeGraphicRef);
		cont.add(FreeModPanel.getContent(),c);
		cont.add(firstmode,c);
		FreeModPanel.setVisible(false);
		
		c.gridy+=3;
		c.gridx=0;
		c.gridwidth=2;
		c.gridheight=1;

		//
		// General Buttons
		//
		c.fill = GridBagConstraints.NONE;

		c.insets.bottom += 5;
		c.anchor = GridBagConstraints.SOUTH;
		bModeChange = new JButton("Modifikation"); //Name suchen!
		bModeChange.setEnabled(true);
		//!HGraphRef.modifyHyperEdges.get(HEdgeRefIndex).getShape().isEmpty());
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
		cont.doLayout();
		setCreationFieldVisibility(false);
		//If we don't have a Shape...set it to visible because it's toggled at the end of this method to init visibility
		cBasicShape.setVisible(!HGraphRef.modifyHyperEdges.get(index).getShape().isEmpty());
		actionPerformed(new ActionEvent(bModeChange,0,"Refresh"));
	}
	public VHyperEdge getActualEdge()
	{
		return HGraphRef.modifyHyperEdges.get(HEdgeRefIndex);
	}
	
	private void buildCirclePanel()
	{
		CircleFields = new JPanel();
		CircleFields.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(5,7,5,7);
		c.gridy = 0;
		c.gridx = 0;
		c.gridwidth=1;
		c.gridheight=1;
		iCOrigX = new IntegerTextField();
		iCOrigX.addCaretListener(this);;
		iCOrigX.setPreferredSize(new Dimension(50, 20));
		CircleOriginX = new JLabel("<html><p>M<sub>X</sub></p></html>");
		CircleFields.add(CircleOriginX,c);
		c.gridx++;
		CircleFields.add(iCOrigX,c);

		c.gridy++;
		c.gridx=0;
		iCOrigY = new IntegerTextField();
		iCOrigY.addCaretListener(this);;
		iCOrigY.setPreferredSize(new Dimension(50, 20));
		CircleOriginY = new JLabel("<html><p>M<sub>Y</sub></p></html>");
		CircleFields.add(CircleOriginY,c);
		c.gridx++;
		CircleFields.add(iCOrigY,c);

		c.gridy++;
		c.gridx=0;
		iCRad = new IntegerTextField();
		iCRad.addCaretListener(this);;
		iCRad.setPreferredSize(new Dimension(50, 20));
		CircleRadius = new JLabel("<html><p>Radius</p></html>");
		CircleFields.add(CircleRadius,c);
		c.gridx++;
		CircleFields.add(iCRad,c);
		CircleFields.setBorder(BorderFactory.createTitledBorder("Kreis"));
//
		CircleFields.setSize(new Dimension(220,100));
	}
	private void buildDegreePanel()
	{
		DegreeFields = new JPanel();
		DegreeFields.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(5,7,5,7);
		c.gridy = 0;
		c.gridx = 0;
		c.gridwidth=2;
		c.gridheight=1;
		c.fill = GridBagConstraints.BOTH;
		iDegree = new JSlider(JSlider.HORIZONTAL,2,7,3);
	    iDegree.setMajorTickSpacing(1);
	    Dimension d = new Dimension(170,50);
	    iDegree.setSize(d);
	    iDegree.setPreferredSize(d);
	    iDegree.setPaintTicks(true);
	    iDegree.setPaintLabels(true);
	    iDegree.setSnapToTicks(true);
		iDegree.addChangeListener(this);
		iDegree.setValue(3);
		DegreeFields.add(iDegree,c);
		c.gridy++;
		c.gridx=0;
		c.gridwidth=2;
		IPInfo = new JLabel("<html><p>&nbsp;</p></html>");
		DegreeFields.add(IPInfo,c);
		DegreeFields.setMinimumSize(new Dimension(220,70));
		DegreeFields.setBorder(BorderFactory.createTitledBorder("Polynomgrad"));
	}
	private void buildInterpolationPanel()
	{
		InterpolationFields = new JPanel();
		InterpolationFields.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(5,7,5,7);
		c.anchor = GridBagConstraints.WEST;
		c.gridy = 0;
		c.gridx = 0;
		c.gridwidth=1;
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
		InterpolationFields.setBorder(BorderFactory.createTitledBorder("neue Punkte einfügen"));
		InterpolationFields.setSize(new Dimension(220,40));
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
				HGraphRef.modifyHyperEdges.get(HEdgeRefIndex).getMinimumMargin()+ (new Double(Math.ceil((double)HGraphRef.modifyHyperEdges.get(HEdgeRefIndex).getWidth()))).intValue(),
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
			
			if ((iCOrigX.getValue() > 0)&&(iCOrigY.getValue() > 0)&&(iCRad.getValue() > 0))
			{ //All valid
				NURBSCreationMessage old = HShapeGraphicRef.getShapeParameters();
				if (old==null)
					return; //invalid
				if  (  
					   (old.getType()==NURBSCreationMessage.CIRCLE)	
					&& (iCOrigX.getValue()==Math.round((float)old.getPoints().firstElement().getX()))
					&& (iCOrigY.getValue()==Math.round((float)old.getPoints().firstElement().getY()))
					&& (iCRad.getValue()==old.getValues().firstElement().intValue())	)
					return; //No Value changed
				NURBSCreationMessage nm = new NURBSCreationMessage(
						4, //TODO: Enable Circles with Degree
						new Point2D.Double((new Integer(iCOrigX.getValue())).doubleValue(), (new Integer(iCOrigY.getValue())).doubleValue()),
						iCRad.getValue()
				);
				HGraphRef.modifyHyperEdges.get(HEdgeRefIndex).setShape(NURBSShapeFactory.CreateShape(nm));
				HShapeGraphicRef.setShapeParameters(nm);
				HGraphRef.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE, HEdgeRefIndex, GraphConstraints.UPDATE|GraphConstraints.HYPEREDGESHAPE|GraphConstraints.CREATION, GraphConstraints.HYPEREDGE)); //HyperEdgeShape Updated
			}
		}
	}
	
	private void setCreationFieldVisibility(boolean visible)
	{
 		CircleFields.setVisible(visible);
		setIPVisibility(visible);
    	DegreeFields.setVisible(visible);
		cBasicShape.setVisible(visible);
		BasicShape.setVisible(visible);
	}
	
	private void setIPVisibility(boolean visible)
	{
		InterpolationFields.setVisible(visible);
		
		if (visible)
		{	//Handling Escape as Cancel
			//Add ESC-Handling
			InputMap iMap = Gui.getInstance().getParentWindow().getRootPane().getInputMap(	 JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
			iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ALT, KeyEvent.ALT_DOWN_MASK), "alternative");

			ActionMap aMap = Gui.getInstance().getParentWindow().getRootPane().getActionMap();
			aMap.put("alternative", new AbstractAction()
				{
					private static final long serialVersionUID = 1L;
					public void actionPerformed(ActionEvent e)
					{
						System.err.println("pressed....");
						rAddBetween.doClick();					
					}
				});
		}
		else //remove
		{
    		InputMap iMap = Gui.getInstance().getParentWindow().getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    		iMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_ALT, 0));

    		ActionMap aMap = Gui.getInstance().getParentWindow().getRootPane().getActionMap();
    		aMap.remove("alternative");
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
	        	DegreeFields.setVisible(true);
	        	iDegree.setEnabled(false);
	        	String Shape = (String)cBasicShape.getModel().getSelectedItem();
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
		        	iDegree.setEnabled(true);
	        	}
	        	else if (Shape.equals("konvexe Hülle"))
	        	{
	        		HShapeGraphicRef.setMouseHandling(VCommonGraphic.NO_MOUSEHANDLING);
	        		CalculateConvexHullShape();
	        		DegreeFields.setVisible(true);
		        	iDegree.setEnabled(true);
	        	}
	        	else
	        	{
	        		HShapeGraphicRef.setMouseHandling(VCommonGraphic.NO_MOUSEHANDLING);
	        	}
	        }
	        if (e.getSource()==bModeChange) //Modus Change Button
	        {
	        	if (cBasicShape.isVisible()) //We are in mode 1 -> change to 2
	        	{
	        		bModeChange.setText("neue Grundfrom");
	        		FreeModPanel.setVisible(true);
	        		FreeModPanel.refresh();
	        		setCreationFieldVisibility(false);
	        		if ((HGraphRef.modifyHyperEdges.get(HEdgeRefIndex).getShape().getDecorationTypes()&NURBSShape.FRAGMENT)==NURBSShape.FRAGMENT)
	        		{
	        			FreeModPanel.resetModus();
	        			FreeModPanel.refresh();
	        		}
	        	}
	        	else //we are in 2 -> change to 1
	        	{
	        		bModeChange.setText("Modifikation");
	        		FreeModPanel.setVisible(false);
	        		cBasicShape.setVisible(true);
	        		BasicShape.setVisible(true);
	        		//New Subcurve
	        		if ((HGraphRef.modifyHyperEdges.get(HEdgeRefIndex).getShape().getDecorationTypes()&NURBSShape.FRAGMENT)==NURBSShape.FRAGMENT)
	        		{
	        			if (cBasicShapeEntries.getIndexOf("Kreis")!=-1)
	        				cBasicShapeEntries.removeElementAt(cBasicShapeEntries.getIndexOf("Kreis"));
	        			if (cBasicShapeEntries.getIndexOf("konvexe Hülle")!=-1)	        			
	        				cBasicShapeEntries.removeElementAt(cBasicShapeEntries.getIndexOf("konvexe Hülle"));
	        			DegreeFields.setVisible(false);
	        		}
	        		else
	        		{
	        			if (cBasicShapeEntries.getIndexOf("Kreis")==-1)
	        				cBasicShapeEntries.insertElementAt("Kreis",0);
	        			if (cBasicShapeEntries.getIndexOf("konvexe Hülle")==-1)
	        				cBasicShapeEntries.insertElementAt("konvexe Hülle",2);
	        			DegreeFields.setVisible(true);
	        		}
	        		cont.repaint();
	        		actionPerformed(new ActionEvent(cBasicShape,0,"Refresh"));
	        	}
	        }
	        else if ((e.getSource()==rAddBetween)||(e.getSource()==rAddEnd))
	        {
	       		NURBSCreationMessage nm = HShapeGraphicRef.getShapeParameters();
				if (rAddBetween.isSelected())
					nm.setStatus(NURBSCreationMessage.ADD_BETWEEN);
				else
					nm.setStatus(NURBSCreationMessage.ADD_END);
				HShapeGraphicRef.setShapeParameters(nm);
	        }	
	        else if (e.getSource()==bCheckShape)
	        {
	        	NURBSShapeValidator validator = new NURBSShapeValidator(HGraphRef, HEdgeRefIndex, null, HShapeGraphicRef); //Check actual Shape of the Edge
	    		if (validator.isShapeValid())
	    		{
	    			JOptionPane.showMessageDialog(Gui.getInstance().getParentWindow(), "<html><center>Der Umriss ist korrekt.</center><br><br><ul><li>Alle Knoten der Hyperkante sind innerhalb des Umrisses</li><li>Alle Knoten der Hyperkante sind mindestens "+HGraphRef.modifyHyperEdges.get(HEdgeRefIndex).getMinimumMargin()+"px</li><li>Alle anderen Knoten sind außerhalb des Umrisses</li></ul></html>", "Der Umriss ist korrekt.", JOptionPane.INFORMATION_MESSAGE);
	    			HShapeGraphicRef.setHighlightedNodes(new Vector<Integer>());
	    		}
	    		else
	    		{
		    		String msg = "<html><center>Der Umriss ist nicht korrekt</center><br><br>Die folgenden Knoten erfüllen nicht die Korrektheit.<br>"+
		    				"Einer der folgenden F"+CONST.html_ae+"lle trifft also zu:<ul><li>au"+CONST.html_sz+"erhalb des Umrisses und geh"+CONST.html_oe+"ren zur Kante</li><li>im Umriss und geh"+CONST.html_oe+"ren nicht zur Kante</li><li>Sie sind im Umriss, aber erf"+CONST.html_ue+"llen den Innenabstand nicht</li></ul>Diese Knoten werden bis zur n"+CONST.html_ae+"chten Uberpr"+CONST.html_ue+"fung rot hervorgehoben.<br><br>";		    		
		    		Vector<Integer> WrongNodes = validator.getInvalidNodeIndices();
		    		for (int j=0; j<WrongNodes.size(); j++)
		    				msg+="#"+WrongNodes.get(j);
		    		if (WrongNodes.size()==0)
		    			msg += "Es gab keine eindeutige Entscheidung, am Ende mehr als 2 Mengen verblieben.";
		    		msg+="</html>";
	    			JOptionPane.showMessageDialog(Gui.getInstance().getParentWindow(), msg, "Der Umriss ist nicht korrekt.", JOptionPane.ERROR_MESSAGE);
	    			HShapeGraphicRef.setHighlightedNodes(WrongNodes);
	    		}
	        }
	        getContent().validate();
	        getContent().repaint();
	}
	public void stateChanged(ChangeEvent e) {
		if ((e.getSource()==iDegree)&&(iDegree.getValue()>0))
		{
	       	String Shape = (String)cBasicShape.getSelectedItem();
       		NURBSCreationMessage nm = HShapeGraphicRef.getShapeParameters();
       		if (nm==null)
       			return;
       		if (iDegree.getValue()==nm.getDegree())
       			return; //No Change
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
	//Update Fields to fit the values of a message
	private void updateDegreeFields(NURBSCreationMessage nm)
	{
		if ((nm==null)||(!nm.isValid()))
		{ //Perhaps set degree back to Std Value
			return;
		}
		int deg = nm.getDegree();
		if ((iDegree.getValue()==deg)||(deg<=0))
			return;
		iDegree.removeChangeListener(this);
		iDegree.setValue(deg);
		iDegree.addChangeListener(this);
		iDegree.setEnabled(nm.getType()!=NURBSCreationMessage.CIRCLE);
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
		DegreeFields.setVisible((nm.getCurve().getDecorationTypes()&NURBSShape.FRAGMENT)!=NURBSShape.FRAGMENT);
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
		
		int i= p.size();
		if (nm.getType()==NURBSCreationMessage.CONVEX_HULL)
			i *= 3;
			
		i = 2*deg-i;
		
		if (i==1)
			IPInfo.setText("<html><p>Noch ein Punkt notwendig.</p></html>");
		else if (i>1)
		IPInfo.setText("<html><p>Noch "+i+" Punkte notwendig.</p></html>");
		else
			IPInfo.setText("<html><p>&nbsp;</p></html>");
	}

	private void updatePanel(NURBSCreationMessage nm)
	{
		if (nm==null) //Init to second Modus
		{
			if (cBasicShape.isVisible())
				actionPerformed(new ActionEvent(bModeChange,0,"Change Modus"));
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
				updateDegreeFields(nm);
				updateCircleFields(nm);
			break;
			case NURBSCreationMessage.CONVEX_HULL: //Init to that
				cBasicShape.setSelectedIndex(2);
				updateDegreeFields(nm);
			break;
		}
		//Update second
		FreeModPanel.refresh();
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
			boolean shape = ((HGraphRef.modifyHyperEdges.get(HEdgeRefIndex).getShape()!=null)&&(!HGraphRef.modifyHyperEdges.get(HEdgeRefIndex).getShape().isEmpty()));
			if ((FreeModPanel.isLocal())&&(!cBasicShape.isVisible())) //Mode2 and SubcurveSelection
			{
				NURBSShape s = HGraphRef.modifyHyperEdges.get(HEdgeRefIndex).getShape();
				if ((s.getDecorationTypes()&NURBSShape.FRAGMENT)==NURBSShape.FRAGMENT) //Looks fuzzy but the Cast next if can't be in the same as this
					if (!((NURBSShapeFragment)s).getSubCurve().isEmpty())
					{
						bModeChange.setEnabled(NURBSShapeFactory.SubcurveSubstitutable((NURBSShapeFragment)s));
					}
					else
						bModeChange.setEnabled(false);
				else
					bModeChange.setEnabled(false);
			}
			else
				bModeChange.setEnabled(shape);

			bCheckShape.setEnabled(shape);
			bOk.setEnabled(shape);
			if (shape)
			{
				IPInfo.setText("<html><p>&nbsp;</p></html>");
				if (FreeModPanel.getContent().isVisible())
					FreeModPanel.refresh();
			}
			else //empty shape
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
