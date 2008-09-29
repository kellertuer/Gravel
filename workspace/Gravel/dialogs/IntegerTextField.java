package dialogs;
import java.awt.*;
import javax.swing.*;
import javax.swing.text.*;
/** 
 * Extension of the textfield to handle only Integers
 * 
 * @author Ronny Bergmann
 *
 */
public class IntegerTextField extends JTextField
{
 
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private long maxValue = Integer.MAX_VALUE;
    private long minValue = 0;
    private int maxLength = String.valueOf(maxValue).length();
    private boolean isIPField = false;
 
    /**
    * Default constructor for IntegerTextField.
    */
    public IntegerTextField()
    {
    	super();
    }
    /**
     * Create and return a new Integer Model
     * 
     * @return IntegerDocument
     */
    protected Document createDefaultModel()
    {
	return new IntegerDocument();
    }
    
    /**
     *  Set the minimum value, that is valid (default is zero)
     *  a negative value is also possible
     * @param value
     */
    public void setMinValue(long value)
    {
	minValue = value;
    }
    /**
     * Get the minimum value
     * @return minimum value
     */
    public long getMinValue()
    {
	return minValue;
    }
 /*   public void setIPField(boolean value)
    {
	isIPField = value;
    }
 
    public boolean getIPField()
    {
	return isIPField;
    }
*/ 
    /**
     * Set the maximum value
     * @param value - the new maximum
     */
    public void setMaxValue(long value)
    {
    	maxValue = value;
    }
    /**
     * get the maximum value for the Integerfield
     * @return the value
     */
    public long getMaxValue()
    {
	return maxValue;
    }
    /**
     * Set the maximum digit count
     * @param value
     */
    public void setMaxLength(int value)
    {
	maxLength = value;
    }
    /**
     * get the maximum digit count
     * @return
     */
    public int getMaxLength()
    {
	return maxLength;
    }
    /**
     * get the value from the Integet Textfield
     * @return a number or minimum Value - 1 if the textfield is empty
     */
    public int getValue()
    {
    	if (getText().equals(""))
    	{
    		return (new Long(minValue-1)).intValue();
    	}
    	return (new Long(Long.parseLong(getText()))).intValue();
    }
    /**
     * Set the IntegerTextfield to the new Value
     * @param i new Value
     */
    public void setValue(int i)
    {
    	this.setText(""+i);
    }
    /**
     * Subclas for handling the textual input of the textfield
     * @author ronny
     *
     */
    private class IntegerDocument extends PlainDocument
    {
    	/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		/**
		 * Every time a key is pressed this insertString method is called and handles the key, updates the value
		 */
		public void insertString(int offs, String str, AttributeSet a) throws BadLocationException
    	{
    		long typedValue = -1;
 
    		StringBuffer textBuffer = new StringBuffer(IntegerTextField.this.getText().trim());
    		//The offset argument must be greater than or equal to 0, and less than or equal to the length of this string buffer
    		if((offs >= 0) && (offs <= textBuffer.length()))
    		{
    			textBuffer.insert(offs,str);
	    		String textValue = textBuffer.toString();
	    		if(textBuffer.length() > maxLength)
	    		{
					JOptionPane.showMessageDialog(IntegerTextField.this, "Die Zahl darf höchstens " + getMaxLength()+" Ziffern haben.", "Error Message",JOptionPane.ERROR_MESSAGE);
					return;
	    		}
 
				if((textValue == null) || (textValue.equals("")))
				{
					remove(0,getLength());
					super.insertString(0, "", null);
					return;
				}
 
				if(textValue.equals("-") && minValue < 0)
				{
					super.insertString(offs,new String(str), a);
					return;
				}
 
				if(str.equals(".") && isIPField)
				{
					super.insertString(offs,new String(str),a);
					return;
				}
				else
				{
					try
					{
						typedValue = Long.parseLong(textValue);
						if((typedValue > maxValue) || (typedValue < minValue))
						{
							//JOptionPane.showMessageDialog(IntegerTextField.this, "Es sind nur Werte zwischen "+getMinValue()+" und " + getMaxValue() + "zugelassen", "Error Message", JOptionPane.ERROR_MESSAGE);
						}
						else
						{
							super.insertString(offs,new String(str),a);
						}
					}
					catch(NumberFormatException ex)
					{
						Toolkit.getDefaultToolkit().beep();
						//JOptionPane.showMessageDialog(IntegerTextField.this, "Only numeric values allowed.", "Error Message", JOptionPane.ERROR_MESSAGE);
					}
				}
		}
    }
}
}