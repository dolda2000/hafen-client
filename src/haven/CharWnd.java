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
    public static final Color tbuff = new Color(128, 128, 255);
    public static final Color every = new Color(255, 255, 255, 16), other = new Color(255, 255, 255, 32);
    public final Collection<Attr> base;
    public final Collection<SAttr> skill;
    public final FoodMeter feps;
    public final Constipations cons;
    public final SkillList csk, nsk;
    public final ExperienceList exps;
    public int exp;
    private int scost;
    private final Tabs.Tab sattr;

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
		} catch(Loading l) {}
		enew = null;
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

	private Tex rtip = null;
	public Object tooltip(Coord c, Widget prev) {
	    List<El> els = this.els;
	    if(els.isEmpty())
		return(null);
	    if(rtip == null) {
		BufferedImage cur = null;
		for(El el : els) {
		    Event ev = el.res.get().layer(Event.class);
		    BufferedImage ln = Text.render(String.format("%s: %s", ev.nm, Utils.odformat2(el.a, 2)), ev.col).img;
		    Resource.Image icon = el.res.get().layer(Resource.imgc);
		    if(icon != null)
			ln = ItemInfo.catimgsh(5, icon.img, ln);
		    cur = ItemInfo.catimgs(0, cur, ln);
		}
		rtip = new TexI(cur);
	    }
	    return(rtip);
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

	public Constipations(int w, int h) {
	    super(w, h, El.h);
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
	private int cbv, ccv;

	private Attr(Glob glob, String attr, Color bg) {
	    super(new Coord(attrw, attrf.height() + 2));
	    Resource res = Resource.load("gfx/hud/chr/" + attr).loadwait();
	    this.nm = attr;
	    this.img = res.layer(Resource.imgc).tex();
	    this.rnm = attrf.render(res.layer(Resource.tooltip).t);
	    this.attr = glob.cattr.get(attr);
	    this.bg = bg;
	}

	public void tick(double dt) {
	    if((attr.base != cbv) || (attr.comp != ccv)) {
		cbv = attr.base; ccv = attr.comp;
		Color c = Color.WHITE;
		if(ccv > cbv) {
		    c = buff;
		    tooltip = Text.render(String.format("%d + %d", cbv, ccv - cbv));
		} else if(ccv < cbv) {
		    c = debuff;
		    tooltip = Text.render(String.format("%d - %d", cbv, cbv - ccv));
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
	    g.aimage(rnm.tex(), cn.add(img.sz().x + 10, 1), 0, 0.5);
	    g.aimage(ct.tex(), cn.add(sz.x - 7, 1), 1, 0.5);
	}

	public void lvlup() {
	    lvlt = 1.0;
	}
    }

    public class SAttr extends Widget {
	public final String nm;
	public final Text rnm;
	public final Glob.CAttr attr;
	public final Tex img;
	public final Color bg;
	public int tbv, tcv, cost;
	private Text ct;
	private int cbv, ccv;

	private SAttr(Glob glob, String attr, Color bg) {
	    super(new Coord(attrw, attrf.height() + 2));
	    Resource res = Resource.load("gfx/hud/chr/" + attr).loadwait();
	    this.nm = attr;
	    this.img = res.layer(Resource.imgc).tex();
	    this.rnm = attrf.render(res.layer(Resource.tooltip).t);
	    this.attr = glob.cattr.get(attr);
	    this.bg = bg;
	    adda(new IButton("gfx/hud/buttons/add", "u", "d", null) {
		    public void click() {adj(1);}
		}, sz.x - 5, sz.y / 2, 1, 0.5);
	    adda(new IButton("gfx/hud/buttons/sub", "u", "d", null) {
		    public void click() {adj(-1);}
		    public boolean mousewheel(Coord c, int a) {adj(-a); return(true);}
		}, sz.x - 20, sz.y / 2, 1, 0.5);
	}

	public void tick(double dt) {
	    if((attr.base != cbv) || (attr.comp != ccv)) {
		cbv = attr.base; ccv = attr.comp;
		if(tbv <= cbv) {
		    tbv = cbv; tcv = ccv;
		    updcost();
		}
		Color c = Color.WHITE;
		if(ccv > cbv) {
		    c = buff;
		    tooltip = Text.render(String.format("%d + %d", cbv, ccv - cbv));
		} else if(ccv < cbv) {
		    c = debuff;
		    tooltip = Text.render(String.format("%d - %d", cbv, cbv - ccv));
		} else {
		    tooltip = null;
		}
		if(tcv > ccv)
		    c = tbuff;
		ct = attrf.render(Integer.toString(tcv), c);
	    }
	}

	public void draw(GOut g) {
	    g.chcolor(bg);
	    g.frect(Coord.z, sz);
	    g.chcolor();
	    super.draw(g);
	    Coord cn = new Coord(0, sz.y / 2);
	    g.aimage(img, cn.add(5, 0), 0, 0.5);
	    g.aimage(rnm.tex(), cn.add(img.sz().x + 10, 1), 0, 0.5);
	    g.aimage(ct.tex(), cn.add(sz.x - 40, 1), 1, 0.5);
	}

	private void updcost() {
	    int cost = 100 * ((tbv + (tbv * tbv)) - (attr.base + (attr.base * attr.base))) / 2;
	    scost += cost - this.cost;
	    this.cost = cost;
	}

	public void adj(int a) {
	    if(tbv + a < attr.base) a = attr.base - tbv;
	    tbv += a; tcv += a;
	    cbv = ccv = 0;
	    updcost();
	}

	public boolean mousewheel(Coord c, int a) {
	    adj(-a);
	    return(true);
	}
    }

    public static class RLabel extends Label {
	private Coord oc;

	public RLabel(Coord oc, String text) {
	    super(text);
	    this.oc = oc;
	}

	protected void added() {
	    this.c = oc.add(-sz.x, 0);
	}

	public void settext(String text) {
	    super.settext(text);
	    this.c = oc.add(-sz.x, 0);
	}
    }

    public class ExpLabel extends RLabel {
	private int cexp;

	public ExpLabel(Coord oc) {
	    super(oc, "0");
	}

	public void draw(GOut g) {
	    super.draw(g);
	    if(exp != cexp)
		settext(Utils.thformat(cexp = exp));
	}
    }

    public class StudyInfo extends Widget {
	public Widget study;
	public int texp, tw;
	private final Text.UText<?> texpt = new Text.UText<Integer>(Text.std) {
	    public Integer value() {return(texp);}
	    public String text(Integer v) {return(Utils.thformat(v));}
	};
	private final Text.UText<?> twt = new Text.UText<Integer>(Text.std) {
	    public Integer value() {return(tw);}
	    public String text(Integer v) {return(v + "/" + ui.sess.glob.cattr.get("int").comp);}
	};

	private StudyInfo(Coord sz, Widget study) {
	    super(sz);
	    this.study = study;
	    add(new Label("Attention:"), 2, 2);
	    add(new Label("Learning points:"), 2, sz.y - 32);
	}

	private void upd() {
	    int texp = 0, tw = 0;
	    for(GItem item : study.children(GItem.class)) {
		try {
		    Curiosity ci = ItemInfo.find(Curiosity.class, item.info());
		    if(ci != null) {
			texp += ci.exp;
			tw += ci.mw;
		    }
		} catch(Loading l) {
		}
	    }
	    this.texp = texp; this.tw = tw;
	}

	public void draw(GOut g) {
	    upd();
	    super.draw(g);
	    g.chcolor(255, 192, 255, 255);
	    g.aimage(twt.get().tex(), new Coord(sz.x - 2, 17), 1.0, 0.0);
	    g.chcolor(192, 192, 255, 255);
	    g.aimage(texpt.get().tex(), sz.add(-2, -15), 1.0, 0.0);
	}
    }

    public static class LoadingTextBox extends RichTextBox {
	private Indir<String> text = null;

	public LoadingTextBox(Coord sz, String text, RichText.Foundry fnd) {super(sz, text, fnd);}
	public LoadingTextBox(Coord sz, String text, Object... attrs) {super(sz, text, attrs);}

	public void settext(Indir<String> text) {
	    this.text = text;
	}

	public void draw(GOut g) {
	    if(text != null) {
		try {
		    settext(text.get());
		    text = null;
		} catch(Loading l) {
		}
	    }
	    super.draw(g);
	}
    }

    private static final PUtils.Convolution iconfilter = new PUtils.Lanczos(3);
    public class Skill {
	public final String nm;
	public final Indir<Resource> res;
	public final int cost;
	private String sortkey;
	private Tex small;
	private final Text.UText<?> rnm = new Text.UText<String>(attrf) {
	    public String value() {
		try {
		    return(res.get().layer(Resource.tooltip).t);
		} catch(Loading l) {
		    return("...");
		}
	    }
	};

	private Skill(String nm, Indir<Resource> res, int cost) {
	    this.nm = nm;
	    this.res = res;
	    this.cost = cost;
	    this.sortkey = nm;
	}

	public String rendertext() {
	    StringBuilder buf = new StringBuilder();
	    Resource res = this.res.get();
	    buf.append("$img[" + res.name + "]\n\n");
	    buf.append("$b{$font[serif,16]{" + res.layer(Resource.tooltip).t + "}}\n\n\n");
	    buf.append("Cost: " + cost + "\n\n");
	    buf.append(res.layer(Resource.pagina).text);
	    return(buf.toString());
	}
    }

    public class Experience {
	public final Indir<Resource> res;
	public final int mtime;
	private String sortkey = "\uffff";
	private Tex small;
	private final Text.UText<?> rnm = new Text.UText<String>(attrf) {
	    public String value() {
		try {
		    return(res.get().layer(Resource.tooltip).t);
		} catch(Loading l) {
		    return("...");
		}
	    }
	};

	private Experience(Indir<Resource> res, int mtime) {
	    this.res = res;
	    this.mtime = mtime;
	}

	public String rendertext() {
	    StringBuilder buf = new StringBuilder();
	    Resource res = this.res.get();
	    buf.append("$img[" + res.name + "]\n\n");
	    buf.append("$b{$font[serif,16]{" + res.layer(Resource.tooltip).t + "}}\n\n\n");
	    buf.append(res.layer(Resource.pagina).text);
	    return(buf.toString());
	}
    }

    public static class SkillList extends Listbox<Skill> {
	public Skill[] skills = new Skill[0];
	private boolean loading = false;
	private final Comparator<Skill> skcomp = new Comparator<Skill>() {
	    public int compare(Skill a, Skill b) {
		return(a.sortkey.compareTo(b.sortkey));
	    }
	};

	public SkillList(int w, int h) {
	    super(w, h, attrf.height() + 2);
	}

	public void tick(double dt) {
	    if(loading) {
		loading = false;
		for(Skill sk : skills) {
		    try {
			sk.sortkey = sk.res.get().layer(Resource.tooltip).t;
		    } catch(Loading l) {
			sk.sortkey = sk.nm;
			loading = true;
		    }
		}
		Arrays.sort(skills, skcomp);
	    }
	}

	protected Skill listitem(int idx) {return(skills[idx]);}
	protected int listitems() {return(skills.length);}

	protected void drawbg(GOut g) {}

	protected void drawitem(GOut g, Skill sk, int idx) {
	    g.chcolor((idx % 2 == 0)?every:other);
	    g.frect(Coord.z, g.sz);
	    g.chcolor();
	    try {
		if(sk.small == null)
		    sk.small = new TexI(PUtils.convolvedown(sk.res.get().layer(Resource.imgc).img, new Coord(itemh, itemh), iconfilter));
		g.image(sk.small, Coord.z);
	    } catch(Loading e) {
		WItem.missing.loadwait();
		g.image(WItem.missing.layer(Resource.imgc).tex(), Coord.z, new Coord(itemh, itemh));
	    }
	    g.aimage(sk.rnm.get().tex(), new Coord(itemh + 5, itemh / 2), 0, 0.5);
	}

	public void pop(Collection<Skill> nsk) {
	    Skill[] skills = nsk.toArray(new Skill[0]);
	    sb.val = 0;
	    sb.max = skills.length - h;
	    Skill psel = sel;
	    sel = null;
	    this.skills = skills;
	    if(psel != null) {
		for(Skill sk : skills) {
		    if(sk.nm.equals(psel.nm)) {
			sel = sk;
			break;
		    }
		}
	    }
	    loading = true;
	}
    }

    public static class ExperienceList extends Listbox<Experience> {
	public Experience[] exps = new Experience[0];
	private boolean loading = false;
	private final Comparator<Experience> comp = new Comparator<Experience>() {
	    public int compare(Experience a, Experience b) {
		return(a.sortkey.compareTo(b.sortkey));
	    }
	};

	public ExperienceList(int w, int h) {
	    super(w, h, attrf.height() + 2);
	}

	public void tick(double dt) {
	    if(loading) {
		loading = false;
		for(Experience exp : exps) {
		    try {
			exp.sortkey = exp.res.get().layer(Resource.tooltip).t;
		    } catch(Loading l) {
			exp.sortkey = "\uffff";
			loading = true;
		    }
		}
		Arrays.sort(exps, comp);
	    }
	}

	protected Experience listitem(int idx) {return(exps[idx]);}
	protected int listitems() {return(exps.length);}

	protected void drawbg(GOut g) {}

	protected void drawitem(GOut g, Experience exp, int idx) {
	    g.chcolor((idx % 2 == 0)?every:other);
	    g.frect(Coord.z, g.sz);
	    g.chcolor();
	    try {
		if(exp.small == null)
		    exp.small = new TexI(PUtils.convolvedown(exp.res.get().layer(Resource.imgc).img, new Coord(itemh, itemh), iconfilter));
		g.image(exp.small, Coord.z);
	    } catch(Loading e) {
		WItem.missing.loadwait();
		g.image(WItem.missing.layer(Resource.imgc).tex(), Coord.z, new Coord(itemh, itemh));
	    }
	    g.aimage(exp.rnm.get().tex(), new Coord(itemh + 5, itemh / 2), 0, 0.5);
	}

	public void pop(Collection<Experience> nl) {
	    Experience[] exps = nl.toArray(new Experience[0]);
	    sb.val = 0;
	    sb.max = exps.length - h;
	    sel = null;
	    this.exps = exps;
	    loading = true;
	}
    }

    @RName("chr")
    public static class $_ implements Factory {
	public Widget create(Widget parent, Object[] args) {
	    return(new CharWnd(parent.ui.sess.glob));
	}
    }

    public CharWnd(Glob glob) {
	super(new Coord(300, 290), "Character Sheet");

	final Tabs tabs = new Tabs(new Coord(15, 10), Coord.z, this);
	Tabs.Tab battr;
	{ 
	    int x = 5, y = 0;

	    battr = tabs.add();
	    battr.add(new Img(catf.render("Base Attributes").tex()), new Coord(x - 5, y)); y += 35;
	    base = new ArrayList<Attr>();
	    Attr aw;
	    base.add(aw = battr.add(new Attr(glob, "str", every), wbox.btloff().add(x, y))); y += aw.sz.y;
	    base.add(aw = battr.add(new Attr(glob, "agi", other), wbox.btloff().add(x, y))); y += aw.sz.y;
	    base.add(aw = battr.add(new Attr(glob, "int", every), wbox.btloff().add(x, y))); y += aw.sz.y;
	    base.add(aw = battr.add(new Attr(glob, "con", other), wbox.btloff().add(x, y))); y += aw.sz.y;
	    base.add(aw = battr.add(new Attr(glob, "prc", every), wbox.btloff().add(x, y))); y += aw.sz.y;
	    base.add(aw = battr.add(new Attr(glob, "csm", other), wbox.btloff().add(x, y))); y += aw.sz.y;
	    base.add(aw = battr.add(new Attr(glob, "dex", every), wbox.btloff().add(x, y))); y += aw.sz.y;
	    base.add(aw = battr.add(new Attr(glob, "psy", other), wbox.btloff().add(x, y))); y += aw.sz.y;
	    Frame.around(battr, base);
	    y += 24;
	    battr.add(new Img(catf.render("Food Event Points").tex()), new Coord(x - 5, y)); y += 35;
	    feps = battr.add(new FoodMeter(), new Coord(x, y));

	    x = 260; y = 0;
	    battr.add(new Img(catf.render("Food Satiations").tex()), new Coord(x - 5, y)); y += 35;
	    cons = battr.add(new Constipations(attrw, base.size()), wbox.btloff().add(x, y));
	    Frame.around(battr, Collections.singletonList(cons));
	}

	{
	    int x = 5, y = 0;

	    sattr = tabs.add();
	    sattr.add(new Img(catf.render("Skill Values").tex()), new Coord(x - 5, y)); y += 35;
	    skill = new ArrayList<SAttr>();
	    SAttr aw;
	    skill.add(aw = sattr.add(new SAttr(glob, "unarmed", every), wbox.btloff().add(x, y))); y += aw.sz.y;
	    skill.add(aw = sattr.add(new SAttr(glob, "ranged", every), wbox.btloff().add(x, y))); y += aw.sz.y;
	    skill.add(aw = sattr.add(new SAttr(glob, "explore", other), wbox.btloff().add(x, y))); y += aw.sz.y;
	    Frame.around(sattr, skill);

	    x = 260; y = 0;
	    sattr.add(new Img(catf.render("Study Report").tex()), new Coord(x - 5, y)); y += 35;
	    y += 156;
	    int rx = x + attrw - 10;
	    Frame.around(sattr, Area.sized(new Coord(x, y).add(wbox.btloff()), new Coord(attrw, 75)));
	    sattr.add(new Label("Learning points:"), new Coord(x + 15, y + 10));
	    sattr.add(new ExpLabel(new Coord(rx, y + 10)));
	    sattr.add(new Label("Cost:"), new Coord(x + 15, y + 25));
	    sattr.add(new RLabel(new Coord(rx, y + 25), "0") {
		    int cc;

		    public void draw(GOut g) {
			if(cc > exp)
			    g.chcolor(debuff);
			super.draw(g);
			if(cc != scost)
			    settext(Utils.thformat(cc = scost));
		    }
		});
	    sattr.add(new Button(75, "Buy") {
		    public void click() {
			ArrayList<Object> args = new ArrayList<Object>();
			for(SAttr attr : skill) {
			    if(attr.tbv > attr.attr.base) {
				args.add(attr.attr.nm);
				args.add(attr.tbv);
			    }
			}
			CharWnd.this.wdgmsg("sattr", args.toArray(new Object[0]));
		    }
		}, new Coord(rx - 75, y + 50));
	}

	Tabs.Tab skills;
	{
	    int x = 5, y = 0;

	    skills = tabs.add();
	    skills.add(new Img(catf.render("Lore & Skills").tex()), new Coord(x - 5, y)); y += 35;
	    RichText.Foundry ifnd = new RichText.Foundry(java.awt.font.TextAttribute.FAMILY, "SansSerif", java.awt.font.TextAttribute.SIZE, 9).aa(true);
	    final LoadingTextBox info = skills.add(new LoadingTextBox(new Coord(attrw, 260), "", ifnd), new Coord(x, y).add(wbox.btloff()));
	    info.bg = new Color(0, 0, 0, 128);
	    Frame.around(skills, Collections.singletonList(info));

	    x = 260; y = 0;
	    skills.add(new Img(catf.render("Entries").tex()), new Coord(x - 5, y)); y += 35;
	    Tabs lists = new Tabs(new Coord(x, y), new Coord(attrw + wbox.bisz().x, 0), skills);
	    Tabs.Tab nsk = lists.add();
	    {
		this.nsk = nsk.add(new SkillList(lists.sz.x - wbox.bisz().x, 7) {
			public void change(final Skill sk) {
			    Skill p = sel;
			    super.change(sk);
			    CharWnd.this.csk.sel = null;
			    CharWnd.this.exps.sel = null;
			    if(sk != null)
				info.settext(new Indir<String>() {public String get() {return(sk.rendertext());}});
			    else if(p != null)
				info.settext("");
			}
		    }, wbox.btloff());
		y = Frame.around(nsk, Collections.singletonList(this.nsk)).sz.y + 5;
		int rx = attrw - 10;
		Frame.around(nsk, Area.sized(new Coord(0, y).add(wbox.btloff()), new Coord(attrw, 69)));
		nsk.add(new Label("Learning points:"), new Coord(15, y + 10));
		nsk.add(new ExpLabel(new Coord(rx, y + 10)));
		nsk.add(new Label("Cost:"), new Coord(15, y + 25));
		nsk.add(new RLabel(new Coord(rx, y + 25), "N/A") {
			Integer cc = null;

			public void draw(GOut g) {
			    if((cc != null) && (cc > exp))
				g.chcolor(debuff);
			    super.draw(g);
			    if((CharWnd.this.nsk.sel == null) && (cc != null)) {
				settext("N/A");
				cc = null;
			    } else if((CharWnd.this.nsk.sel != null) && (cc == null)) {
				settext(Utils.thformat(cc = CharWnd.this.nsk.sel.cost));
			    }
			}
		    });
		nsk.add(new Button(75, "Buy") {
			public void click() {
			    if(CharWnd.this.nsk.sel != null)
				CharWnd.this.wdgmsg("buy", CharWnd.this.nsk.sel.nm);
			}
		    }, new Coord(rx - 75, y + 44));
	    }
	    Tabs.Tab csk = lists.add();
	    {
		this.csk = csk.add(new SkillList(lists.sz.x - wbox.bisz().x, 11) {
			public void change(final Skill sk) {
			    Skill p = sel;
			    super.change(sk);
			    CharWnd.this.nsk.sel = null;
			    CharWnd.this.exps.sel = null;
			    if(sk != null)
				info.settext(new Indir<String>() {public String get() {return(sk.rendertext());}});
			    else if(p != null)
				info.settext("");
			}
		    }, wbox.btloff());
		Frame.around(csk, Collections.singletonList(this.csk));
	    }
	    Tabs.Tab exps = lists.add();
	    {
		this.exps = exps.add(new ExperienceList(lists.sz.x - wbox.bisz().x, 11) {
			public void change(final Experience exp) {
			    Experience p = sel;
			    super.change(exp);
			    CharWnd.this.nsk.sel = null;
			    CharWnd.this.csk.sel = null;
			    if(exp != null)
				info.settext(new Indir<String>() {public String get() {return(exp.rendertext());}});
			    else if(p != null)
				info.settext("");
			}
		    }, wbox.btloff());
		Frame.around(exps, Collections.singletonList(this.exps));
	    }
	    lists.pack();
	    int bw = (lists.sz.x + 5) / 3;
	    x = lists.c.x;
	    y = lists.c.y + lists.sz.y + 5;
	    skills.add(lists.new TabButton(bw - 5, "Available", nsk), new Coord(x, y));
	    skills.add(lists.new TabButton(bw - 5, "Current", csk), new Coord(x + bw, y));
	    skills.add(lists.new TabButton(bw - 5, "Lore", exps), new Coord(x + bw * 2, y));
	}

	{
	    Widget prev;

	    class TB extends IButton {
		final Tabs.Tab tab;
		TB(String nm, Tabs.Tab tab) {
		    super(Resource.loadimg("gfx/hud/chr/" + nm + "u"), Resource.loadimg("gfx/hud/chr/" + nm + "d"));
		    this.tab = tab;
		}

		public void click() {
		    tabs.showtab(tab);
		}
	    }

	    tabs.pack();
	    prev = add(new TB("battr", battr), new Coord(tabs.c.x + 5, tabs.c.y + tabs.sz.y + 10));
	    prev.settip("Base Attributes");
	    prev = add(new TB("sattr", sattr), new Coord(prev.c.x + prev.sz.x + 10, prev.c.y));
	    prev.settip("Skill Values");
	    prev = add(new TB("skill", skills), new Coord(prev.c.x + prev.sz.x + 10, prev.c.y));
	    prev.settip("Lore & Skills");
	}

	resize(contentsz().add(15, 10));
    }

    public void addchild(Widget child, Object... args) {
	String place = (args[0] instanceof String)?(((String)args[0]).intern()):null;
	if(place == "study") {
	    sattr.add(child, new Coord(260, 35).add(wbox.btloff()));
	    Frame.around(sattr, Collections.singletonList(child));
	    Widget inf = sattr.add(new StudyInfo(new Coord(attrw - 150, child.sz.y), child), new Coord(260 + 150, child.c.y).add(wbox.btloff().x, 0));
	    Frame.around(sattr, Collections.singletonList(inf));
	} else {
	    super.addchild(child, args);
	}
    }

    private void decsklist(Collection<Skill> buf, Object[] args, int a) {
	while(a < args.length) {
	    String nm = (String)args[a++];
	    Indir<Resource> res = ui.sess.getres((Integer)args[a++]);
	    int cost = ((Number)args[a++]).intValue();
	    buf.add(new Skill(nm, res, cost));
	}
    }

    private void decexplist(Collection<Experience> buf, Object[] args, int a) {
	while(a < args.length) {
	    Indir<Resource> res = ui.sess.getres((Integer)args[a++]);
	    int mtime = ((Number)args[a++]).intValue();
	    buf.add(new Experience(res, mtime));
	}
    }

    public void uimsg(String nm, Object... args) {
	if(nm == "exp") {
	    exp = ((Number)args[0]).intValue();
	} else if(nm == "food") {
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
	} else if(nm == "csk") {
	    /* One *could* argue that rmessages should have some
	     * built-in fragmentation scheme. ^^ */
	    boolean rst = ((Integer)args[0]) != 0;
	    Collection<Skill> buf = rst?new ArrayList<Skill>():new ArrayList<Skill>(Arrays.asList(csk.skills));
	    decsklist(buf, args, 1);
	    csk.pop(buf);
	} else if(nm == "nsk") {
	    boolean rst = ((Integer)args[0]) != 0;
	    Collection<Skill> buf = rst?new ArrayList<Skill>():new ArrayList<Skill>(Arrays.asList(nsk.skills));
	    decsklist(buf, args, 1);
	    nsk.pop(buf);
	} else if(nm == "exps") {
	    boolean rst = ((Integer)args[0]) != 0;
	    Collection<Experience> buf = rst?new ArrayList<Experience>():new ArrayList<Experience>(Arrays.asList(exps.exps));
	    decexplist(buf, args, 1);
	    exps.pop(buf);
	} else {
	    super.uimsg(nm, args);
	}
    }
}
