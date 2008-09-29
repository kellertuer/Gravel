package algorithm;

import model.MGraph;
import model.VGraph;

import algorithm.forms.*;
import algorithm.model.LayeredTreeDraw;
import algorithm.model.MagnetismAndSprings;
import algorithm.model.RandomVisualize;
import algorithm.model.VAlgorithmIF;

/**
 * the algorithm factory is planned to create the Algorithm User Interface and the algorithm itself
 * 
 * but its still work in progress
 * @author ronny
 *
 */
public class AlgorithmFactory {

	public static final String RANDOM_VISUALIZE = "Zufällige Anordnung";
	public static final String LAYERED_TREE_DRAW = "Ebenenbasiertes Binärbaum-Zeichnen";
	public static final String SPRINGS_AND_MAGNETISM = "Feder- und Magneten-Modell";
	
	/**
	 * returns an algorithm depending on the name
	 * 
	 * @param AlgType name of the algorithm
	 * 
	 * @return
	 */
	public static VAlgorithmIF getAlgorithm(String AlgType)
	{
		if (AlgType.equals(LAYERED_TREE_DRAW))
			return new LayeredTreeDraw(); 
		else if (AlgType.equals(SPRINGS_AND_MAGNETISM))
			return new MagnetismAndSprings(); 
		else //if (AlgType.equals(RANDOM_VISUALIZ)) STD
			return new RandomVisualize(); 
		
	}
	/**
	 * opens the algorithm parameter form to determine the parameters for the actual algorithm
	 * 
	 * @param AlgType type with which the algorithm was initiated
	 * @param o an MGraph or VGraph depending on the Sort of algorithm
	 * @return the Algorithm Parameter Form to open and show it
	 * 
	 * @throws Exception
	 */
	public static AlgorithmParameterForm getForm(String AlgType, Object o) throws Exception
	{
		MGraph mg;
		VGraph vg;
		String graphtype = "";
		if (o instanceof VGraph)
		{
			vg = (VGraph) o;
			mg = vg.getMathGraph();
			graphtype = "VGraph";
		}
		else if (o instanceof MGraph)
		{
			mg = (MGraph) o;
			vg = null;
			graphtype = "MGraph";
		}
		else
			throw new Exception ("Unknown type of Object");
		
		if (AlgType.equals(RANDOM_VISUALIZE))
			return new RandomVisualizeForm(mg); 

		if (AlgType.equals(LAYERED_TREE_DRAW))
			return new LayeredTreeDrawForm(mg); 
		
		if (vg!=null)
		{
			if (AlgType.equals(SPRINGS_AND_MAGNETISM))
				return new MagnetismAndSpringsForm(vg);
		}
		
		throw new Exception("Unknown Combination of Type ("+AlgType+") and Graph ("+graphtype+").");			
	}
}
