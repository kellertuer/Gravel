  package view;

import help.view.HelpPanel;
import history.CommonGraphHistoryManager;
import io.GeneralPreferences;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;

import javax.swing.ActionMap;
import javax.swing.ButtonGroup;
import javax.swing.InputMap;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;

import model.VGraph;
import model.VGraphInterface;
import model.VHyperEdge;
import model.VHyperGraph;
import model.VItem;
import model.Messages.GraphConstraints;
import model.Messages.GraphMessage;

import dialogs.JFileDialogs;
import dialogs.JPreferencesDialog;
import dialogs.JSelectionModifyDialog;

import algorithm.AlgorithmFactory;

import view.pieces.GridComponent;

/**
 * Class Containing the Menu and Handling actions on that Menu, for example evoke the Dialogs.
 *
 * @author Ronny Bergmann
 * @since 0.2
 */
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
	JMenuItem mVModusShape;
	JMenuItem mVGrid, mVGDirCh, mVGLoopCh, mVGMultipleCh;
	JMenuItem mEdDelSelection,mEdModifySelection, mEdArrangeSelection;
	JMenuItem mEdUndo, mEdRedo;
	JMenuItem mAVTest,mAVLTD, mAVMAS;
	JMenuItem mHIndex,mHAbout;
	JFileDialogs fileDialogs;
	VCommonGraphic graphpart;
	CommonGraphHistoryManager GraphHistory;
	int MenuAccModifier;
	//Indicators for Macintosh Systems, Graph (iff fals Hypergraph) and the knowledge of file the graph is saved in
	boolean isMac, isGraph, isFileKnown;
	public MainMenu(VCommonGraphic vgraphic)
	{
        graphpart = vgraphic;
        GraphHistory = graphpart.getGraphHistoryManager();
        isMac = (System.getProperty("os.name").toLowerCase().indexOf("mac")!=-1);
        isGraph = vgraphic.getType()==VCommonGraphic.VGRAPHIC;
        createMenuBar();
		fileDialogs = new JFileDialogs(vgraphic);
		setOpaque(true);
        setPreferredSize(new Dimension(200, 20));
        if (isGraph)
        	((VGraphic)graphpart).getGraph().addObserver(this);
        else
        	((VHyperGraphic)graphpart).getGraph().addObserver(this);
        isFileKnown=false;
 	}
	public void changeVGraph(VCommonGraphic vgraphic)
	{
		//remove old observerstuff
        if (isGraph)
        	((VGraphic)graphpart).getGraph().deleteObserver(this);
        else
        	((VHyperGraphic)graphpart).getGraph().deleteObserver(this);
        
        graphpart = vgraphic;
        GraphHistory = graphpart.getGraphHistoryManager();
        isGraph = vgraphic.getType()==VCommonGraphic.VGRAPHIC;
        refreshMenuBar();
        if (vgraphic instanceof VHyperShapeGraphic)
        	mVModusShape.setText("Umriss "+main.CONST.utf8_ue+"bernehmen");
        else 
        {
    		fileDialogs = new JFileDialogs(vgraphic); //This results in only new filedialogs iff
    												  //We don't edit on a shape
        	if (vgraphic.getType()==VCommonGraphic.VHYPERGRAPHIC)
        	{
        		mVModusShape.setText("Hyperkantenumriss...");
        		mVModusShape.setEnabled(getIndexofSingeSelectedHyperEdge() > 0);
        	}
        }
        if (isGraph)
        	((VGraphic)graphpart).getGraph().addObserver(this);
        else
        	((VHyperGraphic)graphpart).getGraph().addObserver(this);
        validate();
        repaint();
	}
	/**
	 * Create the Menu-Bar with all its items
	 */
	private void createMenuBar() {
		MenuAccModifier = java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
		
        buildFileMenuItems();
        buildFileMenu();
        add(mFile);
        
        buildEditMenuItems();
        buildEditMenu();
        add(mEdit);

        buildViewMenuItems();
        buildViewMenu();
        add(mView);

        buildHelpMenuItems();
        buildHelpMenu();
        add(mHelp);
    }
	private void refreshMenuBar()
	{
        buildFileMenu();
        
        buildEditMenu();

        buildViewMenu();

        buildHelpMenu();
		mEdUndo.setEnabled(GraphHistory.CanUndo());
		mEdRedo.setEnabled(GraphHistory.CanRedo());
        validate();
	}
	
	private void buildFileMenuItems()
	{
		//Das erste Menü : Datei
        mFNew = new JMenuItem("Neuer Graph");
        mFNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, MenuAccModifier));
        if (!isMac) mFNew.setMnemonic(KeyEvent.VK_N);
        mFNew.addActionListener(this);
        
        mFOpen = new JMenuItem("Öffnen...");
        mFOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, MenuAccModifier));
        mFOpen.addActionListener(this);
        if (!isMac) mFOpen.setMnemonic(KeyEvent.VK_O);
        
        mFSave = new JMenuItem("Speichern");
        mFSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, MenuAccModifier));
        mFSave.addActionListener(this);
        if (!isMac) mFSave.setMnemonic(KeyEvent.VK_S);
        
        mFSaveAs = new JMenuItem("Speichern unter...");
        mFSaveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, (java.awt.event.InputEvent.SHIFT_MASK | MenuAccModifier)));
        //aktiv setzen, falls da ein File ist.
        if (!isMac) mFSaveAs.setMnemonic(KeyEvent.VK_A);
        mFSaveAs.addActionListener(this);

        if (!isMac)
        {
        	//Nur fuer Windows : Datei Einstellungen, beim Mac ist das im Mac Menü
        	mFWinPrefs = new JMenuItem("Einstellungen...");
        	mFWinPrefs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, MenuAccModifier));
        	mFWinPrefs.addActionListener(this);
        	mFWinPrefs.setMnemonic(KeyEvent.VK_P);
        }
        
        mFExport = new JMenuItem("Exportieren...");
        mFExport.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, MenuAccModifier));
        mFExport.addActionListener(this);
        if (!isMac) mFExport.setMnemonic(KeyEvent.VK_E);
       
        if (!isMac)
        {
        	//Datei Beenden
        	mFExit = new JMenuItem("Beenden");
        	mFExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, MenuAccModifier));
            if (!isMac) mFExit.setMnemonic(KeyEvent.VK_Q);
        	mFExit.addActionListener(this);
        }
	}
	private void buildFileMenu()
	{
		if (mFile==null)
		{
			if (isMac)
	        {
	        	mFile = new JMenu("Ablage");
	        }
	        else
	        {
	        	mFile = new JMenu("Datei");
	        	mFile.setMnemonic(KeyEvent.VK_D);         
	        }
		}
		else
			mFile.removeAll();
		
	        mFile.add(mFNew);
	        mFile.add(mFOpen);
	        isFileKnown = !GeneralPreferences.getInstance().getStringValue("graph.lastfile").equals("$NONE");
	        mFile.add(mFSave);
	        mFile.add(mFSaveAs);

	        if (!isMac)	//Nur fuer Windows : Datei Einstellungen, beim Mac ist das im Mac Menü
	        	mFile.add(mFWinPrefs);
	        mFile.add(mFExport);        
	       
	        if (!isMac)
	        {
	        	mFile.addSeparator();
	        	mFile.add(mFExit);
	        }

	}

	private void buildEditMenuItems()
	{
        mEdUndo = new JMenuItem("Widerrufen");
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
        mEdDelSelection = new JMenuItem("Auswahl löschen");
    	if (isMac)
    		mEdDelSelection.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, MenuAccModifier));    		
    	else
    	{
    		mEdDelSelection.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE,0));
            mEdDelSelection.setMnemonic(KeyEvent.VK_D);
    	}
    	mEdDelSelection.addActionListener(this);
    	mEdDelSelection.setEnabled(hasGraphSelection());
    	
    	mEdModifySelection = new JMenuItem("Auswahl bearbeiten...");
   		mEdModifySelection.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, MenuAccModifier));    		
       	if (!isMac)
           mEdModifySelection.setMnemonic(KeyEvent.VK_M);
    	mEdModifySelection.addActionListener(this);
    	mEdModifySelection.setEnabled(hasGraphSelection());

    	mEdArrangeSelection = new JMenuItem("Auswahl anordnen...");
   		//mEdArrangeSelection.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, MenuAccModifier));    		
       	if (!isMac)
           mEdArrangeSelection.setMnemonic(KeyEvent.VK_R);
    	mEdArrangeSelection.addActionListener(this);
    	mEdArrangeSelection.setEnabled(hasGraphSelectedNodes());
  	}
	private void buildEditMenu()
	{
		if (mEdit==null)
			mEdit = new JMenu("Bearbeiten");
		else
			mEdit.removeAll();
		if (!isMac) mEdit.setMnemonic(KeyEvent.VK_E);
    	mEdit.add(mEdUndo);
    	mEdit.add(mEdRedo);
    	mEdit.addSeparator();
    	mEdit.add(mEdDelSelection);        
        mEdit.add(mEdModifySelection);
        mEdit.add(mEdArrangeSelection);
	}
	
	private void buildViewMenuItems()
	{
        if (isGraph)
        {
        	VGraph vG = ((VGraphic)graphpart).getGraph();
        	//Editor Umformung directed / undirected Graph
        	mVGraph = new JMenu("Graph umformen (zu)");
        	if (!isMac) mVGraph.setMnemonic(KeyEvent.VK_G);
        
        	if (vG.getMathGraph().isDirected())
        		mVGDirCh = new JMenuItem("ungerichtet");
        	else
        		mVGDirCh = new JMenuItem("umformen zu gerichtetem Graph");        	
        	
        	mVGDirCh.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, MenuAccModifier));
        	mVGDirCh.addActionListener(this);
        	if (!isMac) mVGDirCh.setMnemonic(KeyEvent.VK_D);
        		mVGraph.add(mVGDirCh);        
        		
        	if (vG.getMathGraph().isLoopAllowed())
        		mVGLoopCh = new JMenuItem("entferne Schleifen");
        	else
        		mVGLoopCh = new JMenuItem("erlaube Schleifen");        	
        	mVGLoopCh.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, MenuAccModifier));
        	mVGLoopCh.addActionListener(this);
        	if (!isMac) mVGLoopCh.setMnemonic(KeyEvent.VK_L);
        		mVGraph.add(mVGLoopCh);        
       
        	if (vG.getMathGraph().isMultipleAllowed())
        		mVGMultipleCh = new JMenuItem("entferne Mehrfachkanten");
        	else
        		mVGMultipleCh = new JMenuItem("erlaube Mehrfachkanten");        	
        	
        	//mVGMultipleCh.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, MenuAccModifier));
        	mVGMultipleCh.addActionListener(this);
        	if (!isMac) mVGMultipleCh.setMnemonic(KeyEvent.VK_M);
        	mVGraph.add(mVGMultipleCh);        
        }
        //--Editor Modus--
        mEdModus = new JMenu("Modus");
        if (!isMac) mEdModus.setMnemonic(KeyEvent.VK_M);
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
        if (!isGraph)
        {
        	mVModusShape = new JMenuItem("Hyperkantenumriss...");
        	mVModusShape.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_6, MenuAccModifier));
        	mVModusShape.addActionListener(this);
        	if (!isMac) mVModusShape.setMnemonic(KeyEvent.VK_S);
        	mVModusShape.setEnabled(getIndexofSingeSelectedHyperEdge() > 0);
        	mEdModus.add(mVModusShape);
        }
        ButtonGroup group = new ButtonGroup();
        group.add(mVModusNormal);
        group.add(mVModusOCM);
        mVModusNormal.setSelected(true);
        //group.setSelected(mEdModusNormal, true);
        
       	//      Editor Kontrollpunkte der Kanten - Für Hyperkanten in deren Umrissmodus
       	mVShowBP = new JCheckBoxMenuItem("Kanten-Kontrollpunkte anzeigen");
       	mVShowBP.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K, MenuAccModifier));
       	mVShowBP.setSelected(GeneralPreferences.getInstance().getBoolValue("vgraphic.cpshow"));
       	if (!isMac) mVShowBP.setMnemonic(KeyEvent.VK_K);
        	mVShowBP.addActionListener(this);
        //Editor Raster
        mVGrid = new JMenuItem("Raster-Einstellungen...");
        mVGrid.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, MenuAccModifier));
        mVGrid.addActionListener(this);
        if (!isMac) mVGrid.setMnemonic(KeyEvent.VK_R);
        
//      --Editor Modus--
        mEdZoom = new JMenu("Maßstab");
        if (!isMac) mEdModus.setMnemonic(KeyEvent.VK_Z);
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
	}
	private void buildViewMenu()
	{
		if (mView==null)
			mView = new JMenu("Ansicht");
		else
			mView.removeAll();
		if (!isMac) mView.setMnemonic(KeyEvent.VK_A);

        if (isGraph)
        	mView.add(mVGraph);
        //--Editor Modus--
        mView.add(mEdModus);
        //normal
       	mView.add(mVShowBP);        
        mView.add(mVGrid);                
        mView.add(mEdZoom);
	}
	
	private void buildHelpMenuItems()
	{
        mHIndex = new JMenuItem("Index",KeyEvent.VK_I);
        	mHIndex.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1,0));
   
        mHIndex.getAccessibleContext().setAccessibleDescription("Index");
        mHIndex.addActionListener(this);
        if (!isMac) mHIndex.setMnemonic(KeyEvent.VK_I);
        if (!isMac)
        {
        	mHAbout = new JMenuItem("Über",KeyEvent.VK_B);
        	mHAbout.getAccessibleContext().setAccessibleDescription(
        			main.CONST.utf8_Ue+"ber Gravel</html>");
        	mHAbout.addActionListener(this);
            mHAbout.setMnemonic(KeyEvent.VK_A);
        }
	}
	private void buildHelpMenu()
	{
		if (mHelp==null)
			mHelp = new JMenu("Hilfe");
		else
			mHelp.removeAll();
        if (!isMac) mHelp.setMnemonic(KeyEvent.VK_H);		
        mHelp.add(mHIndex);
        if (!isMac)
        {
        	mHelp.add(mHAbout);
        }
	}
	
	private boolean hasGraphSelection()
	{
    	if (isGraph)
    		return ((VGraphic)graphpart).getGraph().hasSelection();
    	else
    		return ((VHyperGraphic)graphpart).getGraph().hasSelection();
	}
	private boolean hasGraphSelectedNodes()
	{
    	if (isGraph)
    		return ((VGraphic)graphpart).getGraph().modifyNodes.hasSelection();
    	else
    		return ((VHyperGraphic)graphpart).getGraph().modifyNodes.hasSelection();
	}
	/**
	 * Check whether to save the Garph before quitting or not
	 * @return true if we can uit or the user wishes to, else false
	 */
	public boolean checkSavedBeforeQuit() 
	{
		if (!Gui.getInstance().ApplyChange())
			return false; //do not quit
		if (!fileDialogs.isGraphSaved()) //Not Saved
		{
			//Fragen
			if (GeneralPreferences.getInstance().getStringValue("graph.lastfile").equals("$NONE")) //Last file known, check for changes
			{
				int cardinality = 0;
				if (Gui.getInstance().getVGraph().getType()==VGraphInterface.GRAPH)
					cardinality = ((VGraph)Gui.getInstance().getVGraph()).getMathGraph().modifyNodes.cardinality();
				else if (Gui.getInstance().getVGraph().getType()==VGraphInterface.HYPERGRAPH)
					cardinality = ((VHyperGraph)Gui.getInstance().getVGraph()).getMathGraph().modifyNodes.cardinality();
				if (cardinality>0) //es gibt überhaupt was
				{
					int n = JOptionPane.showConfirmDialog(Gui.getInstance().getParentWindow(), "<html>Der aktuelle Graph wurde nicht gespeichert.<br>M"+main.CONST.html_oe+"chten Sie den Graph noch speichern ?</html>","Gravel beenden",JOptionPane.YES_NO_CANCEL_OPTION);
					if (n==JOptionPane.YES_OPTION)
						return fileDialogs.SaveAs(); //If someone chose yes and aborted, we don't want to quit
					else if (n==JOptionPane.NO_OPTION)
						return true; //He does not want to save but just quit
					else //Cancel
						return false; //Do not quit
				}
			}
			else //No last file known
			{
				String file = (new File(GeneralPreferences.getInstance().getStringValue("graph.lastfile")).getName());
				int n = JOptionPane.showConfirmDialog(Gui.getInstance().getParentWindow(), "<html>Die letzten "+main.CONST.html_Ae+"nderungen des Graphen<br><i>'"+file+"'</i><br>wurden noch nicht gespeichert.<br>M"+main.CONST.html_oe+"chten Sie diese noch speichern ?</html>","Gravel beenden",JOptionPane.YES_NO_CANCEL_OPTION);
	    		if (n==JOptionPane.YES_OPTION)
	    			return fileDialogs.Save();
				else if (n==JOptionPane.NO_OPTION)
					return true; //He does not want to save but just quit
				else //Cancel
					return false; //Do not quit
			}
		}
		return true;
	}
	private int getIndexofSingeSelectedHyperEdge()
	{
   		//Check whether exactely one edge is selected
		Iterator<VHyperEdge> HEIt = ((VHyperGraphic)graphpart).getGraph().modifyHyperEdges.getIterator();
		int selindex = -1;
		while (HEIt.hasNext())
		{
			VHyperEdge actual = HEIt.next();
			if ((actual.getSelectedStatus()&VItem.SELECTED)==VItem.SELECTED)
			{
				if (selindex!=-1) //we already had one
					selindex=0;
				else
					selindex = actual.getIndex();
			}
		} //so if selindex is -1 we have no selection, if its 0 we had at least one
		if (selindex > 0)
			return selindex;
		else
			return -1;
	}
	public void actionPerformed(ActionEvent e) 
	{
		if (e.getSource() instanceof JMenuItem) 
    	{
			JMenuItem item = (JMenuItem) e.getSource();
			if (item == mFNew)
    			fileDialogs.NewGraph(); 
			else if (item == mFOpen)
    			isFileKnown = fileDialogs.Open(); //If Successfull set Save to true
			else if (item == mFSave)
			{
				if (isFileKnown)
					isFileKnown = fileDialogs.Save();
				else
					isFileKnown = fileDialogs.SaveAs();
			}
			else if (item == mFSaveAs)
        		isFileKnown = fileDialogs.SaveAs();
			else if (item == mFWinPrefs)
			{
		    	new JPreferencesDialog();
		    	Gui.getInstance().getVGraph().pushNotify(new GraphMessage(GraphConstraints.SELECTION|GraphConstraints.GRAPH_ALL_ELEMENTS,GraphConstraints.UPDATE));
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
    	    	graphpart.setMouseHandling(VCommonGraphic.STD_MOUSEHANDLING);
    	    } else
    	    if (item == mVModusOCM)
        	  	graphpart.setMouseHandling(VCommonGraphic.OCM_MOUSEHANDLING);
    	    else if (item==mVModusShape)
    	    {
    	    	if (mVModusShape.getText().equals("Hyperkantenumriss..."))
    	    	{
    	    		mVModusShape.setText("Umriss "+main.CONST.utf8_ue+"bernehmen");
    	    		mVModusOCM.setEnabled(false);
    	    		mVModusNormal.setEnabled(false);
    	    		if (((VHyperGraphic)graphpart).getGraph().modifyHyperEdges.get(getIndexofSingeSelectedHyperEdge())!=null)
    	    			Gui.getInstance().InitShapeModification(getIndexofSingeSelectedHyperEdge());
    	    	}
    	    	else if (mVModusShape.getText().equals("Umriss "+main.CONST.utf8_ue+"bernehmen"))
    	    	{
    	    		mVModusOCM.setEnabled(true);
    	    		mVModusNormal.setEnabled(true);
    	    		InputMap iMap = Gui.getInstance().getParentWindow().getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    	    		iMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));

    	    		ActionMap aMap = Gui.getInstance().getParentWindow().getRootPane().getActionMap();
    	    		aMap.remove("escape");
    	    		Gui.getInstance().getParentWindow().getRootPane().setDefaultButton(null); //Remove again
    	    		mVModusShape.setText("Hyperkantenumruss...");
    	    		Gui.getInstance().rebuildmaingrid(true);
    	    	}   	
    	    }
        	else if (item == mVZoom1)
        		graphpart.setZoom(50);
        	else if (item == mVZoom2)
        		graphpart.setZoom(100);
        	else if (item == mVZoom3)
        		graphpart.setZoom(200);
        	else if (item == mVGrid)
    	    {
    	    	GridComponent g = new GridComponent();
    	    	graphpart.addPiece("Grid",g);
    	    	g.showDialog(true);
    	    	graphpart.removePiece("Grid");
    	    	graphpart.requestFocus();
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
    			if (isGraph)
    				((VGraphic)graphpart).getGraph().removeSelection();
    			else
    				((VHyperGraphic)graphpart).getGraph().removeSelection();
    		}
     		else if (item == mEdModifySelection)
    		{
     			if (isGraph)
     				new JSelectionModifyDialog(false, true, "Auswahl bearbeiten", ((VGraphic)graphpart).getGraph());
     			else //Hypergraph
     				new JSelectionModifyDialog(false, true, "Auswahl bearbeiten", ((VHyperGraphic)graphpart).getGraph());
     			mEdUndo.setEnabled(GraphHistory.CanUndo());
     			mEdRedo.setEnabled(GraphHistory.CanRedo());
       		}
     		else if (item == mEdArrangeSelection)
    		{
     			if (isGraph)
     				new JSelectionModifyDialog(true, false, "selektierte Knoten anordnen", ((VGraphic)graphpart).getGraph());
     			else //Hypergraph
     				new JSelectionModifyDialog(true, false, "selektierte Knoten anordnen", ((VHyperGraphic)graphpart).getGraph());
     			mEdUndo.setEnabled(GraphHistory.CanUndo());
     			mEdRedo.setEnabled(GraphHistory.CanRedo());
    		}
			if(!isGraph)
			{
				Gui.getInstance().refresh();
				return;
			}
	   	    if (item==mVGDirCh)
    	    {
    			//Only active if we have a Graph
    	    	if (((VGraphic)graphpart).getGraph().getMathGraph().isDirected())
    	    	{
    				int n = JOptionPane.showConfirmDialog(Gui.getInstance().getParentWindow(), "<html>Wirklich umformen in ungerichteten Graphen ?<br>Dabei k"+main.CONST.html_oe+"nnen Kanten verloren gehen.","Umformen bestätigen",JOptionPane.YES_NO_OPTION);
    	    		if (n==JOptionPane.YES_OPTION)
    	    		{
    	    			((VGraphic)graphpart).getGraph().setDirected(false);
    	    		}
    	    	}
    	    	else
    	    		((VGraphic)graphpart).getGraph().setDirected(true);
       	    } else
        	if (item==mVGLoopCh)
        	{
        	  	if (((VGraphic)graphpart).getGraph().getMathGraph().isLoopAllowed())
        	   	{
        			int n = JOptionPane.showConfirmDialog(Gui.getInstance().getParentWindow(), "<html>Wirklich umformen in einen Graphen ohne Schleifen ?<br>Dabei werdem alle existenten Schleifen gel"+main.CONST.html_oe+"scht.","Umformen bestätigen",JOptionPane.YES_NO_OPTION);
        	   		if (n==JOptionPane.YES_OPTION)
        	   			((VGraphic)graphpart).getGraph().modifyEdges.setLoopsAllowed(false);
        	   	}
        	   	else
        	   		((VGraphic)graphpart).getGraph().modifyEdges.setLoopsAllowed(true);
           	} else	
            if (item==mVGMultipleCh)
           	{
           	  	if (((VGraphic)graphpart).getGraph().getMathGraph().isMultipleAllowed())
           	   	{
            			int n = JOptionPane.showConfirmDialog(Gui.getInstance().getParentWindow(), "<html>Wirklich umformen in einen Graphen ohne Mehrfachkanten ?<br>Dabei werden alle Mehrfachkanten zwischen Knoten auf eine Kante reduziert.","Umformen bestätigen",JOptionPane.YES_NO_OPTION);
            	   		if (n==JOptionPane.YES_OPTION)
            	   		{
            	   			((VGraphic)graphpart).getGraph().modifyEdges.setMultipleAllowed(false);
            	   		}
            	   	}
            	   	else
            	   		((VGraphic)graphpart).getGraph().modifyEdges.setMultipleAllowed(true);
             }
    	}
		Gui.getInstance().refresh();
	}
	
	public void update(Observable arg0, Object arg1) 
	{
		GraphMessage m = (GraphMessage)arg1;
		if (m==null)
			return;
		if ((m.getModification()&GraphConstraints.HISTORY)>0)
		{
			mEdUndo.setEnabled(GraphHistory.CanUndo());
			mEdRedo.setEnabled(GraphHistory.CanRedo());
		}
		//either Selection changed or was affected
		if (((m.getAffectedElementTypes()&GraphConstraints.SELECTION)==GraphConstraints.SELECTION)||((m.getModifiedElementTypes()&GraphConstraints.SELECTION)==GraphConstraints.SELECTION))
		{
			mEdDelSelection.setEnabled(hasGraphSelection());		
			mEdModifySelection.setEnabled(hasGraphSelection());		
			mEdArrangeSelection.setEnabled(hasGraphSelection());
		}
		if (graphpart.getType()==VCommonGraphic.VHYPERGRAPHIC)
		{
			if (!(graphpart instanceof VHyperShapeGraphic))
				mVModusShape.setEnabled(getIndexofSingeSelectedHyperEdge() > 0);
		}
		if (graphpart.getType()!=VCommonGraphic.VGRAPHIC)
		{
			Gui.getInstance().refresh();
			return;
		}
		if ((m.getModifiedElementTypes()&GraphConstraints.DIRECTION)==GraphConstraints.DIRECTION) //directed changed
		{
			//Only active if we have a Graph
	    	if (((VGraphic)graphpart).getGraph().getMathGraph().isDirected())
	        	mVGDirCh.setText("ungerichtet");
	        else
	        	mVGDirCh.setText("gerichtet");        	
		}
		if ((m.getModifiedElementTypes()&GraphConstraints.LOOPS)==GraphConstraints.LOOPS) //Loops changed
		{
			//Only active if we have a Graph
	    	if (((VGraphic)graphpart).getGraph().getMathGraph().isLoopAllowed())
	        	mVGLoopCh.setText("entferne Schleifen");
	        else
	        	mVGLoopCh.setText("erlaube Schleifen");        	
		}
		if ((m.getModifiedElementTypes()&GraphConstraints.MULTIPLE)==GraphConstraints.MULTIPLE) //Multiple Edges Changed
		{
			//Only active if we have a Graph
	    	if (((VGraphic)graphpart).getGraph().getMathGraph().isMultipleAllowed())
	        	mVGMultipleCh.setText("entferne Mehrfachkanten");
	        else
	        	mVGMultipleCh.setText("erlaube Mehrfachkanten");        	
		}
		Gui.getInstance().refresh();
	}
}