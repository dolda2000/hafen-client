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

import haven.render.*;
import haven.render.sl.*;
import static haven.render.sl.Cons.*;
import static haven.Utils.eq;

public class RenderedNormals extends State {
    public static final Slot<RenderedNormals> slot = new Slot<>(Slot.Type.SYS, RenderedNormals.class);
    public static final FragData fragnorm = new FragData(Type.VEC4, "fragnorm", p -> ((p.get(States.maskdepth.slot) == null) ? p.get(slot).img : null), slot, States.maskdepth.slot);
    public final Texture.Image<?> img;

    public RenderedNormals(Texture.Image<?> img) {
	this.img = img;
    }

    public boolean equals(Object o) {
	return((o instanceof RenderedNormals) &&
	       eq(((RenderedNormals)o).img, this.img));
    }

    private static final ShaderMacro shader = prog -> {
	Homo3D.frageyen(prog.fctx);
	ValBlock.Value val = prog.fctx.mainvals.ext(fragnorm, () -> prog.fctx.mainvals.new Value(Type.VEC4) {
		public Expression root() {
		    return(vec4(mul(add(Homo3D.frageyen(prog.fctx).depref(), l(1.0)), l(0.5)), l(1.0)));
		}

		protected void cons2(Block blk) {
		    blk.add(new LBinOp.Assign(fragnorm.ref(), init));
		}
	    });
	val.force();
    };
    public ShaderMacro shader() {
	return(shader);
    }

    public void apply(Pipe p) {p.put(slot, this);}

    public static class Canon implements Pipe.Op, Disposable {
	public Texture2D tex = null;
	private int refcount = 0;

	public void apply(Pipe p) {
	    FrameConfig fb = p.get(FrameConfig.slot);
	    if((tex == null) || !tex.sz().equals(fb.sz)) {
		if(tex != null)
		    tex.dispose();
		tex = new Texture2D(fb.sz, DataBuffer.Usage.STATIC, new VectorFormat(4, NumberFormat.UNORM8), null);
	    }
	    p.prep(new RenderedNormals(tex.image(0)));
	}

	public void dispose() {
	    if(tex != null)
		tex.dispose();
	}
    }

    public static Canon get(Pipe state) {
	RenderContext ctx = state.get(RenderContext.slot);
	if(ctx == null)
	    return(null);
	Canon ret;
	synchronized(ctx) {
	    ret = (Canon)ctx.basic(Canon.class);
	    if(ret == null) {
		ret = new Canon();
		ctx.basic(Canon.class, ret);
	    }
	    ret.refcount++;
	}
	return(ret);
    }

    public static void put(Pipe state) {
	RenderContext ctx = state.get(RenderContext.slot);
	if(ctx == null)
	    return;
	synchronized(ctx) {
	    Canon cur = (Canon)ctx.basic(Canon.class);
	    if(cur == null)
		throw(new IllegalStateException());
	    if(--cur.refcount <= 0) {
		ctx.basic(Canon.class, null);
		cur.dispose();
	    }
	}
    }

    @Material.ResName("masknorm")
    public static class $maskcol implements Material.ResCons {
	final Pipe.Op mask = p -> p.put(slot, null);
	public Pipe.Op cons(Resource res, Object... args) {
	    return(mask);
	}
    }
}
