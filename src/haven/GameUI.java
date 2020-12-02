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
import static haven.Inventory.invsq;

public class GameUI extends ConsoleHost implements Console.Directory {
    public static final Text.Foundry msgfoundry = new Text.Foundry(Text.dfont, 14);
    private static final int blpw = UI.scale(142), brpw = UI.scale(142);
    public final String chrid, genus;
    public final long plid;
    private final Hidepanel ulpanel, umpanel, urpanel, blpanel, mapmenupanel, brpanel, menupanel;
    public Avaview portrait;
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
    public double prog = -1;
    private boolean afk = false;
    public BeltSlot[] belt = new BeltSlot[144];
    public Belt beltwdg;
    public final Map<Integer, String> polowners = new HashMap<Integer, String>();
    public Bufflist buffs;

    private static final OwnerContext.ClassResolver<BeltSlot> beltctxr = new OwnerContext.ClassResolver<BeltSlot>()
	.add(Glob.class, slot -> slot.wdg().ui.sess.glob)
	.add(Session.class, slot -> slot.wdg().ui.sess);
    public class BeltSlot implements GSprite.Owner {
	public final int idx;
	public final Indir<Resource> res;
	public final Message sdt;

	public BeltSlot(int idx, Indir<Resource> res, Message sdt) {
	    this.idx = idx;
	    this.res = res;
	    this.sdt = sdt;
	}

	private GSprite spr = null;
	public GSprite spr() {
	    GSprite ret = this.spr;
	    if(ret == null)
		ret = this.spr = GSprite.create(this, res.get(), Message.nil);
	    return(ret);
	}

	public Resource getres() {return(res.get());}
	public Random mkrandoom() {return(new Random(System.identityHashCode(this)));}
	public <T> T context(Class<T> cl) {return(beltctxr.context(cl, this));}
	private GameUI wdg() {return(GameUI.this);}
    }

    public abstract class Belt extends Widget {
	public Belt(Coord sz) {
	    super(sz);
	}

	public void keyact(final int slot) {
	    if(map != null) {
		Coord mvc = map.rootxlate(ui.mc);
		if(mvc.isect(Coord.z, map.sz)) {
		    map.new Hittest(mvc) {
			    protected void hit(Coord pc, Coord2d mc, ClickData inf) {
				Object[] args = {slot, 1, ui.modflags(), mc.floor(OCache.posres)};
				if(inf != null)
				    args = Utils.extend(args, inf.clickargs());
				GameUI.this.wdgmsg("belt", args);
			    }
			    
			    protected void nohit(Coord pc) {
				GameUI.this.wdgmsg("belt", slot, 1, ui.modflags());
			    }
			}.run();
		}
	    }
	}
    }
    
    @RName("gameui")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    String chrid = (String)args[0];
	    int plid = (Integer)args[1];
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
	chat = add(new ChatUI(0, 0));
	if(Utils.getprefb("chatvis", true)) {
	    chat.hresize(chat.savedh);
	    chat.show();
	}
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
	portrait = ulpanel.add(new Avaview(Avaview.dasz, plid, "avacam") {
		public boolean mousedown(Coord c, int button) {
		    return(true);
		}
	    }, UI.scale(new Coord(10, 10)));
	buffs = ulpanel.add(new Bufflist(), UI.scale(new Coord(95, 65)));
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
			    lines.add(buf.substring(0, p));
			    buf.delete(0, p + 1);
			}
		    }
		    for(String ln : lines)
			syslog.append(ln, Color.WHITE);
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
		    if(!wnd.visible)
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
		    score -= (1 - Math.pow(tarea.closest(plc).dist(plc) / sz.dist(Coord.z), 2)) * 1.5;
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
	    Utils.setprefc("wndc-chr", chrwdg.sz);
	if(zerg != null)
	    Utils.setprefc("wndc-zerg", zerg.c);
	if(mapfile != null) {
	    Utils.setprefc("wndc-map", mapfile.c);
	    Utils.setprefc("wndsz-map", mapfile.asz);
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
	    if(Config.mapbase != null) {
		try {
		    mapstore = HashDirCache.get(Config.mapbase.toURI());
		} catch(java.net.URISyntaxException e) {
		}
	    }
	    if(mapstore != null) {
		MapFile file = MapFile.load(mapstore, mapfilename());
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
	    final Widget mkwdg = child;
	    makewnd = new Window(Coord.z, "Crafting", true) {
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
	    add(child, UI.scale(10), UI.scale(95));
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
			return(new Coord(10, GameUI.this.sz.y - blpanel.sz.y - this.sz.y - 10));
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
    
    private static final Resource.Anim progt = Resource.local().loadwait("gfx/hud/prog").layer(Resource.animc);
    private Tex curprog = null;
    private int curprogf, curprogb;
    private void drawprog(GOut g, double prog) {
	int fr = Utils.clip((int)Math.floor(prog * progt.f.length), 0, progt.f.length - 2);
	int bf = Utils.clip((int)(((prog * progt.f.length) - fr) * 255), 0, 255);
	if((curprog == null) || (curprogf != fr) || (curprogb != bf)) {
	    if(curprog != null)
		curprog.dispose();
	    WritableRaster buf = PUtils.imgraster(progt.f[fr][0].ssz);
	    PUtils.blit(buf, progt.f[fr][0].scaled().getRaster(), Coord.z);
	    PUtils.blendblit(buf, progt.f[fr + 1][0].scaled().getRaster(), Coord.z, bf);
	    curprog = new TexI(PUtils.rasterimg(buf)); curprogf = fr; curprogb = bf;
	}
	g.aimage(curprog, new Coord(sz.x / 2, (sz.y * 4) / 10), 0.5, 0.5);
    }

    public void draw(GOut g) {
	beltwdg.c = new Coord(chat.c.x, Math.min(chat.c.y - beltwdg.sz.y, sz.y - beltwdg.sz.y));
	super.draw(g);
	if(prog >= 0)
	    drawprog(g, prog);
	int by = sz.y;
	if(chat.visible)
	    by = Math.min(by, chat.c.y);
	if(beltwdg.visible)
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
	if(!chat.visible) {
	    chat.drawsmall(g, new Coord(blpw + UI.scale(10), by), UI.scale(50));
	}
    }
    
    private String iconconfname() {
	StringBuilder buf = new StringBuilder();
	buf.append("data/mm-icons");
	if(genus != null)
	    buf.append("/" + genus);
	if(ui.sess != null)
	    buf.append("/" + ui.sess.username);
	return(buf.toString());
    }

    private GobIcon.Settings loadiconconf() {
	if(ResCache.global == null)
	    return(new GobIcon.Settings());
	try {
	    try(StreamMessage fp = new StreamMessage(ResCache.global.fetch(iconconfname()))) {
		return(GobIcon.Settings.load(fp));
	    }
	} catch(java.io.FileNotFoundException e) {
	    return(new GobIcon.Settings());
	} catch(Exception e) {
	    new Warning(e, "failed to load icon-conf").issue();
	    return(new GobIcon.Settings());
	}
    }

    public void saveiconconf() {
	if(ResCache.global == null)
	    return;
	try {
	    try(StreamMessage fp = new StreamMessage(ResCache.global.store(iconconfname()))) {
		iconconf.save(fp);
	    }
	} catch(Exception e) {
	    new Warning(e, "failed to store icon-conf").issue();
	}
    }

    public class CornerMap extends MiniMap {
	public CornerMap(Coord sz, MapFile file) {
	    super(sz, file);
	    follow(new MapLocator(map));
	}

	public boolean dragp(int button) {
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
	    error(err);
	} else if(msg == "msg") {
	    String text = (String)args[0];
	    msg(text);
	} else if(msg == "prog") {
	    if(args.length > 0)
		prog = ((Number)args[0]).doubleValue() / 100.0;
	    else
		prog = -1;
	} else if(msg == "setbelt") {
	    int slot = (Integer)args[0];
	    if(args.length < 2) {
		belt[slot] = null;
	    } else {
		Indir<Resource> res = ui.sess.getres((Integer)args[1]);
		Message sdt = Message.nil;
		if(args.length > 2)
		    sdt = new MessageBuf((byte[])args[2]);
		belt[slot] = new BeltSlot(slot, res, sdt);
	    }
	} else if(msg == "polowner") {
	    int id = (Integer)args[0];
	    String o = (String)args[1];
	    boolean n = ((Integer)args[2]) != 0;
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
	    Indir<Resource> res = ui.sess.getres((Integer)args[0]);
	    if(help == null)
		help = adda(new HelpWnd(res), 0.5, 0.25);
	    else
		help.res = res;
	} else if(msg == "map-mark") {
	    long gobid = Utils.uint32((Integer)args[0]);
	    long oid = (Long)args[1];
	    Indir<Resource> res = ui.sess.getres((Integer)args[2]);
	    String nm = (String)args[3];
	    if(mapfile != null)
		mapfile.markobj(gobid, oid, res, nm);
	} else if(msg == "map-icons") {
	    GobIcon.Settings conf = this.iconconf;
	    int tag = (Integer)args[0];
	    if(args.length < 2) {
		if(conf.tag != tag)
		    wdgmsg("map-icons", conf.tag);
	    } else if(args[1] instanceof String) {
		Resource.Spec res = new Resource.Spec(null, (String)args[1], (Integer)args[2]);
		GobIcon.Setting cset = new GobIcon.Setting(res);
		boolean has = conf.settings.containsKey(res.name);
		cset.show = cset.defshow = ((Integer)args[3]) != 0;
		conf.receive(tag, new GobIcon.Setting[] {cset});
		saveiconconf();
		if(!has && conf.notify) {
		    ui.sess.glob.loader.defer(() -> {
			    Resource lres = Resource.remote().load(res.name, res.ver).get();
			    Resource.Tooltip tip = lres.layer(Resource.tooltip);
			    if(tip != null)
				msg(String.format("%s added to list of seen icons.", tip.t));
			}, (Supplier<Object>)() -> null);
		}
	    } else if(args[1] instanceof Object[]) {
		Object[] sub = (Object[])args[1];
		int a = 0;
		Collection<GobIcon.Setting> csets = new ArrayList<>();
		while(a < sub.length) {
		    String resnm = (String)sub[a++];
		    int resver = (Integer)sub[a++];
		    int fl = (Integer)sub[a++];
		    Resource.Spec res = new Resource.Spec(null, resnm, resver);
		    GobIcon.Setting cset = new GobIcon.Setting(res);
		    cset.show = cset.defshow = ((fl & 1) != 0);
		    csets.add(cset);
		}
		conf.receive(tag, csets.toArray(new GobIcon.Setting[0]));
		saveiconconf();
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
	return(wnd.visible);
    }

    private void togglewnd(Window wnd) {
	if(wnd != null) {
	    if(wnd.show(!wnd.visible)) {
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
	private void toggleol(int id, boolean a) {
	    if(map != null) {
		if(a) {
		    map.enol(id); map.enol(id + 1);
		} else {
		    map.disol(id); map.disol(id + 1);
		}
	    }
	}

	public MapMenu() {
	    super(mapmenubg.sz());
	    add(new MenuCheckBox("lbtn-claim", kb_claim, "Display personal claims"), 0, 0).changed(a -> toggleol(0, a));
	    add(new MenuCheckBox("lbtn-vil", kb_vil, "Display village claims"), 0, 0).changed(a -> toggleol(2, a));
	    add(new MenuCheckBox("lbtn-rlm", kb_rlm, "Display realms"), 0, 0).changed(a -> toggleol(4, a));
	    add(new MenuCheckBox("lbtn-map", kb_map, "Map")).state(() -> wndstate(mapfile)).click(() -> {
		    togglewnd(mapfile);
		    if(mapfile != null)
			Utils.setprefb("wndvis-map", mapfile.visible);
		});
	    add(new MenuCheckBox("lbtn-ico", kb_ico, "Icon settings"), 0, 0).state(() -> wndstate(iconwnd)).click(() -> {
		    if(iconconf == null)
			return;
		    if(iconwnd == null) {
			iconwnd = new GobIcon.SettingsWindow(iconconf, () -> Utils.defer(GameUI.this::saveiconconf));
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
    public boolean globtype(char key, KeyEvent ev) {
	if(key == ':') {
	    entercmd();
	    return(true);
	} else if((Config.screenurl != null) && kb_shoot.key().match(ev)) {
	    Screenshooter.take(this, Config.screenurl);
	    return(true);
	} else if(kb_hide.key().match(ev)) {
	    toggleui();
	    return(true);
	} else if(kb_chat.key().match(ev)) {
	    if(chat.visible && !chat.hasfocus) {
		setfocus(chat);
	    } else {
		if(chat.targeth == 0) {
		    chat.sresize(chat.savedh);
		    setfocus(chat);
		} else {
		    chat.sresize(0);
		}
	    }
	    Utils.setprefb("chatvis", chat.targeth != 0);
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
	this.sz = sz;
	chat.resize(sz.x - blpw - brpw);
	chat.move(new Coord(blpw, sz.y));
	if(map != null)
	    map.resize(sz);
	beltwdg.c = new Coord(blpw + UI.scale(10), sz.y - beltwdg.sz.y - UI.scale(5));
	super.resize(sz);
    }
    
    public void presize() {
	resize(parent.sz);
    }
    
    public void msg(String msg, Color color, Color logcol) {
	msgtime = Utils.rtime();
	lastmsg = msgfoundry.render(msg, color);
	syslog.append(msg, logcol);
    }

    public void msg(String msg, Color color) {
	msg(msg, color, color);
    }

    private static final Resource errsfx = Resource.local().loadwait("sfx/error");
    private double lasterrsfx = 0;
    public void error(String msg) {
	msg(msg, new Color(192, 0, 0), new Color(255, 0, 0));
	double now = Utils.rtime();
	if(now - lasterrsfx > 0.1) {
	    Audio.play(errsfx);
	    lasterrsfx = now;
	}
    }

    private static final Resource msgsfx = Resource.local().loadwait("sfx/msg");
    private double lastmsgsfx = 0;
    public void msg(String msg) {
	msg(msg, Color.WHITE, Color.WHITE);
	double now = Utils.rtime();
	if(now - lastmsgsfx > 0.1) {
	    Audio.play(msgsfx);
	    lastmsgsfx = now;
	}
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
    
	private int beltslot(Coord c) {
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
			belt[slot].spr().draw(g.reclip(c.add(UI.scale(1), UI.scale(1)), invsq.sz().sub(UI.scale(2), UI.scale(2))));
		} catch(Loading e) {}
		g.chcolor(156, 180, 158, 255);
		FastText.aprintf(g, c.add(invsq.sz().sub(UI.scale(2), 0)), 1, 1, "F%d", i + 1);
		g.chcolor();
	    }
	}
	
	public boolean mousedown(Coord c, int button) {
	    int slot = beltslot(c);
	    if(slot != -1) {
		if(button == 1)
		    GameUI.this.wdgmsg("belt", slot, 1, ui.modflags());
		if(button == 3)
		    GameUI.this.wdgmsg("setbelt", slot, 1);
		return(true);
	    }
	    return(false);
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
		if(thing instanceof Resource) {
		    Resource res = (Resource)thing;
		    if(res.layer(Resource.action) != null) {
			GameUI.this.wdgmsg("setbelt", slot, res.name);
			return(true);
		    }
		}
	    }
	    return(false);
	}
    }
    
    private static final Tex nkeybg = Resource.loadtex("gfx/hud/hb-main");
    public class NKeyBelt extends Belt implements DTarget, DropTarget {
	public int curbelt = 0;
	final Coord pagoff = UI.scale(new Coord(5, 25));

	public NKeyBelt() {
	    super(nkeybg.sz());
	    adda(new IButton("gfx/hud/hb-btn-chat", "", "-d", "-h") {
		    Tex glow;
		    {
			this.tooltip = RichText.render("Chat ($col[255,255,0]{Ctrl+C})", 0);
			glow = new TexI(PUtils.rasterimg(PUtils.blurmask(up.getRaster(), 2, 2, Color.WHITE)));
		    }

		    public void click() {
			if(chat.targeth == 0) {
			    chat.sresize(chat.savedh);
			    setfocus(chat);
			} else {
			    chat.sresize(0);
			}
			Utils.setprefb("chatvis", chat.targeth != 0);
		    }

		    public void draw(GOut g) {
			super.draw(g);
			Color urg = chat.urgcols[chat.urgency];
			if(urg != null) {
			    GOut g2 = g.reclipl(new Coord(-2, -2), g.sz().add(4, 4));
			    g2.chcolor(urg.getRed(), urg.getGreen(), urg.getBlue(), 128);
			    g2.image(glow, Coord.z, UI.scale(glow.sz()));
			}
		    }
		}, sz, 1, 1);
	}
	
	private Coord beltc(int i) {
	    return(pagoff.add(UI.scale((36 * i) + (10 * (i / 5))), 0));
	}
    
	private int beltslot(Coord c) {
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
			belt[slot].spr().draw(g.reclip(c.add(UI.scale(1), UI.scale(1)), invsq.sz().sub(UI.scale(2), UI.scale(2))));
		    }
		} catch(Loading e) {}
		g.chcolor(156, 180, 158, 255);
		FastText.aprintf(g, c.add(invsq.sz().sub(UI.scale(2), 0)), 1, 1, "%d", (i + 1) % 10);
		g.chcolor();
	    }
	    super.draw(g);
	}
	
	public boolean mousedown(Coord c, int button) {
	    int slot = beltslot(c);
	    if(slot != -1) {
		if(button == 1)
		    GameUI.this.wdgmsg("belt", slot, 1, ui.modflags());
		if(button == 3)
		    GameUI.this.wdgmsg("setbelt", slot, 1);
		return(true);
	    }
	    return(super.mousedown(c, button));
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
		if(thing instanceof Resource) {
		    Resource res = (Resource)thing;
		    if(res.layer(Resource.action) != null) {
			GameUI.this.wdgmsg("setbelt", slot, res.name);
			return(true);
		    }
		}
	    }
	    return(false);
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
			add(gettype(args[1]).create(ui, new Object[0]), 200, 200);
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
