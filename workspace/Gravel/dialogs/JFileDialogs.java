package dialogs;

import io.GeneralPreferences;
import io.PNGWriter;
import io.GravelMLReader;
import io.SVGWriter;
import io.TeXWriter;
import io.MyLaTeXPictureWriter;
import io.MyTikZPictureWriter;
import io.GravelMLWriter;

import java.awt.Component;

import java.io.File;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import model.VGraph;

import view.Gui;
import view.VGraphic;

/**
 * JFileDialogs stellt alle Dialoge zum Laden und Speichern von Graphen zur Verfügung
 * Observes VGraph of the Editor to indicate whether the graph was saved yet or not.
 * 
 * 
 * - "Laden" des eigenen Formates
 * 		wobei hier eventuell der VisualisierungsWizard aufgerufen wird, falls kein visueller Graph vorliegt
 * - "Speichern unter" des eigenen Formates mit der Auswahl, welches der beiden Formate gespeichert werden soll
 * - "Speichern" des aktuellen Graphen unter dem zuletzt verwendeten Namen im zuletzt gewählten Format
 * 
 * 
 * 		PNG Export
 * 		LaTeX Picture Export
 * 		SVG		Export als Vektorgrafik
 * (	GraphML Export mit oder ohne visuelle Informationen, Subsets gehen verloren
 * 		.dot	Export das Format von Graphviz
 * 
 * 		GraphML Import eines mathematischen Graphen => Wizard starten
 *		.dot	Import so jeder Knoten mindestens x und y enthält VGraph sonst MGraph
 * 		GML Import ?		
 * 
 * @author Ronny Bergmann
 */
public class JFileDialogs implements Observer 
{
	/**
	 * A Simple FileFilter that only displays Folders and the files of a given Extension
	 * 
	 * @author Ronny
	 */
	class SimpleFilter extends FileFilter
	{
		private String m_description = null;
		private String m_extension = null;
		public SimpleFilter(String extension, String description) 
		{
			m_description = description;
			m_extension = "." + extension.toLowerCase();
		}
		public String getDescription() 
		{
			return m_description;
		}
		public String getStdFileExtension()
		{
			return m_extension;
		}
		public boolean accept(File f) 
	    {
			if (f == null)
				return false;
			if (f.isDirectory())
				return true;
			return f.getName().toLowerCase().endsWith(m_extension);
	    }
	}
	
	/**
	 * modified JFileChooser to ask before overwriting any file
	 * 
	 * @author Ronny Bergmann
	 */
	class JOverwriteCheckFileChooser extends JFileChooser
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		String command;
		public JOverwriteCheckFileChooser(String s)
		{
			super(s);
		}
		public int showSaveDialog(Component parent)
		{
		command = "save";
		return super.showSaveDialog(parent);
		}
		public int showOpenDialog(Component parent)
		{
		command = "open";
		return super.showOpenDialog(parent);
		}
		public void approveSelection()
		{
			
			int selection = -1;
			File fold = getSelectedFile();
			String s = ((SimpleFilter)getFileFilter()).getStdFileExtension();
			File f;
			if ((s!=null)&&(getExtension(fold)==null)) //nur etwa test angegeben
			{
				f = new File (fold.getParent()+"/"+fold.getName()+s);
				System.err.println("Changed to "+f.getParent()+"/"+f.getName());
			}
			else
				f = fold;
			//if f.getE
			if(command.equalsIgnoreCase("save"))
			{
				if(getSelectedFile().exists())
					selection = JOptionPane.showConfirmDialog(this, "<html><p>Die Datei existiert bereits. M"+main.CONST.html_oe+"chten Sie die Datei<br>"+f.getName()+"<br>"+main.CONST.html_ue+"berschreiben ?</p></html>"
							, "Datei überschreiben", JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
				if(selection == JOptionPane.NO_OPTION)
					return;
				else
					super.approveSelection();
			}
			else
				super.approveSelection();
			this.setSelectedFile(f);
		}
		private String getExtension(File f)
		{
				String ext = null;
		        String s = f.getName();
		        int i = s.lastIndexOf('.');
		        if (i > 0 &&  i < s.length() - 1) {
		            ext = s.substring(i+1).toLowerCase();
		        }
		        return ext;
		}
	}
	
	private boolean actualgraphsaved = false;
	private boolean saveVisual = true;
	
	private VGraphic vGc;
	
	/**
	 * Constructor 
	 * @param pvg the actual graph editor Component
	 * @param programmstart initialize graphsaved
	 */
	public JFileDialogs(VGraphic pvg, boolean graphsaved)
	{
		vGc = pvg;
		vGc.getVGraph().addObserver(this);
		actualgraphsaved = graphsaved;		
	}
	
	/**
	 * Extracts the extension of a File
	 * 
	 * @param f the File to get the extension from
	 * @return the extension as a String
	 */
	private String getExtension(File f)
	{
			String ext = null;
	        String s = f.getName();
	        int i = s.lastIndexOf('.');
	        if (i > 0 &&  i < s.length() - 1) {
	            ext = s.substring(i+1).toLowerCase();
	        }
	        return ext;
	}
	
	/**
	 * Loads a Graph from a GravelGraphML Source
	 * 
	 * @return true, if a Graph is loaded, else false
	 */
	public boolean Open()
	{
		if (!SaveOnNewOrOpen()) //s.o. aborted
			return false; //Chosen cancel
		JFileChooser fc = new JFileChooser("Öffnen einer Gravel-Datei");
		//Letzten Ordner verwenden
		if (!GeneralPreferences.getInstance().getStringValue("graph.lastfile").equals("$NONE"))
			fc.setCurrentDirectory(new File(GeneralPreferences.getInstance().getStringValue("graph.lastfile")).getParentFile());
		fc.addChoosableFileFilter(new SimpleFilter("XML","GravelML"));
		int returnVal = fc.showOpenDialog(Gui.getInstance().getParentWindow());
		if (returnVal == JFileChooser.APPROVE_OPTION)
		   {
				File f = fc.getSelectedFile();
	    		GravelMLReader R = new GravelMLReader();
	    		String error="";
	    		error = R.checkGraph(f); //Check File
	    		if (error.equals("")) //if okay - load
	    		{
	    			error = R.readGraph(); // Graph einlesen, Fehler merken
	    		}
	    		//bei einem der beiden ist ein Fehler aufgetreten
	    		if (!error.equals("")) //es liegt ein fehler vor
	    		{
	    			JOptionPane.showMessageDialog(Gui.getInstance().getParentWindow(), "<html><p>Der Graph konnte nicht geladen werden<br>Fehler :<br>"+error+"</p></html>","Fehler beim Laden",JOptionPane.ERROR_MESSAGE);				
	    			return false;
	    		} //kein Fehler und ein VGraph
	    		else if (R.getVGraph()!=null)
	    		{
	    			Gui.getInstance().setVGraph(R.getVGraph());
	    			GeneralPreferences.getInstance().setStringValue("graph.lastfile",f.getAbsolutePath());
					Gui.getInstance().getParentWindow().setTitle(Gui.WindowName+" - "+f.getName()+"");
	    			vGc.getVGraph().getMathGraph().deleteObserver(this);
	    			vGc.getVGraph().addObserver(this);
	    			actualgraphsaved = true;
	    			return true;
	    		}
	    		else
	    		{
	    			System.err.println("MGraph geladen, TODO Wizard hier einbauen.");
					JOptionPane.showMessageDialog(Gui.getInstance().getParentWindow(), "<html><p>Die Datei <br><i>"+f.getName()+"</i><br>enth"+main.CONST.html_ae+"lt einen mathematischen Graphen. Diese können bisher nicht weiter verarbeitet werden.</p></html>","Hinweis",JOptionPane.INFORMATION_MESSAGE);				
	    			//TODO: Wizard 
					//Gui.getInstance().getParentWindow().setTitle(Gui.WindowName+" - "+f.getName()+" (math only)");					
	    			//vGc.getVGraph().getMathGraph().addObserver(this);
	    			//vGc.getVGraph().deleteObserver(this);
	    			//actualgraphsaved = true; //because only the math part was loaded
					return true;
	    		}
	    		
	       }
		return false; //Chosen Cancel
	}
	
	/**
	 * Saves a Graph to a GravelGraphML File 
	 *
	 * The File name and Save Modus are extracted from the generalpreferences 
	 *
	 *@return true, if the saving was successful, else false
	 */
	public boolean Save()
	{
		if (GeneralPreferences.getInstance().getStringValue("graph.lastfile").equals("$NONE"))
		{
			return false;
		}
		String filename = GeneralPreferences.getInstance().getStringValue("graph.lastfile");
		File f = new File (filename);
		GravelMLWriter iw = new GravelMLWriter(vGc.getVGraph()); //Save the actual reference
		saveVisual = GeneralPreferences.getInstance().getStringValue("graph.fileformat").equals("visual");
		if (saveVisual)
		{
			if (iw.saveVisualToFile(f).equals("")) //Saving successfull
			{
				Gui.getInstance().getParentWindow().setTitle(Gui.WindowName+" - "+f.getName());
				actualgraphsaved = true;
				return true;
			}
		}
		else
		{
			if (iw.saveMathToFile(f).equals("")) //Saving sucessful
			{
				Gui.getInstance().getParentWindow().setTitle(Gui.WindowName+" - "+f.getName()+" (math only)");
				actualgraphsaved = true;
				return true;				
			}
		}
		return false; //an error occured
	}
	
	/**
	 * Save as a File 
	 *
	 * @return true, if the saving was sucessful
	 */
	public boolean SaveAs()
	{
		JFileChooser fc = new JOverwriteCheckFileChooser("Speichern unter");
		//Last used Directory
		if (!GeneralPreferences.getInstance().getStringValue("graph.lastfile").equals("$NONE"))
			fc.setCurrentDirectory(new File(GeneralPreferences.getInstance().getStringValue("graph.lastfile")).getParentFile());
		
		FileFilter graphml = new SimpleFilter("xml","GravelML (.xml)");
		fc.removeChoosableFileFilter(fc.getFileFilter());
		fc.addChoosableFileFilter(graphml);
		fc.setFileHidingEnabled(true); 
		saveVisual = GeneralPreferences.getInstance().getStringValue("graph.fileformat").equals("visual");
		fc.setFileFilter(graphml);	
		int returnVal = fc.showSaveDialog(Gui.getInstance().getParentWindow());
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			File f = fc.getSelectedFile();
			GravelMLWriter iw = new GravelMLWriter(vGc.getVGraph());
			
			SaveAsDialog sad = new SaveAsDialog(Gui.getInstance().getParentWindow());
			if (sad.IsAccepted())
			{
				//Dialog visual or not
				saveVisual = sad.IsVisual();
				if (saveVisual)
				{					
					GeneralPreferences.getInstance().setStringValue("graph.fileformat","visual");
					if (iw.saveVisualToFile(f).equals(""))
					{
						GeneralPreferences.getInstance().setStringValue("graph.lastfile",f.getAbsolutePath());
						//Observe VGraph
						vGc.getVGraph().getMathGraph().deleteObserver(this);
						vGc.getVGraph().addObserver(this);
						Gui.getInstance().getParentWindow().setTitle(Gui.WindowName+" - "+f.getName());
						actualgraphsaved = true;
					}
				}
				else
				{
					GeneralPreferences.getInstance().setStringValue("graph.fileformat","math");				
					if (iw.saveMathToFile(f).equals(""))
					{
						GeneralPreferences.getInstance().setStringValue("graph.lastfile",f.getAbsolutePath());
						//Observe MGraph
						vGc.getVGraph().getMathGraph().addObserver(this);
						vGc.getVGraph().deleteObserver(this);
						Gui.getInstance().getParentWindow().setTitle(Gui.WindowName+" - "+f.getName()+" (math only)");
						actualgraphsaved = true;
					}
				}
			} //End of SaveAsAccepted
			return true;
		}
		//Chosen Cancel
		return false;
	}

	/**
	 * Export an File
	 *	PNG
	 *	TeX
	 *rest still todo
	 */
	public boolean Export()
	{
		JFileChooser fc = new JOverwriteCheckFileChooser("Exportieren");
		//Wenn man schon nen File hat das Directory davon verwenden
		FileFilter png = new SimpleFilter("PNG","Portable Network Graphics");
		FileFilter tex = new SimpleFilter("TEX","LaTeX-Picture-Grafik");
		FileFilter svg = new SimpleFilter("SVG","Scalable Vector Graphics");
		fc.addChoosableFileFilter(png);
		fc.addChoosableFileFilter(tex);
		fc.addChoosableFileFilter(svg);
		if (!GeneralPreferences.getInstance().getStringValue("graph.lastfile").equals("$NONE"))
			fc.setCurrentDirectory(new File(GeneralPreferences.getInstance().getStringValue("graph.lastfile")).getParentFile());

		int returnVal = fc.showSaveDialog(Gui.getInstance().getParentWindow());
		   if (returnVal == JFileChooser.APPROVE_OPTION)
		   {
			    File f = fc.getSelectedFile();
	    		if (png.accept(f))
	    		{
	    			ExportPNGDialog esvgd = new ExportPNGDialog(Gui.getInstance().getParentWindow(),
	    					(vGc.getVGraph().getMaxPoint(vGc.getGraphics()).x-vGc.getVGraph().getMinPoint(vGc.getGraphics()).x),
	    					(vGc.getVGraph().getMaxPoint(vGc.getGraphics()).y-vGc.getVGraph().getMinPoint(vGc.getGraphics()).y));
	    			if (esvgd.IsAccepted())
	    			{
	    				PNGWriter iw = new PNGWriter(vGc); 
	    				iw.PNGExport(f,esvgd.getSizeX(),esvgd.getSizeY());
	    				return true;
	    			}
	    		}
	    		if (svg.accept(f))
	    		{
	    			ExportSVGDialog esvgd = new ExportSVGDialog(Gui.getInstance().getParentWindow(),
	    					(vGc.getVGraph().getMaxPoint(vGc.getGraphics()).x-vGc.getVGraph().getMinPoint(vGc.getGraphics()).x),
	    					(vGc.getVGraph().getMaxPoint(vGc.getGraphics()).y-vGc.getVGraph().getMinPoint(vGc.getGraphics()).y));
	    			if (esvgd.IsAccepted())
	    			{
	    				SVGWriter iw = new SVGWriter(vGc,esvgd.getSizeX(),""); 
	    				iw.saveToFile(f);
	    				return true;
	    			}
	    		}
	    		else if (tex.accept(f))
	    		{
	    			ExportTeXDialog etexd = new ExportTeXDialog(Gui.getInstance().getParentWindow(),
	    					(vGc.getVGraph().getMaxPoint(vGc.getGraphics()).x-vGc.getVGraph().getMinPoint(vGc.getGraphics()).x),
	    					(vGc.getVGraph().getMaxPoint(vGc.getGraphics()).y-vGc.getVGraph().getMinPoint(vGc.getGraphics()).y));
	    			if (etexd.IsAccepted())
	    			{
	    				String type="";
	    				if (etexd.IsWholeDocument())
	    					type="doc";
	    				else if (etexd.IsOnlyFigure())
	    					type="fig";
	    				TeXWriter lp;
	    				if (etexd.IsPlainTeX())
	    					lp = new MyLaTeXPictureWriter(vGc,etexd.getSizeX(),type);
	    				else
	    					lp = new MyTikZPictureWriter(vGc,etexd.getSizeX(),type);
	    				String error = lp.saveToFile(f);
		    			if (error.equals(""))
		    				return true;
		    			JOptionPane.showMessageDialog(Gui.getInstance().getParentWindow(),"<html>Beim Exportieren des Graphen ist folgener Fehler aufgetreten: <br>"+error,"Fehler",JOptionPane.ERROR_MESSAGE);
	    			}
	    		}
	    		else
	    		{
	    			String s = "'."+getExtension(f)+"'";
	    			if (getExtension(f)==null)
	    				s="<i>leer</i>";
	    			JOptionPane.showMessageDialog(Gui.getInstance().getParentWindow(),"<html>Das Format "+s+" wird nicht unterstützt.<br><br>Unterst"+main.CONST.html_ue+"tzte Formate:<br>-PNG (.png) <br>-LaTeX (.tex) ","Fehler",JOptionPane.ERROR_MESSAGE);
	    		}
	       }	   
		   return false;
	}

	public boolean isGraphSaved()
	{
		return actualgraphsaved;
	}
	private boolean SaveOnNewOrOpen()
	{
		if ((!actualgraphsaved)&&(vGc.getVGraph().NodeCount()>0)) //save
        {
        	if (GeneralPreferences.getInstance().getStringValue("graph.lastfile").equals("$NONE"))
        	{ //SaveAs anbieten
				int sel = JOptionPane.showConfirmDialog(Gui.getInstance().getParentWindow(), "<html><p>Der aktuelle Graph ist nicht gespeichert worden.M"+main.CONST.html_oe+"chten Sie den Graph noch speichern ?</p></html>"
						, "Graph nicht gespeichert", JOptionPane.YES_NO_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE);
			if (sel == JOptionPane.CANCEL_OPTION)
				return false;
			if(sel == JOptionPane.YES_OPTION)
				SaveAs();
        	}
        	else //Sonst Save anbieten
        	{
        		File f = new File(GeneralPreferences.getInstance().getStringValue("graph.lastfile"));
				int sel = JOptionPane.showConfirmDialog(Gui.getInstance().getParentWindow(), "<html><p>Die letzten "+main.CONST.html_Ae+"nderungen an<br><i>"+f.getName()+"</i><br>wurden nicht gespeichert. M"+main.CONST.html_oe+"chten Sie diese jetzt speichern ?</p></html>"
						, "Graph nicht gespeichert", JOptionPane.YES_NO_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE);
				if (sel == JOptionPane.CANCEL_OPTION)
					return false;
				if(sel == JOptionPane.YES_OPTION)
					if (!Save())
						System.err.println("Internal Save error!");
        	}
        }
		//No one aborted so return true
		return true;
	}

	public void NewGraph() 
	{
		if (!SaveOnNewOrOpen()) //s.o. aborted
			return;
		VGraph vg = new VGraph(GeneralPreferences.getInstance().getBoolValue("graph.directed"),GeneralPreferences.getInstance().getBoolValue("graph.allowloops"),GeneralPreferences.getInstance().getBoolValue("graph.allowmultiple"));
		
		GeneralPreferences.getInstance().setStringValue("graph.lastfile","$NONE");
		vGc.getVGraph().replace(vg);
		vGc.getVGraph().getMathGraph().deleteObserver(this);
		vGc.getVGraph().addObserver(this);
		Gui.getInstance().getParentWindow().setTitle(Gui.WindowName);
	}	
	
	public void update(Observable arg0, Object arg1) {
		if (!actualgraphsaved) //Not saved yet
			return;
		
		String todo = (String)arg1;
		if (todo.contains("N") || todo.contains("E") || todo.contains("S"))
		{	//Graph Changed => not Saved anymore
			actualgraphsaved = false;
			if (!GeneralPreferences.getInstance().getStringValue("graph.lastfile").equals("$NONE"))
			{
				if (saveVisual)
				{
					Gui.getInstance().getParentWindow().setTitle(Gui.WindowName+" - "+(new File(GeneralPreferences.getInstance().getStringValue("graph.lastfile")).getName())+"*");
				}
				else
				{
					Gui.getInstance().getParentWindow().setTitle(Gui.WindowName+" - "+(new File(GeneralPreferences.getInstance().getStringValue("graph.lastfile")).getName())+"* (math only)");					
				}
			}
			else
				Gui.getInstance().getParentWindow().setTitle(Gui.WindowName);									
		}
	}

}
