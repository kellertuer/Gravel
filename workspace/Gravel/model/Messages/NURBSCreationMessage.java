package model.Messages;

import java.awt.geom.Point2D;
import java.util.Vector;

import model.NURBSShape;
import model.NURBSShapeFragment;

/**
 * This Message contains all Information - including the type - needed for the calculation/creation of a NURBSCurve
 * 
 * The general Information contains
 * - a Degree of the B-Splines 
 * - a Margin from each node to the nodes inside the shape  
 * - a Status for the creation type, (if existent)
 * - a Set of Points that may be used different for the types of creation
 *  -a Set of Integers that may be used different for the types of creation
 *  -a NURBSShape that may be a Fragment or another Decorator to handle specific Creations depenting on them
 *
 * == Types ==
 * - Interpolation
 *  * Degree is needed
 *  * margin and Set of Integer not required
 *  * Status may be ADD_END or ADD_BETWEEN and indicates where new Interpolation Points are added
 *  
 *  - Interpolation with replacement of a Subcurve
 *  * same as Interpolation, only one IP is needed, instead of an degree a NURBSShapeFragment is required
 *  
 * - Circle Creation
 *  * Degree is needed
 *  * margin and Status not required
 *  * The first Point of the Set is the Circle Origin
 *  * The first Integer of the Set is the Circle Radius
 *  
 *  - Convex Hull
 *  * Status optional
 *  - Degree and Margin required
 *  - Set of Points used for the node positions inside the shape
 *  - Set of Integer used for the sizes of the nodes (so both sizes must be equal)
 *  
 * @author Ronny Bergmann
 * @since 0.4
 *
 */
public class NURBSCreationMessage {

	public static final int INTERPOLATION = 0;
	public static final int CIRCLE = 1;
	public static final int CONVEX_HULL = 2; 

	public static final int ADD_END = 1;
	public static final int ADD_BETWEEN = 2;
	
	private int type, status=0, degree, margin=0;
	private Vector<Point2D> points=new Vector<Point2D>();
	private Vector<Integer> values=new Vector<Integer>();
	private NURBSShape curve=new NURBSShape();
	/**
	 * Create an invalid Message - ATTENTION: Type can't be changed, this Message is always invalid
	 */
	public NURBSCreationMessage()
	{
		type=-1;
	}
	/**
	 * Create an Interpoaltion Creation Message with
	 * @param deg its Degree
	 * @param status its Status (ADD_END or ADD_BETWEEN)
	 * @param IP the Interpolation Points
	 */
	public NURBSCreationMessage(int deg, int status, Vector<Point2D> IP)
	{
		if ((status!=ADD_END)&&(status!=ADD_BETWEEN))
		{ //set to invalid
			type=-1;
			return;
		}
		type = INTERPOLATION;
		degree = deg;
			
		this.status = status;
		points = IP;
	}

	/**
	 * Create an Interpoaltion Creation Message with
	 * @param sub a Part of a NURBSShape that is replaced by the interpolation stuff
	 * @param status its Status (ADD_END or ADD_BETWEEN)
	 * @param IP the Interpolation Points
	 */
	public NURBSCreationMessage(NURBSShapeFragment sub, int status, Vector<Point2D> IP)
	{
		if ((status!=ADD_END)&&(status!=ADD_BETWEEN))
		{ //set to invalid
			type=-1;
			return;
		}
		type = INTERPOLATION;
		curve = sub;
		
		this.status = status;
		points = IP;
	}

	/**
	 * Create an Circle Creation Message with
	 * @param deg Degree (not yet used, but is planned)
	 * @param origin the Circle Origin
	 * @param radius the Circle radius
	 */
	public NURBSCreationMessage(int deg, Point2D origin, int radius)
	{
		degree=deg;
		type = CIRCLE;
		points.add(origin);
		values.add(radius);
	}
	
	/**
	 * Create an Convex Hull Creation Message with
	 * @param deg Degree
	 * @param margin inner Margin
	 * @param nodes Nodepositions and
	 * @param sizes Sizes of the nodes
	 */
	public NURBSCreationMessage(int deg, int margin, Vector<Point2D> nodes, Vector<Integer> sizes)
	{
		type = CONVEX_HULL;
		degree = deg;
		this.margin = margin;
		points = nodes;
		values = sizes;
	}

	/**
	 * Clone this Message if it is valid
	 * Else an invalid Message is created
	 */
	public NURBSCreationMessage clone()
	{
		if (!isValid())
			return new NURBSCreationMessage();
		//all cases have points
		Vector<Point2D> pclone = new Vector<Point2D>();
		for (int i=0; i<points.size(); i++)
			pclone.add(new Point2D.Double(points.get(i).getX(), points.get(i).getY()));
		Vector<Integer> vclone = new Vector<Integer>();
		if ((type==CIRCLE)||(type==CONVEX_HULL))
		{
			for (int i=0; i<values.size(); i++)
				vclone.add(values.get(i).intValue());
		}
		switch(type)
		{
			case INTERPOLATION: 
				if (degree > 0)
					return new NURBSCreationMessage(degree, status, pclone);
				else
					return new NURBSCreationMessage((NURBSShapeFragment)curve,status,pclone);
			case CIRCLE:
				return new NURBSCreationMessage(degree, pclone.firstElement(), vclone.firstElement());
			case CONVEX_HULL:
				return new NURBSCreationMessage(degree, margin, pclone, vclone);
			default: return null;
		}
	}

	public boolean isValid()
	{
		switch (type)
		{
			case INTERPOLATION:
				return ((points!=null)&&(points.size()>=0)&&((status&(ADD_END|ADD_BETWEEN))>0)) //Both
				&& ( (degree>=1) //Normal IP
				   || ( (degree==0) && ((curve.getDecorationTypes()&NURBSShape.FRAGMENT)==NURBSShape.FRAGMENT))); //Sub replacement
			case CIRCLE: //TODO degree
				return (points!=null)&&(points.size()==1)&&(values!=null)&&(values.size()==1);
			case CONVEX_HULL:
				return (degree>=0)&&(margin>=0)&&(points!=null)&&(values!=null)&&(values.size()==points.size());
			default: 
				return false;
		}		
	}
	//
	// Get & Set
	//
	public int getDegree() {
		return degree;
	}
	public void setDegree(int degree) {
		this.degree = degree;
	}
	public int getMargin() {
		return margin;
	}
	public void setMargin(int margin) {
		this.margin = margin;
	}
	public Vector<Point2D> getPoints() {
		return points;
	}
	public void setPoints(Vector<Point2D> points) {
		this.points = points;
	}
	public Vector<Integer> getValues() {
		return values;
	}
	public void setValues(Vector<Integer> values) {
		this.values = values;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public int getStatus() {
		return status;
	}
	public int getType() {
		return type;
	}
	public NURBSShape getCurve() {
		return curve;
	}
	public void setCurve(NURBSShape curve) {
		if ((type==INTERPOLATION)&&((curve.getDecorationTypes()&NURBSShape.FRAGMENT)!=NURBSShape.FRAGMENT))
			this.curve = new NURBSShape();
		else
			this.curve = curve;
	}
}
