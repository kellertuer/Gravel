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

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import dialogs.components.*;

import view.Gui;

import model.MGraph;
import model.VEdge;
import model.VGraph;
import model.VLoopEdge;
import model.VNode;
import model.VStraightLineEdge;
import model.VSubSet;
/**
 * This class provides an UI for modifying all selected Nodes and Edges (if they exist)
 * 
 * @author ronny
 *
 */
public class JSelectionModifyDialog extends JDialog implements ActionListener, CaretListener 
{
	private static final long serialVersionUID = 1L;
	VGraph vg;
	boolean show_position, show_nodeprop, show_edgeprop, show_subsets;
	
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
	//drittes Tab - Kanteneigenschaften
	//
	private JLabel EdgeName, EdgeWidth, EdgeValue, EdgePreview;
	private JTextField sEdgeName;
	private IntegerTextField iEdgeWidth, iEdgeValue;
	private JCheckBox bChEdgeName, bChEdgeWidth, bChEdgeValue;
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
	private JLabel SubSet;
	private Vector<String> subsetnames;
	private JCheckBox[] bSubSet;
	private JScrollPane iSubSets;
	private JCheckBox bChSubSets;
	
	
	private JButton bCancel, bOk;
	/**
	 * Initializes the Dialog and shows all possible tabs
	 * 
	 * @param vg the corresponding VGraph
	 */
	public JSelectionModifyDialog(VGraph vg)
	{
		this(true, true, true, "Selektion bearbeiten", vg);
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
	public JSelectionModifyDialog(boolean translate, boolean properties, boolean subgraphs, String title, VGraph graph)
	{
		vg = graph;
		//If selected nodes exist and the tab should be shown
		show_position = translate & vg.selectedNodeExists();
		//If selected nodes exist and the porperties should be shown
		show_nodeprop = properties & vg.selectedNodeExists();
		show_edgeprop = properties & vg.selectedEdgeExists();
		show_subsets = subgraphs & (vg.selectedEdgeExists() || vg.selectedNodeExists())&&(vg.SubSetCount() > 0);
		//None of the tabs should be shown, that would be quite wrong
		setTitle(title);
		tabs = new JTabbedPane();
		if (show_position)
			tabs.addTab("Position",buildPositionTab());
		if (show_nodeprop)
			tabs.addTab("Knoten",buildNodePropContent());
		if (show_edgeprop)
			tabs.addTab("Kanten",buildEdgePropContent());
		if (show_subsets)
			tabs.addTab("Untergraphen",buildSubSetContent());
		
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
		
		c.gridx = 1;
		c.anchor = GridBagConstraints.EAST;
		bOk = new JButton("Ok");
		bOk.setMnemonic(KeyEvent.VK_ENTER);
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
		if (show_subsets)
			fillCommonSubSets();

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
		//Werte suchen fuer die Initialisierung/Gemeinsame werte der Selektion
		Iterator<VNode> nodeiter = vg.getNodeIterator();
		int preNodeSize=-1, preNodeTextSize=-1, preNodeTextDis=-1, preNodeTextRot=-1;
		//ShowText ist der wert und given sagt, ob der allgemeingültig ist
		boolean preNodeShowText=false, preNodeShowTextgiven=true,beginning = true;
		String nodename=null;
		while (nodeiter.hasNext())
		{
			VNode temp = nodeiter.next();
			if (temp.isSelected())
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
					nodename = vg.getNodeName(temp.index);
					//Replace the id numer by $ID (at least try to get a common name
					nodename = GeneralPreferences.replace(nodename,""+temp.index, "$ID");
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
						if (!GeneralPreferences.replace(nodename,"$ID",""+temp.index).equals(vg.getNodeName(temp.index)))
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
		
		c.gridy++;
		c.gridx=0;
		c.gridwidth = 3;
		EdgeTabs = new JTabbedPane();
		cText = new CEdgeTextParameters(null,false,true); //not global but with checkboxes, Std-Values
		EdgeTabs.add("Beschriftung",cText.getContent());
		cLine = new CEdgeLineParameters(null,false,true); //not global, with CheckBoxes, Std-Values
		EdgeTabs.add("Linienart", cLine.getContent());
		if (vg.isDirected())
		{
			cArrow = new CEdgeArrowParameters(null,true); //std values, with checks
			EdgeTabs.add("Pfeil", cArrow.getContent());
		}
		Iterator<VEdge> edgeiter = vg.getEdgeIterator();
		boolean loops=false;
		while (edgeiter.hasNext())
		{
			VEdge e = edgeiter.next();
			loops |= (e.getType()==VEdge.LOOP)&&(e.isSelected()); //Existiert eine selectierte Schleife ?
		}
		if (loops)
		{
			cLoop = new CLoopParameters(null,vg.isDirected(),true);
			EdgeTabs.add("Schleife",cLoop);
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
		//Werte suchen fuer die Initialisierung/Gemeinsame werte der Selektion
		Iterator<VEdge> edgeiter = vg.getEdgeIterator();
		VEdge pre = new VStraightLineEdge(0,0);
		int preEdgeValue=0;
		boolean preEdgeShowTextgiven=true,beginning = true, preEdgeTextShowValuegiven=true, preEdgeLineTypegiven=true;
		String preEdgeName=null;
		VLoopEdge vle = null;
		boolean clockwiseequal = true;
		while (edgeiter.hasNext())
		{
			VEdge temp = edgeiter.next();
			if (temp.isSelected())
			{
				if (beginning)
				{
					beginning = false;
					pre = temp.clone();
					preEdgeValue = vg.getEdgeProperties(temp.index).get(MGraph.EDGEVALUE);
					preEdgeName = vg.getEdgeName(temp.index);
					//Replace the id numer by $ID (at least try to get a common name
					preEdgeName = GeneralPreferences.replace(preEdgeName,""+temp.index, "$ID");
					preEdgeName = GeneralPreferences.replace(preEdgeName,""+vg.getEdgeProperties(temp.index).get(MGraph.EDGESTARTINDEX), "$SID");
					preEdgeName = GeneralPreferences.replace(preEdgeName,""+vg.getEdgeProperties(temp.index).get(MGraph.EDGEENDINDEX), "$EID");					
					if (pre.getType()==VEdge.LOOP) // very first is a Loop
					{
						vle = (VLoopEdge)pre.clone();
					}
				}
				else
				{ // if an edge value differs set common name to -1
					if (vg.getEdgeProperties(temp.index).get(MGraph.EDGEVALUE)!=preEdgeValue)
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
					
					if (pre.getArrowAngle()!=temp.getArrowAngle())
						pre.setArrowAngle(-1f);
					if (pre.getArrowPart()!=temp.getArrowPart())
						pre.setArrowPart(-1f);
					if (pre.getArrowPos()!=temp.getArrowPos())
						pre.setArrowPos(-1f);
					if (pre.getArrowSize()!=temp.getArrowSize())
						pre.setArrowSize(-1f);
					if (preEdgeName!=null)
					{
						String tname = GeneralPreferences.replace(preEdgeName,"$ID",""+temp.index);
						tname = GeneralPreferences.replace(tname,"$SID",""+vg.getEdgeProperties(temp.index).get(MGraph.EDGESTARTINDEX));
						tname = GeneralPreferences.replace(tname,"$EID",""+vg.getEdgeProperties(temp.index).get(MGraph.EDGEENDINDEX));
						if (!tname.equals(vg.getEdgeName(temp.index)))
							preEdgeName = null;
					}
					if (temp.getType()==VEdge.LOOP)
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
		
		cText.InitValues(pre);
		cText.updateUI(preEdgeShowTextgiven, preEdgeTextShowValuegiven);

		cLine.InitValues(pre);
		cLine.updateUI(preEdgeLineTypegiven);
		
		if (vg.isDirected())
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
		{
			cLoop.InitValues(vle, clockwiseequal);
		}
	}
	
	//
	//
	// Subgraph Tab
	//
	//
	/**
	 * Build the Tab for the Subsets
	 * @return the subset Container
	 */
	private Container buildSubSetContent()
	{
		Container SubSetContent = new Container();
		SubSetContent.setLayout(new GridBagLayout());
		Container SubSetList = new Container();
		SubSetList.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(0,0,0,0);
		c.anchor = GridBagConstraints.CENTER;
		c.gridy = 0;
		c.gridx = 0;
		bChSubSets = new JCheckBox("<html><p>Selektion den Untergraphen zuprdnen:</p></html>");
		bChSubSets.addActionListener(this);
		SubSetContent.add(bChSubSets,c);
		subsetnames = vg.getSetNames();
		int temp = 0;
		for (int i=0; i<subsetnames.size(); i++)
		{
			if (subsetnames.elementAt(i)!=null) //Ein Knoten mit dem Index existiert
			temp ++; //Anzahl Untergraphen zaehlen
		}
		this.bSubSet = new JCheckBox[temp];
		temp = 0;
		for (int i=0; i<subsetnames.size(); i++)
		{
			if (subsetnames.elementAt(i)!=null) //Ein Knoten mit dem Index existiert
			{
				bSubSet[temp] = new JCheckBox(vg.getSubSetName(i));
				SubSetList.add(bSubSet[temp],c);
				c.gridy++;
				temp++; //Anzahl Knoten zaehlen
			}
		}
		c.gridy = 1;
		iSubSets = new JScrollPane(SubSetList);
		iSubSets.setAlignmentX(JScrollPane.LEFT_ALIGNMENT);
		iSubSets.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		iSubSets.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		iSubSets.setPreferredSize(new Dimension(200,150));
		SubSetContent.add(iSubSets,c);
		return SubSetContent;
	}
	private void fillCommonSubSets()
	{
		boolean preSubSetsequal = true;
		Vector<Boolean> subsets = new Vector<Boolean>(vg.getSetNames().size());
		subsets.setSize(vg.getSetNames().size());
		Iterator<VNode> nodeiter = vg.getNodeIterator();
		boolean beginning = true;
		while (nodeiter.hasNext())
		{
			VNode actual = nodeiter.next();
			if (actual.isSelected())
			{
				Iterator<VSubSet> subsetiter = vg.getSubSetIterator();
				while (subsetiter.hasNext())
				{
					VSubSet s = subsetiter.next();
					if (beginning) //Set the Subsets the first node belongs to...
						subsets.set(s.getIndex(), new Boolean(vg.SubSetcontainsNode(actual.index, s.getIndex())));
					else
					{
						if (subsets.get(s.getIndex()).booleanValue()!=vg.SubSetcontainsNode(actual.index, s.getIndex()))
							preSubSetsequal = false;
					}
				}
				beginning = false;
			}
		}
		Iterator<VEdge> edgeiter = vg.getEdgeIterator();
		while (edgeiter.hasNext())
		{
			VEdge actual = edgeiter.next();
			if (actual.isSelected())
			{
				Iterator<VSubSet> subsetiter = vg.getSubSetIterator();
				while (subsetiter.hasNext())
				{
					VSubSet s = subsetiter.next();
					if (beginning) //Set the Subsets the first edge belongs to (happens if no node selected) belongs to...
						subsets.set(s.getIndex(), new Boolean(vg.SubSetcontainsEdge(actual.index, s.getIndex())));
					else
					{
						if (subsets.get(s.getIndex()).booleanValue()!=vg.SubSetcontainsEdge(actual.index, s.getIndex()))
							preSubSetsequal = false;
					}
				}
				beginning = false;
			}
		}
		this.bChSubSets.setSelected(preSubSetsequal);
		if (preSubSetsequal) //All selected are in the same subsets
		{
			int position = 0;
			for (int i=0; i<subsets.size(); i++)
			{
				if (subsets.get(i)!=null)
				{
					this.bSubSet[position].setSelected(subsets.get(i).booleanValue());
					position++;
				}
			}
		}
		else
		{
			bChSubSets.setForeground(Color.GRAY);
			iSubSets.setEnabled(false);
			for (int i=0; i<bSubSet.length; i++)
			{
				bSubSet[i].setEnabled(false);
			}
		}
	}
	//
	//Modify Graph
	//
	/**
	 * If a node is Translated, this method moves its adjacent edges (controllpoints) with half the movement
	 */
	private void translateAdjacentEdges(int nodeindex, int x, int y)
	{
		x = Math.round(x/2);
		y = Math.round(y/2);
		Iterator<VEdge> edgeiter = vg.getEdgeIterator();
		while (edgeiter.hasNext())
		{
			VEdge e = edgeiter.next();
			int start = vg.getEdgeProperties(e.index).elementAt(MGraph.EDGESTARTINDEX);
			int ende = vg.getEdgeProperties(e.index).elementAt(MGraph.EDGEENDINDEX);
			if ((start==nodeindex)||(ende==nodeindex))
					e.translate(x, y);
		}
	}
	/**
	 * Translates all Selected Node by the Values given through the UI
	 *
	 */
	private void translate()
	{
		Iterator<VNode> nodeiter = vg.getNodeIterator();
		while (nodeiter.hasNext())
		{
			VNode t = nodeiter.next();
			if (t.isSelected())
			{
					Point newpoint = t.getPosition(); //bisherige Knotenposition
					newpoint.translate(iPosMoveX.getValue(),iPosMoveY.getValue()); //Bewegung im Graphen aber mit Rungungsfehlern, also nur zurbetrachtung der Gesamtgraphbewegung
					if (newpoint.x < 0)
					{
						vg.translate(Math.abs(newpoint.x), 0); //Um die Differenz verschieben (Zoomfactor aufheben)
						newpoint.x=0;
					}
					if (newpoint.y < 0)
					{
						vg.translate(0,Math.abs(newpoint.y));
						newpoint.y = 0;
					}
					t.setPosition(newpoint); //Translate selected node
					//move Adjacent Edges
					translateAdjacentEdges(t.index,iPosMoveX.getValue(),iPosMoveY.getValue());
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
		vg.translate(x, y);
		//Kreismittelpunkt auch verschieben
		iOriginX.setValue(iOriginX.getValue()+x);
		iOriginY.setValue(iOriginY.getValue()+y);

		//Knoten zaehlen
		int nodecount = 0;
		Iterator<VNode> nodeiter = vg.getNodeIterator();
		while (nodeiter.hasNext())
		{
			if (nodeiter.next().isSelected())
				nodecount++;				
		}
		//System.err.println("Found "+nodecount+" selected nodes");
		
		//loslegen
		double start = - (new Integer(iFirstNodeAtDegree.getValue())).doubleValue();
		double part = (new Integer(360)).doubleValue()/(new Integer(nodecount)).doubleValue();
		
		int mx = iOriginX.getValue();
		int my = iOriginY.getValue();
		int mr = iCircleRadius.getValue();
		double actualdeg = start;
		nodeiter = vg.getNodeIterator();
		while (nodeiter.hasNext()) 
		{
			VNode temp = nodeiter.next();
			if (temp.isSelected())
			{
				double posx = (new Integer(mr)).doubleValue()*Math.cos(2*Math.PI*actualdeg/360d);
				double posy = (new Integer(mr)).doubleValue()*Math.sin(2*Math.PI*actualdeg/360d);
						
				Point newpos = new Point(mx+Math.round((new Double(posx)).floatValue()),my+Math.round((new Double(posy)).floatValue()));
			//	System.err.println("Now placing node #"+temp.index+"("+vg.getNodeName(temp.index)+") at "+actualdeg+" Degree - Position on Circle ("+posx+","+posy+") and in RL ("+newpos.x+","+newpos.y+")");
				int diffx = temp.getPosition().x - newpos.x;
				int diffy = temp.getPosition().y - newpos.y;
				temp.setPosition(newpos);	
				//move adjacent edges also
				translateAdjacentEdges(temp.index, diffx, diffy);
				actualdeg += part % 360;
			}
		}		
	}
	/**
	 * Modify Selected Ndes and set all selected Values (checked Values) in each node
	 *
	 */
	private void modifyNodes()
	{ //Set all nodes to the selected values, if they are selected
		Iterator<VNode> nodeiter = vg.getNodeIterator();
		while (nodeiter.hasNext())
		{
			VNode actual = nodeiter.next();
			if (actual.isSelected())
			{
				//Node Name
				if (bChNodeName.isSelected())
					vg.setNodeName(actual.index, GeneralPreferences.replace(sNodeName.getText(), "$ID",""+actual.index));
				//Node Size
				if (bChNodeSize.isSelected())
					actual.setSize(iNodeSize.getValue());
				actual = cNodeName.modifyNode(actual);
			} //end if selected
		} //end while
		
	}
	/**
	 * Modify selected Edges - set all checked Values in each selected Edge
	 *
	 */
	private void modifyEdges()
	{
		Iterator<VEdge> edgeiter = vg.getEdgeIterator();
		while (edgeiter.hasNext())
		{
			VEdge actual = edgeiter.next();
			if (actual.isSelected())
			{
				//stupidly apply all values that are selected to do so
				if (bChEdgeName.isSelected())
				{
					String t = sEdgeName.getText();
					t = GeneralPreferences.replace(t,"$ID",actual.index+"");
					t = GeneralPreferences.replace(t,"$SID",vg.getEdgeProperties(actual.index).get(MGraph.EDGESTARTINDEX)+"");
					t = GeneralPreferences.replace(t,"$EID",vg.getEdgeProperties(actual.index).get(MGraph.EDGEENDINDEX)+"");
					vg.setEdgeName(actual.index, t);
				}
				if (bChEdgeValue.isSelected())
					vg.getMathGraph().setEdgeValue(actual.index, iEdgeValue.getValue());
				if (bChEdgeWidth.isSelected())
					actual.setWidth(iEdgeWidth.getValue());
				
				actual = cText.modifyEdge(actual);
				actual = cLine.modifyEdge(actual);
				
				if (vg.isDirected())
					actual = cArrow.modifyEdge(actual);

				if (cLoop!=null) //Loops exist
					actual = cLoop.modifyEdge(actual); //Modify Loops
			} // fi selected
		} //end while (Edge Iteration
	} //end modify Edges
	
	//
	//
	//Checks
	//
	//
	/**
	 * modify the selected Edges and Nodes in the SubSets they belong to
	 */
	private void modifySubSets()
	{
		if (!bChSubSets.isSelected())
			return; //Don't do it if the user diesn't want to
		Iterator<VNode> nodeiter = vg.getNodeIterator();
		while (nodeiter.hasNext())
		{
			VNode actual = nodeiter.next();
			if (actual.isSelected())
			{
				Vector<String> names = vg.getSetNames();
				int position = 0;
				for (int i=0; i<names.size(); i++)
				{
					if (names.get(i)!=null) //SubSet with this index exists
					{
						if (bSubSet[position].isSelected())
							vg.addNodetoSubSet(actual.index, i);
						else
							vg.removeNodefromSubSet(actual.index, i);
						position++;
					}
				}
			}
		}
		//And the same for the edges
		Iterator<VEdge> edgeiter = vg.getEdgeIterator();
		while (edgeiter.hasNext())
		{
			VEdge actual = edgeiter.next();
			if (actual.isSelected())
			{
				Vector<String> names = vg.getSetNames();
				int position = 0;
				for (int i=0; i<names.size(); i++)
				{
					if (names.get(i)!=null) //SubSet with this index exists
					{
						if (bSubSet[position].isSelected())
							vg.addEdgetoSubSet(actual.index, i);
						else
							vg.removeEdgefromSubSet(actual.index, i);
						position++;
					}
				}
			}
		}

	}
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
		if (vg.isDirected())
		{
			t = cArrow.VerifyInput();
			if (!t.equals(""))
			{
				JOptionPane.showMessageDialog(this, "<html><p>Ver"+main.CONST.html_ae+"ndern der Werte nicht m"+main.CONST.html_oe+"glich.<br>"+t+".</p></html>","Fehler",JOptionPane.ERROR_MESSAGE);
				return false;				
			}
				
		}
		if (cLoop!=null)
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
		//SubSets don't have to be checked ;) only checkboxes, so no user error possible
		
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
		}
		if (show_nodeprop)
			modifyNodes();
		if (show_edgeprop)
			modifyEdges();
		if (show_subsets)
			modifySubSets();
		vg.pushNotify("NES");
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
		//SubSet
		else if (event.getSource()==bChSubSets)
		{
			if (bChSubSets.isSelected())
				bChSubSets.setForeground(Color.BLACK);
			else
				bChSubSets.setForeground(Color.GRAY);
			
			for (int i=0; i<bSubSet.length; i++)
			{
				bSubSet[i].setEnabled(bChSubSets.isSelected());
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
