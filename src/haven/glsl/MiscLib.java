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

import static haven.glsl.Cons.*;
import static haven.glsl.Function.PDir.*;
import static haven.glsl.Type.*;
import haven.glsl.ValBlock.Value;

public abstract class MiscLib {
    private static final AutoVarying frageyen = new AutoVarying(VEC3, new Symbol.Gen("svar_eyen")) {
	    protected Expression root(VertexContext vctx) {
		return(vctx.eyen.depref());
	    }
	};
    public static Value frageyen(final FragmentContext fctx) {
	return(fctx.mainvals.ext(frageyen, new ValBlock.Factory() {
		public Value make(ValBlock vals) {
		    Value ret = vals.new Value(VEC3, new Symbol.Gen("eyen")) {
			    public Expression root() {
				return(frageyen.ref());
			    }
			};
		    ret.mod(new Macro1<Expression>() {
			    public Expression expand(Expression in) {
				return(normalize(in));
			    }
			}, 0);
		    return(ret);
		}
	    }));
    }
    public static final AutoVarying frageyev = new AutoVarying(VEC3, new Symbol.Gen("svar_eyev")) {
	    protected Expression root(VertexContext vctx) {
		return(pick(vctx.eyev.depref(), "xyz"));
	    }
	};
    private static final Object vertedir_id = new Object();
    public static Value vertedir(final VertexContext vctx) {
	return(vctx.mainvals.ext(vertedir_id, new ValBlock.Factory() {
		public Value make(ValBlock vals) {
		    return(vals.new Value(VEC3, new Symbol.Gen("edir")) {
			    public Expression root() {
				return(normalize(pick(vctx.eyev.depref(), "xyz")));
			    }
			});
		}
	    }));
    }
    private static final Object fragedir_id = new Object();
    public static Value fragedir(final FragmentContext fctx) {
	return(fctx.mainvals.ext(fragedir_id, new ValBlock.Factory() {
		public Value make(ValBlock vals) {
		    return(vals.new Value(VEC3, new Symbol.Gen("edir")) {
			    public Expression root() {
				return(normalize(frageyev.ref()));
			    }
			});
		}
	    }));
    }
}
