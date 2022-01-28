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

public class Composer {
    public Composer(Widget wdg) {
        this.wdg = wdg;
        this.hmrgn = 0;
        this.vmrgn = 0;
        this.hpad = 0;
        this.vpad = 0;
        this.y = 0;
    }

    public Composer hmrgn(int value) {
        hmrgn = value;
        return this;
    }

    public Composer vmrgn(int value) {
        vmrgn = value;
        return this;
    }

    public Composer hpad(int value) {
        hpad = value;
        return this;
    }

    public Composer vpad(int value) {
        vpad = value;
        return this;
    }

    public Composer pad(Coord value) {
        return hpad(value.x).vpad(value.y);
    }

    public int y() {
        return y;
    }

    public void add(int height) {
        y += height + vmrgn;
    }

    public <T extends Widget> T add(T child) {
        wdg.add(child, new Coord(hpad, vpad + y));
        y += child.sz.y + vmrgn;
	return(child);
    }

    public <T extends Widget> T add(T child, int x) {
        wdg.add(child, new Coord(hpad + x, vpad + y));
        y += child.sz.y + vmrgn;
	return(child);
    }

    public <T extends Widget> T adda(T child, int x, double ax) {
        wdg.adda(child, hpad + x, vpad + y, ax, 0);
        y += child.sz.y + vmrgn;
	return(child);
    }

    public void addr(Widget ... children) {
        int maxh = 0;
        for (Widget child : children) {
            maxh = Math.max(maxh, child.sz.y);
        }
        int x = hpad;
        for (Widget child : children) {
            wdg.add(child, new Coord(x, vpad + y + (maxh - child.sz.y) / 2));
            x += child.sz.x + hmrgn;
        }
        y += maxh + vmrgn;
    }

    public void addrf(int firstw, Widget ... children) {
        int maxh = 0;
        for (Widget child : children) {
            maxh = Math.max(maxh, child.sz.y);
        }
        Widget first = children[0];
        int padding = Math.max(0, firstw - first.sz.x);
        wdg.add(first, new Coord(hpad, vpad + y));
        int x = hpad + first.sz.x + padding + hmrgn;
        for (int i = 1; i < children.length; ++i) {
            Widget child = children[i];
            wdg.add(child, new Coord(x, vpad + y));
            x += child.sz.x + hmrgn;
        }
        y += maxh + vmrgn;
    }

    public void addar(int totalw, Widget ... children) {
	if(children.length == 1) {
	    adda(children[0], totalw / 2, 0.5);
	    return;
	}
        int maxh = 0, wsum = 0;
        int maxw = 0;
        for (Widget child : children) {
	    wsum += child.sz.x;
            maxw = Math.max(maxw, child.sz.x);
            maxh = Math.max(maxh, child.sz.y);
        }
        int x = hpad;
	int tpad = totalw - wsum, npad = children.length - 1, perror = 0;
        for (Widget child : children) {
            wdg.add(child, new Coord(x, vpad + y + (maxh - child.sz.y) / 2));
	    x += child.sz.x;
	    perror += tpad;
	    x += perror / npad;
	    perror %= npad;
        }
        y += maxh + vmrgn;
    }

    private final Widget wdg;
    private int hmrgn;
    private int vmrgn;
    private int hpad;
    private int vpad;
    private int y;
}
