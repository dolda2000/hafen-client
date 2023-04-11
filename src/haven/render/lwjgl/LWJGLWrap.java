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

package haven.render.lwjgl;

import java.nio.*;
import haven.render.gl.GL;
import org.lwjgl.opengl.*;

public class LWJGLWrap implements GL {
    public static final LWJGLWrap instance = new LWJGLWrap();

    private static ByteBuffer ckbuf(ByteBuffer buf, long size) {
	if(buf.remaining() != size)
	    throw(new AssertionError(buf.remaining() + " != " + size));
	return(buf);
    }

    private static int[] ckbuf(int[] buf, int size) {
	if(buf.length != size)
	    throw(new AssertionError(buf.length + " != " + size));
	return(buf);
    }
    private static float[] ckbuf(float[] buf, int size) {
	if(buf.length != size)
	    throw(new AssertionError(buf.length + " != " + size));
	return(buf);
    }
    private static String[] cksrcbuf(int count, String[] string, int[] lengths) {
	if(string.length != count)
	    throw(new AssertionError(string.length + " != " + count));
	if(lengths.length != count)
	    throw(new AssertionError(lengths.length + " != " + count));
	for(int i = 0; i < count; i++) {
	    if(lengths[i] != string[i].length())
		throw(new AssertionError(string[i].length() + " != " + lengths[i]));
	}
	return(string);
    }

    public void glActiveTexture(int texture) {GL30.glActiveTexture(texture);}
    public void glAttachShader(int program, int shader) {GL30.glAttachShader(program, shader);}
    public void glBindAttribLocation(int program, int index, String name) {GL30.glBindAttribLocation(program, index, name);}
    public void glBindBuffer(int target, int buffer) {GL30.glBindBuffer(target, buffer);}
    public void glBindFragDataLocation(int program, int colornumber, String name) {GL30.glBindFragDataLocation(program, colornumber, name);}
    public void glBindFramebuffer(int target, int buffer) {GL30.glBindFramebuffer(target, buffer);}
    public void glBindRenderbuffer(int target, int buffer) {GL30.glBindRenderbuffer(target, buffer);}
    public void glBindTexture(int target, int texture) {GL30.glBindTexture(target, texture);}
    public void glBindVertexArray(int array) {GL30.glBindVertexArray(array);}
    public void glBlendColor(float red, float green, float blue, float alpha) {GL30.glBlendColor(red, green, blue, alpha);}
    public void glBlendEquation(int mode) {GL30.glBlendEquation(mode);}
    public void glBlendEquationSeparate(int cmode, int amode) {GL30.glBlendEquationSeparate(cmode, amode);}
    public void glBlendFunc(int sfac, int dfac) {GL30.glBlendFunc(sfac, dfac);}
    public void glBlendFuncSeparate(int csfac, int cdfac, int asfac, int adfac) {GL30.glBlendFuncSeparate(csfac, cdfac, asfac, adfac);}
    public void glBufferData(int target, long size, ByteBuffer data, int usage) {
	if(data == null)
	    GL30.glBufferData(target, size, usage);
	else
	    GL30.glBufferData(target, ckbuf(data, size), usage);
    }
    public void glBufferSubData(int target, long offset, long size, ByteBuffer data) {GL30.glBufferSubData(target, offset, ckbuf(data, size));}
    public int glCheckFramebufferStatus(int target) {return(GL30.glCheckFramebufferStatus(target));}
    public void glClear(int mask) {GL30.glClear(mask);}
    public void glClearBufferfv(int buffer, int drawbuffer, float[] value) {GL30.glClearBufferfv(buffer, drawbuffer, value);}
    public void glClearBufferiv(int buffer, int drawbuffer, int[] value) {GL30.glClearBufferiv(buffer, drawbuffer, value);}
    public void glClearBufferuiv(int buffer, int drawbuffer, int[] value) {GL30.glClearBufferuiv(buffer, drawbuffer, value);}
    public void glClearColor(float r, float g, float b, float a) {GL30.glClearColor(r, g, b, a);}
    public void glClearDepth(double d) {GL30.glClearDepth(d);}
    public void glColorMask(boolean r, boolean g, boolean b, boolean a) {GL30.glColorMask(r, g, b, a);}
    public void glColorMaski(int buf, boolean r, boolean g, boolean b, boolean a) {GL30.glColorMaski(buf, r, g, b, a);}
    public void glCompileShader(int shader) {GL30.glCompileShader(shader);}
    public int glCreateProgram() {return(GL30.glCreateProgram());}
    public int glCreateShader(int type) {return(GL30.glCreateShader(type));}
    public void glCullFace(int mode) {GL30.glCullFace(mode);}
    public void glDeleteBuffers(int count, int[] buffers) {GL30.glDeleteBuffers(ckbuf(buffers, count));}
    public void glDeleteFramebuffers(int count, int[] buffers) {GL30.glDeleteFramebuffers(ckbuf(buffers, count));}
    public void glDeleteShader(int id) {GL30.glDeleteShader(id);}
    public void glDeleteQueries(int count, int[] buffer) {GL30.glDeleteQueries(ckbuf(buffer, count));}
    public void glDeleteProgram(int id) {GL30.glDeleteProgram(id);}
    public void glDeleteRenderbuffers(int count, int[] buffers) {GL30.glDeleteRenderbuffers(ckbuf(buffers, count));}
    public void glDeleteTextures(int count, int[] buffers) {GL30.glDeleteTextures(ckbuf(buffers, count));}
    public void glDeleteVertexArrays(int count, int[] buffers) {GL30.glDeleteVertexArrays(ckbuf(buffers, count));}
    public void glDebugMessageControl(int source, int type, int severity, int count, int[] ids, boolean enabled) {GL43.glDebugMessageControl(source, type, severity, ckbuf(ids, count), enabled);}
    public void glDeleteSync(long id) {GL32.glDeleteSync(id);}
    public void glDepthFunc(int func) {GL30.glDepthFunc(func);}
    public void glDepthMask(boolean mask) {GL30.glDepthMask(mask);}
    public void glDisable(int cap) {GL30.glDisable(cap);}
    public void glDisablei(int cap, int index) {GL30.glDisablei(cap, index);}
    public void glDisableClientState(int cap) {GL30.glDisableClientState(cap);}
    public void glDisableVertexAttribArray(int location) {GL30.glDisableVertexAttribArray(location);}
    public void glDrawBuffer(int buf) {GL30.glDrawBuffer(buf);}
    public void glDrawBuffers(int n, int[] bufs) {GL30.glDrawBuffers(ckbuf(bufs, n));}
    public void glDrawArraysInstanced(int mode, int first, int count, int primcount) {GL31.glDrawArraysInstanced(mode, first, count, primcount);}
    public void glDrawArrays(int mode, int first, int count) {GL30.glDrawArrays(mode, first, count);}
    public void glDrawElementsInstanced(int mode, int count, int type, long indices, int primcount) {GL31.glDrawElementsInstanced(mode, count, type, indices, primcount);}
    public void glDrawElements(int mode, int count, int type, long indices) {GL30.glDrawElements(mode, count, type, indices);}
    public void glDrawRangeElements(int mode, int start, int end, int count, int type, long indices) {GL30.glDrawRangeElements(mode, start, end, count, type, indices);}
    public void glEnable(int cap) {GL30.glEnable(cap);}
    public void glEnablei(int cap, int index) {GL30.glEnablei(cap, index);}
    public void glEnableClientState(int cap) {GL30.glEnableClientState(cap);}
    public void glEnableVertexAttribArray(int location) {GL30.glEnableVertexAttribArray(location);}
    public long glFenceSync(int condition, int flags) {return(GL32.glFenceSync(condition, flags));}
    public void glFinish() {GL30.glFinish();}
    public void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level) {GL30.glFramebufferTexture2D(target, attachment, textarget, texture, level);}
    public void glFramebufferRenderbuffer(int target, int attachment, int rbtarget, int renderbuffer) {GL30.glFramebufferRenderbuffer(target, attachment, rbtarget, renderbuffer);}
    public void glGenBuffers(int n, int[] buffer) {GL30.glGenBuffers(ckbuf(buffer, n));}
    public void glGenFramebuffers(int n, int[] buffer) {GL30.glGenFramebuffers(ckbuf(buffer, n));}
    public void glGenQueries(int n, int[] buffer) {GL30.glGenQueries(ckbuf(buffer, n));}
    public void glGenTextures(int n, int[] buffer) {GL30.glGenTextures(ckbuf(buffer, n));}
    public void glGenVertexArrays(int n, int[] buffer) {GL30.glGenVertexArrays(ckbuf(buffer, n));}
    public void glGetBufferSubData(int target, int offset, int size, ByteBuffer data) {GL30.glGetBufferSubData(target, offset, ckbuf(data, size));}
    public int glGetDebugMessageLog(int count, int bufsize, int[] sources, int[] types, int[] ids, int[] severities, int[] lengths, byte[] buffer) {return(GL43.glGetDebugMessageLog(count, ckbuf(sources, count), ckbuf(types, count), ckbuf(ids, count), ckbuf(severities, count), ckbuf(lengths, count), ByteBuffer.wrap(buffer)));}
    public int glGetError() {return(GL30.glGetError());}
    public void glGetFloatv(int pname, float[] data) {GL30.glGetFloatv(pname, data);}
    public void glGetIntegerv(int pname, int[] data) {GL30.glGetIntegerv(pname, data);}
    public String glGetString(int name) {return(GL30.glGetString(name));}
    public String glGetStringi(int name, int index) {return(GL30.glGetStringi(name, index));}
    public void glGetProgramInfoLog(int shader, int maxlength, int[] length, byte[] infolog) {GL30.glGetProgramInfoLog(shader, length, ckbuf(ByteBuffer.wrap(infolog), maxlength));}
    public void glGetProgramiv(int shader, int pname, int[] buf) {GL30.glGetProgramiv(shader, pname, buf);}
    public void glGetQueryObjectiv(int id, int pname, int[] params) {GL30.glGetQueryObjectiv(id, pname, params);}
    public void glGetQueryObjecti64v(int id, int pname, long[] params) {GL33.glGetQueryObjecti64v(id, pname, params);}
    public void glGetShaderInfoLog(int shader, int maxlength, int[] length, byte[] infolog) {GL30.glGetShaderInfoLog(shader, length, ckbuf(ByteBuffer.wrap(infolog), maxlength));}
    public void glGetShaderiv(int shader, int pname, int[] buf) {GL30.glGetShaderiv(shader, pname, buf);}
    public void glGetSynciv(long sync, int pname, int bufsize, int[] lengths, int[] values) {GL32.glGetSynciv(sync, pname, lengths, ckbuf(values, bufsize));}
    public void glGetTexImage(int target, int level, int format, int type, ByteBuffer pixels) {GL30.glGetTexImage(target, level, format, type, pixels);}
    public void glGetTexImage(int target, int level, int format, int type, long offset) {GL30.glGetTexImage(target, level, format, type, offset);}
    public int glGetUniformLocation(int program, String name) {return(GL30.glGetUniformLocation(program, name));}
    public void glLineWidth(float w) {GL30.glLineWidth(w);}
    public void glLinkProgram(int program) {GL30.glLinkProgram(program);}
    public void glObjectLabel(int identifier, int name, int length, byte[] label) {GL43.glObjectLabel(identifier, name, ckbuf(ByteBuffer.wrap(label), length));}
    public void glPixelStorei(int pname, int param) {GL30.glPixelStorei(pname, param);}
    public void glPointSize(float size) {GL30.glPointSize(size);}
    public void glPolygonMode(int face, int mode) {GL30.glPolygonMode(face, mode);}
    public void glPolygonOffset(float factor, float units) {GL30.glPolygonOffset(factor, units);}
    public void glQueryCounter(int id, int target) {GL33.glQueryCounter(id, target);}
    public void glReadBuffer(int buf) {GL30.glReadBuffer(buf);}
    public void glReadPixels(int x, int y, int width, int height, int format, int type, ByteBuffer data) {GL30.glReadPixels(x, y, width, height, format, type, data);}
    public void glReadPixels(int x, int y, int width, int height, int format, int type, long offset) {GL30.glReadPixels(x, y, width, height, format, type, offset);}
    public void glRenderbufferStorage(int target, int format, int width, int height) {GL30.glRenderbufferStorage(target, format, width, height);}
    public void glRenderbufferStorageMultisample(int target, int samples, int format, int width, int height) {GL30.glRenderbufferStorageMultisample(target, samples, format, width, height);}
    public void glSampleCoverage(float value, boolean invert) {GL30.glSampleCoverage(value, invert);}
    public void glShaderSource(int shader, int count, String[] string, int[] lengths) {GL30.glShaderSource(shader, cksrcbuf(count, string, lengths));}
    public void glScissor(int x, int y, int w, int h) {GL30.glScissor(x, y, w, h);}
    public void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, ByteBuffer data) {GL30.glTexImage2D(target, level, internalformat, width, height, border, format, type, data);}
    public void glTexSubImage2D(int target, int level, int xoff, int yoff, int width, int height, int format, int type, ByteBuffer data) {GL30.glTexSubImage2D(target, level, xoff, yoff, width, height, format, type, data);}
    public void glTexImage2DMultisample(int target, int samples, int internalformat, int width, int height, boolean fixedsamplelocations) {GL32.glTexImage2DMultisample(target, samples, internalformat, width, height, fixedsamplelocations);}
    public void glTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int border, int format, int type, ByteBuffer data) {GL30.glTexImage3D(target, level, internalformat, width, height, depth, border, format, type, data);}
    public void glTexSubImage3D(int target, int level, int xoff, int yoff, int zoff, int width, int height, int depth, int format, int type, ByteBuffer data) {GL30.glTexSubImage3D(target, level, xoff, yoff, zoff, width, height, depth, format, type, data);}
    public void glTexParameterf(int target, int pname, float param) {GL30.glTexParameterf(target, pname, param);}
    public void glTexParameterfv(int target, int pname, float[] param) {GL30.glTexParameterfv(target, pname, param);}
    public void glTexParameteri(int target, int pname, int param) {GL30.glTexParameteri(target, pname, param);}
    public void glUniform1f(int location, float v0) {GL30.glUniform1f(location, v0);}
    public void glUniform2f(int location, float v0, float v1) {GL30.glUniform2f(location, v0, v1);}
    public void glUniform3f(int location, float v0, float v1, float v2) {GL30.glUniform3f(location, v0, v1, v2);}
    public void glUniform3fv(int location, int count, float[] val) {GL30.glUniform3fv(location, ckbuf(val, count));}
    public void glUniform4f(int location, float v0, float v1, float v2, float v3) {GL30.glUniform4f(location, v0, v1, v2, v3);}
    public void glUniform4fv(int location, int count, float[] val) {GL30.glUniform4fv(location, ckbuf(val, count));}
    public void glUniform1i(int location, int v0) {GL30.glUniform1i(location, v0);}
    public void glUniform2i(int location, int v0, int v1) {GL30.glUniform2i(location, v0, v1);}
    public void glUniform3i(int location, int v0, int v1, int v2) {GL30.glUniform3i(location, v0, v1, v2);} 
    public void glUniform4i(int location, int v0, int v1, int v2, int v3) {GL30.glUniform4i(location, v0, v1, v2, v3);}
    public void glUniformMatrix3fv(int location, int count, boolean transpose, float[] value) {GL30.glUniformMatrix3fv(location, transpose, ckbuf(value, count * 9));}
    public void glUniformMatrix4fv(int location, int count, boolean transpose, float[] value) {GL30.glUniformMatrix4fv(location, transpose, ckbuf(value, count * 16));}
    public void glUseProgram(int program) {GL30.glUseProgram(program);}
    public void glVertexAttribDivisor(int location, int divisor) {GL33.glVertexAttribDivisor(location, divisor);}
    public void glVertexAttribPointer(int location, int size, int type, boolean normalized, int stride, long pointer) {GL30.glVertexAttribPointer(location, size, type, normalized, stride, pointer);}
    public void glVertexAttribIPointer(int location, int size, int type, int stride, long pointer) {GL30.glVertexAttribIPointer(location, size, type, stride, pointer);}
    public void glViewport(int x, int y, int w, int h) {GL30.glViewport(x, y, w, h);}
}
