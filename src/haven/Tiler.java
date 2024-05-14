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
import java.lang.annotation.*;
import haven.Surface.Vertex;
import haven.Surface.MeshVertex;
import haven.render.*;

public abstract class Tiler {
    public final int id;
    
    public Tiler(int id) {
	this.id = id;
    }
    
    public static class MPart {
	public Coord lc, gc;
	public Surface.Vertex[] v;
	public float[] tcx, tcy;
	public int[] f;
	public Pipe.Op mat = null;

	public MPart(Coord lc, Coord gc, Surface.Vertex[] v, float[] tcx, float[] tcy, int[] f) {
	    this.lc = lc; this.gc = gc; this.v = v; this.tcx = tcx; this.tcy = tcy; this.f = f;
	}

	public MPart(MPart... parts) {
	    this.lc = parts[0].lc; this.gc = parts[0].gc;
	    int[][] vmap = new int[parts.length][];
	    int vbn = 0;
	    for(int i = 0; i < parts.length; i++) {
		vbn += parts[i].v.length;
		vmap[i] = new int[parts[i].v.length];
	    }
	    Vertex[] vbuf = new Vertex[vbn];
	    int vn  = 0;
	    for(int i = 0; i < parts.length; i++) {
		int cvn = vn;
		Vertex[] cv = parts[i].v;
		for(int o = 0; o < cv.length; o++) {
		    found: {
			for(int u = 0; u < cvn; u++) {
			    if(cv[o] == vbuf[u]) {
				vmap[i][o] = u;
				break found;
			    }
			}
			vbuf[vmap[i][o] = vn++] = cv[o];
		    }
		}
	    }
	    this.v = Utils.splice(vbuf, 0, vn);
	    int fn = 0;
	    for(MPart p : parts)
		fn += p.f.length;
	    this.f = new int[fn];
	    fn = 0;
	    for(int i = 0; i < parts.length; i++) {
		for(int o = 0; o < parts[i].f.length; o++)
		    this.f[fn++] = vmap[i][parts[i].f[o]];
	    }
	    mapvertices(parts, vmap);
	}

	protected void mapvertices(MPart[] parts, int[][] vmap) {
	    tcx = new float[v.length]; tcy = new float[v.length];
	    for(int i = 0; i < parts.length; i++) {
		for(int o = 0; o < parts[i].v.length; o++) {
		    tcx[vmap[i][o]] = parts[i].tcx[o];
		    tcy[vmap[i][o]] = parts[i].tcy[o];
		}
	    }
	}

	public Pipe.Op mcomb(Pipe.Op mat) {
	    return((this.mat == null)?mat:(Pipe.Op.compose(mat, this.mat)));
	}

	public static final float[] ctcx = {0, 0, 1, 1}, ctcy = {0, 1, 1, 0};
	public static final int[] rdiag = {0, 1, 2, 0, 2, 3}, ldiag = {0, 1, 3, 1, 2, 3};
	public static MPart splitquad(Coord lc, Coord gc, Surface.Vertex[] corners, boolean diag) {
	    return(new MPart(lc, gc, corners, ctcx, ctcy, diag?ldiag:rdiag));
	}
    }

    public static interface MCons {
	public void faces(MapMesh m, MPart desc);
	public static final MCons nil = new MCons() {
		public void faces(MapMesh m, MPart desc) {}
	    };
    }

    public static interface CTrans {
	public MCons tcons(int z, int bmask, int cmask);
    }

    public static interface VertFactory {
	public MeshVertex make(MeshBuf buf, MPart d, int i);
	public static final VertFactory id = new VertFactory() {
		public MeshVertex make(MeshBuf buf, MPart d, int i) {
		    return(new MeshVertex(buf, d.v[i]));
		}
	    };
    }

    public static class SModel extends MapMesh.Model {
	private final VertFactory f;
	private final MeshVertex[] map;

	public SModel(MapMesh m, NodeWrap mat, VertFactory f) {
	    super(m, mat);
	    this.f = f;
	    this.map = new MeshVertex[m.data(MapMesh.gnd).vl.length];
	}

	public MeshVertex get(MPart d, int i) {
	    MeshVertex ret;
	    if((ret = map[d.v[i].vi]) == null)
		ret = map[d.v[i].vi] = f.make(this, d, i);
	    return(ret);
	}

	public MeshVertex[] get(MPart d) {
	    MeshVertex[] ret = new MeshVertex[d.v.length];
	    for(int i = 0; i < d.v.length; i++)
		ret[i] = get(d, i);
	    return(ret);
	}

	public static class Key implements MapMesh.DataID<SModel> {
	    public final NodeWrap mat;
	    public final VertFactory f;
	    private final int hash;

	    public Key(NodeWrap mat, VertFactory f) {
		this.mat = mat;
		this.f = f;
		this.hash = (mat.hashCode() * 31) + f.hashCode();
	    }

	    public int hashCode() {
		return(hash);
	    }

	    public boolean equals(Object x) {
		return((x instanceof Key) && mat.equals(((Key)x).mat) && f.equals(((Key)x).f));
	    }

	    public SModel make(MapMesh m) {
		return(new SModel(m, mat, f));
	    }
	}

	public static SModel get(MapMesh m, NodeWrap mat, VertFactory f) {
	    return(m.data(new Key(mat, f)));
	}
    }

    public static void flatmodel(MapMesh m, Coord lc) {
	MapMesh.MapSurface s = m.data(m.gnd);
	if(s.split[s.bs.o(lc)]) {
	    s.new Face(s.surf[s.vs.o(lc.x, lc.y)],
		       s.surf[s.vs.o(lc.x, lc.y + 1)],
		       s.surf[s.vs.o(lc.x + 1, lc.y + 1)]);
	    s.new Face(s.surf[s.vs.o(lc.x, lc.y)],
		       s.surf[s.vs.o(lc.x + 1, lc.y + 1)],
		       s.surf[s.vs.o(lc.x + 1, lc.y)]);
	} else {
	    s.new Face(s.surf[s.vs.o(lc.x, lc.y)],
		       s.surf[s.vs.o(lc.x, lc.y + 1)],
		       s.surf[s.vs.o(lc.x + 1, lc.y)]);
	    s.new Face(s.surf[s.vs.o(lc.x, lc.y + 1)],
		       s.surf[s.vs.o(lc.x + 1, lc.y + 1)],
		       s.surf[s.vs.o(lc.x + 1, lc.y)]);
	}
    }

    public void model(MapMesh m, Random rnd, Coord lc, Coord gc) {
	flatmodel(m, lc);
    }

    public void lay(MapMesh m, Coord lc, Coord gc, MCons cons, boolean cover) {
	MapMesh.MapSurface s = m.data(m.gnd);
	cons.faces(m, MPart.splitquad(lc, gc, s.fortilea(lc), s.split[s.bs.o(lc)]));
    }

    public abstract void lay(MapMesh m, Random rnd, Coord lc, Coord gc);
    public abstract void trans(MapMesh m, Random rnd, Tiler gt, Coord lc, Coord gc, int z, int bmask, int cmask);

    public static class MapZSurface implements MCache.ZSurface {
	public final MapMesh m;

	public MapZSurface(MapMesh m) {
	    this.m = m;
	}

	public double getz(Coord tc) {
	    return(m.map.getfz(tc));
	}
    }

    public MCache.ZSurface getsurf(MapMesh m, MCache.SurfaceID id) {
	return(new MapZSurface(m));
    }

    public Pipe.Op clickstate() {
	return(MapMesh.clickmain);
    }
    
    public Pipe.Op drawstate(Glob glob, Coord3f c) {
	return(null);
    }
    
    public static class FactMaker extends Resource.PublishedCode.Instancer.Chain<Factory> {
	public FactMaker() {
	    super(Factory.class);
	    add(new Direct<>(Factory.class));
	    add(new Construct<>(Factory.class, Tiler.class, new Class<?>[] {Integer.TYPE, Tileset.class},
				(cons) -> (id, set) -> cons.apply(new Object[] {id, set})));
	}
    }

    @Resource.PublishedCode(name = "tile", instancer = FactMaker.class)
    public static interface Factory {
	public Tiler create(int id, Tileset set);
    }

    @dolda.jglob.Discoverable
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ResName {
	public String value();
    }

    private static final Map<String, Factory> rnames = new TreeMap<String, Factory>();
    static {
	for(Class<?> cl : dolda.jglob.Loader.get(ResName.class).classes()) {
	    String nm = cl.getAnnotation(ResName.class).value();
	    rnames.put(nm, Utils.construct(cl.asSubclass(Factory.class)));
	}
    }

    public static Factory byname(String name) {
	return(rnames.get(name));
    }
}
