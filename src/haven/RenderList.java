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
    public final GLConfig cfg;
    private Slot[] list = new Slot[100];
    private int cur = 0;
    private Slot curp = null;
    private GLState.Global[] gstates = new GLState.Global[0];
    private static final ThreadLocal<RenderList> curref = new ThreadLocal<RenderList>();
    
    public class Slot {
	public Rendered r;
	public GLState.Buffer os = new GLState.Buffer(cfg), cs = new GLState.Buffer(cfg);
	public Rendered.Order o;
	public boolean d;
	public Slot p;
    }
    
    public RenderList(GLConfig cfg) {
	this.cfg = cfg;
    }
    
    private Slot getslot() {
	int i = cur++;
	if(i >= list.length) {
	    Slot[] n = new Slot[i * 2];
	    System.arraycopy(list, 0, n, 0, i);
	    list = n;
	}
	Slot s;
	if((s = list[i]) == null)
	    s = list[i] = new Slot();
	return(s);
    }

    private final Iterable<Slot> slotsi = new Iterable<Slot>() {
	public Iterator<Slot> iterator() {
	    return(new Iterator<Slot>() {
		    private int i = 0;

		    public Slot next() {
			return(list[i++]);
		    }

		    public boolean hasNext() {
			return(i < cur);
		    }

		    public void remove() {
			throw(new UnsupportedOperationException());
		    }
		});
	}
    };
    public Iterable<Slot> slots() {
	return(slotsi);
    }

    public static RenderList current() {
	return(curref.get());
    }

    protected void setup(Slot s, Rendered r) {
	s.r = r;
	Slot pp = s.p = curp;
	if(pp == null)
	    curref.set(this);
	try {
	    curp = s;
	    s.d = r.setup(this);
	} finally {
	    if((curp = pp) == null)
		curref.remove();
	}
    }
    
    protected void postsetup(Slot ps, GLState.Buffer t) {
	gstates = getgstates();
	Slot pp = curp;
	try {
	    curp = ps;
	    for(GLState.Global gs : gstates) {
		t.copy(ps.cs);
		gs.postsetup(this);
	    }
	} finally {
	    curp = pp;
	}
    }

    public void setup(Rendered r, GLState.Buffer t) {
	rewind();
	Slot s = getslot();
	t.copy(s.os); t.copy(s.cs);
	setup(s, r);
	postsetup(s, t);
    }

    public void add(Rendered r, GLState t) {
	Slot s = getslot();
	if(curp == null)
	    throw(new RuntimeException("Tried to set up relative slot with no parent"));
	curp.cs.copy(s.os);
	if(t != null)
	    t.prep(s.os);
	s.os.copy(s.cs);
	setup(s, r);
    }
    
    public void add2(Rendered r, GLState.Buffer t) {
	Slot s = getslot();
	t.copy(s.os);
	s.r = r;
	s.p = curp;
	s.d = true;
    }
    
    public GLState.Buffer cstate() {
	return(curp.cs);
    }

    public GLState.Buffer state() {
	return(curp.os);
    }
    
    public void prepo(GLState t) {
	t.prep(curp.os);
    }
    
    public void prepc(GLState t) {
	t.prep(curp.cs);
    }
    
    @SuppressWarnings("unchecked")
    private static final Comparator<Slot> cmp = new Comparator<Slot>() {
	public int compare(Slot a, Slot b) {
	    if(!a.d && !b.d)
		return(0);
	    if(a.d && !b.d)
		return(-1);
	    if(!a.d && b.d)
		return(1);
	    int az = a.o.mainz(), bz = b.o.mainz();
	    if(az != bz)
		return(az - bz);
	    if(a.o != b.o)
		throw(new RuntimeException("Found two different orderings with the same main-Z: " + a.o + " and " + b.o));
	    return(a.o.cmp().compare(a.r, b.r, a.os, b.os));
	}
    };
    
    private GLState.Global[] getgstates() {
	/* This is probably a fast way to intern the states. */
	IdentityHashMap<GLState.Global, GLState.Global> gstates = new IdentityHashMap<GLState.Global, GLState.Global>(this.gstates.length);
	for(int i = 0; i < cur; i++) {
	    if(!list[i].d)
		continue;
	    GLState.Buffer ctx = list[i].os;
	    GLState[] sl = ctx.states();
	    for(GLState st : sl) {
		if(st instanceof GLState.GlobalState) {
		    GLState.Global gst = ((GLState.GlobalState)st).global(this, ctx);
		    gstates.put(gst, gst);
		}
	    }
	}
	return(gstates.keySet().toArray(new GLState.Global[0]));
    }

    public void fin() {
	for(int i = 0; i < cur; i++) {
	    if((list[i].o = list[i].os.get(Rendered.order)) == null)
		list[i].o = Rendered.deflt;
	}
	Arrays.sort(list, 0, cur, cmp);
    }
    
    protected void render(GOut g, Rendered r) {
	r.draw(g);
    }

    public void render(GOut g) {
	for(GLState.Global gs : gstates)
	    gs.prerender(this, g);
	for(int i = 0; i < cur; i++) {
	    Slot s = list[i];
	    if(!s.d)
		break;
	    g.st.set(s.os);
	    render(g, s.r);
	}
	for(GLState.Global gs : gstates)
	    gs.postrender(this, g);
    }

    public void rewind() {
	if(curp != null)
	    throw(new RuntimeException("Tried to rewind RenderList while adding to it."));
	cur = 0;
    }

    public void dump(java.io.PrintStream out) {
	for(Slot s : slots())
	    out.println((s.d?" ":"!") + s.r + ": " + s.os);
    }
}
