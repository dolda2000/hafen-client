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
import haven.MapFile.Marker;
import haven.MapFile.PMarker;
import haven.MapFile.SMarker;
import haven.MapFileWidget.*;
import haven.MapFileWidget.Location;
import haven.BuddyWnd.GroupSelector;

public class MapWnd extends Window {
    public final MapFileWidget view;
    public final MapView mv;
    public final MarkerList list;
    private final Widget toolbar;
    private TextEntry namesel;
    private GroupSelector colsel;
    private Button mremove;
    private Predicate<Marker> mflt = pmarkers;
    private Comparator<Marker> mcmp = namecmp;
    private List<Marker> markers = Collections.emptyList();
    private int markerseq = -1;

    private final static Predicate<Marker> pmarkers = (m -> m instanceof PMarker);
    private final static Predicate<Marker> smarkers = (m -> m instanceof SMarker);
    private final static Comparator<Marker> namecmp = ((a, b) -> a.nm.compareTo(b.nm));

    public MapWnd(MapFile file, MapView mv, Coord sz, String title) {
	super(sz, title);
	this.mv = mv;
	view = add(new MapFileWidget(file, Coord.z) {
		public boolean clickmarker(DisplayMarker mark, int button) {
		    return(MapWnd.this.clickmarker(mark.m, button));
		}
		public boolean clickloc(Location loc, int button) {
		    return(MapWnd.this.clickloc(loc, button));
		}
	    });
	recenter();
	toolbar = add(new Widget(Coord.z));
	toolbar.add(new IButton("gfx/hud/mmap/home", "", "-d", "-h") {
		{tooltip = RichText.render("Follow ($col[255,255,0]{Home})", 0);}
		public void click() {
		    recenter();
		}
	    }, 5, 5);
	toolbar.pack();
	list = add(new MarkerList(200, 0));
	resize(sz);
    }

    public void tick(double dt) {
	super.tick(dt);
	if(visible && (markerseq != view.file.markerseq)) {
	    if(view.file.lock.readLock().tryLock()) {
		try {
		    List<Marker> markers = view.file.markers.stream().filter(mflt).collect(java.util.stream.Collectors.toList());
		    markers.sort(mcmp);
		    this.markers = markers;
		} finally {
		    view.file.lock.readLock().unlock();
		}
	    }
	}
    }

    public static final Color every = new Color(255, 255, 255, 16), other = new Color(255, 255, 255, 32);
    public class MarkerList extends Listbox<Marker> {
	private final Text.Foundry fnd = CharWnd.attrf;

	public Marker listitem(int idx) {return(markers.get(idx));}
	public int listitems() {return(markers.size());}

	public MarkerList(int w, int n) {
	    super(w, n, 20);
	}

	private Function<String, Text> names = new CachedFunction<>(500, nm -> fnd.render(nm));
	protected void drawbg(GOut g) {}
	public void drawitem(GOut g, Marker mark, int idx) {
	    g.chcolor(((idx % 2) == 0)?every:other);
	    g.frect(Coord.z, g.sz);
	    if(mark instanceof PMarker)
		g.chcolor(((PMarker)mark).color);
	    else
		g.chcolor();
	    g.aimage(names.apply(mark.nm).tex(), new Coord(5, itemh / 2), 0, 0.5);
	}

	public void change(Marker mark) {
	    change2(mark);
	    if(mark != null)
		view.center(new SpecLocator(mark.seg, mark.tc));
	}

	public void change2(Marker mark) {
	    this.sel = mark;

	    if(namesel != null) {
		ui.destroy(namesel);
		namesel = null;
		if(colsel != null) {
		    ui.destroy(colsel);
		    colsel = null;
		    ui.destroy(mremove);
		    mremove = null;
		}
	    }

	    if(mark != null) {
		if(namesel == null) {
		    namesel = MapWnd.this.add(new TextEntry(190, "") {
			    {dshow = true;}
			    public void activate(String text) {
				mark.nm = text;
				view.file.update(mark);
				commit();
			    }
			}, c.x, c.y + sz.y + 10);
		}
		namesel.settext(mark.nm);
		namesel.buf.point = mark.nm.length();
		namesel.commit();
		if(mark instanceof PMarker) {
		    PMarker pm = (PMarker)mark;
		    colsel = MapWnd.this.add(new GroupSelector(0) {
			    public void changed(int group) {
				this.group = group;
				pm.color = BuddyWnd.gc[group];
				view.file.update(mark);
			    }
			}, c.x, namesel.c.y + namesel.sz.y + 10);
		    if((colsel.group = Utils.index(BuddyWnd.gc, pm.color)) < 0)
			colsel.group = 0;
		    mremove = MapWnd.this.add(new Button(190, "Remove", false) {
			    public void click() {
				view.file.remove(mark);
				change2(null);
			    }
			}, c.x, colsel.c.y + colsel.sz.y + 10);
		}
	    }
	}
    }

    public void resize(Coord sz) {
	super.resize(sz);
	toolbar.c = new Coord(0, sz.y - toolbar.sz.y);
	list.resize(list.sz.x, sz.y - 90);
	list.c = new Coord(sz.x - list.sz.x, 0);
	view.resize(new Coord(sz.x - list.sz.x, toolbar.c.y));
    }

    public void recenter() {
	view.follow(new MapLocator(mv));
    }

    public boolean keydown(KeyEvent ev) {
	if(ev.getKeyCode() == KeyEvent.VK_HOME) {
	    recenter();
	    return(true);
	}
	return(super.keydown(ev));
    }

    private static final String[] names;
    static {
	try {
	    try(java.io.BufferedReader r = new java.io.BufferedReader(new java.io.FileReader("/usr/share/dict/words"))) {
		names = r.lines().collect(java.util.stream.Collectors.toList()).toArray(new String[0]);
	    }
	} catch(java.io.IOException e) {
	    throw(new RuntimeException(e));
	}
    }
    private static String randname() {
	String ret = names[new Random().nextInt(names.length)];
	ret = ret.substring(0, 1).toUpperCase() + ret.substring(1);
	return(ret);
    }

    private static Color randcol() {
	Random rnd = new Random();
	int r = rnd.nextInt(256);
	int g = rnd.nextInt(256);
	int b = rnd.nextInt(256);
	int m = Math.max(Math.max(r, g), b);
	r = (r * 255) / m;
	g = (g * 255) / m;
	b = (b * 255) / m;
	return(new Color(r, g, b));
    }
    public boolean clickloc(Location loc, int button) {
	if(button == 3) {
	    view.file.add(new PMarker(loc.seg.id, loc.tc, randname(), randcol()));
	    return(true);
	}
	return(false);
    }

    public boolean clickmarker(Marker mark, int button) {
	if(button == 1) {
	    list.change2(mark);
	    list.display(mark);
	    return(true);
	}
	return(false);
    }
}
