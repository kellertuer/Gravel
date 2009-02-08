package view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.StringReader;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.TreeMap;


import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import model.GraphStatisticAtoms;

import dialogs.JStatisticsDialog;

import etc.StringLexer;
import etc.StringParser;

/**
 * Diese Klasse repräsentiert die Statistik in der Oberfläche.
 * <br>Die Klasse ist ein Observer des Graphen und aktualisiert sich dementsprechend.
 * <br>
 * <br>Außerdem sind die Einträge in der Statistik individuell editierbar
 * <br> Beobachtet die Atome.
 * @author Ronny Bergmann
 *
 */
public class Statistics extends JPanel implements Observer, ActionListener 
{
	private class MyDefaultTableModel extends DefaultTableModel
	{
		private static final long serialVersionUID = 1L;
		public MyDefaultTableModel(int x, int y)
		{
			super(x,y);
		}
	    public boolean isCellEditable(int row, int column)
	    {
	        return false;
	    }
	};
	private static final long serialVersionUID = 1L;
	//Zuordnungen Name -> Formel (nicht arithmethisch, enthält Variablen
	private TreeMap<String, String> formeln;
	private Double[] values;
	private GraphStatisticAtoms vgs;
	private JTable StatsTable;
	private DefaultTableModel StatsModel;
	private JButton bPlus,bMinus,bEdit;
	/**
	 * Init Statistics based on a VGraphic g
	 * @param g
	 */
	public Statistics(VGraphic g)
	{
		vgs = new GraphStatisticAtoms(g);
		vgs.addObserver(this);
		formeln = new TreeMap<String,String>();
		formeln.put("Knoten","$Node.Count");
		formeln.put("Kanten","$Edge.Count");
		
		setLayout(new BorderLayout());

		JPanel ButtonArea = new JPanel();
		
		ButtonArea.setLayout(new BoxLayout(ButtonArea, BoxLayout.Y_AXIS));
		ButtonArea.setSize(new Dimension(25,80));
		/*GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(0,0,0,0);
		c.anchor = GridBagConstraints.SOUTH;
		c.gridy=0;c.gridx = 0;c.gridwidth=1; */
		ButtonArea.add(Box.createVerticalGlue());
		String curDir = System.getProperty("user.dir");
		curDir+="/data/img/plusicon.gif";
		bPlus = new JButton(new ImageIcon(curDir));	
		bPlus.addActionListener(this);
		bPlus.setSize(new Dimension(17,17));
		ButtonArea.add(bPlus);
		curDir = System.getProperty("user.dir");
		curDir+="/data/img/minusicon.gif";
		bMinus = new JButton(new ImageIcon(curDir));	
		bMinus.addActionListener(this);
		bMinus.setSize(new Dimension(17,17));
		//c.gridy++;
		ButtonArea.add(bMinus);
		curDir = System.getProperty("user.dir");
		curDir+="/data/img/pencilicon.gif";
		bEdit = new JButton(new ImageIcon(curDir));	
		bEdit.addActionListener(this);
		bEdit.setSize(new Dimension(17,17));
		//c.gridy++;
		ButtonArea.add(bEdit);
		ButtonArea.add(Box.createRigidArea(new Dimension(17,10)));
		ButtonArea.validate();
		ButtonArea.doLayout();
		add(ButtonArea,BorderLayout.EAST);
		buildTable();
		
		JPanel northtable = new JPanel();
		
		northtable.setLayout(new BoxLayout(northtable,BoxLayout.Y_AXIS));
		JLabel title = new JLabel("<html><div align=center>Statistik</div></html>");
		northtable.add(title);
		add(northtable,BorderLayout.NORTH);
		JScrollPane TabScroll = new JScrollPane(StatsTable);
		TabScroll.setMinimumSize(new Dimension(120,100));
		//StatsTable.setFillsViewportHeight(true);
		StatsTable.setShowVerticalLines(true);
		StatsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		add(TabScroll,BorderLayout.CENTER);
		this.validate();
		//this.doLayout();
	}
	/**
	 * Set activity of the Statistics
	 * @param a true, if active, else false
	 */
	public void setActive(boolean a)
	{
		bPlus.setVisible(a);
		bEdit.setVisible(a);
		bMinus.setVisible(a);
		StatsTable.setEnabled(a);
	}
	/**
	 * Set Rows, that are the Table-Entries
	 * @param f
	 */
	public void setRows(TreeMap<String, String> f)
	{
		formeln = f;
		refreshTable();
	}
	/**
	 * Return the actual Formulars represented in the statistics
	 * @return
	 */@SuppressWarnings("unchecked")	
	public TreeMap<String, String> getRows()
	{
		return (TreeMap<String, String>)formeln.clone();
	}
	/**
	 * Build the Table with its model
	 */
	private void buildTable()
	{
		StatsModel = new MyDefaultTableModel(0,0);
		CalcValues();
		StatsModel.addColumn("Name", formeln.keySet().toArray());
		Object[] tabvals = new Object[values.length];
		for (int i=0; i<tabvals.length; i++)
		{
			if ((new Double(values[i])).toString().endsWith(".0"))
				tabvals[i] = Math.round(values[i]);
			else
				tabvals[i] = values[i];
		}
		StatsModel.addColumn("Wert", tabvals);
		StatsTable = new JTable(StatsModel);
		StatsTable.getColumnModel().setColumnMargin(0);
		
		//StatsTable.setSize(70,100);
		StatsTable.setEnabled(true);
		StatsTable.getTableHeader().setVisible(true);
		StatsTable.getTableHeader().setReorderingAllowed(false);
		StatsTable.setRowSelectionAllowed(true);
		//StatsTable.setCellSelectionEnabled(false);
		StatsTable.setColumnSelectionAllowed(false);	
		((JLabel)StatsTable.getDefaultRenderer(Object.class)).setHorizontalAlignment (JLabel.RIGHT);
		((JLabel)StatsTable.getDefaultRenderer(String.class)).setHorizontalAlignment (JLabel.LEFT);
		
	}
	/**
	 * Refresh Data in the table
	 */
	private void refreshTable()
	{
		StatsModel = new MyDefaultTableModel(0,0);
		CalcValues();
		StatsModel.addColumn("Name", formeln.keySet().toArray());
		Object[] tabvals = new Object[values.length];
		for (int i=0; i<tabvals.length; i++)
		{
			if ((new Double(values[i])).toString().endsWith(".0"))
				tabvals[i] = Math.round(values[i]);
			else
				tabvals[i] = values[i];
		}
		StatsModel.addColumn("Wert", tabvals);
		StatsTable.setModel(StatsModel);
		StatsTable.getTableHeader().setReorderingAllowed(false);
	}
	/**
	 * (Re)Calculate Values in the right column
	 */
	private void CalcValues()
	{
		Iterator<String> iter = formeln.keySet().iterator();
		values = new Double[formeln.size()];
		int i=0;
		while (iter.hasNext())
		{
			String actkey = iter.next();
			values[i++] = interprete(formeln.get(actkey));
		}
		
	}
	/**
	 * Interprete (Parse) one Formular identificated by its Name
	 * @param key the formular identifier
	 * @return Parsed Value
	 */
	public double interpreteKey(String key)
	{
		String formular = formeln.get(key);
		if (!formular.contains(key)) //keine Rekursion enthalten
			return interprete(formular);
		else
			return Double.NaN;
	}
	/**
	 * Interprese (Parse, Calculate) a String
	 * @param s
	 * @return
	 */
	public double interprete(String s)
	{	
		for (int i=0; i<GraphStatisticAtoms.ATOMS.length; i++)
		{
			s = replace(s,GraphStatisticAtoms.ATOMS[i], vgs.getValuebyName(GraphStatisticAtoms.ATOMS[i]).toString());
		}
		Iterator<String> iter = formeln.keySet().iterator();
		while (iter.hasNext())
		{
			String actKey = iter.next();
			String formular = formeln.get(actKey);
			if ((s.contains(actKey))&&(!formular.contains(s))) //Nur wenn s nicht die Formel ist und der Key vorkommt...
			//ersteres verhindert rekursion im Term zweiteres direkte Rekursion
			s = replace(s, actKey, new Double(interprete(formeln.get(actKey))).toString());
		}
		//System.err.println("...evaluating "+s);
		return evaluate(s);
	}
	/**
	 * Parse s
	 * @param s
	 * @return
	 */
	private double evaluate(String s)
	{
		StringReader reader = new StringReader(s);
		StringLexer lexer = new StringLexer(reader);
		StringParser parser = new StringParser(lexer);
		double erg = 0;
		try 
		{
			erg = parser.expr();
		}
		catch (Exception e)
		{
			erg = Double.NaN;
		}
		return erg;
	}	
	/**
	 * Einfache SubStringersetzung
	 * @param in Eingabe
	 * @param remove entfernen und 
	 * @param replace ersetzen durch diesen
	 * @return
	 */
	public static String replace(String in,String remove, String replace) 
	{
		if (in==null || remove==null || remove.length()==0) return in;
		StringBuffer sb = new StringBuffer();
		int oldIndex = 0;
		int newIndex = 0;
		int remLength = remove.length();
		while ( (newIndex = in.indexOf(remove,oldIndex)) > -1) 
		{
				//copy from last to new appearance
				sb.append(in.substring(oldIndex,newIndex));
				sb.append(replace);
				//set old index to end of last apperance.
				oldIndex = newIndex + remLength;
		}
		int inLength = in.length();
		//add part after last appearance of string to remove
		if(oldIndex<inLength) sb.append(in.substring(oldIndex,inLength));
		return sb.toString();
	}
	/**
	 * Get the Value of a fomular identified by its key
	 * @param ValKey
	 * @return
	 */
	public double getValue(String ValKey)
	{
		if (formeln.get(ValKey)!=null)
			return interprete(formeln.get(ValKey));
		else
			return Double.NEGATIVE_INFINITY;
	}
	/**
	 * Get Formular identified by its String
	 * @param ValKey
	 * @return
	 */
	public String getValueString(String ValKey)
	{
		return formeln.get(ValKey);
	}
	/**
	 * Set Formular
	 * @param valKey
	 * @param Expr
	 */
	public void setValue(String valKey, String Expr)
	{
		formeln.put(valKey, Expr);
	}
	/**
	 * Remove entry
	 * @param valKey
	 */
	public void removeValue(String valKey)
	{
		formeln.remove(valKey);
	}
	
	public void update(Observable arg0, Object arg1) {
		refreshTable();
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource()==bPlus)
		{
			new JStatisticsDialog(this,formeln);
			this.refreshTable();	
		}
		else if (e.getSource()==bMinus)
		{
			System.out.println("Deleting Row "+StatsTable.getSelectedRow());
			if (StatsTable.getSelectedRow()!=-1)
				formeln.remove(StatsModel.getValueAt(StatsTable.getSelectedRow(), 0));
			refreshTable();
		}
		else if (e.getSource()==bEdit)
		{
			if (StatsTable.getSelectedRow()!=-1)
			{
				new JStatisticsDialog(this,formeln,StatsModel.getValueAt(StatsTable.getSelectedRow(), 0).toString(),formeln.get(StatsModel.getValueAt(StatsTable.getSelectedRow(), 0)));
			}
			refreshTable();
		}
	}
}
