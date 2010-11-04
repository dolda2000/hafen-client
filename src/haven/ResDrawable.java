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

import java.awt.Color;

public class ResDrawable extends Drawable {
    final Indir<Resource> res;
    final Message sdt;
    Sprite spr = null;
    int delay = 0;
	
    public ResDrawable(Gob gob, Indir<Resource> res, Message sdt) {
	super(gob);
	this.res = res;
	this.sdt = sdt;
	init();
    }
	
    public ResDrawable(Gob gob, Resource res) {
	this(gob, res.indir(), new Message(0));
    }
	
    public void init() {
	if(spr != null)
	    return;
	if(res.get() == null)
	    return;
	spr = Sprite.create(gob, res.get(), sdt.clone());
    }
	
    public void setup(RenderList rl) {
	init();
	if(spr != null) {
	    if((MapView.lighting == 2) && res.get().name.equals("gfx/borka/body")) {
		Color amb = new Color(128, 64, 0);
		Color col = new Color(255, 224, 192);
		PosLight spot = new PosLight(amb, col, Color.WHITE, new Coord3f(0.0f, 4.0f, 15.0f));
		spot.att(1.0f, 0.5f / 55.0f, 1.0f / 5500.0f);
		rl.add(spot);
	    }
	    spr.setup(rl);
	}
    }
	
    public void ctick(int dt) {
	if(spr == null) {
	    delay += dt;
	} else {
	    spr.tick(delay + dt);
	    delay = 0;
	}
    }
    
    public Resource.Neg getneg() {
	Resource r = res.get();
	if(r == null)
	    return(null);
	return(r.layer(Resource.negc));
    }
}
