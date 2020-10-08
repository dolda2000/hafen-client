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

public interface Waitable {
    public void waitfor(Runnable callback, Consumer<Waiting> reg);

    public static interface Waiting {
	public void cancel();

	public static Waiting dummy = new Waiting() {
		public void cancel() {}
	    };
    }

    public static class Queue implements Waitable {
	private Collection<Waiter> waiters = null;

	private class Waiter implements Waiting {
	    final Runnable callback;

	    Waiter(Runnable callback) {
		this.callback = callback;
	    }

	    public void cancel() {
		synchronized(Queue.this) {
		    if(waiters != null)
			waiters.remove(this);
		}
	    }
	}

	public void wnotify() {
	    Collection<Waiter> list;
	    synchronized(this) {
		list = waiters;
		waiters = null;
	    }
	    if(list != null) {
		for(Waiter w : list)
		    w.callback.run();
	    }
	}

	private Waiter add(Waiter w) {
	    synchronized(this) {
		if(waiters == null)
		    waiters = new HashSet<>();
		waiters.add(w);
		return(w);
	    }
	}

	public Waiter add(Runnable callback) {
	    return(add(new Waiter(callback)));
	}

	public void waitfor(Runnable callback, Consumer<Waiting> reg) {
	    synchronized(this) {
		reg.accept(add(callback));
	    }
	}
    }

    public static class Disjunction implements Waiting, Runnable {
	private final Waiting[] ops;
	private final Runnable callback;
	private boolean done = false, ready = false;

	public Disjunction(Runnable callback, Waitable... ops) {
	    this.callback = callback;
	    this.ops = new Waiting[ops.length];
	    for(int i = 0; i < ops.length; i++) {
		int I = i;
		ops[i].waitfor(this, wait -> this.ops[I] = wait);
	    }
	}

	public void run() {
	    boolean c = false, r = false;
	    synchronized(this) {
		if(!done) {
		    c = true;
		    done = true;
		}
		r = ready;
	    }
	    if(c) {
		cancel();
		if(r)
		    callback.run();
	    }
	}

	public void cancel() {
	    for(Waiting wait : ops) {
		if(wait != null)
		    wait.cancel();
	    }
	}
    }

    public static void or(Runnable callback, Consumer<Waiting> reg, Waitable... ops) {
	Disjunction ret = new Disjunction(callback, ops);
	synchronized(ret) {
	    if(ret.done) {
		reg.accept(Waiting.dummy);
		callback.run();
	    } else {
		reg.accept(ret);
		ret.ready = true;
	    }
	}
    }

    public abstract static class Checker implements Waiting, Runnable {
	public final Runnable callback;
	private Waiting cw;

	public Checker(Runnable callback) {
	    this.callback = callback;
	}

	protected abstract Object monitor();
	protected abstract boolean check();
	protected abstract Waiting add();

	public Checker addi() {
	    add();
	    return(this);
	}

	public void run() {
	    synchronized(monitor()) {
		if(check()) {
		    callback.run();
		    cw = null;
		} else {
		    cw = add();
		}
	    }
	}

	public void cancel() {
	    synchronized(monitor()) {
		if(cw != null) {
		    cw.cancel();
		    cw = null;
		}
	    }
	}
    }
}
