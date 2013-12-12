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

package haven.resutil;

import java.util.*;
import haven.*;
import haven.glsl.*;
import static haven.glsl.Cons.*;
import static haven.glsl.Function.PDir.*;
import static haven.glsl.Type.*;
import haven.glsl.ValBlock.Value;

public class AlphaTex extends GLState {
    public static final Slot<AlphaTex> slot = new Slot<AlphaTex>(Slot.Type.DRAW, AlphaTex.class);
    public static final Attribute clipc = new Attribute(VEC2);
    public static final MeshBuf.LayerID<MeshBuf.Vec2Layer> lclip = new MeshBuf.V2LayerID(clipc);
    private static final Uniform ctex = new Uniform(SAMPLER2D);
    public final TexGL tex;
    private TexUnit sampler;

    public AlphaTex(TexGL tex) {
	this.tex = tex;
    }

    private static final ShaderMacro[] shaders = {
	new ShaderMacro() {
	    final AutoVarying fc = new AutoVarying(VEC2) {
		    {ipol = Interpol.CENTROID;}
		    protected Expression root(VertexContext vctx) {
			return(clipc.ref());
		    }
		};
	    public void modify(ProgramContext prog) {
		prog.dump = true;
		final ValBlock.Value val = prog.fctx.uniform.new Value(FLOAT) {
			public Expression root() {
			    return(pick(texture2D(ctex.ref(), fc.ref()), "r"));
			}
		    };
		val.force();
		prog.fctx.fragcol.mod(new Macro1<Expression>() {
			public Expression expand(Expression in) {
			    return(mul(in, vec4(l(1.0), l(1.0), l(1.0), val.ref())));
			}
		    }, 100);
	    }
	}
    };

    public ShaderMacro[] shaders() {return(shaders);}
    public boolean reqshader() {return(true);}

    public void reapply(GOut g) {
	g.gl.glUniform1i(g.st.prog.uniform(ctex), sampler.id);
    }

    public void apply(GOut g) {
	sampler = TexGL.lbind(g, tex);
	reapply(g);
    }

    public void unapply(GOut g) {
	sampler.ufree(); sampler = null;
    }

    public void prep(Buffer buf) {
	buf.put(slot, this);
    }
}
