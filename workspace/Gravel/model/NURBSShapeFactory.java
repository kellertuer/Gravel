package model;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.Vector;

public class NURBSShapeFactory {

	public final static int CIRCLE_ORIGIN = 1;
	public final static int CIRCLE_RADIUS = 2;
	
	public final static int DISTANCE_TO_NODE = 3;
	public final static int MAX_INDEX = 4;
	
	public static VHyperEdgeShape CreateShape(String type, Vector<Object> Parameters)
	{
		int distance;
		try	{distance = Integer.parseInt(Parameters.get(DISTANCE_TO_NODE).toString());}
		catch (Exception e) {return null;}
		if (type.toLowerCase().equals("circle"))
		{
			int radius;
			try	{radius = Integer.parseInt(Parameters.get(CIRCLE_RADIUS).toString());}
			catch (Exception e) {return null;}
			return CreateCircle((Point) Parameters.get(CIRCLE_ORIGIN), radius, distance);
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
}
