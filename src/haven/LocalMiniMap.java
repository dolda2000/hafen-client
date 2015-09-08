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

public class LocalMiniMap extends Widget {
	public static final Resource plarrow = Resource.local().loadwait("gfx/hud/mmap/plarrow");

    public final MapView mv;
	private final MinimapCache cache;
    private Coord cc = null;
    private MinimapTile cur = null;
    private Coord off = Coord.z;
    private Coord doff;
    private UI.Grab grab;
    private boolean showradius;

    public LocalMiniMap(Coord sz, MapView mv) {
	super(sz);
	this.mv = mv;
	this.cache = new MinimapCache(new MinimapRenderer(mv.ui.sess.glob.map));
    this.showradius = Config.getMinimapRadiusEnabled();
    }
    
    public Coord p2c(Coord pc) {
	return(pc.div(tilesz).sub(cc).add(sz.div(2)));
    }

    public Coord c2p(Coord c) {
	return(c.sub(sz.div(2)).add(cc).mul(tilesz).add(tilesz.div(2)));
    }

    public void drawicons(GOut g) {
	OCache oc = ui.sess.glob.oc;
	synchronized(oc) {
	    for(Gob gob : oc) {
        try {
            GobIcon icon = gob.getattr(GobIcon.class);
            if (icon == null) {
                try {
                    if (MinimapIcons.isVisible(gob)) {
                        String resname = MinimapIcons.getIconResourceName(gob);
                        icon = new GobIcon(gob, Resource.local().load(resname), true);
                        gob.setattr(icon);
                    }
                } catch (Exception e){
                    continue;
                };
            } else if (icon.custom) {
                if (!MinimapIcons.isVisible(gob))
                    gob.delattr(GobIcon.class);
            }
            if (icon != null) {
                Coord gc = p2c(gob.rc).sub(off);
                Tex tex = icon.tex();
                g.image(tex, gc.sub(tex.sz().div(2)));
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
            }
        }
    }

	try {
		synchronized(ui.sess.glob.party.memb) {
		    for(Party.Member m : ui.sess.glob.party.memb.values()) {
			Coord ptc;
			try {
			    ptc = m.getc();
			} catch(MCache.LoadingMap e) {
			    ptc = null;
			}
			if(ptc == null)
			    continue;
			ptc = p2c(ptc).sub(off);
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
                Coord rc = ptc.add((-500 / tilesz.x), (-500 / tilesz.y));
                Coord rs = new Coord((1000 / tilesz.x), (1000 / tilesz.y));
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
	else
	    mv.wdgmsg("click", rootpos().add(c), c2p(coff), button, ui.modflags(), 0, (int)gob.id, gob.rc, 0, -1);
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
    }

    public void setOffset(Coord value) {
        off = value;
    }

    public void toggleRadius() {
        showradius = !showradius;
        Config.setMinimapRadiusEnabled(showradius);
    }
}
