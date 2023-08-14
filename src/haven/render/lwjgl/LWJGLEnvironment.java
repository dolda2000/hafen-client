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
import org.lwjgl.opengl.awt.*;
import haven.render.gl.GL;

public class LWJGLEnvironment extends GLEnvironment {
    public final GLData effdata;

    public LWJGLEnvironment(GLData effdata, Area wnd) {
	super(LWJGLWrap.instance, wnd);
	this.effdata = effdata;
    }

    public static class LWJGLCaps extends Caps {
	public final boolean coreprof;

	public LWJGLCaps(GL gl, LWJGLEnvironment env) {
	    super(gl);
	    this.coreprof = true;
	    // this.coreprof = (env.effdata.profile == GLData.Profile.CORE);
	}

	public void checkreq() {
	    super.checkreq();
	    if(!coreprof || ((major < 3) || ((major == 3) && (minor < 2))))
		throw(new HardwareException("Graphics context is not a core OpenGL profile.", this));
	}
    }

    public LWJGLCaps mkcaps(GL initgl) {
	return(new LWJGLCaps(initgl, this));
    }

    public SysBuffer malloc(int sz) {
	return(new LWJGLBuffer(this, sz));
    }

    public SysBuffer subsume(ByteBuffer data, int sz) {
	SysBuffer ret = new LWJGLBuffer(this, sz);
	ByteBuffer cp = ret.data();
	cp.put(data);
	cp.rewind();
	return(ret);
    }
}
