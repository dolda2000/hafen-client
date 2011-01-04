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
 *  Other parts of t(his source tree adhere to other copying
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

public class GrowingPlant extends StaticSprite {
    public static class Factory implements Sprite.Factory {
	private final int sn;
	
	public Factory(int sn) {
	    this.sn = sn;
	}
	
	public Sprite create(Owner owner, Resource res, Message sdt) {
	    return(new GrowingPlant((Gob)owner, res, sn, sdt.uint8()));
	}
    }
    
    private static Rendered[] cons(Gob gob, Resource res, int sn, int st) {
	List<Integer> meshes = new ArrayList<Integer>();
	Collection<FastMesh.MeshRes> rc = res.layers(FastMesh.MeshRes.class);
	for(FastMesh.MeshRes r : rc) {
	    if((r.id == st) && (r.mat != null) && !meshes.contains(r.ref))
		meshes.add(r.ref);
	}
	Map<Material, MeshBuf> parts = new HashMap<Material, MeshBuf>();
	Random rnd = gob.mkrandoom();
	float cz = gob.glob.map.getcz(gob.rc);
	for(int i = 0; i < sn; i++) {
	    Coord3f off;
	    {
		float x = (rnd.nextFloat() * 44) - 22;
		float y = (rnd.nextFloat() * 44) - 22;
		off = new Coord3f(x, y, gob.glob.map.getcz(gob.rc.x + x, gob.rc.y + y) - cz);
	    }
	    int ref = meshes.get(rnd.nextInt(meshes.size()));
	    for(FastMesh.MeshRes r : rc) {
		if(r.ref == ref) {
		    MeshBuf buf = parts.get(r.mat);
		    if(buf == null)
			parts.put(r.mat, buf = new MeshBuf());
		    MeshBuf.Vertex[] cv = buf.copy(r.m);
		    for(MeshBuf.Vertex v : cv) {
			v.pos.x += off.x;
			v.pos.y -= off.y;
			v.pos.z += off.z;
		    }
		}
	    }
	}
	Rendered[] ret = new Rendered[parts.size()];
	int i = 0;
	for(Map.Entry<Material, MeshBuf> e : parts.entrySet())
	    ret[i++] = e.getKey().apply(e.getValue().mkmesh());
	return(ret);
    }

    public GrowingPlant(Gob owner, Resource res, int sn, int st) {
	super(owner, res, cons(owner, res, sn, st));
    }
}
