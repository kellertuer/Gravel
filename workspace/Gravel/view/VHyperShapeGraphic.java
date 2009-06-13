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
	private NURBSCreationMessage noModus=null;
	private ShapeModificationMouseHandler secondModus;
	private static final long serialVersionUID = 1L;
	private int actualMouseState, highlightedHyperEdge;
	private Vector<Integer> invalidNodesforShape;
	
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
		invalidNodesforShape = new Vector<Integer>();
		vGh = new HyperEdgeShapeHistoryManager(this,hyperedgeindex);
	}	

	public void paint(Graphics2D g2)
	{
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		paintgrid(g2);
		displaySelection(g2);
		paintMouseModeDetails(g2);
		paintHyperEdges(g2);
		paintNodes(g2);
		if ((firstModus!=null)&&(firstModus.getSelectionRectangle()!=null))
		{
			g2.setColor(selColor);
			g2.setStroke(new BasicStroke(1,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));
			g2.draw(firstModus.getSelectionRectangle());
		}
	//	paintDEBUG(g2);
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
			if (!temp.getShape().isEmpty())
			{
				if (temp.getIndex()==highlightedHyperEdge)
				{
					g2.setColor(temp.getColor());
				}
				else
					g2.setColor(this.selColor.brighter());
				g2.setStroke(new BasicStroke(temp.getWidth()*zoomfactor,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));

				NURBSShape s = temp.getShape().stripDecorations().clone(); //really only nurbs
				s.scale(zoomfactor);
				g2.draw(temp.getLinestyle().modifyPath(s.getCurve(5d/(double)zoomfactor),temp.getWidth(),zoomfactor));
			}
		}
	}
	private void displaySelection(Graphics2D g2)
	{
		NURBSShape shape = vG.modifyHyperEdges.get(highlightedHyperEdge).getShape();
		if ((shape.getDecorationTypes()&NURBSShape.FRAGMENT)!=NURBSShape.FRAGMENT)
			return;
		NURBSShapeFragment s = (NURBSShapeFragment) shape;
		if (s==null)
			return;
		float selSize = (float)selWidth/2f + (float) vG.modifyHyperEdges.get(highlightedHyperEdge).getWidth();
		NURBSShape drawSel = s.getSubCurve().clone().stripDecorations(); //really only nurbs
		drawSel.scale(zoomfactor);
		g2.setColor(selColor);
		g2.setStroke(new BasicStroke(selSize*zoomfactor,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));
		g2.draw(drawSel.getCurve(5d/(double)zoomfactor)); //draw only a preview				
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
				g2.setColor(this.selColor.brighter());
			if (invalidNodesforShape.contains(temp.getIndex()))
				g2.setColor(Color.red.darker());
			g2.fillOval(Math.round(temp.getdrawpoint().x*zoomfactor), Math.round(temp.getdrawpoint().y*zoomfactor), Math.round(temp.getSize()*zoomfactor), Math.round(temp.getSize()*zoomfactor));
			if (temp.isNameVisible())
			{	
				if (hEdge.containsNode(temp.getIndex()))
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
			NURBSShape draw = tempshape.stripDecorations().clone(); //really only NURBS
			draw.scale(zoomfactor);
			g2.setStroke(new BasicStroke(1*zoomfactor,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));
			g2.setColor(selColor);
			g2.draw(draw.getCurve(5d/(double)zoomfactor)); //draw only a preview
		}			
		if (actualMouseState==INTERPOLATION_MOUSEHANDLING)
		{
			NURBSCreationMessage nm = firstModus.getShapeParameters();
			Vector<Point2D> IP = nm.getPoints();
			Iterator<Point2D> iter = IP.iterator();
			while (iter.hasNext())
			{
				Point2D p = iter.next();
				Point p2 = new Point(Math.round((float)p.getX()),Math.round((float)p.getY()));
				this.drawCP(g2, p2, Color.BLUE);
			}
			if ((nm.getCurve().getDecorationTypes()&NURBSShape.FRAGMENT)==NURBSShape.FRAGMENT)
			{
				NURBSShape draw = ((NURBSShapeFragment)nm.getCurve()).getSubCurve().stripDecorations().clone();
				draw.scale(zoomfactor);
				g2.setStroke(new BasicStroke(1*zoomfactor,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));
				Color nsel = new Color((3*selColor.getBlue()+Color.DARK_GRAY.getBlue())/4,
						(3*selColor.getBlue()+Color.DARK_GRAY.getBlue())/4,
						(3*selColor.getBlue()+Color.DARK_GRAY.getBlue())/4);
				g2.setColor(nsel);
				g2.draw(draw.getCurve(5d/(double)zoomfactor)); //draw only a preview
			}
			
		}
		else if (actualMouseState==CIRCLE_MOUSEHANDLING)
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
		if (((actualMouseState&KNOT_MODIFICATION_MOUSEHANDLING)>0)&&(!vG.modifyHyperEdges.get(highlightedHyperEdge).getShape().isEmpty()))
		{
			g2.setColor(selColor);
			int size = (selWidth+vG.modifyHyperEdges.get(highlightedHyperEdge).getWidth())/2;
			Vector<Point2D> kP = vG.modifyHyperEdges.get(highlightedHyperEdge).getShape().getKnotPoints();
			for (int i=0; i<kP.size(); i++)
			{
				g2.fillOval(Math.round(((float)kP.get(i).getX()-(float)size)*zoomfactor), Math.round(((float)kP.get(i).getY()-(float)size)*zoomfactor), Math.round(2*size*zoomfactor), Math.round(2*size*zoomfactor));
			}
		}
		if (secondModus==null)
			return;
		//All cases (Shape & CurvePointMod) draw tempshape
		NURBSShape tempshape = secondModus.getShape();
		if ((tempshape==null)||(tempshape.isEmpty()))
				return;
		if ((actualMouseState&SUBCURVE_MOUSEHANDLING) > 0)
		{ //Draw CP always in that modus
			NURBSShapeFragment t = (NURBSShapeFragment)tempshape;
			if (!Double.isNaN(t.getStart()))
			{
				Point2D Start = tempshape.CurveAt(t.getStart());
				Point p2 = new Point(Math.round((float)Start.getX()),Math.round((float)Start.getY()));
				this.drawCP(g2, p2, selColor.darker());
			}
			if (!Double.isNaN(t.getEnd()))
			{
				Point2D End = tempshape.CurveAt(t.getEnd());
				Point p2 = new Point(Math.round((float)End.getX()),Math.round((float)End.getY()));
				this.drawCP(g2, p2, selColor.darker());
			}
			//TODO Remove DEBUG
			Point2D StartCurve = tempshape.CurveAt(tempshape.Knots.get(tempshape.getDegree()));
			Point p2 = new Point(Math.round((float)StartCurve.getX()),Math.round((float)StartCurve.getY()));
			this.drawCP(g2, p2, Color.GREEN);
		}
		if (!secondModus.dragged())
			return; //Draw all other stuff only while drag
		g2.setColor(selColor);
		g2.setStroke(new BasicStroke(1,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));
		if ((actualMouseState&SHAPE_MOUSEHANDLING) > 0) //ShapeModification always means to indicate the Drag with a line
		{
			Point2D p = secondModus.getDragStartPoint();
			Point2D p2 = secondModus.getDragPoint();
			if ((p!=null)&&(p2.getX()!=0d)&&(p2.getY()!=0d)) //Set per Drag
				g2.drawLine(Math.round((float)p.getX()*zoomfactor),Math.round((float)p.getY()*zoomfactor),
						Math.round((float)p2.getX()*zoomfactor), Math.round((float)p2.getY()*zoomfactor));
		}
		if ((actualMouseState&SUBCURVE_MOUSEHANDLING) > 0)
		{ //This modus always delivers the shape including an subcurve, though this might be an empty shape
			NURBSShape drawSel = ((NURBSShapeFragment)tempshape).getSubCurve().clone(); //Is only a NURBS
			drawSel.scale(zoomfactor);
			g2.setColor(selColor.darker());
			g2.setStroke(new BasicStroke(((float)selWidth/2f+(float)vG.modifyHyperEdges.get(highlightedHyperEdge).getWidth())*zoomfactor,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));
			g2.draw(drawSel.getCurve(5d/(double)zoomfactor)); //draw only a preview				
		}
		else
		{
			NURBSShape draw = tempshape.stripDecorations().clone(); //Just Curve itself
			draw.scale(zoomfactor);
			g2.setStroke(new BasicStroke(1,BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));
			g2.draw(draw.getCurve(5d/(double)zoomfactor)); //draw only a preview
		}
	}
	private void paintDEBUG(Graphics2D g2)
	{
		g2.setColor(Color.orange.darker());
		NURBSShape s =  vG.modifyHyperEdges.get(highlightedHyperEdge).getShape().stripDecorations().clone();
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
		boolean listenerclasschanged = (state&(~DETAIL_MASK)) != (actualMouseState&(~DETAIL_MASK));
		if (listenerclasschanged)
			resetMouseHandling(); //remove old listeners
		switch (state&(~DETAIL_MASK)) //State without Detail 
		{
			case CIRCLE_MOUSEHANDLING:
				if (listenerclasschanged)
					firstModus = new CircleCreationHandler(this,highlightedHyperEdge);
			break;
			case INTERPOLATION_MOUSEHANDLING:
				if (listenerclasschanged)
					firstModus = new InterpolationCreationHandler(this,highlightedHyperEdge);
			break;
			case CURVEPOINT_MOVEMENT_MOUSEHANDLING:
				if (listenerclasschanged)
					secondModus = new FreeModificationHandler(vG, highlightedHyperEdge);
			break;
			case SHAPE_MOUSEHANDLING:
				if (listenerclasschanged)
					secondModus = new ShapeAffinTransformationHandler(state&(DETAIL_MASK),vG, highlightedHyperEdge);
				else //just update State
					((ShapeAffinTransformationHandler)secondModus).setModificationState(state&(DETAIL_MASK));
			break;
			case SUBCURVE_MOUSEHANDLING:
				if (listenerclasschanged) //If we are not yet in that modus
					secondModus = new ShapeSubcurveSelectionHandler(state&(DETAIL_MASK),vG, highlightedHyperEdge);
				else //just update State
					((ShapeSubcurveSelectionHandler)secondModus).setModificationState(state&(DETAIL_MASK));
			break;
			case KNOT_MODIFICATION_MOUSEHANDLING:
				if (listenerclasschanged)
					secondModus = new KnotModificationMouseHandler(state&DETAIL_MASK, vG, highlightedHyperEdge); //TODO
				else //Just update State
					((KnotModificationMouseHandler)secondModus).setModificationState(state&(DETAIL_MASK));
			break;
			case NO_MOUSEHANDLING:
			default:
				resetMouseHandling();
				actualMouseState = NO_MOUSEHANDLING;
				return;
		}
		actualMouseState = state;				
		if ((firstModus!=null)&&(listenerclasschanged)) //Update Info in the Drag-Handler about the Grid - add actionlistener again
		{
			this.addMouseListener(firstModus);
			this.addMouseMotionListener(firstModus);
			firstModus.setGrid(gridy,gridy);
			firstModus.setGridOrientated(gridenabled&&gridorientated);
		}
		if ((secondModus!=null)&&(listenerclasschanged)) //Update Info in the Drag-Handler about the Grid - only if it is really new
		{
			this.addMouseListener(secondModus);
			this.addMouseMotionListener(secondModus);
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
		noModus=null;
	}
	/**
	 * get actial ShapeParameters that are used for computation of the shape
	 * These Values are valid in every Modus of Creation
	 * These Values are null in every Modus of Modification
	 * @return
	 */
	public NURBSCreationMessage getShapeParameters()
	{
		if (firstModus!=null)
			return firstModus.getShapeParameters();
		else if (getMouseHandling()==NO_MOUSEHANDLING) //without mousehandling this value is stored here
			return noModus;
	else
		return null;		
	}
	/**
	 * Set the parameters for ShapeCreation in the first modus 
	 * (e.g. when they where changed in the panel to the left)
	 * 
	 * If the Message is valid the MouseHandler is set to the specified Type and the Parameters in the MouseHandler are updated
	 * If the Message is invalid the MouseHandler is set to the standard-value of second modus (modification)
	 * If the Message is null, the MouseHandler is set to first modus or left as is (if already in first modus) and all values in the Handler are cleared
	 * 
	 * @param nm
	 */
	public void setShapeParameters(NURBSCreationMessage nm)
	{
		if (secondModus!=null)
		{
			if ((nm==null) || (!nm.isValid())) //If we are in second modus and we get no Message or an invalid one, we stay in this modus
				return;
			//Every else case we change to a first modus case few lines below with updateMouseHandling
		}
		else if ((nm!=null)&&(!nm.isValid())) //Creation Modus and invalid message means we change to modification
		{
			setMouseHandling(CURVEPOINT_MOVEMENT_MOUSEHANDLING);
			vG.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGESHAPE,GraphConstraints.HISTORY)); //Notify Panel
			return;
		}
		int prevMouseHandling = getMouseHandling();
		//So now it is existent and valid, update to correct modus and notify panel
		updateMouseHandling(nm); //Update to correct modus
		if (getMouseHandling()==NO_MOUSEHANDLING)
			noModus=nm;
		else //so here firstmodus is not null
			firstModus.setShapeParameters(nm); //Update new Modus' values
		if (nm!=null) //Set to new shape
			vG.modifyHyperEdges.get(highlightedHyperEdge).setShape(NURBSShapeFactory.CreateShape(nm));		
		else //clear
			vG.modifyHyperEdges.get(highlightedHyperEdge).setShape(new NURBSShape());
		//Notify all despite History (e.g. for update of Panel) if we changed CreationModus
		if ((getMouseHandling()!=prevMouseHandling)||(nm==null)) //Either MOuse Modus Change or Parameter Reset
			vG.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGESHAPE,GraphConstraints.HISTORY)); //Notify Panel
		repaint();
	}
	/**
	 * Internal update of Mousehandling depending on an NURBSCreationmessage.
	 * The NURBSCreationmessage contains
	 * - either an modus for the creation modus, than that modus is set
	 * - or the message is inValid or null, than no change is done
	 * 
	 * @param nm
	 */
	private void updateMouseHandling(NURBSCreationMessage nm)
	{
		if ((nm==null)||(!nm.isValid()))
			return;
		switch (nm.getType())
		{
			case NURBSCreationMessage.INTERPOLATION:
				if (getMouseHandling()!=INTERPOLATION_MOUSEHANDLING) //really change
				{
					setMouseHandling(INTERPOLATION_MOUSEHANDLING);
				}
				break;			
			case NURBSCreationMessage.CIRCLE:
			if (getMouseHandling()!=CIRCLE_MOUSEHANDLING)
				setMouseHandling(CIRCLE_MOUSEHANDLING);
			break;
			case NURBSCreationMessage.CONVEX_HULL:
				if (getMouseHandling()!=NO_MOUSEHANDLING)
					setMouseHandling(NO_MOUSEHANDLING);
			break;
		}
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
 					secondModus.resetShape();
 				}
 			}
 			else if ((firstModus!=null)&&(!firstModus.dragged())) //First Modus and we have no drag
			{
				if (((m.getModification()&GraphConstraints.BLOCK_END)==GraphConstraints.BLOCK_END)) //Drag just ended -> Set Circle as Shape
				{
					vG.modifyHyperEdges.get(highlightedHyperEdge).setShape(firstModus.getShape());
					firstModus.resetShape();
				}
			}
 			else if ((secondModus!=null)&&(!secondModus.dragged())) //Second Modus and we have no drag
			{
				if (((m.getModification()&GraphConstraints.BLOCK_END)==GraphConstraints.BLOCK_END)) //Drag just ended -> Set Circle as Shape
				{
					NURBSShape s = secondModus.getShape();
					if ((s.getDecorationTypes()&NURBSShape.FRAGMENT)==NURBSShape.FRAGMENT)
						((NURBSShapeFragment)s).refreshDecoration();

					vG.modifyHyperEdges.get(highlightedHyperEdge).setShape(s);
					vG.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE, highlightedHyperEdge, GraphConstraints.UPDATE|GraphConstraints.HYPEREDGESHAPE, GraphConstraints.HYPEREDGE)); //HyperEdgeShape Updated
					secondModus.resetShape();
				}
			}
 			repaint();
		}
	}

	public void setHighlightedNodes(Vector<Integer> nodes) {
		invalidNodesforShape = nodes;
	}
}