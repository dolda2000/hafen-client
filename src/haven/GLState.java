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
import haven.glsl.ShaderMacro;
import static haven.GOut.checkerr;

public abstract class GLState {
    public abstract void apply(GOut g);
    public abstract void unapply(GOut g);
    public abstract void prep(Buffer buf);
    
    public void applyfrom(GOut g, GLState from) {
	throw(new RuntimeException("Called applyfrom on non-conformant GLState (" + from + " -> " + this + ")"));
    }
    public void applyto(GOut g, GLState to) {
    }
    public void reapply(GOut g) {
    }
    
    public ShaderMacro[] shaders() {
	return(null);
    }
    
    public boolean reqshaders() {
	return(false);
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
	public final Type type;
	public final int id;
	public final Class<T> scl;
	private int depid = -1;
	private final Slot<?>[] dep, rdep;
	private Slot[] grdep;
	
	public static enum Type {
	    SYS, GEOM, DRAW
	}
	
	public Slot(Type type, Class<T> scl, Slot<?>[] dep, Slot<?>[] rdep) {
	    this.type = type;
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
	
	public Slot(Type type, Class<T> scl, Slot... dep) {
	    this(type, scl, dep, null);
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
	    Comparator<Slot> cmp = new Comparator<Slot>() {
		public int compare(Slot a, Slot b) {
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
	
	public String toString() {
	    return("Slot<" + scl.getName() + ">");
	}
    }
    
    public static class Buffer {
	private GLState[] states = new GLState[slotnum];
	public final GLConfig cfg;
	
	public Buffer(GLConfig cfg) {
	    this.cfg = cfg;
	}
	
	public Buffer copy() {
	    Buffer ret = new Buffer(cfg);
	    System.arraycopy(states, 0, ret.states, 0, states.length);
	    return(ret);
	}
	
	public void copy(Buffer dest) {
	    dest.adjust();
	    System.arraycopy(states, 0, dest.states, 0, states.length);
	    for(int i = states.length; i < dest.states.length; i++)
		dest.states[i] = null;
	}

	public void copy(Buffer dest, Slot.Type type) {
	    dest.adjust();
	    adjust();
	    for(int i = 0; i < states.length; i++) {
		if(idlist[i].type == type)
		    dest.states[i] = states[i];
	    }
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
	
	/* Should be used very, very sparingly. */
	GLState[] states() {
	    return(states);
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

    public static class TexUnit {
	private final Applier st;
	public final int id;

	private TexUnit(Applier st, int id) {
	    this.st = st;
	    this.id = id;
	}

	public void act() {
	    st.texunit(id);
	}

	public void free() {
	    if(st.textab[id] != null)
		throw(new RuntimeException("Texunit " + id + " freed twice"));
	    st.textab[id] = this;
	}

	public void ufree() {
	    act();
	    st.gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
	    free();
	}
    }

    public static class Applier {
	public static boolean debug = false;
	private Buffer old, cur, next;
	public final GL2 gl;
	public final GLConfig cfg;
	private boolean[] trans = new boolean[0], repl = new boolean[0], adirty = new boolean[0];
	private ShaderMacro[][] shaders = new ShaderMacro[0][], nshaders = new ShaderMacro[0][];
	private int proghash = 0, nproghash = 0;
	public ShaderMacro.Program prog;
	public boolean usedprog;
	public boolean pdirty = false, sdirty = false;
	public long time = 0;
	
	/* It seems ugly to treat these so specially, but right now I
	 * cannot see any good alternative. */
	public Matrix4f cam = Matrix4f.id, wxf = Matrix4f.id, mv = Matrix4f.identity();
	private Matrix4f ccam = null, cwxf = null;
	
	public Applier(GL2 gl, GLConfig cfg) {
	    this.gl = gl;
	    this.cfg = cfg;
	    this.old = new Buffer(cfg);
	    this.cur = new Buffer(cfg);
	    this.next = new Buffer(cfg);
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
	
	public <T extends GLState> T old(Slot<T> slot) {
	    return(old.get(slot));
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
	    long st = 0;
	    if(Config.profile) st = System.nanoTime();
	    if(trans.length < slotnum) {
		synchronized(Slot.class) {
		    trans = new boolean[slotnum];
		    repl = new boolean[slotnum];
		    shaders = Utils.extend(shaders, slotnum);
		    nshaders = Utils.extend(shaders, slotnum);
		}
	    }
	    bufdiff(cur, next, trans, repl);
	    Slot<?>[] deplist = GLState.deplist;
	    nproghash = proghash;
	    for(int i = trans.length - 1; i >= 0; i--) {
		nshaders[i] = shaders[i];
		if(repl[i] || trans[i]) {
		    GLState nst = next.states[i];
		    ShaderMacro[] ns = (nst == null)?null:nst.shaders();
		    if(ns != nshaders[i]) {
			nproghash ^= System.identityHashCode(nshaders[i]) ^ System.identityHashCode(ns);
			nshaders[i] = ns;
			sdirty = true;
		    }
		}
	    }
	    usedprog = prog != null;
	    if(sdirty) {
		ShaderMacro.Program np;
		boolean usesl;
		switch(g.gc.pref.progmode.val) {
		case ALWAYS:
		    usesl = true;
		    break;
		case REQ:
		    usesl = false;
		    for(int i = 0; i < trans.length; i++) {
			if((nshaders[i] != null) && next.states[i].reqshaders()) {
			    usesl = true;
			    break;
			}
		    }
		    break;
		case NEVER:
		default: /* ¦] */
		    usesl = false;
		    break;
		}
		if(usesl) {
		    np = findprog(nproghash, nshaders);
		} else {
		    np = null;
		}
		if(np != prog) {
		    if(np != null)
			np.apply(g);
		    else
			g.gl.glUseProgramObjectARB(0);
		    prog = np;
		    if(debug)
			checkerr(g.gl);
		    pdirty = true;
		}
	    }
	    if((prog != null) != usedprog) {
		for(int i = 0; i < trans.length; i++) {
		    if(trans[i])
			repl[i] = true;
		}
	    }
	    cur.copy(old);
	    for(int i = deplist.length - 1; i >= 0; i--) {
		int id = deplist[i].id;
		if(repl[id]) {
		    if(cur.states[id] != null) {
			cur.states[id].unapply(g);
			if(debug)
			    stcheckerr(g, "unapply", cur.states[id]);
		    }
		    cur.states[id] = null;
		    proghash ^= System.identityHashCode(shaders[id]);
		    shaders[id] = null;
		}
	    }
	    /* Note on invariants: States may exit non-locally
	     * from apply, applyto/applyfrom or reapply (e.g. by
	     * throwing Loading exceptions) in a defined way
	     * provided they do so before they have changed any GL
	     * state. If they exit non-locally after GL state has
	     * been altered, future results are undefined. */
	    for(int i = 0; i < deplist.length; i++) {
		int id = deplist[i].id;
		if(repl[id]) {
		    if(next.states[id] != null) {
			next.states[id].apply(g);
			cur.states[id] = next.states[id];
			proghash ^= System.identityHashCode(shaders[id]) ^ System.identityHashCode(nshaders[id]);
			shaders[id] = nshaders[id];
			if(debug)
			    stcheckerr(g, "apply", cur.states[id]);
		    }
		    if(!pdirty && (prog != null))
			prog.adirty(deplist[i]);
		} else if(trans[id]) {
		    cur.states[id].applyto(g, next.states[id]);
		    if(debug)
			stcheckerr(g, "applyto", cur.states[id]);
		    next.states[id].applyfrom(g, cur.states[id]);
		    cur.states[id] = next.states[id];
		    proghash ^= System.identityHashCode(shaders[id]) ^ System.identityHashCode(nshaders[id]);
		    shaders[id] = nshaders[id];
		    if(debug)
			stcheckerr(g, "applyfrom", cur.states[id]);
		    if(!pdirty && (prog != null))
			prog.adirty(deplist[i]);
		} else if((prog != null) && pdirty && (shaders[id] != null)) {
		    cur.states[id].reapply(g);
		    if(debug)
			stcheckerr(g, "reapply", cur.states[id]);
		}
	    }
	    if((ccam != cam) || (cwxf != wxf)) {
		/* See comment above */
		mv.load(ccam = cam).mul1(cwxf = wxf);
		matmode(GL2.GL_MODELVIEW);
		gl.glLoadMatrixf(mv.m, 0);
	    }
	    if(prog != null)
		prog.autoapply(g, pdirty);
	    pdirty = sdirty = false;
	    checkerr(gl);
	    if(Config.profile)
		time += System.nanoTime() - st;
	}
	
	public static class ApplyException extends RuntimeException {
	    public final transient GLState st;
	    public final String func;
	    
	    public ApplyException(String func, GLState st, Throwable cause) {
		super("Error in " + func + " of " + st, cause);
		this.st = st;
		this.func = func;
	    }
	}
	
	private void stcheckerr(GOut g, String func, GLState st) {
	    try {
		checkerr(g.gl);
	    } catch(RuntimeException e) {
		throw(new ApplyException(func, st, e));
	    }
	}

	/* "Meta-states" */
	private int matmode = GL2.GL_MODELVIEW;
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

	private TexUnit[] textab = new TexUnit[0];

	public TexUnit texalloc() {
	    int i;
	    for(i = 0; i < textab.length; i++) {
		if(textab[i] != null) {
		    TexUnit ret = textab[i];
		    textab[i] = null;
		    return(ret);
		}
	    }
	    textab = new TexUnit[i + 1];
	    return(new TexUnit(this, i));
	}

	public TexUnit texalloc(GOut g, TexGL tex) {
	    TexUnit ret = texalloc();
	    ret.act();
	    gl.glBindTexture(GL.GL_TEXTURE_2D, tex.glid(g));
	    return(ret);
	}

	public TexUnit texalloc(GOut g, TexMS tex) {
	    TexUnit ret = texalloc();
	    ret.act();
	    gl.glBindTexture(GL3.GL_TEXTURE_2D_MULTISAMPLE, tex.glid(g));
	    return(ret);
	}

	/* Program internation */
	public static class SavedProg {
	    public final int hash;
	    public final ShaderMacro.Program prog;
	    public final ShaderMacro[][] shaders;
	    public SavedProg next;
	    boolean used = true;
	    
	    public SavedProg(int hash, ShaderMacro.Program prog, ShaderMacro[][] shaders) {
		this.hash = hash;
		this.prog = prog;
		this.shaders = Utils.splice(shaders, 0);
	    }
	}
	
	private SavedProg[] ptab = new SavedProg[32];
	private int nprog = 0;
	private long lastclean = System.currentTimeMillis();
	
	private ShaderMacro.Program findprog(int hash, ShaderMacro[][] shaders) {
	    int idx = hash & (ptab.length - 1);
	    outer: for(SavedProg s = ptab[idx]; s != null; s = s.next) {
		if(s.hash != hash)
		    continue;
		int i;
		for(i = 0; i < s.shaders.length; i++) {
		    if(shaders[i] != s.shaders[i])
			continue outer;
		}
		for(; i < shaders.length; i++) {
		    if(shaders[i] != null)
			continue outer;
		}
		s.used = true;
		return(s.prog);
	    }
	    Collection<ShaderMacro> mods = new LinkedList<ShaderMacro>();
	    for(int i = 0; i < shaders.length; i++) {
		if(shaders[i] == null)
		    continue;
		for(int o = 0; o < shaders[i].length; o++)
		    mods.add(shaders[i][o]);
	    }
	    ShaderMacro.Program prog = ShaderMacro.Program.build(mods);
	    SavedProg s = new SavedProg(hash, prog, shaders);
	    s.next = ptab[idx];
	    ptab[idx] = s;
	    nprog++;
	    if(nprog > ptab.length)
		rehash(ptab.length * 2);
	    return(prog);
	}
	
	private void rehash(int nlen) {
	    SavedProg[] ntab = new SavedProg[nlen];
	    for(int i = 0; i < ptab.length; i++) {
		while(ptab[i] != null) {
		    SavedProg s = ptab[i];
		    ptab[i] = s.next;
		    int ni = s.hash & (ntab.length - 1);
		    s.next = ntab[ni];
		    ntab[ni] = s;
		}
	    }
	    ptab = ntab;
	}
	
	public void clean() {
	    long now = System.currentTimeMillis();
	    if(now - lastclean > 60000) {
		for(int i = 0; i < ptab.length; i++) {
		    SavedProg c, p;
		    for(c = ptab[i], p = null; c != null; p = c, c = c.next) {
			if(!c.used) {
			    if(p != null)
				p.next = c.next;
			    else
				ptab[i] = c.next;
			    c.prog.dispose();
			    nprog--;
			} else {
			    c.used = false;
			}
		    }
		}
		/* XXX: Rehash into smaller table? It's probably not a
		 * problem, but it might be nice just for
		 * completeness. */
		lastclean = now;
	    }
	}
	
	public int numprogs() {
	    return(nprog);
	}
    }
    
    private class Wrapping implements Rendered {
	private final Rendered r;
	
	private Wrapping(Rendered r) {
	    if(r == null)
		throw(new NullPointerException("Wrapping null in " + GLState.this));
	    this.r = r;
	}
	
	public void draw(GOut g) {}
	
	public boolean setup(RenderList rl) {
	    rl.add(r, GLState.this);
	    return(false);
	}
    }
    
    public Rendered apply(Rendered r) {
	return(new Wrapping(r));
    }
    
    public static abstract class Abstract extends GLState {
	public void apply(GOut g) {}
	public void unapply(GOut g) {}
    }

    public static class Composed extends Abstract {
	public final GLState[] states;

	public Composed(GLState... states) {
	    for(GLState st : states) {
		if(st == null)
		    throw(new RuntimeException("null state in list of " + Arrays.asList(states)));
	    }
	    this.states = states;
	}

	public boolean equals(Object o) {
	    if(!(o instanceof Composed))
		return(false);
	    return(Arrays.equals(states, ((Composed)o).states));
	}

	public int hashCode() {
	    return(Arrays.hashCode(states));
	}

	public void prep(Buffer buf) {
	    for(GLState st : states)
		st.prep(buf);
	}

	public String toString() {
	    return("composed(" + Arrays.asList(states) + ")");
	}
    }

    public static GLState compose(GLState... states) {
	return(new Composed(states));
    }
    
    public static class Delegate extends Abstract {
	public GLState del;
	
	public Delegate(GLState del) {
	    this.del = del;
	}
	
	public void prep(Buffer buf) {
	    del.prep(buf);
	}
    }
    
    public interface GlobalState {
	public Global global(RenderList r, Buffer ctx);
    }
    
    public interface Global {
	public void postsetup(RenderList rl);
	public void prerender(RenderList rl, GOut g);
	public void postrender(RenderList rl, GOut g);
    }
    
    public static abstract class StandAlone extends GLState {
	public final Slot<StandAlone> slot;
	
	public StandAlone(Slot.Type type, Slot<?>... dep) {
	    slot = new Slot<StandAlone>(type, StandAlone.class, dep);
	}
	
	public void prep(Buffer buf) {
	    buf.put(slot, this);
	}
    }
    
    public static final GLState nullstate = new GLState() {
	    public void apply(GOut g) {}
	    public void unapply(GOut g) {}
	    public void prep(Buffer buf) {}
	};

    static {
	Console.setscmd("applydb", new Console.Command() {
		public void run(Console cons, String[] args) {
		    Applier.debug = Utils.parsebool(args[1], false);
		}
	    });
    }
}
