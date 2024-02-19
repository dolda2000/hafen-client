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

package haven.resutil;

import haven.*;
import haven.render.*;
import haven.render.sl.*;
import java.util.*;
import java.nio.*;
import haven.render.Texture2D.Sampler2D;
import static haven.render.sl.Cons.*;
import static haven.render.sl.Type.*;

public class OverTex extends State {
    public static final Slot<OverTex> slot = new Slot<OverTex>(Slot.Type.DRAW, OverTex.class);
    public static final Attribute otexc = new Attribute(VEC2, "otexc");
    public static boolean otexdb = false;
    private static final Uniform ctex = new Uniform(SAMPLER2D, p -> p.get(slot).tex, slot);
    private static final Map<Function, ShaderMacro> shcache = new HashMap<Function, ShaderMacro>();
    private final ShaderMacro shader;
    public final Sampler2D tex;

    public static final AutoVarying rtexcoord = new AutoVarying(VEC2, "s_otexc") {
	    protected Expression root(VertexContext vctx) {
		return(otexc.ref());
	    }
	};

    public static ValBlock.Value texcoord(FragmentContext fctx) {
	return(fctx.uniform.ext(rtexcoord, () -> fctx.uniform.new Value(VEC2) {
		public Expression root() {
		    return(rtexcoord.ref());
		}
	    }));
    }

    private static ShaderMacro shfor(final Function blend) {
	return(new ShaderMacro() {
		public void modify(final ProgramContext prog) {
		    texcoord(prog.fctx);
		    final ValBlock.Value color = prog.fctx.uniform.new Value(VEC4) {
			    public Expression root() {
				return(texture2D(ctex.ref(), texcoord(prog.fctx).depref()));
			    }
			};
		    color.force();
		    FragColor.fragcol(prog.fctx).mod(in -> blend.call(in, color.ref()), 10);
		}
	    });
    }

    public OverTex(Sampler2D tex, Function blend) {
	this.tex = tex;
	ShaderMacro sh;
	synchronized(shcache) {
	    sh = shcache.get(blend);
	    if(sh == null)
		shcache.put(blend, sh = shfor(blend));
	}
	shader = sh;
    }

    public OverTex(Sampler2D tex) {
	this(tex, MiscLib.cpblend);
    }

    public ShaderMacro shader() {return(shader);}

    public void apply(Pipe buf) {
	if(!(otexdb /* && Debug.kf3 XXXRENDER */))
	    buf.put(slot, this);
    }

    @Material.ResName("otex")
    public static class $ctex implements Material.ResCons2 {
	public Material.Res.Resolver cons(final Resource res, Object... args) {
	    final Indir<Resource> tres;
	    final int tid;
	    int a = 0;
	    if(args[a] instanceof String) {
		tres = res.pool.load((String)args[a], Utils.iv(args[a + 1]));
		tid = Utils.iv(args[a + 2]);
		a += 3;
	    } else {
		tres = res.indir();
		tid = Utils.iv(args[a]);
		a += 1;
	    }
	    final Function blend;
	    if(args.length > a) {
		String nm = (String)args[a++];
		if(nm.equals("cp")) {
		    blend = MiscLib.cpblend;
		} else if(nm.equals("ol")) {
		    blend = MiscLib.olblend;
		} else if(nm.equals("a")) {
		    blend = MiscLib.colblend;
		} else {
		    throw(new Resource.LoadException("Unknown overtex blend mode: " + nm, res));
		}
	    } else {
		blend = MiscLib.cpblend;
	    }
	    return(new Material.Res.Resolver() {
		    public void resolve(Collection<Pipe.Op> buf, Collection<Pipe.Op> dynbuf) {
			TexR rt = tres.get().layer(TexR.class, tid);
			if(rt == null)
			    throw(new RuntimeException(String.format("Specified texture %d for %s not found in %s", tid, res, tres)));
			buf.add(new OverTex(rt.tex().img, blend));
		    }
		});
	}
    }

    @VertexBuf.ResName("otex2")
    public static class OTexC extends VertexBuf.FloatData {
	public OTexC(FloatBuffer data) {super(otexc, 2, data);}
	public OTexC(Resource res, Message buf, int nv) {this(VertexBuf.loadbuf2(Utils.wfbuf(nv * 2), buf));}
    }
    @VertexBuf.ResName("otex")
    public static class CDecode implements VertexBuf.DataCons {
	public void cons(Collection<VertexBuf.AttribData> dst, Resource res, Message buf, int nv) {
	    dst.add(new OTexC(VertexBuf.loadbuf(Utils.wfbuf(nv * 2), buf)));
	}
    }

    static {
	Console.setscmd("otexdb", new Console.Command() {
		public void run(Console cons, String[] args) {
		    otexdb = Utils.parsebool(args[1], false);
		}
	    });
    }
}
