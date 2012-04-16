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

public class FBView {
    public final GLFrameBuffer fbo;
    public RenderList rls;
    public GLState basicstate;
    private final PView.RenderState rstate = new RenderState();
    
    private class RenderState extends PView.RenderState {
	public Coord ul() {
	    return(Coord.z);
	}
	
	public Coord sz() {
	    return(fbo.sz());
	}
    }
    
    public FBView(GLFrameBuffer fbo, GLState basic) {
	this.fbo = fbo;
	this.basicstate = basic;
    }
    
    protected GLState.Buffer basic(GOut g) {
	GLState.Buffer buf = g.basicstate();
	rstate.prep(buf);
	if(basicstate != null)
	    basicstate.prep(buf);
	return(buf);
    }

    protected void clear(GOut g) {
	g.gl.glClearColor(0, 0, 0, 0);
	g.gl.glClear(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT);
    }

    public void render(Rendered root, GOut ctx) {
	if((rls == null) || (rls.cfg != ctx.gc))
	    rls = new RenderList(ctx.gc);
	GOut g = gderive(ctx);
	GLState.Buffer bk = g.st.copy();
	try {
	    GLState.Buffer def = basic(g);
	    rls.setup(root, def);
	    rls.fin();
	    g.st.set(def);
	    g.apply();
	    clear(g);
	    rls.render(g);
	} finally {
	    g.st.set(bk);
	}
    }
    
    public GOut gderive(GOut orig) {
	GLState.Buffer def = orig.basicstate();
	fbo.prep(def);
	return(new GOut(orig.gl, orig.ctx, orig.gc, orig.st, def, fbo.sz()));
    }
    
    public void dispose() {
	fbo.dispose();
    }
}
