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

import haven.render.*;
import javax.media.opengl.GL;

public abstract class GLPipeState<T extends State> {
    public State.Slot<? extends T> slot;
    public abstract void apply(BGL gl, T from, T to);

    public GLPipeState(State.Slot<? extends T> slot) {
	this.slot = slot;
    }

    public static final GLPipeState<States.Viewport> viewport = new GLPipeState<States.Viewport>(States.viewport) {
	    public void apply(BGL gl, States.Viewport from, States.Viewport to) {
		if(to != null) {
		    gl.glViewport(to.area.ul.x, to.area.ul.y, to.area.br.x - to.area.ul.x, to.area.br.y - to.area.ul.y);
		} else {
		    /* XXX? Is this even important? */
		}
	    }
	};

    public static final GLPipeState<States.Scissor> scissor = new GLPipeState<States.Scissor>(States.scissor) {
	    public void apply(BGL gl, States.Scissor from, States.Scissor to) {
		if(to != null) {
		    gl.glScissor(to.area.ul.x, to.area.ul.y, to.area.br.x - to.area.ul.x, to.area.br.y - to.area.ul.y);
		    if(from == null)
			gl.glEnable(GL.GL_SCISSOR_TEST);
		} else {
		    gl.glDisable(GL.GL_SCISSOR_TEST);
		}
	    }
	};

    public static final GLPipeState<State> depthtest = new GLPipeState<State>(States.depthtest.slot) {
	    public void apply(BGL gl, State from, State to) {
		if(to != null)
		    gl.glEnable(GL.GL_DEPTH_TEST);
		else
		    gl.glDisable(GL.GL_DEPTH_TEST);
	    }
	};

    public static final GLPipeState<?>[] all = {viewport, scissor, depthtest};
    public static final GLPipeState<?>[] matching;
    static {
	int max = all[0].slot.id;
	for(int i = 1; i < all.length; i++)
	    max = Math.max(max, all[i].slot.id);
	matching = new GLPipeState<?>[max + 1];
	for(GLPipeState<?> st : all)
	    matching[st.slot.id] = st;
    }
}
