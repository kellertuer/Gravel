package view;


import history.CommonGraphHistoryManager;

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
	protected VHyperGraph vG;
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
		vGh = new CommonGraphHistoryManager(vG);
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
//	paintSubCurveDEBUG(g2);
	}
	private void paintSubCurveDEBUG(Graphics2D g2)
	{
		Vector<Point2D> IP = new Vector<Point2D>();
		IP.add(new Point2D.Double(60,180));
		IP.add(new Point2D.Double(50,110));
		IP.add(new Point2D.Double(150,40));
		IP.add(new Point2D.Double(270,60));
		IP.add(new Point2D.Double(270,100));
		IP.add(new Point2D.Double(220,110));
		IP.add(new Point2D.Double(200,180));
		IP.add(new Point2D.Double(200,210));
		IP.add(new Point2D.Double(270,210));
		IP.add(new Point2D.Double(230,270));
		IP.add(new Point2D.Double(200,260));
		IP.add(new Point2D.Double(175,270));
		IP.add(new Point2D.Double(185,300));
		IP.add(new Point2D.Double(80,330));
		int degree = 4;
		NURBSCreationMessage nm = new NURBSCreationMessage(degree, NURBSCreationMessage.ADD_END, IP);
		NURBSShape c = NURBSShapeFactory.CreateShape(nm);
		
		g2.setStroke(new BasicStroke(1.2f,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));
		NURBSShape cs = c.stripDecorations().clone();
		cs.scale(zoomfactor);
		g2.setStroke(new BasicStroke(1.2f,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));
		g2.setColor(Color.black);
		g2.draw(cs.getCurve(5d/(double)zoomfactor));
		if ((vG.modifyNodes.get(1)==null)||(vG.modifyNodes.get(2)==null))
			return;
		Point2D p1 = new Point2D.Double(vG.modifyNodes.get(1).getPosition().x,vG.modifyNodes.get(1).getPosition().y);
		Point2D p2 = new Point2D.Double(vG.modifyNodes.get(2).getPosition().x,vG.modifyNodes.get(2).getPosition().y);
		NURBSShapeProjection proj1 = new NURBSShapeProjection(c,p1);
		NURBSShapeProjection proj2 = new NURBSShapeProjection(c,p2);
		double u1 = proj1.getResultParameter(), u2 = proj2.getResultParameter();

		NURBSShape subc = (new NURBSShapeFragment(c,u1, u2)).getSubCurve();
		System.err.print("SubCurve ["+u1+","+u2+"] ");
		if (u2<u1)
			System.err.println("And running over Start/End");
		else
			System.err.println(" ");
//		subc.translate(p1.getX()-pp1.getX(), p1.getY()-pp1.getY());
		g2.setStroke(new BasicStroke(selWidth,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));
		NURBSShape subcs = subc.stripDecorations().clone();
		subcs.scale(zoomfactor);
		g2.setColor(selColor);
		g2.draw(subcs.getCurve(5d/(double)zoomfactor));	
		
		//Complete Curve after Subcurve - again
		g2.setStroke(new BasicStroke(1.2f,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));
		g2.setColor(Color.black);
		g2.draw(cs.getCurve(5d/(double)zoomfactor));
		drawCP(g2,new Point(Math.round((float)c.CurveAt(c.Knots.get(degree)).getX()),Math.round((float)c.CurveAt(c.Knots.get(degree)).getY())),Color.green.brighter());
		Vector<Point> projectionpoints = new Vector<Point>();
		Iterator<VNode> iter = vG.modifyNodes.getIterator();
		while (iter.hasNext())
		{
			VNode actual = iter.next();
			projectionpoints.add(actual.getPosition());
		}
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
		NURBSCreationMessage nm = new NURBSCreationMessage(degree, NURBSCreationMessage.ADD_END, IP);
		NURBSShape c = NURBSShapeFactory.CreateShape(nm);
		
		g2.setStroke(new BasicStroke(1.2f,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));
		NURBSShape cs = c.stripDecorations().clone();
		cs.scale(zoomfactor);
		g2.setColor(Color.black);
		g2.draw(cs.getCurve(5d/(double)zoomfactor));
		//
		// Start of Validation-Algorithm
		//
		Iterator<VNode> nid = vG.modifyNodes.getIterator();
		while (nid.hasNext())
		{
			VNode n = nid.next();
			Point2D p = new Point2D.Double(n.getPosition().getX(),n.getPosition().getY());
			NURBSShapeValidator.findSuccessor(p, null, c,g2,zoomfactor);
		}
	}
	private void paintDerivDEBUG(Graphics2D g2)
	{
		Vector<Double> knots = new Vector<Double>();
		for (int i=0; i<=18; i++)
			knots.add(((double)i-3d)/6d);
		
		Vector<Point2D> points = new Vector<Point2D>();
		Vector<Double> weights = new Vector<Double>();
		points.add(new Point2D.Double(60d,65d)); weights.add(1d);
		points.add(new Point2D.Double(80d,105d)); weights.add(1d);
		points.add(new Point2D.Double(100d,235d)); weights.add(1d);
		points.add(new Point2D.Double(190d,335d)); weights.add(1d);
		points.add(new Point2D.Double(200d,335d)); weights.add(1d);
		points.add(new Point2D.Double(330d,135d)); weights.add(.8d);
		points.add(new Point2D.Double(430d,235d)); weights.add(1d);
		points.add(new Point2D.Double(530d,135d)); weights.add(1d);
		points.add(new Point2D.Double(530d,160d)); weights.add(1d);
		points.add(new Point2D.Double(580d,125d)); weights.add(.6d);
		points.add(new Point2D.Double(430d,110d)); weights.add(1d);
		points.add(new Point2D.Double(230d,155d));weights.add(1d);
		for (int i=0; i<3; i++)
		{
			points.add((Point2D)points.get(i).clone());
			weights.add(weights.get(i).doubleValue());
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
			if (i>0)
			{
				g2.drawLine(Math.round((float)c.controlPoints.get(i).getX()*zoomfactor),Math.round((float)c.controlPoints.get(i).getY()*zoomfactor),
						Math.round((float)c.controlPoints.get(i-1).getX()*zoomfactor),Math.round((float)c.controlPoints.get(i-1).getY()*zoomfactor));
			}
		}
		for (int i=0; i<1000; i++)
		{
			double pos = c.Knots.get(c.degree) + (c.Knots.get(c.degree)+c.Knots.get(c.maxKnotIndex-c.degree))*((double) i)/1000d;
			Point2D p = c.CurveAt(pos);
//			drawCP(g2,new Point(Math.round((float)p.getX()),Math.round((float)p.getY())),Color.LIGHT_GRAY);
			Point2D ps = c.DerivateCurveAt(1,pos);
			g2.setColor(Color.orange.brighter());
			Point2D normps = new Point2D.Double(ps.getX()/ps.distance(0d,0d),ps.getY()/ps.distance(0d,0d));
			Point2D deriv2 = c.DerivateCurveAt(2,pos);
			double length = deriv2.distance(0d,0d)/80;
			length = Math.abs(length);
			
			//Now orthogonal to the first derivate the seconds derivates length
			g2.drawLine(Math.round((float)p.getX()*zoomfactor), Math.round((float)p.getY()*zoomfactor),
					Math.round((float)(p.getX()-normps.getY()*length)*zoomfactor),
					Math.round((float)(p.getY()+normps.getX()*length)*zoomfactor));
		}
	}
	private void paintPIDEBUG(Graphics2D g2)
	{
		Vector<Double> knots = new Vector<Double>();
		for (int i=0; i<=22; i++)
			knots.add(((double)i-3d)/6d);
		
		Vector<Point2D> points = new Vector<Point2D>();
		Vector<Double> weights = new Vector<Double>();
		points.add(new Point2D.Double(30d,5d)); weights.add(.8);
		points.add(new Point2D.Double(50d,45d)); weights.add(.7);
		points.add(new Point2D.Double(150d,275d)); weights.add(1d);
		points.add(new Point2D.Double(160d,275d)); weights.add(1d);
		points.add(new Point2D.Double(170d,275d)); weights.add(1d);
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
				NURBSShape s = temp.getShape().stripDecorations().clone();
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