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
import java.util.function.*;
import java.awt.Color;
import java.awt.event.KeyEvent;
import haven.MapFile.Segment;
import haven.MapFile.DataGrid;
import haven.MapFile.Grid;
import haven.MapFile.GridInfo;
import haven.MapFile.Marker;
import haven.MapFile.PMarker;
import haven.MapFile.SMarker;
import static haven.MCache.cmaps;
import static haven.Utils.or;

public class MapFileWidget extends Widget {
    public final MapFile file;
    public Location curloc;
    private Locator setloc;
    private boolean follow;
    private Area dgext, dtext;
    private Segment dseg;
    private int dlvl;
    private int zoomlevel = 0;
    private DisplayGrid[] display;
    private Collection<DisplayMarker> markers = null;
    private int markerseq = -1;
    private UI.Grab drag;
    private boolean dragging;
    private Coord dsc, dmc;
    private boolean hmarkers = false;

    public MapFileWidget(MapFile file, Coord sz) {
	super();
	this.file = file;
    }

    public static class Location {
	public final Segment seg;
	public final Coord tc;

	public Location(Segment seg, Coord tc) {
	    Objects.requireNonNull(seg);
	    Objects.requireNonNull(tc);
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
	    Coord mc = new Coord2d(mv.getcc()).floor(MCache.tilesz);
	    if(mc == null)
		throw(new Loading("Waiting for initial location"));
	    MCache.Grid plg = mv.ui.sess.glob.map.getgrid(mc.div(cmaps));
	    GridInfo info = file.gridinfo.get(plg.id);
	    if(info == null)
		throw(new Loading("No grid info, probably coming soon"));
	    Segment seg = file.segments.get(info.seg);
	    if(seg == null)
		throw(new Loading("No segment info, probably coming soon"));
	    return(new Location(seg, info.sc.mul(cmaps).add(mc.sub(plg.ul))));
	}
    }

    public static class SpecLocator implements Locator {
	public final long seg;
	public final Coord tc;

	public SpecLocator(long seg, Coord tc) {this.seg = seg; this.tc = tc;}

	public Location locate(MapFile file) {
	    Segment seg = file.segments.get(this.seg);
	    if(seg == null)
		return(null);
	    return(new Location(seg, tc));
	}
    }

    public void center(Location loc) {
	curloc = loc;
    }

    public Location resolve(Locator loc) {
	if(!file.lock.readLock().tryLock())
	    throw(new Loading("Map file is busy"));
	try {
	    return(loc.locate(file));
	} finally {
	    file.lock.readLock().unlock();
	}
    }

    public void tick(double dt) {
	if(setloc != null) {
	    try {
		Location loc = resolve(setloc);
		center(loc);
		if(!follow)
		    setloc = null;
	    } catch(Loading l) {
	    }
	}
    }

    public static class DisplayGrid {
	public final Segment seg;
	public final Coord sc;
	public final Indir<? extends DataGrid> gref;
	private DataGrid cgrid = null;
	private Defer.Future<Tex> img = null;

	public DisplayGrid(Segment seg, Coord sc, Indir<? extends DataGrid> gref) {
	    this.seg = seg;
	    this.sc = sc;
	    this.gref = gref;
	}

	public Tex img() {
	    DataGrid grid = gref.get();
	    if(grid != cgrid) {
		if(img != null)
		    img.cancel();
		img = Defer.later(() -> new TexI(grid.render(sc.mul(cmaps))));
		cgrid = grid;
	    }
	    return((img == null)?null:img.get());
	}
    }

    public static class DisplayMarker {
	public static final Resource.Image flagbg, flagfg;
	public static final Coord flagcc;
	public final Marker m;
	public final Text tip;
	public Area hit;
	private Resource.Image img;
	private Coord cc;

	static {
	    Resource flag = Resource.local().loadwait("gfx/hud/mmap/flag");
	    flagbg = flag.layer(Resource.imgc, 1);
	    flagfg = flag.layer(Resource.imgc, 0);
	    flagcc = flag.layer(Resource.negc).cc;
	}

	public DisplayMarker(Marker marker) {
	    this.m = marker;
	    this.tip = Text.render(m.nm);
	    if(marker instanceof PMarker)
		this.hit = Area.sized(flagcc.inv(), flagbg.sz);
	}

	public void draw(GOut g, Coord c) {
	    if(m instanceof PMarker) {
		Coord ul = c.sub(flagcc);
		g.chcolor(((PMarker)m).color);
		g.image(flagfg, ul);
		g.chcolor();
		g.image(flagbg, ul);
	    } else if(m instanceof SMarker) {
		SMarker sm = (SMarker)m;
		try {
		    if(cc == null) {
			Resource res = MapFile.loadsaved(Resource.remote(), sm.res);
			img = res.layer(Resource.imgc);
			Resource.Neg neg = res.layer(Resource.negc);
			cc = (neg != null)?neg.cc:img.sz.div(2);
			if(hit == null)
			    hit = Area.sized(cc.inv(), img.sz);
		    }
		} catch(Loading l) {
		} catch(Exception e) {
		    cc = Coord.z;
		}
		if(img != null)
		    g.image(img, c.sub(cc));
	    }
	}
    }

    private void remark(Location loc, Area ext) {
	if(file.lock.readLock().tryLock()) {
	    try {
		Collection<DisplayMarker> marks = new ArrayList<>();
		for(Marker mark : file.markers) {
		    if((mark.seg == loc.seg.id) && ext.contains(mark.tc))
			marks.add(new DisplayMarker(mark));
		}
		markers = marks;
		markerseq = file.markerseq;
	    } finally {
		file.lock.readLock().unlock();
	    }
	}
    }

    private void redisplay(Location loc) {
	Coord hsz = sz.div(2);
	Coord zmaps = cmaps.mul(1 << zoomlevel);
	Area next = Area.sized(loc.tc.sub(hsz.mul(1 << zoomlevel)).div(zmaps),
			       sz.add(cmaps).sub(1, 1).div(cmaps).add(1, 1));
	if((display == null) || (loc.seg != dseg) || (zoomlevel != dlvl) || !next.equals(dgext)) {
	    DisplayGrid[] nd = new DisplayGrid[next.rsz()];
	    if((display != null) && (loc.seg == dseg) && (zoomlevel == dlvl)) {
		for(Coord c : dgext) {
		    if(next.contains(c))
			nd[next.ri(c)] = display[dgext.ri(c)];
		}
	    }
	    display = nd;
	    dseg = loc.seg;
	    dlvl = zoomlevel;
	    dgext = next;
	    dtext = Area.sized(next.ul.mul(zmaps), next.sz().mul(zmaps));
	    markers = null;
	}
    }

    public Coord xlate(Location loc) {
	Location curloc = this.curloc;
	if((curloc == null) || (curloc.seg != loc.seg))
	    return(null);
	return(loc.tc.sub(curloc.tc).div(1 << dlvl).add(sz.div(2)));
    }

    public void draw(GOut g) {
	Location loc = this.curloc;
	if(loc == null)
	    return;
	Coord hsz = sz.div(2);
	redisplay(loc);
	if(file.lock.readLock().tryLock()) {
	    try {
		for(Coord c : dgext) {
		    if(display[dgext.ri(c)] == null)
			display[dgext.ri(c)] = new DisplayGrid(loc.seg, c, loc.seg.grid(dlvl, c.mul(1 << dlvl)));
		}
	    } finally {
		file.lock.readLock().unlock();
	    }
	}
	Coord zmaps = cmaps.mul(1 << dlvl);
	for(Coord c : dgext) {
	    Tex img;
	    try {
		DisplayGrid disp = display[dgext.ri(c)];
		if((disp == null) || ((img = disp.img()) == null))
		    continue;
	    } catch(Loading l) {
		continue;
	    }
	    Coord ul = c.mul(cmaps).sub(loc.tc.div(1 << dlvl)).add(hsz);
	    g.image(img, ul);
	}
	if(!hmarkers) {
	    if((markers == null) || (file.markerseq != markerseq))
		remark(loc, dtext.margin(cmaps.mul(1 << dlvl)));
	    if(markers != null) {
		for(DisplayMarker mark : markers)
		    mark.draw(g, mark.m.tc.sub(loc.tc).div(1 << dlvl).add(hsz));
	    }
	}
    }

    private static boolean hascomplete(DisplayGrid[] disp, Area dext, Coord c) {
	DisplayGrid dg = disp[dext.ri(c)];
	if(dg == null)
	    return(false);
	return(dg.gref.get() != null);
    }

    private boolean allowzoomout() {
	DisplayGrid[] disp = this.display;
	Area dext = this.dgext;
	try {
	    for(int x = dext.ul.x; x < dext.br.x; x++) {
		if(hascomplete(disp, dext, new Coord(x, dext.ul.y)) ||
		   hascomplete(disp, dext, new Coord(x, dext.br.y - 1)))
		    return(true);
	    }
	    for(int y = dext.ul.y; y < dext.br.y; y++) {
		if(hascomplete(disp, dext, new Coord(dext.ul.x, y)) ||
		   hascomplete(disp, dext, new Coord(dext.br.x - 1, y)))
		    return(true);
	    }
	} catch(Loading l) {
	    return(false);
	}
	return(false);
    }

    public void center(Locator loc) {
	setloc = loc;
	follow = false;
    }

    public void follow(Locator loc) {
	setloc = loc;
	follow = true;
    }

    public boolean clickloc(Location loc, int button) {
	return(false);
    }

    public boolean clickmarker(DisplayMarker mark, int button) {
	return(false);
    }

    private DisplayMarker markerat(Coord tc) {
	if(!hmarkers && (markers != null)) {
	    for(DisplayMarker mark : markers) {
		if((mark.hit != null) && mark.hit.contains(tc.sub(mark.m.tc).div(1 << dlvl)))
		    return(mark);
	    }
	}
	return(null);
    }

    public boolean mousedown(Coord c, int button) {
	Coord tc = null;
	if(curloc != null)
	    tc = c.sub(sz.div(2)).mul(1 << dlvl).add(curloc.tc);
	if(tc != null) {
	    DisplayMarker mark = markerat(tc);
	    if((mark != null) && clickmarker(mark, button))
		return(true);
	    if(clickloc(new Location(curloc.seg, tc), button))
		return(true);
	}
	if(button == 1) {
	    Location loc = curloc;
	    if((drag == null) && (loc != null)) {
		drag = ui.grabmouse(this);
		dsc = c;
		dmc = loc.tc;
		dragging = false;
	    }
	    return(true);
	}
	return(super.mousedown(c, button));
    }

    public boolean globtype(char key, java.awt.event.KeyEvent ev) {
	if((key == 'm') && tvisible()) {
	    hmarkers = !hmarkers;
	    return(true);
	}
	return(super.globtype(key, ev));
    }

    public void mousemove(Coord c) {
	if(drag != null) {
	    if(dragging) {
		setloc = null;
		follow = false;
		curloc = new Location(curloc.seg, dmc.add(dsc.sub(c).mul(1 << dlvl)));
	    } else if(c.dist(dsc) > 5) {
		dragging = true;
	    }
	}
	super.mousemove(c);
    }

    public boolean mouseup(Coord c, int button) {
	if((drag != null) && (button == 1)) {
	    drag.remove();
	    drag = null;
	}
	return(super.mouseup(c, button));
    }

    public boolean mousewheel(Coord c, int amount) {
	if(amount > 0) {
	    if(allowzoomout())
		zoomlevel = Math.min(zoomlevel + 1, dlvl + 1);
	} else {
	    zoomlevel = Math.max(zoomlevel - 1, 0);
	}
	return(true);
    }

    public Object tooltip(Coord c, Widget prev) {
	if(curloc != null) {
	    Coord tc = c.sub(sz.div(2)).mul(1 << dlvl).add(curloc.tc);
	    DisplayMarker mark = markerat(tc);
	    if(mark != null) {
		return(mark.tip);
	    }
	}
	return(super.tooltip(c, prev));
    }
}
