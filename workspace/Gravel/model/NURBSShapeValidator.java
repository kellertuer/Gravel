package model;

/**
 * The NURBSSHapeValidator determines, whether a given NURBSShape C
 * is a valid shape for a specified Hyperedge e in an visual HypgerGraph 
 * 
 * That is the case if and only if the following three conclusions hold
 * - All Nodepositions p_i of nodes belonging to e are inside the NURBSSHape
 * - their distance to the Shape is at most distance+their radius, for each v_i in e
 * - All Nodepositions p_i of nodes not belonging to e are outside the NURBSShape
 *
 * @author Ronny Bergmann
 * @since 0.4 
 */
public class NURBSShapeValidator extends NURBSShape {

	
}
