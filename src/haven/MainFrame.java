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
    final UIPanel p;
    private final ThreadGroup g;
    private Thread mt;
    DisplayMode fsmode = null, prefs = null;
	
    static {
	try {
	    javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
	} catch(Exception e) {}
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
	if(prefs != null)
	    return;
	prefs = dev.getDisplayMode();
	try {
	    setVisible(false);
	    dispose();
	    setUndecorated(true);
	    setVisible(true);
	    dev.setFullScreenWindow(this);
	    dev.setDisplayMode(fsmode);
	    pack();
	} catch(Exception e) {
	    throw(new RuntimeException(e));
	}
    }
	
    public void setwnd() {
	GraphicsDevice dev = getGraphicsConfiguration().getDevice();
	if(prefs == null)
	    return;
	try {
	    dev.setDisplayMode(prefs);
	    dev.setFullScreenWindow(null);
	    setVisible(false);
	    dispose();
	    setUndecorated(false);
	    setVisible(true);
	} catch(Exception e) {
	    throw(new RuntimeException(e));
	}
	prefs = null;
    }

    public boolean hasfs() {
	return(prefs != null);
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
		    if(args.length == 3) {
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
    }

    public MainFrame(Coord isz) {
	super("Haven and Hearth");
	Coord sz;
	if(isz == null) {
	    sz = Utils.getprefc("wndsz", new Coord(800, 600));
	    if(sz.x < 640) sz.x = 640;
	    if(sz.y < 480) sz.y = 480;
	} else {
	    sz = isz;
	}
	this.g = new ThreadGroup(HackThread.tg(), "Haven client");
	Component pp = (Component)(this.p = new JOGLPanel(sz));
	if(fsmode == null) {
	    Coord pfm = Utils.getprefc("fsmode", null);
	    if(pfm != null)
		fsmode = findmode(pfm.x, pfm.y);
	}
	if(fsmode == null) {
	    DisplayMode cm = getGraphicsConfiguration().getDevice().getDisplayMode();
	    fsmode = findmode(cm.getWidth(), cm.getHeight());
	}
	if(fsmode == null)
	    fsmode = findmode(800, 600);
	add(pp);
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
	if(prefs == null) {
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

    public static Session connect(Object[] args) {
	String username;
	byte[] cookie;
	if((Config.authuser != null) && (Config.authck != null)) {
	    username = Config.authuser;
	    cookie = Config.authck;
	} else {
	    if((username = Utils.getpref("tokenname@" + Config.defserv, null)) == null)
		throw(new RuntimeException("No explicit or saved username"));
	    String token = Utils.getpref("savedtoken@" + Config.defserv, null);
	    if(token == null)
		throw(new RuntimeException("No saved token"));
	    try {
		AuthClient cl = new AuthClient((Config.authserv == null) ? Config.defserv : Config.authserv, Config.authport);
		try {
		    if((username = cl.trytoken(username, Utils.hex2byte(token))) == null)
			throw(new RuntimeException("Authentication with saved token failed"));
		    cookie = cl.getcookie();
		} finally {
		    cl.close();
		}
	    } catch(IOException e) {
		throw(new RuntimeException(e));
	    }
	}
	Session sess;
	try {
	    sess = new Session(new java.net.InetSocketAddress(java.net.InetAddress.getByName(Config.defserv), Config.mainport), username, cookie, args);
	} catch(IOException e) {
	    throw(new RuntimeException(e));
	}
	boolean irq = false;
	try {
	    synchronized(sess) {
		while(sess.state != "") {
		    if(sess.connfailed != 0)
			throw(new RuntimeException(String.format("connection failure: %d", sess.connfailed)));
		    try {
			sess.wait();
		    } catch(InterruptedException e) {
			irq = true;
		    }
		}
	    }
	} finally {
	    if(irq)
		Thread.currentThread().interrupt();
	}
	return(sess);
    }

    private void uiloop() throws InterruptedException {
	UI.Runner fun = null;
	while(true) {
	    if(fun == null)
		fun = new Bootstrap();
	    String t = fun.title();
	    if(t == null)
		setTitle("Haven and Hearth");
	    else
		setTitle("Haven and Hearth \u2013 " + t);
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
    
    public static void setupres() {
	if(ResCache.global != null)
	    Resource.setcache(ResCache.global);
	if(Config.resurl != null)
	    Resource.addurl(Config.resurl);
	if(ResCache.global != null) {
	    /*
	    try {
		Resource.loadlist(Resource.remote(), ResCache.global.fetch("tmp/allused"), -10);
	    } catch(IOException e) {}
	    */
	}
	if(!Config.nopreload) {
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

    private static void main2(String[] args) {
	Config.cmdline(args);
	try {
	    javabughack();
	} catch(InterruptedException e) {
	    return;
	}
	setupres();
	UI.Runner fun = null;
	if(Config.servargs != null)
	    fun = new RemoteUI(connect(Config.servargs));
	MainFrame f = new MainFrame(null);
	if(Utils.getprefb("fullscreen", false))
	    f.setfs();
	f.run(fun);
	dumplist(Resource.remote().loadwaited(), Config.loadwaited);
	dumplist(Resource.remote().cached(), Config.allused);
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
	System.exit(0);
    }
    
    public static void main(final String[] args) {
	/* Set up the error handler as early as humanly possible. */
	ThreadGroup g = new ThreadGroup("Haven main group");
	String ed;
	if(!(ed = Utils.getprop("haven.errorurl", "")).equals("")) {
	    try {
		final haven.error.ErrorHandler hg = new haven.error.ErrorHandler(new java.net.URL(ed));
		hg.sethandler(new haven.error.ErrorGui(null) {
			public void errorsent() {
			    hg.interrupt();
			}
		    });
		g = hg;
		new DeadlockWatchdog(hg).start();
	    } catch(java.net.MalformedURLException e) {
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
