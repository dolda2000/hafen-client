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

public abstract class NormNumber extends Number {
    public long longValue() {
	return(intValue());
    }

    public float floatValue() {
	return((float)doubleValue());
    }

    private static void avoid(int x, int v) {
	if(x == v)
	    throw(new IllegalArgumentException(Integer.toString(v)));
    }

    public static class SNorm8 extends NormNumber {
	public final byte val;

	public SNorm8(byte val) {avoid(val & 0xff, 0x80); this.val = val;}

	public int intValue()       {return(val == 127 ? 1 : val == -127 ? -1 : 0);}
	public float floatValue()   {return(val * (1.0f / 127.0f));}
	public double doubleValue() {return(val * (1.0  / 127.0 ));}
    }

    public static class UNorm8 extends NormNumber {
	public final byte val;

	public UNorm8(byte val) {this.val = val;}

	public int intValue()       {return((val & 0xff) == 255 ? 1 : 0);}
	public float floatValue()   {return((val & 0xff) * (1.0f / 255.0f));}
	public double doubleValue() {return((val & 0xff) * (1.0  / 255.0 ));}
    }

    public static class MNorm8 extends NormNumber {
	public final byte val;

	public MNorm8(byte val) {this.val = val;}

	public int intValue()       {return(0);}
	public float floatValue()   {return((val & 0xff) * (1.0f / 256.0f));}
	public double doubleValue() {return((val & 0xff) * (1.0  / 256.0 ));}
    }

    public static class SNorm16 extends NormNumber {
	public final short val;

	public SNorm16(short val) {avoid(val & 0xffff, 0x8000); this.val = val;}

	public int intValue()       {return(val == 32767 ? 1 : val == -32767 ? -1 : 0);}
	public float floatValue()   {return(val * (1.0f / 32767.0f));}
	public double doubleValue() {return(val * (1.0  / 32767.0 ));}
    }

    public static class UNorm16 extends NormNumber {
	public final short val;

	public UNorm16(short val) {this.val = val;}

	public int intValue()       {return((val & 0xffff) == 65535 ? 1 : 0);}
	public float floatValue()   {return((val & 0xffff) * (1.0f / 65535.0f));}
	public double doubleValue() {return((val & 0xffff) * (1.0  / 65535.0 ));}
    }

    public static class MNorm16 extends NormNumber {
	public final short val;

	public MNorm16(short val) {this.val = val;}

	public int intValue()       {return(0);}
	public float floatValue()   {return((val & 0xffff) * (1.0f / 65536.0f));}
	public double doubleValue() {return((val & 0xffff) * (1.0  / 65536.0 ));}
    }

    public static class SNorm32 extends NormNumber {
	public final int val;

	public SNorm32(int val) {avoid(val, 0x80000000); this.val = val;}

	public int intValue()       {return(val == 0x7fffffff ? 1 : val == -0x7fffffff ? -1 : 0);}
	public double doubleValue() {return(val * (1.0  / 0x7fffffff.0p0));}
    }

    public static class UNorm32 extends NormNumber {
	public final int val;

	public UNorm32(int val) {this.val = val;}

	public int intValue()       {return(val == 0xffffffff ? 1 : 0);}
	public double doubleValue() {return((val & 0xffffffffl) * (1.0 / 0xffffffff.0p0));}
    }

    public static class MNorm32 extends NormNumber {
	public final int val;

	public MNorm32(int val) {this.val = val;}

	public int intValue()       {return(0);}
	public double doubleValue() {return((val & 0xffffffffl) * (1.0 / 0x100000000.0p0));}
    }

    public static SNorm8   decsnorm8(int val) {return(new SNorm8((byte)Utils.clip(val, -127, 127)));}
    public static UNorm8   decunorm8(int val) {return(new UNorm8((byte)Utils.clip(val, 0, 255)));}
    public static MNorm8   decmnorm8(int val) {return(new MNorm8((byte)val));}
    public static SNorm16 decsnorm16(int val) {return(new SNorm16((short)Utils.clip(val, -32767, 32767)));}
    public static UNorm16 decunorm16(int val) {return(new UNorm16((short)Utils.clip(val, 0, 65535)));}
    public static MNorm16 decmnorm16(int val) {return(new MNorm16((short)val));}
    public static SNorm32 decsnorm32(int val) {return(new SNorm32(val));}
    public static UNorm32 decunorm32(int val) {return(new UNorm32(val));}
    public static MNorm32 decmnorm32(int val) {return(new MNorm32(val));}

    public static SNorm8   decsnorm8(Message msg) {return( decsnorm8(msg.int8()));}
    public static UNorm8   decunorm8(Message msg) {return( decunorm8(msg.uint8()));}
    public static MNorm8   decmnorm8(Message msg) {return( decmnorm8(msg.uint8()));}
    public static SNorm16 decsnorm16(Message msg) {return(decsnorm16(msg.int16()));}
    public static UNorm16 decunorm16(Message msg) {return(decunorm16(msg.uint16()));}
    public static MNorm16 decmnorm16(Message msg) {return(decmnorm16(msg.uint16()));}
    public static SNorm32 decsnorm32(Message msg) {return(decsnorm32(msg.int32()));}
    public static UNorm32 decunorm32(Message msg) {return(decunorm32(msg.int32()));}
    public static MNorm32 decmnorm32(Message msg) {return(decmnorm32(msg.int32()));}

    public static SNorm8   snorm8(float val) {return( snorm8(Math.round(Utils.clip(val, -1, 1) * 0x7f.0p0f)));}
    public static SNorm16 snorm16(float val) {return(snorm16(Math.round(Utils.clip(val, -1, 1) * 0x7fff.0p0f)));}
    public static SNorm32 snorm32(float val) {return(snorm32(Math.round(Utils.clip(val, -1, 1) * 0x7fffff80.0p0f)));}
    public static UNorm8   unorm8(float val) {return( unorm8(Math.round(Utils.clip(val,  0, 1) * 0xff.0p0f)));}
    public static UNorm16 unorm16(float val) {return(unorm16(Math.round(Utils.clip(val,  0, 1) * 0xffff.0p0f)));}
    public static UNorm32 unorm32(float val) {return(unorm32(Math.round(Utils.clip(val,  0, 1) * 0xffffff00.0p0f)));}
    public static MNorm8   mnorm8(float val) {return( mnorm8(Math.round(Utils.floormod(val, 1.0f) * 0x100.0p0f)));}
    public static MNorm16 mnorm16(float val) {return(mnorm16(Math.round(Utils.floormod(val, 1.0f) * 0x10000.0p0f)));}
    public static MNorm32 mnorm32(float val) {return(mnorm32(Math.round(Utils.floormod(val, 1.0f) * 0x10000000.0p0f) << 4));}

    public static SNorm8   snorm8(double val) {return( snorm8((int)Math.round(Utils.clip(val, -1, 1) * 0x7f.0p0)));}
    public static SNorm16 snorm16(double val) {return(snorm16((int)Math.round(Utils.clip(val, -1, 1) * 0x7fff.0p0)));}
    public static SNorm32 snorm32(double val) {return(snorm32((int)Math.round(Utils.clip(val, -1, 1) * 0x7fffffff.0p0)));}
    public static UNorm8   unorm8(double val) {return( unorm8((int)Math.round(Utils.clip(val,  0, 1) * 0xff.0p0)));}
    public static UNorm16 unorm16(double val) {return(unorm16((int)Math.round(Utils.clip(val,  0, 1) * 0xffff.0p0)));}
    public static UNorm32 unorm32(double val) {return(unorm32((int)Math.round(Utils.clip(val,  0, 1) * 0xffffffff.0p0)));}
    public static MNorm8   mnorm8(double val) {return( mnorm8((int)Math.round(Utils.floormod(val, 1.0) * 0x100.0p0)));}
    public static MNorm16 mnorm16(double val) {return(mnorm16((int)Math.round(Utils.floormod(val, 1.0) * 0x10000.0p0)));}
    public static MNorm32 mnorm32(double val) {return(mnorm32((int)Math.round(Utils.floormod(val, 1.0) * 0x100000000.0p0)));}

    public String toString() {
	return(Double.toString(doubleValue()));
    }
}
