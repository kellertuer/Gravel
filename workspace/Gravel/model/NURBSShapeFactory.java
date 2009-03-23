package model;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.Vector;

public class NURBSShapeFactory {

	public final static int CIRCLE_ORIGIN = 1;
	public final static int CIRCLE_RADIUS = 2;
	
	public final static int DISTANCE_TO_NODE = 3;
	
	public final static int POINTS = 4;
	public final static int DEGREE = 5;
	public final static int SIZES = 6;
	public final static int MAX_INDEX = 7;
	
	@SuppressWarnings("unchecked")
	public static VHyperEdgeShape CreateShape(String type, Vector<Object> Parameters)
	{
		int distance;
		try	{distance = Integer.parseInt(Parameters.get(DISTANCE_TO_NODE).toString());}
		catch (Exception e) {return new VHyperEdgeShape();} //EmptyShape
		if (type.toLowerCase().equals("circle"))
		{
			int radius;
			try	{radius = Integer.parseInt(Parameters.get(CIRCLE_RADIUS).toString());}
			catch (Exception e) {return new VHyperEdgeShape();} //Empty Shape
			return CreateCircle((Point) Parameters.get(CIRCLE_ORIGIN), radius, distance);
		}
		else if (type.toLowerCase().equals("global interpolation"))
		{
			Vector<Point2D> IP_Points;
			try {IP_Points = (Vector<Point2D>) Parameters.get(POINTS);}
			catch (Exception e) {return new VHyperEdgeShape();} //Empty Shape
			int degree;
			try	{degree = Integer.parseInt(Parameters.get(DEGREE).toString());}
			catch (Exception e) {return new VHyperEdgeShape();}
			return CreateInterpolation(IP_Points, degree);
		}
		else if (type.toLowerCase().equals("convex hull"))
		{
			Vector<Point2D> nodes;
			try {nodes = (Vector<Point2D>) Parameters.get(POINTS);}
			catch (Exception e) {return new VHyperEdgeShape();} //Empty Shape
			Vector<Integer> Sizes;
			try {Sizes = (Vector<Integer>) Parameters.get(SIZES);}
			catch (Exception e) {return new VHyperEdgeShape();} //Empty Shape
			int degree;
			try	{degree = Integer.parseInt(Parameters.get(DEGREE).toString());}
			catch (Exception e) {return new VHyperEdgeShape();}
			return CreateConvexHullPolygon(nodes, Sizes, degree, distance);
		}
		return null;
	}
	
	private static VHyperEdgeShape CreateCircle(Point origin, int radius, int dist)
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
		return new VHyperEdgeShape(knots,controlPoints,weights,dist);
	}

	/**
  	  * Create and return a Smooth NURBS-Curve of degree degree through the given Interpolation Points
  	  * 
	  * @param q The Interpolation-Points
	  * @param degree the degree
	  * @return
	 */
	private static VHyperEdgeShape CreateInterpolation(Vector<Point2D> q, int degree)
	{
		//Based on Algorithm 9.1 from the NURBS-Book
		
		int maxIPIndex = q.size()-1; //So we have the InterpolationPoints q.get(0) to q.get(maxIPIndex)
		int maxKnotIndex = maxIPIndex+degree+1;//So we have Knots with indices from Zero to MaxKnotIndex
		
		//First determine the Points, where we interpolate depending to the Interpolation-Points
		//This Part uses the cetripetal Aproach
		double d = 0d;
		for (int i=1; i<=maxIPIndex; i++)
			d += Math.sqrt(q.get(i).distance(q.get(i-1)));
		Vector<Double> lgspoints = new Vector<Double>();
		lgspoints.setSize(q.size());
		lgspoints.set(0,0d);
		lgspoints.set(maxIPIndex, 1d);
		for (int i=1; i<maxIPIndex; i++)
			lgspoints.set(i, lgspoints.get(i-1).doubleValue() + Math.sqrt(q.get(i).distance(q.get(i-1)))/d);
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
		Vector<Point2D> ControlPoints = new Vector<Point2D>(); //The Resulting ConrolPointVecotr
		ControlPoints.setSize(maxIPIndex+1);
		Vector<Double> weights = new Vector<Double>();
		weights.setSize(maxIPIndex+1);
		for (int i=0; i<=maxIPIndex; i++)
		{
			weights.set(i,1.0d);
			ControlPoints.set(i,new Point(0,0)); //Zero Initialization
		}
		
		VHyperEdgeShape temp = new VHyperEdgeShape(Knots, ControlPoints,weights,0); //Temporary Shape for the BasisFunctions
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
			double thisX = q.get(i).getX(), thisY = q.get(i).getY();
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
		temp.setCurveTo(Knots, ControlPoints, weights);
		return temp;
	}

	/**
	 * Based on Grahams Scan this Method creates a convex hull and calculates Interpolation-Points that are at least
	 * The size of a node plus the minimal Distance away from the node 
	 * 
	 */
	private static VHyperEdgeShape CreateConvexHullPolygon(Vector<Point2D> nodes, Vector<Integer> sizes, int degree, int distance)
	{
		if (nodes.size()<2) //mindestens 3 Knoten nötig
			return new VHyperEdgeShape();
		Vector<Point2D> ConvexHull = GrahamsScan(nodes);
		//Move each point by Size+distance away from center of convex Hull
		double midX=0d, midY=0d;
		for (int i=0; i<ConvexHull.size(); i++)
		{
			midX += ConvexHull.get(i).getX();
			midY += ConvexHull.get(i).getY();
		}
		Point2D.Double mid = new Point2D.Double(midX/(double)ConvexHull.size(), midY/(double)ConvexHull.size());
		for (int i=0; i<ConvexHull.size(); i++) //Move Each Point of the Convex Hull
		{
			double thisX = ConvexHull.get(i).getX();
			double thisY = ConvexHull.get(i).getY();
			int pos = nodes.indexOf(ConvexHull.get(i));
			Point2D direction = new Point2D.Double(thisX-mid.getX(), thisY-mid.getY());
			double length = direction.distance(0d,0d);
			ConvexHull.set(i, new Point2D.Double(thisX + direction.getX()/length*(distance+sizes.get(pos)), thisY + direction.getY()/length*(distance+sizes.get(pos))));
		}
		ConvexHull.add((Point2D) ConvexHull.firstElement().clone());
		if (ConvexHull.size()<=degree)
			return new VHyperEdgeShape();
		return CreateInterpolation(ConvexHull, degree);
	}
	
	public static Vector<Point2D> GrahamsScan(Vector<Point2D> nodes)
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
	private static double CrossProduct(Point2D p1, Point2D p2, Point2D p3)
	{
       return (p2.getX() - p1.getX())*(p3.getY() - p1.getY()) - (p3.getX() - p1.getX())*(p2.getY() - p1.getY());
	}
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
