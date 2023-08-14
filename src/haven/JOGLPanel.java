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
import java.awt.Toolkit;
import java.awt.Robot;
import java.awt.Point;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.*;
import haven.render.*;
import haven.render.States;
import haven.render.gl.*;
import haven.render.jogl.*;
import com.jogamp.opengl.GL;

public class JOGLPanel extends GLCanvas implements GLPanel, Console.Directory {
    private static final boolean dumpbgl = true;
    public boolean aswap;
    private JOGLEnvironment env = null;
    private Area shape;
    private Pipe base, wnd;
    private final Loop main = new Loop(this);

    public static class ProfileException extends Environment.UnavailableException {
	public final String availability;

	public ProfileException(Throwable cause) {
	    super("No OpenGL suitable profile is available", cause);
	    String a;
	    try {
		a = GLProfile.glAvailabilityToString();
	    } catch(Throwable t) {
		a = String.valueOf(t);
	    }
	    this.availability = a;
	}
    }

    private static GLCapabilities mkcaps() {
	GLProfile prof;
	try {
	    prof = GLProfile.getMaxProgrammableCore(true);
	} catch(com.jogamp.opengl.GLException e) {
	    try {
		/* If not core, let GLEnvironment handle that. */
		prof = GLProfile.getDefault();
	    } catch(com.jogamp.opengl.GLException e2) {
		e2.addSuppressed(e);
		throw(new ProfileException(e2));
	    }
	}
	GLCapabilities caps = new GLCapabilities(prof);
	caps.setDoubleBuffered(true);
	caps.setAlphaBits(8);
	caps.setRedBits(8);
	caps.setGreenBits(8);
	caps.setBlueBits(8);
	return(caps);
    }

    public JOGLPanel() {
	super(mkcaps(), null, null);
	base = new BufPipe();
	base.prep(new FragColor<>(FragColor.defcolor)).prep(new DepthBuffer<>(DepthBuffer.defdepth));
	base.prep(FragColor.blend(new BlendMode()));
	addGLEventListener(new GLEventListener() {
		public void display(GLAutoDrawable d) {
		    redraw(d.getGL());
		}

		public void init(GLAutoDrawable d) {
		    setAutoSwapBufferMode(false);
		    /* XXX: This apparently fixes a scaling problem on
		     * OSX, and doesn't seem to have any effect on
		     * other platforms. It seems like a weird
		     * workaround, and I do wonder if there isn't some
		     * underlying bug in JOGL instead, but it hasn't
		     * broken anything yet, so I guess why not. */
		    setSurfaceScale(new float[] {1, 1});
		}

		public void reshape(GLAutoDrawable wdg, int x, int y, int w, int h) {
		    Area area = Area.sized(new Coord(x, y), new Coord(w, h));
		    shape = area;
		    wnd = base.copy();
		    wnd.prep(new States.Viewport(area)).prep(new Ortho2D(area));
		}

		public void dispose(GLAutoDrawable wdg) {
		}
	    });
	setFocusTraversalKeysEnabled(false);
	newui(null);
    }

    private boolean iswap() {
	return(main.ui.gprefs.vsync.val);
    }

    private void initgl(GL gl) {
	Collection<String> exts = Arrays.asList(gl.glGetString(GL.GL_EXTENSIONS).split(" "));
	GLCapabilitiesImmutable caps = getChosenGLCapabilities();
	gl.setSwapInterval((aswap = iswap()) ? 1 : 0);
	if(exts.contains("GL_ARB_multisample") && caps.getSampleBuffers()) {
	    /* Apparently, having sample buffers in the config enables
	     * multisampling by default on some systems. */
	    gl.glDisable(GL.GL_MULTISAMPLE);
	}
    }

    private final haven.error.ErrorHandler errh = haven.error.ErrorHandler.find();
    private void setenv(JOGLEnvironment env) {
	if(this.env != null)
	    this.env.dispose();
	this.env = env;
	if(main.ui != null)
	    main.ui.env = env;

	if(errh != null) {
	    GLEnvironment.Caps caps = env.caps();
	    errh.lsetprop("gl.vendor", caps.vendor);
	    errh.lsetprop("gl.version", caps.version);
	    errh.lsetprop("gl.renderer", caps.renderer);
	    errh.lsetprop("render.caps", caps);
	}
    }

    private void redraw(GL gl) {
	GLContext ctx = gl.getContext();
	GLEnvironment env;
	synchronized(this) {
	    if((this.env == null) || (this.env.ctx != ctx)) {
		setenv(new JOGLEnvironment(gl, ctx, shape));
		initgl(gl);
	    }
	    env = this.env;
	    if(!env.shape().equals(shape))
		env.reshape(shape);
	}
	GL3 gl3 = gl.getGL3();
	try {
	    if(false) {
		System.err.println("\n-----\n\n");
		gl3 = new TraceGL3(gl3, System.err);
	    }
	    if(main.gldebug) {
		gl3 = new DebugGL3(gl3);
	    }
	    env.process(new JOGLWrap(gl3));
	    long end = System.nanoTime();
	} catch(BGL.BGLException e) {
	    if(dumpbgl)
		e.dump.dump();
	    Utils.setprefb("glcrash", true);
	    throw(e);
	}
    }

    /* XXX: Should be in GLPanel, but since GSettings use
     * serialization to save itself, it can't be moved (without
     * breaking existing settings). */
    public static enum SyncMode {
	FRAME, TICK, SEQ, FINISH
    }

    public GLEnvironment env() {return(env);}
    public Area shape() {return(shape);}
    public Pipe basestate() {return(wnd);}

    public void glswap(haven.render.gl.GL gl) {
	boolean iswap = iswap();
	if(main.gldebug)
	    haven.render.gl.GLException.checkfor(gl, null);
	if(iswap != aswap)
	    ((WrappedJOGL)gl).getGL().setSwapInterval((aswap = iswap) ? 1 : 0);
	if(main.gldebug)
	    haven.render.gl.GLException.checkfor(gl, null);
	JOGLPanel.this.swapBuffers();
	if(main.gldebug)
	    haven.render.gl.GLException.checkfor(gl, null);
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
		long wst = System.nanoTime();
		env.submitwait();
		main.ridletime += System.nanoTime() - wst;
		uglyjoglhack();
	    }
	} catch(InterruptedException e) {
	}
    }

    public void run() {
	Thread drawthread = new HackThread(JOGLPanel.this::renderloop, "Render thread");
	drawthread.start();
	try {
	    try {
		synchronized(JOGLPanel.this) {
		    while(env == null)
			JOGLPanel.this.wait();
		}
		main.run();
	    } finally {
		drawthread.interrupt();
		drawthread.join();
	    }
	} catch(InterruptedException e) {
	}
    }

    public UI newui(UI.Runner fun) {
	return(main.newui(fun));
    }

    public void background(boolean bg) {
	main.bgmode = bg;
    }

    private Robot awtrobot;
    public void setmousepos(Coord c) {
	java.awt.EventQueue.invokeLater(() -> {
		if(awtrobot == null) {
		    try {
			awtrobot = new Robot(getGraphicsConfiguration().getDevice());
		    } catch(java.awt.AWTException e) {
			return;
		    }
		}
		Point rp = getLocationOnScreen();
		awtrobot.mouseMove(rp.x + c.x, rp.y + c.y);
	    });
    }

    private Map<String, Console.Command> cmdmap = new TreeMap<String, Console.Command>();
    {
	cmdmap.put("renderer", (cons, args) -> {
		cons.out.printf("Rendering backend: JOGL %s\n", JoglVersion.getInstance().getImplementationVersion());
		if(env != null) {
		    GLEnvironment.Caps caps = env.caps();
		    cons.out.printf("Rendering device: %s, %s\n", caps.vendor(), caps.device());
		    cons.out.printf("Driver version: %s\n", caps.driver());
		}
	    });
	cmdmap.put("glcrash", (cons, args) -> {
		GL gl = getGL();
		new HackThread(() -> {
			try {
			    while(true) {
				env.submitwait();
				redraw(gl);
			    }
			} catch(InterruptedException e) {
			}},
		    "GL crasher").start();
	    });
    }
    public Map<String, Console.Command> findcmds() {
	return(cmdmap);
    }
}
