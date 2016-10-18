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

import java.util.function.*;
import java.awt.image.BufferedImage;
import haven.MapFile.Segment;
import haven.MapFile.Grid;
import haven.MapFile.GridInfo;
import static haven.MCache.cmaps;

public class MapFileWidget extends Widget {
    public final MapFile file;
    public Location curloc;
    private Locator setloc;
    private boolean follow;
    private Area dext;
    private Indir<BufferedImage>[] display;

    public MapFileWidget(MapFile file, Coord sz) {
	super();
	this.file = file;
	resize(sz);
    }

    public static class Location {
	public final Segment seg;
	public final Coord tc;

	public Location(Segment seg, Coord tc) {
	    this.seg = seg; this.tc = tc;
	}
    }

    public interface Locator {
	Location locate(MapFile file) throws Loading;
    }

    public static class MapLocator implements Locator {
	public final MapView mv;

	public MapLocator(MapView mv) {this.mv = mv;}

	public Location locate(MapFile file) {
	    Coord mc = new Coord(mv.getcc()).div(MCache.tilesz);
	    if(mc == null)
		throw(new Loading("Waiting for initial location"));
	    MCache.Grid plg = mv.ui.sess.glob.map.getgrid(mc.div(cmaps));
	    GridInfo info = file.gridinfo.get(plg.id);
	    if(info == null)
		throw(new Loading("No grid info, probably coming soon"));
	    return(new Location(file.segments.get(info.seg), info.sc.mul(cmaps).add(mc.sub(plg.ul))));
	}
    }

    public void center(Location loc) {
	curloc = loc;
    }

    public void tick(double dt) {
	if(setloc != null) {
	    try {
		Location loc;
		if(!file.lock.readLock().tryLock())
		    throw(new Loading("Map file is busy"));
		try {
		    loc = setloc.locate(file);
		} finally {
		    file.lock.readLock().unlock();
		}
		center(loc);
		if(!follow)
		    setloc = null;
	    } catch(Loading l) {
	    }
	}
    }

    public void resize(Coord sz) {
	super.resize(sz);
	Coord hsz = sz.div(2);
	Area next = Area.sized((curloc == null)?Coord.z:curloc.tc.sub(hsz).div(cmaps),
			       sz.add(cmaps).sub(1, 1).div(cmaps));
	if((display == null) || !next.equals(dext)) {
	    @SuppressWarnings("unchecked")
	    Indir<BufferedImage>[] nd = new Indir[next.rsz()];
	    if(display != null) {
		for(Coord c : dext) {
		    if(next.contains(c))
			nd[next.ri(c)] = display[dext.ri(c)];
		}
	    }
	    display = nd;
	    dext = next;
	}
    }

    public void draw(GOut g) {
	Location loc = this.curloc;
	if(loc == null)
	    return;
    }

    public void center(Locator loc) {
	setloc = loc;
	follow = false;
    }

    public void follow(Locator loc) {
	setloc = loc;
	follow = true;
    }

    public class MapWindow extends Window {
	public final MapFileWidget view;

	public MapWindow(MapFile file, Coord sz) {
	    super(sz, "Map");
	    view = add(new MapFileWidget(file, sz));
	    pack();
	}

	public MapWindow(MapFile file) {
	    this(file, new Coord(500, 500));
	}
    }
}
