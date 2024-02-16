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
import haven.resutil.FoodInfo;
import static haven.CharWnd.*;
import static haven.PUtils.*;

public class BAttrWnd extends Widget {
    public final List<Attr> attrs;
    public final FoodMeter feps;
    public final Constipations cons;
    public final GlutMeter glut;

    public static class Attr extends Widget {
	public final String nm;
	public final Text rnm;
	public final Glob.CAttr attr;
	public final Tex img;
	public final Color bg;
	private double lvlt = 0.0;
	private Text ct;
	private int cbv = -1, ccv = -1;

	private Attr(Glob glob, String attr, Color bg) {
	    super(new Coord(attrw, attrf.height() + UI.scale(2)));
	    Resource res = Resource.local().loadwait("gfx/hud/chr/" + attr);
	    this.nm = attr;
	    this.rnm = attrf.render(res.flayer(Resource.tooltip).t);
	    this.img = new TexI(convolve(res.flayer(Resource.imgc).img, new Coord(this.sz.y, this.sz.y), iconfilter));
	    this.attr = glob.getcattr(attr);
	    this.bg = bg;
	}

	public void tick(double dt) {
	    if((attr.base != cbv) || (attr.comp != ccv)) {
		cbv = attr.base; ccv = attr.comp;
		Color c = Color.WHITE;
		if(ccv > cbv) {
		    c = buff;
		    tooltip = String.format("%d + %d", cbv, ccv - cbv);
		} else if(ccv < cbv) {
		    c = debuff;
		    tooltip = String.format("%d - %d", cbv, cbv - ccv);
		} else {
		    tooltip = null;
		}
		ct = attrf.render(Integer.toString(ccv), c);
	    }
	    if((lvlt > 0.0) && ((lvlt -= dt) < 0))
		lvlt = 0.0;
	}

	public void draw(GOut g) {
	    if(lvlt != 0.0)
		g.chcolor(Utils.blendcol(bg, new Color(128, 255, 128, 128), lvlt));
	    else
		g.chcolor(bg);
	    g.frect(Coord.z, sz);
	    g.chcolor();
	    Coord cn = new Coord(0, sz.y / 2);
	    g.aimage(img, cn.add(5, 0), 0, 0.5);
	    g.aimage(rnm.tex(), cn.add(img.sz().x + UI.scale(10), 1), 0, 0.5);
	    if(ct != null)
		g.aimage(ct.tex(), cn.add(sz.x - UI.scale(7), 1), 1, 0.5);
	}

	public void lvlup() {
	    lvlt = 1.0;
	}
    }

    public static class Constipations extends SListBox<Constipations.El, Widget> {
	public static final PUtils.Convolution tflt = new PUtils.Hanning(1);
	public static final Color hilit = new Color(255, 255, 0, 48);
	public static final Color buffed = new Color(160, 255, 160), full = new Color(250, 230, 64), none = new Color(250, 19, 43);
	public final List<El> els = new ArrayList<El>();
	public static final Comparator<El> ecmp = (a, b) -> {
	    if(a.a < b.a)
		return(-1);
	    else if(a.a > b.a)
		return(1);
	    return(0);
	};

	public class El {
	    public final ResData t;
	    public double a;
	    private boolean hl;

	    public El(ResData t, double a) {this.t = t; this.a = a;}
	    public void update(double a) {this.a = a;}
	}

	public Constipations(Coord sz) {
	    super(sz, attrf.height() + UI.scale(2));
	}

	public static class Reordered<T> extends AbstractList<T> {
	    private final List<T> back;
	    private final Comparator<? super T> cmp;
	    private Integer[] order = {};

	    public Reordered(List<T> back, Comparator<? super T> cmp) {
		this.back = back;
		this.cmp = cmp;
		update();
	    }

	    public int size() {
		return(back.size());
	    }

	    public T get(int i) {
		return(back.get(order[i]));
	    }

	    public void update() {
		if(order.length != back.size()) {
		    order = new Integer[back.size()];
		    for(int i = 0; i < order.length; i++)
			order[i] = i;
		}
		Arrays.sort(order, (a, b) -> cmp.compare(back.get(a), back.get(b)));
	    }
	}

	private final Reordered<El> oels = new Reordered<>(els, ecmp);
	protected List<El> items() {return(oels);}
	protected Widget makeitem(El el, int idx, Coord sz) {return(new Item(sz, el));}

	public static class ItemIcon extends IconText {
	    public final ItemSpec spec;

	    public ItemIcon(Coord sz, ItemSpec spec) {
		super(sz);
		this.spec = spec;
	    }

	    protected BufferedImage img() {return(spec.image());}
	    protected String text() {return(spec.name());}
	    protected PUtils.Convolution filter() {return(tflt);}
	}

	public class Item extends Widget {
	    public final El el;
	    private Widget nm, a;
	    private double da = Double.NaN;

	    public Item(Coord sz, El el) {
		super(sz);
		this.el = el;
		update();
	    }

	    private void update() {
		if(el.a != da) {
		    if(nm != null) {nm.reqdestroy(); nm = null;}
		    if( a != null) { a.reqdestroy();  a = null;}
		    Label a = adda(new Label(String.format("%d%%", Math.max((int)Math.round((1.0 - el.a) * 100), 1)), attrf),
				   sz.x - UI.scale(1), sz.y / 2, 1.0, 0.5);
		    a.setcolor((el.a > 1.0) ? buffed : Utils.blendcol(none, full, el.a));
		    nm = adda(new ItemIcon(sz, new ItemSpec(OwnerContext.uictx.curry(Constipations.this.ui), el.t, null)),
			      0, sz.y / 2, 0.0, 0.5);
		    this.a = a;
		    da = el.a;
		}
	    }

	    public void draw(GOut g) {
		update();
		super.draw(g);
	    }
	}

	private ItemInfo.InfoTip lasttip = null;
	public void draw(GOut g) {
	    ItemInfo.InfoTip tip = (ui.lasttip instanceof ItemInfo.InfoTip) ? (ItemInfo.InfoTip)ui.lasttip : null;
	    if(tip != lasttip) {
		for(El el : els)
		    el.hl = false;
		FoodInfo finf = Loading.or(() -> (tip == null) ? null : ItemInfo.find(FoodInfo.class, tip.info()), (FoodInfo)null);
		if(finf != null) {
		    for(int o = 0; o < finf.types.length; o++)
			els.get(finf.types[o]).hl = true;
		}
		lasttip = tip;
	    }
	    super.draw(g);
	}

	protected void drawslot(GOut g, El el, int idx, Area area) {
	    g.chcolor(el.hl ? hilit : ((idx % 2) == 0) ? every : other);
	    g.frect2(area.ul, area.br);
	    g.chcolor();
	}

	public boolean unselect(int button) {
	    return(false);
	}

	public void update(ResData t, double a) {
	    prev: {
		for(Iterator<El> i = els.iterator(); i.hasNext();) {
		    El el = i.next();
		    if(!Utils.eq(el.t, t))
			continue;
		    if(a == 1.0)
			i.remove();
		    else
			el.update(a);
		    break prev;
		}
		els.add(new El(t, a));
	    }
	    oels.update();
	}
    }

    public static class FoodMeter extends Widget {
	public static final Tex frame =  Resource.loadtex("gfx/hud/chr/foodm");
	public static final Coord marg = new Coord(5, 5), trmg = new Coord(10, 10);
	public double cap;
	public List<El> els = new LinkedList<El>();
	private List<El> enew = null, etr = null;
	private Indir<Resource> trev = null;
	private Tex trol;
	private double trtm = 0;

	@Resource.LayerName("foodev")
	public static class Event extends Resource.Layer {
	    public final Color col;
	    public final String nm;
	    public final int sort;

	    public Event(Resource res, Message buf) {
		res.super();
		int ver = buf.uint8();
		if(ver == 1) {
		    col = new Color(buf.uint8(), buf.uint8(), buf.uint8(), buf.uint8());
		    nm = buf.string();
		    sort = buf.int16();
		} else {
		    throw(new Resource.LoadException("unknown foodev version: " + ver, res));
		}
	    }

	    public void init() {}
	}

	public static class El {
	    public final Indir<Resource> res;
	    public double a;

	    public El(Indir<Resource> res, double a) {this.res = res; this.a = a;}

	    private Event ev = null;
	    public Event ev() {
		if(ev == null)
		    ev = res.get().layer(Event.class);
		return(ev);
	    }
	}
	public static final Comparator<El> dcmp = new Comparator<El>() {
	    public int compare(El a, El b) {
		int c;
		if((c = (a.ev().sort - b.ev().sort)) != 0)
		    return(c);
		return(a.ev().nm.compareTo(b.ev().nm));
	    }
	};

	public FoodMeter() {
	    super(frame.sz());
	}

	private BufferedImage mktrol(List<El> els, Indir<Resource> trev) {
	    BufferedImage buf = TexI.mkbuf(sz.add(trmg.mul(2)));
	    Coord marg2 = marg.add(trmg);
	    Graphics g = buf.getGraphics();
	    double x = 0;
	    int w = sz.x - (marg.x * 2);
	    for(El el : els) {
		int l = (int)Math.floor((x / cap) * w);
		int r = (int)Math.floor(((x += el.a) / cap) * w);
		if(el.res == trev) {
		    g.setColor(Utils.blendcol(el.ev().col, Color.WHITE, 0.5));
		    g.fillRect(marg2.x - (trmg.x / 2) + l, marg2.y - (trmg.y / 2), r - l + trmg.x, sz.y - (marg.y * 2) + trmg.y);
		}
	    }
	    imgblur(buf.getRaster(), trmg.x, trmg.y);
	    return(buf);
	}

	private void drawels(GOut g, List<El> els, int alpha) {
	    double x = 0;
	    int w = sz.x - (marg.x * 2);
	    for(El el : els) {
		int l = (int)Math.floor((x / cap) * w);
		int r = (int)Math.floor(((x += el.a) / cap) * w);
		try {
		    Color col = el.ev().col;
		    g.chcolor(new Color(col.getRed(), col.getGreen(), col.getBlue(), alpha));
		    g.frect(new Coord(marg.x + l, marg.y), new Coord(r - l, sz.y - (marg.y * 2)));
		} catch(Loading e) {
		}
	    }
	}

	public void tick(double dt) {
	    if(enew != null) {
		try {
		    Collections.sort(enew, dcmp);
		    els = enew;
		    rtip = null;
		    enew = null;
		} catch(Loading l) {}
	    }
	    if(trev != null) {
		try {
		    Collections.sort(etr, dcmp);
		    GameUI gui = getparent(GameUI.class);
		    if(gui != null)
			gui.msg(String.format("You gained " + Loading.waitfor(trev).flayer(Event.class).nm), Color.WHITE);
		    trol = new TexI(mktrol(etr, trev));
		    trtm = Utils.rtime();
		    trev = null;
		} catch(Loading l) {}
	    }
	}

	public void draw(GOut g) {
	    double d = (trtm > 0)?(Utils.rtime() - trtm):Double.POSITIVE_INFINITY;
	    g.chcolor(0, 0, 0, 255);
	    g.frect(marg, sz.sub(marg.mul(2)));
	    drawels(g, els, 255);
	    if(d < 1.0)
		drawels(g, etr, (int)(255 - (d * 255)));
	    g.chcolor();
	    g.image(frame, Coord.z);
	    if(d < 2.5) {
		GOut g2 = g.reclipl(trmg.inv(), sz.add(trmg.mul(2)));
		g2.chcolor(255, 255, 255, (int)(255 - ((d * 255) * (1.0 / 2.5))));
		g2.image(trol, Coord.z);
	    } else {
		trtm = 0;
	    }
	}

	public void update(Object... args) {
	    int n = 0;
	    this.cap = Utils.fv(args[n++]);
	    List<El> enew = new LinkedList<El>();
	    while(n < args.length) {
		Indir<Resource> res = ui.sess.getres((Integer)args[n++]);
		double a = Utils.fv(args[n++]);
		enew.add(new El(res, a));
	    }
	    this.enew = enew;
	}

	public void trig(Indir<Resource> ev) {
	    etr = (enew != null)?enew:els;
	    trev = ev;
	}

	private Tex rtip = null;
	public Object tooltip(Coord c, Widget prev) {
	    if(rtip == null) {
		List<El> els = this.els;
		BufferedImage cur = null;
		double sum = 0.0;
		for(El el : els) {
		    Event ev = el.res.get().flayer(Event.class);
		    Color col = Utils.blendcol(ev.col, Color.WHITE, 0.5);
		    BufferedImage ln = Text.render(String.format("%s: %s", ev.nm, Utils.odformat2(el.a, 2)), col).img;
		    Resource.Image icon = el.res.get().layer(Resource.imgc);
		    if(icon != null)
			ln = ItemInfo.catimgsh(5, convolve(icon.img, new Coord(ln.getHeight(), ln.getHeight()), iconfilter), ln);
		    cur = ItemInfo.catimgs(0, cur, ln);
		    sum += el.a;
		}
		cur = ItemInfo.catimgs(0, cur, Text.render(String.format("Total: %s/%s", Utils.odformat2(sum, 2), Utils.odformat(cap, 2))).img);
		rtip = new TexI(cur);
	    }
	    return(rtip);
	}
    }

    public static class GlutMeter extends Widget {
	public static final Tex frame = Resource.loadtex("gfx/hud/chr/glutm");
	public static final Coord marg = new Coord(5, 5);
	public Color fg = Color.BLACK, bg = Color.BLACK;
	public double glut, lglut, gmod;
	public String lbl;

	public GlutMeter() {
	    super(frame.sz());
	}

	public void draw(GOut g) {
	    Coord isz = sz.sub(marg.mul(2));
	    g.chcolor(bg);
	    g.frect(marg, isz);
	    g.chcolor(fg);
	    g.frect(marg, new Coord((int)Math.round(isz.x * (glut - Math.floor(glut))), isz.y));
	    g.chcolor();
	    g.image(frame, Coord.z);
	}

	public void update(Object... args) {
	    int a = 0;
	    this.glut = Utils.dv(args[a++]);
	    this.lglut = Utils.dv(args[a++]);
	    this.gmod = Utils.dv(args[a++]);
	    this.lbl = (String)args[a++];
	    this.bg = (Color)args[a++];
	    this.fg = (Color)args[a++];
	    rtip = null;
	}

	private Tex rtip = null;
	public Object tooltip(Coord c, Widget prev) {
	    if(rtip == null) {
		rtip = RichText.render(String.format("%s: %.1f\u2030\nFood efficacy: %d%%", lbl, glut * 1000, Math.round(gmod * 100)), -1).tex();
	    }
	    return(rtip);
	}
    }

    public BAttrWnd(Glob glob) {
	Widget prev;
	prev = add(CharWnd.settip(new Img(catf.render("Base Attributes").tex()), "gfx/hud/chr/tips/base"), Coord.z);
	attrs = new ArrayList<>();
	Attr aw;
	attrs.add(aw = add(new Attr(glob, "str", every), prev.pos("bl").adds(5, 0).add(wbox.btloff())));
	attrs.add(aw = add(new Attr(glob, "agi", other), aw.pos("bl")));
	attrs.add(aw = add(new Attr(glob, "int", every), aw.pos("bl")));
	attrs.add(aw = add(new Attr(glob, "con", other), aw.pos("bl")));
	attrs.add(aw = add(new Attr(glob, "prc", every), aw.pos("bl")));
	attrs.add(aw = add(new Attr(glob, "csm", other), aw.pos("bl")));
	attrs.add(aw = add(new Attr(glob, "dex", every), aw.pos("bl")));
	attrs.add(aw = add(new Attr(glob, "wil", other), aw.pos("bl")));
	attrs.add(aw = add(new Attr(glob, "psy", every), aw.pos("bl")));
	prev = Frame.around(this, attrs);
	prev = add(CharWnd.settip(new Img(catf.render("Food Event Points").tex()), "gfx/hud/chr/tips/fep"), prev.pos("bl").x(0).adds(0, 10));
	feps = add(new FoodMeter(), prev.pos("bl").adds(5, 2));

	int ah = attrs.get(attrs.size() - 1).pos("bl").y - attrs.get(0).pos("ul").y;
	prev = add(CharWnd.settip(new Img(catf.render("Food Satiations").tex()), "gfx/hud/chr/tips/constip"), width, 0);
	cons = add(new Constipations(Coord.of(attrw, ah)), prev.pos("bl").adds(5, 0).add(wbox.btloff()));
	prev = Frame.around(this, Collections.singletonList(cons));
	prev = add(CharWnd.settip(new Img(catf.render("Hunger Level").tex()), "gfx/hud/chr/tips/hunger"), prev.pos("bl").x(width).adds(0, 10));
	glut = add(new GlutMeter(), prev.pos("bl").adds(5, 2));
	pack();
    }

    public static Collection<String> msgs = Arrays.asList("food", "glut", "ftrig", "lvl", "const");
    public void uimsg(String nm, Object... args) {
	if(nm == "food") {
	    feps.update(args);
	} else if(nm == "glut") {
	    glut.update(args);
	} else if(nm == "ftrig") {
	    feps.trig(ui.sess.getres((Integer)args[0]));
	} else if(nm == "lvl") {
	    for(Attr aw : attrs) {
		if(aw.nm.equals(args[0]))
		    aw.lvlup();
	    }
	} else if(nm == "const") {
	    int a = 0;
	    while(a < args.length) {
		ResData t = new ResData(ui.sess.getres((Integer)args[a++]), MessageBuf.nil);
		if(args[a] instanceof byte[])
		    t.sdt = new MessageBuf((byte[])args[a++]);
		double m = Utils.dv(args[a++]);
		cons.update(t, m);
	    }
	} else {
	    super.uimsg(nm, args);
	}
    }
}
