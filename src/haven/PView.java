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

import static haven.GOut.checkerr;
import javax.media.opengl.*;

public abstract class PView extends Widget {
    private RenderList rls = new RenderList();
    public static final GLState.Slot<RenderState> proj = new GLState.Slot<RenderState>(RenderState.class, HavenPanel.proj2d);
    public static final GLState.Slot<Camera> cam = new GLState.Slot<Camera>(Camera.class, proj);
    public static final GLState.Slot<Location> loc = new GLState.Slot<Location>(Location.class, cam);
    protected Light.Model lm;
    private GLState pstate;
    
    public class RenderState extends GLState {
	public final float field = 0.5f;
	public final float aspect = ((float)sz.y) / ((float)sz.x);
	public final Matrix4f projmat = new Matrix4f();
	
	public void apply(GOut g) {
	    GL gl = g.gl;
	    gl.glScissor(g.ul.x, ui.root.sz.y - g.ul.y - g.sz.y, g.sz.x, g.sz.y);
	    gl.glViewport(g.ul.x, ui.root.sz.y - g.ul.y - g.sz.y, g.sz.x, g.sz.y);

	    gl.glAlphaFunc(gl.GL_GREATER, 0.5f);
	    gl.glEnable(gl.GL_DEPTH_TEST);
	    gl.glEnable(gl.GL_CULL_FACE);
	    gl.glEnable(gl.GL_SCISSOR_TEST);
	    gl.glEnable(gl.GL_ALPHA_TEST);
	    gl.glDepthFunc(gl.GL_LEQUAL);
	    gl.glClearDepth(1.0);

	    g.st.matmode(GL.GL_PROJECTION);
	    gl.glPushMatrix();
	    gl.glLoadIdentity();
	    gl.glFrustum(-field, field, -aspect * field, aspect * field, 1, 5000);
	    projmat.getgl(gl, GL.GL_PROJECTION_MATRIX);

	    g.st.matmode(gl.GL_MODELVIEW);
	    gl.glPushMatrix();
	    gl.glLoadIdentity();
	}
	
	public void unapply(GOut g) {
	    GL gl = g.gl;

	    g.st.matmode(gl.GL_MODELVIEW);
	    gl.glPopMatrix();

	    g.st.matmode(gl.GL_PROJECTION);
	    gl.glPopMatrix();

	    gl.glDisable(gl.GL_DEPTH_TEST);
	    gl.glDisable(gl.GL_CULL_FACE);
	    gl.glDisable(gl.GL_SCISSOR_TEST);
	    gl.glDisable(gl.GL_ALPHA_TEST);

	    gl.glViewport(g.root().ul.x, g.root().ul.y, g.root().sz.x, g.root().sz.y);
	    gl.glScissor(g.root().ul.x, g.root().ul.y, g.root().sz.x, g.root().sz.y);
	}
	
	public void prep(Buffer b) {
	    b.put(proj, this);
	}
	
	public Coord3f toscreen(Coord3f ec, Coord sz) {
	    float[] o = projmat.mul4(ec.to4a(1));
	    o[0] /= o[3]; o[1] /= o[3];
	    o[0] = ((o[0] + 1) / 2) * sz.x;
	    o[1] = ((-o[1] + 1) / 2) * sz.y;
	    return(new Coord3f(o[0], o[1], o[2]));
	}
    }
    
    public PView(Coord c, Coord sz, Widget parent) {
	super(c, sz, parent);
	pstate = new RenderState();
	lm = new Light.Model();
	lm.cc = GL.GL_SEPARATE_SPECULAR_COLOR;
    }
    
    protected GLState.Buffer basic(GOut g) {
	GLState.Buffer buf = g.st.copy();
	pstate.prep(buf);
	camera().prep(buf);
	return(buf);
    }

    protected abstract Camera camera();
    protected abstract void setup(RenderList rls);
    
    public void resize(Coord sz) {
	super.resize(sz);
	pstate = new RenderState();
    }

    private final Rendered scene = new Rendered() {
	    public void draw(GOut g) {
	    }
	    
	    public Order setup(RenderList rl) {
		PView.this.setup(rl);
		return(null);
	    }
	};

    public void draw(GOut g) {
	GLState.Buffer bk = g.st.copy();
	GLState.Buffer def = basic(g);
	try {
	    lm.prep(def);
	    new Light.LightList().prep(def);
	    Light.elights.prep(def);
	    rls.setup(scene, def);
	    g.st.set(def);
	    g.apply();
	    GL gl = g.gl;
	    gl.glClear(gl.GL_DEPTH_BUFFER_BIT | gl.GL_COLOR_BUFFER_BIT);
	    rls.render(g);
	} finally {
	    g.st.set(bk);
	}
    }
}
