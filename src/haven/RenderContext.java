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
import haven.render.*;
import haven.render.sl.ShaderMacro;
import haven.render.Texture.Sampler;
import haven.render.Texture2D.Sampler2D;

public abstract class RenderContext extends State implements OwnerContext {
    public static final Slot<RenderContext> slot = new Slot<>(Slot.Type.SYS, RenderContext.class);
    private final List<PostProcessor> post = new ArrayList<>();
    private final Map<Global, Integer> global = new IdentityHashMap<>();

    public static class FrameFormat {
	public VectorFormat cfmt;
	public int samples;
	public Coord sz;

	public FrameFormat(VectorFormat cfmt, int samples, Coord sz) {
	    this.cfmt = cfmt; this.samples = samples; this.sz = sz;
	}

	public FrameFormat(Texture tex) {
	    if(tex instanceof Texture2D) {
		Texture2D t = (Texture2D)tex;
		this.cfmt = t.ifmt;
		this.samples = 1;
		this.sz = t.sz();
	    } else if(tex instanceof Texture2DMS) {
		Texture2DMS t = (Texture2DMS)tex;
		this.cfmt = t.ifmt;
		this.samples = t.s;
		this.sz = t.sz();
	    } else {
		throw(new ClassCastException(String.valueOf(tex)));
	    }
	}

	public FrameFormat(FrameFormat from) {
	    this.cfmt = from.cfmt;
	    this.samples = from.samples;
	    this.sz = from.sz;
	}

	public boolean equals(FrameFormat that) {
	    return(Utils.eq(this.cfmt, that.cfmt) && (this.samples == that.samples) && Utils.eq(this.sz, that.sz));
	}
	public boolean equals(Object x) {
	    return((x instanceof FrameFormat) && equals((FrameFormat)x));
	}

	public Texture maketex() {
	    VectorFormat efmt = cfmt;
	    if(efmt.cf == NumberFormat.DEPTH)
		efmt = new VectorFormat(1, NumberFormat.FLOAT32);
	    if(samples == 1)
		return(new Texture2D(sz, DataBuffer.Usage.STATIC, cfmt, efmt, null));
	    else
		return(new Texture2DMS(sz, samples, cfmt));
	}

	public boolean matching(Texture tex) {
	    return((tex != null) && equals(new FrameFormat(tex)));
	}
    }

    public static abstract class PostProcessor implements Disposable {
	public static final int ORDER_RESOLVE = -200, ORDER_TONEMAP = -100, ORDER_DEFAULT = 0, ORDER_RESAMPLE = 100;
	public Sampler buf = null;

	public void run(GOut g, Sampler2D in) {
	    throw(new RuntimeException("no PostProcessor.run variant implemented: " + this.getClass()));
	}
	public void run(GOut g, Sampler in) {
	    run(g, (Sampler2D)in);
	}
	public int order() {return(ORDER_DEFAULT);}
	public FrameFormat outformat(FrameFormat in) {return(in);}

	public void dispose() {
	    if(buf != null)
		buf.dispose();
	}
    }

    public static interface Global {
	public default void prerender(Render out) {}
	public default void postrender(Render out) {}
    }

    public Collection<PostProcessor> postproc() {return(post);}
    public void add(PostProcessor post) {
	this.post.add(post);
	Collections.sort(this.post, Comparator.comparing(PostProcessor::order));
    }
    public void remove(PostProcessor post) {this.post.remove(post);}

    public abstract Pipe.Op basic(Object id);
    public abstract void basic(Object id, Pipe.Op state);

    public void add(Global glob) {
	synchronized(global) {
	    Integer cur = global.get(glob);
	    global.put(glob, (cur == null) ? 1 : (cur + 1));
	}
    }

    public void put(Global glob) {
	synchronized(global) {
	    Integer cur = global.get(glob);
	    if(cur == null)
		throw(new RuntimeException("removing non-present glob: " + glob));
	    if(cur <= 0) {
		throw(new AssertionError(String.valueOf(cur)));
	    } else if(cur == 1) {
		global.remove(glob);
		if(glob instanceof Disposable)
		    ((Disposable)glob).dispose();
	    } else {
		global.put(glob, cur - 1);
	    }
	}
    }

    public void prerender(Render out) {
	synchronized(global) {
	    for(Global glob : global.keySet())
		glob.prerender(out);
	}
    }

    public void postrender(Render out) {
	synchronized(global) {
	    for(Global glob : global.keySet())
		glob.postrender(out);
	}
    }

    public ShaderMacro shader() {return(null);}
    public void apply(Pipe p) {p.put(slot, this);}
}
