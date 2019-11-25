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

public class Speedget extends Widget {
    public static final Tex imgs[][];
    public static final String tips[];
    public static final Coord tsz;
    public int cur, max;

    static {
	String[] names = {"crawl", "walk", "run", "sprint"};
	String[] vars = {"dis", "off", "on"};
	imgs = new Tex[names.length][vars.length];
	int w = 0;
	for(int i = 0; i < names.length; i++) {
	    for(int o = 0; o < vars.length; o++)
		imgs[i][o] = Resource.loadtex("gfx/hud/meter/rmeter/" + names[i] + "-" + vars[o]);
	    w += imgs[i][0].sz().x;
	}
	tsz = new Coord(w, imgs[0][0].sz().y);
	tips = new String[names.length];
	for(int i = 0; i < names.length; i++) {
	    tips[i] = Resource.local().loadwait("gfx/hud/meter/rmeter/" + names[i] + "-on").layer(Resource.tooltip).t;
	}
    }

    @RName("speedget")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    int cur = (Integer)args[0];
	    int max = (Integer)args[1];
	    return(new Speedget(cur, max));
	}
    }

    public Speedget(int cur, int max) {
	super(tsz);
	this.cur = cur;
	this.max = max;
    }

    public void draw(GOut g) {
	int x = 0;
	for(int i = 0; i < 4; i++) {
	    Tex t;
	    if(i == cur)
		t = imgs[i][2];
	    else if(i > max)
		t = imgs[i][0];
	    else
		t = imgs[i][1];
	    g.image(t, new Coord(x, 0));
	    x += t.sz().x;
	}
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "cur")
	    cur = (Integer)args[0];
	else if(msg == "max")
	    max = (Integer)args[0];
    }

    public void set(int s) {
	wdgmsg("set", s);
    }

    public boolean mousedown(Coord c, int button) {
	int x = 0;
	for(int i = 0; i < 4; i++) {
	    x += imgs[i][0].sz().x;
	    if(c.x < x) {
		set(i);
		break;
	    }
	}
	return(true);
    }

    public boolean mousewheel(Coord c, int amount) {
	if(max >= 0)
	    set(Utils.clip(cur + amount, 0, max));
	return(true);
    }

    public Object tooltip(Coord c, Widget prev) {
	if((cur >= 0) && (cur < tips.length))
	    return(String.format("Selected speed: " + tips[cur]));
	return(null);
    }

    public static final KeyBinding kb_speedup = KeyBinding.get("speed-up", KeyMatch.forchar('R', KeyMatch.S | KeyMatch.C | KeyMatch.M, KeyMatch.C));
    public static final KeyBinding kb_speeddn = KeyBinding.get("speed-down", KeyMatch.forchar('R', KeyMatch.S | KeyMatch.C | KeyMatch.M, KeyMatch.S | KeyMatch.C));
    public static final KeyBinding[] kb_speeds = {
	KeyBinding.get("speed-set/0", KeyMatch.nil),
	KeyBinding.get("speed-set/1", KeyMatch.nil),
	KeyBinding.get("speed-set/2", KeyMatch.nil),
	KeyBinding.get("speed-set/3", KeyMatch.nil),
    };
    public boolean globtype(char key, KeyEvent ev) {
	int dir = 0;
	if(kb_speedup.key().match(ev))
	    dir = 1;
	else if(kb_speeddn.key().match(ev))
	    dir = -1;
	if(dir != 0) {
	    if(max >= 0) {
		set(Utils.clip(cur + dir, 0, max));
	    }
	    return(true);
	}
	for(int i = 0; i < kb_speeds.length; i++) {
	    if(kb_speeds[i].key().match(ev)) {
		set(i);
		return(true);
	    }
	}
	return(super.globtype(key, ev));
    }
}
