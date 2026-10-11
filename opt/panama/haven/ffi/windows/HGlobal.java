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

package haven.ffi.windows;

import haven.*;
import haven.ffi.windows.Win32.*;

public class HGlobal {
    private static final Win32 win = Win32.get();
    private final Holder h;
    private final Runnable c;

    private static class Holder implements Finalizer.Cleaner {
	final Handle hmem;
	boolean relinquished = false;

	Holder(Handle hmem) {
	    this.hmem = hmem;
	}

	public void clean() {
	    if(!relinquished)
		win.GlobalFree(hmem);
	}
    }

    public HGlobal(Handle hmem, boolean borrowed) {
	this.h = new Holder(hmem);
	c = Finalizer.finalize(this, h);
	h.relinquished = borrowed;
    }

    public HGlobal(int bytes) {
	this(win.GlobalAlloc(Win32.GMEM_MOVEABLE | Win32.GMEM_ZEROINIT, bytes), false);
    }

    public Handle hmem() {
	return(h.hmem);
    }

    public void relinquish() {
	h.relinquished = true;
    }

    public void free() {
	c.run();
    }
}
