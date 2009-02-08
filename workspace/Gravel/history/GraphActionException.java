package history;

/**
 * Own Exception Class for GraphActions and their problems or errors
 * 
 * @author Ronny Bergmann
 * @since 0.3
 */
public class GraphActionException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 0x539;

	public GraphActionException()
	{
		
	}
	public GraphActionException(String s)
	{
		super(s);
	}
}
