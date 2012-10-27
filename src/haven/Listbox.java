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

import java.awt.Color;

public abstract class Listbox<T> extends Widget {
    public final int h, itemh;
    public final Scrollbar sb;
    public T sel;

    public Listbox(Coord c, Widget parent, int w, int h, int itemh) {
	super(c, new Coord(w, h * itemh), parent);
	this.h = h;
	this.itemh = itemh;
	this.sb = new Scrollbar(new Coord(sz.x, 0), sz.y, this, 0, 0);
    }

    protected abstract T listitem(int i);
    protected abstract int listitems();
    protected abstract void drawitem(GOut g, T item, Coord c);

    protected void drawsel(GOut g, Coord c) {
	g.chcolor(255, 255, 0, 128);
	g.frect(c, new Coord(sz.x, itemh));
	g.chcolor();
    }

    public void draw(GOut g) {
	sb.max = listitems() - h;
	g.chcolor(Color.BLACK);
	g.frect(Coord.z, sz);
	g.chcolor();
	int n = listitems();
	for(int i = 0; i < h; i++) {
	    int idx = i + sb.val;
	    if(idx >= n)
		break;
	    T item = listitem(idx);
	    Coord c = new Coord(0, i * itemh);
	    if(item == sel)
		drawsel(g, c);
	    drawitem(g, item, c);
	}
	super.draw(g);
    }

    public boolean mousewheel(Coord c, int amount) {
	sb.ch(amount);
	return(true);
    }

    public void change(T item) {
	this.sel = item;
    }

    protected void itemclick(T item, int button) {
	if(button == 1)
	    change(item);
    }

    public T itemat(Coord c) {
	int idx = (c.y / itemh) + sb.val;
	if(idx >= listitems())
	    return(null);
	return(listitem(idx));
    }

    public boolean mousedown(Coord c, int button) {
	if(super.mousedown(c, button))
	    return(true);
	T item = itemat(c);
	if((item == null) && (button == 1))
	    change(null);
	else if(item != null)
	    itemclick(item, button);
	return(true);
    }
}
