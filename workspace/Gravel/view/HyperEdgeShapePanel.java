package view;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
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

import dialogs.IntegerTextField;


import model.NURBSShapeFactory;
import model.VHyperEdge;
import model.VHyperGraph;
import model.Messages.GraphConstraints;
import model.Messages.GraphMessage;

/**
 * This Class represents all GUI-Elements, e.g. Parameters / Buttons,
 * for manipulating the Shape of a single hyperedge (the drawing is in the
 * initially passed VHyperShapeGraphic
 * 
 * this includes its CreationParameters, e.g. a Circle 
 * - Modification Parameters and Modi (TODO)
 * - Validation of the Shape (distance to each Node of the VHyperEdge, and whether they are all inside the Shape TODO)
 * 
 * Therefore the interaction is split into 2 seperate steps:
 * 1) Create a Basic Shape, e.g. a circle, polygon or Interpolation Points
 * 2) Modify the Shape by
 *  -- dragging specific Shape-Areas,
 *  -- In- and Decreasing both knots and degree
 *  	(perhaps select a pasrt of the shape and in/decrease knots there?)
 *  -- Rotation, Scaling Shifting of the whole Shape (or selections? Is that possible?)
 *  
 *  TODO Despite that a second mode has the possibility to bet set to expert mode, where you can drag ControlPoints
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
	
	private JLabel CircleOriginX, CircleOriginY, CircleRadius;
	private IntegerTextField iCOrigX, iCOrigY, iCRad;

	private JLabel knots, degree, Todo;
	private JButton bIncKnots, bDecKnots, bIncDegree, bDecDegree;
	
	private JButton bModeChange, bOk, bCancel;

	private int HEdgeRefIndex; //Reference to the HyperEdge in the Graph that is edited here
	private VHyperGraph HGraphRef; //Reference to the edited Graph, should be a copy of the Graph from the main GUI because the user might cancel this dialog 
	private VHyperShapeGraphic HShapeGraphicRef;

	private Container CircleFields, FreeModFields;
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
		//TODO Change to a Slider with actual Value in the middle and the possibility to vary that by -+10?
		Distance = new JLabel("<html><p>Mindestabstand<br><font size=\"-2\">Knoten \u2194 Umriss</font></p></html>");
		cont.add(Distance,c);
		c.gridx++;
		cont.add(iDistance,c);

		c.gridy++;
		c.gridx=0;
		String[] BasicShapes = {"Kreis", "TODO", "mehr", "grundformen"};
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
		
		//Container for the fields of the second mode
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
		iCOrigX.setPreferredSize(new Dimension(80, 20));
		CircleOriginX = new JLabel("<html><p>Mittelpunkt X</p></html>");
		CircleFields.add(CircleOriginX,c);
		c.gridx++;
		CircleFields.add(iCOrigX,c);

		c.gridy++;
		c.gridx=0;
		iCOrigY = new IntegerTextField();
		iCOrigY.addCaretListener(this);;
		iCOrigY.setPreferredSize(new Dimension(80, 20));
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
	
	private void buildFreeModPanel()
	{
		FreeModFields = new Container();
		FreeModFields.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(5,3,5,3);
		c.anchor = GridBagConstraints.WEST;
		c.gridy = 0;
		c.gridx = 0;
		c.gridwidth=1;
		c.gridheight=1;

		knots = new JLabel("Knotenvektor");
		FreeModFields.add(knots,c);

		c.gridy++;
		bIncKnots = new JButton("mehr");
		bIncKnots.addActionListener(this);
		FreeModFields.add(bIncKnots,c);

		c.gridx++;
		bDecKnots = new JButton("weniger");
		bDecKnots.setEnabled(false);
		FreeModFields.add(bDecKnots,c);
		
		c.gridy++;
		c.gridx=0;
		degree = new JLabel("Polynomgrad: "+3); //TODO get Initial Std Polynomdegree
		FreeModFields.add(degree,c);
		
		c.gridy++;
		bIncDegree = new JButton("<html>erh"+main.CONST.html_oe+"hen</html>");
		bIncDegree.setEnabled(false);
		FreeModFields.add(bIncDegree,c);
		
		c.gridx++;
		bDecDegree = new JButton("verringern");
		bDecDegree.setEnabled(false);
		FreeModFields.add(bDecDegree,c);

		c.gridy++;
		c.gridx=0;
		c.gridwidth=2;
		Todo = new JLabel("<html><p><i>TODO:</i><br>Drehen,<br>Skalieren,<br>Verschieben,<br>Erweitern-Buttons<br>bzw. Icons,...</p></html>");
		FreeModFields.add(Todo,c);

	}
	/**
	 * get the GUI-Content
	 * @return
	 */
	public Container getContent()
	{
		return cont;
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
				param.add(NURBSShapeFactory.DISTANCE_TO_NODE,iDistance.getValue()); //TODO-Std Value			
				HGraphRef.modifyHyperEdges.get(HEdgeRefIndex).setShape(NURBSShapeFactory.CreateShape("Circle",param));
				HGraphRef.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE, GraphConstraints.UPDATE|GraphConstraints.HYPEREDGESHAPE)); //HyperEdgeShape Updated
			}
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
	        	String Shape = (String)cBasicShape.getSelectedItem();
	        	if (Shape.equals("Kreis"))
	        	{
	        		HShapeGraphicRef.setMouseHandling(VCommonGraphic.CIRCLE_MOUSEHANDLING);
	        		CircleFields.setVisible(true);
	        	}
	        	else
	        	{
	        		HShapeGraphicRef.setMouseHandling(VCommonGraphic.NO_MOUSEHANDLING);
	        		CircleFields.setVisible(false);
	        	}
	        }
	        if (e.getSource()==bModeChange)
	        {
	        	if (cBasicShape.isVisible()) //We are in mode 1
	        	{
	        		bModeChange.setText("neue Grundform");
	        		CircleFields.setVisible(false);
	        		cBasicShape.setVisible(false);
	        		BasicShape.setVisible(false);
	        		FreeModFields.setVisible(true);
	        		HShapeGraphicRef.setMouseHandling(VCommonGraphic.SHAPE_MODIFICATION_MOUSEHANDLING);
	        	}
	        	else
	        	{
	        		bModeChange.setText("freie Modifikation (?)");
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

	}

	public void update(Observable o, Object arg) {
		if (arg instanceof GraphMessage) //All Other GraphUpdates are handled in VGRaphCommons
		{
			GraphMessage m = (GraphMessage) arg;
			if ((m.getModifiedElementTypes()==GraphConstraints.HYPEREDGE)
				&&(m.getModification()==(GraphConstraints.UPDATE|GraphConstraints.HYPEREDGESHAPE)))
			{
				if  (cBasicShape.isVisible()&&((((String)cBasicShape.getSelectedItem()).equals("Kreis"))))
				{
					Vector<Object> params = HShapeGraphicRef.getShapeParameters();
					Point porig = (Point) params.get(NURBSShapeFactory.CIRCLE_ORIGIN);
					int size = Integer.parseInt(params.get(NURBSShapeFactory.CIRCLE_RADIUS).toString());
					if (porig==null) //There is no Shape-Stuff anymore but we have to update the Button for Mode again
					{//Update Activity of Button, because the last one is without new info.
						bModeChange.setEnabled(!HGraphRef.modifyHyperEdges.get(HEdgeRefIndex).getShape().isEmpty());
						bOk.setEnabled(!HGraphRef.modifyHyperEdges.get(HEdgeRefIndex).getShape().isEmpty());

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
			}
		}	
	}
}
