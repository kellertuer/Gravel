package view;


import history.GraphHistoryManager;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
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
		//DEBUG
		g2.setColor(Color.black);
		g2.setStroke(new BasicStroke(1*zoomfactor,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));
		Vector<Point2D> P = new Vector<Point2D>();
		Vector<Double> weights = new Vector<Double>();
		P.add(new Point2D.Double(10,100)); weights.add(1d);
		P.add(new Point2D.Double(70,90)); weights.add(1d);
		P.add(new Point2D.Double(25,30)); weights.add(1d);
		P.add(new Point2D.Double(200,10)); weights.add(3d);
		P.add(new Point2D.Double(100,85)); weights.add(1d);
		P.add(new Point2D.Double(300,100)); weights.add(1d);
		P.add(new Point2D.Double(350,100)); weights.add(1d);
		Vector<Double> U = new Vector<Double>();
		U.add(0d);U.add(0d);U.add(0d);U.add(0d);
		U.add(.25d);U.add(.5d); U.add(.75d);
		U.add(1d);U.add(1d);U.add(1d);U.add(1d);
		VHyperEdgeShape s = new VHyperEdgeShape(U,P,weights,27);
		Vector<Double> X = new Vector<Double>();		
		X.add(.125d); 
		X.add(.375d);
		X.add(.625d);
		X.add(.875d);		
		s.RefineKnots(X);
		X.clear();
		X.add(.0625d);
		X.add(.1875d);
		X.add(.4375d);
		X.add(.5625d);
		X.add(.6875d);
		X.add(.8125d);
		X.add(.9375d);
		s.RefineKnots(X);
		g2.setColor(Color.BLACK);
		s.scale(zoomfactor);
		GeneralPath path = s.getCurve(.002d);
		g2.draw(path);
		Iterator<Point2D> pi = s.controlPoints.iterator();
		while (pi.hasNext())
		{
			Point2D p = (Point2D) pi.next();
			g2.setColor(Color.BLACK);
			g2.setStroke(new BasicStroke(1,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));

			g2.drawLine(Math.round(((float)p.getX()-3)),Math.round((float)p.getY()),Math.round(((float)p.getX()+3)),Math.round((float)p.getY()));
			g2.drawLine(Math.round(((float)p.getX())),Math.round(((float)p.getY()-3)),Math.round((float)p.getX()),Math.round(((float)p.getY()+3)));
		}
		s.scale(1/zoomfactor);
		int i=87;
		Point2D PCurve = s.NURBSCurveAt(((double)i/100));
		Point2D PDest = new Point2D.Double(PCurve.getX(),PCurve.getY()+50d);
		if (vG.modifyNodes.get(1)!=null)
			PDest = (Point2D) vG.modifyNodes.get(1).getPosition().clone();
		g2.setColor(Color.RED);		
		s.movePoint(PCurve, PDest);
		PCurve.setLocation(PCurve.getX()*zoomfactor, PCurve.getY()*zoomfactor);
		PDest.setLocation(PDest.getX()*zoomfactor, PDest.getY()*zoomfactor);
		g2.drawLine(Math.round(((float)PCurve.getX()-3)),Math.round((float)PCurve.getY()),Math.round(((float)PCurve.getX()+3)),Math.round((float)PCurve.getY()));
		g2.drawLine(Math.round(((float)PCurve.getX())),Math.round(((float)PCurve.getY()-3)),Math.round((float)PCurve.getX()),Math.round(((float)PCurve.getY()+3)));
		g2.drawLine(Math.round(((float)PDest.getX()-3)),Math.round((float)PDest.getY()),Math.round(((float)PDest.getX()+3)),Math.round((float)PDest.getY()));
		g2.drawLine(Math.round(((float)PDest.getX())),Math.round(((float)PDest.getY()-3)),Math.round((float)PDest.getX()),Math.round(((float)PDest.getY()+3)));
		g2.drawLine(Math.round(((float)PDest.getX())),Math.round(((float)PDest.getY())),Math.round((float)PCurve.getX()),Math.round(((float)PCurve.getY())));
		s.scale(zoomfactor);
		path = s.getCurve(.002d);
		g2.setColor(Color.BLUE);
		g2.draw(path);
		pi = s.controlPoints.iterator();
		while (pi.hasNext())
		{
			Point2D p = (Point2D) pi.next();
			g2.setColor(Color.BLUE);
			g2.setStroke(new BasicStroke(1,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));

			g2.drawLine(Math.round(((float)p.getX()-3)),Math.round((float)p.getY()),Math.round(((float)p.getX()+3)),Math.round((float)p.getY()));
			g2.drawLine(Math.round(((float)p.getX())),Math.round(((float)p.getY()-3)),Math.round((float)p.getX()),Math.round(((float)p.getY()+3)));
		}
		s.scale(1/zoomfactor);
		Iterator<VNode> ni = vG.modifyNodes.getIterator();
		while (ni.hasNext())
		{
			VNode actual = ni.next();
			if (actual.getIndex()>1)
			{
				Point2D p = s.ProjectionPoint((Point2D) actual.getPosition().clone());
				g2.setColor(Color.GRAY);
				g2.drawLine(
						Math.round(actual.getPosition().x*zoomfactor),
						Math.round(actual.getPosition().y*zoomfactor),
						Math.round((float)p.getX()*zoomfactor),
						Math.round((float)p.getY()*zoomfactor));
				if (p.distance(actual.getPosition())<=2.0d)
					g2.setColor(Color.GREEN);
				else
					g2.setColor(Color.RED);					
				g2.setStroke(new BasicStroke(1,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));

				g2.drawLine(Math.round(((float)actual.getPosition().getX()*zoomfactor-3)),Math.round((float)actual.getPosition().getY()*zoomfactor),Math.round(((float)actual.getPosition().getX()*zoomfactor+3)),Math.round((float)actual.getPosition().getY()*zoomfactor));
				g2.drawLine(Math.round(((float)actual.getPosition().getX()*zoomfactor)),Math.round(((float)actual.getPosition().getY()*zoomfactor-3)),Math.round((float)actual.getPosition().getX()*zoomfactor),Math.round(((float)actual.getPosition().getY()*zoomfactor+3)));

			}
		}
		Vector<Object> param = new Vector<Object>();
		param.setSize(NURBSShapeFactory.MAX_INDEX);
		param.add(NURBSShapeFactory.CIRCLE_ORIGIN, new Point(Math.round((float)(200*zoomfactor)),Math.round((float)(200*zoomfactor))));
		param.add(NURBSShapeFactory.CIRCLE_RADIUS, Math.round((float)(180*zoomfactor)));
		param.add(NURBSShapeFactory.DISTANCE_TO_NODE,20);
		VHyperEdgeShape circle = NURBSShapeFactory.CreateShape("Circle",param);
		g2.setColor(Color.CYAN);
		g2.setStroke(new BasicStroke(1*zoomfactor,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));
		path = circle.getCurve(.002d);
		g2.draw(path);
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
			VHyperEdge temp = ei.next(); //Grafischer Teil
			//TODO Draw HyperEdge
		}
		//
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