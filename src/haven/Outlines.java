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
import haven.glsl.*;
import static haven.glsl.Cons.*;
import static haven.glsl.Type.*;
import javax.media.opengl.*;

public class Outlines implements Rendered {
    public void draw(GOut g) {}

    private final static Uniform snrm = new Uniform(SAMPLER2D);
    private final static Uniform sdep = new Uniform(SAMPLER2D);
    private final ShaderMacro[] shaders;

    public Outlines(final boolean symmetric) {
	shaders = new ShaderMacro[] {
	    new ShaderMacro() {
		Color color = Color.BLACK;
		Coord[] points = {
		    new Coord(-1,  0),
		    new Coord( 1,  0),
		    new Coord( 0, -1),
		    new Coord( 0,  1),
		};
		Function ofac = new Function.Def(FLOAT) {{
		    Expression tc = Tex2D.texcoord.ref();
		    LValue ret = code.local(FLOAT, l(0.0)).ref();
		    Expression lnrm = code.local(VEC3, mul(sub(pick(texture2D(snrm.ref(), tc), "rgb"), l(0.5)), l(2.0))).ref();
		    Expression ldep = code.local(FLOAT, pick(texture2D(sdep.ref(), tc), "z")).ref();
		    for(int i = 0; i < points.length; i++) {
			Expression ctc = add(tc, mul(vec2(points[i]), MiscLib.pixelpitch.ref()));
			Expression cnrm = mul(sub(pick(texture2D(snrm.ref(), ctc), "rgb"), l(0.5)), l(2.0));
			Expression cdep = pick(texture2D(sdep.ref(), ctc), "z");
			if(symmetric) {
			    code.add(aadd(ret, sub(l(1.0), abs(dot(lnrm, cnrm)))));
			    code.add(aadd(ret, smoothstep(l(1 / 3000.0), l(1 / 2000.0), abs(sub(cdep, ldep)))));
			} else {
			    cnrm = code.local(VEC3, cnrm).ref();
			    code.add(new If(gt(pick(cross(lnrm, cnrm), "z"), l(0.0)),
					    stmt(aadd(ret, sub(l(1.0), abs(dot(lnrm, cnrm)))))));
			    code.add(aadd(ret, smoothstep(l(1 / 3000.0), l(1 / 2000.0), max(sub(ldep, cdep), l(0.0)))));
			}
		    }
		    code.add(new Return(smoothstep(l(0.4), l(0.6), min(ret, l(1.0)))));
		}};

		public void modify(ProgramContext prog) {
		    prog.fctx.fragcol.mod(new Macro1<Expression>() {
			    public Expression expand(Expression in) {
				return(vec4(col3(color), mix(l(0.0), l(1.0), ofac.call())));
			    }
			}, 0);
		}
	    }
	};
    }

    public boolean setup(RenderList rl) {
	final PView.ConfContext ctx = (PView.ConfContext)rl.state().get(PView.ctx);
	final RenderedNormals nrm = ctx.data(RenderedNormals.id);
	ctx.cfg.tdepth = true;
	ctx.cfg.add(nrm);
	rl.prepc(Rendered.postfx);
	rl.add(new Rendered.ScreenQuad(), new States.AdHoc(shaders) {
		private TexUnit tnrm;
		private TexUnit tdep;

		public void reapply(GOut g) {
		    GL2 gl = g.gl;
		    gl.glUniform1i(g.st.prog.uniform(snrm), tnrm.id);
		    gl.glUniform1i(g.st.prog.uniform(sdep), tdep.id);
		}

		public void apply(GOut g) {
		    GL gl = g.gl;
		    (tnrm = g.st.texalloc()).act();
		    gl.glBindTexture(GL.GL_TEXTURE_2D, nrm.tex.glid(g));
		    (tdep = g.st.texalloc()).act();
		    gl.glBindTexture(GL.GL_TEXTURE_2D, ctx.cur.depth.glid(g));
		    reapply(g);
		}

		public void unapply(GOut g) {
		    GL gl = g.gl;
		    tnrm.act();
		    gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
		    tnrm.free(); tnrm = null;
		    tdep.act();
		    gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
		    tdep.free(); tdep = null;
		}
	    });
	return(false);
    }
}
