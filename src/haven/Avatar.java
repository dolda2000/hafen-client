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

public class Avatar extends GAttrib {
    public List<Indir<Resource>> layers = null;
    private List<Resource.Image> images = null;

    public Avatar(Gob gob) {
	super(gob);
    }
	
    void setlayers(List<Indir<Resource>> layers) {
	synchronized(this) {
	    this.layers = layers;
	    this.images = null;
	}
    }

    public List<Resource.Image> images() {
	synchronized(this) {
	    if((images == null) && (layers != null)) {
		List<Resource.Image> nimg = new ArrayList<>(layers.size());
		for(Indir<Resource> res : layers) {
		    nimg.add(res.get().layer(Resource.imgc));
		}
		Collections.sort(nimg, (a, b) -> (a.z - b.z));
		images = nimg;
	    }
	    return(images);
	}
    }

    @OCache.DeltaType(OCache.OD_AVATAR)
    public static class $avatar implements OCache.Delta {
	public void apply(Gob g, OCache.AttrDelta msg) {
	    List<Indir<Resource>> layers = new LinkedList<Indir<Resource>>();
	    while(true) {
		int layer = msg.uint16();
		if(layer == 65535)
		    break;
		layers.add(OCache.Delta.getres(g, layer));
	    }
	    Avatar ava = g.getattr(Avatar.class);
	    if(ava == null) {
		ava = new Avatar(g);
		g.setattr(ava);
	    }
	    ava.setlayers(layers);
	}
    }
}
