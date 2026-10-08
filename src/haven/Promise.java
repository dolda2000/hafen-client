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

public class Promise<T> {
    private final Checker<T> check = new Checker<>(this);
    private T value;
    private Throwable error;

    private static class Checker<T> implements Finalizer.Cleaner {
	private static final boolean DEBUG = true;
	final Throwable created = DEBUG ? new Throwable("created at") : null;
	boolean settled = false;
	Callback<T> callback = null;

	Checker(Promise<T> p) {
	    Finalizer.finalize(p, this);
	}

	public void clean() {
	    try {
		if(!settled) {
		    if(callback != null) {
			callback.callback(null, new RuntimeException("promise never settled", created));
		    }  else {
			throw(new RuntimeException("promise never settled", created));
		    }
		}
	    } catch(Throwable t) {
		Thread.UncaughtExceptionHandler h = Thread.currentThread().getUncaughtExceptionHandler();
		if(h == null)
		    throw(t);
		else
		    h.uncaughtException(Thread.currentThread(), t);
	    }
	}
    }

    private static class Callback<T> {
	private final Consumer<? super T> resolve;
	private final Consumer<? super Throwable> reject;
	private final Runnable finish;
	private boolean called;

	Callback(Consumer<? super T> resolve, Consumer<? super Throwable> reject, Runnable finish) {
	    if(resolve == null) throw(new NullPointerException());
	    if(reject  == null) throw(new NullPointerException());
	    this.resolve = resolve;
	    this.reject = reject;
	    this.finish = finish;
	}

	void callback(T val, Throwable err) {
	    synchronized(this) {
		if(called)
		    throw(new IllegalStateException());
		called = true;
	    }
	    try {
		if(err != null)
		    reject.accept(err);
		else
		    resolve.accept(val);
	    } finally {
		if(finish != null)
		    finish.run();
	    }
	}
    }

    private void settle(T value, Throwable error) {
	Callback<T> callback;
	synchronized(this) {
	    if(check.settled)
		throw(new IllegalStateException());
	    this.value = value;
	    this.error = error;
	    check.settled = true;
	    callback = check.callback;
	}
	if(callback != null) {
	    callback.callback(value, error);
	}
    }

    public Promise<T> resolve(T value) {
	settle(value, null);
	return(this);
    }

    public Promise<T> reject(Throwable error) {
	settle(null, error);
	return(this);
    }

    public static <T> Promise<T> of(Supplier<T> from) {
	T val;
	try {
	    val = from.get();
	} catch(Throwable t) {
	    return(new Promise<T>().reject(t));
	}
	return(new Promise<T>().resolve(val));
    }

    public void callback(Callback<T> callback) {
	boolean done;;
	synchronized(this) {
	    if(check.callback != null)
		throw(new IllegalStateException());
	    check.callback = callback;
	    done = check.settled;
	}
	if(done)
	    check.callback.callback(value, error);
    }
    public void callback(Consumer<? super T> res, Consumer<? super Throwable> rej, Runnable fin) {
	callback(new Callback<>(res, rej, fin));
    }

    private Callback<T> forwarder() {
	return(new Callback<>(this::resolve, this::reject, null));
    }

    private static <V, N> Promise<N> produce(Function<V, Promise<N>> raw, V val) {
	try {
	    Promise<N> ret = raw.apply(val);
	    return((ret != null) ? ret : new Promise<N>().resolve(null));
	} catch(Throwable t) {
	    return(new Promise<N>().reject(t));
	}
    }

    private static <V, N> Promise<N> reproduce(Function<? super Throwable, Promise<N>> raw, Throwable err) {
	if(raw == null)
	    return(new Promise<N>().reject(err));
	return(produce(raw, err));
    }

    public <N> Promise<N> then(Function<? super T, Promise<N>> res, Function<? super Throwable, Promise<N>> rej, Runnable fin) {
	Promise<N> ret = new Promise<>();
	callback(val ->   produce(res, val).callback(ret.forwarder()),
		 err -> reproduce(rej, err).callback(ret.forwarder()),
		 fin);
	return(ret);
    }

    public <N> Promise<N> then(Function<? super T, Promise<N>> res, Function<? super Throwable, Promise<N>> rej) {
	return(then(res, rej, null));
    }

    public <N> Promise<N> then(Function<? super T, Promise<N>> res) {
	return(then(res, null));
    }

    /*
    private <V> void rejecting(Function<V, ? extends T> raw, V val) {
	try {
	    resolve(raw.apply(val));
	} catch(Throwable t) {
	    reject(t);
	}
    }

    public void rejector(Function<? super Throwable, ? extends T> raw, Throwable err) {
	if(raw == null)
	    reject(err);
	rejecting(raw, err);
    }

    public <N> Promise<N> map(Function<? super T, ? extends N> res, Function<? super Throwable, ? extends N> rej, Runnable fin) {
	Promise<N> ret = new Promise<>();
	callback(val -> ret.rejecting(res, val),
		 err -> ret. rejector(rej, err),
		 fin);
	return(ret);
    }
    */

    public static <V, N> Function<V, Promise<N>> mapper(Function<? super V, ? extends N> raw) {
	if(raw == null) return(null);
	return(rval -> {
	    N nval;
	    try {
		nval = raw.apply(rval);
	    } catch(Throwable t) {
		return(new Promise<N>().reject(t));
	    }
	    return(new Promise<N>().resolve(nval));
	});
    }

    public static <V, R> Function<V, R> fnonnull(Function<V, R> raw) {
	return(val -> (val == null) ? null : raw.apply(val));
    }

    public static <V, R> Function<V, R> fnonnull(Function<V, R> raw, V def) {
	return(val -> raw.apply((val == null) ? def : val));
    }

    public static <V, R> Function<V, R> fnonnull(Function<V, R> raw, Supplier<V> def) {
	return(val -> raw.apply((val == null) ? def.get() : val));
    }

    public static <V> Consumer<V> cnonnull(Consumer<V> raw) {
	return(val -> {if(val != null) raw.accept(val);});
    }

    public static <V> Consumer<V> cnonnull(Consumer<V> raw, V def) {
	return(val -> raw.accept((val == null) ? def : val));
    }

    public static <V> Consumer<V> cnonnull(Consumer<V> raw, Supplier<V> def) {
	return(val -> raw.accept((val == null) ? def.get() : val));
    }

    public <N> Promise<N> map(Function<? super T, ? extends N> res, Function<? super Throwable, ? extends N> rej, Runnable fin) {
	return(then(mapper(res), mapper(rej), fin));
    }

    public <N> Promise<N> map(Function<? super T, ? extends N> res, Function<? super Throwable, ? extends N> rej) {
	return(map(res, rej, null));
    }

    public <N> Promise<N> map(Function<? super T, ? extends N> res) {
	return(map(res, null));
    }

    public Promise<?> consume(Consumer<? super T> res) {
	return(map(val -> {res.accept(val); return(null);}));
    }

    public Promise<?> tee(Consumer<? super T> res) {
	return(map(val -> {res.accept(val); return(val);}));
    }

    public Promise<T> except(Function<? super Throwable, ? extends T> rej) {
	return(map(val -> val, rej));
    }

    public Promise<T> except(Consumer<? super Throwable> rej) {
	return(map(val -> val, err -> {rej.accept(err); return(null);}));
    }

    @SuppressWarnings("unchecked")
    public T waitfor(boolean interruptible) throws InterruptedException {
	Object[] buf = {null, null};
	callback(val -> {
	    synchronized(buf) {
		buf[0] = val;
		buf[1] = true;
		buf.notifyAll();
	    }
	}, err -> {
	    synchronized(buf) {
		buf[0] = null;
		buf[1] = err;
		buf.notifyAll();
	    }
	}, null);
	synchronized(buf) {
	    boolean irq = false;
	    while(buf[1] == null) {
		try {
		    buf.wait();
		} catch(InterruptedException e) {
		    if(interruptible)
			throw(e);
		    else
			irq = true;
		}
	    }
	    if(irq)
		Thread.currentThread().interrupt();
	    if(buf[1] instanceof Throwable)
		throw(new Future.PastException((Throwable)buf[1]));
	    return((T)buf[0]);
	}
    }

    public T waitfor() {
	try {
	    return(waitfor(false));
	} catch(InterruptedException e) {
	    throw(new AssertionError(e));
	}
    }

    public Promise<T> defer() {
	Promise<T> ret = new Promise<>();
	callback(val -> Defer.later(() -> ret.resolve(val), null),
		 err -> Defer.later(() -> ret.reject(err), null),
		 null);
	return(ret);
    }

    public void report(UI ui, String prefix) {
	except(err -> {
	    String msg = err.getMessage();
	    if(prefix != null)
		msg = prefix + ": " + msg;
	    synchronized(ui) {
		ui.error(msg);
	    }
	});
    }

    public void report(UI ui) {
	report(ui, null);
    }

    public void warn(String message) {
	except(err -> {new Warning(err, message).issue();});
    }

    public static <T> Promise<T> deferred(Supplier<T> task, Consumer<Runnable> enqueue) {
	Promise<T> ret = new Promise<>();
	enqueue.accept(() -> {
	    T val;
	    try {
		val = task.get();
	    } catch(Throwable t) {
		ret.reject(t);
		return;
	    }
	    ret.resolve(val);
	});
	return(ret);
    }

    public static <T> Promise<List<T>> collect(List<Promise<? extends T>> parts) {
	int n = parts.size();
	List<T> buf = new ArrayList<>();
	int[] remaining = {n};
	Promise<List<T>> ret = new Promise<>();
	for(int i : Utils.range(n)) {
	    buf.add(null);
	    parts.get(i).callback(val -> {
		buf.set(i, val);
		synchronized(remaining) {
		    if(--remaining[0] == 0)
			ret.resolve(buf);
		}
	    }, err -> {
		synchronized(remaining) {
		    remaining[0] = -1;
		}
		ret.reject(err);
	    }, null);
	}
	return(ret);
    }
}
