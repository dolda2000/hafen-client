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
import haven.Skeleton.Pose;
import haven.Skeleton.PoseMod;
import haven.MorphedMesh.Morpher;

public class SkelSprite extends Sprite implements Gob.Overlay.CUpd, Skeleton.HasPose {
    /* XXXRENDER
    public static final GLState
	rigid = new Material.Colors(java.awt.Color.GREEN),
	morphed = new Material.Colors(java.awt.Color.RED),
	unboned = new Material.Colors(java.awt.Color.YELLOW);
    */
    public static boolean bonedb = false;
    public static final float ipollen = 0.3f;
    public final Skeleton skel;
    public final Pose pose;
    public PoseMod[] mods = new PoseMod[0];
    public MeshAnim.Anim[] manims = new MeshAnim.Anim[0];
    /* XXXRENDER
    private Morpher.Factory mmorph;
    */
    private final PoseMorph pmorph;
    private Pose oldpose;
    private float ipold;
    private boolean stat = true;
    private RenderTree.Node[] parts;
    private MorphedMesh[] morphparts = {};
    
    public static final Factory fact = new Factory() {
	    public Sprite create(Owner owner, Resource res, Message sdt) {
		if(res.layer(Skeleton.Res.class) == null)
		    return(null);
		return(new SkelSprite(owner, res, sdt));
	    }
	};
    
    private SkelSprite(Owner owner, Resource res, Message sdt) {
	super(owner, res);
	skel = res.layer(Skeleton.Res.class).s;
	pose = skel.new Pose(skel.bindpose);
	pmorph = new PoseMorph(pose);
	int fl = sdt.eom()?0xffff0000:decnum(sdt);
	chposes(fl, true);
	chparts(fl);
    }

    /* XXX: It's ugly to snoop inside a wrapping, but I can't think of
     * a better way to apply morphing to renderlinks right now. */
    private RenderTree.Node animwrap(Pipe.Op.Wrapping wrap, Collection<MorphedMesh> mbuf) {
	if(!(wrap.r instanceof FastMesh))
	    return(wrap);
	FastMesh m = (FastMesh)wrap.r;
	/* XXXRENDER
	for(MeshAnim.Anim anim : manims) {
	    if(anim.desc().animp(m)) {
		Rendered ret = wrap.st().apply(new MorphedMesh(m, mmorph));
		if(bonedb)
		    ret = morphed.apply(ret);
		return(ret);
	    }
	}
	*/
	RenderTree.Node ret;
	if(PoseMorph.boned(m)) {
	    String bnm = PoseMorph.boneidp(m);
	    if(true /* XXXRENDER */ || bnm == null) {
		MorphedMesh mpart = new MorphedMesh(m, pmorph);
		ret = wrap.op.apply(mpart, wrap.locked);
		mbuf.add(mpart);
		/* XXXRENDER
		if(bonedb)
		    ret = morphed.apply(ret);
		*/
	    /* XXXRENDER
	    } else {
		ret = pose.bonetrans2(skel.bones.get(bnm).idx).apply(wrap);
		if(bonedb)
		    ret = rigid.apply(ret);
	    */
	    }
	} else {
	    ret = wrap;
	    /* XXXRENDER
	    if(bonedb)
		ret = unboned.apply(ret);
	    */
	}
	return(ret);
    }

    private void chparts(int mask) {
	Collection<RenderTree.Node> rl = new LinkedList<RenderTree.Node>();
	Collection<MorphedMesh> mbuf = new LinkedList<MorphedMesh>();
	for(FastMesh.MeshRes mr : res.layers(FastMesh.MeshRes.class)) {
	    if((mr.mat != null) && ((mr.id < 0) || (((1 << mr.id) & mask) != 0)))
		rl.add(animwrap(mr.mat.get().apply(mr.m), mbuf));
	}
	for(RenderLink.Res lr : res.layers(RenderLink.Res.class)) {
	    if((lr.id < 0) || (((1 << lr.id) & mask) != 0)) {
		RenderTree.Node r = lr.l.make();
		if(r instanceof Pipe.Op.Wrapping)
		    r = animwrap((Pipe.Op.Wrapping)r, mbuf);
		rl.add(r);
	    }
	}
	this.parts = rl.toArray(new RenderTree.Node[0]);
	this.morphparts = mbuf.toArray(new MorphedMesh[0]);
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

    private void chmanims(int mask) {
	Collection<MeshAnim.Anim> anims = new LinkedList<MeshAnim.Anim>();
	for(MeshAnim.Res ar : res.layers(MeshAnim.Res.class)) {
	    if((ar.id < 0) || (((1 << ar.id) & mask) != 0))
		anims.add(ar.make());
	}
	this.manims = anims.toArray(new MeshAnim.Anim[0]);
	// this.mmorph = MorphedMesh.combine(this.manims); XXXRENDER
    }

    private Map<Skeleton.ResPose, PoseMod> modids = new HashMap<Skeleton.ResPose, PoseMod>();
    private void chposes(int mask, boolean old) {
	chmanims(mask);
	if(!old) {
	    this.oldpose = skel.new Pose(pose);
	    this.ipold = 1.0f;
	}
	Collection<PoseMod> poses = new LinkedList<PoseMod>();
	stat = true;
	Skeleton.ModOwner mo = (owner instanceof Skeleton.ModOwner)?(Skeleton.ModOwner)owner:Skeleton.ModOwner.nil;
	Map<Skeleton.ResPose, PoseMod> newids = new HashMap<Skeleton.ResPose, PoseMod>();
	for(Skeleton.ResPose p : res.layers(Skeleton.ResPose.class)) {
	    if((p.id < 0) || ((mask & (1 << p.id)) != 0)) {
		Skeleton.PoseMod mod;
		if((mod = modids.get(p)) == null) {
		    mod = p.forskel(mo, skel, p.defmode);
		    if(old)
			mod.age();
		}
		if(p.id >= 0)
		    newids.put(p, mod);
		if(!mod.stat())
		    stat = false;
		poses.add(mod);
	    }
	}
	this.mods = poses.toArray(new PoseMod[0]);
	this.modids = newids;
	rebuild();
    }

    public void update(Message sdt) {
	int fl = sdt.eom()?0xffff0000:decnum(sdt);
	chposes(fl, false);
	chparts(fl);
    }
    
    @Override public void added(RenderTree.Slot slot) {
	for(RenderTree.Node p : parts)
	    slot.add(p);
	// slot.add(pose.debug); XXXRENDER
    }
    
    public boolean tick(double ddt) {
	float dt = (float)ddt;
	if(!stat || (ipold > 0)) {
	    boolean done = true;
	    for(PoseMod m : mods) {
		m.tick(dt);
		done = done && m.done();
	    }
	    if(done)
		stat = true;
	    if(ipold > 0) {
		if((ipold -= (dt / ipollen)) < 0) {
		    ipold = 0;
		    oldpose = null;
		}
	    }
	    rebuild();
	}
	for(MeshAnim.Anim anim : manims)
	    anim.tick(dt);
	return(false);
    }

    public void gtick(Render g) {
	for(MorphedMesh mesh : morphparts)
	    mesh.update(g);
    }

    public Object staticp() {
	if(!stat || (manims.length > 0) || (ipold > 0))
	    return(null);
	return(Gob.SemiStatic.class);
    }

    public Pose getpose() {
	return(pose);
    }

    static {
	Console.setscmd("bonedb", new Console.Command() {
		public void run(Console cons, String[] args) {
		    bonedb = Utils.parsebool(args[1], false);
		}
	    });
    }
}
