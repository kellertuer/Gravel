package model;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.Vector;

public class NURBSShapeFactory {

	public final static int CIRCLE_ORIGIN = 1;
	public final static int CIRCLE_RADIUS = 2;
	
	public final static int DISTANCE_TO_NODE = 3;
	
	public final static int IP_POINTS = 4;
	public final static int DEGREE = 5;
	public final static int MAX_INDEX = 6;
	
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
			try {IP_Points = (Vector<Point2D>) Parameters.get(IP_POINTS);}
			catch (Exception e) {return new VHyperEdgeShape();} //Empty Shape
			int degree;
			try	{degree = Integer.parseInt(Parameters.get(DEGREE).toString());}
			catch (Exception e) {return new VHyperEdgeShape();}
			return CreateInterpolation(IP_Points, degree);
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
		System.err.println("Created with"+Knots.size()+" Knots, "+ControlPoints.size()+" CPs and "+weights.size()+"weights");
		temp.setCurveTo(Knots, ControlPoints, weights);
		return temp;
	}

}
