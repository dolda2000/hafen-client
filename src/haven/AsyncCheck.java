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

import java.util.function.*;

public class AsyncCheck<T> {
    public final Object mon;
    public final Supplier<T> check;
    public final Consumer<T> use;
    public final String name;
    private double timeout = 5;
    private Thread th = null;

    public AsyncCheck(Object mon, String name, Supplier<T> check, Consumer<T> use) {
	this.mon = mon;
	this.check = check;
	this.use = use;
	this.name = name;
    }

    private void checkloop() {
	try {
	    while(true) {
		T item;
		synchronized(mon) {
		    double start = Utils.rtime(), now = start;
		    while(true) {
			if((item = check.get()) != null)
			    break;
			if((now - start) >= timeout)
			    return;
			mon.wait((long)((timeout - (now - start)) * 1000) + 100);
			now = Utils.rtime();
		    }
		}
		use.accept(item);
	    }
	} catch(InterruptedException e) {
	} finally {
	    synchronized(mon) {
		if(th == Thread.currentThread())
		    th = null;
		check();
	    }
	}
    }

    public void check() {
	synchronized(mon) {
	    if((check.get() != null) && (th == null)) {
		th = new HackThread(this::checkloop, name);
		th.setDaemon(true);
		th.start();
	    }
	}
    }

    public AsyncCheck<T> timeout(double timeout) {
	this.timeout = timeout;
	return(this);
    }
}
