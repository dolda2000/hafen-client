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
import java.nio.file.*;
import haven.*;
import haven.iosys.*;
import haven.render.*;
import haven.render.gl.*;
import haven.iosys.tk.*;
import haven.ffi.*;
import haven.ffi.posix.*;
import haven.ffi.x11.*;
import haven.ffi.gl.*;
import haven.ffi.dbus.*;
import haven.ffi.x11.XLib.*;
import haven.ffi.x11.XInput.*;
import haven.ffi.posix.FileDescriptor;
import static haven.ffi.x11.XKeysym.*;
import static haven.iosys.tk.Key.Std.*;

@Toolkit.Available(name = "glx")
public class GLXContext implements Providers.Factory<Toolkit> {
    public static final boolean DEBUG = false;
    private final LibC libc;
    private final XLib xlib;
    private final XInput xi;
    private final GLX glx;
    private final OpenGL gl;
    private final Xcursor xcr;
    private final Xrandr xrr;

    private GLXContext() {
	try {
	    this.libc = LibC.get();
	    this.xlib = XLib.get();
	    this.xi = XInput.get();
	    this.glx = GLX.get();
	    this.gl = glx.gl();
	    this.xcr = Xcursor.get();
	    this.xrr = Xrandr.get();
	} catch(RuntimeException e) {
	    throw(new Unavailable("X11 libraries not available", e));
	}
	xlib.XInitThreads();
	xlib.XSetErrorHandler();
	xlib.XrmInitialize();
    }

    private static GLXContext instance = null;
    public static GLXContext get() {
	if(instance == null) {
	    synchronized(GLXToolkit.class) {
		if(instance == null)
		    instance = new GLXContext();
	    }
	}
	return(instance);
    }

    public Toolkit open(String... args) {
	return(new GLXToolkit((args.length == 0) ? null : args[0], -1));
    }

    public int priority() {
	return(System.getProperty("os.name", "").equals("Linux") ? 100 : 0);
    }
    public boolean experimental() {return(false);}

    public static class XCursor implements Cursor {
	public static final XCursor inherit = new XCursor(null, XID.None);
	public final XID id;
	private final Runnable clean;

	public XCursor(Display dpy, XID id) {
	    this.id = id;
	    if(dpy != null)
		clean = Finalizer.finalize(this, () -> {if(!dpy.closed) dpy.lib.XFreeCursor(dpy, id);});
	    else
		clean = null;
	}

	public void dispose() {
	    clean.run();
	}
    }

    public class GLXToolkit implements Toolkit {
	private static final int[][] glversions = {
	    {4, 6}, {4, 5}, {4, 4}, {4, 3}, {4, 2}, {4, 1}, {4, 0},
				    {3, 3}, {3, 2}, {3, 1}, {3, 0},
						    {2, 1}, {2, 0},
	};
	private static final int[] fbattribs = {
	    GLX.GLX_DRAWABLE_TYPE, GLX.GLX_WINDOW_BIT,
	    GLX.GLX_RENDER_TYPE, GLX.GLX_RGBA_BIT,
	    GLX.GLX_DOUBLEBUFFER, 1,
	    GLX.GLX_DEPTH_SIZE, 24, GLX.GLX_RED_SIZE, 8, GLX.GLX_GREEN_SIZE, 8, GLX.GLX_BLUE_SIZE, 8,
	    XLib.None
	};
	public final Display dpy;
	public final int nscreen;
	public final Screen screen;
	public final ExtensionInfo ext_xi, ext_glx;
	public final XkbExtensionInfo ext_xkb;
	public final List<String> exts;
	public final GLX.GLXFBConfig fb;
	public final XLib.XVisualInfo vis;
	public final XID colormap;
	public final GLX.GLXContext ctx;
	public final XIM im;
	public final long imstyle;
	public final int mincode, maxcode;
	public final XID[][] keymap;
	public final int mod_alt, mod_meta, mod_altgr, mod_super;
	public final Map<Integer, Integer> rmodmap = new HashMap<>();
	public final Map<Integer, XIPointerInfo> pointers = new HashMap<>();
	public final Cursor.Caps ccaps;
	public final Xrandr.XRRExtensionInfo xrrinfo;
	public final XrmDatabase xrdb;
	public final Atomic ATOM = new Atomic("ATOM");
	public final Atomic CARDINAL = new Atomic("CARDINAL");
	public final Atomic WINDOW = new Atomic("WINDOW");
	public final Atomic STRING = new Atomic("STRING");
	public final Atomic UTF8_STRING = new Atomic("UTF8_STRING");
	public final Atomic WM_NAME = new Atomic("WM_NAME");
	public final Atomic WM_PROTOCOLS = new Atomic("WM_PROTOCOLS");
	public final Atomic WM_DELETE_WINDOW = new Atomic("WM_DELETE_WINDOW");
	public final Atomic _NET_WM_NAME = new Atomic("_NET_WM_NAME");
	public final Atomic _NET_WM_ICON = new Atomic("_NET_WM_ICON");
	public final Atomic _NET_WM_STATE = new Atomic("_NET_WM_STATE");
	public final Atomic _NET_WM_STATE_HIDDEN = new Atomic("_NET_WM_STATE_HIDDEN");
	public final Atomic _NET_WM_STATE_MAXIMIZED_VERT = new Atomic("_NET_WM_STATE_MAXIMIZED_VERT");
	public final Atomic _NET_WM_STATE_MAXIMIZED_HORZ = new Atomic("_NET_WM_STATE_MAXIMIZED_HORZ");
	public final Atomic _NET_WM_STATE_FULLSCREEN = new Atomic("_NET_WM_STATE_FULLSCREEN");
	public final Atomic _NET_WM_PID = new Atomic("_NET_WM_PID");
	public final Atomic _NET_WM_PING = new Atomic("_NET_WM_PING");
	public final Atomic _NET_SUPPORTING_WM_CHECK = new Atomic("_NET_SUPPORTING_WM_CHECK");
	public final Atomic PRIMARY = new Atomic("PRIMARY");
	public final Atomic CLIPBOARD = new Atomic("CLIPBOARD");
	public final Atomic TARGETS = new Atomic("TARGETS");
	public final Atomic INCR = new Atomic("INCR");
	public final Atomic SELECTED_DATA = new Atomic("SELECTED_DATA");
	public final Atomic IMAGE_PNG = new Atomic("image/png");
	public final Atomic IMAGE_JPEG = new Atomic("image/jpeg");
	public final Atomic TEXT_PLAIN = new Atomic("text/plain");
	public final Atomic TEXT_PLAIN_UTF8 = new Atomic("text/plain;charset=utf8");
	public final Atomic TEXT_URILIST = new Atomic("text/uri-list");
	public final Atomic XdndAware = new Atomic("XdndAware");
	public final Atomic XdndSelection = new Atomic("XdndSelection");
	public final Atomic XdndTypeList = new Atomic("XdndTypeList");
	public final Atomic XdndActionList = new Atomic("XdndActionList");
	public final Atomic XdndActionDescription = new Atomic("XdndActionDescription");
	public final Atomic XdndEnter = new Atomic("XdndEnter");
	public final Atomic XdndLeave = new Atomic("XdndLeave");
	public final Atomic XdndPosition = new Atomic("XdndPosition");
	public final Atomic XdndStatus = new Atomic("XdndStatus");
	public final Atomic XdndDrop = new Atomic("XdndDrop");
	public final Atomic XdndFinished = new Atomic("XdndFinished");
	public final Atomic XdndActionCopy = new Atomic("XdndActionCopy");
	public final Atomic XdndActionMove = new Atomic("XdndActionMove");
	public final Atomic XdndActionLink = new Atomic("XdndActionLink");
	public final String srvvendor, wmname;
	public final int srvrelease;
	private boolean closed = false;

	public class Atomic {
	    public final String name;
	    private Atom id = null;

	    public Atomic(String name) {
		this.name = name;
	    }

	    public boolean is(Atom a) {
		if(id == null)
		    id = dpy.lib.XInternAtom(dpy, name, false);
		return(a.equals(id));
	    }
	}

	private void prepare(Atomic... atoms) {
	    String[] names = new String[atoms.length];
	    for(int i = 0; i < atoms.length; i++)
		names[i] = atoms[i].name;
	    Atom[] ret = xlib.XInternAtoms(dpy, names, false);
	    for(int i = 0; i < atoms.length; i++)
		atoms[i].id = ret[i];
	}

	private GLX.GLXContext createctx() {
	    GLX.GLXContext ret = null;
	    XException lasterr = null;
	    for(int[] ver : glversions) {
		int[] ctxattribs = {
		    GLX.GLX_CONTEXT_PROFILE_MASK_ARB, GLX.GLX_CONTEXT_CORE_PROFILE_BIT_ARB,
		    GLX.GLX_CONTEXT_MAJOR_VERSION_ARB, ver[0], GLX.GLX_CONTEXT_MINOR_VERSION_ARB, ver[1],
		    XLib.None,
		};
		try {
		    ret = glx.glXCreateContextAttribsARB(dpy, fb, null, true, ctxattribs);
		} catch(XException e) {
		    if(e.request_code != ext_glx.opcode)
			throw(e);
		    lasterr = e;
		    continue;
		}
		break;
	    }
	    if(ret == null)
		throw(new Unavailable("could not create OpenGL context", lasterr));
	    return(ret);
	}

	public class XIPointerInfo {
	    public final String[] buttons;
	    public final Map<Integer, Scroll> scroll = new HashMap<>();

	    public class Scroll {
		public final int type;
		public final double inc;
		public double lastval;

		private Scroll(XIScrollClassInfo si, XIValuatorClassInfo vi) {
		    this.type = si.scroll_type();
		    this.inc = si.increment();
		    this.lastval = vi.value();
		}
	    }

	    private XIPointerInfo(XIDeviceInfo dev, Map<Integer, XIDeviceInfo> devs) {
		String[] buttons = new String[0];
		Map<Integer, XIValuatorClassInfo> vals = new IntMap<>();
		for(XIAnyClassInfo cl : dev.classes()) {
		    if(cl.type() == XInput.XIButtonClass) {
			XIButtonClassInfo bi = cl.button();
			Atom[] labels = bi.labels();
			for(int i = 0; i < labels.length; i++) {
			    if(labels[i] == null)
				continue;
			    Atom lbl = labels[i];
			    check: for(int o = 0; o < i; o++) {
				if(lbl.equals(labels[o])) {
				    for(int u = 0; u < labels.length; u++) {
					if(lbl.equals(labels[u]))
					    labels[u] = null;
				    }
				    break check;
				}
			    }
			}
			buttons = new String[labels.length];
			for(int i = 0; i < labels.length; i++) {
			    buttons[i] = (labels[i] == null) ? null : xlib.XGetAtomName(dpy, labels[i]);
			}
		    } else if(cl.type() == XInput.XIValuatorClass) {
			XIValuatorClassInfo vi = cl.valuator();
			/* Apparently, master pointer valuators can
			 * sometimes be invalid from XIQueryDevice,
			 * even though the values from the slave are
			 * correct... */
			XIDeviceInfo src = devs.get(vi.sourceid());
			if(src != null) {
			    for(XIAnyClassInfo scl : src.classes()) {
				if(scl.type() == XInput.XIValuatorClass) {
				    XIValuatorClassInfo svi = scl.valuator();
				    if(svi.label().equals(vi.label()))
					vals.put(vi.number(), svi);
				}
			    }
			}
		    }
		}
		for(XIAnyClassInfo cl : dev.classes()) {
		    if(cl.type() == XInput.XIScrollClass) {
			XIScrollClassInfo si = cl.scroll();
			if(vals.containsKey(si.number())) {
			    if((si.scroll_type() == XInput.XIScrollTypeVertical) || (si.scroll_type() == XInput.XIScrollTypeHorizontal)) {
				scroll.put(si.number(), new Scroll(si, vals.get(si.number())));
			    }
			}
		    }
		}
		this.buttons = buttons;
	    }
	}

	public Map<Integer, XIDeviceInfo> devicemap() {
	    Map<Integer, XIDeviceInfo> ret = new IntMap<>();
	    xi.XIQueryDevice(dpy, XInput.XIAllDevices).forEach(dev -> ret.put(dev.deviceid(), dev));
	    return(ret);
	}

	public GLXToolkit(String display, int nscreen) {
	    boolean done = false;
	    try {
		if(!xlib.XkbLibraryVersion(new int[] {XLib.XkbMajorVersion, XLib.XkbMinorVersion}))
		    throw(new Unavailable("XKB client support unavailable"));
		if((this.dpy = xlib.XOpenDisplay(display)) == null)
		    throw(new Unavailable("Cannot open X11 display"));
		if(nscreen < 0)
		    nscreen = xlib.DefaultScreen(dpy);
		else if(nscreen >= xlib.ScreenCount(dpy))
		    throw(new IllegalArgumentException("no such screen: " + nscreen));
		this.nscreen = nscreen;
		this.screen = xlib.ScreenOfDisplay(dpy, nscreen);
		srvvendor = xlib.XServerVendor(dpy);
		srvrelease = xlib.XVendorRelease(dpy);

		/* Atoms */
		prepare(ATOM, CARDINAL, WINDOW, STRING, UTF8_STRING,
			WM_NAME, WM_PROTOCOLS, WM_DELETE_WINDOW,
			_NET_WM_NAME, _NET_WM_ICON, _NET_WM_PID, _NET_WM_PING,
			_NET_WM_STATE, _NET_WM_STATE_MAXIMIZED_VERT, _NET_WM_STATE_MAXIMIZED_HORZ,
			_NET_WM_STATE_HIDDEN, _NET_WM_STATE_FULLSCREEN,
			_NET_SUPPORTING_WM_CHECK,
			PRIMARY, CLIPBOARD, TARGETS, INCR, SELECTED_DATA,
			IMAGE_PNG, IMAGE_JPEG, TEXT_PLAIN, TEXT_PLAIN_UTF8, TEXT_URILIST,
			XdndAware, XdndSelection, XdndTypeList, XdndActionList,
			XdndEnter, XdndLeave, XdndPosition, XdndStatus, XdndDrop, XdndFinished,
			XdndActionCopy, XdndActionMove, XdndActionLink
			);

		/* xrdb */
		{
		    XrmDatabase xrdb = xlib.XrmGetStringDatabase(xlib.XResourceManagerString(dpy));
		    xrdb = xlib.XrmMergeDatabases(xlib.XrmGetStringDatabase(xlib.XScreenResourceString(screen)), xrdb);
		    xrdb = xlib.XrmMergeDatabases(xlib.XrmGetFileDatabase(Utils.pj(Utils.path(System.getProperty("user.home")), ".Xdefaults").toString()), xrdb);
		    xrdb = xlib.XrmMergeDatabases(xlib.XrmGetStringDatabase(System.getenv("XENVIRONMENT")), xrdb);
		    this.xrdb = xrdb;
		}

		/* Keyboard input */
		{
		    if((ext_xkb = xlib.XkbQueryExtension(dpy, XLib.XkbMajorVersion, XLib.XkbMinorVersion)) == null)
			throw(new Unavailable("XKB is not supported"));
		    boolean[] supported = {false};
		    xlib.XkbSetDetectableAutoRepeat(dpy, true, supported);
		    if(!supported[0])
			Warning.warn("detectable auto-repeat is not available");
		}
		{
		    XIM im = xlib.XOpenIM(dpy, null, null);
		    long cst = 0;
		    if(im != null) {
			long[] styles = ((XIMStyles)xlib.XGetIMValues(im, Arrays.asList(XLib.XNQueryInputStyle)).get(XLib.XNQueryInputStyle)).styles();
			Arrays.sort(styles);
			if(Arrays.binarySearch(styles, XLib.XIMPreeditNothing | XLib.XIMStatusNothing) >= 0)
			    cst = XLib.XIMPreeditNothing | XLib.XIMStatusNothing;
			else if(Arrays.binarySearch(styles, XLib.XIMPreeditNone | XLib.XIMStatusNone) >= 0)
			    cst = XLib.XIMPreeditNone | XLib.XIMStatusNone;
			else
			    cst = 0;
		    } else {
			Warning.warn("could not open X11 keyboard input method");
		    }
		    if((im != null) && (cst != 0)) {
			this.im = im;
			this.imstyle = cst;
			
		    } else {
			if(im != null)
			    xlib.XCloseIM(im);
			this.im = null;
			this.imstyle = 0;
		    }
		}
		{
		    int[] ival = xlib.XDisplayKeycodes(dpy);
		    mincode = ival[0]; maxcode = ival[1];
		    keymap = xlib.XGetKeyboardMapping(dpy, mincode, maxcode + 1 - mincode);
		    int[][] modmap = xlib.XGetModifierMapping(dpy).mapping();
		    for(int i = 0; i < 8; i++) {
			for(int key : modmap[i])
			    rmodmap.put(key, i);
		    }
		    int mod_alt = 0, mod_meta = 0, mod_altgr = 0, mod_super = 0;
		    for(int i = 3; i < 8; i++) {
			for(int key : modmap[i]) {
			    for(XID sym : keymap[key - mincode]) {
				if(sym.equals(XK_Alt_L) || sym.equals(XK_Alt_R)) {
				    mod_alt = i;
				} else if(sym.equals(XK_Meta_L) || sym.equals(XK_Meta_R)) {
				    mod_meta = i;
				} else if(sym.equals(XK_ISO_Level3_Shift)) {
				    mod_altgr = i;
				} else if(sym.equals(XK_Super_L) || sym.equals(XK_Super_R)) {
				    mod_super = i;
				}
			    }
			}
		    }
		    if((mod_alt == 0) && (mod_meta == 0))
			Warning.warn("found no Alt or Meta modifiers on X11 display");
		    this.mod_alt = mod_alt; this.mod_meta = mod_meta; this.mod_altgr = mod_altgr; this.mod_super = mod_super;
		}

		/* XInput2 */
		if((ext_xi = xlib.XQueryExtension(dpy, "XInputExtension")) == null)
		    throw(new Unavailable("XInputExtension is not supported"));

		{
		    int ver[] = {2, 2};
		    if((xi.XIQueryVersion(dpy, ver) != XLib.Success) || ((ver[0] < 2) || ((ver[0] == 2) && (ver[1] < 2))))
			throw(new Unavailable("XInput >=2.2 not supported"));
		    XIEventMask mask = new XIEventMask(XInput.XIAllDevices);
		    mask.set(XInput.XI_DeviceChanged, XInput.XI_HierarchyChanged);
		    xi.XISelectEvents(dpy, screen.root(), Arrays.asList(mask));
		    refreshxi();
		    if(false) {
			for(XInput.XIDeviceInfo dev : xi.XIQueryDevice(dpy, XInput.XIAllDevices)) {
			    Debug.dump(dev, dev.name());
			    for(XInput.XIAnyClassInfo cl : dev.classes()) {
				if(cl.type() == XInput.XIValuatorClass) {
				    XInput.XIValuatorClassInfo v = cl.valuator();
				    Debug.dump(v, xlib.XGetAtomName(dpy, v.label()));
				} else if(cl.type() == XInput.XIScrollClass) {
				    Debug.dump(cl.scroll());
				}
			    }
			}
		    }
		}

		/* Xcursor */
		{
		    if(xcr.XcursorSupportsARGB(dpy)) {
			Coord maxcursor = xlib.XQueryBestCursor(dpy, screen.root(), Coord.of(512, 512));
			ccaps = new Cursor.Caps(Math.min(maxcursor.x, maxcursor.y), xcr.XcursorGetDefaultSize(dpy));
		    } else {
			ccaps = null;
		    }
		    emptycurs = makecursor(PUtils.rasterimg(PUtils.imgraster(Coord.of(1, 1))), Coord.z);
		}

		/* Xrandr */
		{
		    if((xrrinfo = xrr.XRRQueryExtension(dpy)) == null)
			Warning.warn("Xrandr is not supported on display");
		}

		/* GLX */
		{
		    if((ext_glx = xlib.XQueryExtension(dpy, "GLX")) == null)
			throw(new Unavailable("GLX is not supported"));
		    String extstr = glx.glXQueryExtensionsString(dpy, nscreen);
		    exts = Arrays.asList(((extstr == null) ? "" : extstr).split(" "));
		    if(!exts.contains("GLX_ARB_create_context_profile"))
			throw(new Unavailable("GLX_ARB_create_context_profile not supported"));

		    GLX.GLXFBConfig[] fbs = glx.glXChooseFBConfig(dpy, nscreen, fbattribs);
		    if(fbs.length == 0)
			throw(new Unavailable("no suitable framebuffer configuration available"));
		    fb = fbs[0];
		    vis = glx.glXGetVisualFromFBConfig(dpy, fb);
		    colormap = xlib.XCreateColormap(dpy, screen.root(), vis.visual(), XLib.AllocNone);
		    this.ctx = createctx();
		}

		/* WM info */
		{
		    String wmname = "Unknown";
		    XProperty wmw = xlib.XGetWindowProperty(dpy, screen.root(), _NET_SUPPORTING_WM_CHECK.id, false, Atom.nil);
		    if((wmw.format == 32) && wmw.type.equals(WINDOW.id)) {
			XProperty nm = xlib.XGetWindowProperty(dpy, wmw.x(0), _NET_WM_NAME.id, false, Atom.nil);
			if(nm.format == 8) {
			    wmname = new String(nm.b(), ABI.C_CHARSET);
			}
		    }
		    this.wmname = wmname;
		}

		done = true;
	    } finally {
		if(!done)
		    dispose();
	    }
	}

	private final FileDescriptor rwake, wwake; {
	    int[] pipe = libc.pipe();
	    rwake = FileDescriptor.of(pipe[0]);
	    wwake = FileDescriptor.of(pipe[1]);
	}

	private void refreshxi() {
	    Map<Integer, XIDeviceInfo> devs = devicemap();
	    synchronized(pointers) {
		pointers.clear();
		for(XIDeviceInfo dev : devs.values()) {
		    if(dev.use() == XInput.XIMasterPointer)
			pointers.put(dev.deviceid(), new XIPointerInfo(dev, devs));
		}
	    }
	}

	public static interface EventWindow {
	    public XID windowid();
	    public void event(XEvent ev);
	    public default void event(XIDeviceEvent ev) {
		Warning.warn("unexpected XIDeviceEvent received for %s: %d", this, ev.evtype());
	    }
	    public default void event(XIFocusEvent ev) {
		Warning.warn("unexpected XIFocusEvent received for %s: %d", this, ev.evtype());
	    }
	}

	private void dispatch(XIDeviceEvent ev) {
	    XID id;
	    id = ev.event();
	    EventWindow wnd;
	    synchronized(etmon) {
		wnd = windows.get(id);
	    }
	    if(wnd == null) {
		Warning.warn(String.format("XInput event received for non-registered window %s: %d", id, ev.evtype()));
		return;
	    }
	    wnd.event(ev);
	}

	private void dispatch(XIFocusEvent ev) {
	    XID id;
	    id = ev.event();
	    EventWindow wnd;
	    synchronized(etmon) {
		wnd = windows.get(id);
	    }
	    if(wnd == null) {
		Warning.warn(String.format("XInput event received for non-registered window %s: %d", id, ev.evtype()));
		return;
	    }
	    wnd.event(ev);
	}

	private void dispatch(XEvent ev) {
	    if(xlib.XFilterEvent(ev, XID.None))
		return;
	    if(ev.type() == XLib.GenericEvent) {
		XGenericEvent ge = ev.xgeneric();
		if(ge.extension() == ext_xi.opcode) {
		    switch(ge.evtype()) {
		    case XInput.XI_DeviceChanged, XInput.XI_HierarchyChanged:
			xrun(this::refreshxi);
			break;
		    case XInput.XI_KeyPress, XInput.XI_KeyRelease, XInput.XI_ButtonPress, XInput.XI_ButtonRelease, XInput.XI_Motion:
			dispatch(xi.XIDeviceEvent(ge));
			break;
		    case XInput.XI_Enter, XInput.XI_Leave, XInput.XI_FocusIn, XInput.XI_FocusOut:
			dispatch(xi.XIFocusEvent(ge));
			break;
		    default:
			Warning.warn(String.format("unexpected XInput event received: %d", ge.evtype()));
			break;
		    }
		} else {
		    Warning.warn(String.format("unexpected generic event received: opcode=%d, type=%d", ge.extension(), ge.evtype()));
		}
	    } else {
		XID id = ev.window();
		EventWindow wnd;
		synchronized(etmon) {
		    wnd = windows.get(id);
		}
		if(wnd == null) {
		    if(DEBUG)
			Debug.dump(String.format("event received for non-registered window %s: %d", id, ev.type()));
		    return;
		}
		wnd.event(ev);
	    }
	}

	private final Object etmon = new Object();
	private Thread evthread = null;
	private int etref = 0;
	private final Map<XID, EventWindow> windows = new HashMap<>();
	private final Queue<Runnable> xtasks = new LinkedList<>();
	private void handleevents() {
	    try {
		LibC.PollFD pfd = libc.pollfd(2);
		while(true) {
		    boolean block = true;
		    Runnable task;
		    synchronized(etmon) {
			task = xtasks.poll();
			if(!xtasks.isEmpty())
			    block = false;
			if((task == null) && block && (etref == 0) && windows.isEmpty()) {
			    evthread = null;
			    return;
			}
		    }
		    if(task != null)
			task.run();
		    while(xlib.XPending(dpy) > 0) {
			XLib.XEvent ev = xlib.XEvent();
			xlib.XNextEvent(dpy, ev);
			dispatch(ev);
		    }
		    pfd.fd(0, xlib.ConnectionNumber(dpy)).events(0, LibC.POLLIN).revents(0, 0);
		    pfd.fd(1, rwake.fileno).events(1, LibC.POLLIN).revents(1, 0);
		    try {
			int rv = libc.poll(pfd, 2, block ? -1 : 0);
		    } catch(StdError e) {
			if(e.errno == LibC.EINTR)
			    continue;
			throw(e);
		    }
		    if((pfd.revents(1) & LibC.POLLIN) != 0)
			libc.read(rwake.fileno, 1024);
		}
	    } finally {
		synchronized(etmon) {
		    if(evthread == Thread.currentThread())
			evthread = null;
		}
	    }
	}

	private class Aliveness implements AutoCloseable {
	    Aliveness() {
		synchronized(etmon) {
		    etref++;
		}
	    }

	    public void close() {
		synchronized(etmon) {
		    etref--;
		}
	    }
	}

	private void ckevthread() {
	    if(evthread == null) {
		Thread th = new HackThread(this::handleevents, "X11 dispatch thread");
		th.start();
		evthread = th;
	    }
	}

	private void xrun(Runnable task) {
	    synchronized(etmon) {
		xtasks.add(task);
		ckevthread();
	    }
	    while(libc.write(wwake.fileno, new byte[] {0}) == 0);
	}

	private <T> T xrun(Supplier<T> task) {
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
	    xrun(r);
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

	private void glrun(XID wnd, Runnable task) {
	    xrun(() -> {
		if(!glx.glXMakeCurrent(dpy, wnd, ctx))
		    throw(new RuntimeException("glXMakeCurrent failed without reason"));
		try {
		    task.run();
		} finally {
		    glx.glXMakeCurrent(dpy, XLib.XID.None, null);
		}
	    });
	}

	private <T> T glrun(XID wnd, Supplier<T> task) {
	    return(xrun(() -> {
		if(!glx.glXMakeCurrent(dpy, wnd, ctx))
		    throw(new RuntimeException("glXMakeCurrent failed without reason"));
		T ret;
		try {
		    ret = task.get();
		} finally {
		    glx.glXMakeCurrent(dpy, XLib.XID.None, null);
		}
		return(ret);
	    }));
	}

	private void register(EventWindow wnd) {
	    synchronized(etmon) {
		windows.put(wnd.windowid(), wnd);
		ckevthread();
	    }
	}

	private void unregister(EventWindow wnd) {
	    synchronized(etmon) {
		windows.remove(wnd.windowid());
	    }
	}

	public class GLXWindow implements Windeye, EventWindow {
	    public final XID id;
	    public final XIC ic;
	    private final Collection<Predicate<XEvent>> listeners = new ArrayList<>();
	    private final Collection<EventListener> callbacks = new java.util.concurrent.CopyOnWriteArrayList<>();
	    private boolean showing = false;
	    private boolean mapped, focused;
	    private GLXEnvironment renv;
	    private Coord size = Coord.z;
	    private int visibility = 0;
	    private int cursi = -1;
	    private DropHandler drop = null;
	    private Xdnd dropping = null;

	    public class GLXEnvironment extends FFIEnvironment {
		private int qstate;

		private GLXEnvironment() {
		    super(gl, Area.sized(size));
		}

		private void process() {
		    synchronized(this) {
			qstate = 2;
		    }
		    process(gl);
		    synchronized(this) {
			if((qstate & 1) != 0)
			    glrun(id, (Runnable)this::process);
			qstate &= ~2;
		    }
		}

		public void submit(Render cmd) {
		    super.submit(cmd);
		    synchronized(this) {
			if(renv == this) {
			    if(qstate == 0)
				glrun(id, (Runnable)this::process);
			    qstate |= 1;
			}
		    }
		}

		public GLXWindow wnd() {
		    return(GLXWindow.this);
		}
	    }

	    public GLXWindow() {
		boolean done = false;
		try {
		    XSetWindowAttributes attrs = xlib.XSetWindowAttributes();
		    long values = XLib.CWColormap | XLib.CWEventMask;
		    attrs.colormap(colormap);
		    long evmask = XLib.StructureNotifyMask | XLib.FocusChangeMask | XLib.PropertyChangeMask;
		    evmask |= XLib.VisibilityChangeMask | XLib.KeyPressMask | XLib.KeyReleaseMask;
		    try(Aliveness _ = new Aliveness()) {
			id = xrun(() -> xlib.XCreateWindow(dpy, screen.root(), 0, 0, 1, 1, 0, vis.depth(), XLib.InputOutput, vis.visual(), values, attrs));
			register(this);
		    }
		    if(im != null) {
			ic = xrun(() -> xlib.XCreateIC(im, Utils.<String, Object>map()
						       .put(XLib.XNInputStyle, imstyle)
						       .put(XLib.XNClientWindow, id).map()));
			if(ic != null) {
			    evmask |= xrun(() -> ((Number)xlib.XGetICValues(ic, Arrays.asList(XLib.XNFilterEvents)).get(XLib.XNFilterEvents))).longValue();
			    xrun(() -> xlib.XSetICFocus(ic));
			} else {
			    Warning.warn(String.format("failed to create X11 input context"));
			}
		    } else {
			ic = null;
		    }

		    {
			XIEventMask mask = new XIEventMask(XInput.XIAllMasterDevices);
			mask.set(XInput.XI_ButtonPress, XInput.XI_ButtonRelease, XInput.XI_Motion, XInput.XI_Enter);
			xrun(() -> xi.XISelectEvents(dpy, id, Arrays.asList(mask)));
		    }

		    xrun(() -> {
			xlib.XChangeProperty(dpy, id, _NET_WM_PID.id, CARDINAL.id, XLib.PropModeReplace, new long[] {libc.getpid()});
			xlib.XChangeProperty(dpy, id, WM_PROTOCOLS.id, ATOM.id, XLib.PropModeReplace, new Atom[] {WM_DELETE_WINDOW.id, _NET_WM_PING.id});
		    });
		    long barda = evmask;
		    xrun(() -> xlib.XSelectInput(dpy, id, barda));

		    done = true;
		} finally {
		    if(!done)
			dispose();
		}
	    }

	    public XID windowid() {return(id);}

	    public void add(EventListener l) {
		callbacks.add(l);
	    }

	    private void callback(Event ev) {
		for(EventListener l : callbacks)
		    l.event(ev);
	    }

	    public GLXToolkit toolkit() {
		return(GLXToolkit.this);
	    }

	    public GLXWindow show(boolean show) {
		if(show && !showing) {
		    Promise<XEvent> waiter = eventwait(ev -> ev.type() == XLib.MapNotify);
		    xrun(() -> xlib.XMapWindow(dpy, id));
		    showing = true;
		    waiter.waitfor();
		} else if(!show && showing) {
		    Promise<XEvent> waiter = eventwait(ev -> ev.type() == XLib.UnmapNotify);
		    xrun(() -> xlib.XUnmapWindow(dpy, id));
		    showing = false;
		    waiter.waitfor();
		}
		return(this);
	    }

	    public GLXWindow size(Coord size) {
		xrun(() -> xlib.XResizeWindow(dpy, id, size.x, size.y));
		return(this);
	    }

	    public GLXWindow title(String title) {
		xrun(() -> {
		    xlib.XChangeProperty(dpy, id,      WM_NAME.id, UTF8_STRING.id, XLib.PropModeReplace, title.getBytes(Utils.utf8));
		    xlib.XChangeProperty(dpy, id, _NET_WM_NAME.id, UTF8_STRING.id, XLib.PropModeReplace, title.getBytes(Utils.utf8));
		});
		return(this);
	    }

	    public GLXWindow cursor(Cursor cursor) {
		if(cursor == null) {
		    xrun(() -> xlib.XDefineCursor(dpy, id, XID.None));
		    return(this);
		}
		XCursor xc;
		if(cursor instanceof Cursor.Std)
		    xc = getlibcursor((Cursor.Std)cursor);
		else
		    xc = (XCursor)cursor;
		xrun(() -> xlib.XDefineCursor(dpy, id, xc.id));
		return(this);
	    }

	    public GLXWindow icon(BufferedImage img) {
		img = PUtils.coercergba(img, false);
		Coord sz = PUtils.imgsz(img);
		long[] pixels = new long[2 + (sz.x * sz.y)];
		pixels[0] = sz.x;
		pixels[1] = sz.y;
		Raster imgd = img.getRaster();
		for(int y = 0, p = 2; y < sz.y; y++) {
		    for(int x = 0; x < sz.x; x++, p++) {
			int a = imgd.getSample(x, y, 3);
			int r = (imgd.getSample(x, y, 0) * a) / 255;
			int g = (imgd.getSample(x, y, 1) * a) / 255;
			int b = (imgd.getSample(x, y, 2) * a) / 255;
			pixels[p] = (b << 0) | (g << 8) | (r << 16) | (a << 24);
		    }
		}
		xrun(() -> xlib.XChangeProperty(dpy, id, _NET_WM_ICON.id, CARDINAL.id, XLib.PropModeReplace, pixels));
		return(this);
	    }

	    public GLXWindow sizing(Sizing info) {
		XLib.XSizeHints hints = new XLib.XSizeHints();
		Coord nsz = null;
		if(info.fixsize == null) {
		    if(info.normsize != null) {
			nsz = info.normsize;
			hints.width  = info.normsize.x;
			hints.height = info.normsize.y;
			hints.base_width  = info.normsize.x;
			hints.base_height = info.normsize.y;
			hints.flags |= XLib.PSize | XLib.PBaseSize;
		    }
		    if(info.minsize != null) {
			hints.min_width  = info.minsize.x;
			hints.min_height = info.minsize.y;
			hints.flags |= XLib.PMinSize;
		    }
		    if(info.maxsize != null) {
			hints.max_width  = info.maxsize.x;
			hints.max_height = info.maxsize.y;
			hints.flags |= XLib.PMaxSize;
		    }
		} else {
		    nsz = info.fixsize;
		    hints.width  = info.fixsize.x;
		    hints.height = info.fixsize.y;
		    hints.base_width  = info.fixsize.x;
		    hints.base_height = info.fixsize.y;
		    hints.min_width  = info.fixsize.x;
		    hints.min_height = info.fixsize.y;
		    hints.max_width  = info.fixsize.x;
		    hints.max_height = info.fixsize.y;
		    hints.flags |= XLib.PSize | XLib.PBaseSize | XLib.PMinSize | XLib.PMaxSize;
		}
		if(hints.flags != 0) {
		    Coord nsz2 = nsz;
		    xrun(() -> {
			if(nsz2 != null)
			    xlib.XResizeWindow(dpy, id, nsz2.x, nsz2.y);
			xlib.XSetWMNormalHints(dpy, id, hints);
		    });
		}
		return(this);
	    }

	    private Set<Atom> curstate = Collections.emptySet();
	    private void wmstate(Atom[] st) {
		xrun(() -> {
		    if(showing) {
			Collection<Atom> p = curstate, n = Arrays.asList(st);
			boolean mhcons = false, mvcons = false;
			for(Atom s : p) {
			    if(!n.contains(s)) {
				if((s.equals(_NET_WM_STATE_MAXIMIZED_VERT.id) && mvcons) || (s.equals(_NET_WM_STATE_MAXIMIZED_HORZ.id) && mhcons))
				    continue;
				XEvent msg = xlib.XEvent().type(XLib.ClientMessage).window(id);
				msg.xclient().message_type(_NET_WM_STATE.id).format(32).l(0, 0).a(1, _NET_WM_STATE_FULLSCREEN.id).a(2, Atom.nil).l(3, 1);
				if(s.equals(_NET_WM_STATE_MAXIMIZED_VERT.id) && p.contains(_NET_WM_STATE_MAXIMIZED_HORZ.id)) {
				    msg.xclient().a(2, _NET_WM_STATE_MAXIMIZED_HORZ.id);
				    mhcons = true;
				} else if(s.equals(_NET_WM_STATE_MAXIMIZED_HORZ.id) && p.contains(_NET_WM_STATE_MAXIMIZED_VERT.id)) {
				    msg.xclient().a(2, _NET_WM_STATE_MAXIMIZED_VERT.id);
				    mvcons = true;
				}
				xlib.XSendEvent(dpy, screen.root(), false, XLib.SubstructureNotifyMask | XLib.SubstructureRedirectMask, msg);
			    }
			}
			for(Atom s : n) {
			    if(!p.contains(s)) {
				XEvent msg = xlib.XEvent().type(XLib.ClientMessage).window(id);
				msg.xclient().message_type(_NET_WM_STATE.id).format(32).l(0, 1).a(1, _NET_WM_STATE_FULLSCREEN.id).a(2, Atom.nil).l(3, 1);
				xlib.XSendEvent(dpy, screen.root(), false, XLib.SubstructureNotifyMask | XLib.SubstructureRedirectMask, msg);
			    }
			}
		    } else {
			xlib.XChangeProperty(dpy, id, _NET_WM_STATE.id, ATOM.id, XLib.PropModeReplace, st);
		    }
		    curstate = (st.length == 0) ? Collections.emptySet() : new HashSet<>(Arrays.asList(st));
		});
	    }

	    public GLXWindow state(State st) {
		xrun(() -> {
		    switch(st) {
		    case MINIMIZED:  xlib.XIconifyWindow(dpy, id, nscreen); break;
		    case NORMAL:     wmstate(new Atom[] {}); break;
		    case MAXIMIZED:  wmstate(new Atom[] {_NET_WM_STATE_MAXIMIZED_VERT.id, _NET_WM_STATE_MAXIMIZED_HORZ.id}); break;
		    case EXCLUSIVE:  wmstate(new Atom[] {_NET_WM_STATE_FULLSCREEN.id}); break;
		    }
		});
		return(this);
	    }

	    public Coord size() {
		return(size);
	    }

	    public State state() {
		if(curstate.contains(_NET_WM_STATE_HIDDEN.id))
		    return(State.MINIMIZED);
		if(curstate.contains(_NET_WM_STATE_FULLSCREEN.id))
		    return(State.EXCLUSIVE);
		else if(curstate.contains(_NET_WM_STATE_MAXIMIZED_VERT.id) && curstate.contains(_NET_WM_STATE_MAXIMIZED_HORZ.id))
		    return(State.MAXIMIZED);
		return(State.NORMAL);
	    }

	    public boolean focused() {
		return(focused);
	    }

	    public Visibility visible() {
		switch(visibility) {
		case XLib.VisibilityUnobscured:        return(Visibility.FULL);
		case XLib.VisibilityPartiallyObscured: return(Visibility.PARTIAL);
		case XLib.VisibilityFullyObscured:     return(Visibility.NONE);
		default:                               return(Visibility.UNKNOWN);
		}
	    }

	    public void dispose() {
		try(Aliveness _ = new Aliveness()) {
		    unregister(this);
		    xrun(() -> {
			if(ic != null)
			    xlib.XDestroyIC(ic);
			xlib.XDestroyWindow(dpy, id);
		    });
		}
	    }

	    public Environment env() {
		if(renv == null) {
		    synchronized(this) {
			if(renv == null) {
			    renv = glrun(id, () -> new GLXEnvironment());
			}
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
		if(ival != cursi)
		    glx.glXSwapIntervalEXT(dpy, id, cursi = ival);
		glx.glXSwapBuffers(dpy, id);
		GLException.checkfor(gl, null);
	    }

	    public void swapbuffers(Render buf, Object mode) {
		GLRender gbuf = (GLRender)buf;
		if(((GLXEnvironment)gbuf.env).wnd() != this)
		    throw(new IllegalArgumentException());
		if(!(mode instanceof Boolean))
		    throw(new IllegalArgumentException());
		gbuf.submit(gl-> this.glswap(gl, ((Boolean)mode) ? 1 : 0));
	    }

	    private Promise<XEvent> eventwait(Predicate<XEvent> test) {
		Promise<XEvent> ret = new Promise<>();
		Predicate<XEvent> cb = ev -> {
		    if(test.test(ev)) {
			ret.resolve(ev);
			return(true);
		    }
		    return(false);
		};
		synchronized(listeners) {
		    listeners.add(cb);
		}
		return(ret);
	    }

	    private void configurenotify(XConfigureEvent ev) {
		size = Coord.of(ev.width(), ev.height());
	    }

	    private void propertynotify(XPropertyEvent ev) {
		if(ev.atom().equals(_NET_WM_STATE.id)) {
		    XProperty val = xlib.XGetWindowProperty(dpy, id, ev.atom(), false, Atom.nil);
		    Set<Atom> nst = new HashSet<>();
		    for(int i = 0; i < val.len; i++)
			nst.add(val.a(i));
		    if(!nst.equals(curstate)) {
			curstate = nst.isEmpty() ? Collections.emptySet() : nst;
		    }
		}
	    }

	    private final XComposeStatus compose = xlib.XComposeStatus();
	    private boolean keylookup(GLXKeyEvent out, XKeyEvent data, boolean doim) {
		byte[] cbuf = new byte[128];
		XID[] sbuf = new XID[1];
		int l;
		if(!doim || (ic == null)) {
		    l = data.lookup(cbuf, sbuf, compose);
		    out.rawsym = sbuf[0];
		    if(l > 0)
			out.str = new String(Utils.splice(cbuf, 0, l), ABI.C_CHARSET);
		    out.sym = getkeysym(sbuf[0]);
		    return(true);
		} else {
		    int[] st = {0};
		    l = data.utf8lookup(ic, cbuf, sbuf, st);
		    while(st[0] == XLib.XBufferOverflow) {
			cbuf = new byte[l + 16];
			l = data.utf8lookup(ic, cbuf, sbuf, st);
		    }
		    boolean rv = false;
		    if(l > 0) {
			if(!((st[0] == XLib.XLookupChars) || (st[0] == XLib.XLookupBoth)))
			    throw(new RuntimeException("unexpected X11 key lookup status: " + st[0]));
			out.str = new String(Utils.splice(cbuf, 0, l), Utils.utf8);
			rv = true;
		    }
		    if(!sbuf[0].equals(XID.None)) {
			if(!((st[0] == XLib.XLookupKeySym) || (st[0] == XLib.XLookupBoth)))
			    throw(new RuntimeException("unexpected X11 key lookup status: " + st[0]));
			out.rawsym = sbuf[0];
			out.sym = getkeysym(sbuf[0]);
			rv = true;
		    }
		    return(rv);
		}
	    }

	    private void keypress(XKeyEvent ev) {
		GLXKeyPressEvent tke = new GLXKeyPressEvent(this, ev);
		if(keylookup(tke, ev, true))
		    callback(tke);
	    }
	    private void keyrelease(XKeyEvent ev) {
		GLXKeyReleaseEvent tke = new GLXKeyReleaseEvent(this, ev);
		if(keylookup(tke, ev, false))
		    callback(tke);
	    }

	    private void clientmsg(XClientMessageEvent ev) {
		if(WM_PROTOCOLS.is(ev.message_type()) && (ev.format() == 32)) {
		    Atom proto = ev.a()[0];
		    if(WM_DELETE_WINDOW.is(proto)) {
			callback(new CloseRequest() {});
		    } else if(_NET_WM_PING.is(proto)) {
			XEvent msg = xlib.XEvent().type(XLib.ClientMessage).window(screen.root());
			msg.xclient().message_type(WM_PROTOCOLS.id).format(32).a(0, _NET_WM_PING.id).l(1, ev.l()[1]).x(2, id);
			xlib.XSendEvent(dpy, screen.root(), false, XLib.SubstructureNotifyMask | XLib.SubstructureRedirectMask, msg);
		    } else {
			if(DEBUG)
			    Debug.dump("unknown window-manager message received: " + xlib.XGetAtomName(dpy, ev.a()[0]));
		    }
		} else if(XdndEnter.is(ev.message_type()) && (ev.format() == 32)) {
		    XID src = ev.x()[0];
		    Set<Atom> types = new HashSet<>();
		    long[] l = ev.l();
		    if((l[1] & 1) != 0) {
			XProperty val = xlib.XGetWindowProperty(dpy, src, XdndTypeList.id, false, Atom.nil);
			types.addAll(Arrays.asList(val.a()));
		    } else {
			Atom[] a = ev.a();
			for(int i = 2; i < 5; i++) {
			    if(!Utils.eq(a[i], Atom.nil))
				types.add(a[i]);
			}
		    }
		    dropping = new Xdnd((int)(l[1] & 0xff000000) >>> 24, src, types);
		} else if(XdndLeave.is(ev.message_type())) {
		    dropping = null;
		} else if(XdndPosition.is(ev.message_type())) {
		    Xdnd d = this.dropping;
		    XID src = ev.x()[0];
		    if((d == null) || !Utils.eq(src, d.src))
			return;
		    long[] l = ev.l();
		    Coord rpos = Coord.of((short)(l[2] >>> 16), (short)l[2]);
		    d.hover(rpos, ev.a()[4], l[3]);
		} else if(XdndDrop.is(ev.message_type())) {
		    Xdnd d = this.dropping;
		    XID src = ev.x()[0];
		    if((d == null) || !Utils.eq(src, d.src))
			return;
		    d.dropped(ev.l()[2]);
		} else {
		    if(DEBUG)
			Debug.dump("unknown client message received: " + xlib.XGetAtomName(dpy, ev.message_type()));
		}
	    }

	    private void selectionclear(XSelectionClearEvent ev) {
		GLXClipboard c = clipboard(ev.selection(), false);
		if(c != null) c.clear(ev);
	    }
	    private void selectionrequest(XSelectionRequestEvent ev) {
		GLXClipboard c = clipboard(ev.selection(), false);
		if(c != null) c.request(ev);
	    }

	    public void event(XEvent ev) {
		synchronized(listeners) {
		    for(Iterator<Predicate<XEvent>> i = listeners.iterator(); i.hasNext();) {
			Predicate<XEvent> cb = i.next();
			if(cb.test(ev))
			    i.remove();
		    }
		}
		switch(ev.type()) {
		case XLib.MapNotify:
		    mapped = true;
		    break;
		case XLib.UnmapNotify:
		    mapped = false;
		    break;
		case XLib.ReparentNotify:
		    break;
		case XLib.ConfigureNotify:
		    configurenotify(ev.xconfigure());
		    break;
		case XLib.FocusIn:
		    focused = true;
		    break;
		case XLib.FocusOut:
		    focused = false;
		    break;
		case XLib.PropertyNotify:
		    if(ev.window().equals(id))
			propertynotify(ev.xproperty());
		    break;
		case XLib.VisibilityNotify:
		    visibility = ev.xvisibility().state();
		    break;
		case XLib.ClientMessage:
		    clientmsg(ev.xclient());
		    break;
		case XLib.KeyPress:
		    keypress(ev.xkey());
		    break;
		case XLib.KeyRelease:
		    keyrelease(ev.xkey());
		    break;
		case XLib.SelectionClear:
		    selectionclear(ev.xselectionclear());
		    break;
		case XLib.SelectionRequest:
		    selectionrequest(ev.xselectionrequest());
		    break;
		default:
		    Warning.warn(String.format("unexpected event received for window %s: %d", id, ev.type()));
		}
	    }

	    public void event(XIDeviceEvent ev) {
		XIPointerInfo ptr;
		synchronized(pointers) {
		    ptr = pointers.get(ev.deviceid());
		}
		if(ptr != null) {
		    switch(ev.evtype()) {
		    case XInput.XI_ButtonPress:
			if((ev.flags() & XInput.XIPointerEmulated) == 0) {
			    int btn = ev.detail();
			    if((btn == 4) || (btn == 5)) {
				callback(new GLXMouseWheelEvent(this, ptr, ev, MouseWheelEvent.Axis.VERT, (btn == 4) ? -1 : 1));
			    } else if((btn == 6) || (btn == 7)) {
				callback(new GLXMouseWheelEvent(this, ptr, ev, MouseWheelEvent.Axis.HORIZ, (btn == 6) ? -1 : 1));
			    } else {
				callback(new GLXMouseDownEvent(this, ptr, ev));
			    }
			}
			break;
		    case XInput.XI_ButtonRelease:
			if((ev.flags() & XInput.XIPointerEmulated) == 0)
			    callback(new GLXMouseUpEvent(this, ptr, ev));
			break;
		    case XInput.XI_Motion: {
			Map<Integer, Double> vals = ev.valuators();
			for(Integer v : ev.valuators().keySet()) {
			    XIPointerInfo.Scroll scr = ptr.scroll.get(v);
			    if(scr != null) {
				double nv = vals.get(v), nvn = nv / scr.inc, lvn = scr.lastval / scr.inc;
				long nvi = (long)Math.floor(nvn), lvi = (long)Math.floor(lvn);
				double delta = nvn - lvn;
				int idelta = (int)(nvi - lvi);
				scr.lastval = nv;
				MouseWheelEvent.Axis axis = null;
				if(scr.type == XInput.XIScrollTypeVertical)
				    axis = MouseWheelEvent.Axis.VERT;
				else if(scr.type == XInput.XIScrollTypeHorizontal)
				    axis = MouseWheelEvent.Axis.HORIZ;
				if(axis != null)
				    callback(new GLXMouseWheelEvent(this, ptr, ev, axis, idelta, delta));
			    }
			}
			if((ev.flags() & XInput.XIPointerEmulated) == 0)
			    callback(new GLXMouseMoveEvent(this, ptr, ev));
			break;
		    }
		    default:
			Warning.warn(String.format("unexpected XInput event received for window %s: %d",  ev.evtype()));
			break;
		    }
		}
	    }

	    public void event(XIFocusEvent ev) {
		switch(ev.evtype()) {
		case XInput.XI_Enter:
		    xrun(() -> {
			Map<Integer, XIDeviceInfo> devs = devicemap();
			XIDeviceInfo dev = devs.get(ev.deviceid());
			if(dev == null)
			    return;
			synchronized(pointers) {
			    pointers.put(dev.deviceid(), new XIPointerInfo(dev, devs));
			}
		    });
		    break;
		default:
		    Warning.warn(String.format("unexpected XInput event received for window %s: %d",  ev.evtype()));
		    break;
		}
	    }

	    public class GLXClipboard implements Clipboard {
		public final Atom selection;
		private final Map<XID, ConvRequest> scoreboard = new HashMap<>();
		private boolean owned = false;
		private XContents contents = null;
		private Runnable expire = null;

		public GLXClipboard(Atom selection) {
		    this.selection = selection;
		}

		private <T> Promise<T> etpromise(Supplier<T> task) {
		    return(Promise.deferred(task, GLXToolkit.this::xrun));
		}

		void clear(XSelectionClearEvent ev) {
		    Runnable exp = null;
		    synchronized(this) {
			if(owned) {
			    exp = expire;
			    owned = false;
			    contents = null;
			    expire = null;
			}
		    }
		    if(exp != null)
			exp.run();
		}

		public class Conversion {
		    public final Atom type;
		    public final byte[] data;

		    public Conversion(Atom type, byte[] data) {
			this.type = type;
			this.data = data;
		    }

		    public Conversion(Atom type, ByteBuffer data) {
			this.type = type;
			this.data = new byte[data.remaining()];
			data.get(this.data);
		    }
		}

		public class XContents {
		    public final Contents cont;
		    public final Map<Atom, Supplier<Promise<Conversion>>> items = new HashMap<>();

		    public XContents(Contents cont) {
			this.cont = cont;
			for(Item<?> item : cont.items) {
			    if(item.fmt == Format.TEXT) {
				items.put(UTF8_STRING.id, () -> cvt_text(item));
				items.put(TEXT_PLAIN.id, () -> cvt_text(item));
				items.put(TEXT_PLAIN_UTF8.id, () -> cvt_text(item));
			    } else if(item.fmt == Format.IMAGE) {
				items.put(IMAGE_PNG.id, () -> cvt_image(item));
			    } else if(item.fmt == Format.PATHS) {
				items.put(TEXT_URILIST.id, () -> cvt_paths(item));
			    }
			}
		    }

		    private Promise<Conversion> cvt_text(Item<?> ritem) {
			Promise<CharSequence> data = ritem.check(Format.TEXT).get();
			return(data.map(text -> {
			    ByteBuffer bdat = Utils.utf8.encode(CharBuffer.wrap(text));
			    return(new Conversion(UTF8_STRING.id, bdat));
			}));
		    }

		    private Promise<Conversion> cvt_image(Item<?> ritem) {
			Promise<BufferedImage> data = ritem.check(Format.IMAGE).get();
			return(data.then(img -> Promise.deferred(() -> {
			    ByteArrayOutputStream buf = new ByteArrayOutputStream();
			    try {
				javax.imageio.ImageIO.write(img, "PNG", buf);
			    } catch(IOException e) {
				throw(new RuntimeException(e));
			    }
			    return(new Conversion(IMAGE_PNG.id, buf.toByteArray()));
			}, t -> Defer.later(t, null))));
		    }

		    private Promise<Conversion> cvt_paths(Item<?> ritem) {
			Promise<Collection<Path>> data = ritem.check(Format.PATHS).get();
			return(data.map(paths -> {
			    ByteArrayOutputStream buf = new ByteArrayOutputStream();
			    try(Writer f = new OutputStreamWriter(buf, Utils.utf8)) {
				for(Path p : paths) {
				    f.write(p.toUri().toString());
				}
			    } catch(IOException e) {
				throw(new RuntimeException(e));
			    }
			    return(new Conversion(UTF8_STRING.id, buf.toByteArray()));
			}));
		    }
		}

		public class ConvRequest {
		    public static final int MAXSIZE = 16000;
		    public final XID requestor;
		    public final Atom target, prop;
		    public final long time;
		    public final XContents cont;
		    ConvRequest next = null;
		    private boolean done = false;
		    private EventWindow listener;
		    private Timeout.Future<?> timeout;
		    private Conversion incr;
		    private int incroff = 0;

		    private ConvRequest(XID requestor, Atom target, Atom prop, long time, XContents cont) {
			this.requestor = requestor;
			this.target = target;
			this.prop = prop;
			this.time = time;
			this.cont = cont;
		    }

		    private void done() {
			synchronized(this) {
			    if(done)
				return;
			    done = true;
			}
			if(listener != null) {
			    xlib.XSelectInput(dpy, requestor, 0);
			    unregister(listener);
			}
			synchronized(GLXClipboard.this) {
			    if(scoreboard.get(requestor) == this)
				scoreboard.remove(requestor);
			    else
				xrun((Runnable)next::respond);
			}
		    }

		    private void notify(Atom property) {
			XEvent msg = xlib.XEvent().type(XLib.SelectionNotify);
			msg.xselection().requestor(requestor).selection(selection).target(target).property(property);
			xlib.XSendEvent(dpy, requestor, false, 0, msg);
		    }

		    private void handle(XPropertyEvent ev) {
			if(Utils.eq(ev.atom(), prop) && (ev.state() == XLib.PropertyDelete)) {
			    int len = Math.min(incr.data.length - incroff, MAXSIZE);
			    if(len > 0) {
				xlib.XChangeProperty(dpy, requestor, prop, incr.type, XLib.PropModeReplace, Utils.splice(incr.data, incroff, len));
				incroff += len;
			    } else {
				xlib.XChangeProperty(dpy, requestor, prop, incr.type, XLib.PropModeReplace, new byte[0]);
				done();
			    }
			}
		    }

		    private void timeout() {
			Warning.warn("selection transfer timed out");
			done();
		    }

		    private void respond(Conversion conv) {
			if(conv.data.length > MAXSIZE) {
			    timeout = Timeout.later(Utils.rtime() + 5, this::timeout, null);
			    incr = conv;
			    xlib.XChangeProperty(dpy, requestor, prop, INCR.id, XLib.PropModeReplace, new long[] {conv.data.length});
			    listener = new EventWindow() {
				public XID windowid() {return(requestor);}
				public void event(XEvent ev) {
				    if(ev.type() == XLib.PropertyNotify) {
					handle(ev.xproperty());
				    }
				}
			    };
			    xlib.XSelectInput(dpy, requestor, XLib.PropertyChangeMask);
			    register(listener);
			    notify(prop);
			} else {
			    xlib.XChangeProperty(dpy, requestor, prop, conv.type, XLib.PropModeReplace, conv.data);
			    notify(prop);
			    done();
			}
		    }

		    void respond() {
			if(cont == null) {
			    notify(Atom.nil);
			    done();
			    return;
			}
			if(TARGETS.is(target)) {
			    List<Atom> resp = new ArrayList<>();
			    resp.add(TARGETS.id);
			    resp.addAll(contents.items.keySet());
			    xlib.XChangeProperty(dpy, requestor, prop, ATOM.id, XLib.PropModeReplace, resp.toArray(new Atom[0]));
			    notify(prop);
			    done();
			} else if(cont.items.containsKey(target)) {
			    cont.items.get(target).get().then(conv -> etpromise(() -> {respond(conv); return(null);})).warn("clipboard error");
			} else {
			    notify(Atom.nil);
			    done();
			}
		    }
		}

		void request(XSelectionRequestEvent ev) {
		    XID rqor = ev.requestor();
		    ConvRequest req;
		    boolean respond = true;
		    synchronized(this) {
			req = new ConvRequest(rqor, ev.target(), ev.property(), ev.time(), contents);
			ConvRequest last = scoreboard.get(rqor);
			if(last != null) {
			    last.next = req;
			    respond = false;
			}
			scoreboard.put(rqor, req);
		    }
		    if(respond)
			req.respond();
		}

		public void put(Contents cnt, Runnable exp) {
		    xrun(() -> {
			synchronized(this) {
			    if(!owned) {
				xlib.XSetSelectionOwner(dpy, selection, id, xlib.CurrentTime);
				owned = true;
			    } else {
				if(expire != null)
				    expire.run();
			    }
			    this.contents = new XContents(cnt);
			    this.expire = exp;
			}
		    });
		}

		private Promise<SelectionRequest> convert(XID owner, Atom target, long time) {
		    return(etpromise(() -> {
			return(new SelectionRequest(selection, owner, target, time));
		    }).then(req -> {
			return(req.promise);
		    }));
		}

		private String cvt_text(SelectionRequest req) {
		    if(req.resp.format != 8)
			throw(new RuntimeException("unexpected selection format for text: " + req.resp.format));
		    if(UTF8_STRING.is(req.resp.type))
			return(new String(req.resp.b(), Utils.utf8));
		    if(STRING.is(req.resp.type))
			return(new String(req.resp.b(), java.nio.charset.Charset.forName("ISO-8859-1")));
		    throw(new RuntimeException("unexpected selection type for text: " + xlib.XGetAtomName(dpy, req.resp.type)));
		}

		private BufferedImage cvt_image(SelectionRequest req) {
		    if(req.resp.format != 8)
			throw(new RuntimeException("unexpected selection format for image: " + req.resp.format));
		    if(Arrays.asList(IMAGE_PNG.id, IMAGE_JPEG.id).contains(req.resp.type))
			try {
			    BufferedImage ret = javax.imageio.ImageIO.read(new ByteArrayInputStream(req.resp.b()));
			    if(ret == null)
				throw(new RuntimeException("could not decode clipboard image"));
			    return(ret);
			} catch(IOException e) {
			    throw(new RuntimeException(e.getMessage(), e));
			}
		    throw(new RuntimeException("unexpected selection type for image: " + xlib.XGetAtomName(dpy, req.resp.type)));
		}

		private Collection<Path> cvt_paths(SelectionRequest req) {
		    if(req.resp.format != 8)
			throw(new RuntimeException("unexpected selection format for paths: " + req.resp.format));
		    if(TEXT_URILIST.is(req.resp.type)) {
			Collection<Path> ret = new ArrayList<>();
			for(String ln : new String(req.resp.b(), Utils.utf8).split("\n")) {
			    ln = ln.trim();
			    if((ln.length() == 0) || (ln.charAt(0) == '#'))
				continue;
			    try {
				ret.add(Paths.get(Utils.uri(ln)));
			    } catch(IllegalArgumentException e) {
				e.printStackTrace();
			    }
			}
			return(ret);
		    }
		    throw(new RuntimeException("unexpected selection type for paths: " + xlib.XGetAtomName(dpy, req.resp.type)));
		}

		private <T> Item<T> mkitem(XID owner, long time, Format<T> fmt, Atom target, Function<SelectionRequest, ? extends T> cvt) {
		    return(new Item<T>(fmt, () -> convert(owner, target, time).map(cvt)));
		}

		Contents mkcontents(Set<Atom> tgts, XID owner, long time) {
		    List<Item<?>> items = new ArrayList<>();
		    if(tgts.contains(UTF8_STRING.id))
			items.add(mkitem(owner, time, Format.TEXT, UTF8_STRING.id, this::cvt_text));
		    else if(tgts.contains(STRING.id))
			items.add(mkitem(owner, time, Format.TEXT, STRING.id, this::cvt_text));
		    if(tgts.contains(IMAGE_JPEG.id))
			items.add(mkitem(owner, time, Format.IMAGE, IMAGE_JPEG.id, this::cvt_image));
		    else if(tgts.contains(IMAGE_PNG.id))
			items.add(mkitem(owner, time, Format.IMAGE, IMAGE_PNG.id, this::cvt_image));
		    if(tgts.contains(TEXT_URILIST.id))
			items.add(mkitem(owner, time, Format.PATHS, TEXT_URILIST.id, this::cvt_paths));
		    return(new Contents(items));
		}

		public Promise<Contents> get() {
		    synchronized(this) {
			if(owned)
			    return(Promise.of(() -> contents.cont));
		    }
		    Promise<Contents> ret = convert(null, TARGETS.id, XLib.CurrentTime).map(treq -> {
			XProperty data = treq.resp;
			if((data == null) || (data.format != 32) || !ATOM.is(data.type))
			    throw(new RuntimeException("selection target fetch failed"));
			Set<Atom> tgts = new HashSet<>(Arrays.asList(data.a()));
			return(mkcontents(tgts, treq.owner, treq.time));
		    });
		    return(ret);
		}
	    }

	    private final Map<Atom, GLXClipboard> clipboards = new HashMap<>();
	    public GLXClipboard clipboard(Atom name, boolean create) {
		synchronized(clipboards) {
		    if(create)
			return(clipboards.computeIfAbsent(name, GLXClipboard::new));
		    return(clipboards.get(name));
		}
	    }

	    public Clipboard clipboard(Object id) {
		Atom name;
		if(id == Clipboard.Std.PRIMARY)
		    name = PRIMARY.id;
		else if(id == Clipboard.Std.CLIPBOARD)
		    name = CLIPBOARD.id;
		else if(id instanceof Atom)
		    name = (Atom)id;
		else if(id instanceof String)
		    name = xrun(() -> xlib.XInternAtom(dpy, (String)id, false));
		else
		    return(Clipboard.nil);
		return(clipboard(name, true));
	    }

	    public class Xdnd {
		public final BMap<DropHandler.Action, Atom> actmap =
		    new HashBMap<>(Utils.<DropHandler.Action, Atom>map()
				   .put(DropHandler.Action.COPY, XdndActionCopy.id)
				   .put(DropHandler.Action.MOVE, XdndActionMove.id)
				   .put(DropHandler.Action.LINK, XdndActionLink.id)
				   .map());
		public final GLXClipboard sel = clipboard(XdndSelection.id, true);
		public final int ver;
		public final XID src;
		public final Set<Atom> types;
		public final Coord rpos;
		private Clipboard.Contents cc = null;
		private Coord lastpos;
		private Atom lastact;
		private long lasttime;

		public Xdnd(int ver, XID src, Set<Atom> types) {
		    this.ver = ver;
		    this.src = src;
		    this.types = types;
		    this.rpos = xlib.XTranslateCoordinates(dpy, id, screen.root(), Coord.z);
		}

		void hover(Coord rpos, Atom act, long time) {
		    if(cc == null)
			cc = sel.mkcontents(new HashSet<>(types), src, time);
		    Coord wpos = rpos.sub(this.rpos);
		    lastpos = wpos;
		    lastact = act;
		    lasttime = time;
		    class Event implements DropHandler.DropHoverEvent {
			public Coord wndc() {return(wpos);}
			public Set<DropHandler.Action> actions() {
			    Set<DropHandler.Action> ret = EnumSet.of(DropHandler.Action.COPY);
			    DropHandler.Action dact = actmap.reverse().get(act);
			    if(dact != null)
				ret.add(dact);
			    return(ret);
			}
			public Clipboard.Contents contents() {return(cc);}
		    }
		    Atom ract = (drop == null) ? null : actmap.get(drop.drophover(new Event()));
		    XEvent msg = xlib.XEvent().type(XLib.ClientMessage).window(src);
		    msg.xclient().message_type(XdndStatus.id).format(32).x(0, id)
			.l(1, (ract == null) ? 0 : 1).l(2, 0).l(3, 0)
			.a(4, (ract == null) ? Atom.nil : ract);
		    xlib.XSendEvent(dpy, src, false, 0, msg);
		}

		private void finish(DropHandler.Action act) {
		    Atom ract = actmap.getOrDefault(act, XdndActionCopy.id);
		    XEvent msg = xlib.XEvent().type(XLib.ClientMessage).window(src);
		    msg.xclient().message_type(XdndFinished.id).format(32).x(0, id)
			.l(1, (ract == null) ? 0 : 1)
			.a(2, (ract == null) ? Atom.nil : ract);
		    xlib.XSendEvent(dpy, src, false, 0, msg);
		}

		void dropped(long time) {
		    if(cc == null)
			return;
		    class Event implements DropHandler.DroppedEvent {
			DropHandler.Action act = null;
			Clipboard.Contents wrapped = wrapcontents(cc);
			boolean fetched = false;
			boolean accepted = false;

			public Coord wndc() {return(lastpos);}
			public Set<DropHandler.Action> actions() {
			    Set<DropHandler.Action> ret = EnumSet.of(DropHandler.Action.COPY);
			    DropHandler.Action dact = actmap.reverse().get(lastact);
			    if(dact != null)
				ret.add(dact);
			    return(ret);
			}
			public Clipboard.Contents contents() {return(wrapped);}
			public void accept(DropHandler.Action act) {
			    this.act = act;
			}

			private <T> Clipboard.Item<T> wrapitem(Clipboard.Item<T> item) {
			    return(new Clipboard.Item<T>(item.fmt, () -> {
				fetched = true;
				return(item.get().map(data -> data, null, () -> {
				    finish(act);
				}));
			    }));
			}

			private Clipboard.Contents wrapcontents(Clipboard.Contents rc) {
			    Collection<Clipboard.Item<?>> items = new ArrayList<>(rc.items.size());
			    for(Clipboard.Item<?> ritem : rc)
				items.add(wrapitem(ritem));
			    return(new Clipboard.Contents(items));
			}
		    }
		    Event de = new Event();
		    Atom ract = null;
		    if(drop != null)
			de.accepted = drop.dropped(de);
		    if(!de.fetched)
			finish(de.act);
		}
	    }

	    public GLXWindow drophandler(DropHandler drop) {
		xrun(() -> {
		    if((this.drop == null) && (drop != null))
			xlib.XChangeProperty(dpy, id, XdndAware.id, ATOM.id, XLib.PropModeReplace, new long[] {5});
		    else if((this.drop != null) && (drop == null))
			xlib.XDeleteProperty(dpy, id, XdndAware.id);
		    this.drop = drop;
		});
		return(this);
	    }
	}

	public Windeye window() {
	    return(new GLXWindow());
	}

	public class SelectionRequest implements EventWindow {
	    public final Atom selection, target;
	    public final Promise<SelectionRequest> promise = new Promise<>();
	    private final XID owner, twnd;
	    private long time;
	    private Timeout.Future<?> timeout;
	    public XProperty resp = null;
	    private byte[] incrbuf = null;
	    private int incroff = 0;
	    private boolean done = false;

	    private SelectionRequest(Atom selection, XID owner, Atom target, long time) {
		this.selection = selection;
		this.owner = (owner == null) ? xlib.XGetSelectionOwner(dpy, selection) : owner;
		this.target = target;
		this.time = time;
		synchronized(etmon) {
		    if(windows.containsKey(owner))
			throw(new RuntimeException("selection requested for own window"));
		}
		if(this.owner.equals(XID.None)) {
		    this.twnd = null;
		    this.resp = new XProperty(dpy, SELECTED_DATA.id, ATOM.id, 32, 0, new byte[0]);
		    promise.resolve(this);
		} else {
		    XSetWindowAttributes attr = xlib.XSetWindowAttributes();
		    attr.event_mask(XLib.PropertyChangeMask);
		    this.twnd = xlib.XCreateWindow(dpy, screen.root(), 0, 0, 1, 1, 0, 0, XLib.InputOnly, vis.visual(), XLib.CWEventMask, attr);
		    register(this);
		    xlib.XConvertSelection(dpy, selection, target, SELECTED_DATA.id, twnd, time);
		    this.timeout = Timeout.later(Utils.rtime() + 5, () -> xrun(this::timeout), null);
		}
	    }

	    private SelectionRequest(Atom selection, Atom target, long time) {
		this(selection, null, target, time);
	    }

	    public XID windowid() {
		return(twnd);
	    }

	    public void dispose() {
		if(!done) {
		    if(twnd != null)
			xlib.XDestroyWindow(dpy, twnd);
		    done = true;
		}
	    }

	    private void handle(XSelectionEvent ev) {
		synchronized(this) {
		    if(done)
			return;
		    boolean dispose = true;
		    try {
			if(time == XLib.CurrentTime)
			    time = ev.time();
			if(Atom.nil.equals(ev.property())) {
			    promise.reject(new IOException("selection conversion failed"));
			    resp = new XProperty(dpy, SELECTED_DATA.id, Atom.nil, 0, 0, new byte[0]);
			} else {
			    XProperty resp = xlib.XGetWindowProperty(dpy, twnd, SELECTED_DATA.id, true, Atom.nil);
			    // Debug.dump(xlib.XGetAtomName(dpy, selection), xlib.XGetAtomName(dpy, target), resp);
			    if(INCR.is(resp.type)) {
				incrbuf = new byte[(int)resp.l(0)];
				dispose = false;
			    } else {
				this.resp = resp;
				timeout.cancel();
				promise.resolve(this);
			    }
			}
		    } finally {
			if(dispose)
			    dispose();
		    }
		}
	    }

	    private void handle(XPropertyEvent ev) {
		if((incrbuf != null) && SELECTED_DATA.is(ev.atom()) && (ev.state() == XLib.PropertyNewValue)) {
		    synchronized(this) {
			if(done)
			    return;
			XProperty part = xlib.XGetWindowProperty(dpy, twnd, SELECTED_DATA.id, true, Atom.nil);
			if(part.len == 0) {
			    this.resp = new XProperty(dpy, part.name, part.type, part.format, incroff, Arrays.copyOf(incrbuf, incroff));
			    incrbuf = null;
			    timeout.cancel();
			    promise.resolve(this);
			    dispose();
			} else {
			    if(incroff + part.len > incrbuf.length)
				incrbuf = Arrays.copyOf(incrbuf, Math.max(incrbuf.length * 2, incroff + part.len));
			    part.copy(incrbuf, incroff, 0, part.len);
			    incroff += part.len;
			}
		    }
		}
	    }

	    public void event(XEvent ev) {
		switch(ev.type()) {
		case XLib.SelectionNotify:
		    handle(ev.xselection());
		    break;
		case XLib.PropertyNotify:
		    handle(ev.xproperty());
		    break;
		}
	    }

	    private void timeout() {
		synchronized(this) {
		    if(resp == null) {
			promise.reject(new IOException("selection conversion timed out"));
			dispose();
		    }
		}
	    }
	}

	public class XRRMonitor implements Monitor {
	    public final Xrandr.XRROutputInfo out;
	    public final Xrandr.XRRCrtcInfo ctl;

	    public XRRMonitor(Xrandr.XRROutputInfo out, Xrandr.XRRCrtcInfo ctl) {
		this.out = out;
		this.ctl = ctl;
	    }

	    public Coord resolution() {
		return(ctl.size());
	    }

	    public int refresh() {
		return(0);
	    }

	    public Coord size() {
		Coord sz = out.mm_size();
		if((ctl.rotation() & (Xrandr.RR_Rotate_90 | Xrandr.RR_Rotate_270)) != 0)
		    sz = Coord.of(sz.y, sz.x);
		return(sz);
	    }

	    public double userdpi() {
		XrmValue dpi = xlib.XrmGetResource(xrdb, "Xft.dpi", "Xft.Dpi");
		if(dpi != null) {
		    try {
			return(Double.parseDouble(dpi.string()));
		    } catch(IllegalArgumentException e) {
		    }
		}
		return(0);
	    }

	    public double density() {
		Coord r = resolution(), sz = size();
		return((((double)r.x / sz.x) + ((double)r.y / sz.y)) * 25.4 / 2);
	    }

	    public String toString() {
		return(String.format("#<x11-monitor %s %spx %smm u%.1fdpi p%.1fdpi>", out.name(), resolution(), size(), userdpi(), density()));
	    }
	}

	public Collection<Monitor> monitors() {
	    if(xrrinfo == null)
		return(Collections.emptyList());
	    Collection<Monitor> ret = new ArrayList<>();
	    Xrandr.XRRScreenResources sres = xrr.XRRGetScreenResources(dpy, screen.root());
	    for(XID oid : sres.outputs()) {
		Xrandr.XRROutputInfo out = xrr.XRRGetOutputInfo(dpy, sres, oid);
		if(out.connection() == Xrandr.RR_Connected) {
		    Xrandr.XRRCrtcInfo ctl = xrr.XRRGetCrtcInfo(dpy, sres, out.crtc());
		    ret.add(new XRRMonitor(out, ctl));
		}
	    }
	    return(ret);
	}

	public Cursor.Caps cursorcaps() {
	    return(ccaps);
	}

	private final Map<String, XCursor> libcursors = new HashMap<>();
	private final XCursor emptycurs, defcurs = new XCursor(null, XID.None);
	private XCursor getlibcursor(String name) {
	    synchronized(libcursors) {
		XCursor ret = libcursors.get(name);
		if(ret == null) {
		    XID id = xrun(() -> xcr.XcursorLibraryLoadCursor(dpy, name));
		    if(id.equals(XID.None))
			ret = defcurs;
		    else
			ret = new XCursor(dpy, id);
		    libcursors.put(name, ret);
		}
		return(ret);
	    }
	}
	private XCursor getlibcursor(Cursor.Std id) {
	    switch(id) {
	    case DEFAULT:   return(defcurs);
	    case NONE:      return(emptycurs);
	    case POINTER:   return(getlibcursor("default"));
	    case WAIT:      return(getlibcursor("wait"));
	    case HAND:      return(getlibcursor("pointer"));
	    case MOVE:      return(getlibcursor("all-resize"));
	    case CARET:     return(getlibcursor("text"));
	    case CROSSHAIR: return(getlibcursor("crosshair"));
	    case SIZE_N:    return(getlibcursor("n-resize"));
	    case SIZE_NE:   return(getlibcursor("ne-resize"));
	    case SIZE_E:    return(getlibcursor("e-resize"));
	    case SIZE_SE:   return(getlibcursor("se-resize"));
	    case SIZE_S:    return(getlibcursor("s-resize"));
	    case SIZE_SW:   return(getlibcursor("sw-resize"));
	    case SIZE_W:    return(getlibcursor("w-resize"));
	    case SIZE_NW:   return(getlibcursor("nw-resize"));
	    }
	    return(defcurs);
	}

	public XCursor makecursor(BufferedImage img, Coord hotspot) {
	    img = PUtils.coercergba(img, false);
	    Coord sz = PUtils.imgsz(img);
	    int[] pixels = new int[sz.x * sz.y];
	    Raster imgd = img.getRaster();
	    for(int y = 0, p = 0; y < sz.y; y++) {
		for(int x = 0; x < sz.x; x++, p++) {
		    int a = imgd.getSample(x, y, 3);
		    int r = (imgd.getSample(x, y, 0) * a) / 255;
		    int g = (imgd.getSample(x, y, 1) * a) / 255;
		    int b = (imgd.getSample(x, y, 2) * a) / 255;
		    pixels[p] = (b << 0) | (g << 8) | (r << 16) | (a << 24);
		}
	    }
	    return(new XCursor(dpy, xrun(() -> xcr.XcursorImageLoadCursor(dpy, xcr.XcursorImageCreate(sz, hotspot, pixels)))));
	}

	private Set<Key.Mod> mods(int state, int key, boolean include) {
	    if(key != 0) {
		Integer mod = rmodmap.get(key);
		if(mod != null) {
		    if(include)
			state |= 1 << mod;
		    else
			state &= ~(1 << mod);
		}
	    }
	    Set<Key.Mod> ret = EnumSet.noneOf(Key.Mod.class);
	    if((state & XLib.ShiftMask) != 0)
		ret.add(Key.Mod.SHIFT);
	    if((state & XLib.ControlMask) != 0)
		ret.add(Key.Mod.CONTROL);
	    if((mod_alt > 0) && ((state & (1 << mod_alt)) != 0))
		ret.add(Key.Mod.ALT);
	    if((mod_meta > 0) && ((state & (1 << mod_meta)) != 0))
		ret.add(Key.Mod.META);
	    if((mod_super > 0) && ((state & (1 << mod_super)) != 0))
		ret.add(Key.Mod.SUPER);
	    if((mod_altgr > 0) && ((state & (1 << mod_altgr)) != 0))
		ret.add(Key.Mod.ALTGR);
	    return(ret);
	}

	public class DesktopPicker implements FilePicker.Factory {
	    public final DesktopPortal.FileChooser iface;

	    public DesktopPicker() {
		try {
		    iface = DesktopPortal.get().FileChooser();
		} catch(DBusError e) {
		    throw(new Unavailable("Desktop portal not available", e));
		}
	    }

	    public FilePicker make(FilePicker.Mode mode, Windeye parent) {
		String wndid = null;
		if(parent != null)
		    wndid = "x11:" + Long.toUnsignedString(((GLXWindow)parent).id.bits(), 16);
		return(iface.make(mode, wndid));
	    }
	}

	private FilePicker curpicker = null;
	public abstract class ExternalPicker implements FilePicker.Factory {
	    public final Path exec;

	    public ExternalPicker(String name) {
		Path exec = null;
		for(String dnm : System.getenv("PATH").split(":")) {
		    Path file = Paths.get(dnm).resolve(name);
		    if(Files.isRegularFile(file) && Files.isExecutable(file)) {
			exec = file;
			break;
		    }
		}
		if(exec == null)
		    throw(new Unavailable(name + " not found"));
		this.exec = exec;
	    }

	    public abstract class Instance implements FilePicker {
		protected final List<String> args = new ArrayList<>();
		protected Process proc;

		public Instance() {
		    args.add(exec.toFile().toString());
		}

		private void monitor(Process proc, Promise<Path> cb) {
		    try {
			BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String result;
			result = in.readLine();
			if(proc.waitFor() != 0)
			    result = null;
			cb.resolve(((result != null) && (result.length() > 0)) ? Paths.get(result) : null);
		    } catch(Throwable t) {
			cb.reject(t);
		    } finally {
			synchronized(GLXToolkit.this) {
			    if(curpicker == this)
				curpicker = null;
			}
		    }
		}

		public Promise<Path> show() {
		    synchronized(GLXToolkit.this) {
			if(proc != null)
			    throw(new IllegalStateException());
			if(curpicker != null)
			    return(new Promise<Path>().reject(new IllegalStateException("file picker already active")));
			ProcessBuilder spec = new ProcessBuilder(args);
			spec.inheritIO();
			spec.redirectOutput(ProcessBuilder.Redirect.PIPE);
			try {
			    proc = spec.start();
			} catch(IOException e) {
			    return(new Promise<Path>().reject(new RuntimeException(e)));
			}
			Promise<Path> ret = new Promise<>();
			Thread mon = new HackThread(() -> monitor(proc, ret), "Filepicker monitor");
			mon.setDaemon(true);
			mon.start();
			curpicker = this;
			return(ret);
		    }
		}
	    }
	}

	public class ZenityPicker extends ExternalPicker {
	    public ZenityPicker() {
		super("zenity");
	    }

	    public class Instance extends ExternalPicker.Instance {
		public Instance(Mode mode) {
		    args.add("--file-selection");
		    if(mode == Mode.SAVE)
			args.add("--save");
		}

		public void filter(String desc, String... exts) {
		    if(desc.indexOf('|') >= 0)
			throw(new IllegalArgumentException(desc));
		    StringBuilder arg = new StringBuilder();
		    arg.append("--file-filter=");
		    arg.append(desc);
		    arg.append('|');
		    boolean first = true;
		    for(String ext : exts) {
			if(!first)
			    arg.append(' ');
			if(ext.indexOf(' ') >= 0)
			    throw(new IllegalArgumentException(ext));
			arg.append("*."); arg.append(ext);
			first = false;
		    }
		    args.add(arg.toString());
		}
	    }

	    public FilePicker make(FilePicker.Mode mode, Windeye parent) {
		return(new Instance(mode));
	    }
	}

	public class KDialogPicker extends ExternalPicker {
	    public KDialogPicker() {
		super("kdialog");
	    }

	    public class Instance extends ExternalPicker.Instance {
		private StringBuilder filter = new StringBuilder();

		public Instance(Mode mode) {
		    if(mode == Mode.SAVE)
			args.add("--getsavefilename");
		    else
			args.add("--getopenfilename");
		    args.add("--title");
		    args.add("Files");
		    args.add(".");
		}

		public void filter(String desc, String... exts) {
		    if(desc.indexOf('|') >= 0)
			throw(new IllegalArgumentException(desc));
		    if(filter.length() > 0)
			filter.append(" | ");
		    filter.append(desc);
		    filter.append(" (");
		    boolean first = true;
		    for(String ext : exts) {
			if(!first)
			    filter.append(' ');
			if(ext.indexOf(' ') >= 0)
			    throw(new IllegalArgumentException(ext));
			filter.append("*."); filter.append(ext);
			first = false;
		    }
		    filter.append(")");
		}

		public Promise<Path> show(Runnable cb) {
		    if(filter.length() > 0)
			args.add(filter.toString());
		    return(super.show());
		}
	    }

	    public FilePicker make(FilePicker.Mode mode, Windeye parent) {
		return(new Instance(mode));
	    }
	}

	public FilePicker.Factory picker() {
	    try {
		return(new DesktopPicker());
	    } catch(Unavailable e) {}
	    try {
		return(new ZenityPicker());
	    } catch(Unavailable e) {}
	    try {
		return(new KDialogPicker());
	    } catch(Unavailable e) {}
	    throw(new Unavailable("No system filepicker available"));
	}

	public void browse(java.net.URI location) throws IOException {
	    try {
		DesktopPortal.OpenURI xdg = DesktopPortal.get().OpenURI();
		xdg.OpenURI(location);
		return;
	    } catch(DBusError e) {
	    }
	    ProcessBuilder spec = new ProcessBuilder(Arrays.asList("xdg-open", location.toString()));
	    spec.inheritIO();
	    Process proc = spec.start();
	    try {
		int st = proc.waitFor();
		if(st != 0)
		    throw(new IOException("xdg-open failed."));
	    } catch(InterruptedException e) {
		Thread.currentThread().interrupt();
		throw(new IOException("interrupted"));
	    }
	}

	public void dispose() {
	    if(!closed) {
		synchronized(this) {
		    if(closed)
			return;
		    if(ctx != null)
			glx.glXDestroyContext(dpy, ctx);
		    if(im != null)
			xlib.XCloseIM(im);
		    if(dpy != null)
			xlib.XCloseDisplay(dpy);
		    closed = true;
		}
	    }
	}

	GLXContext ctx() {
	    return(GLXContext.this);
	}

	public String description() {
	    return(String.format("X11/GLX, %s/%d %s", srvvendor, srvrelease, wmname));
	}
    }

    private static final Map<XID, X11Keysym> extsyms = new HashMap<>();
    private Key.Sym getkeysym(XID sym) {
	Key.Sym ret = stdsyms.get(sym);
	if(ret == null) {
	    synchronized(extsyms) {
		X11Keysym k = extsyms.get(sym);
		if(k == null)
		    extsyms.put(sym, k = new X11Keysym(xlib, sym));
		ret = k;
	    }
	}
	return(ret);
    }

    public static class X11Keysym implements Key.Sym {
	public final XID sym;
	public final String id, nm;

	public X11Keysym(XLib xlib, XID sym) {
	    this.sym = sym;
	    this.nm = xlib.XKeysymToString(sym);
	    this.id = String.format("x11:%s", nm).intern();
	}

	public String id() {return(id);}
	public String nm() {return(nm);}

	public int hashCode() {return(sym.hashCode());}
	public boolean equals(X11Keysym that) {return(this.sym.equals(that.sym));}
	public boolean equals(Object x) {return((x instanceof X11Keysym) && equals((X11Keysym)x));}

	public String toString() {
	    return(String.format("#<x-keysym %s(%s)>", nm, sym));
	}
    }

    public static class X11Key implements Key {
	public final int code;
	public final XID[] rawsyms;
	public final Sym[] keysyms;
	public final String id;

	private X11Key(GLXToolkit tk, int code) {
	    this.code = code;
	    if((code >= tk.mincode) && (code <= tk.maxcode))
		this.rawsyms = tk.keymap[code - tk.mincode];
	    else
		this.rawsyms = new XID[0];
	    this.keysyms = new Sym[this.rawsyms.length];
	    for(int i = 0; i < this.rawsyms.length; i++)
		this.keysyms[i] = tk.ctx().getkeysym(this.rawsyms[i]);
	    this.id = ("x11:" + code).intern();
	}

	public String id() {return(id);}

	public Sym primary() {
	    return((keysyms.length > 0) ? keysyms[0] : null);
	}

	public Sym primary(Collection<? extends Sym> of) {
	    for(Sym sym : keysyms) {
		if(of.contains(sym))
		    return(sym);
	    }
	    return(null);
	}

	public String toString() {
	    return(String.format("#<x-key %d syms=%s>", code, Arrays.deepToString(keysyms)));
	}
    }

    public static abstract class GLXKeyEvent {
	public final GLXToolkit.GLXWindow wnd;
	public final int state;
	public final Set<Key.Mod> mods;
	public XID rawsym;
	public Key.Sym sym;
	public X11Key key;
	public String str = "";

	public GLXKeyEvent(GLXToolkit.GLXWindow wnd, XKeyEvent ev, boolean include) {
	    this.wnd = wnd;
	    this.state = ev.state();
	    this.key = new X11Key(wnd.toolkit(), ev.keycode());
	    this.mods = wnd.toolkit().mods(ev.state(), ev.keycode(), include);
	}

	public String string() {return(str);}
	public Key key() {return(key);}
	public Key.Sym sym() {return(sym);}
	public Set<Key.Mod> mods() {return(mods);}

	public String toString() {
	    return(String.format("#<%s %s state=%x sym=%s str=\"%s\">", getClass().getSimpleName(), key, state, rawsym, (str == null) ? "" : Utils.bprint.enc(str.getBytes(Utils.utf8))));
	}
    }
    public static class GLXKeyPressEvent extends GLXKeyEvent implements Toolkit.KeyDownEvent {
	GLXKeyPressEvent(GLXToolkit.GLXWindow wnd, XKeyEvent ev) {super(wnd, ev, true);}
    }
    public static class GLXKeyReleaseEvent extends GLXKeyEvent implements Toolkit.KeyUpEvent {
	GLXKeyReleaseEvent(GLXToolkit.GLXWindow wnd, XKeyEvent ev) {super(wnd, ev, false);}
    }

    private static MouseBtn buttonid(GLXToolkit.XIPointerInfo ptr, int xi) {
	switch(xi) {
	case 1: return(MouseBtn.Std.LEFT);
	case 2: return(MouseBtn.Std.MIDDLE);
	case 3: return(MouseBtn.Std.RIGHT);
	case 8: return(MouseBtn.Std.BACK);
	case 9: return(MouseBtn.Std.FORWARD);
	}
	if((xi <= ptr.buttons.length) && (ptr.buttons[xi - 1] != null)) {
	    String nm = ptr.buttons[xi - 1];
	    return(new MouseBtn() {
		public String id() {return(("x11:" + nm).intern());}
		public String nm() {return(nm);}
	    });
	} else {
	    return(new MouseBtn() {
		public String id() {return(("x11:" + xi).intern());}
		public String nm() {return("Button " + xi);}
	    });
	}
    }

    public static abstract class GLXMouseEvent {
	public final GLXToolkit.GLXWindow wnd;
	public final GLXToolkit.XIPointerInfo ptr;
	public final Coord wndc, rootc;
	public final Set<MouseBtn> held;
	public final Set<Key.Mod> mods;

	public GLXMouseEvent(GLXToolkit.GLXWindow wnd, GLXToolkit.XIPointerInfo ptr, XIDeviceEvent ev) {
	    this.wnd = wnd;
	    this.ptr = ptr;
	    this.wndc = Coord.of((int)ev.event_x(), (int)ev.event_y());
	    this.rootc = Coord.of((int)ev.root_x(), (int)ev.root_y());
	    this.held = new HashSet<>();
	    ev.buttons().forEach(b -> held.add(buttonid(ptr, b)));
	    this.mods = wnd.toolkit().mods(ev.mods().effective(), 0, true);
	}

	public Coord wndc() {return(wndc);}
	public Set<MouseBtn> held() {return(held);}
	public Set<Key.Mod> mods() {return(mods);}

	public String toString() {
	    return(String.format("#<%s wnd=%s root=%s held=%s mods=%s>", getClass().getSimpleName(), wndc, rootc, held, mods));
	}
    }
    public static abstract class GLXMouseButtonEvent extends GLXMouseEvent {
	public final MouseBtn button;

	public GLXMouseButtonEvent(GLXToolkit.GLXWindow wnd, GLXToolkit.XIPointerInfo ptr, XIDeviceEvent ev, boolean include) {
	    super(wnd, ptr, ev);
	    this.button = buttonid(ptr, ev.detail());
	    if(include)
		held.add(button);
	    else
		held.remove(button);
	}

	public MouseBtn button() {return(button);}

	public String toString() {
	    return(String.format("#<%s wnd=%s root=%s btn=%d held=%s mods=%s>", getClass().getSimpleName(), wndc, rootc, button, held, mods));
	}
    }
    public static class GLXMouseDownEvent extends GLXMouseButtonEvent implements Toolkit.MouseDownEvent {
	GLXMouseDownEvent(GLXToolkit.GLXWindow wnd, GLXToolkit.XIPointerInfo ptr, XIDeviceEvent ev) {super(wnd, ptr, ev, true);}
    }
    public static class GLXMouseUpEvent extends GLXMouseButtonEvent implements Toolkit.MouseUpEvent {
	GLXMouseUpEvent(GLXToolkit.GLXWindow wnd, GLXToolkit.XIPointerInfo ptr, XIDeviceEvent ev) {super(wnd, ptr, ev, false);}
    }
    public static class GLXMouseMoveEvent extends GLXMouseEvent implements Toolkit.MouseMoveEvent {
	GLXMouseMoveEvent(GLXToolkit.GLXWindow wnd, GLXToolkit.XIPointerInfo ptr, XIDeviceEvent ev) {super(wnd, ptr, ev);}
    }
    public static class GLXMouseWheelEvent extends GLXMouseEvent implements Toolkit.MouseWheelEvent {
	public final Axis axis;
	public final int amount;
	public final double sub;

	GLXMouseWheelEvent(GLXToolkit.GLXWindow wnd, GLXToolkit.XIPointerInfo ptr, XIDeviceEvent ev, Axis axis, int amount, double sub) {
	    super(wnd, ptr, ev);
	    this.axis = axis;
	    this.amount = amount;
	    this.sub = sub;
	}
	GLXMouseWheelEvent(GLXToolkit.GLXWindow wnd, GLXToolkit.XIPointerInfo ptr, XIDeviceEvent ev, Axis axis, int amount) {
	    this(wnd, ptr, ev, axis, amount, amount);
	}

	public Axis axis() {return(axis);}
	public int amount() {return(amount);}
	public double subamount() {return(sub);}
    }

    public static final Map<XID, Key.Std> stdsyms = Utils.<XID, Key.Std>map()
	.put(XK_Return, ENTER)             .put(XK_BackSpace, BACKSPACE)    .put(XK_Tab, TAB)
	.put(XK_Cancel, CANCEL)            .put(XK_Clear, CLEAR)            .put(XK_Pause, PAUSE)
	.put(XK_Caps_Lock, CAPSLOCK)       .put(XK_Escape, ESCAPE)          .put(XK_space, SPACE)
	.put(XK_Page_Up, PAGEUP)           .put(XK_Page_Down, PAGEDOWN)     .put(XK_End, END)
	.put(XK_Home, HOME)                .put(XK_Left, LEFT)              .put(XK_Up, UP)
	.put(XK_Right, RIGHT)              .put(XK_Down, DOWN)              .put(XK_comma, COMMA)
	.put(XK_minus, MINUS)              .put(XK_period, PERIOD)          .put(XK_slash, SLASH)
	.put(XK_semicolon, SEMICOLON)      .put(XK_equal, EQUALS)           .put(XK_bracketleft, LEFTBRACKET)
	.put(XK_bracketright, RIGHTBRACKET).put(XK_backslash, BACKSLASH)    .put(XK_Delete, DELETE)
	.put(XK_Num_Lock, NUMLOCK)         .put(XK_Scroll_Lock, SCROLLLOCK) .put(XK_Print, PRINTSCREEN)
	.put(XK_Insert, INSERT)            .put(XK_Help, HELP)              .put(XK_grave, BACKQUOTE)
	.put(XK_apostrophe, QUOTE)         .put(XK_bar, BAR)

	.put(XK_Shift_L, SHIFT)            .put(XK_Shift_R, SHIFT)          .put(XK_Control_L, CONTROL)
	.put(XK_Control_R, CONTROL)        .put(XK_Alt_L, ALT)              .put(XK_Alt_R, ALT)
	.put(XK_Meta_L, META)              .put(XK_Meta_R, META)            .put(XK_Super_L, WINDOWS)
	.put(XK_Super_R, WINDOWS)          .put(XK_ISO_Level3_Shift, ALTGR)

	.put(XK_0, N0)                     .put(XK_1, N1)                   .put(XK_2, N2)
	.put(XK_3, N3)                     .put(XK_4, N4)                   .put(XK_5, N5)
	.put(XK_6, N6)                     .put(XK_7, N7)                   .put(XK_8, N8)
	.put(XK_9, N9)

	.put(XK_A, A)                      .put(XK_B, B)                    .put(XK_C, C)
	.put(XK_D, D)                      .put(XK_E, E)                    .put(XK_F, F)
	.put(XK_G, G)                      .put(XK_H, H)                    .put(XK_I, I)
	.put(XK_J, J)                      .put(XK_K, K)                    .put(XK_L, L)
	.put(XK_M, M)                      .put(XK_N, N)                    .put(XK_O, O)
	.put(XK_P, P)                      .put(XK_Q, Q)                    .put(XK_R, R)
	.put(XK_S, S)                      .put(XK_T, T)                    .put(XK_U, U)
	.put(XK_V, V)                      .put(XK_W, W)                    .put(XK_X, X)
	.put(XK_Y, Y)                      .put(XK_Z, Z)
	.put(XK_a, A)                      .put(XK_b, B)                    .put(XK_c, C)
	.put(XK_d, D)                      .put(XK_e, E)                    .put(XK_f, F)
	.put(XK_g, G)                      .put(XK_h, H)                    .put(XK_i, I)
	.put(XK_j, J)                      .put(XK_k, K)                    .put(XK_l, L)
	.put(XK_m, M)                      .put(XK_n, N)                    .put(XK_o, O)
	.put(XK_p, P)                      .put(XK_q, Q)                    .put(XK_r, R)
	.put(XK_s, S)                      .put(XK_t, T)                    .put(XK_u, U)
	.put(XK_v, V)                      .put(XK_w, W)                    .put(XK_x, X)
	.put(XK_y, Y)                      .put(XK_z, Z)

	.put(XK_ampersand, AMPERSAND)      .put(XK_asterisk, ASTERISK)      .put(XK_quotedbl, DBLQUOTE)
	.put(XK_less, LT)                  .put(XK_greater, GT)             .put(XK_braceleft, LEFTBRACE)
	.put(XK_braceright, RIGHTBRACE)    .put(XK_at, AT)                  .put(XK_colon, COLON)
	.put(XK_asciicircum, CIRCUMFLEX)   .put(XK_dollar, DOLLAR)          .put(XK_EuroSign, EUROSIGN)
	.put(XK_exclam, EXCL)              .put(XK_exclamdown, INVEXCL)     .put(XK_parenleft, LEFTPAREN)
	.put(XK_parenright, RIGHTPAREN)    .put(XK_numbersign, NUMBERSIGN)  .put(XK_plus, PLUS)
	.put(XK_underscore, UNDERSCORE)    .put(XK_Menu, MENU)              .put(XK_Undo, UNDO)
	.put(XK_Redo, AGAIN)               .put(XK_Find, FIND)              .put(XK_Multi_key, COMPOSE)
	.put(XK_Begin, BEGIN)

	.put(XK_dead_grave, DEADGRAVE)            .put(XK_dead_acute, DEADACUTE)        .put(XK_dead_circumflex, DEADCIRCUMFLEX)
	.put(XK_dead_tilde, DEADTILDE)            .put(XK_dead_macron, DEADMACRON)      .put(XK_dead_breve, DEADBREVE)
	.put(XK_dead_abovedot, DEADABOVEDOT)      .put(XK_dead_diaeresis, DEADDIAERESIS).put(XK_dead_abovering, DEADABOVERING)
	.put(XK_dead_doubleacute, DEADDOUBLEACUTE).put(XK_dead_caron, DEADCARON)        .put(XK_dead_cedilla, DEADCEDILLA)
	.put(XK_dead_ogonek, DEADOGONEK)          .put(XK_dead_iota, DEADIOTA)          .put(XK_dead_voiced_sound, DEADVOICED)
	.put(XK_dead_semivoiced_sound, DEADSEMIVOICED)

	.put(XK_KP_Left, NP_LEFT)    .put(XK_KP_Up, NP_UP)           .put(XK_KP_Right, NP_RIGHT)
	.put(XK_KP_Down, NP_DOWN)    .put(XK_KP_Multiply, NP_MUL)    .put(XK_KP_Add, NP_ADD)
	.put(XK_KP_Separator, NP_SEP).put(XK_KP_Subtract, NP_SUB)    .put(XK_KP_Decimal, NP_DEC)
	.put(XK_KP_Divide, NP_DIV)
	.put(XK_KP_0, NP0)           .put(XK_KP_1, NP1)              .put(XK_KP_2, NP2)
	.put(XK_KP_3, NP3)           .put(XK_KP_4, NP4)              .put(XK_KP_5, NP5)
	.put(XK_KP_6, NP6)           .put(XK_KP_7, NP7)              .put(XK_KP_8, NP8)
	.put(XK_KP_9, NP9)
	.put(XK_KP_Home, HOME)       .put(XK_KP_Page_Up, PAGEUP)     .put(XK_KP_Page_Down, PAGEDOWN)
	.put(XK_KP_End, END)         .put(XK_KP_Insert, INSERT)      .put(XK_KP_Enter, ENTER)

	.put(XK_Henkan_Mode, CONVERT)   .put(XK_Muhenkan, NONCONVERT).put(XK_Henkan_Mode, MODECHANGE)
	.put(XK_Hiragana_Katakana, KANA).put(XK_Kanji, KANJI)        .put(XK_Katakana, KATAKANA)
	.put(XK_Hiragana, HIRAGANA)     .put(XK_Zenkaku, FULLWIDTH)  .put(XK_Hankaku, HALFWIDTH)
	.put(XK_Romaji, ROMAN)          .put(XK_Zen_Koho, ALLCAND)   .put(XK_Mae_Koho, PREVCAND)
	.put(XK_Kana_Lock, KANALOCK)

	.put(XK_F1, F1)                    .put(XK_F2, F2)                  .put(XK_F3, F3)
	.put(XK_F4, F1)                    .put(XK_F5, F2)                  .put(XK_F3, F3)
	.put(XK_F7, F1)                    .put(XK_F8, F2)                  .put(XK_F9, F9)
	.put(XK_F10, F10)                  .put(XK_F11, F11)                .put(XK_F12, F12)
	.put(XK_F13, F13)                  .put(XK_F14, F14)                .put(XK_F15, F15)
	.put(XK_F16, F16)                  .put(XK_F17, F17)                .put(XK_F18, F18)
	.put(XK_F19, F19)                  .put(XK_F20, F20)                .put(XK_F21, F21)
	.put(XK_F22, F22)                  .put(XK_F23, F23)                .put(XK_F24, F24)
	.map();
}
