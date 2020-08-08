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

import haven.*;
import haven.render.sl.*;

public class FrameConfig extends State {
    public static final Slot<FrameConfig> slot = new Slot<>(Slot.Type.SYS, FrameConfig.class);
    public static final Uniform u_numsamples = new Uniform(Type.INT, "numsamples", p -> p.get(slot).samples, slot);
    public static final Uniform u_screensize = new Uniform(Type.VEC2, "screensize", p -> p.get(slot).sz, slot);
    public static final Uniform u_pixelpitch = new Uniform(Type.VEC2, "pixelpitch", p -> {
	    Coord sz = p.get(slot).sz;
	    return(new float[] {1.0f / sz.x, 1.0f / sz.y});
	}, slot);
    public final Coord sz;
    public final int samples;

    public FrameConfig(Coord sz, int samples) {
	this.sz = sz;
	this.samples = samples;
    }

    public FrameConfig(Coord sz) {
	this(sz, 1);
    }

    public ShaderMacro shader() {return(null);}
    public void apply(Pipe p) {p.put(slot, this);}

    public String toString() {return(String.format("#<framecfg %sx%s %s>", sz.x, sz.y, samples));}
}
