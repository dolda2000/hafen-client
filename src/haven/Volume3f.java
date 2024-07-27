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

public class Volume3f {
    public Coord3f n, p;

    public Volume3f(Coord3f n, Coord3f p) {
	this.n = n;
	this.p = p;
    }

    public static Volume3f corn(Coord3f n, Coord3f p) {
	return(new Volume3f(n, p));
    }

    public static Volume3f point(Coord3f c) {
	return(corn(c, c));
    }

    public static Volume3f sized(Coord3f n, Coord3f sz) {
	return(new Volume3f(n, n.add(sz)));
    }

    public static Volume3f sized(Coord3f sz) {
	return(sized(Coord3f.o, sz));
    }

    public boolean equals(Volume3f o) {
	return(n.equals(o.n) && p.equals(o.p));
    }

    public boolean equals(Object o) {
	return((o instanceof Volume3f) && equals((Volume3f)o));
    }

    public Coord3f sz() {
	return(p.sub(n));
    }

    public boolean positive() {
	return((p.x > n.x) && (p.y > n.y) && (p.z > n.z));
    }

    public boolean contains(Coord3f c) {
	return((c.x >= n.x) && (c.y >= n.y) && (c.z >= n.z) &&
	       (c.x <= p.x) && (c.y <= p.y) && (c.z <= p.z));
    }

    public boolean isects(Volume3f o) {
	return((p.x > o.n.x) && (p.y > o.n.y) && (p.z > o.n.z) &&
	       (o.p.x > n.x) && (o.p.y > n.y) && (o.p.z > n.z));
    }

    public boolean contains(Volume3f o) {
	return((o.n.x >= n.x) && (o.n.y >= n.y) && (o.n.z >= n.z) &&
	       (o.p.x <= p.x) && (o.p.y <= p.y) && (o.p.z <= p.z));
    }

    public Coord3f closest(Coord3f c) {
	if(contains(c))
	    return(c);
	return(Coord3f.of(Utils.clip(c.x, n.x, p.x),
			  Utils.clip(c.y, n.y, p.y),
			  Utils.clip(c.z, n.z, p.z)));
    }

    public float volume() {
	return((p.x - n.x) * (p.y - n.y) * (p.z * n.z));
    }

    public Volume3f xl(Coord3f off) {
	return(corn(n.add(off), p.add(off)));
    }

    public Volume3f margin(Coord3f nm, Coord3f pm) {
	return(corn(n.sub(nm), p.add(pm)));
    }
    public Volume3f margin(Coord3f m) {
	return(margin(m, m));
    }
    public Volume3f margin(float m) {
	return(margin(Coord3f.of(m, m, m)));
    }

    public Volume3f include(Coord3f c) {
	return(corn(Coord3f.of(Math.min(n.x, c.x), Math.min(n.y, c.y), Math.min(n.z, c.z)),
		    Coord3f.of(Math.max(p.x, c.x), Math.max(p.y, c.y), Math.max(p.z, c.z))));
    }

    public Volume3f include(Volume3f v) {
	return(corn(Coord3f.of(Math.min(n.x, v.n.x), Math.min(n.y, v.n.y), Math.min(n.z, v.n.z)),
		    Coord3f.of(Math.max(p.x, v.p.x), Math.max(p.y, v.p.y), Math.max(p.z, v.p.z))));
    }

    public String toString() {
	return(String.format("(%s - %s)", n, p));
    }
}
