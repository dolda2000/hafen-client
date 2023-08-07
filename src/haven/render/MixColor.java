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

import java.awt.Color;
import haven.*;
import haven.render.sl.*;
import static haven.render.sl.Cons.*;
import static haven.render.sl.Type.*;

public class MixColor extends State implements InstanceBatch.AttribState {
    public static final Slot<MixColor> slot = new Slot<>(Slot.Type.DRAW, MixColor.class)
	.instanced(new Instancable<MixColor>() {
		final Instancer<MixColor> nil = Instancer.dummy();
		public Instancer<MixColor> instid(MixColor st) {
		    return((st == null) ? nil : instancer);
		}
	    });
    public static final InstancedUniform u_color = new InstancedUniform.Vec4("mixcolor", p -> p.get(slot).color, slot);
    public final float[] color;

    public MixColor(float[] color) {
	this.color = color;
    }

    public MixColor(FColor color) {
	this(color.to4a());
    }

    public MixColor(float r, float g, float b, float a) {
	this(new FColor(r, g, b, a));
    }

    public MixColor(Color color) {
	this(new FColor(color));
    }

    public MixColor(int r, int g, int b, int a) {
	this(new Color(r, g, b, a));
    }

    public static final AutoVarying transfer = new AutoVarying(Type.VEC4) {
	    protected Expression root(VertexContext vctx) {
		return(u_color.ref());
	    }
	};
    private static final ShaderMacro shader = prog -> {
	FragColor.fragcol(prog.fctx).mod(in -> MiscLib.colblend.call(in, transfer.ref()), 0);
    };
    public ShaderMacro shader() {return(shader);}

    public void apply(Pipe p) {p.put(slot, this);}

    private static final Instancer<MixColor> instancer = new Instancer<MixColor>() {
	    final MixColor instanced = new MixColor((float[])null) {
		    final ShaderMacro shader = ShaderMacro.compose(mkinstanced, MixColor.shader);
		    public ShaderMacro shader() {return(shader);}
		};
	    public MixColor inststate(MixColor uinst, InstanceBatch bat) {
		return(instanced);
	    }
	};
    public InstancedAttribute[] attribs() {
	return(new InstancedAttribute[] {u_color.attrib});
    }
}
