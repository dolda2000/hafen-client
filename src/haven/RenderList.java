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
    public static final int INSTANCE_THRESHOLD = 10;
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
	public int ihash;
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
	    int ret;
	    int az = a.o.mainz(), bz = b.o.mainz();
	    if((ret = (az - bz)) != 0)
		return(ret);
	    if((ret = a.o.cmp().compare(a.r, b.r, a.os, b.os)) != 0)
		return(ret);
	    if((ret = ((System.identityHashCode(a.r) & 0x7fffffff) - (System.identityHashCode(b.r) & 0x7fffffff))) != 0)
		return(ret);
	    return((a.ihash & 0x7fffffff) - (b.ihash & 0x7fffffff));
	}
    };
    
    private GLState[] dbc = new GLState[0];
    private GLState.Global[] getgstates() {
	/* This is probably a fast way to intern the states. */
	IdentityHashMap<GLState.Global, GLState.Global> gstates = new IdentityHashMap<GLState.Global, GLState.Global>(this.gstates.length);
	for(int i = 0; i < dbc.length; i++)
	    dbc[i] = null;
	for(int i = 0; i < cur; i++) {
	    if(!list[i].d)
		continue;
	    GLState.Buffer ctx = list[i].os;
	    GLState[] sl = ctx.states();
	    if(sl.length > dbc.length)
		dbc = new GLState[sl.length];
	    for(int o = 0; o < sl.length; o++) {
		GLState st = sl[o];
		if(st == dbc[o])
		    continue;
		if(st instanceof GLState.GlobalState) {
		    GLState.Global gst = ((GLState.GlobalState)st).global(this, ctx);
		    gstates.put(gst, gst);
		}
		dbc[o] = st;
	    }
	}
	return(gstates.keySet().toArray(new GLState.Global[0]));
    }

    public void fin() {
	for(int i = 0; i < cur; i++) {
	    Slot s = list[i];
	    if(s.os.get(Rendered.skip.slot) != null)
		s.d = false;
	}
	int nd = 0;
	for(int i = 0, o = cur - 1; i < o;) {
	    for(; (i < o) && list[i].d; i++);
	    for(; (i < o) && !list[o].d; o--);
	    if(i < o) {
		Slot t = list[i];
		list[i] = list[o];
		list[o] = t;
		i++; o--;
	    }
	    nd = i + 1;
	}
	for(int i = 0; i < nd; i++) {
	    Slot s = list[i];
	    if((s.o = s.os.get(Rendered.order)) == null)
		s.o = Rendered.deflt;
	    if(s.d)
		s.ihash = s.os.ihash();
	}
	Arrays.sort(list, 0, nd, cmp);
    }

    public static class RLoad extends Loading {
	public RLoad(Loading cause) {
	    super(cause);
	}
    }

    public boolean ignload = true;
    protected void render(GOut g, Rendered r) {
	try {
	    r.draw(g);
	} catch(RLoad l) {
	    if(ignload)
		return;
	    else
		throw(l);
	}
    }

    protected boolean renderinst(GOut g, Rendered.Instanced r, List<GLState.Buffer> instances) {
	try {
	    return(r.drawinst(g, instances));
	} catch(RLoad l) {
	    if(ignload)
		return(true);
	    else
		throw(l);
	}
    }

    public int drawn, instanced, instancified;
    private final List<GLState.Buffer> instbuf = new ArrayList<GLState.Buffer>();
    public void render(GOut g) {
	for(GLState.Global gs : gstates)
	    gs.prerender(this, g);
	drawn = instanced = instancified = 0;
	boolean doinst = g.gc.pref.instancing.val;
	int skipinst = 0, i = 0;
	rloop: while((i < cur) && list[i].d) {
	    Slot s = list[i];
	    tryinst: {
		if(!doinst || (i < skipinst) || !(s.r instanceof Rendered.Instanced))
		    break tryinst;
		int o;
		instbuf.clear();
		instbuf.add(s.os);
		for(o = i + 1; (o < cur) && list[o].d; o++) {
		    if((list[o].r != s.r) || (list[o].ihash != s.ihash))
			break;
		    if(!s.os.iequals(list[o].os))
			break;
		    instbuf.add(list[o].os);
		}
		if(o - i < INSTANCE_THRESHOLD)
		    break tryinst;
		Rendered.Instanced ir = (Rendered.Instanced)s.r;
		if(renderinst(g, ir, instbuf)) {
		    instanced++;
		    instancified += instbuf.size();
		    i = o;
		    continue rloop;
		} else {
		    skipinst = o;
		}
	    }
	    g.st.set(s.os);
	    render(g, s.r);
	    drawn++;
	    i++;
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
