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

import java.io.*;
import java.net.*;

public class HttpStatus extends HackThread {
    public static final Config.Variable<URI> mond = Config.Services.var("srvmon", "");
    public final URI src;
    public boolean syn = false;
    public String status;
    public int users;
    private boolean quit = false;

    public HttpStatus(URI src) {
	super("Server status updater");
	setDaemon(true);
	this.src = src;
    }

    public HttpStatus() {
	this(mond.get());
    }

    private void handle(String... words) {
	if(words.length < 1) {
	    synchronized(this) {
		syn = true;
		notifyAll();
	    }
	    return;
	}
	switch(words[0]) {
	    case "state": {
		synchronized(this) {
		    this.status = words[2].intern();
		    notifyAll();
		}
		break;
	    }
	    case "users": {
		synchronized(this) {
		    this.users = Integer.parseInt(words[1]);
		    notifyAll();
		}
		break;
	    }
	}
    }

    private void handle(byte[] buf, int off, int len) {
	String[] words = Utils.splitwords(new String(buf, off, len, Utils.utf8));
	handle(words);
    }

    private static final int[] delays = {1, 2, 5, 10, 60};
    private InputStream cur = null;
    public void run() {
	int retries = 0;
	while(!quit) {
	    if(retries > 0) {
		try {
		    Thread.sleep(delays[retries - 1] * 1000);
		} catch(InterruptedException e2) {
		    continue;
		}
	    }
	    if(retries < delays.length)
		retries++;
	    try {
		synchronized(this) {
		    syn = false;
		    status = "";
		    notifyAll();
		}
		URLConnection c = Http.open(src.toURL());
		c.setUseCaches(false);
		InputStream fp = c.getInputStream();
		try {
		    synchronized(this) {
			cur = fp;
		    }
		    byte[] buf = new byte[1024];
		    int len = 0;
		    while(true) {
			int off = len;
			int rv = fp.read(buf, len, buf.length - len);
			if(rv < 0)
			    break;
			len += rv;
			line: while(true) {
			    for(int i = off; i < len; i++) {
				if(buf[i] == 10) {
				    handle(buf, 0, i);
				    System.arraycopy(buf, i + 1, buf, 0, len -= i + 1);
				    off = 0;
				    continue line;
				}
			    }
			    break;
			}
			if(syn)
			    retries = 1;
		    }
		} finally {
		    fp.close();
		    synchronized(this) {
			cur = null;
		    }
		}
	    } catch(IOException e) {
	    }
	}
    }

    public void quit() {
	synchronized(this) {
	    quit = true;
	    interrupt();
	    try {
		if(cur != null)
		    cur.close();
	    } catch(IOException e) {
	    }
	}
    }
}
