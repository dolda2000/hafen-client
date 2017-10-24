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
import haven.render.Texture.Image;
import haven.render.Texture.Sampler;
import haven.render.Texture2D.Sampler2D;
import java.nio.*;
import javax.media.opengl.*;

public abstract class GLTexture extends GLObject implements BGL.ID {
    protected int id;

    public GLTexture(GLEnvironment env) {
	super(env);
	env.prepare(this);
    }

    public void create(GL2 gl) {
	int[] buf = {0};
	gl.glGenTextures(1, buf, 0);
	this.id = buf[0];
    }

    protected void delete(BGL gl) {
	BGL.ID[] buf = {this};
	gl.glDeleteTextures(1, buf, 0);
    }

    public int glid() {
	return(id);
    }

    public abstract void bind(BGL gl);
    public abstract void unbind(BGL gl);

    static int magfilter(Sampler smp) {
	switch(smp.magfilter) {
	case NEAREST: return(GL.GL_NEAREST);
	case LINEAR:  return(GL.GL_LINEAR);
	default: throw(new IllegalArgumentException(String.format("%s.magfilter: %s", smp, smp.magfilter)));
	}
    }
    static int minfilter(Sampler smp) {
	if(smp.mipfilter == null) {
	    switch(smp.minfilter) {
	    case NEAREST: return(GL.GL_NEAREST);
	    case LINEAR:  return(GL.GL_LINEAR);
	    default: throw(new IllegalArgumentException(String.format("%s.minfilter: %s", smp, smp.minfilter)));
	    }
	}
	switch(smp.mipfilter) {
	case NEAREST:
	    switch(smp.minfilter) {
	    case NEAREST: return(GL.GL_NEAREST_MIPMAP_NEAREST);
	    case LINEAR:  return(GL.GL_LINEAR_MIPMAP_NEAREST);
	    default: throw(new IllegalArgumentException(String.format("%s.minfilter: %s", smp, smp.minfilter)));
	    }
	case LINEAR:
	    switch(smp.minfilter) {
	    case NEAREST: return(GL.GL_NEAREST_MIPMAP_LINEAR);
	    case LINEAR:  return(GL.GL_LINEAR_MIPMAP_LINEAR);
	    default: throw(new IllegalArgumentException(String.format("%s.minfilter: %s", smp, smp.minfilter)));
	    }
	default: throw(new IllegalArgumentException(String.format("%s.mipfilter: %s", smp, smp.mipfilter)));
	}
    }
    static int wrapmode(Texture.Wrapping w) {
	switch(w) {
	case REPEAT: return(GL.GL_REPEAT);
	case REPEAT_MIRROR: return(GL.GL_MIRRORED_REPEAT);
	case CLAMP: return(GL.GL_CLAMP_TO_EDGE);
	case CLAMP_BORDER: return(GL2.GL_CLAMP_TO_BORDER);
	default: throw(new IllegalArgumentException(String.format("wrapmode: %s", w)));
	}
    }

    static int texifmt(VectorFormat fmt) {
	switch(fmt.nc) {
	case 1:
	    switch(fmt.cf) {
	    case UNORM8: return(GL2.GL_R8);
	    case SNORM8: return(GL2.GL_R8_SNORM);
	    case FLOAT16: return(GL2.GL_R16F);
	    }
	case 2:
	    switch(fmt.cf) {
	    case UNORM8: return(GL2.GL_RG8);
	    case SNORM8: return(GL2.GL_RG8_SNORM);
	    case FLOAT16: return(GL2.GL_RG16F);
	    }
	case 3:
	    switch(fmt.cf) {
	    case UNORM8: return(GL2.GL_RGB8);
	    case SNORM8: return(GL2.GL_RGB8_SNORM);
	    case FLOAT16: return(GL2.GL_RGB16F);
	    }
	case 4:
	    switch(fmt.cf) {
	    case UNORM8: return(GL2.GL_RGBA8);
	    case SNORM8: return(GL2.GL_RGBA8_SNORM);
	    case FLOAT16: return(GL2.GL_RGBA16F);
	    }
	}
	throw(new IllegalArgumentException(String.format("internalformat: %s", fmt)));
    }

    static int texefmt1(VectorFormat fmt) {
	switch(fmt.nc) {
	case 1: return(GL2.GL_RED);
	case 2: return(GL2.GL_RG);
	case 3: return(GL2.GL_RGB);
	case 4: return(GL2.GL_RGBA);
	}
	throw(new IllegalArgumentException(String.format("externalformat1: %s", fmt)));
    }

    static int texefmt2(VectorFormat fmt) {
	switch(fmt.cf) {
	case UNORM8: return(GL2.GL_UNSIGNED_BYTE);
	case SNORM8: return(GL2.GL_BYTE);
	case UNORM16: return(GL2.GL_UNSIGNED_SHORT);
	case SNORM16: return(GL2.GL_SHORT);
	case UNORM32: return(GL2.GL_UNSIGNED_INT);
	case SNORM32: return(GL2.GL_INT);
	case FLOAT32: return(GL2.GL_FLOAT);
	}
	throw(new IllegalArgumentException(String.format("externalformat1: %s", fmt)));
    }

    public static class Tex2D extends GLTexture {
	public final Sampler2D data;
	Sampler2D sampler;

	public Tex2D(GLEnvironment env, Sampler2D data, FillBuffers.Array[] pixels) {
	    super(env);
	    this.data = data;
	    this.sampler = data;
	    int ifmt = texifmt(data.tex.ifmt);
	    int pfmt = texefmt1(data.tex.efmt);
	    int pnum = texefmt2(data.tex.efmt);
	    env.prepare((GLRender g) -> {
		    g.state.apply(g.gl, new TexState(new GLTexture[] {Tex2D.this}, 0));
		    BGL gl = g.gl();
		    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, magfilter(data));
		    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, minfilter(data));
		    if(data.anisotropy > 0)
			gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAX_ANISOTROPY_EXT, data.anisotropy);
		    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, wrapmode(data.swrap));
		    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, wrapmode(data.twrap));
		    gl.glTexParameterfv(GL.GL_TEXTURE_2D, GL2.GL_TEXTURE_BORDER_COLOR, data.border.to4a(), 0);
		    for(int i = 0; i < pixels.length; i++) {
			if(pixels[i] != null) {
			    Image<?> img = data.tex.image(i);
			    gl.glTexImage2D(GL.GL_TEXTURE_2D, i, ifmt, img.w, img.h, 0, pfmt, pnum, ByteBuffer.wrap(pixels[i].data));
			}
		    }
		    gl.bglCheckErr();
		});
	}

	public static Tex2D create(GLEnvironment env, Sampler2D data) {
	    FillBuffers.Array[] pixels = new FillBuffers.Array[data.tex.images().size()];
	    for(int i = 0; i < pixels.length; i++)
		pixels[i] = (FillBuffers.Array)data.tex.init.fill(data.tex.image(i), env);
	    return(new Tex2D(env, data, pixels));
	}

	public void bind(BGL gl) {
	    gl.glBindTexture(GL.GL_TEXTURE_2D, this);
	}
	public void unbind(BGL gl) {
	    gl.glBindTexture(GL.GL_TEXTURE_2D, null);
	}
    }
}
