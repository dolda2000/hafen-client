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

public class PointSize extends State {
    public static final Slot<State> slot = new Slot<>(Slot.Type.GEOM, State.class);
    public static final Uniform u_ptsz = new Uniform(Type.FLOAT, "pointsize", p -> ((PointSize)p.get(slot)).sz, slot);
    public final float sz;

    public PointSize(float sz) {
	this.sz = sz;
    }

    private static final ShaderMacro shader = prog -> {
	prog.vctx.ptsz.mod(in -> u_ptsz.ref(), 0);
	prog.vctx.ptsz.force();
    };
    public ShaderMacro shader() {return(shader);}
    public void apply(Pipe p) {p.put(slot, this);}
}
