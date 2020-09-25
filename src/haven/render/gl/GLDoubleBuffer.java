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
import com.jogamp.opengl.*;

public class GLDoubleBuffer {
    private List<Buffered> changed = null;
    private int prevsz = 16;

    public class Buffered implements BGL.Request {
	private BufferBGL cur, next;

	public void run(GL3 gl) {
	    if(cur != null)
		cur.run(gl);
	}

	public void abort() {
	    cur.abort();
	}

	public void update(BufferBGL gl) {
	    if(gl == null)
		throw(new NullPointerException());
	    if(this.cur == null) {
		this.cur = gl;
	    } else {
		synchronized(GLDoubleBuffer.this) {
		    if(changed == null) {
			this.cur = gl;
		    } else {
			if(this.next == null)
			    changed.add(this);
			this.next = gl;
		    }
		}
	    }
	}
    }

    public boolean get(long timeout) throws InterruptedException {
	synchronized(this) {
	    if(timeout == 0) {
		while(changed != null)
		    wait();
	    } else {
		long now = System.currentTimeMillis(), start = now;
		while(changed != null) {
		    if(now > start + timeout)
			return(false);
		    wait(start + timeout - now);
		    now = System.currentTimeMillis();
		}
	    }
	    changed = new ArrayList<Buffered>(prevsz * 2);
	}
	return(true);
    }

    public void put() {
	synchronized(this) {
	    if(changed != null) {
		for(Buffered req : changed) {
		    if(req.next != null) {
			req.cur = req.next;
			req.next = null;
		    }
		}
		prevsz = Math.max(changed.size(), 16);
		changed = null;
		notifyAll();
	    }
	}
    }

    public void put(BGL gl) {
	gl.bglSubmit(new BGL.Request() {
		public void run(GL3 gl) {put();}
		public void abort() {put();}
	    });
    }
}
