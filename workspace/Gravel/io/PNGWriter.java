package io;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import model.VGraphInterface;

import view.VCommonGraphic;
import view.VGraphic;
import view.VHyperGraphic;
/**
 * Export the Graph to a PNG-File
 * @author Ronny Bergmann
 */
public class PNGWriter
{
	private VCommonGraphic vGc; //Die Umgebung zum zeichnen
	/**
	 * Init the exporter to a given Graphic Enviroment, that draws the 
	 * @param vg The VGraphic-Envronment containing the graph, that should be written to an PNG
	 * @param tB - Indicator whether the Background of the image should be transparent or white
	 */
	public PNGWriter(VCommonGraphic vg)
	{
		vGc = vg;
	}
	/**
	 * Exports the Graph given in the Initialization to a file an resize the graph to a specified size
	 * 
	 * @param f specified file F
	 * @param x Export Size x 
	 * @param y Export Size y
	 * @param tB - Set this value to true, if the Background should be transparent, to set the BG wwhite choose false
	 */
	public void PNGExport(File f,int x,int y, boolean tB)
	{
		BufferedImage img;
		String type="png";
		VGraphInterface vG=null;
		if (vGc.getType()==VCommonGraphic.VGRAPHIC)
			vG = ((VGraphic)vGc).getGraph();
		else if (vGc.getType()==VCommonGraphic.VHYPERGRAPHIC)
			vG = ((VHyperGraphic)vGc).getGraph();
		Point min =  vG.getMinPoint(vGc.getGraphics());
		Point max =  vG.getMaxPoint(vGc.getGraphics());
		int oldz = vGc.getZoom();
		int origx = (max.x-min.x);
		float z2 = (float)x/(float)origx;
		vGc.setZoom(Math.round(z2*100));
		img = new BufferedImage(Math.round((float)max.x*z2)+1,Math.round((float)max.y*z2)+1, BufferedImage.TYPE_INT_ARGB );
		
		Graphics2D g = img.createGraphics();
	
		//Actual Image Size
		Dimension dim = new Dimension((new Double(Math.ceil((double)min.x*(double)z2))).intValue()+x,(new Double(Math.ceil((double)min.y*(double)z2))).intValue()+y);
		if (!tB)
		{
			g.setBackground(Color.WHITE);		
			//Set complete area to white
			g.clearRect(0,0,(new Double(Math.ceil((double)min.x*(double)z2))).intValue()+x,(new Double(Math.ceil((double)min.y*(double)z2))).intValue()+y);
		}
		vGc.setSize(dim);
		vGc.paint(g);
		img = img.getSubimage((new Double(Math.ceil((double)min.x*(double)z2))).intValue(),(new Double(Math.ceil((double)min.y*(double)z2))).intValue(),x,y);
		try {
			ImageIO.write(img, type, f);
		} catch (IOException e) {
        	main.DEBUG.println(main.DEBUG.MIDDLE,"PNG Writing failed : "+e.getMessage());
		}
		vGc.setZoom(oldz);
	}
}