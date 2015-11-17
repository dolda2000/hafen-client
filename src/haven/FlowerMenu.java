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
import static java.lang.Math.PI;

public class FlowerMenu extends Widget {
    public static final Color pink = new Color(255, 0, 128);
    public static final Color ptc = Color.YELLOW;
    public static final Text.Foundry ptf = new Text.Foundry(Text.dfont, 12);
    public static final IBox pbox = Window.wbox;
    public static final IBox customBox = new IBox("gfx/hud/tab", "tl", "tr", "bl", "br", "extvl", "extvr", "extht", "exthb");
    public static final Coord customBoxPadding = new Coord(5, 5);
    public static final Tex pbg = Window.bg;
    public static final int ph = 30, ppl = 8;
    public Petal[] opts;
    private UI.Grab mg, kg;
    private double a = 1.0;

    @RName("sm")
    public static class $_ implements Factory {
	public Widget create(Widget parent, Object[] args) {
	    String[] opts = new String[args.length];
	    for(int i = 0; i < args.length; i++)
		opts[i] = (String)args[i];
	    return(new FlowerMenu(opts));
	}
    }

    public class Petal extends Widget {
	public String name;
	public double ta, tr;
	public int num;
	protected Text text;
	protected double a = 1;

	public Petal(String name) {
	    super(Coord.z);
	    this.name = name;
	    text = ptf.render(name, ptc);
	    resize(text.sz().x + 25, ph);
	}

	public void move(Coord c) {
	    this.c = c.sub(sz.div(2));
	}

	public void move(double a, double r) {
	    move(Coord.sc(a, r));
	}

	public void draw(GOut g) {
	    g.chcolor(new Color(255, 255, 255, (int)(255 * a)));
	    g.image(pbg, new Coord(3, 3), new Coord(3, 3), sz.add(new Coord(-6, -6)));
	    pbox.draw(g, Coord.z, sz);
	    g.image(text.tex(), sz.div(2).sub(text.sz().div(2)));
	}

	public boolean mousedown(Coord c, int button) {
	    choose(this);
	    return(true);
	}

	public Area ta(Coord tc) {
	    return(Area.sized(tc.sub(sz.div(2)), sz));
	}

	public Area ta(double a, double r) {
	    return(ta(Coord.sc(a, r)));
	}
    }

    public class CustomPetal extends Petal {
        boolean h = false;

        public CustomPetal(String name) {
            super(name);
            resize(text.sz().x + 30, 25);
        }

        @Override
        public void draw(GOut g) {
            g.chcolor(new Color(255, 255, 255, (int)(255 * a)));
            Coord bgc = new Coord();
            for(bgc.y = 0; bgc.y < sz.y; bgc.y += pbg.sz().y) {
                for(bgc.x = 0; bgc.x < sz.x; bgc.x += pbg.sz().x)
                    g.image(pbg, bgc, Coord.z, sz);
            }
            customBox.draw(g, Coord.z, sz);
            if (h) {
                g.chcolor(0, 0, 0, (int)(128 * a));
                g.frect(Coord.z, sz);
                g.chcolor(new Color(255, 255, 255, (int)(255 * a)));
            }
            FastText.print(g, new Coord(5, 5), Integer.toString((num + 1) % 10));
            g.image(text.tex(), sz.sub(8, 0).sub(text.sz()).div(2).add(8, 0));
            g.chcolor();
        }

        @Override
        public void move(double a, double r) {
        }

        @Override
        public void mousemove(Coord c) {
            h = c.isect(Coord.z, sz.sub(1, 1));
        }
    }

    public class Opening extends NormAnim {
	Opening() {super(Config.enableMenuAnimation.get() ? 0.25 : 0);}
	
	public void ntick(double s) {
	    for(Petal p : opts) {
		p.move(p.ta + ((1 - s) * PI), p.tr * s);
		p.a = s;
	    }
        FlowerMenu.this.a = s;
	}
    }

    public class Chosen extends NormAnim {
	Petal chosen;
		
	Chosen(Petal c) {
	    super(Config.enableMenuAnimation.get() ? 0.75 : 0);
	    chosen = c;
	}
		
	public void ntick(double s) {
	    for(Petal p : opts) {
		if(p == chosen) {
		    if(s > 0.6) {
			p.a = 1 - ((s - 0.6) / 0.4);
		    } else if(s < 0.3) {
			p.move(p.ta, p.tr * (1 - (s / 0.3)));
		    }
		} else {
		    if(s > 0.3)
			p.a = 0;
		    else
			p.a = 1 - (s / 0.3);
		}
	    }
        FlowerMenu.this.a = (s > 0.3) ? 0 : 1 - (s / 0.3);
	    if(s == 1.0)
		ui.destroy(FlowerMenu.this);
	}
    }

    public class Cancel extends NormAnim {
	Cancel() {super(Config.enableMenuAnimation.get() ? 0.25 : 0);}

	public void ntick(double s) {
	    for(Petal p : opts) {
		p.move(p.ta + ((s) * PI), p.tr * (1 - s));
		p.a = 1 - s;
	    }
        FlowerMenu.this.a = 1 - s;
	    if(s == 1.0)
		ui.destroy(FlowerMenu.this);
	}
    }

    private void organize(Petal[] opts) {
	Area bounds = parent.area().xl(c.inv());
	int l = 1, p = 0, i = 0, mp = 0, ml = 1, t = 0, tt = -1;
	boolean muri = false;
	while(i < opts.length) {
	    place: {
		double ta = (PI / 2) - (p * (2 * PI / (l * ppl)));
		double tr = 75 + (50 * (l - 1));
		if(!muri && !bounds.contains(opts[i].ta(ta, tr))) {
		    if(tt < 0) {
			tt = ppl * l;
			t = 1;
			mp = p;
			ml = l;
		    } else if(++t >= tt) {
			muri = true;
			p = mp;
			l = ml;
			continue;
		    }
		    break place;
		}
		tt = -1;
		opts[i].ta = ta;
		opts[i].tr = tr;
		i++;
	    }
	    if(++p >= (ppl * l)) {
		l++;
		p = 0;
	    }
	}
    }

    private void organizeCustom(Petal[] opts) {
        int width = 80;
        for (Petal petal : opts)
            width = Math.max(width, petal.sz.x);
        Coord c = new Coord(customBoxPadding);
        for (Petal petal : opts) {
            petal.c = new Coord(c);
            petal.resize(width, petal.sz.y);
            c.y += petal.sz.y - 1;
        }
        pack();
        // clip to parent
        int x = Utils.clip(this.c.x, 0, parent.sz.x - sz.x);
        int y = Utils.clip(this.c.y, 0, parent.sz.y - sz.y);
        move(x, y);
    }

    public FlowerMenu(String... options) {
	super(Coord.z);
	opts = new Petal[options.length];
	for(int i = 0; i < options.length; i++) {
        Petal petal = Config.enableCustomFlowerMenu.get() ? new CustomPetal(options[i]) : new Petal(options[i]);
	    add(opts[i] = petal);
	    opts[i].num = i;
	}
    }

    protected void added() {
	if(c.equals(-1, -1))
	    c = parent.ui.lcc;
	mg = ui.grabmouse(this);
	kg = ui.grabkeys(this);
    if (Config.enableCustomFlowerMenu.get())
	    organizeCustom(opts);
    else
        organize(opts);
	new Opening();
    }

    public boolean mousedown(Coord c, int button) {
	if(!anims.isEmpty())
	    return(true);
	if(!super.mousedown(c, button))
	    choose(null);
	return(true);
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "cancel") {
	    new Cancel();
	    mg.remove();
	    kg.remove();
	} else if(msg == "act") {
	    new Chosen(opts[(Integer)args[0]]);
	    mg.remove();
	    kg.remove();
	}
    }

    public void draw(GOut g) {
	super.draw(g, false);
    if (Config.enableCustomFlowerMenu.get() && opts.length > 0) {
        g.chcolor(new Color(255, 255, 255, (int)(255 * a)));
        pbox.draw(g, Coord.z, sz);
        g.chcolor();
    }
    }

    public boolean keydown(java.awt.event.KeyEvent ev) {
	return(true);
    }

    public boolean type(char key, java.awt.event.KeyEvent ev) {
	if((key >= '0') && (key <= '9')) {
	    int opt = (key == '0')?10:(key - '1');
	    if(opt < opts.length) {
		choose(opts[opt]);
		kg.remove();
	    }
	    return(true);
	} else if(key == 27) {
	    choose(null);
	    kg.remove();
	    return(true);
	}
	return(false);
    }

    public void choose(Petal option) {
	if(option == null) {
	    wdgmsg("cl", -1);
	} else {
	    wdgmsg("cl", option.num, ui.modflags());
	}
    }

    @Override
    public void resize(Coord sz) {
        if (Config.enableCustomFlowerMenu.get())
            sz = sz.add(customBoxPadding);
        super.resize(sz);
    }
}
