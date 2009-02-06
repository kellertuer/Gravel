package dialogs.components;

import io.GeneralPreferences;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import view.VGraphic;

import model.VEdge;
import model.VEdgeLinestyle;
import model.VEdgeText;
import model.VGraph;
import model.VNode;
import model.VStraightLineEdge;
/**
 * This Class contains the Preview for an edge, that is edited. It is an Observer, 
 * so that it might react on Input Fields, for example the Input Fields of the Arrow Properties
 * 
 * @author ronny
 *
 */
public class CEdgeArrowPreview extends Container implements Observer{

	private static final long serialVersionUID = 1L;
	
	private VGraphic ArrowG;
	private VEdge ArrowEdge;

	/**
	 * Constrcts the preview for the values of a given edge v
	 * If the edge is null, the global Std Values are used
	 * 
	 * @param e
	 */
	public CEdgeArrowPreview(VEdge e)
	{
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(0,0,0,0);
		c.anchor = GridBagConstraints.WEST;

		VGraph ArrowTest = new VGraph(true,false,false); //directed without loops nor multiple
		ArrowTest.addNode(new VNode(1,30,15,20,0,0,0,false),"T1");
		ArrowTest.addNode(new VNode(2,170,15,20,0,0,0,false),"T2");
		ArrowTest.addSubSet(1,"white", new Color(0.5f,0.5f,0.5f,1f));
		ArrowTest.addNodetoSubSet(1,1);
		ArrowTest.addNodetoSubSet(2,1);
		ArrowEdge = new VStraightLineEdge(1,2);
		ArrowTest.addEdge(ArrowEdge,1,2);
		ArrowTest.setEdgeName(ArrowEdge.index,"e");
		ArrowTest.getEdge(ArrowTest.getEdgeIndices(1,2).firstElement()).setTextProperties(new VEdgeText());
		ArrowTest.getEdge(ArrowTest.getEdgeIndices(1,2).firstElement()).setLinestyle(new VEdgeLinestyle());
		ArrowG = new VGraphic(new Dimension(200,60), ArrowTest);
		ArrowG.setMouseHandling(VGraphic.NO_MOUSEHANDLING);
		ArrowG.setPreferredSize(new Dimension(200,62));
		ArrowG.setZoomEnabled(false);
		GeneralPreferences.getInstance().deleteObserver(ArrowG);
		add(ArrowG,c);
		InitValues(e);
	}
	/**
	 * Initialize the Preview with the Values of a given Edge 
	 * Choose null to get the global Preferencex
	 * 
	 * @param e
	 */
	public void InitValues(VEdge e)
	{
		if (e!=null)
		{
			ArrowEdge = ArrowG.getVGraph().getEdge(1);
			ArrowEdge.setArrowAngle(e.getArrowAngle());
			ArrowEdge.setArrowPart(e.getArrowPart());
			ArrowEdge.setArrowSize(e.getArrowSize());
			ArrowEdge.setArrowPos(e.getArrowPos());
			
			ArrowEdge.getTextProperties().setDistance(e.getTextProperties().getDistance());
			ArrowEdge.getTextProperties().setPosition(e.getTextProperties().getPosition());
			ArrowEdge.getTextProperties().setSize(e.getTextProperties().getPosition());
			ArrowEdge.getTextProperties().setshowvalue(e.getTextProperties().isshowvalue());
			ArrowEdge.getTextProperties().setVisible(e.getTextProperties().isVisible());

			ArrowEdge.getLinestyle().setType(e.getLinestyle().getType());
			ArrowEdge.getLinestyle().setLength(e.getLinestyle().getLength());
			ArrowEdge.getLinestyle().setDistance(e.getLinestyle().getDistance());
		}
		else
		{
			GeneralPreferences gp = GeneralPreferences.getInstance();
			ArrowEdge.setArrowAngle(gp.getIntValue("edge.arralpha"));
			ArrowEdge.setArrowPart((float)gp.getIntValue("edge.arrpart")/100.0f);
			ArrowEdge.setArrowSize(gp.getIntValue("edge.arrsize"));
			ArrowEdge.setArrowPos(gp.getIntValue("edge.arrpos")/100.0f);
			
			ArrowEdge.getTextProperties().setDistance(gp.getIntValue("edge.text_distance"));
			ArrowEdge.getTextProperties().setPosition(gp.getIntValue("edge.text_position"));
			ArrowEdge.getTextProperties().setSize(gp.getIntValue("edge.text_size"));
			ArrowEdge.getTextProperties().setshowvalue(gp.getBoolValue("edge.text_showvalue"));
			ArrowEdge.getTextProperties().setVisible(gp.getBoolValue("edge.text_visible"));

			ArrowEdge.getLinestyle().setType(gp.getIntValue("edge.line_type"));
			ArrowEdge.getLinestyle().setLength(gp.getIntValue("edge.line_length"));
			ArrowEdge.getLinestyle().setDistance(gp.getIntValue("edge.line_distance"));
		}
		ArrowG.repaint();
		ArrowG.validate();
		this.validate();
	}
	@SuppressWarnings("unchecked")
	public void update(Observable arg0, Object arg1) 
	{
		try{
			VEdge e = (VEdge) arg1;
			if (e!=null)
			{
				InitValues(e);
			}
		}
		catch (Exception e){}
		
		try {
			Vector<Integer> v = (Vector<Integer>) arg1;
			
			if (v!=null)
			{
				if (v.firstElement()==CEdgeArrowParameters.CEDGEARROWPARAMETERS)
				{
					ArrowEdge.setArrowSize(v.get(CEdgeArrowParameters.ARROW_SIZE));
					ArrowEdge.setArrowAngle(v.get(CEdgeArrowParameters.ARROW_ANGLE));
					ArrowEdge.setArrowPos(v.get(CEdgeArrowParameters.ARROW_POS)/100.0f);
					ArrowEdge.setArrowPart(v.get(CEdgeArrowParameters.ARROW_PART)/100.0f);
					ArrowG.repaint();

				}
				if (v.firstElement()==CEdgeTextParameters.CEDGETEXTPARAMETERS)
				{
					VEdgeText m = new VEdgeText(v.get(CEdgeTextParameters.TEXTDISTANCE),
												v.get(CEdgeTextParameters.TEXTPOSITION),
												v.get(CEdgeTextParameters.TEXTSIZE),
												v.get(CEdgeTextParameters.BTEXTVISIBLE)==1,
												v.get(CEdgeTextParameters.BSHOWTEXTVALUE)==1);
					ArrowEdge.setTextProperties(m);
					ArrowG.repaint();
				}
				if (v.firstElement()==CEdgeLineParameters.CEDGELINEPARAMETERS)
				{
					VEdgeLinestyle l = new VEdgeLinestyle(v.get(CEdgeLineParameters.LINETYPE)+1,
														  v.get(CEdgeLineParameters.LINELENGTH),
														  v.get(CEdgeLineParameters.LINEDISTANCE));
					ArrowEdge.setLinestyle(l);
					ArrowG.repaint();
				}
			}
		}
		catch (Exception e){}
		
		ArrowG.repaint();
	}

}
