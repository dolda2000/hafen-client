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

import static haven.MCache.tilesz;
import java.util.*;
import java.awt.Color;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import haven.render.*;
import haven.Surface.Vertex;
import haven.Surface.MeshVertex;
import haven.render.Rendered.Order;

public class MapMesh implements RenderTree.Node, Disposable {
    public final Coord ul, sz;
    public final MCache map;
    public FastMesh flat;
    private final long rnd;
    private Map<DataID, Object> data = new LinkedHashMap<DataID, Object>();
    private List<RenderTree.Node> extras = new ArrayList<RenderTree.Node>();
    private List<Disposable> dparts = new ArrayList<Disposable>();

    public interface DataID<T> {
	public T make(MapMesh m);
    }

    public static <T> DataID<T> makeid(Class<T> cl) {
	try {
	    final java.lang.reflect.Constructor<T> cons = cl.getConstructor(MapMesh.class);
	    return(new DataID<T>() {
		    public T make(MapMesh m) {
			return(Utils.construct(cons, m));
		    }
		});
	} catch(NoSuchMethodException e) {}
	try {
	    final java.lang.reflect.Constructor<T> cons = cl.getConstructor();
	    return(new DataID<T>() {
		    public T make(MapMesh m) {
			return(Utils.construct(cons));
		    }
		});
	} catch(NoSuchMethodException e) {}
	throw(new Error("No proper data-ID constructor found"));
    }

    public static interface ConsHooks {
	public void sfin();
	public void calcnrm();
	public void postcalcnrm(Random rnd);
	public boolean clean();
    }

    public static class Hooks implements ConsHooks {
	public void sfin() {};
	public void calcnrm() {}
	public void postcalcnrm(Random rnd) {}
	public boolean clean() {return(false);}
    }

    @SuppressWarnings("unchecked")
    public <T> T data(DataID<T> id) {
	T ret = (T)data.get(id);
	if(ret == null)
	    data.put(id, ret = id.make(this));
	return(ret);
    }

    public static class Scan {
        public final Coord ul, sz, br;
        public final int l;

        public Scan(Coord ul, Coord sz) {
            this.ul = ul;
            this.sz = sz;
            this.br = sz.add(ul);
            this.l = sz.x * sz.y;
        }

        public int o(int x, int y) {
            return((x - ul.x) + ((y - ul.y) * sz.x));
        }

        public int o(Coord in) {
            return(o(in.x, in.y));
        }
    }

    public class MapSurface extends haven.Surface implements ConsHooks {
	public final Scan vs = new Scan(new Coord(-1, -1), sz.add(3, 3));
	public final Scan ts = new Scan(Coord.z, sz);
	public final Vertex[] surf = new Vertex[vs.l];
	public final boolean[] split = new boolean[ts.l];

	public MapSurface() {
	    for(int y = vs.ul.y; y < vs.br.y; y++) {
		for(int x = vs.ul.x; x < vs.br.x; x++) {
		    surf[vs.o(x, y)] = new Vertex(x * (float)tilesz.x, y * -(float)tilesz.y, (float)map.getfz(ul.add(x, y)));
		}
	    }
	    for(int y = ts.ul.y; y < ts.br.y; y++) {
		for(int x = ts.ul.x; x < ts.br.x; x++) {
		    split[ts.o(x, y)] = Math.abs(surf[vs.o(x, y)].z - surf[vs.o(x + 1, y + 1)].z) > Math.abs(surf[vs.o(x + 1, y)].z - surf[vs.o(x, y + 1)].z);
		}
	    }
	}

	public Vertex fortile(Coord c) {
	    return(surf[vs.o(c)]);
	}

	public Vertex[] fortilea(Coord c) {
	    return(new Vertex[] {
		    surf[vs.o(c.x, c.y)],
		    surf[vs.o(c.x, c.y + 1)],
		    surf[vs.o(c.x + 1, c.y + 1)],
		    surf[vs.o(c.x + 1, c.y)],
		});
	}

	public void sfin() {fin();}
	public void calcnrm() {}
	public void postcalcnrm(Random rnd) {}
	public boolean clean() {return(true);}
    }
    public static final DataID<MapSurface> gnd = makeid(MapSurface.class);

    public static class MLOrder extends Order<MLOrder> {
	public final int z;

	public MLOrder(int z, int subz) {
	    this.z = (z << 8) + subz;
	}

	public MLOrder(int z) {
	    this(z, 0);
	}

	public int mainorder() {
	    return(1000);
	}

	public boolean equals(Object x) {
	    return((x instanceof MLOrder) && (((MLOrder)x).z == this.z));
	}

	public int hashCode() {
	    return(z);
	}

	private final static Comparator<MLOrder> cmp = new Comparator<MLOrder>() {
	    public int compare(MLOrder a, MLOrder b) {
		return(a.z - b.z);
	    }
	};

	public Comparator<MLOrder> comparator() {return(cmp);}
    }
    public static Order premap = new Order.Default(990);
    public static Order postmap = new Order.Default(1010);

    private MapMesh(MCache map, Coord ul, Coord sz, Random rnd) {
	this.map = map;
	this.ul = ul;
	this.sz = sz;
	this.rnd = rnd.nextLong();
    }

    public Random rnd() {
	return(new Random(this.rnd));
    }

    public Random rnd(Coord c) {
	Random ret = rnd();
	ret.setSeed(ret.nextInt() + c.x);
	ret.setSeed(ret.nextInt() + c.y);
	return(ret);
    }

    public static Random grnd(Coord c) {
	Random ret = new Random(1192414289);
	ret.setSeed(ret.nextInt() + c.x);
	ret.setSeed(ret.nextInt() + c.y);
	return(ret);
    }

    private static void dotrans(MapMesh m, Random rnd, Coord lc, Coord gc) {
	Tiler ground = m.map.tiler(m.map.gettile(gc));
	int tr[][] = new int[3][3];
	int max = -1;
	for(int y = -1; y <= 1; y++) {
	    for(int x = -1; x <= 1; x++) {
		if((x == 0) && (y == 0))
		    continue;
		int tn = m.map.gettile(gc.add(x, y));
		tr[x + 1][y + 1] = tn;
		if(tn > max)
		    max = tn;
	    }
	}
	int bx[] = {0, 1, 2, 1};
	int by[] = {1, 0, 1, 2};
	int cx[] = {0, 2, 2, 0};
	int cy[] = {0, 0, 2, 2};
	for(int i = max; i >= 0; i--) {
	    int bm = 0, cm = 0;
	    for(int o = 0; o < 4; o++) {
		if(tr[bx[o]][by[o]] == i)
		    bm |= 1 << o;
	    }
	    for(int o = 0; o < 4; o++) {
		if((bm & ((1 << o) | (1 << ((o + 1) % 4)))) != 0)
		    continue;
		if(tr[cx[o]][cy[o]] == i)
		    cm |= 1 << o;
	    }
	    if((bm != 0) || (cm != 0)) {
		Tiler t = m.map.tiler(i);
		if(t == null)
		    continue;
		t.trans(m, rnd, ground, lc, gc, 255 - i, bm, cm);
	    }
	}
    }

    public static class Model extends MeshBuf implements ConsHooks {
	public final MapMesh m;
	public final NodeWrap mat;

	public Model(MapMesh m, NodeWrap mat) {
	    this.m = m;
	    this.mat = mat;
	}

	public void sfin() {}
	public void calcnrm() {}
	public boolean clean() {return(false);}

	public void postcalcnrm(Random rnd) {
	    FastMesh mesh = mkmesh();
	    m.extras.add(mat.apply(mesh));
	    m.dparts.add(mesh);
	}

	public static class MatKey implements DataID<Model> {
	    public final NodeWrap mat;
	    private final int hash;

	    public MatKey(NodeWrap mat) {
		this.mat = mat;
		this.hash = mat.hashCode() * 37;
	    }

	    public int hashCode() {
		return(hash);
	    }

	    public boolean equals(Object x) {
		return((x instanceof MatKey) && mat.equals(((MatKey)x).mat));
	    }

	    public Model make(MapMesh m) {
		return(new Model(m, mat));
	    }
	}

	public static Model get(MapMesh m, NodeWrap mat) {
	    return(m.data(new MatKey(mat)));
	}
    }

    public static MapMesh build(MCache mc, Random rnd, Coord ul, Coord sz) {
	MapMesh m = new MapMesh(mc, ul, sz, rnd);
	Coord c = new Coord();
	rnd = m.rnd();
	
	for(c.y = 0; c.y < sz.y; c.y++) {
	    for(c.x = 0; c.x < sz.x; c.x++) {
		Coord gc = c.add(ul);
		long ns = rnd.nextLong();
		mc.tiler(mc.gettile(gc)).model(m, rnd, c, gc);
		rnd.setSeed(ns);
	    }
	}
	for(Object obj : m.data.values()) {
	    if(obj instanceof ConsHooks)
		((ConsHooks)obj).sfin();
	}
	for(c.y = 0; c.y < sz.y; c.y++) {
	    for(c.x = 0; c.x < sz.x; c.x++) {
		Coord gc = c.add(ul);
		long ns = rnd.nextLong();
		mc.tiler(mc.gettile(gc)).lay(m, rnd, c, gc);
		dotrans(m, rnd, c, gc);
		rnd.setSeed(ns);
	    }
	}
	for(Object obj : m.data.values()) {
	    if(obj instanceof ConsHooks)
		((ConsHooks)obj).calcnrm();
	}
	for(Object obj : m.data.values()) {
	    if(obj instanceof ConsHooks)
		((ConsHooks)obj).postcalcnrm(rnd);
	}
	
	m.consflat();
	
	m.clean();
	return(m);
    }

    private static Pipe.Op gmmat = Pipe.Op.compose(new States.DepthBias(-1, -1),
						   new Order.Default(1001));
    public static RenderTree.Node groundmod(MCache map, Coord2d cc, Coord2d ul, Coord2d br, double a) {
	double si = Math.sin(a), co = Math.cos(a);
	MeshBuf buf = new MeshBuf();
	float cz = (float)map.getcz(cc);
	Coord ult, brt;
	{
	    Coord tult = null, tbrt = null;
	    for(Coord2d corn : new Coord2d[] {ul, new Coord2d(ul.x, br.y), br, new Coord2d(br.x, ul.y)}) {
		float cx = (float)((cc.x + co * corn.x - si * corn.y) / tilesz.x);
		float cy = (float)((cc.y + co * corn.y + si * corn.x) / tilesz.y);
		if(tult == null) {
		    tult = new Coord((int)Math.floor(cx), (int)Math.floor(cy));
		    tbrt = new Coord((int)Math.ceil(cx), (int)Math.ceil(cy));
		} else {
		    tult.x = Math.min(tult.x, (int)Math.floor(cx));
		    tult.y = Math.min(tult.y, (int)Math.floor(cy));
		    tbrt.x = Math.max(tbrt.x, (int)Math.ceil(cx));
		    tbrt.y = Math.max(tbrt.y, (int)Math.ceil(cy));
		}
	    }
	    ult = tult; brt = tbrt;
	}

	Tiler.MCons cons = new Tiler.MCons() {
		final MeshBuf.Tex ta = buf.layer(MeshBuf.tex);
		final Map<Vertex, MeshVertex> cv = new HashMap<>();

		public void faces(MapMesh m, Tiler.MPart d) {
		    Coord3f[] texc = new Coord3f[d.v.length];
		    for(int i = 0; i < d.v.length; i++) {
			texc[i] = new Coord3f((float)(((m.ul.x + d.lc.x + d.tcx[i]) * tilesz.x) - cc.x),
					      (float)(((m.ul.y + d.lc.y + d.tcy[i]) * tilesz.y) - cc.y),
					      0);
			texc[i] = new Coord3f((float)(co * texc[i].x + si * texc[i].y),
					      (float)(co * texc[i].y - si * texc[i].x),
					      0);
			texc[i].x = (float)((texc[i].x - ul.x) / (br.x - ul.x));
			texc[i].y = (float)((texc[i].y - ul.y) / (br.y - ul.y));
		    }

		    boolean[] vf = new boolean[d.f.length / 3];
		    boolean[] vv = new boolean[d.v.length];
		    for(int i = 0, o = 0; i < vf.length; i++, o += 3) {
			boolean f = true;
			int vs = 0, hs = 0;
			for(int u = 0; u < 3; u++) {
			    int vi = d.f[o + u];
			    int ch = (texc[vi].x < 0)?-1:((texc[vi].x > 1)?1:0);
			    int cv = (texc[vi].y < 0)?-1:((texc[vi].y > 1)?1:0);
			    boolean diff = false;
			    if(f) {
				hs = ch; vs = cv; f = false;
			    } else {
				diff = (hs != ch) || (vs != cv);
			    }
			    if(diff || ((ch == 0) && (cv == 0))) {
				vf[i] = true;
				for(int p = 0; p < 3; p++)
				    vv[d.f[o + p]] = true;
				break;
			    }
			}
		    }

		    MeshVertex[] mv = new MeshVertex[d.v.length];
		    for(int i = 0; i < d.v.length; i++) {
			if(!vv[i])
			    continue;
			if((mv[i] = cv.get(d.v[i])) == null) {
			    cv.put(d.v[i], mv[i] = new MeshVertex(buf, d.v[i]));
			    mv[i].pos = mv[i].pos.add((float)((m.ul.x * tilesz.x) - cc.x), (float)(cc.y - (m.ul.y * tilesz.y)), -cz);
			    ta.set(mv[i], texc[i]);
			}
		    }

		    for(int i = 0, o = 0; i < vf.length; i++, o += 3) {
			if(!vf[i])
			    continue;
			buf.new Face(mv[d.f[o]], mv[d.f[o + 1]], mv[d.f[o + 2]]);
		    }
		}
	    };

	Coord t = new Coord();
	for(t.y = ult.y; t.y < brt.y; t.y++) {
	    for(t.x = ult.x; t.x < brt.x; t.x++) {
		MapMesh cut = map.getcut(t.div(MCache.cutsz));
		Tiler tile = map.tiler(map.gettile(t));
		tile.lay(cut, t.sub(cut.ul), t, cons, false);
	    }
	}
	return(gmmat.apply(buf.mkmesh()));
    }

    public static class OLOrder extends Order<OLOrder> {
	public final MCache.OverlayInfo id;

	public OLOrder(MCache.OverlayInfo id) {
	    this.id = id;
	}

	public int mainorder() {
	    return(1002);
	}

	public boolean equals(Object x) {
	    return((x instanceof OLOrder) && (((OLOrder)x).id == this.id));
	}

	public int hashCode() {
	    return(System.identityHashCode(id));
	}

	private final static Comparator<OLOrder> cmp = (a, b) -> {
	    return(Utils.idcmp.compare(a.id, b.id));
	};

	public Comparator<OLOrder> comparator() {return(cmp);}
    }
    private static final VertexArray.Layout olvfmt = new VertexArray.Layout(new VertexArray.Layout.Input(Homo3D.vertex, new VectorFormat(3, NumberFormat.FLOAT32), 0, 0, 16),
									    new VertexArray.Layout.Input(Homo3D.normal, new VectorFormat(3, NumberFormat.SNORM8), 0, 12, 16));
    private static class OLArray {
	VertexArray dat;
	int[] vl;

	OLArray(VertexArray dat, int[] vl) {
	    this.dat = dat;
	    this.vl = vl;
	}
    }
    private OLArray makeolvbuf() {
	MapSurface ms = data(gnd);
	int[] vl = new int[ms.vl.length];
	haven.Surface.Normals sn = ms.data(haven.Surface.nrm);
	for(int i = 0; i < vl.length; i++)
	    vl[i] = -1;
	VertexBuilder vbuf = new VertexBuilder(olvfmt);
	class Buf implements Tiler.MCons {
	    public void faces(MapMesh m, Tiler.MPart d) {
		for(Vertex v : d.v) {
		    if(vl[v.vi] < 0) {
			vbuf.set(0, v);
			vbuf.set(1, sn.get(v));
			vl[v.vi] = vbuf.emit();
		    }
		}
	    }
	}
	Buf buf = new Buf();
	for(int y = 0; y < sz.y; y++) {
	    for(int x = 0; x < sz.x; x++) {
		Coord gc = ul.add(x, y);
		map.tiler(map.gettile(gc)).lay(this, new Coord(x, y), gc, buf, false);
	    }
	}
	return(new OLArray(vbuf.finv(), vl));
    }
    private OLArray olvert = null;

    private static class ShallowWrap implements RenderTree.Node, Rendered, Disposable {
	final Rendered r;
	final Pipe.Op st;

	ShallowWrap(Rendered r, Pipe.Op st) {
	    this.r = r;
	    this.st = st;
	}

	public void added(RenderTree.Slot slot) {
	    slot.ostate(st);
	}

	public void draw(Pipe context, Render out) {
	    r.draw(context, out);
	}

	public void dispose() {
	    if(r instanceof Disposable)
		((Disposable)r).dispose();
	}
    }

    public RenderTree.Node makeol(MCache.OverlayInfo id) {
	if(olvert == null)
	    olvert = makeolvbuf();
	class Buf implements Tiler.MCons {
	    short[] fl = new short[16];
	    int fn = 0;

	    public void faces(MapMesh m, Tiler.MPart d) {
		while(fn + d.f.length > fl.length)
		    fl = Utils.extend(fl, fl.length * 2);
		for(int fi : d.f)
		    fl[fn++] = (short)olvert.vl[d.v[fi].vi];
	    }
	}
	Coord t = new Coord();
	Area a = Area.sized(ul, sz);
	boolean[] ol = new boolean[a.area()];
	map.getol(id, a, ol);
	Buf buf = new Buf();
	for(t.y = 0; t.y < sz.y; t.y++) {
	    for(t.x = 0; t.x < sz.x; t.x++) {
		if(ol[t.x + (t.y * sz.x)]) {
		    Coord gc = t.add(ul);
		    map.tiler(map.gettile(gc)).lay(this, t, gc, buf, false);
		}
	    }
	}
	if(buf.fn == 0)
	    return(null);
	haven.render.Model mod = new haven.render.Model(haven.render.Model.Mode.TRIANGLES, olvert.dat,
							new haven.render.Model.Indices(buf.fn, NumberFormat.UINT16, DataBuffer.Usage.STATIC,
										       DataBuffer.Filler.of(Arrays.copyOf(buf.fl, buf.fn))));
	return(new ShallowWrap(mod, new OLOrder(id)));
    }

    public RenderTree.Node makeolol(MCache.OverlayInfo id) {
	if(olvert == null)
	    olvert = makeolvbuf();
	class Buf implements Tiler.MCons {
	    int mask;
	    short[] fl = new short[16];
	    int fn = 0;

	    public void faces(MapMesh m, Tiler.MPart d) {
		byte[] ef = new byte[d.v.length];
		for(int i = 0; i < d.v.length; i++) {
		    if(d.tcy[i] == 0.0f) ef[i] |= 1;
		    if(d.tcx[i] == 1.0f) ef[i] |= 2;
		    if(d.tcy[i] == 1.0f) ef[i] |= 4;
		    if(d.tcx[i] == 0.0f) ef[i] |= 8;
		}
		while(fn + (d.f.length * 2) > fl.length)
		    fl = Utils.extend(fl, fl.length * 2);
		for(int i = 0; i < d.f.length; i += 3) {
		    for(int a = 0; a < 3; a++) {
			int b = (a + 1) % 3;
			if((ef[d.f[i + a]] & ef[d.f[i + b]] & mask) != 0) {
			    fl[fn++] = (short)olvert.vl[d.v[d.f[i + a]].vi];
			    fl[fn++] = (short)olvert.vl[d.v[d.f[i + b]].vi];
			}
		    }
		}
	    }
	}
	Area a = Area.sized(ul, sz);
	Area ma = a.margin(1);
	boolean[] ol = new boolean[ma.area()];
	map.getol(id, ma, ol);
	Buf buf = new Buf();
	for(Coord t : a) {
	    if(ol[ma.ri(t)]) {
		buf.mask = 0;
		for(int d = 0; d < 4; d++) {
		    if(!ol[ma.ri(t.add(Coord.uecw[d]))])
			buf.mask |= 1 << d;
		}
		if(buf.mask != 0)
		    map.tiler(map.gettile(t)).lay(this, t.sub(a.ul), t, buf, false);
	    }
	}
	if(buf.fn == 0)
	    return(null);
	haven.render.Model mod = new haven.render.Model(haven.render.Model.Mode.LINES, olvert.dat,
							new haven.render.Model.Indices(buf.fn, NumberFormat.UINT16, DataBuffer.Usage.STATIC,
										       DataBuffer.Filler.of(Arrays.copyOf(buf.fl, buf.fn))));
	return(new ShallowWrap(mod, Pipe.Op.compose(new OLOrder(id), new States.LineWidth(2))));
    }

    private void clean() {
	int on = data.size();
	for(Iterator<Map.Entry<DataID, Object>> i = data.entrySet().iterator(); i.hasNext();) {
	    Object d = i.next().getValue();
	    if(!(d instanceof ConsHooks) || !((ConsHooks)d).clean())
		i.remove();
	}
    }

    public void draw(GOut g) {
    }

    private void consflat() {
	class Buf implements Tiler.MCons {
	    final float tpx = 1.0f / sz.x, tpy = 1.0f / sz.y;
	    int vn = 0, in = 0, vl = sz.x * sz.y * 4;
	    float[] pos = new float[vl * 3];
	    float[] col = new float[vl * 2];
	    short[] ind = new short[sz.x * sz.y * 6];

	    public void faces(MapMesh m, Tiler.MPart d) {
		if(vn + d.v.length > vl) {
		    vl *= 2;
		    pos = Utils.extend(pos, vl * 3);
		    col = Utils.extend(col, vl * 2);
		}
		float cx = (float)d.lc.x / (float)m.sz.x, cy = (float)d.lc.y / (float)m.sz.y;
		for(int i = 0; i < d.v.length; i++) {
		    int pb = (vn + i) * 3, cb = (vn + i) * 2;
		    pos[pb + 0] = d.v[i].x; pos[pb + 1] = d.v[i].y; pos[pb + 2] = d.v[i].z;
		    col[cb + 0] = cx + (d.tcx[i] * tpx); col[cb + 1] = cy + (d.tcy[i] * tpy);
		}
		if(in + d.f.length > ind.length)
		    ind = Utils.extend(ind, ind.length * 2);
		for(int fi : d.f)
		    ind[in++] = (short)(vn + fi);
		vn += d.v.length;
	    }
	}
	Buf buf = new Buf();
	Coord c = new Coord();
	for(c.y = 0; c.y < sz.y; c.y++) {
	    for(c.x = 0; c.x < sz.x; c.x++) {
		Coord gc = c.add(ul);
		map.tiler(map.gettile(gc)).lay(this, c, gc, buf, true);
	    }
	}
	float[] pos = buf.pos, col = buf.col;
	short[] ind = buf.ind;
	if(pos.length != buf.vn * 3) pos = Utils.extend(pos, buf.vn * 3);
	if(col.length != buf.vn * 2) col = Utils.extend(col, buf.vn * 2);
	if(ind.length != buf.in) ind = Utils.extend(ind, buf.in);
	VertexBuf.VertexData posa = new VertexBuf.VertexData(FloatBuffer.wrap(pos));
	ClickLocation.LocData loca = new ClickLocation.LocData(FloatBuffer.wrap(col));
	ShortBuffer indb = ShortBuffer.wrap(ind);
	flat = new FastMesh(new VertexBuf(posa, loca), indb);
    }

    private static final VertexArray.Layout gridfmt = new VertexArray.Layout(new VertexArray.Layout.Input(Homo3D.vertex, new VectorFormat(3, NumberFormat.FLOAT32), 0, 0, 12));
    private RenderTree.Node consgrid() {
	class Buf implements Tiler.MCons {
	    int vn = 0, in = 0, vl = sz.x * sz.y * 4;
	    float[] pos = new float[vl * 3];
	    short[] ind = new short[sz.x * sz.y * 8];

	    int getvert(Tiler.MPart d, int i, int[] imap) {
		if(imap[i] >= 0)
		    return(imap[i]);
		if(vn >= vl) {
		    vl *= 2;
		    pos = Utils.extend(pos, vl * 3);
		}
		int pb = vn * 3;
		pos[pb + 0] = d.v[i].x; pos[pb + 1] = d.v[i].y; pos[pb + 2] = d.v[i].z;
		return(imap[i] = vn++);
	    }

	    public void faces(MapMesh m, Tiler.MPart d) {
		byte[] ef = new byte[d.v.length];
		int[] imap = new int[d.v.length];
		for(int i = 0; i < imap.length; imap[i++] = -1);
		for(int i = 0, ivn = 0; i < d.v.length; i++) {
		    if(d.tcy[i] == 0.0f) ef[i] |= 1;
		    if(d.tcx[i] == 1.0f) ef[i] |= 2;
		    if(d.tcy[i] == 1.0f) ef[i] |= 4;
		    if(d.tcx[i] == 0.0f) ef[i] |= 8;
		}
		for(int i = 0; i < d.f.length; i += 3) {
		    for(int a = 0; a < 3; a++) {
			int b = (a + 1) % 3;
			if((ef[d.f[i + a]] & ef[d.f[i + b]]) != 0) {
			    if(in + 2 > ind.length)
				ind = Utils.extend(ind, ind.length * 2);
			    ind[in++] = (short)getvert(d, d.f[i + a], imap);
			    ind[in++] = (short)getvert(d, d.f[i + b], imap);
			}
		    }
		}
	    }
	}
	Buf buf = new Buf();
	Coord c = new Coord();
	for(c.y = 0; c.y < sz.y; c.y++) {
	    for(c.x = 0; c.x < sz.x; c.x++) {
		Coord gc = c.add(ul);
		map.tiler(map.gettile(gc)).lay(this, c, gc, buf, false);
	    }
	}
	float[] pos = buf.pos;
	short[] ind = buf.ind;
	if(pos.length != buf.vn * 3) pos = Utils.extend(pos, buf.vn * 3);
	if(ind.length != buf.in) ind = Utils.extend(ind, buf.in);
	VertexArray vdat = new VertexArray(gridfmt, new VertexArray.Buffer(pos.length * 4, DataBuffer.Usage.STATIC, DataBuffer.Filler.of(pos)));
	return(new haven.render.Model(haven.render.Model.Mode.LINES, vdat, new haven.render.Model.Indices(ind.length, NumberFormat.UINT16, DataBuffer.Usage.STATIC, DataBuffer.Filler.of(ind))));
    }

    private RenderTree.Node grid = null;
    public RenderTree.Node grid() {
	if(grid == null)
	    synchronized(this) {
		if(grid == null)
		    grid = consgrid();
	    }
	return(grid);
    }

    public void dispose() {
	for(Disposable p : dparts)
	    p.dispose();
    }

    public void added(RenderTree.Slot slot) {
	for(RenderTree.Node e : extras)
	    slot.add(e);
    }

    public String toString() {
	return(String.format("#<map-mesh %s+%s>", ul, sz));
    }
}
