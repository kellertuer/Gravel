package dialogs.components;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import dialogs.IntegerTextField;

import view.VCommonGraphic;
import view.VHyperShapeGraphic;

import model.NURBSShapeFactory;
import model.VHyperEdge;
import model.VHyperGraph;
import model.Messages.GraphConstraints;
import model.Messages.GraphMessage;

/**
 * This Class represents all GUI-Elements for manipulating the 
 * Shape of a single hyperedge,
 * including its Creation, modification and check for validity
 * 
 * Therefore the interaction is split into 2 seperate steps:
 * 1) Create a Basic Shape, e.g. a circle, polygon or Interpolation Points
 * 2) Modify the Shape by
 *  
 *  -- dragging specific Shape-Areas,
 *  -- In- and Decreasing both knots and degree
 *  	(perhaps select a pasrt of the shape and in/decrease knots there?)

 *  Despite that this second mode has the possibility to bet set to expert mode, where you can drag ControlPoints
 *		(and perhaps change their weight by double click?)  
 * 
 * Based on a single Hyperedgeindex and the corresponding VHyperGraph the
 * GUI is initialized
 * @author Ronny Bergmann
 * @since 0.4
 *
 */
public class CHyperEdgeShapeParameters implements CaretListener, ActionListener, Observer {

	private VHyperShapeGraphic Editfield;
	private Container cont;
	private IntegerTextField iDistance;
	private JComboBox cBasicShape;
	private JLabel Distance, BasicShape;
	
	private JLabel CircleOriginX, CircleOriginY, CircleRadius;
	private IntegerTextField iCOrigX, iCOrigY, iCRad;

	private JLabel knots, degree, Todo;
	private JButton bIncKnots, bDecKnots, bIncDegree, bDecDegree;
	
	private JButton bModeChange;

	private int HEdgeRefIndex; //Reference to the HyperEdge in the Graph that is edited here
	private VHyperGraph HGraphRef; //Reference to the edited Graph, should be a copy of the Graph from the main GUI because the user might cancel this dialog 

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
	public CHyperEdgeShapeParameters(int index, VHyperGraph vhg)
	{
		if (vhg.modifyHyperEdges.get(index)==null)
			return;
		HEdgeRefIndex = index;
		HGraphRef = vhg;
		vhg.addObserver(this);
		cont = new Container();
		cont.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(0,7,0,7);
		c.anchor = GridBagConstraints.WEST;
		c.gridy = 0;
		c.gridx = 0;
		c.gridwidth=2;
		c.gridheight=4;
		Dimension d = new Dimension(400,400);
		Editfield = new VHyperShapeGraphic(d,vhg,index);
		Editfield.setMouseHandling(VHyperShapeGraphic.NO_MOUSEHANDLING);
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
		c.gridx=2;
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
		c.gridx=2;
		c.gridwidth=2;
		c.insets = new Insets(30,7,0,7);
		
		//
		// Lay all Small Containers with Options at this position nonvisible
		//

		buildCirclePanel();
		cont.add(CircleFields,c);
		CircleFields.setVisible(false);
		
		buildFreeModPanel();
		cont.add(FreeModFields,c);
		FreeModFields.setVisible(false);
		
		c.gridy++;
		c.gridx=2;
		c.gridwidth=2;
		
		c.anchor = GridBagConstraints.SOUTHWEST;
		bModeChange = new JButton("freie Modifikation (?)"); //Name suchen!
		bModeChange.setEnabled(HGraphRef.modifyHyperEdges.get(HEdgeRefIndex).getShape().isEmpty());
		bModeChange.addActionListener(this);
		cont.add(bModeChange,c);
		
		//Container for the fields of the second mode
		
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
	public void repaint()
	{
		cont.repaint();
		
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
				//TODO Change to VHyperEdge Update but up to now just for redraw
				HGraphRef.pushNotify(new GraphMessage(GraphConstraints.SELECTION,GraphConstraints.UPDATE));			
			}
		}
	}
	
	public void actionPerformed(ActionEvent e) {
	        if (e.getSource()==cBasicShape)//ComboBox
	        {
	        	String Shape = (String)cBasicShape.getSelectedItem();
	        	if (Shape.equals("Kreis"))
	        	{
	        		Editfield.setMouseHandling(VCommonGraphic.CIRCLE_MOUSEHANDLING);
	        		CircleFields.setVisible(true);
	        	}
	        	else
	        	{
	        		Editfield.setMouseHandling(VCommonGraphic.NO_MOUSEHANDLING);
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
	        		Editfield.setMouseHandling(VCommonGraphic.SHAPE_MODIFICATION_MOUSEHANDLING);
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
	        	Editfield.setMouseHandling(Editfield.getMouseHandling()); //ReInit Drag/Click
	        }

	}

	public void update(Observable o, Object arg) {
		if (arg instanceof GraphMessage) //All Other GraphUpdates are handled in VGRaphCommons
		{
			GraphMessage m = (GraphMessage) arg;
			if ((m.getModifiedElementTypes()==GraphConstraints.SELECTION)
				&&((m.getModification()&GraphConstraints.UPDATE)==GraphConstraints.UPDATE))
			{
				if  (cBasicShape.isVisible()&&((((String)cBasicShape.getSelectedItem()).equals("Kreis"))))
				{
					Vector<Object> params = Editfield.getShapeParameters();
					Point porig = (Point) params.get(NURBSShapeFactory.CIRCLE_ORIGIN);
					int size = Integer.parseInt(params.get(NURBSShapeFactory.CIRCLE_RADIUS).toString());
					if (porig==null)
						return;
				
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
			//Update Activity of Button
			bModeChange.setEnabled(!HGraphRef.modifyHyperEdges.get(HEdgeRefIndex).getShape().isEmpty());
		}	
	}
}
