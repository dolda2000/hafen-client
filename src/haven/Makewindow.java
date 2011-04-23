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
import java.awt.Font;

public class Makewindow extends Widget {
    Widget obtn, cbtn;
    List<Spec> inputs = Collections.emptyList();
    List<Spec> outputs = Collections.emptyList();
    static Coord boff = new Coord(7, 9);
    final int xoff = 40, yoff = 55;
    public static final Text.Foundry nmf = new Text.Foundry(new Font("Serif", Font.PLAIN, 20));

    static {
	Widget.addtype("make", new WidgetFactory() {
		public Widget create(Coord c, Widget parent, Object[] args) {
		    return(new Makewindow(c, parent, (String)args[0]));
		}
	    });
    }
    
    public static class Spec {
	public Indir<Resource> res;
	public int num;
	
	public Spec(Indir<Resource> res, int num) {
	    this.res = res;
	    this.num = num;
	}
    }
	
    public Makewindow(Coord c, Widget parent, String rcpnm) {
	super(c, Coord.z, parent);
	Label nm = new Label(new Coord(0, 0), this, rcpnm, nmf);
	nm.c = new Coord(sz.x - nm.sz.x, 0);
	new Label(new Coord(0, 8), this, "Input:");
	new Label(new Coord(0, 63), this, "Result:");
	obtn = new Button(new Coord(290, 71), 60, this, "Craft");
	cbtn = new Button(new Coord(360, 71), 60, this, "Craft All");
	pack();
    }
	
    public void uimsg(String msg, Object... args) {
	if(msg == "inpop") {
	    inputs = new LinkedList<Spec>();
	    for(int i = 0; i < args.length; i += 2)
		inputs.add(new Spec(ui.sess.getres((Integer)args[i]), (Integer)args[i + 1]));
	} else if(msg == "opop") {
	    outputs = new LinkedList<Spec>();
	    for(int i = 0; i < args.length; i += 2)
		outputs.add(new Spec(ui.sess.getres((Integer)args[i]), (Integer)args[i + 1]));
	}
    }
	
    public void draw(GOut g) {
	Coord c = new Coord(xoff, 0);
	for(Spec s : inputs) {
	    g.image(Inventory.invsq, c);
	    try {
		Resource res = s.res.get();
		g.image(res.layer(Resource.imgc).tex(), c);
	    } catch(Loading e) {
	    }
	    c = c.add(31, 0);
	}
	c = new Coord(xoff, yoff);
	for(Spec s : outputs) {
	    g.image(Inventory.invsq, c);
	    try {
		Resource res = s.res.get();
		g.image(res.layer(Resource.imgc).tex(), c);
	    } catch(Loading e) {
	    }
	    c = c.add(31, 0);
	}
	super.draw(g);
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
	if(sender == obtn) {
	    if(msg == "activate")
		wdgmsg("make", 0);
	    return;
	}
	if(sender == cbtn) {
	    if(msg == "activate")
		wdgmsg("make", 1);
	    return;
	}
	super.wdgmsg(sender, msg, args);
    }
    
    public boolean globtype(char ch, java.awt.event.KeyEvent ev) {
	if(ch == '\n') {
	    wdgmsg("make", ui.modctrl?1:0);
	    return(true);
	}
	return(super.globtype(ch, ev));
    }
}
