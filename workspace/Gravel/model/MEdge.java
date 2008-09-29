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
	public MEdge(int i,int s, int e, int v)
	{
		index = i;
		StartIndex = s;
		EndIndex = e;
		Value = v;
		name="";
	}
	public MEdge(int i,int s, int e, int v,String n)
	{
		this(i,s,e,v);
		name = n;
	}
}