/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import java.util.*;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.event.*;

public interface UIPanel extends Runnable {
    public UI newui(Session sess);
    public void background(boolean bg);

    /* Stuff that is inherited from AWT components, but that have to
     * be declared since Java is stupid. */
    public void setSize(int w, int h);
    public Dimension getSize();

    public static class Dispatcher implements KeyListener, MouseListener, MouseWheelListener, MouseMotionListener {
	public final Queue<InputEvent> events = new LinkedList<>();
	public MouseEvent mousemv = null;

	public void dispatch(UI ui) {
	    synchronized(this) {
		if(mousemv != null) {
		    Coord pos = new Coord(mousemv.getX(), mousemv.getY());
		    ui.mousemove(mousemv, pos);
		    mousemv = null;
		}
		InputEvent e;
		while((e = events.poll()) != null) {
		    if(e instanceof MouseEvent) {
			MouseEvent me = (MouseEvent)e;
			if(me.getID() == MouseEvent.MOUSE_PRESSED) {
			    ui.mousedown(me, new Coord(me.getX(), me.getY()), me.getButton());
			} else if(me.getID() == MouseEvent.MOUSE_RELEASED) {
			    ui.mouseup(me, new Coord(me.getX(), me.getY()), me.getButton());
			} else if(me instanceof MouseWheelEvent) {
			    ui.mousewheel(me, new Coord(me.getX(), me.getY()), ((MouseWheelEvent)me).getWheelRotation());
			}
		    } else if(e instanceof KeyEvent) {
			KeyEvent ke = (KeyEvent)e;
			if(ke.getID() == KeyEvent.KEY_PRESSED) {
			    ui.keydown(ke);
			} else if(ke.getID() == KeyEvent.KEY_RELEASED) {
			    ui.keyup(ke);
			} else if(ke.getID() == KeyEvent.KEY_TYPED) {
			    ui.type(ke);
			}
		    }
		    ui.lastevent = Utils.rtime();
		}
	    }
	}

	public void register(Component wdg) {
	    wdg.addKeyListener(this);
	    wdg.addMouseListener(this);
	    wdg.addMouseWheelListener(this);
	    wdg.addMouseMotionListener(this);
	}

	public void keyTyped(KeyEvent e) {
	    synchronized(this) {
		events.add(e);
		notifyAll();
	    }
	}
	public void keyPressed(KeyEvent e) {
	    synchronized(this) {
		events.add(e);
		notifyAll();
	    }
	}
	public void keyReleased(KeyEvent e) {
	    synchronized(this) {
		events.add(e);
		notifyAll();
	    }
	}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mouseClicked(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {
	    synchronized(this) {
		events.add(e);
		notifyAll();
	    }
	}
	public void mouseReleased(MouseEvent e) {
	    synchronized(this) {
		events.add(e);
		notifyAll();
	    }
	}
	public void mouseWheelMoved(MouseWheelEvent e) {
	    synchronized(this) {
		events.add(e);
		notifyAll();
	    }
	}
	public void mouseDragged(MouseEvent e) {
	    synchronized(this) {
		mousemv = e;
	    }
	}
	public void mouseMoved(MouseEvent e) {
	    synchronized(this) {
		mousemv = e;
	    }
	}
    }
}
