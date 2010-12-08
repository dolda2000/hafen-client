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

public class Profwnd extends Window {
    public final Profile prof;
    public long mt = 50000000;
    private static final int h = 80;
    
    public Profwnd(Coord c, Widget parent, Profile prof, String title) {
	super(c, new Coord(prof.hist.length, h), parent, title);
	this.prof = prof;
    }
    
    public void cdraw(GOut g) {
	long[] ttl = new long[prof.hist.length];
	for(int i = 0; i < prof.hist.length; i++) {
	    if(prof.hist[i] != null)
		ttl[i] = prof.hist[i].total;
	}
	Arrays.sort(ttl);
	int ti = ttl.length;
	for(int i = 0; i < ttl.length; i++) {
	    if(ttl[i] != 0) {
		ti = ttl.length - ((ttl.length - i) / 10);
		break;
	    }
	}
	if(ti < ttl.length)
	    mt = ttl[ti];
	else
	    mt = 50000000;
	g.image(prof.draw(h, mt / h), Coord.z);
    }
    
    public String tooltip(Coord c, boolean again) {
	c = xlate(c, false);
	if((c.x >= 0) && (c.x < prof.hist.length) && (c.y >= 0) && (c.y < h)) {
	    int x = c.x;
	    int y = c.y;
	    long t = (h - y) * (mt / h);
	    Profile.Frame f = prof.hist[x];
	    if(f != null) {
		for(int i = 0; i < f.prt.length; i++) {
		    if((t -= f.prt[i]) < 0)
			return(String.format("%.2f ms, %s: %.2f ms", (((double)f.total) / 1000000), f.nm[i], (((double)f.prt[i]) / 1000000)));
		}
	    }
	}
	return("");
    }
    
    public void wdgmsg(Widget sender, String msg, Object... args) {
	if(msg.equals("close")) {
	    ui.destroy(this);
	} else {
	    super.wdgmsg(sender, msg, args);
	}
    }
}
