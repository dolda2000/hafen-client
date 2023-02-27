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
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.function.*;
import haven.ItemInfo.AttrCache;
import static haven.ItemInfo.find;
import static haven.Inventory.sqsz;

public class WItem extends Widget implements DTarget {
    public static final Resource missing = Resource.local().loadwait("gfx/invobjs/missing");
    public final GItem item;
    public Contents contents;
    public Window contentswnd;
    private boolean hovering = false;
    private Resource cspr = null;
    private Message csdt = Message.nil;

    public WItem(GItem item) {
	super(sqsz);
	this.item = item;
    }

    public void drawmain(GOut g, GSprite spr) {
	spr.draw(g);
    }

    public static BufferedImage shorttip(List<ItemInfo> info) {
	return(ItemInfo.shorttip(info));
    }

    public static BufferedImage longtip(GItem item, List<ItemInfo> info) {
	BufferedImage img = ItemInfo.longtip(info);
	Resource.Pagina pg = item.res.get().layer(Resource.pagina);
	if(pg != null)
	    img = ItemInfo.catimgs(0, img, RichText.render("\n" + pg.text, UI.scale(200)).img);
	return(img);
    }

    public BufferedImage longtip(List<ItemInfo> info) {
	return(longtip(item, info));
    }

    public class ItemTip implements Indir<Tex>, ItemInfo.InfoTip {
	private final TexI tex;

	public ItemTip(BufferedImage img) {
	    if(img == null)
		throw(new Loading());
	    tex = new TexI(img);
	}

	public GItem item() {return(item);}
	public List<ItemInfo> info() {return(item.info());}
	public Tex get() {return(tex);}
    }

    public class ShortTip extends ItemTip {
	public ShortTip(List<ItemInfo> info) {super(shorttip(info));}
    }

    public class LongTip extends ItemTip {
	public LongTip(List<ItemInfo> info) {super(longtip(info));}
    }

    private double hoverstart;
    private ItemTip shorttip = null, longtip = null;
    private List<ItemInfo> ttinfo = null;
    public Object tooltip(Coord c, Widget prev) {
	double now = Utils.rtime();
	if(prev == this) {
	} else if(prev instanceof WItem) {
	    double ps = ((WItem)prev).hoverstart;
	    if(now - ps < 1.0)
		hoverstart = now;
	    else
		hoverstart = ps;
	} else {
	    hoverstart = now;
	}
	try {
	    List<ItemInfo> info = item.info();
	    if(info.size() < 1)
		return(null);
	    if(info != ttinfo) {
		shorttip = longtip = null;
		ttinfo = info;
	    }
	    if(now - hoverstart < 1.0) {
		if(shorttip == null)
		    shorttip = new ShortTip(info);
		return(shorttip);
	    } else {
		if(longtip == null)
		    longtip = new LongTip(info);
		return(longtip);
	    }
	} catch(Loading e) {
	    return("...");
	}
    }

    private List<ItemInfo> info() {return(item.info());}
    public final AttrCache<Color> olcol = new AttrCache<>(this::info, info -> {
	    ArrayList<GItem.ColorInfo> ols = new ArrayList<>();
	    for(ItemInfo inf : info) {
		if(inf instanceof GItem.ColorInfo)
		    ols.add((GItem.ColorInfo)inf);
	    }
	    if(ols.size() == 0)
		return(() -> null);
	    if(ols.size() == 1)
		return(ols.get(0)::olcol);
	    ols.trimToSize();
	    return(() -> {
		    Color ret = null;
		    for(GItem.ColorInfo ci : ols) {
			Color c = ci.olcol();
			if(c != null)
			    ret = (ret == null) ? c : Utils.preblend(ret, c);
		    }
		    return(ret);
		});
	});
    public final AttrCache<GItem.InfoOverlay<?>[]> itemols = new AttrCache<>(this::info, info -> {
	    ArrayList<GItem.InfoOverlay<?>> buf = new ArrayList<>();
	    for(ItemInfo inf : info) {
		if(inf instanceof GItem.OverlayInfo)
		    buf.add(GItem.InfoOverlay.create((GItem.OverlayInfo<?>)inf));
	    }
	    GItem.InfoOverlay<?>[] ret = buf.toArray(new GItem.InfoOverlay<?>[0]);
	    return(() -> ret);
	});
    public final AttrCache<Double> itemmeter = new AttrCache<>(this::info, AttrCache.map1(GItem.MeterInfo.class, minf -> minf::meter));

    private GSprite lspr = null;
    public void tick(double dt) {
	/* XXX: This is ugly and there should be a better way to
	 * ensure the resizing happens as it should, but I can't think
	 * of one yet. */
	GSprite spr = item.spr();
	if((spr != null) && (spr != lspr)) {
	    Coord sz = new Coord(spr.sz());
	    if((sz.x % sqsz.x) != 0)
		sz.x = sqsz.x * ((sz.x / sqsz.x) + 1);
	    if((sz.y % sqsz.y) != 0)
		sz.y = sqsz.y * ((sz.y / sqsz.y) + 1);
	    resize(sz);
	    lspr = spr;
	}
	if(hovering) {
	    if(contents == null) {
		if((item.contents != null) && (contentswnd == null)) {
		    /* XXX: This is a bit weird, but I'm not sure what the alternative is... */
		    Widget cont = getparent(GameUI.class);
		    if(cont == null) cont = ui.root;
		    ckparent: for(Widget prev : cont.children()) {
			if(prev instanceof Contents) {
			    for(Widget p = parent; p != null; p = p.parent) {
				if(p == prev)
				    break ckparent;
				if(p instanceof Contents)
				    break;
			    }
			    return;
			}
		    }
		    item.contents.unlink();
		    contents = cont.add(new Contents(this, item.contents), parentpos(cont, sz.sub(5, 5).sub(Contents.hovermarg)));
		}
	    }
	} else {
	    if((contents != null) && !contents.hovering && !contents.hasmore()) {
		contents.reqdestroy();
		contents = null;
	    }
	}
	hovering = false;
    }

    public void draw(GOut g) {
	GSprite spr = item.spr();
	if(spr != null) {
	    Coord sz = spr.sz();
	    g.defstate();
	    if(olcol.get() != null)
		g.usestate(new ColorMask(olcol.get()));
	    drawmain(g, spr);
	    g.defstate();
	    GItem.InfoOverlay<?>[] ols = itemols.get();
	    if(ols != null) {
		for(GItem.InfoOverlay<?> ol : ols)
		    ol.draw(g);
	    }
	    Double meter = (item.meter > 0) ? Double.valueOf(item.meter / 100.0) : itemmeter.get();
	    if((meter != null) && (meter > 0)) {
		g.chcolor(255, 255, 255, 64);
		Coord half = sz.div(2);
		g.prect(half, half.inv(), half, meter * Math.PI * 2);
		g.chcolor();
	    }
	} else {
	    g.image(missing.layer(Resource.imgc).tex(), Coord.z, sz);
	}
    }

    public boolean mousedown(Coord c, int btn) {
	if(btn == 1) {
	    if(ui.modshift) {
		int n = ui.modctrl ? -1 : 1;
		item.wdgmsg("transfer", c, n);
	    } else if(ui.modctrl) {
		int n = ui.modmeta ? -1 : 1;
		item.wdgmsg("drop", c, n);
	    } else {
		item.wdgmsg("take", c);
	    }
	    return(true);
	} else if(btn == 3) {
	    item.wdgmsg("iact", c, ui.modflags());
	    return(true);
	}
	return(false);
    }

    public boolean drop(Coord cc, Coord ul) {
	return(false);
    }

    public boolean iteminteract(Coord cc, Coord ul) {
	item.wdgmsg("itemact", ui.modflags());
	return(true);
    }

    public boolean mousehover(Coord c) {
	if(item.contents != null) {
	    hovering = true;
	    return(true);
	}
	return(super.mousehover(c));
    }

    public void destroy() {
	if(contents != null) {
	    contents.reqdestroy();
	    contents = null;
	}
	if(contentswnd != null) {
	    contentswnd.reqdestroy();
	    contentswnd = null;
	}
	super.destroy();
    }

    public static class Contents extends Widget {
	public static final Coord hovermarg = UI.scale(8, 8);
	public static final Tex bg = Window.bg;
	public static final IBox obox = Window.wbox;
	public final WItem cont;
	public final Widget inv;
	private boolean invdest, hovering;
	private UI.Grab dm = null;
	private Coord doff;

	public Contents(WItem cont, Widget inv) {
	    z(90);
	    this.cont = cont;
	    /* XXX? This whole movement of the inv widget between
	     * various parents is kind of weird, but it's not
	     * obviously incorrect either. A proxy widget was tried,
	     * but that was even worse, due to rootpos and similar
	     * things being unavoidable wrong. */
	    this.inv = add(inv, hovermarg.add(obox.ctloff()));
	    this.tick(0);
	}

	public void draw(GOut g) {
	    Coord bgc = new Coord();
	    Coord ctl = hovermarg.add(obox.btloff());
	    Coord cbr = sz.sub(obox.cisz()).add(ctl);
	    for(bgc.y = ctl.y; bgc.y < cbr.y; bgc.y += bg.sz().y) {
		for(bgc.x = ctl.x; bgc.x < cbr.x; bgc.x += bg.sz().x)
		    g.image(bg, bgc, ctl, cbr);
	    }
	    obox.draw(g, hovermarg, sz.sub(hovermarg));
	    super.draw(g);
	}

	public void tick(double dt) {
	    super.tick(dt);
	    resize(inv.c.add(inv.sz).add(obox.btloff()));
	    hovering = false;
	}

	public void destroy() {
	    if(!invdest) {
		inv.unlink();
		cont.item.add(inv);
	    }
	    super.destroy();
	}

	public boolean hasmore() {
	    for(WItem item : children(WItem.class)) {
		if(item.contents != null)
		    return(true);
	    }
	    return(false);
	}

	public void cdestroy(Widget w) {
	    super.cdestroy(w);
	    if(w == inv) {
		cont.item.cdestroy(w);
		invdest = true;
		this.destroy();
		cont.contents = null;
	    }
	}

	public boolean mousedown(Coord c, int btn) {
	    if(super.mousedown(c, btn))
		return(true);
	    if(btn == 1) {
		dm = ui.grabmouse(this);
		doff = c;
		return(true);
	    }
	    return(false);
	}

	public boolean mouseup(Coord c, int btn) {
	    if((dm != null) && (btn == 1)) {
		dm.remove();
		dm = null;
		return(true);
	    }
	    return(super.mouseup(c, btn));
	}

	public void mousemove(Coord c) {
	    if(dm != null) {
		if(c.dist(doff) > 10) {
		    dm.remove();
		    dm = null;
		    Coord off = inv.c;
		    inv.unlink();
		    ContentsWindow wnd = new ContentsWindow(cont, inv);
		    off = off.sub(wnd.xlate(wnd.inv.c, true));
		    cont.contentswnd = parent.add(wnd, this.c.add(off));
		    wnd.drag(doff.sub(off));
		    invdest = true;
		    destroy();
		    cont.contents = null;
		}
	    } else {
		super.mousemove(c);
	    }
	}

	public boolean mousehover(Coord c) {
	    super.mousehover(c);
	    hovering = true;
	    return(true);
	}
    }

    public static class ContentsWindow extends Window {
	public final WItem cont;
	public final Widget inv;
	private boolean invdest;
	private Coord psz = null;

	public ContentsWindow(WItem cont, Widget inv) {
	    super(Coord.z, cont.item.contentsnm);
	    this.cont = cont;
	    this.inv = add(inv, Coord.z);
	    this.tick(0);
	}

	public void tick(double dt) {
	    if(cont.item.contents != inv) {
		destroy();
		cont.contentswnd = null;
		return;
	    }
	    super.tick(dt);
	    if(!Utils.eq(inv.sz, psz))
		resize(inv.c.add(psz = inv.sz));
	}

	public void wdgmsg(Widget sender, String msg, Object... args) {
	    if((sender == this) && (msg == "close")) {
		reqdestroy();
		cont.contentswnd = null;
	    } else {
		super.wdgmsg(sender, msg, args);
	    }
	}

	public void destroy() {
	    if(!invdest) {
		inv.unlink();
		cont.item.add(inv);
	    }
	    super.destroy();
	}

	public void cdestroy(Widget w) {
	    super.cdestroy(w);
	    if(w == inv) {
		cont.item.cdestroy(w);
		invdest = true;
		this.destroy();
		cont.contentswnd = null;
	    }
	}
    }
}
