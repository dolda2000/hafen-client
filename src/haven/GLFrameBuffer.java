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

package haven;

import javax.media.opengl.*;

public class GLFrameBuffer extends GLState {
    public static final Slot<GLFrameBuffer> slot = new Slot<GLFrameBuffer>(Slot.Type.SYS, GLFrameBuffer.class, HavenPanel.global);
    private final TexGL[] color;
    private final TexGL depth;
    private final RenderBuffer altdepth;
    private FBO fbo;

    public static class FBO extends GLObject {
	public final int id;
	
	public FBO(GL gl) {
	    super(gl);
	    int[] buf = new int[1];
	    gl.glGenFramebuffersEXT(1, buf, 0);
	    this.id = buf[0];
	    GOut.checkerr(gl);
	}
	
	protected void delete() {
	    int[] buf = {id};
	    gl.glDeleteFramebuffersEXT(1, buf, 0);
	    GOut.checkerr(gl);
	}
    }
    
    public static class RenderBuffer {
	public final Coord sz;
	public final int fmt;
	private RBO rbo;
	
	public RenderBuffer(Coord sz, int fmt) {
	    this.sz = sz;
	    this.fmt = fmt;
	}
	
	public int glid(GL gl) {
	    if((rbo != null) && (rbo.gl != gl))
		dispose();
	    if(rbo == null) {
		rbo = new RBO(gl);
		gl.glBindRenderbufferEXT(GL.GL_RENDERBUFFER_EXT, rbo.id);
		gl.glRenderbufferStorageEXT(GL.GL_RENDERBUFFER_EXT, fmt, sz.x, sz.y);
	    }
	    return(rbo.id);
	}
	
	public void dispose() {
	    synchronized(this) {
		if(rbo != null) {
		    rbo.dispose();
		    rbo = null;
		}
	    }
	}
	
	public static class RBO extends GLObject {
	    public final int id;
	    
	    public RBO(GL gl) {
		super(gl);
		int[] buf = new int[1];
		gl.glGenRenderbuffersEXT(1, buf, 0);
		this.id = buf[0];
		GOut.checkerr(gl);
	    }
	    
	    protected void delete() {
		int[] buf = {id};
		gl.glDeleteRenderbuffersEXT(1, buf, 0);
		GOut.checkerr(gl);
	    }
	}
    }

    public GLFrameBuffer(TexGL color, TexGL depth) {
	if(color == null)
	    this.color = new TexGL[0];
	else
	    this.color = new TexGL[] {color};
	if((this.depth = depth) == null) {
	    if(this.color.length == 0)
		throw(new RuntimeException("Cannot create a framebuffer with neither color nor depth"));
	    this.altdepth = new RenderBuffer(this.color[0].tdim, GL.GL_DEPTH_COMPONENT);
	} else {
	    this.altdepth = null;
	}
    }
    
    public Coord sz() {
	/* This is not perfect, but there's no current (or probably
	 * sane) situation where it would fail. */
	if(depth != null)
	    return(depth.sz());
	else
	    return(altdepth.sz);
    }
    
    public void apply(GOut g) {
	GL gl = g.gl;
	synchronized(this) {
	    if((fbo != null) && (fbo.gl != gl))
		dispose();
	    if(fbo == null) {
		fbo = new FBO(gl);
		gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, fbo.id);
		for(int i = 0; i < color.length; i++)
		    gl.glFramebufferTexture2DEXT(GL.GL_FRAMEBUFFER_EXT, GL.GL_COLOR_ATTACHMENT0_EXT + i, GL.GL_TEXTURE_2D, color[i].glid(g), 0);
		if(depth != null)
		    gl.glFramebufferTexture2DEXT(GL.GL_FRAMEBUFFER_EXT, GL.GL_DEPTH_ATTACHMENT_EXT, GL.GL_TEXTURE_2D, depth.glid(g), 0);
		else
		    gl.glFramebufferRenderbufferEXT(GL.GL_FRAMEBUFFER_EXT, GL.GL_DEPTH_ATTACHMENT_EXT, GL.GL_RENDERBUFFER_EXT, altdepth.glid(gl));
		if(color.length == 0) {
		    gl.glDrawBuffer(GL.GL_NONE);
		    gl.glReadBuffer(GL.GL_NONE);
		}
		GOut.checkerr(gl);
		int st = gl.glCheckFramebufferStatusEXT(GL.GL_FRAMEBUFFER_EXT);
		if(st != GL.GL_FRAMEBUFFER_COMPLETE_EXT)
		    throw(new RuntimeException("FBO failed completeness test: " + st));
	    } else {
		gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, fbo.id);
		if(color.length == 0) {
		    gl.glDrawBuffer(GL.GL_NONE);
		    gl.glReadBuffer(GL.GL_NONE);
		}
	    }
	}
    }
    
    public void unapply(GOut g) {
	GL gl = g.gl;
	gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, 0);
	if(color.length == 0) {
	    gl.glDrawBuffer(GL.GL_BACK);
	    gl.glReadBuffer(GL.GL_BACK);
	}
    }

    public void prep(Buffer buf) {
	buf.put(slot, this);
    }

    public void dispose() {
	synchronized(this) {
	    if(fbo != null) {
		fbo.dispose();
		fbo = null;
	    }
	}
	if(altdepth != null)
	    altdepth.dispose();
    }
}
