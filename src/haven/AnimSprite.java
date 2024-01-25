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
import haven.render.*;

public class AnimSprite extends Sprite {
    private final RenderTree.Node[] parts;
    private final MeshAnim.Animation[] anims;

    public static final Factory fact = new Factory() {
	    public Sprite create(Owner owner, Resource res, Message sdt) {
		if(res.layer(MeshAnim.Res.class) == null)
		    return(null);
		return(new AnimSprite(owner, res, sdt) {
			public String toString() {
			    return(String.format("#<anim-sprite %s>", res.name));
			}
		    });
	    }
	};

    private AnimSprite(Owner owner, Resource res, Message sdt) {
	super(owner, res);
	int mask = sdt.eom() ? 0xffff0000 : decnum(sdt);
	Collection<MeshAnim.Animation> anims = new LinkedList<>();
	for(MeshAnim.Res ar : res.layers(MeshAnim.Res.class)) {
	    if((ar.id < 0) || (((1 << ar.id) & mask) != 0))
		anims.add(ar.make());
	}
	this.anims = anims.toArray(new MeshAnim.Animation[0]);
	Collection<RenderTree.Node> rl = new LinkedList<>();
	mesh: for(FastMesh.MeshRes mr : res.layers(FastMesh.MeshRes.class)) {
	    if((mr.mat != null) && ((mr.id < 0) || (((1 << mr.id) & mask) != 0))) {
		for(MeshAnim.Animation anim : anims) {
		    if(anim.desc().animp(mr.m)) {
			rl.add(RUtils.StateTickNode.from(mr.mat.get().apply(anim.desc().apply(mr.m)), anim::state));
			continue mesh;
		    }
		}
		rl.add(mr.mat.get().apply(mr.m));
	    }
	}
	Owner rec = null;
	for(RenderLink.Res lr : res.layers(RenderLink.Res.class)) {
	    if((lr.id < 0) || (((1 << lr.id) & mask) != 0)) {
		if(rec == null)
		    rec = new RecOwner();
		rl.add(lr.l.make(rec));
	    }
	}
	parts = rl.toArray(new RenderTree.Node[0]);
    }

    public void added(RenderTree.Slot slot) {
	for(RenderTree.Node p : parts)
	    slot.add(p);
    }

    public boolean tick(double ddt) {
	float dt = (float)ddt;
	boolean ret = false;
	for(MeshAnim.Animation anim : anims)
	    ret = ret | anim.tick(dt);
	return(ret);
    }

    public void age() {
	for(MeshAnim.Animation anim : anims)
	    anim.age();
    }
}
