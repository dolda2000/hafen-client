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

public class IMeter extends Widget {
    public static final Coord off = UI.scale(22, 7);
    public static final Coord fsz = UI.scale(101, 24);
    public static final Coord msz = UI.scale(75, 10);
    public final Indir<Resource> bg;
    public List<Meter> meters;
    
    @RName("im")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    Indir<Resource> bg = ui.sess.getres((Integer)args[0]);
	    List<Meter> meters = decmeters(args, 1);
	    return(new IMeter(bg, meters));
	}
    }
    
    public IMeter(Indir<Resource> bg, List<Meter> meters) {
	super(fsz);
	this.bg = bg;
	this.meters = meters;
    }
    
    public static class Meter {
	public final Color c;
	public final double a;
	
	public Meter(Color c, double a) {
	    this.c = c;
	    this.a = a;
	}
    }
    
    public void draw(GOut g) {
	try {
	    Tex bg = this.bg.get().layer(Resource.imgc).tex();
	    g.chcolor(0, 0, 0, 255);
	    g.frect(off, msz);
	    g.chcolor();
	    for(Meter m : meters) {
		int w = msz.x;
		w = (int)Math.ceil(w * m.a);
		g.chcolor(m.c);
		g.frect(off, new Coord(w, msz.y));
	    }
	    g.chcolor();
	    g.image(bg, Coord.z);
	} catch(Loading l) {
	}
    }
    
    private static List<Meter> decmeters(Object[] args, int s) {
	ArrayList<Meter> buf = new ArrayList<>();
	for(int a = s; a < args.length; a += 2)
	    buf.add(new Meter((Color)args[a], ((Number)args[a + 1]).doubleValue() * 0.01));
	buf.trimToSize();
	return(buf);
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "set") {
	    this.meters = decmeters(args, 0);
	} else {
	    super.uimsg(msg, args);
	}
    }
}
