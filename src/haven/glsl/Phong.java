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

import static haven.glsl.Cons.*;
import static haven.glsl.Function.PDir.*;
import static haven.glsl.Type.*;
import haven.glsl.ValBlock.Value;

public class Phong extends ValBlock.Group {
    private final ProgramContext prog;
    private final Expression vert, edir, norm;
    public final Value bcol = new GValue(VEC4), scol = new GValue(VEC3);
    public static final Uniform nlights = new Uniform(INT);

    public class DoLight extends Function.Def {
	public final Variable lvl, df;

	private DoLight() {
	    super(VOID);
	    Expression i = param(IN, INT).ref();
	    Expression vert = param(IN, VEC3).ref();
	    Expression edir = param(IN, VEC3).ref();
	    Expression norm = param(IN, VEC3).ref();
	    LValue diff = param(INOUT, VEC3).ref();
	    LValue spec = param(INOUT, VEC3).ref();
	    Expression ls = idx(prog.gl_LightSource.ref(), i);
	    Expression mat = prog.gl_FrontMaterial.ref();

	    lvl = code.local(FLOAT, null);
	    Variable dir = code.local(VEC3, null);
	    Block.Local rel = new Block.Local(VEC3);
	    Block.Local dist = new Block.Local(FLOAT);
	    code.add(new If(eq(pick(fref(ls, "position"), "w"), l(0.0)),
			    new Block(stmt(ass(lvl, l(1.0))),
				      stmt(ass(dir, pick(fref(ls, "position"), "xyz")))),
			    new Block(rel.new Def(sub(pick(fref(ls, "position"), "xyz"), vert)),
				      stmt(ass(dir, normalize(rel.ref()))),
				      dist.new Def(length(rel.ref())),
				      stmt(ass(lvl, inv(add(fref(ls, "constantAttenuation"),
							    mul(fref(ls, "constantAttenuation"), dist.ref()),
							    mul(fref(ls, "constantAttenuation"), dist.ref(), dist.ref()))))))));
	    code.add(stmt(aadd(diff, mul(pick(fref(mat, "ambient"), "rgb"),
					 pick(fref(ls, "ambient"), "rgb"),
					 lvl.ref()))));
	    df = code.local(FLOAT, dot(norm, dir.ref()));
	    Expression shine = fref(mat, "shininess");
	    Expression refspec = pow(max(dot(edir, reflect(neg(dir.ref()), norm)), l(0.0)), shine);
	    Expression hvspec = pow(max(dot(norm, normalize(add(edir, dir.ref())))), shine);
	    code.add(new If(gt(df.ref(), l(0.0)),
			    new Block(stmt(aadd(diff, mul(pick(fref(mat, "diffuse"), "rgb"),
						pick(fref(ls,  "diffuse"), "rgb"),
							  df.ref(), lvl.ref()))),
				      new If(gt(shine, l(0.5)),
					     stmt(aadd(spec, mul(pick(fref(mat, "specular"), "rgb"),
								 pick(fref(ls,  "specular"), "rgb"),
								 refspec)))))));
	}
    }
    public final DoLight dolight;

    public void cons1() {
    }

    public void cons2(Block blk) {
	bcol.var = blk.local(VEC4, fref(prog.gl_FrontMaterial.ref(), "emission"));
	scol.var = blk.local(VEC3, Vec3Cons.z);
	/*
	Variable i = blk.local(INT, "i", null);
	blk.add(new For(ass(i, l(0)), lt(i.ref(), nlights.ref()), linc(i.ref()),
			stmt(dolight.call(i.ref(), vert, edir, norm, pick(bcol.var.ref(), "rgb"), scol.var.ref()))));
	*/
	for(int i = 0; i < 4; i++) {
	    /* No few drivers seem to be having trouble with the for
	     * loop. It would be nice to be able to select this code
	     * path only on those drivers. */
	    blk.add(new If(gt(nlights.ref(), l(i)),
			   stmt(dolight.call(l(i), vert, edir, norm, pick(bcol.var.ref(), "rgb"), scol.var.ref()))));
	}
	blk.add(ass(pick(bcol.var.ref(), "a"), pick(fref(prog.gl_FrontMaterial.ref(), "diffuse"), "a")));
    }

    private static void fmod(final FragmentContext fctx, final Expression bcol, final Expression scol) {
	fctx.fragcol.mod(new Macro1<Expression>() {
		public Expression expand(Expression in) {
		    return(add(mul(in, bcol), vec4(scol, l(0.0))));
		}
	    }, 500);
    }

    public Phong(VertexContext vctx) {
	vctx.mainvals.super();
	prog = vctx.prog;
	Value edir = MiscLib.vertedir(vctx);
	depend(vctx.eyev); depend(edir); depend(vctx.eyen);
	this.vert = pick(vctx.eyev.ref(), "xyz");
	this.edir = edir.ref();
	this.norm = vctx.eyen.ref();

	Expression bcol = new AutoVarying(VEC4) {
		public Expression root(VertexContext vctx) {return(Phong.this.bcol.depref());}
	    }.ref();
	Expression scol = new AutoVarying(VEC3) {
		public Expression root(VertexContext vctx) {return(Phong.this.scol.depref());}
	    }.ref();
	fmod(vctx.prog.fctx, bcol, scol);
	dolight = new DoLight();
	prog.module(this);
    }

    public Phong(FragmentContext fctx) {
	fctx.mainvals.super();
	prog = fctx.prog;
	Value edir = MiscLib.fragedir(fctx);
	Value norm = MiscLib.frageyen(fctx);
	depend(edir); depend(norm);
	this.vert = MiscLib.frageyev.ref();
	this.edir = edir.ref();
	this.norm = norm.ref();
	fmod(fctx, bcol.ref(), scol.ref());
	fctx.fragcol.depend(bcol);
	dolight = new DoLight();
	prog.module(this);
    }
}
