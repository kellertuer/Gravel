package view;

import history.CommonGraphHistoryManager;

import java.awt.*;
import java.awt.event.MouseEvent;

import java.util.Iterator;
import java.util.Observable;
import java.util.Vector;

import control.*;
import model.*;
import model.Messages.*;

/**
 * class VGraphic
 * - Implementierung der Darstellung eines Graphen in einer Graphics2D Umgebung
 * 
 * @author Ronny Bergmann
 *
 */
public class VGraphic extends VCommonGraphic
{
	// VGraph : Die Daten des Graphen
	private VGraph vG;
	// Visual Styles
	private BasicStroke vEdgeStyle;
	//Mouse Handler
	private DragMouseHandler Drag;
	private ClickMouseHandler Click;
	private static final long serialVersionUID = 1L;
	
	/**
	 * Create a New Graphical representation of an VGraph with a given size
	 * @param d Size of the Area the VGraphics gets
	 * @param Graph Graph to be represented
	 */
	public VGraphic(Dimension d,VGraph Graph) {
		super(d,Graph);
		vEdgeStyle = new BasicStroke(5.0f, BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND);
		
		vG = Graph;
		vG.addObserver(this); //Die Graphikumgebung als Observer der Datenstruktur eintragen
		vGh = new CommonGraphHistoryManager(vG);
		gp.addObserver(this);
	}
	
	public void paint(Graphics g) 
	{
		Graphics2D g2 = (Graphics2D) g;
		//Mit Antialiasing
		paint(g2);
	}
	
	public void paint(Graphics2D g2)
	{
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		paintgrid(g2);
		paintEdges(g2);
		paintNodes(g2);
		if (gp.getBoolValue("vgraphic.cpshow"))
			paintControllPoints(g2);
		if ((Drag!=null)&&(Drag.getSelectionRectangle()!=null))
		{
			g2.setColor(selColor);
			g2.setStroke(new BasicStroke(1,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));
			g2.draw(Drag.getSelectionRectangle());
		}
	}
	/**
	 * @param g
	 */
	private void paintEdges(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g;
		Iterator<VEdge> ei = vG.modifyEdges.getIterator();
		g2.setStroke(vEdgeStyle);
		boolean directed = vG.getMathGraph().isDirected();
		while (ei.hasNext()) // drawEdges
		{
			VEdge temp = ei.next(); //Grafischer Teil
			MEdge tempm = vG.getMathGraph().modifyEdges.get(temp.getIndex());
			Point p1 = vG.modifyNodes.get(tempm.StartIndex).getPosition(); //Startkoordinaten
			VNode EndNode = vG.modifyNodes.get(tempm.EndIndex); //Endknoten
			VNode StartNode = vG.modifyNodes.get(tempm.StartIndex); //Endknoten
			Point p2 = EndNode.getPosition();
			if ((((temp.getSelectedStatus()&VItem.SELECTED)==VItem.SELECTED)||((temp.getSelectedStatus()&VItem.SOFT_SELECTED)==VItem.SOFT_SELECTED))&&((temp.getSelectedStatus()&VItem.SOFT_DESELECTED)!=VItem.SOFT_DESELECTED))
			{
				//Falls die Kante Selektiert ist (und nicht temporär deselektiert, zeichne drunter eine etwas dickere Kante in der selectioncolor
				g2.setColor(selColor);
				vEdgeStyle = new BasicStroke(Math.round((temp.getWidth()+selWidth)*zoomfactor),BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
				g2.setStroke(vEdgeStyle);
				g2.draw(temp.getPath(p1,p2,zoomfactor));
				if (directed) g2.fill(temp.getArrowShape(p1,p2,Math.round(StartNode.getSize()/2),Math.round(EndNode.getSize()/2),zoomfactor));
			}
			g2.setColor(temp.getColor());
			vEdgeStyle = new BasicStroke(Math.round(temp.getWidth()*zoomfactor),BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
			g2.setStroke(vEdgeStyle);
			g2.draw(temp.getDrawPath(p1,p2,zoomfactor));
			if (directed) g2.fill(temp.getArrowShape(p1,p2,Math.round(StartNode.getSize()/2),Math.round(EndNode.getSize()/2),zoomfactor));
			//And finally draw the text if visible
			if (temp.getTextProperties().isVisible()) //Visible
			{
				Point m = temp.getTextCenter(p1,p2);
				//get the text wich should be displayd
			    String text = "";
			    if (temp.getTextProperties().isshowvalue())
					text = ""+tempm.Value;
			    else
			    	text = tempm.name;
			    //Show it
				Font f = new Font("Arial",Font.PLAIN, Math.round(temp.getTextProperties().getSize()*zoomfactor));
				g2.setFont(f);
				g2.setColor(Color.black);
				FontMetrics metrics = g2.getFontMetrics(f);
			    int hgt = metrics.getAscent()-metrics.getLeading()-metrics.getDescent();
			    if (text==null)
			    	text = "";
			    int adv = metrics.stringWidth(text);
			    m.x = Math.round(m.x*zoomfactor);
				m.y = Math.round(m.y*zoomfactor);
				//adjust the point form the middle to the bottom left corner
				m.x -= Math.round(adv/2); m.y += Math.round(hgt/2); //For Drawing, move to Top Left
				g2.drawString(text,m.x,m.y);
			}
		}
	}
	/**
	 * Paint Edge Controll-Points in the Graphic
	 * @param g
	 */
	private void paintControllPoints(Graphics g)
	{
		Iterator<VEdge> edgeiter = vG.modifyEdges.getIterator();
		while (edgeiter.hasNext()) // drawEdges
		{
			VEdge temp = edgeiter.next(); //Grafischer Teil
			Vector<Point> p = temp.getControlPoints();
			for (int i=0; i<p.size(); i++)
				drawCP(g,p.get(i), Color.BLUE.brighter());
		}
	}
	/**
	 * Get the represented Graph for manipulation.
	 * The Manipulation is handled by pushing Notifications to the Graph-Observers 
	 * 
	 * @return the actual VGraph that is handled in the this GUI
	 */
	public VGraph getGraph()
	{
		return vG;
	}
	public void setMouseHandling(int state) {
		if (Drag!=null)
		{
			MouseEvent e = new MouseEvent(this,111,System.nanoTime(),0,-1,-1,1,false);		
			Drag.mouseReleased(e);
		}
		switch (state) 
		{
			case NO_MOUSEHANDLING:
				resetMouseHandling();
			break;
			case OCM_MOUSEHANDLING:
				resetMouseHandling();
				Click = new OCMClickMouseHandler(this);
				Drag = new OCMDragMouseHandler(this);
				this.addMouseListener(Drag);
				this.addMouseMotionListener(Drag);
				this.addMouseListener(Click);
			break;
			case STD_MOUSEHANDLING:
			default:
				resetMouseHandling();
				Click = new StandardClickMouseHandler(this);
				Drag = new StandardDragMouseHandler(this);
				this.addMouseListener(Drag);
				this.addMouseMotionListener(Drag);
				this.addMouseListener(Click);
				break;
		}
		if (Drag!=null) //Update Info in the Drag-Handler about the Grid.
		{
			Drag.setGrid(gridy,gridy);
			Drag.setGridOrientated(gridenabled&&gridorientated);
		}
		repaint();
	}
	/**
	 * Set the MouseHandling to NO_MOUSEHANDLING
	 */
	private void resetMouseHandling()
	{
		if (Drag!=null)
			Drag.removeGraphObservers();
		if (Click!=null)
			Click.removeGraphObservers();
		this.removeMouseListener(Drag);
		this.removeMouseMotionListener(Drag);
		this.removeMouseListener(Click);
		Drag = null;
		Click = null;
	}

	protected Point DragMouseOffSet()
	{
		if ((Drag!=null)&&(Drag.dragged()))
				return Drag.getMouseOffSet();
		else
			return null;
	}
	public void handlePreferencesUpdate()
	{
		super.handlePreferencesUpdate();
		if (Drag!=null)
		{
			Drag.setGridOrientated(gridenabled&&gridorientated);
			Drag.setGrid(gridx,gridy);
		}
	}
	public void update(Observable o, Object arg)
	{
		super.updateControls(o,arg);
		if (arg instanceof GraphMessage) //All Other GraphUpdates are handled in VGRaphCommons
		{
			if (Click!=null) 
				Click.update(o,arg);
			repaint();
		}
		else if (super.Controls.containsKey((String)arg)) //We got news from grid or zoom
			handlePreferencesUpdate();
	}

	public int getType() {
		return VCommonGraphic.VGRAPHIC;
	}
}