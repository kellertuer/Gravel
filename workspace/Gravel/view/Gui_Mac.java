package view;

import com.apple.eawt.*;

import io.GeneralPreferences;

import java.awt.Dimension;
import java.awt.event.*;
import java.io.File;

import java.util.TreeMap;

import javax.swing.*;

import model.VGraphInterface;
import model.Messages.GraphConstraints;
import model.Messages.GraphMessage;

import dialogs.JPreferencesDialog;


/**
 * 	Hauptklasse der GUI, stellt das Hauptfenster zur Verfügung mittels Singelton-Muster.
 * 	Enth&auml;lt die Grafikumgebung, die Liste sowie die Statistik des Graphen. Au&szlig;erdem die Toolbar und das Men&uuml;  
 *
 *	@author Ronny Bergmann
 */
public class Gui_Mac extends Application implements  ApplicationListener, WindowListener
{
	public static final String WindowName = "Gravel";
	static private Gui_Mac instance;
	private Gui normalgui;
	/**
	 * gibt die GUI zurück, falls diese existiert.
	 * <br>Existiert sie nicht, wird eine GUI erstellt und zurückgegeben
	 * 
	 * @return die Referenz auf die GUI
	 */
	public synchronized static Gui_Mac getInstance()
	{
		if (instance == null)
		{
			instance = new Gui_Mac(null);
		}
		return instance;	
	}
	/**
	 * Returns the GUI if it exists and creates one, if it doesn't
	 * this method also starts the gui with a file if it exists, else sets the file to f
	 */
	public synchronized static Gui_Mac getInstance(File f)
	{		
		if (instance == null)
		{
			instance = new Gui_Mac(f);
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
	private Gui_Mac(File f) 
	{
		normalgui = Gui.getInstance(f);
		//Auf einem Mac das Menü nicht im Frame lassen
    	//Und die Spezifischen Menüs enablen
		this.addAboutMenuItem();
    	this.setEnabledAboutMenu(true);
    	this.addApplicationListener(this);
    	this.addPreferencesMenuItem();
    	this.setEnabledPreferencesMenu(true);
		normalgui.show();
		normalgui.getParentWindow().setMinimumSize(normalgui.getParentWindow().getSize());
	}
	
	public void setFile(File f)
	{
		normalgui.setFile(f);
	}
    /**
     * buildmaingrid() baut ein Grid in dem oben eine Buttonliste ist und dadrunter ein
     * SplitPane in dem man die beiden Inhalte, den Graph und den Tree mit Eigenschaften in der Breite variieren kann
     */
    public void buildmaingrid(Dimension window, Dimension graphsize) 
    {
        normalgui.buildmaingrid(window,graphsize);
    }
    /**
     * Display About-Dialog
     */
    public void showAbout()
    {
    	normalgui.showAbout();
    }
    /**
     * Handle Quit-Action
     */
    public void doQuit()
    {
    	normalgui.doQuit();
    }
    //External getting referrence of the Graph for manipulating ist
    public VGraphInterface getVGraph()
	{
		return normalgui.getVGraph();
	}
    public void setVGraph(VGraphInterface vg)
	{
		normalgui.setVGraph(vg);
	}
    public TreeMap<String, String> getStatRows()
    {
    	return normalgui.getStatRows();
    }
    
   public JFrame getParentWindow()
   {
	   return normalgui.getParentWindow();
   }
   
   //Apple Erweiterungen
    public void handleAbout(ApplicationEvent e) 
    {
    	showAbout();
    	e.setHandled(true);
    }
    public void HandleAbout(ApplicationEvent e){
       	showAbout();
    	e.setHandled(true);
    }
 
    public void handlePreferences(ApplicationEvent e)
    {
    	new JPreferencesDialog();
    	normalgui.getVGraph().pushNotify(new GraphMessage(GraphConstraints.SELECTION|GraphConstraints.GRAPH_ALL_ELEMENTS,GraphConstraints.UPDATE));
    	//e.setHandled(true);
    }
	public void handleQuit(ApplicationEvent e)
	{
		doQuit();
	}
	public void handleOpenApplication(ApplicationEvent arg0) {}
	public void handleOpenFile(ApplicationEvent arg0) {}
	public void handlePrintFile(ApplicationEvent arg0) {}
	public void handleReOpenApplication(ApplicationEvent arg0) {}
	
	public void windowActivated(WindowEvent arg0)
	{}
	public void windowClosed(WindowEvent arg0) 
	{
		GeneralPreferences gp = GeneralPreferences.getInstance();
		
		if (!gp.getBoolValue("pref.saveonexit")) //nicht speichern, also die alten sachen mal Laden
			gp.readXML();
		//sonst speichern, aber vorher noch den letzten File löschen, wenn nichts geladen werden soll
		if (!gp.getBoolValue("graph.loadfileonstart")) 
			gp.setStringValue("graph.lastfile","$NONE");
		//evtl neue daten Speichern wenn nicht oben geladen worden ist
			gp.writetoXML();
		System.exit(0);
	}
	public void windowClosing(WindowEvent e) {
	    e.getWindow().dispose();		
	}
	public void windowDeactivated(WindowEvent arg0) {}
	public void windowDeiconified(WindowEvent arg0) {}
	public void windowIconified(WindowEvent arg0) {}
	public void windowOpened(WindowEvent arg0) {}
}












