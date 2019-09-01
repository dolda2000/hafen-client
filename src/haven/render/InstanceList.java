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

public class InstanceList implements RenderList<Rendered>, Disposable {
    private final RenderList<Rendered> back;
    private final Map<InstKey, Object> instreg = new HashMap<>();
    private final Map<Slot<? extends Rendered>, InstancedSlot.Instance> slotmap = new IdentityHashMap<>();
    private final Map<Pipe, Object> pipemap = new IdentityHashMap<>();

    private static int[] _uinstidlist = null;
    private static int[] uinstidlist() {
	State.Slot.Slots si = State.Slot.slots;
	int[] ret = _uinstidlist;
	if((ret != null) && (ret.length == (si.idlist.length + 1)))
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

    private static class InstKey {
	final Object instid;
	final Pipe[] ust;

	InstKey(Slot<? extends Rendered> slot) {
	    this.instid = ((Instancable)slot.obj()).instanceid();
	    this.ust = uinststate(slot.state());
	}

	public int hashCode() {
	    int ret = System.identityHashCode(instid);
	    for(int i = 0; i < ust.length; i++)
		ret = (ret * 31) + System.identityHashCode(ust[i]);
	    return(ret);
	}

	private boolean equals(InstKey that) {
	    if(this.instid != that.instid)
		return(false);
	    if(this.ust.length != that.ust.length)
		return(false);
	    for(int i = 0; i < ust.length; i++) {
		if(this.ust[i] != that.ust[i])
		    return(false);
	    }
	    return(true);
	}

	public boolean equals(Object x) {
	    return((x instanceof InstKey) ? equals((InstKey)x) : false);
	}
    }

    private static class InstanceState extends BufPipe {
	final int[] mask;

	private <T extends State> void inststate0(State.Slot<T> slot, GroupPipe from, InstancedSlot batch) {
	    this.put(slot, slot.instanced.inststate(from.get(slot), batch));
	}

	InstanceState(GroupPipe from, InstancedSlot batch) {
	    int ns = 0, fn = from.nstates();;
	    int[] mask = new int[fn];
	    for(int i = 0; i < fn; i++) {
		State.Slot<?> slot = State.Slot.byid(i);
		if(slot.instanced != null) {
		    inststate0(slot, from, batch);
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

    private class InstancedSlot implements RenderList.Slot<Rendered>, InstanceBatch {
	final Instanced rend;
	final InstanceState ist;
	final GroupPipe ust;
	Instance[] insts;
	int ni;

	class Instance {
	    final Slot<? extends Rendered> slot;
	    int idx;

	    Instance(Slot<? extends Rendered> slot) {
		this.slot = slot;
	    }

	    @SuppressWarnings("unchecked")
	    void register() {
		if(slotmap.put(slot, this) != null)
		    throw(new AssertionError());
		GroupPipe st = slot.state();
		for(int i = 0; i < ist.mask.length; i++) {
		    int gn = st.gstate(ist.mask[i]);
		    if(gn < 0)
			continue;
		    Pipe p = st.group(gn);
		    Object cur = pipemap.get(p);
		    if(cur == null) {
			pipemap.put(p, this);
		    } else if(cur instanceof Instance) {
			List<Instance> nl = new ArrayList<>(2);
			nl.add((Instance)cur);
			nl.add(this);
			pipemap.put(p, this);
		    } else if(cur instanceof List) {
			List<Instance> ls = (List<Instance>)cur;
			ls.add(this);
		    } else {
			throw(new AssertionError());
		    }
		}
	    }

	    @SuppressWarnings("unchecked")
	    void unregister() {
		if(slotmap.remove(slot) != this)
		    throw(new AssertionError());
		GroupPipe st = slot.state();
		for(int i = 0; i < ist.mask.length; i++) {
		    int gn = st.gstate(ist.mask[i]);
		    if(gn < 0)
			continue;
		    Pipe p = st.group(gn);
		    Object cur = pipemap.get(p);
		    if(cur == null) {
			throw(new AssertionError());
		    } else if(cur == this) {
			pipemap.remove(p);
		    } else if(cur instanceof List) {
			List<Instance> ls = (List<Instance>)cur;
			ls.remove(this);
			if(ls.size() < 2)
			    pipemap.put(p, ls.get(0));
		    } else {
			throw(new AssertionError());
		    }
		}
	    }

	    void update(Pipe group, int[] mask) {
	    }
	}

	InstancedSlot(Slot<? extends Rendered>[] slots) {
	    this.ust = slots[0].state();
	    this.ist = new InstanceState(ust, this);
	    this.rend = ((Instancable)slots[0].obj()).instancify(this);
	    Instance[] insts = new Instance[slots.length];
	    for(int i = 0; i < slots.length; i++) {
		insts[i] = new Instance(slots[i]);
		insts[i].idx = i;
		if((i > 0) && (InstanceState.compare(this.ist, new InstanceState(slots[i].state(), this)) != 0))
		    throw(new RuntimeException("instantiation-IDs not yet implemented"));
	    }
	    this.insts = insts;
	    this.ni = slots.length;
	}

	void register() {
	    for(int i = 0; i < ni; i++) {
		insts[i].register();
		iupdate(i);
	    }
	}

	void unregister() {
	    for(int i = 0; i < ni; i++)
		insts[i].unregister();
	}

	private void iupdate(int idx) {
	    rend.iupdate(idx);
	    for(int i = 0; i < ist.mask.length; i++) {
		State st = ist.get(State.Slot.byid(ist.mask[i]));
		if(st instanceof InstanceBatch.Client)
		    ((InstanceBatch.Client)st).iupdate(idx);
	    }
	}

	private void itrim(int idx) {
	    rend.itrim(idx);
	    for(int i = 0; i < ist.mask.length; i++) {
		State st = ist.get(State.Slot.byid(ist.mask[i]));
		if(st instanceof InstanceBatch.Client)
		    ((InstanceBatch.Client)st).itrim(idx);
	    }
	}

	void add(Slot<? extends Rendered> ns) {
	    Instance inst = new Instance(ns);
	    inst.register();
	    if(insts.length == ni)
		insts = Arrays.copyOf(insts, insts.length * 2);
	    insts[inst.idx = ni++] = inst;
	    iupdate(inst.idx);
	}

	void remove(Slot<? extends Rendered> ns) {
	    Instance inst = slotmap.get(ns);
	    int ri = inst.idx;
	    if(insts[ri] != inst)
		throw(new AssertionError());
	    /* De-instancify when ni goes from 2 to 1? It's not
	     * *obviously* better to do so, and if the slot has once
	     * been instancified, it's probably not unreasonable to
	     * expect it to become so again in the future, in which
	     * case the updating overhead can be avoided. */
	    inst.unregister();
	    (insts[ri] = insts[--ni]).idx = inst.idx;
	    inst.idx = -1;
	    if(ni < 0)
		throw(new AssertionError());
	    if(ri < ni)
		iupdate(ri);
	    itrim(ni);
	}

	void dispose() {
	    rend.dispose();
	}

	void update(Slot<? extends Rendered> ns) {
	    InstanceList.this.remove(ns);
	    InstanceList.this.add(ns);
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

	public State.Slot<?>[] batchstates() {
	    State.Slot<?>[] ret = new State.Slot<?>[ist.mask.length];
	    for(int i = 0; i < ist.mask.length; i++)
		ret[i] = State.Slot.byid(ist.mask[i]);
	    return(ret);
	}

	public <T extends State> T batchstate(State.Slot<T> slot) {
	    return(ist.get(slot));
	}

	public int instances() {
	    return(ni);
	}

	public Pipe inststate(int idx) {
	    if(idx >= ni)
		throw(new ArrayIndexOutOfBoundsException(idx));
	    return(insts[idx].slot.state());
	}

	public void instupdate() {
	    back.update(this);
	}

	public <T extends State> void update(State.Slot<? super T> slot, T state) {
	    ist.put(slot, state);
	    back.update(this);
	}
    }

    public InstanceList(RenderList<Rendered> back) {
	this.back = back;
    }

    @SuppressWarnings("unchecked")
    public void add(Slot<? extends Rendered> slot) {
	if(!(slot.obj() instanceof Instancable)) {
	    back.add(slot);
	    return;
	}
	InstKey key = new InstKey(slot);
	synchronized(this) {
	    Object cur = instreg.get(key);
	    if(cur == null) {
		back.add(slot);
		instreg.put(key, slot);
	    } else if(cur instanceof Slot) {
		Slot<? extends Rendered> cs = (Slot<? extends Rendered>)cur;
		InstancedSlot ni = new InstancedSlot(new Slot[] {cs, slot});
		back.remove(cs);
		try {
		    back.add(ni);
		} catch(RuntimeException e) {
		    try {
			back.add(cs);
		    } catch(RuntimeException e2) {
			Error err = new Error("Unexpected non-local exit", e2);
			err.addSuppressed(e);
			throw(err);
		    }
		    throw(e);
		}
		instreg.put(key, ni);
		ni.register();
	    } else if(cur instanceof InstancedSlot) {
		((InstancedSlot)cur).add(slot);
	    } else {
		throw(new AssertionError());
	    }
	}
    }

    @SuppressWarnings("unchecked")
    public void remove(Slot<? extends Rendered> slot) {
	if(!(slot.obj() instanceof Instancable)) {
	    back.remove(slot);
	    return;
	}
	InstKey key = new InstKey(slot);
	synchronized(this) {
	    Object cur = instreg.get(key);
	    if(cur == null) {
		throw(new IllegalStateException("removing non-present slot"));
	    } else if(cur instanceof Slot) {
		if(cur != slot)
		    throw(new IllegalStateException("removing non-present slot"));
		back.remove(slot);
	    } else if(cur instanceof InstancedSlot) {
		InstancedSlot b = (InstancedSlot)cur;
		b.remove(slot);
		if(b.ni < 1) {
		    b.unregister();
		    b.dispose();
		    if(instreg.remove(key) != b)
			throw(new AssertionError());
		}
	    } else {
		throw(new AssertionError());
	    }
	}
    }

    public void update(Slot<? extends Rendered> slot) {
	if(!(slot.obj() instanceof Instancable)) {
	    back.update(slot);
	    return;
	}
	InstKey key = new InstKey(slot);
	synchronized(this) {
	    Object cur = instreg.get(key);
	    if(cur == null) {
		throw(new IllegalStateException("updating non-present slot"));
	    } else if(cur instanceof Slot) {
		if(cur != slot)
		    throw(new IllegalStateException("updating non-present slot"));
		back.update(slot);
	    } else if(cur instanceof InstancedSlot) {
		((InstancedSlot)cur).update(slot);
	    } else {
		throw(new AssertionError());
	    }
	}
    }

    @SuppressWarnings("unchecked")
    public void update(Pipe group, int[] mask) {
	back.update(group, mask);
	synchronized(this) {
	    Object insts = pipemap.get(group);
	    if(insts instanceof InstancedSlot.Instance) {
		((InstancedSlot.Instance)insts).update(group, mask);
	    } else if(insts instanceof List) {
		for(InstancedSlot.Instance inst : (List<InstancedSlot.Instance>)insts)
		    inst.update(group, mask);
	    }
	}
    }

    public void dispose() {
	/* XXXRENDER */
    }
}
