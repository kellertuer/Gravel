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
		paintDEBUG(g2);
	}
	private void paintDEBUG(Graphics2D g2)
	{
		Vector<Double> weights = new Vector<Double>();
		weights.add(.5);weights.add(.5);weights.add(.4);weights.add(.7); weights.add(.8); weights.add(.5);
		Vector<Point2D> points = new Vector<Point2D>();
		points.add(new Point2D.Double(50d,150d));	points.add(new Point2D.Double(150d,400d));
		points.add(new Point2D.Double(300d,25d));
		points.add(new Point2D.Double(350d,150d));
		points.add(new Point2D.Double(450d,30d));
		points.add(new Point2D.Double(450d,80d));
		Vector<Double> knots = new Vector<Double>();
		knots.add(0d);knots.add(0d);knots.add(0d);knots.add(0d);
		knots.add(.33d);
		knots.add(.66d);
		knots.add(1d);knots.add(1d);knots.add(1d);knots.add(1d);
		VHyperEdgeShape c = new  VHyperEdgeShape(knots,points,weights,3);
		g2.setStroke(new BasicStroke(2,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));
		VHyperEdgeShape cs = c.clone();
		cs.scale(zoomfactor);
		g2.draw(cs.getCurve(5d/(double)zoomfactor));
		Point2D p = new Point2D.Double(80d,170d);
		drawCP(g2,new Point(Math.round((float)p.getX()),Math.round((float)p.getY())),Color.magenta);
		NURBSShapeProjection proj = new NURBSShapeProjection(c,p);

		Vector<VHyperEdgeShape> parts = proj.DecomposeCurve(c);
		for (int i=0; i<parts.size(); i++)
		{
			System.err.println(parts.size());
			if (i%2==0)
				g2.setColor(selColor.GREEN.brighter());
			else
				g2.setColor(selColor);

			VHyperEdgeShape s = parts.get(i).clone();
			s.translate(0,10d);
			s.scale(zoomfactor);
			g2.draw(s.getCurve(5d/(double)zoomfactor));
		}
			
	}
	/**
	 * @param g
	 */
	private void paintHyperEdges(Graphics2D g2)
	{
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
				g2.draw(temp.getLinestyle().modifyPath(s.getCurve(5d/(double)zoomfactor),temp.getWidth(),zoomfactor));
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