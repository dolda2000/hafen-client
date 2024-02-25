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
import java.nio.FloatBuffer;
import haven.render.Texture2D.Sampler2D;
import static haven.render.sl.Cons.*;
import static haven.render.sl.Type.*;

public class BumpMap extends State {
    public static final Slot<BumpMap> slot = new Slot<BumpMap>(Slot.Type.DRAW, BumpMap.class);
    public static final Attribute tan = new Attribute(VEC3, "tan");
    public static final Attribute bit = new Attribute(VEC3, "bit");
    private static final Uniform ctex = new Uniform(SAMPLER2D, p -> p.get(slot).tex, slot);
    public final Sampler2D tex;
    private final ShaderMacro shader;

    public BumpMap(Sampler2D tex, boolean otex) {
	this.tex = tex;
	this.shader = shaders[otex ? 1 : 0];
    }
    public BumpMap(Sampler2D tex) {
	this(tex, false);
    }

    public static class Shader implements ShaderMacro {
	public final boolean otex;

	private Shader(boolean otex) {
	    this.otex = otex;
	}

	public static final AutoVarying tanc = new AutoVarying(VEC3) {
		protected Expression root(VertexContext vctx) {
		    return(Homo3D.get(vctx.prog).nlocxf(tan.ref()));
		}
	    };
	public static final AutoVarying bitc = new AutoVarying(VEC3) {
		protected Expression root(VertexContext vctx) {
		    return(Homo3D.get(vctx.prog).nlocxf(bit.ref()));
		}
	    };
	public void modify(final ProgramContext prog) {
	    ValBlock.Value texc;
	    if(!otex)
		texc = Tex2D.get(prog).texcoord();
	    else
		texc = OverTex.texcoord(prog.fctx);
	    final ValBlock.Value nmod = prog.fctx.uniform.new Value(VEC3) {
		    public Expression root() {
			return(mul(sub(pick(texture2D(ctex.ref(), texc.depref()), "rgb"),
				       l(0.5)), l(2.0)));
		    }
		};
	    nmod.force();
	    Homo3D.frageyen(prog.fctx).mod(in -> {
		    Expression m = nmod.ref();
		    return(add(mul(pick(m, "s"), tanc.ref()),
			       mul(pick(m, "t"), bitc.ref()),
			       mul(pick(m, "p"), in)));
		}, -100);
	    // FragColor.fragcol(prog.fctx).mod(in -> mix(in, vec4(nmod.ref(), l(1.0)), l(0.5)), 1000);

	    MeshMorph.get(prog.vctx).add(tanc.value(prog.vctx), MeshMorph.MorphType.DIR);
	    MeshMorph.get(prog.vctx).add(bitc.value(prog.vctx), MeshMorph.MorphType.DIR);
	}
    }
    private static final Shader[] shaders = {new Shader(false), new Shader(true)};

    public ShaderMacro shader() {return(shader);}

    public void apply(Pipe buf) {
	// if(buf.cfg.pref.flight.val) XXXRENDER
	buf.put(slot, this);
    }

    public static final MeshBuf.LayerID<MeshBuf.Vec3Layer> ltan = new MeshBuf.V3LayerID(tan);
    public static final MeshBuf.LayerID<MeshBuf.Vec3Layer> lbit = new MeshBuf.V3LayerID(bit);

    @Material.ResName("bump")
    public static class $bump implements Material.ResCons2 {
	public Material.Res.Resolver cons(final Resource res, Object... args) {
	    boolean totex = false;
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
	    if(a < args.length) {
		String f = (String)args[a++];
		if(f.indexOf('o') >= 0)
		    totex = true;
	    }
	    boolean otex = totex;
	    return(new Material.Res.Resolver() {
		    public void resolve(Collection<Pipe.Op> buf, Collection<Pipe.Op> dynbuf) {
			TexR rt = tres.get().layer(TexR.class, tid);
			if(rt == null)
			    throw(new RuntimeException(String.format("Specified texture %d for %s not found in %s", tid, res, tres)));
			buf.add(new BumpMap(rt.tex().img, otex));
		    }
		});
	}
    }

    @VertexBuf.ResName("tan2")
    public static class Tangents extends VertexBuf.FloatData {
	public Tangents(FloatBuffer data) {super(tan, 3, data);}
	public Tangents(Resource res, Message buf, int nv) {this(VertexBuf.loadbuf2(Utils.wfbuf(nv * 3), buf));}
    }
    @VertexBuf.ResName("bit2")
    public static class BiTangents extends VertexBuf.FloatData {
	public BiTangents(FloatBuffer data) {super(bit, 3, data);}
	public BiTangents(Resource res, Message buf, int nv) {this(VertexBuf.loadbuf2(Utils.wfbuf(nv * 3), buf));}
    }
    @VertexBuf.ResName("tan")
    public static class TanDecode implements VertexBuf.DataCons {
	public void cons(Collection<VertexBuf.AttribData> dst, Resource res, Message buf, int nv) {
	    dst.add(new Tangents(VertexBuf.loadbuf(Utils.wfbuf(nv * 3), buf)));
	}
    }
    @VertexBuf.ResName("bit")
    public static class BitDecode implements VertexBuf.DataCons {
	public void cons(Collection<VertexBuf.AttribData> dst, Resource res, Message buf, int nv) {
	    dst.add(new BiTangents(VertexBuf.loadbuf(Utils.wfbuf(nv * 3), buf)));
	}
    }
}
