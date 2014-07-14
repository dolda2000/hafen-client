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
import java.awt.image.BufferedImage;
import java.awt.font.TextAttribute;
import java.util.*;
import static haven.Window.wbox;

public class CharWnd extends Window {
    public static final Text.Furnace catf = new PUtils.BlurFurn(new PUtils.TexFurn(new Text.Foundry(Text.fraktur, 25).aa(true), Window.ctex), 3, 1, new Color(128, 64, 0));
    public static final Text.Foundry attrf = new Text.Foundry(Text.fraktur, 18).aa(true);
    public static final Color debuff = new Color(255, 128, 128);
    public static final Color buff = new Color(128, 255, 128);

    public static class FoodMeter extends Widget {
	public static final Tex frame = Resource.loadtex("gfx/hud/chr/foodm");
	public static final Coord marg = new Coord(5, 5);

	public FoodMeter(Coord c, Widget parent) {
	    super(c, frame.sz(), parent);
	}

	public void draw(GOut g) {
	    g.chcolor(0, 0, 0, 255);
	    g.frect(marg, sz.sub(marg.mul(2)));
	    g.chcolor();
	    g.image(frame, Coord.z);
	}
    }

    public static final int attrw = FoodMeter.frame.sz().x - wbox.bisz().x;
    public class Attr extends Widget {
	public final String nm;
	public final Text rnm;
	public final Glob.CAttr attr;
	public final Tex img;
	public final Color bg;
	private Text ct;
	private int cv;

	private Attr(String attr, String rnm, Coord c, Color bg) {
	    super(c, new Coord(attrw, attrf.height() + 2), CharWnd.this);
	    this.nm = attr;
	    this.img = Resource.load("gfx/hud/chr/" + attr).loadwait().layer(Resource.imgc).tex();
	    this.rnm = attrf.render(rnm);
	    this.attr = ui.sess.glob.cattr.get(attr);
	    this.bg = bg;
	}

	public void tick(double dt) {
	    if(attr.comp != cv) {
		cv = attr.comp;
		Color c = Color.WHITE;
		if(attr.comp > attr.base) {
		    c = buff;
		    tooltip = Text.render(String.format("%d + %d", attr.base, attr.comp - attr.base));
		} else if(attr.comp < attr.base) {
		    c = debuff;
		    tooltip = Text.render(String.format("%d - %d", attr.base, attr.base - attr.comp));
		} else {
		    tooltip = null;
		}
		ct = attrf.render(Integer.toString(cv), c);
	    }
	}

	public void draw(GOut g) {
	    g.chcolor(bg);
	    g.frect(Coord.z, sz);
	    g.chcolor();
	    Coord cn = new Coord(0, sz.y / 2);
	    g.aimage(img, cn.add(5, 0), 0, 0.5);
	    g.aimage(rnm.tex(), cn.add(img.sz().x + 10, 1), 0, 0.5);
	    g.aimage(ct.tex(), cn.add(sz.x - 7, 1), 1, 0.5);
	}
    }

    @RName("chr")
    public static class $_ implements Factory {
	public Widget create(Coord c, Widget parent, Object[] args) {
	    return(new CharWnd(c, parent));
	}
    }

    public CharWnd(Coord pc, Widget parent) {
	super(pc, new Coord(300, 290), parent, "Character Sheet");

	int x = 15, y = 10;
	new Img(new Coord(x, y), catf.render("Base Attributes").tex(), this); y += 35;
	List<Attr> base = new ArrayList<Attr>();
	Attr aw;
	Color every = new Color(255, 255, 255, 16), other = new Color(255, 255, 255, 32);
	base.add(aw = new Attr("str", "Strength",     wbox.btloff().add(x, y), every)); y += aw.sz.y;
	base.add(aw = new Attr("agi", "Agility",      wbox.btloff().add(x, y), other)); y += aw.sz.y;
	base.add(aw = new Attr("int", "Intelligence", wbox.btloff().add(x, y), every)); y += aw.sz.y;
	base.add(aw = new Attr("con", "Constitution", wbox.btloff().add(x, y), other)); y += aw.sz.y;
	base.add(aw = new Attr("prc", "Perception",   wbox.btloff().add(x, y), every)); y += aw.sz.y;
	base.add(aw = new Attr("csm", "Charisma",     wbox.btloff().add(x, y), other)); y += aw.sz.y;
	base.add(aw = new Attr("dex", "Dexterity",    wbox.btloff().add(x, y), every)); y += aw.sz.y;
	base.add(aw = new Attr("psy", "Psyche",       wbox.btloff().add(x, y), other)); y += aw.sz.y;
	Frame.around(this, base);
	y += 20;
	new FoodMeter(new Coord(x, y), this);

	resize(contentsz().add(15, 10));
    }
}
