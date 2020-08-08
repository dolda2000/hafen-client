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
import static haven.Utils.eq;

public class ProxyPipe implements Pipe {
    private Pipe bk;

    public ProxyPipe(Pipe bk) {
	this.bk = bk;
    }
    public ProxyPipe() {this(Pipe.nil);}

    public <T extends State> T get(Slot<T> slot) {return(bk.get(slot));}
    public State[] states() {return(bk.states());}
    public Pipe copy() {return(bk.copy());}

    public Pipe update(Pipe np) {
	Pipe ret = this.bk;
	this.bk = np;
	return(ret);
    }

    public int[] dupdate(Pipe np) {
	Pipe pp = update(np);
	State[] ns = np.states(), ps = pp.states(), as, bs;
	if(ns.length < ps.length) {
	    as = ns; bs = ps;
	} else {
	    as = ps; bs = ns;
	}
	int[] ret = new int[bs.length];
	int n = 0, i = 0;
	for(; i < as.length; i++) {
	    if(!eq(as[i], bs[i]))
		ret[n++] = i;
	}
	for(; i < bs.length; i++) {
	    if(bs[i] != null)
		ret[n++] = i;
	}
	return(Arrays.copyOf(ret, n));
    }
}
