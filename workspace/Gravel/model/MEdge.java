package model;
/**
 * Eine mathematische Kante
 * <br>besteht aus :
 * <br>-einem eigenen Index
 * <br>-einem Startknotenindex
 * <br>-einem Endknotenindex
 * <br>-einem Kantenwert
 * <br>-einem boolschen Wert, ob die Kante gerichtet ist, oder nicht
 * 
 * @author Ronny Bergmann
 *
 */
public class MEdge	{
	public int index;
	public int StartIndex;
	public int EndIndex;
	public int Value;
	public String name;
	/**
	 * Constructor of an Egde
	 * @param i index of the Edge
	 * @param s adjacent Startnodeindex
	 * @param e adjacent Endnodeindex
	 * @param v value of the edge
	 * @param n name of the edge
	 */
	public MEdge(int i,int s, int e, int v,String n)
	{
		index = i;
		StartIndex = s;
		EndIndex = e;
		Value = v;
		name = n;
	}
	public MEdge clone()
	{
		return new MEdge (index,StartIndex,EndIndex,Value, new String(name.toCharArray()));
	}
}