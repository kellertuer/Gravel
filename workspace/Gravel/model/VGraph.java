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
 * The MGraph may the extracted and being used to generate another VGraph to the same MGraph
 *
 * The VGraph is observable, so that every Observer may update itself, if something changes here
 *
 * the update may give Information about the updated parts by the GraphMessage
 *
 * @author Ronny Bergmann 
 */
public class VGraph extends Observable {

	private TreeSet<VNode> vNodes;
	TreeSet<VEdge> vEdges;
	private HashSet<VSubSet> vSubSets;
	private Lock EdgeLock, NodeLock;
	public MGraph mG;
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
	// Allgemeine Methoden
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
			removeNode_(n.next().getIndex());
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
			removeEdge_(n2.next().getIndex());
		}
		setChanged();
		notifyObservers(
			new GraphMessage(GraphMessage.SELECTION|GraphMessage.NODE|GraphMessage.EDGE, //Changed
							GraphMessage.REMOVAL, //Status 
							GraphMessage.NODE|GraphMessage.EDGE|GraphMessage.SELECTION) //Affected		
		);		
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
							if (t.getIndex() < t2.getIndex())
							{
								Vector<Integer> ttot2 = mG.getEdgeIndices(t.getIndex(),t2.getIndex());
								Vector<Integer> t2tot = mG.getEdgeIndices(t2.getIndex(),t.getIndex());
								//In the nonmultiple case each Vector has exactely one or no edge in it
								if ((!ttot2.isEmpty())&&(!t2tot.isEmpty()))
								{
									int e1 = ttot2.firstElement();
									int e2 = t2tot.firstElement();
									MEdge m = mG.getEdge(e2);
									m.Value = mG.getEdge(e2).Value+mG.getEdge(e1).Value;
								//	mG.replaceEdge(m); Notify is pushed for MGraph at the end of the method
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
						int ts = mG.getEdge(t.getIndex()).StartIndex;
						int te = mG.getEdge(t.getIndex()).StartIndex;
						Vector<Integer> indices = mG.getEdgeIndices(ts,te);
						Iterator<Integer> iiter = indices.iterator();
						while (iiter.hasNext())
						{
							VEdge act = getEdge(iiter.next());
							if ((mG.getEdge(act.getIndex()).StartIndex==te)&&(!mG.isDirected())&&(act.getType()==VEdge.ORTHOGONAL)&&(t.getType()==VEdge.ORTHOGONAL)) 
							//ungerichtet, beide orthogonal und entgegengesetz gespeichert
							{
								if ((((VOrthogonalEdge)act).getVerticalFirst()!=((VOrthogonalEdge)t).getVerticalFirst())&&(!removed.get(act.getIndex())))
								{
									//System.err.println("removing Edge #"+t.index+" because ORTH and it is similar to #"+act.index);
									toDelete.add(t);
									removed.set(t.getIndex());
								}
							}
							else if ((t.PathEquals(act)&&(!removed.get(act.getIndex())))&&(t.getIndex()!=act.getIndex())) //same path
							{
								//System.err.println("removing Edge #"+t.index+" because it is similar to #"+act.index);
								toDelete.add(t);
								removed.set(t.getIndex());
							}
						} //end inner while
					} //end outer while
					Iterator<VEdge> e3 = toDelete.iterator();
					while (e3.hasNext())
						removeEdge_(e3.next().getIndex());
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
	 * <br>The pushNotify Should be used to indicate complete replacement to reset observers
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
										GraphMessage.REPLACEMENT, //Status 
										GraphMessage.ALL_ELEMENTS) //Affected		
		);
		setChanged();
		notifyObservers(
				new GraphMessage(GraphMessage.ALL_ELEMENTS, //Type
						GraphMessage.REPLACEMENT, //Status 
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
			clone.addSubSet(actualSet,mG.getSubSet(actualSet.index)); //Jedes Set kopieren
		}
		//Knoten
		Iterator<VNode> n2 = vNodes.iterator();
		while (n2.hasNext())
		{
			VNode nodeclone = n2.next().clone();
			clone.addNode(nodeclone, mG.getNode(nodeclone.getIndex()));
			//In alle Sets einfuegen
			n1 = vSubSets.iterator();
			while (n1.hasNext())
			{
				VSubSet actualSet = n1.next();
				if (mG.SubSetcontainsNode(nodeclone.getIndex(),actualSet.index))
					clone.addNodetoSubSet(nodeclone.getIndex(),actualSet.index); //In jedes Set setzen wo er war
			}
		}
		//Analog Kanten
		Iterator<VEdge> n3 = vEdges.iterator();
		while (n3.hasNext())
		{
			VEdge cloneEdge = n3.next().clone();
			MEdge me = mG.getEdge(cloneEdge.getIndex());
			clone.addEdge(cloneEdge,me);
			//In alle Sets einfuegen
			n1 = vSubSets.iterator();
			while (n1.hasNext())
			{
				VSubSet actualSet = n1.next();
				if (mG.SubSetcontainsEdge(cloneEdge.getIndex(),actualSet.index))
					clone.addEdgetoSubSet(cloneEdge.getIndex(),actualSet.getIndex()); //Jedes Set kopieren
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
				int adv = metrics.stringWidth(mG.getNode(actual.getIndex()).name);
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
				int adv = metrics.stringWidth(mG.getNode(actual.getIndex()).name);
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
					MEdge me = mG.getEdge(e.getIndex());
					if (me.StartIndex==me.EndIndex)
					{
						removed.set(e.getIndex());
						deledges.add(e);
					}
					else
						removed.clear(e.getIndex());
				}
				Iterator<VEdge> n3 = deledges.iterator();
				while (n3.hasNext()) // Diese loeschen
				{
					removeEdge(n3.next().getIndex());
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
						if (((!mG.isDirected())&&(t2.getIndex()<=t.getIndex()))||(mG.isDirected())) //in the nondirected case only half the cases
						{
							if (mG.existsEdge(t.getIndex(),t2.getIndex())>1) //we have to delete
							{
								Vector<Integer> multipleedges = mG.getEdgeIndices(t.getIndex(),t2.getIndex());
								int value = mG.getEdge(multipleedges.firstElement()).Value;
								//Add up the values and remove the edges from the second to the last
								Iterator<Integer> iter = multipleedges.iterator();
								iter.next();
								while(iter.hasNext())
								{
									int nextindex = iter.next();
									value += mG.getEdge(nextindex).Value;
									removeEdge(nextindex);
									removed.set(nextindex);
								}
								MEdge e = mG.getEdge(multipleedges.firstElement());
								e.Value = value;
								//mG.replaceEdge(e); Notify is pushed below
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
	 * Adds a new Node to the VGraph (and the corresponding mnode to the MGraph underneath)
	 * If one of the arguments is null nothing happens
	 * If a Node with the index of the VNode already exists, nothing happens
	 * 
	 * @param node
	 *            the new VNode
	 * @param mnode
	 *            the depending mnode. If its index differs from the VNode its set to the VNodes index
	 */
	public void addNode(VNode node, MNode mnode) {
		if ((node==null)||(mnode==null))
			return;
		if (getNode(node.getIndex()) == null) {
			if (mnode.index!=node.getIndex())
				mnode.index = node.getIndex();
			vNodes.add(node);
			mG.addNode(mnode);
			setChanged();
			//Graph changed with an add, only nodes affected
			notifyObservers(new GraphMessage(GraphMessage.NODE,node.getIndex(),GraphMessage.ADDITION,GraphMessage.NODE));	
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
			if (temp.getIndex() == i) {
				return temp;
			}
		}
		return null;
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
	 * Change the index of a node
	 * each other value of a node can be changed by replaceNode
	 * but an index update requires an modification of adjacent edges
	 * 
	 * @param oldi
	 * @param newi
	 */
	public void changeNodeIndex(int oldi, int newi)
	{
		if (oldi==newi)
			return;
		if ((getNode(oldi)==null)||(getNode(newi)!=null)) //old not or new already in use
			return;
		mG.changeNodeIndex(oldi, newi);
		getNode(oldi).setIndex(newi);
		setChanged();
		notifyObservers(new GraphMessage(GraphMessage.NODE,GraphMessage.UPDATE, GraphMessage.ALL_ELEMENTS));	
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
			//System.err.println("not removing node "+i+" - not in Graph");
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
			if (Edges.get(aktuelle.getIndex())) // Aktuelle muss entfernt werden
			{
				toDelete.add(aktuelle);
			}
		}
		Iterator<VEdge> iter2 = toDelete.iterator();
		while (iter2.hasNext()) {
			removeEdge_(iter2.next().getIndex()); // loeschen
		}
		vNodes.remove(getNode(i));
	}
	/**
	 * Replace the node with index given by an copy of Node with the parameter,
	 * if a node with same index as parameter exists, else do nothing
	 * 
	 * Its SubSetStuff is not changed
	 * 
	 * @param node - Node that is exchanged into the graph (if a node exists with same index)
	 * @param mnode - mnode to change e.g. the name - if its index differs from the node-index, the node index is taken
	 * 
	 */
	public void replaceNode(VNode node, MNode mnode)
	{
		if ((node==null)||(mnode==null))
			return;
		if (mnode.index!=node.getIndex())
			mnode.index = node.getIndex();
		mG.replaceNode(new MNode(node.getIndex(), mnode.name));
		node = node.clone(); //Clone node to lose color
		NodeLock.lock(); //Knoten finden
		try
		{
			Iterator<VNode> n = vNodes.iterator();				
			while (n.hasNext())
			{
				VNode t = n.next();
				if (t.getIndex()==node.getIndex())
				{
					vNodes.remove(t);
					vNodes.add(node);
					setChanged();
					notifyObservers(new GraphMessage(GraphMessage.NODE,node.getIndex(), GraphMessage.REPLACEMENT,GraphMessage.NODE));	
					break;
				}
			}
		}
		finally {NodeLock.unlock();}
		Iterator<VSubSet> esi = this.vSubSets.iterator();
		while (esi.hasNext())
		{
			VSubSet s = esi.next();
			if (mG.SubSetcontainsNode(node.getIndex(), s.getIndex()))
				node.addColor(s.getColor());
		}

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
					int i = mG.getNextEdgeIndex();
					//Standard ist eine StraightLineEdge
					MEdge me;
					if (Start.getIndex()==0)
						me = new MEdge(i,Start.getIndex(),temp.getIndex(),GeneralPreferences.getInstance().getIntValue("edge.value"),"\u22C6");
					else
						me = new MEdge(i,Start.getIndex(),temp.getIndex(),GeneralPreferences.getInstance().getIntValue("edge.value"),GeneralPreferences.getInstance().getEdgeName(i, Start.getIndex(), temp.getIndex()));
					
						addEdge(new VStraightLineEdge(i,GeneralPreferences.getInstance().getIntValue("edge.width")),me);
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
					int i = mG.getNextEdgeIndex();
					//Standard ist eine StraightLineEdge
					MEdge me;
					if (Ende.getIndex()==0)
						me = new MEdge(i,temp.getIndex(),Ende.getIndex(),GeneralPreferences.getInstance().getIntValue("edge.value"),"\u22C6");
					else
						me = new MEdge(i,temp.getIndex(),Ende.getIndex(),GeneralPreferences.getInstance().getIntValue("edge.value"),GeneralPreferences.getInstance().getEdgeName(i, temp.getIndex(), Ende.getIndex()));					
						addEdge(new VStraightLineEdge(i,GeneralPreferences.getInstance().getIntValue("edge.width")),me);
				}
		}
		this.pushNotify(new GraphMessage(GraphMessage.EDGE,GraphMessage.BLOCK_END));
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
	 * add a new edge with given visual information in a VEdge from a source to a target and a value 
	 * 
	 * @param edge 
	 * 				the new VEdge
	 * @param medge 
	 * 				mathematical elements of the new edge, if its index differs, this index is ignored
	 */
	public void addEdge(VEdge edge, MEdge medge) 
	{
		if ((edge==null)||(medge==null))
				return;
		if (medge.index!=edge.getIndex())
			medge.index = edge.getIndex();
		if (similarPathEdgeIndex(edge,medge.StartIndex,medge.EndIndex) > 0)
		{
			System.err.println("DEBUG : Similar Edge Exists, doing nothing");
			return;
		}
		if (mG.addEdge(medge)) //succesfull added in MathGraph
		{
			EdgeLock.lock();
			try 
			{
				// In einem ungerichteten Graphen existiert eine Kante von e zu s und die ist StraightLine und die neue Kante ist dies auch	
				if ((medge.StartIndex!=medge.EndIndex)&&(mG.isDirected())&&(mG.existsEdge(medge.EndIndex, medge.StartIndex)==1)&&(getEdge(mG.getEdgeIndices(medge.EndIndex, medge.StartIndex).firstElement()).getType()==VEdge.STRAIGHTLINE)&&(edge.getType()==VEdge.STRAIGHTLINE))
				{ //Dann würde diese Kante direkt auf der anderen liegen
					Point start = getNode(medge.StartIndex).getPosition();
					Point ende = getNode(medge.EndIndex).getPosition();
					Point dir = new Point(ende.x-start.x,ende.y-start.y);
					double length = dir.distanceSq(new Point(0,0));
					Point.Double orthogonal_norm = new Point.Double ((double)dir.y/length,-(double)dir.x/length);
					Point bz1 = new Point(Math.round((float)start.x + (float)dir.x/2 + (float)orthogonal_norm.x*(float)length/4),Math.round((float)start.y + (float)dir.y/2 + (float)orthogonal_norm.y*(float)length/4));
					Point bz2 = new Point(Math.round((float)start.x + (float)dir.x/2 - (float)orthogonal_norm.x*(float)length/4),Math.round((float)start.y + (float)dir.y/2 - (float)orthogonal_norm.y*(float)length/4));
					VEdgeArrow arr = edge.getArrow().clone();
					//Update the new Edge
					edge = new VQuadCurveEdge(edge.getIndex(),edge.width,bz1);
					edge.setArrow(arr);
					//Update the old edge
					VEdge temp = getEdge(mG.getEdgeIndices(medge.EndIndex, medge.StartIndex).firstElement());
					arr = temp.getArrow().clone();
					vEdges.remove(temp);
					temp = new VQuadCurveEdge(temp.getIndex(),temp.width,bz2);
					temp.setArrow(arr);
					Iterator<VSubSet> siter = vSubSets.iterator();
					//The new edge color must be rebuild. The Subsets are all up to date because the index hasn't changed
					while(siter.hasNext())
					{
						VSubSet actual = siter.next();
						if (mG.SubSetcontainsEdge(temp.getIndex(), actual.getIndex()))
							temp.addColor(actual.getColor());
					}
					vEdges.add(temp); //add modified edge in counter directtion
				}
				vEdges.add(edge); //add edge
				mG.setEdgeName(edge.getIndex(), medge.name);
			} 
			finally {EdgeLock.unlock();}
			setChanged();
			notifyObservers(new GraphMessage(GraphMessage.EDGE,edge.getIndex(),GraphMessage.ADDITION,GraphMessage.EDGE));	
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
				MEdge me = mG.getEdge(act.getIndex());
				if ((me.StartIndex==e)&&(!mG.isDirected())&&(act.getType()==VEdge.ORTHOGONAL)&&(edge.getType()==VEdge.ORTHOGONAL)) 
				//ungerichtet, beide orthogonal und entgegengesetz gespeichert
				{
					if (((VOrthogonalEdge)act).getVerticalFirst()!=((VOrthogonalEdge)edge).getVerticalFirst())
						return act.getIndex();
				}
				else if ((edge.PathEquals(act))&&(edge.getIndex()!=act.getIndex())) //same path but different indexx
				{
					return act.getIndex();
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
			if (temp.getIndex() == i) {
				return temp;
			}
		}
		return null;
	}
	/**
	 * Replace the edge with index given by an copy of edge with the parameter,
	 * if a node with same index as parameter exists, else do nothing
	 * 
	 * Its SubSetStuff is not changed
	 * 
	 * 
	 * @param e VEdge to be replaced with its Edge with similar index in the graph
	 * @param me Medge containing the mathematical stuff of the VEdge - index of his edge is ignored,
	 * 			if it differs from first parameter index
	 */
	public void replaceEdge(VEdge e, MEdge me)
	{
		if (me.index!=e.getIndex())
			me.index = e.getIndex();
		
		mG.replaceEdge(me);
		e = e.clone(); //Lose color!
		EdgeLock.lock(); //Knoten finden
		try
		{
			Iterator<VEdge> ei = vEdges.iterator();				
			while (ei.hasNext())
			{
				VEdge t = ei.next();
				if (t.getIndex()==e.getIndex())
				{
					vEdges.remove(t);
					vEdges.add(e);
					setChanged();
					notifyObservers(new GraphMessage(GraphMessage.EDGE,e.getIndex(), GraphMessage.REPLACEMENT,GraphMessage.EDGE));	
					break;
				}
			}
		}
		finally {EdgeLock.unlock();}
		Iterator<VSubSet> esi = this.vSubSets.iterator();
		while (esi.hasNext())
		{
			VSubSet s = esi.next();
			if (mG.SubSetcontainsEdge(e.getIndex(), s.getIndex()))
				e.addColor(s.getColor());
		}
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
		variation *=(float)GeneralPreferences.getInstance().getIntValue("vgraphic.zoom")/100; //jop is gut
		Iterator<VEdge> n = vEdges.iterator();
		while (n.hasNext()) {
			VEdge temp = n.next();
			// naechste Kante
			MEdge me = mG.getEdge(temp.getIndex());
			Point p1 = (Point)getNode(me.StartIndex).getPosition().clone();
			Point p2 = (Point)getNode(me.EndIndex).getPosition().clone();
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
	 * @param variation
	 * 			the distance from the point m
	 * @return an Vector containing an edge and the number of its CP in Range if exists, else null
	 */
	@SuppressWarnings("unchecked")
	public Vector getControlPointinRange(Point m, double variation) {
		Iterator<VEdge> n = vEdges.iterator();
		while (n.hasNext()) {
			VEdge temp = n.next(); // naechste Kante
			switch (temp.getType()) {
				case VEdge.LOOP :
				case VEdge.QUADCURVE : // Wenns eine Bezierkante ist
				{
					Point p = temp.getControlPoints().firstElement();
					if (p.distance(m) <= variation) {
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
						if (p.get(i).distance(m) <= variation) {
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
	 * Add a new SubSet to the VGraph
	 * The Index of the msubset is ignored (if differs from VSubSet-Index)
	 * 
	 * The Mathematical SubSet may be used to introduce the new subset with already given stuff
	 * So if a node in this VGraph exists, that is marked as belonging to the
	 * MSubSet it gets by adding the new Color added, too
	 * 
	 * If a SubSet with same index as VSUbSet exists or one of the arguments in null
	 * nothing happens
	 * 
	 * @paran subset new VSubSet to be added here
	 * @param msubset new MSubSet to be added in MGraph underneath and used for initialization of SubSet
	 */
	public void addSubSet(VSubSet subset, MSubSet msubset)
	{
		if ((subset==null)||(msubset==null)) //Oneof them Null
				return;
		if (getSubSet(subset.getIndex())!=null) //SubSet exists?
			return;
		//Create an empty temporary SubSet for Math
		MSubSet temp = new MSubSet(subset.getIndex(),msubset.getName());		
		mG.addSubSet(temp);
		vSubSets.add(subset.clone());
		Iterator<VNode> nit = vNodes.iterator();
		while (nit.hasNext())
		{
			int nindex = nit.next().getIndex();
			if (msubset.containsNode(nindex))
				addNodetoSubSet(nindex,subset.getIndex());
		}
		Iterator<VEdge> eit = vEdges.iterator();
		while (eit.hasNext())
		{
			int eindex = eit.next().getIndex();
			if (msubset.containsEdge(eindex))
				addEdgetoSubSet(eindex,subset.getIndex());
		}
		setChanged();
		notifyObservers(new GraphMessage(GraphMessage.SUBSET,subset.getIndex(),GraphMessage.ADDITION,GraphMessage.ALL_ELEMENTS));	
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
			if (mG.SubSetcontainsNode(actual.getIndex(), SetIndex))
				removeNodefromSubSet_(actual.getIndex(), SetIndex);
		}
		Iterator<VEdge> iterEdge = vEdges.iterator();
		while (iterEdge.hasNext()) {
			VEdge actual = iterEdge.next();
			if (mG.SubSetcontainsEdge(actual.getIndex(), SetIndex))
				removeEdgefromSubSet_(actual.getIndex(), SetIndex);
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
	 * add an Edge to a set
	 * 
	 * @param edgeindex
	 * 			edgeindex
	 * @param SetIndex
	 * 			setindex
	 * @see MGraph.addEdgetoSet(edgeindex,setindex)
	 */
	public void addEdgetoSubSet(int edgeindex, int SetIndex) {
		if ((mG.getEdge(edgeindex) != null)
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
	{	if (mG.SubSetcontainsEdge(edgeindex, SetIndex)) 
		{
			// Mathematisch hinzufuegen
			mG.removeEdgefromSet(edgeindex, SetIndex);
			// Und Knotenfarbe updaten
			Iterator<VSubSet> iter = vSubSets.iterator();
			while (iter.hasNext()) 
			{
				VSubSet actual = iter.next();
				if (actual.getIndex() == SetIndex) 
					getEdge(edgeindex).removeColor(actual.getColor());
			}
		}
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
			if (mG.SubSetcontainsNode(n.getIndex(),SetIndex))
			{
				n.removeColor(actual.getColor()); n.addColor(newcolour);
			}
		}
		Iterator<VEdge> edgeiter = vEdges.iterator();
		while (edgeiter.hasNext())
		{
			VEdge n = edgeiter.next();
			if (mG.SubSetcontainsEdge(n.getIndex(),SetIndex))
			{
				n.removeColor(actual.getColor()); n.addColor(newcolour);
			}
		}
		actual.setColor(newcolour);
		return true;
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