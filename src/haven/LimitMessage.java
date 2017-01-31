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

public class LimitMessage extends Message {
    private final Message bk;
    private int left;
    private final boolean eoferror;

    public LimitMessage(Message bk, int left, boolean eoferror) {
	this.bk = bk;
	this.left = left;
	this.eoferror = eoferror;
    }

    public LimitMessage(Message bk, int left) {
	this(bk, left, true);
    }

    public boolean underflow(int hint) {
	if(left < 1)
	    return(false);
	if(hint + rt - rh <= rbuf.length) {
	    System.arraycopy(rbuf, rh, rbuf, 0, rt - rh);
	} else {
	    byte[] n = new byte[Math.min(left, Math.max(hint, 32)) + rt - rh];
	    System.arraycopy(rbuf, rh, n, 0, rt - rh);
	    rbuf = n;
	}
	rt -= rh;
	rh = 0;
	if(bk.rt - bk.rh < 1) {
	    if(!bk.underflow(hint)) {
		if(eoferror)
		    throw(new EOF(String.format("Early EOF in sized message with %d bytes left", left)));
		return(false);
	    }
	}
	int len = Math.min(left, Math.min(bk.rt - bk.rh, rbuf.length - rt));
	System.arraycopy(bk.rbuf, bk.rh, rbuf, rt, len);
	bk.rh += len;
	rt += len;
	left -= len;
	return(true);
    }

    public void overflow(int min) {
	throw(new RuntimeException("LimitMessage is not writeable"));
    }
}
