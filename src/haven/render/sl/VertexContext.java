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

package haven.render.sl;

import java.util.function.*;

public class VertexContext extends ShaderContext {
    public VertexContext(ProgramContext prog) {
	super(prog);
    }

    public final Function.Def main = new Function.Def(Type.VOID, new Symbol.Fix("main"));
    public final ValBlock mainvals = new ValBlock();
    private final OrderList<Consumer<Block>> code = new OrderList<>();
    {
	code.add(mainvals::cons, 0);
    }

    public static final Variable gl_Position = new Variable.Implicit(Type.VEC4, new Symbol.Fix("gl_Position"));
    public static final Variable gl_PointSize = new Variable.Implicit(Type.FLOAT, new Symbol.Fix("gl_PointSize"));

    public static final Variable gl_VertexID = new Variable.Implicit(Type.INT, new Symbol.Fix("gl_VertexID"));
    public static final Variable gl_InstanceID = new Variable.Implicit(Type.INT, new Symbol.Fix("gl_InstanceID"));

    public Expression vertid() {return(gl_VertexID.ref());}
    public Expression instid() {return(gl_InstanceID.ref());}

    public final ValBlock.Value posv = mainvals.new Value(Type.VEC4, new Symbol.Gen("posv")) {
	    {force();}

	    public Expression root() {
		return(Vec4Cons.z);
	    }

	    public void cons2(Block blk) {
		tgt = gl_Position.ref();
		blk.add(new LBinOp.Assign(tgt, init));
	    }
	};
    public final ValBlock.Value ptsz = mainvals.new Value(Type.FLOAT, new Symbol.Gen("ptsz")) {
	    public Expression root() {
		return(new FloatLiteral(1.0));
	    }

	    protected void cons2(Block blk) {
		tgt = gl_PointSize.ref();
		blk.add(new LBinOp.Assign(tgt, init));
	    }
	};

    public void mainmod(Consumer<Block> macro, int order) {
	code.add(macro, order);
    }

    public void construct(java.io.Writer out) {
	for(Consumer<Block> macro : code)
	    macro.accept(main.code);
	main.define(this);
	PostProc.autoproc(this);
	output(new Output(out, this));
    }
}
