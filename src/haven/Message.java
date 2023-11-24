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
import dolda.coe.*;
import java.awt.Color;

public abstract class Message {
    public static final int T_END = 0;
    public static final int T_INT = 1;
    public static final int T_STR = 2;
    public static final int T_COORD = 3;
    public static final int T_UINT8 = 4;
    public static final int T_UINT16 = 5;
    public static final int T_COLOR = 6;
    public static final int T_FCOLOR = 7;
    public static final int T_TTOL = 8;
    public static final int T_INT8 = 9;
    public static final int T_INT16 = 10;
    public static final int T_NIL = 12;
    public static final int T_UID = 13;
    public static final int T_BYTES = 14;
    public static final int T_FLOAT32 = 15;
    public static final int T_FLOAT64 = 16;
    public static final int T_FCOORD32 = 18;
    public static final int T_FCOORD64 = 19;
    public static final int T_FLOAT8 = 21;
    public static final int T_FLOAT16 = 22;
    public static final int T_SNORM8 = 23;
    public static final int T_UNORM8 = 24;
    public static final int T_MNORM8 = 25;
    public static final int T_SNORM16 = 26;
    public static final int T_UNORM16 = 27;
    public static final int T_MNORM16 = 28;
    public static final int T_SNORM32 = 29;
    public static final int T_UNORM32 = 30;
    public static final int T_MNORM32 = 31;

    private final static byte[] empty = new byte[0];
    public int rh = 0, rt = 0, wh = 0, wt = 0;
    public byte[] rbuf = empty, wbuf = empty;

    public static final Message nil = new Message() {
	    public boolean underflow(int hint) {return(false);}
	    public void overflow(int min) {throw(new RuntimeException("nil message is not writable"));}
	    public String toString() {return("Message(nil)");}
	};

    public static class BinError extends RuntimeException {
	public Message msg;

	public BinError(String message) {
	    super(message);
	}
	public BinError(String message, Throwable cause) {
	    super(message, cause);
	}
	public BinError(Throwable cause) {
	    super(cause);
	}

	public BinError msg(Message msg) {
	    if(msg instanceof java.io.Serializable)
		this.msg = msg;
	    return(this);
	}
    }
    public static class EOF extends BinError {
	public EOF(String message) {
	    super(message);
	}
    }
    public static class FormatError extends BinError {
	public FormatError(String message) {
	    super(message);
	}
	public FormatError(String message, Throwable cause) {
	    super(message, cause);
	}
    }

    public abstract boolean underflow(int hint);

    private void rensure(int len) {
	while(len > rt - rh) {
	    if(!underflow(rh + len - rt))
		throw(new EOF("Required " + len + " bytes, got only " + (rt - rh)).msg(this));
	}
    }
    private int rget(int len) {
	rensure(len);
	int co = rh;
	rh += len;
	return(co);
    }

    public boolean eom() {
	return(!((rh < rt) || underflow(1)));
    }

    public int int8() {
	rensure(1);
	return(rbuf[rh++]);
    }
    public int uint8() {
	return(int8() & 0xff);
    }
    public int int16() {
	int off = rget(2);
	return(Utils.int16d(rbuf, off));
    }
    public int uint16() {
	int off = rget(2);
	return(Utils.uint16d(rbuf, off));
    }
    public int int32() {
	int off = rget(4);
	return(Utils.int32d(rbuf, off));
    }
    public long uint32() {
	int off = rget(4);
	return(Utils.uint32d(rbuf, off));
    }
    public long int64() {
	int off = rget(8);
	return(Utils.int64d(rbuf, off));
    }
    public String string() {
	int l = 0;
	while(true) {
	    if(l >= rt - rh) {
		if(!underflow(256))
		    throw(new EOF("Found no NUL (at length " + l + ")").msg(this));
	    }
	    if(rbuf[l + rh] == 0) {
		String ret = new String(rbuf, rh, l, Utils.utf8);
		rh += l + 1;
		return(ret);
	    }
	    l++;
	}
    }
    public void skip(int n) {
	while(n > 0) {
	    if(rh >= rt) {
		if(!underflow(Math.min(n, 1024)))
		    throw(new EOF("Out of bytes to skip").msg(this));
	    }
	    int s = Math.min(n, rt - rh);
	    rh += s;
	    n -= s;
	}
    }
    public void skip() {
	do rh = rt; while(underflow(1024));
    }
    public byte[] bytes(int n) {
	byte[] ret = new byte[n];
	rensure(n);
	System.arraycopy(rbuf, rh, ret, 0, n);
	rh += n;
	return(ret);
    }
    public byte[] bytes() {
	while(underflow(65536));
	return(bytes(rt - rh));
    }
    public void bytes(byte[] b, int off, int len) {
	int olen = len;
	while(len > 0) {
	    if(rh >= rt) {
		if(!underflow(Math.min(len, 1024)))
		    throw(new EOF("Required " + olen + " bytes, got only " + (olen - len)).msg(this));
	    }
	    int r = Math.min(len, rt - rh);
	    System.arraycopy(rbuf, rh, b, off, r);
	    rh += r;
	    off += r;
	    len -= r;
	}
    }
    public void bytes(byte[] b) {bytes(b, 0, b.length);}
    public Coord coord() {
	return(new Coord(int32(), int32()));
    }
    public Color color() {
	return(new Color(uint8(), uint8(), uint8(), uint8()));
    }
    public FColor fcolor() {
	return(new FColor(float32(), float32(), float32(), float32()));
    }
    public float float8() {
	return(Utils.mfdec((byte)int8()));
    }
    public float float16() {
	return(Utils.hfdec((short)int16()));
    }
    public float float32() {
	int off = rget(4);
	return(Utils.float32d(rbuf, off));
    }
    public double float64() {
	int off = rget(8);
	return(Utils.float64d(rbuf, off));
    }
    public double cpfloat() {
	int off = rget(5);
	return(Utils.floatd(rbuf, off));
    }

    public float snorm8() {
	return(Utils.clip(int8(), -0x7f, 0x7f) / 0x7fp0f);
    }
    public float unorm8() {
	return(uint8() / 0xffp0f);
    }
    public float mnorm8() {
	return(uint8() / 0x100p0f);
    }
    public float snorm16() {
	return(Utils.clip(int16(), -0x7fff, 0x7fff) / 0x7fffp0f);
    }
    public float unorm16() {
	return(uint16() / 0xffffp0f);
    }
    public float mnorm16() {
	return(uint16() / 0x10000p0f);
    }
    public double snorm32() {
	return(Utils.clip(int32(), -0x7fffffff, 0x7fffffff) / 0x7fffffffp0);
    }
    public double unorm32() {
	return(uint32() / 0xffffffffp0);
    }
    public double mnorm32() {
	return(uint32() / 0x100000000p0);
    }

    public Object tto(int type) {
	switch(type) {
	case T_INT:
	    return(int32());
	case T_STR:
	    return(string());
	case T_COORD:
	    return(coord());
	case T_UINT8:
	    return(uint8());
	case T_UINT16:
	    return(uint16());
	case T_INT8:
	    return(int8());
	case T_INT16:
	    return(int16());
	case T_COLOR:
	    return(color());
	case T_FCOLOR:
	    return(fcolor());
	case T_TTOL:
	    return(list());
	case T_NIL:
	    return(null);
	case T_UID:
	    return(UID.of(int64()));
	case T_BYTES:
	    int len = uint8();
	    if((len & 128) != 0)
		len = int32();
	    return(bytes(len));
	case T_FLOAT8:
	    return(MiniFloat.decode((byte)int8()));
	case T_FLOAT16:
	    return(HalfFloat.decode((short)int16()));
	case T_FLOAT32:
	    return(float32());
	case T_FLOAT64:
	    return(float64());
	case T_FCOORD32:
	    return(new Coord2d(float32(), float32()));
	case T_FCOORD64:
	    return(new Coord2d(float64(), float64()));
	case T_SNORM8:  return( NormNumber.decsnorm8(this));
	case T_SNORM16: return(NormNumber.decsnorm16(this));
	case T_SNORM32: return(NormNumber.decsnorm32(this));
	case T_UNORM8:  return( NormNumber.decunorm8(this));
	case T_UNORM16: return(NormNumber.decunorm16(this));
	case T_UNORM32: return(NormNumber.decunorm32(this));
	case T_MNORM8:  return( NormNumber.decmnorm8(this));
	case T_MNORM16: return(NormNumber.decmnorm16(this));
	case T_MNORM32: return(NormNumber.decmnorm32(this));
	default:
	    throw(new FormatError("unknown type tag: " + type).msg(this));
	}
    }

    public Object tto() {
	return(tto(uint8()));
    }

    public Object[] list() {
	ArrayList<Object> ret = new ArrayList<Object>();
	while(true) {
	    if(eom())
		break;
	    int t = uint8();
	    if(t == T_END)
		break;
	    ret.add(tto(t));
	}
	return(ret.toArray());
    }

    public abstract void overflow(int min);

    private void wensure(int len) {
	if(len > wt - wh)
	    overflow(len);
    }
    private int wget(int len) {
	wensure(len);
	int co = wh;
	wh += len;
	return(co);
    }

    public Message addbytes(byte[] src, int off, int len) {
	wensure(len);
	System.arraycopy(src, off, wbuf, wh, len);
	wh += len;
	return(this);
    }
    public Message addbytes(byte[] src) {
	addbytes(src, 0, src.length);
	return(this);
    }
    public Message addint8(byte num) {
	wensure(1);
	wbuf[wh++] = num;
	return(this);
    }
    public Message adduint8(int num) {
	wensure(1);
	wbuf[wh++] = (byte)num;
	return(this);
    }
    public Message addint16(short num) {
	int off = wget(2);
	Utils.int16e(num, wbuf, off);
	return(this);
    }
    public Message adduint16(int num) {
	int off = wget(2);
	Utils.uint16e(num, wbuf, off);
	return(this);
    }
    public Message addint32(int num) {
	int off = wget(4);
	Utils.int32e(num, wbuf, off);
	return(this);
    }
    public Message adduint32(long num) {
	int off = wget(4);
	Utils.uint32e(num, wbuf, off);
	return(this);
    }
    public Message addint64(long num) {
	int off = wget(8);
	Utils.int64e(num, wbuf, off);
	return(this);
    }
    public Message addstring2(String str) {
	addbytes(str.getBytes(Utils.utf8));
	return(this);
    }
    public Message addstring(String str) {
	addstring2(str); adduint8(0);
	return(this);
    }
    public Message addcoord(Coord c) {
	addint32(c.x); addint32(c.y);
	return(this);
    }
    public Message addcolor(Color color) {
	adduint8(color.getRed()); adduint8(color.getGreen());
	adduint8(color.getBlue()); adduint8(color.getAlpha());
	return(this);
    }
    public Message addfloat8(float num) {
	return(addint8(Utils.mfenc(num)));
    }
    public Message addfloat16(float num) {
	return(addint16(Utils.hfenc(num)));
    }
    public Message addfloat32(float num) {
	int off = wget(4);
	Utils.float32e(num, wbuf, off);
	return(this);
    }
    public Message addfloat64(double num) {
	int off = wget(8);
	Utils.float64e(num, wbuf, off);
	return(this);
    }
    public Message addfcolor(FColor color) {
	addfloat32(color.r); addfloat32(color.g);
	addfloat32(color.b); addfloat32(color.a);
	return(this);
    }

    public Message addlist(Object... args) {
	for(Object o : args) {
	    if(o == null) {
		adduint8(T_NIL);
	    } else if(o instanceof Integer) {
		adduint8(T_INT);
		addint32(((Integer)o).intValue());
	    } else if(o instanceof String) {
		adduint8(T_STR);
		addstring((String)o);
	    } else if(o instanceof Coord) {
		adduint8(T_COORD);
		addcoord((Coord)o);
	    } else if(o instanceof byte[]) {
		byte[] b = (byte[])o;
		adduint8(T_BYTES);
		if(b.length < 128) {
		    adduint8(b.length);
		} else {
		    adduint8(0x80);
		    addint32(b.length);
		}
		addbytes(b);
	    } else if(o instanceof Color) {
		adduint8(T_COLOR);
		addcolor((Color)o);
	    } else if(o instanceof FColor) {
		adduint8(T_FCOLOR);
		addfcolor((FColor)o);
	    } else if(o instanceof MiniFloat) {
		adduint8(T_FLOAT8);
		addint8(((MiniFloat)o).bits);
	    } else if(o instanceof HalfFloat) {
		adduint8(T_FLOAT16);
		addint16(((HalfFloat)o).bits);
	    } else if(o instanceof Float) {
		adduint8(T_FLOAT32);
		addfloat32(((Float)o).floatValue());
	    } else if(o instanceof Double) {
		adduint8(T_FLOAT64);
		addfloat64(((Double)o).doubleValue());
	    } else if(o instanceof UID) {
		adduint8(T_UID);
		addint64(((UID)o).longValue());
	    } else if(o instanceof NormNumber.SNorm8) {
		adduint8(T_SNORM8);
		addint8(((NormNumber.SNorm8)o).val);
	    } else if(o instanceof NormNumber.UNorm8) {
		adduint8(T_UNORM8);
		adduint8(((NormNumber.UNorm8)o).val & 0xff);
	    } else if(o instanceof NormNumber.MNorm8) {
		adduint8(T_MNORM8);
		adduint8(((NormNumber.MNorm8)o).val & 0xff);
	    } else if(o instanceof NormNumber.SNorm16) {
		adduint8(T_SNORM16);
		addint16(((NormNumber.SNorm16)o).val);
	    } else if(o instanceof NormNumber.UNorm16) {
		adduint8(T_UNORM16);
		adduint16(((NormNumber.UNorm16)o).val & 0xffff);
	    } else if(o instanceof NormNumber.MNorm16) {
		adduint8(T_MNORM16);
		adduint16(((NormNumber.MNorm16)o).val & 0xffff);
	    } else if(o instanceof NormNumber.SNorm32) {
		adduint8(T_SNORM32);
		addint32(((NormNumber.SNorm32)o).val);
	    } else if(o instanceof NormNumber.UNorm32) {
		adduint8(T_UNORM32);
		addint32(((NormNumber.UNorm32)o).val);
	    } else if(o instanceof NormNumber.MNorm32) {
		adduint8(T_MNORM32);
		adduint32(((NormNumber.MNorm32)o).val);
	    } else if(o instanceof Coord2d) {
		adduint8(T_FCOORD64);
		addfloat64(((Coord2d)o).x);
		addfloat64(((Coord2d)o).y);
	    } else if(o instanceof Object[]) {
		adduint8(T_TTOL);
		addlist((Object[])o);
		adduint8(T_END);
	    } else {
		throw(new RuntimeException("Cannot encode a " + o.getClass() + " as TTO"));
	    }
	}
	return(this);
    }

    static {
	ObjectData.register(Message.class, (msg, buf) -> {
		if((msg.rbuf.length > 0) || (msg.rh != 0) || (msg.rt != 0)) {
		    buf.put(Symbol.get("read-buf"), msg.rbuf);
		    buf.put(Symbol.get("read-head"), msg.rh);
		    buf.put(Symbol.get("read-tail"), msg.rt);
		}
		if((msg.wbuf.length > 0) || (msg.wh != 0) || (msg.wt != 0)) {
		    buf.put(Symbol.get("write-buf"), msg.wbuf);
		    buf.put(Symbol.get("write-head"), msg.wh);
		    buf.put(Symbol.get("write-tail"), msg.wt);
		}
	    });
    }
}
