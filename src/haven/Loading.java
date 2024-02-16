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

import java.util.function.Consumer;
import java.util.function.Supplier;

public class Loading extends RuntimeException implements Waitable {
    public final Loading rec;

    public Loading() {
	super();
	rec = null;
    }

    public Loading(String msg) {
	super(msg);
	rec = null;
    }
    
    public Loading(Throwable cause) {
	super(cause);
	rec = null;
    }
    
    public Loading(String msg, Throwable cause) {
	super(msg, cause);
	rec = null;
    }

    public Loading(Loading rec) {
	super(rec);
	this.rec = rec;
    }

    public Loading(String msg, Loading rec) {
	super(msg, rec);
	this.rec = rec;
    }

    public String getMessage() {
	if(rec != null)
	    return(rec.getMessage());
	return(super.getMessage());
    }

    public static class UnwaitableEvent extends RuntimeException {
	public final Loading event;

	public UnwaitableEvent(String message, Loading event) {
	    super(message);
	    this.event = event;
	}

	public UnwaitableEvent(Loading event) {
	    this("Tried to wait for unwaitable event", event);
	}
    }

    public void waitfor(Runnable callback, Consumer<Waitable.Waiting> reg) {
	throw(new UnwaitableEvent(this));
    }

    public boolean boostprio(int prio) {
	return(false);
    }

    private void queuewait() throws InterruptedException {
	boolean[] buf = {false};
	Waitable.Waiting[] wbuf = {null};
	waitfor(() -> {
		synchronized(buf) {
		    buf[0] = true;
		    buf.notifyAll();
		}
	    },
	    wait -> wbuf[0] = wait);
	try {
	    synchronized(buf) {
		while(!buf[0])
		    buf.wait();
	    }
	} finally {
	    wbuf[0].cancel();
	}
    }

    public void waitfor() throws InterruptedException {
	if(rec != null) {
	    rec.waitfor();
	    return;
	} else {
	    queuewait();
	}
    }

    public static <T> T waitforint(Indir<T> x) throws InterruptedException {
	while(true) {
	    try {
		return(x.get());
	    } catch(Loading l) {
		l.boostprio(10);
		l.waitfor();
	    }
	}
    }

    public static <T> T waitfor(Indir<T> x) {
	boolean intd = false;
	try {
	    while(true) {
		try {
		    return(waitforint(x));
		} catch(InterruptedException e) {
		    intd = true;
		}
	    }
	} finally {
	    if(intd)
		Thread.currentThread().interrupt();
	}
    }

    public static <T> T or(Supplier<T> x, T def) {
	try {
	    return(x.get());
	} catch(Loading l) {
	    return(def);
	}
    }

    public static <T> T or(Supplier<T> x, Supplier<T> def) {
	try {
	    return(x.get());
	} catch(Loading l) {
	    return(def.get());
	}
    }
}
