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
    public final GLState mat;

    @ResName("trn")
    public static class Factory implements Tiler.Factory {
	public Tiler create(int id, Resource.Tileset set) {
	    Material.Res mat = set.getres().layer(Material.Res.class);
	    return(new TerrainTile(id, mat.get()));
	}
    }

    public TerrainTile(int id, GLState mat) {
	super(id);
	this.mat = mat;
    }

    public static class Plane extends MapMesh.Shape {
	public MapMesh.SPoint[] vrt;
	public Coord3f[] tc;

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
	}

	public void build(MeshBuf buf) {
	    MeshBuf.Tex btex = buf.layer(MeshBuf.tex);
	    MeshBuf.Vertex v1 = buf.new Vertex(vrt[0].pos, vrt[0].nrm);
	    MeshBuf.Vertex v2 = buf.new Vertex(vrt[1].pos, vrt[1].nrm);
	    MeshBuf.Vertex v3 = buf.new Vertex(vrt[2].pos, vrt[2].nrm);
	    MeshBuf.Vertex v4 = buf.new Vertex(vrt[3].pos, vrt[3].nrm);
	    btex.set(v1, tc[0]);
	    btex.set(v2, tc[1]);
	    btex.set(v3, tc[2]);
	    btex.set(v4, tc[3]);
	    MapMesh.splitquad(buf, v1, v2, v3, v4);
	}
    }

    public void lay(MapMesh m, Random rnd, Coord lc, Coord gc) {
	new Plane(m, m.gnd(), lc, 0, mat);
    }

    public void trans(MapMesh m, Random rnd, Tiler gt, Coord lc, Coord gc, int z, int bmask, int cmask) {
    }
}
