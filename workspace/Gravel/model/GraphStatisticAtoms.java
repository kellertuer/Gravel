package model;

import java.awt.Point;
import java.awt.geom.PathIterator;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.TreeMap;
import java.util.Vector;

import model.Messages.GraphConstraints;
import model.Messages.GraphMessage;

import view.VCommonGraphic;
import view.VGraphic;
import view.VHyperGraphic;

/**
 * The Mathematical Statistic calcualtion of the Standard Vallues that can be used to create own Entries
 * 
 * This Class observes the Graph and reacts on Graph-Changes
 * and is observable, so that GUI-Classes who use this class can update their fields
 * 
 * This Observer may only subscribe to GraphMessage-delivering Classes
 * @author Ronny Bergmann
 *
 */
public class GraphStatisticAtoms extends Observable implements Observer {

	public static final String[] ATOMS = {
	"$Node.Count", //Knotenanzahl
	"$Edge.Count", //Kantenanzahl                     
	"$Graph.MaxX", //Maximal genutzte Koordinate in der Ebene - X
	"$Graph.MaxY", //Maximal genutzte Koordinate in der Ebene - Y
	"$Graph.MinX", //Minimal genutzte Koordinate in der Ebene - X
	"$Graph.MinY", //Minimal genutzte Koordinate in der Ebene - Y
	"$Graph.Area", //Vom Graph benutzte Fläche
	"$Node.InDegree.Min", //Minimaler Eingrad ((im ungerichteten sind in,out und degree je identisch)
	"$Node.InDegree.Max", //Maximaler Eingrad 
	"$Node.InDegree.Av", //durschnittlicher Eingrad
	"$Node.OutDegree.Min", //Min Ausgrad
	"$Node.OutDegree.Max", //Max Ausgrad
	"$Node.OutDegree.Av", //durschnittlicher Ausgrad
	"$Node.Degree.Min", //Min Grad (im ungerichteten in+out, sonst mit in und out identisch
	"$Node.Degree.Max", //Max Grad
	"$Node.Degree.Av", //durchschnittlicher Grad
	"$Edge.Length.Min", //Min Kantenlänge
	"$Edge.Length.Max", //Max Kantenlänge
	"$Edge.Length.Av", //durchschnittliche Kantenlänge
	"$Edge.Bends.Min", //Minimale Knickanzahl
	"$Edge.Bends.Max", //Max
	"$Edge.Bends.Av", //durchschnittliche
	"$Node.Distance.Min", //Knotenabstand, minimal
	"$Node.Distance.Max", //Knotenabstand, maximal
	"$Node.Distance.Av", //Knotenabstand, durchschnittlich
	};
	
	private VGraph vg;
	private VHyperGraph vhg;
	private VCommonGraphic vcg;
	private TreeMap<String,Double> atomvalues;
	private int blockdepth;
	/**
	 * Init the Statistics as it corresponds always to a graph there must be a
	 * @param g VGraph 
	 */
	public GraphStatisticAtoms(VCommonGraphic g)
	{
		blockdepth=0;
		vcg=g;
		atomvalues = new TreeMap<String, Double>();
		if (g.getType()==VCommonGraphic.VGRAPHIC)
		{
			vg = ((VGraphic)g).getGraph();
			vhg=null;
			vg.addObserver(this);
			//INIT VALUES
			update(vg,new GraphMessage(GraphConstraints.GRAPH_ALL_ELEMENTS,GraphConstraints.ADDITION,GraphConstraints.GRAPH_ALL_ELEMENTS));
		}
		else if (g.getType()==VCommonGraphic.VHYPERGRAPHIC)
		{
			vg = null;
			vhg= ((VHyperGraphic)g).getGraph();
			vhg.addObserver(this);
			//INIT VALUES
			update(vhg,new GraphMessage(GraphConstraints.HYPERGRAPH_ALL_ELEMENTS,GraphConstraints.ADDITION,GraphConstraints.HYPERGRAPH_ALL_ELEMENTS));
		}
	}
	/**
	 * Get the Value of an Atom Value, if there is no Value with the given Name, NaN is returned
	 * @param atomname name of the value
	 * @return the value in the graph of the atom if it exists, else NaN
	 */
	public Double getValuebyName(String atomname)
	{
		if (atomvalues.get(atomname)!=null)
			return atomvalues.get(atomname);
		else
			return Double.NaN;
	}
	/**
	 * Calculate all Atom Values in the given VGraph and save hem in the Treemap
	 */
	private void calculate()
	{
		if (vg!=null)
			calculateGraph();
		else if (vhg!=null)
			calculateHypergraph();
	}
	private void calculateHypergraph()
	{
		//TODO: Calculate Hypergraph-Stats
	}
	private void calculateGraph()
	{
		TreeMap<Integer,Integer> valenz, invalenz, outvalenz, bends;
		TreeMap<Integer,Double> kantenlaenge, knotenabstand;
		valenz = new TreeMap<Integer,Integer>();
		invalenz = new TreeMap<Integer,Integer>();
		outvalenz = new TreeMap<Integer,Integer>();
		bends = new TreeMap<Integer,Integer>();
		kantenlaenge = new TreeMap<Integer,Double>();
		knotenabstand = new TreeMap<Integer,Double>();
		Iterator<MEdge> edgeiter = vg.getMathGraph().modifyEdges.getIterator();
		while (edgeiter.hasNext())
		{
			//Valenzen
			MEdge me = edgeiter.next();
			int start = me.StartIndex, ende = me.EndIndex;
			if (valenz.get(start)==null)
				valenz.put(start,0);
			if (valenz.get(ende)==null)
				valenz.put(ende,0);
			if (invalenz.get(ende)==null)
				invalenz.put(ende,0);
			if (outvalenz.get(start)==null)
				outvalenz.put(start,0);
			valenz.put(start,valenz.get(start)+1);
			valenz.put(ende,valenz.get(ende)+1);
			outvalenz.put(start,outvalenz.get(start)+1);
			invalenz.put(ende,invalenz.get(ende)+1);
			//
			Point startpoint = vg.modifyNodes.get(start).getPosition();
			Point endpoint = vg.modifyNodes.get(ende).getPosition();
			VEdge actual = vg.modifyEdges.get(me.index);
			switch(actual.getEdgeType())
			{
				case VEdge.ORTHOGONAL : {
					bends.put(actual.getIndex(),1); 
					Point cp;
					if (((VOrthogonalEdge)actual).getVerticalFirst())
						cp = new Point(startpoint.x,endpoint.y);
					else
						cp = new Point(startpoint.x,endpoint.x);
					kantenlaenge.put(actual.getIndex(),startpoint.distance(cp) + cp.distance(endpoint));
					break;
				}
				case VEdge.SEGMENTED : {
					Vector<Point> p = ((VSegmentedEdge)actual).getControlPoints();
					int count = 0, lastindex = 0;
					for (int i=0; i<p.size(); i++)
					{
						if (p.get(i)!=null)
						{
							if (count==0)
								kantenlaenge.put(actual.getIndex(),startpoint.distance(p.get(i)));
							else
								kantenlaenge.put(actual.getIndex(), kantenlaenge.get(actual.getIndex())+p.get(lastindex).distance(p.get(i)));
							lastindex = i; count++;
						}
					}
					if (count==0) 
						kantenlaenge.put(actual.getIndex(),startpoint.distance(endpoint)); //Kommt sowas vor ?
					else
						kantenlaenge.put(actual.getIndex(), kantenlaenge.get(actual.getIndex()) + p.get(lastindex).distance(endpoint));
					bends.put(actual.getIndex(),count);
					break;
				}
				case VEdge.STRAIGHTLINE : {
					kantenlaenge.put(actual.getIndex(),startpoint.distance(endpoint));
					bends.put(actual.getIndex(),0);
					break;
				}
				case VEdge.LOOP : 
				case VEdge.QUADCURVE : {
						bends.put(actual.getIndex(),0);
						PathIterator path = actual.getPath(startpoint, endpoint, 1.0f).getPathIterator(null,0.001);
						double[] coords = new double[2]; double x = 0.0, y = 0.0, lastx=0.0, lasty = 0.0;
						kantenlaenge.put(actual.getIndex(),0.0d);
						while( !path.isDone() ) 
					    {
					    	int type = path.currentSegment(coords);
					    	x = coords[0]; y = coords[1];
					    	if (type==PathIterator.SEG_LINETO)
					    			kantenlaenge.put(actual.getIndex(), kantenlaenge.get(actual.getIndex()) + (new Point.Double(x,y)).distance(lastx,lasty));
					    	lastx = x; lasty = y;
					    	path.next();
					    }
						break;
				}
				default : break;
			} //End Switch Edgetyoe
		} //End While edgeiter.hasnext
		//
		//Knotenabstaende
		Iterator<MNode> nodeiter = vg.getMathGraph().modifyNodes.getIterator();
		int i=0;
		while (nodeiter.hasNext())
		{
			MNode mn = nodeiter.next();
			if (valenz.get(mn.index)==null) //Oben nie dran gewesen ==> 0 setzen
				valenz.put(mn.index,0);
			if (invalenz.get(mn.index)==null) //Oben nie dran gewesen ==> 0 setzen
				invalenz.put(mn.index,0);
			if (outvalenz.get(mn.index)==null) //Oben nie dran gewesen ==> 0 setzen
				outvalenz.put(mn.index,0);
			
			//Mit allen folgenden vergleichen
			Iterator<MNode> rest = vg.getMathGraph().modifyNodes.getIterator();
			while (!mn.equals(rest.next())&&rest.hasNext()){}
			while (rest.hasNext())
			{
				knotenabstand.put(i++,vg.modifyNodes.get(mn.index).getPosition().distance(vg.modifyNodes.get(rest.next().index).getPosition()));
			}
		}
		//Nun noch werte Setzen
		//Valenzen
		if (!vg.getMathGraph().isDirected())
		{
			invalenz = valenz; outvalenz = valenz;
		}
		//InValent
		Vector<Double> minmaxav = getIntStats(invalenz);
		atomvalues.put("$Node.InDegree.Min", minmaxav.get(0));
		atomvalues.put("$Node.InDegree.Max", minmaxav.get(1));
		atomvalues.put("$Node.InDegree.Av", minmaxav.get(2));
		minmaxav = getIntStats(outvalenz);
		atomvalues.put("$Node.OutDegree.Min", minmaxav.get(0));
		atomvalues.put("$Node.OutDegree.Max", minmaxav.get(1));
		atomvalues.put("$Node.OutDegree.Av", minmaxav.get(2));
		minmaxav = getIntStats(valenz);
		atomvalues.put("$Node.Degree.Min", minmaxav.get(0));
		atomvalues.put("$Node.Degree.Max", minmaxav.get(1));
		atomvalues.put("$Node.Degree.Av", minmaxav.get(2));
		//Kantenlaenge
		minmaxav = getDoubleStats(kantenlaenge);
		atomvalues.put("$Edge.Length.Min", minmaxav.get(0));
		atomvalues.put("$Edge.Length.Max", minmaxav.get(1));
		atomvalues.put("$Edge.Length.Av", minmaxav.get(2));
		
		//Bends
		minmaxav = getIntStats(bends);
		atomvalues.put("$Edge.Bends.Min", minmaxav.get(0));
		atomvalues.put("$Edge.Bends.Max", minmaxav.get(1));
		atomvalues.put("$Edge.Bends.Av", minmaxav.get(2));
		
		//Knotenabstände
		minmaxav = getDoubleStats(knotenabstand);
		atomvalues.put("$Node.Distance.Min", minmaxav.get(0));
		atomvalues.put("$Node.Distance.Max", minmaxav.get(1));
		atomvalues.put("$Node.Distance.Av", minmaxav.get(2));
		
	}
	/**
	 * Change all Integer Values to Double and Compute min, max and average of the 
	 * @param vals given treemap of values
	 * @return
	 */
	private Vector<Double> getIntStats(TreeMap<Integer,Integer> vals)
	{
		if (vals.isEmpty()) //Keine Werte => alles 0 setzen;
		{
			Vector<Double> ret = new Vector<Double>();
			ret.add(0.0d);ret.add(0.0d);ret.add(0.0d);
			return ret;
		}
		
		int max=Integer.MIN_VALUE, min=Integer.MAX_VALUE;
		double average = 0;
		Iterator<Integer> iter = vals.keySet().iterator();
		int count=0;
		while (iter.hasNext())
		{
			Integer i = vals.get(iter.next());
			if (i>max) max = i;
			if (i<min) min = i;
			average += i;
			count++;
			}
		average /= count;
		Vector<Double> ret = new Vector<Double>();
		ret.add((double)min);
		ret.add((double)max);
		ret.add(average);
		return ret;
	}
	/**
	 * Compute min max and avg of the given
	 * @param vals treemap of double values
	 * @return
	 */
	private Vector<Double> getDoubleStats(TreeMap<Integer,Double> vals)
	{
		if (vals.isEmpty()) //Keine Werte => alles 0 setzen;
		{
			Vector<Double> ret = new Vector<Double>();
			ret.add(0.0d);ret.add(0.0d);ret.add(0.0d);
			return ret;
		}
		
		double max=Double.MIN_VALUE, min=Double.MAX_VALUE;
		double average = 0;
		Iterator<Integer> iter = vals.keySet().iterator();
		int count=0;
		while (iter.hasNext())
		{
			Double i = vals.get(iter.next());
			if (i>max) max = i;
			if (i<min) min = i;
		average += i;
		count++;
		}
		average /= count;
		Vector<Double> ret = new Vector<Double>();
		ret.add((double)min);
		ret.add((double)max);
		ret.add(average);
		return ret;
	}
	
	public void update(Observable arg0, Object arg1) 
	{	
		GraphMessage m = (GraphMessage)arg1;
		if ((m.getModification()&GraphConstraints.BLOCK_END)==GraphConstraints.BLOCK_END) //Block ends with this Message
		{
			if (blockdepth > 0)
				blockdepth--;
		}
		else if ((m.getModification()&GraphConstraints.BLOCK_START)==GraphConstraints.BLOCK_START)
		{
				blockdepth++;
			return;
		}
		if (blockdepth>0)
				return;

		calculate();
		VGraphInterface temp=null;
		if (vg!=null)
		{
			temp = vg;
			atomvalues.put("$Node.Count",(double)vg.getMathGraph().modifyNodes.cardinality()); 
			atomvalues.put("$Edge.Count",(double)vg.getMathGraph().modifyEdges.cardinality()); 
		}
		else if (vhg!=null)
		{
			temp = vhg;
			atomvalues.put("$Node.Count",(double)vhg.getMathGraph().modifyNodes.cardinality()); 
			atomvalues.put("$Edge.Count",(double)vhg.getMathGraph().modifyHyperEdges.cardinality()); 
		}
		atomvalues.put("$Graph.MaxX",(double)temp.getMaxPoint(vcg.getGraphics()).x);
		atomvalues.put("$Graph.MaxY",(double)temp.getMaxPoint(vcg.getGraphics()).y);
		atomvalues.put("$Graph.MinX",(double)temp.getMinPoint(vcg.getGraphics()).x);
		atomvalues.put("$Graph.MinY",(double)temp.getMinPoint(vcg.getGraphics()).y);
		atomvalues.put("$Graph.Area",(double)0);
		setChanged();
		notifyObservers("Atoms");
	}
}