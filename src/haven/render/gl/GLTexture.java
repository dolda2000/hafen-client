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
import haven.render.TextureCube.CubeImage;
import haven.render.TextureCube.SamplerCube;
import java.nio.*;
import java.util.*;
import javax.media.opengl.*;

public abstract class GLTexture extends GLObject implements BGL.ID {
    protected int id, state = 0;
    Collection<GLFrameBuffer> fbos = null;

    public GLTexture(GLEnvironment env) {
	super(env);
	env.prepare(this);
    }

    public void create(GL2 gl) {
	ckstate(state, 0);
	int[] buf = {0};
	gl.glGenTextures(1, buf, 0);
	GLException.checkfor(gl);
	this.id = buf[0];
	state = 1;
    }

    protected void delete(GL2 gl) {
	ckstate(state, 1);
	gl.glDeleteTextures(1, new int[] {id}, 0);
	state = 2;
    }

    public int glid() {
	ckstate(state, 1);
	return(id);
    }

    public String toString() {
	return(String.format("#<gl.tex %d @ %08x>", id, System.identityHashCode(this)));
    }

    public void dispose() {
	while(true) {
	    GLFrameBuffer fbo, last = null;
	    synchronized(this) {
		if((fbos == null) || fbos.isEmpty())
		    break;
		fbo = fbos.iterator().next();
	    }
	    if(fbo == last)
		throw(new RuntimeException(String.format("FBO %s somehow not unregistering from %s when disposing", fbo, this)));
	    fbo.dispose();
	    last = fbo;
	}
	super.dispose();
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
	    case UNORM16: return(GL2.GL_R16);
	    case SNORM16: return(GL2.GL_R16_SNORM);
	    case FLOAT16: return(GL2.GL_R16F);
	    case DEPTH: return(GL2.GL_DEPTH_COMPONENT);
	    }
	case 2:
	    switch(fmt.cf) {
	    case UNORM8: return(GL2.GL_RG8);
	    case SNORM8: return(GL2.GL_RG8_SNORM);
	    case UNORM16: return(GL2.GL_RG16);
	    case SNORM16: return(GL2.GL_RG16_SNORM);
	    case FLOAT16: return(GL2.GL_RG16F);
	    }
	case 3:
	    switch(fmt.cf) {
	    case UNORM8: return(GL2.GL_RGB8);
	    case SNORM8: return(GL2.GL_RGB8_SNORM);
	    case UNORM16: return(GL2.GL_RGB16);
	    case SNORM16: return(GL2.GL_RGB16_SNORM);
	    case FLOAT16: return(GL2.GL_RGB16F);
	    }
	case 4:
	    switch(fmt.cf) {
	    case UNORM8: return(GL2.GL_RGBA8);
	    case SNORM8: return(GL2.GL_RGBA8_SNORM);
	    case UNORM16: return(GL2.GL_RGBA16);
	    case SNORM16: return(GL2.GL_RGBA16_SNORM);
	    case FLOAT16: return(GL2.GL_RGBA16F);
	    }
	}
	throw(new IllegalArgumentException(String.format("internalformat: %s", fmt)));
    }

    static int texefmt1(VectorFormat ifmt, VectorFormat efmt) {
	if(ifmt.cf == NumberFormat.DEPTH) {
	    if(efmt.nc != 1)
		throw(new IllegalArgumentException(String.format("externalformat components != 1 for depth texture: %s", efmt)));
	    return(GL2.GL_DEPTH_COMPONENT);
	}
	switch(efmt.nc) {
	case 1: return(GL2.GL_RED);
	case 2: return(GL2.GL_RG);
	case 3: return(GL2.GL_RGB);
	case 4: return(GL2.GL_RGBA);
	}
	throw(new IllegalArgumentException(String.format("externalformat1: %s", efmt)));
    }

    static int texefmt2(VectorFormat ifmt, VectorFormat efmt) {
	switch(efmt.cf) {
	case UNORM8: return(GL2.GL_UNSIGNED_BYTE);
	case SNORM8: return(GL2.GL_BYTE);
	case UNORM16: return(GL2.GL_UNSIGNED_SHORT);
	case SNORM16: return(GL2.GL_SHORT);
	case UNORM32: return(GL2.GL_UNSIGNED_INT);
	case SNORM32: return(GL2.GL_INT);
	case FLOAT32: return(GL2.GL_FLOAT);
	}
	throw(new IllegalArgumentException(String.format("externalformat2: %s", efmt)));
    }

    static int texface(TextureCube.Face face) {
	switch(face) {
	    case XP: return(GL2.GL_TEXTURE_CUBE_MAP_POSITIVE_X);
	    case XN: return(GL2.GL_TEXTURE_CUBE_MAP_NEGATIVE_X);
	    case YP: return(GL2.GL_TEXTURE_CUBE_MAP_POSITIVE_Y);
	    case YN: return(GL2.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y);
	    case ZP: return(GL2.GL_TEXTURE_CUBE_MAP_POSITIVE_Z);
	    case ZN: return(GL2.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z);
	}
	throw(new IllegalArgumentException(String.format("texface: %s", face)));
    }

    public static class Tex2D extends GLTexture {
	public final Texture2D data;
	Sampler2D sampler;

	public Tex2D(GLEnvironment env, Texture2D data, FillBuffers.Array[] pixels) {
	    super(env);
	    this.data = data;
	    int ifmt = texifmt(data.ifmt);
	    int pfmt = texefmt1(data.ifmt, data.efmt);
	    int pnum = texefmt2(data.ifmt, data.efmt);
	    env.prepare((GLRender g) -> {
		    if(g.state.prog() != null)
			throw(new RuntimeException("program unexpectedly used in prep context"));
		    BGL gl = g.gl();
		    gl.glActiveTexture(GL.GL_TEXTURE0);
		    bind(gl);
		    if(pixels[0] != null)
			gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, ifmt, data.w, data.h, 0, pfmt, pnum, ByteBuffer.wrap(pixels[0].data));
		    else
			gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, ifmt, data.w, data.h, 0, pfmt, pnum, null);
		    long mem = data.ifmt.size() * data.w * data.h;
		    for(int i = 1; i < pixels.length; i++) {
			if(pixels[i] != null) {
			    Image<?> img = data.image(i);
			    gl.glTexImage2D(GL.GL_TEXTURE_2D, i, ifmt, img.w, img.h, 0, pfmt, pnum, ByteBuffer.wrap(pixels[i].data));
			    mem += data.ifmt.size() * img.w * img.h;
			}
		    }
		    setmem(GLEnvironment.MemStats.TEXTURES, mem);
		    unbind(gl);
		    gl.bglCheckErr();
		});
	}

	public static Tex2D create(GLEnvironment env, Texture2D data) {
	    FillBuffers.Array[] pixels = new FillBuffers.Array[data.images().size()];
	    if(data.init != null) {
		for(int i = 0; i < pixels.length; i++)
		    pixels[i] = (FillBuffers.Array)data.init.fill(data.image(i), env);
		data.init.done();
	    }
	    return(new Tex2D(env, data, pixels));
	}

	public void setsampler(Sampler2D data) {
	    if(sampler == data)
		return;
	    if(sampler != null) {
		if(sampler.equals(data))
		    return;
		throw(new IllegalArgumentException("OpenGL 2.0 does not support multiple (different) samplers per texture"));
	    }
	    env.prepare((GLRender g) -> {
		    if(g.state.prog() != null)
			throw(new RuntimeException("program unexpectedly used in prep context"));
		    BGL gl = g.gl();
		    gl.glActiveTexture(GL.GL_TEXTURE0);
		    bind(gl);
		    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, magfilter(data));
		    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, minfilter(data));
		    if(data.anisotropy > 0)
			gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAX_ANISOTROPY_EXT, data.anisotropy);
		    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, wrapmode(data.swrap));
		    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, wrapmode(data.twrap));
		    gl.glTexParameterfv(GL.GL_TEXTURE_2D, GL2.GL_TEXTURE_BORDER_COLOR, data.border.to4a(), 0);
		    unbind(gl);
		    gl.bglCheckErr();
		});
	    sampler = data;
	}

	public void bind(BGL gl) {
	    gl.glBindTexture(GL.GL_TEXTURE_2D, this);
	}
	public void unbind(BGL gl) {
	    gl.glBindTexture(GL.GL_TEXTURE_2D, null);
	}

	public String toString() {
	    return(String.format("#<gl.tex2d %d @ %08x %s>", id, System.identityHashCode(this), data));
	}
    }

    public static class TexCube extends GLTexture {
	public final TextureCube data;
	SamplerCube sampler;

	public TexCube(GLEnvironment env, TextureCube data, CubeImage[] images, FillBuffers.Array[] pixels) {
	    super(env);
	    this.data = data;
	    int ifmt = texifmt(data.ifmt);
	    int pfmt = texefmt1(data.ifmt, data.efmt);
	    int pnum = texefmt2(data.ifmt, data.efmt);
	    env.prepare((GLRender g) -> {
		    if(g.state.prog() != null)
			throw(new RuntimeException("program unexpectedly used in prep context"));
		    BGL gl = g.gl();
		    gl.glActiveTexture(GL.GL_TEXTURE0);
		    bind(gl);
		    long mem = 0;
		    for(int i = 0; i < pixels.length; i++) {
			CubeImage img = images[i];
			int tgt = texface(img.face);
			if(pixels[i] != null) {
			    gl.glTexImage2D(tgt, img.level, ifmt, img.w, img.h, 0, pfmt, pnum, ByteBuffer.wrap(pixels[i].data));
			    mem += data.ifmt.size() * img.w * img.h;
			} else if(img.level == 0) {
			    gl.glTexImage2D(tgt, 0, ifmt, data.w, data.h, 0, pfmt, pnum, null);
			    mem += data.ifmt.size() * data.w * data.h;
			}
		    }
		    setmem(GLEnvironment.MemStats.TEXTURES, mem);
		    unbind(gl);
		    gl.bglCheckErr();
		});
	}

	public static TexCube create(GLEnvironment env, TextureCube data) {
	    CubeImage[] images = new CubeImage[data.images().size()];
	    FillBuffers.Array[] pixels = new FillBuffers.Array[data.images().size()];
	    int i = 0;
	    for(CubeImage img : data.images()) {
		images[i] = img;
		if(data.init != null)
		    pixels[i] = (FillBuffers.Array)data.init.fill(img, env);
		i++;
	    }
	    if(data.init != null)
		data.init.done();
	    return(new TexCube(env, data, images, pixels));
	}

	public void setsampler(SamplerCube data) {
	    if(sampler == data)
		return;
	    if(sampler != null)
		throw(new IllegalArgumentException("OpenGL 2.0 does not support multiple samplers per texture"));
	    env.prepare((GLRender g) -> {
		    if(g.state.prog() != null)
			throw(new RuntimeException("program unexpectedly used in prep context"));
		    BGL gl = g.gl();
		    gl.glActiveTexture(GL.GL_TEXTURE0);
		    bind(gl);
		    gl.glTexParameteri(GL.GL_TEXTURE_CUBE_MAP, GL.GL_TEXTURE_MAG_FILTER, magfilter(data));
		    gl.glTexParameteri(GL.GL_TEXTURE_CUBE_MAP, GL.GL_TEXTURE_MIN_FILTER, minfilter(data));
		    if(data.anisotropy > 0)
			gl.glTexParameterf(GL.GL_TEXTURE_CUBE_MAP, GL.GL_TEXTURE_MAX_ANISOTROPY_EXT, data.anisotropy);
		    gl.glTexParameteri(GL.GL_TEXTURE_CUBE_MAP, GL.GL_TEXTURE_WRAP_S, wrapmode(data.swrap));
		    gl.glTexParameteri(GL.GL_TEXTURE_CUBE_MAP, GL.GL_TEXTURE_WRAP_T, wrapmode(data.twrap));
		    gl.glTexParameterfv(GL.GL_TEXTURE_CUBE_MAP, GL2.GL_TEXTURE_BORDER_COLOR, data.border.to4a(), 0);
		    unbind(gl);
		    gl.bglCheckErr();
		});
	    sampler = data;
	}

	public void bind(BGL gl) {
	    gl.glBindTexture(GL.GL_TEXTURE_CUBE_MAP, this);
	}
	public void unbind(BGL gl) {
	    gl.glBindTexture(GL.GL_TEXTURE_CUBE_MAP, null);
	}

	public String toString() {
	    return(String.format("#<gl.texcube %d @ %08x %s>", id, System.identityHashCode(this), data));
	}
    }
}
