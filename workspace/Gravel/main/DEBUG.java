package main;
/**
 * Encapsulated Debug Stuff.
 * 
 * @author Ronny Bergmann
 */
public class DEBUG {

	public static final int NONE=0;
	public static final int LOW=1;
	public static final int MIDDLE=2;
	public static final int HIGH=3;
	
	private static int level=NONE;
	
	/**
	 * Set Debug-Level to a specific value of
	 * - NONE: No Debug
	 * - LOW: only rough data
	 * - MIDDLE: little bit more than low, but not much debug output
	 * - HIGH: very detailed debug output
	 * @param lvl
	 */
	public static void setDebugLevel(int lvl)
	{
		level = lvl;
	}
	/**
	 * Get the actual Debug Level (for comparison with the above mentioned levels)
	 * 
	 * You should check whether the level is above or equal a given value before starting any output
	 * 
	 * @return
	 */
	public static int getDebugLevel()
	{
		return level;
	}
	/**
	 * Debug output if lvl is below the actual output level, this method prints s with linebreak at the end.
	 * @param lvl
	 * @param s
	 */
	public static void println(int lvl, String s)
	{
		if (lvl<=level)
			System.err.println(s);
	}
	/**
	 * Debug output if lvl is below the actual output level, this method prints s.
	 * @param lvl
	 * @param s
	 */	
	public static void print(int lvl, String s)
	{
		if (lvl<=level)
			System.err.print(s);
	}
}