package control;

import javax.swing.JButton;
/**
  * Little helping thread for the runText
  * It does only one thing in a single thread
  * adding points to the original text to indicate running
  * 
  * which is also given to the MouseOverButtonListener, that might interrupt the display
  * to display it's own text when the mouse is moved over this Button
  * 
  * @author ronny
  *
  */
public class DisplayRunningController extends Thread {
	JButton DisplayOn;
    int pointCount=0;
    String OrigTextPre,OrigTextPost;
    boolean running=true,pause=false;
	public DisplayRunningController(JButton target)
	{
		DisplayOn = target;
		String Origtext = DisplayOn.getText();
		if (Origtext.endsWith("</html>"))
		{
			OrigTextPre = Origtext.substring(0,Origtext.length()-7);
			OrigTextPost = "</html>";
		}
		else
		{
			OrigTextPre = Origtext;
			OrigTextPost = "";
		}
	}
	//Little Helping Function for the animated button
    public void run() {
    	while (running)
    	{
    		pointCount++;
    		if (pointCount>3)
    			pointCount=1;
    		String t= ""+OrigTextPre;
    		for(int i = 0; i < pointCount; i++)
    			t +=".";
    		t+=OrigTextPost;
    		if (!pause)
    			DisplayOn.setText(t);
    		try {
    			sleep(500);
    		}
    		catch(InterruptedException e) {}
    	}
    }
    public void setAnimationPaused(boolean paused)
    {
    	pause = paused;
    }
    public void stopAnimation()
    {
	   	DisplayOn.setText(OrigTextPre+OrigTextPost);
	   	running=false;
	}
}
