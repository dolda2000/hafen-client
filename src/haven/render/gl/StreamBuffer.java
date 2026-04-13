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

import java.util.*;
import java.util.function.*;
import java.nio.*;
import haven.render.*;
import haven.Finalizer;

/*
 * This buffer exist only to conserve allocation bandwidth, by keeping
 * persistently allocated transfer buffers. In an ideal world where
 * allocation bandwidth doesn't matter, this should be utterly
 * unnecessary, but between specific performance problems with the CMS
 * GC and the general performance characteristics of G1, there seems
 * to be a need to optimize this particular metric. Updates to
 * streaming buffers then being a very major contributor to allocation
 * bandwidth, there seems to be a necessity to jump through this hoop
 * in order to make the JVM happy.
 */
public class StreamBuffer implements haven.Disposable {
    public final GLBuffer rbuf;
    public final int size;
    private final Pool pool;

    public StreamBuffer(GLEnvironment env, int size) {
	this.rbuf = new GLBuffer(env);
	this.size = size;
	this.pool = new Pool(size, sz -> env.malloc(sz));
    }

    public ByteBuffer get() {return(pool.get());}
    public void put(ByteBuffer buf) {pool.put(buf);}

    public void put(BGL gl, ByteBuffer buf) {
	if(buf == null) throw(new NullPointerException());
	gl.bglSubmit(new BGL.Request() {
		public void run(GL gl) {put(buf);}
		public void abort() {put(buf);}
	    });
    }

    public class Fill implements FillBuffer {
	public final ByteBuffer data;
	private final boolean[] clear;
	private final Runnable clean;

	public Fill() {
	    boolean[] clear = this.clear = new boolean[] {false};
	    StreamBuffer bref = StreamBuffer.this;
	    ByteBuffer data = this.data = bref.get();
	    clean = Finalizer.finalize(this, () -> {
		    synchronized(clear) {
			if(!clear[0]) {
			    bref.put(data);
			    clear[0] = true;
			}
		    }
		});
	}

	public int size() {return(size);}
	public boolean compatible(Environment env) {return(env == rbuf.env);}

	public ByteBuffer push() {
	    return(data);
	}

	public void pull(ByteBuffer buf) {
	    data.put(buf);
	}

	ByteBuffer get() {
	    synchronized(clear) {
		ByteBuffer ret = this.data;
		clear[0] = true;
		((Buffer)ret).rewind();
		return(ret);
	    }
	}

	public void dispose() {
	    clean.run();
	}
    }

    public void dispose() {
	rbuf.dispose();
	pool.dispose();
    }

    /* Pool of fixed-size SysBuffers, reused across get/put cycles to
     * conserve allocation bandwidth. Extracted from StreamBuffer so the
     * reuse semantics can be unit-tested without a live GL context. */
    static final class Pool implements haven.Disposable {
	private final int size;
	private final IntFunction<SysBuffer> alloc;
	private SysBuffer[] bufs = {};
	private boolean[] used = {};

	Pool(int size, IntFunction<SysBuffer> alloc) {
	    this.size = size;
	    this.alloc = alloc;
	}

	synchronized ByteBuffer get() {
	    for(int i = 0; i < bufs.length; i++) {
		if(!used[i]) {
		    if(bufs[i] == null)
			bufs[i] = alloc.apply(size);
		    ByteBuffer ret = bufs[i].data();
		    ((Buffer)ret).rewind();
		    used[i] = true;
		    return(ret);
		}
	    }
	    int n = bufs.length;
	    bufs = Arrays.copyOf(bufs, Math.max(1, n * 2));
	    used = Arrays.copyOf(used, Math.max(1, n * 2));
	    bufs[n] = alloc.apply(size);
	    used[n] = true;
	    return(bufs[n].data());
	}

	synchronized void put(ByteBuffer buf) {
	    if(buf == null) throw(new NullPointerException());
	    for(int i = 0; i < bufs.length; i++) {
		if((bufs[i] != null) && (bufs[i].data() == buf)) {
		    if(!used[i])
			throw(new RuntimeException());
		    used[i] = false;
		}
	    }
	}

	public synchronized void dispose() {
	    for(int i = 0; i < bufs.length; i++) {
		if(bufs[i] != null) {
		    bufs[i].dispose();
		    bufs[i] = null;
		}
	    }
	}

	/* Test-only accessors. */
	synchronized int allocated() {
	    int n = 0;
	    for(int i = 0; i < bufs.length; i++)
		if(bufs[i] != null) n++;
	    return(n);
	}
    }
}
