package model;

import java.awt.Point;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.Stack;
import java.util.Vector;

/**
 * This Class represents the Shape of an individual VHyperEdge in form of a NURBS
 * The nonuniform rational B-Spline may be
 * - clamped, that ist with a knot vektor t0=...=td and tm-d = ... = tm, so that the endpoints are interpolated
 * - unclamped, that is without those multiplicities, so that some intervals of the knotvektor are not in the curve
 *
 *
 * @see VHyperEdge
 * 
 * Other Methods are
 * - Min and Max of the Bounding Box of the ControlPolygon
 * - Addition (Refinement of Knots)
 * - Removal of Knots
 * - TODO ?get&set knots &weights?
 * 
 * 
 * @author Ronny Bergmann
 * @since 0.4
 */
public class NURBSShape {

	public final static int CLAMPED = 0;
	public final static int UNCLAMPED = 1;
	
	public final static int NO_DECORATION = 0;
	public final static int VALIDATOR = 1;
	public final static int FRAGMENT = 2;
	public final static int PROJECTION = 4;
	public static final int GRAPHML = 8;
	
	public Vector<Double> Knots;
	public Vector<Point2D> controlPoints; //ControlPoints, TODO: Set protected after DEBUG
	public Vector<Double> cpWeight;
	protected Vector<Point2dHom> controlPointsHom; //b in homogeneous coordinates multiplied by weight
	protected int NURBSType; //May be clamped or unclamped	
	//TODO: Set Protected after finishing debug
	public int maxKnotIndex, //The Knots are numbered 0,1,...,maxKnotIndex
				maxCPIndex; //The ControlPoints are numbered 0,1,...,maxCPIndex
	public int	degree; //Order of the piecewise polynomials - depends on the maxIndices Above: degree = maxKnotIndex-maxCPindex-1
	/**
	 * Create an empty NURBSShape,
	 * which has no controlpoints, weights nor knots
	 * this is never rendered anywhere and is used to indicate that 
	 * either nothing was done yet or the last try to get a shape went wrong
	 */
	public NURBSShape()
	{
		clear();
	}
	/**
	 * Init an NURBSShape.
	 * An NURBSShape is defined by a knotvektor (Knots or t), 
	 * its ControlPoints (CPoints or P_i),
	 * and weights for the ControlPoints that specify the influence of an individual ControlPoint to the curve
	 * 
	 * First a potential Degree (d) is computet (#Knots - #ControlPoints-1) and then the validity is checked, so if
	 * it is closed (first d+1 Knots are equal and last d+1 are also equal, kntot-vector is nondecreasing)
	 * @param pKnots Knots of the NURBS
	 * @param cpoints Controlpoints of the NURBS
	 * @param weights weights of the P_i 
	 */
	public NURBSShape(Vector<Double> pKnots, Vector<Point2D> CPoints, Vector<Double> weights)//, int degree)
	{
		setCurveTo(pKnots,CPoints,weights);
	}
	/**
	 * Private Constructor to (re)create with Homogeneous ControlPointVector
	 * @param knots
	 * @param pPw
	 */
	protected NURBSShape(Vector<Double> knots, Vector<Point2dHom> pPw)
	{
		Knots = new Vector<Double>();
		cpWeight = new Vector<Double>();
		controlPoints = new Vector<Point2D>();
		maxCPIndex=0; maxKnotIndex=0; degree=0;
		controlPointsHom = new Vector<Point2dHom>();
		controlPoints.setSize(pPw.size()); 
		cpWeight.setSize(pPw.size());
		for (int i=0; i<pPw.size(); i++)
		{
			double stw = pPw.get(i).w;
			if (stw==0)
				controlPoints.set(i,new Point2D.Double(pPw.get(i).x,pPw.get(i).y));
			else
				controlPoints.set(i,new Point2D.Double(pPw.get(i).x/pPw.get(i).w,pPw.get(i).y/pPw.get(i).w));
			cpWeight.set(i,stw);
		}
		setCurveTo(knots, controlPoints, cpWeight);
	}
	/**
	 * Set the Curve to another NURBS
	 * @param pKnots
	 * @param CPoints
	 * @param weights
	 */
	public void setCurveTo(Vector<Double> pKnots, Vector<Point2D> CPoints, Vector<Double> weights)
	{
		int result = validate(pKnots, CPoints, weights);
		if (result==-1)
		{
			clear();
			return;
		}
		NURBSType = result;
		Knots = pKnots;
		controlPoints = CPoints;
		cpWeight = weights;
		maxCPIndex = CPoints.size()-1;
		maxKnotIndex = Knots.size()-1;
		degree = Knots.size()-controlPoints.size()-1;
		if (!isEmpty())
			refreshInternalValues();
	}
	/**
	 * Return the type of NURBS-Curve that is contained in here
	 * @return
	 */
	public int getType()
	{
		return NURBSType;
	}
	/**
	 * Strip a NURBSShape off every Decorator
	 * This class just returns itself - every Decorator should call upser.strip, because then in the end this method is reached
	 * @return
	 */
	public NURBSShape stripDecorations()
	{
		return this;
	}
	/**
	 * Return all Decorations this class has - this class has no decoration
	 * @return
	 */
	public int getDecorationTypes()
	{
		return NO_DECORATION;
	}
	/**
	 * Check, whether the NURBSShape is empty
	 * if it is empty, many algorithms don't work at all
	 * @return
	 */
	public boolean isEmpty()
	{
		return (Knots.isEmpty()||controlPoints.isEmpty()||cpWeight.isEmpty());
	}
	/**
	 * Compares this Curve to another (minDist does not matter)
	 * if alle values of t,b,w are equal it returns true, else false
	 * 
	 * @param s another Shape
	 * @return true if Shape s is equal to this else false
	 */
	public boolean CurveEquals(NURBSShape s)
	{
		if ((s.controlPoints.size()!=controlPoints.size())||(Knots.size()!=s.Knots.size()))
			return false;
		Iterator<Point2D> bi = controlPoints.iterator();
		while (bi.hasNext())
		{
			Point2D p = bi.next();
			if (s.controlPoints.get(controlPoints.indexOf(p)).distance(p)!=0.0d)
				return false;
		}
		Iterator<Double> ti = Knots.iterator();
		while (ti.hasNext())
		{
			Double v = ti.next();
			if (s.Knots.get(Knots.indexOf(v)).compareTo(v)==0) //Equal
				return false;
		}
		Iterator<Double> wi = cpWeight.iterator();
		while (wi.hasNext())
		{
			Double v = wi.next();
			if (s.cpWeight.get(cpWeight.indexOf(v)).compareTo(v)==0) //Equal
				return false;
		}
		return true;
	}
	/**
	 * Check whether the given Knots, ControlPoints and weight specify a correct NURBS-Curve
	 * (1) #CP = #weights
	 * (2) clamped or unclamped Curve
	 * (3) For the Degree enough knots
	 * @param pKnots
	 * @param CPoints
	 * @param weights
	 * @return the type of curve if it is valid, else -1
	 */
	private int validate(Vector<Double> pKnots, Vector<Point2D> CPoints, Vector<Double> weights)
	{
		if ((pKnots.size()==0)&&(CPoints.size()==0)&&(weights.size()==0))
			return -1;
		if (CPoints.size()!=weights.size())
			return -1;
		for (int i=1; i<pKnots.size()-1; i++)
		{
			if (pKnots.get(i)<pKnots.get(i-1))
				return -1; //Not nondecreasing
		}
		int d = pKnots.size()-CPoints.size()-1;
		if (d<1)
			return -1;
		boolean clamped = true;
		if (pKnots.size() >= 2*d+2)
		{
			//Check for clamped Curve
			double first = pKnots.firstElement(), last=pKnots.lastElement();
			for (int i=1; i<=d; i++)
			{
				if (pKnots.get(i)!=first)
					clamped = false;
				if (pKnots.get(pKnots.size()-1-i)!=last)
					clamped = false;
			}
		}
		else
			clamped = false;
		if (clamped)
			return CLAMPED;
		//Check for unclamped needed?
		else
		{ 
			return UNCLAMPED;
		}
	}
	/**
	 * Empty this shape and set it to nonexistent
	 */
	protected void clear()
	{
		Knots = new Vector<Double>();
		cpWeight = new Vector<Double>();
		controlPoints = new Vector<Point2D>();
		maxCPIndex=0; maxKnotIndex=0; degree=0;
		controlPointsHom = new Vector<Point2dHom>();
	}
	/**
	 * Initialization of the internal homogeneous Vector
	 * Should be called everytime either the b or w vector are completly exchanged
	 */
	protected void refreshInternalValues()
	{
		controlPointsHom = new Vector<Point2dHom>();
		Iterator<Point2D> ib =  controlPoints.iterator();
		int i=0;
		while (ib.hasNext()) //Modify to be 3D Coordinates (homogeneous 2D)
		{
			Point2D p = ib.next();
			double weight = cpWeight.get(i);
			Point2dHom newp = new Point2dHom(p.getX(),p.getY(),weight);
			newp.set(newp.x*weight, newp.y*weight, weight);
			controlPointsHom.add(newp);
			i++;
		}
	}
	/**
	 * Return a complete independent Copy of this Shape
	 */
	public NURBSShape clone()
	{
		Vector<Double> k = new Vector<Double>();
		Iterator<Double> iter = Knots.iterator();
		while (iter.hasNext())
			k.addElement(new Double(iter.next().doubleValue()));
		
		Vector<Double> w = new Vector<Double>();
		iter = cpWeight.iterator();
		while (iter.hasNext())
			w.addElement(new Double(iter.next().doubleValue()));

		Vector<Point2D> p = new Vector<Point2D>();
		Iterator<Point2D> iter2 = controlPoints.iterator();
		while (iter2.hasNext())
		{
			Point2D next = iter2.next();
			p.addElement((Point2D) next.clone());			
		}
		return new NURBSShape(k,p,w);
	}
	/**
	 * Get Maximum (bottom right edge) of the CP bunding box
	 */
	public Point2D getMax()
	{
		double x = Double.MIN_VALUE;
		double y = Double.MIN_VALUE;
		Iterator<Point2D> bi = controlPoints.iterator();
		while (bi.hasNext())
		{
			Point2D p = bi.next();
			if (p.getX() > x)
				x = p.getX();
			if (p.getY() > y)
				y = p.getY();
		}
		return new Point2D.Double(x,y);
	}
	/**
	 * Get Minimum (top left edge) of the CP bunding box
	 */
	public Point2D getMin()
	{
		double x = Double.MAX_VALUE;
		double y = Double.MAX_VALUE;
		Iterator<Point2D> bi = controlPoints.iterator();
		while (bi.hasNext())
		{
			Point2D p = bi.next();
			if (p.getX() < x)
				x = p.getX();
			if (p.getY() < y)
				y = p.getY();
		}
		return new Point2D.Double(x,y);
	}
	/**
	 * Scale all Controlpoints by factor s, if you want to resize a shape
	 * make sure to translate its middle to 0,0 before and back afterwards
	 * @param s
	 */
	public void scale(double s)
	{
		scale(s,s);
	}
	/**
	 * Scale all Controlpoints by factor sx and sy in the directions X and Y, if you want to resize a shape
	 * make sure to translate its middle to 0,0 before and back afterwards
	 * @param s
	 */
	public void scale(double sx, double sy)
	{
		Iterator<Point2D> bi = controlPoints.iterator();
		while (bi.hasNext())
		{
			Point2D p = bi.next();
			p.setLocation(p.getX()*sx,p.getY()*sy);
		}
		//recalculate Homogeneous
		refreshInternalValues();		
	}
	/**
	 * Translate Curve - due to Translation Invariance, only the ControlPoints need to be moved
	 * @param x
	 * @param y
	 */
	public void translate(double x, double y)
	{
		Vector<Point2D> Q = new Vector<Point2D>();
		Iterator<Point2D> bi = controlPoints.iterator();
		while (bi.hasNext())
		{
			Point2D p = bi.next();
			Q.add(new Point2D.Double(p.getX()+x,p.getY()+y));
		}
		this.setCurveTo(Knots,Q,cpWeight);
	}
	/**
	 * Rotate the Curve - due to Rotation Invariance, only the ControlPoints need to be moved
	 * Center of Rotation is the Origin (0,0)
	 * 
	 * @param degree Amount of rotation - The Rotation is anticlockwise (for positive degree)
	 */
	public void rotate(double degree)
	{
		Vector<Point2D> Q = new Vector<Point2D>();
		Iterator<Point2D> bi = controlPoints.iterator();
		double rad = degree*Math.PI/180d;
		while (bi.hasNext())
		{
			Point2D p = bi.next(); //Next Point
			double x = p.getX()*Math.cos(rad) + p.getY()*Math.sin(rad);
			double y = -p.getX()*Math.sin(rad) + p.getY()*Math.cos(rad);
			Q.add(new Point2D.Double(x,y));
		}
		this.setCurveTo(Knots,Q,cpWeight);
	}
	/**
	 * Get the Curve as a piecewise approximated linear Java Path
	 * @param maxdist is the maximum distance of two consecutive Points in the resulting Path
	 * If you set this value too high, the computation time will increase significantly, if you set it too low
	 * the path only roughly represents your curve
	 * 
	 * @return
	 */
	public GeneralPath getCurve(double maxdist) //Adapt to a length on the curve?
	{
		GeneralPath path = new GeneralPath();
		if (isEmpty())
			return path;
		Stack<Point2D> calculatedPoints = new Stack<Point2D>();
		Stack<Double> calculatedParameters = new Stack<Double>();
		//Startpoint
		double actualu = Knots.get(degree);
		Point2D actualPoint = this.CurveAt(actualu);
		path.moveTo((float)actualPoint.getX(), (float)actualPoint.getY());
		//Init Stack with the endpoint
		calculatedParameters.push(Knots.get(maxKnotIndex-degree)); //Last value, that can be evaluated
		calculatedPoints.push(CurveAt(calculatedParameters.peek().doubleValue()));
		calculatedParameters.push((Knots.get(maxKnotIndex-degree)+Knots.get(degree))/2d); //Middle, because start and end ar equal
		calculatedPoints.push(CurveAt(calculatedParameters.peek().doubleValue()));
		//
		//Calculate values in between as long as they are not near enough
		//
		while(!calculatedPoints.empty())
		{
			Point2D comparePoint = calculatedPoints.peek();
			double compareu = calculatedParameters.peek();
			if (actualPoint.distance(comparePoint) <= maxdist) //these two are near enough
			{
				actualPoint = calculatedPoints.pop();
				actualu = calculatedParameters.pop();
				path.lineTo((float)actualPoint.getX(), (float)actualPoint.getY());
			}
			else //Not near enough - take middle between them and push that
			{
				double middleu = (compareu+actualu)/2d;
				if ((middleu==compareu)||(middleu==actualu)) //Bei Maschinengenauigkeit angelangt
				{
					actualPoint = calculatedPoints.pop();
					actualu = calculatedParameters.pop();
					path.lineTo((float)actualPoint.getX(), (float)actualPoint.getY());					
				}
				else
				{
					calculatedPoints.push(CurveAt(middleu));
					calculatedParameters.push(middleu);
				}
			}
		}
		return path;
	}
	/**
	 * Find the interval u \in [t.get(j),t.get(j+1)) and return the index j
	 * 
	 * because the first and last d+1 values of t are assumed equal, the 
	 * @param u
	 * @return
	 */
	protected int findSpan(double u)
	{
		if ((u<Knots.firstElement())||(u>Knots.lastElement())) //Out of range for all types
				return -1;
		if ((NURBSType&UNCLAMPED)==UNCLAMPED) //Unclamped Curve, starts with Knots.get(d) ends with maxCPIndex-d
		{
			if ((u<Knots.get(degree))||(u>Knots.get(maxKnotIndex-degree)))
					return -1;			
		}
		//Binary Search for the intervall
		int low = degree; //because the first d+1 are equal too
		int high = maxKnotIndex-degree; //see above
		if (u==Knots.get(high)) //Special case because the last intervall is not open to the right
			return high-1; //So t_{m-d} belongs just as endpoint to the curve Part [t_m-d-1,t_m-d]
		int mid = Math.round((low+high)/2);
		while ((u<Knots.get(mid)) || (u>=Knots.get(mid+1)))
		{ 
			if (u < Knots.get(mid))
					high = mid;
			else
				low = mid;
			mid = Math.round((low+high)/2);
		} //get the first t AFTER u in Variable j
		return mid;
	}
	/**
	 * Evaluate Curve at a given point relative on the Knot-vector,
	 * that is 0 ist the startpoint, 1 is the endpoint
	 * 
	 * @param u
	 * @return
	 */
	public Point2D CurveRelativeAt(double u)
	{
		if (u>1)
			u -= Math.floor(u);
		double a = Knots.get(degree);
		double b = Knots.get(maxKnotIndex-degree);
		double rel = a + (b-a)*u;
		return CurveAt(rel);
	}
	/**
	 * Evaluate Curve Derivatives a given point relative on the Knot-vector,
	 * that is 0 ist the startpoint, 1 is the endpoint
	 * 
	 * @param u relative position
	 * @param deriv, derivth derivate of the curve
	 * @return
	 */
	public Point2D DerivateCurveRelativeAt(int deriv,double u)
	{
		if (u>1)
			u -= Math.floor(u);
		double a = Knots.get(degree);
		double b = Knots.get(maxKnotIndex-degree);
		double rel = a + (b-a)*u;
		return DerivateCurveValuesAt(deriv,rel).get(deriv);
	}
	/**
	 * Evaluate the Curve at given point u \in [t_0,t_m]
	 * @param u
	 * @return
	 */
	public Point2D.Double CurveAt(double u)
	{	
		Point2dHom erg = deBoer3D(u); //Result in homogeneous Values on Our Points		
		if (erg==null)
		{
			System.err.println("NURBSShape::CurveAt "+u+" not in "+Knots.get(degree)+","+Knots.get(maxKnotIndex-degree)+"");
			return null;
		}
		if (erg.w==0) //
			return new Point2D.Double(erg.x,erg.y);
		else
			return new Point2D.Double(erg.x/erg.w,erg.y/erg.w);
		
	}
	public double WeightAt(double u)
	{
		Point2dHom erg = deBoer3D(u); //Result in homogeneous Values on Our Points
		if (erg==null)
		{
			System.err.println("NURBSShape::WeightAt3D: "+u+" not in "+Knots.get(degree)+","+Knots.get(maxKnotIndex-degree)+"");
			return Double.NaN;
		}
			return erg.w;
	}
	/**
	 * get the Value of all derivates up to derivate-th Degree of the NURBSShape at Position u
	 * @param pDegree
	 * @param u
	 * @return
	 */
	public Vector<Point2D> DerivateCurveValuesAt(int derivate, double u)
	{
		int maxMultiplicity = 1, temp=1;
		for (int i=degree+1; i<=maxKnotIndex-degree; i++)
		{
			if (Knots.get(i).doubleValue()==Knots.get(i-1).doubleValue())
				temp++;
			else if (temp > maxMultiplicity)
				maxMultiplicity = temp;
		}
//		if (Knots.indexOf(u)!=-1)
//			System.err.println("Wah! Max Mult: "+maxMultiplicity+" and Degree "+degree+" so it is "+(degree-maxMultiplicity)+" times diff");
		Vector<Point2D> CK = new Vector<Point2D>(); //result
		if (derivate==0)
		{
			CK.add(CurveAt(u));
			return CK;
		}
		Vector<Point2dHom> AdersWders = getDerivatesHomAt(derivate, u); //x,y are the Aders, z is wders
		CK.setSize(derivate+1);
		for (int k=0; k<=derivate; k++)
		{ //Calculate kth Derivate
			double vx = AdersWders.get(k).x;
			double vy = AdersWders.get(k).y;
			for (int i=1; i<=k; i++)
			{
				double factor = binomial(k,i)*AdersWders.get(i).w;
				vx -= factor*CK.get(k-i).getX();
				vy -= factor*CK.get(k-i).getY();
			}
			if ((AdersWders.get(0).w!=0.0)) //wders[0]!=0
			{
				vx /= AdersWders.get(0).w;
				vy /= AdersWders.get(0).w;
			}
			CK.set(k,new Point2D.Double(vx,vy));
		}
		return CK;
	}
	/**
	 * Calculate all Nonvanishing BasisFunctions at u
	 * This is a Variation of Alg 2.2 from the NURBS-Book
	 */
	private Vector<Vector<Double>> AllBasisFunctions(double u)
	{
		Vector<Vector<Double>> N = new Vector<Vector<Double>>();
		N.setSize(degree+1);
		int i = findSpan(u);
		if (i==-1)
			return N;
		for (int j=0; j<=degree; j++)
		{
			N.set(j, new Vector<Double>());
			N.get(j).setSize(degree+1);
		}				
		for (int deg=0; deg<=degree; deg++) //All Degrees less then degree
		{ //Inside this 
			Vector<Double> left = new Vector<Double>(); left.setSize(deg+1);
			Vector<Double> right = new Vector<Double>(); right.setSize(deg+1);
			N.get(0).set(deg,1.0);
			for (int j=1; j<=deg; j++) //All Basis Values of degree 
			{
				left.set(j,u-Knots.get(i+1-j));
				right.set(j,Knots.get(i+j)-u);
				double saved = 0d;
				for (int r=0; r<j; r++)
				{
					double temp = N.get(r).get(deg)/(right.get(r+1)+left.get(j-r));
					N.get(r).set(deg, saved+right.get(r+1)*temp);
					saved = left.get(j-r)*temp;
				}
				N.get(j).set(deg,saved);
			}
		}
		return N;
	}
	/**
	 * Compute ControlPoints of the Derivatives up to deriv
	 * Based on Alg 3.3 with 
	 * @param derivative
	 */
	private Vector<Vector<Point2dHom>> CurveDerivativeControlPointsHom(int d, int r1, int r2)
	{ //n==maxCPIndex, d<=degree, p==degree, U==Knots, P==ControlPointsHom
		//
		Vector<Vector<Point2dHom>> PK = new Vector<Vector<Point2dHom>>();
		if (d>degree)
			return PK;
		PK.setSize(d+1);
		int r = r2-r1;
		PK.set(0,new Vector<Point2dHom>());PK.get(0).setSize(r+1);
		for (int i=0; i<=r; i++)
			PK.get(0).set(i, (Point2dHom) controlPointsHom.get(r1+i).clone());
		for (int k=1; k<=d; k++) //through all derivatives
		{
			PK.set(k,new Vector<Point2dHom>()); PK.get(k).setSize(r-k+1);
			int tmp = degree-k+1;
			for (int i=0; i<=r-k; i++)
			{ //Code from p. 99
				double denom = Knots.get(r1+i+degree+1)-Knots.get(r1+i+k);
				double newx = tmp*(PK.get(k-1).get(i+1).x - PK.get(k-1).get(i).x)/denom;
				double newy = tmp*(PK.get(k-1).get(i+1).y - PK.get(k-1).get(i).y)/denom;
				double newz = tmp*(PK.get(k-1).get(i+1).w - PK.get(k-1).get(i).w)/denom;
				PK.get(k).set(i, new Point2dHom(newx,newy,newz));
			}
		}	
		return PK;
	}
	public Vector<Point2dHom> getDerivatesHomAt(int d, double u)
	{//n==maxCPIndex, p==degree, U==Knots, P==controlPointsHom
		Vector<Point2dHom> CK = new Vector<Point2dHom>();
		CK.setSize(d+1);
		int du = Math.min(d,degree);
		for (int k=degree+1; k<=degree; k++)
			CK.set(k, new Point2dHom(0d,0d,0d));
		int span = findSpan(u);
		Vector<Vector<Double>> N = AllBasisFunctions(u);
		Vector<Vector<Point2dHom>> PK = CurveDerivativeControlPointsHom(du,span-degree,span);
		for (int k=0; k<=du; k++)
		{
			CK.set(k,new Point2dHom(0d,0d,0d));
			for (int j=0; j<=degree-k; j++)
			{
				double newx = CK.get(k).x + N.get(j).get(degree-k)*PK.get(k).get(j).x;
				double newy = CK.get(k).y + N.get(j).get(degree-k)*PK.get(k).get(j).y;
				double newz = CK.get(k).w + N.get(j).get(degree-k)*PK.get(k).get(j).w;
				CK.set(k, new Point2dHom(newx,newy,newz));
			}
		}
		return CK;
	}
	/**
	 * Calulation of Alpha, refer to deBoer-Algorithm
	 * @param u
	 * @param i
	 * @param j
	 * @return
	 */
	private double alpha(double u,int i, int j)
	{
		if ((u==Knots.get(i)&&(Knots.get(i+degree-j+1)==Knots.get(i))))
			return 0;
		return (u-Knots.get(i))/(Knots.get(i+degree-j+1)-Knots.get(i));
	}
	/**
	 * Calculate the Value of the NURBSCurve in homogeneous Coordinates iterative
	 * 
	 * This method works for 2d homogeneous or 3d Stuff.
	 *
	 * @param u Point u \in [a,b], whose point we want
	 * @return a 3d-Value of the Point in the Curve or null if u is out of range
	 */
	private Point2dHom deBoer3D(double u)
	{
		int i = findSpan(u);
		if (i==-1)
			return null;
		Vector<Point2dHom> fixedj = new Vector<Point2dHom>();
		fixedj.setSize(degree+1); //for values 0,...,d, because only d+1 Basis Functions are nonzero
		//Init with the Points
		for (int l=i; l>=i-degree; l--) //Beginning with i,0 up to i-d,0
		{
			fixedj.set(l-i+degree,controlPointsHom.get(l));
		}
		
		for (int k=1; k<=degree; k++) //Compute higher and hihger values of the degree
		{
			for (int l=i; l>=i-degree+k; l--) //Stop each iteration one earlier
			{
				Point2dHom bimjm = fixedj.get(l-i+degree-1);//b_i-1^j-1
				double alpha = alpha(u,l,k);
				Point2dHom bijm = fixedj.get(l-i+degree);
				double x = (1-alpha)*bimjm.x + alpha*bijm.x;
				double y = (1-alpha)*bimjm.y + alpha*bijm.y;
				double z = (1-alpha)*bimjm.w + alpha*bijm.w;
				fixedj.set(l-i+degree,new Point2dHom(x,y,z));
				//System.err.println("Computing ("+l+","+k+") :"+fixedj.get(l-i+j));
				//saving in "+(l-i+j)+" based on pervious values in "+(l-i+j-1)+" and "+(l-i+j)+".");
			}
		}
		return fixedj.get(degree);
	}
	/**
	 * Calculate all Nonzero BSpline-Functions of this NURBS.
	 * These are all N_i,d with i between findSpan(u) (first t less or equal u) and 
	 * findSpan(u)-d due to locality of the functions
	 * 
	 * The Nonzero Functions are returned in an compressed array, so
	 * the least entry of the Vector (V.get(0)) is the Function N_i,d, i=findSpan(u)-d
	 * and the last the one with i=findSpan(u)
	 * 
	 * @param u
	 * @return All nonzero Values of BSpline-Basis-Functions at u
	 */
	protected Vector<Double> BasisBSpline(double u)
	{
		//Calculate all Needed Values
		int max = findSpan(u); //Only constant function that is nonzero
		//this is also the maximum value that is nonzero in the resulting BasisN
		int min = max-degree; //minimum nonzero function in N due to loaclity
		Vector<Double> N = new Vector<Double>();
		N.setSize(degree+1); //Due to locality only d+1 values (from min to max) are needed
		for (int k=0; k<degree; k++)
		{
			N.set(k,0.0d); //Set min+k to zero
		}
		N.set(degree, 1.0); //The highest constant (N_i,0) is 0 all others are zero
		for (int actualDegree=1; actualDegree<=degree; actualDegree++) //calcutlate higher degrees
		{
			//Max-actualDegree is the first nonzero function, max the last
			for (int k=max-actualDegree; k<=max; k++)
			{ //Calculate N_k,actualDegree
				double fac1,fac2;
				if (Knots.get(k+actualDegree).doubleValue()==Knots.get(k).doubleValue()) //divided by 0 -> per Def 1
					fac1=0.0d;
				else
					fac1 = (u-Knots.get(k))/(Knots.get(k+actualDegree)-Knots.get(k));
				if (Knots.get(k+actualDegree+1).doubleValue()==Knots.get(k+1).doubleValue()) //divided by 0 per Def. 1
					fac2=0.0d;
				else
					fac2 = (Knots.get(k+actualDegree+1)-u)/(Knots.get(k+actualDegree +1)-Knots.get(k+1));
				
				double kth = N.get(k-min); //N_k of value one degree less (shiftet by min)
				double kpth = 0.0d; //N_k+1 of previous degree init with 0, because it stays 0 if k=max
				if (k<max)
					kpth = N.get(k+1-min); //shiftet by min
				
				N.set(k-min, fac1*kth + fac2*kpth); //Override kth entry because it is not needed anymore
				//Calculate N_k,actualDegree
			}
		}
		return N;
	}
	/**
	 * Get the Value of the ith NURBS-BasisFunction R_number,d at Point u
	 * @param u
	 * @param number
	 * @return
	 */
	private double BasisR(double u, int number)
	{
		int max = findSpan(u);
		int min = max - degree;
		if ((number<min)||(number>max))
			return 0.0d; //Due to locality R_i,d is zero in these cases
		//Else Compute BSpline-Basis
		Vector<Double> N = BasisBSpline(u);		
		double nomin = N.get(number-min)*cpWeight.get(number); //get Specific B-Spline (see function above shiftet by min multiply by its weight
		double denomin = 0.0d;
		for (int k=0; k<=degree; k++)
		{
			denomin += N.get(k)* cpWeight.get(k+min); //See above shiftet by min
		}
		return (nomin/denomin);
	}
	/**
	 * Get the ControlPoint of the NURBSShape that is the nearest ofthe Point m
	 * @param m
	 * @return
	 */
	public Point2D getNearestCP(Point m) {
		double mindist = Double.MAX_VALUE;
		Point2D result = null;
		Iterator<Point2D> bi = controlPoints.iterator();
		while (bi.hasNext())
		{
			Point2D p = bi.next();
			double dist = p.distance(m);
			if (dist < mindist)
			{
				mindist = dist;
				result = p;
			}
		}
		return result;
	}
	/**
	 * Check whether a given Point is in Range of the Curve.
	 * The Range or max distance is the variance so if
	 * @param x the specified point has maximum
	 * @param variance the value specified
	 * @return this method returns true, else false
	 */
	public boolean isPointOnCurve(Point2D x, double variance)
	{
		return x.distance(ProjectionPoint(x))<=variance;
	}
	/**
	 * After working on the front or end of the curve, it might be neccessary for unclamped closed curves to update
	 * the other end,
	 * 
	 * The Parameters specifies, whch values are changed
	 * if changeFront = true
	 * 		the last values are taken and updated at the front
	 * if changeFront = false
	 * 		the first values are taken and updated at the end
	 * 
	 * @param changeFront
	 */
	protected void updateCircular(boolean changeFront)
	{
		double offset = Knots.get(maxKnotIndex-degree)-Knots.get(degree);
		//Update Circular Part front
		if (changeFront)
		{
			for (int j=0; j<degree; j++)
				Knots.set(j, Knots.get(maxKnotIndex-2*degree+j).doubleValue()-offset);
			for (int j=0; j<degree; j++)
			{
				controlPoints.set(j, (Point2D) controlPoints.get(maxCPIndex-degree+1+j).clone());
				cpWeight.set(j, cpWeight.get(maxCPIndex-degree+1+j).doubleValue());
			}
		}
		else //Update last values
		{
			for (int j=0; j<degree; j++)
				Knots.set(maxKnotIndex-degree+j, Knots.get(degree+j).doubleValue()+offset);
			for (int j=0; j<degree; j++)
			{
				controlPoints.set(maxCPIndex-degree+1+j, (Point2D) controlPoints.get(j).clone());
				cpWeight.set(maxCPIndex-degree+1+j, cpWeight.get(j).doubleValue());
			}

		}
		refreshInternalValues();
	}
	/**
	 * Add a single Knot at u, if it's in range, update circular if it's an unclamped curve
	 * @param u
	 */
	public void addKnot(double u)
	{
		if ((Knots.contains(u))||(u < Knots.firstElement())||(u>Knots.lastElement()))
			return;
		int i = findSpan(u);
		Vector<Double> ref = new Vector<Double>();
		ref.add(u);
		RefineKnots(ref);
		if ((getType()&UNCLAMPED)!=UNCLAMPED)
			return;
		if (i<=2*degree) //Front changed
			updateCircular(false); //Update end
		else if (i>=maxKnotIndex-2*degree)
			updateCircular(true); //Update front
	}
	/**
	 * Remove a Knot if the Curve C(Knot(get(i)) is at most tol away from p
	 * @param p any point p that is in Distance of at most
	 * @param tol to the Curvepoint represented by a Knot
	 * 
	 * @return success of removal
	 */
	public boolean removeKnotNear(Point2D p, double tol)
	{
		int knotIndex=0;
		if (isEmpty())
			return false;
		if (maxCPIndex<=2*degree+1)
			return false; //Too less Knots
		double minDist = Double.MAX_VALUE;
		for (int i=degree+1; i<maxKnotIndex-degree; i++) //Search for Knot with minimum Distance to p
		{
			Point2D actualKnotPoint  = CurveAt(Knots.get(i));
			if (actualKnotPoint.distance(p) <= minDist)
			{
				knotIndex = i;
				minDist = actualKnotPoint.distance(p);
			}
		}
		if (minDist>tol) //Minimum Knot too far away from p
			return false;
		//Multiplicity of knotIndex
		int mult = knotIndex;
		while (Knots.get(mult)==Knots.get(knotIndex))
			mult++;
		mult = mult - knotIndex; //Is the multiplicity
		//Removal with a simplificated version of ALGORITHM 5.8 from NURBSBook
		double knotu = Knots.get(knotIndex);
		int lastAffected = knotIndex-mult, firstAffected = knotIndex-degree, offSet = firstAffected-1; //OffSet from temporary Vector to original
		Vector<Point2dHom> temp = new Vector<Point2dHom>();
		temp.setSize(lastAffected-offSet+2);
		temp.set(0,(Point2dHom)controlPointsHom.get(offSet).clone()); temp.set(lastAffected-offSet+1, (Point2dHom)controlPointsHom.get(lastAffected+1).clone());
		int i=firstAffected, j=lastAffected, ii=1, jj=lastAffected-offSet; //i,j are the values of the actual Controlpoints, ii,jj are those of the temporary Vector
		while ((j-i) > 0) //Just one removal, t=0 from the algorithm
		{
			double alphi = (knotu-Knots.get(i).doubleValue()) / (Knots.get(i+degree+1)-Knots.get(i));
			double alphj = (knotu-Knots.get(j).doubleValue()) / (Knots.get(j+degree+1)-Knots.get(j));
			Point2dHom newii = (Point2dHom) controlPointsHom.get(i).clone();
			Point2dHom newiisummand = (Point2dHom)temp.get(ii-1).clone();
			newiisummand.scale(-1d*(1d-alphi));	newii.add(newiisummand);newii.scale(alphi);
			temp.set(ii,newii);
			Point2dHom newjj = (Point2dHom) controlPointsHom.get(j).clone();
			Point2dHom newjjsummand = (Point2dHom)temp.get(jj+1).clone();
			newjjsummand.scale(-1d*(alphj)); newjj.add(newjjsummand);newjj.scale(1d-alphj);
			temp.set(jj,newjj);
			i++; ii++; j--; jj--;
		}
		for (int k=0; k<temp.size(); k++)
		{
			if (temp.get(k)!=null)
			{
				if (temp.get(k).w<0)
				{
					System.err.println("NURBSShape::removeKnoteNear() ... Can't remove Knot!");
					return false;
				}
			}
		}
		//Leave out the check for removal, we have undo, save new cp (see algorithm and think about t=1)
		i=firstAffected; j=lastAffected;
		while ((j-i) > 0)
		{
			controlPointsHom.set(i, temp.get(i-offSet));
			controlPointsHom.set(j, temp.get(j-offSet));
			i++; j--;
		}
		//Shift unaffected Knots
		for (int k=knotIndex+1; k<=maxKnotIndex; k++)
			Knots.set(k-1, Knots.get(k).doubleValue());
		Knots.setSize(Knots.size()-1); //Remove last Element
		//Shift all unaffected ControlPoints
		int firstCPOut = (2*knotIndex-mult-degree)/2;
		for (int k=firstCPOut+1; k<=maxCPIndex; k++)
			controlPointsHom.set(k-1, controlPointsHom.get(k));
		//Remove Last Element
		controlPointsHom.setSize(controlPointsHom.size()-1);
		//Recompute Points & weights
		controlPoints = new Vector<Point2D>(); 
		cpWeight = new Vector<Double>();
		Iterator<Point2dHom> Pwi = controlPointsHom.iterator();
		while (Pwi.hasNext())
		{
			Point2dHom p1 = Pwi.next();
			if (p1.w==0)
				controlPoints.add(new Point2D.Double(p1.x,p1.y));
			else
				controlPoints.add(new Point2D.Double(p1.x/p1.w, p1.y/p1.w));
			cpWeight.add(p1.w);
		}
		maxCPIndex = controlPoints.size()-1;
		maxKnotIndex = Knots.size()-1;
		degree = Knots.size()-controlPoints.size()-1;
		refreshInternalValues();
		if (knotIndex<=2*degree)
			updateCircular(false);
		else if (knotIndex>=(maxKnotIndex+1-2*degree)) //+1 for the old values
			updateCircular(true);
		return true;
	}
	/**
	 * Refine the Curve to add some new knots contained in X from wich each is between t[0] and t[m]
	 * This Method does not care about circular closed curves
	 * If you have any vector refining first AND last degree Elements somewhere
	 * Split it Refine the front, call CircularUpdate(false) and Refine end (and call CircularUpdate(true))
	 * @param X
	 */
	protected void RefineKnots(Vector<Double> X)
	{
		if (isEmpty())
			return;
		Iterator<Double> testI = X.iterator();
		while (testI.hasNext())
		{
			double thisx = testI.next().doubleValue();
			if ((thisx < Knots.firstElement())||(thisx>Knots.lastElement())) //Out Of Range
				return;
		}
		//Compare The NURBS Book A5.4
		int a = findSpan(X.firstElement()), b=findSpan(X.lastElement())+1;
		if ((a==-1)||(b==0))
			return; //Out of range
		Vector<Point2dHom> newPw;
		newPw = new Vector<Point2dHom>();
		newPw.setSize(controlPointsHom.size()+X.size());
		Vector<Double> newt = new Vector<Double>();
		newt.setSize(Knots.size()+X.size());
		for (int j=0; j<=a-degree; j++)//Copy the first not changed values of the CPs
			newPw.set(j, (Point2dHom) controlPointsHom.get(j).clone());
		for (int j=b-1; j<=maxCPIndex; j++)//Copy the last not changed values of the CPs
			newPw.set(j+X.size(), (Point2dHom) controlPointsHom.get(j).clone());
		for (int j=0; j<=a; j++)//Copy the first not changed values of t
			newt.set(j, Knots.get(j).doubleValue());
		for (int j=b+degree; j<=maxKnotIndex; j++)//Copy the last not changed values of t
			newt.set(j+X.size(), Knots.get(j).doubleValue());
		
		int i=b+degree-1; //Last Value that's new in t
		int k=b+degree+X.size()-1; //Last Value that's new in Pw
		for (int j=X.size()-1; j>=0; j--) //Insert new knots backwards beginning at X.lastElement
		{ 
			while ((X.get(j) <= Knots.get(i)) && (i > a)) //These Values are not affected by Insertion of actual Not, copy them
			{
				newPw.set(k-degree-1, (Point2dHom) controlPointsHom.get(i-degree-1).clone());
				newt.set(k, Knots.get(i).doubleValue());
				k--;i--;
			}
			newPw.set(k-degree-1, (Point2dHom) newPw.get(k-degree).clone());
			for (int l=1; l<=degree; l++)
			{
				int actualindex = k-degree+l;
				double alpha = newt.get(k+l).doubleValue()-X.get(j).doubleValue();
				if (Math.abs(alpha) == 0.0d)
					newPw.set(actualindex-1, (Point2dHom) newPw.get(actualindex).clone());
				else
				{
					alpha = alpha/(newt.get(k+l).doubleValue()-Knots.get(i-degree+l).doubleValue());
					Point2dHom p1 = (Point2dHom) newPw.get(actualindex-1).clone();
					p1.scale(alpha);
					Point2dHom p2 = (Point2dHom) newPw.get(actualindex).clone();
					p2.scale(1.0d - alpha);
					p1.add(p2);
					newPw.set(actualindex-1,p1);
				}
			} //All Points recomputed for this insertion
			newt.set(k, X.get(j).doubleValue());
			k--;
		}
		//Recompute Points & weights
		controlPoints = new Vector<Point2D>(); 
		cpWeight = new Vector<Double>();
		Iterator<Point2dHom> Pwi = newPw.iterator();
		while (Pwi.hasNext())
		{
			Point2dHom p1 = Pwi.next();
			if (p1.w==0)
				controlPoints.add(new Point2D.Double(p1.x,p1.y));
			else
				controlPoints.add(new Point2D.Double(p1.x/p1.w, p1.y/p1.w));
			cpWeight.add(p1.w);
		}
		Knots = newt;
		maxCPIndex = controlPoints.size()-1;
		maxKnotIndex = Knots.size()-1;
		degree = Knots.size()-controlPoints.size()-1;
		refreshInternalValues();
	}
	/**
	 * For Display-Purposes - get the Points that lie on the curve at the knot-parameter-points
	 * t[degree+1],...,t[maxKnot-degree-1]
	 * That displays all Knots that are removable
	 * @return
	 */
	public Vector<Point2D> getRemovableKnotPoints()
	{
		Vector<Point2D> KnotPoints = new Vector<Point2D>();
		for (int i=degree+1; i<maxKnotIndex-degree; i++)
		{
			KnotPoints.add(CurveAt(Knots.get(i)));
		}
		return KnotPoints;
	}
	public Point2D ProjectionPoint(Point2D d)
	{
		NURBSShapeProjection projection = new NURBSShapeProjection(this,d);
		return projection.getResultPoint();
	}
	/**
	 * Projects the point d to a point, whose distance is minimal to d and on the curve
	 * 
	 * @param d
	 * @return
	 */
	public double ProjectionPointParameter(Point2D d)
	{
		NURBSShapeProjection projection = new NURBSShapeProjection(this,d);
		return projection.getResultParameter();
	}
	/**
	 * If the first Point lies on the Curve (given a specific variance, e.g. 2.0
	 * the point is moved to the second argument and the Curve is Computed again.
	 * @param src
	 * @param dest
	 * @return
	 */
	public void movePoint(double position, Point2D dest)
	{
		Point2D src = CurveAt(position);
		//Find the specific P, which has the most influence at src to move.
		double min = Double.MAX_VALUE;
		int Pindex=0;
		for (int i=0; i<maxCPIndex; i++)
		{
			double nodei = 0.0d;
			for (int j=1; j<=degree; j++)
			{
				nodei = nodei + (double)Knots.get(i+j);
			}
			nodei = nodei/degree;
			if (Math.abs(position-nodei)<min)
			{
				Pindex=i;
				min = Math.abs(position-nodei);
			}
		}
		Point2D Pk = controlPoints.get(Pindex);		
		Point2D direction = new Point2D.Double(dest.getX()-src.getX(),dest.getY()-src.getY());
		double mov = BasisR(position,Pindex);
		Point2D.Double Pnew = new Point2D.Double(
				Pk.getX() + direction.getX()/mov,
				Pk.getY() + direction.getY()/mov		
		);
		controlPoints.set(Pindex,Pnew);
		if ((getType()&UNCLAMPED)==UNCLAMPED) //unclamped is always used as periodic
		{
			if (Pindex<degree) //first degree ones -> move last degree ones
				controlPoints.set(maxCPIndex-degree+Pindex+1, (Point2D) Pnew.clone());
			else if (Pindex > maxCPIndex-degree) //last degree ones -> move first
				controlPoints.set(Pindex-1- maxCPIndex+degree, (Point2D) Pnew.clone());
		}
		boolean closedclamped = ( ((getType()&CLAMPED)==CLAMPED) //Clamped but at least closed
				&& (controlPoints.get(0).getX()==controlPoints.get(maxCPIndex).getX())
				&& (controlPoints.get(0).getY()==controlPoints.get(maxCPIndex).getY()));
		if (closedclamped)
		{
			if (Pindex==0) //first -> move last
				controlPoints.set(maxCPIndex, (Point2D) Pnew.clone());
			else if (Pindex==maxCPIndex) // last->move first
				controlPoints.set(0, (Point2D) Pnew.clone());
		}
		refreshInternalValues();
	}
	// return integer nearest to x
	long nint(double x)
	{
		if (x < 0.0) return (long) Math.ceil(x - 0.5);
	      return (long) Math.floor(x + 0.5);
	}
	// return log n!
	double logFactorial(int n) {
		double ans = 0.0;
		for (int i = 1; i <= n; i++)
			ans += Math.log(i);
		return ans;
	}
	// return the binomial coefficient n choose k.
	long binomial(int n, int k)
	{
		return nint(Math.exp(logFactorial(n) - logFactorial(k) - logFactorial(n-k)));
	}
	public int getDegree() {
		return degree;
	}
}
