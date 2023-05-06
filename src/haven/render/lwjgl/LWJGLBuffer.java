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

package haven.render.lwjgl;

import java.nio.*;
import haven.*;
import haven.render.gl.*;
import org.lwjgl.system.*;

public class LWJGLBuffer extends GLObject implements SysBuffer {
    public static final boolean LEAK_CAUSE = false;
    private ByteBuffer data;
    private final Cleanup clean;

    private static class Cleanup implements Finalizer.Cleaner, Disposable {
	private final Throwable init = LEAK_CAUSE ? new Throwable() : null;
	private final ByteBuffer data;
	private final Runnable fin;
	private boolean clean;

	Cleanup(LWJGLBuffer ob) {
	    this.data = ob.data;
	    fin = Finalizer.finalize(ob, this);
	}

	public void clean() {
	    if(!clean)
		new Warning(init , "LWJGL buffer leaked (" + data.capacity() + " bytes)").issue();
	    MemoryUtil.memFree(data);
	}

	public void dispose() {
	    clean = true;
	    fin.run();
	}
    }

    public LWJGLBuffer(LWJGLEnvironment env, int sz) {
	super(env);
	data = MemoryUtil.memAlloc(sz);
	clean = new Cleanup(this);
    }

    public ByteBuffer data() {
	if(data == null)
	    throw(new IllegalStateException("already disposed"));
	return(data);
    }

    public void create(GL gl) {throw(new UnsupportedOperationException());}

    protected void delete(GL gl) {
	if(data == null)
	    throw(new IllegalStateException("already disposed"));
	data = null;
	clean.dispose();
    }
}
