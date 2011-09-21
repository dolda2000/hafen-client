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

public class Homing extends Moving {
    long tgt;
    Coord tc;
    int v;
    double dist;
    
    public Homing(Gob gob, long tgt, Coord tc, int v) {
	super(gob);
	this.tgt = tgt;
	this.tc = tc;
	this.v = v;
    }
    
    public Coord3f getc() {
	Coord tc = this.tc;
	Gob tgt = gob.glob.oc.getgob(this.tgt);
	if(tgt != null)
	    tc = tgt.rc;
	Coord d = tc.add(gob.rc.inv());
	double e = gob.rc.dist(tc);
	float rx = gob.rc.x, ry = gob.rc.y;
	if(e > 0.00001) {
	    rx += (float)((d.x / e) * dist);
	    ry += (float)((d.y / e) * dist);
	}
	return(new Coord3f(rx, ry, gob.glob.map.getcz(rx, ry)));
    }
    
    public double getv() {
	return((v / 100.0) / 0.06);
    }
    
    public void move(Coord c) {
	dist = 0;
    }
    
    public void ctick(int dt) {
	double da = ((double)dt / 1000) / 0.06;
	dist += (da * 0.9) * ((double)v / 100);
    }
}
