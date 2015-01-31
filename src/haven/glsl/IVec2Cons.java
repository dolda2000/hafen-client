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

public class IVec2Cons extends Expression {
    public static final IVec2Cons z = new IVec2Cons(IntLiteral.z, IntLiteral.z);
    public static final IVec2Cons u = new IVec2Cons(IntLiteral.u, IntLiteral.u);
    public final Expression[] els;

    public IVec2Cons(Expression... els) {
	if((els.length < 1) || (els.length > 2))
	    throw(new RuntimeException("Invalid number of arguments for ivec2: " + els.length));
	this.els = els;
    }

    public void walk(Walker w) {
	for(Expression el : els)
	    w.el(el);
    }

    public void output(Output out) {
	out.write("ivec2(");
	els[0].output(out);
	for(int i = 1; i < els.length; i++) {
	    out.write(", ");
	    els[i].output(out);
	}
	out.write(")");
    }
}
