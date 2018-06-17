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

import javax.media.opengl.*;

public class Vao0State extends VaoState {
    public final GLProgram.VarID[] enable;
    public final GLBuffer ebo;

    public Vao0State(GLProgram.VarID[] enable, GLBuffer ebo) {
	this.enable = enable;
	this.ebo = ebo;
    }

    public void apply(BGL gl) {
	for(GLProgram.VarID attr : enable)
	    gl.glEnableVertexAttribArray(attr);
	gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, ebo);
    }

    public void unapply(BGL gl) {
	for(GLProgram.VarID attr : enable)
	    gl.glDisableVertexAttribArray(attr);
	gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, null);
    }

    public void applyto(BGL gl, GLState sthat) {
	if(!(sthat instanceof Vao0State)) {
	    super.applyto(gl, sthat);
	    return;
	}
	Vao0State that = (Vao0State)sthat;
	found: for(GLProgram.VarID a1 : this.enable) {
	    for(GLProgram.VarID a2 : that.enable) {
		if(a2 == a1)
		    continue found;
	    }
	    gl.glDisableVertexAttribArray(a1);
	}
	found: for(GLProgram.VarID a1 : that.enable) {
	    for(GLProgram.VarID a2 : this.enable) {
		if(a2 == a1)
		    continue found;
	    }
	    gl.glEnableVertexAttribArray(a1);
	}
	if(that.ebo != this.ebo)
	    gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, that.ebo);
    }

    public static void apply(BGL gl, Applier st, GLProgram.VarID[] enable) {
	GLState cur = st.glstates[slot];
	GLBuffer ebo = null;
	eq: if(cur instanceof Vao0State) {
	    Vao0State that = (Vao0State)cur;
	    ebo = that.ebo;
	    if(that.enable.length == enable.length) {
		for(int i = 0; i < enable.length; i++) {
		    if(enable[i] != that.enable[i])
			break eq;
		}
		return;
	    }
	}
	st.apply(gl, new Vao0State(enable, ebo));
    }

    public static void apply(BGL gl, Applier st, GLBuffer ebo) {
	GLState cur = st.glstates[slot];
	GLProgram.VarID[] enable = null;
	if(cur instanceof Vao0State) {
	    Vao0State that = (Vao0State)cur;
	    if(that.ebo == ebo)
		return;
	    enable = that.enable;
	}
	st.apply(gl, new Vao0State((enable == null) ? new GLProgram.VarID[0] : enable, ebo));
    }
}
