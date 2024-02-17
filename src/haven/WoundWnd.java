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
import static haven.CharWnd.*;

public class WoundWnd extends Widget {
    public final Widget woundbox;
    public final WoundList wounds;
    public Wound.Info wound;

    public static class Wound {
	public final int id, parentid;
	public Indir<Resource> res;
	public Object qdata;
	public int level;
	private String sortkey = "\uffff";

	private Wound(int id, Indir<Resource> res, Object qdata, int parentid) {
	    this.id = id;
	    this.res = res;
	    this.qdata = qdata;
	    this.parentid = parentid;
	}

	public static class Box extends LoadingTextBox implements Info {
	    public final int id;
	    public final Indir<Resource> res;

	    public Box(int id, Indir<Resource> res) {
		super(Coord.z, "", ifnd);
		bg = null;
		this.id = id;
		this.res = res;
		settext(new Indir<String>() {public String get() {return(rendertext());}});
	    }

	    protected void added() {
		resize(parent.sz);
	    }

	    public String rendertext() {
		StringBuilder buf = new StringBuilder();
		Resource res = this.res.get();
		buf.append("$img[" + res.name + "]\n\n");
		buf.append("$b{$font[serif,16]{" + res.flayer(Resource.tooltip).t + "}}\n\n\n");
		buf.append(res.flayer(Resource.pagina).text);
		return(buf.toString());
	    }

	    public int woundid() {return(id);}
	}

	@RName("wound")
	public static class $wound implements Factory {
	    public Widget create(UI ui, Object[] args) {
		int id = (Integer)args[0];
		Indir<Resource> res = ui.sess.getres((Integer)args[1]);
		return(new Box(id, res));
	    }
	}
	public interface Info {
	    public int woundid();
	}
    }

    public class WoundList extends SListBox<Wound, Widget> {
	public List<Wound> wounds = new ArrayList<Wound>();
	private boolean loading = false;
	private final Comparator<Wound> wcomp = new Comparator<Wound>() {
	    public int compare(Wound a, Wound b) {
		return(a.sortkey.compareTo(b.sortkey));
	    }
	};

	private WoundList(Coord sz) {
	    super(sz, attrf.height() + UI.scale(2));
	}

	protected List<Wound> items() {return(wounds);}
	protected Widget makeitem(Wound w, int idx, Coord sz) {return(new Item(sz, w));}

	private List<Wound> treesort(List<Wound> from, int pid, int level) {
	    List<Wound> direct = new ArrayList<>(from.size());
	    for(Wound w : from) {
		if(w.parentid == pid) {
		    w.level = level;
		    direct.add(w);
		}
	    }
	    Collections.sort(direct, wcomp);
	    List<Wound> ret = new ArrayList<>(from.size());
	    for(Wound w : direct) {
		ret.add(w);
		ret.addAll(treesort(from, w.id, level + 1));
	    }
	    return(ret);
	}

	public void tick(double dt) {
	    if(loading) {
		loading = false;
		for(Wound w : wounds) {
		    try {
			w.sortkey = w.res.get().flayer(Resource.tooltip).t;
		    } catch(Loading l) {
			w.sortkey = "\uffff";
			loading = true;
		    }
		}
		wounds = treesort(wounds, -1, 0);
	    }
	    super.tick(dt);
	}

	public class Item extends Widget implements DTarget {
	    public final Wound w;
	    private Widget qd, nm;
	    private Object dres, dqd;

	    public Item(Coord sz, Wound w) {
		super(sz);
		this.w = w;
		update();
	    }

	    private void update() {
		if(qd != null) {qd.reqdestroy(); qd = null;}
		if(nm != null) {nm.reqdestroy(); nm = null;}
		int nw = sz.x;
		if(w.qdata != null) {
		    qd = adda(new Label(w.qdata.toString(), attrf), sz.x - UI.scale(1), sz.y / 2, 1.0, 0.5);
		    nw = qd.c.x - UI.scale(5);
		}
		int x = w.level * itemh;
		nm = adda(IconText.of(Coord.of(nw - x, sz.y), w.res), x, sz.y / 2, 0.0, 0.5);
		this.dqd = w.qdata;
		this.dres = w.res;
	    }

	    public boolean drop(Coord cc, Coord ul) {
		return(false);
	    }

	    public boolean iteminteract(Coord cc, Coord ul) {
		WoundWnd.this.wdgmsg("wiact", w.id, ui.modflags());
		return(true);
	    }

	    public void draw(GOut g) {
		if(!Utils.eq(dres, w.res) || !Utils.eq(dqd, w.qdata))
		    update();
		super.draw(g);
	    }

	    public boolean mousedown(Coord c, int button) {
		if(super.mousedown(c, button))
		    return(true);
		if(button == 1) {
		    WoundWnd.this.wdgmsg("wsel", w.id);
		    return(true);
		} else if(button == 3) {
		    WoundWnd.this.wdgmsg("wclick", w.id, button, ui.modflags());
		    return(true);
		}
		return(false);
	    }
	}

	protected void drawslot(GOut g, Wound w, int idx, Area area) {
	    super.drawslot(g, w, idx, area);
	    if((wound != null) && (wound.woundid() == w.id))
		drawsel(g, w, idx, area);
	}

	protected boolean unselect(int button) {
	    if(button == 1)
		WoundWnd.this.wdgmsg("wsel", (Object)null);
	    return(true);
	}

	public Wound get(int id) {
	    for(Wound w : wounds) {
		if(w.id == id)
		    return(w);
	    }
	    return(null);
	}

	public void add(Wound w) {
	    wounds.add(w);
	}

	public Wound remove(int id) {
	    for(Iterator<Wound> i = wounds.iterator(); i.hasNext();) {
		Wound w = i.next();
		if(w.id == id) {
		    i.remove();
		    return(w);
		}
	    }
	    return(null);
	}
    }

    public WoundWnd() {
	Widget prev;

	prev = add(CharWnd.settip(new Img(catf.render("Health & Wounds").tex()), "gfx/hud/chr/tips/wounds"), 0, 0);
	this.wounds = add(new WoundList(Coord.of(attrw, height)), prev.pos("bl").x(width + UI.scale(5)).add(wbox.btloff()));
	Frame.around(this, Collections.singletonList(this.wounds));
	woundbox = add(new Widget(new Coord(attrw, this.wounds.sz.y)) {
		public void draw(GOut g) {
		    g.chcolor(0, 0, 0, 128);
		    g.frect(Coord.z, sz);
		    g.chcolor();
		    super.draw(g);
		}

		public void cdestroy(Widget w) {
		    if(w == wound)
			wound = null;
		}
	    }, prev.pos("bl").adds(5, 0).add(wbox.btloff()));
	Frame.around(this, Collections.singletonList(woundbox));
	pack();
    }

    public static final Collection<String> children = Arrays.asList("wound");
    public void addchild(Widget child, Object... args) {
	String place = (args[0] instanceof String) ? (((String)args[0]).intern()) : null;
	if(place == "wound") {
	    this.wound = (Wound.Info)child;
	    woundbox.add(child, Coord.z);
	} else {
	    super.addchild(child, args);
	}
    }

    private void decwound(Object[] args, int a, int len) {
	int id = (Integer)args[a];
	Indir<Resource> res = (args[a + 1] == null) ? null : ui.sess.getres((Integer)args[a + 1]);
	if(res != null) {
	    Object qdata = args[a + 2];
	    int parentid = (len > 3) ? ((args[a + 3] == null) ? -1 : Utils.iv(args[a + 3])) : -1;
	    Wound w = wounds.get(id);
	    if(w == null) {
		wounds.add(new Wound(id, res, qdata, parentid));
	    } else {
		w.res = res;
		w.qdata = qdata;
	    }
	    wounds.loading = true;
	} else {
	    wounds.remove(id);
	}
    }

    public static final Collection<String> msgs = Arrays.asList("wounds");
    public void uimsg(String nm, Object... args) {
	if(nm == "wounds") {
	    if(args.length > 0) {
		if(args[0] instanceof Object[]) {
		    for(int i = 0; i < args.length; i++)
			decwound((Object[])args[i], 0, ((Object[])args[i]).length);
		} else {
		    for(int i = 0; i < args.length; i += 3)
			decwound(args, i, 3);
		}
	    }
	} else {
	    super.uimsg(nm, args);
	}
    }
}
