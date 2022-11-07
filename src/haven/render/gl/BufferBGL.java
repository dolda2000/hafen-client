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

package haven.render.gl;

import haven.Utils;
import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.nio.file.*;
import com.jogamp.opengl.*;

public class BufferBGL extends BGL {
    public static final BufferBGL empty = new BufferBGL(0) {
	    protected void add(Command cmd) {
		throw(new RuntimeException());
	    }
	};
    private static Profile curprof = null;
    private Command[] list;
    private int n = 0;

    public BufferBGL(int c) {
	list = new Command[Math.max(c, 1)];
    }
    public BufferBGL() {this(128);}

    public void run(GL3 gl) {
	for(int i = 0; i < n; i++) {
	    if(curprof != null)
		curprof.register(list[i]);
	    try {
		try {
		    list[i].run(gl);
		} catch(com.jogamp.opengl.GLException exc) {
		    /* How nice wouldn't it be if DebugGL could be
		     * subclasseed to customize the errors. */
		    checkdebuggl(exc);
		    throw(exc);
		}
	    } catch(Exception exc) {
		throw(new BGLException(this, list[i], exc));
	    }
	}
    }

    private static final Pattern joglerrp = Pattern.compile("GL-Error 0x([0-9a-fA-F]+)\\s");
    private void checkdebuggl(Exception exc) {
	String msg = exc.getMessage();
	GLException wrap = null;
	if(msg.indexOf("GL_INVALID_ENUM") >= 0) {
	    wrap = new GLException.GLInvalidEnumException();
	} else if(msg.indexOf("GL_INVALID_VALUE") >= 0) {
	    wrap = new GLException.GLInvalidValueException();
	} else if(msg.indexOf("GL_INVALID_OPERATION") >= 0) {
	    wrap = new GLException.GLInvalidOperationException();
	} else if(msg.indexOf("GL_OUT_OF_MEMORY") >= 0) {
	    wrap = new GLException.GLOutOfMemoryException();
	} else {
	    Matcher m = joglerrp.matcher(msg);
	    if(m.find()) {
		try {
		    wrap = GLException.glexcfor(Integer.parseInt(m.group(1), 16));
		} catch(NumberFormatException e) {
		    exc.addSuppressed(e);
		}
	    }
	}
	if(wrap != null) {
	    wrap.initCause(exc);
	    throw(wrap);
	}
    }

    public void abort() {
	for(int i = 0; i < n; i++)
	    list[i].abort();
    }

    protected void add(Command cmd) {
	if(n >= list.length)
	    list = Arrays.copyOf(list, list.length * 2);
	list[n++] = cmd;
    }

    public BufferBGL trim() {
	list = Arrays.copyOf(list, n);
	return(this);
    }

    protected Iterable<Command> dump() {
	return(new Iterable<Command>() {
		public Iterator<Command> iterator() {
		    return(new Iterator<Command>() {
			    int i = 0;
			    public boolean hasNext() {
				return(i < n);
			    }
			    public Command next() {
				if(i < n)
				    return(list[i++]);
				throw(new NoSuchElementException());
			    }
			    public void remove() {
				throw(new UnsupportedOperationException());
			    }
			});
		}
	    });
    }

    public static class Profile {
	private double stime;
	private int n;
	private Command[] cmds = new Command[128];
	private double[] times = new double[128];

	private void register(Command cmd) {
	    if(n >= cmds.length) {
		cmds = Arrays.copyOf(cmds, cmds.length * 2);
		times = Arrays.copyOf(times, cmds.length * 2);
	    }
	    cmds[n] = cmd;
	    times[n] = Utils.rtime();
	    n++;
	}

	public final Request start = gl -> {
	    if(curprof != null)
		throw(new IllegalStateException());
	    curprof = this;
	    stime = Utils.rtime();
	};
	public final Request stop = gl -> {
	    if(curprof == null)
		throw(new IllegalStateException());
	    curprof = null;
	};

	public void dump(PrintStream out) {
	    for(int i = 0; i < n; i++)
		out.printf("%f\t%f\t%s\n", (times[i] - stime) * 1000, ((i < n - 1) ? times[i + 1] - times[i] : 0) * 1000, cmds[i]);
	}

	public Request dump(Path out) {
	    return(gl -> {
		    try(OutputStream fp = new BufferedOutputStream(Files.newOutputStream(out))) {
			dump(new PrintStream(fp));
		    } catch(IOException e) {
			throw(new RuntimeException(e));
		    }
		});
	}
    }
}
