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

public class Console {
    private static final Map<String, Command> scommands = new HashMap<String, Command>();
    private final Map<String, Command> commands = new HashMap<String, Command>();
    private final Collection<Directory> dirs = new LinkedList<Directory>();
    private final ThreadLocal<Host> host = new ThreadLocal<>();
    public PrintWriter out;

    {
	clearout();
    }

    public static interface Command {
	public void run(Console cons, String[] args) throws Exception;
    }

    public static interface Directory {
	public Map<String, Command> findcmds();
    }

    public static interface Host {
    }

    public static void setscmd(String name, Command cmd) {
	synchronized(scommands) {
	    scommands.put(name, cmd);
	}
    }

    public void setcmd(String name, Command cmd) {
	synchronized(commands) {
	    commands.put(name, cmd);
	}
    }

    public Command findcmd(String name) {
	Command ret;
	synchronized(scommands) {
	    if((ret = scommands.get(name)) != null)
		return(ret);
	}
	synchronized(commands) {
	    if((ret = commands.get(name)) != null)
		return(ret);
	}
	synchronized(dirs) {
	    for(Directory dir : dirs) {
		if((ret = dir.findcmds().get(name)) != null)
		    return(ret);
	    }
	}
	return(null);
    }

    public void add(Directory dir) {
	synchronized(dirs) {
	    dirs.add(dir);
	}
    }

    public void run(Host host, String[] args) throws Exception {
	if(args.length < 1)
	    return;
	Command cmd = findcmd(args[0]);
	if(cmd == null)
	    throw(new Exception(args[0] + ": no such command"));
	Host ph = this.host.get();
	try {
	    this.host.set(host);
	    cmd.run(this, args);
	} finally {
	    this.host.set(ph);
	}
    }

    public void run(Host host, String cmdl) throws Exception {
	run(host, Utils.splitwords(cmdl));
    }

    public Host host() {
	return(host.get());
    }

    public void clearout() {
	out = new PrintWriter(new Writer() {
		public void write(char[] b, int o, int c) {}
		public void close() {}
		public void flush() {}
	    });
    }

    static {
	setscmd("die", (cons, args) -> {
	    throw(new Error("Triggered death"));
	});
	setscmd("sleep", (cons, args) -> {
	    long ms = (long)(Double.parseDouble(args[1]) * 1000);
	    try {
		Thread.sleep(ms);
	    } catch(InterruptedException e) {
		Thread.currentThread().interrupt();
		throw(new RuntimeException(e));
	    }
	});
	setscmd("lockdie", (cons, args) -> {
	    Object m1 = new Object(), m2 = new Object();
	    int[] sync = {0};
	    new HackThread(() -> {
		    try {
			synchronized(m2) {
			    synchronized(sync) {
				while(sync[0] != 1)
				    sync.wait();
				sync[0] = 2;
				sync.notifyAll();
			    }
			    synchronized(m1) {
				synchronized(sync) {
				    sync[0] = 3;
				    sync.notifyAll();
				}
			    }
			}
		    } catch(InterruptedException e) {}
	    }, "Deadlocker").start();
	    try {
		synchronized(m1) {
		    synchronized(sync) {
			sync[0] = 1;
			sync.notifyAll();
			while(sync[0] != 2)
			    sync.wait();
		    }
		    synchronized(m2) {
			synchronized(sync) {
			    sync[0] = 3;
			    sync.notifyAll();
			}
		    }
		}
	    } catch(InterruptedException e) {}
	});
	setscmd("threads", (cons, args) -> {
	    Utils.dumptg(null, cons.out);
	});
	setscmd("gc", (cons, args) -> {
	    System.gc();
	});
    }
}
