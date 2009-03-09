package dialogs;

import io.GeneralPreferences;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import dialogs.components.CNodeNameParameters;

import view.Gui;

import model.MNode;
import model.VGraph;
import model.VNode;
import model.Messages.GraphConstraints;
import model.Messages.GraphMessage;

/** JNodeDialog
 *  Dialog for creation and variation of nodes
 *  
 *  all values, the subgraphs the node belongs to and the visibility of the node name can be changed.
 */
public class JNodeDialog extends JDialog implements ActionListener, ItemListener
{
	private static final long serialVersionUID = 423L;
	private String oldname;
	private int oldindex, oldx, oldy,oldsize; //Beim Abbrechen Wiederherstellen
	private VGraph graphref;
	private IntegerTextField iNodeIndex,ixPos,iyPos,iSize;
	private JTextField sname, Colorfield;

	private GeneralPreferences gp;
	
	private VNode chNode;
	private Vector<String> subgraphlist;
	private JCheckBox[] SubgraphChecks;
	private JScrollPane iSubgraphs;
	
	private CNodeNameParameters cNodeName;
	
	JTabbedPane tabs;
	
	private JButton bOK, bCancel;
	
	/**
	 * Initialize the Dialog with predefined values for the fields, opens a "Neuen Knoten erstellen"-Dialog 
	 *  
	 * @param index node index
	 * @param name node name
	 * @param x node position x
	 * @param y node position y
	 * @param size node size (diameter
	 * @param vg vgraph the node should be inserted into
	 */
	public JNodeDialog(int index,String name, int x, int y, int size, VGraph vg)
	{
		chNode = null;
		oldname = name;
		oldindex = index;
		oldx = x;
		oldy = y;
		oldsize = size;
		graphref = vg;
		CreateDialog(null);
	}
	/**
	 * Constructor for node variation, 
	 * 
	 * @param v existing node that should be variated
	 * @param vg VGRaph the node is placed in
	 */
	public JNodeDialog(VNode v, VGraph vg)
	{
		graphref=vg;
		CreateDialog(v);
	}
	/**
	 * Create the Dialog and initialize the checknode with v
	 * 
	 * @param v the check node
	 */
	private void CreateDialog(VNode v)
	{
		gp = GeneralPreferences.getInstance();
		if (v==null)
		{
			this.setTitle("Neuen Knoten einfügen");
			chNode = null;
		}
		else
		{
			chNode = v;
			oldindex = v.getIndex();
			oldx = v.getPosition().x;
			oldy = v.getPosition().y;
			oldsize = v.getSize();
			oldname = graphref.getMathGraph().modifyNodes.get(oldindex).name;
			this.setTitle("Eigenschaften des Knotens '"+oldname+"' (#"+v.getIndex()+")");	
		}
		
		Container content = getContentPane();
		GridBagConstraints c = new GridBagConstraints();
		tabs = new JTabbedPane();
		tabs.setTabPlacement(JTabbedPane.TOP);
		tabs.addTab("Allgemein", buildMainPane());
		cNodeName = new CNodeNameParameters(chNode,false);
		tabs.addTab("Ansicht", cNodeName);
		Container ContentPane = this.getContentPane();
		ContentPane.removeAll();
		ContentPane.setLayout(new GridBagLayout());
		c.insets = new Insets(7,7,7,7);
		c.anchor = GridBagConstraints.WEST;
	
		c.gridy=0;c.gridx=0;c.gridwidth=2;
		ContentPane.add(tabs,c);
		c.insets = new Insets(7,7,7,7);
		c.gridwidth = 1;
		
		
		c.gridy++;
		c.gridx = 0;
		c.anchor = GridBagConstraints.WEST;
		c.gridy++;
		c.gridx = 0;
		c.insets = new Insets(3,3,3,3);
		bCancel = new JButton("Abbrechen");
		bCancel.addActionListener(this); 
		//ESC-Handling
		InputMap iMap = getRootPane().getInputMap(	 JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape");
		
		ActionMap aMap = getRootPane().getActionMap();
		aMap.put("escape", new AbstractAction()
			{
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e)
				{
					dispose();
				}
		 	});
		 	
		content.add(bCancel,c);
		
		c.gridx = 1;
		c.anchor = GridBagConstraints.EAST;
		if (v==null)
			bOK = new JButton("Knoten erstellen");
		else
			bOK = new JButton("<html>"+main.CONST.html_Ae+"nderungen speichern</html>");
		bOK.addActionListener(this);
		content.add(bOK,c);
		
		fillValues();
		
		this.getRootPane().setDefaultButton(bOK);
		setResizable(false);
		this.setModal(true);
		pack();
		Point p = new Point(0,0);
		p.y += Math.round(Gui.getInstance().getParentWindow().getHeight()/2);
		p.x += Math.round(Gui.getInstance().getParentWindow().getWidth()/2);
		p.y -= Math.round(getHeight()/2);
		p.x -= Math.round(getWidth()/2);
		setLocation(p.x,p.y);
		this.setVisible(true);
	}
	/**
	 * priavte method to build the main pane
	 * @return the initilized main pane
	 */
	private JPanel buildMainPane()
	{
		JPanel content = new JPanel(); 
		content.setLayout(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(7,7,7,7);
		c.anchor = GridBagConstraints.WEST;
		c.gridy = 0;
		c.gridx = 0;
		content.add(new JLabel("Name"),c);
		c.gridx = 1;
		sname = new JTextField();
		sname.setPreferredSize(new Dimension(200, 20));

		content.add(sname,c);
		
		c.gridy++;
		c.gridx = 0;
		content.add(new JLabel("Knotenindex"),c);
		c.gridx = 1;
		iNodeIndex = new IntegerTextField();
		iNodeIndex.setPreferredSize(new Dimension(200, 20));
		iNodeIndex.setValue(oldindex);
		content.add(iNodeIndex,c);
		
		c.gridy++;
		c.gridx = 0;
		content.add(new JLabel("horizontale Postion"),c);
		c.gridx = 1;
		ixPos = new IntegerTextField();
		ixPos.setPreferredSize(new Dimension(200, 20));
		content.add(ixPos,c);
		
		c.gridy++;
		c.gridx = 0;
		content.add(new JLabel("vertikale Postion"),c);
		c.gridx = 1;
		iyPos = new IntegerTextField();
		iyPos.setPreferredSize(new Dimension(200, 20));
		content.add(iyPos,c);
		
		c.gridy++;
		c.gridx = 0;
		content.add(new JLabel("<html>Gr"+main.CONST.html_oe+""+main.CONST.html_sz+"e</html>"),c);
		c.gridx = 1;
		iSize = new IntegerTextField();
		iSize.setPreferredSize(new Dimension(200, 20));

		content.add(iSize,c);
		
		c.gridy++;
		c.gridx = 0;
		content.add(new JLabel("<html><font color=#999999>Farbe (ergibt sich aus den Untergraphen)</font></html>"),c);
		c.gridx = 1;
		Colorfield = new JTextField(); 
		Colorfield.setPreferredSize(new Dimension(200,20));
		Colorfield.setEditable(false);
		content.add(Colorfield,c);
		c.gridy++;
		c.gridx = 0;
		buildSubgraphList();
		c.gridy++;
		c.gridx=0;
		c.insets = new Insets(0,7,0,7);
		c.anchor = GridBagConstraints.CENTER;
		c.gridwidth = 2;
		content.add(new JLabel("Untergraphen"),c);
		c.gridy++;
		c.gridx=0;
		content.add(iSubgraphs,c);
		c.gridy++;
		if (gp.getBoolValue("grid.orientated")&&gp.getBoolValue("grid.enabled"))
			content.add(new JLabel("<html><font size='-2'><br>Der Knoten wird beim Speichern am nächsten Gitterpunkt ausgerichtet.</font></html>"),c);
		return content;
	}
	/**
	 * fill values in all parameter fields
	 *
	 */
	private void fillValues()
	{	
		//		Main Tab
		sname.setText(oldname);
		ixPos.setValue(oldx);
		iyPos.setValue(oldy);
		iSize.setValue(oldsize);
		if (chNode!=null)
			Colorfield.setBackground(chNode.getColor());
		else
			Colorfield.setBackground(Color.BLACK);
	}
	/**
	 * build the sub set lists for the node
	 *
	 */
	private void buildSubgraphList()
	{
		Container CiSubgraphs = new Container();
		CiSubgraphs.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(0,0,0,0);
		c.anchor = GridBagConstraints.NORTHWEST;
		c.gridy = 0;
		c.gridx = 0;
		subgraphlist = graphref.getMathGraph().modifySubgraphs.getNames();
		int temp = 0;
		for (int i=0; i<subgraphlist.size(); i++)
		{
			if (subgraphlist.elementAt(i)!=null) //Ein Knoten mit dem Index existiert
			temp ++; //Anzahl Knoten zaehlen
		}
		SubgraphChecks = new JCheckBox[temp];
		temp = 0;
		for (int i=0; i<subgraphlist.size(); i++)
		{
			if (subgraphlist.elementAt(i)!=null) //Ein Knoten mit dem Index existiert
			{
				SubgraphChecks[temp] = new JCheckBox(graphref.getMathGraph().modifySubgraphs.get(i).getName());
				if (chNode!=null)
					SubgraphChecks[temp].setSelected(graphref.getMathGraph().modifySubgraphs.get(i).containsNode(chNode.getIndex()));
				CiSubgraphs.add(SubgraphChecks[temp],c);
				SubgraphChecks[temp].addItemListener(this);
				c.gridy++;
				temp++; //Anzahl Knoten zaehlen
			}
		}
		iSubgraphs = new JScrollPane(CiSubgraphs);
		iSubgraphs.setAlignmentX(JScrollPane.LEFT_ALIGNMENT);
		iSubgraphs.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		iSubgraphs.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		iSubgraphs.setPreferredSize(new Dimension(200,100));
		
	}
	/**
	 * handle the button and radio button actions
	 */
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource()==bCancel)
		{
			this.dispose();
		}
		if (e.getSource()==bOK)
		{
			if ((iNodeIndex.getValue()==-1)||(sname.getText().equals(""))||(ixPos.getValue()==-1)||(iyPos.getValue()==-1)||(iSize.getValue()==-1))
			{
				if (chNode==null) //Creation
					JOptionPane.showMessageDialog(this, "<html><p>Erstellen des Knotens nicht m"+main.CONST.html_oe+"glich.<br><br>Einige Felder wurden nicht ausgef"+main.CONST.html_ue+"llt.</p></hmtl>", "Fehler", JOptionPane.ERROR_MESSAGE);
				else //Modification
					JOptionPane.showMessageDialog(this, "<html><p>"+main.CONST.html_Ae+"nderungen am Knoten nicht m"+main.CONST.html_oe+"glich.<br><br>Einige Felder wurden nicht ausgef"+main.CONST.html_ue+"llt.</p></hmtl>", "Fehler", JOptionPane.ERROR_MESSAGE);					
				return;
			
			}
			String t = cNodeName.VerifyInput();
			if (!t.equals(""))
			{
				if (chNode==null)
					JOptionPane.showMessageDialog(this, "<html><p>Erstellen des Knotens nicht m"+main.CONST.html_oe+"glich.<br><br>"+t+".</p></hmtl>", "Fehler", JOptionPane.ERROR_MESSAGE);
				else
					JOptionPane.showMessageDialog(this, "<html><p>"+main.CONST.html_Ae+"nderungen am Knoten nicht m"+main.CONST.html_oe+"glich.<br><br>"+t+".</p></hmtl>", "Fehler", JOptionPane.ERROR_MESSAGE);					
				return;	
			}
			if (chNode==null) //neuer Knoten, index testen
			{
				if (graphref.modifyNodes.get(iNodeIndex.getValue())!=null) //So einen gibt es schon
				{
					JOptionPane.showMessageDialog(this, "<html><p>Erstellen des Knotens nicht m"+main.CONST.html_oe+"glich.<br><br>Der Index ist bereits vergeben.</p></hmtl>", "Fehler", JOptionPane.ERROR_MESSAGE);
					return;
				}
				else if (iNodeIndex.getValue()<=0)
				{
					JOptionPane.showMessageDialog(this, "<html><p>Erstellen des Knotens nicht m"+main.CONST.html_oe+"glich.<br><br>Der Index muss echt gr"+main.CONST.html_oe+""+main.CONST.html_sz+"er 0 sein.</p></hmtl>", "Fehler", JOptionPane.ERROR_MESSAGE);
					return;					
				}
				else
				{
					graphref.pushNotify(new GraphMessage(GraphConstraints.NODE, iNodeIndex.getValue(), GraphConstraints.ADDITION|GraphConstraints.BLOCK_START, GraphConstraints.NODE));
					//Neuen Knoten einfuegen
					VNode newnode = new VNode(iNodeIndex.getValue(),ixPos.getValue(), iyPos.getValue(), iSize.getValue(),0,0,0,false);
					newnode = cNodeName.modifyNode(newnode);
					graphref.modifyNodes.add(newnode, new MNode(newnode.getIndex(),sname.getText()));
				}
			}
			else //Knoten geaendert
			{
				if (oldindex != iNodeIndex.getValue()) //Der Benutzer hat den Index geaendert
				{
					if (graphref.modifyNodes.get(iNodeIndex.getValue())!=null) //So einen gibt es schon
					{
						JOptionPane.showMessageDialog(this, "<html><p>"+main.CONST.html_Ae+"nderung nicht m"+main.CONST.html_oe+"glich.<br><br>Der Index ist bereits vergeben.</p></hmtl>", "Fehler", JOptionPane.ERROR_MESSAGE);
						return;
					}
					else if (iNodeIndex.getValue()==0)
					{
						JOptionPane.showMessageDialog(this, "<html><p>Erstellen des Knotens nicht m"+main.CONST.html_oe+"glich.<br><br>Der Index muss echt gr"+main.CONST.html_oe+""+main.CONST.html_sz+"er 0 sein.</p></hmtl>", "Fehler", JOptionPane.ERROR_MESSAGE);
						return;					
					}
					graphref.pushNotify(new GraphMessage(GraphConstraints.NODE, GraphConstraints.UPDATE|GraphConstraints.BLOCK_START, GraphConstraints.GRAPH_ALL_ELEMENTS));
					graphref.modifyNodes.changeIndex(oldindex, iNodeIndex.getValue());
				}
				else
				{
					graphref.pushNotify(new GraphMessage(GraphConstraints.NODE, iNodeIndex.getValue(), GraphConstraints.UPDATE|GraphConstraints.BLOCK_START, GraphConstraints.NODE));
				}
				//Allgemeine Werte aktualisieren
				graphref.getMathGraph().modifyNodes.get(iNodeIndex.getValue()).name = sname.getText();
				VNode n = graphref.modifyNodes.get(iNodeIndex.getValue()); 
				n.setPosition(new Point(ixPos.getValue(), iyPos.getValue()));
				n.setSize(iSize.getValue());
				//Knotennamenanzeigewerte
				cNodeName.modifyNode(graphref.modifyNodes.get(iNodeIndex.getValue()));
				

			}
			//Gruppen noch wieder aktualisieren
			int temp = 0;
			for (int i=0; i<subgraphlist.size(); i++)
			{
				if (subgraphlist.elementAt(i)!=null) //Ein Untergraph mit dem Index existiert
				{
					if (SubgraphChecks[temp].isSelected())
						graphref.modifySubgraphs.addNodetoSubgraph(iNodeIndex.getValue(), i);
					else //Sonst entfernen, da das nur geupdated wird oben !
						graphref.modifySubgraphs.removeNodefromSubgraph(iNodeIndex.getValue(), i);
					temp++; //Anzahl Knoten zaehlen
				}
			}
			if (gp.getBoolValue("grid.orientated")&&gp.getBoolValue("grid.enabled"))
			{ //Noch am Raster ausrichten, falls das aktiv ist
				graphref.modifyNodes.get(iNodeIndex.getValue()).setPosition(gridsnap(iNodeIndex.getValue()));	
			}
			graphref.pushNotify(new GraphMessage(GraphConstraints.NODE,GraphConstraints.BLOCK_END));
			this.dispose();
		}
		
	}
	/**
	 * snap the variated/created node to a gridpoint
	 * @param nodeindex index of the node
	 * @return
	 */
	private Point gridsnap(int nodeindex)
	{
		VNode movingNode = graphref.modifyNodes.get(iNodeIndex.getValue());
		int gridx = gp.getIntValue("grid.x");
		int gridy = gp.getIntValue("grid.y");
		int xdistanceupper = movingNode.getPosition().x%gridx;
		int xdistancelower = gridx-xdistanceupper;
		int ydistanceupper = movingNode.getPosition().y%gridy;
		int ydistancelower = gridy-ydistanceupper;
		Point newpos = new Point();
		if (xdistanceupper>xdistancelower) //näher am oberen der beiden werte
			newpos.x = (new Double(Math.ceil((double)movingNode.getPosition().x/(double)gridx)).intValue())*gridx;
		else 
			newpos.x =  (new Double(Math.floor((double)movingNode.getPosition().x/(double)gridx)).intValue())*gridx;
		if (ydistanceupper>ydistancelower) //näher am oberen der beiden werte
			newpos.y = (new Double(Math.ceil((double)movingNode.getPosition().y/(double)gridy)).intValue())*gridy;
		else 
			newpos.y =  (new Double(Math.floor((double)movingNode.getPosition().y/(double)gridy)).intValue())*gridy;
		return newpos;
	}
	/**
	 * react on changes in subgraph-things, vary the color
	 */
	public void itemStateChanged(ItemEvent event) {
		for (int i=0; i<SubgraphChecks.length; i++)
		{
			if (event.getSource()==SubgraphChecks[i])
			{
				//Ein Zustand hat sich geändert, neue Farbe berechnen
				Color colour = Color.BLACK;
				int colourcount = 0;
				int temp = 0; //zum mitzaehlen
				for (int j=0; j<subgraphlist.size();j++)
				{
					if (subgraphlist.elementAt(j)!=null)
					{
						if (SubgraphChecks[temp].isSelected())
						{
							Color newc = graphref.modifySubgraphs.get(j).getColor();
							int b=colour.getBlue()*colourcount + newc.getBlue();
							int a=colour.getAlpha()*colourcount + newc.getAlpha();
							int g=colour.getGreen()*colourcount + newc.getGreen();
							int r=colour.getRed()*colourcount + newc.getRed();
							colourcount++;
							colour = new Color((r/colourcount),(g/colourcount),(b/colourcount),(a/colourcount));
						}
							temp++;
					}
				}
				Colorfield.setBackground(colour);
				Colorfield.repaint();
				return;
			}
		}
	}

}
