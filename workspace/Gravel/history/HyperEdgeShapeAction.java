package history;

import java.util.Vector;

import model.VHyperEdge;
import model.VHyperGraph;
import model.Messages.GraphConstraints;

public class HyperEdgeShapeAction extends HyperGraphAction {

	public HyperEdgeShapeAction(VHyperEdge o, int action,
			VHyperGraph environment) throws GraphActionException {
		super(o, action, environment);
	}

	public HyperEdgeShapeAction(Vector<Object> Parameters, int action, VHyperGraph environment) throws GraphActionException
	{
		super(environment, action, true); //Init stuff to a HypergraphChange			
		if (environment==null)
			throw new GraphActionException("Could not Create Action: environmental HyperGraph must not be null.");
		ActionObject=Parameters.clone();
		Action=action;
		if ((action&(GraphConstraints.REMOVAL))>0) //Create or delete is active
		{
			ActionObject=null;
			Action=0;
			throw new GraphActionException("Creating or Deletion Parameters is not possible as an Creation always consists of multiple parameter changes.");
		}
			Objecttype=GraphConstraints.HISTORY;
	}
}
