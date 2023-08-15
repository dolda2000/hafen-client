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

import java.io.PrintWriter;

public class Chatwindow extends Window {
    private static final PrintWriter stdout = new PrintWriter(System.out);
    public final TextEntry in;
    public final Textlog out;
    private boolean stdio;
    private Thread reader;
    private final StringBuilder readbuf = new StringBuilder();

    @RName("chat")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    return(new Chatwindow(UI.scale((Coord)args[0])));
	}
    }

    public Chatwindow(Coord sz) {
	super(sz, "Chat");
	in = adda(new TextEntry(sz.x, ""), 0, sz.y, 0.0, 1.0);
	in.canactivate = true;
	out = add(new Textlog(new Coord(sz.x, in.c.y)), Coord.z);
    }

    private void stdin() {
	System.out.print("> "); System.out.flush();
	while(true) {
	    synchronized(this) {
		if(!stdio) {
		    this.reader = null;
		    this.notifyAll();
		    return;
		}
	    }
	    try {
		int c = System.in.read();
		if(c == '\n') {
		    String cmd = readbuf.toString();
		    readbuf.setLength(0);
		    if((cmd.length() > 0) && (cmd.charAt(0) == ':')) {
			PrintWriter prev = ui.cons.out;
			try {
			    ui.cons.out = stdout;
			    ui.cons.run(cmd.substring(1));
			    stdout.flush();
			} catch(Exception e) {
			    String msg = e.getMessage();
			    if(msg == null)
				msg = e.toString();
			    System.out.println(msg);
			} finally {
			    ui.cons.out = prev;
			}
		    } else {
			wdgmsg("msg", cmd);
		    }
		    System.out.print("> "); System.out.flush();
		} else if(c > 0) {
		    readbuf.append((char)c);
		}
	    } catch(java.io.IOException e) {
		e.printStackTrace();
	    }
	}
    }

    public void stdio(boolean on) {
	synchronized(this) {
	    this.stdio = on;
	    if(stdio && (reader == null)) {
		reader = new HackThread(this::stdin, "stdin reader");
		reader.start();
	    } else if(!stdio && (reader != null)) {
		reader.interrupt();
		synchronized(this) {
		    try {
			while(reader != null)
			    this.wait();
		    } catch(InterruptedException e) {
			Thread.currentThread().interrupt();
		    }
		}
	    }
	}
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "log") {
	    if(stdio) {
		StringBuilder buf = new StringBuilder();
		buf.append("\r\033[K");
		buf.append((String)args[0]);
		buf.append("\n> ");
		System.out.print(buf); System.out.flush();
	    }
	    out.append((String)args[0]);
	} else if(msg == "show") {
	    super.uimsg(msg, args);
	    stdio(!visible);
	} else {
	    super.uimsg(msg, args);
	}
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
	if(sender == in) {
	    if(msg == "activate") {
		wdgmsg("msg", args[0]);
		in.settext("");
		return;
	    }
	}
	super.wdgmsg(sender, msg, args);
    }
}
