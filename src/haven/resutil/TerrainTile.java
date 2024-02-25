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
import java.util.*;
import java.awt.Color;
import haven.MapMesh.Scan;
import haven.Tileset.Tile;
import static haven.resutil.GroundTile.tcx;
import static haven.resutil.GroundTile.tcy;

public class TerrainTile extends Tiler implements Tiler.MCons, Tiler.CTrans {
    public final NodeWrap base;
    public final SNoise3 noise;
    public final Var[] var;
    public final Tileset transset;
    public final Pipe.Op draw;

    public static class Var {
	public NodeWrap mat;
	public double thrl, thrh;
	public double nz;
	public Pipe.Op draw;

	public Var(NodeWrap mat, double thrl, double thrh, double nz) {
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

	final VertFactory[] lvfac = new VertFactory[var.length + 1]; {
	    for(int i = 0; i < var.length + 1; i++) {
		final int l = i;
		lvfac[i] = new VertFactory() {
			final float fac = 25f / 4f;

			float bv(Coord lc, float tcx, float tcy) {
			    float icx = 1 - tcx, icy = 1 - tcy;
			    return((((bv[l][vs.o(lc.x + 0, lc.y + 0)] * icx) + (bv[l][vs.o(lc.x + 1, lc.y + 0)] * tcx)) * icy) +
				   (((bv[l][vs.o(lc.x + 0, lc.y + 1)] * icx) + (bv[l][vs.o(lc.x + 1, lc.y + 1)] * tcx)) * tcy));
			}

			public Surface.MeshVertex make(MeshBuf buf, MPart d, int i) {
			    Surface.MeshVertex ret = new Surface.MeshVertex(buf, d.v[i]);
			    Coord3f tan = Coord3f.yu.cmul(ret.nrm).norm();
			    Coord3f bit = ret.nrm.cmul(Coord3f.xu).norm();
			    Coord3f tc = new Coord3f((d.lc.x + d.tcx[i]) / fac, (d.lc.y + d.tcy[i]) / fac, 0);
			    int alpha = (int)(bv(d.lc, d.tcx[i], d.tcy[i]) * 255);
			    buf.layer(BumpMap.ltan).set(ret, tan);
			    buf.layer(BumpMap.lbit).set(ret, bit);
			    buf.layer(MeshBuf.tex).set(ret, tc);
			    buf.layer(MeshBuf.col).set(ret, new Color(255, 255, 255, alpha));
			    return(ret);
			}
		    };
	    }
	}
    }
    public final MapMesh.DataID<Blend> blend = new MapMesh.DataID<Blend>() {
	public Blend make(MapMesh m) {
	    return(new Blend(m));
	}
    };

    @ResName("trn")
    public static class Factory implements Tiler.Factory {
	public TerrainTile create(int id, Tileset set) {
	    Resource res = set.getres();
	    Tileset trans = null;
	    NodeWrap base = null;
	    Collection<Var> var = new LinkedList<Var>();
	    NodeWrap commat = null;
	    for(Object rdesc : set.ta) {
		Object[] desc = (Object[])rdesc;
		String p = (String)desc[0];
		if(p.equals("common-mat")) {
		    if(desc[1] instanceof Integer) {
			int mid = Utils.iv(desc[1]);
			commat = res.flayer(Material.Res.class, mid).get();
		    } else if(desc[1] instanceof String) {
			String mnm = (String)desc[1];
			int mver = Utils.iv(desc[2]);
			if(desc.length > 3) {
			    commat = res.pool.load(mnm, mver).get().flayer(Material.Res.class, Utils.iv(desc[3])).get();
			} else {
			    commat = Material.fromres((Material.Owner)null, res.pool.load(mnm, mver).get(), Message.nil);
			}
		    }
		}
	    }
	    for(Object rdesc : set.ta) {
		Object[] desc = (Object[])rdesc;
		String p = (String)desc[0];
		if(p.equals("base")) {
		    int mid = Utils.iv(desc[1]);
		    base = NodeWrap.compose(commat, res.flayer(Material.Res.class, mid).get());
		} else if(p.equals("var")) {
		    int mid = Utils.iv(desc[1]);
		    double thrl, thrh;
		    if(desc[2] instanceof Object[]) {
			thrl = Utils.fv(((Object[])desc[2])[0]);
			thrh = Utils.fv(((Object[])desc[2])[1]);
		    } else {
			thrl = Utils.fv(desc[2]);
			thrh = Double.MAX_VALUE;
		    }
		    double nz = (res.name.hashCode() * mid * 8129) % 10000;
		    NodeWrap mat = NodeWrap.compose(commat, res.flayer(Material.Res.class, mid).get());
		    var.add(new Var(mat, thrl, thrh, nz));
		} else if(p.equals("trans")) {
		    Resource tres = set.getres().pool.load((String)desc[1], Utils.iv(desc[2])).get();
		    trans = tres.layer(Tileset.class);
		}
	    }
	    /* XXX? Arguably ugly, but tell the base tileset format
	     * that. Also arguably nice to be able to set terrain and
	     * flavobj materials in one go. */
	    set.flavobjmat = commat;
	    return(new TerrainTile(id, new SNoise3(res.name.hashCode()), base, var.toArray(new Var[0]), trans));
	}
    }

    public TerrainTile(int id, SNoise3 noise, NodeWrap base, Var[] var, Tileset transset) {
	super(id);
	this.noise = noise;
	int z = 0;
	this.base = base;
	this.draw = Pipe.Op.compose(new MapMesh.MLOrder(0, z++), VertexColor.instance);
	for(Var v : this.var = var)
	    v.draw = Pipe.Op.compose(new MapMesh.MLOrder(0, z++), VertexColor.instance);
	this.transset = transset;
    }

    public void lay(MapMesh m, Random rnd, Coord lc, Coord gc) {
	lay(m, lc, gc, this, false);
    }

    public void faces(MapMesh m, MPart d) {
	Blend b = m.data(blend);
	Surface.MeshVertex[] mv = new Surface.MeshVertex[d.v.length];
	for(int i = 0; i < var.length + 1; i++) {
	    if(b.en[i][b.es.o(d.lc)]) {
		NodeWrap mat = (i == 0) ? this.base : (var[i - 1].mat);
		Pipe.Op draw = d.mcomb((i == 0) ? this.draw : (var[i - 1].draw));
		SModel buf = SModel.get(m, NodeWrap.compose(mat, draw), b.lvfac[i]);
		for(int o = 0; o < d.v.length; o++)
		    mv[o] = buf.get(d, o);
		for(int fi = 0; fi < d.f.length; fi += 3)
		    buf.new Face(mv[d.f[fi]], mv[d.f[fi + 1]], mv[d.f[fi + 2]]);
	    }
	}
    }

    private final static Map<TexRender, AlphaTex> transtex = new WeakHashMap<TexRender, AlphaTex>();

    /* XXX: Some strange javac bug seems to make it resolve the
     * trans() references to the wrong signature, thus the name
     * distinction. */
    public void _faces(MapMesh m, int z, Tile trans, MPart d) {
	Tex ttex = trans.tex();
	float tl = tcx(ttex, 0), tt = tcy(ttex, 0), tw = tcx(ttex, ttex.sz().x) - tl, th = tcy(ttex, ttex.sz().y) - tt;
	TexRender gt;
	if(ttex instanceof TexRender)
	    gt = (TexRender)ttex;
	else if((ttex instanceof TexSI) && (((TexSI)ttex).parent instanceof TexRender))
	    gt = (TexRender)((TexSI)ttex).parent;
	else
	    throw(new RuntimeException("Cannot use texture for transitions: " + ttex));
	AlphaTex alpha;
	synchronized(transtex) {
	    if((alpha = transtex.get(gt)) == null)
		transtex.put(gt, alpha = new AlphaTex(gt.img, 0.01f));
	}
	Blend b = m.data(blend);
	Surface.MeshVertex[] mv = new Surface.MeshVertex[d.v.length];
	for(int i = 0; i < var.length + 1; i++) {
	    if(b.en[i][b.es.o(d.lc)]) {
		NodeWrap mat = (i == 0) ? this.base : (var[i - 1].mat);
		Pipe.Op draw = (i == 0) ? this.draw : (var[i - 1].draw);
		draw = d.mcomb(Pipe.Op.compose(draw, new MapMesh.MLOrder(z, i), alpha));
		MeshBuf buf = MapMesh.Model.get(m, NodeWrap.compose(mat, draw));
		MeshBuf.Vec2Layer cc = buf.layer(AlphaTex.lclip);
		for(int o = 0; o < d.v.length; o++) {
		    mv[o] = b.lvfac[i].make(buf, d, o);
		    cc.set(mv[o], new Coord3f(tl + (tw * d.tcx[o]), tt + (th * d.tcy[o]), 0));
		}
		for(int fi = 0; fi < d.f.length; fi += 3)
		    buf.new Face(mv[d.f[fi]], mv[d.f[fi + 1]], mv[d.f[fi + 2]]);
	    }
	}
    }

    private MCons tcons(final int z, final Tile t) {
	return(new MCons() {
		public void faces(MapMesh m, MPart d) {
		    _faces(m, z, t, d);
		}
	    });
    }

    public MCons tcons(final int z, final int bmask, final int cmask) {
	if((transset == null) || ((bmask == 0) && (cmask == 0)))
	    return(MCons.nil);
	return(new MCons() {
		public void faces(MapMesh m, MPart d) {
		    Random rnd = m.rnd(d.lc);
		    if((transset.btrans != null) && (bmask != 0))
			tcons(z, transset.btrans[bmask - 1].pick(rnd)).faces(m, d);
		    if((transset.ctrans != null) && (cmask != 0))
			tcons(z, transset.ctrans[cmask - 1].pick(rnd)).faces(m, d);
		}
	    });
    }

    public void trans(MapMesh m, Random rnd, Tiler gt, Coord lc, Coord gc, int z, int bmask, int cmask) {
	if(transset == null)
	    return;
	if(m.map.gettile(gc) <= id)
	    return;
	if((transset.btrans != null) && (bmask > 0))
	    gt.lay(m, lc, gc, tcons(z, transset.btrans[bmask - 1].pick(rnd)), false);
	if((transset.ctrans != null) && (cmask > 0))
	    gt.lay(m, lc, gc, tcons(z, transset.ctrans[cmask - 1].pick(rnd)), false);
    }

    public static class RidgeTile extends TerrainTile implements Ridges.RidgeTile {
	public final Tiler.MCons rcons;
	public final int rth;

	@ResName("trn-r")
	public static class RFactory implements Tiler.Factory {
	    public Tiler create(int id, Tileset set) {
		TerrainTile base = new Factory().create(id, set);
		double rth = 20;
		Pipe.Op mat = null;
		float texh = 11f;
		for(Object rdesc : set.ta) {
		    Object[] desc = (Object[])rdesc;
		    String p = (String)desc[0];
		    if(p.equals("rmat")) {
			Resource mres = set.getres().pool.load((String)desc[1], Utils.iv(desc[2])).get();
			mat = mres.flayer(Material.Res.class).get();
			if(desc.length > 3)
			    texh = Utils.fv(desc[3]);
		    } else if(p.equals("rthres")) {
			rth = Utils.dv(desc[1]);
		    }
		}
		if(mat == null)
		    throw(new RuntimeException("Ridge-tiles must be given a ridge material, in " + set.getres().name));
		return(new RidgeTile(base.id, base.noise, base.base, base.var, base.transset, (int)rth, mat, texh));
	    }
	}

	public RidgeTile(int id, SNoise3 noise, NodeWrap base, Var[] var, Tileset transset, int rth, Pipe.Op rmat, float texh) {
	    super(id, noise, base, var, transset);
	    this.rth = rth;
	    this.rcons = new Ridges.TexCons(rmat, texh);
	}

	public double breakz() {return(rth);}

	public void model(MapMesh m, Random rnd, Coord lc, Coord gc) {
	    if(!m.data(Ridges.id).model(lc))
		super.model(m, rnd, lc, gc);
	}

	public void lay(MapMesh m, Coord lc, Coord gc, MCons cons, boolean cover) {
	    Ridges r = m.data(Ridges.id);
	    if(!r.laygnd(lc, cons))
		super.lay(m, lc, gc, cons, cover);
	    else if(cover)
		r.layridge(lc, cons);
	}

	public void lay(MapMesh m, Random rnd, Coord lc, Coord gc) {
	    super.lay(m, rnd, lc, gc);
	    m.data(Ridges.id).layridge(lc, rcons);
	}
    }
}
