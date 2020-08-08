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

import haven.*;
import static haven.render.sl.Cons.*;
import static haven.render.sl.Function.PDir.*;
import static haven.render.sl.Type.*;

public abstract class MiscLib {
    public static final Function colblend = new Function.Def(VEC4) {{
	Expression base = param(IN, VEC4).ref();
	Expression blend = param(IN, VEC4).ref();
	code.add(new Return(vec4(add(mul(pick(base, "rgb"), sub(l(1.0), pick(blend, "a"))),
				     mul(pick(blend, "rgb"), pick(blend, "a"))),
				 pick(base, "a"))));
    }};
    public static final Function olblend = new Function.Def(VEC4) {{
	Expression base = param(IN, VEC4).ref();
	Expression blend = param(IN, VEC4).ref();
	/* XXX: It seems weird indeed that Haven would be the only
	 * program having trouble with this, but on Intel GPUs, the
	 * sign() function very much appears to be buggy somehow
	 * (quite unclear just how; no explanation I can think of
	 * seems to make sense). Thus, this uses (x * 1000) instead of
	 * sign(x). Assuming color values mapped from uint8_t's, 1000
	 * is enough to saturate the smallest deviations from 0.5. */
	code.add(new Return(vec4(mix(mul(l(2.0), pick(base, "rgb"), pick(blend, "rgb")),
				     sub(l(1.0), mul(l(2.0), sub(l(1.0), pick(base, "rgb")), sub(l(1.0), pick(blend, "rgb")))),
				     clamp(mul(sub(pick(blend, "rgb"), l(0.5)), l(1000.0)), l(0.0), l(1.0))),
				 pick(base, "a"))));
    }};
    public static final Function cpblend = new Function.Def(VEC4) {{
	Expression base = param(IN, VEC4).ref();
	Expression blend = param(IN, VEC4).ref();
	/* XXX: See olblend comment */
	code.add(new Return(vec4(mix(mul(l(2.0), pick(base, "rgb"), pick(blend, "rgb")),
				     sub(l(1.0), mul(l(2.0), sub(l(1.0), pick(base, "rgb")), sub(l(1.0), pick(blend, "rgb")))),
				     clamp(mul(sub(pick(base, "rgb"), l(0.5)), l(1000.0)), l(0.0), l(1.0))),
				 pick(base, "a"))));
    }};

    public static final Function rgb2hsv = new Function.Def(VEC3, "rgb2hsv") {{
	Expression c = param(IN, VEC3).ref();
	Expression p = code.local(VEC4, mix(vec4(pick(c, "bg"), l(-1.0), l( 2.0 / 3.0)),
					    vec4(pick(c, "gb"), l( 0.0), l(-1.0 / 3.0)),
					    step(pick(c, "b"), pick(c, "g")))).ref();
	Expression q = code.local(VEC4, mix(vec4(pick(p, "xyw"), pick(c, "r")),
					    vec4(pick(c, "r"), pick(p, "yzx")),
					    step(pick(p, "x"), pick(c, "r")))).ref();
	Expression d = code.local(FLOAT, sub(pick(q, "x"), min(pick(q, "w"), pick(q, "y")))).ref();
	Expression e = l(1.0e-10);
	code.add(new Return(vec3(abs(add(pick(q, "z"), div(sub(pick(q, "w"), pick(q, "y")), add(mul(l(6.0), d), e)))),
				 div(d, add(pick(q, "x"), e)),
				 pick(q, "x"))));
    }};

    public static final Function hsv2rgb = new Function.Def(VEC3, "hsv2rgb") {{
	Expression c = param(IN, VEC3).ref();
	Expression p = code.local(VEC3, abs(sub(mul(fract(add(pick(c, "xxx"), vec3(1.0, 2.0 / 3.0, 1.0 / 3.0))), l(6.0)), l(3.0)))).ref();
	code.add(new Return(mul(pick(c, "z"), mix(vec3(l(1.0)), clamp(sub(p, l(1.0)), l(0.0), l(1.0)), pick(c, "y")))));
    }};

    public static final Function lin2srgb = new Function.Def(VEC4, "lin2srgb") {{
	Expression c = param(IN, VEC4).ref();
	code.add(new Return(vec4(pow(pick(c, "rgb"), vec3(l(1.0 / 2.2))), pick(c, "a"))));
    }};

    public static final Function srgb2lin = new Function.Def(VEC4, "srgb2lin") {{
	Expression c = param(IN, VEC4).ref();
	code.add(new Return(vec4(pow(pick(c, "rgb"), vec3(l(2.2))), pick(c, "a"))));
    }};
}
