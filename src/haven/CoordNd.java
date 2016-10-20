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

import java.util.function.*;
import static java.lang.Math.*;

public class CoordNd implements java.io.Serializable {
    public final double[] el;
    public static final CoordNd o = new CoordNd(0);

    public CoordNd(int n) {
	this.el = new double[n];
    }

    public CoordNd(double... el) {
	this.el = el;
    }

    public double el(int i) {
	return((i < el.length)?el[i]:0.0);
    }

    public boolean equals(Object o) {
	if(!(o instanceof CoordNd))
	    return(false);
	CoordNd c = (CoordNd)o;
	for(int i = 0; i < min(el.length, c.el.length); i++) {
	    if(c.el[i] != el[i])
		return(false);
	}
	if(el.length < c.el.length) {
	    for(int i = el.length; i < c.el.length; i++) {
		if(c.el[i] != 0.0)
		    return(false);
	    }
	} else {
	    for(int i = c.el.length; i < el.length; i++) {
		if(el[i] != 0.0)
		    return(false);
	    }
	}
	return(true);
    }

    public int hashCode() {
	int e, ret = 0;
	for(e = el.length - 1; (e >= 0) && (el[e] == 0.0); e--);
	for(int i = 0; i <= e; i++) {
	    long E = Double.doubleToLongBits(el[i]);
	    ret = (ret * 31) + (int)(E ^ (E >> 32));
	}
	return(ret);
    }

    private CoordNd op(DoubleUnaryOperator op) {
	double[] n = new double[el.length];
	for(int i = 0; i < el.length; i++)
	    n[i] = op.applyAsDouble(el[i]);
	return(new CoordNd(n));
    }

    private CoordNd op(DoubleBinaryOperator op, double[] b) {
	double[] n = new double[max(el.length, b.length)];
	for(int i = 0; i < min(el.length, b.length); i++)
	    n[i] = op.applyAsDouble(el[i], b[i]);
	if(el.length < b.length) {
	    for(int i = el.length; i < b.length; i++)
		n[i] = op.applyAsDouble(el[i], 0.0);
	} else {
	    for(int i = b.length; i < el.length; i++)
		n[i] = op.applyAsDouble(b[i], 0.0);
	}
	return(new CoordNd(n));
    }

    public CoordNd add(CoordNd b) {
	return(op((x, y) -> x + y, b.el));
    }

    public CoordNd inv() {
	return(op(x -> -x));
    }

    public CoordNd sub(CoordNd b) {
	return(op((x, y) -> x - y, b.el));
    }

    public CoordNd mul(double f) {
	return(op(x -> x * f));
    }

    public CoordNd mul(CoordNd b) {
	return(op((x, y) -> x * y, b.el));
    }

    public CoordNd div(double f) {
	return(op(x -> x / f));
    }

    public CoordNd div(CoordNd b) {
	return(op((x, y) -> x / y, b.el));
    }

    public CoordNd mod(double b) {
	return(op(x -> Utils.floormod(x, b)));
    }

    public CoordNd mod(CoordNd b) {
	return(op(Utils::floormod, b.el));
    }

    public CoordNd round() {
	return(op(x -> Math.round(x)));
    }

    public CoordNd floor() {
	return(op(x -> Math.floor(x)));
    }

    public CoordNd ceil() {
	return(op(x -> Math.ceil(x)));
    }

    public double dmul(CoordNd b) {
	double ret = 0;
	for(int i = 0; i < min(el.length, b.el.length); i++)
	    ret += el[i] * b.el[i];
	return(ret);
    }

    public double abs() {
	double a = 0.0;
	for(int i = 0; i < el.length; i++)
	    a += el[i] * el[i];
	return(sqrt(a));
    }

    public CoordNd norm(double l) {
	return(mul(l / abs()));
    }

    public CoordNd norm() {
	return(norm(1.0));
    }

    public double dist(CoordNd o) {
	return(sub(o).abs());
    }

    public String toString() {
	StringBuilder buf = new StringBuilder();
	buf.append('(');
	for(int i = 0; i < el.length; i++) {
	    if(i > 0)
		buf.append(", ");
	    buf.append(el[i]);
	}
	buf.append(')');
	return(buf.toString());
    }
}
