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

import haven.Coord;
import haven.render.*;
import java.util.*;

public class GLFrameBuffer extends GLObject implements BGL.ID {
    public final Attachment[] color;
    public final Attachment depth;
    public final Coord sz;
    private int id;

    public GLFrameBuffer(GLEnvironment env, Attachment[] color, Attachment depth) {
	super(env);
	if(color.length > 0) {
	    sz = color[0].sz();
	    if((depth != null) && !sz.equals(depth.sz()))
		throw(new IllegalArgumentException(String.format("Framebuffer attachments have differing sizes: color[0]=%s, depth=%s", sz, depth.sz())));
	    for(int i = 1; i < color.length; i++) {
		if(!sz.equals(color[i].sz()))
		    throw(new IllegalArgumentException(String.format("Framebuffer attachments have differing sizes: color[0]=%s, color[i]=%s", sz, color[i].sz())));
	    }
	} else if(depth != null) {
	    this.sz = depth.sz();
	} else {
	    this.sz = null;
	}
	this.color = color;
	this.depth = depth;
	env.prepare(this);
	env.prepare((GLRender r) -> {
		r.state.apply(r.gl, new FboState(env, this, new int[0], null));
		BGL gl = r.gl();
		for(int i = 0; i < GLFrameBuffer.this.color.length; i++)
		    GLFrameBuffer.this.color[i].attach(gl, this, GL.GL_COLOR_ATTACHMENT0 + i);
		if(GLFrameBuffer.this.depth != null)
		    GLFrameBuffer.this.depth.attach(gl, this, GL.GL_DEPTH_ATTACHMENT);
		gl.bglSubmit(rgl -> {
			int st = rgl.glCheckFramebufferStatus(GL.GL_FRAMEBUFFER);
			if(st != GL.GL_FRAMEBUFFER_COMPLETE)
			    throw(new FormatException("FBO failed completeness test: " + GLException.constname(st), GLFrameBuffer.this));
		    });
	    });
	register();
    }
	
    public void create(GL gl) {
	int[] buf = new int[1];
	gl.glGenFramebuffers(1, buf);
	GLException.checkfor(gl, env);
	this.id = buf[0];
	setmem(GLEnvironment.MemStats.FBOS, 0);
    }
	
    protected void delete(GL gl) {
	gl.glDeleteFramebuffers(1, new int[] {id});
	setmem(null, 0);
    }
	
    public int glid() {
	return(id);
    }

    private void register(GLTexture tex) {
	synchronized(tex) {
	    if(tex.fbos == null)
		tex.fbos = new ArrayList<>();
	    tex.fbos.add(this);
	}
    }
    private void register() {
	if(depth != null)
	    register(depth.tex);
	for(Attachment c : color)
	    register(c.tex);
    }

    private void unregister(GLTexture tex) {
	synchronized(tex) {
	    if((tex.fbos == null) || !tex.fbos.remove(this))
		throw(new RuntimeException(String.format("FBO oddly not registered with texture %s", tex)));
	}
    }
    public void dispose() {
	if(depth != null)
	    unregister(depth.tex);
	for(Attachment c : color)
	    unregister(c.tex);
	super.dispose();
    }

    public static class FormatException extends RuntimeException {
	public final Coord sz;
	public final VectorFormat[] cfmt;
	public final VectorFormat dfmt;

	public FormatException(String message, GLFrameBuffer fbo) {
	    super(message);
	    this.sz = fbo.sz;
	    cfmt = new VectorFormat[fbo.color.length];
	    for(int i = 0; i < cfmt.length; i++) {
		if(fbo.color[i] instanceof Attach2D) {
		    Texture desc = fbo.color[i].tex.desc();
		    if(desc != null)
			cfmt[i] = desc.ifmt;
		}
	    }
	    if(fbo.depth instanceof Attach2D) {
		Texture desc = fbo.depth.tex.desc();
		dfmt = (desc == null) ? null : desc.ifmt;
	    } else {
		dfmt = null;
	    }
	}
    }

    public static abstract class Attachment {
	public final GLTexture tex;

	public Attachment(GLTexture tex) {this.tex = tex;}

	public abstract void attach(BGL gl, GLFrameBuffer fbo, int point);
	public abstract Coord sz();
    }

    public static class Attach2D extends Attachment {
	public final int level, w, h;

	public Attach2D(GLTexture.Tex2D tex, Texture.Image<Texture2D> img) {
	    super(tex);
	    this.level = img.level;
	    this.w = img.w;
	    this.h = img.h;
	}

	public void attach(BGL gl, GLFrameBuffer fbo, int point) {
	    gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, point, GL.GL_TEXTURE_2D, tex, level);
	}
	public Coord sz() {
	    return(new Coord(w, h));
	}

	public int hashCode() {
	    return((System.identityHashCode(tex) * 31) + level);
	}
	public boolean equals(Object o) {
	    if(!(o instanceof Attach2D)) return(false);
	    Attach2D that = (Attach2D)o;
	    return((this.tex == that.tex) && (this.level == that.level));
	}
    }

    @SuppressWarnings("unchecked")
    public static Attachment prepimg(GLEnvironment env, Texture.Image<?> img) {
	if(img.tex instanceof Texture2D)
	    return(new Attach2D(env.prepare((Texture2D)img.tex), (Texture.Image<Texture2D>)img));
	throw(new IllegalArgumentException(String.format("Unsupported image type for framebuffer attachment: %s", img)));
    }
}
