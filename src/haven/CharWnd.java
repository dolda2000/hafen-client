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
import java.awt.font.TextAttribute;
import java.util.*;
import static haven.Window.wbox;
import static haven.PUtils.*;

public class CharWnd extends Window {
    public static final Text.Furnace catf = new BlurFurn(new TexFurn(new Text.Foundry(Text.fraktur, 25).aa(true), Window.ctex), 3, 2, new Color(96, 48, 0));
    public static final Text.Foundry attrf = new Text.Foundry(Text.fraktur, 18).aa(true);
    public static final Color debuff = new Color(255, 128, 128);
    public static final Color buff = new Color(128, 255, 128);
    public static final Color every = new Color(255, 255, 255, 16), other = new Color(255, 255, 255, 32);
    public final Collection<Attr> base;
    public final FoodMeter feps;
    public final Constipations cons;

    public static class FoodMeter extends Widget {
	public static final Tex frame = Resource.loadtex("gfx/hud/chr/foodm");
	public static final Coord marg = new Coord(5, 5), trmg = new Coord(10, 10);
	public double cap;
	public List<El> els = new LinkedList<El>();
	private List<El> enew = null, etr = null;
	private Indir<Resource> trev = null;
	private Tex trol;
	private long trtm = 0;

	@Resource.LayerName("foodev")
	public static class Event extends Resource.Layer {
	    public final Color col;
	    public final String nm;
	    public final int sort;

	    public Event(Resource res, byte[] bbuf) {
		res.super();
		Message buf = new Message(0, bbuf);
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

	public FoodMeter(Coord c, Widget parent) {
	    super(c, frame.sz(), parent);
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
		} catch(Loading l) {}
	    }
	    if(trev != null) {
		try {
		    Collections.sort(etr, dcmp);
		    trol = new TexI(mktrol(etr, trev));
		    trtm = System.currentTimeMillis();
		    trev = null;
		} catch(Loading l) {}
	    }
	}

	public void draw(GOut g) {
	    int d = (trtm > 0)?((int)(System.currentTimeMillis() - trtm)):Integer.MAX_VALUE;
	    g.chcolor(0, 0, 0, 255);
	    g.frect(marg, sz.sub(marg.mul(2)));
	    drawels(g, els, 255);
	    if(d < 1000)
		drawels(g, etr, 255 - ((d * 255) / 1000));
	    g.chcolor();
	    g.image(frame, Coord.z);
	    if(d < 2500) {
		GOut g2 = g.reclipl(trmg.inv(), sz.add(trmg.mul(2)));
		g2.chcolor(255, 255, 255, 255 - ((d * 255) / 2500));
		g2.image(trol, Coord.z);
	    } else {
		trtm = 0;
	    }
	}

	public void update(Object... args) {
	    int n = 0;
	    this.cap = (Float)args[n++];
	    List<El> enew = new LinkedList<El>();
	    while(n < args.length) {
		Indir<Resource> res = ui.sess.getres((Integer)args[n++]);
		double a = (Float)args[n++];
		enew.add(new El(res, a));
	    }
	    this.enew = enew;
	}

	public void trig(Indir<Resource> ev) {
	    etr = (enew != null)?enew:els;
	    trev = ev;
	}
    }

    public static class Constipations extends Listbox<Constipations.El> {
	public static final Text.Foundry elf = attrf;
	public static final Convolution tflt = new Hanning(1);
	public static final Color full = new Color(250, 230, 64), none = new Color(250, 19, 43);
	public final List<El> els = new ArrayList<El>();

	public static class El {
	    public static final int h = elf.height() + 2;
	    public final Indir<Resource> t;
	    public double a;
	    private Tex tt, at;

	    public El(Indir<Resource> t, double a) {this.t = t; this.a = a;}
	    public void update(double a) {this.a = a; at = null;}

	    public Tex tt() {
		if(tt == null) {
		    BufferedImage img = t.get().layer(Resource.imgc).img;
		    String nm = t.get().layer(Resource.tooltip).t;
		    Text rnm = elf.render(nm);
		    BufferedImage buf = TexI.mkbuf(new Coord(El.h + 5 + rnm.sz().x, h));
		    Graphics g = buf.getGraphics();
		    g.drawImage(convolvedown(img, new Coord(h, h), tflt), 0, 0, null);
		    g.drawImage(rnm.img, h + 5, ((h - rnm.sz().y) / 2) + 1, null);
		    g.dispose();
		    tt = new TexI(buf);
		}
		return(tt);
	    }

	    public Tex at() {
		if(at == null)
		    at = elf.render(String.format("%d%%", (int)Math.round(a * 100)), Utils.blendcol(none, full, a)).tex();
		return(at);
	    }
	}

	public static final Comparator<El> ecmp = new Comparator<El>() {
	    public int compare(El a, El b) {
		if(a.a < b.a)
		    return(-1);
		else if(a.a > b.a)
		    return(1);
		return(0);
	    }
	};

	public Constipations(Coord c, Widget parent, int w, int h) {
	    super(c, parent, w, h, El.h);
	}

	protected void drawbg(GOut g) {}
	protected El listitem(int i) {return(els.get(i));}
	protected int listitems() {return(els.size());}

	protected void drawitem(GOut g, El el, int idx) {
	    g.chcolor(((idx % 2) == 0)?every:other);
	    g.frect(Coord.z, g.sz);
	    g.chcolor();
	    try {
		g.image(el.tt(), Coord.z);
	    } catch(Loading e) {}
	    Tex at = el.at();
	    g.image(at, new Coord(sz.x - at.sz().x, (El.h - at.sz().y) / 2));
	}

	public void update(Indir<Resource> t, double a) {
	    prev: {
		for(Iterator<El> i = els.iterator(); i.hasNext();) {
		    El el = i.next();
		    if(el.t != t)
			continue;
		    if(a == 1.0)
			i.remove();
		    else
			el.update(a);
		    break prev;
		}
		els.add(new El(t, a));
	    }
	    Collections.sort(els, ecmp);
	}

	public boolean mousedown(Coord c, int button) {
	    return(false);
	}
    }

    public static final int attrw = FoodMeter.frame.sz().x - wbox.bisz().x;
    public class Attr extends Widget {
	public final String nm;
	public final Text rnm;
	public final Glob.CAttr attr;
	public final Tex img;
	public final Color bg;
	private double lvlt = 0.0;
	private Text ct;
	private int cv;

	private Attr(String attr, String rnm, Coord c, Color bg) {
	    super(c, new Coord(attrw, attrf.height() + 2), CharWnd.this);
	    this.nm = attr;
	    this.img = Resource.load("gfx/hud/chr/" + attr).loadwait().layer(Resource.imgc).tex();
	    this.rnm = attrf.render(rnm);
	    this.attr = ui.sess.glob.cattr.get(attr);
	    this.bg = bg;
	}

	public void tick(double dt) {
	    if(attr.comp != cv) {
		cv = attr.comp;
		Color c = Color.WHITE;
		if(attr.comp > attr.base) {
		    c = buff;
		    tooltip = Text.render(String.format("%d + %d", attr.base, attr.comp - attr.base));
		} else if(attr.comp < attr.base) {
		    c = debuff;
		    tooltip = Text.render(String.format("%d - %d", attr.base, attr.base - attr.comp));
		} else {
		    tooltip = null;
		}
		ct = attrf.render(Integer.toString(cv), c);
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
	    g.aimage(rnm.tex(), cn.add(img.sz().x + 10, 1), 0, 0.5);
	    g.aimage(ct.tex(), cn.add(sz.x - 7, 1), 1, 0.5);
	}

	public void lvlup() {
	    lvlt = 1.0;
	}
    }

    @RName("chr")
    public static class $_ implements Factory {
	public Widget create(Coord c, Widget parent, Object[] args) {
	    return(new CharWnd(c, parent));
	}
    }

    public CharWnd(Coord pc, Widget parent) {
	super(pc, new Coord(300, 290), parent, "Character Sheet");

	int x = 15, y = 10;
	new Img(new Coord(x - 5, y), catf.render("Base Attributes").tex(), this); y += 35;
	base = new ArrayList<Attr>();
	Attr aw;
	base.add(aw = new Attr("str", "Strength",     wbox.btloff().add(x, y), every)); y += aw.sz.y;
	base.add(aw = new Attr("agi", "Agility",      wbox.btloff().add(x, y), other)); y += aw.sz.y;
	base.add(aw = new Attr("int", "Intelligence", wbox.btloff().add(x, y), every)); y += aw.sz.y;
	base.add(aw = new Attr("con", "Constitution", wbox.btloff().add(x, y), other)); y += aw.sz.y;
	base.add(aw = new Attr("prc", "Perception",   wbox.btloff().add(x, y), every)); y += aw.sz.y;
	base.add(aw = new Attr("csm", "Charisma",     wbox.btloff().add(x, y), other)); y += aw.sz.y;
	base.add(aw = new Attr("dex", "Dexterity",    wbox.btloff().add(x, y), every)); y += aw.sz.y;
	base.add(aw = new Attr("psy", "Psyche",       wbox.btloff().add(x, y), other)); y += aw.sz.y;
	Frame.around(this, base);
	y += 20;
	new Img(new Coord(x - 5, y), catf.render("Food Event Points").tex(), this); y += 35;
	feps = new FoodMeter(new Coord(x, y), this);

	x = 275; y = 10;
	new Img(new Coord(x - 5, y), catf.render("Food Satiations").tex(), this); y += 35;
	cons = new Constipations(wbox.btloff().add(x, y), this, attrw, base.size());
	Frame.around(this, Collections.singletonList(cons));

	resize(contentsz().add(15, 10));
    }

    public void uimsg(String nm, Object... args) {
	if(nm == "food") {
	    feps.update(args);
	} else if(nm == "ftrig") {
	    feps.trig(ui.sess.getres((Integer)args[0]));
	} else if(nm == "lvl") {
	    for(Attr aw : base) {
		if(aw.nm.equals(args[0]))
		    aw.lvlup();
	    }
	} else if(nm == "const") {
	    int a = 0;
	    while(a < args.length) {
		Indir<Resource> t = ui.sess.getres((Integer)args[a++]);
		double m = ((Number)args[a++]).doubleValue();
		cons.update(t, m);
	    }
	} else {
	    super.uimsg(nm, args);
	}
    }
}
