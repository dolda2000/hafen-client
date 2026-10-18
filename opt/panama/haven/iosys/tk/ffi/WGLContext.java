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
import java.lang.foreign.*;
import java.awt.color.*;
import java.awt.image.*;
import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.nio.file.*;
import haven.*;
import haven.iosys.*;
import haven.render.*;
import haven.render.gl.*;
import haven.iosys.tk.*;
import haven.ffi.*;
import haven.ffi.windows.*;
import haven.ffi.gl.*;
import haven.ffi.windows.Win32.*;
import static haven.iosys.tk.Key.Std.*;

/*
 * TODO:
 *   * Use OLE clipboard.
 *   * Figure out why eventloop blocks while windows
 *     are being moved/resized.
 *   * Figure out why GL batches are sometimes being
 *     run after a window has been disposed.
 */
@Toolkit.Available(name = "wgl")
public class WGLContext implements Providers.Factory<Toolkit> {
    private final Win32 win;
    private final WGL wgl;
    private final SHCore shcore;
    private final COM com;

    private WGLContext() {
	try {
	    win = Win32.get();
	    wgl = WGL.get();
	    com = COM.get();
	} catch(RuntimeException e) {
	    throw(new Unavailable("Win32 environment not available", e));
	}
	SHCore shcore = null;
	try {
	    shcore = SHCore.get();
	} catch(RuntimeException e) {}
	this.shcore = shcore;
    }

    private static WGLContext instance = null;
    public static WGLContext get() {
	if(instance == null) {
	    synchronized(WGLContext.class) {
		if(instance == null)
		    instance = new WGLContext();
	    }
	}
	return(instance);
    }

    public Toolkit open(String... args) {
	return(new WGLToolkit());
    }

    public int priority() {
	return(System.getProperty("os.name", "").startsWith("Windows ") ? 100 : 0);
    }
    public boolean experimental() {return(false);}

    public class WGLToolkit implements Toolkit {
	private static final int WM_USER_WAKE  = Win32.WM_USER + 1;
	private static final int WM_USER_XLATE = Win32.WM_USER + 2;
	private static final int[][] glversions = {
	    {4, 6}, {4, 5}, {4, 4}, {4, 3}, {4, 2}, {4, 1}, {4, 0},
				    {3, 3}, {3, 2}, {3, 1}, {3, 0},
						    {2, 1}, {2, 0},
	};
	private static int serial = 0;
	private final String wclassname = "haven-window-" + serial;
	private final Handle hInstance;
	private final WndClassEx wndclass;
	private final KeyboardState kb = new KeyboardState();
	private final Map<Handle, LayoutMap> layouts = new HashMap<>();
	private final Map<W32Key, W32Key> curkeys = new HashMap<>();
	private WGL.Ext text;
	private Handle ctx;
	private OpenGL gl;
	private int pfmt;
	private PIXELFORMATDESCRIPTOR pfd;
	private Collection<String> exts;

	private WGLToolkit() {
	    hInstance = win.GetModuleHandle(null);
	    wndclass = win.WndClassEx();
	    wndclass.hInstance(hInstance).lpszClassName(wclassname).lpfnWndProc(this::wndproc).style(Win32.CS_OWNDC);
	    win.RegisterClassEx(wndclass);
	    createctx();
	}

	private void createctx() {
	    Handle hwnd = null, hdc = null, temp = null;
	    ctx = null;
	    StdError lasterr = null;
	    try {
		hwnd = win.CreateWindowEx(0, wclassname, null, 0, null, null, null, null, hInstance);
		hdc = win.GetDC(hwnd);
		pfd = win.PIXELFORMATDESCRIPTOR();
		pfd.nVersion(1);
		pfd.dwFlags(Win32.PFD_DRAW_TO_WINDOW | Win32.PFD_SUPPORT_OPENGL | Win32.PFD_DOUBLEBUFFER);
		pfd.cColorBits(24).cAlphaBits(8).cDepthBits(24);
		pfmt = win.ChoosePixelFormat(hdc, pfd);
		win.SetPixelFormat(hdc, pfmt, pfd);
		temp = wgl.wglCreateContext(hdc);
		wgl.wglMakeCurrent(hdc, temp);
		text = wgl.ext();
		try {
		    exts = new HashSet<>(Arrays.asList(text.wglGetExtensionsStringEXT().split(" ")));
		} catch(MissingFunction e) {
		    try {
			exts = new HashSet<>(Arrays.asList(text.wglGetExtensionsStringARB(hdc).split(" ")));
		    } catch(MissingFunction e2) {
			throw(new Unavailable("No WGL extensions available"));
		    }
		}
		if(!exts.contains("WGL_ARB_create_context_profile"))
		    throw(new Unavailable("WGL_ARB_create_context_profile not available"));
		if(!exts.contains("WGL_EXT_swap_control"))
		    throw(new Unavailable("WGL_EXT_swap_control not available"));
		
		for(int[] ver : glversions) {
		    try {
			ctx = text.wglCreateContextAttribsARB(hdc, null, new int[] {
				WGL.WGL_CONTEXT_PROFILE_MASK_ARB, WGL.WGL_CONTEXT_CORE_PROFILE_BIT_ARB,
				WGL.WGL_CONTEXT_MAJOR_VERSION_ARB, ver[0], WGL.WGL_CONTEXT_MINOR_VERSION_ARB, ver[1],
				0, 0,
			    });
			break;
		    } catch(StdError e) {
			lasterr = e;
		    }
		}
		if(ctx == null) 
		    throw(new Unavailable("could not create OpenGL context", lasterr));
		wgl.wglMakeCurrent(hdc, ctx);
		gl = wgl.gl();
		wgl.wglMakeCurrent(hdc, null);
	    } finally {
		if(temp != null)
		    wgl.wglDeleteContext(temp);
		if(hdc != null)
		    win.ReleaseDC(hwnd, hdc);
		if(hwnd != null)
		    win.DestroyWindow(hwnd);
	    }
	}

	public class W32Monitor implements Monitor { 
	    public final MONITORINFO minf;
	    public final int udpi, mdpi, gdpi;
	    public final int fac;

	    public W32Monitor(Handle hm, int gdpi) {
		this.minf = win.MONITORINFO();
		win.GetMonitorInfo(hm, minf);
		this.gdpi = gdpi;
		int udpi = 0, mdpi = 0, fac = 0;
		if(shcore != null) {
		    try {
			Coord v = shcore.GetDpiForMonitor(hm, SHCore.MDT_EFFECTIVE_DPI);
			udpi = (v.x + v.y) / 2;
		    } catch(HResultError e) {}
		    try {
			Coord v = shcore.GetDpiForMonitor(hm, SHCore.MDT_RAW_DPI);
			mdpi = (v.x + v.y) / 2;
		    } catch(HResultError e) {}
		    try {
			fac = shcore.GetScaleFactorForMonitor(hm);
		    } catch(HResultError e) {}
		}
		this.udpi = udpi; this.mdpi = mdpi; this.fac = fac;
	    }

	    public Coord resolution() {
		return(minf.rcMonitor().sz());
	    }

	    public int refresh() {
		return(0);
	    }

	    public double scaling() {
		return(fac / 100.0);
	    }

	    public double userdpi() {
		return((udpi == 0) ? gdpi : udpi);
	    }

	    public double density() {
		return(mdpi);
	    }

	    public String toString() {
		return(String.format("#<w32-monitor %s u%ddpi m%ddpi g%ddpi %d%%>", minf.rcMonitor(), udpi, mdpi, gdpi, fac));
	    }
	}

	public Collection<Monitor> monitors() {
	    Collection<Monitor> ret = new ArrayList<>();
	    int gdpi;
	    Handle hdc = win.GetDC(null);
	    try {
		gdpi = (win.GetDeviceCaps(hdc, Win32.LOGPIXELSX) + win.GetDeviceCaps(hdc, Win32.LOGPIXELSY)) / 2;
	    } finally {
		win.ReleaseDC(null, hdc);
	    }
	    for(Handle monitor : win.EnumDisplayMonitors())
		ret.add(new W32Monitor(monitor, gdpi));
	    return(ret);
	}

	public Cursor.Caps cursorcaps() {
	    return(new Cursor.Caps(256, Math.min(win.GetSystemMetrics(Win32.SM_CXCURSOR), win.GetSystemMetrics(Win32.SM_CYCURSOR))));
	}

	public class HCursor implements Cursor {
	    public final Handle id;
	    private final Runnable clean;

	    public HCursor(Handle id, boolean shared) {
		this.id = id;
		Win32 win = WGLContext.this.win;
		if(shared)
		    clean = null;
		else
		    clean = Finalizer.finalize(this, () -> win.DestroyIcon(id));
	    }

	    public void dispose() {
		clean.run();
	    }
	}
	public final HCursor defcurs = new HCursor(win.LoadCursor(null, Win32.IDC_ARROW), true);
	public final HCursor nocursor = new HCursor(Handle.nil, true);
	public final Map<Cursor.Std, HCursor> stdcursors = Utils.<Cursor.Std, HCursor>map()
	    .put(Cursor.Std.DEFAULT,   defcurs)
	    .put(Cursor.Std.NONE,      nocursor)
	    .put(Cursor.Std.POINTER,   new HCursor(win.LoadCursor(null, Win32.IDC_ARROW), true))
	    .put(Cursor.Std.WAIT,      new HCursor(win.LoadCursor(null, Win32.IDC_WAIT), true))
	    .put(Cursor.Std.HAND,      new HCursor(win.LoadCursor(null, Win32.IDC_HAND), true))
	    .put(Cursor.Std.MOVE,      new HCursor(win.LoadCursor(null, Win32.IDC_SIZEALL), true))
	    .put(Cursor.Std.CARET,     new HCursor(win.LoadCursor(null, Win32.IDC_IBEAM), true))
	    .put(Cursor.Std.CROSSHAIR, new HCursor(win.LoadCursor(null, Win32.IDC_CROSS), true))
	    .put(Cursor.Std.SIZE_N,    new HCursor(win.LoadCursor(null, Win32.IDC_SIZENS), true))
	    .put(Cursor.Std.SIZE_NE,   new HCursor(win.LoadCursor(null, Win32.IDC_SIZENESW), true))
	    .put(Cursor.Std.SIZE_E,    new HCursor(win.LoadCursor(null, Win32.IDC_SIZEWE), true))
	    .put(Cursor.Std.SIZE_SE,   new HCursor(win.LoadCursor(null, Win32.IDC_SIZENWSE), true))
	    .put(Cursor.Std.SIZE_S,    new HCursor(win.LoadCursor(null, Win32.IDC_SIZENS), true))
	    .put(Cursor.Std.SIZE_SW,   new HCursor(win.LoadCursor(null, Win32.IDC_SIZENESW), true))
	    .put(Cursor.Std.SIZE_W,    new HCursor(win.LoadCursor(null, Win32.IDC_SIZEWE), true))
	    .put(Cursor.Std.SIZE_NW,   new HCursor(win.LoadCursor(null, Win32.IDC_SIZENWSE), true))
	    .map();

	public Cursor makecursor(BufferedImage image, Coord hotspot) {
	    return(new HCursor(makeicon(image, hotspot), false));
	}

	private long wndproc(Handle hwnd, int umsg, long wparam, long lparam) {
	    WGLWindow wnd;
	    synchronized(mtmon) {
		wnd = windows.get(hwnd);
	    }
	    if(wnd == null) {
		// Warning.warn(String.format("message received for non-registered windows %s: %d", hwnd, umsg));
		return(win.DefWindowProc(hwnd, umsg, wparam, lparam));
	    }
	    return(wnd.message(hwnd, umsg, wparam, lparam));
	}

	private long keymsgid = 1;
	private void dispatchmsg(MSG msg) {
	    boolean bracket = (msg.hwnd() != Handle.nil) && ((msg.message() == Win32.WM_KEYDOWN) ||
							     (msg.message() == Win32.WM_SYSKEYDOWN));
	    if(bracket)
		win.PostMessage(msg.hwnd(), WM_USER_XLATE, keymsgid, 1);
	    win.TranslateMessage(msg);
	    if(bracket)
		win.PostMessage(msg.hwnd(), WM_USER_XLATE, keymsgid, 2);
	    win.DispatchMessage(msg);
	}

	private final Object mtmon = new Object();
	private Thread msgthread = null;
	private boolean msgrunning = true;
	private volatile int msgthreadid = 0;
	private final Map<Handle, WGLWindow> windows = new HashMap<>();
	private final Queue<Runnable> mtasks = new LinkedList<>();
	private void handlemsgs() {
	    try {
		com.OleInitialize();
		MSG msg = win.MSG();
		win.PeekMessage(msg, null, 0, 0, Win32.PM_NOREMOVE);
		synchronized(mtmon) {
		    msgthreadid = win.GetCurrentThreadId();
		    mtmon.notifyAll();
		}
		while(true) {
		    Runnable task;
		    synchronized(mtmon) {
			task = mtasks.poll();
			if((task == null) && windows.isEmpty() && !msgrunning) {
			    msgthread = null;
			    return;
			}
		    }
		    if(task != null)
			task.run();
		    while(win.PeekMessage(msg, null, 0, 0, Win32.PM_REMOVE))
			dispatchmsg(msg);
		    if(task == null) {
			if(win.GetMessage(msg, null, 0, 0))
			    dispatchmsg(msg);
		    }
		}
	    } finally {
		synchronized(mtmon) {
		    if(msgthread == Thread.currentThread())
			msgthread = null;
		}
		com.OleUninitialize();
	    }
	}

	private void ckmsgthread() {
	    if(msgthread == null) {
		msgthreadid = 0;
		Thread th = new HackThread(this::handlemsgs, "Win32 message dispatch thread");
		th.start();
		msgthread = th;
		boolean irq = false;
		while(msgthreadid == 0) {
		    try {
			mtmon.wait();
		    } catch(InterruptedException e) {
			irq = true;
		    }
		}
		if(irq)
		    Thread.currentThread().interrupt();
	    }
	}

	private void msgrun(Runnable task) {
	    synchronized(mtmon) {
		mtasks.add(task);
		ckmsgthread();
	    }
	    win.PostThreadMessage(msgthreadid, WM_USER_WAKE, 0, 0);
	}

	private <T> T msgrun(Supplier<T> task) {
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
	    msgrun(r);
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

	public static class MissingDCException extends RuntimeException {
	    public final boolean destroyed, showing;
	    public final Windeye.State state;
	    public final Coord size;

	    public MissingDCException(WGLWindow wnd) {
		super("GDI device context missing for window");
		this.destroyed = wnd.destroyed;
		this.showing = wnd.showing;
		this.state = wnd.curstate;
		this.size = wnd.size;
	    }
	}

	private void glrun(WGLWindow wnd, Runnable task) {
	    msgrun(() -> {
		Handle hdc;
		try {
		    hdc = win.GetDC(wnd.hwnd);
		} catch(StdError e) {
		    throw(new MissingDCException(wnd));
		}
		try {
		    wgl.wglMakeCurrent(hdc, ctx);
		    try {
			task.run();
		    } finally {
			wgl.wglMakeCurrent(hdc, null);
		    }
		} finally {
		    win.ReleaseDC(wnd.hwnd, hdc);
		}
	    });
	}

	private <T> T glrun(WGLWindow wnd, Supplier<T> task) {
	    return(msgrun(() -> {
		Handle hdc;
		try {
		    hdc = win.GetDC(wnd.hwnd);
		} catch(StdError e) {
		    throw(new MissingDCException(wnd));
		}
		try {
		    wgl.wglMakeCurrent(hdc, ctx);
		    T ret;
		    try {
			ret = task.get();
		    } finally {
			wgl.wglMakeCurrent(hdc, null);
		    }
		    return(ret);
		} finally {
		    win.ReleaseDC(wnd.hwnd, hdc);
		}
	    }));
	}

	private void register(WGLWindow wnd) {
	    synchronized(mtmon) {
		windows.put(wnd.hwnd, wnd);
		ckmsgthread();
	    }
	}

	private void unregister(WGLWindow wnd) {
	    synchronized(mtmon) {
		WGLWindow reg = windows.remove(wnd.hwnd);
		if(reg == null)
		    throw(new IllegalStateException("window unregistered doubly"));
		else if(reg != wnd)
		    throw(new RuntimeException("window registration not matching"));
	    }
	}

	public class LayoutMap {
	    private final Map<Integer, Key.Std[]> syms = new HashMap<>();

	    LayoutMap(Handle hkl) {
		Map<Integer, List<Integer>> scan = new HashMap<>();
		Set<Character> seen = new HashSet<>();
		Key.Std[] all = Key.Std.values();
		for(int i = 0; i < all.length; i++) {
		    Key.Std sym = all[i];
		    if((sym.ch == 0) || seen.contains(sym.ch))
			continue;
		    seen.add(sym.ch);
		    int sv = win.VkKeyScanEx(sym.ch, hkl);
		    if(sv == 0xffff)
			continue;
		    int vk = sv & 0xff, ss = (sv >>> 8) & 0xff;
		    int vsc = win.MapVirtualKeyEx(vk, Win32.MAPVK_VK_TO_VSC_EX, hkl);
		    if(vsc == 0)
			continue;
		    if((vsc & ~0xff) != 0)
			vsc = 0x100 | (vsc & 0xff);
		    scan.computeIfAbsent(vsc, k -> new ArrayList<>()).add((ss << 16) | i);
		}
		for(Map.Entry<Integer, List<Integer>> ent : scan.entrySet()) {
		    List<Integer> vals = ent.getValue();
		    Collections.sort(vals);
		    Key.Std[] syms = new Key.Std[vals.size()];
		    for(int i = 0; i < vals.size(); i++)
			syms[i] = all[vals.get(i) & 0xffff];
		    this.syms.put(ent.getKey(), syms);
		}
	    }

	    public Key.Sym[] syms(boolean ext, int sc) {
		int vsc = (sc & 0xff) | (ext ? 0x100 : 0);
		Key.Std[] ret = syms.get(vsc);
		return((ret == null) ? new Key.Std[0] : ret);
	    }
	}

	public class VKSym implements Key.Sym {
	    public final int vk;
	    public final String id, nm;

	    public VKSym(int vk, int sc, boolean ext) {
		this.vk = vk;
		this.id = ("w32:v" + vk).intern();
		this.nm = win.GetKeyNameText((sc << 16) | (ext ? (1 << 24) : 0));
	    }

	    public String id() {return(id);}
	    public String nm() {return(nm);}

	    public int hashCode() {return(vk);}
	    public boolean equals(VKSym that) {return(this.vk == that.vk);}
	    public boolean equals(Object x) {return((x instanceof VKSym) && equals((VKSym)x));}

	    public String toString() {
		return(String.format("#<vksym %s(%x)>", nm, vk));
	    }
	}

	public class CharSym implements Key.Sym {
	    public final char ch;
	    public final String id, nm;

	    public CharSym(char ch) {
		this.ch = ch;
		this.id = ("w32:c" + (int)ch).intern();
		this.nm = Character.toString(ch);
	    }

	    public String id() {return(id);}
	    public String nm() {return(nm);}

	    public int hashCode() {return(ch);}
	    public boolean equals(CharSym that) {return(this.ch == that.ch);}
	    public boolean equals(Object x) {return((x instanceof CharSym) && equals((CharSym)x));}

	    public String toString() {
		return(String.format("#<charsym %s>", nm));
	    }
	}

	public static class W32ScanCode implements Key.Loc {
	    public final int sc;

	    public W32ScanCode(int sc) {
		this.sc = sc;
	    }

	    public String id() {return(("32:" + sc).intern());}
	    public String toString() {return("<" + sc + ">");}
	}

	public class W32Key implements Key {
	    public final Handle pkl;
	    public final int vk, sc;
	    public final boolean ext;
	    public final Loc loc;

	    public W32Key(Handle pkl, int vk, int sc, boolean ext) {
		this.pkl = pkl;
		this.vk = vk;
		this.sc = sc;
		this.ext = ext;
		Loc loc = stdkeys.get(sc + (ext ? 500 : 0));
		this.loc = (loc != null) ? loc : new W32ScanCode(sc);
	    }

	    private Sym[] syms = null;
	    public Sym[] syms() {
		if(syms == null) {
		    Sym vsym = stdvsyms.get(vk);
		    vsym = (vsym != null) ? vsym : new VKSym(vk, sc, ext);
		    char prich = (char)win.MapVirtualKeyEx(vk, Win32.MAPVK_VK_TO_CHAR, pkl);
		    Sym csym = (prich == 0) ? null : stdcsyms.get(prich);
		    csym = (csym != null) ? csym : (prich != 0) ? new CharSym(prich) : null;
		    ArrayList<Sym> syms = new ArrayList<>();
		    if(vsym != null)
			syms.add(vsym);
		    if((csym != null) && !syms.contains(csym))
			syms.add(csym);
		    for(Sym msym : layouts.computeIfAbsent(pkl, LayoutMap::new).syms(ext, sc)) {
			if(!syms.contains(msym))
			    syms.add(msym);
		    }
		    for(Handle hkl : win.GetKeyboardLayoutList()) {
			if(Utils.eq(hkl, pkl))
			    continue;
			for(Sym msym : layouts.computeIfAbsent(hkl, LayoutMap::new).syms(ext, sc)) {
			    if(!syms.contains(msym))
				syms.add(msym);
			}
		    }
		    this.syms = syms.toArray(new Sym[0]);
		}
		return(syms);
	    }

	    public String id() {
		if(ext)
		    return(("w32e:" + sc).intern());
		return(("w32:" + sc).intern());
	    }

	    public Loc location() {return(loc);}

	    public Sym primary() {
		if(syms().length > 0)
		    return(syms()[0]);
		return(null);
	    }

	    public Sym primary(Collection<? extends Sym> of) {
		for(Sym sym : syms()) {
		    if(of.contains(sym))
			return(sym);
		}
		return(null);
	    }

	    public int hashCode() {return((((pkl.hashCode() * 31) + vk) * 31) + sc + (ext ? 256 : 0));}
	    public boolean equals(W32Key that) {return(Utils.eq(this.pkl, that.pkl) && (this.vk == that.vk) &&
						       (this.sc == that.sc) && (this.ext == that.ext));}
	    public boolean equals(Object x) {return((x instanceof W32Key) && equals((W32Key)x));}

	    public String toString() {
		return(String.format("#<winkey vk=%x sc=%x%s %s syms=%s>", vk, sc, ext ? "(ext)" : "", loc, Arrays.deepToString(syms())));
	    }
	}

	public class WGLWindow implements Windeye {
	    private static final Coord2d[] visgrid = {
		Coord2d.of(0.1, 0.1), Coord2d.of(0.5, 0.1), Coord2d.of(0.9, 0.1),
		Coord2d.of(0.1, 0.5), Coord2d.of(0.5, 0.5), Coord2d.of(0.9, 0.5),
		Coord2d.of(0.1, 0.9), Coord2d.of(0.5, 0.9), Coord2d.of(0.9, 0.9),
	    };
	    public final Handle hwnd;
	    private final Collection<EventListener> callbacks = new java.util.concurrent.CopyOnWriteArrayList<>();
	    private final W32Clipboard clipboard = new W32Clipboard();
	    private final boolean[] visible = new boolean[visgrid.length];
	    private Coord size = Coord.z;
	    private boolean focused, showing = false, destroyed = false;
	    private State curstate = State.NORMAL;
	    private WGLEnvironment renv;
	    private int vwheelacc = 0, hwheelacc = 0;
	    private Coord fixsize = null, minsize = null, maxsize = null;
	    private String wndstyle = "normal";
	    private Area preexcl = null;
	    private Handle icon = null, cursor = null;
	    private int cursi = -1;

	    private WGLWindow() {
		Arrays.fill(visible, true);
		hwnd = win.CreateWindowEx(0, wclassname, null, STYLE_NORMAL,
					  null, null, null, null, hInstance);
		register(this);
		Handle hdc = win.GetDC(hwnd);
		try {
		    win.SetPixelFormat(hdc, pfmt, pfd);
		} finally {
		    win.ReleaseDC(hwnd, hdc);
		}
	    }

	    public class WGLEnvironment extends FFIEnvironment {
		private int qstate;

		private WGLEnvironment() {
		    super(gl);
		}

		private void process() {
		    synchronized(this) {
			qstate = 2;
		    }
		    process(gl);
		    synchronized(this) {
			if((qstate & 1) != 0)
			    glrun(WGLWindow.this, (Runnable)this::process);
			qstate &= ~2;
		    }
		}

		public void submit(Render cmd) {
		    super.submit(cmd);
		    synchronized(this) {
			if(renv == this) {
			    if(qstate == 0)
				glrun(WGLWindow.this, (Runnable)this::process);
			    qstate |= 1;
			}
		    }
		}

		public WGLWindow wnd() {
		    return(WGLWindow.this);
		}
	    }

	    public WGLToolkit toolkit() {
		return(WGLToolkit.this);
	    }

	    public void add(EventListener l) {
		callbacks.add(l);
	    }

	    private void callback(Event ev) {
		for(EventListener l : callbacks)
		    l.event(ev);
	    }
	    private void callback(W32KeyDownEvent ev) {
		ev.finish();
		/* Filter IME-consumed keys entirely. */
		if(ev.code == Win32.VK_PROCESSKEY)
		    return;
		callback((Event)ev);
	    }

	    private void minmaxinfo(long wparam, long lparam) {
		Win32.MINMAXINFO inf = win.MINMAXINFO(lparam);
		if(minsize != null)
		    inf.ptMinTrackSize(minsize);
		if(maxsize != null)
		    inf.ptMaxTrackSize(minsize);
	    }

	    private W32KeyDownEvent xlating = null;
	    private boolean xlstate = false;
	    private long message(Handle hwnd, int umsg, long wparam, long lparam) {
		// System.err.printf("%x %x %x\n", umsg, wparam, lparam);
		switch(umsg) {
		case Win32.WM_CLOSE:
		    callback(new CloseRequest() {});
		    return(0);
		case Win32.WM_SHOWWINDOW:
		    showing = wparam != 0;
		    return(0);
		case Win32.WM_SETFOCUS:
		    Arrays.fill(visible, true);
		    focused = true;
		    return(0);
		case Win32.WM_KILLFOCUS:
		    focused = false;
		    return(0);
		case Win32.WM_SIZE:
		    if(wparam == Win32.SIZE_RESTORED)
			curstate = State.NORMAL;
		    else if(wparam == Win32.SIZE_MINIMIZED)
			curstate = State.MINIMIZED;
		    else if(wparam == Win32.SIZE_MAXIMIZED)
			curstate = State.MAXIMIZED;
		    size = Coord.of((int)((lparam & 0x0000ffff) >> 0),
				    (int)((lparam & 0xffff0000) >> 16));
		    return(0);
		case Win32.WM_GETMINMAXINFO:
		    minmaxinfo(wparam, lparam);
		    return(0);
		case Win32.WM_SETCURSOR:
		    if(((lparam & 0x0000ffff) == Win32.HTCLIENT) && (cursor != null)) {
			win.SetCursor(cursor);
			return(0);
		    }
		    return(win.DefWindowProc(hwnd, umsg, wparam, lparam));

		case Win32.WM_KEYDOWN: case Win32.WM_SYSKEYDOWN:
		    if(xlating != null) {
			/* Apparently, the Win32 message queue always picks all posted
			 * messages (including from TranslateMessage) before picking any
			 * new system-posted messages, so there *should* never be a reason
			 * to be buffering multiple WM_KEYDOWNs for out-of-order
			 * translation. Perhaps there might still be some weird Windows
			 * thing necessitating this at some point in the future,
			 * however... */
			Warning.warn("WM_KEYDOWN strangely not done translating upon new one: " + xlating);
			callback(xlating);
		    }
		    xlating = new W32KeyDownEvent(keymsgid++, wparam, lparam);
		    return((umsg == Win32.WM_KEYDOWN) ? 0 : win.DefWindowProc(hwnd, umsg, wparam, lparam));
		case Win32.WM_CHAR: case Win32.WM_SYSCHAR:
		    if(xlstate) {
			xlating.strbuf.append((char)wparam);
		    } else {
			callback(new W32CharEvent((char)wparam));
		    }
		    return((umsg == Win32.WM_CHAR) ? 0 : win.DefWindowProc(hwnd, umsg, wparam, lparam));
		case WM_USER_XLATE:
		    if(xlating == null) {
			Warning.warn("got WM_USER_XLATE without any pending WM_KEYDOWN");
		    } else if(xlating.keymsgid != wparam) {
			Warning.warn("got WM_USER_XLATE with mismatched keymsgid");
		    } else {
			if(lparam == 1) {
			    xlstate = true;
			} else if(lparam == 2) {
			    callback(xlating);
			    xlating = null;
			    xlstate = false;
			}
		    }
		    return(0);
		case Win32.WM_KEYUP: case Win32.WM_SYSKEYUP:
		    callback(new W32KeyUpEvent(wparam, lparam));
		    return((umsg == Win32.WM_KEYUP) ? 0 : win.DefWindowProc(hwnd, umsg, wparam, lparam));
		case Win32.WM_SYSCOMMAND:
		    if((wparam & 0xfff0) == Win32.SC_KEYMENU)
			return(0);
		    return(win.DefWindowProc(hwnd, umsg, wparam, lparam));

		case Win32.WM_MOUSEMOVE:
		    callback(new W32MouseMoveEvent(wparam, lparam));
		    return(0);
		case Win32.WM_LBUTTONDOWN:
		    callback(new W32MouseDownEvent(MouseBtn.Std.LEFT, wparam, lparam));
		    return(0);
		case Win32.WM_LBUTTONUP:
		    callback(new W32MouseUpEvent(MouseBtn.Std.LEFT, wparam, lparam));
		    return(0);
		case Win32.WM_MBUTTONDOWN:
		    callback(new W32MouseDownEvent(MouseBtn.Std.MIDDLE, wparam, lparam));
		    return(0);
		case Win32.WM_MBUTTONUP:
		    callback(new W32MouseUpEvent(MouseBtn.Std.MIDDLE, wparam, lparam));
		    return(0);
		case Win32.WM_RBUTTONDOWN:
		    callback(new W32MouseDownEvent(MouseBtn.Std.RIGHT, wparam, lparam));
		    return(0);
		case Win32.WM_RBUTTONUP:
		    callback(new W32MouseUpEvent(MouseBtn.Std.RIGHT, wparam, lparam));
		    return(0);
		case Win32.WM_XBUTTONDOWN: {
		    long btn = (wparam & 0xffff0000) >> 16;
		    if(btn == 1)
			callback(new W32MouseDownEvent(MouseBtn.Std.BACK, wparam, lparam));
		    else if(btn == 2)
			callback(new W32MouseDownEvent(MouseBtn.Std.FORWARD, wparam, lparam));
		    return(0);
		}
		case Win32.WM_XBUTTONUP: {
		    long btn = (wparam & 0xffff0000) >> 16;
		    if(btn == 1)
			callback(new W32MouseUpEvent(MouseBtn.Std.BACK, wparam, lparam));
		    else if(btn == 2)
			callback(new W32MouseUpEvent(MouseBtn.Std.FORWARD, wparam, lparam));
		    return(0);
		}
		case Win32.WM_MOUSEWHEEL: {
		    int raw = (short)((wparam & 0xffff0000) >> 16);
		    vwheelacc += raw;
		    int amount = vwheelacc / 120;
		    vwheelacc -= amount * 120;
		    callback(new W32MouseWheelEvent(MouseWheelEvent.Axis.VERT, amount, raw / 120.0, wparam, lparam));
		    return(0);
		}
		case Win32.WM_HMOUSEWHEEL: {
		    int raw = (short)((wparam & 0xffff0000) >> 16);
		    hwheelacc += raw;
		    int amount = hwheelacc / 120;
		    hwheelacc -= amount * 120;
		    callback(new W32MouseWheelEvent(MouseWheelEvent.Axis.HORIZ, amount, raw / 120.0, wparam, lparam));
		    return(0);
		}
		case Win32.WM_DESTROYCLIPBOARD: {
		    clipboard.lost();
		    return(0);
		}
		default:
		    return(win.DefWindowProc(hwnd, umsg, wparam, lparam));
		}
	    }

	    private static final int STYLE_NORMAL    = Win32.WS_OVERLAPPEDWINDOW;
	    private static final int STYLE_FIXED     = Win32.WS_OVERLAPPED | Win32.WS_CAPTION | Win32.WS_SYSMENU | Win32.WS_MINIMIZEBOX;
	    private static final int STYLE_EXCLUSIVE = Win32.WS_POPUP | Win32.WS_MINIMIZEBOX;
	    private static final int STYLE_MASK      = STYLE_NORMAL | STYLE_FIXED | STYLE_EXCLUSIVE;
	    private void setstate(State st) {
		if(st == State.EXCLUSIVE) {
		    if(wndstyle != "popup") {
			Win32.MONITORINFO inf = win.MONITORINFO();
			win.GetMonitorInfo(win.MonitorFromWindow(hwnd, Win32.MONITOR_DEFAULTTOPRIMARY), inf);
			Area fs = inf.rcMonitor();
			preexcl = win.GetWindowRect(hwnd);
			win.SetWindowLongPtr(hwnd, Win32.GWL_STYLE, (win.GetWindowLongPtr(hwnd, Win32.GWL_STYLE) & ~STYLE_MASK) | STYLE_EXCLUSIVE);
			win.SetWindowPos(hwnd, null, fs.ul, fs.sz(), Win32.SWP_FRAMECHANGED);
			wndstyle = "popup";
		    }
		} else {
		    if(fixsize != null) {
			if(wndstyle != "fixed") {
			    win.SetWindowLongPtr(hwnd, Win32.GWL_STYLE, Win32.WS_CAPTION | Win32.WS_SYSMENU);
			    win.SetWindowPos(hwnd, null, null, fixsize, Win32.SWP_FRAMECHANGED);
			    wndstyle = "fixed";
			}
		    } else {
			if(wndstyle != "normal") {
			    win.SetWindowLongPtr(hwnd, Win32.GWL_STYLE, Win32.WS_OVERLAPPEDWINDOW);
			    if(preexcl != null) {
				win.SetWindowPos(hwnd, null, preexcl.ul, preexcl.sz(), Win32.SWP_FRAMECHANGED);
				preexcl = null;
			    } else {
				win.SetWindowPos(hwnd, null, null, null, Win32.SWP_FRAMECHANGED);
			    }
			    wndstyle = "normal";
			}
		    }
		    switch(st) {
		    case MINIMIZED: win.ShowWindow(hwnd, Win32.SW_MINIMIZE); break;
		    case NORMAL:    win.ShowWindow(hwnd, Win32.SW_NORMAL); break;
		    case MAXIMIZED: win.ShowWindow(hwnd, Win32.SW_MAXIMIZE); break;
		    }
		}
	    }

	    public WGLWindow show(boolean vis) {
		if(vis) {
		    boolean rv = msgrun(() -> {setstate(curstate); return(true);});
		} else {
		    boolean rv = msgrun(() -> win.ShowWindow(hwnd, Win32.SW_HIDE));
		}
		return(this);
	    }

	    public WGLWindow sizing(Sizing info) {
		msgrun(() -> {
		    if((info.normsize != null) && (curstate == State.NORMAL))
			win.SetWindowPos(hwnd, null, null, fixsize(info.normsize), 0);
		    fixsize = fixsize(info.fixsize);
		    minsize = fixsize(info.minsize);
		    maxsize = fixsize(info.maxsize);
		    if(showing)
			setstate(curstate);
		});
		return(this);
	    }

	    public WGLWindow state(State st) {
		msgrun(() -> {
		    if(showing)
			setstate(st);
		    curstate = st;
		});
		return(this);
	    }

	    public WGLWindow title(String name) {
		msgrun(() -> win.SetWindowText(hwnd, name));
		return(this);
	    }

	    public WGLWindow icon(BufferedImage icon) {
		Handle nicon = makeicon(icon, null);
		win.SendMessage(hwnd, Win32.WM_SETICON, Win32.ICON_SMALL, nicon);
		win.SendMessage(hwnd, Win32.WM_SETICON, Win32.ICON_BIG, nicon);
		if(this.icon != null)
		    win.DestroyIcon(this.icon);
		this.icon = nicon;
		return(this);
	    }

	    public WGLWindow cursor(Cursor cursor) {
		HCursor hc;
		if(cursor instanceof Cursor.Std) {
		    hc = stdcursors.getOrDefault(cursor, defcurs);
		} else {
		    hc = (HCursor)cursor;
		}
		msgrun(() -> {
		    win.SetCursor(hc.id);
		    this.cursor = hc.id;
		});
		return(this);
	    }

	    private Coord fixsize(Coord sz) {
		if(sz == null)
		    return(null);
		return(win.AdjustWindowRectEx(Area.sized(sz), Win32.WS_OVERLAPPEDWINDOW, false, 0).sz());
	    }

	    public Coord size() {
		return(size);
	    }

	    public State state() {
		return(curstate);
	    }

	    public boolean focused() {
		return(focused);
	    }

	    public Environment env() {
		if(renv == null) {
		    synchronized(this) {
			if(renv == null)
			    renv = glrun(this, () -> new WGLEnvironment());
		    }
		}
		return(renv);
	    }

	    public Visibility visible() {
		if(curstate == State.MINIMIZED)
		    return(Visibility.NONE);
		boolean any = false, all = true;
		for(boolean v : visible) {
		    any |= v;
		    all &= v;
		}
		return(all ? Visibility.FULL : any ? Visibility.PARTIAL : Visibility.NONE);
	    }

	    private int vispoint = 0;
	    private void checkvisibility() {
		int i = vispoint;
		vispoint = (vispoint + 1) % visgrid.length;
		Coord c = win.ClientToScreen(hwnd, visgrid[i].mul(size.x, size.y).round());
		visible[i] = win.WindowFromPoint(c).equals(hwnd);
	    }

	    private static final Pipe.Op glfb = Pipe.Op.compose(new FragColor<>(FragColor.defcolor),
								new DepthBuffer<>(DepthBuffer.defdepth));
	    public Pipe.Op fbstate() {
		return(glfb);
	    }

	    private void glswap(GL gl, int ival) {
		GLException.checkfor(gl, null);
		if(ival != cursi)
		    text.wglSwapIntervalEXT(cursi = ival);
		win.SwapBuffers(wgl.wglGetCurrentDC());
		GLException.checkfor(gl, null);
	    }

	    public void swapbuffers(Render g, Object mode) {
		checkvisibility();
		GLRender gbuf = (GLRender)g;
		if(((WGLEnvironment)gbuf.env).wnd() != this)
		    throw(new IllegalArgumentException());
		gbuf.submit(gl -> this.glswap(gl, ((Boolean)mode) ? 1 : 0));
	    }

	    public Clipboard clipboard(Object id) {
		if(id == Clipboard.Std.CLIPBOARD)
		    return(clipboard);
		return(Clipboard.nil);
	    }

	    public void dispose() {
		unregister(this);
		msgrun(() -> {
		    win.DestroyWindow(hwnd);
		    destroyed = true;
		});
	    }

	    public class W32Clipboard implements Clipboard {
		public static final Supplier<Integer> CF_PNG = Utils.cache(() -> Win32.get().RegisterClipboardFormat("PNG"));
		private Runnable expire = null;

		private static final double[] retries = {0.0, 0.01, 0.02, 0.05, 0.10, 0.25};
		private void tryopen(Promise<Object> res, int count) {
		    try {
			win.OpenClipboard(hwnd);
		    } catch(StdError err) {
			if(err.code != Win32.ERROR_ACCESS_DENIED) {
			    res.reject(err);
			    return;
			}
			if(count < retries.length) {
			    Timeout.later(Utils.rtime() + retries[0], () -> msgrun(() -> tryopen(res, count + 1)), null);
			} else {
			    res.reject(err);
			}
			return;
		    }
		    res.resolve(null);
		}

		public Promise<Object> openclipboard() {
		    Promise<Object> ret = new Promise<>();
		    msgrun(() -> tryopen(ret, 0));
		    return(ret);
		}

		public static class ClipboardData extends HGlobal {
		    public final int format;

		    public ClipboardData(int format, int bytes) {
			super(bytes);
			this.format = format;
		    }
		}

		private ClipboardData cvt_text(CharSequence t) {
		    ByteBuffer enc = win.C_WCHARSET().encode(CharBuffer.wrap(t));
		    ClipboardData ret = new ClipboardData(Win32.CF_UNICODETEXT, enc.remaining() + 2);
		    MemorySegment mem = win.GlobalLock(ret.hmem());
		    try {
			FUtils.memcpy(mem, enc, 0, enc.position(), enc.remaining());
		    } finally {
			win.GlobalUnlock(ret.hmem());
		    }
		    return(ret);
		}

		private ClipboardData cvt_dib(BufferedImage img) {
		    img = PUtils.coercergba(img, false);
		    Coord sz = PUtils.imgsz(img);
		    ClipboardData ret = new ClipboardData(Win32.CF_DIBV5, win.sizeof_BITMAPV5HEADER() + (4 * sz.x * sz.y));
		    MemorySegment mem = win.GlobalLock(ret.hmem());
		    try {
			BITMAPV5HEADER head = win.BITMAPV5HEADER(mem);
			head.bV5Size((int)head.sizeof());
			head.bV5Width(sz.x).bV5Height(sz.y);
			head.bV5Planes(1).bV5BitCount(32);
			head.bV5Compression(Win32.BI_BITFIELDS).bV5SizeImage(4 * sz.x * sz.y);
			head.bV5RedMask (0x00ff0000).bV5GreenMask(0x0000ff00);
			head.bV5BlueMask(0x000000ff).bV5AlphaMask(0xff000000);
			int off = (int)head.sizeof();
			Raster r = img.getRaster();
			for(int y = sz.y - 1; y >= 0; y--) {
			    for(int x = 0; x < sz.x; x++) {
				mem.set(ValueLayout.JAVA_BYTE, off++, (byte)r.getSample(x, y, 2));
				mem.set(ValueLayout.JAVA_BYTE, off++, (byte)r.getSample(x, y, 1));
				mem.set(ValueLayout.JAVA_BYTE, off++, (byte)r.getSample(x, y, 0));
				mem.set(ValueLayout.JAVA_BYTE, off++, (byte)r.getSample(x, y, 3));
			    }
			}
		    } finally {
			win.GlobalUnlock(ret.hmem());
		    }
		    return(ret);
		}

		private ClipboardData cvt_png(BufferedImage img) {
		    ByteArrayOutputStream buf = new ByteArrayOutputStream();
		    try {
			javax.imageio.ImageIO.write(img, "PNG", buf);
		    } catch(IOException e) {
			throw(new RuntimeException(e));
		    }
		    byte[] data = buf.toByteArray();
		    ClipboardData ret = new ClipboardData(CF_PNG.get(), data.length);
		    MemorySegment mem = win.GlobalLock(ret.hmem());
		    try {
			FUtils.memcpy(mem, data);
		    } finally {
			win.GlobalUnlock(ret.hmem());
		    }
		    return(ret);
		}

		private ClipboardData cvt_paths(Collection<Path> paths) {
		    StringBuilder buf = new StringBuilder();
		    for(Path p : paths) {
			buf.append(p.toString());
			buf.append('\0');
		    }
		    buf.append('\0');
		    ByteBuffer enc = win.C_WCHARSET().encode(CharBuffer.wrap(buf));
		    ClipboardData ret = new ClipboardData(Win32.CF_HDROP, win.sizeof_DROPFILES() + enc.remaining());
		    MemorySegment mem = win.GlobalLock(ret.hmem());
		    try {
			DROPFILES head = win.DROPFILES(mem);
			head.pFiles((int)head.sizeof()).fWide(true);
			FUtils.memcpy(mem, enc, head.sizeof(), enc.position(), enc.remaining());
		    } finally {
			win.GlobalUnlock(ret.hmem());
		    }
		    return(ret);
		}

		public void put(Contents c, Runnable expire) {
		    List<Promise<? extends ClipboardData>> items = new ArrayList<>();
		    for(Item<?> item : c.items) {
			if(item.fmt == Format.TEXT) {
			    items.add(item.check(Format.TEXT).get().then(val -> Promise.deferred(() -> cvt_text(val), t -> Defer.later(t, null))));
			} else if(item.fmt == Format.IMAGE) {
			    items.add(item.check(Format.IMAGE).get().then(val -> Promise.deferred(() -> cvt_dib(val), t -> Defer.later(t, null))));
			    items.add(item.check(Format.IMAGE).get().then(val -> Promise.deferred(() -> cvt_png(val), t -> Defer.later(t, null))));
			} else if(item.fmt == Format.PATHS) {
			    items.add(item.check(Format.PATHS).get().then(val -> Promise.deferred(() -> cvt_paths(val), t -> Defer.later(t, null))));
			}
		    }
		    Promise.collect(items).then(data -> openclipboard().then(_ -> Promise.deferred(() -> {
			try {
			    win.EmptyClipboard();
			    for(ClipboardData datum : data) {
				win.SetClipboardData(datum.format, datum.hmem());
				datum.relinquish();
			    }
			} finally {
			    win.CloseClipboard();
			}
			this.expire = expire;
			return(null);
		    }, WGLToolkit.this::msgrun)));
		}

		void lost() {
		    if(expire != null) {
			expire.run();
			expire = null;
		    }
		}

		private CharSequence mktext(Handle hmem, Charset charset) {
		    MemorySegment mem = win.GlobalLock(hmem);
		    try {
			return(mem.getString(0, charset));
		    } finally {
			win.GlobalUnlock(hmem);
		    }
		}

		private void mktext(List<Item<?>> buf, Handle hmem, Charset charset) {
		    buf.add(new Item<CharSequence>(Format.TEXT, mktext(hmem, charset)));
		}

		private int colorbyte(int rgb, int boff, int size) {
		    if(size == 8)
			return((rgb >>> boff) & 0xff);
		    int raw = (rgb >>> boff) & ((1 << size) - 1);
		    return((raw << (8 - size)) | (raw >>> (size - (8 - size))));
		}

		private BufferedImage paldibtoimg(Coord sz, ByteBuffer pixels, int off, int rstride, int pstride, int bits, int[] palette) {
		    WritableRaster buf = PUtils.imgraster(sz);
		    for(int y = 0, roff = off; y < sz.y; y++, roff += rstride) {
			for(int x = 0, poff = roff * 8; x < sz.x; x++, poff += pstride) {
			    int idx = (pixels.get(poff >> 3) & 0xff) >>> (poff & 7);
			    buf.setSample(x, y, 0, (palette[idx] >>> 16) & 0xff);
			    buf.setSample(x, y, 1, (palette[idx] >>>  8) & 0xff);
			    buf.setSample(x, y, 2, (palette[idx] >>>  0) & 0xff);
			    buf.setSample(x, y, 3, 0xff);
			}
		    }
		    return(PUtils.rasterimg(buf));
		}

		private BufferedImage rgbdibtoimg(Coord sz, ByteBuffer pixels, int off, int rstride, int pstride, int[] sizes, int[] boffs) {
		    WritableRaster buf = PUtils.imgraster(sz);
		    for(int y = 0, roff = off; y < sz.y; y++, roff += rstride) {
			for(int x = 0, poff = roff; x < sz.x; x++, poff += pstride) {
			    int rgb = pixels.getInt(poff);
			    for(int b = 0; b < sizes.length; b++)
				buf.setSample(x, y, b, colorbyte(rgb, boffs[b], sizes[b]));
			    if(sizes.length < 4)
				buf.setSample(x, y, 3, 0xff);
			}
		    }
		    return(PUtils.rasterimg(buf));
		}

		private Supplier<BufferedImage> mkdib(Handle hmem) {
		    MemorySegment mem = win.GlobalLock(hmem);
		    try {
			BITMAPV5HEADER bmp = win.BITMAPV5HEADER(mem);
			if(bmp.bV5Size() == bmp.sizeof()) {
			    Coord sz; boolean inv;
			    if(bmp.bV5Height() < 0) {
				sz = Coord.of(bmp.bV5Width(), -bmp.bV5Height());
				inv = false;
			    } else {
				sz = Coord.of(bmp.bV5Width(), bmp.bV5Height());
				inv = true;
			    }
			    int comp = bmp.bV5Compression();
			    int bits = bmp.bV5BitCount();
			    int palsize = bmp.bV5ClrUsed();
			    if(palsize == 0) {
				if(bits <= 8)
				    palsize = 1 << bits;
			    }
			    int off = bmp.bV5Size() + (palsize * 4);
			    int size = (int)(mem.byteSize() - off);
			    int[] palette = FUtils.memcpyi(mem, bmp.bV5Size(), palsize);
			    ByteBuffer pixels = ByteBuffer.allocate(size);
			    pixels.order(ByteOrder.LITTLE_ENDIAN);
			    MemorySegment.copy(mem, off, MemorySegment.ofBuffer(pixels), 0, size);
			    int rowsz = (((sz.x * bits) >> 3) + 3) & ~3;
			    switch(comp) {
			    case Win32.BI_RGB:
				switch(bits) {
				case 1:
				    if(inv)
					return(() -> paldibtoimg(sz, pixels, rowsz * (sz.y - 1), -rowsz, 1, 1, palette));
				    else
					return(() -> paldibtoimg(sz, pixels, 0, rowsz, 1, 1, palette));
				case 4:
				    if(inv)
					return(() -> paldibtoimg(sz, pixels, rowsz * (sz.y - 1), -rowsz, 4, 4, palette));
				    else
					return(() -> paldibtoimg(sz, pixels, 0, rowsz, 4, 4, palette));
				case 8:
				    if(inv)
					return(() -> paldibtoimg(sz, pixels, rowsz * (sz.y - 1), -rowsz, 8, 8, palette));
				    else
					return(() -> paldibtoimg(sz, pixels, 0, rowsz, 8, 8, palette));
				case 16:
				    if(inv)
					return(() -> rgbdibtoimg(sz, pixels, rowsz * (sz.y - 1), -rowsz, 2, new int[] {5, 5, 5}, new int[] {10, 5, 0}));
				    else
					return(() -> rgbdibtoimg(sz, pixels, 0, rowsz, 2, new int[] {5, 5, 5}, new int[] {10, 5, 0}));
				case 24:
				    if(inv)
					return(() -> rgbdibtoimg(sz, pixels, rowsz * (sz.y - 1), -rowsz, 3, new int[] {8, 8, 8}, new int[] {16, 8, 0}));
				    else
					return(() -> rgbdibtoimg(sz, pixels, 0, rowsz, 3, new int[] {8, 8, 8}, new int[] {16, 8, 0}));
				case 32:
				    if(inv)
					return(() -> rgbdibtoimg(sz, pixels, rowsz * (sz.y - 1), -rowsz, 4, new int[] {8, 8, 8}, new int[] {16, 8, 0}));
				    else
					return(() -> rgbdibtoimg(sz, pixels, 0, rowsz, 4, new int[] {8, 8, 8}, new int[] {16, 8, 0}));
				default:
				    return(() -> {throw(new RuntimeException("unexpected RGB DIB bit-count: " + bits));});
				}
			    case Win32.BI_BITFIELDS: {
				int[] masks = new int[] {bmp.bV5RedMask(), bmp.bV5GreenMask(), bmp.bV5BlueMask(), bmp.bV5AlphaMask()};
				if(masks[3] == 0)
				    masks = Arrays.copyOf(masks, 3);
				int[] sizes = new int[masks.length], boffs = new int[masks.length];
				for(int i = 0; i < masks.length; i++) {
				    boffs[i] = Integer.numberOfTrailingZeros(masks[i]);
				    sizes[i] = Integer.bitCount(masks[i]);
				}
				if(inv)
				    return(() -> rgbdibtoimg(sz, pixels, rowsz * (sz.y - 1), -rowsz, bits >> 3, sizes, boffs));
				else
				    return(() -> rgbdibtoimg(sz, pixels, 0, rowsz, bits >> 3, sizes, boffs));
			    }
			    default:
				return(() -> {throw(new RuntimeException("unexpected DIB compression type: " + comp));});
			    }
			} else {
			    return(() -> {throw(new RuntimeException("unexpected DIB header size: " + bmp.bV5Size()));});
			}
		    } finally {
			win.GlobalUnlock(hmem);
		    }
		}

		private void mkdib(List<Item<?>> buf, Handle hmem) {
		    buf.add(new Item<BufferedImage>(Format.IMAGE, () -> Promise.deferred(mkdib(hmem), t -> Defer.later(t, null))));
		}

		private Collection<Path> mkfiles(Handle hmem) {
		    MemorySegment mem = win.GlobalLock(hmem);
		    try {
			DROPFILES head = win.DROPFILES(mem);
			int off = head.pFiles();
			boolean wide = head.fWide();
			ByteBuffer raw = ByteBuffer.wrap(FUtils.memcpy(mem, off, (int)(mem.byteSize() - off)));
			Collection<String> names = new ArrayList<>();
			if(head.fWide()) {
			    for(int p = 0, o = 0; o < raw.capacity(); o += 2) {
				if(raw.getShort(o) == 0) {
				    if(o == p)
					break;
				    names.add(win.C_WCHARSET().decode(raw.duplicate().position(p).limit(o)).toString());
				    p = o + 2;
				}
			    }
			} else {
			    Charset charset = win.codepage(win.GetACP(), Win32.latin1);
			    for(int p = 0, o = 0; o < raw.capacity(); o++) {
				if(raw.get(o) == 0) {
				    if(o == p)
					break;
				    names.add(charset.decode(raw.duplicate().position(p).limit(o)).toString());
				    p = o + 1;
				}
			    }
			}
			Collection<Path> paths = new ArrayList<>();
			names.forEach(nm -> paths.add(Paths.get(nm)));
			return(paths);
		    } finally {
			win.GlobalUnlock(hmem);
		    }
		}

		private void mkfiles(List<Item<?>> buf, Handle hmem) {
		    buf.add(new Item<Collection<Path>>(Format.PATHS, () -> Promise.of(() -> mkfiles(hmem))));
		}

		private Contents mkcontents() {
		    try {
			List<Item<?>> ret = new ArrayList<>();
			if(win.IsClipboardFormatAvailable(Win32.CF_UNICODETEXT))
			    mktext(ret, win.GetClipboardData(Win32.CF_UNICODETEXT), win.C_WCHARSET());
			if(win.IsClipboardFormatAvailable(Win32.CF_DIBV5))
			    mkdib(ret, win.GetClipboardData(Win32.CF_DIBV5));
			if(win.IsClipboardFormatAvailable(Win32.CF_HDROP))
			    mkfiles(ret, win.GetClipboardData(Win32.CF_HDROP));
			return(new Contents(ret));
		    } finally {
			win.CloseClipboard();
		    }
		}

		public Promise<Contents> get() {
		    return(openclipboard().then(_ -> Promise.deferred(this::mkcontents, WGLToolkit.this::msgrun)));
		}
	    }

	    public class DropTarget implements IDropTarget {
		public final BMap<DropHandler.Action, Integer> actmap =
		    new HashBMap<>(Utils.<DropHandler.Action, Integer>map()
				   .put(DropHandler.Action.COPY, DROPEFFECT_COPY)
				   .put(DropHandler.Action.MOVE, DROPEFFECT_MOVE)
				   .put(DropHandler.Action.LINK, DROPEFFECT_LINK)
				   .map());
		public final DropHandler handler;
		private Clipboard.Contents current;

		public DropTarget(DropHandler handler) {
		    this.handler = handler;
		}

		private Promise<CharSequence> cvt_text(IDataObject data) {
		    return(Promise.deferred(() -> {
			COM.FORMATETC fmt = com.FORMATETC();
			fmt.dwAspect(COM.DVASPECT_CONTENT).lindex(-1).tymed(COM.TYMED_HGLOBAL);
			COM.STGMEDIUM stg = data.GetData(fmt.cfFormat(Win32.CF_UNICODETEXT));
			return(clipboard.mktext(((HGlobal)stg.get()).hmem(), win.C_WCHARSET()));
		    }, WGLToolkit.this::msgrun));
		}

		private Promise<BufferedImage> cvt_dib(IDataObject data) {
		    return(Promise.deferred(() -> {
			COM.FORMATETC fmt = com.FORMATETC();
			fmt.dwAspect(COM.DVASPECT_CONTENT).lindex(-1).tymed(COM.TYMED_HGLOBAL);
			COM.STGMEDIUM stg = data.GetData(fmt.cfFormat(Win32.CF_DIBV5));
			return(clipboard.mkdib(((HGlobal)stg.get()).hmem()));
		    }, WGLToolkit.this::msgrun).defer().map(Supplier::get));
		}

		private Promise<Collection<Path>> cvt_files(IDataObject data) {
		    return(Promise.deferred(() -> {
			COM.FORMATETC fmt = com.FORMATETC();
			fmt.dwAspect(COM.DVASPECT_CONTENT).lindex(-1).tymed(COM.TYMED_HGLOBAL);
			COM.STGMEDIUM stg = data.GetData(fmt.cfFormat(Win32.CF_HDROP));
			return(clipboard.mkfiles(((HGlobal)stg.get()).hmem()));
		    }, WGLToolkit.this::msgrun));
		}

		private boolean query(IDataObject data, int fmt) {
		    COM.FORMATETC etc = com.FORMATETC();
		    etc.dwAspect(COM.DVASPECT_CONTENT).lindex(-1).tymed(COM.TYMED_HGLOBAL);
		    try {
			return(data.QueryGetData(etc.cfFormat(fmt)) == Win32.S_OK);
		    } catch(HResultError e) {
			return(false);
		    }
		}

		private Clipboard.Contents mkcontents(IDataObject data) {
		    List<Clipboard.Item<?>> items = new ArrayList<>();
		    if(query(data, Win32.CF_UNICODETEXT))
			items.add(new Clipboard.Item<CharSequence>(Clipboard.Format.TEXT, () -> cvt_text(data)));
		    if(query(data, Win32.CF_DIBV5))
			items.add(new Clipboard.Item<BufferedImage>(Clipboard.Format.IMAGE, () -> cvt_dib(data)));
		    if(query(data, Win32.CF_HDROP))
			items.add(new Clipboard.Item<Collection<Path>>(Clipboard.Format.PATHS, () -> cvt_files(data)));
		    return(new Clipboard.Contents(items));
		}

		private int hover(Clipboard.Contents cont, Coord wndc) {
		    class Event implements DropHandler.DropHoverEvent {
			public Coord wndc() {return(wndc);}
			public Set<DropHandler.Action> actions() {return(EnumSet.allOf(DropHandler.Action.class));}
			public Clipboard.Contents contents() {return(cont);}
		    }
		    Integer ract = actmap.get(handler.drophover(new Event()));
		    return((ract == null) ? DROPEFFECT_NONE : ract);
		}

		public int DragEnter(IDataObject dataobj, int keystate, Coord pt) {
		    return(hover(current = mkcontents(dataobj), win.ScreenToClient(hwnd, pt)));
		}

		public int DragOver(int keystate, Coord pt) {
		    return(hover(current, win.ScreenToClient(hwnd, pt)));
		}

		public void DragLeave() {
		    current = null;
		}

		public int Drop(IDataObject dataobj, int keystate, Coord pt) {
		    Coord wndc = win.ScreenToClient(hwnd, pt);
		    Clipboard.Contents cont = mkcontents(dataobj);
		    class Event implements DropHandler.DroppedEvent {
			DropHandler.Action accepted = null;

			public Coord wndc() {return(wndc);}
			public Set<DropHandler.Action> actions() {return(EnumSet.allOf(DropHandler.Action.class));}
			public Clipboard.Contents contents() {return(cont);}

			public void accept(DropHandler.Action act) {
			    accepted = act;
			}
		    }
		    Event ev = new Event();
		    if(!handler.dropped(ev))
			throw(new HResultError(HResultError.DRAGDROP_S_CANCEL));
		    Integer ract = actmap.get(ev.accepted);
		    return((ract == null) ? DROPEFFECT_NONE : ract);
		}
	    }

	    public WGLWindow drophandler(DropHandler h) {
		msgrun(() -> {
		    if(h == null)
			com.RevokeDragDrop(hwnd);
		    else
			com.RegisterDragDrop(hwnd, new DropTarget(h));
		});
		return(this);
	    }

	    public class W32CharEvent implements KeyDownEvent {
		public String string;
		public Set<Key.Mod> mods;

		public W32CharEvent(char ch) {
		    this.string = Character.toString(ch);
		    mods = xlmods(kb);
		}

		public String string() {return(string);}
		public Key key() {return(Key.nil);}
		public Key.Sym sym() {return(null);}
		public Set<Key.Mod> mods() {return(mods);}

		public String toString() {
		    return(String.format("#<%s \"%s\">", getClass().getSimpleName(), Utils.strsafe(string)));
		}
	    }

	    public class W32KeyEvent {
		public int code, repeat, scancode;
		public boolean ext, ctx, pstate, tstate;
		public W32Key key;
		public Set<Key.Mod> mods;

		public W32KeyEvent(long wparam, long lparam) {
		    code     = (int)wparam;
		    repeat   = (int)((lparam & 0x0000ffff) >> 0);
		    scancode = (int)((lparam & 0x00ff0000) >> 16);
		    ext      =       (lparam & 0x01000000) != 0;
		    ctx      =       (lparam & 0x20000000) != 0;
		    pstate   =       (lparam & 0x40000000) != 0;
		    tstate   =       (lparam & 0x80000000) != 0;

		    win.GetKeyboardState(kb);
		    W32Key key = new W32Key(win.GetKeyboardLayout(0), code, scancode, ext);
		    this.key = curkeys.computeIfAbsent(key, Function.identity());
		    mods = xlmods(kb);
		}

		public String string() {return("");}
		public Key key() {return(key);}
		public Set<Key.Mod> mods() {return(mods);}

		public String toString() {
		    return(String.format("#<%s %s %s>", getClass().getSimpleName(), key, mods));
		}
	    }
	    public class W32KeyDownEvent extends W32KeyEvent implements KeyDownEvent {
		public final long keymsgid;
		public StringBuilder strbuf = new StringBuilder();
		public String string;
		public Key.Sym sym = null;

		public W32KeyDownEvent(long keymsgid, long wparam, long lparam) {
		    super(wparam, lparam);
		    this.keymsgid = keymsgid;
		}

		W32KeyDownEvent finish() {
		    string = strbuf.toString();
		    if(string.length() == 1) {
			for(Key.Sym sym : key.syms()) {
			    if((sym instanceof Key.Std) && (((Key.Std)sym).ch == Character.toUpperCase(string.charAt(0)))) {
				this.sym = sym;
				break;
			    } else if((sym instanceof CharSym) && (((CharSym)sym).ch == string.charAt(0))) {
				this.sym = sym;
				break;
			    }
			}
		    }
		    if(sym == null)
			sym = key.primary();
		    return(this);
		}

		public Key.Sym sym() {return(sym);}
		public String string() {return(string);}

		public String toString() {
		    return(String.format("#<%s %s %s sym=%s, str=\"%s\">", getClass().getSimpleName(), key, mods, sym, Utils.strsafe(string)));
		}
	    }
	    public class W32KeyUpEvent extends W32KeyEvent implements KeyUpEvent {
		public W32KeyUpEvent(long wparam, long lparam) {super(wparam, lparam);}
	    }

	    public class W32MouseEvent {
		public final Coord wndc;
		public final Set<MouseBtn> held = new HashSet<>();
		public final Set<Key.Mod> mods;

		public W32MouseEvent(long wparam, long lparam, boolean screen) {
		    Coord c = Coord.of((short)((lparam & 0x0000ffff) >> 0),
				       (short)((lparam & 0xffff0000) >> 16));
		    if(screen)
			wndc = win.ScreenToClient(hwnd, c);
		    else
			wndc = c;
		    if((wparam & Win32.MK_LBUTTON) != 0) held.add(MouseBtn.Std.LEFT);
		    if((wparam & Win32.MK_MBUTTON) != 0) held.add(MouseBtn.Std.MIDDLE);
		    if((wparam & Win32.MK_RBUTTON) != 0) held.add(MouseBtn.Std.RIGHT);
		    if((wparam & Win32.MK_XBUTTON1) != 0) held.add(MouseBtn.Std.BACK);
		    if((wparam & Win32.MK_XBUTTON2) != 0) held.add(MouseBtn.Std.FORWARD);
		    win.GetKeyboardState(kb);
		    mods = xlmods(kb);
		}

		public Coord wndc() {return(wndc);}
		public Set<MouseBtn> held() {return(held);}
		public Set<Key.Mod> mods() {return(mods);}
	    }
	    public class W32MouseMoveEvent extends W32MouseEvent implements MouseMoveEvent {
		public W32MouseMoveEvent(long wparam, long lparam) {super(wparam, lparam, false);}
	    }
	    public class W32MouseButtonEvent extends W32MouseEvent {
		public final MouseBtn btn;

		public W32MouseButtonEvent(MouseBtn btn, long wparam, long lparam) {
		    super(wparam, lparam, false);
		    this.btn = btn;
		}

		public MouseBtn button() {return(btn);}
	    }
	    public class W32MouseDownEvent extends W32MouseButtonEvent implements MouseDownEvent {
		public W32MouseDownEvent(MouseBtn btn, long wparam, long lparam) {super(btn, wparam, lparam);}
	    }
	    public class W32MouseUpEvent extends W32MouseButtonEvent implements MouseUpEvent {
		public W32MouseUpEvent(MouseBtn btn, long wparam, long lparam) {super(btn, wparam, lparam);}
	    }

	    public class W32MouseWheelEvent extends W32MouseEvent implements MouseWheelEvent {
		public final Axis axis;
		public final int amount;
		public final double sub;

		public W32MouseWheelEvent(Axis axis, int amount, double sub, long wparam, long lparam) {
		    super(wparam, lparam, true);
		    this.axis = axis;
		    this.amount = amount;
		    this.sub = sub;
		}

		public Axis axis() {return(axis);}
		public int amount() {return(amount);}
		public double subamount() {return(sub);}
	    }
	}

	public Windeye window() {
	    return(msgrun(WGLWindow::new));
	}

	public class ShellPicker implements FilePicker.Factory {
	    public class Picker implements FilePicker {
		private Mode mode;
		private Handle parent;
		private String[][] filter = {
		    {"All files", "*.*"}
		};

		public Picker(Mode mode, WGLWindow parent) {
		    this.mode = mode;
		    this.parent = (parent == null) ? Handle.nil : parent.hwnd;
		}

		public void filter(String desc, String... exts) {
		    StringBuilder spec = new StringBuilder();
		    for(String ext : exts) {
			if(spec.length() > 0)
			    spec.append(';');
			spec.append("*." + ext);
		    }
		    filter = new String[][] {
			{desc, spec.toString()},
			{"All files", "*.*"}
		    };
		}

		public Promise<Path> show() {
		    Promise<Path> ret = new Promise<>();
		    Thread th = new HackThread(() -> {
			com.CoInitializeEx(COM.COINIT_APARTMENTTHREADED | COM.COINIT_DISABLE_OLE1DDE);
			boolean done = false;
			try {
			    IFileDialog dlg = com.CoCreateInstance(((mode == Mode.OPEN) ?
								    IFileDialog.CLSID_FileOpenDialog :
								    IFileDialog.CLSID_FileSaveDialog),
								   null, COM.CLSCTX_INPROC_SERVER, IFileDialog.IID);
			    dlg.SetFileTypes(filter);
			    try {
				dlg.Show(parent);
				ret.resolve(Paths.get(dlg.GetResult().GetDisplayName(IShellItem.SIGDN_FILESYSPATH)));
				done = true;
			    } catch(HResultError e) {
				if(e.hresult == HResultError.ERROR_CANCELLED)
				    ret.resolve(null);
				else
				    ret.reject(e);
				done = true;
			    }
			} catch(Throwable t) {
			    if(!done)
				ret.reject(t);
			} finally {
			    com.CoUninitialize();
			}
		    }, "File picker thread");
		    th.setDaemon(true);
		    th.start();
		    return(ret);
		}
	    }

	    public FilePicker make(FilePicker.Mode mode, Windeye parent) {
		return(new Picker(mode, (WGLWindow)parent));
	    }
	}

	public FilePicker.Factory picker() {
	    return(new ShellPicker());
	}

	private Handle makeicon(BufferedImage img, Coord hotspot) {
	    img = PUtils.coercergba(img, false);
	    Handle dc = null, dib = null, mask = null;
	    try {
		BITMAPV5HEADER bmp = win.BITMAPV5HEADER();
		bmp.bV5Width(img.getWidth()).bV5Height(-img.getHeight());
		bmp.bV5Planes(1).bV5BitCount(32).bV5Compression(Win32.BI_BITFIELDS);
		bmp.bV5RedMask(0x00ff0000).bV5GreenMask(0x0000ff00).bV5BlueMask(0x000000ff).bV5AlphaMask(0xff000000);
		dc = win.GetDC(null);
		ByteBuffer[] bitsp = {null};
		dib = win.CreateDIBSection(dc, bmp, Win32.DIB_RGB_COLORS, bitsp, null, 0);
		ByteBuffer bits = bitsp[0];
		bits.order(ByteOrder.nativeOrder());
		Raster idat = img.getRaster();
		for(int y = 0, o = 0; y < img.getHeight(); y++) {
		    for(int x = 0; x < img.getWidth(); x++, o += 4) {
			bits.putInt(o, (idat.getSample(x, y, 0) << 16) | (idat.getSample(x, y, 1) <<  8) |
				       (idat.getSample(x, y, 2) <<  0) | (idat.getSample(x, y, 3) << 24));
		    }
		}
		mask = win.CreateBitmap(img.getWidth(), img.getHeight(), 1, 1, null);
		ICONINFO info = win.ICONINFO();
		if(hotspot == null) {
		    info.fIcon(1);
		} else {
		    info.fIcon(0);
		    info.xHotspot(hotspot.x).yHotspot(hotspot.y);
		}
		info.hbmMask(mask).hbmColor(dib);
		return(win.CreateIconIndirect(info));
	    } finally {
		if(mask != null)
		    win.DeleteObject(mask);
		if(dib != null)
		    win.DeleteObject(dib);
		if(dc != null)
		    win.ReleaseDC(null, dc);
	    }
	}

	public void dispose() {
	    synchronized(mtmon) {
		if(msgthread != null)
		    msgrun(() -> {msgrunning = false;});
	    }
	    wgl.wglDeleteContext(ctx);
	    win.UnregisterClass(wclassname, hInstance);
	}

	public void browse(java.net.URI location) throws IOException {
	    SHELLEXECUTEINFO info = win.SHELLEXECUTEINFO();
	    info.lpVerb("open");
	    info.lpFile(location.toString());
	    try {
		win.ShellExecuteEx(info);
	    } catch(StdError e) {
		throw(new IOException("Browser execution failed: " + e.getMessage()));
	    }
	}

	public String description() {
	    return("Win32/WGL");
	}

	public Set<Key.Mod> xlmods(KeyboardState kb) {
	    Set<Key.Mod> ret = EnumSet.noneOf(Key.Mod.class);
	    if((kb.get(0x10) & 0xf0) != 0)
		ret.add(Key.Mod.SHIFT);
	    if((kb.get(0x11) & 0xf0) != 0)
		ret.add(Key.Mod.CONTROL);
	    if((kb.get(0x12) & 0xf0) != 0)
		ret.add(Key.Mod.ALT);
	    if((kb.get(0xa5) & 0xf0) != 0)
		ret.add(Key.Mod.ALTGR);
	    if(((kb.get(0x5b) & 0xf0) != 0) || ((kb.get(0x5c) & 0xf0) != 0))
		ret.add(Key.Mod.SUPER);
	    return(ret);
	}
    }

    public static final Map<Integer, Key.Std> stdvsyms = Utils.<Integer, Key.Std>map()
	.put(0x0d, ENTER)         .put(0x08, BACKSPACE)     .put(0x09, TAB)
	.put(0x03, CANCEL)        .put(0x0c, CLEAR)         .put(0x10, SHIFT)
	.put(0x11, CONTROL)       .put(0x12, ALT)           .put(0x13, PAUSE)
	.put(0x14, CAPSLOCK)      .put(0x1b, ESCAPE)        .put(0x20, SPACE)
	.put(0x21, PAGEUP)        .put(0x22, PAGEDOWN)      .put(0x23, END)
	.put(0x24, HOME)          .put(0x25, LEFT)          .put(0x26, UP)
	.put(0x27, RIGHT)         .put(0x28, DOWN)
	.put(0x2e, DELETE)
	.put(0x90, NUMLOCK)       .put(0x91, SCROLLLOCK)    .put(0x2c, PRINTSCREEN)
	.put(0x2d, INSERT)        .put(0x2f, HELP)

	.put(0x5b, WINDOWS)       .put(0x5c, WINDOWS)       .put(0x5d, MENU)
	.put(0x18, FINAL)         .put(0x1c, CONVERT)       .put(0x1d, NONCONVERT)
	.put(0x1e, ACCEPT)        .put(0x1f, MODECHANGE)    .put(0x15, KANA)
	.put(0x19, KANJI)

	.put(0x30, N0)            .put(0x31, N1)            .put(0x32, N2)
	.put(0x33, N3)            .put(0x34, N4)            .put(0x35, N5)
	.put(0x36, N6)            .put(0x37, N7)            .put(0x38, N8)
	.put(0x39, N9)

	.put(0x41, A)             .put(0x42, B)             .put(0x43, C)
	.put(0x44, D)             .put(0x45, E)             .put(0x46, F)
	.put(0x47, G)             .put(0x48, H)             .put(0x49, I)
	.put(0x4a, J)             .put(0x4b, K)             .put(0x4c, L)
	.put(0x4d, M)             .put(0x4e, N)             .put(0x4f, O)
	.put(0x50, P)             .put(0x51, Q)             .put(0x52, R)
	.put(0x53, S)             .put(0x54, T)             .put(0x55, U)
	.put(0x56, V)             .put(0x57, W)             .put(0x58, X)
	.put(0x59, Y)             .put(0x5a, Z)

	.put(0x70, F1)            .put(0x71, F2)            .put(0x72, F3)
	.put(0x73, F4)            .put(0x74, F5)            .put(0x75, F6)
	.put(0x76, F7)            .put(0x77, F8)            .put(0x78, F9)
	.put(0x79, F10)           .put(0x7a, F11)           .put(0x7b, F12)
	.put(0x7c, F13)           .put(0x7d, F14)           .put(0x7e, F15)
	.put(0x7f, F16)           .put(0x80, F17)           .put(0x81, F18)
	.put(0x82, F19)           .put(0x83, F20)           .put(0x84, F21)
	.put(0x85, F22)           .put(0x86, F23)           .put(0x87, F24)

	.put(0x60, NP0)           .put(0x61, NP1)           .put(0x62, NP2)
	.put(0x63, NP3)           .put(0x64, NP4)           .put(0x65, NP5)
	.put(0x66, NP6)           .put(0x67, NP7)           .put(0x68, NP8)
	.put(0x69, NP9)           .put(0x6f, NP_DIV)        .put(0x6a, NP_MUL)
	.put(0x6d, NP_SUB)        .put(0x6b, NP_ADD)        .put(0x6c, NP_SEP)
	.put(0x6e, NP_DEC)
	.map();

    private static Map<Character, Key.Std> stdcsyms() {
	Map<Character, Key.Std> ret = new HashMap<>();
	for(Key.Std sym : Key.Std.values()) {
	    if((sym.ch != 0) && !ret.containsKey(sym.ch))
		ret.put(sym.ch, sym);
	}
	return(ret);
    }

    public static final Map<Character, Key.Std> stdcsyms = stdcsyms();

    public static final Map<Integer, Key.Loc.Std> stdkeys = Utils.<Integer, Key.Loc.Std>map()
	.put(  1, Key.Loc.Std.ESC ).put( 59, Key.Loc.Std.FK01).put( 60, Key.Loc.Std.FK02).put( 61, Key.Loc.Std.FK03).put( 62, Key.Loc.Std.FK04)
	.put( 63, Key.Loc.Std.FK05).put( 64, Key.Loc.Std.FK06).put( 65, Key.Loc.Std.FK07).put( 66, Key.Loc.Std.FK08).put( 67, Key.Loc.Std.FK09)
	.put( 68, Key.Loc.Std.FK10).put( 87, Key.Loc.Std.FK11).put( 88, Key.Loc.Std.FK12).put( 84, Key.Loc.Std.PRSC).put( 70, Key.Loc.Std.SCLK)
	.put( 69, Key.Loc.Std.PAUS).put(570, Key.Loc.Std.PAUS)

	.put( 41, Key.Loc.Std.TLDE).put(  2, Key.Loc.Std.AE01).put(  3, Key.Loc.Std.AE02).put(  4, Key.Loc.Std.AE03).put(  5, Key.Loc.Std.AE04)
	.put(  6, Key.Loc.Std.AE05).put(  7, Key.Loc.Std.AE06).put(  8, Key.Loc.Std.AE07).put(  9, Key.Loc.Std.AE08).put( 10, Key.Loc.Std.AE09)
	.put( 11, Key.Loc.Std.AE10).put( 12, Key.Loc.Std.AE11).put( 13, Key.Loc.Std.AE12).put( 14, Key.Loc.Std.BKSP).put(582, Key.Loc.Std.INS )
	.put(571, Key.Loc.Std.HOME).put(573, Key.Loc.Std.PGUP).put(569, Key.Loc.Std.NMLK).put(553, Key.Loc.Std.KPDV).put( 55, Key.Loc.Std.KPMU)
	.put( 74, Key.Loc.Std.KPSU)

	.put( 15, Key.Loc.Std.TAB ).put( 16, Key.Loc.Std.AD01).put( 17, Key.Loc.Std.AD02).put( 18, Key.Loc.Std.AD03).put( 19, Key.Loc.Std.AD04)
	.put( 20, Key.Loc.Std.AD05).put( 21, Key.Loc.Std.AD06).put( 22, Key.Loc.Std.AD07).put( 23, Key.Loc.Std.AD08).put( 24, Key.Loc.Std.AD09)
	.put( 25, Key.Loc.Std.AD10).put( 26, Key.Loc.Std.AD11).put( 27, Key.Loc.Std.AD12).put(583, Key.Loc.Std.DEL ).put(579, Key.Loc.Std.END )
	.put(581, Key.Loc.Std.PGDN).put( 71, Key.Loc.Std.KP7 ).put( 72, Key.Loc.Std.KP8 ).put( 73, Key.Loc.Std.KP9 ).put( 78, Key.Loc.Std.KPAD)

	.put( 58, Key.Loc.Std.CAPS).put( 30, Key.Loc.Std.AC01).put( 31, Key.Loc.Std.AC02).put( 32, Key.Loc.Std.AC03).put( 33, Key.Loc.Std.AC04)
	.put( 34, Key.Loc.Std.AC05).put( 35, Key.Loc.Std.AC06).put( 36, Key.Loc.Std.AC07).put( 37, Key.Loc.Std.AC08).put( 38, Key.Loc.Std.AC09)
	.put( 39, Key.Loc.Std.AC10).put( 40, Key.Loc.Std.AC11).put( 43, Key.Loc.Std.BKSL).put( 28, Key.Loc.Std.RTRN).put( 75, Key.Loc.Std.KP4 )
	.put( 76, Key.Loc.Std.KP5 ).put( 77, Key.Loc.Std.KP6 )

	.put( 42, Key.Loc.Std.LFSH).put( 86, Key.Loc.Std.LSGT).put( 44, Key.Loc.Std.AB01).put( 45, Key.Loc.Std.AB02).put( 46, Key.Loc.Std.AB03)
	.put( 47, Key.Loc.Std.AB04).put( 48, Key.Loc.Std.AB05).put( 49, Key.Loc.Std.AB06).put( 50, Key.Loc.Std.AB07).put( 51, Key.Loc.Std.AB08)
	.put( 52, Key.Loc.Std.AB09).put( 53, Key.Loc.Std.AB10).put( 54, Key.Loc.Std.RTSH).put(572, Key.Loc.Std.UP  ).put( 79, Key.Loc.Std.KP1 )
	.put( 80, Key.Loc.Std.KP2 ).put( 81, Key.Loc.Std.KP3 ).put(528, Key.Loc.Std.KPEN)

	.put( 29, Key.Loc.Std.LCTL).put(591, Key.Loc.Std.LWIN).put( 56, Key.Loc.Std.LALT).put( 57, Key.Loc.Std.SPCE).put(556, Key.Loc.Std.RALT)
	.put(592, Key.Loc.Std.RWIN).put(593, Key.Loc.Std.MENU).put(529, Key.Loc.Std.RCTL).put(575, Key.Loc.Std.LEFT).put(580, Key.Loc.Std.DOWN)
	.put(577, Key.Loc.Std.RGHT).put( 82, Key.Loc.Std.KP0 ).put( 83, Key.Loc.Std.KPDL)
	.map();
}
