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
import static haven.CharWnd.*;
import static haven.PUtils.*;

public class SkillWnd extends Widget {
    public final SkillGrid skg;
    public final CredoGrid credos;
    public final ExpGrid exps;
    private CharWnd chr;

    public class Skill {
	public final String nm;
	public final Indir<Resource> res;
	public final int cost;
	public boolean has = false;
	private String sortkey;
	private Tex small;

	private Skill(String nm, Indir<Resource> res, int cost, boolean has) {
	    this.nm = nm;
	    this.res = res;
	    this.cost = cost;
	    this.has = has;
	    this.sortkey = nm;
	}

	public String rendertext() {
	    StringBuilder buf = new StringBuilder();
	    Resource res = this.res.get();
	    buf.append("$img[" + res.name + "]\n\n");
	    buf.append("$b{$font[serif,16]{" + res.flayer(Resource.tooltip).t + "}}\n\n\n");
	    if(cost > 0)
		buf.append("Cost: " + cost + "\n\n");
	    buf.append(res.flayer(Resource.pagina).text);
	    return(buf.toString());
	}

	private Text tooltip = null;
	public Text tooltip() {
	    if(tooltip == null)
		tooltip = Text.render(res.get().flayer(Resource.tooltip).t);
	    return(tooltip);
	}
    }

    public class Credo {
	public final String nm;
	public final Indir<Resource> res;
	public boolean has = false;
	private String sortkey;
	private Tex small;

	private Credo(String nm, Indir<Resource> res, boolean has) {
	    this.nm = nm;
	    this.res = res;
	    this.has = has;
	    this.sortkey = nm;
	}

	public String rendertext() {
	    StringBuilder buf = new StringBuilder();
	    Resource res = this.res.get();
	    buf.append("$img[" + res.name + "]\n\n");
	    buf.append("$b{$font[serif,16]{" + res.flayer(Resource.tooltip).t + "}}\n\n\n");
	    buf.append(res.flayer(Resource.pagina).text);
	    return(buf.toString());
	}

	private Text tooltip = null;
	public Text tooltip() {
	    if(tooltip == null)
		tooltip = Text.render(res.get().flayer(Resource.tooltip).t);
	    return(tooltip);
	}
    }

    public class Experience {
	public final Indir<Resource> res;
	public final int mtime, score;
	private String sortkey = "\uffff";
	private Tex small;

	private Experience(Indir<Resource> res, int mtime, int score) {
	    this.res = res;
	    this.mtime = mtime;
	    this.score = score;
	}

	public String rendertext() {
	    StringBuilder buf = new StringBuilder();
	    Resource res = this.res.get();
	    buf.append("$img[" + res.name + "]\n\n");
	    buf.append("$b{$font[serif,16]{" + res.flayer(Resource.tooltip).t + "}}\n\n\n");
	    if(score > 0)
		buf.append("Experience points: " + Utils.thformat(score) + "\n\n");
	    buf.append(res.flayer(Resource.pagina).text);
	    return(buf.toString());
	}

	private Text tooltip = null;
	public Text tooltip() {
	    if(tooltip == null)
		tooltip = Text.render(res.get().flayer(Resource.tooltip).t);
	    return(tooltip);
	}
    }

    public class SkillGrid extends GridList<Skill> {
	public final Group nsk, csk;
	private boolean loading = false;

	public SkillGrid(Coord sz) {
	    super(sz);
	    nsk = new Group(UI.scale(40, 40), new Coord(-1, 5), "Available Skills", Collections.emptyList());
	    csk = new Group(UI.scale(40, 40), new Coord(-1, 5), "Known Skills", Collections.emptyList());
	    itemtooltip = Skill::tooltip;
	}

	protected void drawitem(GOut g, Skill sk) {
	    if(sk.small == null)
		sk.small = new TexI(convolvedown(sk.res.get().flayer(Resource.imgc).img, UI.scale(40, 40), iconfilter));
	    g.image(sk.small, Coord.z);
	}

	protected void update() {
	    super.update();
	    loading = true;
	}

	private void sksort(List<Skill> skills) {
	    for(Skill sk : skills) {
		try {
		    sk.sortkey = sk.res.get().flayer(Resource.tooltip).t;
		} catch(Loading l) {
		    sk.sortkey = sk.nm;
		    loading = true;
		}
	    }
	    Collections.sort(skills, (a, b) -> a.sortkey.compareTo(b.sortkey));
	}

	public void tick(double dt) {
	    super.tick(dt);
	    if(loading) {
		loading = false;
		sksort(nsk.items);
		sksort(csk.items);
	    }
	}
    }

    public class CredoGrid extends Scrollport {
	public final Coord crsz = UI.scale(70, 88);
	public final int btnw = UI.scale(100);
	public final Tex credoufr = new TexI(convolvedown(Resource.loadimg("gfx/hud/chr/yrkirframe"), crsz, iconfilter));
	public final Tex credosfr = new TexI(convolvedown(Resource.loadimg("gfx/hud/chr/yrkirsframe"), crsz, iconfilter));
	public final Text.Foundry prsf = new Text.Foundry(Text.fraktur, 15).aa(true);
	public final int m = UI.scale(5);
	public List<Credo> ncr = Collections.emptyList(), ccr = Collections.emptyList();
	public Credo pcr = null;
	public int pcl, pclt, pcql, pcqlt, pqid, cost;
	public Credo sel = null;
	private final Img pcrc, ncrc, ccrc;
	private final Button pbtn, qbtn;
	private boolean loading = false;

	public CredoGrid(Coord sz) {
	    super(sz);
	    pcrc = new Img(GridList.dcatf.render("Pursuing").tex());
	    ncrc = new Img(GridList.dcatf.render("Credos Available").tex());
	    ccrc = new Img(GridList.dcatf.render("Credos Acquired").tex());
	    pbtn = new Button(btnw, "Pursue", false) {
		    public void click() {
			if(sel != null)
			    SkillWnd.this.wdgmsg("crpursue", sel.nm);
		    }
		};
	    qbtn = new Button(btnw, "Show quest", false) {
		    public void click() {
			SkillWnd.this.wdgmsg("qsel", pqid);
			getparent(CharWnd.class).questtab.showtab();
		    }
		};
	}

	private Tex crtex(Credo cr) {
	    if(cr.small == null)
		cr.small = new TexI(convolvedown(cr.res.get().flayer(Resource.imgc).img, crsz, iconfilter));
	    return(cr.small);
	}

	private class CredoImg extends Img {
	    private final Credo cr;

	    CredoImg(Credo cr) {
		super(crtex(cr));
		this.cr = cr;
		this.tooltip = Text.render(cr.res.get().flayer(Resource.tooltip).t);
	    }

	    public void draw(GOut g) {
		super.draw(g);
		g.image((cr == sel) ? credosfr : credoufr, Coord.z);
	    }

	    public boolean mousedown(Coord c, int button) {
		if(button == 1) {
		    change(cr);
		}
		return(true);
	    }
	}

	private int crgrid(int y, Collection<Credo> crs) {
	    int col = 0;
	    for(Credo cr : crs) {
		if(col >= 3) {
		    col = 0;
		    y += crsz.y + m;
		}
		cont.add(new CredoImg(cr), col * (crsz.x + m) + m, y);
		col++;
	    }
	    return(y + crsz.y + m);
	}

	private void sort(List<Credo> buf) {
	    Collections.sort(buf, Comparator.comparing(cr -> cr.res.get().flayer(Resource.tooltip).t));
	}

	private void update() {
	    sort(ccr); sort(ncr);
	    for(Widget ch = cont.child; ch != null; ch = cont.child)
		ch.destroy();
	    int y = 0;
	    if(pcr != null) {
		cont.add(pcrc, m, y);
		y += pcrc.sz.y + m;
		Widget pcrim = cont.add(new CredoImg(pcr), m, y);
		cont.add(new Label(String.format("Level: %d/%d", pcl, pclt), prsf), pcrim.c.x + pcrim.sz.x + m, y);
		cont.add(new Label(String.format("Quest: %d/%d", pcql, pcqlt), prsf), pcrim.c.x + pcrim.sz.x + m, y + UI.scale(20));
		cont.adda(qbtn, pcrim.c.x + pcrim.sz.x + m, y + pcrim.sz.y, 0, 1);
		y += pcrim.sz.y;
		y += UI.scale(10);
	    }

	    if(ncr.size() > 0) {
		cont.add(ncrc, m, y);
		y += ncrc.sz.y + 5;
		y = crgrid(y, ncr);
		if(pcr == null) {
		    cont.add(pbtn, m, y);
		    if(cost > 0)
			cont.adda(new Label(String.format("Cost: %,d LP", cost)), pbtn.c.x + pbtn.sz.x + UI.scale(10), pbtn.c.y + (pbtn.sz.y / 2), 0, 0.5);
		    y += pbtn.sz.y;
		}
		y += UI.scale(10);
	    }

	    if(ccr.size() > 0) {
		cont.add(ccrc, m, y);
		y += ccrc.sz.y + m;
		y = crgrid(y, ccr);
	    }
	    cont.update();
	}

	public void tick(double dt) {
	    if(loading) {
		loading = false;
		try {
		    update();
		} catch(Loading l) {
		    loading = true;
		}
	    }
	}

	public void change(Credo cr) {
	    sel = cr;
	}

	public void pcr(Credo cr, int crl, int crlt, int crql, int crqlt, int qid) {
	    this.pcr = cr;
	    this.pcl = crl;
	    this.pclt = crlt;
	    this.pcql = crql;
	    this.pcqlt = crqlt;
	    this.pqid = qid;
	    loading = true;
	}

	public void ncr(List<Credo> cr) {
	    this.ncr = cr;
	    loading = true;
	}

	public void ccr(List<Credo> cr) {
	    this.ccr = cr;
	    loading = true;
	}

	public boolean mousedown(Coord c, int button) {
	    if(super.mousedown(c, button))
		return(true);
	    change(null);
	    return(true);
	}
    }

    public class ExpGrid extends GridList<Experience> {
	public final Group seen;
	private boolean loading = false;

	public ExpGrid(Coord sz) {
	    super(sz);
	    seen = new Group(UI.scale(40, 40), new Coord(-1, 5), null, Collections.emptyList());
	    itemtooltip = Experience::tooltip;
	}

	protected void drawitem(GOut g, Experience exp) {
	    if(exp.small == null)
		exp.small = new TexI(convolvedown(exp.res.get().flayer(Resource.imgc).img, UI.scale(40, 40), iconfilter));
	    g.image(exp.small, Coord.z);
	}

	protected void update() {
	    super.update();
	    loading = true;
	}

	public void tick(double dt) {
	    super.tick(dt);
	    if(loading) {
		loading = false;
		for(Experience exp : seen.items) {
		    try {
			exp.sortkey = exp.res.get().flayer(Resource.tooltip).t;
		    } catch(Loading l) {
			exp.sortkey = "\uffff";
			loading = true;
		    }
		}
		Collections.sort(seen.items, (a, b) -> a.sortkey.compareTo(b.sortkey));
	    }
	}
    }

    protected void attached() {
	this.chr = getparent(CharWnd.class);
	super.attached();
    }

    public SkillWnd() {
	Widget prev;

	prev = add(CharWnd.settip(new Img(catf.render("Lore & Skills").tex()), "gfx/hud/chr/tips/skills"), Coord.z);
	LoadingTextBox info = add(new LoadingTextBox(new Coord(attrw, height), "", ifnd), prev.pos("bl").adds(5, 0).add(wbox.btloff()));
	info.bg = new Color(0, 0, 0, 128);
	Frame.around(this, Collections.singletonList(info));

	prev = add(new Img(catf.render("Entries").tex()), width, 0);
	Tabs lists = new Tabs(prev.pos("bl").adds(5, 0), new Coord(attrw + wbox.bisz().x, 0), this);
	int gh = UI.scale(241);
	Tabs.Tab sktab = lists.add();
	{
	    Frame f = sktab.add(new Frame(new Coord(lists.sz.x, UI.scale(192)), false), 0, 0);
	    int y = f.sz.y + UI.scale(5);
	    skg = f.addin(new SkillGrid(Coord.z) {
		    public void change(Skill sk) {
			Skill p = sel;
			super.change(sk);
			SkillWnd.this.exps.sel = null;
			SkillWnd.this.credos.sel = null;
			if (sk != null)
			    info.settext(sk::rendertext);
			else if (p != null)
			    info.settext("");
		    }
		});
	    Widget bf = sktab.adda(new Frame(new Coord(f.sz.x, UI.scale(44)), false), f.c.x, gh, 0.0, 1.0);
	    Button bbtn = sktab.adda(new Button(UI.scale(50), "Buy").action(() -> {
			if (skg.sel != null)
			    SkillWnd.this.wdgmsg("buy", skg.sel.nm);
	    }), bf.pos("ibr").subs(10, 0).y(bf.pos("mid").y), 1.0, 0.5);
	    Label clbl = sktab.adda(new Label("Cost:"), bf.pos("iul").adds(10, 0).y(bf.pos("mid").y), 0, 0.5);
	    sktab.adda(new RLabel<Pair<Integer, Integer>>(() -> new Pair<>(((skg.sel == null) || skg.sel.has) ? null : skg.sel.cost, this.chr.exp),
							  n -> (n.a == null) ? "N/A" : String.format("%,d / %,d LP", n.a, n.b),
							  n -> ((n.a != null) && (n.a > n.b)) ? debuff : Color.WHITE),
		       bbtn.pos("ul").subs(10, 0).y(bf.pos("mid").y), 1.0, 0.5);
	}

	Tabs.Tab credos = lists.add();
	{
	    Frame f = credos.add(new Frame(new Coord(lists.sz.x, gh), false), 0, 0);
	    this.credos = f.addin(new CredoGrid(Coord.z) {
		    public void change(Credo cr) {
			Credo p = sel;
			super.change(cr);
			SkillWnd.this.skg.sel = null;
			SkillWnd.this.exps.sel = null;
			if (cr != null)
			    info.settext(cr::rendertext);
			else if (p != null)
			    info.settext("");
		    }
		});
	}

	Tabs.Tab exps = lists.add();
	{
	    Frame f = exps.add(new Frame(new Coord(lists.sz.x, gh), false), 0, 0);
	    this.exps = f.addin(new ExpGrid(Coord.z) {
		    public void change(Experience exp) {
			Experience p = sel;
			super.change(exp);
			SkillWnd.this.skg.sel = null;
			SkillWnd.this.credos.sel = null;
			if (exp != null)
			    info.settext(exp::rendertext);
			else if (p != null)
			    info.settext("");
		    }
		});
	}
	lists.pack();
	addhlp(lists.c.add(0, lists.sz.y + UI.scale(5)), UI.scale(5), lists.sz.x,
	      lists.new TabButton(0, "Skills", sktab),
	      lists.new TabButton(0, "Credos", credos),
	      lists.new TabButton(0, "Lore",   exps));
	pack();
    }

    private List<Skill> decsklist(Object[] args, int a, boolean has) {
	List<Skill> buf = new ArrayList<>();
	while(a < args.length) {
	    String nm = (String)args[a++];
	    Indir<Resource> res = ui.sess.getres((Integer)args[a++]);
	    int cost = Utils.iv(args[a++]);
	    buf.add(new Skill(nm, res, cost, has));
	}
	return(buf);
    }

    private List<Credo> deccrlist(Object[] args, int a, boolean has) {
	List<Credo> buf = new ArrayList<>();
	while(a < args.length) {
	    String nm = (String)args[a++];
	    Indir<Resource> res = ui.sess.getres((Integer)args[a++]);
	    buf.add(new Credo(nm, res, has));
	}
	return(buf);
    }

    private List<Experience> decexplist(Object[] args, int a) {
	List<Experience> buf = new ArrayList<>();
	while(a < args.length) {
	    Indir<Resource> res = ui.sess.getres((Integer)args[a++]);
	    int mtime = Utils.iv(args[a++]);
	    int score = Utils.iv(args[a++]);
	    buf.add(new Experience(res, mtime, score));
	}
	return(buf);
    }

    public static final Collection<String> msgs = Arrays.asList("csk", "nsk", "ccr", "ncr", "crcost", "pcr", "exps");
    public void uimsg(String nm, Object... args) {
	if(nm == "csk") {
	    skg.csk.update(decsklist(args, 0, true));
	} else if(nm == "nsk") {
	    skg.nsk.update(decsklist(args, 0, false));
	} else if(nm == "ccr") {
	    credos.ccr(deccrlist(args, 0, true));
	} else if(nm == "ncr") {
	    credos.ncr(deccrlist(args, 0, false));
	} else if(nm == "crcost") {
	    credos.cost = (Integer)args[0];
	} else if(nm == "pcr") {
	    if(args.length > 0) {
		int a = 0;
		String cnm = (String)args[a++];
		Indir<Resource> res = ui.sess.getres((Integer)args[a++]);
		int crl = Utils.iv(args[a++]), crlt = Utils.iv(args[a++]);
		int crql = Utils.iv(args[a++]), crqlt = Utils.iv(args[a++]);
		int qid = Utils.iv(args[a++]);
		credos.pcr(new Credo(cnm, res, false),
			   crl, crlt, crql, crqlt, qid);
	    } else {
		credos.pcr(null, 0, 0, 0, 0, 0);
	    }
	} else if(nm == "exps") {
	    exps.seen.update(decexplist(args, 0));
	} else {
	    super.uimsg(nm, args);
	}
    }
}
