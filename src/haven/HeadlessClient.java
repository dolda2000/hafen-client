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

import java.util.*;
import java.io.*;
import haven.render.*;
import haven.iosys.tk.*;
import haven.iosys.audio.*;

public class HeadlessClient implements Console.Directory, Console.Host {
    private static final PrintWriter stdout = new PrintWriter(System.out);
    private static Coord size = Coord.of(1920,1080);
    private final Environment env;
    private Thread mt;
    private UILoop loop;

    public HeadlessClient() {
	env = Acephal.instance().env();
    }

    public class HeadlessLoop extends UILoop {
	private final FragColor<Texture.Image<Texture2D>> col;
	private final DepthBuffer<Texture.Image<Texture2D>> dpt;

	private HeadlessLoop() {
	    super(DummyToolkit.DummyWindow.of(size, HeadlessClient.this.env, null));
	    col = new FragColor<>(new Texture2D(size, DataBuffer.Usage.STATIC, new VectorFormat(4, NumberFormat.UNORM8), null).image(0));
	    dpt = new DepthBuffer<>(new Texture2D(size, DataBuffer.Usage.STATIC, Texture.DEPTH, new VectorFormat(1, NumberFormat.FLOAT32), null).image(0));
	}

	protected Pipe basestate() {
	    Pipe base = super.basestate();
	    base.prep(col).prep(dpt);
	    return(base);
	}

	public UI newui(UI.Runner fun) {
	    UI ui = super.newui(fun);
	    ui.cons.out = stdout;
	    ui.cons.add(HeadlessClient.this);
	    ui.grab(new Widget(), UI.NoticeEvent.class, ev -> {
		stdout.println(ev.msg.message());
		return(false);
	    });
	    return(ui);
	}

	protected AudioSystem.SinkLine audiosink() {
	    return(DummyAudio.DummySink.instance);
	}

	protected void drawcursor(UI ui, GOut g) {
	}

	protected void dispatch(UI ui) {
	}

	protected boolean bgmode() {
	    return(false);
	}
    }

    public void run(UI.Runner task) {
	if(mt != null) throw(new IllegalStateException());
	mt = Thread.currentThread();
	Thread stdio = new HackThread(this::stdin, "stdio reader");
	stdio.start();
	UILoop loop = this.loop = new HeadlessLoop();
	loop.start();
	try {
	    try {
		while(task != null)
		    task = task.run(loop.newui(task));
	    } catch(InterruptedException e) {
	    } finally {
		loop.newui(null);
	    }
	} finally {
	    loop.dispose();
	    stdio.interrupt();
	    this.loop = null;
	    mt = null;
	}
    }

    private void stdin() {
	StringBuilder readbuf = new StringBuilder();
	stdout.print("> "); stdout.flush();
	try {
	    while(true) {
		try {
		    while(System.in.available() == 0)
			Thread.sleep(100);
		    int c = System.in.read();
		    if(c == '\n') {
			UI ui = loop.ui;
			synchronized(ui) {
			    String cmd = readbuf.toString();
			    readbuf.setLength(0);
			    if((cmd.length() > 0) && (cmd.charAt(0) == ':')) {
				try {
				    loop.ui.cons.run(this, cmd.substring(1));
				    stdout.flush();
				} catch(Exception e) {
				    String msg = e.getMessage();
				    if(msg == null)
					msg = e.toString();
				    stdout.println(msg);
				}
			    } else {
				loop.ui.root.wdgmsg("cmd", cmd);
			    }
			}
			stdout.print("> "); stdout.flush();
		    } else if(c > 0) {
			readbuf.append((char)c);
		    }
		} catch(IOException e) {
		    e.printStackTrace();
		}
	    }
	} catch(InterruptedException e) {
	}
    }

    private Map<String, Console.Command> cmdmap = new TreeMap<String, Console.Command>();
    {
	cmdmap.put("q", (cons, args) -> {
	    mt.interrupt();
	});
    }
    public Map<String, Console.Command> findcmds() {
	return(cmdmap);
    }

    private static void usage(PrintStream out) {
	out.println("usage: HeadlessClient [OPTIONS] [SERVER[:PORT] [ARGS...]]");
	out.println("Options include:");
	out.println("  -h                 Display this help");
	out.println("  -s WxH             Specify output dimensions");
	out.println("  -u USER            Authenticate as USER (together with -C)");
	out.println("  -C HEXCOOKIE       Authenticate with specified hex-encoded cookie");
	out.println("  -p PREFSPEC        Use alternate preference prefix");
    }

    private static void parseargs(String[] args) {
	PosixArgs opt = PosixArgs.getopt(args, "hs:u:C:p:");
	if(opt == null) {
	    usage(System.err);
	    System.exit(1);
	}
	for(char c : opt.parsed()) {
	    switch(c) {
	    case 'h':
		usage(System.out);
		System.exit(0);
		break;
	    case 's':
		int x = opt.arg.indexOf('x');
		size = Coord.of(Integer.parseInt(opt.arg.substring(0, x)), Integer.parseInt(opt.arg.substring(x + 1)));
		break;
	    case 'u':
		Bootstrap.authuser.set(opt.arg);
		break;
	    case 'C':
		Bootstrap.authck.set(Utils.hex.dec(opt.arg));
		break;
	    case 'p':
		Utils.prefspec.set(opt.arg);
		break;
	    }
	}
	if(opt.rest.length > 0)
	    Bootstrap.authserv.set(NamedSocketAddress.parse(opt.rest[0], AuthClient.DEFPORT));
	if(opt.rest.length > 1)
	    Bootstrap.servargs.set(Utils.splice(opt.rest, 1));
    }

    public static void main2(String[] args) {
	Utils.initlocale();
	parseargs(args);
	Client.setupres();
	String[] servargs = Bootstrap.servargs.get();
	if(servargs == null)
	    servargs = new String[0];
	UI.Runner main;
	try {
	    main = new RemoteUI(Client.connect(servargs));
	} catch(Client.ConnectionError e) {
	    System.err.println("hafen: " + e.getMessage());
	    System.exit(1);
	    return;
	}
	new HeadlessClient().run(main);
    }

    public static void main(String[] args) {
	ThreadGroup g = new haven.error.SimpleHandler("Haven main group", true);
	Thread main = new HackThread(g, () -> main2(args), "Haven main thread");
	main.start();
    }
}
