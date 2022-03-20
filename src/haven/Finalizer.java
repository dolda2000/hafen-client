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
import java.security.*;

/* Java is deprecating Object.finalize (understandably enough), but
 * since Java 8 is still overwhelmingly popular, and
 * java.lang.ref.Cleaner was only introduced in Java 9, I seem to be
 * left with little choice but to implement my own. At least it's not
 * particularly complicated. */
public class Finalizer {
    private final ReferenceQueue<Object> queue = new ReferenceQueue<>();
    private Function<Runnable, Thread> ctx;
    private Thread th;
    private Ref list;

    public Finalizer(Function<Runnable, Thread> ctx) {
	this.ctx = ctx;
    }

    public Finalizer() {
	this(tgt -> new HackThread(tgt, "Finalization thread"));
    }

    private class Ref extends PhantomReference<Object> implements Runnable {
	final Runnable action;
	boolean linked;
	Ref next, prev;

	Ref(Object x, Runnable action) {
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
	    }
	}

	public void run() {
	    boolean linked;
	    synchronized(Finalizer.this) {
		linked = this.linked;
		clear();
	    }
	    if(linked)
		action.run();
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

    public Runnable add(Object x, Runnable action) {
	synchronized(this) {
	    Ref ret = new Ref(x, action);
	    ret.add();
	    ckrun();
	    return(ret);
	}
    }

    private static final Map<ThreadGroup, Finalizer> groups = new WeakHashMap<>();
    private static Finalizer getgroup() {
	return(AccessController.doPrivileged((PrivilegedAction<Finalizer>)() -> {
		    ThreadGroup tg = Thread.currentThread().getThreadGroup();
		    synchronized(groups) {
			Finalizer ret = groups.get(tg);
			if(ret == null)
			    groups.put(tg, ret = new Finalizer(tgt -> new HackThread(tg, tgt, "Finalization thread")));
			return(ret);
		    }
		}));
    }

    public static Runnable finalize(Object x, Runnable action) {
	return(getgroup().add(x, action));
    }

    public static class LeakCheck implements Disposable {
	public final String desc;
	private final Runnable fin;
	private boolean clean = false;

	public LeakCheck(Object guarded, String desc) {
	    this.desc = desc;
	    fin = Finalizer.finalize(guarded, this::disposed);
	}

	public LeakCheck(Object guarded) {
	    this(guarded, guarded.toString());
	}

	private void disposed() {
	    synchronized(this) {
		if(!this.clean)
		    Warning.warn("leak-check: %s leaked", desc);
	    }
	}

	public void dispose() {
	    synchronized(this) {
		if(this.clean)
		    Warning.warn("leak-check: %s already disposed", desc);
		this.clean = true;
	    }
	}
    }
}
