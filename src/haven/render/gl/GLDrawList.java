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
    private DrawSlot root = null;

    private static int btheight(DrawSlot s) {
	return((s == null) ? 0 : s.th);
    }

    DrawSlot first() {
	if(root == null)
	    return(null);
	for(DrawSlot s = root; true; s = s.tl) {
	    if(s.tl == null)
		return(s);
	}
    }

    private static final Comparator<DrawSlot> order = null;
    private static AtomicLong uniqid = new AtomicLong();
    private class DrawSlot {
	final long sortid;
	DrawSlot tp, tl, tr;
	int th = 0;

	DrawSlot() {
	    sortid = uniqid.getAndIncrement();
	}

	DrawSlot prev() {
	    if(tl != null) {
		for(DrawSlot s = tl; true; s = s.tr) {
		    if(s.tr == null)
			return(s);
		}
	    } else {
		for(DrawSlot s = tp, ps = this; s != null; ps = s, s = s.tp) {
		    if(s.tl != ps)
			return(s);
		}
		return(null);
	    }
	}
	DrawSlot next() {
	    if(tr != null) {
		for(DrawSlot s = tr; true; s = s.tl) {
		    if(s.tl == null)
			return(s);
		}
	    } else {
		for(DrawSlot s = tp, ps = this; s != null; ps = s, s = s.tp) {
		    if(s.tr != ps)
			return(s);
		}
		return(null);
	    }
	}

	private int setheight() {
	    return(th = (Math.max(btheight(tl), btheight(tr)) + 1));
	}

	private void bbtrl() {
	    if(btheight(tr.tl) > btheight(tr.tr))
		tr.bbtrr();
	    DrawSlot p = tp, r = tr, rl = r.tl;
	    (tr = rl).tp = this;
	    setheight();
	    (r.tl = this).tp = r;
	    r.setheight();
	    if(p == null)
		(root = r).tp = null;
	    else if(p.tl == this)
		(p.tl = r).tp = p;
	    else
		(p.tr = r).tp = p;
	}
	private void bbtrr() {
	    if(btheight(tl.tr) > btheight(tl.tl))
		tl.bbtrl();
	    DrawSlot p = tp, l = tl, lr = l.tr;
	    (tl = lr).tp = this;
	    setheight();
	    (l.tr = this).tp = l;
	    l.setheight();
	    if(p == null)
		(root = l).tp = null;
	    else if(p.tl == this)
		(p.tl = l).tp = p;
	    else
		(p.tr = l).tp = p;
	}
	private void insert(DrawSlot child) {
	    int c = order.compare(child, this);
	    if(c < 0) {
		if(tl == null)
		    (tl = child).tp = this;
		else
		    tl.insert(child);
	    } else if(c > 0) {
		if(tr == null)
		    (tr = child).tp = this;
		else
		    tr.insert(child);
	    } else {
		throw(new RuntimeException());
	    }
	    if(btheight(tl) > btheight(tr) + 1)
		bbtrr();
	    if(btheight(tr) > btheight(tl) + 1)
		bbtrl();
	    setheight();
	}
	void insert() {
	    if((tp != null) || (root == this))
		throw(new IllegalStateException());
	    th = 1;
	    if(root == null) {
		root = this;
	    } else {
		root.insert(this);
	    }
	}
	void remove() {
	    if((tp == null) && (root != this))
		throw(new IllegalStateException());
	    DrawSlot rep;
	    if((tl != null) && (tr != null)) {
		for(rep = tr; rep.tl != null; rep = rep.tl);
		if(rep.tr != null) {
		    DrawSlot p = rep.tp;
		    if(p.tl == rep)
			(p.tl = rep.tr).tp = p;
		    else
			(p.tr = rep.tr).tp = p;
		}
		(rep.tl = tl).tp = rep;
		(rep.tr = tr).tp = rep;
	    } else if(tl != null) {
		rep = tl;
	    } else if(tr != null) {
		rep = tr;
	    } else {
		rep = null;
	    }
	    if(tp != null) {
		if(tp.tl == this)
		    tp.tl = rep;
		else
		    tp.tr = rep;
	    } else {
		root = rep;
	    }
	    if(rep != null)
		rep.tp = tp;
	    tr = tl = tp = null;
	}
    }

    private void verify(DrawSlot t) {
	if(t.tl != null) {
	    if(t.tl.tp != t)
		throw(new AssertionError());
	    if(order.compare(t.tl, t) >= 0)
		throw(new AssertionError());
	    verify(t.tl);
	}
	if(t.tr != null) {
	    if(t.tr.tp != t)
		throw(new AssertionError());
	    if(order.compare(t.tr, t) <= 0)
		throw(new AssertionError());
	    verify(t.tr);
	}
    }
    private void verify() {
	if(root != null) {
	    if(root.tp != null)
		throw(new AssertionError());
	    verify(root);
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
