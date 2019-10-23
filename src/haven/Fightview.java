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
    public static final Tex bg = Resource.loadtex("gfx/hud/bosq");
    public static final int height = 5;
    public static final int ymarg = 5;
    public static final int width = 165;
    public static final Coord avasz = new Coord(27, 27);
    public static final Coord cavac = new Coord(width - Avaview.dasz.x - 10, 10);
    public static final Coord cgivec = new Coord(cavac.x - 35, cavac.y);
    public static final Coord cpursc = new Coord(cavac.x - 75, cgivec.y + 35);
    public final LinkedList<Relation> lsrel = new LinkedList<Relation>();
    public final Bufflist buffs = add(new Bufflist()); {buffs.hide();}
    public final Map<Long, Widget> obinfo = new HashMap<>();
    public Relation current = null;
    public Indir<Resource> blk, batk, iatk;
    public double atkcs, atkct;
    public Indir<Resource> lastact = null;
    public double lastuse = 0;
    private GiveButton curgive;
    private Avaview curava;
    private Button curpurs;
    
    public class Relation {
        public final long gobid;
        public final Avaview ava;
	public final GiveButton give;
	public final Button purs;
	public final Bufflist buffs = add(new Bufflist()); {buffs.hide();}
	public final Bufflist relbuffs = add(new Bufflist()); {relbuffs.hide();}
	public int ip, oip;
	public Indir<Resource> lastact = null;
	public double lastuse = 0;
	public boolean invalid = false;
        
        public Relation(long gobid) {
            this.gobid = gobid;
            add(this.ava = new Avaview(avasz, gobid, "avacam")).canactivate = true;
	    add(this.give = new GiveButton(0, new Coord(15, 15)));
	    add(this.purs = new Button(70, "Pursue"));
        }
	
	public void give(int state) {
	    if(this == current)
		curgive.state = state;
	    this.give.state = state;
	}
	
	public void show(boolean state) {
	    ava.show(state);
	    give.show(state);
	    purs.show(state);
	}
	
	public void remove() {
	    ui.destroy(ava);
	    ui.destroy(give);
	    ui.destroy(purs);
	    ui.destroy(buffs);
	    ui.destroy(relbuffs);
	    invalid = true;
	}

	public void use(Indir<Resource> act) {
	    lastact = act;
	    lastuse = Utils.rtime();
	}
    }

    public void use(Indir<Resource> act) {
	lastact = act;
	lastuse = Utils.rtime();
    }
    
    @RName("frv")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    return(new Fightview());
	}
    }
    
    public Fightview() {
        super(new Coord(width, (bg.sz().y + ymarg) * height));
    }

    public void addchild(Widget child, Object... args) {
	if(args[0].equals("buff")) {
	    Widget p;
	    if(args[1] == null)
		p = buffs;
	    else
		p = getrel((Integer)args[1]).buffs;
	    p.addchild(child);
	} else if(args[0].equals("relbuff")) {
	    getrel((Integer)args[1]).relbuffs.addchild(child);
	} else {
	    super.addchild(child, args);
	}
    }

    /* XXX? It's a bit ugly that there's no trimming of obinfo, but
     * it's not obvious that one really ever wants it trimmed, and
     * it's really not like it uses a lot of memory. */
    public Widget obinfo(long gobid, boolean creat) {
	synchronized(obinfo) {
	    Widget ret = obinfo.get(gobid);
	    if((ret == null) && creat)
		obinfo.put(gobid, ret = new AWidget());
	    return(ret);
	}
    }

    public <T extends Widget> T obinfo(long gobid, Class<T> cl, boolean creat) {
	Widget cnt = obinfo(gobid, creat);
	if(cnt == null)
	    return(null);
	T ret = cnt.getchild(cl);
	if((ret == null) && creat) {
	    try {
		ret = Utils.construct(cl.getConstructor());
	    } catch(NoSuchMethodException e) {
		throw(new RuntimeException(e));
	    }
	    cnt.add(ret);
	}
	return(ret);
    }

    public static interface ObInfo {
	public default int prio() {return(1000);}
	public default Coord2d grav() {return(new Coord2d(0, 1));}
    }

    private void setcur(Relation rel) {
	if((current == null) && (rel != null)) {
	    add(curgive = new GiveButton(0), cgivec);
	    add(curava = new Avaview(Avaview.dasz, rel.gobid, "avacam"), cavac).canactivate = true;
	    add(curpurs = new Button(70, "Pursue"), cpursc);
	    curgive.state = rel.give.state;
	} else if((current != null) && (rel == null)) {
	    ui.destroy(curgive);
	    ui.destroy(curava);
	    ui.destroy(curpurs);
	    curgive = null;
	    curava = null;
	    curpurs = null;
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
    
    public void tick(double dt) {
	super.tick(dt);
	for(Relation rel : lsrel) {
	    Widget inf = obinfo(rel.gobid, false);
	    if(inf != null)
		inf.tick(dt);
	}
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
	    rel.purs.c = new Coord(rel.ava.c.x + rel.ava.sz.x + 5, 4 + y);
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
	if(sender == curava) {
	    wdgmsg("click", (int)current.gobid, args[0]);
	    return;
	} else if(sender == curgive) {
	    wdgmsg("give", (int)current.gobid, args[0]);
	    return;
	} else if(sender == curpurs) {
	    wdgmsg("prs", (int)current.gobid);
	    return;
	}
	for(Relation rel : lsrel) {
	    if(sender == rel.ava) {
		wdgmsg("click", (int)rel.gobid, args[0]);
		return;
	    } else if(sender == rel.give) {
		wdgmsg("give", (int)rel.gobid, args[0]);
		return;
	    } else if(sender == rel.purs) {
		wdgmsg("prs", (int)rel.gobid);
		return;
	    }
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
	    rel.ip = (Integer)args[2];
	    rel.oip = (Integer)args[3];
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
	    rel.ip = (Integer)args[2];
	    rel.oip = (Integer)args[3];
            return;
	} else if(msg == "used") {
	    use((args[0] == null)?null:ui.sess.getres((Integer)args[0]));
	    return;
	} else if(msg == "ruse") {
	    Relation rel = getrel((Integer)args[0]);
	    rel.use((args[1] == null)?null:ui.sess.getres((Integer)args[1]));
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
	    atkcs = Utils.rtime();
	    atkct = atkcs + (((Number)args[0]).doubleValue() * 0.06);
	    return;
	} else if(msg == "blk") {
	    blk = n2r((Integer)args[0]);
	    return;
	} else if(msg == "atk") {
	    batk = n2r((Integer)args[0]);
	    iatk = n2r((Integer)args[1]);
	    return;
	}
        super.uimsg(msg, args);
    }
}
