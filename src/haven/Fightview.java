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

public class Fightview extends Widget {
    static Tex bg = Resource.loadtex("gfx/hud/bosq");
    static int height = 5;
    static int ymarg = 5;
    static int width = 135;
    static Coord avasz = new Coord(27, 27);
    static Coord cavac = new Coord(35, 10);
    static Coord cgivec = new Coord(0, 10);
    LinkedList<Relation> lsrel = new LinkedList<Relation>();
    public Relation current = null;
    public Indir<Resource> blk, batk, iatk;
    public long atkc = -1;
    public int off, def;
    private GiveButton curgive;
    private Avaview curava;
    
    public class Relation {
        long gobid;
        Avaview ava;
	GiveButton give;
        
        public Relation(long gobid) {
            this.gobid = gobid;
            this.ava = new Avaview(Coord.z, avasz, Fightview.this, gobid, "avacam");
	    this.give = new GiveButton(Coord.z, Fightview.this, 0, new Coord(15, 15));
        }
	
	public void give(int state) {
	    if(this == current)
		curgive.state = state;
	    this.give.state = state;
	}
	
	public void show(boolean state) {
	    ava.show(state);
	    give.show(state);
	}
	
	public void remove() {
	    ui.destroy(ava);
	    ui.destroy(give);
	}
    }
    
    static {
        Widget.addtype("frv", new WidgetFactory() {
            public Widget create(Coord c, Widget parent, Object[] args) {
                return(new Fightview(c, parent));
            }
        });
    }
    
    public Fightview(Coord c, Widget parent) {
        super(c, new Coord(width, (bg.sz().y + ymarg) * height), parent);
    }

    private void setcur(Relation rel) {
	if((current == null) && (rel != null)) {
	    curgive = new GiveButton(cgivec, this, 0) {
		    public void wdgmsg(String name, Object... args) {
			if(name == "click")
			    Fightview.this.wdgmsg("give", (int)current.gobid, args[0]);
		    }
		};
	    curava = new Avaview(cavac, Avaview.dasz, this, rel.gobid, "avacam") {
		    public void wdgmsg(String name, Object... args) {
			if(name == "click")
			    Fightview.this.wdgmsg("click", (int)current.gobid, args[0]);
		    }
		};
	} else if((current != null) && (rel == null)) {
	    ui.destroy(curgive);
	    ui.destroy(curava);
	    curgive = null;
	    curava = null;
	} else if((current != null) && (rel != null)) {
	    curgive.state = rel.give.state;
	    curava.avagob = rel.gobid;
	}
	current = rel;
    }
    
    public void destroy() {
	setcur(null);
	super.destroy();
    }
    
    public void draw(GOut g) {
        int y = 10;
	if(curava != null)
	    y = curava.c.y + curava.sz.y + 10;
	int x = width - bg.sz().x - 10;
        for(Relation rel : lsrel) {
            if(rel == current) {
		rel.show(false);
                continue;
	    }
            g.image(bg, new Coord(x, y));
            rel.ava.c = new Coord(x + 25, ((bg.sz().y - rel.ava.sz.y) / 2) + y);
	    rel.give.c = new Coord(x + 5, 4 + y);
	    rel.show(true);
            y += bg.sz().y + ymarg;
        }
        super.draw(g);
    }
    
    public static class Notfound extends RuntimeException {
        public final long id;
        
        public Notfound(long id) {
            super("No relation for Gob ID " + id + " found");
            this.id = id;
        }
    }
    
    private Relation getrel(long gobid) {
        for(Relation rel : lsrel) {
            if(rel.gobid == gobid)
                return(rel);
        }
        throw(new Notfound(gobid));
    }
    
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if(sender instanceof Avaview) {
            for(Relation rel : lsrel) {
                if(rel.ava == sender)
                    wdgmsg("click", (int)rel.gobid, args[0]);
            }
            return;
        }
	if(sender instanceof GiveButton) {
            for(Relation rel : lsrel) {
                if(rel.give == sender)
                    wdgmsg("give", (int)rel.gobid, args[0]);
            }
            return;
	}
        super.wdgmsg(sender, msg, args);
    }
    
    private Indir<Resource> n2r(int num) {
	if(num < 0)
	    return(null);
	return(ui.sess.getres(num));
    }

    public void uimsg(String msg, Object... args) {
        if(msg == "new") {
            Relation rel = new Relation((Integer)args[0]);
	    rel.give((Integer)args[1]);
            lsrel.addFirst(rel);
            return;
        } else if(msg == "del") {
            Relation rel = getrel((Integer)args[0]);
	    rel.remove();
            lsrel.remove(rel);
	    if(rel == current)
		setcur(null);
            return;
        } else if(msg == "upd") {
            Relation rel = getrel((Integer)args[0]);
	    rel.give((Integer)args[1]);
            return;
        } else if(msg == "cur") {
            try {
                Relation rel = getrel((Integer)args[0]);
                lsrel.remove(rel);
                lsrel.addFirst(rel);
		setcur(rel);
            } catch(Notfound e) {
		setcur(null);
	    }
            return;
        } else if(msg == "atkc") {
	    atkc = System.currentTimeMillis() + (((Integer)args[0]) * 60);
	    return;
	} else if(msg == "blk") {
	    blk = n2r((Integer)args[0]);
	    return;
	} else if(msg == "atk") {
	    batk = n2r((Integer)args[0]);
	    iatk = n2r((Integer)args[1]);
	    return;
        } else if(msg == "offdef") {
	    off = (Integer)args[0];
	    def = (Integer)args[1];
	    return;
	}
        super.uimsg(msg, args);
    }
}
