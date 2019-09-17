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
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.event.KeyEvent;
import static haven.CharWnd.attrf;
import static haven.Window.wbox;
import static haven.Inventory.invsq;

public class FightWnd extends Widget {
    public final int nsave;
    public int maxact;
    public final Actions actlist;
    public final Savelist savelist;
    public List<Action> acts = new ArrayList<Action>();
    public final Action[] order;
    public int usesave;
    private final Text[] saves;
    private final ImageInfoBox info;
    private final Label count;
    private final Map<Indir<Resource>, Object[]> actrawinfo = new HashMap<>();

    public static interface IconInfo {
	public void draw(BufferedImage img, Graphics g);
    }

    private static final OwnerContext.ClassResolver<FightWnd> actxr = new OwnerContext.ClassResolver<FightWnd>()
	.add(Glob.class, wdg -> wdg.ui.sess.glob)
	.add(Session.class, wdg -> wdg.ui.sess);
    public static final Text.Foundry namef = new Text.Foundry(Text.serif.deriveFont(java.awt.Font.BOLD, 16f)).aa(true);
    public class Action implements ItemInfo.ResOwner {
	public final Indir<Resource> res;
	private final int id;
	public int a, u;
	private Text rnm, ru;
	private Tex ri;

	public Action(Indir<Resource> res, int id, int a, int u) {this.res = res; this.id = id; this.a = a; this.u = u;}

	public String rendertext() {
	    StringBuilder buf = new StringBuilder();
	    Resource res = this.res.get();
	    buf.append("$img[" + res.name + "]\n\n");
	    buf.append("$b{$font[serif,16]{" + res.layer(Resource.tooltip).t + "}}\n\n");
	    Resource.Pagina pag = res.layer(Resource.pagina);
	    if(pag != null)
		buf.append(pag.text);
	    return(buf.toString());
	}

	private void a(int a) {
	    if(this.a != a) {
		this.a = a;
		this.ru = null;
	    }
	}

	private void u(int u) {
	    if(this.u != u) {
		this.u = u;
		this.ru = null;
		recount();
	    }
	}

	public Resource resource() {return(res.get());}

	private List<ItemInfo> info = null;
	public List<ItemInfo> info() {
	    if(info == null) {
		Object[] rawinfo = actrawinfo.get(this.res);
		if(rawinfo != null)
		    info = ItemInfo.buildinfo(this, rawinfo);
		else
		    info = Arrays.asList(new ItemInfo.Name(this, res.get().layer(Resource.tooltip).t));
	    }
	    return(info);
	}
	public <T> T context(Class<T> cl) {return(actxr.context(cl, FightWnd.this));}

	public BufferedImage rendericon() {
	    BufferedImage ret = res.get().layer(Resource.imgc).img;
	    Graphics g = null;
	    for(ItemInfo inf : info()) {
		if(inf instanceof IconInfo) {
		    if(g == null) {
			BufferedImage buf = TexI.mkbuf(PUtils.imgsz(ret));
			g = buf.getGraphics();
			ret = buf;
		    }
		    ((IconInfo)inf).draw(ret, g);
		}
	    }
	    if(g != null)
		g.dispose();
	    return(ret);
	}

	private Tex icon = null;
	public Tex icon() {
	    if(icon == null)
		icon = new TexI(rendericon());
	    return(icon);
	}

	public BufferedImage renderinfo(int width) {
	    ItemInfo.Layout l = new ItemInfo.Layout();
	    l.width = width;
	    List<ItemInfo> info = info();
	    l.cmp.add(rendericon(), Coord.z);
	    ItemInfo.Name nm = ItemInfo.find(ItemInfo.Name.class, info);
	    l.cmp.add(namef.render(nm.str.text).img, new Coord(0, l.cmp.sz.y + 10));
	    l.cmp.sz = l.cmp.sz.add(0, 10);
	    for(ItemInfo inf : info) {
		if((inf != nm) && (inf instanceof ItemInfo.Tip)) {
		    l.add((ItemInfo.Tip)inf);
		}
	    }
	    Resource.Pagina pag = res.get().layer(Resource.pagina);
	    if(pag != null)
		l.add(new ItemInfo.Pagina(this, pag.text));
	    return(l.render());
	}
    }

    private void recount() {
	int u = 0;
	for(Action act : acts)
	    u += act.u;
	count.settext(String.format("Used: %d/%d", u, maxact));
	count.setcolor((u > maxact)?Color.RED:Color.WHITE);
    }

    public static class ImageInfoBox extends Widget {
	private Tex img;
	private Indir<Tex> loading;
	private final Scrollbar sb;

	public ImageInfoBox(Coord sz) {
	    super(sz);
	    sb = adda(new Scrollbar(sz.y, 0, 1), sz.x, 0, 1, 0);
	}

	public void drawbg(GOut g) {
	    g.chcolor(0, 0, 0, 128);
	    g.frect(Coord.z, sz);
	    g.chcolor();
	}

	public Coord marg() {return(new Coord(10, 10));}

	public void tick(double dt) {
	    if(loading != null) {
		try {
		    set(loading.get());
		    loading = null;
		} catch(Loading l) {
		}
	    }
	    super.tick(dt);
	}

	public void draw(GOut g) {
	    drawbg(g);
	    if(img != null)
		g.image(img, marg().sub(0, sb.val));
	    super.draw(g);
	}

	public void set(Tex img) {
	    this.img = img;
	    if(img != null) {
		sb.max = img.sz().y + (marg().y * 2) - sz.y;
		sb.val = 0;
	    } else {
		sb.max = sb.val = 0;
	    }
	}
	public void set(Indir<Tex> loading) {
	    this.loading = loading;
	}

	public boolean mousewheel(Coord c, int amount) {
	    sb.ch(amount * 20);
	    return(true);
	}

	public void resize(Coord sz) {
	    super.resize(sz);
	    sb.c = new Coord(sz.x - sb.sz.x, 0);
	    sb.resize(sz.y);
	    set(img);
	}
    }

    private static final Tex[] add = {Resource.loadtex("gfx/hud/buttons/addu"),
			      Resource.loadtex("gfx/hud/buttons/addd")};
    private static final Tex[] sub = {Resource.loadtex("gfx/hud/buttons/subu"),
			      Resource.loadtex("gfx/hud/buttons/subd")};
    public class Actions extends Listbox<Action> implements DTarget {
	private boolean loading = false;
	private int da = -1, ds = -1;
	UI.Grab d = null;
	Action drag = null;
	Coord dp;

	public Actions(int w, int h) {
	    super(w, h, attrf.height() + 2);
	}

	protected Action listitem(int n) {return(acts.get(n));}
	protected int listitems() {return(acts.size());}

	protected void drawbg(GOut g) {}

	protected void drawitem(GOut g, Action act, int idx) {
	    g.chcolor((idx % 2 == 0)?CharWnd.every:CharWnd.other);
	    g.frect(Coord.z, g.sz);
	    g.chcolor();
	    if(act.ru == null) act.ru = attrf.render(String.format("%d/%d", act.u, act.a));
	    try {
		if(act.ri == null)
		    act.ri = new TexI(PUtils.convolvedown(act.rendericon(), new Coord(itemh, itemh), CharWnd.iconfilter));
		g.image(act.ri, Coord.z);
	    } catch(Loading l) {
		g.image(WItem.missing.layer(Resource.imgc).tex(), Coord.z, new Coord(itemh, itemh));
	    }
	    int ty = (itemh - act.rnm.sz().y) / 2;
	    g.image(act.rnm.tex(), new Coord(itemh + 2, ty));
	    g.aimage(act.ru.tex(), new Coord(sz.x - 45, ty), 1.0, 0.0);
	    g.aimage(add[da == idx?1:0], new Coord(sz.x - 10, itemh / 2), 1.0, 0.5);
	    g.aimage(sub[ds == idx?1:0], new Coord(sz.x - 25, itemh / 2), 1.0, 0.5);
	}

	public void change(final Action act) {
	    if(act != null)
		info.set(() -> new TexI(act.renderinfo(info.sz.x - 20)));
	    else if(sel != null)
		info.set((Tex)null);
	    super.change(act);
	}

	public boolean mousewheel(Coord c, int am) {
	    if(ui.modshift) {
		Action act = itemat(c);
		if(act != null)
		    setu(act, act.u - am);
		return(true);
	    }
	    return(super.mousewheel(c, am));
	}

	public void draw(GOut g) {
	    if(loading) {
		loading = false;
		for(Action act : acts) {
		    try {
			act.rnm = attrf.render(act.res.get().layer(Resource.tooltip).t);
		    } catch(Loading l) {
			act.rnm = attrf.render("...");
			loading = true;
		    }
		}
		Collections.sort(acts, new Comparator<Action>() {
			public int compare(Action a, Action b) {
			    int ret = a.rnm.text.compareTo(b.rnm.text);
			    return(ret);
			}
		    });
	    }
	    if((drag != null) && (dp == null)) {
		try {
		    final Tex dt = drag.res.get().layer(Resource.imgc).tex();
		    ui.drawafter(new UI.AfterDraw() {
			    public void draw(GOut g) {
				g.image(dt, ui.mc.add(dt.sz().div(2).inv()));
			    }
			});
		} catch(Loading l) {}
	    }
	    super.draw(g);
	}

	private boolean onadd(Coord c, int idx) {
	    Coord ic = c.sub(0, (idx - sb.val) * itemh);
	    int by = (itemh - add[0].sz().y) / 2;
	    return(ic.isect(new Coord(sz.x - 10 - add[0].sz().x, by), add[0].sz()));
	}

	private boolean onsub(Coord c, int idx) {
	    Coord ic = c.sub(0, (idx - sb.val) * itemh);
	    int by = (itemh - sub[0].sz().y) / 2;
	    return(ic.isect(new Coord(sz.x - 25 - add[0].sz().x, by), add[0].sz()));
	}

	public void drag(Action act) {
	    if(d == null)
		d = ui.grabmouse(this);
	    drag = act;
	    dp = null;
	}

	public boolean mousedown(Coord c, int button) {
	    if(button == 1) {
		int idx = (c.y / itemh) + sb.val;
		if(idx < listitems()) {
		    if(onadd(c, idx)) {
			da = idx;
			d = ui.grabmouse(this);
			return(true);
		    } else if(onsub(c, idx)) {
			ds = idx;
			d = ui.grabmouse(this);
			return(true);
		    }
		}
		super.mousedown(c, button);
		if((sel != null) && (c.x < sb.c.x)) {
		    d = ui.grabmouse(this);
		    drag = sel;
		    dp = c;
		}
		return(true);
	    }
	    return(super.mousedown(c, button));
	}

	public void mousemove(Coord c) {
	    super.mousemove(c);
	    if((drag != null) && (dp != null)) {
		if(c.dist(dp) > 5)
		    dp = null;
	    }
	}

	private boolean setu(Action act, int u) {
	    u = Utils.clip(u, 0, act.a);
	    int s;
	    for(s = 0; s < order.length; s++) {
		if(order[s] == act)
		    break;
	    }
	    if(u > 0) {
		if(s == order.length) {
		    for(s = 0; s < order.length; s++) {
			if(order[s] == null)
			    break;
		    }
		    if(s == order.length)
			return(false);
		    order[s] = act;
		}
	    } else {
		if(s < order.length)
		    order[s] = null;
	    }
	    act.u(u);
	    return(true);
	}

	public boolean mouseup(Coord c, int button) {
	    if((d != null) && (button == 1)) {
		d.remove();
		d = null;
		if(drag != null) {
		    if(dp == null)
			ui.dropthing(ui.root, c.add(rootpos()), drag);
		    drag = null;
		}
		if(da >= 0) {
		    if(onadd(c, da)) {
			Action act = listitem(da);
			setu(act, act.u + 1);
		    }
		    da = -1;
		} else if(ds >= 0) {
		    if(onsub(c, ds)) {
			Action act = listitem(ds);
			setu(act, act.u - 1);
		    }
		    ds = -1;
		}
		return(true);
	    }
	    return(super.mouseup(c, button));
	}

	public boolean drop(Coord cc, Coord ul) {
	    return(false);
	}

	public boolean iteminteract(Coord cc, Coord ul) {
	    Action act = itemat(cc);
	    if(act != null)
		FightWnd.this.wdgmsg("itemact", act.id, ui.modflags());
	    return(true);
	}
    }

    public int findorder(Action a) {
	for(int i = 0; i < order.length; i++) {
	    if(order[i] == a)
		return(i);
	}
	return(-1);
    }

    public static final String[] keys = {"1", "2", "3", "4", "5", "\u21e71", "\u21e72", "\u21e73", "\u21e74", "\u21e75"};
    public class BView extends Widget implements DropTarget {
	private UI.Grab grab;
	private Action drag;
	private Coord dp;
	private final Coord[] animoff = new Coord[order.length];
	private final double[] animpr = new double[order.length];
	private boolean anim = false;

	private BView() {
	    super(new Coord(((invsq.sz().x + 2) * (order.length - 1)) + (10 * ((order.length - 1) / 5)), 0).add(invsq.sz()));
	}

	private Coord itemc(int i) {
	    return(new Coord(((invsq.sz().x + 2) * i) + (10 * (i / 5)), 0));
	}

	private int citem(Coord c) {
	    for(int i = 0; i < order.length; i++) {
		if(c.isect(itemc(i), invsq.sz()))
		    return(i);
	    }
	    return(-1);
	}

	final Tex[] keys = new Tex[10];
	{
	    for(int i = 0; i < 10; i++)
		this.keys[i] = Text.render(FightWnd.keys[i]).tex();
	}
	public void draw(GOut g) {
	    int[] reo = null;
	    if(anim) {
		reo = new int[order.length];
		for(int i = 0, a = 0, b = order.length - 1; i < order.length; i++) {
		    if(animoff[i] == null)
			reo[a++] = i;
		    else
			reo[b--] = i;
		}
	    }
	    for(int io = 0; io < order.length; io++) {
		int i = (reo == null)?io:reo[io];
		Coord c = itemc(i);
		g.image(invsq, c);
		Action act = order[i];
		try {
		    if(act != null) {
			Coord ic = c.add(1, 1);
			if(animoff[i] != null) {
			    ic = ic.add(animoff[i].mul(Math.pow(1.0 - animpr[i], 3)));
			}
			g.image(act.res.get().layer(Resource.imgc).tex(), ic);
		    }
		} catch(Loading l) {}
		g.chcolor(156, 180, 158, 255);
		g.aimage(keys[i], c.add(invsq.sz().sub(2, 0)), 1, 1);
		g.chcolor();
	    }
	}

	public boolean mousedown(Coord c, int button) {
	    if(button == 1) {
		int s = citem(c);
		if(s >= 0) {
		    Action act = order[s];
		    actlist.change(act);
		    actlist.display();
		    if(act != null) {
			grab = ui.grabmouse(this);
			drag = act;
			dp = c;
		    }
		    return(true);
		}
	    } else if(button == 3) {
		int s = citem(c);
		if(s >= 0) {
		    if(order[s] != null)
			order[s].u(0);
		    order[s] = null;
		    return(true);
		}
	    }
	    return(super.mousedown(c, button));
	}

	public void mousemove(Coord c) {
	    super.mousemove(c);
	    if(dp != null) {
		if(c.dist(dp) > 5) {
		    grab.remove();
		    actlist.drag(drag);
		    grab = null;
		    drag = null;
		    dp = null;
		}
	    }
	}

	public boolean mouseup(Coord c, int button) {
	    if(grab != null) {
		grab.remove();
		grab = null;
		drag = null;
		dp = null;
	    }
	    return(super.mouseup(c, button));
	}

	private void animate(int s, Coord off) {
	    animoff[s] = off;
	    animpr[s] = 0.0;
	    anim = true;
	}

	public boolean dropthing(Coord c, Object thing) {
	    if(thing instanceof Action) {
		Action act = (Action)thing;
		int s = citem(c);
		if(s < 0)
		    return(false);
		if(order[s] != act) {
		    int cp = findorder(act);
		    if(cp >= 0)
			order[cp] = order[s];
		    if(order[s] != null) {
			if(cp >= 0) {
			    animate(cp, itemc(s).sub(itemc(cp)));
			} else {
			    order[s].u(0);
			}
		    }
		    order[s] = act;
		    if(act.u < 1)
			act.u(1);
		}
		return(true);
	    }
	    return(false);
	}

	public void tick(double dt) {
	    if(anim) {
		boolean na = false;
		for(int i = 0; i < order.length; i++) {
		    if(animoff[i] != null) {
			if((animpr[i] += (dt * 3)) > 1.0)
			    animoff[i] = null;
			else
			    na = true;
		    }
		}
		anim = na;
	    }
	}
    }

    public class Savelist extends Listbox<Integer> {
	private int edit = -1;
	private Text.Line redit = null;
	private LineEdit nmed;
	private double focusstart;

	public Savelist(int w, int h) {
	    super(w, h, attrf.height() + 2);
	    setcanfocus(true);
	    sel = Integer.valueOf(0);
	}

	protected Integer listitem(int idx) {return(idx);}
	protected int listitems() {return(nsave);}

	protected void drawbg(GOut g) {}

	protected void drawitem(GOut g, Integer save, int n) {
	    g.chcolor((n % 2 == 0)?CharWnd.every:CharWnd.other);
	    g.frect(Coord.z, g.sz);
	    g.chcolor();
	    if(n == edit) {
		if(redit == null)
		    redit = attrf.render(nmed.line);
		g.aimage(redit.tex(), new Coord(20, itemh / 2), 0.0, 0.5);
		if(hasfocus && (((Utils.rtime() - focusstart) % 1.0) < 0.5)) {
		    int cx = redit.advance(nmed.point);
		    g.chcolor(255, 255, 255, 255);
		    Coord co = new Coord(20 + cx + 1, (g.sz.y - redit.sz().y) / 2);
		    g.line(co, co.add(0, redit.sz().y), 1);
		    g.chcolor();
		}
	    } else {
		g.aimage(saves[n].tex(), new Coord(20, itemh / 2), 0.0, 0.5);
	    }
	    if(n == usesave)
		g.aimage(CheckBox.smark, new Coord(itemh / 2, itemh / 2), 0.5, 0.5);
	}

	private Coord lc = null;
	private double lt = 0;
	public boolean mousedown(Coord c, int button) {
	    boolean ret = super.mousedown(c, button);
	    if(ret && (button == 1)) {
		double now = Utils.rtime();
		if(((now - lt) < 0.5) && (c.dist(lc) < 10) && (sel != null) && (saves[sel] != unused)) {
		    if(sel == usesave) {
			edit = sel;
			nmed = new LineEdit(saves[sel].text) {
				protected void done(String line) {
				    saves[edit] = attrf.render(line);
				    edit = -1;
				    nmed = null;
				}

				protected void changed() {
				    redit = null;
				}
			    };
			redit = null;
			parent.setfocus(this);
			focusstart = now;
		    } else {
			load(sel);
			use(sel);
		    }
		} else {
		    lt = now;
		    lc = c;
		}
	    }
	    return(ret);
	}

	public void change(Integer sel) {
	    super.change(sel);
	    if((edit != -1) && (edit != sel)) {
		edit = -1;
		redit = null;
		nmed = null;
	    }
	}

	public boolean keydown(KeyEvent ev) {
	    if(edit != -1) {
		if(ev.getKeyChar() == 27) {
		    edit = -1;
		    redit = null;
		    nmed = null;
		    return(true);
		} else {
		    return(nmed.key(ev));
		}
	    }
	    return(super.keydown(ev));
	}
    }

    @RName("fmg")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    return(new FightWnd((Integer)args[0], (Integer)args[1], (Integer)args[2]));
	}
    }

    public void load(int n) {
	wdgmsg("load", n);
    }

    public void save(int n) {
	List<Object> args = new LinkedList<Object>();
	args.add(n);
	if(saves[n] != unused)
	    args.add(saves[n].text);
	for(int i = 0; i < order.length; i++) {
	    if(order[i] == null) {
		args.add(null);
	    } else {
		args.add(order[i].id);
		args.add(order[i].u);
	    }
	}
	wdgmsg("save", args.toArray(new Object[0]));
    }

    public void use(int n) {
	wdgmsg("use", n);
    }

    private Text unused = new Text.Foundry(attrf.font.deriveFont(java.awt.Font.ITALIC)).aa(true).render("Unused save");
    public FightWnd(int nsave, int nact, int max) {
	super(Coord.z);
	this.nsave = nsave;
	this.maxact = max;
	this.order = new Action[nact];
	this.saves = new Text[nsave];
	for(int i = 0; i < nsave; i++)
	    saves[i] = unused;

	Widget p;
	info = add(new ImageInfoBox(new Coord(223, 152)), new Coord(5, 35).add(wbox.btloff()));
	Frame.around(this, Collections.singletonList(info));

	add(new Img(CharWnd.catf.render("Martial Arts & Combat Schools").tex()), 0, 0);
	actlist = add(new Actions(250, 8), new Coord(245, 35).add(wbox.btloff()));
	Frame.around(this, Collections.singletonList(actlist));

	p = add(new BView(), 5, 200);
	count = add(new Label(""), p.c.add(p.sz.x + 10, 0));

	savelist = add(new Savelist(370, 3), new Coord(5, 238).add(wbox.btloff()));
	Frame.around(this, Collections.singletonList(savelist));
	add(new Button(110, "Load", false) {
		public void click() {
		    load(savelist.sel);
		    use(savelist.sel);
		}
	    }, 395, 238);
	add(new Button(110, "Save", false) {
		public void click() {
		    if(savelist.sel < 0) {
			getparent(GameUI.class).error("No save entry selected.");
		    } else {
			save(savelist.sel);
			use(savelist.sel);
		    }
		}
	    }, 395, 265);
	pack();
    }

    public Action findact(int resid) {
	for(Action act : acts) {
	    if(act.id == resid)
		return(act);
	}
	return(null);
    }

    public void uimsg(String nm, Object... args) {
	if(nm == "avail") {
	    List<Action> acts = new ArrayList<Action>();
	    int a = 0;
	    while(true) {
		int resid = (Integer)args[a++];
		if(resid < 0)
		    break;
		int av = (Integer)args[a++];
		Action pact = findact(resid);
		if(pact == null) {
		    acts.add(new Action(ui.sess.getres(resid), resid, av, 0));
		} else {
		    acts.add(pact);
		    pact.a(av);
		}
	    }
	    this.acts = acts;
	    actlist.loading = true;
	} else if(nm == "tt") {
	    Indir<Resource> res = ui.sess.getres((Integer)args[0]);
	    Object[] rawinfo = (Object[])args[1];
	    actrawinfo.put(res, rawinfo);
	} else if(nm == "used") {
	    int a = 0;
	    for(Action act : acts)
		act.u(0);
	    for(int i = 0; i < order.length; i++) {
		int resid = (Integer)args[a++];
		if(resid < 0) {
		    order[i] = null;
		    continue;
		}
		int us = (Integer)args[a++];
		(order[i] = findact(resid)).u(us);
	    }
	} else if(nm == "saved") {
	    int fl = (Integer)args[0];
	    for(int i = 0; i < nsave; i++) {
		if((fl & (1 << i)) != 0) {
		    if(args[i + 1] instanceof String)
			saves[i] = attrf.render((String)args[i + 1]);
		    else
			saves[i] = attrf.render(String.format("Saved school %d", i + 1));
		} else {
		    saves[i] = unused;
		}
	    }
	} else if(nm == "use") {
	    usesave = (Integer)args[0];
	    savelist.change(Integer.valueOf(usesave));
	} else if(nm == "max") {
	    maxact = (Integer)args[0];
	    recount();
	} else {
	    super.uimsg(nm, args);
	}
    }
}
