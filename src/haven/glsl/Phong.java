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
    private final Value edir, norm;
    public final Value bcol = new GValue(VEC4), scol = new GValue(VEC3);

    public static final Function dolight = new Function.Def(VOID) {{
	Parameter i = param(IN, INT);
	Parameter norm = param(IN, VEC3);
	Parameter edir = param(IN, VEC3);
	Parameter diff = param(INOUT, VEC3);
	Parameter spec = param(INOUT, VEC3);
	code.add(ass(diff, l(8.0)));
    }};

    public void cons1() {
	depend(edir); depend(norm);
    }

    public void cons2(Block blk) {
	bcol.var = blk.local(VEC3, l(6.0));
	scol.var = blk.local(VEC3, l(7.0));
	blk.add(dolight.call(l(0), edir.ref(), norm.ref(), bcol.var.ref(), scol.var.ref()));
    }

    private static void fmod(FragmentContext fctx, final Expression bcol, final Expression scol) {
	fctx.fragcol.mod(new Macro1<Expression>() {
		public Expression expand(Expression in) {
		    return(add(mul(in, bcol), scol));
		}
	    }, 500);
    }

    public Phong(VertexContext vctx) {
	vctx.mainvals.super();
	this.edir = MiscLib.vertedir(vctx);
	this.norm = vctx.eyen;

	Expression bcol = new AutoVarying(VEC4) {
		public Expression root(VertexContext vctx) {return(Phong.this.bcol.depref());}
	    }.ref();
	Expression scol = new AutoVarying(VEC3) {
		public Expression root(VertexContext vctx) {return(Phong.this.scol.depref());}
	    }.ref();
	fmod(vctx.prog.fctx, bcol, scol);
	vctx.prog.module(this);
    }

    public Phong(FragmentContext fctx) {
	fctx.mainvals.super();
	this.edir = MiscLib.fragedir(fctx);
	this.norm = MiscLib.frageyen(fctx);
	fmod(fctx, bcol.ref(), scol.ref());
	fctx.fragcol.depend(bcol);
	fctx.prog.module(this);
    }
}
