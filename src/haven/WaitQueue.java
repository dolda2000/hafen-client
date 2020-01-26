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

public class WaitQueue {
    private Collection<Waiter> waiters = null;

    public static interface Waiting {
	public void cancel();

	public static Waiting dummy = new Waiting() {
		public void cancel() {}
	    };
    }

    private class Waiter implements Waiting {
	final Runnable callback;

	Waiter(Runnable callback) {
	    this.callback = callback;
	}

	public void cancel() {
	    synchronized(WaitQueue.this) {
		if(waiters != null)
		    waiters.remove(this);
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

    public void wnotify() {
	synchronized(this) {
	    Collection<Waiter> list = waiters;
	    if(list != null) {
		waiters = null;
		for(Waiter w : list)
		    w.callback.run();
	    }
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
}
