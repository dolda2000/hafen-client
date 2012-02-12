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
    
    public static final GLState.Slot<LightList> lights = new GLState.Slot<LightList>(GLState.Slot.Type.DRAW, LightList.class, PView.cam);
    public static final GLState.Slot<Model> model = new GLState.Slot<Model>(GLState.Slot.Type.DRAW, Model.class, PView.proj);
    public static final GLState.Slot<GLState> lighting = new GLState.Slot<GLState>(GLState.Slot.Type.DRAW, GLState.class, model, lights);
    
    public static class BaseLights extends GLState {
	private final GLShader[] shaders;
	
	public BaseLights(GLShader[] shaders) {
	    this.shaders = shaders;
	}

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
	
	public boolean reqshaders() {
	    return(true);
	}
	
	public void prep(Buffer buf) {
	    buf.put(lighting, this);
	}
    }

    public static final GLState vlights = new BaseLights(new GLShader[] {
	    GLShader.VertexShader.load(Light.class, "glsl/vlight.vert"),
	    GLShader.FragmentShader.load(Light.class, "glsl/vlight.frag"),
	}) {
	    public boolean reqshaders() {
		return(false);
	    }
	};
    
    public static final GLState plights = new BaseLights(new GLShader[] {
	    GLShader.VertexShader.load(Light.class, "glsl/plight.vert"),
	    GLShader.FragmentShader.load(Light.class, "glsl/plight-base.frag"),
	    GLShader.FragmentShader.load(Light.class, "glsl/plight.frag"),
	});
    
    public static final GLState vcel = new BaseLights(new GLShader[] {
	    GLShader.VertexShader.load(Light.class, "glsl/vlight.vert"),
	    GLShader.FragmentShader.load(Light.class, "glsl/vcel-diff.frag"),
	    GLShader.FragmentShader.load(Light.class, "glsl/vcel-spec.frag"),
	});
    
    public static final GLState pcel = new BaseLights(new GLShader[] {
	    GLShader.VertexShader.load(Light.class, "glsl/plight.vert"),
	    GLShader.FragmentShader.load(Light.class, "glsl/plight-base.frag"),
	    GLShader.FragmentShader.load(Light.class, "glsl/pcel.frag"),
	});
    
    public static class PSLights extends BaseLights {
	public static class ShadowMap extends GLState implements GlobalState, Global {
	    public final static Slot<ShadowMap> smap = new Slot<ShadowMap>(Slot.Type.DRAW, ShadowMap.class, new Slot[] {lights}, new Slot[] {lighting});
	    public DirLight light;
	    public final TexE lbuf;
	    private final Projection lproj;
	    private final DirCam lcam;
	    private final FBView tgt;
	    private final static Matrix4f texbias = new Matrix4f(0.5f, 0.0f, 0.0f, 0.5f,
								 0.0f, 0.5f, 0.0f, 0.5f,
								 0.0f, 0.0f, 0.5f, 0.5f,
								 0.0f, 0.0f, 0.0f, 1.0f);
	    private final List<RenderList.Slot> parts = new ArrayList<RenderList.Slot>();
	    private int slidx;
	    private Matrix4f txf;
	    
	    public ShadowMap(Coord res, float size, float depth) {
		lbuf = new TexE(res, GL.GL_DEPTH_COMPONENT, GL.GL_DEPTH_COMPONENT, GL.GL_UNSIGNED_INT);
		lbuf.magfilter = GL.GL_LINEAR;
		lbuf.wrapmode = GL.GL_CLAMP;
		lproj = Projection.ortho(-size, size, -size, size, 1, depth);
		lcam = new DirCam();
		tgt = new FBView(new GLFrameBuffer(null, lbuf), GLState.compose(lproj, lcam));
	    }
	
	    private final Rendered scene = new Rendered() {
		    public void draw(GOut g) {}
		    
		    public boolean setup(RenderList rl) {
			GLState.Buffer buf = new GLState.Buffer(rl.cfg);
			for(RenderList.Slot s : parts) {
			    rl.state().copy(buf);
			    s.os.copy(buf, GLState.Slot.Type.GEOM);
			    rl.add2(s.r, buf);
			}
			return(false);
		    }
		};

	    public void setpos(Coord3f base, Coord3f dir) {
		lcam.base = base;
		lcam.dir = dir;
	    }
	    
	    public void dispose() {
		lbuf.dispose();
		tgt.dispose();
	    }
	    
	    public void prerender(RenderList rl, GOut g) {
		parts.clear();
		LightList ll = null;
		Camera cam = null;
		for(RenderList.Slot s : rl.slots()) {
		    if(!s.d)
			continue;
		    if((s.os.get(smap) != this) || (s.os.get(lighting) != pslights))
			continue;
		    if(ll == null) {
			PView.RenderState rs = s.os.get(PView.wnd);
			cam = s.os.get(PView.cam);
			ll = s.os.get(lights);
		    }
		    parts.add(s);
		}
	    
		slidx = -1;
		for(int i = 0; i < ll.ll.size(); i++) {
		    if(ll.ll.get(i) == light) {
			slidx = i;
			break;
		    }
		}
		Matrix4f cm = Transform.rxinvert(cam.fin(Matrix4f.id));
		/*
		txf = cm;
		barda(txf);
		txf = lcam.fin(Matrix4f.id).mul(txf);
		barda(txf);
		txf = lproj.fin(Matrix4f.id).mul(txf);
		barda(txf);
		txf = texbias.mul(txf);
		barda(txf);
		*/
		txf = texbias
		    .mul(lproj.fin(Matrix4f.id))
		    .mul(lcam.fin(Matrix4f.id))
		    .mul(cm);
		tgt.render(scene, g);
	    }
	    
	    /*
	    static void barda(Matrix4f m) {
		float[] a = m.mul4(new float[] {0, 0, 0, 1});
		System.err.println(String.format("(%f, %f, %f, %f)", a[0], a[1], a[2], a[3]));
	    }
	    */
	    
	    public Global global(RenderList rl, Buffer ctx) {return(this);}
	    
	    public void postsetup(RenderList rl) {}
	    public void postrender(RenderList rl, GOut g) {
		/* g.image(lbuf, Coord.z, g.sz); */
	    }
	    
	    public void prep(Buffer buf) {
		buf.put(smap, this);
	    }

	    public void apply(GOut g) {
		GL gl = g.gl;
		g.st.texunit(1);
		gl.glBindTexture(GL.GL_TEXTURE_2D, lbuf.glid(g));
	    }
	
	    public void unapply(GOut g) {
		GL gl = g.gl;
		g.st.texunit(1);
		gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
	    }

	}
	
	public PSLights() {
	    super(new GLShader[] {
		    GLShader.VertexShader.load(Light.class, "glsl/pslight.vert"),
		    GLShader.FragmentShader.load(Light.class, "glsl/plight-base.frag"),
		    GLShader.FragmentShader.load(Light.class, "glsl/pslight.frag"),
		});
	}
	    
	public void reapply(GOut g) {
	    super.reapply(g);
	    GL gl = g.gl;
	    ShadowMap map = g.st.cur(ShadowMap.smap);
	    if(map != null) {
		gl.glUniformMatrix4fv(g.st.prog.uniform("pslight_txf"), 1, false, map.txf.m, 0);
		gl.glUniform1i(g.st.prog.uniform("pslight_sl"), map.slidx);
	    } else {
		gl.glUniform1i(g.st.prog.uniform("pslight_sl"), -1);
	    }
	    gl.glUniform1i(g.st.prog.uniform("pslight_map"), 1);
	}
    }
    public static final GLState pslights = new PSLights();
    /*
    public static final GLState pslights = new GLState() {
	    private final Map<PView.RenderState, PSLights> states = new WeakHashMap<PView.RenderState, PSLights>();
	    
	    public void apply(GOut g) {}
	    public void unapply(GOut g) {}
	    public void prep(Buffer buf) {
		PView.RenderState rs = buf.get(PView.wnd);
		PSLights l;
		synchronized(states) {
		    if((l = states.get(rs)) == null)
			states.put(rs, l = new PSLights(new Coord(1024, 1024)));
		}
		l.prep(buf);
	    }
	};
    */
    
    public static final GLState deflight = new GLState() {
	    public void apply(GOut g) {}
	    public void unapply(GOut g) {}
	    
	    public void prep(Buffer buf) {
		buf.cfg.deflight.prep(buf);
	    }
	};
    
    public static class LightList extends GLState {
	private final List<Light> ll = new ArrayList<Light>();
	private final List<Matrix4f> vl = new ArrayList<Matrix4f>();
	private final List<Light> en = new ArrayList<Light>();
	public int nlights = 0;
	
	public void apply(GOut g) {
	    GL gl = g.gl;
	    int nl = ll.size();
	    if(g.gc.maxlights < nl)
		nl = g.gc.maxlights;
	    en.clear();
	    for(int i = 0; i < nl; i++) {
		Matrix4f mv = vl.get(i);
		Light l = ll.get(i);
		g.st.matmode(GL.GL_MODELVIEW);
		gl.glLoadMatrixf(mv.m, 0);
		en.add(l);
		l.enable(g, i);
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
	
	private void add(Light l, Matrix4f loc) {
	    ll.add(l);
	    vl.add(loc);
	    if(ll.size() != vl.size())
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
    public boolean setup(RenderList rl) {
	LightList l = rl.state().get(lights);
	if(l != null) {
	    Camera cam = rl.state().get(PView.cam);
	    Location loc = rl.state().get(PView.loc);
	    Matrix4f mv = cam.fin(Matrix4f.identity());
	    if(loc != null)
		mv = mv.mul(loc.fin(Matrix4f.identity()));
	    l.add(this, mv);
	}
	return(false);
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
