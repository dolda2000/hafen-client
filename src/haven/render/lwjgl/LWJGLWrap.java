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

public class LWJGLWrap {
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
    public void glBufferData(int target, long size, ByteBuffer data, int usage) {GL30.glBufferData(target, ckbuf(data, size), usage);}
    public void glBufferSubData(int target, long offset, long size, ByteBuffer data) {GL30.glBufferSubData(target, offset, ckbuf(data, size));}
    public int glCheckFramebufferStatus(int target) {return(GL30.glCheckFramebufferStatus(target));}
    public void glClear(int mask) {GL30.glClear(mask);}
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
}
