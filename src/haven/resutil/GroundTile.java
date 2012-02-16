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

package haven.resutil;

import java.util.*;
import haven.*;
import haven.Resource.Tile;

public class GroundTile extends Tiler {
    public final Resource.Tileset set;
    public static final Factory fac = new Factory() {
	    public Tiler create(int id, Resource.Tileset set) {
		return(new GroundTile(id, set));
	    }
	};

    public GroundTile(int id, Resource.Tileset set) {
	super(id);
	this.set = set;
    }
    
    public void lay(MapMesh m, Random rnd, Coord lc, Coord gc) {
	Tile g = set.ground.pick(rnd);
	m.new Plane(m.gnd(), lc, 0, g);
    }
    
    public void trans(MapMesh m, Random rnd, Tiler gt, Coord lc, Coord gc, int z, int bmask, int cmask) {
	if(m.map.gettile(gc) <= id)
	    return;
	if((set.btrans != null) && (bmask > 0))
	    gt.layover(m, lc, gc, z, set.btrans[bmask - 1].pick(rnd));
	if((set.ctrans != null) && (cmask > 0))
	    gt.layover(m, lc, gc, z, set.ctrans[cmask - 1].pick(rnd));
    }
}
