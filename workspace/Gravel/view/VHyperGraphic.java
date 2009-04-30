package view;


import history.GraphHistoryManager;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
//import javax.swing.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Queue;
import java.util.Vector;

import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Map.Entry;

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
	//	paintDEBUG(g2);
	}
	private void paintDEBUG(Graphics2D g2)
	{
		Vector<Point2D> IP = new Vector<Point2D>();
		IP.add(new Point2D.Double(60,200));
		IP.add(new Point2D.Double(50,130));
		IP.add(new Point2D.Double(150,60));
		IP.add(new Point2D.Double(270,80));
		IP.add(new Point2D.Double(270,120));
		IP.add(new Point2D.Double(220,130));
		IP.add(new Point2D.Double(200,200));
		IP.add(new Point2D.Double(200,230));
		IP.add(new Point2D.Double(270,230));
		IP.add(new Point2D.Double(230,290));
		IP.add(new Point2D.Double(200,280));
		IP.add(new Point2D.Double(175,290));
		IP.add(new Point2D.Double(185,320));
		IP.add(new Point2D.Double(80,350));
		int degree = 4;
		NURBSShape c = NURBSShapeFactory.CreateInterpolation(IP,degree);
		
		g2.setStroke(new BasicStroke(1.2f,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));
		NURBSShape cs = c.clone();
		cs.scale(zoomfactor);
		g2.setColor(Color.black);
		g2.draw(cs.getCurve(5d/(double)zoomfactor));
		VHyperEdge e = vG.modifyHyperEdges.get(1);
		if (e==null)
			return;
		//
		// Start of Validation-Algorithm
		//
		NURBSShapeValidator s = new NURBSShapeValidator(vG,1,c);
	}
	private void paintPIDEBUG(Graphics2D g2)
	{
		Vector<Double> knots = new Vector<Double>();
		for (int i=0; i<=20; i++)
			knots.add(((double)i-3d)/6d);
		
		Vector<Point2D> points = new Vector<Point2D>();
		Vector<Double> weights = new Vector<Double>();
		points.add(new Point2D.Double(30d,5d)); weights.add(.8);
		points.add(new Point2D.Double(50d,45d)); weights.add(.7);
		points.add(new Point2D.Double(150d,275d)); weights.add(1d);
		points.add(new Point2D.Double(300d,75d)); weights.add(1d);
		points.add(new Point2D.Double(400d,175d)); weights.add(2d);
		points.add(new Point2D.Double(500d,75d)); weights.add(2d);
		points.add(new Point2D.Double(500d,100d)); weights.add(2d);
		points.add(new Point2D.Double(550d,65d)); weights.add(2d);
		points.add(new Point2D.Double(400d,50d)); weights.add(2d);
		points.add(new Point2D.Double(200d,95d));weights.add(3d);
		for (int i=0; i<5; i++)
		{
			points.add((Point2D)points.get(i).clone());
			weights.add(weights.get(i).doubleValue());
		}
		for (int i=0; i<points.size(); i++)
		{
			float gv = 0f;
			drawCP(g2,new Point(Math.round((float)points.get(i).getX()),Math.round((float)points.get(i).getY())),new Color(gv,gv,gv));
		}
		g2.setColor(Color.black);
		NURBSShape c = new  NURBSShape(knots,points,weights);
//		System.err.println(c.degree);
		g2.setStroke(new BasicStroke(2,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));
		NURBSShape cs = c.clone();
		cs.scale(zoomfactor);
		g2.draw(cs.getCurve(5d/(double)zoomfactor));
		for (int i=0; i<c.controlPoints.size(); i++)
		{
			drawCP(g2,new Point(Math.round((float)c.controlPoints.get(i).getX()),Math.round((float)c.controlPoints.get(i).getY())),Color.cyan.darker());
		}
		Vector<Point> projectionpoints = new Vector<Point>();
		Iterator<VNode> iter = vG.modifyNodes.getIterator();
		while (iter.hasNext())
		{
			VNode actual = iter.next();
			projectionpoints.add(actual.getPosition());
		}
		//TODO: Measure again after Optimization of the Square Computation
	    long time = System.currentTimeMillis();
	    System.err.print("#"+projectionpoints.size()+" ");
	    for (int j=0; j<projectionpoints.size(); j++)
		{
			Point p = projectionpoints.get(j);
			NURBSShapeProjection proj = new NURBSShapeProjection(c,p);
			double dist = p.distance(proj.getResultPoint());
			Color cross = Color.magenta;
			if (dist<=2.0)
				cross = Color.green.darker().darker();
			drawCP(g2,new Point(Math.round((float)p.getX()),Math.round((float)p.getY())),cross);
			drawCP(g2,new Point(Math.round((float)proj.getResultPoint().getX()), Math.round((float)proj.getResultPoint().getY())), Color.ORANGE);
			g2.setColor(Color.orange);
			g2.drawLine(Math.round((float)p.getX()*zoomfactor),Math.round((float)p.getY()*zoomfactor),
					Math.round((float)proj.getResultPoint().getX()*zoomfactor), Math.round((float)proj.getResultPoint().getY()*zoomfactor));
		}
        time = -time + System.currentTimeMillis();
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        SimpleDateFormat sdf = new SimpleDateFormat("ss:SSS");
        System.err.println(" "+sdf.format(time)+" Sekunden");  
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
			if (!temp.getShape().isEmpty())
			{
				NURBSShape s = temp.getShape().clone();
				s.scale(zoomfactor);
				GeneralPath p = s.getCurve(5d/(double)zoomfactor);
				if ((((temp.getSelectedStatus()&VItem.SELECTED)==VItem.SELECTED)||((temp.getSelectedStatus()&VItem.SOFT_SELECTED)==VItem.SOFT_SELECTED))&&((temp.getSelectedStatus()&VItem.SOFT_DESELECTED)!=VItem.SOFT_DESELECTED))
				{
					//Falls die Kante Selektiert ist (und nicht temporär deselektiert, zeichne drunter eine etwas dickere Kante in der selectioncolor
					g2.setColor(selColor);
					g2.setStroke(new BasicStroke(Math.round((temp.getWidth()+selWidth)*zoomfactor),BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
					g2.draw(temp.getLinestyle().modifyPath(p,temp.getWidth()+selWidth,zoomfactor));
				}
				g2.setColor(temp.getColor());
				g2.setStroke(new BasicStroke(temp.getWidth()*zoomfactor,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));
				g2.draw(temp.getLinestyle().modifyPath(p,temp.getWidth(),zoomfactor));
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