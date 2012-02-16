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
    
    public void laycomplex(MapMesh m, Random rnd, Coord lc, Coord gc, boolean[] b) {
	int[] p = new int[4];
	int s;
	{
	    int cp = 0;
	    for(s = 0; true; s++) {
		if(b[s])
		    break;
	    }
	    for(int i = (s + 1) % 4; i != s; i = (i + 1) % 4) {
		if(b[Utils.floormod(i - 1, 4)])
		    cp++;
		p[i] = cp;
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
