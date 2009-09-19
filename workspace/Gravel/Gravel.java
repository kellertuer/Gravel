import java.io.File;

import javax.swing.UIManager;

import main.DEBUG;

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
	private static File f = null;
	//This Boolean indicates whether to start Gravel or not. Help and Version information
	//are displayed without starting Gravel
	private static boolean start=true;
	//Values for bold ascii
	private static final String NORMAL     = "\u001b[0m";
	private static final String BOLD       = "\u001b[1m";
	private static final String UNDERLINE  = "\u001b[4m";
	
	public static void main(String[] args)
	{	 
		handleArguments(args);	

		if (!start)
			return;
			//Evoke application as an own thead
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
	                	Gui_Mac.getInstance(f);
	                }
	            	else
	            	{
	            		//Andere Systeme ohne Mac Dekoration
	                    Gui.getInstance(f);
    		            Gui.getInstance().show();
	            	}
	            }
	        });
	    }
		/**
		 * Handle arguments, that are given to this application via console
		 * @param args
		 */
		private static void handleArguments(String[] args)
		{
			//If we encounter --help, -h display help and end
			//if we encounter --version or -v display version info and end
			for (int i=0; i<args.length; i++)
			{
				String actual = args[i];
				String low = actual.toLowerCase();
				String command = low.split("=")[0]; 
				if (command.equals("--help")||(low.equals("-h")))
					displayHelp();
				else if (command.equals("--version")||(low.equals("-v")))
					displayVersion();
				else if (command.equals("--file"))
				{
					if (actual.split("=").length==2) //work case sensitive
					{
						f = new File(actual.split("=")[1]);
						if (!f.exists())
						{
							System.out.println(UNDERLINE+f.getAbsolutePath()+NORMAL+" does not exist!");
							f=null;
						}
					}
				}
				else if (command.equals("--debug"))
				{
					if (low.split("=").length==2)
					{
						setDebugTo(low.split("=")[1]);
					}
				}
				else if (low.equals("-d"))
				{
					int debugindex = i+1;
					if (debugindex == args.length)
						System.out.println("-d given, but no debug.");
					else
						setDebugTo(args[debugindex]);
				}
				else if (low.endsWith(".xml"))
				{
					f = new File(actual);
					if (!f.exists())
					{
						System.out.println(UNDERLINE+f.getAbsolutePath()+NORMAL+" does not exist!");
						f=null;
					}
					
				}
			}
		}
		private static void displayHelp()
		{
	    	String text = "usage: gravel.jar [--help] [-h] [--version] [-v] "+
	    				  "[--file="+UNDERLINE+"filename"+NORMAL+"] "+
	    				  "[--debug="+UNDERLINE+"level"+NORMAL+"] [-d "+UNDERLINE+"level"+NORMAL+"]"+
	    				  "["+UNDERLINE+"FILE"+NORMAL+"]\n\n";
	    	text += "Gravel with only a "+UNDERLINE+"FILE"+NORMAL+" as an argument (file must end with '.xml) starts gravel and loads this file\n";
	    	text += "The available options are:\n\n";
	    	text += "   "+BOLD+"--debug"+NORMAL+"="+UNDERLINE+"level"+NORMAL+"\tSets the debug level, available values are:\n";
	    	text += "   \t\t"+UNDERLINE+"0"+NORMAL+" or "+UNDERLINE+"none"+NORMAL+" no debug (default).\n";
	    	text += "   \t\t"+UNDERLINE+"1"+NORMAL+" or "+UNDERLINE+"low"+NORMAL+" only displays important debug.\n";
	    	text += "   \t\t"+UNDERLINE+"2"+NORMAL+" or "+UNDERLINE+"middle"+NORMAL+" displays low and middle debug.\n";
	    	text += "   \t\t"+UNDERLINE+"3"+NORMAL+" or "+UNDERLINE+"high"+NORMAL+" displays all debug.\n\n";
	    	text += "   "+BOLD+"--file="+NORMAL+UNDERLINE+"filename"+NORMAL+"\t loads the file specified by "+UNDERLINE+"filename"+NORMAL+"\n\n";
	    	text += "   "+BOLD+"--help"+NORMAL+"\t\t displays this help\n\n";
	    	text += "   "+BOLD+"--version"+NORMAL+"\t\t displays the version and license information\n\n";
	    	text += "And there are the following short command versions:\n";
	    	text +="   "+BOLD+"-d"+NORMAL+" "+UNDERLINE+"level"+NORMAL+"\t is equal to --debug=level\n";
	    	text +="   "+BOLD+"-h"+NORMAL+" \t is equal to --help\n";
	    	text +="   "+BOLD+"-v"+NORMAL+" \t is equal to --version\n";
	    	
	    	System.out.print(text);
			
	    	start=false;
		}
		private static void displayVersion()
		{
	    	String text = "                                   "+BOLD+"Gravel "+main.CONST.version+NORMAL+"\n";
	    	      text += "                      "+UNDERLINE+"An editor for graphs an hypergraphs"+NORMAL+"\n";
	    	      text += "                                                              (build "+main.CONST.lastchangedshort+")\n\n";
	    	      text += "Copyright (C) 2007-2009 Ronny Bergmann\n\n";
	    	      text += "This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.\n\n";
	    	      text += "This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.\n\n";
	    	      text += "You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.\n";
	    	
	    	System.out.print(text);
	    	start=false;
		}
		private static void setDebugTo(String s)
		{
			String debuglevel = s.toLowerCase();
			if (debuglevel.equals("0")||debuglevel.equals("none"))
				DEBUG.setDebugLevel(DEBUG.NONE);
			else if (debuglevel.equals("1")||debuglevel.equals("low"))
				DEBUG.setDebugLevel(DEBUG.LOW);
			else if (debuglevel.equals("2")||debuglevel.equals("middle"))
				DEBUG.setDebugLevel(DEBUG.MIDDLE);
			else if (debuglevel.equals("3")||debuglevel.equals("high"))
				DEBUG.setDebugLevel(DEBUG.HIGH);
			else
				System.err.println("No valid Debug Level given, it stays default (none).");
		}
}