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
import static haven.Utils.c2fa;

public class Material {
    public float[] amb, dif, spc, emi;
    public float shine;
    public Tex tex;
    
    public Material() {
	amb = new float[] {0.2f, 0.2f, 0.2f, 1.0f};
	dif = new float[] {0.8f, 0.8f, 0.8f, 1.0f};
	spc = new float[] {0.0f, 0.0f, 0.0f, 1.0f};
	emi = new float[] {0.0f, 0.0f, 0.0f, 1.0f};
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
	    int t = buf[2];
	    Color amb = col(buf, 3);
	    Color dif = col(buf, 23);
	    Color spc = col(buf, 43);
	    double shine = Utils.floatd(buf, 63);
	    Color emi = col(buf, 68);
	    this.m = new Material(amb, dif, spc, emi, (float)shine);
	    if(t == 1) {
	    } else if(t == 2) {
		texid = Utils.uint16d(buf, 88);
	    } else {
		throw(new Resource.LoadException("Unknown material type: " + t, getres()));
	    }
	}
	
	public void init() {
	    if(texid >= 0) {
		for(Resource.Image img : getres().layers(Resource.imgc)) {
		    if(img.id == texid)
			m.tex = img.tex();
		}
		if(m.tex == null)
		    throw(new Resource.LoadException("Specified texture not found: " + texid, getres()));
	    }
	}
    }
}
