package view;


import history.GraphHistoryManager;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
//import javax.swing.*;
import java.util.Iterator;
import java.util.Observable;
import java.util.Vector;

import control.*;
import model.*;
import model.Messages.*;

/**
 * - Implementierung der Darstellung eines Hypergraphen in einer Graphics2D Umgebung
 * 
 * @author Ronny Bergmann
 *
 */
public class VHyperGraphic extends VCommonGraphic
{
	// VGraph : Die Daten des Graphen
	private VHyperGraph vG;
	// Visual Styles
	private BasicStroke vHyperEdgeStyle;
	private DragMouseHandler Drag;
	private ClickMouseHandler Click;
	private static final long serialVersionUID = 1L;
	
	/**
	 * Create a New Graphical representation of an VGraph with a given size
	 * @param d Size of the Area the VGraphics gets
	 * @param Graph Graph to be represented
	 */
	public VHyperGraphic(Dimension d,VHyperGraph Graph)
	{
		super(d,Graph);
		//GeneralPreferences als beobachter eintragen

		vHyperEdgeStyle = new BasicStroke(5.0f, BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND);
		selColor = new Color(gp.getIntValue("vgraphic.selcolr"),gp.getIntValue("vgraphic.selcolg"),gp.getIntValue("vgraphic.selcolb"));
		selWidth = gp.getIntValue("vgraphic.selwidth");
		
		vG = Graph;
		vG.addObserver(this); //Die Graphikumgebung als Observer der Datenstruktur eintragen
		//TODO: HistoryManager Umschreiben
		vGh = new GraphHistoryManager(new VGraph(true,true,true));
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
		paintHyperEdges(g2);
		paintNodes(g2);
		if ((Drag!=null)&&(Drag.getSelectionRectangle()!=null))
		{
			g2.setColor(selColor);
			g2.setStroke(new BasicStroke(1,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));
			g2.draw(Drag.getSelectionRectangle());
		}
		Vector<Object> Params = new Vector<Object>();
		Params.setSize(NURBSShapeFactory.MAX_INDEX);
		Vector<Point2D> Q = new Vector<Point2D>();
		Q.add(new Point2D.Double(60,40));
		Q.add(new Point2D.Double(90,80));
		Q.add(new Point2D.Double(70,80));
		Q.add(new Point2D.Double(20,40));
		Q.add(new Point2D.Double(200,100));
		Params.set(NURBSShapeFactory.IP_POINTS,Q.clone());
		Params.set(NURBSShapeFactory.DEGREE,3);
		Params.set(NURBSShapeFactory.DISTANCE_TO_NODE,0);
		VHyperEdgeShape s = NURBSShapeFactory.CreateShape("global interpolation",Params);
		s.scale(zoomfactor);
		g2.setColor(Color.BLACK);
		g2.setStroke(new BasicStroke(2*zoomfactor,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));
		g2.draw(s.getCurve(0.002d));
		for (int i=0; i<Q.size(); i++)
		{
			drawCP(g2, new Point(Math.round((float)Q.get(i).getX()), Math.round((float)Q.get(i).getY())), Color.pink);
			g2.setColor(Color.black);					
			Font f = new Font("Arial",Font.PLAIN, Math.round(12*zoomfactor));
			g2.setFont(f);
			//mittelpunkt des Textes
			int x = Math.round((float)Q.get(i).getX());
			int y = Math.round((float)Q.get(i).getY());
			
		    FontMetrics metrics = g2.getFontMetrics(f);
		    int hgt = metrics.getAscent()-metrics.getLeading()-metrics.getDescent();
		    int adv = metrics.stringWidth("Q"+i);
		    x = Math.round(x*zoomfactor);
		    y = Math.round(y*zoomfactor);
		    x -= Math.round(adv/2); y += Math.round(hgt/2);
			g2.drawString("Q"+i, x,y);

		}
	}
	/**
	 * @param g
	 */
	private void paintHyperEdges(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g;
		Iterator<VHyperEdge> ei = vG.modifyHyperEdges.getIterator();
		g2.setStroke(vHyperEdgeStyle);
		while (ei.hasNext()) // drawEdges
		{
			VHyperEdge temp = ei.next();
			g2.setColor(temp.getColor());
			g2.setStroke(new BasicStroke(temp.getWidth()*zoomfactor,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));
			if (!temp.getShape().isEmpty())
			{
				VHyperEdgeShape s = temp.getShape().clone();
				s.scale(zoomfactor);
				g2.draw(temp.getLinestyle().modifyPath(s.getCurve(0.02d/(double)zoomfactor),temp.getWidth(),zoomfactor));
			}
		}
	}
	/**
	 * Get the represented Graph for manipulation.
	 * The Manipulation is handled by pushing Notifications to the Graph-Observers 
	 * 
	 * @return the actual VGraph that is handled in the this GUI
	 */
	public VHyperGraph getGraph()
	{
		return vG;
	}
	public void setMouseHandling(int state) {
		if (Drag!=null)
		{
			MouseEvent e = new MouseEvent(this,111,System.nanoTime(),0,-1,-1,1,false);		
			Drag.mouseReleased(e);
		}
		resetMouseHandling();
		switch (state) 
		{
			case NO_MOUSEHANDLING:
			break;
			case OCM_MOUSEHANDLING:
				Click = new OCMClickMouseHandler(this);
				Drag = new OCMDragMouseHandler(this);
				this.addMouseListener(Drag);
				this.addMouseMotionListener(Drag);
				this.addMouseListener(Click);
			break;
			case STD_MOUSEHANDLING:
			default:
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
	public void handlePreferencedUpdate()
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
		else if (o.equals(gp)) //We got news from gp
			handlePreferencesUpdate();
	}
	public int getType() {
		return VCommonGraphic.VHYPERGRAPHIC;
	}

}