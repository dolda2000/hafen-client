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
import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.event.*;

public interface UIPanel extends Runnable {
    public static final Cursor emptycurs = Toolkit.getDefaultToolkit().createCustomCursor(TexI.mkbuf(new Coord(1, 1)), new java.awt.Point(), "");

    public UI newui(UI.Runner fun);
    public void background(boolean bg);

    /* Stuff that is inherited from AWT components, but that have to
     * be declared since Java is stupid. */
    public void setSize(int w, int h);
    public Dimension getSize();

    public static class Dispatcher implements KeyListener, MouseListener, MouseWheelListener, MouseMotionListener {
	public final Queue<InputEvent> events = new LinkedList<>();
	public MouseEvent mousemv = null;
	private KeyEvent lastpress = null;

	public void dispatch(UI ui) {
	    Collection<InputEvent> copy;
	    MouseEvent mousemv;
	    synchronized(this) {
		mousemv = this.mousemv;
		this.mousemv = null;
		copy = new ArrayList<>(events);
		events.clear();
	    }
	    if(mousemv != null) {
		Coord pos = new Coord(mousemv.getX(), mousemv.getY());
		ui.mousemove(mousemv, pos);
	    }
	    for(InputEvent e : copy) {
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
			InputEvent ne = events.peek();
			if(ne instanceof KeyEvent) {
			    /* This is an extension of the below hack
			     * to handle dead keys (on Windows). It's
			     * extremely ugly and error-prone and
			     * should be dealt with, but I've no idea
			     * what the alternative would be.*/
			    KeyEvent nke = (KeyEvent)ne;
			    if((nke.getID() == KeyEvent.KEY_TYPED) && (nke.getWhen() == ke.getWhen())) {
				ke.setKeyChar(nke.getKeyChar());
				events.remove();
			    }
			}
			ui.keydown(ke);
			lastpress = ke;
			if(ke.getKeyCode() == Debug.FRAME_DEBUG_KEY)
			    Debug.fdk = true;
		    } else if(ke.getID() == KeyEvent.KEY_RELEASED) {
			ui.keyup(ke);
			if(ke.getKeyCode() == Debug.FRAME_DEBUG_KEY)
			    Debug.fdk = false;
		    } else if(ke.getID() == KeyEvent.KEY_TYPED) {
			KeyEvent lp = lastpress;
			if((lp != null) && (lp.getKeyChar() == ke.getKeyChar())) {
			    /* Squelch this event. It certainly is an
			     * ugly hack, but I just haven't found any
			     * other way to disambiguate these
			     * duplicate events. Also, apparently
			     * getWhen() cannot be completely trusted
			     * to have the same value for a
			     * KEY_PRESSED and corresponding KEY_TYPED
			     * event.*/
			} else {
			    ui.keydown(ke);
			}
		    }
		}
		ui.lastevent = Utils.rtime();
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

    public static Cursor makeawtcurs(BufferedImage img, Coord hs) {
	java.awt.Dimension cd = Toolkit.getDefaultToolkit().getBestCursorSize(img.getWidth(), img.getHeight());
	BufferedImage buf = TexI.mkbuf(new Coord((int)cd.getWidth(), (int)cd.getHeight()));
	java.awt.Graphics g = buf.getGraphics();
	g.drawImage(img, 0, 0, null);
	g.dispose();
	return(Toolkit.getDefaultToolkit().createCustomCursor(buf, new java.awt.Point(hs.x, hs.y), ""));
    }
}
