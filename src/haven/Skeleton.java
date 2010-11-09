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

public class Skeleton {
    public Map<String, Bone> bones = new HashMap<String, Bone>();
    public Bone[] blist;
    
    public class Bone {
	public String name;
	public Coord3f ipos, irax;
	public float irang;
	public Bone parent;
	int idx;
	
	public Bone(String name, Coord3f ipos, Coord3f irax, float irang) {
	    this.name = name;
	    this.ipos = ipos;
	    this.irax = irax;
	    this.irang = irang;
	}
    }
    
    public static class Res extends Resource.Layer {
	public final Skeleton s;
	
	public Res(Resource res, byte[] buf) {
	    res.super();
	    s = new Skeleton();
	    Map<Bone, String> pm = new HashMap<Bone, String>();
	    int[] off = {0};
	    while(off[0] < buf.length) {
		String bnm = Utils.strd(buf, off);
		Coord3f pos = new Coord3f((float)Utils.floatd(buf, off[0]), (float)Utils.floatd(buf, off[0] + 5), (float)Utils.floatd(buf, off[0] + 10));
		off[0] += 15;
		Coord3f rax = new Coord3f((float)Utils.floatd(buf, off[0]), (float)Utils.floatd(buf, off[0] + 5), (float)Utils.floatd(buf, off[0] + 10));
		off[0] += 15;
		float rang = (float)Utils.floatd(buf, off[0]);
		off[0] += 5;
		String bp = Utils.strd(buf, off);
		Bone b = s.new Bone(bnm, pos, rax, rang);
		if(s.bones.put(bnm, b) != null)
		    throw(new Resource.LoadException("Duplicate bone name: " + bnm, getres()));
		pm.put(b, bp);
	    }
	    s.blist = new Bone[s.bones.size()];
	    int idx = 0;
	    for(Bone b : s.bones.values()) {
		s.blist[b.idx = idx++] = b;
		String bp = pm.get(b);
		if(bp.length() == 0) {
		    b.parent = null;
		} else {
		    if((b.parent = s.bones.get(bp)) == null)
			throw(new Resource.LoadException("Parent bone " + bp + " not found for " + b.name, getres()));
		}
	    }
	}
	
	public void init() {}
    }
}
