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

public abstract class PackCont extends Widget {
    public boolean packpar;
    private boolean packed = false;

    public PackCont() {
	super(Coord.z);
    }

    public PackCont packpar(boolean packpar) {this.packpar = packpar; return(this);}

    public <T extends Widget> T add(T child) {
	T ret = super.add(child);
	repack0();
	return(ret);
    }

    public void cdestroy(Widget ch) {
	super.cdestroy(ch);
	repack0();
    }

    public void cresize(Widget ch) {
	super.cresize(ch);
	repack0();
    }

    public void pack() {
	if(!packed) {
	    packed = true;
	    repack();
	}
	super.pack();
	if(packpar && (parent != null))
	    parent.pack();
    }

    private void repack0() {
	if(packed) {
	    repack();
	    pack();
	}
    }

    protected abstract void repack();

    public abstract static class LinPack extends PackCont {
	public final List<Widget> order = new ArrayList<>();
	public int margin = 0;

	public LinPack margin(int margin) {this.margin = margin; return(this);}

	protected abstract Coord pad(int p);

	public <T extends Widget> T last(T child, int pad) {
	    order.add(child);
	    return(add(child, pad(pad)));
	}

	public <T extends Widget> T insert(T child, int p, int pad) {
	    order.add(p, child);
	    return(add(child, pad(pad)));
	}

	public <T extends Widget> T after(T child, Widget after, int pad) {
	    int p = order.indexOf(after);
	    if(p < 0)
		throw(new IllegalArgumentException(after + " is not currently ordered"));
	    return(insert(child, p + 1, pad));
	}

	public <T extends Widget> T before(T child, Widget after, int pad) {
	    int p = order.indexOf(after);
	    if(p < 0)
		throw(new IllegalArgumentException(after + " is not currently ordered"));
	    return(insert(child, p, pad));
	}

	public void cdestroy(Widget ch) {
	    order.remove(ch);
	    super.cdestroy(ch);
	}

	public void addchild(Widget child, Object... args) {
	    if(args.length == 0) {
		last(child, 0);
	    } else if(args[0] instanceof Integer) {
		switch(Utils.iv(args[0])) {
		case (int)'l': last(child, Utils.iv(args[1])); break;
		case (int)'i': insert(child, Utils.iv(args[1]), Utils.iv(args[2])); break;
		case (int)'a': after(child, ui.getwidget(Utils.iv(args[1])), Utils.iv(args[2])); break;
		case (int)'b': before(child, ui.getwidget(Utils.iv(args[1])), Utils.iv(args[2])); break;
		default:
		    throw(new UI.UIException("Unknown child widget creation specification.", null, args));
		}
	    } else {
		super.addchild(child, args);
	    }
	}

	public static class VPack extends LinPack {
	    protected Coord pad(int p) {return(Coord.of(p, 0));}

	    protected void repack() {
		int y = 0;
		for(Widget ch : order) {
		    ch.move(Coord.of(ch.c.x, y));
		    y += ch.sz.y + margin;
		}
	    }
	}

	public static class HPack extends LinPack {
	    protected Coord pad(int p) {return(Coord.of(0, p));}

	    protected void repack() {
		int x = 0;
		for(Widget ch : order) {
		    ch.move(Coord.of(x, ch.c.y));
		    x += ch.sz.x + margin;
		}
	    }
	}

	@RName("linpack")
	public static class $linpack implements Factory {
	    public Widget create(UI ui, Object[] args) {
		LinPack ret = Utils.bv(args[0]) ? new HPack() : new VPack();
		if(args.length > 1)
		    ret.margin(UI.scale(Utils.iv(args[1])));
		return(ret);
	    }
	}
    }
}
