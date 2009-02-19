package dialogs;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.ButtonGroup;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import dialogs.components.*;

import model.MEdge;
import model.VEdge;
import model.VEdgeLinestyle;
import model.VEdgeText;
import model.VGraph;
import model.VItem;
import model.VOrthogonalEdge;
import model.VQuadCurveEdge;
import model.VSegmentedEdge;
import model.VStraightLineEdge;
import model.Messages.GraphMessage;

import view.Gui;
/**
 * This Dialog is used to change the properties of an edge and to create new edges.
 * 
 * It includes Initialization of all field with the values of a given edge,
 * the text of the validity of the parameter fields
 * and
 * the change of the edge in the graph it belongs to
 *
 *
 * @author Ronny Bergmann
 */
public class JEdgeDialog extends JDialog implements ActionListener, ItemListener
{
	private static final long serialVersionUID = 1L;

	private int oldindex, oldvalue, oldstart, oldend, oldwidth; //Beim Abbrechen Wiederherstellen
	@SuppressWarnings("unused")
	private VEdgeText oldText;	
	@SuppressWarnings("unused")
	private VEdgeLinestyle oldLinestyle;
	private Vector<Point> segmentpoints;
	private Vector<String> pointstrings;
	private JComboBox cStart, cEnd;
	private Vector<String> nodelist; //Zum rueckwaerts nachschauen des Indexes
	private String[] nodenames; //Array der Knotennamen
	private Vector<String> subgraphlist;
	private JCheckBox[] SubgraphChecks;
	private JScrollPane iSubgraph;
	private VGraph graphref;
	private IntegerTextField iEdgeIndex, iWidth, iValue, iQCx,iQCy,iSegx,iSegy;
	private JButton bSegAdd,bSegRem;
	private JList lPoints;
	private JTextField Colorfield, EdgeName;
	private JCheckBox bOrthVertFirst;
	private ButtonGroup gEdgeType;
	private JRadioButton rStraightLine, rQuadCurve, rSegmented, rOrthogonal;
	private JButton bOK, bCancel;
		
	private CEdgeArrowParameters cArrow;
	private CEdgeArrowPreview cArrowP;
	private CEdgeLineParameters cLine;
	private CEdgeTextParameters cText;
	
	private Container cEdgeTypeField, cStraightLine, cQuadCurve, cSegmented, cOrthogonal, MainContent,ArrowContent, TextContent;
	CLoopParameters cLoop;
	private JTabbedPane tabs;
	private VEdge chEdge;
	
	/**
	 * Initialize the dialog with given values for creation of an edge 
	 * @param index the edge index
	 * @param start index of the startnode
	 * @param end index of the endnode of the edhe
	 * @param width width of the edge
	 * @param vg corresponding vgraph
	 */
	public JEdgeDialog(int index, int start, int end, int width,VGraph vg)
	{
		chEdge = null;
		oldindex = index;
		oldstart = start;
		oldend = end;
		oldwidth = width;
		graphref = vg;
		oldText = new VEdgeText(); //Std-Werte
		oldLinestyle = new VEdgeLinestyle();
		CreateDialog(null, Gui.getInstance().getVGraph());
	}
	/**
	 * Initialization of the Dialog with the properties of
	 * @param e an edge in the 
	 * @param vg corresponding VGraph
	 */
	public JEdgeDialog(VEdge e, VGraph vg)
	{
		CreateDialog(e,vg);
	}
	/**
	 * initialization of all parameter fields to the given values
	 * @param e
	 * @param vG
	 */
	private void CreateDialog(VEdge e, VGraph vG)
	{
		graphref = vG;		
		if (e==null)
		{
			this.setTitle("Neue Kante erstellen");
			chEdge = null;
		}
		else
		{
			chEdge = e;
			oldindex = e.getIndex();
			MEdge me = graphref.getMathGraph().modifyEdges.get(e.getIndex());
			oldstart = me.StartIndex;
			oldend = me.EndIndex;
			oldvalue = me.Value;
			oldwidth = e.getWidth();
			oldText = e.getTextProperties().clone();
			oldLinestyle = e.getLinestyle().clone(); 
			this.setTitle("Eigenschaften der Kante '"+graphref.getMathGraph().modifyEdges.get(e.getIndex()).name+"' (#"+e.getIndex()+") von '"+graphref.getMathGraph().modifyNodes.get(oldstart).name+"'->'"+graphref.getMathGraph().modifyNodes.get(oldend).name+"'");	
		}
		
		tabs = new JTabbedPane();
		GridBagConstraints c = new GridBagConstraints();
		
		buildMainContent();
		buildEdgeRadioButtons();

		cArrowP = new CEdgeArrowPreview(chEdge);
		cText = new CEdgeTextParameters(chEdge,false,false); //nonglobal, no checks
		cLine = new CEdgeLineParameters(chEdge,false,false); //nonglobal no checks
		cText.addObserver(cArrowP);
		cLine.addObserver(cArrowP);
		
		TextContent = new Container();
		TextContent.setLayout(new GridBagLayout());
		c.insets = new Insets(0,0,0,0);
		c.anchor = GridBagConstraints.CENTER;
		c.gridx=0; c.gridy=0; c.gridwidth=1;
		TextContent.add(cText.getContent(),c);
		c.gridy++; TextContent.add(cLine.getContent(),c);

		c.insets = new Insets(7,7,7,7);
		c.anchor = GridBagConstraints.WEST;
		
		tabs.addTab("Allgemein", MainContent);
		tabs.addTab("Ansicht", TextContent);
		if (chEdge!=null)
		{
			if (chEdge.getType()==VEdge.LOOP)
				tabs.addTab(" Schleife ",cLoop);
			else
				tabs.addTab("Kantentyp", cEdgeTypeField);
		}				
		else
			tabs.addTab("Kantentyp", cEdgeTypeField);
		
		if (graphref.getMathGraph().isDirected())
		{
			cArrow = new CEdgeArrowParameters(chEdge,false);
			cArrow.addObserver(cArrowP);
			c.insets = new Insets(0,0,0,0);
			c.anchor = GridBagConstraints.CENTER;
			ArrowContent = new Container();
			ArrowContent.setLayout(new GridBagLayout());
			c.gridx=0; c.gridy=0; c.gridwidth=1;
			ArrowContent.add(cArrow.getContent(),c);
			c.gridy++; ArrowContent.add(cArrowP,c);
			tabs.addTab("Pfeil", ArrowContent);
		}
			//else
		//	tabs.addTab("Pfeil", new JLabel("<html><body>Pfeil-Eigenschaften sind nur einstellbar,<br> wenn ein gerichteter Graph vorliegt</body></html>"));
		Container ContentPane = this.getContentPane();
		ContentPane.setLayout(new GridBagLayout());
		c.gridy=0;c.gridx=0;c.gridwidth=2;
		ContentPane.add(tabs,c);
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
		if (e==null)
			bOK = new JButton("Kante erstellen");
		else
			bOK = new JButton("<html>"+main.CONST.html_Ae+"nderungen speichern</html>");
		bOK.addActionListener(this);
		ContentPane.add(bOK,c);
		
		this.getRootPane().setDefaultButton(bOK);
		setResizable(false);
		this.setModal(true);
		pack();
		InitVisibility();
		Point p = new Point(0,0);
		p.y += Math.round(Gui.getInstance().getParentWindow().getHeight()/2);
		p.x += Math.round(Gui.getInstance().getParentWindow().getWidth()/2);
		p.y -= Math.round(getHeight()/2);
		p.x -= Math.round(getWidth()/2);

		setLocation(p.x,p.y);
		
		this.setVisible(true);
	}
	/**
	 * Build the main Tab containing the mathematical values and the standard visual properties
	 *
	 */
	private void buildMainContent()
	{
		MainContent = new Container();
		MainContent.setLayout(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(7,7,7,7);
		c.anchor = GridBagConstraints.WEST;
		c.gridy = 0;
		c.gridx = 0;
		MainContent.add(new JLabel("Kantenindex"),c);
		c.gridx = 1;
		iEdgeIndex = new IntegerTextField();
		iEdgeIndex.setPreferredSize(new Dimension(100, 20));

		MainContent.add(iEdgeIndex,c);
		
		c.gridy++;
		c.gridx = 0;
		MainContent.add(new JLabel("Startknoten"),c);
		c.gridx = 1;
		buildNodeList();
		cStart = new JComboBox(nodenames);
		MainContent.add(cStart,c);
		
		c.gridy++;
		c.gridx = 0;
		MainContent.add(new JLabel("Endknotenknoten"),c);
		c.gridx = 1;
		cEnd = new JComboBox(nodenames);
		MainContent.add(cEnd,c);
				
		//c.gridy++;
		//c.gridx = 0;
		
		c.gridy++;
		c.gridx = 0;
		MainContent.add(new JLabel("<html>Name</html>"),c);
		c.gridx = 1;
		EdgeName = new JTextField();
		EdgeName.setPreferredSize(new Dimension(100, 20));
		MainContent.add(EdgeName,c);

		c.gridy++;
		c.gridx = 0;
		MainContent.add(new JLabel("<html>Gewicht</html>"),c);
		c.gridx = 1;
		iValue = new IntegerTextField();
		iValue.setPreferredSize(new Dimension(100, 20));
		MainContent.add(iValue,c);

		c.gridy++;
		c.gridx = 0;
		MainContent.add(new JLabel("<html>Breite</html>"),c);
		c.gridx = 1;
		iWidth = new IntegerTextField();
		iWidth.setPreferredSize(new Dimension(100, 20));
		MainContent.add(iWidth,c);

		c.gridy++;
		c.gridx = 0;
		MainContent.add(new JLabel("<html><font color=#666666>Farbe<br><font size=\"-2\">ergibt sich aus den Untergraphen</font></font></html>"),c);
		c.gridx = 1;
		Colorfield = new JTextField(); 
		Colorfield.setPreferredSize(new Dimension(100,20));
		Colorfield.setEditable(false);
		MainContent.add(Colorfield,c);
		c.gridy++;
		c.gridx = 0;
		buildSubgraphList();
		c.gridy++;
		c.gridx=0;
		c.insets = new Insets(0,7,0,7);
		c.anchor = GridBagConstraints.CENTER;
		c.gridwidth = 2;
		MainContent.add(new JLabel("Untergraphen"),c);
		c.gridy++;
		c.gridx=0;
		MainContent.add(iSubgraph,c);		
		c.gridy++;
		c.gridx = 0;
		c.insets = new Insets(7,7,7,7);
		c.gridwidth = 1;
		
		if (chEdge==null)
		{
			Colorfield.setBackground(Color.BLACK);
		}
		else
			Colorfield.setBackground(chEdge.getColor());
		
		//Werte einfuegen
		iEdgeIndex.setValue(oldindex);
		cStart.setSelectedIndex(Index2StringPos(oldstart));
		cStart.addActionListener(this);
		cEnd.setSelectedIndex(Index2StringPos(oldend));
		cEnd.addActionListener(this);
		iWidth.setValue(oldwidth);
		iValue.setValue(oldvalue);
		if (chEdge==null)
			EdgeName.setText("#"+oldstart+"->#"+oldend);
		else
		{
			EdgeName.setText(graphref.getMathGraph().modifyEdges.get(oldindex).name);
		}	
	}
	/**
	 * Help function to create the list of node names from the corresponding VGraph 
	 * to create the Nodelists for start and endnode
	 */
	private void buildNodeList()
	{
		nodelist = graphref.getMathGraph().modifyNodes.getNames();
		int temp = 0;
		for (int i=0; i<nodelist.size(); i++)
		{
			if (nodelist.elementAt(i)!=null) //Ein Knoten mit dem Index existiert
			temp ++; //Anzahl Knoten zaehlen
		}
		nodenames = new String[temp];
		temp = 0;
		for (int i=0; i<nodelist.size(); i++)
		{
			if (nodelist.elementAt(i)!=null) //Ein Knoten mit dem Index existiert
			{
				nodenames[temp] = graphref.getMathGraph().modifyNodes.get(i).name+"   (#"+i+")";
				temp++; //Anzahl Knoten zaehlen
			}
		}
	}
	/**
	 * Build the list of subgraphs
	 */
	private void buildSubgraphList()
	{
		Container CiSubgraphs = new Container();
		CiSubgraphs.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(0,0,0,0);
		c.anchor = GridBagConstraints.NORTHWEST;
		c.gridy = 0;
		c.gridx = 0;
		subgraphlist = graphref.getMathGraph().modifySubgraphs.getNames();
		int temp = 0;
		for (int i=0; i<subgraphlist.size(); i++)
		{
			if (subgraphlist.elementAt(i)!=null) //Ein Knoten mit dem Index existiert
			temp ++; //Anzahl Knoten zaehlen
		}
		SubgraphChecks = new JCheckBox[temp];
		temp = 0;
		for (int i=0; i<subgraphlist.size(); i++)
		{
			if (subgraphlist.elementAt(i)!=null) //Ein Knoten mit dem Index existiert
			{
				SubgraphChecks[temp] = new JCheckBox(graphref.getMathGraph().modifySubgraphs.get(i).getName());
				if (chEdge!=null)
					SubgraphChecks[temp].setSelected(graphref.getMathGraph().modifySubgraphs.get(i).containsEdge(chEdge.getIndex()));
				CiSubgraphs.add(SubgraphChecks[temp],c);
				SubgraphChecks[temp].addItemListener(this);
				c.gridy++;
				temp++; //Anzahl Knoten zaehlen
			}
		}
		iSubgraph = new JScrollPane(CiSubgraphs);
		iSubgraph.setAlignmentX(JScrollPane.LEFT_ALIGNMENT);
		iSubgraph.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		iSubgraph.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		iSubgraph.setPreferredSize(new Dimension(200,100));
	}
	/**
	 * Build the Tab with values to the corresponding VEdgeTypes
	 */
	private void buildEdgeRadioButtons()
	{
		cEdgeTypeField = new Container();
		cEdgeTypeField.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(3,3,3,3);
		c.anchor = GridBagConstraints.CENTER;
		c.gridy = 0;
		c.gridx = 0;

		Container cEdgeRadioField = new Container();
		cEdgeRadioField.setLayout(new GridBagLayout());
		c.insets = new Insets(3,3,3,3);
		c.anchor = GridBagConstraints.CENTER;
	
		gEdgeType = new ButtonGroup();
		
	    rStraightLine = new JRadioButton("gerade");
	    rStraightLine.addActionListener(this);
	    cEdgeRadioField.add(rStraightLine,c);
	    gEdgeType.add(rStraightLine);
	    
	    c.gridx++;

	    rQuadCurve = new JRadioButton("gebogen");
	    rQuadCurve.addActionListener(this);
	    cEdgeRadioField.add(rQuadCurve,c);
	    gEdgeType.add(rQuadCurve);
	    
	    c.gridx++;

	    rSegmented = new JRadioButton("segmentiert");
	    rSegmented.addActionListener(this);
	    cEdgeRadioField.add(rSegmented,c);
	    gEdgeType.add(rSegmented);
	    
	    c.gridx++;

	    rOrthogonal = new JRadioButton("orthogonal");
	    rOrthogonal.addActionListener(this);
	    cEdgeRadioField.add(rOrthogonal,c);
	    gEdgeType.add(rOrthogonal);	    
	    c.gridx=0;
	    c.gridwidth=1;
	    c.anchor = GridBagConstraints.CENTER;
	    cEdgeTypeField.add(cEdgeRadioField,c);
	    c.gridy++;
	    buildEdgeTypeParameterfields();
		c.insets = new Insets(0,15,0,0);
	    cEdgeTypeField.add(cStraightLine,c);
	    cEdgeTypeField.add(cQuadCurve,c);
	    cEdgeTypeField.add(cSegmented,c);
	    cEdgeTypeField.add(cOrthogonal,c);
		cLoop = new CLoopParameters(chEdge, graphref.getMathGraph().isDirected(),false); //without checks
		//Set Loop Field Width to the same width as the cEdgeTypefield width
		cLoop.setPreferredSize(new Dimension(cEdgeTypeField.getPreferredSize().width,cLoop.getPreferredSize().height));
	}
	/**
	 * Init visibility of the EdgeTypeParameterFields
	 *
	 */
	private void InitVisibility()
	{
		cQuadCurve.setVisible(false); 
		cSegmented.setVisible(false); 
		cOrthogonal.setVisible(false); 
		cStraightLine.setVisible(false);
		int type = VEdge.STRAIGHTLINE;
	    if (chEdge!=null)
	    	type = chEdge.getType();  
	   	switch(type)
	    {
			case	VEdge.QUADCURVE : //Wenns eine Bezierkante ist
			{
				rQuadCurve.setSelected(true);
				cQuadCurve.setVisible(true);
				break;
			}
			case	VEdge.SEGMENTED : //Wenns eine Bezierkante ist
			{
				rSegmented.setSelected(true); 
				cSegmented.setVisible(true);
				break;
			}
			case	VEdge.ORTHOGONAL : //Wenns eine Bezierkante ist
			{
				rOrthogonal.setSelected(true); 
				cOrthogonal.setVisible(true);
				break;
			}
			default	: 
			{ 
				rStraightLine.setSelected(true); 
				cStraightLine.setVisible(true);
				break;
			} //Sonst - Straightline	
		}
	   	this.validate();
	}
	/**
	 * Init ParameterFields of EdgeTypeFields
	 */
	private void buildEdgeTypeParameterfields()
	{
		Dimension prefSize = new Dimension(420,100);
		Dimension tfdim = new Dimension(100,20);
		//StraightLine Parameters (none)
		cStraightLine = new Container();
		cStraightLine.setPreferredSize(prefSize);
		
		//QuadCurveParameters (X,Y)
		cQuadCurve = new Container();
		cQuadCurve.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(3,3,3,3);
		c.gridy = 0;
		c.gridx = 0;
		c.anchor = GridBagConstraints.WEST;
		iQCx = new IntegerTextField(); iQCx.setPreferredSize(tfdim);
		cQuadCurve.add(new JLabel("Bezier-Punkt X"),c);
		c.gridx++;
		cQuadCurve.add(iQCx,c);
		c.gridy++; c.gridx=0;
		iQCy = new IntegerTextField(); iQCy.setPreferredSize(tfdim);
		cQuadCurve.add(new JLabel("Bezier-Punkt Y"),c);
		c.gridx++;
		cQuadCurve.add(iQCy,c);
		cQuadCurve.setPreferredSize(prefSize);
		if ((chEdge!=null)&&(chEdge.getType()==VEdge.QUADCURVE)) //Werte holen
		{
			Point p = ((VQuadCurveEdge) chEdge).getControlPoints().firstElement();
			iQCx.setValue(p.x);
			iQCy.setValue(p.y);
		}
		//Segmented Parameters (Add Point, remove specific Point)
		cSegmented = new Container();
		cSegmented.setLayout(new GridBagLayout());
		
		cSegmented.setPreferredSize(prefSize);
		c.gridy = 0;
		c.gridx = 0;
		c.gridheight=3;
		if ((chEdge!=null)&&(chEdge.getType()==VEdge.SEGMENTED))
				segmentpoints = chEdge.getControlPoints();
			else
				segmentpoints = new Vector<Point>();
		buildpointlist();
		lPoints = new JList(pointstrings);
		JScrollPane SlPoints = new JScrollPane(lPoints);
		SlPoints.setPreferredSize(new Dimension(90,90));
		cSegmented.add(SlPoints,c);
		c.gridheight = 1;
		c.gridy = 0; c.gridx = 1;
		cSegmented.add(new JLabel(" X  "),c);
		c.gridy = 0; c.gridx = 2;
		iSegx = new IntegerTextField(); iSegx.setPreferredSize(tfdim);
		cSegmented.add(iSegx,c);
		c.gridy = 1; c.gridx = 1;
		cSegmented.add(new JLabel(" Y  "),c);
		c.gridx = 2;
		iSegy = new IntegerTextField(); iSegy.setPreferredSize(tfdim);
		cSegmented.add(iSegy,c);
		c.gridx = 3; c.gridy=0; c.gridheight=2;
		bSegAdd = new JButton("<html>Punkt hinzuf"+main.CONST.html_ue+"gen</html>");
		bSegAdd.addActionListener(this);
		cSegmented.add(bSegAdd,c);
		c.gridy = 2; c.gridx=1; c.gridheight=1; c.gridwidth = 3;
		bSegRem = new JButton("selektierten Punkt entfernen");
		cSegmented.add(bSegRem,c);
		bSegRem.addActionListener(this);
		c.gridwidth = 1;
		//Orthogonal Parameters (which part first)
		cOrthogonal = new Container();
		cOrthogonal.setLayout(new GridBagLayout());
		c.gridy = 0;
		c.gridx = 0;
		bOrthVertFirst = new JCheckBox();
		cOrthogonal.add(new JLabel("vertikal vom Start entfernen "),c);
		c.gridx++;
		cOrthogonal.add(bOrthVertFirst,c);
		cOrthogonal.setPreferredSize(prefSize);
		if ((chEdge!=null)&&(chEdge.getType()==VEdge.ORTHOGONAL))
				bOrthVertFirst.setSelected(((VOrthogonalEdge) chEdge).getVerticalFirst());
	}
	/**
	 * Build the point list for an segmented edge
	 */
	private void buildpointlist()
	{
		pointstrings = new Vector<String>();
		pointstrings.setSize(segmentpoints.size());
		for (int i=0; i<segmentpoints.size(); i++)
		{
			if (segmentpoints.get(i)!=null)
			{
				Point p = segmentpoints.get(i);
				pointstrings.set(i,"("+p.x+","+p.y+")");
			}
		}
	}
	/**
	 * Get the Node index from the position in the nodelist
	 * @param pos Position in the Node List Array
	 * @return Index of the nide
	 */
	private int StringPos2Index(int pos)
	{
		int index=0;
		//Suche den #index Nr pos, der nicht 0 ist , 
		for (int i=0; i<=pos; i++)
		{
			if (i>0)
				index++;
			while(nodelist.elementAt(index)==null)
			{
				index++;
			}
		}
		return index;
	}
	/**
	 * Get the position of the node identified by the index
	 * @param index
	 * @return
	 */
	private int Index2StringPos(int index)
	{
		int pos = 0;
		for (int i=0; i<index; i++)
		{
			if (nodelist.elementAt(i)!=null) //Ein Knoten mit dem Index existiert
			{
				pos++;
			}
		}
		return pos;
	}
	/**
	 * Check the parameter fields to be valid and change the edge / add the edge to the graph
	 */
	private void Check()
	{
//		Test, ob die notwendigen Felder ausgefuellt sind, wobei die Knotendropdowns nicht geprüft werden muessen
		if ((iEdgeIndex.getValue()==-1)||(iWidth.getValue()==-1)||(iValue.getValue()==-1))
		{
			String message = new String();
			if (chEdge ==null)
				message = "<html><p>Erstellen der Kante nicht m"+main.CONST.html_oe+"glich.";
			else
				message = "<html><p>"+main.CONST.html_Ae+"ndern der Kante nicht m"+main.CONST.html_oe+"glich.";
			message+="<br><br>Einige Felder nicht ausgef"+main.CONST.html_ue+"llt.</p></hmtl>";
			JOptionPane.showMessageDialog(this,message, "Fehler", JOptionPane.ERROR_MESSAGE);
			return;
		}
		//Pruefen der Textfelder, falls aktiviert
		String t = cText.VerifyInput(EdgeName.getText());
		if (!t.equals(""))
		{
			JOptionPane.showMessageDialog(this,"<html><p>"+main.CONST.html_Ae+"ndern der Kante nicht mungen nicht m"+main.CONST.html_oe+"glich.<br><br>"+t+"</p></html>", "Fehler", JOptionPane.ERROR_MESSAGE);				
			return;
		}
		//Testen der Linienbedingungen, Felder die nicht ausgefüllt und gleichzeitig nicht benötigt werden, werden auch nicht gesetzt
		t = cLine.VerifyInput();
		if (!t.equals(""))
		{
			JOptionPane.showMessageDialog(this,"<html><p>"+main.CONST.html_Ae+"ndern der Kante nicht mungen nicht m"+main.CONST.html_oe+"glich.<br><br>"+t+"</p></html>", "Fehler", JOptionPane.ERROR_MESSAGE);				
			return;
		}
		
		int startindex = StringPos2Index(cStart.getSelectedIndex());
		int endindex = StringPos2Index(cEnd.getSelectedIndex());
		if (chEdge==null) //neuer Kante, index testen
		{
			//Index bereits vergeben ?
			if (graphref.modifyEdges.get(iEdgeIndex.getValue())!=null) //So einen gibt es schon
			{
				JOptionPane.showMessageDialog(this, "<html><p>Erstellen der Kante nicht m"+main.CONST.html_oe+"glich.<br><br>Der Index ist bereits vergeben.</p></hmtl>", "Fehler", JOptionPane.ERROR_MESSAGE);
				return;
			}
			VEdge typeedge = CheckType();
			if (typeedge==null)
			{
				return;
			}
			//Existiert schon eine Kante zwischen den beiden Knoten (ungerichtet) oder gar in die Richtung ?
			if (graphref.modifyEdges.getIndexWithSimilarEdgePath(typeedge, startindex, endindex) > 0) //ähnliche Kante existiert schon Doppelkanten darf es nicht geben
			{
				JOptionPane.showMessageDialog(this, "<html><p>Erstellen des Kante nicht m"+main.CONST.html_oe+"glich.<br><br>Eine Kante zwischen den Knoten existiert bereits.</p></hmtl>", "Fehler", JOptionPane.ERROR_MESSAGE);					
				return;
			}
			if (startindex==endindex)
			{
				if (!graphref.getMathGraph().isLoopAllowed()) //keine Schleifen erlaubt
				{	
					JOptionPane.showMessageDialog(this, "<html><p>Erstellen des Kante nicht m"+main.CONST.html_oe+"glich.<br><br>Schleifen sind in diesem Graphen nicht erlaubt.</p></hmtl>", "Fehler", JOptionPane.ERROR_MESSAGE);					
					return;
				}
				t = cLoop.VerifyInput();
				if (!t.equals(""))
				{
					JOptionPane.showMessageDialog(this, "<html><p>Erstellen des Kante nicht m"+main.CONST.html_oe+"glich.<br><br>"+t+"</p></hmtl>", "Fehler", JOptionPane.ERROR_MESSAGE);					
					return;					
				}
			}
		}
		else //Kantenaenderungsdialog
		{
//			Auswertung der neuen Daten, Pruefung auf Korrektheit
			//Falls sich der Kantenindex geaendert hat darf dieser nicht vergeben sein
			if ((graphref.modifyEdges.get(iEdgeIndex.getValue())!=null)&&(iEdgeIndex.getValue()!=oldindex)) //So einen gibt es schon
			{
				JOptionPane.showMessageDialog(this, "<html><p>"+main.CONST.html_Ae+"nderung der Kante nicht m"+main.CONST.html_oe+"glich.<br><br>Der Index ist bereits vergeben.</p></html>", "Fehler", JOptionPane.ERROR_MESSAGE);
				return;
			}
			//Falls sich Start oder Ende geaendert haben, darf keine Zweite Kante existieren
			if ((startindex!=oldstart)||(endindex!=oldend))//Kante wurde umgebaut
			{
				if (startindex==endindex)
				{
					if (!graphref.getMathGraph().isLoopAllowed())
					{
						JOptionPane.showMessageDialog(this, "<html><p>"+main.CONST.html_Ae+"ndern der Kante nicht m"+main.CONST.html_oe+"glich.<br><br>Eine Selbstkante (Start- und Endknoten identisch) sind deaktiviert.</p></hmtl>", "Fehler", JOptionPane.ERROR_MESSAGE);					
						return;
					}
					t = cLoop.VerifyInput();
					if (!t.equals(""))
					{
						JOptionPane.showMessageDialog(this, "<html><p>Erstellen des Kante nicht m"+main.CONST.html_oe+"glich.<br><br>"+t+"</p></hmtl>", "Fehler", JOptionPane.ERROR_MESSAGE);					
						return;					
					}
				}
			}
			if (CheckType()==null)
				return;
			if (graphref.modifyEdges.getIndexWithSimilarEdgePath(CheckType(), startindex,endindex) > 0) //do the actual input fields create an edge similar to an existing ?
			{
				if ((!graphref.getMathGraph().isMultipleAllowed())&&(graphref.modifyEdges.getIndexWithSimilarEdgePath(CheckType(), startindex,endindex) != oldindex)) //ist nicht die alte Kante
				{	
					JOptionPane.showMessageDialog(this, "<html><p>"+main.CONST.html_Ae+"ndern der Kante nicht m&ouml;glich.<br><br>Eine Kante zwischen den Knoten existiert bereits.</p></hmtl>", "Fehler", JOptionPane.ERROR_MESSAGE);					
					return;
				}
			}
			//Alles in Ordnung, aendern also loeschen
			//Als Block
			if (oldindex==iEdgeIndex.getValue()) //Index not changed -> Just an EdgeReplace
				graphref.pushNotify(new GraphMessage(GraphMessage.EDGE,oldindex,GraphMessage.UPDATE|GraphMessage.BLOCK_START,GraphMessage.EDGE));
			else
				graphref.pushNotify(new GraphMessage(GraphMessage.EDGE,GraphMessage.UPDATE|GraphMessage.BLOCK_START,GraphMessage.EDGE));
			graphref.modifyEdges.remove(oldindex);
		}
		//hinzufuegen
		VEdge addEdge = CheckType();
		 graphref.modifyEdges.add(
				 addEdge,
				 new MEdge(addEdge.getIndex(),startindex, endindex, iValue.getValue(),EdgeName.getText()),
				 graphref.modifyNodes.get(startindex).getPosition(),
				 graphref.modifyNodes.get(endindex).getPosition());
		//Gruppen einbauen
		int temp = 0;
		for (int i=0; i<subgraphlist.size(); i++)
		{
			if (subgraphlist.elementAt(i)!=null) //Ein Untergraph mit dem Index existiert
			{
				if (SubgraphChecks[temp].isSelected())
				{
					graphref.modifySubgraphs.addEdgetoSubgraph(iEdgeIndex.getValue(), i);
				}
				temp++; //Anzahl Knoten zaehlen
			}
		}
		//Text bauen
		VEdge e = graphref.modifyEdges.get(iEdgeIndex.getValue());
		e = cText.modifyEdge(e);
		e = cLine.modifyEdge(e);			
		if (chEdge!=null)//Change edge, end block
		{
			if (oldindex==iEdgeIndex.getValue()) //Index not changed -> Just an EdgeReplace
				graphref.pushNotify(new GraphMessage(GraphMessage.EDGE,oldindex,GraphMessage.BLOCK_END,GraphMessage.EDGE));
			else
				graphref.pushNotify(new GraphMessage(GraphMessage.EDGE,GraphMessage.BLOCK_END,GraphMessage.EDGE));
		}
		this.dispose();
	}
	/**
	 * Verify the EdgeType and its Parameter Fields
	 * @return a constructed subtype of VEDge to set further edge parameters in the check-method
	 */
	private VEdge CheckType()
	{
		VEdge retvalue=null;
		//Check des Arrows
		if (graphref.getMathGraph().isDirected())
		{
			String t = cArrow.VerifyInput();
			if (!t.equals(""))
			{
				JOptionPane.showMessageDialog(this, "<html><p>"+main.CONST.html_Ae+"ndern der Kante nicht m"+main.CONST.html_oe+"glich.<br><br>Keine Pfeilspitzengr"+main.CONST.html_oe+""+main.CONST.html_sz+"e angegeben.</p></hmtl>", "Fehler", JOptionPane.ERROR_MESSAGE);					
				//evtl fragen ob man die beiden Kanten zusammenlegen soll
				return null;
			}
			//Die anderen beiden Werte sind stets gegeben
		}
		int start = StringPos2Index(cStart.getSelectedIndex());
		int end = StringPos2Index(cEnd.getSelectedIndex());
		if ((start==end)&&(graphref.getMathGraph().isLoopAllowed()))
		{
			retvalue = cLoop.createEdge(iEdgeIndex.getValue(), iWidth.getValue());
		}
		else if (rStraightLine.isSelected())
			{
				//nichts zu pruefen, nur bauen
				retvalue = new VStraightLineEdge(iEdgeIndex.getValue(),iWidth.getValue()); //keine Pr�fung notwendig 
			}
			else if (rQuadCurve.isSelected())
			{
				if ((iQCx.getValue()==-1)||(iQCy.getValue()==-1)) //eins nicht ausgefüllt
				{
						JOptionPane.showMessageDialog(this, "<html><p>"+main.CONST.html_Ae+"ndern der Kante nicht m"+main.CONST.html_oe+"glich.<br><br>Der Kontrollpunkt (Bezier Punkt) wurde nicht vollst&auml;ndig angegeben.</p></hmtl>", "Fehler", JOptionPane.ERROR_MESSAGE);					
						return null;
				}
				retvalue = new VQuadCurveEdge(iEdgeIndex.getValue(),iWidth.getValue(),new Point(iQCx.getValue(),iQCy.getValue()));
			}	
			else if (rSegmented.isSelected())
			{
				if (segmentpoints.size()==0)
				{
					JOptionPane.showMessageDialog(this, "<html><p>"+main.CONST.html_Ae+"ndern der Kante nicht m"+main.CONST.html_oe+"glich.<br><br>Bitte mindestens einen Wegpunkt angeben!.</p></hmtl>", "Fehler", JOptionPane.ERROR_MESSAGE);					
					return null;
				}
				retvalue = new VSegmentedEdge(iEdgeIndex.getValue(),iWidth.getValue(),segmentpoints);
			}
			else if (rOrthogonal.isSelected())
			{
				//Kein Test nötig
				retvalue = new VOrthogonalEdge(iEdgeIndex.getValue(), iWidth.getValue(),bOrthVertFirst.isSelected());
			}
		if (retvalue!=null)
		{
			if (graphref.getMathGraph().isDirected()) //Pfeildaten setzen
			{
				retvalue = cArrow.modifyEdge(retvalue);
			}
			//If Edge exists and is selected
			if ((chEdge!=null)&&((chEdge.getSelectedStatus() & VItem.SELECTED) == VItem.SELECTED))
				retvalue.setSelectedStatus(VItem.SELECTED);
		}
		return retvalue;
	}
	
	public void actionPerformed(ActionEvent event)
	{
		if (event.getSource()==rStraightLine)
		{
			cStraightLine.setVisible(true);
			cQuadCurve.setVisible(false);
			cSegmented.setVisible(false);
			cOrthogonal.setVisible(false);
		}
		else if (event.getSource()==rQuadCurve)
		{
			cStraightLine.setVisible(false);
			cQuadCurve.setVisible(true);
			cSegmented.setVisible(false);
			cOrthogonal.setVisible(false);
			this.invalidate();
		}
		else if (event.getSource()==rSegmented)
		{
			cStraightLine.setVisible(false);
			cQuadCurve.setVisible(false);
			cSegmented.setVisible(true);
			cOrthogonal.setVisible(false);
		}
		else if (event.getSource()==rOrthogonal)
		{
			cStraightLine.setVisible(false);
			cQuadCurve.setVisible(false);
			cSegmented.setVisible(false);
			cOrthogonal.setVisible(true);
		}
		else if (event.getSource()==bSegAdd)
		{
			if ((iSegx.getValue()!=-1)&&(iSegy.getValue()!=-1))
			{
				Point p = new Point(iSegx.getValue(),iSegy.getValue());
				for (int i=0; i<segmentpoints.size(); i++)
					if ((segmentpoints.get(i).x==p.x)&&(segmentpoints.get(i).y==p.y))
						return; //Doppelt eintragen ist nich
				
				segmentpoints.add(p);
				buildpointlist();
				lPoints.setListData(pointstrings);
			}
		}
		else if (event.getSource()==bSegRem)
		{
			String sel = (String) lPoints.getSelectedValue();
			//Lösche alle selektierten einträge
			if (sel==null) //nichts selektiert
				return;
			segmentpoints.remove(pointstrings.indexOf(sel));
			int selIndex = pointstrings.indexOf(sel);
			buildpointlist();
			lPoints.setListData(pointstrings);
			if (pointstrings.size()>0)
			{
				if (selIndex==pointstrings.size()+1)
					lPoints.setSelectedIndex(pointstrings.size());
				else
					lPoints.setSelectedIndex(selIndex);
			}
			
		}
		else if ((event.getSource()==this.cEnd)||(event.getSource()==cStart))
		{
			int start = StringPos2Index(cStart.getSelectedIndex());
			int end = StringPos2Index(cEnd.getSelectedIndex());
			if (tabs.getTabCount() > 2)
			{
				if ((start==end)&&(graphref.getMathGraph().isLoopAllowed()))
				{
					tabs.setComponentAt(2,cLoop);
					tabs.setTitleAt(2," Schleife ");
				}
				else
				{
					tabs.setComponentAt(2,cEdgeTypeField);
					tabs.setTitleAt(2,"Kantentyp");
				}
			}
		}
		else if (event.getSource()==bCancel)
		{
			this.dispose();
		}
		else if (event.getSource()==bOK)
		{
			Check();
		}
	}
	
	public void itemStateChanged(ItemEvent event) 
	{		
		for (int i=0; i<SubgraphChecks.length; i++)
		{
			if (event.getSource()==SubgraphChecks[i])
			{
				//Ein Zustand hat sich geändert, neue Farbe berechnen
				Color colour = Color.BLACK;
				int colourcount = 0;
				int temp = 0; //zum mitzaehlen
				for (int j=0; j<subgraphlist.size();j++)
				{
					if (subgraphlist.elementAt(j)!=null)
					{
						if (SubgraphChecks[temp].isSelected())
						{
							Color newc = graphref.modifySubgraphs.get(j).getColor();
							int b=colour.getBlue()*colourcount + newc.getBlue();
							int a=colour.getAlpha()*colourcount + newc.getAlpha();
							int g=colour.getGreen()*colourcount + newc.getGreen();
							int r=colour.getRed()*colourcount + newc.getRed();
							colourcount++;
							colour = new Color((r/colourcount),(g/colourcount),(b/colourcount),(a/colourcount));
						}
							temp++;
					}
				}
				Colorfield.setBackground(colour);
				return;
			}
		}
	}
}