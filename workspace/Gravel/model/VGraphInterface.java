package model;

import model.Messages.GraphConstraints;

public interface VGraphInterface {

	public final int GRAPH = GraphConstraints.VISUAL|GraphConstraints.GRAPH;
	public final int HYPERGRAPH = GraphConstraints.VISUAL|GraphConstraints.HYPERGRAPH;
	
	/**
	 * get the Type of the 
	 * @return
	 */
	int getType();
	
}
