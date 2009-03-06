package model;

import model.Messages.GraphConstraints;

public interface MGraphInterface {

	public final int GRAPH = GraphConstraints.MATH|GraphConstraints.GRAPH;
	public final int HYPERGRAPH = GraphConstraints.MATH|GraphConstraints.HYPERGRAPH;
	
	//Implementing Methods
	public MNodeSet modifyNodes=null;
	public MSubgraphSet modifySubgraphs=null;
	/**
	 * get the Type of the 
	 * @return
	 */
	int getType();
}
