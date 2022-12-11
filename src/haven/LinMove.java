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

public class LinMove extends Moving {
    public static final double MAXOVER = 0.5;
    public Coord2d s, v;
    public double t, lt, e;
    public boolean ts = false;

    public LinMove(Gob gob, Coord2d s, Coord2d v) {
	super(gob);
	this.s = s;
	this.v = v;
	this.t = 0;
	this.e = Double.NaN;
    }

    public Coord3f getc() {
	return(gob.placer().getc(s.add(v.mul(t)), gob.a));
    }

    public double getv() {
	return(v.abs());
    }

    public void ctick(double dt) {
	if(!ts) {
	    t += dt * 0.9;
	    if(!Double.isNaN(e) && (t > e)) {
		t = e;
	    } else if(t > lt + MAXOVER) {
		t = lt + MAXOVER;
		ts = true;
	    }
	}
    }

    public void sett(double t) {
	lt = t;
	if(t > this.t) {
	    this.t = t;
	    ts = false;
	}
    }

    @OCache.DeltaType(OCache.OD_LINBEG)
    public static class $linbeg implements OCache.Delta {
	public void apply(Gob g, Message msg) {
	    Coord2d s = msg.coord().mul(OCache.posres);
	    Coord2d v = msg.coord().mul(OCache.posres);
	    LinMove lm = g.getattr(LinMove.class);
	    if((lm == null) || !lm.s.equals(s) || !lm.v.equals(v)) {
		g.setattr(new LinMove(g, s, v));
	    }
	}
    }

    @OCache.DeltaType(OCache.OD_LINSTEP)
    public static class $linstep implements OCache.Delta {
	public void apply(Gob g, Message msg) {
	    double t, e;
	    int w = msg.int32();
	    if(w == -1) {
		t = e = -1;
	    } else if((w & 0x80000000) == 0) {
		t = w * 0x1p-10;
		e = -1;
	    } else {
		t = (w & ~0x80000000) * 0x1p-10;
		w = msg.int32();
		e = (w < 0)?-1:(w * 0x1p-10);
	    }
	    Moving m = g.getattr(Moving.class);
	    if((m == null) || !(m instanceof LinMove))
		return;
	    LinMove lm = (LinMove)m;
	    if(t < 0)
		g.delattr(Moving.class);
	    else
		lm.sett(t);
	    if(e >= 0)
		lm.e = e;
	    else
		lm.e = Double.NaN;
	}
    }
}
