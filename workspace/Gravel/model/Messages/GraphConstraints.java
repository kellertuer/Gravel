package model.Messages;

public interface GraphConstraints {

	//Type of change (and affection)
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
	public static final int ALL = 0xffff;

	//Type of whole graphs
	public static final int MATH = 2048; 
	public static final int VISUAL = 4096; 
	//
	public static final int GRAPH = 1024; 
	public static final int HYPERGRAPH = 512; 
	
	//Modifications
	public static final int UPDATE = 1;
	public static final int ADDITION = 2;
	public static final int REMOVAL = 4;
	public static final int HISTORY = 8;
	public static final int TRANSLATION = 16;
	public static final int REPLACEMENT = 32;
	public static final int INDEXCHANGED = 64;

	//Special Stati for single parts
	public static final int HYPEREDGESHAPE = 64;	
	public static final int PARTINFO = HYPEREDGESHAPE;
	
	//Special Stati for Block Updates
	public static final int BLOCK_START = 512;
	public static final int BLOCK_END = 1024;
	public static final int BLOCK_ABORT = 2048;
	public static final int BLOCK_ALL = BLOCK_START|BLOCK_END|BLOCK_ABORT; //All 3 Block-Stati
}
