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

public class SkelSprite extends Sprite {
    private final Skeleton skel;
    private Skeleton.Pose pose;
    private final Rendered[] parts;
    
    public static final Factory fact = new Factory() {
	    public Sprite create(Owner owner, Resource res, Message sdt) {
		if(res.layer(Skeleton.Res.class) == null)
		    return(null);
		return(new SkelSprite(owner, res, sdt));
	    }
	};
    
    private Skeleton.Bone modb = null;
    private Skeleton.PoseMod mod = null;
    private SkelSprite(Owner owner, Resource res, Message sdt) {
	super(owner, res);
	skel = res.layer(Skeleton.Res.class).s;
	pose = skel.new Pose(skel.bindpose);
	Collection<Rendered> rl = new LinkedList<Rendered>();
	for(FastMesh.MeshRes mr : res.layers(FastMesh.MeshRes.class)) {
	    if(mr.mat != null)
		rl.add(mr.mat.apply(new MorphedMesh(mr.m, pose)));
	}
	this.parts = rl.toArray(new Rendered[0]);
	
	modb = skel.bones.get("Bone.013_R.002");
	mod = skel.new PoseMod();
    }
    
    public boolean setup(RenderList rl) {
	for(Rendered p : parts)
	    rl.add(p, null);
	rl.add(pose.debug, null);
	return(false);
    }
    
    double at = 0.0;
    public boolean tick(int dt) {
	if(modb != null) {
	    at += dt / 1000.0;
	    mod.reset();
	    mod.rot(modb.idx, (float)Math.sin(at * 5.0) * 0.2f, 1f, 0f, 0f);
	    pose.reset();
	    mod.apply(pose);
	    pose.gbuild();
	}
	return(false);
    }
}
