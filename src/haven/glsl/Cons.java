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

public class Cons {
    public static Assign ass(LValue l, Expression r) {
	return(new Assign(l, r));
    }

    public static Assign ass(Variable l, Expression r) {
	return(ass(l.ref(), r));
    }

    public static Add add(Expression... terms) {
	return(new Add(terms));
    }

    public static Mul mul(Expression... terms) {
	return(new Mul(terms));
    }

    public static IntLiteral l(int val) {
	return(new IntLiteral(val));
    }

    public static FloatLiteral l(double val) {
	return(new FloatLiteral(val));
    }

    public static Expression length(Expression x) {return(Function.Builtin.length.call(x));}
    public static Expression distance(Expression x, Expression y) {return(Function.Builtin.distance.call(x, y));}
    public static Expression dot(Expression x, Expression y) {return(Function.Builtin.dot.call(x, y));}
    public static Expression cross(Expression x, Expression y) {return(Function.Builtin.cross.call(x, y));}
}
