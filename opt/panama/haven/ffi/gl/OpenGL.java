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

package haven.ffi.gl;

import haven.ffi.*;
import java.nio.*;
import java.lang.invoke.*;
import java.lang.foreign.*;
import haven.ffi.x11.GLX;
import static haven.ffi.ABI.*;
import static haven.ffi.FUtils.*;
import static java.lang.foreign.ValueLayout.ADDRESS;

public abstract class OpenGL implements haven.render.gl.GL {
    public static abstract class Base extends OpenGL {
	static final ValueLayout.OfInt    GLenum     = ValueLayout.JAVA_INT;
	static final ValueLayout.OfByte   GLboolean  = ValueLayout.JAVA_BYTE;
	static final ValueLayout.OfInt    GLbitfield = ValueLayout.JAVA_INT;
	static final ValueLayout.OfByte   GLbyte     = ValueLayout.JAVA_BYTE;
	static final ValueLayout.OfByte   GLchar     = ValueLayout.JAVA_BYTE;
	static final ValueLayout.OfShort  GLshort    = ValueLayout.JAVA_SHORT;
	static final ValueLayout.OfInt    GLint      = ValueLayout.JAVA_INT;
	static final ValueLayout.OfByte   GLubyte    = ValueLayout.JAVA_BYTE;
	static final ValueLayout.OfShort  GLushort   = ValueLayout.JAVA_SHORT;
	static final ValueLayout.OfInt    GLuint     = ValueLayout.JAVA_INT;
	static final ValueLayout.OfLong   GLint64    = ValueLayout.JAVA_LONG;
	static final ValueLayout.OfLong   GLyint64   = ValueLayout.JAVA_LONG;
	static final ValueLayout.OfInt    GLsizei    = ValueLayout.JAVA_INT;
	static final ValueLayout.OfFloat  GLfloat    = ValueLayout.JAVA_FLOAT;
	static final ValueLayout.OfFloat  GLclampf   = ValueLayout.JAVA_FLOAT;
	static final ValueLayout.OfDouble GLdouble   = ValueLayout.JAVA_DOUBLE;
	static final ValueLayout.OfDouble GLclampd   = ValueLayout.JAVA_DOUBLE;
	static final MemoryLayout GLsync = PTRINT_T;
	static final MemoryLayout GLsizeiptr = SIZE_T;
	static final MemoryLayout GLintptr = SIZE_T;
	private static final Linker.Option critical = Linker.Option.critical(false);
	private static final Linker.Option heapdata = Linker.Option.critical(true);

	private static byte b(boolean v) {
	    return((byte)(v ? 1 : 0));
	}

	private static MemorySegment memcpy(Arena st, byte[] v) {
	    MemorySegment mem = st.allocate(GLchar, v.length);
	    for(int i = 0; i < v.length; i++)
		mem.set(GLchar, i * GLchar.byteSize(), v[i]);
	    return(mem);
	}

	private static MemorySegment memcpy(Arena st, int[] v) {
	    MemorySegment mem = st.allocate(GLint, v.length);
	    for(int i = 0; i < v.length; i++)
		mem.set(GLint, i * GLint.byteSize(), v[i]);
	    return(mem);
	}

	private static MemorySegment memcpy(Arena st, float[] v) {
	    MemorySegment mem = st.allocate(GLfloat, v.length);
	    for(int i = 0; i < v.length; i++)
		mem.set(GLfloat, i * GLfloat.byteSize(), v[i]);
	    return(mem);
	}

	private byte[] memcpy(byte[] dst, MemorySegment src, int n) {
	    for(int i = 0; i < n; i++)
		dst[i] = src.get(GLchar, GLchar.byteSize() * i);
	    return(dst);
	}

	private int[] memcpy(int[] dst, MemorySegment src, int n) {
	    for(int i = 0; i < n; i++)
		dst[i] = src.get(GLint, GLint.byteSize() * i);
	    return(dst);
	}

	private long[] memcpy(long[] dst, MemorySegment src, int n) {
	    for(int i = 0; i < n; i++)
		dst[i] = src.get(GLint64, GLint64.byteSize() * i);
	    return(dst);
	}

	private float[] memcpy(float[] dst, MemorySegment src, int n) {
	    for(int i = 0; i < n; i++)
		dst[i] = src.get(GLfloat, GLfloat.byteSize() * i);
	    return(dst);
	}

	private MemorySegment bufmem(Buffer buf) {
	    return((buf == null) ? MemorySegment.NULL : MemorySegment.ofBuffer(buf));
	}

	protected abstract MethodHandle lookup(String name, FunctionDescriptor sig, Linker.Option... options);

	private final MethodHandle glActiveTexture = lookup("glActiveTexture", FunctionDescriptor.ofVoid(GLenum), critical);
	public void glActiveTexture(int texture) {
	    try {
		glActiveTexture.invoke(texture);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glAttachShader = lookup("glAttachShader", FunctionDescriptor.ofVoid(GLuint, GLuint), critical);
	public void glAttachShader(int program, int shader) {
	    try {
		glAttachShader.invoke(program, shader);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glBindAttribLocation = lookup("glBindAttribLocation", FunctionDescriptor.ofVoid(GLuint, GLuint, ADDRESS), critical);
	public void glBindAttribLocation(int program, int index, String name) {
	    try(Arena st = Arena.ofConfined()) {
		glBindAttribLocation.invoke(program, index, st.allocateFrom(name));
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glBindBuffer = lookup("glBindBuffer", FunctionDescriptor.ofVoid(GLenum, GLuint), critical);
	public void glBindBuffer(int target, int buffer) {
	    try {
		glBindBuffer.invoke(target, buffer);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glBindFragDataLocation = lookup("glBindFragDataLocation", FunctionDescriptor.ofVoid(GLuint, GLuint, ADDRESS), critical);
	public void glBindFragDataLocation(int program, int colornumber, String name) {
	    try(Arena st = Arena.ofConfined()) {
		glBindFragDataLocation.invoke(program, colornumber, st.allocateFrom(name, C_CHARSET));
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glBindFramebuffer = lookup("glBindFramebuffer", FunctionDescriptor.ofVoid(GLenum, GLuint), critical);
	public void glBindFramebuffer(int target, int buffer) {
	    try {
		glBindFramebuffer.invoke(target, buffer);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glBindRenderbuffer = lookup("glBindRenderbuffer", FunctionDescriptor.ofVoid(GLenum, GLuint), critical);
	public void glBindRenderbuffer(int target, int buffer) {
	    try {
		glBindRenderbuffer.invoke(target, buffer);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glBindTexture = lookup("glBindTexture", FunctionDescriptor.ofVoid(GLenum, GLuint), critical);
	public void glBindTexture(int target, int texture) {
	    try {
		glBindTexture.invoke(target, texture);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glBindVertexArray = lookup("glBindVertexArray", FunctionDescriptor.ofVoid(GLuint), critical);
	public void glBindVertexArray(int array) {
	    try {
		glBindVertexArray.invoke(array);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glBlendColor = lookup("glBlendColor", FunctionDescriptor.ofVoid(GLfloat, GLfloat, GLfloat, GLfloat), critical);
	public void glBlendColor(float red, float green, float blue, float alpha) {
	    try {
		glBlendColor.invoke(red, green, blue, alpha);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glBlendEquation = lookup("glBlendEquation", FunctionDescriptor.ofVoid(GLenum), critical);
	public void glBlendEquation(int mode) {
	    try {
		glBlendEquation.invoke(mode);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glBlendEquationSeparate = lookup("glBlendEquationSeparate", FunctionDescriptor.ofVoid(GLenum, GLenum), critical);
	public void glBlendEquationSeparate(int cmode, int amode) {
	    try {
		glBlendEquationSeparate.invoke(cmode, amode);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glBlendFunc = lookup("glBlendFunc", FunctionDescriptor.ofVoid(GLenum, GLenum), critical);
	public void glBlendFunc(int sfac, int dfac) {
	    try {
		glBlendFunc.invoke(sfac, dfac);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glBlendFuncSeparate = lookup("glBlendFuncSeparate", FunctionDescriptor.ofVoid(GLenum, GLenum, GLenum, GLenum), critical);
	public void glBlendFuncSeparate(int csfac, int cdfac, int asfac, int adfac) {
	    try {
		glBlendFuncSeparate.invoke(csfac, cdfac, asfac, adfac);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glBufferData = lookup("glBufferData", FunctionDescriptor.ofVoid(GLenum, GLsizeiptr, ADDRESS, GLenum), heapdata);
	public void glBufferData(int target, long size, ByteBuffer data, int usage) {
	    try {
		glBufferData.invoke(target, size, bufmem(data), usage);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glBufferSubData = lookup("glBufferSubData", FunctionDescriptor.ofVoid(GLenum, GLintptr, GLsizeiptr, ADDRESS), heapdata);
	public void glBufferSubData(int target, long offset, long size, ByteBuffer data) {
	    try {
		glBufferSubData.invoke(target, offset, size, bufmem(data));
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glCheckFramebufferStatus = lookup("glCheckFramebufferStatus", FunctionDescriptor.of(GLenum, GLenum), critical);
	public int glCheckFramebufferStatus(int target) {
	    try {
		return((int)glCheckFramebufferStatus.invoke(target));
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glClear = lookup("glClear", FunctionDescriptor.ofVoid(GLbitfield), critical);
	public void glClear(int mask) {
	    try {
		glClear.invoke(mask);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glClearBufferfv = lookup("glClearBufferfv", FunctionDescriptor.ofVoid(GLenum, GLint, ADDRESS), critical);
	public void glClearBufferfv(int buffer, int drawbuffer, float[] value) {
	    try(Arena st = Arena.ofConfined()) {
		glClearBufferfv.invoke(buffer, drawbuffer, memcpy(st, value));
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glClearBufferiv = lookup("glClearBufferiv", FunctionDescriptor.ofVoid(GLenum, GLint, ADDRESS), critical);
	public void glClearBufferiv(int buffer, int drawbuffer, int[] value) {
	    try(Arena st = Arena.ofConfined()) {
		glClearBufferiv.invoke(buffer, drawbuffer, memcpy(st, value));
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glClearBufferuiv = lookup("glClearBufferuiv", FunctionDescriptor.ofVoid(GLenum, GLint, ADDRESS), critical);
	public void glClearBufferuiv(int buffer, int drawbuffer, int[] value) {
	    try(Arena st = Arena.ofConfined()) {
		glClearBufferuiv.invoke(buffer, drawbuffer, memcpy(st, value));
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glClearColor = lookup("glClearColor", FunctionDescriptor.ofVoid(GLfloat, GLfloat, GLfloat, GLfloat), critical);
	public void glClearColor(float r, float g, float b, float a) {
	    try {
		glClearColor.invoke(r, g, b, a);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glClearDepth = lookup("glClearDepth", FunctionDescriptor.ofVoid(GLdouble), critical);
	public void glClearDepth(double d) {
	    try {
		glClearDepth.invoke(d);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glColorMask = lookup("glColorMask", FunctionDescriptor.ofVoid(GLboolean, GLboolean, GLboolean, GLboolean), critical);
	public void glColorMask(boolean r, boolean g, boolean b, boolean a) {
	    try {
		glColorMask.invoke(b(r), b(g), b(b), b(a));
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glColorMaski = lookup("glColorMaski", FunctionDescriptor.ofVoid(GLuint, GLboolean, GLboolean, GLboolean, GLboolean), critical);
	public void glColorMaski(int buf, boolean r, boolean g, boolean b, boolean a) {
	    try {
		glColorMaski.invoke(buf, b(r), b(g), b(b), b(a));
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glCompileShader = lookup("glCompileShader", FunctionDescriptor.ofVoid(GLuint), critical);
	public void glCompileShader(int shader) {
	    try {
		glCompileShader.invoke(shader);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glCreateProgram = lookup("glCreateProgram", FunctionDescriptor.of(GLuint), critical);
	public int glCreateProgram() {
	    try {
		return((int)glCreateProgram.invoke());
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glCreateShader = lookup("glCreateShader", FunctionDescriptor.of(GLuint, GLenum), critical);
	public int glCreateShader(int type) {
	    try {
		return((int)glCreateShader.invoke(type));
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glDeleteBuffers = lookup("glDeleteBuffers", FunctionDescriptor.ofVoid(GLsizei, ADDRESS), critical);
	public void glDeleteBuffers(int count, int[] buffers) {
	    try(Arena st = Arena.ofConfined()) {
		glDeleteBuffers.invoke(count, memcpy(st, buffers));
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glDeleteFramebuffers = lookup("glDeleteFramebuffers", FunctionDescriptor.ofVoid(GLsizei, ADDRESS), critical);
	public void glDeleteFramebuffers(int count, int[] buffers) {
	    try(Arena st = Arena.ofConfined()) {
		glDeleteFramebuffers.invoke(count, memcpy(st, buffers));
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glDeleteShader = lookup("glDeleteShader", FunctionDescriptor.ofVoid(GLuint), critical);
	public void glDeleteShader(int id) {
	    try {
		glDeleteShader.invoke(id);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glDeleteProgram = lookup("glDeleteProgram", FunctionDescriptor.ofVoid(GLuint), critical);
	public void glDeleteProgram(int id) {
	    try {
		glDeleteProgram.invoke(id);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glDeleteQueries = lookup("glDeleteQueries", FunctionDescriptor.ofVoid(GLsizei, ADDRESS), critical);
	public void glDeleteQueries(int count, int[] buffer) {
	    try(Arena st = Arena.ofConfined()) {
		glDeleteQueries.invoke(count, memcpy(st, buffer));
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glDeleteRenderbuffers = lookup("glDeleteRenderbuffers", FunctionDescriptor.ofVoid(GLsizei, ADDRESS), critical);
	public void glDeleteRenderbuffers(int count, int[] buffers) {
	    try(Arena st = Arena.ofConfined()) {
		glDeleteRenderbuffers.invoke(count, memcpy(st, buffers));
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glDeleteSync = lookup("glDeleteSync", FunctionDescriptor.ofVoid(GLsync), critical);
	public void glDeleteSync(long id) {
	    try {
		glDeleteSync.invoke(id);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glDeleteTextures = lookup("glDeleteTextures", FunctionDescriptor.ofVoid(GLsizei, ADDRESS), critical);
	public void glDeleteTextures(int count, int[] buffers) {
	    try(Arena st = Arena.ofConfined()) {
		glDeleteTextures.invoke(count, memcpy(st, buffers));
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glDeleteVertexArrays = lookup("glDeleteVertexArrays", FunctionDescriptor.ofVoid(GLsizei, ADDRESS), critical);
	public void glDeleteVertexArrays(int count, int[] buffers) {
	    try(Arena st = Arena.ofConfined()) {
		glDeleteVertexArrays.invoke(count, memcpy(st, buffers));
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glCullFace = lookup("glCullFace", FunctionDescriptor.ofVoid(GLenum), critical);
	public void glCullFace(int mode) {
	    try {
		glCullFace.invoke(mode);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glDebugMessageControl = lookup("glDebugMessageControl", FunctionDescriptor.ofVoid(GLenum, GLenum, GLenum, GLsizei, ADDRESS, GLboolean), critical);
	public void glDebugMessageControl(int source, int type, int severity, int count, int[] ids, boolean enabled) {
	    try(Arena st = Arena.ofConfined()) {
		glDebugMessageControl.invoke(source, type, severity, count, memcpy(st, ids), b(enabled));
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glDepthFunc = lookup("glDepthFunc", FunctionDescriptor.ofVoid(GLenum), critical);
	public void glDepthFunc(int func) {
	    try {
		glDepthFunc.invoke(func);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glDepthMask = lookup("glDepthMask", FunctionDescriptor.ofVoid(GLboolean), critical);
	public void glDepthMask(boolean mask) {
	    try {
		glDepthMask.invoke(b(mask));
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glDisable = lookup("glDisable", FunctionDescriptor.ofVoid(GLenum), critical);
	public void glDisable(int cap) {
	    try {
		glDisable.invoke(cap);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glDisablei = lookup("glDisablei", FunctionDescriptor.ofVoid(GLenum, GLuint), critical);
	public void glDisablei(int cap, int index) {
	    try {
		glDisablei.invoke(cap, index);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glDisableVertexAttribArray = lookup("glDisableVertexAttribArray", FunctionDescriptor.ofVoid(GLuint), critical);
	public void glDisableVertexAttribArray(int location) {
	    try {
		glDisableVertexAttribArray.invoke(location);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glDrawBuffer = lookup("glDrawBuffer", FunctionDescriptor.ofVoid(GLenum), critical);
	public void glDrawBuffer(int buf) {
	    try {
		glDrawBuffer.invoke(buf);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glDrawBuffers = lookup("glDrawBuffers", FunctionDescriptor.ofVoid(GLsizei, ADDRESS), critical);
	public void glDrawBuffers(int n, int[] bufs) {
	    try(Arena st = Arena.ofConfined()) {
		glDrawBuffers.invoke(n, memcpy(st, bufs));
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glDrawArraysInstanced = lookup("glDrawArraysInstanced", FunctionDescriptor.ofVoid(GLenum, GLint, GLsizei, GLsizei), critical);
	public void glDrawArraysInstanced(int mode, int first, int count, int primcount) {
	    try {
		glDrawArraysInstanced.invoke(mode, first, count, primcount);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glDrawArrays = lookup("glDrawArrays", FunctionDescriptor.ofVoid(GLenum, GLint, GLsizei), critical);
	public void glDrawArrays(int mode, int first, int count) {
	    try {
		glDrawArrays.invoke(mode, first, count);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glDrawElementsInstanced = lookup("glDrawElementsInstanced", FunctionDescriptor.ofVoid(GLenum, GLsizei, GLenum, PTRINT_T, GLsizei), critical);
	public void glDrawElementsInstanced(int mode, int count, int type, long indices, int primcount) {
	    try {
		glDrawElementsInstanced.invoke(mode, count, type, indices, primcount);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glDrawElements = lookup("glDrawElements", FunctionDescriptor.ofVoid(GLenum, GLsizei, GLenum, PTRINT_T), critical);
	public void glDrawElements(int mode, int count, int type, long indices) {
	    try {
		glDrawElements.invoke(mode, count, type, indices);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glDrawRangeElements = lookup("glDrawRangeElements", FunctionDescriptor.ofVoid(GLenum, GLuint, GLuint, GLsizei, GLenum, PTRINT_T), critical);
	public void glDrawRangeElements(int mode, int start, int end, int count, int type, long indices) {
	    try {
		glDrawRangeElements.invoke(mode, start, end, count, type, indices);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glEnable = lookup("glEnable", FunctionDescriptor.ofVoid(GLenum), critical);
	public void glEnable(int cap) {
	    try {
		glEnable.invoke(cap);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glEnablei = lookup("glEnablei", FunctionDescriptor.ofVoid(GLenum, GLuint), critical);
	public void glEnablei(int cap, int index) {
	    try {
		glEnablei.invoke(cap, index);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glEnableVertexAttribArray = lookup("glEnableVertexAttribArray", FunctionDescriptor.ofVoid(GLuint), critical);
	public void glEnableVertexAttribArray(int location) {
	    try {
		glEnableVertexAttribArray.invoke(location);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glFenceSync = lookup("glFenceSync", FunctionDescriptor.of(GLsync, GLenum, GLbitfield), critical);
	public long glFenceSync(int condition, int flags) {
	    try {
		return((long)glFenceSync.invoke(condition, flags));
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glFinish = lookup("glFinish", FunctionDescriptor.ofVoid(), critical);
	public void glFinish() {
	    try {
		glFinish.invoke();
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glFramebufferTexture2D = lookup("glFramebufferTexture2D", FunctionDescriptor.ofVoid(C_INT, C_INT, C_INT, C_INT, C_INT), critical);
	public void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level) {
	    try {
		glFramebufferTexture2D.invoke(target, attachment, textarget, texture, level);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glFramebufferRenderbuffer = lookup("glFramebufferRenderbuffer", FunctionDescriptor.ofVoid(GLenum, GLenum, GLenum, GLuint), critical);
	public void glFramebufferRenderbuffer(int target, int attachment, int rbtarget, int renderbuffer) {
	    try {
		glFramebufferRenderbuffer.invoke(target, attachment, rbtarget, renderbuffer);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glGenBuffers = lookup("glGenBuffers", FunctionDescriptor.ofVoid(GLsizei, ADDRESS), critical);
	public void glGenBuffers(int n, int[] buffer) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment buf = st.allocate(GLint, n);
		glGenBuffers.invoke(n, buf);
		memcpy(buffer, buf, n);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glGenFramebuffers = lookup("glGenFramebuffers", FunctionDescriptor.ofVoid(GLsizei, ADDRESS), critical);
	public void glGenFramebuffers(int n, int[] buffer) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment buf = st.allocate(GLint, n);
		glGenFramebuffers.invoke(n, buf);
		memcpy(buffer, buf, n);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glGenQueries = lookup("glGenQueries", FunctionDescriptor.ofVoid(GLsizei, ADDRESS), critical);
	public void glGenQueries(int n, int[] buffer) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment buf = st.allocate(GLint, n);
		glGenQueries.invoke(n, buf);
		memcpy(buffer, buf, n);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glGenTextures = lookup("glGenTextures", FunctionDescriptor.ofVoid(GLsizei, ADDRESS), critical);
	public void glGenTextures(int n, int[] buffer) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment buf = st.allocate(GLint, n);
		glGenTextures.invoke(n, buf);
		memcpy(buffer, buf, n);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glGenVertexArrays = lookup("glGenVertexArrays", FunctionDescriptor.ofVoid(GLsizei, ADDRESS), critical);
	public void glGenVertexArrays(int n, int[] buffer) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment buf = st.allocate(GLint, n);
		glGenVertexArrays.invoke(n, buf);
		memcpy(buffer, buf, n);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glGetBufferSubData = lookup("glGetBufferSubData", FunctionDescriptor.ofVoid(GLenum, GLintptr, GLsizeiptr, ADDRESS), heapdata);
	public void glGetBufferSubData(int target, int offset, int size, ByteBuffer data) {
	    try {
		glGetBufferSubData.invoke(target, offset, size, bufmem(data));
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glGetDebugMessageLog = lookup("glGetDebugMessageLog", FunctionDescriptor.of(GLuint, GLuint, GLsizei, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS), critical);
	public int glGetDebugMessageLog(int count, int bufsize, int[] sources, int[] types, int[] ids, int[] severities, int[] lengths, byte[] buffer) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment sobuf = st.allocate(GLenum, count), tybuf = st.allocate(GLenum, count), idbuf = st.allocate(GLuint, count), sebuf = st.allocate(GLenum, count);
		MemorySegment lebuf = st.allocate(GLsizei, count), mebuf = st.allocate(GLchar, bufsize);
		int ret = (int)glGetDebugMessageLog.invoke(count, bufsize, sobuf, tybuf, idbuf, sebuf, lebuf, mebuf);
		memcpy(sources, sobuf, ret); memcpy(types, tybuf, ret); memcpy(ids, idbuf, ret);
		memcpy(severities, sobuf, ret); memcpy(lengths, lebuf, ret); memcpy(buffer, mebuf, bufsize);
		return(ret);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glGetError = lookup("glGetError", FunctionDescriptor.of(GLenum), critical);
	public int glGetError() {
	    try {
		return((int)glGetError.invoke());
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glGetFloatv = lookup("glGetFloatv", FunctionDescriptor.ofVoid(GLenum, ADDRESS), critical);
	public void glGetFloatv(int pname, float[] data) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment buf = st.allocate(GLfloat, data.length);
		glGetFloatv.invoke(pname, buf);
		memcpy(data, buf, data.length);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glGetIntegerv = lookup("glGetIntegerv", FunctionDescriptor.ofVoid(GLenum, ADDRESS), critical);
	public void glGetIntegerv(int pname, int[] data) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment buf = st.allocate(C_INT, data.length);
		glGetIntegerv.invoke(pname, buf);
		memcpy(data, buf, data.length);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glGetString = lookup("glGetString", FunctionDescriptor.of(ADDRESS, GLenum), critical);
	public String glGetString(int name) {
	    try {
		return(((MemorySegment)glGetString.invoke(name)).reinterpret(Long.MAX_VALUE).getString(0, C_CHARSET));
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glGetStringi = lookup("glGetStringi", FunctionDescriptor.of(ADDRESS, GLenum, GLuint), critical);
	public String glGetStringi(int name, int index) {
	    try {
		return(((MemorySegment)glGetStringi.invoke(name, index)).reinterpret(Long.MAX_VALUE).getString(0, C_CHARSET));
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glGetProgramInfoLog = lookup("glGetProgramInfoLog", FunctionDescriptor.ofVoid(GLuint, GLsizei, ADDRESS, ADDRESS), critical);
	public void glGetProgramInfoLog(int shader, int maxlength, int[] length, byte[] infolog) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment lbuf = st.allocate(GLsizei), ibuf = st.allocate(GLchar, maxlength);
		glGetProgramInfoLog.invoke(shader, maxlength, lbuf, ibuf);
		length[0] = lbuf.get(GLsizei, 0); memcpy(infolog, ibuf, length[0] + 1);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glGetProgramiv = lookup("glGetProgramiv", FunctionDescriptor.ofVoid(GLuint, GLenum, ADDRESS), critical);
	public void glGetProgramiv(int shader, int pname, int[] rbuf) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment buf = st.allocate(GLint);
		glGetProgramiv.invoke(shader, pname, buf);
		rbuf[0] = (int)getint(buf, 0, GLint, true);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glGetQueryObjectiv = lookup("glGetQueryObjectiv", FunctionDescriptor.ofVoid(GLuint, GLenum, ADDRESS), critical);
	public void glGetQueryObjectiv(int id, int pname, int[] params) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment buf = st.allocate(GLint, params.length);
		glGetQueryObjectiv.invoke(id, pname, buf);
		memcpy(params, buf, params.length);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glGetQueryObjecti64v = lookup("glGetQueryObjecti64v", FunctionDescriptor.ofVoid(GLuint, GLenum, ADDRESS), critical);
	public void glGetQueryObjecti64v(int id, int pname, long[] params) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment buf = st.allocate(GLint64, params.length);
		glGetQueryObjecti64v.invoke(id, pname, buf);
		memcpy(params, buf, params.length);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glGetShaderInfoLog = lookup("glGetShaderInfoLog", FunctionDescriptor.ofVoid(GLuint, GLsizei, ADDRESS, ADDRESS), critical);
	public void glGetShaderInfoLog(int shader, int maxlength, int[] length, byte[] infolog) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment lbuf = st.allocate(GLsizei), ibuf = st.allocate(GLchar, maxlength);
		glGetShaderInfoLog.invoke(shader, maxlength, lbuf, ibuf);
		length[0] = lbuf.get(GLsizei, 0); memcpy(infolog, ibuf, length[0] + 1);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glGetShaderiv = lookup("glGetShaderiv", FunctionDescriptor.ofVoid(GLuint, GLenum, ADDRESS), critical);
	public void glGetShaderiv(int shader, int pname, int[] rbuf) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment buf = st.allocate(GLint);
		glGetShaderiv.invoke(shader, pname, buf);
		rbuf[0] = (int)getint(buf, 0, GLint, true);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glGetSynciv = lookup("glGetSynciv", FunctionDescriptor.ofVoid(GLsync, GLenum, GLsizei, ADDRESS, ADDRESS), critical);
	public void glGetSynciv(long sync, int pname, int bufsize, int[] length, int[] values) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment lbuf = (length == null) ? MemorySegment.NULL : st.allocate(GLsizei), vbuf = st.allocate(GLint, bufsize);
		glGetSynciv.invoke(sync, pname, bufsize, lbuf, vbuf);
		if(length != null)
		    length[0] = lbuf.get(GLsizei, 0);
		memcpy(values, vbuf, bufsize);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glGetTexImage = lookup("glGetTexImage", FunctionDescriptor.ofVoid(GLenum, GLint, GLenum, GLenum, ADDRESS), heapdata);
	public void glGetTexImage(int target, int level, int format, int type, ByteBuffer pixels) {
	    try {
		glGetTexImage.invoke(target, level, format, type, bufmem(pixels));
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glGetTexImage2 = lookup("glGetTexImage", FunctionDescriptor.ofVoid(GLenum, GLint, GLenum, GLenum, PTRINT_T), critical);
	public void glGetTexImage(int target, int level, int format, int type, long offset) {
	    try {
		glGetTexImage2.invoke(target, level, format, type, offset);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glGetUniformLocation = lookup("glGetUniformLocation", FunctionDescriptor.of(GLint, GLuint, ADDRESS), critical);
	public int glGetUniformLocation(int program, String name) {
	    try(Arena st = Arena.ofConfined()) {
		return((int)glGetUniformLocation.invoke(program, st.allocateFrom(name, C_CHARSET)));
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glDebugMessageInsert = lookup("glDebugMessageInsert", FunctionDescriptor.ofVoid(GLenum, GLenum, GLuint, GLenum, GLsizei, ADDRESS), critical);
	public void glDebugMessageInsert(int source, int type, int id, int severity, String message) {
	    try(Arena st = Arena.ofConfined()) {
		glDebugMessageInsert.invoke(source,type, id, severity, -1, st.allocateFrom(message));
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glLineWidth = lookup("glLineWidth", FunctionDescriptor.ofVoid(GLfloat), critical);
	public void glLineWidth(float w) {
	    try {
		glLineWidth.invoke(w);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glLinkProgram = lookup("glLinkProgram", FunctionDescriptor.ofVoid(GLuint), critical);
	public void glLinkProgram(int program) {
	    try {
		glLinkProgram.invoke(program);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glObjectLabel = lookup("glObjectLabel", FunctionDescriptor.ofVoid(GLenum, GLuint, GLsizei, ADDRESS), critical);
	public void glObjectLabel(int identifier, int name, int length, byte[] label) {
	    try(Arena st = Arena.ofConfined()) {
		glObjectLabel.invoke(identifier, name, length, memcpy(st, label));
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glPixelStorei = lookup("glPixelStorei", FunctionDescriptor.ofVoid(GLenum, GLint), critical);
	public void glPixelStorei(int pname, int param) {
	    try {
		glPixelStorei.invoke(pname, param);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glPointSize = lookup("glPointSize", FunctionDescriptor.ofVoid(GLfloat), critical);
	public void glPointSize(float size) {
	    try {
		glPointSize.invoke(size);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glPolygonMode = lookup("glPolygonMode", FunctionDescriptor.ofVoid(GLenum, GLenum), critical);
	public void glPolygonMode(int face, int mode) {
	    try {
		glPolygonMode.invoke(face, mode);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glPolygonOffset = lookup("glPolygonOffset", FunctionDescriptor.ofVoid(GLfloat, GLfloat), critical);
	public void glPolygonOffset(float factor, float units) {
	    try {
		glPolygonOffset.invoke(factor, units);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glPopDebugGroup = lookup("glPopDebugGroup", FunctionDescriptor.ofVoid(), critical);
	public void glPopDebugGroup() {
	    try {
		glPopDebugGroup.invoke();
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glPushDebugGroup = lookup("glPushDebugGroup", FunctionDescriptor.ofVoid(GLenum, GLint, GLsizei, ADDRESS), critical);
	public void glPushDebugGroup(int source, int id, String name) {
	    try(Arena st = Arena.ofConfined()) {
		glPushDebugGroup.invoke(source, id, -1, st.allocateFrom(name));
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glQueryCounter = lookup("glQueryCounter", FunctionDescriptor.ofVoid(GLuint, GLenum), critical);
	public void glQueryCounter(int id, int target) {
	    try {
		glQueryCounter.invoke(id, target);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glReadBuffer = lookup("glReadBuffer", FunctionDescriptor.ofVoid(GLenum), critical);
	public void glReadBuffer(int buf) {
	    try {
		glReadBuffer.invoke(buf);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glReadPixels = lookup("glReadPixels", FunctionDescriptor.ofVoid(GLint, GLint, GLsizei, GLsizei, GLenum, GLenum, ADDRESS), heapdata);
	public void glReadPixels(int x, int y, int width, int height, int format, int type, ByteBuffer data) {
	    try {
		glReadPixels.invoke(x, y, width, height, format, type, bufmem(data));
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glReadPixels2 = lookup("glReadPixels", FunctionDescriptor.ofVoid(GLint, GLint, GLsizei, GLsizei, GLenum, GLenum, PTRINT_T), critical);
	public void glReadPixels(int x, int y, int width, int height, int format, int type, long offset) {
	    try {
		glReadPixels2.invoke(x, y, width, height, format, type, offset);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glRenderbufferStorage = lookup("glRenderbufferStorage", FunctionDescriptor.ofVoid(GLenum, GLenum, GLsizei, GLsizei), critical);
	public void glRenderbufferStorage(int target, int format, int width, int height) {
	    try {
		glRenderbufferStorage.invoke(target, format, width, height);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glRenderbufferStorageMultisample = lookup("glRenderbufferStorageMultisample", FunctionDescriptor.ofVoid(GLenum, GLsizei, GLenum, GLsizei, GLsizei), critical);
	public void glRenderbufferStorageMultisample(int target, int samples, int format, int width, int height) {
	    try {
		glRenderbufferStorageMultisample.invoke(target, samples, format, width, height);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glSampleCoverage = lookup("glSampleCoverage", FunctionDescriptor.ofVoid(GLfloat, GLboolean), critical);
	public void glSampleCoverage(float value, boolean invert) {
	    try {
		glSampleCoverage.invoke(value, b(invert));
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glScissor = lookup("glScissor", FunctionDescriptor.ofVoid(GLint, GLint, GLsizei, GLsizei), critical);
	public void glScissor(int x, int y, int w, int h) {
	    try {
		glScissor.invoke(x, y, w, h);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glShaderSource = lookup("glShaderSource", FunctionDescriptor.ofVoid(GLuint, GLsizei, ADDRESS, ADDRESS), critical);
	public void glShaderSource(int shader, int count, String[] string, int[] lengths) {
	    if(string.length != lengths.length)
		throw(new IllegalArgumentException());
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment sary = st.allocate(ADDRESS, string.length);
		MemorySegment lary = st.allocate(GLint, string.length);
		for(int i = 0; i < string.length; i++) {
		    MemorySegment str = st.allocateFrom(string[i], C_CHARSET);
		    sary.set(ADDRESS, i * ADDRESS.byteSize(), str);
		    setint(lary, i * GLint.byteSize(), GLint, lengths[i]);
		}
		glShaderSource.invoke(shader, count, sary, lary);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glTexImage2D = lookup("glTexImage2D", FunctionDescriptor.ofVoid(GLenum, GLint, GLint, GLsizei, GLsizei, GLint, GLenum, GLenum, ADDRESS), heapdata);
	public void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, ByteBuffer data) {
	    try {
		glTexImage2D.invoke(target, level, internalformat, width, height, border, format, type, bufmem(data));
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glTexSubImage2D = lookup("glTexSubImage2D", FunctionDescriptor.ofVoid(GLenum, GLint, GLint, GLint, GLsizei, GLsizei, GLenum, GLenum, ADDRESS), heapdata);
	public void glTexSubImage2D(int target, int level, int xoff, int yoff, int width, int height, int format, int type, ByteBuffer data) {
	    try {
		glTexSubImage2D.invoke(target, level, xoff, yoff, width, height, format, type, bufmem(data));
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glTexImage2DMultisample = lookup("glTexImage2DMultisample", FunctionDescriptor.ofVoid(GLenum, GLsizei, GLenum, GLsizei, GLsizei, GLboolean), critical);
	public void glTexImage2DMultisample(int target, int samples, int internalformat, int width, int height, boolean fixedsamplelocations) {
	    try {
		glTexImage2DMultisample.invoke(target, samples, internalformat, width, height, b(fixedsamplelocations));
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glTexImage3D = lookup("glTexImage3D", FunctionDescriptor.ofVoid(GLenum, GLint, GLint, GLsizei, GLsizei, GLsizei, GLint, GLenum, GLenum, ADDRESS), heapdata);
	public void glTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int border, int format, int type, ByteBuffer data) {
	    try {
		glTexImage3D.invoke(target, level, internalformat, width, height, depth, border, format, type, bufmem(data));
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glTexSubImage3D = lookup("glTexSubImage3D", FunctionDescriptor.ofVoid(GLenum, GLint, GLint, GLint, GLint, GLsizei, GLsizei, GLsizei, GLenum, GLenum, ADDRESS), heapdata);
	public void glTexSubImage3D(int target, int level, int xoff, int yoff, int zoff, int width, int height, int depth, int format, int type, ByteBuffer data) {
	    try {
		glTexSubImage3D.invoke(target, level, xoff, yoff, zoff, width, height, depth, format, type, bufmem(data));
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glTexParameterf = lookup("glTexParameterf", FunctionDescriptor.ofVoid(GLenum, GLenum, GLfloat), critical);
	public void glTexParameterf(int target, int pname, float param) {
	    try {
		glTexParameterf.invoke(target, pname, param);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glTexParameterfv = lookup("glTexParameterfv", FunctionDescriptor.ofVoid(GLenum, GLenum, ADDRESS), critical);
	public void glTexParameterfv(int target, int pname, float[] param) {
	    try(Arena st = Arena.ofConfined()) {
		glTexParameterfv.invoke(target, pname, memcpy(st, param));
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glTexParameteri = lookup("glTexParameteri", FunctionDescriptor.ofVoid(GLenum, GLenum, GLint), critical);
	public void glTexParameteri(int target, int pname, int param) {
	    try {
		glTexParameteri.invoke(target, pname, param);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glUniform1f = lookup("glUniform1f", FunctionDescriptor.ofVoid(GLint, GLfloat), critical);
	public void glUniform1f(int location, float v0) {
	    try {
		glUniform1f.invoke(location, v0);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glUniform2f = lookup("glUniform2f", FunctionDescriptor.ofVoid(GLint, GLfloat, GLfloat), critical);
	public void glUniform2f(int location, float v0, float v1) {
	    try {
		glUniform2f.invoke(location, v0, v1);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glUniform3f = lookup("glUniform3f", FunctionDescriptor.ofVoid(GLint, GLfloat, GLfloat, GLfloat), critical);
	public void glUniform3f(int location, float v0, float v1, float v2) {
	    try {
		glUniform3f.invoke(location, v0, v1, v2);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glUniform3fv = lookup("glUniform3fv", FunctionDescriptor.ofVoid(GLint, GLsizei, ADDRESS), critical);
	public void glUniform3fv(int location, int count, float[] val) {
	    try(Arena st = Arena.ofConfined()) {
		glUniform3fv.invoke(location, count, memcpy(st, val));
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glUniform4f = lookup("glUniform4f", FunctionDescriptor.ofVoid(GLint, GLfloat, GLfloat, GLfloat, GLfloat), critical);
	public void glUniform4f(int location, float v0, float v1, float v2, float v3) {
	    try {
		glUniform4f.invoke(location, v0, v1, v2, v3);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glUniform4fv = lookup("glUniform4fv", FunctionDescriptor.ofVoid(GLint, GLsizei, ADDRESS), critical);
	public void glUniform4fv(int location, int count, float[] val) {
	    try(Arena st = Arena.ofConfined()) {
		glUniform4fv.invoke(location, count, memcpy(st, val));
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glUniform1i = lookup("glUniform1i", FunctionDescriptor.ofVoid(GLint, GLint), critical);
	public void glUniform1i(int location, int v0) {
	    try {
		glUniform1i.invoke(location, v0);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glUniform2i = lookup("glUniform2i", FunctionDescriptor.ofVoid(GLint, GLint, GLint), critical);
	public void glUniform2i(int location, int v0, int v1) {
	    try {
		glUniform2i.invoke(location, v0, v1);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glUniform3i = lookup("glUniform3i", FunctionDescriptor.ofVoid(GLint, GLint, GLint, GLint), critical);
	public void glUniform3i(int location, int v0, int v1, int v2) {
	    try {
		glUniform3i.invoke(location, v0, v1, v2);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glUniform4i = lookup("glUniform4i", FunctionDescriptor.ofVoid(GLint, GLint, GLint, GLint, GLint), critical);
	public void glUniform4i(int location, int v0, int v1, int v2, int v3) {
	    try {
		glUniform4i.invoke(location, v0, v1, v2, v3);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glUniformMatrix3fv = lookup("glUniformMatrix3fv", FunctionDescriptor.ofVoid(GLint, GLsizei, GLboolean, ADDRESS), critical);
	public void glUniformMatrix3fv(int location, int count, boolean transpose, float[] value) {
	    try(Arena st = Arena.ofConfined()) {
		glUniformMatrix3fv.invoke(location, count, b(transpose), memcpy(st, value));
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glUniformMatrix4fv = lookup("glUniformMatrix4fv", FunctionDescriptor.ofVoid(GLint, GLsizei, GLboolean, ADDRESS), critical);
	public void glUniformMatrix4fv(int location, int count, boolean transpose, float[] value) {
	    try(Arena st = Arena.ofConfined()) {
		glUniformMatrix4fv.invoke(location, count, b(transpose), memcpy(st, value));
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glUseProgram = lookup("glUseProgram", FunctionDescriptor.ofVoid(GLuint), critical);
	public void glUseProgram(int program) {
	    try {
		glUseProgram.invoke(program);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glVertexAttribDivisor = lookup("glVertexAttribDivisor", FunctionDescriptor.ofVoid(GLuint, GLuint), critical);
	public void glVertexAttribDivisor(int location, int divisor) {
	    try {
		glVertexAttribDivisor.invoke(location, divisor);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glVertexAttribPointer = lookup("glVertexAttribPointer", FunctionDescriptor.ofVoid(GLuint, GLint, GLenum, GLboolean, GLsizei, PTRINT_T), critical);
	public void glVertexAttribPointer(int location, int size, int type, boolean normalized, int stride, long pointer) {
	    try {
		glVertexAttribPointer.invoke(location, size, type, b(normalized), stride, pointer);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glVertexAttribIPointer = lookup("glVertexAttribIPointer", FunctionDescriptor.ofVoid(GLuint, GLint, GLenum, GLsizei, PTRINT_T), critical);
	public void glVertexAttribIPointer(int location, int size, int type, int stride, long pointer) {
	    try {
		glVertexAttribIPointer.invoke(location, size, type, stride, pointer);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}

	private final MethodHandle glViewport = lookup("glViewport", FunctionDescriptor.ofVoid(GLint, GLint, GLsizei, GLsizei), critical);
	public void glViewport(int x, int y, int w, int h) {
	    try {
		glViewport.invoke(x, y, w, h);
	    } catch(Throwable e) {throw(new InvocationException(e));}
	}
    }
}
