package view;

import io.GeneralPreferences;
import io.GravelMLReader;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.TreeMap;

import javax.swing.*;


import model.VGraph;

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
	private VGraph MainGraph;
	//Der MainContentFrame
	private JFrame frame;
	private JPanel mainPanel;
	private VGraphic graphpart; 
	private Statistics stats;
	private GraphTree graphlist;
	private JToolBar gToolBar;
	private MainMenu MenuBar;
	private static Gui instance;
	private GeneralPreferences gp;
	/**
	 * gibt die GUI zurück, falls diese existiert.
	 * <br>Existiert sie nicht, wird eine GUI erstellt und zurückgegeben
	 * 
	 * @return die Referenz auf die GUI
	 */
	public synchronized static Gui getInstance()
	{		
		if (instance == null)
		{
			instance = new Gui();
		}
		return instance;	
	}
	/**
	 * Konstruktor der GUI, erstellt das Fenster, initialisiert Komponenten
	 * <br>Aufgrund des Singelton-Musters ist der Konstruktor private
	 *
	 */
	private Gui() 
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
		if (gp.getBoolValue("graph.loadfileonstart")&&(!gp.getStringValue("graph.lastfile").equals("$NONE")))
		{
			GravelMLReader R = new GravelMLReader();
			String error="";
			try {
				error = R.checkGraph(new File(gp.getStringValue("graph.lastfile")));
			}
			catch(Exception e){}
			if (error.equals("")) //letzten laden
			{
				error = R.readGraph(); // Graph einlesen, Fehler merken
			}
			//bei einem der beiden ist ein Fehler aufgetreten
			if (!error.equals("")) //es liegt ein fehler vor
			{
				JOptionPane.showMessageDialog(frame, "<html><p>Der Graph konnte nicht geladen werden<br>Fehler :<br>"+error+"</p></html>","Initialisierungsfehler",JOptionPane.ERROR_MESSAGE);				
				MainGraph = new VGraph(gp.getBoolValue("graph.directed"),gp.getBoolValue("graph.allowloops"),gp.getBoolValue("graph.allowmultiple"));
			} //kein Fehler und ein VGraph
			else if (R.getVGraph()!=null)
			{
				MainGraph=R.getVGraph(); //weiter so
				gp.setStringValue("graph.fileformat","visual");
				frame.setTitle(Gui.WindowName+" - "+(new File(gp.getStringValue("graph.lastfile")).getName())+"");
			}
			else
			{
				System.err.println(" DEBUG MGraph geladen, TODO Wizard hier einbauen.");
				//TODO: Wizard 
				JOptionPane.showMessageDialog(frame, "<html><p>Der letzte gespeicherte Graph ist ein mathematischer Graph. Diese können zwar geladen, danach jedoch nicht weiter verarbeitet werden.<br>Gravel startet mit einem neuen Graphen.</p></html>","Hinweis",JOptionPane.INFORMATION_MESSAGE);				
				MainGraph = new VGraph(gp.getBoolValue("graph.directed"),gp.getBoolValue("graph.allowloops"),gp.getBoolValue("graph.allowmultiple"));
			}
		}
		else //don't load a Graph on start
			MainGraph = new VGraph(gp.getBoolValue("graph.directed"),gp.getBoolValue("graph.allowloops"),gp.getBoolValue("graph.allowmultiple"));			
        //frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.addWindowListener(this);        
        BorderLayout test = new BorderLayout();
        frame.setLayout(test);
        buildmaingrid();
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
   	}
	/**
	 * 
	 *
	 */
	public void show()
	{
        frame.pack();
        frame.setVisible(true);
        frame.setResizable(true);
	}
    /*
     * buildmaingrid() baut ein Grid in dem oben eine Buttonliste ist und dadrunter ein
     * SplitPane in dem man die beiden Inhalte, den Graph und den Tree mit Eigenschaften in der Breite variieren kann
     */
    public void buildmaingrid() 
    {
        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        
        //Die GraphicKomponente
        graphpart = new VGraphic(new Dimension(gp.getIntValue("vgraphic.x"),gp.getIntValue("vgraphic.y")),MainGraph);
        graphpart.setMouseHandling(VGraphic.STD_MOUSEHANDLING);
        //Das Ganze als Scrollpane
        JScrollPane scrollPane = new JScrollPane(graphpart);
        scrollPane.setViewportView(graphpart);
        graphpart.setViewPort(scrollPane.getViewport());
        scrollPane.setMinimumSize(new Dimension(gp.getIntValue("vgraphic.x"),gp.getIntValue("vgraphic.y")));
        scrollPane.setPreferredSize(new Dimension(gp.getIntValue("vgraphic.x"),gp.getIntValue("vgraphic.y")));
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        graphpart.setSize(new Dimension(gp.getIntValue("vgraphic.x"),gp.getIntValue("vgraphic.y")));
        graphpart.validate();
        //Toolbar
        gToolBar = new MainToolbar(graphpart);
        gToolBar.setSize(new Dimension(gp.getIntValue("window.x"),gToolBar.getSize().height));
    	gToolBar.setMinimumSize(gToolBar.getSize());
    	mainPanel.add(gToolBar,BorderLayout.NORTH);
        
        //Liste
        graphlist = new GraphTree(MainGraph);
        
        //Das Ganze als Scrollpane
        JScrollPane scrollPane2 = new JScrollPane(graphlist);
        scrollPane2.setMinimumSize(new Dimension(gp.getIntValue("window.x")-gp.getIntValue("vgraphic.x"),gp.getIntValue("window.y")-gp.getIntValue("statistics.y")));
        
        //Unter die GraphList noch die Statistik
        stats = new Statistics(graphpart);
        JSplitPane rightside = new JSplitPane(JSplitPane.VERTICAL_SPLIT,scrollPane2,stats);
        rightside.setPreferredSize(new Dimension(gp.getIntValue("window.x")-gp.getIntValue("vgraphic.x"),gp.getIntValue("window.y")));
        rightside.setResizeWeight(1.0);
        rightside.validate();
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                   scrollPane,rightside/*graphlist*/);
        splitPane.setDividerLocation(0.66);
        //splitPane.setOneTouchExpandable(true);
        splitPane.setPreferredSize(new Dimension(500+50+40,500+15));
        splitPane.setResizeWeight(1.0);
        mainPanel.add(splitPane,BorderLayout.CENTER);
        mainPanel.setMinimumSize(new Dimension(gp.getIntValue("window.x"),gp.getIntValue("window.y")));
        mainPanel.setPreferredSize(new Dimension(gp.getIntValue("window.x"),gp.getIntValue("window.y")));
        mainPanel.doLayout();
    }

    public void showAbout()
    {
    	String text = "<html><div align=center><font size='+1'>Gravel "+main.CONST.version+"</font><br>Ein Editor für Graphen</div><br>\nCopyright (C) 2007 Ronny Bergmann<br>\n";
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
    public void doQuit()
    {
    		//Check File Saved
    		MenuBar.checkSaved();    		
			frame.setVisible(false);
			frame.dispose();
    }
    //External getting referrence of the Graph for manipulating ist
    /**
     * Get actual VGraph for Manipulation
     * 
     * @return the VGraph from the editor
     */
    public VGraph getVGraph()
	{
		return MainGraph;
	}
    /**
     * Change the VGraph to a new one, also resets History.
     * @param vg
     */
    public void setVGraph(VGraph vg)
	{
		MainGraph.replace(vg);
	}
    public TreeMap<String, String> getStatRows()
    {
    	return stats.getRows();
    }
    
   public JFrame getParentWindow()
   {
	   return frame;
   }
   
   public void windowActivated(WindowEvent arg0) {}
	public void windowClosed(WindowEvent arg0) 
	{
		if (!gp.getBoolValue("pref.saveonexit")) //nicht speichern, also die alten sachen mal Laden
			gp.readXML();
		//sonst speichern, aber vorher noch den letzten File löschen, wenn nichts geladen werden soll
		if (!gp.getBoolValue("graph.loadfileonstart")) 
			gp.setStringValue("graph.lastfile","$NONE");
		//Zoom auch nich speichern
		gp.setIntValue("vgraphic.zoom",100);
		//evtl neue daten Speichern wenn nicht oben geladen worden ist
			gp.writetoXML();
		System.exit(0);
	}
	public void windowClosing(WindowEvent e) {
		doQuit();
		e.getWindow().dispose();		
	}
	public void windowDeactivated(WindowEvent arg0) {}
	public void windowDeiconified(WindowEvent arg0) {}
	public void windowIconified(WindowEvent arg0) {}
	public void windowOpened(WindowEvent arg0) {}
}