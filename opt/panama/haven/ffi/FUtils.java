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

package haven.ffi;

import haven.*;
import java.nio.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.lang.invoke.*;
import java.lang.foreign.*;
import java.util.*;
import java.util.function.*;
import java.lang.foreign.MemoryLayout.PathElement;

public class FUtils {
    public static boolean nullp(MemorySegment mem) {
	return(mem.address() == 0);
    }

    public static <T> MemorySegment ornull(T thing, Function<? super T, ? extends MemorySegment> get) {
	return((thing == null) ? MemorySegment.NULL : get.apply(thing));
    }

    public static StructLayout struct(MemoryLayout... decl) {
	List<MemoryLayout> padded = new ArrayList<>(decl.length);
	long off = 0;
	for(int i = 0; i < decl.length; i++) {
	    long alg = decl[i].byteAlignment();
	    long noff = ((off + alg - 1) / alg) * alg;
	    if(noff > off) {
		padded.add(MemoryLayout.paddingLayout(noff - off));
		off = noff;
	    }
	    padded.add(decl[i]);
	    off += decl[i].byteSize();
	}
	return(MemoryLayout.structLayout(padded.toArray(new MemoryLayout[0])));
    }

    public static Map<String, Object> dumpstruct(MemorySegment mem, StructLayout desc) {
	Map<String, Object> ret = new LinkedHashMap<>();
	for(MemoryLayout memb : desc.memberLayouts()) {
	    if(memb.name().isEmpty())
		continue;
	    if(memb instanceof ValueLayout) {
		ret.put(memb.name().get(), desc.varHandle(PathElement.groupElement(memb.name().get())).get(mem,  0));
	    }
	}
	return(ret);
    }

    public static MethodHandle slookup(MethodHandles.Lookup lookup, Class<?> cl, String name, Class<?> rtype, Class<?>... atypes) {
	try {
	    return(lookup.findStatic(cl, name, MethodType.methodType(rtype, atypes)));
	} catch(NoSuchMethodException | IllegalAccessException e) {
	    throw(new RuntimeException(e));
	}
    }

    public static MethodHandle vlookup(MethodHandles.Lookup lookup, Class<?> cl, String name, Class<?> rtype, Class<?>... atypes) {
	try {
	    return(lookup.findVirtual(cl, name, MethodType.methodType(rtype, atypes)));
	} catch(NoSuchMethodException | IllegalAccessException e) {
	    throw(new RuntimeException(e));
	}
    }

    public static String fmtstruct(String name, MemorySegment mem, StructLayout desc) {
	StringBuilder buf = new StringBuilder();
	buf.append("(" + name + ") {");
	boolean f = true;
	for(MemoryLayout memb : desc.memberLayouts()) {
	    if(memb.name().isEmpty())
		continue;
	    String val = null;
	    if(memb instanceof ValueLayout) {
		if(memb instanceof AddressLayout) {
		    val = "0x" + Long.toHexString(((MemorySegment)desc.varHandle(PathElement.groupElement(memb.name().get())).get(mem,  0)).address());
		} else {
		    val = String.valueOf(desc.varHandle(PathElement.groupElement(memb.name().get())).get(mem,  0));
		}
	    }
	    if(val != null) {
		if(!f)
		    buf.append(", ");
		f = false;
		buf.append("."); buf.append(memb.name().get()); buf.append(" = "); buf.append(val);
	    }
	}
	buf.append("}");
	return(buf.toString());
    }

    public static long getint(MemorySegment mem, long offset, MemoryLayout type, boolean signed) {
	switch((int)type.byteSize()) {
	case 1: {
	    byte ret = mem.get(ValueLayout.JAVA_BYTE, offset);
	    return(signed ? ret : (ret & 0xffl));
	}
	case 2: {
	    short ret = mem.get(ValueLayout.JAVA_SHORT, offset);
	    return(signed ? ret : (ret & 0xffffl));
	}
	case 4: {
	    int ret = mem.get(ValueLayout.JAVA_INT, offset);
	    return(signed ? ret : (ret & 0xffffffffl));
	}
	case 8: {
	    long ret = mem.get(ValueLayout.JAVA_LONG, offset);
	    return(ret);
	}
	}
	throw(new RuntimeException(String.valueOf(type)));
    }

    public static MemorySegment setint(MemorySegment mem, long offset, MemoryLayout type, long value) {
	switch((int)type.byteSize()) {
	case 1:
	    mem.set(ValueLayout.JAVA_BYTE, offset, (byte)value);
	    return(mem);
	case 2:
	    mem.set(ValueLayout.JAVA_SHORT, offset, (short)value);
	    return(mem);
	case 4:
	    mem.set(ValueLayout.JAVA_INT, offset, (int)value);
	    return(mem);
	case 8:
	    mem.set(ValueLayout.JAVA_LONG, offset, value);
	    return(mem);
	}
	throw(new RuntimeException(String.valueOf(type)));
    }

    public static byte[] memcpy(byte[] dst, MemorySegment src, int doff, long soff, int len) {
	for(int i = 0; i < len; i++)
	    dst[doff + i] = (byte)src.get(ValueLayout.JAVA_BYTE, soff + i);
	return(dst);
    }

    public static String nstring(MemorySegment src, long off, int len, Charset charset) {
	return(new String(memcpy(new byte[len], src.reinterpret(off + len), 0, off, len), charset));
    }

    public static MemorySegment memcpy(MemorySegment dst, ByteBuffer src, long doff, int soff, int len) {
	for(int i = 0; i < len; i++)
	    dst.set(ValueLayout.JAVA_BYTE, doff + i, src.get(soff + i));
	return(dst);
    }

    public static MemorySegment memcpy(MemorySegment dst, byte[] v) {
	for(int i = 0; i < v.length; i++)
	    dst.set(ValueLayout.JAVA_BYTE, i, v[i]);
	return(dst);
    }

    public static MemorySegment memcpy(MemorySegment dst, short[] v) {
	for(int i = 0; i < v.length; i++)
	    dst.set(ValueLayout.JAVA_SHORT, i * ValueLayout.JAVA_SHORT.byteSize(), v[i]);
	return(dst);
    }

    public static MemorySegment memcpy(MemorySegment dst, int[] v) {
	for(int i = 0; i < v.length; i++)
	    dst.set(ValueLayout.JAVA_INT, i * ValueLayout.JAVA_INT.byteSize(), v[i]);
	return(dst);
    }

    public static MemorySegment memcpy(MemorySegment dst, long[] v) {
	for(int i = 0; i < v.length; i++)
	    dst.set(ValueLayout.JAVA_LONG, i * ValueLayout.JAVA_LONG.byteSize(), v[i]);
	return(dst);
    }

    public static MemorySegment memcpy(MemorySegment dst, float[] v) {
	for(int i = 0; i < v.length; i++)
	    dst.set(ValueLayout.JAVA_FLOAT, i * ValueLayout.JAVA_FLOAT.byteSize(), v[i]);
	return(dst);
    }

    public static MemorySegment memcpy(MemorySegment dst, double[] v) {
	for(int i = 0; i < v.length; i++)
	    dst.set(ValueLayout.JAVA_DOUBLE, i * ValueLayout.JAVA_DOUBLE.byteSize(), v[i]);
	return(dst);
    }

    public static MemorySegment bufcpy(Arena st, ByteBuffer buf, long count) {
	if(buf == null)
	    return(MemorySegment.NULL);
	if(buf.remaining() < count)
	    throw(new BufferUnderflowException());
	if(buf.isDirect()) {
	    return(MemorySegment.ofBuffer(buf));
	} else {
	    return(memcpy(st.allocate(count), buf, 0, buf.position(), (int)count));
	}
    }

    public static String constname(Class<?> cl, long val) {
	String ret = null;
	for(java.lang.reflect.Field f : cl.getFields()) {
	    if(((f.getModifiers() & java.lang.reflect.Modifier.STATIC) != 0) &&
	       ((f.getModifiers() & java.lang.reflect.Modifier.PUBLIC) != 0)) {
		long v;
		try {
		    if(f.getType() == Byte.TYPE)
			v = f.getByte(null);
		    else if(f.getType() == Short.TYPE)
			v = f.getShort(null);
		    else if(f.getType() == Integer.TYPE)
			v = f.getInt(null);
		    else if(f.getType() == Long.TYPE)
			v = f.getLong(null);
		    else
			continue;
		} catch(IllegalAccessException e) {
		    continue;
		}
		if(v == val) {
		    if(ret == null)
			ret = f.getName();
		    else
			ret = ret + " or " + f.getName();
		}
	    }
	}
	if(ret == null)
	    return(Long.toUnsignedString(val, 16));
	return(ret);
    }

    private static Collection<Path> libpath = null;
    public static SymbolLookup loadlib(String name, Arena arena) {
	Collection<Path> libpath;
	synchronized(FUtils.class) {
	    if(FUtils.libpath == null) {
		String spec = Utils.getprop("java.library.path", "");
		if(spec.equals("")) {
		    FUtils.libpath = Collections.emptyList();
		} else {
		    ArrayList<Path> buf = new ArrayList<>();
		    int p = 0;
		    String sep = java.io.File.pathSeparator;
		    boolean done = false;
		    while(!done) {
			int p2 = spec.indexOf(sep, p);
			String nm;
			if(p2 < 0) {
			    nm = spec.substring(p);
			    done = true;
			} else {
			    nm = spec.substring(p, p2);
			    p = p2 + sep.length();
			}
			try {
			    buf.add(Paths.get(nm));
			} catch(InvalidPathException e) {
			    new Warning(e, "invalid library path: " + e).issue();
			}
		    }
		    FUtils.libpath = buf;
		}
	    }
	    libpath = FUtils.libpath;
	}
	if(libpath != null) {
	    for(Path p : libpath) {
		try {
		    return(SymbolLookup.libraryLookup(p.resolve(name), arena));
		} catch(IllegalArgumentException e) {
		    e.printStackTrace();
		}
	    }
	}
	return(SymbolLookup.libraryLookup(name, arena));
    }
}
