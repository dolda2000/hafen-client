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
import static haven.CharWnd.attrf;
import static haven.Window.wbox;

public class FightWnd extends Widget {
    public final int nsave;
    public final Actions actlist;
    public final Savelist savelist;
    public List<Action> acts = new ArrayList<Action>();
    public int usesave;
    private final Text[] saves;
    private final String[] savenames;
    private Config.Pref<String[]> savepref;
    private final CharWnd.LoadingTextBox info;

    public class Action {
	public final Indir<Resource> res;
	private final int id;
	public int a, u;
	private Text rnm, ru;
	private Tex ri;

	public Action(Indir<Resource> res, int id, int a, int u) {this.res = res; this.id = id; this.a = a; this.u = u;}

	public String rendertext() {
	    StringBuilder buf = new StringBuilder();
	    Resource res = this.res.get();
	    buf.append("$img[" + res.name + "]\n\n");
	    buf.append("$b{$font[serif,16]{" + res.layer(Resource.tooltip).t + "}}\n\n");
	    Resource.Pagina pag = res.layer(Resource.pagina);
	    if(pag != null)
		buf.append(pag.text);
	    return(buf.toString());
	}
    }

    private static final Tex[] add = {Resource.loadtex("gfx/hud/buttons/addu"),
			      Resource.loadtex("gfx/hud/buttons/addd")};
    private static final Tex[] sub = {Resource.loadtex("gfx/hud/buttons/subu"),
			      Resource.loadtex("gfx/hud/buttons/subd")};
    public class Actions extends Listbox<Action> {
	private boolean loading = false;
	private int da = -1, ds = -1;
	UI.Grab d = null;

	public Actions(int w, int h) {
	    super(w, h, attrf.height() + 2);
	}

	protected Action listitem(int n) {return(acts.get(n));}
	protected int listitems() {return(acts.size());}

	protected void drawbg(GOut g) {}

	protected void drawitem(GOut g, Action act, int idx) {
	    g.chcolor((idx % 2 == 0)?CharWnd.every:CharWnd.other);
	    g.frect(Coord.z, g.sz);
	    g.chcolor();
	    if(act.ru == null) act.ru = attrf.render(String.format("%d/%d", act.u, act.a));
	    try {
		if(act.ri == null)
		    act.ri = new TexI(PUtils.convolvedown(act.res.get().layer(Resource.imgc).img, new Coord(itemh, itemh), CharWnd.iconfilter));
		g.image(act.ri, Coord.z);
	    } catch(Loading l) {
		g.image(WItem.missing.layer(Resource.imgc).tex(), Coord.z, new Coord(itemh, itemh));
	    }
	    int ty = (itemh - act.rnm.sz().y) / 2;
	    g.image(act.rnm.tex(), new Coord(itemh + 2, ty));
	    g.aimage(act.ru.tex(), new Coord(sz.x - 45, ty), 1.0, 0.0);
	    g.aimage(add[da == idx?1:0], new Coord(sz.x - 10, itemh / 2), 1.0, 0.5);
	    g.aimage(sub[ds == idx?1:0], new Coord(sz.x - 25, itemh / 2), 1.0, 0.5);
	}

	public void change(final Action act) {
	    if(act != null)
		info.settext(new Indir<String>() {public String get() {return(act.rendertext());}});
	    else if(sel != null)
		info.settext("");
	    super.change(act);
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
			act.rnm = attrf.render(act.res.get().layer(Resource.tooltip).t);
		    } catch(Loading l) {
			act.rnm = attrf.render("...");
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

	private boolean onadd(Coord c, int idx) {
	    Coord ic = c.sub(0, (idx - sb.val) * itemh);
	    int by = (itemh - add[0].sz().y) / 2;
	    return(ic.isect(new Coord(sz.x - 10 - add[0].sz().x, by), add[0].sz()));
	}

	private boolean onsub(Coord c, int idx) {
	    Coord ic = c.sub(0, (idx - sb.val) * itemh);
	    int by = (itemh - sub[0].sz().y) / 2;
	    return(ic.isect(new Coord(sz.x - 25 - add[0].sz().x, by), add[0].sz()));
	}

	public boolean mousedown(Coord c, int button) {
	    if(button == 1) {
		int idx = (c.y / itemh) + sb.val;
		if(idx < listitems()) {
		    if(onadd(c, idx)) {
			da = idx;
			d = ui.grabmouse(this);
			return(true);
		    } else if(onsub(c, idx)) {
			ds = idx;
			d = ui.grabmouse(this);
			return(true);
		    }
		}
	    }
	    return(super.mousedown(c, button));
	}

	public boolean mouseup(Coord c, int button) {
	    if((d != null) && (button == 1)) {
		d.remove();
		d = null;
		if(da >= 0) {
		    if(onadd(c, da)) {
			Action act = listitem(da);
			act.u = Math.min(act.u + 1, act.a);
			act.ru = null;
		    }
		    da = -1;
		} else if(ds >= 0) {
		    if(onsub(c, ds)) {
			Action act = listitem(ds);
			act.u = Math.max(act.u - 1, 0);
			act.ru = null;
		    }
		    ds = -1;
		}
		return(true);
	    }
	    return(super.mouseup(c, button));
	}
    }

    public class Savelist extends Listbox<Integer> {
	public Savelist(int w, int h) {
	    super(w, h, attrf.height() + 2);
	    sel = Integer.valueOf(0);
	}

	protected Integer listitem(int idx) {return(idx);}
	protected int listitems() {return(nsave);}

	protected void drawbg(GOut g) {}

	protected void drawitem(GOut g, Integer save, int n) {
	    g.chcolor((n % 2 == 0)?CharWnd.every:CharWnd.other);
	    g.frect(Coord.z, g.sz);
	    g.chcolor();
	    g.aimage(saves[n].tex(), new Coord(20, itemh / 2), 0.0, 0.5);
	    if(n == usesave)
		g.aimage(CheckBox.smark, new Coord(itemh / 2, itemh / 2), 0.5, 0.5);
	}

    @Override
    protected void itemactivate(final Integer idx) {
        if (saves[idx] == unused)
            return;
        Window input = new SaveNameWnd(savenames[idx]) {
            public void setname(String name) {
                setsave(idx, name);
                destroy();
            }
        };
        FightWnd.this.add(input, FightWnd.this.sz.sub(input.sz).div(2));
    }
    }

    @RName("fmg")
    public static class $_ implements Factory {
	public Widget create(Widget parent, Object[] args) {
	    return(new FightWnd((Integer)args[0]));
	}
    }

    public void load(int n) {
	wdgmsg("load", n);
    }

    public void save(int n) {
	List<Object> args = new LinkedList<Object>();
	args.add(n);
	for(Action act : acts) {
	    args.add(act.id);
	    args.add(act.u);
	}
	args.add(-1);
	wdgmsg("save", args.toArray(new Object[0]));
    }

    public void use(int n) {
	wdgmsg("use", n);
    }

    private Text unused = new Text.Foundry(attrf.font.deriveFont(java.awt.Font.ITALIC)).aa(true).render("Unused save");
    public FightWnd(int nsave) {
	super(Coord.z);
	this.nsave = nsave;
	this.saves = new Text[nsave];
    this.savenames = new String[nsave];
    for(int i = 0; i < nsave; i++)
        saves[i] = unused;
    info = add(new CharWnd.LoadingTextBox(new Coord(223, 220), "", CharWnd.ifnd), new Coord(5, 35).add(wbox.btloff()));
	info.bg = new Color(0, 0, 0, 128);
	Frame.around(this, Collections.singletonList(info));

	add(new Img(CharWnd.catf.render("Martial Arts & Combat Schools").tex()), 0, 0);
	actlist = add(new Actions(250, 7), new Coord(245, 35).add(wbox.btloff()));
	Frame.around(this, Collections.singletonList(actlist));
	savelist = add(new Savelist(250, 3), new Coord(245, 225).add(wbox.btloff()));
	Frame.around(this, Collections.singletonList(savelist));

	add(new Button(110, "Load", false) {
		public void click() {
		    load(savelist.sel);
		    use(savelist.sel);
		}
	    }, 5, 274);
	add(new Button(110, "Save", false) {
		public void click() {
            final int idx = savelist.sel;
            if (saves[idx] == unused) {
                Window input = new SaveNameWnd(savenames[idx]) {
                    public void setname(String name) {
                        setsave(idx, name);
                        save(idx);
                        use(idx);
                        destroy();
                    }
                };
                FightWnd.this.add(input, FightWnd.this.sz.sub(input.sz).div(2));
            } else {
                save(savelist.sel);
                use(savelist.sel);
            }
		}
	    }, 127, 274);
	/*
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
	*/
	pack();
    }

    @Override
    public void attach(UI ui) {
        super.attach(ui);
        savepref = Config.getDeckNames(ui.sess.username, ui.sess.charname);
        String[] saved = savepref.get();
        for(int i = 0; i < nsave; i++)
            savenames[i] = (i < saved.length) ? saved[i] : "";
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
	} else if(nm == "saved") {
	    int fl = (Integer)args[0];
	    for(int i = 0; i < nsave; i++) {
		if((fl & (1 << i)) != 0) {
            setsave(i, savenames[i]);
        }
		else
		    delsave(i);
	    }
	} else if(nm == "use") {
	    usesave = (Integer)args[0];
	} else {
	    super.uimsg(nm, args);
	}
    }

    private void delsave(int idx) {
        saves[idx] = unused;
        savenames[idx] = "";
        savepref.set(savenames);
    }

    private void setsave(int idx, String text) {
        if (text == null || text.isEmpty())
            text = String.format("Saved school %d", idx + 1);
        saves[idx] = attrf.render(text);
        savenames[idx] = text;
        savepref.set(savenames);
    }

    private static abstract class SaveNameWnd extends Window {
        private final TextEntry entry;

        public SaveNameWnd(String text) {
            super(Coord.z, "Enter name...");
            entry = add(new TextEntry(200, "") {
                public void activate(String text) {
                    setname(text);
                }
            });
            entry.settext(text);
            setcanfocus(true);
            setfocusctl(true);
            setfocus(entry);
            pack();
        }

        public abstract void setname(String name);

        @Override
        public void show() {
            super.show();
            parent.setfocus(this);
        }

        @Override
        public void lostfocus() {
            super.lostfocus();
            destroy();
        }

        @Override
        public void wdgmsg(Widget sender, String msg, Object... args) {
            if((sender == this) && (msg.equals("close"))) {
                destroy();
            } else {
                super.wdgmsg(sender, msg, args);
            }
        }
    }
}
