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

import java.io.*;
import java.lang.management.*;

public class DeadlockWatchdog extends HackThread {
    private boolean running = true;

    public DeadlockWatchdog(ThreadGroup tg) {
	super(tg, null, "Deadlock watchdog");
	setDaemon(true);
    }

    public DeadlockWatchdog() {
	this(null);
    }

    public static class ThreadState implements Serializable {
	public final String name;
	public final StackTraceElement[] trace;
	public final String[] locks;
	public final int[] lockdepth;

	public ThreadState(ThreadInfo mi) {
	    this.name = mi.getThreadName();
	    this.trace = mi.getStackTrace();
	    MonitorInfo[] mons = mi.getLockedMonitors();
	    LockInfo[] syncs = mi.getLockedSynchronizers();
	    this.locks = new String[mons.length + syncs.length];
	    this.lockdepth = new int[mons.length];
	    for(int i = 0; i < mons.length; i++) {
		locks[i] = String.valueOf(mons[i]);
		lockdepth[i] = mons[i].getLockedStackDepth();
	    }
	    for(int i = 0; i < syncs.length; i++) {
		locks[i + mons.length] = String.valueOf(syncs[i]);
	    }
	}
    }

    public static class DeadlockException extends RuntimeException {
	public final ThreadState[] threads;

	public DeadlockException(ThreadState[] threads) {
	    super("Deadlock detected");
	    this.threads = threads;
	}
    }

    protected void report(ThreadInfo[] threads) {
	ThreadState[] states = new ThreadState[threads.length];
	for(int i = 0; i < threads.length; i++)
	    states[i] = new ThreadState(threads[i]);
	throw(new DeadlockException(states));
    }

    public void run() {
	ThreadMXBean tm = ManagementFactory.getThreadMXBean();
	while(running) {
	    try {
		Thread.sleep(10000);
	    } catch(InterruptedException e) {
		continue;
	    }
	    long[] locked = tm.findDeadlockedThreads();
	    if(locked != null) {
		ThreadInfo[] threads = tm.getThreadInfo(locked, true, true);
		report(threads);
	    }
	}
    }

    public void quit() {
	running = false;
	interrupt();
    }
}
