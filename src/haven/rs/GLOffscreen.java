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

package haven.rs;

import haven.*;
import haven.render.*;
import haven.render.gl.*;
import com.jogamp.opengl.*;

public class GLOffscreen implements Context {
    public final GLProfile prof;
    public final GLAutoDrawable buf;
    private final Object dmon = new Object();
    private GLEnvironment benv = null;
    private final Environment penv;

    public GLOffscreen() {
	prof = GLProfile.getMaxProgrammableCore(true);
	GLDrawableFactory df = GLDrawableFactory.getFactory(prof);
	this.buf = df.createOffscreenAutoDrawable(null, caps(prof), null, 1, 1);
	buf.addGLEventListener(new GLEventListener() {
		public void display(GLAutoDrawable d) {
		    redraw(d.getGL().getGL3());
		}

		public void init(GLAutoDrawable d) {
		}

		public void reshape(GLAutoDrawable d, int x, int y, int w, int h) {
		}

		public void dispose(GLAutoDrawable d) {
		}
	    });
	buf.display();
	if(benv == null)
	    throw(new AssertionError("offscreen display call was not honored"));
	penv = new ProxyEnv();
    }

    private class ProxyEnv extends Environment.Proxy {
	public Environment back() {return(benv);}

	public void submit(Render r) {
	    super.submit(r);
	    synchronized(dmon) {
		buf.display();
	    }
	}
    }

    protected GLCapabilities caps(GLProfile prof) {
	GLCapabilities ret = new GLCapabilities(prof);
	ret.setDoubleBuffered(true);
	ret.setAlphaBits(8);
	ret.setRedBits(8);
	ret.setGreenBits(8);
	ret.setBlueBits(8);
	return(ret);
    }

    private void redraw(GL3 gl) {
	// gl = new TraceGL3(gl, System.err);
	GLContext ctx = gl.getContext();
	if(benv == null)
	    benv = new GLEnvironment(gl, ctx, Area.sized(Coord.z, new Coord(1, 1)));
	if(benv.ctx != ctx)
	    throw(new AssertionError());
	benv.process(gl);
	benv.finish(gl);
    }

    public Environment env() {
	return(penv);
    }

    public void dispose() {
    }

    private static GLOffscreen defctx = null;
    public static GLOffscreen get() {
	if(defctx == null) {
	    synchronized(GLOffscreen.class) {
		if(defctx == null)
		    defctx = new GLOffscreen();
	    }
	}
	return(defctx);
    }
}
