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
import javax.media.opengl.*;
import static haven.Utils.c2fa;

public class Material extends GLState {
    public float[] amb, dif, spc, emi;
    public float shine;
    public Tex tex;
    public boolean facecull = true, mipmap = false;
    
    private static final float[] defamb = {0.2f, 0.2f, 0.2f, 1.0f};
    private static final float[] defdif = {0.8f, 0.8f, 0.8f, 1.0f};
    private static final float[] defspc = {0.0f, 0.0f, 0.0f, 1.0f};
    private static final float[] defemi = {0.0f, 0.0f, 0.0f, 1.0f};
    public Material() {
	amb = defamb;
	dif = defdif;
	spc = defspc;
	emi = defemi;
    }

    public Material(Color amb, Color dif, Color spc, Color emi, float shine) {
	build(amb, dif, spc, emi);
	this.shine = shine;
    }
    
    public Material(Tex tex) {
	this();
	this.tex = tex;
    }

    public void build(Color amb, Color dif, Color spc, Color emi) {
	this.amb = c2fa(amb);
	this.dif = c2fa(dif);
	this.spc = c2fa(spc);
	this.emi = c2fa(emi);
    }
    
    private void apply(GOut g, Material cur) {
	GL gl = g.gl;
	gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT, amb, 0);
	gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_DIFFUSE, dif, 0);
	gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SPECULAR, spc, 0);
	gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_EMISSION, emi, 0);
	gl.glMaterialf(GL.GL_FRONT_AND_BACK, GL.GL_SHININESS, shine);
	if(!facecull && ((cur == null) || (cur.facecull)))
	    gl.glDisable(GL.GL_CULL_FACE);
	else if(facecull && (cur != null) && !cur.facecull)
	    gl.glEnable(GL.GL_CULL_FACE);
	if(tex != null)
	    tex.select(g);
    }

    public void apply(GOut g) {
	apply(g, null);
    }
    
    public void unapply(GOut g) {
	GL gl = g.gl;
	gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT, defamb, 0);
	gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_DIFFUSE, defdif, 0);
	gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SPECULAR, defspc, 0);
	gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_EMISSION, defemi, 0);
	gl.glMaterialf(GL.GL_FRONT_AND_BACK, GL.GL_SHININESS, 0.0f);
	if(!facecull)
	    gl.glEnable(GL.GL_CULL_FACE);
	g.texsel(-1);
    }
    
    public boolean applyfrom(GOut g, GLState from) {
	if(from instanceof Material) {
	    apply(g, (Material)from);
	    return(true);
	}
	return(super.applyfrom(g, from));
    }
    
    private class Wrapping implements Rendered {
	private final Rendered r;
	
	private Wrapping(Rendered r) {
	    this.r = r;
	}
	
	public void draw(GOut g) {
	    g.matsel(Material.this);
	    r.draw(g);
	}

	public boolean setup(RenderList rl) {
	    return(r.setup(rl));
	}
    }
    
    private class FWrapping extends Wrapping implements FRendered {
	private final FRendered f; /* :-P */

	private FWrapping(FRendered r) {
	    super(r);
	    this.f = r;
	}
	
	public void drawflat(GOut g) {
	    g.matsel(null);
	    f.drawflat(g);
	}
    }

    public Rendered apply(Rendered r) {
	if(r instanceof FRendered)
	    return(new FWrapping((FRendered)r));
	else
	    return(new Wrapping(r));
    }
    
    public static class Res extends Resource.Layer {
	public final int id;
	public final transient Material m;
	private int texid = -1;
	
	private static Color col(byte[] buf, int off) {
	    double r = Utils.floatd(buf, off);
	    double g = Utils.floatd(buf, off + 5);
	    double b = Utils.floatd(buf, off + 10);
	    double a = Utils.floatd(buf, off + 15);
	    return(new Color((float)r, (float)g, (float)b, (float)a));
	}

	public Res(Resource res, byte[] buf) {
	    res.super();
	    id = Utils.uint16d(buf, 0);
	    int fl = buf[2];
	    int off = 3;
	    if((fl & 1) != 0) {
		Color amb = col(buf, off); off += 20;
		Color dif = col(buf, off); off += 20;
		Color spc = col(buf, off); off += 20;
		double shine = Utils.floatd(buf, off); off += 5;
		Color emi = col(buf, off); off += 20;
		this.m = new Material(amb, dif, spc, emi, (float)shine);
	    } else {
		this.m = new Material();
	    }
	    if((fl & 2) != 0)
		texid = Utils.uint16d(buf, off); off += 2;
	    if((fl & 4) != 0)
		this.m.facecull = false;
	    if((fl & 8) != 0)
		this.m.mipmap = true;
	    if((fl & ~15) != 0)
		throw(new Resource.LoadException("Unknown material flags: " + fl, getres()));
	}
	
	public void init() {
	    if(texid >= 0) {
		for(Resource.Image img : getres().layers(Resource.imgc)) {
		    if(img.id == texid) {
			m.tex = img.tex();
			if(m.mipmap)
			    ((TexGL)m.tex).mipmap();
		    }
		}
		if(m.tex == null)
		    throw(new Resource.LoadException("Specified texture not found: " + texid, getres()));
	    }
	}
    }
}
