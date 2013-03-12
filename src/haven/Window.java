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
import java.awt.Font;
import java.awt.image.BufferedImage;

public class Window extends Widget implements DTarget {
    static Tex bg = Resource.loadtex("gfx/hud/bgtex");
    static Tex cl = Resource.loadtex("gfx/hud/cleft");
    static Tex cm = Resource.loadtex("gfx/hud/cmain");
    static Tex cr = Resource.loadtex("gfx/hud/cright");
    static BufferedImage[] cbtni = new BufferedImage[] {
	Resource.loadimg("gfx/hud/cbtn"),
	Resource.loadimg("gfx/hud/cbtnd"),
	Resource.loadimg("gfx/hud/cbtnh")}; 
    static Color cc = Color.YELLOW;
    static Text.Foundry cf = new Text.Foundry(new Font("Serif", Font.PLAIN, 12));
    static IBox wbox;
    boolean dt = false;
    Text cap;
    boolean dm = false;
    public Coord atl, asz, wsz;
    public Coord tlo, rbo;
    public Coord mrgn = new Coord(13, 13);
    public Coord doff;
    public IButton cbtn;
	
    @RName("wnd")
    public static class $_ implements Factory {
	public Widget create(Coord c, Widget parent, Object[] args) {
	    if(args.length < 2)
		return(new Window(c, (Coord)args[0], parent, null));
	    else
		return(new Window(c, (Coord)args[0], parent, (String)args[1]));
	}
    }

    static {
	wbox = new IBox("gfx/hud", "tl", "tr", "bl", "br", "extvl", "extvr", "extht", "exthb");
    }

    private void placecbtn() {
	cbtn.c = new Coord(wsz.x - 3 - Utils.imgsz(cbtni[0]).x, 3).add(mrgn.inv().add(wbox.tloff().inv()));
    }
	
    public Window(Coord c, Coord sz, Widget parent, String cap, Coord tlo, Coord rbo) {
	super(c, new Coord(0, 0), parent);
	this.tlo = tlo;
	this.rbo = rbo;
	cbtn = new IButton(Coord.z, this, cbtni[0], cbtni[1], cbtni[2]);
	if(cap != null)
	    this.cap = cf.render(cap, cc);
	sz = sz.add(tlo).add(rbo).add(wbox.bisz()).add(mrgn.mul(2));
	this.sz = sz;
	atl = new Coord(wbox.bl.sz().x, wbox.bt.sz().y).add(tlo);
	wsz = sz.add(tlo.inv()).add(rbo.inv());
	asz = new Coord(wsz.x - wbox.bl.sz().x - wbox.br.sz().x - mrgn.x, wsz.y - wbox.bt.sz().y - wbox.bb.sz().y - mrgn.y);
	placecbtn();
	setfocustab(true);
	parent.setfocus(this);
    }
	
    public Window(Coord c, Coord sz, Widget parent, String cap) {
	this(c, sz, parent, cap, new Coord(0, 0), new Coord(0, 0));
    }
	
    public void cdraw(GOut g) {
    }
	
    public void draw(GOut og) {
	GOut g = og.reclip(tlo, wsz);
	Coord bgc = new Coord();
	for(bgc.y = 3; bgc.y < wsz.y - 6; bgc.y += bg.sz().y) {
	    for(bgc.x = 3; bgc.x < wsz.x - 6; bgc.x += bg.sz().x)
		g.image(bg, bgc, new Coord(3, 3), wsz.add(new Coord(-6, -6)));
	}
	cdraw(og.reclip(xlate(Coord.z, true), sz));
	wbox.draw(g, Coord.z, wsz);
	if(cap != null) {
	    GOut cg = og.reclip(new Coord(0, -7), sz.add(0, 7));
	    int w = cap.tex().sz().x;
	    cg.image(cl, new Coord((sz.x / 2) - (w / 2) - cl.sz().x, 0));
	    cg.image(cm, new Coord((sz.x / 2) - (w / 2), 0), new Coord(w, cm.sz().y));
	    cg.image(cr, new Coord((sz.x / 2) + (w / 2), 0));
	    cg.image(cap.tex(), new Coord((sz.x / 2) - (w / 2), 0));
	}
	super.draw(og);
    }
	
    public Coord contentsz() {
	Coord max = new Coord(0, 0);
	for(Widget wdg = child; wdg != null; wdg = wdg.next) {
	    if(wdg == cbtn)
		continue;
	    if(!wdg.visible)
		continue;
	    Coord br = wdg.c.add(wdg.sz);
	    if(br.x > max.x)
		max.x = br.x;
	    if(br.y > max.y)
		max.y = br.y;
	}
	return(max);
    }

    public void resize(Coord sz) {
	this.sz = sz.add(wbox.bsz().add(mrgn.mul(2)).add(tlo).add(rbo)).add(-1, -1);
	wsz = this.sz.add(tlo.inv()).add(rbo.inv());
	asz = new Coord(wsz.x - wbox.bl.sz().x - wbox.br.sz().x, wsz.y - wbox.bt.sz().y - wbox.bb.sz().y).add(mrgn.mul(2).inv());
	placecbtn();
	for(Widget ch = child; ch != null; ch = ch.next)
	    ch.presize();
    }
	
    public void uimsg(String msg, Object... args) {
	if(msg == "pack") {
	    pack();
	} else if(msg == "dt") {
	    dt = (Integer)args[0] != 0;
	} else {
	    super.uimsg(msg, args);
	}
    }
	
    public Coord xlate(Coord c, boolean in) {
	Coord ctl = wbox.tloff();
	if(in)
	    return(c.add(ctl).add(tlo).add(mrgn));
	else
	    return(c.add(ctl.inv()).add(tlo.inv()).add(mrgn.inv()));
    }
	
    public boolean mousedown(Coord c, int button) {
	parent.setfocus(this);
	raise();
	if(super.mousedown(c, button))
	    return(true);
	if(!c.isect(tlo, sz.add(tlo.inv()).add(rbo.inv())))
	    return(false);
	if(button == 1) {
	    ui.grabmouse(this);
	    dm = true;
	    doff = c;
	}
	return(true);
    }
	
    public boolean mouseup(Coord c, int button) {
	if(dm) {
	    ui.grabmouse(null);
	    dm = false;
	} else {
	    super.mouseup(c, button);
	}
	return(true);
    }
	
    public void mousemove(Coord c) {
	if(dm) {
	    this.c = this.c.add(c.add(doff.inv()));
	} else {
	    super.mousemove(c);
	}
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
	if(sender == cbtn) {
	    wdgmsg("close");
	} else {
	    super.wdgmsg(sender, msg, args);
	}
    }
	
    public boolean type(char key, java.awt.event.KeyEvent ev) {
	if(super.type(key, ev))
	    return(true);
	if(key == 27) {
	    wdgmsg("close");
	    return(true);
	}
	return(false);
    }
	
    public boolean drop(Coord cc, Coord ul) {
	if(dt) {
	    wdgmsg("drop", cc);
	    return(true);
	}
	return(false);
    }
	
    public boolean iteminteract(Coord cc, Coord ul) {
	return(false);
    }
    
    public Object tooltip(Coord c, Widget prev) {
	Object ret = super.tooltip(c, prev);
	if(ret != null)
	    return(ret);
	else
	    return("");
    }
}
