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
import java.awt.event.KeyEvent;
import static haven.Inventory.invsq;

public class GameUI extends ConsoleHost implements Console.Directory {
    public static final Text.Foundry errfoundry = new Text.Foundry(Text.dfont, 14, new Color(192, 0, 0));
    private static final int cnto = 135;
    public final String chrid;
    public final long plid;
    public Avaview portrait;
    public MenuGrid menu;
    public MapView map;
    public Widget mmap;
    public Fightview fv;
    public FightWnd fw;
    private Widget[] meters = {};
    private Text lasterr;
    private long errtime;
    private Window invwnd, equwnd, makewnd;
    public Inventory maininv;
    public CharWnd chrwdg;
    public BuddyWnd buddies;
    public Polity polity;
    public HelpWnd help;
    public OptWnd opts;
    public Collection<DraggedItem> hand = new LinkedList<DraggedItem>();
    private WItem vhand;
    public ChatUI chat;
    public ChatUI.Channel syslog;
    public int prog = -1;
    private boolean afk = false;
    @SuppressWarnings("unchecked")
    public Indir<Resource>[] belt = new Indir[144];
    public Belt beltwdg;
    public String polowner;
    public Bufflist buffs;

    public abstract class Belt extends Widget {
	public Belt(Coord sz) {
	    super(sz);
	}

	public void keyact(final int slot) {
	    if(map != null) {
		Coord mvc = map.rootxlate(ui.mc);
		if(mvc.isect(Coord.z, map.sz)) {
		    map.delay(map.new Hittest(mvc) {
			    protected void hit(Coord pc, Coord mc, MapView.ClickInfo inf) {
				if(inf == null)
				    GameUI.this.wdgmsg("belt", slot, 1, ui.modflags(), mc);
				else
				    GameUI.this.wdgmsg("belt", slot, 1, ui.modflags(), mc, (int)inf.gob.id, inf.gob.rc);
			    }
			    
			    protected void nohit(Coord pc) {
				GameUI.this.wdgmsg("belt", slot, 1, ui.modflags());
			    }
			});
		}
	    }
	}
    }
    
    @RName("gameui")
    public static class $_ implements Factory {
	public Widget create(Widget parent, Object[] args) {
	    String chrid = (String)args[0];
	    int plid = (Integer)args[1];
	    return(new GameUI(chrid, plid));
	}
    }
    
    public GameUI(String chrid, long plid) {
	this.chrid = chrid;
	this.plid = plid;
	setcanfocus(true);
	setfocusctl(true);
	menu = add(new MenuGrid());
	portrait = add(new Avaview(Avaview.dasz, plid, "avacam"), new Coord(10, 10));
	buffs = add(new Bufflist(), new Coord(95, 50));
	chat = add(new ChatUI(0));
	syslog = chat.add(new ChatUI.Log("System"));
	opts = add(new OptWnd());
	opts.hide();
    }

    protected void added() {
	resize(parent.sz);
	ui.cons.out = new java.io.PrintWriter(new java.io.Writer() {
		StringBuilder buf = new StringBuilder();
		
		public void write(char[] src, int off, int len) {
		    buf.append(src, off, len);
		    int p;
		    while((p = buf.indexOf("\n")) >= 0) {
			syslog.append(buf.substring(0, p), Color.WHITE);
			buf.delete(0, p + 1);
		    }
		}
		
		public void close() {}
		public void flush() {}
	    });
	Debug.log = ui.cons.out;
	opts.c = sz.sub(opts.sz).div(2);
    }
    
    static class Hidewnd extends Window {
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

    public void addchild(Widget child, Object... args) {
	String place = ((String)args[0]).intern();
	if(place == "mapview") {
	    child.resize(sz);
	    map = add((MapView)child, Coord.z);
	    map.lower();
	    if(mmap != null)
		ui.destroy(mmap);
	    mmap = adda(new Frame(new Coord(125, 125), true), 0, sz.y, 0, 1);
	    mmap.add(new LocalMiniMap(new Coord(125, 125), map));
	} else if(place == "fight") {
	    fv = adda((Fightview)child, sz.x, 0, 1, 0);
	} else if(place == "fmg") {
	    fw = add((FightWnd)child, 50, 50);
	    fw.hide();
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
	    add(invwnd, new Coord(100, 100));
	} else if(place == "equ") {
	    equwnd = new Hidewnd(Coord.z, "Equipment");
	    equwnd.add(child, Coord.z);
	    equwnd.pack();
	    equwnd.hide();
	    add(equwnd, new Coord(400, 10));
	} else if(place == "hand") {
	    GItem g = add((GItem)child);
	    Coord lc = (Coord)args[1];
	    hand.add(new DraggedItem(g, lc));
	    updhand();
	} else if(place == "chr") {
	    chrwdg = add((CharWnd)child, new Coord(300, 50));
	    chrwdg.hide();
	} else if(place == "craft") {
	    final Widget mkwdg = child;
	    makewnd = new Window(Coord.z, "Crafting") {
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
		};
	    makewnd.add(mkwdg, Coord.z);
	    makewnd.pack();
	    add(makewnd, new Coord(400, 200));
	} else if(place == "buddy") {
	    buddies = add((BuddyWnd)child, 187, 50);
	    buddies.hide();
	} else if(place == "pol") {
	    polity = add((Polity)child, 500, 50);
	    polity.hide();
	} else if(place == "chat") {
	    chat.addchild(child);
	} else if(place == "party") {
	    add(child, 10, 95);
	} else if(place == "meter") {
	    int x = (meters.length % 3) * 65;
	    int y = (meters.length / 3) * 20;
	    add(child, portrait.c.x + portrait.sz.x + 10 + x, portrait.c.y + y);
	    meters = Utils.extend(meters, child);
	} else if(place == "buff") {
	    buffs.addchild(child);
	} else if(place == "misc") {
	    add(child, (Coord)args[1]);
	} else {
	    throw(new UI.UIException("Illegal gameui child", place, args));
	}
    }
    
    public void cdestroy(Widget w) {
	if(w instanceof GItem) {
	    for(Iterator<DraggedItem> i = hand.iterator(); i.hasNext();) {
		DraggedItem di = i.next();
		if(di.item == w) {
		    i.remove();
		    updhand();
		}
	    }
	} else if(w == polity) {
	    polity = null;
	} else if(w == chrwdg) {
	    chrwdg = null;
	} else if(w == fw) {
	    fw = null;
	}
    }
    
    static final Tex[] progt;
    static {
	Tex[] p = new Tex[22];
	for(int i = 0; i < p.length; i++)
	    p[i] = Resource.loadtex(String.format("gfx/hud/prog/%02d", i));
	progt = p;
    }
    public void draw(GOut g) {
	boolean beltp = !chat.visible;
	beltwdg.show(beltp);
	super.draw(g);
	if(prog >= 0) {
	    Tex pi = progt[Utils.clip((prog * progt.length + 50) / 100, 0, progt.length - 1)];
	    g.aimage(pi, new Coord(sz.x / 2, (sz.y * 4) / 10), 0.5, 0.5);
	}
	int by = sz.y;
	if(chat.visible)
	    by = Math.min(by, chat.c.y);
	if(beltwdg.visible)
	    by = Math.min(by, beltwdg.c.y);
	if(cmdline != null) {
	    drawcmd(g, new Coord(cnto + 10, by -= 20));
	} else if(lasterr != null) {
	    if((System.currentTimeMillis() - errtime) > 3000) {
		lasterr = null;
	    } else {
		g.chcolor(0, 0, 0, 192);
		g.frect(new Coord(cnto + 8, by - 22), lasterr.sz().add(4, 4));
		g.chcolor();
		g.image(lasterr.tex(), new Coord(cnto + 10, by -= 20));
	    }
	}
	if(!chat.visible) {
	    chat.drawsmall(g, new Coord(cnto + 10, by), 50);
	}
    }
    
    public void tick(double dt) {
	super.tick(dt);
	if(!afk && (System.currentTimeMillis() - ui.lastevent > 300000)) {
	    afk = true;
	    wdgmsg("afk");
	} else if(afk && (System.currentTimeMillis() - ui.lastevent < 300000)) {
	    afk = false;
	}
    }
    
    public void uimsg(String msg, Object... args) {
	if(msg == "err") {
	    String err = (String)args[0];
	    error(err);
	} else if(msg == "prog") {
	    if(args.length > 0)
		prog = (Integer)args[0];
	    else
		prog = -1;
	} else if(msg == "setbelt") {
	    int slot = (Integer)args[0];
	    if(args.length < 2) {
		belt[slot] = null;
	    } else {
		belt[slot] = ui.sess.getres((Integer)args[1]);
	    }
	} else if(msg == "polowner") {
	    String o = (String)args[0];
	    boolean n = ((Integer)args[1]) != 0;
	    if(o.length() == 0)
		o = null;
	    else
		o = o.intern();
	    if(o != polowner) {
		if(map != null) {
		    if(o == null) {
			if(polowner != null)
			    map.setpoltext("Leaving " + polowner);
		    } else {
			map.setpoltext("Entering " + o);
		    }
		}
		polowner = o;
	    }
	} else if(msg == "showhelp") {
	    Indir<Resource> res = ui.sess.getres((Integer)args[0]);
	    if(help == null)
		help = adda(new HelpWnd(res), sz.div(2), 0.5, 0.5);
	    else
		help.res = res;
	} else {
	    super.uimsg(msg, args);
	}
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
	if(sender == menu) {
	    wdgmsg(msg, args);
	    return;
	} else if((sender == chrwdg) && (msg == "close")) {
	    chrwdg.hide();
	} else if((sender == buddies) && (msg == "close")) {
	    buddies.hide();
	} else if((sender == polity) && (msg == "close")) {
	    polity.hide();
	} else if((sender == help) && (msg == "close")) {
	    ui.destroy(help);
	    help = null;
	    return;
	} else if((sender == fw) && (msg == "close")) {
	    fw.hide();
	}
	super.wdgmsg(sender, msg, args);
    }

    private void fitwdg(Widget wdg) {
	if(wdg.c.x < 0)
	    wdg.c.x = 0;
	if(wdg.c.y < 0)
	    wdg.c.y = 0;
	if(wdg.c.x + wdg.sz.x > sz.x)
	    wdg.c.x = sz.x - wdg.sz.x;
	if(wdg.c.y + wdg.sz.y > sz.y)
	    wdg.c.y = sz.y - wdg.sz.y;
    }

    public boolean globtype(char key, KeyEvent ev) {
	if(key == ':') {
	    entercmd();
	    return(true);
	} else if(key == 9) {
	    if((invwnd != null) && invwnd.show(!invwnd.visible)) {
		invwnd.raise();
		fitwdg(invwnd);
	    }
	    return(true);
	} else if(key == 5) {
	    if((equwnd != null) && equwnd.show(!equwnd.visible)) {
		equwnd.raise();
		fitwdg(equwnd);
	    }
	    return(true);
	} else if(key == 20) {
	    if((chrwdg != null) && chrwdg.show(!chrwdg.visible)) {
		chrwdg.raise();
		fitwdg(chrwdg);
	    }
	} else if(key == 6) {
	    if((fw != null) && fw.show(!fw.visible)) {
		fw.raise();
		fitwdg(fw);
	    }
	} else if(key == 2) {
	    if((buddies != null) && buddies.show(!buddies.visible)) {
		buddies.raise();
		fitwdg(buddies);
		setfocus(buddies);
	    }
	    return(true);
	} else if(key == 16) {
	    if((polity != null) && polity.show(!polity.visible)) {
		polity.raise();
		fitwdg(polity);
		setfocus(polity);
	    }
	    return(true);
	} else if(key == 15) {
	    if(opts.show(!opts.visible)) {
		opts.raise();
		fitwdg(opts);
		setfocus(opts);
	    }
	    return(true);
	}
	return(super.globtype(key, ev));
    }
    
    public boolean mousedown(Coord c, int button) {
	return(super.mousedown(c, button));
    }

    public void resize(Coord sz) {
	this.sz = sz;
	menu.c = sz.sub(menu.sz);
	chat.resize(sz.x - cnto - menu.sz.x);
	chat.move(new Coord(cnto, sz.y));
	if(map != null)
	    map.resize(sz);
	if(mmap != null)
	    mmap.c = new Coord(0, sz.y - mmap.sz.y);
	if(fv != null)
	    fv.c = new Coord(sz.x - Fightview.width, 0);
	beltwdg.c = new Coord(cnto + 10, sz.y - beltwdg.sz.y - 5);
	super.resize(sz);
    }
    
    public void presize() {
	resize(parent.sz);
    }
    
    private static final Resource errsfx = Resource.load("sfx/error");
    public void error(String msg) {
	errtime = System.currentTimeMillis();
	lasterr = errfoundry.render(msg);
	syslog.append(msg, Color.RED);
	Audio.play(errsfx);
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
	    super(new Coord(450, 34));
	}

	private Coord beltc(int i) {
	    return(new Coord(((invsq.sz().x + 2) * i) + (10 * (i / 4)), 0));
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
			g.image(belt[slot].get().layer(Resource.imgc).tex(), c.add(1, 1));
		} catch(Loading e) {}
		g.chcolor(156, 180, 158, 255);
		FastText.aprintf(g, c.add(invsq.sz().sub(2, 0)), 1, 1, "F%d", i + 1);
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
	    if(key != 0)
		return(false);
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
    
    public class NKeyBelt extends Belt implements DTarget, DropTarget {
	public int curbelt = 0;

	public NKeyBelt() {
	    super(new Coord(368, 34));
	}
	
	private Coord beltc(int i) {
	    return(new Coord(((invsq.sz().x + 2) * i) + (10 * (i / 5)), 0));
	}
    
	private int beltslot(Coord c) {
	    for(int i = 0; i < 10; i++) {
		if(c.isect(beltc(i), invsq.sz()))
		    return(i + (curbelt * 12));
	    }
	    return(-1);
	}
    
	public void draw(GOut g) {
	    for(int i = 0; i < 10; i++) {
		int slot = i + (curbelt * 12);
		Coord c = beltc(i);
		g.image(invsq, beltc(i));
		try {
		    if(belt[slot] != null)
			g.image(belt[slot].get().layer(Resource.imgc).tex(), c.add(1, 1));
		} catch(Loading e) {}
		g.chcolor(156, 180, 158, 255);
		FastText.aprintf(g, c.add(invsq.sz().sub(2, 0)), 1, 1, "%d", (i + 1) % 10);
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
	    if(key != 0)
		return(false);
	    int c = ev.getKeyChar();
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
	cmdmap.put("tool", new Console.Command() {
		public void run(Console cons, String[] args) {
		    add(gettype(args[1]).create(GameUI.this, new Object[0]), 200, 200);
		}
	    });
    }
    public Map<String, Console.Command> findcmds() {
	return(cmdmap);
    }
}
