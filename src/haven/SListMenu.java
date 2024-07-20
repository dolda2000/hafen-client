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
import java.util.function.*;
import java.awt.image.BufferedImage;

public abstract class SListMenu<I, W extends Widget> extends Widget {
    public static final Text.Foundry bigf = CharWnd.attrf;
    public static final Text.Foundry smallf = new Text.Foundry(Text.fraktur, 14).aa(true);
    public static final Tex bg = Window.bg;
    public static final IBox obox = Window.wbox;
    public final InnerList box;
    public boolean grab = true;
    private UI.Grab mg, kg;

    protected abstract List<? extends I> items();
    protected abstract W makeitem(I item, int idx, Coord sz);
    protected abstract void choice(I item);

    public SListMenu(Coord sz, int itemh) {
	box = new InnerList(sz, itemh);
	Coord osz = box.sz.add(obox.cisz());
	resize(Coord.of(osz.x, 0));
	aresize(this.sz, osz);
	add(box, obox.ctloff());
    }

    public class Item extends SListWidget.ItemWidget<I> {
	private Item(I item, W child) {
	    super(box, child.sz, item);
	    add(child, 0, 0);
	}
    }

    public class InnerList extends SListBox<I, Item> {
	private Coord mc = Coord.of(-1, -1);

	private InnerList(Coord sz, int itemh) {
	    super(sz, itemh);
	}

	protected List<? extends I> items() {
	    return(SListMenu.this.items());
	}

	protected Item makeitem(I item, int idx, Coord sz) {
	    return(new Item(item, SListMenu.this.makeitem(item, idx, sz)));
	}

	public void change(I item) {
	    choice(item);
	}

	public void mousemove(Coord c) {
	    super.mousemove(c);
	    this.mc = c;
	}

	protected void drawbg(GOut g, I item, int idx, Area area) {
	    if(area.contains(mc)) {
		g.chcolor(255, 255, 0, 128);
		g.frect2(area.ul, area.br);
		g.chcolor();
	    } else {
		super.drawbg(g, item, idx, area);
	    }
	}
    }

    private void aresize(Coord f, Coord t) {
	clearanims(Anim.class);
	new NormAnim(0.15) {
	    public void ntick(double a) {
		double b = (a >= 1) ? 0 : Math.cos(Math.PI * 2.5 * a) * Math.exp(-5 * a);
		resize(t.add(f.sub(t).mul(b)));
	    }
	};
    }

    private boolean inited = false;
    public void tick(double dt) {
	if(!inited) {
	    int n = items().size();
	    if((n * box.itemh) < box.sz.y) {
		box.resizeh(box.itemh * n);
		aresize(this.sz, box.sz.add(obox.cisz()));
	    }
	    inited = true;
	}
	super.tick(dt);
    }

    public void draw(GOut g) {
	Coord bgc = new Coord();
	Coord ctl = obox.btloff();
	Coord cbr = sz.sub(obox.cisz()).add(ctl);
	for(bgc.y = ctl.y; bgc.y < cbr.y; bgc.y += bg.sz().y) {
	    for(bgc.x = ctl.x; bgc.x < cbr.x; bgc.x += bg.sz().x)
		g.image(bg, bgc, ctl, cbr);
	}
	obox.draw(g, Coord.z, sz);
	super.draw(g);
    }

    public boolean mousedown(Coord c, int button) {
	if(!c.isect(Coord.z, sz)) {
	    choice(null);
	} else {
	    super.mousedown(c, button);
	}
	return(true);
    }

    public boolean mousehover(Coord c, boolean hovering) {
	super.mousehover(c, hovering);
	return(hovering);
    }

    public boolean keydown(java.awt.event.KeyEvent ev) {
	if(key_esc.match(ev))
	    choice(null);
	return(true);
    }

    protected void added() {
	if(grab) {
	    mg = ui.grabmouse(this);
	    kg = ui.grabkeys(this);
	}
    }

    public void destroy() {
	if(mg != null) {
	    mg.remove();
	    kg.remove();
	}
	super.destroy();
    }

    public Object tooltip(Coord c, Widget prev) {
	Object ret = super.tooltip(c, prev);
	return((ret != null) ? ret : "");
    }

    public SListMenu<I, W> addat(Widget wdg, Coord c) {
	wdg.ui.root.add(this, wdg.rootpos(c));
	return(this);
    }

    public SListMenu nograb() {
	grab = false;
	return(this);
    }

    public static abstract class TextMenu<I> extends SListMenu<I, Widget> {
	public final Text.Foundry fnd;

	public TextMenu(Coord sz, Text.Foundry fnd) {
	    super(sz, fnd.height());
	    this.fnd = fnd;
	}
	public TextMenu(Coord sz) {this(sz, bigf);}

	protected abstract String nameof(I item);

	protected Widget makeitem(I item, int idx, Coord sz) {
	    return(new SListWidget.TextItem(sz) {
		    protected String text() {return(nameof(item));}
		    protected Text.Foundry foundry() {return(fnd);}
		});
	}
    }

    public static <I> SListMenu<I, Widget> of(Coord sz, Text.Foundry fnd, List<? extends I> items, Function<? super I, String> nmf, Consumer<? super I> action, Runnable cancel) {
	return(new TextMenu<I>(sz, (fnd == null) ? bigf : fnd) {
		protected String nameof(I item) {return(nmf.apply(item));}
		protected List<? extends I> items() {return(items);}
		protected void choice(I item) {
		    if(item != null)
			action.accept(item);
		    else if(cancel != null)
			cancel.run();
		    reqdestroy();
		}
	    });
    }
    public static <I> SListMenu<I, Widget> of(Coord sz, List<? extends I> items, Function<? super I, String> nmf, Consumer<? super I> action) {
	return(of(sz, null, items, nmf, action, null));
    }

    public static interface Action extends Runnable {
	public String name();

	public static Action of(String name, Runnable fun) {
	    return(new Action() {
		    public String name() {return(name);}
		    public void run() {fun.run();}
		});
	}
    }

    public static SListMenu<Action, Widget> of(Coord sz, Text.Foundry fnd, List<? extends Action> actions, Runnable cancel) {
	return(of(sz, fnd, actions, Action::name, Action::run, cancel));
    }

    public static SListMenu<Action, Widget> of(Coord sz, Text.Foundry fnd, List<? extends Action> actions) {
	return(of(sz, fnd, actions, null));
    }

    public static abstract class IconMenu<I> extends SListMenu<I, Widget> {
	public final Text.Foundry fnd;

	public IconMenu(Coord sz, Text.Foundry fnd) {
	    super(sz, fnd.height());
	    this.fnd = fnd;
	}
	public IconMenu(Coord sz) {this(sz, bigf);}

	protected abstract String nameof(I item);
	protected abstract BufferedImage iconof(I item);

	protected Widget makeitem(I item, int idx, Coord sz) {
	    return(new SListWidget.IconText(sz) {
		    protected BufferedImage img() {return(iconof(item));}
		    protected String text() {return(nameof(item));}
		    protected Text.Foundry foundry() {return(fnd);}
		});
	}
    }

    public static <I> SListMenu<I, Widget> of(Coord sz, Text.Foundry fnd, List<? extends I> items, Function<? super I, String> nmf, Function<? super I, BufferedImage> imgf, Consumer<? super I> action, Runnable cancel) {
	return(new IconMenu<I>(sz, (fnd == null) ? bigf : fnd) {
		protected String nameof(I item) {return(nmf.apply(item));}
		protected BufferedImage iconof(I item) {return(imgf.apply(item));}
		protected List<? extends I> items() {return(items);}
		protected void choice(I item) {
		    if(item != null)
			action.accept(item);
		    else if(cancel != null)
			cancel.run();
		    reqdestroy();
		}
	    });
    }
    public static <I> SListMenu<I, Widget> of(Coord sz, List<? extends I> items, Function<? super I, String> nmf, Function<? super I, BufferedImage> imgf, Consumer<? super I> action) {
	return(of(sz, null, items, nmf, imgf, action, null));
    }
}
