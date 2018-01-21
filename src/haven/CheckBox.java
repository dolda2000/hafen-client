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

public class CheckBox extends Widget {
    public static final Coord boxsz = new Coord(13, 13);
    public Text lbl;
    public boolean a = false;

    @RName("chk")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    CheckBox ret = new CheckBox((String)args[0]);
	    ret.canactivate = true;
	    return(ret);
	}
    }

    public CheckBox(String lbl) {
	this.lbl = Text.std.render(lbl, java.awt.Color.WHITE);
	sz = new Coord(boxsz.x + 5 + this.lbl.sz().x, Math.max(boxsz.y, this.lbl.sz().y));
    }
	
    public boolean mousedown(Coord c, int button) {
	if(button != 1)
	    return(false);
	set(!a);
	return(true);
    }
    
    public void set(boolean a) {
	this.a = a;
	changed(a);
    }

    public void draw(GOut g) {
	g.image(lbl.tex(), new Coord(boxsz.x + 5, 0));
	g.chcolor(255, 255, 255, 225);
	g.frect(new Coord(0, sz.y - boxsz.y), boxsz);
	if(a) {
	    g.chcolor(0, 0, 0, 255);
	    g.line(new Coord(1, sz.y - boxsz.y + 1), new Coord(boxsz.x - 1, sz.y - 1), 1);
	    g.line(new Coord(1, sz.y - 1), new Coord(boxsz.x - 1, sz.y - boxsz.y + 1), 1);
	}
	g.chcolor();
	super.draw(g);
    }
    
    public void changed(boolean val) {
	if(canactivate)
	    wdgmsg("ch", a?1:0);
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "ch") {
	    this.a = ((Integer)args[0]) != 0;
	} else {
	    super.uimsg(msg, args);
	}
    }
}
