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

package haven.render;

import haven.*;
import haven.render.sl.*;
import static haven.render.sl.Type.*;
import static haven.render.sl.Cons.*;

public class Ortho2D extends State {
    public final float l, u, r, b;
    private final float[] k, m;
    public static final Attribute pos = new Attribute(VEC2, "opos2d");
    private static final Uniform kv = new Uniform(VEC2, "k2d", p -> ((Ortho2D)p.get(States.vxf)).k, States.vxf);
    private static final Uniform mv = new Uniform(VEC2, "m2d", p -> ((Ortho2D)p.get(States.vxf)).m, States.vxf);

    public Ortho2D(float l, float u, float r, float b) {
	this.l = l; this.u = u; this.r = r; this.b = b;
	float w = r - l; float h = b - u;
	k = new float[] {2f / w, -2f / h};
	m = new float[] {-1 - (l * k[0]), 1 - (u * k[1])};
    }

    public Ortho2D(Area area) {
	this(area.ul.x, area.ul.y, area.br.x, area.br.y);
    }

    private static final ShaderMacro shader = prog -> {
	prog.vctx.posv.mod(in -> vec4(add(mv.ref(), mul(pos.ref(), kv.ref())), l(0.0), l(1.0)), 0);
    };
    public ShaderMacro shader() {return(shader);}
    public void apply(Pipe p) {p.put(States.vxf, this);}

    public String toString() {
	return(String.format("#<ortho2d (%s, %s)-(%s, %s)>", l, u, r, b));
    }
}
