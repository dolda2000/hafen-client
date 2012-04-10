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
import haven.Resource.Tile;
import haven.MapMesh.*;

public class RidgeTile extends GroundTile {
    public final int[] breaks;
    public final Resource[] walls;
    public final Resource[] lcorn;
    public final Resource[] rcorn;
    public final Resource[] strans, c1trans, c2trans;

    public RidgeTile(int id, Resource.Tileset set, int[] breaks, Resource[] walls, Resource[] lcorn, Resource[] rcorn,
		     Resource[] strans, Resource[] c1trans, Resource[] c2trans) {
	super(id, set);
	this.breaks = breaks;
	this.walls = walls;
	this.lcorn = lcorn;
	this.rcorn = rcorn;
	this.strans = strans;
	this.c1trans = c1trans;
	this.c2trans = c2trans;
    }
    
    public boolean[] breaks(MapMesh m, Coord gc, int diff) {
	int z00 = m.map.getz(gc),
	    z10 = m.map.getz(gc.add(1, 0)),
	    z01 = m.map.getz(gc.add(0, 1)),
	    z11 = m.map.getz(gc.add(1, 1));
	return(new boolean[] {
		Math.abs(z00 - z10) >= diff,
		Math.abs(z10 - z11) >= diff,
		Math.abs(z11 - z01) >= diff,
		Math.abs(z01 - z00) >= diff,
	    });
    }

    public boolean isend(MapMesh m, Coord gc, boolean[] b) {
	return(((b[0]?1:0) + (b[1]?1:0) + (b[2]?1:0) + (b[3]?1:0)) == 1);
    }
    
    public boolean isstraight(MapMesh m, Coord gc, boolean[] b) {
	return((b[0] && b[2] && !b[1] && !b[3]) ||
	       (b[1] && b[3] && !b[0] && !b[2]));
    }

    @SuppressWarnings("unchecked")
    private static <T> T[] shift(T[] a, int n) {
	T[] r = (T[])java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), a.length);
	for(int i = 0; i < a.length; i++)
	    r[(i + n) % a.length] = a[i];
	return(r);
    }

    public void makewall(MapMesh m, Coord3f ul, Coord3f bl, Coord3f br, Coord3f ur, Resource wall, float w) {
	float hw = w / 2.0f;
	float xbx, xby;
	float lzof, lzsf, lzs;
	float rzof, rzsf, rzs;
	float tysf, tys;
	{
	    double tx = br.x - bl.x, ty = br.y - bl.y;
	    double lf = 1.0 / Math.sqrt((tx * tx) + (ty * ty));
	    xbx = (float)(tx * lf); xby = (float)(ty * lf);
	    lzof = (float)((br.z - bl.z) * lf);
	    lzsf = (float)((ur.z - br.z - ul.z + bl.z) * lf / 11.0);
	    lzs  = (float)((ul.z - bl.z) / 11.0);
	    rzof = (float)((bl.z - br.z) * lf);
	    rzsf = (float)((ul.z - bl.z - ur.z + br.z) * lf / 11.0);
	    rzs  = (float)((ur.z - br.z) / 11.0);
	    tys  = (int)(((ul.z - bl.z) + 5) / 11);
	    tysf = (float)((((int)(((ur.z - br.z) + 5) / 11)) - tys) * lf);
	}
	float ybx = -xby, yby = xbx;
	for(FastMesh.MeshRes r : wall.layers(FastMesh.MeshRes.class)) {
	    MeshBuf buf = m.model(r.mat.get(), MeshBuf.class);
	    MeshBuf.Vertex[] vs = buf.copy(r.m);
	    for(MeshBuf.Vertex v : vs) {
		float x = v.pos.x, y = v.pos.y, z = v.pos.z;
		v.pos.x = (x * xbx) + (y * ybx) + bl.x;
		v.pos.y = (x * xby) + (y * yby) + bl.y;
		if(x < hw) {
		    v.pos.z = (lzof * x) + ((lzs + (lzsf * x)) * z) + bl.z;
		} else {
		    float X = w - x;
		    v.pos.z = (rzof * X) + ((rzs + (rzsf * X)) * z) + br.z;
		}
		float nx = v.nrm.x, ny = v.nrm.y;
		v.nrm.x = (nx * xbx) + (ny * ybx);
		v.nrm.y = (nx * xby) + (ny * yby);
		v.tex.y = (tys + (tysf * x)) * v.tex.y;
	    }
	}
    }

    public class Ridges extends MapMesh.Hooks {
	public final MapMesh m;
	private final Tile[] tiles;
	
	private Ridges(MapMesh m) {
	    this.m = m;
	    this.tiles = new Tile[m.sz.x * m.sz.y];
	}
	
	public class Tile {
	    public TilePlane[] planes = new TilePlane[4];
	    int n;
	    
	    public class TilePlane {
		public SPoint[] vrt;
		public float u, l, b, r;
		
		public TilePlane(SPoint[] vrt) {
		    this.vrt = vrt;
		    u = l = 0;
		    b = r = 1;
		    planes[n++] = this;
		}
	    }
	    
	    public void layover(int z, Resource.Tile tile) {
		int w = tile.tex().sz().x, h = tile.tex().sz().y;
		for(int i = 0; i < n; i++) {
		    Plane p = m.new Plane(planes[i].vrt, z, tile.tex(), tile.t == 'g');
		    p.texrot(new Coord((int)(w * planes[i].l), (int)(h * planes[i].u)),
			     new Coord((int)(w * planes[i].r), (int)(h * planes[i].b)),
			     0, false);
		}
	    }
	}
	
	public Tile get(Coord c) {
	    return(tiles[c.x + (m.sz.x * c.y)]);
	}
	
	public void set(Coord c, Tile t) {
	    tiles[c.x + (m.sz.x * c.y)] = t;
	}
	
	public void postcalcnrm(Random rnd) {
	}
    }

    private static final int[]
	cwx = {0, 1, 1, 0},
	cwy = {0, 0, 1, 1},
	ecwx = { 0,  1,  0, -1},
	ecwy = {-1,  0,  1,  0};

    public void remapquad(Ridges.Tile.TilePlane p, int q) {
	p.u = cwy[q] * 0.5f; p.l = cwx[q] * 0.5f;
	p.b = p.u + 0.5f;    p.r = p.l + 0.5f;
    }

    public void remaphalf(Ridges.Tile.TilePlane p, int fq) {
	int l = Math.min(cwx[fq], cwx[(fq + 1) % 4]), r = Math.max(cwx[fq], cwx[(fq + 1) % 4]) + 1;
	int t = Math.min(cwy[fq], cwy[(fq + 1) % 4]), b = Math.max(cwy[fq], cwy[(fq + 1) % 4]) + 1;
	p.u = t * 0.5f;
	p.l = l * 0.5f;
	p.b = b * 0.5f;
	p.r = r * 0.5f;
    }

    private Ridges rget(MapMesh m) {
	Ridges r = (Ridges)m.data.get(Ridges.class);
	if(r == null)
	    m.data.put(Ridges.class, r = new Ridges(m));
	return(r);
    }

    private void layend(MapMesh m, Random rnd, Coord lc, Coord gc, int dir) {
	Surface g = m.gnd();
	SPoint
	    bl = g.spoint(lc.add(cwx[dir], cwy[dir])),
	    br = g.spoint(lc.add(cwx[(dir + 1) % 4], cwy[(dir + 1) % 4])),
	    fr = g.spoint(lc.add(cwx[(dir + 2) % 4], cwy[(dir + 2) % 4])),
	    fl = g.spoint(lc.add(cwx[(dir + 3) % 4], cwy[(dir + 3) % 4]));
	boolean cw = bl.pos.z > br.pos.z;
	SPoint bu = new SPoint(bl.pos.add(br.pos).mul(0.5f));
	SPoint bb = new SPoint(bl.pos.add(br.pos).mul(0.5f));
	SPoint fm = new SPoint(fl.pos.add(fr.pos).mul(0.5f));
	Ridges r = rget(m);
	Ridges.Tile tile = r.new Tile();
	Ridges.Tile.TilePlane left, right;
	SPoint[] uh;
	if(cw) {
	    bu.pos.z = bl.pos.z;
	    bb.pos.z = br.pos.z;
	    left  = tile.new TilePlane(uh = shift(new SPoint[] {fl, fm, bu, bl}, 5 - dir));
	    right = tile.new TilePlane(     shift(new SPoint[] {fm, fr, br, bb}, 5 - dir));
	} else {
	    bu.pos.z = br.pos.z;
	    bb.pos.z = bl.pos.z;
	    left  = tile.new TilePlane(     shift(new SPoint[] {fl, fm, bb, bl}, 5 - dir));
	    right = tile.new TilePlane(uh = shift(new SPoint[] {fm, fr, br, bu}, 5 - dir));
	}
	remaphalf(left , (dir + 3) % 4);
	remaphalf(right, (dir + 1) % 4);
	r.set(lc, tile);
	tile.layover(0, set.ground.pick(rnd));
	m.new Plane(uh, 256, strans[rnd.nextInt(strans.length)].layer(Resource.imgc).tex(), false)
	    .texrot(null, null, 1 + dir + (cw?2:0), false);
	if(cw)
	    makewall(m, fm.pos, fm.pos, bb.pos, bu.pos, walls[rnd.nextInt(walls.length)], 11);
	else
	    makewall(m, bu.pos, bb.pos, fm.pos, fm.pos, walls[rnd.nextInt(walls.length)], 11);
    }

    public void layend(MapMesh m, Random rnd, Coord lc, Coord gc, boolean[] b) {
	for(int dir = 0; dir < 4; dir++) {
	    if(b[dir]) {
		layend(m, rnd, lc, gc, dir);
		return;
	    }
	}
    }
    
    public void layridge(MapMesh m, Random rnd, Coord lc, Coord gc, boolean[] b) {
	int z00 = m.map.getz(gc),
	    z10 = m.map.getz(gc.add(1, 0)),
	    z01 = m.map.getz(gc.add(0, 1)),
	    z11 = m.map.getz(gc.add(1, 1));
	int dir = b[0]?((z00 > z10)?0:2):((z00 > z01)?1:3);
	boolean tb1 = m.map.tiler(m.map.gettile(gc.add(ecwx[dir], ecwy[dir]))) instanceof RidgeTile;
	boolean tb2 = m.map.tiler(m.map.gettile(gc.add(ecwx[(dir + 2) % 4], ecwy[(dir + 2) % 4]))) instanceof RidgeTile;
	if(!tb1 && !tb2) {
	    /* XXX */
	} else if(!tb1) {
	    layend(m, rnd, lc, gc, (dir + 2) % 4);
	    return;
	} else if(!tb2) {
	    layend(m, rnd, lc, gc, dir);
	}
	Surface g = m.gnd();
	SPoint
	    ur = g.spoint(lc.add(cwx[dir], cwy[dir])),
	    br = g.spoint(lc.add(cwx[(dir + 1) % 4], cwy[(dir + 1) % 4])),
	    bl = g.spoint(lc.add(cwx[(dir + 2) % 4], cwy[(dir + 2) % 4])),
	    ul = g.spoint(lc.add(cwx[(dir + 3) % 4], cwy[(dir + 3) % 4]));
	SPoint
	    mlu = new SPoint(ul.pos.add(bl.pos).mul(0.5f)),
	    mlb = new SPoint(ul.pos.add(bl.pos).mul(0.5f)),
	    mru = new SPoint(ur.pos.add(br.pos).mul(0.5f)),
	    mrb = new SPoint(ur.pos.add(br.pos).mul(0.5f));
	mlu.pos.z = ul.pos.z;
	mru.pos.z = ur.pos.z;
	mlb.pos.z = bl.pos.z;
	mrb.pos.z = br.pos.z;
	Ridges r = rget(m);
	Ridges.Tile tile = r.new Tile();
	Ridges.Tile.TilePlane upper = tile.new TilePlane(shift(new SPoint[] {ul, mlu, mru, ur}, 5 - dir));
	Ridges.Tile.TilePlane lower = tile.new TilePlane(shift(new SPoint[] {mlb, bl, br, mrb}, 5 - dir));
	remaphalf(upper, (dir + 3) % 4);
	remaphalf(lower, (dir + 1) % 4);
	r.set(lc, tile);
	tile.layover(0, set.ground.pick(rnd));
	m.new Plane(upper.vrt, 256, strans[rnd.nextInt(strans.length)].layer(Resource.imgc).tex(), false)
	    .texrot(null, null, 3 + dir, false);
	makewall(m, mlu.pos, mlb.pos, mrb.pos, mru.pos, walls[rnd.nextInt(walls.length)], 11);
    }
    
    public void mkcornwall(MapMesh m, Random rnd, Coord3f ul, Coord3f bl, Coord3f br, Coord3f ur, boolean cw) {
	if(cw)
	    makewall(m, ul, bl, br, ur, lcorn[rnd.nextInt(lcorn.length)], 5.5f);
	else
	    makewall(m, ul, bl, br, ur, rcorn[rnd.nextInt(rcorn.length)], 5.5f);
    }

    public void laycomplex(MapMesh m, Random rnd, Coord lc, Coord gc, boolean[] b) {
	Surface g = m.gnd();
	SPoint[] crn = {
	    g.spoint(lc),
	    g.spoint(lc.add(1, 0)),
	    g.spoint(lc.add(1, 1)),
	    g.spoint(lc.add(0, 1)),
	};
	int s;
	for(s = 0; true; s++) {
	    if(b[s]) {
		s = (s + 1) % 4;
		break;
	    }
	}
	SPoint[] ct = new SPoint[4];
	SPoint[] h1 = new SPoint[4];
	SPoint[] h2 = new SPoint[4];
	{
	    for(int i = s, n = 0; n < 4; i = (i + 1) % 4, n++) {
		if(!b[(i + 3) % 4]) {
		    h1[i] = h2[(i + 3) % 4];
		    h1[i].pos.z = (h1[i].pos.z + crn[i].pos.z) * 0.5f;
		} else {
		    h1[i] = new SPoint(crn[(i + 3) % 4].pos.add(crn[i].pos).mul(0.5f));
		    h1[i].pos.z = crn[i].pos.z;
		}
		h2[i] = new SPoint(crn[(i + 1) % 4].pos.add(crn[i].pos).mul(0.5f));
		h2[i].pos.z = crn[i].pos.z;
	    }
	    SPoint cc = null;
	    for(int i = s, n = 0; n < 4; i = (i + 1) % 4, n++) {
		if(cc == null) {
		    cc = new SPoint(crn[0].pos.add(crn[1].pos).add(crn[2].pos).add(crn[3].pos).mul(0.25f));
		    if(b[i])
			cc.pos.z = crn[i].pos.z;
		    else
			cc.pos.z = (h1[i].pos.z + h2[(i + 1) % 4].pos.z) * 0.5f;
		}
		ct[i] = cc;
		if(b[i])
		    cc = null;
	    }
	    for(int i = s, n = 0; n < 4; i = (i + 1) % 4, n++) {
		if(b[i] && !(m.map.tiler(m.map.gettile(gc.add(ecwx[i], ecwy[i]))) instanceof RidgeTile)) {
		    h2[i].pos.z = (h2[i].pos.z + h1[(i + 1) % 4].pos.z) * 0.5f;
		    h1[(i + 1) % 4] = h2[i];
		}
	    }
	}
	Ridges r = rget(m);
	Ridges.Tile tile = r.new Tile();
	boolean cont = false;
	for(int i = s, n = 0; n < 4; i = (i + 1) % 4, n++) {
	    if(cont) {
		cont = false;
	    } else if(!b[i] && b[(i + 1) % 4] && b[(i + 3) % 4]) {
		Ridges.Tile.TilePlane pl = tile.new TilePlane(shift(new SPoint[] {crn[i], h1[i], h2[(i + 1) % 4], crn[(i + 1) % 4]}, 4 - i));
		remaphalf(pl, i);
		cont = true;
		SPoint pc = ct[(i + 3) % 4], cc = ct[i];
		if(pc.pos.z > cc.pos.z) {
		    mkcornwall(m, rnd, pc.pos, cc.pos, h1[i].pos, h2[(i + 3) % 4].pos, true);
		} else {
		    mkcornwall(m, rnd, h1[i].pos, h2[(i + 3) % 4].pos, pc.pos, cc.pos, false);
		    m.new Plane(pl.vrt, 256, strans[rnd.nextInt(strans.length)].layer(Resource.imgc).tex(), false)
			.texrot(null, null, i, false);
		}
	    } else {
		Ridges.Tile.TilePlane pl = tile.new TilePlane(shift(new SPoint[] {crn[i], h1[i], ct[i], h2[i]}, 4 - i));
		remapquad(pl, i);
		boolean[] ub = new boolean[4], db = new boolean[4], tb = new boolean[4];
		for(int o = 0; o < 4; o++) {
		    int u = (i + o) % 4;
		    tb[o] = b[u];
		    ub[o] = b[u] && (h2[u].pos.z < h1[(u + 1) % 4].pos.z);
		    db[o] = b[u] && (h2[u].pos.z > h1[(u + 1) % 4].pos.z);
		}
		if(ub[3] && db[0]) {
		    m.new Plane(pl.vrt, 256, c1trans[rnd.nextInt(c1trans.length)].layer(Resource.imgc).tex(), false)
			.texrot(null, null, i, false);
		} else if(!tb[0] && !tb[3] && db[1] && ub[2]) {
		    m.new Plane(pl.vrt, 256, c2trans[rnd.nextInt(c2trans.length)].layer(Resource.imgc).tex(), false)
			.texrot(null, null, i, false);
		} else if(ub[3] && !db[0]) {
		    Tex t = strans[rnd.nextInt(strans.length)].layer(Resource.imgc).tex();
		    m.new Plane(pl.vrt, 256, t, false)
			.texrot(Coord.z, new Coord(t.sz().x / 2, t.sz().y), i, false);
		} else if(!ub[3] && db[0]) {
		    Tex t = strans[rnd.nextInt(strans.length)].layer(Resource.imgc).tex();
		    m.new Plane(pl.vrt, 256, t, false)
			.texrot(Coord.z, new Coord(t.sz().x / 2, t.sz().y), i + 3, false);
		}
		if(b[(i + 3) % 4]) {
		    SPoint pc = ct[(i + 3) % 4], cc = ct[i];
		    if(pc.pos.z > cc.pos.z)
			mkcornwall(m, rnd, pc.pos, cc.pos, h1[i].pos, h2[(i + 3) % 4].pos, true);
		    else
			mkcornwall(m, rnd, h1[i].pos, h2[(i + 3) % 4].pos, pc.pos, cc.pos, false);
		}
	    }
	}
	r.set(lc, tile);
	tile.layover(0, set.ground.pick(rnd));
    }

    public void lay(MapMesh m, Random rnd, Coord lc, Coord gc) {
	boolean[] b = breaks(m, gc, breaks[0]);
	if(b[0] || b[1] || b[2] || b[3]) {
	    if(isend(m, gc, b)) {
		layend(m, rnd, lc, gc, b);
	    } else if(isstraight(m, gc, b)) {
		layridge(m, rnd, lc, gc, b);
	    } else {
		laycomplex(m, rnd, lc, gc, b);
	    }
	} else {
	    super.lay(m, rnd, lc, gc);
	}
    }
    
    public void layover(MapMesh m, Coord lc, Coord gc, int z, Tile t) {
	boolean[] b = breaks(m, gc, breaks[0]);
	if(b[0] || b[1] || b[2] || b[3]) {
	    Ridges.Tile tile = rget(m).get(lc);
	    if(tile == null)
		throw(new NullPointerException("Ridged tile has not been properly initialized"));
	    tile.layover(z, t);
	} else {
	    super.layover(m, lc, gc, z, t);
	}
    }
}
