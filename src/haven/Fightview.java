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
import static haven.Utils.uint32;

public class Fightview extends Widget {
    public static final Tex bg = Resource.loadtex("gfx/hud/bosq");
    public static final int height = 5;
    public static final int ymarg = UI.scale(5);
    public static final int width = UI.scale(175);
    public static final Coord avasz = Coord.of(bg.sz().y - UI.scale(6));
    public static final Coord cavac = new Coord(width - Avaview.dasz.x - UI.scale(10), UI.scale(10));
    public static final Coord cgivec = new Coord(cavac.x - UI.scale(35), cavac.y);
    public static final Coord cpursc = new Coord(cavac.x - UI.scale(75), cgivec.y + UI.scale(35));
    public final LinkedList<Relation> lsrel = new LinkedList<Relation>();
    public final Bufflist buffs = add(new Bufflist()); {buffs.hide();}
    public final Map<Long, Widget> obinfo = new HashMap<>();
    public final Rellist lsdisp;
    public Relation current = null;
    public Indir<Resource> blk, batk, iatk;
    public double atkcs, atkct;
    public Indir<Resource> lastact = null;
    public double lastuse = 0;
    public Mainrel curdisp;
    private List<Relation> nonmain = Collections.emptyList();

    public class Relation {
        public final long gobid;
	public final Bufflist buffs = add(new Bufflist()); {buffs.hide();}
	public final Bufflist relbuffs = add(new Bufflist()); {relbuffs.hide();}
	public int gst, ip, oip;
	public Indir<Resource> lastact = null;
	public double lastuse = 0;
	public boolean invalid = false;

        public Relation(long gobid) {
            this.gobid = gobid;
        }

	public void give(int state) {
	    this.gst = state;
	}

	public void remove() {
	    buffs.destroy();
	    relbuffs.destroy();
	    invalid = true;
	}

	public void use(Indir<Resource> act) {
	    lastact = act;
	    lastuse = Utils.rtime();
	}
    }

    public class Relbox extends Widget {
	public final Relation rel;
	public final Avaview ava;
	public final GiveButton give;
	public final Button purs;

	public Relbox(Relation rel) {
	    super(bg.sz());
	    this.rel = rel;
	    Widget avaf = adda(Frame.with(ava = new Avaview(avasz, rel.gobid, "avacam"), true), UI.scale(25), sz.y / 2, 0.0, 0.5);
	    ava.canactivate = true;
	    add(give = new GiveButton(0, UI.scale(15, 15)), UI.scale(5, 4));
	    adda(purs = new Button(UI.scale(70), "Pursue"), avaf.c.x + avaf.sz.x + UI.scale(5), avaf.c.y + (avaf.sz.y / 2), 0.0, 0.5);
	}

	public void draw(GOut g) {
	    g.image(bg, Coord.z);
	    give.state = rel.gst;
	    super.draw(g);
	}

	public void wdgmsg(Widget sender, String msg, Object... args) {
	    if(sender == ava) {
		Fightview.this.wdgmsg("click", (int)rel.gobid, args[0]);
	    } else if(sender == give) {
		Fightview.this.wdgmsg("give", (int)rel.gobid, args[0]);
	    } else if(sender == purs) {
		Fightview.this.wdgmsg("prs", (int)rel.gobid);
	    } else {
		super.wdgmsg(sender, msg, args);
	    }
	}
    }

    public class Rellist extends SListBox<Relation, Relbox> {
	public Rellist(int h) {
	    super(Coord.of(bg.sz().x, ((bg.sz().y + ymarg) * h) - ymarg), bg.sz().y, ymarg);
	}

	protected List<Relation> items() {
	    return(nonmain);
	}

	protected Relbox makeitem(Relation rel, int idx, Coord sz) {
	    return(new Relbox(rel));
	}

	protected void drawslot(GOut g, Relation item, int idx, Area area) {
	}

	public boolean mousewheel(Coord c, int amount) {
	    if(!sb.vis())
		return(false);
	    return(super.mousewheel(c, amount));
	}

	protected boolean unselect(int button) {
	    return(false);
	}
    }

    public class Mainrel extends Widget {
	public final Relation rel;
	public final Avaview ava;
	public final GiveButton give;
	public final Button purs;

	public Mainrel(Relation rel) {
	    this.rel = rel;
	    Widget avaf = add(Frame.with(ava = new Avaview(Avaview.dasz, rel.gobid, "avacam"), false));
	    ava.canactivate = true;
	    adda(give = new GiveButton(0), avaf.pos("ul").subs(5, 0), 1.0, 0.0);
	    adda(purs = new Button(UI.scale(70), "Pursue"), give.pos("br").adds(0, 5), 1.0, 0.0);
	    lpack();
	}

	private void lpack() {
	    int mx = 0, my = 0;
	    for(Widget ch : children()) {
		mx = Math.min(mx, ch.c.x);
		my = Math.min(my, ch.c.y);
	    }
	    for(Widget ch : children())
		ch.c = ch.c.sub(mx, my);
	    pack();
	}

	public void draw(GOut g) {
	    give.state = rel.gst;
	    super.draw(g);
	}

	public void wdgmsg(Widget sender, String msg, Object... args) {
	    if(sender == ava) {
		Fightview.this.wdgmsg("click", (int)rel.gobid, args[0]);
	    } else if(sender == give) {
		Fightview.this.wdgmsg("give", (int)rel.gobid, args[0]);
	    } else if(sender == purs) {
		Fightview.this.wdgmsg("prs", (int)rel.gobid);
	    } else {
		super.wdgmsg(sender, msg, args);
	    }
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
	lsdisp = add(new Rellist(height));
	layout();
    }

    public void addchild(Widget child, Object... args) {
	if(args[0].equals("buff")) {
	    Widget p;
	    if(args[1] == null)
		p = buffs;
	    else
		p = getrel(Utils.uiv(args[1])).buffs;
	    p.addchild(child);
	} else if(args[0].equals("relbuff")) {
	    getrel(Utils.uiv(args[1])).relbuffs.addchild(child);
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

    private void layout() {
	Coord pos = Coord.of(sz.x - UI.scale(10), UI.scale(10));
	if(curdisp != null) {
	    curdisp.move(pos.sub(curdisp.sz.x, 0));
	    pos = curdisp.pos("br");
	}
	lsdisp.move(pos.add(-lsdisp.sz.x, UI.scale(10)));
	resize(sz.x, lsdisp.c.y + lsdisp.sz.y);
    }

    private void updrel() {
	List<Relation> nrel = new ArrayList<>(lsrel);
	nrel.remove(current);
	nonmain = nrel;
    }

    private void setcur(Relation rel) {
	if(current == rel)
	    return;
	if(curdisp != null) {
	    curdisp.reqdestroy();
	    curdisp = null;
	}
	if(rel != null) {
	    add(curdisp = new Mainrel(rel));
	}
	current = rel;
	layout();
	updrel();
    }
    
    public void tick(double dt) {
	super.tick(dt);
	for(Relation rel : lsrel) {
	    Widget inf = obinfo(rel.gobid, false);
	    if(inf != null)
		inf.tick(dt);
	}
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

    public void uimsg(String msg, Object... args) {
        if(msg == "new") {
            Relation rel = new Relation(Utils.uiv(args[0]));
	    rel.give(Utils.iv(args[1]));
	    rel.ip = Utils.iv(args[2]);
	    rel.oip = Utils.iv(args[3]);
            lsrel.addFirst(rel);
	    updrel();
            return;
        } else if(msg == "del") {
            Relation rel = getrel(Utils.uiv(args[0]));
	    rel.remove();
            lsrel.remove(rel);
	    if(rel == current)
		setcur(null);
	    updrel();
            return;
        } else if(msg == "upd") {
            Relation rel = getrel(Utils.uiv(args[0]));
	    rel.give(Utils.iv(args[1]));
	    rel.ip = Utils.iv(args[2]);
	    rel.oip = Utils.iv(args[3]);
            return;
	} else if(msg == "used") {
	    use((args[0] == null) ? null : ui.sess.getresv(args[0]));
	    return;
	} else if(msg == "ruse") {
	    Relation rel = getrel(Utils.uiv(args[0]));
	    rel.use((args[1] == null) ? null : ui.sess.getresv(args[1]));
	    return;
        } else if(msg == "cur") {
            try {
                Relation rel = getrel(Utils.uiv(args[0]));
                lsrel.remove(rel);
                lsrel.addFirst(rel);
		setcur(rel);
            } catch(Notfound e) {
		setcur(null);
	    }
            return;
	} else if(msg == "atkc") {
	    atkcs = Utils.rtime();
	    atkct = atkcs + (Utils.dv(args[0]) * 0.06);
	    return;
	} else if(msg == "blk") {
	    blk = ui.sess.getresv(args[0]);
	    return;
	} else if(msg == "atk") {
	    batk = ui.sess.getresv(args[0]);
	    iatk = ui.sess.getresv(args[1]);
	    return;
	}
        super.uimsg(msg, args);
    }
}
