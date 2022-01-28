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

package haven.render.gl;

import java.util.*;
import com.jogamp.opengl.GL;

public class TexState extends GLState {
    public final GLTexture[] bound;
    public final int active;

    public TexState(GLTexture[] bound, int active) {
	this.bound = bound;
	this.active = active;
    }

    public void apply(BGL gl) {
	int lb = -1;
	for(int i = 0; i < bound.length; i++) {
	    if(bound[i] != null) {
		gl.glActiveTexture(GL.GL_TEXTURE0 + i);
		bound[i].bind(gl);
		lb = i;
	    }
	}
	if(lb != active)
	    gl.glActiveTexture(GL.GL_TEXTURE0 + active);
    }

    public void unapply(BGL gl) {
	for(int i = 0; i < bound.length; i++) {
	    if(bound[i] != null) {
		gl.glActiveTexture(GL.GL_TEXTURE0 + i);
		bound[i].unbind(gl);
	    }
	}
    }

    public void applyto(BGL gl, GLState to) {
	TexState that = (TexState)to;
	int lb = active;
	for(int i = 0; i < Math.min(this.bound.length, that.bound.length); i++) {
	    if(this.bound[i] != that.bound[i]) {
		gl.glActiveTexture(GL.GL_TEXTURE0 + i);
		that.bound[i].bind(gl);
		lb = i;
	    }
	}
	if(this.bound.length > that.bound.length) {
	    for(int i = that.bound.length; i < this.bound.length; i++) {
		if(this.bound[i] != null) {
		    gl.glActiveTexture(GL.GL_TEXTURE0 + i);
		    this.bound[i].unbind(gl);
		    lb = i;
		}
	    }
	} else if(that.bound.length > this.bound.length) {
	    for(int i = this.bound.length; i < that.bound.length; i++) {
		if(that.bound[i] != null) {
		    gl.glActiveTexture(GL.GL_TEXTURE0 + i);
		    that.bound[i].bind(gl);
		    lb = i;
		}
	    }
	}
	if(lb != that.active)
	    gl.glActiveTexture(GL.GL_TEXTURE0 + that.active);
    }

    public static void act(BGL gl, Applier st, int unit) {
	TexState cur = (TexState)st.glstates[slot];
	if((cur == null) || (cur.active != unit)) {
	    st.apply(gl, new TexState(cur.bound, unit));
	}
    }

    public static void bind(BGL gl, Applier st, int unit, GLTexture tex) {
	TexState cur = (TexState)st.glstates[slot];
	if(tex != null) {
	    if((cur == null) || (cur.bound.length <= unit) || (cur.bound[unit] != tex)) {
		GLTexture[] nbind = (cur == null) ? new GLTexture[unit + 1] : Arrays.copyOf(cur.bound, Math.max(cur.bound.length, unit + 1));
		nbind[unit] = tex;
		st.apply(gl, new TexState(nbind, unit));
	    }
	} else {
	    if((cur != null) && (cur.bound.length > unit) && (cur.bound[unit] != null)) {
		if(unit == cur.bound.length - 1) {
		    int i;
		    for(i = cur.bound.length - 2; (i >= 0) && (cur.bound[i] == null); i--);
		    if(i < 0)
			st.apply(gl, slot, null);
		    else
			st.apply(gl, new TexState(Arrays.copyOf(cur.bound, i + 1), i));
		} else {
		    GLTexture[] nbind = Arrays.copyOf(cur.bound, cur.bound.length);
		    nbind[unit] = null;
		    st.apply(gl, new TexState(nbind, unit));
		}
	    }
	}
    }

    public static int slot = slotidx(TexState.class);
    public int slotidx() {return(slot);}
}
