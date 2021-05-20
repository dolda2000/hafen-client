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
import java.io.*;

public abstract class RetryingInputStream extends InputStream {
    private InputStream cur;
    private boolean eof, fatal;
    private long pos;
    private int retries;
    private IOException lasterr;

    protected InputStream create() throws IOException {
	throw(new IOException("RetryingInputStream must override at least some version of create()"));
    }

    protected InputStream create(long pos) throws IOException {
	InputStream ret = create();
	for(long s = 0; s < pos; s += ret.skip(pos - s));
	return(ret);
    }

    private static final double[] sleep = {0.01, 0.1, 0.5, 1.0, 2.0, 5.0};
    protected void retry(int retries, IOException lasterr) throws IOException {
	if(lasterr instanceof FileNotFoundException)
	    throw(lasterr);
	if(retries >= sleep.length) {
	    throw(new IOException("already retried 5 times", lasterr));
	} else {
	    try {
		Thread.sleep((int)(sleep[retries] * 1000));
	    } catch(InterruptedException e) {
		Thread.currentThread().interrupt();
		throw(new InterruptedIOException("interrupted at " + retries + " retries"));
	    }
	}
    }

    private InputStream get() throws IOException {
	if(cur == null) {
	    int count = retries++;
	    if(count > 0) {
		try {
		    retry(count, lasterr);
		} catch(Throwable e) {
		    fatal = true;
		    throw(e);
		}
	    }
	    cur = create(pos);
	}
	return(cur);
    }

    private void failed(IOException err) throws IOException {
	if(fatal)
	    throw(err);
	lasterr = err;
	if(cur != null) {
	    try {
		cur.close();
	    } catch(IOException e) {}
	    cur = null;
	}
    }

    public long skip(long len) throws IOException {
	if(eof)
	    return(0);
	try {
	    long ret = get().skip(len);
	    pos += ret;
	    return(ret);
	} catch(IOException e) {
	    failed(e);
	    return(0);
	}
    }

    public int read(byte[] buf, int off, int len) throws IOException {
	if(eof)
	    return(-1);
	int ret;
	try {
	    ret = get().read(buf, off, len);
	} catch(IOException e) {
	    failed(e);
	    return(0);
	}
	if(ret < 0) {
	    eof = true;
	    cur.close();
	    cur = null;
	} else {
	    pos += ret;
	}
	return(ret);
    }

    public int read() throws IOException {
	if(eof)
	    return(-1);
	int ret;
	while(true) {
	    try {
		ret = get().read();
		break;
	    } catch(IOException e) {
		failed(e);
	    }
	}
	if(ret < 0) {
	    eof = true;
	    cur.close();
	    cur = null;
	} else {
	    pos++;
	}
	return(ret);
    }

    public void close() throws IOException {
	if(cur != null) {
	    cur.close();
	    cur = null;
	}
    }
}
