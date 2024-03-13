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

public class BaseColor extends State {
    public static final Slot<BaseColor> slot = new Slot<>(Slot.Type.DRAW, BaseColor.class);
    public static final Uniform u_color = new Uniform(VEC4, "basecolor", p -> p.get(slot).color, slot);
    public final FColor color;

    public BaseColor(FColor color) {
	this.color = color;
    }

    public BaseColor(float r, float g, float b, float a) {
	this(new FColor(r, g, b, a));
    }

    public BaseColor(Color color) {
	this(new FColor(color));
    }

    public BaseColor(int r, int g, int b, int a) {
	this(new Color(r, g, b, a));
    }

    public Color color() {
	return(new Color((int)Math.round(color.r * 255), (int)Math.round(color.g * 255),
			 (int)Math.round(color.b * 255), (int)Math.round(color.a * 255)));
    }

    private static final ShaderMacro shader = prog -> {
	FragColor.fragcol(prog.fctx).mod(in -> mul(in, u_color.ref()), 0);
    };
    public ShaderMacro shader() {return(shader);}

    public void apply(Pipe p) {p.put(slot, this);}

    public String toString() {return(String.format("#<basecolor %s %s %s %s>", color.r, color.g, color.b, color.a));}
}
