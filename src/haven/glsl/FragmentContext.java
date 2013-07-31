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

public class FragmentContext extends ShaderContext {
    public FragmentContext(ProgramContext prog) {
	super(prog);
    }

    public final Function.Def main = new Function.Def(Type.VOID, new Symbol.Fix("main"));
    public final ValBlock mainvals = new ValBlock();
    public final ValBlock uniform = new ValBlock();

    public static final Variable gl_FragColor = new Variable.Implicit(Type.VEC4, new Symbol.Fix("gl_FragColor"));
    public static final Variable gl_FragCoord = new Variable.Implicit(Type.VEC4, new Symbol.Fix("gl_FragCoord"));

    public final ValBlock.Value fragcol = mainvals.new Value(Type.VEC4) {
	    {force();}

	    public Expression root() {
		return(Vec4Cons.u);
	    }

	    protected void cons2(Block blk) {
		blk.add(new LBinOp.Assign(gl_FragColor.ref(), init));
	    }
	};

    public void construct(java.io.Writer out) {
	uniform.cons(main.code);
	main.code.add(new Placeholder("Uniform control up until here."));
	mainvals.cons(main.code);
	main.define(this);
	output(new Output(out, this));
    }
}
