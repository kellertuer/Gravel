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

import java.text.SimpleDateFormat;
import java.util.TimeZone;

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
		paintPointInversionDEBUG(g2);
	}
	private void paintDEBUG(Graphics2D g2)
	{
		Vector<Point2D> IP = new Vector<Point2D>();
		IP.add(new Point2D.Double(150,60));
		IP.add(new Point2D.Double(300,100));
		IP.add(new Point2D.Double(170,120));
		IP.add(new Point2D.Double(120,150));
		IP.add(new Point2D.Double(90,230));
		IP.add(new Point2D.Double(120,230));
		IP.add(new Point2D.Double(70,260));
		IP.add(new Point2D.Double(75,260));
		IP.add(new Point2D.Double(80,260));
		IP.add(new Point2D.Double(90,260));
		IP.add(new Point2D.Double(70,290));
		IP.add(new Point2D.Double(50,130));
		IP.add((Point2D) IP.firstElement().clone());
		NURBSShape c = NURBSShapeFactory.CreateInterpolation(IP,3);
		g2.setStroke(new BasicStroke(1.2f,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));
		NURBSShape cs = c.clone();
		cs.scale(zoomfactor);
		g2.draw(cs.getCurve(5d/(double)zoomfactor));
		for (int i=0; i<IP.size(); i++)
		{
			drawCP(g2,new Point(Math.round((float)IP.get(i).getX()),Math.round((float)IP.get(i).getY())),Color.cyan.darker());
		}
		for (int i=0; i<c.controlPoints.size(); i++) //c.controlPoints.size(); i++)
		{
			drawCP(g2,new Point(Math.round((float)c.controlPoints.get(i).getX()),Math.round((float)c.controlPoints.get(i).getY())),Color.red.brighter().brighter());
		}
		g2.setColor(Color.blue.brighter().brighter());
		Vector<Point2D> IP2 = new Vector<Point2D>();
		for (int i=0; i<=4; i++)
		{
			IP2.add((Point2D)IP.get(IP.size()-6+i)); //So we don't copy the double endpoint
		}
		for (int i=0; i<=4; i++)
		{
			IP2.add((Point2D)IP.get(i));
		}
		drawCP(g2,new Point(Math.round((float)IP2.get(0).getX()),Math.round((float)IP2.get(0).getY())),Color.green);
		NURBSShape c2 = NURBSShapeFactory.CreateInterpolation(IP2,3);
		g2.setStroke(new BasicStroke(.9f,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));
		NURBSShape cs2 = c2.clone();
//		cs2.translate(10,0);
		cs2.scale(zoomfactor);
		g2.setColor(Color.darkGray);
		g2.draw(cs2.getCurve(5d/(double)zoomfactor));
		Color co = Color.green.darker().darker();
		for (int i=0; i<c2.controlPoints.size(); i++)
		{
			drawCP(g2,new Point(Math.round((float)c2.controlPoints.get(i).getX()),Math.round((float)c2.controlPoints.get(i).getY())),co);
			co = co.brighter();
		}
		for (int j=c.degree-1; j>=0; j--) //from first that should less than zero to the lowest value
		{
			double lastval = c.Knots.get(j+1);
			double correspInterval = c.Knots.get(c.maxKnotIndex-c.degree - (c.degree-1-j)) - c.Knots.get(c.maxKnotIndex-c.degree - (c.degree-1-j) -1);
			c.Knots.set(j, lastval - correspInterval);
		}
		//last degree values following One
			for (int j=0; j<c.degree; j++) //from first that should less than zero to the lowest value
		{
			double lastval = c.Knots.get(c.maxKnotIndex-c.degree+j);
			double correspInterval = c.Knots.get(c.degree+j+1) - c.Knots.get(c.degree+j);
			c.Knots.set(c.maxKnotIndex-c.degree+j+1,lastval + correspInterval);
		}
		for (int i=0; i<3; i++)
		{
			c.controlPoints.set(i, (Point2D)c2.controlPoints.get(i+4).clone());
			c.controlPoints.set(c.controlPoints.size()-1-2+i,(Point2D)c2.controlPoints.get(i+4));
		}
		System.err.println(c2.controlPoints.size());
		cs = c.clone();
		cs.scale(zoomfactor);
		g2.setColor(Color.blue);
		g2.draw(cs.getCurve(5d/(double)zoomfactor));
	}
	private void paintPointInversionDEBUG(Graphics2D g2)
	{
		Vector<Double> knots = new Vector<Double>();
		for (int i=0; i<=16; i++)
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
		for (int i=0; i<3; i++)
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