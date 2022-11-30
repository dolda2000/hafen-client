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

import haven.render.*;

public abstract class Drawable extends GAttrib implements Skeleton.HasPose, RenderTree.Node {
    public Drawable(Gob gob) {
	super(gob);
    }
	
    public abstract Resource getres();
    
    private static final Gob.Placer nilplace = new Gob.Placer() {
	    public Coord3f getc(Coord2d rc, double ra) {throw(new RuntimeException());}
	    public Matrix4f getr(Coord2d rc, double ra) {throw(new RuntimeException());}
	};
    protected Gob.Placer placer = null;
    public Gob.Placer placer() {
	if(placer == null) {
	    Resource res = getres();
	    Resource.Props props = res.layer(Resource.props);
	    if(props != null) {
		Object[] desc = (Object[])props.get("place");
		if(desc != null) {
		    String type = (String)desc[0];
		    boolean opt = false;
		    if(type.startsWith("o:")) {
			opt = true;
			type = type.substring(2);
		    }
		    switch(type) {
		    case "surface":
			MCache.SurfaceID id;
			String surf = (String)desc[1];
			switch(surf) {
			case "map": id = MCache.SurfaceID.map; break;
			case "trn": id = MCache.SurfaceID.trn; break;
			default:
			    Warning.warn("%s specifes unknown surface: %s", res.name, surf);
			    id = MCache.SurfaceID.map;
			    break;
			}
			placer = new Gob.DefaultPlace(gob.glob.map, id);
			break;
		    default:
			if(opt) {
			    Warning.warn("%s specifes unknown placement: %s", res.name, type);
			    break;
			} else {
			    throw(new RuntimeException(String.format("%s specifes unknown placement: %s", res.name, type)));
			}
		    }
		}
	    }
	    if(placer == null)
		placer = nilplace;
	}
	return((placer == nilplace) ? null : placer);
    }

    public void gtick(Render g) {
    }

    public Skeleton.Pose getpose() {
	return(null);
    }
}
