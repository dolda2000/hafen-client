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

package haven;

import java.nio.FloatBuffer;
import haven.render.*;
import haven.render.sl.*;
import haven.render.sl.ValBlock.Value;
import static haven.render.sl.Type.*;

public class ClickLocation<T extends Texture.Image> extends State {
    public static final Slot<ClickLocation> tex = new Slot<>(Slot.Type.SYS, ClickLocation.class);
    public static final FragData fragloc = new FragData(Type.VEC2, "fragloc", p -> p.get(tex).image, tex);
    public static final Attribute vertex = new Attribute(VEC2, "location");
    public final T image;

    public ClickLocation(T image) {
	this.image = image;
    }

    public void apply(Pipe p) {p.put(tex, this);}

    public static final AutoVarying vertloc = new AutoVarying(VEC2) {
	    protected Expression root(VertexContext vctx) {
		return(vertex.ref());
	    }
	};

    public static Value fragloc(FragmentContext fctx) {
	return(fctx.mainvals.ext(fragloc, () -> fctx.mainvals.new Value(VEC2) {
		public Expression root() {
		    return(vertloc.ref());
		}

		protected void cons2(Block blk) {
		    blk.add(new LBinOp.Assign(fragloc.ref(), init));
		}
	    }));
    }

    private static final ShaderMacro shader = prog -> fragloc(prog.fctx).force();
    public ShaderMacro shader() {
	return(shader);
    }

    public static class LocData extends VertexBuf.FloatData {
	public LocData(FloatBuffer data) {
	    super(vertex, 2, data);
	}
    }
}
