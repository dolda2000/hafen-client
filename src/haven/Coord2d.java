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
	return(new Coord2d(x + X, y + Y));
    }

    public Coord2d add(Coord2d b) {
	return(add(b.x, b.y));
    }

    public Coord2d inv() {
	return(new Coord2d(-x, -y));
    }

    public Coord2d sub(double X, double Y) {
	return(new Coord2d(x - X, y - Y));
    }

    public Coord2d sub(Coord2d b) {
	return(sub(b.x, b.y));
    }

    public Coord2d mul(double f) {
	return(new Coord2d(x * f, y * f));
    }

    public Coord2d mul(double X, double Y) {
	return(new Coord2d(x * X, y * Y));
    }

    public Coord2d mul(Coord2d b) {
	return(mul(b.x, b.y));
    }

    public Coord2d div(double f) {
	return(new Coord2d(x / f, y / f));
    }

    public Coord2d div(double X, double Y) {
	return(new Coord2d(x / X, y / Y));
    }

    public Coord2d div(Coord2d b) {
	return(div(b.x, b.y));
    }

    public Coord round() {
	return(new Coord((int)Math.round(x), (int)Math.round(y)));
    }

    public Coord floor() {
	return(new Coord((int)Math.floor(x), (int)Math.floor(y)));
    }

    public Coord floor(double X, double Y) {
	return(new Coord((int)Math.floor(x / X), (int)Math.floor(y / Y)));
    }

    public Coord floor(Coord2d f) {
	return(floor(f.x, f.y));
    }

    public Coord2d mod() {
	return(new Coord2d(x - Math.floor(x), y - Math.floor(y)));
    }

    public Coord2d mod(double X, double Y) {
	return(new Coord2d(x - (Math.floor(x / X) * X), y - (Math.floor(y / Y) * Y)));
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

    public static Coord2d sc(double a, double r) {
	return(new Coord2d(Math.cos(a) * r, Math.sin(a) * r));
    }

    public String toString() {
	return("(" + x + ", " + y + ")");
    }
}
