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

import java.util.*;
import java.awt.Color;
import javax.media.opengl.*;
import static haven.Utils.c2fa;

public class Light implements Rendered {
    public float[] amb, dif, spc;
    
    private static final float[] defamb = {0.0f, 0.0f, 0.0f, 1.0f};
    private static final float[] defdif = {1.0f, 1.0f, 1.0f, 1.0f};
    private static final float[] defspc = {1.0f, 1.0f, 1.0f, 1.0f};

    public Light() {
	this.amb = defamb;
	this.dif = defdif;
	this.spc = defspc;
    }
    
    public Light(Color col) {
	this.amb = defamb;
	this.dif = this.spc = c2fa(col);
    }

    public Light(Color amb, Color dif, Color spc) {
	this.amb = c2fa(amb);
	this.dif = c2fa(dif);
	this.spc = c2fa(spc);
    }

    public void enable(GOut g, int idx) {
	GL gl = g.gl;
	gl.glEnable(GL.GL_LIGHT0 + idx);
	gl.glLightfv(GL.GL_LIGHT0 + idx, GL.GL_AMBIENT, amb, 0);
	gl.glLightfv(GL.GL_LIGHT0 + idx, GL.GL_DIFFUSE, dif, 0);
	gl.glLightfv(GL.GL_LIGHT0 + idx, GL.GL_SPECULAR, spc, 0);
    }
    
    public void disable(GOut g, int idx) {
	GL gl = g.gl;
	gl.glLightfv(GL.GL_LIGHT0 + idx, GL.GL_AMBIENT, defamb, 0);
	gl.glLightfv(GL.GL_LIGHT0 + idx, GL.GL_DIFFUSE, defdif, 0);
	gl.glLightfv(GL.GL_LIGHT0 + idx, GL.GL_SPECULAR, defspc, 0);
	gl.glDisable(GL.GL_LIGHT0 + idx);
    }
    
    public static final GLState.Slot<LightList> lights = new GLState.Slot<LightList>(LightList.class, PView.cam);
    public static final GLState.Slot<Model> model = new GLState.Slot<Model>(Model.class, PView.proj);
    public static final GLState.Slot<GLState> lighting = new GLState.Slot<GLState>(GLState.class, model, lights);
    
    public static final GLState vlights = new GLState() {
	    private final GLShader[] shaders = {
		GLShader.VertexShader.load(this.getClass(), "glsl/vlight.vert"),
		GLShader.FragmentShader.load(this.getClass(), "glsl/vlight.frag"),
	    };
	    
	    public void apply(GOut g) {
		GL gl = g.gl;
		if(g.st.prog == null)
		    gl.glEnable(GL.GL_LIGHTING);
		else
		    reapply(g);
	    }
	    
	    public void reapply(GOut g) {
		GL gl = g.gl;
		gl.glUniform1i(g.st.prog.uniform("nlights"), g.st.get(lights).nlights);
	    }
	    
	    public void unapply(GOut g) {
		GL gl = g.gl;
		if(!g.st.usedprog)
		    gl.glDisable(GL.GL_LIGHTING);
	    }
	    
	    public GLShader[] shaders() {
		return(shaders);
	    }
	    
	    public void prep(Buffer buf) {
		buf.put(lighting, this);
	    }
	};
    
    public static final GLState plights = new GLState() {
	    private final GLShader[] shaders = {
		GLShader.VertexShader.load(this.getClass(), "glsl/plight.vert"),
		GLShader.FragmentShader.load(this.getClass(), "glsl/plight-diff.frag"),
		GLShader.FragmentShader.load(this.getClass(), "glsl/plight-spec.frag"),
	    };
	    
	    public void apply(GOut g) {
		GL gl = g.gl;
		if(g.st.prog != null)
		    reapply(g);
	    }
	    
	    public void reapply(GOut g) {
		GL gl = g.gl;
		gl.glUniform1i(g.st.prog.uniform("nlights"), g.st.get(lights).nlights);
	    }
	    
	    public void unapply(GOut g) {
		GL gl = g.gl;
	    }
	    
	    public GLShader[] shaders() {
		return(shaders);
	    }
	    
	    public boolean reqshaders() {
		return(true);
	    }
	    
	    public void prep(Buffer buf) {
		buf.put(lighting, this);
	    }
	};
    
    public static class LightList extends GLState {
	private final List<Light> ll = new ArrayList<Light>();
	private final List<Location> sl = new ArrayList<Location>();
	private final List<Light> en = new ArrayList<Light>();
	public int nlights = 0;
	
	public void apply(GOut g) {
	    GL gl = g.gl;
	    int nl = ll.size();
	    if(g.gc.maxlights < nl)
		nl = g.gc.maxlights;
	    en.clear();
	    for(int i = 0; i < nl; i++) {
		Location loc = sl.get(i);
		Light l = ll.get(i);
		if(loc != null)
		    loc.apply(g);
		try {
		    en.add(l);
		    l.enable(g, i);
		} finally {
		    if(loc != null)
			loc.unapply(g);
		}
		GOut.checkerr(gl);
	    }
	    nlights = nl;
	}
	
	public void unapply(GOut g) {
	    for(int i = 0; i < en.size(); i++) {
		en.get(i).disable(g, i);
		GOut.checkerr(g.gl);
	    }
	    nlights = 0;
	}
	
	public int capply() {
	    return(1000);
	}
	
	public int cunapply() {
	    return(1000);
	}
	
	public void prep(Buffer buf) {
	    buf.put(lights, this);
	}
	
	private void add(Light l, Location loc) {
	    ll.add(l);
	    sl.add(loc);
	    if(ll.size() != sl.size())
		throw(new RuntimeException());
	}
    }
    
    public static class Model extends GLState {
	public float[] amb;
	public int cc = GL.GL_SINGLE_COLOR;
	private static final float[] defamb = {0.2f, 0.2f, 0.2f, 1.0f};
	
	public Model(Color amb) {
	    this.amb = c2fa(amb);
	}
	
	public Model() {
	    this(Color.BLACK);
	}
	
	public void apply(GOut g) {
	    GL gl = g.gl;
	    gl.glLightModelfv(GL.GL_LIGHT_MODEL_AMBIENT, amb, 0);
	    gl.glLightModeli(GL.GL_LIGHT_MODEL_COLOR_CONTROL, cc);
	}
	
	public void unapply(GOut g) {
	    GL gl = g.gl;
	    gl.glLightModelfv(GL.GL_LIGHT_MODEL_AMBIENT, defamb, 0);
	    gl.glLightModeli(GL.GL_LIGHT_MODEL_COLOR_CONTROL, GL.GL_SINGLE_COLOR);
	}
	
	public void prep(Buffer buf) {
	    buf.put(model, this);
	}
    }

    public void draw(GOut g) {}
    public Order setup(RenderList rl) {
	LightList l = rl.state().get(lights);
	if(l != null)
	    l.add(this, rl.state().get(PView.loc));
	return(null);
    }
    
    public static class Res extends Resource.Layer {
	public final int id;
	public final Color amb, dif, spc;
	public boolean hatt, hexp;
	public float ac, al, aq, exp;
	public Coord3f dir;
	
	private static Color cold(byte[] buf, int[] off) {
	    double r, g, b, a;
	    r = Utils.floatd(buf, off[0]); off[0] += 5;
	    g = Utils.floatd(buf, off[0]); off[0] += 5;
	    b = Utils.floatd(buf, off[0]); off[0] += 5;
	    a = Utils.floatd(buf, off[0]); off[0] += 5;
	    return(new Color((int)(r * 255.0), (int)(g * 255.0), (int)(b * 255.0), (int)(a * 255.0)));
	}
	
	public Res(Resource res, byte[] buf) {
	    res.super();
	    int[] off = {0};
	    this.id = Utils.int16d(buf, off[0]); off[0] += 2;
	    this.amb = cold(buf, off);
	    this.dif = cold(buf, off);
	    this.spc = cold(buf, off);
	    while(off[0] < buf.length) {
		int t = buf[off[0]]; off[0]++;
		if(t == 1) {
		    hatt = true;
		    ac = (float)Utils.floatd(buf, off[0]); off[0] += 5;
		    al = (float)Utils.floatd(buf, off[0]); off[0] += 5;
		    aq = (float)Utils.floatd(buf, off[0]); off[0] += 5;
		} else if(t == 2) {
		    float x = (float)Utils.floatd(buf, off[0]); off[0] += 5;
		    float y = (float)Utils.floatd(buf, off[0]); off[0] += 5;
		    float z = (float)Utils.floatd(buf, off[0]); off[0] += 5;
		    dir = new Coord3f(x, y, z);
		} else if(t == 3) {
		    hexp = true;
		    exp = (float)Utils.floatd(buf, off[0]); off[0] += 5;
		} else {
		    throw(new Resource.LoadException("Unknown light data: " + t, getres()));
		}
	    }
	}
	
	public Light make() {
	    if(hatt) {
		PosLight ret;
		if(hexp)
		    ret = new SpotLight(amb, dif, spc, Coord3f.o, dir, exp);
		else
		    ret = new PosLight(amb, dif, spc, Coord3f.o);
		ret.att(ac, al, aq);
		return(ret);
	    } else {
		return(new DirLight(amb, dif, spc, dir));
	    }
	}
	
	public void init() {
	}
    }
}
