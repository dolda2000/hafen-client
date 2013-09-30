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
import javax.media.opengl.*;
import java.awt.Color;

public class MapMesh implements Rendered, Disposable {
    public final Coord ul, sz;
    public final MCache map;
    private Map<Tex, GLState[]> texmap = new HashMap<Tex, GLState[]>();
    private Map<DataID, Object> data = new HashMap<DataID, Object>();
    private List<Rendered> extras = new ArrayList<Rendered>();
    private List<Layer> layers;
    private FastMesh[] flats;
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

    public static class Hooks {
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

    private static final Material.Colors gcol = new Material.Colors(new Color(128, 128, 128), new Color(255, 255, 255), new Color(0, 0, 0), new Color(0, 0, 0));
    public GLState stfor(Tex tex, boolean clip) {
	TexGL gt;
	if(tex instanceof TexGL)
	    gt = (TexGL)tex;
	else if((tex instanceof TexSI) && (((TexSI)tex).parent instanceof TexGL))
	    gt = (TexGL)((TexSI)tex).parent;
	else
	    throw(new RuntimeException("Cannot use texture for map rendering: " + tex));
	GLState[] ret = texmap.get(gt);
	if(ret == null) {
	    /* texmap.put(gt, ret = new Material(gt)); */
	    texmap.put(gt, ret = new GLState[] {
		    new Material(Light.deflight, gcol, gt.draw(), gt.clip()),
		    new Material(Light.deflight, gcol, gt.draw()),
		});
	}
	return(ret[clip?0:1]);
    }

    public static class SPoint {
	public Coord3f pos, nrm = Coord3f.zu;
	public SPoint(Coord3f pos) {
	    this.pos = pos;
	}
    }
    
    public class Surface extends Hooks {
	public final SPoint[] surf;
	
	public Surface() {
	    surf = new SPoint[(sz.x + 3) * (sz.y + 3)];
	    Coord c = new Coord();
	    int i = 0;
	    for(c.y = -1; c.y <= sz.y + 1; c.y++) {
		for(c.x = -1; c.x <= sz.x + 1; c.x++) {
		    surf[i++] = new SPoint(new Coord3f(c.x * tilesz.x, c.y * -tilesz.y, map.getz(ul.add(c))));
		}
	    }
	}
	
	public int idx(Coord lc) {
	    return((lc.x + 1) + ((lc.y + 1) * (sz.x + 3)));
	}
	
	public SPoint spoint(Coord lc) {
	    return(surf[idx(lc)]);
	}

	public void calcnrm() {
	    Coord c = new Coord();
	    int i = idx(Coord.z), r = (sz.x + 3);
	    for(c.y = 0; c.y <= sz.y; c.y++) {
		for(c.x = 0; c.x <= sz.x; c.x++) {
		    SPoint p = surf[i];
		    Coord3f s = surf[i + r].pos.sub(p.pos);
		    Coord3f w = surf[i - 1].pos.sub(p.pos);
		    Coord3f n = surf[i - r].pos.sub(p.pos);
		    Coord3f e = surf[i + 1].pos.sub(p.pos);
		    Coord3f nrm = (n.cmul(w)).add(e.cmul(n)).add(s.cmul(e)).add(w.cmul(s)).norm();
		    p.nrm = nrm;
		    i++;
		}
		i += 2;
	    }
	}

	public SPoint[] fortile(Coord sc) {
	    return(new SPoint[] {
		    spoint(sc),
		    spoint(sc.add(0, 1)),
		    spoint(sc.add(1, 1)),
		    spoint(sc.add(1, 0)),
		});
	}
    }
    
    public static void splitquad(MeshBuf buf, MeshBuf.Vertex v1, MeshBuf.Vertex v2, MeshBuf.Vertex v3, MeshBuf.Vertex v4) {
	if(Math.abs(v1.pos.z - v3.pos.z) > Math.abs(v2.pos.z - v4.pos.z)) {
	    buf.new Face(v1, v2, v3);
	    buf.new Face(v1, v3, v4);
	} else {
	    buf.new Face(v1, v2, v4);
	    buf.new Face(v2, v3, v4);
	}
    }

    public abstract class Shape {
	public Shape(int z, GLState st) {
	    reg(z, st);
	}
	
	public abstract void build(MeshBuf buf);
	
	private void reg(int z, GLState st) {
	    for(Layer l : layers) {
		if((l.st == st) && (l.z == z)) {
		    l.pl.add(this);
		    return;
		}
	    }
	    Layer l = new Layer();
	    l.st = st;
	    l.z = z;
	    l.pl.add(this);
	    layers.add(l);
	}
    }

    /* Inner classes cannot have static declarations D:< */
    private static SPoint[] fortile(Surface surf, Coord sc) {
	return(surf.fortile(sc));
    }

    public class Plane extends Shape {
	public SPoint[] vrt;
	public int[] texx, texy;
	public Tex tex = null;
	
	public Plane(SPoint[] vrt, int z, GLState st) {
	    super(z, st);
	    this.vrt = vrt;
	}
	
	public Plane(Surface surf, Coord sc, int z, GLState st) {
	    this(fortile(surf, sc), z, st);
	}

	public Plane(SPoint[] vrt, int z, GLState st, Tex tex) {
	    this(vrt, z, st);
	    this.tex = tex;
	    texrot(null, null, 0, false);
	}

	public Plane(SPoint[] vrt, int z, Tex tex, boolean clip) {
	    this(vrt, z, stfor(tex, clip), tex);
	}
	
	public Plane(Surface surf, Coord sc, int z, Tex tex, boolean clip) {
	    this(fortile(surf, sc), z, tex, clip);
	}

	public Plane(Surface surf, Coord sc, int z, Tex tex) {
	    this(surf, sc, z, tex, true);
	}
	
	public Plane(Surface surf, Coord sc, int z, Resource.Tile tile) {
	    this(surf, sc, z, tile.tex(), tile.t != 'g');
	}
	
	public void texrot(Coord ul, Coord br, int rot, boolean flipx) {
	    if(ul == null) ul = Coord.z;
	    if(br == null) br = tex.sz();
	    int[] x, y;
	    if(!flipx) {
		x = new int[] {ul.x, ul.x, br.x, br.x};
		y = new int[] {ul.y, br.y, br.y, ul.y};
	    } else {
		x = new int[] {br.x, br.x, ul.x, ul.x};
		y = new int[] {ul.y, br.y, br.y, ul.y};
	    }
	    if(texx == null) {
		texx = new int[4];
		texy = new int[4];
	    }
	    for(int i = 0; i < 4; i++) {
		texx[i] = x[(i + rot) % 4];
		texy[i] = y[(i + rot) % 4];
	    }
	}
	
	public void build(MeshBuf buf) {
	    MeshBuf.Tex btex = buf.layer(MeshBuf.tex);
	    MeshBuf.Vertex v1 = buf.new Vertex(vrt[0].pos, vrt[0].nrm);
	    MeshBuf.Vertex v2 = buf.new Vertex(vrt[1].pos, vrt[1].nrm);
	    MeshBuf.Vertex v3 = buf.new Vertex(vrt[2].pos, vrt[2].nrm);
	    MeshBuf.Vertex v4 = buf.new Vertex(vrt[3].pos, vrt[3].nrm);
	    Tex tex = this.tex;
	    if(tex != null) {
		int r = tex.sz().x, b = tex.sz().y;
		btex.set(v1, new Coord3f(tex.tcx(texx[0]), tex.tcy(texy[0]), 0.0f));
		btex.set(v2, new Coord3f(tex.tcx(texx[1]), tex.tcy(texy[1]), 0.0f));
		btex.set(v3, new Coord3f(tex.tcx(texx[2]), tex.tcy(texy[2]), 0.0f));
		btex.set(v4, new Coord3f(tex.tcx(texx[3]), tex.tcy(texy[3]), 0.0f));
	    }
	    splitquad(buf, v1, v2, v3, v4);
	}
    }
    
    private static final Order mmorder = new Order<Layer>() {
	public int mainz() {
	    return(1000);
	}
	
	private final RComparator<Layer> cmp = new RComparator<Layer>() {
	    public int compare(Layer a, Layer b, GLState.Buffer sa, GLState.Buffer sb) {
		return(a.z - b.z);
	    }
	};
	
	public RComparator<Layer> cmp() {
	    return(cmp);
	}
    };

    private static class Layer implements Rendered {
	GLState st;
	int z;
	FastMesh mesh;
	Collection<Shape> pl = new LinkedList<Shape>();
	
	public void draw(GOut g) {
	    mesh.draw(g);
	}
	
	public boolean setup(RenderList rl) {
	    rl.prepo(st);
	    rl.prepo(mmorder);
	    return(true);
	}
    }
    
    private MapMesh(MCache map, Coord ul, Coord sz) {
	this.map = map;
	this.ul = ul;
	this.sz = sz;
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

    public class Ground extends Surface {
	public boolean clean() {return(true);}
    }
    private static DataID<Ground> gndid = makeid(Ground.class);
    public Surface gnd() {
	return(data(gndid));
    }
    
    public static class Models extends Hooks {
	private final MapMesh m;
	private final Map<GLState, MeshBuf> models = new HashMap<GLState, MeshBuf>();

	public Models(MapMesh m) {
	    this.m = m;
	}

	private static DataID<Models> msid = makeid(Models.class);
	public static MeshBuf get(MapMesh m, GLState st) {
	    Models ms = m.data(msid);
	    MeshBuf ret = ms.models.get(st);
	    if(ret == null)
		ms.models.put(st, ret = new MeshBuf());
	    return(ret);
	}

	public void postcalcnrm(Random rnd) {
	    for(Map.Entry<GLState, MeshBuf> mod : models.entrySet()) {
		FastMesh mesh = mod.getValue().mkmesh();
		m.extras.add(mod.getKey().apply(mesh));
		m.dparts.add(mesh);
	    }
	}
    }

    public static MapMesh build(MCache mc, Random rnd, Coord ul, Coord sz) {
	MapMesh m = new MapMesh(mc, ul, sz);
	Coord c = new Coord();
	m.layers = new ArrayList<Layer>();
	
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
	    if(obj instanceof Hooks)
		((Hooks)obj).calcnrm();
	}
	for(Object obj : m.data.values()) {
	    if(obj instanceof Hooks)
		((Hooks)obj).postcalcnrm(rnd);
	}
	for(Layer l : m.layers) {
	    MeshBuf buf = new MeshBuf();
	    if(l.pl.isEmpty())
		throw(new RuntimeException("Map layer without planes?!"));
	    for(Shape p : l.pl)
		p.build(buf);
	    l.mesh = buf.mkmesh();
	    m.dparts.add(l.mesh);
	}
	Collections.sort(m.layers, new Comparator<Layer>() {
		public int compare(Layer a, Layer b) {
		    return(a.z - b.z);
		}
	    });
	
	m.consflat();
	
	m.clean();
	return(m);
    }

    private static States.DepthOffset gmoff = new States.DepthOffset(-1, -1);
    public static class GroundMod implements Rendered, Disposable {
	private static final Order gmorder = new Order.Default(1001);
	public final Material mat;
	public final Coord cc;
	public final FastMesh mesh;
	
	public GroundMod(MCache map, DataID<? extends Surface> surf, Tex tex, Coord cc, Coord ul, Coord br) {
	    this.mat = new Material(tex);
	    this.cc = cc;
	    if(tex instanceof TexGL) {
		TexGL gt = (TexGL)tex;
		if(gt.wrapmode != GL2.GL_CLAMP_TO_BORDER) {
		    gt.wrapmode = GL2.GL_CLAMP_TO_BORDER;
		    gt.dispose();
		}
	    }
	    if(surf == null)
		surf = gndid;
	    MeshBuf buf = new MeshBuf();
	    MeshBuf.Tex ta = buf.layer(MeshBuf.tex);
	    Coord ult = ul.div(tilesz);
	    Coord brt = br.sub(1, 1).div(tilesz).add(1, 1);
	    Coord t = new Coord();
	    float cz = map.getcz(cc);
	    MeshBuf.Vertex[][] vm = new MeshBuf.Vertex[brt.x - ult.x + 1][brt.y - ult.y + 1];
	    for(t.y = ult.y; t.y <= brt.y; t.y++) {
		for(t.x = ult.x; t.x <= brt.x; t.x++) {
		    MapMesh cut = map.getcut(t.div(MCache.cutsz));
		    SPoint p = cut.data(surf).spoint(t.mod(MCache.cutsz));
		    Coord3f texc = new Coord3f((float)((t.x * tilesz.x) - ul.x) / (float)(br.x - ul.x),
					       (float)((t.y * tilesz.y) - ul.y) / (float)(br.y - ul.y),
					       0);
		    Coord3f pos = p.pos.add((cut.ul.x * tilesz.x) - cc.x, -((cut.ul.y * tilesz.y) - cc.y), -cz);
		    MeshBuf.Vertex v = vm[t.x - ult.x][t.y - ult.y] = buf.new Vertex(pos, p.nrm);
		    ta.set(v, texc);
		}
	    }
	    for(t.y = 0; t.y < brt.y - ult.y; t.y++) {
		for(t.x = 0; t.x < brt.x - ult.x; t.x++) {
		    splitquad(buf, vm[t.x][t.y], vm[t.x][t.y + 1], vm[t.x + 1][t.y + 1], vm[t.x + 1][t.y]);
		}
	    }
	    mesh = buf.mkmesh();
	}

	@Deprecated
	public GroundMod(MCache map, Class<? extends Surface> surf, Tex tex, Coord cc, Coord ul, Coord br) {
	    this(map, (DataID<Surface>)null, tex, cc, ul, br);
	    if(surf != null)
		throw(new RuntimeException());
	}

	public void dispose() {
	    mesh.dispose();
	}

	public void draw(GOut g) {
	}
		
	public boolean setup(RenderList rl) {
	    rl.prepc(gmorder);
	    rl.prepc(mat);
	    rl.prepc(gmoff);
	    rl.add(mesh, null);
	    return(false);
	}
    }
    
    public static final Order olorder = new Order.Default(1002);
    public Rendered[] makeols() {
	Surface surf = new Surface();
	surf.calcnrm();
	MeshBuf buf = new MeshBuf();
	MeshBuf.Vertex[][] v = new MeshBuf.Vertex[sz.x + 1][sz.y + 1];
	Coord t = new Coord();
	for(t.y = 0; t.y <= sz.y; t.y++) {
	    for(t.x = 0; t.x <= sz.x; t.x++) {
		SPoint p = surf.spoint(t);
		v[t.x][t.y] = buf.new Vertex(p.pos, p.nrm);
	    }
	}
	int[][] ol = new int[sz.x][sz.y];
	for(t.y = 0; t.y < sz.y; t.y++) {
	    for(t.x = 0; t.x < sz.x; t.x++) {
		ol[t.x][t.y] = map.getol(ul.add(t));
	    }
	}
	Rendered[] ret = new Rendered[32];
	for(int i = 0; i < 32; i++) {
	    boolean h = false;
	    buf.clearfaces();
	    for(t.y = 0; t.y < sz.y; t.y++) {
		for(t.x = 0; t.x < sz.x; t.x++) {
		    if((ol[t.x][t.y] & (1 << i)) != 0) {
			h = true;
			splitquad(buf, v[t.x][t.y], v[t.x][t.y + 1], v[t.x + 1][t.y + 1], v[t.x + 1][t.y]);
		    }
		}
	    }
	    if(h) {
		final FastMesh mesh = buf.mkmesh();
		class OL implements Rendered, Disposable {
		    public void draw(GOut g) {
			mesh.draw(g);
		    }

		    public void dispose() {
			mesh.dispose();
		    }

		    public boolean setup(RenderList rl) {
			rl.prepo(olorder);
			return(true);
		    }
		}
		ret[i] = new OL();
	    }
	}
	return(ret);
    }

    private void clean() {
	texmap = null;
	for(Layer l : layers)
	    l.pl = null;
	int on = data.size();
	for(Iterator<Map.Entry<DataID, Object>> i = data.entrySet().iterator(); i.hasNext();) {
	    Object d = i.next().getValue();
	    if(!(d instanceof Hooks) || !((Hooks)d).clean())
		i.remove();
	}
    }
    
    public void draw(GOut g) {
    }
    
    private void consflat() {
	Surface g = gnd();
	java.nio.FloatBuffer pos = Utils.mkfbuf(sz.x * sz.y * 12);
	java.nio.FloatBuffer col1 = Utils.mkfbuf(sz.x * sz.y * 16);
	java.nio.FloatBuffer col2 = Utils.mkfbuf(sz.x * sz.y * 16);
	java.nio.ShortBuffer ind = Utils.mksbuf(sz.x * sz.y * 6);
	short i = 0;
	Coord c = new Coord();
	for(c.y = 0; c.y < sz.y; c.y++) {
	    for(c.x = 0; c.x < sz.x; c.x++) {
		SPoint p;
		p = g.spoint(c);
		pos.put(p.pos.x).put(p.pos.y).put(p.pos.z);
		col1.put((c.x + 1) / 256.0f).put((c.y + 1) / 256.0f).put(0).put(1);
		col2.put(0).put(0).put(0).put(1);

		p = g.spoint(c.add(0, 1));
		pos.put(p.pos.x).put(p.pos.y).put(p.pos.z);
		col1.put((c.x + 1) / 256.0f).put((c.y + 1) / 256.0f).put(0).put(1);
		col2.put(0).put(1).put(0).put(1);

		p = g.spoint(c.add(1, 1));
		pos.put(p.pos.x).put(p.pos.y).put(p.pos.z);
		col1.put((c.x + 1) / 256.0f).put((c.y + 1) / 256.0f).put(0).put(1);
		col2.put(1).put(1).put(0).put(1);

		p = g.spoint(c.add(1, 0));
		pos.put(p.pos.x).put(p.pos.y).put(p.pos.z);
		col1.put((c.x + 1) / 256.0f).put((c.y + 1) / 256.0f).put(0).put(1);
		col2.put(1).put(0).put(0).put(1);

		ind.put(i).put((short)(i + 1)).put((short)(i + 2));
		ind.put(i).put((short)(i + 2)).put((short)(i + 3));
		i += 4;
	    }
	}
	VertexBuf.VertexArray posa = new VertexBuf.VertexArray(pos);
	VertexBuf.ColorArray cola1 = new VertexBuf.ColorArray(col1);
	VertexBuf.ColorArray cola2 = new VertexBuf.ColorArray(col2);
	flats = new FastMesh[] {
	    new FastMesh(new VertexBuf(posa), ind),
	    new FastMesh(new VertexBuf(posa, cola1), ind),
	    new FastMesh(new VertexBuf(posa, cola2), ind),
	};
    }

    public void drawflat(GOut g, int mode) {
	g.apply();
	flats[mode].draw(g);
    }
    
    public void dispose() {
	for(Disposable p : dparts)
	    p.dispose();
    }
    
    public boolean setup(RenderList rl) {
	for(Layer l : layers)
	    rl.add(l, null);
	for(Rendered e : extras)
	    rl.add(e, null);
	return(true);
    }
}
