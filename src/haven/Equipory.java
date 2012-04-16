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
import static haven.Inventory.invsq;

public class Equipory extends Widget implements DTarget {
    static Coord ecoords[] = {
	new Coord(0, 0),
	new Coord(299, 0),
	new Coord(0, 33),
	new Coord(299, 33),
	new Coord(0, 66),
	new Coord(299, 66),
	new Coord(0, 99),
	new Coord(299, 99),
	new Coord(0, 132),
	new Coord(299, 132),
	new Coord(0, 165),
	new Coord(299, 165),
	new Coord(0, 198),
	new Coord(299, 198),
	new Coord(0, 231),
	new Coord(299, 231),
    };
    static Coord isz;
    static {
	isz = new Coord();
	for(Coord ec : ecoords) {
	    if(ec.x + invsq.sz().x > isz.x)
		isz.x = ec.x + invsq.sz().x;
	    if(ec.y + invsq.sz().y > isz.y)
		isz.y = ec.y + invsq.sz().y;
	}
    }
    Map<GItem, WItem[]> wmap = new HashMap<GItem, WItem[]>();
	
    static {
	Widget.addtype("epry", new WidgetFactory() {
		public Widget create(Coord c, Widget parent, Object[] args) {
		    return(new Equipory(c, parent));
		}
	    });
    }
	
    public Equipory(Coord c, Widget parent) {
	super(c, isz, parent);
	Avaview ava = new Avaview(new Coord(34, 0), new Coord(265, 265), this, getparent(GameUI.class).plid, "avacam") {
		public boolean mousedown(Coord c, int button) {
		    return(false);
		}
	    };
	ava.color = null;
    }
	
    public Widget makechild(String type, Object[] pargs, Object[] cargs) {
	Widget ret = gettype(type).create(Coord.z, this, cargs);
	if(ret instanceof GItem) {
	    GItem g = (GItem)ret;
	    WItem[] v = new WItem[pargs.length];
	    for(int i = 0; i < pargs.length; i++) {
		int ep = (Integer)pargs[i];
		v[i] = new WItem(ecoords[ep].add(1, 1), this, g);
	    }
	    wmap.put(g, v);
	}
	return(ret);
    }
    
    public void cdestroy(Widget w) {
	super.cdestroy(w);
	if(w instanceof GItem) {
	    GItem i = (GItem)w;
	    for(WItem v : wmap.remove(i))
		ui.destroy(v);
	}
    }
    
    public boolean drop(Coord cc, Coord ul) {
	ul = ul.add(invsq.sz().div(2));
	for(int i = 0; i < ecoords.length; i++) {
	    if(ul.isect(ecoords[i], invsq.sz())) {
		wdgmsg("drop", i);
		return(true);
	    }
	}
	wdgmsg("drop", -1);
	return(true);
    }
    
    public void draw(GOut g) {
	for(Coord ec : ecoords)
	    g.image(invsq, ec);
	super.draw(g);
    }
	
    public boolean iteminteract(Coord cc, Coord ul) {
	return(false);
    }
}
