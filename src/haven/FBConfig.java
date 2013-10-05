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
import javax.media.opengl.*;
import haven.glsl.*;

public class FBConfig {
    public Coord sz;
    public boolean hdr, tdepth;
    public GLFrameBuffer fb;
    public PView.RenderState wnd;
    public TexGL color[], depth;
    private RenderTarget[] tgts = new RenderTarget[0];

    public FBConfig(Coord sz) {
	this.sz = sz;
    }

    public boolean cleanp() {
	if(hdr || tdepth)
	    return(false);
	for(int i = 0; i < tgts.length; i++) {
	    if(tgts[i] != null)
		return(false);
	}
	return(true);
    }

    private void create() {
	Collection<TexGL> color = new LinkedList<TexGL>();
	TexGL depth;
	Collection<ShaderMacro> sh = new LinkedList<ShaderMacro>();
	if(hdr) {
	    color.add(new TexE(sz, GL.GL_RGBA16F, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE));
	} else {
	    color.add(new TexE(sz, GL.GL_RGBA, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE));
	}
	if(tdepth) {
	    depth = new TexE(sz, GL2.GL_DEPTH_COMPONENT, GL2.GL_DEPTH_COMPONENT, GL.GL_UNSIGNED_INT);
	} else {
	    depth = null;
	}
	for(int i = 0; i < tgts.length; i++) {
	    if(tgts[i] != null) {
		color.add(tgts[i].maketex(this));
		sh.add(tgts[i].code(this, i + 1));
	    }
	}
	this.color = color.toArray(new TexGL[0]);
	this.depth = depth;
	/* XXX: Shaders should be canonized and cached to avoid
	 * creation of unnecessary identical programs when
	 * configurations change. */
	final ShaderMacro[] shaders = sh.toArray(new ShaderMacro[0]);
	this.fb = new GLFrameBuffer(this.color, this.depth) {
		public ShaderMacro[] shaders() {return(shaders);}
		public boolean reqshaders() {return(shaders.length > 0);}
	    };
	this.wnd = new PView.RenderState() {
		public Coord ul() {return(Coord.z);}
		public Coord sz() {return(sz);}
	    };
    }

    private static <T> boolean hasuo(T[] a, T[] b) {
	outer: for(T ae : a) {
	    for(T be : b) {
		if(ae == be)
		    continue outer;
	    }
	    return(false);
	}
	return(true);
    }

    public static boolean equals(FBConfig a, FBConfig b) {
	if(!a.sz.equals(b.sz))
	    return(false);
	if((a.hdr != b.hdr) || (a.tdepth != b.tdepth))
	    return(false);
	if(!hasuo(a.tgts, b.tgts) || !hasuo(b.tgts, a.tgts))
	    return(false);
	return(true);
    }

    public void fin(FBConfig last) {
	if(equals(this, last)) {
	    fb = last.fb;
	    wnd = last.wnd;
	    color = last.color;
	    depth = last.depth;
	    tgts = last.tgts;
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
	    g.image(color[0], Coord.z);
	}
    }

    public void add(RenderTarget tgt) {
	int i;
	for(i = 0; i < tgts.length; i++) {
	    if(tgts[i] == null)
		tgts[i] = tgt;
	    return;
	}
	tgts = Utils.extend(tgts, i + 1);
	tgts[i] = tgt;
    }

    public static abstract class RenderTarget {
	public TexGL tex;

	public TexGL maketex(FBConfig cfg) {
	    return(tex = new TexE(cfg.sz, GL.GL_RGBA, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE));
	}

	public abstract ShaderMacro code(FBConfig cfg, int id);
    }
}
