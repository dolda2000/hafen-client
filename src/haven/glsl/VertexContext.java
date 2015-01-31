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

package haven.glsl;

import haven.*;

public class VertexContext extends ShaderContext {
    public VertexContext(ProgramContext prog) {
	super(prog);
    }

    public final Function.Def main = new Function.Def(Type.VOID, new Symbol.Fix("main"));
    public final ValBlock mainvals = new ValBlock();
    private final OrderList<CodeMacro> code = new OrderList<CodeMacro>();
    {
	code.add(new CodeMacro() {
		public void expand(Block blk) {
		    mainvals.cons(blk);
		}
	    }, 0);
    }

    public static final Variable gl_Vertex = new Variable.Implicit(Type.VEC4, new Symbol.Fix("gl_Vertex"));
    public static final Variable gl_Normal = new Variable.Implicit(Type.VEC3, new Symbol.Fix("gl_Normal"));
    public static final Variable gl_Color = new Variable.Implicit(Type.VEC4, new Symbol.Fix("gl_Color"));
    public static final Variable gl_Position = new Variable.Implicit(Type.VEC4, new Symbol.Fix("gl_Position"));
    public static final Variable[] gl_MultiTexCoord = {
	new Variable.Implicit(Type.VEC4, new Symbol.Fix("gl_MultiTexCoord0")),
	new Variable.Implicit(Type.VEC4, new Symbol.Fix("gl_MultiTexCoord1")),
	new Variable.Implicit(Type.VEC4, new Symbol.Fix("gl_MultiTexCoord2")),
	new Variable.Implicit(Type.VEC4, new Symbol.Fix("gl_MultiTexCoord3")),
	new Variable.Implicit(Type.VEC4, new Symbol.Fix("gl_MultiTexCoord4")),
	new Variable.Implicit(Type.VEC4, new Symbol.Fix("gl_MultiTexCoord5")),
	new Variable.Implicit(Type.VEC4, new Symbol.Fix("gl_MultiTexCoord6")),
	new Variable.Implicit(Type.VEC4, new Symbol.Fix("gl_MultiTexCoord7")),
    };

    public static final Uniform wxf = new Uniform.AutoApply(Type.MAT4, "wxf", PView.loc) {
	    public void apply(GOut g, int loc) {
		Location.Chain wxf_s = g.st.get(PView.loc);
		Matrix4f wxf = (wxf_s == null)?Matrix4f.id:wxf_s.fin(Matrix4f.id);
		g.gl.glUniformMatrix4fv(loc, 1, false, wxf.m, 0);
	    }
	};
    public static final Uniform cam = new Uniform.AutoApply(Type.MAT4, "cam", PView.cam) {
	    public void apply(GOut g, int loc) {
		Camera cam_s = g.st.get(PView.cam);
		Matrix4f cam = (cam_s == null)?Matrix4f.id:cam_s.fin(Matrix4f.id);
		g.gl.glUniformMatrix4fv(loc, 1, false, cam.m, 0);
	    }
	};
    public static final Uniform proj = new Uniform.AutoApply(Type.MAT4, "proj", PView.proj) {
	    public void apply(GOut g, int loc) {
		g.gl.glUniformMatrix4fv(loc, 1, false, g.st.proj.m, 0);
	    }
	};
    public static final Uniform mv = new Uniform.AutoApply(Type.MAT4, "mv", PView.loc, PView.cam) {
	    public void apply(GOut g, int loc) {
		Matrix4f mv = Matrix4f.id;
		Camera cam_s = g.st.get(PView.cam);
		if(cam_s != null) mv = cam_s.fin(mv);
		Location.Chain wxf_s = g.st.get(PView.loc);
		if(wxf_s != null) mv = wxf_s.fin(mv);
		g.gl.glUniformMatrix4fv(loc, 1, false, mv.m, 0);
	    }
	};
    public static final Uniform pmv = new Uniform.AutoApply(Type.MAT4, "pmv", PView.loc, PView.cam, PView.proj) {
	    public void apply(GOut g, int loc) {
		Matrix4f pmv = g.st.proj;
		Camera cam_s = g.st.get(PView.cam);
		if(cam_s != null) pmv = cam_s.fin(pmv);
		Location.Chain wxf_s = g.st.get(PView.loc);
		if(wxf_s != null) pmv = wxf_s.fin(pmv);
		g.gl.glUniformMatrix4fv(loc, 1, false, pmv.m, 0);
	    }
	};
     /* If, at some unexpected point in an unexpected future, I were
      * to use anisotropic transforms, this will have to get a matrix
      * inverter implemented for it. */
    public static final Expression nxf = Cons.mat3(mv.ref());

    public final ValBlock.Value objv = mainvals.new Value(Type.VEC4, new Symbol.Gen("objv")) {
	    public Expression root() {
		return(gl_Vertex.ref());
	    }
	};
    public final ValBlock.Value mapv = mainvals.new Value(Type.VEC4, new Symbol.Gen("mapv")) {
	    {softdep(objv);}

	    public Expression root() {
		return(new PostProc.AutoMacro(PostProc.misc) {
			public Expression expand(Context ctx) {
			    if(objv.used) {
				return(new Mul(wxf.ref(), objv.ref()));
			    } else {
				return(new Mul(wxf.ref(), gl_Vertex.ref()));
			    }
			}
		    });
	    }
	};
    public final ValBlock.Value eyev = mainvals.new Value(Type.VEC4, new Symbol.Gen("eyev")) {
	    {softdep(objv); softdep(mapv);}

	    public Expression root() {
		return(new PostProc.AutoMacro(PostProc.misc) {
			public Expression expand(Context ctx) {
			    if(mapv.used) {
				return(new Mul(cam.ref(), mapv.ref()));
			    } else if(objv.used) {
				return(new Mul(mv.ref(), objv.ref()));
			    } else {
				return(new Mul(mv.ref(), gl_Vertex.ref()));
			    }
			}
		    });
	    }
	};
    public final ValBlock.Value eyen = mainvals.new Value(Type.VEC3, new Symbol.Gen("eyen")) {
	    public Expression root() {
		return(new Mul(nxf, gl_Normal.ref()));
	    }
	};
    public final ValBlock.Value posv = mainvals.new Value(Type.VEC4, new Symbol.Gen("posv")) {
	    {
		softdep(objv); softdep(mapv); softdep(eyev);
		force();
	    }

	    public Expression root() {
		return(new PostProc.AutoMacro(PostProc.misc) {
			public Expression expand(Context ctx) {
			    if(eyev.used) {
				return(new Mul(proj.ref(), eyev.ref()));
			    } else if(mapv.used) {
				return(new Mul(proj.ref(), cam.ref(), mapv.ref()));
			    } else if(objv.used) {
				return(new Mul(pmv.ref(), objv.ref()));
			    } else {
				return(new Mul(pmv.ref(), gl_Vertex.ref()));
			    }
			}
		    });
	    }

	    protected void cons2(Block blk) {
		tgt = gl_Position.ref();
		blk.add(new LBinOp.Assign(tgt, init));
	    }
	};

    public void mainmod(CodeMacro macro, int order) {
	code.add(macro, order);
    }

    public void construct(java.io.Writer out) {
	for(CodeMacro macro : code)
	    macro.expand(main.code);
	main.define(this);
	PostProc.autoproc(this);
	output(new Output(out, this));
    }
}
