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

package haven.render;

import haven.*;
import static haven.GOut.checkerr;
import javax.media.opengl.*;

public class PView extends Widget {
    float a = 0.0f;
    Coord3f camo = Coord3f.o;
    float camd = 5.0f, came, cama;
    
    public PView(Coord c, Coord sz, Widget parent) {
	super(c, sz, parent);
    }
    
    private void cube(GL gl) {
	gl.glBegin(gl.GL_QUADS);
	gl.glNormal3f(0.0f, 0.0f, 1.0f);
	gl.glColor3f(1.0f, 0.0f, 0.0f);
	gl.glVertex3f(-1.0f, 1.0f, 1.0f);
	gl.glVertex3f(-1.0f, -1.0f, 1.0f);
	gl.glVertex3f(1.0f, -1.0f, 1.0f);
	gl.glVertex3f(1.0f, 1.0f, 1.0f);

	gl.glNormal3f(1.0f, 0.0f, 0.0f);
	gl.glColor3f(0.0f, 1.0f, 0.0f);
	gl.glVertex3f(1.0f, 1.0f, 1.0f);
	gl.glVertex3f(1.0f, -1.0f, 1.0f);
	gl.glVertex3f(1.0f, -1.0f, -1.0f);
	gl.glVertex3f(1.0f, 1.0f, -1.0f);

	gl.glNormal3f(-1.0f, 0.0f, 0.0f);
	gl.glColor3f(0.0f, 0.0f, 1.0f);
	gl.glVertex3f(-1.0f, 1.0f, 1.0f);
	gl.glVertex3f(-1.0f, 1.0f, -1.0f);
	gl.glVertex3f(-1.0f, -1.0f, -1.0f);
	gl.glVertex3f(-1.0f, -1.0f, 1.0f);

	gl.glNormal3f(0.0f, 1.0f, 0.0f);
	gl.glColor3f(0.0f, 1.0f, 1.0f);
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
    }

    public void render(GOut g) {
	GL gl = g.gl;
	gl.glPushMatrix();
	gl.glTranslatef(-1.5f, 0.0f, 0.0f);
	//gl.glRotatef(a, 1.0f, 1.0f, 1.0f);
	cube(gl);
	gl.glPopMatrix();

	gl.glPushMatrix();
	gl.glTranslatef(1.5f, 0.0f, 0.0f);
	//gl.glRotatef(a, 1.0f, 1.0f, 1.0f);
	cube(gl);
	gl.glPopMatrix();
    }

    protected void setcam(GOut g) {
	GL gl = g.gl;
	camo = Coord3f.o.sadd(0, a, 1.5f);
	a += 0.1;
	gl.glTranslatef(0.0f, 0.0f, -camd);
	gl.glRotatef(90.0f - (float)(came * 180.0 / Math.PI), -1.0f, 0.0f, 0.0f);
	gl.glRotatef(90.0f + (float)(cama * 180.0 / Math.PI), 0.0f, 0.0f, -1.0f);
	gl.glTranslatef(-camo.x, -camo.y, -camo.z);
    }

    public void draw(GOut g) {
	GL gl = g.gl;
	g.texsel(-1);
	gl.glScissor(g.ul.x, ui.root.sz.y - g.ul.y - g.sz.y, g.sz.x, g.sz.y);
	gl.glViewport(g.ul.x, ui.root.sz.y - g.ul.y - g.sz.y, g.sz.x, g.sz.y);
	gl.glMatrixMode(gl.GL_MODELVIEW);
	gl.glPushMatrix();
	gl.glLoadIdentity();
	gl.glMatrixMode(gl.GL_PROJECTION);
	gl.glPushMatrix();
	gl.glLoadIdentity();
	gl.glEnable(gl.GL_DEPTH_TEST);
	gl.glEnable(gl.GL_CULL_FACE);
	gl.glEnable(gl.GL_SCISSOR_TEST);
	gl.glEnable(gl.GL_LIGHTING);
	gl.glEnable(gl.GL_LIGHT0);
	gl.glEnable(gl.GL_COLOR_MATERIAL);
	gl.glClearDepth(1.0);
	gl.glDepthFunc(gl.GL_LEQUAL);
	gl.glClear(gl.GL_DEPTH_BUFFER_BIT | gl.GL_COLOR_BUFFER_BIT);
	try {
	    checkerr(gl);
	    double r = ((double)sz.y) / ((double)sz.x);
	    gl.glFrustum(-1, 1, -r, r, 1, 50);
	    gl.glMatrixMode(gl.GL_MODELVIEW);
	    setcam(g);
	    render(g);
	} finally {
	    gl.glMatrixMode(gl.GL_PROJECTION);
	    gl.glPopMatrix();
	    gl.glMatrixMode(gl.GL_MODELVIEW);
	    gl.glPopMatrix();
	    gl.glDisable(gl.GL_DEPTH_TEST);
	    gl.glDisable(gl.GL_CULL_FACE);
	    gl.glDisable(gl.GL_SCISSOR_TEST);
	    gl.glDisable(gl.GL_LIGHTING);
	    gl.glDisable(gl.GL_LIGHT0);
	    gl.glDisable(gl.GL_COLOR_MATERIAL);
	    gl.glViewport(g.root().ul.x, g.root().ul.y, g.root().sz.x, g.root().sz.y);
	    gl.glScissor(g.root().ul.x, g.root().ul.y, g.root().sz.x, g.root().sz.y);
	}
	checkerr(gl);
    }
    
    public void mousemove(Coord c) {
	if(c.x < 0 || c.x >= sz.x || c.y < 0 || c.y >= sz.y)
	    return;
	came = (float)Math.PI / 2 * ((float)c.y / (float)sz.y);
	cama = (float)Math.PI * 2 * ((float)c.x / (float)sz.x);
    }
}
