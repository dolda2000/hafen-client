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

import haven.render.sl.*;
import java.util.*;
import haven.render.VertexArray.Layout.Input;
import java.nio.ByteBuffer;

public interface InstanceBatch {
    public State.Slot<?>[] batchstates();
    public <T extends State> T batchstate(State.Slot<T> slot);
    public int instances();
    public Pipe inststate(int inst);
    public void instupdate();
    public <T extends State> void update(State.Slot<? super T> slot, T state);

    public static interface Client {
	public void iupdate(int idx);
	public void itrim(int max);
    }

    public static interface AttribState {
	public InstancedAttribute[] attribs();
    }

    public static class AttributeData implements haven.Disposable {
	public final InstanceBatch bat;
	public final Input[] fmt;
	private int bufsz;
	private VertexArray.Buffer buf = null;
	private Environment curenv;

	public AttributeData(InstanceBatch bat) {
	    ArrayList<InstancedAttribute> attribs = new ArrayList<>();
	    for(State.Slot<?> slot : bat.batchstates()) {
		State st = bat.batchstate(slot);
		if(st instanceof AttribState) {
		    for(InstancedAttribute attr : ((AttribState)st).attribs())
			attribs.add(attr);
		}
	    }
	    int[] offsets = new int[attribs.size()];
	    int tsz = 0;
	    for(int i = 0; i < offsets.length; i++) {
		InstancedAttribute attr = attribs.get(i);
		offsets[i] = tsz;
		tsz += attr.attrsize();
		tsz = (tsz + 3) & ~3;
	    }
	    Input[] fmt = new Input[attribs.size()];
	    for(int i = 0, offset = 0; i < fmt.length; i++) {
		InstancedAttribute attr = attribs.get(i);
		fmt[i] = new Input(attr, attr.attrfmt(), 0, offsets[i], tsz);
	    }
	    this.bat = bat;
	    this.fmt = fmt;

	    this.bufsz = Math.max(16, bat.instances() * 2);
	    if(stride() > 0)
		this.buf = new VertexArray.Buffer(this.bufsz * stride(), DataBuffer.Usage.STREAM, this::refill);
	}

	private FillBuffer refill(DataBuffer dst, Environment env) {
	    FillBuffer ret = env.fillbuf(dst);
	    ByteBuffer buf = ret.push();
	    int st = stride();
	    int f = 0;
	    int t = Math.min(bat.instances(), dst.size() / st);
	    for(int i = f; i < t; i++) {
		Pipe ist = bat.inststate(i);
		for(int o = 0; o < fmt.length; o++)
		    ((InstancedAttribute)fmt[o].tgt).attrfill(buf, i * st + fmt[o].offset, ist);
	    }
	    this.curenv = env;
	    return(ret);
	}

	public int stride() {
	    if(fmt.length > 0)
		return(fmt[0].stride);
	    return(0);
	}

	public boolean iupdate(int idx) {
	    if(stride() == 0)
		return(false);
	    if(idx >= this.bufsz) {
		this.bufsz = idx * 2;
		this.buf.dispose();
		this.buf = new VertexArray.Buffer(this.bufsz * stride(), DataBuffer.Usage.STREAM, this::refill);
		return(true);
	    } else {
		Environment env = this.curenv;
		if(env != null) {
		    Render r = env.render();
		    r.update(this.buf, this::refill);
		    env.submit(r);
		}
		return(false);
	    }
	}

	public void dispose() {
	    if(buf != null)
		buf.dispose();
	}
    }
}
