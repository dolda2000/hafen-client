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

public class RenderTree {
    private final Slot root;

    public RenderTree() {
	root = new Slot(this, null, null);
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

    public static class DepPipe implements Pipe {
	private State[] states = {};
	private boolean[] def = {};
	private boolean[] deps = {};
	private final Pipe parent;
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

	public void lock() {
	    lock = true;
	}

	@SuppressWarnings("unchecked")
	public <T extends State> T get(State.Slot<T> slot) {
	    int idx = slot.id;
	    if(!lock) {
		alloc(idx);
		if(!def[idx])
		    deps[idx] = true;
	    }
	    if((idx < states.length) && def[idx])
		return((T)states[idx]);
	    return((parent == null) ? null : parent.get(slot));
	}

	private void alloc(int idx) {
	    if(states.length < idx) {
		states = Arrays.copyOf(states, idx + 1);
		def = Arrays.copyOf(def, idx + 1);
		deps = Arrays.copyOf(def, idx + 1);
	    }
	}

	public <T extends State> void put(State.Slot<? super T> slot, T state) {
	    if(lock)
		throw(new IllegalStateException("locked"));
	    int idx = slot.id;
	    alloc(idx);
	    def[idx] = true;
	    states[idx] = state;
	}

	public Pipe copy() {
	    return(new BufPipe(states()));
	}

	public State[] states() {
	    State[] ret;
	    if(parent == null) {
		ret = new State[states.length];
	    } else {
		State[] ps = parent.states();
		ret = Arrays.copyOf(ps, Math.max(states.length, ps.length));
	    }
	    for(int i = 0; i < states.length; i++) {
		if(def[i])
		    ret[i] = states[i];
	    }
	    return(ret);
	}
    }

    public static class SlotPipe implements Pipe {
	DepPipe bk = null;

	public <T extends State> T get(State.Slot<T> slot) {
	    int idx = slot.id;
	    if((bk.states.length <= idx) || !bk.def[idx])
		throw(new RuntimeException("Reading undefined slot " + slot + " from slot-pipe"));
	    return(bk.get(slot));
	}

	public Pipe copy() {
	    return(new BufPipe(states()));
	}

	public State[] states() {
	    State[] ret = new State[bk.states.length];
	    for(int i = 0; i < ret.length; i++) {
		if(bk.def[i])
		    ret[i] = bk.states[i];
	    }
	    return(ret);
	}
    }

    public static class Slot {
	public final RenderTree tree;
	public final Slot parent;
	public final Node node;
	private Collection<Slot>[] rdeps = null;
	private Slot[] deps = null;
	private Pipe.Op cstate, ostate;
	private Slot[] children = null;
	private int nchildren = 0;
	private int pidx = -1;

	public Slot(RenderTree tree, Slot parent, Node node) {
	    this.tree = tree;
	    this.parent = parent;
	    this.node = node;
	}

	private void addch(Slot ch) {
	    if(ch.pidx != -1)
		throw(new IllegalStateException());
	    if(children == null)
		children = new Slot[1];
	    else if(children.length <= nchildren + 1) {
		children = Arrays.copyOf(children, children.length * 2);
		int nidx = nchildren++;
		children[nidx] = ch;
		ch.pidx = nidx;
	    }
	}

	private void removech(Slot ch) {
	    int idx = ch.pidx;
	    if(idx < 0)
		throw(new IllegalStateException());
	    if(children[idx] != ch)
		throw(new RuntimeException());
	    (children[idx] = children[--nchildren]).pidx = idx;
	    ch.pidx = -1;
	}

	public Slot add(Node n, Pipe.Op state) {
	    Slot ch = new Slot(tree, this, n);
	    ch.cstate = state;
	    addch(ch);
	    n.added(ch);
	    return(ch);
	}

	public void remove() {
	    parent.removech(this);
	}

	private DepPipe mkdstate(Pipe.Op cstate, Pipe.Op ostate) {
	    DepPipe ret = new DepPipe(parent.istate());
	    if(cstate != null)
		ret.prep(cstate);
	    if(ostate != null)
		ret.prep(ostate);
	    ret.lock();
	    return(ret);
	}

	private void remrdep(int stidx, Slot rdep) {
	    if((rdeps == null) || (rdeps.length <= stidx) ||
	       (rdeps[stidx] == null) || !rdeps[stidx].remove(rdep))
		throw(new RuntimeException("Reverse dependency did strangely not exist"));
	}

	@SuppressWarnings("unchecked")
	private void addrdep(int stidx, Slot rdep) {
	    if(rdeps == null)
		rdeps = (Collection<Slot>[])new Collection[stidx + 1];
	    else if(rdeps.length <= stidx)
		rdeps = Arrays.copyOf(rdeps, stidx + 1);
	    if(rdeps[stidx] == null)
		rdeps[stidx] = new ArrayList<>();
	    rdeps[stidx].add(rdep);
	}

	private void adddep(int stidx, Slot dep) {
	    if(deps == null)
		deps = new Slot[stidx + 1];
	    else if(deps.length <= stidx)
		deps = Arrays.copyOf(deps, stidx + 1);
	    deps[stidx] = dep;
	    dep.addrdep(stidx, this);
	}

	private DepPipe dstate = null;
	private void dstate(DepPipe nst) {
	    if(this.dstate != null) {
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
			for(Slot sp = parent; sp != null; sp = sp.parent) {
			    if((sp.dstate != null) && sp.dstate.def[i]) {
				adddep(i, sp);
				break dep;
			    }
			}
		    }
		}
		this.dstate = nst;
	    }
	}

	private DepPipe dstate() {
	    if(dstate == null)
		dstate(mkdstate(this.cstate, this.ostate));
	    return(dstate);
	}

	private void chstate(Pipe.Op cstate, Pipe.Op ostate) {
	    
	}

	public void cstate(Pipe.Op state) {
	    if(state != this.cstate)
		chstate(state, this.ostate);
	}

	public void ostate(Pipe.Op state) {
	    if(state != this.ostate)
		chstate(this.cstate, state);
	}

	private Inheritance istate = null;
	private Inheritance istate() {
	    if(istate == null) {
		if(parent == null) {
		    istate = new Inheritance(new Pipe[0], new int[0]);
		} else {
		    Inheritance pi = parent.istate();
		    DepPipe ds = dstate();
		    Pipe[] istates = new Pipe[Math.max(pi.gstates.length, ds.def.length)];
		    boolean f = false;
		    for(int i = 0; i < istates.length; i++) {
			if((i < ds.def.length) && ds.def[i]) {
			    istates[i] = ds;
			    f = true;
			} else if(i < pi.gstates.length) {
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

	public GroupPipe state() {
	    return(istate());
	}
    }

    public static interface Node {
	public void added(Slot slot);
	public void removed(Slot slot);
    }

    public Slot add(Node n, Pipe.Op state) {
	return(root.add(n, state));
    }
}
