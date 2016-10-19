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

import java.util.zip.*;
import java.io.*;

public class ZMessage extends Message implements Closeable, Flushable {
    private Inflater zi = null;
    private Deflater zo = null;
    private boolean eof;
    private final Message bk;

    public ZMessage(Message from) {
	this.bk = from;
    }

    public boolean underflow(int hint) {
	if(zi == null) {
	    if(eof)
		return(false);
	    zi = new Inflater();
	}
	boolean ret = false;
	if(rbuf.length - rt < 1) {
	    byte[] n = new byte[Math.max(1024, rt - rh) + rt - rh];
	    System.arraycopy(rbuf, rh, n, 0, rt - rh);
	    rt -= rh;
	    rh = 0;
	    rbuf = n;
	}
	try {
	    while(true) {
		int rv = zi.inflate(rbuf, rt, rbuf.length - rt);
		if(rv == 0) {
		    if(zi.finished()) {
			zi.end();
			zi = null;
			eof = true;
			return(ret);
		    }
		    if(zi.needsInput()) {
			if(bk.rt - bk.rh < 1) {
			    if(!bk.underflow(128))
				throw(new EOF("Unterminated z-blob"));
			}
			zi.setInput(bk.rbuf, bk.rh, bk.rt - bk.rh);
			bk.rh = bk.rt;
		    }
		} else {
		    rt += rv;
		    return(true);
		}
	    }
	} catch(DataFormatException e) {
	    throw(new FormatError("Malformed z-blob", e));
	}
    }

    private void flush(boolean sync, boolean finish) {
	if(zo == null)
	    zo = new Deflater(9);
	zo.setInput(wbuf, 0, wh);
	if(finish)
	    zo.finish();
	while(!zo.needsInput() || (finish && !zo.finished())) {
	    if(bk.wt - bk.wh < 1)
		bk.overflow(1024);
	    int rv = zo.deflate(bk.wbuf, bk.wh, bk.wt - bk.wh, sync?Deflater.SYNC_FLUSH:Deflater.NO_FLUSH);
	    bk.wh += rv;
	}
	wh = 0;
	if(finish) {
	    zo.end();
	    zo = null;
	}
    }

    public void flush() {
	flush(true, false);
    }

    public void overflow(int min) {
	if(wh > 1024)
	    flush(false, false);
	if(wt - wh < min) {
	    int l = (wbuf.length == 0)?1024:wbuf.length;
	    while(l < wh + min)
		l *= 2;
	    byte[] n = new byte[l];
	    System.arraycopy(wbuf, 0, n, 0, wh);
	    wbuf = n;
	    wt = wbuf.length;
	}
    }

    public void finish() {
	flush(false, true);
    }

    public void close() throws IOException {
	finish();
	if(bk instanceof Closeable)
	    ((Closeable)bk).close();
    }
}
