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
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.function.*;
import haven.resutil.FoodInfo;
import haven.resutil.Curiosity;
import static haven.PUtils.*;

public class CharWnd extends Window {
    public static final RichText.Foundry ifnd = new RichText.Foundry(Resource.remote(), java.awt.font.TextAttribute.FAMILY, "SansSerif", java.awt.font.TextAttribute.SIZE, UI.scale(9)).aa(true);
    public static final Text.Furnace catf = new BlurFurn(new TexFurn(new Text.Foundry(Text.fraktur, 25).aa(true), Window.ctex), UI.scale(3), UI.scale(2), new Color(96, 48, 0));
    public static final Text.Furnace failf = new BlurFurn(new TexFurn(new Text.Foundry(Text.fraktur, 25).aa(true), Resource.loadimg("gfx/hud/fontred")), UI.scale(3), UI.scale(2), new Color(96, 48, 0));
    public static final Text.Foundry attrf = new Text.Foundry(Text.fraktur.deriveFont((float)Math.floor(UI.scale(18.0)))).aa(true);
    public static final PUtils.Convolution iconfilter = new PUtils.Lanczos(3);
    public static final int attrw = BAttrWnd.FoodMeter.frame.sz().x - wbox.bisz().x;
    public static final Color debuff = new Color(255, 128, 128);
    public static final Color buff = new Color(128, 255, 128);
    public static final Color tbuff = new Color(128, 128, 255);
    public static final Color every = new Color(255, 255, 255, 16), other = new Color(255, 255, 255, 32);
    public static final int width = UI.scale(255);
    public static final int height = UI.scale(260);
    public BAttrWnd battr;
    public SAttrWnd sattr;
    public SkillWnd skill;
    public FightWnd fight;
    public WoundWnd wound;
    public QuestWnd quest;
    public final Tabs.Tab battrtab, sattrtab, skilltab, fighttab, woundtab, questtab;
    public int exp, enc;

    public static class TabProxy extends AWidget {
	public final Class<? extends Widget> tcl;
	public final String id;
	private Widget tab = null;

	public TabProxy(Class<? extends Widget> tcl, String id) {
	    this.tcl = tcl;
	    this.id = id;
	}

	protected void added() {
	    super.added();
	    if(tab == null) {
		CharWnd chr = getparent(CharWnd.class);
		tab = chr.getchild(tcl);
		unlink();
		if(tab != null) {
		    tab.addchild(this, id);
		} else {
		    tab = Utils.construct(tcl);
		    tab.addchild(this, id);
		    chr.addchild(tab, "tab");
		}
	    }
	}

	public void uimsg(String nm, Object... args) {
	    tab.uimsg(nm, args);
	}
    }

    public <T> T getchild(Class<T> cl) {
	T ret = super.getchild(cl);
	if(ret != null)
	    return(ret);
	if(ret == null) {
	    for(Widget ch : children()) {
		if((ch instanceof Tabs.Tab) && ((ret = ch.getchild(cl)) != null))
		    return(ret);
	    }
	}
	return(null);
    }

    public static class RLabel<V> extends Label {
	private final Supplier<V> val;
	private final Function<V, String> fmt;
	private final Function<V, Color> col;
	private Coord oc;
	private Color lc;
	private V lv;

        private RLabel(Supplier<V> val, Function<V, String> fmt, Function<V, Color> col, V ival) {
            super(ival == null ? "" : fmt.apply(ival));
	    this.val = val;
	    this.fmt = fmt;
	    this.col = col;
	    this.lv = ival;
            this.oc = oc;
	    if((col != null) && (ival != null))
		setcolor(lc = col.apply(ival));
        }

        public RLabel(Supplier<V> val, Function<V, String> fmt, Function<V, Color> col) {
	    this(val, fmt, col, null);
	}

        public RLabel(Supplier<V> val, Function<V, String> fmt, Color col) {
	    this(val, fmt, (Function<V, Color>)null);
	    setcolor(col);
	}

	private void update() {
	    V v = val.get();
	    if(!Utils.eq(v, lv)) {
		settext(fmt.apply(v));
		lv = v;
		if(col != null) {
		    Color c = col.apply(v);
		    if(!Utils.eq(c, lc)) {
			setcolor(c);
			lc = c;
		    }
		}
	    }
	}

	protected void attached() {
	    super.attached();
	    if(oc == null)
		oc = new Coord(c.x + sz.x, c.y);
	    if(lv == null)
		update();
	}

	public void settext(String text) {
	    super.settext(text);
	    if(oc != null)
		move(oc.add(-sz.x, 0));
	}

	public void tick(double dt) {
	    update();
	}
    }

    public static class LoadingTextBox extends RichTextBox {
	private Indir<String> text = null;

	public LoadingTextBox(Coord sz, String text, RichText.Foundry fnd) {super(sz, text, fnd);}
	public LoadingTextBox(Coord sz, String text, Object... attrs) {super(sz, text, attrs);}

	public void settext(Indir<String> text) {
	    this.text = text;
	}

	public void draw(GOut g) {
	    if(text != null) {
		try {
		    settext(text.get());
		    text = null;
		} catch(Loading l) {
		}
	    }
	    super.draw(g);
	}
    }

    @RName("chr")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    return(new CharWnd(ui.sess.glob));
	}
    }

    public static <T extends Widget> T settip(T wdg, String resnm) {
	wdg.tooltip = new Widget.PaginaTip(new Resource.Spec(Resource.local(), resnm));
	return(wdg);
    }

    public CharWnd(Glob glob) {
	super(UI.scale(new Coord(300, 290)), "Character Sheet");

	Tabs tabs = new Tabs(new Coord(15, 10), UI.scale(506, 315), this);
        battrtab = tabs.add();
        sattrtab = tabs.add();
	skilltab = tabs.add();
	fighttab = tabs.add();
	woundtab = tabs.add();
	questtab = tabs.add();

	{
	    Widget prev;

	    class TB extends IButton {
		final Tabs.Tab tab;
		TB(String nm, Tabs.Tab tab, String tip) {
		    super("gfx/hud/chr/" + nm, "u", "d", null);
		    this.tab = tab;
		    settip(tip);
		}

		public void click() {
		    tabs.showtab(tab);
		}

		protected void depress() {
		    ui.sfx(Button.clbtdown.stream());
		}

		protected void unpress() {
		    ui.sfx(Button.clbtup.stream());
		}
	    }

	    this.addhl(new Coord(tabs.c.x, tabs.c.y + tabs.sz.y + UI.scale(10)), tabs.sz.x,
		new TB("battr", battrtab, "Base Attributes"),
		new TB("sattr", sattrtab, "Abilities"),
		new TB("skill", skilltab, "Lore & Skills"),
		new TB("fgt",   fighttab, "Martial Arts & Combat Schools"),
		new TB("wound", woundtab, "Health & Wounds"),
		new TB("quest", questtab, "Quest Log")
	    );
	}

	resize(contentsz().add(UI.scale(15, 10)));
    }

    public void addchild(Widget child, Object... args) {
	String place = (args[0] instanceof String) ? (((String)args[0]).intern()) : null;
	if((place == "tab") || /* XXX: Remove me! */ Utils.eq(args[0], Coord.of(47, 47))) {
	    if(child instanceof BAttrWnd) {
		battr = battrtab.add((BAttrWnd)child, Coord.z);
	    } else if(child instanceof SAttrWnd) {
		sattr = sattrtab.add((SAttrWnd)child, Coord.z);
	    } else if(child instanceof SkillWnd) {
		skill = skilltab.add((SkillWnd)child, Coord.z);
	    } else if(child instanceof FightWnd) {
		fight = fighttab.add((FightWnd)child, Coord.z);
	    } else if(child instanceof WoundWnd) {
		wound = woundtab.add((WoundWnd)child, Coord.z);
	    } else if(child instanceof QuestWnd) {
		quest = questtab.add((QuestWnd)child, Coord.z);
	    } else if(child instanceof TabProxy) {
		add(child);
	    } else {
		throw(new RuntimeException("unknown tab widget: " + child));
	    }
	} else if(place == "fmg") {
	    /* XXX: Remove me! */
	    fight = fighttab.add((FightWnd)child, 0, 0);
	} else {
	    super.addchild(child, args);
	}
    }

    public void uimsg(String nm, Object... args) {
	if(nm == "attr") {
	    int a = 0;
	    while(a < args.length) {
		String attr = (String)args[a++];
		int base = Utils.iv(args[a++]);
		int comp = Utils.iv(args[a++]);
		ui.sess.glob.cattr(attr, base, comp);
	    }
	} else if(nm == "exp") {
	    exp = Utils.iv(args[0]);
	} else if(nm == "enc") {
	    enc = Utils.iv(args[0]);
	} else {
	    super.uimsg(nm, args);
	}
    }
}
