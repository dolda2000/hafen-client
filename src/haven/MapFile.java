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
import static haven.MCache.cmaps;

public class MapFile {
    public final ResCache store;
    public ResCache store() {return(store);}
    public final Collection<Long> knownsegs = new HashSet<Long>();
    public final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public MapFile(ResCache store) {
	this.store = store;
    }

    private void checklock() {
	if((lock.getReadHoldCount() == 0) && !lock.isWriteLockedByCurrentThread())
	    throw(new IllegalMonitorStateException());
    }

    public static MapFile load(ResCache store) {
	MapFile file = new MapFile(store);
	InputStream fp;
	try {
	    fp = store.fetch("map/index");
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
	    } else {
		Debug.log.printf("mapfile warning: unknown mapfile index version: %i\n", ver);
		return(null);
	    }
	} catch(Message.BinError e) {
	    Debug.log.printf("mapfile warning: error when loading index: %s\n", e);
	    return(null);
	}
	return(file);
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
		fp = store().fetch(String.format("map/gi-%x", id));
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
		fp = store().store(String.format("map/gi-%x", info.id));
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

    public static class TileInfo {
	public final Resource.Spec res;
	public final int prio;

	public TileInfo(Resource.Spec res, int prio) {
	    this.res = res; this.prio = prio;
	}
    }

    public static class Grid {
	public final long id;
	public final TileInfo[] tilesets;
	public final byte[] tiles;
	private int useq = -1;

	public Grid(long id, TileInfo[] tilesets, byte[] tiles) {
	    this.id = id;
	    this.tilesets = tilesets;
	    this.tiles = tiles;
	}

	public static Grid from(MCache map, MCache.Grid cg) {
	    int oseq = cg.seq;
	    int nt = 0;
	    Resource.Spec[] sets = new Resource.Spec[256];
	    int[] tmap = new int[256];
	    int[] rmap = new int[256];
	    Arrays.fill(tmap, -1);
	    for(int tn : cg.tiles) {
		if(tmap[tn] == -1) {
		    tmap[tn] = nt;
		    rmap[nt] = tn;
		    sets[nt] = map.nsets[tn];
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
	    Grid g = new Grid(cg.id, infos, tiles);
	    g.useq = oseq;
	    return(g);
	}

	public void save(Message fp) {
	    fp.adduint8(1);
	    ZMessage z = new ZMessage(fp);
	    z.addint64(id);
	    z.adduint8(tilesets.length);
	    for(int i = 0; i < tilesets.length; i++) {
		z.addstring(tilesets[i].res.name);
		z.adduint16(tilesets[i].res.ver);
		z.adduint8(tilesets[i].prio);
	    }
	    z.addbytes(tiles);
	    z.finish();
	}

	public void save(ResCache store) {
	    OutputStream fp;
	    try {
		fp = store.store(String.format("map/grid-%x", id));
	    } catch(IOException e) {
		throw(new StreamMessage.IOError(e));
	    }
	    try(StreamMessage out = new StreamMessage(fp)) {
		save(out);
	    }
	}

	public static Grid load(ResCache store, long id) {
	    InputStream fp;
	    try {
		fp = store.fetch(String.format("map/grid-%x", id));
	    } catch(IOException e) {
		return(null);
	    }
	    try(StreamMessage data = new StreamMessage(fp)) {
		int ver = data.uint8();
		if(ver == 1) {
		    ZMessage z = new ZMessage(data);
		    long storedid = z.int64();
		    if(storedid != id)
			throw(new Message.FormatError(String.format("Grid ID mismatch: expected %s, got %s", id, storedid)));
		    List<TileInfo> tilesets = new ArrayList<TileInfo>();
		    for(int i = 0, no = z.uint8(); i < no; i++)
			tilesets.add(new TileInfo(new Resource.Spec(Resource.remote(), z.string(), z.uint16()), z.uint8()));
		    byte[] tiles = z.bytes(cmaps.x * cmaps.y);
		    return(new Grid(id, tilesets.toArray(new TileInfo[0]), tiles));
		} else {
		    throw(new Message.FormatError(String.format("Unknown grid data version for %x: %i", id, ver)));
		}
	    } catch(Message.BinError e) {
		Debug.log.printf("mapfile warning: error when loading grid %x: %s\n", id, e);
		return(null);
	    }
	}
    }

    public class Segment {
	public final long id;
	private final BMap<Coord, Long> map = new HashBMap<>();
	private final Map<Long, Grid> loaded = new HashMap<>();
	private final Map<Long, Defer.Future<Grid>> loading = new HashMap<>();

	public Segment(long id) {
	    this.id = id;
	}

	private void include(Grid grid, Coord sc) {
	    map.put(sc, grid.id);
	    loaded.put(grid.id, grid);
	}

	private Indir<Grid> grid(long id) {
	    checklock();
	    Grid g = loaded.get(id);
	    if(g != null)
		return(() -> g);
	    Defer.Future<Grid> f = loading.get(id);
	    if(f == null) {
		f = Defer.later(() -> {
			Grid lg = Grid.load(store, id);
			lock.writeLock().lock();
			try {
			    loaded.put(id, lg);
			    loading.remove(id);
			} finally {
			    lock.writeLock().unlock();
			}
			return(lg);
		    });
		loading.put(id, f);
	    }
	    Defer.Future<Grid> F = f;
	    return(() -> F.get());
	}

	public Indir<Grid> grid(Coord gc) {
	    checklock();
	    Long id = map.get(gc);
	    if(id == null)
		return(null);
	    return(grid(id));
	}
    }

    public final BackCache<Long, Segment> segments = new BackCache<>(5, id -> {
	    checklock();
	    InputStream fp;
	    try {
		fp = store().fetch(String.format("map/seg-%x", id));
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
		fp = store().store(String.format("map/seg-%x", seg.id));
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
	});

    public void update(MCache map, Collection<MCache.Grid> grids) {
	lock.writeLock().lock();
	try {
	    long mseg = -1;
	    Coord moff = null;
	    Collection<MCache.Grid> missing = new ArrayList<>(grids.size());
	    Set<Segment> dirty = new HashSet<Segment>();
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
		if(seg.id == mseg) {
		    Grid cur = seg.loaded.get(g.id);
		    if(!((cur != null) && (cur.useq == g.seq))) {
			Grid sg = Grid.from(map, g);
			sg.save(store);
		    }
		}
	    }
	    if(!missing.isEmpty()) {
		Segment seg;
		if(mseg == -1) {
		    seg = new Segment(Utils.el(missing).id);
		    Debug.log.printf("mapfile: creating new segment %x\n", seg.id);
		} else {
		    seg = segments.get(mseg);
		}
		dirty.add(seg);
		for(MCache.Grid g : missing) {
		    Grid sg = Grid.from(map, g);
		    Coord sc = g.gc.add(moff);
		    sg.save(store);
		    seg.include(sg, g.gc.add(moff));
		    gridinfo.put(g.id, new GridInfo(g.id, seg.id, sc));
		}
	    }
	    for(Segment seg : dirty) {
		segments.put(seg.id, seg);
	    }
	} finally {
	    lock.writeLock().unlock();
	}
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
	if(!grids.isEmpty())
	    Utils.defer(() -> update(map, grids));
    }
}
