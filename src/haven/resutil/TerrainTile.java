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

public class TerrainTile extends Tiler {
    public final GLState base;
    public final SNoise3 noise;
    public final Var[] var;

    public static class Var {
	public final GLState mat;
	public final double thr;
	public final double nz;

	public Var(GLState mat, double thr, double nz) {
	    this.mat = mat; this.thr = thr; this.nz = nz;
	}
    }

    public static class Scan {
        public final Coord ul, sz, br;
        public final int l;

        public Scan(Coord ul, Coord sz) {
            this.ul = ul;
            this.sz = sz;
            this.br = sz.add(ul);
            this.l = sz.x * sz.y;
        }

        public int o(int x, int y) {
            return((x - ul.x) + ((y - ul.y) * sz.x));
        }

        public int o(Coord in) {
            return(o(in.x, in.y));
        }
    }

    public class Blend {
	final MapMesh m;
	final Scan bscan;
	final GLState[] bmat;

	private Blend(MapMesh m) {
	    this.m = m;
	    bscan = new Scan(Coord.z, m.sz);
	    bmat = new GLState[bscan.l];
	    for(int y = bscan.ul.y; y < bscan.br.y; y++) {
		for(int x = bscan.ul.x; x < bscan.br.x; x++) {
		    bmat[bscan.o(x, y)] = base;
		    for(Var v : var) {
			if(noise.get(10, x + m.ul.x, y + m.ul.y, v.nz) >= v.thr)
			    bmat[bscan.o(x, y)] = v.mat;
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
		    float thr = (Float)desc[2];
		    double nz = (res.name.hashCode() * mid) % 10000;
		    var.add(new Var(res.layer(Material.Res.class, mid).get(), thr, nz));
		}
	    }
	    return(new TerrainTile(id, res.name.hashCode(), base, var.toArray(new Var[0])));
	}
    }

    public TerrainTile(int id, long nseed, GLState base, Var[] var) {
	super(id);
	this.noise = new SNoise3(nseed);
	this.base = GLState.compose(base, States.vertexcolor);
	this.var = var;
    }

    public class Plane extends MapMesh.Shape {
	public MapMesh.SPoint[] vrt;
	public Coord3f[] tc;
	float u, l;

	public Plane(MapMesh m, MapMesh.Surface surf, Coord sc, int z, GLState mat) {
	    m.super(z, mat);
	    vrt = surf.fortile(sc);
	    float fac = 25f / 4f;
	    tc = new Coord3f[] {
		new Coord3f((sc.x + 0) / fac, (sc.y + 0) / fac, 0),
		new Coord3f((sc.x + 0) / fac, (sc.y + 1) / fac, 0),
		new Coord3f((sc.x + 1) / fac, (sc.y + 1) / fac, 0),
		new Coord3f((sc.x + 1) / fac, (sc.y + 0) / fac, 0),
	    };
	    l = m.ul.x * 11;
	    u = m.ul.y * 11;
	}

	public void build(MeshBuf buf) {
	    MeshBuf.Tex btex = buf.layer(MeshBuf.tex);
	    MeshBuf.Col bcol = buf.layer(MeshBuf.col);
	    MeshBuf.Vertex v1 = buf.new Vertex(vrt[0].pos, vrt[0].nrm);
	    MeshBuf.Vertex v2 = buf.new Vertex(vrt[1].pos, vrt[1].nrm);
	    MeshBuf.Vertex v3 = buf.new Vertex(vrt[2].pos, vrt[2].nrm);
	    MeshBuf.Vertex v4 = buf.new Vertex(vrt[3].pos, vrt[3].nrm);
	    btex.set(v1, tc[0]); bcol.set(v1, new Color(255, noise.geti(0, 256, 100, vrt[0].pos.x + l, -vrt[0].pos.y + u, 0), 255));
	    btex.set(v2, tc[1]); bcol.set(v2, new Color(255, noise.geti(0, 256, 100, vrt[1].pos.x + l, -vrt[1].pos.y + u, 0), 255));
	    btex.set(v3, tc[2]); bcol.set(v3, new Color(255, noise.geti(0, 256, 100, vrt[2].pos.x + l, -vrt[2].pos.y + u, 0), 255));
	    btex.set(v4, tc[3]); bcol.set(v4, new Color(255, noise.geti(0, 256, 100, vrt[3].pos.x + l, -vrt[3].pos.y + u, 0), 255));
	    MapMesh.splitquad(buf, v1, v2, v3, v4);
	}
    }

    public void lay(MapMesh m, Random rnd, Coord lc, Coord gc) {
	Blend b = m.data(blend);
	new Plane(m, m.gnd(), lc, 0, b.bmat[b.bscan.o(lc)]);
    }

    public void trans(MapMesh m, Random rnd, Tiler gt, Coord lc, Coord gc, int z, int bmask, int cmask) {
    }
}
