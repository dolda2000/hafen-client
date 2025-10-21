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
import java.io.*;
import java.awt.Color;

public class Profile {
    public final Part[] hist;
    protected int i = 0;

    public static abstract class Part {
	public final Object nm;
	private List<Part> ch = null;

	public Part(Object nm) {
	    this.nm = nm;
	}

	public abstract double f();
	public abstract double t();

	public double d() {
	    return(t() - f());
	}

	public void add(Part sub) {
	    if(ch == null)
		ch = new ArrayList<>();
	    ch.add(sub);
	}

	public List<Part> sub() {
	    return((ch != null) ? ch : Collections.emptyList());
	}

	private static final String[] units = {"s", "ms", "\u00b5s", "ns"};
	private void dump(PrintStream out, int indent) {
	    for(int i = 0; i < indent; i++)
		out.print("    ");
	    if(nm != null)
		out.print(nm + ": ");
	    double d = d();
	    for(int i = 0; i < units.length; i++, d *= 1e3) {
		if((d > 1) || (i == units.length - 1)) {
		    out.print(String.format("%.2f %s\n", d, units[i]));
		    break;
		}
	    }
	    List<Part> bytime = new ArrayList<>(sub());
	    Collections.sort(bytime, Comparator.comparing(Part::d).reversed());
	    for(Part p : bytime)
		p.dump(out, indent + 1);
	}

	public void dump(PrintStream out) {
	    dump(out, 0);
	}

	public String toString() {
	    StringBuilder buf = new StringBuilder();
	    for(Part p : sub()) {
		if(buf.length() > 0)
		    buf.append(", ");
		buf.append(p.nm + ": " + p.d());
	    }
	    buf.append(", total: " + d());
	    return(buf.toString());
	}
    }

    public void add(Part frame) {
	hist[i] = frame;
	if(++i >= hist.length)
	    i = 0;
    }

    public Profile(int hl) {
	hist = new Part[hl];
    }

    public Part last() {
	if(i == 0)
	    return(hist[hist.length - 1]);
	return(hist[i - 1]);
    }

    public Profile copy() {
	Profile ret = new Profile(hist.length);
	System.arraycopy(this.hist, 0, ret.hist, 0, hist.length);
	ret.i = this.i;
	return(ret);
    }

    public void dump(PrintStream out) {
	String[] parts = new String[0];
	double[] avg = new double[0];
	double[] min = new double[0];
	double[] max = new double[0];
	int n = 0;
	for(Part f : hist) {
	    if(f == null)
		continue;
	    List<Part> prt = f.sub();
	    for(int i = 0; i <= prt.size(); i++) {
		String nm; double tm;
		if(i < f.sub().size()) {
		    nm = prt.get(i).nm.toString();
		    tm = prt.get(i).d();
		} else {
		    nm = "total";
		    tm = f.d();
		}
		int o;
		for(o = 0; o < parts.length; o++) {
		    if(parts[o].equals(nm))
			break;
		}
		if(o < parts.length) {
		    avg[o] += tm;
		    min[o] = Math.min(min[o], tm);
		    max[o] = Math.max(max[o], tm);
		} else {
		    parts = Utils.extend(parts, parts.length + 1);
		    avg = Utils.extend(avg, avg.length + 1);
		    min = Utils.extend(min, min.length + 1);
		    max = Utils.extend(max, max.length + 1);
		    parts[o] = nm;
		    avg[o] = min[o] = max[o] = tm;
		}
	    }
	    n++;
	}
	for(int i = 0; i < avg.length; i++)
	    avg[i] /= n;
	double[] vsum = new double[avg.length];
	for(Part f : hist) {
	    if(f == null)
		continue;
	    List<Part> prt = f.sub();
	    for(int i = 0; i <= prt.size(); i++) {
		String nm; double tm;
		if(i < prt.size()) {
		    nm = prt.get(i).nm.toString();
		    tm = prt.get(i).d();
		} else {
		    nm = "total";
		    tm = f.d();
		}
		int o;
		for(o = 0; o < parts.length; o++) {
		    if(parts[o].equals(nm))
			break;
		}
		vsum[o] += Math.pow(avg[o] - tm, 2);
	    }
	}
	for(int i = 0; i < vsum.length; i++)
	    vsum[i] = Math.sqrt(vsum[i] / n);
	for(int i = 0; i < parts.length; i++)
	    out.println(String.format("%s: %.2f\u00b1%.2f (%.2f-%.2f)", parts[i], avg[i] * 1000, vsum[i] * 1000, min[i] * 1000, max[i] * 1000));
	out.println();
    }
}
