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

package haven;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.lang.reflect.*;

public class MainFrame extends java.awt.Frame implements Console.Directory {
    public static final Config.Variable<Boolean> initfullscreen = Config.Variable.propb("haven.fullscreen", false);
    public static final Config.Variable<String> renderer = Config.Variable.prop("haven.renderer", "jogl");
    public static final Config.Variable<Boolean> status = Config.Variable.propb("haven.status", false);
    final UIPanel p;
    private final ThreadGroup g;
    private Thread mt;
    boolean fullscreen;
    DisplayMode fsmode = null, prefs = null;
    Coord prefssz = null;
	
    public static void initawt() {
	try {
	    System.setProperty("apple.awt.application.name", "Haven & Hearth");
	    javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
	} catch(Exception e) {}
    }

    static {
	initawt();
    }
	
    DisplayMode findmode(int w, int h) {
	GraphicsDevice dev = getGraphicsConfiguration().getDevice();
	if(!dev.isFullScreenSupported())
	    return(null);
	DisplayMode b = null;
	for(DisplayMode m : dev.getDisplayModes()) {
	    int d = m.getBitDepth();
	    if((m.getWidth() == w) && (m.getHeight() == h) && ((d == 24) || (d == 32) || (d == DisplayMode.BIT_DEPTH_MULTI))) {
		if((b == null) || (d > b.getBitDepth()) || ((d == b.getBitDepth()) && (m.getRefreshRate() > b.getRefreshRate())))
		    b = m;
	    }
	}
	return(b);
    }
	
    public void setfs() {
	GraphicsDevice dev = getGraphicsConfiguration().getDevice();
	if(fullscreen)
	    return;
	fullscreen = true;
	prefssz = new Coord(getSize());
	try {
	    setVisible(false);
	    dispose();
	    setUndecorated(true);
	    setVisible(true);
	    dev.setFullScreenWindow(this);
	    if(fsmode != null) {
		prefs = dev.getDisplayMode();
		dev.setDisplayMode(fsmode);
	    }
	    pack();
	} catch(Exception e) {
	    throw(new RuntimeException(e));
	}
    }
	
    public void setwnd() {
	GraphicsDevice dev = getGraphicsConfiguration().getDevice();
	if(!fullscreen)
	    return;
	try {
	    if(prefs != null)
		dev.setDisplayMode(prefs);
	    dev.setFullScreenWindow(null);
	    setVisible(false);
	    dispose();
	    setUndecorated(false);
	    if(prefssz != null)
		setSize(prefssz.x, prefssz.y);
	    setVisible(true);
	} catch(Exception e) {
	    throw(new RuntimeException(e));
	}
	fullscreen = false;
    }

    private Map<String, Console.Command> cmdmap = new TreeMap<String, Console.Command>();
    {
	cmdmap.put("sz", new Console.Command() {
		public void run(Console cons, String[] args) {
		    if(args.length == 3) {
			int w = Integer.parseInt(args[1]),
			    h = Integer.parseInt(args[2]);
			p.setSize(w, h);
			pack();
			Utils.setprefc("wndsz", new Coord(w, h));
		    } else if(args.length == 2) {
			if(args[1].equals("dyn")) {
			    setResizable(true);
			    Utils.setprefb("wndlock", false);
			} else if(args[1].equals("lock")) {
			    setResizable(false);
			    Utils.setprefb("wndlock", true);
			}
		    }
		}
	    });
	cmdmap.put("fsmode", new Console.Command() {
		public void run(Console cons, String[] args) throws Exception {
		    if((args.length < 2) || args[1].equals("none")) {
			fsmode = null;
			Utils.setprefc("fsmode", Coord.z);
		    } else if(args.length == 3) {
			DisplayMode mode = findmode(Integer.parseInt(args[1]), Integer.parseInt(args[2]));
			if(mode == null)
			    throw(new Exception("No such mode is available"));
			fsmode = mode;
			Utils.setprefc("fsmode", new Coord(mode.getWidth(), mode.getHeight()));
		    }
		}
	    });
	cmdmap.put("fs", new Console.Command() {
		public void run(Console cons, String[] args) {
		    if(args.length >= 2) {
			if(Utils.atoi(args[1]) != 0)
			    getToolkit().getSystemEventQueue().invokeLater(MainFrame.this::setfs);
			else
			    getToolkit().getSystemEventQueue().invokeLater(MainFrame.this::setwnd);
		    }
		}
	    });
    }
    public Map<String, Console.Command> findcmds() {
	return(cmdmap);
    }

    private void seticon() {
	Image icon;
	try {
	    InputStream data = MainFrame.class.getResourceAsStream("icon.png");
	    icon = javax.imageio.ImageIO.read(data);
	    data.close();
	} catch(IOException e) {
	    throw(new Error(e));
	}
	setIconImage(icon);
	try {
	    Class<?> ctb = Class.forName("java.awt.Taskbar");
	    Object tb = ctb.getMethod("getTaskbar").invoke(null);
	    ctb.getMethod("setIconImage", Image.class).invoke(tb, icon);
	} catch(Exception e) {
	}
    }

    private UIPanel renderer() {
	String id = renderer.get();
	switch(id) {
	case "jogl":
	    return(new JOGLPanel());
	case "lwjgl":
	    return(new LWJGLPanel());
	default:
	    throw(new RuntimeException("invalid renderer specified in haven.renderer: " + id));
	}
    }

    public MainFrame(Coord isz) {
	super("Haven & Hearth");
	Coord sz;
	if(isz == null) {
	    sz = Utils.getprefc("wndsz", new Coord(800, 600));
	    if(sz.x < 640) sz.x = 640;
	    if(sz.y < 480) sz.y = 480;
	} else {
	    sz = isz;
	}
	this.g = new ThreadGroup(HackThread.tg(), "Haven client");
	Component pp = (Component)(this.p = renderer());
	if(fsmode == null) {
	    Coord pfm = Utils.getprefc("fsmode", null);
	    if((pfm != null) && !pfm.equals(Coord.z))
		fsmode = findmode(pfm.x, pfm.y);
	}
	add(pp);
	pp.setSize(sz.x, sz.y);
	pack();
	setResizable(!Utils.getprefb("wndlock", false));
	pp.requestFocus();
	seticon();
	setVisible(true);
	addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
		    mt.interrupt();
		}

		public void windowActivated(WindowEvent e) {
		    p.background(false);
		}

		public void windowDeactivated(WindowEvent e) {
		    p.background(true);
		}
	    });
	if((isz == null) && Utils.getprefb("wndmax", false))
	    setExtendedState(getExtendedState() | MAXIMIZED_BOTH);
    }
	
    private void savewndstate() {
	if(!fullscreen) {
	    if(getExtendedState() == NORMAL)
		/* Apparent, getSize attempts to return the "outer
		 * size" of the window, including WM decorations, even
		 * though setSize sets the "inner size" of the
		 * window. Therefore, use the Panel's size instead; it
		 * ought to correspond to the inner size at all
		 * times. */{
		Dimension dim = p.getSize();
		Utils.setprefc("wndsz", new Coord(dim.width, dim.height));
	    }
	    Utils.setprefb("wndmax", (getExtendedState() & MAXIMIZED_BOTH) != 0);
	}
    }

    public static class ConnectionError extends RuntimeException {
	public ConnectionError(String mesg) {
	    super(mesg);
	}
    }

    public static Session connect(Object[] args) {
	String username;
	byte[] cookie;
	if((Bootstrap.authuser.get() != null) && (Bootstrap.authck.get() != null)) {
	    username = Bootstrap.authuser.get();
	    cookie = Bootstrap.authck.get();
	} else {
	    if(Bootstrap.authuser.get() != null) {
		username = Bootstrap.authuser.get();
	    } else {
		if((username = Utils.getpref("tokenname@" + Bootstrap.defserv.get(), null)) == null)
		    throw(new ConnectionError("no explicit or saved username for host: " + Bootstrap.defserv.get()));
	    }
	    String token = Utils.getpref("savedtoken-" + username + "@" + Bootstrap.defserv.get(), null);
	    if(token == null)
		throw(new ConnectionError("no saved token for user: " + username));
	    try {
		AuthClient cl = new AuthClient((Bootstrap.authserv.get() == null) ? Bootstrap.defserv.get() : Bootstrap.authserv.get(), Bootstrap.authport.get());
		try {
		    try {
			username = new AuthClient.TokenCred(username, Utils.hex2byte(token)).tryauth(cl);
		    } catch(AuthClient.Credentials.AuthException e) {
			throw(new ConnectionError("authentication with saved token failed"));
		    }
		    cookie = cl.getcookie();
		} finally {
		    cl.close();
		}
	    } catch(IOException e) {
		throw(new RuntimeException(e));
	    }
	}
	try {
	    return(new Session(new java.net.InetSocketAddress(java.net.InetAddress.getByName(Bootstrap.defserv.get()), Bootstrap.mainport.get()), username, cookie, args));
	} catch(Connection.SessionError e) {
	    throw(new ConnectionError(e.getMessage()));
	} catch(InterruptedException exc) {
	    throw(new RuntimeException(exc));
	} catch(IOException e) {
	    throw(new RuntimeException(e));
	}
    }

    private void uiloop() throws InterruptedException {
	UI.Runner fun = null;
	while(true) {
	    if(fun == null)
		fun = new Bootstrap();
	    String t = fun.title();
	    if(t == null)
		setTitle("Haven & Hearth");
	    else
		setTitle("Haven & Hearth \u2013 " + t);
	    fun = fun.run(p.newui(fun));
	}
    }

    private void run(UI.Runner task) {
	synchronized(this) {
	    if(this.mt != null)
		throw(new RuntimeException("MainFrame is already running"));
	    this.mt = Thread.currentThread();
	}
	try {
	    Thread ui = new HackThread(p, "Haven UI thread");
	    ui.start();
	    try {
		try {
		    if(task == null) {
			uiloop();
		    } else {
			while(task != null)
			    task = task.run(p.newui(task));
		    }
		} catch(InterruptedException e) {
		} finally {
		    p.newui(null);
		}
		savewndstate();
	    } finally {
		ui.interrupt();
		try {
		    ui.join(5000);
		} catch(InterruptedException e) {}
		if(ui.isAlive())
		    Warning.warn("ui thread failed to terminate");
		dispose();
	    }
	} finally {
	    synchronized(this) {
		this.mt = null;
	    }
	}
    }
    
    public static final Config.Variable<Boolean> nopreload = Config.Variable.propb("haven.nopreload", false);
    public static void setupres() {
	if(ResCache.global != null)
	    Resource.setcache(ResCache.global);
	if(Resource.resurl.get() != null)
	    Resource.addurl(Resource.resurl.get());
	if(ResCache.global != null) {
	    /*
	    try {
		Resource.loadlist(Resource.remote(), ResCache.global.fetch("tmp/allused"), -10);
	    } catch(IOException e) {}
	    */
	}
	if(!nopreload.get()) {
	    try {
		InputStream pls;
		pls = Resource.class.getResourceAsStream("res-preload");
		if(pls != null)
		    Resource.loadlist(Resource.remote(), pls, -5);
		pls = Resource.class.getResourceAsStream("res-bgload");
		if(pls != null)
		    Resource.loadlist(Resource.remote(), pls, -10);
	    } catch(IOException e) {
		throw(new Error(e));
	    }
	}
    }

    public static final Config.Variable<Path> loadwaited = Config.Variable.propp("haven.loadwaited", "");
    public static final Config.Variable<Path> allused = Config.Variable.propp("haven.allused", "");
    public static void resdump() {
	dumplist(Resource.remote().loadwaited(), loadwaited.get());
	dumplist(Resource.remote().cached(), allused.get());
	if(ResCache.global != null) {
	    try {
		Writer w = new OutputStreamWriter(ResCache.global.store("tmp/allused"), "UTF-8");
		try {
		    Resource.dumplist(Resource.remote().used(), w);
		} finally {
		    w.close();
		}
	    } catch(IOException e) {}
	}
    }

    static {
	WebBrowser.self = DesktopBrowser.create();
    }

    private static void javabughack() throws InterruptedException {
	/* Work around a stupid deadlock bug in AWT. */
	try {
	    javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
		    public void run() {
			PrintStream bitbucket = new PrintStream(new ByteArrayOutputStream());
			bitbucket.print(LoginScreen.textf);
			bitbucket.print(LoginScreen.textfs);
		    }
		});
	} catch(java.lang.reflect.InvocationTargetException e) {
	    /* Oh, how I love Swing! */
	    throw(new Error(e));
	}
	/* Work around another deadl bug in Sun's JNLP client. */
	javax.imageio.spi.IIORegistry.getDefaultInstance();
    }

    public static void status(String state) {
	if(status.get()) {
	    System.out.println("hafen:status:" + state);
	    System.out.flush();
	}
    }

    private static void main2(String[] args) {
	Config.cmdline(args);
	status("start");
	try {
	    javabughack();
	} catch(InterruptedException e) {
	    return;
	}
	setupres();
	UI.Runner fun = null;
	if(Bootstrap.servargs.get() != null) {
	    try {
		fun = new RemoteUI(connect(Bootstrap.servargs.get()));
	    } catch(ConnectionError e) {
		System.err.println("hafen: " + e.getMessage());
		System.exit(1);
	    }
	}
	MainFrame f = new MainFrame(null);
	status("visible");
	if(initfullscreen.get())
	    f.setfs();
	f.run(fun);
	resdump();
	status("exit");
	System.exit(0);
    }
    
    public static void main(final String[] args) {
	/* Set up the error handler as early as humanly possible. */
	ThreadGroup g = new ThreadGroup("Haven main group");
	String ed = Utils.getprop("haven.errorurl", "");
	if(ed.equals("stderr")) {
	    g = new haven.error.SimpleHandler("Haven main group", true);
	} else if(!ed.equals("")) {
	    try {
		final haven.error.ErrorHandler hg = new haven.error.ErrorHandler(new java.net.URI(ed).toURL());
		hg.sethandler(new haven.error.ErrorGui(null) {
			public void errorsent() {
			    hg.interrupt();
			}
		    });
		g = hg;
		new DeadlockWatchdog(hg).start();
	    } catch(java.net.MalformedURLException | java.net.URISyntaxException e) {
	    }
	}
	Thread main = new HackThread(g, () -> main2(args), "Haven main thread");
	main.start();
    }
	
    private static void dumplist(Collection<Resource> list, Path fn) {
	try {
	    if(fn != null) {
		try(Writer w = Files.newBufferedWriter(fn, Utils.utf8)) {
		    Resource.dumplist(list, w);
		}
	    }
	} catch(IOException e) {
	    throw(new RuntimeException(e));
	}
    }
}
