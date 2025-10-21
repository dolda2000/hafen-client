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
    private long fno = 1;

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

	public Part(Object nm) {
	    super(nm);
	    f = System.nanoTime();
	}

	public double f() {return(txl(f));}
	public double t() {return(txl(t));}

	public Part part(Object nm) {
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
	public Frame() {super(String.format("frame %,d", fno++));}

	public void fin() {
	    super.fin();
	    CPUProfile.this.add(this);
	}
    }

    public static final ThreadLocal<Current> current = new ThreadLocal<>();
    public static class Current implements AutoCloseable {
	public final Part part;
	private final Current parent;

	public Current(Part part, Current parent) {
	    this.part = part;
	    this.parent = parent;
	}

	public void fin() {
	    part.fin();
	    current.set(parent);
	}

	public void close() {
	    fin();
	}
    }

    public static Current set(Part part) {
	Current ret = new Current(part, null);
	current.set(ret);
	return(ret);
    }

    public static Current begin(Object nm) {
	Current cur = current.get();
	if(cur == null)
	    return(null);
	Current ret = new Current(cur.part.part(nm), cur);
	current.set(ret);
	return(ret);
    }

    public static Current phase(Object nm) {
	Current cur = current.get();
	if(cur == null)
	    return(null);
	Current ret = new Current(cur.parent.part.part(nm), cur.parent);
	current.set(ret);
	return(ret);
    }

    public static Current phase(Current on, Object nm) {
	Current cur = current.get();
	if(cur == null)
	    return(null);
	Current ret = new Current(on.part.part(nm), on);
	current.set(ret);
	return(ret);
    }

    public static void end(Current cur) {
	if(cur == null)
	    return;
	cur.fin();
    }

    public static void end() {
	end(current.get());
    }
}
