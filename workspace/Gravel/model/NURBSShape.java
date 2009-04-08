package model;

import java.awt.Point;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.Stack;
import java.util.Vector;

import javax.vecmath.Point3d;

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
 * - TODO Removal of knots (with or without tollerance? i think wihout because there is undo - soon)
 * - TODO ?get&set knots &weights?
 * - TODO ?In/Decrease Degree of the polynonials?
 * 
 * 
 * @author Ronny Bergmann
 *
 */
public class NURBSShape {

	protected Vector<Double> Knots;
	protected Vector<Double> cpWeight;
	public Vector<Point2D> controlPoints; //ControlPoints
	protected Vector<Point3d> controlPointsHom; //b in homogeneous coordinates multiplied by weight

	protected int maxKnotIndex, //The Knots are numbered 0,1,...,maxKnotIndex
				maxCPIndex, //The ControlPoints are numbered 0,1,...,maxCPIndex
				degree; //Order of the piecewise polynomials - depends on the maxIndices Above: degree = maxKnotIndex-maxCPindex-1
	/**
	 * Create an empty shape so nothing ever happens but its at least not null
	 */
	public NURBSShape()
	{
		Knots = new Vector<Double>();
		cpWeight = new Vector<Double>();
		controlPoints = new Vector<Point2D>();
		maxCPIndex=0; maxKnotIndex=0; degree=0;
		controlPointsHom = new Vector<Point3d>();
	}
	/**
	 * Init an HyperEdgeShape with
	 * @param pKnots nots of the NURBS
	 * @param cpoints Controlpoints of the NURBS
	 * @param weights weights of the CP 
	 * @param dist minimal distance the curve should have from each node (whose are not saved here)
	 */
	public NURBSShape(Vector<Double> pKnots, Vector<Point2D> CPoints, Vector<Double> weights)//, int degree)
	{
		setCurveTo(pKnots,CPoints,weights);
	}
	/**
	 * Set the Curve to another NURBS
	 * @param pKnots
	 * @param CPoints
	 * @param weights
	 */
	public void setCurveTo(Vector<Double> pKnots, Vector<Point2D> CPoints, Vector<Double> weights)
	{
		Knots = pKnots;
		controlPoints = CPoints;
		cpWeight = weights;
		maxCPIndex = CPoints.size()-1;
		maxKnotIndex = Knots.size()-1;
		degree = Knots.size()-controlPoints.size()-1;
		if (!isEmpty())
			InitHomogeneous();
	}
	/**
	 * Private Constructor to (re)create with Homogeneous ControlPointVector
	 * @param knots
	 * @param pPw
	 * @param dist
	 */
	protected NURBSShape(Vector<Double> knots, Vector<Point3d> pPw)
	{
		Knots = new Vector<Double>();
		cpWeight = new Vector<Double>();
		controlPoints = new Vector<Point2D>();
		maxCPIndex=0; maxKnotIndex=0; degree=0;
		controlPointsHom = new Vector<Point3d>();
		controlPoints.setSize(pPw.size()); 
		cpWeight.setSize(pPw.size());
		for (int i=0; i<pPw.size(); i++)
		{
			double stw = pPw.get(i).z;
			if (stw==0)
				controlPoints.set(i,new Point2D.Double(pPw.get(i).x,pPw.get(i).y));
			else
				controlPoints.set(i,new Point2D.Double(pPw.get(i).x/pPw.get(i).z,pPw.get(i).y/pPw.get(i).z));
			cpWeight.set(i,stw);
		}
		Knots = knots;
		maxCPIndex = controlPoints.size()-1;
		maxKnotIndex = Knots.size()-1;
		degree = Knots.size()-controlPoints.size()-1;
		InitHomogeneous();		
	}
	/**
	 * Initialization of the internal homogeneous Vector
	 * Should be called everytime either the b or w vector are completly exchanged
	 */
	private void InitHomogeneous()
	{
		controlPointsHom = new Vector<Point3d>();
		Iterator<Point2D> ib =  controlPoints.iterator();
		while (ib.hasNext()) //Modify to be 3D Coordinates (homogeneous 2D)
		{
			Point2D p = ib.next();
			double weight = cpWeight.get(controlPoints.indexOf(p));
			Point3d newp = new Point3d(p.getX(),p.getY(),weight);
			newp.set(newp.x*weight, newp.y*weight, weight);
			controlPointsHom.add(newp);
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
	 * Scale all Controlpoints by factor s, of you want to resize a shape
	 * make shure to translate its middle to 0,0 before and back afterwards
	 * @param s
	 */
	public void scale(double s)
	{
		Iterator<Point2D> bi = controlPoints.iterator();
		while (bi.hasNext())
		{
			Point2D p = bi.next();
			p.setLocation(p.getX()*s,p.getY()*s);
		}
		//recalculate Homogeneous
		InitHomogeneous();
		
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
		while (bi.hasNext())
		{
			Point2D p = bi.next(); //Next Point
			double rad = degree*Math.PI/180d;
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
		double actualu = Knots.firstElement();
		Point2D actualPoint = this.NURBSCurveAt(actualu);
		path.moveTo((float)actualPoint.getX(), (float)actualPoint.getY());
		//Init Stack with the endpoint
		calculatedParameters.push(Knots.lastElement());
		calculatedPoints.push(NURBSCurveAt(calculatedParameters.peek().doubleValue()));
		calculatedParameters.push((Knots.lastElement()+Knots.firstElement())/2d); //Middle, because start and end ar equal
		calculatedPoints.push(NURBSCurveAt(calculatedParameters.peek().doubleValue()));
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
				calculatedPoints.push(NURBSCurveAt(middleu));
				calculatedParameters.push(middleu);
			}
		}
		return path;
	}
	public boolean isEmpty()
	{
		return (Knots.isEmpty()||controlPoints.isEmpty()||cpWeight.isEmpty());
	}
	/**
	 * Find the interval u \in [t.get(j),t.get(j+1)] and return the index j
	 * 
	 * because the first and last d+1 values of t are assumed equal, the 
	 * @param u
	 * @return
	 */
	public int findSpan(double u)
	{
		if (u==Knots.lastElement())
		{
			return Knots.indexOf(Knots.lastElement())-1; //first value of t equal to t.get(m)==t.lastElement - which is m-d		
		}
		//Binary Search for the intervall
		int low = degree; //because the first d+1 are equal too
		int high = maxKnotIndex-degree; //see above
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
	 * Private Method to evaluate the Curve at given point u \in [t_0,t_m]
	 * @param u
	 * @return
	 */
	public Point2D.Double NURBSCurveAt(double u)
	{	
		Point3d erg = deBoer3D(u); //Result in homogeneous Values on Our Points		
		if (erg.z==0) //
			return new Point2D.Double(erg.x,erg.y);
		else
			return new Point2D.Double(erg.x/erg.z,erg.y/erg.z);
		
	}
	/**
	 * get the Value of the degree-th Derivate of this NURBS at Position u
	 * @param pDegree
	 * @param u
	 * @return
	 */
	public Point2D DerivateCurveAt(int derivate, double u)
	{
		if (derivate==0)
			return NURBSCurveAt(u);
		Vector<Point3d> DerivatesBSpline = new Vector<Point3d>();
		DerivatesBSpline = DerivateValues(derivate, u);
		Vector<Point2D> DerivatesNURBS = new Vector<Point2D>();
		DerivatesNURBS.setSize(derivate);
		DerivatesNURBS.set(0,NURBSCurveAt(u));
		for (int k=1; k<=derivate; k++)
		{ //Calculate kth Derivate
			Point2D.Double thisdeg = new Point2D.Double(DerivatesBSpline.get(k).x,DerivatesBSpline.get(k).y);
			double denominator = deBoer3D(u).z;
			for (int i=1; i<=k; i++)
			{
				double factor = binomial(k,i)*DerivatesBSpline.get(i).z;
				Point2D prev = (Point2D) DerivatesNURBS.get(k-i).clone();
				thisdeg.x = thisdeg.x - prev.getX()*factor;
				thisdeg.y = thisdeg.y - prev.getY()*factor;
			}
			if (denominator!=0.0)
			{
				thisdeg.x = thisdeg.x/denominator;
				thisdeg.y = thisdeg.y/denominator;
			}
			DerivatesNURBS.add(k, new Point2D.Double(thisdeg.x,thisdeg.y));
		}
		return DerivatesNURBS.elementAt(derivate);
	}
	private Vector<Point3d> DerivateValues(int derivate, double u)
	{
		int du = Math.min(degree, derivate);
		Vector<Point3d> CK = new Vector<Point3d>();
		int span = findSpan(u);
		CK.setSize(derivate+1);
		for (int k=degree+1; k<=derivate; k++)
			CK.set(k,new Point3d(0d,0d,0d)); //All higher derivates zero
		double[][] nders = DersBasisFuns(du,u);
		for (int k=0; k<=du; k++) //compute kth value
		{
			CK.set(k, new Point3d(0d,0d,0d));
			for (int j=0; j<=degree; j++)
			{
				Point3d Addition = (Point3d) controlPointsHom.get(span-degree+j).clone();
				Addition.scale(nders[k][j]);
				CK.get(k).add(Addition);
			}
		}
		return CK;
	}
	private double[][] DersBasisFuns(int derivate, double u)
	{
		//Adapted Alg 2.3 - U=Knots, p=degree, n=derivate 
		double[][] derivates = new double[derivate+1][degree+1];
		double[][] ndu = new double[degree+1][degree+1];
		ndu[0][0]=1d;
		int i = findSpan(u);
		double[] left = new double[degree+1]; double[] right = new double[degree+1];
		for (int j=1; j<=degree; j++)
		{
			left[j] = u-Knots.get(i+1-j);
			right[j] = Knots.get(i+j)-u;
			double saved=0d;
			for (int r=0; r<j; r++)
			{
				ndu[j][r] = right[r+1]+left[j-r];
				double temp = ndu[r][j-1]/ndu[j][r];
				ndu[r][j] = saved+right[r+1]*temp;
				saved = left[j-r]*temp;
			}
			ndu[j][j] = saved;
		}
		for (int j=0; j<=degree; j++) //Load Basis Funs
			derivates[0][j] = ndu[j][degree];
		//Compute Derivates
		for (int r=0; r<=degree; r++)
		{
			int s1=0, s2=1;
			double[][] a = new double[2][derivate+1];
			a[0][0] = 1d;
			for (int k=1; k<=derivate; k++)
			{
				double d=0d; int rk = r-k, degk = degree-k;
				if (r>=k)
				{
					a[s2][0] = a[s1][0]/ndu[degk+1][rk];
					d = a[s2][0]*ndu[rk][degk];
				}
				int j1,j2;
				if (rk>=-1)
					j1=1;
				else
					j1=-rk;
				if (r-1<=degk)
					j2=k-1;
				else
					j2=degree-r;
				for (int j=j1; j<=j2; j++)
				{
					a[s2][j] = (a[s1][j]+a[s1][j-1])/ndu[degk+1][rk+j];
					d += a[s2][j]*ndu[rk+j][degk];	
				}
				if (r<= degk)
				{
					a[s2][k] = -a[s1][k-1]/ndu[degk+1][r];
					d += a[s2][k]*ndu[r][degk];
				}
				derivates[k][r] = d;
				int j=s1; s1=s2; s2=j; //Switch rows
			}
		}
		//Multiply throguh to correct factors to eq 2.9
		int r=degree;
		for (int k=1; k<=derivate; k++)
		{
			for (int j=0; j<=degree; j++)
				derivates[k][j] *=r;
			r *= (degree-k);
		}
		return derivates;
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
	 * @param u Point u \in [0,1], which result we want
	 * @return a 3d-Value of the Point in the Curve.
	 */
	private Point3d deBoer3D(double u)
	{
		int i = findSpan(u);
		Vector<Point3d> fixedj = new Vector<Point3d>();
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
				Point3d bimjm = fixedj.get(l-i+degree-1);//b_i-1^j-1
				double alpha = alpha(u,l,k);
				Point3d bijm = fixedj.get(l-i+degree);
				double x = (1-alpha)*bimjm.x + alpha*bijm.x;
				double y = (1-alpha)*bimjm.y + alpha*bijm.y;
				double z = (1-alpha)*bimjm.z + alpha*bijm.z;
				fixedj.set(l-i+degree,new Point3d(x,y,z));
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
	public Vector<Double> BasisBSpline(double u)
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
	public double BasisR(double u, int number)
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
	 * Compares this Curve to another (minDist does not matter)
	 * if alle values of t,b,w are equal it returns true, else false
	 * 
	 * TODO: Check whether there's a way to check for equality if these values are different ?
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
	
	public void refineMiddleKnots()
	{
		if (isEmpty())
			return;
		Vector<Double> newknots = new Vector<Double>();
		Iterator<Double> knotIter = Knots.iterator();
		Double lastvalue = Knots.firstElement();
		while (knotIter.hasNext())
		{
			Double d = knotIter.next();
			if (lastvalue.doubleValue()!=d.doubleValue()) //We have a real distance between last and this value
			{
				newknots.addElement(lastvalue.doubleValue() + (d.doubleValue()-lastvalue.doubleValue())/2.0d);				
			}
			
			lastvalue = d;
		}
		RefineKnots(newknots);
	}
	/**
	 * Refine the Curve to add some new knots contained in X from wich each is between t[0] and t[m]
	 * @param X
	 */
	public void RefineKnots(Vector<Double> X)
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
		Vector<Point3d> newPw;
		newPw = new Vector<Point3d>();
		newPw.setSize(controlPointsHom.size()+X.size());
		Vector<Double> newt = new Vector<Double>();
		newt.setSize(Knots.size()+X.size());
		for (int j=0; j<=a-degree; j++)//Copy the first not changed values of the CPs
			newPw.set(j, controlPointsHom.get(j));
		for (int j=b-1; j<=maxCPIndex; j++)//Copy the last not changed values of the CPs
			newPw.set(j+X.size(), controlPointsHom.get(j));
		for (int j=0; j<=a; j++)//Copy the first not changed values of t
			newt.set(j, Knots.get(j));
		for (int j=b+degree; j<=maxKnotIndex; j++)//Copy the last not changed values of t
			newt.set(j+X.size(), Knots.get(j));
		
		int i=b+degree-1; //Last Value that's new in t
		int k=b+degree+X.size()-1; //Last Value that's new in Pw
		for (int j=X.size()-1; j>=0; j--) //Insert new knots backwards beginning at X.lastElement
		{ 
			while (X.get(j) <= Knots.get(i) && i > a) //These Values are not affected by Insertion of actual Not, copy them
			{
				newPw.set(k-degree-1, (Point3d) controlPointsHom.get(i-degree-1).clone());
				newt.set(k, Knots.get(i));
				k--;i--;
			}
			newPw.set(k-degree-1, (Point3d) newPw.get(k-degree).clone());
			for (int l=1; l<=degree; l++)
			{
				int actualindex = k-degree+l;
				double alpha = newt.get(k+l)-X.get(j);
				if (Math.abs(alpha) == 0.0d)
					newPw.set(actualindex-1, (Point3d) newPw.get(actualindex).clone());
				else
				{
					alpha = alpha/(newt.get(k+l)-Knots.get(i-degree+l));
					Point3d p1 = (Point3d) newPw.get(actualindex-1).clone();
					p1.scale(alpha);
					Point3d p2 = (Point3d) newPw.get(actualindex).clone();
					p2.scale(1.0d - alpha);
					p1.add(p2);
					newPw.set(actualindex-1,p1);
				}
			} //All Points recomputed for this insertion
			newt.set(k, X.get(j));
			k--;
		}
		//Recompute Points & weights
		controlPoints = new Vector<Point2D>(); 
		cpWeight = new Vector<Double>();
		Iterator<Point3d> Pwi = newPw.iterator();
		while (Pwi.hasNext())
		{
			Point3d p1 = Pwi.next();
			if (p1.z==0)
				controlPoints.add(new Point2D.Double(p1.x,p1.y));
			else
				controlPoints.add(new Point2D.Double(p1.x/p1.z, p1.y/p1.z));
			cpWeight.add(p1.z);
		}
		Knots = newt;
		maxCPIndex = controlPoints.size()-1;
		maxKnotIndex = Knots.size()-1;
		degree = Knots.size()-controlPoints.size()-1;
		InitHomogeneous();
	}
	
	public void removeKnot(double knotval)
	{
		
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
		Point2D src = NURBSCurveAt(position);
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
		if (Pindex==0)
			controlPoints.set(controlPoints.size()-1, (Point2D) Pnew.clone());
		else if (Pindex==(controlPoints.size()-1))
			controlPoints.set(0, (Point2D) Pnew.clone());
		InitHomogeneous();
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
}
