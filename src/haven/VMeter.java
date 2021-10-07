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
import java.util.*;

public class VMeter extends Widget implements ItemInfo.Owner {
    public static final Tex bg = Resource.loadtex("gfx/hud/vm-frame");
    public static final Tex fg = Resource.loadtex("gfx/hud/vm-tex");
    private Color cl;
    private int amount;
    private ItemInfo.Raw rawinfo = null;
    private List<ItemInfo> info = Collections.emptyList();

    @RName("vm")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    Color cl;
	    if(args.length > 4) {
		cl = new Color((Integer)args[1],
			       (Integer)args[2],
			       (Integer)args[3],
			       (Integer)args[4]);
	    } else if(args.length > 3) {
		cl = new Color((Integer)args[1],
			       (Integer)args[2],
			       (Integer)args[3]);
	    } else {
		cl = (Color)args[1];
	    }
	    return(new VMeter((Integer)args[0], cl));
	}
    }

    public VMeter(int amount, Color cl) {
	super(bg.sz());
	this.amount = amount;
	this.cl = cl;
    }

    public void draw(GOut g) {
	g.image(bg, Coord.z);
	g.chcolor(cl);
	int h = (sz.y - UI.scale(6));
	h = (h * amount) / 100;
	g.image(fg, new Coord(0, 0), new Coord(0, sz.y - UI.scale(3) - h), sz.add(0, h));
    }

    private static final OwnerContext.ClassResolver<VMeter> ctxr = new OwnerContext.ClassResolver<VMeter>()
	.add(Glob.class, wdg -> wdg.ui.sess.glob)
	.add(Session.class, wdg -> wdg.ui.sess);
    public <T> T context(Class<T> cl) {return(ctxr.context(cl, this));}

    public List<ItemInfo> info() {
	if(info == null)
	    info = ItemInfo.buildinfo(this, rawinfo);
	return(info);
    }

    private double hoverstart;
    private Tex shorttip, longtip;
    public Object tooltip(Coord c, Widget prev) {
	if(rawinfo == null)
	    return(super.tooltip(c, prev));
	double now = Utils.rtime();
	if(prev != this)
	    hoverstart = now;
	try {
	    if(now - hoverstart < 1.0) {
		if(shorttip == null)
		    shorttip = new TexI(ItemInfo.shorttip(info()));
		return(shorttip);
	    } else {
		if(longtip == null)
		    longtip = new TexI(ItemInfo.longtip(info()));
		return(longtip);
	    }
	} catch(Loading e) {
	    return("...");
	}
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "set") {
	    amount = (Integer)args[0];
	    if(args.length > 1)
		cl = (Color)args[1];
	} else if(msg == "col") {
	    cl = (Color)args[0];
	} else if(msg == "tip") {
	    if(args[0] instanceof Object[]) {
		rawinfo = new ItemInfo.Raw((Object[])args[0]);
		info = null;
		shorttip = longtip = null;
	    } else {
		super.uimsg(msg, args);
	    }
	} else {
	    super.uimsg(msg, args);
	}
    }
}
