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

    /* XXX: Should be somewhere else, but nor sure where just yet. */
    private MCache.SurfaceID getsurf(String surf) {
	switch(surf) {
	case "map": return(MCache.SurfaceID.map);
	case "trn": return(MCache.SurfaceID.trn);
	default:
	    Warning.warn("unknown surface: %s", surf);
	    return(MCache.SurfaceID.map);
	}
    }

    protected Gob.Placer placer = null;
    public Gob.Placer placer() {
	if(placer == null) {
	    Resource res = getres();
	    Resource.Props props = (res == null) ? null : res.layer(Resource.props);
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
		    case "surface": {
			placer = new Gob.DefaultPlace(gob.glob.map, getsurf((String)desc[1]));
			break;
		    }
		    case "incline": {
			placer = new Gob.InclinePlace(gob.glob.map, getsurf((String)desc[1]));
			break;
		    }
		    case "base": {
			String id = "";
			if(desc.length > 2)
			    id = (String)desc[2];
			placer = new Gob.BasePlace(gob.glob.map, getsurf((String)desc[1]), res, id);
			break;
		    }
		    case "plane": {
			String id = "";
			if(desc.length > 2)
			    id = (String)desc[2];
			placer = new Gob.PlanePlace(gob.glob.map, getsurf((String)desc[1]), res, id);
			break;
		    }
		    default: {
			if(opt) {
			    Warning.warn("%s specifes unknown placement: %s", res.name, type);
			    break;
			} else {
			    throw(new RuntimeException(String.format("%s specifes unknown placement: %s", res.name, type)));
			}
		    }
		    }
		}
	    }
	    if(placer == null)
		placer = gob.glob.map.trnplace;
	}
	return(placer);
    }

    public void gtick(Render g) {
    }

    public Skeleton.Pose getpose() {
	return(null);
    }
}
