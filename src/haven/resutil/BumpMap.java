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
import haven.glsl.*;
import static haven.glsl.Cons.*;
import static haven.glsl.Type.*;
import haven.MapMesh.Scan;
import java.util.*;
import javax.media.opengl.*;

public class BumpMap extends GLState {
    public static final Slot<BumpMap> slot = new Slot<BumpMap>(Slot.Type.DRAW, BumpMap.class);
    public static final Attribute tan = new Attribute(VEC3);
    public static final Attribute bit = new Attribute(VEC3);
    private static final Uniform ctex = new Uniform(SAMPLER2D);
    public final TexGL tex;
    private TexUnit sampler;

    public BumpMap(TexGL tex) {
	this.tex = tex;
    }

    private static final ShaderMacro[] shaders = {
	new ShaderMacro() {
	    final AutoVarying tanc = new AutoVarying(VEC3) {
		    protected Expression root(VertexContext vctx) {
			return(mul(vctx.gl_NormalMatrix.ref(), tan.ref()));
		    }
		};
	    final AutoVarying bitc = new AutoVarying(VEC3) {
		    protected Expression root(VertexContext vctx) {
			return(mul(vctx.gl_NormalMatrix.ref(), bit.ref()));
		    }
		};
	    public void modify(ProgramContext prog) {
		final ValBlock.Value nmod = prog.fctx.uniform.new Value(VEC3) {
			public Expression root() {
			    return(mul(sub(pick(texture2D(ctex.ref(), Tex2D.texcoord.ref()), "rgb"),
					   l(0.5)), l(2.0)));
			}
		    };
		nmod.force();
		MiscLib.frageyen(prog.fctx).mod(new Macro1<Expression>() {
			public Expression expand(Expression in) {
			    Expression m = nmod.ref();
			    return(add(mul(pick(m, "s"), tanc.ref()),
				       mul(pick(m, "t"), bitc.ref()),
				       mul(pick(m, "p"), in)));
			}
		    }, -100);
		/*
		prog.fctx.fragcol.mod(new Macro1<Expression>() {
			public Expression expand(Expression in) {
			    return(mix(in, vec4(nmod.ref(), l(1.0)), l(0.5)));
			}
		    }, 1000);
		*/
	    }
	}
    };

    public ShaderMacro[] shaders() {return(shaders);}
    public boolean reqshaders() {return(true);}

    public void reapply(GOut g) {
	g.gl.glUniform1i(g.st.prog.uniform(ctex), sampler.id);
    }

    public void apply(GOut g) {
	sampler = TexGL.lbind(g, tex);
	reapply(g);
    }

    public void unapply(GOut g) {
	GL2 gl = g.gl;
	sampler.act();
	gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
	sampler.free(); sampler = null;
    }

    public void prep(Buffer buf) {
	buf.put(slot, this);
    }

    public static class MapTangents extends MapMesh.Hooks {
	public final MapMesh m;
	public final Scan s;
	public final Coord3f[] tan, bit;

	public MapTangents(MapMesh m) {
	    this.m = m;
	    s = new Scan(Coord.z, m.sz.add(1, 1));
	    tan = new Coord3f[s.l];
	    bit = new Coord3f[s.l];
	    for(int i = 0; i < s.l; i++) {
		tan[i] = new Coord3f(0, 0, 0);
		bit[i] = new Coord3f(0, 0, 0);
	    }
	}

	public void postcalcnrm(Random rnd) {
	    MapMesh.Surface gnd = m.gnd();
	    for(int y = s.ul.y; y < s.br.y; y++) {
		for(int x = s.ul.x; x < s.br.x; x++) {
		    MapMesh.SPoint sp = gnd.spoint(new Coord(x, y));
		    Coord3f ct = Coord3f.yu.cmul(sp.nrm).norm();
		    Coord3f cb = sp.nrm.cmul(Coord3f.xu).norm();
		    Coord3f mt = tan[s.o(x, y)];
		    mt.x = ct.x; mt.y = ct.y; mt.z = ct.z;
		    Coord3f mb = bit[s.o(x, y)];
		    mb.x = cb.x; mb.y = cb.y; mb.z = cb.z;
		}
	    }
	}

	public void set(MeshBuf buf, Coord lc, MeshBuf.Vertex v1, MeshBuf.Vertex v2, MeshBuf.Vertex v3, MeshBuf.Vertex v4) {
	    MeshBuf.Vec3Layer btan = buf.layer(ltan);
	    MeshBuf.Vec3Layer bbit = buf.layer(lbit);
	    btan.set(v1, tan[s.o(lc)]); bbit.set(v1, bit[s.o(lc)]);
	    btan.set(v2, tan[s.o(lc.add(0, 1))]); bbit.set(v2, bit[s.o(lc.add(0, 1))]);
	    btan.set(v3, tan[s.o(lc.add(1, 1))]); bbit.set(v3, bit[s.o(lc.add(1, 1))]);
	    btan.set(v4, tan[s.o(lc.add(1, 0))]); bbit.set(v4, bit[s.o(lc.add(1, 0))]);
	}

	public static final MapMesh.DataID<MapTangents> id = MapMesh.makeid(MapTangents.class);
    }

    public static final MeshBuf.LayerID<MeshBuf.Vec3Layer> ltan = new MeshBuf.V3LayerID(tan);
    public static final MeshBuf.LayerID<MeshBuf.Vec3Layer> lbit = new MeshBuf.V3LayerID(bit);

    @Material.ResName("bump")
    public static class $bump implements Material.ResCons2 {
	public void cons(final Resource res, List<GLState> states, List<Material.Res.Resolver> left, Object... args) {
	    final Resource tres;
	    final int tid;
	    int a = 0;
	    if(args[a] instanceof String) {
		tres = Resource.load((String)args[a], (Integer)args[a + 1]);
		tid = (Integer)args[a + 2];
		a += 3;
	    } else {
		tres = res;
		tid = (Integer)args[a];
		a += 1;
	    }
	    left.add(new Material.Res.Resolver() {
		    public void resolve(Collection<GLState> buf) {
			TexR rt = tres.layer(TexR.class, tid);
			if(rt == null)
			    throw(new RuntimeException(String.format("Specified texture %d for %s not found in %s", tid, res, tres)));
			/* XXX: It is somewhat doubtful that this cast is really quite reasonable. */
			buf.add(new BumpMap((TexGL)rt.tex()));
		    }
		});
	}
    }
}
