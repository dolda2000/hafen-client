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

public class TestView extends PView {
    static final Resource tmesh;
    static {
	Resource res = Resource.load("gfx/test");
	res.loadwait();
	tmesh = res;
	VertexBuf v = tmesh.layer(VertexBuf.VertexRes.class).b;
    }
    
    public TestView(Coord c, Coord sz, Widget parent) {
	super(c, sz, parent);
	PointedCam cam;
	camera = cam = new PointedCam();
	cam.a = (float)Math.PI * 3 / 2;
	cam.e = (float)Math.PI / 2;
    }

    public static class Cube implements Rendered {
	public void draw(GOut g) {
	    GL gl = g.gl;
	    
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
	
	public boolean setup(RenderList rls) {
	    return(true);
	}
    }

    protected void setup(RenderList rls) {
	for(FastMesh.MeshRes m : tmesh.layers(FastMesh.MeshRes.class))
	    rls.add(m.m, null);
	/*
	rls.add(new Cube(), Transform.xlate(new Coord3f(-1.5f, 0, 0)));
	rls.add(new Cube(), Transform.xlate(new Coord3f(1.5f, 0, 0)));
	*/
    }

    public void mousemove(Coord c) {
	PointedCam cam = (PointedCam)camera;
	if(c.x < 0 || c.x >= sz.x || c.y < 0 || c.y >= sz.y)
	    return;
	cam.e = (float)Math.PI / 2 * ((float)c.y / (float)sz.y);
	cam.a = (float)Math.PI * 2 * ((float)c.x / (float)sz.x);
    }
}
