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
import javax.media.opengl.*;
import haven.glsl.*;
import static haven.glsl.Cons.*;
import static haven.glsl.Function.PDir.*;
import static haven.glsl.Type.*;

public class CloudShadow extends GLState {
    public static final Slot<CloudShadow> slot = new Slot<CloudShadow>(Slot.Type.DRAW, CloudShadow.class, Light.lighting);
    public final TexGL tex;

    public static final CloudShadow def = new CloudShadow((TexGL)Resource.load("gfx/fx/clouds").loadwait().layer(TexR.class).tex());
    public CloudShadow(TexGL tex) {
	this.tex = tex;
    }

    public static final Uniform tsky = new Uniform(SAMPLER2D);
    private static final ShaderMacro[] shaders = {
	new ShaderMacro() {
	    final Expression v = vec2(8, 4);
	    final Expression scale = l(1.0 / 1000.0);

	    public void modify(ProgramContext prog) {
		final Phong ph = prog.getmod(Phong.class);
		if((ph == null) || !ph.pfrag)
		    return;
		final ValBlock.Value shval = prog.fctx.uniform.new Value(FLOAT) {
			public Expression root() {
			    return(pick(texture2D(tsky.ref(), mul(add(add(pick(MiscLib.fragmapv.ref(), "xy"),
									  mul(pick(MiscLib.fragmapv.ref(), "z"), l(0.25))),
								      mul(v, MiscLib.time.ref())), scale)), "r"));
			}

			protected void cons2(Block blk) {
			    var = new Variable.Global(FLOAT);
			    blk.add(ass(var, init));
			}
		    };
		shval.force();
		ph.dolight.mod(new Runnable() {
			public void run() {
			    ph.dolight.dcalc.add(new If(eq(MapView.amblight.ref(), ph.dolight.i),
							stmt(amul(ph.dolight.dl.var.ref(), shval.ref()))),
						 ph.dolight.dcurs);
			}
		    }, 0);
	    }
	}
    };

    public ShaderMacro[] shaders() {return(shaders);}
    public boolean reqshaders() {return(true);}

    private TexUnit sampler;

    public void reapply(GOut g) {
	int u = g.st.prog.cuniform(tsky);
	if(u >= 0)
	    g.gl.glUniform1i(u, sampler.id);
    }

    public void apply(GOut g) {
	sampler = TexGL.lbind(g, tex);
	reapply(g);
    }

    public void unapply(GOut g) {
	sampler.act();
	g.gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
	sampler.free(); sampler = null;
    }

    public void prep(Buffer buf) {
	buf.put(slot, this);
    }
}
