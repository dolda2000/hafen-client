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
import javax.media.opengl.*;
import javax.media.opengl.awt.*;
import haven.render.*;
import haven.render.States;
import haven.render.gl.*;
import haven.render.gl.BufferBGL; // XXX: Remove me

public class JOGLPanel extends GLCanvas implements Runnable, UIPanel {
    public final boolean vsync = true;
    private GLEnvironment env = null;
    private UI ui;
    private Pipe base, wnd;

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
	base = new Pipe();
	base.prep(new FragColor(FragColor.defcolor)).prep(new DepthBuffer(DepthBuffer.defdepth));
	base.prep(new States.Blending());
	setSize(sz.x, sz.y);
	addGLEventListener(new GLEventListener() {
		public void display(GLAutoDrawable d) {
		    redraw(d.getGL().getGL2());
		}

		public void init(GLAutoDrawable d) {
		}

		public void reshape(GLAutoDrawable wdg, int x, int y, int w, int h) {
		    Area area = Area.sized(new Coord(x, y), new Coord(w, h));
		    wnd = base.copy();
		    wnd.prep(new States.Viewport(area)).prep(new Ortho2D(area));
		}

		public void dispose(GLAutoDrawable wdg) {
		}
	    });
    }

    private static class Frame {
	GLRender buf;
	GLEnvironment env;
	BufferBGL dispose;

	Frame(GLRender buf, GLEnvironment env, BufferBGL dispose) {
	    this.buf = buf;
	    this.env = env;
	    this.dispose = dispose;
	}
    }

    private final Frame[] curdraw = {null};

    private void initgl(GL2 gl) {
	Collection<String> exts = Arrays.asList(gl.glGetString(GL.GL_EXTENSIONS).split(" "));
	GLCapabilitiesImmutable caps = getChosenGLCapabilities();
	gl.setSwapInterval(1);
	if(exts.contains("GL_ARB_multisample") && caps.getSampleBuffers()) {
	    /* Apparently, having sample buffers in the config enables
	     * multisampling by default on some systems. */
	    gl.glDisable(GL.GL_MULTISAMPLE);
	}
    }

    private void redraw(GL2 gl) {
	if(false) {
	    System.err.print("\n-----\n\n");
	    gl = new TraceGL2(gl, System.err);
	}
	GLContext ctx = gl.getContext();
	GLEnvironment env;
	synchronized(this) {
	    if((this.env == null) || (this.env.ctx != ctx)) {
		this.env = new GLEnvironment(ctx);
		initgl(gl);
	    }
	    env = this.env;
	}
	Frame f;
	synchronized(curdraw) {
	    f = curdraw[0];
	    curdraw[0] = null;
	    curdraw.notifyAll();
	}
	if(f != null) {
	    if(f.env == env) {
		env.submit(gl, f.buf);
		f.dispose.run(gl);
	    } else {
		f.buf.dispose();
	    }
	}
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
	    synchronized(this) {
		if(env == null)
		    throw(new RuntimeException("Did not get GL environment even after display"));
		notifyAll();
	    }
	    while(true) {
		uglyjoglhack();
	    }
	} catch(InterruptedException e) {
	}
    }

    private void display(GLRender buf) {
	buf.clear(wnd, FragColor.fragcol, FColor.BLACK);
	GOut g = new GOut(buf, wnd.copy(), new Coord(getSize()));;
	synchronized(ui) {
	    ui.draw(g);
	}
    }

    public void run() {
	Thread drawthread = new HackThread(this::renderloop, "Render thread");
	drawthread.start();
	try {
	    try {
		synchronized(this) {
		    while(this.env == null)
			this.wait();
		}
		while(true) {
		    GLEnvironment env = this.env;
		    GLRender buf = env.render();
		    synchronized(curdraw) {
			while(curdraw[0] != null)
			    curdraw.wait();
		    }
		    display(buf);
		    BufferBGL dispose = env.disposeall();
		    synchronized(curdraw) {
			if(curdraw[0] != null)
			    throw(new AssertionError());
			curdraw[0] = new Frame(buf, env, dispose);
			curdraw.notifyAll();
		    }
		}
	    } finally {
		drawthread.interrupt();
		drawthread.join();
	    }
	} catch(InterruptedException e) {
	}
    }

    public UI newui(Session sess) {
	if(ui != null)
	    ui.destroy();
	ui = new UI(new Coord(getSize()), sess);
	return(ui);
    }

    public void background(boolean bg) {
    }
}
