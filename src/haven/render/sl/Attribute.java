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

public class Attribute extends Variable.Global {
    public Attribute(Type type, Symbol name) {
	super(type, name);
    }

    public Attribute(Type type, String infix) {
	this(type, new Symbol.Shared("s_" + infix));
    }

    public Attribute(Type type) {
	this(type, new Symbol.Shared());
    }

    private class Def extends Definition {
	public void output(Output out) {
	    if(out.ctx instanceof VertexContext) {
		VertexContext vctx = (VertexContext)out.ctx;
		vctx.prog.attribs.add(Attribute.this);
		out.write("in ");
	    } else {
		throw(new RuntimeException("use of attribute variable outside vertex context: " + Attribute.this));
	    }
	    super.output(out);
	}
    }

    public void use(Context ctx) {
	type.use(ctx);
	if(!defined(ctx))
	    ctx.vardefs.add(new Def());
    }
}
