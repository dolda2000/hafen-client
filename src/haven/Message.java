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
import java.awt.Color;
import java.util.zip.Inflater;

public class Message implements java.io.Serializable {
    public static final int RMSG_NEWWDG = 0;
    public static final int RMSG_WDGMSG = 1;
    public static final int RMSG_DSTWDG = 2;
    public static final int RMSG_MAPIV = 3;
    public static final int RMSG_GLOBLOB = 4;
    public static final int RMSG_PAGINAE = 5;
    public static final int RMSG_RESID = 6;
    public static final int RMSG_PARTY = 7;
    public static final int RMSG_SFX = 8;
    public static final int RMSG_CATTR = 9;
    public static final int RMSG_MUSIC = 10;
    public static final int RMSG_TILES = 11;
    public static final int RMSG_BUFF = 12;
    public static final int RMSG_SESSKEY = 13;
	
    public static final int T_END = 0;
    public static final int T_INT = 1;
    public static final int T_STR = 2;
    public static final int T_COORD = 3;
    public static final int T_UINT8 = 4;
    public static final int T_UINT16 = 5;
    public static final int T_COLOR = 6;
    public static final int T_TTOL = 8;
    public static final int T_INT8 = 9;
    public static final int T_INT16 = 10;
    public static final int T_NIL = 12;
    public static final int T_BYTES = 14;
    public static final int T_FLOAT32 = 15;
    public static final int T_FLOAT64 = 16;
	
    public static final Message nil = new Message(0);

    public int type;
    public byte[] blob;
    public long last = 0;
    public int retx = 0;
    public int seq;
    public int off = 0;
	
    public Message(int type, byte[] blob) {
	this.type = type;
	this.blob = blob;
    }
	
    public Message(int type, byte[] blob, int offset, int len) {
	this.type = type;
	this.blob = new byte[len];
	System.arraycopy(blob, offset, this.blob, 0, len);
    }
	
    public Message(int type) {
	this.type = type;
	blob = new byte[0];
    }
	
    public boolean equals(Object o2) {
	if(!(o2 instanceof Message))
	    return(false);
	Message m2 = (Message)o2;
	if(m2.blob.length != blob.length)
	    return(false);
	for(int i = 0; i < blob.length; i++) {
	    if(m2.blob[i] != blob[i])
		return(false);
	}
	return(true);
    }

    public Message clone() {
	return(new Message(type, blob));
    }
	
    public Message derive(int type, int len) {
	int ooff = off;
	off += len;
	return(new Message(type, blob, ooff, len));
    }
	
    public void addbytes(byte[] src, int off, int len) {
	byte[] n = new byte[blob.length + len];
	System.arraycopy(blob, 0, n, 0, blob.length);
	System.arraycopy(src, off, n, blob.length, len);
	blob = n;
    }

    public void addbytes(byte[] src) {
	addbytes(src, 0, src.length);
    }
	
    public void adduint8(int num) {
	addbytes(new byte[] {Utils.sb(num)});
    }
	
    public void adduint16(int num) {
	byte[] buf = new byte[2];
	Utils.uint16e(num, buf, 0);
	addbytes(buf);
    }
	
    public void addint32(int num) {
	byte[] buf = new byte[4];
	Utils.int32e(num, buf, 0);
	addbytes(buf);
    }
    
    public void adduint32(long num) {
	byte[] buf = new byte[4];
	Utils.uint32e(num, buf, 0);
	addbytes(buf);
    }
    
    public void addstring2(String str) {
	byte[] buf;
	try {
	    buf = str.getBytes("utf-8");
	} catch(java.io.UnsupportedEncodingException e) {
	    throw(new RuntimeException(e));
	}
	addbytes(buf);
    }
	
    public void addstring(String str) {
	addstring2(str);
	addbytes(new byte[] {0});
    }
	
    public void addcoord(Coord c) {
	addint32(c.x);
	addint32(c.y);
    }
	
    public void addlist(Object... args) {
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
	    } else {
		throw(new RuntimeException("Cannot encode a " + o.getClass() + " as TTO"));
	    }
	}
    }
	
    public boolean eom() {
	return(off >= blob.length);
    }
	
    public int int8() {
	return(blob[off++]);
    }

    public int uint8() {
	return(Utils.ub(blob[off++]));
    }
	
    public int int16() {
	off += 2;
	return(Utils.int16d(blob, off - 2));
    }
	
    public int uint16() {
	off += 2;
	return(Utils.uint16d(blob, off - 2));
    }
	
    public int int32() {
	off += 4;
	return(Utils.int32d(blob, off - 4));
    }
    
    public long uint32() {
	off += 4;
	return(Utils.uint32d(blob, off - 4));
    }

    public long int64() {
	off += 8;
	return(Utils.int64d(blob, off - 8));
    }
	
    public String string() {
	int[] ob = new int[] {off};
	String ret = Utils.strd(blob, ob);
	off = ob[0];
	return(ret);
    }
    
    public byte[] bytes(int n) {
	byte[] ret = new byte[n];
	System.arraycopy(blob, off, ret, 0, n);
	off += n;
	return(ret);
    }
    
    public byte[] bytes() {
	return(bytes(blob.length - off));
    }
	
    public Coord coord() {
	return(new Coord(int32(), int32()));
    }
        
    public Color color() {
	return(new Color(uint8(), uint8(), uint8(), uint8()));
    }

    public float float32() {
	off += 4;
	return(Utils.float32d(blob, off - 4));
    }

    public double float64() {
	off += 8;
	return(Utils.float64d(blob, off - 8));
    }
	
    public Object[] list() {
	ArrayList<Object> ret = new ArrayList<Object>();
	list: while(true) {
	    if(off >= blob.length)
		break;
	    int t = uint8();
	    switch(t) {
	    case T_END:
		break list;
	    case T_INT:
		ret.add(int32());
		break;
	    case T_STR:
		ret.add(string());
		break;
	    case T_COORD:
		ret.add(coord());
		break;
	    case T_UINT8:
		ret.add(uint8());
		break;
	    case T_UINT16:
		ret.add(uint16());
		break;
	    case T_INT8:
		ret.add(int8());
		break;
	    case T_INT16:
		ret.add(int16());
		break;
	    case T_COLOR:
		ret.add(color());
		break;
	    case T_TTOL:
		ret.add(list());
		break;
	    case T_NIL:
		ret.add(null);
		break;
	    case T_BYTES:
		int len = uint8();
		if((len & 128) != 0)
		    len = int32();
		ret.add(bytes(len));
		break;
	    case T_FLOAT32:
		ret.add(float32());
		break;
	    case T_FLOAT64:
		ret.add(float64());
		break;
	    default:
		throw(new RuntimeException("Encountered unknown type " + t + " in TTO list."));
	    }
	}
	return(ret.toArray());
    }

    public Message inflate(int length) {
	Message ret = new Message(0);
	Inflater z = new Inflater();
	z.setInput(blob, off, length);
	byte[] buf = new byte[10000];
	while(true) {
	    try {
		int len;
		if((len = z.inflate(buf)) == 0) {
		    if(!z.finished())
			throw(new RuntimeException("Got unterminated gzip blob"));
		    break;
		}
		ret.addbytes(buf, 0, len);
	    } catch(java.util.zip.DataFormatException e) {
		throw(new RuntimeException("Got malformed gzip blob", e));
	    }
	}
	return(ret);
    }

    public Message inflate() {
	return(inflate(blob.length - off));
    }

    public String toString() {
	String ret = "";
	for(byte b : blob) {
	    ret += String.format("%02x ", b);
	}
	return("Message(" + type + "): " + ret);
    }
}
