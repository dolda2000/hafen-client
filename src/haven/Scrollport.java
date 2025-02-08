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

public class Scrollport extends Widget {
    public final Scrollbar bar;
    public final Scrollcont cont;

    @RName("scr")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    return(new Scrollport((Coord)args[0]));
	}
    }

    public Scrollport(Coord sz) {
	super(sz);
	bar = adda(new Scrollbar(sz.y, 0, 0) {
		public void changed() {
		    cont.sy = bar.val;
		}
	    }, sz.x, 0, 1, 0);
	cont = add(new Scrollcont(sz.sub(bar.sz.x, 0)) {
		public void update() {
		    bar.max = Math.max(0, contentsz().y + 10 - sz.y);
		}
	    }, Coord.z);
    }

    public static class Scrollcont extends Widget {
	public int sy = 0;

	public Scrollcont(Coord sz) {
	    super(sz);
	}

	public void update() {}

	public <T extends Widget> T add(T child) {
	    super.add(child);
	    update();
	    return(child);
	}

	public Coord xlate(Coord c, boolean in) {
	    if(in)
		return(c.add(0, -sy));
	    else
		return(c.add(0, sy));
	}

	public void draw(GOut g) {
	    Widget next;
		
	    for(Widget wdg = child; wdg != null; wdg = next) {
		next = wdg.next;
		if(!wdg.visible)
		    continue;
		Coord cc = xlate(wdg.c, true);
		if((cc.y + wdg.sz.y < 0) || (cc.y > sz.y))
		    continue;
		wdg.draw(g.reclip(cc, wdg.sz));
	    }
	}
    }

    public boolean mousewheel(MouseWheelEvent ev) {
	bar.ch(ev.a * UI.scale(15));
	return(true);
    }

    public void addchild(Widget child, Object... args) {
	cont.addchild(child, args);
    }

    public void resize(Coord nsz) {
	super.resize(nsz);
	bar.c = new Coord(sz.x - bar.sz.x, 0);
	bar.resize(nsz.y);
	cont.resize(sz.sub(bar.sz.x, 0));
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "wpack") {
	    resize(new Coord(cont.contentsz().x + bar.sz.x, sz.y));
	} else {
	    super.uimsg(msg, args);
	}
    }
}
