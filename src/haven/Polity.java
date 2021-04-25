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

import java.awt.Color;
import java.awt.Font;
import java.util.*;
import static haven.BuddyWnd.width;

public class Polity extends Widget {
    public final String cap, name;
    public int auth, acap, adrain, aseq;
    public boolean offline;
    public final List<Member> memb = new ArrayList<Member>();
    public final Map<Integer, Member> idmap = new HashMap<Integer, Member>();
    protected Widget mw;

    public static final Text unk = Text.render("???");
    public static final Text self = Text.render("You", new Color(192, 192, 255));
    public class Member {
	public final Integer id;

	public Member(Integer id) {
	    this.id = id;
	}

	public void draw(GOut g) {
	    Text rn;
	    if(id == null) {
		rn = self;
	    } else {
		BuddyWnd.Buddy b = getparent(GameUI.class).buddies.find(id);
		rn = (b == null) ? unk : (b.rname());
	    }
	    g.aimage(rn.tex(), UI.scale(0, 10), 0, 0.5);
	}
    }

    public class MemberList extends Searchbox<Member> {

	public MemberList(int w, int h) {
	    super(w, h, UI.scale(20));
	}

	public Member listitem(int idx) {return(memb.get(idx));}
	public int listitems() {return(memb.size());}
	public String itemname(int idx) {
	    Member m = memb.get(idx);
	    if(m.id == null)
		return("You");
	    BuddyWnd.Buddy b = getparent(GameUI.class).buddies.find(m.id);
	    return((b == null) ? "???" : b.name);
	}
	public boolean searchmatch(int idx, String txt) {return(itemname(idx).toLowerCase().indexOf(txt.toLowerCase()) >= 0);}

	protected void drawbg(GOut g) {
	    g.chcolor(0, 0, 0, 128);
	    g.frect(Coord.z, sz);
	    g.chcolor();
	}

	public void drawitem(GOut g, Member m, int idx) {
	    if(soughtitem(idx)) {
		g.chcolor(255, 255, 0, 32);
		g.frect(Coord.z, g.sz());
		g.chcolor();
	    }
	    if((mw instanceof MemberWidget) && Utils.eq(((MemberWidget)mw).id, m.id))
		drawsel(g);
	    m.draw(g);
	}

	public void change(Member pm) {
	    if(pm == null)
		Polity.this.wdgmsg("sel");
	    else
		Polity.this.wdgmsg("sel", pm.id);
	}
    }

    public static abstract class MemberWidget extends Widget {
	public final Integer id;

	public MemberWidget(Coord sz, Integer id) {
	    super(sz);
	    this.id = id;
	}
    }

    public static final Text.Foundry nmf = new Text.Foundry(Text.serif.deriveFont(Font.BOLD, UI.scale(14))).aa(true);
    public static final Text.Foundry membf = new Text.Foundry(Text.serif.deriveFont(Font.BOLD, UI.scale(12))).aa(true);

    public Polity(String cap, String name) {
	super(new Coord(width, UI.scale(200)));
	this.cap = cap;
	this.name = name;
    }

    public class AuthMeter extends Widget {
	public AuthMeter(Coord sz) {
	    super(sz);
	}

	private int aseq = -1;
	private Tex rauth = null;
	public void draw(GOut g) {
	    synchronized(Polity.this) {
		g.chcolor(0, 0, 0, 255);
		g.frect(new Coord(0, 0), new Coord(sz.x, sz.y));
		g.chcolor(128, 0, 0, 255);
		int mw = (int)((sz.x - 2) * (long)auth) / ((acap == 0) ? 1 : acap);
		g.frect(new Coord(1, 1), new Coord(mw, sz.y - UI.scale(2)));
		g.chcolor();
		if((rauth != null) && (aseq != Polity.this.aseq)) {
		    rauth.dispose();
		    rauth = null;
		    aseq = Polity.this.aseq;
		}
		if(rauth == null) {
		    Color col = offline ? Color.RED : Color.WHITE;
		    rauth = new TexI(Utils.outline2(Text.render(String.format("%s/%s", auth, acap), col).img, Utils.contrast(col)));
		}
		g.aimage(rauth, sz.div(2), 0.5, 0.5);
	    }
	}

	public Object tooltip(Coord c, Widget prev) {
	    if(adrain > 0)
		return(String.format("Drain: %,d/day", adrain));
	    return(null);
	}
    }

    protected Member parsememb(Object[] args) {
	Integer id = (Integer)args[0];
	return(new Member(id));
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "auth") {
	    synchronized(this) {
		auth = (Integer)args[0];
		acap = (Integer)args[1];
		adrain = (Integer)args[2];
		offline = ((Integer)args[3]) != 0;
		aseq++;
	    }
	} else if(msg == "add") {
	    Member pm = parsememb(args);
	    synchronized(this) {
		memb.add(pm);
		idmap.put(pm.id, pm);
	    }
	} else if(msg == "rm") {
	    Integer id = (Integer)args[0];
	    synchronized(this) {
		Member pm = idmap.get(id);
		memb.remove(pm);
		idmap.remove(id);
	    }
	} else {
	    super.uimsg(msg, args);
	}
    }

    public void addchild(Widget child, Object... args) {
	if(args[0] instanceof String) {
	    String p = (String)args[0];
	    if(p.equals("m")) {
		mw = child;
		add(child, 0, UI.scale(210));
		pack();
		return;
	    }
	}
	super.addchild(child, args);
    }

    public void cdestroy(Widget w) {
	if(w == mw) {
	    mw = null;
	    pack();
	}
    }
}
