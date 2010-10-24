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
    public Transform camera;
    
    public PView(Coord c, Coord sz, Widget parent) {
	super(c, sz, parent);
    }
    
    protected abstract void setup(RenderList rls);

    private void transform(GOut g, int i) {
	RenderList.Slot s = rls.list[i];
	if(s.p != -1)
	    transform(g, s.p);
	if(s.t != null)
	    s.t.apply(g);
    }

    protected void render(GOut g) {
	rls.rewind();
	setup(rls);
	GL gl = g.gl;
	for(int i = 0; i < rls.cur; i++) {
	    if(rls.list[i].r == null)
		continue;
	    gl.glPushMatrix();
	    transform(g, i);
	    rls.list[i].r.draw(g);
	    gl.glPopMatrix();
	}
    }

    public void draw(GOut g) {
	if(camera == null)
	    return;
	GL gl = g.gl;
	g.texsel(-1);
	gl.glScissor(g.ul.x, ui.root.sz.y - g.ul.y - g.sz.y, g.sz.x, g.sz.y);
	gl.glViewport(g.ul.x, ui.root.sz.y - g.ul.y - g.sz.y, g.sz.x, g.sz.y);
	gl.glMatrixMode(gl.GL_MODELVIEW);
	gl.glPushMatrix();
	gl.glLoadIdentity();
	gl.glMatrixMode(gl.GL_PROJECTION);
	gl.glPushMatrix();
	gl.glLoadIdentity();
	gl.glEnable(gl.GL_DEPTH_TEST);
	gl.glEnable(gl.GL_CULL_FACE);
	gl.glEnable(gl.GL_SCISSOR_TEST);
	gl.glEnable(gl.GL_LIGHTING);
	gl.glEnable(gl.GL_LIGHT0);
	gl.glEnable(gl.GL_COLOR_MATERIAL);
	gl.glClearDepth(1.0);
	gl.glDepthFunc(gl.GL_LEQUAL);
	gl.glClear(gl.GL_DEPTH_BUFFER_BIT | gl.GL_COLOR_BUFFER_BIT);
	try {
	    checkerr(gl);
	    double r = ((double)sz.y) / ((double)sz.x);
	    gl.glFrustum(-1, 1, -r, r, 1, 500);
	    gl.glMatrixMode(gl.GL_MODELVIEW);
	    camera.apply(g);
	    render(g);
	} finally {
	    gl.glMatrixMode(gl.GL_PROJECTION);
	    gl.glPopMatrix();
	    gl.glMatrixMode(gl.GL_MODELVIEW);
	    gl.glPopMatrix();
	    gl.glDisable(gl.GL_DEPTH_TEST);
	    gl.glDisable(gl.GL_CULL_FACE);
	    gl.glDisable(gl.GL_SCISSOR_TEST);
	    gl.glDisable(gl.GL_LIGHTING);
	    gl.glDisable(gl.GL_LIGHT0);
	    gl.glDisable(gl.GL_COLOR_MATERIAL);
	    gl.glViewport(g.root().ul.x, g.root().ul.y, g.root().sz.x, g.root().sz.y);
	    gl.glScissor(g.root().ul.x, g.root().ul.y, g.root().sz.x, g.root().sz.y);
	}
	checkerr(gl);
    }
}
