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

public abstract class LayerMeter extends Widget implements ItemInfo.Owner {
    protected ItemInfo.Raw rawinfo = null;
    protected List<ItemInfo> info = Collections.emptyList();
    protected List<Meter> meters = Collections.emptyList();

    public LayerMeter(Coord sz) {
	super(sz);
    }

    public static class Meter {
	public final double a;
	public final Color c;
	
	public Meter(double a, Color c) {
	    this.a = a;
	    this.c = c;
	}
    }

    public void set(List<Meter> meters) {
	this.meters = meters;
    }

    public void set(double a, Color c) {
	set(Collections.singletonList(new Meter(a, c)));
    }

    private static double av(Object arg) {
	if(arg instanceof Integer)
	    return(((Integer)arg).doubleValue() * 0.01);
	else
	    return(Utils.dv(arg));
    }

    public static List<Meter> decmeters(Object[] args, int s) {
	if(args.length == s)
	    return(Collections.emptyList());
	ArrayList<Meter> buf = new ArrayList<>();
	for(int a = s; a < args.length; a += 2)
	    buf.add(new Meter(av(args[a]), (Color)args[a + 1]));
	buf.trimToSize();
	return(buf);
    }

    private static final OwnerContext.ClassResolver<LayerMeter> ctxr = new OwnerContext.ClassResolver<LayerMeter>()
	.add(LayerMeter.class, wdg -> wdg)
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
	} catch(Loading l) {
	    return("...");
	}
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "set") {
	    if(args.length == 1) {
		set(av(args[0]), meters.isEmpty() ? Color.WHITE : meters.get(0).c);
	    } else {
		set(decmeters(args, 0));
	    }
	} else if(msg == "col") {
	    set(meters.isEmpty() ? 0 : meters.get(0).a, (Color)args[0]);
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
