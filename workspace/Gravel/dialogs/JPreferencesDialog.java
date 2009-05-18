package dialogs;


import io.GeneralPreferences;

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
import java.awt.event.KeyListener;


import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.ButtonGroup;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import dialogs.components.*;

import model.VEdge;
import model.VLoopEdge;
import model.VNode;
import model.VStraightLineEdge;

import view.Gui;
import view.pieces.GridComponent;
/**
 * Dialog for changing the General Preferences, and check its validity
 * @author Ronny Bergmann
 *
 */public class JPreferencesDialog extends JDialog implements ActionListener, ItemListener, ChangeListener, CaretListener, KeyListener
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 42L;
	private GeneralPreferences gp;
	private IntegerTextField iNodeSize, iEdgeWidth, iEdgeValue, iControlPointSize, iUndoStacksize;
	private JTextField tNodeName, tSubgraphName, tEdgeName;
	private JLabel tNodePreview, tSubgraphPreview, tEdgePreview;
	private JCheckBox bControlPoint,bSaveOnExit,bAllowLoops, bAllowMultiple, bLoadLastGraphOnStart, bUndotrackSelection;
	private ButtonGroup GraphType;
	private JRadioButton rDirected, rUndirected;
	private GridComponent grid;
	private JButton bOK, bCancel,bSave,bRestore,bLoad;
	private JLabel prefStatus;
	
	CNodeNameParameters cNodeName;
	
	//Arrow
	private CEdgeArrowParameters cArrow;
	//Arrow Preview 
	private CEdgeArrowPreview cArrowPreview;
	//Text
	private CEdgeTextParameters cText;
	//Line
	private CEdgeLineParameters cLine;
	
	private JTabbedPane tabs;
	
	//All together in an Array of Tab
	Container[] edgesubs;
	String[] edgesubtexts;
	
	//Loop Parameters
	CLoopParameters cLoop;
	
	/**
	 * Open an initialize the dialog
	 */
	public JPreferencesDialog()
	{
		gp = GeneralPreferences.getInstance();
		CreateDialog("   ");
	}
	/**
	 * Init the Dialog and call all the tab-initializations
	 * @param status
	 */
	private void CreateDialog(String status)
	{
		setTitle("Einstellungen");
		prefStatus = new JLabel(status);
		tabs = new JTabbedPane();
		String[] nametemp = {"Pfeil","Beschriftung","Linienstil","Schleifen"};
		edgesubtexts = nametemp;
		tabs.setTabPlacement(JTabbedPane.TOP);
		tabs.addTab("Allgemein", buildPropPane());
		tabs.addTab("Ansicht", buildViewPane());
		tabs.addTab("Graph", buildGraphPane());
		tabs.addTab("Knoten",buildNodePane());
		tabs.addTab("Kante", buildEdgePane());
		Container ContentPane = this.getContentPane();
		ContentPane.removeAll();
		ContentPane.setLayout(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(7,7,7,7);
		c.anchor = GridBagConstraints.WEST;
	
		c.gridy=0;c.gridx=0;c.gridwidth=2;
		ContentPane.add(tabs,c);
		c.gridy++;c.gridx = 0;c.gridwidth=1;
		c.anchor = GridBagConstraints.WEST;
		
		bCancel = new JButton("Abbrechen");
		c.insets = new Insets(3,3,3,3);
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
		bOK = new JButton("Ok");
		bOK.addActionListener(this);
		ContentPane.add(bOK,c);
		
		fillValues();
		
		this.getRootPane().setDefaultButton(bOK);
		setResizable(false);
		this.setModal(true);
		pack();

		Point p = new Point(0,0);
		p.y += Math.round(Gui.getInstance().getParentWindow().getHeight()/2);
		p.x += Math.round(Gui.getInstance().getParentWindow().getWidth()/2);
		p.y -= Math.round(getHeight()/2);
		p.x -= Math.round(getWidth()/2);

		setLocation(p.x,p.y);
		this.setVisible(true);
	}
	/**
	 * Fill all fields with the GeneralPreferences-Values after they are initialized
	 */
	private void fillValues()
	{
		bSaveOnExit.setSelected(gp.getBoolValue("pref.saveonexit"));
		bLoadLastGraphOnStart.setSelected(gp.getBoolValue("graph.loadfileonstart"));
		bUndotrackSelection.setSelected(gp.getBoolValue("history.trackSelection"));
		iUndoStacksize.setValue(gp.getIntValue("history.Stacksize"));
		
		rDirected.setSelected(gp.getBoolValue("graph.directed"));
		rUndirected.setSelected(!gp.getBoolValue("graph.directed"));
		bAllowLoops.setSelected(gp.getBoolValue("graph.allowloops"));
		bAllowMultiple.setSelected(gp.getBoolValue("graph.allowmultiple"));
		
		iControlPointSize.setValue(gp.getIntValue("vgraphic.cpsize"));
		bControlPoint.setSelected(gp.getBoolValue("vgraphic.cpshow"));
		iControlPointSize.setEnabled(bControlPoint.isSelected());
		
		iEdgeWidth.setValue(gp.getIntValue("edge.width"));
		iEdgeValue.setValue(gp.getIntValue("edge.value"));
		tEdgeName.setText(gp.getStringValue("edge.name"));
			
		tNodeName.setText(gp.getStringValue("node.name"));
		iNodeSize.setValue(gp.getIntValue("node.size"));	
				
		tSubgraphName.setText(gp.getStringValue("subgraph.name"));
		
		grid.reload();
		
		validate();
	}
	/**
	 * Build the main Tab with its parameter fields and
	 * @return return the JPanel conaitning all fields
	 */
	private JPanel buildGraphPane()
	{
		JPanel content = new JPanel();
		content.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(7,7,7,7);
		
		
		
		
		c.gridx = 0; c.gridy = 0;
		c.gridwidth=3; c.anchor = GridBagConstraints.CENTER;
		content.add(new JLabel("<html>Graph<</html>"),c);

		c.gridx = 0; c.gridy++;c.gridwidth=2; 
		c.anchor = GridBagConstraints.WEST;
		GraphType = new ButtonGroup();
		content.add(new JLabel("Neue Graphen erstellen mit den Eigenschaften"),c);
		
		c.gridx = 0; c.gridy++;c.gridwidth=1; 
		rDirected = new JRadioButton("gerichtet");
		rUndirected = new JRadioButton("ungerichtet");
		GraphType.add(rDirected); GraphType.add(rUndirected);
		content.add(rDirected,c); c.gridx++; content.add(rUndirected,c);
	
		c.gridx = 0; c.gridy++; c.gridwidth = 2;
		bAllowLoops = new JCheckBox("Selbstschleifen zulassen");
		content.add(bAllowLoops,c);
		
		c.gridx = 0; c.gridy++; c.gridwidth = 2;
		bAllowMultiple = new JCheckBox("Mehrfachkanten zulassen");
		content.add(bAllowMultiple,c);
		
		c.gridy++;
		c.gridx=0; c.gridwidth=2; c.anchor = GridBagConstraints.EAST;
		content.add(prefStatus,c);

		JSeparator separator = new JSeparator(JSeparator.HORIZONTAL);
		c.gridx = 0; c.gridy++;c.gridwidth=2; 
		content.add(separator,c);

		c.gridy++; c.gridx=0;
		c.gridwidth=3; c.anchor = GridBagConstraints.CENTER;
		content.add(new JLabel("<html>Untergraph</html>"),c);

		c.anchor = GridBagConstraints.EAST;
		c.gridy++; c.gridx=0;
		c.gridwidth=1;
		content.add(new JLabel("Name"),c);
		tSubgraphName = new JTextField();
		tSubgraphName.addCaretListener(this);
		c.gridx++;
		c.gridwidth=2;
		content.add(tSubgraphName,c);
		tSubgraphPreview = new JLabel();
		c.insets = new Insets(0,14,7,0);
		c.gridy++; content.add(tSubgraphPreview,c);
		tSubgraphName.setPreferredSize(new Dimension(200, 20));
		c.gridy++;
		c.gridx=0;
		c.insets = new Insets(7,7,7,7);

		return content;
	}
	/**
	 * Build the View-Properties and
	 * @return return the JPanel containing all Parameter Fields
	 */
	private JPanel buildViewPane()
	{
		JPanel content = new JPanel();
		content.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(7,7,7,7);
		c.anchor = GridBagConstraints.WEST;
		
		c.gridx = 0; c.gridy = 0;

		bControlPoint = new JCheckBox("Zeige Kantenkontrollpunkte");
		content.add(bControlPoint,c);

		iControlPointSize = new IntegerTextField();
		iControlPointSize.setPreferredSize(new Dimension(150, 20));
		c.gridy++;
		c.gridx=0;
		content.add(new JLabel("<html>Kontrollpunktgr"+main.CONST.html_oe+""+main.CONST.html_sz+"e</html>"),c);
		c.gridx++;
		content.add(iControlPointSize,c);
		c.gridx = 0; c.gridy++; c.gridwidth=2;
		c.insets = new Insets(30,0,0,0);
		grid = new GridComponent();
		content.add(grid.getContent(),c);

		bControlPoint.addActionListener(this);
		
		return content;
	}
	/**
	 * Initialize all Node Standard Values
	 * @return and return the JPanel
	 */
	private JPanel buildNodePane()
	{
		JPanel content = new JPanel();
		content.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(7,7,7,7);
		c.anchor = GridBagConstraints.WEST;
		
		c.gridx = 0; c.gridy = 0; 
		content.add(new JLabel("Name"),c);
		tNodeName = new JTextField();
		tNodeName.addCaretListener(this);
		c.gridx++;
		content.add(tNodeName,c);
		tNodePreview = new JLabel();
		c.insets = new Insets(0,14,7,0);	c.gridy++; content.add(tNodePreview,c);
		tNodeName.setPreferredSize(new Dimension(150, 20));
		c.gridy++;c.gridx=0; 		c.insets = new Insets(7,7,7,7);
		content.add(new JLabel("<html>Gr"+main.CONST.html_oe+""+main.CONST.html_sz+"e</html>"),c);
		c.gridx++;
		iNodeSize = new IntegerTextField();
		iNodeSize.setPreferredSize(new Dimension(150, 20));
		content.add(iNodeSize,c);
		c.gridx=0; c.gridy++; c.gridwidth=2;
		cNodeName = new CNodeNameParameters(null,false);
		content.add(cNodeName,c);
		return content;
	}
	/**
	 * Create the Subgraph-Stanardvalues Parameter Tab
	 * @return and return the JPanel
	 */
	/**
	 * Build the Edge JPanel containing all Edge Standard Value and
	 * @return return the JPanel
	 */
	private JPanel buildEdgePane()
	{
		JPanel content = new JPanel();
		content.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(7,7,7,7);
		c.anchor = GridBagConstraints.WEST;
		
		c.gridx = 0; c.gridy = 0; 
		content.add(new JLabel("Breite"),c);
		iEdgeWidth = new IntegerTextField();
		c.gridx++;
		content.add(iEdgeWidth,c);
		iEdgeWidth.setPreferredSize(new Dimension(150, 20));
		iEdgeWidth.addCaretListener(this);
		c.gridy++;
		c.gridx=0;
		content.add(new JLabel("<html>Wert</html>"),c);
		c.gridx++;
		iEdgeValue = new IntegerTextField();
		iEdgeValue.setPreferredSize(new Dimension(150, 20));
		content.add(iEdgeValue,c);
		
		c.gridx =0; c.gridy++; 
		content.add(new JLabel("Name"),c);
		tEdgeName = new JTextField();
		tEdgeName.addCaretListener(this);
		c.gridx++;
		content.add(tEdgeName,c);
		tEdgePreview = new JLabel();
		c.insets = new Insets(0,14,7,0);
		c.gridy++; content.add(tEdgePreview,c);
		tEdgeName.setPreferredSize(new Dimension(150, 20));
		
		c.gridy++;
		c.gridx = 0; c.gridwidth = 2;
		cArrowPreview = new CEdgeArrowPreview(null);

		edgesubs = new Container[edgesubtexts.length];
		cArrow = new CEdgeArrowParameters(null,false);
		cArrow.addObserver(cArrowPreview);
		edgesubs[0] = cArrow.getContent();
		cText = new CEdgeTextParameters(null,true,false);
		cText.addObserver(cArrowPreview);
		edgesubs[1] = cText.getContent();
		cLine = new CEdgeLineParameters(null,true,false);
		cLine.addObserver(cArrowPreview);
		edgesubs[2] = cLine.getContent();
		
		cLoop = new CLoopParameters(null,true,false);
		edgesubs[3] = cLoop;
		JTabbedPane edgesubtab = new JTabbedPane();
		for (int i=0; i<edgesubs.length; i++)
			edgesubtab.addTab(edgesubtexts[i],edgesubs[i]);
		content.add(edgesubtab,c);
		c.gridwidth = 2;
		c.gridy++;
		c.insets = new Insets(0,0,0,0);
		c.anchor = GridBagConstraints.NORTH;
		content.add(cArrowPreview,c);
		return content;
	}
	
	private JPanel buildPropPane() {
		JPanel content = new JPanel();
		content.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(7,7,7,7);
		c.anchor = GridBagConstraints.WEST;
		
		c.gridx = 0; c.gridy = 0;
		c.gridwidth = 2;
		content.add(new JLabel("<html>Alle Einstellungen sind Standardwerte, die als<br>Vorgabe beim Erstellen neuer Elemente genutzt werden."),c);
		c.gridx = 0; c.gridy++;c.gridwidth=1;
		
		content.add(new JLabel("Letzte gespeicherte Werte laden"),c);
		c.gridx++;
		bLoad = new JButton("Laden");
		bLoad.addActionListener(this);
		content.add(bLoad,c);
		
		c.gridx = 0; c.gridy++;
		content.add(new JLabel("Anfangswerte laden"),c);
		c.gridx++;
		bRestore = new JButton("<html>Zur"+main.CONST.html_ue+"cksetzen</html>");
		bRestore.addActionListener(this);
		content.add(bRestore,c);
		
		c.gridx = 0; c.gridy++;
		content.add(new JLabel("Werte speichern"),c);
		c.gridx++;
		bSave = new JButton("Speichern");
		bSave.addActionListener(this);
		content.add(bSave,c);	
		
		c.gridx = 0; c.gridy++;c.gridwidth=2; 
		bSaveOnExit  = new JCheckBox("Einstellungen bei Beenden des Programms speichern");		
		content.add(bSaveOnExit,c);

		c.gridx = 0; c.gridy++;c.gridwidth=2; 
		bLoadLastGraphOnStart = new JCheckBox("Beim Start letzten gespeicherten Graphen laden");
		content.add(bLoadLastGraphOnStart,c);

		JSeparator separator = new JSeparator(JSeparator.HORIZONTAL);
		c.gridx = 0; c.gridy++;c.gridwidth=2; 
		content.add(separator,c);
		
		c.gridx = 0; c.gridy++;c.gridwidth=2; 
		content.add(new JLabel("<html>Die Einstellungen f"+main.CONST.html_ue+"r die Widerrufen/Wiederholen-Aktionen<br>werden erst auf einen neuen oder geladenen Graphen aktiv."),c);
		c.gridx = 0; c.gridy++;c.gridwidth=1; 
		content.add(new JLabel("<html>Anzahl widerrufbarer Aktionen<br><font size=-2>Experimentiereinstellung</font>"),c);
		iUndoStacksize = new IntegerTextField();
		c.gridx++;
		content.add(iUndoStacksize,c);
		iUndoStacksize.setPreferredSize(new Dimension(100, 20));
		c.gridx = 0; c.gridy++;c.gridwidth=2; 
		bUndotrackSelection = new JCheckBox("<html>Ver"+main.CONST.html_ae+"nderungen der Auswahl aufzeichnen<br><font size=-2>Durch Aufzeichnung k"+main.CONST.html_oe+"nnen diese widerrufen werden.</font></html>");
		content.add(bUndotrackSelection,c);

		return content;
	}

	/**
	 * Check all Parameter Fields and 
	 * 
	 * @return return an occuring error, if there is no error an empty String is returned
	 */
	private String check()
	{
		if (iUndoStacksize.getValue()==-1)
			return "Die Anzahl Aktionen, die Widerrufen werden k"+main.CONST.html_oe+"nnen, ist nicht angegeben";
		if (bControlPoint.isSelected()&&(iControlPointSize.getValue()==-1))
		{
			return "<html>Die Kantenkontrollpunktgr"+main.CONST.html_oe+""+main.CONST.html_sz+"e ist fehlerhaft</html>";
		}
		if (iNodeSize.getValue()==-1)
			return "<html>Bitte eine Knotengr"+main.CONST.html_oe+""+main.CONST.html_sz+"e angeben</html>";

		String t = cNodeName.VerifyInput();
		if (!t.equals(""))
		return "<html><p>"+"</p></html>";
				
		//Checking Edge-Panel
		if (iEdgeWidth.getValue()==-1)
			return "Bitte eine Kantenbreite angeben.";
		if (iEdgeValue.getValue()==-1)
			return "Bitte einen Kantenwert angeben.";
		t = cArrow.VerifyInput();
		if (!t.equals(""))
			return "<html><p>"+"</p></html>";
		
		t = cText.VerifyInput(tEdgeName.getText());
		if (!t.equals(""))
			return "<html><p>Bitte bei den Standardwerten für den Kantentext die Werte vervollst"+main.CONST.html_ae+"ndigen<br><br>"+t+"</p></html>";
		t = cLine.VerifyInput();
		if (!t.equals(""))
			return "<html><p>Bitte bei den Standardwerten für die Kantenline die Werte vervollst"+main.CONST.html_ae+"ndigen<br><br>"+t+"</p></html>";
			
		t = cLoop.VerifyInput();
		if (!t.equals(""))
			return "<html><p>Bitte bei den Standardwerten für Schleifen die Werte vervollst"+main.CONST.html_ae+"ndigen<br><br>"+t+"</p></html>";
		
		return "";
		
	}
	/**
	 * Write the Parameter Field Values to the preferences file
	 *
	 */
	private void formtogp()
	{
		if (!check().equals(""))
			return;
		VEdge chEdge = cArrow.modifyEdge(new VStraightLineEdge(1,iEdgeWidth.getValue()));
		chEdge = cText.modifyEdge(chEdge);
		chEdge = cLine.modifyEdge(chEdge);
		gp.setIntValue("edge.arralpha",Math.round(chEdge.getArrow().getAngle()));
		gp.setIntValue("edge.arrpart",Math.round(chEdge.getArrow().getPart()*100));
		gp.setIntValue("edge.arrsize", Math.round(chEdge.getArrow().getSize()));
		gp.setIntValue("edge.arrpos", Math.round(chEdge.getArrow().getPos()*100));
		gp.setIntValue("edge.value", iEdgeValue.getValue());
		gp.setIntValue("edge.width", iEdgeWidth.getValue());
		gp.setStringValue("edge.name", tEdgeName.getText());
		
		
		VLoopEdge vle = cLoop.createEdge(1,1);
		gp.setIntValue("edge.looplength",vle.getLength());
		gp.setIntValue("edge.loopproportion",(new Double(vle.getProportion()*100.0d)).intValue());
		gp.setIntValue("edge.loopdirection",vle.getDirection());
		gp.setBoolValue("edge.loopclockwise",vle.isClockwise());
		
		
		gp.setBoolValue("grid.enabled", this.grid.isEnabled());
		gp.setBoolValue("grid.synchron", grid.isSynchron());
		gp.setIntValue("grid.x", grid.getGridX());
		gp.setIntValue("grid.y", grid.getGridY());
		
		gp.setBoolValue("history.trackSelection", bUndotrackSelection.isSelected());
		gp.setIntValue("history.Stacksize", iUndoStacksize.getValue());
		
		gp.setStringValue("node.name", tNodeName.getText());
		gp.setIntValue("node.size", iNodeSize.getValue());
		
		VNode n = new VNode(0,0,0,0,0,0,0,false);
		n = cNodeName.modifyNode(n);
		gp.setIntValue("node.name_size",n.getNameSize());
		gp.setIntValue("node.name_rotation",n.getNameRotation());
		gp.setIntValue("node.name_distance",n.getNameDistance());
		gp.setBoolValue("node.name_visible",n.isNameVisible());
		
		gp.setBoolValue("graph.directed", this.rDirected.isSelected());
		gp.setBoolValue("graph.allowloops", bAllowLoops.isSelected());
		gp.setBoolValue("graph.allowmultiple", bAllowMultiple.isSelected());
		
		gp.setBoolValue("pref.saveonexit", this.bSaveOnExit.isSelected());
		gp.setBoolValue("graph.loadfileonstart",bLoadLastGraphOnStart.isSelected());			
		gp.setStringValue("subgraph.name", tSubgraphName.getText());
		
		gp.setBoolValue("vgraphic.cpshow", this.bControlPoint.isSelected());
		gp.setIntValue("vgraphic.cpsize", this.iControlPointSize.getValue());
		
		gp.setBoolValue("edge.text_visible", chEdge.getTextProperties().isVisible());
		gp.setIntValue("edge.text_position",chEdge.getTextProperties().getPosition());
		gp.setIntValue("edge.text_distance",chEdge.getTextProperties().getDistance());
		gp.setIntValue("edge.text_size",chEdge.getTextProperties().getSize());
		gp.setBoolValue("edge.text_showvalue",chEdge.getTextProperties().isshowvalue());
		
		//Line Style stuff
		gp.setIntValue("edge.line_type",chEdge.getLinestyle().getType());
		gp.setIntValue("edge.line_distance",chEdge.getLinestyle().getDistance());
		gp.setIntValue("edge.line_length", chEdge.getLinestyle().getLength());
	}
	
	public void actionPerformed(ActionEvent event)
	{
		if (event.getSource()==bControlPoint)
		{
			iControlPointSize.setEnabled(bControlPoint.isSelected());
		}
		if (event.getSource()==bCancel)
		{
			this.dispose();
		}
		else if (event.getSource()==bLoad)
		{
			gp.readXML();
			if (gp.check())
			{
				//CreateDialog("Einstellungen geladen.");
				prefStatus.setText("Einstellungen geladen.");
				fillValues();
			}
			else
				prefStatus.setText("Die XML Datei ist feherlhaft!");
		}
		else if (event.getSource()==bRestore)
		{
			gp.resettoDefault();
		//	CreateDialog("<html>Einstellungen zur&uuml;ckgesetzt</html>");
			prefStatus.setText("<html>Einstellungen zur"+main.CONST.html_ue+"ckgesetzt</html>");
			fillValues();
		}
		else if (event.getSource()==bSave)
		{
			if (check().equals(""))
			{
				this.formtogp();
				gp.writetoXML();			
				prefStatus.setText("Einstellungen gespeichert.");
			}
			else
			{
				JOptionPane.showMessageDialog(this,
				    check(),
				    "Fehler",
				    JOptionPane.ERROR_MESSAGE);
			}
		}
		else if (event.getSource()==bOK)
		{
			if (check().equals("")) //Alle Felder checken und in gp eintragen
			{
				this.formtogp();
				this.dispose();
			}
			else
			{
				JOptionPane.showMessageDialog(this,
					    check(),
					    "Fehler",
					    JOptionPane.ERROR_MESSAGE);
			}
		}
		else
		{}
	}
	
	public void itemStateChanged(ItemEvent event) {}

	public void stateChanged(ChangeEvent e) {}
	
	public void caretUpdate(CaretEvent event) {
/*		else if (event.getSource()==iEdgeWidth)
		{
			if ((iEdgeWidth.getValue()>0)&&(iEdgeWidth.getValue()!=ArrowEdge.getWidth()))
			{ //Wert geändert
				ArrowEdge.setWidth(iEdgeWidth.getValue());
				ArrowG.repaint();
			}
		}
		else if (event.getSource()==iLineDistance)
		{
			if ((iLineDistance.getValue()!=-1)&&(iLineDistance.getValue()!=ArrowEdge.getLinestyle().getDistance()))
			{
				ArrowEdge.getLinestyle().setDistance(iLineDistance.getValue());
				ArrowG.repaint();
			}
		}
		else if (event.getSource()==iLineLength)
		{
			if ((iLineLength.getValue()!=-1)&&(iLineLength.getValue()!=ArrowEdge.getLinestyle().getLength()))
					{
						ArrowEdge.getLinestyle().setLength(iLineLength.getValue());
						ArrowG.repaint();
					}
		}*/
		if (event.getSource()==tNodeName)
		{
			String t = GeneralPreferences.replace(tNodeName.getText(),"$ID","4");
			if (t.length() > 10)
				t = t.substring(0,10)+"...";	
			tNodePreview.setText("<html><font size=-1>Vorschau: <i>"+t+"</i></font></html>");
			tNodePreview.validate();
		}
		else if (event.getSource()==tEdgeName)
		{
			String t = tEdgeName.getText();
			t = GeneralPreferences.replace(t,"$ID","4");
			t = GeneralPreferences.replace(t,"$SID","1");
			t = GeneralPreferences.replace(t,"$EID","2");
			if (t.length() > 10)
				t = t.substring(0,10)+"...";
			tEdgePreview.setText("<html><font size=-1>Vorschau: <i>"+t+"</i></font></html>");
			tEdgePreview.validate();
		}
		else if (event.getSource()==tSubgraphName)
		{
			String t = GeneralPreferences.replace(tSubgraphName.getText(),"$ID","4"); 
			if (t.length() > 15)
				t = t.substring(0,15)+"...";
			tSubgraphPreview.setText("<html><font size=-1>Vorschau: <i>"+t+"</i> </font></html>");
		} 
	}
	public void keyPressed(KeyEvent e) {}
	public void keyReleased(KeyEvent e) {}
	public void keyTyped(KeyEvent e) { //Handle ESC on Cancel
	        if (e.getKeyChar() == KeyEvent.VK_ESCAPE)
	            bCancel.doClick();
	}
}
