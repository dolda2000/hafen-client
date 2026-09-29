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

import haven.render.*;
import java.util.*;
import java.nio.*;
import java.awt.Color;
import haven.Profile.*;

public class Profchart extends Widget {
    public static final double zoomfac = 1.5;
    public static final int h = UI.scale(10);
    public static final int marg = UI.scale(2);
    public final Part root;
    private double f, t;

    private static int depth(Part p) {
	int d = 0;
	for(Part ch : p.sub())
	    d = Math.max(d, depth(ch) + 1);
	return(d);
    }

    public Profchart(Part root) {
	this.root = root;
	resize(UI.scale(400), (depth(root) * (h + marg)) - marg);
	f = root.f(); t = root.t();
    }

    private int bisect(List<Part> parts, double t, boolean left) {
	int l = 0, r = parts.size();
	while(r > l) {
	    int m = l + ((r - l) / 2);
	    Part p = parts.get(m);
	    if(t > p.t()) {
		l = m + 1;
	    } else if(t < p.f()) {
		r = m;
	    } else {
		return(m);
	    }
	}
	return(left ? l - 1 : l);
    }

    private void drawlevel(GOut g, List<Part> parts, int y, int co) {
	double d = t - f, id = 1.0 / d;
	int fi = bisect(parts, f, false), li = bisect(parts, t, true);
	for(int i = fi; i <= li; i++) {
	    Part p = parts.get(i);
	    g.chcolor(Utils.vgapal[(i + co) % 16]);
	    g.frect2(Coord.of(Utils.ifloor((p.f() - f) * id * sz.x), y),
		     Coord.of(Utils.iceil ((p.t() - f) * id * sz.x), y + h));
	    if(!p.sub().isEmpty())
		drawlevel(g, p.sub(), y + h + marg, co + i);
	}
    }

    public void draw(GOut g) {
	drawlevel(g, root.sub(), 0, 0);
    }

    private Part partat(Coord c) {
	double T = f + (((double)c.x / (double)sz.x) * (t - f));
	int y = 0;
	List<Part> parts = root.sub();
	while(true) {
	    int pi = bisect(parts, T, false);
	    if(pi >= parts.size())
		return(null);
	    Part p = parts.get(pi);
	    if(!(T >= p.f()) && (T <= p.t()))
		return(null);
	    if((c.y >= y) && (c.y < y + h))
		return(p);
	    if((parts = p.sub()).isEmpty())
		return(null);
	    y += h + marg;
	    if(c.y < y)
		return(null);
	}
    }

    public String tooltip(Coord c, Widget prev) {
	Part p = partat(c);
	if(p == null)
	    return(null);
	return(Profile.timefmt(p.d()) + ": " + p.nm);
    }

    public boolean mousewheel(MouseWheelEvent ev) {
	double T = f + (((double)ev.c.x / (double)sz.x) * (t - f));
	double F = Math.pow(zoomfac, ev.s);
	double of = f, ot = t;
	f = Math.max(T - ((T - f) * F), root.f());
	t = Math.min(T + ((t - T) * F), root.t());
	return(true);
    }

    private UI.Grab drag = null;
    private double dot;
    public boolean mousedown(MouseDownEvent ev) {
	if(ev.b != 2)
	    return(super.mousedown(ev));
	drag = ui.grabmouse(this);
	dot = f + (((double)ev.c.x / (double)sz.x) * (t - f));
	return(true);
    }

    public void mousemove(MouseMoveEvent ev) {
	super.mousemove(ev);
	if(drag != null) {
	    double d = t - f;
	    f = Utils.clip(dot - (d * ((double)ev.c.x / (double)sz.x)), root.f(), root.t() - d);
	    t = f + d;
	}
    }

    public boolean mouseup(MouseUpEvent ev) {
	if(ev.b != 2)
	    return(super.mouseup(ev));
	if(drag == null)
	    return(false);
	drag.remove();
	drag = null;
	return(true);
    }

    public static Window wndfor(Part root) {
	Profchart chart = new Profchart(root);
	Window wnd = new Window(Coord.z, String.valueOf(root.nm)) {
	    public void reqclose() {
		ui.destroy(this);
	    }

	    public void resize(Coord sz) {
		super.resize(sz);
		chart.resize(sz);
	    }

	    protected Deco makedeco() {
		return(new DefaultDeco(true).dragsize(true));
	    }
	};
	wnd.add(chart);
	wnd.pack();
	return(wnd);
    }
}
