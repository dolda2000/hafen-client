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

import haven.*;
import haven.glsl.*;
import static haven.glsl.Cons.*;
import static haven.glsl.Type.*;

public class HueMod extends GLState {
    public static final Slot<HueMod> slot = new Slot<>(Slot.Type.DRAW, HueMod.class);
    final float tgthue, huemod, satmod;

    public HueMod(float tgthue, float huemod, float satmod) {
	this.tgthue = tgthue;
	this.huemod = huemod;
	this.satmod = satmod;
    }

    private static final Uniform cxf = new Uniform(VEC3);
    private static final Function apply = new Function.Def(VEC4) {{
	Expression c = param(PDir.IN, Type.VEC4).ref();
	LValue t = code.local(VEC3, MiscLib.rgb2hsv.call(pick(c, "rgb"))).ref();
	Expression th = pick(cxf.ref(), "x");
	Expression hm = pick(cxf.ref(), "y");
	Expression hue = fract(add(mul(sub(fract(add(pick(t, "r"), sub(l(1.0 + 0.5), th))), l(0.5)), hm), add(l(1.0), th)));
	code.add(ass(t, vec3(hue, mul(pick(t, "g"), pick(cxf.ref(), "z")), pick(t, "b"))));
	code.add(new Return(vec4(MiscLib.hsv2rgb.call(t), pick(c, "a"))));
    }};
    private static final ShaderMacro shader = prog -> {
	prog.fctx.fragcol.mod(apply::call, 1000);
    };
    public ShaderMacro shader() {return(shader);}

    public void reapply(GOut g) {
	g.gl.glUniform3f(g.st.prog.uniform(cxf), tgthue, 1.0f - huemod, satmod);
    }

    public void apply(GOut g) {
	reapply(g);
    }

    public void unapply(GOut g) {}

    public void prep(Buffer buf) {
	buf.put(slot, this);
    }
}
