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
import java.nio.file.*;
import haven.error.ErrorHandler;

public class Warning extends Throwable {
    public static final int DEBUG = 0;
    public static final int ERROR = 5;
    public static final int CRITICAL = 10;
    public final double ctime = Utils.rtime();
    public int level = DEBUG;
    public boolean trace = false, ctrace = true;

    public Warning(Throwable cause, String message) {
	super(message, cause);
    }

    public Warning(String message) {
	super(message);
    }

    public Warning(String message, Object... args) {
	this(String.format(message, args));
    }

    public Warning(Throwable cause) {
	super(cause);
    }

    public Warning level(int level) {this.level = level; return(this);}
    public Warning trace(boolean trace) {this.trace = trace; return(this);}
    public Warning ctrace(boolean ctrace) {this.ctrace = ctrace; return(this);}

    public void report(PrintStream out, String head) {
	out.printf("%s%s\n", head, (getClass() == Warning.class) ? getMessage() : toString());
	if(trace) {
	    for(StackTraceElement frame : getStackTrace())
		out.println("\tat " + frame);
	}
	if((getCause() != null) && ctrace)
	    getCause().printStackTrace(out);
	out.flush();
    }

    private static final int LOGSIZE = 10;
    private static LinkedList<Warning> log = null;
    public void issue() {
	report(System.err, "haven: warning: ");
	if(level >= ERROR) {
	    /* XXX: Report in some user-visible way. */
	}
	if(level >= CRITICAL) {
	    try(OutputStream fp = Files.newOutputStream(Debug.somedir("haven-errors.log"), StandardOpenOption.APPEND, StandardOpenOption.CREATE)) {
		report(new PrintStream(fp), String.format("%s: ", new Date()));
	    } catch(IOException e) {
		new Warning(e, "could not log critical warning").level(ERROR).issue();
	    }
	}
	synchronized(Warning.class) {
	    if(log == null) {
		ErrorHandler errh = ErrorHandler.find();
		if(errh != null)
		    errh.lsetprop("warnings", log = new LinkedList<>());
	    }
	    if(log != null) {
		log.add(this);
		while(log.size() > LOGSIZE)
		    log.removeFirst();
	    }
	}
    }

    public static void warn(int level, String fmt, Object... args) {
	new Warning(fmt, args).level(level).issue();
    }

    public static void warn(String fmt, Object... args) {
	warn(DEBUG, fmt, args);
    }
}
