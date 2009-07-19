package model;

import io.GeneralPreferences;

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
import java.util.Vector;

import model.VEdge;
import model.VNode;
import model.Messages.GraphConstraints;
import model.Messages.GraphMessage;
/**
 * VGraph encapsulates an MGraph and keeps visual information about every node, edge and subgraphs in the MGraph
 * each manipulation on the VGraph is also given to the MGraph
 * The MGraph may the extracted and being used to generate another VGraph to the same MGraph
 *
 * The VGraph is observable, so that every Observer may update itself, if something changes here
 *
 * the update may give Information about the updated parts by the GraphMessage
 *
 * @author Ronny Bergmann 
 */
public class VGraph extends Observable implements VGraphInterface {

	MGraph mG;
	public VNodeSet modifyNodes;
	public VEdgeSet modifyEdges;
	public VSubgraphSet modifySubgraphs;
	/**
	 * Constructor
	 * 
	 * @param d indicates whether the graph is directed or not
	 * @param l indicates whether the graph might have loops or not
	 * @param m indicates whether the graph might have multiple edges between two nodes or not
	 */	
	public VGraph(boolean d, boolean l, boolean m)
	{
		mG = new MGraph(d,l,m);
		modifyNodes = new VNodeSet(mG);
		modifyEdges = new VEdgeSet(mG);
		modifySubgraphs = new VSubgraphSet(mG);
		modifySubgraphs.addObserver(modifyNodes); //Nodes must react on SubgraphChanges (Color)
		modifySubgraphs.addObserver(modifyEdges); //Edges must react on SubgraphChanges (Color)
		//Subgraph has not to react on anything, because the Membership is kept im mG and not in VSubgraphSet
		modifyNodes.addObserver(this);
		modifyEdges.addObserver(this);
		modifySubgraphs.addObserver(this);
	}
	/* (non-Javadoc)
	 * @see model.VGraphInterface#deselect()
	 */
	public void deselect() {
		boolean hasSel = hasSelection();
		modifyNodes.deselect();
		modifyEdges.deselect();
		setChanged();
		notifyObservers(new GraphMessage(GraphConstraints.SELECTION,GraphConstraints.UPDATE));
		if (hasSel)
		{ //Only if we really deselected -> notify
			setChanged();
			notifyObservers(new GraphMessage(GraphConstraints.SELECTION,GraphConstraints.UPDATE));
		}
	}
	/* (non-Javadoc)
	 * @see model.VGraphInterface#removeSelection()
	 */
	public void removeSelection()
	{
		setChanged();
		notifyObservers(
			new GraphMessage(GraphConstraints.SELECTION|GraphConstraints.NODE|GraphConstraints.EDGE, //Changed
							GraphConstraints.REMOVAL|GraphConstraints.BLOCK_START, //Status 
							GraphConstraints.NODE|GraphConstraints.EDGE|GraphConstraints.SELECTION) //Affected		
			);
		Iterator<VNode> n = modifyNodes.getIterator();
		HashSet<VNode> selected = new HashSet<VNode>();
		while (n.hasNext()) {
			VNode node = n.next();
			if ((node.getSelectedStatus()&VItem.SELECTED)==VItem.SELECTED)
				selected.add(node);
		}
		n = selected.iterator();
		while (n.hasNext())
		{
			modifyNodes.remove(n.next().getIndex());
		}
		Iterator<VEdge> n2 = modifyEdges.getIterator();
		HashSet<VEdge> selected2 = new HashSet<VEdge>();
		while (n2.hasNext()) {
			VEdge edge = n2.next();
			if ((edge.getSelectedStatus() & VItem.SELECTED) == VItem.SELECTED)
				selected2.add(edge);
		}
		n2 = selected2.iterator();
		while (n2.hasNext())
		{
			modifyEdges.remove(n2.next().getIndex());
		}
		setChanged();
		notifyObservers(
			new GraphMessage(GraphConstraints.SELECTION|GraphConstraints.NODE|GraphConstraints.EDGE, //Changed
							GraphConstraints.REMOVAL|GraphConstraints.BLOCK_END, //Status 
							GraphConstraints.NODE|GraphConstraints.EDGE|GraphConstraints.SELECTION) //Affected		
		);		
	}
	public boolean hasSelection()
	{
		return modifyNodes.hasSelection()||modifyEdges.hasSelection();
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
			return removed;
		if (!d) //also falls auf ungerichtet umgestellt wird
		{
			setChanged();
			notifyObservers(new GraphMessage(GraphConstraints.EDGE|GraphConstraints.DIRECTION,GraphConstraints.UPDATE|GraphConstraints.BLOCK_START, GraphConstraints.EDGE));
			if (!mG.isMultipleAllowed()) //Ist auch nur ein Problem, wenn keine Mehrfachkanten erlaubt sind
			{
					Iterator<VNode> n = modifyNodes.getIterator();
					while (n.hasNext())
					{
						VNode t = n.next();
						Iterator<VNode> n2 = modifyNodes.getIterator();			//jeweils
						while (n2.hasNext())
						{
							VNode t2 = n2.next();
							if (t.getIndex() < t2.getIndex())
							{
								Vector<Integer> ttot2 = mG.modifyEdges.indicesBetween(t.getIndex(), t2.getIndex());
								Vector<Integer> t2tot = mG.modifyEdges.indicesBetween(t2.getIndex(), t.getIndex());
								//In the nonmultiple case each Vector has exactely one or no edge in it
								if ((!ttot2.isEmpty())&&(!t2tot.isEmpty()))
								{
									int e1 = ttot2.firstElement();
									int e2 = t2tot.firstElement();
									MEdge m = mG.modifyEdges.get(e2);
									m.Value = mG.modifyEdges.get(e2).Value+mG.modifyEdges.get(e1).Value;
									modifyEdges.remove(e1);
									removed.set(e1);
								}
							} //End no duplicate
						}
					}
					if (mG.setDirected(d).cardinality() > 0)
						System.err.println("DEBUG ; Beim gerichtet Setzen läuft was falsch");
			} //end of if !allowedmultiple
			else //multiple allowed - the other way around
			{
				if (mG.setDirected(d).cardinality() > 0)
				{
					System.err.println("DEBUG ; Beim gerichtet Setzen läuft was falsch");
				} 				
			//	modifyEdges.getEdgeIterator(); //find similar Edges
			//	try
			//	{		
					HashSet<VEdge> toDelete = new HashSet<VEdge>(); // zu entfernende Kanten
					Iterator<VEdge> e = modifyEdges.getIterator();				
					while (e.hasNext())
					{
						VEdge t = e.next();
						int ts = mG.modifyEdges.get(t.getIndex()).StartIndex;
						int te = mG.modifyEdges.get(t.getIndex()).StartIndex;
						Vector<Integer> indices = mG.modifyEdges.indicesBetween(ts, te);
						Iterator<Integer> iiter = indices.iterator();
						while (iiter.hasNext())
						{
							VEdge act = modifyEdges.get(iiter.next());
							if ((mG.modifyEdges.get(act.getIndex()).StartIndex==te)&&(!mG.isDirected())&&(act.getEdgeType()==VEdge.ORTHOGONAL)&&(t.getEdgeType()==VEdge.ORTHOGONAL)) 
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
						modifyEdges.remove_(e3.next().getIndex());
					if (mG.setDirected(d).cardinality() > 0)
						System.err.println("DEBUG ; Beim gerichtet Setzen läuft was falsch");
			} //end of deleting similar edges in multiple directed graphs
			setChanged();
			notifyObservers(new GraphMessage(GraphConstraints.EDGE|GraphConstraints.DIRECTION,GraphConstraints.UPDATE|GraphConstraints.BLOCK_END, GraphConstraints.EDGE));
		}//end if !d
		else //undirected
		{
			mG.setDirected(d); //change
		//im MGraph auch noch
		setChanged();
		notifyObservers(new GraphMessage(GraphConstraints.EDGE|GraphConstraints.DIRECTION,GraphConstraints.UPDATE, GraphConstraints.EDGE));
		}
		return removed;
	}
	/* (non-Javadoc)
	 * @see model.VGraphInterface#translate(int, int)
	 */
	public void translate(int x, int y)
	{
		Iterator<VNode> iter1 = modifyNodes.getIterator();
		while (iter1.hasNext())
		{
			iter1.next().translate(x, y);
		}
		Iterator<VEdge> iter2 = modifyEdges.getIterator();
		while(iter2.hasNext())
		{
			iter2.next().translate(x,y);
		}
		setChanged();
		notifyObservers(
				new GraphMessage(GraphConstraints.NODE|GraphConstraints.EDGE, //Type
								GraphConstraints.TRANSLATION, //Status 
								GraphConstraints.NODE|GraphConstraints.EDGE|GraphConstraints.SELECTION|GraphConstraints.SUBGRAPH) //Affected		
			);
	}
	/* (non-Javadoc)
	 * @see model.VGraphInterface#getSingleSelectedItem()
	 */
	public VItem getSingleSelectedItem()
	{
		VNode SelNode = modifyNodes.getSingleSelectedNode();
		VEdge SelEdge = modifyEdges.getSingleSelectedEdge();
		if (SelNode==null) //Then it is only an edge if that is not null
		{
			if ((SelEdge!=null)&&(SelEdge.getIndex()!=-1))
				return SelEdge;
			else
				return null;
		}
		if (SelEdge==null)
		{
			if ((SelNode!=null)&&(SelNode.getIndex()!=-1))
				return SelNode;
			else
				return null;
		}
		return null;
	}

	/**
	 * Get the Mathematical Graph this graph depends on. If you change stuff in there this Graph changes too
	 * @return
	 */
	public MGraph getMathGraph() {
		return mG;
	}
	/**
	 * Replace this VGraph with another one.
	 * @param anotherone
	 */
	public void replace(VGraph anotherone)
	{
		//Del Me
		modifyNodes.deleteObserver(this); modifyEdges.deleteObserver(this); modifySubgraphs.deleteObserver(this);
		//But keep themselfs observed by each other
		//Replacement
		modifyNodes =anotherone.modifyNodes;
		modifyEdges = anotherone.modifyEdges;
		modifySubgraphs = anotherone.modifySubgraphs;

		//Renew actual stuff
		modifyNodes.addObserver(modifyEdges); //Edges must react on NodeDeletions
		modifySubgraphs.addObserver(modifyNodes); //Nodes must react on SubgraphChanges
		modifySubgraphs.addObserver(modifyEdges); //Edges must react on SubgraphChanges
		modifyNodes.addObserver(this);
		modifyEdges.addObserver(this);
		modifySubgraphs.addObserver(this);

		mG = anotherone.mG;
		mG.pushNotify(
						new GraphMessage(GraphConstraints.ELEMENT_MASK, //Type
										GraphConstraints.REPLACEMENT, //Status 
										GraphConstraints.ELEMENT_MASK) //Affected		
		);
		setChanged();
		notifyObservers(
				new GraphMessage(GraphConstraints.ELEMENT_MASK, //Type
						GraphConstraints.REPLACEMENT, //Status 
						GraphConstraints.ELEMENT_MASK) //Affected		
		);
	}
	/**
	 * Get a clone of this graph
	 * @return a Copy
	 */
	public VGraph clone()
	{
		VGraph clone = new VGraph(mG.isDirected(),mG.isLoopAllowed(),mG.isMultipleAllowed());
		//Untergraphen
		Iterator<VSubgraph> n1 = modifySubgraphs.getIterator();
		while (n1.hasNext())
		{
			VSubgraph actualSet = n1.next().clone();
			//Add it as an empty SubSet
			clone.modifySubgraphs.add(actualSet, new MSubgraph(actualSet.index, mG.modifySubgraphs.get(actualSet.index).getName())); //Jedes Set kopieren
		}
		//Knoten
		Iterator<VNode> n2 = modifyNodes.getIterator();
		while (n2.hasNext())
		{
			VNode nodeclone = n2.next().clone();
			clone.modifyNodes.add(nodeclone, mG.modifyNodes.get(nodeclone.getIndex()));
			//In alle Sets einfuegen
			n1 = modifySubgraphs.getIterator();
			while (n1.hasNext())
			{
				VSubgraph actualSet = n1.next();
				if (mG.modifySubgraphs.get(actualSet.index).containsNode(nodeclone.getIndex()))
					clone.modifySubgraphs.addNodetoSubgraph(nodeclone.getIndex(), actualSet.index); //In jedes Set setzen wo er war
			}
		}
		//Analog Kanten
		Iterator<VEdge> n3 = modifyEdges.getIterator();
		while (n3.hasNext())
		{
			VEdge cloneEdge = n3.next().clone();
			MEdge me = mG.modifyEdges.get(cloneEdge.getIndex());
			clone.modifyEdges.add(cloneEdge, me, modifyNodes.get(me.StartIndex).getPosition(), modifyNodes.get(me.EndIndex).getPosition());
			//In alle Sets einfuegen
			n1 = modifySubgraphs.getIterator();
			while (n1.hasNext())
			{
				VSubgraph actualSet = n1.next();
				if (mG.modifySubgraphs.get(actualSet.index).containsEdge(cloneEdge.getIndex()))
					clone.modifySubgraphs.addEdgetoSubgraph(cloneEdge.getIndex(), actualSet.getIndex()); //Jedes Set kopieren
			}
		}
		//und zurückgeben
		return clone;
	}
	/* (non-Javadoc)
	 * @see model.VGraphInterface#getMaxPoint(java.awt.Graphics)
	 */
	public Point getMaxPoint(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g;
		Point maximum = new Point(0,0);
		Iterator<VNode> iter1 = modifyNodes.getIterator();
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
				int adv = metrics.stringWidth(mG.modifyNodes.get(actual.getIndex()).name);
				x += new Double(Math.floor((double)adv/2.0d)).intValue(); y += new Double(Math.floor((double)hgt/2.0d)).intValue(); //Bottom Right Corner
				if (x > maximum.x)
					maximum.x = x;
				if (y > maximum.y)
					maximum.y = y;
			}
		}
		Iterator<VEdge> iter2 = modifyEdges.getIterator();
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
	/* (non-Javadoc)
	 * @see model.VGraphInterface#getMinPoint(java.awt.Graphics)
	 */
	public Point getMinPoint(Graphics g)
	{
		Graphics2D g2 = (Graphics2D)g; 
		Point minimum = new Point(Integer.MAX_VALUE,Integer.MAX_VALUE);
		Iterator<VNode> iter1 = modifyNodes.getIterator();
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
				int adv = metrics.stringWidth(mG.modifyNodes.get(actual.getIndex()).name);
				x -= new Double(Math.floor((double)adv/2.0d)).intValue(); y -= new Double(Math.floor((double)hgt/2.0d)).intValue(); //Top Left Corner
				if (x < minimum.x)
					minimum.x = x;
				if (y < minimum.y)
					minimum.y = y;
			}
		}
		Iterator<VEdge> iter2 = modifyEdges.getIterator();
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
	 * get the edge in Range of a given point.
	 * an edge is in Range, if the distance from the edge-line or line segments to the point p is smaller than the edge width
	 * <br><br>
	 * <i>not very exact at the moment</i>
	 * 
	 * @param p a point
	 * @param variation the variation m may be away from the edge
	 * @return the first edge in range, if there is one, else null
	 */
	public VEdge getEdgeinRangeOf(Point m, double variation) {
		Iterator<VEdge> n = modifyEdges.getIterator();
		while (n.hasNext()) {
			VEdge temp = n.next();
			// naechste Kante
			MEdge me = mG.modifyEdges.get(temp.getIndex());
			Point p1 = (Point)modifyNodes.get(me.StartIndex).getPosition().clone();
			Point p2 = (Point)modifyNodes.get(me.EndIndex).getPosition().clone();
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
				    	if (distanceSquare < (variation+(double)temp.width)) 
				    	{
					    		return temp;
					    }		    			
		    		}
		    	}
		    	lastx = x; lasty = y;
		    	path.next();
		    }
		    //if the shortest distance is smaller than  
		    if (closestDistanceSquare < (variation+(double)temp.width))
		    	return temp;
		}
		return null; // keinen gefunden
	}
	/**
	 * add edges from evey selected node to a given node
	 * @param Ende
	 * 				the target of all new edges
	 */
	public void addEdgesfromSelectedNodes(VNode Ende) {
		pushNotify(new GraphMessage(GraphConstraints.EDGE,GraphConstraints.ADDITION|GraphConstraints.BLOCK_START));
		Iterator<VNode> iter = modifyNodes.getIterator();
		while (iter.hasNext()) 
		{
				VNode temp = iter.next();
				if (((temp.getSelectedStatus()&VItem.SELECTED)==VItem.SELECTED) && (temp != Ende)) 
				{
					int i = mG.modifyEdges.getNextIndex();
					//Standard ist eine StraightLineEdge
					MEdge me;
					if (Ende.getIndex()==0)
						me = new MEdge(i,temp.getIndex(),Ende.getIndex(),GeneralPreferences.getInstance().getIntValue("edge.value"),"\u22C6");
					else
						me = new MEdge(i,temp.getIndex(),Ende.getIndex(),GeneralPreferences.getInstance().getIntValue("edge.value"),GeneralPreferences.getInstance().getEdgeName(i, temp.getIndex(), Ende.getIndex()));					
						modifyEdges.add(new VStraightLineEdge(i,GeneralPreferences.getInstance().getIntValue("edge.width")), me,modifyNodes.get(temp.getIndex()).getPosition(), Ende.getPosition());
				}
		}
		pushNotify(new GraphMessage(GraphConstraints.EDGE,GraphConstraints.BLOCK_END));
	}
	/**
	 * add edges from a given node to evey selected node
	 * @param Start 
	 * 				the source of all new edges
	 */
	public void addEdgestoSelectedNodes(VNode Start) {
		pushNotify(new GraphMessage(GraphConstraints.EDGE,GraphConstraints.ADDITION|GraphConstraints.BLOCK_START));
		Iterator<VNode> iter = modifyNodes.getIterator();
		while (iter.hasNext()) 
		{
				VNode temp = iter.next();
				if (((temp.getSelectedStatus() & VItem.SELECTED) == VItem.SELECTED) && (temp != Start)) 
				{
					int i = mG.modifyEdges.getNextIndex();
					//Standard ist eine StraightLineEdge
					MEdge me;
					if (Start.getIndex()==0)
						me = new MEdge(i,Start.getIndex(),temp.getIndex(),GeneralPreferences.getInstance().getIntValue("edge.value"),"\u22C6");
					else
						me = new MEdge(i,Start.getIndex(),temp.getIndex(),GeneralPreferences.getInstance().getIntValue("edge.value"),GeneralPreferences.getInstance().getEdgeName(i, Start.getIndex(), temp.getIndex()));
					
						modifyEdges.add(new VStraightLineEdge(i,GeneralPreferences.getInstance().getIntValue("edge.width")), me,	Start.getPosition(), temp.getPosition());
				}
		}
		pushNotify(new GraphMessage(GraphConstraints.EDGE,GraphConstraints.BLOCK_END));
	}
	/* (non-Javadoc)
	 * @see model.VGraphInterface#pushNotify(java.lang.Object)
	 */
	public void pushNotify(Object o) {
		setChanged();
		if (o == null)
			notifyObservers();
		else
			notifyObservers(o);
	}
	/* (non-Javadoc)
	 * @see model.VGraphInterface#getType()
	 */
	public int getType() {
		return VGraphInterface.GRAPH;
	}
	public void update(Observable o, Object arg) 
	{
		if (arg instanceof GraphMessage) //Not nice but haven't found a beautiful way after hours
		{
			GraphMessage m = (GraphMessage)arg;
			if (m!=null) //send all GraphChange-Messages to Observers of the VGraph
			{
				setChanged();
				notifyObservers(m);
			}
		}
	}
}