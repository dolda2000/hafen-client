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

public class Line2d {
    public final Coord2d m, k;

    public Line2d(Coord2d m, Coord2d k) {
	this.m = m;
	this.k = k;
    }

    public static Line2d from(Coord2d m, Coord2d k) {return(new Line2d(m, k));}
    public static Line2d twixt(Coord2d a, Coord2d b) {return(new Line2d(a, b.sub(a)));}

    public Coord2d end() {
	return(m.add(k));
    }

    public Coord2d at(double t) {
	return(m.add(k.mul(t)));
    }

    public Coord2d cross(Line2d that) {
	Coord2d m = this.m, k = this.k, n = that.m, j = that.k;
	double det = (k.x * j.y) - (k.y * j.x);
	if(det == 0)
	    return(Coord2d.of(Double.NaN, Double.NaN));
	Coord2d mn = m.sub(n);
	det = 1.0 / det;
	return(Coord2d.of(det * ((j.x * mn.y) - (j.y * mn.x)),
			  det * ((k.x * mn.y) - (k.y * mn.x))));
    }

    public static class GridIsect implements DefaultCollection<Coord2d> {
	public final Coord2d s, t, d, g, o;
	public final boolean incl;

	public GridIsect(Coord2d s, Coord2d t, Coord2d g, Coord2d o, boolean incl) {
	    this.s = s;
	    this.t = t;
	    this.d = t.sub(s);
	    this.g = g;
	    this.o = o;
	    this.incl = incl;
	}

	public GridIsect(Line2d line, Coord2d g, Coord2d o, boolean incl) {
	    this(line.m, line.end(), g, o, incl);
	}

	public GridIsect(Coord2d s, Coord2d t, Coord2d g, boolean incl) {
	    this(s, t, g, Coord2d.z, incl);
	}

	public GridIsect(Coord2d s, Coord2d t, Coord2d g) {
	    this(s, t, g, Coord2d.z, true);
	}

	public GridIsect(Line2d line, Coord2d g) {
	    this(line, g, Coord2d.z, true);
	}

	public Iterator<Coord2d> iterator() {
	    return(new Iterator<Coord2d>() {
		    Coord2d next = s;;
		    double nx, ny, nxt, nyt;
		    {
			if(d.x > 0)
			    nx = Math.floor((s.x - o.x) / g.x) + 1;
			else
			    nx = Math.ceil ((s.x - o.x) / g.x) - 1;
			nxt = Math.abs(((nx * g.x) + o.x - s.x) / d.x);
			if(d.y > 0)
			    ny = Math.floor((s.y - o.y) / g.y) + 1;
			else
			    ny = Math.ceil ((s.y - o.y) / g.y) - 1;
			nyt = Math.abs(((ny * g.y) + o.y - s.y) / d.y);
		    }

		    public boolean hasNext() {
			return(next != null);
		    }

		    public Coord2d next() {
			if(next == null)
			    throw(new java.util.NoSuchElementException());
			Coord2d ret = next;
			boolean ux = false, uy = false;
			if(next == t) {
			    next = null;
			} else if((nxt >= 1) && (nyt >= 1)) {
			    next = incl ? t : null;
			} else if(nxt == nyt) {
			    next = Coord2d.of((nx * g.x) + o.x, (ny * g.y) + o.y);
			    ux = uy = true;
			} else if(nxt < nyt) {
			    next = Coord2d.of((nx * g.x) + o.x, s.y + (d.y * nxt));
			    ux = true;
			} else {
			    next = Coord2d.of(s.x + (d.x * nyt), (ny * g.y) + o.y);
			    uy = true;
			}
			if(ux) {
			    nx += (d.x > 0) ? 1 : -1;
			    nxt = ((nx * g.x) + o.x - s.x) / d.x;
			}
			if(uy) {
			    ny += (d.y > 0) ? 1 : -1;
			    nyt = ((ny * g.y) + o.y - s.y) / d.y;
			}
			return(ret);
		    }
		});
	}
    }

    public Collection<Coord2d> gridisect(Coord2d g, Coord2d o, boolean incl) {return(new GridIsect(this, g, o, incl));}
    public Collection<Coord2d> gridisect(Coord2d g, boolean incl) {return(gridisect(g, Coord2d.z, incl));}
    public Collection<Coord2d> gridisect(Coord2d g) {return(gridisect(g, true));}
}
