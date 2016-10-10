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
    
    public ItemDrag(Coord dc, GItem item) {
	super(item);
	this.doff = dc;
    }

    protected void added() {
	this.c = parent.ui.mc.add(doff.inv());
	ui.grabmouse(this);
    }
    
    public void drawmain(GOut g, GSprite spr) {
	g.chcolor(255, 255, 255, 128);
	super.drawmain(g, spr);
	g.chcolor();
    }

    public void tick(double dt) {
	super.tick(dt);
	if(parent.child != this)
	    raise();
    }

    public boolean dropon(Widget w, Coord c) {
	if(w instanceof DTarget) {
	    if(((DTarget)w).drop(c, c.add(doff.inv())))
		return(true);
	}
	for(Widget wdg = w.lchild; wdg != null; wdg = wdg.prev) {
	    if((wdg == this) || !wdg.visible)
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
	    if((wdg == this) || !wdg.visible)
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
	if(ui.modctrl) {
	    /* XXX */
	    GameUI gui = getparent(GameUI.class);
	    if((gui != null) && (gui.map != null)) {
		ui.modctrl = false;
		return(gui.map.mousedown(gui.map.rootxlate(c.add(rootpos())), button));
	    }
	}
	if(button == 1) {
	    dropon(parent, c.add(this.c));
	    return(true);
	} else if(button == 3) {
	    interact(parent, c.add(this.c));
	    return(true);
	}
	return(false);
    }

    public void mousemove(Coord c) {
	this.c = this.c.add(c.add(doff.inv()));
    }
}
