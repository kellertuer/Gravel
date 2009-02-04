package model;

import io.GeneralPreferences;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Observable;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import model.VEdge;
import model.VNode;
/**
 * VGraph encapsulates an MGraph and keeps visual information about every node, edge and subset in the MGraph
 * each manipulation on the VGraph is also given to the MGraph
 * The MGraph may the extracted and beeing used to generate another VGraph to the same MGraph
 *
 * The VGraph is observable, so that every Observer may update itself, if something changes here
 *
 * the update may give Information about the updated parts by the GraphMessage
 *
 * @author Ronny Bergmann 
 */
public class VGraph extends Observable {

	private TreeSet<VNode> vNodes;
	private TreeSet<VEdge> vEdges;
	private HashSet<VSubSet> vSubSets;
	private Lock EdgeLock, NodeLock;
	private MGraph mG;
	/**
	 * Constructor
	 * 
	 * @param d indicates whether the graph is directed or not
	 * @param l indicates whether the graph might have loops or not
	 * @param m indicates whether the graph might have multiple edges between two nodes or not
	 */	
	public VGraph(boolean d, boolean l, boolean m)
	{
		vNodes = new TreeSet<VNode>(new VNode.NodeIndexComparator());
		vEdges = new TreeSet<VEdge>(new VEdge.EdgeIndexComparator());
		vSubSets = new HashSet<VSubSet>();
		EdgeLock = new ReentrantLock();
		NodeLock = new ReentrantLock();
		mG = new MGraph(d,l,m);
	}
	//
	//
	// Allgemeine Methoden
	//
	//
	/**
	 * deselect all Nodes and Edges
	 */
	public void deselect() {
		Iterator<VNode> n = vNodes.iterator();
		while (n.hasNext()) {
			n.next().deselect();
		}
		Iterator<VEdge> n2 = vEdges.iterator();
		while (n2.hasNext()) {
			n2.next().deselect();
		}
		setChanged();
		notifyObservers(new GraphMessage(GraphMessage.SELECTION,GraphMessage.UPDATE));
	}
	/**
	 * deletes all selected Nodes and Edges. That means, that also all incident Edges of selected Nodes are deleted
	 *
	 */
	public void removeSelection()
	{
		Iterator<VNode> n = vNodes.iterator();
		HashSet<VNode> selected = new HashSet<VNode>();
		while (n.hasNext()) {
			VNode node = n.next();
			if ((node.getSelectedStatus()&VItem.SELECTED)==VItem.SELECTED)
				selected.add(node);
		}
		n = selected.iterator();
		while (n.hasNext())
		{
			removeNode_(n.next().index);
		}
		Iterator<VEdge> n2 = vEdges.iterator();
		HashSet<VEdge> selected2 = new HashSet<VEdge>();
		while (n2.hasNext()) {
			VEdge edge = n2.next();
			if ((edge.getSelectedStatus() & VItem.SELECTED) == VItem.SELECTED)
				selected2.add(edge);
		}
		n2 = selected2.iterator();
		while (n2.hasNext())
		{
			removeEdge_(n2.next().index);
		}
		setChanged();
		notifyObservers(
			new GraphMessage(GraphMessage.SELECTION|GraphMessage.NODE|GraphMessage.EDGE, //Changed
							GraphMessage.REMOVAL, //Status 
							GraphMessage.NODE|GraphMessage.EDGE|GraphMessage.SELECTION) //Affected		
		);		
	}
	/**
	 * A Graph is directed or not. If directed, all Edges are directed. If not, all Edges are undirected.
	 * <br><br>
	 * TODO: Is it perhaps better to put this variable back to the edges and do a check here...
	 * <br>
	 * @return true, if the Graph is directed, else false
	 */
	public boolean isDirected()
	{
		return mG.isDirected();
	}
	/**
	 *  Modify the Graph to the given directed or undirected Value.
	 *  <br>If modifying to undirected, some edges may be deleted (if and only if two edges from a to b and b to a exist)
	 *  <br><br>
	 * @param d
	 */
	public BitSet setDirected(boolean d)
	{
		BitSet removed = new BitSet();
		if (d==mG.isDirected())
			return removed; //nicht geändert
		//Auf gerihctet umstellen ist kein Problem. 
		if (!d) //also falls auf ungerichtet umgestellt wird
		{
			if (!mG.isMultipleAllowed()) //Ist auch nur ein Problem, wenn keine Mehrfachkanten erlaubt sind
			{
				NodeLock.lock(); //Knoten finden
				try
				{
					Iterator<VNode> n = vNodes.iterator();				//Knotenpaare
					while (n.hasNext())
					{
						VNode t = n.next();
						Iterator<VNode> n2 = vNodes.iterator();			//jeweils
						while (n2.hasNext())
						{
							VNode t2 = n2.next();
							if (t.index < t2.index)
							{
								Vector<Integer> ttot2 = getEdgeIndices(t.index,t2.index);
								Vector<Integer> t2tot = getEdgeIndices(t2.index,t.index);
								//In the nonmultiple case each Vector has exactely one or no edge in it
								if ((!ttot2.isEmpty())&&(!t2tot.isEmpty()))
								{
									int e1 = ttot2.firstElement();
									int e2 = t2tot.firstElement();
									mG.setEdgeValue(e2, getEdgeProperties(e2).get(MGraph.EDGEVALUE)+getEdgeProperties(e1).get(MGraph.EDGEVALUE));
									removeEdge(e1);
									removed.set(e1);
								}
							} //End no duplicate
						}
					}
				}	
				finally {NodeLock.unlock();}
				if (mG.setDirected(d).cardinality() > 0)
				{
					System.err.println("DEBUG ; Beim gerichtet Setzen läuft was falsch");
				} 				
			} //end of if !allowedmultiple
			else //multiple allowed - the other way around
			{
				if (mG.setDirected(d).cardinality() > 0)
				{
					System.err.println("DEBUG ; Beim gerichtet Setzen läuft was falsch");
				} 				
				EdgeLock.lock(); //find similar Edges
				try
				{		
					HashSet<VEdge> toDelete = new HashSet<VEdge>(); // zu entfernende Kanten
					Iterator<VEdge> e = vEdges.iterator();				
					while (e.hasNext())
					{
						VEdge t = e.next();
						int ts = mG.getEdgeProperties(t.index).get(MGraph.EDGESTARTINDEX);
						int te = mG.getEdgeProperties(t.index).get(MGraph.EDGEENDINDEX);
						Vector<Integer> indices = mG.getEdgeIndices(ts,te);
						Iterator<Integer> iiter = indices.iterator();
						while (iiter.hasNext())
						{
							VEdge act = getEdge(iiter.next());
							if ((mG.getEdgeProperties(act.index).get(MGraph.EDGESTARTINDEX)==te)&&(!mG.isDirected())&&(act.getType()==VEdge.ORTHOGONAL)&&(t.getType()==VEdge.ORTHOGONAL)) 
							//ungerichtet, beide orthogonal und entgegengesetz gespeichert
							{
								if ((((VOrthogonalEdge)act).getVerticalFirst()!=((VOrthogonalEdge)t).getVerticalFirst())&&(!removed.get(act.index)))
								{
									System.err.println("removing Edge #"+t.index+" because ORTH and it is similar to #"+act.index);
									toDelete.add(t);
									removed.set(t.index);
								}
							}
							else if ((t.PathEquals(act)&&(!removed.get(act.index)))&&(t.index!=act.index)) //same path
							{
								System.err.println("removing Edge #"+t.index+" because it is similar to #"+act.index);
								toDelete.add(t);
								removed.set(t.index);
							}
						} //end inner while
					} //end outer while
					Iterator<VEdge> e3 = toDelete.iterator();
					while (e3.hasNext())
						removeEdge_(e3.next().index);
				} finally{EdgeLock.unlock();}
			} //end of deleting similar edges in multiple directed graphs
		}//end if !d
		else //undirected
			mG.setDirected(d); //change
		//im MGraph auch noch
		setChanged();
		notifyObservers(
				new GraphMessage(GraphMessage.EDGE|GraphMessage.DIRECTION, //Type
								GraphMessage.UPDATE) //Status 
			);
		return removed;
	}
	/**
	 * Translates the Graph by the given Offset in x and y direction
	 * <br><br>
	 * @param x Translation on the X-axis
	 * @param y Translation on the Y-axis
	 */
	public void translate(int x, int y)
	{
		Iterator<VNode> iter1 = vNodes.iterator();
		while (iter1.hasNext())
		{
			iter1.next().translate(x, y);
		}
		Iterator<VEdge> iter2 = vEdges.iterator();
		while(iter2.hasNext())
		{
			iter2.next().translate(x,y);
		}
		setChanged();
		notifyObservers(
				new GraphMessage(GraphMessage.NODE|GraphMessage.EDGE, //Type
								GraphMessage.TRANSLATION, //Status 
								GraphMessage.NODE|GraphMessage.EDGE|GraphMessage.SELECTION|GraphMessage.SUBSET) //Affected		
			);
	}
	/**
	 * Get the Math-Graph underneath this VGraph
	 * <br><br>
	 * @return a referenceto the MGraph
	 */
	public MGraph getMathGraph() {
		return mG;
	}
	/**
	 * replace the actual VGraph with another one.
	 * <br>Use this Method to replace this VGraph with a new loaded one or a new visualised one.
	 * <br>
	 * @param anotherone the New VGraph
	 */
	public void replace(VGraph anotherone)
	{
		vNodes=anotherone.vNodes;
		vEdges=anotherone.vEdges;
		vSubSets = anotherone.vSubSets;
		mG = anotherone.mG;
		mG.pushNotify(
						new GraphMessage(GraphMessage.ALL_ELEMENTS, //Type
										GraphMessage.UPDATE, //Status 
										GraphMessage.ALL_ELEMENTS) //Affected		
		);
		setChanged();
		notifyObservers(
				new GraphMessage(GraphMessage.ALL_ELEMENTS, //Type
						GraphMessage.UPDATE, //Status 
						GraphMessage.ALL_ELEMENTS) //Affected		
		);
	}
	/**
	 * Clone the VGraph and
	 * @return the copy
	 */
	public VGraph clone()
	{
		VGraph clone = new VGraph(mG.isDirected(),mG.isLoopAllowed(), mG.isMultipleAllowed());
		//Untergraphen
		Iterator<VSubSet> n1 = vSubSets.iterator();
		while (n1.hasNext())
		{
			VSubSet actualSet = n1.next();
			clone.addSubSet(actualSet.getIndex(),mG.getSubSetName(actualSet.index),actualSet.getColor()); //Jedes Set kopieren
		}
		//Knoten
		Iterator<VNode> n2 = vNodes.iterator();
		while (n2.hasNext())
		{
			VNode nodeclone = n2.next().clone();
			clone.addNode(nodeclone, mG.getNodeName(nodeclone.index));
			//In alle Sets einfuegen
			n1 = vSubSets.iterator();
			while (n1.hasNext())
			{
				VSubSet actualSet = n1.next();
				if (this.SubSetcontainsNode(nodeclone.index,actualSet.index))
					clone.addNodetoSubSet(nodeclone.index,actualSet.index); //In jedes Set setzen wo er war
			}
		}
		//Analog Kanten
		Iterator<VEdge> n3 = vEdges.iterator();
		while (n3.hasNext())
		{
			VEdge cloneEdge = n3.next().clone();
			Vector<Integer> values = mG.getEdgeProperties(cloneEdge.index);
			clone.addEdge(cloneEdge,values.elementAt(MGraph.EDGESTARTINDEX),values.elementAt(MGraph.EDGEENDINDEX),values.elementAt(MGraph.EDGEVALUE));
			//Name klonen
			clone.setEdgeName(cloneEdge.index, this.getEdgeName(cloneEdge.index));
			//In alle Sets einfuegen
			n1 = vSubSets.iterator();
			while (n1.hasNext())
			{
				VSubSet actualSet = n1.next();
				if (this.SubSetcontainsEdge(cloneEdge.index,actualSet.index))
					clone.addEdgetoSubSet(cloneEdge.index,actualSet.getIndex()); //Jedes Set kopieren
			}
		}
		//und zurückgeben
		return clone;
	}
	/**
	 * returns the maximum point that is used by the VGraph.
	 * <br>On nodes the size of the node is included
	 * <br>On Edges the control point is included 
	 * 
	 * @return Maximum as a point
	 */
	public Point getMaxPoint(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g;
		Point maximum = new Point(0,0);
		Iterator<VNode> iter1 = vNodes.iterator();
		while (iter1.hasNext())
		{
			VNode actual = iter1.next();
			Point p = (Point) actual.getPosition().clone();
			p.translate(Math.round(actual.getSize()/2), Math.round(actual.getSize()/2));
			p.x++; p.y++;
			if (p.x > maximum.x)
				maximum.x = p.x;
			if (p.y > maximum.y)
				maximum.y = p.y;
//			Node Text - only if Graphics are not null, they are null if there is no visible picture. 
			//And if node name is visible
			if ((g2!=null)&&(actual.isNameVisible()))
			{
				Font f = new Font("Arial",Font.PLAIN, Math.round(actual.getNameSize()));
				//mittelpunkt des Textes
				int x = actual.getPosition().x + Math.round((float)actual.getNameDistance()*(float)Math.cos(Math.toRadians((double)actual.getNameRotation())));
				int y = actual.getPosition().y - Math.round((float)actual.getNameDistance()*(float)Math.sin(Math.toRadians((double)actual.getNameRotation())));
			
				FontMetrics metrics = g2.getFontMetrics(f);
				int hgt = metrics.getAscent()-metrics.getLeading()+metrics.getDescent();
				int adv = metrics.stringWidth(getNodeName(actual.index));
				x += new Double(Math.floor((double)adv/2.0d)).intValue(); y += new Double(Math.floor((double)hgt/2.0d)).intValue(); //Bottom Right Corner
				if (x > maximum.x)
					maximum.x = x;
				if (y > maximum.y)
					maximum.y = y;
			}
		}
		Iterator<VEdge> iter2 = vEdges.iterator();
		while(iter2.hasNext())
		{
			Point edgemax = iter2.next().getMax();
			if (edgemax.x > maximum.x)
				maximum.x = edgemax.x;
			if (edgemax.y > maximum.y)
				maximum.y = edgemax.y;
		}
		return maximum;
	}
	/**
	 * returns the minimum point that is used by the VGraph.
	 * <br>On nodes the size of the node and the size of the text is included
	 * <br>On Edges the control point is included
	 * <br>The Graphics are needed to compute the fontsize
	 * <br>Zoom is not encalculated 
	 * <br>
	 * @param the Graphic in which the Graph lies. 
	 * @return Point MinPoint
	 */
	public Point getMinPoint(Graphics g)
	{
		Graphics2D g2 = (Graphics2D)g; 
		Point minimum = new Point(Integer.MAX_VALUE,Integer.MAX_VALUE);
		Iterator<VNode> iter1 = vNodes.iterator();		
		while (iter1.hasNext())
		{
			VNode actual = iter1.next();
			Point p = (Point) actual.getPosition().clone();
			p.translate(-Math.round(actual.getSize()/2), -Math.round(actual.getSize()/2));
			if (p.x>0) p.x--; 
			if (p.y>0) p.y--; 
			if (p.x < minimum.x)
				minimum.x = p.x;
			if (p.y < minimum.y)
				minimum.y = p.y;
			//Node Text - only if Graphics are not null, they are null if there is no visible picture. 
			if ((g2!=null)&&(actual.isNameVisible()))
			{
				Font f = new Font("Arial",Font.PLAIN, Math.round(actual.getNameSize()));
				//mittelpunkt des Textes
				int x = actual.getPosition().x + Math.round((float)actual.getNameDistance()*(float)Math.cos(Math.toRadians((double)actual.getNameRotation())));
				int y = actual.getPosition().y - Math.round((float)actual.getNameDistance()*(float)Math.sin(Math.toRadians((double)actual.getNameRotation())));
			
				FontMetrics metrics = g2.getFontMetrics(f);
				int hgt = metrics.getAscent()-metrics.getLeading()+metrics.getDescent();
				int adv = metrics.stringWidth(getNodeName(actual.index));
				x -= new Double(Math.floor((double)adv/2.0d)).intValue(); y -= new Double(Math.floor((double)hgt/2.0d)).intValue(); //Top Left Corner
				if (x < minimum.x)
					minimum.x = x;
				if (y < minimum.y)
					minimum.y = y;
			}
		}
		Iterator<VEdge> iter2 = vEdges.iterator();
		while(iter2.hasNext())
		{
			Point edgemax = iter2.next().getMin();
			if (edgemax.x < minimum.x)
				minimum.x = edgemax.x;
			if (edgemax.y < minimum.y)
				minimum.y = edgemax.y;
		}
		return minimum;
	}
	/**
	 * Sets the Indicator for Loops in the graph to the parameter value
	 *
	 * @param b new Acceptance of Loops
	 */
	public BitSet setLoopsAllowed(boolean b)
	{
		BitSet removed = new BitSet();
		if ((mG.isLoopAllowed())&&(!b)) //disbabling
		{
			EdgeLock.lock();
			try
			{
				HashSet<VEdge> deledges = new HashSet<VEdge>();
				Iterator<VEdge> n2 = vEdges.iterator();
				while (n2.hasNext())
				{
					VEdge e = n2.next();
					if (mG.getEdgeProperties(e.index).get(MGraph.EDGESTARTINDEX)==mG.getEdgeProperties(e.index).get(MGraph.EDGEENDINDEX))
					{
						removed.set(e.index);
						deledges.add(e);
					}
					else
						removed.clear(e.index);
				}
				Iterator<VEdge> n3 = deledges.iterator();
				while (n3.hasNext()) // Diese loeschen
				{
					removeEdge(n3.next().index);
				}
			} finally {EdgeLock.unlock();}
		}	
		if (b!=mG.isLoopAllowed())
		{
			if (mG.setLoopsAllowed(b).cardinality() > 0)
			{
				System.err.println("DEBUG : Beim Umwandeln ds Graphen in schleifenlos stimmt was nicht, ");
			}
			setChanged();
			//Loops done, update Edges
			notifyObservers(new GraphMessage(GraphMessage.LOOPS,GraphMessage.UPDATE,GraphMessage.EDGE));	
		}
		return removed;
	}
	/**
	 * Return the Indicator whether Loops are allowed or not
	 * 
	 * @return true, if loops are allowed, else false
	 */
	public boolean isLoopAllowed()
	{
		return mG.isLoopAllowed();
	}
	/**
	 * Set the possibility of multiple edges to the new value
	 * If multiple edges are disabled, the multiple edges are removed and the edge values between two nodes are added
	 * @param a
	 */
	public BitSet setMultipleAllowed(boolean b)
	{
		BitSet removed = new BitSet();
		if ((mG.isMultipleAllowed())&&(!b)) //Changed from allowed to not allowed, so remove all multiple
		{	
			NodeLock.lock(); //Knoten finden
			try
			{
				Iterator<VNode> n = vNodes.iterator();				
				while (n.hasNext())
				{
					VNode t = n.next();
					Iterator<VNode> n2 = vNodes.iterator();
					while (n2.hasNext())
					{
						VNode t2 = n2.next();
						//if the graph is directed
						if (((!mG.isDirected())&&(t2.index<=t.index))||(mG.isDirected())) //in the nondirected case only half the cases
						{
							if (mG.existsEdge(t.index,t2.index)>1) //we have to delete
							{
								Vector<Integer> multipleedges = getEdgeIndices(t.index,t2.index);
								int value = getEdgeProperties(multipleedges.firstElement()).get(MGraph.EDGEVALUE);
								//Add up the values and remove the edges from the second to the last
								Iterator<Integer> iter = multipleedges.iterator();
								iter.next();
								while(iter.hasNext())
								{
									int nextindex = iter.next();
									value += getEdgeProperties(nextindex).get(MGraph.EDGEVALUE);
									removeEdge(nextindex);
									removed.set(nextindex);
								}
								mG.setEdgeValue(multipleedges.firstElement(),value);
							}
						}					
					}
				}
			}
			finally {NodeLock.unlock();}
		}
		if (b!=mG.isMultipleAllowed())
		{
			if (mG.setMultipleAllowed(b).cardinality() > 0)
			{
				System.err.println("DEBUG : AllowMultiple set to false ERROR on that");
			}
			setChanged();
			//Allowance Updated, affected the edges
			notifyObservers(new GraphMessage(GraphMessage.MULTIPLE,GraphMessage.UPDATE,GraphMessage.EDGE));	
		}
		return removed;

	}
	/**
	 * Returns the Value, whether multiple Edges are allowed or not (inherited from inner Class MGraph)
	 * @return true if Multiple Edges between nodes are allowed, else false
	 */
	public boolean isMultipleAllowed()
	{
		return mG.isMultipleAllowed();
	}
	/**
	 * informs all subscribers about a change. This Method is used to push a notify from outside
	 * mit dem Oject o als Parameter
	 */
	public void pushNotify(Object o) {
		setChanged();
		if (o == null)
			notifyObservers();
		else
			notifyObservers(o);
	}	
	//
	//
	// Knotenmethoden
	//
	//
	/**
	 * Adds a new Node to the VGraph (and the MGraph underneath)
	 * 
	 * @param node
	 *            the new VNode
	 * @param name
	 *            name of the VNode that is stored in the MGraph
	 */
	public void addNode(VNode node, String name) {
		if (getNode(node.index) == null) {
			vNodes.add(node);
			mG.addNode(node.index, name);
			setChanged();
			//Graph changed with an add, only nodes affected
			notifyObservers(new GraphMessage(GraphMessage.NODE,node.index,GraphMessage.ADDITION,GraphMessage.NODE));	
		}
	}
	/**
	 * Get the Node with a given index
	 * @param i the index of the Node
	 * @return if a node with the given index, if it doesn't exist, it returns null
	 */
	public VNode getNode(int i) {
		Iterator<VNode> n = vNodes.iterator();
		while (n.hasNext()) {
			VNode temp = n.next();
			if (temp.index == i) {
				return temp;
			}
		}
		return null;
	}
	/**
	 * returns the name of a node with the index, if this node exists
	 * @param i index of the node 
	 * @return null, if no node with index i exists, else the name
	 * @see MGraph.getNodeName()
	 */
	public String getNodeName(int i) {
		return mG.getNodeName(i);
	}
	/**
	 * sets the the name of a node with the index, if this node exists, else it does nothing
	 * @param i
	 * @param newname
	 * @see MGraph.setNodeName()
	 */
	public void setNodeName(int i, String newname) {
		mG.setNodeName(i, newname);
		setChanged();
		notifyObservers(new GraphMessage(GraphMessage.NODE,i,GraphMessage.UPDATE,GraphMessage.NODE));	
	}
	/**
	 * removes a node and all incident edges
	 * if no node with the given index exists, nothing happens
	 * 
	 * @param i index of the node
	 * @see MGraph.removeNode(int i)
	 */
	public void removeNode(int i)
	{
		removeNode_(i);
		setChanged();
		notifyObservers(new GraphMessage(GraphMessage.NODE,i,GraphMessage.REMOVAL,GraphMessage.ALL_ELEMENTS));	
	}
	/**
	 * removes a Node from the Graph without notifying the Observers
	 * ATTENTION : Internal Use only, if you Use this method make sure you notify the Observers yourself!
	 * @param i Index of the Node to be removed
	 */	
	private void removeNode_(int i) {
		if (getNode(i)==null)
		{
			System.err.println("not removing node "+i+" - not in Graph");
			return;
		}
		
		Iterator<VSubSet> s = vSubSets.iterator();
		while (s.hasNext()) {
			removeNodefromSubSet_(i, s.next().getIndex());
		}
		BitSet Edges = mG.removeNode(i); // auf mG entfernen und adjazente
											// Kanten merken
		HashSet<VEdge> toDelete = new HashSet<VEdge>(); // zu entfernende Kanten
		Iterator<VEdge> n = vEdges.iterator();
		while (n.hasNext()) {
			VEdge aktuelle = n.next(); // Zusammensammeln der zu loeschenden
										// Kanten
			if (Edges.get(aktuelle.index)) // Aktuelle muss entfernt werden
			{
				toDelete.add(aktuelle);
			}
		}
		Iterator<VEdge> iter2 = toDelete.iterator();
		while (iter2.hasNext()) {
			removeEdge_(iter2.next().index); // loeschen
		}
		vNodes.remove(getNode(i));
	}
	/**
	 *  get an unused node index
	 *  
	 * @return the smallest node index beyond all existent nodes
	 */
	public int getNextNodeIndex() 
	{
		return mG.getNextNodeIndex();
	}
	/**
	 * get the node in Range of a given point.
	 * a node is in Range, if the distance from the node-position to the point p is smaller than the nodesize
	 * 
	 * @param p a point
	 * @return the first node in range, if there is one, else null
	 */
	public VNode getNodeinRange(Point p) {
		Iterator<VNode> n = vNodes.iterator();
		while (n.hasNext()) {
			VNode temp = n.next();
			if (temp.getPosition().distance(p) <= temp.getSize() / 2) {
				return temp;
			}
		}
		return null; // keinen gefunden
	}
	/**
	 * Check, whether at least one node is selected
	 * @return true, if there is at least one selected node, else false
	 */
	public boolean selectedNodeExists() {
		Iterator<VNode> n = vNodes.iterator();
		while (n.hasNext()) {
			if ((n.next().getSelectedStatus() & VItem.SELECTED) == VItem.SELECTED)
				return true;
		}
		return false;
	}
	/**
	 * update the node with an old given index to a new VNode and a new index
	 * all incident edges are updated also
	 * <br>
	 * @param oldindex
	 * @param newname
	 * @param v
	 */
	public void updateNode(int oldindex, String newname, VNode v) {
		if ((getNode(v.index) != null) && (oldindex != v.index)) // falls der neue Index schon vergeben ist
			return;
		if (oldindex!=v.index)
		{ // Zunächst von allen SubSets aktualisieren (remove old set new)
			Iterator<VSubSet> s = vSubSets.iterator();
			while (s.hasNext())
				removeNodefromSubSet_(oldindex, s.next().getIndex());
			mG.updateNodeIndex(oldindex, v.index);
			// und danach zu den Gruppen neu hinzufuegen
			s = vSubSets.iterator();
			while (s.hasNext())
				addNodetoSubSet(v.index, s.next().getIndex());
		}
		// Knotenname setzen
		mG.setNodeName(v.index, newname);
		// alle anderen Informationen so setzen
		VNode temp = getNode(oldindex);
		temp.setSize(v.getSize());
		temp.setPosition(v.getPosition());
		temp.setNameDistance(v.getNameDistance());
		temp.setNameRotation(v.getNameRotation());
		temp.setNameSize(v.getNameSize());
		temp.setNameVisible(v.isNameVisible());
		temp.index = v.index;
		setChanged();
		notifyObservers(new GraphMessage(GraphMessage.NODE,v.index,GraphMessage.UPDATE,GraphMessage.NODE));	
	}
	/**
	 * get a list of the node names in a vector, where each node name is stored at it's index
	 * every other component of the vector is null
	 * 
	 * @return a Vector of all node names, 
	 */
	public Vector<String> getNodeNames() {
		Vector<String> ret = new Vector<String>();
		Iterator<VNode> n = vNodes.iterator();
		while (n.hasNext()) {
			VNode actual = n.next();
			if ((actual.index + 1) > ret.size()) {
				ret.setSize(actual.index + 1);
			}
			if (actual.index!=0) //kein temp-knoten
				ret.set(actual.index, getNodeName(actual.index));
		}
		return ret;
	}
	/**
	 * add edges from a given node to evey selected node
	 * 
	 * @param Start 
	 * 				the source of all new edges
	 */
	public void addEdgestoSelectedNodes(VNode Start) {
		pushNotify(new GraphMessage(GraphMessage.EDGE,GraphMessage.ADDITION|GraphMessage.BLOCK_START));
		Iterator<VNode> iter = vNodes.iterator();
		while (iter.hasNext()) 
		{
				VNode temp = iter.next();
				if (((temp.getSelectedStatus() & VItem.SELECTED) == VItem.SELECTED) && (temp != Start)) 
				{
					int i = getNextEdgeIndex();
					//Standard ist eine StraightLineEdge
					addEdge(new VStraightLineEdge(i,GeneralPreferences.getInstance().getIntValue("edge.width")),Start.index,temp.index,GeneralPreferences.getInstance().getIntValue("edge.value"));
					if (Start.index==0)
						setEdgeName(i,"\u22C6");
					else
						setEdgeName(i,GeneralPreferences.getInstance().getEdgeName(i, Start.index, temp.index));
				}
		}
		this.pushNotify(new GraphMessage(GraphMessage.EDGE,GraphMessage.BLOCK_END));
	}
	/**
	 * add edges from evey selected node to a given node
	 * 
	 * @param Ende
	 * 				the target of all new edges
	 */
	public void addEdgesfromSelectedNodes(VNode Ende) {
		this.pushNotify(new GraphMessage(GraphMessage.EDGE,GraphMessage.ADDITION|GraphMessage.BLOCK_START));
		Iterator<VNode> iter = vNodes.iterator();
		while (iter.hasNext()) 
		{
				VNode temp = iter.next();
				if (((temp.getSelectedStatus()&VItem.SELECTED)==VItem.SELECTED) && (temp != Ende)) 
				{
					int i = getNextEdgeIndex();
					//Standard ist eine StraightLineEdge
					addEdge(new VStraightLineEdge(i,GeneralPreferences.getInstance().getIntValue("edge.width")),temp.index,Ende.index,GeneralPreferences.getInstance().getIntValue("edge.value"));
					if (Ende.index==0)
						setEdgeName(i,"\u22C6");
					else
						setEdgeName(i,GeneralPreferences.getInstance().getEdgeName(i, temp.index, Ende.index));
				}
		}
		this.pushNotify(new GraphMessage(GraphMessage.EDGE,GraphMessage.BLOCK_END));
	}
	/**
	 * get the number of nodes
	 * @return the number of nodes
	 */
	public int NodeCount()
	{
		return mG.NodeCount();
	}
	/**
	 * Get a new Node Iterator
	 * 
	 * @return the Iterator typed with VNode
	 */
	public Iterator<VNode> getNodeIterator()
	{
		return vNodes.iterator();
	}
	//
	//
	// Kantenmethoden
	//
	//
	/**
	 * add a new edge with given visual information in a VEdge from a source to a target
	 * the value of the edge is set to 1
	 * 
	 * @param edge 
	 * 				the new VEdge
	 * @param s 
	 * 				the source of the VEdge
	 * @param e
	 * 				the target of the VEdge
	 */
	public void addEdge(VEdge edge, int s, int e) {
		addEdge(edge, s, e, 1,"");
	}
	/**
	 * add a new edge with given visual information in a VEdge from a source to a target
	 * the value of the edge is set to 1
	 * 
	 * @param edge 
	 * 				the new VEdge
	 * @param s 
	 * 				the source of the VEdge
	 * @param e
	 * 				the target of the VEdge
	 * @param name 
	 * 				name of the edge (optional)
	 */
	public void addEdge(VEdge edge, int s, int e, String name) {
		addEdge(edge, s, e, 1,name);
	}
	/**
	 * add a new edge with given visual information in a VEdge from a source to a target and a value without a name 
	 * 
	 * @param edge 
	 * 				the new VEdge
	 * @param s 
	 * 				the source of the edge
	 * @param e
	 * 				the target of the edge
	 * @param v
	 * 				the value of the  edge
	 * 
	 */
	public void addEdge(VEdge edge, int s, int e, int v)
	{
		addEdge(edge,s,e,v,"");
	}
	/**
	 * add a new edge with given visual information in a VEdge from a source to a target and a value 
	 * 
	 * @param edge 
	 * 				the new VEdge
	 * @param s 
	 * 				the source of the edge
	 * @param e
	 * 				the target of the edge
	 * @param v
	 * 				the value of the  edge
	 * @param name
	 * 				name of the edge
	 */
	public void addEdge(VEdge edge, int s, int e, int v, String name) 
	{
		if (similarPathEdgeIndex(edge,s,e) > 0)
		{
			System.err.println("DEBUG : Similar Edge Exists, doing nothing");
			return;
		}
		if (mG.addEdge(edge.index, s, e, v)) //succesfull added in MathGraph
		{
			EdgeLock.lock();
			try 
			{
				// In einem ungerichteten Graphen existiert eine Kante von e zu s und die ist StraightLine und die neue Kante ist dies auch	
				if ((s!=e)&&(mG.isDirected())&&(mG.existsEdge(e, s)==1)&&(getEdge(getEdgeIndices(e,s).firstElement()).getType()==VEdge.STRAIGHTLINE)&&(edge.getType()==VEdge.STRAIGHTLINE))
				{ //Dann würde diese Kante direkt auf der anderen liegen
					Point start = getNode(s).getPosition();
					Point ende = getNode(e).getPosition();
					Point dir = new Point(ende.x-start.x,ende.y-start.y);
					double length = dir.distanceSq(new Point(0,0));
					Point.Double orthogonal_norm = new Point.Double ((double)dir.y/length,-(double)dir.x/length);
					Point bz1 = new Point(Math.round((float)start.x + (float)dir.x/2 + (float)orthogonal_norm.x*(float)length/4),Math.round((float)start.y + (float)dir.y/2 + (float)orthogonal_norm.y*(float)length/4));
					Point bz2 = new Point(Math.round((float)start.x + (float)dir.x/2 - (float)orthogonal_norm.x*(float)length/4),Math.round((float)start.y + (float)dir.y/2 - (float)orthogonal_norm.y*(float)length/4));
					float arrsize = edge.getArrowSize();float arralpha = edge.getArrowAngle();float arrpart = edge.getArrowPart(); float arrpos = edge.getArrowPos();
					//Update the new Edge
					edge = new VQuadCurveEdge(edge.index,edge.width,bz1);
					edge.setArrowSize(arrsize);edge.setArrowAngle(arralpha);edge.setArrowPart(arrpart); edge.setArrowPos(arrpos);
					//Update the old edge
					VEdge temp = getEdge(getEdgeIndices(e,s).firstElement());
					arrsize = temp.getArrowSize();arralpha = temp.getArrowAngle();arrpart = temp.getArrowPart(); arrpos = temp.getArrowPos();
					vEdges.remove(temp);
					temp = new VQuadCurveEdge(temp.index,temp.width,bz2);
					temp.setArrowSize(arrsize);temp.setArrowAngle(arralpha);temp.setArrowPart(arrpart); temp.setArrowPos(arrpos);
					Iterator<VSubSet> siter = vSubSets.iterator();
					//The new edge color must be rebuild. The Subsets are all up to date because the index hasn't changed
					while(siter.hasNext())
					{
						VSubSet actual = siter.next();
						if (SubSetcontainsEdge(temp.index, actual.getIndex()))
							temp.addColor(actual.getColor());
					}
					vEdges.add(temp); //add modified edge in counter directtion
				}
				vEdges.add(edge); //add edge
				mG.setEdgeName(edge.index, name);
			} 
			finally {EdgeLock.unlock();}
			setChanged();
			notifyObservers(new GraphMessage(GraphMessage.EDGE,edge.index,GraphMessage.ADDITION,GraphMessage.EDGE));	
		}
	}
	/**
	 * Checks whether an similar edge exists between s and e
	 * If multiple edges are allowed, an edge is similar, if it has same type and same path
	 * If multiples are not allowed, an edge is similar, if it has same start and end
	 * 
	 * An Edge is not similar to itself because than every edge that is already in the graph an checked against it would return true
	 * 
	 * @param edge edge to be checked to similarity existence
	 * @param s start node index
	 * @param e end node index
	 * @return the index of the similar dge, if it exists, else 0
	 */
	public int similarPathEdgeIndex(VEdge edge, int s, int e)
	{
		if (edge==null)
			return 0;
		//Check whether an edge is the same as this if multiples are allowed
		if (mG.isMultipleAllowed())
		{
			Vector<Integer> indices = mG.getEdgeIndices(s,e);
			Iterator<Integer> iiter = indices.iterator();
			while (iiter.hasNext())
			{
				VEdge act = getEdge(iiter.next());
				if ((mG.getEdgeProperties(act.index).get(MGraph.EDGESTARTINDEX)==e)&&(!mG.isDirected())&&(act.getType()==VEdge.ORTHOGONAL)&&(edge.getType()==VEdge.ORTHOGONAL)) 
				//ungerichtet, beide orthogonal und entgegengesetz gespeichert
				{
					if (((VOrthogonalEdge)act).getVerticalFirst()!=((VOrthogonalEdge)edge).getVerticalFirst())
						return act.index;
				}
				else if ((edge.PathEquals(act))&&(edge.index!=act.index)) //same path but different indexx
				{
					return act.index;
				}

			}
		}
		else if (mG.getEdgeIndices(s,e).size()>0)
		{
			return mG.getEdgeIndices(s,e).firstElement();
		}
		return 0;
	}
	/**
	 * get the edge with a given index, if existens
	 * 
	 * @param i
	 * 			index of the searched edge
	 * @return the edge, if existens, else null 
	 */
	public VEdge getEdge(int i) {
		Iterator<VEdge> n = vEdges.iterator();
		while (n.hasNext()) {
			VEdge temp = n.next();
			if (temp.index == i) {
				return temp;
			}
		}
		return null;
	}
	/**
	 * Get the indices of Edges between these two nodes
	 * @param start start node definnied by index
	 * @param ende end node
	 * @return the indices of the edges between start and end
	 * @see MGraph.getEdgeIndex(source,target)
	 */
	public Vector<Integer> getEdgeIndices(int start, int ende)
	{
		return mG.getEdgeIndices(start, ende);
	}
	/**
	 * remove the edge with index i
	 * @param i
	 * 				index of the edge to be removed
	 * @return
	 * 			true, if an edge was removed, false, if not
	 */
	public boolean removeEdge(int i)
	{
		if (removeEdge_(i))
		{
			setChanged();
			notifyObservers(new GraphMessage(GraphMessage.EDGE,i,GraphMessage.REMOVAL,GraphMessage.EDGE|GraphMessage.SUBSET|GraphMessage.SELECTION));	
			return true;
		}
		return false;
	}
	/**
	 * removes an Edge from the Graph without notifying the Observers
	 * ATTENTION : Internal Use only, if you Use this method make sure you notify the Observers yourself!
	 * @param i Index of the Edge to be removed
	 * @return
	 */
	private boolean removeEdge_(int i) {
		if (getEdge(i) != null) {
			Iterator<VSubSet> s = vSubSets.iterator();
			while (s.hasNext()) {
				removeEdgefromSubSet_(i, s.next().getIndex());
			}
			mG.removeEdge(i);
			vEdges.remove(getEdge(i));
			return true;
		} else {
			return false;
		}
	}
	/**
	 * get the smallest edge index beyond all used edge indices
	 * @return a unused edge index
	 */
	public int getNextEdgeIndex() {
		return mG.getNextEdgeIndex();
	}
	/**
	 * get the edge in Range of a given point.
	 * an edge is in Range, if the distance from the edge-line or line segments to the point p is smaller than the edge width
	 * <br><br>
	 * <i>not very exact at the moment</i>
	 * 
	 * @param p a point
	 * @param variation the variation m may be away from the edge
	 * @return the first edge in range, if there is one, else null
	 */
	public VEdge getEdgeinRange(Point m, double variation) {
		//System.err.println("("+m.x+","+m.y+") Variation is "+variation+" zoom "+(float)GeneralPreferences.getInstance().getIntValue("vgraphic.zoom")/100);
		variation *=(float)GeneralPreferences.getInstance().getIntValue("vgraphic.zoom")/100; //jop is gut
		Iterator<VEdge> n = vEdges.iterator();
		while (n.hasNext()) {
			VEdge temp = n.next();
			// naechste Kante
			Vector<Integer> Properties = mG.getEdgeProperties(temp.index); // Deren abstrakte Werte
			Point p1 = (Point)getNode(Properties.get(MGraph.EDGESTARTINDEX)).getPosition().clone();
			Point p2 = (Point)getNode(Properties.get(MGraph.EDGEENDINDEX)).getPosition().clone();
			// getEdgeShape
			GeneralPath p = temp.getPath(p1, p2,1.0f); //no zoom on check!
		    PathIterator path = p.getPathIterator(null, 0.001); 
		    // 0.005 = the flatness; reduce if result is not accurate enough!
		    double[] coords = new double[2];
		    double x = 0.0, y = 0.0, lastx=0.0, lasty = 0.0;
		    double closestDistanceSquare = Double.MAX_VALUE;
		    while( !path.isDone() ) 
		    {
		    	int type = path.currentSegment(coords);
		    	x = coords[0]; y = coords[1];
		    	switch(type)
		    	{
		    		case PathIterator.SEG_LINETO:
		    		{
		    			double v = variation + (float)temp.width;
		    			Rectangle2D.Double r = new Rectangle.Double(m.x-v/2,m.y-v/2,v,v);
		    			Line2D.Double l = new Line2D.Double(lastx,lasty,x,y);
		    			if (l.intersects(r))
		    			{
		    				return temp;
		    			}
		    			break;
		    		}
		    		case PathIterator.SEG_MOVETO: break;
		    		default:
		    		{
				    	//System.err.print("("+new Double(x).intValue()+","+new Double(y).intValue()+") ");
				    	double distanceSquare = Point.distanceSq(x,y,m.x,m.y);
				    	if (distanceSquare < (variation+(float)temp.width)) 
				    	{
					    		return temp;
					    }		    			
		    		}
		    	}
		    	lastx = x; lasty = y;
		    	path.next();
		    }
		    //if the shortest distance is smaller than  
		    if (closestDistanceSquare < (variation+(float)temp.width))
		    	return temp;
		}
		return null; // keinen gefunden
	}
	/**
	 * Check, whether at least one node is selected
	 * @return true, if there is at least one selected node, else false
	 */
	public boolean selectedEdgeExists() {
		Iterator<VEdge> n = vEdges.iterator();
		while (n.hasNext()) {
			if ((n.next().getSelectedStatus()&VItem.SELECTED)==VItem.SELECTED)
				return true;
		}
		return false;
	}
	/**
	 * returns a vector with the mathematical properties of an edge, if the edge exists
	 * <br><br>Use the MGraph CONSTANTS to extract single values!
	 * @param i
	 * @return a vector with the int properties of the edge, if no edge exists null
	 * @see MGraph.getEdgeProperties(int index)
	 */
	public Vector<Integer> getEdgeProperties(int i) {
		return mG.getEdgeProperties(i);
	}
	/**
	 * get a list of the edge names in a vector, where each edge name is stored at it's index
	 * every other component of the vector is null
	 * <br><br>
	 * an edge name is the mathematical given edge name 
	 * <br><br>
	 * @return a Vector of all edge names, 
	 */	
	public Vector<String> getEdgeNames() {
		Vector<String> ret = new Vector<String>();
		Iterator<VEdge> n = vEdges.iterator();
		while (n.hasNext()) {
			VEdge actual = n.next();
			if ((actual.index + 1) > ret.size()) {
				ret.setSize(actual.index + 1);
			}
			Vector<Integer> v = getEdgeProperties(actual.index);
			if (v==null)
			{
				//In diesem Fall ist die Aktualisierung zu langsam
			}
			else if ((v.elementAt(MGraph.EDGESTARTINDEX).toString().equals("0"))&&(v.elementAt(MGraph.EDGEENDINDEX).toString().equals("0")))
			{
				//temporäre Kante
			}
			else
			{
				ret.set(actual.index, mG.getEdgeName(actual.index));
			}
		}
		return ret;
	}
	/**
	 * returns the name of a node with the index, if this node exists
	 * @param i index of the node 
	 * @return null, if no node with index i exists, else the name
	 * @see MGraph.getNodeName()
	 */
	public String getEdgeName(int i) {
		return mG.getEdgeName(i);
	}
	/**
	 * sets the the name of a node with the index, if this node exists, else it does nothing
	 * @param i
	 * @param newname
	 * @see MGraph.setNodeName()
	 */
	public void setEdgeName(int i, String newname) {
		mG.setEdgeName(i, newname);
		setChanged();
		notifyObservers(new GraphMessage(GraphMessage.EDGE,i,GraphMessage.UPDATE,GraphMessage.EDGE));	
	}
	/**
	 * get the Number of edges contained in this graph
	 * @return the number of edges in this graph
	 */
	public int EdgeCount()
	{
		return mG.EdgeCount();
	}
	/**
	 * get a new edge iterator
	 * @return
	 * 			a Iterator of type VEdge
	 */
	public Iterator<VEdge> getEdgeIterator() {
		return vEdges.iterator();
	}
	/**
	 * get an Control point near the point m
	 * a Control point is any point in a QuadCurveEdge or SegmentedEdge despite source and target
	 * 
	 * @param m
	 * 			a Point 
	 * @param abweichung
	 * 			the distance from the point m
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Vector getControlPointinRange(Point m, int abweichung) {
		Iterator<VEdge> n = vEdges.iterator();
		while (n.hasNext()) {
			VEdge temp = n.next(); // naechste Kante
			switch (temp.getType()) {
				case VEdge.LOOP :
				case VEdge.QUADCURVE : // Wenns eine Bezierkante ist
				{
					Point p = temp.getControlPoints().firstElement();
					if (p.distance(m) <= ((float) abweichung)) {
						Vector c = new Vector();
						c.add(temp);
						c.add(new Integer(0));
						return c;
					}
					break;
				}
				case VEdge.SEGMENTED : {
					Vector<Point> p = temp.getControlPoints();
					for (int i = 0; i < p.size(); i++) {
						if (p.get(i).distance(m) <= ((float) abweichung)) {
							Vector c = new Vector();
							c.add(temp); // Kante anfügen
							c.add(new Integer(i)); // Punkt angeben
							return c;
						}
					}
					break;
				}
				default : {
					break;
				} // Sonst - Straightline
			}
		}
		return null; // keinen gefunden
	}
	//
	//
	// Untergraphenmethoden
	//
	//
	/**
	 * add a new Set to the VGraph
	 * @param SetIndex
	 * 				index of the new Set, must not be used by other Sets
	 * @param SetName
	 * 				name of the new Set
	 * @patam SetColor
	 * 				Color of the new Set (that is given to the nodes or edges in this set or the sets boundary)
	 */
	public void addSubSet(int SetIndex, String SetName, Color SetColor) {
		if (getSubSet(SetIndex)!=null)
			return;
		mG.addSubSet(SetIndex, SetName);
		vSubSets.add(new VSubSet(SetIndex, SetColor));
		setChanged();
		notifyObservers(new GraphMessage(GraphMessage.SUBSET,SetIndex,GraphMessage.ADDITION,GraphMessage.SUBSET));	
	}
	/**
	 * remove a set from the VGraph and remove the Sets color from each node or edge contained in the set
	 * <br><br>
	 * if no set exists with given index SetIndex nothing happens
	 * 
	 * @param SetIndex
	 * 					Index of the set to be deleted
	 */
	public void removeSubSet(int SetIndex) {
		VSubSet toDel = null;
		if (getSubSet(SetIndex)==null)
			return;
		Iterator<VNode> iterNode = vNodes.iterator();
		while (iterNode.hasNext()) {
			VNode actual = iterNode.next();
			if (SubSetcontainsNode(actual.index, SetIndex))
				removeNodefromSubSet_(actual.index, SetIndex);
		}
		Iterator<VEdge> iterEdge = vEdges.iterator();
		while (iterEdge.hasNext()) {
			VEdge actual = iterEdge.next();
			if (SubSetcontainsEdge(actual.index, SetIndex))
				removeEdgefromSubSet_(actual.index, SetIndex);
		}
		Iterator<VSubSet> iterSet = vSubSets.iterator();
		while (iterSet.hasNext()) {
			VSubSet actual = iterSet.next();
			if (actual.getIndex() == SetIndex)
				toDel = actual;
		}
		vSubSets.remove(toDel);
		mG.removeSubSet(toDel.getIndex());
		setChanged();
		notifyObservers(new GraphMessage(GraphMessage.SUBSET,SetIndex,GraphMessage.REMOVAL,GraphMessage.ALL_ELEMENTS));	
	}
	/**
	 * get the smallest subset index beyond all used subset indices
	 * @return a unused subset index
	 */
	public int getNextSubSetIndex() {
		return mG.getNextSetIndex();
	}
	/**
	 * Add a node to the Set
	 * if the node or the set does not exist, nothing happens
	 *
	 * @param nodeindex
	 * 					the node to be added
	 * @param SetIndex
	 * 					the set to be expanded
	 * 
	 * @see MGraph.addNodetoSet(nodeindex,setindex)
	 */
	public void addNodetoSubSet(int nodeindex, int SetIndex) {
		if ((mG.existsNode(nodeindex)) && (mG.getSubSet(SetIndex)!=null)
				&& (!mG.SubSetcontainsNode(nodeindex, SetIndex))) {
			// Mathematisch hinzufuegen
			mG.addNodetoSubSet(nodeindex, SetIndex);
			// Und Knotenfarbe updaten
			Iterator<VSubSet> iter = vSubSets.iterator();
			while (iter.hasNext()) {
				VSubSet actual = iter.next();
				if (actual.getIndex() == SetIndex) {
					VNode t = getNode(nodeindex);
					t.addColor(actual.getColor());
				}
			}
		}
		setChanged();
		// Knoten wurden verändert und Sets
		notifyObservers(new GraphMessage(GraphMessage.SUBSET,SetIndex,GraphMessage.UPDATE,GraphMessage.SUBSET|GraphMessage.NODE));	
	}
	/**
	 * remove a node from a set
	 * if the node or the set does not exist or the node is not in the set, nothing happens
	 * 
	 * @param nodeindex
	 * 				the node index to be removed from the
	 * @param SetIndex
	 * 				set with this index
	 */
	public void removeNodefromSubSet(int nodeindex, int SetIndex) {
		removeNodefromSubSet_(nodeindex,SetIndex);		
		setChanged();
		notifyObservers(new GraphMessage(GraphMessage.SUBSET,SetIndex,GraphMessage.UPDATE,GraphMessage.SUBSET|GraphMessage.NODE));	
	}
	/**
	 * remove Node from Sub set without informing the observsers
	 * ATTENTION : Internal Use only, if you use this method make sure to notify Observers !
	 * @param nodeindex
	 * @param SetIndex
	 */
	private void removeNodefromSubSet_(int nodeindex, int SetIndex)
	{
		if (mG.SubSetcontainsNode(nodeindex, SetIndex)) {
			// Mathematisch hinzufuegen
			mG.removeNodefromSet(nodeindex, SetIndex);
			// Und Knotenfarbe updaten
			Iterator<VSubSet> iter = vSubSets.iterator();
			while (iter.hasNext()) {
				VSubSet actual = iter.next();
				if (actual.getIndex() == SetIndex) {
					getNode(nodeindex).removeColor(actual.getColor());
				}
			}
		}
	}

	/**
	 * Indicates whether a node is contained in a set
	 * @param nodeindex
	 * 				nodeindex
	 * @param SetIndex
	 * 				setindex
	 * @return true if set and node exist and the node is in the set
	 */
	public boolean SubSetcontainsNode(int nodeindex, int SetIndex) {
		return mG.SubSetcontainsNode(nodeindex, SetIndex);
	}
	/**
	 * add an Edge to a set
	 * 
	 * @param edgeindex
	 * 			edgeindex
	 * @param SetIndex
	 * 			setindex
	 * @see MGraph.addEdgetoSet(edgeindex,setindex)
	 */
	public void addEdgetoSubSet(int edgeindex, int SetIndex) {
		if ((mG.getEdgeProperties(edgeindex) != null)
				&& (mG.getSubSet(SetIndex)!=null)
				&& (!mG.SubSetcontainsEdge(edgeindex, SetIndex))) {
			// Mathematisch hinzufuegen
			mG.addEdgetoSubSet(edgeindex, SetIndex);
			// Und Knotenfarbe updaten
			Iterator<VSubSet> iter = vSubSets.iterator();
			while (iter.hasNext()) {
				VSubSet actual = iter.next();
				if (actual.getIndex() == SetIndex) {
					getEdge(edgeindex).addColor(actual.getColor());
				}
			}
		}
		setChanged();
		notifyObservers(new GraphMessage(GraphMessage.SUBSET,SetIndex,GraphMessage.UPDATE,GraphMessage.SUBSET|GraphMessage.EDGE));	
	}
	/**
	 * remove an edge from a set
	 * @param edgeindex
	 * 				edge index of the edge to be removed 
	 * @param SetIndex
	 * 				set index of the set
	 */
	public void removeEdgefromSubSet(int edgeindex, int SetIndex) {
		removeEdgefromSubSet_(edgeindex,SetIndex);
		setChanged();
		notifyObservers(new GraphMessage(GraphMessage.SUBSET,SetIndex,GraphMessage.UPDATE,GraphMessage.SUBSET|GraphMessage.EDGE));	
	}
	/**
	 * remove an edge from a set without informing the observers 
	 * ATTENTION : Internal Use only, if you use this methd make sure to notify Observers yourself !
	 * @param edgeindex
	 * @param SetIndex
	 */
	private void removeEdgefromSubSet_(int edgeindex, int SetIndex)
	{	if (mG.SubSetcontainsEdge(edgeindex, SetIndex)) {
		// Mathematisch hinzufuegen
		mG.removeEdgefromSet(edgeindex, SetIndex);
		// Und Knotenfarbe updaten
		Iterator<VSubSet> iter = vSubSets.iterator();
		while (iter.hasNext()) {
			VSubSet actual = iter.next();
			if (actual.getIndex() == SetIndex) {
				getEdge(edgeindex).removeColor(actual.getColor());
			}
		}
	}
}
	/**
	 * Check whether an edge is contained in a subset
	 * @param edgeindex
	 * 				the edge index
	 * @param SetIndex
	 * 			the set index
	 * @return true if and only if the edge exists and the set exists and the edge is in the set
	 */	
	public boolean SubSetcontainsEdge(int edgeindex, int SetIndex) {
		return mG.SubSetcontainsEdge(edgeindex, SetIndex);
	}
	/**
	 * get a list of the subset names in a vector, where each subset name is stored at it's index
	 * every other component of the vector is null
	 * @return a Vector of all subset names, 
	 */	
	public Vector<String> getSetNames() {
		Vector<String> ret = new Vector<String>();
		Iterator<VSubSet> s = vSubSets.iterator();
		while (s.hasNext()) {
			VSubSet actual = s.next();
			if ((actual.index + 1) > ret.size()) {
				ret.setSize(actual.index + 1);
			}
			ret.set(actual.getIndex(), mG.getSubSetName(actual.index));
		}
		return ret;
	}
	/**
	 * get the set with index i
	 * @param i
	 * 		index of the set
	 * @return
	 * 		null, if no set with index i exists, else the set
	 */
	public VSubSet getSubSet(int i) {
		Iterator<VSubSet> s = vSubSets.iterator();
		while (s.hasNext()) {
			VSubSet actual = s.next();
			if (i == actual.getIndex()) {
				return actual;
			}
		}
		return null;
	}
	/**
	 * Set the Color of a Subset to a new color. Returns true, if the color was changed, else false.
	 * The color is not changed, if another Subset already got that color
	 * @param newcolour
	 */
	public boolean setSubSetColor(int SetIndex, Color newcolour)
	{
		VSubSet actual=null;
		Iterator<VSubSet> subsetiter = vSubSets.iterator();
		while (subsetiter.hasNext())
		{
			VSubSet s = subsetiter.next();
			if (s.getIndex()==SetIndex)
				actual = s;
			else if (s.getColor().equals(newcolour))
				return false;
		}
		if (actual==null)
			return false;
		Iterator<VNode> nodeiter = vNodes.iterator();
		while (nodeiter.hasNext())
		{
			VNode n = nodeiter.next();
			if (SubSetcontainsNode(n.index,SetIndex))
			{
				n.removeColor(actual.getColor()); n.addColor(newcolour);
			}
		}
		Iterator<VEdge> edgeiter = vEdges.iterator();
		while (edgeiter.hasNext())
		{
			VEdge n = edgeiter.next();
			if (SubSetcontainsEdge(n.index,SetIndex))
			{
				n.removeColor(actual.getColor()); n.addColor(newcolour);
			}
		}
		actual.setColor(newcolour);
		return true;
	}

	/**
	 * get the name of the set with index i
	 * @param i
	 * 			index of the set
	 * @return
	 * 		null, if no set with index i exists, else the name as a string
	 * @see MGraph.getSetName(int i)
	 */
	public String getSubSetName(int i) {
		return mG.getSubSetName(i);
	}
	/**
	 * set the name of Set with index i
	 * @param i
	 * 			index of the set
	 * @param s
	 * 			new name
	 */
	public void setSubSetName(int i,String s) {
		mG.setSubSetName(i,s);
	}
	/**
	 * returns the number of Subsets existing
	 * @return the number of SubSets
	 */
	public int SubSetCount() {
		return mG.SubSetCount();
	}
	/**
	 * get a new Iterator for the subsets
	 * @return
	 * 		an Iterator typed to VSubSet
	 */	
	public Iterator<VSubSet> getSubSetIterator() {
		return vSubSets.iterator();
	}
}
