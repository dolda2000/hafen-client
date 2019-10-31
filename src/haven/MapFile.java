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
import java.util.concurrent.locks.*;
import java.io.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import haven.Defer.Future;
import static haven.MCache.cmaps;

public class MapFile {
    public static boolean debug = false;
    public final ResCache store;
    public final String filename;
    public final Collection<Long> knownsegs = new HashSet<>();
    public final Collection<Marker> markers = new ArrayList<>();
    public final Map<Long, SMarker> smarkers = new HashMap<>();
    public int markerseq = 0;
    public final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public MapFile(ResCache store, String filename) {
	this.store = store;
	this.filename = filename;
    }

    private void checklock() {
	if((lock.getReadHoldCount() == 0) && !lock.isWriteLockedByCurrentThread())
	    throw(new IllegalMonitorStateException());
    }

    private String mangle(String datum) {
	StringBuilder buf = new StringBuilder();
	buf.append("map/");
	if(!filename.equals("")) {
	    buf.append(filename);
	    buf.append('/');
	}
	buf.append(datum);
	return(buf.toString());
    }
    private InputStream sfetch(String ctl, Object... args) throws IOException {
	return(store.fetch(mangle(String.format(ctl, args))));
    }
    private OutputStream sstore(String ctl, Object... args) throws IOException {
	return(store.store(mangle(String.format(ctl, args))));
    }

    public static MapFile load(ResCache store, String filename) {
	MapFile file = new MapFile(store, filename);
	InputStream fp;
	try {
	    fp = file.sfetch("index");
	} catch(FileNotFoundException e) {
	    return(file);
	} catch(IOException e) {
	    return(null);
	}
	try(StreamMessage data = new StreamMessage(fp)) {
	    int ver = data.uint8();
	    if(ver == 1) {
		for(int i = 0, no = data.int32(); i < no; i++)
		    file.knownsegs.add(data.int64());
		for(int i = 0, no = data.int32(); i < no; i++) {
		    Marker mark = loadmarker(data);
		    file.markers.add(mark);
		    if(mark instanceof SMarker)
			file.smarkers.put(((SMarker)mark).oid, (SMarker)mark);
		}
	    } else {
		Debug.log.printf("mapfile warning: unknown mapfile index version: %d\n", ver);
		return(null);
	    }
	} catch(Message.BinError e) {
	    Debug.log.printf("mapfile warning: error when loading index: %s\n", e);
	    return(null);
	}
	return(file);
    }

    private void save() {
	checklock();
	OutputStream fp;
	try {
	    fp = sstore("index");
	} catch(IOException e) {
	    throw(new StreamMessage.IOError(e));
	}
	try(StreamMessage out = new StreamMessage(fp)) {
	    out.adduint8(1);
	    out.addint32(knownsegs.size());
	    for(Long seg : knownsegs)
		out.addint64(seg);
	    out.addint32(markers.size());
	    for(Marker mark : markers)
		savemarker(out, mark);
	}
    }

    public void defersave() {
	synchronized(procmon) {
	    gdirty = true;
	    process();
	}
    }

    public static class GridInfo {
	public final long id, seg;
	public final Coord sc;

	public GridInfo(long id, long seg, Coord sc) {
	    this.id = id; this.seg = seg; this.sc = sc;
	}
    }

    public final BackCache<Long, GridInfo> gridinfo = new BackCache<>(100, id -> {
	    checklock();
	    InputStream fp;
	    try {
		fp = sfetch("gi-%x", id);
	    } catch(IOException e) {
		return(null);
	    }
	    try(StreamMessage data = new StreamMessage(fp)) {
		int ver = data.uint8();
		if(ver == 1) {
		    return(new GridInfo(data.int64(), data.int64(), data.coord()));
		} else {
		    throw(new Message.FormatError("Unknown gridinfo version: " + ver));
		}
	    } catch(Message.BinError e) {
		Debug.log.printf("mapfile warning: error when loading gridinfo for %x: %s\n", id, e);
		return(null);
	    }
	}, (id, info) -> {
	    checklock();
	    OutputStream fp;
	    try {
		fp = sstore("gi-%x", info.id);
	    } catch(IOException e) {
		throw(new StreamMessage.IOError(e));
	    }
	    try(StreamMessage out = new StreamMessage(fp)) {
		out.adduint8(1);
		out.addint64(info.id);
		out.addint64(info.seg);
		out.addcoord(info.sc);
	    }
	});

    private static Runnable locked(Runnable r, Lock lock) {
	return(() -> {
		lock.lock();
		try {
		    r.run();
		} finally {
		    lock.unlock();
		}
	    });
    }

    private final Object procmon = new Object();
    private Thread processor = null;
    private final Collection<Pair<MCache, Collection<MCache.Grid>>> updqueue = new HashSet<>();
    private final Collection<Segment> dirty = new HashSet<>();
    private boolean gdirty = false;
    private class Processor extends HackThread {
	Processor() {
	    super("Mapfile processor");
	}

	public void run() {
	    try {
		long last = System.currentTimeMillis();
		while(true) {
		    Runnable task;
		    long now = System.currentTimeMillis();
		    synchronized(procmon) {
			if(!updqueue.isEmpty()) {
			    Pair<MCache, Collection<MCache.Grid>> el = Utils.take(updqueue);
			    task = () -> MapFile.this.update(el.a, el.b);
			} else if(!dirty.isEmpty()) {
			    Segment seg = Utils.take(dirty);
			    task = locked(() -> segments.put(seg.id, seg), lock.writeLock());
			} else if(gdirty) {
			    task = locked(MapFile.this::save, lock.readLock());
			    gdirty = false;
			} else {
			    if(now - last > 10000) {
				processor = null;
				return;
			    }
			    procmon.wait(5000);
			    continue;
			}
		    }
		    task.run();
		    last = now;
		}
	    } catch(InterruptedException e) {
	    } finally {
		synchronized(procmon) {
		    processor = null;
		}
	    }
	}
    }
    private void process() {
	synchronized(procmon) {
	    if(processor == null) {
		Thread np = new Processor();
		np.start();
		processor = np;
	    }
	    procmon.notifyAll();
	}
    }

    public static Resource loadsaved(Resource.Pool pool, Resource.Spec spec) {
	try {
	    return(spec.get());
	} catch(Loading l) {
	    throw(l);
	} catch(Exception e) {
	    return(pool.load(spec.name).get());
	}
    }

    public abstract static class Marker {
	public long seg;
	public Coord tc;
	public String nm;

	public Marker(long seg, Coord tc, String nm) {
	    this.seg = seg;
	    this.tc = tc;
	    this.nm = nm;
	}
    }

    public static class PMarker extends Marker {
	public Color color;

	public PMarker(long seg, Coord tc, String nm, Color color) {
	    super(seg, tc, nm);
	    this.color = color;
	}
    }

    public static class SMarker extends Marker {
	public long oid;
	public Resource.Spec res;

	public SMarker(long seg, Coord tc, String nm, long oid, Resource.Spec res) {
	    super(seg, tc, nm);
	    this.oid = oid;
	    this.res = res;
	}
    }

    private static Marker loadmarker(Message fp) {
	int ver = fp.uint8();
	if(ver == 1) {
	    long seg = fp.int64();
	    Coord tc = fp.coord();
	    String nm = fp.string();
	    char type = (char)fp.uint8();
	    switch(type) {
	    case 'p':
		Color color = fp.color();
		return(new PMarker(seg, tc, nm, color));
	    case 's':
		long oid = fp.int64();
		Resource.Spec res = new Resource.Spec(Resource.remote(), fp.string(), fp.uint16());
		return(new SMarker(seg, tc, nm, oid, res));
	    default:
		throw(new Message.FormatError("Unknown marker type: " + (int)type));
	    }
	} else {
	    throw(new Message.FormatError("Unknown marker version: " + ver));
	}
    }

    private static void savemarker(Message fp, Marker mark) {
	fp.adduint8(1);
	fp.addint64(mark.seg);
	fp.addcoord(mark.tc);
	fp.addstring(mark.nm);
	if(mark instanceof PMarker) {
	    fp.adduint8('p');
	    fp.addcolor(((PMarker)mark).color);
	} else if(mark instanceof SMarker) {
	    SMarker sm = (SMarker)mark;
	    fp.adduint8('s');
	    fp.addint64(sm.oid);
	    fp.addstring(sm.res.name);
	    fp.adduint16(sm.res.ver);
	} else {
	    throw(new ClassCastException("Can only save PMarkers and SMarkers"));
	}
    }

    public void add(Marker mark) {
	lock.writeLock().lock();
	try {
	    if(markers.add(mark)) {
		if(mark instanceof SMarker)
		    smarkers.put(((SMarker)mark).oid, (SMarker)mark);
		defersave();
		markerseq++;
	    }
	} finally {
	    lock.writeLock().unlock();
	}
    }

    public void remove(Marker mark) {
	lock.writeLock().lock();
	try {
	    if(markers.remove(mark)) {
		if(mark instanceof SMarker)
		    smarkers.remove(((SMarker)mark).oid, (SMarker)mark);
		defersave();
		markerseq++;
	    }
	} finally {
	    lock.writeLock().unlock();
	}
    }

    public void update(Marker mark) {
	lock.readLock().lock();
	try {
	    if(markers.contains(mark)) {
		defersave();
		markerseq++;
	    }
	} finally {
	    lock.readLock().unlock();
	}
    }

    public static class TileInfo {
	public final Resource.Spec res;
	public final int prio;

	public TileInfo(Resource.Spec res, int prio) {
	    this.res = res; this.prio = prio;
	}
    }

    public static class DataGrid {
	public final TileInfo[] tilesets;
	public final byte[] tiles;
	public final long mtime;

	public DataGrid(TileInfo[] tilesets, byte[] tiles, long mtime) {
	    this.tilesets = tilesets;
	    this.tiles = tiles;
	    this.mtime = mtime;
	}

	public int gettile(Coord c) {
	    return(tiles[c.x + (c.y * cmaps.x)] & 0xff);
	}

	private BufferedImage tiletex(int t, BufferedImage[] texes, boolean[] cached) {
	    if(!cached[t]) {
		Resource r = null;
		try {
		    r = loadsaved(Resource.remote(), tilesets[t].res);
		} catch(Loading l) {
		    throw(l);
		} catch(Exception e) {
		    Debug.log.printf("mapfile warning: could not load tileset resource %s(v%d): %s\n", tilesets[t].res.name, tilesets[t].res.ver, e);
		}
		if(r != null) {
		    Resource.Image ir = r.layer(Resource.imgc);
		    if(ir != null) {
			texes[t] = ir.img;
		    }
		}
		cached[t] = true;
	    }
	    return(texes[t]);
	}

	public BufferedImage render(Coord off) {
	    BufferedImage[] texes = new BufferedImage[256];
	    boolean[] cached = new boolean[256];
	    WritableRaster buf = PUtils.imgraster(cmaps);
	    Coord c = new Coord();
	    for(c.y = 0; c.y < cmaps.y; c.y++) {
		for(c.x = 0; c.x < cmaps.x; c.x++) {
		    int t = gettile(c);
		    BufferedImage tex = tiletex(t, texes, cached);
		    int rgb = 0;
		    if(tex != null)
			rgb = tex.getRGB(Utils.floormod(c.x + off.x, tex.getWidth()),
					 Utils.floormod(c.y + off.y, tex.getHeight()));
		    buf.setSample(c.x, c.y, 0, (rgb & 0x00ff0000) >>> 16);
		    buf.setSample(c.x, c.y, 1, (rgb & 0x0000ff00) >>>  8);
		    buf.setSample(c.x, c.y, 2, (rgb & 0x000000ff) >>>  0);
		    buf.setSample(c.x, c.y, 3, (rgb & 0xff000000) >>> 24);
		}
	    }
	    for(c.y = 1; c.y < cmaps.y - 1; c.y++) {
		for(c.x = 1; c.x < cmaps.x - 1; c.x++) {
		    int p = tilesets[gettile(c)].prio;
		    if((tilesets[gettile(c.add(-1, 0))].prio > p) ||
		       (tilesets[gettile(c.add( 1, 0))].prio > p) ||
		       (tilesets[gettile(c.add(0, -1))].prio > p) ||
		       (tilesets[gettile(c.add(0,  1))].prio > p))
		    {
			buf.setSample(c.x, c.y, 0, 0);
			buf.setSample(c.x, c.y, 1, 0);
			buf.setSample(c.x, c.y, 2, 0);
			buf.setSample(c.x, c.y, 3, 255);
		    }
		}
	    }
	    return(PUtils.rasterimg(buf));
	}

	public static final Resource.Spec notile = new Resource.Spec(Resource.remote(), "gfx/tiles/notile", -1);
	public static final DataGrid nogrid;
	static {
	    nogrid = new DataGrid(new TileInfo[] {new TileInfo(notile, 0)}, new byte[cmaps.x * cmaps.y], 0);
	}
    }

    public static class Grid extends DataGrid {
	public final long id;
	private boolean[] norepl;
	private int useq = -1;

	public Grid(long id, TileInfo[] tilesets, byte[] tiles, long mtime) {
	    super(tilesets, tiles, mtime);
	    this.id = id;
	}

	public static Grid from(MCache map, MCache.Grid cg) {
	    int oseq = cg.seq;
	    int nt = 0;
	    Resource.Spec[] sets = new Resource.Spec[256];
	    int[] tmap = new int[256];
	    int[] rmap = new int[256];
	    boolean[] norepl = new boolean[256];
	    Arrays.fill(tmap, -1);
	    for(int tn : cg.tiles) {
		if(tmap[tn] == -1) {
		    tmap[tn] = nt;
		    rmap[nt] = tn;
		    sets[nt] = map.nsets[tn];
		    try {
			for(String tag : map.tileset(tn).tags) {
			    if(tag.equals("norepl"))
				norepl[nt] = true;
			}
		    } catch(Loading l) {
		    }
		    nt++;
		}
	    }
	    int[] prios = new int[nt];
	    for(int i = 0, tn = 0; i < 256; i++) {
		if(tmap[i] != -1)
		    prios[tmap[i]] = tn++;
	    }
	    TileInfo[] infos = new TileInfo[nt];
	    for(int i = 0; i < nt; i++)
		infos[i] = new TileInfo(sets[i], prios[i]);
	    byte[] tiles = new byte[cmaps.x * cmaps.y];
	    for(int i = 0; i < cg.tiles.length; i++)
		tiles[i] = (byte)(tmap[cg.tiles[i]]);
	    Grid g = new Grid(cg.id, infos, tiles, System.currentTimeMillis());
	    g.norepl = norepl;
	    g.useq = oseq;
	    return(g);
	}

	public Grid mergeprev(Grid prev) {
	    if((norepl == null) || (prev.tiles.length != this.tiles.length))
		return(this);
	    boolean[] used = new boolean[prev.tilesets.length];
	    boolean any = false;
	    int[] tmap = new int[prev.tilesets.length];
	    for(int i = 0; i < tmap.length; i++)
		tmap[i] = -1;
	    for(int i = 0; i < this.tiles.length; i++) {
		if(norepl[this.tiles[i]]) {
		    used[prev.tiles[i]] = true;
		    any = true;
		}
	    }
	    if(!any)
		return(this);
	    TileInfo[] ntilesets = this.tilesets;
	    for(int i = 0; i < used.length; i++) {
		if(used[i] && (tmap[i] < 0)) {
		    dedup: {
			for(int o = 0; o < this.tilesets.length; o++) {
			    if(this.tilesets[o].res.name.equals(prev.tilesets[i].res.name)) {
				tmap[i] = o;
				break dedup;
			    }
			}
			tmap[i] = ntilesets.length;
			ntilesets = Utils.extend(ntilesets, prev.tilesets[i]);
		    }
		}
	    }
	    byte[] ntiles = new byte[this.tiles.length];
	    for(int i = 0; i < this.tiles.length; i++) {
		if(norepl[this.tiles[i]])
		    ntiles[i] = (byte)tmap[prev.tiles[i]];
		else
		    ntiles[i] = this.tiles[i];
	    }
	    Grid g = new Grid(this.id, ntilesets, ntiles, this.mtime);
	    g.useq = this.useq;
	    return(g);
	}

	public void save(Message fp) {
	    fp.adduint8(2);
	    ZMessage z = new ZMessage(fp);
	    z.addint64(id);
	    z.addint64(mtime);
	    z.adduint8(tilesets.length);
	    for(int i = 0; i < tilesets.length; i++) {
		z.addstring(tilesets[i].res.name);
		z.adduint16(tilesets[i].res.ver);
		z.adduint8(tilesets[i].prio);
	    }
	    z.addbytes(tiles);
	    z.finish();
	}

	public void save(MapFile file) {
	    OutputStream fp;
	    try {
		fp = file.sstore("grid-%x", id);
	    } catch(IOException e) {
		throw(new StreamMessage.IOError(e));
	    }
	    try(StreamMessage out = new StreamMessage(fp)) {
		save(out);
	    }
	}

	public static Grid load(MapFile file, long id) {
	    InputStream fp;
	    try {
		fp = file.sfetch("grid-%x", id);
	    } catch(IOException e) {
		Debug.log.printf("mapfile warning: error when locating grid %x: %s\n", id, e);
		return(null);
	    }
	    try(StreamMessage data = new StreamMessage(fp)) {
		int ver = data.uint8();
		if((ver >= 1) && (ver <= 2)) {
		    ZMessage z = new ZMessage(data);
		    long storedid = z.int64();
		    if(storedid != id)
			throw(new Message.FormatError(String.format("Grid ID mismatch: expected %s, got %s", id, storedid)));
		    long mtime = (ver >= 2) ? z.int64() : System.currentTimeMillis();
		    List<TileInfo> tilesets = new ArrayList<TileInfo>();
		    for(int i = 0, no = z.uint8(); i < no; i++)
			tilesets.add(new TileInfo(new Resource.Spec(Resource.remote(), z.string(), z.uint16()), z.uint8()));
		    byte[] tiles = z.bytes(cmaps.x * cmaps.y);
		    return(new Grid(id, tilesets.toArray(new TileInfo[0]), tiles, mtime));
		} else {
		    throw(new Message.FormatError(String.format("Unknown grid data version for %x: %d", id, ver)));
		}
	    } catch(Message.BinError e) {
		Debug.log.printf("mapfile warning: error when loading grid %x: %s\n", id, e);
		return(null);
	    }
	}
    }

    public static class ZoomGrid extends DataGrid {
	public final long seg;
	public final int lvl;
	public final Coord sc;

	public ZoomGrid(long seg, int lvl, Coord sc, TileInfo[] tilesets, byte[] tiles, long mtime) {
	    super(tilesets, tiles, mtime);
	    this.seg = seg;
	    this.lvl = lvl;
	    this.sc = sc;
	}

	public static ZoomGrid fetch(MapFile file, Segment seg, int lvl, Coord sc) {
	    ZoomGrid loaded = load(file, seg.id, lvl, sc);
	    if(loaded != null)
		return(loaded);
	    return(from(file, seg, lvl, sc));
	}

	private static DataGrid fetchg(MapFile file, Segment seg, int lvl, Coord sc) {
	    if(lvl == 0) {
		Long id = seg.map.get(sc);
		if(id == null)
		    return(null);
		return(Grid.load(file, id));
	    } else {
		return(fetch(file, seg, lvl, sc));
	    }
	}

	public static ZoomGrid from(MapFile file, Segment seg, int lvl, Coord sc) {
	    if((lvl < 1) || ((sc.x & ((1 << lvl) - 1)) != 0) || ((sc.y & ((1 << lvl) - 1)) != 0))
		throw(new IllegalArgumentException(String.format("%s %s", sc, lvl)));
	    DataGrid[] lower = new DataGrid[4];
	    boolean any = false;
	    long maxmtime = 0;
	    for(int i = 0; i < 4; i++) {
		int x = i % 2, y = i / 2;
		lower[i] = fetchg(file, seg, lvl - 1, sc.add(x << (lvl - 1), y << (lvl - 1)));
		if(lower[i] != null) {
		    any = true;
		    maxmtime = Math.max(maxmtime, lower[i].mtime);
		} else {
		    lower[i] = DataGrid.nogrid;
		}
	    }
	    if(!any)
		return(null);

	    /* XXX: This is hardly "correct", but the correct
	     * implementation would require a topological sort, and
	     * it's not like it really matters that much. */
	    int nt = 0;
	    TileInfo[] infos;
	    Map<String, Integer> rinfos;
	    {
		Resource.Pool pool = null;
		String[] sets = new String[256];
		Set<String> hassets = new HashSet<>();
		Map<String, Integer> vers = new HashMap<>();
		for(int i = 0; i < 4; i++) {
		    if(lower[i] == null)
			continue;
		    for(int tn = 0; tn < lower[i].tilesets.length; tn++) {
			Resource.Spec set = lower[i].tilesets[tn].res;
			if(pool == null)
			    pool = set.pool;
			vers.put(set.name, Math.max(vers.getOrDefault(set.name, 0), set.ver));
			if(!hassets.contains(set.name)) {
			    sets[nt++] = set.name;
			    hassets.add(set.name);
			}
		    }
		}
		if(nt >= 256)
		    throw(new IllegalArgumentException(Integer.toString(nt)));
		infos = new TileInfo[nt];
		rinfos = new HashMap<>();
		for(int i = 0; i < nt; i++) {
		    infos[i] = new TileInfo(new Resource.Spec(pool, sets[i], vers.get(sets[i])), i);
		    rinfos.put(sets[i], i);
		}
	    }

	    byte[] tiles = new byte[cmaps.x * cmaps.y];
	    for(int gn = 0; gn < 4; gn++) {
		int gx = gn % 2, gy = gn / 2;
		DataGrid cg = lower[gn];
		if(cg == null)
		    continue;
		byte[] tmap = new byte[256];
		Arrays.fill(tmap, (byte)-1);
		for(int i = 0; i < cg.tilesets.length; i++)
		    tmap[i] = rinfos.get(cg.tilesets[i].res.name).byteValue();
		Coord off = cmaps.div(2).mul(gx, gy);
		byte[] tc = new byte[4];
		byte[] tcn = new byte[4];
		for(int y = 0; y < cmaps.y / 2; y++) {
		    for(int x = 0; x < cmaps.x / 2; x++) {
			int nd = 0;
			for(int sy = 0; sy < 2; sy++) {
			    for(int sx = 0; sx < 2; sx++) {
				byte st = tmap[cg.gettile(new Coord(x * 2, y * 2))];
				st: {
				    for(int i = 0; i < nd; i++) {
					if(tc[i] == st) {
					    tcn[i]++;
					    break st;
					}
				    }
				    tc[nd] = st;
				    tcn[nd] = 1;
				    nd++;
				}
			    }
			}
			int mi = 0;
			for(int i = 1; i < nd; i++) {
			    if(tcn[i] > tcn[mi])
				mi = i;
			}
			tiles[(x + off.x) + ((y + off.y) * cmaps.x)] = tc[mi];
		    }
		}
	    }
	    ZoomGrid ret = new ZoomGrid(seg.id, lvl, sc, infos, tiles, maxmtime);
	    ret.save(file);
	    return(ret);
	}

	public void save(Message fp) {
	    fp.adduint8(1);
	    ZMessage z = new ZMessage(fp);
	    z.addint64(seg);
	    z.addint32(lvl);
	    z.addcoord(sc);
	    z.addint64(mtime);
	    z.adduint8(tilesets.length);
	    for(int i = 0; i < tilesets.length; i++) {
		z.addstring(tilesets[i].res.name);
		z.adduint16(tilesets[i].res.ver);
		z.adduint8(tilesets[i].prio);
	    }
	    z.addbytes(tiles);
	    z.finish();
	}

	public void save(MapFile file) {
	    OutputStream fp;
	    try {
		fp = file.sstore("zgrid-%x-%d-%d-%d", seg, lvl, sc.x, sc.y);
	    } catch(IOException e) {
		throw(new StreamMessage.IOError(e));
	    }
	    try(StreamMessage out = new StreamMessage(fp)) {
		save(out);
	    }
	}

	public static ZoomGrid load(MapFile file, long seg, int lvl, Coord sc) {
	    InputStream fp;
	    try {
		fp = file.sfetch("zgrid-%x-%d-%d-%d", seg, lvl, sc.x, sc.y);
	    } catch(FileNotFoundException e) {
		return(null);
	    } catch(IOException e) {
		Debug.log.printf("mapfile warning: error when locating zoomgrid (%d, %d) in %x@%d: %s\n", sc.x, sc.y, seg, lvl, e);
		return(null);
	    }
	    try(StreamMessage data = new StreamMessage(fp)) {
		if(data.eom())
		    return(null);
		int ver = data.uint8();
		if(ver == 1) {
		    ZMessage z = new ZMessage(data);
		    long storedseg = z.int64();
		    if(storedseg != seg)
			throw(new Message.FormatError(String.format("Zoomgrid segment mismatch: expected %s, got %s", seg, storedseg)));
		    long storedlvl = z.int32();
		    if(storedlvl != lvl)
			throw(new Message.FormatError(String.format("Zoomgrid level mismatch: expected %s, got %s", lvl, storedlvl)));
		    Coord storedsc = z.coord();
		    if(!sc.equals(storedsc))
			throw(new Message.FormatError(String.format("Zoomgrid coord mismatch: expected %s, got %s", sc, storedsc)));

		    long mtime = z.int64();
		    List<TileInfo> tilesets = new ArrayList<TileInfo>();
		    for(int i = 0, no = z.uint8(); i < no; i++)
			tilesets.add(new TileInfo(new Resource.Spec(Resource.remote(), z.string(), z.uint16()), z.uint8()));
		    byte[] tiles = z.bytes(cmaps.x * cmaps.y);
		    return(new ZoomGrid(seg, lvl, sc, tilesets.toArray(new TileInfo[0]), tiles, mtime));
		} else {
		    throw(new Message.FormatError(String.format("Unknown zoomgrid data version for (%d, %d) in %x@%d: %d", sc.x, sc.y, seg, lvl, ver)));
		}
	    } catch(Message.BinError e) {
		Debug.log.printf("Unknown zoomgrid data version for (%d, %d) in %x@%d: %s", sc.x, sc.y, seg, lvl, e);
		return(null);
	    }
	}

	public static int inval(MapFile file, long seg, Coord sc) {
	    for(int lvl = 1; true; lvl++) {
		sc = new Coord(sc.x & ~((1 << lvl) - 1), sc.y & ~((1 << lvl) - 1));
		try {
		    file.sfetch("zgrid-%x-%d-%d-%d", seg, lvl, sc.x, sc.y).close();
		} catch(FileNotFoundException e) {
		    return(lvl - 1);
		} catch(IOException e) {
		    Debug.log.printf("mapfile warning: error when invalidating zoomgrid (%d, %d) in %x@%d: %s\n", sc.x, sc.y, seg, lvl, e);
		    return(lvl - 1);
		}
		try {
		    file.sstore("zgrid-%x-%d-%d-%d", seg, lvl, sc.x, sc.y).close();
		} catch(IOException e) {
		    throw(new StreamMessage.IOError(e));
		}
	    }
	}
    }

    public static class ZoomCoord {
	public final int lvl;
	public final Coord c;

	public ZoomCoord(int lvl, Coord c) {
	    this.lvl = lvl;
	    this.c = c;
	}

	public int hashCode() {
	    return((c.hashCode() * 31) + lvl);
	}

	public boolean equals(Object o) {
	    if(!(o instanceof ZoomCoord))
		return(false);
	    ZoomCoord that = (ZoomCoord)o;
	    return((this.lvl == that.lvl) && this.c.equals(that.c));
	}
    }

    public class Segment {
	public final long id;
	private final BMap<Coord, Long> map = new HashBMap<>();
	private final Map<Long, Cached> cache = new CacheMap<>(CacheMap.RefType.WEAK);
	private final Map<Coord, ByCoord> ccache = new CacheMap<>(CacheMap.RefType.WEAK);
	private final Map<ZoomCoord, ByZCoord> zcache = new CacheMap<>(CacheMap.RefType.WEAK);

	public Segment(long id) {
	    this.id = id;
	}

	private class Cached implements Indir<Grid> {
	    Grid loaded;
	    Future<Grid> loading;

	    Cached(Future<Grid> loading) {
		this.loading = loading;
	    }

	    public Grid get() {
		if(loaded == null)
		    loaded = loading.get(0);
		return(loaded);
	    }
	}

	private Grid loaded(long id) {
	    checklock();
	    synchronized(cache) {
		Cached cur = cache.get(id);
		if(cur != null)
		    return(cur.loaded);
	    }
	    return(null);
	}

	private Future<Grid> loadgrid(long id) {
	    return(Defer.later(() -> Grid.load(MapFile.this, id)));
	}

	private Cached grid0(long id) {
	    checklock();
	    synchronized(cache) {
		return(cache.computeIfAbsent(id, k -> new Cached(loadgrid(k))));
	    }
	}
	public Indir<Grid> grid(long id) {return(grid0(id));}

	private class ByCoord implements Indir<Grid> {
	    final Coord sc;
	    Cached cur;

	    ByCoord(Coord sc, Cached cur) {
		this.sc = sc;
		this.cur = cur;
	    }

	    public Grid get() {
		Cached cur = this.cur;
		if(cur == null)
		    return(null);
		return(cur.get());
	    }
	}

	private Future<ZoomGrid> loadzgrid(ZoomCoord zc) {
	    return(Defer.later(() -> ZoomGrid.fetch(MapFile.this, Segment.this, zc.lvl, zc.c)));
	}

	private class ByZCoord implements Indir<ZoomGrid> {
	    final ZoomCoord zc;
	    ZoomGrid loaded;
	    Future<ZoomGrid> loading;

	    ByZCoord(ZoomCoord zc, Future<ZoomGrid> loading) {
		this.zc = zc;
		this.loading = loading;
	    }

	    public ZoomGrid get() {
		if(loaded == null)
		    loaded = loading.get(0);
		return(loaded);
	    }
	}

	public Indir<Grid> grid(Coord gc) {
	    checklock();
	    synchronized(ccache) {
		return(ccache.computeIfAbsent(gc, k -> {
			    Long id = map.get(k);
			    Cached cur = (id == null)?null:grid0(id);
			    return(new ByCoord(k, cur));
			}));
	    }
	}

	public Indir<? extends DataGrid> grid(int lvl, Coord gc) {
	    if((lvl < 0) || ((gc.x & ((1 << lvl) - 1)) != 0) || ((gc.x & ((1 << lvl) - 1)) != 0))
		throw(new IllegalArgumentException(String.format("%s %s", gc, lvl)));
	    if(lvl == 0)
		return(grid(gc));
	    synchronized(zcache) {
		ZoomCoord zc = new ZoomCoord(lvl, gc);
		return(zcache.computeIfAbsent(zc, k -> new ByZCoord(k, loadzgrid(k))));
	    }
	}

	private void include(long id, Coord sc) {
	    map.put(sc, id);
	    int zl = ZoomGrid.inval(MapFile.this, this.id, sc);
	    synchronized(zcache) {
		for(int lvl = 1; lvl < zl; lvl++) {
		    ZoomCoord zc = new ZoomCoord(lvl, new Coord(sc.x & ~((1 << lvl) - 1), sc.y & ~((1 << lvl) - 1)));
		    ByZCoord zg = zcache.get(zc);
		    if(zg != null) {
			zg.loading = loadzgrid(zc);
			zg.loaded = null;
		    }
		}
	    }
	    ByCoord bc;
	    synchronized(ccache) {
		bc = ccache.get(sc);
	    }
	    if((bc != null) && (bc.cur == null))
		bc.cur = grid0(id);
	}

	private void include(Grid grid, Coord sc) {
	    checklock();
	    include(grid.id, sc);
	    synchronized(cache) {
		Cached cur = cache.get(grid.id);
		if(cur != null)
		    cur.loaded = grid;
	    }
	}
    }

    public final BackCache<Long, Segment> segments = new BackCache<>(5, id -> {
	    checklock();
	    InputStream fp;
	    try {
		fp = sfetch("seg-%x", id);
	    } catch(IOException e) {
		return(null);
	    }
	    try(StreamMessage data = new StreamMessage(fp)) {
		int ver = data.uint8();
		if(ver == 1) {
		    Segment seg = new Segment(id);
		    ZMessage z = new ZMessage(data);
		    long storedid = z.int64();
		    if(storedid != id)
			throw(new Message.FormatError(String.format("Segment ID mismatch: expected %x, got %x", id, storedid)));
		    for(int i = 0, no = z.int32(); i < no; i++)
			seg.map.put(z.coord(), z.int64());
		    return(seg);
		} else {
		    throw(new Message.FormatError("Unknown segment data version: " + ver));
		}
	    } catch(Message.BinError e) {
		Debug.log.printf("mapfile warning: error when loading segment %x: %s\n", id, e);
		return(null);
	    }
	}, (id, seg) -> {
	    checklock();
	    OutputStream fp;
	    try {
		fp = sstore("seg-%x", seg.id);
	    } catch(IOException e) {
		throw(new StreamMessage.IOError(e));
	    }
	    try(StreamMessage out = new StreamMessage(fp)) {
		out.adduint8(1);
		ZMessage z = new ZMessage(out);
		z.addint64(seg.id);
		z.addint32(seg.map.size());
		for(Map.Entry<Coord, Long> e : seg.map.entrySet())
		    z.addcoord(e.getKey()).addint64(e.getValue());
		z.finish();
	    }
	    if(knownsegs.add(id))
		defersave();
	});

    private void merge(Segment dst, Segment src, Coord soff) {
	checklock();
	for(Map.Entry<Coord, Long> gi : src.map.entrySet()) {
	    long id = gi.getValue();
	    Coord sc = gi.getKey();
	    Coord dc = sc.sub(soff);
	    dst.include(id, dc);
	    gridinfo.put(id, new GridInfo(id, dst.id, dc));
	}
	boolean mf = false;
	for(Marker mark : markers) {
	    if(mark.seg == src.id) {
		mark.seg = dst.id;
		mark.tc = mark.tc.sub(soff.mul(cmaps));
		mf = true;
	    }
	}
	if(mf)
	    markerseq++;
	knownsegs.remove(src.id);
	defersave();
	synchronized(procmon) {
	    dirty.add(dst);
	    process();
	}
    }

    public void update(MCache map, Collection<MCache.Grid> grids) {
	lock.writeLock().lock();
	try {
	    long mseg = -1;
	    Coord moff = null;
	    Collection<MCache.Grid> missing = new ArrayList<>(grids.size());
	    Collection<Pair<Long, Coord>> merge = null;
	    for(MCache.Grid g : grids) {
		GridInfo info = gridinfo.get(g.id);
		if(info == null) {
		    missing.add(g);
		    continue;
		}
		Segment seg = segments.get(info.seg);
		if(seg == null) {
		    missing.add(g);
		    continue;
		}
		if(moff == null) {
		    Coord psc = seg.map.reverse().get(g.id);
		    if(psc == null) {
			Debug.log.printf("mapfile warning: grid %x is oddly gone from segment %x; was at %s\n", g.id, seg.id, info.sc);
			missing.add(g);
			continue;
		    } else if(!psc.equals(info.sc)) {
			Debug.log.printf("mapfile warning: segment-offset mismatch for grid %x in segment %x: segment has %s, gridinfo has %s\n", g.id, seg.id, psc, info.sc);
			missing.add(g);
			continue;
		    }
		    mseg = seg.id;
		    moff = info.sc.sub(g.gc);
		}
		Grid cur = seg.loaded(g.id);
		if(!((cur != null) && (cur.useq == g.seq))) {
		    Grid sg = Grid.from(map, g);
		    Grid prev = cur;
		    if(prev == null)
			prev = Grid.load(MapFile.this, sg.id);
		    if(prev != null)
			sg = sg.mergeprev(prev);
		    sg.save(MapFile.this);
		}
		if(seg.id != mseg) {
		    if(merge == null)
			merge = new HashSet<>();
		    Coord soff = info.sc.sub(g.gc.add(moff));
		    merge.add(new Pair<>(seg.id, soff));
		}
	    }
	    if(!missing.isEmpty()) {
		Segment seg;
		if(mseg == -1) {
		    seg = new Segment(Utils.el(missing).id);
		    moff = Coord.z;
		    if(debug) Debug.log.printf("mapfile: creating new segment %x\n", seg.id);
		} else {
		    seg = segments.get(mseg);
		}
		synchronized(procmon) {
		    dirty.add(seg);
		    process();
		}
		for(MCache.Grid g : missing) {
		    Grid sg = Grid.from(map, g);
		    Coord sc = g.gc.add(moff);
		    sg.save(MapFile.this);
		    seg.include(sg, sc);
		    gridinfo.put(g.id, new GridInfo(g.id, seg.id, sc));
		}
	    }
	    if(merge != null) {
		for(Pair<Long, Coord> mel : merge) {
		    Segment a = segments.get(mseg);
		    Segment b = segments.get(mel.a);
		    Coord ab = mel.b;
		    Segment src, dst; Coord soff;
		    if(a.map.size() > b.map.size()) {
			src = b; dst = a;
			soff = ab;
		    } else {
			src = a; dst = b;
			soff = ab.inv();
		    }
		    if(debug) Debug.log.printf("mapfile: merging segment %x (%d) into %x (%d) at %s\n", src.id, src.map.size(), dst.id, dst.map.size(), soff);
		    merge(dst, src, soff);
		}
	    }
	} finally {
	    lock.writeLock().unlock();
	}
	if(debug) Debug.log.printf("mapfile: update completed\n");
    }

    private static final Coord[] inout = new Coord[] {
	new Coord( 0,  0),
	new Coord( 0, -1), new Coord( 1,  0), new Coord( 0,  1), new Coord(-1,  0),
	new Coord( 1, -1), new Coord( 1,  1), new Coord(-1,  1), new Coord(-1, -1),
    };
    public void update(MCache map, Coord cgc) {
	Collection<MCache.Grid> grids = new ArrayList<>();
	for(Coord off : inout) {
	    Coord gc = cgc.add(off);
	    try {
		grids.add(map.getgrid(gc));
	    } catch(Loading l) {
		continue;
	    }
	}
	if(!grids.isEmpty()) {
	    synchronized(procmon) {
		updqueue.add(new Pair<>(map, grids));
		process();
	    }
	}
    }
}
