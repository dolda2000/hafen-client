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
import haven.MapMesh.Surface;
import javax.media.opengl.*;
import java.awt.Color;

public class WaterTile extends Tiler {
    public final int depth;
    public final Resource.Tileset set;
    public final GLState mat;
    private static final Material.Colors bcol = new Material.Colors(new Color(128, 128, 128), new Color(255, 255, 255), new Color(0, 0, 0), new Color(0, 0, 0));
    
    public static class Bottom extends Surface {
	final MapMesh m;
	final boolean[] s;
	final int[] ed;
	
	public Bottom(MapMesh m) {
	    m.super();
	    this.m = m;
	    Coord sz = m.sz;
	    MCache map = m.map;
	    int[] d = new int[(sz.x + 5) * (sz.y + 5)];
	    s = new boolean[(sz.x + 3) * (sz.y + 3)];
	    ed = new int[(sz.x + 3) * (sz.y + 3)];
	    Coord c = new Coord();
	    int i = 0;
	    for(c.y = -2; c.y <= sz.y + 2; c.y++) {
		for(c.x = -2; c.x <= sz.x + 2; c.x++) {
		    Tiler t = map.tiler(map.gettile(c.add(m.ul)));
		    if(t instanceof WaterTile)
			d[i] = ((WaterTile)t).depth;
		    else
			d[i] = 0;
		    i++;
		}
	    }
	    i = 1 + (sz.x + 5);
	    int r = sz.x + 5;
	    for(c.y = -1; c.y <= sz.y + 1; c.y++) {
		for(c.x = -1; c.x <= sz.x + 1; c.x++) {
		    int td = d[i];
		    if(d[i - r - 1] < td)
			td = d[i - r - 1];
		    if(d[i - r] < td)
			td = d[i - r];
		    if(d[i - 1] < td)
			td = d[i - 1];
		    spoint(c).pos.z -= td;
		    ed[idx(c)] = td;
		    if(td == 0)
			s[idx(c)] = true;
		    i++;
		}
		i += 2;
	    }
	}

	public int d(int x, int y) {
	    return(ed[(x + 1) + ((y + 1) * (m.sz.x + 3))]);
	}
	
	public void calcnrm() {
	    super.calcnrm();
	    Coord c = new Coord();
	    for(c.y = 0; c.y <= m.sz.y; c.y++) {
		for(c.x = 0; c.x <= m.sz.x; c.x++) {
		    if(s[idx(c)])
			spoint(c).nrm = m.gnd().spoint(c).nrm;
		}
	    }
	}
    }
    
    static final TexCube sky = new TexCube(Resource.loadimg("gfx/tiles/skycube"));
    static final Tex srf = Resource.loadtex("gfx/tiles/watertex");
    private static States.DepthOffset soff = new States.DepthOffset(2, 2);
    public static final GLState surfmat = new GLState.StandAlone(GLState.Slot.Type.DRAW, PView.cam) {
	    private TexUnit sampler;

	    public void apply(GOut g) {
		GL2 gl = g.gl;
		(sampler = g.st.texalloc()).act();
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
		sampler.act();
		g.st.matmode(GL.GL_TEXTURE);
		gl.glPopMatrix();
		gl.glDisable(GL.GL_TEXTURE_CUBE_MAP);
		gl.glDisable(GL2.GL_TEXTURE_GEN_S);
		gl.glDisable(GL2.GL_TEXTURE_GEN_T);
		gl.glDisable(GL2.GL_TEXTURE_GEN_R);
		gl.glColor3f(1, 1, 1);
		sampler.free(); sampler = null;
	    }
	    
	    public void prep(Buffer buf) {
		buf.put(States.color, null);
		buf.put(Light.lighting, null);
		soff.prep(buf);
		super.prep(buf);
	    }
	};
    public static class Shallows extends WaterTile {
	public Shallows(int id, Resource.Tileset set) {
	    super(id, set, 5);
	}
    }
    
    public static class Deep extends WaterTile {
	public Deep(int id, Resource.Tileset set) {
	    super(id, set, 30);
	}
    }

    public static class DepthLayer extends MeshBuf.Layer<Float> {
	public DepthLayer(MeshBuf buf) {buf.super();}

	public VertexBuf.Vec1Array build(Collection<Float> in) {
	    java.nio.FloatBuffer data = Utils.mkfbuf(in.size());
	    for(Float d : in)
		data.put((d == null)?0:d);
	    return(new VertexBuf.Vec1Array(data, BottomFog.depth));
	}
    }
    public static final MeshBuf.LayerID<DepthLayer> depthlayer = new MeshBuf.LayerID<DepthLayer>(DepthLayer.class);

    public static class BottomFog extends GLState.StandAlone {
	public static final double maxdepth = 25;
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
				return(mix(in, vec4(l(0.05), l(0.15), l(0.10), l(1.0)), min(div(fragd.ref(), l(maxdepth)), l(1.0))));
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
    }
    public static final BottomFog waterfog = new BottomFog();
    
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
	    DepthLayer da = buf.layer(depthlayer);
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

    public WaterTile(int id, Resource.Tileset set, int depth) {
	super(id);
	this.set = set;
	this.depth = depth;
	TexGL tex = (TexGL)((TexSI)set.ground.pick(0).tex()).parent;
	mat = new Material(Light.deflight, bcol, tex.draw(), waterfog);
    }
    
    public void lay(MapMesh m, Random rnd, Coord lc, Coord gc) {
	Tile g = set.ground.pick(rnd);
	new BottomPlane(m, m.surf(Bottom.class), lc, 0, mat, g.tex());
	m.new Plane(m.gnd(), lc, 257, surfmat);
    }
    
    public void trans(MapMesh m, Random rnd, Tiler gt, Coord lc, Coord gc, int z, int bmask, int cmask) {
	if(m.map.gettile(gc) <= id)
	    return;
	if((set.btrans != null) && (bmask > 0)) {
	    Tile t = set.btrans[bmask - 1].pick(rnd);
	    if(gt instanceof WaterTile)
		new BottomPlane(m, m.surf(Bottom.class), lc, z, mat, t.tex());
	    else
		gt.layover(m, lc, gc, z, t);
	}
	if((set.ctrans != null) && (cmask > 0)) {
	    Tile t = set.ctrans[cmask - 1].pick(rnd);
	    if(gt instanceof WaterTile)
		new BottomPlane(m, m.surf(Bottom.class), lc, z, mat, t.tex());
	    else
		gt.layover(m, lc, gc, z, t);
	}
    }

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
				return(mix(in, vec4(l(0.05), l(0.15), l(0.10), l(1.0)), clamp(div(fragd.ref(), l(BottomFog.maxdepth)), l(0.0), l(1.0))));
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

    public GLState drawstate(Coord3f c) {
	return(obfog);
    }
}
