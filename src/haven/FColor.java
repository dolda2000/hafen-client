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
    public float r, g, b, a;

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

    public FColor blend(FColor o, float f) {
	float F = 1.0f - f;
	return(new FColor((r * F) + (o.r * f),
			  (g * F) + (o.g * f),
			  (b * F) + (o.b * f),
			  (a * F) + (o.a * f)));
    }

    public float[] to3a() {
	return(new float[] {r, g, b});
    }

    public float[] to4a() {
	return(new float[] {r, g, b, a});
    }

    public String toString() {
	return(String.format("color(%f, %f, %f, %f)", r, g, b, a));
    }
}
