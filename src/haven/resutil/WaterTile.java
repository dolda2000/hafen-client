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
import java.awt.image.BufferedImage;
import java.awt.Color;
import haven.*;
import haven.render.*;
import haven.render.sl.*;
import haven.MapMesh.Scan;
import haven.Surface.Vertex;
import haven.Surface.MeshVertex;
import haven.render.TextureCube.SamplerCube;
import static haven.render.sl.Cons.*;

public class WaterTile extends Tiler {
    public final int depth;
    private static final Pipe.Op bcol = new Light.PhongLight(true, new Color(128, 128, 128), new Color(255, 255, 255), new Color(0, 0, 0), new Color(0, 0, 0), 0);
    public final Tiler.MCons bottom;
    
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
		for(int x = ds.ul.x; x < ds.br.x; x++) {
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
		    int vd = ed[ss.o(x, y)];
		    surf[vs.o(x, y)] = new BottomVertex(ms, ms.surf[vs.o(x, y)].add(0, 0, -vd), vd);
		}
	    }
	    for(int y = ts.ul.y; y < ts.br.y; y++) {
		for(int x = ts.ul.x; x < ts.br.x; x++) {
		    split[ts.o(x, y)] = Math.abs(surf[vs.o(x, y)].z - surf[vs.o(x + 1, y + 1)].z) > Math.abs(surf[vs.o(x + 1, y)].z - surf[vs.o(x, y + 1)].z);
		}
	    }
	}

	public static class BottomVertex extends Vertex {
	    public final float d;

	    public BottomVertex(Surface surf, Coord3f c, float d) {
		surf.super(c);
		this.d = d;
	    }

	    public void modify(MeshBuf buf, MeshBuf.Vertex v) {
		buf.layer(depthlayer).set(v, d);
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

    static final SamplerCube sky;
    static final TexRender nrm;
    static {
	sky = new SamplerCube(new RUtils.CubeFill(() -> Resource.local().load("gfx/tiles/skycube").get().layer(Resource.imgc).img).mktex());
	nrm = Resource.local().loadwait("gfx/tiles/wnrm").layer(TexR.class).tex();
    }

    private static final State.Slot<State> surfslot = new State.Slot<>(State.Slot.Type.DRAW, State.class);
    private static final Pipe.Op surfextra = Pipe.Op.compose(new States.DepthBias(2, 2), FragColor.blend(new BlendMode(BlendMode.Factor.ONE, BlendMode.Factor.ONE)));
    public static class BetterSurface extends State {
	private final Uniform ssky = new Uniform(Type.SAMPLERCUBE, p -> sky);
	private final Uniform snrm = new Uniform(Type.SAMPLER2D, p -> nrm.img);
	private final Uniform icam = new Uniform(Type.MAT3, p -> Homo3D.camxf(p).transpose(), Homo3D.cam);

	private BetterSurface() {
	}

	private ShaderMacro shader = new ShaderMacro() {
		final AutoVarying skyc = new AutoVarying(Type.VEC3) {
			protected Expression root(VertexContext vctx) {
			    return(mul(icam.ref(), reflect(Homo3D.vertedir(vctx).depref(), Homo3D.get(vctx.prog).eyen.depref())));
			}
		    };
		public void modify(final ProgramContext prog) {
		    Homo3D.fragedir(prog.fctx);
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
								      add(mul(pick(Homo3D.fragmapv.ref(), "st"), vec2(l(0.01), l(0.012))),
									  mul(FrameInfo.time(), vec2(l(0.025), l(0.035))))),
							    "rgb"),
						       pick(texture2D(snrm.ref(),
								      add(mul(pick(Homo3D.fragmapv.ref(), "st"), vec2(l(0.019), l(0.018))),
									  mul(FrameInfo.time(), vec2(l(-0.035), l(-0.025))))),
							    "rgb")),
						   add(pick(texture2D(snrm.ref(),
								      add(mul(pick(Homo3D.fragmapv.ref(), "st"), vec2(l(0.01), l(0.012))),
									  add(mul(FrameInfo.time(), vec2(l(0.025), l(0.035))), vec2(l(0.5), l(0.5))))),
							    "rgb"),
						       pick(texture2D(snrm.ref(),
								      add(mul(pick(Homo3D.fragmapv.ref(), "st"), vec2(l(0.019), l(0.018))),
									  add(mul(FrameInfo.time(), vec2(l(-0.035), l(-0.025))), vec2(l(0.5), l(0.5))))),
							    "rgb")),
						   abs(sub(Cons.mod(FrameInfo.time(), l(2.0)), l(1.0)))),
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
		    Homo3D.frageyen(prog.fctx).mod(in -> {
			    Expression m = nmod.ref();
			    return(add(mul(pick(m, "x"), vec3(l(1.0), l(0.0), l(0.0))),
				       mul(pick(m, "y"), vec3(l(0.0), l(1.0), l(0.0))),
				       mul(pick(m, "z"), in)));
			}, -10);
		    FragColor.fragcol(prog.fctx).
			mod(in -> mul(in, textureCube(ssky.ref(),
						      neg(mul(icam.ref(), reflect(Homo3D.fragedir(prog.fctx).depref(),
										  Homo3D.frageyen(prog.fctx).depref())))),
				      l(0.4))
			    , 0);
		}
	    };

	public ShaderMacro shader() {return(shader);}

	public void apply(Pipe buf) {
	    buf.put(surfslot, this);
	    surfextra.apply(buf);
	}
    }

    public static final Pipe.Op surfmat = Pipe.Op.compose(new BetterSurface(), new Rendered.Order.Default(6000));

    public static final MeshBuf.LayerID<MeshBuf.Vec1Layer> depthlayer = new MeshBuf.V1LayerID(BottomFog.depth);

    public static class BottomFog extends State.StandAlone {
	public static final double maxdepth = 8; /* XXX: These should be parameterized. */
	public static final Color fogcolor = new Color(0, 16, 48);
	/* XXXRENDER
	public static final Expression mfogcolor = mul(col3(fogcolor), pick(fref(idx(ProgramContext.gl_LightSource.ref(), MapView.amblight.ref()), "diffuse"), "rgb"));
	*/
	public static final Expression mfogcolor = col3(fogcolor);
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

	private final ShaderMacro shader = prog -> {
	    FragColor.fragcol(prog.fctx).mod(in -> rgbmix.call(in, mfogcolor, min(div(fragd.ref(), l(maxdepth)), l(1.0))), 1000);
	};

	private BottomFog() {
	    super(Slot.Type.DRAW);
	}

	public ShaderMacro shader() {return(shader);}
    }
    public static final BottomFog waterfog = new BottomFog();
    private static final Pipe.Op botmat = Pipe.Op.compose(waterfog, new States.DepthBias(4, 4));

    public static final Pipe.Op obfog = new State.StandAlone(State.Slot.Type.DRAW) {
	    {
		slot.instanced = new Instancable<StandAlone>() {
			Instancer<StandAlone> dummy = Instancer.dummy();
			public Instancer<StandAlone> instid(StandAlone st) {
			    if(st == null)
				return(dummy);
			    return(null);
			}
		    };
	    }
	final AutoVarying fragd = new AutoVarying(Type.FLOAT) {
		protected Expression root(VertexContext vctx) {
		    return(sub(pick(MapView.maploc.ref(), "z"), pick(Homo3D.get(vctx.prog).mapv.depref(), "z")));
		}
	    };

	final ShaderMacro shader = prog -> {
	    FragColor.fragcol(prog.fctx).mod(in -> BottomFog.rgbmix.call(in, BottomFog.mfogcolor, clamp(div(fragd.ref(), l(BottomFog.maxdepth)), l(0.0), l(1.0))), 1000);
	};
	public ShaderMacro shader() {return(shader);}
    };

    @ResName("water")
    public static class Fac implements Factory {
	public Tiler create(int id, Tileset set) {
	    int a = 0;
	    int depth = (Integer)set.ta[a++];
	    Tiler.MCons bottom = new GroundTile(id, set);
	    while(a < set.ta.length) {
		Object[] desc = (Object[])set.ta[a++];
		String p = (String)desc[0];
		if(p.equals("bottom") /* Backwards compatibility */ || p.equals("gnd") || p.equals("trn")) {
		    Resource bres = set.getres().pool.load((String)desc[1], (Integer)desc[2]).get();
		    Tileset ts = bres.layer(Tileset.class);
		    Tiler b = ts.tfac().create(id, ts);
		    bottom = (Tiler.MCons)b;
		}
	    }
	    return(new WaterTile(id, bottom, depth));
	}
    }

    public WaterTile(int id, Tiler.MCons bottom, int depth) {
	super(id);
	this.bottom = bottom;
	this.depth = depth;
    }

    @Deprecated
    public WaterTile(int id, Tileset set, int depth) {
	this(id, new GroundTile(0, set), depth);
    }

    public void lay(MapMesh m, Random rnd, Coord lc, Coord gc) {
	MapMesh.MapSurface ms = m.data(MapMesh.gnd);
	SModel smod = SModel.get(m, surfmat, VertFactory.id);
	MPart d = MPart.splitquad(lc, gc, ms.fortilea(lc), ms.split[ms.ts.o(lc)]);
	MeshVertex[] v = smod.get(d);
	smod.new Face(v[d.f[0]], v[d.f[1]], v[d.f[2]]);
	smod.new Face(v[d.f[3]], v[d.f[4]], v[d.f[5]]);
	Bottom b = m.data(Bottom.id);
	MPart bd = MPart.splitquad(lc, gc, b.fortilea(lc), ms.split[ms.ts.o(lc)]);
	bd.mat = botmat;
	bottom.faces(m, bd);
    }

    public void trans(MapMesh m, Random rnd, Tiler gt, Coord lc, Coord gc, int z, int bmask, int cmask) {
	if(m.map.gettile(gc) <= id)
	    return;
	if(gt instanceof WaterTile) {
	    if(bottom instanceof CTrans) {
		MapMesh.MapSurface ms = m.data(MapMesh.gnd);
		Bottom b = m.data(Bottom.id);
		MPart d = MPart.splitquad(lc, gc, b.fortilea(lc), ms.split[ms.ts.o(lc)]);
		d.mat = botmat;
		((CTrans)bottom).tcons(z, bmask, cmask).faces(m, d);
	    }
	} else {
	    if(bottom instanceof Tiler)
		((Tiler)bottom).trans(m, rnd, gt, lc, gc, z, bmask, cmask);
	}
    }

    public Pipe.Op drawstate(Glob glob, Coord3f c) {
	/* XXXRENDER
	if(cfg.pref.wsurf.val)
	    return(obfog);
	return(null);
	*/
	return(obfog);
    }
}
