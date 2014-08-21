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
import java.awt.Color;

public class Makewindow extends Widget {
    Widget obtn, cbtn;
    List<Spec> inputs = Collections.emptyList();
    List<Spec> outputs = Collections.emptyList();
    static Coord boff = new Coord(7, 9);
    final int xoff = 40, yoff = 55;
    public static final Text.Foundry nmf = new Text.Foundry(Text.serif, 20);

    @RName("make")
    public static class $_ implements Factory {
	public Widget create(Coord c, Widget parent, Object[] args) {
	    return(new Makewindow(c, parent, (String)args[0]));
	}
    }
    
    public class Spec implements GSprite.Owner {
	public Indir<Resource> res;
	public Message sdt;
	public Tex num;
	private GSprite spr;

	public Spec(Indir<Resource> res, Message sdt, int num) {
	    this.res = res;
	    this.sdt = sdt;
	    if(num >= 0)
		this.num = new TexI(Utils.outline2(Text.render(Integer.toString(num), Color.WHITE).img, Utils.contrast(Color.WHITE)));
	    else
		this.num = null;
	}

	public void draw(GOut g) {
	    try {
		if(spr == null)
		    spr = GSprite.create(this, res.get(), sdt);
		spr.draw(g);
	    } catch(Loading e) {}
	    if(num != null)
		g.aimage(num, Inventory.sqsz, 1.0, 1.0);
	}

	private Random rnd = null;
	public Random mkrandoom() {
	    if(rnd == null)
		rnd = new Random();
	    return(rnd);
	}
	public Resource getres() {return(res.get());}
	public Glob glob() {return(ui.sess.glob);}
    }
	
    public Makewindow(Coord c, Widget parent, String rcpnm) {
	super(c, Coord.z, parent);
	Label nm = new Label(new Coord(0, 0), this, rcpnm, nmf);
	nm.c = new Coord(sz.x - nm.sz.x, 0);
	new Label(new Coord(0, 8), this, "Input:");
	new Label(new Coord(0, 63), this, "Result:");
	obtn = new Button(new Coord(265, 71), 85, this, "Craft");
	cbtn = new Button(new Coord(360, 71), 85, this, "Craft All");
	pack();
    }
	
    public void uimsg(String msg, Object... args) {
	if(msg == "inpop") {
	    List<Spec> inputs = new LinkedList<Spec>();
	    for(int i = 0; i < args.length;) {
		int resid = (Integer)args[i++];
		Message sdt = (args[i] instanceof byte[])?new Message(0, (byte[])args[i++]):Message.nil;
		int num = (Integer)args[i++];
		inputs.add(new Spec(ui.sess.getres(resid), sdt, num));
	    }
	    this.inputs = inputs;
	} else if(msg == "opop") {
	    List<Spec> outputs = new LinkedList<Spec>();
	    for(int i = 0; i < args.length;) {
		int resid = (Integer)args[i++];
		Message sdt = (args[i] instanceof byte[])?new Message(0, (byte[])args[i++]):Message.nil;
		int num = (Integer)args[i++];
		outputs.add(new Spec(ui.sess.getres(resid), sdt, num));
	    }
	    this.outputs = outputs;
	}
    }
	
    public void draw(GOut g) {
	Coord c = new Coord(xoff, 0);
	for(Spec s : inputs) {
	    GOut sg = g.reclip(c, Inventory.invsq.sz());
	    sg.image(Inventory.invsq, Coord.z);
	    s.draw(sg);
	    c = c.add(Inventory.sqsz.x, 0);
	}
	c = new Coord(xoff, yoff);
	for(Spec s : outputs) {
	    GOut sg = g.reclip(c, Inventory.invsq.sz());
	    sg.image(Inventory.invsq, Coord.z);
	    s.draw(sg);
	    c = c.add(Inventory.sqsz.x, 0);
	}
	super.draw(g);
    }
    
    private long hoverstart;
    private Resource lasttip;
    private Object stip, ltip;
    public Object tooltip(Coord mc, Widget prev) {
	Resource tres = null;
	Coord c = new Coord(xoff, 0);
	find: {
	    for(Spec s : inputs) {
		if(mc.isect(c, Inventory.invsq.sz())) {
		    tres = s.res.get();
		    break find;
		}
		c = c.add(31, 0);
	    }
	    c = new Coord(xoff, yoff);
	    for(Spec s : outputs) {
		if(mc.isect(c, Inventory.invsq.sz())) {
		    tres = s.res.get();
		    break find;
		}
		c = c.add(31, 0);
	    }
	}
	if(tres == null)
	    return(null);
	if(lasttip != tres) {
	    lasttip = tres;
	    stip = ltip = null;
	}
	long now = System.currentTimeMillis();
	boolean sh = true;
	if(prev != this)
	    hoverstart = now;
	else if(now - hoverstart > 1000)
	    sh = false;
	if(sh) {
	    if(stip == null)
		stip = Text.render(tres.layer(Resource.tooltip).t);
	    return(stip);
	} else {
	    if(ltip == null) {
		String t = tres.layer(Resource.tooltip).t;
		Resource.Pagina p = tres.layer(Resource.pagina);
		if(p != null)
		    t += "\n\n" + tres.layer(Resource.pagina).text;
		ltip = RichText.render(t, 300);
	    }
	    return(ltip);
	}
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
    
    public static class MakePrep extends ItemInfo implements GItem.ColorInfo {
	private final static Color olcol = new Color(0, 255, 0, 64);
	public MakePrep(Owner owner) {
	    super(owner);
	}
	
	public Color olcol() {
	    return(olcol);
	}
    }
}
