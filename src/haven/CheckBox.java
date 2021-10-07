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

public class CheckBox extends ACheckBox {
    public static final Tex lbox = Resource.loadtex("gfx/hud/chkbox");
    public static final Tex lmark = Resource.loadtex("gfx/hud/chkmark");
    public static final Tex sbox = Resource.loadtex("gfx/hud/chkboxs");
    public static final Tex smark = Resource.loadtex("gfx/hud/chkmarks");
    public final Tex box, mark;
    public final Coord loff;
    Text lbl;

    @RName("chk")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    CheckBox ret = new CheckBox((String)args[0]);
	    ret.canactivate = true;
	    return(ret);
	}
    }

    public CheckBox(String lbl, boolean lg) {
	this.lbl = Text.std.render(lbl, java.awt.Color.WHITE);
	if(lg) {
	    box = lbox; mark = lmark;
	    loff = new Coord(0, this.lbl.sz().y / 2);
	} else {
	    box = sbox; mark = smark;
	    loff = UI.scale(new Coord(5, 0));
	}
	sz = new Coord(box.sz().x + UI.scale(5) + this.lbl.sz().x, Math.max(box.sz().y, this.lbl.sz().y));
    }

    public CheckBox(String lbl) {
	this(lbl, false);
    }

    public void draw(GOut g) {
        g.image(lbl.tex(), loff.add(box.sz().x, (sz.y - lbl.sz().y) / 2));
        g.image(box, Coord.z.add(0, (sz.y - box.sz().y) / 2));
        if(state())
            g.image(mark, Coord.z.add(0, (sz.y - mark.sz().y) / 2));
        super.draw(g);
    }
    public boolean mousedown(Coord c, int button) {
	if(button == 1) {
	    click();
	    return(true);
	}
	return(super.mousedown(c, button));
    }
}
