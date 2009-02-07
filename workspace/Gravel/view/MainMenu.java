  package view;

import help.view.HelpPanel;
import history.GraphHistoryManager;
import io.GeneralPreferences;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Observable;
import java.util.Observer;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;

import model.GraphMessage;

import dialogs.JFileDialogs;
import dialogs.JPreferencesDialog;
import dialogs.JSelectionModifyDialog;

import algorithm.AlgorithmFactory;

import view.pieces.GridComponent;

public class MainMenu extends JMenuBar implements ActionListener, Observer
{	
	private static final long serialVersionUID = 1L;
	//	Die Menüeinträge
	//Datei Visualisiere, Algorithemn, About
	JMenu mFile, mEdit, mView,mEdModus,mEdZoom, mAlg, mAlgV, mHelp,mVGraph;
	//Die einzelnen Menüeinträge
	JMenuItem mFExit, mFNew, mFOpen, mFWinPrefs,mFSave, mFSaveAs, mFExport;
	JCheckBoxMenuItem mVShowBP;
	JRadioButtonMenuItem mVModusNormal,mVModusOCM, mVZoom1,mVZoom2,mVZoom3;
	JMenuItem mVGrid, mVGDirCh, mVGLoopCh, mVGMultipleCh;
	JMenuItem mEdDelSelection,mEdModifySelection, mEdArrangeSelection;
	JMenuItem mEdUndo, mEdRedo;
	JMenuItem mAVTest,mAVLTD, mAVMAS;
	JMenuItem mHIndex,mHAbout;
	JFileDialogs fileDialogs;
	VGraphic graphpart;
	GraphHistoryManager GraphHistory;
	
	boolean isMac;
	public MainMenu(VGraphic vgraphic)
	{
        this.graphpart = vgraphic;
        GraphHistory = graphpart.getGraphHistoryManager();
        isMac = (System.getProperty("os.name").toLowerCase().indexOf("mac")!=-1);
        createMenuBar();
		fileDialogs = new JFileDialogs(vgraphic);
		setOpaque(true);
        setPreferredSize(new Dimension(200, 20));
        graphpart.getVGraph().addObserver(this);
 	}
	@SuppressWarnings("deprecation")
	private void createMenuBar() {
		int MenuAccModifier;
		
		MenuAccModifier = java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
		
		//Das erste Menü : Datei
        if (isMac)
        {
        	mFile = new JMenu("Ablage");
        }
        else
        {
        	mFile = new JMenu("Datei");
        	mFile.setMnemonic(KeyEvent.VK_D);         
        }
        add(mFile);
        //Datei öffnen
        mFNew = new JMenuItem("Neuer Graph");
        mFNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, MenuAccModifier));
        if (!isMac) mFNew.setMnemonic(KeyEvent.VK_N);
        mFNew.addActionListener(this);
        mFile.add(mFNew);
        
        mFOpen = new JMenuItem("Öffnen...");
        mFOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, MenuAccModifier));
        mFOpen.addActionListener(this);
        if (!isMac) mFOpen.setMnemonic(KeyEvent.VK_O);
        mFile.add(mFOpen);
        
        mFSave = new JMenuItem("Speichern");
        mFSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, MenuAccModifier));
        //aktiv setzen, falls da ein File ist.
        mFSave.setEnabled(!GeneralPreferences.getInstance().getStringValue("graph.lastfile").equals("$NONE"));
        mFSave.addActionListener(this);
        if (!isMac) mFSave.setMnemonic(KeyEvent.VK_S);
        mFile.add(mFSave);
        
        mFSaveAs = new JMenuItem("Speichern unter...");
        mFSaveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, (java.awt.event.InputEvent.SHIFT_MASK | MenuAccModifier)));
        //aktiv setzen, falls da ein File ist.
        if (!isMac) mFSaveAs.setMnemonic(KeyEvent.VK_A);
        mFSaveAs.addActionListener(this);
        mFile.add(mFSaveAs);

        if (!isMac)
        {
        	//Nur fuer Windows : Datei Einstellungen, beim Mac ist das im Mac Menü
        	mFWinPrefs = new JMenuItem("Einstellungen...");
        	mFWinPrefs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, MenuAccModifier));
        	mFWinPrefs.addActionListener(this);
        	mFWinPrefs.setMnemonic(KeyEvent.VK_P);
        	mFile.add(mFWinPrefs);
        }
        
        mFExport = new JMenuItem("Exportieren...");
        mFExport.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, MenuAccModifier));
        mFExport.addActionListener(this);
        if (!isMac) mFExport.setMnemonic(KeyEvent.VK_E);
        mFile.add(mFExport);        
       
        if (!isMac)
        {
        	mFile.addSeparator();

        	//Datei Beenden
        	mFExit = new JMenuItem("Beenden");
        	mFExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, MenuAccModifier));
            if (!isMac) mFExit.setMnemonic(KeyEvent.VK_Q);
        	mFExit.addActionListener(this);
        	mFile.add(mFExit);
        }
        //Menüpunkt bearbeiten
        mEdit = new JMenu("Bearbeiten");
        if (!isMac) mEdit.setMnemonic(KeyEvent.VK_E);
        add(mEdit);

        mEdUndo = new JMenuItem("Wiederrufen");
       	mEdUndo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z,MenuAccModifier));
        if (!isMac)
        	mEdUndo.setMnemonic(KeyEvent.VK_U);
        mEdUndo.addActionListener(this);
        mEdUndo.setEnabled(GraphHistory.CanUndo());
        
        mEdRedo = new JMenuItem("Wiederholen");
       	mEdRedo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, (java.awt.event.InputEvent.SHIFT_MASK | MenuAccModifier)));
        if (!isMac)
        	mEdRedo.setMnemonic(KeyEvent.VK_R);
        mEdRedo.addActionListener(this);
        mEdRedo.setEnabled(GraphHistory.CanRedo());

        //Delete Selection
        mEdDelSelection = new JMenuItem("Selektion löschen");
    	if (isMac)
    		mEdDelSelection.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, MenuAccModifier));    		
    	else
    	{
    		mEdDelSelection.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE,0));
            mEdDelSelection.setMnemonic(KeyEvent.VK_D);
    	}
    	mEdDelSelection.addActionListener(this);
    	mEdDelSelection.setEnabled(graphpart.getVGraph().selectedEdgeExists()||graphpart.getVGraph().selectedNodeExists());
    	
    	mEdModifySelection = new JMenuItem("Selektion bearbeiten...");
   		mEdModifySelection.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, MenuAccModifier));    		
       	if (!isMac)
           mEdModifySelection.setMnemonic(KeyEvent.VK_M);
    	mEdModifySelection.addActionListener(this);
    	mEdModifySelection.setEnabled(graphpart.getVGraph().selectedEdgeExists()||graphpart.getVGraph().selectedNodeExists());

    	mEdArrangeSelection = new JMenuItem("Selektion anordnen...");
   		//mEdArrangeSelection.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, MenuAccModifier));    		
       	if (!isMac)
           mEdArrangeSelection.setMnemonic(KeyEvent.VK_R);
    	mEdArrangeSelection.addActionListener(this);
    	mEdArrangeSelection.setEnabled(graphpart.getVGraph().selectedNodeExists());

    	
    	if (!isMac) mEdit.setMnemonic(KeyEvent.VK_E);
    	mEdit.add(mEdUndo);
    	mEdit.add(mEdRedo);
    	
    	mEdit.add(mEdDelSelection);        
        mEdit.add(mEdModifySelection);
        mEdit.add(mEdArrangeSelection);
        //Menüpunkt Ansicht
        mView = new JMenu("Ansicht");
        if (!isMac) mView.setMnemonic(KeyEvent.VK_A);
        add(mView);

//      Editor Umformung directed / undirected Graph
        mVGraph = new JMenu("Graph umformen (zu)");
        if (!isMac) mVGraph.setMnemonic(KeyEvent.VK_G);
        mView.add(mVGraph);
        
        if (graphpart.getVGraph().isDirected())
        	mVGDirCh = new JMenuItem("ungerichtet");
        else
        	mVGDirCh = new JMenuItem("umformen zu gerichtetem Graph");        	
        mVGDirCh.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, MenuAccModifier));
        mVGDirCh.addActionListener(this);
        if (!isMac) mVGDirCh.setMnemonic(KeyEvent.VK_D);
        mVGraph.add(mVGDirCh);        

        if (graphpart.getVGraph().isLoopAllowed())
        	mVGLoopCh = new JMenuItem("entferne Schleifen");
        else
        	mVGLoopCh = new JMenuItem("erlaube Schleifen");        	
        mVGLoopCh.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, MenuAccModifier));
        mVGLoopCh.addActionListener(this);
        if (!isMac) mVGLoopCh.setMnemonic(KeyEvent.VK_L);
        mVGraph.add(mVGLoopCh);        
       
        if (graphpart.getVGraph().isMultipleAllowed())
        	mVGMultipleCh = new JMenuItem("entferne Mehrfachkanten");
        else
        	mVGMultipleCh = new JMenuItem("erlaube Mehrfachkanten");        	
        //mVGMultipleCh.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, MenuAccModifier));
        mVGMultipleCh.addActionListener(this);
        if (!isMac) mVGMultipleCh.setMnemonic(KeyEvent.VK_M);
        mVGraph.add(mVGMultipleCh);        
        
        //--Editor Modus--
        mEdModus = new JMenu("Modus");
        if (!isMac) mEdModus.setMnemonic(KeyEvent.VK_M);
        mView.add(mEdModus);
        //normal
        mVModusNormal = new JRadioButtonMenuItem("Standard");
        mVModusNormal.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_4, MenuAccModifier));
        mVModusNormal.addActionListener(this);
        if (!isMac) mVModusNormal.setMnemonic(KeyEvent.VK_S);
        mEdModus.add(mVModusNormal);
        mVModusOCM = new JRadioButtonMenuItem("One-Click");
        mVModusOCM.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_5, MenuAccModifier));
        mVModusOCM.addActionListener(this);
        if (!isMac) mVModusOCM.setMnemonic(KeyEvent.VK_O);
        mEdModus.add(mVModusOCM);
        ButtonGroup group = new ButtonGroup();
        group.add(mVModusNormal);
        group.add(mVModusOCM);
        mVModusNormal.setSelected(true);
        //group.setSelected(mEdModusNormal, true);
        
//      Editor Kontrollpunkte der Kanten
        mVShowBP = new JCheckBoxMenuItem("Kanten-Kontrollpunkte anzeigen");
        mVShowBP.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, MenuAccModifier));
        mVShowBP.setSelected(GeneralPreferences.getInstance().getBoolValue("vgraphic.cpshow"));
        if (!isMac) mVShowBP.setMnemonic(KeyEvent.VK_A);
        mVShowBP.addActionListener(this);
        mView.add(mVShowBP);        
        
        //Editor Raster
        mVGrid = new JMenuItem("Raster-Einstellungen...");
        mVGrid.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, MenuAccModifier));
        mVGrid.addActionListener(this);
        if (!isMac) mVGrid.setMnemonic(KeyEvent.VK_R);
        mView.add(mVGrid);        
        
//      --Editor Modus--
        mEdZoom = new JMenu("Maßstab");
        if (!isMac) mEdModus.setMnemonic(KeyEvent.VK_Z);
        mView.add(mEdZoom);
        //normal
        mVZoom1 = new JRadioButtonMenuItem("50%");
        mVZoom1.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, MenuAccModifier));
        mVZoom1.addActionListener(this);
        if (!isMac) mVZoom1.setMnemonic(KeyEvent.VK_1);
        mEdZoom.add(mVZoom1);
        mVZoom2 = new JRadioButtonMenuItem("100%");
        mVZoom2.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, MenuAccModifier));
        mVZoom2.setSelected(true);
        if (!isMac) mEdZoom.setMnemonic(KeyEvent.VK_2);
        mVZoom2.addActionListener(this);
        mEdZoom.add(mVZoom2);
        mVZoom3 = new JRadioButtonMenuItem("200%");
        mVZoom3.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_3, MenuAccModifier));
        mVZoom3.addActionListener(this);
        if (!isMac) mEdModus.setMnemonic(KeyEvent.VK_3);
        mEdZoom.add(mVZoom3);
        ButtonGroup zoomgroup = new ButtonGroup();
        zoomgroup.add(mVZoom1);
        zoomgroup.add(mVZoom2);
        zoomgroup.add(mVZoom3); 
        
        
        //Menüpunkt Algorithmen
        mAlg = new JMenu("Algorithmen");
        if (!isMac) mAlg.setMnemonic(KeyEvent.VK_A);
        mAlg.getAccessibleContext().setAccessibleDescription(
                "Algorithmen in dem Graphen (oder in Mengen)");
        add(mAlg);
        mAlgV = new JMenu("Visualisiere");
        mAlg.add(mAlgV);
        mAVTest = new JMenuItem(AlgorithmFactory.RANDOM_VISUALIZE+"...");
        mAVTest.addActionListener(this);
        mAlgV.add(mAVTest);
 
        mAVLTD = new JMenuItem(AlgorithmFactory.LAYERED_TREE_DRAW+"...");
        mAVLTD.addActionListener(this);
        mAlgV.add(mAVLTD);

        mAVMAS = new JMenuItem(AlgorithmFactory.SPRINGS_AND_MAGNETISM+"...");
        mAVMAS.addActionListener(this);
        mAlgV.add(mAVMAS);

        //Letzter Menüpunkt : Hilfe.
        mHelp = new JMenu("Hilfe");
        if (!isMac) mHelp.setMnemonic(KeyEvent.VK_H);
        add(mHelp);
        mHIndex = new JMenuItem("Index",KeyEvent.VK_I);
        /*if (isMac)
        {
        	mHIndex.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, KeyEvent.SHIFT_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        }
        else
        { */
        	mHIndex.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1,0));
        //}
        mHIndex.getAccessibleContext().setAccessibleDescription("Hilfe-Index");
        mHIndex.addActionListener(this);
        if (!isMac) mHIndex.setMnemonic(KeyEvent.VK_I);
        mHelp.add(mHIndex);
        if (!isMac)
        {
        	mHAbout = new JMenuItem("Über",KeyEvent.VK_B);
        	mHAbout.getAccessibleContext().setAccessibleDescription(
        			main.CONST.utf8_Ue+"ber Gravel</html>");
        	mHAbout.addActionListener(this);
            if (!isMac) mHAbout.setMnemonic(KeyEvent.VK_A);
        	mHelp.add(mHAbout);
        }
    }
	public void actionPerformed(ActionEvent e) 
	{
		if (e.getSource() instanceof JMenuItem) 
    	{
			JMenuItem item = (JMenuItem) e.getSource();
			if (item == mFNew)
    			fileDialogs.NewGraph(); 
			else if (item == mFOpen)
    			mFSave.setEnabled(fileDialogs.Open()); //If Successfull set Save to true
			else if (item == mFSave)
    			mFSave.setEnabled(fileDialogs.Save());
			else if (item == mFSaveAs)
        		mFSave.setEnabled(fileDialogs.SaveAs());
			else if (item == mFWinPrefs)
			{
		    	new JPreferencesDialog();
		    	Gui.getInstance().getVGraph().pushNotify(new GraphMessage(GraphMessage.SELECTION|GraphMessage.ALL_ELEMENTS,GraphMessage.UPDATE));
			}
			else if (item == mFExport)
    			fileDialogs.Export();
			else
			//Datei Beenden angeklickt
			if (item == mFExit)
    	    {
    			Gui.getInstance().doQuit();
    	    } else 
    	    if (item == mVShowBP)
    		{
    	       GeneralPreferences.getInstance().setBoolValue("vgraphic.cpshow",mVShowBP.isSelected());
    		} else
    	    if (item == mVModusNormal)
    	    {
    	    	graphpart.setMouseHandling(VGraphic.STD_MOUSEHANDLING);
    	    } else
    	    if (item == mVModusOCM)
        	  	graphpart.setMouseHandling(VGraphic.OCM_MOUSEHANDLING);
        	else if (item == mVZoom1)
     	       GeneralPreferences.getInstance().setIntValue("vgraphic.zoom",50);
        	else if (item == mVZoom2)
        		GeneralPreferences.getInstance().setIntValue("vgraphic.zoom",100);
        	else if (item == mVZoom3)
        		GeneralPreferences.getInstance().setIntValue("vgraphic.zoom",200);
        	else if (item == mVGrid)
    	    {
    	    	GridComponent g = new GridComponent();
    	    	graphpart.addPiece("Grid",g);
    	    	g.showDialog(true);
    	    	
    	    	graphpart.removePiece("Grid");
    	    } else 
    	    if (item==mVGDirCh)
    	    {
    	    	if (graphpart.getVGraph().isDirected())
    	    	{
    				int n = JOptionPane.showConfirmDialog(Gui.getInstance().getParentWindow(), "<html>Wirklich umformen in ungerichteten Graphen ?<br>Dabei k"+main.CONST.html_oe+"nnen Kanten verloren gehen.","Umformen bestätigen",JOptionPane.YES_NO_OPTION);
    	    		if (n==JOptionPane.YES_OPTION)
    	    		{
    	    			graphpart.getVGraph().setDirected(false);
    	    		}
    	    	}
    	    	else
    	    		graphpart.getVGraph().setDirected(true);
       	    } else
        	if (item==mVGLoopCh)
        	{
        	  	if (graphpart.getVGraph().isLoopAllowed())
        	   	{
        			int n = JOptionPane.showConfirmDialog(Gui.getInstance().getParentWindow(), "<html>Wirklich umformen in einen Graphen ohne Schleifen ?<br>Dabei werdem alle existenten Schleifen gel"+main.CONST.html_oe+"scht.","Umformen bestätigen",JOptionPane.YES_NO_OPTION);
        	   		if (n==JOptionPane.YES_OPTION)
        	   		{
        	   			graphpart.getVGraph().setLoopsAllowed(false);
        	   		}
        	   	}
        	   	else
        	   		graphpart.getVGraph().setLoopsAllowed(true);
           	} else	
            if (item==mVGMultipleCh)
           	{
           	  	if (graphpart.getVGraph().isMultipleAllowed())
           	   	{
            			int n = JOptionPane.showConfirmDialog(Gui.getInstance().getParentWindow(), "<html>Wirklich umformen in einen Graphen ohne Mehrfachkanten ?<br>Dabei werden alle Mehrfachkanten zwischen Knoten auf eine Kante reduziert.","Umformen bestätigen",JOptionPane.YES_NO_OPTION);
            	   		if (n==JOptionPane.YES_OPTION)
            	   		{
            	   			graphpart.getVGraph().setMultipleAllowed(false);
            	   		}
            	   	}
            	   	else
            	   		graphpart.getVGraph().setMultipleAllowed(true);
               	} else	

    	    if (item == mAVTest)
    		{
    			new AlgorithmGUI(AlgorithmFactory.RANDOM_VISUALIZE);
    		} else if (item == mAVLTD)
    		{
    			new AlgorithmGUI(AlgorithmFactory.LAYERED_TREE_DRAW);    			
    		}
    		 else if (item == mAVMAS)
     		{
     			new AlgorithmGUI(AlgorithmFactory.SPRINGS_AND_MAGNETISM);    			
     		}
    		else if (item==mHIndex)
    		{
    			HelpPanel.getInstance().showHelp();
    		}
    		else if (item == mHAbout)
    		{  
    			Gui.getInstance().showAbout();
    		}
    		else if (item == mEdUndo)
    		{
    			GraphHistory.Undo();
    			mEdRedo.setEnabled(GraphHistory.CanRedo());
    			mEdUndo.setEnabled(GraphHistory.CanUndo());
    		}
    		else if (item == mEdRedo)
    		{
    			GraphHistory.Redo();
       			mEdRedo.setEnabled(GraphHistory.CanRedo());
       			mEdUndo.setEnabled(GraphHistory.CanUndo());
       		}
    		else if (item == mEdDelSelection)
    		{
    			graphpart.getVGraph().removeSelection();
    		}
     		else if (item == mEdModifySelection)
    		{
    			new JSelectionModifyDialog(false, true, true, "Selektion bearbeiten", graphpart.getVGraph());
    		}
     		else if (item == mEdArrangeSelection)
    		{
    			new JSelectionModifyDialog(true, false, false, "selektierte Knoten nordnen", graphpart.getVGraph());
    		}
    	}
	}
	public void checkSaved() 
	{
		if (!fileDialogs.isGraphSaved()) //Not Saved
		{
			//Fragen
			if (GeneralPreferences.getInstance().getStringValue("graph.lastfile").equals("$NONE"))
			{
				if (Gui.getInstance().getVGraph().NodeCount()>0) //es gibt überhaupt was
				{
					int n = JOptionPane.showConfirmDialog(Gui.getInstance().getParentWindow(), "<html>Der aktuelle Graph wurde nicht gespeichert. M"+main.CONST.html_oe+"chten Sie den Graph noch speichern ?</html>","Gravel beenden",JOptionPane.YES_NO_OPTION);
					if (n==JOptionPane.YES_OPTION)
					{
						fileDialogs.SaveAs();
					}
				}
			}
			else
			{
				String file = (new File(GeneralPreferences.getInstance().getStringValue("graph.lastfile")).getName());
				int n = JOptionPane.showConfirmDialog(Gui.getInstance().getParentWindow(), "<html>M"+main.CONST.html_oe+"chten Sie die aktuellen "+main.CONST.html_Ae+"nderungen an dem Graphen noch in '"+file+"' speichern ?</html>","Gravel beenden",JOptionPane.YES_NO_OPTION);
	    		if (n==JOptionPane.YES_OPTION)
	    		{
	    			fileDialogs.Save();
	    		}				
			}
		}
	}
	public void update(Observable arg0, Object arg1) 
	{
		GraphMessage m = (GraphMessage)arg1;
		if (m==null)
			return;
		//either Selection changed or was affected
		if (((m.getAffectedTypes()&GraphMessage.SELECTION)==GraphMessage.SELECTION)||((m.getAction()&GraphMessage.SELECTION)==GraphMessage.SELECTION))
		{
			mEdDelSelection.setEnabled(graphpart.getVGraph().selectedEdgeExists()||graphpart.getVGraph().selectedNodeExists());		
			mEdModifySelection.setEnabled(graphpart.getVGraph().selectedEdgeExists()||graphpart.getVGraph().selectedNodeExists());		
			mEdArrangeSelection.setEnabled(graphpart.getVGraph().selectedNodeExists());
		}
		else if ((m.getAction()&GraphMessage.DIRECTION)==GraphMessage.DIRECTION) //directed changed
		{
	    	if (graphpart.getVGraph().isDirected())
	        	mVGDirCh.setText("ungerichtet");
	        else
	        	mVGDirCh.setText("gerichtet");        	
		}
		else if ((m.getAction()&GraphMessage.LOOPS)==GraphMessage.LOOPS) //Loops changed
		{
	    	if (graphpart.getVGraph().isLoopAllowed())
	        	mVGLoopCh.setText("entferne Schleifen");
	        else
	        	mVGLoopCh.setText("erlaube Schleifen");        	
		}
		else if ((m.getAction()&GraphMessage.MULTIPLE)==GraphMessage.MULTIPLE) //Multiple Edges Changed
		{
	    	if (graphpart.getVGraph().isMultipleAllowed())
	        	mVGMultipleCh.setText("entferne Mehrfachkanten");
	        else
	        	mVGMultipleCh.setText("erlaube Mehrfachkanten");        	
		}
		mEdUndo.setEnabled(GraphHistory.CanUndo());
		mEdRedo.setEnabled(GraphHistory.CanRedo());
	}
}
