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

public class GameUI extends ConsoleHost implements DTarget, DropTarget, Console.Directory {
    public final String chrid;
    public final long plid;
    public MenuGrid menu;
    public MapView map;
    public MiniMap mmap;
    public Fightview fv;
    public static final Text.Foundry errfoundry = new Text.Foundry(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 14), new Color(192, 0, 0));
    private Text lasterr;
    private long errtime;
    private Window invwnd, equwnd, makewnd;
    public BuddyWnd buddies;
    public Polity polity;
    public Collection<GItem> hand = new LinkedList<GItem>();
    private WItem vhand;
    public ChatUI chat;
    public ChatUI.Channel syslog;
    public int prog = -1;
    private boolean afk = false;
    @SuppressWarnings("unchecked")
    public Indir<Resource>[] belt = new Indir[144];
    public Belt beltwdg;
    public String polowner;
    
    public abstract class Belt {
	public abstract int draw(GOut g, int by);
	public abstract boolean click(Coord c, int button);
	public abstract boolean key(KeyEvent ev);
	public abstract boolean item(Coord c);
	public abstract boolean thing(Coord c, Object thing);
    }
    
    static {
	addtype("gameui", new WidgetFactory() {
		public Widget create(Coord c, Widget parent, Object[] args) {
		    String chrid = (String)args[0];
		    int plid = (Integer)args[1];
		    return(new GameUI(parent, chrid, plid));
		}
	    });
    }
    
    public GameUI(Widget parent, String chrid, long plid) {
	super(Coord.z, parent.sz, parent);
	this.chrid = chrid;
	this.plid = plid;
	setcanfocus(true);
	setfocusctl(true);
	menu = new MenuGrid(Coord.z, this);
	new Avaview(new Coord(10, 10), this, plid);
	new Bufflist(new Coord(95, 50), this);
	chat = new ChatUI(Coord.z, 0, this);
	syslog = new ChatUI.Log(chat, "System");
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
	resize(sz);
    }
    
    static class Hidewnd extends Window {
	Hidewnd(Coord c, Coord sz, Widget parent, String cap) {
	    super(c, sz, parent, cap);
	}
	
	public void wdgmsg(Widget sender, String msg, Object... args) {
	    if((sender == this) && msg.equals("close")) {
		this.hide();
		return;
	    }
	    super.wdgmsg(sender, msg, args);
	}
    }

    private void updhand() {
	if((hand.isEmpty() && (vhand != null)) || ((vhand != null) && !hand.contains(vhand.item))) {
	    ui.destroy(vhand);
	    vhand = null;
	}
	if(!hand.isEmpty() && (vhand == null)) {
	    GItem fi = hand.iterator().next();
	    vhand = new ItemDrag(new Coord(15, 15), this, fi);
	}
    }

    public Widget makechild(String type, Object[] pargs, Object[] cargs) {
	String place = ((String)pargs[0]).intern();
	if(place == "mapview") {
	    Coord cc = (Coord)cargs[0];
	    map = new MapView(Coord.z, sz, this, cc, plid);
	    map.lower();
	    if(mmap != null)
		ui.destroy(mmap);
	    // mmap = new MiniMap(new Coord(0, sz.y - 125), new Coord(125, 125), this, map);
	    return(map);
	} else if(place == "fight") {
	    fv = (Fightview)gettype(type).create(new Coord(sz.x - Fightview.width, 0), this, cargs);
	    return(fv);
	} else if(place == "inv") {
	    invwnd = new Hidewnd(new Coord(100, 100), Coord.z, this, "Inventory");
	    Widget inv = gettype(type).create(Coord.z, invwnd, cargs);
	    invwnd.pack();
	    invwnd.hide();
	    return(inv);
	} else if(place == "equ") {
	    equwnd = new Hidewnd(new Coord(400, 10), Coord.z, this, "Equipment");
	    Widget equ = gettype(type).create(Coord.z, equwnd, cargs);
	    equwnd.pack();
	    equwnd.hide();
	    return(equ);
	} else if(place == "hand") {
	    GItem g = (GItem)gettype(type).create((Coord)pargs[1], this, cargs);
	    hand.add(g);
	    updhand();
	    return(g);
	} else if(place == "craft") {
	    final Widget[] mk = {null};
	    makewnd = new Window(new Coord(200, 100), Coord.z, this, "Crafting") {
		    public void wdgmsg(Widget sender, String msg, Object... args) {
			if((sender == this) && msg.equals("close")) {
			    mk[0].wdgmsg("close");
			    return;
			}
			super.wdgmsg(sender, msg, args);
		    }
		    public void cdestroy(Widget w) {
			if(w == mk[0]) {
			    ui.destroy(this);
			    makewnd = null;
			}
		    }
		};
	    mk[0] = gettype(type).create(Coord.z, makewnd, cargs);
	    makewnd.pack();
	    return(mk[0]);
	} else if(place == "buddy") {
	    buddies = (BuddyWnd)gettype(type).create(new Coord(187, 50), this, cargs);
	    buddies.hide();
	    return(buddies);
	} else if(place == "pol") {
	    polity = (Polity)gettype(type).create(new Coord(500, 50), this, cargs);
	    polity.hide();
	    return(polity);
	} else if(place == "chat") {
	    return(chat.makechild(type, new Object[] {}, cargs));
	} else if(place == "party") {
	    return(gettype(type).create(new Coord(10, 95), this, cargs));
	} else if(place == "misc") {
	    return(gettype(type).create((Coord)pargs[1], this, cargs));
	} else {
	    throw(new UI.UIException("Illegal gameui child", type, pargs));
	}
    }
    
    public void cdestroy(Widget w) {
	if((w instanceof GItem) && hand.contains(w)) {
	    hand.remove(w);
	    updhand();
	} else if(w == polity) {
	    polity = null;
	}
    }
    
    private boolean showbeltp() {
	return(!chat.expanded);
    }

    static Text.Foundry progf = new Text.Foundry(new java.awt.Font("serif", java.awt.Font.BOLD, 24));
    static {progf.aa = true;}
    Text progt = null;
    public void draw(GOut g) {
	super.draw(g);
	if(prog >= 0) {
	    String progs = String.format("%d%%", prog);
	    if((progt == null) || !progs.equals(progt.text))
		progt = progf.render(progs);
	    g.aimage(progt.tex(), new Coord(sz.x / 2, (sz.y * 4) / 10), 0.5, 0.5);
	}
	int by = sz.y;
	if(chat.expanded)
	    by -= chat.sz.y;
	if(showbeltp()) {
	    by -= beltwdg.draw(g, by);
	}
	if(cmdline != null) {
	    drawcmd(g, new Coord(135, by -= 20));
	} else if(lasterr != null) {
	    if((System.currentTimeMillis() - errtime) > 3000) {
		lasterr = null;
	    } else {
		g.image(lasterr.tex(), new Coord(135, by -= 20));
	    }
	}
	if(!chat.expanded) {
	    chat.drawsmall(g, new Coord(135, by), 50);
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
	    syslog.append(err, Color.RED);
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
	} else {
	    super.uimsg(msg, args);
	}
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
	if(sender == menu) {
	    wdgmsg(msg, args);
	    return;
	} else if((sender == buddies) && (msg == "close")) {
	    buddies.hide();
	} else if((sender == polity) && (msg == "close")) {
	    polity.hide();
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
	} else if(key == 2) {
	    if((buddies != null) && buddies.show(!buddies.visible)) {
		buddies.raise();
		fitwdg(buddies);
		setfocus(buddies);
	    }
	    return(true);
	} else if(key == 20) {
	    if((polity != null) && polity.show(!polity.visible)) {
		polity.raise();
		fitwdg(polity);
		setfocus(polity);
	    }
	    return(true);
	}
	if((key == 0) && beltwdg.key(ev))
	    return(true);
	return(super.globtype(key, ev));
    }
    
    public boolean mousedown(Coord c, int button) {
	if(beltwdg.click(c, button))
	    return(true);
	return(super.mousedown(c, button));
    }

    public boolean drop(Coord cc, Coord ul) {
	return(beltwdg.item(cc));
    }
    
    public boolean iteminteract(Coord cc, Coord ul) {
	return(false);
    }

    public boolean dropthing(Coord c, Object thing) {
	return(beltwdg.thing(c, thing));
    }
    
    public void resize(Coord sz) {
	super.resize(sz);
	menu.c = sz.sub(menu.sz);
	chat.resize(sz.x - 125 - menu.sz.x);
	chat.move(new Coord(125, sz.y));
	if(map != null)
	    map.resize(sz);
	if(mmap != null)
	    mmap.c = new Coord(0, sz.y - mmap.sz.y);
	if(fv != null)
	    fv.c = new Coord(sz.x - Fightview.width, 0);
    }
    
    public void presize() {
	resize(parent.sz);
    }
    
    public void error(String msg) {
	errtime = System.currentTimeMillis();
	lasterr = errfoundry.render(msg);
    }
    
    public void act(String... args) {
	wdgmsg("act", (Object[])args);
    }

    public class FKeyBelt extends Belt {
	public final int beltkeys[] = {KeyEvent.VK_F1, KeyEvent.VK_F2, KeyEvent.VK_F3, KeyEvent.VK_F4,
				       KeyEvent.VK_F5, KeyEvent.VK_F6, KeyEvent.VK_F7, KeyEvent.VK_F8,
				       KeyEvent.VK_F9, KeyEvent.VK_F10, KeyEvent.VK_F11, KeyEvent.VK_F12};
	public int curbelt = 0;
	
	private Coord beltc(int i) {
	    return(new Coord(/* ((sz.x - (invsq.sz().x * 12) - (2 * 11)) / 2) */
			     135
			     + ((invsq.sz().x + 2) * i)
			     + (10 * (i / 4)),
			     sz.y - invsq.sz().y - 2));
	}
    
	private int beltslot(Coord c) {
	    for(int i = 0; i < 12; i++) {
		if(c.isect(beltc(i), invsq.sz()))
		    return(i + (curbelt * 12));
	    }
	    return(-1);
	}
    
	public int draw(GOut g, int by) {
	    for(int i = 0; i < 12; i++) {
		int slot = i + (curbelt * 12);
		Coord c = beltc(i);
		g.image(invsq, beltc(i));
		try {
		    if(belt[slot] != null)
			g.image(belt[slot].get().layer(Resource.imgc).tex(), c.add(1, 1));
		} catch(Loading e) {}
		g.chcolor(156, 180, 158, 255);
		FastText.aprintf(g, c.add(invsq.sz()), 1, 1, "F%d", i + 1);
		g.chcolor();
	    }
	    return(invsq.sz().y);
	}
	
	public boolean click(Coord c, int button) {
	    int slot = beltslot(c);
	    if(slot != -1) {
		if(button == 1)
		    wdgmsg("belt", slot, 1, ui.modflags());
		if(button == 3)
		    wdgmsg("setbelt", slot, 1);
		return(true);
	    }
	    return(false);
	}

	public boolean key(KeyEvent ev) {
	    boolean M = (ev.getModifiersEx() & (KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)) != 0;
	    for(int i = 0; i < beltkeys.length; i++) {
		if(ev.getKeyCode() == beltkeys[i]) {
		    if(M) {
			curbelt = i;
			return(true);
		    } else {
			wdgmsg("belt", i + (curbelt * 12), 1, ui.modflags());
			return(true);
		    }
		}
	    }
	    return(false);
	}
	
	public boolean item(Coord c) {
	    int slot = beltslot(c);
	    if(slot != -1) {
		wdgmsg("setbelt", slot, 0);
		return(true);
	    }
	    return(false);
	}
	
	public boolean thing(Coord c, Object thing) {
	    int slot = beltslot(c);
	    if(slot != -1) {
		if(thing instanceof Resource) {
		    Resource res = (Resource)thing;
		    if(res.layer(Resource.action) != null) {
			wdgmsg("setbelt", slot, res.name);
			return(true);
		    }
		}
	    }
	    return(false);
	}
    }
    
    public class NKeyBelt extends Belt {
	public int curbelt = 0;
	
	private Coord beltc(int i) {
	    return(new Coord(/* ((sz.x - (invsq.sz().x * 12) - (2 * 11)) / 2) */
			     135
			     + ((invsq.sz().x + 2) * i)
			     + (10 * (i / 5)),
			     sz.y - invsq.sz().y - 2));
	}
    
	private int beltslot(Coord c) {
	    for(int i = 0; i < 10; i++) {
		if(c.isect(beltc(i), invsq.sz()))
		    return(i + (curbelt * 12));
	    }
	    return(-1);
	}
    
	public int draw(GOut g, int by) {
	    for(int i = 0; i < 10; i++) {
		int slot = i + (curbelt * 12);
		Coord c = beltc(i);
		g.image(invsq, beltc(i));
		try {
		    if(belt[slot] != null)
			g.image(belt[slot].get().layer(Resource.imgc).tex(), c.add(1, 1));
		} catch(Loading e) {}
		g.chcolor(156, 180, 158, 255);
		FastText.aprintf(g, c.add(invsq.sz()), 1, 1, "%d", (i + 1) % 10);
		g.chcolor();
	    }
	    return(invsq.sz().y);
	}
	
	public boolean click(Coord c, int button) {
	    int slot = beltslot(c);
	    if(slot != -1) {
		if(button == 1)
		    wdgmsg("belt", slot, 1, ui.modflags());
		if(button == 3)
		    wdgmsg("setbelt", slot, 1);
		return(true);
	    }
	    return(false);
	}

	public boolean key(KeyEvent ev) {
	    int c = ev.getKeyChar();
	    if((c < KeyEvent.VK_0) || (c > KeyEvent.VK_9))
		return(false);
	    int i = Utils.floormod(c - KeyEvent.VK_0 - 1, 10);
	    boolean M = (ev.getModifiersEx() & (KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)) != 0;
	    if(M)
		curbelt = i;
	    else
		wdgmsg("belt", i + (curbelt * 12), 1, ui.modflags());
	    return(true);
	}
	
	public boolean item(Coord c) {
	    int slot = beltslot(c);
	    if(slot != -1) {
		wdgmsg("setbelt", slot, 0);
		return(true);
	    }
	    return(false);
	}
	
	public boolean thing(Coord c, Object thing) {
	    int slot = beltslot(c);
	    if(slot != -1) {
		if(thing instanceof Resource) {
		    Resource res = (Resource)thing;
		    if(res.layer(Resource.action) != null) {
			wdgmsg("setbelt", slot, res.name);
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
	    beltwdg = new NKeyBelt();
	} else if(val.equals("f")) {
	    beltwdg = new FKeyBelt();
	} else {
	    beltwdg = new NKeyBelt();
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
			beltwdg = new FKeyBelt();
			Utils.setpref("belttype", "f");
		    } else if(args[1].equals("n")) {
			beltwdg = new NKeyBelt();
			Utils.setpref("belttype", "n");
		    }
		}
	    });
    }
    public Map<String, Console.Command> findcmds() {
	return(cmdmap);
    }
}
