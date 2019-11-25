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

public class Bufflist extends Widget {
    public static final int margin = 2;
    public static final int num = 5;

    public interface Managed {
	public void move(Coord c, double off);
    }

    private void arrange(Widget imm) {
	int i = 0, rn = 0, x = 0, y = 0, maxh = 0;
	Coord br = new Coord();
	Collection<Pair<Managed, Coord>> mv = new ArrayList<>();
	for(Widget wdg = child; wdg != null; wdg = wdg.next) {
	    if(!(wdg instanceof Managed))
		continue;
	    Managed ch = (Managed)wdg;
	    Coord c = new Coord(x, y);
	    if(ch == imm)
		wdg.c = c;
	    else
		mv.add(new Pair<>(ch, c));
	    i++;
	    x += wdg.sz.x + margin;
	    maxh = Math.max(maxh, wdg.sz.y);
	    if(++rn >= num) {
		x = 0;
		y += maxh + margin;
		maxh = 0;
		rn = 0;
	    }
	    if(c.x + wdg.sz.x > br.x) br.x = c.x + wdg.sz.x;
	    if(c.y + wdg.sz.y > br.y) br.y = c.y + wdg.sz.y;
	}
	resize(br);
	double off = 1.0 / mv.size(), coff = 0.0;
	for(Pair<Managed, Coord> p : mv) {
	    p.a.move(p.b, coff);
	    coff += off;
	}
    }

    public void addchild(Widget child, Object... args) {
	add(child);
	arrange(child);
    }

    public void cdestroy(Widget ch) {
	arrange(null);
    }

    public void draw(GOut g) {
	for(Widget wdg = child, next; wdg != null; wdg = next) {
	    next = wdg.next;
	    if(!wdg.visible || !(wdg instanceof Managed))
		continue;
	    wdg.draw(g.reclipl(xlate(wdg.c, true), wdg.sz));
	}
    }
}
