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
import static haven.Coord.upcw;

public class WaterTile extends Tiler {
    public final int depth;
    public final Tiler.MCons bottom;

    public static class FlowData {
	public static final float[] nxpcw, nypcw;
	public static final int I = 10;
	public final float[] xv, yv;
	public final Scan vs;

	static {
	    nxpcw = new float[8];
	    nypcw = new float[8];
	    for(int v = 0; v < 8; v++) {
		float l = (float)Math.hypot(upcw[v].x, upcw[v].y);
		nxpcw[v] = upcw[v].x / l;
		nypcw[v] = upcw[v].y / l;
	    }
	}

	public class Field {
	    public final MapMesh m;
	    public final MCache map;
	    public final float[] xs, ys;
	    public float[] xv, yv;
	    public final boolean[] wv;
	    public final Scan vs, fs, ts;

	    public Field(MapMesh m) {
		this.m = m;
		map = m.map;
		vs = new Scan(Coord.z, m.sz.add(1, 1));
		fs = new Scan(Coord.of(-I, -I), m.sz.add(1 + (I * 2), 1 + (I * 2)));
		ts = new Scan(Coord.of(-I - 1, -I - 1), m.sz.add(3 + (I * 2), 3 + (I * 2)));
		xs = new float[fs.l];
		ys = new float[fs.l];
		xv = new float[fs.l];
		yv = new float[fs.l];
		wv = new boolean[ts.l];
	    }

	    public void calc() {
		water();
		slopes();
		for(int i = 0; i < I; i++)
		    iter();
	    }

	    private void water() {
		for(int y = -I - 1; y < m.sz.y + I + 1; y++) {
		    for(int x = -I - 1; x < m.sz.x + I + 1; x++) {
			if(map.tiler(map.gettile(m.ul.add(x, y))) instanceof WaterTile) {
			    wv[ts.o(x + 0, y + 0)] = true;
			    wv[ts.o(x + 1, y + 0)] = true;
			    wv[ts.o(x + 1, y + 1)] = true;
			    wv[ts.o(x + 0, y + 1)] = true;
			}
		    }
		}
	    }

	    private void slopes() {
		for(int y = -I; y <= m.sz.y + I; y++) {
		    for(int x = -I; x <= m.sz.x + I; x++) {
			if(!wv[ts.o(x, y)])
			    continue;
			double tz = map.getfz(m.ul.add(x, y));
			if(wv[ts.o(x - 1, y)])
			    xs[fs.o(x, y)] += map.getfz(m.ul.add(x - 1, y)) - tz;
			if(wv[ts.o(x + 1, y)])
			    xs[fs.o(x, y)] += tz - map.getfz(m.ul.add(x + 1, y));
			if(wv[ts.o(x, y - 1)])
			    ys[fs.o(x, y)] += map.getfz(m.ul.add(x, y - 1)) - tz;
			if(wv[ts.o(x, y + 1)])
			    ys[fs.o(x, y)] += tz - map.getfz(m.ul.add(x, y + 1));
		    }
		}
	    }

	    private void iter() {
		float[] nxv = new float[fs.l];
		float[] nyv = new float[fs.l];
		float[] p = new float[fs.l];
		for(int y = -I; y <= m.sz.y + I; y++) {
		    for(int x = -I; x <= m.sz.x + I; x++) {
			int O = fs.o(x, y);
			if(!wv[ts.o(x, y)])
			    continue;
			float xs = Utils.clip(this.xs[O], -25f, 25f), ys = Utils.clip(this.ys[O], -25f, 25f);
			nxv[O] = xv[O] + xs;
			nyv[O] = yv[O] + ys;
			float nv = (float)Math.hypot(nxv[O], nyv[O]);
			float mv = Math.max((float)Math.hypot(xs, ys),
					    (float)Math.hypot(xv[O], yv[O]));
			if(nv > mv) {
			    nxv[O] *= mv / nv;
			    nyv[O] *= mv / nv;
			}
		    }
		}
		for(int y = -I; y <= m.sz.y + I; y++) {
		    for(int x = -I; x <= m.sz.x + I; x++) {
			int O = fs.o(x, y);
			if(!wv[ts.o(x, y)])
			    continue;
			for(int i = 0; i < 8; i++) {
			    int X = x + upcw[i].x, Y = y + upcw[i].y;
			    if(fs.has(X, Y) && wv[ts.o(X, Y)])
				p[O] += (nxv[fs.o(X, Y)] * -nxpcw[i]) + (nyv[fs.o(X, Y)] * -nypcw[i]);
			}
		    }
		}
		float PF = 0.75f;
		for(int y = -I; y <= m.sz.y + I; y++) {
		    for(int x = -I; x <= m.sz.x + I; x++) {
			int O = fs.o(x, y);
			if(!wv[ts.o(x, y)])
			    continue;
			int n = 0;
			for(int i = 0; i < 8; i++) {
			    if(wv[ts.o(upcw[i].add(x, y))])
				n++;
			}
			for(int i = 0; i < 8; i++) {
			    int X = x + upcw[i].x, Y = y + upcw[i].y;
			    if(fs.has(X, Y) && wv[ts.o(X, Y)]) {
				nxv[fs.o(X, Y)] += p[O] * PF * nxpcw[i] / n;
				nyv[fs.o(X, Y)] += p[O] * PF * nypcw[i] / n;
			    }
			}
		    }
		}
		xv = nxv;
		yv = nyv;
	    }
	}

	public FlowData(MapMesh m) {
	    Field f = new Field(m);
	    f.calc();
	    this.vs = f.vs;
	    this.xv = new float[vs.l];
	    this.yv = new float[vs.l];
	    for(int y = 0; y <= m.sz.y; y++) {
		for(int x = 0; x <= m.sz.x; x++) {
		    xv[vs.o(x, y)] = f.xv[f.fs.o(x, y)];
		    yv[vs.o(x, y)] = f.yv[f.fs.o(x, y)];
		}
	    }
	}

	public Coord3f vel(Coord tc) {
	    return(Coord3f.of(xv[vs.o(tc)], -yv[vs.o(tc)], 0));
	}

	public static final MapMesh.DataID<FlowData> id = MapMesh.makeid(FlowData.class);
    }

    public static class BottomData implements MapMesh.ConsHooks {
	public final float[] depth;
	public final Scan ds;

	public BottomData(MapMesh m) {
	    ds = new Scan(Coord.z, m.sz.add(1, 1));
	    depth = new float[ds.l];
	}

	public boolean clean() {
	    return(true);
	}

	public static final MapMesh.DataID<BottomData> id = MapMesh.makeid(BottomData.class);
    }

    public static class Bottom implements MapMesh.ConsHooks {
	final MapMesh m;
	final boolean[] s;
	final Vertex[] surf;
	final boolean[] split;
	float[] ed;
	final Scan vs, ss;
	final BottomData prs;
	
	public Bottom(MapMesh m) {
	    this.prs = m.data(BottomData.id);
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
	    float[] d = new float[ds.l];
	    s = new boolean[ss.l];
	    ed = new float[ss.l];
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
		    float td = d[ds.o(x, y)];
		    td = Math.min(td, d[ds.o(x - 1, y - 1)]);
		    td = Math.min(td, d[ds.o(x, y - 1)]);
		    td = Math.min(td, d[ds.o(x - 1, y)]);
		    ed[ss.o(x, y)] = td;
		    if(td == 0)
			s[ss.o(x, y)] = true;
		}
	    }
	    for(int i = 0; i < 8; i++) {
		float[] sd = new float[ss.l];
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
		    float vd = ed[ss.o(x, y)];
		    surf[vs.o(x, y)] = new BottomVertex(ms, ms.surf[vs.o(x, y)].add(0, 0, -vd), vd);
		}
	    }
	    for(int y = ts.ul.y; y < ts.br.y; y++) {
		for(int x = ts.ul.x; x < ts.br.x; x++) {
		    split[ts.o(x, y)] = Math.abs(surf[vs.o(x, y)].z - surf[vs.o(x + 1, y + 1)].z) > Math.abs(surf[vs.o(x + 1, y)].z - surf[vs.o(x, y + 1)].z);
		}
	    }

	    for(int y = prs.ds.ul.y; y < prs.ds.br.y; y++) {
		for(int x = prs.ds.ul.x; x < prs.ds.br.x; x++)
		    prs.depth[prs.ds.o(x, y)] = ed[ss.o(x, y)];
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

	public float d(int x, int y) {
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

    static final SamplerCube sky = new SamplerCube(new RUtils.CubeFill(() -> Resource.local().load("gfx/tiles/skycube").get().layer(Resource.imgc).img).mktex());
    static final TexRender nrm = Resource.local().loadwait("gfx/tiles/wnrm").layer(TexR.class).tex();
    static final TexRender flow = Resource.local().loadwait("gfx/tiles/wfoam").layer(TexR.class).tex();

    private static final State.Slot<State> surfslot = new State.Slot<>(State.Slot.Type.DRAW, State.class);
    private static final Pipe.Op surfextra = Pipe.Op.compose(new States.DepthBias(2, 2), new States.Facecull());
    private static final Pipe.Op baseextra = Pipe.Op.compose(surfextra, FragColor.blend(new BlendMode(BlendMode.Factor.ONE, BlendMode.Factor.ONE)));
    public static class BaseSurface extends State {
	private final Uniform ssky = new Uniform(Type.SAMPLERCUBE, p -> sky);
	private final Uniform snrm = new Uniform(Type.SAMPLER2D, p -> nrm.img);
	private final Uniform icam = new Uniform(Type.MAT3, p -> Homo3D.camxf(p).transpose(), Homo3D.cam);

	private BaseSurface() {
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
								  add(mul(pick(Homo3D.fragmapv.ref(), "st"), vec2(l(0.01), l(0.012))),
								      mul(Cons.mod(FrameInfo.time(), l(2.0)), vec2(l(0.025), l(0.035))))),
							"rgb"),
						   pick(texture2D(snrm.ref(),
								  add(mul(pick(Homo3D.fragmapv.ref(), "st"), vec2(l(0.019), l(0.018))),
								      mul(Cons.mod(add(FrameInfo.time(), l(1.0)), l(2.0)), vec2(l(-0.035), l(-0.025))))),
							"rgb"),
						   abs(sub(Cons.mod(FrameInfo.time(), l(2.0)), l(1.0)))),
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
								  add(mul(pick(Homo3D.fragmapv.ref(), "st"), vec2(l(0.01), l(0.012))),
								      mul(FrameInfo.time(), vec2(l(0.025), l(0.035))))),
							"rgb"),
						   pick(texture2D(snrm.ref(),
								  add(mul(pick(Homo3D.fragmapv.ref(), "st"), vec2(l(0.019), l(0.018))),
								      mul(FrameInfo.time(), vec2(l(-0.035), l(-0.025))))),
							"rgb")),
					       l(0.5 * 2)), vec3(l(1.0 / 16), l(1.0 / 16), l(1.0))));
				*/
				/*
				return(mul(sub(pick(texture2D(snrm.ref(),
							      mul(pick(Homo3D.fragmapv.ref(), "st"), l(0.005))),
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
		    FragColor.fragcol(prog.fctx)
			.mod(in -> mul(in, textureCube(ssky.ref(),
						       neg(mul(icam.ref(), reflect(Homo3D.fragedir(prog.fctx).depref(),
										   Homo3D.frageyen(prog.fctx).depref())))),
				       l(0.4)),
			     0);
		}
	    };

	public ShaderMacro shader() {return(shader);}

	public void apply(Pipe buf) {
	    buf.put(surfslot, this);
	    baseextra.apply(buf);
	}
    }
    public static final Pipe.Op surfmat = Pipe.Op.compose(new BaseSurface(), new Rendered.Order.Default(6000));

    private static final Pipe.Op foamextra = Pipe.Op.compose(surfextra, FragColor.blend(new BlendMode(BlendMode.Factor.ONE, BlendMode.Factor.ONE)),
							     new Light.PhongLight(true, new Color(255, 255, 255), new Color(128, 128, 128), new Color(0, 0, 0), new Color(0, 0, 0), 0),
							     ShadowMap.maskshadow);
    public static class FoamSurface extends State {
	public static final Attribute[] vertv = new Attribute[4];
	@SuppressWarnings("unchecked")
	public static final MeshBuf.LayerID<MeshBuf.Vec2Layer>[] lvertv = new MeshBuf.LayerID[4];
	public static final AutoVarying[] vvertv = new AutoVarying[4];
	public static final AutoVarying[] vverti = new AutoVarying[4];
	public static final Attribute vipol = new Attribute(Type.VEC2);
	public static final MeshBuf.LayerID<MeshBuf.Vec2Layer> lvipol = new MeshBuf.V2LayerID(vipol);
	private final Uniform ssky = new Uniform(Type.SAMPLERCUBE, p -> sky);
	private final Uniform snrm = new Uniform(Type.SAMPLER2D, p -> nrm.img);
	private final Uniform sflow = new Uniform(Type.SAMPLER2D, p -> flow.img);
	private final Uniform icam = new Uniform(Type.MAT3, p -> Homo3D.camxf(p).transpose(), Homo3D.cam);

	static {
	    for(int I = 0; I < 4; I++) {
		int i = I;
		vertv[i] = new Attribute(Type.VEC2);
		lvertv[i] = new MeshBuf.V2LayerID(vertv[i]);
		vvertv[i] = new AutoVarying(Type.VEC2) {
			public Expression root(VertexContext vctx) {
			    return(vertv[i].ref());
			}
		    };
		vverti[i] = new AutoVarying(Type.FLOAT) {
			public Expression root(VertexContext vctx) {
			    return(min(mul(length(vertv[i].ref()), l(0.06)), l(1.0)));
			}
		    };
	    }
	}

	private FoamSurface() {
	}

	private ShaderMacro shader = new ShaderMacro() {
		final AutoVarying skyc = new AutoVarying(Type.VEC3) {
			protected Expression root(VertexContext vctx) {
			    return(mul(icam.ref(), reflect(Homo3D.vertedir(vctx).depref(), Homo3D.get(vctx.prog).eyen.depref())));
			}
		    };
		AutoVarying vvipol = new AutoVarying(Type.VEC2) {
			protected Expression root(VertexContext vctx) {return(vipol.ref());}
		    };
		public void modify(final ProgramContext prog) {
		    double fres = 0.1;
		    FragColor.fragcol(prog.fctx)
			.mod(in -> mul(in, vec4(vec3(mix(mix(mul(pick(texture2D(sflow.ref(),
									    add(mul(pick(Homo3D.fragmapv.ref(), "st"), vec2(l(fres), l(fres))),
										mul(FrameInfo.time(), mul(vvertv[0].ref(), l(-0.1))))),
								      "r"),
								 vverti[0].ref()),
							     mul(pick(texture2D(sflow.ref(),
										add(mul(pick(Homo3D.fragmapv.ref(), "st"), vec2(l(fres), l(fres))),
										    mul(FrameInfo.time(), mul(vvertv[1].ref(), l(-0.1))))),
								      "r"),
								 vverti[1].ref()),
							     pick(vvipol.ref(), "x")),
							 mix(mul(pick(texture2D(sflow.ref(),
										add(mul(pick(Homo3D.fragmapv.ref(), "st"), vec2(l(fres), l(fres))),
										    mul(FrameInfo.time(), mul(vvertv[3].ref(), l(-0.1))))),
								      "r"),
								 vverti[3].ref()),
							     mul(pick(texture2D(sflow.ref(),
										add(mul(pick(Homo3D.fragmapv.ref(), "st"), vec2(l(fres), l(fres))),
										    mul(FrameInfo.time(), mul(vvertv[2].ref(), l(-0.1))))),
								      "r"),
								 vverti[2].ref()),
							     pick(vvipol.ref(), "x")),
							 pick(vvipol.ref(), "y"))), l(1.0))),
			     0);
		}
	    };

	public ShaderMacro shader() {return(shader);}


	public void apply(Pipe buf) {
	    buf.put(surfslot, this);
	    foamextra.apply(buf);
	}
    }
    public static final Pipe.Op foammat = Pipe.Op.compose(new FoamSurface(), new Rendered.Order.Default(6001));

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

    public static class ObFog extends State implements InstanceBatch.AttribState {
	public static final Slot<ObFog> slot = new Slot<>(State.Slot.Type.DRAW, ObFog.class)
	    .instanced(new Instancable<ObFog>() {
		    final Instancer<ObFog> nil = Instancer.dummy();
		    public Instancer<ObFog> instid(ObFog st) {
			return((st == null) ? nil : instancer);
		    }
		});
	public final float basez;

	public ObFog(float basez) {
	    this.basez = basez;
	}

	public boolean equals(ObFog that) {return(this.basez == that.basez);}
	public boolean equals(Object x) {return((x instanceof ObFog) && equals((ObFog)x));}
	public int hashCode() {return(Float.floatToIntBits(basez));}

	private static final InstancedUniform cbasez = new InstancedUniform.Float1("basez", p -> p.get(slot).basez, slot);
	private static final AutoVarying fragd = new AutoVarying(Type.FLOAT) {
		protected Expression root(VertexContext vctx) {
		    return(sub(cbasez.ref(), pick(Homo3D.get(vctx.prog).mapv.depref(), "z")));
		}
	    };
	private static final ShaderMacro shader = prog -> {
	    FragColor.fragcol(prog.fctx).mod(in -> BottomFog.rgbmix.call(in, BottomFog.mfogcolor, clamp(div(fragd.ref(), l(BottomFog.maxdepth)), l(0.0), l(1.0))), 1000);
	};
	public ShaderMacro shader() {return(shader);}

	public void apply(Pipe p) {p.put(slot, this);}

	private static final Instancer<ObFog> instancer = new Instancer<ObFog>() {
		final ObFog instanced = new ObFog(0) {
		    final ShaderMacro shader = ShaderMacro.compose(mkinstanced, ObFog.shader);
		    public ShaderMacro shader() {return(shader);}
		};

		public ObFog inststate(ObFog uinst, InstanceBatch bat) {
		    return(instanced);
		}
	    };
	public InstancedAttribute[] attribs() {
	    return(new InstancedAttribute[] {cbasez.attrib});
	}
    }

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
		    Tileset ts = bres.flayer(Tileset.class);
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

    public void lay(MapMesh m, Random rnd, Coord lc, Coord gc) {
	MapMesh.MapSurface ms = m.data(MapMesh.gnd);
	MPart d = MPart.splitquad(lc, gc, ms.fortilea(lc), ms.split[ms.ts.o(lc)]);

	{
	    MeshBuf mesh = MapMesh.Model.get(m, surfmat);
	    MeshVertex[] mv = new MeshVertex[d.v.length];
	    for(int i = 0; i < d.v.length; i++)
		mv[i] = new MeshVertex(mesh, d.v[i]);
	    for(int i = 0; i < d.f.length; i += 3)
		mesh.new Face(mv[d.f[i]], mv[d.f[i + 1]], mv[d.f[i + 2]]);
	}

	foam: {
	    FlowData sd = m.data(FlowData.id);
	    skip: {
		for(int i = 0; i < 4; i++) {
		    if(!sd.vel(d.lc.add(Coord.uccw[i])).equals(Coord3f.o))
			break skip;
		}
		break foam;
	    }
	    MeshBuf mesh = MapMesh.Model.get(m, foammat);
	    MeshVertex[] mv = new MeshVertex[d.v.length];
	    MeshBuf.Vec2Layer[] vertv = new MeshBuf.Vec2Layer[4];
	    for(int i = 0; i < 4; i++)
		vertv[i] = mesh.layer(FoamSurface.lvertv[i]);
	    MeshBuf.Vec2Layer vipol = mesh.layer(FoamSurface.lvipol);
	    for(int i = 0; i < d.v.length; i++) {
		mv[i] = new MeshVertex(mesh, d.v[i]);
		for(int o = 0; o < 4; o++)
		    vertv[o].set(mv[i], sd.vel(d.lc.add(Coord.uccw[o])));
		vipol.set(mv[i], Coord3f.of(d.tcx[i], d.tcy[i], 0));
	    }
	    for(int i = 0; i < d.f.length; i += 3)
		mesh.new Face(mv[d.f[i]], mv[d.f[i + 1]], mv[d.f[i + 2]]);
	}

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

    public static class BottomSurface extends MapZSurface {
	public final BottomData b;

	public BottomSurface(MapMesh m) {
	    super(m);
	    this.b = m.data(BottomData.id);
	}

	public double getz(Coord tc) {
	    return(super.getz(tc) - b.depth[b.ds.o(tc.sub(m.ul))]);
	}
    }

    public MCache.ZSurface getsurf(MapMesh m, MCache.SurfaceID id) {
	if(id.hasparent(MCache.SurfaceID.trn))
	    return(new BottomSurface(m));
	return(super.getsurf(m, id));
    }

    public static final Pipe.Op clickstate = Pipe.Op.compose(MapMesh.clickpost, States.maskdepth);
    public Pipe.Op clickstate() {return(clickstate);}

    public Pipe.Op drawstate(Glob glob, Coord3f c) {
	float mz = glob.map.getcz(c.x, c.y);
	return(new ObFog(mz));
    }
}
