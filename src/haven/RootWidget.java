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
import java.awt.event.KeyEvent;

public class RootWidget extends ConsoleHost implements UI.MessageWidget {
    public static final Resource defcurs = Resource.local().loadwait("gfx/hud/curs/arw");
    public static final Text.Foundry msgfoundry = new Text.Foundry(Text.dfont, 14);
    public static final Resource errsfx = Resource.local().loadwait("sfx/error");
    public static final Resource msgsfx = Resource.local().loadwait("sfx/msg");
    public boolean modtip = false;
    Profile guprof, grprof, ggprof;
    private Text lastmsg;
    private double msgtime;
	
    public RootWidget(UI ui, Coord sz) {
	super(ui, new Coord(0, 0), sz);
	setfocusctl(true);
	hasfocus = true;
	cursor = defcurs.indir();
    }
	
    public boolean globtype(char key, KeyEvent ev) {
	if(!super.globtype(key, ev)) {
	    if(key == '`') {
		if(UIPanel.profile.get()) {
		    add(new Profwnd(guprof, "UI profile"), UI.scale(100, 100));
		    add(new Profwnd(grprof, "GL profile"), UI.scale(500, 100));
		    /* XXXRENDER
		    GameUI gi = findchild(GameUI.class);
		    if((gi != null) && (gi.map != null))
			add(new Profwnd(gi.map.prof, "Map profile"), UI.scale(100, 250));
		    */
		}
		if(UIPanel.profilegpu.get()) {
		    add(new Profwnd(ggprof, "GPU profile"), UI.scale(500, 250));
		}
	    } else if(key == ':') {
		entercmd();
	    } else if(key != 0) {
		wdgmsg("gk", (int)key);
	    }
	}
	return(true);
    }

    public void draw(GOut g) {
	super.draw(g);
	if(cmdline != null) {
	    drawcmd(g, new Coord(UI.scale(20), sz.y - UI.scale(20)));
	} else if(lastmsg != null) {
	    if((Utils.rtime() - msgtime) > 3.0) {
		lastmsg = null;
	    } else {
		Coord msgc = pos("cbl").adds(20, -20).sub(0, lastmsg.sz().y);
		g.chcolor(0, 0, 0, 192);
		g.frect(msgc.sub(UI.scale(2, 2)), lastmsg.sz().add(UI.scale(4, 4)));
		g.chcolor();
		g.image(lastmsg.tex(), msgc);
	    }
	}
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "err") {
	    ui.error((String)args[0]);
	} else if(msg == "msg") {
	    ui.msg((String)args[0]);
	} else if(msg == "sfx") {
	    int a = 0;
	    Indir<Resource> resid = ui.sess.getres((Integer)args[a++]);
	    double vol = (args.length > a) ? ((Number)args[a++]).doubleValue() : 1.0;
	    double spd = (args.length > a) ? ((Number)args[a++]).doubleValue() : 1.0;
	    ui.sess.glob.loader.defer(() -> {
		    Audio.CS clip = Audio.fromres(resid.get());
		    if(spd != 1.0)
			clip = new Audio.Resampler(clip).sp(spd);
		    if(vol != 1.0)
			clip = new Audio.VolAdjust(clip, vol);
		    Audio.play(clip);
		}, null);
	} else if(msg == "bgm") {
	    int a = 0;
	    Indir<Resource> resid = (args.length > a) ? ui.sess.getres((Integer)args[a++]) : null;
	    boolean loop = (args.length > a) ? ((Number)args[a++]).intValue() != 0 : false;
	    if(Music.enabled) {
		if(resid == null)
		    Music.play(null, false);
		else
		    Music.play(resid, loop);
	    }
	} else {
	    super.uimsg(msg, args);
	}
    }

    public void msg(String msg, Color color) {
	lastmsg = msgfoundry.render(msg, color);
	msgtime = Utils.rtime();
    }

    private double lasterrsfx = 0;
    public void error(String msg) {
	msg(msg, new Color(192, 0, 0));
	double now = Utils.rtime();
	if(now - lasterrsfx > 0.1) {
	    ui.sfx(errsfx);
	    lasterrsfx = now;
	}
    }

    private double lastmsgsfx = 0;
    public void msg(String msg) {
	msg(msg, Color.WHITE);
	double now = Utils.rtime();
	if(now - lastmsgsfx > 0.1) {
	    ui.sfx(msgsfx);
	    lastmsgsfx = now;
	}
    }

    public Object tooltip(Coord c, Widget prev) {
	if(modtip && (ui.modflags() != 0))
	    return(KeyMatch.modname(ui.modflags()));
	return(super.tooltip(c, prev));
    }
}
