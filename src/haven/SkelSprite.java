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
import haven.Skeleton.Pose;
import haven.Skeleton.PoseMod;
import haven.Skeleton.TrackMod;

public class SkelSprite extends Sprite implements Gob.Overlay.CUpd {
    private static final GLState
	rigid = new Material.Colors(java.awt.Color.GREEN),
	morphed = new Material.Colors(java.awt.Color.RED),
	unboned = new Material.Colors(java.awt.Color.YELLOW);
    public static boolean bonedb = false;
    public static final float defipol = 0;
    private final Skeleton skel;
    public final Pose pose;
    private Pose oldpose;
    private float ipold, ipol;
    private PoseMod[] mods = new PoseMod[0];
    private boolean stat = true;
    private Rendered[] parts;
    
    public static final Factory fact = new Factory() {
	    public Sprite create(Owner owner, Resource res, Message sdt) {
		if(res.layer(Skeleton.Res.class) == null)
		    return(null);
		return(new SkelSprite(owner, res, sdt));
	    }
	};
    
    public static int decnum(Message sdt) {
	if(sdt == null)
	    return(0);
	int ret = 0, off = 0;
	while(!sdt.eom()) {
	    ret |= sdt.uint8() << off;
	    off += 8;
	}
	return(ret);
    }

    private SkelSprite(Owner owner, Resource res, Message sdt) {
	super(owner, res);
	skel = res.layer(Skeleton.Res.class).s;
	pose = skel.new Pose(skel.bindpose);
	int fl = sdt.eom()?0xffff0000:SkelSprite.decnum(sdt);
	chparts(fl);
	chposes(fl, 0);
    }

    private void chparts(int mask) {
	Collection<Rendered> rl = new LinkedList<Rendered>();
	for(FastMesh.MeshRes mr : res.layers(FastMesh.MeshRes.class)) {
	    if((mr.mat != null) && ((mr.id < 0) || (((1 << mr.id) & mask) != 0))) {
		Rendered r;
		if(MorphedMesh.boned(mr.m)) {
		    String bnm = MorphedMesh.boneidp(mr.m);
		    if(bnm == null) {
			r = mr.mat.get().apply(new MorphedMesh(mr.m, pose));
			if(bonedb)
			    r = morphed.apply(r);
		    } else {
			r = pose.bonetrans2(skel.bones.get(bnm).idx).apply(mr.mat.get().apply(mr.m));
			if(bonedb)
			    r = rigid.apply(r);
		    }
		} else {
		    r = mr.mat.get().apply(mr.m);
		    if(bonedb)
			r = unboned.apply(r);
		}
		rl.add(r);
	    }
	}
	this.parts = rl.toArray(new Rendered[0]);
    }
    
    private void rebuild() {
	pose.reset();
	for(PoseMod m : mods)
	    m.apply(pose);
	if(ipold > 0) {
	    float f = ipold * ipold * (3 - (2 * ipold));
	    pose.blend(oldpose, f);
	}
	pose.gbuild();
    }

    private void chposes(int mask, float ipol) {
	if((this.ipol = ipol) > 0) {
	    this.oldpose = skel.new Pose(pose);
	    this.ipold = 1.0f;
	}
	Collection<PoseMod> poses = new LinkedList<PoseMod>();
	stat = true;
	for(Skeleton.ResPose p : res.layers(Skeleton.ResPose.class)) {
	    if((p.id < 0) || ((mask & (1 << p.id)) != 0)) {
		Skeleton.TrackMod mod = p.forskel(skel, p.defmode);
		if(owner instanceof Gob)
		    mod.fxgob = (Gob)owner;
		if(!mod.stat())
		    stat = false;
		poses.add(mod);
	    }
	}
	this.mods = poses.toArray(new PoseMod[0]);
	rebuild();
    }

    public void update(Message sdt) {
	int fl = sdt.eom()?0xffff0000:SkelSprite.decnum(sdt);
	chparts(fl);
	chposes(fl, defipol);
    }
    
    public boolean setup(RenderList rl) {
	for(Rendered p : parts)
	    rl.add(p, null);
	/* rl.add(pose.debug, null); */
	return(false);
    }
    
    private boolean tick(int idt, double v) {
	if(!stat || (ipold > 0)) {
	    float dt = idt / 1000.0f;
	    for(PoseMod m : mods) {
		float mdt = dt;
		if(m instanceof TrackMod) {
		    TrackMod t = (TrackMod)m;
		    if(t.speedmod)
			mdt *= (float)(v / t.nspeed);
		}
		m.tick(mdt);
	    }
	    if(ipold > 0) {
		if((ipold -= (dt / ipol)) < 0) {
		    ipold = 0;
		    oldpose = null;
		}
	    }
	    rebuild();
	}
	return(false);
    }

    public boolean tick(int dt) {
	double v = 0;
	if(owner instanceof Gob) {
	    Moving mv = ((Gob)owner).getattr(Moving.class);
	    if(mv != null)
		v = mv.getv();
	}
	return(tick(dt, v));
    }

    static {
	Console.setscmd("bonedb", new Console.Command() {
		public void run(Console cons, String[] args) {
		    bonedb = Utils.parsebool(args[1], false);
		}
	    });
    }
}
