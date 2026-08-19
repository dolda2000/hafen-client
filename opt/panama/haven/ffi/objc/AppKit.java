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
import haven.ffi.objc.CoreGraphics.*;
import java.lang.invoke.*;
import java.lang.foreign.*;
import java.util.*;
import java.util.function.*;
import haven.ffi.objc.Runtime.Class;
import java.lang.foreign.MemoryLayout.PathElement;
import static haven.ffi.ABI.*;
import static haven.ffi.FUtils.*;
import static java.lang.foreign.ValueLayout.ADDRESS;

public abstract class AppKit {
    public static final int NSWindowStyleMaskBorderless = 0;
    public static final int NSWindowStyleMaskTitled = 1 << 0;
    public static final int NSWindowStyleMaskClosable = 1 << 1;
    public static final int NSWindowStyleMaskMiniaturizable = 1 << 2;
    public static final int NSWindowStyleMaskResizable = 1 << 3;
    public static final int NSWindowStyleMaskUtilityWindow = 1 << 4;
    public static final int NSWindowStyleMaskDocModalWindow = 1 << 6;
    public static final int NSWindowStyleMaskNonactivatingPanel = 1 << 7;
    public static final int NSWindowStyleMaskUnifiedTitleAndToolbar = 1 << 12;
    public static final int NSWindowStyleMaskHUDWindow = 1 << 13;
    public static final int NSWindowStyleMaskFullScreen = 1 << 14;
    public static final int NSWindowStyleMaskFullSizeContentView = 1 << 15;

    public static final int NSBackingStoreRetained = 0;
    public static final int NSBackingStoreBuffered = 2;

    public interface NSApplication {
	public ID id();
	public void run();
    }

    public interface WindowDelegate {
	public default boolean windowShouldClose(ID sender) {return(true);}
	public default void windowWillClose(ID notification) {}
    }

    public interface NSWindow {
	public ID id();
	public void setDelegate(WindowDelegate delegate);
	public int styleMask();
	public void setStyleMask(int value);
	public void setContentSize(Coord sz);
	public void setContentMinSize(Coord sz);
	public void setContentMaxSize(Coord sz);
	public void makeKeyAndOrderFront(ID sender);
	public void orderOut(ID sender);
	public void setTitle(String title);
	public void center();
	public Coord cascadeTopLeftFromPoint(Coord c);
    }

    public abstract NSApplication NSApplication_sharedApplication();
    public abstract NSWindow NSWindow(Area contentRect, int style, int backingStoreType, boolean defer);

    static class VersionC extends AppKit {
	private static final MemoryLayout C_ID = Runtime.objc4.C_ID;
	private static final MemoryLayout C_SEL = Runtime.objc4.C_SEL;
	private static final MemoryLayout OC_BOOL = Runtime.objc4.OC_BOOL;
	private static final MemoryLayout NSUInteger = C_LONG;
	private final SymbolLookup dylib = SymbolLookup.libraryLookup("/System/Library/Frameworks/AppKit.framework/AppKit", Arena.global());
	private final Arena localarena = Arena.ofAuto();
	final Runtime rt = Runtime.get();
	final CoreGraphics cg = CoreGraphics.get();
	final Foundation fnd = Foundation.get();
	private final SEL sel_alloc = rt.sel_registerName("alloc");
	private final SEL sel_init = rt.sel_registerName("init");

	private final Runtime.Class NSApplication = rt.objc_getClass("NSApplication");
	private final SEL sel_run = rt.sel_registerName("run");
	class NSApplication implements AppKit.NSApplication {
	    public final ID id;

	    public NSApplication(ID id) {
		this.id = id;
	    }

	    public ID id() {return(id);}
	    public void run() {rt.objc_msgSend_void(id, sel_run);}
	}

	private final SEL sel_sharedApplication = rt.sel_registerName("sharedApplication");
	public NSApplication NSApplication_sharedApplication() {
	    return(new NSApplication(rt.objc_msgSend_id(NSApplication.id(), sel_sharedApplication)));
	}

	private Class WindowDelegateAdapter = null;
	class WindowDelegateAdapter {
	    private static final Map<Integer, WindowDelegateAdapter> reg = new HashMap<>();
	    private static int nextkey = 0;
	    public final ID id;
	    public final WindowDelegate callback;

	    public WindowDelegateAdapter(WindowDelegate callback) {
		synchronized(WindowDelegateAdapter.class) {
		    if(WindowDelegateAdapter == null) {
			WindowDelegateAdapter = rt.objc_allocateClassPair(rt.objc_getClass("NSObject"), "IOSYSWindowDelegateAdapter", 0);
			rt.class_addIvar(WindowDelegateAdapter, "java", 4, 2, "i");
			rt.class_addMethod(WindowDelegateAdapter, rt.sel_registerName("windowShouldClose:"),
					   supcall(localarena, MethodHandles.lookup(), WindowDelegateAdapter.class, "windowShouldClose", VersionC.this,
						   OC_BOOL, C_ID, C_SEL, C_ID), "B@:@");
			rt.class_addMethod(WindowDelegateAdapter, rt.sel_registerName("windowWillClose:"),
					   supcall(localarena, MethodHandles.lookup(), WindowDelegateAdapter.class, "windowWillClose", VersionC.this,
						   null, C_ID, C_SEL, C_ID), "B@:@");
		    }
		    int key = nextkey++;
		    ID id = this.id = rt.objc_msgSend_id(rt.objc_msgSend_id(WindowDelegateAdapter.id(), sel_alloc), sel_init);
		    this.callback = callback;
		    reg.put(key, this);
		    Runtime rt = VersionC.this.rt;
		    Finalizer.finalize(this, () -> {
			synchronized(reg) {
			    reg.remove(key);
			    rt.release(id);
			}
		    });
		}
	    }

	    private static <R> R callback(VersionC ak, MemorySegment objp, Function<WindowDelegate, R> fun, R eret) {
		try {
		    Runtime rt = ak.rt;
		    ID obj = rt.id(objp);
		    int key = rt.object_getIvar(obj, ak.WindowDelegateAdapter, "java", ValueLayout.JAVA_INT).get(ValueLayout.JAVA_INT, 0);
		    WindowDelegateAdapter java;
		    synchronized(reg) {
			java = reg.get(key);
		    }
		    return(fun.apply(java.callback));
		} catch(Throwable t) {
		    Thread.UncaughtExceptionHandler h = Thread.currentThread().getUncaughtExceptionHandler();
		    if(h == null)
			new Warning(t, "Uncaught exception in window delegate").issue();
		    else
			h.uncaughtException(Thread.currentThread(), t);
		    return(eret);
		}
	    }

	    private static void callback(VersionC ak, MemorySegment objp, Consumer<WindowDelegate> fun) {
		callback(ak, objp, dlg -> {fun.accept(dlg); return(null);}, null);
	    }

	    private static byte windowShouldClose(VersionC ak, MemorySegment objp, MemorySegment sel, MemorySegment sender) {
		return((byte)callback(ak, objp, dg -> (byte)(dg.windowShouldClose(ak.rt.id(sender)) ? 1 : 0), 0));
	    }

	    private static void windowWillClose(VersionC ak, MemorySegment objp, MemorySegment sel, MemorySegment notification) {
		callback(ak, objp, dg -> dg.windowWillClose(ak.rt.id(notification)));
	    }
	}

	private final Runtime.Class NSWindow = rt.objc_getClass("NSWindow");
	private final SEL sel_setDelegate = rt.sel_registerName("setDelegate:");
	private final SEL sel_styleMask = rt.sel_registerName("styleMask");
	private final SEL sel_setStyleMask = rt.sel_registerName("setStyleMask:");
	private final SEL sel_setContentSize = rt.sel_registerName("setContentSize:");
	private final SEL sel_setContentMinSize = rt.sel_registerName("setContentMinSize:");
	private final SEL sel_setContentMaxSize = rt.sel_registerName("setContentMaxSize:");
	private final SEL sel_makeKeyAndOrderFront = rt.sel_registerName("makeKeyAndOrderFront:");
	private final SEL sel_orderOut = rt.sel_registerName("orderOut:");
	private final SEL sel_setTitle = rt.sel_registerName("setTitle:");
	private final SEL sel_center = rt.sel_registerName("center");
	private final SEL sel_cascadeTopLeftFromPoint = rt.sel_registerName("cascadeTopLeftFromPoint:");
	private final MethodHandle sendmsg_void_CGSize = rt.msgtype(null, cg.C_CGSize());
	private final MethodHandle sendmsg_CGPoint_CGPoint = rt.msgtype(cg.C_CGPoint(), cg.C_CGPoint());
	private final MethodHandle sendmsg_NSUInt = rt.msgtype(NSUInteger);
	private final MethodHandle sendmsg_void_NSUInt = rt.msgtype(null, NSUInteger);
	class NSWindow implements AppKit.NSWindow {
	    public final ID id;

	    public NSWindow(ID id) {
		this.id = id;
	    }

	    public ID id() {return(id);}

	    private WindowDelegateAdapter delegate = null;
	    public void setDelegate(WindowDelegate delegate) {
		this.delegate = new WindowDelegateAdapter(delegate);
		rt.objc_msgSend_void(id, sel_setDelegate, this.delegate.id);
	    }
	    
	    public int styleMask() {
		try {
		    return((int)(long)sendmsg_NSUInt.invoke(id.mem(), sel_styleMask.mem()));
		} catch(Throwable t) {
		    throw(new RuntimeException(t));
		}
	    }
	    public void setStyleMask(int value) {
		try {
		    sendmsg_void_NSUInt.invoke(id.mem(), sel_setStyleMask.mem(), value);
		} catch(Throwable t) {
		    throw(new RuntimeException(t));
		}
	    }
	    public void setContentSize(Coord sz) {
		try(Arena st = Arena.ofConfined()) {
		    CGSize cgsz = cg.CGSize(st);
		    cgsz.width(sz.x).height(sz.y);
		    try {
			sendmsg_void_CGSize.invoke(id.mem(), sel_setContentSize.mem(), cgsz.mem());
		    } catch(Throwable t) {
			throw(new RuntimeException(t));
		    }
		}
	    }
	    public void setContentMinSize(Coord sz) {
		try(Arena st = Arena.ofConfined()) {
		    CGSize cgsz = cg.CGSize(st);
		    cgsz.width(sz.x).height(sz.y);
		    try {
			sendmsg_void_CGSize.invoke(id.mem(), sel_setContentMinSize.mem(), cgsz.mem());
		    } catch(Throwable t) {
			throw(new RuntimeException(t));
		    }
		}
	    }
	    public void setContentMaxSize(Coord sz) {
		try(Arena st = Arena.ofConfined()) {
		    CGSize cgsz = cg.CGSize(st);
		    cgsz.width(sz.x).height(sz.y);
		    try {
			sendmsg_void_CGSize.invoke(id.mem(), sel_setContentMaxSize.mem(), cgsz.mem());
		    } catch(Throwable t) {
			throw(new RuntimeException(t));
		    }
		}
	    }
	    public void makeKeyAndOrderFront(ID sender) {
		rt.objc_msgSend_void(id, sel_makeKeyAndOrderFront, sender);
	    }
	    public void orderOut(ID sender) {
		rt.objc_msgSend_void(id, sel_orderOut, sender);
	    }
	    public void setTitle(String title) {
		rt.objc_msgSend_void(id, sel_setTitle, fnd.NSString(title).id());
	    }
	    public void center() {
		rt.objc_msgSend_void(id, sel_center);
	    }
	    public Coord cascadeTopLeftFromPoint(Coord c) {
		try(Arena st = Arena.ofConfined()) {
		    MemorySegment rv;
		    CGPoint pt = cg.CGPoint(st);
		    pt.x(c.x).y(c.y);
		    try {
			rv = (MemorySegment)sendmsg_CGPoint_CGPoint.invoke(st, id.mem(), sel_cascadeTopLeftFromPoint.mem(),
									   pt.mem());
		    } catch(Throwable t) {
			throw(new RuntimeException(t));
		    }
		    CGPoint rpt = cg.CGPoint(rv);
		    return(Coord.of((int)rpt.x(), (int)rpt.y()));
		}
	    }
	}
	private final SEL sel_initWithContentRect_styleMask_backing_defer = rt.sel_registerName("initWithContentRect:styleMask:backing:defer:");
	private final MethodHandle sendmsg_id_CGRect_int_int_bool = rt.msgtype(C_ID, cg.C_CGRect(), NSUInteger, NSUInteger, OC_BOOL);
	public NSWindow NSWindow(Area contentRect, int style, int backingStoreType, boolean defer) {
	    ID id = rt.objc_msgSend_id(NSWindow.id(), sel_alloc);
	    CGRect rect = cg.CGRect();
	    rect.origin().x(contentRect.ul.x).y(contentRect.ul.y);
	    rect.size().width(contentRect.sz().x).height(contentRect.sz().y);
	    try {
		id = rt.id((MemorySegment)sendmsg_id_CGRect_int_int_bool.invoke(id.mem(),
										sel_initWithContentRect_styleMask_backing_defer.mem(),
										rect.mem(), style, backingStoreType, defer ? (byte)1 : (byte)0));
	    } catch(Throwable t) {
		throw(new RuntimeException(t));
	    }
	    return(new NSWindow(id));
	}
    }

    private static AppKit instance = null;
    public static AppKit get() {
	if(instance == null) {
	    synchronized(AppKit.class) {
		if(instance == null) {
		    instance = new VersionC();
		}
	    }
	}
	return(instance);
    }

    public static void main(String[] args) throws Exception {
	Runtime rt = Runtime.get();
	rt.mainrun(() -> {
	    AppKit api = AppKit.get();
	    NSApplication app = api.NSApplication_sharedApplication();
	    NSWindow wnd = api.NSWindow(Area.sized(Coord.of(100, 100), Coord.of(600, 600)), 15, 2, true);
	    wnd.setTitle("Test");
	    wnd.setDelegate(new WindowDelegate() {
		public boolean windowShouldClose(ID sender) {
		    Debug.dump(sender);
		    return(true);
		}
		public void windowWillClose(ID sender) {
		    Debug.dump(sender);
		}
	    });
	    wnd.makeKeyAndOrderFront(null);
	    app.run();
	});
	Thread.sleep(1000);
	rt.mainrun(() -> System.err.println(Foundation.get().processInfo().operatingSystemVersionString()));
	Thread.sleep(1000);
    }
}
