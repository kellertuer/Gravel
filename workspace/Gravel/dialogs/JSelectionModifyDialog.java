package dialogs;

import io.GeneralPreferences;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import dialogs.components.*;

import view.Gui;
import model.*;
import model.Messages.GraphConstraints;
import model.Messages.GraphMessage;
/**
 * This class provides an UI for modifying all selected Nodes and Edges (if they exist)
 * 
 * @author ronny
 *
 */
public class JSelectionModifyDialog extends JDialog implements ActionListener, CaretListener 
{
	private static final long serialVersionUID = 1L;
	VSubgraphSet vsubs=null;
	VNodeSet vnodes=null;
	VEdgeSet vedges=null;
	VHyperEdgeSet vhyperedges=null;
	MSubgraphSet msubs=null;
	MNodeSet mnodes=null;
	MEdgeSet medges=null;
	MHyperEdgeSet mhyperedges=null;
	boolean directed = false;
	boolean show_position, show_nodeprop, show_edgeprop, show_subgraphs;
	VGraphInterface graphRef;
	JTabbedPane tabs;
	//
	//Erstes Tab  Positionierung
	//
	
	//Position Tab fields
	private JComboBox cPosition;
	private String[] Positionnames;
	//Translate selected
	private IntegerTextField iPosMoveX, iPosMoveY;
	//Arrange - General
	private JComboBox cOrderBy;
	//On A Circle
	private IntegerTextField iOriginX, iOriginY, iCircleRadius, iFirstNodeAtDegree;
	//
	private Container[] Positioncontent;
	
	//
	//Zweites Tab - Knoteneigenschaften
	//
	private JLabel NodePreview, NodeName, NodeSize;
	private IntegerTextField iNodeSize;
	private JTextField sNodeName;
	//Aktivierungsbuttons
	private JCheckBox bChNodeSize, bChNodeName;
	private CNodeNameParameters cNodeName;
	
	//
	//drittes Tab - Kanteneigenschaften - auch: Hyperkanteneigenschaften, weil es nur einen Wert gibt der neu ist
	//
	private JLabel EdgeName, EdgeWidth, EdgeValue, EdgePreview, HyperedgeMargin;
	private JTextField sEdgeName;
	private IntegerTextField iEdgeWidth, iEdgeValue, iHyperedgeMargin;
	private JCheckBox bChEdgeName, bChEdgeWidth, bChEdgeValue, bChHyperedgeMargin;
	private JTabbedPane EdgeTabs;
	
	//SubTabs - (1) Text
	private CEdgeTextParameters cText;
	//Subtabs - (2) Line
	private CEdgeLineParameters cLine;	
	//Subtabs - (3) Arrow
	private CEdgeArrowParameters cArrow;
	//Subtabs - (4) Schleife
	private CLoopParameters cLoop;
	
	//viertes Tab - Untergraphen
	@SuppressWarnings("unused")
	private JLabel lSubgraph;
	private Vector<String> subgraphnames;
	private JCheckBox[] bSubgraph;
	private JScrollPane iSubgraph;
	private JCheckBox bChSubgraph;
	
	
	private JButton bCancel, bOk;
	/**
	 * Short Init for the Editing of all selected Elements of the VGraph
	 * @param vhg hypergraph where all selected values should be edited
	 */
	public JSelectionModifyDialog(VGraph vg)
	{
		this(false, true, "Auswahl bearbeiten", vg);
	}
	/**
	 * Short Init for the Editing of all selected Elements of the VHyperGraph
	 * @param vhg hypergraph where all selected values should be edited
	 */
	public JSelectionModifyDialog(VHyperGraph vhg)
	{
		this(false, true, "Auswahl bearbeiten", vhg);
	}
	/**
	 * Initializes the Dialog and shows all possible tabs
	 * 
	 * @param arrangeknots 
	 * 			set true if the arrangement of knots should be displayed
	 * @param editValues
	 * 			set true if the values of selected Elements should be edited (including all subgraphs)
	 * @param vg the corresponding VGraph
	 */
	public JSelectionModifyDialog(boolean arrangeknots, boolean editValues, String WindowTitle, VGraph vg)
	{
		vsubs = vg.modifySubgraphs;
		vnodes = vg.modifyNodes;
		vedges = vg.modifyEdges;
		msubs = vg.getMathGraph().modifySubgraphs;
		mnodes = vg.getMathGraph().modifyNodes;
		medges = vg.getMathGraph().modifyEdges;
		directed = vg.getMathGraph().isDirected();
		graphRef = vg;
		InitDialog(arrangeknots, editValues, editValues, WindowTitle);
	}
	/**
	 * Initializes the Dialog and shows all possible tabs
	 * 
	 * @param arrangeknots 
	 * 			set true if the arrangement of knots should be displayed
	 * @param editValues
	 * 			set true if the values of selected Elements should be edited (including all subgraphs)
	 * @param vhg the corresponding VHyperGraph
	 */
	public JSelectionModifyDialog(boolean arrangeknots, boolean editValues, String WindowTitle, VHyperGraph vhg)
	{
		vsubs = vhg.modifySubgraphs;
		vnodes = vhg.modifyNodes;
		vhyperedges = vhg.modifyHyperEdges;
		msubs = vhg.getMathGraph().modifySubgraphs;
		mnodes = vhg.getMathGraph().modifyNodes;
		mhyperedges = vhg.getMathGraph().modifyHyperEdges;
		graphRef = vhg;
		InitDialog(arrangeknots, editValues, editValues, WindowTitle);
	}

	/**
	 * Initializes the Dialog with the possibility to disable some tabs
	 * and specify a title for the window
	 * 
	 * @param translate
	 * @param properties
	 * @param subgraphs
	 * @param title
	 * @param graph
	 */
	private void InitDialog(boolean translate, boolean properties, boolean subgraphs, String title)
	{
		//If selected nodes exist and the tab should be shown
		show_position = translate & vnodes.hasSelection();
		//If selected nodes exist and the porperties should be shown
		show_nodeprop = properties & vnodes.hasSelection();
		boolean edgeselection;
		if (vedges!=null)
			edgeselection = vedges.hasSelection();
		else if (vhyperedges!=null)
			edgeselection = vhyperedges.hasSelection();
		else
			edgeselection = false;
		show_edgeprop = properties & edgeselection;
		show_subgraphs = subgraphs & (edgeselection || vnodes.hasSelection())&&(msubs.cardinality() > 0);
		//None of the tabs should be shown, that would be quite wrong
		setTitle(title);
		tabs = new JTabbedPane();
		if (show_position)
			tabs.addTab("Position",buildPositionTab());
		if (show_nodeprop)
			tabs.addTab("Knoten",buildNodePropContent());
		String edgetabname="";
		if (vedges!=null)
			edgetabname = "Kanten";
		else if (vhyperedges!=null)
			edgetabname = "Hyperkanten";
		if (show_edgeprop)
			tabs.addTab(edgetabname,buildEdgePropContent());
		if (show_subgraphs)
			tabs.addTab("Untergraphen",buildSubgraphContent());
		
		Container ContentPane = this.getContentPane();
		ContentPane.removeAll();
		ContentPane.setLayout(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(7,7,7,7);
		c.anchor = GridBagConstraints.WEST;
	
		c.gridy=0;c.gridx=0;c.gridwidth=2;
		if (tabs.getTabCount()==0) //then all bool values were false, this should not happen...but who knows...
		{
			//in the normal cse, this should not happen
			//because the menu does only activate the item for this Dialog if there is anything selected, but ... to be sure...
			JOptionPane.showMessageDialog(this, "<html><p>Es wurden im Graphen weder Kanten noch Knoten selektiert.<br>Dadurch k"+main.CONST.html_oe+"nnen auch keine Ver"+main.CONST.html_ae+"nderungen vorgenommen werden.</p></html>","Initialisierungsfehler",JOptionPane.ERROR_MESSAGE);
			this.dispose();
			return;
		}
		else if (tabs.getTabCount() == 1)
		{
			ContentPane.add(tabs.getComponent(0),c);
		}
		else
		{
			ContentPane.add(tabs,c);	
		}		
		c.gridy++;c.gridx = 0;c.gridwidth=1;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(3,3,3,3);

		bCancel = new JButton("Abbrechen");
		bCancel.addActionListener(this);
		ContentPane.add(bCancel,c);
		//Add ESC-Handling
		InputMap iMap = getRootPane().getInputMap(	 JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape");

		ActionMap aMap = getRootPane().getActionMap();
		aMap.put("escape", new AbstractAction()
			{
				private static final long serialVersionUID = 1L;
				public void actionPerformed(ActionEvent e)
				{
					dispose();
				}
		 	});

		c.gridx = 1;
		c.anchor = GridBagConstraints.EAST;
		bOk = new JButton("Ok");
		bOk.addActionListener(this);
		ContentPane.add(bOk,c);
		
		this.getRootPane().setDefaultButton(bOk);
		setResizable(false);
		this.setModal(true);
		pack();
		if (show_position)
		{
			cPosition.setSelectedItem(Positionnames[0]);
			Dimension maxsize = new Dimension(0,0);
			for (int i=0; i<Positioncontent.length; i++)
			{
				int h = Positioncontent[i].getHeight();
				if (h > maxsize.height)
					maxsize.height = h;
				int b = Positioncontent[i].getWidth();
				if (b > maxsize.width)
					maxsize.width = b;
			}
			for (int i=0; i<Positioncontent.length; i++)
			{
				Positioncontent[i].setPreferredSize(maxsize);
			}
		}
		Point p = new Point(0,0);
		p.y += Math.round(Gui.getInstance().getParentWindow().getHeight()/2);
		p.x += Math.round(Gui.getInstance().getParentWindow().getWidth()/2);
		p.y -= Math.round(getHeight()/2);
		p.x -= Math.round(getWidth()/2);

		if (show_nodeprop)
			fillCommonNodeValues();
		if (show_edgeprop)
			fillCommonEdgeValues();
		if (show_subgraphs)
			fillCommonSubgraphValues();

		setLocation(p.x,p.y);
		this.setVisible(true);
	}
	/**
	 * building the JPanel with Elements to Rearrange the nodes
	 * 
	 * @return the constructed JPanel
	 */
	private JPanel buildPositionTab()
	{
		JPanel content = new JPanel();
		content.setLayout(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(7,7,7,7);
		c.anchor = GridBagConstraints.WEST;
		c.gridy = 0;
		c.gridx = 0;
		c.gridwidth = 2;
		content.add(new JLabel("Verändern der Positionen der selektierten Knoten"),c);
	
		c.gridy++;
		c.gridwidth = 1;
		
		c.gridx++;
		String[] name = {"keine Veränderung","Verschieben","Im Kreis anordnen"};
		Positionnames = name;
		cPosition = new JComboBox(name);
		cPosition.addActionListener(this);
		content.add(cPosition,c);
		
		c.gridy++;
		c.gridx=0;
		c.gridwidth=2;
		Positioncontent = new Container[name.length];
		Positioncontent[0] = new Container();
		//Positioncontent[0].add(new JLabel(""))
		Positioncontent[1] = buildPosTranslateContent(); //Position - Verschieben
		Positioncontent[2] = buildPosArrangeCircle(); //Position - Verschieben
		for (int i=0; i<Positioncontent.length; i++)
		{
			Positioncontent[i].validate();
			Positioncontent[i].setIgnoreRepaint(true);
			content.add(Positioncontent[i],c);
		}

		c.gridy++;
		c.gridx=0;
		return content;
	}
	/**
	 * Build the Elements for Translation of nodes, that is shown if the user selects "Translate Nodes"
	 * @return the Container with these Elements
	 */
	private Container buildPosTranslateContent()
	{
		Container PosContent = new Container();
		PosContent.setLayout(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(7,7,7,7);
		c.anchor = GridBagConstraints.WEST;
		c.gridy = 0;
		c.gridx = 0;
		c.gridwidth=1;
		PosContent.add(new JLabel("Verschiebung in X-Richtung"),c);
		iPosMoveX = new IntegerTextField();
		iPosMoveX.setMinValue(Integer.MIN_VALUE+1);
		iPosMoveX.setPreferredSize(new Dimension(150,20));
		
		c.gridx++;
		PosContent.add(iPosMoveX,c);
		
		c.gridy++;
		c.gridx=0;
		PosContent.add(new JLabel("Verschiebung in Y-Richtung"),c);
		iPosMoveY = new IntegerTextField();
		iPosMoveY.setPreferredSize(new Dimension(150,20));
		iPosMoveY.setMinValue(Integer.MIN_VALUE+1);
		c.gridx++;
		PosContent.add(iPosMoveY,c);
		
		return PosContent;
	}
	/**
	 * Build the Container with the Elements for Arranging the nodes in a Circle
	 * @return Container with the Elements to specify the Circle 
	 */
	private Container buildPosArrangeCircle()
	{
		Container PosContent = new Container();
		PosContent.setLayout(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(7,7,7,7);
		c.anchor = GridBagConstraints.WEST;
		c.gridy = 0;
		c.gridx = 0;
		c.gridwidth=1;
		
		PosContent.add(new JLabel("Sortiere Knoten nach"),c);
		c.gridx++;
		String[] ordernames = {"ID","Name"};
		cOrderBy = new JComboBox(ordernames);
		cOrderBy.setSelectedIndex(0);
		cOrderBy.setEnabled(false); //TODO Selection-Modify - Order by Nodename and so on einbauen
		PosContent.add(cOrderBy,c);
		
		c.gridy++;
		c.gridx=0;
		PosContent.add(new JLabel("Kreismittelpunkt, X"),c);
		iOriginX = new IntegerTextField();
		iOriginX.setPreferredSize(new Dimension(150,20));
		c.gridx++;
		PosContent.add(iOriginX,c);
		
		c.gridy++;
		c.gridx=0;
		PosContent.add(new JLabel("Kreismittelpunkt, Y"),c);
		iOriginY = new IntegerTextField();
		iOriginY.setPreferredSize(new Dimension(150,20));
		c.gridx++;
		PosContent.add(iOriginY,c);
		
		c.gridy++;
		c.gridx=0;
		PosContent.add(new JLabel("Kreisradius"),c);
		iCircleRadius = new IntegerTextField();
		iCircleRadius.setPreferredSize(new Dimension(150,20));
		c.gridx++;
		PosContent.add(iCircleRadius,c);
		
		c.gridy++;
		c.gridx=0;
		PosContent.add(new JLabel("Ersten Knoten bei (Grad)"),c);
		iFirstNodeAtDegree = new IntegerTextField();
		iFirstNodeAtDegree.setPreferredSize(new Dimension(150,20));
		c.gridx++;
		PosContent.add(iFirstNodeAtDegree,c);
		c.gridy++; c.gridx=0; c.gridwidth=2; PosContent.add(new JLabel("<html><font size=-1>I(0=rechts, 90=oben, 180=links, 270=unten)</font></html>"),c);

		return PosContent;
	}
	/**
	 * Build and 
	 * @return the Container for Modifying Node Properties
	 */
	private Container buildNodePropContent()
	{
		Container NodeContent = new Container();
		NodeContent.setLayout(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(7,7,7,7);
		c.anchor = GridBagConstraints.WEST;
		c.gridy = 0;
		c.gridx = 0;
		c.gridwidth=1;
		bChNodeName = new JCheckBox();
		NodeContent.add(bChNodeName, c);
		bChNodeName.addActionListener(this);
		c.gridx++;
		NodeName = new JLabel("Name");
		NodeContent.add(NodeName,c);
		c.gridx++;
		sNodeName = new JTextField();
		sNodeName.setPreferredSize(new Dimension(200, 20));
		sNodeName.addCaretListener(this);
		NodeContent.add(sNodeName,c);		
		//Vorschau
		NodePreview = new JLabel();
		c.insets = new Insets(0,14,7,0);	c.gridy++;c.gridx = 2;
		NodeContent.add(NodePreview,c);
		NodePreview.setPreferredSize(new Dimension(150, 20));
		
		c.gridy++;
		c.gridx=0; 		c.insets = new Insets(7,7,7,7);
		bChNodeSize = new JCheckBox();
		NodeContent.add(bChNodeSize, c);
		bChNodeSize.addActionListener(this);
		c.gridx++;
		NodeSize = new JLabel("<html><p>Gr"+main.CONST.html_oe+main.CONST.html_sz+"e</p></html>");
		NodeContent.add(NodeSize,c);
		c.gridx++;
		iNodeSize = new IntegerTextField();
		iNodeSize.setPreferredSize(new Dimension(200, 20));
		NodeContent.add(iNodeSize,c);		
		
		c.gridy++; c.gridx=0; c.gridwidth=3;
		cNodeName = new CNodeNameParameters(null,true);
		NodeContent.add(cNodeName,c);
		return NodeContent;

	}	
	/**
	 * Find common Values of all selected nodes, and set the activation of the textfields in the Properties-Container
	 *
	 */
	private void fillCommonNodeValues()
	{
		//Werte suchen fuer die Initialisierung/Gemeinsame werte der Auswahl
		Iterator<VNode> nodeiter = vnodes.getIterator();
		int preNodeSize=-1, preNodeTextSize=-1, preNodeTextDis=-1, preNodeTextRot=-1;
		//ShowText ist der wert und given sagt, ob der allgemeingültig ist
		boolean preNodeShowText=false, preNodeShowTextgiven=true,beginning = true;
		String nodename=null;
		while (nodeiter.hasNext())
		{
			VNode temp = nodeiter.next();
			if ((temp.getSelectedStatus() & VItem.SELECTED) == VItem.SELECTED)
			{
				if (beginning)
				{
					beginning = false;
					preNodeShowText = temp.isNameVisible();
					preNodeShowTextgiven = true;
					preNodeSize = temp.getSize();
					preNodeTextSize = temp.getNameSize();
					preNodeTextDis = temp.getNameDistance();
					preNodeTextRot = temp.getNameRotation();
					nodename = mnodes.get(temp.getIndex()).name;
					//Replace the id numer by $ID (at least try to get a common name
					nodename = GeneralPreferences.replace(nodename,""+temp.getIndex(), "$ID");
				}
				else
				{ // if a node differs set common name to -1
					if (temp.isNameVisible()!=preNodeShowText)
						preNodeShowTextgiven = false;
					if (temp.getSize()!=preNodeSize)
						preNodeSize = -1;
					if (temp.getNameSize()!=preNodeTextSize)
						preNodeTextSize=-1;
					if (temp.getNameDistance()!=preNodeTextDis)
						preNodeTextDis=-1;
					if (temp.getNameRotation()!=preNodeTextRot)
						preNodeTextRot=-1;
					if (nodename!=null)
					{
						if (!GeneralPreferences.replace(nodename,"$ID",""+temp.getIndex()).equals(mnodes.get(temp.getIndex()).name))
							nodename = null;
					}	
				}	
			} //end is selected
		} //End while
		//Werte - fill name
	    if (nodename!=null)
		{
	    	sNodeName.setText(nodename);
			NodePreview.setText("<html><p>Vorschau :<i>"+GeneralPreferences.replace(nodename,"$ID","4")+"</i>");
		}
	    else
	    {
	    	NodePreview.setText("<html><p>Vorschau : </p></html>"); NodePreview.setForeground(Color.GRAY);
	    	NodeName.setForeground(Color.GRAY);
	    }
			bChNodeName.setSelected(nodename!=null);sNodeName.setEnabled(nodename!=null);

		//fill Node Size
		if (preNodeSize!=-1)
			iNodeSize.setValue(preNodeSize);
		else
			NodeSize.setForeground(Color.GRAY);
		bChNodeSize.setSelected(preNodeSize!=-1);
		iNodeSize.setEnabled(preNodeSize!=-1);
		//fill ShowNodeName
		VNode v = new VNode(0,0,0,0,preNodeTextDis,preNodeTextRot,preNodeTextSize,preNodeShowText);
		cNodeName.InitValues(v, preNodeShowTextgiven);
	}
	//
	//
	//Kanten Eigenschaften Tab
	//
	//
	/**
	 * Build the Tabulator with the Edge Values, and all its Textfields and Sliders for modifying them
	 * @return the Container with the Tab
	 */
	private Container buildEdgePropContent()
	{
		Container EdgeContent = new Container();
		EdgeContent.setLayout(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(7,7,7,7);
		c.anchor = GridBagConstraints.WEST;
		c.gridy = 0;
		c.gridx = 0;
		c.gridwidth=1;
		bChEdgeName = new JCheckBox();
		EdgeContent.add(bChEdgeName, c);
		bChEdgeName.addActionListener(this);
		c.gridx++;
		EdgeName = new JLabel("Name");
		EdgeContent.add(EdgeName,c);
		c.gridx++;
		sEdgeName = new JTextField();
		sEdgeName.setPreferredSize(new Dimension(200, 20));
		sEdgeName.addCaretListener(this);
		EdgeContent.add(sEdgeName,c);		
		//Vorschau
		EdgePreview = new JLabel();
		c.insets = new Insets(0,14,7,0);	c.gridy++;c.gridx = 2;
		EdgeContent.add(EdgePreview,c);
		EdgePreview.setPreferredSize(new Dimension(150, 20));
		c.gridy++;
		c.gridx=0; 		c.insets = new Insets(7,7,7,7);

		bChEdgeValue = new JCheckBox();
		EdgeContent.add(bChEdgeValue,c);
		bChEdgeValue.addActionListener(this);
		c.gridx++;
		EdgeValue = new JLabel("Gewicht");
		EdgeContent.add(EdgeValue,c);
		c.gridx++;
		iEdgeValue = new IntegerTextField();
		iEdgeValue.setPreferredSize(new Dimension(200, 20));
		iEdgeValue.addCaretListener(this);
		EdgeContent.add(iEdgeValue,c);		
		
		c.gridy++;
		c.gridx=0;
		bChEdgeWidth = new JCheckBox();
		EdgeContent.add(bChEdgeWidth,c);
		bChEdgeWidth.addActionListener(this);
		c.gridx++;
		EdgeWidth = new JLabel("Breite");
		EdgeContent.add(EdgeWidth,c);
		c.gridx++;
		iEdgeWidth = new IntegerTextField();
		iEdgeWidth.setPreferredSize(new Dimension(200, 20));
		iEdgeWidth.addCaretListener(this);
		EdgeContent.add(iEdgeWidth,c);		

		if (vhyperedges!=null)
		{
			c.gridy++;
			c.gridx=0;
			bChHyperedgeMargin = new JCheckBox();
			EdgeContent.add(bChHyperedgeMargin,c);
			bChHyperedgeMargin.addActionListener(this);
			c.gridx++;
			HyperedgeMargin = new JLabel("Innanabstand");
			EdgeContent.add(HyperedgeMargin,c);
			c.gridx++;
			iHyperedgeMargin = new IntegerTextField();
			iHyperedgeMargin.setPreferredSize(new Dimension(200, 20));
			iHyperedgeMargin.addCaretListener(this);
			EdgeContent.add(iHyperedgeMargin,c);		
		}
		
		c.gridy++;
		c.gridx=0;
		c.gridwidth = 3;
		EdgeTabs = new JTabbedPane();
		cText = new CEdgeTextParameters(null,false,true); //not global but with checkboxes, Std-Values
		EdgeTabs.add("Beschriftung",cText.getContent());
		cLine = new CEdgeLineParameters(null,false,true); //not global, with CheckBoxes, Std-Values
		EdgeTabs.add("Linienart", cLine.getContent());
		if (vedges!=null)
		{
			if (directed) //only happens in graphs
			{
				cArrow = new CEdgeArrowParameters(null,true); //std values, with checks
				EdgeTabs.add("Pfeil", cArrow.getContent());
			}
			Iterator<VEdge> edgeiter =vedges.getIterator();
			boolean loops=false;
			while (edgeiter.hasNext())
			{
				VEdge e = edgeiter.next();
				loops |= (e.getEdgeType()==VEdge.LOOP)&&((e.getSelectedStatus() & VItem.SELECTED) == VItem.SELECTED); //Existiert eine selectierte Schleife ?
			}
			if (loops)
			{
				cLoop = new CLoopParameters(null,directed,true);
				EdgeTabs.add("Schleife",cLoop);
			}
		}
		EdgeContent.add(EdgeTabs,c);
		return EdgeContent;
	}

	/**
	 * Run through all selected Edges and find common values, that all edges have. Set the common values into the UI and disable all other fields
	 *
	 */
	private void fillCommonEdgeValues()
	{
		if (vedges!=null)
			fillEdgeValues();
		else if (vhyperedges!=null)
			fillHyperedgeValues();
	}
	private void fillEdgeValues()
	{
		if (vedges==null)
			return;
		//Werte suchen fuer die Initialisierung/Gemeinsame werte der Auswahl
		Iterator<VEdge> edgeiter = vedges.getIterator();
		VEdge pre = new VStraightLineEdge(0,0);
		int preEdgeValue=0;
		boolean preEdgeShowTextgiven=true,beginning = true, preEdgeTextShowValuegiven=true, preEdgeLineTypegiven=true;
		String preEdgeName=null;
		VLoopEdge vle = null;
		boolean clockwiseequal = true;
		while (edgeiter.hasNext())
		{
			VEdge temp = edgeiter.next();
			if (((temp.getSelectedStatus() & VItem.SELECTED) == VItem.SELECTED))
			{
				if (beginning)
				{
					beginning = false;
					pre = temp.clone();
					MEdge me = medges.get(temp.getIndex());
					preEdgeValue = me.Value;
					preEdgeName = me.name;
					//Replace the id numer by $ID (at least try to get a common name
					preEdgeName = GeneralPreferences.replace(preEdgeName,""+temp.getIndex(), "$ID");
					preEdgeName = GeneralPreferences.replace(preEdgeName,""+me.StartIndex, "$SID");
					preEdgeName = GeneralPreferences.replace(preEdgeName,""+me.EndIndex, "$EID");					
					if (pre.getEdgeType()==VEdge.LOOP) // very first is a Loop
					{
						vle = (VLoopEdge)pre.clone();
					}
				}
				else
				{ // if an edge value differs set common name to -1
					if (medges.get(temp.getIndex()).Value!=preEdgeValue)
						preEdgeValue=-1;
					if (pre.getWidth()!=temp.getWidth())
						pre.setWidth(-1);
					
					if (pre.getTextProperties().getDistance()!=temp.getTextProperties().getDistance())
						pre.getTextProperties().setDistance(-1);
					if (pre.getTextProperties().getPosition()!=temp.getTextProperties().getPosition())
						pre.getTextProperties().setPosition(-1);
					if (pre.getTextProperties().getSize()!=temp.getTextProperties().getSize())
						pre.getTextProperties().setSize(-1);
					if (pre.getTextProperties().isVisible()!=temp.getTextProperties().isVisible())
						preEdgeShowTextgiven=false;
					if (pre.getTextProperties().isshowvalue()!=temp.getTextProperties().isshowvalue())
						preEdgeTextShowValuegiven=false;
					
					if (pre.getLinestyle().getDistance()!=temp.getLinestyle().getDistance())
						pre.getLinestyle().setDistance(-1);
					if (pre.getLinestyle().getLength()!=temp.getLinestyle().getLength())
						pre.getLinestyle().setLength(-1);
					if (pre.getLinestyle().getType()!=temp.getLinestyle().getType())
						preEdgeLineTypegiven=false;
					
					if (pre.getArrow().getAngle()!=temp.getArrow().getAngle())
						pre.getArrow().setAngle(-1f);
					if (pre.getArrow().getPart()!=temp.getArrow().getPart())
						pre.getArrow().setPart(-1f);
					if (pre.getArrow().getPos()!=temp.getArrow().getPos())
						pre.getArrow().setPos(-1f);
					if (pre.getArrow().getSize()!=temp.getArrow().getSize())
						pre.getArrow().setSize(-1f);
					if (preEdgeName!=null)
					{
						String tname = GeneralPreferences.replace(preEdgeName,"$ID",""+temp.getIndex());
						MEdge me = medges.get(temp.getIndex());
						tname = GeneralPreferences.replace(tname,"$SID",""+me.StartIndex);
						tname = GeneralPreferences.replace(tname,"$EID",""+me.EndIndex);
						if (!tname.equals(medges.get(temp.getIndex()).name))
							preEdgeName = null;
					}
					if (temp.getEdgeType()==VEdge.LOOP)
					{
						if (vle==null)
						{
							vle = (VLoopEdge) temp.clone();
						}
						else
						{
							VLoopEdge tvle = (VLoopEdge) temp;
							if (vle.getLength()!=tvle.getLength())
								vle.setLength(-1);
							if (vle.getProportion()!=tvle.getProportion())
								vle.setProportion(-1.0d);
							if (vle.getDirection()!=tvle.getDirection())
								vle.setDirection(-1);
							if (vle.isClockwise()!=tvle.isClockwise())
								clockwiseequal=false;
						}
					}
				}	
			} //end is selected
		} //End while
		//Werte - fill name
		if (preEdgeValue!=-1)
			iEdgeValue.setValue(preEdgeValue);
		else
			EdgeValue.setForeground(Color.GRAY);
		bChEdgeValue.setSelected(preEdgeValue!=-1);iEdgeValue.setEnabled(preEdgeValue!=-1);
		
		if (pre.getWidth()!=-1)
			iEdgeWidth.setValue(pre.getWidth());
		else
			EdgeWidth.setForeground(Color.GRAY);
		bChEdgeWidth.setSelected(pre.getWidth()!=-1);iEdgeWidth.setEnabled(pre.getWidth()!=-1);
		
		cText.InitValues(pre.getTextProperties());
		cText.updateUI(preEdgeShowTextgiven, preEdgeTextShowValuegiven);

		cLine.InitValues(pre.getLinestyle());
		cLine.updateUI(preEdgeLineTypegiven);
		
		if (directed)
		{
			cArrow.InitValues(pre);
		}
		if (preEdgeName!=null)
		{
			sEdgeName.setText(preEdgeName);
			String t = sEdgeName.getText();
			t = GeneralPreferences.replace(t,"$ID","4");
			t = GeneralPreferences.replace(t,"$SID","1");
			t = GeneralPreferences.replace(t,"$EID","2");
			if (t.length() > 10)
				t = t.substring(0,10)+"...";
			EdgePreview.setText("<html><font size=-1>Vorschau: <i>"+t+"</i></font></html>");
			EdgePreview.validate();			
		}
		else
			EdgeName.setForeground(Color.GRAY);
		bChEdgeName.setSelected(preEdgeName!=null); sEdgeName.setEnabled(preEdgeName!=null);
		if (vle!=null) //Loop exists
			cLoop.InitValues(vle, clockwiseequal);
	}
	private void fillHyperedgeValues()
	{
		if (vhyperedges==null)
			return;
		//Werte suchen fuer die Initialisierung/Gemeinsame werte der Auswahl
		Iterator<VHyperEdge> hyperedgeiter = vhyperedges.getIterator();
		VHyperEdge pre = new VHyperEdge(0,0,0);
		int preEdgeValue=0;
		boolean preEdgeShowTextgiven=true,beginning = true, preEdgeTextShowValuegiven=true, preEdgeLineTypegiven=true;
		String preEdgeName=null;
		while (hyperedgeiter.hasNext())
		{
			VHyperEdge temp = hyperedgeiter.next();
			if (((temp.getSelectedStatus() & VItem.SELECTED) == VItem.SELECTED))
			{
				if (beginning)
				{
					beginning = false;
					pre = temp.clone();
					MHyperEdge me = mhyperedges.get(temp.getIndex());
					preEdgeValue = me.Value;
					preEdgeName = me.name;
					//Replace the id numer by $ID (at least try to get a common name
					preEdgeName = GeneralPreferences.replace(preEdgeName,""+temp.getIndex(), "$ID");
				}
				else
				{ // if an edge value differs set common name to -1
					if (mhyperedges.get(temp.getIndex()).Value!=preEdgeValue)
						preEdgeValue=-1;
					if (pre.getWidth()!=temp.getWidth())
						pre.setWidth(-1);
					if (pre.getMinimumMargin()!=temp.getMinimumMargin())
						pre.setMinimumMargin(-1);
					
					if (pre.getTextProperties().getDistance()!=temp.getTextProperties().getDistance())
						pre.getTextProperties().setDistance(-1);
					if (pre.getTextProperties().getPosition()!=temp.getTextProperties().getPosition())
						pre.getTextProperties().setPosition(-1);
					if (pre.getTextProperties().getSize()!=temp.getTextProperties().getSize())
						pre.getTextProperties().setSize(-1);
					if (pre.getTextProperties().isVisible()!=temp.getTextProperties().isVisible())
						preEdgeShowTextgiven=false;
					if (pre.getTextProperties().isshowvalue()!=temp.getTextProperties().isshowvalue())
						preEdgeTextShowValuegiven=false;
					
					if (pre.getLinestyle().getDistance()!=temp.getLinestyle().getDistance())
						pre.getLinestyle().setDistance(-1);
					if (pre.getLinestyle().getLength()!=temp.getLinestyle().getLength())
						pre.getLinestyle().setLength(-1);
					if (pre.getLinestyle().getType()!=temp.getLinestyle().getType())
						preEdgeLineTypegiven=false;
					if (preEdgeName!=null)
					{
						String tname = GeneralPreferences.replace(preEdgeName,"$ID",""+temp.getIndex());
						if (!tname.equals(mhyperedges.get(temp.getIndex()).name))
							preEdgeName = null;
					}
				}	
			} //end is selected
		} //End while
		//Werte - fill name
		if (preEdgeValue!=-1)
			iEdgeValue.setValue(preEdgeValue);
		else
			EdgeValue.setForeground(Color.GRAY);
		bChEdgeValue.setSelected(preEdgeValue!=-1);iEdgeValue.setEnabled(preEdgeValue!=-1);
		
		if (pre.getWidth()!=-1)
			iEdgeWidth.setValue(pre.getWidth());
		else
			EdgeWidth.setForeground(Color.GRAY);
		bChEdgeWidth.setSelected(pre.getWidth()!=-1);iEdgeWidth.setEnabled(pre.getWidth()!=-1);

		if (pre.getWidth()!=-1)
			iHyperedgeMargin.setValue(pre.getMinimumMargin());
		else
			HyperedgeMargin.setForeground(Color.GRAY);
		bChHyperedgeMargin.setSelected(pre.getMinimumMargin()!=-1);iHyperedgeMargin.setEnabled(pre.getMinimumMargin()!=-1);
		
		cText.InitValues(pre.getTextProperties());
		cText.updateUI(preEdgeShowTextgiven, preEdgeTextShowValuegiven);

		cLine.InitValues(pre.getLinestyle());
		cLine.updateUI(preEdgeLineTypegiven);
		
		if (preEdgeName!=null)
		{
			sEdgeName.setText(preEdgeName);
			String t = sEdgeName.getText();
			t = GeneralPreferences.replace(t,"$ID","4");
			if (t.length() > 10)
				t = t.substring(0,10)+"...";
			EdgePreview.setText("<html><font size=-1>Vorschau: <i>"+t+"</i></font></html>");
			EdgePreview.validate();			
		}
		else
			EdgeName.setForeground(Color.GRAY);
		bChEdgeName.setSelected(preEdgeName!=null); sEdgeName.setEnabled(preEdgeName!=null);
	}
	//
	//
	// Subgraph Tab
	//
	//
	/**
	 * Build the Tab for the Subgraphs
	 * @return the subgraph Container
	 */
	private Container buildSubgraphContent()
	{
		Container SubgrephContent = new Container();
		SubgrephContent.setLayout(new GridBagLayout());
		Container SubgraphList = new Container();
		SubgraphList.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(0,0,0,0);
		c.anchor = GridBagConstraints.CENTER;
		c.gridy = 0;
		c.gridx = 0;
		bChSubgraph = new JCheckBox("<html><p>Auswahl den Untergraphen zuprdnen:</p></html>");
		bChSubgraph.addActionListener(this);
		SubgrephContent.add(bChSubgraph,c);
		subgraphnames = msubs.getNames();
		int temp = 0;
		for (int i=0; i<subgraphnames.size(); i++)
		{
			if (subgraphnames.elementAt(i)!=null) //Ein Knoten mit dem Index existiert
			temp ++; //Anzahl Untergraphen zaehlen
		}
		this.bSubgraph = new JCheckBox[temp];
		temp = 0;
		for (int i=0; i<subgraphnames.size(); i++)
		{
			if (subgraphnames.elementAt(i)!=null) //Ein Knoten mit dem Index existiert
			{
				bSubgraph[temp] = new JCheckBox(msubs.get(i).getName());
				SubgraphList.add(bSubgraph[temp],c);
				c.gridy++;
				temp++; //Anzahl Knoten zaehlen
			}
		}
		c.gridy = 1;
		iSubgraph = new JScrollPane(SubgraphList);
		iSubgraph.setAlignmentX(JScrollPane.LEFT_ALIGNMENT);
		iSubgraph.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		iSubgraph.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		iSubgraph.setPreferredSize(new Dimension(200,150));
		SubgrephContent.add(iSubgraph,c);
		return SubgrephContent;
	}
	private void fillCommonSubgraphValues()
	{
		boolean preSubgraphsequal = true;
		Vector<Boolean> subgraphs = new Vector<Boolean>(msubs.getNames().size());
		subgraphs.setSize(msubs.getNames().size());
		Iterator<VNode> nodeiter = vnodes.getIterator();
		boolean beginning = true;
		while (nodeiter.hasNext())
		{
			VNode actual = nodeiter.next();
			if ((actual.getSelectedStatus() & VItem.SELECTED) == VItem.SELECTED)
			{
				Iterator<VSubgraph> siter = vsubs.getIterator();
				while (siter.hasNext())
				{
					VSubgraph s = siter.next();
					if (beginning) //Set the Subgraph the first node belongs to...
						subgraphs.set(s.getIndex(), new Boolean(msubs.get(s.getIndex()).containsNode(actual.getIndex())));
					else
					{
						if (subgraphs.get(s.getIndex()).booleanValue()!=msubs.get(s.getIndex()).containsNode(actual.getIndex()))
							preSubgraphsequal = false;
					}
				}
				beginning = false;
			}
		}
		Iterator<VEdge> edgeiter = null; 
		if (vedges!=null)
			edgeiter = vedges.getIterator();
		Iterator<VHyperEdge> hyperedgeiter = null; 
		if (vhyperedges!=null)
			hyperedgeiter = vhyperedges.getIterator();
		while ( ( (edgeiter!=null)&&(edgeiter.hasNext()) ) || ( (hyperedgeiter!=null)&&(hyperedgeiter.hasNext()) ) )
		{
			VItem actual;
			if (edgeiter!=null)
				actual = edgeiter.next();
			else if (hyperedgeiter!=null)
				actual = hyperedgeiter.next();
			else
				break;
			if ((actual.getSelectedStatus() & VItem.SELECTED) == VItem.SELECTED)
			{
				Iterator<VSubgraph> siter = vsubs.getIterator();
				while (siter.hasNext())
				{
					VSubgraph s = siter.next();
					if (beginning) //Set the Subgraphs the first edge belongs to (happens if no node selected) belongs to...
						subgraphs.set(s.getIndex(), new Boolean(msubs.get(s.getIndex()).containsEdge(actual.getIndex())));
					else
					{
						if (subgraphs.get(s.getIndex()).booleanValue()!=msubs.get(s.getIndex()).containsEdge(actual.getIndex()))
							preSubgraphsequal = false;
					}
				}
				beginning = false;
			}
		}
		this.bChSubgraph.setSelected(preSubgraphsequal);
		if (preSubgraphsequal) //All selected are in the same subgraphs
		{
			int position = 0;
			for (int i=0; i<subgraphs.size(); i++)
			{
				if (subgraphs.get(i)!=null)
				{
					this.bSubgraph[position].setSelected(subgraphs.get(i).booleanValue());
					position++;
				}
			}
		}
		else
		{
			bChSubgraph.setForeground(Color.GRAY);
			iSubgraph.setEnabled(false);
			for (int i=0; i<bSubgraph.length; i++)
			{
				bSubgraph[i].setEnabled(false);
			}
		}
	}
	//
	//Modify Graph
	//
	/**
	 * If a node is Translated, this method moves its adjacent edges (controllpoints) with half the movement
	 */
	private void translateIncidentEdges(int nodeindex, int x, int y)
	{
		if (vedges!=null)
		{
			x = Math.round(x/2);
			y = Math.round(y/2);
			Iterator<VEdge> edgeiter = vedges.getIterator();
			while (edgeiter.hasNext())
			{
				VEdge e = edgeiter.next();
				MEdge me = medges.get(e.getIndex());
				if ((me.StartIndex==nodeindex)||(me.EndIndex==nodeindex))
						e.translate(x, y);
			}
		}
		else if (vhyperedges !=null) //Move all incident hyperedges same amount 
		{
			Iterator<VHyperEdge> edgeiter = vhyperedges.getIterator();
			while (edgeiter.hasNext())
			{
				VHyperEdge e = edgeiter.next();
				if (mhyperedges.get(e.getIndex()).isincidentTo(nodeindex))
						e.translate(x, y);
			}
		}
	}
	/**
	 * Translates all Selected Node by the Values given through the UI
	 *
	 */
	private void translate()
	{
		Iterator<VNode> nodeiter = vnodes.getIterator();
		while (nodeiter.hasNext())
		{
			VNode t = nodeiter.next();
			if ((t.getSelectedStatus() & VItem.SELECTED) == VItem.SELECTED)
			{
					Point newpoint = t.getPosition(); //bisherige Knotenposition
					newpoint.translate(iPosMoveX.getValue(),iPosMoveY.getValue()); //Bewegung im Graphen aber mit Rungungsfehlern, also nur zurbetrachtung der Gesamtgraphbewegung
					if (newpoint.x < 0)
					{
						graphRef.translate(Math.abs(newpoint.x), 0); //Um die Differenz verschieben (Zoomfactor aufheben)
						newpoint.x=0;
					}
					if (newpoint.y < 0)
					{
						graphRef.translate(0,Math.abs(newpoint.y));
						newpoint.y = 0;
					}
					t.setPosition(newpoint); //Translate selected node
					//move Adjacent Edges
					translateIncidentEdges(t.getIndex(),iPosMoveX.getValue(),iPosMoveY.getValue());
			}
		}
	}
	/**
	 * Arranges the selected Nodes in a Circle and sorts them befor...(Planned)
	 * Up to now they are sorted by ID
	 *
	 */
	private void arrangeCircle()
	{
		//Verschieben noetig ?
		int x=0,y=0;
		if (iOriginX.getValue() - iCircleRadius.getValue() < 0)
			x = -(iOriginX.getValue() - iCircleRadius.getValue());
		if (iOriginY.getValue() - iCircleRadius.getValue() < 0)
			y = -(iOriginY.getValue() - iCircleRadius.getValue());
		//Graph verschieben
		graphRef.translate(x, y);
		//Kreismittelpunkt auch verschieben
		iOriginX.setValue(iOriginX.getValue()+x);
		iOriginY.setValue(iOriginY.getValue()+y);

		//Knoten zaehlen
		int nodecount = 0;
		Iterator<VNode> nodeiter = vnodes.getIterator();
		while (nodeiter.hasNext())
		{
			if ((nodeiter.next().getSelectedStatus() & VItem.SELECTED) == VItem.SELECTED)
				nodecount++;				
		}
		
		//loslegen
		double start = - (new Integer(iFirstNodeAtDegree.getValue())).doubleValue();
		double part = (new Integer(360)).doubleValue()/(new Integer(nodecount)).doubleValue();
		
		int mx = iOriginX.getValue();
		int my = iOriginY.getValue();
		int mr = iCircleRadius.getValue();
		double actualdeg = start;
		nodeiter = vnodes.getIterator();
		while (nodeiter.hasNext()) 
		{
			VNode temp = nodeiter.next();
			if (((temp.getSelectedStatus() & VItem.SELECTED) == VItem.SELECTED))
			{
				double posx = (new Integer(mr)).doubleValue()*Math.cos(2*Math.PI*actualdeg/360d);
				double posy = (new Integer(mr)).doubleValue()*Math.sin(2*Math.PI*actualdeg/360d);
						
				Point newpos = new Point(mx+Math.round((new Double(posx)).floatValue()),my+Math.round((new Double(posy)).floatValue()));

				int diffx = temp.getPosition().x - newpos.x;
				int diffy = temp.getPosition().y - newpos.y;
				temp.setPosition(newpos);	
				//move adjacent edges also
				translateIncidentEdges(temp.getIndex(), diffx, diffy);
				actualdeg += part % 360;
			}
		}		
	}
	/**
	 * Modify Selected Ndes and set all selected Values (checked Values) in each node
	 *
	 */
	private boolean modifyNodes()
	{ 
		boolean changed = false;
		//Set all nodes to the selected values, if they are selected
		Iterator<VNode> nodeiter = vnodes.getIterator();
		while (nodeiter.hasNext())
		{
			VNode actual = nodeiter.next();
			if ((actual.getSelectedStatus() & VItem.SELECTED) == VItem.SELECTED)
			{
				//Node Name
				if (bChNodeName.isSelected())
				{
					mnodes.get(actual.getIndex()).name = GeneralPreferences.replace(sNodeName.getText(), "$ID",""+actual.getIndex());
					changed = true;
				}
				//Node Size
				if (bChNodeSize.isSelected())
				{
					actual.setSize(iNodeSize.getValue());
					changed = true;
				}
				actual = cNodeName.modifyNode(actual);
				changed = true;
			} //end if selected
		} //end while
		return changed;
	}
	/**
	 * Modify selected Edges - set all checked Values in each selected Edge
	 *
	 */
	private boolean modifyEdges()
	{
		if (vedges==null)
			return false;
		boolean changed = false;
		Iterator<VEdge> edgeiter = vedges.getIterator();
		while (edgeiter.hasNext())
		{
			VEdge actual = edgeiter.next();
			if ((actual.getSelectedStatus() & VItem.SELECTED) == VItem.SELECTED)
			{
				//stupidly apply all values that are selected to do so
				if (bChEdgeName.isSelected())
				{
					String t = sEdgeName.getText();
					t = GeneralPreferences.replace(t,"$ID",actual.getIndex()+"");
					t = GeneralPreferences.replace(t,"$SID",medges.get(actual.getIndex()).StartIndex+"");
					t = GeneralPreferences.replace(t,"$EID",medges.get(actual.getIndex()).EndIndex+"");
					medges.get(actual.getIndex()).name= t;
					changed=true;
				}
				if (bChEdgeValue.isSelected())
				{
					MEdge e = medges.get(actual.getIndex());
					e.Value = iEdgeValue.getValue();
					changed=true;
				}
				if (bChEdgeWidth.isSelected())
				{
					actual.setWidth(iEdgeWidth.getValue());
					changed=true;
				}
				actual = cText.modifyEdge(actual);
				actual = cLine.modifyEdge(actual);
				
				if (directed)
				{
					actual = cArrow.modifyEdge(actual);
					changed=true;
				}
				if (cLoop!=null) //Loops exist
				{
					actual = cLoop.modifyEdge(actual); //Modify Loops
					changed=true;
				}
			} // fi selected
		} //end while (Edge Iteration
		return changed;
	} //end modify Edges
	/**
	 * Modify selected HyperEdges - set all checked Values in each selected Edge
	 *
	 */
	private boolean modifyHyperEdges()
	{
		if (vhyperedges==null)
			return false;
		boolean changed = false;
		Iterator<VHyperEdge> edgeiter = vhyperedges.getIterator();
		while (edgeiter.hasNext())
		{
			VHyperEdge actual = edgeiter.next();
			if ((actual.getSelectedStatus() & VItem.SELECTED) == VItem.SELECTED)
			{
				//stupidly apply all values that are selected to do so
				if (bChEdgeName.isSelected())
				{
					String t = sEdgeName.getText();
					t = GeneralPreferences.replace(t,"$ID",actual.getIndex()+"");
					mhyperedges.get(actual.getIndex()).name= t;
					changed=true;
				}
				if (bChEdgeValue.isSelected())
				{
					MHyperEdge e = mhyperedges.get(actual.getIndex());
					e.Value = iEdgeValue.getValue();
					changed=true;
				}
				if (bChEdgeWidth.isSelected())
				{
					actual.setWidth(iEdgeWidth.getValue());
					changed=true;
				}
				if (bChHyperedgeMargin.isSelected())
				{
					actual.setMinimumMargin(iHyperedgeMargin.getValue());
					changed=true;
				}
				actual = cText.modifyHyperEdge(actual);
				actual = cLine.modifyHyperEdge(actual);
			} // fi selected
		} //end while (Edge Iteration
		return changed;
	} //end modify Edges
	/**
	 * modify the selected Edges and Nodes in the Subgraphs they belong to
	 * 
	 * @return true if something was changed, else false
	 */
	private boolean modifySubgraphs()
	{
		if (!bChSubgraph.isSelected())
			return false; //Don't do it if the user diesn't want to
		boolean change=false;
		Iterator<VNode> nodeiter = vnodes.getIterator();
		while (nodeiter.hasNext())
		{
			VNode actual = nodeiter.next();
			if ((actual.getSelectedStatus() & VItem.SELECTED) == VItem.SELECTED)
			{
				Vector<String> names = msubs.getNames();
				int position = 0;
				for (int i=0; i<names.size(); i++)
				{
					if (names.get(i)!=null) //Subgraph with this index exists
					{
						change = true;
						if (bSubgraph[position].isSelected())
							vsubs.addNodetoSubgraph(actual.getIndex(), i);
						else
							vsubs.removeNodefromSubgraph(actual.getIndex(), i);
						position++;
					}
				}
			}
		}
		//And the same for the edges/hyperedges
		Iterator<VEdge> edgeiter = null; 
		if (vedges!=null)
			edgeiter = vedges.getIterator();
		Iterator<VHyperEdge> hyperedgeiter = null; 
		if (vhyperedges!=null)
			hyperedgeiter = vhyperedges.getIterator();
		while ( ( (edgeiter!=null)&&(edgeiter.hasNext()) ) || ( (hyperedgeiter!=null)&&(hyperedgeiter.hasNext()) ) )
		{
			VItem actual;
			if (edgeiter!=null)
				actual = edgeiter.next();
			else if (hyperedgeiter!=null)
				actual = hyperedgeiter.next();
			else
				break;
			if ((actual.getSelectedStatus() & VItem.SELECTED) == VItem.SELECTED)
			{
				Vector<String> names = msubs.getNames();
				int position = 0;
				for (int i=0; i<names.size(); i++)
				{
					if (names.get(i)!=null) //Subgraph with this index exists
					{
						change = true;
						if (bSubgraph[position].isSelected())
							vsubs.addEdgetoSubgraph(actual.getIndex(), i);
						else
							vsubs.removeEdgefromSubgraph(actual.getIndex(), i);
						position++;
					}
				}
			}
		}
		return change;
	}
	//
	//
	//Checks
	//
	//
	/**
	 * Check the Inputfields of the Position Tab. If Method is Chosen its fields must be filles
	 * @return true, if all is correct else false
	 */
	private boolean checkPosition()
	{
		if (cPosition.getSelectedIndex()==1)
		{
			if ((iPosMoveX.getValue()<iPosMoveX.getMinValue())||(iPosMoveY.getValue()<iPosMoveX.getMinValue()))
			{
				JOptionPane.showMessageDialog(this, "<html><p>Verschieben nicht m"+main.CONST.html_oe+"glich.<br>Einer der beiden Werte wurde nicht angegeben.</p></html>","Fehler",JOptionPane.ERROR_MESSAGE);
				return false;
			}
		}
		if (cPosition.getSelectedIndex()==2) //Arrange in Circle
		{
			if ((iCircleRadius.getValue()==-1)||(iOriginX.getValue()==-1)||(iOriginY.getValue()==-1)||(iFirstNodeAtDegree.getValue()==-1))
			{
				JOptionPane.showMessageDialog(this, "<html><p>Anordnen nicht m"+main.CONST.html_oe+"glich.<br>Einer der vier Werte wurde nicht angegeben.</p></html>","Fehler",JOptionPane.ERROR_MESSAGE);
				return false;
			}
			int negX = iOriginY.getValue() - iCircleRadius.getValue();
			int negY = iOriginX.getValue() - iCircleRadius.getValue();
			if ((negX < 0)||(negY < 0))
			{
				int sel = JOptionPane.showConfirmDialog(Gui.getInstance().getParentWindow(), "<html><p>Die Anordnung im Kreis verschiebt den Graphen,<br>da der Kreis aus der Zeichenfl"+main.CONST.html_ae+"che herausragt.</p></html>"
						, "Verschieben bestätigen", JOptionPane.OK_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE);
				if (sel == JOptionPane.CANCEL_OPTION)
					return false;
			}
		}
		return true;
	}
	/**
	 * Check All Node Fields. If a Box is Checked its Value must be Valid
	 * @return true, if all fields are correct else false (also returns an error message to the UI)
	 */
	private boolean checkNode()
	{
		if (bChNodeSize.isSelected()&&(iNodeSize.getValue()==-1))
		{
			JOptionPane.showMessageDialog(this, "<html><p>Ver"+main.CONST.html_ae+"ndern der Werte nicht m"+main.CONST.html_oe+"glich.<br>Die neue Knotengr"+main.CONST.html_oe+main.CONST.html_sz+"e wurde nicht angegeben.</p></html>","Fehler",JOptionPane.ERROR_MESSAGE);
			return false;				
		}
		String t = cNodeName.VerifyInput();
		if (!t.equals(""))
		{
			JOptionPane.showMessageDialog(this, "<html><p>Ver"+main.CONST.html_ae+"ndern der Werte nicht m"+main.CONST.html_oe+"glich.<br>"+t+"</p></html>","Fehler",JOptionPane.ERROR_MESSAGE);
			return false;				
		}
		return true;
	}
	/**
	 * Check All Edge Fields, if a Checkbox is activated, its Value behind must be Valid
	 * @return true if all fields are valid else false
	 */
	private boolean checkEdge()
	{
		if (bChEdgeValue.isSelected()&&(iEdgeValue.getValue()==-1))
		{
			JOptionPane.showMessageDialog(this, "<html><p>Ver"+main.CONST.html_ae+"ndern der Werte nicht m"+main.CONST.html_oe+"glich.<br>Das neue Kantengewicht wurde nicht angegeben.</p></html>","Fehler",JOptionPane.ERROR_MESSAGE);
			return false;				
		}
		if (bChEdgeWidth.isSelected()&&(iEdgeWidth.getValue()==-1))
		{
			JOptionPane.showMessageDialog(this, "<html><p>Ver"+main.CONST.html_ae+"ndern der Werte nicht m"+main.CONST.html_oe+"glich.<br>Die neue Kantenbreite wurde nicht angegeben.</p></html>","Fehler",JOptionPane.ERROR_MESSAGE);
			return false;				
		}
		if ((vhyperedges!=null)&&(bChHyperedgeMargin.isSelected())&&(iHyperedgeMargin.getValue()==-1))
		{
			JOptionPane.showMessageDialog(this, "<html><p>Ver"+main.CONST.html_ae+"ndern der Werte nicht m"+main.CONST.html_oe+"glich.<br>Der neue Hyperkanteninnenabstand wurde nicht angegeben.</p></html>","Fehler",JOptionPane.ERROR_MESSAGE);
			return false;							
		}
		String t="";
		if (bChEdgeName.isSelected())
			t = cText.VerifyInput(sEdgeName.getText());
		else //the edge text does not bother us
			t = cText.VerifyInput("  ");
		if (!t.equals(""))
		{
			JOptionPane.showMessageDialog(this, "<html><p>Ver"+main.CONST.html_ae+"ndern der Werte nicht m"+main.CONST.html_oe+"glich.<br>"+t+".</p></html>","Fehler",JOptionPane.ERROR_MESSAGE);
			return false;				
		}
		t = cLine.VerifyInput();
		if (!t.equals(""))
		{
			JOptionPane.showMessageDialog(this, "<html><p>Ver"+main.CONST.html_ae+"ndern der Werte nicht m"+main.CONST.html_oe+"glich.<br>"+t+".</p></html>","Fehler",JOptionPane.ERROR_MESSAGE);
			return false;				
		}
		if (directed)
		{
			t = cArrow.VerifyInput();
			if (!t.equals(""))
			{
				JOptionPane.showMessageDialog(this, "<html><p>Ver"+main.CONST.html_ae+"ndern der Werte nicht m"+main.CONST.html_oe+"glich.<br>"+t+".</p></html>","Fehler",JOptionPane.ERROR_MESSAGE);
				return false;				
			}
				
		}
		if ((vedges!=null)&&(cLoop!=null))
		{
			t="";
			t = cLoop.VerifyInput();
			if (!t.equals(""))
			{
				JOptionPane.showMessageDialog(this, "<html><p>Ver"+main.CONST.html_ae+"ndern der Werte nicht m"+main.CONST.html_oe+"glich.<br><br>"+t+"</p></html>","Fehler",JOptionPane.ERROR_MESSAGE);
				return false;				
			}
		}
			return true;
	}
	/**
	 * Check all Tabs if they are correct (uses the three Methods for each Tab)
	 * @return true if all fields aree correct, else false
	 */
	private boolean check()
	{
		if (show_position)
		{
			if (!checkPosition())
				return false;
		}
		if (show_nodeprop)
		{
			if (!checkNode())
				return false;
		}
		if (show_edgeprop)
		{
			if (!checkEdge())
				return false;
		}
		//Subgraphs don't have to be checked ;) only checkboxes, so no user error possible
		
		return true;
	}
	/**
	 * Modify The Grah as the user has specified
	 * @return true if a Modification was done
	 */
	private boolean doModify()
	{
		if (!check())
			return false;
		int changed = 0;		
		if (show_position)
		{
			if (cPosition.getSelectedIndex()==1)
			{
				translate();
			}
			if (cPosition.getSelectedIndex()==2)
			{
				arrangeCircle();
			}
			changed = GraphConstraints.NODE;
			if (vedges!=null)
				changed |= GraphConstraints.EDGE;
			else if (vhyperedges!=null)
				changed |= GraphConstraints.HYPEREDGE;
		}
		if (show_nodeprop)
				changed |= GraphConstraints.NODE;
		if (show_edgeprop)
		{
			if (vedges!=null)
				changed |= GraphConstraints.EDGE;
			else if (vhyperedges!=null)
				changed |= GraphConstraints.HYPEREDGE;
		}
		if (show_subgraphs)
				changed |= GraphConstraints.SUBGRAPH;

		GraphMessage startblock = new GraphMessage(changed,GraphConstraints.BLOCK_START|GraphConstraints.UPDATE);
		startblock.setMessage("Auswahl verändert");
		graphRef.pushNotify(startblock);
		if (show_nodeprop)
			modifyNodes();
		if (show_edgeprop)
		{
			if (vedges!=null)
				modifyEdges();
			else if (vhyperedges!=null)
				modifyHyperEdges();
		}
		if (show_subgraphs)
			modifySubgraphs();
		
		graphRef.pushNotify(new GraphMessage(changed,GraphConstraints.BLOCK_END));
		return true;
	}
	public void actionPerformed(ActionEvent event) {
		if (event.getSource()==cPosition)
		{
			for (int i=0; i<Positioncontent.length; i++)
			{
				Positioncontent[i].setVisible(false);
			}
			Positioncontent[cPosition.getSelectedIndex()].setVisible(true);
			this.invalidate();
		}
		else if (event.getSource()==bCancel)
		{
			this.dispose();
		}
		else if (event.getSource()==bOk)
		{
			if (doModify())
				dispose();
		}
		//
		//Knoten Enable Checkboxes
		else if (event.getSource()==bChNodeName)
		{
			sNodeName.setEnabled(bChNodeName.isSelected());
			if (bChNodeName.isSelected())
			{
				NodeName.setForeground(Color.BLACK);
				NodePreview.setForeground(Color.BLACK);
			}
			else
			{
				NodeName.setForeground(Color.GRAY);
				NodePreview.setForeground(Color.GRAY);
			}
		}
		else if (event.getSource()==bChNodeSize)
		{
			iNodeSize.setEnabled(bChNodeSize.isSelected());
			if (bChNodeSize.isSelected())
				NodeSize.setForeground(Color.BLACK);
			else
				NodeSize.setForeground(Color.GRAY);
		}
		//KantenDatenfelder
		else if (event.getSource()==bChEdgeName)
		{
			sEdgeName.setEnabled(bChEdgeName.isSelected());
			if (bChEdgeName.isSelected())
				EdgeName.setForeground(Color.BLACK);
			else
				EdgeName.setForeground(Color.GRAY);
		}
		else if (event.getSource()==bChEdgeWidth)
		{
			iEdgeWidth.setEnabled(bChEdgeWidth.isSelected());
			if (bChEdgeWidth.isSelected())
				EdgeWidth.setForeground(Color.BLACK);
			else
				EdgeWidth.setForeground(Color.GRAY);
		}
		else if (event.getSource()==bChEdgeValue)
		{
			iEdgeValue.setEnabled(bChEdgeValue.isSelected());
			if (bChEdgeValue.isSelected())
				EdgeValue.setForeground(Color.BLACK);
			else
				EdgeValue.setForeground(Color.GRAY);
		}
		else if (event.getSource()==bChHyperedgeMargin)
		{
			iHyperedgeMargin.setEnabled(bChHyperedgeMargin.isSelected());
			if (bChHyperedgeMargin.isSelected())
				HyperedgeMargin.setForeground(Color.BLACK);
			else
				HyperedgeMargin.setForeground(Color.GRAY);
		}
		//Subgraph elements
		else if (event.getSource()==bChSubgraph)
		{
			if (bChSubgraph.isSelected())
				bChSubgraph.setForeground(Color.BLACK);
			else
				bChSubgraph.setForeground(Color.GRAY);
			
			for (int i=0; i<bSubgraph.length; i++)
			{
				bSubgraph[i].setEnabled(bChSubgraph.isSelected());
			}

		}
	}
	public void caretUpdate(CaretEvent event) 
	{
	    if (event.getSource()==sNodeName)
		{
			String t = GeneralPreferences.replace(sNodeName.getText(),"$ID","4");
			if (t.length() > 10)
				t = t.substring(0,10)+"...";	
			NodePreview.setText("<html><font size=-1>Vorschau: <i>"+t+"</i></font></html>");
			NodePreview.validate();
		}
		else if (event.getSource()==sEdgeName)
		{
			String t = sEdgeName.getText();
			t = GeneralPreferences.replace(t,"$ID","4");
			t = GeneralPreferences.replace(t,"$SID","1");
			t = GeneralPreferences.replace(t,"$EID","2");
			if (t.length() > 10)
				t = t.substring(0,10)+"...";
			EdgePreview.setText("<html><font size=-1>Vorschau: <i>"+t+"</i></font></html>");
			EdgePreview.validate();
		}
	}
}
