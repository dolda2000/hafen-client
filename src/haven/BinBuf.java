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

public class BinBuf extends BinCodec {
    private final int oh;
    
    public BinBuf(byte[] blob, int off, int len) {
	this.rbuf = blob;
	this.rh = this.oh = off;
	this.rt = off + len;
    }
    public BinBuf(byte[] blob) {
	this(blob, 0, blob.length);
    }
    public BinBuf() {
	this.oh = 0;
    }

    protected boolean underflow(int hint) {
	return(false);
    }
    protected void overflow(int min) {
	if(wbuf.length == 0) {
	    wbuf = new byte[32];
	    wh = 0;
	    wt = 32;
	} else {
	    byte[] n = new byte[wbuf.length * 2];
	    int cl = wt - wh;
	    System.arraycopy(wbuf, wh, n, 0, cl);
	    wbuf = n;
	    wh = 0;
	    wt = cl;
	}
    }

    public byte[] fin() {
	byte[] ret = new byte[wt - wh];
	System.arraycopy(wbuf, wh, ret, 0, wt - wh);
	return(ret);
    }
}
