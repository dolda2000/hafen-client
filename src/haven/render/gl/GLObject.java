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

package haven.render.gl;

import haven.*;
import java.util.*;

public abstract class GLObject implements Disposable {
    public static final boolean LEAK_CHECK = true;
    public final GLEnvironment env;
    private boolean del = false, disp = false;
    private GLEnvironment.MemStats pool = null;
    private long mem;
    private int rc = 0;
    int dispseq;

    public GLObject(GLEnvironment env) {
	this.env = env;
    }

    public abstract void create(GL gl);
    public void abortcreate() {}
    protected abstract void delete(GL gl);

    protected boolean leakcheck() {return(true);}

    private final Disposable lck = (LEAK_CHECK && leakcheck()) ? Finalizer.leakcheck(this) : null;
    protected void dispose0() {
	if(lck != null)
	    lck.dispose();
	synchronized(env.disposed) {
	    if(del)
		return;
	    dispseq = env.dispseq();
	    env.disposed.add(this);
	    del = true;
	}
    }

    public Throwable disptrace = null;
    public void dispose() {
	synchronized(this) {
	    disp = true;
	    if(disptrace == null)
		disptrace = new Throwable();
	    if(rc == 0)
		dispose0();
	}
    }

    public static class UseAfterFreeException extends RuntimeException {
	public UseAfterFreeException(Throwable cause) {
	    super("already disposed", cause);
	}
    }

    private static final java.util.concurrent.atomic.AtomicInteger ar = new java.util.concurrent.atomic.AtomicInteger(0);
    void get() {
	synchronized(this) {
	    if(del)
		throw(new UseAfterFreeException(disptrace));
	    rc++;
	    int na = ar.incrementAndGet();
	    // System.err.printf("%d ", na);
	}
    }

    void put() {
	synchronized(this) {
	    rc--;
	    if(rc < 0)
		throw(new AssertionError("rc < 0"));
	    else if((rc == 0) && disp)
		dispose0();
	    int na = ar.decrementAndGet();
	    // System.err.printf("%d ", na);
	}
    }

    protected void ckstate(int st, int ex) {
	if(st != ex)
	    throw(new IllegalStateException(String.format("unexpected state %d, expected %d, for %s", st, ex, this)));
    }

    protected void setmem(GLEnvironment.MemStats pool, long mem) {
	synchronized(env.stats_obj) {
	    if(this.pool != null) {
		env.stats_obj[this.pool.ordinal()]--;
		env.stats_mem[this.pool.ordinal()] -= this.mem;
		this.pool = null;
		this.mem = 0;
	    }
	    if(pool != null) {
		env.stats_obj[pool.ordinal()]++;
		env.stats_mem[pool.ordinal()] += mem;
		this.pool = pool;
		this.mem = mem;
	    }
	}
    }
}
