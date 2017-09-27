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

package haven.render;

import java.util.*;
import haven.render.sl.Attribute;
import haven.Disposable;

public class VertexArray implements Disposable {
    public final Buffer[] bufs;
    public final int n;
    public boolean shared = false;
    public Disposable ro;

    public VertexArray(Collection<Buffer> bufs) {
	Buffer[] na = new Buffer[bufs.size()];
	Iterator<Buffer> bi = bufs.iterator();
	na[0] = bi.next();
	int num = na[0].n;
	for(int i = 1; bi.hasNext(); i++) {
	    na[i] = bi.next();
	    if(na[i].n != num)
		throw(new IllegalArgumentException("Buffer sizes do not match"));
	}
	this.bufs = na;
	this.n = num;
    }

    public VertexArray(Buffer... bufs) {
	this(Arrays.asList(bufs));
    }

    public static class Buffer implements DataBuffer, Disposable {
	public final int n, nc;
	public final NumberFormat fmt;
	public final Usage usage;
	public final Attribute tgt;
	public final Filler<? super Buffer> init;
	public boolean shared = false;
	public Disposable ro;

	public Buffer(int n, int nc, NumberFormat fmt, Usage usage, Attribute tgt, Filler<? super Buffer> init) {
	    this.n = n;
	    this.nc = nc;
	    this.fmt = fmt;
	    this.usage = usage;
	    this.tgt = tgt;
	    this.init = init;
	}

	public int size() {
	    return(n * nc * fmt.size);
	}

	public Buffer shared() {
	    this.shared = true;
	    return(this);
	}

	public void dispose() {
	    synchronized(this) {
		if(ro != null) {
		    ro.dispose();
		    ro = null;
		}
	    }
	}
    }

    public VertexArray shared() {
	this.shared = true;
	return(this);
    }

    public void dispose() {
	synchronized(this) {
	    if(ro != null) {
		ro.dispose();
		ro = null;
	    }
	}
	for(Buffer buf : bufs) {
	    if(!buf.shared)
		buf.dispose();
	}
    }
}
