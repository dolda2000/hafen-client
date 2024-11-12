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

public abstract class SListBox<I, W extends Widget> extends SListWidget<I, W> implements Scrollable {
    public static final Color every = new Color(255, 255, 255, 16), other = new Color(255, 255, 255, 32);
    public final int itemh, marg;
    public final Scrollbar sb;
    private Map<I, W> curw = new IdentityHashMap<>();
    private I[] curi;
    private int n = -1, h, curo = 0, itemw = 0;
    private int maxy = 0, cury = 0;
    private boolean reset = false;

    public SListBox(Coord sz, int itemh, int marg) {
	super(sz);
	this.itemh = itemh;
	this.marg = marg;
	if(autoscroll())
	    this.sb = add(new Scrollbar(0, this));
	else
	    this.sb = null;
	resize(sz);
    }
    public SListBox(Coord sz, int itemh) {this(sz, itemh, 0);}

    protected boolean autoscroll() {return(true);}
    public int scrollmin() {return(0);}
    public int scrollmax() {return(maxy);}
    public int scrollval() {return(cury);}
    public void scrollval(int val) {cury = val;}

    @SuppressWarnings("unchecked")
    public void tick(double dt) {
	boolean reset = this.reset;
	this.reset = false;
	List<? extends I> items = items();
	if(items.size() != n) {
	    n = items.size();
	    int th = (n == 0) ? 0 : (itemh + ((n - 1) * (itemh + marg)));
	    maxy = th - sz.y;
	    cury = Math.min(cury, Math.max(maxy, 0));
	}
	int itemw = sz.x - (((sb != null) && sb.vis()) ? sb.sz.x : 0);
	if(itemw != this.itemw) {
	    reset = true;
	    this.itemw = itemw;
	}
	int sy = cury, off = sy / (itemh + marg);
	if(reset) {
	    for(W cw : curw.values())
		cw.destroy();
	    curi = null;
	    curw.clear();
	}
	boolean update = false;
	if((curi == null) || (curi.length != h) || (curo != off))
	    update = true;
	if(!update) {
	    for(int i = 0; i < h; i++) {
		I item = (i + curo < items.size()) ? items.get(i + curo) : null;
		if((curi[i] != item)) {
		    update = true;
		    break;
		}
	    }
	}
	if(update) {
	    I[] newi = (I[])new Object[h];
	    Map<I, W> neww = new IdentityHashMap<>();
	    Coord itemsz = Coord.of(itemw, itemh);
	    for(int i = 0; i < h; i++) {
		int np = i + off;
		newi[i] = (np < items.size()) ? items.get(np) : null;
		if(newi[i] != null) {
		    W pw = curw.remove(newi[i]);
		    if(pw == null)
			neww.put(newi[i], add(makeitem(newi[i], np, itemsz)));
		    else
			neww.put(newi[i], pw);
		}
	    }
	    for(W pw : curw.values())
		pw.destroy();
	    curi = newi;
	    curw = neww;
	    curo = off;
	}
	boolean updpos = update;
	if(!updpos) {
	    for(int i = 0; i < curi.length; i++) {
		if(curi[i] != null) {
		    W w = curw.get(curi[i]);
		    if(w.c.y != ((i * (itemh + marg)) - sy)) {
			updpos = true;
			break;
		    }
		}
	    }
	}
	if(updpos) {
	    for(int i = 0; i < curi.length; i++) {
		if(curi[i] != null)
		    curw.get(curi[i]).move(Coord.of(0, ((i + curo) * (itemh + marg)) - sy));
	    }
	}
	super.tick(dt);
    }

    public void reset() {
	this.reset = true;
    }

    public W getcur(I item) {
	return(curw.get(item));
    }

    protected void drawbg(GOut g) {
    }

    protected void drawbg(GOut g, I item, int idx, Area area) {
	g.chcolor(((idx % 2) == 0) ? every : other);
	g.frect2(area.ul, area.br);
	g.chcolor();
    }

    protected void drawsel(GOut g, I item, int idx, Area area) {
	g.chcolor(255, 255, 0, 128);
	g.frect2(area.ul, area.br);
	g.chcolor();
    }

    protected void drawslot(GOut g, I item, int idx, Area area) {
	drawbg(g, item, idx, area);
	if((sel != null) && (sel == item))
	    drawsel(g, item, idx, area);
    }

    public void draw(GOut g) {
	drawbg(g);
	if(curi != null) {
	    List<? extends I> items = items();
	    int sy = cury;
	    for(int i = 0; (i < curi.length) && (i + curo < items.size()); i++) {
		if(curi[i] != null)
		    drawslot(g, curi[i], i + curo, Area.sized(Coord.of(0, ((i + curo) * (itemh + marg)) - sy), Coord.of(itemw, itemh)));
	    }
	}
	super.draw(g);
    }

    public int slotat(Coord c) {
	return((c.y + cury) / (itemh + marg));
    }

    public boolean mousewheel(MouseWheelEvent ev) {
	if(ev.propagate(this) || super.mousewheel(ev))
	    return(true);
	int step = sz.y / 8;
	if(maxy > 0)
	    step = Math.min(step, maxy / 8);
	step = Math.max(step, itemh);
	cury = Math.max(Math.min(cury + (step * ev.a), maxy), 0);
	return(true);
    }

    protected boolean unselect(int button) {
	if(button == 1)
	    change(null);
	return(true);
    }

    protected boolean slotclick(Coord c, int slot, int button) {
	return(false);
    }

    public boolean mousedown(MouseDownEvent ev) {
	if(ev.propagate(this) || super.mousedown(ev))
	    return(true);
	int slot = slotat(ev.c);
	if((slot >= 0) && slotclick(ev.c, slotat(ev.c), ev.b))
	    return(true);
	return(unselect(ev.b));
    }

    public void resize(Coord sz) {
	super.resize(sz);
	if(sb != null) {
	    sb.resize(sz.y);
	    sb.c = new Coord(sz.x - sb.sz.x, 0);
	}
	h = Math.max(((sz.y + itemh + marg - 2) / (itemh + marg)), 0) + 1;
    }

    public void display(int idx) {
	int y = idx * (itemh + marg);
	if(y < cury)
	    cury = y;
	else if(y + itemh >= cury + sz.y)
	    cury = Math.max((y + itemh) - sz.y, 0);
    }

    public void display(I item) {
	int p = items().indexOf(item);
	if(p >= 0)
	    display(p);
    }

    public void display() {
	display(sel);
    }
}
