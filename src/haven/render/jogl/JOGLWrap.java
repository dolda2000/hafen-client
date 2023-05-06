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

package haven.render.jogl;

import java.util.regex.*;
import java.nio.*;
import com.jogamp.opengl.*;
import haven.render.gl.GL;
import haven.render.gl.GLException;

public class JOGLWrap implements GL, WrappedJOGL {
    public final GL3 back;

    public JOGLWrap(GL3 back) {
	this.back = back;
    }

    public com.jogamp.opengl.GL getGL() {return(back);}

    public void glActiveTexture(int texture) {back.glActiveTexture(texture);}
    public void glAttachShader(int program, int shader) {back.glAttachShader(program, shader);}
    public void glBindAttribLocation(int program, int index, String name) {back.glBindAttribLocation(program, index, name);}
    public void glBindBuffer(int target, int buffer) {back.glBindBuffer(target, buffer);}
    public void glBindFragDataLocation(int program, int colornumber, String name) {back.glBindFragDataLocation(program, colornumber, name);}
    public void glBindFramebuffer(int target, int buffer) {back.glBindFramebuffer(target, buffer);}
    public void glBindRenderbuffer(int target, int buffer) {back.glBindRenderbuffer(target, buffer);}
    public void glBindTexture(int target, int texture) {back.glBindTexture(target, texture);}
    public void glBindVertexArray(int array) {back.glBindVertexArray(array);}
    public void glBlendColor(float red, float green, float blue, float alpha) {back.glBlendColor(red, green, blue, alpha);}
    public void glBlendEquation(int mode) {back.glBlendEquation(mode);}
    public void glBlendEquationSeparate(int cmode, int amode) {back.glBlendEquationSeparate(cmode, amode);}
    public void glBlendFunc(int sfac, int dfac) {back.glBlendFunc(sfac, dfac);}
    public void glBlendFuncSeparate(int csfac, int cdfac, int asfac, int adfac) {back.glBlendFuncSeparate(csfac, cdfac, asfac, adfac);}
    public void glBufferData(int target, long size, ByteBuffer data, int usage) {back.glBufferData(target, size, data, usage);}
    public void glBufferSubData(int target, long offset, long size, ByteBuffer data) {back.glBufferSubData(target, offset, size, data);}
    public int glCheckFramebufferStatus(int target) {return(back.glCheckFramebufferStatus(target));}
    public void glClear(int mask) {back.glClear(mask);}
    public void glClearBufferfv(int buffer, int drawbuffer, float[] value) {back.glClearBufferfv(buffer, drawbuffer, value, 0);}
    public void glClearBufferiv(int buffer, int drawbuffer, int[] value) {back.glClearBufferiv(buffer, drawbuffer, value, 0);}
    public void glClearBufferuiv(int buffer, int drawbuffer, int[] value) {back.glClearBufferuiv(buffer, drawbuffer, value, 0);}
    public void glClearColor(float r, float g, float b, float a) {back.glClearColor(r, g, b, a);}
    public void glClearDepth(double d) {back.glClearDepth(d);}
    public void glColorMask(boolean r, boolean g, boolean b, boolean a) {back.glColorMask(r, g, b, a);}
    public void glColorMaski(int buf, boolean r, boolean g, boolean b, boolean a) {back.glColorMaski(buf, r, g, b, a);}
    public void glCompileShader(int shader) {back.glCompileShader(shader);}
    public int glCreateProgram() {return(back.glCreateProgram());}
    public int glCreateShader(int type) {return(back.glCreateShader(type));}
    public void glCullFace(int mode) {back.glCullFace(mode);}
    public void glDeleteBuffers(int count, int[] buffers) {back.glDeleteBuffers(count, buffers, 0);}
    public void glDeleteFramebuffers(int count, int[] buffers) {back.glDeleteFramebuffers(count, buffers, 0);}
    public void glDeleteShader(int id) {back.glDeleteShader(id);}
    public void glDeleteQueries(int count, int[] buffer) {back.glDeleteQueries(count, buffer, 0);}
    public void glDeleteProgram(int id) {back.glDeleteProgram(id);}
    public void glDeleteRenderbuffers(int count, int[] buffers) {back.glDeleteRenderbuffers(count, buffers, 0);}
    public void glDeleteTextures(int count, int[] buffers) {back.glDeleteTextures(count, buffers, 0);}
    public void glDeleteVertexArrays(int count, int[] buffers) {back.glDeleteVertexArrays(count, buffers, 0);}
    public void glDebugMessageControl(int source, int type, int severity, int count, int[] ids, boolean enabled) {back.glDebugMessageControl(source, type, severity, count, ids, 0, enabled);}
    public void glDeleteSync(long id) {back.glDeleteSync(id);}
    public void glDepthFunc(int func) {back.glDepthFunc(func);}
    public void glDepthMask(boolean mask) {back.glDepthMask(mask);}
    public void glDisable(int cap) {back.glDisable(cap);}
    public void glDisablei(int cap, int index) {back.glDisablei(cap, index);}
    public void glDisableClientState(int cap) {back.glDisableClientState(cap);}
    public void glDisableVertexAttribArray(int location) {back.glDisableVertexAttribArray(location);}
    public void glDrawBuffer(int buf) {back.glDrawBuffer(buf);}
    public void glDrawBuffers(int n, int[] bufs) {back.glDrawBuffers(n, bufs, 0);}
    public void glDrawArraysInstanced(int mode, int first, int count, int primcount) {back.glDrawArraysInstanced(mode, first, count, primcount);}
    public void glDrawArrays(int mode, int first, int count) {back.glDrawArrays(mode, first, count);}
    public void glDrawElementsInstanced(int mode, int count, int type, long indices, int primcount) {back.glDrawElementsInstanced(mode, count, type, indices, primcount);}
    public void glDrawElements(int mode, int count, int type, long indices) {back.glDrawElements(mode, count, type, indices);}
    public void glDrawRangeElements(int mode, int start, int end, int count, int type, long indices) {back.glDrawRangeElements(mode, start, end, count, type, indices);}
    public void glEnable(int cap) {back.glEnable(cap);}
    public void glEnablei(int cap, int index) {back.glEnablei(cap, index);}
    public void glEnableClientState(int cap) {back.glEnableClientState(cap);}
    public void glEnableVertexAttribArray(int location) {back.glEnableVertexAttribArray(location);}
    public long glFenceSync(int condition, int flags) {return(back.glFenceSync(condition, flags));}
    public void glFinish() {back.glFinish();}
    public void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level) {back.glFramebufferTexture2D(target, attachment, textarget, texture, level);}
    public void glFramebufferRenderbuffer(int target, int attachment, int rbtarget, int renderbuffer) {back.glFramebufferRenderbuffer(target, attachment, rbtarget, renderbuffer);}
    public void glGenBuffers(int n, int[] buffer) {back.glGenBuffers(n, buffer, 0);}
    public void glGenFramebuffers(int n, int[] buffer) {back.glGenFramebuffers(n, buffer, 0);}
    public void glGenQueries(int n, int[] buffer) {back.glGenQueries(n, buffer, 0);}
    public void glGenTextures(int n, int[] buffer) {back.glGenTextures(n, buffer, 0);}
    public void glGenVertexArrays(int n, int[] buffer) {back.glGenVertexArrays(n, buffer, 0);}
    public void glGetBufferSubData(int target, int offset, int size, ByteBuffer data) {back.glGetBufferSubData(target, offset, size, data);}
    public int glGetDebugMessageLog(int count, int bufsize, int[] sources, int[] types, int[] ids, int[] severities, int[] lengths, byte[] buffer) {return(back.glGetDebugMessageLog(count, bufsize, sources, 0, types, 0, ids, 0, severities, 0, lengths, 0, buffer, 0));}
    public int glGetError() {return(back.glGetError());}
    public void glGetFloatv(int pname, float[] data) {back.glGetFloatv(pname, data, 0);}
    public void glGetIntegerv(int pname, int[] data) {back.glGetIntegerv(pname, data, 0);}
    public String glGetString(int name) {return(back.glGetString(name));}
    public String glGetStringi(int name, int index) {return(back.glGetStringi(name, index));}
    public void glGetProgramInfoLog(int shader, int maxlength, int[] length, byte[] infolog) {back.glGetProgramInfoLog(shader, maxlength, length, 0, infolog, 0);}
    public void glGetProgramiv(int shader, int pname, int[] buf) {back.glGetProgramiv(shader, pname, buf, 0);}
    public void glGetQueryObjectiv(int id, int pname, int[] params) {back.glGetQueryObjectiv(id, pname, params, 0);}
    public void glGetQueryObjecti64v(int id, int pname, long[] params) {back.glGetQueryObjecti64v(id, pname, params, 0);}
    public void glGetShaderInfoLog(int shader, int maxlength, int[] length, byte[] infolog) {back.glGetShaderInfoLog(shader, maxlength, length, 0, infolog, 0);}
    public void glGetShaderiv(int shader, int pname, int[] buf) {back.glGetShaderiv(shader, pname, buf, 0);}
    public void glGetSynciv(long sync, int pname, int bufsize, int[] lengths, int[] values) {back.glGetSynciv(sync, pname, bufsize, lengths, 0, values, 0);}
    public void glGetTexImage(int target, int level, int format, int type, ByteBuffer pixels) {back.glGetTexImage(target, level, format, type, pixels);}
    public void glGetTexImage(int target, int level, int format, int type, long offset) {back.glGetTexImage(target, level, format, type, offset);}
    public int glGetUniformLocation(int program, String name) {return(back.glGetUniformLocation(program, name));}
    public void glLineWidth(float w) {back.glLineWidth(w);}
    public void glLinkProgram(int program) {back.glLinkProgram(program);}
    public void glObjectLabel(int identifier, int name, int length, byte[] label) {back.glObjectLabel(identifier, name, length, label, 0);}
    public void glPixelStorei(int pname, int param) {back.glPixelStorei(pname, param);}
    public void glPointSize(float size) {back.glPointSize(size);}
    public void glPolygonMode(int face, int mode) {back.glPolygonMode(face, mode);}
    public void glPolygonOffset(float factor, float units) {back.glPolygonOffset(factor, units);}
    public void glQueryCounter(int id, int target) {back.glQueryCounter(id, target);}
    public void glReadBuffer(int buf) {back.glReadBuffer(buf);}
    public void glReadPixels(int x, int y, int width, int height, int format, int type, ByteBuffer data) {back.glReadPixels(x, y, width, height, format, type, data);}
    public void glReadPixels(int x, int y, int width, int height, int format, int type, long offset) {back.glReadPixels(x, y, width, height, format, type, offset);}
    public void glRenderbufferStorage(int target, int format, int width, int height) {back.glRenderbufferStorage(target, format, width, height);}
    public void glRenderbufferStorageMultisample(int target, int samples, int format, int width, int height) {back.glRenderbufferStorageMultisample(target, samples, format, width, height);}
    public void glSampleCoverage(float value, boolean invert) {back.glSampleCoverage(value, invert);}
    public void glShaderSource(int shader, int count, String[] string, int[] lengths) {back.glShaderSource(shader, count, string, lengths, 0);}
    public void glScissor(int x, int y, int w, int h) {back.glScissor(x, y, w, h);}
    public void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, ByteBuffer data) {back.glTexImage2D(target, level, internalformat, width, height, border, format, type, data);}
    public void glTexSubImage2D(int target, int level, int xoff, int yoff, int width, int height, int format, int type, ByteBuffer data) {back.glTexSubImage2D(target, level, xoff, yoff, width, height, format, type, data);}
    public void glTexImage2DMultisample(int target, int samples, int internalformat, int width, int height, boolean fixedsamplelocations) {back.glTexImage2DMultisample(target, samples, internalformat, width, height, fixedsamplelocations);}
    public void glTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int border, int format, int type, ByteBuffer data) {back.glTexImage3D(target, level, internalformat, width, height, depth, border, format, type, data);}
    public void glTexSubImage3D(int target, int level, int xoff, int yoff, int zoff, int width, int height, int depth, int format, int type, ByteBuffer data) {back.glTexSubImage3D(target, level, xoff, yoff, zoff, width, height, depth, format, type, data);}
    public void glTexParameterf(int target, int pname, float param) {back.glTexParameterf(target, pname, param);}
    public void glTexParameterfv(int target, int pname, float[] param) {back.glTexParameterfv(target, pname, param, 0);}
    public void glTexParameteri(int target, int pname, int param) {back.glTexParameteri(target, pname, param);}
    public void glUniform1f(int location, float v0) {back.glUniform1f(location, v0);}
    public void glUniform2f(int location, float v0, float v1) {back.glUniform2f(location, v0, v1);}
    public void glUniform3f(int location, float v0, float v1, float v2) {back.glUniform3f(location, v0, v1, v2);}
    public void glUniform3fv(int location, int count, float[] val) {back.glUniform3fv(location, count, val, 0);}
    public void glUniform4f(int location, float v0, float v1, float v2, float v3) {back.glUniform4f(location, v0, v1, v2, v3);}
    public void glUniform4fv(int location, int count, float[] val) {back.glUniform4fv(location, count, val, 0);}
    public void glUniform1i(int location, int v0) {back.glUniform1i(location, v0);}
    public void glUniform2i(int location, int v0, int v1) {back.glUniform2i(location, v0, v1);}
    public void glUniform3i(int location, int v0, int v1, int v2) {back.glUniform3i(location, v0, v1, v2);} 
    public void glUniform4i(int location, int v0, int v1, int v2, int v3) {back.glUniform4i(location, v0, v1, v2, v3);}
    public void glUniformMatrix3fv(int location, int count, boolean transpose, float[] value) {back.glUniformMatrix3fv(location, count, transpose, value, 0);}
    public void glUniformMatrix4fv(int location, int count, boolean transpose, float[] value) {back.glUniformMatrix4fv(location, count, transpose, value, 0);}
    public void glUseProgram(int program) {back.glUseProgram(program);}
    public void glVertexAttribDivisor(int location, int divisor) {back.glVertexAttribDivisor(location, divisor);}
    public void glVertexAttribPointer(int location, int size, int type, boolean normalized, int stride, long pointer) {back.glVertexAttribPointer(location, size, type, normalized, stride, pointer);}
    public void glVertexAttribIPointer(int location, int size, int type, int stride, long pointer) {back.glVertexAttribIPointer(location, size, type, stride, pointer);}
    public void glViewport(int x, int y, int w, int h) {back.glViewport(x, y, w, h);}

    private static final Pattern joglerrp = Pattern.compile("GL-Error 0x([0-9a-fA-F]+)\\s");
    public static void xlatejoglexc(RuntimeException exc) {
	/* How nice wouldn't it be if DebugGL could be
	 * subclasseed to customize the errors. */
	if(exc instanceof com.jogamp.opengl.GLException) {
	    String msg = exc.getMessage();
	    GLException wrap = null;
	    if(msg.indexOf("GL_INVALID_ENUM") >= 0) {
		wrap = new GLException.GLInvalidEnumException();
	    } else if(msg.indexOf("GL_INVALID_VALUE") >= 0) {
		wrap = new GLException.GLInvalidValueException();
	    } else if(msg.indexOf("GL_INVALID_OPERATION") >= 0) {
		wrap = new GLException.GLInvalidOperationException();
	    } else if(msg.indexOf("GL_OUT_OF_MEMORY") >= 0) {
		wrap = new GLException.GLOutOfMemoryException();
	    } else {
		Matcher m = joglerrp.matcher(msg);
		if(m.find()) {
		    try {
			wrap = GLException.glexcfor(Integer.parseInt(m.group(1), 16));
		    } catch(NumberFormatException e) {
			exc.addSuppressed(e);
		    }
		}
	    }
	    if(wrap != null) {
		wrap.initCause(exc);
		throw(wrap);
	    }
	}
    }

    public void xlateexc(RuntimeException exc) {
	xlatejoglexc(exc);
    }
}
