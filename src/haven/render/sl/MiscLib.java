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
}