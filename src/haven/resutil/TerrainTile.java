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
import java.util.*;
import java.awt.Color;
import haven.MapMesh.Scan;
import haven.Resource.Tile;
import haven.Resource.Tileset;

public class TerrainTile extends Tiler {
    public final GLState base;
    public final SNoise3 noise;
    public final Var[] var;
    public final Tileset transset;

    public static class Var {
	public GLState mat;
	public double thrl, thrh;
	public double nz;

	public Var(GLState mat, double thrl, double thrh, double nz) {
	    this.mat = mat; this.thrl = thrl; this.thrh = thrh; this.nz = nz;
	}
    }

    private static final int sr = 12;
    public class Blend {
	final MapMesh m;
	final Scan vs, es;
	final float[][] bv;
	final boolean[][] en;

	private Blend(MapMesh m) {
	    this.m = m;
	    vs = new Scan(Coord.z.sub(sr, sr), m.sz.add(sr * 2 + 1, sr * 2 + 1));
	    float[][] buf1 = new float[var.length + 1][vs.l];
	    float[][] lwc = new float[var.length + 1][vs.l];
	    for(int i = 0; i < var.length + 1; i++) {
		for(int y = vs.ul.y; y < vs.br.y; y++) {
		    for(int x = vs.ul.x; x < vs.br.x; x++) {
			lwc[i][vs.o(x, y)] = (float)noise.getr(0.5, 1.5, 32, x + m.ul.x, y + m.ul.y, i * 23);
		    }
		}
	    }
	    setbase(buf1);
	    for(int i = 0; i < sr; i++) {
		float[][] buf2 = new float[var.length + 1][vs.l];
		for(int y = vs.ul.y; y < vs.br.y; y++) {
		    for(int x = vs.ul.x; x < vs.br.x; x++) {
			for(int o = 0; o < var.length + 1; o++) {
			    float s = buf1[o][vs.o(x, y)] * 4;
			    float w = 4;
			    float lw = lwc[o][vs.o(x, y)];
			    if(lw < 0)
				lw = lw * lw * lw;
			    else
				lw = lw * lw;
			    if(x > vs.ul.x) {
				s += buf1[o][vs.o(x - 1, y)] * lw;
				w += lw;
			    }
			    if(y > vs.ul.y) {
				s += buf1[o][vs.o(x, y - 1)] * lw;
				w += lw;
			    }
			    if(x < vs.br.x - 1) {
				s += buf1[o][vs.o(x + 1, y)] * lw;
				w += lw;
			    }
			    if(y < vs.br.y - 1) {
				s += buf1[o][vs.o(x, y + 1)] * lw;
				w += lw;
			    }
			    buf2[o][vs.o(x, y)] = s / w;
			}
		    }
		}
		buf1 = buf2;
	    }
	    bv = buf1;
	    for(int y = vs.ul.y; y < vs.br.y; y++) {
		for(int x = vs.ul.x; x < vs.br.x; x++) {
		    for(int i = 0; i < var.length + 1; i++) {
			float v = bv[i][vs.o(x, y)];
			v = v * 1.2f - 0.1f;
			if(v < 0)
			    v = 0;
			else if(v > 1)
			    v = 1;
			else
			    v = 0.25f + (0.75f * v);
			bv[i][vs.o(x, y)] = v;
		    }
		}
	    }
	    es = new Scan(Coord.z, m.sz);
	    en = new boolean[var.length + 1][es.l];
	    for(int y = es.ul.y; y < es.br.y; y++) {
		for(int x = es.ul.x; x < es.br.x; x++) {
		    boolean fall = false;
		    for(int i = var.length; i >= 0; i--) {
			if(fall) {
			    en[i][es.o(x, y)] = false;
			} else if((bv[i][vs.o(x    , y    )] < 0.001f) && (bv[i][vs.o(x + 1, y    )] < 0.001f) &&
				  (bv[i][vs.o(x    , y + 1)] < 0.001f) && (bv[i][vs.o(x + 1, y + 1)] < 0.001f)) {
			    en[i][es.o(x, y)] = false;
			} else {
			    en[i][es.o(x, y)] = true;
			    if((bv[i][vs.o(x    , y    )] > 0.99f) && (bv[i][vs.o(x + 1, y    )] > 0.99f) &&
			       (bv[i][vs.o(x    , y + 1)] > 0.99f) && (bv[i][vs.o(x + 1, y + 1)] > 0.99f)) {
				fall = true;
			    }
			}
		    }
		}
	    }
	}

	private void setbase(float[][] bv) {
	    for(int y = vs.ul.y; y < vs.br.y - 1; y++) {
		for(int x = vs.ul.x; x < vs.br.x - 1; x++) {
		    fall: {
			for(int i = var.length - 1; i >= 0; i--) {
			    Var v = var[i];
			    double n = 0;
			    for(double s = 64; s >= 8; s /= 2)
				n += noise.get(s, x + m.ul.x, y + m.ul.y, v.nz);
			    if(((n / 2) >= v.thrl) && ((n / 2) <= v.thrh)) {
				bv[i + 1][vs.o(x, y)] = 1;
				bv[i + 1][vs.o(x + 1, y)] = 1;
				bv[i + 1][vs.o(x, y + 1)] = 1;
				bv[i + 1][vs.o(x + 1, y + 1)] = 1;
				break fall;
			    }
			}
			bv[0][vs.o(x, y)] = 1;
			bv[0][vs.o(x + 1, y)] = 1;
			bv[0][vs.o(x, y + 1)] = 1;
			bv[0][vs.o(x + 1, y + 1)] = 1;
		    }
		}
	    }
	}
    }
    private final MapMesh.DataID<Blend> blend = new MapMesh.DataID<Blend>() {
	public Blend make(MapMesh m) {
	    return(new Blend(m));
	}
    };

    @ResName("trn")
    public static class Factory implements Tiler.Factory {
	public Tiler create(int id, Resource.Tileset set) {
	    Resource res = set.getres();
	    Tileset trans = null;
	    Material base = null;
	    Collection<Var> var = new LinkedList<Var>();
	    for(Object rdesc : set.ta) {
		Object[] desc = (Object[])rdesc;
		String p = (String)desc[0];
		if(p.equals("base")) {
		    int mid = (Integer)desc[1];
		    base = res.layer(Material.Res.class, mid).get();
		} else if(p.equals("var")) {
		    int mid = (Integer)desc[1];
		    double thrl, thrh;
		    if(desc[2] instanceof Object[]) {
			thrl = (Float)((Object[])desc[2])[0];
			thrh = (Float)((Object[])desc[2])[1];
		    } else {
			thrl = (Float)desc[2];
			thrh = Double.MAX_VALUE;
		    }
		    double nz = (res.name.hashCode() * mid * 8129) % 10000;
		    var.add(new Var(res.layer(Material.Res.class, mid).get(), thrl, thrh, nz));
		} else if(p.equals("trans")) {
		    Resource tres = Resource.load((String)desc[1], (Integer)desc[2]);
		    trans = tres.layer(Resource.tileset);
		}
	    }
	    return(new TerrainTile(id, res.name.hashCode(), base, var.toArray(new Var[0]), trans));
	}
    }

    public TerrainTile(int id, long nseed, GLState base, Var[] var, Tileset transset) {
	super(id);
	this.noise = new SNoise3(nseed);
	this.base = GLState.compose(base, States.vertexcolor);
	for(Var v : this.var = var)
	    v.mat = GLState.compose(v.mat, States.vertexcolor);
	this.transset = transset;
    }

    public class Plane extends MapMesh.Shape {
	public Coord lc;
	public MapMesh.SPoint[] vrt;
	public Coord3f[] tc;
	public int[] alpha;

	public Plane(MapMesh m, MapMesh.Surface surf, Coord sc, int z, GLState mat, int[] alpha) {
	    m.super(z, mat);
	    this.lc = new Coord(sc);
	    vrt = surf.fortile(sc);
	    float fac = 25f / 4f;
	    tc = new Coord3f[] {
		new Coord3f((sc.x + 0) / fac, (sc.y + 0) / fac, 0),
		new Coord3f((sc.x + 0) / fac, (sc.y + 1) / fac, 0),
		new Coord3f((sc.x + 1) / fac, (sc.y + 1) / fac, 0),
		new Coord3f((sc.x + 1) / fac, (sc.y + 0) / fac, 0),
	    };
	    m.data(BumpMap.MapTangents.id);
	    this.alpha = alpha;
	}

	public MeshBuf.Vertex mkvert(MeshBuf buf, int n) {
	    MeshBuf.Vertex v = buf.new Vertex(vrt[n].pos, vrt[n].nrm);
	    buf.layer(MeshBuf.tex).set(v, tc[n]);
	    buf.layer(MeshBuf.col).set(v, new Color(255, 255, 255, alpha[n]));
	    return(v);
	}

	public void build(MeshBuf buf) {
	    MeshBuf.Vertex v1 = mkvert(buf, 0);
	    MeshBuf.Vertex v2 = mkvert(buf, 1);
	    MeshBuf.Vertex v3 = mkvert(buf, 2);
	    MeshBuf.Vertex v4 = mkvert(buf, 3);
	    m().data(BumpMap.MapTangents.id).set(buf, lc, v1, v2, v3, v4);
	    MapMesh.splitquad(buf, v1, v2, v3, v4);
	}
    }

    public void lay(MapMesh m, Random rnd, Coord lc, Coord gc) {
	Blend b = m.data(blend);
	for(int i = 0; i < var.length + 1; i++) {
	    GLState mat = (i == 0)?base:(var[i - 1].mat);
	    if(b.en[i][b.es.o(lc)])
		new Plane(m, m.gnd(), lc, i, mat, new int[] {
			(int)(b.bv[i][b.vs.o(lc)] * 255),
			(int)(b.bv[i][b.vs.o(lc.add(0, 1))] * 255),
			(int)(b.bv[i][b.vs.o(lc.add(1, 1))] * 255),
			(int)(b.bv[i][b.vs.o(lc.add(1, 0))] * 255),
		    });
	}
    }

    public class TransPlane extends Plane {
	public Coord3f[] cc;

	public TransPlane(MapMesh m, MapMesh.Surface surf, Coord sc, int z, GLState mat, int[] alpha, Tex tex) {
	    super(m, surf, sc, z, mat, alpha);
	    Coord s = tex.sz();
	    cc = new Coord3f[] {
		new Coord3f(tex.tcx(0), tex.tcy(0), 0),
		new Coord3f(tex.tcx(0), tex.tcy(s.y), 0),
		new Coord3f(tex.tcx(s.x), tex.tcy(s.y), 0),
		new Coord3f(tex.tcx(s.x), tex.tcy(0), 0),
	    };
	}

	public MeshBuf.Vertex mkvert(MeshBuf buf, int n) {
	    MeshBuf.Vertex v = super.mkvert(buf, n);
	    buf.layer(AlphaTex.lclip).set(v, cc[n]);
	    return(v);
	}
    }

    private final static Map<TexGL, AlphaTex> transtex = new WeakHashMap<TexGL, AlphaTex>();
    private final static IDSet<GLState> transmats = new IDSet<GLState>();

    private void laytrans(MapMesh m, Coord lc, int z, Tile t) {
	Blend b = m.data(blend);
	for(int i = 0; i < var.length + 1; i++) {
	    GLState mat = (i == 0)?base:(var[i - 1].mat);
	    Tex tt = t.tex();
	    TexGL gt;
	    if(tt instanceof TexGL)
		gt = (TexGL)tt;
	    else if((tt instanceof TexSI) && (((TexSI)tt).parent instanceof TexGL))
		gt = (TexGL)((TexSI)tt).parent;
	    else
		throw(new RuntimeException("Cannot use texture for transitions: " + tt));
	    AlphaTex alpha;
	    synchronized(transtex) {
		if((alpha = transtex.get(gt)) == null)
		    transtex.put(gt, alpha = new AlphaTex(gt, 0.01f));
	    }
	    mat = transmats.intern(GLState.compose(mat, alpha));
	    if(b.en[i][b.es.o(lc)])
		new TransPlane(m, m.gnd(), lc, z + i, mat, new int[] {
			(int)(b.bv[i][b.vs.o(lc)] * 255),
			(int)(b.bv[i][b.vs.o(lc.add(0, 1))] * 255),
			(int)(b.bv[i][b.vs.o(lc.add(1, 1))] * 255),
			(int)(b.bv[i][b.vs.o(lc.add(1, 0))] * 255),
		    }, tt);
	}
    }

    public void trans(MapMesh m, Random rnd, Tiler gt, Coord lc, Coord gc, int z, int bmask, int cmask) {
	if(transset == null)
	    return;
	if(m.map.gettile(gc) <= id)
	    return;
	if((transset.btrans != null) && (bmask > 0))
	    laytrans(m, lc, z, transset.btrans[bmask - 1].pick(rnd));
	if((transset.ctrans != null) && (cmask > 0))
	    laytrans(m, lc, z, transset.ctrans[cmask - 1].pick(rnd));
    }
}
