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

import haven.Disposable;
import java.util.*;
import javax.media.opengl.*;

public abstract class GLObject implements Disposable {
    public final GLEnvironment env;
    private boolean del = false;
    private GLEnvironment.MemStats pool = null;
    private long mem;
    int dispseq;

    public GLObject(GLEnvironment env) {
	this.env = env;
    }

    public abstract void create(GL2 gl);
    protected abstract void delete(GL2 gl);

    public void dispose() {
	dispseq = env.dispseq();
	synchronized(env.disposed) {
	    if(del)
		return;
	    env.disposed.add(this);
	    del = true;
	    setmem(null, 0);
	}
    }

    protected void finalize() {
	dispose();
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
