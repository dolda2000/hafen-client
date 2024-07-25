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
import java.awt.Color;

public class Party {
    public Map<Long, Member> memb = Collections.emptyMap();
    public Member leader = null;
    public int id;
    private final Glob glob;
    private int mseq = 0;

    public Party(Glob glob) {
	this.glob = glob;
    }

    public class Member {
	public final long gobid;
	public final int seq;
	private Coord2d c = null;
	private double ma = Math.random() * Math.PI * 2;
	private double oa = Double.NaN;
	public Color col = Color.BLACK;

	public Member(long gobid) {
	    this.gobid = gobid;
	    this.seq = mseq++;
	}

	public Gob getgob() {
	    return(glob.oc.getgob(gobid));
	}

	public Coord2d getc() {
	    Gob gob;
	    try {
		if((gob = getgob()) != null) {
		    this.oa = gob.a;
		    return(new Coord2d(gob.getc()));
		}
	    } catch(Loading e) {}
	    this.oa = Double.NaN;
	    return(c);
	}

	void setc(Coord2d c) {
	    if((this.c != null) && (c != null))
		ma = this.c.angle(c);
	    this.c = c;
	}

	public double geta() {
	    return(Double.isNaN(oa) ? ma : oa);
	}
    }
}
