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

package haven.ffi.objc;

import haven.*;
import haven.ffi.*;
import java.lang.invoke.*;
import java.lang.foreign.*;
import java.lang.foreign.MemoryLayout.PathElement;
import java.util.*;
import java.util.function.*;
import static haven.ffi.ABI.*;
import static haven.ffi.FUtils.*;
import static java.lang.foreign.ValueLayout.ADDRESS;

public abstract class Runtime {
    public static interface Class {
	public ID id();
    }

    public static interface Ivar {
    }

    public static interface SEL {
	MemorySegment mem();
    }

    public static interface ID {
	MemorySegment mem();
    }

    public static interface NSObject {
	public ID id();
    }

    public abstract Class objc_getClass(String name);
    public abstract String object_getClassName(ID id);
    public abstract Ivar class_getInstanceVariable(Runtime.Class cls, String name);
    public abstract String ivar_getName(Runtime.Ivar v);
    public abstract long ivar_getOffset(Runtime.Ivar v);
    public abstract Class objc_allocateClassPair(Class superclass, String name, int extraBytes);
    public abstract void objc_registerClassPair(Class cls);
    public abstract void class_addMethod(Runtime.Class cls, Runtime.SEL name, MemorySegment imp, String types);
    public abstract void class_addIvar(Runtime.Class cls, String name, long size, int alignment, String types);
    public abstract SEL sel_registerName(String name);

    abstract MemoryLayout C_ID();
    abstract MemoryLayout C_SEL();
    abstract ID id(MemorySegment mem);
    abstract SEL sel(MemorySegment mem);
    abstract MethodHandle msgtype(MemoryLayout rtype, MemoryLayout... args);

    public abstract void objc_msgSend_void(ID self, SEL sel);
    public abstract void objc_msgSend_void(ID self, SEL sel, boolean arg1);
    public abstract void objc_msgSend_void(ID self, SEL sel, MemorySegment arg1, int arg2);
    public abstract void objc_msgSend_void(ID self, SEL sel, ID arg1);
    public abstract void objc_msgSend_void(ID self, SEL sel, ID arg1, boolean arg2);
    public abstract void objc_msgSend_void(ID self, SEL sel, SEL arg1, ID arg2, boolean arg3);
    public abstract ID objc_msgSend_id(ID self, SEL sel);
    public abstract ID objc_msgSend_id(ID self, SEL sel, int arg1);
    public abstract ID objc_msgSend_id(ID self, SEL sel, MemorySegment arg1);
    public abstract ID objc_msgSend_id(ID self, SEL sel, MemorySegment arg1, int arg2);
    public abstract ID objc_msgSend_id(ID self, SEL sel, ID arg1);
    public abstract ID objc_msgSend_id(ID self, SEL sel, ID arg1, ID arg2);
    abstract MemorySegment objc_msgSend_ptr(ID self, SEL sel);
    public abstract boolean objc_msgSend_bool(ID self, SEL sel);
    public abstract boolean objc_msgSend_bool(ID self, SEL sel, int arg1);
    public abstract int objc_msgSend_int(ID self, SEL sel);
    public abstract int objc_msgSend_NSUInt(ID self, SEL sel);
    public abstract double objc_msgSend_double(ID self, SEL sel);


    public String object_getClassName(Class cls) {return(object_getClassName(cls.id()));}

    MemorySegment object_getIvar(ID id, Class cls, String name, MemoryLayout as) {
	long off = ivar_getOffset(class_getInstanceVariable(cls, name));
	return(MemorySegment.ofAddress(id.mem().address() + off).reinterpret(as.byteSize()));
    }

    private final Map<Integer, Runnable> maintasks = new HashMap<>();
    private final Arena localarena = Arena.ofAuto();
    private int maintaskid = 0;
    private Class runnable = null;

    private static void mainrun_handle(Runtime rt, MemorySegment objp, MemorySegment sel) {
	try {
	    ID obj = rt.id(objp);
	    int id = rt.object_getIvar(obj, rt.runnable, "task", ValueLayout.JAVA_INT).get(ValueLayout.JAVA_INT, 0);
	    Runnable task;
	    synchronized(rt.maintasks) {
		task = rt.maintasks.remove(id);
	    }
	    task.run();
	} catch(Throwable t) {
	    Thread.UncaughtExceptionHandler h = Thread.currentThread().getUncaughtExceptionHandler();
	    if(h == null)
		new Warning(t, "Uncaught exception in maintask").issue();
	    else
		h.uncaughtException(Thread.currentThread(), t);
	}
    }

    public void mainrun(Runnable task) {
	synchronized(maintasks) {
	    if(runnable == null) {
		runnable = objc_allocateClassPair(objc_getClass("NSObject"), "IOSYSRunnable", 0);
		class_addIvar(runnable, "task", 4, 2, "i");
		class_addMethod(runnable, sel_registerName("IOSYSCallback"), 
				ld.upcallStub(MethodHandles.insertArguments(slookup(MethodHandles.lookup(), Runtime.class, "mainrun_handle",
										    Void.TYPE, Runtime.class, MemorySegment.class, MemorySegment.class),
									    0, this),
					      FunctionDescriptor.ofVoid(C_ID(), C_SEL()), localarena),
				"v@:");
		objc_registerClassPair(runnable);
	    }
	    int id = maintaskid++;
	    maintasks.put(id, task);
	    ID obj = objc_msgSend_id(runnable.id(), sel_registerName("alloc"));
	    object_getIvar(obj, runnable, "task", ValueLayout.JAVA_INT).set(ValueLayout.JAVA_INT, 0, id);
	    objc_msgSend_void(obj, sel_registerName("performSelectorOnMainThread:withObject:waitUntilDone:"),
			      sel_registerName("IOSYSCallback"), null, false);
	}
    }

    private final Supplier<SEL> sel_retain = Utils.cache(() -> sel_registerName("retain"));
    private final Supplier<SEL> sel_release = Utils.cache(() -> sel_registerName("release"));
    public void release(ID id) {
	objc_msgSend_void(id, sel_release.get());
    }
    public void gcrelease(Object obj, ID id) {
	if(id == null)
	    throw(new NullPointerException());
	Finalizer.finalize(obj, () -> release(id));
    }
    public void gcrelease(NSObject obj) {
	gcrelease(obj, obj.id());
    }
    public <T extends NSObject> T retain(T obj) {
	objc_msgSend_id(obj.id(), sel_retain.get());
	gcrelease(obj);
	return(obj);
    }

    static class objc4 extends Runtime {
	static final MemoryLayout C_Class = ADDRESS;
	static final MemoryLayout C_SEL = ADDRESS;
	static final MemoryLayout C_ID = ADDRESS;
	static final MemoryLayout C_Ivar = ADDRESS;
	static final MemoryLayout OC_BOOL = C_CHAR;
	static final MemoryLayout NSInteger = C_LONG;
	static final MemoryLayout NSUInteger = C_LONG;
	private final SymbolLookup rt = SymbolLookup.libraryLookup("libobjc.A.dylib", Arena.global());

	objc4() {
	    /* x86-64 support would require eg. snd_msgSend_stret and such */
	    if(!System.getProperty("os.arch").equals("aarch64"))
		throw(new RuntimeException("This ObjC runtime is only implemented for AArch64"));
	}

	MemoryLayout C_ID() {return(C_ID);}
	MemoryLayout C_SEL() {return(C_SEL);}

	private static MemorySegment nid(Runtime.ID id) {
	    return((id == null) ? MemorySegment.NULL : id.mem());
	}

	static final StructLayout _Class = struct(new MemoryLayout[] {
		C_Class.withName("isa"),
	    });
	static class Class extends StructInstance implements Runtime.Class {
	    public final objc4 lib;

	    Class(objc4 lib, MemorySegment mem) {
		super(mem);
		this.lib = lib;
	    }

	    protected StructLayout $layout() {return(_Class);}
	    MemorySegment mem() {return(mem);}

	    private final ID id = new ID(mem);
	    public ID id() {return(id);}

	    public String toString() {
		return(lib.object_getClassName(this));
	    }
	}

	static final StructLayout _Ivar = struct(new MemoryLayout[] {
	    });
	static class Ivar extends StructInstance implements Runtime.Ivar {
	    public final objc4 lib;

	    Ivar(objc4 lib, MemorySegment mem) {
		super(mem);
		this.lib = lib;
	    }

	    protected StructLayout $layout() {return(_Ivar);}
	    public MemorySegment mem() {return(mem);}

	    public String toString() {
		return("\"" + lib.ivar_getName(this) + "\"");
	    }
	}

	static final StructLayout _SEL = struct(new MemoryLayout[] {
	    });
	static class SEL extends StructInstance implements Runtime.SEL {
	    public final objc4 lib;

	    SEL(objc4 lib, MemorySegment mem) {
		super(mem);
		this.lib = lib;
	    }

	    static SEL of(objc4 lib, MemorySegment mem) {
		return(nullp(mem) ? null : new SEL(lib, mem));
	    }

	    protected StructLayout $layout() {return(_SEL);}
	    public MemorySegment mem() {return(mem);}

	    public boolean equals(SEL that) {return(this.mem.equals(that.mem));}
	    public boolean equals(Object x) {return((x instanceof SEL) && equals((SEL)x));}
	    public int hashCode() {return(Long.hashCode(mem.address()));}

	    public String toString() {
		return("\"" + lib.sel_getName(this) + "\"");
	    }
	}
	SEL sel(MemorySegment mem) {return(SEL.of(this, mem));}

	static final StructLayout _ID = struct(new MemoryLayout[] {
		C_Class.withName("isa"),
	    });
	static class ID extends StructInstance implements Runtime.ID {
	    ID(MemorySegment mem) {
		super(mem);
	    }

	    static ID of(MemorySegment mem) {
		return(nullp(mem) ? null : new ID(mem));
	    }

	    protected StructLayout $layout() {return(_ID);}
	    public MemorySegment mem() {return(mem);}

	    public String toString() {
		return("$" + Long.toUnsignedString(mem.address()));
	    }
	}
	ID id(MemorySegment mem) {return(ID.of(mem));}

	private final MethodHandle objc_getClass = ld.downcallHandle(rt.find("objc_getClass").get(), FunctionDescriptor.of(C_Class, ADDRESS));
	public Class objc_getClass(String name) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment rv;
		try {
		    rv = (MemorySegment)objc_getClass.invoke(st.allocateFrom(name, Utils.utf8));
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		}
		return(nullp(rv) ? null : new Class(this, rv));
	    }
	}

	private final MethodHandle object_getClassName = ld.downcallHandle(rt.find("object_getClassName").get(), FunctionDescriptor.of(ADDRESS, C_ID));
	public String object_getClassName(Runtime.ID id) {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)object_getClassName.invoke(((ID)id).mem());
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	    return(nullp(rv) ? null : rv.reinterpret(Long.MAX_VALUE).getString(0, Utils.utf8));
	}

	private final MethodHandle class_getInstanceVariable = ld.downcallHandle(rt.find("class_getInstanceVariable").get(), FunctionDescriptor.of(C_Ivar, C_Class, ADDRESS));
	public Ivar class_getInstanceVariable(Runtime.Class cls, String name) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment rv;
		try {
		    rv = (MemorySegment)class_getInstanceVariable.invoke(((Class)cls).mem(), st.allocateFrom(name, Utils.utf8));
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		}
		return(nullp(rv) ? null : new Ivar(this, rv));
	    }
	}

	private final MethodHandle ivar_getName = ld.downcallHandle(rt.find("ivar_getName").get(), FunctionDescriptor.of(ADDRESS, C_Ivar));
	public String ivar_getName(Runtime.Ivar v) {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)ivar_getName.invoke(((Ivar)v).mem());
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	    return(nullp(rv) ? null : rv.reinterpret(Long.MAX_VALUE).getString(0, Utils.utf8));
	}

	private final MethodHandle ivar_getOffset = ld.downcallHandle(rt.find("ivar_getOffset").get(), FunctionDescriptor.of(PTRINT_T, C_Ivar));
	public long ivar_getOffset(Runtime.Ivar v) {
	    long rv;
	    try {
		rv = (long)ivar_getOffset.invoke(((Ivar)v).mem());
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	    return(rv);
	}

	private final MethodHandle objc_allocateClassPair = ld.downcallHandle(rt.find("objc_allocateClassPair").get(), FunctionDescriptor.of(C_Class, C_Class, ADDRESS, SIZE_T));
	public Class objc_allocateClassPair(Runtime.Class superclass, String name, int extraBytes) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment rv;
		try {
		    rv = (MemorySegment)objc_allocateClassPair.invoke(((Class)superclass).mem(), st.allocateFrom(name, Utils.utf8), extraBytes);
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		}
		if(nullp(rv))
		    throw(new RuntimeException("failed to allocate class pair: " + name));
		return(new Class(this, rv));
	    }
	}

	private final MethodHandle objc_registerClassPair = ld.downcallHandle(rt.find("objc_registerClassPair").get(), FunctionDescriptor.ofVoid(C_Class));
	public void objc_registerClassPair(Runtime.Class cls) {
	    try {
		objc_registerClassPair.invoke(((Class)cls).mem());
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle class_addIvar = ld.downcallHandle(rt.find("class_addIvar").get(), FunctionDescriptor.of(OC_BOOL, C_Class, ADDRESS, SIZE_T, ValueLayout.JAVA_BYTE, ADDRESS));
	public void class_addIvar(Runtime.Class cls, String name, long size, int alignment, String types) {
	    try(Arena st = Arena.ofConfined()) {
		int rv;
		try {
		    rv = (int)class_addIvar.invoke(((Class)cls).mem(), st.allocateFrom(name, Utils.utf8), size, (byte)alignment, st.allocateFrom(types, Utils.utf8));
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		}
		if(rv == 0)
		    throw(new RuntimeException("failed to add ivar"));
	    }
	}

	private final MethodHandle class_addMethod = ld.downcallHandle(rt.find("class_addMethod").get(), FunctionDescriptor.of(OC_BOOL, C_Class, C_SEL, ADDRESS, ADDRESS));
	public void class_addMethod(Runtime.Class cls, Runtime.SEL name, MemorySegment imp, String types) {
	    try(Arena st = Arena.ofConfined()) {
		int rv;
		try {
		    rv = (int)class_addMethod.invoke(((Class)cls).mem(), ((SEL)name).mem(), imp, st.allocateFrom(types, Utils.utf8));
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		}
		if(rv == 0)
		    throw(new RuntimeException("failed to add method"));
	    }
	}

	private final MethodHandle sel_registerName = ld.downcallHandle(rt.find("sel_registerName").get(), FunctionDescriptor.of(C_SEL, ADDRESS));
	public SEL sel_registerName(String name) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment rv;
		try {
		    rv = (MemorySegment)sel_registerName.invoke(st.allocateFrom(name, Utils.utf8));
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		}
		return(nullp(rv) ? null : new SEL(this, rv));
	    }
	}

	private final MethodHandle sel_getName = ld.downcallHandle(rt.find("sel_getName").get(), FunctionDescriptor.of(ADDRESS, C_SEL));
	public String sel_getName(SEL sel) {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)sel_getName.invoke(sel.mem());
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	    return(nullp(rv) ? null : rv.reinterpret(Long.MAX_VALUE).getString(0, Utils.utf8));
	}

	private final MemorySegment objc_msgSend = rt.find("objc_msgSend").get();

	MethodHandle msgtype(MemoryLayout rtype, MemoryLayout... args) {
	    MemoryLayout[] fargs = new MemoryLayout[args.length + 2];
	    fargs[0] = C_ID;
	    fargs[1] = C_SEL;
	    System.arraycopy(args, 0, fargs, 2, args.length);
	    FunctionDescriptor desc = (rtype == null) ? FunctionDescriptor.ofVoid(fargs) : FunctionDescriptor.of(rtype, fargs);
	    return(ld.downcallHandle(objc_msgSend, desc));
	}

	private final MethodHandle objc_msgSend_void = msgtype(null);
	public void objc_msgSend_void(Runtime.ID self, Runtime.SEL sel) {
	    try {
		objc_msgSend_void.invoke(self.mem(), sel.mem());
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle objc_msgSend_void_bool = msgtype(null, OC_BOOL);
	public void objc_msgSend_void(Runtime.ID self, Runtime.SEL sel, boolean arg1) {
	    try {
		objc_msgSend_void_bool.invoke(self.mem(), sel.mem(), arg1 ? (byte)1 : (byte)0);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle objc_msgSend_void_ptr_int = msgtype(null, ADDRESS, C_INT);
	public void objc_msgSend_void(Runtime.ID self, Runtime.SEL sel, MemorySegment arg1, int arg2) {
	    try {
		objc_msgSend_void_ptr_int.invoke(self.mem(), sel.mem(), arg1, arg2);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle objc_msgSend_void_id = msgtype(null, C_ID);
	public void objc_msgSend_void(Runtime.ID self, Runtime.SEL sel, Runtime.ID arg1) {
	    try {
		objc_msgSend_void_id.invoke(self.mem(), sel.mem(), nid(arg1));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle objc_msgSend_void_id_bool = msgtype(null, C_ID, OC_BOOL);
	public void objc_msgSend_void(Runtime.ID self, Runtime.SEL sel, Runtime.ID arg1, boolean arg2) {
	    try {
		objc_msgSend_void_id_bool.invoke(self.mem(), sel.mem(), nid(arg1), arg2 ? (byte)1 : (byte)0);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle objc_msgSend_void_SEL_id_bool = msgtype(null, C_SEL, C_ID, OC_BOOL);
	public void objc_msgSend_void(Runtime.ID self, Runtime.SEL sel, Runtime.SEL arg1, Runtime.ID arg2, boolean arg3) {
	    try {
		objc_msgSend_void_SEL_id_bool.invoke(self.mem(), sel.mem(), ((SEL)arg1).mem(), nid(arg2), arg3 ? (byte)1 : (byte)0);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle objc_msgSend_id = msgtype(C_ID);
	public ID objc_msgSend_id(Runtime.ID self, Runtime.SEL sel) {
	    try {
		return(id((MemorySegment)objc_msgSend_id.invoke(self.mem(), sel.mem())));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle objc_msgSend_id_NSUInt = msgtype(C_ID, NSUInteger);
	public ID objc_msgSend_id(Runtime.ID self, Runtime.SEL sel, int arg1) {
	    try {
		return(id((MemorySegment)objc_msgSend_id_NSUInt.invoke(self.mem(), sel.mem(), arg1)));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle objc_msgSend_id_ptr = msgtype(C_ID, ADDRESS);
	public ID objc_msgSend_id(Runtime.ID self, Runtime.SEL sel, MemorySegment arg1) {
	    try {
		return(id((MemorySegment)objc_msgSend_id_ptr.invoke(self.mem(), sel.mem(), arg1)));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle objc_msgSend_id_ptr_NSUInt = msgtype(C_ID, ADDRESS, NSUInteger);
	public ID objc_msgSend_id(Runtime.ID self, Runtime.SEL sel, MemorySegment arg1, int arg2) {
	    try {
		return(id((MemorySegment)objc_msgSend_id_ptr_NSUInt.invoke(self.mem(), sel.mem(), arg1, arg2)));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle objc_msgSend_id_id = msgtype(C_ID, C_ID);
	public ID objc_msgSend_id(Runtime.ID self, Runtime.SEL sel, Runtime.ID arg1) {
	    try {
		return(id((MemorySegment)objc_msgSend_id_id.invoke(self.mem(), sel.mem(), nid(arg1))));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle objc_msgSend_id_id_id = msgtype(C_ID, C_ID, C_ID);
	public ID objc_msgSend_id(Runtime.ID self, Runtime.SEL sel, Runtime.ID arg1, Runtime.ID arg2) {
	    try {
		return(id((MemorySegment)objc_msgSend_id_id_id.invoke(self.mem(), sel.mem(), nid(arg1), nid(arg2))));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle objc_msgSend_ptr = msgtype(ADDRESS);
	MemorySegment objc_msgSend_ptr(Runtime.ID self, Runtime.SEL sel) {
	    try {
		return((MemorySegment)objc_msgSend_id.invoke(self.mem(), sel.mem()));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle objc_msgSend_bool = msgtype(OC_BOOL);
	public boolean objc_msgSend_bool(Runtime.ID self, Runtime.SEL sel) {
	    try {
		return((int)objc_msgSend_bool.invoke(self.mem(), sel.mem()) != 0);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle objc_msgSend_bool_int = msgtype(OC_BOOL, C_INT);
	public boolean objc_msgSend_bool(Runtime.ID self, Runtime.SEL sel, int arg1) {
	    try {
		return((int)objc_msgSend_bool_int.invoke(self.mem(), sel.mem(), arg1) != 0);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle objc_msgSend_int = msgtype(C_INT);
	public int objc_msgSend_int(Runtime.ID self, Runtime.SEL sel) {
	    try {
		return((int)objc_msgSend_int.invoke(self.mem(), sel.mem()));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle objc_msgSend_NSUInt = msgtype(NSUInteger);
	public int objc_msgSend_NSUInt(Runtime.ID self, Runtime.SEL sel) {
	    try {
		return((int)(long)objc_msgSend_NSUInt.invoke(self.mem(), sel.mem()));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle objc_msgSend_double = msgtype(C_DOUBLE);
	public double objc_msgSend_double(Runtime.ID self, Runtime.SEL sel) {
	    try {
		return((double)objc_msgSend_double.invoke(self.mem(), sel.mem()));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}
    }

    private static Runtime instance = null;
    public static Runtime get() {
	if(instance == null) {
	    synchronized(Runtime.class) {
		if(instance == null) {
		    instance = new objc4();
		}
	    }
	}
	return(instance);
    }
}
