package view;

import history.CommonGraphHistoryManager;
import io.GeneralPreferences;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JViewport;

import view.pieces.GridComponent;
import view.pieces.ZoomComponent;

import model.*;
import model.Messages.GraphConstraints;
import model.Messages.GraphMessage;

public abstract class VCommonGraphic extends Component implements Observer
{
	/**
	 * These are Different MODI a VCommonGraphic may depend its controlers on
	 */
	private static final long serialVersionUID = 1L;
	//
	// Mouse Modi for all subversions
	//
	public static final int NO_MOUSEHANDLING = 0;
	public static final int STD_MOUSEHANDLING = 1;
	public static final int OCM_MOUSEHANDLING = 2;
	public static final int CIRCLE_MOUSEHANDLING = 4;
	public static final int CURVEPOINT_MOVEMENT_MOUSEHANDLING = 8;
	public static final int INTERPOLATION_MOUSEHANDLING = 16;
	public static final int SHAPE_MOUSEHANDLING = 32;
	public static final int SUBCURVE_MOUSEHANDLING = 64;
	public static final int KNOT_MODIFICATION_MOUSEHANDLING = 128;
	//
	//Detail Info on some MouseHandlers
	//
	public static final int SET_START = 256;
	public static final int SET_END = 512;
	public static final int TOGGLE = 1024;
	public static final int SCALE = 2048;
	public static final int ROTATE = 4096;
	public static final int TRANSLATE = 8192;
	public static final int SCALE_DIR = 16384;
	public static final int ADD = 32768;
	public static final int REMOVE = 65536;

	public static final int DETAIL_MASK = ADD|REMOVE|SET_START|SET_END|TOGGLE|SCALE|ROTATE|TRANSLATE|SCALE_DIR;
	public static final int NO_DETAIL = 0;
	//
	// GetType Subclasses
	//
	public static final int VGRAPHIC = 1;
	public static final int VHYPERGRAPHIC = 2;
//	public static final int VHYPERSHAPEGRAPHIC = 4;
	
	private HashMap<String,Observable> Controls;
	protected Color selColor; //Color of selected Elements
	protected int selWidth; //Width of selection border

	protected float zoomfactor; //protected for all subelements
	protected int gridx,gridy;
	protected boolean gridenabled,gridorientated;
	protected GeneralPreferences gp;
	private JViewport vp;
	private VGraphInterface vG;
	protected CommonGraphHistoryManager vGh;

	public VCommonGraphic(Dimension d, VGraphInterface GeneralGraph)
	{
		vG=GeneralGraph;
		gp = GeneralPreferences.getInstance();
		gp.addObserver(this);
		this.setSize(d);
		this.setBounds(0, 0, d.width, d.height);
		zoomfactor = gp.getFloatValue("zoom");
		gridx = gp.getIntValue("grid.x");
		gridy = gp.getIntValue("grid.y");
		gridenabled = gp.getBoolValue("grid.enabled");
		gridorientated = gp.getBoolValue("grid.orientated");

		Controls = new HashMap<String,Observable>();
		selColor = new Color(gp.getIntValue("vgraphic.selcolr"),gp.getIntValue("vgraphic.selcolg"),gp.getIntValue("vgraphic.selcolb"));
		selWidth = gp.getIntValue("vgraphic.selwidth");
	}
	/**
	 * Paint nodes in the Graphic g
	 * @param g
	 */
	protected void paintNodes(Graphics g)
	{
		VNodeSet nodes=null;
		MNodeSet mnodes=null;
		//Get actual Sets depending on GraphInterface Reference
		if (vG.getType()==VGraphInterface.GRAPH)
		{
			nodes = ((VGraph)vG).modifyNodes;
			mnodes = ((VGraph)vG).getMathGraph().modifyNodes;
		}
		else if (vG.getType()==VGraphInterface.HYPERGRAPH)
		{
			nodes =  ((VHyperGraph)vG).modifyNodes;
			mnodes = ((VHyperGraph)vG).getMathGraph().modifyNodes;
		}
		Graphics2D g2 = (Graphics2D) g;
		Iterator<VNode> nodeiter = nodes.getIterator();
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
				Point m = temp.getTextCenter();
				
			    FontMetrics metrics = g2.getFontMetrics(f);
			    int hgt = metrics.getAscent()-metrics.getLeading()-metrics.getDescent();
			    int adv = metrics.stringWidth(mnodes.get(temp.getIndex()).name);
			    m.x = Math.round(m.x*zoomfactor); m.y = Math.round(m.y*zoomfactor); //Zoom
			    m.x -= Math.round(adv/2); m.y += Math.round(hgt/2); //Move to top left
				g2.drawString(mnodes.get(temp.getIndex()).name, m.x,m.y);
				
			}
		}
		g2.setColor(Color.black);
	}
	/**
	 * Draw a single ControllPoint in
	 * @param g the Graphic g
	 * @param p at the Position of this point
	 */
	public void drawCP(Graphics g, Point p, Color c)
	{
		int cpsize = gp.getIntValue("vgraphic.cpsize");
		Graphics2D g2 = (Graphics2D) g;
		g2.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
		g2.setColor(c);
		g2.drawLine(Math.round((p.x-cpsize)*zoomfactor),Math.round(p.y*zoomfactor),Math.round((p.x+cpsize)*zoomfactor),Math.round(p.y*zoomfactor));
		g2.drawLine(Math.round(p.x*zoomfactor),Math.round((p.y-cpsize)*zoomfactor),Math.round(p.x*zoomfactor),Math.round((p.y+cpsize)*zoomfactor));
		
	}
	/**
	 * Paint a grid in the Graphics
	 * @param g
	 */
	protected void paintgrid(Graphics g)
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
	 * Set Zoom to a specific value - if a Component named „Zoom“ (ZoomComponent) exists, ist value is set, too
	 * @param percent new Zoom in percent
	 */
	public void setZoom(int percent)
	{
		zoomfactor = ((float)percent)/100.0f;
		if (this.Controls.get("Zoom")!=null)
		{
			((ZoomComponent)Controls.get("Zoom")).setZoom(percent);
		}
	}
	/**
	 * Get the actual Zoom-Factor in percent.
	 * @return an int representing the zoom in percent.
	 */
	public int getZoom()
	{
		return (new Float(zoomfactor*100.0f)).intValue();
	}
	protected abstract Point DragMouseOffSet();
	protected void handleGraphUpdate(GraphMessage m)
	{
		if ((m.getAffectedElementTypes()&(GraphConstraints.GRAPH_ALL_ELEMENTS|GraphConstraints.SELECTION)) > 0) //Anything in Elements or selections changed
		{
			Point MouseOffSet = vp.getViewPosition();
			if (DragMouseOffSet()!=null)
				MouseOffSet = DragMouseOffSet(); //Bewegungspunkt
			Point GraphSize = new Point(Math.round(vG.getMaxPoint(getGraphics()).x*zoomfactor),Math.round(vG.getMaxPoint(getGraphics()).y*zoomfactor));
			int offset = gp.getIntValue("vgraphic.framedistance");
			int x = Math.max(GraphSize.x+offset,vp.getViewRect().x+vp.getViewRect().width-offset);
			int y = Math.max(GraphSize.y+offset,vp.getViewRect().y+vp.getViewRect().height-offset);
			setPreferredSize(new Dimension(x,y));
			setSize(new Dimension(x,y));
			//Nun soll mitgescrollt werden, falls ein Knoten oder eine Kante (CP) in Bewegung ist 
			//und die Maus theoretisch aus dem sichtbaren kram raus ist
			Rectangle r = vp.getViewRect();
			if ((DragMouseOffSet()!=null)&&(!r.contains(MouseOffSet)))	
			{		
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
		else if ((m.getModifiedElementTypes()&GraphConstraints.SUBGRAPH)==GraphConstraints.SUBGRAPH)
		{
			repaint();
		}
	}
	public void handlePreferencesUpdate()
	{
		gridx = gp.getIntValue("grid.x");
		gridy = gp.getIntValue("grid.y");
		gridenabled = gp.getBoolValue("grid.enabled");
		gridorientated = gp.getBoolValue("grid.orientated");
		selColor = new Color(gp.getIntValue("vgraphic.selcolr"),gp.getIntValue("vgraphic.selcolg"),gp.getIntValue("vgraphic.selcolb"));
		selWidth = gp.getIntValue("vgraphic.selwidth");
		vG.pushNotify(new GraphMessage(GraphConstraints.SELECTION,GraphConstraints.UPDATE)); //Zoom and Selection stuff belong to the mark actions on a graph - they don't change the state to "not saved yet"
		repaint();
	}
	protected void updateControls(Observable o, Object arg)
	{
		if ((Controls.get(arg)!=null)&&(Controls.get(arg).equals(o))) 
			//We got a Message from an Control that has Subscribed itself
			{
				if (arg.equals("Zoom"))
				{
					ZoomComponent myZoom = ((ZoomComponent)o);
					if (myZoom.getZoom()!=getZoom()) //Zoom really changed externally - update here
						zoomfactor = ((float)myZoom.getZoom())/100.0f;
					gp.deleteObserver(this);
					gp.setFloatValue("zoom",zoomfactor);
					gp.addObserver(this);
					//internally update scrolls
					handleGraphUpdate(new GraphMessage(GraphConstraints.SELECTION,GraphConstraints.UPDATE));
				}
				else if (arg.equals("Grid"))
				{
					gridx = ((GridComponent)o).getGridX();
					gridy = ((GridComponent)o).getGridY();
					gridenabled = ((GridComponent)o).isEnabled();
					gridorientated = ((GridComponent)o).isOrientated();
					gp.deleteObserver(this);
					gp.setIntValue("grid.x",gridx);
					gp.setIntValue("grid.y",gridy);
					gp.setBoolValue("grid.enabled",gridenabled);
					gp.setBoolValue("grid.orientated",gridorientated);
					gp.addObserver(this);
					repaint();
					//UpdateGrid
				}
			}
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
	public abstract void setMouseHandling(int state);
	/**
	 * Get Type of Graphic
	 * Either
	 * VHYPERGRAPHIC or VGRAPHIC
	 * @return
	 */
	public abstract int getType();
	/**
	 * Get the GraphHIstoryManager that tracks, records and enables undo for the graph
	 * Use this Method to do the Undo/Redo and sth like that
	 * 
	 * @return GraphHistoryManager
	 */
	public CommonGraphHistoryManager getGraphHistoryManager() {
		return vGh;
	}
}