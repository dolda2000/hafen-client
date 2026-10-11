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

package haven.ffi.windows;

import haven.*;
import haven.ffi.*;
import java.util.*;
import java.util.function.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.nio.*;
import java.lang.invoke.*;
import java.lang.foreign.*;
import java.lang.ref.*;
import java.lang.reflect.*;
import java.lang.annotation.*;
import java.lang.foreign.MemoryLayout.PathElement;
import static haven.ffi.ABI.*;
import static haven.ffi.FUtils.*;
import static haven.ffi.windows.Win32.*;
import static java.lang.foreign.ValueLayout.ADDRESS;

public abstract class COM {
    public static final int COINIT_APARTMENTTHREADED = 0x2;
    public static final int COINIT_MULTITHREADED     = 0x0;
    public static final int COINIT_DISABLE_OLE1DDE   = 0x4;
    public static final int COINIT_SPEED_OVER_MEMORY = 0x8;

    public static final int CLSCTX_INPROC_SERVER    = 0x1;
    public static final int CLSCTX_INPROC_HANDLER   = 0x2;
    public static final int CLSCTX_LOCAL_SERVER     = 0x4;
    public static final int CLSCTX_INPROC_SERVER16  = 0x8;
    public static final int CLSCTX_REMOTE_SERVER    = 0x10;
    public static final int CLSCTX_INPROC_HANDLER16 = 0x20;

    public static final int DVASPECT_CONTENT   = 1;
    public static final int DVASPECT_THUMBNAIL = 2;
    public static final int DVASPECT_ICON      = 4;
    public static final int DVASPECT_DOCPRINT  = 8;

    public static final int TYMED_NULL     =  0;
    public static final int TYMED_HGLOBAL  =  1;
    public static final int TYMED_FILE     =  2;
    public static final int TYMED_ISTREAM  =  4;
    public static final int TYMED_ISTORAGE =  8;
    public static final int TYMED_GDI      = 16;
    public static final int TYMED_MFPICT   = 32;
    public static final int TYMED_ENHMF    = 64;

    public static final int DATADIR_GET = 1;
    public static final int DATADIR_SET = 2;

    public abstract void CoUninitialize();
    public abstract void CoInitializeEx(int init);
    public abstract void OleUninitialize();
    public abstract void OleInitialize();
    public abstract <I extends IUnknown> I CoCreateInstance(CLSID clsid, IUnknown outer, int context, IID<I> iid);
    abstract MemorySegment CoTaskMemAlloc(int bytes);
    abstract void CoTaskMemFree(MemorySegment mem);
    public abstract void RegisterDragDrop(Handle hwnd, IDropTarget droptarget);
    public abstract void RevokeDragDrop(Handle hwnd);

    public static boolean SUCCEEDED(int hr) {return(hr >= 0);}

    public static abstract class FORMATETC extends StructInstance {
	protected FORMATETC(MemorySegment mem) {
	    super(mem);
	}

	public static FORMATETC fromcom(MemorySegment mem) {
	    return(COM.get().FORMATETC_fromcom(mem));
	}

	MemorySegment mem() {return(mem);}

	public abstract FORMATETC cfFormat(int value);
	public abstract int cfFormat();
	public abstract FORMATETC dwAspect(int value);
	public abstract int dwAspect();
	public abstract FORMATETC lindex(int value);
	public abstract int lindex();
	public abstract FORMATETC tymed(int value);
	public abstract int tymed();
    }
    public abstract MemoryLayout C_FORMATETC();
    public abstract FORMATETC FORMATETC();
    abstract FORMATETC FORMATETC_fromcom(MemorySegment mem);

    public static abstract class STGMEDIUM extends StructInstance {
	protected STGMEDIUM(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}

	public abstract STGMEDIUM tymed(int value);
	public abstract int tymed();
	public abstract STGMEDIUM handle(Handle value);
	public abstract Handle handle();
	abstract STGMEDIUM address(MemorySegment value);
	abstract MemorySegment address();

	public Object get() {
	    int t = tymed();
	    switch(t) {
	    case TYMED_HGLOBAL:
		return(new HGlobal(handle(), true));
	    case TYMED_FILE:
		return(address().getString(0, Win32.get().C_WCHARSET()));
	    default:
		throw(new ClassCastException("unexpected medium-type: " + t));
	    }
	}
    }
    public abstract MemoryLayout C_STGMEDIUM();
    public abstract STGMEDIUM STGMEDIUM();

    static MemorySegment memguid(UUID uuid) {
	MemorySegment ret = Arena.ofAuto().allocate(16);
	ret.set(ValueLayout.JAVA_INT, 0, (int)(uuid.getMostSignificantBits() >>> 32));
	ret.set(ValueLayout.JAVA_SHORT, 4, (short)(uuid.getMostSignificantBits() >>> 16));
	ret.set(ValueLayout.JAVA_SHORT, 6, (short)(uuid.getMostSignificantBits() >>> 0));
	for(int i = 0; i < 8; i++)
	    ret.set(ValueLayout.JAVA_BYTE, 15 - i, (byte)(uuid.getLeastSignificantBits() >>> (i * 8)));
	return(ret);
    }

    public static class CLSID {
	public final UUID uuid;
	final MemorySegment mem;

	public CLSID(UUID uuid) {
	    this.uuid = uuid;
	    this.mem = memguid(uuid);
	}

	public CLSID(long msb, long lsb) {
	    this(new UUID(msb, lsb));
	}
    }

    public static class IID<I extends IUnknown> {
	public final Class<I> iface;
	public final UUID uuid;
	final MemorySegment mem;

	public IID(Class<I> iface, UUID uuid) {
	    this.iface = iface;
	    this.uuid = uuid;
	    this.mem = memguid(uuid);
	}

	public IID(Class<I> iface, long msb, long lsb) {
	    this(iface, new UUID(msb, lsb));
	}

	public static <I extends IUnknown> IID<I> get(Class<I> cl) {
	    try {
		Field f = cl.getDeclaredField("IID");
		IID<?> iid = (IID<?>)f.get(null);
		if(iid.iface != cl)
		    throw(new RuntimeException(cl.toString()));
		@SuppressWarnings("unchecked") IID<I> checked = (IID<I>)iid;
		return(checked);
	    } catch(NoSuchFieldException e) {
		throw(new ClassCastException("not a declared COM interface: " + cl));
	    } catch(IllegalAccessException e) {
		throw(new RuntimeException(e));
	    }
	}
    }

    MemorySegment getvtab(MemorySegment obj, int idx) {
	return(obj.reinterpret(ADDRESS.byteSize()).get(ADDRESS, 0)
	       .reinterpret(Long.MAX_VALUE).getAtIndex(ADDRESS, idx));
    }

    public static class Downcall {
	public final COM com;
	public final MethodHandle fun;

	public Downcall(COM com, MethodHandle fun) {
	    this.com = com;
	    this.fun = fun;
	}
    }

    public static interface DowncallAdapter {
	public Object call(MemorySegment obj, Object[] args);

	public static interface Arg {
	    public Object convert(Arena st, Object val);
	}
    }

    public static interface UpcallAdapter {
	public int call(Registry.LocalStub self, Object[] args);

	public static final UpcallAdapter nil = (self, args) -> {
	    throw(new RuntimeException("no upcall defined"));
	};
    }

    public static class ValueConversion {
	public final MemoryLayout arg;
	public final Class<?> par;
	public final DowncallAdapter.Arg darg;
	public final Function<MemorySegment, Object> dret;
	public final Function<Object, Object> uarg;
	public final BiConsumer<Object, MemorySegment> uret;

	public ValueConversion(MemoryLayout arg, Class<?> par, DowncallAdapter.Arg darg, Function<MemorySegment, Object> dret, Function<Object, Object> uarg, BiConsumer<Object, MemorySegment> uret) {
	    this.arg = arg;
	    this.par = par;
	    this.darg = darg;
	    this.dret = dret;
	    this.uarg = uarg;
	    this.uret = uret;
	}
    }
    private static Map<Pair<MemoryLayout, Class<?>>, ValueConversion> valxfs = new HashMap<>();
    public static ValueConversion valxf(MemoryLayout arg, Class<?> par) {
	synchronized(valxfs) {
	    return(valxfs.get(new Pair<>(arg, par)));
	}
    }
    public static void valxf(ValueConversion xf) {
	synchronized(valxfs) {
	    valxfs.put(new Pair<>(xf.arg, xf.par), xf);
	}
    }

    public class StandardDowncall implements DowncallAdapter {
	private final int vindex;
	private final MethodHandle fun;
	private final Arg[] argxf;
	private final MemoryLayout outtype;
	private final Function<MemorySegment, Object> outxf;

	public StandardDowncall(Descriptor desc, Method mth) {
	    Win32 win = Win32.get();
	    this.vindex = desc.vindex;
	    this.outtype = desc.outtype;
	    Class<?>[] pars = mth.getParameterTypes();
	    MemoryLayout[] args = desc.sig.argumentLayouts().toArray(new MemoryLayout[0]);
	    if(pars.length != args.length - ((outtype == null) ? 1 : 2))
		throw(new IllegalArgumentException(mth + ": " + Arrays.asList(pars) + " does not match " + Arrays.asList(args)));
	    argxf = new Arg[pars.length];
	    fun = ld.downcallHandle(desc.sig);
	    for(int i = 0; i < pars.length; i++) {
		Class<?> p = pars[i]; MemoryLayout a = args[i + 1];
		Arg xf;
		ValueConversion valxf = valxf(a, p);
		if((valxf != null) && (valxf.darg != null)) {
		    xf = valxf.darg;
		} else if(a == ValueLayout.JAVA_BYTE) {
		    if(p == Byte.TYPE) {
			xf = null;
		    } else if(p == Boolean.TYPE) {
			xf = (st, v) -> ((boolean)v) ? (byte)1 : (byte)0;
		    } else {
			throw(new IllegalArgumentException(p + " -> " + a));
		    }
		} else if(a == ValueLayout.JAVA_SHORT) {
		    if((p == Short.TYPE) || (p == Byte.TYPE)) {
			xf = null;
		    } else if(p == Boolean.TYPE) {
			xf = (st, v) -> ((boolean)v) ? (byte)1 : (byte)0;
		    } else {
			throw(new IllegalArgumentException(p + " -> " + a));
		    }
		} else if(a == ValueLayout.JAVA_INT) {
		    if((p == Integer.TYPE) || (p == Short.TYPE) || (p == Byte.TYPE)) {
			xf = null;
		    } else if(p == Boolean.TYPE) {
			xf = (st, v) -> ((boolean)v) ? (byte)1 : (byte)0;
		    } else {
			throw(new IllegalArgumentException(p + " -> " + a));
		    }
		} else if(a == ValueLayout.JAVA_LONG) {
		    if((p == Long.TYPE) || (p == Integer.TYPE) || (p == Short.TYPE) || (p == Byte.TYPE)) {
			xf = null;
		    } else if(p == Boolean.TYPE) {
			xf = (st, v) -> ((boolean)v) ? (byte)1 : (byte)0;
		    } else {
			throw(new IllegalArgumentException(p + " -> " + a));
		    }
		} else if(a == ADDRESS) {
		    if(p == MemorySegment.class) {
			xf = null;
		    } else if(p == Handle.class) {
			xf = (st, v) -> (v == null) ? MemorySegment.NULL : ((Handle)v).bits;
		    } else if(p == IID.class) {
			xf = (st, v) -> ((IID)v).mem;
		    } else if(p == CLSID.class) {
			xf = (st, v) -> ((CLSID)v).mem;
		    } else if(p == String.class) {
			xf = (st, v) -> (v == null) ? MemorySegment.NULL : st.allocateFrom((String)v, win.C_WCHARSET());
		    } else if(IUnknown.class.isAssignableFrom(p)) {
			xf = (st, v) -> reg.addrforobj((IUnknown)v, IID.get(p.asSubclass(IUnknown.class)));
		    } else if(StructInstance.class.isAssignableFrom(p)) {
			xf = (st, v) -> StructInstance.extract((StructInstance)v);
		    } else {
			throw(new IllegalArgumentException(p + " -> " + a));
		    }
		} else {
		    throw(new IllegalArgumentException(p + " -> " + a));
		}
		argxf[i] = xf;
	    }
	    if(outtype == null) {
		outxf = null;
	    } else {
		Class<?> rtype = mth.getReturnType();
		ValueConversion valxf = valxf(outtype, rtype);
		if((valxf != null) && (valxf.dret != null)) {
		    outxf = valxf.dret;
		} else if(rtype == Integer.TYPE) {
		    if((outtype == ValueLayout.JAVA_INT) || (outtype == ValueLayout.JAVA_SHORT) || (outtype == ValueLayout.JAVA_INT)) {
			outxf = buf -> (int)getint(buf, 0, outtype, true);
		    } else {
			throw(new IllegalArgumentException(outtype + " -> " + rtype));
		    }
		} else if(rtype == String.class) {
		    if(outtype == ADDRESS) {
			outxf = buf -> {
			    MemorySegment str = buf.get(ADDRESS, 0);
			    if(nullp(str))
				return(null);
			    String ret = str.reinterpret(Long.MAX_VALUE).getString(0, win.C_WCHARSET());
			    CoTaskMemFree(str);
			    return(ret);
			};
		    } else {
			throw(new IllegalArgumentException(outtype + " -> " + rtype));
		    }
		} else if(IUnknown.class.isAssignableFrom(rtype)) {
		    if(outtype == ADDRESS) {
			outxf = buf -> reg.objforaddr(buf.get(ADDRESS, 0), rtype.asSubclass(IUnknown.class), false);
		    } else {
			throw(new IllegalArgumentException(outtype + " -> " + rtype));
		    }
		} else {
		    throw(new IllegalArgumentException(outtype + " -> " + rtype));
		}
	    }
	}

	public Object call(MemorySegment obj, Object[] pars) {
	    if(nullp(obj))
		throw(new NullPointerException());
	    try(Arena st = Arena.ofConfined()) {
		Object[] args = new Object[pars.length + ((outtype == null) ? 2 : 3)];
		args[0] = getvtab(obj, vindex);
		args[1] = obj;
		for(int i = 0; i < pars.length; i++)
		    args[i + 2] = (argxf[i] == null) ? pars[i] : argxf[i].convert(st, pars[i]);
		MemorySegment outbuf = null;
		if(outtype != null) {
		    outbuf = st.allocate(outtype);
		    args[pars.length + 2] = outbuf;
		}
		int rv;
		try {
		    rv = (int)fun.invokeWithArguments(args);
		} catch(Throwable t) {
		    throw(new InvocationException(t));
		}
		if(!SUCCEEDED(rv))
		    throw(new HResultError(rv));
		if(outtype == null)
		    return(rv);
		return(outxf.apply(outbuf));
	    }
	}
    }

    public static class CustomDowncall implements DowncallAdapter {
	private final Downcall info;
	private final Method handler;

	public CustomDowncall(Descriptor desc, Method mth, Method handler) {
	    this.info = new Downcall(COM.get(), ld.downcallHandle(desc.sig));
	    this.handler = handler;
	}

	public Object call(MemorySegment obj, Object[] pars) {
	    Object[] args = new Object[pars.length + 2];
	    args[0] = info;
	    args[1] = obj;
	    System.arraycopy(pars, 0, args, 2, pars.length);
	    return(Utils.invoke(handler, null, args));
	}
    }

    public class StandardUpcall implements UpcallAdapter {
	private final Function<Object, Object>[] argxf;
	private final BiConsumer<Object, MemorySegment> outxf;
	private final Method method;
	private final MemoryLayout outtype;
	private final boolean resret;

	@SuppressWarnings("unchecked")
	public StandardUpcall(Descriptor desc, Method mth) {
	    Win32 win = Win32.get();
	    this.method = mth;
	    outtype = desc.outtype;
	    Class<?>[] pars = mth.getParameterTypes();
	    MemoryLayout[] args = desc.sig.argumentLayouts().toArray(new MemoryLayout[0]);
	    argxf = new Function[pars.length];
	    if(pars.length != args.length - ((outtype == null) ? 1 : 2))
		throw(new IllegalArgumentException());
	    for(int i = 0; i < pars.length; i++) {
		Class<?> p = pars[i]; MemoryLayout a = args[i + 1];
		Function<Object, Object> xf;
		ValueConversion valxf = valxf(a, p);
		if((valxf != null) && (valxf.uarg != null)) {
		    xf = valxf.uarg;
		} else if((a == ValueLayout.JAVA_BYTE) || (a == ValueLayout.JAVA_SHORT) ||
		   (a == ValueLayout.JAVA_INT)  || (a == ValueLayout.JAVA_LONG)) {
		    if(p == Byte.TYPE)
			xf = v -> ((Number)v).byteValue();
		    else if(p == Short.TYPE)
			xf = v -> ((Number)v).shortValue();
		    else if(p == Integer.TYPE)
			xf = v -> ((Number)v).intValue();
		    else if(p == Long.TYPE)
			xf = v -> ((Number)v).longValue();
		    else
			throw(new IllegalArgumentException(a + " -> " + p));
		} else if(a == ADDRESS) {
		    if(p == MemorySegment.class) {
			xf = null;
		    } else if(p == String.class) {
			xf = v -> nullp((MemorySegment)v) ? null : ((MemorySegment)v).reinterpret(Long.MAX_VALUE).getString(0, win.C_WCHARSET());
		    } else if(p == Handle.class) {
			xf = v -> Handle.of((MemorySegment)v);
		    } else if(IUnknown.class.isAssignableFrom(p)) {
			xf = v -> reg.objforaddr((MemorySegment)v, p.asSubclass(IUnknown.class), true);
		    } else if(StructInstance.class.isAssignableFrom(p)) {
			try {
			    Method cons = p.getDeclaredMethod("fromcom", MemorySegment.class);
			    if(!Modifier.isStatic(cons.getModifiers()))
				throw(new IllegalArgumentException("not statuc " + cons));
			    xf = v -> p.cast(Utils.invoke(cons, null, v));
			} catch(NoSuchMethodException e) {
			    throw(new IllegalArgumentException("cannot construct " + p));
			}
		    } else {
			throw(new IllegalArgumentException(a + " -> " + p));
		    }
		} else {
		    throw(new IllegalArgumentException(a + " -> " + p));
		}
		argxf[i] = xf;
	    }
	    Class<?> rtype = mth.getReturnType();
	    if(outtype == null) {
		if(rtype == Integer.TYPE)
		    resret = true;
		else if(rtype == Void.TYPE)
		    resret = false;
		else
		    throw(new IllegalArgumentException(rtype + " -> void"));
		outxf = null;
	    } else {
		resret = false;
		ValueConversion valxf = valxf(outtype, rtype);
		if((valxf != null) && (valxf.uret != null)) {
		    outxf = valxf.uret;
		} else if(outtype == ValueLayout.JAVA_BYTE) {
		    outxf = (v, buf) -> buf.set(ValueLayout.JAVA_BYTE, 0, ((Number)v).byteValue());
		} else if(outtype == ValueLayout.JAVA_SHORT) {
		    outxf = (v, buf) -> buf.set(ValueLayout.JAVA_SHORT, 0, ((Number)v).shortValue());
		} else if(outtype == ValueLayout.JAVA_INT) {
		    outxf = (v, buf) -> buf.set(ValueLayout.JAVA_INT, 0, ((Number)v).intValue());
		} else if(outtype == ValueLayout.JAVA_LONG) {
		    outxf = (v, buf) -> buf.set(ValueLayout.JAVA_LONG, 0, ((Number)v).longValue());
		} else if(outtype == ADDRESS) {
		    if(rtype == String.class) {
			outxf = (v, buf) -> {
			    if(v == null) {
				buf.set(ADDRESS, 0, MemorySegment.NULL);
				return;
			    }
			    try(Arena st = Arena.ofConfined()) {
				MemorySegment enc = st.allocateFrom((String)v, win.C_WCHARSET());
				MemorySegment ret = CoTaskMemAlloc((int)enc.byteSize());
				MemorySegment.copy(enc, 0, ret, 0, enc.byteSize());
				buf.set(ADDRESS, 0, ret);
			    }
			};
		    } else if(IUnknown.class.isAssignableFrom(rtype)) {
			outxf = (v, buf) -> buf.set(ADDRESS, 0, reg.addrforobj((IUnknown)v, IID.get(rtype.asSubclass(IUnknown.class))));
		    } else {
			throw(new IllegalArgumentException(rtype + " -> "  + outtype));
		    }
		} else {
		    throw(new IllegalArgumentException(rtype + " -> "  + outtype));
		}
	    }
	}

	public int call(Registry.LocalStub stub, Object[] args) {
	    try {
		Object[] pars = new Object[argxf.length];
		for(int i = 0; i < pars.length; i++)
		    pars[i] = (argxf[i] == null) ? args[i] : (argxf[i].apply(args[i]));
		Object rv = Utils.invoke(method, stub.obj, pars);
		if(outtype != null)
		    outxf.accept(rv, ((MemorySegment)args[args.length - 1]).reinterpret(outtype.byteSize()));
		if(resret)
		    return(((Number)rv).intValue());
		return(0);
	    } catch(HResultError e) {
		return(e.hresult);
	    }
	}
    }

    public static class Descriptor {
	public final int vindex;
	public final FunctionDescriptor sig;
	public final MemoryLayout outtype;
	DowncallAdapter da = null;
	UpcallAdapter ua = null;

	public Descriptor(int vindex, FunctionDescriptor sig, MemoryLayout outtype) {
	    this.vindex = vindex;
	    this.sig = sig;
	    this.outtype = outtype;
	}

	public Descriptor(int vindex, FunctionDescriptor sig) {
	    this(vindex, sig, null);
	}

	public Descriptor downcall(DowncallAdapter da) {
	    this.da = da; return(this);
	}
	public Descriptor upcall(UpcallAdapter ua) {
	    this.ua = ua; return(this);
	}

	private static final Map<Method, Descriptor> cache = new ConcurrentHashMap<>();
	public static Descriptor get(Method method) {
	    Descriptor ret = cache.get(method);
	    if(ret != null)
		return(ret);
	    try {
		Field f = method.getDeclaringClass().getDeclaredField("sig_" + method.getName());
		if(!Modifier.isStatic(f.getModifiers()))
		    return(null);
		ret = (Descriptor)f.get(null);
		synchronized(ret) {
		    if(ret.da == null) {
			Method handler = smethod(method.getDeclaringClass(), "downcall_" + method.getName());
			if(handler != null)
			    ret.da = new CustomDowncall(ret, method, handler);
			else
			    ret.da = COM.get().new StandardDowncall(ret, method);
		    }
		    if(ret.ua == null) {
			ret.ua = COM.get().new StandardUpcall(ret, method);
		    }
		}
		cache.put(method, ret);
		return(ret);
	    } catch(ReflectiveOperationException e) {
		return(null);
	    }
	}
    }

    public class Registry {
	private final Map<Pair<IUnknown, IID>, LocalStub<?>> stubsbyobj = new HashMap<>();
	private final Map<Long, LocalStub> stubsbyaddr = new HashMap<>();
	private final Map<Long, Reference<Foreign>> foreign = new HashMap<>();

	public class Callback {
	    public final MemorySegment stub;
	    public final Method method;
	    public final UpcallAdapter ua;

	    private static Class<?> maptype(MemoryLayout type) {
		if(type == ValueLayout.JAVA_BYTE)
		    return(Byte.TYPE);
		else if(type == ValueLayout.JAVA_SHORT)
		    return(Short.TYPE);
		else if(type == ValueLayout.JAVA_INT)
		    return(Integer.TYPE);
		else if(type == ValueLayout.JAVA_LONG)
		    return(Long.TYPE);
		else if(type == ADDRESS)
		    return(MemorySegment.class);
		else if(type instanceof StructLayout)
		    return(MemorySegment.class);
		throw(new IllegalArgumentException(String.valueOf(type)));
	    }

	    Callback(Arena alloc, Method mth, Descriptor desc) {
		MemoryLayout[] args = desc.sig.argumentLayouts().toArray(new MemoryLayout[0]);
		Class<?>[] pars = new Class<?>[args.length];
		for(int i = 0; i < args.length; i++)
		    pars[i] = maptype(args[i]);
		Optional<MemoryLayout> rtype = desc.sig.returnLayout();
		MethodType type = MethodType.methodType(rtype.map(Callback::maptype).orElse(Void.TYPE), pars);
		MethodHandle handler = callback.bindTo(this);
		handler = handler.asCollector(Object[].class, args.length - 1);
		handler = handler.asType(type);
		this.stub = ld.upcallStub(handler, desc.sig, alloc);
		this.method = mth;
		this.ua = desc.ua;
	    }

	    private static final MethodHandle callback = vlookup(MethodHandles.lookup(), Callback.class, "callback", Integer.TYPE,
								 MemorySegment.class, Object[].class);
	    private int callback(MemorySegment self, Object[] args) {
		return(upcallwrap(() -> {
		    LocalStub<?> stub;
		    synchronized(Registry.this) {
			stub = stubsbyaddr.get(self.address());
		    }
		    if(stub == null) {
			Warning.warn("call to " + method + " on unregistered stub $" + Long.toUnsignedString(self.address(), 16));
			return(HResultError.E_FAIL);
		    }
		    if((method != null) && !method.getDeclaringClass().isInstance(stub.obj)) {
			Warning.warn("call to " + method + " on non-matching " + stub.obj);
			return(HResultError.E_FAIL);
		    }
		    return(ua.call(stub, args));
		}, HResultError.E_FAIL));
	    }
	}

	public class VTable {
	    public final MemorySegment vtbl;
	    private final Callback[] retain;

	    VTable(Class<? extends IUnknown> iface) {
		Arena alloc = Arena.ofAuto();
		Callback[] table = new Callback[3];
		table[0] = stub_QueryInterface;
		table[1] = stub_AddRef;
		table[2] = stub_Release;
		for(Method mth : iface.getMethods()) {
		    if(!IUnknown.class.isAssignableFrom(mth.getDeclaringClass()) || (mth.getDeclaringClass() == IUnknown.class))
			continue;
		    Descriptor desc = Descriptor.get(mth);
		    if(desc == null)
			continue;
		    if(desc.vindex >= table.length)
			table = Arrays.copyOf(table, desc.vindex + 1);
		    table[desc.vindex] = new Callback(alloc, mth, desc);
		}
		vtbl = alloc.allocate(ADDRESS, table.length);
		for(int i = 0; i < table.length; i++) {
		    if(table[i] != null)
			vtbl.setAtIndex(ADDRESS, i, table[i].stub);
		}
		retain = table;
	    }
	}
	private final Map<Class<? extends IUnknown>, VTable> vtables = new WeakHashMap<>();
	public VTable vtable(Class<? extends IUnknown> iface) {
	    synchronized(vtables) {
		return(vtables.computeIfAbsent(iface, VTable::new));
	    }
	}

	private final Callback stub_QueryInterface, stub_AddRef, stub_Release;
	{
	    Arena alloc = Arena.ofAuto();
	    stub_QueryInterface = new Callback(alloc, null, new Descriptor(0, FunctionDescriptor.of(HRESULT(), ADDRESS, REFIID(), LPVOID()), ADDRESS)
					       .upcall(LocalStub::QueryInterface));
	    stub_AddRef = new Callback(alloc, null, new Descriptor(0, FunctionDescriptor.of(UINT(), ADDRESS))
				       .upcall(LocalStub::AddRef));
	    stub_Release = new Callback(alloc, null, new Descriptor(0, FunctionDescriptor.of(UINT(), ADDRESS))
				       .upcall(LocalStub::Release));
	}
	public class LocalStub<I extends IUnknown> {
	    private final AtomicInteger rc = new AtomicInteger(1);
	    private final I obj;
	    private final IID<I> iface;
	    private final MemorySegment mem;

	    LocalStub(I obj, IID<I> iface) {
		this.obj = obj;
		this.iface = iface;
		mem = Arena.ofAuto().allocate(ADDRESS);
		mem.set(ADDRESS, 0, vtable(iface.iface).vtbl);
	    }

	    Registry reg() {return(Registry.this);}

	    static int QueryInterface(LocalStub stub, Object[] args) {
		Warning.warn("QueryInterface called on COM stub");
		return(HResultError.E_NOINTERFACE);
	    }

	    static int AddRef(LocalStub stub, Object[] args) {
		return(stub.rc.incrementAndGet());
	    }

	    static int Release(LocalStub stub, Object[] args) {
		int rc = stub.rc.decrementAndGet();
		if(rc == 0) {
		    Registry reg = stub.reg();
		    synchronized(reg) {
			if(reg.stubsbyaddr.remove(stub.mem) != stub)
			    throw(new RuntimeException("stub mismatch for " + stub.obj + " when released"));
			if(reg.stubsbyobj.remove(new Pair<>(stub.obj, stub.iface)) != stub)
			    throw(new RuntimeException("stub mismatch for " + stub.obj + " when released"));
		    }
		}
		return(rc);
	    }
	}

	public class Foreign implements InvocationHandler {
	    private static final Method mth_toString = vmethod(Object.class, "toString");
	    public final Class<? extends IUnknown> iface;
	    final MemorySegment mem;
	    Object proxy = null;

	    Foreign(MemorySegment mem, Class<? extends IUnknown> iface) {
		this.iface = iface;
		this.mem = mem;
	    }

	    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if(method.equals(mth_toString))
		    return("#<com-proxy " + iface + ">");
		if(args == null)
		    args = new Object[0];
		Descriptor desc = Descriptor.get(method);
		if(desc != null)
		    return(desc.da.call(mem, args));
		if(method.isDefault())
		    return(InvocationHandler.invokeDefault(proxy, method, args));
		throw(new RuntimeException("no downcall implementation for " + method));
	    }
	}

	final int VINDEX_ADDREF = 1;
	final MethodHandle native_AddRef = ld.downcallHandle(FunctionDescriptor.of(ULONG(), ADDRESS));
	void addref(MemorySegment obj) {
	    MemorySegment rfun = getvtab(obj, VINDEX_ADDREF);
	    try {
		native_AddRef.invoke(rfun, obj);
	    } catch(Throwable t) {
		throw(new InvocationException(t));
	    }
	}

	final int VINDEX_RELEASE = 2;
	final MethodHandle native_Release = ld.downcallHandle(FunctionDescriptor.of(ULONG(), ADDRESS));
	void release(MemorySegment obj) {
	    MemorySegment rfun = getvtab(obj, VINDEX_RELEASE);
	    try {
		native_Release.invoke(rfun, obj);
	    } catch(Throwable t) {
		throw(new InvocationException(t));
	    }
	}

	public <I extends IUnknown> I objforaddr(MemorySegment mem, Class<I> iface, boolean borrowed) {
	    synchronized(this) {
		{
		    LocalStub stub = stubsbyaddr.get(mem.address());
		    if(stub != null) {
			if(!borrowed)
			    stub.rc.decrementAndGet();
			return(iface.cast(stub));
		    }
		}
		{
		    Reference<Foreign> ref = foreign.get(mem.address());
		    Foreign obj = (ref == null) ? null : ref.get();
		    if(obj == null) {
			if(borrowed)
			    addref(mem);
			obj = new Foreign(mem, iface);
			obj.proxy = Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[] {iface}, obj);
			Reference<Foreign> nref = new WeakReference<>(obj);
			Finalizer.finalize(obj, () -> {
			    if(foreign.remove(mem.address()) != nref)
				throw(new AssertionError());
			    release(mem);
			});
			foreign.put(mem.address(), nref);
		    } else {
			if(!borrowed)
			    release(mem);
		    }
		    return(iface.cast(obj.proxy));
		}
	    }
	}

	public <I extends IUnknown> MemorySegment addrforobj(I obj, IID<I> iface) {
	    if(obj == null)
		return(MemorySegment.NULL);
	    InvocationHandler h = null;
	    try {
		h = Proxy.getInvocationHandler(obj);
	    } catch(IllegalArgumentException e) {
	    }
	    if(h instanceof Foreign) {
		Foreign f = (Foreign)h;
		if(!iface.iface.isInstance(obj))
		    throw(new ClassCastException("foreign instance is " + f.iface + ", not " + iface.iface));
		return(f.mem);
	    }
	    if(obj instanceof IUnknown) {
		synchronized(this) {
		    LocalStub<?> stub = stubsbyobj.get(new Pair<>(obj, iface));
		    if(stub != null)
			return(stub.mem);
		    stub = new LocalStub<I>(obj, iface);
		    stubsbyobj.put(new Pair<>(obj, iface), stub);
		    stubsbyaddr.put(stub.mem.address(), stub);
		    return(stub.mem);
		}
	    }
	    throw(new IllegalArgumentException(String.valueOf(obj)));
	}
    }
    final Registry reg = new Registry();

    public abstract MemoryLayout DWORD();
    public abstract MemoryLayout UINT();
    public abstract MemoryLayout ULONG();
    public abstract MemoryLayout HRESULT();
    public abstract MemoryLayout REFIID();
    public abstract MemoryLayout LPVOID();
    public abstract MemoryLayout HWND();

    static class Win64Unicode extends COM {
	private static final MemoryLayout HRESULT    = Win32.Win64Unicode.HRESULT;
	private static final MemoryLayout DWORD      = Win32.Win64Unicode.DWORD;
	private static final MemoryLayout WORD       = Win32.Win64Unicode.WORD;
	private static final MemoryLayout LPVOID     = Win32.Win64Unicode.LPVOID;
	private static final MemoryLayout UINT       = Win32.Win64Unicode.UINT;
	private static final MemoryLayout LONG       = Win32.Win64Unicode.LONG;
	private static final MemoryLayout ULONG      = Win32.Win64Unicode.ULONG;
	private static final MemoryLayout CLIPFORMAT = Win32.Win64Unicode.CLIPFORMAT;
	private static final MemoryLayout HANDLE     = Win32.Win64Unicode.HANDLE;
	private static final MemoryLayout HBITMAP    = Win32.Win64Unicode.HBITMAP;
	private static final MemoryLayout HGLOBAL    = Win32.Win64Unicode.HGLOBAL;
	private static final MemoryLayout HWND       = Win32.Win64Unicode.HWND;
	private static final MemoryLayout LPOLESTR   = ADDRESS;
	private static final MemoryLayout REFCLSID   = ADDRESS;
	private static final MemoryLayout REFIID     = ADDRESS;
	private final SymbolLookup ole32 = loadlib("OLE32.DLL", Arena.global());

	public MemoryLayout DWORD()   {return(DWORD);}
	public MemoryLayout UINT()    {return(UINT);}
	public MemoryLayout ULONG()   {return(ULONG);}
	public MemoryLayout HRESULT() {return(HRESULT);}
	public MemoryLayout REFIID()  {return(REFIID);}
	public MemoryLayout LPVOID()  {return(LPVOID);}
	public MemoryLayout HWND()    {return(HWND);}

	private final MethodHandle CoUninitialize = ld.downcallHandle(ole32.find("CoUninitialize").get(), FunctionDescriptor.ofVoid());
	public void CoUninitialize() {
	    try {
		CoUninitialize.invoke();
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	private final MethodHandle CoInitializeEx = ld.downcallHandle(ole32.find("CoInitializeEx").get(), FunctionDescriptor.of(HRESULT, LPVOID, DWORD));
	public void CoInitializeEx(int init) {
	    int rv;
	    try {
		rv = (int)CoInitializeEx.invoke(MemorySegment.NULL, init);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(!SUCCEEDED(rv))
		throw(new HResultError(rv));
	}

	private final MethodHandle OleUninitialize = ld.downcallHandle(ole32.find("OleUninitialize").get(), FunctionDescriptor.ofVoid());
	public void OleUninitialize() {
	    try {
		OleUninitialize.invoke();
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	private final MethodHandle OleInitialize = ld.downcallHandle(ole32.find("OleInitialize").get(), FunctionDescriptor.of(HRESULT, LPVOID));
	public void OleInitialize() {
	    int rv;
	    try {
		rv = (int)OleInitialize.invoke(MemorySegment.NULL);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(!SUCCEEDED(rv))
		throw(new HResultError(rv));
	}

	private final MethodHandle CoCreateInstance = ld.downcallHandle(ole32.find("CoCreateInstance").get(), FunctionDescriptor.of(HRESULT, REFCLSID, ADDRESS, DWORD, REFIID, LPVOID));
	public <I extends IUnknown> I CoCreateInstance(CLSID clsid, IUnknown outer, int context, IID<I> iid) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment buf = st.allocate(LPVOID);
		MemorySegment outerp = (outer == null) ? MemorySegment.NULL : reg.addrforobj(outer, IUnknown.IID);
		int rv;
		try {
		    rv = (int)CoCreateInstance.invoke(clsid.mem, outerp, context, iid.mem, buf);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(!SUCCEEDED(rv))
		    throw(new HResultError(rv));
		return(reg.objforaddr(buf.get(ADDRESS, 0), iid.iface, false));
	    }
	}

	private final MethodHandle CoTaskMemAlloc = ld.downcallHandle(ole32.find("CoTaskMemAlloc").get(), FunctionDescriptor.of(LPVOID, SIZE_T));
	MemorySegment CoTaskMemAlloc(int bytes) {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)CoTaskMemAlloc.invoke(bytes);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    return(nullp(rv) ? null : rv);
	}

	private final MethodHandle CoTaskMemFree = ld.downcallHandle(ole32.find("CoTaskMemFree").get(), FunctionDescriptor.ofVoid(LPVOID));
	void CoTaskMemFree(MemorySegment mem) {
	    try {
		CoTaskMemFree.invoke(mem);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	private final MethodHandle RegisterDragDrop = ld.downcallHandle(ole32.find("RegisterDragDrop").get(), FunctionDescriptor.of(HRESULT, HWND, ADDRESS));
	public void RegisterDragDrop(Handle hwnd, IDropTarget droptarget) {
	    MemorySegment dt = reg.addrforobj(droptarget, IDropTarget.IID);
	    int rv;
	    try {
		rv = (int)RegisterDragDrop.invoke(hwnd.bits, dt);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(!SUCCEEDED(rv))
		throw(new HResultError(rv));
	}

	private final MethodHandle RevokeDragDrop = ld.downcallHandle(ole32.find("RevokeDragDrop").get(), FunctionDescriptor.of(HRESULT, HWND));
	public void RevokeDragDrop(Handle hwnd) {
	    int rv;
	    try {
		rv = (int)RevokeDragDrop.invoke(hwnd.bits);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(!SUCCEEDED(rv))
		throw(new HResultError(rv));
	}

	static final StructLayout _FORMATETC = struct(new MemoryLayout[] {
		CLIPFORMAT.withName("cfFormat"),
		ADDRESS.withName("ptd"),
		DWORD.withName("dwAspect"),
		LONG.withName("lindex"),
		DWORD.withName("tymed"),
	    });
	public static class FORMATETC extends COM.FORMATETC {
	    private FORMATETC(MemorySegment mem) {
		super(mem);
	    }
	    private FORMATETC(Arena alloc) {
		this(alloc.allocate(_FORMATETC.byteSize()));
	    }
	    private FORMATETC() {
		this(Arena.ofAuto());
	    }

	    protected StructLayout $layout() {return(_FORMATETC);}

	    private static final VarHandle cfFormat = _FORMATETC.varHandle(PathElement.groupElement("cfFormat"));
	    public FORMATETC cfFormat(int value) {cfFormat.set(mem, 0, (short)value); return(this);}
	    public int cfFormat() {return((int)cfFormat.get(mem, 0));}
	    private static final VarHandle dwAspect = _FORMATETC.varHandle(PathElement.groupElement("dwAspect"));
	    public FORMATETC dwAspect(int value) {dwAspect.set(mem, 0, value); return(this);}
	    public int dwAspect() {return((int)dwAspect.get(mem, 0));}
	    private static final VarHandle lindex = _FORMATETC.varHandle(PathElement.groupElement("lindex"));
	    public FORMATETC lindex(int value) {lindex.set(mem, 0, value); return(this);}
	    public int lindex() {return((int)lindex.get(mem, 0));}
	    private static final VarHandle tymed = _FORMATETC.varHandle(PathElement.groupElement("tymed"));
	    public FORMATETC tymed(int value) {tymed.set(mem, 0, value); return(this);}
	    public int tymed() {return((int)tymed.get(mem, 0));}
	}
	public MemoryLayout C_FORMATETC() {return(_FORMATETC);}
	public FORMATETC FORMATETC() {return(new FORMATETC());}
	FORMATETC FORMATETC_fromcom(MemorySegment mem) {return(new FORMATETC(mem.reinterpret(_FORMATETC.byteSize())));}

	static final StructLayout _STGMEDIUM = struct(new MemoryLayout[] {
		DWORD.withName("tymed"),
		MemoryLayout.unionLayout(new MemoryLayout[] {
		    HANDLE.withName("handle"),
		    ADDRESS.withName("address"),
		}).withName("data"),
		ADDRESS.withName("pUnkForRelease"),
	    });
	public static class STGMEDIUM extends COM.STGMEDIUM {
	    private STGMEDIUM(MemorySegment mem) {
		super(mem);
	    }
	    private STGMEDIUM(Arena alloc) {
		this(alloc.allocate(_STGMEDIUM.byteSize()));
	    }
	    private STGMEDIUM() {
		this(Arena.ofAuto());
	    }

	    protected StructLayout $layout() {return(_STGMEDIUM);}

	    private static final VarHandle tymed = _STGMEDIUM.varHandle(PathElement.groupElement("tymed"));
	    public STGMEDIUM tymed(int value) {tymed.set(mem, 0, value); return(this);}
	    public int tymed() {return((int)tymed.get(mem, 0));}
	    private static final VarHandle handle = _STGMEDIUM.varHandle(PathElement.groupElement("data"), PathElement.groupElement("handle"));
	    public STGMEDIUM handle(Handle value) {handle.set(mem, 0, (value == null) ? MemorySegment.NULL : value.bits); return(this);}
	    public Handle handle() {return(Handle.of((MemorySegment)handle.get(mem, 0)));}
	    private static final VarHandle address = _STGMEDIUM.varHandle(PathElement.groupElement("data"), PathElement.groupElement("address"));
	    STGMEDIUM address(MemorySegment value) {address.set(mem, 0, (value == null) ? MemorySegment.NULL : value); return(this);}
	    MemorySegment address() {return((MemorySegment)handle.get(mem, 0));}
	}
	public MemoryLayout C_STGMEDIUM() {return(_STGMEDIUM);}
	public STGMEDIUM STGMEDIUM() {return(new STGMEDIUM());}

	private final MethodHandle ReleaseStgMedium = ld.downcallHandle(ole32.find("ReleaseStgMedium").get(), FunctionDescriptor.ofVoid(ADDRESS));
	void ReleaseStgMedium(MemorySegment sm) {
	    try {
		RevokeDragDrop.invoke(sm);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	{
	    Win32 win = Win32.get();
	    valxf(new ValueConversion(Win32.Win64Unicode._POINT, Coord.class,
				      (st, v) -> win.POINT(st).c((Coord)v),
				      null,
				      v -> win.POINT((MemorySegment)v).c(),
				      null));
	    valxf(new ValueConversion(_STGMEDIUM, COM.STGMEDIUM.class,
				      null,
				      buf -> {
					  STGMEDIUM ret = STGMEDIUM();
					  MemorySegment mem = ret.mem();
					  MemorySegment.copy(buf, 0, mem, 0, ret.$layout().byteSize());
					  Finalizer.finalize(ret, () -> ReleaseStgMedium(mem));
					  return(ret);
				      },
				      null,
				      (v, buf) -> {
					  MemorySegment.copy(((STGMEDIUM)v).mem(), 0, buf, 0, _STGMEDIUM.byteSize());
				      }));
	}
    }

    private static COM instance = null;
    public static COM get() {
	if(instance == null) {
	    synchronized(COM.class) {
		if(instance == null)
		    instance = tryload("COM", Win64Unicode::new);
	    }
	}
	return(instance);
    }

    public static void main(String[] args) throws Exception {
	COM com = COM.get();
	com.CoInitializeEx(COINIT_APARTMENTTHREADED | COINIT_DISABLE_OLE1DDE);
	IFileDialog fd = com.CoCreateInstance(IFileDialog.CLSID_FileOpenDialog, null, CLSCTX_INPROC_SERVER, IFileDialog.IID);
	fd.SetFileTypes(new String[][] {{"All files", "*.*"}});
	int cookie = fd.Advise(new IFileDialogEvents() {
	    public void OnFolderChanging(IFileDialog fd, IShellItem folder) {
		Debug.dump(fd, folder.GetDisplayName(IShellItem.SIGDN_FILESYSPATH));
	    }
	});
	fd.Show(null);
	IShellItem ret = fd.GetResult();
	Debug.dump(ret.GetDisplayName(IShellItem.SIGDN_NORMALDISPLAY));
	Debug.dump(ret.GetDisplayName(IShellItem.SIGDN_PARENTRELATIVEPARSING));
	Debug.dump(ret.GetDisplayName(IShellItem.SIGDN_DESKTOPABSOLUTEPARSING));
	Debug.dump(ret.GetDisplayName(IShellItem.SIGDN_PARENTRELATIVEEDITING));
	Debug.dump(ret.GetDisplayName(IShellItem.SIGDN_DESKTOPABSOLUTEEDITING));
	Debug.dump(ret.GetDisplayName(IShellItem.SIGDN_FILESYSPATH));
	Debug.dump(ret.GetDisplayName(IShellItem.SIGDN_URL));
	Debug.dump(ret.GetDisplayName(IShellItem.SIGDN_PARENTRELATIVEFORADDRESSBAR));
	Debug.dump(ret.GetDisplayName(IShellItem.SIGDN_PARENTRELATIVE));
	Debug.dump(ret.GetDisplayName(IShellItem.SIGDN_PARENTRELATIVEFORUI));
	fd = null;
	for(int i = 0; i < 100; i++) {
	    System.gc();
	    Thread.sleep(100);
	}
    }
}
