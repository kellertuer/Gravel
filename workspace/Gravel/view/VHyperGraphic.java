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
		paintPIDEBUG(g2);
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
		Point2D HighestCP= new Point2D.Double(Double.MAX_VALUE,Double.MAX_VALUE);
		for (int i=0; i<c.controlPoints.size(); i++)
		{
			Point2D actual = c.controlPoints.get(i);
			if (actual.getY()<HighestCP.getY())
				HighestCP = (Point2D)actual.clone();	
		}
		Queue<Point2D> Points = new LinkedList<Point2D>();
		Iterator<VNode> vti = vG.modifyNodes.getIterator();
		HashMap<Integer,Integer> NodeSetIndex = new HashMap<Integer,Integer>();
		HashMap<Point2D,Integer> NodePos2Index = new HashMap<Point2D,Integer>();
		HashMap<Point2D,Double> OldRadii = new HashMap<Point2D,Double>();
		HashMap<Point2D,Integer> OldSet = new HashMap<Point2D,Integer>();
		while (vti.hasNext())
		{
			VNode n = vti.next();
			Point p = n.getPosition();
			Point2D p2 = new Point2D.Double(p.getX(),p.getY());
			Points.offer(p2);
			NodeSetIndex.put(n.getIndex(),n.getIndex()); //Ste into its own Set
			NodePos2Index.put(p2,n.getIndex());
			OldSet.put(p2,n.getIndex());
		}
		int base = vG.getMathGraph().modifyNodes.getNextIndex();
		for (int i=0; i<c.maxCPIndex-c.degree; i++)
		{
			Points.offer(c.controlPoints.get(i));
			OldSet.put(c.controlPoints.get(i), base+i+1);
		}
		//Number of Projections made, 
		int i=0;
		//MaxSize of any circle used, because a circle with this radius is much bigger than the whole graph
		int maxSize = Math.max( Math.round((float)(vG.getMaxPoint(g2).getX()-vG.getMinPoint(g2).getX())),
								Math.round((float)(vG.getMaxPoint(g2).getY()-vG.getMinPoint(g2).getY())) );
		//Check each Intervall, whether we are done
		int checkInterval = Points.size();
		boolean valid=true, running=true;
		while (!Points.isEmpty()&&running) 
		{
			Point2D actualP = Points.poll();
			NURBSShapeProjection proj = new NURBSShapeProjection(c,actualP);
			Point2D ProjP = proj.getResultPoint(); //This Point belong definetly to the same set as actualP but lies on the Curve
			double radius= ProjP.distance(actualP)-(double)e.getWidth()/2d;
			if (radius<maxSize)
			{	OldRadii.put(actualP,radius);
				g2.setColor(Color.gray);
				g2.drawOval(Math.round((float)(actualP.getX()-radius)*zoomfactor),
					Math.round((float)(actualP.getY()-radius)*zoomfactor),
					Math.round((float)(2*radius)*zoomfactor), Math.round((float)(2*radius)*zoomfactor));
				Point2D ProjDir = new Point2D.Double(ProjP.getX()-actualP.getX(),ProjP.getY()-actualP.getY());
				ProjDir = new Point2D.Double(radius/ProjP.distance(actualP)*ProjDir.getX(),radius/ProjP.distance(actualP)*ProjDir.getY());
				//Check whether other Old Points interfere with this one
				Iterator<Entry<Point2D,Double>> RadiusIterator = OldRadii.entrySet().iterator();
				while (RadiusIterator.hasNext()) //Iterate all old Points
				{
					Entry<Point2D,Double> actEntry = RadiusIterator.next();
					//If the distance of the actualPoint to this is smaller that the sum of both radii - both are in the same set
					if (actEntry.getKey().distance(actualP)<(actEntry.getValue()+radius))
					{
						int sameset = Math.min(OldSet.get(actEntry.getKey()),OldSet.get(actualP));
						int changeset = Math.max(OldSet.get(actEntry.getKey()),OldSet.get(actualP));
						if (changeset!=sameset) //not in the same set yet
						{ //Union
							Iterator<Entry<Point2D,Integer>> it = OldSet.entrySet().iterator();
							while (it.hasNext())
							{
								Entry<Point2D,Integer> checkEntry = it.next();
								if (checkEntry.getValue().intValue()==changeset)
								{
									checkEntry.setValue(sameset);
									if (NodePos2Index.containsKey(checkEntry.getKey()))
										NodeSetIndex.put(NodePos2Index.get(checkEntry.getKey()).intValue(),sameset);
								}
							}
						}	
					}
//				System.err.print("-->#"+NodeSetIndex.values().size());
				}
				//Calculate a new Point for the set (TODO: the other two new points in 90 and 270 Degree ?)
				Point2D newP = new Point2D.Double(actualP.getX()-ProjDir.getX(),actualP.getY()-ProjDir.getY());
				drawCP(g2,new Point(Math.round((float)newP.getX()/zoomfactor),Math.round((float)newP.getY()/zoomfactor)),Color.BLUE); //Draw as handled
//				if (++i<100)
//				{
					Points.offer(newP);
					OldSet.put(newP,OldSet.get(actualP)); //New value is in the same set as actualP
//				}
				i++;
				if ((i%checkInterval)==0)
				{
					int inSet=-1, outSet = OldSet.get(HighestCP).intValue();
					Iterator<Point2D> nodeiter = NodePos2Index.keySet().iterator();
					boolean twosets=true;
					while (nodeiter.hasNext())
					{
						Point2D pos = nodeiter.next();
						int id = NodePos2Index.get(pos);
						if (vG.getMathGraph().modifyHyperEdges.get(e.getIndex()).containsNode(id))
						{
							if (inSet==-1) //not set yet This nodes set must be the one for VH
								inSet = OldSet.get(pos); 
							else if (OldSet.get(pos)==outSet) //node of Hyperedge outside
							{
								valid=false; running=false; System.err.println("Node #"+id+" outside shape but in Edge");
							}
						}
						else
						{
							if ((inSet!=-1)&&(inSet==OldSet.get(pos))) //Another node not from edge is inside
							{
								valid=false; running=false; System.err.println("Node #"+id+" inside but not in Edge!");
							}
						}
						if ((inSet!=-1)&&(inSet!=OldSet.get(pos))&&(outSet!=OldSet.get(pos)))
						{
							System.err.println("Not yet only 2 sets left");
							twosets=false; //More than two sets
							break;
						}
					}
					//If we have only two sets left we're done
					running &= (!twosets);
					if (!running)
						System.err.println("#"+i+" InSet="+inSet+" OutSet="+outSet+" valid="+valid+" running="+running+" twosets="+twosets);
				}
			}		
		}
		for (int j=0; j<vG.getMathGraph().modifyNodes.getNextIndex(); j++)
		{
			if (NodeSetIndex.containsKey(j))
				System.err.print("#"+j+"->"+NodeSetIndex.get(j)+" ");
		}
		System.err.println("\n");
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