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
    public static final Tex bg = Resource.loadtex("gfx/hud/wnd/bgtex");
    public static final Tex cl = Resource.loadtex("gfx/hud/wnd/cleft");
    public static final Tex cm = Resource.loadtex("gfx/hud/wnd/cmain");
    public static final Tex cr = Resource.loadtex("gfx/hud/wnd/cright");
    public static final int capo = 7, capio = 2;
    public static final Coord mrgn = new Coord(13, 13);
    public static final Color cc = Color.YELLOW;
    public static final Text.Foundry cf = new Text.Foundry(Text.serif, 12);
    public static final IBox wbox = new IBox("gfx/hud/wnd", "tl", "tr", "bl", "br", "extvl", "extvr", "extht", "exthb") {
	    final Coord co = new Coord(3, 3), bo = new Coord(2, 2);

	    public Coord btloff() {return(super.btloff().sub(bo));}
	    public Coord ctloff() {return(super.ctloff().sub(co));}
	    public Coord bisz() {return(super.bisz().sub(bo.mul(2)));}
	    public Coord cisz() {return(super.cisz().sub(co.mul(2)));}
	};
    private static final BufferedImage[] cbtni = new BufferedImage[] {
	Resource.loadimg("gfx/hud/wnd/cbtn"),
	Resource.loadimg("gfx/hud/wnd/cbtnd"),
	Resource.loadimg("gfx/hud/wnd/cbtnh")};
    public final Coord tlo, rbo;
    public final IButton cbtn;
    public boolean dt = false;
    public Text cap;
    public Coord wtl, wsz, ctl, csz, atl, asz;
    private UI.Grab dm = null;
    private Coord doff;

    @RName("wnd")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    if(args.length < 2)
		return(new Window((Coord)args[0], null));
	    else
		return(new Window((Coord)args[0], (String)args[1]));
	}
    }

    public Window(Coord sz, String cap, Coord tlo, Coord rbo) {
	this.tlo = tlo;
	this.rbo = rbo;
	cbtn = add(new IButton(cbtni[0], cbtni[1], cbtni[2]));
	chcap(cap);
	resize(sz);
	setfocustab(true);
    }

    public Window(Coord sz, String cap) {
	this(sz, cap, new Coord(0, 0), new Coord(0, 0));
    }

    protected void added() {
	parent.setfocus(this);
    }

    public void chcap(String cap) {
	if(cap == null)
	    this.cap = null;
	else
	    this.cap = cf.render(cap, cc);
    }

    public void cdraw(GOut g) {
    }

    public void draw(GOut g) {
	Coord bgc = new Coord();
	for(bgc.y = ctl.y; bgc.y < ctl.y + csz.y; bgc.y += bg.sz().y) {
	    for(bgc.x = ctl.x; bgc.x < ctl.x + csz.x; bgc.x += bg.sz().x)
		g.image(bg, bgc, ctl, csz);
	}
	cdraw(g.reclip(atl, asz));
	wbox.draw(g, wtl, wsz);
	if(cap != null) {
	    int w = cap.sz().x;
	    int y = wtl.y - capo;
	    g.image(cl, new Coord(wtl.x + (wsz.x / 2) - (w / 2) - cl.sz().x, y));
	    g.image(cm, new Coord(wtl.x + (wsz.x / 2) - (w / 2), y), new Coord(w, cm.sz().y));
	    g.image(cr, new Coord(wtl.x + (wsz.x / 2) + (w / 2), y));
	    g.image(cap.tex(), new Coord(wtl.x + (wsz.x / 2) - (w / 2), y + capio));
	}
	super.draw(g);
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

    private void placecbtn() {
	cbtn.c = xlate(new Coord(ctl.x + csz.x - cbtn.sz.x, ctl.y).add(2, -2), false);
    }

    public void resize(Coord sz) {
	asz = sz;
	csz = asz.add(mrgn.mul(2));
	wsz = csz.add(wbox.bisz());
	wtl = new Coord(tlo.x, Math.max(tlo.y, capo));
	this.sz = wsz.add(wtl).add(rbo);
	ctl = wtl.add(wbox.btloff());
	atl = ctl.add(mrgn);
	placecbtn();
	for(Widget ch = child; ch != null; ch = ch.next)
	    ch.presize();
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "pack") {
	    pack();
	} else if(msg == "dt") {
	    dt = (Integer)args[0] != 0;
	} else if(msg == "cap") {
	    String cap = (String)args[0];
	    chcap(cap.equals("")?null:cap);
	} else {
	    super.uimsg(msg, args);
	}
    }

    public Coord xlate(Coord c, boolean in) {
	if(in)
	    return(c.add(atl));
	else
	    return(c.sub(atl));
    }

    public boolean mousedown(Coord c, int button) {
	parent.setfocus(this);
	raise();
	if(super.mousedown(c, button))
	    return(true);
	if(c.isect(wtl, wsz) || ((cap != null) && c.isect(wtl.add((wsz.x / 2) - (cap.sz().x / 2), -capo), new Coord(cap.sz().x, capo)))) {
	    if(button == 1) {
		dm = ui.grabmouse(this);
		doff = c;
	    }
	    return(true);
	}
	return(true);
    }

    public boolean mouseup(Coord c, int button) {
	if(dm != null) {
	    dm.remove();
	    dm = null;
	} else {
	    super.mouseup(c, button);
	}
	return(true);
    }

    public void mousemove(Coord c) {
	if(dm != null) {
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

    public boolean keydown(java.awt.event.KeyEvent ev) {
	if(super.keydown(ev))
	    return(true);
	if(ev.getKeyChar() == 27) {
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
