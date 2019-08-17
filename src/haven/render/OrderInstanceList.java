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

package haven.render;

import java.util.*;
import java.util.concurrent.atomic.*;
import haven.*;
import haven.render.Rendered.Instancable;
import haven.render.Rendered.Instanced;

public class OrderInstanceList implements RenderList<Rendered> {
    private final RenderList<Rendered> back;
    private final Map<Pipe, Object> orderidx = new IdentityHashMap<>();
    private final Map<Slot<? extends Rendered>, OrderSlot> slotmap = new IdentityHashMap<>();
    private OrderSlot root = null;

    private static int[] _uinstidlist = null;
    private static int[] uinstidlist() {
	State.Slot.Slots si = State.Slot.slots;
	int[] ret = _uinstidlist;
	if((ret != null) || (ret.length == (si.idlist.length + 1)))
	    return(ret);
	ret = new int[si.idlist.length];
	for(int i = 0, n = 0; i < ret.length; i++) {
	    if(si.idlist[i].instanced == null)
		ret[i] = n++;
	    else
		ret[i] = -n - 1;
	}
	return(_uinstidlist = ret);
    }

    private static Pipe[] uinststate(GroupPipe st) {
	int[] us = uinstidlist();
	int ls;
	for(ls = st.nstates() - 1; (ls >= 0) && (st.gstate(ls) < 0); ls--);
	if(ls < 0)
	    return(new Pipe[0]);
	int c = us[ls];
	Pipe[] ret = new Pipe[(c >= 0) ? (c + 1) : (-c - 1)];
	for(int i = 0; i <= ls; i++) {
	    if(us[i] >= 0) {
		int gn = st.gstate(i);
		if(gn >= 0)
		    ret[us[i]] = st.group(gn);
	    }
	}
	return(ret);
    }

    private static int btheight(OrderSlot s) {
	return((s == null) ? 0 : s.th);
    }
    private static int btsubsize(OrderSlot s) {
	return((s == null) ? 0 : s.tsubsize);
    }
    private static OrderSlot setp(OrderSlot s, OrderSlot p) {
	if(s != null)
	    s.tp = p;
	return(s);
    }

    private static int istcompare(OrderSlot a, OrderSlot b) {
	int c;
	if((c = Utils.idcmp.compare(a.bk, b.bk)) != 0)
	    return(c);
	if((c = Utils.idcmp.compare(a.instid, b.instid)) != 0)
	    return(c);
	if((a.ust == null) && (b.ust != null))
	    return(-1);
	if((a.ust != null) && (b.ust == null))
	    return(1);
	if((c = (a.ust.length - b.ust.length)) != 0)
	    return(c);
	for(int i = 0; i < a.ust.length; i++) {
	    if((c = Utils.idcmp.compare(a.ust[i], b.ust[i])) != 0)
		return(c);
	}
	return(0);
    }

    private static int ordercompare(OrderSlot a, OrderSlot b) {
	if(a == b)
	    return(0);
	int c;
	if((c = Rendered.Order.cmp.compare(a.gorder, b.gorder)) != 0)
	    return(c);
	if((c = istcompare(a, b)) != 0)
	    return(c);
	return((a.sortid < b.sortid) ? -1 : 1);
    }

    private static AtomicLong uniqid = new AtomicLong();
    private abstract class OrderSlot {
	/* List structure */
	final long sortid = uniqid.getAndIncrement();
	OrderSlot tp, tl, tr;
	int th = 1;
	int tsubsize = 1;	/* Not critical, only used for debugging purposes. */

	OrderSlot prev() {
	    if(tl != null) {
		for(OrderSlot s = tl; true; s = s.tr) {
		    if(s.tr == null)
			return(s);
		}
	    } else {
		for(OrderSlot s = tp, ps = this; s != null; ps = s, s = s.tp) {
		    if(s.tl != ps)
			return(s);
		}
		return(null);
	    }
	}
	OrderSlot next() {
	    if(tr != null) {
		for(OrderSlot s = tr; true; s = s.tl) {
		    if(s.tl == null)
			return(s);
		}
	    } else {
		for(OrderSlot s = tp, ps = this; s != null; ps = s, s = s.tp) {
		    if(s.tr != ps)
			return(s);
		}
		return(null);
	    }
	}

	private int setheight() {
	    tsubsize = btsubsize(tl) + btsubsize(tr) + 1;
	    return(th = (Math.max(btheight(tl), btheight(tr)) + 1));
	}

	private void bbtrl() {
	    if(btheight(tr.tl) > btheight(tr.tr))
		tr.bbtrr();
	    OrderSlot p = tp, r = tr, rl = r.tl;
	    setp(tr = rl, this);
	    setheight();
	    setp(r.tl = this, r);
	    r.setheight();
	    if(p == null)
		setp(root = r, null);
	    else if(p.tl == this)
		setp(p.tl = r, p);
	    else
		setp(p.tr = r, p);
	}
	private void bbtrr() {
	    if(btheight(tl.tr) > btheight(tl.tl))
		tl.bbtrl();
	    OrderSlot p = tp, l = tl, lr = l.tr;
	    setp(tl = lr, this);
	    setheight();
	    setp(l.tr = this, l);
	    l.setheight();
	    if(p == null)
		setp(root = l, null);
	    else if(p.tl == this)
		setp(p.tl = l, p);
	    else
		setp(p.tr = l, p);
	}

	void insert(OrderSlot child, boolean d) {
	    if((d ? tr : tl) != null)
		throw(new AssertionError());
	    if(d)
		(tr = child).tp = this;
	    else
		(tl = child).tp = this;
	    for(OrderSlot s = this, p = s.tp; s != null; s = p, p = s.tp) {
		if(btheight(s.tl) > btheight(s.tr) + 1)
		    s.bbtrr();
		else if(btheight(s.tr) > btheight(s.tl) + 1)
		    s.bbtrl();
		s.setheight();
	    }
	}
	void tremove() {
	    if((tp == null) && (root != this))
		throw(new IllegalStateException());
	    OrderSlot rep;
	    if((tl != null) && (tr != null)) {
		for(rep = tr; rep.tl != null; rep = rep.tl);

		OrderSlot p = rep.tp;
		if(p.tl == rep)
		    setp(p.tl = rep.tr, p);
		else
		    setp(p.tr = rep.tr, p);
		p.setheight();

		setp(rep.tl = tl, rep);
		setp(rep.tr = tr, rep);
		rep.setheight();
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
	    for(OrderSlot p = tp, pp; p != null; p = pp) {
		p.setheight();
		pp = p.tp;
		if(btheight(p.tl) > btheight(p.tr) + 1)
		    p.bbtrr();
		else if(btheight(p.tr) > btheight(p.tl) + 1)
		    p.bbtrl();
	    }
	    tr = tl = tp = null;
	}
	void treplace(OrderSlot repl) {
	    repl.tp = this.tp;
	    repl.tl = setp(this.tl, repl);
	    repl.tr = setp(this.tr, repl);
	    if(this.tp.tl == this)
		this.tp.tl = repl;
	    else if(this.tp.tr == this)
		this.tp.tr = repl;
	    else
		throw(new AssertionError());
	}

	/* Instancing information */
	final Rendered bk;
	final Object instid;
	final Pipe[] ust;
	Rendered.Order gorder = Rendered.deflt;

	OrderSlot(Slot<? extends Rendered> slot) {
	    this.bk = slot.obj();
	    this.instid = (bk instanceof Instanced) ? ((Instancable)bk).instanceid() : null;
	    this.ust = (instid == null) ? null : uinststate(slot.state());
	}

	void remove() {
	    tremove();
	}

	abstract void merge(Slot<? extends Rendered> slot);

	public void dispose() {}
    }

    private class NormalSlot extends OrderSlot {
	final Slot<? extends Rendered> bkslot;

	NormalSlot(Slot<? extends Rendered> slot) {
	    super(slot);
	    this.bkslot = slot;
	}

	@SuppressWarnings("unchecked")
	void merge(Slot<? extends Rendered> slot) {
	    Slot<? extends Rendered>[] slots = new Slot[2];
	    Rendered.Order no = slot.state().get(Rendered.order);
	    if(Rendered.Order.cmp.compare(gorder, (no == null) ? Rendered.deflt : no) > 0) {
		slots[0] = slot;
		slots[1] = bkslot;
	    } else {
		slots[0] = bkslot;
		slots[1] = slot;
	    }
	    InstancedSlot ni = new InstancedSlot(slots);
	    back.remove(bkslot);
	    try {
		back.add(ni);
	    } catch(RuntimeException e) {
		try {
		    back.add(bkslot);
		} catch(RuntimeException e2) {
		    Error err = new Error("Unexpected non-local exit", e2);
		    err.addSuppressed(e);
		    throw(err);
		}
		throw(e);
	    }
	    treplace(ni);
	}
    }

    private static class InstanceState extends BufPipe {
	final int[] mask;

	private <T extends State> void inststate0(State.Slot<T> slot, GroupPipe from) {
	    this.put(slot, slot.instanced.inststate(from.get(slot)));
	}

	InstanceState(GroupPipe from) {
	    int ns = 0, fn = from.nstates();;
	    int[] mask = new int[fn];
	    for(int i = 0; i < fn; i++) {
		State.Slot<?> slot = State.Slot.byid(i);
		if(slot.instanced != null) {
		    inststate0(slot, from);
		    mask[ns++] = i;
		}
	    }
	    this.mask = Arrays.copyOf(mask, ns);
	}

	public static int compare(InstanceState x, InstanceState y) {
	    int c;
	    if((c = x.mask.length - y.mask.length) != 0)
		return(c);
	    for(int i = 0; i < x.mask.length; i++) {
		if((c = x.mask[i] - y.mask[i]) != 0)
		    return(c);
		State.Slot<?> slot = State.Slot.byid(x.mask[i]);
		if((c = Utils.idcmp.compare(x.get(slot), x.get(slot))) != 0)
		    return(c);
	    }
	    return(0);
	}
    }

    private class InstancedSlot extends OrderSlot implements RenderList.Slot<Rendered> {
	final Instanced rend;
	final InstanceState ist;
	final GroupPipe ust;
	Instance[] insts;

	class Instance {
	    final Slot<? extends Rendered> slot;
	    int idx;

	    Instance(Slot<? extends Rendered> slot) {
		this.slot = slot;
	    }
	}

	InstancedSlot(Slot<? extends Rendered>[] slots) {
	    super(slots[0]);
	    this.ust = slots[0].state();
	    this.ist = new InstanceState(ust);
	    Instance[] insts = new Instance[slots.length];
	    for(int i = 0; i < slots.length; i++) {
		insts[i] = new Instance(slots[i]);
		insts[i].idx = i;
		if((i > 0) && (InstanceState.compare(this.ist, new InstanceState(slots[i].state())) != 0))
		    throw(new RuntimeException("instantiation-IDs not yet implemented"));
	    }
	    this.insts = insts;
	    this.rend = ((Instancable)this.bk).instancify();
	}

	private class StateSum implements GroupPipe {
	    public Pipe group(int idx) {
		if(idx == 0)
		    return(ist);
		return(ust.group(idx - 1));
	    }

	    public int gstate(int id) {
		if(State.Slot.byid(id).instanced != null)
		    return(0);
		int ret = ust.gstate(id);
		return((ret < 0) ? ret : (ret + 1));
	    }

	    public int nstates() {
		return(ust.nstates());
	    }
	}

	public Rendered obj() {
	    return(rend);
	}

	private GroupPipe state = null;
	public GroupPipe state() {
	    if(state == null)
		state = new StateSum();
	    return(state);
	}

	void merge(Slot<? extends Rendered> slot) {
	    if(InstanceState.compare(this.ist, new InstanceState(slot.state())) != 0)
		throw(new RuntimeException("instantiation-IDs not yet implemented"));
	}
    }

    public OrderInstanceList(RenderList<Rendered> back) {
	this.back = back;
    }

    public void add(Slot<? extends Rendered> slot) {
	OrderSlot probe = new NormalSlot(slot);

	synchronized(this) {
	    OrderSlot found;
	    boolean flt;
	    findinsert: {
		OrderSlot s = root;
		if(s == null) {
		    found = null;
		    flt = false;
		} else {
		    while(true) {
			int c = ordercompare(probe, s);
			if(c == 0)
			    throw(new AssertionError());
			flt = (c > 0);
			OrderSlot ch = flt ? s.tr : s.tl;
			if(ch == null) {
			    found = ch;
			    break findinsert;
			} else {
			    s = ch;
			}
		    }
		}
	    }

	    if(found == null) {
		back.add(slot);
		root = probe;
		slotmap.put(slot, probe);
	    } else {
		OrderSlot eq = null;
		if(probe.instid != null) {
		    if(istcompare(probe, found) == 0) {
			eq = found;
		    } else {
			OrderSlot other = flt ? found.next() : found.prev();
			if(istcompare(probe, other) == 0)
			    eq = other;
		    }
		}
		if(eq != null) {
		    eq.merge(slot);
		} else {
		    back.add(slot);
		    found.insert(probe, flt);
		    slotmap.put(slot, probe);
		}
	    }
	}
    }

    public void remove(Slot<? extends Rendered> slot) {
	synchronized(this) {
	    OrderSlot oslot = slotmap.remove(slot);
	    if(oslot == null)
		throw(new AssertionError());
	    back.remove(slot);
	    oslot.remove();
	    oslot.dispose();
	}
    }

    public void update(Slot<? extends Rendered> slot) {
	synchronized(this) {
	    OrderSlot oslot = slotmap.get(slot);
	    if(oslot instanceof NormalSlot) {
		back.update(slot);
	    } else {
		/* XXX */
	    }
	}
    }

    public void update(Pipe group, int[] mask) {
	back.update(group, mask);
    }
}
