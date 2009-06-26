package model;

import view.VCommonGraphic;

import io.GeneralPreferences;

import java.awt.Color;
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
	private class PointInfo
	{ //Additional Info for a Point
		int set;
		double radius;
		Point2D projectionPoint=null, previousPoint=null;
		//if this point belongs to a node, this is its index, else its null
		int nodeIndex=-1;
		public PointInfo(int s, double r,Point2D projP, Point2D pre, int ni)
		{
			set = s; radius = r; nodeIndex=ni; projectionPoint = projP; previousPoint = pre;
		}
		public PointInfo(int s, double r,Point2D projP, int ni)
		{
			set = s; radius = r; nodeIndex=ni; projectionPoint = projP;
		}
		public PointInfo(int s, double r, Point2D pr)
		{
			this(s,r,pr,-1);
		}
		public String toString()
		{
			if (nodeIndex==-1)
				return "PointInfo: is in Set "+set+" and distance "+radius+" to Curve (C(u) = "+projectionPoint;
			else
				return "PointInfo: Node #"+nodeIndex+" is in Set "+set+" and distance "+radius+" to Curve (C(u) = "+projectionPoint;
						
		}
	}
	private final double TOL = 0.01d, MINRAD = 1d;
	float zoom = GeneralPreferences.getInstance().getFloatValue("zoom");
	private Point2D CPOutside;
	//Points we have to work on 
	private Queue<Point2D> Points = new LinkedList<Point2D>();
	//Function that assigns every node a setnumber - which will be in the beginning its index and if
	//the setnumber of the node position changes - this one is also updated
	private HashMap<Point2D,PointInfo> pointInformation = new HashMap<Point2D,PointInfo>();
	
	private Vector<Integer> invalidNodeIndices  = new Vector<Integer>();
	private boolean ResultValidation;
	
	private NURBSShape origCurve;
	public NURBSShapeValidator(VHyperGraph vG, int HyperEdgeIndex, NURBSShape Curve, VCommonGraphic g)
	{
		ResultValidation=false;
		VHyperEdge e = vG.modifyHyperEdges.get(HyperEdgeIndex);
		if (e==null)
			return;
		if (Curve!=null)
			origCurve = Curve; //If given use the specific curve
		else //it is null, use the Curve of the hyperedge
			origCurve = e.getShape();
		if ((origCurve==null) || (origCurve.isEmpty()))
			return;
		NURBSShape clone = origCurve.stripDecorations().clone();
		//Now also the curve and the Hyperedge are correct for the check
		ResultValidation=true;
		setCurveTo(clone.Knots, clone.controlPoints, clone.cpWeight);
		//
		// Start of Validation-Algorithm
		//
		//Search for the Point that is definetly outside
		CPOutside= new Point2D.Double(Double.MAX_VALUE,Double.MAX_VALUE);		
		for (int i=0; i<maxCPIndex-degree; i++)
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
		Graphics2D g2 = (Graphics2D) g.getGraphics();
		//(Graphics2D)(new VHyperGraphic(new Dimension(0,0),vG)).getGraphics();
		Point MaxPoint = vG.getMaxPoint(g2);
		Point MinPoint = vG.getMinPoint(g2);
		int maxSize = Math.max(MaxPoint.x-MinPoint.x, MaxPoint.y-MinPoint.y);
		//Check each Intervall, whether we are done
		int checkIntervall = Points.size();
		boolean running=true;
		
		while (!Points.isEmpty()&&running) 
		{
			Point2D actualP = Points.poll();
			PointInfo actualInfo = pointInformation.get(actualP);
			if ((Double.isNaN(actualInfo.radius))||(actualInfo.projectionPoint==null))
			{
				NURBSShapeProjection proj = new NURBSShapeProjection(this,actualP);
				actualInfo.radius = proj.getResultPoint().distance(actualP);
				actualInfo.projectionPoint = proj.getResultPoint();
			}
			if ((actualInfo.radius < maxSize)&&(actualInfo.radius > MINRAD))
			{	
				i++;
				g2.setColor(Color.gray);
				g2.drawOval(Math.round((float)(actualP.getX()-actualInfo.radius)*zoom),
					Math.round((float)(actualP.getY()-actualInfo.radius)*zoom),
					Math.round((float)(2*actualInfo.radius)*zoom), Math.round((float)(2*actualInfo.radius)*zoom));
				//Calculate Distance and direction from Point to its projection
				boolean circlehandled = false; //Indicator whether the new circle is completely inside another
				Iterator<Entry<Point2D,PointInfo>> RadiusIterator = pointInformation.entrySet().iterator();
				while (RadiusIterator.hasNext()) //Iterate all old Points
				{
					Entry<Point2D,PointInfo> actEntry = RadiusIterator.next();
					//If the radius is given and distance of the actualPoint to this is smaller that the sum of both radii - both are in the same set
					if (actEntry.getKey()!=actualP)
					{
						if ((!Double.isNaN(actEntry.getValue().radius)) && ( ( (actEntry.getKey().distance(actualP)+actualInfo.radius) < actEntry.getValue().radius)) )
						{
							circlehandled = true; //The circle around actualP was completely handled by actEntry.getKey()
						}
						if ((!Double.isNaN(actEntry.getValue().radius)) &&(actEntry.getKey().distance(actualP)<(actEntry.getValue().radius+actualInfo.radius - TOL)) )
						{ //Both circles overlap -> union sets
							int a = actEntry.getValue().set;
							int b = actualInfo.set;
							if (a!=b) //not in the same set yet -> Union of both sets in the minimum (sameset)
								UnionSets(a,b);
						}
					}
				}
				Vector<Point2D> Succ = findSuccessors(actualP);
				if ((Succ==null)||(Succ.size()==0))
				{}
				else if (!circlehandled) 	//At least One Successor at 180°
				{
					for (int j=0; j<Succ.size(); j++)
					{
						if (!pointInformation.containsKey(Succ.get(j))) //Just set the set
						{
							PointInfo newPInfo = new PointInfo(actualInfo.set, Double.NaN,null,actualP,-1);
							pointInformation.put(Succ.get(j),newPInfo);
							Points.offer(Succ.get(j));
						}
						else //Just offer, because its q
							Points.offer(Succ.get(j));
						g.drawCP(g2, new Point(Math.round((float)Succ.get(j).getX()),Math.round((float)Succ.get(j).getY())),Color.ORANGE);
					}
				}
				if ((i%checkIntervall)==0)
				{
					System.err.print(i+"   -  ");
					boolean valid = CheckSet(vG, HyperEdgeIndex);
					Iterator<MNode> nodeiter = vG.getMathGraph().modifyNodes.getIterator();
					while (nodeiter.hasNext()) //Iterator for all node-positions
					{
						int id = nodeiter.next().index;
						Point2D pos = getPointOfNode(id);
						System.err.print(id+"in"+pointInformation.get(pos).set+"  ");
					}
					System.err.println("- All nodes in #"+pointInformation.get(CPOutside).set+" are outside ("+Points.size()+" nodes left)");
					//If either ResultValid=true Wrong.size()==0 we're ready because the shape is valid
					//If ResultValid=false and Wrong.size()>0 we're ready because the shape is invalid
					running = ! (  (valid&&(invalidNodeIndices.size()==0)) || (!valid&&(invalidNodeIndices.size()>0)) );
					if (!running)
						ResultValidation = valid;
				}
			}	//end if circle big enough
		} //end while
		if (ResultValidation) //Nodes are valid due to inside or outside
		{	checkDistances(vG,HyperEdgeIndex);
			//Check whether we got after all points and CheckDistance to more than 2 sets without wrong nodes
			Vector<Integer> Insets = new Vector<Integer>();
			Vector<Integer> Outsets = new Vector<Integer>();
			Iterator<MNode> nodeiter = vG.getMathGraph().modifyNodes.getIterator();
			while (nodeiter.hasNext()) //Iterator for all node-positions
			{
				int id = nodeiter.next().index;
				Point2D pos = getPointOfNode(id);
				if (vG.getMathGraph().modifyHyperEdges.get(HyperEdgeIndex).containsNode(id))
				{
					if (!Insets.contains(pointInformation.get(pos).set))
						Insets.add(pointInformation.get(pos).set);
				}
				else
					if (!Outsets.contains(pointInformation.get(pos).set))
						Outsets.add(pointInformation.get(pos).set);
					
			}
			if ((Insets.size() > 1) || (Outsets.size() > 1))
				ResultValidation = false;
		}
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
	@Override
	public NURBSShape stripDecorations()
	{
		return origCurve.stripDecorations();
	}
	@Override
	public int getDecorationTypes()
	{
		return origCurve.getDecorationTypes()|NURBSShape.VALIDATOR;
	}
	private Point2D getPointOfNode(int i)
	{
		//Iterate over Points and get its nodeindex
		Iterator<Entry<Point2D, PointInfo>> it = pointInformation.entrySet().iterator();
		while (it.hasNext())
		{
			Entry<Point2D, PointInfo> actualEntry = it.next();
			if (actualEntry.getValue().nodeIndex==i)
				return actualEntry.getKey();
		}
		return null;
	}
	/**
	 * 
	 * Union the two sets specified by a and b in the set with smaller index
	 * All Points are searched, whether they are in Set a or b and put into min{a,b}
	 * 
	 * @param SetFunction
	 * @param max
	 * @param min
	 */
	private void UnionSets(int a, int b)
	{
		if (a==b)
			return;
		int min = Math.min(a,b);
		int max = Math.max(a,b);
		Iterator<Entry<Point2D,PointInfo>> it = pointInformation.entrySet().iterator();
		while (it.hasNext())
		{
			Entry<Point2D,PointInfo> checkEntry = it.next();
			
			if (checkEntry.getValue().set==max)
			{
				checkEntry.getValue().set=min;
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
			//Set is the node index, radius is not given yet, Nodeindex is the nodeindex
			PointInfo p2Info = new PointInfo(n.getIndex(), Double.NaN, null,n.getIndex());
			pointInformation.put(p2, p2Info);
		}
		int base = vG.getMathGraph().modifyNodes.getNextIndex();
		for (int i=0; i<maxCPIndex-degree; i++)
		{
			Points.offer(controlPoints.get(i));
			//Set is the a free index greater that biggest node index, Radius and INdex are not yet resp never given
			PointInfo cpInfo = new PointInfo(base+i, Double.NaN,null, -2); //-2 inidcates original CP
			pointInformation.put(controlPoints.get(i),cpInfo);
		}
	}
	/**
	 * Find a successor for the point p depending upon its precessor pre
	 * and the nurbs Curve c (its projection and secod derivative)
	 * @param p any point p we want the successor of
	 * @param pre precessor of that point
	 * @param c NURBS Shape we are projecting onto
	 * @param Debug - Debug Graphics
	 * @param z Debug-Zoom
	 * @return
	 */
	private Vector<Point2D> findSuccessors(Point2D p)
	{
		if (!pointInformation.containsKey(p))
			return null;
		NURBSShapeProjection projP = new NURBSShapeProjection(this,p);
		Point2D p_c = projP.getResultPoint(); //This Point belong definetly to the same set as actualP but lies on the Curve
		double r_p = p_c.distance(p); //1 linewidth

		Point2D ProjDir = new Point2D.Double(p_c.getX()-p.getX(),p_c.getY()-p.getY());
		//Calculate a new Point for the set (TODO: the other two new points in 90 and 270 Degree or another better Choice?)
		Point2D q = new Point2D.Double(p.getX()-ProjDir.getX(),p.getY()-ProjDir.getY());
		//Calculate Distance and direction from Point to its projection
		NURBSShapeProjection projQ = new NURBSShapeProjection(this,q);
		Point2D q_c = projQ.getResultPoint(); //This Point belong definetly to the same set as actualP but lies on the Curve
		double r_q = q_c.distance(q); //1 linewidth
		//The two real new Points
		double alpha = Math.PI/2d + r_q/(2*r_p)*(Math.PI/2d);
		double degTOL = Math.PI/36; //5 Degree Tolerance
		if (alpha>(Math.PI-degTOL))
			alpha = Math.PI;
		else if (alpha<(Math.PI/2d + degTOL))
			alpha = Math.PI/2d;
		Vector<Point2D> result = new Vector<Point2D>();
		if (alpha==Math.PI) //Result is only Q
		{ 	//We only put Q into the result, but that also means we
			//put the Projection point info into pointinformation so that we don't have to compute them again
			result.add(q);
			if (!pointInformation.containsKey(q)) //q is in the same set as p with radius r_q, ProjP q_c Predecessor p and is no point for a node
				pointInformation.put(q, new PointInfo(pointInformation.get(p).set,r_q,q_c,p,-1));
			else
				return new Vector<Point2D>(); //We handled that point already, no Successor
		}
		else 
		{
			Point2D q1 = new Point2D.Double(p.getX()+Math.cos(alpha)*ProjDir.getX() + Math.sin(alpha)*ProjDir.getY(),
					p.getY() - Math.sin(alpha)*ProjDir.getX() + Math.cos(alpha)*ProjDir.getY());
			Point2D q2 = new Point2D.Double(p.getX()+Math.cos(alpha)*ProjDir.getX() - Math.sin(alpha)*ProjDir.getY(),
					p.getY() + Math.sin(alpha)*ProjDir.getX() + Math.cos(alpha)*ProjDir.getY());
			if ((pointInformation.get(p).previousPoint!=null)&&(alpha>(Math.PI/2d)))
					//We have a Predecessor, try to continue direction
					//If we don't just split - then use both
			{
				//We take that qi with the greater angle, that is more
				double c1 = pointInformation.get(p).previousPoint.distance(q1);
				double c2 = pointInformation.get(p).previousPoint.distance(q2);
				if (c1>=c2)
					result.add(q1);
				else
					result.add(q2);
			}
			else //No predecessor - use both 
			{
				result.add(q1);
				result.add(q2);
			}
		}
		return result;
	}
	/**
	 * Check the actual sets of NodePositions whether they are legal or not and
	 * whether there are only two sets left
	 * 
	 * The result is given as boolean, though additionally the classglobal Aray of Nodeindices is filled with those nodes that are identified as wrong (if they exist)
	 * 
	 * This method does not terminate for curves where the internal nodes are nearly isolated so that there are no circles 
	 * that interconnect the internal regions which never results in twosets to be true
	 * 
	 * @param vG
	 * @param HEIndex
	 */
	private boolean CheckSet(VHyperGraph vG, int HEIndex)
	{
		boolean result;
		//All Hyperedge nodes must be in a set (inSet) and all other in exactely one other set (outset)
		int inSet=-1, outSet = pointInformation.get(CPOutside).set;
		Iterator<MNode> nit = vG.getMathGraph().modifyNodes.getIterator();
		while ((nit.hasNext())&&(inSet==-1)) //Find Set of any node inside Hyper Edge - if we're ready - this should be the only one inside
		{
			int nodeid = nit.next().index;
			if (vG.getMathGraph().modifyHyperEdges.get(HEIndex).containsNode(nodeid))
			{
				Point2D p2 = getPointOfNode(nodeid);
				inSet = pointInformation.get(p2).set;
			}
		}
		result = true;
		Iterator<VNode> nodeiter = vG.modifyNodes.getIterator();
		boolean twosets=true;
		while (nodeiter.hasNext())
		{
			int id= nodeiter.next().getIndex();
			Point2D pos = getPointOfNode(id);
			int set = pointInformation.get(pos).set;
			if (vG.getMathGraph().modifyHyperEdges.get(HEIndex).containsNode(id))
			{
				if (set==outSet) //node of Hyperedge outside
				{
					invalidNodeIndices.add(id);
					result = false;
					System.err.println("Node #"+id+" outside shape but in Edge");
				}
			}
			else //Outside
			{
				if (inSet==set) //Another node not from edge is inside
				{
					invalidNodeIndices.add(id);
					result=false;
					System.err.println("Node #"+id+" inside but not in Edge!");
				}
			}
			if ((set!=inSet)&&(outSet!=set))
			{
				twosets=false; //More than two sets
				break;
			}
		}
		
		if (!twosets) //We'Re not ready yet
		{
			if (invalidNodeIndices.size()>0)
				return false; //We have wrong nodes
			else
			invalidNodeIndices.clear(); return false;
		}
		return result;
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
		Iterator<MNode> nodeiter = vG.getMathGraph().modifyNodes.getIterator();
		while (nodeiter.hasNext()) //Iterator for all node-positions
		{
			int id = nodeiter.next().index;
			Point2D pos = getPointOfNode(id);
			if (vG.getMathGraph().modifyHyperEdges.get(HEIndex).containsNode(id))
			{ //Inside so its radius (distance to projecion must be at most minDist
				double noderadius = (double)vG.modifyNodes.get(id).getSize()/2d;
				if (pointInformation.get(pos).radius <= ((double)minDist + noderadius)) //shape of node must be at most mindist away from shape
				{
					System.err.println("Node #"+id+" does violate margin, "+pointInformation.get(pos).radius+" < "+((double)minDist + noderadius)+"");
					invalidNodeIndices.add(id);
					ResultValidation = false;
				}
			}
		}
	}
}
