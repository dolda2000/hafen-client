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
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import static haven.CharWnd.*;

public class WoundWnd extends Widget {
    public static final Text.Foundry namef = new Text.Foundry(Text.serif.deriveFont(java.awt.Font.BOLD), 16).aa(true);
    public final Widget woundbox;
    public final WoundList wounds;
    public Wound.Info wound;

    @RName("wounds")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    return(new WoundWnd());
	}
    }

    public static interface QuickInfo {
	public default Widget qwdg(int h) {
	    if(qstr() == null)
		return(null);
	    return(new Label(qstr(), attrf));
	}

	public default String qstr() {
	    return(null);
	}
    }

    public static class WoundPagina extends ItemInfo.Tip {
	public final String str;

	public WoundPagina(Owner owner, String str) {
	    super(owner);
	    this.str = str;
	}

	public void layout(Layout l) {
	    BufferedImage t = ifnd.render(str, l.width).img;
	    if(t != null) {
		l.cmp.add(t, Coord.of(0, l.cmp.sz.y));
		l.cmp.sz = l.cmp.sz.add(0, UI.scale(10));
	    }
	}

	public int order() {return(10);}
    }

    public static class Wound implements ItemInfo.ResOwner {
	public final Glob glob;
	public final int id, parentid;
	public Indir<Resource> res;
	public int level;
	public ItemInfo.Raw rawinfo;
	private String sortkey = "\uffff";

	private Wound(Glob glob, int id, Indir<Resource> res, int parentid) {
	    this.glob = glob;
	    this.id = id;
	    this.res = res;
	    this.parentid = parentid;
	}

	private static final OwnerContext.ClassResolver<Wound> ctxr = new OwnerContext.ClassResolver<Wound>()
	    .add(Wound.class, wnd -> wnd)
	    .add(Glob.class, wnd -> wnd.glob)
	    .add(Session.class, wnd -> wnd.glob.sess);
	public <T> T context(Class<T> cl) {return(ctxr.context(cl, this));}
	public Resource resource() {return(res.get());}

	private List<ItemInfo> info;
	public List<ItemInfo> info() {
	    if(info == null) {
		List<ItemInfo> info = ItemInfo.buildinfo(this, rawinfo);
		Resource.Pagina pag = res.get().layer(Resource.pagina);
		if(pag != null)
		    info.add(new WoundPagina(this, pag.text));
		this.info = info;
	    }
	    return(info);
	}

	public BufferedImage icon() {
	    return(CharWnd.IconInfo.render(res.get().flayer(Resource.imgc).scaled(), info()));
	}

	public String name() {
	    return(ItemInfo.find(ItemInfo.Name.class, info()).str.text);
	}

	public interface Info {
	    public int woundid();
	}
    }

    public static class WoundBox extends ImageInfoBox implements Wound.Info {
	public final int id;
	private List<ItemInfo> info;

	public WoundBox(int id) {
	    super(Coord.z);
	    this.id = id;
	}

	protected void added() {
	    resize(parent.sz);
	}

	public Wound wound() {
	    return(getparent(WoundWnd.class).wounds.get(id));
	}

	public void tick(double dt) {
	    super.tick(dt);
	    if(this.info != wound().info())
		set(() -> new TexI(renderinfo(sz.x - Scrollbar.width - (marg().x * 2))));
	}

	public void drawbg(GOut g) {}

	public BufferedImage renderinfo(int width) {
	    Wound wnd = wound();
	    ItemInfo.Layout l = new ItemInfo.Layout(wnd);
	    l.width = width;
	    List<ItemInfo> info = wnd.info();
	    l.cmp.add(wnd.icon(), Coord.z);
	    ItemInfo.Name nm = ItemInfo.find(ItemInfo.Name.class, info);
	    l.cmp.add(namef.render(nm.str.text).img, Coord.of(0, l.cmp.sz.y + UI.scale(10)));
	    l.cmp.sz = l.cmp.sz.add(0, UI.scale(10));
	    for(ItemInfo inf : info) {
		if((inf != nm) && (inf instanceof ItemInfo.Tip))
		    l.add((ItemInfo.Tip)inf);
	    }
	    this.info = info;
	    return(l.render());
	}

	public int woundid() {return(id);}
    }

    @RName("wound")
    public static class $wound implements Factory {
	public Widget create(UI ui, Object[] args) {
	    int id = Utils.iv(args[0]);
	    return(new WoundBox(id));
	}
    }

    public class WoundList extends SListBox<Wound, Widget> {
	public List<Wound> wounds = new ArrayList<Wound>();
	private boolean loading = false;
	private final Comparator<Wound> wcomp = new Comparator<Wound>() {
	    public int compare(Wound a, Wound b) {
		return(a.sortkey.compareTo(b.sortkey));
	    }
	};

	private WoundList(Coord sz) {
	    super(sz, attrf.height() + UI.scale(2));
	}

	protected List<Wound> items() {return(wounds);}
	protected Widget makeitem(Wound w, int idx, Coord sz) {return(new Item(sz, w));}

	private List<Wound> treesort(List<Wound> from, int pid, int level) {
	    List<Wound> direct = new ArrayList<>(from.size());
	    for(Wound w : from) {
		if(w.parentid == pid) {
		    w.level = level;
		    direct.add(w);
		}
	    }
	    Collections.sort(direct, wcomp);
	    List<Wound> ret = new ArrayList<>(from.size());
	    for(Wound w : direct) {
		ret.add(w);
		ret.addAll(treesort(from, w.id, level + 1));
	    }
	    return(ret);
	}

	public void tick(double dt) {
	    if(loading) {
		loading = false;
		for(Wound w : wounds) {
		    try {
			w.sortkey = w.res.get().flayer(Resource.tooltip).t;
		    } catch(Loading l) {
			w.sortkey = "\uffff";
			loading = true;
		    }
		}
		wounds = treesort(wounds, -1, 0);
	    }
	    super.tick(dt);
	}

	public class Item extends Widget implements DTarget {
	    public final Wound w;
	    private Widget qd, nm;
	    private Object dres, dinfo;

	    public Item(Coord sz, Wound w) {
		super(sz);
		this.w = w;
		update();
	    }

	    private void update() {
		if(qd != null) {qd.reqdestroy(); qd = null;}
		if(nm != null) {nm.reqdestroy(); nm = null;}
		List<ItemInfo> info = null;
		try {
		    info = w.info();
		} catch(Loading l) {}
		QuickInfo qdata = (info == null) ? null : ItemInfo.find(QuickInfo.class, info);
		int nw = sz.x;
		if(qdata != null) {
		    qd = adda(qdata.qwdg(sz.y), sz.x - UI.scale(1), sz.y / 2, 1.0, 0.5);
		    nw = qd.c.x - UI.scale(5);
		}
		int x = w.level * itemh;
		nm = adda(IconText.of(Coord.of(nw - x, sz.y), w::icon, w::name), x, sz.y / 2, 0.0, 0.5);
		this.dinfo = info;
		this.dres = w.res;
	    }

	    public boolean drop(Coord cc, Coord ul) {
		return(false);
	    }

	    public boolean iteminteract(Coord cc, Coord ul) {
		WoundWnd.this.wdgmsg("wiact", w.id, ui.modflags());
		return(true);
	    }

	    public void draw(GOut g) {
		Object cinfo = null;
		try {
		    cinfo = w.info();
		} catch(Loading l) {}
		if(!Utils.eq(dres, w.res) || (dinfo != cinfo))
		    update();
		super.draw(g);
	    }

	    public boolean mousedown(MouseDownEvent ev) {
		if(ev.propagate(this) || super.mousedown(ev))
		    return(true);
		if(ev.b == 1) {
		    WoundWnd.this.wdgmsg("wsel", w.id);
		    return(true);
		} else if(ev.b == 3) {
		    WoundWnd.this.wdgmsg("wclick", w.id, ev.b, ui.modflags());
		    return(true);
		}
		return(false);
	    }
	}

	protected void drawslot(GOut g, Wound w, int idx, Area area) {
	    super.drawslot(g, w, idx, area);
	    if((wound != null) && (wound.woundid() == w.id))
		drawsel(g, w, idx, area);
	}

	protected boolean unselect(int button) {
	    if(button == 1)
		WoundWnd.this.wdgmsg("wsel", (Object)null);
	    return(true);
	}

	public Wound get(int id) {
	    for(Wound w : wounds) {
		if(w.id == id)
		    return(w);
	    }
	    return(null);
	}

	public void add(Wound w) {
	    wounds.add(w);
	}

	public Wound remove(int id) {
	    for(Iterator<Wound> i = wounds.iterator(); i.hasNext();) {
		Wound w = i.next();
		if(w.id == id) {
		    i.remove();
		    return(w);
		}
	    }
	    return(null);
	}
    }

    public WoundWnd() {
	Widget prev;

	prev = add(CharWnd.settip(new Img(catf.render("Health & Wounds").tex()), "gfx/hud/chr/tips/wounds"), 0, 0);
	this.wounds = add(new WoundList(Coord.of(attrw, height)), prev.pos("bl").x(width + UI.scale(5)).add(wbox.btloff()));
	Frame.around(this, Collections.singletonList(this.wounds));
	woundbox = add(new Widget(new Coord(attrw, this.wounds.sz.y)) {
		public void draw(GOut g) {
		    g.chcolor(0, 0, 0, 128);
		    g.frect(Coord.z, sz);
		    g.chcolor();
		    super.draw(g);
		}

		public void cdestroy(Widget w) {
		    if(w == wound)
			wound = null;
		}
	    }, prev.pos("bl").adds(5, 0).add(wbox.btloff()));
	Frame.around(this, Collections.singletonList(woundbox));
	pack();
    }

    public void addchild(Widget child, Object... args) {
	String place = (args[0] instanceof String) ? (((String)args[0]).intern()) : null;
	if(place == "wound") {
	    this.wound = (Wound.Info)child;
	    woundbox.add(child, Coord.z);
	} else {
	    super.addchild(child, args);
	}
    }

    private void decwound(Object[] args, int a, int len) {
	int id = Utils.iv(args[a]);
	Indir<Resource> res = (args[a + 1] == null) ? null : ui.sess.getresv(args[a + 1]);
	if(res != null) {
	    int parentid = (len > 3) ? ((args[a + 3] == null) ? -1 : Utils.iv(args[a + 3])) : -1;
	    Wound w = wounds.get(id);
	    if(w == null) {
		wounds.add(w =new Wound(ui.sess.glob, id, res, parentid));
	    } else {
		w.res = res;
	    }
	    w.rawinfo = new ItemInfo.Raw(Utils.splice(args, a + 4, len - 4));
	    w.info = null;
	    wounds.loading = true;
	} else {
	    wounds.remove(id);
	}
    }

    public void uimsg(String nm, Object... args) {
	if(nm == "wounds") {
	    if(args.length > 0) {
		if(args[0] instanceof Object[]) {
		    for(int i = 0; i < args.length; i++)
			decwound((Object[])args[i], 0, ((Object[])args[i]).length);
		} else {
		    for(int i = 0; i < args.length; i += 3)
			decwound(args, i, 3);
		}
	    }
	} else {
	    super.uimsg(nm, args);
	}
    }
}
