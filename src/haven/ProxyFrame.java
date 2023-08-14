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

public class ProxyFrame<T extends Widget> extends Frame {
    public final T ch;
    public Color color = Color.WHITE;

    public ProxyFrame(T child, boolean resize) {
	super(child.sz, !resize);
	this.ch = child;
	if(resize)
	    ch.resize(inner());
	add(child, Coord.z);
    }

    public void drawframe(GOut g) {
	if(color != null) {
	    g.chcolor(color);
	    box.draw(g, Coord.z, sz);
	}
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "col") {
	    color = (Color)args[0];
	} else {
	    ch.uimsg(msg, args);
	}
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
	if(sender == ch)
	    wdgmsg(msg, args);
	else
	    super.wdgmsg(sender, msg, args);
    }
}
