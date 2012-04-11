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

import java.awt.Color;
import java.util.*;
import javax.media.opengl.*;
import static haven.Utils.c2fa;

public class Material extends GLState {
    public final GLState[] states;
    
    public static final GLState nofacecull = new GLState.StandAlone(Slot.Type.GEOM, PView.proj) {
	    public void apply(GOut g) {
		g.gl.glDisable(GL.GL_CULL_FACE);
	    }
	    
	    public void unapply(GOut g) {
		g.gl.glEnable(GL.GL_CULL_FACE);
	    }
	};
    
    public static final float[] defamb = {0.2f, 0.2f, 0.2f, 1.0f};
    public static final float[] defdif = {0.8f, 0.8f, 0.8f, 1.0f};
    public static final float[] defspc = {0.0f, 0.0f, 0.0f, 1.0f};
    public static final float[] defemi = {0.0f, 0.0f, 0.0f, 1.0f};
    
    public static final GLState.Slot<Colors> colors = new GLState.Slot<Colors>(Slot.Type.DRAW, Colors.class);
    public static class Colors extends GLState {
	public float[] amb, dif, spc, emi;
	public float shine;
    
	public Colors() {
	    amb = defamb;
	    dif = defdif;
	    spc = defspc;
	    emi = defemi;
	}

	private Colors(float[] amb, float[] dif, float[] spc, float[] emi, float shine) {
	    this.amb = amb;
	    this.dif = dif;
	    this.spc = spc;
	    this.emi = emi;
	    this.shine = shine;
	}
	
	private static float[] colmul(float[] c1, float[] c2) {
	    return(new float[] {c1[0] * c2[0], c1[1] * c2[1], c1[2] * c2[2], c1[3] * c2[3]});
	}
	
	private static float[] colblend(float[] in, float[] bl) {
	    float f1 = bl[3], f2 = 1.0f - f1;
	    return(new float[] {(in[0] * f2) + (bl[0] * f1),
				(in[1] * f2) + (bl[1] * f1),
				(in[2] * f2) + (bl[2] * f1),
				in[3]});
	}

	public Colors(Color amb, Color dif, Color spc, Color emi, float shine) {
	    this(c2fa(amb), c2fa(dif), c2fa(spc), c2fa(emi), shine);
	}
	
	public Colors(Color amb, Color dif, Color spc, Color emi) {
	    this(amb, dif, spc, emi, 0);
	}
	
	public Colors(Color col) {
	    this(new Color((int)(col.getRed() * defamb[0]), (int)(col.getGreen() * defamb[1]), (int)(col.getBlue() * defamb[2]), col.getAlpha()),
		 new Color((int)(col.getRed() * defdif[0]), (int)(col.getGreen() * defdif[1]), (int)(col.getBlue() * defdif[2]), col.getAlpha()),
		 new Color(0, 0, 0, 0),
		 new Color(0, 0, 0, 0),
		 0);
	}
    
	public void apply(GOut g) {
	    GL gl = g.gl;
	    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT, amb, 0);
	    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_DIFFUSE, dif, 0);
	    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SPECULAR, spc, 0);
	    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_EMISSION, emi, 0);
	    gl.glMaterialf(GL.GL_FRONT_AND_BACK, GL.GL_SHININESS, shine);
	}

	public void unapply(GOut g) {
	    GL gl = g.gl;
	    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT, defamb, 0);
	    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_DIFFUSE, defdif, 0);
	    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SPECULAR, defspc, 0);
	    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_EMISSION, defemi, 0);
	    gl.glMaterialf(GL.GL_FRONT_AND_BACK, GL.GL_SHININESS, 0.0f);
	}
    
	public int capplyfrom(GLState from) {
	    if(from instanceof Colors)
		return(5);
	    return(-1);
	}

	public void applyfrom(GOut g, GLState from) {
	    if(from instanceof Colors)
		apply(g);
	}
	
	public void prep(Buffer buf) {
	    Colors p = buf.get(colors);
	    if(p != null)
		buf.put(colors, p.combine(this));
	    else
		buf.put(colors, this);
	}
	
	public Colors combine(Colors other) {
	    return(new Colors(colblend(other.amb, this.amb),
			      colblend(other.dif, this.dif),
			      colblend(other.spc, this.spc),
			      colblend(other.emi, this.emi),
			      other.shine));
	}
    
	public String toString() {
	    return(String.format("(%.1f, %.1f, %.1f), (%.1f, %.1f, %.1f), (%.1f, %.1f, %.1f @ %.1f)",
				 amb[0], amb[1], amb[2], dif[0], dif[1], dif[2], spc[0], spc[1], spc[2], shine));
	}
    }
    
    public void apply(GOut g) {}
    
    public void unapply(GOut g) {}
    
    public Material(GLState... states) {
	this.states = states;
    }

    public Material() {
	this(Light.deflight, new Colors());
    }
    
    public Material(Color amb, Color dif, Color spc, Color emi, float shine) {
	this(Light.deflight, new Colors(amb, dif, spc, emi, shine));
    }
    
    public Material(Color col) {
	this(Light.deflight, new Colors(col));
    }
    
    public Material(Tex tex) {
	this(Light.deflight, new Colors(), tex.draw(), tex.clip());
    }
    
    public String toString() {
	return(Arrays.asList(states).toString());
    }
    
    public void prep(Buffer buf) {
	for(GLState st : states)
	    st.prep(buf);
    }
    
    public static class Res extends Resource.Layer implements Resource.IDLayer<Integer> {
	public final int id;
	private transient List<GLState> states = new LinkedList<GLState>();
	private transient List<Resolver> left = new LinkedList<Resolver>();
	private transient Material m;
	private boolean mipmap = false, linear = false;
	
	private interface Resolver {
	    public void resolve(Collection<GLState> buf);
	}
	
	private static Color col(byte[] buf, int[] off) {
	    double r = Utils.floatd(buf, off[0]); off[0] += 5;
	    double g = Utils.floatd(buf, off[0]); off[0] += 5;
	    double b = Utils.floatd(buf, off[0]); off[0] += 5;
	    double a = Utils.floatd(buf, off[0]); off[0] += 5;
	    return(new Color((float)r, (float)g, (float)b, (float)a));
	}

	public Res(Resource res, byte[] buf) {
	    res.super();
	    id = Utils.uint16d(buf, 0);
	    int[] off = {2};
	    GLState light = Light.deflight;
	    while(off[0] < buf.length) {
		String thing = Utils.strd(buf, off).intern();
		if(thing == "col") {
		    Color amb = col(buf, off);
		    Color dif = col(buf, off);
		    Color spc = col(buf, off);
		    double shine = Utils.floatd(buf, off[0]); off[0] += 5;
		    Color emi = col(buf, off);
		    states.add(new Colors(amb, dif, spc, emi, (float)shine));
		} else if(thing == "linear") {
		    linear = true;
		} else if(thing == "mipmap") {
		    mipmap = true;
		} else if(thing == "nofacecull") {
		    states.add(nofacecull);
		} else if(thing == "tex") {
		    final int id = Utils.uint16d(buf, off[0]); off[0] += 2;
		    left.add(new Resolver() {
			    public void resolve(Collection<GLState> buf) {
				for(Resource.Image img : getres().layers(Resource.imgc)) {
				    if(img.id == id) {
					buf.add(img.tex().draw());
					buf.add(img.tex().clip());
					return;
				    }
				}
				throw(new RuntimeException(String.format("Specified texture %d not found in %s", id, getres())));
			    }
			});
		} else if(thing == "texlink") {
		    final String nm = Utils.strd(buf, off);
		    final int ver = Utils.uint16d(buf, off[0]); off[0] += 2;
		    final int id = Utils.uint16d(buf, off[0]); off[0] += 2;
		    left.add(new Resolver() {
			    public void resolve(Collection<GLState> buf) {
				Resource res = Resource.load(nm, ver);
				for(Resource.Image img : res.layers(Resource.imgc)) {
				    if(img.id == id) {
					buf.add(img.tex().draw());
					buf.add(img.tex().clip());
					return;
				    }
				}
				throw(new RuntimeException(String.format("Specified texture %d for %s not found in %s", id, getres(), res)));
			    }
			});
		} else if(thing == "light") {
		    String l = Utils.strd(buf, off);
		    if(l.equals("pv")) {
			light = Light.vlights;
		    } else if(l.equals("pp")) {
			light = Light.plights;
		    } else if(l.equals("vc")) {
			light = Light.vcel;
		    } else if(l.equals("pc")) {
			light = Light.pcel;
		    } else if(l.equals("n")) {
			light = null;
		    } else {
			throw(new Resource.LoadException("Unknown lighting type: " + thing, getres()));
		    }
		} else {
		    throw(new Resource.LoadException("Unknown material part: " + thing, getres()));
		}
	    }
	    if(light != null)
		states.add(light);
	}
	
	public Material get() {
	    synchronized(this) {
		if(m == null) {
		    for(Iterator<Resolver> i = left.iterator(); i.hasNext();) {
			Resolver r = i.next();
			r.resolve(states);
			i.remove();
		    }
		    m = new Material(states.toArray(new GLState[0])) {
			    public String toString() {
				return(super.toString() + "@" + getres().name);
			    }
			};
		}
		return(m);
	    }
	}

	public void init() {
	    for(Resource.Image img : getres().layers(Resource.imgc, false)) {
		TexGL tex = (TexGL)img.tex();
		if(mipmap)
		    tex.mipmap();
		if(linear)
		    tex.magfilter(GL.GL_LINEAR);
	    }
	}
	
	public Integer layerid() {
	    return(id);
	}
    }
}
