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
import haven.Resource.Tile;

public abstract class Tiler {
    public final int id;
    
    public Tiler(int id) {
	this.id = id;
    }
    
    public static class MPart {
	public Surface.Vertex[] v;
	public float[] tcx, tcy;
	public int[] f;
	public GLState mat = null;

	public MPart(Surface.Vertex[] v, float[] tcx, float[] tcy, int[] f) {
	    this.v = v; this.tcx = tcx; this.tcy = tcy; this.f = f;
	}

	public GLState mcomb(GLState mat) {
	    return((this.mat == null)?mat:(GLState.compose(mat, this.mat)));
	}

	public static final float[] ctcx = {0, 0, 1, 1}, ctcy = {0, 1, 1, 0};
	public static final int[] rdiag = {0, 1, 2, 0, 2, 3}, ldiag = {0, 1, 3, 1, 2, 3};
	public static MPart splitquad(Surface.Vertex[] corners, boolean diag) {
	    return(new MPart(corners, ctcx, ctcy, diag?rdiag:ldiag));
	}
    }

    public static interface MCons {
	public void faces(MapMesh m, Coord lc, Coord gc, MPart desc);
    }

    public void model(MapMesh m, Random rnd, Coord lc, Coord gc) {
	MapMesh.MapSurface s = m.data(m.gnd);
	if(s.split[s.ts.o(lc)]) {
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

    public void lay(MapMesh m, Coord lc, Coord gc, MCons cons) {
	MapMesh.MapSurface s = m.data(m.gnd);
	cons.faces(m, lc, gc, MPart.splitquad(s.fortilea(lc), s.split[s.ts.o(lc)]));
    }

    public abstract void lay(MapMesh m, Random rnd, Coord lc, Coord gc);
    public abstract void trans(MapMesh m, Random rnd, Tiler gt, Coord lc, Coord gc, int z, int bmask, int cmask);
    
    public void layover(MapMesh m, Coord lc, Coord gc, int z, Tile t) {
	m.new Plane(m.gnd(), lc, z, t);
    }

    public GLState drawstate(Glob glob, GLConfig cfg, Coord3f c) {
	return(null);
    }
    
    public static class FactMaker implements Resource.PublishedCode.Instancer {
	public Factory make(Class<?> cl) throws InstantiationException, IllegalAccessException {
	    if(Factory.class.isAssignableFrom(cl)) {
		return(cl.asSubclass(Factory.class).newInstance());
	    } else if(Tiler.class.isAssignableFrom(cl)) {
		Class<? extends Tiler> tcl = cl.asSubclass(Tiler.class);
		try {
		    final Constructor<? extends Tiler> cons = tcl.getConstructor(Integer.TYPE, Resource.Tileset.class);
		    return(new Factory() {
			    public Tiler create(int id, Resource.Tileset set) {
				return(Utils.construct(cons, id, set));
			    }
			});
		} catch(NoSuchMethodException e) {}
		throw(new RuntimeException("Could not find dynamic tiler contructor for " + tcl));
	    }
	    return(null);
	}
    }

    @Resource.PublishedCode(name = "tile", instancer = FactMaker.class)
    public static interface Factory {
	public Tiler create(int id, Resource.Tileset set);
    }

    @dolda.jglob.Discoverable
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ResName {
	public String value();
    }

    private static final Map<String, Factory> rnames = new TreeMap<String, Factory>();
    static {
	java.security.AccessController.doPrivileged(new java.security.PrivilegedAction<Object>() {
		public Object run() {
		    for(Class<?> cl : dolda.jglob.Loader.get(ResName.class).classes()) {
			String nm = cl.getAnnotation(ResName.class).value();
			try {
			    rnames.put(nm, (Factory)cl.newInstance());
			} catch(InstantiationException e) {
			    throw(new Error(e));
			} catch(IllegalAccessException e) {
			    throw(new Error(e));
			}
		    }
		    return(null);
		}
	    });
    }

    public static Factory byname(String name) {
	return(rnames.get(name));
    }
}
