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
    
    private class MemberList extends Listbox<Member> {
	final Text unk = Text.render("???");
	
	private MemberList(Coord c, int w, int h, Widget parent) {
	    super(c, parent, w, h, 20);
	}
	
	public Member listitem(int idx) {return(memb.get(idx));}
	public int listitems() {return(memb.size());}

	public void drawitem(GOut g, Member m) {
	    if((mw instanceof MemberWidget) && (((MemberWidget)mw).id == m.id))
		drawsel(g);
	    BuddyWnd.Buddy b = getparent(GameUI.class).buddies.find(m.id);
	    Text rn = (b == null)?unk:(b.rname());
	    g.aimage(rn.tex(), new Coord(0, 10), 0, 0.5);
	}

	public void change(Member pm) {
	    if(pm == null)
		Polity.this.wdgmsg("sel", (Object)null);
	    else
		Polity.this.wdgmsg("sel", pm.id);
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
	super(c, new Coord(200, 200), parent, "Town");
	this.name = name;
	new Label(new Coord(0, 5), this, name, nmf);
	new Label(new Coord(0, 45), this, "Members:");
	ml = new MemberList(new Coord(0, 60), 200, 7, this);
	pack();
    }
    
    private Tex rauth = null;
    public void cdraw(GOut g) {
	if(acap > 0) {
	    synchronized(this) {
		g.chcolor(0, 0, 0, 255);
		g.frect(new Coord(0, 23), new Coord(200, 20));
		g.chcolor(128, 0, 0, 255);
		g.frect(new Coord(0, 24), new Coord((200 * auth) / acap, 18));
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
	    if(p.equals("m")) {
		mw = gettype(type).create(new Coord(0, 210), this, cargs);
		pack();
		return(mw);
	    }
	}
	return(super.makechild(type, pargs, cargs));
    }

    public void cdestroy(Widget w) {
	if(w == mw) {
	    mw = null;
	    pack();
	}
    }
}
