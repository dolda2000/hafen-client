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
import java.util.concurrent.locks.*;
import haven.Locked;
import static haven.Utils.eq;

public class RenderTree {
    private final Lock lock = new ReentrantLock();
    private final TreeSlot root;
    private final List<Client<?>> clients = new ArrayList<>();

    public RenderTree() {
	root = new TreeSlot(this, null, null);
    }

    public Locked lock() {
	return(new Locked(this.lock));
    }

    private static class Client<R> {
	final Class<R> type;
	final RenderList<R> list;

	Client(Class<R> type, RenderList<R> list) {
	    this.type = type;
	    this.list = list;
	}

	@SuppressWarnings("unchecked")
	void added(TreeSlot slot) {
	    if(type.isInstance(slot.node))
		list.add((RenderList.Slot<R>)slot);
	}

	@SuppressWarnings("unchecked")
	void removed(TreeSlot slot) {
	    if(type.isInstance(slot.node))
		list.remove((RenderList.Slot<R>)slot);
	}

	@SuppressWarnings("unchecked")
	void updated(TreeSlot slot) {
	    if(type.isInstance(slot.node))
		list.update((RenderList.Slot<R>)slot);
	}

	void updated(Pipe group, int[] mask) {
	    list.update(group, mask);
	}
    }

    public static class Inheritance implements GroupPipe {
	private final Pipe[] groups;
	private final int[] gstates;

	public Inheritance(Pipe[] groups, int[] gstates) {
	    this.groups = groups;
	    this.gstates = gstates;
	}

	public Pipe[] groups() {return(groups);}
	public int[] gstates() {return(gstates);}
    }

    public static class DepInfo {
	public State[] states = {};
	public boolean[] def = {};
	public boolean[] deps = {};
	public int ndef = 0;

	private void alloc(int idx) {
	    if(states.length <= idx) {
		states = Arrays.copyOf(states, idx + 1);
		def = Arrays.copyOf(def, idx + 1);
		deps = Arrays.copyOf(deps, idx + 1);
	    }
	}

	public int[] defdiff(DepInfo that) {
	    if(that.def.length < this.def.length)
		return(that.defdiff(this));
	    int nch = 0;
	    for(int i = 0; i < this.def.length; i++) {
		if(this.def[i] != that.def[i])
		    nch++;
	    }
	    for(int i = this.def.length; i < that.def.length; i++) {
		if(that.def[i])
		    nch++;
	    }
	    if(nch == 0)
		return(null);
	    int[] ch = new int[nch];
	    nch = 0;
	    for(int i = 0; i < this.def.length; i++) {
		if(this.def[i] != that.def[i])
		    ch[nch++] = i;
	    }
	    for(int i = this.def.length; i < that.def.length; i++) {
		if(that.def[i])
		    ch[nch++] = i;
	    }
	    return(ch);
	}

	/*
	public boolean defequal(DepInfo that) {
	    if(that.def.length < this.def.length)
		return(that.defequal(this));
	    if(this.ndef != that.ndef)
		return(false);
	    int i;
	    for(i = 0; i < this.def.length; i++) {
		if(this.def[i] != that.def[i])
		    return(false);
	    }
	    for(; i < that.def.length; i++) {
		if(that.def[i])
		    return(false);
	    }
	    return(true);
	}
	*/
    }

    public static class DepPipe implements Pipe {
	public final DepInfo data = new DepInfo();
	public final Pipe parent;
	private boolean lock = false;

	public DepPipe(Pipe parent) {
	    this.parent = parent;
	}

	public DepPipe(Pipe parent, Pipe.Op st) {
	    this(parent);
	    prep(st);
	    lock = true;
	}

	public DepPipe prep(Pipe.Op op) {
	    op.apply(this);
	    return(this);
	}

	public DepInfo lock() {
	    lock = true;
	    return(data);
	}

	@SuppressWarnings("unchecked")
	public <T extends State> T get(State.Slot<T> slot) {
	    int idx = slot.id;
	    if(!lock) {
		data.alloc(idx);
		if(!data.def[idx])
		    data.deps[idx] = true;
	    }
	    if((idx < data.states.length) && data.def[idx])
		return((T)data.states[idx]);
	    return((parent == null) ? null : parent.get(slot));
	}

	public <T extends State> void put(State.Slot<? super T> slot, T state) {
	    if(lock)
		throw(new IllegalStateException("locked"));
	    int idx = slot.id;
	    data.alloc(idx);
	    if(!data.def[idx]) {
		data.def[idx] = true;
		data.ndef++;
	    }
	    data.states[idx] = state;
	}

	public Pipe copy() {
	    return(new BufPipe(states()));
	}

	public State[] states() {
	    State[] ret;
	    if(parent == null) {
		ret = new State[data.states.length];
	    } else {
		State[] ps = parent.states();
		ret = Arrays.copyOf(ps, Math.max(data.states.length, ps.length));
	    }
	    for(int i = 0; i < data.states.length; i++) {
		if(data.def[i])
		    ret[i] = data.states[i];
	    }
	    return(ret);
	}
    }

    public static interface Slot extends RenderList.Slot<Node> {
	public Slot add(Node n, Pipe.Op state);
	public default Slot add(Node n) {return(add(n, null));}
	public void remove();
	public void cstate(Pipe.Op state);
	public void ostate(Pipe.Op state);
    }

    static class TreeSlot implements Slot {
	final RenderTree tree;
	final TreeSlot parent;
	final Node node;
	private DepInfo dstate = null;
	private Collection<TreeSlot>[] rdeps = null;
	private TreeSlot[] deps = null;
	private Pipe.Op cstate, ostate;
	private TreeSlot[] children = null;
	private int nchildren = 0;
	private int pidx = -1;

	private TreeSlot(RenderTree tree, TreeSlot parent, Node node) {
	    this.tree = tree;
	    this.parent = parent;
	    this.node = node;
	}

	private void addch(TreeSlot ch) {
	    if(ch.pidx != -1)
		throw(new IllegalStateException());
	    if(children == null)
		children = new TreeSlot[1];
	    else if(children.length <= nchildren + 1)
		children = Arrays.copyOf(children, children.length * 2);
	    int nidx = nchildren++;
	    children[nidx] = ch;
	    ch.pidx = nidx;
	}

	private void removech(TreeSlot ch) {
	    int idx = ch.pidx;
	    if(idx < 0)
		throw(new IllegalStateException());
	    if(children[idx] != ch)
		throw(new RuntimeException());
	    (children[idx] = children[--nchildren]).pidx = idx;
	    ch.pidx = -1;
	}

	public Iterable<TreeSlot> children() {
	    return(() -> new Iterator<TreeSlot>() {
		    int i = 0;
		    public boolean hasNext() {return(i < nchildren);}
		    public TreeSlot next() {return(children[i++]);}
		});
	}

	public TreeSlot add(Node n, Pipe.Op state) {
	    try(Locked lk = tree.lock()) {
		TreeSlot ch = new TreeSlot(tree, this, n);
		ch.cstate = state;
		addch(ch);
		synchronized(tree.clients) {
		    ListIterator<Client<?>> it = tree.clients.listIterator();
		    try {
			while(it.hasNext()) {
			    Client<?> cl = it.next();
			    cl.added(ch);
			}
		    } catch(RuntimeException e) {
			try {
			    removech(ch);
			    it.previous();
			    while(it.hasPrevious()) {
				Client<?> cl = it.previous();
				cl.removed(ch);
			    }
			} catch(RuntimeException e2) {
			    Error err = new Error("Unexpected non-local exit", e2);
			    err.addSuppressed(e);
			    throw(err);
			}
			throw(e);
		    }
		}
		if(n != null) {
		    try {
			n.added(ch);
		    } catch(RuntimeException e) {
			ch.remove();
			throw(e);
		    }
		}
		return(ch);
	    }
	}

	public void remove() {
	    try(Locked lk = tree.lock()) {
		while(nchildren > 0)
		    children[nchildren - 1].remove();
		parent.removech(this);
		try {
		    if(node != null)
			node.removed(this);
		    synchronized(tree.clients) {
			tree.clients.forEach(cl -> cl.removed(this));
		    }
		} catch(RuntimeException e) {
		    throw(new Error("Unexpected non-local exit", e));
		}
	    }
	}

	private DepInfo mkdstate(Pipe.Op cstate, Pipe.Op ostate) {
	    DepPipe buf = new DepPipe(parent.istate());
	    if(cstate != null)
		buf.prep(cstate);
	    if(ostate != null)
		buf.prep(ostate);
	    return(buf.lock());
	}

	private void remrdep(int stidx, TreeSlot rdep) {
	    if((rdeps == null) || (rdeps.length <= stidx) ||
	       (rdeps[stidx] == null) || !rdeps[stidx].remove(rdep))
		throw(new RuntimeException("Reverse dependency did strangely not exist"));
	}

	@SuppressWarnings("unchecked")
	private void addrdep(int stidx, TreeSlot rdep) {
	    if(rdeps == null)
		rdeps = (Collection<TreeSlot>[])new Collection[stidx + 1];
	    else if(rdeps.length <= stidx)
		rdeps = Arrays.copyOf(rdeps, stidx + 1);
	    if(rdeps[stidx] == null)
		rdeps[stidx] = new ArrayList<>();
	    rdeps[stidx].add(rdep);
	}

	private void adddep(int stidx, TreeSlot dep) {
	    if(deps == null)
		deps = new TreeSlot[stidx + 1];
	    else if(deps.length <= stidx)
		deps = Arrays.copyOf(deps, stidx + 1);
	    deps[stidx] = dep;
	    dep.addrdep(stidx, this);
	}

	private void rdepupd() {
	    upddstate(mkdstate(cstate, ostate));
	}

	private DepInfo setdstate(DepInfo nst) {
	    DepInfo pst = this.dstate;
	    if(pst != null) {
		if(deps != null) {
		    for(int i = 0; i < deps.length; i++) {
			if(deps[i] != null)
			    deps[i].remrdep(i, this);
		    }
		    deps = null;
		}
		this.dstate = null;
	    }
	    if(nst != null) {
		for(int i = nst.states.length - 1; i >= 0; i--) {
		    dep: if(nst.deps[i]) {
			for(TreeSlot sp = parent; sp != null; sp = sp.parent) {
			    if((sp.dstate != null) && (sp.dstate.def.length > i) && sp.dstate.def[i]) {
				adddep(i, sp);
				break dep;
			    }
			}
		    }
		}
		this.dstate = nst;
	    }
	    return(pst);
	}

	private void upddstate(DepInfo nst) {
	    DepInfo pst = setdstate(nst);
	    int[] defch = nst.defdiff(pst);
	    if(defch == null) {
		int[] ch = new int[pst.ndef];
		int maxi = Math.min(nst.states.length, pst.states.length), nch = 0;
		ArrayList<TreeSlot> cdeps = new ArrayList<TreeSlot>();
		for(int i = 0; i < maxi; i++) {
		    if(pst.def[i] && !eq(pst.states[i], nst.states[i])) {
			ch[nch++] = i;
			if((rdeps != null) && (rdeps[i] != null)) {
			    for(TreeSlot rdep : rdeps[i]) {
				if(!cdeps.contains(rdep))
				    cdeps.add(rdep);
			    }
			}
		    }
		}
		int[] tch = Arrays.copyOf(ch, nch);
		for(TreeSlot rdep : cdeps)
		    rdep.rdepupd();
		Pipe pdst = this.pdstate;
		if(pdst != null) {
		    synchronized(tree.clients) {
			tree.clients.forEach(cl -> cl.updated(pdst, tch));
		    }
		}
	    } else {
		/* XXX? Optimize specifically for non-defined slots being updated? */
		updtotal(false);
	    }
	}

	private void updtotal(boolean setds) {
	    this.pdstate = null;
	    this.istate = null;
	    if(setds)
		setdstate(mkdstate(cstate, ostate));
	    for(TreeSlot child : children())
		child.updtotal(true);
	    synchronized(tree.clients) {
		tree.clients.forEach(cl -> cl.updated(this));
	    }
	}

	private DepInfo dstate() {
	    if(dstate == null)
		setdstate(mkdstate(this.cstate, this.ostate));
	    return(dstate);
	}

	private void chstate(Pipe.Op cstate, Pipe.Op ostate) {
	    if(this.dstate != null) {
		DepInfo pst = this.dstate;
		try {
		    upddstate(mkdstate(cstate, ostate));
		} catch(RuntimeException e) {
		    try {
			upddstate(pst);
		    } catch(RuntimeException e2) {
			Error err = new Error("Unexpected non-local exit", e2);
			err.addSuppressed(e);
			throw(err);
		    }
		    throw(e);
		}
	    }
	    this.cstate = cstate;
	    this.ostate = ostate;
	}

	public void cstate(Pipe.Op state) {
	    try(Locked lk = tree.lock()) {
		if(state != this.cstate)
		    chstate(state, this.ostate);
	    }
	}

	public void ostate(Pipe.Op state) {
	    try(Locked lk = tree.lock()) {
		if(state != this.ostate)
		    chstate(this.cstate, state);
	    }
	}

	public class SlotPipe implements Pipe {
	    @SuppressWarnings("unchecked")
	    public <T extends State> T get(State.Slot<T> slot) {
		DepInfo bk = dstate();
		int idx = slot.id;
		if((bk.states.length <= idx) || !bk.def[idx])
		    throw(new RuntimeException("Reading undefined slot " + slot + " from slot-pipe"));
		return((T)bk.states[idx]);
	    }

	    public Pipe copy() {
		return(new BufPipe(states()));
	    }

	    public State[] states() {
		throw(new UnsupportedOperationException("SlotPipe::states"));
	    }
	}

	private Pipe pdstate = null;
	private Pipe pdstate() {
	    if(this.pdstate == null)
		this.pdstate = new SlotPipe();
	    return(this.pdstate);
	}

	private Inheritance istate = null;
	private Inheritance istate() {
	    if(istate == null) {
		if(parent == null) {
		    istate = new Inheritance(new Pipe[0], new int[0]);
		} else {
		    Inheritance pi = parent.istate();
		    DepInfo ds = dstate();
		    Pipe[] istates = new Pipe[Math.max(pi.gstates.length, ds.def.length)];
		    boolean f = false;
		    for(int i = 0; i < istates.length; i++) {
			if((i < ds.def.length) && ds.def[i]) {
			    istates[i] = pdstate();
			    f = true;
			} else if(i < pi.gstates.length) {
			    if(pi.gstates[i] >= 0)
				istates[i] = pi.groups[pi.gstates[i]];
			}
		    }
		    if(f) {
			Pipe[] groups = new Pipe[istates.length];
			int[] gstates = new int[istates.length];
			int n = 0;
			idx: for(int i = 0; i < istates.length; i++) {
			    Pipe p = istates[i];
			    if(p == null) {
				gstates[i] = -1;
			    } else {
				for(int o = 0; o < n; o++) {
				    if(groups[o] == p) {
					gstates[i] = o;
					continue idx;
				    }
				}
				groups[gstates[i] = n++] = p;
			    }
			}
			istate = new Inheritance(Arrays.copyOf(groups, n), gstates);
		    } else {
			istate = pi;
		    }
		}
	    }
	    return(istate);
	}

	public Node obj() {
	    return(node);
	}

	public GroupPipe state() {
	    return(istate());
	}
    }

    public static interface Node {
	public default void added(Slot slot) {}
	public default void removed(Slot slot) {}

	public static class Track1 implements Node {
	    protected Slot slot = null;

	    public void added(RenderTree.Slot slot) {
		synchronized(this) {
		    if(this.slot != null)
			throw(new RuntimeException());
		    this.slot = slot;
		}
	    }

	    public void removed(RenderTree.Slot slot) {
		synchronized(this) {
		    if(this.slot != slot)
			throw(new RuntimeException());
		    this.slot = null;
		}
	    }
	}
    }

    public Iterable<Slot> slots() {
	return(new Iterable<Slot>() {
		public Iterator<Slot> iterator() {
		    return(new Iterator<Slot>() {
			    int[] cs = {0, 0, 0, 0, 0, 0, 0, 0};
			    TreeSlot[] ss = {root, null, null, null, null, null, null, null};
			    int sp = 0;
			    TreeSlot next = root;

			    public boolean hasNext() {
				if(next != null)
				    return(true);
				if(sp < 0)
				    return(false);
				while(cs[sp] >= ss[sp].nchildren) {
				    if(--sp < 0)
					return(false);
				}
				next = ss[sp].children[cs[sp]++];
				if(++sp >= ss.length) {
				    ss = Arrays.copyOf(ss, ss.length * 2);
				    cs = Arrays.copyOf(cs, cs.length * 2);
				}
				cs[sp] = 0;
				ss[sp] = next;
				return(true);
			    }

			    public Slot next() {
				if(!hasNext())
				    throw(new NoSuchElementException());
				Slot ret = next;
				next = null;
				return(ret);
			    }
			});
		}
	    });
    }

    public Slot add(Node n, Pipe.Op state) {
	return(root.add(n, state));
    }
    public Slot add(Node n) {
	return(root.add(n));
    }

    public <R> void add(RenderList<R> list, Class<R> type) {
	synchronized(clients) {
	    clients.add(new Client<R>(type, list));
	}
    }

    public void remove(RenderList list) {
	synchronized(clients) {
	    clients.removeIf(cl -> cl.list == list);
	}
    }

    private void dump(TreeSlot slot, int ind) {
	for(int i = 0; i < ind; i++)
	    System.err.print("    ");
	System.err.printf("%s(%d)\n", slot, slot.nchildren);
	for(int i = 0; i < slot.nchildren; i++)
	    dump(slot.children[i], ind + 1);
    }

    public void dump() {dump(root, 0);}
}
