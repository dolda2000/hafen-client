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

package haven;

import java.util.*;

public interface RenderLink {
    public Rendered make();
    
    @Resource.LayerName("rlink")
    public class Res extends Resource.Layer {
	public transient final RenderLink l;
	
	public Res(Resource res, byte[] buf) {
	    res.super();
	    int t = buf[0];
	    int[] off = {1};
	    if(t == 0) {
		String meshnm = Utils.strd(buf, off);
		int meshver = Utils.uint16d(buf, off[0]); off[0] += 2;
		final int meshid = Utils.int16d(buf, off[0]); off[0] += 2;
		String matnm = Utils.strd(buf, off);
		int matver = Utils.uint16d(buf, off[0]); off[0] += 2;
		final int matid = Utils.int16d(buf, off[0]); off[0] += 2;
		final Resource mesh = Resource.load(meshnm, meshver);
		final Resource mat = Resource.load(matnm, matver);
		l = new RenderLink() {
			Rendered res = null;
			public Rendered make() {
			    if(res == null) {
				FastMesh m = null;
				for(FastMesh.MeshRes mr : mesh.layers(FastMesh.MeshRes.class)) {
				    if((meshid < 0) || (mr.id == meshid)) {
					m = mr.m;
					break;
				    }
				}
				Material M = null;
				for(Material.Res mr : mat.layers(Material.Res.class)) {
				    if((matid < 0) || (mr.id == matid)) {
					M = mr.get();
					break;
				    }
				}
				if(m == null)
				    throw(new Sprite.ResourceException("Could not find specified mesh by ID " + meshid, getres()));
				if(M == null)
				    throw(new Sprite.ResourceException("Could not find specified material by ID " + matid, getres()));
				res = M.apply(m);
			    }
			    return(res);
			}
		    };
	    } else if(t == 1) {
		String nm = Utils.strd(buf, off);
		int ver = Utils.uint16d(buf, off[0]); off[0] += 2;
		final Resource amb = Resource.load(nm, ver);
		l = new RenderLink() {
			public Rendered make() {
			    return(new ActAudio.Ambience(amb));
			}
		    };
	    } else if(t == 2) {
		String nm = Utils.strd(buf, off);
		int ver = Utils.uint16d(buf, off[0]); off[0] += 2;
		final Resource lres = Resource.load(nm, ver);
		final int meshid = Utils.int16d(buf, off[0]); off[0] += 2;
		l = new RenderLink() {
			Rendered res = null;
			public Rendered make() {
			    if(res == null) {
				ArrayList<Rendered> cl = new ArrayList<Rendered>();
				for(FastMesh.MeshRes mr : lres.layers(FastMesh.MeshRes.class)) {
				    if((mr.id < 0) || (mr.id == meshid))
					cl.add(mr.mat.get().apply(mr.m));
				}
				final Rendered[] ca = cl.toArray(new Rendered[0]);
				res = new Rendered() {
					public void draw(GOut g) {}
					public boolean setup(RenderList ls) {
					    for(Rendered r : ca)
						ls.add(r, null);
					    return(false);
					}
				    };
			    }
			    return(res);
			}
		    };
	    } else {
		throw(new Resource.LoadException("Invalid renderlink type: " + t, getres()));
	    }
	}
	
	public void init() {
	}
    }
}
