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
import javax.media.opengl.*;
import static haven.GOut.checkerr;

public abstract class GLState {
    public abstract void apply(GOut g);
    public abstract void unapply(GOut g);
    public abstract void prep(Buffer buf);
    
    public void applyfrom(GOut g, GLState from) {
	throw(new RuntimeException("Called applyfrom on non-conformant GLState"));
    }
    public void applyto(GOut g, GLState to) {
    }

    public int capply() {
	return(10);
    }
    public int cunapply() {
	return(1);
    }
    public int capplyfrom(GLState from) {
	return(-1);
    }
    public int capplyto(GLState to) {
	return(0);
    }
    
    private static int slotnum = 0;
    private static Slot<?>[] deplist = new Slot<?>[0];
    private static Slot<?>[] idlist = new Slot<?>[0];
    
    public static class Slot<T extends GLState> {
	private static boolean dirty = false;
	private static Collection<Slot<?>> all = new LinkedList<Slot<?>>();
	public final int id;
	public final Class<T> scl;
	private int depid = -1;
	private final Slot<?>[] dep, rdep;
	private Slot[] grdep;
	
	public Slot(Class<T> scl, Slot<?>[] dep, Slot<?>[] rdep) {
	    this.scl = scl;
	    synchronized(Slot.class) {
		this.id = slotnum++;
		dirty = true;
		Slot<?>[] nlist = new Slot<?>[slotnum];
		System.arraycopy(idlist, 0, nlist, 0, idlist.length);
		nlist[this.id] = this;
		idlist = nlist;
		all.add(this);
	    }
	    if(dep == null)
		this.dep = new Slot<?>[0];
	    else
		this.dep = dep;
	    if(rdep == null)
		this.rdep = new Slot<?>[0];
	    else
		this.rdep = rdep;
	    for(Slot<?> ds : this.dep) {
		if(ds == null)
		    throw(new NullPointerException());
	    }
	    for(Slot<?> ds : this.rdep) {
		if(ds == null)
		    throw(new NullPointerException());
	    }
	}
	
	public Slot(Class<T> scl, Slot... dep) {
	    this(scl, dep, null);
	}
	
	private static void makedeps(Collection<Slot<?>> slots) {
	    Map<Slot<?>, Set<Slot<?>>> lrdep = new HashMap<Slot<?>, Set<Slot<?>>>();
	    for(Slot<?> s : slots)
		lrdep.put(s, new HashSet<Slot<?>>());
	    for(Slot<?> s : slots) {
		lrdep.get(s).addAll(Arrays.asList(s.rdep));
		for(Slot<?> ds : s.dep)
		    lrdep.get(ds).add(s);
	    }
	    Set<Slot<?>> left = new HashSet<Slot<?>>(slots);
	    final Map<Slot<?>, Integer> order = new HashMap<Slot<?>, Integer>();
	    int id = left.size() - 1;
	    Slot<?>[] cp = new Slot<?>[0];
	    while(!left.isEmpty()) {
		boolean err = true;
		fin:
		for(Iterator<Slot<?>> i = left.iterator(); i.hasNext();) {
		    Slot<?> s = i.next();
		    for(Slot<?> ds : lrdep.get(s)) {
			if(left.contains(ds))
			    continue fin;
		    }
		    err = false;
		    order.put(s, s.depid = id--);
		    Set<Slot<?>> grdep = new HashSet<Slot<?>>();
		    for(Slot<?> ds : lrdep.get(s)) {
			grdep.add(ds);
			for(Slot<?> ds2 : ds.grdep)
			    grdep.add(ds2);
		    }
		    s.grdep = grdep.toArray(cp);
		    i.remove();
		}
		if(err)
		    throw(new RuntimeException("Cycle encountered while compiling state slot dependencies"));
	    }
	    Comparator<Slot<?>> cmp = new Comparator<Slot<?>>() {
		public int compare(Slot<?> a, Slot<?> b) {
		    return(order.get(a) - order.get(b));
		}
	    };
	    for(Slot<?> s : slots)
		Arrays.sort(s.grdep, cmp);
	}
	
	public static void update() {
	    synchronized(Slot.class) {
		if(!dirty)
		    return;
		makedeps(all);
		deplist = new Slot<?>[all.size()];
		for(Slot s : all)
		    deplist[s.depid] = s;
		dirty = false;
	    }
	}
    }
    
    public static class Buffer {
	private GLState[] states = new GLState[slotnum];
	
	public Buffer copy() {
	    Buffer ret = new Buffer();
	    System.arraycopy(states, 0, ret.states, 0, states.length);
	    return(ret);
	}
	
	public void copy(Buffer dest) {
	    dest.adjust();
	    System.arraycopy(states, 0, dest.states, 0, states.length);
	    for(int i = states.length; i < dest.states.length; i++)
		dest.states[i] = null;
	}
	
	private void adjust() {
	    if(states.length < slotnum) {
		GLState[] n = new GLState[slotnum];
		System.arraycopy(states, 0, n, 0, states.length);
		this.states = n;
	    }
	}

	public <T extends GLState> void put(Slot<? super T> slot, T state) {
	    if(states.length <= slot.id)
		adjust();
	    states[slot.id] = state;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends GLState> T get(Slot<T> slot) {
	    if(states.length <= slot.id)
		return(null);
	    return((T)states[slot.id]);
	}
	
	public boolean equals(Object o) {
	    if(!(o instanceof Buffer))
		return(false);
	    Buffer b = (Buffer)o;
	    adjust();
	    b.adjust();
	    for(int i = 0; i < states.length; i++) {
		if(!states[i].equals(b.states[i]))
		    return(false);
	    }
	    return(true);
	}
	
	public String toString() {
	    StringBuilder buf = new StringBuilder();
	    buf.append('[');
	    for(int i = 0; i < states.length; i++) {
		if(i > 0)
		    buf.append(", ");
		if(states[i] == null)
		    buf.append("null");
		else
		    buf.append(states[i].toString());
	    }
	    buf.append(']');
	    return(buf.toString());
	}
    }
    
    public static int bufdiff(Buffer f, Buffer t, boolean[] trans, boolean[] repl) {
	Slot.update();
	int cost = 0;
	f.adjust(); t.adjust();
	if(trans != null) {
	    for(int i = 0; i < trans.length; i++) {
		trans[i] = false;
		repl[i] = false;
	    }
	}
	for(int i = 0; i < f.states.length; i++) {
	    if(((f.states[i] == null) != (t.states[i] == null)) ||
	       ((f.states[i] != null) && (t.states[i] != null) && !f.states[i].equals(t.states[i]))) {
		if(!repl[i]) {
		    int cat = -1, caf = -1;
		    if((t.states[i] != null) && (f.states[i] != null)) {
			cat = f.states[i].capplyto(t.states[i]);
			caf = t.states[i].capplyfrom(f.states[i]);
		    }
		    if((cat >= 0) && (caf >= 0)) {
			cost += cat + caf;
			if(trans != null)
			    trans[i] = true;
		    } else {
			if(f.states[i] != null)
			    cost += f.states[i].cunapply();
			if(t.states[i] != null)
			    cost += t.states[i].capply();
			if(trans != null)
			    repl[i] = true;
		    }
		}
		for(Slot ds : idlist[i].grdep) {
		    int id = ds.id;
		    if(repl[id])
			continue;
		    if(trans != null)
			repl[id] = true;
		    if(t.states[id] != null)
			cost += t.states[id].cunapply();
		    if(f.states[id] != null)
			cost += f.states[id].capply();
		}
	    }
	}
	return(cost);
    }
    
    public static class Applier {
	private Buffer cur = new Buffer(), next = new Buffer();
	public final GL gl;
	private boolean[] trans = new boolean[0], repl = new boolean[0];
	
	public Applier(GL gl) {
	    this.gl = gl;
	}
	
	public <T extends GLState> void put(Slot<? super T> slot, T state) {
	    next.put(slot, state);
	}
	
	public <T extends GLState> T get(Slot<T> slot) {
	    return(next.get(slot));
	}
	
	public <T extends GLState> T cur(Slot<T> slot) {
	    return(cur.get(slot));
	}
	
	public void prep(GLState st) {
	    st.prep(next);
	}

	public void set(Buffer to) {
	    to.copy(next);
	}
	
	public void copy(Buffer dest) {
	    next.copy(dest);
	}
	
	public Buffer copy() {
	    return(next.copy());
	}
	
	public void apply(GOut g) {
	    if(trans.length < slotnum) {
		synchronized(Slot.class) {
		    trans = new boolean[slotnum];
		    repl = new boolean[slotnum];
		}
	    }
	    bufdiff(cur, next, trans, repl);
	    for(int i = trans.length - 1; i >= 0; i--) {
		if(repl[i]) {
		    if(cur.states[i] != null)
			cur.states[i].unapply(g);
		    cur.states[i] = null;
		}
	    }
	    for(int i = 0; i < trans.length; i++) {
		if(repl[i]) {
		    cur.states[i] = next.states[i];
		    if(cur.states[i] != null)
			cur.states[i].apply(g);
		} else if(trans[i]) {
		    cur.states[i].applyto(g, next.states[i]);
		    GLState cs = cur.states[i];
		    (cur.states[i] = next.states[i]).applyfrom(g, cs);
		}
	    }
	    checkerr(gl);
	}

	/* "Meta-states" */
	private int matmode = GL.GL_MODELVIEW;
	private int texunit = 0;
	
	public void matmode(int mode) {
	    if(mode != matmode) {
		gl.glMatrixMode(mode);
		matmode = mode;
	    }
	}
	
	public void texunit(int unit) {
	    if(unit != texunit) {
		gl.glActiveTexture(GL.GL_TEXTURE0 + unit);
		texunit = unit;
	    }
	}
    }
    
    private class Wrapping implements Rendered {
	private final Rendered r;
	
	private Wrapping(Rendered r) {
	    this.r = r;
	}
	
	public void draw(GOut g) {}
	
	public Order setup(RenderList rl) {
	    rl.add(r, GLState.this);
	    return(null);
	}
    }
    
    public Rendered apply(Rendered r) {
	return(new Wrapping(r));
    }
    
    public static GLState compose(final GLState... states) {
	return(new GLState() {
		public void apply(GOut g) {}
		public void unapply(GOut g) {}
		public void prep(Buffer buf) {
		    for(GLState st : states) {
			st.prep(buf);
		    }
		}
	    });
    }
    
    public static abstract class StandAlone extends GLState {
	public final Slot<StandAlone> slot;
	
	public StandAlone(Slot<?>... dep) {
	    slot = new Slot<StandAlone>(StandAlone.class, dep);
	}
	
	public void prep(Buffer buf) {
	    buf.put(slot, this);
	}
    }
}
