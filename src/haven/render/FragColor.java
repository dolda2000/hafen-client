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

import haven.render.sl.*;
import haven.render.sl.ValBlock.Value;
import static haven.Utils.eq;

public class FragColor<T> extends State {
    public static final Slot<FragColor> slot = new Slot<>(Slot.Type.SYS, FragColor.class);
    public static final Slot<FragBlend> blend = new Slot<>(Slot.Type.SYS, FragBlend.class);
    public static final FragData fragcol = new FragData(Type.VEC4, "fragcol", p -> {
	    Object img = p.get(slot).image;
	    FragBlend b = p.get(blend);
	    if(b != null)
		return(new FragTarget(img).blend(b.mode));
	    return(img);
    }, slot, blend).primary();
    public static final Object defcolor = new Object() {
	    public String toString() {return("#<default color buffer>");}
	};
    public final T image;
    public final boolean srgb;

    public FragColor(T image, boolean srgb) {
	this.image = image;
	this.srgb = srgb;
    }

    public FragColor(T image) {
	this(image, false);
    }

    private static class FragBlend extends State {
	final BlendMode mode;

	FragBlend(BlendMode mode) {this.mode = mode;}

	public void apply(Pipe buf) {buf.put(FragColor.blend, this);}
	public ShaderMacro shader() {return(null);}
    }

    public static Pipe.Op blend(BlendMode mode) {
	return(new FragBlend(mode));
    }

    private static class ColorValue extends ValBlock.Value {
	boolean srgb = false;

	ColorValue(ValBlock vals) {
	    vals.super(Type.VEC4);
	}

	public Expression root() {
	    return(Vec4Cons.u);
	}

	protected void cons2(Block blk) {
	    Expression val = init;
	    if(srgb)
		val = MiscLib.lin2srgb.call(val);
	    blk.add(new LBinOp.Assign(fragcol.ref(), val));
	}
    }

    private static ColorValue fragcol0(FragmentContext fctx) {
	return(fctx.mainvals.ext(fragcol, () -> new ColorValue(fctx.mainvals)));
    }
    public static Value fragcol(FragmentContext fctx) {
	return(fragcol0(fctx));
    }

    private static final ShaderMacro value = prog -> fragcol0(prog.fctx).force();
    private static final ShaderMacro mksrgb = prog -> fragcol0(prog.fctx).srgb = true;
    private static final ShaderMacro[] shaders = {value, ShaderMacro.compose(value, mksrgb)};
    public ShaderMacro shader() {
	return(shaders[srgb ? 1 : 0]);
    }

    public void apply(Pipe p) {p.put(slot, this);}

    public int hashCode() {
	return(System.identityHashCode(image));
    }

    public boolean equals(Object o) {
	return((o instanceof FragColor) &&
	       eq(((FragColor)o).image, this.image));
    }

    public String toString() {return(String.format("#<fragcolor %s>", image));}
}
