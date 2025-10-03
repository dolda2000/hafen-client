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

package haven;

import java.util.*;

public class CPUProfile extends Profile {
    private final long epoch;

    public CPUProfile(int hl) {
	super(hl);
	epoch = System.nanoTime();
    }

    private double txl(long tm) {
	return((tm - epoch) * 1e-9);
    }

    private class Part extends Profile.Part {
	private long f, t;
	private Part curp = null;

	public Part(String nm) {
	    super(nm);
	    f = System.nanoTime();
	}

	public double f() {return(txl(f));}
	public double t() {return(txl(t));}

	public Part part(String nm) {
	    Part p = new Part(nm);
	    if((curp != null) && (curp.t == 0))
		curp.t = p.f;
	    add(p);
	    return(curp = p);
	}

	private void fin(long tm) {
	    t = tm;
	    if((curp != null) && (curp.t == 0))
		curp.fin(tm);
	}

	public void fin() {
	    fin(System.nanoTime());
	}
    }

    public class Frame extends Part {
	public Frame() {super(null);}

	public void fin() {
	    super.fin();
	    CPUProfile.this.add(this);
	}
    }
}
