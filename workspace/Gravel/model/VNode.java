package model;
import java.awt.Color;
import java.awt.Point;
import java.util.Comparator;
/**
 * The VNode contains values for
 * - the node position
 * - the node size
 * - the properties of its text
 * - an indicator whether the node is selected or not
 * @author Ronny Bergmann
 */
public class VNode extends VItem {
	
		public static class NodeIndexComparator implements Comparator<VNode>
		{
			public int compare(VNode a, VNode b)
			{
				if (a.index < b.index)
					return -1;
				if (a.index == b.index)
					return 0;
				else // >
					return 1;
			}
			public boolean equals(VNode a, VNode b)
			{
				return a.index==b.index;
			}
		}
		public int index;  //kind of Nodekey
		private Point Pos; //Position in the middle
		private int size; //Radius des Knotens
		
		private int name_distance, name_rotation, name_size;
		private boolean name_visible=false;
		
		private Color colour; //Farbe des Knotens
		private int setCount; //Anzahl Mengen in denen der Knoten beteiligt ist, f�r Color 
		/**
		 * 
		 * @param i
		 * @param x
		 * @param y
		 * @param s
		 * 
		 * @deprecated please user the Constructor with name_visibility_values
		 */
		public VNode(int i,int x,int y,int s)
		{
			index = i;
			Pos = new Point(x,y);
			size = s;
			setCount = 0;
			colour = Color.black;
		}
		/**
		 * Initialize a visual Node
		 * @param i its index
		 * @param x position x
		 * @param y position y
		 * @param s size
		 * @param nd node_text distance
		 * @param nr node_text orientation in degree
		 * @param ns node_text size in px
		 * @param nv node_text visibility
		 */
		public VNode(int i, int x, int y, int s, int nd, int nr, int ns, boolean nv)
		{
			this(i,x,y,s);
			name_distance = nd;
			name_rotation = nr;
			name_size = ns;
			name_visible = nv;
		}
		/**
		 * Actual Color the actual Color of the node
		 * @return the color of the node
		 */
		public Color getColor()
		{
			return colour;
		}
		/**
		 * Add a further Color to the node - used by subsets
		 * the color is computed by the average of all color added to the node
		 * @param newc new color added to the node
		 */
		public void addColor(Color newc)
		{
			if (newc==null)
				return;
			int b=colour.getBlue()*setCount + newc.getBlue();
			int a=colour.getAlpha()*setCount + newc.getAlpha();
			int g=colour.getGreen()*setCount + newc.getGreen();
			int r=colour.getRed()*setCount + newc.getRed();
			setCount++;
			colour = new Color((r/setCount),(g/setCount),(b/setCount),(a/setCount));
		}
		/**
		 * Remove a color by compuattion if at least one color was added to the node
		 * @param newc
		 */
		public void removeColor(Color newc)
		{
			if (setCount > 1)
			{
				int b=colour.getBlue()*setCount - newc.getBlue();
				int a=colour.getAlpha()*setCount - newc.getAlpha();
				int g=colour.getGreen()*setCount - newc.getGreen();
				int r=colour.getRed()*setCount - newc.getRed();
				//Durch Rundungsfehler können dabei negative werte entstehen, diese also verhindern
				if (b<0) b=0;
				if (a<0) a=0;
				if (r<0) r=0;
				if (g<0) g=0;
				setCount--;
				colour = new Color((r/setCount),(g/setCount),(b/setCount),(a/setCount));
			}
			else
			{
				colour = Color.black;
				setCount = 0;
			}
		}
		/**
		 * The Draw Point is the top left corner of the rectangle sorrounding the circle of the nodesize
		 * @return
		 */
		public Point getdrawpoint()
		{
			return new Point(Pos.x-size/2,Pos.y-size/2);
		}
		/**
		 * Get the Position of the node
		 * @return
		 */
		public Point getPosition()
		{
			return Pos;
		}
		/**
		 * Set the position of the node to a 
		 * @param p new point p
		 */
		public void setPosition(Point p)
		{
				Pos = p;
		}
		/**
		 * Translate the node by 
		 * @param x
		 * @param y
		 */
		public void translate(int x, int y)
		{
			Pos.translate(x,y);
			//For Safety Issues
			if (Pos.x < 0)
				Pos.x=0;
			if (Pos.y < 0)
				Pos.y = 0;
		}
		/**
		 * Get the size of the node
		 * @return the size in px
		 */
		public int getSize()
		{
			return size;
		}
		/**
		 * Set the size to a new
		 * @param i size in px
		 */
		public void setSize(int i)
		{
			size = i;
		}
		public String toString()
		{
			return "(#"+index+") - Pos:"+Pos;
		}
		/**
		 * Get the Distance from the node position to the middle of the text that should be displayd
		 * @return a distance in px
		 */
		public int getNameDistance() {
			return name_distance;
		}
		/**
		 * Set the Distance from the node position to the middle of the text that should be displayd to
		 * @return a new distance in px
		 */
		public void setNameDistance(int name_distance) {
			this.name_distance = name_distance;
		}
		/**
		 * Get the Oriantation of the Node name 
		 * @return a value in Degree where 0° is at the top and it is clockwise counted
		 */
		public int getNameRotation() {
			return name_rotation;
		}
		/**
		 * Set the Oriantation of the Node name to a 
		 * @param name_rotation new value in Degree where 0° is at the top and it is clockwise counted
		 */
		public void setNameRotation(int name_rotation) {
			this.name_rotation = name_rotation;
		}
		/**
		 * get the name size 
		 * @return as a value in px
		 */
		public int getNameSize() {
			return name_size;
		}
		/**
		 * Set the Size of the node name to
		 * @param name_size a new size in px
		 */
		public void setNameSize(int name_size) {
			this.name_size = name_size;
		}
		/**
		 * Is the node name visible ?
		 * @return true if the node name is visible else false
		 */
		public boolean isNameVisible() {
			return name_visible;
		}
		/**
		 * Set the visibility of the nodename to
		 * @param name_visible
		 */
		public void setNameVisible(boolean name_visible) {
			this.name_visible = name_visible;
		}
}