package view;

import history.GraphHistoryManager;
import io.GeneralPreferences;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
//import javax.swing.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Observable;
import java.util.Vector;
import javax.swing.JViewport;

import control.ClickMouseHandler;
import control.DragMouseHandler;

import control.OCMClickMouseHandler;
import control.OCMDragMouseHandler;
import control.StandardClickMouseHandler;
import control.StandardDragMouseHandler;

import model.GraphMessage;
import model.MGraph;
import model.VEdge;
import model.VGraph;
import model.VItem;
import model.VNode;

import view.pieces.GridComponent;
import view.pieces.ZoomComponent;

import java.util.Observer;

/**
 * class VGraphic
 * - Implementierung der Darstellung eines Graphen in einer Graphics2D Umgebung
 * 
 * @author Ronny Bergmann
 *
 */
public class VGraphic extends Component implements 	Observer
{
	public static final int NO_MOUSEHANDLING=0;
	public static final int STD_MOUSEHANDLING=1;
	public static final int OCM_MOUSEHANDLING=2;
	
	// VGraph : Die Daten des Graphen
	private VGraph vG;
	private GraphHistoryManager vGh;
	
	private JViewport vp;
	
	private float zoomfactor;
	private int gridx,gridy;
	private boolean gridenabled,gridorientated, usezoom;
	GeneralPreferences gp;
	
	//Eine Liste der GUI-Elemente, die zur direkten Einstellung dienen (also ausser GeneralPreferences)
	//etwa Zoom, Grid,...
	
	private HashMap<String,Observable> Controls;
	
	// Visual Styles
	private BasicStroke vEdgeStyle;
	private Color selColor; //Color of selected Elements
	private int selWidth; //Width of selection border
	private DragMouseHandler Drag;
	private ClickMouseHandler Click;
	private static final long serialVersionUID = 1L;
	
	/**
	 * Create a New Graphical representation of an VGraph with a given size
	 * @param d Size of the Area the VGraphics gets
	 * @param Graph Graph to be represented
	 */
	public VGraphic(Dimension d,VGraph Graph) {
		//GeneralPreferences als beobachter eintragen
		gp = GeneralPreferences.getInstance();
		gp.addObserver(this);
		this.setSize(d);
		this.setBounds(0, 0, d.width, d.height);
		
		//zoomfactor = (float)gp.getIntValue("vgraphic.zoom") / 100;
		this.setZoomEnabled(true);
		gridx = gp.getIntValue("grid.x");
		gridy = gp.getIntValue("grid.y");
		gridenabled = gp.getBoolValue("grid.enabled");
		gridorientated = gp.getBoolValue("grid.orientated");
		
		vEdgeStyle = new BasicStroke(5.0f, BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND);
		selColor = new Color(gp.getIntValue("vgraphic.selcolr"),gp.getIntValue("vgraphic.selcolg"),gp.getIntValue("vgraphic.selcolb"));
		selWidth = gp.getIntValue("vgraphic.selwidth");
		
		vG = Graph;
		vG.addObserver(this); //Die Graphikumgebung als Observer der Datenstruktur eintragen
		vGh = new GraphHistoryManager(vG);
		Controls = new HashMap<String,Observable>();
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
	 * Paint Edges in the graphic
	 * @param g
	 */
	private void paintEdges(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g;
		Iterator<VEdge> ei = vG.getEdgeIterator();
		g2.setStroke(vEdgeStyle);
		boolean directed = vG.isDirected();
		while (ei.hasNext()) // drawEdges
		{
			VEdge temp = ei.next(); //Grafischer Teil
			Vector<Integer> val = vG.getEdgeProperties(temp.index); //Daten dazu
			Point p1 = vG.getNode(val.get(MGraph.EDGESTARTINDEX)).getPosition(); //Startkoordinaten
			VNode EndNode = vG.getNode(val.get(MGraph.EDGEENDINDEX)); //Endknoten
			VNode StartNode = vG.getNode(val.get(MGraph.EDGESTARTINDEX)); //Endknoten
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
				int pos; boolean top; double part;
				if (temp.getTextProperties().getPosition() > 50)
				{ //below edge
					pos = temp.getTextProperties().getPosition() - 50;
					top = false;
					part = 1-((double)pos)*2.0d/100.0d; //from the end - so 1- at the part
				}
				else
				{
					pos = temp.getTextProperties().getPosition();
					top = true;
					part = ((double)pos)*2.0d/100.0d;
				}
				Point p = temp.getPointonEdge(p1,p2, part);
				Point2D.Double dir = temp.getDirectionatPointonEdge(p1,p2, part);
				double l = dir.distance(0.0d,0.0d);
				//and norm dir
				dir.x = dir.x/l; dir.y = dir.y/l;
				//And now from the point on the edge the distance
				Point m = new Point(0,0); //middle of the text
				if (top) //Countter Clockwise rotation of dir
				{
					m.x = p.x + (new Long(Math.round(((double)temp.getTextProperties().getDistance())*dir.y)).intValue());
					m.y = p.y - (new Long(Math.round(((double)temp.getTextProperties().getDistance())*dir.x)).intValue());				
				}
				else //invert both direction elements
				{
					m.x = p.x - (new Long(Math.round(((double)temp.getTextProperties().getDistance())*dir.y)).intValue());
					m.y = p.y + (new Long(Math.round(((double)temp.getTextProperties().getDistance())*dir.x)).intValue());				
				}
				//get the text wich should be displayd
			    String text = "";
			    if (temp.getTextProperties().isshowvalue())
					text = val.get(MGraph.EDGEVALUE).toString();
			    else
			    	text = vG.getEdgeName(temp.index);
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
				m.x -= Math.round(adv/2); m.y += Math.round(hgt/2);
				g2.drawString(text,m.x,m.y);
			}
		}
	}
	/**
	 * Paint nodes in the Graphic g
	 * @param g
	 */
	private void paintNodes(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g;
		Iterator<VNode> nodeiter = vG.getNodeIterator();
		while (nodeiter.hasNext()) // drawNodes
		{
			VNode temp = nodeiter.next();
			if ((((temp.getSelectedStatus()&VItem.SELECTED)==VItem.SELECTED)||((temp.getSelectedStatus()&VItem.SOFT_SELECTED)==VItem.SOFT_SELECTED))&&((temp.getSelectedStatus()&VItem.SOFT_DESELECTED)!=VItem.SOFT_DESELECTED))
			{ //Draw all Nodes that are selected or temporarily selected as selected in the GUI
				g2.setColor(selColor);
				g2.fillOval(Math.round((temp.getdrawpoint().x-selWidth/2)*zoomfactor), 
						Math.round((temp.getdrawpoint().y-selWidth/2)*zoomfactor),
						Math.round((temp.getSize() + selWidth)*zoomfactor),
						Math.round((temp.getSize() + selWidth)*zoomfactor));
			}
			g2.setColor(temp.getColor());
			g2.fillOval(Math.round(temp.getdrawpoint().x*zoomfactor), Math.round(temp.getdrawpoint().y*zoomfactor), Math.round(temp.getSize()*zoomfactor), Math.round(temp.getSize()*zoomfactor));
			if (temp.isNameVisible())
			{	
				g2.setColor(Color.black);					
				Font f = new Font("Arial",Font.PLAIN, Math.round(temp.getNameSize()*zoomfactor));
				g2.setFont(f);
				//mittelpunkt des Textes
				int x = temp.getPosition().x + Math.round((float)temp.getNameDistance()*(float)Math.cos(Math.toRadians((double)temp.getNameRotation())));
				int y = temp.getPosition().y - Math.round((float)temp.getNameDistance()*(float)Math.sin(Math.toRadians((double)temp.getNameRotation())));
				
				//System.err.println("For "+temp.getNameRotation()+" Degrees  and NameDistance "+temp.getNameDistance()
				//					+" is ("+temp.getPosition().x+"+("+Math.round((float)temp.getNameDistance()*(float)Math.cos(Math.toRadians((double)temp.getNameRotation())))+") = "+x
				//					+" and ("+temp.getPosition().y+"+"+Math.round((float)temp.getNameDistance()*(float)Math.sin(Math.toRadians((double)temp.getNameRotation())))+") = "+y);
			    FontMetrics metrics = g2.getFontMetrics(f);
			    int hgt = metrics.getAscent()-metrics.getLeading()-metrics.getDescent();
			    int adv = metrics.stringWidth(vG.getNodeName(temp.index));
			    x = Math.round(x*zoomfactor);
			    y = Math.round(y*zoomfactor);
			    x -= Math.round(adv/2); y += Math.round(hgt/2);
				g2.drawString(vG.getNodeName(temp.index), x,y);
				
			}
		}
		g2.setColor(Color.black);
	}
	/**
	 * Paint Edge Controll-Points in the Graphic
	 * @param g
	 */
	private void paintControllPoints(Graphics g)
	{
		Iterator<VEdge> edgeiter = vG.getEdgeIterator();
		while (edgeiter.hasNext()) // drawEdges
		{
			VEdge temp = edgeiter.next(); //Grafischer Teil
			Vector<Point> p = temp.getControlPoints();
			for (int i=0; i<p.size(); i++)
				drawCP(g,p.get(i));
		}
	}
	/**
	 * Draw a single ControllPoint in
	 * @param g the Graphic g
	 * @param p at the Position of this point
	 */
	private void drawCP(Graphics g, Point p)
	{
		int cpsize = gp.getIntValue("vgraphic.cpsize");
		Graphics2D g2 = (Graphics2D) g;
		g2.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
		g2.setColor(Color.BLUE);
		g2.drawLine(Math.round((p.x-cpsize)*zoomfactor),Math.round(p.y*zoomfactor),Math.round((p.x+cpsize)*zoomfactor),Math.round(p.y*zoomfactor));
		g2.drawLine(Math.round(p.x*zoomfactor),Math.round((p.y-cpsize)*zoomfactor),Math.round(p.x*zoomfactor),Math.round((p.y+cpsize)*zoomfactor));
		
	}
	/**
	 * Paint a grid in the Graphics
	 * @param g
	 */
	private void paintgrid(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g;
		if ((!gridenabled)||(vp==null))
			return;
		int minX = vp.getViewRect().x;
		int maxX = vp.getViewRect().x + vp.getViewRect().width;
		int minY = vp.getViewRect().y;
		int maxY = vp.getViewRect().y + vp.getViewRect().height;
		g2.setColor(Color.GRAY);
		g2.setStroke(new BasicStroke(1,BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		//Draw vertical Lines
		for (int i=Math.round(gridx*zoomfactor); i<maxX; i+=Math.round(gridx*zoomfactor))
		{
			if (i >= minX)
				g2.drawLine(i, minY,i,maxY);
		}
		//horizontal
		for (int i=Math.round(gridy*zoomfactor); i<maxY; i+=Math.round(gridy*zoomfactor))
		{
			if (i >= minY)
				g2.drawLine(minX, i, maxX, i);
		}		
	}
	/**
	 * Get the represented Graph for manipulation.
	 * The Manipulation is handled by pushing Notifications to the Graph-Observers 
	 * 
	 * @return the actual VGraph that is handled in the this GUI
	 */
	public VGraph getVGraph()
	{
		return vG;
	}
	/**
	 * Get the GraphHIstoryManager that tracks, records and enables undo for the graph
	 * Use this Method to do the Undo/Redo and sth like that
	 * 
	 * @return GraphHistoryManager
	 */
	public GraphHistoryManager getGraphHistoryManager()
	{
		return vGh;
	}
	/**
	 * Set the ViewPort of the VGraphic
	 * @param p new ViewPort
	 */
	public void setViewPort(JViewport p)
	{
		vp = p;
	}
	/**
	 * Get the Viewport
	 * @return
	 */
	public JViewport getViewPort()
	{
		return vp;
	}
	/**
	 * Add an Element to the controls.
	 * These Controls are observed for changes
	 * The Name is used to identify the Piece and react on changes in it
	 * @param name
	 * @param element
	 */
	public void addPiece(String name, Observable element)
	{
		Controls.put(name,element);
		element.addObserver(this);
	}
	/**
	 * Remove an Element of the controls. It is no longer observed
	 * So no further action in the Observable object trigger changes here
	 * @param name name of the Object to be removed
	 */
	public void removePiece(String name)
	{
		Controls.get(name).deleteObserver(this);
		Controls.remove(name);
	}
	/**
	 * Change Mouse-Handling to a new State.
	 * Mouse actions in Progress are stopped (e.g. an Drag in progress)
	 * 
	 * possible Values
	 * - NO_MOUSEHANDLING - Mouse-Actions aren't handled in any way
	 * - STD_MOUSEHANDLING - Standard-Mode (default)
	 * - OCM_MOUSEHANDLING - Set to OneClick-Mode
	 * 
	 * @param state new Mousehandling state
	 */
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
				Click = new OCMClickMouseHandler(vG);
				Drag = new OCMDragMouseHandler(vG);
				this.addMouseListener(Drag);
				this.addMouseMotionListener(Drag);
				this.addMouseListener(Click);
			break;
			case STD_MOUSEHANDLING:
			default:
				resetMouseHandling();
				Click = new StandardClickMouseHandler(vG);
				Drag = new StandardDragMouseHandler(vG);
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
	/**
	 * Set The possibility to Zoom to a new value
	 * @param activity ture, if zoom should be able, else false
	 */
	public void setZoomEnabled(boolean activity)
	{
		usezoom = activity;
		if (usezoom)
		{
			zoomfactor = (float)gp.getIntValue("vgraphic.zoom") / 100;
		}
		else
		{
			zoomfactor = 1.0f;
		}
	}
	
	public void update(Observable o, Object arg) 
	{
		if (o.equals(vG)&&(((GraphMessage)arg)!=null)) //Der Graph wurde aktualisiert und auch echt ein String gegeben
		{
			GraphMessage m = (GraphMessage)arg;
			if ((m.getAffectedTypes()&(GraphMessage.ALL_ELEMENTS|GraphMessage.SELECTION)) > 0) //Anything in Elements or selections changed
			{
				Point MouseOffSet = new Point(0,0);
				if (Drag!=null)
					MouseOffSet = Drag.getMouseOffSet(); //Bewegungspunkt
				Point GraphSize = new Point(Math.round(vG.getMaxPoint(getGraphics()).x*zoomfactor),Math.round(vG.getMaxPoint(getGraphics()).y*zoomfactor));
				int offset = gp.getIntValue("vgraphic.framedistance");
				int x = Math.max(GraphSize.x+offset,vp.getViewRect().x+vp.getViewRect().width-offset);
				int y = Math.max(GraphSize.y+offset,vp.getViewRect().y+vp.getViewRect().height-offset);
				setPreferredSize(new Dimension(x,y));
				setSize(new Dimension(x,y));
				//Nun soll mitgescrollt werden, falls ein Knoten oder eine Kante (CP) in Bewegung ist 
				//und die Maus theoretisch aus dem sichtbaren kram raus ist
				Rectangle r = vp.getViewRect();
				if ((Drag!=null)&&(Drag.dragged())&&(!r.contains(MouseOffSet)))	
				{		
					//System.err.print("Move Me : "+r+" and "+MouseOffSet);
					int xdiff = MouseOffSet.x-r.x-r.width;
					if (xdiff > 0) //Dann ist die Maus nach rechts rausgewandert
						r.x += xdiff;
					xdiff = r.x - MouseOffSet.x;
					if (xdiff > 0) //nach Links rausgewandert
						r.x -= xdiff;
					int ydiff = MouseOffSet.y-r.y-r.height;
					if (ydiff > 0) //nach unten rausgewandert
						r.y += ydiff;
					ydiff = r.y - MouseOffSet.y;
					if (ydiff > 0) //nach oben rausgewandert
						r.y -=ydiff;
					if (r.y < 0) r.y = 0;
					if (r.x < 0) r.x = 0;
					vp.setViewPosition(new Point(r.x,r.y));
					//wiederholen bis das nicht mehr der fall ist ?
				}
				vp.revalidate();
				vp.getParent().validate();
				repaint();	
			}
			else if ((m.getAction()&GraphMessage.SUBSET)==GraphMessage.SUBSET)
			{
				repaint();
				if (Click!=null)
					Click.updateSubSetList();
			}
		}
		else if ((Controls.get((String)arg)!=null)&&(Controls.get((String)arg).equals(o))) //Der String entpsirhct dem eingetragenen Control
		{
			if (((String)arg).equals("Zoom"))
			{
				//reset zoomfactor
				gp.setIntValue("vgraphic.zoom",((ZoomComponent)o).getZoom());
			}
			else if (((String)arg).equals("Grid"))
			{
				gridx = ((GridComponent)o).getGridX();
				gridy = ((GridComponent)o).getGridY();
				gridenabled = ((GridComponent)o).isEnabled();
				gridorientated = ((GridComponent)o).isOrientated();
				if (Drag!=null)
				{
					Drag.setGridOrientated(gridenabled&&gridorientated);
					Drag.setGrid(gridx,gridy);
				}
				repaint();
				//UpdateGrid
			}
		}
		else if (o.equals(gp)) //Preferences geupdated
		{
			if (usezoom)
			{
				zoomfactor = (float)gp.getIntValue("vgraphic.zoom") / 100;
				if (Controls.get("Zoom")!=null) //A Zoom Component is added to this 
				{
					if (((ZoomComponent)Controls.get("Zoom")).getZoom()!=gp.getIntValue("vgraphic.zoom")) //The Change was not set from the ZoomComponent
					{ //Update the Component
						((ZoomComponent)Controls.get("Zoom")).setZoom(gp.getIntValue("vgraphic.zoom"));
					}
				}
			}
			gridx = gp.getIntValue("grid.x");
			gridy = gp.getIntValue("grid.y");
			gridenabled = gp.getBoolValue("grid.enabled");
			gridorientated = gp.getBoolValue("grid.orientated");
			if (Drag!=null)
			{
				Drag.setGridOrientated(gridenabled&&gridorientated);
				Drag.setGrid(gridx,gridy);
			}
			selColor = new Color(gp.getIntValue("vgraphic.selcolr"),gp.getIntValue("vgraphic.selcolg"),gp.getIntValue("vgraphic.selcolb"));
			selWidth = gp.getIntValue("vgraphic.selwidth");
			vG.pushNotify(new GraphMessage(GraphMessage.SELECTION,GraphMessage.UPDATE)); //Zoom and Selection stuff belong to the mark actions on a graph - they don't change the state to "not saved yet"
			repaint();
		}
	}
}