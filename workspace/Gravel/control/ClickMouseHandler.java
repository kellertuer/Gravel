package control;

import io.GeneralPreferences;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import view.*;
import dialogs.*;
import model.*;
import model.Messages.*;

/**
 * Super class for the mouse handler for mouseclicks
 * this class is abstract, and is implemented by every mouse mode that is available in gravel
 * 
 * This abstract superclass is also an observer. it is an oberserv the VGraph
 * 
 * This Observer may only subscribe to Observables that send GraphMessages
 * @author ronny
 *
 */
public abstract class ClickMouseHandler implements MouseListener, Observer
{
	CommonNodeClickListener NodeMouseActions;
	SelectionClickListener SelectionMouseActions;
	ContextMenuClickListener PopupClickActions;

	public ClickMouseHandler(VGraphic g)
	{
		NodeMouseActions = new CommonNodeClickListener(g);
		SelectionMouseActions = new SelectionClickListener(g);;
		PopupClickActions = new ContextMenuClickListener(g);
	}

	public ClickMouseHandler(VHyperGraphic hg)
	{
		NodeMouseActions = new CommonNodeClickListener(hg);
		SelectionMouseActions = new SelectionClickListener(hg);;
		PopupClickActions = new ContextMenuClickListener(hg);
	}

	public void mouseClicked(MouseEvent e)
	{
		NodeMouseActions.mouseClicked(e);
		SelectionMouseActions.mouseClicked(e);
		PopupClickActions.mouseClicked(e);
	}
	public void update(Observable o, Object arg)
	{
		PopupClickActions.update(o, arg);
	}
}
