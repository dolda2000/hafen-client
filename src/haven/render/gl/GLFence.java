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

import java.util.function.*;
import com.jogamp.opengl.*;

public class GLFence extends GLQuery {
    public final Consumer<GL3> callback;
    protected long id;

    public GLFence(GLEnvironment env, Consumer<GL3> callback) {
	super(env);
	this.callback = callback;
    }

    public void create(GL3 gl) {
	id = gl.glFenceSync(GL3.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
	env.queries.add(this);
    }

    public boolean check(GL3 gl) {
	int[] vbuf = {0};
	gl.glGetSynciv(id, GL3.GL_SYNC_STATUS, 1, null, 0, vbuf, 0);
	if(vbuf[0] != GL3.GL_SIGNALED)
	    return(false);
	callback.accept(gl);
	return(true);
    }

    public void delete(GL3 gl) {
	gl.glDeleteSync(id);
    }
}
