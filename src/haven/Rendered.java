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

public interface Rendered {
    public void draw(GOut g);
    public boolean setup(RenderList r);
    
    public static class Dot implements Rendered {
	public void draw(GOut g) {
	    GL gl = g.gl;
	    g.matsel(null);
	    gl.glDisable(GL.GL_LIGHTING);
	    gl.glDisable(GL.GL_DEPTH_TEST);
	    gl.glBegin(GL.GL_POINTS);
	    gl.glColor3f(1.0f, 0.0f, 0.0f);
	    gl.glVertex3f(0.0f, 0.0f, 0.0f);
	    gl.glEnd();
	    gl.glEnable(GL.GL_LIGHTING);
	    gl.glEnable(GL.GL_DEPTH_TEST);
	}
	
	public boolean setup(RenderList r) {
	    return(true);
	}
    }

    public static class Cube implements Rendered {
	public void draw(GOut g) {
	    GL gl = g.gl;
	    g.matsel(null);
	    
	    gl.glEnable(GL.GL_COLOR_MATERIAL);
	    gl.glBegin(GL.GL_QUADS);
	    gl.glNormal3f(0.0f, 0.0f, 1.0f);
	    gl.glColor3f(0.0f, 0.0f, 1.0f);
	    gl.glVertex3f(-1.0f, 1.0f, 1.0f);
	    gl.glVertex3f(-1.0f, -1.0f, 1.0f);
	    gl.glVertex3f(1.0f, -1.0f, 1.0f);
	    gl.glVertex3f(1.0f, 1.0f, 1.0f);

	    gl.glNormal3f(1.0f, 0.0f, 0.0f);
	    gl.glColor3f(1.0f, 0.0f, 0.0f);
	    gl.glVertex3f(1.0f, 1.0f, 1.0f);
	    gl.glVertex3f(1.0f, -1.0f, 1.0f);
	    gl.glVertex3f(1.0f, -1.0f, -1.0f);
	    gl.glVertex3f(1.0f, 1.0f, -1.0f);

	    gl.glNormal3f(-1.0f, 0.0f, 0.0f);
	    gl.glColor3f(0.0f, 1.0f, 1.0f);
	    gl.glVertex3f(-1.0f, 1.0f, 1.0f);
	    gl.glVertex3f(-1.0f, 1.0f, -1.0f);
	    gl.glVertex3f(-1.0f, -1.0f, -1.0f);
	    gl.glVertex3f(-1.0f, -1.0f, 1.0f);

	    gl.glNormal3f(0.0f, 1.0f, 0.0f);
	    gl.glColor3f(0.0f, 1.0f, 0.0f);
	    gl.glVertex3f(-1.0f, 1.0f, 1.0f);
	    gl.glVertex3f(1.0f, 1.0f, 1.0f);
	    gl.glVertex3f(1.0f, 1.0f, -1.0f);
	    gl.glVertex3f(-1.0f, 1.0f, -1.0f);

	    gl.glNormal3f(0.0f, -1.0f, 0.0f);
	    gl.glColor3f(1.0f, 0.0f, 1.0f);
	    gl.glVertex3f(-1.0f, -1.0f, 1.0f);
	    gl.glVertex3f(-1.0f, -1.0f, -1.0f);
	    gl.glVertex3f(1.0f, -1.0f, -1.0f);
	    gl.glVertex3f(1.0f, -1.0f, 1.0f);

	    gl.glNormal3f(0.0f, 0.0f, -1.0f);
	    gl.glColor3f(1.0f, 1.0f, 0.0f);
	    gl.glVertex3f(-1.0f, 1.0f, -1.0f);
	    gl.glVertex3f(1.0f, 1.0f, -1.0f);
	    gl.glVertex3f(1.0f, -1.0f, -1.0f);
	    gl.glVertex3f(-1.0f, -1.0f, -1.0f);
	    gl.glEnd();
	    gl.glColor3f(1.0f, 1.0f, 1.0f);
	    gl.glDisable(GL.GL_COLOR_MATERIAL);
	}
	
	public boolean setup(RenderList rls) {
	    return(true);
	}
    }
}
