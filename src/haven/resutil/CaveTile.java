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

public class CaveTile extends Tiler {
    public final Resource.Tileset set;
    public final int h;
    public final Material wtex;
    
    public CaveTile(int id, Resource.Tileset set, int h, Tex wtex) {
	super(id);
	this.set = set;
	this.h = h;
	this.wtex = new Material(wtex);
    }
    
    public void lay(MapMesh m, Random rnd, Coord lc, Coord gc) {
	Tile g = set.ground.pick(rnd);
	m.new Plane(m.gnd(), lc, 0, g);
    }
    
    private void wall(MeshBuf buf, MapMesh.SPoint s1, MapMesh.SPoint s2, Coord3f nrm) {
	MeshBuf.Vertex v1 = buf.new Vertex(s1.pos, nrm, new Coord3f(0, 1, 0)),
	    v2 = buf.new Vertex(s2.pos, nrm, new Coord3f(1, 1, 0)),
	    v3 = buf.new Vertex(s2.pos.add(0, 0, h), nrm, new Coord3f(1, 0, 0)),
	    v4 = buf.new Vertex(s1.pos.add(0, 0, h), nrm, new Coord3f(0, 0, 0));
	buf.new Face(v1, v3, v4);
	buf.new Face(v1, v2, v3);
    }

    public void trans(MapMesh m, Random rnd, Tiler gt, Coord lc, Coord gc, int z, int bmask, int cmask) {
	int cid = m.map.gettile(gc);
	if((cid <= id) || (m.map.tiler(cid) instanceof CaveTile))
	    return;
	if(bmask == 0)
	    return;
	MeshBuf buf = m.model(wtex, MeshBuf.class);
	MapMesh.Surface gnd = m.gnd();
	if((bmask & 1) != 0)
	    wall(buf, gnd.spoint(lc.add(0, 1)), gnd.spoint(lc), new Coord3f(1, 0, 0));
	if((bmask & 2) != 0)
	    wall(buf, gnd.spoint(lc), gnd.spoint(lc.add(1, 0)), new Coord3f(0, -1, 0));
	if((bmask & 4) != 0)
	    wall(buf, gnd.spoint(lc.add(1, 0)), gnd.spoint(lc.add(1, 1)), new Coord3f(-1, 0, 0));
	if((bmask & 8) != 0)
	    wall(buf, gnd.spoint(lc.add(1, 1)), gnd.spoint(lc.add(0, 1)), new Coord3f(0, 1, 0));
    }
}
