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

package haven.resutil;

import java.util.*;
import haven.*;
import haven.glsl.*;
import static haven.glsl.Cons.*;
import static haven.glsl.Function.PDir.*;
import static haven.glsl.Type.*;
import haven.glsl.ValBlock.Value;

@Material.ResName("texrot")
public class TexAnim extends GLState {
    public static final Slot<TexAnim> slot = new Slot<TexAnim>(Slot.Type.DRAW, TexAnim.class);
    public final Coord3f ax;

    public TexAnim(Coord3f ax) {
	this.ax = ax;
    }

    public TexAnim(Resource res, Object... args) {
	this(new Coord3f(((Number)args[0]).floatValue(), ((Number)args[1]).floatValue(), 0));
    }

    private static final Uniform cax = new Uniform(VEC2);
    private static final ShaderMacro shader = prog -> {
	Tex2D.rtexcoord.value(prog.vctx).mod(in -> add(in, mul(cax.ref(), MiscLib.time.ref())), 0);
    };
    public ShaderMacro shader() {return(shader);}

    public void reapply(GOut g) {
	g.gl.glUniform2f(g.st.prog.uniform(cax), ax.x, ax.y);
    }

    public void apply(GOut g) {
	reapply(g);
    }

    public void unapply(GOut g) {}

    public void prep(Buffer buf) {
	buf.put(slot, this);
    }
}
