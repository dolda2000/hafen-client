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
import javax.media.opengl.GL;

public class FboState extends GLState {
    public final GLFrameBuffer fbo;
    public final int[] dbufs;

    public FboState(GLFrameBuffer fbo, int[] dbufs) {
	this.fbo = fbo;
	this.dbufs = dbufs;
    }

    public void applydbufs(BGL gl) {
	if(dbufs != null) {
	    if(dbufs.length == 1)
		gl.glDrawBuffer(dbufs[0]);
	    else
		gl.glDrawBuffers(dbufs.length, dbufs, 0);
	}
    }

    public void apply(BGL gl) {
	if(fbo != null)
	    gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fbo);
	applydbufs(gl);
    }

    public void unapply(BGL gl) {
	gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, null);
	gl.glDrawBuffer(GL.GL_BACK);
    }

    public void applyto(BGL gl, GLState to) {
	FboState that = (FboState)to;
	if(this.fbo != that.fbo)
	    gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, that.fbo);
	that.applydbufs(gl);
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

    private static FboState forfvals(GLEnvironment env, Object depthp, Object[] fvalsp) {
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
	search: for(int i = 0; i < fvalsp.length; i++) {
	    if(fvalsp[i] == null) {
		dbufs[i] = GL.GL_NONE;
	    } else {
		for(int o = 0; o < color.length; o++) {
		    if(color[o] == fvalsp[i]) {
			dbufs[i] = GL.GL_COLOR_ATTACHMENT0 + o;
			continue search;
		    }
		}
		throw(new RuntimeException());
	    }
	}
	return(new FboState(find(env, color, depth), dbufs));
    }

    public static FboState make(GLEnvironment env, Object depth, Object[] fvals) {
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
	    return(forfvals(env, depth, fvals));
	} else if(def) {
	    if(depth == null)
		throw(new IllegalArgumentException("The default OpenGL framebuffer cannot be depth-less"));
	    int[] dbufs = new int[fvals.length];
	    for(int i = 0; i < fvals.length; i++) {
		if(fvals[i] == null)
		    dbufs[i] = GL.GL_NONE;
		else
		    dbufs[i] = GL.GL_BACK;
	    }
	    return(new FboState(null, dbufs));
	} else {
	    throw(new IllegalArgumentException(String.format("Illegal framebuffer configuration: depth=%s, colors=%s", depth, Arrays.asList(fvals))));
	}
    }

    public static void set(BGL gl, Applier st, Object depth, Object[] fvals) {
	st.apply(gl, make(st.env, depth, fvals));
    }

    public static int slot = slotidx(FboState.class);
    public int slotidx() {return(slot);}
}
