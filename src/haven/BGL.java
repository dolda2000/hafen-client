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
import java.nio.Buffer;

public class BGL {
    private static abstract class Command {
	public abstract void run(GL2 gl);
    }

    public interface ID {
	public int glid();
    }

    public interface Request {
	public void run(GL2 gl);
    }

    private Command[] list;
    private int n = 0;

    public BGL(int c) {
	list = new Command[c];
    }
    public BGL() {this(128);}

    public void run(GL2 gl) {
	for(int i = 0; i < n; i++)
	    list[i].run(gl);
    }

    private void add(Command cmd) {
	if(n >= list.length)
	    list = Utils.extend(list, list.length * 2);
	list[n++] = cmd;
    }

    public void bglCheckErr() {
	final Throwable place = null;
	add(new Command() {
		public void run(GL2 gl) {
		    try {
			GOut.checkerr(gl);
		    } catch(GOut.GLException e) {
			e.initCause(place);
			throw(e);
		    }
		}
	    });
    }

    public void bglCreate(final GLObject ob) {
	add(new Command() {
		public void run(GL2 gl) {ob.create(gl);}
	    });
    }

    public void bglSubmit(final Request req) {
	add(new Command() {
		public void run(GL2 gl) {req.run(gl);}
	    });
    }

    public void glActiveTexture(final int texture) {
	add(new Command() {
		public void run(GL2 gl) {gl.glActiveTexture(texture);}
	    });
    }

    public void glAttachShader(final ID program, final ID shader) {
	add(new Command() {
		public void run(GL2 gl) {gl.glAttachShader(program.glid(), shader.glid());}
	    });
    }

    public void glBegin(final int mode) {
	add(new Command() {
		public void run(GL2 gl) {gl.glBegin(mode);}
	    });
    }

    public void glBindBuffer(final int target, final ID buffer) {
	add(new Command() {
		public void run(GL2 gl) {gl.glBindTexture(target, (buffer == null)?0:buffer.glid());}
	    });
    }

    public void glBindTexture(final int target, final ID texture) {
	add(new Command() {
		public void run(GL2 gl) {gl.glBindTexture(target, (texture == null)?0:texture.glid());}
	    });
    }

    public void glBlendEquationSeparate(final int cmode, final int amode) {
	add(new Command() {
		public void run(GL2 gl) {gl.glBlendEquationSeparate(cmode, amode);}
	    });
    }

    public void glBlendFunc(final int sfac, final int dfac) {
	add(new Command() {
		public void run(GL2 gl) {gl.glBlendFunc(sfac, dfac);}
	    });
    }

    public void glBufferData(final int target, final long size, final Buffer data, final int usage) {
	add(new Command() {
		public void run(GL2 gl) {gl.glBufferData(target, size, data, usage);}
	    });
    }

    public void glClear(final int mask) {
	add(new Command() {
		public void run(GL2 gl) {gl.glClear(mask);}
	    });
    }

    public void glClearColor(final float r, final float g, final float b, final float a) {
	add(new Command() {
		public void run(GL2 gl) {gl.glClearColor(r, g, b, a);}
	    });
    }

    public void glColor3f(final float r, final float g, final float b) {
	add(new Command() {
		public void run(GL2 gl) {gl.glColor3f(r, g, b);}
	    });
    }

    public void glColor4f(final float r, final float g, final float b, final float a) {
	add(new Command() {
		public void run(GL2 gl) {gl.glColor4f(r, g, b, a);}
	    });
    }

    public void glColor4fv(final float[] v, final int n) {
	add(new Command() {
		public void run(GL2 gl) {gl.glColor4fv(v, n);}
	    });
    }

    public void glColorMask(final boolean r, final boolean g, final boolean b, final boolean a) {
	add(new Command() {
		public void run(GL2 gl) {gl.glColorMask(r, g, b, a);}
	    });
    }

    public void glCompileShaderARB(final ID shader) {
	add(new Command() {
		public void run(GL2 gl) {gl.glCompileShaderARB(shader.glid());}
	    });
    }

    public void glDeleteBuffers(final int count, final ID[] buffers, final int n) {
	add(new Command() {
		public void run(GL2 gl) {
		    int[] buf = new int[buffers.length];
		    for(int i = 0; i < buf.length; i++)
			buf[i] = buffers[i].glid();
		    gl.glDeleteBuffers(count, buf, n);
		}
	    });
    }

    public void glDeleteTextures(final int count, final ID[] buffers, final int n) {
	add(new Command() {
		public void run(GL2 gl) {
		    int[] buf = new int[buffers.length];
		    for(int i = 0; i < buf.length; i++)
			buf[i] = buffers[i].glid();
		    gl.glDeleteTextures(count, buf, n);
		}
	    });
    }

    public void glDeleteObjectARB(final ID id) {
	add(new Command() {
		public void run(GL2 gl) {gl.glDeleteObjectARB(id.glid());}
	    });
    }

    public void glDepthMask(final boolean mask) {
	add(new Command() {
		public void run(GL2 gl) {gl.glDepthMask(mask);}
	    });
    }

    public void glDisable(final int cap) {
	add(new Command() {
		public void run(GL2 gl) {gl.glDisable(cap);}
	    });
    }

    public void glDisableVertexAttribArray(final ID location) {
	add(new Command() {
		public void run(GL2 gl) {gl.glDisableVertexAttribArray(location.glid());}
	    });
    }

    public void glDisableVertexAttribArray(final ID location, final int offset) {
	add(new Command() {
		public void run(GL2 gl) {gl.glDisableVertexAttribArray(location.glid() + offset);}
	    });
    }

    public void glEnable(final int cap) {
	add(new Command() {
		public void run(GL2 gl) {gl.glEnable(cap);}
	    });
    }

    public void glEnableVertexAttribArray(final ID location) {
	add(new Command() {
		public void run(GL2 gl) {gl.glEnableVertexAttribArray(location.glid());}
	    });
    }

    public void glEnableVertexAttribArray(final ID location, final int offset) {
	add(new Command() {
		public void run(GL2 gl) {gl.glEnableVertexAttribArray(location.glid() + offset);}
	    });
    }

    public void glEnd() {
	add(new Command() {
		public void run(GL2 gl) {gl.glEnd();}
	    });
    }

    public void glFogi(final int pname, final int param) {
	add(new Command() {
		public void run(GL2 gl) {gl.glFogi(pname, param);}
	    });
    }

    public void glFogf(final int pname, final float param) {
	add(new Command() {
		public void run(GL2 gl) {gl.glFogf(pname, param);}
	    });
    }

    public void glFogfv(final int pname, final float[] param, final int n) {
	add(new Command() {
		public void run(GL2 gl) {gl.glFogfv(pname, param, n);}
	    });
    }

    public void glLineWidth(final float w) {
	add(new Command() {
		public void run(GL2 gl) {gl.glLineWidth(w);}
	    });
    }

    public void glLinkProgram(final ID program) {
	add(new Command() {
		public void run(GL2 gl) {gl.glLinkProgram(program.glid());}
	    });
    }

    public void glLoadMatrixf(final float[] m, final int i) {
	add(new Command() {
		public void run(GL2 gl) {gl.glLoadMatrixf(m, i);}
	    });
    }

    public void glMatrixMode(final int mode) {
	add(new Command() {
		public void run(GL2 gl) {gl.glMatrixMode(mode);}
	    });
    }

    public void glPixelStorei(final int pname, final int param) {
	add(new Command() {
		public void run(GL2 gl) {gl.glPixelStorei(pname, param);}
	    });
    }

    public void glPointSize(final float size) {
	add(new Command() {
		public void run(GL2 gl) {gl.glPointSize(size);}
	    });
    }

    public void glPolygonMode(final int face, final int mode) {
	add(new Command() {
		public void run(GL2 gl) {gl.glPolygonMode(face, mode);}
	    });
    }

    public void glPolygonOffset(final float factor, final float units) {
	add(new Command() {
		public void run(GL2 gl) {gl.glPolygonOffset(factor, units);}
	    });
    }

    public void glSampleCoverage(final float value, final boolean invert) {
	add(new Command() {
		public void run(GL2 gl) {gl.glSampleCoverage(value, invert);}
	    });
    }

    public void glShaderSourceARB(final ID shader, final int count, final String[] string, final int[] length, final int n) {
	add(new Command() {
		public void run(GL2 gl) {gl.glShaderSourceARB(shader.glid(), count, string, length, n);}
	    });
    }

    public void glTexCoord2f(final float s, final float t) {
	add(new Command() {
		public void run(GL2 gl) {gl.glTexCoord2f(s, t);}
	    });
    }

    public void glTexImage2D(final int target, final int level, final int internalformat, final int width, final int height, final int border, final int format, final int type, final Buffer data) {
	add(new Command() {
		public void run(GL2 gl) {gl.glTexImage2D(target, level, internalformat, width, height, border, format, type, data);}
	    });
    }

    public void glTexSubImage2D(final int target, final int level, final int xoff, final int yoff, final int width, final int height, final int format, final int type, final Buffer data) {
	add(new Command() {
		public void run(GL2 gl) {gl.glTexSubImage2D(target, level, xoff, yoff, width, height, format, type, data);}
	    });
    }

    public void glTexParameterf(final int target, final int pname, final float param) {
	add(new Command() {
		public void run(GL2 gl) {gl.glTexParameterf(target, pname, param);}
	    });
    }

    public void glTexParameteri(final int target, final int pname, final int param) {
	add(new Command() {
		public void run(GL2 gl) {gl.glTexParameteri(target, pname, param);}
	    });
    }

    public void glUniform1i(final ID location, final int v0) {
	add(new Command() {
		public void run(GL2 gl) {gl.glUniform1i(location.glid(), v0);}
	    });
    }

    public void glUniformMatrix4fv(final ID location, final int count, final boolean transpose, final float[] value, final int n) {
	add(new Command() {
		public void run(GL2 gl) {gl.glUniformMatrix4fv(location.glid(), count, transpose, value, n);}
	    });
    }

    public void glUseProgramObjectARB(final ID program) {
	add(new Command() {
		public void run(GL2 gl) {gl.glUseProgramObjectARB(program.glid());}
	    });
    }

    public void glVertex2f(final float x, final float y) {
	add(new Command() {
		public void run(GL2 gl) {gl.glVertex2f(x, y);}
	    });
    }

    public void glVertex2i(final int x, final int y) {
	add(new Command() {
		public void run(GL2 gl) {gl.glVertex2i(x, y);}
	    });
    }

    public void glVertex3i(final int x, final int y, final int z) {
	add(new Command() {
		public void run(GL2 gl) {gl.glVertex3i(x, y, z);}
	    });
    }

    public void glVertexAttribDivisor(final ID location, final int divisor) {
	add(new Command() {
		public void run(GL2 gl) {((GL3)gl).glVertexAttribDivisor(location.glid(), divisor);}
	    });
    }

    public void glVertexAttribDivisor(final ID location, final int offset, final int divisor) {
	add(new Command() {
		public void run(GL2 gl) {((GL3)gl).glVertexAttribDivisor(location.glid() + offset, divisor);}
	    });
    }

    public void glVertexAttribPointer(final ID location, final int size, final int type, final boolean normalized, final int stride, final long pointer) {
	add(new Command() {
		public void run(GL2 gl) {gl.glVertexAttribPointer(location.glid(), size, type, normalized, stride, pointer);}
	    });
    }

    public void glVertexAttribPointer(final ID location, final int offset, final int size, final int type, final boolean normalized, final int stride, final long pointer) {
	add(new Command() {
		public void run(GL2 gl) {gl.glVertexAttribPointer(location.glid() + offset, size, type, normalized, stride, pointer);}
	    });
    }

    public void joglSetSwapInterval(final int swap) {
	add(new Command() {
		public void run(GL2 gl) {gl.setSwapInterval(swap);}
	    });
    }
}
