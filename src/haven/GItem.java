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
import haven.render.*;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class GItem extends AWidget implements ItemInfo.SpriteOwner, GSprite.Owner, RandomSource {
    public Indir<Resource> res;
    public MessageBuf sdt;
    public int meter = 0;
    public int num = -1;
    public Widget contents = null;
    public String contentsnm = null;
    public Object contentsid = null;
    public ContentsWindow contentswnd = null;
    public int infoseq;
    private Widget hovering;
    private boolean hoverset;
    private GSprite spr;
    private ItemInfo.Raw rawinfo;
    private List<ItemInfo> info = Collections.emptyList();

    @RName("item")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    Indir<Resource> res = ui.sess.getresv(args[0]);
	    Message sdt = (args.length > 1) ? new MessageBuf((byte[])args[1]) : Message.nil;
	    return(new GItem(res, sdt));
	}
    }

    public interface RStateInfo {
	public Pipe.Op rstate();
    }

    public interface ColorInfo extends RStateInfo {
	public Color olcol();

	public default Pipe.Op rstate() {
	    return(buf -> {
		    Color col = olcol();
		    if(col != null)
			new ColorMask(col).apply(buf);
		});
	}
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
	if(!hoverset)
	    hovering = null;
	hoverset = false;
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
	    num = Utils.iv(args[0]);
	} else if(name == "chres") {
	    synchronized(this) {
		res = ui.sess.getresv(args[0]);
		sdt = (args.length > 1) ? new MessageBuf((byte[])args[1]) : MessageBuf.nil;
		spr = null;
	    }
	} else if(name == "tt") {
	    info = null;
	    rawinfo = new ItemInfo.Raw(args);
	    infoseq++;
	} else if(name == "meter") {
	    meter = Utils.iv(args[0]);
	} else if(name == "contopen") {
	    if(contentswnd != null) {
		boolean nst;
		if(args[0] == null)
		    nst = (contentswnd.st != "wnd");
		else
		    nst = Utils.bv(args[0]);
		contentswnd.wndshow(nst);
	    }
	} else {
	    super.uimsg(name, args);
	}
    }

    public void addchild(Widget child, Object... args) {
	/* XXX: Update this to use a checkable args[0] once a
	 * reasonable majority of clients can be expected to not crash
	 * on that. */
	if(true || ((String)args[0]).equals("contents")) {
	    contents = child;
	    contentsnm = (String)args[1];
	    contentsid = null;
	    if(args.length > 2)
		contentsid = args[2];
	    contentswnd = contparent().add(new ContentsWindow(this, contents));
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

    public static class HoverDeco extends Window.Deco {
	public static final Coord hovermarg = UI.scale(12, 12);
	public static final Tex bg = Window.bg;
	public static final IBox box = Window.wbox;
	public Area ca;
	private UI.Grab dm = null;
	private Coord doff;

	public void iresize(Coord isz) {
	    ca = Area.sized(hovermarg.add(box.btloff()), isz);
	    resize(ca.br.add(box.bbroff()));
	}

	public Area contarea() {
	    return(ca);
	}

	public void draw(GOut g) {
	    Coord bgc = new Coord();
	    Coord ctl = hovermarg.add(box.btloff());
	    Coord cbr = sz.sub(box.bbroff());
	    for(bgc.y = ctl.y; bgc.y < cbr.y; bgc.y += bg.sz().y) {
		for(bgc.x = ctl.x; bgc.x < cbr.x; bgc.x += bg.sz().x)
		    g.image(bg, bgc, ctl, cbr);
	    }
	    box.draw(g, hovermarg, sz.sub(hovermarg));
	    super.draw(g);
	}

	public boolean checkhit(Coord c) {
	    return((c.x >= hovermarg.x) && (c.y >= hovermarg.y));
	}

	public boolean mousedown(Coord c, int btn) {
	    if(super.mousedown(c, btn))
		return(true);
	    if(checkhit(c) && (btn == 1)) {
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
		    ContentsWindow wnd = (ContentsWindow)parent;
		    wnd.drag(doff);
		    wnd.chstate("wnd");
		}
	    } else {
		super.mousemove(c);
	    }
	}
    }

    public static class ContentsWindow extends Window {
	public static final Coord overlap = UI.scale(2, 2);
	public final GItem cont;
	public final Widget inv;
	private final Object id;
	private Coord psz = null;
	private String st;
	private boolean hovering;

	public ContentsWindow(GItem cont, Widget inv) {
	    super(Coord.z, cont.contentsnm);
	    this.cont = cont;
	    this.inv = add(inv, Coord.z);
	    this.id = cont.contentsid;
	    this.tick(0);
	    Coord c = null;
	    if(Utils.getprefb(String.format("cont-wndvis/%s", id), false))
		c = Utils.getprefc(String.format("cont-wndc/%s", id), null);
	    if(c != null) {
		this.c = c;
		chstate("wnd");
	    } else {
		chstate("hide");
	    }
	}

	private void chstate(String nst) {
	    if(nst == st)
		return;
	    if(st == "wnd") {
		if(id != null)
		    Utils.setprefb(String.format("cont-wndvis/%s", id), false);
	    }
	    st = nst;
	    if(nst == "hide") {
		hide();
	    } else if(nst == "hover") {
		chdeco(new HoverDeco());
		show();
		z(90);
		if(parent != null)
		    raise();
	    } else if(nst == "wnd") {
		chdeco(new DefaultDeco());
		show();
		z(0);
		if(parent != null)
		    raise();
		if(id != null)
		    Utils.setprefb(String.format("cont-wndvis/%s", id), true);
	    }
	}

	private void ckhover() {
	    Widget hover = cont.hovering;
	    if(hover != null) {
		ckparent: for(Widget prev : parent.children()) {
		    if((prev instanceof ContentsWindow) && (((ContentsWindow)prev).st == "hover")) {
			for(Widget p = hover; p != null; p = p.parent) {
			    if(p == prev)
				break ckparent;
			    if(p instanceof ContentsWindow)
				break;
			}
			return;
		    }
		}
		chstate("hover");
		move(hover.parentpos(parent, hover.sz.sub(overlap).sub(HoverDeco.hovermarg)));
	    }
	}

	private void ckunhover() {
	    if((cont.hovering == null) && !hovering) {
		boolean hasmore = false;
		for(GItem item : children(GItem.class)) {
		    if((item.contentswnd != null) && (item.contentswnd.st == "hover")) {
			hasmore = true;
			break;
		    }
		}
		if(!hasmore)
		    chstate("hide");
	    }
	}

	private Coord lc = null;
	public void tick(double dt) {
	    super.tick(dt);
	    if(st == "hide") {
		ckhover();
	    } else if(st == "hover") {
		ckunhover();
	    }
	    if(!Utils.eq(inv.sz, psz))
		resize(inv.c.add(psz = inv.sz));
	    if(st == "wnd") {
		if(!Utils.eq(lc, this.c) && (id != null))
		    Utils.setprefc(String.format("cont-wndc/%s", id), lc = this.c);
	    }
	}

	public void wdgmsg(Widget sender, String msg, Object... args) {
	    if((sender == this) && (msg == "close")) {
		chstate("hide");
	    } else {
		super.wdgmsg(sender, msg, args);
	    }
	}

	public void cdestroy(Widget w) {
	    super.cdestroy(w);
	    if(w == inv) {
		cont.contents = null;
		cont.contentsnm = null;
		cont.contentsid = null;
		cont.contentswnd = null;
		this.destroy();
	    }
	}

	public boolean mousehover(Coord c, boolean on) {
	    hovering = on;
	    return(super.mousehover(c, on));
	}

	public void wndshow(boolean show) {
	    if(show && (st != "wnd")) {
		Coord wc = null;
		if(id != null)
		    wc = Utils.getprefc(String.format("cont-wndc/%s", id), null);
		if(st == "hide") {
		    if(wc == null)
			wc = cont.rootxlate(ui.mc).add(overlap);
		}
		chstate("wnd");
		if(wc != null)
		    move(wc);
	    } else if(!show && (st == "wnd")) {
		chstate("hide");
	    }
	}
    }
}
