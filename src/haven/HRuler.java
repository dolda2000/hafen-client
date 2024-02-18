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

public class HRuler extends Widget {
    public static final Color defcol = new Color(192, 192, 192, 128);
    public final Coord marg;
    public final Color color;

    public HRuler(int w, Coord marg, Color color) {
	super(Coord.of(w, (marg.y * 2) + 1));
	this.marg = marg;
	this.color = color;
    }

    public HRuler(int w, Coord marg) {
	this(w, marg, defcol);
    }

    private static Coord defmarg(int w) {
	return(Coord.of(w / 10, UI.scale(2)));
    }

    public HRuler(int w, Color color) {
	this(w, defmarg(w), color);
    }

    public HRuler(int w) {
	this(w, defmarg(w));
    }

    @RName("hr")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    int w = UI.scale(Utils.iv(args[0]));
	    Color col = defcol;
	    if(args.length > 1)
		col = (Color)args[1];
	    return(new HRuler(w, col));
	}
    }

    public void draw(GOut g) {
	g.chcolor(color);
	g.line(marg, Coord.of(sz.x - 1 - marg.x, marg.y), 1);
    }
}
