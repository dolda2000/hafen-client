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

package haven.render.gl;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import haven.render.*;

public class GLDrawList implements DrawList {
    public final GLEnvironment env;
    private DrawSlot[] slots = new DrawSlot[0];

    public static int parenti(int n) {
	int l = n & -n;
	return((n & ~((l << 2) - 1)) + (l << 1));
    }
    public static int lefti(int n) {
	int l = n & -n;
	if(l == 1)
	    return(0);
	return(n - (l >> 1));
    }
    public static int righti(int n) {
	int l = n & -n;
	if(l == 1)
	    return(0);
	return(n + (l >> 1));
    }

    int rooti() {
	return(slots.length << 1);
    }
    DrawSlot root() {
	int n = rooti();
	return((n == 0) ? null : slots[n]);
    }

    void resize(int nl) {
	DrawSlot[] cs = this.slots, ns = new DrawSlot[nl];
	for(int i = 1; i < cs.length; i++) {
	    ns[i * 2] = cs[i];
	    if(cs[i] != null)
		cs[i].idx = i * 2;
	}
	this.slots = ns;
    }

    private static int btheight(int n) {
	return(((n == 0) || (slots[n] == null)) ? 0 : slots[n].th);
    }

    private void bbtrr(int t) {
	int l = lefti(t), ll = lefti(l), lr = righti(l);
	if(btheight(lr) > btheight(ll))
	    bbtrl(l);
	
    }
    private void bbtrl(int t) {
    }

    private static final Comparator<DrawSlot> order = null;
    private static AtomicLong uniqid = new AtomicLong();
    private class DrawSlot {
	final long sortid;
	int idx = 0, th = 0;

	DrawSlot() {
	    sortid = uniqid.getAndIncrement();
	}

	DrawSlot parent() {
	    if(idx == 0) throw(new IllegalStateException());
	    int n = parenti(idx);
	    return((n == slots.length) ? null : slots[n]);
	}
	DrawSlot left() {
	    if(idx == 0) throw(new IllegalStateException());
	    int n = lefti(idx);
	    return((n == 0) ? null : slots[n]);
	}
	DrawSlot right() {
	    if(idx == 0) throw(new IllegalStateException());
	    int n = righti(idx);
	    return((n == 0) ? null : slots[n]);
	}
	DrawSlot prev() {
	    for(int i = idx - 1; i > 0; i--) {
		if(slots[i] != null)
		    return(slots[i]);
	    }
	    return(null);
	}
	DrawSlot next() {
	    for(int i = idx + 1; i < slots.length; i++) {
		if(slots[i] != null)
		    return(slots[i]);
	    }
	    return(null);
	}

	private int setheight() {
	    DrawSlot l = left(), r = right();
	    return(th = (Math.max((l == null) ? 0 : l.th, (r == null) ? 0 : r.th) + 1));
	}

	private int insidx(DrawSlot child) {
	    int c = order.compare(child, this);
	    if(c < 0) {
		return(lefti(idx));
	    } else if(c > 0) {
		return(righti(idx));
	    } else {
		throw(new AssertionError());
	    }
	}
	private void insert(DrawSlot child) {
	    int ci = insidx(child);
	    if(ci == 0) {
		resize(slots.length * 2);
		ci = insidx(child);
	    }
	    if(slots[ci] == null) {
		slots[ci] = child;
		child.idx = ci;
		child.th = 1;
	    } else {
		slots[ci].insert(child);
	    }
	    if(btheight(lefti(idx)) > btheight(righti(idx) + 1))
		bbtrr(idx);
	    if(btheight(righti(idx)) > btheight(lefti(idx) + 1))
		bbtrl(idx);
	    setheight();
	}
	void insert() {
	    if(idx != 0) throw(new IllegalStateException());
	    DrawSlot root = root();
	    if(root == null) {
		slots = new DrawSlot[2];
		slots[1] = this;
		this.idx = 1;
	    } else {
		root.insert(this);
	    }
	}
    }

    public GLDrawList(GLEnvironment env) {
	this.env = env;
    }

    public void draw(Render r) {
    }

    public void add(Slot<Rendered> slot) {
    }

    public void remove(Slot<Rendered> slot) {
    }

    public void update(Slot<Rendered> slot) {
    }

    public void update(Pipe group) {
    }

    public void dispose() {
    }
}
