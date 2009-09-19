package view;

import history.CommonGraphHistoryManager;
import io.GeneralPreferences;
import io.GraphMLReader;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.TreeMap;

import javax.swing.*;

import dialogs.JFileDialogs;



import model.MHyperEdge;
import model.VGraph;
import model.VGraphInterface;
import model.VHyperGraph;
import model.Messages.GraphConstraints;
import model.Messages.GraphMessage;

/**
 * 	Hauptklasse der GUI, stellt das Hauptfenster zur Verfügung mittels Singelton-Muster.
 * 	Enth&auml;lt die Grafikumgebung, die Liste sowie die Statistik des Graphen. Au&szlig;erdem die Toolbar und das Men&uuml;  
 *
 *	@author Ronny Bergmann
 */
public class Gui implements WindowListener
{
	public static final String WindowName = "Gravel "+main.CONST.version;
	
	//Global der Graph als Datensatz
	private VGraphInterface MainGraph;
	//Der MainContentFrame
	private JFrame frame;
	private JPanel mainPanel;
	private VCommonGraphic graphpart; 
	private Statistics stats;
	private GraphTree graphlist;
	private MainToolbar gToolBar;
	private MainMenu MenuBar;
	private static Gui instance;
	private GeneralPreferences gp;
	private VHyperShapeGraphic shapePart= null;
	private HyperEdgeShapePanel shapeParameters = null;
	private JScrollPane mainScroll, treeScroll, shapeScroll;
	private JSplitPane mainSplit;
	/**
	 * Returns the GUI, if it exists, if it does not exists, an instance is creataed
	 * @return the reference to the GUI
	 */
	public synchronized static Gui getInstance()
	{		
		if (instance == null)
		{
			instance = new Gui(null);
		}
		return instance;	
	}
	/**
	 * Returns the GUI if it exists and creates one, if it doesn't
	 * this method also starts the gui with a file if it exists
	 */
	public synchronized static Gui getInstance(File f)
	{		
		if (instance == null)
		{
			instance = new Gui(f);
		}
		else
			instance.setFile(f);
		return instance;	
	}

	/**
	 * Konstruktor der GUI, erstellt das Fenster, initialisiert Komponenten
	 * <br>Aufgrund des Singelton-Musters ist der Konstruktor private
	 *
	 */
	private Gui(File f) 
	{
		//Create and set up the window.
        frame = new JFrame(WindowName);
        //Loading Preferences
        gp = GeneralPreferences.getInstance();
		if (!gp.check())
		{
			//Preferences not valid
			JOptionPane.showMessageDialog(frame, "<html><p>Die Einstellungen konnten nicht geladen werden.<br>Sie werden auf den Standard gesetzt und gespeichert.</p></html>","Initialisierungsfehler",JOptionPane.ERROR_MESSAGE);
			gp.resettoDefault();
			gp.writetoXML();
		}
		//Init all to an empty stuff
		
		if (gp.getBoolValue("graph.new_with_graph")) //Start with new graph
			MainGraph = new VGraph(gp.getBoolValue("graph.directed"),gp.getBoolValue("graph.allowloops"),gp.getBoolValue("graph.allowmultiple"));
		else //Start with new Hypergraph
			MainGraph = new VHyperGraph();
		
		frame.addWindowListener(this);        
        BorderLayout test = new BorderLayout();
        frame.setLayout(test);
        buildmaingrid(new Dimension(gp.getIntValue("window.x"),gp.getIntValue("window.y")),new Dimension(gp.getIntValue("vgraphic.x"),gp.getIntValue("vgraphic.y")));
        frame.getContentPane().add("Center",mainPanel);        
        //Display the window.
        //Create the menu bar and bind it's actions on the graphpart.
        MenuBar = new MainMenu(graphpart);
        //Set the menu bar and add the label to the content pane.
    	frame.setJMenuBar(MenuBar);
        if (System.getProperty("os.name").toLowerCase().indexOf("mac")!=-1)
        {
        	//The normal way would be just to set the frame Menubar to NULL
        	//But then the Key-Shortcuts won't work.
        	//But if the Menubar is still in the Frame the shortcuts work. 
        	//So this just sets the MenuBarSize to 0
        	MenuBar.setBounds(new Rectangle(0,0,0,0));
        	MenuBar.setSize(new Dimension(0,0));
        	MenuBar.setOpaque(false);
        	MenuBar.setPreferredSize(new Dimension(0,0));
        	MenuBar.setMaximumSize(new Dimension(0,0));
        }
        frame.setMinimumSize(new Dimension(300,200));
        frame.getContentPane().setMinimumSize(new Dimension(300,200));

        //Load
		if (f!=null)
			setFile(f);
		else if (gp.getBoolValue("graph.loadfileonstart")&&(!gp.getStringValue("graph.lastfile").equals("$NONE")))
			setFile(new File(gp.getStringValue("graph.lastfile")));
	}
	/**
	 * Explicitly open the File f
	 * @param f
	 */
	public void setFile(File f)
	{
		if (!f.exists())
			JOptionPane.showMessageDialog(frame, "<html><p>Die angegebene Datei<br><br><i>"+f.getAbsolutePath()+"</i><br><br>existiert nicht.</p></html>","Initialisierungsfehler",JOptionPane.ERROR_MESSAGE);
		JFileDialogs fD = new JFileDialogs(graphpart);
		fD.Open(f);		
	}
	/**
	 * 	Set the GUI visible
	 */
	public void show()
	{
        frame.pack();
        frame.setVisible(true);
        frame.setResizable(true);
	}
    /**
     * buildmaingrid() baut ein Grid in dem oben eine Buttonliste ist und dadrunter ein
     * SplitPane in dem man die beiden Inhalte, den Graph und den Tree mit Eigenschaften in der Breite variieren kann
     */
    protected void buildmaingrid(Dimension window, Dimension graphsize) 
    {
        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        
        //Die GraphicKomponente
        if (MainGraph.getType()==VGraphInterface.GRAPH)
        	graphpart = new VGraphic(graphsize,(VGraph)MainGraph);
        else if (MainGraph.getType()==VGraphInterface.HYPERGRAPH)
        	graphpart = new VHyperGraphic(graphsize,(VHyperGraph)MainGraph);        	
        graphpart.setMouseHandling(VCommonGraphic.STD_MOUSEHANDLING);
        //Das Ganze als Scrollpane
        mainScroll = new JScrollPane(graphpart);
        mainScroll.setViewportView(graphpart);
        graphpart.setViewPort(mainScroll.getViewport());
        mainScroll.setMinimumSize(new Dimension(gp.getIntValue("vgraphic.x"),gp.getIntValue("vgraphic.y")));
        mainScroll.setPreferredSize(graphsize);
        mainScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        mainScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        graphpart.setSize(graphsize);
        graphpart.validate();
        
        //Toolbar
        gToolBar = new MainToolbar(graphpart);
        gToolBar.setSize(new Dimension(window.width,gToolBar.getSize().height));
    	gToolBar.setMinimumSize(gToolBar.getSize());
    	mainPanel.add(gToolBar,BorderLayout.NORTH);
        
        //Liste
        graphlist = new GraphTree(MainGraph);
        
        //Das Ganze als Scrollpane
        treeScroll = new JScrollPane(graphlist);
        treeScroll.setMinimumSize(new Dimension(gp.getIntValue("window.x")-gp.getIntValue("vgraphic.x"),gp.getIntValue("window.y")-gp.getIntValue("vgraphic.y")));
        
        //Unter die GraphList noch die Statistik
        stats = new Statistics(graphpart);
        JSplitPane rightside = new JSplitPane(JSplitPane.VERTICAL_SPLIT,treeScroll,stats);
        rightside.setPreferredSize(new Dimension(window.width-graphsize.width,window.height-graphsize.height));
        rightside.setResizeWeight(1.0);
        rightside.validate();
        mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                   mainScroll,rightside/*graphlist*/);
        mainSplit.setDividerLocation(0.66);
        mainSplit.setResizeWeight(1.0);
        mainPanel.add(mainSplit,BorderLayout.CENTER);
        mainPanel.setMinimumSize(window);
        mainPanel.setPreferredSize(window);
        mainPanel.doLayout();
        shapePart= null;
    	shapeParameters = null;
    }
    /**
     * buildmaingrid() baut ein Grid in dem oben eine Buttonliste ist und dadrunter ein
     * SplitPane in dem man die beiden Inhalte, den Graph und den Tree mit Eigenschaften in der Breite variieren kann
     */
    public void rebuildmaingrid(boolean applyChange) 
    {
    	if (shapePart!=null)
    	{	
       		graphpart.getGraphHistoryManager().setObservation(true);
       		shapePart.getGraphHistoryManager().setObservation(false);
    		if (applyChange)
    		{
    			int index = shapeParameters.getActualEdge().getIndex();
    			MHyperEdge mhe = shapePart.getGraph().getMathGraph().modifyHyperEdges.get(index);
    			((VHyperGraph)MainGraph).modifyHyperEdges.replace(shapeParameters.getActualEdge(), mhe);
    			//Push the change as a block of changes
    			MainGraph.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,index,GraphConstraints.UPDATE|GraphConstraints.HYPEREDGESHAPE,GraphConstraints.HYPEREDGE));
    		}
    		//Recreate Selection
    		MainGraph.setSelection(shapePart.getGraph());
    	}
        shapePart = null;
    	shapeParameters = null;
    	mainPanel.remove(mainSplit);
        //Unter die GraphList noch die Statistik
        mainScroll.setViewportView(graphpart);
        graphpart.setViewPort(mainScroll.getViewport());
        JSplitPane rightside = new JSplitPane(JSplitPane.VERTICAL_SPLIT,treeScroll,stats);
        rightside.setPreferredSize(new Dimension(mainPanel.getBounds().getSize().width - graphpart.getBounds().getSize().width,mainPanel.getBounds().getSize().height - graphpart.getBounds().getSize().height));
        rightside.setResizeWeight(1.0);
        rightside.validate();
        frame.validate();
        mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                   mainScroll,rightside/*graphlist*/);
        mainSplit.validate();
        mainSplit.setDividerLocation(mainSplit.getSize().width
        - mainSplit.getInsets().right
        - mainSplit.getDividerSize()
        - rightside.getWidth());
        mainSplit.setResizeWeight(1.0);
        mainPanel.add(mainSplit,BorderLayout.CENTER);
        mainPanel.doLayout();
        gToolBar.changeVGraph(graphpart);
        MenuBar.changeVGraph(graphpart);
        getParentWindow().validate();
    }
    /**
     * Indicates whether there any hyperedge shape modification going on
     * @return true if there is a shape in modification, else false
     */
    public boolean isShapeModificationActive()
    {
    	return shapePart!=null;
    }
    /**
     * Ask for the unchanged hyperedge shape, if there is one,
     * whether to apply these changes, discard them or cancle action
     * 
     * The return value indicates whether to proceed with the started action (e.g. quit or load ...)
     * So that means
     * - YES applies the last hyperedge shape changes to the graph and proceeds
     * - NO does not apply the changes and proceeds
     * - CANCEL does not apply changes and does not proceed
     * 
     * @return true if the action should proceed, so false iff the user clicked abort
     * 
     */
    public boolean ApplyChange()
    {
		if (Gui.getInstance().isShapeModificationActive())
		{
			if (!shapePart.getGraphHistoryManager().IsGraphUnchanged()) //Edge changed
			{
				int n = JOptionPane.showConfirmDialog(Gui.getInstance().getParentWindow(), "<html>Der aktuelle Hyperkantenumriss<br>ist noch nicht im Graphen gespeichert.<br>M"+main.CONST.html_oe+"chten Sie den Umriss "+main.CONST.html_ue+"bernehmen?</html>","Umriss übernehmen",JOptionPane.YES_NO_CANCEL_OPTION);
				if (n==JOptionPane.YES_OPTION)
					rebuildmaingrid(true);
				else if (n==JOptionPane.NO_OPTION)
					rebuildmaingrid(false);
				else
					return false; //Do not continue
			}
			else //no Changes, so we don't need to apply
				rebuildmaingrid(false);
			return true; //All the else cases here mean we continue.
		}
		else
			return true; //No Shape - continue anyway
    }
    /**
     * buildmaingrid() baut ein Grid in dem oben eine Buttonliste ist und dadrunter ein
     * SplitPane in dem man die beiden Inhalte, den Graph und den Tree mit Eigenschaften in der Breite variieren kann
     */
    public void InitShapeModification(int edge) 
    {
   	   if (MainGraph.getType()!=VGraphInterface.HYPERGRAPH)
    		return; //Only for HyperGraphs that...
   	   if (((VHyperGraph)MainGraph).modifyHyperEdges.get(edge)==null)
   		   return; //Have the specific Edge in themselves
		graphpart.getGraphHistoryManager().setObservation(false);
   	   	mainPanel.remove(mainSplit);
   	   	shapePart = new VHyperShapeGraphic(graphpart.getBounds().getSize(), ((VHyperGraph)MainGraph).clone(), edge);
        //Das Ganze als Scrollpane
        shapeScroll = new JScrollPane(shapePart);
        shapeScroll.setViewportView(shapePart);
        shapePart.setViewPort(shapeScroll.getViewport());
        shapeScroll.setMinimumSize(new Dimension(shapePart.getBounds().getSize()));
        shapeScroll.setPreferredSize(shapePart.getBounds().getSize());
        shapeScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        shapeScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        shapePart.setSize(graphpart.getBounds().getSize());
        shapePart.validate();
        gToolBar.changeVGraph(shapePart);
        MenuBar.changeVGraph(shapePart);                
        //Das Ganze als Scrollpane
        shapeParameters = new HyperEdgeShapePanel(edge,shapePart);
        mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                   shapeScroll,shapeParameters.getContent());
        frame.validate();
        mainSplit.validate();
        shapeParameters.getContent().validate();
        mainSplit.setResizeWeight(1.0); 
        mainPanel.add(mainSplit,BorderLayout.CENTER);
        mainPanel.doLayout();
       frame.setMinimumSize(new Dimension(300,200));
       frame.getContentPane().setMinimumSize(new Dimension(300,200));
//       mainSplit.setDividerLocation(mainSplit.getSize().width
//               - mainSplit.getInsets().right
//               - mainSplit.getDividerSize()
//               - shapeParameters.getContent().getWidth());
       getParentWindow().validate();
    }
    /**
     * Show About-Dialog
     */
    public void showAbout()
    {
    	String text = "<html><div align=center><font size='+1'>Gravel "+main.CONST.version+"</font><br>Ein Editor für Graphen</div><br>\nCopyright (C) 2007-2009 Ronny Bergmann<br>\n";
    	text += "<br>Für dieses Programm besteht KEINERLEI GARANTIE.<br>Dies ist freie Software, die Sie unter bestimmten";
    	text += "<br> Bedingungen weitergeben dürfen;<br>";
    	text += "Weitere Details in der LICENSE.txt oder ";
    	text += "<br>in der Hilfe im Abschnitt \u201ELizenzbestimmungen\u201D";
    	text += "</font></html>";
    	JLabel AboutLabel = new JLabel();
		AboutLabel.setText(text);
        AboutLabel.setOpaque(true);
		JOptionPane.showMessageDialog(frame, AboutLabel,"Über Gravel",JOptionPane.INFORMATION_MESSAGE);
    }
    /**
     * Get the actual Draing area as Graphcs-Object, e.g. for measurements
     * @return
     */
    public Graphics getActualGraphics()
    {
    	if (shapePart!=null)
    		return shapePart.getGraphics();
    	else
    		return graphpart.getGraphics();
    }    
    /**
     * Refresh Gui and set it active
     */
    public void refresh()
    {
    	frame.toFront();
    }
    /**
     * Handle Quit
     */
    public boolean doQuit()
    {
    		//Check File Saved
    		boolean quit = MenuBar.checkSavedBeforeQuit();
    		if (quit)
    		{
    			frame.setVisible(false);
				frame.dispose();
    		}
    		return quit;
    }
    //External getting referrence of the Graph for manipulating ist
    /**
     * Get actual VGraph for Manipulation
     * 
     * @return the VGraph from the editor
     */
    public VGraphInterface getVGraph()
	{
		return MainGraph;
	}
    /**
     * Change the VGraph to a new one, also resets History.
     * @param vg
     */
    public void setVGraph(VGraphInterface vg)
	{
    	if (vg.getType()==MainGraph.getType()) //Type not changed - simple replace
    	{
        	switch (vg.getType())
        	{
        		case VGraphInterface.GRAPH: 
        		{
               		((VGraph)MainGraph).replace((VGraph)vg);
               		break;
        		}
        		case VGraphInterface.HYPERGRAPH:
        		{
               		((VHyperGraph)MainGraph).replace((VHyperGraph)vg);
               		break;
        		}
        	}
    	}
    	else //Type Changed - init new to vg
    	{
   			MainGraph = vg;
   			frame.getContentPane().removeAll();
   			buildmaingrid(mainPanel.getSize(),graphpart.getSize()); //Rebuild all
   			frame.getContentPane().add("Center",mainPanel);        
   	        //Display the window.
   	        //Create the menu bar and bind it's actions on the graphpart.
   	        MenuBar = new MainMenu(graphpart);
   	        //Set the menu bar and add the label to the content pane.
   	    	frame.setJMenuBar(MenuBar);
   	        if (System.getProperty("os.name").toLowerCase().indexOf("mac")!=-1)
   	        {
   	        	//The normal way would be just to set the frame Menubar to NULL
   	        	//But then the Key-Shortcuts won't work.
   	        	//But if the Menubar is still in the Frame the shortcuts work. 
   	        	//So this just sets the MenuBarSize to 0
   	        	MenuBar.setBounds(new Rectangle(0,0,0,0));
   	        	MenuBar.setSize(new Dimension(0,0));
   	        	MenuBar.setOpaque(false);
   	        	MenuBar.setPreferredSize(new Dimension(0,0));
   	        	MenuBar.setMaximumSize(new Dimension(0,0));
   	        }
   	        frame.pack();
   		}
	}
   /**
    * Get Entries of the Statistics
    * @return
    */
    public TreeMap<String, String> getStatRows()
    {
    	return stats.getRows();
    }
    /**
     * Get GUI Parent(Main-)frame
     * @return
     */
   public JFrame getParentWindow()
   {
	   return frame;
   }
   /**
    * Get the GraphHistoryManager that tracks actions in the actual graph
    * @return the GraphHistoryManager
    */
   public CommonGraphHistoryManager getGraphHistoryManager()
   {
	   return graphpart.getGraphHistoryManager();
   }
  
   public void windowActivated(WindowEvent arg0) {}
   
   public void windowClosed(WindowEvent arg0) 
	{
		if (!gp.getBoolValue("pref.saveonexit")) //nicht speichern, also die alten sachen mal Laden
			gp.readXML();
		//sonst speichern, aber vorher noch den letzten File löschen, wenn nichts geladen werden soll
		if (!gp.getBoolValue("graph.loadfileonstart")) 
			gp.setStringValue("graph.lastfile","$NONE");
		//evtl neue daten Speichern wenn nicht oben geladen worden ist
			gp.setFloatValue("zoom",1f);
			gp.writetoXML();
		System.exit(0);
	}
   
   public void windowClosing(WindowEvent e) {
		if (doQuit())
			e.getWindow().dispose();		
	}
   public void windowDeactivated(WindowEvent arg0) {}
   
   public void windowDeiconified(WindowEvent arg0) {}
   
   public void windowIconified(WindowEvent arg0) {}
   
   public void windowOpened(WindowEvent arg0) {}
}