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

public class MiniFloat extends Number {
    public final byte bits;

    public MiniFloat(byte bits) {
	this.bits = bits;
    }

    public static MiniFloat decode(byte bits) {return(new MiniFloat(bits));}
    public static MiniFloat of(float val) {return(new MiniFloat(bits(val)));}
    public static MiniFloat of(double val) {return(of((float)val));}

    public boolean equals(MiniFloat that) {
	return(this.bits == that.bits);
    }
    public boolean equals(Object that) {
	return((that instanceof MiniFloat) && equals((MiniFloat)that));
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

    public static float bits(byte bits) {
	int b = ((int)bits) & 0xff;
	int e = (b & 0x78) >> 3;
	int m = b & 0x07;
	int ee;
	if(e == 0) {
	    if(m == 0) {
		ee = 0;
	    } else {
		int n = Integer.numberOfLeadingZeros(m) - 29;
		ee = (-7 - n) + 127;
		m = (m << (n + 1)) & 0x07;
	    }
	} else if(e == 0x0f) {
	    ee = 0xff;
	} else {
	    ee = e - 7 + 127;
	}
	int f32 = ((b & 0x80) << 24) |
	    (ee << 23) |
	    (m << 20);
	return(Float.intBitsToFloat(f32));
    }

    public static byte bits(float f) {
	int b = Float.floatToIntBits(f);
	int e = (b & 0x7f800000) >> 23;
	int m = b & 0x007fffff;
	int ee;
	if(e == 0) {
	    ee = 0;
	    m = 0;
	} else if(e == 0xff) {
	    ee = 0x0f;
	} else if(e < 127 - 6) {
	    ee = 0;
	    m = (m | 0x00800000) >> ((127 - 6) - e);
	} else if(e > 127 + 7) {
	    return(((b & 0x80000000) == 0) ? ((byte)0x78) : ((byte)0xf8));
	} else {
	    ee = e - 127 + 7;
	}
	int f8 = ((b >> 24) & 0x80) |
	    (ee << 3) |
	    (m >> 20);
	return((byte)f8);
    }
}
