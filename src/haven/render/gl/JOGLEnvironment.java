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

import haven.*;
import com.jogamp.opengl.*;

public class JOGLEnvironment extends GLEnvironment {
    public final GLContext ctx;

    public JOGLEnvironment(GL3 initgl, GLContext ctx, Area wnd) {
	super(new JOGLWrap(initgl), wnd);
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
	    this.coreprof = ((JOGLWrap)gl).back.getContext().isGLCoreProfile();
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
}
