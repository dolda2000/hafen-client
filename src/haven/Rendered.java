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
    public boolean setup(RenderList r);
    
    public static interface RComparator<T extends Rendered> {
	public int compare(T a, T b, GLState.Buffer sa, GLState.Buffer sb);
    }

    public static final GLState.Slot<Order> order = new GLState.Slot<Order>(GLState.Slot.Type.GEOM, Order.class, HavenPanel.global);
    public static abstract class Order<T extends Rendered> extends GLState {
	public abstract int mainz();
	public abstract RComparator<? super T> cmp();
	
	public void apply(GOut g) {}
	public void unapply(GOut g) {}
	public void prep(GLState.Buffer buf) {
	    buf.put(order, this);
	}

	public static class Default extends Order<Rendered> {
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
    public final static Order first = new Order.Default(Integer.MIN_VALUE);
    public final static Order last = new Order.Default(Integer.MAX_VALUE);

    public final static Order eyesort = new Order.Default(10000) {
	    private final RComparator<Rendered> cmp = new RComparator<Rendered>() {
		public int compare(Rendered a, Rendered b, GLState.Buffer sa, GLState.Buffer sb) {
		    /* It would be nice to be able to cache these
		     * results somewhere. */
		    Camera ca = sa.get(PView.cam);
		    Location la = sa.get(PView.loc);
		    Matrix4f mva = ca.fin(Matrix4f.id).mul(la.fin(Matrix4f.id));
		    float da = (float)Math.sqrt((mva.m[12] * mva.m[12]) + (mva.m[13] * mva.m[13]) + (mva.m[14] * mva.m[14]));
		    Camera cb = sb.get(PView.cam);
		    Location lb = sb.get(PView.loc);
		    Matrix4f mvb = cb.fin(Matrix4f.id).mul(lb.fin(Matrix4f.id));
		    float db = (float)Math.sqrt((mvb.m[12] * mvb.m[12]) + (mvb.m[13] * mvb.m[13]) + (mvb.m[14] * mvb.m[14]));
		    if(da < db)
			return(1);
		    else
			return(-1);
		}
	    };
	    
	    public RComparator<Rendered> cmp() {
		return(cmp);
	    }
	};

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
	
	public boolean setup(RenderList r) {
	    return(true);
	}
    }

    public static class Axes implements Rendered {
	public final float[] mid;
	
	public Axes(java.awt.Color mid) {
	    this.mid = Utils.c2fa(mid);
	}
	
	public Axes() {
	    this(java.awt.Color.BLACK);
	}
	
	public void draw(GOut g) {
	    GL gl = g.gl;
	    g.st.put(Light.lighting, null);
	    g.state(States.xray);
	    g.apply();
	    gl.glBegin(GL.GL_LINES);
	    gl.glColor4fv(mid, 0);
	    gl.glVertex3f(0, 0, 0);
	    gl.glColor3f(1, 0, 0);
	    gl.glVertex3f(1, 0, 0);
	    gl.glColor4fv(mid, 0);
	    gl.glVertex3f(0, 0, 0);
	    gl.glColor3f(0, 1, 0);
	    gl.glVertex3f(0, 1, 0);
	    gl.glColor4fv(mid, 0);
	    gl.glVertex3f(0, 0, 0);
	    gl.glColor3f(0, 0, 1);
	    gl.glVertex3f(0, 0, 1);
	    gl.glEnd();
	}
	
	public boolean setup(RenderList r) {
	    r.state().put(States.color, null);
	    return(true);
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
	
	public boolean setup(RenderList r) {
	    r.state().put(States.color, null);
	    r.state().put(Light.lighting, null);
	    return(true);
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
	
	public boolean setup(RenderList rls) {
	    rls.state().put(States.color, null);
	    return(true);
	}
    }
    
    public static class ScreenQuad implements Rendered {
	private static final Projection proj = new Projection(Matrix4f.id);
	public final boolean tex;
	
	public ScreenQuad(boolean tex) {
	    this.tex = tex;
	}
	    
	public void draw(GOut g) {
	    GL gl = g.gl;
	    g.apply();
		
	    g.gl.glDisable(GL.GL_DEPTH_TEST);
	    g.gl.glDepthMask(false);
	    gl.glBegin(GL.GL_QUADS);
	    if(tex) gl.glTexCoord2f(0.0f, 0.0f);
	    gl.glVertex3f(-1.0f, -1.0f, 0.0f);
	    if(tex) gl.glTexCoord2f(1.0f, 0.0f);
	    gl.glVertex3f( 1.0f, -1.0f, 0.0f);
	    if(tex) gl.glTexCoord2f(1.0f, 1.0f);
	    gl.glVertex3f( 1.0f,  1.0f, 0.0f);
	    if(tex) gl.glTexCoord2f(0.0f, 1.0f);
	    gl.glVertex3f(-1.0f,  1.0f, 0.0f);
	    gl.glEnd();
	    g.gl.glEnable(GL.GL_DEPTH_TEST);
	    g.gl.glDepthMask(true);
	}
	    
	public boolean setup(RenderList rls) {
	    rls.prepo(proj);
	    rls.state().put(PView.cam, null);
	    rls.state().put(PView.loc, null);
	    return(true);
	}
    }
}
