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

public class MenuGrid extends Widget {
    public final static Tex bg = Resource.loadtex("gfx/hud/invsq");
    public final static Coord bgsz = bg.sz().add(-1, -1);
    public final static Pagina next = new Pagina(null, Resource.local().loadwait("gfx/hud/sc-next").indir());
    public final static Pagina bk = new Pagina(null, Resource.local().loadwait("gfx/hud/sc-back").indir());
    public final static RichText.Foundry ttfnd = new RichText.Foundry(TextAttribute.FAMILY, "SansSerif", TextAttribute.SIZE, 10);
    public final Set<Pagina> paginae = new HashSet<Pagina>();
    private static Coord gsz = new Coord(4, 4);
    private Pagina cur, pressed, dragging, layout[][] = new Pagina[gsz.x][gsz.y];
    private UI.Grab grab;
    private int curoff = 0;
    private boolean recons = true;
    private Map<Character, Pagina> hotmap = new TreeMap<Character, Pagina>();
	
    @RName("scm")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    return(new MenuGrid());
	}
    }

    public static class Pagina implements java.io.Serializable, ItemInfo.Owner {
	public final Glob glob;
	public final Indir<Resource> res;
	public State st;
	public int meter, dtime;
	public long gettime;
	public Image img;
	public int newp;
	public long fstart;
	public Object[] rawinfo = {};

	public interface Image {
	    public Tex tex();
	}

	public static enum State {
	    ENABLED, DISABLED {
		public Image img(final Pagina pag) {
		    return(new Image() {
			    private Tex c = null;

			    public Tex tex() {
				if(pag.res() == null)
				    return(null);
				if(c == null)
				    c = new TexI(PUtils.monochromize(pag.res().layer(Resource.imgc).img, Color.LIGHT_GRAY));
				return(c);
			    }
			});
		}
	    };

	    public Image img(final Pagina pag) {
		return(new Image() {
			public Tex tex() {
			    if(pag.res() == null)
				return(null);
			    return(pag.res().layer(Resource.imgc).tex());
			}
		    });
	    }
	}

	public Pagina(Glob glob, Indir<Resource> res) {
	    this.glob = glob;
	    this.res = res;
	    state(State.ENABLED);
	}

	public Resource res() {
	    return(res.get());
	}

	public Resource.AButton act() {
	    return(res().layer(Resource.action));
	}

	public void state(State st) {
	    this.st = st;
	    this.img = st.img(this);
	}

	private List<ItemInfo> info = null;
	public List<ItemInfo> info() {
	    if(info == null)
		info = ItemInfo.buildinfo(this, rawinfo);
	    return(info);
	}
	private static final OwnerContext.ClassResolver<Pagina> ctxr = new OwnerContext.ClassResolver<Pagina>()
	    .add(Glob.class, p -> p.glob)
	    .add(Session.class, p -> p.glob.sess);
	public <T> T context(Class<T> cl) {return(ctxr.context(cl, this));}
    }

    public class PaginaException extends RuntimeException {
	public Pagina pag;
	
	public PaginaException(Pagina p) {
	    super("Invalid pagina: " + p.res);
	    pag = p;
	}
    }

    public Map<Indir<Resource>, Pagina> pmap = new WeakHashMap<Indir<Resource>, Pagina>();
    public Pagina paginafor(Indir<Resource> res) {
	if(res == null)
	    return(null);
	synchronized(pmap) {
	    Pagina p = pmap.get(res);
	    if(p == null)
		pmap.put(res, p = new Pagina(ui.sess.glob, res));
	    return(p);
	}
    }

    private boolean cons(Pagina p, Collection<Pagina> buf) {
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
		    throw(new PaginaException(pag));
		Pagina parent = paginafor(ad.parent);
		if((pag.newp != 0) && (parent != null) && (parent.newp == 0)) {
		    parent.newp = 2;
		    parent.fstart = (parent.fstart == 0)?pag.fstart:Math.min(parent.fstart, pag.fstart);
		}
		if(parent == p)
		    buf.add(pag);
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
	super(bgsz.mul(gsz).add(1, 1));
    }

    private static Comparator<Pagina> sorter = new Comparator<Pagina>() {
	public int compare(Pagina a, Pagina b) {
	    AButton aa = a.act(), ab = b.act();
	    if((aa.ad.length == 0) && (ab.ad.length > 0))
		return(-1);
	    if((aa.ad.length > 0) && (ab.ad.length == 0))
		return(1);
	    return(aa.name.compareTo(ab.name));
	}
    };

    private void updlayout() {
	synchronized(paginae) {
	    List<Pagina> cur = new ArrayList<Pagina>();
	    recons = !cons(this.cur, cur);
	    Collections.sort(cur, sorter);
	    int i = curoff;
	    hotmap.clear();
	    for(int y = 0; y < gsz.y; y++) {
		for(int x = 0; x < gsz.x; x++) {
		    Pagina btn = null;
		    if((this.cur != null) && (x == gsz.x - 1) && (y == gsz.y - 1)) {
			btn = bk;
		    } else if((cur.size() > ((gsz.x * gsz.y) - 1)) && (x == gsz.x - 2) && (y == gsz.y - 1)) {
			btn = next;
		    } else if(i < cur.size()) {
			Resource.AButton ad = cur.get(i).act();
			if(ad.hk != 0)
			    hotmap.put(Character.toUpperCase(ad.hk), cur.get(i));
			btn = cur.get(i++);
		    }
		    layout[x][y] = btn;
		}
	    }
	}
    }

    private static BufferedImage rendertt(Pagina pag, boolean withpg) {
	Resource.AButton ad = pag.res.get().layer(Resource.action);
	Resource.Pagina pg = pag.res.get().layer(Resource.pagina);
	String tt = ad.name;
	int pos = tt.toUpperCase().indexOf(Character.toUpperCase(ad.hk));
	if(pos >= 0)
	    tt = tt.substring(0, pos) + "$col[255,255,0]{" + tt.charAt(pos) + "}" + tt.substring(pos + 1);
	else if(ad.hk != 0)
	    tt += " [" + ad.hk + "]";
	BufferedImage ret = ttfnd.render(tt, 300).img;
	if(withpg) {
	    List<ItemInfo> info = pag.info();
	    info.removeIf(el -> el instanceof ItemInfo.Name);
	    if(!info.isEmpty())
		ret = ItemInfo.catimgs(0, ret, ItemInfo.longtip(info));
	    if(pg != null)
		ret = ItemInfo.catimgs(0, ret, ttfnd.render("\n" + pg.text, 200).img);
	}
	return(ret);
    }

    private static Map<Pagina, Tex> glowmasks = new WeakHashMap<Pagina, Tex>();
    private Tex glowmask(Pagina pag) {
	Tex ret = glowmasks.get(pag);
	if(ret == null) {
	    ret = new TexI(PUtils.glowmask(PUtils.glowmask(pag.res().layer(Resource.imgc).img.getRaster()), 4, new Color(32, 255, 32)));
	    glowmasks.put(pag, ret);
	}
	return(ret);
    }
    public void draw(GOut g) {
	long now = System.currentTimeMillis();
	for(int y = 0; y < gsz.y; y++) {
	    for(int x = 0; x < gsz.x; x++) {
		Coord p = bgsz.mul(new Coord(x, y));
		g.image(bg, p);
		Pagina btn = layout[x][y];
		if(btn != null) {
		    Tex btex = btn.img.tex();
		    g.image(btex, p.add(1, 1));
		    if(btn.meter > 0) {
			double m = btn.meter / 1000.0;
			if(btn.dtime > 0)
			    m += (1 - m) * (double)(now - btn.gettime) / (double)btn.dtime;
			m = Utils.clip(m, 0, 1);
			g.chcolor(255, 255, 255, 128);
			g.fellipse(p.add(bgsz.div(2)), bgsz.div(2), Math.PI / 2, ((Math.PI / 2) + (Math.PI * 2 * m)));
			g.chcolor();
		    }
		    if(btn.newp != 0) {
			if(btn.fstart == 0) {
			    btn.fstart = now;
			} else {
			    double ph = ((now - btn.fstart) / 1000.0) - (((x + (y * gsz.x)) * 0.15) % 1.0);
			    if(ph < 1.25) {
				g.chcolor(255, 255, 255, (int)(255 * ((Math.cos(ph * Math.PI * 2) * -0.5) + 0.5)));
				g.image(glowmask(btn), p.sub(4, 4));
				g.chcolor();
			    } else {
				g.chcolor(255, 255, 255, 128);
				g.image(glowmask(btn), p.sub(4, 4));
				g.chcolor();
			    }
			}
		    }
		    if(btn == pressed) {
			g.chcolor(new Color(0, 0, 0, 128));
			g.frect(p.add(1, 1), btex.sz());
			g.chcolor();
		    }
		}
	    }
	}
	super.draw(g);
	if(dragging != null) {
	    final Tex dt = dragging.img.tex();
	    ui.drawafter(new UI.AfterDraw() {
		    public void draw(GOut g) {
			g.image(dt, ui.mc.add(dt.sz().div(2).inv()));
		    }
		});
	}
    }

    private Pagina curttp = null;
    private boolean curttl = false;
    private Tex curtt = null;
    private long hoverstart;
    public Object tooltip(Coord c, Widget prev) {
	Pagina pag = bhit(c);
	long now = System.currentTimeMillis();
	if((pag != null) && (pag.act() != null)) {
	    if(prev != this)
		hoverstart = now;
	    boolean ttl = (now - hoverstart) > 500;
	    if((pag != curttp) || (ttl != curttl)) {
		try {
		    curtt = new TexI(rendertt(pag, ttl));
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

    private Pagina bhit(Coord c) {
	Coord bc = c.div(bgsz);
	if((bc.x >= 0) && (bc.y >= 0) && (bc.x < gsz.x) && (bc.y < gsz.y))
	    return(layout[bc.x][bc.y]);
	else
	    return(null);
    }

    public boolean mousedown(Coord c, int button) {
	Pagina h = bhit(c);
	if((button == 1) && (h != null)) {
	    pressed = h;
	    grab = ui.grabmouse(this);
	}
	return(true);
    }

    public void mousemove(Coord c) {
	if((dragging == null) && (pressed != null)) {
	    Pagina h = bhit(c);
	    if(h != pressed)
		dragging = pressed;
	}
    }

    private void use(Pagina r, boolean reset) {
	Collection<Pagina> sub = new LinkedList<Pagina>(),
	    cur = new LinkedList<Pagina>();
	cons(r, sub);
	cons(this.cur, cur);
	if(sub.size() > 0) {
	    this.cur = r;
	    curoff = 0;
	} else if(r == bk) {
	    this.cur = paginafor(this.cur.act().parent);
	    curoff = 0;
	} else if(r == next) {
	    if((curoff + 14) >= cur.size())
		curoff = 0;
	    else
		curoff += 14;
	} else {
	    r.newp = 0;
	    wdgmsg("act", (Object[])r.act().ad);
	    if(reset)
		this.cur = null;
	    curoff = 0;
	}
	updlayout();
    }

    public void tick(double dt) {
	if(recons)
	    updlayout();
    }

    public boolean mouseup(Coord c, int button) {
	Pagina h = bhit(c);
	if((button == 1) && (grab != null)) {
	    if(dragging != null) {
		ui.dropthing(ui.root, ui.mc, dragging.res());
		dragging = pressed = null;
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
			    pag.meter = ((Number)args[a++]).intValue();
			    pag.gettime = System.currentTimeMillis();
			    pag.dtime = (Integer)args[a++];
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

    public boolean globtype(char k, KeyEvent ev) {
	if((k == 27) && (this.cur != null)) {
	    this.cur = null;
	    curoff = 0;
	    updlayout();
	    return(true);
	} else if((k == 8) && (this.cur != null)) {
	    this.cur = paginafor(this.cur.act().parent);
	    curoff = 0;
	    updlayout();
	    return(true);
	} else if((k == 'N') && (layout[gsz.x - 2][gsz.y - 1] == next)) {
	    use(next, false);
	    return(true);
	}
	Pagina r = hotmap.get(Character.toUpperCase(k));
	if(r != null) {
	    use(r, true);
	    return(true);
	}
	return(false);
    }
}
