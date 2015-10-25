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

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

import static haven.MCache.cmaps;
import static haven.MCache.tilesz;

public class LocalMiniMap extends Widget implements Console.Directory {
	private static final Resource plarrow = Resource.local().loadwait("gfx/hud/mmap/plarrow");
    private static final Resource plalarm = Resource.local().loadwait("sfx/alarmplayer");

    public final MapView mv;
	private final MinimapCache cache;
    private Coord cc = null;
    private MinimapTile cur = null;
    private Coord off = Coord.z;
    private Coord doff;
    private UI.Grab grab;
    private boolean showradius;
    private boolean showgrid;
    private final HashSet<Long> threats = new HashSet<Long>();

    public LocalMiniMap(Coord sz, MapView mv) {
	super(sz);
	this.mv = mv;
	this.cache = new MinimapCache(new MinimapRenderer(mv.ui.sess.glob.map));
    this.showradius = Config.minimapShowRadius.get();
    this.showgrid = Config.minimapShowGrid.get();
    }
    
    public Coord p2c(Coord pc) {
	return(pc.div(tilesz).sub(cc).add(sz.div(2)));
    }

    public Coord c2p(Coord c) {
	return(c.sub(sz.div(2)).add(cc).mul(tilesz).add(tilesz.div(2)));
    }

    public void drawicons(GOut g) {
	OCache oc = ui.sess.glob.oc;
    ArrayList<Gob> players = new ArrayList<Gob>();
	synchronized(oc) {
	    for(Gob gob : oc) {
        try {
            // don't display icons for party members
            if (ui.sess.glob.party.memb.containsKey(gob.id))
                continue;
            if (gob.isPlayer()) {
                players.add(gob);
                continue;
            }
            MinimapIcon icon = gob.getMinimapIcon();
            if (icon != null && icon.visible())
                drawicon(g, icon, p2c(gob.rc).sub(off));
		} catch(Loading l) {}
	    }
        boolean autohearth = Config.enableAutoHearth.get();
        boolean alarm = Config.enableStrangerAlarm.get();
        for (Gob gob : players) {
            if (autohearth || alarm) {
                if (gob.isThreat() && !threats.contains(gob.id)) {
                    threats.add(gob.id);
                    if (alarm)
                        Audio.play(plalarm, Config.alarmVolume.get() / 1000.0f);
                    if (autohearth)
                        getparent(GameUI.class).menu.wdgmsg("act", "travel", "hearth");
                }
            }
            MinimapIcon icon = gob.getMinimapIcon();
            if (icon != null && icon.visible())
                drawicon(g, icon, p2c(gob.rc).sub(off));
        }
	}
    }

    private static void drawicon(GOut g, MinimapIcon icon, Coord gc) {
        Tex tex = icon.tex();
        if (tex != null) {
            g.chcolor(icon.color());
            g.image(icon.tex(), gc.sub(tex.sz().div(2)));
        }
    }

    public Gob findicongob(Coord c) {
	OCache oc = ui.sess.glob.oc;
	synchronized (oc) {
        for(Gob gob : oc) {
		try {
            if (gob.id == mv.plgob)
                continue;
            MinimapIcon icon = gob.getMinimapIcon();
		    if (icon != null && icon.visible()) {
			Coord gc = p2c(gob.rc);
			Coord sz = icon.tex().sz();
			if(c.isect(gc.sub(sz.div(2)), sz))
			    return(gob);
		    }
		} catch(Loading l) {}
	    }
	}
	return(null);
    }

    public void tick(double dt) {
	Gob pl = ui.sess.glob.oc.getgob(mv.plgob);
	if(pl == null) {
	    this.cc = null;
	    return;
	}
	this.cc = pl.rc.div(tilesz);
    }

    public void draw(GOut g) {
    g = g.reclip(Coord.z, sz);
	if(cc == null)
	    return;
	final Coord plg = cc.div(cmaps);
	synchronized (cache) {
		cache.checkSession(plg);
	}

    Coord coff = cc.add(off);
	Coord ulg = coff.sub(sz.div(2)).div(cmaps);
	Coord blg = coff.add(sz.div(2)).div(cmaps);
	Coord cg = new Coord();

    synchronized (cache) {
        for (cg.y = ulg.y; cg.y <= blg.y; cg.y++) {
            for (cg.x = ulg.x; cg.x <= blg.x; cg.x++) {
                Defer.Future<MinimapTile> f = cache.get(cg);
                Tex image = f.done() ? f.get().img : MiniMap.bg;
                g.image(image, cg.mul(cmaps).sub(coff).add(sz.div(2)));
                if (showgrid) {
                    g.chcolor(Color.DARK_GRAY);
                    g.rect(cg.mul(cmaps).sub(coff).add(sz.div(2)), MCache.cmaps);
                    g.chcolor();
                }
            }
        }
    }

	try {
		synchronized(ui.sess.glob.party.memb) {
		    for(Party.Member m : ui.sess.glob.party.memb.values()) {
			Coord mc;
			try {
                mc = m.getc();
			} catch(MCache.LoadingMap e) {
                mc = null;
			}
			if(mc == null)
			    continue;
			Coord ptc = p2c(mc).sub(off);
			// do not display party members outside of window
			// this should be replaced with the proper method of clipping rotated texture
			if (!ptc.isect(c, sz))
				continue;
			double angle = m.getangle() + Math.PI / 2;
			Coord origin = plarrow.layer(Resource.negc).cc;
			g.chcolor(m.col.getRed(), m.col.getGreen(), m.col.getBlue(), 180);
			g.image(plarrow.layer(Resource.imgc).tex(), ptc.sub(origin), origin, angle);
			g.chcolor();
            if (showradius && m.gobid == mv.plgob) {
                // view radius is 9x9 "server" grids
                Coord rc = p2c(mc.div(MCache.sgridsz).sub(4, 4).mul(MCache.sgridsz)).sub(off);
                Coord rs = MCache.sgridsz.mul(9).div(tilesz);
                g.chcolor(255, 255, 255, 60);
                g.frect(rc, rs);
                g.chcolor(0, 0, 0, 128);
                g.rect(rc, rs);
                g.chcolor();
            }
		    }
		}
	} catch(Loading l) {}
	drawicons(g);
    }

    public boolean mousedown(Coord c, int button) {
	if(cc == null)
	    return(false);

    // allow to drag minimap with middle button
    if (button == 2) {
        grab = this.ui.grabmouse(this);
        doff = c;
        return true;
    }

    Coord coff = c.add(off);
	Gob gob = findicongob(coff);
	if(gob == null)
	    mv.wdgmsg("click", rootpos().add(c), c2p(coff), button, ui.modflags());
	else {
        if (ui.modmeta && button == 1)
            tooltip = gob.getres().name;
	    mv.wdgmsg("click", rootpos().add(c), c2p(coff), button, ui.modflags(), 0, (int)gob.id, gob.rc, 0, -1);
    }
	return(true);
    }

    @Override
    public boolean mouseup(Coord c, int button) {
        if (grab != null) {
            grab.remove();
            grab = null;
            return true;
        }
        return super.mouseup(c, button);
    }

    @Override
    public void mousemove(Coord c) {
        if (grab != null) {
            off = off.add(doff.sub(c));
            doff = c;
        } else {
            super.mousemove(c);
        }
        // remove gob tooltip
        if (!ui.modmeta)
            tooltip = null;
    }

    @Override
    public Map<String, Console.Command> findcmds() {
        return(cmdmap);
    }

    public void setOffset(Coord value) {
        off = value;
    }

    public void toggleRadius() {
        showradius = !showradius;
        Config.minimapShowRadius.set(showradius);
    }

    public void toggleCustomIcons() {
        ui.sess.glob.icons.toggle();
        getparent(GameUI.class).notification("Custom icons are %s", ui.sess.glob.icons.enabled() ? "enabled" : "disabled");
    }

    public void toggleGrid() {
        showgrid = !showgrid;
        Config.minimapShowGrid.set(showgrid);
    }

    private Map<String, Console.Command> cmdmap = new TreeMap<String, Console.Command>(); {
        cmdmap.put("icons", new Console.Command() {
            public void run(Console console, String[] args) throws Exception {
                if (args.length == 2) {
                    String arg = args[1];
                    if (arg.equals("reload")) {
                        ui.sess.glob.icons.config.reload();
                        ui.sess.glob.icons.reset();
                        getparent(GameUI.class).notification("Custom icons configuration is reloaded");
                        return;
                    }
                }
                throw new Exception("No such setting");
            }
        });
    }
}
