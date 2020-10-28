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

import static haven.MCache.cmaps;
import static haven.MCache.tilesz;
import static haven.OCache.posres;
import haven.MCache.Grid;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.*;
import java.io.*;
import haven.resutil.Ridges;

public class LocalMiniMap extends Widget {
    public static final Tex bg = Resource.loadtex("gfx/hud/mmap/ptex");
    public static final Tex nomap = Resource.loadtex("gfx/hud/mmap/nomap");
    public static final Resource plx = Resource.local().loadwait("gfx/hud/mmap/x");
    public final MapView mv;
    public final GobIcon.Settings iconconf;
    private Coord cc = null;
    private MapTile cur = null;
    private final Map<Pair<Grid, Integer>, Defer.Future<MapTile>> cache = new LinkedHashMap<Pair<Grid, Integer>, Defer.Future<MapTile>>(5, 0.75f, true) {
	protected boolean removeEldestEntry(Map.Entry<Pair<Grid, Integer>, Defer.Future<MapTile>> eldest) {
	    if(size() > 5) {
		try {
		    MapTile t = eldest.getValue().get();
		    t.img.dispose();
		} catch(RuntimeException e) {
		}
		return(true);
	    }
	    return(false);
	}
    };
    
    public static class MapTile {
	public final Tex img;
	public final Coord ul;
	public final Grid grid;
	public final int seq;
	
	public MapTile(Tex img, Coord ul, Grid grid, int seq) {
	    this.img = img;
	    this.ul = ul;
	    this.grid = grid;
	    this.seq = seq;
	}
    }

    public LocalMiniMap(Coord sz, MapView mv, GobIcon.Settings iconconf) {
	super(sz);
	this.mv = mv;
	this.iconconf = iconconf;
    }

    public Coord p2c(Coord2d pc) {
	return(UI.scale(pc.floor(tilesz)).sub(UI.scale(cc)).add(sz.div(2)));
    }

    public Coord2d c2p(Coord c) {
	return(UI.unscale(c.sub(sz.div(2))).add(cc).mul(tilesz).add(tilesz.div(2)));
    }

    public void drawicons(GOut g) {
	OCache oc = ui.sess.glob.oc;
	synchronized(oc) {
	    for(Gob gob : oc) {
		try {
		    GobIcon icon = gob.getattr(GobIcon.class);
		    if(icon != null) {
			GobIcon.Setting conf = iconconf.get(icon.res.get());
			if((conf != null) && conf.show) {
			    Coord gc = p2c(gob.rc);
			    Tex tex = icon.tex();
			    g.image(tex, gc.sub(tex.sz().div(2)));
			}
		    }
		} catch(Loading l) {}
	    }
	}
    }

    public Gob findicongob(Coord c) {
	OCache oc = ui.sess.glob.oc;
	synchronized(oc) {
	    for(Gob gob : oc) {
		try {
		    GobIcon icon = gob.getattr(GobIcon.class);
		    if(icon != null) {
			GobIcon.Setting conf = iconconf.get(icon.res.get());
			if((conf != null) && conf.show) {
			    Coord gc = p2c(gob.rc);
			    Coord sz = icon.tex().sz();
			    if(c.isect(gc.sub(sz.div(2)), sz))
				return(gob);
			}
		    }
		} catch(Loading l) {}
	    }
	}
	return(null);
    }

    public void tick(double dt) {
	Gob pl = ui.sess.glob.oc.getgob(mv.plgob);
	if(pl == null)
	    this.cc = mv.cc.floor(tilesz);
	else
	    this.cc = pl.rc.floor(tilesz);
    }

    public static void drawplx(GOut g, Coord ptc) {
	Tex tex = plx.layer(Resource.imgc).tex();
	g.image(tex, ptc.sub(UI.scale(plx.layer(Resource.negc).cc)));
    }

    public void draw(GOut g) {
	if(cc == null)
	    return;
	map: {
	    final Grid plg;
	    try {
		plg = ui.sess.glob.map.getgrid(cc.div(cmaps));
	    } catch(Loading l) {
		break map;
	    }
	    final int seq = plg.seq;
	    if((cur == null) || (plg != cur.grid) || (seq != cur.seq)) {
		Defer.Future<MapTile> f;
		synchronized(cache) {
		    f = cache.get(new Pair<Grid, Integer>(plg, seq));
		    if(f == null) {
			f = Defer.later(new Defer.Callable<MapTile> () {
				public MapTile call() {
				    Coord ul = plg.ul.sub(cmaps).add(1, 1);
				    return(new MapTile(new TexI(MapSource.drawmap(ui.sess.glob.map, Area.sized(ul, cmaps.mul(3).sub(2, 2)))), ul, plg, seq));
				}
			    });
			cache.put(new Pair<Grid, Integer>(plg, seq), f);
		    }
		}
		if(f.done())
		    cur = f.get();
	    }
	}
	if(cur != null) {
	    g.image(bg, Coord.z, UI.scale(bg.sz()));
	    g.image(cur.img, UI.scale(cur.ul.sub(cc)).add(sz.div(2)), UI.scale(cur.img.sz()));
	    drawicons(g);
	    try {
		synchronized(ui.sess.glob.party.memb) {
		    for(Party.Member m : ui.sess.glob.party.memb.values()) {
			Coord2d ppc;
			try {
			    ppc = m.getc();
			} catch(MCache.LoadingMap e) {
			    ppc = null;
			}
			if(ppc == null)
			    continue;
			Coord ptc = p2c(ppc);
			g.chcolor(m.col.getRed(), m.col.getGreen(), m.col.getBlue(), 255);
			drawplx(g, ptc);
			g.chcolor();
		    }
		}
	    } catch(Loading l) {}
	} else {
	    g.image(nomap, Coord.z);
	}
    }

    public boolean mousedown(Coord c, int button) {
	if(cc == null)
	    return(false);
	Gob gob = findicongob(c);
	if(gob == null)
	    mv.wdgmsg("click", rootpos().add(c), c2p(c).floor(posres), button, ui.modflags());
	else
	    mv.wdgmsg("click", rootpos().add(c), c2p(c).floor(posres), button, ui.modflags(), 0, (int)gob.id, gob.rc.floor(posres), 0, -1);
	return(true);
    }
}
