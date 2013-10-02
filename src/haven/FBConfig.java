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

public class FBConfig {
    public Coord sz;
    public boolean hdr, tdepth;
    public GLFrameBuffer fb;
    public PView.RenderState wnd;
    public TexGL color, depth;

    public FBConfig(Coord sz) {
	this.sz = sz;
    }

    public boolean cleanp() {
	return(!hdr && !tdepth);
    }

    private void create() {
	TexGL color, depth;
	if(hdr) {
	    color = new TexE(sz, GL.GL_RGBA16F, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE);
	} else {
	    color = new TexE(sz, GL.GL_RGBA, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE);
	}
	if(tdepth) {
	    depth = new TexE(sz, GL2.GL_DEPTH_COMPONENT, GL2.GL_DEPTH_COMPONENT, GL.GL_UNSIGNED_INT);
	} else {
	    depth = null;
	}
	this.color = color;
	this.depth = depth;
	this.fb = new GLFrameBuffer(color, depth);
	this.wnd = new PView.RenderState() {
		public Coord ul() {return(Coord.z);}
		public Coord sz() {return(sz);}
	    };
    }

    public void fin(FBConfig last) {
	if(sz.equals(last.sz) && (hdr == last.hdr) && (tdepth == last.tdepth)) {
	    fb = last.fb;
	    wnd = last.wnd;
	    color = last.color;
	    depth = last.depth;
	    return;
	}
	if(last.fb != null)
	    last.fb.dispose();
	if(cleanp())
	    return;
	create();
    }

    public void resolve(GOut g) {
	if(fb != null) {
	    g.image(color, Coord.z);
	}
    }
}
