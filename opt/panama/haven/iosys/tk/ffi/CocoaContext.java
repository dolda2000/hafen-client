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
import haven.ffi.objc.Runtime;
import static haven.iosys.tk.Key.Std.*;

@Toolkit.Available(name = "cgl")
public class CocoaContext implements Providers.Factory<Toolkit> {
    private final Runtime rt;
    private final Foundation fnd;
    private final CoreGraphics cg;
    private final AppKit ak;
    private final CGL cgl;
    private final OpenGL gl;

    private CocoaContext() {
	try {
	    rt = Runtime.get();
	    fnd = Foundation.get();
	    cg = CoreGraphics.get();
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
	return(new CocoaToolkit());
    }

    public int priority() {
	return(System.getProperty("os.name", "").startsWith("Mac OS") ? 100 : 0);
    }
    public boolean experimental() {return(true);}
    public boolean autouse() {return(false);}

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

    public class CocoaToolkit implements Toolkit {
	private static final int glversions[] = {
	    CGL.NSOpenGLProfileVersion4_1Core,
	    CGL.NSOpenGLProfileVersion3_2Core,
	    CGL.NSOpenGLProfileVersionLegacy,
	};
	public final NSApplication app;
	public final NSOpenGLContext ctx;

	private CocoaToolkit() {
	    try {
		javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
	    } catch(Exception e) {
		throw(new RuntimeException(e));
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

	public Cursor.Caps cursorcaps() {
	    return(null);
	}

	public Cursor makecursor(BufferedImage img, Coord hs) {
	    return(null);
	}

	void mainrun(Runnable task) {
	    rt.mainrun(task);
	}

	private <T> T mainrun(Supplier<T> task) {
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

	public class CocoaWindow implements Windeye {
	    public final NSWindow nsw;
	    public final NSView view;
	    private final Collection<EventListener> callbacks = new java.util.concurrent.CopyOnWriteArrayList<>();
	    private boolean shown = false;
	    private Sizing sizeinfo = new Sizing().normsize(Coord.of(800, 600));
	    private CGLEnvironment renv;
	    private Coord size = Coord.z;

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
		nsw = mainrun(() -> ak.NSWindow(cg.CGRect(Area.sized(Coord.of(1, 1))), 
						AppKit.NSWindowStyleMaskTitled |
						AppKit.NSWindowStyleMaskClosable |
						AppKit.NSWindowStyleMaskMiniaturizable |
						AppKit.NSWindowStyleMaskResizable,
						AppKit.NSBackingStoreBuffered,
						true));
		mainrun(() -> {
		    nsw.setDelegate(new WindowDelegate());
		    nsw.setAcceptsMouseMovedEvents(true);
		});
		view = mainrun(() -> ak.NSView(new ViewDelegate(), cg.CGRect(Area.sized(Coord.of(1, 1)))));
		mainrun(() -> {
		    view.setWantsBestResolutionOpenGLSurface(true);
		    nsw.setContentView(view);
		});
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
		private final StringBuilder textbuf = new StringBuilder();
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
		    double x = event.scrollingDeltaX();
		    double y = event.scrollingDeltaY();
		    if(event.hasPreciseScrollingDeltas()) {
			x /= 15;
			y /= 15;
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
		    if(textbuf.length() > 0) {
			Warning.warn("unexpected text accumulated at beginning of keyDown: " + textbuf);
			textbuf.setLength(0);
		    }
		    view.interpretKeyEvents(fnd.NSArray(event));
		    Debug.dump(Utils.bprint.enc(textbuf.toString().getBytes()), event.keyCode());
		    textbuf.setLength(0);
		}

		public void insertText(String string) {
		    textbuf.append(string);
		}

		private final Runtime.SEL sel_insertNewline = rt.sel_registerName("insertNewline:");
		private final Runtime.SEL sel_insertTab = rt.sel_registerName("insertTab:");
		private final Runtime.SEL sel_deleteBackward = rt.sel_registerName("deleteBackward:");
		public void doCommandBySelector(Runtime.SEL selector) {
		    if(selector.equals(sel_insertNewline)) {
			textbuf.append('\n');
		    } else if(selector.equals(sel_insertTab)) {
			textbuf.append('\t');
		    } else if(selector.equals(sel_deleteBackward)) {
			textbuf.append('\b');
		    }
		}
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

	    public CocoaWindow show(boolean show) {
		mainrun(() -> {
		    if(!shown) {
			if(show) {
			    updatesizing(sizeinfo);
			    nsw.cascadeTopLeftFromPoint(cg.CGPoint(Coord.z));
			    nsw.makeKeyAndOrderFront(null);
			    shown = true;
			}
		    } else {
			if(show) {
			    nsw.makeKeyAndOrderFront(null);
			} else {
			    nsw.orderOut(null);
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

	    public CocoaWindow cursor(Cursor curs) {
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
		return(this);
	    }

	    public Coord size() {
		return(size);
	    }

	    public State state() {
		return(State.NORMAL);
	    }

	    public boolean focused() {
		return(true);
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

	    private void glswap(GL gl, int ival) {
		GLException.checkfor(gl, null);
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
	    return(new CocoaWindow());
	}

	public String description() {
	    return("Cocoa/CGL, OSX " + fnd.processInfo().operatingSystemVersionString());
	}

	public void dispose() {
	}
    }
}
