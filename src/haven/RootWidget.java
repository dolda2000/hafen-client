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

public class RootWidget extends ConsoleHost implements UI.Notice.Handler, Widget.CursorQuery.Handler, Console.Directory {
    public static final Text.Foundry msgfoundry = new Text.Foundry(Text.dfont, 14);
    public static final Resource defcurs = Resource.local().loadwait("gfx/hud/curs/arw");
    public boolean modtip = false;
    Profile guprof, grprof, ggprof;
    private Text lastmsg;
    private double msgtime;
	
    public RootWidget(UI ui, Coord sz) {
	super(ui, new Coord(0, 0), sz);
	setfocusctl(true);
	hasfocus = true;
    }
	
    public boolean getcurs(CursorQuery ev) {
	Resource ret = defcurs;
	if(cursor != null) {
	    try {
		ret = cursor.get();
	    } catch(Loading l) {}
	}
	ev.set(ret);
	return(false);
    }

    public boolean globtype(GlobKeyEvent ev) {
	if(ev.propagate(this))
	    return(true);
	if(ev.c == '`') {
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
	    return(true);
	} else if(ev.c == ':') {
	    entercmd();
	    return(true);
	} else if(ev.c != 0) {
	    wdgmsg("gk", (int)ev.c, ev.mods);
	    return(true);
	}
	return(super.globtype(ev));
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
	    if(args.length == 1) {
		ui.msg((String)args[0]);
	    } else {
		ui.loader.defer(() -> {
			int a = 0;
			UI.SimpleMessage info = new UI.InfoMessage((String)args[a++]);
			if(args[a] instanceof Color)
			    info.color = (Color)args[a++];
			if(args.length > a) {
			    Indir<Resource> res = ui.sess.getresv(args[a++]);
			    info.sfx = (res == null) ? null : Audio.resclip(res.get());
			}
			ui.msg(info);
		    }, null);
	    }
	} else if(msg == "msg2") {
	    ui.loader.defer(() -> {
		    Resource res = ui.sess.getresv(args[0]).get();
		    UI.Notice.Factory fac = res.getcode(UI.Notice.Factory.class, true);
		    ui.msg(fac.format(new OwnerContext() {
			    public <T> T context(Class<T> cl) {
				return(wdgctx.context(cl, RootWidget.this));
			    }
			}, Utils.splice(args, 1)));
		}, null);
	} else if(msg == "sfx") {
	    int a = 0;
	    Indir<Resource> resid = ui.sess.getresv(args[a++]);
	    double vol = (args.length > a) ? Utils.dv(args[a++]) : 1.0;
	    double spd = (args.length > a) ? Utils.dv(args[a++]) : 1.0;
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
	    Indir<Resource> resid = (args.length > a) ? ui.sess.getresv(args[a++]) : null;
	    boolean loop = (args.length > a) ? Utils.bv(args[a++]) : false;
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

    public boolean msg(UI.NoticeEvent ev) {
	if(ev.propagate(this))
	    return(true);
	msg(ev.msg.message(), ev.msg.color());
	ui.sfxrl(ev.msg.sfx());
	return(true);
    }

    public void error(String msg) {
	ui.error(msg);
    }

    public Object tooltip(Coord c, Widget prev) {
	if(modtip && (ui.modflags() != 0))
	    return(KeyMatch.modname(ui.modflags()));
	return(super.tooltip(c, prev));
    }

    private Map<String, Console.Command> cmdmap = new TreeMap<String, Console.Command>();
    {
	cmdmap.put("wdgtree", new Console.Command() {
		public void run(Console cons, String[] args) throws Exception {
		    for(Widget w = RootWidget.this; w != null; w = w.rnext()) {
			for(Widget p = w.parent; p != null; p = p.parent)
			    cons.out.write('\t');
			cons.out.write(w.visible ? 'S' : 'H');
			cons.out.write(' ');
			cons.out.write(w.hasfocus ? "F" : "f");
			cons.out.write(w.focusctl ? "C" : "c");
			cons.out.write(w.focustab ? "T" : "t");
			cons.out.write(w.canfocus ? "A" : "a");
			cons.out.write(w.autofocus ? "T" : "t");
			cons.out.write(((w.parent != null) && (w.parent.focused == w)) ? "P" : "p");
			cons.out.write(' ');
			cons.out.write(w.toString());
			cons.out.write('\n');
		    }
		}
	    });
    }
    public Map<String, Console.Command> findcmds() {
	return(cmdmap);
    }
}
