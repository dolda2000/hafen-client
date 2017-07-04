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

public class Tabs {
    public Widget parent;
    public Coord c, sz;
    public Tab curtab = null;
    public Collection<Tab> tabs = new LinkedList<Tab>();

    public Tabs(Coord c, Coord sz, Widget parent) {
	this.c = c;
	this.sz = sz;
	this.parent = parent;
    }

    public class Tab extends Widget {
	public TabButton btn;
	
	public Tab() {
	    super(Tabs.this.sz);
	    if(curtab == null)
		curtab = this;
	    else
		hide();
	    tabs.add(this);
	}

	public void destroy() {
	    super.destroy();
	    tabs.remove(this);
	}

	public void showtab() {
	    Tabs.this.showtab(this);
	}
    }

    public Tab add() {
	return(parent.add(new Tab(), c));
    }

    public class TabButton extends Button {
	public final Tab tab;

	public TabButton(int w, String text, Tab tab) {
	    super(w, text, false);
	    this.tab = tab;
	}

	public void click() {
	    showtab(tab);
	}
    }

    public void showtab(Tab tab) {
	Tab old = curtab;
	if(old != null)
	    old.hide();
	if((curtab = tab) != null)
	    curtab.show();
	changed(old, tab);
    }

    public void resize(Coord sz) {
	for(Tab tab : tabs)
	    tab.resize(sz);
	this.sz = sz;
    }

    public Coord contentsz() {
	Coord max = new Coord(0, 0);
	for(Tab tab : tabs) {
	    Coord br = tab.contentsz();
	    if(br.x > max.x) max.x = br.x;
	    if(br.y > max.y) max.y = br.y;
	}
	return(max);
    }

    public void pack() {
	resize(contentsz());
    }

    public void indpack() {
	for(Tab tab : tabs)
	    tab.pack();
	this.sz = contentsz();
    }

    public void changed(Tab from, Tab to) {}
}
