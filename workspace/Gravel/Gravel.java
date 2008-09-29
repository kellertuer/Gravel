import javax.swing.UIManager;

import view.Gui;
import view.Gui_Mac;
/**
 * Gravel
 * 
 * Main Class of the Gravel Project. this one is in the default package, so that the name of the Mac-Menu
 * is only Gravel. I'm trying to get another solution for that
 * 
 * The Gravel Main Class creates the main window depending on the OS, on which the program
 * is started. For MacOS an MacOS Decoator is used to get functionality of mac software
 * for all other OS a normal main window is chosen
 * 
 * @author Ronny Bergmann
 *
 */
public class Gravel {
		public static void main(String[] args) {	 
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
	            public void run() {
	            	 try
	                 { 
	                     UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); 
	                 }
	                 catch(Exception e)
	                 {
	                     //Kein Look and feel möglich - Standardeinstellungen werden verwendet
	                     e.printStackTrace();
	                 } 
	            	if (System.getProperty("os.name").toLowerCase().indexOf("mac")!=-1)
	                {    		
	            		//Mac-spezifische Systemwerte, wie etwa die Verwendung des Application-Menüs oben am Rand
	                	System.setProperty("apple.laf.useScreenMenuBar","true"); //Use Menu Bar
	            		System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Gravel");
	                	System.setProperty("com.apple.macos.use-file-dialog-packages","true");
	                	System.setProperty("com.apple.eawt.CocoaComponent.CompatibilityMode", "false"); 
	                	System.setProperty("com.apple.mrj.application.growbox.intrudes","false");
	                	//Hauptfenster mit Mac-Deko
	                	Gui_Mac.getInstance();
	                }
	            	else
	            	{
	            		//Andere Systeme ohne Mac Dekoration
	                    Gui.getInstance();
    		            Gui.getInstance().show();
	            	}
	            }
	        });
	    }
}