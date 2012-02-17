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

    public RidgeTile(int id, Resource.Tileset set, int[] breaks) {
	super(id, set);
	this.breaks = breaks;
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

    private static final GLState rmat = new Material(new java.awt.Color(128, 128, 128, 255));
    private static final int[]
	ulx = {0, 0, 1, 1},
	uly = {1, 0, 0, 1},
	urx = {0, 1, 1, 0},
	ury = {0, 0, 1, 1},
	blx = {1, 0, 0, 1},
	bly = {1, 1, 0, 0},
	brx = {1, 1, 0, 0},
	bry = {0, 1, 1, 0};

    public void remaptex(Plane left, Plane right, int dir, Tex tex) {
	int tx = tex.sz().x, ty = tex.sz().y;
	int hx = tx / 2, hy = ty / 2;
	if(dir == 0) {
	    left .texul = new Coord( 0,  0); left .texbr = new Coord(hx, ty);
	    right.texul = new Coord(hx,  0); right.texbr = new Coord(tx, ty);
	} else if(dir == 1) {
	    left .texul = new Coord( 0,  0); left .texbr = new Coord(tx, hy);
	    right.texul = new Coord( 0, hy); right.texbr = new Coord(tx, ty);
	} else if(dir == 2) {
	    left .texul = new Coord(hx,  0); left .texbr = new Coord(tx, ty);
	    right.texul = new Coord( 0,  0); right.texbr = new Coord(hx, ty);
	} else if(dir == 3) {
	    left .texul = new Coord( 0, hy); left .texbr = new Coord(tx, ty);
	    right.texul = new Coord( 0,  0); right.texbr = new Coord(tx, hy);
	}
    }

    public void layend(MapMesh m, Random rnd, Coord lc, Coord gc, boolean[] b) {
	int dir;
	for(dir = 0; dir < 4; dir++) {
	    if(b[dir])
		break;
	}
	boolean cw = (m.map.getz(gc.add(urx[dir], ury[dir])) > m.map.getz(gc.add(brx[dir], bry[dir])));
	Surface g = m.gnd();
	SPoint
	    fl = g.spoint(lc.add(ulx[dir], uly[dir])),
	    bl = g.spoint(lc.add(urx[dir], ury[dir])),
	    br = g.spoint(lc.add(brx[dir], bry[dir])),
	    fr = g.spoint(lc.add(blx[dir], bly[dir]));
	SPoint bu = new SPoint(bl.pos.add(br.pos).mul(0.5f));
	SPoint bb = new SPoint(bl.pos.add(br.pos).mul(0.5f));
	SPoint fm = new SPoint(fl.pos.add(fr.pos).mul(0.5f));
	Tile tile = set.ground.pick(rnd);
	Plane left, right;
	if(cw) {
	    bu.pos.z = bl.pos.z;
	    bb.pos.z = br.pos.z;
	    left  = m.new Plane(shift(new SPoint[] {fl, fm, bu, bl}, 5 - dir), 0, tile.tex(), false);
	    right = m.new Plane(shift(new SPoint[] {fm, fr, br, bb}, 5 - dir), 0, tile.tex(), false);
	} else {
	    bu.pos.z = br.pos.z;
	    bb.pos.z = bl.pos.z;
	    left  = m.new Plane(new SPoint[] {fl, fm, bb, bl}, 0, tile.tex(), false);
	    right = m.new Plane(new SPoint[] {fm, fr, br, bu}, 0, tile.tex(), false);
	}
	remaptex(left, right, dir, tile.tex());
	MeshBuf mod = m.model(rmat, MeshBuf.class);
	MeshBuf.Vertex vbu = mod.new Vertex(bu.pos, bu.nrm);
	MeshBuf.Vertex vbb = mod.new Vertex(bb.pos, bb.nrm);
	MeshBuf.Vertex vfm = mod.new Vertex(fm.pos, fm.nrm);
	if(cw)
	    mod.new Face(vfm, vbb, vbu);
	else
	    mod.new Face(vbu, vbb, vfm);
    }
    
    public void layridge(MapMesh m, Random rnd, Coord lc, Coord gc, boolean[] b) {
	int z00 = m.map.getz(gc),
	    z10 = m.map.getz(gc.add(1, 0)),
	    z01 = m.map.getz(gc.add(0, 1)),
	    z11 = m.map.getz(gc.add(1, 1));
	int dir = b[0]?((z00 > z10)?0:2):((z00 > z01)?1:3);
	Surface g = m.gnd();
	SPoint
	    ul = g.spoint(lc.add(ulx[dir], uly[dir])),
	    ur = g.spoint(lc.add(urx[dir], ury[dir])),
	    bl = g.spoint(lc.add(blx[dir], bly[dir])),
	    br = g.spoint(lc.add(brx[dir], bry[dir]));
	SPoint
	    mlu = new SPoint(ul.pos.add(bl.pos).mul(0.5f)),
	    mlb = new SPoint(ul.pos.add(bl.pos).mul(0.5f)),
	    mru = new SPoint(ur.pos.add(br.pos).mul(0.5f)),
	    mrb = new SPoint(ur.pos.add(br.pos).mul(0.5f));
	mlu.pos.z = ul.pos.z;
	mru.pos.z = ur.pos.z;
	mlb.pos.z = bl.pos.z;
	mrb.pos.z = br.pos.z;
	Tile tile = set.ground.pick(rnd);
	Plane upper = m.new Plane(shift(new SPoint[] {ul, mlu, mru, ur}, 5 - dir), 0, tile.tex(), false);
	Plane lower = m.new Plane(shift(new SPoint[] {mlb, bl, br, mrb}, 5 - dir), 0, tile.tex(), false);
	remaptex(upper, lower, dir, tile.tex());
	MeshBuf mod = m.model(rmat, MeshBuf.class);
	MeshBuf.Vertex vul = mod.new Vertex(mlu.pos, mlu.nrm);
	MeshBuf.Vertex vbl = mod.new Vertex(mlb.pos, mlb.nrm);
	MeshBuf.Vertex vbr = mod.new Vertex(mrb.pos, mrb.nrm);
	MeshBuf.Vertex vur = mod.new Vertex(mru.pos, mru.nrm);
	mod.new Face(vul, vbl, vur);
	mod.new Face(vbl, vbr, vur);
    }
    
    public void mkcornwall(MapMesh m, Random rnd, Coord3f ul, Coord3f bl, Coord3f br, Coord3f ur, boolean cw) {
	MeshBuf mod = m.model(rmat, MeshBuf.class);
	MeshBuf.Vertex vul = mod.new Vertex(ul, Coord3f.zu);
	MeshBuf.Vertex vbl = mod.new Vertex(bl, Coord3f.zu);
	MeshBuf.Vertex vbr = mod.new Vertex(br, Coord3f.zu);
	MeshBuf.Vertex vur = mod.new Vertex(ur, Coord3f.zu);
	if(cw) {
	    mod.new Face(vul, vbl, vur);
	    mod.new Face(vbl, vbr, vur);
	} else {
	    mod.new Face(vul, vbl, vur);
	    mod.new Face(vbl, vbr, vur);
	}
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
	}
	Tile tile = set.ground.pick(rnd);
	boolean cont = false;
	for(int i = s, n = 0; n < 4; i = (i + 1) % 4, n++) {
	    if(cont) {
		cont = false;
	    } else if(!b[i]) {
		Plane pl = m.new Plane(new SPoint[] {crn[i], h1[i], h2[(i + 1) % 4], crn[(i + 1) % 4]}, 0, tile.tex(), false);
		cont = true;
		SPoint pc = ct[(i + 3) % 4], cc = ct[i];
		if(pc.pos.z > cc.pos.z)
		    mkcornwall(m, rnd, pc.pos, cc.pos, h1[i].pos, h2[(i + 3) % 4].pos, true);
		else
		    mkcornwall(m, rnd, h1[i].pos, h2[(i + 3) % 4].pos, pc.pos, cc.pos, false);
	    } else if(!b[(i + 3) % 4]) {
		Plane pl = m.new Plane(new SPoint[] {crn[i], h1[i], ct[i], h2[i]}, 0, tile.tex(), false);
	    } else {
		Plane pl = m.new Plane(new SPoint[] {crn[i], h1[i], ct[i], h2[i]}, 0, tile.tex(), false);
		SPoint pc = ct[(i + 3) % 4], cc = ct[i];
		if(pc.pos.z > cc.pos.z)
		    mkcornwall(m, rnd, pc.pos, cc.pos, h1[i].pos, h2[(i + 3) % 4].pos, true);
		else
		    mkcornwall(m, rnd, h1[i].pos, h2[(i + 3) % 4].pos, pc.pos, cc.pos, false);
	    }
	}
    }

    public boolean isplain(MapMesh m, Coord gc) {
	boolean[] b = breaks(m, gc, breaks[0]);
	return(b[0] || b[1] || b[2] || b[3]);
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
	} else {
	    super.layover(m, lc, gc, z, t);
	}
    }
}
