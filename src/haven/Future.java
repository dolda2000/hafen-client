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

import java.util.function.*;

public interface Future<T> extends Indir<T> {
    public T get();
    public boolean done();

    public static class PastException extends RuntimeException {
	public PastException(Throwable cause) {
	    super(cause);
	}
    }

    public static class Simple<T> implements Future<T> {
	private final Waitable.Queue wq = new Waitable.Queue();
	private T val;
	private Throwable exc;
	private boolean set = false;

	public boolean done() {
	    synchronized(this) {
		return(set);
	    }
	}

	public static class NotDone extends Loading {
	    public final transient Simple future;

	    private NotDone(Simple future) {
		this.future = future;
	    }

	    public void waitfor(Runnable callback, Consumer<Waitable.Waiting> reg) {
		synchronized(future) {
		    if(future.set) {
			reg.accept(Waitable.Waiting.dummy);
			callback.run();
		    } else {
			reg.accept(future.wq.add(callback));
		    }
		}
	    }
	}

	public T get() {
	    synchronized(this) {
		if(!set)
		    throw(new NotDone(this));
		if(exc != null)
		    throw(new PastException(exc));
		return(val);
	    }
	}

	public void set(T val) {
	    synchronized(this) {
		if(this.set)
		    throw(new IllegalStateException());
		this.val = val;
		this.set = true;
		this.wq.wnotify();
	    }
	}

	public void error(Throwable cause) {
	    synchronized(this) {
		if(this.set)
		    throw(new IllegalStateException());
		this.exc = cause;
		this.set = true;
		this.wq.wnotify();
	    }
	}
    }
}
