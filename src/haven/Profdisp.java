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

public class Profdisp extends Widget {
    private static final int h = UI.scale(80);
    public final Profile prof;
    public double mt = 0.05;
    private Tex tex = null;
    private double dscale = 0;
    private Tex sscl = null;

    public Profdisp(Profile prof) {
	super(new Coord(prof.hist.length + UI.scale(50), h));
	this.prof = prof;
	setcanfocus(true);
    }

    private static final String[] units = {"s", "ms", "\u00b5s", "ns"};
    public void draw(GOut g) {
	double[] ttl = new double[prof.hist.length];
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
	    mt = 0.05;
	mt *= 1.1;
	if(tex != null)
	    tex.dispose();
	if((sscl == null) || (dscale < mt * 0.70) || (dscale > mt)) {
	    int p = (int)Math.floor(Math.log10(mt));
	    double b = Math.pow(10.0, p) * 0.5;
	    dscale = Math.floor(mt / b) * b;
	    int u = Utils.clip(-Utils.floordiv(p, 3), 0, units.length - 1);
	    if(sscl != null)
		sscl.dispose();
	    sscl = Text.render(String.format("%.1f %s", dscale * Math.pow(10.0, u * 3), units[u])).tex();
	}
	tex = prof.draw(h, mt / h);
	g.image(tex, Coord.z);
	int sy = (int)Math.round((1 - (dscale / mt)) * h);
	g.chcolor(192, 192, 192, 128);
	g.line(new Coord(0, sy), new Coord(prof.hist.length, sy), 1);
	g.chcolor();
	g.image(sscl, new Coord(prof.hist.length + UI.scale(2), sy - (sscl.sz().y / 2)));
    }

    public boolean keydown(KeyDownEvent ev) {
	if(ev.c == 'd') {
	    prof.dump(System.err);
	    return(true);
	}
	return(super.keydown(ev));
    }

    public String tooltip(Coord c, Widget prev) {
	c = xlate(c, false);
	if((c.x >= 0) && (c.x < prof.hist.length) && (c.y >= 0) && (c.y < h)) {
	    int x = c.x;
	    int y = c.y;
	    double t = (h - y) * (mt / h);
	    Profile.Frame f = prof.hist[x];
	    if(f != null) {
		for(int i = 0; i < f.prt.length; i++) {
		    if((t -= f.prt[i]) < 0)
			return(String.format("%.2f ms, %s: %.2f ms", f.total * 1000, f.nm[i], f.prt[i] * 1000));
		}
	    }
	}
	return("");
    }
}
