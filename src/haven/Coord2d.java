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

import java.util.Iterator;
import static java.lang.Math.PI;

public class Coord2d implements Comparable<Coord2d>, java.io.Serializable {
    public double x, y;
    public static final Coord2d z = new Coord2d(0, 0);

    public Coord2d(double x, double y) {
	this.x = x;
	this.y = y;
    }

    public Coord2d(Coord c) {
	this(c.x, c.y);
    }

    public Coord2d(Coord3f c) {
	this(c.x, c.y);
    }

    public Coord2d() {
	this(0, 0);
    }

    public static Coord2d of(double x, double y) {return(new Coord2d(x, y));}
    public static Coord2d of(double x) {return(of(x, x));}
    public static Coord2d of(Coord c) {return(of(c.x, c.y));}
    public static Coord2d of(Coord3f c) {return(of(c.x, c.y));}

    public boolean equals(double X, double Y) {
	return((x == X) && (y == Y));
    }

    public boolean equals(Object o) {
	if(!(o instanceof Coord2d))
	    return(false);
	Coord2d c = (Coord2d)o;
	return(equals(c.x, c.y));
    }

    public int hashCode() {
	long X = Double.doubleToLongBits(x);
	long Y = Double.doubleToLongBits(y);
	return((((int)(X ^ (X >>> 32))) * 31) + ((int)(Y ^ (Y >>> 32))));
    }

    public int compareTo(Coord2d c) {
	if(c.y < y) return(-1);
	if(c.y > y) return(1);
	if(c.x < x) return(-1);
	if(c.y > y) return(1);
	return(0);
    }

    public Coord2d add(double X, double Y) {
	return(of(x + X, y + Y));
    }

    public Coord2d add(Coord2d b) {
	return(add(b.x, b.y));
    }

    public Coord2d inv() {
	return(of(-x, -y));
    }

    public Coord2d sub(double X, double Y) {
	return(of(x - X, y - Y));
    }

    public Coord2d sub(Coord2d b) {
	return(sub(b.x, b.y));
    }

    public Coord2d mul(double f) {
	return(of(x * f, y * f));
    }

    public Coord2d mul(double X, double Y) {
	return(of(x * X, y * Y));
    }

    public Coord2d mul(Coord2d b) {
	return(mul(b.x, b.y));
    }

    public Coord2d div(double f) {
	return(of(x / f, y / f));
    }

    public Coord2d div(double X, double Y) {
	return(of(x / X, y / Y));
    }

    public Coord2d div(Coord2d b) {
	return(div(b.x, b.y));
    }

    public Coord round() {
	return(Coord.of((int)Math.round(x), (int)Math.round(y)));
    }
    public Coord2d roundf() {
	return(Coord2d.of(Math.round(x), Math.round(y)));
    }

    public Coord floor() {
	return(Coord.of((int)Math.floor(x), (int)Math.floor(y)));
    }
    public Coord2d floorf() {
	return(Coord2d.of(Math.floor(x), Math.floor(y)));
    }

    public Coord floor(double X, double Y) {
	return(Coord.of((int)Math.floor(x / X), (int)Math.floor(y / Y)));
    }

    public Coord floor(Coord2d f) {
	return(floor(f.x, f.y));
    }

    public Coord2d mod() {
	return(of(x - Math.floor(x), y - Math.floor(y)));
    }

    public Coord2d mod(double X, double Y) {
	return(of(x - (Math.floor(x / X) * X), y - (Math.floor(y / Y) * Y)));
    }

    public Coord2d mod(Coord2d f) {
	return(mod(f.x, f.y));
    }

    public double angle(Coord2d o) {
	return(Math.atan2(o.y - y, o.x - x));
    }

    public double dist(Coord2d o) {
	return(Math.hypot(x - o.x, y - o.y));
    }

    public double abs() {
	return(Math.hypot(x, y));
    }

    public Coord2d norm(double n) {
	return(mul(n / abs()));
    }

    public Coord2d norm() {
	return(norm(1.0));
    }

    public static Coord2d sc(double a, double r) {
	return(of(Math.cos(a) * r, Math.sin(a) * r));
    }

    public String toString() {
	return("(" + x + ", " + y + ")");
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

	public GridIsect(Coord2d s, Coord2d t, Coord2d g, boolean incl) {
	    this(s, t, g, z, incl);
	}

	public GridIsect(Coord2d s, Coord2d t, Coord2d g) {
	    this(s, t, g, z, true);
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
}
