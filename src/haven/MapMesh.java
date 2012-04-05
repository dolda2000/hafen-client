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

public class MapMesh implements Rendered {
    public final Coord ul, sz;
    public final MCache map;
    private Map<Class<? extends Surface>, Surface> surfmap = new HashMap<Class<? extends Surface>, Surface>();
    private Map<Tex, GLState[]> texmap = new HashMap<Tex, GLState[]>();
    private Map<GLState, MeshBuf> modbuf = new HashMap<GLState, MeshBuf>();
    public Map<Object, Object> data = new HashMap<Object, Object>();
    private List<Rendered> extras = new ArrayList<Rendered>();
    private List<Layer> layers;
    private float[] zmap;

    public static class SPoint {
	public Coord3f pos, nrm = Coord3f.zu;
	public SPoint(Coord3f pos) {
	    this.pos = pos;
	}
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

    public class Surface {
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
    }
    
    private static void splitquad(MeshBuf buf, MeshBuf.Vertex v1, MeshBuf.Vertex v2, MeshBuf.Vertex v3, MeshBuf.Vertex v4) {
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
	return(new SPoint[] {
		surf.spoint(sc),
		surf.spoint(sc.add(0, 1)),
		surf.spoint(sc.add(1, 1)),
		surf.spoint(sc.add(1, 0)),
	    });
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

	public Plane(SPoint[] vrt, int z, Tex tex, boolean clip) {
	    this(vrt, z, stfor(tex, clip));
	    this.tex = tex;
	    texrot(null, null, 0, false);
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
	    MeshBuf.Vertex v1 = buf.new Vertex(vrt[0].pos, vrt[0].nrm);
	    MeshBuf.Vertex v2 = buf.new Vertex(vrt[1].pos, vrt[1].nrm);
	    MeshBuf.Vertex v3 = buf.new Vertex(vrt[2].pos, vrt[2].nrm);
	    MeshBuf.Vertex v4 = buf.new Vertex(vrt[3].pos, vrt[3].nrm);
	    Tex tex = this.tex;
	    if(tex != null) {
		int r = tex.sz().x, b = tex.sz().y;
		v1.tex = new Coord3f(tex.tcx(texx[0]), tex.tcy(texy[0]), 0.0f);
		v2.tex = new Coord3f(tex.tcx(texx[1]), tex.tcy(texy[1]), 0.0f);
		v3.tex = new Coord3f(tex.tcx(texx[2]), tex.tcy(texy[2]), 0.0f);
		v4.tex = new Coord3f(tex.tcx(texx[3]), tex.tcy(texy[3]), 0.0f);
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
	    Tiler t = m.map.tiler(i);
	    if(t == null)
		continue;
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
	    if((bm != 0) || (cm != 0))
		t.trans(m, rnd, ground, lc, gc, 255 - i, bm, cm);
	}
    }

    public <T extends Surface> T surf(Class<T> cl) {
	Surface ret = surfmap.get(cl);
	if(ret == null) {
	    try {
		java.lang.reflect.Constructor<T> c = cl.getConstructor(MapMesh.class);
		ret = c.newInstance(this);
		surfmap.put(cl, ret);
	    } catch(NoSuchMethodException e) {
		throw(new RuntimeException(e));
	    } catch(InstantiationException e) {
		throw(new RuntimeException(e));
	    } catch(IllegalAccessException e) {
		throw(new RuntimeException(e));
	    } catch(java.lang.reflect.InvocationTargetException e) {
		if(e.getCause() instanceof RuntimeException)
		    throw((RuntimeException)e.getCause());
		throw(new RuntimeException(e));
	    }
	}
	return(cl.cast(ret));
    }
    
    public Surface gnd() {
	return(surf(Surface.class));
    }
    
    public static class Hooks {
	public void postcalcnrm(Random rnd) {}
    }

    public <T extends MeshBuf> T model(GLState st, Class<T> init) {
	MeshBuf ret = modbuf.get(st);
	if(ret == null) {
	    try {
		java.lang.reflect.Constructor<T> c = init.getConstructor();
		ret = c.newInstance();
		modbuf.put(st, ret);
	    } catch(NoSuchMethodException e) {
		throw(new RuntimeException(e));
	    } catch(InstantiationException e) {
		throw(new RuntimeException(e));
	    } catch(IllegalAccessException e) {
		throw(new RuntimeException(e));
	    } catch(java.lang.reflect.InvocationTargetException e) {
		if(e.getCause() instanceof RuntimeException)
		    throw((RuntimeException)e.getCause());
		throw(new RuntimeException(e));
	    }
	}
	return(init.cast(ret));
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
	for(Surface s : m.surfmap.values())
	    s.calcnrm();
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
	}
	Collections.sort(m.layers, new Comparator<Layer>() {
		public int compare(Layer a, Layer b) {
		    return(a.z - b.z);
		}
	    });
	for(Map.Entry<GLState, MeshBuf> mod : m.modbuf.entrySet()) {
	    if(!mod.getValue().emptyp())
		m.extras.add(mod.getKey().apply(mod.getValue().mkmesh()));
	}
	
	Surface g = m.gnd();
	m.zmap = new float[(sz.x + 1) * (sz.y + 1)];
	int i = 0;
	for(c.y = 0; c.y <= sz.y; c.y++) {
	    for(c.x = 0; c.x <= sz.x; c.x++) {
		m.zmap[i++] = g.spoint(c).pos.z;
	    }
	}
	
	m.clean();
	return(m);
    }

    private static States.DepthOffset gmoff = new States.DepthOffset(-1, -1);
    public static class GroundMod implements Rendered {
	private static final Order gmorder = new Order.Default(1001);
	public final Material mat;
	public final Coord cc;
	public final FastMesh mesh;
	
	public GroundMod(MCache map, Class<? extends Surface> surf, Tex tex, Coord cc, Coord ul, Coord br) {
	    this.mat = new Material(tex);
	    this.cc = cc;
	    if(tex instanceof TexGL) {
		TexGL gt = (TexGL)tex;
		if(gt.wrapmode != GL.GL_CLAMP_TO_BORDER) {
		    gt.wrapmode = GL.GL_CLAMP_TO_BORDER;
		    gt.dispose();
		}
	    }
	    if(surf == null)
		surf = Surface.class;
	    MeshBuf buf = new MeshBuf();
	    Coord ult = ul.div(tilesz);
	    Coord brt = br.sub(1, 1).div(tilesz).add(1, 1);
	    Coord t = new Coord();
	    float cz = map.getcz(cc);
	    MeshBuf.Vertex[][] vm = new MeshBuf.Vertex[brt.x - ult.x + 1][brt.y - ult.y + 1];
	    for(t.y = ult.y; t.y <= brt.y; t.y++) {
		for(t.x = ult.x; t.x <= brt.x; t.x++) {
		    MapMesh cut = map.getcut(t.div(MCache.cutsz));
		    SPoint p = cut.surf(surf).spoint(t.mod(MCache.cutsz));
		    Coord3f texc = new Coord3f((float)((t.x * tilesz.x) - ul.x) / (float)(br.x - ul.x),
					       (float)((t.y * tilesz.y) - ul.y) / (float)(br.y - ul.y),
					       0);
		    Coord3f pos = p.pos.add((cut.ul.x * tilesz.x) - cc.x, -((cut.ul.y * tilesz.y) - cc.y), -cz);
		    vm[t.x - ult.x][t.y - ult.y] = buf.new Vertex(pos, p.nrm, texc);
		}
	    }
	    for(t.y = 0; t.y < brt.y - ult.y; t.y++) {
		for(t.x = 0; t.x < brt.x - ult.x; t.x++) {
		    splitquad(buf, vm[t.x][t.y], vm[t.x][t.y + 1], vm[t.x + 1][t.y + 1], vm[t.x + 1][t.y]);
		}
	    }
	    mesh = buf.mkmesh();
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
	final MeshBuf buf = new MeshBuf();
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
	    if(h)
		ret[i] = new Rendered() {
			FastMesh mesh = buf.mkmesh();
			
			public void draw(GOut g) {
			    mesh.draw(g);
			}
			
			public boolean setup(RenderList rl) {
			    rl.prepo(olorder);
			    return(true);
			}
		    };
	}
	return(ret);
    }

    private void clean() {
	texmap = null;
	for(Layer l : layers)
	    l.pl = null;
	modbuf = null;
	data = null;
    }
    
    public void draw(GOut g) {
    }
    
    private float fz(int x, int y) {
	return(zmap[x + (y * (sz.x + 1))]);
    }

    public void drawflat(GOut g, int mode) {
	g.apply();
	GL gl = g.gl;
	gl.glBegin(GL.GL_TRIANGLES);
	Coord c = new Coord();
	for(c.y = 0; c.y < sz.y; c.y++) {
	    for(c.x = 0; c.x < sz.x; c.x++) {
		if(mode == 1)
		    gl.glColor3f((c.x + 1) / 256.0f, (c.y + 1) / 256.0f, 0.0f);
		if(mode == 2)
		    gl.glColor3f(0.0f, 0.0f, 0.0f);
		gl.glVertex3f(c.x * tilesz.x, c.y * -tilesz.y, fz(c.x, c.y));
		if(mode == 2)
		    gl.glColor3f(0.0f, 1.0f, 0.0f);
		gl.glVertex3f(c.x * tilesz.x, (c.y + 1) * -tilesz.y, fz(c.x, c.y + 1));
		if(mode == 2)
		    gl.glColor3f(1.0f, 1.0f, 0.0f);
		gl.glVertex3f((c.x + 1) * tilesz.x, (c.y + 1) * -tilesz.y, fz(c.x + 1, c.y + 1));
		if(mode == 2)
		    gl.glColor3f(0.0f, 0.0f, 0.0f);
		gl.glVertex3f(c.x * tilesz.x, c.y * -tilesz.y, fz(c.x, c.y));
		if(mode == 2)
		    gl.glColor3f(1.0f, 1.0f, 0.0f);
		gl.glVertex3f((c.x + 1) * tilesz.x, (c.y + 1) * -tilesz.y, fz(c.x + 1, c.y + 1));
		if(mode == 2)
		    gl.glColor3f(1.0f, 0.0f, 0.0f);
		gl.glVertex3f((c.x + 1) * tilesz.x, c.y * -tilesz.y, fz(c.x + 1, c.y));
	    }
	}	
	gl.glEnd();
	GOut.checkerr(gl);
    }
    
    public boolean setup(RenderList rl) {
	for(Layer l : layers)
	    rl.add(l, null);
	for(Rendered e : extras)
	    rl.add(e, null);
	return(true);
    }
}
