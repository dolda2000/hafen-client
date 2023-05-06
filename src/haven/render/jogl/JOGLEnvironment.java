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

package haven.render.jogl;

import java.nio.*;
import haven.*;
import haven.render.gl.*;
import com.jogamp.opengl.*;
import haven.render.gl.GL;

public class JOGLEnvironment extends GLEnvironment {
    public final GLContext ctx;

    private static GL bestwrap(com.jogamp.opengl.GL back) {
	try {
	    return(new JOGLWrap(back.getGL3()));
	} catch(com.jogamp.opengl.GLException e) {
	    return(new JOGLWrapBackup(back));
	}
    }

    public JOGLEnvironment(com.jogamp.opengl.GL initgl, GLContext ctx, Area wnd) {
	super(bestwrap(initgl), wnd);
	if(debuglog)
	    ctx.enableGLDebugMessage(true);
	this.ctx = ctx;
	this.nilfbo_id = ctx.getDefaultDrawFramebuffer();
	this.nilfbo_db = ctx.getDefaultReadBuffer();
    }

    public static class JOGLCaps extends Caps {
	public final boolean coreprof;

	public JOGLCaps(GL gl) {
	    super(gl);
	    this.coreprof = ((WrappedJOGL)gl).getGL().getContext().isGLCoreProfile();
	}

	public void checkreq() {
	    super.checkreq();
	    if(!coreprof)
		throw(new HardwareException("Graphics context is not a core OpenGL profile.", this));
	}
    }

    public JOGLCaps mkcaps(GL initgl) {
	return(new JOGLCaps(initgl));
    }

    public SysBuffer malloc(int sz) {
	return(new JOGLBuffer(sz));
    }

    public SysBuffer subsume(ByteBuffer data, int sz) {
	if(data.remaining() < sz) {
	    String msg = data.remaining() + " < " + sz;
	    throw(new BufferUnderflowException() {
		    public String getMessage() {return(msg);}
		});
	}
	SysBuffer ret = new JOGLBuffer(data.duplicate());
	data.position(data.position() + sz);
	return(ret);
    }
}
