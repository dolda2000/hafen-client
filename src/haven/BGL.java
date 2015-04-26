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
	add(new Command() {
		public void run(GL2 gl) {GOut.checkerr(gl);}
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

    public void glBindTexture(final int target, final int texture) {
	add(new Command() {
		public void run(GL2 gl) {gl.glBindTexture(target, texture);}
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

    public void glDeleteObjectARB(final int id) {
	add(new Command() {
		public void run(GL2 gl) {gl.glDeleteObjectARB(id);}
	    });
    }

    public void glDisable(final int cap) {
	add(new Command() {
		public void run(GL2 gl) {gl.glDisable(cap);}
	    });
    }

    public void glEnable(final int cap) {
	add(new Command() {
		public void run(GL2 gl) {gl.glEnable(cap);}
	    });
    }

    public void glEnd() {
	add(new Command() {
		public void run(GL2 gl) {gl.glEnd();}
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

    public void glPointSize(final float size) {
	add(new Command() {
		public void run(GL2 gl) {gl.glPointSize(size);}
	    });
    }

    public void glTexCoord2f(final float s, final float t) {
	add(new Command() {
		public void run(GL2 gl) {gl.glTexCoord2f(s, t);}
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

    public void joglSetSwapInterval(final int swap) {
	add(new Command() {
		public void run(GL2 gl) {gl.setSwapInterval(swap);}
	    });
    }
}
