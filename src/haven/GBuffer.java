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

import java.awt.Color;
import javax.media.opengl.*;

public class GBuffer {
    public final Coord sz;
    public final TexGL buf;
    public final GLFrameBuffer fbo;
    private final GLState ostate;

    public GBuffer(Coord sz) {
	this.sz = sz;
	buf = new TexE(sz, GL.GL_RGBA, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE);
	fbo = new GLFrameBuffer(buf, null);
	ostate = HavenPanel.OrthoState.fixed(sz);
    }

    public void clear(GOut g, Color col) {
	g.state2d();
	g.apply();
	g.gl.glClearColor(col.getRed() / 255f, col.getGreen() / 255f,
			  col.getBlue() / 255f, col.getAlpha() / 255f);
	g.gl.glClear(GL.GL_COLOR_BUFFER_BIT);
    }

    public GOut graphics(GOut from, GLState extra) {
	GLState.Buffer basic = from.basicstate();
	if(extra != null)
	    extra.prep(basic);
	ostate.prep(basic);
	fbo.prep(basic);
	return(new GOut(from.gl, from.curgl, from.gc, from.st, basic, sz));
    }

    public GOut graphics(GOut from) {
	return(graphics(from, null));
    }

    public void dispose() {
	fbo.dispose();
	buf.dispose();
    }
}
