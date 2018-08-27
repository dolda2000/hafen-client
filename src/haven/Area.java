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

public class Area implements Iterable<Coord>, java.io.Serializable {
    public Coord ul, br;

    public Area(Coord ul, Coord br) {
	this.ul = ul;
	this.br = br;
    }

    public int hashCode() {
	return((ul.hashCode() * 31) + br.hashCode());
    }

    public boolean equals(Object o) {
	if(!(o instanceof Area))
	    return(false);
	Area a = (Area)o;
	return(a.ul.equals(ul) && a.br.equals(br));
    }

    public static Area sized(Coord ul, Coord sz) {
	return(new Area(ul, ul.add(sz)));
    }

    public Coord sz() {
	return(br.sub(ul));
    }

    public boolean contains(Coord c) {
	return((c.x >= ul.x) && (c.y >= ul.y) && (c.x < br.x) && (c.y < br.y));
    }

    public boolean isects(Area o) {
	return((br.x > o.ul.x) && (br.y > o.ul.y) && (o.br.x > ul.x) && (o.br.y > ul.y));
    }

    public boolean contains(Area o) {
	return((o.ul.x >= ul.x) && (o.ul.y >= ul.y) && (o.br.x <= br.x) && (o.br.y <= br.y));
    }

    public Area overlap(Area o) {
	if(!isects(o))
	    return(null);
	return(new Area(new Coord(Math.max(ul.x, o.ul.x), Math.max(ul.y, o.ul.y)),
			new Coord(Math.min(br.x, o.br.x), Math.min(br.y, o.br.y))));
    }

    public Coord closest(Coord p) {
	if(contains(p))
	    return(p);
	return(new Coord(Utils.clip(p.x, ul.x, br.x),
			 Utils.clip(p.y, ul.y, br.y)));
    }

    public int area() {
	return((br.x - ul.x) * (br.y - ul.y));
    }

    public Area xl(Coord off) {
	return(new Area(ul.add(off), br.add(off)));
    }

    public Area margin(Coord m) {
	return(new Area(ul.sub(m), br.add(m)));
    }

    public Area margin(int m) {
	return(margin(new Coord(m, m)));
    }

    public Iterator<Coord> iterator() {
	return(new Iterator<Coord>() {
		int x = ul.x, y = ul.y;

		public boolean hasNext() {
		    return((y < br.y) && (x < br.x));
		}

		public Coord next() {
		    Coord ret = new Coord(x, y);
		    if(++x >= br.x) {
			x = ul.x;
			y++;
		    }
		    return(ret);
		}
	    });
    }

    public int ri(Coord c) {
	return((c.x - ul.x) + ((c.y - ul.y) * (br.x - ul.x)));
    }

    public int rsz() {
	return((br.x - ul.x) * (br.y - ul.y));
    }

    public String toString() {
	return(String.format("((%d, %d) - (%d, %d))", ul.x, ul.y, br.x, br.y));
    }
}
