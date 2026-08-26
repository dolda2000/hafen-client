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

package haven.iosys.tk.ffi;

import java.util.*;
import java.util.function.*;
import java.awt.image.*;
import java.io.*;
import java.nio.*;
import haven.*;
import haven.iosys.*;
import haven.render.*;
import haven.render.gl.*;
import haven.iosys.tk.*;
import haven.ffi.*;
import haven.ffi.objc.*;
import haven.ffi.gl.*;
import haven.ffi.objc.AppKit.*;
import haven.ffi.objc.CGL.*;
import haven.ffi.objc.CoreGraphics.*;
import haven.ffi.objc.Runtime;
import static haven.ffi.objc.Carbon.*;
import static haven.iosys.tk.Key.Std.*;

@Toolkit.Available(name = "cgl")
public class CocoaContext implements Providers.Factory<Toolkit> {
    private final Runtime rt;
    private final Foundation fnd;
    private final CoreGraphics cg;
    private final Carbon carb;
    private final AppKit ak;
    private final CGL cgl;
    private final OpenGL gl;

    private CocoaContext() {
	try {
	    rt = Runtime.get();
	    fnd = Foundation.get();
	    cg = CoreGraphics.get();
	    carb = Carbon.get();
	    ak = AppKit.get();
	    cgl = CGL.get();
	    gl = cgl.gl();
	} catch(RuntimeException e) {
	    throw(new Unavailable("Cocoa libraries not available", e));
	}
    }

    private static CocoaContext instance = null;
    public static CocoaContext get() {
	if(instance == null) {
	    synchronized(CocoaContext.class) {
		if(instance == null)
		    instance = new CocoaContext();
	    }
	}
	return(instance);
    }

    public Toolkit open(String... args) {
	try {
	    javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
	} catch(Exception e) {
	    throw(new RuntimeException(e));
	}
	return(mainrun(CocoaToolkit::new));
    }

    public int priority() {
	return(System.getProperty("os.name", "").startsWith("Mac OS") ? 100 : 0);
    }
    public boolean experimental() {return(true);}

    private NSApplication app = null;
    public NSApplication app() {
	synchronized(this) {
	    if(app == null) {
		app = ak.NSApplication_sharedApplication();
		/*
		rt.mainrun(() -> {
		    app.setActivationPolicy(AppKit.NSApplicationActivationPolicyRegular);
		    app.finishLaunching();
		    app.run();
		});
		*/
	    }
	    return(app);
	}
    }

    void mainrun(Runnable task) {
	rt.mainrun(task);
    }

    <T> T mainrun(Supplier<T> task) {
	class Runner implements Runnable {
	    T val;
	    boolean done;

	    public void run() {
		val = task.get();
		synchronized(this) {
		    done = true;
		    notifyAll();
		}
	    }
	}
	Runner r = new Runner();
	mainrun(r);
	boolean irq = false;
	synchronized(r) {
	    while(!r.done) {
		try {
		    r.wait();
		} catch(InterruptedException e) {
		    irq = true;
		}
	    }
	}
	if(irq)
	    Thread.currentThread().interrupt();
	return(r.val);
    }

    public class CocoaToolkit implements Toolkit {
	private static final int glversions[] = {
	    CGL.NSOpenGLProfileVersion4_1Core,
	    CGL.NSOpenGLProfileVersion3_2Core,
	    CGL.NSOpenGLProfileVersionLegacy,
	};
	public final NSApplication app;
	public final NSOpenGLContext ctx;
	public final Map<String, LayoutMap> layouts = new IdentityHashMap<>();
	public final int kbdtype;
	public final NSCursor nocursor = ak.NSCursor(ak.NSImage(cg.CGSize(Coord.of(1, 1))), cg.CGPoint(Coord.z));

	private CocoaToolkit() {
	    kbdtype = carb.LMGetKbdType();
	    for(Carbon.TISInputSource is : carb.TISCreateInputSourceList(false)) {
		if(is.inputSourceType() == Carbon.kTISTypeKeyboardLayout)
		    layouts.put(is.inputSourceID(), new LayoutMap(is));
	    }
	    app = app();
	    NSOpenGLPixelFormat fmt = null;
	    for(int ver : glversions) {
		int[] ctxattribs = {
		    CGL.NSOpenGLPFADoubleBuffer,
		    CGL.NSOpenGLPFAColorSize, 24,
		    CGL.NSOpenGLPFAAlphaSize, 8,
		    CGL.NSOpenGLPFAOpenGLProfile, ver,
		    0, 0
		};
		if((fmt = cgl.NSOpenGLPixelFormat(ctxattribs)) != null)
		    break;
	    }
	    if(fmt == null)
		throw(new Unavailable("could not create OpenGL context"));
	    ctx = cgl.NSOpenGLContext(fmt, null);
	}

	public class CocoaMonitor implements Monitor {
	    public final String nm;
	    public final Coord res;
	    public final int id, refresh;
	    public final double scale, density;

	    public CocoaMonitor(NSScreen scr) {
		nm = scr.localizedName();
		id = scr.screenNumber();
		res = scr.convertRectToBacking(scr.frame()).size().c();
		refresh = (int)Math.round(1.0 / scr.maximumRefreshInterval());
		scale = scr.backingScaleFactor();
		CGSize sz = cg.CGDisplayScreenSize(id);
		density = 25.4 * ((res.x / sz.width()) + (res.y / sz.height())) / 2;
	    }

	    public Coord resolution() {return(res);}
	    public int refresh() {return(refresh);}
	    public double scaling() {return(scale);}
	    public double density() {return(density);}

	    public String toString() {
		return(String.format("#<screen %d (%s) %dx%d@%d, scale=%.1f, dpi=%.1f>", id, nm, res.x, res.y, refresh, scale, density));
	    }
	}

	public Collection<Monitor> monitors() {
	    List<Monitor> ret = new ArrayList<>();
	    for(NSScreen scr : ak.NSScreen_screens())
		ret.add(new CocoaMonitor(scr));
	    return(ret);
	}

	public class CocoaCursor implements Cursor {
	    public final CGImage img;
	    public final Coord hs;
	    private NSCursor nsc;

	    public CocoaCursor(CGImage img, Coord hs) {
		this.img = img;
		this.hs = hs;
	    }

	    public NSCursor nsc(NSView view) {
		if(nsc == null) {
		    NSImage nsi = ak.NSImage(view.convertSizeFromBacking(cg.CGSize(Coord.of(img.getWidth(), img.getHeight()))));
		    nsi.addRepresentation(ak.NSBitmapImageRep(img));
		    nsc = ak.NSCursor(nsi, view.convertPointFromBacking(cg.CGPoint(hs)));
		}
		return(nsc);
	    }

	    public void dispose() {}
	}

	public Cursor.Caps cursorcaps() {
	    return(new Cursor.Caps(256, 0));
	}

	public Cursor makecursor(BufferedImage img, Coord hs) {
	    return(new CocoaCursor(cg.CGImageCreate(img), hs));
	}

	private <T> T glrun0(NSView view, Supplier<T> task) {
	    ctx.setView(view);
	    ctx.makeCurrentContext();
	    try {
		ctx.update();
		return(task.get());
	    } finally {
		ctx.clearCurrentContext();
		// ctx.clearDrawable();
	    }
	}

	<T> T glrun(NSView view, Supplier<T> task) {
	    return(mainrun(() -> glrun0(view, task)));
	}

	void glrun(NSView view, Runnable task) {
	    mainrun(() -> {glrun0(view, () -> {task.run(); return(null);});});
	}

	public static class CocoaMouseBtn implements MouseBtn {
	    public final int number;

	    public CocoaMouseBtn(int number) {
		this.number = number;
	    }

	    public String id() {return(("cocoa:" + number).intern());}
	    public String nm() {return("Button " + (number + 1));}
	}

	public static MouseBtn buttonid(int num) {
	    switch(num) {
	    case 0: return(MouseBtn.Std.LEFT);
	    case 1: return(MouseBtn.Std.RIGHT);
	    case 2: return(MouseBtn.Std.MIDDLE);
	    }
	    return(new CocoaMouseBtn(num));
	}

	public static Set<Key.Mod> modflags(int flags) {
	    Set<Key.Mod> ret = EnumSet.noneOf(Key.Mod.class);
	    if((flags & AppKit.NSShiftKeyMask) != 0)
		ret.add(Key.Mod.SHIFT);
	    if((flags & AppKit.NSControlKeyMask) != 0)
		ret.add(Key.Mod.CONTROL);
	    if((flags & AppKit.NSAlternateKeyMask) != 0) {
		ret.add(Key.Mod.ALT);
		ret.add(Key.Mod.META);
	    }
	    return(ret);
	}

	public class NamedSym implements Key.Sym {
	    public final String nm, id;

	    public NamedSym(String nm) {
		this.nm = nm;
		this.id = ("osxn:" + nm).intern();
	    }

	    public String id() {return(id);}
	    public String nm() {return(nm);}

	    public int hashCode() {return(nm.hashCode());}
	    public boolean equals(NamedSym that) {return(this.nm.equals(that.nm));}
	    public boolean equals(Object x) {return((x instanceof NamedSym) && equals((NamedSym)x));}

	    public String toString() {return("{" + nm + "}");}
	}

	public class CodeSym implements Key.Sym {
	    public final int kc;
	    public final String nm, id;

	    public CodeSym(int kc) {
		this.kc = kc;
		this.id = ("osxc:" + kc).intern();
		this.nm = String.format("Unknown key " + kc);
	    }

	    public String id() {return(id);}
	    public String nm() {return(nm);}

	    public int hashCode() {return(kc);}
	    public boolean equals(CodeSym that) {return(this.kc == that.kc);}
	    public boolean equals(Object x) {return((x instanceof CodeSym) && equals((CodeSym)x));}

	    public String toString() {return(String.format("#<key " + kc + ">"));}
	}

	public class LayoutMap {
	    public static final int[] states = {
		0,
		Carbon.shiftKey,
		Carbon.optionKey,
		Carbon.optionKey | Carbon.shiftKey,
		Carbon.alphaLock,
		Carbon.alphaLock | Carbon.shiftKey,
		Carbon.alphaLock | Carbon.optionKey,
		Carbon.alphaLock | Carbon.optionKey | Carbon.shiftKey,
	    };
	    public final String id;
	    public final Carbon.UCKeyboardLayout layout;
	    private final Map<Integer, Key.Sym[]> names = new HashMap<>();

	    public LayoutMap(Carbon.TISInputSource layout) {
		this.id = layout.inputSourceID();
		this.layout = layout.unicodeKeyLayoutData();
	    }

	    public Key.Sym[] get(int code) {
		Key.Sym[] ret = names.get(code);
		if(ret == null) {
		    List<Key.Sym> buf = new ArrayList<>();
		    for(int state : states) {
			String name = carb.UCKeyTranslate(layout, code, Carbon.kUCKeyActionDown, (state >> 8) & 0xff, kbdtype, Carbon.kUCKeyTranslateNoDeadKeysMask);
			if((name == null) || (name.length() == 0))
			    continue;
			Key.Sym sym = null;
			if(name.length() == 1) {
			    char ch = name.charAt(0);
			    if((sym = stdcsyms.get(name.charAt(0))) == null)
				sym = stdcsyms.get(Character.toUpperCase(name.charAt(0)));
			}
			if((sym == null) && (name.charAt(0) >= 32))
			    sym = new NamedSym(name);
			if(sym != null && !buf.contains(sym))
			    buf.add(sym);
		    }
		    names.put(code, ret = buf.toArray(new Key.Sym[0]));
		}
		return(ret);
	    }
	}

	private void addlayout(List<LayoutMap> buf, LayoutMap map) {
	    for(LayoutMap prev : buf) {
		if(prev.id == map.id)
		    return;
	    }
	    buf.add(map);
	}
	private void addlayout(List<LayoutMap> buf, Carbon.TISInputSource layout) {
	    addlayout(buf, layouts.computeIfAbsent(layout.inputSourceID(), key -> new LayoutMap(layout)));
	}

	private List<LayoutMap> curlayouts() {
	    List<LayoutMap> ret = new ArrayList<>();
	    addlayout(ret, carb.TISCopyCurrentKeyboardLayoutInputSource());
	    addlayout(ret, carb.TISCopyCurrentASCIICapableKeyboardLayoutInputSource());
	    for(LayoutMap layout : layouts.values())
		addlayout(ret, layout);
	    return(ret);
	}

	public static class CocoaKeyCode implements Key.Loc {
	    public int code;

	    public CocoaKeyCode(int code) {
		this.code = code;
	    }

	    public String id() {return(("osx:" + code).intern());}
	    public String toString() {return("<" + code + ">");}
	}

	public class CocoaKey implements Key {
	    public final String playout;
	    public final int kc;
	    public final Sym[] syms;
	    public final Loc loc;

	    public CocoaKey(int kc) {
		this.kc = kc;
		Sym vsym = stdvsyms.get(kc);
		if(vsym == null) vsym = new CodeSym(kc);
		ArrayList<Sym> syms = new ArrayList<>();
		syms.add(vsym);
		String playout = null;
		for(LayoutMap layout : curlayouts()) {
		    if(playout == null)
			playout = layout.id;
		    for(Sym sym : layout.get(kc)) {
			if(!syms.contains(sym))
			    syms.add(sym);
		    }
		}
		this.syms = syms.toArray(new Sym[0]);
		this.playout = playout;
		Loc loc = stdkeys.get(kc);
		this.loc = (loc == null) ? new CocoaKeyCode(kc) : loc;
	    }

	    public String id() {
		return(("osx:" + kc).intern());
	    }

	    public Loc location() {return(loc);}

	    public Sym primary() {
		if(syms.length > 0)
		    return(syms[0]);
		return(null);
	    }

	    public Sym primary(Collection<? extends Sym> of) {
		for(Sym sym : syms) {
		    if(of.contains(sym))
			return(sym);
		}
		return(null);
	    }

	    public int hashCode() {return((playout.hashCode() * 31) + kc);}
	    public boolean equals(CocoaKey that) {return(Utils.eq(this.playout, that.playout) && (this.kc == that.kc));}
	    public boolean equals(Object x) {return((x instanceof CocoaKey) && equals((CocoaKey)x));}

	    public String toString() {
		return(String.format("#<osxkey kc=%x %s syms=%s>", kc, loc, Arrays.deepToString(syms)));
	    }
	}

	public class CocoaWindow implements Windeye {
	    public final NSWindow nsw;
	    public final NSView view;
	    private final Collection<EventListener> callbacks = new java.util.concurrent.CopyOnWriteArrayList<>();
	    private boolean shown = false;
	    private Sizing sizeinfo = new Sizing().normsize(Coord.of(800, 600));
	    private State showstate = null;
	    private CGLEnvironment renv;
	    private Coord size = Coord.z;
	    private NSCursor cursor = null;

	    public class CGLEnvironment extends FFIEnvironment {
		private int qstate;

		private CGLEnvironment() {
		    super(gl, Area.sized(Coord.of(1, 1)));
		}

		private void process() {
		    synchronized(this) {
			qstate = 2;
		    }
		    process(gl);
		    synchronized(this) {
			if((qstate & 1) != 0)
			    glrun(view, (Runnable)this::process);
			qstate &= ~2;
		    }
		}

		public void submit(Render cmd) {
		    super.submit(cmd);
		    synchronized(this) {
			if(renv == this) {
			    if(qstate == 0)
				glrun(view, (Runnable)this::process);
			    qstate |= 1;
			}
		    }
		}

		public CocoaWindow wnd() {return(CocoaWindow.this);}
	    }

	    private CocoaWindow() {
		nsw = ak.NSWindow(cg.CGRect(Area.sized(Coord.of(1, 1))), 
				  AppKit.NSWindowStyleMaskTitled |
				  AppKit.NSWindowStyleMaskClosable |
				  AppKit.NSWindowStyleMaskMiniaturizable |
				  AppKit.NSWindowStyleMaskResizable,
				  AppKit.NSBackingStoreBuffered,
				  true);
		nsw.setDelegate(new WindowDelegate());
		nsw.setAcceptsMouseMovedEvents(true);
		nsw.setCollectionBehavior(AppKit.NSWindowCollectionBehaviorFullScreenPrimary);
		view = ak.NSView(new ViewDelegate(), cg.CGRect(Area.sized(Coord.of(1, 1))));
		view.setWantsBestResolutionOpenGLSurface(true);
		nsw.setContentView(view);
	    }

	    class WindowDelegate implements AppKit.WindowDelegate {
		public boolean windowShouldClose(NSWindow sender) {
		    callback(new CloseRequest() {});
		    return(false);
		}

		public void windowDidResize(NSNotification notification) {
		    size = view.convertRectToBacking(view.bounds()).size().c();
		}
	    }

	    class ViewDelegate extends AppKit.NSViewDelegate {
		private StringBuilder textbuf = null;
		private double sax = 0, say = 0;

		public boolean acceptsFirstResponder() {return(true);}

		public void mouseDown(NSEvent event) {callback(new CocoaMouseDownEvent(event));}
		public void mouseDragged(NSEvent event) {callback(new CocoaMouseMoveEvent(event));}
		public void mouseUp(NSEvent event) {callback(new CocoaMouseUpEvent(event));}
		public void rightMouseDown(NSEvent event) {callback(new CocoaMouseDownEvent(event));}
		public void rightMouseDragged(NSEvent event) {callback(new CocoaMouseMoveEvent(event));}
		public void rightMouseUp(NSEvent event) {callback(new CocoaMouseUpEvent(event));}
		public void otherMouseDown(NSEvent event) {callback(new CocoaMouseDownEvent(event));}
		public void otherMouseDragged(NSEvent event) {callback(new CocoaMouseMoveEvent(event));}
		public void otherMouseUp(NSEvent event) {callback(new CocoaMouseUpEvent(event));}

		public void scrollWheel(NSEvent event) {
		    double x, y;
		    if(event.hasPreciseScrollingDeltas()) {
			x = event.scrollingDeltaX() / -15;
			y = event.scrollingDeltaY() / -15;
		    } else {
			/* HACK: Assume non-precise scrolling deltas
			 * mean we have a coarse, physical
			 * scroll-wheel, and try to undo Cocoa's
			 * scroll acceleration and Shift-axis-mangling
			 * the best we can. :/ */
			CGEvent cg = event.CGEvent();
			x = cg.getIntegerValueField(CoreGraphics.kCGScrollWheelEventDeltaAxis2);
			y = cg.getIntegerValueField(CoreGraphics.kCGScrollWheelEventDeltaAxis1);
			if(x < 0) x = 1; else if(x > 0) x = -1;
			if(y < 0) y = 1; else if(y > 0) y = -1;
			if((event.modifierFlags() & AppKit.NSShiftKeyMask) != 0) {
			    double t = x;
			    x = y; y = t;
			}
		    }
		    if(x != 0) {
			sax += x;
			int amount = (int)sax;
			sax -= amount;
			callback(new CocoaMouseWheelEvent(event, MouseWheelEvent.Axis.HORIZ, amount, x));
		    }
		    if(y != 0) {
			say += y;
			int amount = (int)say;
			say -= amount;
			callback(new CocoaMouseWheelEvent(event, MouseWheelEvent.Axis.VERT, amount, y));
		    }
		}

		public void mouseMoved(NSEvent event) {
		    callback(new CocoaMouseMoveEvent(event));
		}

		public void keyDown(NSEvent event) {
		    textbuf = new StringBuilder();
		    view.interpretKeyEvents(fnd.NSArray(event));
		    callback(new CocoaKeyDownEvent(event, textbuf.toString()));
		    textbuf = null;
		}

		public void keyUp(NSEvent event) {
		    callback(new CocoaKeyUpEvent(event));
		}

		public void insertText(String string) {
		    if(textbuf != null) {
			textbuf.append(string);
		    } else {
			callback(new CocoaInsertTextEvent(string));
		    }
		}

		private final Runtime.SEL sel_insertNewline = rt.sel_registerName("insertNewline:");
		private final Runtime.SEL sel_insertTab = rt.sel_registerName("insertTab:");
		private final Runtime.SEL sel_deleteBackward = rt.sel_registerName("deleteBackward:");
		private final Runtime.SEL sel_cancelOperation = rt.sel_registerName("cancelOperation:");
		public void doCommandBySelector(Runtime.SEL selector) {
		    if(selector.equals(sel_insertNewline)) {
			textbuf.append('\n');
		    } else if(selector.equals(sel_insertTab)) {
			textbuf.append('\t');
		    } else if(selector.equals(sel_deleteBackward)) {
			textbuf.append('\b');
		    } else if(selector.equals(sel_cancelOperation)) {
			textbuf.append('\033');
		    }
		}

		public void resetCursorRects() {
		    updatecursor();
		}
	    }

	    public class CocoaKeyEvent {
		public final NSEvent event;
		public final CocoaKey key;
		public final Set<Key.Mod> mods;

		public CocoaKeyEvent(NSEvent event) {
		    this.event = event;
		    this.key = new CocoaKey(event.keyCode());
		    this.mods = modflags(event.modifierFlags());
		}

		public String string() {return("");}
		public Key key() {return(key);}
		public Set<Key.Mod> mods() {return(mods);}
	    }

	    public class CocoaKeyDownEvent extends CocoaKeyEvent implements KeyDownEvent {
		public final Key.Sym sym;
		public final String text;

		public CocoaKeyDownEvent(NSEvent event, String text) {
		    super(event);
		    if((text.length() == 0) && mods.contains(Key.Mod.CONTROL)) {
			Key.Sym ctl = key.primary(ctlkeys);
			if(ctl != null)
			    text = Character.toString(ctlkeys.indexOf(ctl));
		    }
		    this.text = text;
		    Key.Sym psym = null;
		    if(text.length() == 1) {
			for(Key.Sym sym : key.syms) {
			    if((sym instanceof Key.Std) && (((Key.Std)sym).ch == Character.toUpperCase(text.charAt(0)))) {
				psym = sym;
				break;
			    } else if((sym instanceof NamedSym) && ((NamedSym)sym).nm.equals(text)) {
				psym = sym;
				break;
			    }
			}
		    }
		    if(psym == null)
			psym = key.primary();
		    this.sym = psym;
		}

		public Key.Sym sym() {return(sym);}
		public String string() {return(text);}
	    }
	    public class CocoaKeyUpEvent extends CocoaKeyEvent implements KeyUpEvent {
		public CocoaKeyUpEvent(NSEvent event) {super(event);}
	    }

	    public class CocoaInsertTextEvent implements KeyDownEvent {
		public final String text;

		public CocoaInsertTextEvent(String text) {
		    this.text = text;
		}

		public Key key() {return(null);}
		public Key.Sym sym() {return(null);}
		public String string() {return(text);}
		public Set<Key.Mod> mods() {return(Collections.emptySet());}
	    }

	    public class CocoaMouseEvent {
		public final NSEvent event;
		public final Coord wndc;

		public CocoaMouseEvent(NSEvent event) {
		    this.event = event;
		    this.wndc = Coord.of(0, size.y)
			.add(view.convertPointToBacking(event.locationInWindow()).c().mul(1, -1));
		}

		public Coord wndc() {return(wndc);}
		public MouseBtn button() {return(buttonid(event.buttonNumber()));}

		public Set<MouseBtn> held() {
		    Set<MouseBtn> ret = new HashSet<>();
		    int fl = ak.NSEvent_pressedMouseButtons();
		    while(true) {
			int bit = Integer.numberOfTrailingZeros(fl);
			if(bit >= 32)
			    return(ret);
			ret.add(buttonid(bit));
			fl &= ~(1 << bit);
		    }
		}

		public Set<Key.Mod> mods() {return(modflags(event.modifierFlags()));}
	    }
	    public class CocoaMouseMoveEvent extends CocoaMouseEvent implements MouseMoveEvent {
		public CocoaMouseMoveEvent(NSEvent event) {super(event);}
	    }
	    public class CocoaMouseDownEvent extends CocoaMouseEvent implements MouseDownEvent {
		public CocoaMouseDownEvent(NSEvent event) {super(event);}
	    }
	    public class CocoaMouseUpEvent extends CocoaMouseEvent implements MouseUpEvent {
		public CocoaMouseUpEvent(NSEvent event) {super(event);}
	    }
	    public class CocoaMouseWheelEvent extends CocoaMouseEvent implements MouseWheelEvent {
		public final Axis axis;
		public final int amount;
		public final double sub;

		public CocoaMouseWheelEvent(NSEvent event, Axis axis, int amount, double sub) {
		    super(event);
		    this.axis = axis;
		    this.amount = amount;
		    this.sub = sub;
		}

		public Axis axis() {return(axis);}
		public int amount() {return(amount);}
		public double subamount() {return(sub);}
	    }

	    public CocoaToolkit toolkit() {
		return(CocoaToolkit.this);
	    }

	    public void add(EventListener l) {
		callbacks.add(l);
	    }

	    private void callback(Event ev) {
		for(EventListener l : callbacks)
		    l.event(ev);
	    }

	    private void updatesizing(Sizing info) {
		if(info.fixsize != null) {
		    nsw.setStyleMask(nsw.styleMask() & ~AppKit.NSWindowStyleMaskResizable);
		    nsw.setContentSize(view.convertSizeFromBacking(cg.CGSize(info.fixsize)));
		} else {
		    nsw.setStyleMask(nsw.styleMask() | AppKit.NSWindowStyleMaskResizable);
		    if(info.normsize != null)
			nsw.setContentSize(view.convertSizeFromBacking(cg.CGSize(info.normsize)));
		    if(info.minsize != null)
			nsw.setContentMinSize(view.convertSizeFromBacking(cg.CGSize(info.minsize)));
		    if(info.maxsize != null)
			nsw.setContentMaxSize(view.convertSizeFromBacking(cg.CGSize(info.maxsize)));
		}
	    }

	    private void updstate(State st) {
		if(nsw.isMiniaturized() && (st != State.MINIMIZED))
		    nsw.deminiaturize();
		if(((nsw.styleMask() & AppKit.NSWindowStyleMaskFullScreen) != 0) && (st != State.EXCLUSIVE))
		    nsw.toggleFullScreen();
		switch(st) {
		case MAXIMIZED: nsw.performZoom(); break;
		case NORMAL: if(nsw.isZoomed()) nsw.zoom(); break;
		case MINIMIZED: nsw.performMiniaturize(); break;
		case EXCLUSIVE: if((nsw.styleMask() & AppKit.NSWindowStyleMaskFullScreen) == 0) nsw.toggleFullScreen(); break;
		}
	    }

	    public CocoaWindow show(boolean show) {
		mainrun(() -> {
		    if(!shown) {
			if(show) {
			    updatesizing(sizeinfo);
			    nsw.cascadeTopLeftFromPoint(cg.CGPoint(Coord.z));
			    nsw.makeKeyAndOrderFront(null);
			    if(showstate != null) {
				updstate(showstate);
				showstate = null;
			    }
			    shown = true;
			}
		    } else {
			if(show) {
			    nsw.makeKeyAndOrderFront(null);
			} else {
			    nsw.orderOut(null);
			    shown = false;
			}
		    }
		});
		return(this);
	    }

	    public CocoaWindow title(String title) {
		mainrun(() -> nsw.setTitle(title));
		return(this);
	    }

	    public CocoaWindow icon(BufferedImage img) {
		return(this);
	    }

	    private void updatecursor() {
		view.discardCursorRects();
		if(cursor != null)
		    view.addCursorRect(view.frame(), cursor);
	    }

	    public CocoaWindow cursor(Cursor curs) {
		mainrun(() -> {
		    NSCursor nsc = null;
		    if(curs instanceof Cursor.Std) {
			switch((Cursor.Std)curs) {
			case DEFAULT: nsc = null; break;
			case NONE: nsc = nocursor; break;
			case POINTER: nsc = ak.NSCursor_arrowCursor(); break;
			case WAIT: nsc = ak.NSCursor_arrowCursor(); break;
			case HAND: nsc = ak.NSCursor_pointingHandCursor(); break;
			case MOVE: nsc = ak.NSCursor_closedHandCursor(); break;
			case CARET: nsc = ak.NSCursor_IBeamCursor(); break;
			case CROSSHAIR: nsc = ak.NSCursor_crosshairCursor(); break;
			case SIZE_N: nsc = ak.NSCursor_resizeUpCursor(); break;
			case SIZE_E: nsc = ak.NSCursor_resizeRightCursor(); break;
			case SIZE_S: nsc = ak.NSCursor_resizeDownCursor(); break;
			case SIZE_W: nsc = ak.NSCursor_resizeLeftCursor(); break;
			case SIZE_NE: nsc = ak.NSCursor_crosshairCursor(); break;
			case SIZE_SE: nsc = ak.NSCursor_crosshairCursor(); break;
			case SIZE_SW: nsc = ak.NSCursor_crosshairCursor(); break;
			case SIZE_NW: nsc = ak.NSCursor_crosshairCursor(); break;
			}
		    } else if(curs instanceof CocoaCursor) {
			nsc = ((CocoaCursor)curs).nsc(view);
		    }
		    this.cursor = nsc;
		    updatecursor();
		});
		return(this);
	    }

	    public CocoaWindow sizing(Sizing info) {
		mainrun(() -> {
		    sizeinfo = info;
		    if(shown)
			updatesizing(info);
		});
		return(this);
	    }

	    public CocoaWindow state(State st) {
		mainrun(() -> {
		    if(!shown) {
			showstate = st;
		    } else {
			updstate(st);
		    }
		});
		return(this);
	    }

	    public Coord size() {
		return(size);
	    }

	    public State state() {
		return(mainrun(() -> {
		    if(nsw.isZoomed())
			return(State.MAXIMIZED);
		    return(State.NORMAL);
		}));
	    }

	    public boolean focused() {
		return(nsw.isKeyWindow() && app.isActive());
	    }
	    public Visibility visible() {
		if((nsw.occlusionState() & AppKit.NSWindowOcclusionStateVisible) == 0)
		    return(Visibility.NONE);
		return(Visibility.FULL);
	    }

	    public Environment env() {
		if(renv == null) {
		    synchronized(this) {
			if(renv == null)
			    renv = glrun(view, CGLEnvironment::new);
		    }
		}
		return(renv);
	    }

	    private static final Pipe.Op glfb = Pipe.Op.compose(new FragColor<>(FragColor.defcolor),
								new DepthBuffer<>(DepthBuffer.defdepth));
	    public Pipe.Op fbstate() {
		return(glfb);
	    }

	    private int cursi = -1;
	    private void glswap(GL gl, int ival) {
		GLException.checkfor(gl, null);
		if(ival != cursi)
		    ctx.setParameters(new int[] {cursi = ival}, CGL.NSOpenGLCPSwapInterval);
		ctx.flushBuffer();
		GLException.checkfor(gl, null);
	    }

	    public void swapbuffers(Render buf, Object mode) {
		GLRender gbuf = (GLRender)buf;
		if(((CGLEnvironment)gbuf.env).wnd() != this)
		    throw(new IllegalArgumentException());
		if(!(mode instanceof Boolean))
		    throw(new IllegalArgumentException());
		gbuf.submit(gl -> this.glswap(gl, ((Boolean)mode) ? 1 : 0));
	    }

	    public void dispose() {
	    }
	}

	public Windeye window() {
	    return(mainrun(CocoaWindow::new));
	}

	public String description() {
	    return("Cocoa/CGL, OSX " + fnd.processInfo().operatingSystemVersionString());
	}

	public void dispose() {
	}
    }

    private static Map<Character, Key.Std> stdcsyms() {
	Map<Character, Key.Std> ret = new HashMap<>();
	for(Key.Std sym : Key.Std.values()) {
	    if((sym.ch != 0) && !ret.containsKey(sym.ch))
		ret.put(sym.ch, sym);
	}
	return(ret);
    }

    public static final Map<Integer, Key.Std> stdvsyms = Utils.<Integer, Key.Std>map()
	.put(kVK_ANSI_KeypadDecimal, NP_DEC)   .put(kVK_ANSI_KeypadMultiply, NP_MUL)   .put(kVK_ANSI_KeypadPlus, NP_ADD)
	.put(kVK_ANSI_KeypadDivide, NP_DIV)    .put(kVK_ANSI_KeypadMinus, NP_SUB)      .put(kVK_ANSI_Keypad0, NP0)
	.put(kVK_ANSI_Keypad1, NP1)            .put(kVK_ANSI_Keypad2, NP2)             .put(kVK_ANSI_Keypad2, NP2)
	.put(kVK_ANSI_Keypad4, NP4)            .put(kVK_ANSI_Keypad5, NP5)             .put(kVK_ANSI_Keypad6, NP6)
	.put(kVK_ANSI_Keypad7, NP7)            .put(kVK_ANSI_Keypad8, NP8)             .put(kVK_ANSI_Keypad9, NP9)

	.put(kVK_Return, ENTER)       .put(kVK_Tab, TAB)           .put(kVK_Space, SPACE)
	.put(kVK_Delete, BACKSPACE)   .put(kVK_Escape, ESCAPE)     .put(kVK_Shift, SHIFT)
	.put(kVK_CapsLock, CAPSLOCK)  .put(kVK_Option, ALT)        .put(kVK_Control, CONTROL)
	.put(kVK_RightShift, SHIFT)   .put(kVK_RightOption, ALT)   .put(kVK_RightControl, CONTROL)
	.put(kVK_F17, F17)            .put(kVK_F18, F18)           .put(kVK_F19, F19)
	.put(kVK_F20, F20)            .put(kVK_F5, F5)             .put(kVK_F6, F6)
	.put(kVK_F7, F7)              .put(kVK_F3, F3)             .put(kVK_F8, F8)
	.put(kVK_F9, F9)              .put(kVK_F11, F11)           .put(kVK_F13, F13)
	.put(kVK_F16, F16)            .put(kVK_F14, F14)           .put(kVK_F10, F10)
	.put(kVK_F12, F12)            .put(kVK_F15, F15)           .put(kVK_Help, HELP)
	.put(kVK_Home, HOME)          .put(kVK_PageUp, PAGEUP)     .put(kVK_ForwardDelete, DELETE)
	.put(kVK_F4, F4)              .put(kVK_End, END)           .put(kVK_F2, F2)
	.put(kVK_PageDown, PAGEDOWN)  .put(kVK_F1, F1)             .put(kVK_LeftArrow, LEFT)
	.put(kVK_RightArrow, RIGHT)   .put(kVK_DownArrow, DOWN)    .put(kVK_UpArrow, UP)
	.map();

    public static final Map<Character, Key.Std> stdcsyms = stdcsyms();

    public static final List<Key.Std> ctlkeys = Arrays.asList(new Key.Std[] {
	SPACE,
	A, B, C, D, E, F, G, H, I, J, K, L, M,
	N, O, P, Q, R, S, T, U, V, W, X, Y, Z,
	LEFTBRACKET, BACKSLASH, RIGHTBRACKET,
    });

    public static final Map<Integer, Key.Loc.Std> stdkeys = Utils.<Integer, Key.Loc.Std>map()
	.put(kVK_Escape,  Key.Loc.Std.ESC).put(kVK_F1, Key.Loc.Std.FK01).put(kVK_F2, Key.Loc.Std.FK02).put(kVK_F3, Key.Loc.Std.FK03).put(kVK_F4, Key.Loc.Std.FK04)
	.put(kVK_F5, Key.Loc.Std.FK05).put(kVK_F6, Key.Loc.Std.FK06).put(kVK_F7, Key.Loc.Std.FK07).put(kVK_F8, Key.Loc.Std.FK08).put(kVK_F9, Key.Loc.Std.FK09)
	.put(kVK_F10, Key.Loc.Std.FK10).put(kVK_F11, Key.Loc.Std.FK11).put(kVK_F12, Key.Loc.Std.FK12).put(kVK_F13, Key.Loc.Std.PRSC).put(kVK_F14, Key.Loc.Std.SCLK).put(kVK_F15, Key.Loc.Std.PAUS)

	.put(kVK_ANSI_Grave, Key.Loc.Std.TLDE).put(kVK_ANSI_1, Key.Loc.Std.AE01).put(kVK_ANSI_2, Key.Loc.Std.AE02).put(kVK_ANSI_3, Key.Loc.Std.AE03).put(kVK_ANSI_4, Key.Loc.Std.AE04)
	.put(kVK_ANSI_5, Key.Loc.Std.AE05).put(kVK_ANSI_6, Key.Loc.Std.AE06).put(kVK_ANSI_7, Key.Loc.Std.AE07).put(kVK_ANSI_8, Key.Loc.Std.AE08).put(kVK_ANSI_9, Key.Loc.Std.AE09)
	.put(kVK_ANSI_0, Key.Loc.Std.AE10).put(kVK_ANSI_Minus, Key.Loc.Std.AE11).put(kVK_ANSI_Equal, Key.Loc.Std.AE12).put(kVK_Delete, Key.Loc.Std.BKSP).put(kVK_Help, Key.Loc.Std.INS)
	.put(kVK_Home, Key.Loc.Std.HOME).put(kVK_PageUp, Key.Loc.Std.PGUP).put(kVK_ANSI_KeypadClear, Key.Loc.Std.NMLK).put(kVK_ANSI_KeypadDivide, Key.Loc.Std.KPDV).put(kVK_ANSI_KeypadMultiply, Key.Loc.Std.KPMU)
	.put(kVK_ANSI_KeypadMinus, Key.Loc.Std.KPSU)

	.put(kVK_Tab,  Key.Loc.Std.TAB).put(kVK_ANSI_Q, Key.Loc.Std.AD01).put(kVK_ANSI_W, Key.Loc.Std.AD02).put(kVK_ANSI_E, Key.Loc.Std.AD03).put(kVK_ANSI_R, Key.Loc.Std.AD04)
	.put(kVK_ANSI_T, Key.Loc.Std.AD05).put(kVK_ANSI_Y, Key.Loc.Std.AD06).put(kVK_ANSI_U, Key.Loc.Std.AD07).put(kVK_ANSI_I, Key.Loc.Std.AD08).put(kVK_ANSI_O, Key.Loc.Std.AD09)
	.put(kVK_ANSI_P, Key.Loc.Std.AD10).put(kVK_ANSI_LeftBracket, Key.Loc.Std.AD11).put(kVK_ANSI_RightBracket, Key.Loc.Std.AD12).put(kVK_ForwardDelete, Key.Loc.Std.DEL).put(kVK_End,  Key.Loc.Std.END)
	.put(kVK_PageDown, Key.Loc.Std.PGDN).put(kVK_ANSI_Keypad7,  Key.Loc.Std.KP7).put(kVK_ANSI_Keypad8,  Key.Loc.Std.KP8).put(kVK_ANSI_Keypad9,  Key.Loc.Std.KP9).put(kVK_ANSI_KeypadPlus, Key.Loc.Std.KPAD)

	.put(kVK_CapsLock, Key.Loc.Std.CAPS).put(kVK_ANSI_A, Key.Loc.Std.AC01).put(kVK_ANSI_S, Key.Loc.Std.AC02).put(kVK_ANSI_D, Key.Loc.Std.AC03).put(kVK_ANSI_F, Key.Loc.Std.AC04)
	.put(kVK_ANSI_G, Key.Loc.Std.AC05).put(kVK_ANSI_H, Key.Loc.Std.AC06).put(kVK_ANSI_J, Key.Loc.Std.AC07).put(kVK_ANSI_K, Key.Loc.Std.AC08).put(kVK_ANSI_L, Key.Loc.Std.AC09)
	.put(kVK_ANSI_Semicolon, Key.Loc.Std.AC10).put(kVK_ANSI_Quote, Key.Loc.Std.AC11).put(kVK_ANSI_Backslash, Key.Loc.Std.BKSL).put(kVK_Return, Key.Loc.Std.RTRN).put(kVK_ANSI_Keypad4,  Key.Loc.Std.KP4)
	.put(kVK_ANSI_Keypad5,  Key.Loc.Std.KP5).put(kVK_ANSI_Keypad6,  Key.Loc.Std.KP6)

	.put(kVK_Shift, Key.Loc.Std.LFSH).put(kVK_ISO_Section, Key.Loc.Std.LSGT).put(kVK_ANSI_Z, Key.Loc.Std.AB01).put(kVK_ANSI_X, Key.Loc.Std.AB02).put(kVK_ANSI_C, Key.Loc.Std.AB03)
	.put(kVK_ANSI_V, Key.Loc.Std.AB04).put(kVK_ANSI_B, Key.Loc.Std.AB05).put(kVK_ANSI_N, Key.Loc.Std.AB06).put(kVK_ANSI_M, Key.Loc.Std.AB07).put(kVK_ANSI_Comma, Key.Loc.Std.AB08)
	.put(kVK_ANSI_Period, Key.Loc.Std.AB09).put(kVK_ANSI_Slash, Key.Loc.Std.AB10).put(kVK_RightShift, Key.Loc.Std.RTSH).put(kVK_UpArrow, Key.Loc.Std.UP  ).put(kVK_ANSI_Keypad1 , Key.Loc.Std.KP1 )
	.put(kVK_ANSI_Keypad2,  Key.Loc.Std.KP2 ).put(kVK_ANSI_Keypad3,  Key.Loc.Std.KP3 ).put(kVK_ANSI_KeypadEnter, Key.Loc.Std.KPEN)

	.put(kVK_Control, Key.Loc.Std.LCTL).put(kVK_Option, Key.Loc.Std.LWIN).put(kVK_Command, Key.Loc.Std.LALT).put(kVK_Space, Key.Loc.Std.SPCE).put(kVK_RightOption, Key.Loc.Std.RALT)
	.put(0x6e, Key.Loc.Std.MENU).put(kVK_RightControl, Key.Loc.Std.RCTL).put(kVK_LeftArrow, Key.Loc.Std.LEFT).put(kVK_DownArrow, Key.Loc.Std.DOWN)
	.put(kVK_RightArrow, Key.Loc.Std.RGHT).put(kVK_ANSI_Keypad0,  Key.Loc.Std.KP0 ).put(kVK_ANSI_KeypadDecimal, Key.Loc.Std.KPDL)
	.map();
}
