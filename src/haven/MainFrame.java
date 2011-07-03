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
import java.util.*;

public class MainFrame extends Frame implements Runnable, FSMan {
    HavenPanel p;
    ThreadGroup g;
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

    public void togglefs() {
	if(prefs == null)
	    setfs();
	else
	    setwnd();
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

    public MainFrame(int w, int h) {
	super("Haven and Hearth");
	p = new HavenPanel(w, h);
	fsmode = findmode(w, h);
	add(p);
	pack();
	setResizable(false);
	p.requestFocus();
	seticon();
	setVisible(true);
	p.init();
    }
	
    public void run() {
	addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
		    g.interrupt();
		}
	    });
	Thread ui = new HackThread(p, "Haven UI thread");
	p.setfsm(this);
	ui.start();
	try {
	    while(true) {
		Bootstrap bill = new Bootstrap();
		if(Config.defserv != null)
		    bill.setaddr(Config.defserv);
		if((Config.authuser != null) && (Config.authck != null)) {
		    bill.setinitcookie(Config.authuser, Config.authck);
		    Config.authck = null;
		}
		Session sess = bill.run(p);
		RemoteUI rui = new RemoteUI(sess);
		rui.run(p.newui(sess));
	    }
	} catch(InterruptedException e) {
	} finally {
	    ui.interrupt();
	    dispose();
	}
    }
    
    public static void setupres() {
	if(ResCache.global != null)
	    Resource.addcache(ResCache.global);
	if(Config.resurl != null)
	    Resource.addurl(Config.resurl);
	if(ResCache.global != null) {
	    try {
		Resource.loadlist(ResCache.global.fetch("tmp/allused"), -10);
	    } catch(IOException e) {}
	}
	if(!Config.nopreload) {
	    try {
		InputStream pls;
		pls = Resource.class.getResourceAsStream("res-preload");
		if(pls != null)
		    Resource.loadlist(pls, -5);
		pls = Resource.class.getResourceAsStream("res-bgload");
		if(pls != null)
		    Resource.loadlist(pls, -10);
	    } catch(IOException e) {
		throw(new Error(e));
	    }
	}
    }
    
    static {
	WebBrowser.self = JnlpBrowser.create();
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
	ThreadGroup g = HackThread.tg();
	setupres();
	MainFrame f = new MainFrame(800, 600);
	if(Config.fullscreen)
	    f.setfs();
	f.g = g;
	if(g instanceof haven.error.ErrorHandler) {
	    final haven.error.ErrorHandler hg = (haven.error.ErrorHandler)g;
	    hg.sethandler(new haven.error.ErrorGui(null) {
		    public void errorsent() {
			hg.interrupt();
		    }
		});
	}
	f.run();
	dumplist(Resource.loadwaited, Config.loadwaited);
	dumplist(Resource.cached(), Config.allused);
	if(ResCache.global != null) {
	    try {
		Collection<Resource> used = new LinkedList<Resource>();
		for(Resource res : Resource.cached()) {
		    if(res.prio >= 0)
			used.add(res);
		}
		Writer w = new OutputStreamWriter(ResCache.global.store("tmp/allused"), "UTF-8");
		try {
		    Resource.dumplist(used, w);
		} finally {
		    w.close();
		}
	    } catch(IOException e) {}
	}
    }
    
    public static void main(final String[] args) {
	/* Set up the error handler as early as humanly possible. */
	ThreadGroup g = new ThreadGroup("Haven client");
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
	    } catch(java.net.MalformedURLException e) {
	    }
	}
	Thread main = new HackThread(g, new Runnable() {
		public void run() {
		    try {
			javabughack();
		    } catch(InterruptedException e) {
			return;
		    }
		    main2(args);
		}
	    }, "Haven main thread");
	main.start();
	try {
	    main.join();
	} catch(InterruptedException e) {
	    g.interrupt();
	    return;
	}
	System.exit(0);
    }
	
    private static void dumplist(Collection<Resource> list, String fn) {
	try {
	    if(fn != null) {
		Writer w = new OutputStreamWriter(new FileOutputStream(fn), "UTF-8");
		try {
		    Resource.dumplist(list, w);
		} finally {
		    w.close();
		}
	    }
	} catch(IOException e) {
	    throw(new RuntimeException(e));
	}
    }
}
