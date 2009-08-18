package model;

import io.GeneralPreferences;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Point2D;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Observable;

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
public class VHyperGraph extends Observable implements VGraphInterface {

	MHyperGraph mG;
	public VNodeSet modifyNodes;
	public VHyperEdgeSet modifyHyperEdges;
	public VSubgraphSet modifySubgraphs;
	/**
	 * Constructor
	 * 
	 */	
	public VHyperGraph()
	{
		mG = new MHyperGraph();
		modifyNodes = new VNodeSet(mG);
		modifyHyperEdges = new VHyperEdgeSet(mG);
		modifySubgraphs = new VSubgraphSet(mG);
		modifySubgraphs.addObserver(modifyNodes); //Nodes must react on SubgraphChanges (Color)
		modifySubgraphs.addObserver(modifyHyperEdges); //Edges must react on SubgraphChanges (Color)
		//Subgraph has not to react on anything, because the Membership is kept im mG and not in VSubgraphSet
		modifyNodes.addObserver(this);
		modifyHyperEdges.addObserver(this);
		modifySubgraphs.addObserver(this);
	}
	/* (non-Javadoc)
	 * @see model.VGraphInterface#deselect()
	 */
	public void deselect() {
		boolean hasSel = hasSelection(); 
		modifyNodes.deselect();
		modifyHyperEdges.deselect();
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
			new GraphMessage(GraphConstraints.SELECTION|GraphConstraints.NODE|GraphConstraints.HYPEREDGE, //Changed
							GraphConstraints.REMOVAL|GraphConstraints.HYPEREDGESHAPE|GraphConstraints.BLOCK_START, //Status 
							GraphConstraints.NODE|GraphConstraints.HYPEREDGE|GraphConstraints.SELECTION) //Affected		
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
		Iterator<VHyperEdge> n2 = modifyHyperEdges.getIterator();
		HashSet<VHyperEdge> selected2 = new HashSet<VHyperEdge>();
		while (n2.hasNext()) {
			VHyperEdge edge = n2.next();
			if ((edge.getSelectedStatus() & VItem.SELECTED) == VItem.SELECTED)
				selected2.add(edge);
		}
		n2 = selected2.iterator();
		while (n2.hasNext())
		{
			modifyHyperEdges.remove(n2.next().getIndex());
		}
		setChanged();
		notifyObservers(
			new GraphMessage(GraphConstraints.SELECTION|GraphConstraints.NODE|GraphConstraints.HYPEREDGE, //Changed
							GraphConstraints.REMOVAL|GraphConstraints.HYPEREDGESHAPE|GraphConstraints.BLOCK_END, //Status 
							GraphConstraints.NODE|GraphConstraints.HYPEREDGE|GraphConstraints.SELECTION) //Affected		
		);		
	}
	public boolean hasSelection()
	{
		return modifyNodes.hasSelection()||modifyHyperEdges.hasSelection();
	}
	/* (non-Javadoc)
	 * @see model.VGraphInterface#getSingleSelectedItem()
	 */
	public VItem getSingleSelectedItem()
	{
		VNode SelNode = modifyNodes.getSingleSelectedNode();
		VHyperEdge SelEdge = modifyHyperEdges.getSingleSelectedEdge();
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
		Iterator<VHyperEdge> iter2 = modifyHyperEdges.getIterator();
		while(iter2.hasNext())
		{
			iter2.next().translate(x,y);
		}
		setChanged();
		notifyObservers(
				new GraphMessage(GraphConstraints.NODE|GraphConstraints.HYPEREDGE, //Type
								GraphConstraints.TRANSLATION|GraphConstraints.HYPEREDGESHAPE, //Status 
								GraphConstraints.NODE|GraphConstraints.HYPEREDGE|GraphConstraints.SELECTION|GraphConstraints.SUBGRAPH) //Affected		
			);
	}
	/**
	 * Get the Mathematical Graph this graph depends on. If you change stuff in there this Graph changes too
	 * @return
	 */
	public MHyperGraph getMathGraph() {
		return mG;
	}
	/**
	 * Replace this VGraph with another one.
	 * @param anotherone
	 */
	public void replace(VHyperGraph anotherone)
	{
		//Del Me
		modifyNodes.deleteObserver(this); modifyHyperEdges.deleteObserver(this); modifySubgraphs.deleteObserver(this);
		//But keep themselfs observed by each other
		//Replacement
		modifyNodes =anotherone.modifyNodes;
		modifyHyperEdges = anotherone.modifyHyperEdges;
		modifySubgraphs = anotherone.modifySubgraphs;

		//Renew actual stuff
		modifyNodes.addObserver(modifyHyperEdges); //Edges must react on NodeDeletions
		modifySubgraphs.addObserver(modifyNodes); //Nodes must react on SubgraphChanges
		modifySubgraphs.addObserver(modifyHyperEdges); //Edges must react on SubgraphChanges
		modifyNodes.addObserver(this);
		modifyHyperEdges.addObserver(this);
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
	public VHyperGraph clone()
	{
		VHyperGraph clone = new VHyperGraph();
		//Knoten
		//Untergraphen
		Iterator<VSubgraph> n1 = modifySubgraphs.getIterator();
		while (n1.hasNext())
		{
			VSubgraph actualSet = n1.next().clone();
			//Add it as an empty SubSet
			clone.modifySubgraphs.add(actualSet, new MSubgraph(actualSet.index, mG.modifySubgraphs.get(actualSet.index).getName())); //Jedes Set kopieren
		}
		Iterator<VNode> n2 = modifyNodes.getIterator();
		while (n2.hasNext())
		{
			VNode nodeclone = n2.next().clone();
			clone.modifyNodes.add(nodeclone, mG.modifyNodes.get(nodeclone.getIndex()).clone());
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
		Iterator<VHyperEdge> n3 = modifyHyperEdges.getIterator();
		while (n3.hasNext())
		{
			VHyperEdge cloneEdge = n3.next().clone();
			MHyperEdge me = mG.modifyHyperEdges.get(cloneEdge.getIndex());
			clone.modifyHyperEdges.add(cloneEdge, me);
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
		Iterator<VHyperEdge> iter2 = modifyHyperEdges.getIterator();
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
		Iterator<VHyperEdge> iter2 = modifyHyperEdges.getIterator();
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
	 * @return the first edge in range, if there is one, else
	 */
	public VHyperEdge getEdgeinRangeOf(Point m, double variation) {
		Iterator<VHyperEdge> n = modifyHyperEdges.getIterator();
		while (n.hasNext()) {
			VHyperEdge temp = n.next();
			if (!temp.getShape().isEmpty())
			{
				NURBSShapeProjection projection = new NURBSShapeProjection(temp.getShape(),m);
				Point2D OnCurve = projection.getResultPoint();
			
				if (OnCurve.distance(m)<=(variation+(double)temp.getWidth()))
					return temp;
			}
		}
		return null; // keinen gefunden
	}
	/**
	 * add edges from evey selected node to a given node
	 * @param Ende
	 * 				the target of all new edges
	 */
	public void createHyperEdgefromfromSelectedNodes() {
		if (!modifyNodes.hasSelection())
			return;
		pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,GraphConstraints.ADDITION|GraphConstraints.BLOCK_START));
		Iterator<VNode> iter = modifyNodes.getIterator();
		int i = mG.modifyHyperEdges.getNextIndex();
		MHyperEdge me = new MHyperEdge(i,GeneralPreferences.getInstance().getIntValue("edge.value"),"HE"+i);
		modifyHyperEdges.add(new VHyperEdge(i,GeneralPreferences.getInstance().getIntValue("edge.width"),GeneralPreferences.getInstance().getIntValue("hyperedge.margin")), me);
		while (iter.hasNext()) 
		{
				VNode temp = iter.next();
				if ((temp.getSelectedStatus()&VItem.SELECTED)==VItem.SELECTED) 
				{
						mG.modifyHyperEdges.addNodeto(temp.getIndex(), me.index);
				}
		}
		pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE,GraphConstraints.BLOCK_END));
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
		return VGraphInterface.HYPERGRAPH;
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