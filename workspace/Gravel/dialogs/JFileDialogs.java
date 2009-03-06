package dialogs;

import io.GeneralPreferences;
import io.PNGWriter;
import io.GravelMLReader;
import io.SVGWriter;
import io.TeXWriter;
import io.LaTeXPictureWriter;
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
import model.VGraphInterface;
import model.VHyperGraph;

import view.Gui;
import view.VCommonGraphic;
import view.VGraphic;
import view.VHyperGraphic;

/**
 * JFileDialogs provides all Dialogs concerning loading/saving and the questions for Loading/Saving of Graphs
 * Observes the Changes in the actually edited (Hyper)graph to indicate whether it was saved yet or not.
 * 
 * Features
 * - Loading of GravelML (without handling pure mathematical graphs, because there is no algorithm/Dialog for that yet)
 * - Save as and Save in GravelML ith questioning of saving format (pure math or all)
 * - Exports to
 * 	- PNG
 * 	- LaTeX	Picture Export
 * 	- SVG	Export als Vektorgrafik

 * Ideas for further Exports: GraphML, .dot, TikZ,...?
 * Ideas for Imports: GraphML, .dot, GML,...?
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
	
	private boolean saveVisual = true;
	
	private VCommonGraphic vGc;
	private int GraphType;
	private VGraphInterface vG;
	private boolean actualState;
	/**
	 * Constructor 
	 * @param pvg the actual graph editor Component
	 * @param programmstart initialize graphsaved
	 */
	public JFileDialogs(VCommonGraphic pvg)
	{
		GraphType = pvg.getType();
		vGc = pvg;
		if (GraphType==VCommonGraphic.VGRAPHIC)
		{
			vG = ((VGraphic)vGc).getGraph(); 
			((VGraphic)vGc).getGraph().addObserver(this);
		}
		else
		{
			vG = ((VHyperGraphic)vGc).getGraph(); 
			((VHyperGraphic)vGc).getGraph().addObserver(this);
		}
		actualState=vGc.getGraphHistoryManager().IsGraphUnchanged();
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
					if (GraphType==VCommonGraphic.VGRAPHIC)
						((VGraphic)vGc).getGraph().addObserver(this);
					else
						((VHyperGraphic)vGc).getGraph().addObserver(this);
	    			//Set actual State saved.
	    			vGc.getGraphHistoryManager().setGraphSaved();
	    			return true;
	    		}
	    		else
	    		{
	    			System.err.println("DEBUG : MGraph geladen, TODO Wizard hier einbauen.");
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
		GravelMLWriter iw = new GravelMLWriter(((VGraphic)vGc).getGraph()); //Save the actual reference
		//TODO Change to the next line if MLWriter done
		//GravelMLWriter iw = new GravelMLWriter(vG);
		
		saveVisual = GeneralPreferences.getInstance().getStringValue("graph.fileformat").equals("visual");
		if (saveVisual)
		{
			if (iw.saveVisualToFile(f).equals("")) //Saving successfull
			{
				Gui.getInstance().getParentWindow().setTitle(Gui.WindowName+" - "+f.getName());
    			//Set actual State saved.
    			vGc.getGraphHistoryManager().setGraphSaved();
				return true;
			}
		}
		else
		{
			if (iw.saveMathToFile(f).equals("")) //Saving sucessful
			{
				Gui.getInstance().getParentWindow().setTitle(Gui.WindowName+" - "+f.getName()+" (math only)");
    			//Set actual State saved.
    			vGc.getGraphHistoryManager().setGraphSaved();
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
			GravelMLWriter iw = new GravelMLWriter(((VGraphic)vGc).getGraph()); //Save the actual reference
			//TODO Change to the next line if MLWriter done
			//GravelMLWriter iw = new GravelMLWriter(vG);
			
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
						if (GraphType==VCommonGraphic.VGRAPHIC)
							((VGraphic)vGc).getGraph().addObserver(this);
						else
							((VHyperGraphic)vGc).getGraph().addObserver(this);
						Gui.getInstance().getParentWindow().setTitle(Gui.WindowName+" - "+f.getName());
		    			//Set actual State saved.
		    			vGc.getGraphHistoryManager().setGraphSaved();
					}
				}
				else
				{
					GeneralPreferences.getInstance().setStringValue("graph.fileformat","math");				
					if (iw.saveMathToFile(f).equals(""))
					{
						GeneralPreferences.getInstance().setStringValue("graph.lastfile",f.getAbsolutePath());
						//Observe MGraph
						if (GraphType==VCommonGraphic.VGRAPHIC)
							((VGraphic)vGc).getGraph().addObserver(this);
						else
							((VHyperGraphic)vGc).getGraph().addObserver(this);
						Gui.getInstance().getParentWindow().setTitle(Gui.WindowName+" - "+f.getName()+" (math only)");
		    			//Set actual State saved.
		    			vGc.getGraphHistoryManager().setGraphSaved();
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
	 *  PNG
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
	    					(vG.getMaxPoint(vGc.getGraphics()).x-vG.getMinPoint(vGc.getGraphics()).x),
	    					(vG.getMaxPoint(vGc.getGraphics()).y-vG.getMinPoint(vGc.getGraphics()).y));
	    			if (esvgd.IsAccepted())
	    			{
	    				PNGWriter iw = new PNGWriter(vGc); 
	    				iw.PNGExport(f,esvgd.getSizeX(),esvgd.getSizeY(),esvgd.isTransparent());
	    				return true;
	    			}
	    		}
	    		else if (svg.accept(f))
	    		{
	    			ExportSVGDialog esvgd = new ExportSVGDialog(Gui.getInstance().getParentWindow(),
	    					(vG.getMaxPoint(vGc.getGraphics()).x-vG.getMinPoint(vGc.getGraphics()).x),
	    					(vG.getMaxPoint(vGc.getGraphics()).y-vG.getMinPoint(vGc.getGraphics()).y));
	    			if (esvgd.IsAccepted())
	    			{
	    				SVGWriter iw = new SVGWriter(vGc,esvgd.getSizeX()); 
	    				iw.saveToFile(f);
	    				return true;
	    			}
	    		}
	    		else if (tex.accept(f))
	    		{
	    			ExportTeXDialog etexd = new ExportTeXDialog(Gui.getInstance().getParentWindow(),
	    					(vG.getMaxPoint(vGc.getGraphics()).x-vG.getMinPoint(vGc.getGraphics()).x),
	    					(vG.getMaxPoint(vGc.getGraphics()).y-vG.getMinPoint(vGc.getGraphics()).y));
	    			if (etexd.IsAccepted())
	    			{
	    				String type="";
	    				if (etexd.IsWholeDocument())
	    					type="doc";
	    				else if (etexd.IsOnlyFigure())
	    					type="fig";
	    				TeXWriter lp;
//	    				if (etexd.IsPlainTeX())
	    					lp = new LaTeXPictureWriter(vGc,etexd.getSizeX(),type);
//	    				else
//	    				{	    			
//	    					lp = new MyTikZPictureWriter(vGc,etexd.getSizeX(),type);
//	    				}
	    				String error = lp.saveToFile(f);
		    			if (error.equals(""))
		    				return true;
		    			JOptionPane.showMessageDialog(Gui.getInstance().getParentWindow(),"<html>Beim Exportieren des Graphen ist folgener Fehler aufgetreten: <br>"+error+"</html>","Fehler",JOptionPane.ERROR_MESSAGE);
	    			}
	    		}
	    		else
	    		{
	    			String s = "'."+getExtension(f)+"'";
	    			if (getExtension(f)==null)
	    				s="<i>leer</i>";
	    			JOptionPane.showMessageDialog(Gui.getInstance().getParentWindow(),"<html>Das Format "+s+" wird nicht unterstützt.<br><br>Unterst"+main.CONST.html_ue+"tzte Formate:<br>-LaTeX (.tex)<br>-PNG (.png)<br>-SVG (.svg)","Fehler",JOptionPane.ERROR_MESSAGE);
	    		}
	       }	   
		   return false;
	}
	/**
	 * Get the actual status of the graph
	 * @return true if the current Graph is saved in a file, else false
	 */
	public boolean isGraphSaved()
	{
		return vGc.getGraphHistoryManager().IsGraphUnchanged();
	}
	/**
	 * Ask on File->New or File->Open to Save an unsaved Graph
	 * and return, whether it was saved or the action was canceled.
	 * @return true if the graph was saved or Saving was denied, false if the action was canceled
	 */
	private boolean SaveOnNewOrOpen()
	{
		boolean existsNode=false;
		if (vG.getType()==VGraphInterface.GRAPH)
			existsNode = ((VGraph)vG).getMathGraph().modifyNodes.cardinality()>0;
		else if (vG.getType()==VGraphInterface.HYPERGRAPH)
			existsNode = ((VHyperGraph)vG).getMathGraph().modifyNodes.cardinality()>0;
			
		if ((!vGc.getGraphHistoryManager().IsGraphUnchanged())&&(existsNode)) //saved/no changes
        {
        	if (GeneralPreferences.getInstance().getStringValue("graph.lastfile").equals("$NONE"))
        	{ //SaveAs anbieten
				int sel = JOptionPane.showConfirmDialog(Gui.getInstance().getParentWindow(), "<html><p>Der aktuelle Graph ist nicht gespeichert worden.<br>M"+main.CONST.html_oe+"chten Sie den Graph noch speichern ?</p></html>"
						, "Graph nicht gespeichert", JOptionPane.YES_NO_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE);
			if (sel == JOptionPane.CANCEL_OPTION)
				return false;
			if(sel == JOptionPane.YES_OPTION)
				SaveAs();
        	}
        	else //Sonst Save anbieten
        	{
        		File f = new File(GeneralPreferences.getInstance().getStringValue("graph.lastfile"));
				int sel = JOptionPane.showConfirmDialog(Gui.getInstance().getParentWindow(), "<html><p>Die letzten "+main.CONST.html_Ae+"nderungen an<br><i>"+f.getName()+"</i><br>wurden nicht gespeichert.<br>M"+main.CONST.html_oe+"chten Sie diese jetzt speichern ?</p></html>"
						, "Graph nicht gespeichert", JOptionPane.YES_NO_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE);
				if (sel == JOptionPane.CANCEL_OPTION)
					return false;
				if(sel == JOptionPane.YES_OPTION)
					Save();
        	}
        }
		//No one aborted so return true
		return true;
	}
	/**
	 * Create a New Graph in the GUI
	 *
	 */
	public void NewGraph() 
	{
		if (!SaveOnNewOrOpen()) //s.o. aborted
			return;
		VGraph vg = new VGraph(GeneralPreferences.getInstance().getBoolValue("graph.directed"),GeneralPreferences.getInstance().getBoolValue("graph.allowloops"),GeneralPreferences.getInstance().getBoolValue("graph.allowmultiple"));
		
		GeneralPreferences.getInstance().setStringValue("graph.lastfile","$NONE");
		//Deactivate HistoryStuff
		Gui.getInstance().setVGraph(vg); //This should kill us if the graphtype changed
		//Reset (and with that reactivate History
		Gui.getInstance().getParentWindow().setTitle(Gui.WindowName);
	}	
	/**
	 * Handle Graph Updates and check, whether a loaded graph is still saved
	 */
	public void update(Observable arg0, Object arg1) {
		if (this.actualState!=vGc.getGraphHistoryManager().IsGraphUnchanged()) //State Changed
			actualState = vGc.getGraphHistoryManager().IsGraphUnchanged();
		else
			return;
		String newtitle="";
		if (actualState) //Graph is unchanged again
		{			
			if (!GeneralPreferences.getInstance().getStringValue("graph.lastfile").equals("$NONE"))
			{	if (saveVisual)
					newtitle = Gui.WindowName+" - "+(new File(GeneralPreferences.getInstance().getStringValue("graph.lastfile")).getName())+"";
				else
					newtitle = Gui.WindowName+" - "+(new File(GeneralPreferences.getInstance().getStringValue("graph.lastfile")).getName())+" (math only)";					
			}
			else
				newtitle = Gui.WindowName;
        	if (System.getProperty("os.name").toLowerCase().indexOf("mac")!=-1) //Back to X as close
        		Gui.getInstance().getParentWindow().getRootPane().putClientProperty( "Window.documentModified", Boolean.FALSE );
		}		
		else //Graph not saved
		{
			if (!GeneralPreferences.getInstance().getStringValue("graph.lastfile").equals("$NONE"))
			{	if (saveVisual)
					newtitle = Gui.WindowName+" - "+(new File(GeneralPreferences.getInstance().getStringValue("graph.lastfile")).getName())+"*";
				else
					newtitle = Gui.WindowName+" - "+(new File(GeneralPreferences.getInstance().getStringValue("graph.lastfile")).getName())+"* (math only)";					
			}
			else
				newtitle = Gui.WindowName;
        	if (System.getProperty("os.name").toLowerCase().indexOf("mac")!=-1) //Back to Circle on CLose
        		Gui.getInstance().getParentWindow().getRootPane().putClientProperty( "Window.documentModified", Boolean.TRUE );

		}
		Gui.getInstance().getParentWindow().setTitle(newtitle);
	}

}
