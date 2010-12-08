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

import java.util.Comparator;
import javax.media.opengl.*;

public interface Rendered {
    public void draw(GOut g);
    public Order setup(RenderList r);
    
    public static interface RComparator<T extends Rendered> {
	public int compare(T a, T b, GLState.Buffer sa, GLState.Buffer sb);
    }

    public static interface Order<T extends Rendered> {
	public int mainz();
	public RComparator<? super T> cmp();
	
	public static class Default implements Order<Rendered> {
	    private final int z;
	    
	    public Default(int z) {
		this.z = z;
	    }
	    
	    public int mainz() {
		return(z);
	    }
	    
	    private RComparator<Rendered> cmp = new RComparator<Rendered>() {
		public int compare(Rendered a, Rendered b, GLState.Buffer sa, GLState.Buffer sb) {
		    return(0);
		}
	    };
	    
	    public RComparator<Rendered> cmp() {
		return(cmp);
	    }
	}
    }

    public final static Order deflt = new Order.Default(0);
    public final static Order last = new Order.Default(Integer.MAX_VALUE);

    public static class Dot implements Rendered {
	public void draw(GOut g) {
	    GL gl = g.gl;
	    g.st.put(Light.lighting, null);
	    g.state(States.xray);
	    g.apply();
	    gl.glBegin(GL.GL_POINTS);
	    gl.glColor3f(1.0f, 0.0f, 0.0f);
	    gl.glVertex3f(0.0f, 0.0f, 0.0f);
	    gl.glEnd();
	}
	
	public Order setup(RenderList r) {
	    return(last);
	}
    }
    
    public static class Line implements Rendered {
	public final Coord3f end;
	
	public Line(Coord3f end) {
	    this.end = end;
	}
	
	public void draw(GOut g) {
	    GL gl = g.gl;
	    g.apply();
	    gl.glBegin(GL.GL_LINES);
	    gl.glColor3f(1, 0, 0);
	    gl.glVertex3f(0, 0, 0);
	    gl.glColor3f(0, 1, 0);
	    gl.glVertex3f(end.x, end.y, end.z);
	    gl.glEnd();
	}
	
	public Order setup(RenderList r) {
	    r.state().put(States.color, null);
	    r.state().put(Light.lighting, null);
	    r.prepo(States.xray);
	    return(last);
	}
    }
    
    public static class Cube implements Rendered {
	public void draw(GOut g) {
	    GL gl = g.gl;
	    g.apply();
	    
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
	
	public Order setup(RenderList rls) {
	    return(last);
	}
    }
}
