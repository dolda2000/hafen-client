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
import haven.render.*;
import haven.render.sl.*;
import haven.render.Texture2D.Sampler2D;
import haven.render.sl.ValBlock.Value;
import static haven.render.sl.Cons.*;
import static haven.render.sl.Function.PDir.*;
import static haven.render.sl.Type.*;

public class AlphaTex extends State {
    public static final Slot<AlphaTex> slot = new Slot<AlphaTex>(Slot.Type.GEOM, AlphaTex.class);
    public static final Attribute clipc = new Attribute(VEC2, "clipc");
    public static final MeshBuf.LayerID<MeshBuf.Vec2Layer> lclip = new MeshBuf.V2LayerID(clipc);
    private static final Uniform ctex = new Uniform(SAMPLER2D, p -> p.get(slot).tex, slot);
    private static final Uniform cclip = new Uniform(FLOAT, p -> p.get(slot).cthr, slot);
    public final Sampler2D tex;
    public final float cthr;

    public AlphaTex(Sampler2D tex, float clip) {
	this.tex = tex;
	this.cthr = clip;
    }

    public AlphaTex(Sampler2D tex) {
	this(tex, 0);
    }

    private static final AutoVarying fc = new AutoVarying(VEC2) {
	    {ipol = Interpol.CENTROID;}
	    protected Expression root(VertexContext vctx) {
		return(clipc.ref());
	    }
	};
    private static Value value(FragmentContext fctx) {
	return(fctx.uniform.ext(ctex, () -> fctx.uniform.new Value(VEC4) {
		public Expression root() {
		    return(texture2D(ctex.ref(), fc.ref()));
		}
	    }));
    }
    private static final ShaderMacro main = prog -> {
	final Value val = value(prog.fctx);
	val.force();
	FragColor.fragcol(prog.fctx).mod(in -> mul(in, val.ref()), 100);
    };
    private static final ShaderMacro clip = prog -> {
	final Value val = value(prog.fctx);
	val.force();
	prog.fctx.mainmod(blk -> blk.add(new If(lt(pick(val.ref(), "a"), cclip.ref()),
						new Discard())),
			  -100);
    };

    private static final ShaderMacro shnc = main;
    private static final ShaderMacro shwc = ShaderMacro.compose(main, clip);

    public ShaderMacro shader() {return((cthr > 0)?shwc:shnc);}

    public void apply(Pipe buf) {
	buf.put(slot, this);
    }
}
