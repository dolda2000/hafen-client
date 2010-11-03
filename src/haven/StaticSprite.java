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

public class StaticSprite extends Sprite {
    Rendered[] parts;
    
    public static final Factory fact = new Factory() {
	    public Sprite create(Owner owner, Resource res, Message sdt) {
		return(new StaticSprite(owner, res, sdt));
	    }
	};
    
    public StaticSprite(Owner owner, Resource res, Message sdt) {
	super(owner, res);
	Collection<Rendered> rl = new LinkedList<Rendered>();
	for(final FastMesh.MeshRes mr : res.layers(FastMesh.MeshRes.class)) {
	    if(mr.mat != null) {
		rl.add(new Rendered() {
			FastMesh m = mr.m;
			Material mat = mr.mat;
			
			public void draw(GOut g) {
			    g.matsel(mat);
			    m.draw(g);
			}
			
			public boolean setup(RenderList l) {
			    return(true);
			}
		    });
	    }
	}
	this.parts = rl.toArray(new Rendered[0]);
    }
    
    public boolean setup(RenderList r) {
	for(Rendered p : parts)
	    r.add(p, null);
	return(false);
    }
}
