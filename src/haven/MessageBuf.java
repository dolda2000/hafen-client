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

public class MessageBuf extends Message {
    public static final MessageBuf nil = new MessageBuf();
    private final int oh;

    public MessageBuf(byte[] blob, int off, int len) {
	if(blob == null)
	    throw(new NullPointerException("blob"));
	this.rbuf = blob;
	this.rh = this.oh = off;
	this.rt = off + len;
    }
    public MessageBuf(byte[] blob) {
	this(blob, 0, blob.length);
    }
    public MessageBuf() {
	this.oh = 0;
    }
    public MessageBuf(Message from) {
	if(from instanceof MessageBuf) {
	    MessageBuf fb = (MessageBuf)from;
	    this.rbuf = fb.rbuf;
	    this.rh = this.oh = fb.rh;
	    this.rt = fb.rt;
	    this.wbuf = fb.wbuf;
	    this.wh = this.wt = fb.wh;
	} else {
	    this.rbuf = from.bytes();
	    this.rh = this.oh = 0;
	    this.rt = rbuf.length;
	}
    }

    public boolean underflow(int hint) {
	return(false);
    }
    public void overflow(int min) {
	int cl = (wt == 0)?32:wt;
	while(cl - wh < min)
	    cl *= 2;
	byte[] n = new byte[cl];
	System.arraycopy(wbuf, 0, n, 0, wh);
	wbuf = n;
	wt = cl;
    }

    public boolean equals(Object o2) {
	if(!(o2 instanceof MessageBuf))
	    return(false);
	MessageBuf m2 = (MessageBuf)o2;
	if((m2.rt - m2.oh) != (rt - oh))
	    return(false);
	for(int i = oh, o = m2.oh; i < rt; i++, o++) {
	    if(m2.rbuf[o] != rbuf[i])
		return(false);
	}
	return(true);
    }

    public int hashCode() {
	int ret = 192581;
	for(int i = oh; i < rt; i++)
	    ret = (ret * 31) + rbuf[i];
	return(ret);
    }

    public int rem() {
	return(rt - rh);
    }

    public void rewind() {
	rh = oh;
    }

    public MessageBuf clone() {
	return(new MessageBuf(rbuf, oh, rt - oh));
    }

    public int size() {
	return(wh);
    }

    public byte[] fin() {
	byte[] ret = new byte[wh];
	System.arraycopy(wbuf, 0, ret, 0, wh);
	return(ret);
    }

    public void fin(byte[] buf, int off) {
	System.arraycopy(wbuf, 0, buf, off, Math.min(wh, buf.length - off));
    }

    public void fin(java.nio.ByteBuffer buf) {
	buf.put(wbuf, 0, wh);
    }

    public String toString() {
	StringBuilder buf = new StringBuilder();
	buf.append("Message(");
	for(int i = oh; i < rt; i++) {
	    if(i > 0)
		buf.append(' ');
	    if(i == rh)
		buf.append('>');
	    buf.append(String.format("%02x", rbuf[i] & 0xff));
	}
	buf.append(")");
	return(buf.toString());
    }
}
