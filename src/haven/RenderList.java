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

public class RenderList {
    Slot[] list = new Slot[100];
    int cur = 0;
    private Slot curp = null;
    Collection<LSlot> lights = new ArrayList<LSlot>();
    
    class Slot {
	Rendered r;
	Transform t;
	Rendered.Order o;
	Slot p;
    }
    
    class LSlot {
	Light l;
	Slot p;
    }
    
    public void add(Rendered r, Transform t) {
	int i = cur++;
	if(i >= list.length) {
	    Slot[] n = new Slot[i * 2];
	    System.arraycopy(list, 0, n, 0, i);
	    list = n;
	}
	Slot s;
	if((s = list[i]) == null)
	    s = list[i] = new Slot();
	s.r = r;
	s.t = t;
	Slot pp = s.p = curp;
	try {
	    curp = list[i];
	    s.o = r.setup(this);
	} finally {
	    curp = pp;
	}
    }
    
    public void add(Light l) {
	LSlot s = new LSlot();
	s.l = l;
	s.p = curp;
	lights.add(s);
    }
    
    @SuppressWarnings("unchecked")
    private static final Comparator<Slot> cmp = new Comparator<Slot>() {
	public int compare(Slot a, Slot b) {
	    if((a.o == null) && (b.o == null))
		return(0);
	    if((a.o != null) && (b.o == null))
		return(-1);
	    if((a.o == null) && (b.o != null))
		return(1);
	    int az = a.o.mainz(), bz = b.o.mainz();
	    if(az != bz)
		return(az - bz);
	    if(a.o != b.o)
		throw(new RuntimeException("Found two different orderings with the same main-Z: " + a.o + " and " + b.o));
	    return(a.o.cmp().compare(a.r, b.r));
	}
    };
    
    public void sort() {
	Arrays.sort(list, 0, cur, cmp);
    }
    
    public void rewind() {
	if(curp != null)
	    throw(new RuntimeException("Tried to rewind RenderList while adding to it."));
	cur = 0;
	lights.clear();
    }
}
