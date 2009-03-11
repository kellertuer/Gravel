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
import model.VHyperGraph;
import model.Messages.GraphConstraints;
import model.Messages.GraphMessage;

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
public class CHyperEdgeShapeParameters implements CaretListener, ActionListener, Observer {

	private VHyperShapeGraphic Editfield;
	private Container cont;
	private IntegerTextField iDistance;
	private JComboBox cBasicShape;
	private JLabel Distance, BasicShape;
	
	private JLabel CircleOriginX, CircleOriginY, CircleRadius;
	private IntegerTextField iCOrigX, iCOrigY, iCRad;
	
	private Container CircleFields;
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
		Editfield = new VHyperShapeGraphic(d,vhg);
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
		Distance = new JLabel("<html><p>Mindestabstand<br><font size=\"-2\">Knoten \u2194 Umriss</font></p></html>");
		cont.add(Distance,c);
		c.gridx++;
		cont.add(iDistance,c);

		c.gridy++;
		c.gridx=2;
		String[] BasicShapes = { "<html>Auswahl</html>", "Kreis", "TODO"};
		cBasicShape = new JComboBox(BasicShapes);
		cBasicShape.setSelectedIndex(0);
		cBasicShape.setPreferredSize(new Dimension(100, 30));
		cBasicShape.addActionListener(this);
		BasicShape = new JLabel("<html><p>Grundform</p></html>");
		cont.add(BasicShape,c);
		c.gridx++;
		cont.add(cBasicShape,c);
		
		c.gridy++;
		c.gridx=2;
		c.gridwidth=2;
		c.insets = new Insets(30,7,0,7);
		buildCirclePanel();
		cont.add(CircleFields,c);
		CircleFields.setVisible(false);
		cont.validate();
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
		iCRad.setPreferredSize(new Dimension(100, 20));
		CircleRadius = new JLabel("<html><p>Radius</p></html>");
		CircleFields.add(CircleRadius,c);
		c.gridx++;
		CircleFields.add(iCRad,c);
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
				Editfield.setShapeParameters(param);
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

	}
	public void update(Observable o, Object arg) {
		if (arg instanceof GraphMessage) //All Other GraphUpdates are handled in VGRaphCommons
		{
			GraphMessage m = (GraphMessage) arg;
			if ((m.getModifiedElementTypes()==GraphConstraints.SELECTION)
				&&((m.getModification()&GraphConstraints.UPDATE)==GraphConstraints.UPDATE))
			{
				if  (((String)cBasicShape.getSelectedItem()).equals("Kreis"))
				{
				Vector<Object> params = Editfield.getShapeParameters();
				Point porig = (Point) params.get(NURBSShapeFactory.CIRCLE_ORIGIN);
				int size = Integer.parseInt(params.get(NURBSShapeFactory.CIRCLE_RADIUS).toString());
				if (porig==null)
						return;
				if (porig.x!=iCOrigX.getValue())
					iCOrigX.setValue(porig.x);
				if (porig.y!=iCOrigY.getValue())
					iCOrigY.setValue(porig.y);
				if (size!=iCRad.getValue())
					iCRad.setValue(size);
				}
			}
		}
		
	}
}
