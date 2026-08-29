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
import haven.ffi.objc.Foundation.*;
import haven.ffi.objc.CoreGraphics.*;
import java.lang.invoke.*;
import java.lang.foreign.*;
import java.util.*;
import java.util.function.*;
import java.awt.image.*;
import haven.ffi.objc.Runtime.Class;
import java.lang.foreign.MemoryLayout.PathElement;
import static haven.ffi.ABI.*;
import static haven.ffi.FUtils.*;
import static java.lang.foreign.ValueLayout.ADDRESS;

public abstract class AppKit {
    public static final int NSWindowStyleMaskBorderless             = 0;
    public static final int NSWindowStyleMaskTitled                 = 1 << 0;
    public static final int NSWindowStyleMaskClosable               = 1 << 1;
    public static final int NSWindowStyleMaskMiniaturizable         = 1 << 2;
    public static final int NSWindowStyleMaskResizable              = 1 << 3;
    public static final int NSWindowStyleMaskUtilityWindow          = 1 << 4;
    public static final int NSWindowStyleMaskDocModalWindow         = 1 << 6;
    public static final int NSWindowStyleMaskNonactivatingPanel     = 1 << 7;
    public static final int NSWindowStyleMaskUnifiedTitleAndToolbar = 1 << 12;
    public static final int NSWindowStyleMaskHUDWindow              = 1 << 13;
    public static final int NSWindowStyleMaskFullScreen             = 1 << 14;
    public static final int NSWindowStyleMaskFullSizeContentView    = 1 << 15;

    public static final int NSWindowCollectionBehaviorFullScreenPrimary   = 1 << 7;
    public static final int NSWindowCollectionBehaviorFullScreenAuxiliary = 1 << 8;
    public static final int NSWindowCollectionBehaviorFullScreenNone      = 1 << 9;

    public static final int NSWindowOcclusionStateVisible = 1 << 1;

    public static final int NSBackingStoreRetained = 0;
    public static final int NSBackingStoreBuffered = 2;

    public static final int NSApplicationActivationPolicyRegular    = 0;
    public static final int NSApplicationActivationPolicyAccessory  = 1;
    public static final int NSApplicationActivationPolicyProhibited = 2;

    public static final int NSAlphaShiftKeyMask = 1 << 16;
    public static final int NSShiftKeyMask      = 1 << 17;
    public static final int NSControlKeyMask    = 1 << 18;
    public static final int NSAlternateKeyMask  = 1 << 19;
    public static final int NSCommandKeyMask    = 1 << 20;
    public static final int NSNumericPadKeyMask = 1 << 21;
    public static final int NSHelpKeyMask       = 1 << 22;
    public static final int NSFunctionKeyMask   = 1 << 23;

    public static final int NSModalResponseCancel = 0;
    public static final int NSModalResponseOK     = 1;

    public static final int NSDragOperationNone    =  0;
    public static final int NSDragOperationCopy    =  1;
    public static final int NSDragOperationLink    =  2;
    public static final int NSDragOperationGeneric =  4;
    public static final int NSDragOperationPrivate =  8;
    public static final int NSDragOperationMove    = 16;
    public static final int NSDragOperationDelete  = 32;

    public interface NSApplication extends Runtime.NSObject {
	public ID id();
	public void run();
	public void finishLaunching();
	public void setActivationPolicy(int policy);
	public boolean isActive();
    }

    public interface NSWorkspace extends Runtime.NSObject {
	public ID id();
	public boolean openURL(NSURL url);
    }

    public interface NSNotification extends Runtime.NSObject {
	public ID id();
    }

    public interface NSImageRep extends Runtime.NSObject {
	public ID id();
    }

    public interface NSBitmapImageRep extends NSImageRep {
    }
    public abstract NSBitmapImageRep NSBitmapImageRep(CGImage image);

    public interface NSImage extends Runtime.NSObject {
	public ID id();
	public boolean isValid();
	public CGSize size();
	public void addRepresentation(NSImageRep rep);
	public NSData TIFFRepresentation();
    }

    public abstract NSImage NSImage(CGSize size);
    public abstract NSImage NSImage(CGImage image, CGSize size);

    public NSImage NSImage(BufferedImage image, CGSize size) {
	NSImage ret = NSImage(size);
	ret.addRepresentation(NSBitmapImageRep(CoreGraphics.get().CGImageCreate(image)));
	return(ret);
    }

    public interface NSCursor extends Runtime.NSObject {
	public ID id();
    }

    public abstract NSCursor NSCursor(NSImage image, CGPoint hotspot);

    public abstract NSCursor NSCursor_arrowCursor();
    public abstract NSCursor NSCursor_IBeamCursor();
    public abstract NSCursor NSCursor_crosshairCursor();
    public abstract NSCursor NSCursor_closedHandCursor();
    public abstract NSCursor NSCursor_pointingHandCursor();
    public abstract NSCursor NSCursor_resizeLeftCursor();
    public abstract NSCursor NSCursor_resizeUpCursor();
    public abstract NSCursor NSCursor_resizeRightCursor();
    public abstract NSCursor NSCursor_resizeDownCursor();

    public interface NSEvent extends Runtime.NSObject {
	public ID id();
	public int type();
	public double timestamp();
	public CGPoint locationInWindow();
	public int modifierFlags();
	public int buttonNumber();
	public boolean hasPreciseScrollingDeltas();
	public double scrollingDeltaX();
	public double scrollingDeltaY();
	public double deltaX();
	public double deltaY();
	public String characters();
	public String charactersIgnoringModifiers();
	public int keyCode();
	public boolean isARepeat();
	public CoreGraphics.CGEvent CGEvent();
    }

    public interface NSScreen extends Runtime.NSObject {
	public ID id();
	public CGRect frame();
	public String localizedName();
	public double backingScaleFactor();
	public CGRect convertRectToBacking(CGRect rect);
	public CGRect convertRectFromBacking(CGRect rect);
	public double minimumRefreshInterval();
	public double maximumRefreshInterval();
	public NSDictionary deviceDescription();
	public String deviceColorSpaceName();
	public CGSize deviceResolution();
	public CGSize deviceSize();
	public int screenNumber();
    }

    public interface NSWindow extends Runtime.NSObject {
	public ID id();
	public void setDelegate(WindowDelegate delegate);
	public void setAcceptsMouseMovedEvents(boolean val);
	public int styleMask();
	public void setStyleMask(int value);
	public int collectionBehavior();
	public void setCollectionBehavior(int value);
	public void setContentSize(CGSize sz);
	public void setContentMinSize(CGSize sz);
	public void setContentMaxSize(CGSize sz);
	public void makeKeyAndOrderFront(ID sender);
	public void orderOut(ID sender);
	public void setTitle(String title);
	public void center();
	public CGPoint cascadeTopLeftFromPoint(CGPoint c);
	public void setContentView(NSView contentView);
	public boolean isZoomed();
	public void zoom();
	public void performZoom();
	public boolean isMiniaturized();
	public void miniaturize();
	public void deminiaturize();
	public void performMiniaturize();
	public void toggleFullScreen();
	public boolean isKeyWindow();
	public int occlusionState();
    }

    public interface WindowDelegate {
	public default boolean windowShouldClose(NSWindow sender) {return(true);}
	public default void windowWillClose(NSNotification notification) {}
	public default void windowDidResize(NSNotification notification) {}
	public default void windowDidMiniaturize(NSNotification notification) {}
	public default void windowDidDeminiaturize(NSNotification notification) {}
	public default void windowDidBecomeKey(NSNotification notification) {}
	public default void windowDidResignKey(NSNotification notification) {}
    }

    public interface NSDraggingInfo extends Runtime.NSObject {
	public NSPasteboard draggingPasteboard();
	public int draggingSequenceNumber();
	public int draggingSourceOperationMask();
	public CGPoint draggingLocation();
    }

    public interface NSView extends Runtime.NSObject {
	public ID id();
	public void interpretKeyEvents(NSArray events);
	public CGRect frame();
	public CGRect bounds();
	public CGRect convertRectToBacking(CGRect rect);
	public CGRect convertRectFromBacking(CGRect rect);
	public CGSize convertSizeToBacking(CGSize size);
	public CGSize convertSizeFromBacking(CGSize size);
	public CGPoint convertPointToBacking(CGPoint point);
	public CGPoint convertPointFromBacking(CGPoint point);
	public CGPoint convertPointFromView(CGPoint point, NSView view);
	public void setWantsBestResolutionOpenGLSurface(boolean val);
	public void addCursorRect(CGRect rect, NSCursor cursor);
	public void discardCursorRects();
	public void registerForDraggedTypes(String... types);
    }

    public static class NSViewDelegate {
	public boolean acceptsFirstResponder() {return(false);}
	public void mouseDown(NSEvent event) {}
	public void mouseDragged(NSEvent event) {}
	public void mouseUp(NSEvent event) {}
	public void rightMouseDown(NSEvent event) {}
	public void rightMouseDragged(NSEvent event) {}
	public void rightMouseUp(NSEvent event) {}
	public void otherMouseDown(NSEvent event) {}
	public void otherMouseDragged(NSEvent event) {}
	public void otherMouseUp(NSEvent event) {}
	public void scrollWheel(NSEvent event) {}
	public void mouseMoved(NSEvent event) {}
	public void mouseEntered(NSEvent event) {}
	public void mouseExited(NSEvent event) {}
	public void keyDown(NSEvent event) {}
	public void keyUp(NSEvent event) {}
	public void insertText(String string) {}
	public void doCommandBySelector(SEL selector) {}
	public void resetCursorRects() {}

	public int draggingEntered(NSDraggingInfo sender) {return(NSDragOperationNone);}
	public int draggingUpdated(NSDraggingInfo sender) {return(NSDragOperationNone);}
	public boolean wantsPeriodicDraggingUpdates() {return(false);}
	public boolean performDragOperation(NSDraggingInfo sender) {return(false);}
    }

    public interface NSPasteboardItem extends Runtime.NSObject {
	public List<String> types();
	public NSData dataForType(String type);
	public String stringForType(String type);
	public boolean setData(NSData data, String type);
    }
    public abstract NSPasteboardItem NSPasteboardItem();

    public interface NSPasteboard extends Runtime.NSObject {
	public int clearContents();
	public boolean writeObjects(NSObject... objects);
	public List<String> types();
	public List<NSPasteboardItem> pasteboardItems();
    }
    public abstract NSPasteboard NSPasteboard_generalPasteboard();

    public interface NSSavePanel extends Runtime.NSObject {
	public NSURL URL();
	public void beginSheetModal(NSWindow window, Consumer<Integer> handler);
	public void begin(Consumer<Integer> handler);
	public void setAllowedFileTypes(String... types);
	public void setAllowsOtherFileTypes(boolean allowed);
    }
    public abstract NSSavePanel NSSavePanel();

    public interface NSOpenPanel extends NSSavePanel {
    }
    public abstract NSOpenPanel NSOpenPanel();

    public abstract NSApplication NSApplication_sharedApplication();
    public abstract NSWorkspace NSWorkspace_sharedWorkspace();
    public abstract int NSEvent_pressedMouseButtons();
    public abstract List<AppKit.NSScreen> NSScreen_screens();
    public abstract NSWindow NSWindow(CGRect contentRect, int style, int backingStoreType, boolean defer);
    public abstract NSView NSView(NSViewDelegate dg, CGRect frameRect);

    static class VersionC extends AppKit {
	private static final MemoryLayout C_ID = Runtime.objc4.C_ID;
	private static final MemoryLayout C_SEL = Runtime.objc4.C_SEL;
	private static final MemoryLayout OC_BOOL = Runtime.objc4.OC_BOOL;
	private static final MemoryLayout NSUInteger = Runtime.objc4.NSUInteger;
	private static final MemoryLayout NSInteger = Runtime.objc4.NSInteger;
	private final SymbolLookup dylib = SymbolLookup.libraryLookup("/System/Library/Frameworks/AppKit.framework/AppKit", Arena.global());
	private final Arena localarena = Arena.ofAuto();
	final Runtime rt = Runtime.get();
	final CoreGraphics cg = CoreGraphics.get();
	final Foundation fnd = Foundation.get();
	private final SEL sel_alloc = rt.sel_registerName("alloc");
	private final SEL sel_init = rt.sel_registerName("init");

	private final Runtime.Class NSApplication = rt.objc_getClass("NSApplication");
	private final SEL sel_run = rt.sel_registerName("run");
	private final SEL sel_finishLaunching = rt.sel_registerName("finishLaunching");
	private final SEL sel_setActivationPolicy = rt.sel_registerName("setActivationPolicy:");
	private final SEL sel_isActive = rt.sel_registerName("isActive");
	class NSApplication implements AppKit.NSApplication {
	    public final ID id;

	    public NSApplication(ID id) {
		this.id = id;
	    }

	    public ID id() {return(id);}
	    public void run() {rt.objc_msgSend_void(id, sel_run);}
	    public void finishLaunching() {rt.objc_msgSend_void(id, sel_finishLaunching);}
	    public void setActivationPolicy(int policy) {
		rt.objc_msgSend_bool(id, sel_setActivationPolicy, policy);
	    }
	    public boolean isActive() {
		return(rt.objc_msgSend_bool(id, sel_isActive));
	    }
	}

	private final SEL sel_sharedApplication = rt.sel_registerName("sharedApplication");
	public NSApplication NSApplication_sharedApplication() {
	    return(new NSApplication(rt.objc_msgSend_id(NSApplication.id(), sel_sharedApplication)));
	}

	private final Runtime.Class cls_NSWorkspace = rt.objc_getClass("NSWorkspace");
	private final SEL sel_openURL = rt.sel_registerName("openURL:");
	class NSWorkspace implements AppKit.NSWorkspace {
	    public final ID id;

	    public NSWorkspace(ID id) {
		this.id = id;
	    }

	    public ID id() {return(id);}

	    public boolean openURL(NSURL url) {
		return(rt.objc_msgSend_bool(id, sel_openURL, url.id()));
	    }
	}

	private final SEL sel_sharedWorkspace = rt.sel_registerName("sharedWorkspace");
	public NSWorkspace NSWorkspace_sharedWorkspace() {
	    return(new NSWorkspace(rt.objc_msgSend_id(cls_NSWorkspace.id(), sel_sharedWorkspace)));
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
			rt.class_addMethod(WindowDelegateAdapter, rt.sel_registerName("windowDidResize:"),
					   supcall(localarena, MethodHandles.lookup(), WindowDelegateAdapter.class, "windowDidResize", VersionC.this,
						   null, C_ID, C_SEL, C_ID), "B@:@");
			rt.class_addMethod(WindowDelegateAdapter, rt.sel_registerName("windowDidMiniaturize:"),
					   supcall(localarena, MethodHandles.lookup(), WindowDelegateAdapter.class, "windowDidMiniaturize", VersionC.this,
						   null, C_ID, C_SEL, C_ID), "B@:@");
			rt.class_addMethod(WindowDelegateAdapter, rt.sel_registerName("windowDidDeminiaturize:"),
					   supcall(localarena, MethodHandles.lookup(), WindowDelegateAdapter.class, "windowDidDeminiaturize", VersionC.this,
						   null, C_ID, C_SEL, C_ID), "B@:@");
			rt.class_addMethod(WindowDelegateAdapter, rt.sel_registerName("windowDidBecomeKey:"),
					   supcall(localarena, MethodHandles.lookup(), WindowDelegateAdapter.class, "windowDidBecomeKey", VersionC.this,
						   null, C_ID, C_SEL, C_ID), "B@:@");
			rt.class_addMethod(WindowDelegateAdapter, rt.sel_registerName("windowDidResignKey:"),
					   supcall(localarena, MethodHandles.lookup(), WindowDelegateAdapter.class, "windowDidResignKey", VersionC.this,
						   null, C_ID, C_SEL, C_ID), "B@:@");
			rt.objc_registerClassPair(WindowDelegateAdapter);
		    }
		    int key = nextkey++;
		    ID id = this.id = rt.objc_msgSend_id(rt.objc_msgSend_id(WindowDelegateAdapter.id(), sel_alloc), sel_init);
		    rt.object_getIvar(id, WindowDelegateAdapter, "java", ValueLayout.JAVA_INT).set(ValueLayout.JAVA_INT, 0, key);
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
		return((byte)callback(ak, objp, dg -> (byte)(dg.windowShouldClose(NSWindow.unretained(ak, ak.rt.id(sender))) ? 1 : 0), 0));
	    }

	    private static void windowWillClose(VersionC ak, MemorySegment objp, MemorySegment sel, MemorySegment notification) {
		callback(ak, objp, dg -> dg.windowWillClose(NSNotification.unretained(ak, ak.rt.id(notification))));
	    }

	    private static void windowDidResize(VersionC ak, MemorySegment objp, MemorySegment sel, MemorySegment notification) {
		callback(ak, objp, dg -> dg.windowDidResize(NSNotification.unretained(ak, ak.rt.id(notification))));
	    }

	    private static void windowDidMiniaturize(VersionC ak, MemorySegment objp, MemorySegment sel, MemorySegment notification) {
		callback(ak, objp, dg -> dg.windowDidMiniaturize(NSNotification.unretained(ak, ak.rt.id(notification))));
	    }

	    private static void windowDidDeminiaturize(VersionC ak, MemorySegment objp, MemorySegment sel, MemorySegment notification) {
		callback(ak, objp, dg -> dg.windowDidDeminiaturize(NSNotification.unretained(ak, ak.rt.id(notification))));
	    }

	    private static void windowDidBecomeKey(VersionC ak, MemorySegment objp, MemorySegment sel, MemorySegment notification) {
		callback(ak, objp, dg -> dg.windowDidBecomeKey(NSNotification.unretained(ak, ak.rt.id(notification))));
	    }

	    private static void windowDidResignKey(VersionC ak, MemorySegment objp, MemorySegment sel, MemorySegment notification) {
		callback(ak, objp, dg -> dg.windowDidResignKey(NSNotification.unretained(ak, ak.rt.id(notification))));
	    }
	}

	class NSNotification implements AppKit.NSNotification {
	    public final ID id;

	    public NSNotification(ID id) {
		this.id = id;
	    }

	    public static NSNotification unretained(VersionC ak, ID id) {
		return(ak.new NSNotification(id));
	    }

	    public ID id() {return(id);}
	}

	private final Class cls_NSBitmapImageRep = rt.objc_getClass("NSBitmapImageRep");
	class NSBitmapImageRep implements AppKit.NSBitmapImageRep {
	    public final ID id;

	    public NSBitmapImageRep(ID id) {
		this.id = id;
	    }

	    public ID id() {return(id);}
	}
	private final SEL sel_initWithCGImage = rt.sel_registerName("initWithCGImage:");
	public NSBitmapImageRep NSBitmapImageRep(CGImage image) {
	    return(rt.wrap(rt.objc_msgSend_id(rt.objc_msgSend_id(cls_NSBitmapImageRep.id(), sel_alloc), sel_initWithCGImage, image.ref()), NSBitmapImageRep::new, false, true));
	}

	private final Class cls_NSImage = rt.objc_getClass("NSImage");
	private final SEL sel_isValid = rt.sel_registerName("isValid");
	private final SEL sel_size = rt.sel_registerName("size");
	private final SEL sel_TIFFRepresentation = rt.sel_registerName("TIFFRepresentation");
	private final SEL sel_addRepresentation = rt.sel_registerName("addRepresentation:");
	class NSImage implements AppKit.NSImage {
	    public final ID id;

	    public NSImage(ID id) {
		this.id = id;
	    }

	    public ID id() {return(id);}

	    public boolean isValid() {return(rt.objc_msgSend_bool(id, sel_isValid));}
	    public CGSize size() {return(cg.objc_msgSend_CGSize(id, sel_size));}
	    public void addRepresentation(NSImageRep rep) {
		rt.objc_msgSend_void(id, sel_addRepresentation, rep.id());
	    }
	    public NSData TIFFRepresentation() {
		return(fnd.NSData(rt.objc_msgSend_id(id, sel_TIFFRepresentation), true, true));
	    }
	}

	private final SEL sel_initWithSize = rt.sel_registerName("initWithSize:");
	public NSImage NSImage(CGSize size) {
	    return(rt.wrap(cg.objc_msgSend_id(rt.objc_msgSend_id(cls_NSImage.id(), sel_alloc), sel_initWithSize, size), NSImage::new, false, true));
	}
	private final SEL sel_initWithCGImage_size = rt.sel_registerName("initWithCGImage:size:");
	public NSImage NSImage(CGImage image, CGSize size) {
	    return(rt.wrap(cg.objc_msgSend_id(rt.objc_msgSend_id(cls_NSImage.id(), sel_alloc), sel_initWithSize, image.ref(), size), NSImage::new, false, true));
	}

	private final Class cls_NSCursor = rt.objc_getClass("NSCursor");
	class NSCursor implements AppKit.NSCursor {
	    public final ID id;

	    public NSCursor(ID id) {
		this.id = id;
	    }

	    public ID id() {return(id);}
	}

	private final SEL sel_initWithImage_hotSpot = rt.sel_registerName("initWithImage:hotSpot:");
	public NSCursor NSCursor(AppKit.NSImage image, CGPoint hotspot) {
	    return(rt.wrap(cg.objc_msgSend_id(rt.objc_msgSend_id(cls_NSCursor.id(), sel_alloc), sel_initWithImage_hotSpot, image.id(), hotspot), NSCursor::new, false, true));
	}

	private final SEL sel_arrowCursor = rt.sel_registerName("arrowCursor");
	private final SEL sel_IBeamCursor = rt.sel_registerName("IBeamCursor");
	private final SEL sel_crosshairCursor = rt.sel_registerName("crosshairCursor");
	private final SEL sel_closedHandCursor = rt.sel_registerName("closedHandCursor");
	private final SEL sel_pointingHandCursor = rt.sel_registerName("pointingHandCursor");
	private final SEL sel_resizeLeftCursor = rt.sel_registerName("resizeLeftCursor");
	private final SEL sel_resizeUpCursor = rt.sel_registerName("resizeUpCursor");
	private final SEL sel_resizeRightCursor = rt.sel_registerName("resizeRightCursor");
	private final SEL sel_resizeDownCursor = rt.sel_registerName("resizeDownCursor");
	public NSCursor NSCursor_arrowCursor() {return(rt.wrap(rt.objc_msgSend_id(cls_NSCursor.id(), sel_arrowCursor), NSCursor::new, false, false));}
	public NSCursor NSCursor_IBeamCursor() {return(rt.wrap(rt.objc_msgSend_id(cls_NSCursor.id(), sel_IBeamCursor), NSCursor::new, false, false));}
	public NSCursor NSCursor_crosshairCursor() {return(rt.wrap(rt.objc_msgSend_id(cls_NSCursor.id(), sel_crosshairCursor), NSCursor::new, false, false));}
	public NSCursor NSCursor_closedHandCursor() {return(rt.wrap(rt.objc_msgSend_id(cls_NSCursor.id(), sel_closedHandCursor), NSCursor::new, false, false));}
	public NSCursor NSCursor_pointingHandCursor() {return(rt.wrap(rt.objc_msgSend_id(cls_NSCursor.id(), sel_pointingHandCursor), NSCursor::new, false, false));}
	public NSCursor NSCursor_resizeLeftCursor() {return(rt.wrap(rt.objc_msgSend_id(cls_NSCursor.id(), sel_resizeLeftCursor), NSCursor::new, false, false));}
	public NSCursor NSCursor_resizeUpCursor() {return(rt.wrap(rt.objc_msgSend_id(cls_NSCursor.id(), sel_resizeUpCursor), NSCursor::new, false, false));}
	public NSCursor NSCursor_resizeRightCursor() {return(rt.wrap(rt.objc_msgSend_id(cls_NSCursor.id(), sel_resizeRightCursor), NSCursor::new, false, false));}
	public NSCursor NSCursor_resizeDownCursor() {return(rt.wrap(rt.objc_msgSend_id(cls_NSCursor.id(), sel_resizeDownCursor), NSCursor::new, false, false));}

	private final Class cls_NSEvent = rt.objc_getClass("NSEvent");
	private final SEL sel_type = rt.sel_registerName("type");
	private final SEL sel_timestamp = rt.sel_registerName("timestamp");
	private final SEL sel_locationInWindow = rt.sel_registerName("locationInWindow");
	private final SEL sel_modifierFlags = rt.sel_registerName("modifierFlags");
	private final SEL sel_buttonNumber = rt.sel_registerName("buttonNumber");
	private final SEL sel_hasPreciseScrollingDeltas = rt.sel_registerName("hasPreciseScrollingDeltas");
	private final SEL sel_scrollingDeltaX = rt.sel_registerName("scrollingDeltaX");
	private final SEL sel_scrollingDeltaY = rt.sel_registerName("scrollingDeltaY");
	private final SEL sel_deltaX = rt.sel_registerName("deltaX");
	private final SEL sel_deltaY = rt.sel_registerName("deltaY");
	private final SEL sel_characters = rt.sel_registerName("characters");
	private final SEL sel_charactersIgnoringModifiers = rt.sel_registerName("charactersIgnoringModifiers");
	private final SEL sel_keyCode = rt.sel_registerName("keyCode");
	private final SEL sel_isARepeat = rt.sel_registerName("isARepeat");
	private final SEL sel_CGEvent = rt.sel_registerName("CGEvent");
	class NSEvent implements AppKit.NSEvent {
	    public final ID id;

	    public NSEvent(ID id) {
		this.id = id;
	    }

	    public static NSEvent unretained(VersionC ak, ID id) {
		return(ak.new NSEvent(id));
	    }
	    public static NSEvent retain(VersionC ak, ID id) {
		NSEvent ret = unretained(ak, id);
		ak.rt.retain(ret);
		return(ret);
	    }

	    public ID id() {return(id);}

	    public CGPoint locationInWindow() {
		return(cg.objc_msgSend_CGPoint(id, sel_locationInWindow));
	    }
	    public int type() {
		return(objc_msgSend_NSUInt(id, sel_type));
	    }
	    public double timestamp() {
		return(rt.objc_msgSend_double(id, sel_timestamp));
	    }
	    public int modifierFlags() {
		return(objc_msgSend_NSUInt(id, sel_modifierFlags));
	    }
	    public int buttonNumber() {
		return(objc_msgSend_NSUInt(id, sel_buttonNumber));
	    }
	    public boolean hasPreciseScrollingDeltas() {
		return(rt.objc_msgSend_bool(id, sel_hasPreciseScrollingDeltas));
	    }
	    public double scrollingDeltaX() {
		return(rt.objc_msgSend_double(id, sel_scrollingDeltaX));
	    }
	    public double scrollingDeltaY() {
		return(rt.objc_msgSend_double(id, sel_scrollingDeltaY));
	    }
	    public double deltaX() {
		return(rt.objc_msgSend_double(id, sel_deltaX));
	    }
	    public double deltaY() {
		return(rt.objc_msgSend_double(id, sel_deltaY));
	    }
	    public String characters() {
		return(fnd.fromNSString(rt.objc_msgSend_id(id, sel_characters)));
	    }
	    public String charactersIgnoringModifiers() {
		return(fnd.fromNSString(rt.objc_msgSend_id(id, sel_charactersIgnoringModifiers)));
	    }
	    public int keyCode() {
		return(rt.objc_msgSend_int(id, sel_keyCode));
	    }
	    public boolean isARepeat() {
		return(rt.objc_msgSend_bool(id, sel_isARepeat));
	    }
	    public CoreGraphics.CGEvent CGEvent() {
		return(cg.CGEvent(rt.objc_msgSend_ptr(id, sel_CGEvent)));
	    }
	}

	private final SEL sel_pressedMouseButtons = rt.sel_registerName("pressedMouseButtons");
	public int NSEvent_pressedMouseButtons() {
	    return(rt.objc_msgSend_int(cls_NSEvent.id(), sel_pressedMouseButtons));
	}

	private final MethodHandle objc_msgSend_NSUInt = rt.msgtype(NSUInteger);
	public int objc_msgSend_NSUInt(Runtime.ID self, Runtime.SEL sel) {
	    try {
		return((int)(long)objc_msgSend_NSUInt.invoke(self.mem(), sel.mem()));
	    } catch(Throwable e) {throw(new RuntimeException(e));}
	}
	private final MethodHandle objc_msgSend_void_NSUInt = rt.msgtype(null, NSUInteger);
	public void objc_msgSend_void_NSUInt(Runtime.ID self, Runtime.SEL sel, int arg1) {
	    try {
		objc_msgSend_void_NSUInt.invoke(self.mem(), sel.mem(), arg1);
	    } catch(Throwable e) {throw(new RuntimeException(e));}
	}

	private final Runtime.Class cls_NSScreen = rt.objc_getClass("NSScreen");
	private final SEL sel_frame = rt.sel_registerName("frame");
	private final SEL sel_localizedName = rt.sel_registerName("localizedName");
	private final SEL sel_backingScaleFactor = rt.sel_registerName("backingScaleFactor");
	private final SEL sel_convertRectToBacking = rt.sel_registerName("convertRectToBacking:");
	private final SEL sel_convertRectFromBacking = rt.sel_registerName("convertRectFromBacking:");
	private final SEL sel_minimumRefreshInterval = rt.sel_registerName("minimumRefreshInterval");
	private final SEL sel_maximumRefreshInterval = rt.sel_registerName("maximumRefreshInterval");
	private final SEL sel_deviceDescription = rt.sel_registerName("deviceDescription");
	private final NSString NSDeviceColorSpaceName = fnd.NSString(rt.constobj(dylib, "NSDeviceColorSpaceName"), false, false);
	private final NSString NSDeviceResolution = fnd.NSString(rt.constobj(dylib, "NSDeviceResolution"), false, false);
	private final NSString NSDeviceSize = fnd.NSString(rt.constobj(dylib, "NSDeviceSize"), false, false);
	class NSScreen implements AppKit.NSScreen {
	    public final ID id;

	    public NSScreen(ID id) {
		this.id = id;
	    }

	    public ID id() {return(id);}

	    public CGRect frame() {
		return(cg.objc_msgSend_CGRect(id, sel_frame));
	    }

	    public String localizedName() {
		return(fnd.fromNSString(rt.objc_msgSend_id(id, sel_localizedName)));
	    }

	    public double backingScaleFactor() {
		return(rt.objc_msgSend_double(id, sel_backingScaleFactor));
	    }

	    public CGRect convertRectToBacking(CGRect rect) {return(cg.objc_msgSend_CGRect(id, sel_convertRectToBacking, rect));}
	    public CGRect convertRectFromBacking(CGRect rect) {return(cg.objc_msgSend_CGRect(id, sel_convertRectFromBacking, rect));}

	    public double minimumRefreshInterval() {return(rt.objc_msgSend_double(id, sel_minimumRefreshInterval));}
	    public double maximumRefreshInterval() {return(rt.objc_msgSend_double(id, sel_maximumRefreshInterval));}

	    public NSDictionary deviceDescription() {
		return(fnd.NSDictionary(rt.objc_msgSend_id(id, sel_deviceDescription), true, true));
	    }

	    public String deviceColorSpaceName() {
		return(fnd.fromNSString(deviceDescription().valueForKey(NSDeviceColorSpaceName)));
	    }
	    public CGSize deviceResolution() {
		return(fnd.NSValue(deviceDescription().valueForKey(NSDeviceResolution), false, false).sizeValue());
	    }
	    public CGSize deviceSize() {
		return(fnd.NSValue(deviceDescription().valueForKey(NSDeviceSize), false, false).sizeValue());
	    }
	    public int screenNumber() {
		return(fnd.NSNumber(deviceDescription().valueForKey("NSScreenNumber"), false, false).intValue());
	    }
	}

	private final SEL sel_screens = rt.sel_registerName("screens");
	public List<AppKit.NSScreen> NSScreen_screens() {
	    List<AppKit.NSScreen> ret = new ArrayList<>();
	    for(ID id : fnd.NSArray(rt.objc_msgSend_id(cls_NSScreen.id(), sel_screens), false, false))
		ret.add(rt.wrap(id, NSScreen::new, true, true));
	    return(ret);
	}

	private final Runtime.Class cls_NSWindow = rt.objc_getClass("NSWindow");
	private final SEL sel_setDelegate = rt.sel_registerName("setDelegate:");
	private final SEL sel_setAcceptsMouseMovedEvents = rt.sel_registerName("setAcceptsMouseMovedEvents:");
	private final SEL sel_styleMask = rt.sel_registerName("styleMask");
	private final SEL sel_setStyleMask = rt.sel_registerName("setStyleMask:");
	private final SEL sel_collectionBehavior = rt.sel_registerName("collectionBehavior");
	private final SEL sel_setCollectionBehavior = rt.sel_registerName("setCollectionBehavior:");
	private final SEL sel_setContentSize = rt.sel_registerName("setContentSize:");
	private final SEL sel_setContentMinSize = rt.sel_registerName("setContentMinSize:");
	private final SEL sel_setContentMaxSize = rt.sel_registerName("setContentMaxSize:");
	private final SEL sel_makeKeyAndOrderFront = rt.sel_registerName("makeKeyAndOrderFront:");
	private final SEL sel_orderOut = rt.sel_registerName("orderOut:");
	private final SEL sel_setTitle = rt.sel_registerName("setTitle:");
	private final SEL sel_center = rt.sel_registerName("center");
	private final SEL sel_cascadeTopLeftFromPoint = rt.sel_registerName("cascadeTopLeftFromPoint:");
	private final SEL sel_setContentView = rt.sel_registerName("setContentView:");
	private final SEL sel_isZoomed = rt.sel_registerName("isZoomed");
	private final SEL sel_zoom = rt.sel_registerName("zoom:");
	private final SEL sel_performZoom = rt.sel_registerName("performZoom:");
	private final SEL sel_isMiniaturized = rt.sel_registerName("isMiniaturized");
	private final SEL sel_miniaturize = rt.sel_registerName("miniaturize:");
	private final SEL sel_deminiaturize = rt.sel_registerName("deminiaturize:");
	private final SEL sel_performMiniaturize = rt.sel_registerName("performMiniaturize:");
	private final SEL sel_toggleFullScreen = rt.sel_registerName("toggleFullScreen:");
	private final SEL sel_isKeyWindow = rt.sel_registerName("isKeyWindow");
	private final SEL sel_occlusionState = rt.sel_registerName("occlusionState");
	class NSWindow implements AppKit.NSWindow {
	    public final ID id;

	    public NSWindow(ID id) {
		this.id = id;
	    }

	    public static NSWindow unretained(VersionC ak, ID id) {
		return(ak.new NSWindow(id));
	    }

	    public ID id() {return(id);}

	    private WindowDelegateAdapter delegate = null;
	    public void setDelegate(WindowDelegate delegate) {
		this.delegate = new WindowDelegateAdapter(delegate);
		rt.objc_msgSend_void(id, sel_setDelegate, this.delegate.id);
	    }
	    
	    public void setAcceptsMouseMovedEvents(boolean value) {
		rt.objc_msgSend_void(id, sel_setAcceptsMouseMovedEvents, value);
	    }
	    public int styleMask() {
		return(objc_msgSend_NSUInt(id, sel_styleMask));
	    }
	    public void setStyleMask(int value) {
		objc_msgSend_void_NSUInt(id, sel_setStyleMask, value);
	    }
	    public int collectionBehavior() {
		return(objc_msgSend_NSUInt(id, sel_collectionBehavior));
	    }
	    public void setCollectionBehavior(int value) {
		objc_msgSend_void_NSUInt(id, sel_setCollectionBehavior, value);
	    }
	    public void setContentSize(CGSize sz) {
		cg.objc_msgSend_void(id, sel_setContentSize, sz);
	    }
	    public void setContentMinSize(CGSize sz) {
		cg.objc_msgSend_void(id, sel_setContentMinSize, sz);
	    }
	    public void setContentMaxSize(CGSize sz) {
		cg.objc_msgSend_void(id, sel_setContentMaxSize, sz);
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
	    public CGPoint cascadeTopLeftFromPoint(CGPoint c) {
		return(cg.objc_msgSend_CGPoint(id, sel_cascadeTopLeftFromPoint, c));
	    }
	    public void setContentView(AppKit.NSView view) {
		rt.objc_msgSend_void(id, sel_setContentView, view.id());
	    }
	    public boolean isZoomed() {
		return(rt.objc_msgSend_bool(id, sel_isZoomed));
	    }
	    public void zoom() {
		rt.objc_msgSend_void(id, sel_zoom, id);
	    }
	    public void performZoom() {
		rt.objc_msgSend_void(id, sel_performZoom, id);
	    }
	    public boolean isMiniaturized() {
		return(rt.objc_msgSend_bool(id, sel_isMiniaturized));
	    }
	    public void miniaturize() {
		rt.objc_msgSend_void(id, sel_miniaturize, id);
	    }
	    public void deminiaturize() {
		rt.objc_msgSend_void(id, sel_deminiaturize, id);
	    }
	    public void performMiniaturize() {
		rt.objc_msgSend_void(id, sel_performMiniaturize, id);
	    }
	    public void toggleFullScreen() {
		rt.objc_msgSend_void(id, sel_toggleFullScreen, id);
	    }
	    public boolean isKeyWindow() {
		return(rt.objc_msgSend_bool(id, sel_isKeyWindow));
	    }
	    public int occlusionState() {
		return(rt.objc_msgSend_NSUInt(id, sel_occlusionState));
	    }
	}
	private final SEL sel_initWithContentRect_styleMask_backing_defer = rt.sel_registerName("initWithContentRect:styleMask:backing:defer:");
	private final MethodHandle sendmsg_id_CGRect_int_int_bool = rt.msgtype(C_ID, cg.C_CGRect(), NSUInteger, NSUInteger, OC_BOOL);
	public NSWindow NSWindow(CGRect contentRect, int style, int backingStoreType, boolean defer) {
	    ID id = rt.objc_msgSend_id(cls_NSWindow.id(), sel_alloc);
	    try {
		id = rt.id((MemorySegment)sendmsg_id_CGRect_int_int_bool.invoke(id.mem(),
										sel_initWithContentRect_styleMask_backing_defer.mem(),
										contentRect.mem(), style, backingStoreType, defer ? (byte)1 : (byte)0));
	    } catch(Throwable t) {
		throw(new RuntimeException(t));
	    }
	    return(new NSWindow(id));
	}

	private final SEL sel_draggingPasteboard = rt.sel_registerName("draggingPasteboard");
	private final SEL sel_draggingSequenceNumber = rt.sel_registerName("draggingSequenceNumber");
	private final SEL sel_draggingSourceOperationMask = rt.sel_registerName("draggingSourceOperationMask");
	private final SEL sel_draggingLocation = rt.sel_registerName("draggingLocation");
	class NSDraggingInfo implements AppKit.NSDraggingInfo {
	    public final ID id;

	    public NSDraggingInfo(ID id) {
		this.id = id;
	    }

	    public ID id() {return(id);}

	    public NSPasteboard draggingPasteboard() {
		return(rt.wrap(rt.objc_msgSend_id(id, sel_draggingPasteboard), NSPasteboard::new, true, true));
	    }
	    public int draggingSequenceNumber() {
		return(rt.objc_msgSend_NSUInt(id, sel_draggingSequenceNumber));
	    }
	    public int draggingSourceOperationMask() {
		return(rt.objc_msgSend_NSUInt(id, sel_draggingSourceOperationMask));
	    }
	    public CGPoint draggingLocation() {
		return(cg.objc_msgSend_CGPoint(id, sel_draggingLocation));
	    }
	}

	private final Class IOSYSView;
	{
	    IOSYSView = rt.objc_allocateClassPair(rt.objc_getClass("NSView"), "IOSYSView", 0);
	    rt.class_addProtocol(IOSYSView, rt.objc_getProtocol("NSDraggingDestination"));
	    rt.class_addIvar(IOSYSView, "java", 4, 2, "i");
	    rt.class_addMethod(IOSYSView, rt.sel_registerName("acceptsFirstResponder"),
			       supcall(localarena, MethodHandles.lookup(), IOSYSView.class, "acceptsFirstResponder", this,
				       OC_BOOL, C_ID, C_SEL), "B@:");
	    rt.class_addMethod(IOSYSView, rt.sel_registerName("mouseDown:"),
			       supcall(localarena, MethodHandles.lookup(), IOSYSView.class, "mouseDown", this,
				       null, C_ID, C_SEL, C_ID), "v@:@");
	    rt.class_addMethod(IOSYSView, rt.sel_registerName("mouseDragged:"),
			       supcall(localarena, MethodHandles.lookup(), IOSYSView.class, "mouseDragged", this,
				       null, C_ID, C_SEL, C_ID), "v@:@");
	    rt.class_addMethod(IOSYSView, rt.sel_registerName("mouseUp:"),
			       supcall(localarena, MethodHandles.lookup(), IOSYSView.class, "mouseUp", this,
				       null, C_ID, C_SEL, C_ID), "v@:@");
	    rt.class_addMethod(IOSYSView, rt.sel_registerName("rightMouseDown:"),
			       supcall(localarena, MethodHandles.lookup(), IOSYSView.class, "rightMouseDown", this,
				       null, C_ID, C_SEL, C_ID), "v@:@");
	    rt.class_addMethod(IOSYSView, rt.sel_registerName("rightMouseDragged:"),
			       supcall(localarena, MethodHandles.lookup(), IOSYSView.class, "rightMouseDragged", this,
				       null, C_ID, C_SEL, C_ID), "v@:@");
	    rt.class_addMethod(IOSYSView, rt.sel_registerName("rightMouseUp:"),
			       supcall(localarena, MethodHandles.lookup(), IOSYSView.class, "rightMouseUp", this,
				       null, C_ID, C_SEL, C_ID), "v@:@");
	    rt.class_addMethod(IOSYSView, rt.sel_registerName("otherMouseDown:"),
			       supcall(localarena, MethodHandles.lookup(), IOSYSView.class, "otherMouseDown", this,
				       null, C_ID, C_SEL, C_ID), "v@:@");
	    rt.class_addMethod(IOSYSView, rt.sel_registerName("otherMouseDragged:"),
			       supcall(localarena, MethodHandles.lookup(), IOSYSView.class, "otherMouseDragged", this,
				       null, C_ID, C_SEL, C_ID), "v@:@");
	    rt.class_addMethod(IOSYSView, rt.sel_registerName("otherMouseUp:"),
			       supcall(localarena, MethodHandles.lookup(), IOSYSView.class, "otherMouseUp", this,
				       null, C_ID, C_SEL, C_ID), "v@:@");
	    rt.class_addMethod(IOSYSView, rt.sel_registerName("scrollWheel:"),
			       supcall(localarena, MethodHandles.lookup(), IOSYSView.class, "scrollWheel", this,
				       null, C_ID, C_SEL, C_ID), "v@:@");
	    rt.class_addMethod(IOSYSView, rt.sel_registerName("mouseMoved:"),
			       supcall(localarena, MethodHandles.lookup(), IOSYSView.class, "mouseMoved", this,
				       null, C_ID, C_SEL, C_ID), "v@:@");
	    rt.class_addMethod(IOSYSView, rt.sel_registerName("mouseEntered:"),
			       supcall(localarena, MethodHandles.lookup(), IOSYSView.class, "mouseEntered", this,
				       null, C_ID, C_SEL, C_ID), "v@:@");
	    rt.class_addMethod(IOSYSView, rt.sel_registerName("mouseExited:"),
			       supcall(localarena, MethodHandles.lookup(), IOSYSView.class, "mouseExited", this,
				       null, C_ID, C_SEL, C_ID), "v@:@");
	    rt.class_addMethod(IOSYSView, rt.sel_registerName("keyDown:"),
			       supcall(localarena, MethodHandles.lookup(), IOSYSView.class, "keyDown", this,
				       null, C_ID, C_SEL, C_ID), "v@:@");
	    rt.class_addMethod(IOSYSView, rt.sel_registerName("keyUp:"),
			       supcall(localarena, MethodHandles.lookup(), IOSYSView.class, "keyUp", this,
				       null, C_ID, C_SEL, C_ID), "v@:@");
	    rt.class_addMethod(IOSYSView, rt.sel_registerName("insertText:"),
			       supcall(localarena, MethodHandles.lookup(), IOSYSView.class, "insertText", this,
				       null, C_ID, C_SEL, C_ID), "v@:@");
	    rt.class_addMethod(IOSYSView, rt.sel_registerName("doCommandBySelector:"),
			       supcall(localarena, MethodHandles.lookup(), IOSYSView.class, "doCommandBySelector", this,
				       null, C_ID, C_SEL, C_SEL), "v@::");
	    rt.class_addMethod(IOSYSView, rt.sel_registerName("resetCursorRects"),
			       supcall(localarena, MethodHandles.lookup(), IOSYSView.class, "resetCursorRects", this,
				       null, C_ID, C_SEL), "v@:");

	    rt.class_addMethod(IOSYSView, rt.sel_registerName("draggingEntered:"),
			       supcall(localarena, MethodHandles.lookup(), IOSYSView.class, "draggingEntered", this,
				       NSUInteger, C_ID, C_SEL, C_ID), "l@:@");
	    rt.class_addMethod(IOSYSView, rt.sel_registerName("draggingUpdated:"),
			       supcall(localarena, MethodHandles.lookup(), IOSYSView.class, "draggingUpdated", this,
				       NSUInteger, C_ID, C_SEL, C_ID), "l@:@");
	    rt.class_addMethod(IOSYSView, rt.sel_registerName("wantsPeriodicDraggingUpdates"),
			       supcall(localarena, MethodHandles.lookup(), IOSYSView.class, "wantsPeriodicDraggingUpdates", this,
				       OC_BOOL, C_ID, C_SEL), "b@:");
	    rt.class_addMethod(IOSYSView, rt.sel_registerName("performDragOperation:"),
			       supcall(localarena, MethodHandles.lookup(), IOSYSView.class, "performDragOperation", this,
				       OC_BOOL, C_ID, C_SEL, C_ID), "b@:@");
	    rt.objc_registerClassPair(IOSYSView);
	}
	private final SEL sel_interpretKeyEvents = rt.sel_registerName("interpretKeyEvents:");
	private final SEL sel_bounds = rt.sel_registerName("bounds");
	private final SEL sel_convertSizeToBacking = rt.sel_registerName("convertSizeToBacking:");
	private final SEL sel_convertSizeFromBacking = rt.sel_registerName("convertSizeFromBacking:");
	private final SEL sel_convertPointToBacking = rt.sel_registerName("convertPointToBacking:");
	private final SEL sel_convertPointFromBacking = rt.sel_registerName("convertPointFromBacking:");
	private final SEL sel_convertPoint_fromView = rt.sel_registerName("convertPoint:fromView:");
	private final SEL sel_setWantsBestResolutionOpenGLSurface = rt.sel_registerName("setWantsBestResolutionOpenGLSurface:");
	private final SEL sel_addCursorRect_cursor = rt.sel_registerName("addCursorRect:cursor:");
	private final SEL sel_discardCursorRects = rt.sel_registerName("discardCursorRects");
	private final SEL sel_registerForDraggedTypes = rt.sel_registerName("registerForDraggedTypes:");
	class IOSYSView implements NSView {
	    private static final Map<Integer, IOSYSView> reg = new HashMap<>();
	    private static int nextkey = 0;
	    private final ID id;
	    private final NSViewDelegate dg;

	    IOSYSView(NSViewDelegate dg) {
		synchronized(IOSYSView.class) {
		    this.dg = dg;
		    int key = nextkey++;
		    ID id = this.id = rt.objc_msgSend_id(IOSYSView.id(), sel_alloc);
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

	    public ID id() {
		return(id);
	    }

	    public void interpretKeyEvents(NSArray events) {
		rt.objc_msgSend_void(id, sel_interpretKeyEvents, events.id());
	    }

	    public CGRect frame() {return(cg.objc_msgSend_CGRect(id, sel_frame));}
	    public CGRect bounds() {return(cg.objc_msgSend_CGRect(id, sel_bounds));}
	    public CGRect convertRectToBacking(CGRect rect) {return(cg.objc_msgSend_CGRect(id, sel_convertRectToBacking, rect));}
	    public CGRect convertRectFromBacking(CGRect rect) {return(cg.objc_msgSend_CGRect(id, sel_convertRectFromBacking, rect));}
	    public CGSize convertSizeToBacking(CGSize size) {return(cg.objc_msgSend_CGSize(id, sel_convertSizeToBacking, size));}
	    public CGSize convertSizeFromBacking(CGSize size) {return(cg.objc_msgSend_CGSize(id, sel_convertSizeFromBacking, size));}
	    public CGPoint convertPointToBacking(CGPoint point) {return(cg.objc_msgSend_CGPoint(id, sel_convertPointToBacking, point));}
	    public CGPoint convertPointFromBacking(CGPoint point) {return(cg.objc_msgSend_CGPoint(id, sel_convertPointFromBacking, point));}
	    public CGPoint convertPointFromView(CGPoint point, NSView view) {return(cg.objc_msgSend_CGPoint(id, sel_convertPoint_fromView, point, (view == null) ? null : view.id()));}

	    public void addCursorRect(CGRect rect, AppKit.NSCursor cursor) {
		cg.objc_msgSend_void(id, sel_addCursorRect_cursor, rect, cursor.id());
	    }
	    public void discardCursorRects() {
		rt.objc_msgSend_void(id, sel_discardCursorRects);
	    }

	    public void setWantsBestResolutionOpenGLSurface(boolean val) {
		rt.objc_msgSend_void(id, sel_setWantsBestResolutionOpenGLSurface, val);
	    }

	    public void registerForDraggedTypes(String... types) {
		NSString[] buf = new NSString[types.length];
		for(int i = 0; i < types.length; i++)
		    buf[i] = fnd.NSString(types[i]);
		NSArray ary = fnd.NSArray(buf);
		rt.objc_msgSend_void(id, sel_registerForDraggedTypes, ary.id());
	    }

	    private static <R> R callback(VersionC ak, MemorySegment objp, Function<IOSYSView, R> fun, R eret) {
		try {
		    Runtime rt = ak.rt;
		    ID obj = rt.id(objp);
		    int key = rt.object_getIvar(obj, ak.IOSYSView, "java", ValueLayout.JAVA_INT).get(ValueLayout.JAVA_INT, 0);
		    IOSYSView java;
		    synchronized(reg) {
			java = reg.get(key);
		    }
		    return(fun.apply(java));
		} catch(Throwable t) {
		    Thread.UncaughtExceptionHandler h = Thread.currentThread().getUncaughtExceptionHandler();
		    if(h == null)
			new Warning(t, "Uncaught exception in window delegate").issue();
		    else
			h.uncaughtException(Thread.currentThread(), t);
		    return(eret);
		}
	    }

	    private static void callback(VersionC ak, MemorySegment objp, Consumer<IOSYSView> fun) {
		callback(ak, objp, view -> {fun.accept(view); return(null);}, null);
	    }

	    private static byte acceptsFirstResponder(VersionC ak, MemorySegment objp, MemorySegment sel) {
		return((byte)callback(ak, objp, view -> (byte)(view.dg.acceptsFirstResponder() ? 1 : 0), 0));
	    }
	    private static void mouseDown(VersionC ak, MemorySegment objp, MemorySegment sel, MemorySegment event) {
		callback(ak, objp, view -> view.dg.mouseDown(NSEvent.retain(ak, ak.rt.id(event))));
	    }
	    private static void mouseDragged(VersionC ak, MemorySegment objp, MemorySegment sel, MemorySegment event) {
		callback(ak, objp, view -> view.dg.mouseDragged(NSEvent.retain(ak, ak.rt.id(event))));
	    }
	    private static void mouseUp(VersionC ak, MemorySegment objp, MemorySegment sel, MemorySegment event) {
		callback(ak, objp, view -> view.dg.mouseUp(NSEvent.retain(ak, ak.rt.id(event))));
	    }
	    private static void rightMouseDown(VersionC ak, MemorySegment objp, MemorySegment sel, MemorySegment event) {
		callback(ak, objp, view -> view.dg.rightMouseDown(NSEvent.retain(ak, ak.rt.id(event))));
	    }
	    private static void rightMouseDragged(VersionC ak, MemorySegment objp, MemorySegment sel, MemorySegment event) {
		callback(ak, objp, view -> view.dg.rightMouseDragged(NSEvent.retain(ak, ak.rt.id(event))));
	    }
	    private static void rightMouseUp(VersionC ak, MemorySegment objp, MemorySegment sel, MemorySegment event) {
		callback(ak, objp, view -> view.dg.rightMouseUp(NSEvent.retain(ak, ak.rt.id(event))));
	    }
	    private static void otherMouseDown(VersionC ak, MemorySegment objp, MemorySegment sel, MemorySegment event) {
		callback(ak, objp, view -> view.dg.otherMouseDown(NSEvent.retain(ak, ak.rt.id(event))));
	    }
	    private static void otherMouseDragged(VersionC ak, MemorySegment objp, MemorySegment sel, MemorySegment event) {
		callback(ak, objp, view -> view.dg.otherMouseDragged(NSEvent.retain(ak, ak.rt.id(event))));
	    }
	    private static void otherMouseUp(VersionC ak, MemorySegment objp, MemorySegment sel, MemorySegment event) {
		callback(ak, objp, view -> view.dg.otherMouseUp(NSEvent.retain(ak, ak.rt.id(event))));
	    }
	    private static void scrollWheel(VersionC ak, MemorySegment objp, MemorySegment sel, MemorySegment event) {
		callback(ak, objp, view -> view.dg.scrollWheel(NSEvent.retain(ak, ak.rt.id(event))));
	    }
	    private static void mouseMoved(VersionC ak, MemorySegment objp, MemorySegment sel, MemorySegment event) {
		callback(ak, objp, view -> view.dg.mouseMoved(NSEvent.retain(ak, ak.rt.id(event))));
	    }
	    private static void mouseEntered(VersionC ak, MemorySegment objp, MemorySegment sel, MemorySegment event) {
		callback(ak, objp, view -> view.dg.mouseEntered(NSEvent.retain(ak, ak.rt.id(event))));
	    }
	    private static void mouseExited(VersionC ak, MemorySegment objp, MemorySegment sel, MemorySegment event) {
		callback(ak, objp, view -> view.dg.mouseExited(NSEvent.retain(ak, ak.rt.id(event))));
	    }
	    private static void keyDown(VersionC ak, MemorySegment objp, MemorySegment sel, MemorySegment event) {
		callback(ak, objp, view -> view.dg.keyDown(NSEvent.retain(ak, ak.rt.id(event))));
	    }
	    private static void keyUp(VersionC ak, MemorySegment objp, MemorySegment sel, MemorySegment event) {
		callback(ak, objp, view -> view.dg.keyUp(NSEvent.retain(ak, ak.rt.id(event))));
	    }
	    private static void insertText(VersionC ak, MemorySegment objp, MemorySegment sel, MemorySegment string) {
		callback(ak, objp, view -> view.dg.insertText(ak.fnd.fromNSString(ak.rt.id(string))));
	    }
	    private static void doCommandBySelector(VersionC ak, MemorySegment objp, MemorySegment sel, MemorySegment selector) {
		callback(ak, objp, view -> view.dg.doCommandBySelector(ak.rt.sel(selector)));
	    }
	    private static void resetCursorRects(VersionC ak, MemorySegment objp, MemorySegment sel) {
		callback(ak, objp, view -> view.dg.resetCursorRects());
	    }

	    private static long draggingEntered(VersionC ak, MemorySegment objp, MemorySegment sel, MemorySegment sender) {
		return(callback(ak, objp, view -> view.dg.draggingEntered(ak.rt.wrap(ak.rt.id(sender), id -> ak.new NSDraggingInfo(id), true, true)), NSDragOperationNone));
	    }
	    private static long draggingUpdated(VersionC ak, MemorySegment objp, MemorySegment sel, MemorySegment sender) {
		return(callback(ak, objp, view -> view.dg.draggingUpdated(ak.rt.wrap(ak.rt.id(sender), id -> ak.new NSDraggingInfo(id), true, true)), NSDragOperationNone));
	    }
	    private static byte wantsPeriodicDraggingUpdates(VersionC ak, MemorySegment objp, MemorySegment sel) {
		return((byte)callback(ak, objp, view -> (byte)(view.dg.wantsPeriodicDraggingUpdates() ? 1 : 0), 0));
	    }
	    private static byte performDragOperation(VersionC ak, MemorySegment objp, MemorySegment sel, MemorySegment sender) {
		return((byte)callback(ak, objp, view -> (byte)(view.dg.performDragOperation(ak.rt.wrap(ak.rt.id(sender), id -> ak.new NSDraggingInfo(id), true, true)) ? 1 : 0), 0));
	    }
	}

	private final SEL sel_initWithFrame = rt.sel_registerName("initWithFrame:");
	private final MethodHandle sendmsg_id_CGRect = rt.msgtype(C_ID, cg.C_CGRect());
	public NSView NSView(NSViewDelegate dg, CGRect frameRect) {
	    IOSYSView view = new IOSYSView(dg);
	    try {
		sendmsg_id_CGRect.invoke(view.id.mem(), sel_initWithFrame.mem(), frameRect.mem());
	    } catch(Throwable t) {
		throw(new RuntimeException(t));
	    }
	    return(view);
	}

	private final Class cls_NSPasteboardItem = rt.objc_getClass("NSPasteboardItem");
	private final SEL sel_types = rt.sel_registerName("types");
	private final SEL sel_dataForType = rt.sel_registerName("dataForType:");
	private final SEL sel_stringForType = rt.sel_registerName("stringForType:");
	private final SEL sel_setData_forType = rt.sel_registerName("setData:forType:");
	class NSPasteboardItem implements AppKit.NSPasteboardItem {
	    public final ID id;

	    NSPasteboardItem(ID id) {
		this.id = id;
	    }

	    public ID id() {return(id);}

	    public List<String> types() {
		ID types = rt.objc_msgSend_id(id, sel_types);
		if(types == null)
		    return(null);
		List<String> ret = new ArrayList<>();
		for(ID typ : fnd.NSArray(types, false, false))
		    ret.add(fnd.fromNSString(typ));
		return(ret);
	    }

	    public NSData dataForType(String type) {
		return(fnd.NSData(rt.objc_msgSend_id(id, sel_dataForType, fnd.NSString(type).id()), true, true));
	    }

	    public String stringForType(String type) {
		return(fnd.fromNSString(rt.objc_msgSend_id(id, sel_stringForType, fnd.NSString(type).id())));
	    }

	    public boolean setData(NSData data, String type) {
		return(rt.objc_msgSend_bool(id, sel_setData_forType, data.id(), fnd.NSString(type).id()));
	    }
	}
	public NSPasteboardItem NSPasteboardItem() {
	    return(rt.wrap(rt.objc_msgSend_id(rt.objc_msgSend_id(cls_NSPasteboardItem.id(), sel_alloc), sel_init), NSPasteboardItem::new, false, true));
	}

	private final Class cls_NSPasteboard = rt.objc_getClass("NSPasteboard");
	private final SEL sel_clearContents = rt.sel_registerName("clearContents");
	private final SEL sel_writeObjects = rt.sel_registerName("writeObjects:");
	private final SEL sel_pasteboardItems = rt.sel_registerName("pasteboardItems");
	class NSPasteboard implements AppKit.NSPasteboard {
	    public final ID id;

	    NSPasteboard(ID id) {
		this.id = id;
	    }

	    public ID id() {return(id);}

	    public int clearContents() {
		return(rt.objc_msgSend_NSUInt(id, sel_clearContents));
	    }

	    public boolean writeObjects(NSObject... objects) {
		return(rt.objc_msgSend_bool(id, sel_writeObjects, fnd.NSArray(objects).id()));
	    }

	    public List<String> types() {
		ID types = rt.objc_msgSend_id(id, sel_types);
		if(types == null)
		    return(null);
		List<String> ret = new ArrayList<>();
		for(ID typ : fnd.NSArray(types, false, false))
		    ret.add(fnd.fromNSString(typ));
		return(ret);
	    }

	    public List<AppKit.NSPasteboardItem> pasteboardItems() {
		ID items = rt.objc_msgSend_id(id, sel_pasteboardItems);
		if(items == null)
		    return(null);
		List<AppKit.NSPasteboardItem> ret = new ArrayList<>();
		for(ID item : fnd.NSArray(items, false, false))
		    ret.add(rt.wrap(item, NSPasteboardItem::new, true, true));
		return(ret);
	    }
	}

	private final SEL sel_generalPasteboard = rt.sel_registerName("generalPasteboard");
	public NSPasteboard NSPasteboard_generalPasteboard() {
	    return(new NSPasteboard(rt.objc_msgSend_id(cls_NSPasteboard.id(), sel_generalPasteboard)));
	}

	private static void handlecompletion(Consumer<Integer> handler, MemorySegment blk, long result) {
	    Runtime.Block.wrap(() -> handler.accept((int)result));
	}
	private final BlockDescriptor blk_handlecompletion = rt.blockdesc(slookup(MethodHandles.lookup(), VersionC.class, "handlecompletion",
										  Void.TYPE, Consumer.class, MemorySegment.class, Long.TYPE),
									  FunctionDescriptor.ofVoid(ADDRESS, NSInteger),
									  "vl");

	private final Class cls_NSSavePanel = rt.objc_getClass("NSSavePanel");
	private final SEL sel_beginSheetModalForWindow_completionHandler = rt.sel_registerName("beginSheetModalForWindow:completionHandler:");
	private final SEL sel_beginWithCompletionHandler = rt.sel_registerName("beginWithCompletionHandler:");
	private final SEL sel_URL = rt.sel_registerName("URL");
	private final SEL sel_setAllowedFileTypes = rt.sel_registerName("setAllowedFileTypes:");
	private final SEL sel_setAllowsOtherFileTypes = rt.sel_registerName("setAllowsOtherFileTypes:");
	class NSSavePanel implements AppKit.NSSavePanel {
	    private static Collection<Block> active = new HashSet<>();
	    public final ID id;

	    NSSavePanel(ID id) {
		this.id = id;
	    }

	    public ID id() {return(id);}

	    public NSURL URL() {
		return(fnd.NSURL(rt.objc_msgSend_id(id, sel_URL), true, true));
	    }

	    public void beginSheetModal(AppKit.NSWindow window, Consumer<Integer> handler) {
		Block[] blk = {null};
		blk[0] = rt.block(blk_handlecompletion, (Consumer<Integer>)val -> {
		    active.remove(blk[0]);
		    handler.accept(val);
		});
		rt.objc_msgSend_void(id, sel_beginSheetModalForWindow_completionHandler, window.id(), blk[0].id());
		active.add(blk[0]);
	    }

	    public void begin(Consumer<Integer> handler) {
		Block[] blk = {null};
		blk[0] = rt.block(blk_handlecompletion, (Consumer<Integer>)val -> {
		    active.remove(blk[0]);
		    handler.accept(val);
		});
		rt.objc_msgSend_void(id, sel_beginWithCompletionHandler, blk[0].id());
		active.add(blk[0]);
	    }

	    public void setAllowedFileTypes(String... types) {
		NSString[] buf = new NSString[types.length];
		for(int i = 0; i < types.length; i++)
		    buf[i] = fnd.NSString(types[i]);
		NSArray arg = fnd.NSArray(buf);
		rt.objc_msgSend_void(id, sel_setAllowedFileTypes, arg.id());
	    }

	    public void setAllowsOtherFileTypes(boolean allow) {
		rt.objc_msgSend_void(id, sel_setAllowsOtherFileTypes, allow);
	    }
	}

	private final SEL sel_savePanel = rt.sel_registerName("savePanel");
	public NSSavePanel NSSavePanel() {
	    return(rt.wrap(rt.objc_msgSend_id(cls_NSSavePanel.id(), sel_savePanel), NSSavePanel::new, true, true));
	}

	private final Class cls_NSOpenPanel = rt.objc_getClass("NSOpenPanel");
	class NSOpenPanel extends NSSavePanel implements AppKit.NSOpenPanel {
	    NSOpenPanel(ID id) {
		super(id);
	    }
	}

	private final SEL sel_openPanel = rt.sel_registerName("openPanel");
	public NSOpenPanel NSOpenPanel() {
	    return(rt.wrap(rt.objc_msgSend_id(cls_NSOpenPanel.id(), sel_openPanel), NSOpenPanel::new, true, true));
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
}
