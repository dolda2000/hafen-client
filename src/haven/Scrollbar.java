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

public class Scrollbar extends Widget {
    static Tex texpap = Resource.loadtex("gfx/hud/texpap");
    static Tex schain = Resource.loadtex("gfx/hud/schain");
    static Tex sflarp = Resource.loadtex("gfx/hud/sflarp");
    public int val, min, max;
    private boolean drag = false;
    
    public Scrollbar(Coord c, int h, Widget parent, int min, int max) {
	super(c.add(-sflarp.sz().x, 0), new Coord(sflarp.sz().x, h), parent);
	this.min = min;
	this.max = max;
	val = min;
    }
    
    public void draw(GOut g) {
	if(max > min) {
	    int cx = (sflarp.sz().x / 2) - (schain.sz().x / 2);
	    for(int y = 0; y < sz.y; y += schain.sz().y - 1)
		g.image(schain, new Coord(cx, y));
	    double a = (double)val / (double)(max - min);
	    int fy = (int)((sz.y - sflarp.sz().y) * a);
	    g.image(sflarp, new Coord(0, fy));
	}
    }
    
    public boolean mousedown(Coord c, int button) {
	if(button != 1)
	    return(false);
	if(max <= min)
	    return(false);
	drag = true;
	ui.grabmouse(this);
	mousemove(c);
	return(true);
    }
    
    public void mousemove(Coord c) {
	if(drag) {
	    double a = (double)(c.y - (sflarp.sz().y / 2)) / (double)(sz.y - sflarp.sz().y);
	    if(a < 0)
		a = 0;
	    if(a > 1)
		a = 1;
	    val = (int)Math.round(a * (max - min)) + min;
	    changed();
	}
    }
    
    public boolean mouseup(Coord c, int button) {
	if(button != 1)
	    return(false);
	if(!drag)
	    return(false);
	drag = false;
	ui.grabmouse(null);
	return(true);
    }
    
    public void changed() {}
    
    public void ch(int a) {
	int val = this.val + a;
	if(val > max)
	    val = max;
	if(val < min)
	    val = min;
	if(this.val != val) {
	    this.val = val;
	    changed();
	}
    }
    
    public void resize(int h) {
	super.resize(new Coord(sflarp.sz().x, h));
    }
    
    public void move(Coord c) {
	this.c = c.add(-sflarp.sz().x, 0);
    }
}
