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
    public static final Resource markcurs = Resource.local().loadwait("gfx/hud/curs/flag");
    public final MapFileWidget view;
    public final MapView mv;
    public final MarkerList list;
    private final Widget toolbar;
    private final Frame viewf, listf;
    private TextEntry namesel;
    private GroupSelector colsel;
    private Button mremove;
    private Predicate<Marker> mflt = pmarkers;
    private Comparator<Marker> mcmp = namecmp;
    private List<Marker> markers = Collections.emptyList();
    private int markerseq = -1;
    private boolean domark = false;

    private final static Predicate<Marker> pmarkers = (m -> m instanceof PMarker);
    private final static Predicate<Marker> smarkers = (m -> m instanceof SMarker);
    private final static Comparator<Marker> namecmp = ((a, b) -> a.nm.compareTo(b.nm));

    public MapWnd(MapFile file, MapView mv, Coord sz, String title) {
	super(sz, title, true);
	this.mv = mv;
	viewf = add(new Frame(Coord.z, true));
	view = viewf.add(new View(file));
	recenter();
	toolbar = add(new Widget(Coord.z));
	toolbar.add(new IButton("gfx/hud/mmap/home", "", "-d", "-h") {
		{tooltip = RichText.render("Follow ($col[255,255,0]{Home})", 0);}
		public void click() {
		    recenter();
		}
	    }, 5, 5);
	toolbar.add(new IButton("gfx/hud/mmap/mark", "", "-d", "-h") {
		{tooltip = RichText.render("Add marker", 0);}
		public void click() {
		    domark = true;
		}
	    }, 36, 5);
	toolbar.pack();
	listf = add(new Frame(new Coord(200, 200), false));
	list = listf.add(new MarkerList(listf.inner().x, 0));
	resize(sz);
    }

    private class View extends MapFileWidget {
	View(MapFile file) {
	    super(file, Coord.z);
	}

	public boolean clickmarker(DisplayMarker mark, int button) {
	    if(button == 1) {
		list.change2(mark.m);
		list.display(mark.m);
		return(true);
	    }
	    return(false);
	}

	public boolean clickloc(Location loc, int button) {
	    if(domark && (button == 1)) {
		Marker nm = new PMarker(loc.seg.id, loc.tc, "New marker", BuddyWnd.gc[new Random().nextInt(BuddyWnd.gc.length)]);
		file.add(nm);
		list.change2(nm);
		list.display(nm);
		domark = false;
		return(true);
	    }
	    return(false);
	}

	public boolean mousedown(Coord c, int button) {
	    if(domark && (button == 3)) {
		domark = false;
		return(true);
	    }
	    return(super.mousedown(c, button));
	}

	public void draw(GOut g) {
	    g.chcolor(0, 0, 0, 128);
	    g.frect(Coord.z, sz);
	    g.chcolor();
	    super.draw(g);
	}

	public Resource getcurs(Coord c) {
	    if(domark)
		return(markcurs);
	    return(super.getcurs(c));
	}
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
			}, listf.c.x, listf.c.y + listf.sz.y + 10);
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
			}, listf.c.x, namesel.c.y + namesel.sz.y + 10);
		    if((colsel.group = Utils.index(BuddyWnd.gc, pm.color)) < 0)
			colsel.group = 0;
		    mremove = MapWnd.this.add(new Button(190, "Remove", false) {
			    public void click() {
				view.file.remove(mark);
				change2(null);
			    }
			}, listf.c.x, colsel.c.y + colsel.sz.y + 10);
		}
	    }
	}
    }

    public void resize(Coord sz) {
	super.resize(sz);
	toolbar.c = new Coord(0, sz.y - toolbar.sz.y);
	listf.resize(listf.sz.x, sz.y - 90);
	listf.c = new Coord(sz.x - listf.sz.x, 0);
	list.resize(listf.inner());
	viewf.resize(new Coord(sz.x - listf.sz.x - 10, toolbar.c.y));
	view.resize(viewf.inner());
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
}
