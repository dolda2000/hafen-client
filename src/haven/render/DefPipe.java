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
import haven.render.State.Slot;

public class DefPipe extends BufPipe {
    public boolean[] mask = {};

    public <T extends State> void put(Slot<? super T> slot, T state) {
	super.put(slot, state);
	if(mask.length <= slot.id)
	    mask = Arrays.copyOf(mask, slot.id + 1);
	mask[slot.id] = true;
    }

    private static int[] ret0 = new int[0];
    public int[] maskdiff(DefPipe that) {
	boolean[] a, b;
	if(this.mask.length < that.mask.length) {
	    a = this.mask; b = that.mask;
	} else {
	    a = that.mask; b = this.mask;
	}
	int n = 0;
	for(int i = 0; i < a.length; i++) {
	    if(a[i] != b[i])
		n++;
	}
	for(int i = a.length; i < b.length; i++) {
	    if(b[i])
		n++;
	}
	if(n == 0)
	    return(ret0);
	int[] ret = new int[n];
	n = 0;
	for(int i = 0; i < a.length; i++) {
	    if(a[i] != b[i])
		ret[n++] = i;
	}
	for(int i = a.length; i < b.length; i++) {
	    if(b[i])
		ret[n++] = i;
	}
	return(ret);
    }
}
