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
import java.text.Collator;

public class BuddyWnd extends Window implements Iterable<BuddyWnd.Buddy> {
    private List<Buddy> buddies = new ArrayList<Buddy>();
    private Map<Integer, Buddy> idmap = new HashMap<Integer, Buddy>();
    private BuddyList bl;
    private Button sbalpha;
    private Button sbgroup;
    private Button sbstatus;
    private TextEntry charpass, opass;
    private Buddy editing = null;
    private TextEntry nicksel;
    private GroupSelector grpsel;
    private FlowerMenu menu;
    public int serial = 0;
    public static final Tex online = Resource.loadtex("gfx/hud/online");
    public static final Tex offline = Resource.loadtex("gfx/hud/offline");
    public static final Color[] gc = new Color[] {
	new Color(255, 255, 255),
	new Color(0, 255, 0),
	new Color(255, 0, 0),
	new Color(0, 0, 255),
	new Color(0, 255, 255),
	new Color(255, 255, 0),
	new Color(255, 0, 255),
	new Color(255, 0, 128),
    };
    private Comparator<Buddy> bcmp;
    private Comparator<Buddy> alphacmp = new Comparator<Buddy>() {
	private Collator c = Collator.getInstance();
	public int compare(Buddy a, Buddy b) {
	    return(c.compare(a.name, b.name));
	}
    };
    private Comparator<Buddy> groupcmp = new Comparator<Buddy>() {
	public int compare(Buddy a, Buddy b) {
	    if(a.group == b.group) return(alphacmp.compare(a, b));
	    else                   return(a.group - b.group);
	}
    };
    private Comparator<Buddy> statuscmp = new Comparator<Buddy>() {
	public int compare(Buddy a, Buddy b) {
	    if(a.online == b.online) return(alphacmp.compare(a, b));
	    else                     return(b.online - a.online);
	}
    };
    
    static {
	Widget.addtype("buddy", new WidgetFactory() {
		public Widget create(Coord c, Widget parent, Object[] args) {
		    return(new BuddyWnd(c, parent));
		}
	    });
    }
    
    public class Buddy {
	int id;
	String name;
	Text rname = null;
	int online;
	int group;
	boolean seen;
	
	public Buddy(int id, String name, int online, int group, boolean seen) {
	    this.id = id;
	    this.name = name;
	    this.online = online;
	    this.group = group;
	    this.seen = seen;
	}
	
	public void forget() {
	    wdgmsg("rm", id);
	}
	
	public void endkin() {
	    wdgmsg("rm", id);
	}
	
	public void chat() {
	    wdgmsg("chat", id);
	}
	
	public void invite() {
	    wdgmsg("inv", id);
	}
	
	public void chname(String name) {
	    wdgmsg("nick", id, name);
	}
	
	public void chgrp(int grp) {
	    wdgmsg("grp", id, grp);
	}
	
	public Text rname() {
	    if((rname == null) || !rname.text.equals(name))
		rname = Text.render(name);
	    return(rname);
	}
    }
    
    public Iterator<Buddy> iterator() {
	synchronized(buddies) {
	    return(new ArrayList<Buddy>(buddies).iterator());
	}
    }
    
    public Buddy find(int id) {
	synchronized(buddies) {
	    return(idmap.get(id));
	}
    }

    public static class GroupSelector extends Widget {
	public int group;
	
	public GroupSelector(Coord c, Widget parent, int group) {
	    super(c, new Coord((gc.length * 20) + 20, 20), parent);
	    this.group = group;
	}
	
	public void draw(GOut g) {
	    for(int i = 0; i < gc.length; i++) {
		if(i == group) {
		    g.chcolor();
		    g.frect(new Coord(i * 20, 0), new Coord(19, 19));
		}
		g.chcolor(gc[i]);
		g.frect(new Coord(2 + (i * 20), 2), new Coord(15, 15));
	    }
	    g.chcolor();
	}
	
	public boolean mousedown(Coord c, int button) {
	    if((c.y >= 2) && (c.y < 17)) {
		int g = (c.x - 2) / 20;
		if((g >= 0) && (g < gc.length) && (c.x >= 2 + (g * 20)) && (c.x < 17 + (g * 20))) {
		    changed(g);
		    return(true);
		}
	    }
	    return(super.mousedown(c, button));
	}
	
	protected void changed(int group) {
	    this.group = group;
	}
    }

    private class BuddyList extends Widget {
	Scrollbar sb;
	int h;
	Buddy sel;
	
	public BuddyList(Coord c, Coord sz, Widget parent) {
	    super(c, sz, parent);
	    h = sz.y / 20;
	    sel = null;
	    sb = new Scrollbar(new Coord(sz.x, 0), sz.y, this, 0, 4);
	}
	
	public void draw(GOut g) {
	    g.chcolor(Color.BLACK);
	    g.frect(Coord.z, sz);
	    g.chcolor();
	    synchronized(buddies) {
		if(buddies.size() == 0) {
		    g.atext("You are alone in the world", sz.div(2), 0.5, 0.5);
		} else {
		    for(int i = 0; i < h; i++) {
			if(i + sb.val >= buddies.size())
			    continue;
			Buddy b = buddies.get(i + sb.val);
			if(b == sel) {
			    g.chcolor(255, 255, 0, 128);
			    g.frect(new Coord(0, i * 20), new Coord(sz.x, 20));
			    g.chcolor();
			}
			if(b.online == 1)
			    g.image(online, new Coord(0, i * 20));
			else if(b.online == 0)
			    g.image(offline, new Coord(0, i * 20));
			g.chcolor(gc[b.group]);
			g.aimage(b.rname().tex(), new Coord(25, i * 20 + 10), 0, 0.5);
			g.chcolor();
		    }
		}
	    }
	    super.draw(g);
	}
	
	public void repop() {
	    sb.val = 0;
	    synchronized(buddies) {
		sb.max = buddies.size() - h;
	    }
	}

	public boolean mousewheel(Coord c, int amount) {
	    sb.ch(amount);
	    return(true);
	}

	public void select(Buddy b) {
	    this.sel = b;
	    changed(this.sel);
	}

	public void opts(final Buddy b, Coord c) {
	    List<String> opts = new ArrayList<String>();
	    if(b.online >= 0) {
		opts.add("Chat");
		if(b.online == 1)
		    opts.add("Invite");
		opts.add("End kinship");
	    } else {
		opts.add("Forget");
	    }
	    if(menu == null) {
		menu = new FlowerMenu(c, ui.root, opts.toArray(new String[0])) {
			public void destroy() {
			    menu = null;
			}
			
			public void choose(Petal opt) {
			    if(opt != null) {
				if(opt.name.equals("End kinship")) {
				    b.endkin();
				} else if(opt.name.equals("Chat")) {
				    b.chat();
				} else if(opt.name.equals("Invite")) {
				    b.invite();
				} else if(opt.name.equals("Forget")) {
				    b.forget();
				}
				uimsg("act", opt.num);
			    } else {
				uimsg("cancel");
			    }
			}
		    };
	    }
	}

	public boolean mousedown(Coord c, int button) {
	    if(super.mousedown(c, button))
		return(true);
	    synchronized(buddies) {
		int sel = (c.y / 20) + sb.val;
		Buddy b = (sel >= buddies.size())?null:buddies.get(sel);
		if(button == 1) {
		    select(b);
		    return(true);
		} else if(button == 3) {
		    if(b != null)
			opts(b, c.add(rootpos()));
		    return(true);
		}
	    }
	    return(false);
	}
	
	public void changed(Buddy b) {
	    if(b == null) {
		if(editing != null) {
		    editing = null;
		    ui.destroy(nicksel);
		    ui.destroy(grpsel);
		}
	    } else {
		if(editing == null) {
		    nicksel = new TextEntry(new Coord(10, 165), new Coord(180, 20), BuddyWnd.this, "") {
			    public void activate(String text) {
				editing.chname(text);
			    }
			};
		    grpsel = new GroupSelector(new Coord(10, 190), BuddyWnd.this, 0) {
			    public void changed(int group) {
				editing.chgrp(group);
			    }
			};
		    BuddyWnd.this.setfocus(nicksel);
		}
		editing = b;
		nicksel.settext(b.name);
		nicksel.buf.point = nicksel.buf.line.length();
		grpsel.group = b.group;
	    }
	}
    }

    public BuddyWnd(Coord c, Widget parent) {
	super(c, new Coord(200, 370), parent, "Kin");
	bl = new BuddyList(new Coord(10, 5), new Coord(180, 140), this);
	new Label(new Coord(5, 215), this, "Sort by:");
	sbstatus = new Button(new Coord(10,  230), 50, this, "Status")      { public void click() { setcmp(statuscmp); } };
	sbgroup  = new Button(new Coord(75,  230), 50, this, "Group")       { public void click() { setcmp(groupcmp); } };
	sbalpha  = new Button(new Coord(140, 230), 50, this, "Name")        { public void click() { setcmp(alphacmp); } };
	String sort = Utils.getpref("buddysort", "");
	if(sort.equals("")) {
	    bcmp = statuscmp;
	} else {
	    if(sort.equals("alpha"))  bcmp = alphacmp;
	    if(sort.equals("group"))  bcmp = groupcmp;
	    if(sort.equals("status")) bcmp = statuscmp;
	}
	new Label(new Coord(0, 250), this, "My hearth secret:");
	charpass = new TextEntry(new Coord(0, 265), new Coord(190, 20), this, "") {
		public void activate(String text) {
		    BuddyWnd.this.wdgmsg("pwd", text);
		}
	    };
	new Button(new Coord(0  , 290), 50, this, "Set")    { public void click() {sendpwd(charpass.text);} };
	new Button(new Coord(60 , 290), 50, this, "Clear")  { public void click() {sendpwd("");} };
	new Button(new Coord(120, 290), 50, this, "Random") { public void click() {sendpwd(randpwd());} };
	new Label(new Coord(0, 310), this, "Make kin by hearth secret:");
	opass = new TextEntry(new Coord(0, 325), new Coord(190, 20), this, "") {
		public void activate(String text) {
		    BuddyWnd.this.wdgmsg("bypwd", text);
		    settext("");
		}
	    };
	new Button(new Coord(0, 350), 50, this, "Add kin") {
	    public void click() {
		BuddyWnd.this.wdgmsg("bypwd", opass.text);
		opass.settext("");
	    }
	};
	bl.repop();
    }
    
    private String randpwd() {
	String charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	StringBuilder buf = new StringBuilder();
	for(int i = 0; i < 8; i++)
	    buf.append(charset.charAt((int)(Math.random() * charset.length())));
	return(buf.toString());
    }
    
    private void sendpwd(String pass) {
	wdgmsg("pwd", pass);
	charpass.settext(pass);
    }

    private void setcmp(Comparator<Buddy> cmp) {
	bcmp = cmp;
	String val = "";
	if(cmp == alphacmp)  val = "alpha";
	if(cmp == groupcmp)  val = "group";
	if(cmp == statuscmp) val = "status";
	Utils.setpref("buddysort", val);
	synchronized(buddies) {
	    Collections.sort(buddies, bcmp);
	}
    }
    
    public void uimsg(String msg, Object... args) {
	if(msg == "add") {
	    int id = (Integer)args[0];
	    String name = ((String)args[1]).intern();
	    int online = (Integer)args[2];
	    int group = (Integer)args[3];
	    boolean seen = ((Integer)args[4]) != 0;
	    Buddy b = new Buddy(id, name, online, group, seen);
	    synchronized(buddies) {
		buddies.add(b);
		idmap.put(b.id, b);
		Collections.sort(buddies, bcmp);
	    }
	    serial++;
	    bl.repop();
	} else if(msg == "rm") {
	    int id = (Integer)args[0];
	    Buddy b;
	    synchronized(buddies) {
		b = idmap.get(id);
		if(b != null) {
		    buddies.remove(b);
		    idmap.remove(id);
		    bl.repop();
		}
	    }
	    if(b == editing) {
		editing = null;
		ui.destroy(nicksel);
		ui.destroy(grpsel);
	    }
	    serial++;
	} else if(msg == "chst") {
	    int id = (Integer)args[0];
	    int online = (Integer)args[1];
	    find(id).online = online;
	} else if(msg == "upd") {
	    int id = (Integer)args[0];
	    String name = (String)args[1];
	    int online = (Integer)args[2];
	    int grp = (Integer)args[3];
	    boolean seen = ((Integer)args[4]) != 0;
	    Buddy b = find(id);
	    synchronized(b) {
		b.name = name;
		b.online = online;
		b.group = grp;
		b.seen = seen;
	    }
	    if(b == editing) {
		nicksel.settext(b.name);
		grpsel.group = b.group;
	    }
	    serial++;
	} else if(msg == "sel") {
	    int id = (Integer)args[0];
	    show();
	    raise();
	    bl.select(find(id));
	} else if(msg == "pwd") {
	    charpass.settext((String)args[0]);
	} else {
	    super.uimsg(msg, args);
	}
    }
    
    public void hide() {
	if(menu != null) {
	    ui.destroy(menu);
	    menu = null;
	}
	super.hide();
    }
    
    public void destroy() {
	if(menu != null) {
	    ui.destroy(menu);
	    menu = null;
	}
	super.destroy();
    }
}
