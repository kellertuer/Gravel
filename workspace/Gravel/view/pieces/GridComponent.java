package view.pieces;

import io.GeneralPreferences;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Observable;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import view.Gui;


/**
 * GridComponent ist der Rasterdialog. Dieser ist entweder als eigenes Fenster aufrufbar (showDialog) oder in andere Dialoge integrierbar (getContent)
 * Die Änderungen im Dialog werden bei der Bestätigung mit OK in die allgemeinen Einstellungen geschrieben.
 * Wird diese Komponente mittels getContent in einem anderen Dialog eingebunden, so lassen sich über die sondierenden Methoden die Werte abfragen und verwenden. * wirken sich dabei 
 * 
 * Die Komponente ist Beobachtbar, wenn sich also die einstellungen ändern, kann das sofort angezeigt werden.
 * Wird etwa der Dialog angezeigt, so ist eine Vorschau des Rasters sofort möglich. Abbrechen setzt auf die vorherigen Werte zurück
 * @author Ronny Bergmann
 *
 */
public class GridComponent extends Observable implements ChangeListener, ActionListener
{
	private JPanel content;
	private JSlider gridxslider, gridyslider;
	private JCheckBox rbEnabled, rbSynchron,rbOrientated;
	private JButton bOk, bCancel;
	private JLabel gridinfox,gridinfoy;
	private JDialog GridDialog;
	private int valuex,valuey, lastchanged;
	GeneralPreferences gp;
	/**
	 * Konstruktor, der alle Dialogfelder mit den Werten aus den allgemeinen Einstellungen initialisiert
	 * danach lassen sich die beiden Verwendungsmöglichkeiten (als selbstständiger Dialog oder einbetten in andre Dialoge) aufrufen
	 * 
	 * Dabei sind die Buttons "Ok" und "Abbrechen" nicht sichtbar - sie werden nur im Falle des eigenständigen Dialogs (showDialog) sichtbar gesetzt
	 */
	public GridComponent()
	{
		gp = GeneralPreferences.getInstance();
		valuex = gp.getIntValue("grid.x");
		valuey = gp.getIntValue("grid.y");
		lastchanged = valuex;
		content = new JPanel();
		content.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(7,7,7,7);
		c.anchor = GridBagConstraints.WEST;
		c.gridy = 0;c.gridx = 0;
	
		rbEnabled = new JCheckBox("Raster aktiviert");
		rbEnabled.addActionListener(this);
		content.add(rbEnabled);
		
		c.gridy++;c.gridx = 0;
		c.gridwidth = 3;
		c.anchor = GridBagConstraints.CENTER;
		gridxslider = new JSlider(JSlider.HORIZONTAL,5,100,valuex);
		gridxslider.setSize(100, 30);
		gridxslider.addChangeListener(this);

		gridxslider.setMajorTickSpacing(25);
		gridxslider.setPaintTicks(true);
		content.add(gridxslider,c);
		c.gridy++;c.gridx = 1;
		c.gridwidth = 2;c.anchor = GridBagConstraints.EAST;
		gridinfox = new JLabel(gethorizText(valuex)+" ");
		content.add(gridinfox,c);
		c.gridy++; c.gridx = 0; 
		c.gridwidth = 3; c.anchor = GridBagConstraints.CENTER;
		gridyslider = new JSlider(JSlider.HORIZONTAL,5,100,valuey);
		gridyslider.setSize(100, 30);
		gridyslider.addChangeListener(this);

		gridyslider.setMajorTickSpacing(25);
		gridyslider.setPaintTicks(true);
		content.add(gridyslider,c);
		c.gridy++;c.gridx = 1;
		c.gridwidth = 2;c.anchor = GridBagConstraints.EAST;
		gridinfoy = new JLabel(getvertText(valuey)+" ");
		content.add(gridinfoy,c);
		c.gridy++; c.gridx = 1;
		c.gridwidth = 2; c.anchor = GridBagConstraints.EAST;
		rbSynchron = new JCheckBox("synchron");
		rbSynchron.addActionListener(this);
		rbSynchron.setSelected(false);
		content.add(rbSynchron,c);
		
		c.gridy++; c.gridx = 1;
		c.gridwidth = 2; c.anchor = GridBagConstraints.EAST;
		rbOrientated = new JCheckBox("Knoten am Raster ausrichten");
		rbOrientated.addActionListener(this);
		rbOrientated.setSelected(false);
		content.add(rbOrientated,c);
		
		c.gridy++; c.gridx = 0; 
		c.gridwidth = 1; c.anchor = GridBagConstraints.EAST;
		bCancel = new JButton("Abbrechen");
		bCancel.addActionListener(this);
		bCancel.setVisible(false);
		content.add(bCancel,c);

		c.gridx+=2;
		bOk = new JButton("Ok");
		bOk.setVisible(false);
		bOk.addActionListener(this);
		content.add(bOk,c);

		reload();
		content.revalidate();
	}
	
	/**
	 * aktualisiert die Werte in der Oberfläche.
	 * Dies ist emfehlenswert, wenn sich die Werte geändert haben.
	 *
	 */
	public void reload()
	{
		valuex = gp.getIntValue("grid.x");
		valuey = gp.getIntValue("grid.y");
		lastchanged = valuex;
		valuex = gridxslider.getValue();
		lastchanged = valuex;
		gridinfox.setText(gethorizText(valuex));
		gridinfoy.setText(getvertText(valuey));
		
		gridxslider.removeChangeListener(this);
		gridxslider.setValue(valuex);				
		gridxslider.addChangeListener(this);
		gridyslider.removeChangeListener(this);
		gridyslider.setValue(valuey);				
		gridyslider.addChangeListener(this);

		rbEnabled.setSelected(!gp.getBoolValue("grid.enabled"));
		rbEnabled.doClick();
		
		rbSynchron.setSelected(gp.getBoolValue("grid.synchron"));
		
		rbOrientated.setSelected(gp.getBoolValue("grid.orientated"));
		
		setChanged();
		notifyObservers("Grid");
	}

	/**
	 * Gibt die Einstellungsoberfläche des Rasterdialogs zurück
	 * 
	 * @return ein JPanel mit den Oberflächenelementen der Rastereinstellungen
	 */
	public JPanel getContent() //Zum Einblenden etwa als Tab
	{
		return content;
	}
	/**
	 * Gibt den aktuellen horizontalen Rasterabstand zurück
	 * @return Rasterabstand X
	 */
	public int getGridX()
	{
		return valuex;
	}
	/**
	 * Gibt den aktuellen vertikalen Rasterabstand zurück
	 * @return Rasterabstand Y
	 */	
	public int getGridY()
	{
		return valuey;
	}
	/**
	 * Gibt den Zustand zurück, der in der Oberfläche für die Synchronisierung der Abstände vorliegt
	 * @return true, falls die Abstände (x und y) synchronisiert sind, sonst false
	 */
	public boolean isSynchron()
	{
		return rbSynchron.isSelected();
	}
	/**
	 * Gibt an, ob das Raster aktiviert ist
	 * @return true, falls das Raster aktiviert ist, sonst false
	 */
	public boolean isEnabled()
	{
		return rbEnabled.isSelected();
	}
	/**
	 * Gibt zurück, ob das Ausrichten am Raster in der Oberfläche aktiviert ist 
	 * @return true, falls Knoten am Raster ausgerichtet werden sollen, sonst false
	 */
	public boolean isOrientated()
	{
		return rbOrientated.isSelected();
	}
	/**
	 * Setzt die Ausrichtung der Knoten am Raster auf den übergebenen Wert
	 * true - im Standardmodus werden Knoten nach dem Bewegen am Raster ausgerichtet, ebenso neu erstellte Knoten
	 * false - keine Ausrichtung am Raster
	 * 
	 * @param b neuer Wert der Ausrichtung
	 */
	public void setOrientated(boolean b)
	{
		rbOrientated.setSelected(b);
	}
	/**
	 * Setzt die Aktivität des Rasters
	 * true - Ratser aktiv (angezeigt)
	 * false - Raster deaktiviert (nicht angezeigt), deaktiviert ebenso alle Oberflächenelemente des Dialogs außer der Aktivierung
	 * 
	 * @param b neue Aktivität des Rasters
	 */
	public void setEnabled(boolean b)
	{
		rbEnabled.setSelected(!b);
		rbEnabled.doClick();
	}
	/**
	 * Setzt die Sichtbarkeit des Schalters zum (de)aktivieren des Rasters
	 * Kann verwendet werden, wenn das Raster definitiv aktiv sein soll und nur die Einstellungen des Rasters veränderbar sein sollen
	 * 
	 * @param b neue Sichtbarkeit des Schalters
	 */
	public void setEnableVisble(boolean b)
	{
		rbEnabled.setVisible(b);
		content.revalidate();
	}
	/**
	 * Opens Grid-Options in seperate Dialog (and therefore adds ESC-Handling 
	 * This Diaog can be made modal by the parameter boolean
	 * 
	 * @param modal sets the Dialog modal if true, els nonmodal
	 */
	public void showDialog(boolean modal)
	{
		GridDialog = new JDialog();
		GridDialog.setTitle("Raster-Einstellungen");
		bOk.setVisible(true);
		bCancel.setVisible(true); 
		GridDialog.setContentPane(content);
		GridDialog.validate();
		GridDialog.setResizable(false);
		GridDialog.setModal(true);
		GridDialog.pack();
		Point p = new Point(0,0);
		p.y += Math.round(Gui.getInstance().getParentWindow().getHeight()/2);
		p.x += Math.round(Gui.getInstance().getParentWindow().getWidth()/2);
		p.y -= Math.round(GridDialog.getHeight()/2);
		p.x -= Math.round(GridDialog.getWidth()/2);
		
		//ESC-Handling
		InputMap iMap = GridDialog.getRootPane().getInputMap(	 JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape");
		
		ActionMap aMap = GridDialog.getRootPane().getActionMap();
		aMap.put("escape", new AbstractAction()
			{
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e)
				{
					doQuit();
				}
		 	});

		
		GridDialog.setLocation(p.x,p.y);
		GridDialog.setVisible(true);
	}
	/**
	 * Wenn sich einer der Regler für die Raster-Abstände geändert hat, wird diese methode aktiviert
	 * 
	 */
	public void stateChanged(ChangeEvent e)
	{
		if (e.getSource()==gridxslider) 
		{
			valuex = gridxslider.getValue();
			lastchanged = valuex;
			gridinfox.setText(gethorizText(valuex));
			if (rbSynchron.isSelected())
			{
				valuey = valuex;
				gridyslider.removeChangeListener(this);
				gridyslider.setValue(valuey);				
				gridyslider.addChangeListener(this);
				gridinfoy.setText(getvertText(valuey));
			}
			setChanged();
			notifyObservers("Grid");
		}
		else if (e.getSource()==gridyslider)
		{	
			valuey = gridyslider.getValue();
			lastchanged = valuey;
			gridinfoy.setText(getvertText(valuey));
			if (rbSynchron.isSelected())
			{
				valuex = valuey;
				gridxslider.removeChangeListener(this);
				gridxslider.setValue(valuex);				
				gridxslider.addChangeListener(this);
				gridinfox.setText(gethorizText(valuex));
			}
			setChanged();
			notifyObservers("Grid");
		}
	}
	private void doQuit()
	{
		 //Setzt auf Anfangs-Werte zurück, informiert die Beobachter (dort neu zeichnen) und beendet den Dialog
		valuex = gp.getIntValue("grid.x");
		valuey = gp.getIntValue("grid.y");
		rbEnabled.setSelected(gp.getBoolValue("grid.enabled"));
		rbSynchron.setSelected(gp.getBoolValue("grid.synchron"));
		rbOrientated.setSelected(gp.getBoolValue("grid.orientated"));
		bOk.setVisible(false);
		bCancel.setVisible(false);
		setChanged();
		notifyObservers("Grid");
		GridDialog.dispose();
	}
	/**
	 * Alle Klicks auf  Checkbox-Elemente und Buttons werden hier behandel
	 */
	public void actionPerformed(ActionEvent e) 
	{
		if (e.getSource()==rbEnabled) 
		{ //Aktivieren und Deaktivieren des Rasters
			boolean activity = rbEnabled.isSelected();
			gridxslider.setEnabled(activity);
			gridinfox.setEnabled(activity);
			gridyslider.setEnabled(activity);
			gridinfoy.setEnabled(activity);
			rbSynchron.setEnabled(activity);			
			rbOrientated.setEnabled(activity);
			setChanged();
			notifyObservers("Grid");
		}
		else if (e.getSource()==rbSynchron)
		{ //Synchronisieren, setzt beide Werte uf den letzten geänderten 
			if (rbSynchron.isSelected())
			{
				valuex = lastchanged;
				valuey = lastchanged;
				gridyslider.removeChangeListener(this);
				gridxslider.removeChangeListener(this);
				gridxslider.setValue(valuex);				
				gridyslider.setValue(valuey);				
				gridyslider.addChangeListener(this);
				gridxslider.addChangeListener(this);
				gridinfox.setText(gethorizText(valuex));
				gridinfoy.setText(getvertText(valuey));
				setChanged();
				notifyObservers("Grid");
			}
		}
		//Die beiden Buttons setzen sich wieder auf unsichtbar nach beenden des einzeldialogs, so
		//läßt sich die Instanz der Klasse danach auch für den Zweck der integrierten Aneige verwenden
		//(Zurücksetzen auf Anfangswert)
		else if (e.getSource()==bOk)
		{ //Speichert die Einstellungen des Dialoges in die Einstellungen und beendet den Dialog
			gp.setBoolValue("grid.enabled",rbEnabled.isSelected());
			gp.setBoolValue("grid.synchron", rbSynchron.isSelected());
			gp.setBoolValue("grid.orientated", rbOrientated.isSelected());
			gp.setIntValue("grid.x",valuex);
			gp.setIntValue("grid.y",valuey);
			bOk.setVisible(false);
			bCancel.setVisible(false);
			GridDialog.dispose();
		}
		else if (e.getSource()==bCancel)
		{
			doQuit();
		}
	}
	/**
	 * liefert den Text für den aktuellen vertikalen Rasterabstand
	 * 
	 * @param val aktueller vertikaler Rasterabstand
	 * @return
	 */
	private String getvertText(int val)
	{
		String Text = "Rasterabstand vertikal: ";
		if (val < 10)
			Text +=" ";
		if (val < 100)
			Text += " ";
		Text += val;
		return Text;
	}
	/**
	 * liefert den Text für den aktuellen horizontaler Rasterabstand
	 * 
	 * @param val aktueller horizontaler Rasterabstand
	 * @return
	 */
	private String gethorizText(int val)
	{
		String Text = "Rasterabstand horizontal: ";
		if (val < 10)
			Text +=" ";
		if (val < 100)
			Text += " ";
		Text += val;
		return Text;
	}
}
