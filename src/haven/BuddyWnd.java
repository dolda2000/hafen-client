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

public class BuddyWnd extends Widget implements Iterable<BuddyWnd.Buddy> {
    private List<Buddy> buddies = new ArrayList<Buddy>();
    private Map<Integer, Buddy> idmap = new HashMap<Integer, Buddy>();
    private BuddyList bl;
    private TextEntry pname, charpass, opass;
    private FlowerMenu menu;
    private BuddyInfo info = null;
    private Widget infof;
    public int serial = 0;
    public static final int width = UI.scale(263);
    public static final int margin1 = UI.scale(5);
    public static final int margin2 = 2 * margin1;
    public static final int margin3 = 2 * margin2;
    public static final int offset = UI.scale(35);
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
    
    @RName("buddy")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    return(new BuddyWnd());
	}
    }
    
    public class Buddy {
	public int id;
	public String name;
	Text rname = null;
	public int online;
	public int group;
	public boolean seen;

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

	public void describe() {
	    wdgmsg("desc", id);
	}

	public void chname(String name) {
	    wdgmsg("nick", id, name);
	}

	public void chgrp(int grp) {
	    wdgmsg("grp", id, grp);
	}

	private void chstatus(int status) {
	    online = status;
	    GameUI gui = getparent(GameUI.class);
	    if(gui != null) {
		if(status == 1)
		    ui.msg(String.format("%s is now online.", name));
	    }
	}

	public Text rname() {
	    if((rname == null) || !rname.text.equals(name))
		rname = Text.render(name);
	    return(rname);
	}

	public Map<String, Runnable> opts() {
	    Map<String, Runnable> opts = new LinkedHashMap<>();
	    if(online >= 0) {
		opts.put("Chat", this::chat);
		if(online == 1)
		    opts.put("Invite", this::invite);
		opts.put("End kinship", this::endkin);
	    } else {
		opts.put("Forget", this::forget);
	    }
	    if(seen)
		opts.put("Describe", this::describe);
	    return(opts);
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

    public static class GroupRect extends Widget {
	final private static Coord offset = UI.scale(new Coord(2, 2));
	final private static Coord selsz = UI.scale(new Coord(19, 19));
	final private static Coord colsz = selsz.sub(offset.mul(2));
	final private GroupSelector selector;
	final private int group;
	private boolean selected;

	public GroupRect(GroupSelector selector, int group, boolean selected) {
	    super(new Coord(margin3, margin3));
	    this.selector = selector;
	    this.group = group;
	    this.selected = selected;
	}

	public void draw(GOut g) {
	    if (selected) {
		g.chcolor(Color.LIGHT_GRAY);
		g.frect(Coord.z, selsz);
	    }
	    g.chcolor(gc[group]);
	    g.frect(offset, colsz);
	    g.chcolor();
	}

	public boolean mousedown(Coord c, int button) {
	    selector.select(group);
	    return (true);
	}

	public void select() {
	    selected = true;
	}

	public void unselect() {
	    selected = false;
	}
    }

    public static class GroupSelector extends Widget {
	public int group;
	public GroupRect[] groups = new GroupRect[gc.length];

	public GroupSelector(int group) {
	    super(new Coord(gc.length * margin3, margin3));
	    this.group = group;
	    for (int i = 0; i < gc.length; ++i) {
		groups[i] = new GroupRect(this, i, group == i);
		add(groups[i], new Coord(i * margin3, 0));
	    }
	}

	protected void changed(int group) {
	}

	public void update(int group) {
	    if(group == this.group)
		return;
	    if(this.group >= 0)
		groups[this.group].unselect();
	    this.group = group;
	    if(group >= 0)
		groups[group].select();
	}

	public void select(int group) {
	    update(group);
	    changed(group);
	}
    }

    @RName("grp")
    public static class $grp implements Factory {
	public Widget create(UI ui, Object[] args) {
	    return(new GroupSelector(Utils.iv(args[0])) {
		    public void changed(int group) {
			wdgmsg("ch", group);
		    }
		});
	}
    }

    private class BuddyInfo extends Widget {
	private final Buddy buddy;
	private final Avaview ava;
	private final TextEntry nick;
	private final GroupSelector grp;
	private long atime, utime;
	private Label atimel = null;
	private Button[] opts = {};

	private BuddyInfo(Coord sz, Buddy buddy) {
	    super(sz);
	    this.buddy = buddy;
	    this.ava = adda(new Avaview(Avaview.dasz, -1, "avacam"), sz.x / 2, margin2, 0.5, 0);
	    Frame.around(this, this.ava);
	    this.nick = add(new TextEntry(sz.x - margin3, buddy.name) {
		    {dshow = true;}
		    public void activate(String text) {
			buddy.chname(text);
			commit();
		    }
		}, margin2, ava.c.y + ava.sz.y + margin2);
	    this.grp = add(new GroupSelector(buddy.group) {
		    public void changed(int group) {
			buddy.chgrp(group);
		    }
		}, margin2, nick.c.y + nick.sz.y + margin2);
	    setopts();
	}

	public void draw(GOut g) {
	    g.chcolor(0, 0, 0, 128);
	    g.frect(Coord.z, sz);
	    g.chcolor();
	    super.draw(g);
	}

	public void tick(double dt) {
	    if((utime != 0) && (Utils.ntime() >= utime))
		setatime();
	}

	private void setatime() {
	    String text;
	    if(buddy.online == 1) {
		this.utime = 0;
		text = "Last seen: Now";
	    } else {
		int au, atime = (int)((long)Utils.ntime() - this.atime);
		String unit;
		if(atime >= (604800 * 2)) {
		    au = 604800;
		    unit = "week";
		} else if(atime >= 86400) {
		    au = 86400;
		    unit = "day";
		} else if(atime >= 3600) {
		    au = 3600;
		    unit = "hour";
		} else if(atime >= 60) {
		    au = 60;
		    unit = "minute";
		} else {
		    au = 1;
		    unit = "second";
		}
		int am = atime / au;
		this.utime = this.atime + ((am + 1) * au);
		text = "Last seen: " + am + " " + unit + ((am > 1)?"s":"") + " ago";
	    }
	    if(atimel != null)
		ui.destroy(atimel);
	    atimel = add(new Label(text), margin2, grp.c.y + grp.sz.y + margin2);
	}

	private void setopts() {
	    for(Button opt : this.opts)
		ui.destroy(opt);
	    Map<String, Runnable> bopts = buddy.opts();
	    List<Button> opts = new ArrayList<>(bopts.size());
	    int y = grp.c.y + grp.sz.y + offset;
	    for(Map.Entry<String, Runnable> opt : bopts.entrySet()) {
		Button btn = add(new Button(sz.x - margin3, opt.getKey(), false, opt.getValue()), margin2, y);
		y = btn.c.y + btn.sz.y + margin1;
		opts.add(btn);
	    }
	    this.opts = opts.toArray(new Button[0]);
	}

	public void uimsg(String msg, Object... args) {
	    if(msg == "i-ava") {
		Composited.Desc desc = Composited.Desc.decode(ui.sess, (Object[])args[0]);
		Resource.Resolver map = new Resource.Resolver.ResourceMap(ui.sess, (Object[])args[1]);
		ava.pop(desc, map);
	    } else if(msg == "i-atime") {
		atime = (long)Utils.ntime() - ((Number)args[0]).longValue();
		setatime();
	    } else {
		super.uimsg(msg, args);
	    }
	}

	public void update() {
	    nick.settext(buddy.name);
	    nick.commit();
	    grp.group = buddy.group;
	    setatime();
	    setopts();
	}
    }

    private class BuddyList extends SSearchBox<Buddy, Widget> {
	public BuddyList(Coord sz) {
	    super(sz, margin3);
	}

	public List<Buddy> allitems() {return(buddies);}
	public boolean searchmatch(Buddy b, String txt) {return(b.name.toLowerCase().indexOf(txt.toLowerCase()) >= 0);}

	public Widget makeitem(Buddy b, int idx, Coord sz) {
	    return(new ItemWidget<Buddy>(this, sz, b) {
		    public void draw(GOut g) {
			if(item.online == 1)
			    g.aimage(online, Coord.of(sz.y / 2), 0.5, 0.5);
			else if(item.online == 0)
			    g.aimage(offline, Coord.of(sz.y / 2), 0.5, 0.5);
			g.chcolor(gc[b.group]);
			g.aimage(b.rname().tex(), Coord.of(sz.y + margin1, sz.y / 2), 0.0, 0.5);
			g.chcolor();
		    }

		    public boolean mousedown(Coord c, int button) {
			if(button == 1)
			    change(item);
			else if(button == 3)
			    opts(b, ui.mc);
			return(true);
		    }
		});
	}

	protected void drawbg(GOut g) {
	    g.chcolor(0, 0, 0, 128);
	    g.frect(Coord.z, sz);
	    g.chcolor();
	}

	protected void drawbg(GOut g, Buddy item, int idx, Area area) {
	}

	public void draw(GOut g) {
	    super.draw(g);
	    if(buddies.size() == 0)
		g.atext("You are alone in the world", sz.div(2), 0.5, 0.5);
	}
	
	public void change(Buddy b) {
	    if(b == null) {
		BuddyWnd.this.wdgmsg("ch", (Object)null);
	    } else {
		BuddyWnd.this.wdgmsg("ch", b.id);
	    }
	}

	public void opts(final Buddy b, Coord c) {
	    if(menu == null) {
		Map<String, Runnable> bopts = b.opts();
		menu = new FlowerMenu(bopts.keySet().toArray(new String[0])) {
			public void destroy() {
			    menu = null;
			    super.destroy();
			}
			
			public void choose(Petal opt) {
			    if(opt != null) {
				Runnable act = bopts.get(opt.name);
				if(act != null)
				    act.run();
				uimsg("act", opt.num);
			    } else {
				uimsg("cancel");
			    }
			}
		    };
		ui.root.add(menu, c);
	    }
	}
    }

    public BuddyWnd() {
	super(new Coord(width, 0));
	setfocustab(true);
	Widget prev;
        prev = add(new Img(CharWnd.catf.render("Kin").tex()));

	bl = add(new BuddyList(Coord.of(sz.x - Window.wbox.bisz().x, UI.scale(140))), prev.pos("bl").add(Window.wbox.btloff()));
	prev = Frame.around(this, Collections.singletonList(bl));

	prev = add(new Label("Sort by:"), prev.pos("bl").adds(0, 5));
	int sbw = ((sz.x + margin1) / 3) - margin1;
	addhl(prev.pos("bl").adds(0, 2), sz.x,
	      prev = new Button(sbw, "Status").action(() -> { setcmp(statuscmp); }),
	             new Button(sbw, "Group" ).action(() -> { setcmp(groupcmp); }),
	             new Button(sbw, "Name"  ).action(() -> { setcmp(alphacmp); })
	      );
	String sort = Utils.getpref("buddysort", "");
	if(sort.equals("")) {
	    bcmp = statuscmp;
	} else {
	    if(sort.equals("alpha"))  bcmp = alphacmp;
	    if(sort.equals("group"))  bcmp = groupcmp;
	    if(sort.equals("status")) bcmp = statuscmp;
	}

	prev = add(new Label("Presentation name:"), prev.pos("bl").adds(0, 10));
	pname = add(new TextEntry(sz.x, "") {
		{dshow = true;}
		public void activate(String text) {
		    setpname(text);
		}
	    }, prev.pos("bl").adds(0, 2));
	prev = add(new Button(sbw, "Set").action(() -> {
		    setpname(pname.text());
	}), pname.pos("bl").adds(0, 5));

	prev = add(new Label("My hearth secret:"), prev.pos("bl").adds(0, 10));
	charpass = add(new TextEntry(sz.x, "") {
		{dshow = true;}
		public void activate(String text) {
		    setpwd(text);
		}
	    }, prev.pos("bl").adds(0, 2));
        addhl(charpass.pos("bl").adds(0, 5), sz.x,
	      prev = new Button(sbw, "Set"   ).action(() -> { setpwd(charpass.text()); }),
	             new Button(sbw, "Clear" ).action(() -> { setpwd(""); }),
	             new Button(sbw, "Random").action(() -> {setpwd(randpwd());})
	      );

	prev = add(new Label("Make kin by hearth secret:"), prev.pos("bl").adds(0, 10));
	opass = add(new TextEntry(sz.x, "") {
		public void activate(String text) {
		    BuddyWnd.this.wdgmsg("bypwd", text);
		    settext("");
		}
	    }, prev.pos("bl").adds(0, 2));
	prev = add(new Button(sbw, "Add kin").action(() -> {
		    BuddyWnd.this.wdgmsg("bypwd", opass.text());
		    opass.settext("");
	}), opass.pos("bl").adds(0, 5));
	pack();
    }

    private String randpwd() {
	String charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	StringBuilder buf = new StringBuilder();
	for(int i = 0; i < 8; i++)
	    buf.append(charset.charAt((int)(Math.random() * charset.length())));
	return(buf.toString());
    }
    
    public void setpwd(String pass) {
	wdgmsg("pwd", pass);
	charpass.settext(pass);
	charpass.commit();
    }

    public void setpname(String name) {
	wdgmsg("pname", name);
	pname.settext(name);
	pname.commit();
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
	    int id = Utils.iv(args[0]);
	    String name = ((String)args[1]).intern();
	    int online = Utils.iv(args[2]);
	    int group = Utils.iv(args[3]);
	    boolean seen = Utils.bv(args[4]);
	    Buddy b = new Buddy(id, name, online, group, seen);
	    synchronized(buddies) {
		buddies.add(b);
		idmap.put(b.id, b);
		Collections.sort(buddies, bcmp);
	    }
	    serial++;
	} else if(msg == "rm") {
	    int id = Utils.iv(args[0]);
	    Buddy b;
	    synchronized(buddies) {
		b = idmap.get(id);
		if(b != null) {
		    buddies.remove(b);
		    idmap.remove(id);
		}
	    }
	    serial++;
	} else if(msg == "chst") {
	    int id = Utils.iv(args[0]);
	    int online = Utils.iv(args[1]);
	    Buddy b = find(id);
	    b.chstatus(online);
	    if((info != null) && (info.buddy == b))
		info.update();
	} else if(msg == "upd") {
	    int id = Utils.iv(args[0]);
	    String name = (String)args[1];
	    int online = Utils.iv(args[2]);
	    int grp = Utils.iv(args[3]);
	    boolean seen = Utils.bv(args[4]);
	    Buddy b = find(id);
	    synchronized(b) {
		b.name = name;
		b.online = online;
		b.group = grp;
		b.seen = seen;
	    }
	    if((info != null) && (info.buddy == b))
		info.update();
	    serial++;
	} else if(msg == "sel") {
	    int id = Utils.iv(args[0]);
	    Window p = getparent(Window.class);
	    if(p != null) {
		p.show();
		p.raise();
	    }
	    bl.change(find(id));
	} else if(msg == "pwd") {
	    charpass.settext((String)args[0]);
	    charpass.buf.point(charpass.buf.length());
	    charpass.commit();
	} else if(msg == "pname") {
	    pname.settext((String)args[0]);
	    pname.buf.point(pname.buf.length());
	    pname.commit();
	} else if(msg == "i-set") {
	    Buddy b = (args[0] == null) ? null : find(Utils.iv(args[0]));
	    bl.sel = b;
	    if((info == null) || (info.buddy != b)) {
		if(info != null) {
		    ui.destroy(info);
		    ui.destroy(infof);
		    info = null;
		    pack();
		}
		if(b != null) {
		    info = add(new BuddyInfo(new Coord(UI.scale(225), sz.y - offset - Window.wbox.bisz().y), b), width + margin3, offset);
		    infof = Frame.around(this, Collections.singletonList(info));
		}
		pack();
	    }
	} else if(msg.substring(0, 2).equals("i-")) {
	    if(info != null)
		info.uimsg(msg, args);
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
