package model;

import view.VHyperGraphic;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Map.Entry;

/**
 * The NURBSSHapeValidator determines, whether a given NURBSShape C
 * is a valid shape for a specified Hyperedge e in an visual HypgerGraph 
 * 
 * That is the case if and only if the following three conclusions hold
 * - All Nodepositions p_i of nodes belonging to e are inside the NURBSSHape
 * - their distance to the Shape is at most distance+their radius, for each v_i in e
 * - All Nodepositions p_i of nodes not belonging to e are outside the NURBSShape
 *
 * @author Ronny Bergmann
 * @since 0.4 
 */
public class NURBSShapeValidator extends NURBSShape {

	public NURBSShapeValidator(VHyperGraph vG, int HyperEdgeIndex, NURBSShape Curve)
	{
		VHyperEdge e = vG.modifyHyperEdges.get(HyperEdgeIndex);
		if (e==null)
			return;
		NURBSShape clone = new NURBSShape();
		if (Curve!=null)
			clone = Curve.clone();
		else
			clone = e.getShape();
		if (clone.isEmpty())
			return;
		setCurveTo(clone.Knots, clone.controlPoints, clone.cpWeight);
		//
		// Start of Validation-Algorithm
		//
		Point2D HighestCP= new Point2D.Double(Double.MAX_VALUE,Double.MAX_VALUE);
		for (int i=0; i<controlPoints.size(); i++)
		{
			Point2D actual = controlPoints.get(i);
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
		for (int i=0; i<maxCPIndex-degree; i++)
		{
			Points.offer(controlPoints.get(i));
			OldSet.put(controlPoints.get(i), base+i+1);
		}
		//Number of Projections made, 
		int i=0;
		//MaxSize of any circle used, because a circle with this radius is much bigger than the whole graph
		//We need something to mearue font-size so...
		Graphics2D g2 = (Graphics2D)(new VHyperGraphic(new Dimension(0,0),vG)).getGraphics();
		int maxSize = Math.max( Math.round((float)(vG.getMaxPoint(g2).getX()-vG.getMinPoint(g2).getX())),
								Math.round((float)(vG.getMaxPoint(g2).getY()-vG.getMinPoint(g2).getY())) );
		//Check each Intervall, whether we are done
		int checkInterval = Points.size();
		boolean valid=true, running=true;
		while (!Points.isEmpty()&&running) 
		{
			Point2D actualP = Points.poll();
			NURBSShapeProjection proj = new NURBSShapeProjection(this,actualP);
			Point2D ProjP = proj.getResultPoint(); //This Point belong definetly to the same set as actualP but lies on the Curve
			double radius= ProjP.distance(actualP)-(double)e.getWidth()/2d;
			if (radius<maxSize)
			{	OldRadii.put(actualP,radius);
//				g2.setColor(Color.gray);
//				g2.drawOval(Math.round((float)(actualP.getX()-radius)*zoomfactor),
//					Math.round((float)(actualP.getY()-radius)*zoomfactor),
//					Math.round((float)(2*radius)*zoomfactor), Math.round((float)(2*radius)*zoomfactor));
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
//				drawCP(g2,new Point(Math.round((float)newP.getX()/zoomfactor),Math.round((float)newP.getY()/zoomfactor)),Color.BLUE); //Draw as handled
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
						System.err.print(id);
						if (vG.getMathGraph().modifyHyperEdges.get(e.getIndex()).containsNode(id))
						{
							if (inSet==-1) //not set yet This nodes set must be the one for VH
								inSet = OldSet.get(pos); 
							else if (OldSet.get(pos)==outSet) //node of Hyperedge outside
							{
								valid=false; running=false; System.err.println("Node #"+id+" outside shape but in Edge");
							}
						}
						else //Outside
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
						System.err.println("\n#"+i+" InSet="+inSet+" OutSet="+outSet+" valid="+valid+" running="+running+" twosets="+twosets);
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
}
