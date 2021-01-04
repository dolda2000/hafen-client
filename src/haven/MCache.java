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
import java.lang.ref.*;
import haven.render.*;

/* XXX: This whole file is a bit of a mess and could use a bit of a
 * rewrite some rainy day. Synchronization especially is quite hairy. */
public class MCache implements MapSource {
    public static final Coord2d tilesz = new Coord2d(11, 11);
    public static final Coord tilesz2 = tilesz.round(); /* XXX: Remove me in due time. */
    public static final Coord cmaps = new Coord(100, 100);
    public static final Coord cutsz = new Coord(25, 25);
    public static final Coord cutn = cmaps.div(cutsz);
    public final Resource.Spec[] nsets = new Resource.Spec[256];
    @SuppressWarnings("unchecked")
    private final Reference<Resource>[] sets = new Reference[256];
    @SuppressWarnings("unchecked")
    private final Reference<Tileset>[] csets = new Reference[256];
    @SuppressWarnings("unchecked")
    private final Reference<Tiler>[] tiles = new Reference[256];
    private final Waitable.Queue gridwait = new Waitable.Queue();
    Map<Coord, Request> req = new HashMap<Coord, Request>();
    Map<Coord, Grid> grids = new HashMap<Coord, Grid>();
    Session sess;
    Set<Overlay> ols = new HashSet<Overlay>();
    public int olseq = 0;
    Map<Integer, Defrag> fragbufs = new TreeMap<Integer, Defrag>();

    public static class LoadingMap extends Loading {
	public final Coord gc;
	private transient final MCache map;

	public LoadingMap(MCache map, Coord gc) {
	    super("Waiting for map data...");
	    this.gc = gc;
	    this.map = map;
	}

	public void waitfor(Runnable callback, Consumer<Waitable.Waiting> reg) {
	    synchronized(map.grids) {
		if(map.grids.containsKey(gc)) {
		    reg.accept(Waitable.Waiting.dummy);
		    callback.run();
		} else {
		    reg.accept(new Waitable.Checker(callback) {
			    protected Object monitor() {return(map.grids);}
			    double st = Utils.rtime();
			    protected boolean check() {
				if((Utils.rtime() - st > 5)) {
				    st = Utils.rtime();
				    return(true);
				}
				return(map.grids.containsKey(gc));
			    }
			    protected Waitable.Waiting add() {return(map.gridwait.add(this));}
			}.addi());
		}
	    }
	}
    }

    private static class Request {
	private long lastreq = 0;
	private int reqs = 0;
    }

    public static interface OverlayInfo {
	public Collection<String> tags();
	public Material mat();
    }

    @Resource.LayerName("overlay")
    public static class ResOverlay extends Resource.Layer implements OverlayInfo {
	public final Collection<String> tags;
	private final int matid;

	public ResOverlay(Resource res, Message buf) {
	    res.super();
	    int ver = buf.uint8();
	    if(ver == 1) {
		int matid = 0;
		Collection<String> tags = Collections.emptyList();
		Object[] data = buf.list();
		for(Object argp : data) {
		    Object[] arg = (Object[])argp;
		    switch((String)arg[0]) {
		    case "tags": {
			ArrayList<String> tbuf = new ArrayList<>();
			for(int i = 1; i < arg.length; i++)
			    tbuf.add(((String)arg[i]).intern());
			tbuf.trimToSize();
			tags = tbuf;
			break;
		    }
		    case "mat": {
			matid = (Integer)arg[1];
			break;
		    }
		    }
		}
		this.matid = matid;
		this.tags = tags;
	    } else {
		throw(new Resource.LoadException("unknown overlay version: " + ver, res));
	    }
	}

	public void init() {
	}

	public Collection<String> tags() {
	    return(tags);
	}

	public Material mat() {
	    return(getres().layer(Material.Res.class, matid).get());
	}

	public String toString() {
	    return(String.format("#<res-overlay %s %d>", getres().name, matid));
	}
    }

    public class Overlay {
	private Area a;
	private OverlayInfo id;

	public Overlay(Area a, OverlayInfo id) {
	    this.a = a;
	    this.id = id;
	    ols.add(this);
	    olseq++;
	}

	@Deprecated public Overlay(Coord c1, Coord c2, int mask) {
	    this(new Area(c1, c2.add(1, 1)), olres.get(Integer.numberOfTrailingZeros(mask)).get().layer(ResOverlay.class));
	}

	public void destroy() {
	    ols.remove(this);
	    olseq++;
	}

	public void update(Area a) {
	    if(!a.equals(this.a)) {
		olseq++;
		this.a = a;
	    }
	}

	@Deprecated public void update(Coord c1, Coord c2) {
	    update(new Area(c1, c2.add(1, 1)));
	}
    }

    /* XXX: To be abolished */
    public static final Map<Integer, Indir<Resource>> olres = Utils.<Integer, Indir<Resource>>map()
	.put(0, Resource.remote().load("gfx/tiles/overlay/cplot-f"))
	.put(1, Resource.remote().load("gfx/tiles/overlay/cplot-o"))
	.put(2, Resource.remote().load("gfx/tiles/overlay/vlg-f"))
	.put(3, Resource.remote().load("gfx/tiles/overlay/vlg-o"))
	.put(4, Resource.remote().load("gfx/tiles/overlay/realm-f"))
	.put(5, Resource.remote().load("gfx/tiles/overlay/realm-o"))
	.put(16, Resource.remote().load("gfx/tiles/overlay/cplot-s"))
	.put(17, Resource.remote().load("gfx/tiles/overlay/sel"))
	.map();
    public class Grid {
	public final Coord gc, ul;
	public final int tiles[] = new int[cmaps.x * cmaps.y];
	public final float z[] = new float[cmaps.x * cmaps.y];
	public Indir<Resource> ols[];
	public boolean ol[][];
	public long id;
	public int seq = -1;
	public String mnm;
	private int olseq = -1;
	private final Cut cuts[];
	private Flavobjs[] fo = new Flavobjs[cutn.x * cutn.y];

	private class Cut {
	    MapMesh mesh;
	    Defer.Future<MapMesh> dmesh;
	    Map<OverlayInfo, RenderTree.Node> ols = new HashMap<>();
	}

	private class Flavobj extends Gob {
	    private Flavobj(Coord2d c, double a) {
		super(sess.glob, c);
		this.a = a;
	    }

	    public Random mkrandoom() {
		Random r = new Random(Grid.this.id);
		r.setSeed(r.nextLong() ^ Double.doubleToLongBits(rc.x));
		r.setSeed(r.nextLong() ^ Double.doubleToLongBits(rc.y));
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

	public double getz(Coord tc) {
	    return(z[tc.x + (tc.y * cmaps.x)]);
	}

	public void getol(OverlayInfo id, Area a, boolean[] buf) {
	    for(int i = 0; i < ols.length; i++) {
		if(ols[i].get().layer(ResOverlay.class) == id) {
		    int o = 0;
		    for(Coord c : a)
			buf[o++] = ol[i][c.x + (c.y * cmaps.x)];
		    return;
		}
	    }
	    for(int o = 0; o < buf.length; o++)
		buf[o] = false;
	}

	private class Flavobjs implements RenderTree.Node {
	    final RenderTree.Node[] mats;
	    final Gob[] all;

	    Flavobjs(Map<NodeWrap, Collection<Gob>> flavobjs) {
		Collection<Gob> all = new ArrayList<>();
		RenderTree.Node[] mats = new RenderTree.Node[flavobjs.size()];
		int i = 0;
		for(Map.Entry<NodeWrap, Collection<Gob>> matent : flavobjs.entrySet()) {
		    final NodeWrap mat = matent.getKey();
		    Collection<Gob> fos = matent.getValue();
		    final Gob[] fol = fos.toArray(new Gob[0]);
		    all.addAll(fos);
		    mats[i] = new RenderTree.Node() {
			    public void added(RenderTree.Slot slot) {
				for(Gob fo : fol)
				    slot.add(fo.placed);
			    }
			};
		    if(mat != null)
			mats[i] = mat.apply(mats[i]);
		    i++;
		}
		this.mats = mats;
		this.all = all.toArray(new Gob[0]);
	    }

	    public void added(RenderTree.Slot slot) {
		for(RenderTree.Node mat : mats)
		    slot.add(mat);
	    }

	    void tick(double dt) {
		for(Gob fo : all)
		    fo.ctick(dt);
	    }

	    void gtick(Render g) {
		for(Gob fo : all)
		    fo.gtick(g);
	    }
	}

	private Flavobjs makeflavor(Coord cutc) {
	    Map<NodeWrap, Collection<Gob>> buf = new HashMap<>();
	    Coord o = new Coord(0, 0);
	    Coord ul = cutc.mul(cutsz);
	    Coord gul = ul.add(gc.mul(cmaps));
	    int i = ul.x + (ul.y * cmaps.x);
	    Random rnd = new Random(id + cutc.x + (cutc.y * cutn.x));
	    for(o.y = 0; o.y < cutsz.x; o.y++, i += (cmaps.x - cutsz.x)) {
		for(o.x = 0; o.x < cutsz.y; o.x++, i++) {
		    Tileset set = tileset(tiles[i]);
		    Collection<Gob> mbuf = buf.get(set.flavobjmat);
		    if(mbuf == null)
			buf.put(set.flavobjmat, mbuf = new ArrayList<>());
		    int fp = rnd.nextInt();
		    int rp = rnd.nextInt();
		    double a = rnd.nextDouble();
		    if(set.flavobjs.size() > 0) {
			if((fp % set.flavprob) == 0) {
			    Indir<Resource> r = set.flavobjs.pick(rp % set.flavobjs.tw);
			    Gob g = new Flavobj(o.add(gul).mul(tilesz).add(tilesz.div(2)), a * 2 * Math.PI);
			    g.setattr(new ResDrawable(g, r, Message.nil));
			    mbuf.add(g);
			}
		    }
		}
	    }
	    return(new Flavobjs(buf));
	}

	public RenderTree.Node getfo(Coord cc) {
	    int foo = cc.x + (cc.y * cutn.x);
	    if(fo[foo] == null)
		fo[foo] = makeflavor(cc);
	    return(fo[foo]);
	}
	
	private Cut geticut(Coord cc) {
	    return(cuts[cc.x + (cc.y * cutn.x)]);
	}

	public MapMesh getcut(Coord cc) {
	    Cut cut = geticut(cc);
	    if(cut.dmesh != null) {
		if(cut.dmesh.done() || (cut.mesh == null)) {
		    MapMesh old = cut.mesh;
		    cut.mesh = cut.dmesh.get();
		    cut.dmesh = null;
		    cut.ols.clear();
		    if(old != null)
			old.dispose();
		}
	    }
	    return(cut.mesh);
	}
	
	public RenderTree.Node getolcut(OverlayInfo id, Coord cc) {
	    int nseq = MCache.this.olseq;
	    if(this.olseq != nseq) {
		for(int i = 0; i < cutn.x * cutn.y; i++) {
		    for(RenderTree.Node r : cuts[i].ols.values()) {
			if(r instanceof Disposable)
			    ((Disposable)r).dispose();
		    }
		    cuts[i].ols.clear();
		}
		this.olseq = nseq;
	    }
	    Cut cut = geticut(cc);
	    if(!cut.ols.containsKey(id))
		cut.ols.put(id, getcut(cc).makeol(id));
	    return(cut.ols.get(id));
	}
	
	private void buildcut(final Coord cc) {
	    final Cut cut = geticut(cc);
	    Defer.Future<?> prev = cut.dmesh;
	    cut.dmesh = Defer.later(new Defer.Callable<MapMesh>() {
		    public MapMesh call() {
			Random rnd = new Random(id);
			rnd.setSeed(rnd.nextInt() ^ cc.x);
			rnd.setSeed(rnd.nextInt() ^ cc.y);
			return(MapMesh.build(MCache.this, rnd, ul.add(cc.mul(cutsz)), cutsz));
		    }

		    public String toString() {
			return("Building map...");
		    }
		});
	    if(prev != null)
		prev.cancel();
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
	
	public void tick(double dt) {
	    for(Flavobjs fol : fo) {
		if(fol != null)
		    fol.tick(dt);
	    }
	}
	
	public void gtick(Render g) {
	    for(Flavobjs fol : fo) {
		if(fol != null)
		    fol.gtick(g);
	    }
	}
	
	private void invalidate() {
	    for(int y = 0; y < cutn.y; y++) {
		for(int x = 0; x < cutn.x; x++)
		    buildcut(new Coord(x, y));
	    }
	    fo = new Flavobjs[cutn.x * cutn.y];
	    for(Coord ic : new Coord[] {
		    new Coord(-1, -1), new Coord( 0, -1), new Coord( 1, -1),
		    new Coord(-1,  0),                    new Coord( 1,  0),
		    new Coord(-1,  1), new Coord( 0,  1), new Coord( 1,  1)}) {
		Grid ng = grids.get(gc.add(ic));
		if(ng != null)
		    ng.ivneigh(ic.inv());
	    }
	}

	public void dispose() {
	    for(Cut cut : cuts) {
		if(cut.dmesh != null)
		    cut.dmesh.cancel();
		if(cut.mesh != null)
		    cut.mesh.dispose();
		for(RenderTree.Node r : cut.ols.values()) {
		    if(r instanceof Disposable)
			((Disposable)r).dispose();
		}
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
	    Message blob = new ZMessage(msg);
	    id = blob.int64();
	    while(true) {
		int tileid = blob.uint8();
		if(tileid == 255)
		    break;
		String resnm = blob.string();
		int resver = blob.uint16();
		nsets[tileid] = new Resource.Spec(Resource.remote(), resnm, resver);
	    }
	    for(int i = 0; i < tiles.length; i++)
		tiles[i] = blob.uint8();
	    for(int i = 0; i < z.length; i++)
		z[i] = blob.int16();
	    @SuppressWarnings("unchecked")
	    Indir<Resource>[] olids = new Indir[0];
	    boolean[][] ols = {};
	    while(true) {
		int pidx = blob.uint8();
		if(pidx == 255)
		    break;
		int fl = pfl[pidx];
		int type = blob.uint8();
		Coord c1 = new Coord(blob.uint8(), blob.uint8());
		Coord c2 = new Coord(blob.uint8(), blob.uint8());
		Indir<Resource> olid;
		if(type == 0) {
		    if((fl & 1) == 1)
			olid = olres.get(1);
		    else
			olid = olres.get(0);
		} else if(type == 1) {
		    if((fl & 1) == 1)
			olid = olres.get(3);
		    else
			olid = olres.get(2);
		} else if(type == 2) {
		    if((fl & 1) == 1)
			olid = olres.get(5);
		    else
			olid = olres.get(4);
		} else {
		    throw(new RuntimeException("Unknown plot type " + type));
		}
		int oi;
		find: {
		    for(oi = 0; oi < olids.length; oi++) {
			if(olids[oi] == olid)
			    break find;
		    }
		    olids = Arrays.copyOf(olids, oi + 1);
		    ols = Arrays.copyOf(ols, oi + 1);
		    olids[oi] = olid;
		}
		boolean[] ol = ols[oi];
		if(ol == null)
		    ols[oi] = ol = new boolean[cmaps.x * cmaps.y];
		for(int y = c1.y; y <= c2.y; y++) {
		    for(int x = c1.x; x <= c2.x; x++) {
			ol[x + (y * cmaps.x)] = true;
		    }
		}
	    }
	    this.ols = olids;
	    this.ol = ols;
	    invalidate();
	    seq++;
	}
    }

    public MCache(Session sess) {
	this.sess = sess;
    }

    public void ctick(double dt) {
	Collection<Grid> copy;
	synchronized(grids) {
	    copy = new ArrayList<>(grids.values());
	}
	for(Grid g : copy)
	    g.tick(dt);
    }

    public void gtick(Render g) {
	Collection<Grid> copy;
	synchronized(grids) {
	    copy = new ArrayList<>(grids.values());
	}
	for(Grid gr : copy)
	    gr.gtick(g);
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
	    synchronized(this) {
		trim(ul, lr);
	    }
	} else if(type == 2) {
	    synchronized(this) {
		trimall();
	    }
	}
    }

    private Grid cached = null;
    public Grid getgrid(Coord gc) {
	synchronized(grids) {
	    if((cached == null) || !cached.gc.equals(gc)) {
		cached = grids.get(gc);
		if(cached == null) {
		    request(gc);
		    throw(new LoadingMap(this, gc));
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

    public double getfz(Coord tc) {
	Grid g = getgridt(tc);
	return(g.getz(tc.sub(g.ul)));
    }

    @Deprecated
    public int getz(Coord tc) {
	return((int)Math.round(getfz(tc)));
    }

    public double getcz(double px, double py) {
	double tw = tilesz.x, th = tilesz.y;
	Coord ul = new Coord(Utils.floordiv(px, tw), Utils.floordiv(py, th));
	double sx = Utils.floormod(px, tw) / tw;
	double sy = Utils.floormod(py, th) / th;
	return(((1.0f - sy) * (((1.0f - sx) * getfz(ul)) + (sx * getfz(ul.add(1, 0))))) +
	       (sy * (((1.0f - sx) * getfz(ul.add(0, 1))) + (sx * getfz(ul.add(1, 1))))));
    }

    public double getcz(Coord2d pc) {
	return(getcz(pc.x, pc.y));
    }

    public float getcz(float px, float py) {
	return((float)getcz((double)px, (double)py));
    }

    public float getcz(Coord pc) {
	return(getcz(pc.x, pc.y));
    }

    public Coord3f getzp(Coord2d pc) {
	return(new Coord3f((float)pc.x, (float)pc.y, (float)getcz(pc)));
    }

    public Collection<OverlayInfo> getols(Area a) {
	Collection<OverlayInfo> ret = new ArrayList<>();
	for(Coord gc : a.div(cmaps)) {
	    Grid g = getgrid(gc);
	    for(Indir<Resource> res : g.ols) {
		OverlayInfo id = res.get().layer(ResOverlay.class);
		if(!ret.contains(id))
		    ret.add(id);
	    }
	}
	for(Overlay lol : ols) {
	    if((lol.a.overlap(a) != null) && !ret.contains(lol.id))
		ret.add(lol.id);
	}
	return(ret);
    }

    public void getol(OverlayInfo id, Area a, boolean[] buf) {
	Area ga = a.div(cmaps);
	if(ga.area() == 1) {
	    Grid g = getgrid(ga.ul);
	    g.getol(id, a.xl(g.ul.inv()), buf);
	} else {
	    boolean[] gbuf = new boolean[cmaps.x * cmaps.y];
	    for(Coord gc : ga) {
		Grid g = getgrid(gc);
		Area gt = Area.sized(g.ul, cmaps);
		g.getol(id, Area.sized(Coord.z, cmaps), gbuf);
		for(Coord tc : a.overlap(gt))
		    buf[a.ri(tc)] = gbuf[(tc.x - gt.ul.x) + ((tc.y - gt.ul.y) * cmaps.x)];
	    }
	}
	for(Overlay lol : ols) {
	    if(lol.id != id)
		continue;
	    Area la = lol.a.overlap(a);
	    if(la != null) {
		for(Coord lc : la)
		    buf[a.ri(lc)] = true;
	    }
	}
    }
    
    public MapMesh getcut(Coord cc) {
	synchronized(grids) {
	    return(getgrid(cc.div(cutn)).getcut(cc.mod(cutn)));
	}
    }
    
    public RenderTree.Node getfo(Coord cc) {
	synchronized(grids) {
	    return(getgrid(cc.div(cutn)).getfo(cc.mod(cutn)));
	}
    }

    public RenderTree.Node getolcut(OverlayInfo id, Coord cc) {
	synchronized(grids) {
	    return(getgrid(cc.div(cutn)).getolcut(id, cc.mod(cutn)));
	}
    }

    public void mapdata2(Message msg) {
	Coord c = msg.coord();
	synchronized(grids) {
	    synchronized(req) {
		if(req.containsKey(c)) {
		    Grid g = grids.get(c);
		    if(g == null) {
			grids.put(c, g = new Grid(c));
			cached = null;
		    }
		    g.fill(msg);
		    req.remove(c);
		    olseq++;
		    gridwait.wnotify();
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
	    fragbuf.add(msg.bytes(), off);
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

    public Resource tilesetr(int i) {
	synchronized(sets) {
	    Resource res = (sets[i] == null)?null:(sets[i].get());
	    if(res == null) {
		if(nsets[i] == null)
		    return(null);
		res = nsets[i].get();
		sets[i] = new SoftReference<Resource>(res);
	    }
	    return(res);
	}
    }

    public Tileset tileset(int i) {
	synchronized(csets) {
	    Tileset cset = (csets[i] == null)?null:(csets[i].get());
	    if(cset == null) {
		Resource res = tilesetr(i);
		if(res == null)
		    return(null);
		csets[i] = new SoftReference<Tileset>(cset = res.layer(Tileset.class));
	    }
	    return(cset);
	}
    }

    public Tiler tiler(int i) {
	synchronized(tiles) {
	    Tiler tile = (tiles[i] == null)?null:(tiles[i].get());
	    if(tile == null) {
		Tileset set = tileset(i);
		if(set == null)
		    return(null);
		tile = set.tfac().create(i, set);
		tiles[i] = new SoftReference<Tiler>(tile);
	    }
	    return(tile);
	}
    }

    public void trimall() {
	synchronized(grids) {
	    synchronized(req) {
		for(Grid g : grids.values())
		    g.dispose();
		grids.clear();
		req.clear();
		cached = null;
	    }
	    gridwait.wnotify();
	}
    }

    public void trim(Coord ul, Coord lr) {
	synchronized(grids) {
	    synchronized(req) {
		for(Iterator<Map.Entry<Coord, Grid>> i = grids.entrySet().iterator(); i.hasNext();) {
		    Map.Entry<Coord, Grid> e = i.next();
		    Coord gc = e.getKey();
		    Grid g = e.getValue();
		    if((gc.x < ul.x) || (gc.y < ul.y) || (gc.x > lr.x) || (gc.y > lr.y)) {
			g.dispose();
			i.remove();
		    }
		}
		for(Iterator<Coord> i = req.keySet().iterator(); i.hasNext();) {
		    Coord gc = i.next();
		    if((gc.x < ul.x) || (gc.y < ul.y) || (gc.x > lr.x) || (gc.y > lr.y))
			i.remove();
		}
		cached = null;
	    }
	    gridwait.wnotify();
	}
    }

    public void request(Coord gc) {
	synchronized(req) {
	    if(!req.containsKey(gc))
		req.put(new Coord(gc), new Request());
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
	boolean updated = false;
	synchronized(req) {
	    for(Iterator<Map.Entry<Coord, Request>> i = req.entrySet().iterator(); i.hasNext();) {
		Map.Entry<Coord, Request> e = i.next();
		Coord c = e.getKey();
		Request r = e.getValue();
		if(now - r.lastreq > 1000) {
		    r.lastreq = now;
		    if(++r.reqs >= 5) {
			i.remove();
			updated = true;
		    } else {
			PMessage msg = new PMessage(Session.MSG_MAPREQ);
			msg.addcoord(c);
			sess.sendmsg(msg);
		    }
		}
	    }
	}
	if(updated) {
	    synchronized(grids) {
		gridwait.wnotify();
	    }
	}
    }
}
