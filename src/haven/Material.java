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
    
    public static final GLState nofacecull = new GLState.StandAlone(PView.proj) {
	    public void apply(GOut g) {
		g.gl.glDisable(GL.GL_CULL_FACE);
	    }
	    
	    public void unapply(GOut g) {
		g.gl.glEnable(GL.GL_CULL_FACE);
	    }
	};
    
    public static final GLState alphaclip = new GLState.StandAlone(PView.proj) {
	    public void apply(GOut g) {
		g.gl.glEnable(GL.GL_ALPHA_TEST);
	    }
	    
	    public void unapply(GOut g) {
		g.gl.glDisable(GL.GL_ALPHA_TEST);
	    }
	};
    
    public static final float[] defamb = {0.2f, 0.2f, 0.2f, 1.0f};
    public static final float[] defdif = {0.8f, 0.8f, 0.8f, 1.0f};
    public static final float[] defspc = {0.0f, 0.0f, 0.0f, 1.0f};
    public static final float[] defemi = {0.0f, 0.0f, 0.0f, 1.0f};
    
    public static final GLState.Slot<Colors> colors = new GLState.Slot<Colors>(Colors.class);
    public static class Colors extends GLState {
	public float[] amb, dif, spc, emi;
	public float shine;
    
	public Colors() {
	    amb = defamb;
	    dif = defdif;
	    spc = defspc;
	    emi = defemi;
	}

	public Colors(Color amb, Color dif, Color spc, Color emi, float shine) {
	    build(amb, dif, spc, emi);
	    this.shine = shine;
	}
	
	public Colors(Color col) {
	    this(new Color((int)(col.getRed() * defamb[0]), (int)(col.getGreen() * defamb[1]), (int)(col.getBlue() * defamb[2]), col.getAlpha()),
		 new Color((int)(col.getRed() * defdif[0]), (int)(col.getGreen() * defdif[1]), (int)(col.getBlue() * defdif[2]), col.getAlpha()),
		 new Color(0, 0, 0, 0),
		 new Color(0, 0, 0, 0),
		 0);
	}
    
	public void build(Color amb, Color dif, Color spc, Color emi) {
	    this.amb = c2fa(amb);
	    this.dif = c2fa(dif);
	    this.spc = c2fa(spc);
	    this.emi = c2fa(emi);
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
	    buf.put(colors, this);
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
	this(new Colors(), alphaclip);
    }
    
    public Material(Color amb, Color dif, Color spc, Color emi, float shine) {
	this(new Colors(amb, dif, spc, emi, shine), alphaclip);
    }
    
    public Material(Color col) {
	this(new Colors(col));
    }
    
    public Material(Tex tex) {
	this(new Colors(), tex, alphaclip);
    }
    
    public String toString() {
	return(Arrays.asList(states).toString());
    }
    
    private static GLState deflight = Light.vlights;
    public void prep(Buffer buf) {
	for(GLState st : states)
	    st.prep(buf);
	buf.cfg.deflight.prep(buf);
    }
    
    public static class Res extends Resource.Layer {
	public final int id;
	private transient List<GLState> states = new LinkedList<GLState>();
	private transient List<Resolver> left = new LinkedList<Resolver>();
	private transient Material m;
	private boolean mipmap = false, linear = false;
	
	private interface Resolver {
	    public GLState resolve();
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
			    public GLState resolve() {
				for(Resource.Image img : getres().layers(Resource.imgc, false)) {
				    if(img.id == id)
					return(img.tex());
				}
				throw(new Resource.LoadException("Specified texture not found: " + id, getres()));
			    }
			});
		} else if(thing == "texlink") {
		    final String nm = Utils.strd(buf, off);
		    final int ver = Utils.uint16d(buf, off[0]); off[0] += 2;
		    final int id = Utils.uint16d(buf, off[0]); off[0] += 2;
		    left.add(new Resolver() {
			    public GLState resolve() {
				Resource res = Resource.load(nm, ver);
				for(Resource.Image img : res.layers(Resource.imgc, false)) {
				    if(img.id == id)
					return(img.tex());
				}
				throw(new Resource.LoadException("Specified texture not found: " + id, getres()));
			    }
			});
		} else {
		    throw(new Resource.LoadException("Unknown material part: " + thing, getres()));
		}
	    }
	    states.add(alphaclip);
	}
	
	public Material get() {
	    synchronized(this) {
		if(m == null) {
		    for(Iterator<Resolver> i = left.iterator(); i.hasNext();) {
			Resolver r = i.next();
			states.add(r.resolve());
			i.remove();
		    }
		    m = new Material(states.toArray(new GLState[0]));
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
    }
}
