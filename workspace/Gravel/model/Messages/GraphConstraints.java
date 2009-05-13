package model.Messages;

/**
 * These Constraints are used through the whole system of Messaging for identification of the type of action
 * 
 * The Element modified is determined by the status values
 * 
 * The Type of action is determined by the Modification Values
 * which might be extended by information about single parts and Block-Information
 * 
 * So using the action stuff should be done the following way:
 * 
 * @author ronny
 *
 */
public interface GraphConstraints {

	/*
	 * Element Information for Indication of changed Elements
	 */
	public static final int NODE = 1;
	public static final int EDGE = 2;
	public static final int HYPEREDGE = 4;
	public static final int SUBGRAPH = 8;
	public static final int GRAPH_ALL_ELEMENTS=NODE|EDGE|SUBGRAPH;
	public static final int HYPERGRAPH_ALL_ELEMENTS=NODE|HYPEREDGE|SUBGRAPH;
	public static final int SELECTION = 16;
	public static final int DIRECTION = 32;
	public static final int LOOPS = 64;
	public static final int MULTIPLE = 128;
	public static final int ELEMENT_MASK = 0xffff;

	/*
	 * Graph Indicators for the types of Graph - may be used in same Indicator as the Element Values
	 */
	public static final int MATH = 2048; 
	public static final int VISUAL = 4096; 
	public static final int GRAPH = 1024; 
	public static final int HYPERGRAPH = 512; 
	
	/*
	 * Modification Indicators, that cover all possible changes that might have happened 
	 */
	public static final int UPDATE = 1;
	public static final int ADDITION = 2;
	public static final int REMOVAL = 4;
	public static final int HISTORY = 8;
	public static final int TRANSLATION = 16;
	public static final int REPLACEMENT = 32;
	public static final int INDEXCHANGED = 64;

	public static final int ACTIONMASK = UPDATE | ADDITION | REMOVAL | HISTORY | TRANSLATION | REPLACEMENT | INDEXCHANGED;
	
	
	/*
	 * Additional Special Indicators for Partial information of the Modification, e.g. whether it was local or not 
	 */
	public static final int HYPEREDGESHAPE = 128;
	public static final int CREATION = 256; //Is there still just Interpolation Parameters ?
	public static final int LOCAL = 512;	//Local Shape change if not given, the action is assumed to be shape-global

	public static final int PARTINFORMATIONMASK = HYPEREDGESHAPE|LOCAL|CREATION;
	
	/*
	 * Additional Special information about block - which may be used to accummulate some actions into one
	 * The complete action should be indicated by the blockstart, so that any method can ignroe the block and handle the actions individually,
	 * though they are blocked
	 * 
	 */
	public static final int BLOCK_START = 1024;
	public static final int BLOCK_END = 2048;
	public static final int BLOCK_ABORT = 4096;
	public static final int BLOCKMASK = BLOCK_START|BLOCK_END|BLOCK_ABORT; //All 3 Block-Stati
}
