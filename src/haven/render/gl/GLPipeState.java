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
import static haven.Utils.eq;
import haven.render.*;
import haven.render.States.*;
import com.jogamp.opengl.*;

public abstract class GLPipeState<T extends State> {
    public State.Slot<? extends T> slot;
    public abstract void apply(GLEnvironment env, BGL gl, T from, T to);

    public void apply(GLEnvironment env, BGL gl, T to) {
	apply(env, gl, null, to);
    }

    public GLPipeState(State.Slot<? extends T> slot) {
	this.slot = slot;
    }

    public static final GLPipeState<Viewport> viewport = new GLPipeState<Viewport>(States.viewport) {
	    public void apply(GLEnvironment env, BGL gl, Viewport from, Viewport to) {
		if(to != null) {
		    gl.glViewport(to.area.ul.x, to.area.ul.y, to.area.br.x - to.area.ul.x, to.area.br.y - to.area.ul.y);
		} else {
		    /* XXX? Is this even important? */
		}
	    }
	};

    public static final GLPipeState<Scissor> scissor = new GLPipeState<Scissor>(States.scissor) {
	    public void apply(GLEnvironment env, BGL gl, Scissor from, Scissor to) {
		if(to != null) {
		    gl.glScissor(to.area.ul.x, to.area.ul.y, to.area.br.x - to.area.ul.x, to.area.br.y - to.area.ul.y);
		    if(from == null)
			gl.glEnable(GL.GL_SCISSOR_TEST);
		} else {
		    gl.glDisable(GL.GL_SCISSOR_TEST);
		}
	    }
	};

    public static final GLPipeState<Facecull> facecull = new GLPipeState<Facecull>(States.facecull) {
	    private int mode(Facecull.Mode mode) {
		switch(mode) {
		    case FRONT: return(GL.GL_FRONT);
		    case BACK:  return(GL.GL_BACK);
		    case BOTH:  return(GL.GL_FRONT_AND_BACK);
		    default:    throw(new IllegalArgumentException(String.format("cull face: %s", mode)));
		}
	    }

	    public void apply(GLEnvironment env, BGL gl, Facecull from, Facecull to) {
		if(to != null) {
		    gl.glEnable(GL.GL_CULL_FACE);
		    gl.glCullFace(mode(to.mode));
		} else {
		    gl.glDisable(GL.GL_CULL_FACE);
		}
	    }
	};

    public static final GLPipeState<Depthtest> depthtest = new GLPipeState<Depthtest>(States.depthtest) {
	    private int depthfunc(Depthtest.Test test) {
		switch(test) {
		    case FALSE: return(GL.GL_NEVER);
		    case TRUE:  return(GL.GL_ALWAYS);
		    case EQ:    return(GL.GL_EQUAL);
		    case NEQ:   return(GL.GL_NOTEQUAL);
		    case LT:    return(GL.GL_LESS);
		    case LE:    return(GL.GL_LEQUAL);
		    case GT:    return(GL.GL_GREATER);
		    case GE:    return(GL.GL_GEQUAL);
		    default:    throw(new IllegalArgumentException(String.format("depth test: %s", test)));
		}
	    }

	    public void apply(GLEnvironment env, BGL gl, Depthtest from, Depthtest to) {
		if(to != null) {
		    gl.glEnable(GL.GL_DEPTH_TEST);
		    gl.glDepthFunc(depthfunc(to.test));
		} else {
		    gl.glDisable(GL.GL_DEPTH_TEST);
		}
	    }
	};

    public static final GLPipeState<State> maskdepth = new GLPipeState<State>(States.maskdepth.slot) {
	    public void apply(GLEnvironment env, BGL gl, State from, State to) {
		if(to != null)
		    gl.glDepthMask(false);
		else
		    gl.glDepthMask(true);
	    }
	};

    public static final GLPipeState<LineWidth> linewidth = new GLPipeState<LineWidth>(States.linewidth) {
	    public void apply(GLEnvironment env, BGL gl, LineWidth from, LineWidth to) {
		if(to != null) {
		    if(!eq(from, to)) {
			/* Apparently, OS X violates the specification
			 * by producing errors instead of implicitly
			 * clamping the linewidth value. */
			gl.glLineWidth(Utils.clip(to.w, env.caps.linemin, env.caps.linemax));
		    }
		} else {
		    gl.glLineWidth(1);
		}
	    }
	};

    public static final GLPipeState<DepthBias> depthbias = new GLPipeState<DepthBias>(States.depthbias) {
	    public void apply(GLEnvironment env, BGL gl, DepthBias from, DepthBias to) {
		if(to != null) {
		    if(from == null)
			gl.glEnable(GL.GL_POLYGON_OFFSET_FILL);
		    if(!eq(from, to))
			gl.glPolygonOffset(to.factor, to.units);
		} else {
		    gl.glDisable(GL.GL_POLYGON_OFFSET_FILL);
		}
	    }
	};

    public static final GLPipeState<?>[] all = {viewport, scissor, facecull, depthtest, maskdepth, linewidth, depthbias};
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
