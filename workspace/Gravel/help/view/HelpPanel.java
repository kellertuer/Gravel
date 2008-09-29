package help.view;

import java.awt.Component;
import java.io.File;
import java.net.URL;

//Using JavaHelp 2.0
import javax.help.*;
import javax.swing.JOptionPane;

import view.Gui;

/**
 * Help Class as Singleton-Pattern
 * Creates the Help Windows on first call and sets it to visible each call after that
 * 
 * 
 * @author Ronny Bergmann
 *
 */
public class HelpPanel
{
	private static final long serialVersionUID = 1L;

	private static HelpPanel instance;
	private HelpSet hs;
	private HelpBroker hb;
	
	/**
	 * get the Instance of Help, that is running. If there is none running, create it
	 * @return
	 */
	public synchronized static HelpPanel getInstance()
	{
		if (instance==null)
		{
			instance=new HelpPanel("de");
		}
		return instance;
	}
	/**
	 * private constructor with a language
	 * 
	 * @param language (up to know only de availabe
	 */
	private HelpPanel(String language)
	{
		ClassLoader loader = this.getClass().getClassLoader();
		URL url;
		try 
		{
			//url = HelpSet.findHelpSet(loader,"data/help/"+language+"/help.hs");
			url = (new File("data/help/"+language+"/help.hs")).toURL();
			hs = new HelpSet(loader,url);
		}
		catch (Exception e)
		{
			//e.printStackTrace();
			JOptionPane.showMessageDialog(Gui.getInstance().getParentWindow(), "<html><p>Die Hilfe konnte nicht geöffnet werden.<br> Es liegt ein Fehler vor:<br>Die Datei help.hs wurde nicht in data/help/"+language+"/ gefunden</p></html>", "Fehler", JOptionPane.ERROR_MESSAGE);
		}
		hb = hs.createHelpBroker();
	}
	/**
	 * Set an Help Button to the specific Help Site
	 *  
	 * @param b Component that is associated with help site
	 * @param key key of he help site
	 */
	public void setButtonToHelpTopic(Component b, String key)
	{
		hb.enableHelpOnButton(b, key, hs);
	}
	/**
	 * Set Help visible (with Index Page)
	 *
	 */
	public void showHelp()
	{
		showHelp(hs.getHomeID().getIDString());
	}
	/**
	 * Show Help and initiate it with a specific help page
	 * @param key
	 */
	public void showHelp(String key)
	{
		hb.setDisplayed(true);
		hb.setCurrentID(key);
	}
}