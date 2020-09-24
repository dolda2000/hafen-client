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

public class GLTimestamp extends GLQuery {
    public final Consumer<Long> callback;
    private int id;

    public GLTimestamp(GLEnvironment env, Consumer<Long> callback) {
	super(env);
	this.callback = callback;
    }

    public void create(GL3 gl) {
	int[] buf = {0};
	gl.glGenQueries(1, buf, 0);
	gl.glQueryCounter(buf[0], GL3.GL_TIMESTAMP);
	id = buf[0];
	env.queries.add(this);
    }

    public boolean check(GL3 gl) {
	int[] rbuf = {0};
	gl.glGetQueryObjectiv(id, GL3.GL_QUERY_RESULT_AVAILABLE, rbuf, 0);
	if(rbuf[0] == 0)
	    return(false);
	long[] tbuf = {0};
	gl.glGetQueryObjecti64v(id, GL3.GL_QUERY_RESULT, tbuf, 0);
	callback.accept(tbuf[0]);
	return(true);
    }

    public void delete(GL3 gl) {
	gl.glDeleteQueries(1, new int[] {id}, 0);
    }
}
