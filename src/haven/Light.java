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
}
