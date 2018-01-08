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

import javax.media.opengl.*;
import javax.media.opengl.awt.*;
import haven.render.gl.*;

public class JOGLPanel extends GLCanvas implements Runnable, UIPanel {
    public final boolean vsync = true;
    private GLEnvironment env = null;
    private Coord sz;

    private static GLCapabilities mkcaps() {
	GLProfile prof = GLProfile.getDefault();
	GLCapabilities caps = new GLCapabilities(prof);
	caps.setDoubleBuffered(true);
	caps.setAlphaBits(8);
	caps.setRedBits(8);
	caps.setGreenBits(8);
	caps.setBlueBits(8);
	return(caps);
    }

    public JOGLPanel(Coord sz) {
	super(mkcaps(), null, null, null);
	this.sz = sz;
	setSize(sz.x, sz.y);
	addGLEventListener(new GLEventListener() {
		public void display(GLAutoDrawable d) {
		    redraw(d.getGL().getGL2());
		}

		public void init(GLAutoDrawable d) {
		}

		public void reshape(GLAutoDrawable wnd, int x, int y, int w, int h) {
		}

		public void dispose(GLAutoDrawable wnd) {
		}
	    });
    }

    private void redraw(GL2 gl) {
    }

    private void uglyjoglhack() throws InterruptedException {
	try {
	    display();
	} catch(RuntimeException e) {
	    InterruptedException irq = Utils.hascause(e, InterruptedException.class);
	    if(irq != null)
		throw(irq);
	    throw(e);
	}
    }

    private void renderloop() {
	try {
	    uglyjoglhack();
	    if(env == null)
		throw(new RuntimeException("Did not get GL environment even after display"));
	} catch(InterruptedException e) {
	}
    }

    public void run() {
	Thread drawthread = new HackThread(this::renderloop, "Render thread");
	drawthread.start();
	try {
	    try {
		synchronized(this) {
		    while(env == null)
			this.wait();
		}
	    } finally {
		drawthread.interrupt();
		drawthread.join();
	    }
	} catch(InterruptedException e) {
	}
    }

    public UI newui(Session sess) {
	return(null);
    }
}
