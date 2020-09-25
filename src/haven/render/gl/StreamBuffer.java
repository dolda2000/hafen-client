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
import java.nio.*;
import com.jogamp.opengl.*;
import haven.render.*;

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
    private ByteBuffer[] xfbufs = {};
    private boolean[] used = {};

    public StreamBuffer(GLEnvironment env, int size) {
	this.rbuf = new GLBuffer(env);
	this.size = size;
    }

    private ByteBuffer mkbuf() {
	return(ByteBuffer.allocate(size).order(ByteOrder.nativeOrder()));
    }

    public ByteBuffer get() {
	synchronized(this) {
	    for(int i = 0; i < xfbufs.length; i++) {
		if(!used[i]) {
		    if(xfbufs[i] == null)
			xfbufs[i] = mkbuf();
		    xfbufs[i].rewind();
		    used[i] = true;
		    return(xfbufs[i]);
		}
	    }
	    int n = xfbufs.length;
	    xfbufs = Arrays.copyOf(xfbufs, Math.max(1, n * 2));
	    used = Arrays.copyOf(used, Math.max(1, n * 2));
	    xfbufs[n] = mkbuf();
	    used[n] = true;
	    return(xfbufs[n]);
	}
    }

    public void put(ByteBuffer buf) {
	if(buf == null) throw(new NullPointerException());
	synchronized(this) {
	    for(int i = 0; i < xfbufs.length; i++) {
		if(xfbufs[i] == buf) {
		    if(!used[i])
			throw(new RuntimeException());
		    used[i] = false;
		}
	    }
	}
    }

    public void put(BGL gl, ByteBuffer buf) {
	if(buf == null) throw(new NullPointerException());
	gl.bglSubmit(new BGL.Request() {
		public void run(GL3 gl) {put(buf);}
		public void abort() {put(buf);}
	    });
    }

    public class Fill implements FillBuffer {
	public ByteBuffer data;

	public Fill() {
	    data = StreamBuffer.this.get();
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
	    synchronized(this) {
		ByteBuffer ret = this.data;
		this.data = null;
		ret.rewind();
		return(ret);
	    }
	}

	public void dispose() {
	    synchronized(this) {
		if(data != null) {
		    put(data);
		    data = null;
		}
	    }
	}

	protected void finalize() {dispose();}
    }

    public void dispose() {
	rbuf.dispose();
    }
}
