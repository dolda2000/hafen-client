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

import com.jogamp.opengl.*;

public class Vao0State extends VaoState {
    public final GLEnvironment env;
    public final BGL.ID[] enable;
    public final boolean[] instanced;
    public final GLBuffer ebo;
    private final GLVertexArray vao0;

    public Vao0State(GLEnvironment env, BGL.ID[] enable, boolean[] instanced, GLBuffer ebo) {
	this.env = env;
	this.enable = enable;
	this.instanced = instanced;
	this.ebo = ebo;
	this.vao0 = env.tempvao.get();
    }

    public void apply(BGL gl) {
	gl.glBindVertexArray(vao0);
	for(int i = 0; i < enable.length; i++) {
	    gl.glEnableVertexAttribArray(enable[i]);
	    if(instanced[i])
		gl.glVertexAttribDivisor(enable[i], 1);
	}
	gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, ebo);
    }

    public void unapply(BGL gl) {
	for(int i = 0; i < enable.length; i++) {
	    gl.glDisableVertexAttribArray(enable[i]);
	    if(instanced[i])
		gl.glVertexAttribDivisor(enable[i], 0);
	}
	gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, null);
	gl.glBindVertexArray(null);
    }

    public void applyto(BGL gl, GLState sthat) {
	if(!(sthat instanceof Vao0State)) {
	    super.applyto(gl, sthat);
	    return;
	}
	Vao0State that = (Vao0State)sthat;
	found: for(int i1 = 0; i1 < this.enable.length; i1++) {
	    for(int i2 = 0; i2 < that.enable.length; i2++) {
		if(that.enable[i2] == this.enable[i1]) {
		    if(that.instanced[i2] && !this.instanced[i1])
			gl.glVertexAttribDivisor(this.enable[i1], 1);
		    else if(!that.instanced[i2] && this.instanced[i1])
			gl.glVertexAttribDivisor(this.enable[i1], 0);
		    continue found;
		}
	    }
	    gl.glDisableVertexAttribArray(this.enable[i1]);
	    if(this.instanced[i1])
		gl.glVertexAttribDivisor(this.enable[i1], 0);
	}
	found: for(int i1 = 0; i1 < that.enable.length; i1++) {
	    for(int i2 = 0; i2 < this.enable.length; i2++) {
		if(this.enable[i2] == that.enable[i1]) {
		    if(that.instanced[i1] && !this.instanced[i2])
			gl.glVertexAttribDivisor(that.enable[i1], 1);
		    else if(!that.instanced[i1] && this.instanced[i2])
			gl.glVertexAttribDivisor(that.enable[i1], 0);
		    continue found;
		}
	    }
	    gl.glEnableVertexAttribArray(that.enable[i1]);
	    if(that.instanced[i1])
		gl.glVertexAttribDivisor(that.enable[i1], 0);
	}
	if(that.ebo != this.ebo)
	    gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, that.ebo);
    }

    public static void apply(GLEnvironment env, BGL gl, Applier st, BGL.ID[] enable, boolean[] instanced) {
	GLState cur = st.glstates[slot];
	GLBuffer ebo = null;
	eq: if(cur instanceof Vao0State) {
	    Vao0State that = (Vao0State)cur;
	    ebo = that.ebo;
	    if(that.enable.length == enable.length) {
		for(int i = 0; i < enable.length; i++) {
		    if((enable[i] != that.enable[i]) || (instanced[i] != that.instanced[i]))
			break eq;
		}
		return;
	    }
	}
	st.apply(gl, new Vao0State(env, enable, instanced, ebo));
    }

    private static final BGL.ID[] nilen = {};
    private static final boolean[] nilinst = {};
    public static void apply(GLEnvironment env, BGL gl, Applier st, GLBuffer ebo) {
	GLState cur = st.glstates[slot];
	BGL.ID[] enable = nilen;
	boolean[] instanced = nilinst;
	if(cur instanceof Vao0State) {
	    Vao0State that = (Vao0State)cur;
	    if(that.ebo == ebo)
		return;
	    enable = that.enable;
	    instanced = that.instanced;
	}
	st.apply(gl, new Vao0State(env, enable, instanced, ebo));
    }
}
