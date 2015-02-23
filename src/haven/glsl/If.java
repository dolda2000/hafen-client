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

public class If extends Statement {
    public final Expression cond;
    public final Statement t, f;

    public If(Expression cond, Statement t, Statement f) {
	this.cond = cond;
	this.t = t;
	this.f = f;
    }

    public If(Expression cond, Statement t) {
	this(cond, t, null);
    }

    public void walk(Walker w) {
	w.el(cond);
	w.el(t);
	if(f != null) w.el(f);
    }

    public void output(Output out) {
	out.write("if(");
	cond.output(out);
	out.write(")");
	if(t instanceof Block) {
	    Block tb = (Block)t;
	    out.write(" ");
	    tb.trail(out, false);
	    if(f != null)
		out.write(" else");
	} else {
	    out.write("\n"); out.indent++; out.indent();
	    t.output(out);
	    out.indent--;
	    if(f != null) {
		out.write("\n");
		out.indent();
		out.write("else");
	    }
	}
	if(f != null) {
	    if(f instanceof Block) {
		Block fb = (Block)f;
		out.write(" ");
		fb.trail(out, false);
	    } else {
		out.write("\n"); out.indent++; out.indent();
		f.output(out);
		out.indent--;
	    }
	}
    }
}
