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
    private static final Tex bg = Resource.loadtex("gfx/hud/equip/bg");
    private static final int rx = 34 + bg.sz().x;
    static Coord ecoords[] = {
	new Coord(0, 0),
	new Coord(rx, 0),
	new Coord(0, 33),
	new Coord(rx, 33),
	new Coord(0, 66),
	new Coord(rx, 66),
	new Coord(0, 99),
	new Coord(rx, 99),
	new Coord(0, 132),
	new Coord(rx, 132),
	new Coord(0, 165),
	new Coord(rx, 165),
	new Coord(0, 198),
	new Coord(rx, 198),
	new Coord(0, 231),
	new Coord(rx, 231),
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
	
    @RName("epry")
    public static class $_ implements Factory {
	public Widget create(Coord c, Widget parent, Object[] args) {
	    long gobid;
	    if(args.length < 1)
		gobid = parent.getparent(GameUI.class).plid;
	    else
		gobid = (Integer)args[0];
	    return(new Equipory(c, parent, gobid));
	}
    }
	
    public Equipory(Coord c, Widget parent, long gobid) {
	super(c, isz, parent);
	Avaview ava = new Avaview(new Coord(34, 0), bg.sz(), this, gobid, "equcam") {
		public boolean mousedown(Coord c, int button) {
		    return(false);
		}

		public void draw(GOut g) {
		    g.image(bg, Coord.z);
		    super.draw(g);
		}

		Outlines outlines = new Outlines(true);
		protected void setup(RenderList rl) {
		    super.setup(rl);
		    rl.add(outlines, null);
		}

		protected java.awt.Color clearcolor() {return(null);}
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
