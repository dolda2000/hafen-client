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

public class Defer extends ThreadGroup {
    private static final Map<ThreadGroup, Defer> groups = new WeakHashMap<ThreadGroup, Defer>();
    private final Queue<Future<?>> queue = new PrioQueue<Future<?>>();
    private final Collection<Thread> pool = new LinkedList<Thread>();
    private final int maxthreads = 2;
    
    public interface Callable<T> {
	public T call() throws InterruptedException;
    }

    public static class CancelledException extends RuntimeException {
	public CancelledException() {
	    super("Execution cancelled");
	}
	
	public CancelledException(Throwable cause) {
	    super(cause);
	}
    }

    public static class DeferredException extends RuntimeException {
	public DeferredException(Throwable cause) {
	    super(cause);
	}
    }

    public static class NotDoneException extends Loading {
    }

    public class Future<T> implements Runnable, Prioritized {
	public final Callable<T> task;
	private int prio = 0;
	private T val;
	private volatile String state = "";
	private RuntimeException exc = null;
	private Thread running = null;
	
	private Future(Callable<T> task) {
	    this.task = task;
	}

	public void cancel() {
	    synchronized(this) {
		if(running != null) {
		    running.interrupt();
		} else {
		    exc = new CancelledException();
		    state = "done";
		}
	    }
	}
	
	public void run() {
	    synchronized(this) {
		if(state == "done")
		    return;
		running = Thread.currentThread();
	    }
	    try {
		val = task.call();
		state = "done";
	    } catch(InterruptedException exc) {
		this.exc = new CancelledException(exc);
		state = "done";
	    } catch(Loading exc) {
	    } catch(RuntimeException exc) {
		this.exc = exc;
		state = "done";
	    } finally {
		if(state != "done")
		    state = "resched";
		running = null;
	    }
	}
	
	public T get() {
	    synchronized(this) {
		boostprio(5);
		if(state == "done") {
		    if(exc != null)
			throw(new DeferredException(exc));
		    return(val);
		}
		if(state == "resched") {
		    defer(this);
		    state = "";
		}
		throw(new NotDoneException());
	    }
	}
	
	public boolean done() {
	    synchronized(this) {
		boostprio(5);
		if(state == "resched") {
		    defer(this);
		    state = "";
		}
		return(state == "done");
	    }
	}
	
	public int priority() {
	    return(prio);
	}
	
	public void boostprio(int prio) {
	    synchronized(this) {
		if(this.prio < prio)
		    this.prio = prio;
	    }
	}
    }

    private class Worker extends HackThread {
	private Worker() {
	    super(Defer.this, null, "Worker thread");
	    setDaemon(true);
	}
	
	public void run() {
	    try {
		while(true) {
		    Future<?> f;
		    try {
			long start = System.currentTimeMillis();
			synchronized(queue) {
			    while((f = queue.poll()) == null) {
				if(System.currentTimeMillis() - start > 5000)
				    return;
				queue.wait(1000);
			    }
			}
		    } catch(InterruptedException e) {
			return;
		    }
		    f.run();
		    f = null;
		}
	    } finally {
		synchronized(queue) {
		    pool.remove(this);
		    if((pool.size() < 1) && !queue.isEmpty()) {
			Thread n = new Worker();
			n.start();
			pool.add(n);
		    }
		}
	    }
	}
    }

    public Defer(ThreadGroup parent) {
	super(parent, "DPC threads");
    }
    
    private void defer(Future<?> f) {
	synchronized(queue) {
	    boolean e = queue.isEmpty();
	    queue.add(f);
	    queue.notify();
	    if((pool.isEmpty() || !e) && (pool.size() < maxthreads)) {
		Thread n = new Worker();
		n.start();
		pool.add(n);
	    }
	}
    }

    public <T> Future<T> defer(Callable<T> task) {
	Future<T> f = new Future<T>(task);
	defer(f);
	return(f);
    }
    
    public static <T> Future<T> later(Callable<T> task) {
	ThreadGroup tg = Thread.currentThread().getThreadGroup();
	if(tg instanceof Defer)
	    return(((Defer)tg).defer(task));
	Defer d;
	synchronized(groups) {
	    if((d = groups.get(tg)) == null)
		groups.put(tg, d = new Defer(tg));
	}
	return(d.defer(task));
    }
}
