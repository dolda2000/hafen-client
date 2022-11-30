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

package haven.render;

import haven.*;
import java.util.function.*;

public abstract class Transform extends State {
    private static final Pair<Matrix4f, Matrix4f> NONE = new Pair<>(null, null);
    private final Function<Matrix4f, Matrix4f> xf;
    private Pair<Matrix4f, Matrix4f> last = NONE;

    public Transform(Function<Matrix4f, Matrix4f> xf) {
	this.xf = xf;
    }

    public Transform(Matrix4f xf) {
	this(new ByMatrix(xf));
    }

    public haven.render.sl.ShaderMacro shader() {return(null);}

    public Matrix4f fin(Matrix4f p) {
	Pair<Matrix4f, Matrix4f> last = this.last;
	if(p != last.a)
	    this.last = last = new Pair<>(p, xf.apply(p));
	return(last.b);
    }

    public boolean equals(Object o) {
	return((o instanceof Transform) && Utils.eq(((Transform)o).xf, this.xf));
    }

    public int hashCode() {
	return(xf.hashCode());
    }

    public static class ByMatrix implements Function<Matrix4f, Matrix4f> {
	public final Matrix4f xf;

	public ByMatrix(Matrix4f xf) {
	    this.xf = xf;
	}

	public Matrix4f apply(Matrix4f p) {
	    if(p == Matrix4f.id)
		return(xf);
	    return(p.mul(xf));
	}

	public boolean equals(Object o) {
	    return((o instanceof ByMatrix) && Utils.eq(((ByMatrix)o).xf, this.xf));
	}

	public int hashCode() {
	    return(xf.hashCode());
	}

	public String toString() {
	    return(xf.toString());
	}
    }

    public static final Function<Matrix4f, Matrix4f> nullrot = new Function<Matrix4f, Matrix4f>() {
	public Matrix4f apply(Matrix4f p) {
	    if(p == Matrix4f.id)
		return(p);
	    Matrix4f r = new Matrix4f(p);
	    /* XXX: This is only actually correct for input matrices
	     * that are only rotating and translating. Tthat's
	     * probably not a problem for now, but please consider a
	     * fix. */
	    r.m[0] = r.m[5] = r.m[10] = 1.0f;
	    r.m[1] = r.m[2] = r.m[4] =
	    r.m[6] = r.m[8] = r.m[9] = 0.0f;
	    return(r);
	}

	public String toString() {
	    return("#nullrot");
	}
    };

    public static Matrix4f makexlate(Matrix4f d, Coord3f c) {
	d.m[ 0] = d.m[ 5] = d.m[10] = d.m[15] = 1.0f;
	d.m[ 1] = d.m[ 2] = d.m[ 3] =
	d.m[ 4] = d.m[ 6] = d.m[ 7] =
	d.m[ 8] = d.m[ 9] = d.m[11] = 0.0f;
	d.m[12] = c.x;
	d.m[13] = c.y;
	d.m[14] = c.z;
	return(d);
    }

    public static Matrix4f makerot(Matrix4f d, Coord3f axis, float s, float c) {
	float C = 1.0f - c;
	float x = axis.x, y = axis.y, z = axis.z;
	d.m[ 3] = d.m[ 7] = d.m[11] = d.m[12] = d.m[13] = d.m[14] = 0.0f;
	d.m[15] = 1.0f;
	d.m[ 0] = (x * x * C) + c;       d.m[ 4] = (y * x * C) - (z * s); d.m[ 8] = (z * x * C) + (y * s);
	d.m[ 1] = (x * y * C) + (z * s); d.m[ 5] = (y * y * C) + c;       d.m[ 9] = (z * y * C) - (x * s);
	d.m[ 2] = (x * z * C) - (y * s); d.m[ 6] = (y * z * C) + (x * s); d.m[10] = (z * z * C) + c;
	return(d);
    }

    public static Matrix4f makerot(Matrix4f d, Coord3f axis, float angle) {
	return(makerot(d, axis, (float)Math.sin(angle), (float)Math.cos(angle)));
    }

    public static Matrix4f makescale(Matrix4f d, float x, float y, float z) {
	d.m[0] = x; d.m[5] = y; d.m[10] = z; d.m[15] = 1.0f;
	d.m[ 1] = d.m[ 2] = d.m[ 3] =
	d.m[ 4] = d.m[ 6] = d.m[ 7] =
	d.m[ 8] = d.m[ 9] = d.m[11] =
	d.m[12] = d.m[13] = d.m[14] = 0.0f;
	return(d);
    }

    public static Matrix4f makescale(Matrix4f d, float s) {
	return(makescale(d, s, s, s));
    }

    public static Matrix4f rxinvert(Matrix4f m) {
	/* This assumes that m is merely a composition of rotations
	 * and translations. */
	return(m.trim3(1).transpose().mul1(makexlate(new Matrix4f(), new Coord3f(-m.m[12], -m.m[13], -m.m[14]))));
    }

    public String toString() {
	return(this.getClass().getName() + "(" + xf + ")");
    }
}
