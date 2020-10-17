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
import haven.error.ErrorHandler;

public class Warning extends Throwable {
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

    private static final int LOGSIZE = 10;
    private static LinkedList<Warning> log = null;
    public void issue() {
	System.err.printf("hafen: warning: %s\n", (getClass() == Warning.class) ? getMessage() : toString());
	if(getCause() != null)
	    getCause().printStackTrace(System.err);
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

    public static void warn(String fmt, Object... args) {
	new Warning(fmt, args).issue();
    }
}
