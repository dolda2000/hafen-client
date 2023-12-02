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
import java.awt.AWTException;
import java.awt.EventQueue;
import java.awt.Robot;
import java.awt.Point;
import org.lwjgl.opengl.awt.*;
import haven.render.*;
import haven.render.States;
import haven.render.gl.*;
import haven.render.lwjgl.*;
import haven.JOGLPanel.SyncMode;

public class LWJGLPanel extends AWTGLCanvas implements GLPanel, Console.Directory {
    private static final int[][] glversions = {
	{4, 6}, {4, 5}, {4, 4}, {4, 3}, {4, 2}, {4, 1}, {4, 0},
	{3, 3}, {3, 2},
    };
    private static final boolean dumpbgl = true;
    private LWJGLEnvironment env = null;
    private boolean aswap;
    private Area shape;
    private Pipe base, wnd;
    private final Loop main = new Loop(this);

    public LWJGLPanel() {
	super();
	base = new BufPipe();
	base.prep(new FragColor<>(FragColor.defcolor)).prep(new DepthBuffer<>(DepthBuffer.defdepth));
	base.prep(FragColor.blend(new BlendMode()));
	setFocusTraversalKeysEnabled(false);
	newui(null);
    }

    public void initGL() {}
    public void paintGL() {}

    protected ContextData createContext() throws AWTException {
	for(int[] ver : glversions) {
	    GLData caps = new GLData();
	    caps.majorVersion = ver[0];
	    caps.minorVersion = ver[1];
	    caps.profile = GLData.Profile.CORE;
	    try {
		return(createContext(caps));
	    } catch(AWTException e) {
		/* Try next */
	    }
	}
	/* Try to get whatever and see if LWJGLEnvironment considers
	 * that to pass muster. */
	return(createContext(new GLData()));
    }

    private final haven.error.ErrorHandler errh = haven.error.ErrorHandler.find();
    private void setenv(LWJGLEnvironment env) {
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

    public GLEnvironment env() {return(env);}
    public Area shape() {return(shape);}
    public Pipe basestate() {return(wnd);}

    private boolean iswap() {
	return(main.ui.gprefs.vsync.val);
    }

    public void glswap(GL gl) {
	boolean iswap = iswap();
	if(main.gldebug)
	    GLException.checkfor(gl, null);
	if(iswap != aswap)
	    setSwapInterval((aswap = iswap) ? 1 : 0);
	swapBuffers();
	if(main.gldebug)
	    GLException.checkfor(gl, null);
    }

    private void reshape(Area shape) {
	this.shape = shape;
	this.wnd = base.copy().prep(new States.Viewport(shape)).prep(new Ortho2D(shape));
    }

    private void initgl() {
	setSwapInterval((aswap = iswap()) ? 1 : 0);
    }

    private void awtrun(Runnable task) throws InterruptedException {
	try {
	    EventQueue.invokeAndWait(task);
	} catch(java.lang.reflect.InvocationTargetException e) {
	    if(e.getCause() instanceof RuntimeException)
		throw((RuntimeException)e.getCause());
	    throw(new RuntimeException(e));
	}
    }

    private void glrun(Runnable task) throws InterruptedException {
	awtrun(() -> runInContext(task));
    }

    private void renderloop() {
	reshape(Area.sized(Coord.of(getWidth(), getHeight())));
	try {
	    glrun(() -> {
		    org.lwjgl.opengl.GL.createCapabilities();
		    synchronized(this) {
			setenv(new LWJGLEnvironment(this.shape));
			initgl();
			notifyAll();
		    }
		});
	    while(true) {
		long wst = System.nanoTime();
		env.submitwait();
		main.ridletime += System.nanoTime() - wst;

		Area shape = Area.sized(Coord.of(getWidth(), getHeight()));
		if(!env.shape().equals(shape)) {
		    this.reshape(shape);
		    env.reshape(shape);
		}
		glrun(() -> {
			try {
			    env.process(LWJGLWrap.instance);
			} catch(BGL.BGLException e) {
			    if(dumpbgl)
				e.dump.dump();
			    throw(e);
			}
		    });
	    }
	} catch(InterruptedException e) {
	}
    }

    public void run() {
	Thread drawthread = new HackThread(LWJGLPanel.this::renderloop, "Render thread");
	drawthread.start();
	try {
	    try {
		synchronized(LWJGLPanel.this) {
		    while(env == null)
			LWJGLPanel.this.wait();
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
		cons.out.printf("Rendering backend: LWJGL %s\n", org.lwjgl.Version.getVersion());
		if(env != null) {
		    GLEnvironment.Caps caps = env.caps();
		    cons.out.printf("Rendering device: %s, %s\n", caps.vendor(), caps.device());
		    cons.out.printf("Driver version: %s\n", caps.driver());
		}
	    });
    }
    public Map<String, Console.Command> findcmds() {
	return(cmdmap);
    }
}
