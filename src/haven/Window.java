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
import static haven.PUtils.*;

public class Window extends Widget implements DTarget {
    public static final Tex bg = Resource.loadtex("gfx/hud/wnd/lg/bg");
    public static Coord wtl = new Coord(0, 9);
    public static final Coord tlm = new Coord(1, 30), brm = new Coord(1, 1), cpo = new Coord(10, 1);
    public static final Coord dlmrgn = new Coord(23, 14), dsmrgn = new Coord(9, 9);
    public static final BufferedImage ctex = Resource.loadimg("gfx/hud/chantex");
    public static final Text.Furnace cf = new Text.Foundry(Text.fraktur, 15, new Color(244, 216, 142)).aa(true);
    public static final IBox wbox = new IBox("gfx/hud/wnd", "tl", "tr", "bl", "br", "extvl", "extvr", "extht", "exthb") {
	    final Coord co = new Coord(3, 3), bo = new Coord(2, 2);

	    public Coord btloff() {return(super.btloff().sub(bo));}
	    public Coord ctloff() {return(super.ctloff().sub(co));}
	    public Coord bisz() {return(super.bisz().sub(bo.mul(2)));}
	    public Coord cisz() {return(super.cisz().sub(co.mul(2)));}
	};
    public static final IBox cbox = new IBox("gfx/hud/wnd/custom", "tl", "tr", "bl", "br", "extvl", "extvr", "extht", "exthb") {
        final Coord co = new Coord(10, 21), bo = new Coord(1, 1);

        public Coord btloff() {return(super.btloff().sub(bo));}
        public Coord ctloff() {return(super.ctloff().sub(co));}
        public Coord bisz() {return(super.bisz().sub(bo.mul(2)));}
        public Coord cisz() {return(super.cisz().sub(co.mul(2)));}
    };
    private static final BufferedImage[] cbtni = new BufferedImage[] {
	Resource.loadimg("gfx/hud/wnd/custom/cbtnu"),
	Resource.loadimg("gfx/hud/wnd/custom/cbtnd"),
	Resource.loadimg("gfx/hud/wnd/custom/cbtnh")};
    public final Coord tlo, rbo, mrgn;
    public final IButton cbtn;
    public boolean dt = false;
    public Text cap;
    public Coord wsz, ctl, csz, atl, asz;
    private UI.Grab dm = null;
    private Coord doff;

    @RName("wnd")
    public static class $_ implements Factory {
	public Widget create(Widget parent, Object[] args) {
	    Coord sz = (Coord)args[0];
	    String cap = (args.length > 1)?(String)args[1]:null;
	    boolean lg = (args.length > 2)?((Integer)args[2] != 0):false;
	    return(new Window(sz, cap, lg, Coord.z, Coord.z));
	}
    }

    public Window(Coord sz, String cap, boolean lg, Coord tlo, Coord rbo) {
	this.tlo = tlo;
	this.rbo = rbo;
	this.mrgn = lg?dlmrgn:dsmrgn;
	cbtn = add(new IButton(cbtni[0], cbtni[1], cbtni[2], false));
	chcap(cap);
	resize(sz);
	setfocustab(true);
    }

    public Window(Coord sz, String cap, boolean lg) {
	this(sz, cap, lg, Coord.z, Coord.z);
    }

    public Window(Coord sz, String cap) {
	this(sz, cap, false);
    }

    protected void added() {
	parent.setfocus(this);
    }

    public void chcap(String cap) {
	if(cap == null)
	    this.cap = null;
	else
	    this.cap = cf.render(cap);
    }

    public String caption() {
	return (cap != null) ? cap.text : null;
    }

    public void cdraw(GOut g) {
    }

    public void draw(GOut g) {
	Coord bgc = new Coord();
	for(bgc.y = ctl.y; bgc.y < ctl.y + csz.y; bgc.y += bg.sz().y) {
	    for(bgc.x = ctl.x; bgc.x < ctl.x + csz.x; bgc.x += bg.sz().x)
		g.image(bg, bgc, ctl, csz);
	}
	bgc.x = ctl.x;
	cdraw(g.reclip(atl, asz));

    cbox.draw(g, wtl, wsz.sub(wtl));
	if(cap != null) {
	    g.image(cap.tex(), wtl.add(cpo));
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
	wsz = csz.add(tlm).add(brm);
	this.sz = wsz.add(tlo).add(rbo);
	ctl = tlo.add(tlm);
	atl = ctl.add(mrgn);
	cbtn.c = xlate(tlo.add(wsz.x - cbtn.sz.x - 3, wtl.y + 4), false);
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
	if(super.mousedown(c, button)) {
	    parent.setfocus(this);
	    raise();
	    return(true);
	}
	if(c.isect(ctl, csz) || c.isect(wtl, wsz.sub(wtl))) {
	    if(button == 1) {
		dm = ui.grabmouse(this);
		doff = c;
	    }
	    parent.setfocus(this);
	    raise();
	    return(true);
	}
	return(false);
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

	public boolean isGrabbed() {
		return dm != null;
	}
}
