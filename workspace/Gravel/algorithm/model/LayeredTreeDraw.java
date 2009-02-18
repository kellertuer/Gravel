package algorithm.model;

import io.GeneralPreferences;

import java.util.HashMap;
import java.util.Iterator;

import model.MEdge;
import model.MGraph;
import model.MNode;
import model.VEdge;
import model.VGraph;
import model.VNode;
import model.VStraightLineEdge;


public class LayeredTreeDraw implements VAlgorithmIF 
{
	private final int UPTREE = 1;
	private final int DOWNTREE = 2;
	private final int UNDIR = 3;
	//UrsprungsGraph
	MGraph mG;
	Iterator<MNode> nodeiter;
	//Parameter
	private int gridX,gridY;
	//Entstehender Graph
	private VGraph ErgebnisGraph;
	private GeneralPreferences gp;
	
	private TreeFunctions ta;
	public LayeredTreeDraw()
	{
		mG = new MGraph(false,false,false);
		ta = new TreeFunctions(mG);
		gp = GeneralPreferences.getInstance();
	}
	public boolean GraphOkay() 
	{
		if (mG==null)
			return false;
		ta = new TreeFunctions(mG);
		return ta.isBTree(2); //Binary Tree
	}

	public VGraph getactualState() 
	{
		return ErgebnisGraph;
	}

	public void run() 
	{
		if (!GraphOkay())
			return;
		MNode startnode = ta.getlastStartnode();
		if (mG.isDirected())
		{
			if (ta.getInDegree(startnode.index)==0) //Down
				ErgebnisGraph.replace(TreeRecursion(DOWNTREE,startnode,null));
			if (ta.getOutDegree(startnode.index)==0) //Up
				ErgebnisGraph.replace(TreeRecursion(UPTREE,startnode,null));
		}
		else
		{
			ErgebnisGraph.replace(TreeRecursion(UNDIR,startnode,null));
		}
	}
	private VGraph TreeRecursion(int type,MNode subtreeroot,MNode parent)
	{	
		if (ta.getDegree(subtreeroot.index)==1) //Blatt, in allen fällen ist nur dies ein PLatt, an 1x1 positionieren (je an gridx, gridy)
		{
			VGraph erg;
			if (type==UNDIR)
				erg = new VGraph(false,false,false);
			else
				erg = new VGraph(true,false,false);				
			erg.modifyNodes.addNode(new VNode(subtreeroot.index,gridX,gridY,gp.getIntValue("node.size"),gp.getIntValue("node.name_distance"),gp.getIntValue("node.name_rotation"),gp.getIntValue("node.name_size"),gp.getBoolValue("node.name_visible")), new MNode(subtreeroot.index,subtreeroot.name));
			return erg;
		}
		//Sonst beide Söhne herausfinden
		MNode left=null, right=null;
		Iterator<MNode> nodeiter = mG.getNodeIterator();
		while (nodeiter.hasNext())
		{
			MNode next = nodeiter.next();
			if (((type==UNDIR)||(type==DOWNTREE))&&((next!=parent)&&(mG.EdgesBetween(subtreeroot.index,next.index)>0))) //Sohn, ungerichteter fall oder downtree
			{
				if (left==null) 
					left = next;
				else 
					right = next;
			} //Und beide gibts, da der Graph okay war und subtreroot kein Blatt war
			if ((type==UPTREE)&&((next!=parent)&&(mG.EdgesBetween(next.index,subtreeroot.index)>0))) //Sohn, uptree
			{
				if (left==null) 
					left = next;
				else 
					right = next;
			} //Und beide gibts, da der Graph okay war und subtreroot kein Blatt war
		}
		//Rekursion und Copy überall identisch
		VGraph leftsubtree = TreeRecursion(type,left,subtreeroot);
		VGraph rightsubtree = TreeRecursion(type,right,subtreeroot);
		//Beide sind mit left und right auf höhe 1, verschiebe right nach rechts und beide auf höhe 2
		leftsubtree.translate(0,gridY);
		//No Enviroment, so no Node-Text-Infos are given
		rightsubtree.translate((1+(int)(leftsubtree.getMaxPoint(null).x/gridX))*gridX,gridY);
		//Da jeder stets den eignen Index verwendet (s.o.) iteriere über right füge die zum andren hinzu
		Iterator<VNode> copynodes = rightsubtree.modifyNodes.getNodeIterator();
		while (copynodes.hasNext())
		{
			VNode shift = copynodes.next();
			leftsubtree.modifyNodes.addNode(shift, new MNode(shift.getIndex(),rightsubtree.getMathGraph().getNode(shift.getIndex()).name));
		}	
		Iterator<VEdge> copyedges = rightsubtree.modifyEdges.getEdgeIterator();
		while (copyedges.hasNext())
		{
			VEdge shift = copyedges.next();
			MEdge shiftm = rightsubtree.getMathGraph().getEdge(shift.getIndex());
			leftsubtree.modifyEdges.addEdge(shift, shiftm,leftsubtree.modifyNodes.getNode(shiftm.StartIndex).getPosition(),leftsubtree.modifyNodes.getNode(shiftm.EndIndex).getPosition());
		}
		//Nun sind beide im left, also auf höhe 1 noch den neuen in der mitte zwischen left und right
		int x = Math.round((leftsubtree.modifyNodes.getNode(left.index).getPosition().x + leftsubtree.modifyNodes.getNode(right.index).getPosition().x)/2);
		VNode VSubTreeroot = new VNode(subtreeroot.index,x, gridY,gp.getIntValue("node.size"),gp.getIntValue("node.name_distance"),gp.getIntValue("node.name_rotation"),gp.getIntValue("node.name_size"),gp.getBoolValue("node.name_visible"));
		leftsubtree.modifyNodes.addNode(VSubTreeroot, new MNode(VSubTreeroot.getIndex(),subtreeroot.name));
		if ((type==UNDIR)||(type==DOWNTREE))
		{
			int l = mG.getEdgeIndices(subtreeroot.index, left.index).firstElement();
			int r = mG.getEdgeIndices(subtreeroot.index, right.index).firstElement();
			leftsubtree.modifyEdges.addEdge(new VStraightLineEdge(l,gp.getIntValue("edge.width")), new MEdge(l,subtreeroot.index, left.index,1,""), VSubTreeroot.getPosition(),leftsubtree.modifyNodes.getNode(left.index).getPosition());
			leftsubtree.modifyEdges.addEdge(new VStraightLineEdge(r,gp.getIntValue("edge.width")), new MEdge(r,subtreeroot.index, right.index,1,""), VSubTreeroot.getPosition(),leftsubtree.modifyNodes.getNode(right.index).getPosition());
		}
		else
		{
			int l = mG.getEdgeIndices(left.index,subtreeroot.index).firstElement();
			int r = mG.getEdgeIndices(right.index,subtreeroot.index).firstElement();
			leftsubtree.modifyEdges.addEdge(new VStraightLineEdge(l,gp.getIntValue("edge.width")), new MEdge(l,left.index, subtreeroot.index,1,""),leftsubtree.modifyNodes.getNode(left.index).getPosition(),VSubTreeroot.getPosition());
			leftsubtree.modifyEdges.addEdge(new VStraightLineEdge(r,gp.getIntValue("edge.width")), new MEdge(r,right.index, subtreeroot.index,1,""),leftsubtree.modifyNodes.getNode(right.index).getPosition(), VSubTreeroot.getPosition());
		}
		return leftsubtree;
	}
	
	public boolean isStepwiseRunable() 
	{
		return false; //Only in one step runable
	}

	public String setParameters(HashMap<String,Object> m) 
	{
		if (m==null)
			return "";
		if (m.get("MGraph")==null)
			return "Kein Graph angegeben";
		if (m.get("GridX")==null)
			return "kein Raster angegeben";
		if (m.get("GridY")==null)
			return "kein Raster angegeben";
		gridX = ((Integer) m.get("GridX")).intValue();
		gridY = ((Integer) m.get("GridY")).intValue();
		mG = (MGraph) m.get("MGraph");
		ta.setGraph(mG);
		ErgebnisGraph = new VGraph(mG.isDirected(),false,false);
		if ((gridX<=0)&&(gridY<=0))
			return "Einer der Max-Werte ist zu klein";
		if (!GraphOkay())
			return "Der Graph ist kein Binärbaum";	
		return "";
	}

	//ALL empty, because this algorithm is not able to be computed stepwise
	public void start() 
	{}
	public void step() 
	{}
	public boolean finished() {return true;} //Nicht schrittweise ausführbar also schrittweise fertig
}
