package view;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;

import view.pieces.ZoomComponent;

import model.VGraph;
import model.VGraphInterface;

import algorithm.AlgorithmFactory;
import algorithm.forms.*;
import algorithm.model.VAlgorithmIF;

public class AlgorithmGUI extends JDialog implements ActionListener
{

	private static final long serialVersionUID = 1L;
	private VGraphic vGc;
	private JButton bPlay, bOneStep;
	private VAlgorithmIF Algorithm;
	private JSlider playspeed;
	private JButton bOk, bCancel, bNew;
	private String AlgorithmType;
	private int waittime = 50; //in Milisekunden
	//private boolean play = true;
	private Timer playtimer;
	public AlgorithmGUI(String algType)
	{
		AlgorithmType = algType;
		//Parameter-Abfrage und Laden des Algorithmusses
		if (!InitAlgorithm(AlgorithmType))
		{
			this.dispose();
			return;
		}
		if (Algorithm.isStepwiseRunable())
		{
			//Initialize the timer for stepwise stuff
			playtimer = new Timer(waittime,this);
			Algorithm.start();
		}	
	
		InitGui();
	}
	private void InitGui()
	{
		this.setTitle("Visualisierungsalgorithmus "+AlgorithmType);
		Container content = getContentPane();
		content.removeAll();
		content.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(7,7,7,7);
		c.anchor = GridBagConstraints.WEST;
		c.gridy = 0;
		c.gridx = 0;
		c.gridheight = 8;
		vGc = new VGraphic(new Dimension(500,500),new VGraph(false,false,false));
		//noneditable
		vGc.setMouseHandling(VCommonGraphic.NO_MOUSEHANDLING);
//		Das Ganze als Scrollpane
        JScrollPane scrollPane = new JScrollPane(vGc);
        scrollPane.setViewportView(vGc);
        vGc.setViewPort(scrollPane.getViewport());
        scrollPane.setMinimumSize(new Dimension(502,502));
        scrollPane.setMaximumSize(new Dimension(502,502));
        scrollPane.setPreferredSize(new Dimension(502,502));
        
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        vGc.setPreferredSize(new Dimension(500,500));
        vGc.setSize(new Dimension(500,500));
        vGc.setVisible(true);
		vGc.setEnabled(true); 
		content.add(scrollPane,c);
		c.gridheight=1;
		c.gridwidth=3;
		c.gridx = 1;
		c.gridy = 0;
		if (Algorithm.isStepwiseRunable()) //Ist der Algorithmus schrittweise ausfuehrbar ?
		{
			vGc.getGraph().replace(Algorithm.getactualState()); //Anfangsleer anzeigen
			Algorithm.getactualState().addObserver(vGc);
			bPlay = new JButton(" Play ");
			bPlay.addActionListener(this);
			content.add(bPlay,c);
			bOneStep = new JButton("Nur einen Schritt weiter");
			bOneStep.addActionListener(this);
			c.gridy++;
			content.add(bOneStep,c);
			playspeed = new JSlider();
			c.gridy++;
			content.add(playspeed,c);
			c.gridy++;
			content.add(new JLabel("Abspielgeschwindigkeit"),c);
			c.gridy++;
		}
		else
		{
			//Am Stück ausführen
			Algorithm.run();
			vGc.getGraph().replace(Algorithm.getactualState());
			Algorithm.getactualState().addObserver(vGc);
		}
		Statistics stats = new Statistics(vGc);
		stats.setActive(false);
		stats.setRows(Gui.getInstance().getStatRows());
		stats.setPreferredSize(new Dimension(120,150));
		content.add(stats,c);
		
		ZoomComponent zoom = new ZoomComponent();
		vGc.addPiece("Zoom",zoom);
		c.gridy++;
		content.add(zoom.getContent(),c);
		c.gridwidth = 1;
		c.gridy = 9;
		c.gridx = 1;
		bOk = new JButton("<html>"+main.CONST.html_Ue+"bernehmen");
		bOk.addActionListener(this);
		if (Algorithm.isStepwiseRunable())
			bOk.setEnabled(false); //zunächst kann man nicht übernehmen, Graph nicht fertig
		content.add(bOk,c);
		c.gridy = 9;
		c.gridx = 2;
		bNew = new JButton("Neu starten");
		bNew.addActionListener(this);
		content.add(bNew,c);
		c.gridy = 9;
		c.gridx = 3;
		bCancel = new JButton("<html>Schlie"+main.CONST.html_sz+"en</html>");
		bCancel.addActionListener(this);
		content.add(bCancel,c);
		
		setResizable(false);
		this.setModal(true);
		pack();
		this.setVisible(true);
	}
	/**
	 * Calls the AlgorithmFabricator
	 * @param AlgType
	 */
	@SuppressWarnings("unchecked")
	public boolean InitAlgorithm(String AlgType)
	{
		String ParamError = new String();
		HashMap erg;
		Algorithm = AlgorithmFactory.getAlgorithm(AlgType);
		AlgorithmParameterForm form;
		try {
			if (Gui.getInstance().getVGraph().getType()!=VGraphInterface.GRAPH)
				return false;
			form = AlgorithmFactory.getForm(AlgType, ((VGraph) Gui.getInstance().getVGraph()));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		do
		{
			erg = form.showDialog();
			
			ParamError = Algorithm.setParameters(erg);
			
			if (!ParamError.equals(""))
				JOptionPane.showMessageDialog(this,ParamError,"Fehler",JOptionPane.ERROR_MESSAGE);
			if (erg==null)
			{
				//JOptionPane.showMessageDialog(this,"<html><p>Parametereingabe abgebrochen. Ausf"+main.CONST.html_ue+"hren nicht m"+main.CONST.html_oe+"glich.</p></html>","Fehler",JOptionPane.ERROR_MESSAGE);
				return false;
			}
		}
		while ((erg!=null)&&(!(ParamError.equals(""))));
		return true;
	}
	
	private void finish()
	{
		if (Algorithm.isStepwiseRunable())
		{
			bPlay.setEnabled(false);
			bOneStep.setEnabled(false);
		}
		bOk.setEnabled(true);
		playtimer.stop();
	}
	public void actionPerformed(ActionEvent e) {
		if (e.getSource()==playtimer)
		{
			Algorithm.step();
			vGc.repaint();
			if (Algorithm.finished())
			{
				bPlay.setText(" Play ");
				finish();	
			}
		}
		else if (e.getSource()==bPlay)
		{
			if (playtimer.isRunning())
			{
				playtimer.stop();
				bPlay.setText(" Play ");
				bOneStep.setEnabled(true);
			}
			else
			{
				playtimer.start();
				bPlay.setText("Pause");
				bOneStep.setEnabled(false);
			}
		}
		else if (e.getSource()==bPlay)
		{
			if (playtimer.isRunning())
				playtimer.stop();
			else
				playtimer.start();
			if (Algorithm.finished())
				finish();			
		}
		else if (e.getSource()==bOneStep)
		{
			Algorithm.step();
			vGc.repaint();
			if (Algorithm.finished())
				finish();
		}
		else if (e.getSource()==bCancel)
		{
			playtimer.stop();
			this.dispose();
		}
		else if (e.getSource()==bOk)
		{
			Gui.getInstance().setVGraph(vGc.getGraph());
			this.dispose();
		}
		else if (e.getSource()==bNew)
		{
			if (!InitAlgorithm(AlgorithmType))
			{
				this.dispose();
				return;
			}
			if (Algorithm.isStepwiseRunable())
			{
				//Initialize the timer for stepwise stuff
				playtimer = new Timer(waittime,this);
				Algorithm.start();
			}	
			InitGui();
		}
	}
}
