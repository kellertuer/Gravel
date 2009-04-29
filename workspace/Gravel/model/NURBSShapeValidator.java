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
import java.util.Vector;
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
 *
 * The Result is as follows:
 * - If the Result is valid, alls Sets of wrong nodes are empty
 * 
 * - If something went wrong in the initialization, the Set of wrong nodes is empty, but valid is also wrong
 * 
 * - If the shape is not valid, the result is false and the set of wrong nodes conatins all nodes that are either
 *     - inside the shape but not in the Hyper edge
 *     - outside the shape but inside the Hyper Edge
 *   
 * @author Ronny Bergmann
 * @since 0.4 
 */
public class NURBSShapeValidator extends NURBSShape {

	private Point2D CPOutside;
	private Queue<Point2D> Points = new LinkedList<Point2D>();
	//Function that assigns every node a setnumber - which will be in the beginning its index and if
	//the setnumber of the node position changes - this one is also updated
	private HashMap<Integer,Integer> node2Set = new HashMap<Integer,Integer>();
	//Function that assigns every nodeposition the associated node index 
	private HashMap<Point2D,Integer> NodePos2Index = new HashMap<Point2D,Integer>();
	//Radii that are aleady computed, used for proofing intersections between circles without computing them again (less projections)
	private HashMap<Point2D,Double> point2Radius = new HashMap<Point2D,Double>();
	//Function that gives every handled point a setnumber (so it belongs to set number x)
	private HashMap<Point2D,Integer> point2Set = new HashMap<Point2D,Integer>();

	private Vector<Integer> invalidNodeIndices  = new Vector<Integer>();
	private boolean ResultValidation;
	
	public NURBSShapeValidator(VHyperGraph vG, int HyperEdgeIndex, NURBSShape Curve)
	{
		ResultValidation=false;
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
		//Now also the curve and the Hyperedge are correct for the check
		ResultValidation=true;
		setCurveTo(clone.Knots, clone.controlPoints, clone.cpWeight);
		//
		// Start of Validation-Algorithm
		//
		CPOutside= new Point2D.Double(Double.MAX_VALUE,Double.MAX_VALUE);		
		for (int i=0; i<controlPoints.size(); i++)
		{
			Point2D actual = controlPoints.get(i);
			if (actual.getY()<CPOutside.getY())
				CPOutside = (Point2D)actual.clone();	
		}
		
		initSets(vG);
		
		//Number of Projections made, 
		int i=0;
		//MaxSize of any circle used, because a circle with this radius is much bigger than the whole graph
		//We need something to mearue font-size so...
		Graphics2D g2 = (Graphics2D)(new VHyperGraphic(new Dimension(0,0),vG)).getGraphics();
		int maxSize = Math.max( Math.round((float)(vG.getMaxPoint(g2).getX()-vG.getMinPoint(g2).getX())),
								Math.round((float)(vG.getMaxPoint(g2).getY()-vG.getMinPoint(g2).getY())) );
		//Check each Intervall, whether we are done
		int checkInterval = Points.size();
		
		boolean running=true;
		
		while (!Points.isEmpty()&&running) 
		{
			Point2D actualP = Points.poll();
			NURBSShapeProjection proj = new NURBSShapeProjection(this,actualP);
			Point2D ProjP = proj.getResultPoint(); //This Point belong definetly to the same set as actualP but lies on the Curve
			double radius= ProjP.distance(actualP)-(double)e.getWidth()/2d;
			if (radius<maxSize)
			{	
				point2Radius.put(actualP,radius);
//				g2.setColor(Color.gray);
//				g2.drawOval(Math.round((float)(actualP.getX()-radius)*zoomfactor),
//					Math.round((float)(actualP.getY()-radius)*zoomfactor),
//					Math.round((float)(2*radius)*zoomfactor), Math.round((float)(2*radius)*zoomfactor));
				//Calculate Distance and direction from Point to its projection
				Point2D ProjDir = new Point2D.Double(ProjP.getX()-actualP.getX(),ProjP.getY()-actualP.getY());
				//and shorten this by 
				ProjDir = new Point2D.Double(radius/ProjP.distance(actualP)*ProjDir.getX(),radius/ProjP.distance(actualP)*ProjDir.getY());
				//Check whether other Old Points interfere with this one
				Iterator<Entry<Point2D,Double>> RadiusIterator = point2Radius.entrySet().iterator();
				while (RadiusIterator.hasNext()) //Iterate all old Points
				{
					Entry<Point2D,Double> actEntry = RadiusIterator.next();
					//If the distance of the actualPoint to this is smaller that the sum of both radii - both are in the same set
					if (actEntry.getKey().distance(actualP)<(actEntry.getValue()+radius))
					{
						int sameset = Math.min(point2Set.get(actEntry.getKey()),point2Set.get(actualP)); //Set we set them both to
						int changeset = Math.max(point2Set.get(actEntry.getKey()),point2Set.get(actualP)); //Set that should be changed
						if (changeset!=sameset) //not in the same set yet -> Union of both sets in the minimum (sameset)
							UnionSets(point2Set, changeset, sameset);
					}
				}
				//Calculate a new Point for the set (TODO: the other two new points in 90 and 270 Degree or another better Choice?)
				Point2D newP = new Point2D.Double(actualP.getX()-ProjDir.getX(),actualP.getY()-ProjDir.getY());
//				drawCP(g2,new Point(Math.round((float)newP.getX()/zoomfactor),Math.round((float)newP.getY()/zoomfactor)),Color.BLUE); //Draw as handled
				Points.offer(newP);
				point2Set.put(newP,point2Set.get(actualP)); //New value is in the same set as actualP
				i++;
				if ((i%checkInterval)==0)
				{
					CheckSet(vG, HyperEdgeIndex);
					//If either ResultValid=true Wrong.size()==0 we're ready because the shape is valid
					//If ResultValid=false and Wrong.size()>0 we're ready because the shape is invalid
					running = ! (  (ResultValidation&&(invalidNodeIndices.size()==0)) || (!ResultValidation&&(invalidNodeIndices.size()>0)) );
					if ((invalidNodeIndices.size()==0)&&(ResultValidation==false)) //Not yet two sets
						ResultValidation=true;
				}
			}	
		}
		if (!ResultValidation)
			System.err.print("IN");
		System.err.println("VALID");
		for (int j=0; j<this.invalidNodeIndices.size(); j++)
		{
				System.err.print("#"+invalidNodeIndices.get(j));
		}
		System.err.println("\n");
	}
	
	/**
	 * Returns whether the input was valid, if it was not valid, no Check was done
	 * @return
	 */
	public boolean isInputValid()
	{
		return !(!ResultValidation&&(invalidNodeIndices.size()==0));
	}
	
	/**
	 * Main result of the Algorithm
	 * (if it was persormed @see isInputValid())
	 * 
	 * @return true is the shape is valid, else false (else also includes invalid input)
	 */
	public boolean isShapeValid()
	{
		return ResultValidation;
	}
	
	/**
	 * Get the indices that are wrong.
	 * This set includes all Node-indices, that are
	 * - Inside the shape but not in the hyper edge
	 * - Outside the shape but inside the hyper edge
	 * 
	 * If the Shape is valid or the input is invalid - the Vector is empty
	 * @return Vector containing all node indices, that represent invalid nodes
	 */
	public Vector<Integer> getInvalidNodeIndices()
	{
		return invalidNodeIndices;
	}
	/**
	 * 
	 * Union the two sets specified by max and min in the Set min
	 * In the Setfunction Specified by the first parameter
	 * 
	 * If the Point is also a Node, update the Set the node belongs to, too.
	 * 
	 * @param SetFunction
	 * @param max
	 * @param min
	 */
	private void UnionSets(HashMap<Point2D,Integer> SetFunction, int max, int min)
	{
		Iterator<Entry<Point2D,Integer>> it = SetFunction.entrySet().iterator();
		while (it.hasNext())
		{
			Entry<Point2D,Integer> checkEntry = it.next();
			if (checkEntry.getValue().intValue()==max)
			{
				checkEntry.setValue(min);
				if (NodePos2Index.containsKey(checkEntry.getKey()))
					node2Set.put(NodePos2Index.get(checkEntry.getKey()).intValue(),min);
			}
		}
	}
	private void initSets(VHyperGraph vG)
	{
		Points = new LinkedList<Point2D>();
		Iterator<VNode> vti = vG.modifyNodes.getIterator();
		while (vti.hasNext())
		{
			VNode n = vti.next();
			Point p = n.getPosition();
			Point2D p2 = new Point2D.Double(p.getX(),p.getY());
			Points.offer(p2);
			node2Set.put(n.getIndex(),n.getIndex()); //Ste into its own Set
			NodePos2Index.put(p2,n.getIndex());
			point2Set.put(p2,n.getIndex());
		}
		int base = vG.getMathGraph().modifyNodes.getNextIndex();
		for (int i=0; i<maxCPIndex-degree; i++)
		{
			Points.offer(controlPoints.get(i));
			point2Set.put(controlPoints.get(i), base+i+1);
		}

	}
	
	/**
	 * Check the actual sets of NodePositions whether they are legal or not and
	 * whether there are only two sets left
	 * 
	 * Results are saved in the classglobal variables ResultValid and Wrong-Integer-set, because this method is just for 
	 * better structure
	 * 
	 * @param vG
	 * @param HEIndex
	 */
	private void CheckSet(VHyperGraph vG, int HEIndex)
	{
		int inSet=-1, outSet = point2Set.get(CPOutside).intValue();
		Iterator<MNode> nit = vG.getMathGraph().modifyNodes.getIterator();
		while (nit.hasNext()||(inSet==-1)) //Find Set of any node inside Hyper Edge - if we're ready - this should be the only one inside
		{
			int nodeid = nit.next().index;
			if (vG.getMathGraph().modifyHyperEdges.get(HEIndex).containsNode(nodeid))
			{
				Point p = vG.modifyNodes.get(nodeid).getPosition();
				Point2D p2 = new Point2D.Double(p.getX(),p.getY());
				inSet = point2Set.get(p2);
			}
		}
		
		Iterator<Point2D> nodeiter = NodePos2Index.keySet().iterator();
		boolean twosets=true;
		while (nodeiter.hasNext())
		{
			Point2D pos = nodeiter.next();
			int id = NodePos2Index.get(pos);
			if (vG.getMathGraph().modifyHyperEdges.get(HEIndex).containsNode(id))
			{
				if (point2Set.get(pos)==outSet) //node of Hyperedge outside
				{
					invalidNodeIndices.add(id);
					ResultValidation = false;
					System.err.println("Node #"+id+" outside shape but in Edge");
				}
			}
			else //Outside
			{
				if (inSet==point2Set.get(pos)) //Another node not from edge is inside
				{
					invalidNodeIndices.add(id);
					ResultValidation=false;
					System.err.println("Node #"+id+" inside but not in Edge!");
				}
			}
			if ((inSet!=point2Set.get(pos))&&(outSet!=point2Set.get(pos)))
			{
				twosets=false; //More than two sets
				break;
			}
		}
		
		if (!twosets) //We'Re not ready yet
		{
			invalidNodeIndices.clear(); ResultValidation=false;
		}
	}
}
