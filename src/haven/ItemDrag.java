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

public class ItemDrag extends WItem {
    public Coord doff;
    
    public ItemDrag(Coord dc, Widget parent, GItem item) {
	super(parent.ui.mc.add(dc.inv()), parent, item);
	this.doff = dc;
	ui.grabmouse(this);
    }
    
    public void drawmain(GOut g, Tex tex) {
	g.chcolor(255, 255, 255, 128);
	g.image(tex, Coord.z);
	g.chcolor();
    }

    public boolean dropon(Widget w, Coord c) {
	if(w instanceof DTarget) {
	    if(((DTarget)w).drop(c, c.add(doff.inv())))
		return(true);
	}
	for(Widget wdg = w.lchild; wdg != null; wdg = wdg.prev) {
	    if(wdg == this)
		continue;
	    Coord cc = w.xlate(wdg.c, true);
	    if(c.isect(cc, wdg.sz)) {
		if(dropon(wdg, c.add(cc.inv())))
		    return(true);
	    }
	}
	return(false);
    }
	
    public boolean interact(Widget w, Coord c) {
	if(w instanceof DTarget) {
	    if(((DTarget)w).iteminteract(c, c.add(doff.inv())))
		return(true);
	}
	for(Widget wdg = w.lchild; wdg != null; wdg = wdg.prev) {
	    if(wdg == this)
		continue;
	    Coord cc = w.xlate(wdg.c, true);
	    if(c.isect(cc, wdg.sz)) {
		if(interact(wdg, c.add(cc.inv())))
		    return(true);
	    }
	}
	return(false);
    }
	
    public boolean mousedown(Coord c, int button) {
	if(button == 1) {
	    dropon(parent, c.add(this.c));
	} else if(button == 3) {
	    interact(parent, c.add(this.c));
	}
	return(false);
    }

    public void mousemove(Coord c) {
	this.c = this.c.add(c.add(doff.inv()));
    }
}
