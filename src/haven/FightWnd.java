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

public class FightWnd extends Window {
    public final int nsave;
    public final Actions actlist;
    public List<Action> acts = new ArrayList<Action>();
    private final CheckBox[] savesel;

    public class Action {
	public final Indir<Resource> res;
	private final int id;
	public int a, u;
	private Text rnm, ru;

	public Action(Indir<Resource> res, int id, int a, int u) {this.res = res; this.id = id; this.a = a; this.u = u;}
    }

    public class Actions extends Listbox<Action> {
	static final int eh = 34;
	private boolean loading = false;

	public Actions(int w, int h) {
	    super(w, h, eh);
	}

	protected Action listitem(int n) {return(acts.get(n));}
	protected int listitems() {return(acts.size());}

	protected void drawitem(GOut g, Action act, int idx) {
	    if(act.ru == null) act.ru = Text.render(String.format("%d/%d", act.u, act.a));
	    try {
		g.image(act.res.get().layer(Resource.imgc).tex(), Coord.z);
	    } catch(Loading l) {
		WItem.missing.loadwait();
		g.image(WItem.missing.layer(Resource.imgc).tex(), Coord.z, sz);
	    }
	    int ty = (eh - act.rnm.sz().y) / 2;
	    g.image(act.rnm.tex(), new Coord(40, ty));
	    g.image(act.ru.tex(), new Coord(sz.x - 50, ty));
	}

	public boolean mousewheel(Coord c, int am) {
	    if(ui.modshift) {
		Action act = itemat(c);
		if(act != null) {
		    act.u = Utils.clip(act.u - am, 0, act.a);
		    act.ru = null;
		}
		return(true);
	    }
	    return(super.mousewheel(c, am));
	}

	public void draw(GOut g) {
	    if(loading) {
		loading = false;
		for(Action act : acts) {
		    try {
			act.rnm = Text.render(act.res.get().layer(Resource.tooltip).t);
		    } catch(Loading l) {
			act.rnm = Text.render("...");
			loading = true;
		    }
		}
		Collections.sort(acts, new Comparator<Action>() {
			public int compare(Action a, Action b) {
			    int ret = a.rnm.text.compareTo(b.rnm.text);
			    return(ret);
			}
		    });
	    }
	    super.draw(g);
	}
    }

    @RName("fmg")
    public static class $_ implements Factory {
	public Widget create(Widget parent, Object[] args) {
	    return(new FightWnd((Integer)args[0]));
	}
    }

    private void save(int n) {
	List<Object> args = new LinkedList<Object>();
	args.add(n);
	for(Action act : acts) {
	    args.add(act.id);
	    args.add(act.u);
	}
	args.add(-1);
	FightWnd.this.wdgmsg("save", args.toArray(new Object[0]));
    }

    public FightWnd(int nsave) {
	super(new Coord(475, 450), "");
	this.nsave = nsave;
	this.savesel = new CheckBox[nsave];
	actlist = add(new Actions(250, 12), new Coord(10, 10));
	int y = actlist.sz.y;
	for(int i = nsave - 1; i >= 0; i--) {
	    final int n = i;
	    add(new Button(50, "Load") {
		    public void click() {
			FightWnd.this.wdgmsg("load", n);
		    }
		}, new Coord(270, y - Button.hs));
	    add(new Button(50, "Save") {
		    public void click() {
			save(n);
		    }
		}, new Coord(330, y - Button.hs));
	    savesel[n] = add(new CheckBox("Use", true) {
		    public boolean mousedown(Coord c, int button) {
			if(button == 1) {
			    if(a)
				FightWnd.this.wdgmsg("use", -1);
			    else
				FightWnd.this.wdgmsg("use", n);
			}
			return(true);
		    }
		}, new Coord(390, y - CheckBox.lbox.sz().y));
	    y -= Button.hs + 15;
	}
    }

    public void uimsg(String nm, Object... args) {
	if(nm == "act") {
	    List<Action> acts = new ArrayList<Action>();
	    int a = 0;
	    while(true) {
		int resid = (Integer)args[a++];
		if(resid < 0)
		    break;
		int av = (Integer)args[a++];
		int us = (Integer)args[a++];
		acts.add(new Action(ui.sess.getres(resid), resid, av, us));
	    }
	    this.acts = acts;
	    actlist.loading = true;
	} else if(nm == "use") {
	    int n = (Integer)args[0];
	    for(int i = 0; i < nsave; i++) {
		savesel[i].a = (i == n);
	    }
	} else {
	    super.uimsg(nm, args);
	}
    }
}
