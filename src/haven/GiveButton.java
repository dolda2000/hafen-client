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

public class GiveButton extends Widget {
    public static Tex bg = Resource.loadtex("gfx/hud/combat/knapp/knapp");
    public static Tex ol = Resource.loadtex("gfx/hud/combat/knapp/ol");
    public static Tex or = Resource.loadtex("gfx/hud/combat/knapp/or");
    public static Tex sl = Resource.loadtex("gfx/hud/combat/knapp/sl");
    public static Tex sr = Resource.loadtex("gfx/hud/combat/knapp/sr");
    int state;
    
    @RName("give")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    return(new GiveButton(Utils.iv(args[0])));
	}
    }
	
    public GiveButton(int state, Coord sz) {
	super(sz);
	this.state = state;
    }
    
    public GiveButton(int state) {
	this(state, bg.sz());
    }
    
    public void draw(GOut g) {
	if(state == 0)
	    g.chcolor(255, 192, 192, 255);
	else if(state == 1)
	    g.chcolor(192, 192, 255, 255);
	else if(state == 2)
	    g.chcolor(192, 255, 192, 255);
	g.image(bg, Coord.z, sz);
	g.chcolor();
	if((state & 1) != 0)
	    g.image(ol, Coord.z, sz);
	else
	    g.image(sl, Coord.z, sz);
	if((state & 2) != 0)
	    g.image(or, Coord.z, sz);
	else
	    g.image(sr, Coord.z, sz);
    }
    
    public boolean mousedown(MouseDownEvent ev) {
	wdgmsg("click", ev.b);
	return(true);
    }
    
    public void uimsg(String name, Object... args) {
	if(name == "ch") {
	    this.state = Utils.iv(args[0]);
	} else {
	    super.uimsg(name, args);
	}
    }
}
