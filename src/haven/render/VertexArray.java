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

package haven.render;

import haven.render.sl.Attribute;

public class VertexArray {
    public final Buffer[] bufs;
    public final int n;

    public VertexArray(Buffer... bufs) {
	Buffer[] na = new Buffer[bufs.length];
	na[0] = bufs[0];
	int num = na[0].n;
	for(int i = 1; i < bufs.length; i++) {
	    na[i] = bufs[i];
	    if(na[i].n != num)
		throw(new IllegalArgumentException("Buffer sizes do not match"));
	}
	this.bufs = na;
	this.n = num;
    }

    public abstract static class Buffer {
	public final int n, nc;
	public final NumberFormat fmt;
	public final Attribute tgt;

	public Buffer(int n, int nc, NumberFormat fmt, Attribute tgt) {
	    this.n = n;
	    this.nc = nc;
	    this.fmt = fmt;
	    this.tgt = tgt;
	}

	public abstract FillBuffer fill(Environment env);
    }
}
