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

public class GameUI extends ConsoleHost implements Console.Directory {
    public final String chrid;
    public final int plid;
    public MenuGrid menu;
    public MapView map;
    public MiniMap mmap;
    public Fightview fv;
    public static final Text.Foundry errfoundry = new Text.Foundry(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 14), new Color(192, 0, 0));
    private Text lasterr;
    private long errtime;
    private Window invwnd, equwnd, makewnd;
    public Collection<GItem> hand = new LinkedList<GItem>();
    private WItem vhand;
    public int prog = -1;
    private boolean afk = false;
    
    static {
	addtype("gameui", new WidgetFactory() {
		public Widget create(Coord c, Widget parent, Object[] args) {
		    String chrid = (String)args[0];
		    int plid = (Integer)args[1];
		    return(new GameUI(parent, chrid, plid));
		}
	    });
    }
    
    public GameUI(Widget parent, String chrid, int plid) {
	super(Coord.z, parent.sz, parent);
	this.chrid = chrid;
	this.plid = plid;
	menu = new MenuGrid(Coord.z, this);
	new Avaview(new Coord(10, 10), this, plid);
	new Bufflist(new Coord(95, 50), this);
	resize(sz);
	setcanfocus(true);
	setfocusctl(true);
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
	    invwnd.visible = false;
	    return(inv);
	} else if(place == "equ") {
	    equwnd = new Hidewnd(new Coord(400, 10), Coord.z, this, "Equipment");
	    Widget equ = gettype(type).create(Coord.z, equwnd, cargs);
	    equwnd.pack();
	    equwnd.visible = false;
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
	}
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
	if(cmdline != null) {
	    drawcmd(g, new Coord(15, sz.y - 20));
	} else if(lasterr != null) {
	    if((System.currentTimeMillis() - errtime) > 3000) {
		lasterr = null;
	    } else {
		g.image(lasterr.tex(), new Coord(15, sz.y - 20));
	    }
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
	    error((String)args[0]);
	} else if(msg == "prog") {
	    if(args.length > 0)
		prog = (Integer)args[0];
	    else
		prog = -1;
	} else if(msg == "setbelt") {
	} else {
	    super.uimsg(msg, args);
	}
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
	if(sender == menu) {
	    wdgmsg(msg, args);
	    return;
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
	    if((invwnd != null) && (invwnd.visible = !invwnd.visible)) {
		invwnd.raise();
		fitwdg(invwnd);
	    }
	    return(true);
	} else if(key == 5) {
	    if((equwnd != null) && (equwnd.visible = !equwnd.visible)) {
		equwnd.raise();
		fitwdg(equwnd);
	    }
	    return(true);
	} else if(key == 1) {
	    wdgmsg("atkm");
	    return(true);
	}
	return(super.globtype(key, ev));
    }
    
    public void resize(Coord sz) {
	super.resize(sz);
	menu.c = sz.sub(menu.sz);
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
    }
    public Map<String, Console.Command> findcmds() {
	return(cmdmap);
    }
}
