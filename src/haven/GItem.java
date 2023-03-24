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

public class GItem extends AWidget implements ItemInfo.SpriteOwner, GSprite.Owner {
    public Indir<Resource> res;
    public MessageBuf sdt;
    public int meter = 0;
    public int num = -1;
    public Widget contents = null;
    public String contentsnm = null;
    public Object contentsid = null;
    public int infoseq;
    private Widget hovering;
    private boolean hoverset;
    private GSprite spr;
    private ItemInfo.Raw rawinfo;
    private List<ItemInfo> info = Collections.emptyList();

    @RName("item")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    int res = (Integer)args[0];
	    Message sdt = (args.length > 1)?new MessageBuf((byte[])args[1]):Message.nil;
	    return(new GItem(ui.sess.getres(res), sdt));
	}
    }

    public interface ColorInfo {
	public Color olcol();
    }

    public interface OverlayInfo<T> {
	public T overlay();
	public void drawoverlay(GOut g, T data);
    }

    public static class InfoOverlay<T> {
	public final OverlayInfo<T> inf;
	public final T data;

	public InfoOverlay(OverlayInfo<T> inf) {
	    this.inf = inf;
	    this.data = inf.overlay();
	}

	public void draw(GOut g) {
	    inf.drawoverlay(g, data);
	}

	public static <S> InfoOverlay<S> create(OverlayInfo<S> inf) {
	    return(new InfoOverlay<S>(inf));
	}
    }

    public interface NumberInfo extends OverlayInfo<Tex> {
	public int itemnum();
	public default Color numcolor() {
	    return(Color.WHITE);
	}

	public default Tex overlay() {
	    return(new TexI(GItem.NumberInfo.numrender(itemnum(), numcolor())));
	}

	public default void drawoverlay(GOut g, Tex tex) {
	    g.aimage(tex, g.sz(), 1, 1);
	}

	public static BufferedImage numrender(int num, Color col) {
	    return(Utils.outline2(Text.render(Integer.toString(num), col).img, Utils.contrast(col)));
	}
    }

    public interface MeterInfo {
	public double meter();
    }

    public static class Amount extends ItemInfo implements NumberInfo {
	private final int num;

	public Amount(Owner owner, int num) {
	    super(owner);
	    this.num = num;
	}

	public int itemnum() {
	    return(num);
	}
    }

    public GItem(Indir<Resource> res, Message sdt) {
	this.res = res;
	this.sdt = new MessageBuf(sdt);
    }

    public GItem(Indir<Resource> res) {
	this(res, Message.nil);
    }

    private Random rnd = null;
    public Random mkrandoom() {
	if(rnd == null)
	    rnd = new Random();
	return(rnd);
    }
    public Resource getres() {return(res.get());}
    private static final OwnerContext.ClassResolver<GItem> ctxr = new OwnerContext.ClassResolver<GItem>()
	.add(GItem.class, wdg -> wdg)
	.add(Glob.class, wdg -> wdg.ui.sess.glob)
	.add(Session.class, wdg -> wdg.ui.sess);
    public <T> T context(Class<T> cl) {return(ctxr.context(cl, this));}
    @Deprecated
    public Glob glob() {return(ui.sess.glob);}

    public GSprite spr() {
	GSprite spr = this.spr;
	if(spr == null) {
	    try {
		spr = this.spr = GSprite.create(this, res.get(), sdt.clone());
	    } catch(Loading l) {
	    }
	}
	return(spr);
    }

    public void tick(double dt) {
	super.tick(dt);
	GSprite spr = spr();
	if(spr != null)
	    spr.tick(dt);
	updcontinfo();
	ckconthover();
    }

    public List<ItemInfo> info() {
	if(this.info == null) {
	    List<ItemInfo> info = ItemInfo.buildinfo(this, rawinfo);
	    addcontinfo(info);
	    Resource.Pagina pg = res.get().layer(Resource.pagina);
	    if(pg != null)
		info.add(new ItemInfo.Pagina(this, pg.text));
	    this.info = info;
	}
	return(this.info);
    }

    public Resource resource() {
	return(res.get());
    }

    public GSprite sprite() {
	if(spr == null)
	    throw(new Loading("Still waiting for sprite to be constructed"));
	return(spr);
    }

    public void uimsg(String name, Object... args) {
	if(name == "num") {
	    num = (Integer)args[0];
	} else if(name == "chres") {
	    synchronized(this) {
		res = ui.sess.getres((Integer)args[0]);
		sdt = (args.length > 1)?new MessageBuf((byte[])args[1]):MessageBuf.nil;
		spr = null;
	    }
	} else if(name == "tt") {
	    info = null;
	    rawinfo = new ItemInfo.Raw(args);
	    infoseq++;
	} else if(name == "meter") {
	    meter = (int)((Number)args[0]).doubleValue();
	} else if(name == "contopen") {
	    boolean nst;
	    if(args[0] == null)
		nst = contentswnd == null;
	    else
		nst = ((Integer)args[0]) != 0;
	    showcontwnd(nst);
	} else {
	    super.uimsg(name, args);
	}
    }

    public void addchild(Widget child, Object... args) {
	/* XXX: Update this to use a checkable args[0] once a
	 * reasonable majority of clients can be expected to not crash
	 * on that. */
	if(true || ((String)args[0]).equals("contents")) {
	    contents = add(child);
	    contentsnm = (String)args[1];
	    contentsid = null;
	    if(args.length > 2)
		contentsid = args[2];
	}
    }

    public void cdestroy(Widget w) {
	super.cdestroy(w);
	if(w == contents) {
	    contents = null;
	    contentsid = null;
	}
    }

    public static interface ContentsInfo {
	public void propagate(List<ItemInfo> buf, ItemInfo.Owner outer);
    }

    /* XXX: Please remove me some time, some day, when custom clients
     * can be expected to have merged ContentsInfo. */
    private static void propagate(ItemInfo inf, List<ItemInfo> buf, ItemInfo.Owner outer) {
	try {
	    java.lang.reflect.Method mth = inf.getClass().getMethod("propagate", List.class, ItemInfo.Owner.class);
	    Utils.invoke(mth, inf, buf, outer);
	} catch(NoSuchMethodException e) {
	}
    }

    private int lastcontseq;
    private List<Pair<GItem, Integer>> lastcontinfo = null;
    private void updcontinfo() {
	if(info == null)
	    return;
	Widget contents = this.contents;
	if(contents != null) {
	    boolean upd = false;
	    if((lastcontinfo == null) || (lastcontseq != contents.childseq)) {
		lastcontinfo = new ArrayList<>();
		for(Widget ch : contents.children()) {
		    if(ch instanceof GItem) {
			GItem item = (GItem)ch;
			lastcontinfo.add(new Pair<>(item, item.infoseq));
		    }
		}
		lastcontseq = contents.childseq;
		upd = true;
	    } else {
		for(ListIterator<Pair<GItem, Integer>> i = lastcontinfo.listIterator(); i.hasNext();) {
		    Pair<GItem, Integer> ch = i.next();
		    if(ch.b != ch.a.infoseq) {
			i.set(new Pair<>(ch.a, ch.a.infoseq));
			upd = true;
		    }
		}
	    }
	    if(upd) {
		info = null;
		infoseq++;
	    }
	} else {
	    lastcontinfo = null;
	}
    }

    private void addcontinfo(List<ItemInfo> buf) {
	Widget contents = this.contents;
	if(contents != null) {
	    for(Widget ch : contents.children()) {
		if(ch instanceof GItem) {
		    for(ItemInfo inf : ((GItem)ch).info()) {
			if(inf instanceof ContentsInfo)
			    ((ContentsInfo)inf).propagate(buf, this);
			else
			    propagate(inf, buf, this);
		    }
		}
	    }
	}
    }

    private Widget contparent() {
	/* XXX: This is a bit weird, but I'm not sure what the alternative is... */
	Widget cont = getparent(GameUI.class);
	return((cont == null) ? cont = ui.root : cont);
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

    public void hovering(Widget hovering) {
	this.hovering = hovering;
	this.hoverset = true;
    }

    private Widget lcont = null;
    public Contents contentswdg;
    public Window contentswnd;
    private void ckconthover() {
	if(lcont != this.contents) {
	    if((this.contents != null) && (this.contentsid != null) && (contentswdg == null) && (contentswnd == null) &&
	       Utils.getprefb(String.format("cont-wndvis/%s", this.contentsid), false)) {
		Coord c = Utils.getprefc(String.format("cont-wndc/%s", this.contentsid), null);
		if(c != null) {
		    this.contents.unlink();
		    contentswnd = contparent().add(new ContentsWindow(this, this.contents), c);
		}
	    }
	    lcont = this.contents;
	}
	if(hovering != null) {
	    if(contentswdg == null) {
		if((this.contents != null) && (contentswnd == null)) {
		    Widget cont = contparent();
		    ckparent: for(Widget prev : cont.children()) {
			if(prev instanceof Contents) {
			    for(Widget p = hovering; p != null; p = p.parent) {
				if(p == prev)
				    break ckparent;
				if(p instanceof Contents)
				    break;
			    }
			    return;
			}
		    }
		    this.contents.unlink();
		    contentswdg = cont.add(new Contents(this, this.contents), hovering.parentpos(cont, hovering.sz.sub(Contents.overlap).sub(Contents.hovermarg)));
		}
	    }
	} else {
	    if((contentswdg != null) && !contentswdg.hovering && !contentswdg.hasmore()) {
		contentswdg.reqdestroy();
		contentswdg = null;
	    }
	}
	if(!hoverset)
	    hovering = null;
	hoverset = false;
    }

    public void showcontwnd(boolean show) {
	if(show && (contentswnd == null)) {
	    Widget cont = contparent();
	    Coord wc = null;
	    if(this.contentsid != null)
		wc = Utils.getprefc(String.format("cont-wndc/%s", this.contentsid), null);
	    if(wc == null)
		wc = cont.rootxlate(ui.mc).add(Contents.overlap);
	    contents.unlink();
	    if(contentswdg != null) {
		contentswdg.invdest = true;
		contentswdg.reqdestroy();
		contentswdg = null;
	    }
	    ContentsWindow wnd = new ContentsWindow(this, this.contents);
	    contentswnd = cont.add(wnd, wc);
	    if(this.contentsid != null) {
		Utils.setprefb(String.format("cont-wndvis/%s", this.contentsid), true);
		Utils.setprefc(String.format("cont-wndc/%s", this.contentsid), wc);
	    }
	} else if(!show && (contentswnd != null)) {
	    contentswnd.reqdestroy();
	    contentswnd = null;
	    Utils.setprefb(String.format("cont-wndvis/%s", this.contentsid), false);
	}
    }

    public static class Contents extends Widget {
	public static final Coord hovermarg = UI.scale(12, 12);
	public static final Coord overlap = UI.scale(2, 2);
	public static final Tex bg = Window.bg;
	public static final IBox obox = Window.wbox;
	public final GItem cont;
	public final Widget inv;
	private boolean invdest, hovering;
	private UI.Grab dm = null;
	private Coord doff;

	public Contents(GItem cont, Widget inv) {
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
	}

	public void destroy() {
	    if(!invdest) {
		inv.unlink();
		cont.add(inv);
	    }
	    super.destroy();
	}

	public boolean hasmore() {
	    for(GItem item : children(GItem.class)) {
		if(item.contentswdg != null)
		    return(true);
	    }
	    return(false);
	}

	public void cdestroy(Widget w) {
	    super.cdestroy(w);
	    if(w == inv) {
		cont.cdestroy(w);
		invdest = true;
		this.destroy();
		cont.contentswdg = null;
	    }
	}

	public boolean checkhit(Coord c) {
	    return((c.x >= hovermarg.x) && (c.y >= hovermarg.y));
	}

	public boolean mousedown(Coord c, int btn) {
	    if(super.mousedown(c, btn))
		return(true);
	    if(checkhit(c)) {
		if(btn == 1) {
		    dm = ui.grabmouse(this);
		    doff = c;
		    return(true);
		}
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
		    cont.contentswdg = null;
		}
	    } else {
		super.mousemove(c);
	    }
	}

	public boolean mousehover(Coord c, boolean on) {
	    super.mousehover(c, hovering);
	    hovering = on;
	    return(on);
	}
    }

    public static class ContentsWindow extends Window {
	public final GItem cont;
	public final Widget inv;
	private boolean invdest;
	private Coord psz = null;
	private Object id;

	public ContentsWindow(GItem cont, Widget inv) {
	    super(Coord.z, cont.contentsnm);
	    this.cont = cont;
	    this.inv = add(inv, Coord.z);
	    this.id = cont.contentsid;
	    this.tick(0);
	}

	private Coord lc = null;
	public void tick(double dt) {
	    if(cont.contents != inv) {
		destroy();
		cont.contentswnd = null;
		return;
	    }
	    super.tick(dt);
	    if(!Utils.eq(inv.sz, psz))
		resize(inv.c.add(psz = inv.sz));
	    if(!Utils.eq(lc, this.c) && (cont.contentsid != null)) {
		Utils.setprefc(String.format("cont-wndc/%s", cont.contentsid), lc = this.c);
		Utils.setprefb(String.format("cont-wndvis/%s", cont.contentsid), true);
	    }
	}

	public void wdgmsg(Widget sender, String msg, Object... args) {
	    if((sender == this) && (msg == "close")) {
		reqdestroy();
		cont.contentswnd = null;
		if(cont.contentsid != null)
		    Utils.setprefb(String.format("cont-wndvis/%s", cont.contentsid), false);
	    } else {
		super.wdgmsg(sender, msg, args);
	    }
	}

	public void destroy() {
	    if(!invdest) {
		inv.unlink();
		cont.add(inv);
	    }
	    super.destroy();
	}

	public void cdestroy(Widget w) {
	    super.cdestroy(w);
	    if(w == inv) {
		cont.cdestroy(w);
		invdest = true;
		this.destroy();
		cont.contentswnd = null;
	    }
	}
    }
}
