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

public abstract class AutoVarying extends Varying {
    public AutoVarying(Type type, Symbol name) {
	super(type, name);
    }

    public AutoVarying(Type type, String prefix) {
	this(type, new Symbol.Shared(prefix));
    }

    public AutoVarying(Type type) {
	this(type, new Symbol.Shared());
    }

    public abstract class Value extends ValBlock.Value {
	public Value(ValBlock blk) {
	    blk.super(AutoVarying.this.type, AutoVarying.this.name);
	}

	protected void cons2(Block blk) {
	    tgt = AutoVarying.this.ref();
	    blk.add(new LBinOp.Assign(tgt, init));
	}
    }

    protected Expression root(VertexContext vctx) {
	throw(new Error("Neither make() nor root() overridden"));
    }

    protected Value make(ValBlock vals, final VertexContext vctx) {
	return(new Value(vals) {
		public Expression root() {
		    return(AutoVarying.this.root(vctx));
		}
	    });
    }

    public ValBlock.Value value(final VertexContext ctx) {
	return(ctx.mainvals.ext(this, new ValBlock.Factory() {
		public ValBlock.Value make(ValBlock vals) {
		    return(AutoVarying.this.make(vals, ctx));
		}
	    }));
    }

    public void use(Context ctx) {
	if(ctx instanceof FragmentContext) {
	    FragmentContext fctx = (FragmentContext)ctx;
	    value(fctx.prog.vctx).force();
	}
	super.use(ctx);
    }
}
