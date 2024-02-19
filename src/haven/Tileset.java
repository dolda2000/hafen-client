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
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import haven.render.*;
import static haven.MCache.tilesz;

@Resource.LayerName("tileset2")
public class Tileset extends Resource.Layer {
    private String tn = "gnd";
    public String[] tags = {};
    public Object[] ta = new Object[0];
    private transient Tiler.Factory tfac;
    public Collection<Indir<Flavor>> flavors = new ArrayList<>();
    public NodeWrap flavobjmat = null;
    public WeightList<Tile> ground;
    public WeightList<Tile>[] ctrans, btrans;
    public int flavprob;

    @Resource.LayerName("tile")
    public static class Tile extends Resource.Layer {
	transient BufferedImage img;
	transient private Tex tex;
	public final int id;
	public final int w;
	public final char t;

	public Tile(Resource res, Message buf) {
	    res.super();
	    t = (char)buf.uint8();
	    id = buf.uint8();
	    w = buf.uint16();
	    try {
		img = Resource.readimage(new MessageInputStream(buf));
	    } catch(IOException e) {
		throw(new Resource.LoadException(e, res));
	    }
	}

	public synchronized Tex tex() {
	    if(tex == null)
		tex = new TexI(img);
	    return(tex);
	}

	public void init() {}
    }

    public static interface Flavor {
	public static class Obj extends Gob {
	    private final long seed;

	    public Obj(Buffer buf, Coord2d c, double a) {
		super(buf.glob, c);
		this.a = a;
		Coord2d ul = Coord2d.of(buf.area.ul).mul(tilesz);
		Random r = new Random(buf.seed);
		r.setSeed(r.nextLong() ^ Double.doubleToLongBits(rc.x - ul.x));
		this.seed = r.nextLong() ^ Double.doubleToLongBits(rc.y - ul.y);
	    }

	    public Random mkrandoom() {
		return(new Random(seed));
	    }
	}

	public static class Buffer {
	    public final Glob glob;
	    public final Area area;
	    public final long seed;
	    final Map<NodeWrap, Collection<Gob>> mats = new HashMap<>();
	    private final Map<Object, Object> data = new IdentityHashMap<>();
	    private final Collection<Runnable> finish = new LinkedList<>();

	    public Buffer(Glob glob, Area area, long seed) {
		this.glob = glob;
		this.area = area;
		this.seed = seed;
	    }

	    public Collection<Gob> matslot(NodeWrap mat) {
		Collection<Gob> ret = mats.get(mat);
		if(ret == null)
		    mats.put(mat, ret = new ArrayList<>());
		return(ret);
	    }

	    public void add(Gob ob, NodeWrap mat) {
		matslot(mat).add(ob);
	    }

	    public void add(Gob ob) {
		add(ob, null);
	    }

	    @SuppressWarnings("unchecked")
	    public <T> T datum(Function<Buffer, T> id) {
		T ret = (T)data.get(id);
		if(ret == null)
		    data.put(id, ret = id.apply(this));
		return(ret);
	    }

	    public void finish(Runnable act) {
		finish.add(act);
	    }

	    public void finish() {
		for(Runnable act = Utils.take(finish); act != null; act = Utils.take(finish))
		    act.run();
	    }
	}

	public static class Terrain implements MapSource {
	    public final MapSource grid, map;
	    public final int tile;
	    public final Area area;
	    private final Coord toff;

	    public Terrain(MapSource grid, MapSource map, int tile, Area area, Coord toff) {
		this.grid = grid;
		this.map = map;
		this.tile = tile;
		this.area = area;
		this.toff = toff;
	    }

	    private boolean[] mask = null;
	    public boolean[] mask() {
		if(mask == null) {
		    mask = new boolean[area.area()];
		    int o = 0;
		    for(int y = area.ul.y; y < area.br.y; y++) {
			for(int x = area.ul.x; x < area.br.x; x++) {
			    mask[o++] = gettile(Coord.of(x, y)) == tile;
			}
		    }
		}
		return(mask);
	    }

	    private List<Coord> tiles = null;
	    public List<Coord> tiles() {
		if(tiles == null) {
		    int[] X = new int[area.area()], Y = new int[area.area()];
		    int n = 0;
		    for(int y = area.ul.y; y < area.br.y; y++) {
			for(int x = area.ul.x; x < area.br.x; x++) {
			    if(gettile(Coord.of(x, y)) == tile) {
				X[n] = x;
				Y[n] = y;
				n++;
			    }
			}
		    }
		    int N = n;
		    tiles = new AbstractList<Coord>() {
			    public int size() {
				return(N);
			    }

			    public Coord get(int i) {
				if((i < 0) || (i >= N))
				    throw(new IndexOutOfBoundsException(String.format("%s/%s", i, N)));
				return(Coord.of(X[i], Y[i]));
			    }
			};
		}
		return(tiles);
	    }

	    public int gettile(Coord tc) {return(grid.gettile(tc.add(toff)));}
	    public double getfz(Coord tc) {return(grid.getfz(tc.add(toff)));}
	    public Tileset tileset(int t) {return(grid.tileset(t));}
	    public Tiler tiler(int t) {return(grid.tiler(t));}
	}

	public void flavor(Buffer buf, Terrain trn, Random seed);

	@Resource.PublishedCode(name = "flavor", instancer = FactMaker.class)
	public static interface Factory {
	    public Flavor make(Tileset trn, Object... args);
	}

	public static class FactMaker extends Resource.PublishedCode.Instancer.Chain<Factory> {
	    public FactMaker() {super(Factory.class);}
	    {
		add(new Direct<>(Factory.class));
		add(new StaticCall<>(Factory.class, "mkflavor", Flavor.class, new Class<?>[] {Tileset.class, Object[].class},
				     (make) -> new Factory() {
					     public Flavor make(Tileset trn, Object... args) {
						 return(make.apply(new Object[]{trn, args}));
					     }
					 }));
		add(new Construct<>(Factory.class, Flavor.class, new Class<?>[] {Tileset.class, Object[].class},
				    (cons) -> new Factory() {
					    public Flavor make(Tileset trn, Object... args) {
						return(cons.apply(new Object[] {trn, args}));
					    }
					}));
		add(new Construct<>(Factory.class, Flavor.class, new Class<?>[] {Object[].class},
				    (cons) -> new Factory() {
					    public Flavor make(Tileset set, Object... args) {
						return(cons.apply(new Object[] {args}));
					    }
					}));
	    }
	}

	@Resource.LayerName("flavobj")
	public static class Res extends Resource.Layer implements Indir<Flavor> {
	    public final Indir<Resource> res;
	    public final Object[] args;
	    private Flavor flav;

	    public Res(Resource res, Message buf) {
		res.super();
		int ver = buf.uint8();
		if(ver == 1) {
		    this.res = new Resource.Spec(res.pool, buf.string(), buf.uint16());
		    this.args = buf.list();
		} else {
		    throw(new Resource.LoadException("unknown flavobj version: " + ver, res));
		}
	    }

	    public Flavor get() {
		if(this.flav == null) {
		    Resource res = this.res.get();
		    Factory ret = res.getcode(Factory.class, false);
		    if(ret != null)
			this.flav = ret.make(getres().flayer(Tileset.class), this.args);
		    else
			this.flav = new SpriteFlavor(this.res, Utils.dv(this.args[0]));
		}
		return(this.flav);
	    }

	    public void init() {}
	}
    }

    public static class SpriteFlavor implements Flavor {
	public final Indir<Resource> res;
	public final double p;

	public SpriteFlavor(Indir<Resource> res, double p) {
	    this.res = res;
	    this.p = p;
	}

	public void flavor(Buffer buf, Terrain trn, Random seed) {
	    Resource res = this.res.get();
	    DRandom trnd = new DRandom(new DRandom(seed).randl(res.name.hashCode(), trn.tile));
	    Random ornd = new Random();
	    Tileset set = trn.tileset(trn.tile);
	    for(Coord tc : trn.tiles()) {
		ornd.setSeed(trnd.randl(tc.x - trn.area.ul.x, tc.y - trn.area.ul.y));
		if(ornd.nextDouble() < p) {
		    Gob g = new Flavor.Obj(buf,
					   tc.mul(tilesz).add(tilesz.mul(ornd.nextDouble(), ornd.nextDouble())),
					   ornd.nextDouble() * 2 * Math.PI);
		    g.setattr(new ResDrawable(g, this.res, Message.nil));
		    buf.add(g, set.flavobjmat);
		}
	    }
	}
    }

    private Tileset(Resource res) {
	res.super();
    }

    public Tileset(Resource res, Message buf) {
	res.super();
	while(!buf.eom()) {
	    int p = buf.uint8();
	    switch(p) {
	    case 0:
		tn = buf.string();
		ta = buf.list();
		break;
	    case 1:
		int flnum = buf.uint16();
		flavprob = buf.uint16();
		List<Indir<Resource>> flr = new ArrayList<>();
		List<Integer> flw = new ArrayList<>();
		int twa = 0;
		for(int i = 0; i < flnum; i++) {
		    flr.add(res.pool.load(buf.string(), buf.uint16()));
		    int w = buf.uint8();
		    flw.add(w);
		    twa += w;
		}
		/* XXX: Bug-for-bug compatibility */
		flw.set(0, flw.get(0) + twa);
		twa += twa;
		int tw = twa;
		for(int i = 0; i < flnum; i++) {
		    Indir<Resource> fres = flr.get(i);
		    int w = flw.get(i);
		    flavors.add(Utils.cache(() -> new SpriteFlavor(fres, (double)w / (double)(flavprob * tw))));
		}
		break;
	    case 2:
		tags = new String[buf.int8()];
		for(int i = 0; i < tags.length; i++)
		    tags[i] = buf.string();
		Arrays.sort(tags);
		break;
	    default:
		throw(new Resource.LoadException("Invalid tileset part " + p + "  in " + res.name, res));
	    }
	}
    }

    public Tiler.Factory tfac() {
	synchronized(this) {
	    if(tfac == null) {
		Resource.CodeEntry ent = getres().layer(Resource.CodeEntry.class);
		if(ent != null) {
		    tfac = ent.get(Tiler.Factory.class);
		} else {
		    if((tfac = Tiler.byname(tn)) == null)
			throw(new RuntimeException("Invalid tiler name in " + getres().name + ": " + tn));
		}
	    }
	    return(tfac);
	}
    }

    private void packtiles(Collection<Tile> tiles, Coord tsz) {
	if(tiles.size() < 1)
	    return;
	int min = -1, minw = -1, minh = -1, mine = -1;
	final int nt = tiles.size();
	for(int i = 1; i <= nt; i++) {
	    int w = Tex.nextp2(tsz.x * i);
	    int h;
	    if((nt % i) == 0)
		h = nt / i;
	    else
		h = (nt / i) + 1;
	    h = Tex.nextp2(tsz.y * h);
	    int a = w * h;
	    int e = (w < h)?h:w;
	    if((min == -1) || (a < min) || ((a == min) && (e < mine))) {
		min = a;
		minw = w;
		minh = h;
		mine = e;
	    }
	}
	final Tile[] order = new Tile[nt];
	final Coord[] place = new Coord[nt];
	Tex packbuf = new TexL(new Coord(minw, minh)) {
		{
		    mipmap(Mipmapper.avg);
		    img.minfilter(Texture.Filter.NEAREST).mipfilter(Texture.Filter.LINEAR);
		    img.magfilter(Texture.Filter.NEAREST);
		    centroid = true;
		}

		public BufferedImage fill() {
		    BufferedImage buf = TexI.mkbuf(sz());
		    Graphics g = buf.createGraphics();
		    for(int i = 0; i < nt; i++)
			g.drawImage(order[i].img, place[i].x, place[i].y, null);
		    g.dispose();
		    return(buf);
		}

		public String toString() {
		    return("TileTex(" + getres().name + ")");
		}

		public String loadname() {
		    return("tileset in " + getres().name);
		}
	    };
	int x = 0, y = 0, n = 0;
	for(Tile t :  tiles) {
	    if(y >= minh)
		throw(new Resource.LoadException("Could not pack tiles into calculated minimum texture", getres()));
	    order[n] = t;
	    place[n] = new Coord(x, y);
	    t.tex = new TexSI(packbuf, place[n], place[n].add(tsz));
	    n++;
	    if((x += tsz.x) > (minw - tsz.x)) {
		x = 0;
		y += tsz.y;
	    }
	}
    }

    @SuppressWarnings("unchecked")
    public void init() {
	WeightList<Tile> ground = new WeightList<Tile>();
	WeightList<Tile>[] ctrans = new WeightList[15];
	WeightList<Tile>[] btrans = new WeightList[15];
	for(int i = 0; i < 15; i++) {
	    ctrans[i] = new WeightList<Tile>();
	    btrans[i] = new WeightList<Tile>();
	}
	int cn = 0, bn = 0;
	Collection<Tile> tiles = new LinkedList<Tile>();
	Coord tsz = null;
	for(Tile t : getres().layers(Tile.class)) {
	    if(t.t == 'g') {
		ground.add(t, t.w);
	    } else if(t.t == 'b') {
		btrans[t.id - 1].add(t, t.w);
		bn++;
	    } else if(t.t == 'c') {
		ctrans[t.id - 1].add(t, t.w);
		cn++;
	    }
	    tiles.add(t);
	    if(tsz == null) {
		tsz = Utils.imgsz(t.img);
	    } else {
		if(!Utils.imgsz(t.img).equals(tsz)) {
		    throw(new Resource.LoadException("Different tile sizes within set", getres()));
		}
	    }
	}
	if(ground.size() > 0)
	    this.ground = ground;
	if(cn > 0)
	    this.ctrans = ctrans;
	if(bn > 0)
	    this.btrans = btrans;
	packtiles(tiles, tsz);

	boolean has = false;
	for(Flavor.Res fr : getres().layers(Flavor.Res.class)) {
	    if(!has) {
		flavors.clear();
		has = true;
	    }
	    flavors.add(fr);
	}
    }
}
