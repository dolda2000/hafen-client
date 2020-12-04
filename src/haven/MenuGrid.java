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
import java.awt.event.KeyEvent;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import haven.Resource.AButton;
import java.util.*;

public class MenuGrid extends Widget implements KeyBinding.Bindable {
    public final static Tex bg = Resource.loadtex("gfx/hud/invsq");
    public final static Coord bgsz = bg.sz().add(-UI.scale(1), -UI.scale(1));
    public final static RichText.Foundry ttfnd = new RichText.Foundry(TextAttribute.FAMILY, "SansSerif", TextAttribute.SIZE, UI.scale(10f));
    private static Coord gsz = new Coord(4, 4);
    public final Set<Pagina> paginae = new HashSet<Pagina>();
    public Pagina cur;
    private Pagina dragging;
    private Collection<PagButton> curbtns = Collections.emptyList();
    private PagButton pressed, layout[][] = new PagButton[gsz.x][gsz.y];
    private UI.Grab grab;
    private int curoff = 0;
    private boolean recons = true;
	
    @RName("scm")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    return(new MenuGrid());
	}
    }

    public static class PagButton implements ItemInfo.Owner {
	public final Pagina pag;
	public final Resource res;
	public final KeyBinding bind;

	public PagButton(Pagina pag) {
	    this.pag = pag;
	    this.res = pag.res();
	    this.bind = binding();
	}

	public BufferedImage img() {return(res.layer(Resource.imgc).scaled());}
	public String name() {return(res.layer(Resource.action).name);}
	public KeyMatch hotkey() {
	    char hk = res.layer(Resource.action).hk;
	    if(hk == 0)
		return(KeyMatch.nil);
	    return(KeyMatch.forchar(Character.toUpperCase(hk), KeyMatch.MODS & ~KeyMatch.S, 0));
	}
	public KeyBinding binding() {
	    return(KeyBinding.get("scm/" + res.name, hotkey()));
	}
	public void use() {
	    pag.scm.wdgmsg("act", (Object[])res.layer(Resource.action).ad);
	}

	public String sortkey() {
	    AButton ai = pag.act();
	    if(ai.ad.length == 0)
		return("\0" + name());
	    return(name());
	}

	private List<ItemInfo> info = null;
	public List<ItemInfo> info() {
	    if(info == null)
		info = ItemInfo.buildinfo(this, pag.rawinfo);
	    return(info);
	}
	private static final OwnerContext.ClassResolver<PagButton> ctxr = new OwnerContext.ClassResolver<PagButton>()
	    .add(Glob.class, p -> p.pag.scm.ui.sess.glob)
	    .add(Session.class, p -> p.pag.scm.ui.sess);
	public <T> T context(Class<T> cl) {return(ctxr.context(cl, this));}

	public BufferedImage rendertt(boolean withpg) {
	    Resource.Pagina pg = res.layer(Resource.pagina);
	    String tt = name();
	    KeyMatch key = bind.key();
	    int pos = -1;
	    char vkey = key.chr;
	    if((vkey == 0) && (key.keyname.length() == 1))
		vkey = key.keyname.charAt(0);
	    if((vkey != 0) && (key.modmatch == 0))
		pos = tt.toUpperCase().indexOf(Character.toUpperCase(vkey));
	    if(pos >= 0)
		tt = tt.substring(0, pos) + "$b{$col[255,128,0]{" + tt.charAt(pos) + "}}" + tt.substring(pos + 1);
	    else if(key != KeyMatch.nil)
		tt += " [$b{$col[255,128,0]{" + key.name() + "}}]";
	    BufferedImage ret = ttfnd.render(tt, UI.scale(300)).img;
	    if(withpg) {
		List<ItemInfo> info = info();
		info.removeIf(el -> el instanceof ItemInfo.Name);
		if(!info.isEmpty())
		    ret = ItemInfo.catimgs(0, ret, ItemInfo.longtip(info));
		if(pg != null)
		    ret = ItemInfo.catimgs(0, ret, ttfnd.render("\n" + pg.text, UI.scale(200)).img);
	    }
	    return(ret);
	}

	@Resource.PublishedCode(name = "pagina")
	public interface Factory {
	    public PagButton make(Pagina info);
	}
    }

    public final PagButton next = new PagButton(new Pagina(this, Resource.local().loadwait("gfx/hud/sc-next").indir())) {
	    {pag.button = this;}

	    public void use() {
		if((curoff + 14) >= curbtns.size())
		    curoff = 0;
		else
		    curoff += 14;
	    }

	    public String name() {return("More...");}

	    public KeyBinding binding() {return(kb_next);}
	};

    public final PagButton bk = new PagButton(new Pagina(this, Resource.local().loadwait("gfx/hud/sc-back").indir())) {
	    {pag.button = this;}

	    public void use() {
		pag.scm.cur = paginafor(pag.scm.cur.act().parent);
		curoff = 0;
	    }

	    public String name() {return("Back");}

	    public KeyBinding binding() {return(kb_back);}
	};

    public static class Pagina {
	public final MenuGrid scm;
	public final Indir<Resource> res;
	public State st;
	public double meter, gettime, dtime, fstart;
	public Indir<Tex> img;
	public int newp;
	public Object[] rawinfo = {};

	public static enum State {
	    ENABLED, DISABLED {
		public Indir<Tex> img(Pagina pag) {
		    return(Utils.cache(() -> new TexI(PUtils.monochromize(pag.button().img(), Color.LIGHT_GRAY))));
		}
	    };

	    public Indir<Tex> img(Pagina pag) {
		return(Utils.cache(() -> new TexI(pag.button().img())));
	    }
	}

	public Pagina(MenuGrid scm, Indir<Resource> res) {
	    this.scm = scm;
	    this.res = res;
	    state(State.ENABLED);
	}

	public Resource res() {
	    return(res.get());
	}

	public Resource.AButton act() {
	    return(res().layer(Resource.action));
	}

	private PagButton button = null;
	public PagButton button() {
	    if(button == null) {
		Resource res = res();
		PagButton.Factory f = res.getcode(PagButton.Factory.class, false);
		if(f == null)
		    button = new PagButton(this);
		else
		    button = f.make(this);
	    }
	    return(button);
	}

	public void state(State st) {
	    this.st = st;
	    this.img = st.img(this);
	}
    }

    public Map<Indir<Resource>, Pagina> pmap = new WeakHashMap<Indir<Resource>, Pagina>();
    public Pagina paginafor(Indir<Resource> res) {
	if(res == null)
	    return(null);
	synchronized(pmap) {
	    Pagina p = pmap.get(res);
	    if(p == null)
		pmap.put(res, p = new Pagina(this, res));
	    return(p);
	}
    }

    private boolean cons(Pagina p, Collection<PagButton> buf) {
	Pagina[] cp = new Pagina[0];
	Collection<Pagina> open, close = new HashSet<Pagina>();
	synchronized(paginae) {
	    open = new LinkedList<Pagina>();
	    for(Pagina pag : paginae) {
		if(pag.newp == 2) {
		    pag.newp = 0;
		    pag.fstart = 0;
		}
		open.add(pag);
	    }
	    for(Pagina pag : pmap.values()) {
		if(pag.newp == 2) {
		    pag.newp = 0;
		    pag.fstart = 0;
		}
	    }
	}
	boolean ret = true;
	while(!open.isEmpty()) {
	    Iterator<Pagina> iter = open.iterator();
	    Pagina pag = iter.next();
	    iter.remove();
	    try {
		AButton ad = pag.act();
		if(ad == null)
		    throw(new RuntimeException("Pagina in " + pag.res + " lacks action"));
		Pagina parent = paginafor(ad.parent);
		if((pag.newp != 0) && (parent != null) && (parent.newp == 0)) {
		    parent.newp = 2;
		    parent.fstart = (parent.fstart == 0)?pag.fstart:Math.min(parent.fstart, pag.fstart);
		}
		if(parent == p)
		    buf.add(pag.button());
		else if((parent != null) && !close.contains(parent) && !open.contains(parent))
		    open.add(parent);
		close.add(pag);
	    } catch(Loading e) {
		ret = false;
	    }
	}
	return(ret);
    }

    public MenuGrid() {
	super(bgsz.mul(gsz).add(UI.scale(1), UI.scale(1)));
    }

    private void updlayout() {
	synchronized(paginae) {
	    List<PagButton> cur = new ArrayList<>();
	    recons = !cons(this.cur, cur);
	    Collections.sort(cur, Comparator.comparing(PagButton::sortkey));
	    this.curbtns = cur;
	    int i = curoff;
	    for(int y = 0; y < gsz.y; y++) {
		for(int x = 0; x < gsz.x; x++) {
		    PagButton btn = null;
		    if((this.cur != null) && (x == gsz.x - 1) && (y == gsz.y - 1)) {
			btn = bk;
		    } else if((cur.size() > ((gsz.x * gsz.y) - 1)) && (x == gsz.x - 2) && (y == gsz.y - 1)) {
			btn = next;
		    } else if(i < cur.size()) {
			btn = cur.get(i++);
		    }
		    layout[x][y] = btn;
		}
	    }
	}
    }

    private static Map<PagButton, Tex> glowmasks = new WeakHashMap<>();
    private Tex glowmask(PagButton pag) {
	Tex ret = glowmasks.get(pag);
	if(ret == null) {
	    ret = new TexI(PUtils.glowmask(PUtils.glowmask(pag.img().getRaster()), 4, new Color(32, 255, 32)));
	    glowmasks.put(pag, ret);
	}
	return(ret);
    }
    public void draw(GOut g) {
	double now = Utils.rtime();
	for(int y = 0; y < gsz.y; y++) {
	    for(int x = 0; x < gsz.x; x++) {
		Coord p = bgsz.mul(new Coord(x, y));
		g.image(bg, p);
		PagButton btn = layout[x][y];
		if(btn != null) {
		    Pagina info = btn.pag;
		    Tex btex;
		    try {
			btex = info.img.get();
			g.image(btex, p.add(UI.scale(1), UI.scale(1)), btex.sz());
		    } catch(NullPointerException e) {
			System.err.println(btn);
			System.err.println(info.scm == this);
			throw(e);
		    }
		    if(info.meter > 0) {
			double m = info.meter;
			if(info.dtime > 0)
			    m += (1 - m) * (now - info.gettime) / info.dtime;
			m = Utils.clip(m, 0, 1);
			g.chcolor(255, 255, 255, 128);
			g.fellipse(p.add(bgsz.div(2)), bgsz.div(2), Math.PI / 2, ((Math.PI / 2) + (Math.PI * 2 * m)));
			g.chcolor();
		    }
		    if(info.newp != 0) {
			if(info.fstart == 0) {
			    info.fstart = now;
			} else {
			    double ph = (now - info.fstart) - (((x + (y * gsz.x)) * 0.15) % 1.0);
			    Tex glow = glowmask(btn);
			    if(ph < 1.25) {
				g.chcolor(255, 255, 255, (int)(255 * ((Math.cos(ph * Math.PI * 2) * -0.5) + 0.5)));
			    } else {
				g.chcolor(255, 255, 255, 128);
			    }
			    g.image(glow, p.sub(4, 4));
			    g.chcolor();
			}
		    }
		    if(btn == pressed) {
			g.chcolor(new Color(0, 0, 0, 128));
			g.frect(p.add(UI.scale(1), UI.scale(1)), btex.sz());
			g.chcolor();
		    }
		}
	    }
	}
	super.draw(g);
	if(dragging != null) {
	    Tex dt = dragging.img.get();
	    ui.drawafter(new UI.AfterDraw() {
		    public void draw(GOut g) {
			g.image(dt, ui.mc.add(dt.sz().div(2).inv()));
		    }
		});
	}
    }

    private PagButton curttp = null;
    private boolean curttl = false;
    private Tex curtt = null;
    private double hoverstart;
    public Object tooltip(Coord c, Widget prev) {
	PagButton pag = bhit(c);
	double now = Utils.rtime();
	if(pag != null) {
	    if(prev != this)
		hoverstart = now;
	    boolean ttl = (now - hoverstart) > 0.5;
	    if((pag != curttp) || (ttl != curttl)) {
		try {
		    BufferedImage ti = pag.rendertt(ttl);
		    curtt = (ti == null) ? null : new TexI(ti);
		} catch(Loading l) {
		    return(null);
		}
		curttp = pag;
		curttl = ttl;
	    }
	    return(curtt);
	} else {
	    hoverstart = now;
	    return(null);
	}
    }

    private PagButton bhit(Coord c) {
	Coord bc = c.div(bgsz);
	if((bc.x >= 0) && (bc.y >= 0) && (bc.x < gsz.x) && (bc.y < gsz.y))
	    return(layout[bc.x][bc.y]);
	else
	    return(null);
    }

    public boolean mousedown(Coord c, int button) {
	PagButton h = bhit(c);
	if((button == 1) && (h != null)) {
	    pressed = h;
	    grab = ui.grabmouse(this);
	}
	return(true);
    }

    public void mousemove(Coord c) {
	if((dragging == null) && (pressed != null)) {
	    PagButton h = bhit(c);
	    if(h != pressed)
		dragging = pressed.pag;
	}
    }

    public void use(PagButton r, boolean reset) {
	Collection<PagButton> sub = new ArrayList<>();
	cons(r.pag, sub);
	if(sub.size() > 0) {
	    this.cur = r.pag;
	    curoff = 0;
	} else {
	    r.pag.newp = 0;
	    r.use();
	    if(reset) {
		this.cur = null;
		curoff = 0;
	    }
	}
	updlayout();
    }

    public void tick(double dt) {
	if(recons)
	    updlayout();
    }

    public boolean mouseup(Coord c, int button) {
	PagButton h = bhit(c);
	if((button == 1) && (grab != null)) {
	    if(dragging != null) {
		ui.dropthing(ui.root, ui.mc, dragging.res());
		pressed = null;
		dragging = null;
	    } else if(pressed != null) {
		if(pressed == h)
		    use(h, false);
		pressed = null;
	    }
	    grab.remove();
	    grab = null;
	}
	return(true);
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "goto") {
	    if(args[0] == null)
		cur = null;
	    else
		cur = paginafor(ui.sess.getres((Integer)args[0]));
	    curoff = 0;
	    updlayout();
	} else if(msg == "fill") {
	    synchronized(paginae) {
		int a = 0;
		while(a < args.length) {
		    int fl = (Integer)args[a++];
		    Pagina pag = paginafor(ui.sess.getres((Integer)args[a++]));
		    if((fl & 1) != 0) {
			pag.state(Pagina.State.ENABLED);
			pag.meter = 0;
			if((fl & 2) != 0)
			    pag.state(Pagina.State.DISABLED);
			if((fl & 4) != 0) {
			    pag.meter = ((Number)args[a++]).doubleValue() / 1000.0;
			    pag.gettime = Utils.rtime();
			    pag.dtime = ((Number)args[a++]).doubleValue() / 1000.0;
			}
			if((fl & 8) != 0)
			    pag.newp = 1;
			if((fl & 16) != 0)
			    pag.rawinfo = (Object[])args[a++];
			else
			    pag.rawinfo = new Object[0];
			paginae.add(pag);
		    } else {
			paginae.remove(pag);
		    }
		}
		updlayout();
	    }
	} else {
	    super.uimsg(msg, args);
	}
    }

    public static final KeyBinding kb_root = KeyBinding.get("scm-root", KeyMatch.forcode(KeyEvent.VK_ESCAPE, 0));
    public static final KeyBinding kb_back = KeyBinding.get("scm-back", KeyMatch.forcode(KeyEvent.VK_BACK_SPACE, 0));
    public static final KeyBinding kb_next = KeyBinding.get("scm-next", KeyMatch.forchar('N', KeyMatch.S | KeyMatch.C | KeyMatch.M, KeyMatch.S));
    public boolean globtype(char k, KeyEvent ev) {
	if(kb_root.key().match(ev) && (this.cur != null)) {
	    this.cur = null;
	    curoff = 0;
	    updlayout();
	    return(true);
	} else if(kb_back.key().match(ev) && (this.cur != null)) {
	    use(bk, false);
	    return(true);
	} else if(kb_next.key().match(ev) && (layout[gsz.x - 2][gsz.y - 1] == next)) {
	    use(next, false);
	    return(true);
	}
	int cp = -1;
	PagButton pag = null;
	for(PagButton btn : curbtns) {
	    if(btn.bind.key().match(ev)) {
		int prio = btn.bind.set() ? 1 : 0;
		if((pag == null) || (prio > cp)) {
		    pag = btn;
		    cp = prio;
		}
	    }
	}
	if(pag != null) {
	    use(pag, true);
	    return(true);
	}
	return(false);
    }

    public KeyBinding getbinding(Coord cc) {
	PagButton h = bhit(cc);
	return((h == null) ? null : h.bind);
    }
}
