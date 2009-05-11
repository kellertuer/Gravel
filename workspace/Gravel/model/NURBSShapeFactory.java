package model;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.Vector;

import javax.vecmath.Point3d;

/**
 * The NURBSShapeFactory creates specific NURBS-Curves based on the input
 * 
 * These NURBS-Curves are always inclamped and closed (and by that degree-1 times continuous also
 * at the start- and endpoint)
 * 
 * @author Ronny Bergmann
 * @since 0.4
 *
 */
public class NURBSShapeFactory {

	public final static int CIRCLE_ORIGIN = 1;
	public final static int CIRCLE_RADIUS = 2;
	
	public final static int DISTANCE_TO_NODE = 3;
	
	public final static int POINTS = 4;
	public final static int DEGREE = 5;
	public final static int SIZES = 6;
	public final static int MAX_INDEX = 7;
	
	/**
	 * Create the Shape based on Specification of Type and needed Parameters
	 * 
	 * All Values given in Capital-Letters are predefined values at which the Parameter must be placed inside the Parameter-Vector
	 * and for each Parameter the Type is given in brackets
	 * 
	 * For the Types
	 * - „Circle“ with Parameters CIRCLE_RAdius (int), CircleOrigin (Point)
	 * - „Global Interpolation“ with given POINTS (Vector<Point2D>) and a DEGREE (int)
	 * - „Convex Hull“ the Nodepositions given through POINTS(Vector<Point2D>) their sizes given by SIZES (Vector<Integer>) and the DEGREE (int)
	 *
	 * @param type
	 * @param Parameters
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static NURBSShape CreateShape(String type, Vector<Object> Parameters)
	{
		int distance;
		try	{distance = Integer.parseInt(Parameters.get(DISTANCE_TO_NODE).toString());}
		catch (Exception e) {return new NURBSShape();} //EmptyShape
		if (type.toLowerCase().equals("circle"))
		{
			int radius;
			try	{radius = Integer.parseInt(Parameters.get(CIRCLE_RADIUS).toString());}
			catch (Exception e) {return new NURBSShape();} //Empty Shape
			return CreateCircle((Point) Parameters.get(CIRCLE_ORIGIN), radius, distance);
		}
		else if (type.toLowerCase().equals("global interpolation"))
		{
			Vector<Point2D> IP_Points;
			try {IP_Points = (Vector<Point2D>) Parameters.get(POINTS);}
			catch (Exception e) {return new NURBSShape();} //Empty Shape
			int degree;
			try	{degree = Integer.parseInt(Parameters.get(DEGREE).toString());}
			catch (Exception e) {return new NURBSShape();}
			return CreateInterpolation(IP_Points, degree);
		}
		else if (type.toLowerCase().equals("convex hull"))
		{
			Vector<Point2D> nodes;
			try {nodes = (Vector<Point2D>) Parameters.get(POINTS);}
			catch (Exception e) {return new NURBSShape();} //Empty Shape
			Vector<Integer> Sizes;
			try {Sizes = (Vector<Integer>) Parameters.get(SIZES);}
			catch (Exception e) {return new NURBSShape();} //Empty Shape
			int degree;
			try	{degree = Integer.parseInt(Parameters.get(DEGREE).toString());}
			catch (Exception e) {return new NURBSShape();}
			return CreateConvexHullPolygon(nodes, Sizes, degree, distance);
		}
		return null;
	}
	
	/**
	 * Create a NURBSSHape Circle based on the Information, where its Origin is, its Radius and the Distance to the nodes
	 * 
	 * TODO: Create an Higher Order Circle
	 * 
	 * @param origin Point of Circle Origin
	 * @param radius Radius of the Circle
	 * @param dist Distance to the nodes
	 * @return
	 */
	private static NURBSShape CreateCircle(Point origin, int radius, int dist)
	{
		//See L. Piegl, W. Tiller: A Menagerie of rational B-Spline Circles
		Vector<Double> knots = new Vector<Double>();
		knots.add(0d);knots.add(0d);knots.add(0d);
		knots.add(.25);
		knots.add(.5);knots.add(.5);
		knots.add(.75);
		knots.add(1d);knots.add(1d);knots.add(1d);
		
		Vector<Double> weights = new Vector<Double>();
		weights.add(1d); weights.add(.5); weights.add(.5);
		weights.add(1d); weights.add(.5); weights.add(.5);
		weights.add(1d);
		
		Vector<Point2D> controlPoints = new Vector<Point2D>();
		controlPoints.add(new Point2D.Double(origin.x+radius, origin.y)); //P0
		controlPoints.add(new Point2D.Double(origin.x+radius, origin.y-radius)); //P1, Top right
		controlPoints.add(new Point2D.Double(origin.x-radius, origin.y-radius)); //P2, Top left
		controlPoints.add(new Point2D.Double(origin.x-radius, origin.y)); //P3
		controlPoints.add(new Point2D.Double(origin.x-radius, origin.y+radius)); //P4, bottom left
		controlPoints.add(new Point2D.Double(origin.x+radius, origin.y+radius)); //P5, bottom right
		controlPoints.add(new Point2D.Double(origin.x+radius, origin.y)); //P6 again P0
		return new NURBSShape(knots,controlPoints,weights);
	}

	/**
  	  * Create and return a Smooth NURBS-Curve of degree degree through the given Interpolation Points,
  	  * that is degree-1 continuous at every Point
  	  * 
	  * @param IP The Interpolation-Points
	  * @param degree the degree
	  * @return
	 */
	public static NURBSShape CreateInterpolation(Vector<Point2D> q, int degree)
	{
		//Based on Algorithm 9.1 from the NURBS-Book
		//close IP to a closed curve
		Vector<Point2D> IP = new Vector<Point2D>();
		int IPCount = q.size();
		for (int i=q.size()-degree; i<q.size(); i++)
			IP.add((Point2D) q.get(i).clone());
		for (int i=0; i<q.size(); i++)
			IP.add((Point2D) q.get(i).clone());
		for (int i=0; i<=degree; i++)
			IP.add((Point2D) q.get(i).clone());
		int maxIPIndex = IP.size()-1; //highest IP Index
		int maxKnotIndex = maxIPIndex+degree+1;//highest KnotIndex in the resulting NURBS-Curve
		if (maxIPIndex < 2*degree) //we have less then 2*degree IP -> no interpolatin possible
			return new NURBSShape();
		//Determine Points to evaluate for IP with cetripetal Aproach
		double d = 0d;
		for (int i=1; i<=maxIPIndex; i++)
			d += Math.sqrt(IP.get(i).distance(IP.get(i-1)));
		Vector<Double> lgspoints = new Vector<Double>();
		lgspoints.setSize(IP.size());
		lgspoints.set(0,0d);
		lgspoints.set(maxIPIndex, 1d);
		for (int i=1; i<maxIPIndex; i++)
			lgspoints.set(i, lgspoints.get(i-1).doubleValue() + Math.sqrt(IP.get(i).distance(IP.get(i-1)))/d);
		//At the lgspoints we evaluate the Curve, get an LGS, that is totally positive and banded
		Vector<Double> Knots = new Vector<Double>();
		Knots.setSize(maxKnotIndex+1); //Because there are 0,...,macKnotIndex Knots
		for (int i=0; i<=degree; i++)
		{
			Knots.set(i,0d); //First degree+1 Entries are zero
			Knots.set(maxKnotIndex-i,1d); //Last p+1 Entries are 1
		}
		for (int j=1; j<=maxIPIndex-degree; j++) //middle values
		{
			double value = 0d;
			for (int i=j; i<=j+degree-1; i++)
			{
				value += lgspoints.get(i);
			}
			value /= degree;
			Knots.set(j+degree, value);
		}
		NURBSShape c = solveLGS(Knots, lgspoints, IP);
		c = unclamp(cutoverlaps(c,lgspoints.get(degree), lgspoints.get(degree+IPCount)));
		for (int i=0; i<degree; i++)
		{
			Point2D a = c.controlPoints.get(i);
			double aw = c.cpWeight.get(i);
			Point2D b = c.controlPoints.get(c.maxCPIndex-degree+1+i);
			double bw = c.cpWeight.get(c.maxCPIndex-degree+1+i);
			
			Point2D middle = new Point2D.Double((a.getX()+b.getX())/2d,	(a.getY()+b.getY())/2d);
			c.cpWeight.set(i, (aw+bw)/2d);
			c.cpWeight.set(c.maxCPIndex-degree+1+i, (aw+bw)/2d);
			c.controlPoints.set(i,(Point2D)middle.clone());
			c.controlPoints.set(c.maxCPIndex-degree+1+i,middle);
		}
		return c;
	}

	/**
	 * Cut the additionally added parts from the curve to get an 
	 * unclamped closed NURBSCurve
	 * 
	 * TODO: Try to set [u1,u2] to [0,1] For simplicity reasons?
	 * @param u1
	 * @param u2
	 */
	private static NURBSShape cutoverlaps(NURBSShape c, double u1, double u2)
	{
		int Start = c.findSpan(u1);
		int End = c.findSpan(u2);
		if (u2==c.Knots.get(c.maxKnotIndex-c.degree)) //Last possible Value the Curve is evaluated
			End++;
		if ((Start==-1)||(End==-1)||(u1>=u2)) //Ohne u out of range or invalid interval
			return new NURBSShape(); //Return amepty Shape
		
		//Raise both endvalues to multiplicity d to get an clamped curve
		int multStart = 0;
		while (c.Knots.get(Start+multStart).doubleValue()==u1)
			multStart++;
		int multEnd = 0;
		while (c.Knots.get(End-multEnd).doubleValue()==u2)
			multEnd++;
		Vector<Double> Refinement = new Vector<Double>();
		for (int i=0; i<=c.degree-multStart; i++)
			Refinement.add(u1);
		for (int i=0; i<=c.degree-multEnd; i++)
			Refinement.add(u2);
		//Nun wird der Start- und der Endpunkt
		NURBSShape subcurve = c.clone();
		subcurve.RefineKnots(Refinement); //Now it interpolates subcurve(u1) and subcurve(u2)
		Vector<Point2D> newCP = new Vector<Point2D>();
		Vector<Double> newWeight= new Vector<Double>();
		for (int i=Start+1; i<(End+c.degree+1-multStart+1); i++)
		{
			newCP.add((Point2D)subcurve.controlPoints.get(i).clone());
			newWeight.add(subcurve.cpWeight.get(i).doubleValue());
		}
		//Copy needed Knots
		int index = 0;
		Vector<Double> newKnots = new Vector<Double>();
		while (subcurve.Knots.get(index)<u1)
			index++;
		while (subcurve.Knots.get(index)<=u2)
		{
			newKnots.add(subcurve.Knots.get(index).doubleValue());
			index++;
		}
		return new NURBSShape(newKnots,newCP,newWeight);
	}
	/**
	 * For given Knot-Vector, lgspoints and InterpolationPoints this Method returns the NURBS-Shape
	 * The Knots define The range and distribution of the ControlPoints in the resulting NURBSShape
	 * The lgspoints (that lie within the range of the knots-values) define on  which values the 
	 * InterpolationPoints are reached within the curve
	 * @param Knots
	 * @param lgspoints
	 * @param IP
	 * @return
	 */
	private static NURBSShape solveLGS(Vector<Double> Knots, Vector<Double> lgspoints, Vector<Point2D> IP) 
	{	
		int maxIPIndex = IP.size()-1; //highest IP Index
		int maxKnotIndex = Knots.size()-1; //highest KnotIndex in the resulting NURBS-Curve
		int degree = maxKnotIndex-maxIPIndex-1;
		Vector<Point2D> ControlPoints = new Vector<Point2D>(); //The Resulting ConrolPointVecotr
		ControlPoints.setSize(maxIPIndex+1);
		Vector<Double> weights = new Vector<Double>();
		weights.setSize(maxIPIndex+1);
		for (int i=0; i<=maxIPIndex; i++)
		{
			weights.set(i,1.0d);
			ControlPoints.set(i,new Point(0,0)); //Zero Initialization
		}
		
		NURBSShape temp = new NURBSShape(Knots, ControlPoints,weights); //Temporary Shape for the BasisFunctions
		//Compute the basis-Function-Values and set them as row of a Matrix
		double[][] LGS = new double[maxIPIndex+1][maxIPIndex+1]; //Already zero initialized;
		for (int i=0; i<=maxIPIndex; i++) //For each Row
		{
			int firstnonzero = temp.findSpan(lgspoints.get(i));
			Vector<Double> nonZeroBasisValues = temp.BasisBSpline(lgspoints.get(i));
			for (int j=0; j<=degree; j++) //Set all nonzero Row values
			{
					LGS[i][j+firstnonzero-degree] = nonZeroBasisValues.get(j);
			}
		}
		//Now Solve the LGS for X to get P_i X-Coordinate and then for Y 
		//Because this matrix is totally positive and semibandwith less than p - gauss without pivoting is possible
		for (int i=0; i<=maxIPIndex; i++) //Calculate LGS = LR
		{
			for (int j=i; j<=maxIPIndex; j++)
			{	
				for (int k=1; k<=i-1; k++)
				{
	               LGS[i][j] -= LGS[i][k] * LGS[k][j];
				}
			}
			for (int j=i+1; j<=maxIPIndex; j++)
			{
				for (int k=1; k<=i-1; k++)
				{
				   LGS[j][i] -= LGS[j][k] * LGS[k][i];
				}
	            LGS[j][i] /= LGS[i][i];
			}
		}
		//Forward Computation of P
		for (int i=0; i<=maxIPIndex; i++)
		{
			double thisX = IP.get(i).getX(), thisY = IP.get(i).getY();
			for (int k=0; k<i; k++)
			{
				thisX -= LGS[i][k]*ControlPoints.get(k).getX();
				thisY -= LGS[i][k]*ControlPoints.get(k).getY();
			}
			ControlPoints.set(i, new Point2D.Double(thisX,thisY));
		}
	//	And Backward with R
		for (int i=maxIPIndex; i>=0; i--)
		{
			double thisPointX=ControlPoints.get(i).getX(), thisPointY=ControlPoints.get(i).getY();
			for (int k=i+1; k<=maxIPIndex; k++)
				{
					thisPointX -= LGS[i][k]*ControlPoints.get(k).getX();
					thisPointY -= LGS[i][k]*ControlPoints.get(k).getY();
				}
			thisPointX /= LGS[i][i];
			thisPointY /= LGS[i][i];
			ControlPoints.set(i,new Point2D.Double(thisPointX,thisPointY));
		}
		//Create a linear Shapepart around Start/End
		return new NURBSShape(Knots, ControlPoints,weights);
	}
	
	/**
	 * Unclamp the Curve at both sides
	 * @param c
	 * @return
	 */
	public static NURBSShape unclamp(NURBSShape c)
	{
		if ((c.getType()&NURBSShape.UNCLAMPED)==NURBSShape.UNCLAMPED)
			return c; //already unclamped
		//Algorithm 12.1
		Vector<Point3d> Pw = c.controlPointsHom;
		Vector<Double> U = c.Knots;
		int p = c.degree, n = c.maxCPIndex;
		for (int i=0; i<=p-2; i++)
		{
			U.set(p-i-1, U.get(p-i) - (U.get(n-i+1)-U.get(n-i)));
			int k=p-1;
			for (int j=i; j>=0; j--)
			{
				double alpha = (U.get(p)-U.get(k))/(U.get(p+j+1)-U.get(k));
				double x = (Pw.get(j).x - alpha*Pw.get(j+1).x)/(1.0-alpha);
				double y = (Pw.get(j).y - alpha*Pw.get(j+1).y)/(1.0-alpha);
				double w = (Pw.get(j).z - alpha*Pw.get(j+1).z)/(1.0-alpha);
				Pw.set(j, new Point3d(x,y,w));
				k--;
			}
		}
		U.set(0, U.get(1) - (U.get(n-p+2)-U.get(n-p+1)));
		for (int i=0; i<=p-2; i++)
		{
			U.set(n+i+2, U.get(n+i+1) + (U.get(p+i+1)-U.get(p+i)));
			for (int j=i; j>=0; j--)
			{
				double alpha = (U.get(n+1)-U.get(n-j))/(U.get(n-j+i+2)-U.get(n-j));
				double x = (Pw.get(n-j).x - (1d-alpha)*Pw.get(n-j-1).x)/alpha;
				double y = (Pw.get(n-j).y - (1d-alpha)*Pw.get(n-j-1).y)/alpha;
				double w = (Pw.get(n-j).z - (1d-alpha)*Pw.get(n-j-1).z)/alpha;
				Pw.set(n-j, new Point3d(x,y,w));
			}
		}
		U.set(n+p+1, U.get(n+p) + (U.get(2*p)-U.get(2*p-1)));
		c = new NURBSShape(U, Pw);
		return c;
	}
	
	/**
	 * 	 * Based on Grahams Scan this Method creates a convex hull and calculates Interpolation-Points that are at least
	 * The size of a node plus the minimal Distance away from the node 
	 * @param nodes Positions of the Nodes inside the convex hull
	 * @param sizes Node-Sizes, so that the Shape is at most sizes(i)+distance away from the node-position
	 * @param degree degree of the resulting NURBSShape
	 * @param distance Distance mentioned above in the sizes
	 * 
	 * @return
	 */
	private static NURBSShape CreateConvexHullPolygon(Vector<Point2D> nodes, Vector<Integer> sizes, int degree, int distance)
	{
		if (nodes.size()<2) //mindestens 3 Knoten nötig
			return new NURBSShape();
		Vector<Point2D> ConvexHull = GrahamsScan(nodes);
		Vector<Point2D> IPoints = new Vector<Point2D>();
		for (int i=0; i<ConvexHull.size(); i++)
		//Compute 3 Points for each Point of the convex hull to expand by minDist + individual node size
		{
			double prevX, prevY;
			if (i==0)
			{
				prevX = ConvexHull.lastElement().getX(); prevY = ConvexHull.lastElement().getY();
			}
			else
			{
				prevX = ConvexHull.get(i-1).getX(); prevY = ConvexHull.get(i-1).getY();				
			}
			double postX,postY;
			if (i==(ConvexHull.size()-1))
			{
				postX = ConvexHull.firstElement().getX(); postY = ConvexHull.firstElement().getY();
			}
			else
			{
				postX = ConvexHull.get(i+1).getX(); postY = ConvexHull.get(i+1).getY();				
			}
			double thisX = ConvexHull.get(i).getX();
			double thisY = ConvexHull.get(i).getY();
			int pos = nodes.indexOf(ConvexHull.get(i));
			Point2D direction1 = new Point2D.Double(prevX-thisX, prevY-thisY);
			double dir1l = direction1.distance(0d,0d);
			Point2D direction2 = new Point2D.Double(postX-thisX, postY-thisY);
			double dir2l = direction2.distance(0d,0d);
			Point2D direction3 = new Point2D.Double(direction1.getX()+direction2.getX(), direction1.getY()+direction2.getY());
			double dir3l = direction3.distance(0d,0d);
			IPoints.add(new Point2D.Double(thisX - direction1.getX()/dir1l*(distance+(double)sizes.get(pos)/2d), thisY - direction1.getY()/dir1l*(distance+(double)sizes.get(pos)/2d)));
			IPoints.add(new Point2D.Double(thisX - direction2.getX()/dir2l*(distance+(double)sizes.get(pos)/2d), thisY - direction2.getY()/dir2l*(distance+(double)sizes.get(pos)/2d)));
			IPoints.add(new Point2D.Double(thisX - direction3.getX()/dir3l*(distance+(double)sizes.get(pos)/2d), thisY - direction3.getY()/dir3l*(distance+(double)sizes.get(pos)/2d)));
		}
		IPoints = GrahamsScan(IPoints); //Make convex again
		if (IPoints.size()<=degree)
			return new NURBSShape();
		return CreateInterpolation(IPoints, degree);
	}
	
	/**
	 * Calculate the Convex Hull of a set of Points 
	 * by using Grahams Scan
	 * 
	 * @param nodes Points from which the resulting Convex Hull is calculated
	 * 
	 * @return Points of the nodes that from the convex hull in clockwise order
	 */
	private static Vector<Point2D> GrahamsScan(Vector<Point2D> nodes)
	{
		//Find Pivot Point with lowest Y Coordinate. If there are two, take the one with lowest X
		Point2D pivot = new Point2D.Double(Double.MAX_VALUE,Double.MAX_VALUE);
		Iterator<Point2D> iter = nodes.iterator();
		while (iter.hasNext())
		{
			Point2D pnext = iter.next();
			if (pnext.getY()<pivot.getY()) //new smallest
				pivot = (Point2D) pnext.clone();
			else if ((pnext.getY()==pivot.getY())&&(pnext.getX()<pivot.getX()))
				pivot = (Point2D) pnext.clone();					
		}
		//Sort the Points nextP-P by angle with the X-Axis, which is between 0 and 180
		//Not so fast implemented but that is not so important, i think
		Vector<Point2D> sortedByAngle = new Vector<Point2D>();
		iter = nodes.iterator();
		while (iter.hasNext())
		{
			Point2D pnext = iter.next();
			Point2D direction = new Point2D.Double(pnext.getX()-pivot.getX(), pnext.getY()-pivot.getY());
			double deg = getDegreefromDirection(direction);
			//Sort em in
			Iterator<Point2D> sortiter = sortedByAngle.iterator();
			double cmpdeg = Double.MIN_VALUE;
			int index=0;
			while ((sortiter.hasNext())&&(cmpdeg<=deg))
			{
				Point2D pcomp = sortiter.next();
				Point2D cmpdir = new Point2D.Double(pcomp.getX()-pivot.getX(), pcomp.getY()-pivot.getY());
				cmpdeg = getDegreefromDirection(cmpdir);
				if (cmpdeg==deg) //Equal Degree, add before cmp by distance
				{
					if (cmpdir.distance(0d,0d)<=direction.distance(0d,0d))
						cmpdeg = deg + 1; //break
					else
						cmpdeg = deg - 1; //continue;
				}
				if (cmpdeg<=deg)
				index++;
			}
			sortedByAngle.add(index,pnext);
		}
		Vector<Point2D> result = new Vector<Point2D>();
		result.setSize(2);
		//Add the first two Elements
		result.set(0, sortedByAngle.get(0));
		result.set(1, sortedByAngle.get(1));
		
		for (int i=2; i < sortedByAngle.size(); i++)
		{
			while ((result.size()>=2)
					&&(CrossProduct(result.get(result.size()-2), result.lastElement(), sortedByAngle.get(i))<=0))
				result.remove(result.size()-1);

			result.add(sortedByAngle.get(i));
		}		 
		return result;
	}
	
	/**
	 * Calculating the cross product of three points
	 * @param p1
	 * @param p2
	 * @param p3
	 * @return
	 */
	private static double CrossProduct(Point2D p1, Point2D p2, Point2D p3)
	{
       return (p2.getX() - p1.getX())*(p3.getY() - p1.getY()) - (p3.getX() - p1.getX())*(p2.getY() - p1.getY());
	}
	
	/**
	 * Calculate the Degree of a given direction
	 * @param dir
	 * @return
	 */
	private static double getDegreefromDirection(Point2D dir)
	{
		//Compute Degree with inverted Y Axis due to Computergraphics
		double x = dir.getX(), y=-dir.getY();
		double length = dir.distance(0d,0d);
		if (x==0d) //90 or 270 Degree
		{
			if (y<0d) //Up
				return 90d;
			else if (y>0d) //Down
				return 270d;
			else
				return 0d;
		}
		if (y==0d) //0 or 180 Degree
		{
			if (x<0d) //Left
				return 180d;
			else //right
				return 0d;
		}
		//Now both are nonzero, 
		if (x>0d)
			if (y<0d) //  1. Quadrant
				return Math.asin(Math.abs(y)/length)*180.d/Math.PI; //In Degree
			else //y>0  , 4. Quadrant
				return Math.acos(Math.abs(y)/length)*180.d/Math.PI + 270d; //In Degree
		else //x<0 left side
			if (y<0d) //2. Quadrant
				return 180.0d - Math.asin(Math.abs(y)/length)*180.d/Math.PI; //In Degree
			else //y>0, 3. Quadrant
				return 270.0d - Math.acos(Math.abs(y)/length)*180.d/Math.PI; //In Degree
	}
}