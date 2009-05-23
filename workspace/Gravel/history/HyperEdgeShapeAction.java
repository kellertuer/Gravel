package history;


import model.*;
import model.Messages.*;

/**
 * Additional Action for tracking Creation Parameters for the NURBSShapeInterpolation and other
 * classes that create NURBS based on  vector of parameters
 * 
 * @author ronny
 *
 */
public class HyperEdgeShapeAction extends HyperGraphAction {

	boolean actionWasLocal;
	VHyperEdge VHEdgeRef;	
	
	/**
	 * Record actual Parameters as a new Action
	 * 
	 * @param Parameters parameters
	 * @param local where they applied to a local part of the curve ?
	 * @param e Hyperedge containing the modified shape (if existent already
	 * @param action What happened to the parameters? May be initial Creation else its an update
	 * @param environment Reference to the VHyperGraph the whole action is happening in
	 * @throws GraphActionException
	 */
	public HyperEdgeShapeAction(NURBSCreationMessage nm,boolean local, VHyperEdge e, int action, VHyperGraph environment) throws GraphActionException
	{
		super(e,action,environment); //Init stuff as a HyperEdge action in superclass			
		if (environment==null)
			throw new GraphActionException("Could not Create Action: environmental HyperGraph must not be null.");
		ActionObject=nm.clone();
		Action=action;
		if ((action&(GraphConstraints.REMOVAL|GraphConstraints.ADDITION))>0) //Only Updates possible, neither creation nor deletion of parameters
		{
			ActionObject=null;
			Action=0;
			throw new GraphActionException("Deletion Parameters is not possible as an Creation always consists of one creation and multiple parameter changes.");
		}
		//Part information to the action
		Objecttype = GraphConstraints.HYPEREDGESHAPE;
		Action=action|GraphConstraints.HYPEREDGESHAPE;
		actionWasLocal = local;
		VHEdgeRef = e;
	}
	
	protected VGraphInterface doReplace(VGraphInterface graph) throws GraphActionException
	{
		//There is just one replace
		if (graph.getType()!=VGraphInterface.HYPERGRAPH)
			throw new GraphActionException("Can't doReplace(): Wrong Type of Graph");
		VHyperGraph g = (VHyperGraph)graph;
		if (g.modifyHyperEdges.get(VHEdgeRef.getIndex())==null)
			throw new GraphActionException("Can't doreplace(): Edge nonexistent to replace its shape");
		//Set shape to the Shape Created by internal paramaters of this action
		g.modifyHyperEdges.get(VHEdgeRef.getIndex()).setShape(NURBSShapeFactory.CreateShape((NURBSCreationMessage)ActionObject));
		g.pushNotify(new GraphMessage(GraphConstraints.HYPEREDGE, VHEdgeRef.getIndex(), GraphConstraints.UPDATE|GraphConstraints.HISTORY|GraphConstraints.HYPEREDGESHAPE|GraphConstraints.CREATION, GraphConstraints.HYPEREDGE));
		return g;
	}
	protected VGraphInterface doCreate(VGraphInterface graph) throws GraphActionException
	{
		throw new GraphActionException("Can't doCreate() on ShapeParameters");		
	}

	protected VGraphInterface doDelete(VGraphInterface graph) throws GraphActionException
	{
		throw new GraphActionException("Can't doDelete() on ShapeParameters");
	}
}
