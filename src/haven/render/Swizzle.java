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
import java.util.Arrays;

public class Swizzle implements java.io.Serializable {
    public static final Swizzle ID3 = id(3);
    public static final Swizzle ID4 = id(4);
    public static final Swizzle BGR = new Swizzle(2, 1, 0);
    public static final Swizzle BGRA = new Swizzle(2, 1, 0, 3);
    public final int[] perm;

    public Swizzle(int... perm) {
	this.perm = perm;
    }

    public boolean idp() {
	for(int i = 0; i < perm.length; i++) {
	    if(perm[i] != i)
		return(false);
	}
	return(true);
    }

    public String toString() {
	StringBuilder buf = new StringBuilder();
	buf.append("#<swizzle ");
	for(int i = 0; i < perm.length; i++) {
	    if(i > 0)
		buf.append(',');
	    buf.append(perm[i]);
	}
	buf.append(">");
	return(buf.toString());
    }

    public boolean equals(Object o) {
	if(!(o instanceof Swizzle))
	    return(false);
	Swizzle that = (Swizzle)o;
	if(this.perm.length != that.perm.length)
	    return(false);
	for(int i = 0; i < perm.length; i++) {
	    if(this.perm[i] != that.perm[i])
		return(false);
	}
	return(true);
    }

    public int hashCode() {
	return(Arrays.hashCode(perm));
    }

    public static Swizzle id(int nc) {
	int[] perm = new int[nc];
	for(int i = 0; i < nc; i++)
	    perm[i] = i;
	return(new Swizzle(perm));
    }
}
