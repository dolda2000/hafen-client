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

public class HalfFloat extends Number {
    public final short bits;

    public HalfFloat(short bits) {
	this.bits = bits;
    }

    public static HalfFloat decode(short bits) {return(new HalfFloat(bits));}
    public static HalfFloat of(float val) {return(new HalfFloat(bits(val)));}
    public static HalfFloat of(double val) {return(of((float)val));}

    public boolean equals(HalfFloat that) {
	return(this.bits == that.bits);
    }
    public boolean equals(Object that) {
	return((that instanceof HalfFloat) && equals((HalfFloat)that));
    }
    public int hashCode() {
	return(bits);
    }

    public float floatValue() {
	return(bits(bits));
    }

    public byte   byteValue()   {return((byte)floatValue());}
    public short  shortValue()  {return((short)floatValue());}
    public int    intValue()    {return((int)floatValue());}
    public long   longValue()   {return((long)floatValue());}
    public double doubleValue() {return((double)floatValue());}

    public String toString() {
	return(Float.toString(floatValue()));
    }

    public static float bits(short bits) {
	int b = ((int)bits) & 0xffff;
	int e = (b & 0x7c00) >> 10;
	int m = b & 0x03ff;
	int ee;
	if(e == 0) {
	    if(m == 0) {
		ee = 0;
	    } else {
		int n = Integer.numberOfLeadingZeros(m) - 22;
		ee = (-15 - n) + 127;
		m = (m << (n + 1)) & 0x03ff;
	    }
	} else if(e == 0x1f) {
	    ee = 0xff;
	} else {
	    ee = e - 15 + 127;
	}
	int f32 = ((b & 0x8000) << 16) |
	    (ee << 23) |
	    (m << 13);
	return(Float.intBitsToFloat(f32));
    }

    public static short bits(float f) {
	int b = Float.floatToIntBits(f);
	int e = (b & 0x7f800000) >> 23;
	int m = b & 0x007fffff;
	int ee;
	if(e == 0) {
	    ee = 0;
	    m = 0;
	} else if(e == 0xff) {
	    ee = 0x1f;
	} else if(e < 127 - 14) {
	    ee = 0;
	    m = (m | 0x00800000) >> ((127 - 14) - e);
	} else if(e > 127 + 15) {
	    return(((b & 0x80000000) == 0) ? ((short)0x7c00) : ((short)0xfc00));
	} else {
	    ee = e - 127 + 15;
	}
	int f16 = ((b >> 16) & 0x8000) |
	    (ee << 10) |
	    (m >> 13);
	return((short)f16);
    }
}
