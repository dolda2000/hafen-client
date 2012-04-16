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
    private RenderList rls;
    public static final GLState.Slot<RenderState> wnd = new GLState.Slot<RenderState>(GLState.Slot.Type.SYS, RenderState.class, HavenPanel.proj2d);
    public static final GLState.Slot<Projection> proj = new GLState.Slot<Projection>(GLState.Slot.Type.SYS, Projection.class, wnd);
    public static final GLState.Slot<Camera> cam = new GLState.Slot<Camera>(GLState.Slot.Type.SYS, Camera.class, proj);
    public static final GLState.Slot<Location> loc = new GLState.Slot<Location>(GLState.Slot.Type.GEOM, Location.class, cam);
    public Profile prof = new Profile(300);
    protected Light.Model lm;
    private GLState rstate, pstate;
    
    public static abstract class RenderState extends GLState {
	public void apply(GOut g) {
	    GL gl = g.gl;
	    gl.glScissor(g.ul.x, g.root().sz.y - g.ul.y - g.sz.y, g.sz.x, g.sz.y);
	    Coord ul = ul();
	    Coord sz = sz();
	    gl.glViewport(ul.x, g.root().sz.y - ul.y - sz.y, sz.x, sz.y);

	    gl.glAlphaFunc(gl.GL_GREATER, 0.5f);
	    gl.glEnable(gl.GL_DEPTH_TEST);
	    gl.glEnable(gl.GL_CULL_FACE);
	    gl.glEnable(gl.GL_SCISSOR_TEST);
	    gl.glDepthFunc(gl.GL_LEQUAL);
	    gl.glClearDepth(1.0);
	}
	
	public void unapply(GOut g) {
	    GL gl = g.gl;

	    gl.glDisable(gl.GL_DEPTH_TEST);
	    gl.glDisable(gl.GL_CULL_FACE);
	    gl.glDisable(gl.GL_SCISSOR_TEST);

	    gl.glViewport(g.root().ul.x, g.root().ul.y, g.root().sz.x, g.root().sz.y);
	    gl.glScissor(g.root().ul.x, g.root().ul.y, g.root().sz.x, g.root().sz.y);
	}
	
	public void prep(Buffer b) {
	    b.put(wnd, this);
	}
	
	public abstract Coord ul();
	public abstract Coord sz();
    }
    
    private class WidgetRenderState extends RenderState {
	public Coord ul() {
	    return(rootpos());
	}
	
	public Coord sz() {
	    return(PView.this.sz);
	}
    }
    
    public PView(Coord c, Coord sz, Widget parent) {
	super(c, sz, parent);
	rstate = new WidgetRenderState();
	pstate = makeproj();
	lm = new Light.Model();
	lm.cc = GL.GL_SEPARATE_SPECULAR_COLOR;
    }
    
    protected GLState.Buffer basic(GOut g) {
	GLState.Buffer buf = g.st.copy();
	rstate.prep(buf);
	pstate.prep(buf);
	camera().prep(buf);
	return(buf);
    }

    protected abstract Camera camera();
    protected abstract void setup(RenderList rls);
    
    protected Projection makeproj() {
	float field = 0.5f;
	float aspect = ((float)sz.y) / ((float)sz.x);
	return(Projection.frustum(-field, field, -aspect * field, aspect * field, 1, 5000));
    }

    public void resize(Coord sz) {
	super.resize(sz);
	pstate = makeproj();
    }

    private final Rendered scene = new Rendered() {
	    public void draw(GOut g) {
	    }
	    
	    public boolean setup(RenderList rl) {
		PView.this.setup(rl);
		return(false);
	    }
	};

    public void draw(GOut g) {
	if((g.sz.x < 1) || (g.sz.y < 1))
	    return;
	if((rls == null) || (rls.cfg != g.gc))
	    rls = new RenderList(g.gc);
	Profile.Frame curf = null;
	if(Config.profile)
	    curf = prof.new Frame();
	GLState.Buffer bk = g.st.copy();
	GLState.Buffer def = basic(g);
	if(g.gc.fsaa)
	    States.fsaa.prep(def);
	try {
	    lm.prep(def);
	    new Light.LightList().prep(def);
	    rls.setup(scene, def);
	    if(curf != null)
		curf.tick("setup");
	    rls.fin();
	    if(curf != null)
		curf.tick("sort");
	    g.st.set(def);
	    g.apply();
	    if(curf != null)
		curf.tick("cls");
	    GL gl = g.gl;
	    gl.glClear(gl.GL_DEPTH_BUFFER_BIT | gl.GL_COLOR_BUFFER_BIT);
	    g.st.time = 0;
	    rls.render(g);
	    if(curf != null) {
		curf.add("apply", g.st.time);
		curf.tick("render", g.st.time);
	    }
	} finally {
	    g.st.set(bk);
	}
	for(RenderList.Slot s : rls.slots()) {
	    if(s.r instanceof Render2D)
		((Render2D)s.r).draw2d(g);
	}
	if(curf != null)
	    curf.tick("2d");
	if(curf != null)
	    curf.fin();
    }
    
    public interface Render2D extends Rendered {
	public void draw2d(GOut g);
    }
    
    public static abstract class Draw2D implements Render2D {
	public void draw(GOut g) {}
	
	public boolean setup(RenderList r) {
	    return(false);
	}
    }
}
