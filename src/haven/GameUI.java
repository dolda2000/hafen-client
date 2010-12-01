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

public class GameUI extends ConsoleHost implements Console.Directory {
    public final String chrid;
    public final int plid;
    public MenuGrid menu;
    public MapView map;
    public static final Text.Foundry errfoundry = new Text.Foundry(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 14), new Color(192, 0, 0));
    private Text lasterr;
    private long errtime;
    private Window invwnd;
    
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
    }
    
    public Widget makechild(String type, Object[] pargs, Object[] cargs) {
	String place = ((String)pargs[0]).intern();
	if(place == "mapview") {
	    Coord cc = (Coord)cargs[0];
	    map = new MapView(Coord.z, sz, this, cc, plid);
	    map.lower();
	    return(map);
	} else if(place == "inv") {
	    invwnd = new Window(new Coord(100, 100), Coord.z, this, "Inventory");
	    Widget inv = gettype(type).create(Coord.z, invwnd, cargs);
	    invwnd.pack();
	    invwnd.visible = false;
	    return(inv);
	} else {
	    throw(new UI.UIException("Illegal gameui child", type, pargs));
	}
    }

    public void draw(GOut g) {
	super.draw(g);
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
    
    public void uimsg(String msg, Object... args) {
	if(msg == "err") {
	    error((String)args[0]);
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

    private void showinv(boolean show) {
	if(invwnd.visible = show) {
	    if(invwnd.c.x < 0)
		invwnd.c.x = 0;
	    if(invwnd.c.y < 0)
		invwnd.c.y = 0;
	    if(invwnd.c.x + invwnd.sz.x > sz.x)
		invwnd.c.x = sz.x - invwnd.sz.x;
	    if(invwnd.c.y + invwnd.sz.y > sz.y)
		invwnd.c.y = sz.y - invwnd.sz.y;
	}
    }

    public boolean globtype(char key, java.awt.event.KeyEvent ev) {
	if(key == ':') {
	    entercmd();
	    return(true);
	} else if(key == 9) {
	    if(invwnd != null)
		showinv(!invwnd.visible);
	    return(true);
	}
	return(super.globtype(key, ev));
    }
    
    public void resize(Coord sz) {
	super.resize(sz);
	menu.c = sz.sub(menu.sz);
	if(map != null)
	    map.resize(sz);
    }
    
    public void presize() {
	resize(parent.sz);
    }
    
    public void error(String msg) {
	errtime = System.currentTimeMillis();
	lasterr = errfoundry.render(msg);
    }

    private Map<String, Console.Command> cmdmap = new TreeMap<String, Console.Command>();
    {
	cmdmap.put("afk", new Console.Command() {
		public void run(Console cons, String[] args) {
		    wdgmsg("afk");
		}
	    });
    }
    public Map<String, Console.Command> findcmds() {
	return(cmdmap);
    }
}
