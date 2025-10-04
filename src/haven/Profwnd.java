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

public class Profwnd extends Window {
    private final Profile prof;
    private final boolean live;

    public Profwnd(Profile prof, String title, boolean live) {
	super(Coord.z, title);
	this.prof = prof;
	this.live = live;
	Widget prev = add(new Profdisp(prof), Coord.z);
	pack();
    }

    public Profwnd(Profile prof, String title) {
	this(prof, title, true);
    }

    public void reqclose() {
	ui.destroy(this);
    }

    private void capture() {
	Window cap = new Profwnd(prof.copy(), this.cap + " (capture)", false);
	if(c.y + (sz.y / 2) < parent.sz.y)
	    parent.adda(cap, this.pos("bl").adds(0, 10), 0, 0);
	else
	    parent.adda(cap, this.pos("ul").subs(0, 10), 0, 1);
    }

    public boolean keydown(KeyDownEvent ev) {
	if(ev.c == 's') {
	    if(live)
		capture();
	    return(true);
	}
	return(super.keydown(ev));
    }
}
