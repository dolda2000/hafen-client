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

import java.nio.*;
import haven.render.*;
import haven.Disposable;

public class FillBuffers {
    public static class Array implements FillBuffer {
	private final GLEnvironment env;
	private final int sz;
	private boolean pushed = false;
	private SysBuffer mem = null;

	public Array(GLEnvironment env, int sz) {
	    this.env = env;
	    this.sz = sz;
	}

	public int size() {return(sz);}
	public boolean compatible(Environment env) {return(env instanceof GLEnvironment);}

	public ByteBuffer push() {
	    if(mem == null) {
		mem = env.malloc(sz);
		pushed = true;
	    } else if(!pushed) {
		throw(new IllegalStateException("already pulled"));
	    }
	    return(mem.data());
	}

	public void pull(ByteBuffer buf) {
	    if(mem != null)
		throw(new IllegalStateException("already " + (pushed ? "pushed" : "pulled")));
	    mem = env.subsume(buf, sz);
	}

	public SysBuffer mem() {
	    if(mem == null)
		push();
	    return(mem);
	}

	public ByteBuffer data() {
	    ByteBuffer data = mem().data();
	    if(pushed)
		data.rewind();
	    return(data);
	}

	public void dispose() {
	    if(mem != null)
		mem.dispose();
	}
    }
}
