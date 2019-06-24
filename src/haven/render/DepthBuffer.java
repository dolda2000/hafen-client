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
import static haven.Utils.eq;

public class DepthBuffer<T> extends State {
    public static final Slot<DepthBuffer> slot = new Slot<>(Slot.Type.SYS, DepthBuffer.class);
    public static final Object defdepth = new Object() {
	    public String toString() {return("#<default depth buffer>");}
	};
    public final T image;

    public DepthBuffer(T image) {
	this.image = image;
    }

    public ShaderMacro shader() {return(null);}
    public void apply(Pipe p) {p.put(slot, this);}

    public int hashCode() {
	return(System.identityHashCode(image));
    }

    public boolean equals(Object o) {
	return((o instanceof DepthBuffer) &&
	       eq(((DepthBuffer)o).image, this.image));
    }

    public String toString() {return(String.format("#<depthbuf %s>", image));}
}
