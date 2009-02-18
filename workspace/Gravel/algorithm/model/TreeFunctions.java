package algorithm.model;

import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.TreeMap;

import model.*;

public class TreeFunctions implements Observer {

	private TreeMap<Integer,Integer> degree, indegree, outdegree, order;
	private int nextorderindex = 0, childMinCount=Integer.MAX_VALUE, childMaxCount=Integer.MIN_VALUE;
	private MNode startnode;
	private MGraph mg;
	/**
	 * Constructor that needs a MathGraph to which all the algorithms refer
	 */
	public TreeFunctions(MGraph pmg)
	{
		setGraph(pmg);
	}
	public void setGraph(MGraph pmg)
	{
		mg = pmg;
		if (mg!=null)
		{	
			calculateDegrees();
			mg.addObserver(this);
		}		
	}
	public int getDegree(int nodeindex)
	{
		if (degree.get(nodeindex)==null)
			return -1;
		else 
			return degree.get(nodeindex);		
	}
	public int getInDegree(int nodeindex)
	{
		if (indegree.get(nodeindex)==null)
			return -1;
		else 
			return indegree.get(nodeindex);		
	}
	public int getOutDegree(int nodeindex)
	{
		if (outdegree.get(nodeindex)==null)
			return -1;
		else 
			return outdegree.get(nodeindex);		
	}
	/** 
	 * This Method tests, whether a graph is a tree or not.
	 * This is calculated by trying to give the nodes an order, the startnode, chosen arbitrary.
	 * each adjacent node must not be ordered yet. 
	 *
	 * @param vg a Graph
	 * @return true, if the graph is a tree, 
	 */
	public boolean isTree()
	{	
		if ((mg==null)||(mg.NodeCount()==0))
			return false; //An Empty Graph is no Tree
		//This maps a number (order) to every node index
		order = new TreeMap<Integer,Integer>();
		//Take any node and begin the recursive check
		nextorderindex = 1;
		if (mg.isDirected()) //directed Graph
		{
			calculateDegrees();
			//In the directed Case there are 2 possibilities for the root node
			//(a) there is exactely one node that has indeg 0, thats the root and the edges point "down the tree"
			//(b) there is exactely one node that has outdeg 0, thats the root and the edges point "up the tree"
			int acount=0, bcount=0, aindex=0, bindex=0;
			Iterator<MNode> nodeiter = mg.getNodeIterator();
			while (nodeiter.hasNext())
			{
				int actualindex = nodeiter.next().index;
				int indeg = indegree.get(actualindex); 
				int outdeg = outdegree.get(actualindex);
				if (indeg==0)
				{
					acount++;
					aindex=actualindex;
				}
				if (outdeg==0)
				{
					bcount++;
					bindex=actualindex;
				}
			}
			if (acount==1) //Case (a)
			{
				order.put(aindex,nextorderindex++);
				startnode = new MNode(aindex, mg.getNode(aindex).name);
				if (!isDirectedTreeRecursive(false,startnode))
						return false; //No Tree
			}
			else if (bcount==1)
			{
				order.put(bindex,nextorderindex++);
				startnode = new MNode(aindex, mg.getNode(aindex).name);
				if (!isDirectedTreeRecursive(true,startnode))
					return false; //No Tree
			}
			else
				return false; //no existent root
		}
		else
		{
			startnode = mg.getNodeIterator().next(); //Choose another one than arbitrary the first in the set ?
			order.put(startnode.index, nextorderindex++); //Initiate this one with order 1
			if (!isUndirectedTreeRecursive(startnode,null))
				return false; //Denn dann existiert ein Kreis			
		}
		// Are all nodes in the tree ?
		Iterator<MNode> iter = mg.getNodeIterator();
		while(iter.hasNext())
		{
			if (order.get(iter.next().index)==null)
				return false;
		}
		return true;
	}
	
	/**
	 * Tests whether the Graph is a Tree and each node is either a leave or has exactely b childs
	 * 
	 * @param b number of childs e.g. 2 for a binary tree
	 * @param mg
	 * @return
	 */
	public boolean isBTree(int b)
	{	
		if ((mg==null)||(mg.NodeCount()==0))
			return false; //An Empty Graph is no Tree
		//This maps a number (order) to every node index
		order = new TreeMap<Integer,Integer>();
		calculateDegrees();
		childMinCount=Integer.MAX_VALUE;
		childMaxCount=Integer.MIN_VALUE;
		//Take any node and begin the recursive check
		nextorderindex = 1;
		if (mg.isDirected()) //directed Graph
		{
			//In the directed Case there are 2 possibilities for the root node
			//(a) there is exactely one node that has indeg 0, thats the root and the edges point "down the tree"
			//(b) there is exactely one node that has outdeg 0, thats the root and the edges point "up the tree"
			int acount=0, bcount=0, aindex=0, bindex=0;
			Iterator<MNode> nodeiter = mg.getNodeIterator();
			while (nodeiter.hasNext())
			{
				int actualindex = nodeiter.next().index;
				int indeg = indegree.get(actualindex); 
				int outdeg = outdegree.get(actualindex);
				if (indeg==0) //This is a node with a possibility for the root of a downward tree
				{
					acount++;
					aindex=actualindex;
				}
				if (outdeg==0) //This is a node with a possibility for the root of an upward tree
				{
					bcount++;
					bindex=actualindex;
				}
			}
			if (acount==1) //Case (a)
			{
				order.put(aindex,nextorderindex++);
				startnode =  new MNode(aindex, mg.getNode(aindex).name);
				if (!isDirectedTreeRecursive(false,startnode))
						return false; //No Tree
			}
			else if (bcount==1)
			{
				order.put(bindex,nextorderindex++);
				startnode =  new MNode(aindex, mg.getNode(aindex).name);
				if (!isDirectedTreeRecursive(true,startnode))
					return false; //No Tree
			}
			else
				return false; //no existent root
		}
		else //Non_directed Case
		{
			startnode = mg.getNodeIterator().next(); //Choose another one than arbitrary the first in the set ?
			if (b!=0) //Chose one with b childs if existent
			{
				Iterator<MNode> iter = mg.getNodeIterator();
				while (iter.hasNext())
				{
					MNode check = iter.next();
					if (degree.get(check.index)==b)
						startnode = check;
				}
			}
			if (degree.get(startnode.index)!=b) //The Startnode has no parent so the degree must be b
			{ //No node with b children exists. So theres no root for a b.tree
				return false;
			}
			order.put(startnode.index, nextorderindex++); //Initiate this one with order 1
			if (!isUndirectedTreeRecursive(startnode,null))
				return false; //Denn dann existiert ein Kreis			
		}
		//In Both Cases (dir/undir) : Are all nodes in the tree ?
		Iterator<MNode> iter = mg.getNodeIterator();
		while(iter.hasNext())
		{
			if (order.get(iter.next().index)==null)
				return false;
		}
		//Are there exactely b children in each internal (non-leaf) node ? <= b ?
		if ((childMinCount!=b)||(childMaxCount!=b))
		{ //No
			return false;
		}
		return true;
	}
	
	public MNode getlastStartnode()
	{
		return startnode;
	}
	
	/**
	 * recursive Ordering with a DFS in a directed.
	 * the parent node is the only node that is adjacent with actual and no child
	 * 
	 * @param direction true means the edges point to the root (up), false, they point to the leaves (down)
	 * @param actual actual node
	 * @param mg
	 * @return true, if the actual node is root of a subtree of mg
	 */
	private boolean isDirectedTreeRecursive(boolean direction, MNode actual)
	{
		if ((direction)&&(indegree.get(actual.index)==0)) //Up pointing and actual has no incoming edges 
			return true; //And a leave is a subtree
		if ((direction)&&(indegree.get(actual.index)==0)) //Up pointing and actual has no incoming edges 
			return true;
		Iterator<MNode> nodeiter = mg.getNodeIterator();
		//Get all adjacent nodes with
		int childcount=0;
		while (nodeiter.hasNext())
		{
			MNode next = nodeiter.next();
			if ((direction)&&(mg.EdgesBetween(next.index,actual.index)>0)) //UP, then there must be an edge next->actual
			{	//Upward Child
				if (order.get(next.index)!=null) //Circle
					return false;
				else
					order.put(next.index, nextorderindex++);
				if (!isDirectedTreeRecursive(direction, next)) //Child no subtree
					return false;
				//still searching, and this child is valid 
				childcount++;
			}
			if ((!direction)&&(mg.EdgesBetween(actual.index,next.index)>0)) //DOWN, then there must be an edge actual->next
			{	//Downward Child
				if (order.get(next.index)!=null) //Circle
					return false;
				else
					order.put(next.index, nextorderindex++);
				if (!isDirectedTreeRecursive(direction, next)) //Child no subtree
					return false;
				//still searching, and this child is valid 
				childcount++;
			}
		}
		if (childcount!=0) //at least one child
		{
			if (childcount<childMinCount) childMinCount = childcount;
			if (childcount>childMaxCount) childMaxCount = childcount;			
		}
		//every child is a valid subtree, so we are
		return true;
	}
	/**
	 * recursive Ordering with a DFS in an undirected graph.
	 * the parent node is the only node that is adjacent with actual and no child
	 * 
	 * @param actual actual node
	 * @param parent parent of
	 * @param mg
	 * @return true, if the actual node is root of a subtree, else false
	 */
	private boolean isUndirectedTreeRecursive(MNode actual, MNode parent)
	{
		//node already has an order. So check every adjacent node despite the parent
		Iterator<MNode> nodeiter = mg.getNodeIterator();
		int childcount = 0;
		while (nodeiter.hasNext()) //give every child an order index
		{
			MNode next = nodeiter.next();
			if ((next!=parent)&&(mg.EdgesBetween(actual.index,next.index)>0)) //Child
			{
				if (order.get(next.index)!=null) //Child has an order already
					return false;
				else
					order.put(next.index, nextorderindex++);
				if (!isUndirectedTreeRecursive(next, actual)) //Child no subtree
					return false;
				//correct child and still woking
				childcount++;
			}
		}
		if (childcount!=0) //at least one child, so it's no leave
		{
			if (childcount<childMinCount) childMinCount = childcount;
			if (childcount>childMaxCount) childMaxCount = childcount;			
		}
		return true; //Every Child is a Subtree
	}
	
	
	/**Calculates the Degree for a graph
	 * and if the graph is directed in and outdegree
	 * 
	 * @param vg
	 */
	private void calculateDegrees()
	{
		if (mg==null)
			return;
		degree = new TreeMap<Integer,Integer>();
		indegree = new TreeMap<Integer,Integer>();
		outdegree = new TreeMap<Integer,Integer>();
		Iterator<MEdge> edgeiter = mg.getEdgeIterator();
		while (edgeiter.hasNext())
		{
			MEdge actual = edgeiter.next();
			int start = actual.StartIndex, ende = actual.EndIndex;
			if (degree.get(start)==null)
				degree.put(start,0);
			if (degree.get(ende)==null)
				degree.put(ende,0);
			if (indegree.get(ende)==null)
				indegree.put(ende,0);
			if (outdegree.get(start)==null)
				outdegree.put(start,0);
			degree.put(start,degree.get(start)+1);
			degree.put(ende,degree.get(ende)+1);
			outdegree.put(start,outdegree.get(start)+1);
			indegree.put(ende,indegree.get(ende)+1);
		} //End While edgeiter.hasnext
		Iterator<MNode> nodeiter = mg.getNodeIterator();
		while (nodeiter.hasNext())
		{
			MNode n = nodeiter.next();
			if (degree.get(n.index)==null)
				degree.put(n.index,0);
			if (indegree.get(n.index)==null)
				indegree.put(n.index,0);
			if (outdegree.get(n.index)==null)
				outdegree.put(n.index,0);
		}
	}
	//Falls sich der MGraph ändert
	public void update(Observable arg0, Object arg1) 
	{
			calculateDegrees();
	}
}
