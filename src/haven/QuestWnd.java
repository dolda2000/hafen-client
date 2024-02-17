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
import java.awt.image.BufferedImage;
import static haven.CharWnd.*;
import static haven.PUtils.*;

public class QuestWnd extends Widget {
    public final Widget questbox;
    public final QuestList cqst, dqst;
    public Quest.Info quest;

    public static class Quest {
	public static final int QST_PEND = 0, QST_DONE = 1, QST_FAIL = 2, QST_DISABLED = 3;
	public static final Color[] stcol = {
	    new Color(255, 255, 64), new Color(64, 255, 64), new Color(255, 64, 64),
	};
	public static final char[] stsym = {'\u2022', '\u2713', '\u2717'};
	public final int id;
	public Indir<Resource> res;
	public String title;
	public int done;
	public int mtime;

	private Quest(int id, Indir<Resource> res, String title, int done, int mtime) {
	    this.id = id;
	    this.res = res;
	    this.title = title;
	    this.done = done;
	    this.mtime = mtime;
	}

	public String title() {
	    if(title != null)
		return(title);
	    return(res.get().flayer(Resource.tooltip).t);
	}

	public static class Condition {
	    public final String desc;
	    public int done;
	    public String status;
	    public Object[] wdata = null;

	    public Condition(String desc, int done, String status) {
		this.desc = desc;
		this.done = done;
		this.status = status;
	    }
	}

	private static final Tex qcmp = catf.render("Quest completed").tex();
	private static final Tex qfail = failf.render("Quest failed").tex();
	public void done(GameUI parent) {
	    parent.add(new Widget() {
		    double a = 0.0;
		    Tex img, title, msg;

		    public void draw(GOut g) {
			if(img != null) {
			    if(a < 0.2)
				g.chcolor(255, 255, 255, (int)(255 * Utils.smoothstep(a / 0.2)));
			    else if(a > 0.8)
				g.chcolor(255, 255, 255, (int)(255 * Utils.smoothstep(1.0 - ((a - 0.8) / 0.2))));
			    /*
			    g.image(img, new Coord(0, (Math.max(img.sz().y, title.sz().y) - img.sz().y) / 2));
			    g.image(title, new Coord(img.sz().x + 25, (Math.max(img.sz().y, title.sz().y) - title.sz().y) / 2));
			    g.image(msg, new Coord((sz.x - msg.sz().x) / 2, Math.max(img.sz().y, title.sz().y) + 25));
			    */
			    int y = 0;
			    g.image(img, new Coord((sz.x - img.sz().x) / 2, y)); y += img.sz().y + 15;
			    g.image(title, new Coord((sz.x - title.sz().x) / 2, y)); y += title.sz().y + 15;
			    g.image(msg, new Coord((sz.x - msg.sz().x) / 2, y));
			}
		    }

		    public void tick(double dt) {
			if(img == null) {
			    try {
				title = (done == QST_DONE?catf:failf).render(title()).tex();
				img = res.get().flayer(Resource.imgc).tex();
				msg = (done == QST_DONE)?qcmp:qfail;
				/*
				resize(new Coord(Math.max(img.sz().x + 25 + title.sz().x, msg.sz().x),
						 Math.max(img.sz().y, title.sz().y) + 25 + msg.sz().y));
				*/
				resize(new Coord(Math.max(Math.max(img.sz().x, title.sz().x), msg.sz().x),
						 img.sz().y + 15 + title.sz().y + 15 + msg.sz().y));
				presize();
			    } catch(Loading l) {
				return;
			    }
			}
			if((a += (dt * 0.2)) > 1.0)
			    destroy();
		    }

		    public void presize() {
			c = parent.sz.sub(sz).div(2);
		    }

		    protected void added() {
			presize();
		    }
		});
	}

	public abstract static class CondWidget extends Widget {
	    public final Condition cond;

	    public CondWidget(Condition cond) {
		this.cond = cond;
	    }

	    public boolean update() {
		return(false);
	    }
	}

	public static class DefaultCond extends CondWidget {
	    public Text text;

	    public DefaultCond(Condition cond) {super(cond);}

	    protected void added() {
		super.added();
		StringBuilder buf = new StringBuilder();
		buf.append(String.format("%s{%c %s", RichText.Parser.col2a(stcol[cond.done]), stsym[cond.done], cond.desc));
		if(cond.status != null) {
		    buf.append(' ');
		    buf.append(cond.status);
		}
		buf.append("}");
		text = ifnd.render(buf.toString(), parent.sz.x - 20);
		resize(text.sz().add(15, 1));
	    }

	    public void draw(GOut g) {
		g.image(text.tex(), new Coord(15, 0));
	    }
	}

	public static class Box extends Widget implements Info, QView.QVInfo {
	    public final int id;
	    public final Indir<Resource> res;
	    public final String title;
	    public Condition[] cond = {};
	    private QView cqv;

	    public Box(int id, Indir<Resource> res, String title) {
		super(Coord.z);
		this.id = id;
		this.res = res;
		this.title = title;
	    }

	    protected void added() {
		resize(parent.sz);
	    }

	    public String title() {
		if(title != null)
		    return(title);
		return(res.get().flayer(Resource.tooltip).t);
	    }

	    public Condition[] conds() {
		return(cond);
	    }

	    private QuestWnd qw = null;
	    public int done() {
		if(qw == null)
		    qw = getparent(QuestWnd.class);
		if(qw == null)
		    return(Quest.QST_PEND);
		Quest qst;
		if((qst = qw.cqst.get(id)) != null)
		    return(qst.done);
		if((qst = qw.dqst.get(id)) != null)
		    return(qst.done);
		return(Quest.QST_PEND);
	    }

	    public void refresh() {
	    }

	    public String rendertext() {
		StringBuilder buf = new StringBuilder();
		Resource res = this.res.get();
		buf.append("$img[" + res.name + "]\n\n");
		buf.append("$b{$font[serif,16]{" + title() + "}}\n\n");
		Resource.Pagina pag = res.layer(Resource.pagina);
		if((pag != null) && !pag.text.equals("")) {
		    buf.append("\n");
		    buf.append(pag.text);
		    buf.append("\n");
		}
		return(buf.toString());
	    }

	    public Condition findcond(String desc) {
		for(Condition cond : this.cond) {
		    if(cond.desc.equals(desc))
			return(cond);
		}
		return(null);
	    }

	    public void uimsg(String msg, Object... args) {
		if(msg == "conds") {
		    int a = 0;
		    List<Condition> ncond = new ArrayList<Condition>(args.length);
		    while(a < args.length) {
			String desc = (String)args[a++];
			int st = Utils.iv(args[a++]);
			String status = (String)args[a++];
			Object[] wdata = null;
			if((a < args.length) && (args[a] instanceof Object[]))
			    wdata = (Object[])args[a++];
			Condition cond = findcond(desc);
			if(cond != null) {
			    boolean ch = false;
			    if(st != cond.done) {cond.done = st; ch = true;}
			    if(!Utils.eq(status, cond.status)) {cond.status = status; ch = true;}
			    if(!Arrays.equals(wdata, cond.wdata)) {cond.wdata = wdata; ch = true;}
			    if(ch && (cqv != null))
				cqv.update(cond);
			} else {
			    cond = new Condition(desc, st, status);
			    cond.wdata = wdata;
			}
			ncond.add(cond);
		    }
		    this.cond = ncond.toArray(new Condition[0]);
		    refresh();
		    if(cqv != null)
			cqv.update();
		} else {
		    super.uimsg(msg, args);
		}
	    }

	    public void destroy() {
		super.destroy();
		if(cqv != null)
		    cqv.reqdestroy();
	    }

	    public int questid() {return(id);}

	    public Widget qview() {
		return(cqv = new QView(this));
	    }
	}

	public static class QView extends Widget {
	    public static final Text.Furnace qtfnd = new BlurFurn(new Text.Foundry(Text.serif.deriveFont(java.awt.Font.BOLD), 16).aa(true), 2, 1, Color.BLACK);
	    public static final Text.Foundry qcfnd = new Text.Foundry(Text.sans, 12).aa(true);
	    public final QVInfo info;
	    private Condition[] ccond;
	    private Tex[] rcond = {};
	    private Tex rtitle = null;
	    private Tex glow, glowon;
	    private double glowt = -1;

	    public interface QVInfo {
		public String title();
		public Condition[] conds();
		public int done();
	    }

	    public QView(QVInfo info) {
		this.info = info;
	    }

	    private void resize() {
		Coord sz = new Coord(0, 0);
		if(rtitle != null) {
		    sz.y += rtitle.sz().y + UI.scale(5);
		    sz.x = Math.max(sz.x, rtitle.sz().x);
		}
		for(Tex c : rcond) {
		    sz.y += c.sz().y;
		    sz.x = Math.max(sz.x, c.sz().x);
		}
		sz.x += UI.scale(3);
		resize(sz);
	    }

	    public void draw(GOut g) {
		int y = 0;
		if(rtitle != null) {
		    if(rootxlate(ui.mc).isect(Coord.z, rtitle.sz()))
			g.chcolor(192, 192, 255, 255);
		    else if(info.done() == QST_DISABLED)
			g.chcolor(255, 128, 0, 255);
		    g.image(rtitle, new Coord(3, y));
		    g.chcolor();
		    y += rtitle.sz().y + 5;
		}
		for(Tex c : rcond) {
		    g.image(c, new Coord(3, y));
		    if(c == glowon) {
			double a = (1.0 - Math.pow(Math.cos(glowt * 2 * Math.PI), 2));
			g.chcolor(255, 255, 255, (int)(128 * a));
			g.image(glow, new Coord(0, y - 3));
			g.chcolor();
		    }
		    y += c.sz().y;
		}
	    }

	    public boolean mousedown(Coord c, int btn) {
		if((rtitle != null) && c.isect(Coord.z, rtitle.sz())) {
		    CharWnd cw = getparent(GameUI.class).chrwdg;
		    cw.show();
		    cw.raise();
		    cw.parent.setfocus(cw);
		    cw.questtab.showtab();
		    return(true);
		}
		return(super.mousedown(c, btn));
	    }

	    public void tick(double dt) {
		if(rtitle == null) {
		    try {
			rtitle = qtfnd.render(info.title()).tex();
			resize();
		    } catch(Loading l) {
		    }
		}
		if(glowt >= 0) {
		    if((glowt += (dt * 0.5)) > 1.0) {
			glowt = -1;
			glow = glowon = null;
		    }
		}
	    }

	    private Text ct(Condition c) {
		return(qcfnd.render(" " + stsym[c.done] + " " + c.desc + ((c.status != null)?(" " + c.status):""), stcol[c.done]));
	    }

	    void update() {
		Condition[] cond = info.conds();
		Tex[] rcond = new Tex[cond.length];
		for(int i = 0; i < cond.length; i++) {
		    Condition c = cond[i];
		    BufferedImage text = ct(c).img;
		    rcond[i] = new TexI(rasterimg(blurmask2(text.getRaster(), 1, 1, Color.BLACK)));
		}
		if(glowon != null) {
		    for(int i = 0; i < this.rcond.length; i++) {
			if(this.rcond[i] == glowon) {
			    for(int o = 0; o < cond.length; o++) {
				if(cond[o] == this.ccond[i]) {
				    glowon = rcond[o];
				    break;
				}
			    }
			    break;
			}
		    }
		}
		this.ccond = cond;
		this.rcond = rcond;
		resize();
	    }

	    void update(Condition c) {
		glow = new TexI(rasterimg(blurmask2(ct(c).img.getRaster(), 3, 2, stcol[c.done])));
		for(int i = 0; i < ccond.length; i++) {
		    if(ccond[i] == c) {
			glowon = rcond[i];
			break;
		    }
		}
		glowt = 0.0;
	    }
	}

	public static class DefaultBox extends Box {
	    private Widget current;
	    private boolean refresh = true;
	    public List<Pair<String, String>> options = Collections.emptyList();
	    public CondWidget[] condw = {};

	    public DefaultBox(int id, Indir<Resource> res, String title) {
		super(id, res, title);
	    }

	    protected void layouth(Widget cont) {
		RichText text = ifnd.render(rendertext(), cont.sz.x - UI.scale(20));
		cont.add(new Img(text.tex()), UI.scale(new Coord(10, 10)));
	    }

	    protected void layoutc(Widget cont) {
		int y = cont.contentsz().y + UI.scale(10);
		CondWidget[] nw = new CondWidget[cond.length];
		CondWidget[] pw = condw;
		cond: for(int i = 0; i < cond.length; i++) {
		    for(int o = 0; o < pw.length; o++) {
			if((pw[o] != null) && (pw[o].cond == cond[i])) {
			    if(pw[o].update()) {
				pw[o].unlink();
				nw[i] = cont.add(pw[o], new Coord(0, y));
				y += nw[i].sz.y;
				pw[o] = null;
				continue cond;
			    }
			}
		    }
		    if(cond[i].wdata != null) {
			Indir<Resource> wres = ui.sess.getres((Integer)cond[i].wdata[0]);
			nw[i] = (CondWidget)wres.get().getcode(Widget.Factory.class, true).create(ui, new Object[] {cond[i]});
		    } else {
			nw[i] = new DefaultCond(cond[i]);
		    }
		    y += cont.add(nw[i], new Coord(0, y)).sz.y;
		}
		condw = nw;
	    }

	    protected void layouto(Widget cont) {
		int y = cont.contentsz().y + UI.scale(10);
		for(Pair<String, String> opt : options) {
		    y += cont.add(new Button(cont.sz.x - UI.scale(20), opt.b, false) {
			    public void click() {
				DefaultBox.this.wdgmsg("opt", opt.a);
			    }
			}, new Coord(UI.scale(10), y)).sz.y + UI.scale(5);
		}
	    }

	    protected void layout(Widget cont) {
		layouth(cont);
		layoutc(cont);
		layouto(cont);
	    }

	    public void draw(GOut g) {
		refresh: if(refresh) {
		    Scrollport newch = new Scrollport(sz);
		    try {
			layout(newch.cont);
		    } catch(Loading l) {
			break refresh;
		    }
		    if(current != null)
			current.destroy();
		    current = add(newch, Coord.z);
		    refresh = false;
		}
		super.draw(g);
	    }

	    public void refresh() {
		refresh = true;
	    }

	    public void uimsg(String msg, Object... args) {
		if(msg == "opts") {
		    List<Pair<String, String>> opts = new ArrayList<>();
		    for(int i = 0; i < args.length; i += 2)
			opts.add(new Pair<>((String)args[i], (String)args[i + 1]));
		    this.options = opts;
		    refresh();
		} else {
		    super.uimsg(msg, args);
		}
	    }
	}

	@RName("quest")
	public static class $quest implements Factory {
	    public Widget create(UI ui, Object[] args) {
		int id = Utils.iv(args[0]);
		Indir<Resource> res = ui.sess.getres((Integer)args[1]);
		String title = (args.length > 2) ? (String)args[2] : null;
		return(new DefaultBox(id, res, title));
	    }
	}
	public interface Info {
	    public int questid();
	    public Widget qview();
	}
    }

    public class QuestList extends SListBox<Quest, Widget> {
	public List<Quest> quests = new ArrayList<Quest>();
	private boolean loading = false;
	private final Comparator<Quest> comp = new Comparator<Quest>() {
	    public int compare(Quest a, Quest b) {
		return(b.mtime - a.mtime);
	    }
	};

	public QuestList(Coord sz) {
	    super(sz, attrf.height() + UI.scale(2));
	}

	protected List<Quest> items() {return(quests);}
	protected Widget makeitem(Quest q, int idx, Coord sz) {return(new Item(sz, q));}

	public void tick(double dt) {
	    if(loading) {
		loading = false;
		Collections.sort(quests, comp);
	    }
	    super.tick(dt);
	}

	public class Item extends Widget {
	    public final Quest q;
	    private final IconText nm;
	    private Object dres, dtit;

	    public Item(Coord sz, Quest q) {
		super(sz);
		this.q = q;
		this.nm = new IconText(sz) {
			protected BufferedImage img() {return(q.res.get().flayer(Resource.imgc).img);}
			protected String text() {return(q.title());}

			protected void drawtext(GOut g) {
			    if(q.done == Quest.QST_DISABLED)
				g.chcolor(255, 128, 0, 255);
			    super.drawtext(g);
			    g.chcolor();
			}
		    };
		add(this.nm, Coord.z);
	    }

	    public void draw(GOut g) {
		if((q.res != dres) || (q.title != dtit)) {
		    nm.invalidate();
		    dres = q.res;
		    dtit = q.title;
		}
		super.draw(g);
	    }

	    public boolean mousedown(Coord c, int button) {
		if(super.mousedown(c, button))
		    return(true);
		if(button == 1) {
		    if((QuestWnd.this.quest != null) && (q.id == QuestWnd.this.quest.questid()))
			QuestWnd.this.wdgmsg("qsel", (Object)null);
		    else
			QuestWnd.this.wdgmsg("qsel", q.id);
		    return(true);
		}
		return(false);
	    }
	}

	protected void drawslot(GOut g, Quest q, int idx, Area area) {
	    super.drawslot(g, q, idx, area);
	    if((quest != null) && (quest.questid() == q.id))
		drawsel(g, q, idx, area);
	}

	protected boolean unselect(int button) {
	    if(button == 1)
		QuestWnd.this.wdgmsg("qsel", (Object)null);
	    return(true);
	}

	public Quest get(int id) {
	    for(Quest q : quests) {
		if(q.id == id)
		    return(q);
	    }
	    return(null);
	}

	public void add(Quest q) {
	    quests.add(q);
	}

	public Quest remove(int id) {
	    for(Iterator<Quest> i = quests.iterator(); i.hasNext();) {
		Quest q = i.next();
		if(q.id == id) {
		    i.remove();
		    return(q);
		}
	    }
	    return(null);
	}

	public void remove(Quest q) {
	    quests.remove(q);
	}
    }

    public QuestWnd() {
	Widget prev;

	prev = add(CharWnd.settip(new Img(catf.render("Quest Log").tex()), "gfx/hud/chr/tips/quests"), new Coord(0, 0));
	questbox = add(new Widget(new Coord(attrw, height)) {
		public void draw(GOut g) {
		    g.chcolor(0, 0, 0, 128);
		    g.frect(Coord.z, sz);
		    g.chcolor();
		    super.draw(g);
		}

		public void cdestroy(Widget w) {
		    if(w == quest)
			quest = null;
		}
	    }, prev.pos("bl").adds(5, 0).add(wbox.btloff()));
	Frame.around(this, Collections.singletonList(questbox));
	Tabs lists = new Tabs(prev.pos("bl").x(width + UI.scale(5)), Coord.z, this);
	Tabs.Tab cqst = lists.add();
	{
	    this.cqst = cqst.add(new QuestList(Coord.of(attrw, height - Button.hs - UI.scale(5))), wbox.btloff());
	    Frame.around(cqst, Collections.singletonList(this.cqst));
	}
	Tabs.Tab dqst = lists.add();
	{
	    this.dqst = dqst.add(new QuestList(Coord.of(attrw, height - Button.hs - UI.scale(5))), wbox.btloff());
	    Frame.around(dqst, Collections.singletonList(this.dqst));
	}
	lists.pack();
	addhlp(lists.c.add(0, lists.sz.y + UI.scale(5)), UI.scale(5), lists.sz.x,
		     lists.new TabButton(0, "Current",   cqst),
		     lists.new TabButton(0, "Completed", dqst));
	pack();
    }

    public static final Collection<String> children = Arrays.asList("quest");
    public void addchild(Widget child, Object... args) {
	String place = (args[0] instanceof String) ? (((String)args[0]).intern()) : null;
	if(place == "quest") {
	    this.quest = (Quest.Info)child;
	    questbox.add(child, Coord.z);
	    getparent(GameUI.class).addchild(this.quest.qview(), "qq");
	} else {
	    super.addchild(child, args);
	}
    }

    public static final Collection<String> msgs = Arrays.asList("quests");
    public void uimsg(String nm, Object... args) {
	if(nm == "quests") {
	    for(int i = 0; i < args.length;) {
		int id = Utils.iv(args[i++]);
		Integer resid = (Integer)args[i++];
		if(resid != null) {
		    Indir<Resource> res = ui.sess.getres(resid);
		    int st = Utils.iv(args[i++]);
		    int mtime = Utils.iv(args[i++]);
		    String title = null;
		    if((i < args.length) && (args[i] instanceof String))
			title = (String)args[i++];
		    QuestList cl = cqst;
		    Quest q = cqst.get(id);
		    if(q == null)
			q = (cl = dqst).get(id);
		    if(q == null) {
			cl = null;
			q = new Quest(id, res, title, st, mtime);
		    } else {
			int fst = q.done;
			q.res = res;
			q.done = st;
			q.mtime = mtime;
			if(((fst == Quest.QST_PEND) || (fst == Quest.QST_DISABLED)) &&
			   !((st == Quest.QST_PEND) || (st == Quest.QST_DISABLED)))
			    q.done(getparent(GameUI.class));
		    }
		    QuestList nl = ((q.done == Quest.QST_PEND) || (q.done == Quest.QST_DISABLED)) ? cqst : dqst;
		    if(nl != cl) {
			if(cl != null)
			    cl.remove(q);
			nl.add(q);
		    }
		    nl.loading = true;
		} else {
		    cqst.remove(id);
		    dqst.remove(id);
		}
	    }
	} else {
	    super.uimsg(nm, args);
	}
    }
}
