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

import java.util.*;
import haven.*;
import haven.glsl.*;
import static haven.glsl.Cons.*;
import haven.Resource.Tile;
import haven.MapMesh.Scan;
import haven.Surface.Vertex;
import haven.Surface.MeshVertex;
import javax.media.opengl.*;
import java.awt.Color;

public class WaterTile extends Tiler {
    public final int depth;
    private static final Material.Colors bcol = new Material.Colors(new Color(128, 128, 128), new Color(255, 255, 255), new Color(0, 0, 0), new Color(0, 0, 0));
    public final Tiler.Cons bottom;
    
    public static class Bottom extends MapMesh.Hooks {
	final MapMesh m;
	final boolean[] s;
	final Vertex[] surf;
	final boolean[] split;
	int[] ed;
	final Scan vs, ss;
	
	public Bottom(MapMesh m) {
	    this.m = m;
	    MapMesh.MapSurface ms = m.data(m.gnd);
	    this.vs = ms.vs;
	    Scan ts = ms.ts;
	    this.surf = new Vertex[vs.l];
	    this.split = new boolean[ts.l];
	    Coord sz = m.sz;
	    MCache map = m.map;
	    Scan ds = new Scan(new Coord(-10, -10), sz.add(21, 21));
	    ss = new Scan(new Coord(-9, -9), sz.add(19,  19));
	    int[] d = new int[ds.l];
	    s = new boolean[ss.l];
	    ed = new int[ss.l];
	    for(int y = ds.ul.y; y < ds.br.y; y++) {
		for(int x = ds.ul.y; x < ds.br.x; x++) {
		    Tiler t = map.tiler(map.gettile(m.ul.add(x, y)));
		    if(t instanceof WaterTile)
			d[ds.o(x, y)] = ((WaterTile)t).depth;
		    else
			d[ds.o(x, y)] = 0;
		}
	    }
	    for(int y = ss.ul.y; y < ss.br.y; y++) {
		for(int x = ss.ul.x; x < ss.br.x; x++) {
		    int td = d[ds.o(x, y)];
		    td = Math.min(td, d[ds.o(x - 1, y - 1)]);
		    td = Math.min(td, d[ds.o(x, y - 1)]);
		    td = Math.min(td, d[ds.o(x - 1, y)]);
		    ed[ss.o(x, y)] = td;
		    if(td == 0)
			s[ss.o(x, y)] = true;
		}
	    }
	    for(int i = 0; i < 8; i++) {
		int[] sd = new int[ss.l];
		for(int y = ss.ul.y + 1; y < ss.br.y - 1; y++) {
		    for(int x = ss.ul.x + 1; x < ss.br.x - 1; x++) {
			if(s[ss.o(x, y)]) {
			    sd[ss.o(x, y)] = ed[ss.o(x, y)];
			} else {
			    sd[ss.o(x, y)] = ((ed[ss.o(x, y)] * 4) +
					      ed[ss.o(x - 1, y)] + ed[ss.o(x + 1, y)] +
					      ed[ss.o(x, y - 1)] + ed[ss.o(x, y + 1)]) / 8;
			}
		    }
		}
		ed = sd;
	    }
	    for(int y = vs.ul.y; y < vs.br.y; y++) {
		for(int x = vs.ul.x; x < vs.br.x; x++) {
		    surf[vs.o(x, y)] = ms.new Vertex(ms.surf[vs.o(x, y)].add(0, 0, -ed[ss.o(x, y)]));
		}
	    }
	    for(int y = ts.ul.y; y < ts.br.y; y++) {
		for(int x = ts.ul.x; x < ts.br.x; x++) {
		    split[ts.o(x, y)] = Math.abs(surf[vs.o(x, y)].z - surf[vs.o(x + 1, y + 1)].z) > Math.abs(surf[vs.o(x + 1, y)].z - surf[vs.o(x, y + 1)].z);
		}
	    }
	}

	public int d(int x, int y) {
	    return(ed[ss.o(x, y)]);
	}

	public Vertex[] fortilea(Coord c) {
	    return(new Vertex[] {
		    surf[vs.o(c.x, c.y)],
		    surf[vs.o(c.x, c.y + 1)],
		    surf[vs.o(c.x + 1, c.y + 1)],
		    surf[vs.o(c.x + 1, c.y)],
		});
	}

	public void calcnrm() {
	    super.calcnrm();
	    MapMesh.MapSurface ms = m.data(MapMesh.gnd);
	    Surface.Normals n = ms.data(Surface.nrm);
	    Coord c = new Coord();
	    for(c.y = 0; c.y <= m.sz.y; c.y++) {
		for(c.x = 0; c.x <= m.sz.x; c.x++) {
		    if(s[ss.o(c)])
			n.set(surf[vs.o(c)], n.get(ms.fortile(c)));
		}
	    }
	}

	public static final MapMesh.DataID<Bottom> id = MapMesh.makeid(Bottom.class);
    }

    public void model(MapMesh m, Random rnd, Coord lc, Coord gc) {
	super.model(m, rnd, lc, gc);
	Bottom b = m.data(Bottom.id);
	MapMesh.MapSurface s = m.data(MapMesh.gnd);
	if(b.split[s.ts.o(lc)]) {
	    s.new Face(b.surf[b.vs.o(lc.x, lc.y)],
		       b.surf[b.vs.o(lc.x, lc.y + 1)],
		       b.surf[b.vs.o(lc.x + 1, lc.y + 1)]);
	    s.new Face(b.surf[b.vs.o(lc.x, lc.y)],
		       b.surf[b.vs.o(lc.x + 1, lc.y + 1)],
		       b.surf[b.vs.o(lc.x + 1, lc.y)]);
	} else {
	    s.new Face(b.surf[b.vs.o(lc.x, lc.y)],
		       b.surf[b.vs.o(lc.x, lc.y + 1)],
		       b.surf[b.vs.o(lc.x + 1, lc.y)]);
	    s.new Face(b.surf[b.vs.o(lc.x, lc.y + 1)],
		       b.surf[b.vs.o(lc.x + 1, lc.y + 1)],
		       b.surf[b.vs.o(lc.x + 1, lc.y)]);
	}
    }

    static final TexCube sky = new TexCube(Resource.loadimg("gfx/tiles/skycube"));
    static final TexI nrm = (TexI)Resource.loadtex("gfx/tiles/wn");
    static {
	nrm.mipmap();
	nrm.magfilter(GL.GL_LINEAR);
    }

    public static class SimpleSurface extends GLState.StandAlone {
	private static States.DepthOffset soff = new States.DepthOffset(2, 2);
	TexUnit tsky, tnrm;

	private SimpleSurface() {
	    super(GLState.Slot.Type.DRAW, PView.cam, HavenPanel.global);
	}

	public void apply(GOut g) {
	    GL2 gl = g.gl;
	    (tsky = g.st.texalloc()).act();
	    gl.glTexGeni(GL2.GL_S, GL2.GL_TEXTURE_GEN_MODE, GL2.GL_REFLECTION_MAP);
	    gl.glTexGeni(GL2.GL_T, GL2.GL_TEXTURE_GEN_MODE, GL2.GL_REFLECTION_MAP);
	    gl.glTexGeni(GL2.GL_R, GL2.GL_TEXTURE_GEN_MODE, GL2.GL_REFLECTION_MAP);
	    gl.glEnable(GL2.GL_TEXTURE_GEN_S);
	    gl.glEnable(GL2.GL_TEXTURE_GEN_T);
	    gl.glEnable(GL2.GL_TEXTURE_GEN_R);
	    gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_MODULATE);
	    gl.glEnable(GL.GL_TEXTURE_CUBE_MAP);
	    gl.glBindTexture(GL.GL_TEXTURE_CUBE_MAP, sky.glid(g));
	    gl.glColor4f(1, 1, 1, 0.5f);
	    g.st.matmode(GL.GL_TEXTURE);
	    gl.glPushMatrix();
	    g.st.cam.transpose().trim3(1).loadgl(gl);
	}

	public void unapply(GOut g) {
	    GL2 gl = g.gl;
	    tsky.act();
	    g.st.matmode(GL.GL_TEXTURE);
	    gl.glPopMatrix();
	    gl.glDisable(GL.GL_TEXTURE_CUBE_MAP);
	    gl.glDisable(GL2.GL_TEXTURE_GEN_S);
	    gl.glDisable(GL2.GL_TEXTURE_GEN_T);
	    gl.glDisable(GL2.GL_TEXTURE_GEN_R);
	    gl.glColor3f(1, 1, 1);
	    tsky.free(); tsky = null;
	}

	public void prep(Buffer buf) {
	    buf.put(States.color, null);
	    buf.put(Light.lighting, null);
	    soff.prep(buf);
	    super.prep(buf);
	}
    }

    public static class BetterSurface extends SimpleSurface {
	private final Uniform ssky = new Uniform(Type.SAMPLERCUBE);
	private final Uniform snrm = new Uniform(Type.SAMPLER2D);
	private final Uniform icam = new Uniform(Type.MAT3);

	private BetterSurface() {
	}

	private ShaderMacro[] shaders = {
	    new ShaderMacro() {
		final AutoVarying skyc = new AutoVarying(Type.VEC3) {
			protected Expression root(VertexContext vctx) {
			    return(mul(icam.ref(), reflect(MiscLib.vertedir(vctx).depref(), vctx.eyen.depref())));
			}
		    };
		public void modify(final ProgramContext prog) {
		    MiscLib.fragedir(prog.fctx);
		    final ValBlock.Value nmod = prog.fctx.uniform.new Value(Type.VEC3) {
			    public Expression root() {
				/*
				return(mul(sub(mix(pick(texture2D(snrm.ref(),
								  add(mul(pick(MiscLib.fragmapv.ref(), "st"), vec2(l(0.01), l(0.012))),
								      mul(Cons.mod(MiscLib.time.ref(), l(2.0)), vec2(l(0.025), l(0.035))))),
							"rgb"),
						   pick(texture2D(snrm.ref(),
								  add(mul(pick(MiscLib.fragmapv.ref(), "st"), vec2(l(0.019), l(0.018))),
								      mul(Cons.mod(add(MiscLib.time.ref(), l(1.0)), l(2.0)), vec2(l(-0.035), l(-0.025))))),
							"rgb"),
						   abs(sub(Cons.mod(MiscLib.time.ref(), l(2.0)), l(1.0)))),
					       l(0.5)), vec3(l(1.0 / 16), l(1.0 / 16), l(1.0))));
				*/
				return(mul(sub(mix(add(pick(texture2D(snrm.ref(),
								      add(mul(pick(MiscLib.fragmapv.ref(), "st"), vec2(l(0.01), l(0.012))),
									  mul(MiscLib.time.ref(), vec2(l(0.025), l(0.035))))),
							    "rgb"),
						       pick(texture2D(snrm.ref(),
								      add(mul(pick(MiscLib.fragmapv.ref(), "st"), vec2(l(0.019), l(0.018))),
									  mul(MiscLib.time.ref(), vec2(l(-0.035), l(-0.025))))),
							    "rgb")),
						   add(pick(texture2D(snrm.ref(),
								      add(mul(pick(MiscLib.fragmapv.ref(), "st"), vec2(l(0.01), l(0.012))),
									  add(mul(MiscLib.time.ref(), vec2(l(0.025), l(0.035))), vec2(l(0.5), l(0.5))))),
							    "rgb"),
						       pick(texture2D(snrm.ref(),
								      add(mul(pick(MiscLib.fragmapv.ref(), "st"), vec2(l(0.019), l(0.018))),
									  add(mul(MiscLib.time.ref(), vec2(l(-0.035), l(-0.025))), vec2(l(0.5), l(0.5))))),
							    "rgb")),
						   abs(sub(haven.glsl.Cons.mod(MiscLib.time.ref(), l(2.0)), l(1.0)))),
					       l(0.5 * 2)), vec3(l(1.0 / 16), l(1.0 / 16), l(1.0))));
				/*
				return(mul(sub(add(pick(texture2D(snrm.ref(),
								  add(mul(pick(MiscLib.fragmapv.ref(), "st"), vec2(l(0.01), l(0.012))),
								      mul(MiscLib.time.ref(), vec2(l(0.025), l(0.035))))),
							"rgb"),
						   pick(texture2D(snrm.ref(),
								  add(mul(pick(MiscLib.fragmapv.ref(), "st"), vec2(l(0.019), l(0.018))),
								      mul(MiscLib.time.ref(), vec2(l(-0.035), l(-0.025))))),
							"rgb")),
					       l(0.5 * 2)), vec3(l(1.0 / 16), l(1.0 / 16), l(1.0))));
				*/
				/*
				return(mul(sub(pick(texture2D(snrm.ref(),
							      mul(pick(MiscLib.fragmapv.ref(), "st"), l(0.005))),
						    "rgb"),
					       l(0.5)), vec3(l(1.0 / 32), l(1.0 / 32), l(1.0))));
				*/
			    }
			};
		    nmod.force();
		    MiscLib.frageyen(prog.fctx).mod(new Macro1<Expression>() {
			    public Expression expand(Expression in) {
				Expression m = nmod.ref();
				return(add(mul(pick(m, "x"), vec3(l(1.0), l(0.0), l(0.0))),
					   mul(pick(m, "y"), vec3(l(0.0), l(1.0), l(0.0))),
					   mul(pick(m, "z"), in)));
			    }
			}, -10);
		    prog.fctx.fragcol.mod(new Macro1<Expression>() {
			    public Expression expand(Expression in) {
				return(mul(in, textureCube(ssky.ref(), neg(mul(icam.ref(), reflect(MiscLib.fragedir(prog.fctx).depref(), MiscLib.frageyen(prog.fctx).depref())))),
					   l(0.4)));
			    }
			}, 0);
		}
	    }
	};

	public void reapply(GOut g) {
	    GL2 gl = g.gl;
	    gl.glUniform1i(g.st.prog.uniform(ssky), tsky.id);
	    gl.glUniform1i(g.st.prog.uniform(snrm), tnrm.id);
	    gl.glUniformMatrix3fv(g.st.prog.uniform(icam), 1, false, g.st.cam.transpose().trim3(), 0);
	}

	private void papply(GOut g) {
	    GL2 gl = g.gl;
	    gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE);
	    (tsky = g.st.texalloc()).act();
	    gl.glBindTexture(GL.GL_TEXTURE_CUBE_MAP, sky.glid(g));
	    (tnrm = g.st.texalloc()).act();
	    gl.glBindTexture(GL.GL_TEXTURE_2D, nrm.glid(g));
	    reapply(g);
	}

	private void punapply(GOut g) {
	    GL2 gl = g.gl;
	    tsky.act();
	    gl.glBindTexture(GL.GL_TEXTURE_CUBE_MAP, 0);
	    tnrm.act();
	    gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
	    tsky.free(); tsky = null;
	    tnrm.free(); tnrm = null;
	    gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
	}

	public ShaderMacro[] shaders() {return(shaders);}
	public boolean reqshaders() {return(true);}

	public void apply(GOut g) {
	    if(g.st.prog == null)
		super.apply(g);
	    else
		papply(g);
	}
	    
	public void unapply(GOut g) {
	    if(!g.st.usedprog)
		super.unapply(g);
	    else
		punapply(g);
	}
    }

    public static final GLState surfmat = new GLState.Abstract() {
	    final GLState s1 = new SimpleSurface(), s2 = new BetterSurface();
	    public void prep(Buffer buf) {
		if(buf.cfg.pref.wsurf.val)
		    s2.prep(buf);
		else
		    s1.prep(buf);
	    }
	};
    public static final GLState surfmatc = GLState.compose(surfmat, new MapMesh.MLOrder(1000));

    public static final MeshBuf.LayerID<MeshBuf.Vec1Layer> depthlayer = new MeshBuf.V1LayerID(BottomFog.depth);

    public static class BottomFog extends GLState.StandAlone {
	public static final double maxdepth = 25;
	public static final Color fogcolor = new Color(13, 38, 25);
	public static final Expression mfogcolor = mul(col3(fogcolor), pick(fref(idx(ProgramContext.gl_LightSource.ref(), MapView.amblight.ref()), "diffuse"), "rgb"));
	public static Function rgbmix = new Function.Def(Type.VEC4) {{
	    Expression a = param(PDir.IN, Type.VEC4).ref();
	    Expression b = param(PDir.IN, Type.VEC3).ref();
	    Expression m = param(PDir.IN, Type.FLOAT).ref();
	    code.add(new Return(vec4(mix(pick(a, "rgb"), b, m), pick(a, "a"))));
	}};
	public static final Attribute depth = new Attribute(Type.FLOAT);
	public static final AutoVarying fragd = new AutoVarying(Type.FLOAT) {
		protected Expression root(VertexContext vctx) {
		    return(depth.ref());
		}
	    };

	private final ShaderMacro shaders[] = {
	    new ShaderMacro() {
		public void modify(ProgramContext prog) {
		    prog.fctx.fragcol.mod(new Macro1<Expression>() {
			    public Expression expand(Expression in) {
				return(rgbmix.call(in, mfogcolor, min(div(fragd.ref(), l(maxdepth)), l(1.0))));
			    }
			}, 1000);
		}
	    }
	};

	private BottomFog() {
	    super(Slot.Type.DRAW);
	}

	public void apply(GOut g) {}
	public void unapply(GOut g) {}
	public ShaderMacro[] shaders() {return(shaders);}
	public boolean reqshaders() {return(true);}
	public void prep(Buffer buf) {
	    if(buf.cfg.pref.wsurf.val)
		super.prep(buf);
	}
    }
    public static final BottomFog waterfog = new BottomFog();
    private static final GLState boff = new States.DepthOffset(4, 4);

    public static final GLState obfog = new GLState.StandAlone(GLState.Slot.Type.DRAW) {
	final AutoVarying fragd = new AutoVarying(Type.FLOAT) {
		protected Expression root(VertexContext vctx) {
		    return(sub(pick(MiscLib.maploc.ref(), "z"), pick(vctx.mapv.depref(), "z")));
		}
	    };

	final ShaderMacro[] shaders = {
	    new ShaderMacro() {
		public void modify(ProgramContext prog) {
		    prog.fctx.fragcol.mod(new Macro1<Expression>() {
			    public Expression expand(Expression in) {
				return(BottomFog.rgbmix.call(in, BottomFog.mfogcolor, clamp(div(fragd.ref(), l(BottomFog.maxdepth)), l(0.0), l(1.0))));
			    }
			}, 1000);
		}
	    }
	};
	public void apply(GOut g) {}
	public void unapply(GOut g) {}
	public ShaderMacro[] shaders() {return(shaders);}
	public boolean reqshaders() {return(true);}
    };

    @ResName("water")
    public static class Fac implements Factory {
	public Tiler create(int id, Resource.Tileset set) {
	    int a = 0;
	    int depth = (Integer)set.ta[a++];
	    Tiler.Cons bottom = new GroundTile(id, set);
	    while(a < set.ta.length) {
		Object[] desc = (Object[])set.ta[a++];
		String p = (String)desc[0];
		if(p.equals("bottom") /* Backwards compatibility */ || p.equals("gnd") || p.equals("trn")) {
		    Resource bres = Resource.load((String)desc[1], (Integer)desc[2]);
		    Resource.Tileset ts = bres.layer(Resource.tileset);
		    Tiler b = ts.tfac().create(id, ts);
		    bottom = (Tiler.Cons)b;
		}
	    }
	    return(new WaterTile(id, bottom, depth));
	}
    }

    public WaterTile(int id, Tiler.Cons bottom, int depth) {
	super(id);
	this.bottom = bottom;
	this.depth = depth;
    }

    @Deprecated
    public WaterTile(int id, Resource.Tileset set, int depth) {
	this(id, new GroundTile(0, set), depth);
    }

    public static class SurfVert extends Surface.BufMap {
	final MapMesh.MapSurface ms;

	public SurfVert(MapMesh m) {
	    m.data(MapMesh.gnd).super(MapMesh.Models.get(m, surfmatc));
	    this.ms = m.data(MapMesh.gnd);
	}

	public MeshVertex[] fortile(Coord c) {
	    return(get(ms.fortilea(c)));
	}

	public static final MapMesh.DataID<SurfVert> id = MapMesh.makeid(SurfVert.class);
    }

    public void lay(MapMesh m, Random rnd, Coord lc, Coord gc) {
	MapMesh.MapSurface ms = m.data(MapMesh.gnd);
	SurfVert surf = m.data(SurfVert.id);
	Bottom b = m.data(Bottom.id);
	MeshVertex[] v = surf.fortile(lc);
	if(ms.split[ms.ts.o(lc)]) {
	    surf.buf.new Face(v[0], v[1], v[2]);
	    surf.buf.new Face(v[0], v[2], v[3]);
	} else {
	    surf.buf.new Face(v[0], v[1], v[3]);
	    surf.buf.new Face(v[1], v[2], v[3]);
	}
	bottom.faces(m, lc, gc, ms.fortilea(lc), ctcx, ctcy, ms.split[ms.ts.o(lc)]?rdiag:ldiag);
    }

    public void trans(MapMesh m, Random rnd, Tiler gt, Coord lc, Coord gc, int z, int bmask, int cmask) {
	if(bottom instanceof Tiler)
	    ((Tiler)bottom).trans(m, rnd, gt, lc, gc, z, bmask, cmask);
    }

    /*
    public static class GWaterTile extends WaterTile {
	public final Resource.Tileset bottom;
	public final GLState mat;

	public GWaterTile(int id, Resource.Tileset set, int depth, Resource.Tileset bottom) {
	    super(id, set, depth);
	    this.bottom = bottom;
	    TexGL tex = (TexGL)((TexSI)bottom.ground.pick(0).tex()).parent;
	    mat = new Material(Light.deflight, bcol, tex.draw(), waterfog, boff);
	}

	public static class BottomPlane extends MapMesh.Plane {
	    Bottom srf;
	    Coord lc;

	    public BottomPlane(MapMesh m, Bottom srf, Coord lc, int z, GLState mat, Tex tex) {
		m.super(srf.fortile(lc), z, mat, tex);
		this.srf = srf;
		this.lc = new Coord(lc);
	    }

	    public void build(MeshBuf buf) {
		MeshBuf.Tex ta = buf.layer(MeshBuf.tex);
		MeshBuf.Vec1Layer da = buf.layer(depthlayer);
		MeshBuf.Vertex v1 = buf.new Vertex(vrt[0].pos, vrt[0].nrm);
		MeshBuf.Vertex v2 = buf.new Vertex(vrt[1].pos, vrt[1].nrm);
		MeshBuf.Vertex v3 = buf.new Vertex(vrt[2].pos, vrt[2].nrm);
		MeshBuf.Vertex v4 = buf.new Vertex(vrt[3].pos, vrt[3].nrm);
		ta.set(v1, new Coord3f(tex.tcx(texx[0]), tex.tcy(texy[0]), 0.0f));
		ta.set(v2, new Coord3f(tex.tcx(texx[1]), tex.tcy(texy[1]), 0.0f));
		ta.set(v3, new Coord3f(tex.tcx(texx[2]), tex.tcy(texy[2]), 0.0f));
		ta.set(v4, new Coord3f(tex.tcx(texx[3]), tex.tcy(texy[3]), 0.0f));
		da.set(v1, (float)srf.d(lc.x, lc.y));
		da.set(v2, (float)srf.d(lc.x, lc.y + 1));
		da.set(v3, (float)srf.d(lc.x + 1, lc.y + 1));
		da.set(v4, (float)srf.d(lc.x + 1, lc.y));
		MapMesh.splitquad(buf, v1, v2, v3, v4);
	    }
	}

	public void lay(MapMesh m, Random rnd, Coord lc, Coord gc) {
	    Tile g = bottom.ground.pick(rnd);
	    new BottomPlane(m, m.data(Bottom.id), lc, 0, mat, g.tex());
	    m.new Plane(m.gnd(), lc, 257, surfmat);
	}
    
	public void trans(MapMesh m, Random rnd, Tiler gt, Coord lc, Coord gc, int z, int bmask, int cmask) {
	    if(m.map.gettile(gc) <= id)
		return;
	    if((bottom.btrans != null) && (bmask > 0)) {
		Tile t = bottom.btrans[bmask - 1].pick(rnd);
		if(gt instanceof WaterTile)
		    new BottomPlane(m, m.data(Bottom.id), lc, z, mat, t.tex());
		else
		    gt.layover(m, lc, gc, z, t);
	    }
	    if((bottom.ctrans != null) && (cmask > 0)) {
		Tile t = bottom.ctrans[cmask - 1].pick(rnd);
		if(gt instanceof WaterTile)
		    new BottomPlane(m, m.data(Bottom.id), lc, z, mat, t.tex());
		else
		    gt.layover(m, lc, gc, z, t);
	    }
	}
    }

    public static class TWaterTile extends WaterTile {
	private final static IDSet<GLState> bmats = new IDSet<GLState>();
	public final TerrainTile bottom;

	public TWaterTile(int id, Resource.Tileset set, int depth, TerrainTile bottom) {
	    super(id, set, depth);
	    this.bottom = bottom;
	}

	public class BottomPlane extends TerrainTile.Plane {
	    float[] depth;

	    public BottomPlane(MapMesh m, Bottom srf, Coord lc, int z, GLState mat, int[] alpha) {
		bottom.super(m, srf, lc, z, mat, alpha);
		this.depth = new float[] {
		    srf.d(lc.x, lc.y),
		    srf.d(lc.x, lc.y + 1),
		    srf.d(lc.x + 1, lc.y + 1),
		    srf.d(lc.x + 1, lc.y),
		};
	    }

	    public MeshBuf.Vertex mkvert(MeshBuf buf, int n) {
		MeshBuf.Vertex v = super.mkvert(buf, n);
		buf.layer(depthlayer).set(v, depth[n]);
		return(v);
	    }
	}

	public void lay(MapMesh m, Random rnd, Coord lc, Coord gc) {
	    TerrainTile.Blend b = m.data(bottom.blend);
	    for(int i = 0; i < bottom.var.length + 1; i++) {
		GLState mat = (i == 0)?bottom.base:(bottom.var[i - 1].mat);
		mat = bmats.intern(GLState.compose(mat, waterfog, boff));
		if(b.en[i][b.es.o(lc)])
		    new BottomPlane(m, m.data(Bottom.id), lc, i, mat, new int[] {
			    (int)(b.bv[i][b.vs.o(lc)] * 255),
			    (int)(b.bv[i][b.vs.o(lc.add(0, 1))] * 255),
			    (int)(b.bv[i][b.vs.o(lc.add(1, 1))] * 255),
			    (int)(b.bv[i][b.vs.o(lc.add(1, 0))] * 255),
			});
	    }
	    m.new Plane(m.gnd(), lc, 257, surfmat);
	}

	public void trans(MapMesh m, Random rnd, Tiler gt, Coord lc, Coord gc, int z, int bmask, int cmask) {
	}
    }
    */

    public GLState drawstate(Glob glob, GLConfig cfg, Coord3f c) {
	if(cfg.pref.wsurf.val)
	    return(obfog);
	return(null);
    }
}
