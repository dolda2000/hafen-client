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
import java.util.function.*;
import java.lang.ref.*;
import java.lang.reflect.*;
import java.security.*;

/* Java is deprecating Object.finalize (understandably enough), but
 * since Java 8 is still overwhelmingly popular, and
 * java.lang.ref.Cleaner was only introduced in Java 9, I seem to be
 * left with little choice but to implement my own. At least it's not
 * particularly complicated. */
public class Finalizer {
    private static boolean CHECK_CYCLES = false;
    private final ReferenceQueue<Object> queue = new ReferenceQueue<>();
    private Function<Runnable, Thread> ctx;
    private Thread th;
    private Ref list;
    private int n;

    public Finalizer(Function<Runnable, Thread> ctx) {
	this.ctx = ctx;
    }

    public Finalizer() {
	this(tgt -> new HackThread(tgt, "Finalization thread"));
    }

    public static interface Cleaner {
	public void clean();
    }

    public static interface Formattable {
	public String format();
    }

    private class Ref extends PhantomReference<Object> implements Runnable {
	final Cleaner action;
	boolean linked;
	Ref next, prev;

	Ref(Object x, Cleaner action) {
	    super(x, queue);
	    this.action = action;
	}

	private void add() {
	    synchronized(Finalizer.this) {
		if(linked)
		    throw(new IllegalStateException());
		prev = null;
		if((next = list) != null)
		    next.prev = this;
		list = this;
		linked = true;
		n++;
	    }
	}

	private void remove() {
	    synchronized(Finalizer.this) {
		if(!linked)
		    return;
		if(next != null)
		    next.prev = prev;
		if(prev != null)
		    prev.next = next;
		if(list == this)
		    list = next;
		linked = false;
		next = prev = null;
		n--;
	    }
	}

	public void run() {
	    boolean linked;
	    synchronized(Finalizer.this) {
		linked = this.linked;
		clear();
	    }
	    if(linked)
		action.clean();
	}

	public void clear() {
	    synchronized(Finalizer.this) {
		remove();
		super.clear();
	    }
	}
    }

    private void run() {
	try {
	    while(true) {
		synchronized(this) {
		    if(list == null)
			break;
		}
		try {
		    Ref ref = (Ref)queue.remove();
		    if(ref != null)
			ref.run();
		} catch(Throwable exc) {
		    new Warning(exc, "unexpected exception in finalizer").issue();
		}
	    }
	} finally {
	    synchronized(this) {
		if(th != Thread.currentThread())
		    Warning.warn("finalizer thread curiously not this thread; %s != %s", th, Thread.currentThread());
		th = null;
		ckrun();
	    }
	}
    }

    private void ckrun() {
	if((list != null) && (th == null)) {
	    th = ctx.apply(this::run);
	    th.setDaemon(true);
	    th.start();
	}
    }

    public Runnable add(Object x, Cleaner action) {
	if(CHECK_CYCLES)
	    checkcycle(x, action);
	synchronized(this) {
	    Ref ret = new Ref(x, action);
	    ret.add();
	    ckrun();
	    return(ret);
	}
    }

    private static final Map<ThreadGroup, Finalizer> groups = new WeakHashMap<>();
    public static Finalizer get() {
	ThreadGroup tg = Thread.currentThread().getThreadGroup();
	synchronized(groups) {
	    Finalizer ret = groups.get(tg);
	    if(ret == null)
		groups.put(tg, ret = new Finalizer(tgt -> new HackThread(tg, tgt, "Finalization thread")));
	    return(ret);
	}
    }

    public static Runnable finalize(Object x, Cleaner action) {
	return(get().add(x, action));
    }

    /* All the infrastructure required for stats seems hardly elegant,
     * but I'm not sure I can think of a better way (and this is
     * hardly optimal as is) to debug potential live-leaks of objects
     * via Finalier. */
    public static class Snapshot {
	private final Collection<Entry> refs;

	public static class Entry {
	    public final int id;
	    public final String desc;
	    private Ref ref;

	    private Entry(Ref ref) {
		this.ref = ref;
		this.id = System.identityHashCode(ref);
		if(ref.action instanceof Formattable)
		    this.desc = ((Formattable)ref.action).format();
		else
		    this.desc = ref.action.getClass().toString();
	    }

	    public boolean equals(Entry that) {return(this.id == that.id);}
	    public boolean equals(Object that) {return((that instanceof Entry) && equals((Entry)that));}
	    public int hashCode() {return(id);}
	}

	private Snapshot(Finalizer from) {
	    Collection<Entry> refs = new HashSet<>();
	    synchronized(from) {
		for(Ref ref = from.list; ref != null; ref = ref.next) {
		    if(!refs.add(new Entry(ref)))
			Warning.warn("identity-hashcode collision");
		}
	    }
	    this.refs = new ArrayList<>(refs);
	}

	public Snapshot weaken() {
	    for(Entry ent : refs)
		ent.ref = null;
	    return(this);
	}

	public String summary() {
	    Map<String, Integer> stats = new HashMap<>();
	    for(Entry ref : refs)
		stats.compute(ref.desc, (k, v) -> ((v == null) ? 0 : v) + 1);
	    
	    List<String> ids = new ArrayList<>(stats.keySet());
	    Collections.sort(ids);
	    int len = 0;
	    for(String id : ids)
		len = Math.max(len, id.length());
	    len += 4;
	    StringBuilder buf = new StringBuilder();
	    for(String id : ids) {
		buf.append(id);
		for(int i = 0, n = len - id.length(); i < n; i++)
		    buf.append(' ');
		buf.append(stats.get(id));
		buf.append('\n');
	    }
	    return(buf.toString());
	}

	public String delta(Snapshot prev) {
	    Set<Entry> prefs = new HashSet<>(), crefs = new HashSet<>(), nrefs = new HashSet<>();
	    for(Entry ref : prev.refs)
		prefs.add(ref);
	    for(Entry ref : this.refs) {
		if(prefs.remove(ref))
		    crefs.add(ref);
		else
		    nrefs.add(ref);
	    }
	    Map<String, Integer> same = new HashMap<>(), rem = new HashMap<>(), add = new HashMap<>();
	    for(Entry ref : crefs)
		same.compute(ref.desc, (k, v) -> ((v == null) ? 0 : v) + 1);
	    for(Entry ref : prefs)
		rem.compute(ref.desc, (k, v) -> ((v == null) ? 0 : v) + 1);
	    for(Entry ref : nrefs)
		add.compute(ref.desc, (k, v) -> ((v == null) ? 0 : v) + 1);
	    Set<String> ids = new HashSet<>(same.keySet());
	    ids.addAll(rem.keySet());
	    ids.addAll(add.keySet());
	    int len = 0;
	    for(String id : ids)
		len = Math.max(len, id.length());
	    len += 4;
	    List<String> sids = new ArrayList<>(ids);
	    Collections.sort(sids);
	    StringBuilder buf = new StringBuilder();
	    for(String id : sids) {
		buf.append(id);
		for(int i = 0, n = len - id.length(); i < n; i++)
		    buf.append(' ');
		buf.append(String.format("%4d= %4d- %4d+", same.getOrDefault(id, 0), rem.getOrDefault(id, 0), add.getOrDefault(id, 0)));
		buf.append('\n');
	    }
	    return(buf.toString());
	}
    }

    public Snapshot snapshot() {
	return(new Snapshot(this));
    }

    private static void checkcycle(Object ref, Object root) {
	Map<Object, Object> back = new IdentityHashMap<>();
	Map<Object, Object> btyp = new IdentityHashMap<>();
	Map<Object, Integer> blen = new IdentityHashMap<>();
	Map<Object, Object> closed = new IdentityHashMap<>();
	Queue<Object> open = new LinkedList<>();
	open.add(root);
	closed.put(root, root);
	back.put(root, null);
	blen.put(root, 0);
	while(!open.isEmpty()) {
	    Object ob = open.remove();
	    if(ob == ref) {
		List<Object> path = new ArrayList<>();
		for(Object prev = ob; prev != null; prev = back.get(prev))
		    path.add(prev);
		path.remove(path.size() - 1);
		Collections.reverse(path);
		StringBuilder buf = new StringBuilder();
		for(Object el : path) {
		    Object pref = btyp.get(el);
		    buf.append(back.get(el));
		    if(pref instanceof Integer)
			buf.append(" [" + pref + "]");
		    else if(pref instanceof Field)
			buf.append(" -> " + ((Field)pref).getName());
		    buf.append("\n");
		}
		throw(new RuntimeException("cycle found:\n" + buf));
	    }
	    int clen = blen.get(ob);
	    closed.put(ob, ob);
	    if(ob instanceof Reference) {
	    } else if(ob instanceof Object[]) {
		Object[] arr = (Object[])ob;
		for(int i = 0; i < arr.length; i++) {
		    if(arr[i] == null)
			continue;
		    Integer slen = blen.get(arr[i]);
		    if((slen != null) && (slen <= clen + 1))
			continue;
		    back.put(arr[i], ob);
		    btyp.put(arr[i], Integer.valueOf(i));
		    blen.put(arr[i], clen + 1);
		    if(!closed.containsKey(arr[i])) {
			open.add(arr[i]);
			closed.put(arr[i], arr[i]);
		    }
		}
	    } else {
		for(Class<?> cl = ob.getClass(); cl != null; cl = cl.getSuperclass()) {
		    for(Field f : cl.getDeclaredFields()) {
			if((f.getModifiers() & Modifier.STATIC) != 0)
			    continue;
			f.setAccessible(true);
			Object nx;
			try {
			    nx = f.get(ob);
			} catch(IllegalAccessException e) {
			    throw(new RuntimeException(e));
			}
			if(nx == null)
			    continue;
			Integer slen = blen.get(nx);
			if((slen != null) && (slen <= clen + 1))
			    continue;
			back.put(nx, ob);
			btyp.put(nx, f);
			blen.put(nx, clen + 1);
			if(!closed.containsKey(nx)) {
			    open.add(nx);
			    closed.put(nx, nx);
			}
		    }
		}
	    }
	}
    }

    public static class LeakCheck implements Disposable, Cleaner, Formattable {
	private static boolean leaking = false;
	public final String desc;
	private final Runnable fin;
	private final Class<?> cls;
	private final Throwable create;
	private boolean clean = false;

	public LeakCheck(Object guarded, String desc) {
	    this.desc = desc;
	    fin = Finalizer.finalize(guarded, this);
	    cls = guarded.getClass();
	    create = leaking ? new Throwable() : null;
	}

	public LeakCheck(Object guarded) {
	    this(guarded, String.format("%s (%x)", guarded.toString(), System.identityHashCode(guarded)));
	}

	public void clean() {
	    synchronized(this) {
		if(!this.clean) {
		    new Warning(create, String.format("leak-check: %s leaked", desc)).issue();
		    leaking = true;
		}
	    }
	}

	public void dispose() {
	    synchronized(this) {
		if(this.clean)
		    Warning.warn("leak-check: %s already disposed", desc);
		this.clean = true;
	    }
	    fin.run();
	}

	public String format() {
	    return(String.format("#<leak-check %s>", cls.getName()));
	}
    }

    public static Disposable leakcheck(Object guarded, String desc) {return(new LeakCheck(guarded, desc));}
    public static Disposable leakcheck(Object guarded) {return(new LeakCheck(guarded));}

    public static class Disposer implements Cleaner, Formattable {
	public final Disposable tgt;

	public Disposer(Disposable tgt) {
	    this.tgt = tgt;
	}

	public void clean() {
	    tgt.dispose();
	}

	public String format() {
	    return(String.format("#<disposer %s>", tgt.getClass().getName()));
	}
    }

    public static class Reference<T extends Disposable> implements Disposable, Indir<T> {
	public final T ob;
	private final Runnable clean;

	public Reference(T ob) {
	    this.ob = ob;
	    this.clean = Finalizer.finalize(this, new Disposer(ob));
	}

	public T get() {return(ob);}

	public void dispose() {
	    clean.run();
	}
    }

    static {
	Console.setscmd("finstats", new Console.Command() {
		Snapshot prev = null;

		public void run(Console cons, String[] args) {
		    System.gc(); System.gc(); System.gc();
		    Snapshot stats = get().snapshot();
		    if(prev != null)
			System.err.println(stats.delta(prev));
		    prev = stats.weaken();
		}
	    });
    }
}
