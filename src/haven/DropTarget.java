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

public interface DropTarget {
    public default boolean dropthing(Coord cc, Object thing) {return(false);}
    public default boolean drophover(Coord cc, boolean hovering, Object thing) {return(false);}

    public default boolean dropthing(Drop ev) {
	return(dropthing(ev.c, ev.thing));
    }
    public default boolean drophover(Hover ev) {
	if(drophover(ev.c, ev.hovering, ev.thing))
	    return(ev.accept(this));
	return(false);
    }

    public static abstract class DropEvent extends Widget.MouseEvent {
	public final Object thing;

	public DropEvent(Coord c, Object thing) {
	    super(c);
	    this.thing = thing;
	}
	public DropEvent(DropEvent from, Coord c) {
	    super(from, c);
	    this.thing = from.thing;
	}
    }

    public static class Drop extends DropEvent {
	public Drop(Coord c, Object thing) {super(c, thing);}
	public Drop(Drop from, Coord c) {super(from, c);}
	public Drop derive(Coord c) {return(new Drop(this, c));}

	protected boolean shandle(Widget w) {
	    if((w instanceof DropTarget) && ((DropTarget)w).dropthing(this))
		return(true);
	    return(super.shandle(w));
	}
    }

    public static class Hover extends DropEvent {
	public final Hover root;
	public boolean hovering;
	public DropTarget tgt;

	public Hover(Coord c, Object thing) {
	    super(c, thing);
	    hovering = true;
	    root = this;
	}
	public Hover(Hover from, Coord c) {
	    super(from, c);
	    root = from.root;
	}
	public Hover derive(Coord c) {return(new Hover(this, c));}

	public Hover hovering(boolean h) {hovering = h; return(this);}

	public boolean accept(DropTarget tgt) {
	    root.tgt = tgt;
	    return(true);
	}

	protected boolean propagation(Widget from) {
	    boolean ret = false;
	    boolean hovering = this.hovering;
	    for(Widget wdg = from.lchild; wdg != null; wdg = wdg.prev) {
		Coord cc = from.xlate(wdg.c, true);
		boolean inside = c.isect(cc, wdg.sz);
		boolean ch = hovering && inside && wdg.visible();
		if(derive(c.sub(cc)).hovering(ch).dispatch(wdg) && ch) {
		    hovering = false;
		    ret = true;
		}
	    }
	    return(ret);
	}

	protected boolean shandle(Widget w) {
	    if((w instanceof DropTarget) && ((DropTarget)w).drophover(this))
		return(true);
	    return(super.shandle(w));
	}
    }

    public static boolean dropthing(Widget wdg, Coord c, Object thing) {
	return(wdg.ui.dispatch(wdg, new Drop(c, thing)));
    }

    public static boolean drophover(Widget wdg, Coord c, Object thing) {
	Hover h = new Hover(c, thing);
	wdg.ui.dispatch(wdg, h);
	return(h.tgt != null);
    }
}
