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
import haven.render.State.Instancer;

public class InstanceList implements RenderList<Rendered>, RenderList.Adapter, Disposable {
    private final List<RenderList<Rendered>> clients = new ArrayList<>();
    private final Adapter master;
    private final Map<InstKey, Object> instreg = new HashMap<>();
    private final Map<Slot<? extends Rendered>, InstKey> uslotmap = new IdentityHashMap<>();
    private final Map<Slot<? extends Rendered>, InstancedSlot.Instance> islotmap = new IdentityHashMap<>();
    private final Map<Pipe, Object> pipemap = new IdentityHashMap<>();
    private final Set<InstancedSlot> dirty = new HashSet<>();
    private int nbypass, ninvalid, nuinst, nbatches, ninst;

    private static int[][][] _stcounts = {};
    private static int[][] stcounts(int n) {
	int[][][] c = _stcounts;
	if((c != null) && (c.length > n) && (c[n] != null))
	    return(c[n]);
	synchronized(InstanceList.class) {
	    if((c == null) || (c.length <= n))
		_stcounts = c = Arrays.copyOf(_stcounts, n + 1);
	    int[][] ret = new int[2][n];
	    State.Slot.Slots si = State.Slot.slots;
	    int un = 0, in = 0;
	    for(int i = 0; i < n; i++) {
		if(si.idlist[i].instanced == null)
		    ret[0][un++] = i;
		else
		    ret[1][in++] = i;
	    }
	    ret[0] = Arrays.copyOf(ret[0], un);
	    ret[1] = Arrays.copyOf(ret[1], in);
	    return(c[n] = ret);
	}
    }

    private static Pipe[] uinststate(GroupPipe st, int ls) {
	int[] us = stcounts(ls + 1)[0];
	Pipe[] ret = new Pipe[us.length];
	for(int i = 0; i < ret.length; i++) {
	    int gn = st.gstate(us[i]);
	    if(gn >= 0)
		ret[i] = st.group(gn);
	}
	return(ret);
    }

    private static <T extends State> Instancer<T> instid0(Pipe buf, State.Slot<T> slot) {
	return(slot.instanced.instid(buf.get(slot)));
    }

    private static Instancer[] instids(GroupPipe st, int ls) {
	int[] is = stcounts(ls + 1)[1];
	Instancer[] ret = new Instancer[is.length];
	for(int i = 0; i < ret.length; i++) {
	    int gn = st.gstate(is[i]);
	    State.Slot<?> slot = State.Slot.byid(is[i]);
	    if(gn >= 0)
		ret[i] = instid0(st.group(gn), slot);
	    else
		ret[i] = slot.instanced.instid(null);
	}
	return(ret);
    }

    private static class InstKey {
	final Object instid;
	final Pipe[] ust;
	/* It may be argued that instids should be compared by equals
	 * rather than by identity, if need be. */
	final Instancer[] instids;
	final int[] instidmap;

	InstKey(Slot<? extends Rendered> slot) {
	    this.instid = ((Instancable)slot.obj()).instanceid();
	    GroupPipe st = slot.state();
	    int ls;
	    for(ls = st.nstates() - 1; (ls >= 0) && (st.gstate(ls) < 0); ls--);
	    if(ls < 0) {
		this.ust = new Pipe[0];
		this.instids = new Instancer[0];
		this.instidmap = new int[0];
	    } else {
		this.ust = uinststate(st, ls);
		this.instids = instids(st, ls);
		this.instidmap = stcounts(ls + 1)[1];
	    }
	}

	boolean valid() {
	    if(instid == null)
		return(false);
	    for(int i = 0; i < instids.length; i++) {
		if(instids[i] == null)
		    return(false);
	    }
	    return(true);
	}

	public int hashCode() {
	    int ret = System.identityHashCode(instid);
	    for(int i = 0; i < ust.length; i++)
		ret = (ret * 31) + System.identityHashCode(ust[i]);
	    for(int i = 0; i < instids.length; i++)
		ret = (ret * 31) + System.identityHashCode(instids[i]);
	    return(ret);
	}

	private boolean equals(InstKey that) {
	    if(this.instid != that.instid)
		return(false);
	    if(this.ust.length != that.ust.length)
		return(false);
	    if(this.instids.length != that.instids.length)
		return(false);
	    for(int i = 0; i < ust.length; i++) {
		if(this.ust[i] != that.ust[i])
		    return(false);
	    }
	    for(int i = 0; i < instids.length; i++) {
		if(this.instids[i] != that.instids[i])
		    return(false);
	    }
	    return(true);
	}

	public boolean equals(Object x) {
	    return((x instanceof InstKey) ? equals((InstKey)x) : false);
	}

	public String toString() {
	    StringBuilder buf = new StringBuilder();
	    buf.append("#<instkey ");
	    buf.append(instid);
	    for(int i = 0; i < ust.length; i++)
		buf.append(String.format(" %x", System.identityHashCode(ust[i])));
	    buf.append(">");
	    return(buf.toString());
	}
    }

    private static class InstanceState extends BufPipe {
	final int[] mask;

	private <T extends State> void inststate0(State.Slot<T> slot, GroupPipe from, InstancedSlot batch) {
	    this.put(slot, slot.instanced.instid(from.get(slot)).inststate(from.get(slot), batch));
	}

	InstanceState(GroupPipe from, InstancedSlot batch) {
	    int ns = 0, fn;
	    for(fn = from.nstates() - 1; (fn >= 0) && (from.gstate(fn) < 0); fn--);
	    fn++;
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

	public String toString() {
	    StringBuilder buf = new StringBuilder();
	    buf.append("[");
	    for(int i = 0; i < mask.length; i++) {
		int id = mask[i];
		if(i > 0)
		    buf.append(", ");
		buf.append(String.format("%d(%s)=%s", id, State.Slot.byid(id).scl.getSimpleName(), get(State.Slot.byid(id))));
	    }
	    buf.append("]");
	    return(buf.toString());
	}
    }

    private class InstancedSlot implements RenderList.Slot<Rendered>, InstanceBatch {
	final InstKey key;
	final Instanced rend;
	final InstanceState ist;
	final GroupPipe ust;
	Instance[] insts;
	int ni;
	boolean backdirty, selfdirty;

	class Instance {
	    final Slot<? extends Rendered> slot;
	    final Pipe[] rpipes;
	    int idx;

	    Instance(Slot<? extends Rendered> slot) {
		this.slot = slot;
		this.rpipes = new Pipe[ist.mask.length];
	    }

	    @SuppressWarnings("unchecked")
	    void register() {
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
			pipemap.put(p, nl);
		    } else if(cur instanceof List) {
			List<Instance> ls = (List<Instance>)cur;
			ls.add(this);
		    } else {
			throw(new AssertionError());
		    }
		    rpipes[i] = p;
		}
	    }

	    @SuppressWarnings("unchecked")
	    void unregister() {
		for(int i = 0; i < ist.mask.length; i++) {
		    Pipe p = rpipes[i];
		    if(p == null)
			continue;
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
		for(int i = 0; i < key.instids.length; i++) {
		    for(int o = 0; o < mask.length; o++) {
			if(mask[o] == key.instidmap[i]) {
			    if(instid0(group, State.Slot.byid(key.instidmap[i])) != key.instids[i]) {
				InstanceList.this.remove(slot);
				InstanceList.this.add(slot);
				return;
			    }
			}
		    }
		}
		/* XXX: There should be a way to only update the
		 * relevant states, by mask. */
		iupdate(idx);
	    }
	}

	InstancedSlot(InstKey key, Slot<? extends Rendered>[] slots) {
	    this.key = key;
	    this.ust = slots[0].state();
	    this.ist = new InstanceState(ust, this);
	    this.rend = ((Instancable)slots[0].obj()).instancify(this);
	    Instance[] insts = new Instance[slots.length];
	    for(int i = 0; i < slots.length; i++) {
		insts[i] = new Instance(slots[i]);
		insts[i].idx = i;
		if(i > 0) {
		    InstanceState test = new InstanceState(slots[i].state(), this);
		    if(InstanceState.compare(this.ist, test) != 0)
			throw(new RuntimeException(String.format("instantiation-IDs not yet implemented (states=%s vs %s)", this.ist, test)));
		}
	    }
	    this.insts = insts;
	    this.ni = slots.length;
	}

	void register() {
	    for(int i = 0; i < ni; i++) {
		insts[i].register();
		if(islotmap.put(insts[i].slot, insts[i]) != null)
		    throw(new AssertionError());
		iupdate(i);
	    }
	}

	void unregister() {
	    for(int i = 0; i < ni; i++) {
		insts[i].unregister();
		if(islotmap.remove(insts[i].slot) != insts[i])
		    throw(new AssertionError());
	    }
	}

	private void iupdate(int idx) {
	    rend.iupdate(idx);
	    for(int i = 0; i < ist.mask.length; i++) {
		State st = ist.get(State.Slot.byid(ist.mask[i]));
		if(st instanceof InstanceBatch.Client)
		    ((InstanceBatch.Client)st).iupdate(idx);
	    }
	    selfdirty = true;
	    dirty.add(this);
	}

	private void itrim(int idx) {
	    rend.itrim(idx);
	    for(int i = 0; i < ist.mask.length; i++) {
		State st = ist.get(State.Slot.byid(ist.mask[i]));
		if(st instanceof InstanceBatch.Client)
		    ((InstanceBatch.Client)st).itrim(idx);
	    }
	    selfdirty = true;
	    dirty.add(this);
	}

	Instance add(Slot<? extends Rendered> ns, InstancedSlot replace) {
	    Instance inst = new Instance(ns);
	    inst.register();
	    Instance prev = islotmap.put(ns, inst);
	    if(replace == null) {
		if(prev != null)
		    throw(new AssertionError());
	    } else {
		if((prev == null) || (prev.slot != ns))
		    throw(new AssertionError());
	    }
	    if(insts.length == ni)
		insts = Arrays.copyOf(insts, insts.length * 2);
	    insts[inst.idx = ni++] = inst;
	    iupdate(inst.idx);
	    return(inst);
	}

	Instance remove(Instance inst) {
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
	    return(inst);
	}

	void dispose() {
	    rend.dispose();
	}

	void update(Slot<? extends Rendered> ns) {
	    /* XXX? Is this really necessary? Can't I just iupdate
	     * this? Also, if it is necessary, doesn't add() run the
	     * risk of throwing exceptions after remove()? */
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
	    backdirty = true;
	    dirty.add(this);
	}

	public <T extends State> void update(State.Slot<? super T> slot, T state) {
	    ist.put(slot, state);
	    backdirty = true;
	    dirty.add(this);
	}

	private void commit(Render g) {
	    if(backdirty) {
		clupdate(this);
		backdirty = false;
	    }
	    if(selfdirty) {
		rend.commit(g);
		selfdirty = false;
	    }
	}
    }

    private void cladd(Slot<? extends Rendered> slot) {
	synchronized(clients) {
	    clients.forEach(cl -> cl.add(slot));
	}
    }

    private void clremove(Slot<? extends Rendered> slot) {
	synchronized(clients) {
	    clients.forEach(cl -> cl.remove(slot));
	}
    }

    private void clupdate(Slot<? extends Rendered> slot) {
	synchronized(clients) {
	    clients.forEach(cl -> cl.update(slot));
	}
    }

    private void clupdate(Pipe group, int[] mask) {
	synchronized(clients) {
	    clients.forEach(cl -> cl.update(group, mask));
	}
    }

    public InstanceList(Adapter master) {
	this.master = master;
    }

    @SuppressWarnings("unchecked")
    private void add0(Slot<? extends Rendered> slot, InstKey key, boolean prevsole, InstancedSlot previnst) {
	Object cur = instreg.get(key);
	if(cur == null) {
	    if(prevsole)
		clupdate(slot);
	    else
		cladd(slot);
	    if(previnst != null)
		remove0(previnst, islotmap.get(slot), true);
	    instreg.put(key, slot);
	    uslotmap.put(slot, key);
	    nuinst++;
	} else if(cur instanceof InstancedSlot) {
	    InstancedSlot curbat = (InstancedSlot)cur;
	    InstancedSlot.Instance prev = null;
	    if(previnst != null)
		prev = islotmap.get(slot);
	    curbat.add(slot, previnst);
	    if(prevsole)
		clremove(slot);
	    if(previnst != null)
		remove0(previnst, prev, false);
	    uslotmap.put(slot, curbat.key);
	    ninst++;
	} else if(cur instanceof Slot) {
	    Slot<? extends Rendered> cs = (Slot<? extends Rendered>)cur;
	    InstKey curkey = uslotmap.get(cs);
	    if(!curkey.equals(key))
		throw(new AssertionError());
	    InstancedSlot ni = new InstancedSlot(curkey, new Slot[] {cs, slot});
	    try {
		cladd(ni);
	    } catch(RuntimeException e) {
		ni.dispose();
		throw(e);
	    }
	    clremove(cs);
	    if(prevsole)
		clremove(slot);
	    if(previnst != null)
		remove0(previnst, islotmap.get(slot), true);
	    instreg.put(curkey, ni);
	    uslotmap.put(slot, curkey);
	    ni.register();
	    nuinst--; nbatches++; ninst += 2;
	} else {
	    throw(new AssertionError());
	}
    }

    @SuppressWarnings("unchecked")
    public void add(Slot<? extends Rendered> slot) {
	if(!(slot.obj() instanceof Instancable)) {
	    cladd(slot);
	    nbypass++;
	    return;
	}
	InstKey key = new InstKey(slot);
	if(!key.valid()) {
	    /* XXX: Slots excluded in this manner aren't registered
	     * for in-pipe updates which might bring them back into
	     * instantiation. I don't really foresee that happening,
	     * but it is also perhaps not so purely theoretical as to
	     * be of purely academic interest. It shouldn't
	     * technically break anything, however; it should just
	     * mean that they can't be re-instantiated if their instid
	     * only changes in-pipe. */
	    cladd(slot);
	    ninvalid++;
	    return;
	}
	synchronized(this) {
	    add0(slot, key, false, null);
	}
    }

    private void remove0(InstancedSlot b, InstancedSlot.Instance inst, boolean unreg) {
	b.remove(inst);
	if(b.ni < 1) {
	    dirty.remove(b);
	    clremove(b);
	    b.unregister();
	    b.dispose();
	    if(instreg.remove(b.key) != b)
		throw(new AssertionError());
	    nbatches--;
	}
	ninst--;
	if(unreg && (islotmap.remove(inst.slot) != inst))
	    throw(new AssertionError());
    }

    @SuppressWarnings("unchecked")
    public void remove(Slot<? extends Rendered> slot) {
	if(!(slot.obj() instanceof Instancable)) {
	    clremove(slot);
	    nbypass--;
	    return;
	}
	synchronized(this) {
	    InstKey key = uslotmap.get(slot);
	    if(key == null) {
		/* throw(new IllegalStateException("removing non-present slot")); */
		/* XXX: Register a marker object in uslotmap and bring
		 * back the above assertion? */
		ninvalid--;
		clremove(slot);
		if(new InstKey(slot).valid())
		    Warning.warn("removing non-present slot with valid inst-key");
		return;
	    }
	    Object cur = instreg.get(key);
	    if(cur == null) {
		throw(new IllegalStateException("removing non-present slot"));
	    } else if(cur instanceof InstancedSlot) {
		InstancedSlot b = (InstancedSlot)cur;
		remove0((InstancedSlot)cur, islotmap.get(slot), true);
	    } else if(cur instanceof Slot) {
		if(cur != slot)
		    throw(new IllegalStateException("removing non-present slot"));
		clremove(slot);
		instreg.remove(key);
		nuinst--;
	    } else {
		throw(new AssertionError());
	    }
	    uslotmap.remove(slot);
	}
    }

    public void update(Slot<? extends Rendered> slot) {
	if(!(slot.obj() instanceof Instancable)) {
	    clupdate(slot);
	    return;
	}
	InstKey key = new InstKey(slot);
	synchronized(this) {
	    InstKey prevkey = uslotmap.get(slot);
	    if(prevkey == null) {
		/* throw(new IllegalStateException("updating non-present slot")); */
		if(key.valid()) {
		    add0(slot, key, true, null);
		    ninvalid--;
		} else {
		    clupdate(slot);
		}
		return;
	    }
	    Object prev = instreg.get(prevkey);
	    if(prev == null) {
		throw(new IllegalStateException("updating non-present slot"));
	    } else if(prev instanceof InstancedSlot) {
		InstancedSlot b = (InstancedSlot)prev;
		if(key.equals(prevkey)) {
		    b.update(slot);
		} else if(!key.valid()) {
		    cladd(slot);
		    remove0(b, islotmap.get(slot), true);
		    uslotmap.remove(slot);
		    ninvalid++;
		} else {
		    add0(slot, key, false, b);
		}
	    } else if(prev instanceof Slot) {
		if(prev != slot)
		    throw(new IllegalStateException("updating non-present slot"));
		if(key.equals(prevkey)) {
		    clupdate(slot);
		} else if(!key.valid()) {
		    clupdate(slot);
		    instreg.remove(prevkey);
		    uslotmap.remove(slot);
		    nuinst--;
		    ninvalid++;
		} else {
		    add0(slot, key, true, null);
		    instreg.remove(prevkey);
		    nuinst--;
		}
	    } else {
		throw(new AssertionError());
	    }
	}
    }

    @SuppressWarnings("unchecked")
    public void update(Pipe group, int[] mask) {
	clupdate(group, mask);
	synchronized(this) {
	    Object insts = pipemap.get(group);
	    if(insts instanceof InstancedSlot.Instance) {
		((InstancedSlot.Instance)insts).update(group, mask);
	    } else if(insts instanceof List) {
		for(InstancedSlot.Instance inst : new ArrayList<>((List<InstancedSlot.Instance>)insts))
		    inst.update(group, mask);
	    }
	}
    }

    public void commit(Render g) {
	synchronized(this) {
	    for(Iterator<InstancedSlot> i = dirty.iterator(); i.hasNext();) {
		InstancedSlot slot = i.next();
		slot.commit(g);
		i.remove();
	    }
	}
    }

    public Locked lock() {
	return(master.lock());
    }

    public Iterable<Slot<?>> slots() {
	return(new Iterable<Slot<?>>() {
		public Iterator<Slot<?>> iterator() {
		    Collection<Slot<?>> ret = new ArrayList<>();
		    for(Object slot : instreg.values()) {
			ret.add((Slot<?>)slot);
		    }
		    for(Slot<?> slot : master.slots()) {
			if(!uslotmap.containsKey(slot))
			    ret.add(slot);
		    }
		    return(ret.iterator());
		}
	    });
    }

    @SuppressWarnings("unchecked")
    public <R> void add(RenderList<R> list, Class<? extends R> type) {
	if(type != Rendered.class)
	    throw(new IllegalArgumentException("instance-list can only reasonably handle rendering clients"));
	if(list == null)
	    throw(new NullPointerException());
	synchronized(clients) {
	    clients.add((RenderList<Rendered>)list);
	}
    }

    public void remove(RenderList<?> list) {
	synchronized(clients) {
	    clients.remove(list);
	}
    }

    public void dispose() {
	/* XXXRENDER */
    }

    public String stats() {
	return(String.format("%,d+%,d(%,d) %d %d", nuinst, nbatches, ninst, ninvalid, nbypass));
    }
}
