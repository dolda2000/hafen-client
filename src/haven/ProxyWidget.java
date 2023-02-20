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

import java.awt.event.KeyEvent;

public class ProxyWidget<W extends Widget> extends Widget implements DTarget {
    public final W proxied;

    public ProxyWidget(W proxied) {
	super(proxied.sz);
	this.proxied = proxied;
    }

    public void tick(double dt) {
	resize(proxied.sz);
    }

    public void draw(GOut g) {
	proxied.draw(g);
    }

    public boolean mousedown(Coord c, int button) {
	return(proxied.mousedown(c, button));
    }
    public boolean mouseup(Coord c, int button) {
	return(proxied.mouseup(c, button));
    }
    public boolean mousewheel(Coord c, int amount) {
	return(proxied.mousewheel(c, amount));
    }
    public void mousemove(Coord c) {
	proxied.mousemove(c);
    }

    /* XXX? Does focus behavior need to be accounted for in some weird
     * and wonderful way? */
    public boolean keydown(KeyEvent ev) {
	return(proxied.keydown(ev));
    }
    public boolean keyup(KeyEvent ev) {
	return(proxied.keyup(ev));
    }

    public Resource getcurs(Coord c) {
	return(proxied.getcurs(c));
    }
    public Object tooltip(Coord c, Widget prev) {
	return(proxied.tooltip(c, prev));
    }

    /* XXX: ProxyWidget needing to implement DTarget is strange, to
     * say the least. What other interfaces might it be missing? */
    public boolean drop(Coord cc, Coord ul) {
	return((proxied instanceof DTarget) && ((DTarget)proxied).drop(cc, ul));
    }
    public boolean iteminteract(Coord cc, Coord ul) {
	return((proxied instanceof DTarget) && ((DTarget)proxied).iteminteract(cc, ul));
    }
}
