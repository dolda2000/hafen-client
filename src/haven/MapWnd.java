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
import java.io.*;
import java.awt.Color;
import java.awt.event.KeyEvent;
import haven.MapFile.Marker;
import haven.MapFile.PMarker;
import haven.MapFile.SMarker;
import haven.MiniMap.*;
import haven.BuddyWnd.GroupSelector;
import static haven.MCache.tilesz;
import static haven.MCache.cmaps;
import javax.swing.JFileChooser;
import javax.swing.filechooser.*;

public class MapWnd extends Window implements Console.Directory {
    public static final Resource markcurs = Resource.local().loadwait("gfx/hud/curs/flag");
    public final MapFile file;
    public final MiniMap view;
    public final MapView mv;
    public final Toolbox tool;
    public boolean hmarkers = false;
    private final Locator player;
    private final Widget toolbar;
    private final Frame viewf;
    private GroupSelector colsel;
    private Button mremove;
    private Predicate<Marker> mflt = pmarkers;
    private Comparator<Marker> mcmp = namecmp;
    private List<Marker> markers = Collections.emptyList();
    private int markerseq = -1;
    private boolean domark = false;
    private final Collection<Runnable> deferred = new LinkedList<>();

    private final static Predicate<Marker> pmarkers = (m -> m instanceof PMarker);
    private final static Predicate<Marker> smarkers = (m -> m instanceof SMarker);
    private final static Comparator<Marker> namecmp = ((a, b) -> a.nm.compareTo(b.nm));

    public static final KeyBinding kb_home = KeyBinding.get("mapwnd/home", KeyMatch.forcode(KeyEvent.VK_HOME, 0));
    public static final KeyBinding kb_mark = KeyBinding.get("mapwnd/mark", KeyMatch.nil);
    public static final KeyBinding kb_hmark = KeyBinding.get("mapwnd/hmark", KeyMatch.forchar('M', KeyMatch.C));
    public static final KeyBinding kb_compact = KeyBinding.get("mapwnd/compact", KeyMatch.forchar('A', KeyMatch.M));
    public MapWnd(MapFile file, MapView mv, Coord sz, String title) {
	super(sz, title, true);
	this.file = file;
	this.mv = mv;
	this.player = new MapLocator(mv);
	viewf = add(new ViewFrame());
	view = viewf.add(new View(file));
	recenter();
	toolbar = add(new Widget(Coord.z));
	toolbar.add(new Img(Resource.loadtex("gfx/hud/mmap/fgwdg")) {
		public boolean mousedown(Coord c, int button) {
		    if((button == 1) && checkhit(c)) {
			MapWnd.this.drag(parentpos(MapWnd.this, c));
			return(true);
		    }
		    return(super.mousedown(c, button));
		}
	    }, Coord.z);
	toolbar.add(new IButton("gfx/hud/mmap/home", "", "-d", "-h") {
		{settip("Follow"); setgkey(kb_home);}
		public void click() {
		    recenter();
		}
	    }, Coord.z);
	toolbar.add(new ICheckBox("gfx/hud/mmap/mark", "", "-d", "-h", "-dh"), Coord.z)
	    .state(() -> domark).set(a -> domark = a)
	    .settip("Add marker").setgkey(kb_mark);
	toolbar.add(new ICheckBox("gfx/hud/mmap/hmark", "", "-d", "-h", "-dh"))
	    .state(() -> hmarkers).set(a -> hmarkers = a)
	    .settip("Hide markers").setgkey(kb_hmark);
	toolbar.add(new ICheckBox("gfx/hud/mmap/wnd", "", "-d", "-h", "-dh"))
	    .state(() -> decohide()).set(a -> {
		    compact(a);
		    Utils.setprefb("compact-map", a);
		})
	    .settip("Compact mode").setgkey(kb_compact);
	toolbar.pack();
	tool = add(new Toolbox());;
	compact(Utils.getprefb("compact-map", false));
	resize(sz);
    }

    private class ViewFrame extends Frame {
	Coord sc = Coord.z;

	ViewFrame() {
	    super(Coord.z, true);
	}

	public void resize(Coord sz) {
	    super.resize(sz);
	    sc = sz.sub(box.bisz()).add(box.btloff()).sub(sizer.sz());
	}

	public void draw(GOut g) {
	    super.draw(g);
	    if(decohide())
		g.image(sizer, sc);
	}

	private UI.Grab drag;
	private Coord dragc;
	public boolean mousedown(Coord c, int button) {
	    Coord cc = c.sub(sc);
	    if((button == 1) && decohide() && (cc.x < sizer.sz().x) && (cc.y < sizer.sz().y) && (cc.y >= sizer.sz().y - UI.scale(25) + (sizer.sz().x - cc.x))) {
		if(drag == null) {
		    drag = ui.grabmouse(this);
		    dragc = asz.sub(parentpos(MapWnd.this, c));
		    return(true);
		}
	    }
	    if((button == 1) && (checkhit(c) || ui.modshift)) {
		MapWnd.this.drag(parentpos(MapWnd.this, c));
		return(true);
	    }
	    return(super.mousedown(c, button));
	}

	public void mousemove(Coord c) {
	    if(drag != null) {
		Coord nsz = parentpos(MapWnd.this, c).add(dragc);
		nsz.x = Math.max(nsz.x, UI.scale(150));
		nsz.y = Math.max(nsz.y, UI.scale(150));
		MapWnd.this.resize(nsz);
	    }
	    super.mousemove(c);
	}

	public boolean mouseup(Coord c, int button) {
	    if((button == 1) && (drag != null)) {
		drag.remove();
		drag = null;
		return(true);
	    }
	    return(super.mouseup(c, button));
	}
    }

    private static final int btnw = UI.scale(95);
    public class Toolbox extends Widget {
	public final MarkerList list;
	private final Frame listf;
	private final Button pmbtn, smbtn, mebtn, mibtn;
	private TextEntry namesel;

	private Toolbox() {
	    super(UI.scale(200, 200));
	    listf = add(new Frame(UI.scale(new Coord(200, 200)), false), 0, 0);
	    list = listf.add(new MarkerList(listf.inner().x, 0), 0, 0);
	    pmbtn = add(new Button(btnw, "Placed", false) {
		    public void click() {
			mflt = pmarkers;
			markerseq = -1;
		    }
		});
	    smbtn = add(new Button(btnw, "Natural", false) {
		    public void click() {
			mflt = smarkers;
			markerseq = -1;
		    }
		});
	    mebtn = add(new Button(btnw, "Export...", false) {
		    public void click() {
			exportmap();
		    }
		});
	    mibtn = add(new Button(btnw, "Import...", false) {
		    public void click() {
			importmap();
		    }
		});
	}

	public void resize(int h) {
	    super.resize(new Coord(sz.x, h));
	    listf.resize(listf.sz.x, sz.y - UI.scale(180));
	    listf.c = new Coord(sz.x - listf.sz.x, 0);
	    list.resize(listf.inner());
	    mebtn.c = new Coord(0, sz.y - mebtn.sz.y);
	    mibtn.c = new Coord(sz.x - btnw, sz.y - mibtn.sz.y);
	    pmbtn.c = new Coord(0, mebtn.c.y - UI.scale(30) - pmbtn.sz.y);
	    smbtn.c = new Coord(sz.x - btnw, mibtn.c.y - UI.scale(30) - smbtn.sz.y);
	    if(namesel != null) {
		namesel.c = listf.c.add(0, listf.sz.y + UI.scale(10));
		if(colsel != null) {
		    colsel.c = namesel.c.add(0, namesel.sz.y + UI.scale(10));
		    mremove.c = colsel.c.add(0, colsel.sz.y + UI.scale(10));
		}
	    }
	}
    }

    private class View extends MiniMap {
	View(MapFile file) {
	    super(file);
	}

	public void drawmarkers(GOut g) {
	    if(!hmarkers)
		super.drawmarkers(g);
	}

	public boolean clickmarker(DisplayMarker mark, Location loc, int button, boolean press) {
	    if((button == 1) && !press) {
		focus(mark.m);
		return(true);
	    }
	    return(false);
	}

	public boolean clickicon(DisplayIcon icon, Location loc, int button, boolean press) {
	    if(!press) {
		mvclick(mv, null, loc, icon.gob, button);
		return(true);
	    }
	    return(false);
	}

	public boolean clickloc(Location loc, int button, boolean press) {
	    if(domark && (button == 1) && !press) {
		Marker nm = new PMarker(loc.seg.id, loc.tc, "New marker", BuddyWnd.gc[new Random().nextInt(BuddyWnd.gc.length)]);
		file.add(nm);
		focus(nm);
		domark = false;
		return(true);
	    }
	    if(!press && (sessloc != null) && (loc.seg == sessloc.seg)) {
		mvclick(mv, null, loc, null, button);
		return(true);
	    }
	    return(false);
	}

	public boolean mousedown(Coord c, int button) {
	    if(domark && (button == 3)) {
		domark = false;
		return(true);
	    }
	    super.mousedown(c, button);
	    return(true);
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
	synchronized(deferred) {
	    for(Iterator<Runnable> i = deferred.iterator(); i.hasNext();) {
		Runnable task = i.next();
		try {
		    task.run();
		} catch(Loading l) {
		    continue;
		}
		i.remove();
	    }
	}
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

    public static final Color every = new Color(255, 255, 255, 16), other = new Color(255, 255, 255, 32), found = new Color(255, 255, 0, 32);
    public class MarkerList extends Searchbox<Marker> {
	private final Text.Foundry fnd = CharWnd.attrf;

	public Marker listitem(int idx) {return(markers.get(idx));}
	public int listitems() {return(markers.size());}
	public boolean searchmatch(int idx, String txt) {return(markers.get(idx).nm.toLowerCase().indexOf(txt.toLowerCase()) >= 0);}

	public MarkerList(int w, int n) {
	    super(w, n, UI.scale(20));
	}

	private Function<String, Text> names = new CachedFunction<>(500, nm -> fnd.render(nm));
	protected void drawbg(GOut g) {}
	public void drawitem(GOut g, Marker mark, int idx) {
	    if(soughtitem(idx)) {
		g.chcolor(found);
		g.frect(Coord.z, g.sz());
	    }
	    g.chcolor(((idx % 2) == 0)?every:other);
	    g.frect(Coord.z, g.sz());
	    if(mark instanceof PMarker)
		g.chcolor(((PMarker)mark).color);
	    else
		g.chcolor();
	    g.aimage(names.apply(mark.nm).tex(), new Coord(UI.scale(5), itemh / 2), 0, 0.5);
	}

	public void change(Marker mark) {
	    change2(mark);
	    if(mark != null)
		view.center(new SpecLocator(mark.seg, mark.tc));
	}

	public void change2(Marker mark) {
	    this.sel = mark;

	    if(tool.namesel != null) {
		ui.destroy(tool.namesel);
		tool.namesel = null;
		if(colsel != null) {
		    ui.destroy(colsel);
		    colsel = null;
		    ui.destroy(mremove);
		    mremove = null;
		}
	    }

	    if(mark != null) {
		if(tool.namesel == null) {
		    tool.namesel = tool.add(new TextEntry(UI.scale(200), "") {
			    {dshow = true;}
			    public void activate(String text) {
				mark.nm = text;
				view.file.update(mark);
				commit();
				change2(null);
			    }
			});
		}
		tool.namesel.settext(mark.nm);
		tool.namesel.buf.point = mark.nm.length();
		tool.namesel.commit();
		if(mark instanceof PMarker) {
		    PMarker pm = (PMarker)mark;
		    colsel = tool.add(new GroupSelector(Math.max(0, Utils.index(BuddyWnd.gc, pm.color))) {
			    public void changed(int group) {
				pm.color = BuddyWnd.gc[group];
				view.file.update(mark);
			    }
			});
		    mremove = tool.add(new Button(UI.scale(200), "Remove", false) {
			    public void click() {
				view.file.remove(mark);
				change2(null);
			    }
			});
		}
		MapWnd.this.resize(asz);
	    }
	}
    }

    public void resize(Coord sz) {
	super.resize(sz);
	tool.resize(sz.y);
	if(!decohide()) {
	    tool.c = new Coord(sz.x - tool.sz.x, 0);
	    viewf.resize(tool.pos("bl").subs(10, 0));
	} else {
	    viewf.resize(sz);
	    tool.c = viewf.pos("ur").adds(10, 0);
	}
	view.resize(viewf.inner());
	toolbar.c = viewf.c.add(0, viewf.sz.y - toolbar.sz.y).add(UI.scale(2), UI.scale(-2));
    }

    public void compact(boolean a) {
	tool.show(!a);
	if(a)
	    delfocusable(tool);
	else
	    newfocusable(tool);
	decohide(a);
	pack();
    }

    public void recenter() {
	view.follow(player);
    }

    public void focus(Marker m) {
	tool.list.change2(m);
	tool.list.display(m);
    }

    protected void drawframe(GOut g) {
	g.image(sizer, ctl.add(csz).sub(sizer.sz()));
	super.drawframe(g);
    }

    private UI.Grab drag;
    private Coord dragc;
    public boolean mousedown(Coord c, int button) {
	Coord cc = c.sub(ctl);
	if((button == 1) && (cc.x < csz.x) && (cc.y < csz.y) && (cc.y >= csz.y - UI.scale(25) + (csz.x - cc.x))) {
	    if(drag == null) {
		drag = ui.grabmouse(this);
		dragc = asz.sub(c);
		return(true);
	    }
	}
	return(super.mousedown(c, button));
    }

    public void mousemove(Coord c) {
	if(drag != null) {
	    Coord nsz = c.add(dragc);
	    nsz.x = Math.max(nsz.x, UI.scale(350));
	    nsz.y = Math.max(nsz.y, UI.scale(150));
	    resize(nsz);
	}
	super.mousemove(c);
    }

    public boolean mouseup(Coord c, int button) {
	if((button == 1) && (drag != null)) {
	    drag.remove();
	    drag = null;
	    return(true);
	}
	return(super.mouseup(c, button));
    }

    public void markobj(long gobid, long oid, Indir<Resource> resid, String nm) {
	synchronized(deferred) {
	    deferred.add(new Runnable() {
		    double f = 0;
		    public void run() {
			Resource res = resid.get();
			String rnm = nm;
			if(rnm == null) {
			    Resource.Tooltip tt = res.layer(Resource.tooltip);
			    if(tt == null)
				return;
			    rnm = tt.t;
			}
			double now = Utils.rtime();
			if(f == 0)
			    f = now;
			Gob gob = ui.sess.glob.oc.getgob(gobid);
			if(gob == null) {
			    if(now - f < 1.0)
				throw(new Loading());
			    return;
			}
			Coord tc = gob.rc.floor(tilesz);
			MCache.Grid obg = ui.sess.glob.map.getgrid(tc.div(cmaps));
			if(!view.file.lock.writeLock().tryLock())
			    throw(new Loading());
			try {
			    MapFile.GridInfo info = view.file.gridinfo.get(obg.id);
			    if(info == null)
				throw(new Loading());
			    Coord sc = tc.add(info.sc.sub(obg.gc).mul(cmaps));
			    SMarker prev = view.file.smarkers.get(oid);
			    if(prev == null) {
				view.file.add(new SMarker(info.seg, sc, rnm, oid, new Resource.Spec(Resource.remote(), res.name, res.ver)));
			    } else {
				if((prev.seg != info.seg) || !prev.tc.equals(sc)) {
				    prev.seg = info.seg;
				    prev.tc = sc;
				    view.file.update(prev);
				}
			    }
			} finally {
			    view.file.lock.writeLock().unlock();
			}
		    }
		});
	}
    }

    public static class ExportWindow extends Window implements MapFile.ExportStatus {
	private Thread th;
	private volatile String prog = "Exporting map...";

	public ExportWindow() {
	    super(UI.scale(new Coord(300, 65)), "Exporting map...", true);
	    adda(new Button(UI.scale(100), "Cancel", false, this::cancel), asz.x / 2, UI.scale(40), 0.5, 0.0);
	}

	public void run(Thread th) {
	    (this.th = th).start();
	}

	public void cdraw(GOut g) {
	    g.text(prog, UI.scale(new Coord(10, 10)));
	}

	public void cancel() {
	    th.interrupt();
	}

	public void tick(double dt) {
	    if(!th.isAlive())
		destroy();
	}

	public void grid(int cs, int ns, int cg, int ng) {
	    this.prog = String.format("Exporting map cut %,d/%,d in segment %,d/%,d", cg, ng, cs, ns);
	}

	public void mark(int cm, int nm) {
	    this.prog = String.format("Exporting marker", cm, nm);
	}
    }

    public static class ImportWindow extends Window {
	private Thread th;
	private volatile String prog = "Initializing";
	private double sprog = -1;

	public ImportWindow() {
	    super(UI.scale(new Coord(300, 65)), "Importing map...", true);
	    adda(new Button(UI.scale(100), "Cancel", false, this::cancel), asz.x / 2, UI.scale(40), 0.5, 0.0);
	}

	public void run(Thread th) {
	    (this.th = th).start();
	}

	public void cdraw(GOut g) {
	    String prog = this.prog;
	    if(sprog >= 0)
		prog = String.format("%s: %d%%", prog, (int)Math.floor(sprog * 100));
	    else
		prog = prog + "...";
	    g.text(prog, UI.scale(new Coord(10, 10)));
	}

	public void cancel() {
	    th.interrupt();
	}

	public void tick(double dt) {
	    if(!th.isAlive())
		destroy();
	}

	public void prog(String prog) {
	    this.prog = prog;
	    this.sprog = -1;
	}

	public void sprog(double sprog) {
	    this.sprog = sprog;
	}
    }

    public void exportmap(File path) {
	GameUI gui = getparent(GameUI.class);
	ExportWindow prog = new ExportWindow();
	Thread th = new HackThread(() -> {
		try {
		    try(OutputStream out = new BufferedOutputStream(new FileOutputStream(path))) {
			file.export(out, MapFile.ExportFilter.all, prog);
		    }
		} catch(IOException e) {
		    e.printStackTrace(Debug.log);
		    gui.error("Unexpected error occurred when exporting map.");
		} catch(InterruptedException e) {
		}
	}, "Mapfile exporter");
	prog.run(th);
	gui.adda(prog, gui.sz.div(2), 0.5, 1.0);
    }

    public void importmap(File path) {
	GameUI gui = getparent(GameUI.class);
	ImportWindow prog = new ImportWindow();
	Thread th = new HackThread(() -> {
		long size = path.length();
		class Updater extends CountingInputStream {
		    Updater(InputStream bk) {super(bk);}

		    protected void update(long val) {
			super.update(val);
			prog.sprog((double)pos / (double)size);
		    }
		}
		try {
		    prog.prog("Validating map data");
		    try(InputStream in = new Updater(new FileInputStream(path))) {
			file.reimport(in, MapFile.ImportFilter.readonly);
		    }
		    prog.prog("Importing map data");
		    try(InputStream in = new Updater(new FileInputStream(path))) {
			file.reimport(in, MapFile.ImportFilter.all);
		    }
		} catch(InterruptedException e) {
		} catch(Exception e) {
		    e.printStackTrace(Debug.log);
		    gui.error("Could not import map: " + e.getMessage());
		}
	}, "Mapfile importer");
	prog.run(th);
	gui.adda(prog, gui.sz.div(2), 0.5, 1.0);
    }

    public void exportmap() {
	java.awt.EventQueue.invokeLater(() -> {
		JFileChooser fc = new JFileChooser();
		fc.setFileFilter(new FileNameExtensionFilter("Exported Haven map data", "hmap"));
		if(fc.showSaveDialog(null) != JFileChooser.APPROVE_OPTION)
		    return;
		File path = fc.getSelectedFile();
		if(path.getName().indexOf('.') < 0)
		    path = new File(path.toString() + ".hmap");
		exportmap(path);
	    });
    }

    public void importmap() {
	java.awt.EventQueue.invokeLater(() -> {
		JFileChooser fc = new JFileChooser();
		fc.setFileFilter(new FileNameExtensionFilter("Exported Haven map data", "hmap"));
		if(fc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
		    return;
		importmap(fc.getSelectedFile());
	    });
    }

    private Map<String, Console.Command> cmdmap = new TreeMap<String, Console.Command>();
    {
	cmdmap.put("exportmap", new Console.Command() {
		public void run(Console cons, String[] args) {
		    if(args.length > 1)
			exportmap(new File(args[1]));
		    else
			exportmap();
		}
	    });
	cmdmap.put("importmap", new Console.Command() {
		public void run(Console cons, String[] args) {
		    if(args.length > 1)
			importmap(new File(args[1]));
		    else
			importmap();
		}
	    });
    }
    public Map<String, Console.Command> findcmds() {
	return(cmdmap);
    }
}
