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
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import haven.render.*;
import haven.render.Pipe;

public class StreamOut {
    public static final Config.Variable<Path> path = Config.Variable.propp("haven.streamout", "");
    public static final Config.Variable<Double> rate = Config.Variable.propf("haven.streamrate", null);
    public final WritableByteChannel out;
    public final Coord sz;
    private final Queue<ByteBuffer> free = new LinkedList<>();
    private ByteBuffer obuf = null;
    private Thread ot = null;
    private boolean running = true;

    public StreamOut(Coord sz, WritableByteChannel out) {
	this.sz = sz;
	this.out = out;
    }

    public StreamOut(Coord sz, Path out) throws IOException {
	this(sz, Files.newByteChannel(out, StandardOpenOption.WRITE));
    }

    private void writeframe(ByteBuffer data) {
	try {
	    for(int y = sz.y - 1; y >= 0; y--) {
		data.limit((y + 1) * sz.x * 3).position(y * sz.x * 3);
		while(data.remaining() > 0)
		    out.write(data);
	    }
	} catch(IOException e) {
	    running = false;
	    throw(new RuntimeException(e));
	}
    }

    private void uoutput() {
	double last = Utils.rtime();
	main: while(running) {
	    ByteBuffer data;
	    synchronized(this) {
		while(obuf == null) {
		    try {
			double now = Utils.rtime();
			if(now - last > 5) {
			    ot = null;
			    return;
			}
			this.wait((int)(1000 * (last + 5 - now)));
		    } catch(InterruptedException e) {
			continue main;
		    }
		}
		data = obuf;
		obuf = null;
	    }
	    writeframe(data);
	    synchronized(this) {
		data.clear();
		free.add(data);
	    }
	    last = Utils.rtime();
	}
    }

    private void routput(double rate) {
	double dur = 1.0 / rate;
	double last = Utils.rtime();
	ByteBuffer data = null;
	main: while(running) {
	    synchronized(this) {
		if(obuf != null) {
		    if(data != null) {
			data.clear();
			free.add(data);
		    }
		    data = obuf;
		    obuf = null;
		}
	    }
	    while(true) {
		double now = Utils.rtime();
		if(now > last + 5) {
		    Warning.warn("streamout frame timing reset");
		    last = now;
		    break;
		} else if(now > last + dur) {
		    last += dur;
		    break;
		}
		try {
		    long nanos = (long)((last + dur - now) * 1e9);
		    Thread.sleep(nanos / 1000000, (int)(nanos % 1000000));
		} catch(InterruptedException e) {
		    continue main;
		}
	    }
	    writeframe(data);
	}
    }

    private void output() {
	try {
	    if(rate.get() != null)
		routput(rate.get());
	    else
		uoutput();
	    try {
		out.close();
	    } catch(IOException e) {
		new Warning(e, "could not close stream-out channel").issue();
	    }
	} finally {
	    synchronized(this) {
		if(running && (obuf != null) && (ot != null)) {
		    ot = new HackThread(this::output, "Stream-out");
		    ot.start();
		}
	    }
	    Utils.defer(() -> System.exit(127));
	}
    }

    private void fin(ByteBuffer buf) {
	synchronized(this) {
	    if(obuf != null)
		free.add(obuf);
	    obuf = buf;
	    this.notifyAll();
	    if(ot == null) {
		ot = new HackThread(this::output, "Stream-out");
		ot.start();
	    }
	}
    }

    public void accept(Render g, Pipe state) {
	if(!running)
	    return;
	ByteBuffer buf;
	synchronized(this) {
	    if(free.isEmpty())
		buf = Utils.mkbbuf(sz.x * sz.y * 3);
	    else
		buf = free.remove();
	}
	g.pget(state, FragColor.fragcol, Area.sized(sz), new VectorFormat(3, NumberFormat.UNORM8), buf, this::fin);
    }

    public void close() {
	synchronized(this) {
	    running = false;
	    if(ot == null) {
		try {
		    out.close();
		} catch(IOException e) {
		    new Warning(e, "could not close stream-out channel").issue();
		}
	    } else {
		ot.interrupt();
	    }
	}
    }
}
