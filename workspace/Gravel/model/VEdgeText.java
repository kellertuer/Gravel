package model;

import io.GeneralPreferences;
/**
 * The Edge is extended by an Text that can be displayd along its path. The Properties are stored in this class
 * @author ronny
 */
public class VEdgeText {

	
	//text size of the edge name
	//visibility of the text
	//if true, show the edge value, if false show the edge name
	private boolean showvalue;
	private boolean visible;

	//distance from the edge and position on the edge
	//Position <50 means above, >50 means below the edge
	private int distance, size;
	float position;

	/**
	 * Std Values
	 *
	 */
	public VEdgeText()
	{
		distance = GeneralPreferences.getInstance().getIntValue("edge.text_distance");
		position = GeneralPreferences.getInstance().getFloatValue("edge.text_position");
		size = GeneralPreferences.getInstance().getIntValue("edge.text_size");
		visible = GeneralPreferences.getInstance().getBoolValue("edge.text_visible");
		showvalue = GeneralPreferences.getInstance().getBoolValue("edge.text_showvalue");	
	}
	/**
	 * given Values
	 *
	 */
	public VEdgeText(int td, float tp, int ts, boolean tv, boolean tw)
	{
		distance = td;
		position = tp;
		size = ts;
		visible = tv;
		showvalue = tw;
	}
	
	/**
	 * Distance of the edge text orthogonal to the edge in px
	 * 
	 * @return the distance in px
	 */
	public int getDistance() {
		return distance;
	}
	/**
	 * Set the Value in px the middle of the text is placed above or below (depending on position) the edge
	 * @param edgetextdistance
	 */
	public void setDistance(int distance) {
		this.distance = distance;
	}
	/**
	 * Returns the position along the edge the text middle is shown.
	 * teh value is between 0 and 100 and
	 * 	0 to 50 means it is shown CCW 90° to the edge direction, so if the edge is from left to right,
	 * 	the text is above the edge
	 * 
	 * 	51 to 100 means the text is placed CW 90° to the edge direction, so again if the edge is from left to right, 
	 * 	the edge text is placed below the edge line itself
	 * 
	 * @return position of the text
	 */
	public float getPosition() {
		return position;
	}
	/**
	 * Sets the position along the edge the text middle is shown.
	 * the value is in percent.
	 * On An Edge it has the following interpretation:
	 * 0-50% means 0 to 50 means it is shown CCW 90° to the edge direction, so if the edge is from left to right,
	 * 	the text is above the edge
	 * 51 - 100% means the text is placed CW 90° to the edge direction, so again if the edge is from left to right, 
	 * 	the edge text is placed below the edge line itself
	 * 
	 * On An HyperEdge it's analogue, just that 0-50% is rotatet CCW and therefore „outside“ (if shape is defined Clockwise)
	 * and for 51-100% „inside“
	 * @return position of the text
	 */
	public void setPosition(float position) {
		this.position = position;
	}
	/**
	 * Returns the parameter, which text of the edge is shown
	 * 
	 * true - the edge Value v is shown
	 * false - the edge name is shown
	 * 
	 * @return type of text that is shown (if visible)
	 */
	public boolean isshowvalue() {
		return showvalue;
	}
	/**
	 * Sets the parameter, which text of the edge is shown
	 * 
	 * true - the edge Value v is shown
	 * false - the edge name is shown
	 * 
	 * @param showvalue
	 */
	public void setshowvalue(boolean showvalue) {
		this.showvalue = showvalue;
	}
	/**
	 *  Returns the actual textsize in px
	 * @return the actual text size
	 */
	public int getSize() {
		return size;
	}
	/**
	 * Set the Text size the  is shown (if visible)
	 * @param size
	 */
	public void setSize(int size) {
		this.size = size;
	}
	/**
	 * Indicating the visibility of the edge text
	 * 
	 * @return true, if this edge text ist visble, esle false
	 */
	public boolean isVisible() {
		return visible;
	}
	/**
	 *  Set the Visibility of the edge
	 * @param visible true for visible, fasle for a invisible text
	 */
	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	public VEdgeText clone()
	{
		return new VEdgeText(distance, position, size, visible, showvalue);
	}
}
