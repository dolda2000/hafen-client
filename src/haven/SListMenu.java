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
import java.util.function.*;

public abstract class SListMenu<I, W extends Widget> extends Widget {
    public static final Tex bg = Window.bg;
    public static final IBox obox = Window.wbox;
    public final InnerList box;
    private UI.Grab mg, kg;

    protected abstract List<? extends I> items();
    protected abstract W makeitem(I item, int idx, Coord sz);
    protected abstract void choice(I item);

    public SListMenu(Coord sz, int itemh) {
	box = new InnerList(sz, itemh);
	resize(box.sz.add(obox.cisz()));
	add(box, obox.ctloff());
    }

    public class Item extends SListWidget.ItemWidget<I> {
	private Item(I item, W child) {
	    super(box, child.sz, item);
	    add(child, 0, 0);
	}
    }

    public class InnerList extends SListBox<I, Item> {
	private Coord mc = Coord.of(-1, -1);

	private InnerList(Coord sz, int itemh) {
	    super(sz, itemh);
	}

	protected List<? extends I> items() {
	    return(SListMenu.this.items());
	}

	protected Item makeitem(I item, int idx, Coord sz) {
	    return(new Item(item, SListMenu.this.makeitem(item, idx, sz)));
	}

	public void change(I item) {
	    choice(item);
	}

	public void mousemove(Coord c) {
	    super.mousemove(c);
	    this.mc = c;
	}

	protected void drawbg(GOut g, I item, int idx, Area area) {
	    if(area.contains(mc)) {
		g.chcolor(255, 255, 0, 128);
		g.frect2(area.ul, area.br);
		g.chcolor();
	    } else {
		super.drawbg(g, item, idx, area);
	    }
	}
    }

    private boolean inited = false;
    public void tick(double dt) {
	if(!inited) {
	    int n = items().size();
	    if(n < (box.sz.y / box.itemh)) {
		box.resizeh(box.itemh * n);
		resize(box.sz.add(obox.cisz()));
	    }
	    inited = true;
	}
	super.tick(dt);
    }

    public void draw(GOut g) {
	Coord bgc = new Coord();
	Coord ctl = obox.btloff();
	Coord cbr = sz.sub(obox.cisz()).add(ctl);
	for(bgc.y = ctl.y; bgc.y < cbr.y; bgc.y += bg.sz().y) {
	    for(bgc.x = ctl.x; bgc.x < cbr.x; bgc.x += bg.sz().x)
		g.image(bg, bgc, ctl, cbr);
	}
	obox.draw(g, Coord.z, sz);
	super.draw(g);
    }

    public boolean mousedown(Coord c, int button) {
	if(!c.isect(Coord.z, sz)) {
	    choice(null);
	} else {
	    super.mousedown(c, button);
	}
	return(true);
    }

    public boolean keydown(java.awt.event.KeyEvent ev) {
	if(key_esc.match(ev))
	    choice(null);
	return(true);
    }

    protected void added() {
	mg = ui.grabmouse(this);
	kg = ui.grabkeys(this);
    }

    public void destroy() {
	mg.remove();
	kg.remove();
	super.destroy();
    }

    public Object tooltip(Coord c, Widget prev) {
	Object ret = super.tooltip(c, prev);
	return((ret != null) ? ret : "");
    }

    public SListMenu<I, W> addat(Widget wdg, Coord c) {
	wdg.ui.root.add(this, wdg.rootpos(c));
	return(this);
    }
}
