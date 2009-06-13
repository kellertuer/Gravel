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
		//if this point belongs to a node, this is its index, else its null
		int nodeIndex=-1;
		public PointInfo(int s, double r, int ni)
		{
			set = s; radius = r; nodeIndex=ni;
		}
		public PointInfo(int s, double r)
		{
			this(s,r,-1);
		}
	}
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
		for (int i=0; i<controlPoints.size(); i++)
		{
			Point2D actual = controlPoints.get(i);
			if (actual.getY()<CPOutside.getY())
				CPOutside = (Point2D)actual.clone();	
		}
		System.err.println(CPOutside);
		initPointSets(vG);
		
		//Number of Projections made, 
		int i=0;
		//MaxSize of any circle used, because a circle with this radius is much bigger than the whole graph
		//We need something to mearue font-size so...
		Graphics2D g2 = (Graphics2D) g.getGraphics();
		//(Graphics2D)(new VHyperGraphic(new Dimension(0,0),vG)).getGraphics();
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
			if ((radius<maxSize) &&(radius > 0))
			{	
				pointInformation.get(actualP).radius = radius; //Change radius
				g2.setColor(Color.gray);
				g2.drawOval(Math.round((float)(actualP.getX()-radius)*zoom),
					Math.round((float)(actualP.getY()-radius)*zoom),
					Math.round((float)(2*radius)*zoom), Math.round((float)(2*radius)*zoom));
				//Calculate Distance and direction from Point to its projection
				Point2D ProjDir = new Point2D.Double(ProjP.getX()-actualP.getX(),ProjP.getY()-actualP.getY());
				double length = ProjDir.distance(0d,0d);
				//get radius in opposite direction 
				ProjDir = new Point2D.Double(radius/length*ProjDir.getX(),radius/length*ProjDir.getY());
				//Check whether other Old Points interfere with this one	
				boolean circlehandled = false; //Indicator whether the new circle is completely inside another
				Iterator<Entry<Point2D,PointInfo>> RadiusIterator = pointInformation.entrySet().iterator();
				while (RadiusIterator.hasNext()) //Iterate all old Points
				{
					Entry<Point2D,PointInfo> actEntry = RadiusIterator.next();
					//If the radius is given and distance of the actualPoint to this is smaller that the sum of both radii - both are in the same set
					if (actEntry.getKey()!=actualP)
					{
						if ((!Double.isNaN(actEntry.getValue().radius)) && ( (actEntry.getKey().distance(actualP)+radius <actEntry.getValue().radius)) )
						{
							circlehandled = true; //The circle around actualP was completely handled by actEntry.getKey()
						}
						if ((!Double.isNaN(actEntry.getValue().radius)) &&(actEntry.getKey().distance(actualP)<(actEntry.getValue().radius+radius)) )
						{ //Both circles overlap -> union sets
							int a = actEntry.getValue().set;
							int b = pointInformation.get(actualP).set;
							if (a!=b) //not in the same set yet -> Union of both sets in the minimum (sameset)
								UnionSets(a,b);
						}
					}
				}
				//Calculate a new Point for the set (TODO: the other two new points in 90 and 270 Degree or another better Choice?)
				Point2D newP = new Point2D.Double(actualP.getX()-ProjDir.getX(),actualP.getY()-ProjDir.getY());
				g.drawCP(g2,new Point(Math.round((float)newP.getX()),Math.round((float)newP.getY())),Color.BLUE); //Draw as handled
				PointInfo newPInfo = new PointInfo(pointInformation.get(actualP).set, Double.NaN, -1);
				if (!pointInformation.containsKey(newP)&&(!circlehandled))
				{
					Points.offer(newP);
					pointInformation.put(newP,newPInfo);
				}
				i++;
				if ((i%checkInterval)==0)
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
					System.err.println("- All nodes in #"+pointInformation.get(CPOutside).set+" are outside");
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
			PointInfo p2Info = new PointInfo(n.getIndex(), Double.NaN, n.getIndex());
			pointInformation.put(p2, p2Info);
		}
		int base = vG.getMathGraph().modifyNodes.getNextIndex();
		for (int i=0; i<maxCPIndex-degree; i++)
		{
			Points.offer(controlPoints.get(i));
			//Set is the a free index greater that biggest node index, Radius and INdex are not yet resp never given
			PointInfo cpInfo = new PointInfo(base+i, Double.NaN, -1);
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
	public static Point2D findSuccessor(Point2D p, Point2D pre, NURBSShape c,Graphics2D Debug,float z)
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
		double l = deriv1.getX()*deriv1.getX() + deriv1.getY()*deriv1.getY();
		double curvature = (deriv1.getX()*deriv2.getY() - deriv2.getX()*deriv1.getY())/ Math.sqrt(l*l*l);
		curvature = curvature*c.WeightAt(hatt);
		curvature = Math.abs(curvature);

		double varphi = Math.atan(curvature)+(Math.PI/2);
		System.err.println("curv:"+curvature);
		if (curvature > 1d/1000d)
			varphi = Math.PI;
		else
			varphi = Math.PI/2d;
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

		return newp;
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
		while (nit.hasNext()||(inSet==-1)) //Find Set of any node inside Hyper Edge - if we're ready - this should be the only one inside
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
