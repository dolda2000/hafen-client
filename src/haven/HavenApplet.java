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

import java.applet.*;
import java.awt.Graphics;
import java.net.URL;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class HavenApplet extends Applet {
    public static Map<ThreadGroup, HavenApplet> applets = new HashMap<ThreadGroup, HavenApplet>();
    ThreadGroup p;
    JOGLPanel h;
    boolean running = false;
    static boolean initedonce = false;
    
    private class ErrorPanel extends Canvas implements haven.error.ErrorStatus {
	String status = "";
	boolean ar = false;
	
	public ErrorPanel() {
	    setBackground(Color.BLACK);
	    addMouseListener(new MouseAdapter() {
		    public void mouseClicked(MouseEvent e) {
			if(ar && !running) {
			    HavenApplet.this.remove(ErrorPanel.this);
			    startgame();
			}
		    }
		});
	}
	
	public boolean goterror(Throwable t) {
	    stopgame();
	    setSize(HavenApplet.this.getSize());
	    HavenApplet.this.add(this);
	    repaint();
	    return(true);
	}
	
	public void connecting() {
	    status = "Connecting to error report server...";
	    repaint();
	}
	
	public void sending() {
	    status = "Sending error report...";
	    repaint();
	}
	
	public void done(String ctype, String info) {
	    status = "Done";
	    ar = true;
	    repaint();
	}
	
	public void senderror(Exception e) {
	    status = "Could not send error report";
	    ar = true;
	    repaint();
	}
	
	public void paint(Graphics g) {
	    g.setColor(getBackground());
	    g.fillRect(0, 0, getWidth(), getHeight());
	    g.setColor(Color.WHITE);
	    FontMetrics m = g.getFontMetrics();
	    int y = 0;
	    g.drawString("An error has occurred.", 0, y + m.getAscent());
	    y += m.getHeight();
	    g.drawString(status, 0, y + m.getAscent());
	    y += m.getHeight();
	    if(ar) {
		g.drawString("Click to restart the game", 0, y + m.getAscent());
		y += m.getHeight();
	    }
	}
    }
    
    private void initonce() {
	if(initedonce)
	    return;
	initedonce = true;
	try {
	    Resource.addurl(new URL("https", getCodeBase().getHost(), 443, "/res/"));
	} catch(java.net.MalformedURLException e) {
	    throw(new RuntimeException(e));
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
    
    public void destroy() {
	stopgame();
    }
    
    public void startgame() {
	if(running)
	    return;
	h = new JOGLPanel(new Coord(800, 600));
	add(h);
	try {
	    p = new haven.error.ErrorHandler(new ErrorPanel(), new URL("http", getCodeBase().getHost(), 80, "/java/error"));
	} catch(java.net.MalformedURLException e) {
	    p = new ThreadGroup("Haven client");
	}
	synchronized(applets) {
	    applets.put(p, this);
	}
	Thread main = new HackThread(p, new Runnable() {
		public void run() {
		    Thread ui = new HackThread(h, "Haven UI thread");
		    ui.start();
		    try {
			UI.Runner fun = null;
			while(true) {
			    if(fun == null) {
				Bootstrap bill = new Bootstrap(getCodeBase().getHost(), Config.mainport);
				if((getParameter("username") != null) && (getParameter("authcookie") != null))
				    bill.setinitcookie(getParameter("username"), Utils.hex2byte(getParameter("authcookie")));
				fun = bill;
			    }
			    fun = fun.run(h.newui(fun));
			}
		    } catch(InterruptedException e) {
		    } finally {
			ui.interrupt();
		    }
		}
	    }, "Haven main thread");
	main.start();
	running = true;
    }
    
    public void stopgame() {
	if(!running)
	    return;
	running = false;
	synchronized(applets) {
	    applets.remove(p);
	}
	p.interrupt();
	remove(h);
	p = null;
	h = null;
    }
    
    public void init() {
	initonce();
	resize(800, 600);
	startgame();
    }
    
    static {
	WebBrowser.self = new WebBrowser() {
		public void show(URL url) {
		    HavenApplet a;
		    synchronized(applets) {
			a = applets.get(HackThread.tg());
		    }
		    if(a != null)
			a.getAppletContext().showDocument(url, "_blank");
		}
	    };
    }
}
