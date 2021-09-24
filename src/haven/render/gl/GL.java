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

public interface GL {
    public static final int GL_INVALID_ENUM = com.jogamp.opengl.GL.GL_INVALID_ENUM;
    public static final int GL_INVALID_VALUE = com.jogamp.opengl.GL.GL_INVALID_VALUE;
    public static final int GL_INVALID_OPERATION = com.jogamp.opengl.GL.GL_INVALID_OPERATION;
    public static final int GL_OUT_OF_MEMORY = com.jogamp.opengl.GL.GL_OUT_OF_MEMORY;

    public void glActiveTexture(int texture);
    public void glAttachShader(int program, int shader);
    public void glBindAttribLocation(int program, int index, String name);
    public void glBindBuffer(int target, int buffer);
    public void glBindFramebuffer(int target, int buffer);
    public void glBindRenderbuffer(int target, int buffer);
    public void glBindTexture(int target, int texture);
    public void glBindVertexArray(int array);
    public void glBlendColor(float red, float green, float blue, float alpha);
    public void glBlendEquation(int mode);
    public void glBlendEquationSeparate(int cmode, int amode);
    public void glBlendFunc(int sfac, int dfac);
    public void glBlendFuncSeparate(int csfac, int cdfac, int asfac, int adfac);
    public void glBufferData(int target, long size, Buffer data, int usage);
    public void glBufferSubData(int target, long offset, long size, Buffer data);
    public void glClear(int mask);
    public void glClearColor(float r, float g, float b, float a);
    public void glClearDepth(double d);
    public void glColorMask(boolean r, boolean g, boolean b, boolean a);
    public void glColorMaski(int buf, boolean r, boolean g, boolean b, boolean a);
    public void glDeleteBuffers(int count, int[] buffers, int n);
    public void glDeleteFramebuffers(int count, int[] buffers, int n);
    public void glDeleteShader(int id);
    public void glDeleteProgram(int id);
    public void glDeleteRenderbuffers(int count, int[] buffers, int n);
    public void glDeleteTextures(int count, int[] buffers, int n);
    public void glDeleteVertexArrays(int count, int[] buffers, int n);
    public void glCullFace(int mode);
    public void glDebugMessageControl(int source, int type, int severity, int count, int[] ids, boolean enabled);
    public void glDeleteSync(long id);
    public void glDepthFunc(int func);
    public void glDepthMask(boolean mask);
    public void glDisable(int cap);
    public void glDisablei(int cap, int index);
    public void glDisableClientState(int cap);
    public void glDisableVertexAttribArray(int location);
    public void glDisableVertexAttribArray(int location, int offset);
    public void glDrawBuffer(int buf);
    public void glDrawBuffers(int n, int[] bufs, int i);
    public void glDrawArraysInstanced(int mode, int first, int count, int primcount);
    public void glDrawArrays(int mode, int first, int count);
    public void glDrawElementsInstanced(int mode, int count, int type, long indices, int primcount);
    public void glDrawElements(int mode, int count, int type, long indices);
    public void glDrawRangeElements(int mode, int start, int end, int count, int type, long indices);
    public void glEnable(int cap);
    public void glEnablei(int cap, int index);
    public void glEnableClientState(int cap);
    public void glEnableVertexAttribArray(int location);
    public void glEnableVertexAttribArray(int location, int offset);
    public void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level);
    public void glFramebufferRenderbuffer(int target, int attachment, int rbtarget, int renderbuffer);
    public int glGetDebugMessageLog(int count, int bufsize, int[] sources, int[] types, int[] ids, int[] severities, int[] lengths, byte[] buffer);
    public int glGetError();
    public void glGetTexImage(int target, int level, int format, int type, Buffer pixels);
    public void glGetTexImage(int target, int level, int format, int type, long offset);
    public void glLineWidth(float w);
    public void glLinkProgram(int program);
    public void glObjectLabel(int identifier, int name, int length, byte[] label);
    public void glObjectLabel(int identifier, int name, String label);
    public void glPixelStorei(int pname, int param);
    public void glPointSize(float size);
    public void glPolygonMode(int face, int mode);
    public void glPolygonOffset(float factor, float units);
    public void glReadBuffer(int buf);
    public void glReadPixels(int x, int y, int width, int height, int format, int type, Buffer data);
    public void glReadPixels(int x, int y, int width, int height, int format, int type, long offset);
    public void glRenderbufferStorage(int target, int format, int width, int height);
    public void glRenderbufferStorageMultisample(int target, int samples, int format, int width, int height);
    public void glSampleCoverage(float value, boolean invert);
    public void glScissor(int x, int y, int w, int h);
    public void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, Buffer data);
    public void glTexSubImage2D(int target, int level, int xoff, int yoff, int width, int height, int format, int type, Buffer data);
    public void glTexImage2DMultisample(int target, int samples, int internalformat, int width, int height, boolean fixedsamplelocations);
    public void glTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int border, int format, int type, Buffer data);
    public void glTexSubImage3D(int target, int level, int xoff, int yoff, int zoff, int width, int height, int depth, int format, int type, Buffer data);
    public void glTexParameterf(int target, int pname, float param);
    public void glTexParameterfv(int target, int pname, float[] param, int n);
    public void glTexParameteri(int target, int pname, int param);
    public void glUniform1f(int location, float v0);
    public void glUniform2f(int location, float v0, float v1);
    public void glUniform3f(int location, float v0, float v1, float v2);
    public void glUniform3fv(int location, int count, float[] val, int n);
    public void glUniform4f(int location, float v0, float v1, float v2, float v3);
    public void glUniform4fv(int location, int count, float[] val, int n);
    public void glUniform1i(int location, int v0);
    public void glUniform2i(int location, int v0, int v1);
    public void glUniform3i(int location, int v0, int v1, int v2);
    public void glUniform4i(int location, int v0, int v1, int v2, int v3);
    public void glUniformMatrix3fv(int location, int count, boolean transpose, float[] value, int n);
    public void glUniformMatrix4fv(int location, int count, boolean transpose, float[] value, int n);
    public void glUseProgram(int program);
    public void glVertexAttribDivisor(int location, int divisor);
    public void glVertexAttribDivisor(int location, int offset, int divisor);
    public void glVertexAttribPointer(int location, int size, int type, boolean normalized, int stride, long pointer);
    public void glVertexAttribPointer(int location, int offset, int size, int type, boolean normalized, int stride, long pointer);
    public void glVertexAttribIPointer(int location, int size, int type, int stride, long pointer);
    public void glVertexAttribIPointer(int location, int offset, int size, int type, int stride, long pointer);
    public void glViewport(int x, int y, int w, int h);
}
