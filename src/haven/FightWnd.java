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
	.add(FightWnd.class, wdg -> wdg)
	.add(Glob.class, wdg -> wdg.ui.sess.glob)
	.add(Session.class, wdg -> wdg.ui.sess);
    public static final Text.Foundry namef = new Text.Foundry(Text.serif.deriveFont(java.awt.Font.BOLD), 16).aa(true);
    public class Action implements ItemInfo.ResOwner {
	public final Indir<Resource> res;
	private final int id;
	public int a, u;
	private String name;

	public Action(Indir<Resource> res, int id, int a, int u) {this.res = res; this.id = id; this.a = a; this.u = u;}

	public String rendertext() {
	    StringBuilder buf = new StringBuilder();
	    Resource res = this.res.get();
	    buf.append("$img[" + res.name + "]\n\n");
	    buf.append("$b{$font[serif,16]{" + res.flayer(Resource.tooltip).t + "}}\n\n");
	    Resource.Pagina pag = res.layer(Resource.pagina);
	    if(pag != null)
		buf.append(pag.text);
	    return(buf.toString());
	}

	private void a(int a) {
	    this.a = a;
	}

	private void u(int u) {
	    if(this.u != u) {
		this.u = u;
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
		    info = Arrays.asList(new ItemInfo.Name(this, res.get().flayer(Resource.tooltip).t));
	    }
	    return(info);
	}
	public <T> T context(Class<T> cl) {return(actxr.context(cl, FightWnd.this));}

	public BufferedImage rendericon() {
	    BufferedImage ret = res.get().flayer(Resource.imgc).scaled();
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
	    l.cmp.add(namef.render(nm.str.text).img, new Coord(0, l.cmp.sz.y + UI.scale(10)));
	    l.cmp.sz = l.cmp.sz.add(0, UI.scale(10));
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

    public class Actions extends SListBox<Action, Widget> {
	private boolean loading = false;
	private Action drag = null;
	private UI.Grab grab;

	public Actions(Coord sz) {
	    super(sz, attrf.height() + UI.scale(2));
	}

	protected List<Action> items() {return(acts);}

	protected Widget makeitem(Action act, int idx, Coord sz) {
	    return(new Item(sz, act));
	}

	public class Item extends Widget implements DTarget {
	    public final Action item;
	    private final Label use;
	    private int u = -1, a = -1;
	    private UI.Grab grab;
	    private Coord dp;

	    public Item(Coord sz, Action act) {
		super(sz);
		this.item = act;
		Widget prev;
		prev = adda(new IButton("gfx/hud/buttons/add", "u", "d", "h").action(() -> setu(item.u + 1)), sz.x - UI.scale(2), sz.y / 2, 1.0, 0.5);
		prev = adda(new IButton("gfx/hud/buttons/sub", "u", "d", "h").action(() -> setu(item.u - 1)), prev.c.x - UI.scale(2), sz.y / 2, 1.0, 0.5);
		prev = use = adda(new Label("0/0", attrf), prev.c.x - UI.scale(5), sz.y / 2, 1.0, 0.5);
		add(IconText.of(Coord.of(prev.c.x - UI.scale(2), sz.y), act::rendericon, () -> act.res.get().flayer(Resource.tooltip).t), Coord.z);
	    }

	    public void tick(double dt) {
		if((item.u != this.u) || (item.a != this.a))
		    use.settext(String.format("%d/%d", this.u = item.u, this.a = item.a));
		super.tick(dt);
	    }

	    public boolean mousewheel(Coord c, int am) {
		if(ui.modshift) {
		    setu(item.u - am);
		    return(true);
		}
		return(super.mousewheel(c, am));
	    }

	    public boolean mousedown(Coord c, int button) {
		if(super.mousedown(c, button))
		    return(true);
		if(button == 1) {
		    change(item);
		    grab = ui.grabmouse(this);
		    dp = c;
		}
		return(true);
	    }

	    public void mousemove(Coord c) {
		super.mousemove(c);
		if((grab != null) && (c.dist(dp) > 5)) {
		    grab.remove();
		    grab = null;
		    drag(item);
		}
	    }

	    public boolean mouseup(Coord c, int button) {
		if((grab != null) && (button == 1)) {
		    grab.remove();
		    grab = null;
		    return(true);
		}
		return(super.mouseup(c, button));
	    }

	    public boolean setu(int u) {
		u = Utils.clip(u, 0, item.a);
		int s;
		for(s = 0; s < order.length; s++) {
		    if(order[s] == item)
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
			order[s] = item;
		    }
		} else {
		    if(s < order.length)
			order[s] = null;
		}
		item.u(u);
		return(true);
	    }

	    public boolean drop(Coord cc, Coord ul) {
		return(false);
	    }

	    public boolean iteminteract(Coord cc, Coord ul) {
		FightWnd.this.wdgmsg("itemact", item.id, ui.modflags());
		return(true);
	    }
	}

	public void change(Action act) {
	    if(act != null)
		info.set(() -> new TexI(act.renderinfo(info.sz.x - UI.scale(20))));
	    else if(sel != null)
		info.set((Tex)null);
	    super.change(act);
	}

	public void tick(double dt) {
	    if(loading) {
		loading = false;
		for(Action act : acts) {
		    try {
			act.name = act.res.get().flayer(Resource.tooltip).t;
		    } catch(Loading l) {
			act.name = "...";
			loading = true;
		    }
		}
		Collections.sort(acts, Comparator.comparing(act -> act.name));
	    }
	    super.tick(dt);
	}

	public void draw(GOut g) {
	    if(drag != null) {
		try {
		    Tex dt = drag.res.get().flayer(Resource.imgc).tex();
		    ui.drawafter(ag -> ag.image(dt, ui.mc.sub(dt.sz().div(2))));
		} catch(Loading l) {
		}
	    }
	    super.draw(g);
	}

	public void drag(Action act) {
	    if(grab == null)
		grab = ui.grabmouse(this);
	    drag = act;
	}

	public boolean mouseup(Coord c, int button) {
	    if((grab != null) && (button == 1)) {
		grab.remove();
		grab = null;
		if(drag != null) {
		    ui.dropthing(ui.root, c.add(rootpos()), drag);
		    drag = null;
		}
		return(true);
	    }
	    return(super.mouseup(c, button));
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
	    super(new Coord(((invsq.sz().x + UI.scale(2)) * (order.length - 1)) + (UI.scale(10) * ((order.length - 1) / 5)), 0).add(invsq.sz()));
	}

	private Coord itemc(int i) {
	    return(new Coord(((invsq.sz().x + UI.scale(2)) * i) + (UI.scale(10) * (i / 5)), 0));
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
			Coord ic = c.add(UI.scale(1), UI.scale(1));
			if(animoff[i] != null) {
			    ic = ic.add(animoff[i].mul(Math.pow(1.0 - animpr[i], 3)));
			}
			Tex tex = act.res.get().flayer(Resource.imgc).tex();
			g.image(tex, ic);
		    }
		} catch(Loading l) {}
		g.chcolor(156, 180, 158, 255);
		g.aimage(keys[i], c.add(invsq.sz().sub(UI.scale(2), 0)), 1, 1);
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

    public class Savelist extends Listbox<Integer> implements ReadLine.Owner {
	private int edit = -1;
	private Text.Line redit = null;
	private ReadLine nmed;
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
	    g.frect(Coord.z, g.sz());
	    g.chcolor();
	    if(n == edit) {
		if(redit == null)
		    redit = attrf.render(nmed.line());
		g.aimage(redit.tex(), new Coord(UI.scale(20), itemh / 2), 0.0, 0.5);
		if(hasfocus && (((Utils.rtime() - focusstart) % 1.0) < 0.5)) {
		    int cx = redit.advance(nmed.point());
		    g.chcolor(255, 255, 255, 255);
		    Coord co = new Coord(UI.scale(20) + cx + UI.scale(1), (g.sz().y - redit.sz().y) / 2);
		    g.line(co, co.add(0, redit.sz().y), 1);
		    g.chcolor();
		}
	    } else {
		g.aimage(saves[n].tex(), new Coord(UI.scale(20), itemh / 2), 0.0, 0.5);
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
			nmed = ReadLine.make(this, saves[sel].text);
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

	public void done(ReadLine buf) {
	    saves[edit] = attrf.render(buf.line());
	    edit = -1;
	    nmed = null;
	}

	public void changed(ReadLine buf) {
	    redit = null;
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
		if(key_esc.match(ev)) {
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
	info = add(new ImageInfoBox(UI.scale(new Coord(223, 160))), UI.scale(new Coord(5, 35)).add(wbox.btloff()));
	Frame.around(this, Collections.singletonList(info));

	add(CharWnd.settip(new Img(CharWnd.catf.render("Martial Arts & Combat Schools").tex()), "gfx/hud/chr/tips/combat"), 0, 0);
	actlist = add(new Actions(UI.scale(250, 160)), UI.scale(new Coord(245, 35)).add(wbox.btloff()));
	Frame.around(this, Collections.singletonList(actlist));

	p = add(new BView(), UI.scale(5, 208));
	count = add(new Label(""), p.pos("ur").adds(10, 0));

	savelist = add(new Savelist(UI.scale(370), 3), p.pos("bl").adds(0, 2).add(wbox.btloff()));
	p = Frame.around(this, Collections.singletonList(savelist));
	p = add(new Button(UI.scale(110), "Load", false).action(() -> {
		    load(savelist.sel);
		    use(savelist.sel);
	}), p.pos("ur").adds(10, 0));
	p = add(new Button(UI.scale(110), "Save", false).action(() -> {
		    if(savelist.sel < 0) {
			getparent(GameUI.class).error("No save entry selected.");
		    } else {
			save(savelist.sel);
			use(savelist.sel);
		    }
	}), p.pos("bl").adds(0, 2));
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
