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
import javax.media.opengl.*;

public class GBuffer {
    private static final Object dmon = new Object();
    public final GLCapabilitiesImmutable caps;
    public final Coord sz;
    private final GLAutoDrawable buf;
    private GLConfig glconf;
    private GLState gstate, ostate;
    private GLState.Applier state;

    public GBuffer(Coord sz) {
	this.sz = sz;
	GLProfile prof = GLProfile.getDefault();
	GLDrawableFactory df = GLDrawableFactory.getFactory(prof);
	/* XXX: This seems a bit unreliable. On Xorg with nvidia
	 * drivers, an OffscreenAutoDrawable produces no results,
	 * while a Pbuffer works; while on Xvfb with mesa-swx, an
	 * OffscreenAutoDrawable works, while Pbuffer creation
	 * fails. :-/ */
	buf = df.createOffscreenAutoDrawable(null, caps(prof), null, sz.x, sz.y, null);
	caps = buf.getChosenGLCapabilities();
	buf.addGLEventListener(new GLEventListener() {
		public void display(GLAutoDrawable d) {
		    GL2 gl = d.getGL().getGL2();
		    /* gl = new TraceGL2(gl, System.err) */
		    redraw(gl);
		    GLObject.disposeall(gl);
		}

		public void init(GLAutoDrawable d) {
		    GL2 gl = d.getGL().getGL2();
		    glconf = GLConfig.fromgl(gl, d.getContext(), caps);
		    glconf.pref = GLSettings.defconf(glconf);
		    gstate = new GLState() {
			    public void apply(GOut g) {
				GL2 gl = g.gl;
				gl.glColor3f(1, 1, 1);
				gl.glPointSize(4);
				gl.setSwapInterval(1);
				gl.glEnable(GL.GL_BLEND);
				gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
				if(g.gc.havefsaa())
				    g.gl.glDisable(GL.GL_MULTISAMPLE);
				GOut.checkerr(gl);
			    }
			    public void unapply(GOut g) {
			    }
			    public void prep(Buffer buf) {
				buf.put(HavenPanel.global, this);
			    }
			};
		}

		public void reshape(GLAutoDrawable d, final int x, final int y, final int w, final int h) {
		    ostate = new GLState() {
			    public void apply(GOut g) {
				GL2 gl = g.gl;
				g.st.matmode(GL2.GL_PROJECTION);
				gl.glLoadIdentity();
				gl.glOrtho(0, w, h, 0, -1, 1);
			    }
			    public void unapply(GOut g) {
			    }
			    public void prep(Buffer buf) {
				buf.put(HavenPanel.proj2d, this);
			    }
			};
		}

		public void dispose(GLAutoDrawable d) {
		    GL2 gl = d.getGL().getGL2();
		    GLObject.disposeall(gl);
		}
	    });
    }

    protected GLCapabilities caps(GLProfile prof) {
	GLCapabilities ret = new GLCapabilities(prof);
	ret.setDoubleBuffered(true);
	ret.setAlphaBits(8);
	ret.setRedBits(8);
	ret.setGreenBits(8);
	ret.setBlueBits(8);
	ret.setSampleBuffers(true);
	ret.setNumSamples(4);
	return(ret);
    }

    private Drawn curdraw;

    protected void redraw(GL2 gl) {
	if((state == null) || (state.gl != gl))
	    state = new GLState.Applier(gl, glconf);
	GLState.Buffer ibuf = new GLState.Buffer(glconf);
	gstate.prep(ibuf);
	ostate.prep(ibuf);
	GOut g = new GOut(gl, buf.getContext(), glconf, state, ibuf, sz);
	g.apply();
	gl.glClearColor(0, 0, 0, 0);
	gl.glClear(GL.GL_COLOR_BUFFER_BIT);
	curdraw.draw(g);
	state.clean();
    }

    public void render(Drawn thing) {
	synchronized(dmon) {
	    curdraw = thing;
	    try {
		buf.display();
	    } finally {
		curdraw = null;
	    }
	}
    }

    public void dispose() {
	buf.destroy();
    }

    public static void main(String[] args) {
	GBuffer test = new GBuffer(new Coord(250, 250));
	test.render(new Drawn() {
		public void draw(GOut g) {
		    g.chcolor(255, 0, 128, 255);
		    g.frect(new Coord(50, 50), new Coord(100, 100));
		    try {
			javax.imageio.ImageIO.write(g.getimage(), "PNG", new java.io.File("/tmp/bard.png"));
		    } catch(java.io.IOException e) {
			throw(new RuntimeException(e));
		    }
		    g.checkerr(g.gl);
		}
	    });
	
    }
}
