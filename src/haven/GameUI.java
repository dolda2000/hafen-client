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
import java.util.function.*;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.image.WritableRaster;
import haven.render.Location;
import static haven.Inventory.invsq;

public class GameUI extends ConsoleHost implements Console.Directory, UI.MessageWidget {
    private static final int blpw = UI.scale(142), brpw = UI.scale(142);
    public final String chrid, genus;
    public final long plid;
    private final Hidepanel ulpanel, umpanel, urpanel, blpanel, mapmenupanel, brpanel, menupanel;
    public Widget portrait;
    public MenuGrid menu;
    public MapView map;
    public GobIcon.Settings iconconf;
    public MiniMap mmap;
    public Fightview fv;
    private List<Widget> meters = new LinkedList<Widget>();
    private Text lastmsg;
    private double msgtime;
    private Window invwnd, equwnd, makewnd, srchwnd, iconwnd;
    private Coord makewndc = Utils.getprefc("makewndc", new Coord(400, 200));
    public Inventory maininv;
    public CharWnd chrwdg;
    public MapWnd mapfile;
    private Widget qqview;
    public BuddyWnd buddies;
    private final Zergwnd zerg;
    public final Collection<Polity> polities = new ArrayList<Polity>();
    public HelpWnd help;
    public OptWnd opts;
    public Collection<DraggedItem> hand = new LinkedList<DraggedItem>();
    public WItem vhand;
    public ChatUI chat;
    public ChatUI.Channel syslog;
    public Progress prog = null;
    private boolean afk = false;
    public BeltSlot[] belt = new BeltSlot[144];
    public Belt beltwdg;
    public final Map<Integer, String> polowners = new HashMap<Integer, String>();
    public Bufflist buffs;

    public static abstract class BeltSlot {
	public final int idx;

	public BeltSlot(int idx) {
	    this.idx = idx;
	}

	public abstract void draw(GOut g);
	public abstract void use(MenuGrid.Interaction iact);
    }

    private static final OwnerContext.ClassResolver<ResBeltSlot> beltctxr = new OwnerContext.ClassResolver<ResBeltSlot>()
	.add(GameUI.class, slot -> slot.wdg())
	.add(Glob.class, slot -> slot.wdg().ui.sess.glob)
	.add(Session.class, slot -> slot.wdg().ui.sess);
    public class ResBeltSlot extends BeltSlot implements GSprite.Owner, RandomSource {
	public final ResData rdt;

	public ResBeltSlot(int idx, ResData rdt) {
	    super(idx);
	    this.rdt = rdt;
	}

	private GSprite spr = null;
	public GSprite spr() {
	    GSprite ret = this.spr;
	    if(ret == null)
		ret = this.spr = GSprite.create(this, rdt.res.get(), new MessageBuf(rdt.sdt));
	    return(ret);
	}

	public void draw(GOut g) {
	    try {
		spr().draw(g);
	    } catch(Loading l) {}
	}

	public void use(MenuGrid.Interaction iact) {
	    Object[] args = {idx, iact.btn, iact.modflags};
	    if(iact.mc != null) {
		args = Utils.extend(args, iact.mc.floor(OCache.posres));
		if(iact.click != null)
		    args = Utils.extend(args, iact.click.clickargs());
	    }
	    GameUI.this.wdgmsg("belt", args);
	}

	public Resource getres() {return(rdt.res.get());}
	public Random mkrandoom() {return(new Random(System.identityHashCode(this)));}
	public <T> T context(Class<T> cl) {return(beltctxr.context(cl, this));}
	private GameUI wdg() {return(GameUI.this);}
    }

    public static class PagBeltSlot extends BeltSlot {
	public final MenuGrid.Pagina pag;

	public PagBeltSlot(int idx, MenuGrid.Pagina pag) {
	    super(idx);
	    this.pag = pag;
	}

	public void draw(GOut g) {
	    try {
		MenuGrid.PagButton btn = pag.button();
		btn.draw(g, btn.spr());
	    } catch(Loading l) {
	    }
	}

	public void use(MenuGrid.Interaction iact) {
	    try {
		pag.scm.use(pag.button(), iact, false);
	    } catch(Loading l) {
	    }
	}

	public static MenuGrid.Pagina resolve(MenuGrid scm, Indir<Resource> resid) {
	    Resource res = resid.get();
	    Resource.AButton act = res.layer(Resource.action);
	    /* XXX: This is quite a hack. Is there a better way? */
	    if((act != null) && (act.ad.length == 0))
		return(scm.paginafor(res.indir()));
	    return(scm.paginafor(resid));
	}
    }

    /* XXX: Remove me */
    public BeltSlot mkbeltslot(int idx, ResData rdt) {
	Resource res = rdt.res.get();
	Resource.AButton act = res.layer(Resource.action);
	if(act != null) {
	    if(act.ad.length == 0)
		return(new PagBeltSlot(idx, menu.paginafor(res.indir())));
	    return(new PagBeltSlot(idx, menu.paginafor(rdt.res)));
	}
	return(new ResBeltSlot(idx, rdt));
    }

    public abstract class Belt extends Widget implements DTarget, DropTarget {
	public Belt(Coord sz) {
	    super(sz);
	}

	public void act(int idx, MenuGrid.Interaction iact) {
	    if(belt[idx] != null)
		belt[idx].use(iact);
	}

	public void keyact(int slot) {
	    if(map != null) {
		BeltSlot si = belt[slot];
		Coord mvc = map.rootxlate(ui.mc);
		if(mvc.isect(Coord.z, map.sz)) {
		    map.new Hittest(mvc) {
			    protected void hit(Coord pc, Coord2d mc, ClickData inf) {
				act(slot, new MenuGrid.Interaction(1, ui.modflags(), mc, inf));
			    }
			    
			    protected void nohit(Coord pc) {
				act(slot, new MenuGrid.Interaction(1, ui.modflags()));
			    }
			}.run();
		}
	    }
	}

	public abstract int beltslot(Coord c);

	public boolean mousedown(Coord c, int button) {
	    int slot = beltslot(c);
	    if(slot != -1) {
		if(button == 1)
		    act(slot, new MenuGrid.Interaction(1, ui.modflags()));
		if(button == 3)
		    GameUI.this.wdgmsg("setbelt", slot, null);
		return(true);
	    }
	    return(super.mousedown(c, button));
	}

	public boolean drop(Coord c, Coord ul) {
	    int slot = beltslot(c);
	    if(slot != -1) {
		GameUI.this.wdgmsg("setbelt", slot, 0);
		return(true);
	    }
	    return(false);
	}

	public boolean iteminteract(Coord c, Coord ul) {return(false);}

	public boolean dropthing(Coord c, Object thing) {
	    int slot = beltslot(c);
	    if(slot != -1) {
		if(thing instanceof MenuGrid.Pagina) {
		    MenuGrid.Pagina pag = (MenuGrid.Pagina)thing;
		    try {
			if(pag.id instanceof Indir)
			    GameUI.this.wdgmsg("setbelt", slot, "res", pag.res().name);
			else
			    GameUI.this.wdgmsg("setbelt", slot, "pag", pag.id);
		    } catch(Loading l) {
		    }
		    return(true);
		}
	    }
	    return(false);
	}
    }
    
    @RName("gameui")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    String chrid = (String)args[0];
	    long plid = Utils.uiv(args[1]);
	    String genus = "";
	    if(args.length > 2)
		genus = (String)args[2];
	    return(new GameUI(chrid, plid, genus));
	}
    }
    
    private final Coord minimapc;
    private final Coord menugridc;
    public GameUI(String chrid, long plid, String genus) {
	this.chrid = chrid;
	this.plid = plid;
	this.genus = genus;
	setcanfocus(true);
	setfocusctl(true);
	chat = add(new ChatUI());
	chat.show(Utils.getprefb("chatvis", true));
	beltwdg.raise();
	blpanel = add(new Hidepanel("gui-bl", null, new Coord(-1,  1)) {
		public void move(double a) {
		    super.move(a);
		    mapmenupanel.move();
		}
	    });
	mapmenupanel = add(new Hidepanel("mapmenu", new Indir<Coord>() {
		public Coord get() {
		    return(new Coord(0, Math.min(blpanel.c.y - mapmenupanel.sz.y + UI.scale(33), GameUI.this.sz.y - mapmenupanel.sz.y)));
		}
	    }, new Coord(-1, 0)));
	brpanel = add(new Hidepanel("gui-br", null, new Coord( 1,  1)) {
		public void move(double a) {
		    super.move(a);
		    menupanel.move();
		}
	    });
	menupanel = add(new Hidepanel("menu", new Indir<Coord>() {
		public Coord get() {
		    return(new Coord(GameUI.this.sz.x, Math.min(brpanel.c.y - UI.scale(79), GameUI.this.sz.y - menupanel.sz.y)));
		}
	    }, new Coord(1, 0)));
	ulpanel = add(new Hidepanel("gui-ul", null, new Coord(-1, -1)));
	umpanel = add(new Hidepanel("gui-um", null, new Coord( 0, -1)));
	urpanel = add(new Hidepanel("gui-ur", null, new Coord( 1, -1)));
	mapmenupanel.add(new MapMenu(), 0, 0);
	blpanel.add(new Img(Resource.loadtex("gfx/hud/blframe")), 0, 0);
	minimapc = new Coord(UI.scale(4), UI.scale(34));
	Tex rbtnbg = Resource.loadtex("gfx/hud/csearch-bg");
	Img brframe = brpanel.add(new Img(Resource.loadtex("gfx/hud/brframe")), rbtnbg.sz().x - UI.scale(22), 0);
	menugridc = brframe.c.add(UI.scale(20), UI.scale(34));
	Img rbtnimg = brpanel.add(new Img(rbtnbg), 0, brpanel.sz.y - rbtnbg.sz().y);
	menupanel.add(new MainMenu(), 0, 0);
	menubuttons(rbtnimg);
	foldbuttons();
	portrait = ulpanel.add(Frame.with(new Avaview(Avaview.dasz, plid, "avacam"), false), UI.scale(10, 10));
	buffs = ulpanel.add(new Bufflist(), portrait.c.x + portrait.sz.x + UI.scale(10), portrait.c.y + ((IMeter.fsz.y + UI.scale(2)) * 2) + UI.scale(5 - 2));
	umpanel.add(new Cal(), Coord.z);
	syslog = chat.add(new ChatUI.Log("System"));
	opts = add(new OptWnd());
	opts.hide();
	zerg = add(new Zergwnd(), Utils.getprefc("wndc-zerg", UI.scale(new Coord(187, 50))));
	zerg.hide();
    }

    protected void attached() {
	iconconf = loadiconconf();
	super.attached();
    }

    public static final KeyBinding kb_srch = KeyBinding.get("scm-srch", KeyMatch.forchar('Z', KeyMatch.C));
    private void menubuttons(Widget bg) {
	brpanel.add(new MenuButton("csearch", kb_srch, "Search actions...") {
		public void click() {
		    if(menu == null)
			return;
		    if(srchwnd == null) {
			srchwnd = new MenuSearch(menu);
			fitwdg(GameUI.this.add(srchwnd, Utils.getprefc("wndc-srch", new Coord(200, 200))));
		    } else {
			if(!srchwnd.hasfocus) {
			    this.setfocus(srchwnd);
			} else {
			    ui.destroy(srchwnd);
			    srchwnd = null;
			}
		    }
		}
	    }, bg.c);
    }

    /* Ice cream */
    private final IButton[] fold_br = new IButton[4];
    private final IButton[] fold_bl = new IButton[4];
    private void updfold(boolean reset) {
	int br;
	if(brpanel.tvis && menupanel.tvis)
	    br = 0;
	else if(brpanel.tvis && !menupanel.tvis)
	    br = 1;
	else if(!brpanel.tvis && !menupanel.tvis)
	    br = 2;
	else
	    br = 3;
	for(int i = 0; i < fold_br.length; i++)
	    fold_br[i].show(i == br);

	int bl;
	if(blpanel.tvis && mapmenupanel.tvis)
	    bl = 0;
	else if(blpanel.tvis && !mapmenupanel.tvis)
	    bl = 1;
	else if(!blpanel.tvis && !mapmenupanel.tvis)
	    bl = 2;
	else
	    bl = 3;
	for(int i = 0; i < fold_bl.length; i++)
	    fold_bl[i].show(i == bl);

	if(reset)
	    resetui();
    }

    private void foldbuttons() {
	final Tex rdnbg = Resource.loadtex("gfx/hud/rbtn-maindwn");
	final Tex rupbg = Resource.loadtex("gfx/hud/rbtn-upbg");
	fold_br[0] = new IButton("gfx/hud/rbtn-dwn", "", "-d", "-h") {
		public void draw(GOut g) {g.image(rdnbg, Coord.z); super.draw(g);}
		public void click() {
		    menupanel.cshow(false);
		    updfold(true);
		}
	    };
	fold_br[1] = new IButton("gfx/hud/rbtn-dwn", "", "-d", "-h") {
		public void draw(GOut g) {g.image(rdnbg, Coord.z); super.draw(g);}
		public void click() {
		    brpanel.cshow(false);
		    updfold(true);
		}
	    };
	fold_br[2] = new IButton("gfx/hud/rbtn-up", "", "-d", "-h") {
		public void draw(GOut g) {g.image(rupbg, Coord.z); super.draw(g);}
		public void click() {
		    menupanel.cshow(true);
		    updfold(true);
		}
		public void presize() {
		    this.c = parent.sz.sub(this.sz);
		}
	    };
	fold_br[3] = new IButton("gfx/hud/rbtn-dwn", "", "-d", "-h") {
		public void draw(GOut g) {g.image(rdnbg, Coord.z); super.draw(g);}
		public void click() {
		    brpanel.cshow(true);
		    updfold(true);
		}
	    };
	menupanel.add(fold_br[0], 0, 0);
	fold_br[0].lower();
	brpanel.adda(fold_br[1], brpanel.sz.x, UI.scale(32), 1, 1);
	adda(fold_br[2], 1, 1);
	fold_br[2].lower();
	menupanel.add(fold_br[3], 0, 0);
	fold_br[3].lower();

	final Tex ldnbg = Resource.loadtex("gfx/hud/lbtn-bgs");
	final Tex lupbg = Resource.loadtex("gfx/hud/lbtn-upbg");
	fold_bl[0] = new IButton("gfx/hud/lbtn-dwn", "", "-d", "-h") {
		public void click() {
		    mapmenupanel.cshow(false);
		    updfold(true);
		}
	    };
	fold_bl[1] = new IButton("gfx/hud/lbtn-dwn", "", "-d", "-h") {
		public void draw(GOut g) {g.image(ldnbg, Coord.z); super.draw(g);}
		public void click() {
		    blpanel.cshow(false);
		    updfold(true);
		}
	    };
	fold_bl[2] = new IButton("gfx/hud/lbtn-up", "", "-d", "-h") {
		public void draw(GOut g) {g.image(lupbg, Coord.z); super.draw(g);}
		public void click() {
		    mapmenupanel.cshow(true);
		    updfold(true);
		}
		public void presize() {
		    this.c = new Coord(0, parent.sz.y - sz.y);
		}
	    };
	fold_bl[3] = new IButton("gfx/hud/lbtn-dwn", "", "-d", "-h") {
		public void click() {
		    blpanel.cshow(true);
		    updfold(true);
		}
	    };
	mapmenupanel.add(fold_bl[0], 0, 0);
	blpanel.adda(fold_bl[1], 0, UI.scale(33), 0, 1);
	adda(fold_bl[2], 0, 1);
	fold_bl[2].lower();
	mapmenupanel.add(fold_bl[3], 0, 0);

	updfold(false);
    }

    protected void added() {
	resize(parent.sz);
	ui.cons.out = new java.io.PrintWriter(new java.io.Writer() {
		StringBuilder buf = new StringBuilder();
		
		public void write(char[] src, int off, int len) {
		    List<String> lines = new ArrayList<String>();
		    synchronized(this) {
			buf.append(src, off, len);
			int p;
			while((p = buf.indexOf("\n")) >= 0) {
			    String ln = buf.substring(0, p).replace("\t", "        ");
			    lines.add(ln);
			    buf.delete(0, p + 1);
			}
		    }
		    for(String ln : lines) {
			syslog.append(ln, Color.WHITE);
		    }
		}
		
		public void close() {}
		public void flush() {}
	    });
	Debug.log = ui.cons.out;
	opts.c = sz.sub(opts.sz).div(2);
    }

    public void dispose() {
	savewndpos();
	Debug.log = new java.io.PrintWriter(System.err);
	ui.cons.clearout();
	super.dispose();
    }
    
    public class Hidepanel extends Widget {
	public final String id;
	public final Coord g;
	public final Indir<Coord> base;
	public boolean tvis;
	private double cur;

	public Hidepanel(String id, Indir<Coord> base, Coord g) {
	    this.id = id;
	    this.base = base;
	    this.g = g;
	    cur = show(tvis = Utils.getprefb(id + "-visible", true))?0:1;
	}

	public <T extends Widget> T add(T child) {
	    super.add(child);
	    pack();
	    if(parent != null)
		move();
	    return(child);
	}

	public Coord base() {
	    if(base != null) return(base.get());
	    return(new Coord((g.x > 0)?parent.sz.x:(g.x < 0)?0:((parent.sz.x - this.sz.x) / 2),
			     (g.y > 0)?parent.sz.y:(g.y < 0)?0:((parent.sz.y - this.sz.y) / 2)));
	}

	public void move(double a) {
	    cur = a;
	    Coord c = new Coord(base());
	    if(g.x < 0)
		c.x -= (int)(sz.x * a);
	    else if(g.x > 0)
		c.x -= (int)(sz.x * (1 - a));
	    if(g.y < 0)
		c.y -= (int)(sz.y * a);
	    else if(g.y > 0)
		c.y -= (int)(sz.y * (1 - a));
	    this.c = c;
	}

	public void move() {
	    move(cur);
	}

	public void presize() {
	    move();
	}

	public void cresize(Widget ch) {
	    sz = contentsz();
	}

	public boolean mshow(final boolean vis) {
	    clearanims(Anim.class);
	    if(vis)
		show();
	    new NormAnim(0.25) {
		final double st = cur, f = vis?0:1;

		public void ntick(double a) {
		    if((a == 1.0) && !vis)
			hide();
		    move(st + (Utils.smoothstep(a) * (f - st)));
		}
	    };
	    tvis = vis;
	    updfold(false);
	    return(vis);
	}

	public boolean mshow() {
	    return(mshow(Utils.getprefb(id + "-visible", true)));
	}

	public boolean cshow(boolean vis) {
	    Utils.setprefb(id + "-visible", vis);
	    if(vis != tvis)
		mshow(vis);
	    return(vis);
	}

	public void cdestroy(Widget w) {
	    parent.cdestroy(w);
	}
    }

    public static class Hidewnd extends Window {
	Hidewnd(Coord sz, String cap, boolean lg) {
	    super(sz, cap, lg);
	}

	Hidewnd(Coord sz, String cap) {
	    super(sz, cap);
	}

	public void wdgmsg(Widget sender, String msg, Object... args) {
	    if((sender == this) && msg.equals("close")) {
		this.hide();
		return;
	    }
	    super.wdgmsg(sender, msg, args);
	}
    }

    static class Zergwnd extends Hidewnd {
	Tabs tabs = new Tabs(Coord.z, Coord.z, this);
	final TButton kin, pol, pol2;

	class TButton extends IButton {
	    Tabs.Tab tab = null;
	    final Tex inv;

	    TButton(String nm, boolean g) {
		super("gfx/hud/buttons/" + nm, "u", "d", null);
		if(g)
		    inv = Resource.loadtex("gfx/hud/buttons/" + nm + "g");
		else
		    inv = null;
	    }

	    public void draw(GOut g) {
		if((tab == null) && (inv != null))
		    g.image(inv, Coord.z);
		else
		    super.draw(g);
	    }

	    public void click() {
		if(tab != null) {
		    tabs.showtab(tab);
		    repack();
		}
	    }
	}

	Zergwnd() {
	    super(Coord.z, "Kith & Kin", true);
	    kin = add(new TButton("kin", false));
	    kin.tooltip = Text.render("Kin");
	    pol = add(new TButton("pol", true));
	    pol2 = add(new TButton("rlm", true));
	}

	private void repack() {
	    tabs.indpack();
	    kin.c = new Coord(0, tabs.curtab.contentsz().y + UI.scale(20));
	    pol.c = new Coord(kin.c.x + kin.sz.x + UI.scale(10), kin.c.y);
	    pol2.c = new Coord(pol.c.x + pol.sz.x + UI.scale(10), pol.c.y);
	    this.pack();
	}

	Tabs.Tab ntab(Widget ch, TButton btn) {
	    Tabs.Tab tab = add(tabs.new Tab() {
		    public void cresize(Widget ch) {
			repack();
		    }
		}, tabs.c);
	    tab.add(ch, Coord.z);
	    btn.tab = tab;
	    repack();
	    return(tab);
	}

	void dtab(TButton btn) {
	    btn.tab.destroy();
	    btn.tab = null;
	    repack();
	}

	void addpol(Polity p) {
	    /* This isn't very nice. :( */
	    TButton btn = p.cap.equals("Village")?pol:pol2;
	    ntab(p, btn);
	    btn.tooltip = Text.render(p.cap);
	}
    }

    static class DraggedItem {
	final GItem item;
	final Coord dc;

	DraggedItem(GItem item, Coord dc) {
	    this.item = item; this.dc = dc;
	}
    }

    private void updhand() {
	if((hand.isEmpty() && (vhand != null)) || ((vhand != null) && !hand.contains(vhand.item))) {
	    ui.destroy(vhand);
	    vhand = null;
	}
	if(!hand.isEmpty() && (vhand == null)) {
	    DraggedItem fi = hand.iterator().next();
	    vhand = add(new ItemDrag(fi.dc, fi.item));
	}
    }

    private String mapfilename() {
	StringBuilder buf = new StringBuilder();
	buf.append(genus);
	String chrid = Utils.getpref("mapfile/" + this.chrid, "");
	if(!chrid.equals("")) {
	    if(buf.length() > 0) buf.append('/');
	    buf.append(chrid);
	}
	return(buf.toString());
    }

    public Coord optplacement(Widget child, Coord org) {
	Set<Window> closed = new HashSet<>();
	Set<Coord> open = new HashSet<>();
	open.add(org);
	Coord opt = null;
	double optscore = Double.NEGATIVE_INFINITY;
	Coord plc = null;
	{
	    Gob pl = map.player();
	    if(pl != null) {
		Coord3f raw = pl.placed.getc();
		if(raw != null)
		    plc = map.screenxf(raw).round2();
	    }
	}
	Area parea = Area.sized(Coord.z, sz);
	while(!open.isEmpty()) {
	    Coord cur = Utils.take(open);
	    double score = 0;
	    Area tarea = Area.sized(cur, child.sz);
	    if(parea.isects(tarea)) {
		double outside = 1.0 - (((double)parea.overlap(tarea).area()) / ((double)tarea.area()));
		if((outside > 0.75) && !cur.equals(org))
		    continue;
		score -= Math.pow(outside, 2) * 100;
	    } else {
		if(!cur.equals(org))
		    continue;
		score -= 100;
	    }
	    {
		boolean any = false;
		for(Widget wdg = this.child; wdg != null; wdg = wdg.next) {
		    if(!(wdg instanceof Window))
			continue;
		    Window wnd = (Window)wdg;
		    if(!wnd.visible())
			continue;
		    Area warea = wnd.parentarea(this);
		    if(warea.isects(tarea)) {
			any = true;
			score -= ((double)warea.overlap(tarea).area()) / ((double)tarea.area());
			if(!closed.contains(wnd)) {
			    open.add(new Coord(wnd.c.x - child.sz.x, cur.y));
			    open.add(new Coord(cur.x, wnd.c.y - child.sz.y));
			    open.add(new Coord(wnd.c.x + wnd.sz.x, cur.y));
			    open.add(new Coord(cur.x, wnd.c.y + wnd.sz.y));
			    closed.add(wnd);
			}
		    }
		}
		if(!any)
		    score += 10;
	    }
	    if(plc != null) {
		if(tarea.contains(plc))
		    score -= 100;
		else
		    score -= (1 - Math.pow(tarea.closest(plc).dist(plc) / sz.dist(Coord.z), 0.5)) * 1.5;
	    }
	    score -= (cur.dist(org) / sz.dist(Coord.z)) * 0.75;
	    if(score > optscore) {
		optscore = score;
		opt = cur;
	    }
	}
	return(opt);
    }

    private void savewndpos() {
	if(invwnd != null)
	    Utils.setprefc("wndc-inv", invwnd.c);
	if(equwnd != null)
	    Utils.setprefc("wndc-equ", equwnd.c);
	if(chrwdg != null)
	    Utils.setprefc("wndc-chr", chrwdg.c);
	if(zerg != null)
	    Utils.setprefc("wndc-zerg", zerg.c);
	if(mapfile != null) {
	    Utils.setprefc("wndc-map", mapfile.c);
	    Utils.setprefc("wndsz-map", mapfile.csz());
	}
    }

    private final BMap<String, Window> wndids = new HashBMap<String, Window>();

    public void addchild(Widget child, Object... args) {
	String place = ((String)args[0]).intern();
	if(place == "mapview") {
	    child.resize(sz);
	    map = add((MapView)child, Coord.z);
	    map.lower();
	    if(mmap != null)
		ui.destroy(mmap);
	    if(mapfile != null) {
		ui.destroy(mapfile);
		mapfile = null;
	    }
	    ResCache mapstore = ResCache.global;
	    if(MapFile.mapbase.get() != null)
		mapstore = HashDirCache.get(MapFile.mapbase.get());
	    if(mapstore != null) {
		MapFile file;
		try {
		    file = MapFile.load(mapstore, mapfilename());
		} catch(java.io.IOException e) {
		    /* XXX: Not quite sure what to do here. It's
		     * certainly not obvious that overwriting the
		     * existing mapfile with a new one is better. */
		    throw(new RuntimeException("failed to load mapfile", e));
		}
		mmap = blpanel.add(new CornerMap(UI.scale(new Coord(133, 133)), file), minimapc);
		mmap.lower();
		mapfile = new MapWnd(file, map, Utils.getprefc("wndsz-map", UI.scale(new Coord(700, 500))), "Map");
		mapfile.show(Utils.getprefb("wndvis-map", false));
		add(mapfile, Utils.getprefc("wndc-map", new Coord(50, 50)));
	    }
	} else if(place == "menu") {
	    menu = (MenuGrid)brpanel.add(child, menugridc);
	} else if(place == "fight") {
	    fv = urpanel.add((Fightview)child, 0, 0);
	} else if(place == "fsess") {
	    add(child, Coord.z);
	} else if(place == "inv") {
	    invwnd = new Hidewnd(Coord.z, "Inventory") {
		    public void cresize(Widget ch) {
			pack();
		    }
		};
	    invwnd.add(maininv = (Inventory)child, Coord.z);
	    invwnd.pack();
	    invwnd.hide();
	    add(invwnd, Utils.getprefc("wndc-inv", new Coord(100, 100)));
	} else if(place == "equ") {
	    equwnd = new Hidewnd(Coord.z, "Equipment");
	    equwnd.add(child, Coord.z);
	    equwnd.pack();
	    equwnd.hide();
	    add(equwnd, Utils.getprefc("wndc-equ", new Coord(400, 10)));
	} else if(place == "hand") {
	    GItem g = add((GItem)child);
	    Coord lc = (Coord)args[1];
	    hand.add(new DraggedItem(g, lc));
	    updhand();
	} else if(place == "chr") {
	    chrwdg = add((CharWnd)child, Utils.getprefc("wndc-chr", new Coord(300, 50)));
	    chrwdg.hide();
	} else if(place == "craft") {
	    String cap = "";
	    Widget mkwdg = child;
	    if(mkwdg instanceof Makewindow)
		cap = ((Makewindow)mkwdg).rcpnm;
	    if(cap.equals(""))
		cap = "Crafting";
	    makewnd = new Window(Coord.z, cap, true) {
		    public void wdgmsg(Widget sender, String msg, Object... args) {
			if((sender == this) && msg.equals("close")) {
			    mkwdg.wdgmsg("close");
			    return;
			}
			super.wdgmsg(sender, msg, args);
		    }
		    public void cdestroy(Widget w) {
			if(w == mkwdg) {
			    ui.destroy(this);
			    makewnd = null;
			}
		    }
		    public void destroy() {
			Utils.setprefc("makewndc", makewndc = this.c);
			super.destroy();
		    }
		};
	    makewnd.add(mkwdg, Coord.z);
	    makewnd.pack();
	    fitwdg(add(makewnd, makewndc));
	} else if(place == "buddy") {
	    zerg.ntab(buddies = (BuddyWnd)child, zerg.kin);
	} else if(place == "pol") {
	    Polity p = (Polity)child;
	    polities.add(p);
	    zerg.addpol(p);
	} else if(place == "chat") {
	    chat.addchild(child);
	} else if(place == "party") {
	    add(child, portrait.pos("bl").adds(0, 10));
	} else if(place == "meter") {
	    int x = (meters.size() % 3) * (IMeter.fsz.x + UI.scale(5));
	    int y = (meters.size() / 3) * (IMeter.fsz.y + UI.scale(2));
	    ulpanel.add(child, portrait.c.x + portrait.sz.x + UI.scale(10) + x, portrait.c.y + y);
	    meters.add(child);
	} else if(place == "buff") {
	    buffs.addchild(child);
	} else if(place == "qq") {
	    if(qqview != null)
		qqview.reqdestroy();
	    final Widget cref = qqview = child;
	    add(new AlignPanel() {
		    {add(cref);}

		    protected Coord getc() {
			return(new Coord(10, mapmenupanel.c.y - this.sz.y - 10));
		    }

		    public void cdestroy(Widget ch) {
			qqview = null;
			destroy();
		    }
		});
	} else if(place == "misc") {
	    Coord c;
	    int a = 1;
	    if(args[a] instanceof Coord) {
		c = (Coord)args[a++];
	    } else if(args[a] instanceof Coord2d) {
		c = ((Coord2d)args[a++]).mul(new Coord2d(this.sz.sub(child.sz))).round();
		c = optplacement(child, c);
	    } else if(args[a] instanceof String) {
		c = relpos((String)args[a++], child, (args.length > a) ? ((Object[])args[a++]) : new Object[] {}, 0);
	    } else {
		throw(new UI.UIException("Illegal gameui child", place, args));
	    }
	    while(a < args.length) {
		Object opt = args[a++];
		if(opt instanceof Object[]) {
		    Object[] opta = (Object[])opt;
		    switch((String)opta[0]) {
		    case "id":
			String wndid = (String)opta[1];
			if(child instanceof Window) {
			    c = Utils.getprefc(String.format("wndc-misc/%s", (String)opta[1]), c);
			    if(!wndids.containsKey(wndid)) {
				c = fitwdg(child, c);
				wndids.put(wndid, (Window)child);
			    } else {
				c = optplacement(child, c);
			    }
			}
			break;
		    case "obj":
			if(child instanceof Window) {
			    ((Window)child).settrans(new GobTrans(map, Utils.uiv(opta[1])));
			}
			break;
		    }
		}
	    }
	    add(child, c);
	} else if(place == "abt") {
	    add(child, Coord.z);
	} else {
	    throw(new UI.UIException("Illegal gameui child", place, args));
	}
    }

    public static class GobTrans implements Window.Transition<GobTrans.Anim, GobTrans.Anim> {
	public static final double time = 0.1;
	public final MapView map;
	public final long gobid;

	public GobTrans(MapView map, long gobid) {
	    this.map = map;
	    this.gobid = gobid;
	}

	private Coord oc() {
	    Gob gob = map.ui.sess.glob.oc.getgob(gobid);
	    if(gob == null)
		return(null);
	    Location.Chain loc = Utils.el(gob.getloc());
	    if(loc == null)
		return(null);
	    return(map.screenxf(loc.fin(Matrix4f.id).mul4(Coord3f.o).invy()).round2());
	}

	public class Anim extends Window.NormAnim {
	    public final Window wnd;
	    private Coord oc;

	    public Anim(Window wnd, boolean hide, Anim from) {
		super(time, from, hide);
		this.wnd = wnd;
		this.oc = wnd.c.add(wnd.sz.div(2));
	    }

	    public void draw(GOut g, Tex tex) {
		GOut pg = g.reclipl(wnd.c.inv(), wnd.parent.sz);
		Coord cur = oc();
		if(cur != null)
		    this.oc = cur;
		Coord sz = tex.sz();
		double na = Utils.smoothstep(this.na);
		pg.chcolor(255, 255, 255, (int)(na * 255));
		double fac = 1.0 - na;
		Coord c = this.oc.sub(sz.div(2)).mul(1.0 - na).add(wnd.c.mul(na));
		pg.image(tex, c.add((int)(sz.x * fac * 0.5), (int)(sz.y * fac * 0.5)),
			 Coord.of((int)(sz.x * (1.0 - fac)), (int)(sz.y * (1.0 - fac))));
	    }
	}

	public Anim show(Window wnd, Anim hide) {return(new Anim(wnd, false, hide));}
	public Anim hide(Window wnd, Anim show) {return(new Anim(wnd, true,  show));}
    }

    public void cdestroy(Widget w) {
	if(w instanceof Window) {
	    String wndid = wndids.reverse().get((Window)w);
	    if(wndid != null) {
		wndids.remove(wndid);
		Utils.setprefc(String.format("wndc-misc/%s", wndid), w.c);
	    }
	}
	if(w instanceof GItem) {
	    for(Iterator<DraggedItem> i = hand.iterator(); i.hasNext();) {
		DraggedItem di = i.next();
		if(di.item == w) {
		    i.remove();
		    updhand();
		}
	    }
	} else if(polities.contains(w)) {
	    polities.remove(w);
	    zerg.dtab(zerg.pol);
	} else if(w == chrwdg) {
	    chrwdg = null;
	}
	meters.remove(w);
    }

    public static class Progress extends Widget {
	private static final Resource.Anim progt = Resource.local().loadwait("gfx/hud/prog").layer(Resource.animc);
	public double prog;
	private TexI curi;
	private String tip;

	public Progress(double prog) {
	    super(progt.f[0][0].ssz);
	    set(prog);
	}

	public void set(double prog) {
	    int fr = Utils.clip((int)Math.floor(prog * progt.f.length), 0, progt.f.length - 2);
	    int bf = Utils.clip((int)(((prog * progt.f.length) - fr) * 255), 0, 255);
	    WritableRaster buf = PUtils.imgraster(progt.f[fr][0].ssz);
	    PUtils.blit(buf, progt.f[fr][0].scaled().getRaster(), Coord.z);
	    PUtils.blendblit(buf, progt.f[fr + 1][0].scaled().getRaster(), Coord.z, bf);
	    if(this.curi != null)
		this.curi.dispose();
	    this.curi = new TexI(PUtils.rasterimg(buf));

	    double d = Math.abs(prog - this.prog);
	    int dec = Math.max(0, (int)Math.round(-Math.log10(d)) - 2);
	    this.tip = String.format("%." + dec + "f%%", prog * 100);
	    this.prog = prog;
	}

	public void draw(GOut g) {
	    g.image(curi, Coord.z);
	}

	public boolean checkhit(Coord c) {
	    return(Utils.checkhit(curi.back, c, 10));
	}

	public Object tooltip(Coord c, Widget prev) {
	    if(checkhit(c))
		return(tip);
	    return(super.tooltip(c, prev));
	}
    }

    public void draw(GOut g) {
	beltwdg.c = new Coord(chat.c.x, Math.min(chat.c.y - beltwdg.sz.y, sz.y - beltwdg.sz.y));
	super.draw(g);
	int by = sz.y;
	if(chat.visible())
	    by = Math.min(by, chat.c.y);
	if(beltwdg.visible())
	    by = Math.min(by, beltwdg.c.y);
	if(cmdline != null) {
	    drawcmd(g, new Coord(blpw + UI.scale(10), by -= UI.scale(20)));
	} else if(lastmsg != null) {
	    if((Utils.rtime() - msgtime) > 3.0) {
		lastmsg = null;
	    } else {
		g.chcolor(0, 0, 0, 192);
		g.frect(new Coord(blpw + UI.scale(8), by - UI.scale(22)), lastmsg.sz().add(UI.scale(4), UI.scale(4)));
		g.chcolor();
		g.image(lastmsg.tex(), new Coord(blpw + UI.scale(10), by -= UI.scale(20)));
	    }
	}
	if(!chat.visible()) {
	    chat.drawsmall(g, new Coord(blpw + UI.scale(10), by), UI.scale(100));
	}
    }
    
    private String iconconfname() {
	StringBuilder buf = new StringBuilder();
	buf.append("data/mm-icons-2");
	if(genus != null)
	    buf.append("/" + genus);
	if(ui.sess != null)
	    buf.append("/" + ui.sess.username);
	return(buf.toString());
    }

    private GobIcon.Settings loadiconconf() {
	String nm = iconconfname();
	try {
	    return(GobIcon.Settings.load(ui, nm));
	} catch(Exception e) {
	    new Warning(e, "could not load icon-conf").issue();
	}
	return(new GobIcon.Settings(ui, nm));
    }

    public class CornerMap extends MiniMap implements Console.Directory {
	public CornerMap(Coord sz, MapFile file) {
	    super(sz, file);
	    follow(new MapLocator(map));
	}

	public boolean dragp(int button) {
	    return(false);
	}

	public boolean clickmarker(DisplayMarker mark, Location loc, int button, boolean press) {
	    if(mark.m instanceof MapFile.SMarker) {
		Gob gob = MarkerID.find(ui.sess.glob.oc, (MapFile.SMarker)mark.m);
		if(gob != null)
		    mvclick(map, null, loc, gob, button);
	    }
	    return(false);
	}

	public boolean clickicon(DisplayIcon icon, Location loc, int button, boolean press) {
	    if(press) {
		mvclick(map, null, loc, icon.gob, button);
		return(true);
	    }
	    return(false);
	}

	public boolean clickloc(Location loc, int button, boolean press) {
	    if(press) {
		mvclick(map, null, loc, null, button);
		return(true);
	    }
	    return(false);
	}

	public void draw(GOut g) {
	    g.image(bg, Coord.z, UI.scale(bg.sz()));
	    super.draw(g);
	}

	protected boolean allowzoomout() {
	    /* XXX? The corner-map has the property that its size
	     * makes it so that the one center grid will very commonly
	     * touch at least one border, making indefinite zoom-out
	     * possible. That will likely cause more problems than
	     * it's worth given the resulting workload in generating
	     * zoomgrids for very high zoom levels, especially when
	     * done by mistake, so lock to an arbitrary five levels of
	     * zoom, at least for now. */
	    if(zoomlevel >= 5)
		return(false);
	    return(super.allowzoomout());
	}
	private Map<String, Console.Command> cmdmap = new TreeMap<String, Console.Command>();
	{
	    cmdmap.put("rmseg", new Console.Command() {
		    public void run(Console cons, String[] args) {
			MiniMap.Location loc = curloc;
			if(loc != null) {
			    try(Locked lk = new Locked(file.lock.writeLock())) {
				file.segments.remove(loc.seg.id);
			    }
			}
		    }
		});
	}
	public Map<String, Console.Command> findcmds() {
	    return(cmdmap);
	}
    }

    private Coord lastsavegrid = null;
    private int lastsaveseq = -1;
    private void mapfiletick() {
	MapView map = this.map;
	MiniMap mmap = this.mmap;
	if((map == null) || (mmap == null))
	    return;
	Gob pl = ui.sess.glob.oc.getgob(map.plgob);
	Coord gc;
	if(pl == null)
	    gc = map.cc.floor(MCache.tilesz).div(MCache.cmaps);
	else
	    gc = pl.rc.floor(MCache.tilesz).div(MCache.cmaps);
	try {
	    MCache.Grid grid = ui.sess.glob.map.getgrid(gc);
	    if((grid != null) && (!Utils.eq(gc, lastsavegrid) || (lastsaveseq != grid.seq))) {
		mmap.file.update(ui.sess.glob.map, gc);
		lastsavegrid = gc;
		lastsaveseq = grid.seq;
	    }
	} catch(Loading l) {
	}
    }

    private double lastwndsave = 0;
    public void tick(double dt) {
	super.tick(dt);
	double now = Utils.rtime();
	if(now - lastwndsave > 60) {
	    savewndpos();
	    lastwndsave = now;
	}
	double idle = now - ui.lastevent;
	if(!afk && (idle > 300)) {
	    afk = true;
	    wdgmsg("afk");
	} else if(afk && (idle <= 300)) {
	    afk = false;
	}
	mapfiletick();
    }
    
    public void uimsg(String msg, Object... args) {
	if(msg == "err") {
	    String err = (String)args[0];
	    ui.error(err);
	} else if(msg == "msg") {
	    String text = (String)args[0];
	    ui.msg(text);
	} else if(msg == "prog") {
	    if(args.length > 0) {
		double p = Utils.dv(args[0]) / 100.0;
		if(prog == null)
		    prog = adda(new Progress(p), 0.5, 0.35);
		else
		    prog.set(p);
	    } else {
		if(prog != null) {
		    prog.reqdestroy();
		    prog = null;
		}
	    }
	} else if(msg == "setbelt") {
	    int slot = Utils.iv(args[0]);
	    if(args.length < 2) {
		belt[slot] = null;
	    } else {
		Indir<Resource> res = ui.sess.getresv(args[1]);
		Message sdt = Message.nil;
		if(args.length > 2)
		    sdt = new MessageBuf((byte[])args[2]);
		ResData rdt = new ResData(res, sdt);
		ui.sess.glob.loader.defer(() -> {
			belt[slot] = mkbeltslot(slot, rdt);
		    }, null);
	    }
	} else if(msg == "setbelt2") {
	    int slot = Utils.iv(args[0]);
	    if(args.length < 2) {
		belt[slot] = null;
	    } else {
		switch((String)args[1]) {
		case "p": {
		    Object id = args[2];
		    belt[slot] = new PagBeltSlot(slot, menu.paginafor(id, null));
		    break;
		}
		case "r": {
		    Indir<Resource> res = ui.sess.getresv(args[2]);
		    ui.sess.glob.loader.defer(() -> {
			    belt[slot] = new PagBeltSlot(slot, PagBeltSlot.resolve(menu, res));
			}, null);
		    break;
		}
		case "d": {
		    Indir<Resource> res = ui.sess.getresv(args[2]);
		    Message sdt = Message.nil;
		    if(args.length > 2)
			sdt = new MessageBuf((byte[])args[3]);
		    belt[slot] = new ResBeltSlot(slot, new ResData(res, sdt));
		    break;
		}
		}
	    }
	} else if(msg == "polowner") {
	    int id = Utils.iv(args[0]);
	    String o = (String)args[1];
	    boolean n = Utils.bv(args[2]);
	    if(o != null)
		o = o.intern();
	    String cur = polowners.get(id);
	    if(map != null) {
		if((o != null) && (cur == null)) {
		    map.setpoltext(id, "Entering " + o);
		} else if((o == null) && (cur != null)) {
		    map.setpoltext(id, "Leaving " + cur);
		}
	    }
	    polowners.put(id, o);
	} else if(msg == "showhelp") {
	    Indir<Resource> res = ui.sess.getresv(args[0]);
	    if(help == null)
		help = adda(new HelpWnd(res), 0.5, 0.25);
	    else
		help.res = res;
	} else if(msg == "map-mark") {
	    long gobid = Utils.uiv(args[0]);
	    long oid = ((Number)args[1]).longValue();
	    Indir<Resource> res = ui.sess.getresv(args[2]);
	    String nm = (String)args[3];
	    if(mapfile != null)
		mapfile.markobj(gobid, oid, res, nm);
	} else if(msg == "map-icons") {
	    GobIcon.Settings conf = this.iconconf;
	    int tag = Utils.iv(args[0]);
	    if(args.length < 2) {
		if(conf.tag != tag)
		    wdgmsg("map-icons", conf.tag);
	    } else {
		conf.receive(args);
	    }
	} else {
	    super.uimsg(msg, args);
	}
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
	if((sender == chrwdg) && (msg == "close")) {
	    chrwdg.hide();
	    return;
	} else if((sender == mapfile) && (msg == "close")) {
	    mapfile.hide();
	    Utils.setprefb("wndvis-map", false);
	    return;
	} else if((sender == help) && (msg == "close")) {
	    ui.destroy(help);
	    help = null;
	    return;
	} else if((sender == srchwnd) && (msg == "close")) {
	    ui.destroy(srchwnd);
	    srchwnd = null;
	    return;
	} else if((sender == iconwnd) && (msg == "close")) {
	    ui.destroy(iconwnd);
	    iconwnd = null;
	    return;
	}
	super.wdgmsg(sender, msg, args);
    }

    private static final int fitmarg = UI.scale(100);
    private Coord fitwdg(Widget wdg, Coord c) {
	Coord ret = new Coord(c);
	ret.x = Math.max(ret.x, Math.min(0, fitmarg - wdg.sz.x));
	ret.y = Math.max(ret.y, Math.min(0, fitmarg - wdg.sz.y));
	ret.x = Math.min(ret.x, sz.x - Math.min(fitmarg, wdg.sz.x));
	ret.y = Math.min(ret.y, sz.y - Math.min(fitmarg, wdg.sz.y));
	return(ret);
    }

    private void fitwdg(Widget wdg) {
	wdg.c = fitwdg(wdg, wdg.c);
    }

    private boolean wndstate(Window wnd) {
	if(wnd == null)
	    return(false);
	return(wnd.visible());
    }

    private void togglewnd(Window wnd) {
	if(wnd != null) {
	    if(wnd.show(!wnd.visible())) {
		wnd.raise();
		fitwdg(wnd);
		setfocus(wnd);
	    }
	}
    }

    public static class MenuButton extends IButton {
	MenuButton(String base, KeyBinding gkey, String tooltip) {
	    super("gfx/hud/" + base, "", "-d", "-h");
	    setgkey(gkey);
	    settip(tooltip);
	}
    }

    public static class MenuCheckBox extends ICheckBox {
	MenuCheckBox(String base, KeyBinding gkey, String tooltip) {
	    super("gfx/hud/" + base, "", "-d", "-h", "-dh");
	    setgkey(gkey);
	    settip(tooltip);
	}
    }

    public static final KeyBinding kb_inv = KeyBinding.get("inv", KeyMatch.forcode(KeyEvent.VK_TAB, 0));
    public static final KeyBinding kb_equ = KeyBinding.get("equ", KeyMatch.forchar('E', KeyMatch.C));
    public static final KeyBinding kb_chr = KeyBinding.get("chr", KeyMatch.forchar('T', KeyMatch.C));
    public static final KeyBinding kb_bud = KeyBinding.get("bud", KeyMatch.forchar('B', KeyMatch.C));
    public static final KeyBinding kb_opt = KeyBinding.get("opt", KeyMatch.forchar('O', KeyMatch.C));
    private static final Tex menubg = Resource.loadtex("gfx/hud/rbtn-bg");
    public class MainMenu extends Widget {
	public MainMenu() {
	    super(menubg.sz());
	    add(new MenuCheckBox("rbtn-inv", kb_inv, "Inventory"), 0, 0).state(() -> wndstate(invwnd)).click(() -> togglewnd(invwnd));
	    add(new MenuCheckBox("rbtn-equ", kb_equ, "Equipment"), 0, 0).state(() -> wndstate(equwnd)).click(() -> togglewnd(equwnd));
	    add(new MenuCheckBox("rbtn-chr", kb_chr, "Character Sheet"), 0, 0).state(() -> wndstate(chrwdg)).click(() -> togglewnd(chrwdg));
	    add(new MenuCheckBox("rbtn-bud", kb_bud, "Kith & Kin"), 0, 0).state(() -> wndstate(zerg)).click(() -> togglewnd(zerg));
	    add(new MenuCheckBox("rbtn-opt", kb_opt, "Options"), 0, 0).state(() -> wndstate(opts)).click(() -> togglewnd(opts));
	}

	public void draw(GOut g) {
	    g.image(menubg, Coord.z);
	    super.draw(g);
	}
    }

    public static final KeyBinding kb_map = KeyBinding.get("map", KeyMatch.forchar('A', KeyMatch.C));
    public static final KeyBinding kb_claim = KeyBinding.get("ol-claim", KeyMatch.nil);
    public static final KeyBinding kb_vil = KeyBinding.get("ol-vil", KeyMatch.nil);
    public static final KeyBinding kb_rlm = KeyBinding.get("ol-rlm", KeyMatch.nil);
    public static final KeyBinding kb_ico = KeyBinding.get("map-icons", KeyMatch.nil);
    private static final Tex mapmenubg = Resource.loadtex("gfx/hud/lbtn-bg");
    public class MapMenu extends Widget {
	private void toggleol(String tag, boolean a) {
	    if(map != null) {
		if(a)
		    map.enol(tag);
		else
		    map.disol(tag);
	    }
	}

	public MapMenu() {
	    super(mapmenubg.sz());
	    add(new MenuCheckBox("lbtn-claim", kb_claim, "Display personal claims"), 0, 0).changed(a -> toggleol("cplot", a));
	    add(new MenuCheckBox("lbtn-vil", kb_vil, "Display village claims"), 0, 0).changed(a -> toggleol("vlg", a));
	    add(new MenuCheckBox("lbtn-rlm", kb_rlm, "Display provinces"), 0, 0).changed(a -> toggleol("prov", a));
	    add(new MenuCheckBox("lbtn-map", kb_map, "Map")).state(() -> wndstate(mapfile)).click(() -> {
		    togglewnd(mapfile);
		    if(mapfile != null)
			Utils.setprefb("wndvis-map", mapfile.visible());
		});
	    add(new MenuCheckBox("lbtn-ico", kb_ico, "Icon settings"), 0, 0).state(() -> wndstate(iconwnd)).click(() -> {
		    if(iconconf == null)
			return;
		    if(iconwnd == null) {
			iconwnd = new GobIcon.SettingsWindow(iconconf);
			fitwdg(GameUI.this.add(iconwnd, Utils.getprefc("wndc-icon", new Coord(200, 200))));
		    } else {
			ui.destroy(iconwnd);
			iconwnd = null;
		    }
		});
	}

	public void draw(GOut g) {
	    g.image(mapmenubg, Coord.z);
	    super.draw(g);
	}
    }

    public static final KeyBinding kb_shoot = KeyBinding.get("screenshot", KeyMatch.forchar('S', KeyMatch.M));
    public static final KeyBinding kb_chat = KeyBinding.get("chat-toggle", KeyMatch.forchar('C', KeyMatch.C));
    public static final KeyBinding kb_hide = KeyBinding.get("ui-toggle", KeyMatch.nil);
    public static final KeyBinding kb_logout = KeyBinding.get("logout", KeyMatch.nil);
    public static final KeyBinding kb_switchchr = KeyBinding.get("logout-cs", KeyMatch.nil);
    public boolean globtype(char key, KeyEvent ev) {
	if(key == ':') {
	    entercmd();
	    return(true);
	} else if((Screenshooter.screenurl.get() != null) && kb_shoot.key().match(ev)) {
	    Screenshooter.take(this, Screenshooter.screenurl.get());
	    return(true);
	} else if(kb_hide.key().match(ev)) {
	    toggleui();
	    return(true);
	} else if(kb_logout.key().match(ev)) {
	    act("lo");
	    return(true);
	} else if(kb_switchchr.key().match(ev)) {
	    act("lo", "cs");
	    return(true);
	} else if(kb_chat.key().match(ev)) {
	    if(chat.visible() && !chat.hasfocus) {
		setfocus(chat);
	    } else {
		if(chat.targetshow) {
		    chat.sshow(false);
		} else {
		    chat.sshow(true);
		    setfocus(chat);
		}
	    }
	    Utils.setprefb("chatvis", chat.targetshow);
	    return(true);
	} else if((key == 27) && (map != null) && !map.hasfocus) {
	    setfocus(map);
	    return(true);
	}
	return(super.globtype(key, ev));
    }
    
    public boolean mousedown(Coord c, int button) {
	return(super.mousedown(c, button));
    }

    private int uimode = 1;
    public void toggleui(int mode) {
	Hidepanel[] panels = {blpanel, brpanel, ulpanel, umpanel, urpanel, menupanel, mapmenupanel};
	switch(uimode = mode) {
	case 0:
	    for(Hidepanel p : panels)
		p.mshow(true);
	    break;
	case 1:
	    for(Hidepanel p : panels)
		p.mshow();
	    break;
	case 2:
	    for(Hidepanel p : panels)
		p.mshow(false);
	    break;
	}
    }

    public void resetui() {
	Hidepanel[] panels = {blpanel, brpanel, ulpanel, umpanel, urpanel, menupanel, mapmenupanel};
	for(Hidepanel p : panels)
	    p.cshow(p.tvis);
	uimode = 1;
    }

    public void toggleui() {
	toggleui((uimode + 1) % 3);
    }

    public void resize(Coord sz) {
	super.resize(sz);
	chat.resize(sz.x - blpw - brpw);
	chat.move(new Coord(blpw, sz.y));
	if(map != null)
	    map.resize(sz);
	if(prog != null)
	    prog.move(sz.sub(prog.sz).mul(0.5, 0.35));
	beltwdg.c = new Coord(blpw + UI.scale(10), sz.y - beltwdg.sz.y - UI.scale(5));
    }
    
    public void presize() {
	resize(parent.sz);
    }
    
    public void msg(String msg, Color color, Color logcol) {
	msgtime = Utils.rtime();
	lastmsg = RootWidget.msgfoundry.render(msg, color);
	syslog.append(msg, logcol);
    }

    public void msg(String msg, Color color) {
	msg(msg, color, color);
    }

    public void msg(String msg, Color color, Audio.Clip sfx) {
	msg(msg, color);
	ui.sfxrl(sfx);
    }

    public void error(String msg) {
	ui.error(msg);
    }
    
    public void act(String... args) {
	wdgmsg("act", (Object[])args);
    }

    public void act(int mods, Coord mc, Gob gob, String... args) {
	int n = args.length;
	Object[] al = new Object[n];
	System.arraycopy(args, 0, al, 0, n);
	if(mc != null) {
	    al = Utils.extend(al, al.length + 2);
	    al[n++] = mods;
	    al[n++] = mc;
	    if(gob != null) {
		al = Utils.extend(al, al.length + 2);
		al[n++] = (int)gob.id;
		al[n++] = gob.rc;
	    }
	}
	wdgmsg("act", al);
    }

    public class FKeyBelt extends Belt implements DTarget, DropTarget {
	public final int beltkeys[] = {KeyEvent.VK_F1, KeyEvent.VK_F2, KeyEvent.VK_F3, KeyEvent.VK_F4,
				       KeyEvent.VK_F5, KeyEvent.VK_F6, KeyEvent.VK_F7, KeyEvent.VK_F8,
				       KeyEvent.VK_F9, KeyEvent.VK_F10, KeyEvent.VK_F11, KeyEvent.VK_F12};
	public int curbelt = 0;

	public FKeyBelt() {
	    super(UI.scale(new Coord(450, 34)));
	}

	private Coord beltc(int i) {
	    return(new Coord((((invsq.sz().x + UI.scale(2)) * i) + (10 * (i / 4))), 0));
	}
    
	public int beltslot(Coord c) {
	    for(int i = 0; i < 12; i++) {
		if(c.isect(beltc(i), invsq.sz()))
		    return(i + (curbelt * 12));
	    }
	    return(-1);
	}
    
	public void draw(GOut g) {
	    for(int i = 0; i < 12; i++) {
		int slot = i + (curbelt * 12);
		Coord c = beltc(i);
		g.image(invsq, beltc(i));
		try {
		    if(belt[slot] != null)
			belt[slot].draw(g.reclip(c.add(UI.scale(1), UI.scale(1)), invsq.sz().sub(UI.scale(2), UI.scale(2))));
		} catch(Loading e) {}
		g.chcolor(156, 180, 158, 255);
		FastText.aprintf(g, c.add(invsq.sz().sub(UI.scale(2), 0)), 1, 1, "F%d", i + 1);
		g.chcolor();
	    }
	}
	
	public boolean globtype(char key, KeyEvent ev) {
	    boolean M = (ev.getModifiersEx() & (KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)) != 0;
	    for(int i = 0; i < beltkeys.length; i++) {
		if(ev.getKeyCode() == beltkeys[i]) {
		    if(M) {
			curbelt = i;
			return(true);
		    } else {
			keyact(i + (curbelt * 12));
			return(true);
		    }
		}
	    }
	    return(false);
	}
    }
    
    private static final Tex nkeybg = Resource.loadtex("gfx/hud/hb-main");
    public class NKeyBelt extends Belt {
	public int curbelt = 0;
	final Coord pagoff = UI.scale(new Coord(5, 25));

	public NKeyBelt() {
	    super(nkeybg.sz());
	    adda(new IButton("gfx/hud/hb-btn-chat", "", "-d", "-h") {
		    Tex glow;
		    {
			this.tooltip = RichText.render("Chat ($col[255,255,0]{Ctrl+C})", 0);
			glow = new TexI(PUtils.rasterimg(PUtils.blurmask(up.getRaster(), UI.scale(2), UI.scale(2), Color.WHITE)));
		    }

		    public void click() {
			if(chat.targetshow) {
			    chat.sshow(false);
			} else {
			    chat.sshow(true);
			    setfocus(chat);
			}
			Utils.setprefb("chatvis", chat.targetshow);
		    }

		    public void draw(GOut g) {
			super.draw(g);
			Color urg = chat.urgcols[chat.urgency];
			if(urg != null) {
			    GOut g2 = g.reclipl2(UI.scale(-4, -4), g.sz().add(UI.scale(4, 4)));
			    g2.chcolor(urg.getRed(), urg.getGreen(), urg.getBlue(), 128);
			    g2.image(glow, Coord.z);
			}
		    }
		}, sz, 1, 1);
	}
	
	private Coord beltc(int i) {
	    return(pagoff.add(UI.scale((36 * i) + (10 * (i / 5))), 0));
	}
    
	public int beltslot(Coord c) {
	    for(int i = 0; i < 10; i++) {
		if(c.isect(beltc(i), invsq.sz()))
		    return(i + (curbelt * 12));
	    }
	    return(-1);
	}
    
	public void draw(GOut g) {
	    g.image(nkeybg, Coord.z);
	    for(int i = 0; i < 10; i++) {
		int slot = i + (curbelt * 12);
		Coord c = beltc(i);
		g.image(invsq, beltc(i));
		try {
		    if(belt[slot] != null) {
			belt[slot].draw(g.reclip(c.add(UI.scale(1), UI.scale(1)), invsq.sz().sub(UI.scale(2), UI.scale(2))));
		    }
		} catch(Loading e) {}
		g.chcolor(156, 180, 158, 255);
		FastText.aprintf(g, c.add(invsq.sz().sub(UI.scale(2), 0)), 1, 1, "%d", (i + 1) % 10);
		g.chcolor();
	    }
	    super.draw(g);
	}
	
	public boolean globtype(char key, KeyEvent ev) {
	    int c = ev.getKeyCode();
	    if((c < KeyEvent.VK_0) || (c > KeyEvent.VK_9))
		return(false);
	    int i = Utils.floormod(c - KeyEvent.VK_0 - 1, 10);
	    boolean M = (ev.getModifiersEx() & (KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)) != 0;
	    if(M) {
		curbelt = i;
	    } else {
		keyact(i + (curbelt * 12));
	    }
	    return(true);
	}
    }
    
    {
	String val = Utils.getpref("belttype", "n");
	if(val.equals("n")) {
	    beltwdg = add(new NKeyBelt());
	} else if(val.equals("f")) {
	    beltwdg = add(new FKeyBelt());
	} else {
	    beltwdg = add(new NKeyBelt());
	}
    }
    
    private Map<String, Console.Command> cmdmap = new TreeMap<String, Console.Command>();
    {
	cmdmap.put("afk", new Console.Command() {
		public void run(Console cons, String[] args) {
		    afk = true;
		    wdgmsg("afk");
		}
	    });
	cmdmap.put("act", new Console.Command() {
		public void run(Console cons, String[] args) {
		    Object[] ad = new Object[args.length - 1];
		    System.arraycopy(args, 1, ad, 0, ad.length);
		    wdgmsg("act", ad);
		}
	    });
	cmdmap.put("belt", new Console.Command() {
		public void run(Console cons, String[] args) {
		    if(args[1].equals("f")) {
			beltwdg.destroy();
			beltwdg = add(new FKeyBelt());
			Utils.setpref("belttype", "f");
			resize(sz);
		    } else if(args[1].equals("n")) {
			beltwdg.destroy();
			beltwdg = add(new NKeyBelt());
			Utils.setpref("belttype", "n");
			resize(sz);
		    }
		}
	    });
	cmdmap.put("chrmap", new Console.Command() {
		public void run(Console cons, String[] args) {
		    Utils.setpref("mapfile/" + chrid, args[1]);
		}
	    });
	cmdmap.put("tool", new Console.Command() {
		public void run(Console cons, String[] args) {
		    try {
			Object[] wargs = new Object[args.length - 2];
			for(int i = 0; i < wargs.length; i++)
			    wargs[i] = args[i + 2];
			add(gettype(args[1]).create(ui, wargs), 200, 200);
		    } catch(RuntimeException e) {
			e.printStackTrace(Debug.log);
		    }
		}
	    });
    }
    public Map<String, Console.Command> findcmds() {
	return(cmdmap);
    }
}
