package model;

import io.GeneralPreferences;

public class VEdgeArrow {

	private float size = GeneralPreferences.getInstance().getIntValue("edge.arrow_size");
	private float part = GeneralPreferences.getInstance().getFloatValue("edge.arrow_part");
	private float alpha = GeneralPreferences.getInstance().getIntValue("edge.arrow_alpha");
	private float pos = GeneralPreferences.getInstance().getFloatValue("edge.arrow_pos");
	
	/**
	 * Initialize the Arrow with the standard values from GeneralPreferences
	 */
	public VEdgeArrow()
	{}
	/**
	 * Initialize Arrow with given
	 * @param arrSize Size
	 * @param arrPart Part of Size filled with arrow
	 * @param arrAlpha Angle in the arrowhead
	 * @param arrPos position along edge
	 */
	public VEdgeArrow(float arrSize, float arrPart, float arrAlpha, float arrPos) {
		this.setSize(arrSize);
		this.setPart(arrPart);
		this.alpha = arrAlpha;
		this.setPos(arrPos);
	}
	/**
	 * Set the position of the Arrow. 0 Start 1 End or in between
	 * @param i
	 */
	public void setPos(float i) {
		pos = i;
	}
	/**
	 * Position of the arrow on the edge. 0 means at the start 1 means at the end.
	 * @return position
	 */
	public float getPos() {
		return pos;
	}
	/**
	 * set the angle in the arrowhead to
	 * @param i Degree
	 */
	public void setAngle(float i)
	{
		//Speicherung intern halbiert 
		alpha = i;
	}
	/**
	 * get the Angle in the arrowhead
	 * @return
	 */
	public float getAngle()
	{
		return alpha;
	}
	/**
	 * Set the Arrowpart. That is the part from the arrowhead along the Edge thats filled with color. 0 means none, 1 means the whole arrow to arrowsize is filled
	 * @param i the new arrowpart - must be between 0 and 1
	 */
	public void setPart(float i)
	{
		part = i;
	}
	/**
	 * get the Arrowpart. That is the part from the arrowhead along the Edge thats filled with color. 0 means none, 1 means the whole arrow to arrowsize is filled
	 * @return
	 */
	public float getPart()
	{
		return part;
	}
	/**
	 * Set the Siize of the Arrow
	 * @param i
	 */
	public void setSize(float i)
	{
		size = i;
	}
	/**
	 * Get the actual Size of the Arrow
	 * @return
	 */
	public float getSize()
	{
		return size;
	}
	public VEdgeArrow clone()
	{
		return new VEdgeArrow(size, part, alpha, pos);
	}
}