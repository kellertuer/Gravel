package io;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import view.VGraphic;
/**
 * Export the Graph to a PNG-File
 * @author Ronny Bergmann
 */
public class PNGWriter
{
	private VGraphic vGc; //Die Umgebung zum zeichnen
	/**
	 * Init the exporter to a given Graphic Enviroment, that draws the 
	 * @param vg
	 */
	public PNGWriter(VGraphic vg)
	{
		vGc = vg;
	}
	/**
	 * Exports the Graph given in the Initialization to a file an resize the graph to a specified size
	 * 
	 * @param f specified file F
	 * @param x Export Size x 
	 * @param y Export Size y
	 */
	public void PNGExport(File f,int x,int y)
	{
		BufferedImage img;
		String type="png";
		Point min =  vGc.getVGraph().getMinPoint(vGc.getGraphics());
		Point max =  vGc.getVGraph().getMaxPoint(vGc.getGraphics());
		int oldz = GeneralPreferences.getInstance().getIntValue("vgraphic.zoom");
		int origx = (max.x-min.x);
		float z2 = (float)x/(float)origx;
		GeneralPreferences.getInstance().setIntValue("vgraphic.zoom",Math.round(z2*100));
		img = new BufferedImage( Math.round((float)max.x*z2)+1,Math.round((float)max.y*z2)+1, BufferedImage.TYPE_INT_ARGB );
		
		Graphics2D g = img.createGraphics();
		
		vGc.setSize(new Dimension((new Double(Math.ceil((double)min.x*(double)z2))).intValue()+x,(new Double(Math.ceil((double)min.y*(double)z2))).intValue()+y));
		vGc.paint(g);
		//System.err.println("Taking Subimage 0,0 : "+Math.round((float)min.x*z2)+","+Math.round((float)min.y*z2)+"Dim:"+x+","+y);
		img = img.getSubimage((new Double(Math.ceil((double)min.x*(double)z2))).intValue(),(new Double(Math.ceil((double)min.y*(double)z2))).intValue(),x,y);
		try {
			ImageIO.write(img, type, f);
		} catch (IOException e) {
			System.err.println("Saving failed...");
			e.printStackTrace();
		}
		GeneralPreferences.getInstance().setIntValue("vgraphic.zoom",oldz);
	}
}