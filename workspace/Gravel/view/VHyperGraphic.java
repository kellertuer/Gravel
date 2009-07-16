package view;


import history.CommonGraphHistoryManager;
import io.DiplExports;

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
//		paintDerivDEBUG(g2);
		paintDEBUG(g2);
	}
	private void paintDEBUG(Graphics2D g2)
	{
		Point offset = new Point(17,18);
		Point max = new Point(183,122);
		if ((vG.modifyHyperEdges.get(1)!=null)&&(vG.modifyHyperEdges.get(1).getShape()!=null)&&(!vG.modifyHyperEdges.get(1).getShape().isEmpty()))
		{
			VEdgeLinestyle solid = new VEdgeLinestyle(VEdgeLinestyle.SOLID,0,0);
			VEdgeLinestyle dashed = new VEdgeLinestyle(VEdgeLinestyle.DASHED,5,4);
			NURBSShape c = vG.modifyHyperEdges.get(1).getShape().clone();
			NURBSShape cs = c.clone();
			cs.scale(zoomfactor);
			g2.setColor(Color.white);
			g2.setStroke(new BasicStroke(vG.modifyHyperEdges.get(1).getWidth()*2*zoomfactor,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));
			g2.draw(cs.getCurve(5d/(double)zoomfactor));
//			c.translate(0d,40d);
			g2.setColor(Color.BLACK);
			g2.setStroke(new BasicStroke(vG.modifyHyperEdges.get(1).getWidth()*zoomfactor,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));
			GeneralPath solidpath = solid.modifyPath(cs.getCurve(5d/(double)zoomfactor), 1, zoomfactor);
			GeneralPath CPPath = new GeneralPath();
			CPPath.moveTo((new Double(cs.controlPoints.get(0).getX())).floatValue(),(new Double(cs.controlPoints.get(0).getY())).floatValue());
			CPPath.lineTo((new Double(cs.controlPoints.get(0).getX())).floatValue(),(new Double(cs.controlPoints.get(0).getY())).floatValue());
			for (int j=1; j<cs.controlPoints.size(); j++)
			{
				CPPath.lineTo((new Double(cs.controlPoints.get(j).getX())).floatValue(),(new Double(cs.controlPoints.get(j).getY())).floatValue());					
				//Force Circle
				CPPath.lineTo((new Double(cs.controlPoints.get(j).getX())).floatValue(),(new Double(cs.controlPoints.get(j).getY())).floatValue());					
			}
			GeneralPath solidCPpath = solid.modifyPath(CPPath, 1, zoomfactor);
			g2.setColor(Color.BLACK);
			g2.draw(solidpath);g2.draw(solidCPpath);
			System.err.println(
					DiplExports.drawOnePath(solidpath.getPathIterator(null), 1,0d, offset, max,false));
			System.err.println(
					DiplExports.drawOnePath(solidCPpath.getPathIterator(null), 1,0d, offset, max,true));
			System.err.println("%  KnotPoints");
			for (int i=cs.degree; i<= cs.maxKnotIndex-cs.degree; i++)
			{
				Point2D p = cs.CurveAt(cs.Knots.get(i));
				System.err.print(DiplExports.drawQuad(p.getX()-(double)offset.x,(double)max.y-p.getY(), 1.5));
				System.err.println("At ("+(p.getX()-(double)offset.x)+","+((double)max.y-p.getY())+")");
			}
		}
	}

	private void paintBezierDEBUG(Graphics2D g2)
	{
		if ((vG.modifyHyperEdges.get(1)!=null)&&(vG.modifyHyperEdges.get(1).getShape()!=null)&&(!vG.modifyHyperEdges.get(1).getShape().isEmpty()))
		{
			VEdgeLinestyle solid = new VEdgeLinestyle(VEdgeLinestyle.SOLID,0,0);
			VEdgeLinestyle dashed = new VEdgeLinestyle(VEdgeLinestyle.DASHED,5,4);
			NURBSShape c = vG.modifyHyperEdges.get(1).getShape().clone();
			NURBSShape cs = c.clone();
			cs.scale(zoomfactor);
			g2.setColor(Color.white);
			g2.setStroke(new BasicStroke(vG.modifyHyperEdges.get(1).getWidth()*2*zoomfactor,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));
			g2.draw(cs.getCurve(5d/(double)zoomfactor));
//			c.translate(0d,40d);
			g2.setColor(Color.BLACK);
			g2.setStroke(new BasicStroke(vG.modifyHyperEdges.get(1).getWidth()*zoomfactor,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));
			Vector<NURBSShape> bezier = NURBSShapeProjection.DecomposeCurve(c);
			for (int i=0; i<bezier.size();i++)
			{
				cs = bezier.get(i).clone();
				cs.scale(zoomfactor);
				GeneralPath solidpath = solid.modifyPath(cs.getCurve(5d/(double)zoomfactor), 1, zoomfactor);
				GeneralPath dashedpath = dashed.modifyPath(cs.getCurve(5d/(double)zoomfactor), 1, zoomfactor);
				GeneralPath CPPath = new GeneralPath();
				CPPath.moveTo((new Double(cs.controlPoints.get(0).getX())).floatValue(),(new Double(cs.controlPoints.get(0).getY())).floatValue());
				if (i==1)
					CPPath.lineTo((new Double(cs.controlPoints.get(0).getX())).floatValue(),(new Double(cs.controlPoints.get(0).getY())).floatValue());
				for (int j=1; j<cs.controlPoints.size(); j++)
				{
					CPPath.lineTo((new Double(cs.controlPoints.get(j).getX())).floatValue(),(new Double(cs.controlPoints.get(j).getY())).floatValue());					
					//Force Circle
					CPPath.lineTo((new Double(cs.controlPoints.get(j).getX())).floatValue(),(new Double(cs.controlPoints.get(j).getY())).floatValue());					
				}
				GeneralPath solidCPpath = solid.modifyPath(CPPath, 1, zoomfactor);
				GeneralPath dashedCPpath = dashed.modifyPath(CPPath, 1, zoomfactor);
				if ((i%2)==0)
				{
					g2.setColor(Color.BLACK);
					g2.draw(solidpath);g2.draw(solidCPpath);
					System.err.println(
							DiplExports.drawOnePath(solidpath.getPathIterator(null), 1,0d, new Point(17,18), new Point(183,122),false));
					System.err.println(
							DiplExports.drawOnePath(solidCPpath.getPathIterator(null), 1,0d, new Point(17,18), new Point(183,122),true));
				}
				else
				{
					g2.setColor(new Color(204,204,235));
					g2.draw(solidpath);g2.draw(solidCPpath);
					System.err.println(
							DiplExports.drawOnePath(solidpath.getPathIterator(null), 1,0d, new Point(17,18), new Point(183,122),false));
					System.err.println(
							DiplExports.drawOnePath(solidCPpath.getPathIterator(null), 1,0d, new Point(17,18), new Point(183,122),true));
					g2.setColor(Color.BLACK);
					System.err.println("\\color{maincolorlight}");
					g2.draw(dashedpath);g2.draw(dashedCPpath);
					System.err.println(
							DiplExports.drawOnePath(dashedpath.getPathIterator(null), 1,0d, new Point(17,18), new Point(183,122),false));
					System.err.println(
							DiplExports.drawOnePath(dashedCPpath.getPathIterator(null), 1,0d, new Point(17,18), new Point(183,122),true));
					System.err.println("\\color{black}");
				}
			}
		}
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
		if (vG.modifyHyperEdges.get(1)!=null)
			c = vG.modifyHyperEdges.get(1).getShape();
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
		int Num=5;
		for (int i=0; i<Num; i++)
		{
			double pos = c.Knots.get(c.getDegree()) + (c.Knots.get(c.maxKnotIndex-c.getDegree())-c.Knots.get(c.getDegree()))*((double) i)/((double)Num);
			if (i==0)
				pos += 0.000001d;
			DerivateHelper(c, pos, g2);
		}
	}
	private void DerivateHelper(NURBSShape c, double pos, Graphics2D g2)
	{
		System.err.print("Eval at "+pos+" (w:"+c.WeightAt(pos)+")");
		Vector<Point2D> derivs = c.DerivateCurveValuesAt(2,pos);
		Point2D p = derivs.get(0);
//		drawCP(g2,new Point(Math.round((float)p.getX()),Math.round((float)p.getY())),Color.LIGHT_GRAY);
		Point2D deriv1 = derivs.get(1);
		Point2D deriv2 = derivs.get(2);
		System.err.println("Deriv1 is "+deriv1+" and Deriv2 is "+deriv2);
		
		double l = deriv1.distance(0d,0d)/100d;
		Point2D normps = new Point2D.Double(deriv1.getX()/deriv1.distance(0d,0d),deriv1.getY()/deriv1.distance(0d,0d));
		double l2 = deriv2.distance(0d,0d)/100d;
		Point2D normps2 = new Point2D.Double(deriv2.getX()/deriv2.distance(0d,0d),deriv2.getY()/deriv2.distance(0d,0d));

		//Now orthogonal to the first derivate the seconds derivates length
		g2.setColor(Color.orange.brighter());
		g2.drawLine(Math.round((float)p.getX()*zoomfactor), Math.round((float)p.getY()*zoomfactor),
				Math.round((float)(p.getX()+normps.getX()*20*l)*zoomfactor),
				Math.round((float)(p.getY()+normps.getY()*20*l)*zoomfactor));
		g2.setColor(Color.red.darker().brighter());
		g2.drawLine(Math.round((float)p.getX()*zoomfactor), Math.round((float)p.getY()*zoomfactor),
				Math.round((float)(p.getX()+normps2.getX()*20*l2)*zoomfactor),
				Math.round((float)(p.getY()+normps2.getY()*20*l2)*zoomfactor));

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
		if (c.isEmpty())
			return;
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
			NURBSShapeProjection proj;
			if (j==0)
				proj = new NURBSShapeProjection(c,p,this,zoomfactor);
			else
				proj = new NURBSShapeProjection(c,p);
			double dist = p.distance(proj.getResultPoint());
//			System.err.println("Node #"+(j+1)+" Projected onto Parameter "+proj.getResultParameter()+" in ["+c.Knots.get(c.degree)+""+c.Knots.get(c.maxKnotIndex-c.degree)+"]");
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
			MHyperEdge tempm = vG.getMathGraph().modifyHyperEdges.get(temp.getIndex());
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
				if (temp.getTextProperties().isVisible()) //Visible
				{
					Point m = temp.getTextCenter();
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