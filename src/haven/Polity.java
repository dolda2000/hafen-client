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
import java.util.*;

public class Polity extends Window {
    public final String name;
    public int auth, acap, adrain;
    public boolean offline;
    private final List<Member> memb = new ArrayList<Member>();
    private final Map<Integer, Member> idmap = new HashMap<Integer, Member>();
    private MemberList ml;
    private Widget mw;
    
    static {
	Widget.addtype("pol", new WidgetFactory() {
		public Widget create(Coord c, Widget parent, Object[] args) {
		    return(new Polity(c, parent, (String)args[0]));
		}
	    });
    }
    
    public class Member {
	public final int id;
	private Text rname = null;
	
	private Member(int id) {
	    this.id = id;
	}
    }
    
    private class MemberList extends Widget {
	final Text unk = Text.render("???");
	Scrollbar sb;
	int h;
	
	private MemberList(Coord c, Coord sz, Widget parent) {
	    super(c, sz, parent);
	    h = sz.y / 20;
	    sb = new Scrollbar(new Coord(sz.x, 0), sz.y, this, 0, 4);
	}
	
	public void draw(GOut g) {
	    g.chcolor(Color.BLACK);
	    g.frect(Coord.z, sz);
	    g.chcolor();
	    Member[] vis = new Member[h];
	    synchronized(this) {
		for(int i = 0; i < h; i++) {
		    if(i + sb.val >= memb.size())
			break;
		    vis[i] = memb.get(i + sb.val);
		}
	    }
	    int selid = -1;
	    if(mw instanceof MemberWidget)
		selid = ((MemberWidget)mw).id;
	    for(int i = 0; i < h; i++) {
		Member pm = vis[i];
		if(pm == null)
		    break;
		if(pm.id == selid) {
		    g.chcolor(255, 255, 0, 128);
		    g.frect(new Coord(0, i * 20), new Coord(sz.x, 20));
		    g.chcolor();
		}
		BuddyWnd.Buddy b = getparent(GameUI.class).buddies.find(pm.id);
		Text rn = (b == null)?unk:(b.rname());
		g.aimage(rn.tex(), new Coord(0, i * 20 + 10), 0, 0.5);
	    }
	}
	
	public void tick(double dt) {
	    synchronized(this) {
		sb.max = memb.size() - h;
	    }
	}
	
	public boolean mousedown(Coord c, int button) {
	    int sel = (c.y / 20) + sb.val;
	    Member pm;
	    synchronized(this) {
		pm = (sel >= memb.size())?null:memb.get(sel);
	    }
	    if(pm == null)
		Polity.this.wdgmsg("sel", -1);
	    else
		Polity.this.wdgmsg("sel", pm.id);
	    return(true);
	}
    }
    
    public static abstract class MemberWidget extends Widget {
	public final int id;
	
	public MemberWidget(Coord c, Coord sz, Widget parent, int id) {
	    super(c, sz, parent);
	    this.id = id;
	}
    }

    private static final Text.Foundry nmf;
    static {
	nmf = new Text.Foundry("Serif", 14);
	nmf.aa = true;
    }

    public Polity(Coord c, Widget parent, String name) {
	super(c, new Coord(200, 390), parent, "Town");
	this.name = name;
	new Label(new Coord(10, 5), this, name, nmf);
	new Label(new Coord(10, 45), this, "Members:");
	ml = new MemberList(new Coord(10, 60), new Coord(180, 140), this);
    }
    
    private Tex rauth = null;
    public void cdraw(GOut g) {
	if(acap > 0) {
	    synchronized(this) {
		g.chcolor(0, 0, 0, 255);
		g.frect(new Coord(10, 23), new Coord(180, 20));
		g.chcolor(128, 0, 0, 255);
		g.frect(new Coord(10, 24), new Coord((180 * auth) / acap, 18));
		g.chcolor();
		if(rauth == null) {
		    Color col = offline?Color.RED:Color.WHITE;
		    rauth = new TexI(Utils.outline2(Text.render(String.format("%s/%s", auth, acap), col).img, Utils.contrast(col)));
		}
		g.aimage(rauth, new Coord(100, 33), 0.5, 0.5);
	    }
	}
    }
    
    public void uimsg(String msg, Object... args) {
	if(msg == "auth") {
	    synchronized(this) {
		auth = (Integer)args[0];
		acap = (Integer)args[1];
		adrain = (Integer)args[2];
		offline = ((Integer)args[3]) != 0;
		rauth = null;
	    }
	} else if(msg == "add") {
	    int id = (Integer)args[0];
	    Member pm = new Member(id);
	    synchronized(this) {
		memb.add(pm);
		idmap.put(id, pm);
	    }
	} else if(msg == "rm") {
	    int id = (Integer)args[0];
	    synchronized(this) {
		Member pm = idmap.get(id);
		memb.remove(pm);
		idmap.remove(id);
	    }
	} else {
	    super.uimsg(msg, args);
	}
    }
    
    public Widget makechild(String type, Object[] pargs, Object[] cargs) {
	if(pargs[0] instanceof String) {
	    String p = (String)pargs[0];
	    if(p.equals("m"))
		return(mw = gettype(type).create(new Coord(10, 210), this, cargs));
	}
	return(super.makechild(type, pargs, cargs));
    }

    public void cdestroy(Widget w) {
	if(w == mw)
	    mw = null;
    }
}
