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
import haven.Resource.Tile;

public abstract class Tiler {
    public final int id;
    
    public Tiler(int id) {
	this.id = id;
    }
    
    public abstract void lay(MapMesh m, Random rnd, Coord lc, Coord gc);
    public abstract void trans(MapMesh m, Random rnd, Tiler gt, Coord lc, Coord gc, int z, int bmask, int cmask);
    
    public void layover(MapMesh m, Coord lc, Coord gc, int z, Tile t) {
	m.new Plane(m.gnd(), lc, z, t);
    }
    
    public static class FactMaker implements Resource.PublishedCode.Instancer {
	public Factory make(Class<?> cl) throws InstantiationException, IllegalAccessException {
	    if(Factory.class.isAssignableFrom(cl)) {
		return(cl.asSubclass(Factory.class).newInstance());
	    } else if(Tiler.class.isAssignableFrom(cl)) {
		final Class<? extends Tiler> tcl = cl.asSubclass(Tiler.class);
		return(new Factory() {
			public Tiler create(int id, Resource.Tileset set) {
			    try {
				try {
				    Constructor<? extends Tiler> m = tcl.getConstructor(Integer.TYPE, Resource.Tileset.class);
				    return(m.newInstance(id, set));
				} catch(NoSuchMethodException e) {}
				throw(new RuntimeException("Could not find dynamic tiler contructor for " + tcl));
			    } catch(IllegalAccessException e) {
				throw(new RuntimeException(e));
			    } catch(InstantiationException e) {
				throw(new RuntimeException(e));
			    } catch(InvocationTargetException e) {
				if(e.getCause() instanceof RuntimeException)
				    throw((RuntimeException)e.getCause());
				throw(new RuntimeException(e));
			    }
			}
		    });
	    }
	    return(null);
	}
    }

    @Resource.PublishedCode(name = "tile", instancer = FactMaker.class)
    public static interface Factory {
	public Tiler create(int id, Resource.Tileset set);
    }
}
