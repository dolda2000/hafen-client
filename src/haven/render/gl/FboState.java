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

import static haven.Utils.eq;
import haven.render.*;
import haven.render.gl.GLFrameBuffer.*;
import java.util.*;
import com.jogamp.opengl.*;

public class FboState extends GLState {
    public static final FragTarget NIL_CONF = new FragTarget(null);
    public static final boolean[] BLEND_ALL = new boolean[0], BLEND_NONE = new boolean[0];
    public static final boolean[] MASK_NONE = new boolean[0];
    public final GLEnvironment env;
    public final GLFrameBuffer fbo;
    public final int[] dbufs;
    public final BlendMode blend;
    public final boolean[] blendbufs;
    public final boolean[] colormask;

    public FboState(GLEnvironment env, GLFrameBuffer fbo, int[] dbufs, FragTarget[] conf) {
	this.env = env;
	this.fbo = fbo;
	this.dbufs = dbufs;
	if(conf != null) {
	    int n = conf.length;
	    BlendMode blend = null;
	    boolean blendall = true, masknone = true;
	    boolean blendbufs[] = new boolean[n];
	    boolean colormask[] = new boolean[n * 4];
	    for(int i = 0; i < n; i++) {
		if(conf[i].blend != null) {
		    if(blend == null)
			blend = conf[i].blend;
		    else if(!blend.equals(conf[i].blend))
			throw(new NotImplemented("OpenGL 3.0 does not support separate blend equations"));
		    blendbufs[i] = true;
		} else {
		    blendall = false;
		}
		for(int o = 0, off = i * 4; o < 4; o++, off++)
		    masknone &= !(colormask[off] = conf[i].mask[o]);
	    }
	    this.blend = blend;
	    if(blendall)
		this.blendbufs = BLEND_ALL;
	    else if(blend == null)
		this.blendbufs = BLEND_NONE;
	    else
		this.blendbufs = blendbufs;
	    if(masknone)
		this.colormask = MASK_NONE;
	    else
		this.colormask = colormask;
	} else {
	    this.blend = null;
	    this.blendbufs = BLEND_NONE;
	    this.colormask = MASK_NONE;
	}
    }

    public void applydbufs(BGL gl) {
	if(dbufs != null) {
	    if(dbufs.length == 0)
		gl.glDrawBuffer(GL.GL_NONE);
	    else if(dbufs.length == 1)
		gl.glDrawBuffer(dbufs[0]);
	    else
		gl.glDrawBuffers(dbufs.length, dbufs, 0);
	}
	/* Just set any valid read-buffer for completeness
	 * checking. GLRender switches to the correct read-buffer
	 * when needed. */
	if(fbo == null) {
	    if(env.nilfbo_id == 0)
		gl.glReadBuffer(GL.GL_BACK);
	    else
		gl.glReadBuffer(env.nilfbo_db);
	} else if(fbo.color.length == 0) {
	    gl.glReadBuffer(GL.GL_NONE);
	} else {
	    gl.glReadBuffer(GL.GL_COLOR_ATTACHMENT0);
	}
    }

    public static int glblendfunc(BlendMode.Function fn) {
	switch(fn) {
	case ADD: return(GL.GL_FUNC_ADD);
	case SUB: return(GL.GL_FUNC_SUBTRACT);
	case RSUB: return(GL.GL_FUNC_REVERSE_SUBTRACT);
	case MIN: return(GL3.GL_MIN);
	case MAX: return(GL3.GL_MAX);
	default: throw(new IllegalArgumentException(String.format("blend function: %s", fn)));
	}
    }

    public static int glblendfac(BlendMode.Factor fac) {
	switch(fac) {
	case ZERO: return(GL.GL_ZERO);
	case ONE: return(GL.GL_ONE);
	case SRC_COLOR: return(GL.GL_SRC_COLOR);
	case DST_COLOR: return(GL.GL_DST_COLOR);
	case INV_SRC_COLOR: return(GL.GL_ONE_MINUS_SRC_COLOR);
	case INV_DST_COLOR: return(GL.GL_ONE_MINUS_DST_COLOR);
	case SRC_ALPHA: return(GL.GL_SRC_ALPHA);
	case DST_ALPHA: return(GL.GL_DST_ALPHA);
	case INV_SRC_ALPHA: return(GL.GL_ONE_MINUS_SRC_ALPHA);
	case INV_DST_ALPHA: return(GL.GL_ONE_MINUS_DST_ALPHA);
	case CONST_COLOR: return(GL3.GL_CONSTANT_COLOR);
	case INV_CONST_COLOR: return(GL3.GL_ONE_MINUS_CONSTANT_COLOR);
	case CONST_ALPHA: return(GL3.GL_CONSTANT_ALPHA);
	case INV_CONST_ALPHA: return(GL3.GL_ONE_MINUS_CONSTANT_ALPHA);
	default: throw(new IllegalArgumentException(String.format("blend factor: %s", fac)));
	}
    }

    public void applyconf(BGL gl) {
	if(blend == null) {
	    gl.glDisable(GL.GL_BLEND);
	} else {
	    if(blendbufs == BLEND_ALL) {
		gl.glEnable(GL.GL_BLEND);
	    } else {
		for(int i = 0; i < blendbufs.length; i++) {
		    if(blendbufs[i])
			gl.glEnablei(GL.GL_BLEND, i);
		    else
			gl.glDisablei(GL.GL_BLEND, i);
		}
	    }
	    if(blend.cfn == blend.afn)
		gl.glBlendEquation(glblendfunc(blend.cfn));
	    else
		gl.glBlendEquationSeparate(glblendfunc(blend.cfn), glblendfunc(blend.afn));
	    if((blend.csrc == blend.asrc) && (blend.cdst == blend.adst))
		gl.glBlendFunc(glblendfac(blend.csrc), glblendfac(blend.cdst));
	    else
		gl.glBlendFuncSeparate(glblendfac(blend.csrc), glblendfac(blend.cdst), glblendfac(blend.asrc), glblendfac(blend.adst));
	    if(blend.color != null)
		gl.glBlendColor(blend.color.r, blend.color.g, blend.color.b, blend.color.a);
	}
	if(colormask == MASK_NONE) {
	    gl.glColorMask(true, true, true, true);
	} else {
	    for(int i = 0, b = 0; i < colormask.length; i += 4, b++)
		gl.glColorMaski(b, !colormask[i + 0], !colormask[i + 1], !colormask[i + 2], !colormask[i + 3]);
	}
    }

    public void apply(BGL gl) {
	gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fbo);
	applydbufs(gl);
	applyconf(gl);
    }

    public void unapply(BGL gl) {
	gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, null);
	if(env.nilfbo_id == 0)
	    gl.glDrawBuffer(GL.GL_BACK);
	else
	    gl.glDrawBuffer(env.nilfbo_db);
    }

    public void applyto(BGL gl, GLState to) {
	FboState that = (FboState)to;
	if(this.fbo != that.fbo)
	    gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, that.fbo);
	that.applydbufs(gl);
	that.applyconf(gl);
    }

    private static boolean compatiblep(GLFrameBuffer fbo, Attachment[] color, Attachment depth) {
	if(!eq(depth, fbo.depth))
	    return(false);
	search: for(Attachment s : color) {
	    for(Attachment h : fbo.color) {
		if(eq(h, s))
		    continue search;
	    }
	    return(false);
	}
	return(true);
    }

    private static GLFrameBuffer find(GLEnvironment env, Attachment[] color, Attachment depth) {
	GLTexture from;
	if(depth != null)
	    from = depth.tex;
	else if(color.length > 0)
	    from = color[0].tex;
	else
	    throw(new NotImplemented("empty framebuffer"));
	synchronized(from) {
	    if(from.fbos != null) {
		for(GLFrameBuffer fbo : from.fbos) {
		    if(compatiblep(fbo, color, depth))
			return(fbo);
		}
	    }
	}
	return(new GLFrameBuffer(env, color, depth));
    }

    private static FboState forfvals(GLEnvironment env, Object depthp, Object[] fvalsp, FragTarget[] conf) {
	Attachment depth = (Attachment)depthp;
	Attachment[] color = new Attachment[fvalsp.length];
	int nc = 0;
	intern: for(int i = 0; i < fvalsp.length; i++) {
	    if(fvalsp[i] == null)
		continue;
	    Attachment c = (Attachment)fvalsp[i];
	    for(int o = 0; (o < color.length) && (color[o] != null); o++) {
		if(color[o] == c)
		    continue intern;
	    }
	    color[nc++] = c;
	}
	color = Arrays.copyOf(color, nc);
	int[] dbufs = new int[fvalsp.length];
	GLFrameBuffer fbo = find(env, color, depth);
	search: for(int i = 0; i < fvalsp.length; i++) {
	    if(fvalsp[i] == null) {
		dbufs[i] = GL.GL_NONE;
	    } else {
		for(int o = 0; o < fbo.color.length; o++) {
		    if(eq(fbo.color[o], fvalsp[i])) {
			dbufs[i] = GL.GL_COLOR_ATTACHMENT0 + o;
			continue search;
		    }
		}
		throw(new RuntimeException());
	    }
	}
	return(new FboState(env, fbo, dbufs, conf));
    }

    public static FboState make(GLEnvironment env, Object depth, Object[] fvals, FragTarget[] conf) {
	boolean any = false, img = true, def = true;
	if(depth != null) {
	    any = true;
	    if(!(depth instanceof Attachment))
		img = false;
	    if(depth != DepthBuffer.defdepth)
		def = false;
	}
	for(int i = 0; i < fvals.length; i++) {
	    if(fvals[i] != null) {
		any = true;
		if(!(fvals[i] instanceof Attachment))
		    img = false;
		if(fvals[i] != FragColor.defcolor)
		    def = false;
	    }
	}
	if(!any) {
	    throw(new NotImplemented("empty framebuffer"));
	} else if(img) {
	    return(forfvals(env, depth, fvals, conf));
	} else if(def) {
	    if(depth == null)
		throw(new IllegalArgumentException("The default OpenGL framebuffer cannot be depth-less"));
	    int[] dbufs = new int[fvals.length];
	    for(int i = 0; i < fvals.length; i++) {
		if(fvals[i] == null) {
		    dbufs[i] = GL.GL_NONE;
		} else {
		    if(env.nilfbo_id == 0)
			dbufs[i] = GL.GL_BACK;
		    else
			dbufs[i] = env.nilfbo_db;
		}
	    }
	    return(new FboState(env, null, dbufs, conf));
	} else {
	    throw(new IllegalArgumentException(String.format("Illegal framebuffer configuration: depth=%s, colors=%s", depth, Arrays.asList(fvals))));
	}
    }

    public static void set(BGL gl, Applier st, Object depth, Object[] fvals, FragTarget[] conf) {
	st.apply(gl, make(st.env, depth, fvals, conf));
    }

    public static int slot = slotidx(FboState.class);
    public int slotidx() {return(slot);}
}
