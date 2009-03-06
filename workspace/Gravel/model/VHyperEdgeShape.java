package model;

import java.awt.Point;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.Vector;

import javax.vecmath.Point3d;

/**
 * This Class represents the Shape of an individual VHyperEdge
 * Its shape is based on NURBS and a minimal distance it should have from each node
 * This Minimum Distance is used to evaluate the validity of the Shape
 * @see VHyperEdge
 * 
 * Other Methods are
 * - Min and Max of the Bounding Box of the ControlPolygon
 * - Addition (Refinement of Knots)
 *
 * - TODO Constructor with Interpolation-Points
 * - TODO Get&Set single CPs (when they're moved)
 * - TODO (needed?) get&set knots &weights
 * - TODO In/Decrease Degree of the polynonials 
 * 
 * @author Ronny Bergmann
 *
 */
public class VHyperEdgeShape {

	private Vector<Double> Knots;
	private Vector<Double> cpWeight;
	public Vector<Point2D> controlPoints; //ControlPoints
	private Vector<Point3d> controlPointsHom; //b in homogeneous coordinates multiplied by weight

	private int minDist;
	private int maxKnotIndex, //The Knots are numbered 0,1,...,maxKnotIndex
				maxCPIndex, //The ControlPoints are numbered 0,1,...,maxCPIndex
				degree; //Order of the piecewise polynomials - depends on the maxIndices Above: degree = maxKnotIndex-maxCPindex-1

	/**
	 * Create an empty shape so nothing ever happens but its at least not null
	 */
	public VHyperEdgeShape()
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
	public VHyperEdgeShape(Vector<Double> pKnots, Vector<Point2D> CPoints, Vector<Double> weights, int dist)//, int degree)
	{
		minDist = dist;
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
		InitHomogeneous();
	}
	/**
	 * Private Constructor to (re)create with Homogeneous ControlPointVector
	 * @param knots
	 * @param pPw
	 * @param dist
	 */
	private VHyperEdgeShape(Vector<Double> knots, Vector<Point3d> pPw, int dist)
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
			cpWeight.add(i,stw);
		}
		minDist = dist;
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
	 * Get the Curve as a piecewise approximated linear Java Path
	 * @param stepsize Size in the Intervall two points on the path differ
	 * TODO: Vary that value to an maximum distance thwo points on the path should have (as Bezier-Paths do)
	 * @return
	 */
	public GeneralPath getCurve(double stepsize) //Adapt to a length on the curve?
	{
		//Intervallborders
		double first = Knots.firstElement();
		double last = Knots.lastElement();
		double actual = first;
		GeneralPath path = new GeneralPath();
		Point2D.Double f = NURBSCurveAt(first);
		path.moveTo((new Double(f.x)).floatValue(), (new Double(f.y)).floatValue());
		actual+=stepsize;
		while (actual<=last)
		{
			f = NURBSCurveAt(actual);
			path.lineTo((new Double(f.x)).floatValue(), (new Double(f.y)).floatValue());
			actual+=stepsize;
		}
	//	path.closePath();
		
		return path;
	}
	/**
	 * Find the interval u \in [t.get(j),t.get(j+1)] and return the index j
	 * 
	 * because the first and last d+1 values of t are assumed equal, the 
	 * @param u
	 * @return
	 */
	private int findSpan(double u)
	{
		if (u==Knots.lastElement())
			return Knots.indexOf(Knots.lastElement())-1; //first value of t equal to t.get(m)==t.lastElement - which is m-d		
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
	public Point2D DerivateCurveAt(int pDegree, double u)
	{
		if (pDegree==0)
			return NURBSCurveAt(u);
		Vector<Point3d> DerivatesBSpline = new Vector<Point3d>();
		DerivatesBSpline.setSize(pDegree+1);
		int actdeg = 1;
		while (actdeg<=pDegree) //Generate all Values of lower derivates at Point u in homogeneous BSpline-Points
		{
			Vector<Point3d> theirCP = CPofDerivate(actdeg);
			Vector<Double> theirt = new Vector<Double>();
			for (int i=actdeg; i<=maxKnotIndex-actdeg; i++)
				theirt.add(i-actdeg,Knots.get(i));
			VHyperEdgeShape theirCurve = new VHyperEdgeShape(theirt,theirCP,0);
			Point3d derivp= theirCurve.deBoer3D(u);
			DerivatesBSpline.set(actdeg,derivp); 
			actdeg++;
		}
		Vector<Point2D> DerivatesNURBS = new Vector<Point2D>();
		DerivatesNURBS.setSize(pDegree);
		DerivatesNURBS.set(0,NURBSCurveAt(u));
		for (int k=1; k<=pDegree; k++)
		{ //Calculate kth Derivate
			Point2D.Double thisdeg = new Point2D.Double(DerivatesBSpline.get(k).x,DerivatesBSpline.get(k).y);
			double denominator = DerivatesBSpline.get(k).z;
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
		return DerivatesNURBS.elementAt(pDegree);
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
	private Vector<Double> BasisBSpline(double u)
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
				if ((u==Knots.get(k)&&(Knots.get(k+actualDegree)==Knots.get(k)))) //0 divided by 0
					fac1=1.0;
				else
					fac1 = (u-Knots.get(k))/(Knots.get(k+actualDegree)-Knots.get(k));
				if ((u==Knots.get(k+actualDegree+1)&&(Knots.get(k+actualDegree+1)==Knots.get(k+1)))) //0 divided by 0
					fac2=1.0;
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
	 * TODO: Check whether there's a way to check for equality if these values are different
	 * @param s another Shape
	 * @return true if Shape s is equal to this else false
	 */
	public boolean CurveEquals(VHyperEdgeShape s)
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
	/**
	 * Refine the Curve to add some new knots contained in X from wich each is between t[0] and t[m]
	 * @param X
	 */
	public void RefineKnots(Vector<Double> X)
	{
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
	/**
	 * Little Helping Function: Compute Controloints of the Derivate
	 * of given degree based on the ControlPoints in this Curve here 
	 * @param pDegree th degree-th Derivate
	 * @return ControlPoints are returned
	 */
	@SuppressWarnings("unchecked")
	private Vector<Point3d> CPofDerivate(int pDegree)
	{
		Vector<Point3d> result = new Vector<Point3d>();
		if (pDegree==0)
		{
			for (int i=0; i<controlPointsHom.size(); i++)
				result.add((Point3d) controlPointsHom.get(i).clone());
			return result;
		}
		Vector<Point3d> degm1 = CPofDerivate(pDegree-1);
		//Compute from those the actually wanted ones
		for (int i=0; i<degm1.size()-1; i++) //one less
		{
			double factor = (degree-pDegree+1)/(Knots.get(i+degree+1)-Knots.get(i+pDegree));
			Point3d next = (Point3d) degm1.get(i+1).clone();
			next.sub(degm1.get(i));
			next.scale(factor);
			result.add(next);
		}
		return result;		
	}
	public Point2D ProjectionPoint(Point2D d)
	{
		return NURBSCurveAt(ProjectionPointParameter(d));
	}
	/**
	 * Projects the point d to a point, whose distance is minimal to d and on the curve
	 * 
	 * TODO: Newtn Iteraton, if this is not qcurate enough?
	 * @param d
	 * @return
	 */
	public double ProjectionPointParameter(Point2D d)
	{
		//TODO: Set the value of the intervalls of u heuristically by length of the line
		double eqdist = .0001; //Find a nice Start-value for u
		double u = Knots.firstElement(),u0 = Knots.firstElement();
		double mindist = Double.MAX_VALUE;
		while (u<=Knots.lastElement())
		{
			Point2D p = NURBSCurveAt(u);
			double thisdist = p.distance(d);
			if (thisdist<mindist)
			{
				u0=u;
				mindist=thisdist;
			}
			u+=eqdist;
		}
		return u0;
	}
	/**
	 * If the first Point lies on the Curve (given a specific variance, e.g. 2.0
	 * the point is moved to the second argument and the Curve is Computed again.
	 * @param src
	 * @param dest
	 * @return
	 */
	public boolean movePoint(Point2D src, Point2D dest)
	{
		double udash = ProjectionPointParameter(src);
		Point2D.Double ProjPoint = NURBSCurveAt(udash);
		if (src.distance(ProjPoint)>=2.0d)
			return false; //No Movement
		//Find the specific P, which has the most influence at src to move.
		double min = Double.MAX_VALUE;
		int Pindex=0;
		for (int i=0; i<=maxCPIndex; i++)
		{
			double nodei = 0.0d;
			for (int j=1; j<=degree; j++)
			{
				nodei = nodei + (double)Knots.get(i+j);
			}
			nodei = nodei/degree;
			if (Math.abs(udash-nodei)<min)
			{
				Pindex=i;
				min = Math.abs(udash-nodei);
			}
		}
		Point2D Pk = controlPoints.get(Pindex);		
		Point2D direction = new Point2D.Double(dest.getX()-src.getX(),dest.getY()-src.getY());
		double mov = BasisR(udash,Pindex);
		Point2D.Double Pnew = new Point2D.Double(
				Pk.getX() + direction.getX()/mov,
				Pk.getY() + direction.getY()/mov		
		);
		controlPoints.set(Pindex,Pnew);
		InitHomogeneous();
		return true;
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
