package dialogs;

import help.view.HelpPanel;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.util.Iterator;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import model.GraphStatisticAtoms;

import view.Gui;
import view.Statistics;
/**
 *  JStatisticsDialog
 *  For Creation of new entries in the Statistics Part of the GUI
 *
 * 	@author Ronny Bergmann 
 */
public class JStatisticsDialog extends JDialog implements ActionListener, TextListener
{

	private static final long serialVersionUID = 48L;

	final static String badNameChars = "`~!@#$^*()_+=\\|\"':;?/.-";
	
	private String oldName, oldExpr;
	private TextField iName, iExpr;
	private JLabel lName, lExpr, lDoubleValue, lError;
	private boolean isNew;
	//Beim Editieren zum testen der neuen Werte (im Vergleich zu den alten), Indikator fürs Editieren (==null falls erstellender Dialog)
	private JButton bOK, bCancel, bHelp;
	private TreeMap<String, String> bisherige_formeln;
	private Statistics parent;
	
	/**
	 * Konstruktor des Dialoges mit bisherigen Werten. Erstellt einen neuen Wert 
	 * 
	 * @param formeln bisherige erstellte Formeln in der Statistik
	 *
	 */
	public JStatisticsDialog(Statistics pParent, TreeMap<String, String> formeln)
	{
		bisherige_formeln = formeln;
		oldName = "";
		oldExpr="";
		isNew = true;
		parent = pParent;
		CreateDialog();
	}
	/**
	 * Initialize a new Dialog for a new Statistics entry
	 * @param pParent the given parent class where the new statistics entry should be added
	 * @param formeln previously created formulars
	 * @param pOldname for change dialogs the old Name and
	 * @param pOldvalueString old expression
	 */
	public JStatisticsDialog(Statistics pParent, TreeMap<String, String> formeln, String pOldname, String pOldvalueString)
	{
		bisherige_formeln = formeln;
		oldName = pOldname;
		oldExpr=pOldvalueString;
		isNew = false;
		parent = pParent;
		CreateDialog();
	}
	/**
	 * Create all Input Fields and the Dialog
	 *
	 */
	private void CreateDialog()
	{
		if (isNew)
		{
			this.setTitle("Neuer Statistik-Eintrag");
		}
		else
		{
			this.setTitle("Bearbeiten des Eintrags \""+oldName+"\"");	
		}		
		Container content = getContentPane();
		content.setLayout(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(7,7,7,7);
		c.anchor = GridBagConstraints.WEST;
		c.gridy = 0;
		c.gridx = 0;
		lName = new JLabel("Name");
		content.add(lName,c);
		c.gridx = 1;
		iName = new TextField();
		iName.addTextListener(this);
		iName.setPreferredSize(new Dimension(200, 20));
		content.add(iName,c);
		
		c.gridy++;
		c.gridx = 0;
		lExpr = new JLabel("Ausdruck");
		content.add(lExpr,c);
		c.gridx = 1;
		iExpr = new TextField();
		iExpr.addTextListener(this);
		iExpr.setPreferredSize(new Dimension(200, 20));
		
		content.add(iExpr,c);
		
		c.gridy++;
		c.gridx = 0; c.gridwidth=3;
		lError = new JLabel("");
		lError.setPreferredSize(new Dimension(400,35));
		content.add(lError,c);
		c.gridy++;
		c.gridx = 1;
		lDoubleValue = new JLabel("Wert :");
		lDoubleValue.setPreferredSize(new Dimension(400,20));
		content.add(lDoubleValue,c);

		c.gridy++; c.gridx=0; c.gridwidth=1;
		bCancel = new JButton("Abbrechen");
		bCancel.addActionListener(this);
		c.insets = new Insets(3,3,3,3);
		content.add(bCancel,c);
		
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 1;
		if (isNew)
			bOK = new JButton("Neuen Statistik Eintrag erstellen");
		else
			bOK = new JButton("<html>"+main.CONST.html_Ae+"nderungen speichern</html>");
		bOK.setMnemonic(KeyEvent.VK_ENTER);
		bOK.addActionListener(this);
		content.add(bOK,c);
		
		c.gridy=0; c.gridx=3; c.gridwidth=1;c.gridheight=2;
		bHelp = new JButton("Hilfe");
		bHelp.addActionListener(this);
		content.add(bHelp,c);
		bHelp.setEnabled(true);
		if (!isNew)
		{
			iName.setText(oldName);
			iExpr.setText(oldExpr);
			lDoubleValue.setText("Wert : "+parent.interpreteKey(oldName));
		}
		
		this.getRootPane().setDefaultButton(bOK);
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
	 * prüft, ob der momentane Arithmetische Ausdruck zu einer Rekursion führen würde.
	 * 
	 * <br><br>Setzt dabei vorraus, dass keine der bisherigen Formeln rekursiv ist. Dies ergibt sich daher, dass diese alle auch diesen Test gemacht haben und die erste Formel nur aus Atomen besteht und nicht rekursiv sein kann.
	 * 
	 * @return false, wenn keine Rekusrion vorliegt, sonst true
	 */
	private boolean ExprIsRecursive()
	{
		String[] bT = new String[bisherige_formeln.size()];
		int belegt = 0;int index = 0;
		String Expr = iExpr.getText();
		String Name = iName.getText();
		Iterator<String> iter = bisherige_formeln.keySet().iterator();
		while (iter.hasNext())
		{
			String actKey = iter.next();
			if (Expr.contains(actKey)) //Enthalten
			{
				bT[belegt++] = actKey;
			}
		}
		while ((index < bT.length)&&(bT[index]!=null))
		{
			//(1) enth�lt der aktuelle Wert den Namen ? dann liegt eine Rekursion vor
			String Check_Expr = bisherige_formeln.get(bT[index]);
			index++;
			if (Check_Expr.contains(Name))
				return true;
			//(2) da eine Rekursion auch über alle bekannten Formeln gehen kann, alle anderen Formeln in dieser hinten an bT anfügen, so sie nicht enthalten sind
			iter = bisherige_formeln.keySet().iterator();
			while (iter.hasNext())
			{
				String actKey = iter.next();
				if (Check_Expr.contains(actKey)) //Key enthalten in der aktuelle expression
				{
					boolean enthalten=false;
					for (int i=0; i<belegt; i++)
					{
						if (bT[i].equals(actKey))
							enthalten=true;
					}
					if (!enthalten) //und noch nich im Array
					{
						bT[belegt++] = actKey;
					}
				}
			}
		}
		return false;
	}
	/**
	 * Check the Given Name and set the error text, if the name is already used or 
	 * a prefix of a used name
	 *
	 */
	private void CheckName()
	{
		if ((iExpr.getText().equals(""))||(iName.getText().equals("")))
		{
			lError.setText(" ");
			return;
		}
		for (int i=0; i<GraphStatisticAtoms.ATOMS.length; i++)
		{
			if (GraphStatisticAtoms.ATOMS[i].equals(iName.getText()))
			{
				lError.setText("<html><font color=#AA0000>Name bereits vergeben!</font></html>");
				lDoubleValue.setText("Wert : #");
				return;
			}
			if (GraphStatisticAtoms.ATOMS[i].startsWith(iName.getText()))
			{
				lError.setText("<html><font color=#AA0000>Der gew"+main.CONST.html_ae+"hlte Name ist Prefix von \""+GraphStatisticAtoms.ATOMS[i]+"\". Namen m"+main.CONST.html_ue+"ssen Prefix-frei sein.</font></html>");
				lDoubleValue.setText("Wert : #");
				return;	
			}
			if (iName.getText().startsWith(GraphStatisticAtoms.ATOMS[i]))
			{
				lError.setText("<html><font color=#AA0000>Der gew"+main.CONST.html_ae+"hlte Name hat \""+GraphStatisticAtoms.ATOMS[i]+"\" als Prefix. Namen m"+main.CONST.html_ue+"ssen Prefix-frei sein.</font></html>");
				lDoubleValue.setText("Wert : #");
				return;	
			}
	
		}
		Iterator<String> iter = bisherige_formeln.keySet().iterator();
		while (iter.hasNext())
		{
			String actKey = iter.next();
			if (!actKey.equals(oldName))
			{	
				if (actKey.equals(iName.getText())&&(!actKey.equals(oldName)))
				{ //Ver�ndert aber vergeben
					lError.setText("<html><font color=#AA0000>Name bereits vergeben!</font></html>");
					lDoubleValue.setText("Wert : #");
					return;
				}
				if (actKey.startsWith(iName.getText()))
				{
					lError.setText("<html><font color=#AA0000>Der gew"+main.CONST.html_ae+"hlte Name ist Prefix von \""+actKey+"\". Namen m"+main.CONST.html_ue+"ssen Prefix-frei sein.</font></html>");
					lDoubleValue.setText("Wert : #");
					return;	
				}
				if (iName.getText().startsWith(actKey))
				{
					lError.setText("<html><font color=#AA0000>Der gew"+main.CONST.html_ae+"hlte Name hat \""+actKey+"\" als Prefix. Namen m"+main.CONST.html_ue+"ssen Prefix-frei sein.</font></html>");
					lDoubleValue.setText("Wert : #");
					return;		
				}
			}
		}
		lError.setText("");
	}
	/**
	 * Check the Expression and show its value if the expression is valid, else show an error message
	 */
	private void CheckExpr()
	{
		if ((iExpr.getText().equals(""))||(iName.getText().equals("")))
		{
			lError.setText(" ");
			return;
		}
		if (iExpr.getText().equals(iName.getText()))
		{
			lError.setText("<html><font color=#AA0000>Der Ausdruck ist identisch mit seinem eigenen Namen.</font></html>");
			return;
		}
		if (iExpr.getText().startsWith(iName.getText()))
		{
			if (("+-*/^()".indexOf(iExpr.getText().charAt(iName.getText().length()))==-1))
			{ //Expr beginnt zwar mit iName aber es ist ein anderer Prefix
			//	System.out.println("anderer Name ("+iExpr.getText().charAt(iName.getText().length())+")");
			}
			else
			{
				lError.setText("<html><font color=#AA0000>Der Ausdruck enth"+main.CONST.html_ae+"lt seinen eigenen Namen. Direkte Rekursion ist nicht erlaubt.</font></html>");
				lDoubleValue.setText("Wert : #");
				return;
			}
		}
		if (iExpr.getText().contains(iName.getText())) //rekursion
		{
			//Enthält zumindest den eignen Namen, ist das auch wirklich der Wert ?
			char beginning = iName.getText().charAt(0);//Existiert, da iName nicht leer
			for (int i=iName.getText().length(); i<iExpr.getText().length()-iName.getText().length()+1; i++)
			{
				if (iExpr.getText().charAt(i)==beginning)
				{
					if (("+-*/^()".indexOf(iExpr.getText().charAt(i-1))==-1))
					{ //Das Zeichen vor dem gefundenen Namen gehört zu einem namen (kein Arithmetisches Zeichen)=> keine rekursion
						//System.out.println("("+("+-*/^()".indexOf(iExpr.getText().charAt(i-1)))+")anderer Name ("+iExpr.getText().charAt(i-1)+")");
					}
					else if ((i+iName.getText().length()<iExpr.getText().length()&&(("+-*/^()".indexOf(iExpr.getText().charAt(i+iName.getText().length()))==-1)))) //Hinterm aktuellen iNameTest nochn Zeichen
					{	//Das zeichen hinter dem Namen (so existent) ist kein arithmetisches => andrer Name
						//System.out.println("anderer Name ("+iExpr.getText().charAt(i+iName.getText().length())+")");
					}
					else
					{	//Kommt iName an dieser Stelle vor ?
						if (iExpr.getText().subSequence(i,i+iName.getText().length()).equals(iName.getText()))
						{
							//System.out.println("iName found at"+i);
							//Kommt exakt hier vor, davor dahinter sind arithmetische Zeichen => Rekursion, direkte 
							lError.setText("<html><font color=#AA0000>Der Ausdruck enth"+main.CONST.html_ae+"lt seinen eigenen Namen. Direkte Rekursion ist nicht erlaubt</font></html>");
							lDoubleValue.setText("Wert : #");
							return;
						}
					}
				}
			}
		
		}
		Iterator<String> iter = bisherige_formeln.keySet().iterator();
		while (iter.hasNext())
		{
			String actKey = iter.next();
			if ((iExpr.getText().contains(actKey))&&(bisherige_formeln.get(actKey).contains(iName.getText())))
			{ //rekursion
				lError.setText("<html><font color=#AA0000>"+iName.getText()+" und "+actKey+" rufen sich gegenseitig auf. Rekursion ist jedoch nicht erlaubt.</font></html>");
				lDoubleValue.setText("Wert : #");
				return;
			}
		}
		if (ExprIsRecursive())
		{
			lError.setText("<html><font color=#AA0000>"+iName.getText()+" bewirkt "+main.CONST.html_ue+"ber einige Terme eine Rekursion. Rekursion ist jedoch nicht erlaubt.</font></html>");
			lDoubleValue.setText("Wert : #");
			return;	
		}
		lDoubleValue.setText("Wert : "+parent.interprete(iExpr.getText()));
		lError.setText("");
	}
	
	@SuppressWarnings("static-access")
	public void actionPerformed(ActionEvent event)
	{
		if (event.getSource()==bHelp)
		{
			HelpPanel.getInstance().showHelp("stats.atoms");
		}
		if (event.getSource()==bCancel)
		{
			this.dispose();
		}
		else if (event.getSource()==bOK)
		{
			CheckName();
			if (lError.getText().equals(""));
				CheckExpr();
			
			if (lError.getText().equals(""))
			{
				if (!isNew)
					parent.removeValue(oldName);
				parent.setValue(iName.getText(), iExpr.getText());
				this.dispose();
			}
			else
			{
				if (!isNew)
					JOptionPane.showMessageDialog(this, "<html><p>"+main.CONST.html_Ae+"ndern des Eintrags nicht m"+main.CONST.html_oe+"glich.<br><br>Entweder wird ein Fehler angezeigt oder einige Felder wurden nicht ausgef"+main.CONST.html_ue+"llt.</p></html>", "Fehler", JOptionPane.ERROR_MESSAGE);
				else
					JOptionPane.showMessageDialog(this, "<html><p>Erstellen des Eintrags nicht m"+main.CONST.html_oe+"glich.<br><br>Entweder wird ein Fehler angezeigt oder einige Felder wurden nicht ausgef"+main.CONST.html_ue+"llt.</p></html>", "Fehler", JOptionPane.ERROR_MESSAGE);
			}
		} 
	}
	
	public void textValueChanged(TextEvent e)
	{
		if ((e.getSource()==iName)||(e.getSource()==iExpr)) //Name geändert
		{
			CheckName();
			if (lError.getText().equals(""))
				CheckExpr();
		}
	}
}
