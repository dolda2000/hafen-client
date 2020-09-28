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

import java.util.*;

public class FragTarget {
    public Object buf;
    public final boolean mask[] = {false, false, false, false};
    public BlendMode blend = null;

    public FragTarget(Object buf) {
	this.buf = buf;
    }

    public FragTarget blend(BlendMode blend) {
	this.blend = blend;
	return(this);
    }

    public FragTarget mask(boolean r, boolean g, boolean b, boolean a) {
	mask[0] = r; mask[1] = g; mask[2] = b; mask[3] = a;
	return(this);
    }

    public FragTarget mask(boolean[] mask) {
	if(mask.length != 4)
	    throw(new IllegalArgumentException());
	for(int i = 0; i < 4; i++)
	    this.mask[i] = mask[i];
	return(this);
    }

    public int hashCode() {
	int ret = buf.hashCode();
	ret = (ret * 31) + (mask[0] ? 8 : 0) + (mask[1] ? 4 : 0) + (mask[2] ? 2 : 0) + (mask[3] ? 1 : 0);
	ret = (ret * 31) + ((blend == null) ? 0 : blend.hashCode());
	return(ret);
    }

    public boolean equals(FragTarget that) {
	return((this.buf == that.buf) && Arrays.equals(this.mask, that.mask) && Objects.equals(this.blend, that.blend));
    }

    public boolean equals(Object o) {
	return((o instanceof FragTarget) && this.equals((FragTarget)o));
    }

    public String toString() {return(String.format("#<frag-target %s, %s>", buf, blend));}
}
