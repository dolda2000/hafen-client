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
import java.lang.reflect.*;
import haven.Resource.Tileset;
import haven.Resource.Tile;

public class MCache {
    public static final Coord tilesz = new Coord(11, 11);
    public static final Coord cmaps = new Coord(100, 100);
    public static final Coord cutsz = new Coord(25, 25);
    public static final Coord cutn = cmaps.div(cutsz);
    public final Resource[] sets = new Resource[256];
    private final Tileset[] csets = new Tileset[256];
    private final Tiler[] tiles = new Tiler[256];
    Map<Coord, Request> req = new HashMap<Coord, Request>();
    Map<Coord, Grid> grids = new HashMap<Coord, Grid>();
    Session sess;
    Set<Overlay> ols = new HashSet<Overlay>();
    int olseq = 0;
    Random gen = new Random();
    Map<Integer, Defrag> fragbufs = new TreeMap<Integer, Defrag>();
    long lastctick = System.currentTimeMillis();

    public static class LoadingMap extends Loading {
    }

    private static class Request {
	private long lastreq = 0;
	private int reqs = 0;
    }

    public class Overlay {
	private Coord c1, c2;
	private int mask;

	public Overlay(Coord c1, Coord c2, int mask) {
	    this.c1 = c1;
	    this.c2 = c2;
	    this.mask = mask;
	    ols.add(this);
	    olseq++;
	}

	public void destroy() {
	    ols.remove(this);
	}

	public void update(Coord c1, Coord c2) {
	    if(!c1.equals(this.c1) || !c2.equals(this.c2)) {
		olseq++;
		this.c1 = c1;
		this.c2 = c2;
	    }
	}
    }

    public class Grid {
	public final int tiles[] = new int[cmaps.x * cmaps.y];
	public final int z[] = new int[cmaps.x * cmaps.y];
	public final int ol[] = new int[cmaps.x * cmaps.y];
	private final Cut cuts[];
	int olseq = -1;
	private Collection<Gob>[] fo = null;
	public final Coord gc, ul;
	public long id;
	String mnm;

	private class Cut {
	    MapMesh mesh;
	    Defer.Future<MapMesh> dmesh;
	    Rendered[] ols;
	    int deftag;
	}

	private class Flavobj extends Gob {
	    private Flavobj(Coord c, double a) {
		super(sess.glob, c);
		this.a = a;
	    }

	    public Random mkrandoom() {
		Random r = new Random(Grid.this.id);
		r.setSeed(r.nextInt() ^ rc.x);
		r.setSeed(r.nextInt() ^ rc.y);
		return(r);
	    }
	}

	public Grid(Coord gc) {
	    this.gc = gc;
	    this.ul = gc.mul(cmaps);
	    cuts = new Cut[cutn.x * cutn.y];
	    for(int i = 0; i < cuts.length; i++)
		cuts[i] = new Cut();
	}

	public int gettile(Coord tc) {
	    return(tiles[tc.x + (tc.y * cmaps.x)]);
	}

	public int getz(Coord tc) {
	    return(z[tc.x + (tc.y * cmaps.x)]);
	}

	public int getol(Coord tc) {
	    return(ol[tc.x + (tc.y * cmaps.x)]);
	}

	private void makeflavor() {
	    @SuppressWarnings("unchecked")
	    Collection<Gob>[] fo = (Collection<Gob>[])new Collection[cutn.x * cutn.y];
	    for(int i = 0; i < fo.length; i++)
		fo[i] = new LinkedList<Gob>();
	    Coord c = new Coord(0, 0);
	    Coord tc = gc.mul(cmaps);
	    int i = 0;
	    Random rnd = new Random(id);
	    for(c.y = 0; c.y < cmaps.x; c.y++) {
		for(c.x = 0; c.x < cmaps.y; c.x++, i++) {
		    Tileset set = tileset(tiles[i]);
		    if(set.flavobjs.size() > 0) {
			if(rnd.nextInt(set.flavprob) == 0) {
			    Resource r = set.flavobjs.pick(rnd);
			    double a = rnd.nextDouble() * 2 * Math.PI;
			    Gob g = new Flavobj(c.add(tc).mul(tilesz).add(tilesz.div(2)), a);
			    g.setattr(new ResDrawable(g, r));
			    Coord cc = c.div(cutsz);
			    fo[cc.x + (cc.y * cutn.x)].add(g);
			}
		    }
		}
	    }
	    this.fo = fo;
	}

	public Collection<Gob> getfo(Coord cc) {
	    if(fo == null)
		makeflavor();
	    return(fo[cc.x + (cc.y * cutn.x)]);
	}
	
	private Cut geticut(Coord cc) {
	    return(cuts[cc.x + (cc.y * cutn.x)]);
	}

	public MapMesh getcut(Coord cc) {
	    Cut cut = geticut(cc);
	    if(cut.dmesh != null) {
		if(cut.dmesh.done() || (cut.mesh == null)) {
		    cut.mesh = cut.dmesh.get();
		    cut.dmesh = null;
		}
	    }
	    return(cut.mesh);
	}
	
	public Rendered getolcut(int ol, Coord cc) {
	    int nseq = MCache.this.olseq;
	    if(this.olseq != nseq) {
		for(int i = 0; i < cutn.x * cutn.y; i++)
		    cuts[i].ols = null;
		this.olseq = nseq;
	    }
	    Cut cut = geticut(cc);
	    if(cut.ols == null)
		cut.ols = getcut(cc).makeols();
	    return(cut.ols[ol]);
	}
	
	private void buildcut(final Coord cc) {
	    final Cut cut = geticut(cc);
	    final int deftag = ++cut.deftag;
	    cut.dmesh = Defer.later(new Defer.Callable<MapMesh>() {
		    public MapMesh call() {
			Random rnd = new Random(id);
			rnd.setSeed(rnd.nextInt() ^ cc.x);
			rnd.setSeed(rnd.nextInt() ^ cc.y);
			return(MapMesh.build(MCache.this, rnd, ul.add(cc.mul(cutsz)), cutsz));
		    }
		});
	}

	public void ivneigh(Coord nc) {
	    Coord cc = new Coord();
	    for(cc.y = 0; cc.y < cutn.y; cc.y++) {
		for(cc.x = 0; cc.x < cutn.x; cc.x++) {
		    if((((nc.x < 0) && (cc.x == 0)) || ((nc.x > 0) && (cc.x == cutn.x - 1)) || (nc.x == 0)) &&
		       (((nc.y < 0) && (cc.y == 0)) || ((nc.y > 0) && (cc.y == cutn.y - 1)) || (nc.y == 0))) {
			buildcut(new Coord(cc));
		    }
		}
	    }
	}
	
	public void tick(int dt) {
	    if(fo != null) {
		for(Collection<Gob> fol : fo) {
		    for(Gob fo : fol)
			fo.ctick(dt);
		}
	    }
	}
	
	private void invalidate() {
	    for(int y = 0; y < cutn.y; y++) {
		for(int x = 0; x < cutn.x; x++)
		    buildcut(new Coord(x, y));
	    }
	    fo = null;
	    for(Coord ic : new Coord[] {
		    new Coord(-1, -1), new Coord( 0, -1), new Coord( 1, -1),
		    new Coord(-1,  0),                    new Coord( 1,  0),
		    new Coord(-1,  1), new Coord( 0,  1), new Coord( 1,  1)}) {
		Grid ng = grids.get(gc.add(ic));
		if(ng != null)
		    ng.ivneigh(ic.inv());
	    }
	}

	public void fill(Message msg) {
	    String mmname = msg.string().intern();
	    if(mmname.equals(""))
		mnm = null;
	    else
		mnm = mmname;
	    int[] pfl = new int[256];
	    while(true) {
		int pidx = msg.uint8();
		if(pidx == 255)
		    break;
		pfl[pidx] = msg.uint8();
	    }
	    Message blob = msg.inflate();
	    id = blob.int64();
	    for(int i = 0; i < tiles.length; i++)
		tiles[i] = blob.uint8();
	    for(int i = 0; i < z.length; i++)
		z[i] = blob.int16();
	    for(int i = 0; i < ol.length; i++)
		ol[i] = 0;
	    while(true) {
		int pidx = blob.uint8();
		if(pidx == 255)
		    break;
		int fl = pfl[pidx];
		int type = blob.uint8();
		Coord c1 = new Coord(blob.uint8(), blob.uint8());
		Coord c2 = new Coord(blob.uint8(), blob.uint8());
		int ol;
		if(type == 0) {
		    if((fl & 1) == 1)
			ol = 2;
		    else
			ol = 1;
		} else if(type == 1) {
		    if((fl & 1) == 1)
			ol = 8;
		    else
			ol = 4;
		} else {
		    throw(new RuntimeException("Unknown plot type " + type));
		}
		for(int y = c1.y; y <= c2.y; y++) {
		    for(int x = c1.x; x <= c2.x; x++) {
			this.ol[x + (y * cmaps.x)] |= ol;
		    }
		}
	    }
	    invalidate();
	}
    }

    public MCache(Session sess) {
	this.sess = sess;
    }

    public void ctick() {
	long now = System.currentTimeMillis();
	int dt = (int)(now - lastctick);
	synchronized(grids) {
	    for(Grid g : grids.values()) {
		g.tick(dt);
	    }
	}
	lastctick = now;
    }

    public void invalidate(Coord cc) {
	synchronized(req) {
	    if(req.get(cc) == null)
		req.put(cc, new Request());
	}
    }

    public void invalblob(Message msg) {
	int type = msg.uint8();
	if(type == 0) {
	    invalidate(msg.coord());
	} else if(type == 1) {
	    Coord ul = msg.coord();
	    Coord lr = msg.coord();
	    trim(ul, lr);
	} else if(type == 2) {
	    trimall();
	}
    }

    private Grid cached = null;
    public Grid getgrid(Coord gc) {
	synchronized(grids) {
	    if((cached == null) || !cached.gc.equals(cached)) {
		cached = grids.get(gc);
		if(cached == null) {
		    request(gc);
		    throw(new LoadingMap());
		}
	    }
	    return(cached);
	}
    }

    public Grid getgridt(Coord tc) {
	return(getgrid(tc.div(cmaps)));
    }

    public int gettile(Coord tc) {
	Grid g = getgridt(tc);
	return(g.gettile(tc.sub(g.ul)));
    }

    public int getz(Coord tc) {
	Grid g = getgridt(tc);
	return(g.getz(tc.sub(g.ul)));
    }

    public float getcz(float px, float py) {
	float tw = tilesz.x, th = tilesz.y;
	Coord ul = new Coord(Utils.floordiv(px, tw), Utils.floordiv(py, th));
	float sx = Utils.floormod(px, tw) / tw;
	float sy = Utils.floormod(py, th) / th;
	return(((1.0f - sy) * (((1.0f - sx) * getz(ul)) + (sx * getz(ul.add(1, 0))))) +
	       (sy * (((1.0f - sx) * getz(ul.add(0, 1))) + (sx * getz(ul.add(1, 1))))));
    }

    public float getcz(Coord pc) {
	return(getcz(pc.x, pc.y));
    }

    public int getol(Coord tc) {
	Grid g = getgridt(tc);
	int ol = g.getol(tc.sub(g.ul));
	for(Overlay lol : ols) {
	    if(tc.isect(lol.c1, lol.c2.add(lol.c1.inv()).add(new Coord(1, 1))))
		ol |= lol.mask;
	}
	return(ol);
    }
    
    public MapMesh getcut(Coord cc) {
	return(getgrid(cc.div(cutn)).getcut(cc.mod(cutn)));
    }
    
    public Collection<Gob> getfo(Coord cc) {
	return(getgrid(cc.div(cutn)).getfo(cc.mod(cutn)));
    }

    public Rendered getolcut(int ol, Coord cc) {
	return(getgrid(cc.div(cutn)).getolcut(ol, cc.mod(cutn)));
    }

    public void mapdata2(Message msg) {
	Coord c = msg.coord();
	synchronized(grids) {
	    synchronized(req) {
		if(req.containsKey(c)) {
		    Grid g = grids.get(c);
		    if(g == null)
			grids.put(c, g = new Grid(c));
		    g.fill(msg);
		    req.remove(c);
		    olseq++;
		}
	    }
	}
    }

    public void mapdata(Message msg) {
	long now = System.currentTimeMillis();
	int pktid = msg.int32();
	int off = msg.uint16();
	int len = msg.uint16();
	Defrag fragbuf;
	synchronized(fragbufs) {
	    if((fragbuf = fragbufs.get(pktid)) == null) {
		fragbuf = new Defrag(len);
		fragbufs.put(pktid, fragbuf);
	    }
	    fragbuf.add(msg.blob, 8, msg.blob.length - 8, off);
	    fragbuf.last = now;
	    if(fragbuf.done()) {
		mapdata2(fragbuf.msg());
		fragbufs.remove(pktid);
	    }
	
	    /* Clean up old buffers */
	    for(Iterator<Map.Entry<Integer, Defrag>> i = fragbufs.entrySet().iterator(); i.hasNext();) {
		Map.Entry<Integer, Defrag> e = i.next();
		Defrag old = e.getValue();
		if(now - old.last > 10000)
		    i.remove();
	    }
	}
    }

    public Tileset tileset(int i) {
	synchronized(csets) {
	    if(csets[i] == null) {
		if(sets[i] == null)
		    return(null);
		if(sets[i].loading)
		    throw(new LoadingMap());
		csets[i] = sets[i].layer(Resource.tileset);
	    }
	    return(csets[i]);
	}
    }

    public Tiler tiler(int i) {
	synchronized(tiles) {
	    if(tiles[i] == null) {
		Tileset set = tileset(i);
		if(set == null)
		    return(null);
		tiles[i] = set.tfac().create(i, set);
	    }
	    return(tiles[i]);
	}
    }

    public void tilemap(Message msg) {
	while(!msg.eom()) {
	    int id = msg.uint8();
	    String resnm = msg.string();
	    int resver = msg.uint16();
	    sets[id] = Resource.load(resnm, resver);
	}
    }

    public void trimall() {
	synchronized(grids) {
	    synchronized(req) {
		grids.clear();
		req.clear();
	    }
	}
    }

    public void trim(Coord ul, Coord lr) {
	synchronized(grids) {
	    synchronized(req) {
		for(Iterator<Map.Entry<Coord, Grid>> i = grids.entrySet().iterator(); i.hasNext();) {
		    Map.Entry<Coord, Grid> e = i.next();
		    Coord gc = e.getKey();
		    Grid g = e.getValue();
		    if((gc.x < ul.x) || (gc.y < ul.y) || (gc.x > lr.x) || (gc.y > lr.y))
			i.remove();
		}
		for(Iterator<Coord> i = req.keySet().iterator(); i.hasNext();) {
		    Coord gc = i.next();
		    if((gc.x < ul.x) || (gc.y < ul.y) || (gc.x > lr.x) || (gc.y > lr.y))
			i.remove();
		}
	    }
	}
    }

    public void request(Coord gc) {
	synchronized(req) {
	    if(!req.containsKey(gc))
		req.put(gc, new Request());
	}
    }

    public void reqarea(Coord ul, Coord br) {
	ul = ul.div(cutsz); br = br.div(cutsz);
	Coord rc = new Coord();
	for(rc.y = ul.y; rc.y <= br.y; rc.y++) {
	    for(rc.x = ul.x; rc.x <= br.x; rc.x++) {
		try {
		    getcut(new Coord(rc));
		} catch(Loading e) {}
	    }
	}
    }

    public void sendreqs() {
	long now = System.currentTimeMillis();
	synchronized(req) {
	    for(Iterator<Map.Entry<Coord, Request>> i = req.entrySet().iterator(); i.hasNext();) {
		Map.Entry<Coord, Request> e = i.next();
		Coord c = e.getKey();
		Request r = e.getValue();
		if(now - r.lastreq > 1000) {
		    r.lastreq = now;
		    if(++r.reqs >= 5) {
			i.remove();
		    } else {
			Message msg = new Message(Session.MSG_MAPREQ);
			msg.addcoord(c);
			sess.sendmsg(msg);
		    }
		}
	    }
	}
    }
}
