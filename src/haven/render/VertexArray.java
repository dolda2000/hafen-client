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
import java.nio.ByteBuffer;
import haven.render.sl.Attribute;
import haven.Disposable;

public class VertexArray implements Disposable {
    public final Layout fmt;
    public final Buffer[] bufs;
    public boolean shared = false;
    public Disposable ro;

    public VertexArray(Layout fmt, Buffer... bufs) {
	if(bufs.length != fmt.nbufs)
	    throw(new IllegalArgumentException(String.format("Vertex layout requires %d buffers, only given %d", fmt.nbufs, bufs.length)));
	this.fmt = fmt;
	this.bufs = bufs;
    }

    public int num() {
	int ret = -1;
	for(int i = 0; i < fmt.inputs.length; i++) {
	    Layout.Input inp = fmt.inputs[i];
	    if(bufs[inp.buf].size < (inp.offset + inp.el.size()))
		return(0);
	    int n = 1 + ((bufs[inp.buf].size - (inp.offset + inp.el.size())) / ((inp.stride != 0) ? inp.stride : inp.el.size()));
	    if((ret < 0) || (n < ret))
		ret = n;
	}
	return(ret);
    }

    public static class Layout {
	public final Input[] inputs;
	public final int nbufs;

	public Layout(Input... inputs) {
	    int mb = 0;
	    for(Input in : inputs)
		mb = Math.max(mb, in.buf);
	    int nb = mb + 1;
	    boolean[] used = new boolean[nb];
	    for(Input in : inputs)
		used[in.buf] = true;
	    for(boolean u : used) {
		if(!u)
		    throw(new RuntimeException("Vertex buffers are not tightly packed"));
	    }
	    Arrays.sort(inputs, (a, b) -> {
		    int c;
		    if((c = a.buf - b.buf) != 0)
			return(c);
		    if((c = a.stride - b.stride) != 0)
			return(c);
		    if((c = a.offset - b.offset) != 0)
			return(c);
		    return(0);
		});
	    this.inputs = inputs;
	    this.nbufs = nb;
	}

	public static class Input {
	    public final Attribute tgt;
	    public final VectorFormat el;
	    public final int buf, offset, stride;
	    public final boolean instanced;

	    public Input(Attribute tgt, VectorFormat el, int buf, int offset, int stride, boolean instanced) {
		this.tgt = tgt;
		this.el = el;
		this.buf = buf;
		this.offset = offset;
		this.stride = stride;
		this.instanced = instanced;
	    }

	    public Input(Attribute tgt, VectorFormat el, int buf, int offset, int stride) {
		this(tgt, el, buf, offset, stride, false);
	    }

	    public int hashCode() {
		int ret = System.identityHashCode(tgt);
		ret = (ret * 31) + el.hashCode();
		ret = (ret * 31) + buf;
		ret = (ret * 31) + offset;
		ret = (ret * 31) + stride;
		return(ret);
	    }

	    public boolean equals(Object o) {
		if(!(o instanceof Input))
		    return(false);
		Input that = (Input)o;
		return((this.buf == that.buf) && (this.offset == that.offset) && (this.stride == that.stride) &&
		       (this.tgt == that.tgt) && this.el.equals(that.el));
	    }

	    public String toString() {
		return(String.format("#<input %s(%s = %sx%s) %d(%d@%d)>", tgt.name, tgt.type, el.cf, el.nc, buf, stride, offset));
	    }
	}

	public int hashCode() {
	    int ret = 0;
	    for(Input in : inputs)
		ret = (ret * 31) + in.hashCode();
	    return(ret);
	}

	public boolean equals(Object o) {
	    if(!(o instanceof Layout))
		return(false);
	    Layout that = (Layout)o;
	    if(this.inputs.length != that.inputs.length)
		return(false);
	    for(int i = 0; i < this.inputs.length; i++) {
		if(!this.inputs[i].equals(that.inputs[i]))
		    return(false);
	    }
	    return(true);
	}

	public String toString() {
	    return(Arrays.asList(inputs).toString());
	}
    }

    public static class Buffer implements DataBuffer, Disposable {
	public final int size;
	public final Usage usage;
	public final Filler<? super Buffer> init;
	public boolean shared = false;
	public Disposable ro;

	public Buffer(int size, Usage usage, Filler<? super Buffer> init) {
	    this.size = size;
	    this.usage = usage;
	    this.init = init;
	}

	public Buffer(ByteBuffer data, Usage usage) {
	    this(data.remaining(), usage, Filler.of(data));
	}

	public int size() {
	    return(size);
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
