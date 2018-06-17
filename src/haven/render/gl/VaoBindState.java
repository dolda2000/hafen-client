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

public class VaoBindState extends VaoState {
    public final GLVertexArray vao;

    public VaoBindState(GLVertexArray vao) {
	this.vao = vao;
    }

    public void apply(BGL gl) {
	gl.glBindVertexArray(vao);
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
	if(that.vao != this.vao)
	    gl.glBindVertexArray(that.vao);
    }

    public static void apply(BGL gl, Applier st, GLVertexArray vao) {
	GLState cur = st.glstates[slot];
	if((cur instanceof VaoBindState) && (((VaoBindState)cur).vao == vao))
	    return;
	st.apply(gl, new VaoBindState(vao));
    }
}
