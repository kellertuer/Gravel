package view;



import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.Observable;
import java.util.Vector;

import history.*;
import control.nurbs.*;
import model.*;
import model.Messages.*;

/**
 * - Implementierung der Darstellung eines Hypergraphen in einer Graphics2D Umgebung
 * 
 * @author Ronny Bergmann
 *
 */
public class VHyperShapeGraphic extends VHyperGraphic
{
	// Visual Styles
	private BasicStroke vHyperEdgeStyle;
	private ShapeCreationMouseHandler firstModus;
	private ShapeModificationMouseHandler secondModus;
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

		vGh = new HyperEdgeShapeHistoryManager(vG,this,hyperedgeindex);
	}	

	public void paint(Graphics2D g2)
	{
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		paintgrid(g2);
		paintMouseModeDetails(g2);
		paintHyperEdges(g2);
		paintNodes(g2);
		if ((firstModus!=null)&&(firstModus.getSelectionRectangle()!=null))
		{
			g2.setColor(selColor);
			g2.setStroke(new BasicStroke(1,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));
			g2.draw(firstModus.getSelectionRectangle());
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
		if ( ((actualMouseState&(CIRCLE_MOUSEHANDLING|INTERPOLATION_MOUSEHANDLING))> 0) && (firstModus!=null)) //We'Re in Creation
		{
			paintCreationDetails(g2);
		}
		if (secondModus!=null)
		{
			paintModificationDetails(g2);
		}
	}
	private void paintCreationDetails(Graphics2D g2)
	{
		if (firstModus==null)
			return;
		NURBSShape tempshape = firstModus.getShape();
		if ((tempshape!=null)&&(firstModus.dragged()))
		{
			NURBSShape draw = tempshape.clone();
			draw.scale(zoomfactor);
			g2.setStroke(new BasicStroke(1,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));
			g2.setColor(selColor);
			g2.draw(draw.getCurve(5d/(double)zoomfactor)); //draw only a preview
		}			
		if (actualMouseState==INTERPOLATION_MOUSEHANDLING)
		{
			Vector<Point2D> IP = firstModus.getShapeParameters().getPoints();
			Iterator<Point2D> iter = IP.iterator();
			while (iter.hasNext())
			{
				Point2D p = iter.next();
				Point p2 = new Point(Math.round((float)p.getX()),Math.round((float)p.getY()));
				this.drawCP(g2, p2, Color.BLUE);
			}
		}
		if (actualMouseState==CIRCLE_MOUSEHANDLING)
		{
			NURBSCreationMessage nm = firstModus.getShapeParameters();
			if ((!nm.isValid()) || (!firstModus.dragged())) //Draw these only when dragging
				return;
			Point2D p = nm.getPoints().firstElement();
			Point p2 = firstModus.getMouseOffSet(); //Actual Point in Graph
			if ((p!=null)&&(p2.x!=0)&&(p2.y!=0)) //Set per Drag
			{
				GeneralPath path = new GeneralPath();
				path.moveTo((new Double(p.getX())).floatValue()*zoomfactor,(new Double(p.getY())).floatValue()*zoomfactor);
				path.lineTo(p2.x,p2.y);
				g2.draw(path);			
			}
		}
	}

	private void paintModificationDetails(Graphics2D g2)
	{
		if (secondModus==null)
			return;
		if (!secondModus.dragged())
			return;
		g2.setColor(selColor);
		if ((actualMouseState&SHAPE) > 0) //ShapeModification always means to indicate the Drag with a line
		{
			Point2D p = secondModus.getDragStartPoint();
			Point2D p2 = secondModus.getDragPoint();
			if ((p!=null)&&(p2.getX()!=0d)&&(p2.getY()!=0d)) //Set per Drag
				g2.drawLine(Math.round((float)p.getX()*zoomfactor),Math.round((float)p.getY()*zoomfactor),
						Math.round((float)p2.getX()*zoomfactor), Math.round((float)p2.getY()*zoomfactor));
		}
		//All cases (Shape & CurvePointMod) draw tempshape
		NURBSShape tempshape = secondModus.getShape();
		if (tempshape!=null)
		{
			NURBSShape draw = tempshape.clone();
			draw.scale(zoomfactor);
			g2.setStroke(new BasicStroke(1,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));
			g2.draw(draw.getCurve(5d/(double)zoomfactor)); //draw only a preview
		}			
	}
	private void paintDEBUG(Graphics2D g2) //TODO: Remove Debug Controlpoint-polygon
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
		MouseEvent e = new MouseEvent(this,111,System.nanoTime(),0,-1,-1,1,false);		
		if (firstModus!=null)
			firstModus.mouseReleased(e);
		if (secondModus!=null)
			secondModus.mouseReleased(e);
		resetMouseHandling();
		switch (state) 
		{
			case CIRCLE_MOUSEHANDLING:
				firstModus = new CircleCreationHandler(this,highlightedHyperEdge);
				this.addMouseListener(firstModus);
				this.addMouseMotionListener(firstModus);
				actualMouseState = state;
			break;
			case INTERPOLATION_MOUSEHANDLING:
				firstModus = new InterpolationCreationHandler(this,highlightedHyperEdge);
				this.addMouseListener(firstModus);
				this.addMouseMotionListener(firstModus);
				actualMouseState = state;
			break;
			case CURVEPOINT_MOVEMENT_MOUSEHANDLING:
				secondModus = new FreeModificationHandler(this, highlightedHyperEdge);
				this.addMouseListener(secondModus);
				this.addMouseMotionListener(secondModus);
				actualMouseState = state;
			break;
			case SHAPE_ROTATE_MOUSEHANDLING:
				secondModus = new ShapeAffinTransformationHandler(this, highlightedHyperEdge);
				((ShapeAffinTransformationHandler)secondModus).setModificationState(ShapeAffinTransformationHandler.ROTATION);
				this.addMouseListener(secondModus);
				this.addMouseMotionListener(secondModus);
				actualMouseState = state;
			break;
			case SHAPE_TRANSLATE_MOUSEHANDLING:
				secondModus = new ShapeAffinTransformationHandler(this, highlightedHyperEdge);
				((ShapeAffinTransformationHandler)secondModus).setModificationState(ShapeAffinTransformationHandler.TRANSLATION);
				this.addMouseListener(secondModus);
				this.addMouseMotionListener(secondModus);
				actualMouseState = state;
			break;
			case SHAPE_SCALE_MOUSEHANDLING:
				secondModus = new ShapeAffinTransformationHandler(this, highlightedHyperEdge);
				((ShapeAffinTransformationHandler)secondModus).setModificationState(ShapeAffinTransformationHandler.SCALING);
				this.addMouseListener(secondModus);
				this.addMouseMotionListener(secondModus);
				actualMouseState = state;
			break;
			case SHAPE_SCALEDIR_MOUSEHANDLING:
				secondModus = new ShapeAffinTransformationHandler(this, highlightedHyperEdge);
				((ShapeAffinTransformationHandler)secondModus).setModificationState(ShapeAffinTransformationHandler.SCALE_ONEDIRECTION);
				this.addMouseListener(secondModus);
				this.addMouseMotionListener(secondModus);
				actualMouseState = state;
			break;
			case NO_MOUSEHANDLING:
			default:
				actualMouseState = NO_MOUSEHANDLING;
			break;
		}
		if (firstModus!=null) //Update Info in the Drag-Handler about the Grid.
		{
			firstModus.setGrid(gridy,gridy);
			firstModus.setGridOrientated(gridenabled&&gridorientated);
		}
		if (secondModus!=null) //Update Info in the Drag-Handler about the Grid.
		{
			secondModus.setGrid(gridy,gridy);
			secondModus.setGridOrientated(gridenabled&&gridorientated);
		}
		repaint();
	}
	/**
	 * Set the MouseHandling to NO_MOUSEHANDLING
	 */
	private void resetMouseHandling()
	{
		this.removeMouseListener(firstModus);
		this.removeMouseMotionListener(firstModus);
		this.removeMouseListener(secondModus);
		this.removeMouseMotionListener(secondModus);
		firstModus=null;
		secondModus=null;
	}
	public NURBSCreationMessage getShapeParameters()
	{
		if (firstModus!=null)
			return firstModus.getShapeParameters();
	else
		return null;		
	}
	/**
	 * Set the parameters for ShapeCreation in the first modus (e.g. when they where changed in the panel to the left)
	 * @param nm
	 */
	public void setShapeParameters(NURBSCreationMessage nm)
	{
		if (firstModus==null) //we are in second modus and must change to first because history or someone else just got us back to creation
		{
			//TODO: Change back to first modus? but how to update HyperEdgeShapePanel?
			return;
		}
		if (nm==null) //no suitable firstmodus available / determinable change to second
		{ //First Modus but no suitable Modus available keep shape but reset freeModus
			firstModus.resetShape();
//			vG.modifyHyperEdges.get(highlightedHyperEdge).setShape(new NURBSShape());			
			return;
		}
		//Is the actual firstmodus right for the Shape descriped in nm?
		if (!nm.isValid())
		{	firstModus.resetShape();
			firstModus.setShapeParameters(nm);
			vG.modifyHyperEdges.get(highlightedHyperEdge).setShape(new NURBSShape());		
			vG.pushNotify(new GraphMessage(GraphConstraints.HYPERGRAPH_ALL_ELEMENTS,GraphConstraints.HISTORY)); //Notify Panel
			return;
		}
		//Tests if we have to change modus...
		firstModus.setShapeParameters(nm);
		//if not change to fitting one
		repaint();
	}
	protected Point DragMouseOffSet()
	{
		if ((firstModus!=null)&&(firstModus.dragged()))
				return firstModus.getMouseOffSet();
		else if ((secondModus!=null)&&(secondModus.dragged()))
				return secondModus.getMouseOffSet();
		else
			return null;
	}
	public void update(Observable o, Object arg)
	{
		super.update(o, arg);
		if (arg instanceof GraphMessage)
		{
			GraphMessage m = (GraphMessage)arg;
 			if ((m.getModification()&GraphConstraints.HISTORY)>0)//We got an undo/redp
			{
 				if (secondModus!=null) //Simple, because we just reset the shape to that one from the graph - history updated that
 				{
 					secondModus.setShape(vG.modifyHyperEdges.get(highlightedHyperEdge).getShape());
 				}
 			}
 			else if ((firstModus!=null)&&(!firstModus.dragged())) //First Modus and we have no drag
			{
				if (((m.getModification()&GraphConstraints.BLOCK_END)==GraphConstraints.BLOCK_END)) //Drag just ended -> Set Circle as Shape
				{
					NURBSCreationMessage nm = firstModus.getShapeParameters();
					vG.modifyHyperEdges.get(highlightedHyperEdge).setShape(firstModus.getShape());
					firstModus.resetShape();
				}
			}
 			else if ((secondModus!=null)&&(!secondModus.dragged())) //Second Modus and we have no drag
			{
				if (((m.getModification()&GraphConstraints.BLOCK_END)==GraphConstraints.BLOCK_END)) //Drag just ended -> Set Circle as Shape
				{
					vG.modifyHyperEdges.get(highlightedHyperEdge).setShape(secondModus.getShape());
					vG.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE, highlightedHyperEdge, GraphConstraints.UPDATE|GraphConstraints.HYPEREDGESHAPE, GraphConstraints.HYPEREDGE)); //HyperEdgeShape Updated
					secondModus.resetShape();
				}
			}
 			repaint();
		}
	}
}