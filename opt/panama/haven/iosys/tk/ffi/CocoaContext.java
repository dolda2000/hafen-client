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
import haven.ffi.objc.Runtime;
import static haven.iosys.tk.Key.Std.*;

@Toolkit.Available(name = "cgl")
public class CocoaContext implements Providers.Factory<Toolkit> {
    private final Runtime rt;
    private final Foundation fnd;
    private final AppKit ak;

    private CocoaContext() {
	try {
	    rt = Runtime.get();
	    fnd = Foundation.get();
	    ak = AppKit.get();
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
		rt.mainrun(app::run);
	    }
	    return(app);
	}
    }

    public class CocoaToolkit implements Toolkit {
	public final NSApplication app = app();

	private CocoaToolkit() {
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

	public class CocoaWindow implements Windeye {
	    public final NSWindow nsw;
	    private final Collection<EventListener> callbacks = new java.util.concurrent.CopyOnWriteArrayList<>();
	    private boolean shown = false;
	    private Sizing sizeinfo = new Sizing().normsize(Coord.of(800, 600));

	    private CocoaWindow() {
		nsw = mainrun(() -> ak.NSWindow(Area.sized(Coord.of(1, 1)), 
						AppKit.NSWindowStyleMaskTitled |
						AppKit.NSWindowStyleMaskClosable |
						AppKit.NSWindowStyleMaskMiniaturizable |
						AppKit.NSWindowStyleMaskResizable,
						AppKit.NSBackingStoreBuffered,
						true));
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
		    nsw.setContentSize(info.fixsize);
		} else {
		    nsw.setStyleMask(nsw.styleMask() | AppKit.NSWindowStyleMaskResizable);
		    if(info.normsize != null)
			nsw.setContentSize(info.normsize);
		    if(info.minsize != null)
			nsw.setContentMinSize(info.minsize);
		    if(info.maxsize != null)
			nsw.setContentMaxSize(info.maxsize);
		}
	    }

	    public CocoaWindow show(boolean show) {
		mainrun(() -> {
		    if(!shown) {
			if(show) {
			    updatesizing(sizeinfo);
			    nsw.cascadeTopLeftFromPoint(Coord.z);
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
		return(null);
	    }

	    public State state() {
		return(State.NORMAL);
	    }

	    public boolean focused() {
		return(true);
	    }

	    public Environment env() {
		return(null);
	    }

	    private static final Pipe.Op glfb = Pipe.Op.compose(new FragColor<>(FragColor.defcolor),
								new DepthBuffer<>(DepthBuffer.defdepth));
	    public Pipe.Op fbstate() {
		return(glfb);
	    }

	    public void swapbuffers(Render out, Object mode) {
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
