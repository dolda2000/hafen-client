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

package haven.iosys.tk;

import java.util.*;
import java.awt.event.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.*;
import haven.*;
import haven.iosys.*;
import haven.render.*;
import haven.render.gl.*;
import haven.render.jogl.*;
import java.awt.EventQueue;
import java.awt.image.BufferedImage;
import com.jogamp.opengl.GL;
import static java.awt.event.KeyEvent.*;

@Toolkit.Available(name = "jogl")
public class JOGLToolkit extends AWTToolkit {
    private final GLCapabilities caps;

    private JOGLToolkit() {
	try {
	    this.caps = mkcaps();
	} catch(ProfileException e) {
	    throw(new Unavailable(e));
	} catch(LinkageError e) {
	    throw(new Unavailable("JOGL libraries not available", e));
	}
    }

    private static Providers.Factory<JOGLToolkit> factory = new Providers.Factory<JOGLToolkit>() {
	private JOGLToolkit instance = null;

	public JOGLToolkit open(String... args) {
	    synchronized(this) {
		if(instance == null)
		    instance = new JOGLToolkit();
	    }
	    return(instance);
	}

	public int priority() {return(-10);}
    };
    public static Providers.Factory<JOGLToolkit> get() {
	return(factory);
    }

    public static class ProfileException extends Environment.UnavailableException {
	public final String availability;

	public ProfileException(Throwable cause) {
	    super("No suitable OpenGL profile is available", cause);
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

    public class JOGLPanel extends GLCanvas {
	public Area shape = Area.sized(Coord.z);
	public PanelEnvironment env;
	private int cursi, pstate = 0;

	public JOGLPanel() {
	    super(caps, null, null);
	    setFocusTraversalKeysEnabled(false);
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

		public void reshape(GLAutoDrawable d, int x, int y, int w, int h) {
		    shape = Area.sized(Coord.of(x, y), Coord.of(w, h));
		}

		public void dispose(GLAutoDrawable d) {
		}
	    });
	}

	private void initgl(GL gl) {
	    Collection<String> exts = Arrays.asList(gl.glGetString(GL.GL_EXTENSIONS).split(" "));
	    GLCapabilitiesImmutable caps = getChosenGLCapabilities();
	    gl.setSwapInterval(cursi = 1);
	    if(exts.contains("GL_ARB_multisample") && caps.getSampleBuffers()) {
		/* Apparently, having sample buffers in the config enables
		 * multisampling by default on some systems. */
		gl.glDisable(GL.GL_MULTISAMPLE);
	    }
	}

	public class PanelEnvironment extends JOGLEnvironment {
	    public PanelEnvironment(GL initgl, GLContext ctx, Area shaoe) {
		super(initgl, ctx, shape);
	    }

	    public void submit(Render cmd) {
		super.submit(cmd);
		synchronized(JOGLPanel.this) {
		    if(pstate == 0)
			EventQueue.invokeLater(JOGLPanel.this::display);
		    pstate |= 1;
		}
	    }

	    public JOGLPanel panel() {
		return(JOGLPanel.this);
	    }
	}

	private void redraw(GL gl) {
	    GLContext ctx = gl.getContext();
	    GLEnvironment env;
	    synchronized(this) {
		if((this.env == null) || (this.env.ctx != ctx)) {
		    if(this.env != null)
			this.env.dispose();
		    this.env = new PanelEnvironment(gl, ctx, shape);
		    notifyAll();
		    initgl(gl);
		}
		env = this.env;
		if(!env.shape().equals(shape))
		    env.reshape(shape);
	    }
	    GL3 gl3 = gl.getGL3();
	    if(false) {
		System.err.println("\n-----\n\n");
		gl3 = new TraceGL3(gl3, System.err);
	    }
	    if(false) {
		gl3 = new DebugGL3(gl3);
	    }
	    synchronized(this) {
		pstate = 2;
	    }
	    env.process(new JOGLWrap(gl3));
	    synchronized(this) {
		if((pstate & 1) != 0)
		    EventQueue.invokeLater(this::display);
		pstate &= ~2;
	    }
	}

	private void glswap(haven.render.gl.GL gl, int ival) {
	    haven.render.gl.GLException.checkfor(gl, null);
	    if(ival != cursi)
		((WrappedJOGL)gl).getGL().setSwapInterval(cursi = ival);
	    swapBuffers();
	    haven.render.gl.GLException.checkfor(gl, null);
	}
    }

    private static final Pipe.Op glfb = Pipe.Op.compose(new FragColor<>(FragColor.defcolor),
							new DepthBuffer<>(DepthBuffer.defdepth));
    public class JOGLWindow extends AWTWindow {
	public final JOGLPanel panel;
	private final EventQueue dsp;

	public JOGLWindow() {
	    panel = new JOGLPanel();
	    frame.add(panel);
	    frame.pack();
	    panel.requestFocus();
	    (dsp = new EventQueue()).register();
	}

	protected JOGLPanel panel() {return(panel);}

	public Environment env() {
	    if(panel.env == null) {
		panel.display();
		try {
		    double st = Utils.rtime(), now = st;
		    synchronized(panel) {
			while((panel.env == null) && ((now - st) < 5)) {
			    panel.wait((int)Math.round(1000 * (5 - (now - st))));
			    now = Utils.rtime();
			}
		    }
		} catch(InterruptedException e) {
		    Thread.currentThread().interrupt();
		}
		if(panel.env == null)
		    throw(new RuntimeException("Did not get GL environment even after display"));
	    }
	    return(panel.env);
	}

	public Pipe.Op fbstate() {
	    return(glfb);
	}

	public void swapbuffers(Render buf, Object mode) {
	    GLRender gbuf = (GLRender)buf;
	    if(((JOGLPanel.PanelEnvironment)gbuf.env).panel() != panel)
		throw(new IllegalArgumentException());
	    if(!(mode instanceof Boolean))
		throw(new IllegalArgumentException());
	    gbuf.submit(gl -> panel.glswap(gl, ((Boolean)mode) ? 1 : 0));
	    java.awt.EventQueue.invokeLater(dsp::process);
	}
    }

    public Windeye window() {
	return(new JOGLWindow());
    }

    public String description() {
	return(String.format("AWT/JOGL, Java %s, JOGL %s", System.getProperty("java.version", ""), JoglVersion.getInstance().getImplementationVersion()));
    }
}
