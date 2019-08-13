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

import haven.*;
import haven.glsl.*;
import static haven.glsl.Cons.*;
import static haven.glsl.Type.*;

public class LatentMat extends GLState.Abstract {
    public static final Slot<LatentMat> slot = new Slot<>(Slot.Type.DRAW, LatentMat.class);
    public final GLState mat;
    public final String id, act;

    public LatentMat(GLState mat, String id) {
	this.mat = mat;
	this.id = id;
	this.act = null;
    }

    public LatentMat(String act) {
	this.mat = null;
	this.id = null;
	this.act = act;
    }

    public void prep(Buffer buf) {
	if((mat != null) && (id != null))
	    buf.put(slot, this);
	if(act != null) {
	    LatentMat cur = buf.get(slot);
	    if((cur != null) && (cur.id == act))
		cur.mat.prep(buf);
	}
    }

    @Material.ResName("latent")
    public static class $latent implements Material.ResCons {
	public GLState cons(Resource res, Object... args) {
	    return(new LatentMat(((String)args[0]).intern()));
	}
    }
}
