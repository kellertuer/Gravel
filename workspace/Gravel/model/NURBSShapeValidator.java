package model;

import view.VHyperGraphic;

import java.awt.Color;
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
			clone = e.getShape().clone();
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
		
		initPointSets(vG);
		
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
					System.err.print(i+"   -  ");
					CheckSetWeak(vG, HyperEdgeIndex);
					Iterator<Point2D> nodeiter = NodePos2Index.keySet().iterator();
					while (nodeiter.hasNext()) //Iterator for all node-positions
					{
						Point2D pos = nodeiter.next();
						int id = NodePos2Index.get(pos);
						System.err.print(id+"in"+node2Set.get(id)+"  ");
					}
					System.err.println("--- Definetly utside-Set "+point2Set.get(CPOutside));
					//If either ResultValid=true Wrong.size()==0 we're ready because the shape is valid
					//If ResultValid=false and Wrong.size()>0 we're ready because the shape is invalid
					running = ! (  (ResultValidation&&(invalidNodeIndices.size()==0)) || (!ResultValidation&&(invalidNodeIndices.size()>0)) );
					if ((invalidNodeIndices.size()==0)&&(ResultValidation==false)) //Not yet two sets
						ResultValidation=true;
				}
			}	
		}
		if (ResultValidation) //Nodes are valid due to inside or outside
			checkDistances(vG,HyperEdgeIndex);
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
	
	private void initPointSets(VHyperGraph vG)
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
	 * Find a successor for the point p depending upon its precessor pre
	 * and the nurbs Curve c (its projection and secod derivative)
	 * @param p
	 * @param pre
	 * @param c
	 * @return
	 */
	public static Vector<Point2D> findSuccessor(Point2D p, Point2D pre, NURBSShape c,Graphics2D Debug,float z)
	{
		NURBSShapeProjection proj = new NURBSShapeProjection(c,p);
		Point2D hatp = proj.getResultPoint(); //This Point belong definetly to the same set as actualP but lies on the Curve
		double hatt = proj.getResultParameter();
		double radius= hatp.distance(p)-1d; //1 linewidth
//		System.err.print("Circle Radius: "+radius);

		Debug.setColor(Color.gray);
		Debug.drawOval(Math.round((float)(p.getX()-radius)*z),
			Math.round((float)(p.getY()-radius)*z),
			Math.round((float)(2*radius)*z), Math.round((float)(2*radius)*z));
		//Calculate Distance and direction from Point to its projection
		Point2D ProjDir = new Point2D.Double(hatp.getX()-p.getX(),hatp.getY()-p.getY());
		//and shorten this by 
		ProjDir = new Point2D.Double(radius/hatp.distance(p)*ProjDir.getX(),radius/hatp.distance(p)*ProjDir.getY());
		Debug.drawLine(Math.round((float)p.getX()*z), Math.round((float)p.getY()*z),
				Math.round((float)(p.getX()+ProjDir.getX())*z),
				Math.round((float)(p.getY()+ProjDir.getY())*z));
		
		Point2D deriv1 = c.DerivateCurveAt(1,hatt);
		Point2D deriv2 = c.DerivateCurveAt(2,hatt);
		double d1 = deriv1.distance(0d,0d);
		double kappa = Math.abs((deriv1.getX()*deriv2.getX() + deriv1.getY()*deriv2.getY()) / d1*d1*d1);
		kappa = kappa/5000000;
		kappa /=10000;
		System.err.println("  - at "+hatt+" we have Abs2 "+kappa+" that is "+((Math.atan(kappa)+(Math.PI/2))/Math.PI)*180+" Degree");
		double varphi = Math.atan(kappa)+(Math.PI/2); 
		double newx = ProjDir.getX()* Math.cos(varphi) +  ProjDir.getY()*Math.sin(varphi);
		double newy = -ProjDir.getX()*Math.sin(varphi) + ProjDir.getY()*Math.cos(varphi);
		Point2D newp = new Point2D.Double(newx,newy);
		Debug.setColor(Color.orange);
		Debug.drawLine(Math.round((float)p.getX()*z), Math.round((float)p.getY()*z),
				Math.round((float)(p.getX()+newp.getX())*z),
				Math.round((float)(p.getY()+newp.getY())*z));
		newp = new Point2D.Double(p.getX()+newp.getX(),p.getY()+newp.getY()); 
		Debug.drawLine(Math.round((float)(newp.getX()-4)*z),Math.round((float)newp.getY()*z),Math.round((float)(newp.getX()+4)*z),Math.round((float)newp.getY()*z));
		Debug.drawLine(Math.round((float)newp.getX()*z),Math.round((float)(newp.getY()-4)*z),Math.round((float)newp.getX()*z),Math.round((float)(newp.getY()+4)*z));

		return null;
	}
	/**
	 * Check the actual sets of NodePositions whether they are legal or not and
	 * whether there are only two sets left
	 * 
	 * Results are saved in the classglobal variables ResultValid and Wrong-Integer-set, because this method is just for 
	 * better structure
	 * 
	 * This method does not terminate for curves where the internal nodes are nearly isolated so that there are no circles 
	 * that interconnect the internal regions which never results in twosets to be true
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
//					System.err.println("Node #"+id+" outside shape but in Edge");
				}
			}
			else //Outside
			{
				if (inSet==point2Set.get(pos)) //Another node not from edge is inside
				{
					invalidNodeIndices.add(id);
					ResultValidation=false;
//					System.err.println("Node #"+id+" inside but not in Edge!");
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
	/**
	 * Check the actual sets of NodePositions whether they are legal or not and
	 * whether there are only two sets left
	 * 
	 * Results are saved in the classglobal variables ResultValid and Wrong-Integer-set, because this method is just for 
	 * better structure
	 * 
	 * This Method approximates the Stron CheckSet by only looking at the external nodes to be really
	 * outside - the internal nodes might be in several sets
	 * 
	 * This is not yet checked to be true every time, but the external nodes converge faster
	 * 
	 * @param vG
	 * @param HEIndex
	 */
	private void CheckSetWeak(VHyperGraph vG, int HEIndex)
	{
		int outSet = point2Set.get(CPOutside).intValue();
		Vector<Integer> inSets = new Vector<Integer>();
		//Build inSet
		Iterator<MNode> nit = vG.getMathGraph().modifyNodes.getIterator();
		while (nit.hasNext()) //Find Set of any node inside Hyper Edge - if we're ready - this should be the only one inside
		{
			int nodeid = nit.next().index;
			if (vG.getMathGraph().modifyHyperEdges.get(HEIndex).containsNode(nodeid))
			{
				Point p = vG.modifyNodes.get(nodeid).getPosition();
				Point2D p2 = new Point2D.Double(p.getX(),p.getY());
				if (!inSets.contains(point2Set.get(p2)))
				{
					inSets.add(point2Set.get(p2));
				}
			}
		}
		
		Iterator<Point2D> nodeiter = NodePos2Index.keySet().iterator();
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
			else //Outside must be the outset to be valid
			{
				if (point2Set.get(pos)!=outSet) //Another node not from edge is not yet accumulated with the outside stuff
				{
					ResultValidation=false;
					if (inSets.contains(point2Set.get(pos))) //same side as an inside node
					{
						System.err.println("Node #"+id+" inside but not in Edge!");
						invalidNodeIndices.add(id);
					}
				}
			}
		}
	}
	
	/**
	 * If all nodes are valid due to the NURBSShape the last point is, whether their distance to the shape is
	 * bigger than the given minimal distance of the NURBSShape
	 */
	private void checkDistances(VHyperGraph vG, int HEIndex)
	{
		if ((!ResultValidation)||(invalidNodeIndices.size()>0)) //already nonvalid
			return;
		int minDist = vG.modifyHyperEdges.get(HEIndex).getMinimumMargin();
		Iterator<Point2D> nodeiter = NodePos2Index.keySet().iterator();
		while (nodeiter.hasNext()) //Iterator for all node-positions
		{
			Point2D pos = nodeiter.next();
			int id = NodePos2Index.get(pos);
			if (vG.getMathGraph().modifyHyperEdges.get(HEIndex).containsNode(id))
			{ //Inside so its radius (distance to projecion must be at most minDist
				double noderadius = (double)vG.modifyNodes.get(id).getSize()/2d;
				if (point2Radius.get(pos).doubleValue() <= ((double)minDist + noderadius)) //shape of node must be at most mindist away from shape
				{
					invalidNodeIndices.add(id);
					ResultValidation = false;
				}
			}
		}
	}
}
