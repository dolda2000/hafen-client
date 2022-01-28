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

public class VaoBindState extends VaoState {
    public static final boolean DO_GL_EBO_FIXUP = true;
    public final GLVertexArray vao;
    /* XXX? It seems that some GL implementations erroneously don't
     * track element buffers with the VAO, so do the safe thing and
     * track it explicitly. */
    public final GLBuffer ebo;

    public VaoBindState(GLVertexArray vao, GLBuffer ebo) {
	this.vao = vao;
	this.ebo = ebo;
    }

    public void apply(BGL gl) {
	gl.glBindVertexArray(vao);
	if(DO_GL_EBO_FIXUP)
	    gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, ebo);
    }

    public void unapply(BGL gl) {
	gl.glBindVertexArray(null);
    }

    public void applyto(BGL gl, GLState sthat) {
	if(!(sthat instanceof VaoBindState)) {
	    super.applyto(gl, sthat);
	    return;
	}
	VaoBindState that = (VaoBindState)sthat;
	if(that.vao != this.vao) {
	    gl.glBindVertexArray(that.vao);
	    if(DO_GL_EBO_FIXUP)
		gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, that.ebo);
	}
    }

    public static void apply(BGL gl, Applier st, GLVertexArray vao, GLBuffer ebo) {
	GLState cur = st.glstates[slot];
	if((cur instanceof VaoBindState) && (((VaoBindState)cur).vao == vao) && (((VaoBindState)cur).ebo == ebo))
	    return;
	st.apply(gl, new VaoBindState(vao, ebo));
    }
}
