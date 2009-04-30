package view;


import history.GraphHistoryManager;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.Observable;
import java.util.Vector;

import control.*;
import control.nurbs.*;
import model.*;
import model.Messages.GraphConstraints;
import model.Messages.GraphMessage;

/**
 * - Implementierung der Darstellung eines Hypergraphen in einer Graphics2D Umgebung
 * 
 * @author Ronny Bergmann
 *
 */
public class VHyperShapeGraphic extends VHyperGraphic
{
	// VGraph : Die Daten des Graphen
	private VHyperGraph vG;
	// Visual Styles
	private BasicStroke vHyperEdgeStyle;
	private ShapeMouseHandler ShapeModifier;
	private static final long serialVersionUID = 1L;
	private int actualMouseState, highlightedHyperEdge;
	
	/**
	 * Create a New Graphical representation of an VGraph with a given size
	 * @param d Size of the Area the VGraphics gets
	 * @param Graph Graph to be represented
	 */
	public VHyperShapeGraphic(Dimension d,VHyperGraph Graph, int hyperedgeindex)
	{
		super(d,Graph);
		//GeneralPreferences als beobachter eintragen

		vHyperEdgeStyle = new BasicStroke(5.0f, BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND);
		selColor = new Color(gp.getIntValue("vgraphic.selcolr"),gp.getIntValue("vgraphic.selcolg"),gp.getIntValue("vgraphic.selcolb"));
		selWidth = gp.getIntValue("vgraphic.selwidth");
		actualMouseState = NO_MOUSEHANDLING;
		highlightedHyperEdge = hyperedgeindex;
		vG = Graph;
		vG.addObserver(this); //Die Graphikumgebung als Observer der Datenstruktur eintragen
		//TODO: HistoryManager Umschreiben
		vGh = new GraphHistoryManager(new VGraph(true,true,true));
	}	

	public void paint(Graphics2D g2)
	{
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		paintgrid(g2);
		paintMouseModeDetails(g2);
		paintHyperEdges(g2);
		paintNodes(g2);
		if ((ShapeModifier!=null)&&(ShapeModifier.getSelectionRectangle()!=null))
		{
			g2.setColor(selColor);
			g2.setStroke(new BasicStroke(1,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));
			g2.draw(ShapeModifier.getSelectionRectangle());
		}
		paintDEBUG(g2);
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
			VHyperEdge temp = ei.next();
			if (temp.getIndex()==highlightedHyperEdge)
				g2.setColor(temp.getColor());
			else
				g2.setColor(Color.GRAY);
			g2.setStroke(new BasicStroke(temp.getWidth()*zoomfactor,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));
			if (!temp.getShape().isEmpty())
			{
				NURBSShape s = temp.getShape().clone();
				s.scale(zoomfactor);
				g2.draw(temp.getLinestyle().modifyPath(s.getCurve(5d/(double)zoomfactor),temp.getWidth(),zoomfactor));
			}
		}
	}

	//@override from VCommonGraphic to only draw Nodes from the hyperedge normal and all other Gray
	protected void paintNodes(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g;
		Iterator<VNode> nodeiter = vG.modifyNodes.getIterator();
		MHyperEdge hEdge = vG.getMathGraph().modifyHyperEdges.get(highlightedHyperEdge);
	
		if (hEdge==null)
			return;
		while (nodeiter.hasNext()) // drawNodes
		{
			VNode temp = nodeiter.next();

			if (hEdge.containsNode(temp.getIndex())) //Colored Node?
				g2.setColor(temp.getColor());
			else
				g2.setColor(Color.GRAY); 
			g2.fillOval(Math.round(temp.getdrawpoint().x*zoomfactor), Math.round(temp.getdrawpoint().y*zoomfactor), Math.round(temp.getSize()*zoomfactor), Math.round(temp.getSize()*zoomfactor));
			if (temp.isNameVisible())
			{	
				if (!g2.getColor().equals(Color.GRAY))
						g2.setColor(Color.black);					
				Font f = new Font("Arial",Font.PLAIN, Math.round(temp.getNameSize()*zoomfactor));
				g2.setFont(f);
				//mittelpunkt des Textes
				int x = temp.getPosition().x + Math.round((float)temp.getNameDistance()*(float)Math.cos(Math.toRadians((double)temp.getNameRotation())));
				int y = temp.getPosition().y - Math.round((float)temp.getNameDistance()*(float)Math.sin(Math.toRadians((double)temp.getNameRotation())));
				
			    FontMetrics metrics = g2.getFontMetrics(f);
			    int hgt = metrics.getAscent()-metrics.getLeading()-metrics.getDescent();
			    int adv = metrics.stringWidth(vG.getMathGraph().modifyNodes.get(temp.getIndex()).name);
			    x = Math.round(x*zoomfactor);
			    y = Math.round(y*zoomfactor);
			    x -= Math.round(adv/2); y += Math.round(hgt/2);
				g2.drawString(vG.getMathGraph().modifyNodes.get(temp.getIndex()).name, x,y);
				
			}
		}
	}
	
	private void paintMouseModeDetails(Graphics2D g2)
	{
		if ((actualMouseState&(CIRCLE_MOUSEHANDLING|CURVEPOINT_MOVEMENT_MOUSEHANDLING|SHAPE|INTERPOLATION_MOUSEHANDLING))> 0)
		{
			NURBSShape tempshape = ((ShapeMouseHandler)ShapeModifier).getShape();
			if ((tempshape!=null)&&(ShapeModifier.dragged()))
			{
				NURBSShape draw = tempshape.clone();
				draw.scale(zoomfactor);
				g2.setStroke(new BasicStroke(1,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));
				g2.setColor(selColor);
				g2.draw(draw.getCurve(5d/(double)zoomfactor)); //draw only a preview
			}			
		}
		if (actualMouseState==INTERPOLATION_MOUSEHANDLING)
		{
			Vector<Point2D> IP = (Vector<Point2D>) ShapeModifier.getShapeParameters().get(NURBSShapeFactory.POINTS);
			Iterator<Point2D> iter = IP.iterator();
			while (iter.hasNext())
			{
				Point2D p = iter.next();
				Point p2 = new Point(Math.round((float)p.getX()),Math.round((float)p.getY()));
				this.drawCP(g2, p2, Color.BLUE);
			}
		}
		if ((actualMouseState==CIRCLE_MOUSEHANDLING)||((actualMouseState&SHAPE) > 0))
		{
			Point p = (Point) ShapeModifier.getShapeParameters().get(NURBSShapeFactory.CIRCLE_ORIGIN);
			Point p2 = ShapeModifier.getMouseOffSet();
			if ((p!=null)&&(p2.x!=0)&&(p2.y!=0)) //Set per Drag
			{
				GeneralPath path = new GeneralPath();
				path.moveTo(p.x*zoomfactor,p.y*zoomfactor);
				path.lineTo(p2.x,p2.y);
				g2.draw(path);			
			}
		}
	}
	private void paintDEBUG(Graphics2D g2)
	{
		g2.setColor(Color.orange.darker());
		NURBSShape s =  vG.modifyHyperEdges.get(highlightedHyperEdge).getShape().clone();
		s.scale(zoomfactor);
		Iterator<Point2D> pi = s.controlPoints.iterator();
		Point2D last=null, first=null;
		while (pi.hasNext())
		{
			Point2D p = (Point2D) pi.next();
			if (first==null)
				first = p;
			g2.setStroke(new BasicStroke(1,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));
			g2.drawLine(Math.round(((float)p.getX()-3)),Math.round((float)p.getY()),Math.round(((float)p.getX()+3)),Math.round((float)p.getY()));
			g2.drawLine(Math.round(((float)p.getX())),Math.round(((float)p.getY()-3)),Math.round((float)p.getX()),Math.round(((float)p.getY()+3)));
			if (last!=null)
				g2.drawLine(Math.round((float)last.getX()),Math.round((float)last.getY()), Math.round((float)p.getX()), Math.round((float)p.getY()));
			last = p;
		}
		s.scale(1/zoomfactor);
	}
	public int getMouseHandling()
	{
		return actualMouseState;
	}
	//MOdified to only Handle those shape stati
	public void setMouseHandling(int state) {
		if (ShapeModifier!=null)
		{
			MouseEvent e = new MouseEvent(this,111,System.nanoTime(),0,-1,-1,1,false);		
			ShapeModifier.mouseReleased(e);
		}
		resetMouseHandling();
		switch (state) 
		{
			case CIRCLE_MOUSEHANDLING:
				ShapeModifier = new CircleCreationHandler(this);
				this.addMouseListener(ShapeModifier);
				this.addMouseMotionListener(ShapeModifier);
				actualMouseState = state;
			break;
			case INTERPOLATION_MOUSEHANDLING:
				ShapeModifier = new InterpolationCreationHandler(this);
				this.addMouseListener(ShapeModifier);
				this.addMouseMotionListener(ShapeModifier);
				actualMouseState = state;
			break;
			case CURVEPOINT_MOVEMENT_MOUSEHANDLING:
				ShapeModifier = new FreeModificationHandler(this, highlightedHyperEdge);
				this.addMouseListener(ShapeModifier);
				this.addMouseMotionListener(ShapeModifier);
				actualMouseState = state;
			break;
			case SHAPE_ROTATE_MOUSEHANDLING:
				ShapeModifier = new ShapeModificationHandler(this, highlightedHyperEdge);
				((ShapeModificationHandler)ShapeModifier).setModificationState(ShapeModificationHandler.ROTATION);
				this.addMouseListener(ShapeModifier);
				this.addMouseMotionListener(ShapeModifier);
				actualMouseState = state;
			break;
			case SHAPE_TRANSLATE_MOUSEHANDLING:
				ShapeModifier = new ShapeModificationHandler(this, highlightedHyperEdge);
				((ShapeModificationHandler)ShapeModifier).setModificationState(ShapeModificationHandler.TRANSLATION);
				this.addMouseListener(ShapeModifier);
				this.addMouseMotionListener(ShapeModifier);
				actualMouseState = state;
			break;
			case SHAPE_SCALE_MOUSEHANDLING:
				ShapeModifier = new ShapeModificationHandler(this, highlightedHyperEdge);
				((ShapeModificationHandler)ShapeModifier).setModificationState(ShapeModificationHandler.SCALING);
				this.addMouseListener(ShapeModifier);
				this.addMouseMotionListener(ShapeModifier);
				actualMouseState = state;
			break;
			case NO_MOUSEHANDLING:
			default:
				actualMouseState = NO_MOUSEHANDLING;
			break;
		}
		if (ShapeModifier!=null) //Update Info in the Drag-Handler about the Grid.
		{
			ShapeModifier.setGrid(gridy,gridy);
			ShapeModifier.setGridOrientated(gridenabled&&gridorientated);
		}
		repaint();
	}
	/**
	 * Set the MouseHandling to NO_MOUSEHANDLING
	 */
	private void resetMouseHandling()
	{
		this.removeMouseListener(ShapeModifier);
		this.removeMouseMotionListener(ShapeModifier);
		ShapeModifier = null;
	}
	public Vector<Object> getShapeParameters()
	{
		if (ShapeModifier!=null)
			return ShapeModifier.getShapeParameters();
	else
		return null;		
	}
	public void setShapeParameters(Vector<Object> p)
	{
		if (ShapeModifier!=null)
			ShapeModifier.setShapeParameters(p);
	}
	protected Point DragMouseOffSet()
	{
		if ((ShapeModifier!=null)&&(ShapeModifier.dragged()))
				return ShapeModifier.getMouseOffSet();
		else
			return null;
	}
	public void update(Observable o, Object arg)
	{
		super.update(o, arg);
		if (arg instanceof GraphMessage)
		{
			GraphMessage m = (GraphMessage)arg;
			if ((ShapeModifier!=null)&&(!ShapeModifier.dragged()) //If we have no drag (anymore) and have one of the MOdification-Mousehandlings AND a Block End
					&&((actualMouseState&CIRCLE_MOUSEHANDLING|CURVEPOINT_MOVEMENT_MOUSEHANDLING|SHAPE|INTERPOLATION_MOUSEHANDLING)>0)
					&&((m.getModification()&GraphConstraints.BLOCK_END)==GraphConstraints.BLOCK_END)) 
			//Drag just ended -> Set Circle as Shape
			{
				vG.modifyHyperEdges.get(highlightedHyperEdge).setShape(ShapeModifier.getShape());
				vG.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE, GraphConstraints.UPDATE|GraphConstraints.HYPEREDGESHAPE)); //HyperEdgeShape Updated
				ShapeModifier.resetShape();
			}
			repaint();
		}
	}
}