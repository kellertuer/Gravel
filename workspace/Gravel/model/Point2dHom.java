package model;

/**
 * A 3 element that represents a 2D Point in homogeneous coordinates
 * 
 *  x X of Point2D
 *  y Y of Point2D
 *  w weight or homogeneous coordinate for the Point
 *  
 *  all values are in double floating point precision
 *
 */
public class Point2dHom
{
	public double x;
	public double y;
	public double w;
    /**
     * Constructs and initializes a Point2dHom Point from x,y and w
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the homogeneous coordinate
     */
    public Point2dHom(double px, double py, double pw)
    {
    	x=px;
    	y=py;
    	w=pw;
    }

    /**
     * Constructs and initializes a Point3d from the specified Point3d.
     * @param p1 the Point3d containing the initialization x y z data
     */
    public Point2dHom(Point2dHom p)
    {
    	x = p.x;
    	y = p.y;
    	w = p.w;
    }


  /**
   * Returns the square of the distance between this point and point p.
   * @param p1 the other point 
   * @return the square of the distance
   */
  public final double distanceSquared(Point2dHom p)
    {
      double dx, dy, dw;
      dx = this.x-p.x;
      dy = this.y-p.y;
      dw = this.w-p.w;
      return (dx*dx+dy*dy+dw*dw);
    }

  /**
   * Sets the value of this tuple to the specified xyz coordinates.
   * @param x the x coordinate
   * @param y the y coordinate
   * @param w the w coordinate
   */
  public void set(double x, double y, double w)
  {
	this.x = x;
	this.y = y;
	this.w = w;
  }
  /**
   * Scale this vector by given factor s
   * @param s
   */
  public final void scale(double s)
  {
      this.x *= s;
      this.y *= s;
      this.w *= s;
  }
  
  /**  
   * Sets the value of the point to the sum of itself and p.
   * @param t1 the other tuple
   */  
  public final void add(Point2dHom p)
  { 
      this.x += p.x;
      this.y += p.y;
      this.w += p.w;
  }
  
  public Point2dHom clone()
  {
	  return new Point2dHom(x,y,w);
  }
  
}
