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
import haven.MapMesh.Scan;
import haven.MapMesh.Model;
import haven.Surface.Vertex;
import haven.Tiler.MPart;
import haven.Tiler.SModel;
import haven.Tiler.VertFactory;
import haven.Surface.MeshVertex;
import static haven.MCache.tilesz;
import static haven.Utils.clip;

public class Ridges {
    public static final MapMesh.DataID<Ridges> id = MapMesh.makeid(Ridges.class);
    public static final int segh = 11;
    public final MapMesh m;
    private final MapMesh.MapSurface ms;
    private final boolean[] breaks;
    private final Vertex[][] edges, edgec;
    private final MPart[] gnd, ridge;
    boolean debug = false;

    public interface RidgeTile {
	public int breakz();
    }

    public static class RPart extends MPart {
	public float[] rcx, rcy;
	public RPart(Coord lc, Coord gc, Surface.Vertex[] v, float[] tcx, float[] tcy, int[] f, float[] rcx, float[] rcy) {
	    super(lc, gc, v, tcx, tcx, f);
	    this.rcx = rcx; this.rcy = rcy;
	}
    }

    private int eo(int x, int y, int e) {
	int s = m.sz.x + 1;
	int b = (x + (y * s)) * 2;
	switch(e) {
	case 0: /* N */
	    return(b + 1);
	case 1: /* E */
	    return(b + 2);
	case 2: /* S */
	    return(b + (s * 2) + 1);
	case 3: /* W */
	    return(b);
	}
	throw(new Error());
    }
    private int eo(Coord c, int e) {return(eo(c.x, c.y, e));}

    private boolean[] breaks() {
	Scan ts = new Scan(new Coord(-1, -1), m.sz.add(2, 2));
	int[] bz = new int[ts.l];
	Coord c = new Coord();
	for(c.y = ts.ul.y; c.y < ts.br.y; c.y++) {
	    for(c.x = ts.ul.x; c.x < ts.br.x; c.x++) {
		MCache map = m.map;
		Tiler t = map.tiler(map.gettile(m.ul.add(c)));
		if(t instanceof RidgeTile)
		    bz[ts.o(c)] = ((RidgeTile)t).breakz();
		else
		    bz[ts.o(c)] = Integer.MAX_VALUE;
	    }
	}
	boolean[] breaks = new boolean[(m.sz.x + 1) * (m.sz.y + 1) * 2];
	for(c.y = 0; c.y <= m.sz.y; c.y++) {
	    for(c.x = 0; c.x <= m.sz.x; c.x++) {
		Coord tc = m.ul.add(c);
		int ul = m.map.getz(tc);
		int xd = Math.abs(ul - m.map.getz(tc.add(1, 0)));
		if((xd > bz[ts.o(c.x, c.y)]) && (xd > bz[ts.o(c.x, c.y - 1)]))
		    breaks[eo(c, 0)] = true;
		int yd = Math.abs(ul - m.map.getz(tc.add(0, 1)));
		if((yd > bz[ts.o(c.x, c.y)]) && (yd > bz[ts.o(c.x - 1, c.y)]))
		    breaks[eo(c, 3)] = true;
	    }
	}
	return(breaks);
    }

    private Coord3f dc(float m, int d) {
	if((d % 2) == 0)
	    return(new Coord3f(m, 0, 0));
	else
	    return(new Coord3f(0, m, 0));
    }

    private static final Coord[] tccs = {new Coord(0, 0), new Coord(1, 0), new Coord(1, 1), new Coord(0, 1)};
    private Vertex[] makeedge(Coord tc, int e) {
	if(e == 1)
	    return(makeedge(tc.add(1, 0), 3));
	if(e == 2)
	    return(makeedge(tc.add(0, 1), 0));
	int lo, hi; {
	    Coord gc = tc.add(m.ul);
	    int z1 = m.map.getz(gc.add(tccs[e])), z2 = m.map.getz(gc.add(tccs[(e + 1) % 4]));
	    lo = Math.min(z1, z2); hi = Math.max(z1, z2);
	}
	int nseg = Math.max((hi - lo + (segh / 2)) / segh, 2) - 1;
	Vertex[] ret = new Vertex[nseg + 1];
	Coord3f base = new Coord3f(tc.add(tccs[e]).add(tc.add(tccs[(e + 1) % 4])).mul(tilesz).mul(1, -1)).div(2); base.z = lo;
	float segi = (float)(hi - lo) / (float)nseg;
	Random rnd = m.grnd(m.ul.add(tc));
	rnd.setSeed(rnd.nextInt() + e);
	float bb = (rnd.nextFloat() - 0.5f) * 7.0f;
	for(int v = 0; v <= nseg; v++) {
	    ret[v] = ms.new Vertex(base.add(dc(bb + ((rnd.nextFloat() - 0.5f) * 4.0f), e)).add(0, 0, v * segi));
	    if((v > 0) && (v < nseg))
		ret[v].z += (rnd.nextFloat() - 0.5f) * segi * 0.5f;
	}
	return(ret);
    }

    private Vertex[] ensureedge(Coord tc, int e) {
	int o = eo(tc, e);
	Vertex[] ret;
	if((ret = edges[o]) == null) {
	    ret = edges[o] = makeedge(tc, e);
	    edgec[o] = new Vertex[] {
		ms.new Vertex(ret[0]),
		ms.new Vertex(ret[ret.length - 1]),
	    };
	}
	return(ret);
    }

    private boolean edgelc(Coord tc, int e) {
	Coord gc = tc.add(m.ul);
	return(m.map.getz(gc.add(tccs[e])) < m.map.getz(gc.add(tccs[(e + 1) % 4])));
    }

    public Ridges(MapMesh m) {
	this.m = m;
	this.ms = m.data(MapMesh.gnd);
	this.breaks = breaks();
	this.edges = new Vertex[(m.sz.x + 1) * (m.sz.y + 1) * 2][];
	this.edgec = new Vertex[(m.sz.x + 1) * (m.sz.y + 1) * 2][];
	this.gnd = new MPart[ms.ts.l];
	this.ridge = new MPart[ms.ts.l];
    }

    public boolean[] breaks(Coord tc) {
	return(new boolean[] {
		breaks[eo(tc, 0)],
		breaks[eo(tc, 1)],
		breaks[eo(tc, 2)],
		breaks[eo(tc, 3)]
	    });
    }

    private static int isend(boolean[] b) {
	for(int i = 0; i < 4; i++) {
	    if(b[i] && !b[(i + 1) % 4] && !b[(i + 2) % 4] && !b[(i + 3) % 4])
		return(i);
	}
	return(-1);
    }

    private static int isdiag(boolean[] b) {
	for(int i = 0; i < 4; i++) {
	    if(b[i] && b[(i + 1) % 4] && !b[(i + 2) % 4] && !b[(i + 3) % 4])
		return(i);
	}
	return(-1);
    }

    private void mkfaces(Vertex[] va, int[] fa) {
	for(int i = 0; i < fa.length; i += 3)
	    ms.new Face(va[fa[i]], va[fa[i + 1]], va[fa[i + 2]]);
    }

    private RPart connect(Coord tc, Vertex[] l, Vertex[] r) {
	int n = l.length, m = r.length;
	Vertex[] va = new Vertex[n + m];
	float[] tcx = new float[n + m], tcy = new float[n + m];
	float[] rcx = new float[n + m], rcy = new float[n + m];
	float lh = l[n - 1].z - l[0].z, rh = r[m - 1].z - r[0].z;
	for(int i = 0; i < n; i++) {
	    va[i] = l[i]; tcx[i] = clip(l[i].x - tc.x, 0, 1); tcy[i] = clip(l[i].y - tc.y, 0, 1);
	    rcx[i] = 0; rcy[i] = (l[i].z - l[0].z) / lh;
	}
	for(int i = 0; i < m; i++) {
	    va[i + n] = r[i]; tcx[i + n] = clip(r[i].x - tc.x, 0, 1); tcy[i + n] = clip(r[i].y - tc.y, 0, 1);
	    rcx[i + n] = 1; rcy[i + n] = (r[i].z - r[0].z) / rh;
	}
	int[] fa = new int[(n + m - 2) * 3];
	int fi = 0;
	int a = 0, b = 0;
	float E = 1.0f / n, F = 1.0f / m, e = E, f = F;
	while((a < n - 1) || (b < m - 1)) {
	    if(e < f) {
		fa[fi++] = a++;
		fa[fi++] = b + n;
		fa[fi++] = a;
		e += E;
	    } else {
		fa[fi++] = a;
		fa[fi++] = b++ + n;
		fa[fi++] = b + n;
		f += F;
	    }
	}
	mkfaces(va, fa);
	return(new RPart(tc, tc.add(this.m.ul), va, tcx, tcy, fa, rcx, rcy));
    }

    private void modelcap(Coord tc, int dir) {
	ensureedge(tc, dir);
	Coord3f close = ms.fortile(tc.add(tccs[(dir + 2) % 4]))
	    .add(ms.fortile(tc.add(tccs[(dir + 3) % 4])))
	    .div(2);
	Vertex[] gv = {
	    ms.fortile(tc.add(tccs[dir])),
	    ms.fortile(tc.add(tccs[(dir + 3) % 4])),
	    ms.new Vertex(close),
	    edgec[eo(tc, dir)][edgelc(tc, dir)?0:1],

	    edgec[eo(tc, dir)][edgelc(tc, dir)?1:0],
	    ms.new Vertex(close),
	    ms.fortile(tc.add(tccs[(dir + 2) % 4])),
	    ms.fortile(tc.add(tccs[(dir + 1) % 4])),
	};
	float[] tcx = new float[8], tcy = new float[8];
	Coord pc = tc.mul(tilesz).mul(1, -1);
	for(int i = 0; i < 8; i++) {
	    tcx[i] = clip((gv[i].x - pc.x) / tilesz.x, 0, 1);
	    tcy[i] = clip(-(gv[i].y - pc.y) / tilesz.y, 0, 1);
	}
	mkfaces(gv, srfi);
	gnd[ms.ts.o(tc)] = new MPart(tc, tc.add(m.ul), gv, tcx, tcy, srfi);

	Vertex[] cls = new Vertex[] {ms.new Vertex(close)};
	if(edgelc(tc, dir))
	    ridge[ms.ts.o(tc)] = connect(tc, edges[eo(tc, dir)], cls);
	else
	    ridge[ms.ts.o(tc)] = connect(tc, cls, edges[eo(tc, dir)]);
    }

    private static final int[] srfi = {0, 1, 2, 0, 2, 3, 4, 5, 6, 4, 6, 7};
    private void modelstraight(Coord tc, int dir) {
	ensureedge(tc, dir); ensureedge(tc, dir + 2);
	Vertex[] gv = {
	    ms.fortile(tc.add(tccs[dir])),
	    ms.fortile(tc.add(tccs[(dir + 3) % 4])),
	    edgec[eo(tc, dir + 2)][edgelc(tc, dir + 2)?1:0],
	    edgec[eo(tc, dir)][edgelc(tc, dir)?0:1],

	    edgec[eo(tc, dir)][edgelc(tc, dir)?1:0],
	    edgec[eo(tc, dir + 2)][edgelc(tc, dir + 2)?0:1],
	    ms.fortile(tc.add(tccs[(dir + 2) % 4])),
	    ms.fortile(tc.add(tccs[(dir + 1) % 4])),
	};
	float[] tcx = new float[8], tcy = new float[8];
	Coord pc = tc.mul(tilesz).mul(1, -1);
	for(int i = 0; i < 8; i++) {
	    tcx[i] = clip((gv[i].x - pc.x) / tilesz.x, 0, 1);
	    tcy[i] = clip(-(gv[i].y - pc.y) / tilesz.y, 0, 1);
	}
	mkfaces(gv, srfi);
	gnd[ms.ts.o(tc)] = new MPart(tc, tc.add(m.ul), gv, tcx, tcy, srfi);

	if(edgelc(tc, dir))
	    ridge[ms.ts.o(tc)] = connect(tc, edges[eo(tc, dir)], edges[eo(tc, dir + 2)]);
	else
	    ridge[ms.ts.o(tc)] = connect(tc, edges[eo(tc, dir + 2)], edges[eo(tc, dir)]);
    }

    public boolean model(Coord tc) {
	tc = new Coord(tc);
	boolean[] b = breaks(tc);
	int d;
	if(!b[0] && !b[1] && !b[2] && !b[3]) {
	    return(false);
	} else if((d = isend(b)) >= 0) {
	    modelcap(tc, d);
	    return(true);
	} else if(b[0] && !b[1] && b[2] && !b[3]) {
	    modelstraight(tc, 0);
	    return(true);
	} else if(!b[0] && b[1] && !b[2] && b[3]) {
	    modelstraight(tc, 1);
	    return(true);
	} else if((d = isdiag(b)) >= 0) {
	} else {
	}
	return(false);
    }

    static final Tiler.MCons testcons = new Tiler.MCons() {
	    GLState mat = GLState.compose(new Material.Colors(new java.awt.Color(255, 255, 255)), States.vertexcolor, Light.deflight);
	    public void faces(MapMesh m, MPart mdesc) {
		RPart desc = (RPart)mdesc;
		Model mod = Model.get(m, mat);
		MeshBuf.Col col = mod.layer(MeshBuf.col);
		MeshVertex[] v = new MeshVertex[desc.v.length];
		for(int i = 0; i < desc.v.length; i++) {
		    v[i] = new MeshVertex(mod, desc.v[i]);
		    col.set(v[i], new java.awt.Color((int)(255 * desc.rcx[i]), (int)(255 * desc.rcy[i]), 0));
		}
		int[] f = desc.f;
		for(int i = 0; i < f.length; i += 3)
		    mod.new Face(v[f[i]], v[f[i + 1]], v[f[i + 2]]);
	    }
	};

    public boolean lay(Coord tc, Tiler.MCons cons, Tiler.MCons rcons) {
	MPart gnd = this.gnd[ms.ts.o(tc)];
	if(gnd == null)
	    return(false);
	cons.faces(m, gnd);
	testcons.faces(m, this.ridge[ms.ts.o(tc)]);
	return(true);
    }
}
