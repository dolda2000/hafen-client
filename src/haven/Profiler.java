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

public class Profiler {
    private static Loop loop;
    public final Thread th;
    private boolean enabled;

    public Profiler(Thread th) {
	this.th = th;
    }

    public Profiler() {
	this(Thread.currentThread());
    }

    public void enable() {
	if(Thread.currentThread() != th)
	    throw(new RuntimeException("Enabled from non-owning thread"));
	if(enabled)
	    throw(new RuntimeException("Enabled when already enabled"));
	if(loop == null) {
	    synchronized(Loop.class) {
		if(loop == null) {
		    loop = new Loop();
		    loop.start();
		}
	    }
	}
	synchronized(loop.current) {
	    loop.current.add(this);
	}
	enabled = true;
    }

    public void disable() {
	if(Thread.currentThread() != th)
	    throw(new RuntimeException("Disabled from non-owning thread"));
	if(!enabled)
	    throw(new RuntimeException("Disabled when already disabled"));
	synchronized(loop.current) {
	    loop.current.remove(this);
	}
	enabled = false;
    }

    protected void tick(StackTraceElement[] bt) {
    }

    private static class Loop extends HackThread {
	private Collection<Profiler> current = new LinkedList<Profiler>();

	Loop() {
	    super("Profiling thread");
	    setDaemon(true);
	}

	public void run() {
	    try {
		while(true) {
		    Thread.sleep(100);
		    Collection<Profiler> copy;
		    synchronized(current) {
			copy = new ArrayList<Profiler>(current);
		    }
		    for(Profiler p : copy) {
			StackTraceElement[] bt = p.th.getStackTrace();
			if(!p.enabled)
			    continue;
			p.tick(bt);
		    }
		}
	    } catch(InterruptedException e) {
	    }
	}
    }
}
