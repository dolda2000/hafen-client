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

public class For extends Statement {
    public final Expression init, cond, step;
    public final Statement body;

    public For(Expression init, Expression cond, Expression step, Statement body) {
	this.init = init;
	this.cond = cond;
	this.step = step;
	this.body = body;
    }

    public void walk(Walker w) {
	if(init != null) w.el(init);
	if(cond != null) w.el(cond);
	if(step != null) w.el(step);
	w.el(body);
    }

    public void output(Output out) {
	out.write("for(");
	if(init != null)
	    init.output(out);
	out.write("; ");
	if(cond != null)
	    cond.output(out);
	out.write("; ");
	if(step != null)
	    step.output(out);
	out.write(")");
	if(body instanceof Block) {
	    out.write(" ");
	    ((Block)body).trail(out, false);
	} else {
	    out.write("\n"); out.indent++; out.indent();
	    body.output(out);
	    out.indent--;
	}
    }
}
