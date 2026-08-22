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
import haven.ffi.objc.Runtime.*;
import java.lang.invoke.*;
import java.lang.foreign.*;
import java.lang.foreign.MemoryLayout.PathElement;
import java.util.*;
import static haven.ffi.ABI.*;
import static haven.ffi.FUtils.*;
import static java.lang.foreign.ValueLayout.ADDRESS;

public abstract class CoreGraphics {
    public static final int kCGMouseEventDeltaX = 4;
    public static final int kCGMouseEventDeltaY = 5;
    public static final int kCGScrollWheelEventDeltaAxis1 = 11;
    public static final int kCGScrollWheelEventDeltaAxis2 = 12;
    public static final int kCGScrollWheelEventFixedPtDeltaAxis1 = 93;
    public static final int kCGScrollWheelEventFixedPtDeltaAxis2 = 94;
    public static final int kCGScrollWheelEventPointDeltaAxis1 = 96;
    public static final int kCGScrollWheelEventPointDeltaAxis2 = 97;

    public static interface CGPoint {
	MemorySegment mem();

	public double x();
	public CGPoint x(double val);
	public double y();
	public CGPoint y(double val);

	public default Coord c() {return(Coord.of((int)x(), (int)y()));}
	public default CGPoint c(Coord c) {x(c.x); y(c.y); return(this);}
    }

    public abstract MemoryLayout C_CGPoint();
    public abstract CGPoint CGPoint(MemorySegment mem);
    public abstract CGPoint CGPoint(Arena alloc);
    public abstract CGPoint CGPoint();
    public CGPoint CGPoint(Coord c) {return(CGPoint().c(c));}

    public static interface CGSize {
	MemorySegment mem();

	public double width();
	public CGSize width(double val);
	public double height();
	public CGSize height(double val);

	public default Coord c() {return(Coord.of((int)width(), (int)height()));}
	public default CGSize c(Coord c) {width(c.x); height(c.y); return(this);}
    }

    public abstract MemoryLayout C_CGSize();
    public abstract CGSize CGSize(MemorySegment mem);
    public abstract CGSize CGSize(Arena alloc);
    public abstract CGSize CGSize();
    public CGSize CGSize(Coord c) {return(CGSize().c(c));}

    public static interface CGRect {
	MemorySegment mem();

	public CGPoint origin();
	public CGSize size();

	public default Area a() {return(Area.sized(origin().c(), size().c()));}
	public default CGRect a(Area a) {origin().c(a.ul); size().c(a.sz()); return(this);}
    }

    public abstract MemoryLayout C_CGRect();
    public abstract CGRect CGRect(MemorySegment mem);
    public abstract CGRect CGRect();
    public CGRect CGRect(Area a) {return(CGRect().a(a));}

    public static interface CGEvent {
	public double getDoubleValueField(int field);
	public long getIntegerValueField(int field);
    }
    abstract CGEvent CGEvent(MemorySegment ref);

    public abstract CGSize CGDisplayScreenSize(int display);
    public abstract long CGDisplayPixelsWide(int display);
    public abstract long CGDisplayPixelsHigh(int display);

    public abstract void objc_msgSend_void(Runtime.ID self, Runtime.SEL sel, CGPoint rect);
    public abstract ID objc_msgSend_id(Runtime.ID self, Runtime.SEL sel, ID arg1, CGPoint arg2);
    public abstract CGPoint objc_msgSend_CGPoint(Runtime.ID self, Runtime.SEL sel);
    public abstract CGPoint objc_msgSend_CGPoint(Runtime.ID self, Runtime.SEL sel, CGPoint rect);
    public abstract void objc_msgSend_void(Runtime.ID self, Runtime.SEL sel, CGSize rect);
    public abstract ID objc_msgSend_id(Runtime.ID self, Runtime.SEL sel, CoreGraphics.CGSize rect);
    public abstract CGSize objc_msgSend_CGSize(Runtime.ID self, Runtime.SEL sel);
    public abstract CGSize objc_msgSend_CGSize(Runtime.ID self, Runtime.SEL sel, CGSize rect);
    public abstract void objc_msgSend_void(Runtime.ID self, Runtime.SEL sel, CGRect rect);
    public abstract void objc_msgSend_void(Runtime.ID self, Runtime.SEL sel, CGRect arg1, Runtime.ID arg2);
    public abstract CGRect objc_msgSend_CGRect(Runtime.ID self, Runtime.SEL sel);
    public abstract CGRect objc_msgSend_CGRect(Runtime.ID self, Runtime.SEL sel, CGRect rect);

    static class VersionA extends CoreGraphics {
	static final MemoryLayout CGFloat = C_DOUBLE;
	static final MemoryLayout CGDirectDisplayID = ValueLayout.JAVA_INT;
	static final MemoryLayout CGEventField = ValueLayout.JAVA_INT;
	private final SymbolLookup dylib = SymbolLookup.libraryLookup("/System/Library/Frameworks/CoreGraphics.framework/CoreGraphics", Arena.global());
	private final Runtime rt = Runtime.get();
	private final CoreFoundation cf = CoreFoundation.get();

	static final StructLayout _CGPoint = struct(new MemoryLayout[] {
		CGFloat.withName("x"),
		CGFloat.withName("y"),
	    });
	static class CGPoint extends StructInstance implements CoreGraphics.CGPoint {
	    CGPoint(MemorySegment mem) {
		super(mem);
	    }
	    CGPoint(Arena alloc) {
		this(alloc.allocate(_CGPoint));
	    }
	    CGPoint() {
		this(Arena.ofAuto());
	    }

	    protected StructLayout $layout() {return(_CGPoint);}
	    public MemorySegment mem() {return(mem);}

	    private static final VarHandle x = _CGPoint.varHandle(PathElement.groupElement("x"));
	    public double x() {return((double)x.get(mem, 0));}
	    public CGPoint x(double val) {x.set(mem, 0, (float)val); return(this);}
	    private static final VarHandle y = _CGPoint.varHandle(PathElement.groupElement("y"));
	    public double y() {return((double)y.get(mem, 0));}
	    public CGPoint y(double val) {y.set(mem, 0, (float)val); return(this);}
	}
	public MemoryLayout C_CGPoint() {return(_CGPoint);}
	public CGPoint CGPoint(MemorySegment mem) {return(new CGPoint(mem));}
	public CGPoint CGPoint(Arena alloc) {return(new CGPoint(alloc));}
	public CGPoint CGPoint() {return(new CGPoint());}

	static final StructLayout _CGSize = struct(new MemoryLayout[] {
		CGFloat.withName("width"),
		CGFloat.withName("height"),
	    });
	static class CGSize extends StructInstance implements CoreGraphics.CGSize {
	    CGSize(MemorySegment mem) {
		super(mem);
	    }
	    CGSize(Arena alloc) {
		this(alloc.allocate(_CGSize));
	    }
	    CGSize() {
		this(Arena.ofAuto());
	    }

	    protected StructLayout $layout() {return(_CGSize);}
	    public MemorySegment mem() {return(mem);}

	    private static final VarHandle width = _CGSize.varHandle(PathElement.groupElement("width"));
	    public double width() {return((double)width.get(mem, 0));}
	    public CGSize width(double val) {width.set(mem, 0, (float)val); return(this);}
	    private static final VarHandle height = _CGSize.varHandle(PathElement.groupElement("height"));
	    public double height() {return((double)height.get(mem, 0));}
	    public CGSize height(double val) {height.set(mem, 0, (float)val); return(this);}
	}
	public MemoryLayout C_CGSize() {return(_CGSize);}
	public CGSize CGSize(MemorySegment mem) {return(new CGSize(mem));}
	public CGSize CGSize(Arena alloc) {return(new CGSize(alloc));}
	public CGSize CGSize() {return(new CGSize());}

	static final StructLayout _CGRect = struct(new MemoryLayout[] {
		_CGPoint.withName("origin"),
		_CGSize.withName("size"),
	    });
	static class CGRect extends StructInstance implements CoreGraphics.CGRect {
	    CGRect(MemorySegment mem) {
		super(mem);
	    }
	    CGRect(Arena alloc) {
		this(alloc.allocate(_CGRect));
	    }
	    CGRect() {
		this(Arena.ofAuto());
	    }

	    protected StructLayout $layout() {return(_CGRect);}
	    public MemorySegment mem() {return(mem);}

	    private static final long origin = _CGRect.byteOffset(PathElement.groupElement("origin"));
	    public CGPoint origin() {return(new CGPoint(mem.asSlice(origin, _CGPoint)));}
	    private static final long size = _CGRect.byteOffset(PathElement.groupElement("size"));
	    public CGSize size() {return(new CGSize(mem.asSlice(size, _CGSize)));}
	}
	public MemoryLayout C_CGRect() {return(_CGRect);}
	public CGRect CGRect(MemorySegment mem) {return(new CGRect(mem));}
	public CGRect CGRect() {return(new CGRect());}

	private final MethodHandle CGEventGetDoubleValueField = ld.downcallHandle(dylib.find("CGEventGetDoubleValueField").get(), FunctionDescriptor.of(C_DOUBLE, ADDRESS, CGEventField));
	private final MethodHandle CGEventGetIntegerValueField = ld.downcallHandle(dylib.find("CGEventGetIntegerValueField").get(), FunctionDescriptor.of(ValueLayout.JAVA_LONG, ADDRESS, CGEventField));
	class CGEvent implements CoreGraphics.CGEvent {
	    final MemorySegment ref;

	    CGEvent(MemorySegment ref, boolean release) {
		this.ref = ref;
		if(release)
		    cf.gcrelease(this, ref);
	    }

	    public double getDoubleValueField(int field) {
		try {
		    return((double)CGEventGetDoubleValueField.invoke(ref, field));
		} catch(Throwable e) {throw(new RuntimeException(e));}
	    }
	    public long getIntegerValueField(int field) {
		try {
		    return((long)CGEventGetIntegerValueField.invoke(ref, field));
		} catch(Throwable e) {throw(new RuntimeException(e));}
	    }
	}

	CGEvent CGEvent(MemorySegment ref) {
	    return(new CGEvent(cf.CFRetain(ref), true));
	}

	private final MethodHandle CGDisplayScreenSize = ld.downcallHandle(dylib.find("CGDisplayScreenSize").get(), FunctionDescriptor.of(_CGSize, CGDirectDisplayID));
	public CGSize CGDisplayScreenSize(int display) {
	    try {
		return(CGSize((MemorySegment)CGDisplayScreenSize.invoke(Arena.ofAuto(), display)));
	    } catch(Throwable e) {throw(new RuntimeException(e));}
	}

	private final MethodHandle CGDisplayPixelsWide = ld.downcallHandle(dylib.find("CGDisplayPixelsWide").get(), FunctionDescriptor.of(SIZE_T, CGDirectDisplayID));
	public long CGDisplayPixelsWide(int display) {
	    try {
		return((long)CGDisplayPixelsWide.invoke(display));
	    } catch(Throwable e) {throw(new RuntimeException(e));}
	}

	private final MethodHandle CGDisplayPixelsHigh = ld.downcallHandle(dylib.find("CGDisplayPixelsHigh").get(), FunctionDescriptor.of(SIZE_T, CGDirectDisplayID));
	public long CGDisplayPixelsHigh(int display) {
	    try {
		return((long)CGDisplayPixelsHigh.invoke(display));
	    } catch(Throwable e) {throw(new RuntimeException(e));}
	}

	private final MethodHandle objc_msgSend_void_CGPoint = rt.msgtype(null, _CGPoint);
	public void objc_msgSend_void(Runtime.ID self, Runtime.SEL sel, CoreGraphics.CGPoint rect) {
	    try {
		objc_msgSend_void_CGPoint.invoke(self.mem(), sel.mem(), rect.mem());
	    } catch(Throwable e) {throw(new RuntimeException(e));}
	}
	private final MethodHandle objc_msgSend_id_id_CGPoint = rt.msgtype(rt.C_ID(), rt.C_ID(), _CGPoint);
	public ID objc_msgSend_id(Runtime.ID self, Runtime.SEL sel, ID arg1, CoreGraphics.CGPoint arg2) {
	    try {
		return(rt.id((MemorySegment)objc_msgSend_id_id_CGPoint.invoke(self.mem(), sel.mem(), arg1.mem(), arg2.mem())));
	    } catch(Throwable e) {throw(new RuntimeException(e));}
	}
	private final MethodHandle objc_msgSend_CGPoint = rt.msgtype(_CGPoint);
	public CGPoint objc_msgSend_CGPoint(Runtime.ID self, Runtime.SEL sel) {
	    try {
		return(CGPoint((MemorySegment)objc_msgSend_CGPoint.invoke(Arena.ofAuto(), self.mem(), sel.mem())));
	    } catch(Throwable e) {throw(new RuntimeException(e));}
	}
	private final MethodHandle objc_msgSend_CGPoint_CGPoint = rt.msgtype(_CGPoint, _CGPoint);
	public CGPoint objc_msgSend_CGPoint(Runtime.ID self, Runtime.SEL sel, CoreGraphics.CGPoint rect) {
	    try {
		return(CGPoint((MemorySegment)objc_msgSend_CGPoint_CGPoint.invoke(Arena.ofAuto(), self.mem(), sel.mem(), rect.mem())));
	    } catch(Throwable e) {throw(new RuntimeException(e));}
	}

	private final MethodHandle objc_msgSend_void_CGSize = rt.msgtype(null, _CGSize);
	public void objc_msgSend_void(Runtime.ID self, Runtime.SEL sel, CoreGraphics.CGSize rect) {
	    try {
		objc_msgSend_void_CGSize.invoke(self.mem(), sel.mem(), rect.mem());
	    } catch(Throwable e) {throw(new RuntimeException(e));}
	}
	private final MethodHandle objc_msgSend_id_CGSize = rt.msgtype(rt.C_ID(), _CGSize);
	public ID objc_msgSend_id(Runtime.ID self, Runtime.SEL sel, CoreGraphics.CGSize rect) {
	    try {
		return(rt.id((MemorySegment)objc_msgSend_id_CGSize.invoke(self.mem(), sel.mem(), rect.mem())));
	    } catch(Throwable e) {throw(new RuntimeException(e));}
	}
	private final MethodHandle objc_msgSend_CGSize = rt.msgtype(_CGSize);
	public CGSize objc_msgSend_CGSize(Runtime.ID self, Runtime.SEL sel) {
	    try {
		return(CGSize((MemorySegment)objc_msgSend_CGSize.invoke(Arena.ofAuto(), self.mem(), sel.mem())));
	    } catch(Throwable e) {throw(new RuntimeException(e));}
	}
	private final MethodHandle objc_msgSend_CGSize_CGSize = rt.msgtype(_CGSize, _CGSize);
	public CGSize objc_msgSend_CGSize(Runtime.ID self, Runtime.SEL sel, CoreGraphics.CGSize rect) {
	    try {
		return(CGSize((MemorySegment)objc_msgSend_CGSize_CGSize.invoke(Arena.ofAuto(), self.mem(), sel.mem(), rect.mem())));
	    } catch(Throwable e) {throw(new RuntimeException(e));}
	}

	private final MethodHandle objc_msgSend_void_CGRect = rt.msgtype(null, _CGRect);
	public void objc_msgSend_void(Runtime.ID self, Runtime.SEL sel, CoreGraphics.CGRect rect) {
	    try {
		objc_msgSend_void_CGRect.invoke(self.mem(), sel.mem(), rect.mem());
	    } catch(Throwable e) {throw(new RuntimeException(e));}
	}
	private final MethodHandle objc_msgSend_void_CGRect_id = rt.msgtype(null, _CGRect, rt.C_ID());
	public void objc_msgSend_void(Runtime.ID self, Runtime.SEL sel, CoreGraphics.CGRect arg1, Runtime.ID arg2) {
	    try {
		objc_msgSend_void_CGRect_id.invoke(self.mem(), sel.mem(), arg1.mem(), arg2.mem());
	    } catch(Throwable e) {throw(new RuntimeException(e));}
	}
	private final MethodHandle objc_msgSend_CGRect = rt.msgtype(_CGRect);
	public CGRect objc_msgSend_CGRect(Runtime.ID self, Runtime.SEL sel) {
	    try {
		return(CGRect((MemorySegment)objc_msgSend_CGRect.invoke(Arena.ofAuto(), self.mem(), sel.mem())));
	    } catch(Throwable e) {throw(new RuntimeException(e));}
	}
	private final MethodHandle objc_msgSend_CGRect_CGRect = rt.msgtype(_CGRect, _CGRect);
	public CGRect objc_msgSend_CGRect(Runtime.ID self, Runtime.SEL sel, CoreGraphics.CGRect rect) {
	    try {
		return(CGRect((MemorySegment)objc_msgSend_CGRect_CGRect.invoke(Arena.ofAuto(), self.mem(), sel.mem(), rect.mem())));
	    } catch(Throwable e) {throw(new RuntimeException(e));}
	}
    }

    private static CoreGraphics instance = null;
    public static CoreGraphics get() {
	if(instance == null) {
	    synchronized(CoreGraphics.class) {
		if(instance == null) {
		    instance = new VersionA();
		}
	    }
	}
	return(instance);
    }
}
