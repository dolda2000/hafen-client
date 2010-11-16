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

public class Light {
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
