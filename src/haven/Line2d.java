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
}
