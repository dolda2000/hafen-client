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

import java.nio.*;
import java.util.function.*;
import javax.media.opengl.*;
import haven.*;
import haven.render.*;
import haven.render.sl.*;
import static haven.render.DataBuffer.Usage.*;

public class GLRender implements Render, Disposable {
    public final GLEnvironment env;
    BufferBGL gl = null;
    final Applier state;
    Applier init = null;

    GLRender(GLEnvironment env) {
	this.env = env;
	this.state = new Applier(env);
    }

    public GLEnvironment env() {return(env);}

    BGL gl() {
	if(this.gl == null) {
	    this.gl = new BufferBGL();
	    this.init = state.clone();
	    if(this.init.prog() != null)
		this.init.prog().glid();
	}
	return(this.gl);
    }

    public static int glmode(Model.Mode mode) {
	switch(mode) {
	case POINTS:          return(GL.GL_POINTS);
	case LINES:           return(GL.GL_LINES);
	case LINE_STRIP:      return(GL.GL_LINE_STRIP);
	case TRIANGLES:       return(GL.GL_TRIANGLES);
	case TRIANGLE_STRIP:  return(GL.GL_TRIANGLE_STRIP);
	case TRIANGLE_FAN:    return(GL.GL_TRIANGLE_FAN);
	default:
	    throw(new RuntimeException("unimplemented draw mode " + mode));
	}
    }

    public static int glattribfmt(NumberFormat fmt) {
	switch(fmt) {
	case UNORM8:    return(GL.GL_UNSIGNED_BYTE);
	case SNORM8:    return(GL.GL_BYTE);
	case UNORM16:   return(GL.GL_UNSIGNED_SHORT);
	case SNORM16:   return(GL.GL_SHORT);
	case UNORM32:   return(GL2.GL_UNSIGNED_INT);
	case SNORM32:   return(GL2.GL_INT);
	case FLOAT16:   return(GL.GL_HALF_FLOAT);
	case FLOAT32:   return(GL.GL_FLOAT);
	case UINT8:     return(GL.GL_UNSIGNED_BYTE);
	case SINT8:     return(GL.GL_BYTE);
	case UINT16:    return(GL.GL_UNSIGNED_SHORT);
	case SINT16:    return(GL.GL_SHORT);
	case UINT32:    return(GL2.GL_UNSIGNED_INT);
	case SINT32:    return(GL2.GL_INT);
	default:
	    throw(new RuntimeException("unimplemented vertex attribute format " + fmt));
	}
    }

    public static boolean glattribnorm(NumberFormat fmt) {
	switch(fmt) {
	case UNORM8:
	case SNORM8:
	case UNORM16:
	case SNORM16:
	case UNORM32:
	case SNORM32:
	    return(true);
	default:
	    return(false);
	}
    }

    public static int glindexfmt(NumberFormat fmt) {
	switch(fmt) {
	case UINT8:     return(GL.GL_UNSIGNED_BYTE);
	case UINT16:    return(GL.GL_UNSIGNED_SHORT);
	case UINT32:    return(GL2.GL_UNSIGNED_INT);
	default:
	    throw(new RuntimeException("unimplemented vertex index format " + fmt));
	}
    }

    public static int glsamplertarget(Type type) {
	if(type == Type.SAMPLER2D)
	    return(GL.GL_TEXTURE_2D);
	else if(type == Type.SAMPLER2DMS)
	    return(GL3.GL_TEXTURE_2D_MULTISAMPLE);
	else if(type == Type.SAMPLER2DMSARRAY)
	    return(GL3.GL_TEXTURE_2D_MULTISAMPLE_ARRAY);
	else if(type == Type.SAMPLER2DARRAY)
	    return(GL.GL_TEXTURE_2D_ARRAY);
	else if(type == Type.SAMPLER2DSHADOW)
	    return(GL2.GL_TEXTURE_2D);
	else if(type == Type.SAMPLERCUBE)
	    return(GL2.GL_TEXTURE_CUBE_MAP);
	else if(type == Type.SAMPLERCUBEARRAY)
	    return(GL4.GL_TEXTURE_CUBE_MAP_ARRAY);
	else if(type == Type.SAMPLERCUBESHADOW)
	    return(GL2.GL_TEXTURE_CUBE_MAP);
	else if(type == Type.SAMPLER3D)
	    return(GL2.GL_TEXTURE_3D);
	else if(type == Type.SAMPLER1D)
	    return(GL2.GL_TEXTURE_1D);
	else if(type == Type.SAMPLER1DARRAY)
	    return(GL2.GL_TEXTURE_1D_ARRAY);
	else
	    throw(new RuntimeException("invalid sampler type: " + type));
    }

    public void submit(Render gsub) {
	if(!(gsub instanceof GLRender))
	    throw(new IllegalArgumentException());
	GLRender sub = (GLRender)gsub;
	if(sub.env != this.env)
	    throw(new IllegalArgumentException());
	if(sub.gl == null)
	    return;
	state.apply(this.gl, sub.init);
	gl().bglCallList(sub.gl);
	state.apply(null, sub.state);
    }

    public void draw(Pipe pipe, Model data) {
	state.apply(this.gl, pipe);
	if(GLVertexArray.ephemeralp(data)) {
	    Disposable indo = null;
	    if(data.ind != null)
		indo = env.prepare(data.ind);
	    Disposable[] bufs = new Disposable[data.va.bufs.length];
	    int ne = 0;
	    for(int i = 0; i < data.va.bufs.length; i++) {
		bufs[i] = env.prepare(data.va.bufs[i]);
		if(data.va.bufs[i].usage == EPHEMERAL)
		    ne++;
	    }

	    GLProgram.VarID[] enable = new GLProgram.VarID[data.va.fmt.inputs.length];
	    for(int i = 0; i < enable.length; i++) {
		/* XXX: Properly handle input attributes not present in the program. */
		enable[i] = state.prog().cattrib(data.va.fmt.inputs[i].tgt);
	    }
	    Vao0State.apply(this.gl, state, enable);

	    BGL gl = gl();
	    int[] offsets = new int[data.va.bufs.length];
	    GLBuffer cbuf = VboState.get(state);
	    GLBuffer vbuf = null;
	    if(ne > 0) {
		vbuf = env.tempvertex.get();
		if(vbuf != cbuf) {
		    gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbuf);
		    cbuf = vbuf;
		}
		if(ne == 1) {
		    int sbuf = -1;
		    if(data.va.bufs.length == 1) {
			sbuf = 0;
		    } else {
			for(int i = 0; i < data.va.bufs.length; i++) {
			    if(data.va.bufs[i].usage == EPHEMERAL) {
				sbuf = i;
				break;
			    }
			}
		    }
		    gl.glBufferData(GL.GL_ARRAY_BUFFER, data.va.bufs[sbuf].size(), ByteBuffer.wrap(((HeapBuffer)bufs[sbuf]).buf), GL2.GL_STREAM_DRAW);
		    offsets[sbuf] = 0;
		} else {
		    int sz = 0;
		    for(int i = 0; i < data.va.bufs.length; i++) {
			if(data.va.bufs[i].usage == EPHEMERAL) {
			    sz += data.va.bufs[i].size();
			    offsets[i] = sz;
			}
		    }
		    int jdsz = sz; GLBuffer jdvbuf = vbuf;
		    gl.bglSubmit(new BGL.Request() {
			    public void run(GL2 gl) {
				ByteBuffer buf = ByteBuffer.wrap(new byte[jdsz]);
				for(int i = 0; i < data.va.bufs.length; i++) {
				    if(data.va.bufs[i].usage == EPHEMERAL)
					buf.put(((HeapBuffer)bufs[i]).buf);
				}
				buf.flip();
				gl.glBufferData(GL.GL_ARRAY_BUFFER, jdsz, buf, GL2.GL_STREAM_DRAW);
			    }
			});
		}
	    }
	    for(int i = 0; i < data.va.fmt.inputs.length; i++) {
		VertexArray.Layout.Input attr = data.va.fmt.inputs[i];
		int off;
		if(data.va.bufs[attr.buf].usage == EPHEMERAL) {
		    if(vbuf != cbuf) {
			gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbuf);
			cbuf = vbuf;
		    }
		    off = offsets[data.va.fmt.inputs[i].buf];
		} else {
		    throw(new NotImplemented("non-ephemeral vertex arrays"));
		}
		gl.glVertexAttribPointer(enable[i], attr.el.nc, glattribfmt(attr.el.cf), glattribnorm(attr.el.cf), attr.stride, attr.offset + off);
	    }
	    VboState.set(state, cbuf);
	    if(data.ind == null) {
		gl.glDrawArrays(glmode(data.mode), data.f, data.n);
	    } else {
		if(data.ind.usage == EPHEMERAL) {
		    Vao0State.apply(gl, state, env.tempindex.get());
		    gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, data.ind.size(), ByteBuffer.wrap(((HeapBuffer)indo).buf), GL2.GL_STREAM_DRAW);
		} else {
		    throw(new NotImplemented("non-ephemeral index arrays"));
		}
		gl.glDrawElements(glmode(data.mode), data.n, glindexfmt(data.ind.fmt), data.f * data.ind.fmt.size);
	    }
	} else {
	    throw(new NotImplemented("non-ephemeral models"));
	}
    }

    public void clear(Pipe pipe, FragData buf, FColor val) {
	state.apply(this.gl, pipe);
	GLProgram prog = state.prog();
	FboState fc = (FboState)state.glstates[FboState.slot];
	if(prog.fragdata.length == 1) {
	    if(buf != prog.fragdata[0])
		throw(new IllegalArgumentException(String.format("%s is not on current framebuffer", buf)));
	    if((fc.dbufs != null) && (fc.dbufs[0] != GL.GL_NONE)) {
		BGL gl = gl();
		gl.glClearColor(val.r, val.g, val.b, val.a);
		gl.glClear(GL.GL_COLOR_BUFFER_BIT);
	    }
	} else {
	    int n = -1;
	    for(int i = 0; i < prog.fragdata.length; i++) {
		if(prog.fragdata[i] == buf) {
		    n = i;
		    break;
		}
	    }
	    if(n < 0)
		throw(new IllegalArgumentException(String.format("%s is not on current framebuffer", buf)));
	    if((fc.dbufs != null) && (fc.dbufs[n] != GL.GL_NONE)) {
		BGL gl = gl();
		gl.glClearColor(val.r, val.g, val.b, val.a);
		gl.glDrawBuffer(fc.dbufs[n]);
		gl.glClear(GL.GL_COLOR_BUFFER_BIT);
		fc.applydbufs(gl);
	    }
	}
    }

    public void clear(Pipe pipe, double val) {
	state.apply(this.gl, pipe);
	FboState fc = (FboState)state.glstates[FboState.slot];
	if((fc.fbo != null) && (fc.fbo.depth == null))
	    throw(new IllegalArgumentException("current framebuffer has no depthbuffer"));
	BGL gl = gl();
	gl.glClearDepth(val);
	gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
    }

    public <T extends DataBuffer> void update(T buf, DataBuffer.Filler<? super T> fill) {
	if(buf instanceof Model.Indices) {
	    Model.Indices ibuf = (Model.Indices)buf;
	    if(ibuf.usage == EPHEMERAL) {
		throw(new NotImplemented("update ephemeral index buffer"));
	    } else {
		FillBuffers.Array data = (FillBuffers.Array)fill.fill(buf, env);
		Vao0State.apply(this.gl, state, (GLBuffer)env.prepare(ibuf));
		BGL gl = gl();
		int usage = (ibuf.usage == STREAM) ? GL.GL_DYNAMIC_DRAW : GL.GL_STATIC_DRAW;
		gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, buf.size(), ByteBuffer.wrap(data.data), usage);
	    }
	} else if(buf instanceof VertexArray.Buffer) {
	    VertexArray.Buffer vbuf = (VertexArray.Buffer)buf;
	    switch(vbuf.usage) {
	    case STATIC: {
		FillBuffers.Array data = (FillBuffers.Array)fill.fill(buf, env);
		VboState.apply(this.gl, state, (GLBuffer)env.prepare(vbuf));
		BGL gl = gl();
		gl.glBufferData(GL.GL_ARRAY_BUFFER, buf.size(), ByteBuffer.wrap(data.data), GL.GL_STATIC_DRAW);
		break;
	    }
	    case STREAM: {
		StreamBuffer ro = (StreamBuffer)env.prepare(vbuf);
		StreamBuffer.Fill data = (StreamBuffer.Fill)fill.fill(buf, env);
		VboState.apply(this.gl, state, ro.rbuf);
		BGL gl = gl();
		ByteBuffer xfbuf = data.get();
		gl.glBufferData(GL.GL_ARRAY_BUFFER, buf.size(), xfbuf, GL.GL_DYNAMIC_DRAW);
		ro.put(gl, xfbuf);
		break;
	    }
	    default:
		throw(new NotImplemented("update " + vbuf.usage + " vertex buffer"));
	    }
	} else {
	    throw(new NotImplemented("updating buffer of type: " + buf.getClass().getName()));
	}
    }

    public void pget(Pipe pipe, FragData buf, Area area, VectorFormat fmt, Consumer<ByteBuffer> callback) {
	state.apply(this.gl, pipe);
	GLProgram prog = state.prog();
	FboState fc = (FboState)state.glstates[FboState.slot];
	int n = -1;
	for(int i = 0; i < prog.fragdata.length; i++) {
	    if(prog.fragdata[i] == buf) {
		n = i;
		break;
	    }
	}
	if(n < 0)
	    throw(new IllegalArgumentException(String.format("%s is not on current framebuffer", buf)));
	BGL gl = gl();
	int gly = env.wnd.br.y - area.br.y;
	Coord sz = area.sz();

	/*
	gl.glPixelStorei(GL.GL_PACK_ALIGNMENT, 1);
	gl.glReadBuffer(fc.dbufs[n]);
	gl.bglSubmit(cgl -> {
		ByteBuffer data = ByteBuffer.wrap(new byte[fmt.size() * area.area()]);
		cgl.glReadPixels(area.ul.x, gly, sz.x, sz.y, GLTexture.texefmt1(fmt, fmt), GLTexture.texefmt2(fmt, fmt), data);
		GLException.checkfor(cgl);
		data.rewind();
		callback.accept(data);
	    });
	*/

	GLBuffer pbo = new GLBuffer(env);
	gl.glBindBuffer(GL2.GL_PIXEL_PACK_BUFFER, pbo);
	gl.glBufferData(GL2.GL_PIXEL_PACK_BUFFER, fmt.size() * area.area(), null, GL2.GL_STREAM_READ);
	gl.glPixelStorei(GL.GL_PACK_ALIGNMENT, 1);
	gl.glReadBuffer(fc.dbufs[n]);
	gl.glReadPixels(area.ul.x, gly, sz.x, sz.y, GLTexture.texefmt1(fmt, fmt), GLTexture.texefmt2(fmt, fmt), 0);
	gl.bglCreate(new GLFence(env, cgl -> {
		    cgl.glBindBuffer(GL2.GL_PIXEL_PACK_BUFFER, pbo.glid());
		    ByteBuffer data = Utils.mkbbuf(fmt.size() * area.area());
		    cgl.glGetBufferSubData(GL2.GL_PIXEL_PACK_BUFFER, 0, fmt.size() * area.area(), data);
		    cgl.glBindBuffer(GL2.GL_PIXEL_PACK_BUFFER, 0);
		    pbo.dispose();
		    GLException.checkfor(cgl);
		    data.rewind();
		    /* XXX: It's not particularly nice to do the
		     * flipping on the dispatch thread, but OpenGL
		     * does not seem to offer any GPU-assisted
		     * flipping. */
		    for(int y = 0; y < sz.y / 2; y++) {
			int to = y * sz.x * 4, bo = (sz.y - y - 1) * sz.x * 4;
			for(int o = 0; o < sz.x * 4; o++, to++, bo++) {
			    byte t = data.get(to);
			    data.put(to, data.get(bo));
			    data.put(bo, t);
			}
		    }
		    callback.accept(data);
	}));
	gl.glBindBuffer(GL2.GL_PIXEL_PACK_BUFFER, null);
    }

    public void pget(Texture.Image img, VectorFormat fmt, Consumer<ByteBuffer> callback) {
	BGL gl = gl();

	state.apply(gl, new Applier(env));
	GLBuffer pbo = new GLBuffer(env);
	final int dsz = fmt.size() * img.w * img.h * img.d;
	gl.glBindBuffer(GL2.GL_PIXEL_PACK_BUFFER, pbo);
	gl.glBufferData(GL2.GL_PIXEL_PACK_BUFFER, dsz, null, GL2.GL_STREAM_READ);
	gl.glPixelStorei(GL.GL_PACK_ALIGNMENT, 1);
	if(img.tex instanceof Texture2D) {
	    GLTexture.Tex2D tex = env.prepare((Texture2D)img.tex);
	    gl.glActiveTexture(GL.GL_TEXTURE0);
	    tex.bind(gl);
	    gl.glGetTexImage(GL2.GL_TEXTURE_2D, img.level, GLTexture.texefmt1(fmt, fmt), GLTexture.texefmt2(fmt, fmt), 0);
	    tex.unbind(gl);
	} else {
	    throw(new NotImplemented("texture-get for " + img.tex.getClass()));
	}
	gl.bglCreate(new GLFence(env, cgl -> {
		    cgl.glBindBuffer(GL2.GL_PIXEL_PACK_BUFFER, pbo.glid());
		    ByteBuffer data = Utils.mkbbuf(dsz);
		    cgl.glGetBufferSubData(GL2.GL_PIXEL_PACK_BUFFER, 0, dsz, data);
		    cgl.glBindBuffer(GL2.GL_PIXEL_PACK_BUFFER, 0);
		    pbo.dispose();
		    GLException.checkfor(cgl);
		    data.rewind();
		    /* XXX: It's not particularly nice to do the
		     * flipping on the dispatch thread, but OpenGL
		     * does not seem to offer any GPU-assisted
		     * flipping. */
		    if(img.d == 1) {
			for(int y = 0; y < img.h / 2; y++) {
			    int to = y * img.w * 4, bo = (img.h - y - 1) * img.w * 4;
			    for(int o = 0; o < img.w * 4; o++, to++, bo++) {
				byte t = data.get(to);
				data.put(to, data.get(bo));
				data.put(bo, t);
			    }
			}
		    }
		    callback.accept(data);
	}));
	gl.glBindBuffer(GL2.GL_PIXEL_PACK_BUFFER, null);
    }

    public void dispose() {
    }
}
