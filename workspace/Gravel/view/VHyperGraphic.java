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
//		paintDEBUG(g2);
//		paintSubCurveIP(g2);
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
		NURBSShape drawSel = c.stripDecorations().clone(); //really only nurbs
		drawSel.scale(zoomfactor);
		g2.setColor(selColor);
		g2.setStroke(new BasicStroke(1f*zoomfactor,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));
		g2.draw(drawSel.getCurve(5d/(double)zoomfactor)); //draw only a preview				
	}
	private void paintSubCurveIP(Graphics2D g2)
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

		NURBSShape cs = c.stripDecorations().clone();
		cs.scale(zoomfactor);
		
		if ((vG.modifyNodes.get(2)==null)||(vG.modifyNodes.get(1)==null))
		{
			g2.setStroke(new BasicStroke(1.3f,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));
			g2.setColor(Color.black);
			g2.draw(cs.getCurve(5d/(double)zoomfactor));
			return;
		}

		double u1 = (new NURBSShapeProjection(c.clone(), new Point2D.Double(vG.modifyNodes.get(1).getPosition().getX(),vG.modifyNodes.get(1).getPosition().getY()))).getResultParameter();
		double u2 = (new NURBSShapeProjection(c.clone(), new Point2D.Double(vG.modifyNodes.get(2).getPosition().getX(),vG.modifyNodes.get(2).getPosition().getY()))).getResultParameter();
		drawCP(g2,new Point(Math.round((float)c.CurveAt(u1).getX()), Math.round((float)c.CurveAt(u1).getY())), Color.ORANGE);
		drawCP(g2,new Point(Math.round((float)c.CurveAt(u2).getX()), Math.round((float)c.CurveAt(u2).getY())), Color.ORANGE);

		System.err.println(u1+" DEBUG "+u2);
		
		NURBSShapeFragment s = new NURBSShapeFragment(c.clone(),u1,u2); //Refine the selected half
		float selSize = (float)selWidth/2f + (float) 1;
		NURBSShape drawSel = s.getSubCurve().stripDecorations().clone(); //really only nurbs
		drawSel.scale(zoomfactor);
		g2.setColor(selColor);
		g2.setStroke(new BasicStroke(selSize*zoomfactor,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));
		g2.draw(drawSel.getCurve(5d/(double)zoomfactor)); //draw only a preview				

		Iterator<VNode> iter = vG.modifyNodes.getIterator();
		Vector<Point2D> q = new Vector<Point2D>();
		//handle Nodes 3,... as IPfor subcurve replacement
		while (iter.hasNext())
		{
			VNode actual = iter.next();
			if ((actual.getIndex()!=2)&&(actual.getIndex()!=1))
				q.add(new Point2D.Double(actual.getPosition().x, actual.getPosition().y));
		}

		g2.setStroke(new BasicStroke(1.3f,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));
		g2.setColor(Color.black);
		g2.draw(cs.getCurve(5d/(double)zoomfactor));

		if (q.size()==0)
			return;

		//On Top draw new Curve
		nm = new NURBSCreationMessage(s,NURBSCreationMessage.ADD_END,q);
		NURBSShape c2 = NURBSShapeFactory.CreateShape(nm);
		if (c2.isEmpty())
			return;
		NURBSShape cs2 = c2.stripDecorations().clone();
		cs2.scale(zoomfactor);
		g2.setStroke(new BasicStroke(1.3f,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));
		g2.setColor(Color.magenta);
		g2.draw(cs2.getCurve(5d/(double)zoomfactor));
		drawCP(g2,new Point(Math.round((float)c2.CurveAt(c2.Knots.get(c2.getDegree())).getX()),Math.round((float)c2.CurveAt(c2.Knots.get(c2.getDegree())).getY())),Color.GREEN);
		drawCP(g2,new Point(Math.round((float)c.CurveAt(c.Knots.get(c.getDegree())).getX()),Math.round((float)c.CurveAt(c.Knots.get(c.getDegree())).getY())),Color.RED);
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
//		c = NURBSShapeFactory.CreateShape(new NURBSCreationMessage(2, new Point2D.Double(200d,200d),150));
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
		if (vG.modifyNodes.get(1)==null)
		{
			for (int i=0; i<1000; i++)
			{
				DerivateHelper(c, c.Knots.get(c.getDegree()) + (c.Knots.get(c.getDegree())+c.Knots.get(c.maxKnotIndex-c.getDegree()))*((double) i)/1000d, g2);
			}
		}
		else
		{
			Iterator<VNode> iter = vG.modifyNodes.getIterator();
			while (iter.hasNext())
			{
				VNode actual = iter.next();
				Point2D p = new Point2D.Double(actual.getPosition().x,actual.getPosition().y);
				NURBSShapeProjection proj = new NURBSShapeProjection(c,p);
				DerivateHelper(c,proj.getResultParameter(), g2);
			}
		}
	}
	private void DerivateHelper(NURBSShape c, double pos, Graphics2D g2)
	{
		Point2D p = c.CurveAt(pos);
//		drawCP(g2,new Point(Math.round((float)p.getX()),Math.round((float)p.getY())),Color.LIGHT_GRAY);
		Point2D deriv1 = c.DerivateCurveAt(1,pos);
		g2.setColor(Color.orange.brighter());
		Point2D deriv2 = c.DerivateCurveAt(2,pos);

		double l = deriv1.getX()*deriv1.getX() + deriv1.getY()*deriv1.getY();
		double curvature = (deriv1.getX()*deriv2.getY() - deriv2.getX()*deriv1.getY())/ Math.sqrt(l*l*l);
		curvature = curvature*c.WeightAt(pos);
		curvature = Math.abs(curvature)*1000;
		System.err.println(curvature);
		Point2D normps = new Point2D.Double(deriv1.getX()/deriv1.distance(0d,0d),deriv1.getY()/deriv1.distance(0d,0d));

		//Now orthogonal to the first derivate the seconds derivates length
		g2.drawLine(Math.round((float)p.getX()*zoomfactor), Math.round((float)p.getY()*zoomfactor),
				Math.round((float)(p.getX()-normps.getY()*curvature)*zoomfactor),
				Math.round((float)(p.getY()+normps.getX()*curvature)*zoomfactor));

	}
	private void paintPIDEBUG(Graphics2D g2)
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
		IP.add(new Point2D.Double(400,200));
		IP.add(new Point2D.Double(400,300));
		IP.add(new Point2D.Double(230,290));
		IP.add(new Point2D.Double(200,280));
		IP.add(new Point2D.Double(175,290));
		IP.add(new Point2D.Double(185,320));
		IP.add(new Point2D.Double(80,350));
		int degree = 6;
		NURBSCreationMessage nm = new NURBSCreationMessage(degree, NURBSCreationMessage.ADD_END, IP);
		NURBSShape c = NURBSShapeFactory.CreateShape(nm);

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
	    System.err.print("#"+projectionpoints.size()+" ");
	    long time = System.currentTimeMillis();
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