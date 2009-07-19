package view.pieces;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

import dialogs.JEdgeDialog;
import dialogs.JHyperEdgeDialog;
import dialogs.JNodeDialog;

import model.*;
import model.Messages.*;

public class ItemInformationComponent implements Observer, ActionListener
{
	private VGraphInterface graphRef;
	private VItem Selected=null;
	private JButton bInformation;
	private JPanel content;
	public ItemInformationComponent(VGraphInterface PvG)
	{
		graphRef = PvG;
		String IconDir = System.getProperty("user.dir")+"/data/img/icon/";
		content = new JPanel();
		content.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(7,7,7,7);
		c.anchor = GridBagConstraints.CENTER;
		c.gridy = 0;
		c.gridx = 0;
		c.gridwidth=5;
		bInformation = new JButton(new ImageIcon(IconDir+"information32.png"));	
		bInformation.setSize(new Dimension(32,32));
		bInformation.addActionListener(this);
		content.add(bInformation,c);
		if (PvG.getType()==VGraphInterface.GRAPH)
		{
			((VGraph)PvG).addObserver(this);
			update(((VGraph)PvG),new GraphMessage(GraphConstraints.SELECTION,GraphConstraints.UPDATE));
		}
		else if (PvG.getType()==VGraphInterface.HYPERGRAPH)
		{
			((VHyperGraph)PvG).addObserver(this);
			update(((VHyperGraph)PvG),new GraphMessage(GraphConstraints.SELECTION,GraphConstraints.UPDATE));
		}
	}
	public JPanel getContent()
	{
		return content;
	}
	public void update(Observable o, Object arg)
	{
		if (arg instanceof GraphMessage) //Not nice but haven't found a beautiful way after hours
		{
			GraphMessage m = (GraphMessage)arg;
			if ( ((m.getAffectedElementTypes()&GraphConstraints.SELECTION)==0)
					&& ((m.getModifiedElementTypes()&GraphConstraints.SELECTION)==0))
				return; //Only update here if selection
				if (graphRef==o)
				{
					Selected = graphRef.getSingleSelectedItem();
					bInformation.setEnabled(Selected!=null);
					if (Selected != null)
					{
						switch(Selected.getType())
						{
							case VItem.EDGE:
								bInformation.setToolTipText("Eigenschften der Kante #"+Selected.getIndex());
								break;
							case VItem.NODE:
								bInformation.setToolTipText("Eigenschften des Kotens Kante #"+Selected.getIndex());
								break;
							case VItem.HYPEREDGE:
								bInformation.setToolTipText("Eigenschften der Hyperkante #"+Selected.getIndex());
								break;
						}
					}
				}
		}
	}
	public void actionPerformed(ActionEvent e) {
		if (Selected == null)
			return;
		switch(Selected.getType())
		{
			case VItem.EDGE:
				new JEdgeDialog((VEdge)Selected,(VGraph)graphRef);
				break;
			case VItem.NODE:
				new JNodeDialog((VNode)Selected,graphRef); 
				break;
			case VItem.HYPEREDGE:
				new JHyperEdgeDialog((VHyperEdge)Selected);
				break;
			default:
			break;
		}
	}
}