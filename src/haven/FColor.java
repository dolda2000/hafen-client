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

import java.awt.Color;

public class FColor {
    public static final FColor BLACK   = new FColor(0, 0, 0);
    public static final FColor WHITE   = new FColor(1, 1, 1);
    public static final FColor RED     = new FColor(1, 0, 0);
    public static final FColor GREEN   = new FColor(0, 1, 0);
    public static final FColor BLUE    = new FColor(0, 0, 1);
    public static final FColor YELLOW  = new FColor(1, 1, 0);
    public static final FColor MAGENTA = new FColor(1, 0, 1);
    public static final FColor CYAN    = new FColor(0, 1, 1);
    public final float r, g, b, a;

    public FColor(float r, float g, float b, float a) {
	this.r = r;
	this.g = g;
	this.b = b;
	this.a = a;
    }

    public FColor(float r, float g, float b) {
	this(r, g, b, 1);
    }

    public FColor(Color c, float f) {
	this(f * c.getRed()   / 255.0f,
	     f * c.getGreen() / 255.0f,
	     f * c.getBlue()  / 255.0f,
	     c.getAlpha() / 255.0f);
    }

    public FColor(Color c) {
	this(c, 1);
    }

    public float[] to3a() {
	return(new float[] {r, g, b});
    }

    public float[] to4a() {
	return(new float[] {r, g, b, a});
    }

    public FColor mul(FColor that) {
	return(new FColor(this.r * that.r, this.g * that.g, this.b * that.b, this.a * that.a));
    }

    public FColor blend(FColor that) {
	float B = that.a, A = 1.0f - B;
	return(new FColor((this.r * A) + (that.r * B),
			  (this.g * A) + (that.g * B),
			  (this.b * A) + (that.b * B),
			  this.a));
    }

    public FColor blend(FColor that, float B) {
	float A = 1.0f - B;
	return(new FColor((this.r * A) + (that.r * B),
			  (this.g * A) + (that.g * B),
			  (this.b * A) + (that.b * B),
			  (this.a * A) + (that.a * B)));
    }

    public int hashCode() {
	return(((((((Float.floatToIntBits(r)) * 31) +
		   Float.floatToIntBits(g)) * 31) +
		 Float.floatToIntBits(b)) * 31) +
	       Float.floatToIntBits(a));
    }

    public boolean equals(Object o) {
	if(!(o instanceof FColor))
	    return(false);
	FColor that = (FColor)o;
	return((this.r == that.r) && (this.g == that.g) && (this.b == that.b) && (this.a == that.a));
    }

    public String toString() {
	return(String.format("color(%f, %f, %f, %f)", r, g, b, a));
    }
}
